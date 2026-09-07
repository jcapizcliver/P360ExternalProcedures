package mx.com.liverpool.p360.services.core.completeness;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;

public final class CompletenessWorkDao {

    private final Connection connection;

    public CompletenessWorkDao(Connection connection) {
        this.connection = connection;
    }

    public void replaceRun(String runId, Collection<String> productIdentifiers) throws SQLException {
        deleteRun(runId);
        if (productIdentifiers == null || productIdentifiers.isEmpty()) return;

        try (PreparedStatement ps = connection.prepareStatement(
                "insert into P360_EXPLOIT.TT_PRODUCT_COMPLETENESS_WORK " +
                "(RUN_ID, PRODUCT_ID) values (?, ?)")) {
            int pending = 0;
            for (String id : productIdentifiers) {
                if (id == null || id.isBlank()) continue;
                ps.setString(1, runId);
                ps.setNString(2, id.trim());
                ps.addBatch();
                pending++;
                if (pending % 1000 == 0) {
                    ps.executeBatch();
                    ps.clearBatch();
                }
            }
            if (pending % 1000 != 0) {
                ps.executeBatch();
                ps.clearBatch();
            }
        }
    }

    public void deleteRun(String runId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "delete from P360_EXPLOIT.TT_PRODUCT_COMPLETENESS_WORK where RUN_ID = ?")) {
            ps.setString(1, runId);
            ps.executeUpdate();
        }
    }
}
