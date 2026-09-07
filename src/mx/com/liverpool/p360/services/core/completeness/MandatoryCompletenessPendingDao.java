package mx.com.liverpool.p360.services.core.completeness;

import java.sql.*;
import java.util.*;
import org.json.JSONArray;

/** All writes are confined to P360_EXPLOIT. Caller owns the transaction. */
public final class MandatoryCompletenessPendingDao {
    public static final String TABLE = "P360_EXPLOIT.TB_MANDATORY_COMP_PENDING";
    public static final String CONTROL = "P360_EXPLOIT.TB_MANDATORY_COMP_CONTROL";
    private final Connection c;
    public MandatoryCompletenessPendingDao(Connection c) { this.c = c; }
    public record Pending(MandatoryCompletenessChange change, long version, long cursor, int attempts) {}

    public void preflight() throws SQLException {
        try (var s = c.createStatement()) {
            s.setQueryTimeout(30);
            try (var r = s.executeQuery("select ID from " + CONTROL + " order by ID")) {
                if (!r.next() || r.getInt(1) != 1 || !r.next() || r.getInt(1) != 2 || r.next())
                    throw new SQLException("Mandatory completeness control rows missing");
            }
            s.executeQuery("select ENTITY_TYPE,ENTITY_IDENTIFIER,CHANGE_VERSION,FORCE_RECALC,"
                    + "CHARACTERISTICS,PAGE_CURSOR,CREATED_AT,UPDATED_AT,NEXT_ATTEMPT,ATTEMPTS,LAST_ERROR from "
                    + TABLE + " where 1=0").close();
            s.executeUpdate("update " + TABLE + " set ATTEMPTS=ATTEMPTS where 1=0");
        }
        c.rollback();
    }

    // Dedicated connection keeps this lock for the lifetime of a worker. Intake uses ID=1.
    public void lockWorker() throws SQLException {
        try (var s = c.createStatement(); var r = s.executeQuery("select ID from " + CONTROL + " where ID=2 for update nowait")) {
            if (!r.next()) throw new SQLException("Worker control missing");
        }
    }

    private long[] lockQuota() throws SQLException {
        try (var s = c.createStatement(); var r = s.executeQuery("select PENDING_COUNT,MAX_PENDING from "
                + CONTROL + " where ID=1 for update wait 5")) {
            if (!r.next()) throw new SQLException("Quota control missing");
            return new long[] {r.getLong(1), r.getLong(2)};
        }
    }

    public void enqueue(MandatoryCompletenessChange change) throws SQLException {
        long[] quota = lockQuota();
        Pending previous = null;
        try (var s = c.prepareStatement("select * from " + TABLE + " where ENTITY_TYPE=? and ENTITY_IDENTIFIER=?")) {
            key(s, change, 1);
            try (var r = s.executeQuery()) { if (r.next()) previous = read(r); }
        }
        if (previous != null) {
            change = change.merge(previous.change());
            try (var s = c.prepareStatement("update " + TABLE + " set CHANGE_VERSION=CHANGE_VERSION+1,"
                    + "FORCE_RECALC=?,CHARACTERISTICS=?,PAGE_CURSOR=0,UPDATED_AT=SYSTIMESTAMP,"
                    + "NEXT_ATTEMPT=least(NEXT_ATTEMPT,SYSTIMESTAMP),ATTEMPTS=0,LAST_ERROR=null "
                    + "where ENTITY_TYPE=? and ENTITY_IDENTIFIER=?")) {
                s.setInt(1, change.force() ? 1 : 0);
                s.setString(2, new JSONArray(change.characteristics()).toString());
                key(s, change, 3); s.executeUpdate();
            }
        } else {
            if (quota[0] >= quota[1]) throw new SQLException("Mandatory pending capacity reached; JMS message remains unacknowledged", "MCFULL");
            try (var s = c.prepareStatement("insert into " + TABLE
                    + " (ENTITY_TYPE,ENTITY_IDENTIFIER,FORCE_RECALC,CHARACTERISTICS) values (?,?,?,?)")) {
                key(s, change, 1); s.setInt(3, change.force() ? 1 : 0);
                s.setString(4, new JSONArray(change.characteristics()).toString()); s.executeUpdate();
            }
            count(1);
        }
    }

    public List<Pending> due(int limit) throws SQLException {
        List<Pending> result = new ArrayList<>();
        try (var s = c.prepareStatement("select * from " + TABLE
                + " where NEXT_ATTEMPT<=SYSTIMESTAMP order by NEXT_ATTEMPT,CREATED_AT fetch first ? rows only")) {
            s.setInt(1, limit); s.setQueryTimeout(30);
            try (var r = s.executeQuery()) { while (r.next()) result.add(read(r)); }
        }
        return result;
    }

    public void complete(Pending p) throws SQLException {
        lockQuota();
        try (var s = c.prepareStatement("delete from " + TABLE + " where ENTITY_TYPE=? and ENTITY_IDENTIFIER=? and CHANGE_VERSION=?")) {
            key(s, p.change(), 1); s.setLong(3, p.version()); count(-s.executeUpdate());
        }
    }

    public void advance(Pending p, long cursor) throws SQLException {
        try (var s = c.prepareStatement("update " + TABLE + " set PAGE_CURSOR=?,ATTEMPTS=0,LAST_ERROR=null,"
                + "NEXT_ATTEMPT=SYSTIMESTAMP+numtodsinterval(2,'SECOND') where ENTITY_TYPE=? and ENTITY_IDENTIFIER=? and CHANGE_VERSION=?")) {
            s.setLong(1, cursor); key(s, p.change(), 2); s.setLong(4, p.version()); s.executeUpdate();
        }
    }

    public void fail(Pending p, Exception error) throws SQLException {
        String text = error.getClass().getSimpleName() + ": " + String.valueOf(error.getMessage());
        // ASCII diagnostic is bounded in bytes too; raw payloads/responses are not retained here.
        text = text.replaceAll("[^\\x20-\\x7e]", "?");
        if (text.length() > 1000) text = text.substring(0, 1000);
        try (var s = c.prepareStatement("update " + TABLE + " set ATTEMPTS=least(ATTEMPTS+1,1000000),LAST_ERROR=?,"
                + "NEXT_ATTEMPT=SYSTIMESTAMP+numtodsinterval(?,'SECOND') where ENTITY_TYPE=? and ENTITY_IDENTIFIER=? and CHANGE_VERSION=?")) {
            s.setString(1, text); s.setLong(2, Math.min(900, 5L << Math.min(p.attempts(), 8)));
            key(s, p.change(), 3); s.setLong(5, p.version()); s.executeUpdate();
        }
    }

    private void count(int delta) throws SQLException {
        if (delta == 0) return;
        try (var s = c.prepareStatement("update " + CONTROL + " set PENDING_COUNT=PENDING_COUNT+? where ID=1")) {
            s.setInt(1, delta); s.executeUpdate();
        }
    }
    private static void key(PreparedStatement s, MandatoryCompletenessChange p, int i) throws SQLException {
        s.setString(i, p.entity()); s.setNString(i+1, p.identifier());
    }
    private static Pending read(ResultSet r) throws SQLException {
        Set<String> codes = new LinkedHashSet<>();
        JSONArray a = new JSONArray(r.getString("CHARACTERISTICS"));
        for (int i=0;i<a.length();i++) codes.add(a.getString(i));
        return new Pending(new MandatoryCompletenessChange(r.getString("ENTITY_TYPE"),r.getNString("ENTITY_IDENTIFIER"),
                r.getInt("FORCE_RECALC") == 1,codes),r.getLong("CHANGE_VERSION"),r.getLong("PAGE_CURSOR"),r.getInt("ATTEMPTS"));
    }
}
