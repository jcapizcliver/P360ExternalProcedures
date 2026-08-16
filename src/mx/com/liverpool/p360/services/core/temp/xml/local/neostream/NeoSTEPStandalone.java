package mx.com.liverpool.p360.services.core.temp.xml.local.neostream;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Standalone runner for the NEO STEP pipeline.
 *
 * Modes:
 *
 *   replay <file-or-directory> [--no-media]
 *       Processes existing XML files and leaves them where they are.
 *       Directories are walked recursively.
 *
 *   drain <landing-directory> <processed-base-directory> [--no-media]
 *       Processes the XML files currently present directly in landing-directory.
 *       Successful files are moved to processed-base-directory/yyyyMMdd/.
 *       Failed files remain in landing-directory.
 *
 *   watch <landing-directory> <processed-base-directory> [--no-media]
 *         [--poll-ms=N] [--settle-ms=N] [--retry-ms=N]
 *       Continuously scans landing-directory. Only .xml files are considered;
 *       .part files are ignored. A file must remain size/mtime-stable for the
 *       settle interval before it is processed. Successful files are archived
 *       by processing day. Failed files remain in landing and are retried after
 *       retry-ms, or immediately if their size/mtime changes.
 *
 * This class never reads the complete XML into memory. It only orchestrates
 * Path objects and delegates the actual streaming work to NeoSTEPFileProcessor.
 */
public final class NeoSTEPStandalone {

    private static final Path LOG_PATH =
            Path.of("..", "logs", "neo_step_standalone.log");

    private static final long DEFAULT_POLL_MS = 2_000L;
    private static final long DEFAULT_SETTLE_MS = 2_000L;
    private static final long DEFAULT_RETRY_MS = 60_000L;

    private NeoSTEPStandalone() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            usage();
            System.exit(1);
            return;
        }

        String mode = args[0].toLowerCase(Locale.ROOT);
        boolean includeMedia = !hasFlag(args, "--no-media");

        switch (mode) {
            case "replay": {
                Path input = Path.of(args[1]);
                RunSummary summary = replay(input, includeMedia);
                printSummary("replay", summary);
                if (summary.failed > 0) {
                    System.exit(2);
                }
                return;
            }

            case "drain": {
                if (args.length < 3) {
                    usage();
                    System.exit(1);
                    return;
                }
                Path landing = Path.of(args[1]);
                Path processedBase = Path.of(args[2]);
                RunSummary summary = drain(landing, processedBase, includeMedia);
                printSummary("drain", summary);
                if (summary.failed > 0) {
                    System.exit(2);
                }
                return;
            }

            case "watch": {
                if (args.length < 3) {
                    usage();
                    System.exit(1);
                    return;
                }
                Path landing = Path.of(args[1]);
                Path processedBase = Path.of(args[2]);

                long pollMs = longOption(args, "--poll-ms=", DEFAULT_POLL_MS);
                long settleMs = longOption(args, "--settle-ms=", DEFAULT_SETTLE_MS);
                long retryMs = longOption(args, "--retry-ms=", DEFAULT_RETRY_MS);

                watch(
                        landing,
                        processedBase,
                        includeMedia,
                        pollMs,
                        settleMs,
                        retryMs);
                return;
            }

            default:
                usage();
                System.exit(1);
        }
    }

    /**
     * Reprocesses a single file or all XML files below a directory recursively.
     * Source files are never moved or deleted.
     */
    public static RunSummary replay(Path input, boolean includeMedia) throws IOException {
        if (input == null || !Files.exists(input)) {
            throw new IOException("Input does not exist: " + input);
        }

        List<Path> files = new ArrayList<>();

        if (Files.isRegularFile(input)) {
            if (isXml(input)) {
                files.add(input);
            }
        } else {
            try (Stream<Path> stream = Files.walk(input)) {
                stream
                    .filter(Files::isRegularFile)
                    .filter(NeoSTEPStandalone::isXml)
                    .sorted(fileOrder())
                    .forEach(files::add);
            }
        }

        log("REPLAY start input=" + input + " files=" + files.size()
                + " includeMedia=" + includeMedia);

        RunSummary summary = new RunSummary();
        for (Path file : files) {
            summary.discovered++;
            if (processOne(file, includeMedia)) {
                summary.processed++;
            } else {
                summary.failed++;
            }
        }

        return summary;
    }

    /**
     * One-shot queue drain. Only files directly inside landing are consumed.
     */
    public static RunSummary drain(
            Path landing,
            Path processedBase,
            boolean includeMedia) throws IOException {

        validateQueueDirectories(landing, processedBase);

        RunSummary summary = new RunSummary();

        // Recover successful files whose archive move failed in an earlier run.
        archivePendingDoneFiles(landing, processedBase, summary);

        List<Path> files = listLandingXmlFiles(landing);

        log("DRAIN start landing=" + landing + " files=" + files.size()
                + " includeMedia=" + includeMedia);

        for (Path file : files) {
            summary.discovered++;

            if (!processOne(file, includeMedia)) {
                summary.failed++;
                continue;
            }

            summary.processed++;

            try {
                Path done = markProcessed(file);
                Path archived = archiveDoneFile(done, processedBase);
                summary.archived++;
                log("ARCHIVED source=" + file + " target=" + archived);
            } catch (Exception e) {
                summary.failed++;
                logException("Processing succeeded but marking/archive failed for " + file, e);
            }
        }

        return summary;
    }

    /**
     * Long-running landing-folder consumer.
     */
    public static void watch(
            Path landing,
            Path processedBase,
            boolean includeMedia,
            long pollMs,
            long settleMs,
            long retryMs) throws Exception {

        validateQueueDirectories(landing, processedBase);

        pollMs = Math.max(250L, pollMs);
        settleMs = Math.max(0L, settleMs);
        retryMs = Math.max(1_000L, retryMs);

        AtomicBoolean running = new AtomicBoolean(true);
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> running.set(false),
                "neo-step-standalone-shutdown"));

        Map<Path, ObservedFile> observed = new HashMap<>();
        Map<Path, FailedFile> failed = new HashMap<>();

        log("WATCH start landing=" + landing
                + " processedBase=" + processedBase
                + " includeMedia=" + includeMedia
                + " pollMs=" + pollMs
                + " settleMs=" + settleMs
                + " retryMs=" + retryMs);

        while (running.get()) {
            long now = System.currentTimeMillis();

            // A .done file was already processed successfully. Never process it again;
            // only retry the archive move.
            archivePendingDoneFiles(landing, processedBase, null);

            List<Path> files = listLandingXmlFiles(landing);

            // Forget state for files that left the landing directory.
            java.util.Set<Path> current = new java.util.HashSet<>(files);
            observed.keySet().removeIf(path -> !current.contains(path));
            failed.keySet().removeIf(path -> !current.contains(path));

            for (Path file : files) {
                if (!running.get()) {
                    break;
                }

                FileStamp stamp;
                try {
                    stamp = stamp(file);
                } catch (IOException e) {
                    logException("Could not stat landing file " + file, e);
                    continue;
                }

                ObservedFile previous = observed.get(file);
                if (previous == null || !previous.stamp.equals(stamp)) {
                    observed.put(file, new ObservedFile(stamp, now));
                    failed.remove(file); // changed file is immediately eligible again after settle.
                    continue;
                }

                if ((now - previous.stableSince) < settleMs) {
                    continue;
                }

                FailedFile failure = failed.get(file);
                if (failure != null
                        && failure.stamp.equals(stamp)
                        && now < failure.nextRetryAt) {
                    continue;
                }

                if (!processOne(file, includeMedia)) {
                    failed.put(file, new FailedFile(stamp, now + retryMs));
                    continue;
                }

                try {
                    Path done = markProcessed(file);
                    observed.remove(file);
                    failed.remove(file);

                    try {
                        Path archived = archiveDoneFile(done, processedBase);
                        log("ARCHIVED source=" + file + " target=" + archived);
                    } catch (Exception archiveFailure) {
                        // The .done file stays in landing and is archive-only on the next loop.
                        logException("Processing succeeded; pending archive for " + done, archiveFailure);
                    }
                } catch (Exception markFailure) {
                    // Extremely unusual: processing succeeded but we could not rename to .done.
                    // Keep a long cooldown so the current process does not immediately duplicate writes.
                    failed.put(file, new FailedFile(stamp, now + retryMs));
                    logException("Processing succeeded but could not mark .done for " + file, markFailure);
                }
            }

            if (!running.get()) {
                break;
            }

            try {
                Thread.sleep(pollMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running.set(false);
            }
        }

        log("WATCH stopped landing=" + landing);
    }

    private static boolean processOne(Path file, boolean includeMedia) {
        long start = System.currentTimeMillis();
        log("PROCESS start file=" + file + " bytes=" + safeSize(file));

        try {
            int products;
            if (includeMedia) {
                products = NeoSTEPFileProcessor.process(file);
            } else {
                products = NeoSTEPFileProcessor.processP360DataOnly(file);
            }

            log("PROCESS ok file=" + file
                    + " products=" + products
                    + " elapsedMs=" + (System.currentTimeMillis() - start));
            return true;

        } catch (Exception e) {
            logException("PROCESS failed file=" + file
                    + " elapsedMs=" + (System.currentTimeMillis() - start), e);
            return false;
        }
    }

    private static List<Path> listLandingXmlFiles(Path landing) throws IOException {
        List<Path> result = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(landing)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path) && isXml(path)) {
                    result.add(path);
                }
            }
        }

        result.sort(fileOrder());
        return result;
    }

    private static Comparator<Path> fileOrder() {
        return Comparator
                .comparingLong(NeoSTEPStandalone::safeLastModified)
                .thenComparing(path -> path.toAbsolutePath().normalize().toString());
    }

    private static boolean isXml(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        return path.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT)
                .endsWith(".xml");
    }

    private static void validateQueueDirectories(
            Path landing,
            Path processedBase) throws IOException {

        if (landing == null) {
            throw new IOException("Landing directory is null");
        }
        if (processedBase == null) {
            throw new IOException("Processed base directory is null");
        }

        Files.createDirectories(landing);
        Files.createDirectories(processedBase);

        if (!Files.isDirectory(landing)) {
            throw new IOException("Landing is not a directory: " + landing);
        }
        if (!Files.isDirectory(processedBase)) {
            throw new IOException("Processed base is not a directory: " + processedBase);
        }
    }

    private static Path markProcessed(Path source) throws IOException {
        Path done = source.resolveSibling(source.getFileName().toString() + ".done");

        try {
            return Files.move(source, done, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            return Files.move(source, done);
        }
    }

    private static void archivePendingDoneFiles(
            Path landing,
            Path processedBase,
            RunSummary summary) {

        List<Path> doneFiles;
        try {
            doneFiles = listLandingDoneFiles(landing);
        } catch (IOException e) {
            logException("Could not list pending .done files in " + landing, e);
            return;
        }

        for (Path done : doneFiles) {
            try {
                Path archived = archiveDoneFile(done, processedBase);
                if (summary != null) {
                    summary.archived++;
                }
                log("ARCHIVED pending done=" + done + " target=" + archived);
            } catch (Exception e) {
                logException("Could not archive already-processed file " + done, e);
            }
        }
    }

    private static List<Path> listLandingDoneFiles(Path landing) throws IOException {
        List<Path> result = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(landing)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)
                        && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xml.done")) {
                    result.add(path);
                }
            }
        }

        result.sort(fileOrder());
        return result;
    }

    private static Path archiveDoneFile(
            Path doneSource,
            Path processedBase) throws IOException {

        String day = new SimpleDateFormat("yyyyMMdd").format(new Date());
        Path dayDirectory = processedBase.resolve(day);
        Files.createDirectories(dayDirectory);

        String doneName = doneSource.getFileName().toString();
        String originalName = doneName.endsWith(".done")
                ? doneName.substring(0, doneName.length() - ".done".length())
                : doneName;

        Path target = uniqueTarget(dayDirectory, originalName);

        try {
            return Files.move(doneSource, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            return moveNonAtomic(doneSource, target);
        } catch (IOException e) {
            return copyDeleteFallback(doneSource, target, e);
        }
    }

    private static Path moveNonAtomic(Path source, Path target) throws IOException {
        try {
            return Files.move(source, target);
        } catch (IOException moveFailure) {
            return copyDeleteFallback(source, target, moveFailure);
        }
    }

    private static Path copyDeleteFallback(
            Path source,
            Path target,
            IOException moveFailure) throws IOException {

        try {
            Files.copy(source, target);
            Files.delete(source);
            return target;
        } catch (IOException copyFailure) {
            copyFailure.addSuppressed(moveFailure);
            try {
                Files.deleteIfExists(target);
            } catch (IOException cleanupFailure) {
                copyFailure.addSuppressed(cleanupFailure);
            }
            throw copyFailure;
        }
    }

    private static Path uniqueTarget(Path directory, String fileName) {
        Path target = directory.resolve(fileName);
        if (!Files.exists(target)) {
            return target;
        }

        String base = fileName;
        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            base = fileName.substring(0, dot);
            extension = fileName.substring(dot);
        }

        String stamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        int counter = 0;
        do {
            String suffix = "__" + stamp + (counter == 0 ? "" : "_" + counter);
            target = directory.resolve(base + suffix + extension);
            counter++;
        } while (Files.exists(target));

        return target;
    }

    private static FileStamp stamp(Path path) throws IOException {
        return new FileStamp(
                Files.size(path),
                Files.getLastModifiedTime(path));
    }

    private static long safeSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1L;
        }
    }

    private static long safeLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return Long.MAX_VALUE;
        }
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (flag.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }

    private static long longOption(String[] args, String prefix, long defaultValue) {
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                String raw = arg.substring(prefix.length());
                try {
                    return Long.parseLong(raw);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid option: " + arg, e);
                }
            }
        }
        return defaultValue;
    }

    private static void printSummary(String mode, RunSummary summary) {
        String message = mode.toUpperCase(Locale.ROOT)
                + " summary: discovered=" + summary.discovered
                + " processed=" + summary.processed
                + " archived=" + summary.archived
                + " failed=" + summary.failed;
        System.out.println(message);
        log(message);
    }

    private static void usage() {
        System.err.println("Usage:");
        System.err.println("  NeoSTEPStandalone replay <file-or-directory> [--no-media]");
        System.err.println("  NeoSTEPStandalone drain <landing-directory> <processed-base-directory> [--no-media]");
        System.err.println("  NeoSTEPStandalone watch <landing-directory> <processed-base-directory> [--no-media] [--poll-ms=N] [--settle-ms=N] [--retry-ms=N]");
    }

    private static synchronized void log(String message) {
        String line = "["
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())
                + "] " + message;

        System.out.println(line);

        try {
            Files.createDirectories(LOG_PATH.getParent());
            try (java.io.BufferedWriter writer = Files.newBufferedWriter(
                    LOG_PATH,
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND)) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException ignored) {
        }
    }

    private static void logException(String message, Exception e) {
        log(message + " error=" + e.getClass().getName()
                + ": " + String.valueOf(e.getMessage()));

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

    public static final class RunSummary {
        public int discovered;
        public int processed;
        public int archived;
        public int failed;
    }

    private static final class FileStamp {
        final long size;
        final FileTime lastModified;

        FileStamp(long size, FileTime lastModified) {
            this.size = size;
            this.lastModified = lastModified;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof FileStamp)) return false;
            FileStamp other = (FileStamp) object;
            return size == other.size
                    && java.util.Objects.equals(lastModified, other.lastModified);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(size, lastModified);
        }
    }

    private static final class ObservedFile {
        final FileStamp stamp;
        final long stableSince;

        ObservedFile(FileStamp stamp, long stableSince) {
            this.stamp = stamp;
            this.stableSince = stableSince;
        }
    }

    private static final class FailedFile {
        final FileStamp stamp;
        final long nextRetryAt;

        FailedFile(FileStamp stamp, long nextRetryAt) {
            this.stamp = stamp;
            this.nextRetryAt = nextRetryAt;
        }
    }
}
