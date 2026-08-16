package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class RestConcurrencyKneeTest {

    /*
     * ============================================================
     * CONFIGURACION RAPIDA
     * ============================================================
     */

    private static final String DEFAULT_METHOD = "GET";

    private static final String DEFAULT_URL =
            "http://172.18.251.7:8080/process-engine/public/rt/GetTemplate"
          + "?template=EU4-28184747"
          + "&business=Marketplace"
          + "&externalInformation=NameExceptions,NameGuide";

    /*
     * Escalones de concurrencia.
     *
     * NO empieces con 1000.
     * Primero queremos encontrar dónde comienza a doblarse la curva.
     */
    private static final int[] DEFAULT_CONCURRENCY_LEVELS = {
            1, 2, 4, 8, 12, 16, 24, 32, 48, 64, 96, 128, 192, 256
    };

    /*
     * Warmup previo a medir cada nivel.
     */
    private static final int DEFAULT_WARMUP_SECONDS = 5;

    /*
     * Tiempo real de medición por nivel.
     */
    private static final int DEFAULT_TEST_SECONDS = 30;

    /*
     * Pausa entre niveles para permitir que servidor/BD se estabilicen.
     */
    private static final int DEFAULT_COOLDOWN_SECONDS = 10;

    /*
     * Timeout individual.
     *
     * Una petición interactiva que lleve un minuto ya no nos aporta mucho
     * para encontrar la rodilla.
     */
    private static final int DEFAULT_REQUEST_TIMEOUT_SECONDS = 30;

    /*
     * Fusibles DEL PROPIO TEST.
     *
     * Si ya cruzamos claramente la rodilla, no tiene sentido seguir
     * incrementando concurrencia hasta tumbar el ambiente.
     */
    private static final double STOP_ERROR_PERCENT = 10.0;
    private static final long STOP_P95_MS = 15_000;

    /*
     * Si quieres probar POST:
     *
     * --method POST
     * --body /ruta/request.json
     */
    private static String method = DEFAULT_METHOD;
    private static String url = DEFAULT_URL;
    private static String requestBody = null;

    private static int warmupSeconds = DEFAULT_WARMUP_SECONDS;
    private static int testSeconds = DEFAULT_TEST_SECONDS;
    private static int cooldownSeconds = DEFAULT_COOLDOWN_SECONDS;
    private static int requestTimeoutSeconds = DEFAULT_REQUEST_TIMEOUT_SECONDS;

    private static int[] concurrencyLevels = DEFAULT_CONCURRENCY_LEVELS;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();

    public static void main(String[] args) throws Exception {

        parseArguments(args);

        System.out.println();
        System.out.println("============================================================");
        System.out.println(" REST CONCURRENCY KNEE TEST");
        System.out.println("============================================================");
        System.out.println("Method       : " + method);
        System.out.println("URL          : " + url);
        System.out.println("Warmup       : " + warmupSeconds + " s");
        System.out.println("Test         : " + testSeconds + " s");
        System.out.println("Cooldown     : " + cooldownSeconds + " s");
        System.out.println("HTTP timeout : " + requestTimeoutSeconds + " s");
        System.out.println();

        System.out.println(
                "CONCURRENCY;REQUESTS;SUCCESS;ERRORS;ERROR_PCT;"
              + "REQ_PER_SEC;AVG_MS;P50_MS;P90_MS;P95_MS;P99_MS;MAX_MS"
        );

        TestResult previous = null;

        for (int concurrency : concurrencyLevels) {

            System.out.println();
            System.out.println(
                    "### LEVEL concurrency=" + concurrency
                  + " warmup=" + warmupSeconds + "s"
                  + " test=" + testSeconds + "s"
            );

            if (warmupSeconds > 0) {
                runLevel(concurrency, warmupSeconds, false);
            }

            TestResult result = runLevel(concurrency, testSeconds, true);

            printResult(result);

            if (previous != null) {
                analyzeStep(previous, result);
            }

            if (shouldStop(result)) {
                System.out.println();
                System.out.println(
                        "*** SAFETY STOP ***"
                      + " La prueba ya cruzó claramente una zona degradada."
                );
                break;
            }

            previous = result;

            if (cooldownSeconds > 0) {
                System.out.println(
                        "Cooldown " + cooldownSeconds + "s..."
                );
                Thread.sleep(cooldownSeconds * 1000L);
            }
        }

        System.out.println();
        System.out.println("============================================================");
        System.out.println(" TEST FINISHED");
        System.out.println("============================================================");
    }

    private static TestResult runLevel(
            int concurrency,
            int durationSeconds,
            boolean collectMetrics) throws InterruptedException {

        ExecutorService executor =
                Executors.newFixedThreadPool(concurrency);

        CountDownLatch ready =
                new CountDownLatch(concurrency);

        CountDownLatch start =
                new CountDownLatch(1);

        long durationNanos =
                TimeUnit.SECONDS.toNanos(durationSeconds);

        Metrics metrics =
                new Metrics();

        for (int i = 0; i < concurrency; i++) {

            executor.submit(() -> {

                ready.countDown();

                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                long deadline =
                        System.nanoTime() + durationNanos;

                while (System.nanoTime() < deadline) {

                    long begin =
                            System.nanoTime();

                    boolean success = false;

                    try {

                        HttpRequest request =
                                createRequest();

                        HttpResponse<String> response =
                                HTTP_CLIENT.send(
                                        request,
                                        HttpResponse.BodyHandlers.ofString()
                                );

                        int status =
                                response.statusCode();

                        success =
                                status >= 200
                             && status < 400;

                    } catch (IOException e) {

                        success = false;

                    } catch (InterruptedException e) {

                        Thread.currentThread().interrupt();
                        break;

                    } catch (Exception e) {

                        success = false;
                    }

                    long elapsedNanos =
                            System.nanoTime() - begin;

                    if (collectMetrics) {

                        long elapsedMicros =
                                TimeUnit.NANOSECONDS.toMicros(
                                        elapsedNanos
                                );

                        metrics.record(
                                elapsedMicros,
                                success
                        );
                    }
                }
            });
        }

        ready.await();

        long testStart =
                System.nanoTime();

        start.countDown();

        executor.shutdown();

        executor.awaitTermination(
                durationSeconds
              + requestTimeoutSeconds
              + 30L,
                TimeUnit.SECONDS
        );

        long testElapsedNanos =
                System.nanoTime() - testStart;

        if (!collectMetrics) {
            return null;
        }

        return metrics.snapshot(
                concurrency,
                testElapsedNanos
        );
    }

    private static HttpRequest createRequest() {

        HttpRequest.Builder builder =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(
                                Duration.ofSeconds(
                                        requestTimeoutSeconds
                                )
                        )
                        .header(
                                "Accept",
                                "application/json"
                        );

        if ("POST".equalsIgnoreCase(method)) {

            String body =
                    requestBody == null
                            ? ""
                            : requestBody;

            builder.header(
                    "Content-Type",
                    "application/json"
            );

            builder.POST(
                    HttpRequest.BodyPublishers.ofString(
                            body,
                            StandardCharsets.UTF_8
                    )
            );

        } else if ("PUT".equalsIgnoreCase(method)) {

            String body =
                    requestBody == null
                            ? ""
                            : requestBody;

            builder.header(
                    "Content-Type",
                    "application/json"
            );

            builder.PUT(
                    HttpRequest.BodyPublishers.ofString(
                            body,
                            StandardCharsets.UTF_8
                    )
            );

        } else if ("DELETE".equalsIgnoreCase(method)) {

            builder.DELETE();

        } else {

            builder.GET();
        }

        return builder.build();
    }

    private static void analyzeStep(
            TestResult previous,
            TestResult current) {

        double throughputGrowth;

        if (previous.requestsPerSecond == 0) {

            throughputGrowth = 0;

        } else {

            throughputGrowth =
                    (
                        current.requestsPerSecond
                      / previous.requestsPerSecond
                      - 1.0
                    ) * 100.0;
        }

        double p95Growth;

        if (previous.p95Ms == 0) {

            p95Growth = 0;

        } else {

            p95Growth =
                    (
                        (double) current.p95Ms
                      / previous.p95Ms
                      - 1.0
                    ) * 100.0;
        }

        System.out.printf(
                Locale.US,
                "Step analysis: throughput %+,.1f%% | p95 %+,.1f%%%n",
                throughputGrowth,
                p95Growth
        );

        /*
         * Heurística de posible rodilla:
         *
         * Añadimos concurrencia,
         * throughput crece poco,
         * pero la latencia crece mucho.
         */
        if (throughputGrowth < 20.0
                && p95Growth > 50.0) {

            System.out.println(
                    ">>> POSSIBLE KNEE: "
                  + "el throughput casi no creció "
                  + "pero p95 aumentó fuertemente."
            );
        }

        if (current.requestsPerSecond
                < previous.requestsPerSecond
            && current.p95Ms
                > previous.p95Ms) {

            System.out.println(
                    ">>> OVERLOAD SIGNAL: "
                  + "menos throughput y más latencia."
            );
        }
    }

    private static boolean shouldStop(
            TestResult result) {

        if (result.errorPercent
                >= STOP_ERROR_PERCENT) {

            System.out.println(
                    "STOP reason: error rate = "
                  + format(result.errorPercent)
                  + "%"
            );

            return true;
        }

        if (result.p95Ms
                >= STOP_P95_MS) {

            System.out.println(
                    "STOP reason: p95 = "
                  + result.p95Ms
                  + " ms"
            );

            return true;
        }

        return false;
    }

    private static void printResult(
            TestResult r) {

        System.out.println(
                r.concurrency
              + ";"
              + r.requests
              + ";"
              + r.success
              + ";"
              + r.errors
              + ";"
              + format(r.errorPercent)
              + ";"
              + format(r.requestsPerSecond)
              + ";"
              + format(r.averageMs)
              + ";"
              + r.p50Ms
              + ";"
              + r.p90Ms
              + ";"
              + r.p95Ms
              + ";"
              + r.p99Ms
              + ";"
              + r.maxMs
        );
    }

    private static String format(
            double value) {

        return String.format(
                Locale.US,
                "%.3f",
                value
        );
    }

    private static void parseArguments(
            String[] args) throws IOException {

        for (int i = 0; i < args.length; i++) {

            switch (args[i]) {

                case "--method":
                    method =
                            args[++i]
                                    .trim()
                                    .toUpperCase(
                                            Locale.ROOT
                                    );
                    break;

                case "--url":
                    url =
                            args[++i];
                    break;

                case "--body":
                    requestBody =
                            Files.readString(
                                    Path.of(args[++i]),
                                    StandardCharsets.UTF_8
                            );
                    break;

                case "--warmup":
                    warmupSeconds =
                            Integer.parseInt(
                                    args[++i]
                            );
                    break;

                case "--seconds":
                    testSeconds =
                            Integer.parseInt(
                                    args[++i]
                            );
                    break;

                case "--cooldown":
                    cooldownSeconds =
                            Integer.parseInt(
                                    args[++i]
                            );
                    break;

                case "--timeout":
                    requestTimeoutSeconds =
                            Integer.parseInt(
                                    args[++i]
                            );
                    break;

                case "--levels":
                    concurrencyLevels =
                            parseLevels(
                                    args[++i]
                            );
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Unknown argument: "
                          + args[i]
                    );
            }
        }
    }

    private static int[] parseLevels(
            String value) {

        String[] parts =
                value.split(",");

        int[] result =
                new int[parts.length];

        for (int i = 0; i < parts.length; i++) {

            result[i] =
                    Integer.parseInt(
                            parts[i].trim()
                    );
        }

        return result;
    }

    private static class Metrics {

        private final LongAdder requests =
                new LongAdder();

        private final LongAdder success =
                new LongAdder();

        private final LongAdder errors =
                new LongAdder();

        private final LongAdder totalMicros =
                new LongAdder();

        private final List<Long> latencyMicros =
                Collections.synchronizedList(
                        new ArrayList<>()
                );

        void record(
                long elapsedMicros,
                boolean successful) {

            requests.increment();

            totalMicros.add(
                    elapsedMicros
            );

            latencyMicros.add(
                    elapsedMicros
            );

            if (successful) {

                success.increment();

            } else {

                errors.increment();
            }
        }

        TestResult snapshot(
                int concurrency,
                long elapsedNanos) {

            List<Long> copy;

            synchronized (latencyMicros) {

                copy =
                        new ArrayList<>(
                                latencyMicros
                        );
            }

            Collections.sort(copy);

            long requestCount =
                    requests.sum();

            long successCount =
                    success.sum();

            long errorCount =
                    errors.sum();

            double elapsedSeconds =
                    elapsedNanos
                  / 1_000_000_000.0;

            double requestsPerSecond =
                    requestCount
                  / elapsedSeconds;

            double averageMs =
                    requestCount == 0
                            ? 0
                            : (
                                totalMicros.sum()
                              / (double) requestCount
                              / 1000.0
                            );

            double errorPercent =
                    requestCount == 0
                            ? 0
                            : (
                                errorCount
                              * 100.0
                              / requestCount
                            );

            return new TestResult(
                    concurrency,
                    requestCount,
                    successCount,
                    errorCount,
                    errorPercent,
                    requestsPerSecond,
                    averageMs,
                    percentileMs(copy, 0.50),
                    percentileMs(copy, 0.90),
                    percentileMs(copy, 0.95),
                    percentileMs(copy, 0.99),
                    percentileMs(copy, 1.00)
            );
        }

        private long percentileMs(
                List<Long> values,
                double percentile) {

            if (values.isEmpty()) {
                return 0;
            }

            int index =
                    (int) Math.ceil(
                            percentile
                          * values.size()
                    ) - 1;

            if (index < 0) {
                index = 0;
            }

            if (index >= values.size()) {
                index =
                        values.size() - 1;
            }

            return Math.round(
                    values.get(index)
                  / 1000.0
            );
        }
    }

    private static class TestResult {

        final int concurrency;

        final long requests;
        final long success;
        final long errors;

        final double errorPercent;
        final double requestsPerSecond;
        final double averageMs;

        final long p50Ms;
        final long p90Ms;
        final long p95Ms;
        final long p99Ms;
        final long maxMs;

        TestResult(
                int concurrency,
                long requests,
                long success,
                long errors,
                double errorPercent,
                double requestsPerSecond,
                double averageMs,
                long p50Ms,
                long p90Ms,
                long p95Ms,
                long p99Ms,
                long maxMs) {

            this.concurrency =
                    concurrency;

            this.requests =
                    requests;

            this.success =
                    success;

            this.errors =
                    errors;

            this.errorPercent =
                    errorPercent;

            this.requestsPerSecond =
                    requestsPerSecond;

            this.averageMs =
                    averageMs;

            this.p50Ms =
                    p50Ms;

            this.p90Ms =
                    p90Ms;

            this.p95Ms =
                    p95Ms;

            this.p99Ms =
                    p99Ms;

            this.maxMs =
                    maxMs;
        }
    }
}