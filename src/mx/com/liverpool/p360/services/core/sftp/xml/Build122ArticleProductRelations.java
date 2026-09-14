package mx.com.liverpool.p360.services.core.sftp.xml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;

/**
 * Recorre archivos .XML de respuestas 122 de ECC/Jana y genera un PSV con
 * relaciones candidatas Article -> Product2G para materiales ATTYP=02.
 *
 * Uso:
 *   java ... Build122ArticleProductRelations <directorio> [archivoSalida]
 *
 * Salida:
 *   znprst|MATNR|SATNR|matnrIdentifier|satnrIdentifier
 *
 * Requiere agregar a DBAccessDataStub el método:
 *   identifiersBySKU(String sku, int entityId)
 */
public final class Build122ArticleProductRelations {

    private static final int ENTITY_ARTICLE = 1000;
    private static final int ENTITY_PRODUCT_2G = 1100;
    private static final String DEFAULT_OUTPUT = "article_product_relations_122.psv";

    private Build122ArticleProductRelations() {
    }

    public static void main(String[] args) {
        if (args.length < 1 || isBlank(args[0])) {
            System.err.println("Uso: Build122ArticleProductRelations <directorio> [archivoSalida]");
            System.exit(2);
        }

        Path directory = Paths.get(args[0]).toAbsolutePath().normalize();
        Path output = args.length > 1 && !isBlank(args[1])
                ? Paths.get(args[1]).toAbsolutePath().normalize()
                : Paths.get(DEFAULT_OUTPUT).toAbsolutePath().normalize();

        if (!Files.isDirectory(directory)) {
            System.err.println("No es un directorio: " + directory);
            System.exit(2);
        }

        ELog log = new ELog() {
            @Override
            public void log(String message) {
                System.out.println(message);
            }

            @Override
            public void logE(Exception e) {
                e.printStackTrace(System.err);
            }
        };

        try (DBAccessDataStub dastub = new DBAccessDataStub(log)) {
            Build122ArticleProductRelations job = new Build122ArticleProductRelations();
            int written = job.scan(directory, output, dastub);

            List<RelationCandidate> ready = loadRelationCandidates(output);
            System.out.println("Listo. Registros ATTYP=02 escritos: " + written);
            System.out.println("Relaciones con ambos identifiers resueltos: " + ready.size());
            System.out.println("Archivo: " + output);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private int scan(Path directory, Path output, DBAccessDataStub dastub)
            throws IOException, ParserConfigurationException, SAXException {

        List<Path> xmlFiles;
        try (Stream<Path> stream = Files.list(directory)) {
            xmlFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".XML"))
                    .sorted((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()))
                    .collect(Collectors.toList());
        }

        System.out.println("Directorio: " + directory);
        System.out.println("Archivos .XML encontrados: " + xmlFiles.size());

        SAXParserFactory factory = newSecureSaxParserFactory();
        Map<String, List<String>> identifierCache = new HashMap<>();

        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        int written = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(
                output,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {

            writer.write("znprst|MATNR|SATNR|matnrIdentifier|satnrIdentifier");
            writer.newLine();

            int fileNo = 0;
            for (Path xml : xmlFiles) {
                fileNo++;
                final List<MaterialRow> rows = new ArrayList<>();

                try {
                    SAXParser parser = factory.newSAXParser();
                    parser.parse(xml.toFile(), new Relation122Handler(rows));
                } catch (Exception e) {
                    System.err.println("ERROR parseando " + xml.getFileName() + ": " + e.getMessage());
                    e.printStackTrace(System.err);
                    continue;
                }

                for (MaterialRow row : rows) {
                    List<String> articleIdentifiers = identifiersBySkuCached(
                            dastub, identifierCache, row.matnr, ENTITY_ARTICLE);
                    List<String> productIdentifiers = identifiersBySkuCached(
                            dastub, identifierCache, row.satnr, ENTITY_PRODUCT_2G);

                    writer.write(psv(row.znprst));
                    writer.write('|');
                    writer.write(psv(row.matnr));
                    writer.write('|');
                    writer.write(psv(row.satnr));
                    writer.write('|');
                    writer.write(psv(joinIdentifiers(articleIdentifiers)));
                    writer.write('|');
                    writer.write(psv(joinIdentifiers(productIdentifiers)));
                    writer.newLine();
                    written++;
                }

                if (fileNo % 100 == 0 || fileNo == xmlFiles.size()) {
                    System.out.println(fileNo + "/" + xmlFiles.size()
                            + " XML; relaciones acumuladas=" + written);
                }
            }
        }

        return written;
    }

    private static List<String> identifiersBySkuCached(
            DBAccessDataStub dastub,
            Map<String, List<String>> cache,
            String sku,
            int entityId) {

        if (isBlank(sku)) {
            return Collections.emptyList();
        }

        String key = entityId + "|" + sku;
        List<String> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        List<String> found = dastub.identifiersBySKU(sku, entityId);
        if (found == null || found.isEmpty()) {
            found = Collections.emptyList();
        } else {
            found = Collections.unmodifiableList(new ArrayList<>(found));
        }

        cache.put(key, found);
        return found;
    }

    private static String joinIdentifiers(List<String> identifiers) {
        if (identifiers == null || identifiers.isEmpty()) {
            return "";
        }
        return String.join(",", identifiers);
    }

    /**
     * Lee el PSV generado y devuelve solamente relaciones que son seguras para
     * el siguiente paso: exactamente un Article identifier y exactamente un
     * Product2G identifier.
     *
     * Si un campo contiene varios identifiers separados por coma se omite a
     * propósito: no conviene automatizar una asociación ambigua.
     */
    public static List<RelationCandidate> loadRelationCandidates(Path input) throws IOException {
        List<RelationCandidate> result = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;

            while ((line = reader.readLine()) != null) {
                if (first) {
                    first = false;
                    if (line.startsWith("znprst|MATNR|SATNR|")) {
                        continue;
                    }
                }

                String[] pieces = splitPsv(line, 5);
                if (pieces.length != 5) {
                    continue;
                }

                String matnrIdentifier = pieces[3];
                String satnrIdentifier = pieces[4];

                if (isBlank(matnrIdentifier) || isBlank(satnrIdentifier)) {
                    continue;
                }
                if (matnrIdentifier.indexOf(',') >= 0 || satnrIdentifier.indexOf(',') >= 0) {
                    continue;
                }

                result.add(new RelationCandidate(
                        pieces[0],
                        pieces[1],
                        pieces[2],
                        matnrIdentifier,
                        satnrIdentifier));
            }
        }

        return result;
    }

    public static final class RelationCandidate {
        private final String znprst;
        private final String matnr;
        private final String satnr;
        private final String articleIdentifier;
        private final String productIdentifier;

        private RelationCandidate(
                String znprst,
                String matnr,
                String satnr,
                String articleIdentifier,
                String productIdentifier) {
            this.znprst = znprst;
            this.matnr = matnr;
            this.satnr = satnr;
            this.articleIdentifier = articleIdentifier;
            this.productIdentifier = productIdentifier;
        }

        public String getZnprst() {
            return znprst;
        }

        public String getMatnr() {
            return matnr;
        }

        public String getSatnr() {
            return satnr;
        }

        public String getArticleIdentifier() {
            return articleIdentifier;
        }

        public String getProductIdentifier() {
            return productIdentifier;
        }

        @Override
        public String toString() {
            return articleIdentifier + " -> " + productIdentifier
                    + " [MATNR=" + matnr + ", SATNR=" + satnr + ", ZNPRST=" + znprst + "]";
        }
    }

    private static final class MaterialRow {
        private final String znprst;
        private final String matnr;
        private final String satnr;

        private MaterialRow(String znprst, String matnr, String satnr) {
            this.znprst = nullToEmpty(znprst);
            this.matnr = nullToEmpty(matnr);
            this.satnr = nullToEmpty(satnr);
        }
    }

    private enum Source {
        ECC,
        JANA,
        UNKNOWN
    }

    private static final class Relation122Handler extends DefaultHandler {
        private final List<MaterialRow> output;

        private boolean insideProduct;
        private Source source = Source.UNKNOWN;
        private String productZnprst;
        private String valueZnprst;
        private String productId;
        private String matnr;
        private String satnr;
        private String attyp;

        private String currentAttributeId;
        private StringBuilder currentText;

        private Relation122Handler(List<MaterialRow> output) {
            this.output = output;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String name = elementName(localName, qName);

            if ("Product".equals(name)) {
                resetProduct();
                insideProduct = true;

                if (attributes.getValue("ZNPRST") != null) {
                    source = Source.ECC;
                    productZnprst = trim(attributes.getValue("ZNPRST"));
                } else if (attributes.getValue("EAN11_EAN") != null) {
                    source = Source.JANA;
                }
                return;
            }

            if (insideProduct && "Value".equals(name)) {
                currentAttributeId = attributes.getValue("AttributeID");
                currentText = new StringBuilder();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (currentText != null) {
                currentText.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String name = elementName(localName, qName);

            if ("Value".equals(name) && currentAttributeId != null) {
                String value = currentText == null ? "" : currentText.toString().trim();
                acceptValue(currentAttributeId, value);
                currentAttributeId = null;
                currentText = null;
                return;
            }

            if ("Product".equals(name) && insideProduct) {
                if ("02".equals(trim(attyp))) {
                    String normalizedMatnr = normalizeMaterial(matnr, source);
                    String normalizedSatnr = normalizeMaterial(satnr, source);

                    // ATTYP=02 debe traer SATNR. Si no lo trae no hay relación
                    // padre-hijo confiable que reconstruir a partir del 122.
                    if (!isBlank(normalizedMatnr) && !isBlank(normalizedSatnr)) {
                        String znprst = firstNotBlank(productZnprst, valueZnprst, productId);
                        znprst = normalizeZnprst(znprst, source);
                        output.add(new MaterialRow(znprst, normalizedMatnr, normalizedSatnr));
                    }
                }

                insideProduct = false;
                resetProduct();
            }
        }

        private void acceptValue(String attributeId, String value) {
            if ("ZNPRST".equals(attributeId)) {
                valueZnprst = value;
            } else if ("PRODUCT_ID".equals(attributeId)) {
                productId = value;
            } else if ("MATNR".equals(attributeId)) {
                matnr = value;
            } else if ("SATNR".equals(attributeId)) {
                satnr = value;
            } else if ("ATTYP".equals(attributeId)) {
                attyp = value;
            }
        }

        private void resetProduct() {
            source = Source.UNKNOWN;
            productZnprst = null;
            valueZnprst = null;
            productId = null;
            matnr = null;
            satnr = null;
            attyp = null;
            currentAttributeId = null;
            currentText = null;
        }
    }

    private static SAXParserFactory newSecureSaxParserFactory() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {
            // Se conserva el mismo enfoque tolerante que usan los parsers 122 actuales.
        }
        return factory;
    }

    private static String normalizeMaterial(String value, Source source) {
        String normalized = trim(value);
        if (source == Source.JANA) {
            normalized = normalized.replaceFirst("^0+", "");
        }
        return normalized;
    }

    private static String normalizeZnprst(String value, Source source) {
        String normalized = trim(value);
        if (source == Source.JANA && normalized.length() == 15 && !normalized.startsWith("S")) {
            return "1" + normalized;
        }
        return normalized;
    }

    private static String firstNotBlank(String... values) {
        if (values != null) {
            for (String value : values) {
                if (!isBlank(value)) {
                    return value.trim();
                }
            }
        }
        return "";
    }

    private static String elementName(String localName, String qName) {
        return localName != null && !localName.isEmpty() ? localName : qName;
    }

    private static String psv(String value) {
        return nullToEmpty(value)
                .replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static String[] splitPsv(String line, int expectedFields) {
        List<String> fields = new ArrayList<>(expectedFields);
        StringBuilder current = new StringBuilder();
        boolean escaped = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (escaped) {
                if (c == 'r') {
                    current.append('\r');
                } else if (c == 'n') {
                    current.append('\n');
                } else {
                    current.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '|') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        if (escaped) {
            current.append('\\');
        }
        fields.add(current.toString());

        return fields.toArray(new String[0]);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
