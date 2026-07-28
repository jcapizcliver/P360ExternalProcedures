package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class LoQueFaltaPubSubPOSTArticles {

    private static final PubSubGCP pubPostProducts = new PubSubGCP(
        PropertiesManager.get("p360.contingency.gcp.service_account_back"),
        PropertiesManager.get("p360.contingency.gcp.project_back"),
        PropertiesManager.get("p360.contingency.gcp.post_products_topic")
    );

    private static final int BATCH_SIZE = 200;

    private static int count = 0;
    private static boolean headerRead = false;

    private static final Map<String, Integer> headerIndex = new HashMap<String, Integer>();

    private static final JSONArray products = new JSONArray();
    private static final JSONObject body = new JSONObject().put("products", products);

    private static String currentProduct2GIdentifier = null;
    private static JSONObject currentProduct = null;
    private static JSONArray currentVariants = null;

    public static void main(String[] args) {

        SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
            '"',
            ',',
            '\\',
            "\n",
            StandardCharsets.UTF_8,
            row -> {
                if (row == null || row.length == 0) {
                    return;
                }

                if (!headerRead) {
                    loadHeader(row);
                    headerRead = true;
                    return;
                }

                String product2GIdentifier = get(row, "PRODUCT2GIDENTIFIER");
                String variantId = get(row, "p360variantId");

                String p360CurrentStatus = get(row, "p360currentStatus");

                String currentStatus = get(row, "CURRENTSTATUS");
                String previousStatus = get(row, "PREVSTATUS");
                String externalStatus = get(row, "EXTERNALSTATUS");

                /*
                 * Criterio:
                 * Entra solo cuando CURRENTSTATUS trae valor
                 * y difiere de p360currentStatus.
                 */
                if (isBlank(currentStatus) || currentStatus.equals(p360CurrentStatus)) {
                    return;
                }

                if (isBlank(product2GIdentifier) || isBlank(variantId)) {
                    return;
                }

                if (currentProduct2GIdentifier == null || !currentProduct2GIdentifier.equals(product2GIdentifier)) {
                    closeCurrentProductIfNeeded();

                    currentProduct2GIdentifier = product2GIdentifier;
                    currentVariants = new JSONArray();

                    currentProduct = new JSONObject()
                        .put("proposalId", product2GIdentifier)
                        .put("variants", currentVariants);
                }

                JSONObject variant = new JSONObject()
                    .put("variantId", variantId)
                    .put("currentStatus", currentStatus)
                    .put("previousStatus", previousStatus)
                    .put("externalStatus", externalStatus);

                if (count < 100) {
                    count++;
                }// else{ System.out.println(body); System.exit(0); }

                currentVariants.put(variant);
            }
        );

        parser.parse(Paths.get("/", "u01", "stage", "data", "OutputMissmatchForStatusEUC_P360_article.csv.sorted"));

        closeCurrentProductIfNeeded();

        if (products.length() > 0) {
            publishAndClear();
        }
    }

    private static void closeCurrentProductIfNeeded() {
        if (currentProduct == null) {
            return;
        }

        products.put(currentProduct);

        currentProduct = null;
        currentVariants = null;

        if (products.length() == BATCH_SIZE) {
            publishAndClear();
        }
    }

    private static void publishAndClear() {
        pubPostProducts.publishMessage(body.toString());

        while (products.length() > 0) {
            products.remove(0);
        }
    }

    private static void loadHeader(String[] row) {
        for (int i = 0; i < row.length; i++) {
            if (row[i] != null) {
                headerIndex.put(row[i].trim().toUpperCase(), i);
            }
        }
    }

    private static String get(String[] row, String columnName) {
        Integer idx = headerIndex.get(columnName.trim().toUpperCase());

        if (idx == null || idx < 0 || idx >= row.length || row[idx] == null) {
            return "";
        }

        return row[idx].trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}