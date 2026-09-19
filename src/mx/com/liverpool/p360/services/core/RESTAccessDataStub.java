package mx.com.liverpool.p360.services.core;

/**
 * Read-only REST counterpart of DBAccessDataStub for the methods whose REST
 * semantics are already exercised by CreateProposal, GetProposals,
 * RealExportProducts and GetTemplateInformation.
 *
 * The public return contracts intentionally mirror DBAccessDataStub so callers
 * can be parity-tested without being modified.
 */
public class RESTAccessDataStub implements AutoCloseable {

    private static final String MASTER = "MASTER";
    private static final String PRIMARY_PRODUCT_TAXONOMY = "PrimaryProductTaxonomy";
    private static final String TARGET_MARKET_MX = "MX";

    private final RESTWrapper rest;
    private final RESTWorkshop rw;
    private final ELog log;

    public RESTAccessDataStub(ELog log) {
        this(log, new RESTWrapper());
    }

    RESTAccessDataStub(ELog log, RESTWrapper rest) {
        this.log = log;
        this.rest = rest;
        this.rw = rest.getRw();
    }

    public org.json.JSONObject getProductData(String identifier) {
        org.json.JSONObject result = emptyProductData(identifier);
        if (blank(identifier)) {
            return result;
        }

        org.json.JSONObject response = getObject(
                "Product2G",
                identifier,
                "Product2GStructureGroupMap,Product2GCharacteristicValue,Product2G,Product2GLang,ProductExtraData",
                true,
                true);

        org.json.JSONObject data = data(response);
        if (data == null) {
            return result;
        }

        org.json.JSONObject extra = qualifiedExtraData(data, "productExtraData", TARGET_MARKET_MX);
        java.util.Map<String, java.util.List<org.json.JSONObject>> characteristics = characteristicMap(data);

        result
            .put("Section", lookupCode(extra, "section"))
            .put("ItemGroup", lookupCode(extra, "itemGroup"))
            .put("ItemGroupS4H", lookupCode(extra, "itemGroupS4H"))
            .put("BrandName", lookupCode(extra, "brandName"))
            .put("BRAND_ID_S4H", lookupCode(extra, "brandIdS4H"))
            .put("Business", firstNonBlank(
                    lookupCode(data, "business"),
                    characteristicCode(characteristics, "Business")))
            .put("SKU", scalarString(data.opt("sku")))
            .put("SupplierID", lookupCode(extra, "supplierID"))
            .put("Template", primaryProductTaxonomyTemplate(data.optJSONArray("structureGroupMap")))
            .put("CurrentStatus", lookupCodeOrScalar(data.opt("currentStatus")))
            .put("AssignTakeNoTake", characteristicCode(characteristics, "AssignTakeNoTake"))
            .put("SAPObjectType", firstNonBlank(
                    lookupCode(extra, "sapObjectType"),
                    characteristicCode(characteristics, "SAPObjectType")))
            .put("FotoTomadaLiverpool", characteristicCode(characteristics, "FotoTomadaLiverpool"))
            .put("MainBarCode", scalarString(data.opt("gtin")))
            .put("MainBarCodeS4H", "")
            .put("SupplierPartNumber", scalarString(extra == null ? null : extra.opt("supplierPartNumber")));

        return result;
    }

    public java.util.Map<String, org.json.JSONObject> getProductData(
            java.util.Collection<String> identifiers) {

        java.util.Map<String, org.json.JSONObject> result = new java.util.LinkedHashMap<>();
        for (String identifier : normalizeStrings(identifiers)) {
            result.put(identifier, getProductData(identifier));
        }
        return result;
    }

    public org.json.JSONObject getArticleData(String identifier) {
        org.json.JSONObject result = emptyArticleData(identifier);
        if (blank(identifier)) {
            return result;
        }

        org.json.JSONObject response = getObject(
                "Article",
                identifier,
                "ArticleCharacteristicValue,Article,ArticleExtraData",
                true,
                true);

        org.json.JSONObject data = data(response);
        if (data == null) {
            return result;
        }

        org.json.JSONObject extra = qualifiedExtraData(data, "articleExtraData", TARGET_MARKET_MX);
        java.util.Map<String, java.util.List<org.json.JSONObject>> characteristics = characteristicMap(data);

        result
            .put("ProductNo", getProductByVariant(identifier))
            .put("SKU", scalarString(data.opt("sku")))
            .put("ColoursLiverpoolAtt", lookupCode(extra, "coloursLiverpoolAtt"))
            .put("TamanoUnico", lookupCode(extra, "tamanoUnico"))
            .put("ProductImage", scalarString(data.opt("productImageURL")))
            .put("MainBarCode", scalarString(data.opt("gtin")))
            .put("MainBarCodeS4H", "")
            .put("AssignTakeNoTake", characteristicCode(characteristics, "AssignTakeNoTake"))
            .put("SupplierPartNumber", scalarString(extra == null ? null : extra.opt("supplierPartNumber")));

        return result;
    }

    public java.util.Map<String, org.json.JSONObject> getArticleData(
            java.util.Collection<String> identifiers) {

        java.util.Map<String, org.json.JSONObject> result = new java.util.LinkedHashMap<>();
        for (String identifier : normalizeStrings(identifiers)) {
            result.put(identifier, getArticleData(identifier));
        }
        return result;
    }

    public org.json.JSONObject getProductExtraData(
            String identifier,
            String[] characteristicIdentifiers) {

        org.json.JSONObject result = new org.json.JSONObject().put("product", safe(identifier));
        java.util.List<String> requested = normalizeStrings(
                characteristicIdentifiers == null
                    ? java.util.Collections.emptyList()
                    : java.util.Arrays.asList(characteristicIdentifiers));

        for (String characteristic : requested) {
            result.put(characteristic, "");
        }
        result.put("ProductName", "")
              .put("DescriptionLong", "")
              .put("DescriptionLong2", "");

        if (blank(identifier)) {
            return result;
        }

        org.json.JSONObject response = getObject(
                "Product2G",
                identifier,
                "Product2GCharacteristicValue,Product2G,Product2GLang",
                true,
                true);
        org.json.JSONObject data = data(response);
        if (data == null) {
            return result;
        }

        java.util.Map<String, java.util.List<org.json.JSONObject>> characteristics = characteristicMap(data);
        for (String characteristic : requested) {
            result.put(characteristic, characteristicJdbcValue(characteristics, characteristic));
        }

        org.json.JSONObject es = languageByKey(data.optJSONArray("lang"), 10);
        if (es != null) {
            result.put("ProductName", scalarString(es.opt("productName")))
                  .put("DescriptionLong", scalarString(es.opt("descriptionLong")))
                  .put("DescriptionLong2", scalarString(es.opt("descriptionLong2")));
        }

        return result;
    }

    public java.util.Map<String, org.json.JSONObject> getProductExtraData(
            java.util.Collection<String> identifiers,
            String[] characteristicIdentifiers) {

        java.util.Map<String, org.json.JSONObject> result = new java.util.LinkedHashMap<>();
        for (String identifier : normalizeStrings(identifiers)) {
            result.put(identifier, getProductExtraData(identifier, characteristicIdentifiers));
        }
        return result;
    }

    public org.json.JSONObject getArticleExtraData(
            String identifier,
            String[] characteristicIdentifiers) {

        org.json.JSONObject result = new org.json.JSONObject().put("variant", safe(identifier));
        java.util.List<String> requested = normalizeStrings(
                characteristicIdentifiers == null
                    ? java.util.Collections.emptyList()
                    : java.util.Arrays.asList(characteristicIdentifiers));

        for (String characteristic : requested) {
            result.put(characteristic, "");
        }

        if (blank(identifier)) {
            return result;
        }

        org.json.JSONObject response = getObject(
                "Article",
                identifier,
                "ArticleCharacteristicValue,Article",
                true,
                true);
        org.json.JSONObject data = data(response);
        if (data == null) {
            return result;
        }

        java.util.Map<String, java.util.List<org.json.JSONObject>> characteristics = characteristicMap(data);
        for (String characteristic : requested) {
            result.put(characteristic, characteristicJdbcValue(characteristics, characteristic));
        }

        return result;
    }

    public java.util.Map<String, org.json.JSONObject> getArticleExtraData(
            java.util.Collection<String> identifiers,
            String[] characteristicIdentifiers) {

        java.util.Map<String, org.json.JSONObject> result = new java.util.LinkedHashMap<>();
        for (String identifier : normalizeStrings(identifiers)) {
            result.put(identifier, getArticleExtraData(identifier, characteristicIdentifiers));
        }
        return result;
    }

    public java.util.Map<String, org.json.JSONObject> getProductCharacteristicValues(
            java.util.Collection<String> identifiers,
            java.util.Collection<String> characteristicIdentifiers)
            throws java.sql.SQLException {

        java.util.List<String> characteristics = normalizeStrings(characteristicIdentifiers);
        java.util.Map<String, org.json.JSONObject> result = new java.util.LinkedHashMap<>();
        for (String identifier : normalizeStrings(identifiers)) {
            org.json.JSONObject item = new org.json.JSONObject().put("product", identifier);
            for (String characteristic : characteristics) {
                item.put(characteristic, "");
            }

            org.json.JSONObject response = getObject(
                    "Product2G",
                    identifier,
                    "Product2GCharacteristicValue,Product2G",
                    true,
                    true);
            org.json.JSONObject data = data(response);
            if (data != null) {
                java.util.Map<String, java.util.List<org.json.JSONObject>> map = characteristicMap(data);
                for (String characteristic : characteristics) {
                    item.put(characteristic, characteristicJdbcValue(map, characteristic));
                }
            }
            result.put(identifier, item);
        }
        return result;
    }

    public java.util.Set<String> getProductVariants(String identifier) {
        java.util.Set<String> variants = new java.util.TreeSet<>();
        if (blank(identifier)) {
            return variants;
        }

        java.util.Map<String, String> qp = new java.util.TreeMap<>();
        qp.put("query",
                "ProductReference.ReferencedSupplierAid(\"" + escapeQuery(identifier)
                + "\") equals \"" + escapeQuery(identifier) + "\"");
        qp.put("fields", "Article.SupplierAID");
        qp.put("pageSize", "5000");
        qp.put("metaData", "true");

        rest.collectData(
                "list",
                "Article",
                null,
                "bySearch",
                qp,
                row -> {
                    org.json.JSONArray values = row.optJSONArray("values");
                    if (values != null && values.length() > 0) {
                        String value = scalarString(values.opt(0));
                        if (!blank(value)) {
                            variants.add(value);
                        }
                    }
                },
                raw -> log("REST getProductVariants error: " + raw),
                false);

        return variants;
    }

    public java.util.Map<String, java.util.Set<String>> getProductVariants(
            java.util.Collection<String> identifiers) {

        java.util.Map<String, java.util.Set<String>> result = new java.util.LinkedHashMap<>();
        for (String identifier : normalizeStrings(identifiers)) {
            result.put(identifier, getProductVariants(identifier));
        }
        return result;
    }

    public String getProductByVariant(String articleIdentifier) {
        if (blank(articleIdentifier)) {
            return null;
        }

        java.util.Map<String, String> qp = new java.util.TreeMap<>();
        qp.put("fields", "ProductReference.ReferencedSupplierAid");
        qp.put("items", "'" + articleIdentifier + "'@1");

        org.json.JSONObject response = rw.makeRequest(
                "GET",
                "/list/Article/ProductReference/byItems",
                qp,
                null);

        org.json.JSONArray rows = response == null ? null : response.optJSONArray("rows");
        if (rows == null || rows.length() == 0) {
            return null;
        }
        org.json.JSONArray values = rows.optJSONObject(0).optJSONArray("values");
        if (values == null || values.length() == 0) {
            return null;
        }
        String value = scalarString(values.opt(0));
        return blank(value) ? null : value;
    }

    public Integer getProductCurrentStatusByArticleIdentifier(String articleIdentifier) {
        if (blank(articleIdentifier)) {
            return null;
        }

        java.util.Map<String, String> qp = new java.util.TreeMap<>();
        qp.put("fields",
                "ProductReference.ReferencedArticle->Product2G.CurrentStatus");
        qp.put("items", "'" + articleIdentifier + "'@1");

        org.json.JSONObject response = rw.makeRequest(
                "GET",
                "/list/Article/ProductReference/byItems",
                qp,
                null);
        org.json.JSONArray rows = response == null ? null : response.optJSONArray("rows");
        if (rows == null || rows.length() == 0) {
            return null;
        }
        org.json.JSONArray values = rows.optJSONObject(0).optJSONArray("values");
        if (values == null || values.length() == 0) {
            return null;
        }
        String status = scalarString(values.opt(0));
        try {
            return blank(status) ? null : Integer.valueOf(status);
        } catch (NumberFormatException e) {
            log("Unexpected CurrentStatus from REST for article " + articleIdentifier + ": " + status);
            return null;
        }
    }

    public String getProductPrimaryTemplate(String identifier) {
        if (blank(identifier)) {
            return null;
        }

        org.json.JSONObject response = getObject(
                "Product2G",
                identifier,
                "Product2GStructureGroupMap,Product2G",
                true,
                true);
        org.json.JSONObject data = data(response);
        if (data == null) {
            return null;
        }
        String value = primaryProductTaxonomyTemplate(data.optJSONArray("structureGroupMap"));
        return blank(value) ? null : value;
    }

    public org.json.JSONObject getProductStatusData(String identifier) {
        org.json.JSONObject result = new org.json.JSONObject()
                .put("CurrentStatus", "")
                .put("PreviousStatus", "")
                .put("ExternalStatus", "");
        if (blank(identifier)) {
            return result;
        }

        // Object API is intentionally used here because CreateProposal writes
        // currentStatus as an Object API property. previous/external status are
        // read only when exposed by the same logical object; otherwise they stay
        // empty so parity testing reveals the missing REST mapping explicitly.
        org.json.JSONObject response = getObject(
                "Product2G",
                identifier,
                "Product2G",
                true,
                true);
        org.json.JSONObject data = data(response);
        if (data == null) {
            return result;
        }

        result.put("CurrentStatus", lookupCodeOrScalar(data.opt("currentStatus")))
              .put("PreviousStatus", lookupCodeOrScalar(data.opt("previousStatus")))
              .put("ExternalStatus", lookupCodeOrScalar(data.opt("externalStatus")));
        return result;
    }

    public String getSkuProductNo(String sku) {
        if (blank(sku)) {
            return null;
        }

        java.util.Map<String, String> qp = new java.util.TreeMap<>();
        qp.put("query", "Product2G.SKU = \"" + escapeQuery(sku) + "\"");
        qp.put("fields", "Product2G.ProductNo");
        qp.put("pageSize", "2");
        qp.put("metaData", "true");

        final String[] found = new String[1];
        rest.collectData(
                "list",
                "Product2G",
                null,
                "bySearch",
                qp,
                row -> {
                    if (found[0] != null) {
                        return;
                    }
                    org.json.JSONArray values = row.optJSONArray("values");
                    if (values != null && values.length() > 0) {
                        String value = scalarString(values.opt(0));
                        if (!blank(value)) {
                            found[0] = value;
                        }
                    }
                },
                raw -> log("REST getSkuProductNo error: " + raw),
                false);
        return found[0];
    }

    public java.util.Map<String, String> getProductsBySKUs(
            java.util.Collection<String> skus) {

        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
        for (String sku : normalizeStrings(skus)) {
            String product = getSkuProductNo(sku);
            if (!blank(product)) {
                result.put(sku, product);
            }
        }
        return result;
    }

    public String getSkuSupplierAid(String sku) {
        if (blank(sku)) {
            return null;
        }

        java.util.Map<String, String> qp = new java.util.TreeMap<>();
        qp.put("query", "Article.SKU = \"" + escapeQuery(sku) + "\"");
        qp.put("fields", "Article.SupplierAID");
        qp.put("pageSize", "2");
        qp.put("metaData", "true");

        final String[] found = new String[1];
        rest.collectData(
                "list",
                "Article",
                null,
                "bySearch",
                qp,
                row -> {
                    if (found[0] != null) {
                        return;
                    }
                    org.json.JSONArray values = row.optJSONArray("values");
                    if (values != null && values.length() > 0) {
                        String value = scalarString(values.opt(0));
                        if (!blank(value)) {
                            found[0] = value;
                        }
                    }
                },
                raw -> log("REST getSkuSupplierAid error: " + raw),
                false);
        return found[0];
    }

    public java.util.Map<String, String> getArticlesBySKUs(
            java.util.Collection<String> skus) {

        java.util.Map<String, String> result = new java.util.LinkedHashMap<>();
        for (String sku : normalizeStrings(skus)) {
            String article = getSkuSupplierAid(sku);
            if (!blank(article)) {
                result.put(sku, article);
            }
        }
        return result;
    }

    public java.util.Map<String, String> getLookupValueCodeNameMap(
            String lookupIdentifier,
            int languageID,
            boolean onlyActive) {

        java.util.Map<String, String> values = new java.util.TreeMap<>();
        if (blank(lookupIdentifier)) {
            return values;
        }

        String language = languageCode(languageID);
        java.util.Map<String, String> qp = new java.util.TreeMap<>();
        qp.put("lookup", "'" + lookupIdentifier + "'");
        qp.put("fields", "LookupValue.Code,LookupValueLang.Name(" + language + ")");
        qp.put("pageSize", "2000");
        if (onlyActive) {
            // The supplied REST usages prove /list/LookupValue/byLookup and its
            // code/name fields, but do not prove the REST identifier used to
            // filter IsActive. Do not invent one here; parity testing will make
            // any inactive-value difference visible until that identifier is
            // confirmed in this installation.
            log("REST lookup active filtering is not applied yet for "
                    + lookupIdentifier + "; comparing the byLookup result as exposed by P360.");
        }

        rest.collectData(
                "list",
                "LookupValue",
                null,
                "byLookup",
                qp,
                row -> {
                    org.json.JSONArray v = row.optJSONArray("values");
                    if (v != null && v.length() >= 2) {
                        String code = scalarString(v.opt(0));
                        if (!blank(code)) {
                            values.put(code, scalarString(v.opt(1)));
                        }
                    }
                },
                raw -> log("REST getLookupValueCodeNameMap error: " + raw),
                false);
        return values;
    }

    public String getLookupValueCodeByName(
            String lookupIdentifier,
            int languageID,
            String name,
            boolean onlyActive) {

        if (blank(name)) {
            return null;
        }
        for (java.util.Map.Entry<String, String> entry :
                getLookupValueCodeNameMap(lookupIdentifier, languageID, onlyActive).entrySet()) {
            if (name.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private org.json.JSONObject getObject(
            String entity,
            String identifier,
            String entityFilter,
            boolean includeLabels,
            boolean includeIds) {

        java.util.Map<String, String> qp = new java.util.TreeMap<>();
        if (includeLabels) {
            qp.put("includeLabels", "true");
        }
        if (includeIds) {
            qp.put("includeIds", "true");
        }
        if (!blank(entityFilter)) {
            qp.put("entityFilter", entityFilter);
        }

        String path = "/object/" + entity + "/'" + rw.encode(identifier) + "'@'" + MASTER + "'";
        org.json.JSONObject response = rw.makeRequest("GET", path, qp, null);
        if (response == null) {
            log("REST Object API returned no JSON for " + entity + " " + identifier
                    + ". Raw response: " + rw.getRawResponse());
        }
        return response;
    }

    private org.json.JSONObject data(org.json.JSONObject response) {
        return response == null ? null : response.optJSONObject("_data");
    }

    private org.json.JSONObject qualifiedExtraData(
            org.json.JSONObject data,
            String property,
            String targetMarketCode) {

        if (data == null) {
            return null;
        }
        org.json.JSONArray values = data.optJSONArray(property);
        if (values == null || values.length() == 0) {
            Object single = data.opt(property);
            return single instanceof org.json.JSONObject ? (org.json.JSONObject) single : null;
        }

        org.json.JSONObject fallback = null;
        for (int i = 0; i < values.length(); i++) {
            org.json.JSONObject row = values.optJSONObject(i);
            if (row == null) {
                continue;
            }
            if (fallback == null) {
                fallback = row;
            }
            org.json.JSONObject qualification = row.optJSONObject("_qualification");
            org.json.JSONObject targetMarket = qualification == null
                    ? null
                    : qualification.optJSONObject("targetMarket");
            if (targetMarket != null
                    && targetMarketCode.equals(targetMarket.optString("_code", ""))) {
                return row;
            }
        }
        return fallback;
    }

    private java.util.Map<String, java.util.List<org.json.JSONObject>> characteristicMap(
            org.json.JSONObject data) {

        java.util.Map<String, java.util.List<org.json.JSONObject>> result = new java.util.LinkedHashMap<>();
        if (data == null) {
            return result;
        }
        org.json.JSONArray records = data.optJSONArray("_characteristicRecords");
        if (records == null) {
            return result;
        }

        for (int i = 0; i < records.length(); i++) {
            org.json.JSONObject record = records.optJSONObject(i);
            if (record == null) {
                continue;
            }
            org.json.JSONObject q = record.optJSONObject("_qualification");
            org.json.JSONObject characteristic = q == null ? null : q.optJSONObject("characteristic");
            String identifier = characteristic == null ? "" : characteristic.optString("_code", "");
            if (blank(identifier)) {
                continue;
            }
            result.computeIfAbsent(identifier, k -> new java.util.ArrayList<>()).add(record);
        }
        return result;
    }

    /**
     * Mirrors DBAccessDataStub's characteristic query:
     * scalar Value when present, otherwise Spanish LookupValueLang.Name.
     */
    private String characteristicJdbcValue(
            java.util.Map<String, java.util.List<org.json.JSONObject>> map,
            String identifier) {

        java.util.List<org.json.JSONObject> rows = map.get(identifier);
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        for (org.json.JSONObject row : rows) {
            Object value = firstCharacteristicValue(row);
            if (value instanceof org.json.JSONObject) {
                org.json.JSONObject o = (org.json.JSONObject) value;
                String label = o.optString("_label", "");
                if (!blank(label)) {
                    return label;
                }
                String code = o.optString("_code", "");
                if (!blank(code)) {
                    return code;
                }
            } else if (value != null && value != org.json.JSONObject.NULL) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private String characteristicCode(
            java.util.Map<String, java.util.List<org.json.JSONObject>> map,
            String identifier) {

        java.util.List<org.json.JSONObject> rows = map.get(identifier);
        if (rows == null || rows.isEmpty()) {
            return "";
        }
        for (org.json.JSONObject row : rows) {
            Object value = firstCharacteristicValue(row);
            if (value instanceof org.json.JSONObject) {
                org.json.JSONObject o = (org.json.JSONObject) value;
                String code = o.optString("_code", "");
                if (!blank(code)) {
                    return code;
                }
                String label = o.optString("_label", "");
                if (!blank(label)) {
                    return label;
                }
            } else if (value != null && value != org.json.JSONObject.NULL) {
                return String.valueOf(value);
            }
        }
        return "";
    }

    private Object firstCharacteristicValue(org.json.JSONObject record) {
        org.json.JSONArray recordLang = record == null ? null : record.optJSONArray("_recordLang");
        if (recordLang == null || recordLang.length() == 0) {
            return null;
        }

        // Prefer language-independent data where available, matching the way
        // existing code reads the first _recordLang entry; otherwise first row.
        org.json.JSONObject selected = null;
        for (int i = 0; i < recordLang.length(); i++) {
            org.json.JSONObject row = recordLang.optJSONObject(i);
            if (row == null) {
                continue;
            }
            if (selected == null) {
                selected = row;
            }
            org.json.JSONObject q = row.optJSONObject("_qualification");
            org.json.JSONObject language = q == null ? null : q.optJSONObject("language");
            if (language != null && language.optInt("_key", Integer.MIN_VALUE) == -1) {
                selected = row;
                break;
            }
        }
        if (selected == null) {
            return null;
        }
        org.json.JSONArray values = selected.optJSONArray("values");
        return values == null || values.length() == 0 ? null : values.opt(0);
    }

    private org.json.JSONObject languageByKey(org.json.JSONArray lang, int languageKey) {
        if (lang == null) {
            return null;
        }
        for (int i = 0; i < lang.length(); i++) {
            org.json.JSONObject row = lang.optJSONObject(i);
            if (row == null) {
                continue;
            }
            org.json.JSONObject q = row.optJSONObject("_qualification");
            org.json.JSONObject language = q == null ? null : q.optJSONObject("language");
            if (language != null && language.optInt("_key", Integer.MIN_VALUE) == languageKey) {
                return row;
            }
        }
        return null;
    }

    private String primaryProductTaxonomyTemplate(org.json.JSONArray maps) {
        if (maps == null) {
            return "";
        }
        for (int i = 0; i < maps.length(); i++) {
            org.json.JSONObject row = maps.optJSONObject(i);
            org.json.JSONObject q = row == null ? null : row.optJSONObject("_qualification");
            org.json.JSONObject sg = q == null ? null : q.optJSONObject("structureGroup");
            if (sg == null) {
                continue;
            }
            String externalId = sg.optString("_externalId", "");
            if (externalId.endsWith("@'" + PRIMARY_PRODUCT_TAXONOMY + "'")) {
                return externalIdentifierLeftPart(externalId);
            }
        }
        return "";
    }

    private String externalIdentifierLeftPart(String externalId) {
        if (blank(externalId)) {
            return "";
        }
        int at = externalId.indexOf('@');
        String left = at < 0 ? externalId : externalId.substring(0, at);
        if (left.length() >= 2 && left.startsWith("'") && left.endsWith("'")) {
            left = left.substring(1, left.length() - 1);
        }
        return left;
    }

    private String lookupCode(org.json.JSONObject owner, String property) {
        return owner == null ? "" : lookupCodeOrScalar(owner.opt(property));
    }

    private String lookupCodeOrScalar(Object value) {
        if (value == null || value == org.json.JSONObject.NULL) {
            return "";
        }
        if (value instanceof org.json.JSONObject) {
            org.json.JSONObject object = (org.json.JSONObject) value;
            String code = object.optString("_code", "");
            if (!blank(code)) {
                return code;
            }
            String externalId = object.optString("_externalId", "");
            if (!blank(externalId)) {
                return externalIdentifierLeftPart(externalId);
            }
            String label = object.optString("_label", "");
            return safe(label);
        }
        return String.valueOf(value);
    }

    private org.json.JSONObject emptyProductData(String identifier) {
        return new org.json.JSONObject()
                .put("product", safe(identifier))
                .put("Section", "")
                .put("ItemGroup", "")
                .put("ItemGroupS4H", "")
                .put("BrandName", "")
                .put("BRAND_ID_S4H", "")
                .put("Business", "")
                .put("SKU", "")
                .put("SupplierID", "")
                .put("Template", "")
                .put("CurrentStatus", "")
                .put("AssignTakeNoTake", "")
                .put("SAPObjectType", "")
                .put("FotoTomadaLiverpool", "")
                .put("MainBarCode", "")
                .put("MainBarCodeS4H", "")
                .put("SupplierPartNumber", "");
    }

    private org.json.JSONObject emptyArticleData(String identifier) {
        return new org.json.JSONObject()
                .put("variant", safe(identifier))
                .put("ProductNo", "")
                .put("ColoursLiverpoolAtt", "")
                .put("TamanoUnico", "")
                .put("ProductImage", "")
                .put("AssignTakeNoTake", "")
                .put("SKU", "")
                .put("MainBarCode", "")
                .put("MainBarCodeS4H", "")
                .put("SupplierPartNumber", "");
    }

    private java.util.List<String> normalizeStrings(java.util.Collection<String> values) {
        java.util.LinkedHashSet<String> normalized = new java.util.LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                if (!blank(value)) {
                    normalized.add(value.trim());
                }
            }
        }
        return new java.util.ArrayList<>(normalized);
    }

    private String scalarString(Object value) {
        return value == null || value == org.json.JSONObject.NULL ? "" : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        if (values != null) {
            for (String value : values) {
                if (!blank(value)) {
                    return value;
                }
            }
        }
        return "";
    }

    private String languageCode(int languageID) {
        // All supplied production code maps LanguageID=10 to Spanish (es).
        return languageID == 10 ? "es" : String.valueOf(languageID);
    }

    private String escapeQuery(String value) {
        return safe(value).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void log(String message) {
        if (log != null) {
            log.log(message);
        } else {
            System.out.println(message);
        }
    }

    @Override
    public void close() {
        // RESTWrapper/RESTWorkshop do not own a persistent JDBC-like resource.
    }
}
