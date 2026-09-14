package mx.com.liverpool.p360.services.core.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Repara pérdidas históricas de media NO final a partir de PACactiveMQListener.
 *
 * Diseño para históricos gigantes:
 *
 * PASS 1
 *   - lee el log línea por línea;
 *   - descarta inmediatamente mensajes irrelevantes;
 *   - reduce los eventos de imagen a registros compactos;
 *   - los escribe en un spool shardeado por variantId.
 *
 * PASS 2
 *   - procesa UN shard por vez;
 *   - mantiene en RAM sólo la máquina de estados de los Items de ese shard;
 *   - emite cada candidato inmediatamente a CSV + NDJSON;
 *   - opcionalmente invoca LasImagenes inmediatamente;
 *   - libera el shard completo antes de continuar.
 *
 * Nunca usa readAllLines/readString para el histórico y nunca conserva una
 * lista global de candidatos/resultados.
 *
 * Sólo se consideran:
 *   ProductImage, ProductImageDetail, Illustration, ProductImageSmosh.
 * Las raíces con sufijo 2 y cualquier otra característica quedan fuera.
 *
 * El auto-repair sólo se permite para FULL_LOSS con Name+URL completos y usa:
 *   replaceAssets=false
 *   repairOnlyIfCurrentNonFinalEmpty=true
 * para no pisar media que haya reaparecido posteriormente.
 */
public final class HistoricalLasImagenesRepair {

    private static final String MESSAGE_MARKER = "A message body:";

    private static final Set<String> NON_FINAL_ROOTS = new LinkedHashSet<String>(Arrays.asList(
            "ProductImage",
            "ProductImageDetail",
            "Illustration",
            "ProductImageSmosh"));

    private static final Comparator<MediaSnapshot> MEDIA_ORDER = (a, b) -> {
        int root = Integer.compare(rootPriority(a.rootCode), rootPriority(b.rootCode));
        if (root != 0) {
            return root;
        }
        return nullSafe(a.recordKey).compareTo(nullSafe(b.recordKey));
    };

    private HistoricalLasImagenesRepair() {
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        Files.createDirectories(config.outputDir);
        prepareSpoolDirectory(config.spoolDir);

        long started = System.currentTimeMillis();
        AnalysisStats analysis;

        try (ShardSpool spool = new ShardSpool(
                config.spoolDir,
                config.shards,
                config.maxOpenShardWriters)) {
            analysis = streamLogToSpool(config, spool);
        }

        System.out.println("PASS1 terminado: lines=" + analysis.lines
                + " json=" + analysis.validJsonMessages
                + " article=" + analysis.articleMessages
                + " mediaPrefilter=" + analysis.mediaPrefilterMessages
                + " xml=" + analysis.validXmlSummaries
                + " xmlErrors=" + analysis.xmlErrors
                + " events=" + analysis.spooledEvents
                + " shards=" + analysis.shardsWithData);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        Path report = config.outputDir.resolve("image_repair_report.csv");
        Path payloads = config.outputDir.resolve("repair_payloads.ndjson");
        ResultCounters counters = new ResultCounters();

        try (BufferedWriter reportWriter = newOutputWriter(report);
             BufferedWriter payloadWriter = newOutputWriter(payloads)) {

            writeReportHeader(reportWriter);

            for (int shard = 0; shard < config.shards; shard++) {
                Path shardFile = shardPath(config.spoolDir, shard);
                if (!Files.isRegularFile(shardFile)) {
                    continue;
                }

                ShardStats shardStats = processShard(
                        shardFile,
                        shard,
                        config,
                        httpClient,
                        reportWriter,
                        payloadWriter,
                        counters);

                reportWriter.flush();
                payloadWriter.flush();

                System.out.println("PASS2 shard=" + shard
                        + " events=" + shardStats.events
                        + " variants=" + shardStats.variants
                        + " candidates=" + shardStats.candidates
                        + " totalCandidates=" + counters.candidates);

                if (!config.keepSpool) {
                    Files.deleteIfExists(shardFile);
                }
            }
        }

        JSONObject statusCounts = new JSONObject();
        for (Map.Entry<String, Long> entry : counters.byStatus.entrySet()) {
            statusCounts.put(entry.getKey(), entry.getValue().longValue());
        }

        JSONObject summary = new JSONObject()
                .put("logFile", config.logFile.toString())
                .put("proposalId", config.proposalId)
                .put("apply", config.apply)
                .put("serviceUrl", config.serviceUrl)
                .put("shards", config.shards)
                .put("maxOpenShardWriters", config.maxOpenShardWriters)
                .put("deletionWaveSeconds", config.deletionWaveSeconds)
                .put("lines", analysis.lines)
                .put("validJsonMessages", analysis.validJsonMessages)
                .put("articleMessages", analysis.articleMessages)
                .put("mediaPrefilterMessages", analysis.mediaPrefilterMessages)
                .put("validXmlSummaries", analysis.validXmlSummaries)
                .put("xmlErrors", analysis.xmlErrors)
                .put("spooledEvents", analysis.spooledEvents)
                .put("candidates", counters.candidates)
                .put("fullLossCandidates", counters.fullLoss)
                .put("partialLossCandidates", counters.partialLoss)
                .put("completeCandidates", counters.complete)
                .put("statusCounts", statusCounts)
                .put("report", report.toString())
                .put("payloadStream", payloads.toString())
                .put("elapsedMs", System.currentTimeMillis() - started)
                .put("generatedAt", Instant.now().toString());

        writeUtf8(config.outputDir.resolve("summary.json"), summary.toString(2));

        if (!config.keepSpool) {
            deleteSpoolDirectoryIfEmpty(config.spoolDir);
        }

        System.out.println("Terminado: candidates=" + counters.candidates
                + " fullLoss=" + counters.fullLoss
                + " partialLoss=" + counters.partialLoss
                + " complete=" + counters.complete);
        System.out.println("CSV=" + report);
        System.out.println("NDJSON=" + payloads);
    }

    /** PASS 1: RAM acotada al tamaño de una línea/XML y al cache de writers. */
    private static AnalysisStats streamLogToSpool(Config config,
                                                  ShardSpool spool) throws Exception {
        AnalysisStats stats = new AnalysisStats();
        DocumentBuilderFactory factory = secureDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        Files.newInputStream(config.logFile), StandardCharsets.UTF_8),
                config.inputBufferChars)) {

            String line;
            while ((line = reader.readLine()) != null) {
                stats.lines++;

                if (config.progressLines > 0
                        && stats.lines % config.progressLines == 0) {
                    System.out.println("PASS1 progress lines=" + stats.lines
                            + " spoolEvents=" + stats.spooledEvents);
                }

                int marker = line.indexOf(MESSAGE_MARKER);
                if (marker < 0) {
                    continue;
                }

                JSONObject root;
                try {
                    root = new JSONObject(
                            line.substring(marker + MESSAGE_MARKER.length()).trim());
                    stats.validJsonMessages++;
                } catch (Exception e) {
                    continue;
                }

                JSONObject change = root.optJSONObject("entityItemChange");
                if (change == null || !"Article".equals(change.optString("_entity", ""))) {
                    continue;
                }

                String variantId = change.optString("_identifier", "");
                String changeSummary = change.optString("_changeSummary", "");
                if (variantId.isEmpty() || changeSummary.isEmpty()) {
                    continue;
                }
                stats.articleMessages++;

                /* Barato: evita parsear XML de Article que claramente no es media. */
                if (!mightContainNonFinalMedia(changeSummary)) {
                    continue;
                }
                stats.mediaPrefilterMessages++;

                try {
                    Document document = builder.parse(
                            new InputSource(new StringReader(changeSummary)));
                    stats.spooledEvents += spoolRelevantEvents(
                            variantId,
                            change.optString("_eventTimestamp", ""),
                            document,
                            spool);
                    stats.validXmlSummaries++;
                } catch (Exception e) {
                    stats.xmlErrors++;
                    try {
                        builder.reset();
                    } catch (UnsupportedOperationException ignored) {
                        // Best effort.
                    }
                }
            }
        }

        stats.shardsWithData = spool.shardsTouched();
        return stats;
    }

    private static boolean mightContainNonFinalMedia(String xml) {
        /* ProductImage también captura lexicalmente Detail/Smosh/2; exactitud viene después. */
        return xml.contains("<_code>ProductImage")
                || xml.contains("<_code>Illustration");
    }

    private static int spoolRelevantEvents(String variantId,
                                           String eventTimestamp,
                                           Document document,
                                           ShardSpool spool) throws IOException {
        Element article = firstDescendant(document.getDocumentElement(), "article");
        if (article == null) {
            return 0;
        }

        int written = 0;
        for (Element record : directChildren(article, "_characteristicRecords")) {
            Element qualification = directChild(record, "_qualification");
            String rootCode = qualificationCharacteristicCode(qualification);
            if (!NON_FINAL_ROOTS.contains(rootCode)) {
                continue;
            }

            String recordKey = qualification == null
                    ? ""
                    : directText(qualification, "recordKey");
            if (recordKey.isEmpty()) {
                recordKey = "0000.0000.RK";
            }

            SpoolEvent event = new SpoolEvent();
            event.variantId = variantId;
            event.eventTimestamp = eventTimestamp;
            event.rootCode = rootCode;
            event.recordKey = recordKey;
            event.changeType = directText(record, "_changeType");

            for (Element childContainer : directChildren(record, "_children")) {
                Element childQualification = directChild(childContainer, "_qualification");
                String childCode = qualificationCharacteristicCode(childQualification);
                FieldKind field = fieldKind(rootCode, childCode);
                if (field == FieldKind.NONE) {
                    continue;
                }
                event.set(
                        field,
                        versionedValue(childContainer, "_old"),
                        versionedValue(childContainer, "_current"));
            }

            if (!event.isRelevant()) {
                continue;
            }

            spool.write(event);
            written++;
        }
        return written;
    }

    /** PASS 2: sólo este shard existe como máquina de estados en RAM. */
    private static ShardStats processShard(Path shardFile,
                                           int shard,
                                           Config config,
                                           HttpClient httpClient,
                                           BufferedWriter reportWriter,
                                           BufferedWriter payloadWriter,
                                           ResultCounters counters) throws Exception {
        ShardStats stats = new ShardStats();
        Map<String, VariantHistory> histories = new LinkedHashMap<String, VariantHistory>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        Files.newInputStream(shardFile), StandardCharsets.UTF_8),
                config.spoolBufferChars)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                stats.events++;
                SpoolEvent event = SpoolEvent.fromJson(new JSONObject(line));
                VariantHistory history = histories.computeIfAbsent(
                        event.variantId, VariantHistory::new);
                applyEvent(history, event);
            }
        }

        stats.variants = histories.size();

        /* No List global de candidates/results: se consume cada uno al instante. */
        for (VariantHistory history : histories.values()) {
            RepairCandidate candidate = buildCandidate(
                    history,
                    config.proposalId,
                    config.deletionWaveSeconds);
            if (candidate == null) {
                continue;
            }

            stats.candidates++;
            counters.recordCandidate(candidate);
            processCandidate(
                    candidate,
                    shard,
                    config,
                    httpClient,
                    reportWriter,
                    payloadWriter,
                    counters);
        }

        histories.clear();
        return stats;
    }

    private static void applyEvent(VariantHistory history, SpoolEvent event) {
        MediaRecordState state = history.state(event.rootCode, event.recordKey);

        if (!event.oldName.isEmpty()) {
            state.name = event.oldName;
        }
        if (!event.oldUrl.isEmpty()) {
            state.url = event.oldUrl;
        }
        if (!event.oldStatus.isEmpty()) {
            state.status = event.oldStatus;
        }

        boolean sawCurrent = false;
        if (!event.currentName.isEmpty()) {
            state.name = event.currentName;
            sawCurrent = true;
        }
        if (!event.currentUrl.isEmpty()) {
            state.url = event.currentUrl;
            sawCurrent = true;
        }
        if (!event.currentStatus.isEmpty()) {
            state.status = event.currentStatus;
            sawCurrent = true;
        }

        if ("CREATED".equals(event.changeType)) {
            state.present = true;
        } else if ("DELETED".equals(event.changeType)) {
            state.present = false;
            state.lastDeletedAt = event.eventTimestamp;
            state.lastDeletedSnapshot = state.snapshot();
        } else if (sawCurrent) {
            state.present = true;
        }
    }

    private static RepairCandidate buildCandidate(VariantHistory history,
                                                  String proposalId,
                                                  long waveSeconds) {
        List<MediaSnapshot> allMissing = new ArrayList<MediaSnapshot>();
        int presentCount = 0;
        String latestDelete = "";

        for (MediaRecordState state : history.records.values()) {
            if (state.present) {
                presentCount++;
            } else if (state.lastDeletedSnapshot != null) {
                allMissing.add(state.lastDeletedSnapshot.copy());
                if (state.lastDeletedAt.compareTo(latestDelete) > 0) {
                    latestDelete = state.lastDeletedAt;
                }
            }
        }

        if (allMissing.isEmpty()) {
            return null;
        }

        /* Sólo la última ola; no resucitar borrados viejos/intencionales. */
        List<MediaSnapshot> missing = new ArrayList<MediaSnapshot>();
        for (MediaSnapshot snapshot : allMissing) {
            if (withinDeletionWave(snapshot.deletedAt, latestDelete, waveSeconds)) {
                missing.add(snapshot);
            }
        }
        if (missing.isEmpty()) {
            return null;
        }

        Collections.sort(missing, MEDIA_ORDER);
        boolean complete = true;
        for (MediaSnapshot snapshot : missing) {
            if (snapshot.name.isEmpty() || snapshot.url.isEmpty()) {
                complete = false;
            }
        }

        RepairCandidate candidate = new RepairCandidate();
        candidate.proposalId = proposalId;
        candidate.variantId = history.variantId;
        candidate.fullLoss = presentCount == 0;
        candidate.complete = complete;
        candidate.lastDeleteAt = latestDelete;
        candidate.photos.addAll(missing);
        return candidate;
    }

    private static void processCandidate(RepairCandidate candidate,
                                         int shard,
                                         Config config,
                                         HttpClient httpClient,
                                         BufferedWriter reportWriter,
                                         BufferedWriter payloadWriter,
                                         ResultCounters counters) throws Exception {
        JSONObject payload = candidate.toLasImagenesPayload();

        long payloadLine = ++counters.payloadLines;
        /* Una línea == un payload interno exacto y válido para LasImagenes. */
        payloadWriter.write(payload.toString());
        payloadWriter.newLine();

        Path individual = null;
        if (config.individualPayloadFiles) {
            Path dir = config.outputDir.resolve("payloads");
            Files.createDirectories(dir);
            individual = dir.resolve("repair_" + safeFile(candidate.variantId) + ".json");
            writeUtf8(individual, payload.toString(2));
        }

        RepairResult result = new RepairResult(candidate);
        result.payloadReference = individual == null
                ? config.outputDir.resolve("repair_payloads.ndjson") + "#line=" + payloadLine
                : individual.toString();

        if (!candidate.fullLoss) {
            result.status = "PARTIAL_LOSS_NOT_APPLIED";
            result.detail = "Quedó media NO final presente al final del historial; no se auto-repara.";
        } else if (!candidate.complete) {
            result.status = "INCOMPLETE_HISTORY_NOT_APPLIED";
            result.detail = "Falta Name o URL en al menos un record eliminado; payload no confiable.";
        } else if (!config.apply) {
            result.status = "DRY_RUN_READY";
            result.detail = "Payload preparado; use --apply para invocar LasImagenes.";
        } else {
            invokeLasImagenes(httpClient, config, payload, result);
        }

        counters.recordStatus(result.status);
        writeReportRow(reportWriter, result, shard);

        if (config.flushEveryCandidates > 0
                && counters.candidates % config.flushEveryCandidates == 0) {
            reportWriter.flush();
            payloadWriter.flush();
        }

        if (config.progressCandidates > 0
                && counters.candidates % config.progressCandidates == 0) {
            System.out.println("PASS2 progress candidates=" + counters.candidates
                    + " variant=" + candidate.variantId
                    + " status=" + result.status);
        }
    }

    private static void invokeLasImagenes(HttpClient client,
                                          Config config,
                                          JSONObject payload,
                                          RepairResult result) {
        JSONObject outer = new JSONObject().put("input", payload.toString());

        for (int attempt = 1; attempt <= config.maxRestAttempts; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.serviceUrl))
                        .timeout(Duration.ofSeconds(config.restRequestTimeoutSeconds))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(
                                outer.toString(), StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                result.httpStatus = response.statusCode();

                if (response.statusCode() == 429 && attempt < config.maxRestAttempts) {
                    Thread.sleep(parseRetryAfter(response, 5L) * 1000L);
                    continue;
                }

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    result.status = "HTTP_FAILED";
                    result.detail = "HTTP " + response.statusCode();
                    return;
                }

                JSONObject service = new JSONObject(response.body());
                int failed = service.optInt("failedVariants", 0);
                int success = service.optInt("successfulVariants", 0);
                int skipped = service.optInt("skippedVariants", 0);

                if (failed > 0) {
                    result.status = "SERVICE_FAILED";
                    result.detail = "LasImagenes reportó failedVariants=" + failed;
                } else if (success > 0) {
                    result.status = "REPAIRED";
                    result.detail = "LasImagenes confirmó el repair.";
                } else if (skipped > 0) {
                    result.status = "SKIPPED_BY_SERVICE_GUARD";
                    result.detail = "LasImagenes no aplicó; skippedVariants=" + skipped;
                } else {
                    result.status = "UNCONFIRMED_SERVICE_RESPONSE";
                    result.detail = "2xx sin success/failed/skipped reconocible.";
                }
                return;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                result.status = "INTERRUPTED";
                result.detail = e.toString();
                return;
            } catch (Exception e) {
                if (attempt == config.maxRestAttempts) {
                    result.status = "REST_EXCEPTION";
                    result.detail = e.toString();
                    return;
                }
                try {
                    Thread.sleep(2000L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    result.status = "INTERRUPTED";
                    result.detail = interrupted.toString();
                    return;
                }
            }
        }
    }

    private static long parseRetryAfter(HttpResponse<?> response, long fallback) {
        try {
            return Math.max(1L,
                    Long.parseLong(response.headers().firstValue("Retry-After")
                            .orElse(Long.toString(fallback))));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static void writeReportHeader(BufferedWriter writer) throws IOException {
        writer.write("SHARD,VARIANT_ID,LOSS_TYPE,LAST_DELETE_AT,PHOTOS,COMPLETE,STATUS,HTTP_STATUS,DETAIL,PAYLOAD_REFERENCE");
        writer.newLine();
    }

    private static void writeReportRow(BufferedWriter writer,
                                       RepairResult result,
                                       int shard) throws IOException {
        RepairCandidate c = result.candidate;
        writer.write(Integer.toString(shard));
        writer.write(',');
        writer.write(csv(c.variantId));
        writer.write(',');
        writer.write(csv(c.fullLoss ? "FULL_LOSS" : "PARTIAL_LOSS"));
        writer.write(',');
        writer.write(csv(c.lastDeleteAt));
        writer.write(',');
        writer.write(Integer.toString(c.photos.size()));
        writer.write(',');
        writer.write(Boolean.toString(c.complete));
        writer.write(',');
        writer.write(csv(result.status));
        writer.write(',');
        writer.write(result.httpStatus == 0 ? "" : Integer.toString(result.httpStatus));
        writer.write(',');
        writer.write(csv(result.detail));
        writer.write(',');
        writer.write(csv(result.payloadReference));
        writer.newLine();
    }

    private static BufferedWriter newOutputWriter(Path path) throws IOException {
        return Files.newBufferedWriter(
                path,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private static void writeUtf8(Path path, String value) throws IOException {
        Files.write(
                path,
                value.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private static String csv(String value) {
        String text = value == null ? "" : value;
        return '"' + text.replace("\"", "\"\"") + '"';
    }

    private static String safeFile(String value) {
        return nullSafe(value).replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static int rootPriority(String root) {
        if ("ProductImage".equals(root)) {
            return 0;
        }
        if ("ProductImageDetail".equals(root)) {
            return 1;
        }
        if ("Illustration".equals(root)) {
            return 2;
        }
        if ("ProductImageSmosh".equals(root)) {
            return 3;
        }
        return 9;
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private static boolean withinDeletionWave(String deletedAt,
                                              String latestDeleteAt,
                                              long waveSeconds) {
        try {
            Instant deleted = Instant.parse(deletedAt);
            Instant latest = Instant.parse(latestDeleteAt);
            long diffMillis = Math.abs(latest.toEpochMilli() - deleted.toEpochMilli());
            return diffMillis <= Math.max(1L, waveSeconds) * 1000L;
        } catch (Exception e) {
            return nullSafe(deletedAt).equals(nullSafe(latestDeleteAt));
        }
    }

    private static FieldKind fieldKind(String rootCode, String childCode) {
        if ((rootCode + "_Name").equals(childCode)) {
            return FieldKind.NAME;
        }
        if ((rootCode + "_URL").equals(childCode)) {
            return FieldKind.URL;
        }
        if ((rootCode + "_Status").equals(childCode)) {
            return FieldKind.STATUS;
        }
        return FieldKind.NONE;
    }

    private static String versionedValue(Element record, String versionTag) {
        Element recordLang = directChild(record, "_recordLang");
        if (recordLang == null) {
            return "";
        }
        Element values = firstDescendant(recordLang, "values");
        if (values == null) {
            return "";
        }
        Element version = directChild(values, versionTag);
        if (version == null) {
            return "";
        }
        Element label = firstDescendant(version, "_label");
        return label == null ? normalizedText(version) : normalizedText(label);
    }

    private static String qualificationCharacteristicCode(Element qualification) {
        if (qualification == null) {
            return "";
        }
        Element characteristic = directChild(qualification, "characteristic");
        return characteristic == null ? "" : directText(characteristic, "_code");
    }

    private static DocumentBuilderFactory secureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        trySetFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException ignored) {
            // Best effort.
        }
        return factory;
    }

    private static void trySetFeature(DocumentBuilderFactory factory,
                                      String feature,
                                      boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignored) {
            // Best effort.
        }
    }

    private static Element directChild(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        return null;
    }

    private static List<Element> directChildren(Element parent, String name) {
        List<Element> result = new ArrayList<Element>();
        if (parent == null) {
            return result;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getNodeName())) {
                result.add((Element) node);
            }
        }
        return result;
    }

    private static Element firstDescendant(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        if (name.equals(parent.getNodeName())) {
            return parent;
        }
        NodeList matches = parent.getElementsByTagName(name);
        return matches.getLength() == 0 ? null : (Element) matches.item(0);
    }

    private static String directText(Element parent, String childName) {
        Element child = directChild(parent, childName);
        return child == null ? "" : normalizedText(child);
    }

    private static String normalizedText(Element element) {
        if (element == null || element.getTextContent() == null) {
            return "";
        }
        return element.getTextContent().trim();
    }

    private static Path shardPath(Path spoolDir, int shard) {
        return spoolDir.resolve(String.format(java.util.Locale.ROOT, "shard-%05d.ndjson", shard));
    }

    private static void prepareSpoolDirectory(Path spoolDir) throws IOException {
        Files.createDirectories(spoolDir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(spoolDir, "shard-*.ndjson")) {
            for (Path path : stream) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void deleteSpoolDirectoryIfEmpty(Path spoolDir) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(spoolDir)) {
            if (!stream.iterator().hasNext()) {
                Files.deleteIfExists(spoolDir);
            }
        }
    }

    private enum FieldKind {
        NONE, NAME, URL, STATUS
    }

    private static final class ShardSpool implements AutoCloseable {
        private final Path spoolDir;
        private final int shardCount;
        private final int maxOpen;
        private final Set<Integer> touched = new LinkedHashSet<Integer>();
        private final LinkedHashMap<Integer, BufferedWriter> writers =
                new LinkedHashMap<Integer, BufferedWriter>(16, 0.75f, true);

        ShardSpool(Path spoolDir, int shardCount, int maxOpen) {
            this.spoolDir = spoolDir;
            this.shardCount = shardCount;
            this.maxOpen = maxOpen;
        }

        void write(SpoolEvent event) throws IOException {
            int shard = Math.floorMod(event.variantId.hashCode(), shardCount);
            BufferedWriter writer = writerFor(shard);
            writer.write(event.toJson().toString());
            writer.newLine();
            touched.add(Integer.valueOf(shard));
        }

        int shardsTouched() {
            return touched.size();
        }

        private BufferedWriter writerFor(int shard) throws IOException {
            Integer key = Integer.valueOf(shard);
            BufferedWriter existing = writers.get(key);
            if (existing != null) {
                return existing;
            }

            if (writers.size() >= maxOpen) {
                Iterator<Map.Entry<Integer, BufferedWriter>> iterator = writers.entrySet().iterator();
                if (iterator.hasNext()) {
                    Map.Entry<Integer, BufferedWriter> eldest = iterator.next();
                    eldest.getValue().close();
                    iterator.remove();
                }
            }

            OpenOption[] options = new OpenOption[] {
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.WRITE
            };
            BufferedWriter created = Files.newBufferedWriter(
                    shardPath(spoolDir, shard), StandardCharsets.UTF_8, options);
            writers.put(key, created);
            return created;
        }

        @Override
        public void close() throws IOException {
            IOException first = null;
            for (BufferedWriter writer : writers.values()) {
                try {
                    writer.close();
                } catch (IOException e) {
                    if (first == null) {
                        first = e;
                    }
                }
            }
            writers.clear();
            if (first != null) {
                throw first;
            }
        }
    }

    private static final class SpoolEvent {
        String variantId = "";
        String eventTimestamp = "";
        String rootCode = "";
        String recordKey = "";
        String changeType = "";
        String oldName = "";
        String currentName = "";
        String oldUrl = "";
        String currentUrl = "";
        String oldStatus = "";
        String currentStatus = "";

        void set(FieldKind field, String oldValue, String currentValue) {
            if (field == FieldKind.NAME) {
                oldName = nullSafe(oldValue);
                currentName = nullSafe(currentValue);
            } else if (field == FieldKind.URL) {
                oldUrl = nullSafe(oldValue);
                currentUrl = nullSafe(currentValue);
            } else if (field == FieldKind.STATUS) {
                oldStatus = nullSafe(oldValue);
                currentStatus = nullSafe(currentValue);
            }
        }

        boolean isRelevant() {
            return "CREATED".equals(changeType)
                    || "DELETED".equals(changeType)
                    || !oldName.isEmpty()
                    || !currentName.isEmpty()
                    || !oldUrl.isEmpty()
                    || !currentUrl.isEmpty()
                    || !oldStatus.isEmpty()
                    || !currentStatus.isEmpty();
        }

        JSONObject toJson() {
            JSONObject json = new JSONObject()
                    .put("v", variantId)
                    .put("t", eventTimestamp)
                    .put("r", rootCode)
                    .put("k", recordKey);
            putIfNotEmpty(json, "c", changeType);
            putIfNotEmpty(json, "on", oldName);
            putIfNotEmpty(json, "cn", currentName);
            putIfNotEmpty(json, "ou", oldUrl);
            putIfNotEmpty(json, "cu", currentUrl);
            putIfNotEmpty(json, "os", oldStatus);
            putIfNotEmpty(json, "cs", currentStatus);
            return json;
        }

        static SpoolEvent fromJson(JSONObject json) {
            SpoolEvent event = new SpoolEvent();
            event.variantId = json.optString("v", "");
            event.eventTimestamp = json.optString("t", "");
            event.rootCode = json.optString("r", "");
            event.recordKey = json.optString("k", "");
            event.changeType = json.optString("c", "");
            event.oldName = json.optString("on", "");
            event.currentName = json.optString("cn", "");
            event.oldUrl = json.optString("ou", "");
            event.currentUrl = json.optString("cu", "");
            event.oldStatus = json.optString("os", "");
            event.currentStatus = json.optString("cs", "");
            return event;
        }

        private static void putIfNotEmpty(JSONObject json, String key, String value) {
            if (value != null && !value.isEmpty()) {
                json.put(key, value);
            }
        }
    }

    private static final class VariantHistory {
        final String variantId;
        final Map<String, MediaRecordState> records =
                new LinkedHashMap<String, MediaRecordState>();

        VariantHistory(String variantId) {
            this.variantId = variantId;
        }

        MediaRecordState state(String rootCode, String recordKey) {
            String key = rootCode + "|" + recordKey;
            return records.computeIfAbsent(
                    key, k -> new MediaRecordState(rootCode, recordKey));
        }
    }

    private static final class MediaRecordState {
        final String rootCode;
        final String recordKey;
        boolean present;
        String name = "";
        String url = "";
        String status = "";
        String lastDeletedAt = "";
        MediaSnapshot lastDeletedSnapshot;

        MediaRecordState(String rootCode, String recordKey) {
            this.rootCode = rootCode;
            this.recordKey = recordKey;
        }

        MediaSnapshot snapshot() {
            MediaSnapshot snapshot = new MediaSnapshot();
            snapshot.rootCode = rootCode;
            snapshot.recordKey = recordKey;
            snapshot.name = name;
            snapshot.url = url;
            snapshot.status = status;
            snapshot.deletedAt = lastDeletedAt;
            return snapshot;
        }
    }

    private static final class MediaSnapshot {
        String rootCode = "";
        String recordKey = "";
        String name = "";
        String url = "";
        String status = "";
        String deletedAt = "";

        MediaSnapshot copy() {
            MediaSnapshot copy = new MediaSnapshot();
            copy.rootCode = rootCode;
            copy.recordKey = recordKey;
            copy.name = name;
            copy.url = url;
            copy.status = status;
            copy.deletedAt = deletedAt;
            return copy;
        }

        JSONObject toPhotoJson() {
            JSONObject photo = new JSONObject()
                    .put("PhotoAssetType", rootCode)
                    .put("PhotoAssetName", name)
                    .put("PhotoAssetURL", url);
            if (!status.isEmpty()) {
                photo.put("PhotoAssetStatus", status);
            }
            return photo;
        }
    }

    private static final class RepairCandidate {
        String proposalId = "";
        String variantId = "";
        boolean fullLoss;
        boolean complete;
        String lastDeleteAt = "";
        final List<MediaSnapshot> photos = new ArrayList<MediaSnapshot>();

        JSONObject toLasImagenesPayload() {
            JSONArray photosJson = new JSONArray();
            for (MediaSnapshot photo : photos) {
                photosJson.put(photo.toPhotoJson());
            }

            JSONObject variant = new JSONObject()
                    .put("variantId", variantId)
                    .put("photos", photosJson);

            JSONObject product = new JSONObject()
                    .put("proposalId", proposalId)
                    .put("variants", new JSONArray().put(variant));

            return new JSONObject()
                    .put("replaceAssets", false)
                    .put("repairOnlyIfCurrentNonFinalEmpty", true)
                    .put("repairSource", "PACactiveMQListenerHistoricalRepair")
                    .put("products", new JSONArray().put(product));
        }
    }

    private static final class RepairResult {
        final RepairCandidate candidate;
        String payloadReference = "";
        String status = "";
        String detail = "";
        int httpStatus;

        RepairResult(RepairCandidate candidate) {
            this.candidate = candidate;
        }
    }

    private static final class AnalysisStats {
        long lines;
        long validJsonMessages;
        long articleMessages;
        long mediaPrefilterMessages;
        long validXmlSummaries;
        long xmlErrors;
        long spooledEvents;
        int shardsWithData;
    }

    private static final class ShardStats {
        long events;
        long variants;
        long candidates;
    }

    private static final class ResultCounters {
        long candidates;
        long fullLoss;
        long partialLoss;
        long complete;
        long payloadLines;
        final Map<String, Long> byStatus = new LinkedHashMap<String, Long>();

        void recordCandidate(RepairCandidate candidate) {
            candidates++;
            if (candidate.fullLoss) {
                fullLoss++;
            } else {
                partialLoss++;
            }
            if (candidate.complete) {
                complete++;
            }
        }

        void recordStatus(String status) {
            Long previous = byStatus.get(status);
            byStatus.put(status,
                    Long.valueOf(previous == null ? 1L : previous.longValue() + 1L));
        }
    }

    private static final class Config {
        Path logFile;
        String proposalId;
        Path outputDir = Paths.get("image-repair-output");
        Path spoolDir;
        String serviceUrl = "http://localhost:8080/public/rt/LasImagenes";
        boolean apply;
        boolean keepSpool;
        boolean individualPayloadFiles;
        long deletionWaveSeconds = 120L;
        int shards = 1024;
        int maxOpenShardWriters = 32;
        int inputBufferChars = 1024 * 1024;
        int spoolBufferChars = 256 * 1024;
        long progressLines = 1_000_000L;
        long progressCandidates = 1_000L;
        long flushEveryCandidates = 100L;
        int maxRestAttempts = 5;
        long restRequestTimeoutSeconds = 180L;

        static Config parse(String[] args) {
            Config c = new Config();

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--log".equals(arg)) {
                    c.logFile = Paths.get(value(args, ++i, arg));
                } else if ("--proposal-id".equals(arg)) {
                    c.proposalId = value(args, ++i, arg);
                } else if ("--output-dir".equals(arg)) {
                    c.outputDir = Paths.get(value(args, ++i, arg));
                } else if ("--spool-dir".equals(arg)) {
                    c.spoolDir = Paths.get(value(args, ++i, arg));
                } else if ("--url".equals(arg)) {
                    c.serviceUrl = value(args, ++i, arg);
                } else if ("--wave-seconds".equals(arg)) {
                    c.deletionWaveSeconds = positiveLong(value(args, ++i, arg), arg);
                } else if ("--shards".equals(arg)) {
                    c.shards = positiveInt(value(args, ++i, arg), arg);
                } else if ("--max-open-shards".equals(arg)) {
                    c.maxOpenShardWriters = positiveInt(value(args, ++i, arg), arg);
                } else if ("--progress-lines".equals(arg)) {
                    c.progressLines = nonNegativeLong(value(args, ++i, arg), arg);
                } else if ("--progress-candidates".equals(arg)) {
                    c.progressCandidates = nonNegativeLong(value(args, ++i, arg), arg);
                } else if ("--flush-every".equals(arg)) {
                    c.flushEveryCandidates = positiveLong(value(args, ++i, arg), arg);
                } else if ("--rest-attempts".equals(arg)) {
                    c.maxRestAttempts = positiveInt(value(args, ++i, arg), arg);
                } else if ("--rest-timeout-seconds".equals(arg)) {
                    c.restRequestTimeoutSeconds = positiveLong(value(args, ++i, arg), arg);
                } else if ("--individual-payload-files".equals(arg)) {
                    c.individualPayloadFiles = true;
                } else if ("--keep-spool".equals(arg)) {
                    c.keepSpool = true;
                } else if ("--apply".equals(arg)) {
                    c.apply = true;
                } else if ("--dry-run".equals(arg)) {
                    c.apply = false;
                } else if ("--help".equals(arg) || "-h".equals(arg)) {
                    usageAndExit(0);
                } else {
                    throw new IllegalArgumentException("Argumento desconocido: " + arg);
                }
            }

            if (c.logFile == null) {
                throw new IllegalArgumentException("Falta --log");
            }
            if (c.proposalId == null || c.proposalId.trim().isEmpty()) {
                throw new IllegalArgumentException("Falta --proposal-id");
            }
            if (!Files.isRegularFile(c.logFile)) {
                throw new IllegalArgumentException("No existe el log: " + c.logFile);
            }
            if (c.shards > 65536) {
                throw new IllegalArgumentException("--shards máximo: 65536");
            }
            if (c.maxOpenShardWriters > c.shards) {
                c.maxOpenShardWriters = c.shards;
            }
            if (c.spoolDir == null) {
                c.spoolDir = c.outputDir.resolve(".historical-image-spool");
            }
            return c;
        }

        private static String value(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Falta valor para " + option);
            }
            return args[index];
        }

        private static int positiveInt(String text, String option) {
            int value = Integer.parseInt(text);
            if (value < 1) {
                throw new IllegalArgumentException(option + " debe ser >= 1");
            }
            return value;
        }

        private static long positiveLong(String text, String option) {
            long value = Long.parseLong(text);
            if (value < 1L) {
                throw new IllegalArgumentException(option + " debe ser >= 1");
            }
            return value;
        }

        private static long nonNegativeLong(String text, String option) {
            long value = Long.parseLong(text);
            if (value < 0L) {
                throw new IllegalArgumentException(option + " debe ser >= 0");
            }
            return value;
        }

        private static void usageAndExit(int code) {
            System.out.println("Uso: HistoricalLasImagenesRepair"
                    + " --log FILE --proposal-id ID"
                    + " [--output-dir DIR] [--spool-dir DIR] [--url URL]"
                    + " [--wave-seconds N] [--shards N] [--max-open-shards N]"
                    + " [--progress-lines N] [--progress-candidates N]"
                    + " [--flush-every N] [--rest-attempts N]"
                    + " [--rest-timeout-seconds N] [--individual-payload-files]"
                    + " [--keep-spool] [--apply|--dry-run]");
            System.exit(code);
        }
    }
}
