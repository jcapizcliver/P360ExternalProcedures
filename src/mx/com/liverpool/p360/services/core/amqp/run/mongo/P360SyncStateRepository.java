package mx.com.liverpool.p360.services.core.amqp.run.mongo;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;

import mx.com.liverpool.p360.services.core.amqp.run.mongo.P360SemanticEvent.EntityType;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class P360SyncStateRepository {

    private final MongoCollection<Document> collection;

    public P360SyncStateRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public Document getAttributeSnapshot(
            EntityType entityType,
            String identifier,
            String characteristic,
            String recordKey,
            String language) {

        Document state = collection.find(eq("_id", attributeKey(
                entityType, identifier, characteristic, recordKey, language)))
                .first();

        return state == null ? null : copyDocument(state.get("snapshot"));
    }

    public void saveAttributeSnapshot(
            EntityType entityType,
            String identifier,
            String ownerProductId,
            String characteristic,
            String recordKey,
            String language,
            Document snapshot) {

        String id = attributeKey(entityType, identifier, characteristic, recordKey, language);
        Document state = new Document("_id", id)
                .append("entity", entityType.name())
                .append("identifier", identifier)
                .append("owner_product_id", ownerProductId)
                .append("kind", "ATTRIBUTE")
                .append("characteristic", characteristic)
                .append("record_key", normalize(recordKey))
                .append("language", normalize(language))
                .append("snapshot", snapshot == null ? null : new Document(snapshot));

        collection.replaceOne(eq("_id", id), state, new ReplaceOptions().upsert(true));
    }

    public void deleteAttributeSnapshot(
            EntityType entityType,
            String identifier,
            String characteristic,
            String recordKey,
            String language) {

        collection.deleteOne(eq("_id", attributeKey(
                entityType, identifier, characteristic, recordKey, language)));
    }

    public Document getImageSnapshot(
            EntityType entityType,
            String identifier,
            String characteristic,
            String recordKey) {

        Document state = collection.find(eq("_id", imageKey(
                entityType, identifier, characteristic, recordKey)))
                .first();

        return state == null ? null : copyDocument(state.get("snapshot"));
    }

    public void saveImageSnapshot(
            EntityType entityType,
            String identifier,
            String ownerProductId,
            String characteristic,
            String recordKey,
            Document snapshot) {

        String id = imageKey(entityType, identifier, characteristic, recordKey);
        Document state = new Document("_id", id)
                .append("entity", entityType.name())
                .append("identifier", identifier)
                .append("owner_product_id", ownerProductId)
                .append("kind", "IMAGE")
                .append("characteristic", characteristic)
                .append("record_key", normalize(recordKey))
                .append("snapshot", snapshot == null ? null : new Document(snapshot));

        collection.replaceOne(eq("_id", id), state, new ReplaceOptions().upsert(true));
    }

    public void deleteImageSnapshot(
            EntityType entityType,
            String identifier,
            String characteristic,
            String recordKey) {

        collection.deleteOne(eq("_id", imageKey(
                entityType, identifier, characteristic, recordKey)));
    }

    public List<Document> listSnapshots(EntityType entityType, String identifier, String kind) {
        Bson filter = and(
                eq("entity", entityType.name()),
                eq("identifier", identifier),
                eq("kind", kind));

        FindIterable<Document> iterable = collection.find(filter);
        List<Document> result = new ArrayList<>();
        for (Document state : iterable) {
            Document snapshot = copyDocument(state.get("snapshot"));
            if (snapshot != null) {
                result.add(snapshot);
            }
        }
        return result;
    }

    public void updateOwner(EntityType entityType, String identifier, String ownerProductId) {
        collection.updateMany(
                and(eq("entity", entityType.name()), eq("identifier", identifier)),
                new Document("$set", new Document("owner_product_id", ownerProductId)));
    }

    public void deleteEntityState(EntityType entityType, String identifier) {
        collection.deleteMany(and(
                eq("entity", entityType.name()),
                eq("identifier", identifier)));
    }

    private String attributeKey(
            EntityType entityType,
            String identifier,
            String characteristic,
            String recordKey,
            String language) {

        return String.join("|",
                entityType.name(),
                normalize(identifier),
                "ATTRIBUTE",
                normalize(characteristic),
                normalize(recordKey),
                normalize(language));
    }

    private String imageKey(
            EntityType entityType,
            String identifier,
            String characteristic,
            String recordKey) {

        return String.join("|",
                entityType.name(),
                normalize(identifier),
                "IMAGE",
                normalize(characteristic),
                normalize(recordKey));
    }

    private String normalize(String value) {
        return value == null ? "" : value;
    }

    @SuppressWarnings("unchecked")
    private Document copyDocument(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Document document) {
            return new Document(document);
        }
        if (value instanceof java.util.Map<?, ?> map) {
            return new Document((java.util.Map<String, Object>) map);
        }
        return null;
    }
}
