package mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.RESTWrapper;

/**
 * Caster por Entidad + Caracteristica.
 * No usa DataCasterFileTraversor ni Value: recibe directo lo que viene de la tabla de faltantes.
 */
public class MissingDataCaster_v001 implements AutoCloseable {

    private final Map<String, String> qp = new HashMap<>();
    private final RESTWrapper rw;
    private final String entity;
    private final String attId;
    private final String dataType;
    private final int batchSize;
    private final JSONArray columns = new JSONArray();
    private final List<JSONObject> pendingRows = new ArrayList<>();
    private final PrintWriter simpleLog;
    private final PrintWriter simpleErrLog;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private int batchNumber = 0;
    private long accepted = 0;
    private long sent = 0;

    public MissingDataCaster_v001(String entity, RESTWrapper rw, String attId, String dataType, int batchSize, String logDir) throws Exception {
        this.entity = entity;
        this.rw = rw;
        this.attId = attId;
        this.dataType = dataType;
        this.batchSize = batchSize <= 0 ? 200 : batchSize;

        Files.createDirectories(Paths.get(logDir));

        columns.put(new JSONObject().put("identifier", buildColumnIdentifier(entity, attId)));
        qp.put("includeObjectsInProtocol", "false");

        String safe = processFileName(entity + "_" + attId);
        this.simpleLog = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(logDir, safe + ".log")), StandardCharsets.UTF_8), true);
        this.simpleErrLog = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(logDir, safe + ".err")), StandardCharsets.UTF_8), true);

        log("INIT datatype=" + dataType + " column=" + columns.getJSONObject(0).getString("identifier"));
    }

    private String buildColumnIdentifier(String entity, String attId) {
        return entity + "CharacteristicValueLang.Value('" + attId + "',root,\"0000.0000.RK\",'" + attId + "',-1)";
    }

    private String processFileName(String fn) {
        return fn.replace('/', '_').replace('\\', '_').replace(':', '_').replace('"', '_').replace(' ', '_');
    }

    public void addValue(String identifier, String sourceValue) {
        if (identifier == null || identifier.trim().isEmpty()) {
            log("SKIP empty identifier for att=" + attId + " value=" + sourceValue);
            return;
        }
        String castedValue = castValue(sourceValue);
        JSONObject row = new JSONObject()
                .put("object", new JSONObject().put("id", "'" + identifier.trim() + "'@1"))
                .put("values", new JSONArray().put(castedValue));
        pendingRows.add(row);
        accepted++;
        if (pendingRows.size() >= batchSize) {
            sendData();
        }
    }

    private String castValue(String value) {
        if (value == null) return "";
        String v = value.trim();
        if ("LOOKUP".equalsIgnoreCase(dataType)) {
            // Para LOOKUP se espera ID/Identifier del valor, no etiqueta. Si tu tabla guarda etiqueta, aquí va el resolver.
            return v;
        }
        return v;
    }

    public void sendData() {
        if (pendingRows.isEmpty()) return;
        batchNumber++;
        JSONArray rows = new JSONArray();
        for (JSONObject row : pendingRows) rows.put(row);
        JSONObject request = new JSONObject().put("columns", columns).put("rows", rows);
        int rowsToSend = pendingRows.size();
        try {
            rw.writeData("list", entity, null, qp, request, this::log);
            sent += rowsToSend;
            log("SENT batch=" + batchNumber + " rows=" + rowsToSend + " totalSent=" + sent + " payload=" + request.toString());
            pendingRows.clear();
        } catch (Exception e) {
            logE(e, request);
            // No se limpian rows si falla: queda reintentable mientras viva el proceso.
            throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
        }
    }

    private void log(String message) {
        simpleLog.println("[" + sdf.format(new Date()) + "] (" + attId + ", " + entity + ", pending=" + pendingRows.size() + ") " + message);
    }

    private void logE(Exception e, JSONObject request) {
        simpleErrLog.println("[" + sdf.format(new Date()) + "] (" + attId + ", " + entity + ", pending=" + pendingRows.size() + ") ERROR batch=" + batchNumber);
        simpleErrLog.println(request == null ? "payload=null" : request.toString());
        e.printStackTrace(simpleErrLog);
    }

    @Override
    public void close() {
        try { sendData(); } finally {
            simpleLog.close();
            simpleErrLog.close();
        }
    }

    public String key() { return entity + "|" + attId; }
    public long getAccepted() { return accepted; }
    public long getSent() { return sent; }
}
