package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

/**
 * Lee un CSV ordenado por ProductNo y publica mensajes a Pub/Sub en lotes.
 *
 * Estructura publicada:
 * {
 *   "products": [
 *     {
 *       "proposalId": "1000534",
 *       "variants": [
 *         {
 *           "variantId": "1000893",
 *           "Color": "0001",
 *           "TamanoUnico": "25.5"
 *         }
 *       ]
 *     }
 *   ]
 * }
 *
 * Las filas cuya columna TamanoUnico esté vacía no se incluyen.
 * El archivo no se carga completo en memoria: solamente se conserva el
 * ProductNo actual y un lote de hasta batchSize propuestas.
 */
public class ProposalVariantsCsvPubSub {

    private static final int DEFAULT_BATCH_SIZE = 1000;

    private static final String COLUMN_VARIANT_ID = "VariantIdentifier";
    private static final String COLUMN_SIZE = "TamanoUnico";
    private static final String COLUMN_COLOR = "Color";
    private static final String COLUMN_PRODUCT_NO = "ProductNo";

    private static final String JSON_PRODUCTS = "products";
    private static final String JSON_PROPOSAL_ID = "proposalId";
    private static final String JSON_VARIANTS = "variants";
    private static final String JSON_VARIANT_ID = "variantId";
    private static final String JSON_COLOR = "Color";
    private static final String JSON_SIZE = "TamanoUnico";

    private final Path input;
    private final int batchSize;
    private final PubSubGCP publisher;

    private final Map<String, Integer> columnIndexes = new HashMap<>();

    private boolean headerProcessed;
    private long logicalRecordNumber;
    private long dataRowsRead;
    private long variantsIncluded;
    private long rowsSkippedWithoutSize;
    private long proposalsPublished;
    private long batchesPublished;

    private String currentProductNo;
    private JSONArray currentVariants = new JSONArray();
    private JSONArray batchProducts = new JSONArray();

    public ProposalVariantsCsvPubSub(Path input, int batchSize) {
        if (input == null) {
            throw new IllegalArgumentException("El archivo de entrada no puede ser null.");
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("El batch size debe ser mayor que cero.");
        }

        this.input = input;
        this.batchSize = batchSize;
        this.publisher = new PubSubGCP(
                PropertiesManager.get("p360.contingency.gcp.service_account_back"),
                PropertiesManager.get("p360.contingency.gcp.project_back"),
                PropertiesManager.get("p360.contingency.gcp.post_products_topic")
        );
    }

    public void process() {
        validateInput();

        SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
                '"',
                ',',
                '\\',
                "\n",
                StandardCharsets.UTF_8,
                this::processRecord
        );

        parser.parse(input);

        if (!headerProcessed) {
            throw new IllegalStateException("El archivo está vacío o no contiene encabezado.");
        }

        finishCurrentProposal();
        publishBatch();
        printSummary();
    }

    private void processRecord(String[] values) {
        logicalRecordNumber++;

        if (isBlankRecord(values)) {
            return;
        }

        if (!headerProcessed) {
            processHeader(values);
            headerProcessed = true;
            return;
        }

        dataRowsRead++;
        validateRecordLength(values);

        String productNo = requiredValue(values, COLUMN_PRODUCT_NO);
        String variantId = requiredValue(values, COLUMN_VARIANT_ID);
        String size = value(values, COLUMN_SIZE);
        String color = value(values, COLUMN_COLOR);

        if (currentProductNo == null) {
            currentProductNo = productNo;
        } else if (!currentProductNo.equals(productNo)) {
            finishCurrentProposal();
            currentProductNo = productNo;
        }

        if (size.isEmpty()) {
            rowsSkippedWithoutSize++;
            return;
        }

        JSONObject variant = new JSONObject()
                .put(JSON_VARIANT_ID, variantId)
                .put(JSON_COLOR, color)
                .put(JSON_SIZE, size);

        currentVariants.put(variant);
        variantsIncluded++;
    }

    private void processHeader(String[] header) {
        for (int i = 0; i < header.length; i++) {
            String columnName = cleanHeaderValue(header[i]);
            if (columnName.isEmpty()) {
                continue;
            }
            if (columnIndexes.put(columnName, i) != null) {
                throw new IllegalArgumentException(
                        "El encabezado contiene duplicada la columna '" + columnName + "'."
                );
            }
        }

        requireColumn(COLUMN_VARIANT_ID);
        requireColumn(COLUMN_SIZE);
        requireColumn(COLUMN_COLOR);
        requireColumn(COLUMN_PRODUCT_NO);
    }

    private void finishCurrentProposal() {
        if (currentProductNo == null) {
            return;
        }

        if (currentVariants.length() > 0) {
            JSONObject product = new JSONObject()
                    .put(JSON_PROPOSAL_ID, currentProductNo)
                    .put(JSON_VARIANTS, currentVariants);

            batchProducts.put(product);

            if (batchProducts.length() == batchSize) {
                publishBatch();
            }
        }

        currentVariants = new JSONArray();
    }

    private void publishBatch() {
        if (batchProducts.length() == 0) {
            return;
        }

        int proposalsInBatch = batchProducts.length();
        JSONObject body = new JSONObject().put(JSON_PRODUCTS, batchProducts);

        publisher.publishMessage(body.toString());

        batchesPublished++;
        proposalsPublished += proposalsInBatch;

        System.out.println(
                "Batch " + batchesPublished
                        + " publicado: " + proposalsInBatch
                        + " propuestas; total publicado: " + proposalsPublished
                        + "; variantes incluidas: " + variantsIncluded
        );

        batchProducts = new JSONArray();
    }

    private String requiredValue(String[] values, String columnName) {
        String result = value(values, columnName);
        if (result.isEmpty()) {
            throw new IllegalArgumentException(
                    "Valor vacío para '" + columnName + "' en el registro lógico "
                            + logicalRecordNumber + "."
            );
        }
        return result;
    }

    private String value(String[] values, String columnName) {
        Integer index = columnIndexes.get(columnName);
        if (index == null) {
            throw new IllegalStateException("No se encontró el índice de la columna '" + columnName + "'.");
        }
        return cleanValue(values[index]);
    }

    private void validateRecordLength(String[] values) {
        int greatestRequiredIndex = Math.max(
                Math.max(columnIndexes.get(COLUMN_VARIANT_ID), columnIndexes.get(COLUMN_SIZE)),
                Math.max(columnIndexes.get(COLUMN_COLOR), columnIndexes.get(COLUMN_PRODUCT_NO))
        );

        if (values.length <= greatestRequiredIndex) {
            throw new IllegalArgumentException(
                    "El registro lógico " + logicalRecordNumber
                            + " tiene " + values.length
                            + " columnas y no alcanza la posición requerida "
                            + (greatestRequiredIndex + 1) + "."
            );
        }
    }

    private void requireColumn(String columnName) {
        if (!columnIndexes.containsKey(columnName)) {
            throw new IllegalArgumentException(
                    "No se encontró la columna obligatoria '" + columnName + "' en el encabezado."
            );
        }
    }

    private boolean isBlankRecord(String[] values) {
        if (values == null || values.length == 0) {
            return true;
        }
        for (String value : values) {
            if (!cleanValue(value).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String cleanHeaderValue(String value) {
        String result = cleanValue(value);
        if (!result.isEmpty() && result.charAt(0) == '\uFEFF') {
            result = result.substring(1).trim();
        }
        return result;
    }

    private String cleanValue(String value) {
        if (value == null) {
            return "";
        }

        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '\r') {
            end--;
        }
        return value.substring(0, end).trim();
    }

    private void validateInput() {
        if (!Files.exists(input)) {
            throw new IllegalArgumentException("No existe el archivo: " + input);
        }
        if (!Files.isRegularFile(input)) {
            throw new IllegalArgumentException("La ruta no es un archivo regular: " + input);
        }
        if (!Files.isReadable(input)) {
            throw new IllegalArgumentException("No se puede leer el archivo: " + input);
        }
    }

    private void printSummary() {
        System.out.println("Proceso finalizado.");
        System.out.println("Archivo: " + input);
        System.out.println("Filas de datos leídas: " + dataRowsRead);
        System.out.println("Variantes incluidas: " + variantsIncluded);
        System.out.println("Filas omitidas por talla vacía: " + rowsSkippedWithoutSize);
        System.out.println("Propuestas publicadas: " + proposalsPublished);
        System.out.println("Batches publicados: " + batchesPublished);
    }

    private static void printUsage() {
        System.err.println(
                "Uso:\n"
                        + "  ProposalVariantsCsvPubSub --in <archivo.csv> [--batch-size <cantidad>]\n\n"
                        + "Ejemplo:\n"
                        + "  ProposalVariantsCsvPubSub --in /u01/stage/product_variants.csv --batch-size 1000"
        );
    }

    public static void main(String[] args) {
        try {
        	args = new String[] { "--in", "c:/opt/LVP/desorden/PROD/DataVariantes_20260717_080232.csv.sorted", "--batch-size", "1000" };
            Arguments arguments = Arguments.parse(args);
            new ProposalVariantsCsvPubSub(arguments.input, arguments.batchSize).process();
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace(System.err);
            printUsage();
            System.exit(1);
        }
    }

    private static final class Arguments {
        private final Path input;
        private final int batchSize;

        private Arguments(Path input, int batchSize) {
            this.input = input;
            this.batchSize = batchSize;
        }

        private static Arguments parse(String[] args) {
            Path input = null;
            int batchSize = DEFAULT_BATCH_SIZE;

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--in":
                        input = Paths.get(requireArgumentValue(args, ++i, "--in"));
                        break;
                    case "--batch-size":
                        String value = requireArgumentValue(args, ++i, "--batch-size");
                        try {
                            batchSize = Integer.parseInt(value);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException(
                                    "Valor inválido para --batch-size: " + value,
                                    e
                            );
                        }
                        break;
                    case "--help":
                    case "-h":
                        printUsage();
                        System.exit(0);
                        break;
                    default:
                        throw new IllegalArgumentException("Argumento no reconocido: " + args[i]);
                }
            }

            if (input == null) {
                throw new IllegalArgumentException("Falta el argumento obligatorio --in.");
            }

            return new Arguments(input, batchSize);
        }

        private static String requireArgumentValue(String[] args, int index, String argumentName) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Falta el valor de " + argumentName + ".");
            }
            return args[index];
        }
    }
}