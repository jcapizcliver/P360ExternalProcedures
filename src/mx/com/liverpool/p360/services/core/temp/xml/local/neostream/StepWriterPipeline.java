package mx.com.liverpool.p360.services.core.temp.xml.local.neostream;

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
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.temp.xml.local.neostream.StepXmlStreamingParser.Classification;
import mx.com.liverpool.p360.services.core.temp.xml.local.neostream.StepXmlStreamingParser.Product;
import mx.com.liverpool.p360.services.core.temp.xml.local.neostream.StepXmlStreamingParser.Value;

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
    private final ProductStatusWriter productStatus;
    private final ProductTextsWriter productTexts;
    private final VariantWriter variants;
    private final RelationWriter relations;
    private final RemainingCharacteristicWriter remaining;
    private final StructureGroupWriter structures;

    public StepWriterPipeline(
            DBAccessDataStub db,
            StepXmlStreamingParser.StepIndex index,
            StepStatusComputer statusComputer,
            Map<String, String> internalToExternalStatusMap,
            ELog log) {
        this.log = log;
        qp.put("includeObjectsInProtocol", "false");
        productName = new ProductNameWriter();
        productIdentity = new ProductIdentityWriter();
        productStatus = new ProductStatusWriter(statusComputer, internalToExternalStatusMap);
        productTexts = new ProductTextsWriter();
        variants = new VariantWriter();
        relations = new RelationWriter();
        remaining = new RemainingCharacteristicWriter(db, index);
        structures = new StructureGroupWriter(db, index);
    }

    public void accept(Product product) {
        if (product == null) return;
        productName.accept(product);
        productIdentity.accept(product);
        productStatus.accept(product);
        productTexts.accept(product);
        variants.accept(product);
        relations.accept(product);
        remaining.accept(product);
        structures.accept(product);
    }

    /** Flush all ordinary data before attempting optional hierarchy associations. */
    public void finishData() {
        finishSafely("ProductName", productName::finish);
        finishSafely("ProductIdentity", productIdentity::finish);
        finishSafely("ProductStatus", productStatus::finish);
        finishSafely("ProductTexts", productTexts::finish);
        finishSafely("Variants", variants::finish);
        finishSafely("Relations", relations::finish);
        finishSafely("RemainingCharacteristic", remaining::finish);
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
                batch("p360.step.batch.product.name", 2000),
                payload -> writeDataSafely("ProductName", "Product2G", null, payload));

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
                batch("p360.step.batch.product.identity", 2000),
                payload -> writeDataSafely("ProductIdentity", "Product2G", null, payload));

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

    private final class ProductStatusWriter {
        private final StepStatusComputer statusComputer;
        private final Map<String, String> internalToExternalStatusMap;

        private final RequestHandler productRequest = new RequestHandler(
                new JSONArray()
                        .put(new JSONObject().put("identifier", "Product2G.CurrentStatus"))
                        .put(new JSONObject().put("identifier", "Product2G.PrevStatus"))
                        .put(new JSONObject().put("identifier", "Product2G.ExternalStatus"))
                        .put(new JSONObject().put(
                                "identifier",
                                "Product2GCharacteristicValueLang.Value('EnriquecidoEnForo',root,\"0000.0000.RK\",'EnriquecidoEnForo',-1)")),
                batch("p360.step.batch.product.status", 1000),
                payload -> writeDataSafely(
                        "ProductStatus", "Product2G", null, payload));

        private final RequestHandler articleRequest = new RequestHandler(
                new JSONArray()
                        .put(new JSONObject().put("identifier", "Article.CurrentStatus"))
                        .put(new JSONObject().put("identifier", "Article.PrevStatus"))
                        .put(new JSONObject().put("identifier", "Article.ExternalStatus")),
                batch("p360.step.batch.article.status", 2000),
                payload -> writeDataSafely(
                        "ArticleStatus", "Article", null, payload));

        ProductStatusWriter(
                StepStatusComputer statusComputer,
                Map<String, String> internalToExternalStatusMap) {
            this.statusComputer = statusComputer;
            this.internalToExternalStatusMap =
                    internalToExternalStatusMap == null
                            ? Collections.emptyMap()
                            : internalToExternalStatusMap;
        }

        void accept(Product product) {
            if (!isRootProduct(product)) return;

            Map<String, Value> values = product.getValueMap();
            String firstApproved = idOrText(values.get("FirstDateApprove"));
            String[] bundle = statusComputer.computeStatus(
                    text(values.get("CalculatedWF_Att")),
                    !firstApproved.isEmpty()
                            ? "Aprobado"
                            : text(values.get("StateSKU")),
                    text(values.get("FotoTomadaLiverpool")),
                    product.getId());

            String currentStatus = bundle[0];
            String prevStatus = bundle[1];
            String externalStatus =
                    currentStatus == null || currentStatus.isEmpty()
                            ? ""
                            : java.util.Objects.toString(
                                    internalToExternalStatusMap.get(currentStatus), "");

            productRequest.addRow(row(
                    product.getId(),
                    new JSONArray()
                            .put(currentStatus)
                            .put(prevStatus)
                            .put(externalStatus)
                            .put(bundle[2])));

            if (!product.getProducts().isEmpty()) {
                for (Product child : product.getProducts()) {
                    addArticleStatus(
                            child.getId(),
                            currentStatus,
                            prevStatus,
                            externalStatus);
                }
            } else if (!product.getUserTypeId().startsWith("SalesItemFamily")) {
                addArticleStatus(
                        product.getId(),
                        currentStatus,
                        prevStatus,
                        externalStatus);
            }
        }

        private void addArticleStatus(
                String articleId,
                String currentStatus,
                String prevStatus,
                String externalStatus) {
            if (articleId == null || articleId.isBlank()) return;
            articleRequest.addRow(row(
                    articleId,
                    new JSONArray()
                            .put(currentStatus)
                            .put(prevStatus)
                            .put(externalStatus)));
        }

        void finish() {
            productRequest.sendData();
            articleRequest.sendData();
        }
    }

    private static final class PathHelper {
        private static void touchMigrationSkip(String productId)
                throws java.io.IOException {
            String base = PropertiesManager.get(
                    "p360.contingency.migration.to_skip_directory");
            if (base == null || base.isBlank()
                    || productId == null || productId.isBlank()) {
                return;
            }
            java.nio.file.Path path =
                    java.nio.file.Paths.get(base, productId);
            try {
                java.nio.file.Files.createFile(path);
            } catch (java.nio.file.FileAlreadyExistsException ignored) {
            }
        }
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
                batch("p360.step.batch.product.texts", 1000),
                payload -> writeDataSafely("ProductTexts", "Product2G", null, payload));

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
                batch("p360.step.batch.article.variants", 5000),
                payload -> writeDataSafely("Variants", "Article", null, payload));

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
                batch("p360.step.batch.relations", 2000),
                payload -> writeDataSafely("Relations", "Article", "ProductReference", payload));

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

        RemainingCharacteristicWriter(
                DBAccessDataStub db,
                StepXmlStreamingParser.StepIndex index) {
            activeProducts = db.getActiveCharacteristicIdentifiers(
                    "Product2G", index.getProductAttributeIds());
            activeArticles = db.getActiveCharacteristicIdentifiers(
                    "Article", index.getArticleAttributeIds());

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
                        batch("p360.step.batch.remaining.characteristic", 10000),
                        payload -> writeDataSafely("RemainingCharacteristic:" + entity + ":" + attribute, entity, null, payload));
                requests.put(attribute, handler);
            }
            handler.addRow(row(objectId, new JSONArray().put(value)));
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
            // Ordered, but failure-isolated. A bad hierarchy must never poison
            // ordinary data or another hierarchy family.
            writeStructureSafely(
                    "PrimaryProductTaxonomy",
                    () -> writePrimary(primary));
            writeStructureSafely(
                    "Sitios Web",
                    () -> writeQualified("Sitios Web", web));
            writeStructureSafely(
                    "CommercialECC",
                    () -> writeQualified("CommercialECC", ecc));
            writeStructureSafely(
                    "CommercialS4H",
                    () -> writeQualified("CommercialS4H", s4h));
        }

        private void writeStructureSafely(
                String structure,
                Runnable writer) {
            try {
                writer.run();
            } catch (Exception e) {
                StepWriterPipeline.this.log(
                        "Falló la escritura de "
                        + structure
                        + "; se continúa con la siguiente familia. "
                        + e.getMessage());
            }
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
            final int batchSize = batch("p360.step.batch.structures", 2000);
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

    private void writeDataSafely(
            String writer,
            String entity,
            String child,
            JSONObject payload) {
        try {
            rw.writeData(
                    "list",
                    entity,
                    child,
                    qp,
                    payload,
                    StepWriterPipeline.this::log);
        } catch (Exception e) {
            log("Writer " + writer + " falló; se conserva el aislamiento y continúa el resto: "
                    + e.getMessage());
        }
    }

    private void finishSafely(
            String writer,
            Runnable finisher) {
        try {
            finisher.run();
        } catch (Exception e) {
            log("Flush final de " + writer + " falló; se continúa con los demás writers: "
                    + e.getMessage());
        }
    }

    private static int batch(String property, int defaultValue) {
        return Math.max(1, Integer.getInteger(property, defaultValue));
    }

    private void log(String message) {
        if (log != null) log.log(message);
    }
}
