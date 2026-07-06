package com.example.ei.forfun.logic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Supervisor operativo para refrescar el perfil Product2G sin dejar al operador ciego.
 *
 * Reglas:
 * - No usa una sola llamada silenciosa para todo el refresh.
 * - Los procedimientos largos se ejecutan en un hilo, mientras otra conexión consulta TT_PROFILE_RUN_LOG.
 * - No mata sesiones Oracle automáticamente. Si no hay avance, solo avisa.
 * - Permite reanudar por fase con --from=...
 *
 * Variables de entorno esperadas:
 * - ORACLE_JDBC_URL / ORACLE_JDBC_USER / ORACLE_JDBC_PASSWORD
 *   o P360_EXPLOIT_JDBC_URL / P360_EXPLOIT_JDBC_USER / P360_EXPLOIT_JDBC_PASSWORD
 */
public final class ProfileProduct2GRefreshSupervisor {

    private static final String SCHEMA = "P360_EXPLOIT";
    private static final DateTimeFormatter LOG_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private enum Phase {
        BASE,
        CONFIG,
        DERIVED,
        DEFINED_GLOBAL,
        DEFINED_LOCAL,
        FINAL,
        VIEWS,
        VALIDATE
    }

    private static final List<Phase> PHASE_ORDER = List.of(
            Phase.BASE,
            Phase.CONFIG,
            Phase.DERIVED,
            Phase.DEFINED_GLOBAL,
            Phase.DEFINED_LOCAL,
            Phase.FINAL,
            Phase.VIEWS,
            Phase.VALIDATE
    );

    private static final class Options {
        Phase from = Phase.BASE;
        Phase only = null;
        boolean views = true;
        boolean dryRun = false;
        long pollSeconds = 30;
        long stallWarnMinutes = 15;
        boolean help = false;
    }

    private static final class DbConfig {
        final String url;
        final String user;
        final String password;

        DbConfig(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }
    }

    private static final class MonitorState {
        String latestRunId;
        String lastFingerprint;
        Instant lastChange = Instant.now();
    }

    private final DbConfig db;
    private final Options options;

    public static void main(String[] args) throws Exception {
        Options options = parseOptions(args);
        if (options.help) {
            printHelp();
            return;
        }

        DbConfig db = loadDbConfig();
        new ProfileProduct2GRefreshSupervisor(db, options).run();
    }

    private ProfileProduct2GRefreshSupervisor(DbConfig db, Options options) {
        this.db = db;
        this.options = options;
    }

    private void run() throws Exception {
        log("START ProfileProduct2GRefreshSupervisor");
        log("from=" + options.from + ", only=" + options.only + ", views=" + options.views + ", pollSeconds=" + options.pollSeconds + ", stallWarnMinutes=" + options.stallWarnMinutes + ", dryRun=" + options.dryRun);

        List<Phase> phases = selectedPhases();
        log("phases=" + phases);

        try (Connection con = openConnection()) {
            con.setAutoCommit(true);
            preflight(con);
        }

        for (Phase phase : phases) {
            if (phase == Phase.VIEWS && !options.views) {
                log("SKIP VIEWS (--no-views)");
                continue;
            }
            runPhase(phase);
        }

        log("FINISHED ProfileProduct2GRefreshSupervisor");
    }

    private List<Phase> selectedPhases() {
        if (options.only != null) {
            if (options.only == Phase.VIEWS && !options.views) {
                return List.of();
            }
            return List.of(options.only);
        }

        List<Phase> result = new ArrayList<>();
        boolean add = false;
        for (Phase phase : PHASE_ORDER) {
            if (phase == options.from) {
                add = true;
            }
            if (add) {
                result.add(phase);
            }
        }
        return result;
    }

    private void runPhase(Phase phase) throws Exception {
        Instant start = Instant.now();
        log("\n========== START PHASE " + phase + " ==========");
        try (Connection con = openConnection()) {
            con.setAutoCommit(true);
            switch (phase) {
                case BASE:
                    callProcedureWithProgress("RUN_PROFILE_PRODUCT2G_BASE", 0, 99, -1, null);
                    validateOneRowPerProduct(con, "TT_PROFILE_PRODUCT2G_BASE", "ArticleRevisionID");
                    printQuery(con, "PRODUCT BASE STATUS", "select count(*) productos, sum(case when \"Plantilla\" is null then 1 else 0 end) sin_plantilla, sum(case when \"ArticleDomainRows\" is null then 1 else 0 end) sin_article_domain, sum(case when \"ArticleDomainRows\" > 1 then 1 else 0 end) con_mas_de_un_article_domain, max(\"ArticleDomainRows\") max_article_domain_rows from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE");
                    break;
                case CONFIG:
                    callProcedureWithProgress("RUN_PROFILE_ATTR_CONFIG", 100, 199, -1, null);
                    validateAttrConfig(con);
                    break;
                case DERIVED:
                    rebuildDerivedTables(con);
                    validateDerived(con);
                    break;
                case DEFINED_GLOBAL:
                    callProcedureWithProgress("RUN_PROFILE_DEFINED_SUMMARY_BY_GROUP", 300, 399, 310, "GLOBAL GROUPS");
                    validateDefinedGlobal(con);
                    break;
                case DEFINED_LOCAL:
                    ensureLocalProcedure(con);
                    callProcedureWithProgress("RUN_PROFILE_DEFINED_SUMMARY_LOCAL", 500, 599, 510, "LOCAL GROUPS");
                    validateDefinedLocal(con);
                    break;
                case FINAL:
                    rebuildFinalSummary(con);
                    validateFinalSummary(con);
                    break;
                case VIEWS:
                    createViews(con);
                    validateViews(con);
                    break;
                case VALIDATE:
                    validateAll(con);
                    break;
                default:
                    throw new IllegalStateException("Fase no soportada: " + phase);
            }
            log("========== FINISH PHASE " + phase + " duration=" + Duration.between(start, Instant.now()).toSeconds() + "s ==========");
        } catch (Exception e) {
            log("========== FAILED PHASE " + phase + " duration=" + Duration.between(start, Instant.now()).toSeconds() + "s ==========");
            throw e;
        }
    }

    private void preflight(Connection con) throws SQLException {
        log("Preflight objects");
        requireTable(con, "TT_PROFILE_RUN_LOG");
        if (shouldRun(Phase.BASE)) {
            requireProcedure(con, "RUN_PROFILE_PRODUCT2G_BASE");
        }
        if (shouldRun(Phase.CONFIG)) {
            requireProcedure(con, "RUN_PROFILE_ATTR_CONFIG");
        }
        if (shouldRun(Phase.DEFINED_GLOBAL)) {
            requireProcedure(con, "RUN_PROFILE_DEFINED_SUMMARY_BY_GROUP");
        }
    }

    private boolean shouldRun(Phase phase) {
        return selectedPhases().contains(phase);
    }

    private void callProcedureWithProgress(String procedureName, int startStepNo, int finishStepNo, int groupStepNo, String groupLabel) throws Exception {
        requireProcedure(openConnection(), procedureName);
        Timestamp since = Timestamp.from(Instant.now().minusSeconds(5));
        log("CALL " + procedureName + " monitorSince=" + since);

        if (options.dryRun) {
            log("DRY RUN: begin " + SCHEMA + "." + procedureName + "; end;");
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "procedure-" + procedureName);
            t.setDaemon(true);
            return t;
        });

        Future<Void> future = executor.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try (Connection callCon = openConnection(); Statement st = callCon.createStatement()) {
                    callCon.setAutoCommit(true);
                    st.execute("begin " + SCHEMA + "." + procedureName + "; end;");
                }
                return null;
            }
        });

        MonitorState state = new MonitorState();
        try (Connection monitorCon = openConnection()) {
            monitorCon.setAutoCommit(true);
            while (!future.isDone()) {
                monitorProcedure(monitorCon, procedureName, startStepNo, finishStepNo, groupStepNo, groupLabel, since, state);
                Thread.sleep(options.pollSeconds * 1000L);
            }
            monitorProcedure(monitorCon, procedureName, startStepNo, finishStepNo, groupStepNo, groupLabel, since, state);
        } finally {
            executor.shutdownNow();
        }

        try {
            future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw new RuntimeException(cause);
        }

        log("CALL FINISHED " + procedureName);
    }

    private void monitorProcedure(Connection con, String procedureName, int startStepNo, int finishStepNo, int groupStepNo, String groupLabel, Timestamp since, MonitorState state) throws SQLException {
        String runId = findLatestRunId(con, procedureName, startStepNo, finishStepNo, since);
        if (runId == null) {
            maybePrintProgress(state, "waiting-run-id", "No RUN_ID yet for " + procedureName);
            return;
        }
        state.latestRunId = runId;

        String latest = fetchLatestLogLine(con, runId);
        if (groupStepNo > 0) {
            String groupProgress = fetchGroupProgress(con, runId, groupStepNo, groupLabel);
            String failures = fetchFailureCount(con, runId);
            String fingerprint = latest + " | " + groupProgress + " | " + failures;
            maybePrintProgress(state, fingerprint, "run_id=" + runId + " | " + groupProgress + " | " + failures + " | latest=" + latest);
        } else {
            String failures = fetchFailureCount(con, runId);
            String fingerprint = latest + " | " + failures;
            maybePrintProgress(state, fingerprint, "run_id=" + runId + " | " + failures + " | latest=" + latest);
        }
    }

    private void maybePrintProgress(MonitorState state, String fingerprint, String message) {
        Instant now = Instant.now();
        if (!fingerprint.equals(state.lastFingerprint)) {
            state.lastFingerprint = fingerprint;
            state.lastChange = now;
            log("PROGRESS " + message);
            return;
        }

        Duration idle = Duration.between(state.lastChange, now);
        if (idle.toMinutes() >= options.stallWarnMinutes) {
            log("WARN no progress change for " + idle.toMinutes() + " minutes | " + message);
            state.lastChange = now;
        }
    }

    private String findLatestRunId(Connection con, String procedureName, int startStepNo, int finishStepNo, Timestamp since) throws SQLException {
        String sql = "select RUN_ID from (" +
                "select RUN_ID, max(START_TS) START_TS " +
                "from P360_EXPLOIT.TT_PROFILE_RUN_LOG " +
                "where STEP_NAME = ? and STEP_NO in (?, ?) and START_TS >= ? " +
                "group by RUN_ID order by START_TS desc" +
                ") where rownum = 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, procedureName);
            ps.setInt(2, startStepNo);
            ps.setInt(3, finishStepNo);
            ps.setTimestamp(4, since);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    private String fetchLatestLogLine(Connection con, String runId) throws SQLException {
        String sql = "select STEP_NO, STEP_NAME, STATUS, START_TS, END_TS, MESSAGE from (" +
                "select STEP_NO, STEP_NAME, STATUS, START_TS, END_TS, MESSAGE " +
                "from P360_EXPLOIT.TT_PROFILE_RUN_LOG where RUN_ID = ? " +
                "order by START_TS desc, STEP_NO desc, STATUS desc" +
                ") where rownum = 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return "step=" + rs.getInt(1) + ", name=" + rs.getString(2) + ", status=" + rs.getString(3) + ", start=" + rs.getTimestamp(4) + ", end=" + rs.getTimestamp(5) + ", msg=" + rs.getString(6);
                }
            }
        }
        return "no-log-lines";
    }

    private String fetchGroupProgress(Connection con, String runId, int groupStepNo, String groupLabel) throws SQLException {
        String totalSql = "select count(*) from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS";
        long total = queryLong(con, totalSql);

        String sql = "select count(*) grupos_terminados, max(to_number(regexp_substr(STEP_NAME, '[0-9]+'))) ultimo_grupo " +
                "from P360_EXPLOIT.TT_PROFILE_RUN_LOG where RUN_ID = ? and STEP_NO = ? and STATUS = 'FINISHED'";
        long done = 0L;
        Long lastGroup = null;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, runId);
            ps.setInt(2, groupStepNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    done = rs.getLong(1);
                    long value = rs.getLong(2);
                    if (!rs.wasNull()) {
                        lastGroup = value;
                    }
                }
            }
        }

        String activeSql = "select STEP_NAME, START_TS, MESSAGE from P360_EXPLOIT.TT_PROFILE_RUN_LOG s " +
                "where s.RUN_ID = ? and s.STEP_NO = ? and s.STATUS = 'STARTED' " +
                "and not exists (select 1 from P360_EXPLOIT.TT_PROFILE_RUN_LOG f where f.RUN_ID = s.RUN_ID and f.STEP_NO = s.STEP_NO and f.STEP_NAME = s.STEP_NAME and f.STATUS = 'FINISHED') " +
                "order by s.START_TS desc";
        String active = "none";
        try (PreparedStatement ps = con.prepareStatement(activeSql)) {
            ps.setString(1, runId);
            ps.setInt(2, groupStepNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    active = rs.getString(1) + " start=" + rs.getTimestamp(2) + " msg=" + rs.getString(3);
                }
            }
        }

        double pct = total == 0 ? 0.0 : (100.0 * done / total);
        return groupLabel + " done=" + done + "/" + total + " pct=" + String.format(Locale.US, "%.2f", pct) + "% lastGroup=" + lastGroup + " active=" + active;
    }

    private String fetchFailureCount(Connection con, String runId) throws SQLException {
        String sql = "select count(*) from P360_EXPLOIT.TT_PROFILE_RUN_LOG where RUN_ID = ? and STATUS = 'FAILED'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, runId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long failed = rs.getLong(1);
                    return "failed=" + failed;
                }
            }
        }
        return "failed=?";
    }

    private void rebuildDerivedTables(Connection con) throws SQLException {
        log("Rebuild derived tables");
        dropTables(con,
                "TT_PROFILE_PRODUCT_ATTR_SUMMARY",
                "TT_PROFILE_PRODUCT_DEFINED_SUMMARY_LOCAL",
                "TT_PROFILE_PRODUCT_DEFINED_SUMMARY",
                "TT_PROFILE_GROUP_REQ_SUMMARY_LOCAL",
                "TT_PROFILE_GROUP_ATTR_REQ_LOCAL",
                "TT_PROFILE_GROUP_REQ_SUMMARY",
                "TT_PROFILE_GROUP_ATTR_REQ",
                "TT_PROFILE_PRODUCT_GROUP_MAP",
                "TT_PROFILE_PRODUCT2G_GROUPS",
                "TT_PROFILE_PRODUCT2G_BASE_GRP",
                "TT_PROFILE_REQ_ESTIMATE"
        );

        execute(con, "CREATE TT_PROFILE_PRODUCT2G_BASE_GRP", """
                create table P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE_GRP nologging as
                select "Plantilla", "BusinessCode", "BusinessName", count(*) "ProductCount"
                from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE
                group by "Plantilla", "BusinessCode", "BusinessName"
                """);
        createIndex(con, "IX_TT_PROF_P2G_GRP_01", "create index P360_EXPLOIT.IX_TT_PROF_P2G_GRP_01 on P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE_GRP(\"Plantilla\")");
        createIndex(con, "IX_TT_PROF_P2G_GRP_02", "create index P360_EXPLOIT.IX_TT_PROF_P2G_GRP_02 on P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE_GRP(\"BusinessCode\")");

        execute(con, "CREATE TT_PROFILE_PRODUCT2G_GROUPS", """
                create table P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS nologging as
                select row_number() over (order by "Plantilla" nulls first, "BusinessCode" nulls first, "BusinessName" nulls first) "GroupID",
                       "Plantilla", "BusinessCode", "BusinessName", "ProductCount"
                from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE_GRP
                """);
        createIndex(con, "IX_TT_PROF_P2G_GROUPS_01", "create index P360_EXPLOIT.IX_TT_PROF_P2G_GROUPS_01 on P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS(\"GroupID\")");
        createIndex(con, "IX_TT_PROF_P2G_GROUPS_02", "create index P360_EXPLOIT.IX_TT_PROF_P2G_GROUPS_02 on P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS(\"Plantilla\",\"BusinessCode\")");

        execute(con, "CREATE TT_PROFILE_PRODUCT_GROUP_MAP", """
                create table P360_EXPLOIT.TT_PROFILE_PRODUCT_GROUP_MAP nologging as
                select pb."ArticleRevisionID", g."GroupID"
                from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE pb
                inner join P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS g
                on nvl(pb."Plantilla", chr(0)) = nvl(g."Plantilla", chr(0))
                and nvl(pb."BusinessCode", chr(0)) = nvl(g."BusinessCode", chr(0))
                and nvl(pb."BusinessName", chr(0)) = nvl(g."BusinessName", chr(0))
                """);
        createIndex(con, "IX_TT_PROF_PROD_GRP_01", "create index P360_EXPLOIT.IX_TT_PROF_PROD_GRP_01 on P360_EXPLOIT.TT_PROFILE_PRODUCT_GROUP_MAP(\"ArticleRevisionID\")");
        createIndex(con, "IX_TT_PROF_PROD_GRP_02", "create index P360_EXPLOIT.IX_TT_PROF_PROD_GRP_02 on P360_EXPLOIT.TT_PROFILE_PRODUCT_GROUP_MAP(\"GroupID\")");

        execute(con, "CREATE TT_PROFILE_GROUP_ATTR_REQ", """
                create table P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ nologging as
                with raw_req as (
                    select g."GroupID", cfg."CharacteristicID", cfg."CharacteristicIdentifier", cfg."IsMandatory", cfg."ConfigSource"
                    from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS g
                    inner join P360_EXPLOIT.TT_PROFILE_ATTR_CONFIG cfg
                    on cfg."ConfigSource" = 'TEMPLATE'
                    and cfg."Plantilla" = g."Plantilla"
                    where cfg."BusinessFilter" is null
                       or length(trim(cfg."BusinessFilter")) = 0
                       or (g."BusinessCode" is not null and instr(upper(cfg."BusinessFilter"), upper(g."BusinessCode")) > 0)
                       or (g."BusinessName" is not null and instr(upper(cfg."BusinessFilter"), upper(g."BusinessName")) > 0)
                    union all
                    select g."GroupID", cfg."CharacteristicID", cfg."CharacteristicIdentifier", cfg."IsMandatory", cfg."ConfigSource"
                    from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS g
                    inner join P360_EXPLOIT.TT_PROFILE_ATTR_CONFIG cfg
                    on cfg."ConfigSource" = 'GLOBAL'
                    where cfg."BusinessFilter" is null
                       or length(trim(cfg."BusinessFilter")) = 0
                       or (g."BusinessCode" is not null and instr(upper(cfg."BusinessFilter"), upper(g."BusinessCode")) > 0)
                       or (g."BusinessName" is not null and instr(upper(cfg."BusinessFilter"), upper(g."BusinessName")) > 0)
                )
                select "GroupID", "CharacteristicID", max("CharacteristicIdentifier") "CharacteristicIdentifier", max("IsMandatory") "IsMandatory",
                       case when count(distinct "ConfigSource") = 2 then 'BOTH' else max("ConfigSource") end "ConfigSourceResolved"
                from raw_req
                group by "GroupID", "CharacteristicID"
                """);
        createIndex(con, "IX_TT_PROF_GRP_REQ_01", "create index P360_EXPLOIT.IX_TT_PROF_GRP_REQ_01 on P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ(\"GroupID\",\"CharacteristicID\")");
        createIndex(con, "IX_TT_PROF_GRP_REQ_02", "create index P360_EXPLOIT.IX_TT_PROF_GRP_REQ_02 on P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ(\"CharacteristicID\")");

        execute(con, "CREATE TT_PROFILE_GROUP_ATTR_REQ_LOCAL", """
                create table P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ_LOCAL nologging as
                select g."GroupID", cfg."CharacteristicID", max(cfg."CharacteristicIdentifier") "CharacteristicIdentifier", max(cfg."IsMandatory") "IsMandatory"
                from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS g
                inner join P360_EXPLOIT.TT_PROFILE_ATTR_CONFIG cfg
                on cfg."ConfigSource" = 'TEMPLATE'
                and cfg."Plantilla" = g."Plantilla"
                where cfg."BusinessFilter" is null
                   or length(trim(cfg."BusinessFilter")) = 0
                   or (g."BusinessCode" is not null and instr(upper(cfg."BusinessFilter"), upper(g."BusinessCode")) > 0)
                   or (g."BusinessName" is not null and instr(upper(cfg."BusinessFilter"), upper(g."BusinessName")) > 0)
                group by g."GroupID", cfg."CharacteristicID"
                """);
        createIndex(con, "IX_TT_PROF_GRP_REQ_LOC_01", "create index P360_EXPLOIT.IX_TT_PROF_GRP_REQ_LOC_01 on P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ_LOCAL(\"GroupID\",\"CharacteristicID\")");
        createIndex(con, "IX_TT_PROF_GRP_REQ_LOC_02", "create index P360_EXPLOIT.IX_TT_PROF_GRP_REQ_LOC_02 on P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ_LOCAL(\"CharacteristicID\")");

        execute(con, "CREATE TT_PROFILE_GROUP_REQ_SUMMARY", """
                create table P360_EXPLOIT.TT_PROFILE_GROUP_REQ_SUMMARY nologging as
                select "GroupID", count(*) "RequiredAttributeCount", sum(case when "IsMandatory" = 1 then 1 else 0 end) "RequiredMandatoryAttributeCount"
                from P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ
                group by "GroupID"
                """);
        createIndex(con, "IX_TT_PROF_GRP_REQ_SUM_01", "create index P360_EXPLOIT.IX_TT_PROF_GRP_REQ_SUM_01 on P360_EXPLOIT.TT_PROFILE_GROUP_REQ_SUMMARY(\"GroupID\")");

        execute(con, "CREATE TT_PROFILE_GROUP_REQ_SUMMARY_LOCAL", """
                create table P360_EXPLOIT.TT_PROFILE_GROUP_REQ_SUMMARY_LOCAL nologging as
                select "GroupID", count(*) "LocalRequiredAttributeCount", sum(case when "IsMandatory" = 1 then 1 else 0 end) "LocalRequiredMandatoryAttributeCount"
                from P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ_LOCAL
                group by "GroupID"
                """);
        createIndex(con, "IX_TT_PROF_GRP_SUM_LOC_01", "create index P360_EXPLOIT.IX_TT_PROF_GRP_SUM_LOC_01 on P360_EXPLOIT.TT_PROFILE_GROUP_REQ_SUMMARY_LOCAL(\"GroupID\")");
    }

    private void rebuildFinalSummary(Connection con) throws SQLException {
        dropTableIfExists(con, "TT_PROFILE_PRODUCT_ATTR_SUMMARY");
        execute(con, "CREATE TT_PROFILE_PRODUCT_ATTR_SUMMARY", """
                create table P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY nologging as
                select pb."ArticleRevisionID", pb."Product2GIdentifier", pb."ProductSKU", pb."EAN", pb."Plantilla", pb."VariantCount",
                       pb."CurrentStatusID", pb."CurrentStatusName", pb."PrevStatusID", pb."PrevStatusCode", pb."PrevStatusName",
                       pb."ExternalStatusID", pb."ExternalStatusCode", pb."ExternalStatusName", pb."FirstDateApproved", pb."LastDateApproved",
                       pb."BusinessID", pb."BusinessCode", pb."BusinessName", pb."ArticleDomainRows",
                       pb."DireccionID", pb."DireccionCode", pb."DireccionName", pb."SectionID", pb."SectionCode", pb."SectionName",
                       pb."ItemGroupID", pb."ItemGroupCode", pb."ItemGroupName", pb."ItemGroupS4HID", pb."ItemGroupS4HCode", pb."ItemGroupS4HName",
                       pb."BrandNameID", pb."BrandNameCode", pb."BrandNameName", pb."BRAND_ID_S4HID", pb."BRAND_ID_S4HCode", pb."BRAND_ID_S4HName",
                       pb."NegocioID", pb."NegocioCode", pb."NegocioName", pb."SAPObjectTypeID", pb."SAPObjectTypeCode", pb."SAPObjectTypeName",
                       pb."SupplierID", pb."SupplierPartNumber", pgm."GroupID",
                       nvl(grs."RequiredAttributeCount",0) "RequiredAttributeCount",
                       nvl(grs."RequiredMandatoryAttributeCount",0) "RequiredMandatoryAttributeCount",
                       nvl(def."DefinedAttributeCount",0) "DefinedAttributeCount",
                       nvl(def."DefinedMandatoryAttributeCount",0) "DefinedMandatoryAttributeCount",
                       nvl(grs."RequiredAttributeCount",0) - nvl(def."DefinedAttributeCount",0) "MissingAttributeCount",
                       nvl(grs."RequiredMandatoryAttributeCount",0) - nvl(def."DefinedMandatoryAttributeCount",0) "MissingMandatoryAttributeCount",
                       round(100 * nvl(def."DefinedAttributeCount",0) / nullif(nvl(grs."RequiredAttributeCount",0),0), 2) "DefinedAttributePct",
                       round(100 * nvl(def."DefinedMandatoryAttributeCount",0) / nullif(nvl(grs."RequiredMandatoryAttributeCount",0),0), 2) "DefinedMandatoryAttributePct",
                       nvl(lrs."LocalRequiredAttributeCount",0) "LocalRequiredAttributeCount",
                       nvl(lrs."LocalRequiredMandatoryAttributeCount",0) "LocalRequiredMandatoryAttributeCount",
                       nvl(ldef."LocalDefinedAttributeCount",0) "LocalDefinedAttributeCount",
                       nvl(ldef."LocalDefinedMandatoryAttributeCount",0) "LocalDefinedMandatoryAttributeCount",
                       nvl(lrs."LocalRequiredAttributeCount",0) - nvl(ldef."LocalDefinedAttributeCount",0) "LocalMissingAttributeCount",
                       nvl(lrs."LocalRequiredMandatoryAttributeCount",0) - nvl(ldef."LocalDefinedMandatoryAttributeCount",0) "LocalMissingMandatoryAttributeCount",
                       round(100 * nvl(ldef."LocalDefinedAttributeCount",0) / nullif(nvl(lrs."LocalRequiredAttributeCount",0),0), 2) "LocalDefinedAttributePct",
                       round(100 * nvl(ldef."LocalDefinedMandatoryAttributeCount",0) / nullif(nvl(lrs."LocalRequiredMandatoryAttributeCount",0),0), 2) "LocalDefinedMandatoryAttributePct"
                from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_BASE pb
                inner join P360_EXPLOIT.TT_PROFILE_PRODUCT_GROUP_MAP pgm on pgm."ArticleRevisionID" = pb."ArticleRevisionID"
                left join P360_EXPLOIT.TT_PROFILE_GROUP_REQ_SUMMARY grs on grs."GroupID" = pgm."GroupID"
                left join P360_EXPLOIT.TT_PROFILE_PRODUCT_DEFINED_SUMMARY def on def."ArticleRevisionID" = pb."ArticleRevisionID"
                left join P360_EXPLOIT.TT_PROFILE_GROUP_REQ_SUMMARY_LOCAL lrs on lrs."GroupID" = pgm."GroupID"
                left join P360_EXPLOIT.TT_PROFILE_PRODUCT_DEFINED_SUMMARY_LOCAL ldef on ldef."ArticleRevisionID" = pb."ArticleRevisionID"
                """);
        createIndex(con, "IX_TT_PROF_ATTR_SUM_01", "create index P360_EXPLOIT.IX_TT_PROF_ATTR_SUM_01 on P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY(\"ArticleRevisionID\")");
        createIndex(con, "IX_TT_PROF_ATTR_SUM_02", "create index P360_EXPLOIT.IX_TT_PROF_ATTR_SUM_02 on P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY(\"Product2GIdentifier\")");
        createIndex(con, "IX_TT_PROF_ATTR_SUM_03", "create index P360_EXPLOIT.IX_TT_PROF_ATTR_SUM_03 on P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY(\"CurrentStatusID\")");
        createIndex(con, "IX_TT_PROF_ATTR_SUM_04", "create index P360_EXPLOIT.IX_TT_PROF_ATTR_SUM_04 on P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY(\"MissingMandatoryAttributeCount\")");
        createIndex(con, "IX_TT_PROF_ATTR_SUM_05", "create index P360_EXPLOIT.IX_TT_PROF_ATTR_SUM_05 on P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY(\"LocalMissingMandatoryAttributeCount\")");
    }

    private void createViews(Connection con) throws SQLException {
        execute(con, "CREATE VW_PROFILE_PRODUCT_ATTR_SUMMARY", "create or replace view P360_EXPLOIT.VW_PROFILE_PRODUCT_ATTR_SUMMARY as select * from P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY");

        execute(con, "CREATE VW_PROFILE_ATTR_BY_STATUS", """
                create or replace view P360_EXPLOIT.VW_PROFILE_ATTR_BY_STATUS as
                select "CurrentStatusID" CURRENT_STATUS_ID,
                       "CurrentStatusName" CURRENT_STATUS_NAME,
                       count(*) PRODUCT_COUNT,
                       round(avg("DefinedAttributePct"),2) AVG_DEFINED_ATTRIBUTE_PCT,
                       round(avg("DefinedMandatoryAttributePct"),2) AVG_DEFINED_MANDATORY_ATTRIBUTE_PCT,
                       round(avg("LocalDefinedAttributePct"),2) AVG_LOCAL_DEFINED_ATTRIBUTE_PCT,
                       round(avg("LocalDefinedMandatoryAttributePct"),2) AVG_LOCAL_DEFINED_MANDATORY_ATTRIBUTE_PCT,
                       sum("RequiredAttributeCount") REQUIRED_ATTRIBUTE_COUNT,
                       sum("DefinedAttributeCount") DEFINED_ATTRIBUTE_COUNT,
                       sum("MissingAttributeCount") MISSING_ATTRIBUTE_COUNT,
                       sum("RequiredMandatoryAttributeCount") REQUIRED_MANDATORY_ATTRIBUTE_COUNT,
                       sum("DefinedMandatoryAttributeCount") DEFINED_MANDATORY_ATTRIBUTE_COUNT,
                       sum("MissingMandatoryAttributeCount") MISSING_MANDATORY_ATTRIBUTE_COUNT,
                       sum("LocalRequiredAttributeCount") LOCAL_REQUIRED_ATTRIBUTE_COUNT,
                       sum("LocalDefinedAttributeCount") LOCAL_DEFINED_ATTRIBUTE_COUNT,
                       sum("LocalMissingAttributeCount") LOCAL_MISSING_ATTRIBUTE_COUNT,
                       sum("LocalRequiredMandatoryAttributeCount") LOCAL_REQUIRED_MANDATORY_ATTRIBUTE_COUNT,
                       sum("LocalDefinedMandatoryAttributeCount") LOCAL_DEFINED_MANDATORY_ATTRIBUTE_COUNT,
                       sum("LocalMissingMandatoryAttributeCount") LOCAL_MISSING_MANDATORY_ATTRIBUTE_COUNT
                from P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY
                group by "CurrentStatusID", "CurrentStatusName"
                """);

        execute(con, "CREATE VW_PROFILE_APPROVED_TOP_MISSING", """
                create or replace view P360_EXPLOIT.VW_PROFILE_APPROVED_TOP_MISSING as
                select MISSING_RANK, PRODUCT2G_IDENTIFIER, PRODUCT_SKU, EAN, PLANTILLA, BUSINESS_CODE, BUSINESS_NAME,
                       CURRENT_STATUS_ID, CURRENT_STATUS_NAME,
                       REQUIRED_ATTRIBUTE_COUNT, DEFINED_ATTRIBUTE_COUNT, MISSING_ATTRIBUTE_COUNT,
                       REQUIRED_MANDATORY_ATTRIBUTE_COUNT, DEFINED_MANDATORY_ATTRIBUTE_COUNT, MISSING_MANDATORY_ATTRIBUTE_COUNT,
                       DEFINED_ATTRIBUTE_PCT, DEFINED_MANDATORY_ATTRIBUTE_PCT,
                       LOCAL_REQUIRED_ATTRIBUTE_COUNT, LOCAL_DEFINED_ATTRIBUTE_COUNT, LOCAL_MISSING_ATTRIBUTE_COUNT,
                       LOCAL_REQUIRED_MANDATORY_ATTRIBUTE_COUNT, LOCAL_DEFINED_MANDATORY_ATTRIBUTE_COUNT, LOCAL_MISSING_MANDATORY_ATTRIBUTE_COUNT,
                       LOCAL_DEFINED_ATTRIBUTE_PCT, LOCAL_DEFINED_MANDATORY_ATTRIBUTE_PCT
                from (
                    select row_number() over (order by "MissingMandatoryAttributeCount" desc, "MissingAttributeCount" desc, "Product2GIdentifier") MISSING_RANK,
                           "Product2GIdentifier" PRODUCT2G_IDENTIFIER,
                           "ProductSKU" PRODUCT_SKU,
                           "EAN" EAN,
                           "Plantilla" PLANTILLA,
                           "BusinessCode" BUSINESS_CODE,
                           "BusinessName" BUSINESS_NAME,
                           "CurrentStatusID" CURRENT_STATUS_ID,
                           "CurrentStatusName" CURRENT_STATUS_NAME,
                           "RequiredAttributeCount" REQUIRED_ATTRIBUTE_COUNT,
                           "DefinedAttributeCount" DEFINED_ATTRIBUTE_COUNT,
                           "MissingAttributeCount" MISSING_ATTRIBUTE_COUNT,
                           "RequiredMandatoryAttributeCount" REQUIRED_MANDATORY_ATTRIBUTE_COUNT,
                           "DefinedMandatoryAttributeCount" DEFINED_MANDATORY_ATTRIBUTE_COUNT,
                           "MissingMandatoryAttributeCount" MISSING_MANDATORY_ATTRIBUTE_COUNT,
                           "DefinedAttributePct" DEFINED_ATTRIBUTE_PCT,
                           "DefinedMandatoryAttributePct" DEFINED_MANDATORY_ATTRIBUTE_PCT,
                           "LocalRequiredAttributeCount" LOCAL_REQUIRED_ATTRIBUTE_COUNT,
                           "LocalDefinedAttributeCount" LOCAL_DEFINED_ATTRIBUTE_COUNT,
                           "LocalMissingAttributeCount" LOCAL_MISSING_ATTRIBUTE_COUNT,
                           "LocalRequiredMandatoryAttributeCount" LOCAL_REQUIRED_MANDATORY_ATTRIBUTE_COUNT,
                           "LocalDefinedMandatoryAttributeCount" LOCAL_DEFINED_MANDATORY_ATTRIBUTE_COUNT,
                           "LocalMissingMandatoryAttributeCount" LOCAL_MISSING_MANDATORY_ATTRIBUTE_COUNT,
                           "LocalDefinedAttributePct" LOCAL_DEFINED_ATTRIBUTE_PCT,
                           "LocalDefinedMandatoryAttributePct" LOCAL_DEFINED_MANDATORY_ATTRIBUTE_PCT
                    from P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY
                    where "CurrentStatusID" = 1007
                )
                where MISSING_RANK <= 100
                """);
    }

    private void ensureLocalProcedure(Connection con) throws SQLException {
        String status = objectStatus(con, "PROCEDURE", "RUN_PROFILE_DEFINED_SUMMARY_LOCAL");
        if ("VALID".equals(status)) {
            log("Procedure RUN_PROFILE_DEFINED_SUMMARY_LOCAL already VALID");
            return;
        }

        log("Creating RUN_PROFILE_DEFINED_SUMMARY_LOCAL because status=" + status);
        execute(con, "CREATE RUN_PROFILE_DEFINED_SUMMARY_LOCAL", """
                create or replace procedure P360_EXPLOIT.RUN_PROFILE_DEFINED_SUMMARY_LOCAL(p_run_id in varchar2 default null)
                authid current_user
                as
                    v_run_id varchar2(64 char) := nvl(p_run_id, to_char(systimestamp, 'YYYYMMDDHH24MISSFF3'));
                    v_start timestamp;
                    v_rows number;

                    procedure log_step(p_step_no number, p_step_name varchar2, p_status varchar2, p_start_ts timestamp, p_message varchar2 default null)
                    is
                        pragma autonomous_transaction;
                    begin
                        insert into P360_EXPLOIT.TT_PROFILE_RUN_LOG(RUN_ID, STEP_NO, STEP_NAME, STATUS, START_TS, END_TS, MESSAGE)
                        values(v_run_id, p_step_no, p_step_name, p_status, p_start_ts, systimestamp, substr(p_message, 1, 4000));
                        commit;
                    end;

                    procedure drop_table_if_exists(p_table_name varchar2)
                    is
                    begin
                        execute immediate 'drop table P360_EXPLOIT.' || p_table_name || ' purge';
                    exception
                        when others then
                            if sqlcode != -942 then
                                raise;
                            end if;
                    end;

                begin
                    log_step(500, 'RUN_PROFILE_DEFINED_SUMMARY_LOCAL', 'STARTED', systimestamp, 'run_id=' || v_run_id);

                    drop_table_if_exists('TT_PROFILE_PRODUCT_DEFINED_SUMMARY_LOCAL');

                    execute immediate q'[
                create table P360_EXPLOIT.TT_PROFILE_PRODUCT_DEFINED_SUMMARY_LOCAL nologging as
                select cast(null as number) "GroupID", cast(null as number) "ArticleRevisionID", cast(null as number) "LocalDefinedAttributeCount", cast(null as number) "LocalDefinedMandatoryAttributeCount"
                from dual
                where 1 = 0
                ]';

                    for r in (
                        select "GroupID", "ProductCount"
                        from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS
                        order by "GroupID"
                    ) loop
                        v_start := systimestamp;
                        log_step(510, 'LOCAL GROUP ' || r."GroupID", 'STARTED', v_start, 'products=' || r."ProductCount");

                        begin
                            execute immediate q'[
                insert into P360_EXPLOIT.TT_PROFILE_PRODUCT_DEFINED_SUMMARY_LOCAL(
                      "GroupID"
                    , "ArticleRevisionID"
                    , "LocalDefinedAttributeCount"
                    , "LocalDefinedMandatoryAttributeCount"
                )
                select /*+ leading(pgm acv req) use_nl(acv) index_rs_asc(acv IX_ACV_TUNE_02) use_nl(req) index(req IX_TT_PROF_GRP_REQ_LOC_01) */
                      pgm."GroupID"
                    , pgm."ArticleRevisionID"
                    , count(distinct acv."CharacteristicID") "LocalDefinedAttributeCount"
                    , count(distinct case when req."IsMandatory" = 1 then acv."CharacteristicID" end) "LocalDefinedMandatoryAttributeCount"
                from P360_EXPLOIT.TT_PROFILE_PRODUCT_GROUP_MAP pgm
                inner join PIM_MASTER."ArticleCharactValue" acv
                on acv."ArticleRevisionID" = pgm."ArticleRevisionID"
                and acv."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
                inner join P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ_LOCAL req
                on req."GroupID" = pgm."GroupID"
                and req."CharacteristicID" = acv."CharacteristicID"
                where pgm."GroupID" = :1
                and (acv."LookupValueID" is not null or length(trim(acv."Value")) > 0)
                group by pgm."GroupID", pgm."ArticleRevisionID"
                ]' using r."GroupID";

                            v_rows := sql%rowcount;
                            commit;
                            log_step(510, 'LOCAL GROUP ' || r."GroupID", 'FINISHED', v_start, 'products=' || r."ProductCount" || ', inserted_rows=' || v_rows);
                        exception
                            when others then
                                rollback;
                                log_step(510, 'LOCAL GROUP ' || r."GroupID", 'FAILED', v_start, 'products=' || r."ProductCount" || ', error=' || sqlerrm);
                                raise;
                        end;
                    end loop;

                    v_start := systimestamp;
                    log_step(590, 'INDEX LOCAL PRODUCT DEFINED SUMMARY 01', 'STARTED', v_start);
                    execute immediate 'create index P360_EXPLOIT.IX_TT_PROF_DEF_LOC_01 on P360_EXPLOIT.TT_PROFILE_PRODUCT_DEFINED_SUMMARY_LOCAL("ArticleRevisionID")';
                    log_step(590, 'INDEX LOCAL PRODUCT DEFINED SUMMARY 01', 'FINISHED', v_start);

                    v_start := systimestamp;
                    log_step(591, 'INDEX LOCAL PRODUCT DEFINED SUMMARY 02', 'STARTED', v_start);
                    execute immediate 'create index P360_EXPLOIT.IX_TT_PROF_DEF_LOC_02 on P360_EXPLOIT.TT_PROFILE_PRODUCT_DEFINED_SUMMARY_LOCAL("GroupID")';
                    log_step(591, 'INDEX LOCAL PRODUCT DEFINED SUMMARY 02', 'FINISHED', v_start);

                    log_step(599, 'RUN_PROFILE_DEFINED_SUMMARY_LOCAL', 'FINISHED', systimestamp, 'run_id=' || v_run_id);
                end RUN_PROFILE_DEFINED_SUMMARY_LOCAL
                """);

        String newStatus = objectStatus(con, "PROCEDURE", "RUN_PROFILE_DEFINED_SUMMARY_LOCAL");
        if (!"VALID".equals(newStatus)) {
            throw new SQLException("RUN_PROFILE_DEFINED_SUMMARY_LOCAL was created but status is " + newStatus);
        }
    }

    private void validateAll(Connection con) throws SQLException {
        validateOneRowPerProduct(con, "TT_PROFILE_PRODUCT2G_BASE", "ArticleRevisionID");
        validateAttrConfig(con);
        validateDerived(con);
        validateDefinedGlobal(con);
        validateDefinedLocal(con);
        validateFinalSummary(con);
        if (options.views) {
            validateViews(con);
        }
    }

    private void validateAttrConfig(Connection con) throws SQLException {
        printQuery(con, "ATTR CONFIG BY SOURCE", "select \"ConfigSource\", count(*) total_config, count(distinct \"CharacteristicID\") distinct_characteristics, sum(case when \"IsMandatory\" = 1 then 1 else 0 end) mandatory_config, sum(case when \"BusinessFilter\" is not null then 1 else 0 end) with_business_filter from P360_EXPLOIT.TT_PROFILE_ATTR_CONFIG group by \"ConfigSource\" order by \"ConfigSource\"");
    }

    private void validateDerived(Connection con) throws SQLException {
        printQuery(con, "GROUPS", "select count(*) grupos, sum(\"ProductCount\") productos from P360_EXPLOIT.TT_PROFILE_PRODUCT2G_GROUPS");
        validateOneRowPerProduct(con, "TT_PROFILE_PRODUCT_GROUP_MAP", "ArticleRevisionID");
        printQuery(con, "GROUP ATTR REQ", "select count(*) group_attribute_rows, count(distinct \"GroupID\") grupos, count(distinct \"CharacteristicID\") atributos_distintos, sum(case when \"IsMandatory\" = 1 then 1 else 0 end) mandatory_group_attribute_rows from P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ");
        printQuery(con, "GROUP ATTR REQ LOCAL", "select count(*) local_group_attribute_rows, count(distinct \"GroupID\") grupos, count(distinct \"CharacteristicID\") atributos_distintos, sum(case when \"IsMandatory\" = 1 then 1 else 0 end) local_mandatory_group_attribute_rows from P360_EXPLOIT.TT_PROFILE_GROUP_ATTR_REQ_LOCAL");
    }

    private void validateDefinedGlobal(Connection con) throws SQLException {
        printQuery(con, "DEFINED GLOBAL", "select count(*) filas, count(distinct \"ArticleRevisionID\") productos_con_definidos, sum(\"DefinedAttributeCount\") total_defined_attrs, sum(\"DefinedMandatoryAttributeCount\") total_defined_mandatory_attrs from P360_EXPLOIT.TT_PROFILE_PRODUCT_DEFINED_SUMMARY");
    }

    private void validateDefinedLocal(Connection con) throws SQLException {
        printQuery(con, "DEFINED LOCAL", "select count(*) filas, count(distinct \"ArticleRevisionID\") productos_con_definidos_local, sum(\"LocalDefinedAttributeCount\") total_local_defined_attrs, sum(\"LocalDefinedMandatoryAttributeCount\") total_local_defined_mandatory_attrs from P360_EXPLOIT.TT_PROFILE_PRODUCT_DEFINED_SUMMARY_LOCAL");
    }

    private void validateFinalSummary(Connection con) throws SQLException {
        validateOneRowPerProduct(con, "TT_PROFILE_PRODUCT_ATTR_SUMMARY", "ArticleRevisionID");
        printQuery(con, "FINAL RANGE", "select min(\"RequiredAttributeCount\") min_required, max(\"RequiredAttributeCount\") max_required, min(\"DefinedAttributeCount\") min_defined, max(\"DefinedAttributeCount\") max_defined, min(\"MissingAttributeCount\") min_missing, max(\"MissingAttributeCount\") max_missing, min(\"LocalRequiredAttributeCount\") min_local_required, max(\"LocalRequiredAttributeCount\") max_local_required, min(\"LocalDefinedAttributeCount\") min_local_defined, max(\"LocalDefinedAttributeCount\") max_local_defined, min(\"LocalMissingAttributeCount\") min_local_missing, max(\"LocalMissingAttributeCount\") max_local_missing from P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY");
        printQuery(con, "FINAL NEGATIVES", "select count(*) productos_con_negativos from P360_EXPLOIT.TT_PROFILE_PRODUCT_ATTR_SUMMARY where \"MissingAttributeCount\" < 0 or \"MissingMandatoryAttributeCount\" < 0 or \"LocalMissingAttributeCount\" < 0 or \"LocalMissingMandatoryAttributeCount\" < 0 or \"DefinedAttributePct\" > 100 or \"DefinedMandatoryAttributePct\" > 100 or \"LocalDefinedAttributePct\" > 100 or \"LocalDefinedMandatoryAttributePct\" > 100");
    }

    private void validateViews(Connection con) throws SQLException {
        printQuery(con, "VIEW PRODUCT SUMMARY", "select count(*) rows_count from P360_EXPLOIT.VW_PROFILE_PRODUCT_ATTR_SUMMARY");
        printQuery(con, "VIEW BY STATUS", "select count(*) rows_count from P360_EXPLOIT.VW_PROFILE_ATTR_BY_STATUS");
        printQuery(con, "VIEW TOP MISSING", "select count(*) rows_count from P360_EXPLOIT.VW_PROFILE_APPROVED_TOP_MISSING");
    }

    private void validateOneRowPerProduct(Connection con, String tableName, String columnName) throws SQLException {
        printQuery(con, tableName + " ONE ROW", "select count(*) filas, count(distinct \"" + columnName + "\") productos, count(*) - count(distinct \"" + columnName + "\") diferencia from P360_EXPLOIT." + tableName);
    }

    private void dropTables(Connection con, String... tableNames) throws SQLException {
        for (String tableName : tableNames) {
            dropTableIfExists(con, tableName);
        }
    }

    private void dropTableIfExists(Connection con, String tableName) throws SQLException {
        log("DROP TABLE " + SCHEMA + "." + tableName);
        if (options.dryRun) {
            return;
        }
        try (Statement st = con.createStatement()) {
            st.execute("drop table " + SCHEMA + "." + tableName + " purge");
        } catch (SQLException e) {
            if (e.getErrorCode() == 942) {
                log("  no existe, ok");
            } else {
                throw e;
            }
        }
    }

    private void createIndex(Connection con, String indexName, String sql) throws SQLException {
        execute(con, "CREATE INDEX " + indexName, sql);
    }

    private void execute(Connection con, String label, String sql) throws SQLException {
        Instant start = Instant.now();
        log("START " + label);
        if (options.dryRun) {
            log("DRY RUN SQL: " + collapse(sql));
            return;
        }
        try (Statement st = con.createStatement()) {
            st.execute(sql);
            log("OK " + label + " duration=" + Duration.between(start, Instant.now()).toMillis() + "ms");
        } catch (SQLTimeoutException e) {
            log("TIMEOUT " + label + " duration=" + Duration.between(start, Instant.now()).toSeconds() + "s");
            throw e;
        } catch (SQLException e) {
            log("ERROR " + label + " duration=" + Duration.between(start, Instant.now()).toSeconds() + "s error=" + e.getMessage());
            throw e;
        }
    }

    private void printQuery(Connection con, String label, String sql) throws SQLException {
        Instant start = Instant.now();
        log("QUERY " + label);
        if (options.dryRun) {
            log("DRY RUN QUERY: " + collapse(sql));
            return;
        }
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            int colCount = rs.getMetaData().getColumnCount();
            StringBuilder headers = new StringBuilder();
            for (int i = 1; i <= colCount; i++) {
                if (i > 1) headers.append(';');
                headers.append(rs.getMetaData().getColumnLabel(i));
            }
            log(headers.toString());
            int rows = 0;
            while (rs.next()) {
                rows++;
                StringBuilder line = new StringBuilder();
                for (int i = 1; i <= colCount; i++) {
                    if (i > 1) line.append(';');
                    line.append(rs.getString(i));
                }
                log(line.toString());
            }
            log("QUERY OK " + label + " rows=" + rows + " duration=" + Duration.between(start, Instant.now()).toMillis() + "ms");
        }
    }

    private long queryLong(Connection con, String sql) throws SQLException {
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        return 0L;
    }

    private void requireTable(Connection con, String tableName) throws SQLException {
        String status = objectStatus(con, "TABLE", tableName);
        if (status == null) {
            throw new SQLException("No existe tabla requerida " + SCHEMA + "." + tableName);
        }
        log("OK table " + tableName + " status=" + status);
    }

    private void requireProcedure(Connection con, String procedureName) throws SQLException {
        try (Connection c = con == null ? openConnection() : con) {
            String status = objectStatus(c, "PROCEDURE", procedureName);
            if (!"VALID".equals(status)) {
                throw new SQLException("Procedimiento requerido no está VALID: " + SCHEMA + "." + procedureName + " status=" + status);
            }
            log("OK procedure " + procedureName + " status=" + status);
        }
    }

    private String objectStatus(Connection con, String objectType, String objectName) throws SQLException {
        String sql = "select STATUS from all_objects where OWNER = ? and OBJECT_TYPE = ? and OBJECT_NAME = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, SCHEMA);
            ps.setString(2, objectType);
            ps.setString(3, objectName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(db.url, db.user, db.password);
    }

    private static Options parseOptions(String[] args) {
        Options options = new Options();
        for (String rawArg : args) {
            String arg = rawArg.trim();
            if (arg.isEmpty()) {
                continue;
            }
            if ("--help".equalsIgnoreCase(arg) || "-h".equalsIgnoreCase(arg)) {
                options.help = true;
            } else if ("--full".equalsIgnoreCase(arg)) {
                options.from = Phase.BASE;
                options.only = null;
            } else if (arg.startsWith("--from=")) {
                options.from = parsePhase(arg.substring("--from=".length()));
                options.only = null;
            } else if (arg.startsWith("--only=")) {
                options.only = parsePhase(arg.substring("--only=".length()));
            } else if ("--no-views".equalsIgnoreCase(arg)) {
                options.views = false;
            } else if ("--dry-run".equalsIgnoreCase(arg)) {
                options.dryRun = true;
            } else if (arg.startsWith("--poll-seconds=")) {
                options.pollSeconds = Long.parseLong(arg.substring("--poll-seconds=".length()));
            } else if (arg.startsWith("--stall-warn-minutes=")) {
                options.stallWarnMinutes = Long.parseLong(arg.substring("--stall-warn-minutes=".length()));
            } else {
                throw new IllegalArgumentException("Argumento no reconocido: " + arg);
            }
        }
        if (options.pollSeconds < 5) {
            throw new IllegalArgumentException("--poll-seconds debe ser >= 5");
        }
        return options;
    }

    private static Phase parsePhase(String value) {
        String normalized = value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
        switch (normalized) {
            case "BASE": return Phase.BASE;
            case "CONFIG": return Phase.CONFIG;
            case "DERIVED": return Phase.DERIVED;
            case "DEFINED_GLOBAL": return Phase.DEFINED_GLOBAL;
            case "GLOBAL": return Phase.DEFINED_GLOBAL;
            case "DEFINED_LOCAL": return Phase.DEFINED_LOCAL;
            case "LOCAL": return Phase.DEFINED_LOCAL;
            case "FINAL": return Phase.FINAL;
            case "VIEWS": return Phase.VIEWS;
            case "VALIDATE": return Phase.VALIDATE;
            default: throw new IllegalArgumentException("Fase no reconocida: " + value + ". Usa base, config, derived, defined-global, defined-local, final, views, validate");
        }
    }

    private static DbConfig loadDbConfig() {
        String url = firstNonBlank(System.getenv("ORACLE_JDBC_URL"), System.getenv("P360_EXPLOIT_JDBC_URL"));
        String user = firstNonBlank(System.getenv("ORACLE_JDBC_USER"), System.getenv("P360_EXPLOIT_JDBC_USER"));
        String password = firstNonBlank(System.getenv("ORACLE_JDBC_PASSWORD"), System.getenv("P360_EXPLOIT_JDBC_PASSWORD"));
        if (url == null || user == null || password == null) {
            throw new IllegalStateException("Faltan ORACLE_JDBC_URL / ORACLE_JDBC_USER / ORACLE_JDBC_PASSWORD o P360_EXPLOIT_JDBC_URL / P360_EXPLOIT_JDBC_USER / P360_EXPLOIT_JDBC_PASSWORD");
        }
        return new DbConfig(url, user, password);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String collapse(String sql) {
        return sql == null ? "" : sql.replace('\n', ' ').replaceAll("\\s+", " ").trim();
    }

    private static void log(String message) {
        System.out.println("[" + LocalDateTime.now().format(LOG_TS) + "] " + message);
        System.out.flush();
    }

    private static void printHelp() {
        System.out.println("Uso:");
        System.out.println("  java -cp bin:\"lib/*\" com.example.ei.forfun.logic.ProfileProduct2GRefreshSupervisor --full");
        System.out.println("  java -cp bin:\"lib/*\" com.example.ei.forfun.logic.ProfileProduct2GRefreshSupervisor --from=derived");
        System.out.println("  java -cp bin:\"lib/*\" com.example.ei.forfun.logic.ProfileProduct2GRefreshSupervisor --only=defined-local");
        System.out.println();
        System.out.println("Fases: base, config, derived, defined-global, defined-local, final, views, validate");
        System.out.println("Opciones: --no-views --poll-seconds=30 --stall-warn-minutes=15 --dry-run");
    }
}
