package mx.com.liverpool.p360.services.core.temp.product2g.maintenance4;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public final class Product2GProductNameUpdater {

    private static final String TARGET_COLUMN = "Product2GLang.ProductName(es)";

    /*
     * AJUSTA ESTO SI TU ENDPOINT REAL ES OTRO.
     * RESTWrapper arma:
     * "/" + api + "/" + entity + (subEntity vacío ? "" : "/" + subEntity)
     */
    private static final String API = "list";
    private static final String ENTITY = "Product2G";
    private static final String SUB_ENTITY = null;

    private static final int DEFAULT_BATCH_SIZE = 100;

    private static final Pattern DOUBLE_QUOTES = Pattern.compile("[\"“”]");
    private static final Pattern NO_APLICA = Pattern.compile("(?iu)(?<!\\p{L})no\\s+aplica(?!\\p{L})");
    private static final Pattern SI_APLICA = Pattern.compile("(?iu)(?<!\\p{L})s[ií]\\s+aplica(?!\\p{L})");
    private static final Pattern LONG_NUMBER_CODE = Pattern.compile(".*\\b\\d{6,}\\b.*");
    private static final Pattern MODEL_CODE = Pattern.compile(".*\\b[A-Z]{1,5}[-_/]?[0-9]{2,}[A-Z0-9-_/]*\\b.*");

    /*
     * No metas ñ aquí. ñ es válida.
     * Esto solo detecta basura típica de encoding roto.
     */
    private static final Pattern MOJIBAKE = Pattern.compile(".*[�ŽžÐð].*|.*\\bbeb[Žž]\\b.*|.*\\bni[\\u2013-]a\\b.*");

    private Product2GProductNameUpdater() {
    }

    public static void main(String[] args) {
//        if (args.length < 1) {
//            printUsage();
//            return;
//        }
    	args = new String[]{ "C:\\Users\\Juan Capiz Castro\\Downloads\\En proceso de prenderse OKs enviables.csv", "--execute", "--batchSize=2000" };
        boolean execute = hasArg(args, "--execute");
        int batchSize = intArg(args, "--batchSize=", DEFAULT_BATCH_SIZE);

        Product2GProductNameUpdater updater = new Product2GProductNameUpdater();

        if ("--repair-casing".equals(args[0])) {
            if (args.length < 3) {
                printUsage();
                return;
            }

            Path originalCsv = Paths.get(args[1]);
            Path currentCsv = Paths.get(args[2]);

            updater.runRepairCasingOnly(originalCsv, currentCsv, execute, batchSize);
            return;
        }

        Path csvPath = Paths.get(args[0]);
        updater.runNormalUpdate(csvPath, execute, batchSize);
    }

    private static void printUsage() {
        System.out.println("MODO NORMAL:");
        System.out.println("java " + Product2GProductNameUpdater.class.getName() + " <archivo.csv> [--execute] [--batchSize=100]");
        System.out.println();
        System.out.println("MODO REPARAR CASING:");
        System.out.println("java " + Product2GProductNameUpdater.class.getName() + " --repair-casing <csv_original> <csv_actual> [--execute] [--batchSize=100]");
        System.out.println();
        System.out.println("Sin --execute solo hace preview.");
    }

    /*
     * MODO NORMAL:
     * Lee un CSV y decide mejores ProductName.
     * Aquí ya está la protección para NO actualizar si el único cambio es casing.
     */
    private void runNormalUpdate(Path csvPath, boolean execute, int batchSize) {
        Map<String, Decision> decisionsByProduct2G = new LinkedHashMap<>();
        Counters counters = new Counters();

        HeaderHolder headerHolder = new HeaderHolder();

        SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
                '"',
                ',',
                '\\',
                "\n",
                StandardCharsets.UTF_8,
                values -> {
                    if (headerHolder.header == null) {
                        headerHolder.header = HeaderMap.from(values);
                        headerHolder.header.require("Product2GIdentifier");
                        headerHolder.header.require("ProductName");
                        headerHolder.header.require("CharactProductName");
                        headerHolder.header.require("DSName");
                        headerHolder.header.require("CharactName");
                        return;
                    }

                    Row row = Row.from(headerHolder.header, values);
                    counters.totalRows++;

                    if (isBlank(row.product2GIdentifier)) {
                        counters.rowsWithoutProduct2GIdentifier++;
                        return;
                    }

                    Decision decision = resolve(row);

                    if (!decision.shouldUpdate) {
                        counters.rowsWithoutUpdate++;
                        return;
                    }

                    Decision previous = decisionsByProduct2G.get(row.product2GIdentifier);

                    if (previous == null || decision.weightedScore > previous.weightedScore) {
                        decisionsByProduct2G.put(row.product2GIdentifier, decision);
                    }
                }
        );

        parser.parse(csvPath);

        counters.distinctProduct2GToUpdate = decisionsByProduct2G.size();

        printSummary("NORMAL_UPDATE", counters, decisionsByProduct2G, execute);

        if (!execute) {
            System.out.println();
            System.out.println("Dry-run solamente. No se mandó POST.");
            System.out.println("Para ejecutar realmente, corre con --execute");
            return;
        }

        postUpdates(decisionsByProduct2G, batchSize);
    }

    /*
     * MODO REPARACIÓN:
     * Compara CSV original vs CSV actual.
     *
     * Solo repara cuando:
     * - mismo Product2GIdentifier
     * - el ProductName original y actual son iguales ignorando mayúsculas/minúsculas
     * - pero no son exactamente iguales
     *
     * Eso significa: solo se dañó el casing.
     * Conserva los cambios materiales buenos.
     */
    private void runRepairCasingOnly(Path originalCsv, Path currentCsv, boolean execute, int batchSize) {
        Map<String, String> originalNames = readProductNamesByProduct2G(originalCsv);
        Map<String, String> currentNames = readProductNamesByProduct2G(currentCsv);

        Map<String, Decision> decisionsByProduct2G = new LinkedHashMap<>();
        Counters counters = new Counters();

        counters.totalRows = currentNames.size();

        for (Map.Entry<String, String> entry : currentNames.entrySet()) {
            String product2GIdentifier = entry.getKey();
            String currentName = entry.getValue();
            String originalName = originalNames.get(product2GIdentifier);

            if (isBlank(product2GIdentifier) || isBlank(originalName) || isBlank(currentName)) {
                counters.rowsWithoutUpdate++;
                continue;
            }

            String oldNorm = normalize(originalName);
            String curNorm = normalize(currentName);

            boolean onlyCaseChange =
                    oldNorm.equalsIgnoreCase(curNorm)
                    && !oldNorm.equals(curNorm);

            if (!onlyCaseChange) {
                counters.rowsWithoutUpdate++;
                continue;
            }

            Decision decision = new Decision(
                    product2GIdentifier,
                    oldNorm,
                    "REPAIR_CASING_FROM_ORIGINAL_CSV",
                    999,
                    true
            );

            decisionsByProduct2G.put(product2GIdentifier, decision);
        }

        counters.distinctProduct2GToUpdate = decisionsByProduct2G.size();

        printSummary("REPAIR_CASING_ONLY", counters, decisionsByProduct2G, execute);

        if (!execute) {
            System.out.println();
            System.out.println("Dry-run solamente. No se mandó POST.");
            System.out.println("Para reparar realmente, corre con --execute");
            return;
        }

        postUpdates(decisionsByProduct2G, batchSize);
    }

    private static Map<String, String> readProductNamesByProduct2G(Path csvPath) {
        Map<String, String> result = new LinkedHashMap<>();
        HeaderHolder headerHolder = new HeaderHolder();

        SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
                '"',
                ',',
                '\\',
                "\n",
                StandardCharsets.UTF_8,
                values -> {
                    if (headerHolder.header == null) {
                        headerHolder.header = HeaderMap.from(values);
                        headerHolder.header.require("Product2GIdentifier");
                        headerHolder.header.require("ProductName");
                        return;
                    }

                    String product2GIdentifier = safeTrim(headerHolder.header.get(values, "Product2GIdentifier"));
                    String productName = safeTrim(headerHolder.header.get(values, "ProductName"));

                    if (!isBlank(product2GIdentifier)) {
                        result.put(product2GIdentifier, productName);
                    }
                }
        );

        parser.parse(csvPath);

        return result;
    }

    private static Decision resolve(Row row) {
        Candidate productName = Candidate.of(
                row.product2GIdentifier,
                "ProductName",
                row.productName,
                40
        );

        Candidate charactProductName = Candidate.of(
                row.product2GIdentifier,
                "CharactProductName",
                row.charactProductName,
                30
        );

        Candidate dsName = Candidate.of(
                row.product2GIdentifier,
                "DSName",
                row.dsName,
                10
        );

        Candidate charactName = Candidate.of(
                row.product2GIdentifier,
                "CharactName",
                row.charactName,
                0
        );

        Candidate chosen;

        if (productName.usable) {
            chosen = productName;

            boolean productNameClearlyBad = isClearlyBadProductName(row.productName, productName.value);

            if (
                    productNameClearlyBad
                    && charactProductName.usable
                    && charactProductName.weightedScore >= productName.weightedScore + 8
            ) {
                chosen = charactProductName;
            }
        } else {
            chosen = bestAvailable(charactProductName, dsName, charactName);
        }

        if (chosen == null || !chosen.usable) {
            return Decision.noUpdate(row.product2GIdentifier, "NO_USABLE_NAME");
        }

        String originalProductName = safeTrim(row.productName);
        String newProductName = safeTrim(chosen.value);

        boolean valueChanged = !sameText(originalProductName, newProductName);

        if (!valueChanged) {
            return Decision.noUpdate(row.product2GIdentifier, "UNCHANGED");
        }

        /*
         * ESTA ES LA PROTECCIÓN QUE FALTABA.
         *
         * Si el cambio solo es:
         * Nike -> nike
         * TCL -> tcl
         * Smart TV -> smart tv
         *
         * y el original no traía basura real,
         * entonces NO se actualiza.
         */
        boolean onlyCaseChange =
                normalize(originalProductName).equalsIgnoreCase(normalize(newProductName))
                && !normalize(originalProductName).equals(normalize(newProductName));

        if (
                onlyCaseChange
                && !hasTemplateGarbage(originalProductName)
                && !hasMojibake(originalProductName)
        ) {
            return Decision.noUpdate(row.product2GIdentifier, "ONLY_CASE_CHANGE_SKIPPED");
        }

        return new Decision(
                row.product2GIdentifier,
                newProductName,
                chosen.source,
                chosen.weightedScore,
                true
        );
    }

    private static Candidate bestAvailable(Candidate... candidates) {
        Candidate best = null;

        for (Candidate candidate : candidates) {
            if (candidate == null || !candidate.usable) {
                continue;
            }

            if (best == null || candidate.weightedScore > best.weightedScore) {
                best = candidate;
            }
        }

        return best;
    }

    private static boolean isClearlyBadProductName(String raw, String pretty) {
        if (isBlank(raw) || isBlank(pretty)) {
            return true;
        }

        if (hasTemplateGarbage(raw)) {
            return true;
        }

        if (hasMojibake(raw) || hasMojibake(pretty)) {
            return true;
        }

        if (looksTooGeneric(pretty)) {
            return true;
        }

        if (looksSystemCodeName(raw)) {
            return true;
        }

        return false;
    }

    private static boolean looksTooGeneric(String value) {
        String text = safeTrim(value).toLowerCase(Locale.ROOT);

        if (text.isEmpty()) {
            return true;
        }

        String[] words = text.split("\\s+");

        if (words.length == 1) {
            return true;
        }

        if (words.length == 2) {
            return text.equals("set joyero")
                    || text.equals("set bisutería")
                    || text.equals("set bisuteria")
                    || text.equals("set reloj")
                    || text.equals("reloj mujer")
                    || text.equals("reloj hombre");
        }

        return false;
    }

    private static boolean looksSystemCodeName(String raw) {
        if (isBlank(raw)) {
            return false;
        }

        String text = raw.trim();

        int letters = 0;
        int upperLetters = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (Character.isLetter(c)) {
                letters++;

                if (Character.isUpperCase(c)) {
                    upperLetters++;
                }
            }
        }

        boolean mostlyUpper = letters >= 8 && upperLetters >= Math.max(6, (int) Math.floor(letters * 0.85));

        return mostlyUpper && (LONG_NUMBER_CODE.matcher(text).matches() || MODEL_CODE.matcher(text).matches());
    }

    private static boolean hasTemplateGarbage(String value) {
        if (value == null) {
            return false;
        }

        return DOUBLE_QUOTES.matcher(value).find()
                || NO_APLICA.matcher(value).find()
                || SI_APLICA.matcher(value).find();
    }

    private static boolean hasMojibake(String value) {
        return value != null && MOJIBAKE.matcher(value).matches();
    }

    private static int scoreRawAndPretty(String raw, String pretty) {
        if (isBlank(raw) || isBlank(pretty)) {
            return -1000;
        }

        int score = 100;

        String trimmedRaw = raw.trim();
        String trimmedPretty = pretty.trim();

        int wordCount = trimmedPretty.split("\\s+").length;

        if (wordCount < 2) {
            score -= 40;
        } else if (wordCount >= 3 && wordCount <= 14) {
            score += 12;
        } else if (wordCount > 20) {
            score -= 10;
        }

        if (trimmedPretty.length() < 8) {
            score -= 25;
        }

        if (trimmedPretty.length() > 120) {
            score -= 8;
        }

        if (hasTemplateGarbage(trimmedRaw)) {
            score -= 25;
        }

        if (hasMojibake(trimmedRaw) || hasMojibake(trimmedPretty)) {
            score -= 40;
        }

        if (looksTooGeneric(trimmedPretty)) {
            score -= 35;
        }

        if (looksSystemCodeName(trimmedRaw)) {
            score -= 20;
        }

        if (trimmedPretty.contains(",")) {
            score -= 2;
        }

        return score;
    }

    private static void postUpdates(Map<String, Decision> decisionsByProduct2G, int batchSize) {
        RESTWrapper rest = new RESTWrapper();

        JSONObject request = newRequest();
        JSONArray rows = request.getJSONArray("rows");

        int addedToBatch = 0;
        int batchNumber = 1;
        int totalSent = 0;

        for (Decision decision : decisionsByProduct2G.values()) {
            rows.put(newUpdateRow(decision.product2GIdentifier, decision.value));
            addedToBatch++;

            if (addedToBatch >= batchSize) {
                int sentNow = addedToBatch;
                sendBatch(rest, request, batchNumber, sentNow);
                totalSent += sentNow;

                request = newRequest();
                rows = request.getJSONArray("rows");
                addedToBatch = 0;
                batchNumber++;
            }
        }

        if (addedToBatch > 0) {
            int sentNow = addedToBatch;
            sendBatch(rest, request, batchNumber, sentNow);
            totalSent += sentNow;
        }

        System.out.println("Total enviado: " + totalSent);
    }

    private static void sendBatch(RESTWrapper rest, JSONObject request, int batchNumber, int rowCount) {
        System.out.println("Enviando batch " + batchNumber + " con " + rowCount + " rows...");

        rest.writeData(
                "POST",
                API,
                ENTITY,
                SUB_ENTITY,
                new HashMap<>(),
                request,
                rawResponse -> {
                    System.out.println("Respuesta batch " + batchNumber + ":");
                    System.out.println(rawResponse);
                }
        );
    }

    private static JSONObject newRequest() {
        JSONObject request = new JSONObject();

        JSONArray columns = new JSONArray();
        columns.put(new JSONObject().put("identifier", TARGET_COLUMN));

        request.put("columns", columns);
        request.put("rows", new JSONArray());

        return request;
    }

    private static JSONObject newUpdateRow(String product2GIdentifier, String productName) {
        JSONObject object = new JSONObject();
        object.put("id", "'" + product2GIdentifier + "'@1");

        JSONObject row = new JSONObject();
        row.put("object", object);
        row.put("values", new JSONArray().put(productName));

        return row;
    }

    private static void printSummary(String mode, Counters counters, Map<String, Decision> decisionsByProduct2G, boolean execute) {
        System.out.println("Modo: " + mode);
        System.out.println("Ejecución real: " + execute);
        System.out.println("Filas leídas: " + counters.totalRows);
        System.out.println("Filas sin Product2GIdentifier: " + counters.rowsWithoutProduct2GIdentifier);
        System.out.println("Filas sin update necesario: " + counters.rowsWithoutUpdate);
        System.out.println("Product2G distintos a actualizar: " + counters.distinctProduct2GToUpdate);

        int preview = 0;

        System.out.println();
        System.out.println("Preview de cambios:");

        for (Decision decision : decisionsByProduct2G.values()) {
            System.out.println(
                    decision.product2GIdentifier
                            + " | source=" + decision.source
                            + " | score=" + decision.weightedScore
                            + " | value=" + decision.value
            );

            preview++;

            if (preview >= 30) {
                break;
            }
        }

        if (decisionsByProduct2G.size() > preview) {
            System.out.println("... " + (decisionsByProduct2G.size() - preview) + " cambios más");
        }
    }

    private static boolean sameText(String a, String b) {
        return safeTrim(a).equals(safeTrim(b));
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean hasArg(String[] args, String expected) {
        for (String arg : args) {
            if (expected.equals(arg)) {
                return true;
            }
        }

        return false;
    }

    private static int intArg(String[] args, String prefix, int defaultValue) {
        for (String arg : args) {
            if (arg != null && arg.startsWith(prefix)) {
                return Integer.parseInt(arg.substring(prefix.length()));
            }
        }

        return defaultValue;
    }

    private static final class HeaderHolder {
        private HeaderMap header;
    }

    private static final class HeaderMap {
        private final Map<String, Integer> indexByName;

        private HeaderMap(Map<String, Integer> indexByName) {
            this.indexByName = indexByName;
        }

        private static HeaderMap from(String[] header) {
            Map<String, Integer> indexByName = new LinkedHashMap<>();

            for (int i = 0; i < header.length; i++) {
                indexByName.put(cleanHeader(header[i]), i);
            }

            return new HeaderMap(indexByName);
        }

        private void require(String column) {
            if (!indexByName.containsKey(cleanHeader(column))) {
                throw new IllegalArgumentException("No existe la columna requerida: " + column);
            }
        }

        private String get(String[] values, String column) {
            Integer index = indexByName.get(cleanHeader(column));

            if (index == null || index < 0 || index >= values.length) {
                return "";
            }

            return values[index] == null ? "" : values[index];
        }

        private static String cleanHeader(String value) {
            if (value == null) {
                return "";
            }

            return value.replace("\uFEFF", "").trim();
        }
    }

    private static final class Row {
        private final String product2GIdentifier;
        private final String productName;
        private final String dsName;
        private final String charactProductName;
        private final String charactName;

        private Row(
                String product2GIdentifier,
                String productName,
                String dsName,
                String charactProductName,
                String charactName
        ) {
            this.product2GIdentifier = safeTrim(product2GIdentifier);
            this.productName = productName;
            this.dsName = dsName;
            this.charactProductName = charactProductName;
            this.charactName = charactName;
        }

        private static Row from(HeaderMap header, String[] values) {
            return new Row(
                    header.get(values, "Product2GIdentifier"),
                    header.get(values, "ProductName"),
                    header.get(values, "DSName"),
                    header.get(values, "CharactProductName"),
                    header.get(values, "CharactName")
            );
        }
    }

    private static final class Candidate {
        private final String product2GIdentifier;
        private final String source;
        private final String raw;
        private final String value;
        private final int sourceWeight;
        private final int rawScore;
        private final int weightedScore;
        private final boolean usable;

        private Candidate(
                String product2GIdentifier,
                String source,
                String raw,
                String value,
                int sourceWeight,
                int rawScore,
                boolean usable
        ) {
            this.product2GIdentifier = product2GIdentifier;
            this.source = source;
            this.raw = raw;
            this.value = value;
            this.sourceWeight = sourceWeight;
            this.rawScore = rawScore;
            this.weightedScore = rawScore + sourceWeight;
            this.usable = usable;
        }

        private static Candidate of(String product2GIdentifier, String source, String raw, int sourceWeight) {
            String pretty = PrettyTextUtil.toPrettyDescription(raw);
            boolean usable = !isBlank(pretty);
            int rawScore = scoreRawAndPretty(raw, pretty);

            return new Candidate(
                    product2GIdentifier,
                    source,
                    raw,
                    pretty,
                    sourceWeight,
                    rawScore,
                    usable
            );
        }
    }

    private static final class Decision {
        private final String product2GIdentifier;
        private final String value;
        private final String source;
        private final int weightedScore;
        private final boolean shouldUpdate;

        private Decision(
                String product2GIdentifier,
                String value,
                String source,
                int weightedScore,
                boolean shouldUpdate
        ) {
            this.product2GIdentifier = product2GIdentifier;
            this.value = value;
            this.source = source;
            this.weightedScore = weightedScore;
            this.shouldUpdate = shouldUpdate;
        }

        private static Decision noUpdate(String product2GIdentifier, String source) {
            return new Decision(product2GIdentifier, "", source, -1000, false);
        }
    }

    private static final class Counters {
        private int totalRows;
        private int rowsWithoutProduct2GIdentifier;
        private int rowsWithoutUpdate;
        private int distinctProduct2GToUpdate;
    }
}