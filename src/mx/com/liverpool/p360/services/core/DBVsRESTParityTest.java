package mx.com.liverpool.p360.services.core;

/**
 * Read-only parity harness for DBAccessDataStub vs RESTAccessDataStub.
 *
 * It deliberately executes both implementations with the same input and emits:
 *   PASS  - same normalized result
 *   DIFF  - both returned but values differ
 *   ERROR - one side threw an exception
 *
 * Arrays and Sets are compared order-insensitively. JSONObject property order is
 * ignored. Scalar String/Number values are compared by their textual value so a
 * JDBC numeric 1007 and REST string "1007" do not produce a false difference.
 */
public class DBVsRESTParityTest {

    public static final class TestPlan {
        public final java.util.List<String> products = new java.util.ArrayList<>();
        public final java.util.List<String> articles = new java.util.ArrayList<>();
        public final java.util.List<String> skus = new java.util.ArrayList<>();
        public final java.util.List<String> productCharacteristics = new java.util.ArrayList<>();
        public final java.util.List<String> articleCharacteristics = new java.util.ArrayList<>();
        public final java.util.List<LookupCase> lookups = new java.util.ArrayList<>();

        public TestPlan product(String... values) {
            add(products, values);
            return this;
        }

        public TestPlan article(String... values) {
            add(articles, values);
            return this;
        }

        public TestPlan sku(String... values) {
            add(skus, values);
            return this;
        }

        public TestPlan productCharacteristic(String... values) {
            add(productCharacteristics, values);
            return this;
        }

        public TestPlan articleCharacteristic(String... values) {
            add(articleCharacteristics, values);
            return this;
        }

        public TestPlan lookup(String identifier, int languageID, boolean onlyActive) {
            lookups.add(new LookupCase(identifier, languageID, onlyActive));
            return this;
        }

        private void add(java.util.List<String> target, String... values) {
            if (values == null) return;
            for (String value : values) {
                if (value != null && !value.isBlank() && !target.contains(value.trim())) {
                    target.add(value.trim());
                }
            }
        }
    }

    public static final class LookupCase {
        public final String identifier;
        public final int languageID;
        public final boolean onlyActive;

        LookupCase(String identifier, int languageID, boolean onlyActive) {
            this.identifier = identifier;
            this.languageID = languageID;
            this.onlyActive = onlyActive;
        }
    }

    public static final class Summary {
        public int pass;
        public int diff;
        public int error;
        public long dbMillis;
        public long restMillis;
        public final org.json.JSONArray cases = new org.json.JSONArray();

        public boolean isClean() {
            return diff == 0 && error == 0;
        }

        public org.json.JSONObject toJson() {
            return new org.json.JSONObject()
                    .put("pass", pass)
                    .put("diff", diff)
                    .put("error", error)
                    .put("dbMillis", dbMillis)
                    .put("restMillis", restMillis)
                    .put("clean", isClean())
                    .put("cases", cases);
        }
    }

    @FunctionalInterface
    private interface CheckedSupplier {
        Object get() throws Exception;
    }

    private final DBAccessDataStub db;
    private final RESTAccessDataStub rest;
    private final ELog log;

    public DBVsRESTParityTest(ELog log) {
        this(log, new DBAccessDataStub(log), new RESTAccessDataStub(log));
    }

    public DBVsRESTParityTest(
            ELog log,
            DBAccessDataStub db,
            RESTAccessDataStub rest) {
        this.log = log;
        this.db = db;
        this.rest = rest;
    }

    public Summary run(TestPlan plan) {
        return run(plan, null);
    }

    /**
     * @param outputCsv optional CSV file; null means console/log only.
     */
    public Summary run(TestPlan plan, java.nio.file.Path outputCsv) {
        if (plan == null) {
            throw new IllegalArgumentException("TestPlan is required");
        }

        Summary summary = new Summary();
        java.util.List<org.json.JSONObject> emitted = new java.util.ArrayList<>();

        String[] productChars = plan.productCharacteristics.toArray(new String[0]);
        String[] articleChars = plan.articleCharacteristics.toArray(new String[0]);

        for (String product : plan.products) {
            compare(summary, emitted, "getProductData", product,
                    () -> db.getProductData(product),
                    () -> rest.getProductData(product));

            compare(summary, emitted, "getProductPrimaryTemplate", product,
                    () -> db.getProductPrimaryTemplate(product),
                    () -> rest.getProductPrimaryTemplate(product));

            compare(summary, emitted, "getProductStatusData", product,
                    () -> db.getProductStatusData(product),
                    () -> rest.getProductStatusData(product));

            compare(summary, emitted, "getProductVariants", product,
                    () -> db.getProductVariants(product),
                    () -> rest.getProductVariants(product));

            if (productChars.length > 0) {
                compare(summary, emitted, "getProductExtraData", product,
                        () -> db.getProductExtraData(product, productChars),
                        () -> rest.getProductExtraData(product, productChars));
            }
        }

        for (String article : plan.articles) {
            compare(summary, emitted, "getArticleData", article,
                    () -> db.getArticleData(article),
                    () -> rest.getArticleData(article));

            compare(summary, emitted, "getProductByVariant", article,
                    () -> db.getProductByVariant(article),
                    () -> rest.getProductByVariant(article));

            compare(summary, emitted, "getProductCurrentStatusByArticleIdentifier", article,
                    () -> db.getProductCurrentStatusByArticleIdentifier(article),
                    () -> rest.getProductCurrentStatusByArticleIdentifier(article));

            if (articleChars.length > 0) {
                compare(summary, emitted, "getArticleExtraData", article,
                        () -> db.getArticleExtraData(article, articleChars),
                        () -> rest.getArticleExtraData(article, articleChars));
            }
        }

        for (String sku : plan.skus) {
            compare(summary, emitted, "getSkuProductNo", sku,
                    () -> db.getSkuProductNo(sku),
                    () -> rest.getSkuProductNo(sku));

            compare(summary, emitted, "getSkuSupplierAid", sku,
                    () -> db.getSkuSupplierAid(sku),
                    () -> rest.getSkuSupplierAid(sku));
        }

        if (!plan.products.isEmpty() && !plan.productCharacteristics.isEmpty()) {
            compare(summary, emitted, "getProductCharacteristicValues", "bulk-products",
                    () -> db.getProductCharacteristicValues(
                            plan.products,
                            plan.productCharacteristics),
                    () -> rest.getProductCharacteristicValues(
                            plan.products,
                            plan.productCharacteristics));
        }

        if (!plan.products.isEmpty()) {
            compare(summary, emitted, "getProductData(Collection)", "bulk-products",
                    () -> db.getProductData(plan.products),
                    () -> rest.getProductData(plan.products));

            compare(summary, emitted, "getProductVariants(Collection)", "bulk-products",
                    () -> db.getProductVariants(plan.products),
                    () -> rest.getProductVariants(plan.products));
        }

        if (!plan.articles.isEmpty()) {
            compare(summary, emitted, "getArticleData(Collection)", "bulk-articles",
                    () -> db.getArticleData(plan.articles),
                    () -> rest.getArticleData(plan.articles));
        }

        if (!plan.skus.isEmpty()) {
            compare(summary, emitted, "getProductsBySKUs", "bulk-skus",
                    () -> db.getProductsBySKUs(plan.skus),
                    () -> rest.getProductsBySKUs(plan.skus));

            compare(summary, emitted, "getArticlesBySKUs", "bulk-skus",
                    () -> db.getArticlesBySKUs(plan.skus),
                    () -> rest.getArticlesBySKUs(plan.skus));
        }

        for (LookupCase lookup : plan.lookups) {
            String key = lookup.identifier + "/lang=" + lookup.languageID
                    + "/active=" + lookup.onlyActive;
            compare(summary, emitted, "getLookupValueCodeNameMap", key,
                    () -> db.getLookupValueCodeNameMap(
                            lookup.identifier,
                            lookup.languageID,
                            lookup.onlyActive),
                    () -> rest.getLookupValueCodeNameMap(
                            lookup.identifier,
                            lookup.languageID,
                            lookup.onlyActive));
        }

        if (outputCsv != null) {
            writeCsv(outputCsv, emitted);
        }

        log("PARITY SUMMARY: PASS=" + summary.pass
                + " DIFF=" + summary.diff
                + " ERROR=" + summary.error
                + " DB=" + summary.dbMillis + " ms"
                + " REST=" + summary.restMillis + " ms");

        return summary;
    }

    private void compare(
            Summary summary,
            java.util.List<org.json.JSONObject> emitted,
            String method,
            String key,
            CheckedSupplier dbSupplier,
            CheckedSupplier restSupplier) {

        Timed dbValue = invoke(dbSupplier);
        Timed restValue = invoke(restSupplier);
        summary.dbMillis += dbValue.millis;
        summary.restMillis += restValue.millis;

        org.json.JSONObject row = new org.json.JSONObject()
                .put("method", method)
                .put("key", key)
                .put("dbMillis", dbValue.millis)
                .put("restMillis", restValue.millis);

        if (dbValue.error != null || restValue.error != null) {
            summary.error++;
            row.put("status", "ERROR")
               .put("dbError", errorString(dbValue.error))
               .put("restError", errorString(restValue.error));
            emit(row);
            emitted.add(row);
            summary.cases.put(row);
            return;
        }

        Object left = normalize(dbValue.value);
        Object right = normalize(restValue.value);
        java.util.List<String> differences = new java.util.ArrayList<>();
        diff("$", left, right, differences, 200);

        if (differences.isEmpty()) {
            summary.pass++;
            row.put("status", "PASS");
        } else {
            summary.diff++;
            org.json.JSONArray d = new org.json.JSONArray();
            for (String difference : differences) {
                d.put(difference);
            }
            row.put("status", "DIFF")
               .put("differences", d)
               .put("db", printable(left))
               .put("rest", printable(right));
        }

        emit(row);
        emitted.add(row);
        summary.cases.put(row);
    }

    private Timed invoke(CheckedSupplier supplier) {
        long start = System.nanoTime();
        try {
            return new Timed(
                    supplier.get(),
                    null,
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        } catch (Throwable e) {
            return new Timed(
                    null,
                    e,
                    java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        }
    }

    private static final class Timed {
        final Object value;
        final Throwable error;
        final long millis;

        Timed(Object value, Throwable error, long millis) {
            this.value = value;
            this.error = error;
            this.millis = millis;
        }
    }

    private Object normalize(Object value) {
        if (value == null || value == org.json.JSONObject.NULL) {
            return null;
        }
        if (value instanceof org.json.JSONObject) {
            org.json.JSONObject object = (org.json.JSONObject) value;
            java.util.Map<String, Object> map = new java.util.TreeMap<>();
            for (Object key : object.keySet()) {
                map.put((String)key, normalize(object.opt((String)key)));
            }
            return map;
        }
        if (value instanceof org.json.JSONArray) {
            org.json.JSONArray array = (org.json.JSONArray) value;
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                list.add(normalize(array.opt(i)));
            }
            return sortNormalized(list);
        }
        if (value instanceof java.util.Map<?, ?>) {
            java.util.Map<String, Object> map = new java.util.TreeMap<>();
            for (java.util.Map.Entry<?, ?> entry : ((java.util.Map<?, ?>) value).entrySet()) {
                map.put(String.valueOf(entry.getKey()), normalize(entry.getValue()));
            }
            return map;
        }
        if (value instanceof java.util.Collection<?>) {
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (Object item : (java.util.Collection<?>) value) {
                list.add(normalize(item));
            }
            return sortNormalized(list);
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (int i = 0; i < length; i++) {
                list.add(normalize(java.lang.reflect.Array.get(value, i)));
            }
            return sortNormalized(list);
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Character) {
            return String.valueOf(value);
        }
        return String.valueOf(value);
    }

    private java.util.List<Object> sortNormalized(java.util.List<Object> values) {
        values.sort(java.util.Comparator.comparing(this::canonical));
        return values;
    }

    @SuppressWarnings("unchecked")
    private void diff(
            String path,
            Object left,
            Object right,
            java.util.List<String> differences,
            int maxDifferences) {

        if (differences.size() >= maxDifferences) return;
        if (java.util.Objects.equals(left, right)) return;

        if (left == null || right == null) {
            differences.add(path + ": DB=" + printable(left) + " REST=" + printable(right));
            return;
        }

        if (left instanceof java.util.Map && right instanceof java.util.Map) {
            java.util.Map<String, Object> l = (java.util.Map<String, Object>) left;
            java.util.Map<String, Object> r = (java.util.Map<String, Object>) right;
            java.util.Set<String> keys = new java.util.TreeSet<>();
            keys.addAll(l.keySet());
            keys.addAll(r.keySet());
            for (String key : keys) {
                diff(path + "." + key, l.get(key), r.get(key), differences, maxDifferences);
                if (differences.size() >= maxDifferences) return;
            }
            return;
        }

        if (left instanceof java.util.List && right instanceof java.util.List) {
            java.util.List<Object> l = (java.util.List<Object>) left;
            java.util.List<Object> r = (java.util.List<Object>) right;
            int max = Math.max(l.size(), r.size());
            for (int i = 0; i < max; i++) {
                Object lv = i < l.size() ? l.get(i) : null;
                Object rv = i < r.size() ? r.get(i) : null;
                diff(path + "[" + i + "]", lv, rv, differences, maxDifferences);
                if (differences.size() >= maxDifferences) return;
            }
            return;
        }

        differences.add(path + ": DB=" + printable(left) + " REST=" + printable(right));
    }

    private String canonical(Object value) {
        if (value == null) return "null";
        if (value instanceof java.util.Map<?, ?>) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (java.util.Map.Entry<?, ?> e : ((java.util.Map<?, ?>) value).entrySet()) {
                if (!first) sb.append(',');
                first = false;
                sb.append(e.getKey()).append(':').append(canonical(e.getValue()));
            }
            return sb.append('}').toString();
        }
        if (value instanceof java.util.List<?>) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : (java.util.List<?>) value) {
                if (!first) sb.append(',');
                first = false;
                sb.append(canonical(item));
            }
            return sb.append(']').toString();
        }
        return String.valueOf(value);
    }

    private String printable(Object value) {
        String canonical = canonical(value);
        return canonical.length() <= 4000 ? canonical : canonical.substring(0, 4000) + "...<truncated>";
    }

    private String errorString(Throwable e) {
        if (e == null) return "";
        return e.getClass().getName() + ": " + java.util.Objects.toString(e.getMessage(), "");
    }

    private void emit(org.json.JSONObject row) {
        log(row.toString());
    }

    private void log(String message) {
        if (log != null) {
            log.log(message);
        } else {
            System.out.println(message);
        }
    }

    private void writeCsv(
            java.nio.file.Path path,
            java.util.List<org.json.JSONObject> rows) {

        try {
            java.nio.file.Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                java.nio.file.Files.createDirectories(parent);
            }
            try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                    path,
                    java.nio.charset.StandardCharsets.UTF_8)) {

                writer.write("method;key;status;dbMillis;restMillis;differences;dbError;restError");
                writer.newLine();
                for (org.json.JSONObject row : rows) {
                    writer.write(csv(row.optString("method", "")) + ";"
                            + csv(row.optString("key", "")) + ";"
                            + csv(row.optString("status", "")) + ";"
                            + row.optLong("dbMillis", 0L) + ";"
                            + row.optLong("restMillis", 0L) + ";"
                            + csv(row.has("differences") ? row.optJSONArray("differences").toString() : "") + ";"
                            + csv(row.optString("dbError", "")) + ";"
                            + csv(row.optString("restError", "")));
                    writer.newLine();
                }
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Could not write parity CSV: " + path, e);
        }
    }

    private String csv(String value) {
        String safe = value == null ? "" : value;
        if (safe.indexOf(';') >= 0 || safe.indexOf('"') >= 0 || safe.indexOf('\n') >= 0 || safe.indexOf('\r') >= 0) {
            return '"' + safe.replace("\"", "\"\"") + '"';
        }
        return safe;
    }

    /**
     * Example intended to be called from any existing component that already
     * owns an ELog instance.
     */
    public static Summary example(ELog log) {
        TestPlan plan = new TestPlan()
                .productCharacteristic(
                        "Business",
                        "AssignTakeNoTake",
                        "FotoTomadaLiverpool")
                .articleCharacteristic(
                        "AssignTakeNoTake");

        // Add real identifiers at runtime, e.g.:
        // plan.product("LVP...").article("...").sku("12345678");

        try (DBAccessDataStub db = new DBAccessDataStub(log);
             RESTAccessDataStub rest = new RESTAccessDataStub(log)) {
            return new DBVsRESTParityTest(log, db, rest).run(plan);
        }
    }
}
