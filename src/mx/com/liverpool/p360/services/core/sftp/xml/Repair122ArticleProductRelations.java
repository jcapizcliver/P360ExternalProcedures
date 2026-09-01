package mx.com.liverpool.p360.services.core.sftp.xml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

/**
 * Reparacion integral de relaciones Article -> Product2G a partir de respuestas 122.
 *
 * Flujo de una sola ejecucion:
 *   1) Lista EXCLUSIVAMENTE archivos con extension exacta .XML del directorio indicado.
 *   2) Detecta por archivo/producto si el origen es ECC o JANA.
 *   3) Para ATTYP=02 obtiene MATNR y SATNR.
 *   4) Resuelve SIEMPRE:
 *        MATNR -> Article.Identifier   (EntityID 1000)
 *        SATNR -> Product2G.Identifier (EntityID 1100)
 *      directamente en Oracle, sin depender de ArticleReference.
 *   5) ZNPRST (ECC) / PRODUCT_ID (Jana) solo se conserva como sourceId de auditoria
 *      y se compara contra el Product2G actualmente relacionado al MATNR.
 *   6) Si ambos endpoints son unicos:
 *        - sin padre actual           -> crea ProductReference en REST
 *        - ya tiene el padre SATNR    -> ALREADY_CORRECT, no duplica
 *        - tiene otro padre           -> CURRENT_PARENT_DIFFERS, NO agrega segundo padre
 *   7) Si falta Article y/o Product2G para MATNR/SATNR, reenvia el XML de origen por SCP.
 *      Cada archivo se envia COMO MAXIMO UNA VEZ aunque tenga muchas filas faltantes.
 *   8) Los REST se mandan en batches de 900.
 *   9) Genera PSV final y JSONL con los requests REST exactos.
 *
 * Uso:
 *   java ...Repair122ArticleProductRelations <directorioXML> [archivoFinal.psv]
 *
 * JDBC:
 *   Lee P360_SERVER_PROPERTIES o, si no existe, /u01/Informatica/server.properties
 *   usando db.master.pool.jdbcDriver, db.master.pool.jdbcUrl, db.master.user y db.master.password.
 */
public final class Repair122ArticleProductRelations {

    private static final int ENTITY_ARTICLE = 1000;
    private static final int ENTITY_PRODUCT_2G = 1100;
    private static final int REST_BATCH_SIZE = 900;
    private static final int REST_MAX_ATTEMPTS = 3;
    private static final int SCP_MAX_ATTEMPTS = 3;
    private static final long SCP_TIMEOUT_SECONDS = 300L;

    private static final String ECC_SCP_DESTINATION =
            "userp360@172.27.203.6:/interfase/mer/in/step/P360/zrtuab122";
    private static final String JANA_SCP_DESTINATION =
            "userp360@172.18.136.24:/interfase/mer/out/step/P360/zrtuab122";

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final RESTWrapper rw = new RESTWrapper();
    private static final RESTWorkshop workshop = rw.getRw();
    private static final String BASE_URL = workshop.getBaseUrl();

    private Repair122ArticleProductRelations() {
    }

    public static void main(String[] args) {
        int exitCode = 0;
        List<RelationRow> rows = new ArrayList<>();
        Path finalOutput = null;
        Path requestsOutput = null;

        try {
            if (args.length < 1 || isBlank(args[0])) {
                System.err.println("Uso: Repair122ArticleProductRelations <directorioXML> [archivoFinal.psv]");
                System.exit(2);
            }

            Path directory = Paths.get(args[0]).toAbsolutePath().normalize();
            if (!Files.isDirectory(directory)) {
                throw new IllegalArgumentException("No es un directorio: " + directory);
            }

            String stamp = LocalDateTime.now().format(TS);
            finalOutput = args.length > 1 && !isBlank(args[1])
                    ? Paths.get(args[1]).toAbsolutePath().normalize()
                    : Paths.get("repair_122_article_product_" + stamp + ".psv").toAbsolutePath().normalize();
            requestsOutput = siblingWithSuffix(finalOutput, ".requests.jsonl");

            ensureParent(finalOutput);
            ensureParent(requestsOutput);
            workshop.setBaseUrl(BASE_URL);

            List<Path> xmlFiles = listExactXmlFiles(directory);
            Stats stats = new Stats();
            stats.xmlFiles = xmlFiles.size();

            System.out.println("============================================================");
            System.out.println("Repair 122 Article -> Product2G");
            System.out.println("Directorio: " + directory);
            System.out.println("Archivos .XML encontrados: " + xmlFiles.size());
            System.out.println("Batch REST: " + REST_BATCH_SIZE);
            System.out.println("Salida final: " + finalOutput);
            System.out.println("Requests REST: " + requestsOutput);
            System.out.println("============================================================");

            JdbcConfig jdbc = initJdbcConfig();
            Class.forName(jdbc.jdbcDriver);

            Map<Path, SourceSystem> sourceByFile = new LinkedHashMap<>();
            Map<Path, String> parseErrors = new LinkedHashMap<>();

            try (Connection connection = DriverManager.getConnection(jdbc.jdbcUrl, jdbc.user, jdbc.password);
                 Resolver resolver = new Resolver(connection)) {

                connection.setAutoCommit(true);

                SAXParserFactory factory = newSecureSaxParserFactory();

                int fileNo = 0;
                for (Path xml : xmlFiles) {
                    fileNo++;
                    List<RelationRow> fromFile = new ArrayList<>();
                    SourceHolder holder = new SourceHolder();

                    try {
                        SAXParser parser = factory.newSAXParser();
                        parser.parse(xml.toFile(), new Relation122Handler(xml, fromFile, holder));
                    } catch (Exception e) {
                        String message = safeMessage(e);
                        parseErrors.put(xml, message);
                        stats.parseErrors++;
                        System.err.println("ERROR parseando " + xml.getFileName() + ": " + message);
                        continue;
                    }

                    SourceSystem fileSource = holder.source;
                    sourceByFile.put(xml, fileSource);
                    if (fileSource == SourceSystem.ECC) stats.eccFiles++;
                    else if (fileSource == SourceSystem.JANA) stats.janaFiles++;
                    else stats.unknownFiles++;

                    for (RelationRow row : fromFile) {
                        evaluateRow(row, resolver, stats);
                        rows.add(row);
                    }

                    if (fileNo % 50 == 0 || fileNo == xmlFiles.size()) {
                        System.out.println(fileNo + "/" + xmlFiles.size()
                                + " XML; ATTYP=02 acumulados=" + rows.size()
                                + "; candidatos REST=" + countAction(rows, "READY_REST")
                                + "; archivos candidatos SCP=" + countFilesNeedingScp(rows));
                    }
                }

                // Persistimos la fotografia de preflight antes de cualquier escritura REST/SCP.
                writeFinal(finalOutput, rows, Collections.emptyMap());

                prepareRestCandidates(rows, stats);
                writeFinal(finalOutput, rows, Collections.emptyMap());

                sendRestBatches(rows, requestsOutput, stats);
                writeFinal(finalOutput, rows, Collections.emptyMap());
            }

            Map<Path, ScpResult> scpResults = sendMissingFilesByScp(rows, sourceByFile, stats);
            writeFinal(finalOutput, rows, scpResults);

            printSummary(stats, rows, parseErrors, scpResults, finalOutput, requestsOutput);

            if (stats.restFailedRows > 0 || stats.scpFailedFiles > 0 || stats.parseErrors > 0) {
                exitCode = 1;
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
            exitCode = 1;
            if (finalOutput != null && !rows.isEmpty()) {
                try {
                    writeFinal(finalOutput, rows, Collections.emptyMap());
                    System.err.println("Se escribio estado parcial en: " + finalOutput);
                } catch (Exception ignored) {
                    ignored.printStackTrace(System.err);
                }
            }
        }

        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private static void evaluateRow(RelationRow row, Resolver resolver, Stats stats) {
        stats.attyp02Rows++;

        if (isBlank(row.matnr) || isBlank(row.satnr)) {
            row.targetState = "INVALID_MATERIALS_IN_XML";
            row.relationAction = "SKIPPED_INVALID_XML_MATERIALS";
            appendDetail(row, "ATTYP=02 requiere MATNR y SATNR no vacios");
            stats.invalidMaterialRows++;
            return;
        }

        try {
            // La verdad de la relacion sale SIEMPRE de MATNR y SATNR.
            // Primero resolvemos endpoints por SKU usando IX_AD_TUNE_01.
            row.articleIdentifiers = resolver.identifiersBySku(row.matnr, ENTITY_ARTICLE);
            row.productIdentifiers = resolver.identifiersBySku(row.satnr, ENTITY_PRODUCT_2G);

            // Solo consultamos ArticleReference cuando MATNR resolvio a UN Article.
            // Asi evitamos repetir ArticleDetail/Res_Int_02 para averiguar el padre actual.
            if (row.articleIdentifiers.size() == 1) {
                row.articleIdentifier = row.articleIdentifiers.get(0);
                row.currentParentIdentifiers =
                        resolver.currentParentsByArticleIdentifier(row.articleIdentifier);
            }
        } catch (RuntimeException e) {
            row.targetState = "DB_LOOKUP_ERROR";
            row.relationAction = "SKIPPED_DB_LOOKUP_ERROR";
            appendDetail(row, safeMessage(e));
            stats.dbLookupErrors++;
            return;
        }

        row.sourceIdVsCurrentParent = compareSourceToCurrentParent(row.sourceId, row.currentParentIdentifiers);

        boolean noArticle = row.articleIdentifiers.isEmpty();
        boolean noProduct = row.productIdentifiers.isEmpty();

        if (noArticle || noProduct) {
            row.targetState = noArticle && noProduct
                    ? "MISSING_BOTH_IDS"
                    : noArticle ? "MISSING_ARTICLE_ID" : "MISSING_PRODUCT_ID";
            row.relationAction = "REQUEUE_SOURCE_FILE";
            row.requiresScp = true;

            if (noArticle && noProduct) stats.missingBothIds++;
            else if (noArticle) stats.missingArticleId++;
            else stats.missingProductId++;

            appendDetail(row, "No se puede crear relacion hasta que existan ambos endpoints");
            return;
        }

        if (row.articleIdentifiers.size() != 1 || row.productIdentifiers.size() != 1) {
            row.targetState = "AMBIGUOUS_ENDPOINT_IDS";
            row.relationAction = "SKIPPED_AMBIGUOUS_ENDPOINT_IDS";
            stats.ambiguousEndpointRows++;
            appendDetail(row, "Article IDs=" + join(row.articleIdentifiers)
                    + "; Product2G IDs=" + join(row.productIdentifiers));
            return;
        }

        // articleIdentifier ya quedo asignado arriba al ser unico.
        row.productIdentifier = row.productIdentifiers.get(0);

        if (row.currentParentIdentifiers.isEmpty()) {
            row.targetState = "NEEDS_RELATION";
            row.relationAction = "READY_REST";
            stats.readyBeforeDedup++;
            return;
        }

        if (row.currentParentIdentifiers.size() == 1) {
            String current = row.currentParentIdentifiers.get(0);
            if (current.equals(row.productIdentifier)) {
                row.targetState = "ALREADY_CORRECT";
                row.relationAction = "ALREADY_CORRECT_NO_SEND";
                stats.alreadyCorrect++;
            } else {
                // Igual que la conciliacion conservadora de los parsers: no agregar un segundo padre.
                row.targetState = "CURRENT_PARENT_DIFFERS";
                row.relationAction = "SKIPPED_CURRENT_PARENT_DIFFERS";
                appendDetail(row, "Actual=" + current + "; SATNR resuelve a=" + row.productIdentifier);
                stats.currentParentDiffers++;
            }
            return;
        }

        row.targetState = "MULTIPLE_CURRENT_PARENTS";
        row.relationAction = "SKIPPED_MULTIPLE_CURRENT_PARENTS";
        appendDetail(row, "Padres actuales=" + join(row.currentParentIdentifiers));
        stats.multipleCurrentParents++;
    }

    private static void prepareRestCandidates(List<RelationRow> rows, Stats stats) {
        Map<String, Set<String>> targetsByArticle = new LinkedHashMap<>();

        for (RelationRow row : rows) {
            if (!"READY_REST".equals(row.relationAction)) continue;
            targetsByArticle
                    .computeIfAbsent(row.articleIdentifier, key -> new LinkedHashSet<>())
                    .add(row.productIdentifier);
        }

        for (RelationRow row : rows) {
            if (!"READY_REST".equals(row.relationAction)) continue;
            Set<String> targets = targetsByArticle.get(row.articleIdentifier);
            if (targets != null && targets.size() > 1) {
                row.targetState = "CONFLICTING_TARGETS_IN_INPUT";
                row.relationAction = "SKIPPED_CONFLICTING_TARGETS";
                appendDetail(row, "El mismo Article apunta a varios SATNR/Product2G: " + String.join(",", targets));
                stats.conflictingTargetRows++;
            }
        }

        Map<String, RelationRow> firstByRelation = new LinkedHashMap<>();
        for (RelationRow row : rows) {
            if (!"READY_REST".equals(row.relationAction)) continue;
            String key = row.articleIdentifier + "\u0000" + row.productIdentifier;
            RelationRow previous = firstByRelation.putIfAbsent(key, row);
            if (previous != null) {
                row.relationAction = "DUPLICATE_SAME_RELATION";
                appendDetail(row, "Relacion ya representada por " + previous.sourceFile.getFileName()
                        + " MATNR=" + previous.matnr + " SATNR=" + previous.satnr);
                stats.duplicateRelationRows++;
            }
        }
    }

    private static void sendRestBatches(List<RelationRow> rows, Path requestsOutput, Stats stats) throws IOException {
        List<RelationRow> ready = rows.stream()
                .filter(row -> "READY_REST".equals(row.relationAction))
                .collect(Collectors.toList());

        try (BufferedWriter requestWriter = Files.newBufferedWriter(
                requestsOutput,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {

            int batchNo = 0;
            for (int offset = 0; offset < ready.size(); offset += REST_BATCH_SIZE) {
                int end = Math.min(offset + REST_BATCH_SIZE, ready.size());
                List<RelationRow> batch = ready.subList(offset, end);
                batchNo++;

                JSONObject request = buildRestRequest(batch);
                Map<String, String> qp = new HashMap<>();
                qp.put("includeObjectsInProtocol", "false");

                JSONObject envelope = new JSONObject()
                        .put("batch", batchNo)
                        .put("rowCount", batch.size())
                        .put("operation", "list")
                        .put("entity", "Article")
                        .put("subEntity", "ProductReference")
                        .put("request", request);

                requestWriter.write(envelope.toString());
                requestWriter.newLine();
                requestWriter.flush();

                boolean success = false;
                RuntimeException lastError = null;

                for (int attempt = 1; attempt <= REST_MAX_ATTEMPTS && !success; attempt++) {
                    System.out.println("REST batch " + batchNo + " intento " + attempt + "/" + REST_MAX_ATTEMPTS
                            + " relaciones=" + batch.size());
                    try {
                        rw.writeData(
                                "list",
                                "Article",
                                "ProductReference",
                                qp,
                                request,
                                Repair122ArticleProductRelations::restLog);
                        success = true;
                    } catch (RuntimeException e) {
                        lastError = e;
                        System.err.println("REST batch " + batchNo + " fallo: " + safeMessage(e));
                        if (attempt < REST_MAX_ATTEMPTS) sleepQuietly(5000L);
                    }
                }

                if (success) {
                    for (RelationRow row : batch) {
                        row.relationAction = "REST_SUBMITTED";
                        row.batch = batchNo;
                        appendDetail(row, "RESTWrapper retorno sin lanzar excepcion");
                    }
                    stats.restSubmitted += batch.size();
                } else {
                    for (RelationRow row : batch) {
                        row.relationAction = "REST_FAILED_AFTER_RETRIES";
                        row.batch = batchNo;
                        appendDetail(row, safeMessage(lastError));
                    }
                    stats.restFailedRows += batch.size();
                }
            }
        }
    }

    private static JSONObject buildRestRequest(List<RelationRow> batch) {
        JSONArray columns = new JSONArray()
                .put(new JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid"));
        JSONArray requestRows = new JSONArray();

        for (RelationRow row : batch) {
            requestRows.put(new JSONObject()
                    .put("object", new JSONObject().put("id", "'" + row.articleIdentifier + "'@1"))
                    .put("qualification", new JSONObject().put("referencedSupplierAid", row.productIdentifier))
                    .put("values", new JSONArray().put(row.productIdentifier)));
        }

        return new JSONObject()
                .put("columns", columns)
                .put("rows", requestRows);
    }

    private static Map<Path, ScpResult> sendMissingFilesByScp(
            List<RelationRow> rows,
            Map<Path, SourceSystem> sourceByFile,
            Stats stats) {

        // LinkedHashMap/Path => un archivo se manda como maximo una vez.
        Map<Path, SourceSystem> uniqueFiles = new LinkedHashMap<>();
        for (RelationRow row : rows) {
            if (!row.requiresScp) continue;
            SourceSystem source = row.source != SourceSystem.UNKNOWN
                    ? row.source
                    : sourceByFile.getOrDefault(row.sourceFile, SourceSystem.UNKNOWN);
            uniqueFiles.putIfAbsent(row.sourceFile, source);
        }

        stats.scpQueuedFiles = uniqueFiles.size();
        Map<Path, ScpResult> results = new LinkedHashMap<>();

        int current = 0;
        for (Map.Entry<Path, SourceSystem> entry : uniqueFiles.entrySet()) {
            current++;
            Path sourceFile = entry.getKey();
            SourceSystem sourceSystem = entry.getValue();

            String destination = sourceSystem == SourceSystem.ECC
                    ? ECC_SCP_DESTINATION
                    : sourceSystem == SourceSystem.JANA ? JANA_SCP_DESTINATION : null;

            if (destination == null) {
                ScpResult result = new ScpResult(false, 0, "UNKNOWN_SOURCE", "No se pudo determinar ECC/JANA");
                results.put(sourceFile, result);
                stats.scpFailedFiles++;
                System.err.println("SCP omitido por origen desconocido: " + sourceFile);
                continue;
            }

            System.out.println("SCP " + current + "/" + uniqueFiles.size() + " [" + sourceSystem + "] "
                    + sourceFile.getFileName() + " -> " + destination);

            ScpResult result = runScpWithRetries(sourceFile, destination);
            results.put(sourceFile, result);
            if (result.success) {
                stats.scpSuccessFiles++;
                if (sourceSystem == SourceSystem.ECC) stats.scpSuccessEcc++;
                if (sourceSystem == SourceSystem.JANA) stats.scpSuccessJana++;
            } else {
                stats.scpFailedFiles++;
                if (sourceSystem == SourceSystem.ECC) stats.scpFailedEcc++;
                if (sourceSystem == SourceSystem.JANA) stats.scpFailedJana++;
            }
        }

        return results;
    }

    private static ScpResult runScpWithRetries(Path source, String destination) {
        String lastOutput = "";
        int lastExit = -1;

        for (int attempt = 1; attempt <= SCP_MAX_ATTEMPTS; attempt++) {
            Process process = null;
            try {
                // Sin shell: no hay problemas de quoting y se usa la relacion de confianza existente.
                ProcessBuilder pb = new ProcessBuilder("scp", source.toString(), destination);
                pb.redirectErrorStream(true);
                process = pb.start();

                boolean finished = process.waitFor(SCP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    lastOutput = "Timeout de " + SCP_TIMEOUT_SECONDS + " segundos";
                    lastExit = -1;
                } else {
                    lastExit = process.exitValue();
                    lastOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
                    if (lastExit == 0) {
                        return new ScpResult(true, attempt, "SCP_SENT", lastOutput);
                    }
                }
            } catch (Exception e) {
                lastOutput = safeMessage(e);
                lastExit = -1;
                if (process != null) process.destroyForcibly();
            }

            System.err.println("SCP intento " + attempt + "/" + SCP_MAX_ATTEMPTS + " fallo para "
                    + source.getFileName() + " exit=" + lastExit + " " + lastOutput);
            if (attempt < SCP_MAX_ATTEMPTS) sleepQuietly(5000L);
        }

        return new ScpResult(false, SCP_MAX_ATTEMPTS, "SCP_FAILED", "exit=" + lastExit + " " + lastOutput);
    }

    private static void writeFinal(Path output, List<RelationRow> rows, Map<Path, ScpResult> scpResults)
            throws IOException {
        ensureParent(output);
        try (BufferedWriter writer = Files.newBufferedWriter(
                output,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {

            writer.write("source|sourceFile|sourceId|MATNR|SATNR|matnrIdentifier|satnrIdentifier"
                    + "|currentParentIdentifier|sourceIdVsCurrentParent|targetRelationState"
                    + "|relationAction|batch|fileScpStatus|detail");
            writer.newLine();

            for (RelationRow row : rows) {
                ScpResult scp = scpResults.get(row.sourceFile);
                String scpStatus;
                if (scp != null) {
                    scpStatus = scp.status + "(attempts=" + scp.attempts + ")";
                } else if (row.requiresScp) {
                    scpStatus = "PENDING_OR_NOT_ATTEMPTED";
                } else {
                    scpStatus = "NOT_REQUIRED";
                }

                writer.write(psv(row.source.name()));
                writer.write('|');
                writer.write(psv(row.sourceFile.toString()));
                writer.write('|');
                writer.write(psv(row.sourceId));
                writer.write('|');
                writer.write(psv(row.matnr));
                writer.write('|');
                writer.write(psv(row.satnr));
                writer.write('|');
                writer.write(psv(!isBlank(row.articleIdentifier) ? row.articleIdentifier : join(row.articleIdentifiers)));
                writer.write('|');
                writer.write(psv(!isBlank(row.productIdentifier) ? row.productIdentifier : join(row.productIdentifiers)));
                writer.write('|');
                writer.write(psv(join(row.currentParentIdentifiers)));
                writer.write('|');
                writer.write(psv(row.sourceIdVsCurrentParent));
                writer.write('|');
                writer.write(psv(row.targetState));
                writer.write('|');
                writer.write(psv(row.relationAction));
                writer.write('|');
                writer.write(row.batch > 0 ? String.valueOf(row.batch) : "");
                writer.write('|');
                writer.write(psv(scpStatus));
                writer.write('|');
                String detail = row.detail;
                if (scp != null && !isBlank(scp.output)) {
                    detail = append(detail, "SCP: " + scp.output);
                }
                writer.write(psv(detail));
                writer.newLine();
            }
        }
    }

    private static void printSummary(
            Stats stats,
            List<RelationRow> rows,
            Map<Path, String> parseErrors,
            Map<Path, ScpResult> scpResults,
            Path finalOutput,
            Path requestsOutput) {

        System.out.println();
        System.out.println("===================== RESUMEN FINAL =====================");
        System.out.println("XML encontrados: " + stats.xmlFiles);
        System.out.println("  ECC: " + stats.eccFiles);
        System.out.println("  JANA: " + stats.janaFiles);
        System.out.println("  UNKNOWN: " + stats.unknownFiles);
        System.out.println("  Errores de parseo: " + stats.parseErrors);
        System.out.println();
        System.out.println("Filas ATTYP=02: " + stats.attyp02Rows);
        System.out.println("  MATNR/SATNR invalidos o vacios: " + stats.invalidMaterialRows);
        System.out.println("  Sin Article ID (MATNR): " + stats.missingArticleId);
        System.out.println("  Sin Product2G ID (SATNR): " + stats.missingProductId);
        System.out.println("  Sin ambos IDs: " + stats.missingBothIds);
        System.out.println("  Endpoints ambiguos: " + stats.ambiguousEndpointRows);
        System.out.println("  Errores lookup BD: " + stats.dbLookupErrors);
        System.out.println();
        System.out.println("Relaciones:");
        System.out.println("  Ya correctas, no reenviadas: " + stats.alreadyCorrect);
        System.out.println("  Padre actual distinto, no tocadas: " + stats.currentParentDiffers);
        System.out.println("  Multiples padres actuales, no tocadas: " + stats.multipleCurrentParents);
        System.out.println("  Candidatas antes de dedupe: " + stats.readyBeforeDedup);
        System.out.println("  Duplicados exactos eliminados: " + stats.duplicateRelationRows);
        System.out.println("  Conflictos Article -> varios targets: " + stats.conflictingTargetRows);
        System.out.println("  Entregadas a RESTWrapper: " + stats.restSubmitted);
        System.out.println("  Fallidas tras retries REST: " + stats.restFailedRows);
        System.out.println();
        System.out.println("Reenvio SCP de XML con endpoint faltante:");
        System.out.println("  Archivos unicos en cola: " + stats.scpQueuedFiles);
        System.out.println("  Enviados correctamente: " + stats.scpSuccessFiles
                + " (ECC=" + stats.scpSuccessEcc + ", JANA=" + stats.scpSuccessJana + ")");
        System.out.println("  Fallidos: " + stats.scpFailedFiles
                + " (ECC=" + stats.scpFailedEcc + ", JANA=" + stats.scpFailedJana + ")");
        System.out.println();

        if (!parseErrors.isEmpty()) {
            System.out.println("XML con error de parseo:");
            for (Map.Entry<Path, String> e : parseErrors.entrySet()) {
                System.out.println("  " + e.getKey().getFileName() + " -> " + e.getValue());
            }
            System.out.println();
        }

        if (!scpResults.isEmpty()) {
            System.out.println("Resultado SCP por archivo:");
            for (Map.Entry<Path, ScpResult> e : scpResults.entrySet()) {
                System.out.println("  " + e.getKey().getFileName() + " -> " + e.getValue().status
                        + " attempts=" + e.getValue().attempts);
            }
            System.out.println();
        }

        System.out.println("Archivo final: " + finalOutput);
        System.out.println("Requests REST exactos: " + requestsOutput);
        System.out.println("Nota REST: REST_SUBMITTED significa que writeData retorno sin lanzar excepcion.");
        System.out.println("=========================================================");
    }

    private static List<Path> listExactXmlFiles(Path directory) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".XML"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .collect(Collectors.toList());
        }
    }

    private static SAXParserFactory newSecureSaxParserFactory() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {
        }
        return factory;
    }

    private static final class Relation122Handler extends DefaultHandler {
        private final Path sourceFile;
        private final List<RelationRow> output;
        private final SourceHolder holder;
        private final LinkedList<ProductState> stack = new LinkedList<>();

        private String currentAttributeId;
        private StringBuilder currentText;

        private Relation122Handler(Path sourceFile, List<RelationRow> output, SourceHolder holder) {
            this.sourceFile = sourceFile;
            this.output = output;
            this.holder = holder;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String name = elementName(localName, qName);

            if ("Product".equals(name)) {
                ProductState state = new ProductState();

                if (attributes.getValue("ZNPRST") != null) {
                    state.source = SourceSystem.ECC;
                    state.productAttributeSourceId = trim(attributes.getValue("ZNPRST"));
                } else if (attributes.getValue("EAN11_EAN") != null) {
                    state.source = SourceSystem.JANA;
                }

                if (holder.source == SourceSystem.UNKNOWN && state.source != SourceSystem.UNKNOWN) {
                    holder.source = state.source;
                }
                stack.addLast(state);
                return;
            }

            if ("Value".equals(name) && !stack.isEmpty()) {
                currentAttributeId = trim(attributes.getValue("AttributeID"));
                currentText = new StringBuilder();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (currentText != null) currentText.append(ch, start, length);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String name = elementName(localName, qName);

            if ("Value".equals(name) && !stack.isEmpty() && currentAttributeId != null) {
                stack.getLast().values.put(currentAttributeId, trim(currentText == null ? "" : currentText.toString()));
                currentAttributeId = null;
                currentText = null;
                return;
            }

            if ("Product".equals(name) && !stack.isEmpty()) {
                ProductState state = stack.removeLast();
                finishProduct(state);
            }
        }

        private void finishProduct(ProductState state) {
            if (state.source == SourceSystem.UNKNOWN) {
                // ECC tambien puede contener PRODUCT_ID vacio, por eso ZNPRST tiene prioridad.
                if (state.values.containsKey("ZNPRST")) state.source = SourceSystem.ECC;
                else if (state.values.containsKey("PRODUCT_ID")) state.source = SourceSystem.JANA;
            }

            if (holder.source == SourceSystem.UNKNOWN && state.source != SourceSystem.UNKNOWN) {
                holder.source = state.source;
            }

            String attyp = trim(state.values.get("ATTYP"));
            if (!"02".equals(attyp)) return;

            RelationRow row = new RelationRow();
            row.sourceFile = sourceFile;
            row.source = state.source;
            row.sourceId = state.source == SourceSystem.ECC
                    ? firstNotBlank(state.productAttributeSourceId, state.values.get("ZNPRST"))
                    : state.source == SourceSystem.JANA ? trim(state.values.get("PRODUCT_ID")) : "";

            row.matnr = normalizeMaterial(state.values.get("MATNR"), state.source);
            row.satnr = normalizeMaterial(state.values.get("SATNR"), state.source);
            row.targetState = "PARSED";
            row.relationAction = "PENDING_DB_LOOKUP";
            output.add(row);
        }
    }

    private static final class ProductState {
        private SourceSystem source = SourceSystem.UNKNOWN;
        private String productAttributeSourceId = "";
        private final Map<String, String> values = new LinkedHashMap<>();
    }

    private static final class SourceHolder {
        private SourceSystem source = SourceSystem.UNKNOWN;
    }

    private enum SourceSystem {
        ECC,
        JANA,
        UNKNOWN
    }

    private static final class Resolver implements AutoCloseable {
        private final PreparedStatement identifiersBySku;
        private final PreparedStatement currentParentsByArticleIdentifier;
        private final Map<String, List<String>> idCache = new HashMap<>();
        private final Map<String, List<String>> currentParentCache = new HashMap<>();

        private Resolver(Connection connection) throws SQLException {
            /*
             * Ruta deliberada:
             *   ArticleDetail.Res_Int_02 -> IX_AD_TUNE_01
             *   ArticleDetail.ArticleRevisionID -> ArticleRevision.ID -> PK_ArticleRevision
             *
             * No permitimos que Oracle empiece por ArticleRevision y filtre ArticleDetail despues.
             */
            identifiersBySku = connection.prepareStatement(
                    " select /*+ leading(ad ar) "
                    + "           use_nl(ar) "
                    + "           index(ad IX_AD_TUNE_01) "
                    + "           index(ar PK_ArticleRevision) */ distinct "
                    + "        ar.\"Identifier\" "
                    + " from \"ArticleDetail\" ad "
                    + " inner join \"ArticleRevision\" ar "
                    + "    on ar.ID = ad.\"ArticleRevisionID\" "
                    + "   and ar.\"EntityID\" = ? "
                    + "   and ar.\"RevisionID\" = 1 "
                    + "   and ar.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
                    + " where ad.\"Res_Int_02\" = ? "
                    + "   and ad.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
                    + " order by ar.\"Identifier\"");

            /*
             * Ya conocemos Article.Identifier por la consulta anterior.
             * No volvemos a pasar por ArticleDetail/MATNR para obtener el padre:
             *
             *   ArticleRevision.Identifier -> IX_AR_TUNE_01
             *   ArticleRevision.ID         -> ArticleReference.ArticleRevisionID
             *                               -> XAK1_ArticleReference
             *
             * RefExtArtIdentifier ES el Identifier del Product2G referenciado, por lo que
             * para comparar la relacion actual no necesitamos volver a unir parent_ar.
             */
            currentParentsByArticleIdentifier = connection.prepareStatement(
                    " select /*+ leading(article_ar article_ref) "
                    + "           use_nl(article_ref) "
                    + "           index(article_ar IX_AR_TUNE_01) "
                    + "           index(article_ref XAK1_ArticleReference) */ distinct "
                    + "        article_ref.\"RefExtArtIdentifier\" "
                    + " from \"ArticleRevision\" article_ar "
                    + " inner join \"ArticleReference\" article_ref "
                    + "    on article_ref.\"ArticleRevisionID\" = article_ar.ID "
                    + "   and article_ref.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
                    + " where article_ar.\"Identifier\" = ? "
                    + "   and article_ar.\"EntityID\" = 1000 "
                    + "   and article_ar.\"RevisionID\" = 1 "
                    + "   and article_ar.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' "
                    + " order by article_ref.\"RefExtArtIdentifier\"");
        }

        private List<String> identifiersBySku(String sku, int entityId) {
            String key = entityId + "|" + sku;
            List<String> cached = idCache.get(key);
            if (cached != null) return cached;

            List<String> result = new ArrayList<>();
            try {
                long numericSku = Long.parseLong(sku);
                identifiersBySku.clearParameters();
                identifiersBySku.setInt(1, entityId);
                // Res_Int_02 es numerico; bind numerico evita conversion implicita y conserva
                // la ruta por IX_AD_TUNE_01.
                identifiersBySku.setLong(2, numericSku);
                try (ResultSet rs = identifiersBySku.executeQuery()) {
                    while (rs.next()) {
                        String id = trim(rs.getString(1));
                        if (!isBlank(id)) result.add(id);
                    }
                }
            } catch (SQLException | NumberFormatException e) {
                throw new RuntimeException("Lookup identifier por SKU fallo. entityId=" + entityId + " sku=" + sku, e);
            }

            result = immutableDistinct(result);
            idCache.put(key, result);
            return result;
        }

        private List<String> currentParentsByArticleIdentifier(String articleIdentifier) {
            List<String> cached = currentParentCache.get(articleIdentifier);
            if (cached != null) return cached;

            List<String> result = new ArrayList<>();
            try {
                currentParentsByArticleIdentifier.clearParameters();
                currentParentsByArticleIdentifier.setString(1, articleIdentifier);
                try (ResultSet rs = currentParentsByArticleIdentifier.executeQuery()) {
                    while (rs.next()) {
                        String id = trim(rs.getString(1));
                        if (!isBlank(id)) result.add(id);
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(
                        "Lookup padre actual por Article.Identifier fallo. identifier=" + articleIdentifier, e);
            }

            result = immutableDistinct(result);
            currentParentCache.put(articleIdentifier, result);
            return result;
        }

        @Override
        public void close() {
            try { identifiersBySku.close(); } catch (Exception ignored) {}
            try { currentParentsByArticleIdentifier.close(); } catch (Exception ignored) {}
        }
    }

    private static List<String> immutableDistinct(List<String> input) {
        if (input == null || input.isEmpty()) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<>(new LinkedHashSet<>(input)));
    }

    private static String compareSourceToCurrentParent(String sourceId, List<String> currentParents) {
        if (isBlank(sourceId)) return "NO_SOURCE_ID";
        if (currentParents == null || currentParents.isEmpty()) return "NO_CURRENT_PARENT";
        if (currentParents.size() > 1) {
            return currentParents.contains(sourceId) ? "MATCH_AMONG_MULTIPLE" : "DIFFERENT_MULTIPLE";
        }
        return sourceId.equals(currentParents.get(0)) ? "MATCH" : "DIFFERENT";
    }

    private static JdbcConfig initJdbcConfig() throws IOException {
        Path propertiesPath = resolveServerPropertiesPath();
        Properties raw = new Properties();
        try (InputStream in = Files.newInputStream(propertiesPath)) {
            raw.load(in);
        }

        JdbcConfig config = new JdbcConfig();
        config.jdbcDriver = resolveRequiredProperty(raw, "db.master.pool.jdbcDriver");
        config.jdbcUrl = resolveRequiredProperty(raw, "db.master.pool.jdbcUrl");
        config.user = resolveRequiredProperty(raw, "db.master.user");
        config.password = resolveRequiredProperty(raw, "db.master.password");
        return config;
    }

    private static Path resolveServerPropertiesPath() {
        String path = System.getenv("P360_SERVER_PROPERTIES");
        if (isBlank(path)) path = "/u01/Informatica/server.properties";

        Path resolved = Paths.get(path).toAbsolutePath().normalize();
        if (!Files.exists(resolved)) {
            throw new IllegalArgumentException("No existe server.properties en: " + resolved);
        }
        if (!Files.isRegularFile(resolved)) {
            throw new IllegalArgumentException("La ruta no es archivo: " + resolved);
        }
        return resolved;
    }

    private static String resolveRequiredProperty(Properties raw, String key) {
        String value = resolvePropertyValue(raw, key, new HashSet<String>());
        if (isBlank(value)) {
            throw new IllegalArgumentException("No se encontro la property requerida: " + key);
        }
        return value.trim();
    }

    private static String resolvePropertyValue(Properties raw, String key, Set<String> visiting) {
        if (!visiting.add(key)) {
            throw new IllegalArgumentException("Referencia circular detectada en properties para: " + key);
        }

        String value = raw.getProperty(key);
        if (value == null) {
            visiting.remove(key);
            return null;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String referencedKey = matcher.group(1);
            String referencedValue = resolvePropertyValue(raw, referencedKey, visiting);
            if (referencedValue == null) {
                throw new IllegalArgumentException("No se pudo resolver property referenciada: " + referencedKey);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(referencedValue));
        }
        matcher.appendTail(sb);
        visiting.remove(key);
        return sb.toString();
    }

    private static final class JdbcConfig {
        private String jdbcDriver;
        private String jdbcUrl;
        private String user;
        private String password;
    }

    private static final class RelationRow {
        private SourceSystem source = SourceSystem.UNKNOWN;
        private Path sourceFile;
        private String sourceId = "";
        private String matnr = "";
        private String satnr = "";
        private List<String> articleIdentifiers = Collections.emptyList();
        private List<String> productIdentifiers = Collections.emptyList();
        private List<String> currentParentIdentifiers = Collections.emptyList();
        private String articleIdentifier = "";
        private String productIdentifier = "";
        private String sourceIdVsCurrentParent = "";
        private String targetState = "";
        private String relationAction = "";
        private int batch;
        private boolean requiresScp;
        private String detail = "";
    }

    private static final class ScpResult {
        private final boolean success;
        private final int attempts;
        private final String status;
        private final String output;

        private ScpResult(boolean success, int attempts, String status, String output) {
            this.success = success;
            this.attempts = attempts;
            this.status = status;
            this.output = output == null ? "" : output;
        }
    }

    private static final class Stats {
        private int xmlFiles;
        private int eccFiles;
        private int janaFiles;
        private int unknownFiles;
        private int parseErrors;
        private int attyp02Rows;
        private int invalidMaterialRows;
        private int missingArticleId;
        private int missingProductId;
        private int missingBothIds;
        private int ambiguousEndpointRows;
        private int dbLookupErrors;
        private int alreadyCorrect;
        private int currentParentDiffers;
        private int multipleCurrentParents;
        private int readyBeforeDedup;
        private int duplicateRelationRows;
        private int conflictingTargetRows;
        private int restSubmitted;
        private int restFailedRows;
        private int scpQueuedFiles;
        private int scpSuccessFiles;
        private int scpFailedFiles;
        private int scpSuccessEcc;
        private int scpSuccessJana;
        private int scpFailedEcc;
        private int scpFailedJana;
    }

    private static Path siblingWithSuffix(Path base, String suffix) {
        String name = base.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        Path parent = base.getParent();
        return (parent == null ? Paths.get(stem + suffix) : parent.resolve(stem + suffix))
                .toAbsolutePath().normalize();
    }

    private static void ensureParent(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) Files.createDirectories(parent);
    }

    private static int countAction(List<RelationRow> rows, String action) {
        int count = 0;
        for (RelationRow row : rows) if (action.equals(row.relationAction)) count++;
        return count;
    }

    private static int countFilesNeedingScp(List<RelationRow> rows) {
        Set<Path> files = new HashSet<>();
        for (RelationRow row : rows) if (row.requiresScp) files.add(row.sourceFile);
        return files.size();
    }

    private static String normalizeMaterial(String value, SourceSystem source) {
        String normalized = trim(value);
        if (source == SourceSystem.JANA && !normalized.isEmpty()) {
            normalized = normalized.replaceFirst("^0+(?!$)", "");
        }
        return normalized;
    }

    private static String elementName(String localName, String qName) {
        return localName != null && !localName.isEmpty() ? localName : qName;
    }

    private static String firstNotBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (!isBlank(value) && !"null".equalsIgnoreCase(value)) return value.trim();
        }
        return "";
    }

    private static String join(List<String> values) {
        return values == null || values.isEmpty() ? "" : String.join(",", values);
    }

    private static String psv(String value) {
        if (value == null) return "";
        return value.replace("|", " ")
                .replace('\r', ' ')
                .replace('\n', ' ');
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim());
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static void appendDetail(RelationRow row, String message) {
        row.detail = append(row.detail, message);
    }

    private static String append(String current, String message) {
        if (isBlank(message)) return current == null ? "" : current;
        if (isBlank(current)) return message;
        return current + "; " + message;
    }

    private static String safeMessage(Throwable t) {
        if (t == null) return "";
        String message = t.getMessage();
        if (isBlank(message) && t.getCause() != null) message = t.getCause().getMessage();
        return isBlank(message) ? t.getClass().getName() : message;
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void restLog(String message) {
        System.out.println("[REST] " + message);
    }
}
