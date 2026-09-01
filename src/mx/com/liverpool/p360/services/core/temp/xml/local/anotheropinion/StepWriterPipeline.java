package mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.example.ei.forfun.logic.WildDateStandardizer;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion.StepXmlStreamingParser.Classification;
import mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion.StepXmlStreamingParser.Product;
import mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion.StepXmlStreamingParser.Value;

/**
 * One fan-out of the streamed root Product to the specialized writers that
 * previously reparsed the whole directory independently.
 */
public final class StepWriterPipeline {

    private final RESTWrapper rw = new RESTWrapper();
    private final Map<String, String> qp = new java.util.TreeMap<>();
    private final ELog log;

    private final ProductNameWriter productName;
    private final ProductIdentityWriter productIdentity;
    private final ProductTextsWriter productTexts;
    private final VariantWriter variants;
    private final RelationWriter relations;
    private final RemainingCharacteristicWriter remaining;
    private final StructureGroupWriter structures;

    public StepWriterPipeline(
            DBAccessDataStub db,
            StepXmlStreamingParser.StepIndex index,
            ELog log) {
        this.log = log;
        qp.put("includeObjectsInProtocol", "false");
        productName = new ProductNameWriter();
        productIdentity = new ProductIdentityWriter();
        productTexts = new ProductTextsWriter();
        variants = new VariantWriter();
        relations = new RelationWriter();
        remaining = new RemainingCharacteristicWriter(index);
        structures = new StructureGroupWriter(db, index);
    }

    public void accept(Product product) {
        if (product == null) return;
        productName.accept(product);
        productIdentity.accept(product);
        productTexts.accept(product);
        variants.accept(product);
        relations.accept(product);
        remaining.accept(product);
        structures.accept(product);
    }

    /** Flush all ordinary data before attempting optional hierarchy associations. */
    public void finishData() {
        productName.finish();
        productIdentity.finish();
        productTexts.finish();
        variants.finish();
        relations.finish();
        remaining.finish();
    }

    /**
     * Structure associations have their own failure domains and strict priority:
     * PrimaryProductTaxonomy -> Sitios Web -> CommercialECC -> CommercialS4H.
     */
    public void finishStructures() {
        structures.finish();
    }

    private final class ProductNameWriter {
        private final RequestHandler request = new RequestHandler(
                new JSONArray().put(new JSONObject().put("identifier", "Product2GLang.ProductName(es)")),
                2000,
                payload -> rw.writeData("list", "Product2G", null, qp, payload, StepWriterPipeline.this::log));

        void accept(Product product) {
            if (!isRootProduct(product)) return;
            Value productName = product.getValueMap().get("ProductName");
            String name = text(productName);
            if (name.isEmpty()) name = product.getName();
            request.addRow(row(product.getId(), new JSONArray().put(name)));
        }

        void finish() { request.sendData(); }
    }

    private final class ProductIdentityWriter {
        private final RequestHandler request = new RequestHandler(
                new JSONArray()
                        .put(new JSONObject().put("identifier", "Product2G.Business"))
                        .put(new JSONObject().put("identifier", "Product2G.SKU"))
                        .put(new JSONObject().put("identifier", "Product2G.EAN")),
                200,
                payload -> rw.writeData("list", "Product2G", null, qp, payload, StepWriterPipeline.this::log));

        void accept(Product product) {
            if (!isRootProduct(product)) return;
            Map<String, Value> values = product.getValueMap();
            String business = determineBusiness(text(values.get("Negocio")), text(values.get("EXTWG_S4H")));
            String ean = text(values.get("MainBarCode"));
            if (ean.isEmpty()) ean = text(values.get("MainBarCodeS4H"));
            request.addRow(row(product.getId(), new JSONArray()
                    .put(business)
                    .put(text(values.get("SKU")))
                    .put(ean)));
        }

        void finish() { request.sendData(); }
    }

    private final class ProductTextsWriter {
        private final RequestHandler request = new RequestHandler(
                new JSONArray()
                        .put(new JSONObject().put("identifier", "Product2G.EmbeddedCodeWAP"))
                        .put(new JSONObject().put("identifier", "Product2G.EmbeddedCodeWEB"))
                        .put(new JSONObject().put("identifier", "Product2G.RefundPolicy"))
                        .put(new JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FirstDateApprove',root,\"0000.0000.RK\",'FirstDateApprove',-1)"))
                        .put(new JSONObject().put("identifier", "Product2GLang.DescriptionShort(es)"))
                        .put(new JSONObject().put("identifier", "Product2GLang.DescriptionLong(es)"))
                        .put(new JSONObject().put("identifier", "Product2GLang.DescriptionLong2(es)"))
                        .put(new JSONObject().put("identifier", "Product2GExtraData.Direccion(MX)"))
                        .put(new JSONObject().put("identifier", "Product2GExtraData.Section(MX)"))
                        .put(new JSONObject().put("identifier", "Product2GExtraData.ItemGroup(MX)"))
                        .put(new JSONObject().put("identifier", "Product2GExtraData.ItemGroupS4H(MX)"))
                        .put(new JSONObject().put("identifier", "Product2GExtraData.BrandName(MX)"))
                        .put(new JSONObject().put("identifier", "Product2GExtraData.BRAND_ID_S4H(MX)"))
                        .put(new JSONObject().put("identifier", "Product2GExtraData.Negocio(MX)"))
                        .put(new JSONObject().put("identifier", "Product2GExtraData.SAPObjectType(MX)"))
                        .put(new JSONObject().put("identifier", "Product2GExtraData.SupplierID(MX)"))
                        .put(new JSONObject().put("identifier", "Product2GExtraData.SupplierPartNumber(MX)")),
                2000,
                payload -> rw.writeData("list", "Product2G", null, qp, payload, StepWriterPipeline.this::log));

        void accept(Product product) {
            if (!isRootProduct(product)) return;
            Map<String, Value> values = product.getValueMap();
            String firstApproved = idOrText(values.get("FirstDateApprove"));
            String normalizedDate = WildDateStandardizer.normalize(
                    firstApproved,
                    java.time.ZoneId.of("America/Mexico_City"),
                    WildDateStandardizer.AmbiguityPolicy.PREFER_DMY).orElse("");

            request.addRow(row(product.getId(), new JSONArray()
                    .put(text(values.get("EmbedCodeWAP")))
                    .put(text(values.get("EmbedCodeWEB")))
                    .put(text(values.get("refundPolicy")))
                    .put(normalizedDate)
                    .put(product.getName())
                    .put(text(values.get("DescriptionLong")))
                    .put(text(values.get("DescriptionLong2")))
                    .put(idOrText(values.get("Direction")))
                    .put(idOrText(values.get("Section")))
                    .put(idOrText(values.get("ItemGroup")))
                    .put(idOrText(values.get("ItemGroupS4H")))
                    .put(idOrText(values.get("BrandName")))
                    .put(idOrText(values.get("BRAND_ID_S4H")))
                    .put(idOrText(values.get("Negocio")))
                    .put(idOrText(values.get("SAPObjectType")))
                    .put(idOrText(values.get("SupplierID")))
                    .put(text(values.get("SupplierPartNumber")))));
        }

        void finish() { request.sendData(); }
    }

    private final class VariantWriter {
        private final RequestHandler request = new RequestHandler(
                new JSONArray()
                        .put(new JSONObject().put("identifier", "Article.SKU"))
                        .put(new JSONObject().put("identifier", "Article.EAN"))
                        .put(new JSONObject().put("identifier", "ArticleLang.DescriptionShort(es)"))
                        .put(new JSONObject().put("identifier", "ArticleExtraData.TamanoUnico(MX)"))
                        .put(new JSONObject().put("identifier", "ArticleExtraData.ColoursLiverpoolAtt(MX)"))
                        .put(new JSONObject().put("identifier", "ArticleExtraData.SupplierPartNumber(MX)"))
                        .put(new JSONObject().put("identifier", "ArticleExtraData.SAPObjectType(MX)")),
                10000,
                payload -> rw.writeData("list", "Article", null, qp, payload, StepWriterPipeline.this::log));

        void accept(Product product) {
            if (!product.getProducts().isEmpty()) {
                for (Product child : product.getProducts()) add(child);
            } else if (!product.getUserTypeId().startsWith("SalesItemFamily")) {
                add(product);
            }
        }

        private void add(Product child) {
            Map<String, Value> values = child.getValueMap();
            String ean = idOrText(values.get("MainBarCode"));
            if (ean.isEmpty()) ean = idOrText(values.get("MainBarCodeS4H"));
            request.addRow(row(child.getId(), new JSONArray()
                    .put(idOrText(values.get("SKU")))
                    .put(ean)
                    .put(idOrText(values.get("Name")))
                    .put(idOrText(values.get("TamanoUnico")))
                    .put(idOrText(values.get("ColoursLiverpoolAtt")))
                    .put(idOrText(values.get("SupplierPartNumber")))
                    .put(idOrText(values.get("SAPObjectType")))));
        }

        void finish() { request.sendData(); }
    }

    private final class RelationWriter {
        private final RequestHandler request = new RequestHandler(
                new JSONArray().put(new JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid")),
                5000,
                payload -> rw.writeData("list", "Article", "ProductReference", qp, payload, StepWriterPipeline.this::log));

        void accept(Product product) {
            if (product.getParentId() != null && product.getParentId().matches("^(S?[0-9]+)$")) {
                add(product.getId(), product.getParentId());
                return;
            }
            if (!product.getProducts().isEmpty()) {
                for (Product child : product.getProducts()) {
                    add(child.getId(), child.getParentId());
                }
            } else if (!product.getUserTypeId().startsWith("SalesItemFamily")) {
                add(product.getId(), product.getId());
            }
        }

        private void add(String articleId, String parentId) {
            if (articleId == null || parentId == null) return;
            request.addRow(new JSONObject()
                    .put("object", new JSONObject().put("id", "'" + articleId + "'@1"))
                    .put("qualification", new JSONObject().put("referencedSupplierAid", parentId))
                    .put("values", new JSONArray().put(parentId)));
        }

        void finish() { request.sendData(); }
    }

    private final class RemainingCharacteristicWriter {
        private final Set<String> activeProducts;
        private final Set<String> activeArticles;
        private final Map<String, RequestHandler> productRequests = new LinkedHashMap<>();
        private final Map<String, RequestHandler> articleRequests = new LinkedHashMap<>();

        private final Set<String> handledProducts = new HashSet<>();
        private final Set<String> handledArticles = new HashSet<>();

        private final Map<String, String> unitsLength = new HashMap<>();
        private final Map<String, String> unitsWeight = new HashMap<>();
        private final Map<String, String> unitsVolume = new HashMap<>();
        private final Set<String> lengthAttributes = new HashSet<>();
        private final Set<String> weightAttributes = new HashSet<>();
        private final Set<String> volumeAttributes = new HashSet<>();

        RemainingCharacteristicWriter(StepXmlStreamingParser.StepIndex index) {
            activeProducts = loadActiveCharacteristics("Product2G");
            activeArticles = loadActiveCharacteristics("Article");
            activeProducts.retainAll(index.getProductAttributeIds());
            activeArticles.retainAll(index.getArticleAttributeIds());

            Collections.addAll(handledProducts,
                    "ProductName", "Business", "EXTWG_S4H", "Negocio", "SKU",
                    "MainBarCode", "MainBarCodeS4H", "FirstDateApprove",
                    "EmbedCodeWAP", "EmbedCodeWEB", "refundPolicy",
                    "DescriptionLong", "DescriptionLong2", "Direction", "Section",
                    "ItemGroup", "ItemGroupS4H", "BrandName", "BRAND_ID_S4H",
                    "SAPObjectType", "SupplierID", "SupplierPartNumber",
                    "CalculatedWF_Att", "FotoTomadaLiverpool", "StateSKU",
                    "EnriquecidoEnForo");
            Collections.addAll(handledArticles,
                    "SKU", "MainBarCode", "MainBarCodeS4H", "Name", "TamanoUnico",
                    "ColoursLiverpoolAtt", "SupplierPartNumber", "SAPObjectType");

            unitsWeight.put("unece.unit.KGM", "KG");
            unitsLength.put("unece.unit.CMT", "CM");
            unitsLength.put("unece.unit.MTR", "M");
            unitsLength.put("unece.unit.MMT", "MM");
            unitsVolume.put("unece.unit.CMQ", "CM3");
            unitsVolume.put("unece.unit.LTR", "L");
            unitsVolume.put("unece.unit.FTQ", "PI3");
            unitsVolume.put("unece.unit.MTQ", "M3");
            Collections.addAll(lengthAttributes,
                    "ProductWidth", "ProductDepth", "ProductHeight", "ZBRECJ", "ZLAECJ",
                    "ZHOECJ", "ZHOEPQ", "ZBREPQ", "ZLAEPQ");
            Collections.addAll(volumeAttributes, "VOLUMAtt", "ZVOLCJ", "ZVOLPQ");
            Collections.addAll(weightAttributes,
                    "PesoBruto", "ProductWeight", "ZBRGCJ", "ZNTGCJ", "ZBRGPQ", "ZNTGPQ");
        }

        void accept(Product product) {
            if (isRootProduct(product)) {
                for (Value value : product.getValues()) {
                    addProductValue(product.getId(), value);
                }
            }
            if (!product.getProducts().isEmpty()) {
                for (Product child : product.getProducts()) {
                    for (Value value : child.getValues()) addArticleValue(child.getId(), value);
                }
            } else if (!product.getUserTypeId().startsWith("SalesItemFamily")) {
                for (Value value : product.getValues()) addArticleValue(product.getId(), value);
            }
        }

        private void addProductValue(String id, Value value) {
            if (value == null || value.getAttributeId() == null) return;
            String attribute = value.getAttributeId();
            if (handledProducts.contains(attribute) || !activeProducts.contains(attribute)) return;
            if (addUnitIfNeeded(id, value)) return;
            add(productRequests, "Product2G", id, attribute, value.idOrText());
        }

        private void addArticleValue(String id, Value value) {
            if (value == null || value.getAttributeId() == null) return;
            String attribute = value.getAttributeId();
            if (handledArticles.contains(attribute) || !activeArticles.contains(attribute)) return;
            add(articleRequests, "Article", id, attribute, value.idOrText());
        }

        private boolean addUnitIfNeeded(String productId, Value value) {
            String unitId = value.getUnitId();
            if (unitId == null || unitId.isEmpty()) return false;
            String attribute = value.getAttributeId();
            if (lengthAttributes.contains(attribute)) {
                String unit = unitsLength.get(unitId);
                if (unit != null) {
                    add(productRequests, "Product2G", productId, "UnidadDeMedidaLongitud", unit);
                    return true;
                }
            }
            if (weightAttributes.contains(attribute)) {
                String unit = unitsWeight.get(unitId);
                if (unit != null) {
                    add(productRequests, "Product2G", productId, "UnidadDeMedidaPeso", unit);
                    return true;
                }
            }
            if (volumeAttributes.contains(attribute)) {
                String unit = unitsVolume.get(unitId);
                if (unit != null) {
                    add(productRequests, "Product2G", productId, "UnidadDeMedidaVolumen", unit);
                    return true;
                }
            }
            return false;
        }

        private void add(
                Map<String, RequestHandler> requests,
                String entity,
                String objectId,
                String attribute,
                String value) {
            if (value == null) return;
            if (value.length() > 2000) value = value.substring(0, 2000);
            RequestHandler handler = requests.get(attribute);
            if (handler == null) {
                JSONArray columns = new JSONArray().put(new JSONObject().put(
                        "identifier",
                        entity + "CharacteristicValueLang.Value('" + attribute
                                + "',root,\"0000.0000.RK\",'" + attribute + "',-1)"));
                handler = new RequestHandler(
                        columns,
                        10000,
                        payload -> rw.writeData("list", entity, null, qp, payload, StepWriterPipeline.this::log));
                requests.put(attribute, handler);
            }
            handler.addRow(row(objectId, new JSONArray().put(value)));
        }

        private Set<String> loadActiveCharacteristics(String entity) {
            Set<String> result = new HashSet<>();
            Map<String, String> query = new java.util.TreeMap<>();
            query.put("fields", "Characteristic.Identifier");
            query.put("query", "Characteristic.IsActive = true and Characteristic.ParentCharacteristic is empty and not Characteristic.Identifier wildcard \"%_Rechazo\" and Characteristic.Entities contains \"" + entity + "\"");
            query.put("pageSize", "10000");
            int start = 0;
            int total = 0;
            do {
                query.put("startIndex", String.valueOf(start));
                JSONObject response = rw.getRw().makeRequest("GET", "/list/Characteristic/bySearch", query, null);
                if (response == null || !response.has("totalSize")) {
                    StepWriterPipeline.this.log("No fue posible cargar Characteristics.Entities para " + entity);
                    break;
                }
                total = response.optInt("totalSize", 0);
                JSONArray rows = response.optJSONArray("rows");
                if (rows != null) {
                    for (int i = 0; i < rows.length(); i++) {
                        JSONArray values = rows.getJSONObject(i).optJSONArray("values");
                        if (values != null && values.length() > 0) result.add(values.optString(0, ""));
                    }
                }
                int pageSize = response.optInt("pageSize", rows == null ? 0 : rows.length());
                if (pageSize <= 0) break;
                start += pageSize;
            } while (start < total);
            return result;
        }

        void finish() {
            for (RequestHandler handler : productRequests.values()) handler.sendData();
            for (RequestHandler handler : articleRequests.values()) handler.sendData();
        }
    }

    private final class StructureGroupWriter {
        private final Set<String> primaryGroups;
        private final Set<String> webGroups;
        private final Set<String> eccGroups;
        private final Set<String> s4hGroups;
        private final List<Assignment> primary = new ArrayList<>();
        private final List<Assignment> web = new ArrayList<>();
        private final List<Assignment> ecc = new ArrayList<>();
        private final List<Assignment> s4h = new ArrayList<>();
        private boolean primaryUnavailableLogged;
        private boolean webUnavailableLogged;
        private boolean eccUnavailableLogged;
        private boolean s4hUnavailableLogged;

        StructureGroupWriter(DBAccessDataStub db, StepXmlStreamingParser.StepIndex index) {
            primaryGroups = db.getExistingStructureGroupIdentifiers(
                    "PrimaryProductTaxonomy", index.getPrimaryStructureGroupIds());
            webGroups = db.getExistingStructureGroupIdentifiers(
                    "Sitios Web", index.getWebStructureGroupIds());
            eccGroups = db.getExistingStructureGroupIdentifiers(
                    "CommercialECC", index.getEccStructureGroupIds());
            s4hGroups = db.getExistingStructureGroupIdentifiers(
                    "CommercialS4H", index.getS4hStructureGroupIds());
        }

        void accept(Product product) {
            if (!isRootProduct(product)) return;
            String parent = product.getParentId();
            if (parent != null && !parent.isBlank()) {
                if (primaryGroups.isEmpty()) {
                    if (!primaryUnavailableLogged) {
                        StepWriterPipeline.this.log("PrimaryProductTaxonomy no pudo cargarse desde BD; se omiten sus asociaciones para no contaminar otros writes.");
                        primaryUnavailableLogged = true;
                    }
                } else if (primaryGroups.contains(parent)) {
                    primary.add(new Assignment(product.getId(), parent));
                } else {
                    StepWriterPipeline.this.log("PrimaryProductTaxonomy ausente, se omite asociación: " + parent + " <- " + product.getId());
                }
            }

            for (Classification classification : product.getClassifications()) {
                if (classification == null || classification.getId() == null || classification.getType() == null) continue;
                String type = classification.getType();
                if ("WebsiteLink".equals(type)) {
                    addIfPresent(web, webGroups, product.getId(), classification.getId(), "Sitios Web");
                } else if ("GALink".equals(type)) {
                    addIfPresent(ecc, eccGroups, product.getId(), classification.getId(), "CommercialECC");
                } else if ("GALink_S4H".equals(type)) {
                    addIfPresent(s4h, s4hGroups, product.getId(), classification.getId(), "CommercialS4H");
                }
            }
        }

        private void addIfPresent(List<Assignment> target, Set<String> valid, String productId, String group, String structure) {
            if (valid.isEmpty()) {
                boolean alreadyLogged = "Sitios Web".equals(structure) ? webUnavailableLogged
                        : "CommercialECC".equals(structure) ? eccUnavailableLogged : s4hUnavailableLogged;
                if (!alreadyLogged) {
                    StepWriterPipeline.this.log(structure + " no pudo cargarse desde BD; se omiten sus asociaciones para proteger el resto de la carga.");
                    if ("Sitios Web".equals(structure)) webUnavailableLogged = true;
                    else if ("CommercialECC".equals(structure)) eccUnavailableLogged = true;
                    else s4hUnavailableLogged = true;
                }
                return;
            }
            if (valid.contains(group)) target.add(new Assignment(productId, group));
            else StepWriterPipeline.this.log(structure + " ausente, se omite asociación: " + group + " <- " + productId);
        }

        void finish() {
            // Strict barriers: never attempt a lower-priority hierarchy before the previous one finished.
            writePrimary(primary);
            writeQualified("Sitios Web", web);
            writeQualified("CommercialECC", ecc);
            writeQualified("CommercialS4H", s4h);
        }

        private void writePrimary(List<Assignment> assignments) {
            JSONArray columns = new JSONArray().put(new JSONObject().put(
                    "identifier", "Product2GStructureMap.ManualMap('PrimaryProductTaxonomy')"));
            writeAssignments(assignments, columns, null);
        }

        private void writeQualified(String structure, List<Assignment> assignments) {
            JSONArray columns = new JSONArray().put(new JSONObject().put(
                    "identifier", "Product2GStructureMap.ManualMap"));
            writeAssignments(assignments, columns, structure);
        }

        private void writeAssignments(List<Assignment> assignments, JSONArray columns, String structure) {
            final int batchSize = 5000;
            JSONArray rows = new JSONArray();
            for (Assignment assignment : assignments) {
                JSONObject row = new JSONObject()
                        .put("object", new JSONObject().put("id", "'" + assignment.productId + "'@1"))
                        .put("values", new JSONArray().put(assignment.group));
                if (structure != null) row.put("qualification", new JSONObject().put("structureId", structure));
                rows.put(row);
                if (rows.length() == batchSize) {
                    rw.writeData("list", "Product2G", "Product2GStructureMap", qp,
                            new JSONObject().put("columns", columns).put("rows", rows), StepWriterPipeline.this::log);
                    rows = new JSONArray();
                }
            }
            if (rows.length() > 0) {
                rw.writeData("list", "Product2G", "Product2GStructureMap", qp,
                        new JSONObject().put("columns", columns).put("rows", rows), StepWriterPipeline.this::log);
            }
        }
    }

    private static final class Assignment {
        final String productId;
        final String group;
        Assignment(String productId, String group) {
            this.productId = productId;
            this.group = group;
        }
    }

    private boolean isRootProduct(Product product) {
        return product.getParentId() == null || !product.getParentId().matches("^(S?[0-9]+)");
    }

    private JSONObject row(String identifier, JSONArray values) {
        return new JSONObject()
                .put("object", new JSONObject().put("id", "'" + identifier + "'@1"))
                .put("values", values);
    }

    private String text(Value value) {
        return value == null || value.getText() == null ? "" : value.getText();
    }

    private String idOrText(Value value) {
        return value == null ? "" : value.idOrText();
    }

    private String determineBusiness(String negocio, String extwgS4h) {
        return negocio.isEmpty() && extwgS4h.isEmpty()
                ? null
                : (negocio.isEmpty() && !extwgS4h.isEmpty()
                        ? "SBB"
                        : "ART. MARKETPLACE".equals(negocio) ? "MKP" : "LVP");
    }

    private void log(String message) {
        if (log != null) log.log(message);
    }
}
