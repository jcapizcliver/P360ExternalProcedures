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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class P360ServiceRestartor {

    private static final String DEFAULT_SERVICE_NAME = "Product_360_10.5";
    private static final int DEFAULT_PORT = 1512;
    private static final int DEFAULT_DELAY_SECONDS = 0;
    private static final int DEFAULT_CHECK_INTERVAL_SECONDS = 3;
    private static final int DEFAULT_STOP_TIMEOUT_SECONDS = 1800;
    private static final Path DEFAULT_PID_FILE = Path.of("/u01/Informatica/PIM/server/logs/hpm_java.pid");

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void main(String[] args) throws Exception {
        Config config = Config.fromArgs(args);

        log("Config:");
        log("  serviceName = " + config.serviceName);
        log("  port = " + config.port);
        log("  delaySeconds = " + config.delaySeconds);
        log("  checkIntervalSeconds = " + config.checkIntervalSeconds);
        log("  stopTimeoutSeconds = " + config.stopTimeoutSeconds);
        log("  pidFile = " + config.pidFile);

        if (config.delaySeconds > 0) {
            log("Waiting " + config.delaySeconds + " seconds before restart...");
            sleepSeconds(config.delaySeconds);
        }

        stopService(config.serviceName, config);
        waitUntilStopped(config);
        startService(config.serviceName, config);

        log("Restart flow completed.");
    }

    private static void stopService(String serviceName, Config config) throws Exception {
        log("Stopping service " + serviceName + "...");
	sendGoogleChatWebhook("Tamos bajando el serber...", config);
        int exit = runCommand(List.of("service", serviceName, "stop"));
        if (exit != 0) {
            throw new IllegalStateException("Stop command failed with exit code " + exit);
        }
        log("Stop command finished.");
    }

    private static void startService(String serviceName, Config config) throws Exception {
        log("Starting service " + serviceName + "...");
        int exit = runCommand(List.of("service", serviceName, "start"));
        if (exit != 0) {
            throw new IllegalStateException("Start command failed with exit code " + exit);
        }
	sendGoogleChatWebhook("Tamos parando el server que estaba tirado, ya nomás hay que esperar.", config);
        log("Start command finished.");
    }

    private static void waitUntilStopped(Config config) throws Exception {
        long deadline = System.currentTimeMillis() + (config.stopTimeoutSeconds * 1000L);

        while (System.currentTimeMillis() < deadline) {
            boolean portOpen = isPortListening(config.port);
            Optional<Long> pid = readPid(config.pidFile);
            boolean pidAlive = pid.isPresent() && isPidAlive(pid.get());

            log("Waiting for shutdown... portListening=" + portOpen
                    + ", pid=" + pid.map(String::valueOf).orElse("N/A")
                    + ", pidAlive=" + pidAlive);

            if (!portOpen && !pidAlive) {
                log("Shutdown confirmed.");
                return;
            }

            sleepSeconds(config.checkIntervalSeconds);
        }

        throw new IllegalStateException("Timeout waiting for service to stop cleanly.");
    }

    private static boolean isPortListening(int port) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "bash",
                "-lc",
                "ss -ltn '( sport = :" + port + " )' | awk 'NR>1 {print $0}'"
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();

        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = p.waitFor();

        if (exit != 0) {
            throw new IllegalStateException("Failed checking port " + port + ", exit code " + exit);
        }

        return !output.trim().isEmpty();
    }

    private static Optional<Long> readPid(Path pidFile) {
        try {
            if (!Files.exists(pidFile)) {
                return Optional.empty();
            }

            String text = Files.readString(pidFile, StandardCharsets.UTF_8).trim();
            if (text.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(Long.parseLong(text));
        } catch (Exception e) {
            log("Could not read PID file " + pidFile + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private static boolean isPidAlive(long pid) throws Exception {
        ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
        return handle != null && handle.isAlive();
    }

    private static int runCommand(List<String> command) throws IOException, InterruptedException {
        log("Executing: " + String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (var reader = process.inputReader(StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                log("  " + line);
            }
        }

        return process.waitFor();
    }

    private static void sleepSeconds(int seconds) throws InterruptedException {
        Thread.sleep(seconds * 1000L);
    }

    private static void log(String message) {
        System.out.println(LocalDateTime.now() + " [P360ServiceRestarter] " + message);
    }

    private static void sendGoogleChatWebhook(String message, Config config) throws Exception {
        if (config.googleChatWebhookUrl == null || config.googleChatWebhookUrl.isBlank()) {
            return;
        }

        String json = "{\"text\":\"" + escapeJson(message) + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.googleChatWebhookUrl))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Google Chat webhook failed: HTTP " + response.statusCode());
        }
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

    private static final class Config {
	final String googleChatWebhookUrl;
        final String serviceName;
        final int port;
        final int delaySeconds;
        final int checkIntervalSeconds;
        final int stopTimeoutSeconds;
        final Path pidFile;

        Config(String googleChatWebhookUrl, String serviceName, int port, int delaySeconds, int checkIntervalSeconds, int stopTimeoutSeconds, Path pidFile) {
	    this.googleChatWebhookUrl = googleChatWebhookUrl;
            this.serviceName = serviceName;
            this.port = port;
            this.delaySeconds = delaySeconds;
            this.checkIntervalSeconds = checkIntervalSeconds;
            this.stopTimeoutSeconds = stopTimeoutSeconds;
            this.pidFile = pidFile;
        }

        static Config fromArgs(String[] args) {
	    String googleChatWebhookUrl = "https://chat.googleapis.com/v1/spaces/AAAAZpaMbww/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=u-P1Me5vwfb04AoqTpZI0QCmPNR4fELWlqPgmupabSY";
            String serviceName = DEFAULT_SERVICE_NAME;
            int port = DEFAULT_PORT;
            int delaySeconds = DEFAULT_DELAY_SECONDS;
            int checkIntervalSeconds = DEFAULT_CHECK_INTERVAL_SECONDS;
            int stopTimeoutSeconds = DEFAULT_STOP_TIMEOUT_SECONDS;
            Path pidFile = DEFAULT_PID_FILE;

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--service-name" -> serviceName = requireValue(args, ++i, "--service-name");
                    case "--port" -> port = Integer.parseInt(requireValue(args, ++i, "--port"));
                    case "--delay-seconds" -> delaySeconds = Integer.parseInt(requireValue(args, ++i, "--delay-seconds"));
                    case "--check-interval-seconds" -> checkIntervalSeconds = Integer.parseInt(requireValue(args, ++i, "--check-interval-seconds"));
                    case "--stop-timeout-seconds" -> stopTimeoutSeconds = Integer.parseInt(requireValue(args, ++i, "--stop-timeout-seconds"));
                    case "--pid-file" -> pidFile = Path.of(requireValue(args, ++i, "--pid-file"));
                    default -> throw new IllegalArgumentException("Unknown argument: " + args[i]);
                }
            }

            return new Config(googleChatWebhookUrl, serviceName, port, delaySeconds, checkIntervalSeconds, stopTimeoutSeconds, pidFile);
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            return args[index];
        }
    }
}
