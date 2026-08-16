package mx.com.liverpool.p360.services.core.temp.xml.local.neostream;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;

import mx.com.liverpool.dataprofiling.preparison.envioproductos.PruebaEnvioPubSubMediaAssets;
import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;

/**
 * Single entry point intended for ReceiveSTEPFile.
 *
 * Old anotheropinion/FastProcess classes remain untouched in their package.
 * This processor uses:
 *   0) optional Media Assets Path processing;
 *   1) tiny SAX index pass;
 *   2) one DBAccessDataStub / one JDBC connection for the STEP;
 *   3) streaming Second Opinion + hard flush barrier;
 *   4) one shared streaming pass fan-out to all ordinary writers;
 *   5) structure writes in strict priority:
 *      PrimaryProductTaxonomy -> Sitios Web -> CommercialECC -> CommercialS4H.
 */
public final class NeoSTEPFileProcessor {

    private static final RESTWorkshop TIME = new RESTWorkshop();
    private static final Path LOG_PATH =
            Path.of("..", "logs", "neo_step_pipeline.log");

    private static final int MAX_CONCURRENT = Math.max(
            1,
            Integer.getInteger("p360.step.max.concurrent", 2));

    private static final Semaphore STEP_PERMITS =
            new Semaphore(MAX_CONCURRENT, true);

    private NeoSTEPFileProcessor() {
    }

    /**
     * Full ReceiveSTEPFile path: Media Assets first, then P360 data.
     *
     * PruebaEnvioPubSubMediaAssets must expose process(Path). A tiny patch for
     * that method is included in this package as a separate integration note.
     */
    public static int process(Path path) throws Exception {
        STEP_PERMITS.acquire();
        ELog log = logger();
        try (StepXmlCharsetNormalizer.NormalizedStepFile step =
                StepXmlCharsetNormalizer.prepare(path, log::log)) {

            Path processingPath = step.getProcessingPath();
            processMediaAssets(processingPath);
            return processP360DataCanonical(processingPath, path, log);
        } finally {
            STEP_PERMITS.release();
        }
    }

    /** Useful to test the new data path without sending Media Assets again. */
    public static int processP360DataOnly(Path path) throws Exception {
        ELog log = logger();
        try (StepXmlCharsetNormalizer.NormalizedStepFile step =
                StepXmlCharsetNormalizer.prepare(path, log::log)) {

            return processP360DataCanonical(
                    step.getProcessingPath(),
                    path,
                    log);
        }
    }

    private static int processP360DataCanonical(
            Path path,
            Path sourcePath,
            ELog log) throws Exception {

        if (path == null) {
            throw new IllegalArgumentException("STEP Path is null");
        }

        long totalStart = System.currentTimeMillis();
        log.log("START STEP: source=" + sourcePath + ", processing=" + path);

        long t = System.currentTimeMillis();
        StepXmlStreamingParser.StepIndex index =
                StepXmlStreamingParser.index(path);

        log.log(
                "Index pass: "
                + TIME.formatTime(System.currentTimeMillis() - t)
                + " products=" + index.getProductIds().size()
                + " productSKUs=" + index.getProductSkus().size()
                + " articles=" + index.getArticleIds().size()
                + " articleSKUs=" + index.getArticleSkus().size()
                + " productAttributesSeen=" + index.getProductAttributeIds().size()
                + " articleAttributesSeen=" + index.getArticleAttributeIds().size());

        int parsedProducts;

        try (DBAccessDataStub db = new DBAccessDataStub(log)) {

            t = System.currentTimeMillis();
            StepDbSnapshot snapshot = StepDbSnapshot.load(db, index);

            log.log(
                    "Bulk DB snapshot: "
                    + TIME.formatTime(System.currentTimeMillis() - t)
                    + " productBySku=" + snapshot.productBySku.size()
                    + " articleBySku=" + snapshot.articleBySku.size()
                    + " productCollisions=" + snapshot.productData.size()
                    + " articleCollisions=" + snapshot.articleData.size()
                    + " productInternalIds="
                    + snapshot.productObjectIdByIdentifier.size()
                    + " articleInternalIds="
                    + snapshot.articleObjectIdByIdentifier.size());

            StepSecondOpinionProcessor secondOpinion =
                    new StepSecondOpinionProcessor(snapshot, log);

            t = System.currentTimeMillis();
            parsedProducts =
                    StepXmlStreamingParser.parse(
                            path,
                            secondOpinion::accept);
            secondOpinion.finish();

            log.log(
                    "Second opinion streaming pass + flush: "
                    + TIME.formatTime(System.currentTimeMillis() - t));

            StepStatusComputer statusComputer =
                    new StepStatusComputer();

            Map<String, String> internalToExternalStatusMap =
                    loadExternalStatusMap(log);

            StepWriterPipeline writers =
                    new StepWriterPipeline(
                            db,
                            index,
                            statusComputer,
                            internalToExternalStatusMap,
                            log);

            t = System.currentTimeMillis();
            StepXmlStreamingParser.parse(path, writers::accept);
            writers.finishData();

            log.log(
                    "Normal writers streaming pass + data flush: "
                    + TIME.formatTime(System.currentTimeMillis() - t));

            t = System.currentTimeMillis();
            writers.finishStructures();

            log.log(
                    "Structure groups "
                    + "(Primary -> Web -> ECC -> S4H): "
                    + TIME.formatTime(System.currentTimeMillis() - t));
        }

        log.log(
                "DONE STEP: source="
                + sourcePath
                + ", processing="
                + path
                + " total="
                + TIME.formatTime(
                        System.currentTimeMillis() - totalStart));

        return parsedProducts;
    }

    /**
     * Folder replay without asking every specialized writer to walk the folder.
     * Each XML is submitted once to this processor.
     */
    public static void processDirectory(
            Path directory,
            boolean includeMediaAssets)
            throws Exception {

        try (java.util.stream.Stream<Path> stream =
                Files.walk(directory)) {

            java.util.List<Path> files =
                    stream
                        .filter(Files::isRegularFile)
                        .filter(path ->
                                path.getFileName()
                                    .toString()
                                    .toLowerCase()
                                    .endsWith(".xml"))
                        .sorted()
                        .toList();

            for (Path file : files) {
                if (includeMediaAssets) {
                    process(file);
                } else {
                    STEP_PERMITS.acquire();
                    try {
                        processP360DataOnly(file);
                    } finally {
                        STEP_PERMITS.release();
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            System.err.println(
                    "Usage: NeoSTEPFileProcessor "
                    + "<xml-file-or-directory> [--no-media]");
            return;
        }

        Path path = Path.of(args[0]);
        boolean includeMedia =
                args.length < 2
                || !"--no-media".equalsIgnoreCase(args[1]);

        if (Files.isDirectory(path)) {
            processDirectory(path, includeMedia);
        } else if (includeMedia) {
            process(path);
        } else {
            STEP_PERMITS.acquire();
            try {
                processP360DataOnly(path);
            } finally {
                STEP_PERMITS.release();
            }
        }
    }

    /**
     * Reflection intentionally keeps this package compilable against the older
     * PruebaEnvioPubSubMediaAssets source that only had process(String).
     * At runtime we require the Path overload rather than silently rematerialize
     * the complete XML as a String.
     */
    private static void processMediaAssets(Path path) throws Exception {
        try {
            Method method =
                    PruebaEnvioPubSubMediaAssets.class.getMethod(
                            "process",
                            Path.class);

            method.invoke(null, path);

        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(
                    "PruebaEnvioPubSubMediaAssets todavía no tiene "
                    + "process(Path). Aplica el patch incluido; no hago "
                    + "fallback a process(String) porque volvería a cargar "
                    + "todo el XML en heap.",
                    e);

        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }
    }

    private static Map<String, String> loadExternalStatusMap(ELog log) {
        Map<String, String> result = new TreeMap<>();

        Path file =
                Path.of(
                        PropertiesManager.get(
                                "p360.contingency.base_directory"),
                        "cache",
                        "templates",
                        "dictionaries",
                        "ExternalStatus");

        try (java.io.BufferedReader br =
                Files.newBufferedReader(file)) {

            String line;
            RESTWorkshop rw = new RESTWorkshop();

            while ((line = br.readLine()) != null) {
                String[] pieces =
                        rw.parseLine(line, "\"", ";", "\\");

                if (pieces.length > 1) {
                    result.put(pieces[0], pieces[1]);
                }
            }

        } catch (IOException e) {
            log.logE(e);
        }

        return result;
    }

    private static ELog logger() {
        return new ELog() {
            @Override
            public void log(String message) {
                try {
                    Files.createDirectories(LOG_PATH.getParent());

                    try (java.io.PrintWriter pw =
                            new java.io.PrintWriter(
                                new java.io.OutputStreamWriter(
                                    new java.io.FileOutputStream(
                                            LOG_PATH.toFile(),
                                            true),
                                    java.nio.charset.StandardCharsets.UTF_8))) {

                        pw.println(
                                "["
                                + new java.text.SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss.SSS")
                                    .format(new java.util.Date())
                                + "] "
                                + message);
                    }

                } catch (IOException ignored) {
                    System.out.println(message);
                }
            }

            @Override
            public void logE(Exception e) {
                try {
                    Files.createDirectories(LOG_PATH.getParent());

                    try (java.io.PrintWriter pw =
                            new java.io.PrintWriter(
                                new java.io.OutputStreamWriter(
                                    new java.io.FileOutputStream(
                                            LOG_PATH.toFile(),
                                            true),
                                    java.nio.charset.StandardCharsets.UTF_8))) {

                        e.printStackTrace(pw);
                    }

                } catch (IOException ignored) {
                    e.printStackTrace();
                }
            }
        };
    }
}
