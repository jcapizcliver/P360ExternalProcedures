package mx.com.liverpool.p360.services.core.temp.exports;

public final class EnvioAtgDao {

    public static final class EnvioAtgRow {
        public final String productId;
        public final Long productSku;
        public final String articleId;
        public final Long articleSku;

        public EnvioAtgRow(String productId, Long productSku, String articleId, Long articleSku) {
            this.productId = productId;
            this.productSku = productSku;
            this.articleId = articleId;
            this.articleSku = articleSku;
        }
    }

    public static long guardarEnvioCompleto(
            java.sql.Connection con,
            String execId,
            String xml,
            java.util.List<EnvioAtgRow> rows
    ) throws java.sql.SQLException {

        boolean oldAutoCommit = con.getAutoCommit();

        try {
            con.setAutoCommit(false);

            long envioAtgExecId = insertarExec(con, execId, "RUNNING");
            long envioAtgXmlId = insertarXml(con, envioAtgExecId, xml);

            insertarDetalleBatch(con, envioAtgXmlId, rows, 1000);

            finalizarExec(con, envioAtgExecId, "SUCCESS");

            con.commit();

            return envioAtgExecId;
        } catch (java.sql.SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }

    public static void actualizarExec(
            java.sql.Connection con,
            long envioAtgExecId,
            String status,
            String message
    ) throws java.sql.SQLException {

        String sql = "update P360_EXPLOIT.TB_ENVIO_ATG_EXEC set \"EndTime\" = systimestamp, \"Status\" = ?, \"Message\" = ? where ID = ?";

        try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);

            if (message == null) {
                ps.setNull(2, java.sql.Types.CLOB);
            } else {
                ps.setString(2, message);
            }

            ps.setLong(3, envioAtgExecId);

            int updated = ps.executeUpdate();

            if (updated != 1) {
                throw new java.sql.SQLException("No se actualizó exactamente una fila en P360_EXPLOIT.TB_ENVIO_ATG_EXEC. ID=" + envioAtgExecId + ", updated=" + updated);
            }
        }
    }
    
    public static long insertarExec(
            java.sql.Connection con,
            String execId,
            String status
    ) throws java.sql.SQLException {

        String sql = "insert into P360_EXPLOIT.TB_ENVIO_ATG_EXEC(\"ExecID\", \"StartTime\", \"Status\") values (?, systimestamp, ?)";

        try (java.sql.PreparedStatement ps = con.prepareStatement(sql, new String[] {"ID"})) {
            ps.setString(1, execId);
            ps.setString(2, status);

            ps.executeUpdate();

            try (java.sql.ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new java.sql.SQLException("No se obtuvo ID generado para P360_EXPLOIT.TB_ENVIO_ATG_EXEC");
                }

                return rs.getLong(1);
            }
        }
    }

    public static long insertarXml(
            java.sql.Connection con,
            long envioAtgExecId,
            String xml
    ) throws java.sql.SQLException {

        String sql =
            "insert into P360_EXPLOIT.TB_ENVIO_ATG_XML(\"pépele\", \"EnvioATGExecID\", \"CreationTime\") " +
            "values (XMLTYPE(?), ?, systimestamp)";

        java.sql.Clob xmlClob = null;

        try (java.sql.PreparedStatement ps = con.prepareStatement(sql, new String[] {"ID"})) {
            xmlClob = con.createClob();
            xmlClob.setString(1, xml);

            ps.setClob(1, xmlClob);
            ps.setLong(2, envioAtgExecId);

            ps.executeUpdate();

            try (java.sql.ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new java.sql.SQLException("No se obtuvo ID generado para P360_EXPLOIT.TB_ENVIO_ATG_XML");
                }

                return rs.getLong(1);
            }
        } finally {
            if (xmlClob != null) {
                try {
                    xmlClob.free();
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static void insertarDetalleBatch(
            java.sql.Connection con,
            long envioAtgXmlId,
            java.util.List<EnvioAtgRow> rows,
            int batchSize
    ) throws java.sql.SQLException {

        String sql = "insert into P360_EXPLOIT.TB_ENVIO_ATG_ITEMS(\"ProductID\", \"ProductSKU\", \"ArticleID\", \"ArticleSKU\", \"EnvioATGXMLID\") values (?, ?, ?, ?, ?)";

        try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            int count = 0;

            for (EnvioAtgRow row : rows) {
                ps.setString(1, row.productId);

                if (row.productSku == null) {
                    ps.setNull(2, java.sql.Types.NUMERIC);
                } else {
                    ps.setLong(2, row.productSku.longValue());
                }

                ps.setString(3, row.articleId);

                if (row.articleSku == null) {
                    ps.setNull(4, java.sql.Types.NUMERIC);
                } else {
                    ps.setLong(4, row.articleSku.longValue());
                }

                ps.setLong(5, envioAtgXmlId);

                ps.addBatch();
                count++;

                if (count % batchSize == 0) {
                    ps.executeBatch();
                    ps.clearBatch();
                }
            }

            ps.executeBatch();
            ps.clearBatch();
        }
    }

    public static long guardarXmlYDetalleFinal(
            java.sql.Connection con,
            long envioAtgExecId,
            String xml,
            java.util.List<EnvioAtgRow> rows
    ) throws java.sql.SQLException {

        boolean oldAutoCommit = con.getAutoCommit();

        try {
            con.setAutoCommit(false);

            actualizarExec(con, envioAtgExecId, "SUCCESS", "Procesado correctamente.");

            long envioAtgXmlId = insertarXml(con, envioAtgExecId, xml);

            insertarDetalleBatch(con, envioAtgXmlId, rows, 1000);

            con.commit();

            return envioAtgXmlId;
        } catch (java.sql.SQLException e) {
            con.rollback();
            throw e;
        } finally {
            con.setAutoCommit(oldAutoCommit);
        }
    }

    private static void finalizarExec(
            java.sql.Connection con,
            long envioAtgExecId,
            String status
    ) throws java.sql.SQLException {

        String sql = "update P360_EXPLOIT.TB_ENVIO_ATG_EXEC set \"EndTime\" = ?, \"Status\" = ? where ID = ?";

        try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setString(2, status);
            ps.setLong(3, envioAtgExecId);
            ps.executeUpdate();
        }
    }
}