package mx.com.liverpool.p360.services.core.amqp.run.mongo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import mx.com.liverpool.p360.services.core.amqp.run.mongo.P360MongoRepository.ApplyResult;
import mx.com.liverpool.p360.services.core.amqp.run.mongo.P360PendingEventRepository.PendingEvent;
import mx.com.liverpool.p360.services.core.amqp.run.mongo.P360SemanticEvent.EntityType;
import mx.com.liverpool.p360.services.core.amqp.run.mongo.P360SemanticEvent.ParentRelationAction;

public class P360SyncService {

    private final ObjectMapper objectMapper;
    private final P360ChangeSummaryParser parser;
    private final P360MongoRepository repository;
    private final P360PendingEventRepository pending;

    private final Set<String> replayingArticles = new HashSet<>();

    public P360SyncService(
            ObjectMapper objectMapper,
            P360ChangeSummaryParser parser,
            P360MongoRepository repository,
            P360PendingEventRepository pending) {
        this.objectMapper = objectMapper;
        this.parser = parser;
        this.repository = repository;
        this.pending = pending;
    }

    /**
     * Acepta JSON puro o una línea de log que contenga "A message body: {...}".
     * Devuelve el número de eventos semánticos aplicados a Mongo.
     */
    public int process(String raw) throws Exception {
        String jsonText = extractJson(raw);
        if (jsonText == null) {
            return 0;
        }
        return processJson(jsonText, true, true);
    }

    private int processJson(
            String jsonText,
            boolean allowPendingStore,
            boolean allowPendingReplay) throws Exception {

        JsonNode root = objectMapper.readTree(jsonText);

        if (root.has("entityItemChange")) {
            JsonNode envelope = root.get("entityItemChange");
            P360SemanticEvent event = parser.parse(envelope);

            if (!event.hasSemanticChanges()) {
                return 0;
            }

            ApplyResult result = repository.apply(event);

            if (result == ApplyResult.DEFERRED) {
                if (allowPendingStore && event.getEntityType() == EntityType.ARTICLE) {
                    pending.save(event, jsonText);
                }
                return 0;
            }

            if (result == ApplyResult.IGNORED) {
                return 0;
            }

            int applied = 1;

            if (allowPendingReplay
                    && event.getEntityType() == EntityType.ARTICLE
                    && event.getParentRelationAction() == ParentRelationAction.UPSERT) {
                applied += replayPending(event.getIdentifier());
            }

            return applied;
        }

        if (root.has("entityItemsDeleted")) {
            return processEntityItemsDeleted(root.get("entityItemsDeleted"));
        }

        return 0;
    }

    private int processEntityItemsDeleted(JsonNode deleted) {
        String entity = text(deleted, "_entity");
        EntityType entityType;

        if ("Product2G".equals(entity)) {
            entityType = EntityType.PRODUCT2G;
        } else if ("Article".equals(entity)) {
            entityType = EntityType.ARTICLE;
        } else {
            return 0;
        }

        JsonNode identifiers = deleted.get("_identifier");
        if (identifiers == null || identifiers.isNull()) {
            return 0;
        }

        int count = 0;

        if (identifiers.isArray()) {
            for (JsonNode value : identifiers) {
                String identifier = value.asText(null);
                if (identifier != null && !identifier.isBlank()) {
                    repository.deleteEntity(entityType, identifier);
                    if (entityType == EntityType.ARTICLE) {
                        pending.deleteByIdentifier(identifier);
                    }
                    count++;
                }
            }
        } else {
            String identifier = identifiers.asText(null);
            if (identifier != null && !identifier.isBlank()) {
                repository.deleteEntity(entityType, identifier);
                if (entityType == EntityType.ARTICLE) {
                    pending.deleteByIdentifier(identifier);
                }
                count++;
            }
        }

        return count;
    }

    private int replayPending(String articleId) throws Exception {
        if (articleId == null || !replayingArticles.add(articleId)) {
            return 0;
        }

        int applied = 0;
        try {
            List<PendingEvent> events = pending.findByIdentifier(articleId);
            for (PendingEvent pendingEvent : events) {
                int result = processJson(pendingEvent.getPayload(), false, false);
                if (result > 0) {
                    pending.delete(pendingEvent.getId());
                    applied += result;
                }
                // Si siguiera DEFERRED, se conserva para un evento posterior.
            }
        } finally {
            replayingArticles.remove(articleId);
        }
        return applied;
    }

    private String extractJson(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        String trimmed = raw.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed;
        }

        int begin = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (begin < 0 || end <= begin) {
            return null;
        }
        return raw.substring(begin, end + 1);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
