package mx.com.liverpool.p360.services.core.amqp.run.mongo;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;

public class MongoMetadataResolver {

    private final MongoCollection<Document> canonicalAttributes;
    private final MongoCollection<Document> templates;

    public MongoMetadataResolver(
            MongoCollection<Document> canonicalAttributes,
            MongoCollection<Document> templates) {
        this.canonicalAttributes = canonicalAttributes;
        this.templates = templates;
    }

    public String attributeDescription(String characteristic) {
        if (characteristic == null) {
            return null;
        }

        Document document = canonicalAttributes.find(or(
                eq("PIM_ATRIBUTO_ID", characteristic),
                eq("pim_atributo_id", characteristic)))
                .first();

        if (document == null) {
            return characteristic;
        }

        String value = firstString(document,
                "PIM_ATRIBUTO_DESC",
                "pim_atributo_desc",
                "CharacteristicName",
                "characteristic_name",
                "descripcion");

        return value == null || value.isBlank() ? characteristic : value;
    }

    public TemplateMetadata templateMetadata(String templateId) {
        if (templateId == null || templateId.isBlank()) {
            return TemplateMetadata.empty();
        }

        Document document = templates.find(or(
                eq("plantilla_id", templateId),
                eq("PIM_PLANTILLA_ID", templateId),
                eq("pim_plantilla_id", templateId)))
                .first();

        if (document == null) {
            return new TemplateMetadata(null, templateId, null);
        }

        String templateName = firstString(document,
                "pim_nivel_nom",
                "PIM_NIVEL_NOM",
                "plantilla_nombre",
                "pim_prod_plantilla");

        String levelId = firstString(document,
                "pim_nivel_id",
                "PIM_NIVEL_ID",
                "plantilla_id",
                "PIM_PLANTILLA_ID");

        if (levelId == null) {
            levelId = templateId;
        }

        List<String> levels = new ArrayList<>();
        addNonBlank(levels, firstString(document, "pim_nivel_nom_1", "PIM_NIVEL_NOM_1"));
        addNonBlank(levels, firstString(document, "pim_nivel_nom_2", "PIM_NIVEL_NOM_2"));
        addNonBlank(levels, firstString(document, "pim_nivel_nom_3", "PIM_NIVEL_NOM_3"));
        addNonBlank(levels, templateName);

        String hierarchy = levels.isEmpty() ? null : String.join(">", levels);
        return new TemplateMetadata(templateName, levelId, hierarchy);
    }

    private String firstString(Document document, String... keys) {
        for (String key : keys) {
            Object value = document.get(key);
            if (value != null) {
                String string = String.valueOf(value);
                if (!string.isBlank()) {
                    return string;
                }
            }
        }
        return null;
    }

    private void addNonBlank(List<String> list, String value) {
        if (value != null && !value.isBlank() && !list.contains(value)) {
            list.add(value);
        }
    }

    public static class TemplateMetadata {
        private final String templateName;
        private final String levelId;
        private final String hierarchy;

        public TemplateMetadata(String templateName, String levelId, String hierarchy) {
            this.templateName = templateName;
            this.levelId = levelId;
            this.hierarchy = hierarchy;
        }

        public static TemplateMetadata empty() {
            return new TemplateMetadata(null, null, null);
        }

        public String getTemplateName() {
            return templateName;
        }

        public String getLevelId() {
            return levelId;
        }

        public String getHierarchy() {
            return hierarchy;
        }
    }
}
