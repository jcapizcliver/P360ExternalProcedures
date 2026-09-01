package mx.com.liverpool.p360.services.core.temp.xml.local.neostream;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.net.DataRequestor;
import mx.com.liverpool.p360.services.core.temp.xml.local.neostream.StepXmlStreamingParser.Product;
import mx.com.liverpool.p360.services.core.temp.xml.local.neostream.StepXmlStreamingParser.Value;

/**
 * Collision/identity repair only.
 *
 * Normal status, texts, SKU/EAN, variants and hierarchy writes are handled by
 * StepWriterPipeline after this processor has completely flushed. That gives
 * identity changes a hard barrier before ordinary data is written.
 */
public final class StepSecondOpinionProcessor {

    private final RESTWrapper rw = new RESTWrapper();
    private final java.util.Map<String, String> qp = new java.util.TreeMap<>();
    private final StepDbSnapshot db;
    private final ELog elog;

    private static final java.util.Set<String> IDENTITY_CHARACTERISTICS =
            new java.util.HashSet<>(java.util.Arrays.asList(
                    "SKU", "MainBarCode", "MainBarCodeS4H"));

    private static final java.util.Set<String> VARIANT_MATCH_CHARACTERISTICS =
            new java.util.HashSet<>(java.util.Arrays.asList(
                    "SKU", "MainBarCode", "MainBarCodeS4H",
                    "ColoursLiverpoolAtt", "TamanoUnico", "SupplierPartNumber"));

    private final RequestHandler reqSuppressSKUAndEAN;
    private final RequestHandler reqSuppressSKUAndEANArticle;
    private final RequestHandler reqAltID;
    private final RequestHandler reqArticleAltID;
    private final RequestHandler reqPID;
    private final RequestHandler reqAID;

    public StepSecondOpinionProcessor(
            StepDbSnapshot db,
            ELog elog) {
        this.db = db;
        this.elog = elog;
        this.qp.put("includeObjectsInProtocol", "false");

        reqSuppressSKUAndEAN = new RequestHandler(new JSONArray()
                .put(new JSONObject().put("identifier", "Product2G.SKU"))
                .put(new JSONObject().put("identifier", "Product2G.EAN"))
                .put(new JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"))
                .put(new JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"))
                .put(new JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)")),
                batch("p360.step.batch.secondopinion", 1000),
                request -> rw.writeData("list", "Product2G", null, qp, request, this::log));

        reqSuppressSKUAndEANArticle = new RequestHandler(new JSONArray()
                .put(new JSONObject().put("identifier", "Article.SKU"))
                .put(new JSONObject().put("identifier", "Article.EAN"))
                .put(new JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"))
                .put(new JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"))
                .put(new JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)")),
                batch("p360.step.batch.secondopinion", 1000),
                request -> rw.writeData("list", "Article", null, qp, request, this::log));

        reqAltID = new RequestHandler(
                new JSONArray().put(new JSONObject().put("identifier", "Product2G.AltProductNo")),
                batch("p360.step.batch.secondopinion", 1000),
                request -> rw.writeData("list", "Product2G", null, qp, request, this::log));
        reqArticleAltID = new RequestHandler(
                new JSONArray().put(new JSONObject().put("identifier", "Article.SupplierAltAID")),
                batch("p360.step.batch.secondopinion", 1000),
                request -> rw.writeData("list", "Article", null, qp, request, this::log));
        reqPID = new RequestHandler(
                new JSONArray().put(new JSONObject().put("identifier", "Product2G.ProductNo")),
                batch("p360.step.batch.secondopinion", 1000),
                request -> rw.writeData("list", "Product2G", null, qp, request, this::log));
        reqAID = new RequestHandler(
                new JSONArray().put(new JSONObject().put("identifier", "Article.SupplierAID")),
                batch("p360.step.batch.secondopinion", 1000),
                request -> rw.writeData("list", "Article", null, qp, request, this::log));
    }

    public void accept(Product product) {
        if (product == null || product.getId() == null) return;
        if (product.getParentId() != null
                && product.getParentId().matches("^(S?[0-9]+)$")) {
            return;
        }

        Map<String, Value> values = product.getValueMap();
        String sku = text(values.get("SKU"));
        String incomingProductId = product.getId();
        String effectiveProductId = incomingProductId;

        String skuOwner = sku.isEmpty() ? null : db.productBySku.get(sku);
        if (skuOwner != null && !skuOwner.isBlank() && !skuOwner.equals(incomingProductId)) {
            JSONObject incomingData = db.productData.get(incomingProductId);
            boolean incomingAlreadyHasData = !isEmptyProductData(incomingData);

            if (!incomingAlreadyHasData) {
                if (skuOwner.length() < 15) {
                    String ownerObjectId = resolveInternalObjectId("Product2G", skuOwner);
                    if (ownerObjectId != null) {
                        reqAltID.addRow(row(ownerObjectId, new JSONArray().put(skuOwner)));
                        reqPID.addRow(row(ownerObjectId, new JSONArray().put(incomingProductId)));
                    }
                } else {
                    reqAltID.addRow(row("'" + skuOwner + "'@1",
                            new JSONArray().put(incomingProductId)));
                    reqAltID.addRow(row("'" + incomingProductId + "'@1",
                            new JSONArray().put(skuOwner)));
                }
            } else {
                if (skuOwner.length() < 15) {
                    String ownerObjectId = resolveInternalObjectId("Product2G", skuOwner);
                    if (ownerObjectId != null) {
                        reqSuppressSKUAndEAN.addRow(row(ownerObjectId,
                                new JSONArray().put("").put("").put("").put("").put("")));
                    }
                } else {
                    log("Resolviendo combinación de productos: "
                            + skuOwner + " <-> " + incomingProductId);
                    resuelveCombinación(skuOwner, incomingProductId);
                    effectiveProductId = skuOwner;
                }
            }
        }

        for (Product child : product.getProducts()) {
            processChild(child, effectiveProductId);
        }
        if (product.getProducts().isEmpty()
                && !product.getUserTypeId().startsWith("SalesItemFamily")) {
            processChild(product, effectiveProductId);
        }
    }

    private void processChild(
            Product child,
            String parentProductId) {

        if (child == null || child.getId() == null) return;
        Map<String, Value> values = child.getValueMap();
        String sku = text(values.get("SKU"));
        String incomingArticleId = child.getId();

        String skuOwner = sku.isEmpty() ? null : db.articleBySku.get(sku);
        if (skuOwner != null && !skuOwner.isBlank() && !skuOwner.equals(incomingArticleId)) {
            JSONObject incomingData = db.articleData.get(incomingArticleId);
            boolean incomingAlreadyHasData = !isEmptyArticleData(incomingData);

            if (!incomingAlreadyHasData) {
                if (skuOwner.length() < 15) {
                    String ownerObjectId = resolveInternalObjectId("Article", skuOwner);
                    if (ownerObjectId != null) {
                        reqArticleAltID.addRow(row(ownerObjectId, new JSONArray().put(skuOwner)));
                        reqAID.addRow(row(ownerObjectId, new JSONArray().put(incomingArticleId)));

                        // This remains a write operation, not a data lookup.
                        try(DBAccessDataStub dastub = new DBAccessDataStub( new ELog() {
							
							@Override
							public void logE(Exception e) {
							}
							
							@Override
							public void log(String message) {
							}
						} )){
                            new DataRequestor(dastub).putSkuSupplierAID(new JSONArray().put(
                                    new JSONObject()
                                            .put("supplierAID", incomingArticleId)
                                            .put("productNo", parentProductId)
                                            .put("sku", sku)));
                        } catch (Exception e) {
                            logE(e);
                        }
                    }
                } else {
                    reqArticleAltID.addRow(row("'" + skuOwner + "'@1",
                            new JSONArray().put(incomingArticleId)));
                    reqArticleAltID.addRow(row("'" + incomingArticleId + "'@1",
                            new JSONArray().put(skuOwner)));
                }
            } else if (skuOwner.length() < 15) {
                String ownerObjectId = resolveInternalObjectId("Article", skuOwner);
                if (ownerObjectId != null) {
                    reqSuppressSKUAndEANArticle.addRow(row(ownerObjectId,
                            new JSONArray().put("").put("").put("").put("").put("")));
                }
            }
        }
    }

    public void finish() {
        reqPID.sendData();
        reqAID.sendData();
        reqArticleAltID.sendData();
        reqAltID.sendData();
        reqSuppressSKUAndEAN.sendData();
        reqSuppressSKUAndEANArticle.sendData();
    }

    private String resolveInternalObjectId(String entity, String identifier) {
        if (identifier == null || identifier.isBlank()) return null;
        if ("Product2G".equals(entity)) {
            return db.productObjectIdByIdentifier.get(identifier);
        }
        if ("Article".equals(entity)) {
            return db.articleObjectIdByIdentifier.get(identifier);
        }
        return null;
    }

    private static int batch(String property, int defaultValue) {
        return Math.max(1, Integer.getInteger(property, defaultValue));
    }

    private JSONObject row(String objectId, JSONArray values) {
        return new JSONObject()
                .put("object", new JSONObject().put("id", objectId))
                .put("values", values);
    }

    private boolean isEmptyProductData(JSONObject data) {
        if (data == null) return true;
        String[] fields = {
                "Section", "ItemGroup", "ItemGroupS4H", "BrandName", "BRAND_ID_S4H",
                "Business", "SKU", "SupplierID", "Template", "CurrentStatus",
                "AssignTakeNoTake", "SAPObjectType", "FotoTomadaLiverpool",
                "MainBarCode", "MainBarCodeS4H", "SupplierPartNumber"
        };
        for (String field : fields) {
            if (!data.optString(field, "").isEmpty()) return false;
        }
        return true;
    }

    private boolean isEmptyArticleData(JSONObject data) {
        if (data == null) return true;
        String[] fields = {
                "ProductNo", "ColoursLiverpoolAtt", "TamanoUnico", "ProductImage",
                "AssignTakeNoTake", "SKU", "MainBarCode", "MainBarCodeS4H",
                "SupplierPartNumber"
        };
        for (String field : fields) {
            if (!data.optString(field, "").isEmpty()) return false;
        }
        return true;
    }

    private String text(Value value) {
        return value == null || value.getText() == null ? "" : value.getText();
    }

    private String idOrText(Value value) {
        return value == null ? "" : value.idOrText();
    }

    private void resuelveCombinación(String id1, String id2) {
		if(id1 != null && id2 != null && !id1.equals(id2)) {
			org.json.JSONObject response1 = rw.getRw().makeRequest("GET", "/object/Product2G/'" + rw.getRw().encode(id1) + "'@1?includeIds=true&includeLabels=true");
			org.json.JSONObject response2 = rw.getRw().makeRequest("GET", "/object/Product2G/'" + rw.getRw().encode(id2) + "'@1?includeIds=true&includeLabels=true");
			
			org.json.JSONObject data1 = response1 != null && response1.has("_data") ? response1.getJSONObject("_data") : null;
			org.json.JSONObject data2 = response2 != null && response2.has("_data") ? response2.getJSONObject("_data") : null;
			
			if(data1 != null && data2 != null) {
				
				/**********************************************************
				 * 
				 * Hacer la lógica de comparación y merge y todo en data1
				 * 
				 ***************************************************************/
				
				java.util.List<String> itemsOfProduct1 = collectArticleObjectIdsByProduct(id1);
			    java.util.List<String> itemsOfProduct2 = collectArticleObjectIdsByProduct(id2);

			    java.util.List<VariantInfo> variants1 = loadVariantInfos(itemsOfProduct1, "id1");
			    java.util.List<VariantInfo> variants2 = loadVariantInfos(itemsOfProduct2, "id2");

			    java.util.Map<String, java.util.List<VariantInfo>> signatureIndex = buildSignatureIndex(variants1, variants2);

			    VariantMergeDecision decision = decideVariantMovement(variants2, signatureIndex);

			    deleteProductReferencesFromArticles(itemsOfProduct2);

			    createProductReferencesToId1(toArticleObjectIds(decision.articlesToMoveToProduct1), id1);

			    clearSkuAndEanFromArticles(decision.duplicatedArticlesToDetachAndClean);

			    mergeMissingProductData(data1, data2);
			    clearProductSkuAndEan(data2);

			    java.util.Map<String, String> qp = new java.util.HashMap<>();

			    org.json.JSONObject write1 = rw.getRw().makeRequest("PUT", "/object/Product2G/'" + rw.getRw().encode(id1) + "'@1", qp, data1.toString());

			    if(write1 == null) {
			        log("PANIC: from id1=" + id1 + ". rawResponse=" + rw.getRw().getRawResponse());
			        return;
			    }

			    qp.clear();

			    org.json.JSONObject write2 = rw.getRw().makeRequest("PUT", "/object/Product2G/'" + rw.getRw().encode(id2) + "'@1", qp, data2.toString());

			    if(write2 == null) {
			        log("PANIC: from id2=" + id2 + ". rawResponse=" + rw.getRw().getRawResponse());
			        return;
			    }

			    log("Combinación finished. id1=" + id1
			            + ", id2=" + id2
			            + ", moveToId1=" + decision.articlesToMoveToProduct1.size()
			            + ", cleanDuplicated=" + decision.duplicatedArticlesToDetachAndClean.size());
				
			}
		}
	}
	
	private java.util.List<String> toArticleObjectIds(java.util.List<VariantInfo> variants) {
	    java.util.List<String> ids = new java.util.ArrayList<>();

	    for(VariantInfo variant : variants) {
	        ids.add(variant.articleObjectId);
	    }

	    return ids;
	}
	
	private void mergeMissingProductData(org.json.JSONObject data1, org.json.JSONObject data2) {
	    java.util.Set<String> excluded = new java.util.HashSet<>();

	    excluded.add("identifier");
	    excluded.add("sku");
	    excluded.add("gtin");
	    excluded.add("statusModification");
	    excluded.add("log");
	    excluded.add("ownLog");

	    mergeObjectMissing(data1, data2, excluded);
	}
	
	private void clearProductSkuAndEan(org.json.JSONObject data) {
	    data.put("sku", org.json.JSONObject.NULL);
	    data.put("gtin", org.json.JSONObject.NULL);
	    clearCharacteristicRecords(data, IDENTITY_CHARACTERISTICS);
	}
	
	private java.util.List<VariantInfo> loadVariantInfos(java.util.List<String> articleObjectIds, String productOwner) {
	    java.util.List<VariantInfo> result = new java.util.ArrayList<>();

	    for(String articleObjectId : articleObjectIds) {
	        org.json.JSONObject response = rw.getRw().makeRequest(
	                "GET",
	                "/object/Article/" + articleObjectId + "?includeIds=true&includeLabels=true"
	        );

	        org.json.JSONObject data = response != null && response.has("_data")
	                ? response.getJSONObject("_data")
	                : null;

	        if(data == null) {
	            log("No se pudo leer Article. owner=" + productOwner + ", article=" + articleObjectId);
	            continue;
	        }

	        java.util.Set<String> signatures = buildVariantSignatures(data);

	        if(signatures.isEmpty()) {
	            log("Article sin firma útil. owner=" + productOwner + ", article=" + articleObjectId);
	        }

	        result.add(new VariantInfo(articleObjectId, productOwner, data, signatures));
	    }

	    return result;
	}
	
	private java.util.Set<String> buildVariantSignatures(org.json.JSONObject data) {
	    java.util.Set<String> signatures = new java.util.LinkedHashSet<>();

	    String sku = stringValue(data.opt("sku"));
	    String gtin = stringValue(data.opt("gtin"));

	    if(!isBlank(sku)) {
	        signatures.add("SKU|" + normalize(sku));
	    }

	    if(!isBlank(gtin)) {
	        signatures.add("EAN|" + normalize(gtin));
	    }

	    java.util.Map<String, String> characteristicValues = extractCharacteristicValues(data);

	    addSignatureIfPresent(signatures, "SKU", characteristicValues.get("SKU"));
	    addSignatureIfPresent(signatures, "MainBarCode", characteristicValues.get("MainBarCode"));
	    addSignatureIfPresent(signatures, "MainBarCodeS4H", characteristicValues.get("MainBarCodeS4H"));

	    String color = firstNotBlank(
	            extractMxExtraDataValue(data, "coloursLiverpoolAtt"),
	            characteristicValues.get("ColoursLiverpoolAtt")
	    );

	    String size = firstNotBlank(
	            extractMxExtraDataValue(data, "tamanoUnico"),
	            characteristicValues.get("TamanoUnico")
	    );

	    String supplierPartNumber = firstNotBlank(
	            extractMxExtraDataValue(data, "supplierPartNumber"),
	            characteristicValues.get("SupplierPartNumber")
	    );

	    if(!isBlank(color) && !isBlank(size) && !isBlank(supplierPartNumber)) {
	        signatures.add("COLOR_SIZE_MODEL|" + normalize(color) + "|" + normalize(size) + "|" + normalize(supplierPartNumber));
	    }

	    return signatures;
	}
	
	private java.util.Map<String, String> extractCharacteristicValues(org.json.JSONObject data) {
	    java.util.Map<String, String> valuesByCode = new java.util.HashMap<>();

	    org.json.JSONArray records = data.optJSONArray("_characteristicRecords");

	    if(records == null) {
	        return valuesByCode;
	    }

	    for(int i = 0; i < records.length(); i++) {
	        org.json.JSONObject record = records.optJSONObject(i);

	        if(record == null) {
	            continue;
	        }

	        String code = nestedValue(record, "_qualification.characteristic._code");

	        if(isBlank(code)) {
	            continue;
	        }

	        if(!VARIANT_MATCH_CHARACTERISTICS.contains(code)) {
	            continue;
	        }

	        String value = firstRecordValue(record);

	        if(!isBlank(value)) {
	            valuesByCode.put(code, value);
	        }
	    }

	    return valuesByCode;
	}

	private String firstRecordValue(org.json.JSONObject record) {
	    org.json.JSONArray recordLang = record.optJSONArray("_recordLang");

	    if(recordLang == null) {
	        return "";
	    }

	    for(int i = 0; i < recordLang.length(); i++) {
	        org.json.JSONObject lang = recordLang.optJSONObject(i);

	        if(lang == null) {
	            continue;
	        }

	        org.json.JSONArray values = lang.optJSONArray("values");

	        if(values == null || values.length() == 0) {
	            continue;
	        }

	        Object value = values.opt(0);

	        if(value == null || value == org.json.JSONObject.NULL) {
	            continue;
	        }

	        if(value instanceof org.json.JSONObject) {
	            org.json.JSONObject valueObject = (org.json.JSONObject) value;

	            String code = valueObject.optString("_code", "");
	            if(!isBlank(code)) {
	                return code;
	            }

	            String label = valueObject.optString("_label", "");
	            if(!isBlank(label)) {
	                return label;
	            }

	            org.json.JSONObject key = valueObject.optJSONObject("_key");
	            if(key != null) {
	                String externalId = key.optString("_externalId", "");
	                if(!isBlank(externalId)) {
	                    return externalId;
	                }

	                String internalId = key.optString("_internalId", "");
	                if(!isBlank(internalId)) {
	                    return internalId;
	                }
	            }

	            return valueObject.toString();
	        }

	        return String.valueOf(value).trim();
	    }

	    return "";
	}

	private String extractMxExtraDataValue(org.json.JSONObject data, String fieldName) {
	    String value = extractMxExtraDataValueFromArray(data.optJSONArray("extraData"), fieldName);

	    if(!isBlank(value)) {
	        return value;
	    }

	    return extractMxExtraDataValueFromArray(data.optJSONArray("productExtraData"), fieldName);
	}

	private String extractMxExtraDataValueFromArray(org.json.JSONArray array, String fieldName) {
	    if(array == null) {
	        return "";
	    }

	    for(int i = 0; i < array.length(); i++) {
	        org.json.JSONObject item = array.optJSONObject(i);

	        if(item == null) {
	            continue;
	        }

	        String targetMarket = firstNotBlank(
	                nestedValue(item, "_qualification.targetMarket._code"),
	                nestedValue(item, "_qualification.targetMarket._key"),
	                nestedValue(item, "_qualification.targetMarket._label")
	        );

	        if(!"MX".equalsIgnoreCase(targetMarket) && !"Mexico".equalsIgnoreCase(targetMarket)) {
	            continue;
	        }

	        Object rawValue = item.opt(fieldName);

	        if(rawValue == null || rawValue == org.json.JSONObject.NULL) {
	            continue;
	        }

	        if(rawValue instanceof org.json.JSONObject) {
	            org.json.JSONObject object = (org.json.JSONObject) rawValue;

	            String code = object.optString("_code", "");
	            if(!isBlank(code)) {
	                return code;
	            }

	            String label = object.optString("_label", "");
	            if(!isBlank(label)) {
	                return label;
	            }

	            org.json.JSONObject key = object.optJSONObject("_key");
	            if(key != null) {
	                String externalId = key.optString("_externalId", "");
	                if(!isBlank(externalId)) {
	                    return externalId;
	                }

	                String internalId = key.optString("_internalId", "");
	                if(!isBlank(internalId)) {
	                    return internalId;
	                }
	            }

	            return object.toString();
	        }

	        return String.valueOf(rawValue).trim();
	    }

	    return "";
	}
	
	private void addSignatureIfPresent(java.util.Set<String> signatures, String name, String value) {
	    if(!isBlank(value)) {
	        signatures.add(name + "|" + normalize(value));
	    }
	}

	private String stringValue(Object value) {
	    if(value == null || value == org.json.JSONObject.NULL) {
	        return "";
	    }

	    return String.valueOf(value).trim();
	}

	private String normalize(String value) {
	    if(value == null) {
	        return "";
	    }

	    return value.trim().toUpperCase(java.util.Locale.ROOT);
	}

	private java.util.Map<String, java.util.List<VariantInfo>> buildSignatureIndex(
	        java.util.List<VariantInfo> variants1,
	        java.util.List<VariantInfo> variants2) {

	    java.util.Map<String, java.util.List<VariantInfo>> index = new java.util.LinkedHashMap<>();

	    addToSignatureIndex(index, variants1);
	    addToSignatureIndex(index, variants2);

	    return index;
	}

	private void addToSignatureIndex(
	        java.util.Map<String, java.util.List<VariantInfo>> index,
	        java.util.List<VariantInfo> variants) {

	    for(VariantInfo variant : variants) {
	        for(String signature : variant.signatures) {
	            index.computeIfAbsent(signature, k -> new java.util.ArrayList<>()).add(variant);
	        }
	    }
	}
	
	private void deleteProductReferencesFromArticles(java.util.List<String> itemsOfTheProduct) {
	    java.util.Map<String, String> qp = new java.util.HashMap<>();
	    StringBuilder sb = new StringBuilder();
	    int a = 0;

	    for(String internalArticleId : itemsOfTheProduct) {
	        sb.append(sb.length() == 0 ? "" : ",").append(internalArticleId);
	        a++;

	        if(a % 1000 == 0) {
	            qp.put("items", sb.toString());
	            rw.deleteData("list", "Article", "ProductReference", "byItems", qp, this::log);
	            sb.setLength(0);
	            qp.clear();
	        }
	    }

	    if(sb.length() > 0) {
	        qp.put("items", sb.toString());
	        rw.deleteData("list", "Article", "ProductReference", "byItems", qp, this::log);
	        sb.setLength(0);
	        qp.clear();
	    }
	}

	private void createProductReferencesToId1(java.util.List<String> itemsOfTheProduct, String id1) {
	    java.util.Map<String, String> qp = new java.util.HashMap<>();
	    qp.put("includeObjectsInProtocol", "false");

	    RequestHandler rh = new RequestHandler(
	            new org.json.JSONArray().put(
	                    new org.json.JSONObject().put("identifier", "ProductReference.ReferencedSupplierAid")
	            ),
	            1000,
	            request -> rw.writeData("list", "Article", "ProductReference", qp, request, this::log)
	    );

	    for(String internalId : itemsOfTheProduct) {
	        rh.addRow(
	                new org.json.JSONObject()
	                        .put("object", new org.json.JSONObject().put("id", internalId))
	                        .put("qualification", new org.json.JSONObject().put("referencedSupplierAid", id1))
	                        .put("values", new org.json.JSONArray().put(id1))
	        );
	    }

	    rh.sendData();
	}

	private void mergeObjectMissing(org.json.JSONObject target, org.json.JSONObject source, java.util.Set<String> excludedKeys) {
	    for(Object keyObject : source.keySet()) {
	        String key = String.valueOf(keyObject);

	        if(excludedKeys != null && excludedKeys.contains(key)) {
	            continue;
	        }

	        Object sourceValue = source.opt(key);

	        if(isEmptyJsonValue(sourceValue)) {
	            continue;
	        }

	        Object targetValue = target.opt(key);

	        if(isEmptyJsonValue(targetValue)) {
	            target.put(key, cloneJsonValue(sourceValue));
	            continue;
	        }

	        if(sourceValue instanceof org.json.JSONObject && targetValue instanceof org.json.JSONObject) {
	            mergeObjectMissing((org.json.JSONObject) targetValue, (org.json.JSONObject) sourceValue, null);
	            continue;
	        }

	        if(sourceValue instanceof org.json.JSONArray && targetValue instanceof org.json.JSONArray) {
	            mergeArrayMissing(key, (org.json.JSONArray) targetValue, (org.json.JSONArray) sourceValue);
	        }
	    }
	}
	
	private java.util.List<String> collectArticleObjectIdsByProduct(String productIdentifier) {
	    java.util.Map<String, String> qp = new java.util.HashMap<>();
	    qp.put("pageSize", "2000");
	    qp.put("products", "'" + productIdentifier + "'@1");

	    java.util.List<String> items = new java.util.ArrayList<>();

	    rw.collectData("list", "Article", null, "byProducts", qp, row -> {
	        items.add(row.getJSONObject("object").getString("id"));
	    });

	    return items;
	}
	
	private void mergeArrayMissing(String sectionName, org.json.JSONArray targetArray, org.json.JSONArray sourceArray) {
	    java.util.Map<String, org.json.JSONObject> targetByKey = new java.util.LinkedHashMap<>();

	    for(int i = 0; i < targetArray.length(); i++) {
	        Object value = targetArray.opt(i);

	        if(value instanceof org.json.JSONObject) {
	            org.json.JSONObject object = (org.json.JSONObject) value;
	            targetByKey.put(buildArrayItemKey(sectionName, object), object);
	        }
	    }

	    for(int i = 0; i < sourceArray.length(); i++) {
	        Object sourceValue = sourceArray.opt(i);

	        if(!(sourceValue instanceof org.json.JSONObject)) {
	            if(!arrayContainsEquivalentValue(targetArray, sourceValue)) {
	                targetArray.put(cloneJsonValue(sourceValue));
	            }
	            continue;
	        }

	        org.json.JSONObject sourceObject = (org.json.JSONObject) sourceValue;
	        String sourceKey = buildArrayItemKey(sectionName, sourceObject);
	        org.json.JSONObject targetObject = targetByKey.get(sourceKey);

	        if(targetObject == null) {
	            targetArray.put(new org.json.JSONObject(sourceObject.toString()));
	        } else {
	            mergeObjectMissing(targetObject, sourceObject, null);
	        }
	    }
	}

	private String buildArrayItemKey(String sectionName, org.json.JSONObject object) {
	    if("lang".equals(sectionName)) {
	        return "lang|" + nestedValue(object, "_qualification.language._key");
	    }

	    if("structureGroupMap".equals(sectionName)) {
	        return "structureGroupMap|" + objectKey(object.optJSONObject("_qualification"), "structureGroup");
	    }

	    if("attribute".equals(sectionName)) {
	        String identifier = object.optString("identifier", "");
	        if(!isBlank(identifier)) {
	            return "attribute|" + identifier;
	        }

	        return "attribute|" + nestedValue(object, "_qualification.nameInKeyLang");
	    }

	    if("_characteristicRecords".equals(sectionName)) {
	        String characteristic = objectKey(object.optJSONObject("_qualification"), "characteristic");
	        String recordKey = nestedValue(object, "_qualification.recordKey");
	        String parentRecordKey = nestedValue(object, "_qualification.parentRecordKey");
	        return "_characteristicRecords|" + characteristic + "|" + recordKey + "|" + parentRecordKey;
	    }

	    if("productExtraData".equals(sectionName)) {
	        return "productExtraData|" + objectKey(object.optJSONObject("_qualification"), "targetMarket");
	    }

	    if("value".equals(sectionName)) {
	        String lang = nestedValue(object, "_qualification.language._key");
	        String identifier = nestedValue(object, "_qualification.identifier");
	        return "value|" + lang + "|" + identifier;
	    }

	    if("_recordLang".equals(sectionName)) {
	        return "_recordLang|" + nestedValue(object, "_qualification.language._key");
	    }

	    return sectionName + "|" + object.toString();
	}

	private String objectKey(org.json.JSONObject parent, String childName) {
	    if(parent == null) {
	        return "";
	    }

	    org.json.JSONObject child = parent.optJSONObject(childName);

	    if(child == null) {
	        return "";
	    }

	    org.json.JSONObject key = child.optJSONObject("_key");

	    if(key != null) {
	        String externalId = key.optString("_externalId", "");
	        String internalId = key.optString("_internalId", "");
	        String entityId = String.valueOf(key.opt("_entityId"));
	        return firstNotBlank(externalId, internalId, entityId);
	    }

	    String externalId = child.optString("_externalId", "");
	    String internalId = child.optString("_internalId", "");
	    String code = child.optString("_code", "");
	    String keyValue = String.valueOf(child.opt("_key"));

	    return firstNotBlank(externalId, internalId, code, keyValue);
	}

	private String nestedValue(org.json.JSONObject object, String path) {
	    if(object == null || isBlank(path)) {
	        return "";
	    }

	    String[] parts = path.split("\\.");
	    Object current = object;

	    for(String part : parts) {
	        if(!(current instanceof org.json.JSONObject)) {
	            return "";
	        }

	        current = ((org.json.JSONObject) current).opt(part);

	        if(current == null || current == org.json.JSONObject.NULL) {
	            return "";
	        }
	    }

	    return String.valueOf(current);
	}

	private void clearSkuAndEanFromArticles(java.util.List<VariantInfo> variants) {
	    for(VariantInfo variant : variants) {
	        org.json.JSONObject data = variant.data;

	        data.put("sku", org.json.JSONObject.NULL);
	        data.put("gtin", org.json.JSONObject.NULL);

	        clearCharacteristicRecords(data, IDENTITY_CHARACTERISTICS);

	        org.json.JSONObject writeResponse = rw.getRw().makeRequest(
	                "PUT",
	                "/object/Article/" + variant.articleObjectId,
	                new java.util.HashMap<>(),
	                data.toString()
	        );

	        if(writeResponse == null) {
	            log("PANIC: fallo PUT Article " + variant.articleObjectId + ". rawResponse=" + rw.getRw().getRawResponse());
	            return;
	        }
	    }
	}

	private void clearCharacteristicRecords(org.json.JSONObject data, java.util.Set<String> characteristicCodesToClear) {
	    org.json.JSONArray records = data.optJSONArray("_characteristicRecords");

	    if(records == null) {
	        return;
	    }

	    org.json.JSONArray kept = new org.json.JSONArray();

	    for(int i = 0; i < records.length(); i++) {
	        org.json.JSONObject record = records.optJSONObject(i);

	        if(record == null) {
	            kept.put(records.opt(i));
	            continue;
	        }

	        String code = nestedValue(record, "_qualification.characteristic._code");

	        if(characteristicCodesToClear.contains(code)) {
	            log("Quitando characteristicRecord de identidad: " + code);
	            continue;
	        }

	        kept.put(record);
	    }

	    data.put("_characteristicRecords", kept);
	}
	
	private VariantMergeDecision decideVariantMovement(
	        java.util.List<VariantInfo> variants2,
	        java.util.Map<String, java.util.List<VariantInfo>> signatureIndex) {

	    java.util.List<VariantInfo> articlesToMoveToProduct1 = new java.util.ArrayList<>();
	    java.util.List<VariantInfo> duplicatedArticlesToDetachAndClean = new java.util.ArrayList<>();

	    for(VariantInfo variant2 : variants2) {
	        if(variant2.signatures.isEmpty()) {
	            log("Article de id2 sin firma útil; se mueve por conservación. article=" + variant2.articleObjectId);
	            articlesToMoveToProduct1.add(variant2);
	            continue;
	        }

	        boolean matchedAgainstId1 = false;
	        java.util.Set<String> matchedRefsForLog = new java.util.LinkedHashSet<>();

	        for(String signature : variant2.signatures) {
	            java.util.List<VariantInfo> refs = signatureIndex.get(signature);

	            if(refs == null || refs.isEmpty()) {
	                continue;
	            }

	            for(VariantInfo ref : refs) {
	                matchedRefsForLog.add(signature + " -> " + ref.toString());

	                if(ref.belongsToProduct1()) {
	                    matchedAgainstId1 = true;
	                }
	            }
	        }

	        if(matchedAgainstId1) {
	            duplicatedArticlesToDetachAndClean.add(variant2);
	            log("Article duplicado contra id1; se despoja SKU/EAN. article="
	                    + variant2.articleObjectId
	                    + ", matches="
	                    + matchedRefsForLog);
	        } else {
	            articlesToMoveToProduct1.add(variant2);
	            log("Article de id2 sin coincidencia contra id1; se mueve a id1. article="
	                    + variant2.articleObjectId
	                    + ", matches="
	                    + matchedRefsForLog);
	        }
	    }

	    return new VariantMergeDecision(articlesToMoveToProduct1, duplicatedArticlesToDetachAndClean);
	}

	private static class VariantMergeDecision {
	    final java.util.List<VariantInfo> articlesToMoveToProduct1;
	    final java.util.List<VariantInfo> duplicatedArticlesToDetachAndClean;

	    VariantMergeDecision(
	            java.util.List<VariantInfo> articlesToMoveToProduct1,
	            java.util.List<VariantInfo> duplicatedArticlesToDetachAndClean) {

	        this.articlesToMoveToProduct1 = articlesToMoveToProduct1;
	        this.duplicatedArticlesToDetachAndClean = duplicatedArticlesToDetachAndClean;
	    }
	}
	
	private static class VariantInfo {
	    final String articleObjectId;
	    final String productOwner;
	    final org.json.JSONObject data;
	    final java.util.Set<String> signatures;

	    VariantInfo(String articleObjectId, String productOwner, org.json.JSONObject data, java.util.Set<String> signatures) {
	        this.articleObjectId = articleObjectId;
	        this.productOwner = productOwner;
	        this.data = data;
	        this.signatures = signatures;
	    }

	    boolean belongsToProduct1() {
	        return "id1".equals(productOwner);
	    }

	    @Override
	    public String toString() {
	        return productOwner + ":" + articleObjectId;
	    }
	}

	private boolean arrayContainsEquivalentValue(org.json.JSONArray array, Object value) {
	    String valueString = String.valueOf(value);

	    for(int i = 0; i < array.length(); i++) {
	        Object current = array.opt(i);

	        if(String.valueOf(current).equals(valueString)) {
	            return true;
	        }
	    }

	    return false;
	}

	private Object cloneJsonValue(Object value) {
	    if(value instanceof org.json.JSONObject) {
	        return new org.json.JSONObject(((org.json.JSONObject) value).toString());
	    }

	    if(value instanceof org.json.JSONArray) {
	        return new org.json.JSONArray(((org.json.JSONArray) value).toString());
	    }

	    return value;
	}

	private boolean isEmptyJsonValue(Object value) {
	    if(value == null || value == org.json.JSONObject.NULL) {
	        return true;
	    }

	    if(value instanceof String) {
	        return ((String) value).trim().isEmpty();
	    }

	    if(value instanceof org.json.JSONArray) {
	        return ((org.json.JSONArray) value).length() == 0;
	    }

	    if(value instanceof org.json.JSONObject) {
	        return ((org.json.JSONObject) value).length() == 0;
	    }

	    return false;
	}

	private boolean isBlank(String value) {
	    return value == null || value.trim().isEmpty();
	}

	private String firstNotBlank(String... values) {
	    if(values == null) {
	        return "";
	    }

	    for(String value : values) {
	        if(!isBlank(value) && !"null".equalsIgnoreCase(value)) {
	            return value;
	        }
	    }

	    return "";
	}
    

    private void log(String message) {
        if (elog != null) elog.log(message);
    }

    private void logE(Exception e) {
        if (elog != null) elog.logE(e);
    }
}
