package mx.com.liverpool.p360.services.core.sftp;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.core.sftp.handlers.ECC122ResponseHandler;
import mx.com.liverpool.p360.services.core.sftp.handlers.Product122;
import mx.com.liverpool.p360.services.core.sftp.handlers.Value;

public class LocalIndexECCFiles {

    private static final RESTWrapper rw = new RESTWrapper();

    private static final Pattern FILE_PATTERN = Pattern.compile(
            "^GenericXMLproducts(\\d{14})\\.XML$",
            Pattern.CASE_INSENSITIVE
    );

    private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final Map<String, LatestSkuFile> latestFileBySku = new TreeMap<>();

    public static void main(String[] args) throws ServiceUnavailableException {
        if (args == null || args.length < 1) {
            System.out.println("Uso: java ... LocalIndexJanaFiles <directorio_o_archivo> [archivo_salida]");
            return;
        }

        java.nio.file.Path input = java.nio.file.Paths.get(args[0]);
        java.nio.file.Path output = java.nio.file.Paths.get(args.length >= 2 ? args[1] : "IxECC");

        try {
            processPath(input);
            writeOutput(output);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Terminé. SKUs encontrados: " + latestFileBySku.size());
    }

    private static void processPath(java.nio.file.Path path) throws IOException {
        if (path == null) {
            return;
        }

        if (java.nio.file.Files.isDirectory(path)) {
            try (Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(path)) {
                stream
                    .filter(java.nio.file.Files::isRegularFile)
                    .forEach(LocalIndexECCFiles::processFileSafe);
            }
        } else if (java.nio.file.Files.isRegularFile(path)) {
            processFileSafe(path);
        }
    }

    private static void processFileSafe(java.nio.file.Path path) {
        try {
            processFile(path);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            System.out.println("Error procesando archivo: " + path);
            e.printStackTrace();
        }
    }

    public static void processFile(java.nio.file.Path path) throws ParserConfigurationException, SAXException, IOException {
        if (path == null || path.getFileName() == null) {
            return;
        }

        String fileName = path.getFileName().toString();
        Matcher matcher = FILE_PATTERN.matcher(fileName);

        if (!matcher.matches()) {
            return;
        }

        String fileTimestampText = matcher.group(1);
        LocalDateTime fileTimestamp = LocalDateTime.parse(fileTimestampText, FILE_TIMESTAMP_FORMAT);

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);

        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {
        }

        SAXParser parser = factory.newSAXParser();
        ECC122ResponseHandler handler = new ECC122ResponseHandler();

        try {
            parser.parse(path.toFile(), handler);
        } catch (NullPointerException e) {
            return;
        }

        java.util.LinkedList<Product122> products = handler.getCollected();

        if (products == null) {
            System.out.println("Malformed file content: " + path);
            return;
        }

        for (Product122 product : products) {
            java.util.LinkedList<Value> values = product.getValues();

            if (values == null) {
                continue;
            }

            for (Value value : values) {
                if (!"MATNR".equals(value.getAttributeId())) {
                    continue;
                }

                String sku = value.getText();

                if (sku == null) {
                    continue;
                }

                sku = sku.trim();

                if (sku.length() == 0) {
                    continue;
                }

                registerSkuFile(sku, product.getProposalId(), path, fileName, fileTimestampText, fileTimestamp);
            }
        }
    }

    private static void registerSkuFile(
            String sku,
            String znprst,
            java.nio.file.Path path,
            String fileName,
            String fileTimestampText,
            LocalDateTime fileTimestamp
    ) {
        LatestSkuFile current = latestFileBySku.get(sku);

        if (current == null) {
            latestFileBySku.put(sku, new LatestSkuFile(znprst, fileName, path, fileTimestampText, fileTimestamp));
            return;
        }

        if (fileTimestamp.isAfter(current.fileTimestamp)) {
            latestFileBySku.put(sku, new LatestSkuFile(znprst, fileName, path, fileTimestampText, fileTimestamp));
            return;
        }

        if (fileTimestamp.equals(current.fileTimestamp) && path.toString().compareTo(current.path.toString()) > 0) {
            latestFileBySku.put(sku, new LatestSkuFile(znprst, fileName, path, fileTimestampText, fileTimestamp));
        }
    }

    private static void writeOutput(java.nio.file.Path output) throws IOException {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream(output.toFile())
                )
        )) {
            latestFileBySku.entrySet().forEach(entry -> {
                LatestSkuFile hit = entry.getValue();

                pw.println(
                        rw.getRw().serializeChunk(
                                new Object[] {
                                        entry.getKey(),
                                        hit.productId,
                                        hit.fileTimestampText,
                                        hit.fileName,
                                        hit.path.toString()
                                }
                        )
                );
            });
        }
    }

    private static final class LatestSkuFile {
    	private final String productId;
        private final String fileName;
        private final java.nio.file.Path path;
        private final String fileTimestampText;
        private final LocalDateTime fileTimestamp;

        private LatestSkuFile(
        		String productId,
                String fileName,
                java.nio.file.Path path,
                String fileTimestampText,
                LocalDateTime fileTimestamp
        ) {
        	this.productId = productId;
            this.fileName = fileName;
            this.path = path;
            this.fileTimestampText = fileTimestampText;
            this.fileTimestamp = fileTimestamp;
        }
    }
}