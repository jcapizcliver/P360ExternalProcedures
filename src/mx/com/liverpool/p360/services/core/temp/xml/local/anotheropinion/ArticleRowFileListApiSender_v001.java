package mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;

import org.json.JSONArray;
import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ArticleRowFileListApiSender_v001 {

    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private final RESTWrapper rw = new RESTWrapper();
    private final Map<String, String> qp = new TreeMap<>();
    private final int batchSize;
    private final boolean dryRun;
    private final File logDir;
    private final Consumer<String> externalLogger;

    private PrintWriter log;
    private PrintWriter err;

    public ArticleRowFileListApiSender_v001(int batchSize, boolean dryRun, File logDir, Consumer<String> externalLogger) {
        this.batchSize = batchSize <= 0 ? 2000 : batchSize;
        this.dryRun = dryRun;
        this.logDir = logDir == null ? new File("article_row_file_sender_v001") : logDir;
        this.externalLogger = externalLogger;
        this.qp.put("includeObjectsInProtocol", "false");
//        this.rw.getRw().setBaseUrl("https://172.18.251.3:1512/rest/V2.0");
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Uso:");
            System.err.println("  java ...ArticleRowFileListApiSender_v001 <rowsDir> [batchSize] [dryRun] [logDir]");
            System.exit(2);
        }

        File rowsDir = new File(args[0]);
        int batchSize = args.length >= 2 ? Integer.parseInt(args[1]) : 2000;
        boolean dryRun = args.length >= 3 ? Boolean.parseBoolean(args[2]) : true;
        File logDir = args.length >= 4 ? new File(args[3]) : new File("article_row_file_sender_v001");

        sendAll(rowsDir, batchSize, dryRun, logDir, System.out::println);
    }

    public static void sendAll(File rowsDir, int batchSize, boolean dryRun, File logDir, Consumer<String> externalLogger) throws Exception {
        ArticleRowFileListApiSender_v001 sender = new ArticleRowFileListApiSender_v001(batchSize, dryRun, logDir, externalLogger);
        sender.sendAll(rowsDir);
    }

    public void sendAll(File rowsDir) throws Exception {
        openLogs();

        try {
            if (rowsDir == null || !rowsDir.exists() || !rowsDir.isDirectory()) {
                throw new IllegalArgumentException("rowsDir no existe o no es directorio: " + rowsDir);
            }

            log("START rowsDir=" + rowsDir.getAbsolutePath() + " batchSize=" + batchSize + " dryRun=" + dryRun);

            sendOne(new File(rowsDir, "Article_EAN.jsonl.gz"), "Article.EAN", "EAN");
            sendOne(new File(rowsDir, "Article_Color.jsonl.gz"), "ArticleExtraData.ColoursLiverpoolAtt(MX)", "COLOR");
            sendOne(new File(rowsDir, "Article_TamanoUnico.jsonl.gz"), "ArticleExtraData.TamanoUnico(MX)", "SIZE");
            sendOne(new File(rowsDir, "Article_SupplierPartNumber.jsonl.gz"), "ArticleExtraData.SupplierPartNumber(MX)", "SUPPLIER_PART_NUMBER");

            log("FINISH rowsDir=" + rowsDir.getAbsolutePath());

        } finally {
            closeLogs();
        }
    }

    private void sendOne(File file, String columnIdentifier, String name) throws Exception {
        if (!file.exists()) {
            log("SKIP missing file name=" + name + " file=" + file.getAbsolutePath());
            return;
        }

        JSONArray columns = new JSONArray().put(new JSONObject().put("identifier", columnIdentifier));
        JSONArray rows = new JSONArray();

        long lines = 0;
        long sentRows = 0;
        long batches = 0;

        log("SEND_FILE_START name=" + name + " column=" + columnIdentifier + " file=" + file.getAbsolutePath());

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file)), StandardCharsets.UTF_8))) {
            String line;

            while ((line = br.readLine()) != null) {
                lines++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                rows.put(new JSONObject(line));

                if (rows.length() >= batchSize) {
                    batches++;
                    int rowCount = rows.length();
                    sendBatch(name, columns, rows, batches);
                    sentRows += rowCount;
                    rows = new JSONArray();
                }

                if (lines % 100000 == 0) {
                    String msg = "SEND_FILE_PROGRESS name=" + name + " lines=" + lines + " sentRows=" + sentRows + " batches=" + batches;
                    log(msg);
                    System.out.println(msg);
                }
            }
        }

        if (rows.length() > 0) {
            batches++;
            int rowCount = rows.length();
            sendBatch(name, columns, rows, batches);
            sentRows += rowCount;
        }

        String done = "SEND_FILE_DONE name=" + name + " lines=" + lines + " sentRows=" + sentRows + " batches=" + batches;
        log(done);
        System.out.println(done);
    }

    private void sendBatch(String name, JSONArray columns, JSONArray rows, long batchNo) throws Exception {
        JSONObject request = new JSONObject()
                .put("columns", columns)
                .put("rows", rows);

        int rowCount = rows.length();

        if (dryRun) {
            String msg = "DRY_RUN_SEND name=" + name + " batch=" + batchNo + " rows=" + rowCount + " payload=" + request.toString();
            log(msg);
            System.out.println(msg);
        } else {
            try {
                rw.writeData("list", "Article", null, qp, request, this::log);
                String msg = "SENT name=" + name + " batch=" + batchNo + " rows=" + rowCount;
                log(msg);
                System.out.println(msg);
            } catch (Exception ex) {
                err.println("[" + SDF.format(new Date()) + "] ERROR_SEND name=" + name + " batch=" + batchNo + " rows=" + rowCount);
                err.println(request.toString());
                ex.printStackTrace(err);
                throw ex;
            }
        }
    }

    private void openLogs() throws Exception {
        logDir.mkdirs();
        log = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(logDir, "article_row_file_sender_v001.log"), true), StandardCharsets.UTF_8), true);
        err = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(logDir, "article_row_file_sender_v001.err"), true), StandardCharsets.UTF_8), true);
    }

    private void closeLogs() {
        if (log != null) log.close();
        if (err != null) err.close();
    }

    private void log(String message) {
        String line = "[" + SDF.format(new Date()) + "] " + message;
        if (log != null) log.println(line);
        if (externalLogger != null) externalLogger.accept(line);
    }
}
