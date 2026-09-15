package mx.com.liverpool.p360.services.core.amqp.run.mongo;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;

import static com.mongodb.client.model.Filters.eq;

public class P360PendingEventRepository {

    private final MongoCollection<Document> collection;

    public P360PendingEventRepository(MongoCollection<Document> collection) {
        this.collection = collection;
    }

    public void save(P360SemanticEvent event, String rawJson) {
        if (event == null || event.getIdentifier() == null || rawJson == null) {
            return;
        }

        String id = sha256(rawJson);
        Document pending = new Document("_id", id)
                .append("entity", event.getEntityType().name())
                .append("identifier", event.getIdentifier())
                .append("event_timestamp", event.getEventTimestamp())
                .append("payload", rawJson);

        collection.replaceOne(eq("_id", id), pending, new ReplaceOptions().upsert(true));
    }

    public List<PendingEvent> findByIdentifier(String identifier) {
        List<PendingEvent> result = new ArrayList<>();
        for (Document document : collection.find(eq("identifier", identifier))
                .sort(Sorts.ascending("event_timestamp", "_id"))) {

            result.add(new PendingEvent(
                    document.getString("_id"),
                    document.getString("payload")));
        }
        return result;
    }

    public void delete(String id) {
        if (id != null) {
            collection.deleteOne(eq("_id", id));
        }
    }

    public void deleteByIdentifier(String identifier) {
        if (identifier != null) {
            collection.deleteMany(eq("identifier", identifier));
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible generar la llave del evento pendiente", e);
        }
    }

    public static class PendingEvent {
        private final String id;
        private final String payload;

        public PendingEvent(String id, String payload) {
            this.id = id;
            this.payload = payload;
        }

        public String getId() {
            return id;
        }

        public String getPayload() {
            return payload;
        }
    }
}
