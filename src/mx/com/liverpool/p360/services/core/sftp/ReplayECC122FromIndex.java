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

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

public class ReplayECC122FromIndex {

    private static final String DEFAULT_REPORT = "ReplayECC122FromIndex_report.csv";

    public static void main(String[] args) throws ServiceUnavailableException {
        if (args == null || args.length < 2) {
            System.out.println("Uso:");
            System.out.println("java ... mx.com.liverpool.p360.services.core.sftp.ReplayECC122FromIndex <lista_skus> <IxECC> [reporte_salida]");
            return;
        }

        Path skuListPath = Paths.get(args[0]);
        Path indexPath = Paths.get(args[1]);
        Path reportPath = Paths.get(args.length >= 3 ? args[2] : DEFAULT_REPORT);

        try {
            new ReplayECC122FromIndex().run(skuListPath, indexPath, reportPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run(Path skuListPath, Path indexPath, Path reportPath) throws IOException, ServiceUnavailableException {
        Map<String, Resolution> resolutions = new LinkedHashMap<>();
        Map<Path, Set<String>> skusByFile = resolveFilesStreaming(skuListPath, indexPath, resolutions);

        writeResolutionReport(reportPath, resolutions);

        System.out.println("SKUs solicitados: " + resolutions.size());
        System.out.println("SKUs ubicados: " + resolutions.values().stream().filter(Resolution::isFound).count());
        System.out.println("Archivos netos a procesar: " + skusByFile.size());
        System.out.println("Reporte: " + reportPath.toAbsolutePath());

        if (skusByFile.isEmpty()) {
            System.out.println("No hay archivos para procesar.");
            return;
        }

        ParseECC122Response parser = new ParseECC122Response();

        System.out.println("Preparando caches/material ECC...");
        parser.prepareLocalReplay();

        int processedFiles = 0;

        for (Map.Entry<Path, Set<String>> entry : skusByFile.entrySet()) {
            Path xmlPath = entry.getKey();

            System.out.println("Procesando archivo: " + xmlPath + " | SKUs solicitados en este archivo: " + entry.getValue().size());

            try {
                parser.processFile(xmlPath, null, null);
                processedFiles++;
            } catch (ParserConfigurationException | SAXException | IOException e) {
                System.out.println("Error procesando archivo: " + xmlPath);
                e.printStackTrace();
            }
        }

        parser.flushPendingWrites();

        System.out.println("Terminé. Archivos procesados: " + processedFiles);
    }

    private Map<Path, Set<String>> resolveFilesStreaming(
            Path skuListPath,
            Path indexPath,
            Map<String, Resolution> resolutions
    ) throws IOException {
        Set<String> requestedSkus = readSkuList(skuListPath);
        Set<String> pendingSkus = new LinkedHashSet<>(requestedSkus);
        Map<String, String> normalizedToOriginal = new LinkedHashMap<>();

        for (String sku : requestedSkus) {
            normalizedToOriginal.put(normalizeSku(sku), sku);
            resolutions.put(sku, Resolution.notFound(sku));
        }

        Map<Path, Set<String>> skusByFile = new LinkedHashMap<>();

        try (BufferedReader br = Files.newBufferedReader(indexPath, StandardCharsets.UTF_8)) {
            String line;

            while ((line = br.readLine()) != null && !pendingSkus.isEmpty()) {
                if (line.trim().length() == 0) {
                    continue;
                }

                String[] pieces = line.split(",", 4);

                if (pieces.length < 4) {
                    continue;
                }

                String indexSku = clean(pieces[0]);
                String normalizedIndexSku = normalizeSku(indexSku);

                String matchedInputSku = null;

                if (pendingSkus.contains(indexSku)) {
                    matchedInputSku = indexSku;
                } else {
                    String originalSku = normalizedToOriginal.get(normalizedIndexSku);

                    if (originalSku != null && pendingSkus.contains(originalSku)) {
                        matchedInputSku = originalSku;
                    }
                }

                if (matchedInputSku == null) {
                    continue;
                }

                String timestamp = clean(pieces[1]);
                String fileName = clean(pieces[2]);
                Path fullPath = Paths.get(clean(pieces[3]));

                IndexHit hit = new IndexHit(indexSku, normalizedIndexSku, timestamp, fileName, fullPath);

                if (!Files.exists(fullPath)) {
                    resolutions.put(matchedInputSku, Resolution.fileNotFound(matchedInputSku, hit));
                    pendingSkus.remove(matchedInputSku);
                    continue;
                }

                resolutions.put(matchedInputSku, Resolution.found(matchedInputSku, hit));

                Set<String> skusInFile = skusByFile.get(fullPath);

                if (skusInFile == null) {
                    skusInFile = new LinkedHashSet<>();
                    skusByFile.put(fullPath, skusInFile);
                }

                skusInFile.add(matchedInputSku);
                pendingSkus.remove(matchedInputSku);
            }
        }

        return skusByFile;
    }

    private Set<String> readSkuList(Path skuListPath) throws IOException {
        Set<String> skus = new LinkedHashSet<>();

        try (BufferedReader br = Files.newBufferedReader(skuListPath, StandardCharsets.UTF_8)) {
            String line;

            while ((line = br.readLine()) != null) {
                String sku = firstColumn(line);

                if (sku == null) {
                    continue;
                }

                sku = clean(sku);

                if (sku.length() == 0) {
                    continue;
                }

                if ("SKU".equalsIgnoreCase(sku)) {
                    continue;
                }

                skus.add(sku);
            }
        }

        return skus;
    }

    private void writeResolutionReport(Path reportPath, Map<String, Resolution> resolutions) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8)) {
            bw.write("InputSKU,IndexSKU,Timestamp,FileName,FullPath,Status,Message");
            bw.newLine();

            for (Resolution resolution : resolutions.values()) {
                bw.write(csv(resolution.inputSku));
                bw.write(",");
                bw.write(csv(resolution.indexSku));
                bw.write(",");
                bw.write(csv(resolution.timestamp));
                bw.write(",");
                bw.write(csv(resolution.fileName));
                bw.write(",");
                bw.write(csv(resolution.fullPath));
                bw.write(",");
                bw.write(csv(resolution.status));
                bw.write(",");
                bw.write(csv(resolution.message));
                bw.newLine();
            }
        }
    }

    private static String firstColumn(String line) {
        if (line == null) {
            return null;
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
        if (value == null) {
            value = "";
        }

        boolean needsQuotes =
                value.indexOf(',') >= 0
             || value.indexOf('"') >= 0
             || value.indexOf('\n') >= 0
             || value.indexOf('\r') >= 0;

        if (!needsQuotes) {
            return value;
        }

        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static final class IndexHit {
        private final String sku;
        private final String normalizedSku;
        private final String timestamp;
        private final String fileName;
        private final Path fullPath;

        private IndexHit(String sku, String normalizedSku, String timestamp, String fileName, Path fullPath) {
            this.sku = sku;
            this.normalizedSku = normalizedSku;
            this.timestamp = timestamp;
            this.fileName = fileName;
            this.fullPath = fullPath;
        }
    }

    private static final class Resolution {
        private final String inputSku;
        private final String indexSku;
        private final String timestamp;
        private final String fileName;
        private final String fullPath;
        private final String status;
        private final String message;

        private Resolution(String inputSku, String indexSku, String timestamp, String fileName, String fullPath, String status, String message) {
            this.inputSku = inputSku;
            this.indexSku = indexSku;
            this.timestamp = timestamp;
            this.fileName = fileName;
            this.fullPath = fullPath;
            this.status = status;
            this.message = message;
        }

        private static Resolution found(String inputSku, IndexHit hit) {
            return new Resolution(
                    inputSku,
                    hit.sku,
                    hit.timestamp,
                    hit.fileName,
                    hit.fullPath.toString(),
                    "FOUND",
                    "Archivo ubicado"
            );
        }

        private static Resolution notFound(String inputSku) {
            return new Resolution(
                    inputSku,
                    "",
                    "",
                    "",
                    "",
                    "INDEX_NOT_FOUND",
                    "SKU no encontrado en índice ECC"
            );
        }

        private static Resolution fileNotFound(String inputSku, IndexHit hit) {
            return new Resolution(
                    inputSku,
                    hit.sku,
                    hit.timestamp,
                    hit.fileName,
                    hit.fullPath.toString(),
                    "FILE_NOT_FOUND",
                    "El SKU existe en índice ECC, pero el XML no existe en filesystem"
            );
        }

        private boolean isFound() {
            return "FOUND".equals(status);
        }
    }
}