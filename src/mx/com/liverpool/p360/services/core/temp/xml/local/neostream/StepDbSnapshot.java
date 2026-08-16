package mx.com.liverpool.p360.services.core.temp.xml.local.neostream;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;

/**
 * Request-scoped DB snapshot used while exactly one STEP file is processed.
 *
 * The snapshot deliberately does NOT preload full Product/Article data for
 * everything in the file. First it resolves SKU ownership in bulk, then it
 * asks for the expensive detail objects only for incoming identifiers that
 * actually collide with a different owner.
 */
public final class StepDbSnapshot {

    public final Map<String, String> productBySku;
    public final Map<String, String> articleBySku;

    /** Full data only for incoming Product2G identifiers involved in a collision. */
    public final Map<String, JSONObject> productData;

    /** Full data only for incoming Article identifiers involved in a collision. */
    public final Map<String, JSONObject> articleData;

    /** REST object id syntax: ArticleRevision.ArticleID + "@" + CatalogID. */
    public final Map<String, String> productObjectIdByIdentifier;
    public final Map<String, String> articleObjectIdByIdentifier;

    private StepDbSnapshot(
            Map<String, String> productBySku,
            Map<String, String> articleBySku,
            Map<String, JSONObject> productData,
            Map<String, JSONObject> articleData,
            Map<String, String> productObjectIdByIdentifier,
            Map<String, String> articleObjectIdByIdentifier) {
        this.productBySku = productBySku;
        this.articleBySku = articleBySku;
        this.productData = productData;
        this.articleData = articleData;
        this.productObjectIdByIdentifier = productObjectIdByIdentifier;
        this.articleObjectIdByIdentifier = articleObjectIdByIdentifier;
    }

    public static StepDbSnapshot load(
            DBAccessDataStub db,
            StepXmlStreamingParser.StepIndex index) {

        Map<String, String> productBySku =
                db.getProductsBySKUs(index.getProductSkus());
        Map<String, String> articleBySku =
                db.getArticlesBySKUs(index.getArticleSkus());

        Set<String> productCollisions = new LinkedHashSet<>();
        Set<String> articleCollisions = new LinkedHashSet<>();
        Set<String> productOwnersNeedingInternalId = new LinkedHashSet<>();
        Set<String> articleOwnersNeedingInternalId = new LinkedHashSet<>();

        for (Map.Entry<String, String> entry : index.getProductSkuById().entrySet()) {
            String incomingId = entry.getKey();
            String owner = productBySku.get(entry.getValue());
            if (owner != null && !owner.isBlank() && !owner.equals(incomingId)) {
                productCollisions.add(incomingId);
                if (owner.length() < 15) {
                    productOwnersNeedingInternalId.add(owner);
                }
            }
        }

        for (Map.Entry<String, String> entry : index.getArticleSkuById().entrySet()) {
            String incomingId = entry.getKey();
            String owner = articleBySku.get(entry.getValue());
            if (owner != null && !owner.isBlank() && !owner.equals(incomingId)) {
                articleCollisions.add(incomingId);
                if (owner.length() < 15) {
                    articleOwnersNeedingInternalId.add(owner);
                }
            }
        }

        Map<String, JSONObject> productData =
                db.getProductData(productCollisions);
        Map<String, JSONObject> articleData =
                db.getArticleData(articleCollisions);

        Map<String, String> productObjectIds =
                db.getObjectInternalIds(1100, productOwnersNeedingInternalId);
        Map<String, String> articleObjectIds =
                db.getObjectInternalIds(1000, articleOwnersNeedingInternalId);

        return new StepDbSnapshot(
                productBySku,
                articleBySku,
                productData,
                articleData,
                productObjectIds,
                articleObjectIds);
    }
}
