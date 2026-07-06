package com.example.ei.forfun.logic;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class P360IncidentWatcherV2 {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final String DEFAULT_HMAC_SECRET = "\u006d\u0069\u0020\u0063\u002a\u0063\u0068\u0074\u0061\u0020\u0068\u0065\u0072\u006d\u006f\u0073\u0061\u0020\u0063\u0067\u0070\u0074";
    private static final String HMAC_ENV_NAME = "P360_INCIDENT_HMAC_SECRET";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final Config config;
    private volatile LocalDateTime lastTriggerTime;

    public P360IncidentWatcherV2(Config config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public static void main(String[] args) throws Exception {
        Path propertiesPath = args != null && args.length > 0
                ? Paths.get(args[0])
                : Paths.get("/u01/workshop/p360-incident-watcher.properties");

        Config config = Config.load(propertiesPath);
        new P360IncidentWatcherV2(config).run();
    }

    public void run() throws Exception {
        Files.createDirectories(config.outputBaseDir);
        log("Watching " + config.consoleLogPath);

        Deque<String> recentLines = new ArrayDeque<>(config.consoleContextLines);
        long pointer = 0L;

        while (true) {
            if (!Files.exists(config.consoleLogPath)) {
                sleep(2000);
                continue;
            }

            try (RandomAccessFile raf = new RandomAccessFile(config.consoleLogPath.toFile(), "r")) {
                long fileLength = raf.length();
                if (pointer > fileLength) {
                    pointer = 0L;
                }
                raf.seek(pointer);

                while (true) {
                    String rawLine = raf.readLine();
                    if (rawLine == null) {
                        pointer = raf.getFilePointer();
                        sleep(1000);
                        continue;
                    }

                    String line = new String(rawLine.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                    pushRecentLine(recentLines, line);

                    if (matchesTrigger(line) && !inCooldown()) {
                        lastTriggerTime = LocalDateTime.now();
                        triggerCapture(line, new ArrayList<>(recentLines));
                    }
                }
            } catch (Exception e) {
                log("Watcher loop failed: " + e.getMessage());
                e.printStackTrace(System.err);
                sleep(3000);
            }
        }
    }

    private void triggerCapture(String triggerLine, List<String> recentLines) {
        String stamp = LocalDateTime.now().format(TS);
        Path incidentDir = config.outputBaseDir.resolve("incident_" + stamp);

        try {
            Files.createDirectories(incidentDir);
            Files.writeString(incidentDir.resolve("trigger.txt"), triggerLine + System.lineSeparator(), StandardCharsets.UTF_8);
            Files.write(incidentDir.resolve("console_context_before.txt"), recentLines, StandardCharsets.UTF_8);
            copyIfExists(config.consoleLogPath, incidentDir.resolve("console.log.snapshot"));
            copyIfExists(config.udaLogPath, incidentDir.resolve("uda.log.snapshot"));
            writeMetadata(incidentDir, triggerLine);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        log("Trigger detected. Capturing evidence into " + incidentDir);

        safeRun(() -> sendGoogleChatSimple(
                config.googleChatWebhookUrl,
                "🚨 P360 trigger detected\n"
                        + "Time: " + LocalDateTime.now() + "\n"
                        + "Trigger: " + triggerLine + "\n"
                        + "Incident dir: " + incidentDir
        ));

        ExecutorService executor = Executors.newFixedThreadPool(6);

        List<CompletableFuture<Void>> tasks = List.of(
                CompletableFuture.runAsync(() -> safeRun(() -> captureDatabase(incidentDir)), executor),
                CompletableFuture.runAsync(() -> safeRun(() -> captureThreadDumps(incidentDir)), executor),
                CompletableFuture.runAsync(() -> safeRun(() -> captureJmx(incidentDir)), executor),
                CompletableFuture.runAsync(() -> safeRun(() -> captureUdaTail(incidentDir)), executor),
                CompletableFuture.runAsync(() -> safeRun(() -> captureOsSnapshot(incidentDir)), executor),
                CompletableFuture.runAsync(() -> safeRun(() -> captureConsoleTail(incidentDir)), executor)
        );

        CompletableFuture
                .allOf(tasks.toArray(new CompletableFuture[0]))
                .whenComplete((ok, ex) -> {
                    try {
                        createManifestAndSignature(incidentDir);
                        Path zip = zipIncidentDirectory(incidentDir);
                        signZip(zip);
                        String pid = readPid();
                        sendGoogleChatWebhook(incidentDir, triggerLine, pid, zip);
			P360ServiceRestartor.main(new String[]{});
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    } finally {
                        executor.shutdown();
                        try {
                            executor.awaitTermination(2, TimeUnit.MINUTES);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        log("Capture finished for " + incidentDir);
                    }
                });
    }

    private void writeMetadata(Path incidentDir, String triggerLine) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("timestamp=" + LocalDateTime.now());
        lines.add("console.log.path=" + config.consoleLogPath);
        lines.add("pid.file.path=" + config.pidFilePath);
        lines.add("uda.log.path=" + config.udaLogPath);
        lines.add("jmx.host=" + config.jmxHost);
        lines.add("jmx.port=" + config.jmxPort);
        lines.add("thread.dumps.count=" + config.threadDumpCount);
        lines.add("thread.dumps.interval.seconds=" + config.threadDumpIntervalSeconds);
        lines.add("uda.capture.seconds=" + config.udaCaptureSeconds);
        lines.add("cooldown.minutes=" + config.cooldownMinutes);
        lines.add("trigger.line=" + triggerLine);
        Files.write(incidentDir.resolve("metadata.properties"), lines, StandardCharsets.UTF_8);
    }

    private void captureDatabase(Path incidentDir) throws Exception {
        String user = requireEnv("ORACLE_JDBC_USER");
        String password = requireEnv("ORACLE_JDBC_PASSWORD");
        String url = requireEnv("ORACLE_JDBC_URL");

        List<String> queries = List.of(
                "select inst_id,sid,serial#,username,status,event,wait_class,seconds_in_wait,last_call_et,sql_id,prev_sql_id,module,machine from gv$session where username in ('PIM_MAIN','PIM_MASTER','PIM_SUPPLIER') order by status desc,last_call_et desc",
                "select username,status,event,module,count(*) from gv$session where username in ('PIM_MAIN','PIM_MASTER','PIM_SUPPLIER') group by username,status,event,module order by count(*) desc",
                "select inst_id,sql_id,child_number,plan_hash_value,parsing_schema_name,executions,round(elapsed_time/1000000,2) elapsed_s,round(case when executions>0 then elapsed_time/executions/1000000 else null end,2) avg_elapsed_s,buffer_gets,disk_reads,rows_processed,last_active_time from gv$sql where last_active_time>=sysdate-(15/1440) and parsing_schema_name in ('PIM_MAIN','PIM_MASTER','PIM_SUPPLIER') order by elapsed_time desc fetch first 50 rows only",
                "select inst_id,sid,serial#,username,opname,target,sofar,totalwork,units,start_time,last_update_time,time_remaining,elapsed_seconds,message,sql_id from gv$session_longops where sofar<totalwork and totalwork>0 order by start_time desc",
                "select s.inst_id,s.sid,s.serial#,s.username,s.module,u.tablespace,u.segtype,round(u.blocks*t.block_size/1024/1024,2) mb,u.sql_id from gv$sort_usage u join gv$session s on s.saddr=u.session_addr and s.inst_id=u.inst_id join dba_tablespaces t on t.tablespace_name=u.tablespace order by mb desc",
                "select inst_id,sid,serial#,username,opname,target,sofar,totalwork,units,start_time,last_update_time,time_remaining,elapsed_seconds,message,sql_id from gv$session_longops where sql_id='5x81tu50fy93v' order by last_update_time desc"
        );

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            int index = 1;
            for (String sql : queries) {
                Path out = incidentDir.resolve(String.format("db_query_%02d.csv", index));
                runQueryToCsv(connection, sql, out);
                Files.writeString(
                        incidentDir.resolve(String.format("db_query_%02d.sql", index)),
                        sql + System.lineSeparator(),
                        StandardCharsets.UTF_8
                );
                index++;
            }
        }
    }

    private void runQueryToCsv(Connection connection, String sql, Path out) throws Exception {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql);
             BufferedWriter writer = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {

            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();

            for (int i = 1; i <= cols; i++) {
                if (i > 1) {
                    writer.write(';');
                }
                writer.write(md.getColumnLabel(i));
            }
            writer.newLine();

            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) {
                        writer.write(';');
                    }
                    Object value = rs.getObject(i);
                    writer.write(escapeCsv(value));
                }
                writer.newLine();
            }
        }
    }

    private void captureThreadDumps(Path incidentDir) throws Exception {
        String pid = readPid();
        Path out = incidentDir.resolve("thread_dumps.log");

        for (int i = 1; i <= config.threadDumpCount; i++) {
            appendLine(out, "===== " + LocalDateTime.now() + " dump " + i + " =====");
            ProcessBuilder pb = new ProcessBuilder("jcmd", pid, "Thread.print", "-l");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            appendStream(out, p.getInputStream());
            int rc = p.waitFor();

            if (rc != 0) {
                appendLine(out, "jcmd returned code " + rc);
            }

            if (i < config.threadDumpCount) {
                sleep(config.threadDumpIntervalSeconds * 1000L);
            }
        }
    }

    private void captureJmx(Path incidentDir) throws Exception {
        Path commandsFile = incidentDir.resolve("jmx_commands.txt");
        Path out = incidentDir.resolve("jmx.log");

        List<String> commands = List.of(
                "open " + config.jmxHost + ":" + config.jmxPort,
                "bean com.zaxxer.hikari:type=Pool ([Hibernate] PIM_MAIN)",
                "get ActiveConnections",
                "get IdleConnections",
                "get TotalConnections",
                "get ThreadsAwaitingConnection",
                "bean com.zaxxer.hikari:type=Pool ([Hibernate] PIM_MASTER)",
                "get ActiveConnections",
                "get IdleConnections",
                "get TotalConnections",
                "get ThreadsAwaitingConnection",
                "bean com.heiler.ppm:name=HttpThreadPool,type=HttpThreadPool",
                "get *",
                "bean com.heiler.ppm:name=ObjectAPI,type=threadPools",
                "get *",
                "bean com.heiler.ppm:name=CommunicationWorker,type=threadPools",
                "get *",
                "bean com.heiler.ppm:name=ReportStoreExecutor,type=threadPools",
                "get *",
                "bye"
        );

        Files.write(commandsFile, commands, StandardCharsets.UTF_8);

        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                config.jmxtermJarPath.toString(),
                "-l",
                config.jmxHost + ":" + config.jmxPort,
                "-n",
                "-i",
                commandsFile.toString()
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        appendStream(out, p.getInputStream());
        int rc = p.waitFor();

        if (rc != 0) {
            appendLine(out, "jmxterm returned code " + rc);
        }
    }

    private void captureUdaTail(Path incidentDir) throws Exception {
        Path out = incidentDir.resolve("uda_follow.log");
        long deadline = System.currentTimeMillis() + (config.udaCaptureSeconds * 1000L);

        if (!Files.exists(config.udaLogPath)) {
            appendLine(out, "UDA log does not exist: " + config.udaLogPath);
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(config.udaLogPath.toFile(), "r")) {
            long pointer = 0L;

            while (System.currentTimeMillis() < deadline) {
                long fileLength = raf.length();
                if (pointer > fileLength) {
                    pointer = 0L;
                }
                raf.seek(pointer);

                String rawLine;
                boolean readSomething = false;

                while ((rawLine = raf.readLine()) != null) {
                    String line = new String(rawLine.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                    appendLine(out, line);
                    readSomething = true;
                }

                pointer = raf.getFilePointer();

                if (!readSomething) {
                    sleep(1000);
                }
            }
        }
    }

    private void captureConsoleTail(Path incidentDir) throws Exception {
        Path out = incidentDir.resolve("console_tail_after_trigger.log");
        long deadline = System.currentTimeMillis() + (config.consoleTailSeconds * 1000L);

        if (!Files.exists(config.consoleLogPath)) {
            appendLine(out, "Console log does not exist: " + config.consoleLogPath);
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(config.consoleLogPath.toFile(), "r")) {
            long pointer = raf.length();

            while (System.currentTimeMillis() < deadline) {
                raf.seek(pointer);
                String rawLine;
                boolean readSomething = false;

                while ((rawLine = raf.readLine()) != null) {
                    String line = new String(rawLine.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                    appendLine(out, line);
                    readSomething = true;
                }

                pointer = raf.getFilePointer();

                if (!readSomething) {
                    sleep(1000);
                }
            }
        }
    }

    private void captureOsSnapshot(Path incidentDir) throws Exception {
        Path out = incidentDir.resolve("os_snapshot.log");

        List<List<String>> commands = List.of(
                List.of("bash", "-lc", "date"),
                List.of("bash", "-lc", "uptime"),
                List.of("bash", "-lc", "vmstat 1 5"),
                List.of("bash", "-lc", "free -m"),
                List.of("bash", "-lc", "ss -s"),
                List.of("bash", "-lc", "ss -tan state time-wait | wc -l"),
                List.of("bash", "-lc", "ps -eLo pid,tid,pcpu,pmem,state,comm --sort=-pcpu | head -80")
        );

        for (List<String> cmd : commands) {
            appendLine(out, "===== " + String.join(" ", cmd) + " =====");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            appendStream(out, p.getInputStream());
            p.waitFor();
            appendLine(out, "");
        }
    }

    private void createManifestAndSignature(Path incidentDir) throws Exception {
        Path manifest = incidentDir.resolve("manifest.sha256");
        List<Path> files = listFilesRecursively(incidentDir);
        files.sort(Comparator.naturalOrder());

        List<String> lines = new ArrayList<>();
        for (Path file : files) {
            if (Files.isDirectory(file)) {
                continue;
            }
            Path relative = incidentDir.relativize(file);
            if (relative.toString().equals("manifest.sha256") || relative.toString().equals("manifest.hmac")) {
                continue;
            }
            String sha = sha256Hex(file);
            lines.add(sha + "  " + relative);
        }

        Files.write(manifest, lines, StandardCharsets.UTF_8);

        String manifestHmac = hmacSha256Hex(getSecret(), Files.readAllBytes(manifest));
        Files.writeString(
                incidentDir.resolve("manifest.hmac"),
                manifestHmac + "  manifest.sha256" + System.lineSeparator(),
                StandardCharsets.UTF_8
        );
    }

    private Path zipIncidentDirectory(Path incidentDir) throws Exception {
        Path zipPath = incidentDir.resolveSibling(incidentDir.getFileName().toString() + ".zip");

        try (OutputStream fos = Files.newOutputStream(zipPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {

            List<Path> files = listFilesRecursively(incidentDir);
            files.sort(Comparator.naturalOrder());

            for (Path file : files) {
                if (Files.isDirectory(file)) {
                    continue;
                }

                Path relative = incidentDir.relativize(file);
                ZipEntry entry = new ZipEntry(relative.toString().replace("\\", "/"));
                zos.putNextEntry(entry);
                Files.copy(file, zos);
                zos.closeEntry();
            }
        }

        return zipPath;
    }

    private void signZip(Path zipPath) throws Exception {
        byte[] zipBytes = Files.readAllBytes(zipPath);
        String hmac = hmacSha256Hex(getSecret(), zipBytes);
        Path sigPath = zipPath.resolveSibling(zipPath.getFileName().toString() + ".hmac");
        Files.writeString(sigPath, hmac + "  " + zipPath.getFileName() + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private void sendGoogleChatWebhook(Path incidentDir, String triggerLine, String pid, Path zipPath) throws Exception {
        if (config.googleChatWebhookUrl == null || config.googleChatWebhookUrl.isBlank()) {
            appendLine(incidentDir.resolve("google_chat_webhook.log"), "Webhook URL is blank, skipping notification");
            return;
        }

        String message = ""
                + "🚨 P360 incident trigger detected\n"
                + "Time: " + LocalDateTime.now() + "\n"
                + "PID: " + pid + "\n"
                + "Trigger: " + triggerLine + "\n"
                + "Incident dir: " + incidentDir + "\n"
                + "Zip: " + zipPath + "\n"
                + "Actions: DB queries, thread dumps, JMX, UDA capture, OS snapshot";

        String json = "{\"text\":\"" + escapeJson(message) + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.googleChatWebhookUrl))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        Files.writeString(
                incidentDir.resolve("google_chat_webhook.log"),
                "HTTP " + response.statusCode() + System.lineSeparator()
                        + response.body() + System.lineSeparator(),
                StandardCharsets.UTF_8,
                Files.exists(incidentDir.resolve("google_chat_webhook.log")) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Google Chat webhook failed: HTTP " + response.statusCode());
        }
    }

    private void sendGoogleChatSimple(String webhookUrl, String text) throws Exception {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        String json = "{\"text\":\"" + escapeJson(text) + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(15))
                .build();

        HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String getSecret() {
        String env = System.getenv(HMAC_ENV_NAME);
        return (env == null || env.isBlank()) ? DEFAULT_HMAC_SECRET : env;
    }

    private String readPid() throws IOException {
        return Files.readString(config.pidFilePath, StandardCharsets.UTF_8).trim();
    }

    private boolean matchesTrigger(String line) {

        String s = line.toLowerCase().replaceAll("\\s+", " ");

        boolean poolTimeout =
                (s.contains("unable to acquire connection within") && s.contains("will try again in"))
                || (s.contains("unable to obtain a connection within") && s.contains("will retry again in"));

        boolean shutdownSignal =
                s.contains("it is not possible to schedule new jobs, preparing shutdown of the servers");

        return poolTimeout || shutdownSignal;
    }

    private boolean inCooldown() {
        LocalDateTime now = LocalDateTime.now();
        return lastTriggerTime != null
                && Duration.between(lastTriggerTime, now).compareTo(Duration.ofMinutes(config.cooldownMinutes)) < 0;
    }

    private void pushRecentLine(Deque<String> buffer, String line) {
        if (buffer.size() >= config.consoleContextLines) {
            buffer.removeFirst();
        }
        buffer.addLast(line);
    }

    private static void copyIfExists(Path source, Path target) throws IOException {
        if (Files.exists(source)) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void appendLine(Path file, String line) throws IOException {
        Files.writeString(
                file,
                line + System.lineSeparator(),
                StandardCharsets.UTF_8,
                Files.exists(file) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE
        );
    }

    private static void appendStream(Path file, InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
             BufferedWriter writer = Files.newBufferedWriter(
                     file,
                     StandardCharsets.UTF_8,
                     Files.exists(file) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE
             )) {

            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private static List<Path> listFilesRecursively(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(root, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                files.add(file);
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    private static String sha256Hex(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return toHex(digest.digest());
    }

    private static String hmacSha256Hex(String secret, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(key);
        return toHex(mac.doFinal(data));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value).replace("\n", " ").replace("\r", " ");
        if (s.contains(";") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String escapeJson(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String requireEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing environment variable: " + name);
        }
        return value;
    }

    private static void safeRun(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void log(String s) {
        System.out.println(LocalDateTime.now() + " [P360IncidentWatcherV2] " + s);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    public static class Config {
        private final String googleChatWebhookUrl;
        private final Path consoleLogPath;
        private final Path pidFilePath;
        private final Path udaLogPath;
        private final Path outputBaseDir;
        private final Path jmxtermJarPath;
        private final String jmxHost;
        private final int jmxPort;
        private final int threadDumpCount;
        private final int threadDumpIntervalSeconds;
        private final int udaCaptureSeconds;
        private final int consoleContextLines;
        private final int consoleTailSeconds;
        private final int cooldownMinutes;
        private final List<String> triggerPatterns;

        private Config(
                String googleChatWebhookUrl,
                Path consoleLogPath,
                Path pidFilePath,
                Path udaLogPath,
                Path outputBaseDir,
                Path jmxtermJarPath,
                String jmxHost,
                int jmxPort,
                int threadDumpCount,
                int threadDumpIntervalSeconds,
                int udaCaptureSeconds,
                int consoleContextLines,
                int consoleTailSeconds,
                int cooldownMinutes,
                List<String> triggerPatterns
        ) {
            this.googleChatWebhookUrl = googleChatWebhookUrl;
            this.consoleLogPath = consoleLogPath;
            this.pidFilePath = pidFilePath;
            this.udaLogPath = udaLogPath;
            this.outputBaseDir = outputBaseDir;
            this.jmxtermJarPath = jmxtermJarPath;
            this.jmxHost = jmxHost;
            this.jmxPort = jmxPort;
            this.threadDumpCount = threadDumpCount;
            this.threadDumpIntervalSeconds = threadDumpIntervalSeconds;
            this.udaCaptureSeconds = udaCaptureSeconds;
            this.consoleContextLines = consoleContextLines;
            this.consoleTailSeconds = consoleTailSeconds;
            this.cooldownMinutes = cooldownMinutes;
            this.triggerPatterns = triggerPatterns;
        }

        public static Config load(Path propertiesPath) throws IOException {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(propertiesPath)) {
                p.load(in);
            }

            List<String> triggers = new ArrayList<>();
            int i = 1;
            while (true) {
                String v = p.getProperty("trigger.pattern." + i);
                if (v == null || v.isBlank()) {
                    break;
                }
                triggers.add(v);
                i++;
            }

            if (triggers.isEmpty()) {
                triggers = Arrays.asList(
                        "Unable to acquire connection within 30000ms",
                        "IncrementPoolOnTimeoutWithRetryConnectionAcquiringStrategy"
                );
            }

            return new Config(
                    p.getProperty("google.chat.webhook.url", "").trim(),
                    Paths.get(required(p, "console.log.path")),
                    Paths.get(required(p, "pid.file.path")),
                    Paths.get(required(p, "uda.log.path")),
                    Paths.get(required(p, "output.base.dir")),
                    Paths.get(required(p, "jmxterm.jar.path")),
                    p.getProperty("jmx.host", "localhost"),
                    Integer.parseInt(p.getProperty("jmx.port", "55555")),
                    Integer.parseInt(p.getProperty("thread.dumps.count", "10")),
                    Integer.parseInt(p.getProperty("thread.dumps.interval.seconds", "5")),
                    Integer.parseInt(p.getProperty("uda.capture.seconds", "600")),
                    Integer.parseInt(p.getProperty("console.context.lines", "300")),
                    Integer.parseInt(p.getProperty("console.tail.seconds", "120")),
                    Integer.parseInt(p.getProperty("cooldown.minutes", "10")),
                    triggers
            );
        }

        private static String required(Properties p, String key) {
            String value = p.getProperty(key);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Missing property: " + key);
            }
            return value.trim();
        }
    }
}
