package mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.p360.services.core.RESTWrapper;

/**
 * ArticleMissingFieldsFromStepStream_v001
 *
 * UNA SOLA CLASE.
 *
 * Hace todo:
 *  1) Carga 4 listas de faltantes desde P360/Oracle.
 *  2) Recorre recursivamente XMLs STEP.
 *  3) Para individuales lee datos del Product padre.
 *  4) Para variantes/items lee datos del child Product y arma object.id con child.getId().
 *  5) Acumula batches por campo.
 *  6) Manda List API cada batchSize.
 *  7) Al final manda lo sobrante.
 *
 * No usa archivos intermedios.
 * No usa hilos.
 *
 * Guard original:
 *  - Solo se aplica para root sin hijos.
 *  - Sirve para saltar "artículos" que STEP dejó sueltos a nivel raíz.
 *  - NO se aplica a children válidos, porque su ParentID normalmente es el ID del producto padre.
 *
 * Campos:
 *  - Article.EAN
 *      XML: MainBarCode, fallback MainBarCodeS4H
 *
 *  - ArticleExtraData.ColoursLiverpoolAtt(MX)
 *      XML: ColoursLiverpoolAtt, usa ID primero
 *
 *  - ArticleExtraData.TamanoUnico(MX)
 *      XML: TamanoUnico, usa ID primero
 *
 *  - ArticleExtraData.SupplierPartNumber(MX)
 *      XML: SupplierPartNumber, usa texto primero
 *
 * Args:
 *  0 jdbcUrl
 *  1 dbUser
 *  2 dbPassword
 *  3 xmlRootDir
 *  4 batchSize opcional, default 2000
 *  5 dryRun opcional, default true
 *  6 logDir opcional, default article_missing_stream_v001
 *  7 maxDryRunPayloads opcional, default 0 sin límite
 *  8 maxFiles opcional, default 0 sin límite
 */
public class ArticleMissingFieldsFromStepStream_v001 {

    private final RESTWrapper rw = new RESTWrapper();
    private final Map<String, String> qp = new TreeMap<>();
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String[] missingEan = new String[0];
    private String[] missingColor = new String[0];
    private String[] missingSize = new String[0];
    private String[] missingSupplierPartNumber = new String[0];

    private final Set<String> sentEan = new HashSet<>();
    private final Set<String> sentColor = new HashSet<>();
    private final Set<String> sentSize = new HashSet<>();
    private final Set<String> sentSupplierPartNumber = new HashSet<>();

    private Batcher eanBatcher;
    private Batcher colorBatcher;
    private Batcher sizeBatcher;
    private Batcher supplierPartNumberBatcher;

    private PrintWriter log;
    private PrintWriter err;

    private boolean dryRun;
    private long maxDryRunPayloads;
    private long dryRunPayloadsPrinted = 0;
    private long maxFiles;

    private long filesSeen = 0;
    private long filesParsed = 0;
    private long rootProductsSeen = 0;
    private long articleCandidatesSeen = 0;
    private long individualFromParent = 0;
    private long variantsFromChildren = 0;
    private long skippedByOriginalParentGuard = 0;

    private long eanHits = 0;
    private long colorHits = 0;
    private long sizeHits = 0;
    private long supplierPartNumberHits = 0;

    private static final StopDryRun STOP_DRY_RUN = new StopDryRun();

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Uso:");
            System.err.println("  java ...ArticleMissingFieldsFromStepStream_v001 <jdbcUrl> <dbUser> <dbPassword> <xmlRootDir> [batchSize] [dryRun] [logDir] [maxDryRunPayloads] [maxFiles]");
            System.err.println("");
            System.err.println("Dry-run payload inmediato:");
            System.err.println("  java ...ArticleMissingFieldsFromStepStream_v001 \"$ORACLE_JDBC_URL\" \"$ORACLE_JDBC_USER\" \"$ORACLE_JDBC_PASSWORD\" /u01/stage/STEP 1 true article_missing_stream_v001_dry 20 0");
            System.err.println("");
            System.err.println("Real:");
            System.err.println("  nohup java ...ArticleMissingFieldsFromStepStream_v001 \"$ORACLE_JDBC_URL\" \"$ORACLE_JDBC_USER\" \"$ORACLE_JDBC_PASSWORD\" /u01/stage/STEP 2000 false article_missing_stream_v001_real 0 0 > article_missing_stream_v001_real.out 2>&1 &");
            System.exit(2);
        }

        long init = System.currentTimeMillis();

        ArticleMissingFieldsFromStepStream_v001 app = new ArticleMissingFieldsFromStepStream_v001();
        app.run(
                args[0],
                args[1],
                args[2],
                args[3],
                args.length >= 5 ? Integer.parseInt(args[4]) : 2000,
                args.length >= 6 ? Boolean.parseBoolean(args[5]) : true,
                args.length >= 7 ? args[6] : "article_missing_stream_v001",
                args.length >= 8 ? Long.parseLong(args[7]) : 0L,
                args.length >= 9 ? Long.parseLong(args[8]) : 0L
        );

        System.out.println("DONE totalMillis=" + (System.currentTimeMillis() - init));
    }

    private void run(String jdbcUrl, String dbUser, String dbPassword, String xmlRootDir, int batchSize, boolean dryRun, String logDir, long maxDryRunPayloads, long maxFiles) throws Exception {
        this.dryRun = dryRun;
        this.maxDryRunPayloads = maxDryRunPayloads;
        this.maxFiles = maxFiles;

        File logDirectory = new File(logDir);
        logDirectory.mkdirs();

        log = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(logDirectory, "article_missing_stream_v001.log"), true), StandardCharsets.UTF_8), true);
        err = new PrintWriter(new OutputStreamWriter(new FileOutputStream(new File(logDirectory, "article_missing_stream_v001.err"), true), StandardCharsets.UTF_8), true);

        qp.put("includeObjectsInProtocol", "false");
        rw.getRw().setBaseUrl("https://172.18.251.3:1512/rest/V2.0");

        try (Connection cn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            setModule(cn);
            missingEan = loadList(cn, SQL_MISSING_EAN, "missingEan");
            missingColor = loadList(cn, SQL_MISSING_COLOR, "missingColor");
            missingSize = loadList(cn, SQL_MISSING_SIZE, "missingSize");
            missingSupplierPartNumber = loadList(cn, SQL_MISSING_SUPPLIER_PART_NUMBER, "missingSupplierPartNumber");
        }

        eanBatcher = new Batcher("Article.EAN", batchSize, "EAN");
        colorBatcher = new Batcher("ArticleExtraData.ColoursLiverpoolAtt(MX)", batchSize, "COLOR");
        sizeBatcher = new Batcher("ArticleExtraData.TamanoUnico(MX)", batchSize, "SIZE");
        supplierPartNumberBatcher = new Batcher("ArticleExtraData.SupplierPartNumber(MX)", batchSize, "SUPPLIER_PART_NUMBER");

        log("START xmlRootDir=" + xmlRootDir
                + " batchSize=" + batchSize
                + " dryRun=" + dryRun
                + " maxDryRunPayloads=" + maxDryRunPayloads
                + " maxFiles=" + maxFiles);

        try {
            runOverDirectories(new File(xmlRootDir));
            flushAll();
        } catch (StopDryRun stop) {
            log("STOP dryRun max payloads reached: " + dryRunPayloadsPrinted);
            System.out.println("STOP dryRun max payloads reached: " + dryRunPayloadsPrinted);
        } finally {
            log("FINISH filesSeen=" + filesSeen
                    + " filesParsed=" + filesParsed
                    + " rootProductsSeen=" + rootProductsSeen
                    + " articleCandidatesSeen=" + articleCandidatesSeen
                    + " individualFromParent=" + individualFromParent
                    + " variantsFromChildren=" + variantsFromChildren
                    + " skippedByOriginalParentGuard=" + skippedByOriginalParentGuard
                    + " eanHits=" + eanHits
                    + " colorHits=" + colorHits
                    + " sizeHits=" + sizeHits
                    + " supplierPartNumberHits=" + supplierPartNumberHits
                    + " sentEan=" + sentEan.size()
                    + " sentColor=" + sentColor.size()
                    + " sentSize=" + sentSize.size()
                    + " sentSupplierPartNumber=" + sentSupplierPartNumber.size()
                    + " eanBatches=" + (eanBatcher == null ? 0 : eanBatcher.batches)
                    + " colorBatches=" + (colorBatcher == null ? 0 : colorBatcher.batches)
                    + " sizeBatches=" + (sizeBatcher == null ? 0 : sizeBatcher.batches)
                    + " supplierPartNumberBatches=" + (supplierPartNumberBatcher == null ? 0 : supplierPartNumberBatcher.batches));

            if (log != null) log.close();
            if (err != null) err.close();
        }
    }

    private void flushAll() {
        eanBatcher.flush();
        colorBatcher.flush();
        sizeBatcher.flush();
        supplierPartNumberBatcher.flush();
    }

    private void setModule(Connection cn) {
        try (java.sql.Statement st = cn.createStatement()) {
            st.execute("begin dbms_application_info.set_module(module_name => 'ArticleMissingStream_v001', action_name => 'load_missing_lists'); end;");
        } catch (Exception ignored) {
        }
    }

    private String[] loadList(Connection cn, String sql, String name) throws Exception {
        LinkedHashSet<String> set = new LinkedHashSet<>();

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setFetchSize(5000);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString(1);
                    if (id != null && !id.trim().isEmpty()) {
                        set.add(id.trim());
                    }
                }
            }
        }

        String[] arr = set.toArray(new String[0]);
        Arrays.sort(arr);

        log("Loaded " + name + "=" + arr.length);
        System.out.println("Loaded " + name + "=" + arr.length);

        return arr;
    }

    private void runOverDirectories(File dir) {
        if (dir == null || !dir.exists()) {
            log("Directory does not exist: " + dir);
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            log("Cannot list directory: " + dir.getAbsolutePath());
            return;
        }

        for (File f : files) {
            if (f.isDirectory()) {
                runOverDirectories(f);
            } else if (f.isFile()) {
                if (maxFiles > 0 && filesSeen >= maxFiles) {
                    return;
                }

                filesSeen++;
                processFile(f);
            }
        }
    }

    private void processFile(File file) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);

            try {
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (Exception ignored) {
            }

            SAXParser parser = factory.newSAXParser();
            Handler handler = new Handler();

            parser.parse(file, handler);

            for (Product root : handler.finishedRoots) {
                processRootProduct(root);
            }

            filesParsed++;

            if (filesParsed % 100 == 0) {
                String msg = "PROGRESS filesParsed=" + filesParsed
                        + " filesSeen=" + filesSeen
                        + " rootProductsSeen=" + rootProductsSeen
                        + " articleCandidatesSeen=" + articleCandidatesSeen
                        + " individualFromParent=" + individualFromParent
                        + " variantsFromChildren=" + variantsFromChildren
                        + " skippedByOriginalParentGuard=" + skippedByOriginalParentGuard
                        + " eanHits=" + eanHits
                        + " colorHits=" + colorHits
                        + " sizeHits=" + sizeHits
                        + " supplierPartNumberHits=" + supplierPartNumberHits
                        + " eanBatches=" + eanBatcher.batches
                        + " colorBatches=" + colorBatcher.batches
                        + " sizeBatches=" + sizeBatcher.batches
                        + " supplierPartNumberBatches=" + supplierPartNumberBatcher.batches;
                log(msg);
                System.out.println(msg);
            }

        } catch (StopDryRun stop) {
            throw stop;
        } catch (Exception ex) {
            err.println("[" + sdf.format(new Date()) + "] Problem processing file: " + file.getAbsolutePath());
            ex.printStackTrace(err);
        }
    }

    private class Handler extends DefaultHandler {
        private final java.util.LinkedList<Product> stack = new java.util.LinkedList<>();
        private final java.util.List<Product> finishedRoots = new java.util.ArrayList<>();

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String name = localName != null && !localName.isEmpty() ? localName : qName;

            if ("Product".equals(name)) {
                String id = attributes.getValue("ID");
                String parentId = attributes.getValue("ParentID");
                String userTypeId = attributes.getValue("UserTypeID");

                if (parentId == null && !stack.isEmpty()) {
                    parentId = stack.getLast().id;
                }

                stack.addLast(new Product(id, parentId, userTypeId));
                return;
            }

            if (!stack.isEmpty() && "Value".equals(name)) {
                Product p = stack.getLast();
                String attributeId = attributes.getValue("AttributeID");
                String valueId = attributes.getValue("ID");
                String unitId = attributes.getValue("UnitID");
                p.workingValue = new Value(attributeId, valueId, unitId);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (stack.isEmpty()) {
                return;
            }

            Product p = stack.getLast();
            if (p.workingValue != null) {
                p.workingValue.text.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String name = localName != null && !localName.isEmpty() ? localName : qName;

            if (stack.isEmpty()) {
                return;
            }

            Product p = stack.getLast();

            if ("Value".equals(name)) {
                if (p.workingValue != null && p.workingValue.attributeId != null) {
                    p.values.put(p.workingValue.attributeId, p.workingValue);
                }
                p.workingValue = null;
                return;
            }

            if ("Product".equals(name)) {
                stack.removeLast();

                if (!stack.isEmpty()) {
                    stack.getLast().children.add(p);
                } else {
                    finishedRoots.add(p);
                }
            }
        }
    }

    private static class Product {
        final String id;
        final String parentId;
        final String userTypeId;
        final Map<String, Value> values = new TreeMap<>();
        final java.util.List<Product> children = new java.util.ArrayList<>();
        Value workingValue;

        Product(String id, String parentId, String userTypeId) {
            this.id = id;
            this.parentId = parentId;
            this.userTypeId = userTypeId;
        }
    }

    private static class Value {
        final String attributeId;
        final String id;
        final String unitId;
        final StringBuilder text = new StringBuilder();

        Value(String attributeId, String id, String unitId) {
            this.attributeId = attributeId;
            this.id = id;
            this.unitId = unitId;
        }
    }

    private void processRootProduct(Product product) {
        rootProductsSeen++;

        if (product.children == null || product.children.isEmpty()) {
            if (shouldSkipLooseRootArticle(product.parentId)) {
                skippedByOriginalParentGuard++;
                log("SKIP_LOOSE_ROOT_ARTICLE articleId=" + product.id + " parentId=" + product.parentId);
                return;
            }

            individualFromParent++;
            processArticleCandidate(product.id, product.values, "PARENT_INDIVIDUAL", product.parentId);
        } else {
            for (Product child : product.children) {
                variantsFromChildren++;
                processArticleCandidate(child.id, child.values, "CHILD_ITEM", product.id);
            }
        }
    }

    private boolean shouldSkipLooseRootArticle(String parentId) {
        return parentId != null && parentId.matches("^(S?[0-9]+)");
    }

    private void processArticleCandidate(String articleId, Map<String, Value> values, String sourceMode, String parentId) {
        articleCandidatesSeen++;

        String id = normalize(articleId);
        if (id == null) {
            return;
        }

        String ean = firstNonBlank(
                textThenId(values.get("MainBarCode")),
                textThenId(values.get("MainBarCodeS4H"))
        );

        String color = idThenText(values.get("ColoursLiverpoolAtt"));
        String size = idThenText(values.get("TamanoUnico"));
        String supplierPartNumber = textThenId(values.get("SupplierPartNumber"));

        if (shouldSend(id, missingEan, sentEan, ean)) {
            eanBatcher.add(id, ean, sourceMode, parentId);
            sentEan.add(id);
            eanHits++;
        }

        if (shouldSend(id, missingColor, sentColor, color)) {
            colorBatcher.add(id, color, sourceMode, parentId);
            sentColor.add(id);
            colorHits++;
        }

        if (shouldSend(id, missingSize, sentSize, size)) {
            sizeBatcher.add(id, size, sourceMode, parentId);
            sentSize.add(id);
            sizeHits++;
        }

        if (shouldSend(id, missingSupplierPartNumber, sentSupplierPartNumber, supplierPartNumber)) {
            supplierPartNumberBatcher.add(id, supplierPartNumber, sourceMode, parentId);
            sentSupplierPartNumber.add(id);
            supplierPartNumberHits++;
        }
    }

    private boolean shouldSend(String id, String[] target, Set<String> alreadySent, String value) {
        return id != null
                && value != null
                && !value.trim().isEmpty()
                && !alreadySent.contains(id)
                && Arrays.binarySearch(target, id) >= 0;
    }

    private String idThenText(Value v) {
        if (v == null) return null;

        String id = normalize(v.id);
        if (id != null) return id;

        return normalize(v.text.toString());
    }

    private String textThenId(Value v) {
        if (v == null) return null;

        String text = normalize(v.text.toString());
        if (text != null) return text;

        return normalize(v.id);
    }

    private String firstNonBlank(String a, String b) {
        return a != null && !a.isEmpty() ? a : b;
    }

    private String normalize(String s) {
        if (s == null) return null;

        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private class Batcher {
        final String columnIdentifier;
        final int batchSize;
        final String name;
        final JSONArray rows = new JSONArray();

        long batches = 0;
        long accepted = 0;

        Batcher(String columnIdentifier, int batchSize, String name) {
            this.columnIdentifier = columnIdentifier;
            this.batchSize = batchSize <= 0 ? 2000 : batchSize;
            this.name = name;
        }

        void add(String identifier, String value, String sourceMode, String parentId) {
            rows.put(new JSONObject()
                    .put("object", new JSONObject().put("id", "'" + identifier + "'@1"))
                    .put("values", new JSONArray().put(value)));

            accepted++;

            log("HIT " + name
                    + " articleId=" + identifier
                    + " parentId=" + parentId
                    + " sourceMode=" + sourceMode
                    + " value=" + value
                    + " pendingRows=" + rows.length());

            if (rows.length() >= batchSize) {
                flush();
            }
        }

        void flush() {
            if (rows.length() == 0) {
                return;
            }

            batches++;

            JSONArray columns = new JSONArray()
                    .put(new JSONObject().put("identifier", columnIdentifier));

            JSONObject request = new JSONObject()
                    .put("columns", columns)
                    .put("rows", rows);

            int count = rows.length();

            try {
                if (dryRun) {
                    dryRunPayloadsPrinted++;

                    String line = "DRY_RUN_PAYLOAD " + name
                            + " batch=" + batches
                            + " rows=" + count
                            + " payload=" + request.toString();

                    log(line);
                    System.out.println(line);

                    if (maxDryRunPayloads > 0 && dryRunPayloadsPrinted >= maxDryRunPayloads) {
                        throw STOP_DRY_RUN;
                    }
                } else {
                    rw.writeData("list", "Article", null, qp, request, ArticleMissingFieldsFromStepStream_v001.this::log);

                    String line = "SENT " + name
                            + " batch=" + batches
                            + " rows=" + count
                            + " accepted=" + accepted;

                    log(line);
                    System.out.println(line);
                }
            } catch (StopDryRun stop) {
                throw stop;
            } catch (Exception ex) {
                err.println("[" + sdf.format(new Date()) + "] ERROR sending " + name + " batch=" + batches + " rows=" + count);
                err.println(request.toString());
                ex.printStackTrace(err);
                throw ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
            } finally {
                while (rows.length() > 0) {
                    rows.remove(0);
                }
            }
        }
    }

    private static class StopDryRun extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private void log(String message) {
        String line = "[" + sdf.format(new Date()) + "] " + message;

        if (log != null) {
            log.println(line);
        }
    }

    private static final String ACTIVE_ARTICLE_FILTER =
            " ar.\"EntityID\" = 1000 " +
            " and ar.\"RevisionID\" = 1 " +
            " and ar.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' " +
            " and length(ar.\"Identifier\") < 16 ";

    private static final String SQL_MISSING_EAN =
            "select ar.\"Identifier\" " +
            "from PIM_MASTER.\"ArticleRevision\" ar " +
            "join PIM_MASTER.\"ArticleDetail\" ad on ad.\"ArticleRevisionID\" = ar.\"ID\" " +
            "where " + ACTIVE_ARTICLE_FILTER +
            " and ad.\"EAN\" is null " +
            "order by ar.\"Identifier\"";

    private static final String SQL_MISSING_COLOR =
            "select distinct ar.\"Identifier\" " +
            "from PIM_MASTER.\"ArticleRevision\" ar " +
            "left join PIM_MASTER.\"ArticleDomain\" dom on dom.\"ArticleRevisionID\" = ar.\"ID\" " +
            "where " + ACTIVE_ARTICLE_FILTER +
            " and dom.\"Res_Int_02\" is null " +
            "order by ar.\"Identifier\"";

	private static final String SQL_MISSING_SIZE =
            "select distinct ar.\"Identifier\" " +
            "from PIM_MASTER.\"ArticleRevision\" ar " +
            "left join PIM_MASTER.\"ArticleDomain\" dom on dom.\"ArticleRevisionID\" = ar.\"ID\" " +
            "left join PIM_MAIN.\"LookupValueRevision\" lvr on lvr.\"LookupValueID\" = dom.\"Res_Int_01\" " +
            " and lvr.\"RevisionID\" = 1 " +
            " and lvr.\"DeletionTimestamp\" = timestamp '9999-12-31 00:00:00.0' " +
            "where " + ACTIVE_ARTICLE_FILTER +
            " and (dom.\"Res_Int_01\" is null or lvr.\"Code\" = 'SIN TAMAÑO') " +
            "order by ar.\"Identifier\"";

    private static final String SQL_MISSING_SUPPLIER_PART_NUMBER =
            "select distinct ar.\"Identifier\" " +
            "from PIM_MASTER.\"ArticleRevision\" ar " +
            "left join PIM_MASTER.\"ArticleDomain\" dom on dom.\"ArticleRevisionID\" = ar.\"ID\" " +
            "where " + ACTIVE_ARTICLE_FILTER +
            " and dom.\"Res_Text250_01\" is null " +
            "order by ar.\"Identifier\"";
}
