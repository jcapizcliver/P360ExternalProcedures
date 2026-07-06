package mx.com.liverpool.p360.services.core.sftp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.sftp.handlers.ECC122ResponseHandler;
import mx.com.liverpool.p360.services.core.sftp.handlers.Product122;
import mx.com.liverpool.p360.services.core.sftp.handlers.Value;

public class ExtractECCMatnrSatnrFromIndex {

    public static void main(String[] args) {
        if (args == null || args.length < 3) {
            System.out.println("Uso:");
            System.out.println("java -cp \"bin:lib/*\" mx.com.liverpool.p360.services.core.sftp.ExtractECCMatnrSatnrFromIndex <skus.txt> <IxECC> <salida.csv>");
            return;
        }

        Path skusPath = Paths.get(args[0]);
        Path indexPath = Paths.get(args[1]);
        Path outputPath = Paths.get(args[2]);

        try {
            new ExtractECCMatnrSatnrFromIndex().run(skusPath, indexPath, outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void run(Path skusPath, Path indexPath, Path outputPath) throws Exception {
        Set<String> requestedSkus = readSkuList(skusPath);

        if (requestedSkus.isEmpty()) {
            System.out.println("No hay SKUs en la lista.");
            return;
        }

        Map<Path, Set<String>> skusByFile = resolveFilesFromIndex(requestedSkus, indexPath);

        System.out.println("SKUs solicitados: " + requestedSkus.size());
        System.out.println("Archivos XML netos: " + skusByFile.size());

        Set<Pair> pairs = new LinkedHashSet<>();
        Set<String> foundSkus = new LinkedHashSet<>();

        for (Map.Entry<Path, Set<String>> entry : skusByFile.entrySet()) {
            Path xmlPath = entry.getKey();
            Set<String> skusForFile = entry.getValue();

            if (!Files.exists(xmlPath)) {
                System.out.println("No existe XML: " + xmlPath);
                continue;
            }

            System.out.println("Leyendo: " + xmlPath + " | SKUs esperados: " + skusForFile.size());

            extractPairsFromXml(xmlPath, skusForFile, pairs, foundSkus);
        }

        writeOutput(outputPath, pairs);

        System.out.println("Pares MATNR,SATNR encontrados: " + pairs.size());
        System.out.println("SKUs encontrados en XML: " + foundSkus.size());
        System.out.println("Salida: " + outputPath.toAbsolutePath());

        Set<String> missing = new LinkedHashSet<>();
        for (String sku : requestedSkus) {
            if (!foundSkus.contains(normalizeSku(sku))) {
                missing.add(sku);
            }
        }

        if (!missing.isEmpty()) {
            System.out.println("SKUs no encontrados en XML: " + missing.size());
            int printed = 0;
            for (String sku : missing) {
                System.out.println("NO_ENCONTRADO_XML: " + sku);
                printed++;
                if (printed >= 50) {
                    System.out.println("...");
                    break;
                }
            }
        }
    }

    private Set<String> readSkuList(Path skusPath) throws IOException {
        Set<String> skus = new LinkedHashSet<>();

        try (BufferedReader br = Files.newBufferedReader(skusPath, StandardCharsets.UTF_8)) {
            String line;

            while ((line = br.readLine()) != null) {
                String sku = firstColumn(line);
                sku = clean(sku);

                if (sku.length() == 0) {
                    continue;
                }

                if ("SKU".equalsIgnoreCase(sku) || "MATNR".equalsIgnoreCase(sku)) {
                    continue;
                }

                skus.add(sku);
            }
        }

        return skus;
    }

    private Map<Path, Set<String>> resolveFilesFromIndex(Set<String> requestedSkus, Path indexPath) throws IOException {
        Map<String, String> normalizedToOriginal = new LinkedHashMap<>();
        Set<String> pending = new LinkedHashSet<>();

        for (String sku : requestedSkus) {
            String normalized = normalizeSku(sku);
            normalizedToOriginal.put(normalized, sku);
            pending.add(normalized);
        }

        Map<Path, Set<String>> skusByFile = new LinkedHashMap<>();

        try (BufferedReader br = Files.newBufferedReader(indexPath, StandardCharsets.UTF_8)) {
            String line;

            while ((line = br.readLine()) != null && !pending.isEmpty()) {
                if (line.trim().length() == 0) {
                    continue;
                }

                String[] pieces = line.split(",", 4);

                if (pieces.length < 4) {
                    continue;
                }

                String indexSku = clean(pieces[0]);

                if ("SKU".equalsIgnoreCase(indexSku) || "MATNR".equalsIgnoreCase(indexSku)) {
                    continue;
                }

                String normalizedIndexSku = normalizeSku(indexSku);

                if (!pending.contains(normalizedIndexSku)) {
                    continue;
                }

                Path xmlPath = Paths.get(clean(pieces[3]));

                Set<String> skus = skusByFile.get(xmlPath);

                if (skus == null) {
                    skus = new LinkedHashSet<>();
                    skusByFile.put(xmlPath, skus);
                }

                skus.add(normalizedIndexSku);
                pending.remove(normalizedIndexSku);
            }
        }

        return skusByFile;
    }

    private void extractPairsFromXml(
            Path xmlPath,
            Set<String> normalizedSkusForFile,
            Set<Pair> pairs,
            Set<String> foundSkus
    ) throws Exception {
        SAXParser parser = newSaxParser();
        ECC122ResponseHandler handler = new ECC122ResponseHandler();

        try {
            parser.parse(xmlPath.toFile(), handler);
        } catch (NullPointerException e) {
            System.out.println("XML malformado o handler tronó con NullPointer: " + xmlPath);
            return;
        } catch (SAXException e) {
            System.out.println("XML inválido: " + xmlPath);
            throw e;
        }

        java.util.LinkedList<Product122> products = handler.getCollected();

        if (products == null) {
            return;
        }

        for (Product122 product : products) {
            String matnr = "";
            String smatnr = "";
            String satnr = "";

            for (Value value : product.getValues()) {
                String attributeId = clean(value.getAttributeId());
                String text = clean(value.getText());

                if ("MATNR".equals(attributeId)) {
                    matnr = text;
                } else if ("SMATNR".equals(attributeId)) {
                    smatnr = text;
                } else if ("SATNR".equals(attributeId)) {
                    satnr = text;
                }
            }

            if (matnr.length() == 0) {
                matnr = smatnr;
            }

            String normalizedMatnr = normalizeSku(matnr);

            if (normalizedSkusForFile.contains(normalizedMatnr)) {
                pairs.add(new Pair(matnr, satnr));
                foundSkus.add(normalizedMatnr);
            }
        }
    }

    private SAXParser newSaxParser() throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);

        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {
        }

        return factory.newSAXParser();
    }

    private void writeOutput(Path outputPath, Set<Pair> pairs) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            bw.write("MATNR,SATNR");
            bw.newLine();

            for (Pair pair : pairs) {
                bw.write(csv(pair.matnr));
                bw.write(",");
                bw.write(csv(pair.satnr));
                bw.newLine();
            }
        }
    }

    private static String firstColumn(String line) {
        if (line == null) {
            return "";
        }

        int comma = line.indexOf(',');
        int semicolon = line.indexOf(';');
        int tab = line.indexOf('\t');

        int cut = -1;

        if (comma >= 0) {
            cut = comma;
        }

        if (semicolon >= 0 && (cut < 0 || semicolon < cut)) {
            cut = semicolon;
        }

        if (tab >= 0 && (cut < 0 || tab < cut)) {
            cut = tab;
        }

        if (cut >= 0) {
            return line.substring(0, cut);
        }

        return line;
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }

        value = value.trim();

        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }

        return value.trim();
    }

    private static String normalizeSku(String sku) {
        sku = clean(sku);

        if (sku.length() == 0) {
            return "";
        }

        String normalized = sku.replaceFirst("^0+", "");

        if (normalized.length() == 0) {
            return "0";
        }

        return normalized;
    }

    private static String csv(String value) {
        value = value == null ? "" : value;

        boolean needsQuotes =
                value.indexOf(',') >= 0
             || value.indexOf('"') >= 0
             || value.indexOf('\\') >= 0
             || value.indexOf('\n') >= 0
             || value.indexOf('\r') >= 0
             || !value.equals(value.trim());

        if (!needsQuotes) {
            return value;
        }

        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static final class Pair {
        private final String matnr;
        private final String satnr;

        private Pair(String matnr, String satnr) {
            this.matnr = matnr == null ? "" : matnr;
            this.satnr = satnr == null ? "" : satnr;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Pair)) {
                return false;
            }

            Pair other = (Pair) obj;

            return this.matnr.equals(other.matnr)
                && this.satnr.equals(other.satnr);
        }

        @Override
        public int hashCode() {
            int result = matnr.hashCode();
            result = 31 * result + satnr.hashCode();
            return result;
        }
    }
}