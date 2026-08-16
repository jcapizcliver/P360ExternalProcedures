package mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;

/** Request-scoped DB snapshot used while one STEP file is being processed. */
public final class StepDbSnapshot {

    public final Map<String, String> productBySku;
    public final Map<String, String> articleBySku;
    public final Map<String, JSONObject> productData;
    public final Map<String, JSONObject> articleData;

    private StepDbSnapshot(
            Map<String, String> productBySku,
            Map<String, String> articleBySku,
            Map<String, JSONObject> productData,
            Map<String, JSONObject> articleData) {
        this.productBySku = productBySku;
        this.articleBySku = articleBySku;
        this.productData = productData;
        this.articleData = articleData;
    }

    public static StepDbSnapshot load(
            DBAccessDataStub db,
            StepXmlStreamingParser.StepIndex index) {

        Map<String, String> productBySku = db.getProductsBySKUs(index.getProductSkus());
        Map<String, String> articleBySku = db.getArticlesBySKUs(index.getArticleSkus());

        Set<String> productIds = new LinkedHashSet<>(index.getProductIds());
        productIds.addAll(productBySku.values());

        Set<String> articleIds = new LinkedHashSet<>(index.getArticleIds());
        articleIds.addAll(articleBySku.values());

        Map<String, JSONObject> productData = db.getProductData(productIds);
        Map<String, JSONObject> articleData = db.getArticleData(articleIds);

        return new StepDbSnapshot(
                productBySku,
                articleBySku,
                productData,
                articleData);
    }
}
