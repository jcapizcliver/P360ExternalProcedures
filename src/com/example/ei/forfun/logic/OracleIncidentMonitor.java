package com.example.ei.forfun.logic;

import java.io.BufferedWriter;
import java.io.IOException;
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
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Monitor de incidentes Oracle/P360 para pruebas controladas.
 *
 * Objetivo:
 *  - detectar el segundo en que la BD pasa de responder normal a responder lento;
 *  - correlacionar latencia del canario con CPU/DB time, logical I/O, redo,
 *    waits, latches, I/O, sesiones, SQL activos, TEMP, UNDO, blockers y pools;
 *  - NO ejecuta DML, DDL ni KILL SESSION.
 *
 * Credenciales: exactamente el mismo mecanismo de SqlRunner:
 *   ORACLE_JDBC_URL
 *   ORACLE_JDBC_USER
 *   ORACLE_JDBC_PASSWORD
 *
 * Uso:
 *   java ... OracleIncidentMonitor [directorioSalida] [intervaloMs] [queryTimeoutSeg]
 *
 * Ejemplo prueba controlada, 1 muestra/segundo:
 *   java ... OracleIncidentMonitor incident_$(date +%Y%m%d_%H%M%S) 1000 4
 *
 * En la consola mientras corre:
 *   MARK START
 *   MARK EXPORT_LAUNCHED
 *   MARK SLOW
 *   MARK ABORT
 *   MARK RECOVERED
 *   STATUS
 *   STOP
 *
 * Recomendación:
 *   - 1000 ms para prueba controlada de pocos minutos.
 *   - 5000 ms para dejarlo horas sin generar tantos datos.
 */
public class OracleIncidentMonitor implements AutoCloseable {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter DIR_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final String[] SYSSTAT_NAMES = {
            "user commits",
            "user rollbacks",
            "transaction rollbacks",
            "rollback changes - undo records applied",
            "logons cumulative",
            "logons current",
            "redo size",
            "redo writes",
            "redo wastage",
            "redo buffer allocation retries",
            "session logical reads",
            "physical reads",
            "physical writes",
            "physical reads direct",
            "physical writes direct",
            "physical writes direct temporary tablespace",
            "execute count",
            "parse count (total)",
            "parse count (hard)",
            "DB block changes"
    };

    private static final String[] EVENT_NAMES = {
            "log file sync",
            "log file parallel write",
            "log buffer space",
            "control file parallel write",
            "db file sequential read",
            "db file scattered read",
            "read by other session",
            "buffer busy waits",
            "latch: In memory undo latch",
            "direct path write temp",
            "direct path read temp",
            "enq: CF - contention",
            "Disk file operations I/O",
            "enq: TX - row lock contention",
            "cursor: pin S wait on X",
            "library cache lock",
            "library cache pin",
            "resmgr:cpu quantum"
    };

    private static final String[] LATCH_NAMES = {
            "In memory undo latch",
            "redo allocation",
            "redo copy",
            "cache buffers chains",
            "cache buffers lru chain",
            "shared pool",
            "row cache objects"
    };

    private static final String[] SESSION_STAT_NAMES = {
            "CPU used by this session",
            "session logical reads",
            "physical reads",
            "physical writes",
            "redo size"
    };

    private final String url;
    private final String user;
    private final String password;
    private final Path outputDir;
    private final long intervalMs;
    private final int queryTimeoutSeconds;

    private Connection monitorConnection;
    private Connection canaryConnection;
    private final ExecutorService networkTimeoutExecutor = Executors.newCachedThreadPool();

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicReference<String> lastDbTimestamp = new AtomicReference<>("");
    private final AtomicReference<String> lastStatusLine = new AtomicReference<>("Todavía sin muestras");

    private final CsvWriter samples;
    private final CsvWriter events;
    private final CsvWriter ioFunctions;
    private final CsvWriter latches;
    private final CsvWriter bufferWaits;
    private final CsvWriter activeSessions;
    private final CsvWriter topSql;
    private final CsvWriter poolSessions;
    private final CsvWriter blockers;
    private final CsvWriter criticalSessions;
    private final CsvWriter resources;
    private final CsvWriter markers;
    private final CsvWriter errors;

    private final Map<String, Long> prevSysstat = new HashMap<>();
    private final Map<String, Long> prevTimeModel = new HashMap<>();
    private final Map<String, Long> prevOsstat = new HashMap<>();
    private final Map<String, EventCounter> prevEvents = new HashMap<>();
    private final Map<String, IoCounter> prevIo = new HashMap<>();
    private final Map<String, LatchCounter> prevLatches = new HashMap<>();
    private final Map<String, WaitStatCounter> prevBufferWaits = new HashMap<>();
    private final Map<String, SqlCounter> prevSql = new HashMap<>();
    private final Map<String, SessionCounter> prevSessionStats = new HashMap<>();

    private long sampleId = 0;
    private long previousSampleNano = 0;

    public static void main(String[] args) throws Exception {
        String url = mustEnv("ORACLE_JDBC_URL");
        String usr = mustEnv("ORACLE_JDBC_USER");
        String pwd = mustEnv("ORACLE_JDBC_PASSWORD");

        Path out = args.length >= 1
                ? Paths.get(args[0])
                : Paths.get("oracle_incident_" + LocalDateTime.now().format(DIR_TS));
        long interval = args.length >= 2 ? Long.parseLong(args[1]) : 1000L;
        int timeout = args.length >= 3 ? Integer.parseInt(args[2]) : 4;

        if (interval < 500L) {
            throw new IllegalArgumentException("intervaloMs mínimo recomendado/permitido: 500");
        }
        if (timeout < 1) {
            throw new IllegalArgumentException("queryTimeoutSeg debe ser >= 1");
        }

        try (OracleIncidentMonitor monitor = new OracleIncidentMonitor(url, usr, pwd, out, interval, timeout)) {
            monitor.run();
        }
    }

    public OracleIncidentMonitor(String url, String user, String password,
                                 Path outputDir, long intervalMs, int queryTimeoutSeconds) throws IOException {
        this.url = url;
        this.user = user;
        this.password = password;
        this.outputDir = outputDir;
        this.intervalMs = intervalMs;
        this.queryTimeoutSeconds = queryTimeoutSeconds;

        Files.createDirectories(outputDir);

        samples = csv("samples.csv",
                "CLIENT_TS", "DB_TS", "SAMPLE_ID", "INTERVAL_MS", "CYCLE_MS",
                "CANARY_MS", "CANARY_STATUS", "MONITOR_QUERY_MS",
                "DB_CPU_DELTA_S", "DB_TIME_DELTA_S", "BG_CPU_DELTA_S",
                "HOST_CPU_PCT", "HOST_LOAD", "NUM_CPUS",
                "USER_COMMITS_DELTA", "USER_ROLLBACKS_DELTA", "TRANSACTION_ROLLBACKS_DELTA",
                "UNDO_APPLIED_DELTA", "LOGONS_DELTA", "LOGONS_PER_S", "LOGONS_CURRENT",
                "REDO_MB_DELTA", "REDO_MB_PER_S", "REDO_WRITES_DELTA", "REDO_WASTAGE_DELTA",
                "REDO_BUFFER_RETRIES_DELTA", "SESSION_LOGICAL_READS_DELTA",
                "PHYSICAL_READS_DELTA", "PHYSICAL_WRITES_DELTA", "PHYSICAL_READS_DIRECT_DELTA",
                "PHYSICAL_WRITES_DIRECT_DELTA", "TEMP_DIRECT_WRITES_DELTA",
                "EXECUTE_COUNT_DELTA", "PARSE_TOTAL_DELTA", "PARSE_HARD_DELTA", "DB_BLOCK_CHANGES_DELTA",
                "ACTIVE_USER_SESSIONS", "ACTIVE_PIM_MASTER", "ACTIVE_PIM_MAIN");

        events = csv("events.csv",
                "CLIENT_TS", "DB_TS", "SAMPLE_ID", "EVENT", "WAITS_DELTA",
                "WAIT_MS_DELTA", "AVG_WAIT_MS_DELTA", "TOTAL_WAITS", "TOTAL_WAIT_MS");

        ioFunctions = csv("io_function.csv",
                "CLIENT_TS", "DB_TS", "SAMPLE_ID", "FUNCTION_NAME",
                "READ_MB_DELTA", "WRITE_MB_DELTA", "READ_IOPS", "WRITE_IOPS",
                "WAITS_DELTA", "WAIT_MS_DELTA", "AVG_WAIT_MS_DELTA");

        latches = csv("latches.csv",
                "CLIENT_TS", "DB_TS", "SAMPLE_ID", "LATCH_NAME",
                "GETS_DELTA", "MISSES_DELTA", "SLEEPS_DELTA", "SPIN_GETS_DELTA");

        bufferWaits = csv("buffer_waits.csv",
                "CLIENT_TS", "DB_TS", "SAMPLE_ID", "BLOCK_CLASS",
                "COUNT_DELTA", "TIME_CS_DELTA", "TOTAL_COUNT", "TOTAL_TIME_CS");

        activeSessions = csv("active_sessions.csv",
                "CLIENT_TS", "DB_TS", "SAMPLE_ID", "INST_ID", "SID", "SERIAL#", "USERNAME", "STATUS",
                "SQL_ID", "SQL_EXEC_ID", "SQL_EXEC_START", "EXEC_S", "EVENT", "WAIT_CLASS", "STATE",
                "SECONDS_IN_WAIT", "BLOCKING_INSTANCE", "BLOCKING_SESSION", "WAITERS",
                "MACHINE", "MODULE", "ACTION", "PROGRAM", "UNDO_BLKS", "UNDO_RECS", "TEMP_BLOCKS",
                "CPU_CS", "CPU_CS_DELTA", "LOGICAL_READS", "LOGICAL_READS_DELTA",
                "PHYSICAL_READS", "PHYSICAL_READS_DELTA", "PHYSICAL_WRITES", "PHYSICAL_WRITES_DELTA",
                "REDO_SIZE", "REDO_SIZE_DELTA", "SQL_TEXT");

        topSql = csv("top_sql.csv",
                "CLIENT_TS", "DB_TS", "SAMPLE_ID", "INST_ID", "SQL_ID", "ACTIVE_SESSIONS",
                "EXECUTIONS", "EXECUTIONS_DELTA", "CPU_S", "CPU_S_DELTA", "ELAPSED_S", "ELAPSED_S_DELTA",
                "BUFFER_GETS", "BUFFER_GETS_DELTA", "DISK_READS", "DISK_READS_DELTA",
                "DIRECT_WRITES", "DIRECT_WRITES_DELTA", "ROWS_PROCESSED", "ROWS_PROCESSED_DELTA",
                "USER_IO_WAIT_S", "USER_IO_WAIT_S_DELTA", "CONCURRENCY_WAIT_S", "CONCURRENCY_WAIT_S_DELTA",
                "APPLICATION_WAIT_S", "APPLICATION_WAIT_S_DELTA", "SQL_TEXT");

        poolSessions = csv("pool_sessions.csv",
                "CLIENT_TS", "DB_TS", "SAMPLE_ID", "INST_ID", "USERNAME", "MACHINE",
                "TOTAL", "ACTIVE", "INACTIVE", "NEW_1M", "NEW_5M", "NEW_15M",
                "OLDEST_LOGON", "NEWEST_LOGON");

        blockers = csv("blockers.csv",
                "CLIENT_TS", "DB_TS", "SAMPLE_ID", "INST_ID", "SID", "SERIAL#", "TYPE", "USERNAME",
                "SQL_ID", "EVENT", "STATE", "SECONDS_IN_WAIT", "PROGRAM", "MACHINE", "WAITERS",
                "UNDO_BLKS", "UNDO_RECS", "SQL_TEXT");

        criticalSessions = csv("critical_sessions.csv",
                "CLIENT_TS", "DB_TS", "SAMPLE_ID", "INST_ID", "SID", "SERIAL#", "TYPE", "USERNAME",
                "STATUS", "SQL_ID", "EVENT", "STATE", "SECONDS_IN_WAIT", "BLOCKING_INSTANCE",
                "BLOCKING_SESSION", "PROGRAM", "MACHINE", "MODULE");

        resources = csv("resources.csv",
                "CLIENT_TS", "DB_TS", "SAMPLE_ID", "RESOURCE_NAME", "CURRENT_UTILIZATION",
                "MAX_UTILIZATION", "LIMIT_VALUE");

        markers = csv("markers.csv", "CLIENT_TS", "LAST_DB_TS", "SAMPLE_ID", "MARK");
        errors = csv("errors.csv", "CLIENT_TS", "DB_TS", "SAMPLE_ID", "AREA", "ELAPSED_MS", "ERROR");
    }

    private CsvWriter csv(String name, String... header) throws IOException {
        return new CsvWriter(outputDir.resolve(name), header);
    }

    public void run() {
        System.out.println("OracleIncidentMonitor iniciado");
        System.out.println("Salida: " + outputDir.toAbsolutePath());
        System.out.println("Intervalo: " + intervalMs + " ms | Query timeout: " + queryTimeoutSeconds + " s");
        System.out.println("Comandos: MARK <texto> | STATUS | STOP");

        startConsoleThread();
        addShutdownHook();

        long next = System.nanoTime();
        while (running.get()) {
            long cycleStart = System.nanoTime();
            sampleId++;

            long nowNano = System.nanoTime();
            double intervalSeconds;
            long actualIntervalMs;
            if (previousSampleNano == 0) {
                actualIntervalMs = intervalMs;
                intervalSeconds = intervalMs / 1000.0;
            } else {
                actualIntervalMs = Math.max(1L, (nowNano - previousSampleNano) / 1_000_000L);
                intervalSeconds = actualIntervalMs / 1000.0;
            }
            previousSampleNano = nowNano;

            String clientTs = now();
            CanaryResult canary = sampleCanary();
            if (!canary.dbTimestamp.isEmpty()) {
                lastDbTimestamp.set(canary.dbTimestamp);
            }
            String dbTs = lastDbTimestamp.get();

            long monitorQueriesStart = System.nanoTime();

            Map<String, Long> sysstat = sampleSysstat();
            Map<String, Long> timeModel = sampleTimeModel();
            Map<String, Long> osstat = sampleOsstat();

            EventSample eventSample = sampleEvents(clientTs, dbTs);
            sampleIo(clientTs, dbTs, intervalSeconds);
            sampleLatches(clientTs, dbTs);
            sampleBufferWaits(clientTs, dbTs);

            List<ActiveSession> active = sampleActiveSessions();
            sampleSessionStats(active);
            sampleTempUsage(active);
            writeActiveSessions(active, clientTs, dbTs);
            sampleTopSql(active, clientTs, dbTs);

            samplePools(clientTs, dbTs);
            sampleBlockers(clientTs, dbTs);
            sampleCriticalSessions(clientTs, dbTs);
            sampleResources(clientTs, dbTs);

            long monitorQueryMs = (System.nanoTime() - monitorQueriesStart) / 1_000_000L;
            long cycleMs = (System.nanoTime() - cycleStart) / 1_000_000L;

            writeMainSample(clientTs, dbTs, actualIntervalMs, cycleMs, canary,
                    monitorQueryMs, intervalSeconds, sysstat, timeModel, osstat, active);

            flushAll();

            long activePimMaster = active.stream().filter(a -> "PIM_MASTER".equals(a.username)).count();
            String status = String.format(Locale.ROOT,
                    "sample=%d db=%s canary=%s active=%d pimMaster=%d logSyncWaitMsDelta=%s cycle=%dms",
                    sampleId,
                    dbTs,
                    canary.status.equals("OK") ? canary.elapsedMs + "ms" : canary.status,
                    active.size(), activePimMaster,
                    eventSample.logFileSyncWaitMsDelta == null ? "" : eventSample.logFileSyncWaitMsDelta,
                    cycleMs);
            lastStatusLine.set(status);

            if (canary.elapsedMs >= 1000 || cycleMs >= Math.max(2000, intervalMs * 2)) {
                System.out.println("*** SLOW *** " + status);
            } else if (sampleId % Math.max(1, 10_000L / intervalMs) == 0) {
                System.out.println(status);
            }

            next += intervalMs * 1_000_000L;
            long sleepNanos = next - System.nanoTime();
            if (sleepNanos > 0) {
                try {
                    long ms = sleepNanos / 1_000_000L;
                    int ns = (int) (sleepNanos % 1_000_000L);
                    Thread.sleep(ms, ns);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                // Si la BD está lenta no intentamos "alcanzar" muestras perdidas disparando ráfagas.
                next = System.nanoTime();
            }
        }

        System.out.println("Monitor detenido. Último estado: " + lastStatusLine.get());
    }

    private CanaryResult sampleCanary() {
        long start = System.nanoTime();
        try {
            Connection c = getCanaryConnection();
            try (PreparedStatement ps = c.prepareStatement(
                    "select /* P360_DIAG_CANARY */ to_char(systimestamp,'YYYY-MM-DD HH24:MI:SS.FF3 TZH:TZM') from dual")) {
                ps.setQueryTimeout(queryTimeoutSeconds);
                try (ResultSet rs = ps.executeQuery()) {
                    String dbTs = rs.next() ? rs.getString(1) : "";
                    long ms = (System.nanoTime() - start) / 1_000_000L;
                    return new CanaryResult(ms, "OK", dbTs);
                }
            }
        } catch (Exception e) {
            long ms = (System.nanoTime() - start) / 1_000_000L;
            logError("CANARY", ms, e);
            resetCanaryConnection();
            return new CanaryResult(ms, "ERROR", "");
        }
    }

    private Map<String, Long> sampleSysstat() {
        Map<String, Long> out = new HashMap<>();
        String sql = "select name,sum(value) value from gv$sysstat where name in (" + sqlStringList(SYSSTAT_NAMES) + ") group by name";
        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.put(rs.getString(1), rs.getLong(2));
            }
        } catch (Exception e) {
            failMonitorQuery("SYSSTAT", start, e);
        }
        return out;
    }

    private Map<String, Long> sampleTimeModel() {
        Map<String, Long> out = new HashMap<>();
        String sql = "select stat_name,sum(value) value from gv$sys_time_model " +
                "where stat_name in ('DB time','DB CPU','background cpu time') group by stat_name";
        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.put(rs.getString(1), rs.getLong(2));
            }
        } catch (Exception e) {
            failMonitorQuery("TIME_MODEL", start, e);
        }
        return out;
    }

    private Map<String, Long> sampleOsstat() {
        Map<String, Long> out = new HashMap<>();
        String sql = "select stat_name,value from v$osstat where stat_name in ('NUM_CPUS','NUM_CPU_CORES','BUSY_TIME','IDLE_TIME','LOAD')";
        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                out.put(rs.getString(1), rs.getLong(2));
            }
        } catch (Exception e) {
            failMonitorQuery("OSSTAT", start, e);
        }
        return out;
    }

    private EventSample sampleEvents(String clientTs, String dbTs) {
        Map<String, EventCounter> current = new HashMap<>();
        String sql = "select event,sum(total_waits) total_waits,sum(time_waited_micro) time_waited_micro " +
                "from gv$system_event where event in (" + sqlStringList(EVENT_NAMES) + ") group by event";
        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                current.put(rs.getString(1), new EventCounter(rs.getLong(2), rs.getLong(3)));
            }
        } catch (Exception e) {
            failMonitorQuery("SYSTEM_EVENT", start, e);
            return new EventSample(null);
        }

        Double logSyncWaitMs = null;
        for (String name : EVENT_NAMES) {
            EventCounter c = current.get(name);
            if (c == null) continue;
            EventCounter p = prevEvents.get(name);
            Long waitsDelta = delta(c.waits, p == null ? null : p.waits);
            Long microsDelta = delta(c.timeWaitedMicro, p == null ? null : p.timeWaitedMicro);
            Double waitMsDelta = microsDelta == null ? null : microsDelta / 1000.0;
            Double avgMs = waitsDelta == null || waitsDelta <= 0 || waitMsDelta == null ? null : waitMsDelta / waitsDelta;

            events.write(clientTs, dbTs, sampleId, name,
                    n(waitsDelta), n(waitMsDelta), n(avgMs), c.waits, c.timeWaitedMicro / 1000.0);

            if ("log file sync".equals(name)) logSyncWaitMs = waitMsDelta;
        }
        prevEvents.clear();
        prevEvents.putAll(current);
        return new EventSample(logSyncWaitMs);
    }

    private void sampleIo(String clientTs, String dbTs, double intervalSeconds) {
        Map<String, IoCounter> current = new HashMap<>();
        String sql = "select function_name," +
                "small_read_megabytes+large_read_megabytes read_mb," +
                "small_write_megabytes+large_write_megabytes write_mb," +
                "small_read_reqs+large_read_reqs read_reqs," +
                "small_write_reqs+large_write_reqs write_reqs," +
                "number_of_waits,wait_time from v$iostat_function " +
                "where function_name in ('LGWR','DBWR','Buffer Cache Reads','Direct Reads','Direct Writes','RMAN','Others')";
        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                current.put(rs.getString(1), new IoCounter(
                        rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5), rs.getLong(6), rs.getLong(7)));
            }
        } catch (Exception e) {
            failMonitorQuery("IOSTAT_FUNCTION", start, e);
            return;
        }

        for (Map.Entry<String, IoCounter> entry : current.entrySet()) {
            String name = entry.getKey();
            IoCounter c = entry.getValue();
            IoCounter p = prevIo.get(name);
            Long readMb = delta(c.readMb, p == null ? null : p.readMb);
            Long writeMb = delta(c.writeMb, p == null ? null : p.writeMb);
            Long readReq = delta(c.readReqs, p == null ? null : p.readReqs);
            Long writeReq = delta(c.writeReqs, p == null ? null : p.writeReqs);
            Long waits = delta(c.waits, p == null ? null : p.waits);
            Long waitMs = delta(c.waitTimeMs, p == null ? null : p.waitTimeMs);
            Double avgWait = waits == null || waits <= 0 || waitMs == null ? null : waitMs.doubleValue() / waits;

            ioFunctions.write(clientTs, dbTs, sampleId, name,
                    n(readMb), n(writeMb), rate(readReq, intervalSeconds), rate(writeReq, intervalSeconds),
                    n(waits), n(waitMs), n(avgWait));
        }
        prevIo.clear();
        prevIo.putAll(current);
    }

    private void sampleLatches(String clientTs, String dbTs) {
        Map<String, LatchCounter> current = new HashMap<>();
        String sql = "select name,sum(gets),sum(misses),sum(sleeps),sum(spin_gets) from gv$latch " +
                "where name in (" + sqlStringList(LATCH_NAMES) + ") group by name";
        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                current.put(rs.getString(1), new LatchCounter(rs.getLong(2), rs.getLong(3), rs.getLong(4), rs.getLong(5)));
            }
        } catch (Exception e) {
            failMonitorQuery("LATCH", start, e);
            return;
        }

        for (Map.Entry<String, LatchCounter> e : current.entrySet()) {
            LatchCounter c = e.getValue();
            LatchCounter p = prevLatches.get(e.getKey());
            latches.write(clientTs, dbTs, sampleId, e.getKey(),
                    n(delta(c.gets, p == null ? null : p.gets)),
                    n(delta(c.misses, p == null ? null : p.misses)),
                    n(delta(c.sleeps, p == null ? null : p.sleeps)),
                    n(delta(c.spinGets, p == null ? null : p.spinGets)));
        }
        prevLatches.clear();
        prevLatches.putAll(current);
    }

    private void sampleBufferWaits(String clientTs, String dbTs) {
        Map<String, WaitStatCounter> current = new HashMap<>();
        String sql = "select class,sum(count) cnt,sum(time) tm from gv$waitstat group by class";
        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                current.put(rs.getString(1), new WaitStatCounter(rs.getLong(2), rs.getLong(3)));
            }
        } catch (Exception e) {
            failMonitorQuery("WAITSTAT", start, e);
            return;
        }

        for (Map.Entry<String, WaitStatCounter> e : current.entrySet()) {
            WaitStatCounter c = e.getValue();
            WaitStatCounter p = prevBufferWaits.get(e.getKey());
            Long countDelta = delta(c.count, p == null ? null : p.count);
            Long timeDelta = delta(c.timeCs, p == null ? null : p.timeCs);
            if ((countDelta != null && countDelta != 0) || sampleId == 1) {
                bufferWaits.write(clientTs, dbTs, sampleId, e.getKey(),
                        n(countDelta), n(timeDelta), c.count, c.timeCs);
            }
        }
        prevBufferWaits.clear();
        prevBufferWaits.putAll(current);
    }

    private List<ActiveSession> sampleActiveSessions() {
        List<ActiveSession> list = new ArrayList<>();
        String sql = "select s.inst_id,s.sid,s.serial#,s.username,s.status,s.sql_id,s.sql_exec_id," +
                "to_char(s.sql_exec_start,'YYYY-MM-DD HH24:MI:SS')," +
                "case when s.sql_exec_start is not null then round((sysdate-s.sql_exec_start)*86400,3) end exec_s," +
                "s.event,s.wait_class,s.state,s.seconds_in_wait,s.blocking_instance,s.blocking_session," +
                "s.machine,s.module,s.action,s.program," +
                "(select count(*) from gv$session w where w.blocking_instance=s.inst_id and w.blocking_session=s.sid) waiters," +
                "nvl(t.used_ublk,0),nvl(t.used_urec,0),substr(q.sql_text,1,1000) " +
                "from gv$session s " +
                "left join gv$transaction t on t.inst_id=s.inst_id and t.addr=s.taddr " +
                "left join gv$sql q on q.inst_id=s.inst_id and q.sql_id=s.sql_id and q.child_number=s.sql_child_number " +
                "where s.type='USER' and s.status='ACTIVE' and s.username is not null " +
                "and s.audsid <> sys_context('USERENV','SESSIONID') " +
                "order by waiters desc,exec_s desc nulls last";
        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ActiveSession a = new ActiveSession();
                int i = 1;
                a.instId = rs.getInt(i++);
                a.sid = rs.getInt(i++);
                a.serial = rs.getLong(i++);
                a.username = rs.getString(i++);
                a.status = rs.getString(i++);
                a.sqlId = rs.getString(i++);
                a.sqlExecId = getNullableLong(rs, i++);
                a.sqlExecStart = rs.getString(i++);
                a.execSeconds = getNullableDouble(rs, i++);
                a.event = rs.getString(i++);
                a.waitClass = rs.getString(i++);
                a.state = rs.getString(i++);
                a.secondsInWait = getNullableLong(rs, i++);
                a.blockingInstance = getNullableLong(rs, i++);
                a.blockingSession = getNullableLong(rs, i++);
                a.machine = rs.getString(i++);
                a.module = rs.getString(i++);
                a.action = rs.getString(i++);
                a.program = rs.getString(i++);
                a.waiters = rs.getLong(i++);
                a.undoBlocks = rs.getLong(i++);
                a.undoRecords = rs.getLong(i++);
                a.sqlText = rs.getString(i);
                list.add(a);
            }
        } catch (Exception e) {
            failMonitorQuery("ACTIVE_SESSIONS", start, e);
        }
        return list;
    }

    private void sampleSessionStats(List<ActiveSession> sessions) {
        if (sessions.isEmpty()) return;
        Map<String, ActiveSession> byInstSid = new HashMap<>();
        Map<Integer, Set<Integer>> sidsByInst = new TreeMap<>();
        for (ActiveSession a : sessions) {
            byInstSid.put(a.instId + ":" + a.sid, a);
            sidsByInst.computeIfAbsent(a.instId, k -> new LinkedHashSet<>()).add(a.sid);
        }

        StringBuilder where = new StringBuilder();
        for (Map.Entry<Integer, Set<Integer>> e : sidsByInst.entrySet()) {
            if (where.length() > 0) where.append(" or ");
            where.append("(ss.inst_id=").append(e.getKey()).append(" and ss.sid in (");
            boolean first = true;
            for (Integer sid : e.getValue()) {
                if (!first) where.append(',');
                where.append(sid);
                first = false;
            }
            where.append("))");
        }

        String sql = "select ss.inst_id,ss.sid,sn.name,ss.value from gv$sesstat ss " +
                "join gv$statname sn on sn.inst_id=ss.inst_id and sn.statistic#=ss.statistic# " +
                "where sn.name in (" + sqlStringList(SESSION_STAT_NAMES) + ") and (" + where + ")";

        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ActiveSession a = byInstSid.get(rs.getInt(1) + ":" + rs.getInt(2));
                if (a == null) continue;
                String name = rs.getString(3);
                long value = rs.getLong(4);
                switch (name) {
                    case "CPU used by this session" -> a.cpuCs = value;
                    case "session logical reads" -> a.logicalReads = value;
                    case "physical reads" -> a.physicalReads = value;
                    case "physical writes" -> a.physicalWrites = value;
                    case "redo size" -> a.redoSize = value;
                    default -> { }
                }
            }
        } catch (Exception e) {
            failMonitorQuery("SESSION_STATS", start, e);
        }
    }

    private void sampleTempUsage(List<ActiveSession> sessions) {
        if (sessions.isEmpty()) return;
        Map<String, ActiveSession> byInstSid = new HashMap<>();
        Map<Integer, Set<Integer>> sidsByInst = new TreeMap<>();
        for (ActiveSession a : sessions) {
            byInstSid.put(a.instId + ":" + a.sid, a);
            sidsByInst.computeIfAbsent(a.instId, k -> new LinkedHashSet<>()).add(a.sid);
        }

        StringBuilder filter = new StringBuilder();
        for (Map.Entry<Integer, Set<Integer>> e : sidsByInst.entrySet()) {
            if (filter.length() > 0) filter.append(" or ");
            filter.append("(s.inst_id=").append(e.getKey()).append(" and s.sid in (");
            boolean first = true;
            for (Integer sid : e.getValue()) {
                if (!first) filter.append(',');
                filter.append(sid);
                first = false;
            }
            filter.append("))");
        }

        String sql = "select s.inst_id,s.sid,nvl(sum(u.blocks),0) temp_blocks " +
                "from gv$session s join gv$tempseg_usage u on u.inst_id=s.inst_id and u.session_addr=s.saddr " +
                "where " + filter + " group by s.inst_id,s.sid";
        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                ActiveSession a = byInstSid.get(rs.getInt(1) + ":" + rs.getInt(2));
                if (a != null) a.tempBlocks = rs.getLong(3);
            }
        } catch (Exception e) {
            // TEMP es útil pero no permitimos que un privilegio faltante tumbe el monitor.
            logError("TEMP_USAGE", elapsedMs(start), e);
        }
    }

    private void writeActiveSessions(List<ActiveSession> sessions, String clientTs, String dbTs) {
        Set<String> currentlySeen = new LinkedHashSet<>();
        for (ActiveSession a : sessions) {
            String key = a.sessionKey();
            currentlySeen.add(key);
            SessionCounter p = prevSessionStats.get(key);
            Long cpuDelta = delta(a.cpuCs, p == null ? null : p.cpuCs);
            Long logicalDelta = delta(a.logicalReads, p == null ? null : p.logicalReads);
            Long readDelta = delta(a.physicalReads, p == null ? null : p.physicalReads);
            Long writeDelta = delta(a.physicalWrites, p == null ? null : p.physicalWrites);
            Long redoDelta = delta(a.redoSize, p == null ? null : p.redoSize);

            activeSessions.write(clientTs, dbTs, sampleId, a.instId, a.sid, a.serial, a.username, a.status,
                    a.sqlId, n(a.sqlExecId), a.sqlExecStart, n(a.execSeconds), a.event, a.waitClass, a.state,
                    n(a.secondsInWait), n(a.blockingInstance), n(a.blockingSession), a.waiters,
                    a.machine, a.module, a.action, a.program, a.undoBlocks, a.undoRecords, a.tempBlocks,
                    a.cpuCs, n(cpuDelta), a.logicalReads, n(logicalDelta), a.physicalReads, n(readDelta),
                    a.physicalWrites, n(writeDelta), a.redoSize, n(redoDelta), a.sqlText);

            prevSessionStats.put(key, new SessionCounter(a.cpuCs, a.logicalReads, a.physicalReads, a.physicalWrites, a.redoSize));
        }
        // Limita memoria: dejamos counters sólo de sesiones vistas recientemente en la muestra actual.
        prevSessionStats.keySet().retainAll(currentlySeen);
    }

    private void sampleTopSql(List<ActiveSession> sessions, String clientTs, String dbTs) {
        Map<Integer, Set<String>> idsByInst = new TreeMap<>();
        Map<String, Integer> activeCount = new HashMap<>();
        for (ActiveSession a : sessions) {
            if (a.sqlId == null || a.sqlId.isBlank()) continue;
            idsByInst.computeIfAbsent(a.instId, k -> new LinkedHashSet<>()).add(a.sqlId);
            activeCount.merge(a.instId + ":" + a.sqlId, 1, Integer::sum);
        }
        if (idsByInst.isEmpty()) return;

        StringBuilder where = new StringBuilder();
        for (Map.Entry<Integer, Set<String>> e : idsByInst.entrySet()) {
            if (where.length() > 0) where.append(" or ");
            where.append("(q.inst_id=").append(e.getKey()).append(" and q.sql_id in (");
            boolean first = true;
            for (String sqlId : e.getValue()) {
                if (!first) where.append(',');
                where.append('\'').append(sqlId.replace("'", "''")).append('\'');
                first = false;
            }
            where.append("))");
        }

        String sql = "select q.inst_id,q.sql_id,sum(q.executions),sum(q.cpu_time),sum(q.elapsed_time)," +
                "sum(q.buffer_gets),sum(q.disk_reads),sum(q.direct_writes),sum(q.rows_processed)," +
                "sum(q.user_io_wait_time),sum(q.concurrency_wait_time),sum(q.application_wait_time)," +
                "max(substr(q.sql_text,1,1000)) " +
                "from gv$sql q where " + where + " group by q.inst_id,q.sql_id";

        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int i = 1;
                int inst = rs.getInt(i++);
                String sqlId = rs.getString(i++);
                SqlCounter c = new SqlCounter(
                        rs.getLong(i++), rs.getLong(i++), rs.getLong(i++), rs.getLong(i++),
                        rs.getLong(i++), rs.getLong(i++), rs.getLong(i++), rs.getLong(i++),
                        rs.getLong(i++), rs.getLong(i++), rs.getString(i));
                String key = inst + ":" + sqlId;
                SqlCounter p = prevSql.get(key);

                topSql.write(clientTs, dbTs, sampleId, inst, sqlId, activeCount.getOrDefault(key, 0),
                        c.executions, n(delta(c.executions, p == null ? null : p.executions)),
                        c.cpuMicros / 1_000_000.0, microsDeltaSeconds(c.cpuMicros, p == null ? null : p.cpuMicros),
                        c.elapsedMicros / 1_000_000.0, microsDeltaSeconds(c.elapsedMicros, p == null ? null : p.elapsedMicros),
                        c.bufferGets, n(delta(c.bufferGets, p == null ? null : p.bufferGets)),
                        c.diskReads, n(delta(c.diskReads, p == null ? null : p.diskReads)),
                        c.directWrites, n(delta(c.directWrites, p == null ? null : p.directWrites)),
                        c.rowsProcessed, n(delta(c.rowsProcessed, p == null ? null : p.rowsProcessed)),
                        c.userIoMicros / 1_000_000.0, microsDeltaSeconds(c.userIoMicros, p == null ? null : p.userIoMicros),
                        c.concurrencyMicros / 1_000_000.0, microsDeltaSeconds(c.concurrencyMicros, p == null ? null : p.concurrencyMicros),
                        c.applicationMicros / 1_000_000.0, microsDeltaSeconds(c.applicationMicros, p == null ? null : p.applicationMicros),
                        c.sqlText);
                prevSql.put(key, c);
            }
        } catch (Exception e) {
            failMonitorQuery("TOP_SQL", start, e);
        }

        // Evita crecimiento infinito de SQL IDs antiguos.
        Set<String> activeKeys = activeCount.keySet();
        prevSql.keySet().retainAll(activeKeys);
    }

    private void samplePools(String clientTs, String dbTs) {
        String sql = "select inst_id,username,nvl(machine,'(null)'),count(*) total," +
                "sum(case when status='ACTIVE' then 1 else 0 end) active," +
                "sum(case when status='INACTIVE' then 1 else 0 end) inactive," +
                "sum(case when logon_time>=sysdate-1/1440 then 1 else 0 end) new_1m," +
                "sum(case when logon_time>=sysdate-5/1440 then 1 else 0 end) new_5m," +
                "sum(case when logon_time>=sysdate-15/1440 then 1 else 0 end) new_15m," +
                "to_char(min(logon_time),'YYYY-MM-DD HH24:MI:SS'),to_char(max(logon_time),'YYYY-MM-DD HH24:MI:SS') " +
                "from gv$session where username in ('PIM_MAIN','PIM_MASTER') " +
                "group by inst_id,username,nvl(machine,'(null)') order by username,machine,inst_id";
        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                poolSessions.write(clientTs, dbTs, sampleId,
                        rs.getInt(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getLong(5),
                        rs.getLong(6), rs.getLong(7), rs.getLong(8), rs.getLong(9), rs.getString(10), rs.getString(11));
            }
        } catch (Exception e) {
            failMonitorQuery("POOL_SESSIONS", start, e);
        }
    }

    private void sampleBlockers(String clientTs, String dbTs) {
        String sql = "select s.inst_id,s.sid,s.serial#,s.type,s.username,s.sql_id,s.event,s.state,s.seconds_in_wait," +
                "s.program,s.machine,count(w.sid) waiters,nvl(t.used_ublk,0),nvl(t.used_urec,0),substr(q.sql_text,1,1000) " +
                "from gv$session s join gv$session w on w.blocking_instance=s.inst_id and w.blocking_session=s.sid " +
                "left join gv$transaction t on t.inst_id=s.inst_id and t.addr=s.taddr " +
                "left join gv$sql q on q.inst_id=s.inst_id and q.sql_id=s.sql_id and q.child_number=s.sql_child_number " +
                "group by s.inst_id,s.sid,s.serial#,s.type,s.username,s.sql_id,s.event,s.state,s.seconds_in_wait," +
                "s.program,s.machine,t.used_ublk,t.used_urec,q.sql_text order by waiters desc";
        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                blockers.write(clientTs, dbTs, sampleId,
                        rs.getInt(1), rs.getInt(2), rs.getLong(3), rs.getString(4), rs.getString(5),
                        rs.getString(6), rs.getString(7), rs.getString(8), getNullableLong(rs, 9),
                        rs.getString(10), rs.getString(11), rs.getLong(12), rs.getLong(13), rs.getLong(14), rs.getString(15));
            }
        } catch (Exception e) {
            failMonitorQuery("BLOCKERS", start, e);
        }
    }

    private void sampleCriticalSessions(String clientTs, String dbTs) {
        String sql = "select inst_id,sid,serial#,type,username,status,sql_id,event,state,seconds_in_wait," +
                "blocking_instance,blocking_session,program,machine,module from gv$session " +
                "where program like '%LGWR%' or event in ('enq: CF - contention','log buffer space','log file parallel write'," +
                "'control file parallel write','latch: In memory undo latch','buffer busy waits','Disk file operations I/O') " +
                "order by type,event,seconds_in_wait desc";
        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                criticalSessions.write(clientTs, dbTs, sampleId,
                        rs.getInt(1), rs.getInt(2), rs.getLong(3), rs.getString(4), rs.getString(5),
                        rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9), getNullableLong(rs, 10),
                        n(getNullableLong(rs, 11)), n(getNullableLong(rs, 12)), rs.getString(13), rs.getString(14), rs.getString(15));
            }
        } catch (Exception e) {
            failMonitorQuery("CRITICAL_SESSIONS", start, e);
        }

        sampleCfLocks(clientTs, dbTs);
    }

    private void sampleCfLocks(String clientTs, String dbTs) {
        String sql = "select l.inst_id,l.sid,s.serial#,s.type,s.username,s.status,s.sql_id,s.event,s.state,s.seconds_in_wait," +
                "s.blocking_instance,s.blocking_session,s.program,s.machine,s.module,l.lmode,l.request,l.block,l.ctime,l.id1,l.id2 " +
                "from gv$lock l left join gv$session s on s.inst_id=l.inst_id and s.sid=l.sid " +
                "where l.type='CF' order by l.block desc,l.request desc,l.ctime desc";
        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String syntheticEvent = "CF_LOCK lmode=" + rs.getLong(16) + " request=" + rs.getLong(17) +
                        " block=" + rs.getLong(18) + " ctime=" + rs.getLong(19) +
                        " id1=" + rs.getLong(20) + " id2=" + rs.getLong(21) +
                        " session_event=" + nullToEmpty(rs.getString(8));
                criticalSessions.write(clientTs, dbTs, sampleId,
                        rs.getInt(1), rs.getInt(2), rs.getLong(3), rs.getString(4), rs.getString(5),
                        rs.getString(6), rs.getString(7), syntheticEvent, rs.getString(9), getNullableLong(rs, 10),
                        n(getNullableLong(rs, 11)), n(getNullableLong(rs, 12)), rs.getString(13), rs.getString(14), rs.getString(15));
            }
        } catch (Exception e) {
            failMonitorQuery("CF_LOCKS", start, e);
        }
    }

    private void sampleResources(String clientTs, String dbTs) {
        String sql = "select resource_name,current_utilization,max_utilization,limit_value from v$resource_limit " +
                "where resource_name in ('processes','sessions','transactions')";
        long start = System.nanoTime();
        try (Statement st = statement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resources.write(clientTs, dbTs, sampleId, rs.getString(1), rs.getLong(2), rs.getLong(3), rs.getString(4));
            }
        } catch (Exception e) {
            failMonitorQuery("RESOURCE_LIMIT", start, e);
        }
    }

    private void writeMainSample(String clientTs, String dbTs, long actualIntervalMs, long cycleMs,
                                 CanaryResult canary, long monitorQueryMs, double intervalSeconds,
                                 Map<String, Long> sysstat, Map<String, Long> timeModel,
                                 Map<String, Long> osstat, List<ActiveSession> active) {

        Long dbCpuUs = deltaMap(timeModel, prevTimeModel, "DB CPU");
        Long dbTimeUs = deltaMap(timeModel, prevTimeModel, "DB time");
        Long bgCpuUs = deltaMap(timeModel, prevTimeModel, "background cpu time");

        Long busy = deltaMap(osstat, prevOsstat, "BUSY_TIME");
        Long idle = deltaMap(osstat, prevOsstat, "IDLE_TIME");
        Double hostCpuPct = null;
        if (busy != null && idle != null && busy + idle > 0) {
            hostCpuPct = busy * 100.0 / (busy + idle);
        }

        Long commits = deltaMap(sysstat, prevSysstat, "user commits");
        Long userRollbacks = deltaMap(sysstat, prevSysstat, "user rollbacks");
        Long txRollbacks = deltaMap(sysstat, prevSysstat, "transaction rollbacks");
        Long undoApplied = deltaMap(sysstat, prevSysstat, "rollback changes - undo records applied");
        Long logons = deltaMap(sysstat, prevSysstat, "logons cumulative");
        Long redoSize = deltaMap(sysstat, prevSysstat, "redo size");
        Long redoWrites = deltaMap(sysstat, prevSysstat, "redo writes");
        Long redoWastage = deltaMap(sysstat, prevSysstat, "redo wastage");
        Long redoRetries = deltaMap(sysstat, prevSysstat, "redo buffer allocation retries");
        Long logicalReads = deltaMap(sysstat, prevSysstat, "session logical reads");
        Long physicalReads = deltaMap(sysstat, prevSysstat, "physical reads");
        Long physicalWrites = deltaMap(sysstat, prevSysstat, "physical writes");
        Long physicalReadsDirect = deltaMap(sysstat, prevSysstat, "physical reads direct");
        Long physicalWritesDirect = deltaMap(sysstat, prevSysstat, "physical writes direct");
        Long tempDirectWrites = deltaMap(sysstat, prevSysstat, "physical writes direct temporary tablespace");
        Long executes = deltaMap(sysstat, prevSysstat, "execute count");
        Long parseTotal = deltaMap(sysstat, prevSysstat, "parse count (total)");
        Long parseHard = deltaMap(sysstat, prevSysstat, "parse count (hard)");
        Long dbBlockChanges = deltaMap(sysstat, prevSysstat, "DB block changes");

        long activeMaster = active.stream().filter(a -> "PIM_MASTER".equals(a.username)).count();
        long activeMain = active.stream().filter(a -> "PIM_MAIN".equals(a.username)).count();

        samples.write(clientTs, dbTs, sampleId, actualIntervalMs, cycleMs,
                canary.elapsedMs, canary.status, monitorQueryMs,
                microsToSeconds(dbCpuUs), microsToSeconds(dbTimeUs), microsToSeconds(bgCpuUs),
                n(hostCpuPct), n(osstat.get("LOAD")), n(osstat.get("NUM_CPUS")),
                n(commits), n(userRollbacks), n(txRollbacks), n(undoApplied),
                n(logons), rate(logons, intervalSeconds), n(sysstat.get("logons current")),
                bytesToMb(redoSize), bytesPerSecondToMb(redoSize, intervalSeconds),
                n(redoWrites), n(redoWastage), n(redoRetries), n(logicalReads),
                n(physicalReads), n(physicalWrites), n(physicalReadsDirect), n(physicalWritesDirect),
                n(tempDirectWrites), n(executes), n(parseTotal), n(parseHard), n(dbBlockChanges),
                active.size(), activeMaster, activeMain);

        prevSysstat.clear();
        prevSysstat.putAll(sysstat);
        prevTimeModel.clear();
        prevTimeModel.putAll(timeModel);
        prevOsstat.clear();
        prevOsstat.putAll(osstat);
    }

    private Statement statement() throws SQLException {
        Connection c = getMonitorConnection();
        Statement st = c.createStatement();
        st.setQueryTimeout(queryTimeoutSeconds);
        st.setFetchSize(500);
        return st;
    }

    private synchronized Connection getMonitorConnection() throws SQLException {
        if (monitorConnection == null || monitorConnection.isClosed()) {
            monitorConnection = newConnection("P360_DIAG_MONITOR");
        }
        return monitorConnection;
    }

    private synchronized Connection getCanaryConnection() throws SQLException {
        if (canaryConnection == null || canaryConnection.isClosed()) {
            canaryConnection = newConnection("P360_DIAG_CANARY");
        }
        return canaryConnection;
    }

    private Connection newConnection(String moduleName) throws SQLException {
        // Mismo mecanismo que SqlRunner: DriverManager + URL/user/password de env vars.
        Connection c = DriverManager.getConnection(url, user, password);
        c.setAutoCommit(true);
        try {
            c.setNetworkTimeout(networkTimeoutExecutor, Math.max(2000, queryTimeoutSeconds * 1000 + 1000));
        } catch (Exception ignored) {
            // Algunos drivers/entornos no soportan setNetworkTimeout; Statement.setQueryTimeout sigue activo.
        }
        try (Statement st = c.createStatement()) {
            st.setQueryTimeout(queryTimeoutSeconds);
            st.execute("begin dbms_application_info.set_module('" + moduleName + "',null); end;");
        } catch (Exception ignored) {
            // Sólo sirve para distinguir nuestras conexiones en GV$SESSION; no es requisito para monitorear.
        }
        return c;
    }

    private void failMonitorQuery(String area, long startNano, Exception e) {
        logError(area, elapsedMs(startNano), e);
        resetMonitorConnection();
    }

    private synchronized void resetMonitorConnection() {
        closeQuietly(monitorConnection);
        monitorConnection = null;
    }

    private synchronized void resetCanaryConnection() {
        closeQuietly(canaryConnection);
        canaryConnection = null;
    }

    private void startConsoleThread() {
        Thread t = new Thread(() -> {
            try {
                java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(System.in, StandardCharsets.UTF_8));
                String line;
                while (running.get() && (line = br.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;
                    String upper = trimmed.toUpperCase(Locale.ROOT);
                    if (upper.equals("STOP") || upper.equals("QUIT") || upper.equals("EXIT")) {
                        mark("STOP_REQUESTED");
                        running.set(false);
                        break;
                    } else if (upper.equals("STATUS")) {
                        System.out.println(lastStatusLine.get());
                    } else if (upper.startsWith("MARK ")) {
                        mark(trimmed.substring(5).trim());
                    } else {
                        System.out.println("Comando desconocido. Usa: MARK <texto> | STATUS | STOP");
                    }
                }
            } catch (IOException e) {
                logError("CONSOLE", 0, e);
            }
        }, "incident-monitor-console");
        t.setDaemon(true);
        t.start();
    }

    private synchronized void mark(String text) {
        markers.write(now(), lastDbTimestamp.get(), sampleId, text);
        markers.flushQuietly();
        System.out.println("MARK sample=" + sampleId + " -> " + text);
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running.set(false);
            try {
                flushAll();
            } catch (Exception ignored) {
            }
        }, "incident-monitor-shutdown"));
    }

    private void logError(String area, long elapsedMs, Exception e) {
        String msg = e.getClass().getSimpleName() + ": " + nullToEmpty(e.getMessage());
        errors.write(now(), lastDbTimestamp.get(), sampleId, area, elapsedMs, msg);
        errors.flushQuietly();
        System.err.println("[" + area + "] " + msg);
    }

    private void flushAll() {
        samples.flushQuietly();
        events.flushQuietly();
        ioFunctions.flushQuietly();
        latches.flushQuietly();
        bufferWaits.flushQuietly();
        activeSessions.flushQuietly();
        topSql.flushQuietly();
        poolSessions.flushQuietly();
        blockers.flushQuietly();
        criticalSessions.flushQuietly();
        resources.flushQuietly();
        markers.flushQuietly();
        errors.flushQuietly();
    }

    @Override
    public void close() {
        running.set(false);
        flushAll();
        closeQuietly(monitorConnection);
        closeQuietly(canaryConnection);
        networkTimeoutExecutor.shutdownNow();

        samples.closeQuietly();
        events.closeQuietly();
        ioFunctions.closeQuietly();
        latches.closeQuietly();
        bufferWaits.closeQuietly();
        activeSessions.closeQuietly();
        topSql.closeQuietly();
        poolSessions.closeQuietly();
        blockers.closeQuietly();
        criticalSessions.closeQuietly();
        resources.closeQuietly();
        markers.closeQuietly();
        errors.closeQuietly();
    }

    private static String mustEnv(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Falta variable de entorno: " + name);
        }
        return v;
    }

    private static String sqlStringList(String[] values) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) b.append(',');
            b.append('\'').append(values[i].replace("'", "''")).append('\'');
        }
        return b.toString();
    }

    private static Long delta(long current, Long previous) {
        if (previous == null) return null;
        long d = current - previous;
        return d < 0 ? null : d;
    }

    private static Long deltaMap(Map<String, Long> current, Map<String, Long> previous, String key) {
        Long c = current.get(key);
        if (c == null) return null;
        return delta(c, previous.get(key));
    }

    private static Object rate(Long delta, double seconds) {
        return delta == null || seconds <= 0 ? "" : delta / seconds;
    }

    private static Object microsToSeconds(Long micros) {
        return micros == null ? "" : micros / 1_000_000.0;
    }

    private static Object microsDeltaSeconds(long current, Long previous) {
        Long d = delta(current, previous);
        return d == null ? "" : d / 1_000_000.0;
    }

    private static Object bytesToMb(Long bytes) {
        return bytes == null ? "" : bytes / 1048576.0;
    }

    private static Object bytesPerSecondToMb(Long bytes, double seconds) {
        return bytes == null || seconds <= 0 ? "" : bytes / 1048576.0 / seconds;
    }

    private static Object n(Object value) {
        return value == null ? "" : value;
    }

    private static String now() {
        return LocalDateTime.now().format(TS);
    }

    private static long elapsedMs(long startNano) {
        return (System.nanoTime() - startNano) / 1_000_000L;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static Long getNullableLong(ResultSet rs, int idx) throws SQLException {
        long v = rs.getLong(idx);
        return rs.wasNull() ? null : v;
    }

    private static Double getNullableDouble(ResultSet rs, int idx) throws SQLException {
        double v = rs.getDouble(idx);
        return rs.wasNull() ? null : v;
    }

    private static void closeQuietly(AutoCloseable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }

    private static final class CanaryResult {
        final long elapsedMs;
        final String status;
        final String dbTimestamp;

        CanaryResult(long elapsedMs, String status, String dbTimestamp) {
            this.elapsedMs = elapsedMs;
            this.status = status;
            this.dbTimestamp = dbTimestamp;
        }
    }

    private static final class EventSample {
        final Double logFileSyncWaitMsDelta;

        EventSample(Double logFileSyncWaitMsDelta) {
            this.logFileSyncWaitMsDelta = logFileSyncWaitMsDelta;
        }
    }

    private static final class EventCounter {
        final long waits;
        final long timeWaitedMicro;

        EventCounter(long waits, long timeWaitedMicro) {
            this.waits = waits;
            this.timeWaitedMicro = timeWaitedMicro;
        }
    }

    private static final class IoCounter {
        final long readMb;
        final long writeMb;
        final long readReqs;
        final long writeReqs;
        final long waits;
        final long waitTimeMs;

        IoCounter(long readMb, long writeMb, long readReqs, long writeReqs, long waits, long waitTimeMs) {
            this.readMb = readMb;
            this.writeMb = writeMb;
            this.readReqs = readReqs;
            this.writeReqs = writeReqs;
            this.waits = waits;
            this.waitTimeMs = waitTimeMs;
        }
    }

    private static final class LatchCounter {
        final long gets;
        final long misses;
        final long sleeps;
        final long spinGets;

        LatchCounter(long gets, long misses, long sleeps, long spinGets) {
            this.gets = gets;
            this.misses = misses;
            this.sleeps = sleeps;
            this.spinGets = spinGets;
        }
    }

    private static final class WaitStatCounter {
        final long count;
        final long timeCs;

        WaitStatCounter(long count, long timeCs) {
            this.count = count;
            this.timeCs = timeCs;
        }
    }

    private static final class SessionCounter {
        final long cpuCs;
        final long logicalReads;
        final long physicalReads;
        final long physicalWrites;
        final long redoSize;

        SessionCounter(long cpuCs, long logicalReads, long physicalReads, long physicalWrites, long redoSize) {
            this.cpuCs = cpuCs;
            this.logicalReads = logicalReads;
            this.physicalReads = physicalReads;
            this.physicalWrites = physicalWrites;
            this.redoSize = redoSize;
        }
    }

    private static final class SqlCounter {
        final long executions;
        final long cpuMicros;
        final long elapsedMicros;
        final long bufferGets;
        final long diskReads;
        final long directWrites;
        final long rowsProcessed;
        final long userIoMicros;
        final long concurrencyMicros;
        final long applicationMicros;
        final String sqlText;

        SqlCounter(long executions, long cpuMicros, long elapsedMicros, long bufferGets,
                   long diskReads, long directWrites, long rowsProcessed, long userIoMicros,
                   long concurrencyMicros, long applicationMicros, String sqlText) {
            this.executions = executions;
            this.cpuMicros = cpuMicros;
            this.elapsedMicros = elapsedMicros;
            this.bufferGets = bufferGets;
            this.diskReads = diskReads;
            this.directWrites = directWrites;
            this.rowsProcessed = rowsProcessed;
            this.userIoMicros = userIoMicros;
            this.concurrencyMicros = concurrencyMicros;
            this.applicationMicros = applicationMicros;
            this.sqlText = sqlText;
        }
    }

    private static final class ActiveSession {
        int instId;
        int sid;
        long serial;
        String username;
        String status;
        String sqlId;
        Long sqlExecId;
        String sqlExecStart;
        Double execSeconds;
        String event;
        String waitClass;
        String state;
        Long secondsInWait;
        Long blockingInstance;
        Long blockingSession;
        String machine;
        String module;
        String action;
        String program;
        long waiters;
        long undoBlocks;
        long undoRecords;
        long tempBlocks;
        long cpuCs;
        long logicalReads;
        long physicalReads;
        long physicalWrites;
        long redoSize;
        String sqlText;

        String sessionKey() {
            return instId + ":" + sid + ":" + serial;
        }
    }

    private static final class CsvWriter {
        private final BufferedWriter writer;

        CsvWriter(Path path, String... header) throws IOException {
            writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            write((Object[]) header);
            writer.flush();
        }

        synchronized void write(Object... values) {
            try {
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) writer.write(',');
                    writer.write(csv(values[i]));
                }
                writer.newLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        synchronized void flushQuietly() {
            try {
                writer.flush();
            } catch (IOException ignored) {
            }
        }

        synchronized void closeQuietly() {
            try {
                writer.flush();
                writer.close();
            } catch (IOException ignored) {
            }
        }

        private static String csv(Object value) {
            String s = value == null ? "" : String.valueOf(value);
            boolean quote = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
            if (!quote) return s;
            return '"' + s.replace("\"", "\"\"") + '"';
        }
    }
}