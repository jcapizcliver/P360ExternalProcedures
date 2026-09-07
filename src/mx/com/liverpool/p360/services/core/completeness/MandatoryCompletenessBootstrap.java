package mx.com.liverpool.p360.services.core.completeness;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager;

/**
 * Inicialización histórica de Product2G.MandatoryCompleteness.
 *
 * Uso recomendado inicial:
 *   java ...MandatoryCompletenessBootstrap --dry-run=true --batch-size=1000 --max-products=1000
 *
 * Después de comparar resultados contra Mandatory Data / consulta validadora:
 *   java ...MandatoryCompletenessBootstrap --dry-run=false --batch-size=10000 --rest-batch-size=1000
 *
 * No mantiene una transacción Oracle abierta mientras llama a la List API.
 */
public final class MandatoryCompletenessBootstrap {

    private static final int DEFAULT_BATCH_SIZE = 10_000;
    private static final int DEFAULT_REST_BATCH_SIZE = 1_000;

    public static void main(String[] args) throws Exception {
        Config config = Config.parse(args);
        MandatoryCompletenessPreflight.checkConfiguration();
        String bootstrapRunId = UUID.randomUUID().toString();

        System.out.println("Mandatory Completeness bootstrap starting"
                + " runId=" + bootstrapRunId
                + " dryRun=" + config.dryRun
                + " batchSize=" + config.batchSize
                + " restBatchSize=" + config.restBatchSize
                + " maxProducts=" + config.maxProducts
                + " startAfterRevisionId=" + config.startAfterRevisionId);

        QuickJdbcConnectionManager cm = new QuickJdbcConnectionManager();
        try (Connection sourceConnection = cm.openConnection(true);
             Connection workConnection = cm.openConnection(true)) {

            sourceConnection.setAutoCommit(true);
            workConnection.setAutoCommit(false);
            MandatoryCompletenessPreflight.checkDatabase(workConnection);

            CompletenessWorkDao workDao = new CompletenessWorkDao(workConnection);
            MandatoryCompletenessService service = new MandatoryCompletenessService(workConnection);
            CompletenessSnapshotDao snapshotDao = new CompletenessSnapshotDao(workConnection);
            MandatoryCompletenessP360Writer writer =
                    new MandatoryCompletenessP360Writer(
                            workConnection,
                            snapshotDao,
                            config.restBatchSize);
            if (!config.dryRun) writer.preflight();
            else System.out.println("Preflight: List API field check deferred; dry-run only writes snapshot");

            long lastRevisionId = config.startAfterRevisionId;
            long processed = 0;
            int batchNo = 0;

            while (config.maxProducts <= 0 || processed < config.maxProducts) {
                int wanted = config.batchSize;
                if (config.maxProducts > 0) {
                    wanted = (int) Math.min(wanted, config.maxProducts - processed);
                }

                ProductPage page = readNextPage(sourceConnection, lastRevisionId, wanted);
                if (page.productIds.isEmpty()) break;

                batchNo++;
                String workRunId = UUID.randomUUID().toString();
                long init = System.currentTimeMillis();

                try {
                    workDao.replaceRun(workRunId, page.productIds);
                    workConnection.commit();

                    List<CompletenessResult> results = service.calculateWorkBatch(workRunId);
                    java.util.Set<String> returned = new java.util.HashSet<>();
                    for (CompletenessResult result : results) {
                        if (!returned.add(result.getProductIdentifier())) {
                            throw new IllegalStateException("Duplicate calculation result");
                        }
                    }
                    if (!returned.equals(new java.util.HashSet<>(page.productIds))) {
                        throw new IllegalStateException("Calculation did not return the requested products");
                    }

                    snapshotDao.upsertMandatory(bootstrapRunId, results);
                    workDao.deleteRun(workRunId);
                    workConnection.commit();

                    // A partir de aquí no existe una transacción Oracle abierta.
                    if (config.dryRun) {
                        writer.markDryRun(results);
                    } else {
                        boolean writeOk = writer.write(results, config.stopOnRestError);
                        if (!writeOk && config.stopOnRestError) {
                            throw new IllegalStateException(
                                    "List API reported errors in batch " + batchNo);
                        }
                    }

                    processed += page.productIds.size();
                    lastRevisionId = page.lastRevisionId;

                    long elapsed = System.currentTimeMillis() - init;
                    System.out.println("batch=" + batchNo
                            + " products=" + page.productIds.size()
                            + " processed=" + processed
                            + " lastRevisionId=" + lastRevisionId
                            + " elapsedMs=" + elapsed);

                } catch (Exception e) {
                    try { workConnection.rollback(); } catch (SQLException ignored) {}
                    try {
                        workDao.deleteRun(workRunId);
                        workConnection.commit();
                    } catch (SQLException cleanup) {
                        e.addSuppressed(cleanup);
                    }
                    throw e;
                }
            }

            System.out.println("Mandatory Completeness bootstrap finished"
                    + " runId=" + bootstrapRunId
                    + " processed=" + processed
                    + " lastRevisionId=" + lastRevisionId);
        }
    }

    private static ProductPage readNextPage(
            Connection connection,
            long afterRevisionId,
            int batchSize) throws SQLException {

        String sql = """
            select ar.ID, ar."Identifier"
              from PIM_MASTER."ArticleRevision" ar
             where ar.ID > ?
               and ar."EntityID" = 1100
               and ar."RevisionID" = 1
               and ar."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
             order by ar.ID
             fetch first ? rows only
            """;

        List<String> ids = new ArrayList<>(batchSize);
        long last = afterRevisionId;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, afterRevisionId);
            ps.setInt(2, batchSize);
            ps.setFetchSize(Math.min(batchSize, 5000));
            ps.setQueryTimeout(60);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    last = rs.getLong(1);
                    String identifier = rs.getString(2);
                    if (identifier == null || identifier.isBlank()) {
                        throw new SQLException("Blank product identifier at revision " + last);
                    }
                    ids.add(identifier);
                }
            }
        }
        return new ProductPage(ids, last);
    }

    private static final class ProductPage {
        final List<String> productIds;
        final long lastRevisionId;

        ProductPage(List<String> productIds, long lastRevisionId) {
            this.productIds = productIds;
            this.lastRevisionId = lastRevisionId;
        }
    }

    private static final class Config {
        final int batchSize;
        final int restBatchSize;
        final long maxProducts;
        final long startAfterRevisionId;
        final boolean dryRun;
        final boolean stopOnRestError;

        Config(
                int batchSize,
                int restBatchSize,
                long maxProducts,
                long startAfterRevisionId,
                boolean dryRun,
                boolean stopOnRestError) {
            this.batchSize = batchSize;
            this.restBatchSize = restBatchSize;
            this.maxProducts = maxProducts;
            this.startAfterRevisionId = startAfterRevisionId;
            this.dryRun = dryRun;
            this.stopOnRestError = stopOnRestError;
        }

        static Config parse(String[] args) {
            int batchSize = DEFAULT_BATCH_SIZE;
            int restBatchSize = DEFAULT_REST_BATCH_SIZE;
            long maxProducts = 0;
            long startAfterRevisionId = 0;
            boolean dryRun = true;
            boolean stopOnRestError = true;

            if (args != null) {
                for (String arg : args) {
                    if (arg == null) continue;
                    if (arg.startsWith("--batch-size=")) {
                        batchSize = Integer.parseInt(arg.substring("--batch-size=".length()));
                    } else if (arg.startsWith("--rest-batch-size=")) {
                        restBatchSize = Integer.parseInt(arg.substring("--rest-batch-size=".length()));
                    } else if (arg.startsWith("--max-products=")) {
                        maxProducts = Long.parseLong(arg.substring("--max-products=".length()));
                    } else if (arg.startsWith("--start-after-revision-id=")) {
                        startAfterRevisionId = Long.parseLong(
                                arg.substring("--start-after-revision-id=".length()));
                    } else if (arg.startsWith("--dry-run=")) {
                        dryRun = parseBoolean(arg.substring("--dry-run=".length()));
                    } else if (arg.startsWith("--stop-on-rest-error=")) {
                        stopOnRestError = parseBoolean(
                                arg.substring("--stop-on-rest-error=".length()));
                    } else {
                        throw new IllegalArgumentException("Unknown option: " + arg);
                    }
                }
            }

            if (batchSize < 1) throw new IllegalArgumentException("batch-size must be > 0");
            if (restBatchSize < 1) throw new IllegalArgumentException("rest-batch-size must be > 0");
            if (maxProducts < 0 || startAfterRevisionId < 0) {
                throw new IllegalArgumentException("Product limit and starting revision must be >= 0");
            }

            return new Config(
                    batchSize,
                    restBatchSize,
                    maxProducts,
                    startAfterRevisionId,
                    dryRun,
                    stopOnRestError);
        }

        private static boolean parseBoolean(String value) {
            if ("true".equalsIgnoreCase(value)) return true;
            if ("false".equalsIgnoreCase(value)) return false;
            throw new IllegalArgumentException("Expected true or false, received: " + value);
        }
    }
}
