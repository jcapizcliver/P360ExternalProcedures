package mx.com.liverpool.p360.services.core.amqp.run;

import java.sql.*;
import java.util.*;
import mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager;
import mx.com.liverpool.p360.services.core.completeness.*;
import mx.com.liverpool.p360.services.core.completeness.MandatoryCompletenessPendingDao.Pending;

/** Independent, bounded incremental worker. No JMS queue and no PIM JDBC writes. */
public final class MandatoryCompletenessChangeProcessor implements Runnable, AutoCloseable {
    private volatile boolean running = true;
    private Thread thread;
    public void start() {
        if (!MandatoryCompletenessIntake.enabled()) return;
        // Fail startup before starting consumers when the support schema is absent.
        try (Connection c = new QuickJdbcConnectionManager().openConnection(false)) {
            new MandatoryCompletenessPendingDao(c).preflight();
        } catch (SQLException e) { throw new IllegalStateException("Mandatory incremental preflight failed", e); }
        thread = new Thread(this, "MandatoryCompletenessChangeProcessor");
        thread.setDaemon(true);
        thread.start();
    }
    public void setRunning(boolean value) { running = value; }
    public void close() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            try { thread.join(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
    private boolean pause(long ms) {
        try { Thread.sleep(ms); return running; }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
    }
    private static boolean bootstrapRunning() {
        // Existing historical process predates the DB worker mutex. Do not race its snapshots/API writes.
        return ProcessHandle.allProcesses().anyMatch(p -> p.info().arguments()
                .map(a -> Arrays.stream(a).anyMatch(s -> s.equals(
                        "mx.com.liverpool.p360.services.core.completeness.MandatoryCompletenessBootstrap")))
                .orElse(false));
    }
    public void run() {
        long lastStatus = 0;
        while (running) {
            try {
                QuickJdbcConnectionManager cm = new QuickJdbcConnectionManager();
                try (Connection mutex = cm.openConnection(false); Connection c = cm.openConnection(false)) {
                    new MandatoryCompletenessPendingDao(mutex).lockWorker();
                    MandatoryCompletenessPendingDao pending = new MandatoryCompletenessPendingDao(c);
                    CompletenessWorkDao work = new CompletenessWorkDao(c);
                    MandatoryCompletenessService service = new MandatoryCompletenessService(c);
                    CompletenessSnapshotDao snapshot = new CompletenessSnapshotDao(c);
                    MandatoryCompletenessP360Writer writer = new MandatoryCompletenessP360Writer(c, snapshot, 100);
                    writer.preflight();
                    System.out.println("Mandatory incremental started; table=" + MandatoryCompletenessPendingDao.TABLE
                            + "; delete-on-success; cap=100000; retries<=900s; independent worker");
                    while (running) {
                        if (bootstrapRunning()) {
                            if (System.currentTimeMillis() - lastStatus > 60000) {
                                System.out.println("Mandatory incremental: capturing changes; waiting for local bootstrap before writes");
                                lastStatus = System.currentTimeMillis();
                            }
                            if (!pause(5000)) return;
                            continue;
                        }
                        // Losing this connection releases the singleton lock: stop before any more writes.
                        if (!mutex.isValid(5)) throw new SQLException("Mandatory worker mutex connection lost");
                        List<Pending> rows = pending.due(50);
                        if (rows.isEmpty()) { if (!pause(2000)) return; continue; }
                        for (Pending row : rows) {
                            if (!running) return;
                            String run = UUID.randomUUID().toString();
                            try {
                                Page page = resolve(c, row);
                                work.replaceRun(run, page.ids());
                                // Work is private to this transaction until deleted; crash/rollback cannot leak rows.
                                Set<String> selected = new LinkedHashSet<>(page.ids());
                                if (!row.change().force() && !selected.isEmpty()) {
                                    Map<String, Set<String>> applicable = service.applicableCharacteristics(run);
                                    selected.removeIf(id -> Collections.disjoint(row.change().characteristics(),
                                            applicable.getOrDefault(id, Set.of())));
                                    work.replaceRun(run, selected);
                                }
                                List<CompletenessResult> results = service.calculateWorkBatch(run);
                                Set<String> returned = new HashSet<>();
                                for (CompletenessResult result : results) {
                                    if (!returned.add(result.getProductIdentifier())) throw new SQLException("Duplicate result");
                                }
                                if (!returned.equals(selected)) throw new SQLException("Incomplete calculation result");
                                snapshot.upsertMandatory(run, results);
                                work.deleteRun(run);
                                c.commit();
                                if (!mutex.isValid(5)) throw new SQLException("Worker mutex lost before API write");
                                if (!writer.write(results, true)) throw new SQLException("List API failed");
                                if (page.more()) pending.advance(row, page.cursor()); else pending.complete(row);
                                c.commit();
                                System.out.println("Mandatory incremental processed entity=" + row.change().entity()
                                        + " id=" + row.change().identifier() + " products=" + results.size()
                                        + " cursor=" + page.cursor() + " more=" + page.more());
                            } catch (Exception error) {
                                c.rollback();
                                pending.fail(row, error);
                                c.commit();
                                System.err.println("Mandatory incremental retry entity=" + row.change().entity()
                                        + " id=" + row.change().identifier() + " error=" + error.getClass().getSimpleName()
                                        + ": " + error.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception error) {
                System.err.println("Mandatory incremental unavailable; durable pending retained: "
                        + error.getClass().getSimpleName() + ": " + error.getMessage());
                if (!pause(10000)) return;
            }
        }
    }

    private record Page(Set<String> ids, long cursor, boolean more) {}
    private static Page resolve(Connection c, Pending row) throws SQLException {
        if (row.change().entity().equals("Product2G"))
            return new Page(Set.of(row.change().identifier()),0,false);
        Set<String> ids = new LinkedHashSet<>();
        if (row.change().entity().equals("Article")) {
            // Include old references as well as current ones, so a removed parent is recalculated too.
            String sql = """
                select distinct parent."Identifier"
                from PIM_MASTER."ArticleRevision" child
                join PIM_MASTER."ArticleReference" ref on ref."ArticleRevisionID"=child.ID
                join PIM_MASTER."ArticleRevision" parent on parent."Identifier"=ref."RefExtArtIdentifier"
                  and parent."EntityID"=1100 and parent."RevisionID"=1
                  and parent."DeletionTimestamp"=timestamp '9999-12-31 00:00:00.0'
                where child."Identifier"=? and child."EntityID"=1000
                """;
            try (var s = c.prepareStatement(sql)) {
                s.setNString(1,row.change().identifier()); s.setQueryTimeout(60);
                try (var r = s.executeQuery()) { while (r.next()) ids.add(r.getString(1)); }
            }
            return new Page(ids,0,false);
        }
        String sql = """
            select /*+ first_rows(100) index_asc(ar "XIF3_ArticleRevision") */ ar.ID,ar."Identifier"
            from PIM_MASTER."ArticleRevision" ar
            where ar.ID>? and ar."EntityID"=1100 and ar."RevisionID"=1
              and ar."DeletionTimestamp"=timestamp '9999-12-31 00:00:00.0'
            order by ar.ID fetch first 100 rows only
            """;
        long cursor = row.cursor();
        int count = 0;
        try (var s = c.prepareStatement(sql)) {
            s.setLong(1,cursor); s.setQueryTimeout(60);
            try (var r = s.executeQuery()) {
                while (r.next()) { cursor=r.getLong(1); ids.add(r.getString(2)); count++; }
            }
        }
        return new Page(ids,cursor,count==100);
    }
}
