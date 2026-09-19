import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * TEMPORARY PRODUCTION CONTAINMENT FOR PRODUCT 360.
 *
 * V11 keeps the V10 protections but separates static size detection from actual-pressure enforcement.
 * The goal is flexibility: a large statement may live while Oracle is healthy,
 * but a large writer is still terminated when it is actually participating in
 * sustained REDO back-pressure.
 *
 * REPORTSTORE WRITER CIRCUIT:
 *   - HEAVY_ONLY/ALL_WRITERS still classify static ReportStore candidates.
 *   - REPORTSTORE_KILL_STATIC_ACTION decides OFF/WARN/KILL for those static
 *     candidates; WARN is useful when the infrastructure has more headroom.
 *   - Explicit SQL_ID blacklist remains immediate regardless of static action.
 *   - Runtime REDO rate and sustained "log buffer space" remain hard safety
 *     signals for any ReportStoreTemp writer, including INSERT ... VALUES.
 *
 * GIANT-WRITER CIRCUIT:
 *   - Applies to INSERT/UPDATE/DELETE/MERGE on ANY table for watched users.
 *   - Normal giant detection is independent from emergency enforcement.
 *   - REPORTSTORE_KILL_GIANT_WRITER_MODE controls OFF/WARN/KILL for a writer
 *     that is merely large; default WARN.
 *   - REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_ACTION independently controls
 *     OFF/WARN/KILL when that large writer itself is sustained on
 *     "log buffer space"; default KILL.
 *   - Emergency kills are globally throttled so the watcher kills one giant
 *     writer, then gives Oracle time to drain and re-evaluate.
 *   - Does not kill SELECT victims just because they are waiting on REDO.
 *
 * ACV RUNAWAY-READER CIRCUIT:
 *   - Applies to SELECTs touching ArticleCharactValue AND
 *     ArticleCharactValueLang, even when they do NOT touch ReportStoreTemp.
 *   - Does NOT kill on physical-reads/sec by itself.
 *   - Requires sustained execution time + large absolute work + consecutive
 *     observations before WARN/KILL.
 *   - Default mode is WARN so thresholds can be tuned from production evidence.
 *
 * The V8 generic ReportStore SELECT read-rate kill was intentionally removed.
 * A fast LookupValue SELECT doing a short burst of physical reads must not be
 * treated as equivalent to a runaway ACV reader or a REDO-producing writer.
 *
 * Required env:
 *   ORACLE_JDBC_URL
 *   ORACLE_JDBC_USER
 *   ORACLE_JDBC_PASSWORD
 *
 * Optional env:
 *   REPORTSTORE_KILL_ENABLED=true|false          default false
 *   REPORTSTORE_KILL_MODE=HEAVY_ONLY|ALL_WRITERS default HEAVY_ONLY
 *   REPORTSTORE_KILL_INTERVAL_MS                 default 1000
 *   REPORTSTORE_KILL_QUERY_TIMEOUT_SEC           default 2
 *   REPORTSTORE_KILL_SESSION_QUERY_TIMEOUT_SEC   default 4
 *   REPORTSTORE_KILL_MIN_ESTIMATED_ROWS          default 250000
 *   REPORTSTORE_KILL_UNKNOWN_PLAN=KILL|ALLOW     default KILL
 *   REPORTSTORE_KILL_STATIC_ACTION=OFF|WARN|KILL default KILL
 *                                                 controls only static ReportStore
 *                                                 HEAVY_ONLY/ALL_WRITERS matches
 *   REPORTSTORE_KILL_SQL_IDS                     comma-separated immediate blacklist;
 *                                                 default bkztn38jw26sg
 *   REPORTSTORE_KILL_USERS                       comma-separated watched DB users;
 *                                                 default PIM_MAIN,PIM_MASTER,P360_EXPLOIT
 *
 *   REPORTSTORE_KILL_GIANT_WRITER_MODE=OFF|WARN|KILL
 *                                                 default WARN
 *   REPORTSTORE_KILL_GIANT_WRITER_MIN_ELAPSED_SEC default 120
 *   REPORTSTORE_KILL_GIANT_WRITER_MIN_DISK_READS_PER_EXEC
 *                                                 default 500000
 *   REPORTSTORE_KILL_GIANT_WRITER_MIN_BUFFER_GETS_PER_EXEC
 *                                                 default 10000000
 *   REPORTSTORE_KILL_GIANT_WRITER_MIN_REDO_MIB_PER_SEC
 *                                                 default 32
 *   REPORTSTORE_KILL_GIANT_WRITER_MIN_POLLS       default 2
 *   REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_ACTION=OFF|WARN|KILL
 *                                                 default KILL
 *   REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_COOLDOWN_SEC
 *                                                 default 10
 *   REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_LOG_BUFFER_SPACE_SEC
 *                                                 default 5
 *   REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_MIN_ELAPSED_SEC
 *                                                 default 30
 *   REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_MIN_DISK_READS_PER_EXEC
 *                                                 default 250000
 *   REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_MIN_BUFFER_GETS_PER_EXEC
 *                                                 default 5000000
 *
 *   REPORTSTORE_KILL_ACV_READER_MODE=OFF|WARN|KILL
 *                                                 default WARN
 *   REPORTSTORE_KILL_ACV_READER_MIN_ELAPSED_SEC  default 60
 *   REPORTSTORE_KILL_ACV_READER_MIN_DISK_READS_PER_EXEC
 *                                                 default 250000
 *   REPORTSTORE_KILL_ACV_READER_MIN_BUFFER_GETS_PER_EXEC
 *                                                 default 5000000
 *   REPORTSTORE_KILL_ACV_READER_MIN_POLLS        default 3
 *   REPORTSTORE_KILL_ACV_READER_MIN_USER_IO_WAIT_SEC_PER_EXEC
 *                                                 default 0 (disabled)
 *   REPORTSTORE_KILL_ACV_READER_REQUIRE_REPORTSTORE
 *                                                 default false
 *
 *   REPORTSTORE_KILL_INSERT_REDO_MIB_PER_SEC     default 32
 *   REPORTSTORE_KILL_LOG_BUFFER_SPACE_MIN_SEC    default 5
 *   REPORTSTORE_KILL_IMMEDIATE_INSERT_TARGETS    default ReportStoreTempB6,ReportStoreTempB7
 *   REPORTSTORE_KILL_ACTION=DISCONNECT|KILL      default DISCONNECT
 *   REPORTSTORE_KILL_ACTION_TIMEOUT_SEC          default 2
 *   REPORTSTORE_KILL_WORKERS                     default 2
 *   REPORTSTORE_KILL_COOLDOWN_SEC                default 60
 *
 * Notes:
 * - SID+SERIAL# remains the destructive-action identity guard.
 * - SQL_ID text/classification is cached; ACV runtime statistics are not.
 * - ACV WARN/KILL decisions use V$SESSION execution age and V$SQL work per
 *   execution. Plan hash is logged for correlation but is not hard-coded as
 *   guilty.
 */
public final class OracleWatcherV11 {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final Pattern DML_WRITER =
            Pattern.compile("^\\s*(INSERT|UPDATE|DELETE|MERGE)\\b",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern WRITER =
            Pattern.compile("^\\s*(INSERT|UPDATE|DELETE|MERGE|TRUNCATE)\\b",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern INSERT =
            Pattern.compile("^\\s*INSERT\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern SELECT =
            Pattern.compile("^\\s*SELECT\\b", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern VALUES =
            Pattern.compile("\\bVALUES\\s*\\(", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern INSERT_TARGET =
            Pattern.compile(
                    "^\\s*INSERT\\s*(?:/\\*.*?\\*/\\s*)?INTO\\s+"
                  + "(?:\\\"?[A-Z0-9_$#]+\\\"?\\.)?\\\"?([A-Z0-9_$#]+)\\\"?",
                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final String URL = requireEnv("ORACLE_JDBC_URL");
    private static final String USER = requireEnv("ORACLE_JDBC_USER");
    private static final String PASSWORD = requireEnv("ORACLE_JDBC_PASSWORD");

    private static final boolean ENABLED =
            Boolean.parseBoolean(System.getenv().getOrDefault(
                    "REPORTSTORE_KILL_ENABLED", "false"));

    private static final String MODE =
            System.getenv().getOrDefault("REPORTSTORE_KILL_MODE", "HEAVY_ONLY")
                    .trim().toUpperCase(Locale.ROOT);

    private static final String STATIC_ACTION =
            System.getenv().getOrDefault("REPORTSTORE_KILL_STATIC_ACTION", "KILL")
                    .trim().toUpperCase(Locale.ROOT);

    private static final String ACTION =
            System.getenv().getOrDefault("REPORTSTORE_KILL_ACTION", "DISCONNECT")
                    .trim().toUpperCase(Locale.ROOT);

    private static final long INTERVAL_MS =
            envLong("REPORTSTORE_KILL_INTERVAL_MS", 1_000L);

    private static final int QUERY_TIMEOUT_SEC =
            envInt("REPORTSTORE_KILL_QUERY_TIMEOUT_SEC", 2);

    private static final int SESSION_QUERY_TIMEOUT_SEC =
            envInt("REPORTSTORE_KILL_SESSION_QUERY_TIMEOUT_SEC", 4);

    private static final long MIN_ESTIMATED_ROWS =
            envLong("REPORTSTORE_KILL_MIN_ESTIMATED_ROWS", 250_000L);

    private static final String UNKNOWN_PLAN =
            System.getenv().getOrDefault("REPORTSTORE_KILL_UNKNOWN_PLAN", "KILL")
                    .trim().toUpperCase(Locale.ROOT);

    // Known pathological SQL IDs are shot on first sight, regardless of elapsed time
    // or current cursor statistics. Add more with a comma-separated env value.
    private static final Set<String> BLACKLIST_SQL_IDS =
            parseCsvSet(System.getenv().getOrDefault(
                    "REPORTSTORE_KILL_SQL_IDS", "bkztn38jw26sg"));

    private static final Set<String> WATCHED_USERS =
            parseCsvSetUpper(System.getenv().getOrDefault(
                    "REPORTSTORE_KILL_USERS",
                    "PIM_MAIN,PIM_MASTER,P360_EXPLOIT"));

    /*
     * V11: global giant-writer circuit.
     *
     * Normal path: a DML execution must be old enough and exceed an absolute
     * work/REDO threshold for consecutive polls.
     *
     * Emergency path: if the writer itself has been in "log buffer space" for
     * several seconds, lower absolute-work thresholds apply immediately. This
     * distinguishes a giant producer from the many tiny DML victims observed
     * during the 2026-08-24 incident.
     */
    private static final String GIANT_WRITER_MODE =
            System.getenv().getOrDefault(
                    "REPORTSTORE_KILL_GIANT_WRITER_MODE", "WARN")
                    .trim().toUpperCase(Locale.ROOT);

    private static final long GIANT_WRITER_MIN_ELAPSED_SEC =
            envLong("REPORTSTORE_KILL_GIANT_WRITER_MIN_ELAPSED_SEC", 120L);

    private static final long GIANT_WRITER_MIN_DISK_READS_PER_EXEC =
            envLong("REPORTSTORE_KILL_GIANT_WRITER_MIN_DISK_READS_PER_EXEC", 500_000L);

    private static final long GIANT_WRITER_MIN_BUFFER_GETS_PER_EXEC =
            envLong("REPORTSTORE_KILL_GIANT_WRITER_MIN_BUFFER_GETS_PER_EXEC", 10_000_000L);

    private static final double GIANT_WRITER_MIN_REDO_MIB_PER_SEC =
            envDouble("REPORTSTORE_KILL_GIANT_WRITER_MIN_REDO_MIB_PER_SEC", 32.0d);

    private static final int GIANT_WRITER_MIN_POLLS =
            envInt("REPORTSTORE_KILL_GIANT_WRITER_MIN_POLLS", 2);

    private static final String GIANT_WRITER_EMERGENCY_ACTION =
            System.getenv().getOrDefault(
                    "REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_ACTION", "KILL")
                    .trim().toUpperCase(Locale.ROOT);

    private static final long GIANT_WRITER_EMERGENCY_COOLDOWN_MS =
            envLong("REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_COOLDOWN_SEC", 10L)
                    * 1000L;

    private static final long GIANT_WRITER_EMERGENCY_LOG_BUFFER_SPACE_SEC =
            envLong("REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_LOG_BUFFER_SPACE_SEC", 5L);

    private static final long GIANT_WRITER_EMERGENCY_MIN_ELAPSED_SEC =
            envLong("REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_MIN_ELAPSED_SEC", 30L);

    private static final long GIANT_WRITER_EMERGENCY_MIN_DISK_READS_PER_EXEC =
            envLong("REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_MIN_DISK_READS_PER_EXEC", 250_000L);

    private static final long GIANT_WRITER_EMERGENCY_MIN_BUFFER_GETS_PER_EXEC =
            envLong("REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_MIN_BUFFER_GETS_PER_EXEC", 5_000_000L);

    /*
     * V11: ACV runaway-reader circuit inherited from V9.
     *
     * The 2026-08-24 evidence separated two populations:
     *   - short Lookup/ReportStore reads: thousands of reads, high instantaneous
     *     read rate, but not runaway;
     *   - pathological ACV readers: ~1.5-2.0M physical reads, ~10-13M logical
     *     reads and ~19 minutes elapsed for only a handful of rows.
     *
     * Therefore reads/sec is telemetry, not an autonomous death sentence.
     */
    private static final String ACV_READER_MODE =
            System.getenv().getOrDefault(
                    "REPORTSTORE_KILL_ACV_READER_MODE", "WARN")
                    .trim().toUpperCase(Locale.ROOT);

    private static final long ACV_READER_MIN_ELAPSED_SEC =
            envLong("REPORTSTORE_KILL_ACV_READER_MIN_ELAPSED_SEC", 60L);

    private static final long ACV_READER_MIN_DISK_READS_PER_EXEC =
            envLong("REPORTSTORE_KILL_ACV_READER_MIN_DISK_READS_PER_EXEC", 250_000L);

    private static final long ACV_READER_MIN_BUFFER_GETS_PER_EXEC =
            envLong("REPORTSTORE_KILL_ACV_READER_MIN_BUFFER_GETS_PER_EXEC", 5_000_000L);

    private static final int ACV_READER_MIN_POLLS =
            envInt("REPORTSTORE_KILL_ACV_READER_MIN_POLLS", 3);

    private static final double ACV_READER_MIN_USER_IO_WAIT_SEC_PER_EXEC =
            envDouble("REPORTSTORE_KILL_ACV_READER_MIN_USER_IO_WAIT_SEC_PER_EXEC", 0.0d);

    private static final boolean ACV_READER_REQUIRE_REPORTSTORE =
            Boolean.parseBoolean(System.getenv().getOrDefault(
                    "REPORTSTORE_KILL_ACV_READER_REQUIRE_REPORTSTORE", "false"));

    /*
     * Runtime safety net for set-based ReportStoreTemp INSERTs.
     * The old plan estimate can be wrong; this observes what the session is
     * actually generating right now. 32 MiB/s is intentionally far below the
     * ~122 MiB/s pathological burst already observed, but high enough not to
     * kill merely because an INSERT is set-based.
     */
    private static final double INSERT_REDO_MIB_PER_SEC =
            envDouble("REPORTSTORE_KILL_INSERT_REDO_MIB_PER_SEC", 32.0d);

    /*
     * Emergency signal: a ReportStoreTemp writer observed waiting on
     * "log buffer space" is already participating in REDO back-pressure.
     * Requiring >= 5 seconds avoids firing on a short victim wait while still
 * catching the sustained stalls observed in the incident.
     */
    private static final long LOG_BUFFER_SPACE_MIN_SEC =
            envLong("REPORTSTORE_KILL_LOG_BUFFER_SPACE_MIN_SEC", 5L);

    private static final Set<String> IMMEDIATE_INSERT_TARGETS =
            parseCsvSetUpper(System.getenv().getOrDefault(
                    "REPORTSTORE_KILL_IMMEDIATE_INSERT_TARGETS",
                    "ReportStoreTempB6,ReportStoreTempB7"));

    private static final int ACTION_TIMEOUT_SEC =
            envInt("REPORTSTORE_KILL_ACTION_TIMEOUT_SEC", 2);

    private static final int KILLER_WORKERS =
            envInt("REPORTSTORE_KILL_WORKERS", 2);

    private static final long COOLDOWN_MS =
            envLong("REPORTSTORE_KILL_COOLDOWN_SEC", 60L) * 1000L;

    private static volatile boolean running = true;

    private static Connection monitorConnection;

    private static final BlockingQueue<Target> killQueue =
            new LinkedBlockingQueue<>(10_000);

    private static final List<Thread> killerThreads = new ArrayList<>();

    // Only the monitor thread touches these maps.
    private static final Map<String, SqlInfo> SQL_CACHE = new HashMap<>();
    private static final Map<String, Integer> ACV_CANDIDATE_POLLS = new HashMap<>();
    private static final Map<String, Long> ACV_WARNING_TIMES = new HashMap<>();
    private static final Map<String, Integer> GIANT_WRITER_CANDIDATE_POLLS = new HashMap<>();
    private static final Map<String, Long> GIANT_WRITER_NORMAL_WARNING_TIMES = new HashMap<>();
    private static final Map<String, Long> GIANT_WRITER_EMERGENCY_WARNING_TIMES = new HashMap<>();
    private static final Map<String, Long> STATIC_WARNING_TIMES = new HashMap<>();

    private static long lastGiantWriterEmergencyKillAt = 0L;

    private static final Map<String, SessionRedoSnap> SESSION_REDO_SNAP = new HashMap<>();

    // Prevents re-queueing the exact same SID+SERIAL while Oracle is cleaning it.
    private static final ConcurrentHashMap<String, Long> WARRANTS =
            new ConcurrentHashMap<>();

    private static final String ACTIVE_SESSIONS =
        "select sid,serial#,username,machine,sql_id,sql_child_number,event,seconds_in_wait, " +
        "       nvl(sql_exec_id,0) sql_exec_id, " +
        "       nvl(to_char(sql_exec_start,'YYYYMMDDHH24MISS'),'NA') sql_exec_start_key, " +
        "       nvl(round((sysdate-sql_exec_start)*86400),0) sql_exec_seconds " +
        "from v$session " +
        "where type='USER' " +
        "  and status='ACTIVE' " +
        "  and username in (" + sqlStringList(WATCHED_USERS) + ") " +
        "  and sql_id is not null";

    // Exact SQL hash lookup; never scans V$SQL with LIKE/REGEXP.
    // SQL_FULLTEXT is needed because ReportStoreTemp can appear well past the first
    // 1000 characters in the Hibernate SELECTs.
    private static final String SQL_TEXT =
        "select sql_fulltext from v$sql " +
        "where sql_id=? and child_number=? and rownum=1";

    private static final String SQL_STATS =
        "select executions,buffer_gets,disk_reads,rows_processed,elapsed_time,cpu_time, " +
        "       user_io_wait_time,plan_hash_value " +
        "from v$sql where sql_id=? and child_number=? and rownum=1";

    private static final String PLAN_ESTIMATE =
        "select cardinality from (" +
        "  select p.id,p.cardinality " +
        "  from v$sql_plan p " +
        "  where p.sql_id=? and p.child_number=? " +
        "    and p.cardinality is not null " +
        "    and p.id > nvl((" +
        "      select min(l.id) from v$sql_plan l " +
        "      where l.sql_id=p.sql_id and l.child_number=p.child_number " +
        "        and l.operation='LOAD TABLE CONVENTIONAL'" +
        "    ),0) " +
        "  order by p.id" +
        ") where rownum=1";

    /*
     * One compact runtime query per poll. This is the key V7 addition:
     * it measures per-session redo growth while the SQL is actually active.
     */
    private static final String ACTIVE_SESSION_REDO =
        "select s.sid,s.serial#,s.sql_id,ss.value redo_bytes " +
        "from v$session s " +
        "join v$sesstat ss on ss.sid=s.sid " +
        "join v$statname sn on sn.statistic#=ss.statistic# " +
        "where s.type='USER' " +
        "  and s.status='ACTIVE' " +
        "  and s.username in (" + sqlStringList(WATCHED_USERS) + ") " +
        "  and s.sql_id is not null " +
        "  and sn.name='redo size'";

    private record SessionSnap(
            int sid,
            int serial,
            String username,
            String machine,
            String sqlId,
            int child,
            String event,
            long secondsInWait,
            long sqlExecId,
            String sqlExecStartKey,
            long sqlExecSeconds) {

        String identityKey() {
            return sid + ":" + serial;
        }

        String executionKey() {
            return sid + ":" + serial + ":" + sqlId + ":" + sqlExecId + ":" + sqlExecStartKey;
        }
    }

    private record SqlInfo(
            String text,
            boolean reportStoreRelated,
            boolean select,
            boolean acvFamily,
            boolean heavy,
            Long estimatedRows,
            String reason) {}

    private record SqlStats(
            long executions,
            long bufferGets,
            long diskReads,
            long rowsProcessed,
            long elapsedMicros,
            long cpuMicros,
            long userIoWaitMicros,
            long planHashValue) {}

    private record WriterDecision(
            boolean normalQualified,
            boolean emergencyQualified,
            int consecutivePolls,
            String normalReason,
            String emergencyReason) {}

    private record AcvDecision(
            boolean qualified,
            int consecutivePolls,
            String reason) {}

    private record SessionRedoSnap(
            long redoBytes,
            long capturedAtMillis) {}

    private record Target(
            int sid,
            int serial,
            String username,
            String machine,
            String sqlId,
            String event,
            String sqlText,
            long sqlExecId,
            String sqlExecStartKey,
            long detectedAtMillis) {

        String identityKey() {
            return sid + ":" + serial;
        }
    }

    public static void main(String[] args) {

        validateConfig();

        log("START intervalMs=" + INTERVAL_MS
                + " queryTimeoutSec=" + QUERY_TIMEOUT_SEC
                + " sessionQueryTimeoutSec=" + SESSION_QUERY_TIMEOUT_SEC
                + " minEstimatedRows=" + MIN_ESTIMATED_ROWS
                + " unknownPlan=" + UNKNOWN_PLAN
                + " watchedUsers=" + WATCHED_USERS
                + " staticAction=" + STATIC_ACTION
                + " giantWriterMode=" + GIANT_WRITER_MODE
                + " giantWriterMinElapsedSec=" + GIANT_WRITER_MIN_ELAPSED_SEC
                + " giantWriterMinDiskReadsPerExec=" + GIANT_WRITER_MIN_DISK_READS_PER_EXEC
                + " giantWriterMinBufferGetsPerExec=" + GIANT_WRITER_MIN_BUFFER_GETS_PER_EXEC
                + " giantWriterMinRedoMiBPerSec=" + GIANT_WRITER_MIN_REDO_MIB_PER_SEC
                + " giantWriterMinPolls=" + GIANT_WRITER_MIN_POLLS
                + " giantWriterEmergencyAction=" + GIANT_WRITER_EMERGENCY_ACTION
                + " giantWriterEmergencyCooldownSec=" + (GIANT_WRITER_EMERGENCY_COOLDOWN_MS / 1000L)
                + " giantWriterEmergencyLogBufferSpaceSec=" + GIANT_WRITER_EMERGENCY_LOG_BUFFER_SPACE_SEC
                + " giantWriterEmergencyMinElapsedSec=" + GIANT_WRITER_EMERGENCY_MIN_ELAPSED_SEC
                + " giantWriterEmergencyMinDiskReadsPerExec=" + GIANT_WRITER_EMERGENCY_MIN_DISK_READS_PER_EXEC
                + " giantWriterEmergencyMinBufferGetsPerExec=" + GIANT_WRITER_EMERGENCY_MIN_BUFFER_GETS_PER_EXEC
                + " acvReaderMode=" + ACV_READER_MODE
                + " acvReaderMinElapsedSec=" + ACV_READER_MIN_ELAPSED_SEC
                + " acvReaderMinDiskReadsPerExec=" + ACV_READER_MIN_DISK_READS_PER_EXEC
                + " acvReaderMinBufferGetsPerExec=" + ACV_READER_MIN_BUFFER_GETS_PER_EXEC
                + " acvReaderMinPolls=" + ACV_READER_MIN_POLLS
                + " acvReaderMinUserIoWaitSecPerExec=" + ACV_READER_MIN_USER_IO_WAIT_SEC_PER_EXEC
                + " acvReaderRequireReportStore=" + ACV_READER_REQUIRE_REPORTSTORE
                + " insertRedoMiBPerSec=" + INSERT_REDO_MIB_PER_SEC
                + " logBufferSpaceMinSec=" + LOG_BUFFER_SPACE_MIN_SEC
                + " immediateInsertTargets=" + IMMEDIATE_INSERT_TARGETS
                + " blacklistSqlIds=" + BLACKLIST_SQL_IDS
                + " actionTimeoutSec=" + ACTION_TIMEOUT_SEC
                + " workers=" + KILLER_WORKERS
                + " mode=" + MODE
                + " action=" + ACTION
                + " enabled=" + ENABLED);

        if (ENABLED) {
            log("ARMED: ReportStore protection enabled; giant-writer normal mode="
                    + GIANT_WRITER_MODE + "; ACV reader circuit mode=" + ACV_READER_MODE + ".");
            startKillerWorkers();
        } else {
            log("DRY RUN: set REPORTSTORE_KILL_ENABLED=true to arm.");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            running = false;
            closeQuietly(monitorConnection);
            for (Thread t : killerThreads) {
                t.interrupt();
            }
            log("STOP");
        }, "oracle-watcher-shutdown"));

        while (running) {
            long started = System.currentTimeMillis();

            try {
                pollOnce();
            } catch (SQLTimeoutException e) {
                log("POLL_TIMEOUT " + oneLine(e.getMessage()));
            } catch (SQLException e) {
                log("DB_ERROR ora=" + e.getErrorCode()
                        + " msg=" + oneLine(e.getMessage()));
                closeQuietly(monitorConnection);
                monitorConnection = null;
            } catch (Throwable t) {
                log("ERROR " + t.getClass().getSimpleName()
                        + ": " + oneLine(t.getMessage()));
            }

            purgeCaches();

            long elapsed = System.currentTimeMillis() - started;
            long sleep = Math.max(100L, INTERVAL_MS - elapsed);

            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static void pollOnce() throws SQLException {

        Connection c = getMonitorConnection();
        List<SessionSnap> sessions = readActiveSessions(c);

        if (sessions.isEmpty()) {
            log("OK no active PIM sessions");
            return;
        }

        // Put the strongest direct REDO back-pressure observations first. This
        // matters together with the emergency global cooldown: when several
        // writers qualify, the one that has waited longest on log buffer space
        // gets first opportunity to be removed, then Oracle is re-evaluated.
        sessions.sort(Comparator
                .comparing((SessionSnap x) -> !"log buffer space".equalsIgnoreCase(nullSafe(x.event())))
                .thenComparing(Comparator.comparingLong(SessionSnap::secondsInWait).reversed())
                .thenComparing(Comparator.comparingLong(SessionSnap::sqlExecSeconds).reversed()));

        int allowedMonitoredSql = 0;
        int matchingTargets = 0;
        Set<String> activeExecutionKeys = new HashSet<>();

        Map<String, Double> redoRatesMiB = readActiveSessionRedoRates(c);

        for (SessionSnap s : sessions) {

            activeExecutionKeys.add(s.executionKey());

            String sqlKey = s.sqlId() + ":" + s.child();
            SqlInfo info = SQL_CACHE.get(sqlKey);

            if (info == null) {
                String sql = fetchSqlText(c, s.sqlId(), s.child());
                info = classify(c, s.sqlId(), s.child(), sql);
                SQL_CACHE.put(sqlKey, info);
            }

            boolean dmlWriter = isDmlWriterSql(info.text());

            if (!info.reportStoreRelated() && !info.acvFamily() && !info.heavy() && !dmlWriter) {
                continue;
            }

            boolean killThis = false;
            boolean giantEmergencyKill = false;
            String runtimeReason = null;

            /*
             * Explicit blacklist remains a hard override. Static-action
             * flexibility must never silently turn a proven SQL_ID blacklist
             * into a warning.
             */
            if (BLACKLIST_SQL_IDS.contains(s.sqlId())) {
                killThis = true;
                runtimeReason = "BLACKLIST_SQL_ID";
            }

            /*
             * V11 static ReportStore classification. HEAVY_ONLY/ALL_WRITERS
             * decides WHAT is considered suspicious; STATIC_ACTION decides
             * whether that static fact alone kills, warns, or is ignored.
             * Runtime REDO/back-pressure protection below remains independent.
             */
            boolean staticReportStoreMatch =
                    ("ALL_WRITERS".equals(MODE) && isReportStoreWriter(info.text()))
                    || info.heavy();

            if (!killThis && staticReportStoreMatch) {
                if ("KILL".equals(STATIC_ACTION)) {
                    killThis = true;
                    runtimeReason = "STATIC_REPORTSTORE_" + info.reason();
                } else if ("WARN".equals(STATIC_ACTION)) {
                    logStaticWarning(s, info);
                }
            }

            /*
             * Runtime REDO protection applies to ANY ReportStoreTemp writer,
             * including INSERT ... VALUES. A writer that is actually producing
             * excessive REDO can still be terminated even when STATIC_ACTION=WARN.
             */
            if (!killThis && isReportStoreWriter(info.text())) {
                double redoMiB = redoRatesMiB.getOrDefault(
                        s.identityKey() + ":" + s.sqlId(), 0.0d);

                if (redoMiB >= INSERT_REDO_MIB_PER_SEC) {
                    killThis = true;
                    runtimeReason = "REPORTSTORE_WRITER_RUNTIME_REDO_MIB_S="
                            + String.format(Locale.ROOT, "%.1f", redoMiB);
                }
            }

            /*
             * Direct ReportStore back-pressure signal. This remains a hard
             * safety net independently from static classification.
             */
            if (!killThis
                    && isReportStoreWriter(info.text())
                    && "log buffer space".equalsIgnoreCase(nullSafe(s.event()))
                    && s.secondsInWait() >= LOG_BUFFER_SPACE_MIN_SEC) {

                killThis = true;
                runtimeReason = "REPORTSTORE_WRITER_LOG_BUFFER_SPACE_"
                        + s.secondsInWait() + "S";
            }

            /*
             * V11 giant-writer circuit. Normal giant detection and emergency
             * enforcement are deliberately independent. A huge writer can live
             * while Oracle is healthy (normal mode WARN), but the same writer
             * can still be killed when it is itself sustained on log buffer
             * space (emergency action KILL).
             */
            if (!killThis
                    && dmlWriter
                    && (!"OFF".equals(GIANT_WRITER_MODE)
                        || !"OFF".equals(GIANT_WRITER_EMERGENCY_ACTION))) {

                double redoMiB = redoRatesMiB.getOrDefault(
                        s.identityKey() + ":" + s.sqlId(), 0.0d);

                WriterDecision decision = evaluateGiantWriter(c, s, info, redoMiB);

                boolean emergencySuppressed = false;

                if (decision.emergencyQualified()) {
                    if ("KILL".equals(GIANT_WRITER_EMERGENCY_ACTION)) {
                        long now = System.currentTimeMillis();
                        long sinceLast = now - lastGiantWriterEmergencyKillAt;

                        if (lastGiantWriterEmergencyKillAt == 0L
                                || sinceLast >= GIANT_WRITER_EMERGENCY_COOLDOWN_MS) {
                            killThis = true;
                            giantEmergencyKill = true;
                            runtimeReason = decision.emergencyReason();
                        } else {
                            emergencySuppressed = true;
                            log("GIANT_WRITER_EMERGENCY_SUPPRESSED"
                                    + " sid=" + s.sid()
                                    + " serial=" + s.serial()
                                    + " sqlId=" + s.sqlId()
                                    + " cooldownRemainingMs="
                                    + (GIANT_WRITER_EMERGENCY_COOLDOWN_MS - sinceLast)
                                    + " reason=" + decision.emergencyReason());
                        }
                    } else if ("WARN".equals(GIANT_WRITER_EMERGENCY_ACTION)) {
                        logGiantWriterWarning(
                                s, info, true, decision.emergencyReason());
                    }
                }

                if (!killThis && !emergencySuppressed && decision.normalQualified()) {
                    if ("KILL".equals(GIANT_WRITER_MODE)) {
                        killThis = true;
                        runtimeReason = decision.normalReason();
                    } else if ("WARN".equals(GIANT_WRITER_MODE)) {
                        logGiantWriterWarning(
                                s, info, false, decision.normalReason());
                    }
                }
            }

            /*
             * ACV runaway-reader circuit stays independent from REDO. It is
             * intentionally WARN by default; short Lookup readers are not killed
             * simply for a high instantaneous physical-read rate.
             */
            if (!killThis
                    && info.select()
                    && info.acvFamily()
                    && !"OFF".equals(ACV_READER_MODE)
                    && (!ACV_READER_REQUIRE_REPORTSTORE || info.reportStoreRelated())) {

                AcvDecision decision = evaluateAcvReader(c, s, info);

                if (decision.qualified()) {
                    if ("KILL".equals(ACV_READER_MODE)) {
                        killThis = true;
                        runtimeReason = decision.reason();
                    } else if ("WARN".equals(ACV_READER_MODE)) {
                        logAcvWarning(s, info, decision);
                    }
                }
            }

            if (!killThis) {
                allowedMonitoredSql++;
                continue;
            }

            matchingTargets++;

            long now = System.currentTimeMillis();
            Long previous = WARRANTS.putIfAbsent(s.identityKey(), now);

            if (previous != null && now - previous < COOLDOWN_MS) {
                continue;
            }

            if (previous != null) {
                WARRANTS.put(s.identityKey(), now);
            }

            Target target = new Target(
                    s.sid(),
                    s.serial(),
                    s.username(),
                    s.machine(),
                    s.sqlId(),
                    s.event(),
                    info.text(),
                    s.sqlExecId(),
                    s.sqlExecStartKey(),
                    now
            );

            log((ENABLED ? "DEATH_WARRANT" : "WOULD_TERMINATE")
                    + " sid=" + s.sid()
                    + " serial=" + s.serial()
                    + " user=" + s.username()
                    + " machine=" + s.machine()
                    + " sqlId=" + s.sqlId()
                    + " event=" + nullSafe(s.event())
                    + " heavy=" + info.heavy()
                    + " estimatedRows=" + (info.estimatedRows() == null ? "UNKNOWN" : info.estimatedRows())
                    + " threshold=" + MIN_ESTIMATED_ROWS
                    + " reason=" + (runtimeReason == null ? info.reason() : runtimeReason)
                    + " sql=" + compact(info.text()));

            if (ENABLED) {
                if (!killQueue.offer(target)) {
                    log("KILL_QUEUE_FULL sid=" + s.sid()
                            + " serial=" + s.serial());
                } else if (giantEmergencyKill) {
                    lastGiantWriterEmergencyKillAt = now;
                }
            }
        }

        ACV_CANDIDATE_POLLS.keySet().removeIf(k -> !activeExecutionKeys.contains(k));
        ACV_WARNING_TIMES.keySet().removeIf(k -> !activeExecutionKeys.contains(k));
        GIANT_WRITER_CANDIDATE_POLLS.keySet().removeIf(k -> !activeExecutionKeys.contains(k));
        GIANT_WRITER_NORMAL_WARNING_TIMES.keySet().removeIf(k -> !activeExecutionKeys.contains(k));
        GIANT_WRITER_EMERGENCY_WARNING_TIMES.keySet().removeIf(k -> !activeExecutionKeys.contains(k));
        STATIC_WARNING_TIMES.keySet().removeIf(k -> !activeExecutionKeys.contains(k));

        if (matchingTargets == 0) {
            log("OK activePim=" + sessions.size()
                    + " cachedSql=" + SQL_CACHE.size()
                    + " allowedMonitoredSql=" + allowedMonitoredSql
                    + " no killable SQL");
        }
    }

    private static List<SessionSnap> readActiveSessions(Connection c)
            throws SQLException {

        List<SessionSnap> out = new ArrayList<>();

        try (PreparedStatement ps = c.prepareStatement(ACTIVE_SESSIONS)) {
            ps.setQueryTimeout(SESSION_QUERY_TIMEOUT_SEC);
            ps.setFetchSize(256);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new SessionSnap(
                            rs.getInt("sid"),
                            rs.getInt("serial#"),
                            rs.getString("username"),
                            rs.getString("machine"),
                            rs.getString("sql_id"),
                            rs.getInt("sql_child_number"),
                            rs.getString("event"),
                            rs.getLong("seconds_in_wait"),
                            rs.getLong("sql_exec_id"),
                            rs.getString("sql_exec_start_key"),
                            rs.getLong("sql_exec_seconds")
                    ));
                }
            }
        }

        return out;
    }

    private static String fetchSqlText(
            Connection c,
            String sqlId,
            int child) throws SQLException {

        try (PreparedStatement ps = c.prepareStatement(SQL_TEXT)) {

            ps.setQueryTimeout(QUERY_TIMEOUT_SEC);
            ps.setString(1, sqlId);
            ps.setInt(2, child);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Clob clob = rs.getClob(1);
                if (clob == null) {
                    return null;
                }

                long length = Math.min(clob.length(), 200_000L);
                return clob.getSubString(1L, (int) length);
            }

        } catch (SQLTimeoutException e) {
            log("SQLTEXT_TIMEOUT sqlId=" + sqlId + " child=" + child);
            return null;
        }
    }

    private static SqlInfo classify(
            Connection c,
            String sqlId,
            int child,
            String sql) throws SQLException {

        // Exact known bad SQL: first sight = death warrant.
        if (BLACKLIST_SQL_IDS.contains(sqlId)) {
            boolean select = sql != null && SELECT.matcher(sql).find();
            boolean acvFamily = sql != null && isAcvFamily(sql);
            boolean touchesReportStore = sql != null
                    && sql.toUpperCase(Locale.ROOT).contains("REPORTSTORETEMP");

            return new SqlInfo(
                    sql,
                    touchesReportStore,
                    select,
                    acvFamily,
                    true,
                    null,
                    "BLACKLIST_SQL_ID");
        }

        if (sql == null) {
            return new SqlInfo(
                    null, false, false, false, false, null, "NO_SQL_TEXT");
        }

        String upper = sql.toUpperCase(Locale.ROOT);
        String normalized = sql.stripLeading().toUpperCase(Locale.ROOT);

        boolean touchesReportStore = upper.contains("REPORTSTORETEMP");
        boolean select = SELECT.matcher(sql).find();
        boolean acvFamily = isAcvFamily(sql);
        boolean writer = WRITER.matcher(sql).find();

        if (touchesReportStore && writer) {

            boolean heavy;
            Long estimatedRows = null;
            String reason;

            if (INSERT.matcher(sql).find()) {

                if (VALUES.matcher(sql).find()) {
                    heavy = false;
                    reason = "ROW_BY_ROW_INSERT_VALUES";
                } else {
                    String insertTarget = extractInsertTarget(sql);

                    if (insertTarget != null
                            && IMMEDIATE_INSERT_TARGETS.contains(
                                    insertTarget.toUpperCase(Locale.ROOT))) {
                        heavy = true;
                        reason = "IMMEDIATE_SET_BASED_INSERT_TARGET=" + insertTarget;
                    } else {
                        estimatedRows = fetchEstimatedRows(c, sqlId, child);

                        if (estimatedRows == null) {
                            heavy = "KILL".equals(UNKNOWN_PLAN);
                            reason = heavy
                                    ? "SET_BASED_INSERT_PLAN_UNKNOWN_KILL"
                                    : "SET_BASED_INSERT_PLAN_UNKNOWN_ALLOW";
                        } else {
                            heavy = estimatedRows >= MIN_ESTIMATED_ROWS;
                            reason = "SET_BASED_INSERT_EST_ROWS=" + estimatedRows;
                        }
                    }
                }

            } else if (normalized.startsWith("MERGE")
                    || normalized.startsWith("UPDATE")
                    || normalized.startsWith("DELETE")) {

                heavy = true;
                reason = "REPORTSTORE_DML";

            } else {
                // TRUNCATE is allowed in HEAVY_ONLY mode.
                heavy = false;
                reason = "REPORTSTORE_TRUNCATE";
            }

            return new SqlInfo(
                    sql,
                    true,
                    false,
                    acvFamily,
                    heavy,
                    estimatedRows,
                    reason);
        }

        /*
         * V9: SELECT classification is static. No read-rate decision is cached
         * here. Runtime ACV work is evaluated per active execution in pollOnce().
         */
        if (select) {
            String reason;

            if (acvFamily && touchesReportStore) {
                reason = "REPORTSTORE_ACV_READ_MONITORED";
            } else if (acvFamily) {
                reason = "ACV_READ_MONITORED";
            } else if (touchesReportStore) {
                reason = "REPORTSTORE_READ_ALLOWED";
            } else {
                reason = "NON_REPORTSTORE_READ";
            }

            return new SqlInfo(
                    sql,
                    touchesReportStore,
                    true,
                    acvFamily,
                    false,
                    null,
                    reason);
        }

        return new SqlInfo(
                sql,
                touchesReportStore,
                false,
                acvFamily,
                false,
                null,
                touchesReportStore ? "REPORTSTORE_NONSELECT_ALLOWED" : "NOT_MONITORED");
    }

    private static SqlStats fetchSqlStats(
            Connection c,
            String sqlId,
            int child) throws SQLException {

        try (PreparedStatement ps = c.prepareStatement(SQL_STATS)) {

            ps.setQueryTimeout(QUERY_TIMEOUT_SEC);
            ps.setString(1, sqlId);
            ps.setInt(2, child);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                return new SqlStats(
                        rs.getLong("executions"),
                        rs.getLong("buffer_gets"),
                        rs.getLong("disk_reads"),
                        rs.getLong("rows_processed"),
                        rs.getLong("elapsed_time"),
                        rs.getLong("cpu_time"),
                        rs.getLong("user_io_wait_time"),
                        rs.getLong("plan_hash_value"));
            }

        } catch (SQLTimeoutException e) {
            log("SQLSTATS_TIMEOUT sqlId=" + sqlId + " child=" + child);
            return null;
        }
    }

    private static WriterDecision evaluateGiantWriter(
            Connection c,
            SessionSnap s,
            SqlInfo info,
            double redoMiBPerSec) throws SQLException {

        SqlStats stats = fetchSqlStats(c, s.sqlId(), s.child());

        if (stats == null) {
            GIANT_WRITER_CANDIDATE_POLLS.remove(s.executionKey());
            return new WriterDecision(
                    false,
                    false,
                    0,
                    "GIANT_WRITER_STATS_UNKNOWN",
                    "GIANT_WRITER_EMERGENCY_STATS_UNKNOWN");
        }

        long execDivisor = Math.max(stats.executions(), 1L);
        long bufferGetsPerExec = stats.bufferGets() / execDivisor;
        long diskReadsPerExec = stats.diskReads() / execDivisor;
        long rowsPerExec = stats.rowsProcessed() / execDivisor;
        double userIoWaitSecPerExec =
                stats.userIoWaitMicros() / 1_000_000.0d / execDivisor;

        boolean normalWork =
                diskReadsPerExec >= GIANT_WRITER_MIN_DISK_READS_PER_EXEC
                || bufferGetsPerExec >= GIANT_WRITER_MIN_BUFFER_GETS_PER_EXEC
                || (GIANT_WRITER_MIN_REDO_MIB_PER_SEC > 0.0d
                    && redoMiBPerSec >= GIANT_WRITER_MIN_REDO_MIB_PER_SEC);

        boolean normalCandidate =
                s.sqlExecSeconds() >= GIANT_WRITER_MIN_ELAPSED_SEC
                && normalWork;

        boolean emergencyWork =
                diskReadsPerExec >= GIANT_WRITER_EMERGENCY_MIN_DISK_READS_PER_EXEC
                || bufferGetsPerExec >= GIANT_WRITER_EMERGENCY_MIN_BUFFER_GETS_PER_EXEC
                || (GIANT_WRITER_MIN_REDO_MIB_PER_SEC > 0.0d
                    && redoMiBPerSec >= GIANT_WRITER_MIN_REDO_MIB_PER_SEC);

        boolean emergencyQualified =
                "log buffer space".equalsIgnoreCase(nullSafe(s.event()))
                && s.secondsInWait() >= GIANT_WRITER_EMERGENCY_LOG_BUFFER_SPACE_SEC
                && s.sqlExecSeconds() >= GIANT_WRITER_EMERGENCY_MIN_ELAPSED_SEC
                && emergencyWork;

        int polls;

        if (normalCandidate) {
            polls = GIANT_WRITER_CANDIDATE_POLLS.merge(
                    s.executionKey(), 1, Integer::sum);
        } else {
            GIANT_WRITER_CANDIDATE_POLLS.remove(s.executionKey());
            polls = 0;
        }

        boolean normalQualified =
                normalCandidate && polls >= GIANT_WRITER_MIN_POLLS;

        String common =
                "_CURRENT_EXEC_SEC=" + s.sqlExecSeconds()
                + "_EVENT=" + nullSafe(s.event()).replace(' ', '_')
                + "_EVENT_WAIT_SEC=" + s.secondsInWait()
                + "_BUFFER_GETS_PER_EXEC=" + bufferGetsPerExec
                + "_DISK_READS_PER_EXEC=" + diskReadsPerExec
                + "_REDO_MIB_S="
                + String.format(Locale.ROOT, "%.1f", redoMiBPerSec)
                + "_USER_IO_WAIT_S_PER_EXEC="
                + String.format(Locale.ROOT, "%.2f", userIoWaitSecPerExec)
                + "_ROWS_PER_EXEC=" + rowsPerExec
                + "_PLAN_HASH=" + stats.planHashValue()
                + "_TARGET=" + Optional.ofNullable(extractInsertTarget(info.text())).orElse("UNKNOWN")
                + "_POLL=" + polls + "/" + GIANT_WRITER_MIN_POLLS;

        return new WriterDecision(
                normalQualified,
                emergencyQualified,
                polls,
                "GIANT_WRITER" + common,
                "GIANT_WRITER_EMERGENCY" + common);
    }

    private static void logGiantWriterWarning(
            SessionSnap s,
            SqlInfo info,
            boolean emergency,
            String reason) {

        long now = System.currentTimeMillis();
        Map<String, Long> warningTimes = emergency
                ? GIANT_WRITER_EMERGENCY_WARNING_TIMES
                : GIANT_WRITER_NORMAL_WARNING_TIMES;
        Long previous = warningTimes.get(s.executionKey());

        if (previous != null && now - previous < COOLDOWN_MS) {
            return;
        }

        warningTimes.put(s.executionKey(), now);

        log((emergency ? "GIANT_WRITER_EMERGENCY_WARNING" : "GIANT_WRITER_WARNING")
                + " sid=" + s.sid()
                + " serial=" + s.serial()
                + " user=" + s.username()
                + " machine=" + s.machine()
                + " sqlId=" + s.sqlId()
                + " event=" + nullSafe(s.event())
                + " reason=" + reason
                + " sql=" + compact(info.text()));
    }

    private static void logStaticWarning(SessionSnap s, SqlInfo info) {
        long now = System.currentTimeMillis();
        Long previous = STATIC_WARNING_TIMES.get(s.executionKey());

        if (previous != null && now - previous < COOLDOWN_MS) {
            return;
        }

        STATIC_WARNING_TIMES.put(s.executionKey(), now);

        log("STATIC_REPORTSTORE_WARNING"
                + " sid=" + s.sid()
                + " serial=" + s.serial()
                + " user=" + s.username()
                + " machine=" + s.machine()
                + " sqlId=" + s.sqlId()
                + " event=" + nullSafe(s.event())
                + " heavy=" + info.heavy()
                + " estimatedRows="
                + (info.estimatedRows() == null ? "UNKNOWN" : info.estimatedRows())
                + " reason=" + info.reason()
                + " sql=" + compact(info.text()));
    }

    private static AcvDecision evaluateAcvReader(
            Connection c,
            SessionSnap s,
            SqlInfo info) throws SQLException {

        SqlStats stats = fetchSqlStats(c, s.sqlId(), s.child());

        if (stats == null) {
            ACV_CANDIDATE_POLLS.remove(s.executionKey());
            return new AcvDecision(
                    false,
                    0,
                    "ACV_READER_STATS_UNKNOWN");
        }

        long execDivisor = Math.max(stats.executions(), 1L);
        long bufferGetsPerExec = stats.bufferGets() / execDivisor;
        long diskReadsPerExec = stats.diskReads() / execDivisor;
        long rowsPerExec = stats.rowsProcessed() / execDivisor;
        double userIoWaitSecPerExec =
                stats.userIoWaitMicros() / 1_000_000.0d / execDivisor;

        boolean enoughElapsed =
                s.sqlExecSeconds() >= ACV_READER_MIN_ELAPSED_SEC;

        boolean enoughAbsoluteWork =
                diskReadsPerExec >= ACV_READER_MIN_DISK_READS_PER_EXEC
                || bufferGetsPerExec >= ACV_READER_MIN_BUFFER_GETS_PER_EXEC;

        boolean enoughUserIo =
                ACV_READER_MIN_USER_IO_WAIT_SEC_PER_EXEC <= 0.0d
                || userIoWaitSecPerExec
                        >= ACV_READER_MIN_USER_IO_WAIT_SEC_PER_EXEC;

        boolean candidate =
                enoughElapsed && enoughAbsoluteWork && enoughUserIo;

        int polls;

        if (candidate) {
            polls = ACV_CANDIDATE_POLLS.merge(
                    s.executionKey(), 1, Integer::sum);
        } else {
            ACV_CANDIDATE_POLLS.remove(s.executionKey());
            polls = 0;
        }

        boolean qualified =
                candidate && polls >= ACV_READER_MIN_POLLS;

        String reason =
                "ACV_RUNAWAY_READER"
                + "_CURRENT_EXEC_SEC=" + s.sqlExecSeconds()
                + "_BUFFER_GETS_PER_EXEC=" + bufferGetsPerExec
                + "_DISK_READS_PER_EXEC=" + diskReadsPerExec
                + "_USER_IO_WAIT_S_PER_EXEC="
                + String.format(Locale.ROOT, "%.2f", userIoWaitSecPerExec)
                + "_ROWS_PER_EXEC=" + rowsPerExec
                + "_PLAN_HASH=" + stats.planHashValue()
                + "_REPORTSTORE=" + info.reportStoreRelated()
                + "_POLL=" + polls + "/" + ACV_READER_MIN_POLLS;

        return new AcvDecision(qualified, polls, reason);
    }

    private static void logAcvWarning(
            SessionSnap s,
            SqlInfo info,
            AcvDecision decision) {

        long now = System.currentTimeMillis();
        Long previous = ACV_WARNING_TIMES.get(s.executionKey());

        if (previous != null && now - previous < COOLDOWN_MS) {
            return;
        }

        ACV_WARNING_TIMES.put(s.executionKey(), now);

        log("ACV_RUNAWAY_WARNING"
                + " sid=" + s.sid()
                + " serial=" + s.serial()
                + " user=" + s.username()
                + " machine=" + s.machine()
                + " sqlId=" + s.sqlId()
                + " event=" + nullSafe(s.event())
                + " reason=" + decision.reason()
                + " sql=" + compact(info.text()));
    }

    private static boolean isAcvFamily(String sql) {
        if (sql == null) {
            return false;
        }

        String upper = sql.toUpperCase(Locale.ROOT);

        return Pattern.compile("\\bARTICLECHARACTVALUE\\b").matcher(upper).find()
                && Pattern.compile("\\bARTICLECHARACTVALUELANG\\b").matcher(upper).find();
    }

    private static String extractInsertTarget(String sql) {
        if (sql == null) {
            return null;
        }

        java.util.regex.Matcher m = INSERT_TARGET.matcher(sql);
        return m.find() ? m.group(1) : null;
    }

    private static boolean isReportStoreWriter(String sql) {
        if (sql == null) {
            return false;
        }

        return sql.toUpperCase(Locale.ROOT).contains("REPORTSTORETEMP")
                && WRITER.matcher(sql).find();
    }

    private static boolean isSetBasedReportStoreInsert(String sql) {
        if (sql == null) {
            return false;
        }

        String upper = sql.toUpperCase(Locale.ROOT);
        return upper.contains("REPORTSTORETEMP")
                && INSERT.matcher(sql).find()
                && !VALUES.matcher(sql).find();
    }

    private static Map<String, Double> readActiveSessionRedoRates(Connection c) {
        Map<String, Double> rates = new HashMap<>();
        long now = System.currentTimeMillis();

        try (PreparedStatement ps = c.prepareStatement(ACTIVE_SESSION_REDO)) {
            ps.setQueryTimeout(QUERY_TIMEOUT_SEC);

            try (ResultSet rs = ps.executeQuery()) {
                Set<String> seen = new HashSet<>();

                while (rs.next()) {
                    int sid = rs.getInt("sid");
                    int serial = rs.getInt("serial#");
                    String sqlId = rs.getString("sql_id");
                    long redoBytes = rs.getLong("redo_bytes");

                    String key = sid + ":" + serial + ":" + sqlId;
                    seen.add(key);

                    SessionRedoSnap previous = SESSION_REDO_SNAP.put(
                            key, new SessionRedoSnap(redoBytes, now));

                    if (previous != null
                            && now > previous.capturedAtMillis()
                            && redoBytes >= previous.redoBytes()) {

                        double seconds =
                                (now - previous.capturedAtMillis()) / 1000.0d;

                        double mibPerSec =
                                (redoBytes - previous.redoBytes())
                                / 1048576.0d / seconds;

                        rates.put(key, mibPerSec);
                    }
                }

                SESSION_REDO_SNAP.keySet().removeIf(k -> !seen.contains(k));
            }

        } catch (SQLTimeoutException e) {
            log("REDO_RATE_TIMEOUT " + oneLine(e.getMessage()));
        } catch (SQLException e) {
            log("REDO_RATE_ERROR ora=" + e.getErrorCode()
                    + " msg=" + oneLine(e.getMessage()));
        }

        return rates;
    }

    private static boolean isDmlWriterSql(String sql) {
        return sql != null && DML_WRITER.matcher(sql).find();
    }

    private static boolean isWriterSql(String sql) {
        return sql != null && WRITER.matcher(sql).find();
    }

    private static Long fetchEstimatedRows(
            Connection c,
            String sqlId,
            int child) throws SQLException {

        try (PreparedStatement ps = c.prepareStatement(PLAN_ESTIMATE)) {

            ps.setQueryTimeout(QUERY_TIMEOUT_SEC);
            ps.setString(1, sqlId);
            ps.setInt(2, child);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long value = rs.getLong(1);
                    return rs.wasNull() ? null : value;
                }
                return null;
            }

        } catch (SQLTimeoutException e) {
            log("PLAN_TIMEOUT sqlId=" + sqlId + " child=" + child);
            return null;
        }
    }

    private static void startKillerWorkers() {

        for (int i = 1; i <= KILLER_WORKERS; i++) {
            final int workerId = i;

            Thread t = new Thread(
                    () -> killerLoop(workerId),
                    "reportstore-killer-" + workerId);

            t.setDaemon(true);
            killerThreads.add(t);
            t.start();
        }
    }

    private static void killerLoop(int workerId) {

        Connection killerConnection = null;

        while (running) {

            try {
                if (killerConnection == null
                        || killerConnection.isClosed()
                        || !killerConnection.isValid(1)) {

                    closeQuietly(killerConnection);

                    killerConnection =
                            DriverManager.getConnection(URL, USER, PASSWORD);

                    killerConnection.setAutoCommit(true);

                    log("KILLER_" + workerId + "_DB_CONNECTED");
                }

                Target target = killQueue.poll(1, TimeUnit.SECONDS);

                if (target == null) {
                    continue;
                }

                terminate(killerConnection, workerId, target);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;

            } catch (SQLException e) {
                log("KILLER_" + workerId
                        + "_DB_ERROR ora=" + e.getErrorCode()
                        + " msg=" + oneLine(e.getMessage()));

                closeQuietly(killerConnection);
                killerConnection = null;

            } catch (Throwable t) {
                log("KILLER_" + workerId
                        + "_ERROR " + t.getClass().getSimpleName()
                        + ": " + oneLine(t.getMessage()));
            }
        }

        closeQuietly(killerConnection);
    }

    private static void terminate(
            Connection c,
            int workerId,
            Target target) throws SQLException {

        // Revalidate the exact execution: a queued reader may already have finished.
        try (PreparedStatement guard = c.prepareStatement(
                "select 1 from v$session where sid=? and serial#=? and username=? "
                + "and machine=? and sql_id=? and type='USER' and status='ACTIVE' "
                + "and nvl(sql_exec_id,0)=? "
                + "and nvl(to_char(sql_exec_start,'YYYYMMDDHH24MISS'),'NA')=?")) {
            guard.setQueryTimeout(SESSION_QUERY_TIMEOUT_SEC);
            guard.setInt(1, target.sid()); guard.setInt(2, target.serial());
            guard.setString(3, target.username()); guard.setString(4, target.machine());
            guard.setString(5, target.sqlId()); guard.setLong(6, target.sqlExecId());
            guard.setString(7, target.sqlExecStartKey());
            try (ResultSet current = guard.executeQuery()) {
                if (!current.next()) {
                    log("STALE_TARGET_SKIPPED sid=" + target.sid() + " sqlId=" + target.sqlId());
                    return;
                }
            }
        }
        String alter;

        if ("KILL".equals(ACTION)) {
            alter = "alter system kill session '"
                    + target.sid() + "," + target.serial()
                    + "' immediate";
        } else {
            alter = "alter system disconnect session '"
                    + target.sid() + "," + target.serial()
                    + "' immediate";
        }

        long delayMs =
                System.currentTimeMillis() - target.detectedAtMillis();

        log("FIRE worker=" + workerId
                + " action=" + ACTION
                + " sid=" + target.sid()
                + " serial=" + target.serial()
                + " queueDelayMs=" + delayMs);

        try (Statement st = c.createStatement()) {

            st.setQueryTimeout(ACTION_TIMEOUT_SEC);
            st.execute(alter);

            log("TERMINATE_SENT worker=" + workerId
                    + " action=" + ACTION
                    + " sid=" + target.sid()
                    + " serial=" + target.serial());

        } catch (SQLTimeoutException e) {

            // The ALTER may already have reached Oracle.
            // Crucially, this worker can timeout without stopping the monitor
            // or the other killer worker(s).
            log("TERMINATE_TIMEOUT worker=" + workerId
                    + " action=" + ACTION
                    + " sid=" + target.sid()
                    + " serial=" + target.serial()
                    + " after=" + ACTION_TIMEOUT_SEC + "s");

        } catch (SQLException e) {

            // ORA-00030 session does not exist.
            // ORA-00031 session marked for kill.
            if (e.getErrorCode() == 30 || e.getErrorCode() == 31) {

                log("ALREADY_GONE worker=" + workerId
                        + " sid=" + target.sid()
                        + " serial=" + target.serial()
                        + " ora=" + e.getErrorCode());

            } else {
                throw e;
            }
        }
    }

    private static Connection getMonitorConnection() throws SQLException {

        if (monitorConnection == null
                || monitorConnection.isClosed()
                || !monitorConnection.isValid(1)) {

            closeQuietly(monitorConnection);

            monitorConnection =
                    DriverManager.getConnection(URL, USER, PASSWORD);

            monitorConnection.setAutoCommit(true);

            log("MONITOR_DB_CONNECTED");
        }

        return monitorConnection;
    }

    private static void purgeCaches() {

        long now = System.currentTimeMillis();

        WARRANTS.entrySet().removeIf(
                e -> now - e.getValue() > Math.max(
                        COOLDOWN_MS * 2L, 300_000L));

        // SQL_ID text is immutable for our purpose. This just prevents
        // unbounded growth over a very long-running temporary watchdog.
        if (SQL_CACHE.size() > 20_000) {
            SQL_CACHE.clear();
            ACV_CANDIDATE_POLLS.clear();
            ACV_WARNING_TIMES.clear();
            GIANT_WRITER_CANDIDATE_POLLS.clear();
            GIANT_WRITER_NORMAL_WARNING_TIMES.clear();
            GIANT_WRITER_EMERGENCY_WARNING_TIMES.clear();
            STATIC_WARNING_TIMES.clear();
            SESSION_REDO_SNAP.clear();
            log("SQL_CACHE_CLEARED");
        }
    }

    private static void validateConfig() {

        if (!"HEAVY_ONLY".equals(MODE)
                && !"ALL_WRITERS".equals(MODE)) {

            throw new IllegalArgumentException(
                    "REPORTSTORE_KILL_MODE must be HEAVY_ONLY or ALL_WRITERS, got: "
                            + MODE);
        }

        if (!"KILL".equals(UNKNOWN_PLAN)
                && !"ALLOW".equals(UNKNOWN_PLAN)) {

            throw new IllegalArgumentException(
                    "REPORTSTORE_KILL_UNKNOWN_PLAN must be KILL or ALLOW, got: "
                            + UNKNOWN_PLAN);
        }

        if (!"DISCONNECT".equals(ACTION)
                && !"KILL".equals(ACTION)) {

            throw new IllegalArgumentException(
                    "REPORTSTORE_KILL_ACTION must be DISCONNECT or KILL, got: "
                            + ACTION);
        }

        if (!"OFF".equals(STATIC_ACTION)
                && !"WARN".equals(STATIC_ACTION)
                && !"KILL".equals(STATIC_ACTION)) {

            throw new IllegalArgumentException(
                    "REPORTSTORE_KILL_STATIC_ACTION must be OFF, WARN or KILL, got: "
                            + STATIC_ACTION);
        }

        if (!"OFF".equals(GIANT_WRITER_MODE)
                && !"WARN".equals(GIANT_WRITER_MODE)
                && !"KILL".equals(GIANT_WRITER_MODE)) {

            throw new IllegalArgumentException(
                    "REPORTSTORE_KILL_GIANT_WRITER_MODE must be OFF, WARN or KILL, got: "
                            + GIANT_WRITER_MODE);
        }

        if (GIANT_WRITER_MIN_POLLS < 1) {
            throw new IllegalArgumentException(
                    "REPORTSTORE_KILL_GIANT_WRITER_MIN_POLLS must be >= 1");
        }

        if (!"OFF".equals(GIANT_WRITER_EMERGENCY_ACTION)
                && !"WARN".equals(GIANT_WRITER_EMERGENCY_ACTION)
                && !"KILL".equals(GIANT_WRITER_EMERGENCY_ACTION)) {

            throw new IllegalArgumentException(
                    "REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_ACTION must be OFF, WARN or KILL, got: "
                            + GIANT_WRITER_EMERGENCY_ACTION);
        }

        if (GIANT_WRITER_EMERGENCY_COOLDOWN_MS < 0L) {
            throw new IllegalArgumentException(
                    "REPORTSTORE_KILL_GIANT_WRITER_EMERGENCY_COOLDOWN_SEC must be >= 0");
        }

        if (WATCHED_USERS.isEmpty()) {
            throw new IllegalArgumentException(
                    "REPORTSTORE_KILL_USERS must contain at least one username");
        }

        if (!"OFF".equals(ACV_READER_MODE)
                && !"WARN".equals(ACV_READER_MODE)
                && !"KILL".equals(ACV_READER_MODE)) {

            throw new IllegalArgumentException(
                    "REPORTSTORE_KILL_ACV_READER_MODE must be OFF, WARN or KILL, got: "
                            + ACV_READER_MODE);
        }

        if (ACV_READER_MIN_POLLS < 1) {
            throw new IllegalArgumentException(
                    "REPORTSTORE_KILL_ACV_READER_MIN_POLLS must be >= 1");
        }

        if (KILLER_WORKERS < 1 || KILLER_WORKERS > 8) {
            throw new IllegalArgumentException(
                    "REPORTSTORE_KILL_WORKERS must be between 1 and 8");
        }
    }

    private static String compact(String s) {

        if (s == null) {
            return "";
        }

        String x = s.replaceAll("\\s+", " ").trim();

        return x.length() <= 320
                ? x
                : x.substring(0, 320) + "...";
    }

    private static String oneLine(String s) {
        return s == null
                ? ""
                : s.replace('\n', ' ')
                   .replace('\r', ' ')
                   .trim();
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static Set<String> parseCsvSet(String csv) {

        Set<String> out = new HashSet<>();

        if (csv == null || csv.isBlank()) {
            return out;
        }

        for (String value : csv.split(",")) {
            String v = value.trim();
            if (!v.isEmpty()) {
                out.add(v);
            }
        }

        return Collections.unmodifiableSet(out);
    }

    private static Set<String> parseCsvSetUpper(String csv) {
        Set<String> out = new HashSet<>();

        if (csv == null || csv.isBlank()) {
            return out;
        }

        for (String value : csv.split(",")) {
            String v = value.trim();
            if (!v.isEmpty()) {
                out.add(v.toUpperCase(Locale.ROOT));
            }
        }

        return Collections.unmodifiableSet(out);
    }

    private static String sqlStringList(Set<String> values) {
        StringJoiner j = new StringJoiner(",");

        for (String value : values) {
            j.add("'" + value.replace("'", "''") + "'");
        }

        return j.toString();
    }

    private static String requireEnv(String name) {

        String v = System.getenv(name);

        if (v == null || v.isBlank()) {
            throw new IllegalStateException(
                    "Missing required environment variable: " + name);
        }

        return v;
    }

    private static long envLong(String name, long def) {

        String v = System.getenv(name);

        return (v == null || v.isBlank())
                ? def
                : Long.parseLong(v);
    }

    private static double envDouble(String name, double def) {
        String v = System.getenv(name);

        return (v == null || v.isBlank())
                ? def
                : Double.parseDouble(v);
    }

    private static int envInt(String name, int def) {

        String v = System.getenv(name);

        return (v == null || v.isBlank())
                ? def
                : Integer.parseInt(v);
    }

    private static void log(String msg) {

        System.out.println(
                "[" + LocalDateTime.now().format(TS) + "] " + msg);

        System.out.flush();
    }

    private static void closeQuietly(AutoCloseable x) {

        if (x == null) {
            return;
        }

        try {
            x.close();
        } catch (Exception ignored) {
        }
    }
}
