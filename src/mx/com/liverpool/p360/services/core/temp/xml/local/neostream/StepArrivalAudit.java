package mx.com.liverpool.p360.services.core.temp.xml.local.neostream;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
import mx.com.liverpool.p360.services.core.DBAccessDataStub;

/** One immutable audit per processing attempt, before any P360 data writes.
 * Existence means active Identifier in MASTER, revision 1, for the given entity.
 * A SKU match with another Identifier does not count as Identifier existence.
 */
public final class StepArrivalAudit {
    private StepArrivalAudit() { }

    public static Path write(DBAccessDataStub db,
            StepXmlStreamingParser.StepIndex index, Path source) throws IOException {
        Instant started = Instant.now();
        Map<String, String> products = db.getObjectInternalIds(1100, index.getProductIds(), true);
        Map<String, String> articles = db.getObjectInternalIds(1000, index.getArticleIds(), true);
        Instant completed = Instant.now();
        Path directory = Path.of(System.getProperty("p360.step.arrival.audit.dir",
                Path.of("..", "logs", "neo_step_arrival").toString()));
        return writeSnapshot(directory, source, started, completed,
                index.getProductIds(), index.getProductSkuById(), products,
                index.getArticleIds(), index.getArticleSkuById(), articles);
    }

    static Path writeSnapshot(Path directory, Path source, Instant started, Instant completed,
            Collection<String> productIds, Map<String, String> productSkus, Map<String, String> products,
            Collection<String> articleIds, Map<String, String> articleSkus, Map<String, String> articles)
            throws IOException {
        Files.createDirectories(directory);
        String attempt = UUID.randomUUID().toString();
        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssSSS'Z'")
                .withZone(ZoneOffset.UTC).format(started);
        Path target = directory.resolve("neo-step-arrival-" + stamp + "-" + attempt + ".jsonl");
        Path partial = directory.resolve(target.getFileName() + ".partial");
        JSONObject common = new JSONObject()
                .put("schemaVersion", 1).put("attemptId", attempt)
                .put("sourceFile", source.toAbsolutePath().normalize().toString())
                .put("checkStartedAt", started.toString()).put("checkCompletedAt", completed.toString())
                .put("phase", "BEFORE_P360_WRITES").put("catalogId", 1).put("revisionId", 1);
        try (BufferedWriter writer = Files.newBufferedWriter(partial, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            writeRows(writer, common, "Product2G", productIds, productSkus, products);
            writeRows(writer, common, "Article", articleIds, articleSkus, articles);
            JSONObject summary = new JSONObject(common.toString()).put("recordType", "SUMMARY")
                    .put("products", productIds.size()).put("articles", articleIds.size())
                    .put("productsExisting", productIds.stream().filter(products::containsKey).count())
                    .put("articlesExisting", articleIds.stream().filter(articles::containsKey).count());
            writer.write(summary.toString());
            writer.newLine();
        }
        try (FileChannel file = FileChannel.open(partial, StandardOpenOption.WRITE)) {
            file.force(true);
        }
        Files.move(partial, target);
        return target.toAbsolutePath().normalize();
    }

    private static void writeRows(BufferedWriter writer, JSONObject common, String entity,
            Collection<String> identifiers, Map<String, String> skus, Map<String, String> existing)
            throws IOException {
        for (String identifier : identifiers) {
            boolean exists = existing.containsKey(identifier);
            JSONObject row = new JSONObject(common.toString()).put("recordType", "IDENTIFIER")
                    .put("entity", entity).put("identifier", identifier)
                    .put("sku", skus.getOrDefault(identifier, ""))
                    .put("existedBeforeProcessing", exists)
                    .put("result", exists ? "EXISTIA" : "NO_EXISTIA")
                    .put("p360ObjectId", exists ? existing.get(identifier) : JSONObject.NULL);
            writer.write(row.toString());
            writer.newLine();
        }
    }
}