package mx.com.liverpool.p360.services.core.temp.xml.local;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Barre XMLs de productos enviados, determina la ultima version por Product raiz,
 * valida reglas de publicacion y detecta articulos/hijos que aparecieron con mas de un padre.
 *
 * Uso:
 *   java mx.com.liverpool.p360.services.core.temp.xml.local.ProductXmlLastVersionValidatorV001 <directorio_xml> [directorio_salida] [creation|modified|filename]
 *
 * Default:
 *   outputDir = ./product_xml_validation_v001
 *   timeMode  = creation
 */
public class ProductXmlLastVersionValidatorV001 {

    private static final Pattern PEPELE_TS = Pattern.compile("^.*?(\\d{10,})\\.xml$", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());

    private final Map<String, ProductOccurrence> latestByRootProductId = new TreeMap<>();
    private final Map<String, ChildHistory> childHistoryByChildId = new TreeMap<>();

    private long filesSeen = 0;
    private long filesParsed = 0;
    private long filesFailed = 0;
    private long rootProductsSeen = 0;
    private long childProductsSeen = 0;
    private String timeMode = "creation";

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 3) {
            System.err.println("Uso: java " + ProductXmlLastVersionValidatorV001.class.getName() + " <directorio_xml> [directorio_salida] [creation|modified|filename]");
            System.exit(1);
        }

        Path inputDir = Paths.get(args[0]);
        Path outputDir = args.length >= 2 ? Paths.get(args[1]) : Paths.get("product_xml_validation_v001");
        String timeMode = args.length >= 3 ? args[2].trim().toLowerCase(Locale.ROOT) : "creation";

        if (!Files.isDirectory(inputDir)) {
            throw new IllegalArgumentException("No es directorio: " + inputDir.toAbsolutePath());
        }
        if (!Arrays.asList("creation", "modified", "filename").contains(timeMode)) {
            throw new IllegalArgumentException("timeMode invalido. Usa: creation, modified o filename");
        }

        Files.createDirectories(outputDir);

        ProductXmlLastVersionValidatorV001 app = new ProductXmlLastVersionValidatorV001();
        app.timeMode = timeMode;
        app.run(inputDir, outputDir);
    }

    private void run(Path inputDir, Path outputDir) throws Exception {
        long init = System.currentTimeMillis();

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {
        }
        SAXParser parser = factory.newSAXParser();

        try (Stream<Path> paths = Files.walk(inputDir, FileVisitOption.FOLLOW_LINKS)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xml"))
                 .forEach(p -> parseOneFile(parser, p));
        }

        List<ValidationFailure> failures = validateLatestProducts();

        Path failuresCsv = outputDir.resolve("validation_failures_latest_v001.csv");
        Path multiParentCsv = outputDir.resolve("child_products_with_multiple_parents_v001.csv");
        Path summaryTxt = outputDir.resolve("summary_v001.txt");

        writeFailuresCsv(failuresCsv, failures);
        writeMultiParentCsv(multiParentCsv);
        writeSummary(summaryTxt, failures, init);

        System.out.println("Done.");
        System.out.println("Latest root products: " + latestByRootProductId.size());
        System.out.println("Validation failures: " + failures.size());
        System.out.println("Child IDs with >1 parent: " + countMultiParentChildren());
        System.out.println("Output: " + outputDir.toAbsolutePath());
    }

    private void parseOneFile(SAXParser parser, Path file) {
        filesSeen++;
        long fileMillis = resolveFileMillis(file);
        Long filenameMillis = extractFilenameMillis(file);

        Handler handler = new Handler();
        try {
            try (InputStream in = Files.newInputStream(file)) {
                parser.parse(in, handler);
            }
            filesParsed++;

            for (Product root : handler.getRootProducts()) {
                rootProductsSeen++;
                ProductOccurrence occ = new ProductOccurrence(
                        root,
                        file.toAbsolutePath().toString(),
                        fileMillis,
                        filenameMillis,
                        handler.getExportTime(),
                        relevantAssetNames(root, handler.getAssetMap())
                );

                ProductOccurrence previous = latestByRootProductId.get(root.id);
                if (previous == null || occ.isNewerThan(previous)) {
                    latestByRootProductId.put(root.id, occ);
                }

                for (Product child : root.children) {
                    childProductsSeen++;
                    childHistoryByChildId.computeIfAbsent(child.id, ChildHistory::new)
                            .add(root.id, file.toAbsolutePath().toString(), fileMillis);
                }
            }
        } catch (Exception e) {
            filesFailed++;
            System.err.println("Problem processing file: " + file.toAbsolutePath() + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private Map<String, String> relevantAssetNames(Product root, Map<String, Asset> allAssets) {
        Set<String> wantedAssetIds = new TreeSet<>();
        collectPrimaryAssetIds(root, wantedAssetIds);
        Map<String, String> result = new TreeMap<>();
        for (String assetId : wantedAssetIds) {
            Asset asset = allAssets.get(assetId);
            if (asset != null) {
                result.put(assetId, trimToEmpty(asset.name));
            }
        }
        return result;
    }

    private void collectPrimaryAssetIds(Product product, Set<String> wantedAssetIds) {
        for (AssetRef ref : product.assetRefs) {
            if ("PrimaryProductImage".equals(ref.type) && notBlank(ref.assetId)) {
                wantedAssetIds.add(ref.assetId);
            }
        }
        for (Product child : product.children) {
            collectPrimaryAssetIds(child, wantedAssetIds);
        }
    }

    private List<ValidationFailure> validateLatestProducts() {
        List<ValidationFailure> failures = new ArrayList<>();
        for (ProductOccurrence occ : latestByRootProductId.values()) {
            Product root = occ.root;
            validateRootProduct(occ, root, failures);
            for (Product child : root.children) {
                validateChildProduct(occ, root, child, failures);
            }
        }
        Collections.sort(failures);
        return failures;
    }

    private void validateRootProduct(ProductOccurrence occ, Product root, List<ValidationFailure> failures) {
        String level = root.children.isEmpty() ? "ROOT_LEAF" : "ROOT";

        requireName(occ, root, root.id, level, failures);

        requireAttr(occ, root, root.id, level, "SKU", failures);
        requireKey(occ, root, root.id, level, "SKUID", failures);
        requireAttr(occ, root, root.id, level, "Status", failures);
        requireAttr(occ, root, root.id, level, "ProductName", failures);
        requireAttr(occ, root, root.id, level, "ProductType", failures);
        requireAttr(occ, root, root.id, level, "ItemGroup2", failures);
        requireAttr(occ, root, root.id, level, "Section", failures);
        requireAttr(occ, root, root.id, level, "isMarketPlace", failures);
        requireAttr(occ, root, root.id, level, "BrandNameATG", failures);
        requireAttr(occ, root, root.id, level, "BrandIDATG", failures);
        requireAnyAttr(occ, root, root.id, level, "Negocio_OR_EXTWG_S4H", failures, "Negocio", "EXTWG_S4H");
        requireAttr(occ, root, root.id, level, "BaseUnitOfMeasure", failures);
        requireAttr(occ, root, root.id, level, "supplierShopId", failures);
        requireAnyAttr(occ, root, root.id, level, "MainBarCodeS4H_OR_MainBarCode", failures, "MainBarCodeS4H", "MainBarCode");
        requireKey(occ, root, root.id, level, "EANKey", failures);
        requireAttr(occ, root, root.id, level, "TypeMainBarCode", failures);
        requireAttr(occ, root, root.id, level, "SupplierID", failures);
        requireAttr(occ, root, root.id, level, "ParentSKU", failures);
        requireAttr(occ, root, root.id, level, "SupplierPartNumber", failures);
        requireClassification(occ, root, root.id, level, "WebsiteLink", failures);

        if (root.children.isEmpty()) {
            requireAttr(occ, root, root.id, level, "ColoursLiverpoolAtt", failures);
            requireAnyAttr(occ, root, root.id, level, "SizeVaD_OR_clothingSize", failures, "SizeVaD", "clothingSize", "TamanoUnico");
            requirePrimaryImageAndNameMatch(occ, root, root.id, level, failures);
        }
    }

    private void validateChildProduct(ProductOccurrence occ, Product root, Product child, List<ValidationFailure> failures) {
        String level = "CHILD";

        requireName(occ, child, root.id, level, failures);

        requireAttr(occ, child, root.id, level, "SKU", failures);
        requireKey(occ, child, root.id, level, "SKUID", failures);
        requireAnyAttr(occ, child, root.id, level, "MainBarCodeS4H_OR_MainBarCode", failures, "MainBarCodeS4H", "MainBarCode");
        requireKey(occ, child, root.id, level, "EANKey", failures);
        requireAttr(occ, child, root.id, level, "Status", failures);
        requireAttr(occ, child, root.id, level, "ColoursLiverpoolAtt", failures);
        requireAnyAttr(occ, child, root.id, level, "SizeVaD_OR_clothingSize", failures, "SizeVaD", "clothingSize", "TamanoUnico");
        requireAttr(occ, child, root.id, level, "SupplierPartNumber", failures);
        requireAttr(occ, child, root.id, level, "variantSequence", failures);
        requirePrimaryImageAndNameMatch(occ, child, root.id, level, failures);
    }

    private void requireName(ProductOccurrence occ, Product product, String rootProductId, String level, List<ValidationFailure> failures) {
        if (!notBlank(product.name)) {
            failures.add(ValidationFailure.of(occ, product, rootProductId, level, "MISSING_NAME", "Etiqueta <Name> vacia o inexistente"));
        }
    }

    private void requireAttr(ProductOccurrence occ, Product product, String rootProductId, String level, String attributeId, List<ValidationFailure> failures) {
        if (!notBlank(product.firstAttrText(attributeId))) {
            failures.add(ValidationFailure.of(occ, product, rootProductId, level, "MISSING_ATTR", attributeId));
        }
    }

    private void requireAnyAttr(ProductOccurrence occ, Product product, String rootProductId, String level, String rule, List<ValidationFailure> failures, String... attributeIds) {
        for (String attributeId : attributeIds) {
            if (notBlank(product.firstAttrText(attributeId))) {
                return;
            }
        }
        failures.add(ValidationFailure.of(occ, product, rootProductId, level, "MISSING_ANY_ATTR", rule + " -> " + String.join(" | ", attributeIds)));
    }

    private void requireKey(ProductOccurrence occ, Product product, String rootProductId, String level, String keyId, List<ValidationFailure> failures) {
        if (!notBlank(product.keyValues.get(keyId))) {
            failures.add(ValidationFailure.of(occ, product, rootProductId, level, "MISSING_KEYVALUE", keyId));
        }
    }

    private void requireClassification(ProductOccurrence occ, Product product, String rootProductId, String level, String type, List<ValidationFailure> failures) {
        for (ClassificationRef ref : product.classifications) {
            if (type.equals(ref.type) && notBlank(ref.classificationId)) {
                return;
            }
        }
        failures.add(ValidationFailure.of(occ, product, rootProductId, level, "MISSING_CLASSIFICATION", "ClassificationReference Type=" + type));
    }

    private void requirePrimaryImageAndNameMatch(ProductOccurrence occ, Product product, String rootProductId, String level, List<ValidationFailure> failures) {
        List<AssetRef> primaryRefs = new ArrayList<>();
        for (AssetRef ref : product.assetRefs) {
            if ("PrimaryProductImage".equals(ref.type)) {
                primaryRefs.add(ref);
            }
        }
        if (primaryRefs.isEmpty()) {
            failures.add(ValidationFailure.of(occ, product, rootProductId, level, "MISSING_PRIMARY_IMAGE", "AssetCrossReference Type=PrimaryProductImage"));
            return;
        }

        List<String> candidates = new ArrayList<>();
        addIfNotBlank(candidates, product.keyValues.get("SKUID"));
        addIfNotBlank(candidates, product.firstAttrText("SKU"));
        addIfNotBlank(candidates, product.id);
        addIfNotBlank(candidates, product.name);

        boolean hasExistingAsset = false;
        for (AssetRef ref : primaryRefs) {
            String assetName = occ.assetNamesByAssetId.get(ref.assetId);
            if (!notBlank(assetName)) {
                continue;
            }
            hasExistingAsset = true;
            if (containsAnyNormalized(assetName, candidates)) {
                return;
            }
        }

        if (!hasExistingAsset) {
            failures.add(ValidationFailure.of(occ, product, rootProductId, level, "PRIMARY_IMAGE_ASSET_NOT_FOUND", primaryRefIds(primaryRefs)));
        } else {
            failures.add(ValidationFailure.of(occ, product, rootProductId, level, "PRIMARY_IMAGE_NAME_MISMATCH",
                    "assetNames=" + assetNames(occ, primaryRefs) + " expectedAny=" + candidates));
        }
    }

    private String primaryRefIds(List<AssetRef> refs) {
        List<String> ids = new ArrayList<>();
        for (AssetRef ref : refs) {
            ids.add(ref.assetId);
        }
        return String.join(" | ", ids);
    }

    private String assetNames(ProductOccurrence occ, List<AssetRef> refs) {
        List<String> names = new ArrayList<>();
        for (AssetRef ref : refs) {
            names.add(ref.assetId + "=" + trimToEmpty(occ.assetNamesByAssetId.get(ref.assetId)));
        }
        return String.join(" | ", names);
    }

    private void writeFailuresCsv(Path output, List<ValidationFailure> failures) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            w.write("rootProductId,productId,parentId,level,userTypeId,fileTimeMillis,fileTimeIso,filePath,exportTime,rule,detail\n");
            for (ValidationFailure f : failures) {
                w.write(csv(f.rootProductId)); w.write(',');
                w.write(csv(f.productId)); w.write(',');
                w.write(csv(f.parentId)); w.write(',');
                w.write(csv(f.level)); w.write(',');
                w.write(csv(f.userTypeId)); w.write(',');
                w.write(Long.toString(f.fileTimeMillis)); w.write(',');
                w.write(csv(iso(f.fileTimeMillis))); w.write(',');
                w.write(csv(f.filePath)); w.write(',');
                w.write(csv(f.exportTime)); w.write(',');
                w.write(csv(f.rule)); w.write(',');
                w.write(csv(f.detail));
                w.write('\n');
            }
        }
    }

    private void writeMultiParentCsv(Path output) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            w.write("childProductId,parentCount,parentProductIds,firstSeenMillis,firstSeenIso,lastSeenMillis,lastSeenIso,occurrenceCount,files\n");
            for (ChildHistory h : childHistoryByChildId.values()) {
                if (h.parentIds.size() <= 1) {
                    continue;
                }
                w.write(csv(h.childProductId)); w.write(',');
                w.write(Integer.toString(h.parentIds.size())); w.write(',');
                w.write(csv(String.join(" | ", h.parentIds))); w.write(',');
                w.write(Long.toString(h.firstSeenMillis)); w.write(',');
                w.write(csv(iso(h.firstSeenMillis))); w.write(',');
                w.write(Long.toString(h.lastSeenMillis)); w.write(',');
                w.write(csv(iso(h.lastSeenMillis))); w.write(',');
                w.write(Integer.toString(h.occurrenceCount)); w.write(',');
                w.write(csv(String.join(" | ", h.files)));
                w.write('\n');
            }
        }
    }

    private void writeSummary(Path output, List<ValidationFailure> failures, long initMillis) throws IOException {
        Map<String, Integer> failuresByRule = new TreeMap<>();
        for (ValidationFailure f : failures) {
            failuresByRule.merge(f.rule, 1, Integer::sum);
        }

        try (BufferedWriter w = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            w.write("timeMode=" + timeMode + "\n");
            w.write("filesSeen=" + filesSeen + "\n");
            w.write("filesParsed=" + filesParsed + "\n");
            w.write("filesFailed=" + filesFailed + "\n");
            w.write("rootProductsSeen=" + rootProductsSeen + "\n");
            w.write("childProductsSeen=" + childProductsSeen + "\n");
            w.write("latestRootProducts=" + latestByRootProductId.size() + "\n");
            w.write("validationFailures=" + failures.size() + "\n");
            w.write("childIdsWithMoreThanOneParent=" + countMultiParentChildren() + "\n");
            w.write("elapsedMillis=" + (System.currentTimeMillis() - initMillis) + "\n");
            w.write("\nfailuresByRule:\n");
            for (Map.Entry<String, Integer> e : failuresByRule.entrySet()) {
                w.write(e.getKey() + "=" + e.getValue() + "\n");
            }
        }
    }

    private int countMultiParentChildren() {
        int c = 0;
        for (ChildHistory h : childHistoryByChildId.values()) {
            if (h.parentIds.size() > 1) {
                c++;
            }
        }
        return c;
    }

    private long resolveFileMillis(Path file) {
        Long filenameMillis = extractFilenameMillis(file);
        try {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            if ("filename".equals(timeMode)) {
                return filenameMillis != null ? filenameMillis : attrs.lastModifiedTime().toMillis();
            }
            if ("modified".equals(timeMode)) {
                return attrs.lastModifiedTime().toMillis();
            }
            long creation = attrs.creationTime().toMillis();
            if (creation > 0L) {
                return creation;
            }
            return attrs.lastModifiedTime().toMillis();
        } catch (Exception e) {
            if (filenameMillis != null) {
                return filenameMillis;
            }
            return new File(file.toString()).lastModified();
        }
    }

    private Long extractFilenameMillis(Path file) {
        Matcher m = PEPELE_TS.matcher(file.getFileName().toString());
        if (!m.matches()) {
            return null;
        }
        try {
            return Long.parseLong(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void addIfNotBlank(List<String> values, String value) {
        if (notBlank(value)) {
            values.add(value.trim());
        }
    }

    private static boolean containsAnyNormalized(String haystack, List<String> needles) {
        String h = normalizeForContains(haystack);
        for (String needle : needles) {
            String n = normalizeForContains(needle);
            if (notBlank(n) && h.contains(n)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeForContains(String s) {
        return trimToEmpty(s).toLowerCase(Locale.ROOT)
                .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u").replace("ü", "u").replace("ñ", "n");
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static String iso(long millis) {
        return ISO_FMT.format(Instant.ofEpochMilli(millis));
    }

    private static String csv(String s) {
        if (s == null) {
            s = "";
        }
        return '"' + s.replace("\"", "\"\"") + '"';
    }

    private static final class ProductOccurrence {
        final Product root;
        final String filePath;
        final long fileTimeMillis;
        final Long filenameMillis;
        final String exportTime;
        final Map<String, String> assetNamesByAssetId;

        ProductOccurrence(Product root, String filePath, long fileTimeMillis, Long filenameMillis, String exportTime, Map<String, String> assetNamesByAssetId) {
            this.root = root;
            this.filePath = filePath;
            this.fileTimeMillis = fileTimeMillis;
            this.filenameMillis = filenameMillis;
            this.exportTime = exportTime;
            this.assetNamesByAssetId = assetNamesByAssetId;
        }

        boolean isNewerThan(ProductOccurrence other) {
            if (this.fileTimeMillis != other.fileTimeMillis) {
                return this.fileTimeMillis > other.fileTimeMillis;
            }
            if (this.filenameMillis != null && other.filenameMillis != null && !Objects.equals(this.filenameMillis, other.filenameMillis)) {
                return this.filenameMillis > other.filenameMillis;
            }
            return this.filePath.compareTo(other.filePath) > 0;
        }
    }

    private static final class ChildHistory {
        final String childProductId;
        final Set<String> parentIds = new TreeSet<>();
        final Set<String> files = new TreeSet<>();
        long firstSeenMillis = Long.MAX_VALUE;
        long lastSeenMillis = Long.MIN_VALUE;
        int occurrenceCount = 0;

        ChildHistory(String childProductId) {
            this.childProductId = childProductId;
        }

        void add(String parentId, String file, long millis) {
            if (notBlank(parentId)) {
                parentIds.add(parentId);
            }
            files.add(file);
            firstSeenMillis = Math.min(firstSeenMillis, millis);
            lastSeenMillis = Math.max(lastSeenMillis, millis);
            occurrenceCount++;
        }
    }

    private static final class ValidationFailure implements Comparable<ValidationFailure> {
        final String rootProductId;
        final String productId;
        final String parentId;
        final String level;
        final String userTypeId;
        final long fileTimeMillis;
        final String filePath;
        final String exportTime;
        final String rule;
        final String detail;

        private ValidationFailure(String rootProductId, String productId, String parentId, String level, String userTypeId,
                                  long fileTimeMillis, String filePath, String exportTime, String rule, String detail) {
            this.rootProductId = rootProductId;
            this.productId = productId;
            this.parentId = parentId;
            this.level = level;
            this.userTypeId = userTypeId;
            this.fileTimeMillis = fileTimeMillis;
            this.filePath = filePath;
            this.exportTime = exportTime;
            this.rule = rule;
            this.detail = detail;
        }

        static ValidationFailure of(ProductOccurrence occ, Product product, String rootProductId, String level, String rule, String detail) {
            return new ValidationFailure(rootProductId, product.id, product.parentId, level, product.userTypeId,
                    occ.fileTimeMillis, occ.filePath, occ.exportTime, rule, detail);
        }

        @Override
        public int compareTo(ValidationFailure o) {
            int c = this.rootProductId.compareTo(o.rootProductId);
            if (c != 0) return c;
            c = this.productId.compareTo(o.productId);
            if (c != 0) return c;
            c = this.rule.compareTo(o.rule);
            if (c != 0) return c;
            return this.detail.compareTo(o.detail);
        }
    }

    private static final class Product {
        final String id;
        final String parentId;
        final String userTypeId;
        String name;
        final Map<String, List<Value>> valuesByAttribute = new LinkedHashMap<>();
        final Map<String, String> keyValues = new LinkedHashMap<>();
        final List<AssetRef> assetRefs = new ArrayList<>();
        final List<ClassificationRef> classifications = new ArrayList<>();
        final List<Product> children = new ArrayList<>();

        Product(String id, String parentId, String userTypeId) {
            this.id = trimToEmpty(id);
            this.parentId = trimToEmpty(parentId);
            this.userTypeId = trimToEmpty(userTypeId);
        }

        void addValue(Value v) {
            if (!notBlank(v.attributeId)) {
                return;
            }
            valuesByAttribute.computeIfAbsent(v.attributeId, k -> new ArrayList<>()).add(v);
        }

        String firstAttrText(String attributeId) {
            List<Value> values = valuesByAttribute.get(attributeId);
            if (values == null) {
                return "";
            }
            for (Value v : values) {
                String txt = v.text.toString();
                if (notBlank(txt)) {
                    return txt.trim();
                }
                if (notBlank(v.id)) {
                    return v.id.trim();
                }
            }
            return "";
        }
    }

    private static final class Value {
        final String attributeId;
        final String id;
        final String unitId;
        final StringBuilder text = new StringBuilder();

        Value(String attributeId, String id, String unitId) {
            this.attributeId = trimToEmpty(attributeId);
            this.id = trimToEmpty(id);
            this.unitId = trimToEmpty(unitId);
        }
    }

    private static final class Asset {
        final String id;
        final String userTypeId;
        String name;

        Asset(String id, String userTypeId) {
            this.id = trimToEmpty(id);
            this.userTypeId = trimToEmpty(userTypeId);
        }
    }

    private static final class AssetRef {
        final String assetId;
        final String type;

        AssetRef(String assetId, String type) {
            this.assetId = trimToEmpty(assetId);
            this.type = trimToEmpty(type);
        }
    }

    private static final class ClassificationRef {
        final String classificationId;
        final String type;

        ClassificationRef(String classificationId, String type) {
            this.classificationId = trimToEmpty(classificationId);
            this.type = trimToEmpty(type);
        }
    }

    private static final class Handler extends DefaultHandler {
        private final ArrayDeque<Product> productStack = new ArrayDeque<>();
        private final List<Product> rootProducts = new ArrayList<>();
        private final Map<String, Asset> assetMap = new TreeMap<>();
        private Asset currentAsset;
        private Value currentValue;
        private String currentKeyId;
        private StringBuilder currentText;
        private String textTarget;
        private String exportTime;

        List<Product> getRootProducts() {
            return rootProducts;
        }

        Map<String, Asset> getAssetMap() {
            return assetMap;
        }

        String getExportTime() {
            return exportTime;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            String name = elementName(localName, qName);

            if ("STEP-ProductInformation".equals(name)) {
                exportTime = attributes.getValue("ExportTime");
                return;
            }

            if ("Product".equals(name)) {
                String id = attributes.getValue("ID");
                String parentId = attributes.getValue("ParentID");
                String userTypeId = attributes.getValue("UserTypeID");
                if (!productStack.isEmpty() && !notBlank(parentId)) {
                    parentId = productStack.peekLast().id;
                }
                productStack.addLast(new Product(id, parentId, userTypeId));
                return;
            }

            if ("Asset".equals(name) && productStack.isEmpty()) {
                currentAsset = new Asset(attributes.getValue("ID"), attributes.getValue("UserTypeID"));
                return;
            }

            if (!productStack.isEmpty()) {
                Product p = productStack.peekLast();
                if ("Name".equals(name)) {
                    beginText("PRODUCT_NAME");
                } else if ("Value".equals(name)) {
                    currentValue = new Value(attributes.getValue("AttributeID"), attributes.getValue("ID"), attributes.getValue("UnitID"));
                    beginText("PRODUCT_VALUE");
                } else if ("KeyValue".equals(name)) {
                    currentKeyId = trimToEmpty(attributes.getValue("KeyID"));
                    beginText("PRODUCT_KEYVALUE");
                } else if ("AssetCrossReference".equals(name)) {
                    p.assetRefs.add(new AssetRef(attributes.getValue("AssetID"), attributes.getValue("Type")));
                } else if ("ClassificationReference".equals(name)) {
                    p.classifications.add(new ClassificationRef(attributes.getValue("ClassificationID"), attributes.getValue("Type")));
                }
                return;
            }

            if (currentAsset != null) {
                if ("Name".equals(name)) {
                    beginText("ASSET_NAME");
                }
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (currentText != null) {
                currentText.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            String name = elementName(localName, qName);

            if (!productStack.isEmpty()) {
                Product p = productStack.peekLast();
                if ("Name".equals(name) && "PRODUCT_NAME".equals(textTarget)) {
                    p.name = text();
                    clearText();
                    return;
                }
                if ("Value".equals(name) && "PRODUCT_VALUE".equals(textTarget)) {
                    currentValue.text.append(text());
                    p.addValue(currentValue);
                    currentValue = null;
                    clearText();
                    return;
                }
                if ("KeyValue".equals(name) && "PRODUCT_KEYVALUE".equals(textTarget)) {
                    if (notBlank(currentKeyId)) {
                        p.keyValues.put(currentKeyId, text());
                    }
                    currentKeyId = null;
                    clearText();
                    return;
                }
                if ("Product".equals(name)) {
                    Product finished = productStack.removeLast();
                    if (productStack.isEmpty()) {
                        rootProducts.add(finished);
                    } else {
                        productStack.peekLast().children.add(finished);
                    }
                    return;
                }
            }

            if (currentAsset != null) {
                if ("Name".equals(name) && "ASSET_NAME".equals(textTarget)) {
                    currentAsset.name = text();
                    clearText();
                    return;
                }
                if ("Asset".equals(name)) {
                    if (notBlank(currentAsset.id)) {
                        assetMap.put(currentAsset.id, currentAsset);
                    }
                    currentAsset = null;
                }
            }
        }

        private void beginText(String target) {
            currentText = new StringBuilder();
            textTarget = target;
        }

        private String text() {
            return currentText == null ? "" : currentText.toString().trim();
        }

        private void clearText() {
            currentText = null;
            textTarget = null;
        }

        private String elementName(String localName, String qName) {
            return localName != null && !localName.isEmpty() ? localName : qName;
        }
    }
}
