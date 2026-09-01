package mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;

/**
 * Entry point for STEP data processing.
 *
 * Design:
 *  1) very cheap SAX index pass: IDs/SKUs/AttributeIDs only;
 *  2) one DBAccessDataStub / one JDBC connection for the whole STEP;
 *  3) streaming second-opinion pass, then flush its identity/collision changes;
 *  4) one streaming fan-out pass for ALL normal writers;
 *  5) structure associations are isolated and sent in strict priority order.
 *
 * There are intentionally two full Product passes instead of one: normal writes
 * must not race ahead of ProductNo/SupplierAID collision corrections. Parsing the
 * XML again is cheap compared with buffering the entire normal-write workload or
 * interleaving unsafe REST writes.
 */
public final class AnotherXMLHandlerNeoProcessPipeline {

    private static final RESTWorkshop TIME = new RESTWorkshop();
    private static final Path LOG_PATH = Path.of("..", "logs", "neo_step_pipeline.log");

    private AnotherXMLHandlerNeoProcessPipeline() {
    }

    public static int processPath(Path path)
            throws ParserConfigurationException, SAXException, IOException {

        long totalStart = System.currentTimeMillis();
        ELog log = logger();
        log.log("START STEP: " + path);

        long t = System.currentTimeMillis();
        StepXmlStreamingParser.StepIndex index = StepXmlStreamingParser.index(path);
        log.log("Index pass: " + TIME.formatTime(System.currentTimeMillis() - t)
                + " products=" + index.getProductIds().size()
                + " productSKUs=" + index.getProductSkus().size()
                + " articles=" + index.getArticleIds().size()
                + " articleSKUs=" + index.getArticleSkus().size());

        int parsedProducts;
        try (DBAccessDataStub db = new DBAccessDataStub(log)) {
            t = System.currentTimeMillis();
            StepDbSnapshot snapshot = StepDbSnapshot.load(db, index);
            log.log("Bulk DB snapshot: " + TIME.formatTime(System.currentTimeMillis() - t)
                    + " productBySku=" + snapshot.productBySku.size()
                    + " articleBySku=" + snapshot.articleBySku.size()
                    + " productData=" + snapshot.productData.size()
                    + " articleData=" + snapshot.articleData.size());

            StepStatusComputer statusComputer = new StepStatusComputer();
            Map<String, String> internalToExternalStatusMap = loadExternalStatusMap(log);

            StepSecondOpinionProcessor secondOpinion = new StepSecondOpinionProcessor(
                    snapshot,
                    statusComputer,
                    internalToExternalStatusMap,
                    log);

            t = System.currentTimeMillis();
            parsedProducts = StepXmlStreamingParser.parse(path, secondOpinion::accept);
            secondOpinion.finish();
            log.log("Second opinion streaming pass + flush: "
                    + TIME.formatTime(System.currentTimeMillis() - t));

            StepWriterPipeline writers = new StepWriterPipeline(db, index, log);
            t = System.currentTimeMillis();
            StepXmlStreamingParser.parse(path, writers::accept);
            writers.finishData();
            log.log("Normal writers streaming pass + data flush: "
                    + TIME.formatTime(System.currentTimeMillis() - t));

            t = System.currentTimeMillis();
            writers.finishStructures();
            log.log("Structure groups (Primary -> Web -> ECC -> S4H): "
                    + TIME.formatTime(System.currentTimeMillis() - t));
        }

        log.log("DONE STEP: " + path + " total="
                + TIME.formatTime(System.currentTimeMillis() - totalStart));
        return parsedProducts;
    }

    /**
     * Folder reprocessing without running every specialized processor over the
     * directory again. Each XML is fully processed exactly once by this pipeline.
     */
    public static void processDirectory(Path directory) throws Exception {
        try (java.util.stream.Stream<Path> stream = Files.walk(directory)) {
            java.util.List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".xml"))
                    .sorted()
                    .toList();
            for (Path file : files) {
                processPath(file);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: AnotherXMLHandlerNeoProcessPipeline <xml-file-or-directory>");
            return;
        }
        Path path = Path.of(args[0]);
        if (Files.isDirectory(path)) processDirectory(path);
        else processPath(path);
    }

    private static Map<String, String> loadExternalStatusMap(ELog log) {
        Map<String, String> result = new TreeMap<>();
        Path file = Path.of(
                PropertiesManager.get("p360.contingency.base_directory"),
                "cache", "templates", "dictionaries", "ExternalStatus");
        try (java.io.BufferedReader br = Files.newBufferedReader(file)) {
            String line;
            RESTWorkshop rw = new RESTWorkshop();
            while ((line = br.readLine()) != null) {
                String[] pieces = rw.parseLine(line, "\"", ";", "\\");
                if (pieces.length > 1) result.put(pieces[0], pieces[1]);
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
                    try (java.io.PrintWriter pw = new java.io.PrintWriter(
                            new java.io.OutputStreamWriter(
                                    new java.io.FileOutputStream(LOG_PATH.toFile(), true),
                                    java.nio.charset.StandardCharsets.UTF_8))) {
                        pw.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                                .format(new java.util.Date()) + "] " + message);
                    }
                } catch (IOException ignored) {
                    System.out.println(message);
                }
            }

            @Override
            public void logE(Exception e) {
                try {
                    Files.createDirectories(LOG_PATH.getParent());
                    try (java.io.PrintWriter pw = new java.io.PrintWriter(
                            new java.io.OutputStreamWriter(
                                    new java.io.FileOutputStream(LOG_PATH.toFile(), true),
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
