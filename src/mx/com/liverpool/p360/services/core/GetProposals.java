package mx.com.liverpool.p360.services.core;

import java.io.Closeable;
import java.io.IOException;

import org.json.JSONObject;

/**
 * Direct-JDBC implementation of GetProposals.
 *
 * The response contract is intentionally kept close to the previous Object API
 * implementation. Product2G/Article data and characteristic trees are loaded in
 * bulk through DBAccessDataStub and adapted locally to the shape consumed by the
 * existing response-building logic.
 */
public class GetProposals implements Closeable {

  private static final int PRODUCT_ENTITY_ID = 1100;
  private static final int ARTICLE_ENTITY_ID = 1000;
  private static final int PRODUCT_CHARACTERISTIC_ENTITY_ID = 1160;
  private static final int PRODUCT_CHARACTERISTIC_LANG_ENTITY_ID = 1161;
  private static final int ARTICLE_CHARACTERISTIC_ENTITY_ID = 1060;
  private static final int ARTICLE_CHARACTERISTIC_LANG_ENTITY_ID = 1061;
  private static final int PRODUCT_DOMAIN_ENTITY_ID = 21006;
  private static final int ARTICLE_DOMAIN_ENTITY_ID = 21106;
  private static final String PRIMARY_PRODUCT_TAXONOMY = "PrimaryProductTaxonomy";
  private static final String CREATION_TYPE = "CreateProposal";

  private final DBAccessDataStub dastub = new DBAccessDataStub(new ELog() {
    @Override
    public void logE(Exception e) {
      GetProposals.this.logE(e);
    }

    @Override
    public void log(String message) {
      GetProposals.this.log(message);
    }
  });

  /* Cache is request-local because a GetProposals instance is created per leader request. */
  private final java.util.Map<String, java.util.Map<String, String>> atributosValidosPorPlantilla =
      new java.util.HashMap<>();
  private org.json.JSONObject globalMetadataCache;

  /* Populated once per request; gatherVariants/getVariants only read these maps. */
  private java.util.Map<String, java.util.Set<String>> batchVariantsByProduct =
      java.util.Collections.emptyMap();
  private java.util.Map<String, org.json.JSONObject> batchVariantsById =
      java.util.Collections.emptyMap();

  private final long myId;

  public GetProposals(String baseUrl, String encoded, long myId) {
    /* baseUrl/encoded are retained in the constructor for binary/source compatibility. */
    this.myId = myId;
  }

  public static void main(String[] args) {
  }

  public String run(String[] args) {
    return processFile(args[0]);
  }

  private String formatMillis(long millis) {
    int days = (int) (millis / (1000 * 60 * 60 * 24));
    millis -= days * 1000 * 60 * 60 * 24;
    int hours = (int) (millis / (1000 * 60 * 60));
    millis -= hours * 1000 * 60 * 60;
    int minutes = (int) (millis / (1000 * 60));
    millis -= minutes * 1000 * 60;
    int seconds = (int) (millis / 1000);
    millis -= seconds * 1000;
    return (days < 10 ? "0" : "") + days + ":"
        + (hours < 10 ? "0" : "") + hours + ":"
        + (minutes < 10 ? "0" : "") + minutes + ":"
        + (seconds < 10 ? "0" : "") + seconds + "." + millis;
  }

  private static final class BatchData {
    final java.util.Map<String, String> productsBySKU;
    final java.util.Map<String, String> productsByIdentifier;
    final java.util.Map<String, org.json.JSONObject> productsById;
    final java.util.Map<String, java.util.Set<String>> variantsByProduct;
    final java.util.Map<String, org.json.JSONObject> variantsById;
    final java.util.Map<String, String> templateNames;

    BatchData(
        java.util.Map<String, String> productsBySKU,
        java.util.Map<String, String> productsByIdentifier,
        java.util.Map<String, org.json.JSONObject> productsById,
        java.util.Map<String, java.util.Set<String>> variantsByProduct,
        java.util.Map<String, org.json.JSONObject> variantsById,
        java.util.Map<String, String> templateNames) {
      this.productsBySKU = productsBySKU;
      this.productsByIdentifier = productsByIdentifier;
      this.productsById = productsById;
      this.variantsByProduct = variantsByProduct;
      this.variantsById = variantsById;
      this.templateNames = templateNames;
    }
  }

  private String processFile(String input) {
    long init = System.currentTimeMillis();
    org.json.JSONArray responses = new org.json.JSONArray();

    try {
      log("An input: " + input);
      org.json.JSONObject request = new org.json.JSONObject(input);
      org.json.JSONArray rows = request.getJSONArray("products");

      BatchData batch = preload(rows);
      this.batchVariantsByProduct = batch.variantsByProduct;
      this.batchVariantsById = batch.variantsById;

      java.util.Set<String> multivalueCharacteristics = collectMultiValueCharacteristics(batch.productsById, batch.variantsById);
      java.util.Map<String, String> headers = java.util.Collections.emptyMap();

      for (int i = 0; i < rows.length(); i++) {
        org.json.JSONObject requested = rows.getJSONObject(i);
        String sku = requested.has("sku") ? requested.optString("sku", null) : null;
        String requestedIdentifier = requested.has("proposalId")
            ? requested.optString("proposalId", null)
            : null;

        String product = null;
        if (sku != null && !sku.isEmpty()) {
          product = batch.productsBySKU.get(sku);
        }
        if ((product == null || product.isEmpty())
            && requestedIdentifier != null
            && !requestedIdentifier.isEmpty()) {
          product = batch.productsByIdentifier.get(requestedIdentifier);
        }

        if (product == null || product.isEmpty()) {
          responses.put(new org.json.JSONObject()
              .put("status", "Not found")
              .put("sku", sku == null ? "" : sku)
              .put("productId", requestedIdentifier == null ? "" : requestedIdentifier));
          continue;
        }

        org.json.JSONObject productEntity = batch.productsById.get(product);
        if (productEntity == null || !productEntity.optBoolean("found", false)) {
          responses.put(new org.json.JSONObject()
              .put("status", "Not found")
              .put("sku", sku == null ? "" : sku)
              .put("productId", requestedIdentifier == null ? product : requestedIdentifier));
          continue;
        }

        String structureGroupId = structureGroup(productEntity, PRIMARY_PRODUCT_TAXONOMY);
        String structureGroupName = structureGroupId == null
            ? ""
            : batch.templateNames.getOrDefault(structureGroupId, "");

        org.json.JSONObject response = buildProductResponse(
            product,
            productEntity,
            sku,
            structureGroupId,
            structureGroupName,
            multivalueCharacteristics,
            headers);
        responses.put(response);
      }

      return responses.toString();
    } catch (Exception e) {
      logE(e);
      return responses.toString();
    } finally {
      log("Elapsed time: " + formatMillis(System.currentTimeMillis() - init));
      this.batchVariantsByProduct = java.util.Collections.emptyMap();
      this.batchVariantsById = java.util.Collections.emptyMap();
    }
  }

  private BatchData preload(org.json.JSONArray rows) {
    java.util.Set<String> skus = new java.util.LinkedHashSet<>();
    java.util.Set<String> identifiers = new java.util.LinkedHashSet<>();

    for (int i = 0; i < rows.length(); i++) {
      org.json.JSONObject row = rows.optJSONObject(i);
      if (row == null) {
        continue;
      }
      String sku = row.optString("sku", "");
      String identifier = row.optString("proposalId", "");
      if (!sku.isEmpty()) {
        skus.add(sku);
      }
      if (!identifier.isEmpty()) {
        identifiers.add(identifier);
      }
    }

    log("Bulk resolving " + skus.size() + " SKU(s) and " + identifiers.size() + " identifier(s).");
    java.util.Map<String, String> productsBySKU = dastub.getProductsBySKUs(skus);
    java.util.Map<String, String> productsByIdentifier = dastub.getProductsByIdentifiers(identifiers);

    java.util.Set<String> productIds = new java.util.LinkedHashSet<>();
    productIds.addAll(productsBySKU.values());
    productIds.addAll(productsByIdentifier.values());
    productIds.remove(null);
    productIds.remove("");

    log("Bulk loading " + productIds.size() + " Product2G entity graph(s).");
    java.util.Map<String, org.json.JSONObject> productsById =
        dastub.getEntityData(PRODUCT_ENTITY_ID, productIds, 10);

    log("Bulk loading product-to-variant relationships.");
    java.util.Map<String, java.util.Set<String>> variantsByProduct =
        dastub.getProductVariants(productIds);

    java.util.Set<String> variantIds = new java.util.LinkedHashSet<>();
    for (java.util.Set<String> values : variantsByProduct.values()) {
      if (values != null) {
        variantIds.addAll(values);
      }
    }

    log("Bulk loading " + variantIds.size() + " Article entity graph(s).");
    java.util.Map<String, org.json.JSONObject> variantsById =
        dastub.getEntityData(ARTICLE_ENTITY_ID, variantIds, 10);

    java.util.Set<String> templates = new java.util.LinkedHashSet<>();
    for (org.json.JSONObject product : productsById.values()) {
      if (product == null || !product.optBoolean("found", false)) {
        continue;
      }
      String template = structureGroup(product, PRIMARY_PRODUCT_TAXONOMY);
      if (template != null && !template.isEmpty()) {
        templates.add(template);
      }
    }

    java.util.Map<String, String> templateNames = new java.util.LinkedHashMap<>();
    for (String template : templates) {
      String name = dastub.getTemplateName(template);
      templateNames.put(template, name == null ? "" : name);
    }

    return new BatchData(
        productsBySKU,
        productsByIdentifier,
        productsById,
        variantsByProduct,
        variantsById,
        templateNames);
  }

  private org.json.JSONObject buildProductResponse(
      String product,
      org.json.JSONObject productEntity,
      String requestSku,
      String structureGroupId,
      String structureGroupName,
      java.util.Set<String> multivalueCharacteristics,
      java.util.Map<String, String> headers) {

    JSONObject response = toObjectApiResponse(productEntity, true);
    String business;
    java.util.Map<String, String> losQueSi;
    JSONObject json = null;
    String sku = requestSku;
    business = getBusinessValueFromObject(response, "Business");
    String modificationDate = null;
    //          log("Searching for following business: " + business + ", as well as for template: " + structureGroupId);
    //          a = System.currentTimeMillis();
    losQueSi = !"".equals(business) && !"".equals(structureGroupId) && business != null && structureGroupId != null ? gatherFieldsToSendByBusiness( headers, structureGroupId, business ) : new java.util.TreeMap<>();
    log("******************* " + losQueSi + " ###########################");
    //          log("Adding template metadata data took: " + formatMillis(System.currentTimeMillis() - a));
    if(losQueSi.isEmpty()) {
    //        	  a = System.currentTimeMillis();
            	  addGlobalData("Liverpool", losQueSi, null, null);
    //        	  log("Adding global meta data took: " + formatMillis(System.currentTimeMillis() - a));
    }
    try{
    	modificationDate = response.getJSONObject("_data").getJSONArray("ownLog").getJSONObject(0).getString("modificationDate");
    }catch(org.json.JSONException ignore){}
    //          log("Adding multivalued characteristics took: " + formatMillis(System.currentTimeMillis() - a));
    org.json.JSONArray characteristicRecords = response.getJSONObject( "_data" ).has("_characteristicRecords") ? response.getJSONObject( "_data" ).getJSONArray( "_characteristicRecords" ) : new org.json.JSONArray();
    JSONObject header = new org.json.JSONObject();
    JSONObject basicData = new org.json.JSONObject();
    JSONObject datosVenta = new org.json.JSONObject();
    JSONObject attributes = new org.json.JSONObject();
    JSONObject logisticData = new org.json.JSONObject();
    org.json.JSONArray photos = new org.json.JSONArray();
    org.json.JSONArray multiMedia = new org.json.JSONArray();
    JSONObject producto = new org.json.JSONObject();
    org.json.JSONArray children = null;
    	      JSONObject child = null;
    JSONObject helper = null;
    String descriptionLong = null;
    String descriptionLong2 = null;
    String embedCodeWEB = null;
    String embedCodeWAP = null;
    String refundPolicy = null;
    String sapObjectType = null;
    String supplierPartNumber = null;
    String supplierPartNumberChar = null;
    String ean = null;
    String supplierID = null;
    String direction = null;
    String section = null;
    String itemGroup = null;
    String itemGroupS4H = null;
    String brandName = null;
    String brandIDS4H = null;
    Object o = null;
    String vcs = null;
    String characteristicIdentifier = null;
    org.json.JSONObject characteristicRecord = null;
    String supplier = "";
    Boolean enriquecidoEnForo = false;
    java.util.LinkedList<org.json.JSONObject> rechazos = new java.util.LinkedList<>();
    org.json.JSONObject productVideo = new org.json.JSONObject();
    org.json.JSONObject ownersManual = new org.json.JSONObject();
    org.json.JSONObject liverpoolManual = new org.json.JSONObject();
    org.json.JSONObject nom = new org.json.JSONObject();
    org.json.JSONArray comments = new org.json.JSONArray();
    //          a = System.currentTimeMillis();
    
    for(int j=0; j<characteristicRecords.length(); j++) {
      characteristicRecord = characteristicRecords.getJSONObject( j );
      characteristicIdentifier = characteristicRecord.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" );
    
      if(characteristicIdentifier.endsWith("_Rechazo")) {
      	rechazos.addLast(characteristicRecord);
      	continue;
      } else if(characteristicIdentifier.equals("OwnersManual")) {
      	collectRejectionInformation(characteristicRecord, ownersManual);
      } else if(characteristicIdentifier.equals("LiverpoolManual")) {
      	collectRejectionInformation(characteristicRecord, liverpoolManual);
      } else if(characteristicIdentifier.equals("ProductVideo")) {
      	collectRejectionInformation(characteristicRecord, productVideo);
      } else if(characteristicIdentifier.equals("NOM")) {
      	collectRejectionInformation(characteristicRecord, nom);
      } else if("EnriquecidoEnForo".equals(characteristicIdentifier)) {
      	o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).get( 0 );
      	enriquecidoEnForo = (Boolean) o;
      } else if("SupplierID".equals(characteristicIdentifier)) {
      	o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
      	supplier = appendZeros( String.valueOf( o ), 20 );
      	continue;
      } else if("Comentario".equals(characteristicIdentifier)) {
      	collectRejectEntry(characteristicRecord, comments);
      	continue;
      }else if("MainBarCode".equals(characteristicIdentifier) || "MainBarCodeS4H".equals(characteristicIdentifier)) {
      	o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
      	ean = o instanceof JSONObject ? ((JSONObject)o).getString( "_label" ) : String.valueOf( o );
      }else if("SupplierPartNumber".equals(characteristicIdentifier)) {
      	o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
      	supplierPartNumberChar = o.toString();
      }
      vcs = losQueSi.get( characteristicIdentifier );
      if(vcs != null) {
        if("Header".equals(vcs)){
          o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
          if((!"Business".equals(characteristicIdentifier)) || "Business".equals(characteristicIdentifier) && (business == null || "".equals(business)) )
        	  header.put( characteristicIdentifier, o instanceof JSONObject ? ((JSONObject)o).getString( "_label" ) : String.valueOf( o ));
        }else if("Atributos".equals(vcs)){
      	  if(multivalueCharacteristics.contains(characteristicIdentifier)) {
      		org.json.JSONArray vls = new org.json.JSONArray();
      		for(int m=0; m<characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").length(); m++) {
      			o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( m );
      			vls.put(o instanceof JSONObject ? ((JSONObject)o).getString( "_label" ) : String.valueOf( o ));
      		}
      		attributes.put( characteristicIdentifier, vls);
      	  }else {
    	                o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
    	                attributes.put( characteristicIdentifier, o instanceof JSONObject ? ((JSONObject)o).getString( "_label" ) : String.valueOf( o ));
      	  }
        }else if("Datos de Venta".equals(vcs)){
          o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
          datosVenta.put( characteristicIdentifier, o instanceof JSONObject ? ((JSONObject)o).getString( "_label" ) : String.valueOf( o ));
        }else if("Datos Básicos".equals(vcs)){
      	  if("FSH_ID".equals(characteristicIdentifier)) {
    //            		  log("--------------------------------------------------------------->" + characteristicRecord);
      	  }
          o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
          try{
            basicData.put( characteristicIdentifier, o instanceof JSONObject ? ((JSONObject)o).getString( "_label" ) : String.valueOf( o ));
          }catch(org.json.JSONException e) {
            log("Bad reference attempted in: " + characteristicRecord.toString());
          }
        }else if("Fotografías".equals(vcs)){
      	  try{
    //            		  log("Got a picture for our compa... " + characteristicIdentifier);
      	                o = characteristicRecord;
      	                helper = new JSONObject();
      	                helper.put( "PhotoAssetType", characteristicIdentifier );
      	                json = (JSONObject) o;
      	                children = json.getJSONArray( "_children" );
      	                for(int k=0; k<children.length(); k++) {
      	                  child = children.getJSONObject( k );
      	                  if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Status" )) {
      	                    helper.put( "PhotoAssetStatus",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getJSONObject( 0 ).getString( "_label" ));
      	                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_URL" ) || child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_URL2" ) ) {
      	                     helper.put( "PhotoAssetURL",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
      	                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Name" ) || child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Name2" )) {
      	                     helper.put( "PhotoAssetName",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
      	                   }
      	                }
      	                photos.put( helper );
      	              }catch(org.json.JSONException e) {
    //  	                log("ERROR: " + e.getMessage() + ". " + String.valueOf(child) + " ## " + characteristicIdentifier);
      	              }
        }else if("Multimedia".equals(vcs)){
          try{
    	                o = characteristicRecord;
    	                helper = new JSONObject();
    	                helper.put( "MultimediaAssetType", characteristicIdentifier );
    	                json = (JSONObject) o;
    	                children = json.getJSONArray( "_children" );
    	                for(int k=0; k<children.length(); k++) {
    	                  child = children.getJSONObject( k );
    	                  if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Status" )) {
    	                    helper.put(  "MultimediaAssetStatus",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getJSONObject( 0 ).getString( "_label" ));
    	                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_URL" ) || child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_URL2" )) {
    	                     helper.put( "MultimediaAssetURL",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
    	                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Name" ) || child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Name2" )) {
    	                     helper.put( "MultimediaAssetName",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
    	                   }
    	                }
    	                multiMedia.put( helper );
    	              }catch(org.json.JSONException e) {
    //	                log("ERROR: " + e.getMessage() + ". " + String.valueOf(child) + " ## " + characteristicIdentifier);
    	              }
        }else if(vcs.startsWith("Datos Logísticos")){
          o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
          logisticData.put(characteristicIdentifier, o instanceof JSONObject ? ((JSONObject)o).getString( "_label" ) : String.valueOf( o ));
        }else if(vcs.startsWith("Producto")){
          o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
          producto.put( characteristicIdentifier, o instanceof JSONObject ? ((JSONObject)o).getString( "_label" ) : String.valueOf( o ));
        }
      }else {
      }
    }
    if( response.getJSONObject("_data").has("productExtraData") ) {
            	  org.json.JSONArray peds = response.getJSONObject("_data").getJSONArray("productExtraData");
            	  org.json.JSONObject ped = null;
        		  for(int m = 0; m<peds.length(); m++) {
        			  ped = peds.getJSONObject(m);
    	        	  if(ped.has("sapObjectType")) {
    	        		  if("MX".equals( ped.getJSONObject("_qualification").getJSONObject("targetMarket").getString("_key") )) {
    		        		  header.put("SAPObjectType", sapObjectType = ped.getJSONObject("sapObjectType").getString("_label"));
    	        		  }
    	        	  }
    	        	  if(ped.has("supplierID")) {
    	        		  if("MX".equals( ped.getJSONObject("_qualification").getJSONObject("targetMarket").getString("_key") )) {
    		        		  supplierID = ped.getJSONObject("supplierID").getString("_code");
    	        		  }
    	        	  }
    	        	  if(ped.has("supplierPartNumber")) {
    	        		  if("MX".equals( ped.getJSONObject("_qualification").getJSONObject("targetMarket").getString("_key") )) {
    	        			  supplierPartNumber = ped.has("supplierPartNumber") ? ped.getString("supplierPartNumber") : "";
    	        		  }
    	        	  }
        		  }
    }
    if(!basicData.has("DescriptionLong")) {
    	        if(response.getJSONObject( "_data" ).has("lang")) {
    	       	  org.json.JSONArray lang = response.getJSONObject("_data" ).getJSONArray("lang");
    	       	  org.json.JSONObject innerObject = null;
    	       	  for(int index=0; index<lang.length(); index++) {
    	       		  innerObject = lang.getJSONObject(index);
    	       		  if(innerObject.has("descriptionLong") && "esl".equals(innerObject.getJSONObject("_qualification").getJSONObject("language").getString("_code"))) {
    	       			 descriptionLong = innerObject.getString("descriptionLong");
    	      			 attributes.put("DescriptionLong", descriptionLong);
    	      		  }
    	       		  if(innerObject.has("descriptionLong2") && "esl".equals(innerObject.getJSONObject("_qualification").getJSONObject("language").getString("_code"))) {
    	      			  descriptionLong2 = innerObject.getString("descriptionLong2");
    	      			  attributes.put("DescriptionLong2", descriptionLong2);
    	      		  }
    	      	  }
    	        }
    	        if(response.getJSONObject( "_data" ).has( "embedCodeWEB" )){
    	      	  embedCodeWEB = response.getJSONObject( "_data" ).getString( "embedCodeWEB" );
    	      	  attributes.put("EmbedCodeWEB", embedCodeWEB);
    	        }
    	        if(response.getJSONObject( "_data" ).has( "embedCodeWAP" )){
    	      	  embedCodeWAP = response.getJSONObject( "_data" ).getString( "embedCodeWAP" );
    	      	  attributes.put("EmbedCodeWAP", embedCodeWAP);
    	        } 
    	        if(response.getJSONObject( "_data" ).has( "refundPolicy" )){
    	      	  refundPolicy = response.getJSONObject( "_data" ).getString( "refundPolicy" );
    	      	  attributes.put("refundPolicy", refundPolicy);
    	        }
    	    }
    		if(ean == null)
    	          if(response.getJSONObject( "_data" ).has( "gtin" )){
    		      	  ean = response.getJSONObject( "_data" ).getString( "gtin" );
    		      	  header.put("MainBarCode", ean);
    		       }
    		if( sku == null || "".equals(sku) )
            	  if(response.getJSONObject( "_data" ).has( "sku" )){
    		      	  sku = String.valueOf( response.getJSONObject( "_data" ).get( "sku" ) );
    		      	  header.put("SKU", sku);
    		       }
    		if( sapObjectType == null ) {
            	  if(header.has("SAPObjectType")) {
            		  sapObjectType = header.getString("SAPObjectType");
            	  }
		    }
		    if("".equals(supplierPartNumber) || supplierPartNumber == null) {
            	  supplierPartNumber = supplierPartNumberChar;
		    }
		    if(!"".equals(supplierPartNumber)) {
    			  basicData.put("SupplierPartNumber", supplierPartNumber);
    		  }
    //	    log("Building response took: " + formatMillis(System.currentTimeMillis() - a));
    	    org.json.JSONObject modifiedFields = new org.json.JSONObject();
    	    org.json.JSONObject minietaaa = new org.json.JSONObject();
            if(ownersManual.length() > 0) {
    			minietaaa.put("OwnersManual", ownersManual);
    		}
            if(liverpoolManual.length() > 0) {
    			minietaaa.put("LiverpoolManual", liverpoolManual);
    		}
            if(productVideo.length() > 0) {
    			minietaaa.put("ProductVideo", productVideo);
    		}
            if(nom.length() > 0) {
    			minietaaa.put("NOM", nom);
    		}
            if(minietaaa.length() > 0) {
            	modifiedFields.put("multiMedia", minietaaa);
            }
            aggregateRejectionsByField(rechazos, losQueSi, modifiedFields);
    //        a = System.currentTimeMillis();
            java.util.LinkedList<String> variants = getVariants(headers, product);
    //        log("Getting variants took: " + formatMillis(System.currentTimeMillis() - a));
            org.json.JSONObject variantRejectionBoard = new org.json.JSONObject();
    //        a = System.currentTimeMillis();
            org.json.JSONArray productVariants = gatherVariants( variants, headers, losQueSi, variantRejectionBoard, supplierPartNumber );
            if(productVariants != null) {
    //	        log("Gathering current variant data took: " + formatMillis( System.currentTimeMillis() - a ));
    	        if("Artículo individual".equals(sapObjectType)) {
    	        	if(!header.has("SKU") && productVariants.length() > 0) {
    	        		if(productVariants.getJSONObject(0).has("SKU")) {
    	        			header.put("SKU", productVariants.getJSONObject(0).getString("SKU"));
    	        		}
    	        	}
    	        	if(!header.has("MainBarCode") && !header.has("MainBarCodeS4H") && ( productVariants.length() > 0 && ( "Liverpool".equals(business) || "Suburbia".equals(business) ) )) {
    	        		if(productVariants.getJSONObject(0).has("MainBarCode") || productVariants.getJSONObject(0).has("MainBarCodeS4H")) {
    		        		header.put( "MainBarCode" + ("Suburbia".equals(business) ? "S4H" : ""), 
    		        				productVariants.getJSONObject(0).has("MainBarCode") ? productVariants.getJSONObject(0).getString("MainBarCode") : productVariants.getJSONObject(0).has("MainBarCodeS4H")
    		        			);
    	        		}
    	        	}
    	        }
    	        if(variantRejectionBoard.length() > 0) {
    				modifiedFields.put("variants", variantRejectionBoard);
    			}
            }
            if(supplier == null || "".equals(supplier)) {
            	if(supplierID != null && !"".equals(supplierID)) {
            		supplier = appendZeros(supplierID, 20);
            	}
            }
            JSONObject jsonRes = new org.json.JSONObject().put("producto", producto).put("basicData", basicData).put("datosVenta", datosVenta).put("attributes", attributes).put("logisticData", logisticData).put("photos", photos).put("multiMedia", multiMedia).put("header", header).put( "variants", productVariants );
            jsonRes.put( "supplier", supplier);
            jsonRes.put( "template", structureGroupId );
            jsonRes.put( "templateName", structureGroupName);
            jsonRes.put( "Business", business );
            jsonRes.put("proposalId", product);
            jsonRes.put("modificationDate", modificationDate);
            jsonRes.put("enrichmentOriginForo", enriquecidoEnForo);
            jsonRes.put("currentStatus", response.getJSONObject("_data").has("currentStatus") ? response.getJSONObject("_data").getJSONObject("currentStatus").getString("_label") : "");
            jsonRes.put("previousStatus", response.getJSONObject("_data").has("previousStatus") ? response.getJSONObject("_data").getJSONObject("previousStatus").getString("_label") : "");
            jsonRes.put("externalStatus", response.getJSONObject("_data").has("externalStatus") ? response.getJSONObject("_data").getJSONObject("externalStatus").getString("_label") : "");
            if(comments.length() > 0) {
            	jsonRes.put("userRemarks", comments);
            }
            if(modifiedFields.length() > 0) {
            	jsonRes.put("modifiedFields", modifiedFields);
            }
            return jsonRes;
  }

  private String appendZeros(String value, int length) {
	  StringBuilder sb = new StringBuilder();
	  int dif = length - value.length();
	  for(int i=0; i<dif; i++) {
		  sb.append("0");
	  }
	  sb.append(value);
	  return sb.toString();
  }

  private void aggregateRejectionsByField(java.util.LinkedList<org.json.JSONObject> rejections, java.util.Map<String, String> vcs, org.json.JSONObject board) {
	  String currentName = null;
	  String vendorCenterSection = null;
	  org.json.JSONObject boardSection = null;
	  org.json.JSONArray currentSectionArray = null;
	  for(org.json.JSONObject rejection : rejections) {
		  currentName = rejection.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
		  currentName = currentName.replaceAll("_Rechazo", "");
		  if(currentName != null) {
			  vendorCenterSection = vcs.get(currentName);
			  if(vendorCenterSection != null) {
				  if(!board.has(vendorCenterSection)) {
					  boardSection = new org.json.JSONObject();
					  board.put(vendorCenterSection, boardSection);
				  }else {
					  boardSection = board.getJSONObject(vendorCenterSection);
				  }
				  if(!boardSection.has(currentName)) {
					  currentSectionArray = new org.json.JSONArray();
					  boardSection.put(currentName, currentSectionArray);
				  }else {
					  currentSectionArray = boardSection.getJSONArray(currentName);
				  }
				  collectRejectEntry(rejection, currentSectionArray);
			  }else {
				  aggregateRejectionsByFieldConNEN(rejection, board);
				  // PANIC
				  log("No vendor center section found when trying to identify for: " + currentName);
			  }
		  }else {
			  //PANIC
			  log("Not found a Name for: " + rejection);
		  }
	  }
  }

  private void aggregateRejectionsByFieldConNEN(org.json.JSONObject rejection, org.json.JSONObject board) {
	  String currentName = null;
	  org.json.JSONArray currentSectionArray = null;
	  currentName = rejection.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
	  currentName = currentName.replaceAll("_Rechazo", "");
	  if(!board.has(currentName)) {
		  currentSectionArray = new org.json.JSONArray();
		  board.put(currentName, currentSectionArray);
	  }else {
		  currentSectionArray = board.getJSONArray(currentName);
	  }
	  collectRejectEntry(rejection, currentSectionArray);
  }


  private void collectRejectEntry(org.json.JSONObject characteristic, org.json.JSONArray array) {
	  org.json.JSONObject childRejection = null;
	  String childRejectionClassName = null;
	  if(characteristic.has("_children")) {
		  org.json.JSONArray childRejections = characteristic.getJSONArray("_children");
		  org.json.JSONObject targetStructure = new org.json.JSONObject();
		  for(int j=0; j<childRejections.length(); j++) {
			  childRejection = childRejections.getJSONObject(j);
			  childRejectionClassName = childRejection.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			  if(childRejectionClassName.startsWith("rem_" )) {
				  targetStructure.put("status", childRejection.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"));
			  }else if(childRejectionClassName.startsWith("rmum_" )) {
				  targetStructure.put("date", childRejection.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
			  }else if(childRejectionClassName.startsWith("rrd_" )) {
				  targetStructure.put("targetRole", childRejection.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"));
			  }else if(childRejectionClassName.startsWith("rre_" )) {
				  targetStructure.put("submittingRole", childRejection.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"));
			  }else if(childRejectionClassName.startsWith("msj_" )) {
				  targetStructure.put("comment", childRejection.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
			  }else if(childRejectionClassName.startsWith("rma_" )) {
				  targetStructure.put("action", childRejection.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"));
			  } else {
//				  log("No valid prefix found for: " + childRejectionClassName + " (" + childRejection + ")");
			  }
		  }
		  if(targetStructure.length() > 0) {
			array.put(targetStructure);
		}
	  }
  }

  private void collectRejectionInformation(org.json.JSONObject multimediaElement, org.json.JSONObject destination) {
	  String assetName = null;
	  if(multimediaElement.has("_children")) {
		  org.json.JSONArray children = multimediaElement.getJSONArray("_children");
		  org.json.JSONObject child = null;
		  org.json.JSONArray childRejections = null;
		  org.json.JSONObject childRejection = null;
		  String childRejectionClassName = null;
		  org.json.JSONObject targetStructure = null;
		  org.json.JSONArray childRejectionTargetStructures = new org.json.JSONArray();
		  for(int i=0; i<children.length(); i++) {
			  child = children.getJSONObject(i);
			  if(child.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").endsWith("_Rejection")) {
				  if(child.has("_children")) {
					  childRejections = child.getJSONArray("_children");
					  targetStructure = new org.json.JSONObject();
					  for(int j=0; j<childRejections.length(); j++) {
						  childRejection = childRejections.getJSONObject(j);
						  childRejectionClassName = childRejection.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
						  if(childRejectionClassName.startsWith("rem_" )) {
							  targetStructure.put("status", childRejection.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"));
						  }else if(childRejectionClassName.startsWith("rmum_" )) {
							  targetStructure.put("date", childRejection.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
						  }else if(childRejectionClassName.startsWith("rrd_" )) {
							  targetStructure.put("targetRole", childRejection.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"));
						  }else if(childRejectionClassName.startsWith("rre_" )) {
							  targetStructure.put("submittingRole", childRejection.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"));
						  }else if(childRejectionClassName.startsWith("rma_" )) {
							  targetStructure.put("action", childRejection.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label"));
						  } else if(childRejectionClassName.endsWith("_AdditionalComment" )) {
							  targetStructure.put("comment", childRejection.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0));
						  }
//						  log("Look at me ese ---->" + childRejectionClassName);
					  }
					  if(targetStructure.length() > 0) {
						childRejectionTargetStructures.put(targetStructure);
					}
				  }
			  }else if(child.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").endsWith("_Name") ) {
				  assetName = child.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
			  }
		  }
		  if(childRejectionTargetStructures.length() > 0) {
			if(assetName != null) try{ destination.put(assetName, childRejectionTargetStructures); } catch (org.json.JSONException e) {}
		  }
	  }
  }

  private String getBusinessValueFromObject(org.json.JSONObject objectAPIResponse, String characteristic){
  	org.json.JSONObject entry = null;
  	org.json.JSONObject data = null;
  	org.json.JSONArray characteristicRecords = null;
  	try{
  		data = objectAPIResponse.getJSONObject("_data");
  		if(data.has("business") && data.getJSONObject("business").has("_label")) {
  			return data.getJSONObject("business").getString("_label");
  		}
  		if(data.has("_characteristicRecords")) {
	  		characteristicRecords = data.getJSONArray("_characteristicRecords");
	  		for(int i=0; i<characteristicRecords.length(); i++){
	  			entry = characteristicRecords.getJSONObject(i);
	  			if(characteristic.equals( entry.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code") )){
	  				return entry.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
	  			}
	  		}
  		}
  	}catch(org.json.JSONException e){ /* log("Problema. " + e.getMessage()); */ logE(e); }
  	return null;
  }

  private org.json.JSONArray gatherVariants(java.util.LinkedList<String> articles, java.util.Map<String, String> headers, java.util.Map<String, String> variantLevelAttributes, org.json.JSONObject variantsRejectionBoard, String supplierPartNumber){
    org.json.JSONArray variants = new org.json.JSONArray();
    try {
      JSONObject response = null;
      for(String article : articles) {
        org.json.JSONObject entity = batchVariantsById.get(article);
        if(entity == null || !entity.optBoolean("found", false)) {
          continue;
        }
        response = toObjectApiResponse(entity, false);
        if(!response.getJSONObject( "_data" ).has( "_characteristicRecords" )) {
        	return null;
        }
        org.json.JSONArray characteristicRecords = response.getJSONObject( "_data" ).getJSONArray( "_characteristicRecords" );
        JSONObject header = new org.json.JSONObject();
        JSONObject basicData = new org.json.JSONObject();
        JSONObject datosVenta = new org.json.JSONObject();
        JSONObject attributes = new org.json.JSONObject();
        JSONObject logisticData = new org.json.JSONObject();
        org.json.JSONArray photos = new org.json.JSONArray();
        org.json.JSONArray multiMedia = new org.json.JSONArray();
        JSONObject producto = new org.json.JSONObject();
        JSONObject json;
        Object o = null;
        String vcs = null;
        String characteristicIdentifier = null;
        org.json.JSONObject characteristicRecord = null;
        org.json.JSONArray children = null;
        JSONObject child = null;
        JSONObject helper = null;
        JSONObject jsonRes = new org.json.JSONObject();
        java.util.LinkedList<org.json.JSONObject> rechazos = new java.util.LinkedList<>();
        org.json.JSONObject productImage = new org.json.JSONObject();
        org.json.JSONObject productImageDetail = new org.json.JSONObject();
        org.json.JSONObject productImageSmosh = new org.json.JSONObject();
        org.json.JSONObject illustration = new org.json.JSONObject();
        org.json.JSONObject extraData = response.getJSONObject("_data").has("articleExtraData") ? response.getJSONObject("_data").getJSONArray("articleExtraData").length() > 0 ? response.getJSONObject("_data").getJSONArray("articleExtraData").getJSONObject(0) : null : null;
        String color = null;
        String tallaUnica = null;
        String supplierPartNumberVariant = null;
        String sapObjectType = null;
        String ean = null;
        String sku = null;
        ean = response.getJSONObject("_data").has("gtin") ? response.getJSONObject("_data").getString("gtin") : "";
        sku = response.getJSONObject("_data").has("sku") ? String.valueOf( response.getJSONObject("_data").get("sku") ) : "";
        if(extraData != null) {
      	  if("MX".equals( extraData.getJSONObject("_qualification").getJSONObject("targetMarket").getString("_code") )) {
      		  try { color = extraData.getJSONObject("coloursLiverpoolAtt").getString("_label"); }catch(org.json.JSONException e) {}
	      	  try { tallaUnica = extraData.getJSONObject("tamanoUnico").getString("_label"); }catch(org.json.JSONException e) {}
	      	  try { supplierPartNumberVariant = extraData.getString("supplierPartNumber"); }catch(org.json.JSONException e) {}
	      	  try { sapObjectType = extraData.getJSONObject("sapObjectType").getString("_label"); }catch(org.json.JSONException e) {}
      	  }
        }
//        log("Now classifying information found for variant: " + article);
        for(int j=0; j<characteristicRecords.length(); j++) {
          characteristicRecord = characteristicRecords.getJSONObject( j );
          characteristicIdentifier = characteristicRecord.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" );
          vcs = variantLevelAttributes.get( characteristicIdentifier );

          if(characteristicIdentifier.endsWith("_Rechazo")) {
          	rechazos.addLast(characteristicRecord);
          	continue;
          } else if(characteristicIdentifier.equals("ProductImage")) {
          	collectRejectionInformation(characteristicRecord, productImage);
          } else if(characteristicIdentifier.equals("ProductImageDetail")) {
          	collectRejectionInformation(characteristicRecord, productImageDetail);
          } else if(characteristicIdentifier.equals("ProductImageSmosh")) {
          	collectRejectionInformation(characteristicRecord, productImageSmosh);
          } else if(characteristicIdentifier.equals("Illustration")) {
           	collectRejectionInformation(characteristicRecord, illustration);
          }
          if(vcs != null) {
            if("Datos de Venta".equals(vcs)){
              o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
              datosVenta.put( characteristicIdentifier, o instanceof JSONObject ? ((JSONObject)o).getString( "_label" ) : String.valueOf( o ));
            }else if("Datos Logísticos".equals(vcs)){
                o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
                datosVenta.put( characteristicIdentifier, o instanceof JSONObject ? ((JSONObject)o).getString( "_label" ) : String.valueOf( o ));
            }else
            if("Fotografías".equals(vcs)){
              try{
                o = characteristicRecord;
                helper = new JSONObject();
                helper.put( "PhotoAssetType", characteristicIdentifier );
                json = (JSONObject) o;
                children = json.getJSONArray( "_children" );
                for(int k=0; k<children.length(); k++) {
                  child = children.getJSONObject( k );
                  if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Status" )) {
                    helper.put( "PhotoAssetStatus",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getJSONObject( 0 ).getString( "_label" ));
                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_URL" ) ) {
                     helper.put( "PhotoAssetURL",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Name" ) ) {
                     helper.put( "PhotoAssetName",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
                   }
                }
                photos.put( helper );
              }catch(org.json.JSONException e) {
//                log("ERROR: " + e.getMessage() + ". " + String.valueOf(child));
              }
            }else if("Multimedia".equals(vcs)){
              try{
                o = characteristicRecord;
                helper = new JSONObject();
                helper.put( "MultimediaAssetType", characteristicIdentifier );
                json = (JSONObject) o;
                children = json.getJSONArray( "_children" );
                for(int k=0; k<children.length(); k++) {
                  child = children.getJSONObject( k );
                  if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Status" )) {
                    helper.put( "MultimediaAssetStatus",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getJSONObject( 0 ).getString( "_label" ));
                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_URL" ) ) {
                     helper.put( "MultimediaAssetURL",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Name" ) ) {
                     helper.put( "MultimediaAssetName",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
                   }
                }
                multiMedia.put( helper );
              }catch(org.json.JSONException e) {
//                log("ERROR: " + e.getMessage() + ". " + String.valueOf(child));
              }
            }
            else{
              o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
              jsonRes.put( characteristicIdentifier, o instanceof JSONObject ? ((JSONObject)o).getString( "_label" ) : String.valueOf( o ));
            }
          }
        }

        org.json.JSONObject modifiedFields = new org.json.JSONObject();
//        org.json.JSONObject photosModifFields = new org.json.JSONObject();
        if(productImage.length() > 0) {
//        	photosModifFields.put("ProductImage", productImage);
        	modifiedFields.put("ProductImage", productImage);
		}
        if(productImageDetail.length() > 0) {
//        	photosModifFields.put("ProductImageDetail", productImageDetail);
        	modifiedFields.put("ProductImageDetail", productImageDetail);
		}
        if(productImageSmosh.length() > 0) {
//        	photosModifFields.put("ProductImageSmosh", productImageSmosh);
        	modifiedFields.put("ProductImageSmosh", productImageSmosh);
		}
        if(illustration.length() > 0) {
//        	photosModifFields.put("Illustration", illustration);
        	modifiedFields.put("Illustration", illustration);
		}
//        if(photosModifFields.length() > 0) {
//        	modifiedFields.put("photos", photosModifFields);
//        }
        aggregateRejectionsByField(rechazos, variantLevelAttributes, modifiedFields);
        if(modifiedFields.length() > 0) {
        	variantsRejectionBoard.put(article, modifiedFields);
        }

        if(!jsonRes.has("TamanoUnico"))
        	jsonRes.put("TamanoUnico", tallaUnica == null ? "" : tallaUnica);
        if(!jsonRes.has("ColoursLiverpoolAtt"))
        	jsonRes.put("ColoursLiverpoolAtt", color == null ? "" : color);
        if(!jsonRes.has("SupplierPartNumber"))
        	jsonRes.put("SupplierPartNumber", supplierPartNumberVariant == null ? supplierPartNumber : supplierPartNumberVariant);
        if(!jsonRes.has("SAPObjectType"))
        	jsonRes.put("SAPObjectType", sapObjectType == null ? "" : sapObjectType);
        if(!jsonRes.has("MainBarCode") && !jsonRes.has("MainBarCodeS4H") && !"".equals(ean)) {
        	jsonRes.put("MainBarCode", ean);
        }
        if(!jsonRes.has("SKU") && !"".equals(sku)) {
        	jsonRes.put("SKU", sku);
        }
        if(producto.length() > 0)
        {
          jsonRes .put("producto", producto);
        }
        if(basicData.length() > 0)
        {
          jsonRes.put("basicData", basicData);
        }
        if(datosVenta.length() > 0)
        {
          jsonRes.put("datosVenta", datosVenta);
        }
        if(attributes.length() > 0)
        {
          jsonRes.put("attributes", attributes);
        }
        if(logisticData.length() > 0)
        {
          jsonRes.put("logisticData", logisticData);
        }
        if(photos.length() > 0)
        {
          jsonRes.put("photos", photos);
        }
        if(multiMedia.length() > 0)
        {
          jsonRes.put("multiMedia", multiMedia);
        }
        if(header.length() > 0)
        {
          jsonRes.put("header", header);
        }
        jsonRes.put( "variantId", article );
        String modificationDate = null;
        try{
          	modificationDate = response.getJSONObject("_data").getJSONArray("ownLog").getJSONObject(0).getString("modificationDate");
         }catch(org.json.JSONException ignore){}
        jsonRes.put("modificationDate", modificationDate);
        jsonRes.put("currentStatus",  response.getJSONObject("_data").has("currentStatus") ? response.getJSONObject("_data").getJSONObject("currentStatus").getString("_label") : "");
        jsonRes.put("previousStatus", response.getJSONObject("_data").has("previousStatus") ? response.getJSONObject("_data").getJSONObject("previousStatus").getString("_label") : "");
        jsonRes.put("externalStatus", response.getJSONObject("_data").has("externalStatus") ? response.getJSONObject("_data").getJSONObject("externalStatus").getString("_label") : "");
        variants.put( jsonRes );
      }
    }catch(Exception e) {
      logE(e);
    }
    return variants;
  }


  private java.util.LinkedList<String> getVariants(java.util.Map<String, String> header, String productId) {
    java.util.LinkedList<String> variants = new java.util.LinkedList<>();
    java.util.Set<String> values = batchVariantsByProduct.get(productId);
    if (values != null) {
      variants.addAll(values);
    }
    return variants;
  }

  private java.util.Map<String, String> gatherFieldsToSendByBusiness(
      java.util.Map<String, String> header,
      String template,
      String business) {

    String cacheKey = String.valueOf(template) + "\u0000" + String.valueOf(business);
    java.util.Map<String, String> cached = atributosValidosPorPlantilla.get(cacheKey);
    if (cached != null) {
      return new java.util.TreeMap<>(cached);
    }

    java.util.Map<String, String> result = new java.util.TreeMap<>();

    /*
     * Keep the exact metadata source used by the legacy implementation.
     * DataRequestor#getTemplateCharacteristicMetaDataByTemplate delegated to
     * getTemplateCharacteristicPropertyValue(template, creationType). That
     * DBAccessDataStub method intentionally does not apply Res_Int_04
     * (CreationType) in this overload, so switching to
     * getTemplateCharacteristicProperties(...) changes the effective set of
     * Vendor Center attributes.
     */
    org.json.JSONArray metadataItems =
        dastub.getTemplateCharacteristicPropertyValue(template, CREATION_TYPE);
    org.json.JSONObject metadata =
        metadataItems.length() > 0 ? metadataItems.optJSONObject(0) : null;

    if (metadata != null) {
      for (Object characteristic : metadata.keySet()) {
    	  if(characteristic instanceof String) {
	        org.json.JSONObject properties = metadata.optJSONObject((String)characteristic);
	        if (properties == null) {
	          continue;
	        }
	        String sent = propertyIgnoreCase(properties, "senttoVendorCenter");
	        String allowedBusiness = propertyIgnoreCase(properties, "allowedBusiness");
	        String section = propertyIgnoreCase(properties, "vendorCenterSection");
	        if ("1".equals(sent)
	            && allowedBusiness != null
	            && allowedBusiness.contains(business)
	            && section != null
	            && !section.isEmpty()) {
	          result.put((String)characteristic, section);
	        }
    	  }
      }
    }

    addGlobalData(business, result, null, null);
    atributosValidosPorPlantilla.put(cacheKey, new java.util.TreeMap<>(result));
    return result;
  }

  private void addGlobalData(
      String negocio,
      java.util.Map<String, String> attributeVendorCenterSection,
      String baseUrl,
      String authorization) {

    if (globalMetadataCache == null) {
      globalMetadataCache = dastub.getGlobalMetadata(CREATION_TYPE);
    }
    org.json.JSONObject metadata = globalMetadataCache;
    if (metadata == null) {
      return;
    }

    for (Object name : metadata.keySet()) {
    	if(name instanceof String) {
	      org.json.JSONObject properties = metadata.optJSONObject((String)name);
	      if (properties == null) {
	        continue;
	      }
	      String sent = propertyIgnoreCase(properties, "senttoVendorCenter");
	      String allowedBusiness = propertyIgnoreCase(properties, "allowedBusiness");
	      String section = propertyIgnoreCase(properties, "vendorCenterSection");
	      if ("1".equals(sent)
	          && allowedBusiness != null
	          && allowedBusiness.contains(negocio)
	          && section != null
	          && !section.isEmpty()) {
	        try{ attributeVendorCenterSection.put((String)name, section); } catch(NullPointerException e) { log("Name: " + name); log("Metadata: " + metadata); logE(e); }
	      }
    	}
    }
  }

  private String propertyIgnoreCase(org.json.JSONObject object, String expected) {
    if (object == null || expected == null) {
      return null;
    }
    if (object.has(expected)) {
      Object value = object.opt(expected);
      return value == null || value == org.json.JSONObject.NULL ? null : String.valueOf(value);
    }
    for (Object key : object.keySet()) {
    	if(key instanceof String) {
	      if (expected.equalsIgnoreCase((String)key)) {
	        Object value = object.opt((String)key);
	        return value == null || value == org.json.JSONObject.NULL ? null : String.valueOf(value);
	      }
    	}
    }
    return null;
  }

  private java.util.Set<String> collectMultiValueCharacteristics(
      java.util.Map<String, org.json.JSONObject> products,
      java.util.Map<String, org.json.JSONObject> variants) {

    java.util.Set<String> result = new java.util.TreeSet<>();
    collectMultiValueCharacteristics(products, result);
    collectMultiValueCharacteristics(variants, result);
    return result;
  }

  private void collectMultiValueCharacteristics(
      java.util.Map<String, org.json.JSONObject> entities,
      java.util.Set<String> destination) {

    for (org.json.JSONObject entity : entities.values()) {
      org.json.JSONArray characteristics = entity == null ? null : entity.optJSONArray("characteristics");
      if (characteristics == null) {
        continue;
      }
      for (int i = 0; i < characteristics.length(); i++) {
        org.json.JSONObject characteristic = characteristics.optJSONObject(i);
        if (characteristic == null || !isMultiValue(characteristic)) {
          continue;
        }
        String identifier = characteristic.optString("Identifier", "");
        if (!identifier.isEmpty()) {
          destination.add(identifier);
        }
      }
    }
  }

  private boolean isMultiValue(org.json.JSONObject characteristic) {
    Object value = characteristic == null ? null : characteristic.opt("IsMultiValue");
    if (value instanceof Boolean) {
      return ((Boolean) value).booleanValue();
    }
    if (value instanceof Number) {
      return ((Number) value).intValue() != 0;
    }
    if (value != null && value != org.json.JSONObject.NULL) {
      String text = String.valueOf(value).trim();
      return "1".equals(text) || "true".equalsIgnoreCase(text);
    }
    return false;
  }

  /**
   * Builds only the Object API subset consumed by the legacy response builder.
   * No HTTP call is made here.
   */
  private org.json.JSONObject toObjectApiResponse(org.json.JSONObject entity, boolean product) {
    org.json.JSONObject response = new org.json.JSONObject();
    org.json.JSONObject data = new org.json.JSONObject();
    response.put("_data", data);

    if (entity == null) {
      return response;
    }

    org.json.JSONObject detail = entity.optJSONObject("detail");
    org.json.JSONObject detailLookups = entity.optJSONObject("detailLookups");
    if (detail == null) {
      detail = new org.json.JSONObject();
    }
    if (detailLookups == null) {
      detailLookups = new org.json.JSONObject();
    }

    if (detail.has("EAN")) {
      data.put("gtin", detail.optString("EAN", ""));
    }
    if (detail.has("Res_Int_02")) {
      data.put("sku", detail.opt("Res_Int_02"));
    }

    org.json.JSONObject revision = entity.optJSONObject("revision");
    /*
     * The legacy Product2G Object API call had no entityFilter and exposed
     * ownLog. The Article call did use an entityFilter and, in practice, did
     * not expose ownLog; preserve that contract instead of inventing a new
     * variant-level modificationDate.
     */
    if (product && revision != null && revision.has("ModificationTimestamp")) {
      data.put(
          "ownLog",
          new org.json.JSONArray().put(
              new org.json.JSONObject().put(
                  "modificationDate",
                  normalizeObjectApiTimestamp(
                      revision.optString("ModificationTimestamp", "")))));
    }

    putStatus(data, "currentStatus", detail.opt("CurrentStatus"));
    putStatus(data, "previousStatus", detail.opt("Res_Int_03"));
    putLookupField(data, "externalStatus", detailLookups.optJSONObject("Res_Int_04"));
    putLookupField(data, "business", detailLookups.optJSONObject("Res_Int_01"));

    if (product) {
      if (detail.has("Res_Text2G_02")) {
        data.put("embedCodeWEB", detail.optString("Res_Text2G_02", ""));
      }
      if (detail.has("Res_Text2G_03")) {
        data.put("embedCodeWAP", detail.optString("Res_Text2G_03", ""));
      }
      if (detail.has("Res_Text2G_04")) {
        data.put("refundPolicy", detail.optString("Res_Text2G_04", ""));
      }
    }

    data.put(
        "_characteristicRecords",
        buildCharacteristicRecords(
            entity,
            product ? PRODUCT_CHARACTERISTIC_ENTITY_ID : ARTICLE_CHARACTERISTIC_ENTITY_ID,
            product ? PRODUCT_CHARACTERISTIC_LANG_ENTITY_ID : ARTICLE_CHARACTERISTIC_LANG_ENTITY_ID));

    appendLanguages(data, entity);
    if (product) {
      appendProductExtraData(data, entity);
    } else {
      appendArticleExtraData(data, entity);
    }

    return response;
  }

  private void appendLanguages(org.json.JSONObject data, org.json.JSONObject entity) {
    org.json.JSONArray source = entity.optJSONArray("languages");
    if (source == null || source.length() == 0) {
      return;
    }
    org.json.JSONArray result = new org.json.JSONArray();
    for (int i = 0; i < source.length(); i++) {
      org.json.JSONObject language = source.optJSONObject(i);
      if (language == null) {
        continue;
      }
      int languageID = language.optInt("LanguageID", Integer.MIN_VALUE);
      org.json.JSONObject row = new org.json.JSONObject();
      row.put("_qualification", new org.json.JSONObject()
          .put("language", new org.json.JSONObject()
              .put("_code", languageID == 10 ? "esl" : String.valueOf(languageID))
              .put("_key", languageID)));
      if (language.has("DescriptionLong")) {
        row.put("descriptionLong", language.optString("DescriptionLong", ""));
      }
      if (language.has("Res_Text2G_01")) {
        row.put("descriptionLong2", language.optString("Res_Text2G_01", ""));
      }
      if (language.has("DescriptionShort")) {
        row.put("descriptionShort", language.optString("DescriptionShort", ""));
      }
      if (language.has("Res_Text250_01")) {
        row.put("productName", language.optString("Res_Text250_01", ""));
      }
      result.put(row);
    }
    if (result.length() > 0) {
      data.put("lang", result);
    }
  }

  private void appendProductExtraData(org.json.JSONObject data, org.json.JSONObject entity) {
    org.json.JSONObject domain = domainRow(entity, PRODUCT_DOMAIN_ENTITY_ID);
    if (domain == null) {
      return;
    }
    org.json.JSONObject extra = new org.json.JSONObject();
    extra.put("_qualification", new org.json.JSONObject()
        .put("targetMarket", new org.json.JSONObject().put("_key", "MX").put("_code", "MX")));

    org.json.JSONObject lookups = domain.optJSONObject("lookups");
    if (lookups != null) {
      putApiLookup(extra, "sapObjectType", lookups.optJSONObject("Res_Int_08"));
      putApiLookup(extra, "supplierID", lookups.optJSONObject("Std_Int_10"));
    }
    if (domain.has("Res_Text250_01")) {
      extra.put("supplierPartNumber", domain.optString("Res_Text250_01", ""));
    }

    data.put("productExtraData", new org.json.JSONArray().put(extra));
  }

  private void appendArticleExtraData(org.json.JSONObject data, org.json.JSONObject entity) {
    org.json.JSONObject domain = domainRow(entity, ARTICLE_DOMAIN_ENTITY_ID);
    if (domain == null) {
      return;
    }
    org.json.JSONObject extra = new org.json.JSONObject();
    extra.put("_qualification", new org.json.JSONObject()
        .put("targetMarket", new org.json.JSONObject().put("_key", "MX").put("_code", "MX")));

    org.json.JSONObject lookups = domain.optJSONObject("lookups");
    if (lookups != null) {
      putApiLookup(extra, "tamanoUnico", lookups.optJSONObject("Res_Int_01"));
      putApiLookup(extra, "coloursLiverpoolAtt", lookups.optJSONObject("Res_Int_02"));
      putApiLookup(extra, "sapObjectType", lookups.optJSONObject("Res_Int_03"));
    }
    if (domain.has("Res_Text250_01")) {
      extra.put("supplierPartNumber", domain.optString("Res_Text250_01", ""));
    }

    data.put("articleExtraData", new org.json.JSONArray().put(extra));
  }

  private void putApiLookup(
      org.json.JSONObject destination,
      String property,
      org.json.JSONObject lookup) {
    org.json.JSONObject apiLookup = apiLookup(lookup);
    if (apiLookup != null) {
      destination.put(property, apiLookup);
    }
  }

  private void putLookupField(
      org.json.JSONObject destination,
      String property,
      org.json.JSONObject lookup) {
    org.json.JSONObject apiLookup = apiLookup(lookup);
    if (apiLookup != null) {
      destination.put(property, apiLookup);
    }
  }

  private org.json.JSONObject apiLookup(org.json.JSONObject lookup) {
    if (lookup == null) {
      return null;
    }
    String code = lookup.optString("Code", "");
    String name = lookup.optString("Name", "");
    if (code.isEmpty() && name.isEmpty()) {
      return null;
    }
    org.json.JSONObject result = new org.json.JSONObject();
    if (!code.isEmpty()) {
      result.put("_code", code);
    }
    result.put("_label", name.isEmpty() ? code : name);
    return result;
  }

  private void putStatus(org.json.JSONObject data, String property, Object rawStatus) {
    String label = statusLabel(rawStatus);
    if (!label.isEmpty()) {
      data.put(property, new org.json.JSONObject().put("_label", label));
    }
  }

  private String statusLabel(Object rawStatus) {
    if (rawStatus == null || rawStatus == org.json.JSONObject.NULL) {
      return "";
    }
    int status;
    if (rawStatus instanceof Number) {
      status = ((Number) rawStatus).intValue();
    } else {
      try {
        status = Integer.parseInt(String.valueOf(rawStatus));
      } catch (NumberFormatException e) {
        return String.valueOf(rawStatus);
      }
    }
    switch (status) {
      case 1001: return "Propuesta Generada";
      case 1002: return "Pendiente Inicio Enriquecimiento";
      case 1003: return "Revisión Compras";
      case 1004: return "Carga de Imagen";
      case 1005: return "Rechazada";
      case 1006: return "Por Actualizar";
      case 1007: return "Aprobada";
      case 1008: return "Modificación";
      case 1009: return "Cancelado";
      case 1010: return "En Proceso Liverpool";
      case 1011: return "En Proceso de Envío";
      case 1020: return "Creación de SKU";
      case 1021: return "Gobierno de Datos";
      case 1022: return "Revisión QA";
      case 1023: return "Category";
      case 1024: return "Rechazo Publicación";
      case 1025: return "Eliminada";
      case 1026: return "En Proceso Foro";
      case 1027: return "Rechazo Compras";
      case 1028: return "Rechazo QA";
      case 1029: return "Rechazo Gobierno";
      case 1030: return "Rechazo Category";
      case 1031: return "Repoblamiento";
      case 1032: return "Excepción de Catalogación";
      case 10031: return "Borrador";
      default: return String.valueOf(status);
    }
  }

  private org.json.JSONObject domainRow(org.json.JSONObject entity, int entityID) {
    org.json.JSONArray domains = entity == null ? null : entity.optJSONArray("domains");
    if (domains == null) {
      return null;
    }
    for (int i = 0; i < domains.length(); i++) {
      org.json.JSONObject row = domains.optJSONObject(i);
      if (row != null && row.optInt("EntityID", Integer.MIN_VALUE) == entityID) {
        return row;
      }
    }
    return null;
  }

  private String structureGroup(org.json.JSONObject entity, String structureIdentifier) {
    org.json.JSONArray structures = entity == null ? null : entity.optJSONArray("structures");
    if (structures == null) {
      return "";
    }
    for (int i = 0; i < structures.length(); i++) {
      org.json.JSONObject row = structures.optJSONObject(i);
      if (row != null && structureIdentifier.equals(row.optString("StructureIdentifier", ""))) {
        return row.optString("StructureGroupIdentifier", "");
      }
    }
    return "";
  }

  private static final class CharacteristicNode {
    final org.json.JSONObject source;
    final org.json.JSONObject api;
    final java.util.List<CharacteristicNode> children = new java.util.ArrayList<>();

    CharacteristicNode(org.json.JSONObject source, org.json.JSONObject api) {
      this.source = source;
      this.api = api;
    }
  }

  private org.json.JSONArray buildCharacteristicRecords(
      org.json.JSONObject entity,
      int characteristicEntityID,
      int languageEntityID) {

    org.json.JSONArray source = entity.optJSONArray("characteristics");
    org.json.JSONArray result = new org.json.JSONArray();
    if (source == null) {
      return result;
    }

    java.util.List<CharacteristicNode> nodes = new java.util.ArrayList<>();
    java.util.Map<String, CharacteristicNode> parentIndex = new java.util.LinkedHashMap<>();

    for (int i = 0; i < source.length(); i++) {
      org.json.JSONObject characteristic = source.optJSONObject(i);
      if (characteristic == null
          || characteristic.optInt("EntityID", Integer.MIN_VALUE) != characteristicEntityID) {
        continue;
      }
      CharacteristicNode node = new CharacteristicNode(
          characteristic,
          toApiCharacteristic(characteristic, languageEntityID));
      nodes.add(node);

      String recordKey = characteristic.optString("RecordKey", "");
      Object characteristicID = characteristic.opt("CharacteristicID");
      if (!recordKey.isEmpty() && characteristicID != null) {
        parentIndex.put(parentIndexKey(characteristicID, recordKey), node);
      }
    }

    java.util.List<CharacteristicNode> roots = new java.util.ArrayList<>();
    for (CharacteristicNode node : nodes) {
      String parentRecordKey = node.source.optString("ParentRecordKey", "");
      Object parentCharacteristicID = node.source.opt("ParentCharacteristicID");
      CharacteristicNode parent = null;
      if (!parentRecordKey.isEmpty()
          && parentCharacteristicID != null
          && parentCharacteristicID != org.json.JSONObject.NULL) {
        parent = parentIndex.get(parentIndexKey(parentCharacteristicID, parentRecordKey));
      }
      if (parent == null) {
        roots.add(node);
      } else {
        parent.children.add(node);
      }
    }

    for (CharacteristicNode node : nodes) {
      if (!node.children.isEmpty()) {
        org.json.JSONArray children = new org.json.JSONArray();
        for (CharacteristicNode child : node.children) {
          children.put(child.api);
        }
        node.api.put("_children", children);
      }
    }

    java.util.Map<String, org.json.JSONObject> groupedMultiValues = new java.util.LinkedHashMap<>();
    for (CharacteristicNode root : roots) {
      String identifier = root.source.optString("Identifier", "");
      if (root.children.isEmpty() && isMultiValue(root.source) && !identifier.isEmpty()) {
        org.json.JSONObject existing = groupedMultiValues.get(identifier);
        if (existing == null) {
          groupedMultiValues.put(identifier, root.api);
          result.put(root.api);
        } else {
          org.json.JSONArray destinationValues = existing
              .getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values");
          org.json.JSONArray sourceValues = root.api
              .getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values");
          for (int i = 0; i < sourceValues.length(); i++) {
            destinationValues.put(sourceValues.get(i));
          }
        }
      } else {
        result.put(root.api);
      }
    }

    return result;
  }

  private String parentIndexKey(Object characteristicID, String recordKey) {
    return String.valueOf(characteristicID) + "\u0000" + recordKey;
  }

  private org.json.JSONObject toApiCharacteristic(
      org.json.JSONObject characteristic,
      int languageEntityID) {

    String identifier = characteristic.optString("Identifier", "");
    org.json.JSONObject result = new org.json.JSONObject()
        .put("_qualification", new org.json.JSONObject()
            .put("characteristic", new org.json.JSONObject().put("_code", identifier)));

    org.json.JSONArray values = new org.json.JSONArray();
    Object value = characteristicValue(characteristic, languageEntityID);
    if (value != null) {
      values.put(value);
    }
    result.put("_recordLang", new org.json.JSONArray().put(
        new org.json.JSONObject().put("values", values)));
    return result;
  }

  private Object characteristicValue(org.json.JSONObject characteristic, int languageEntityID) {
    org.json.JSONObject languageValue = preferredLanguageValue(characteristic, languageEntityID);
    org.json.JSONObject lookup = null;
    boolean hasTextValue = false;
    String textValue = null;

    if (languageValue != null) {
      lookup = languageValue.optJSONObject("lookup");
      if (languageValue.has("Value")) {
        hasTextValue = true;
        textValue = languageValue.optString("Value", "");
      }
    }
    if (lookup == null) {
      lookup = characteristic.optJSONObject("lookup");
    }
    if (!hasTextValue && characteristic.has("Value")) {
      hasTextValue = true;
      textValue = characteristic.optString("Value", "");
    }

    org.json.JSONObject apiLookup = apiLookup(lookup);
    if (apiLookup != null) {
      return apiLookup;
    }
    if (!hasTextValue) {
      return null;
    }

    String dataType = characteristic.optString("DataType", "");
    if ("BOOLEAN".equalsIgnoreCase(dataType)
        || "BOOL".equalsIgnoreCase(dataType)
        || "true".equalsIgnoreCase(textValue)
        || "false".equalsIgnoreCase(textValue)) {
      if ("true".equalsIgnoreCase(textValue) || "false".equalsIgnoreCase(textValue)) {
        return Boolean.valueOf(textValue);
      }
    }
    return normalizeObjectApiTimestamp(textValue);
  }

  /*
   * P360's Object API serializes date-like characteristic values as ISO-8601
   * strings. Direct ACVL reads can expose values such as
   * "2026-08-04 16:00:40.792 300"; keep ordinary strings untouched and only
   * normalize the timestamp-shaped representation.
   */
  private String normalizeObjectApiTimestamp(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2})[ T](\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?)(?:\\s+\\d+)?(?:Z)?$")
        .matcher(trimmed);
    if (!matcher.matches()) {
      return value;
    }
    return matcher.group(1) + "T" + matcher.group(2) + "Z";
  }

  private org.json.JSONObject preferredLanguageValue(
      org.json.JSONObject characteristic,
      int languageEntityID) {

    org.json.JSONArray values = characteristic.optJSONArray("languageValues");
    if (values == null) {
      return null;
    }
    org.json.JSONObject language10 = null;
    org.json.JSONObject any = null;
    for (int i = 0; i < values.length(); i++) {
      org.json.JSONObject value = values.optJSONObject(i);
      if (value == null || value.optInt("EntityID", Integer.MIN_VALUE) != languageEntityID) {
        continue;
      }
      if (any == null) {
        any = value;
      }
      int languageID = value.optInt("LanguageID", Integer.MIN_VALUE);
      if (languageID == -1) {
        return value;
      }
      if (languageID == 10 && language10 == null) {
        language10 = value;
      }
    }
    return language10 != null ? language10 : any;
  }

	private static final java.util.logging.Logger LOGGER =
	        java.util.logging.Logger.getLogger(GetProposals.class.getName());

	private static final java.time.format.DateTimeFormatter LOG_TIMESTAMP =
	        java.time.format.DateTimeFormatter
	                .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
	                .withZone(java.time.ZoneId.systemDefault());

	private static final class FlushingFileHandler extends java.util.logging.FileHandler {

	    private FlushingFileHandler(String pattern, boolean append) throws java.io.IOException {
	        super(pattern, append);
	    }

	    @Override
	    public synchronized void publish(java.util.logging.LogRecord record) {
	        if (!isLoggable(record)) {
	            return;
	        }

	        super.publish(record);

	        /*
	         * FileHandler normalmente mantiene abierto el archivo.
	         * Este flush asegura que el registro se entregue al sistema
	         * operativo antes de regresar al hilo solicitante.
	         */
	        flush();
	    }
	}

	static {
	    try {
	        LOGGER.setUseParentHandlers(false);
	        LOGGER.setLevel(java.util.logging.Level.ALL);

	        /*
	         * Evita duplicados si por alguna razón ya hubiera handlers
	         * asociados al mismo nombre de logger.
	         */
	        for (java.util.logging.Handler handler : LOGGER.getHandlers()) {
	            LOGGER.removeHandler(handler);
	            handler.close();
	        }

	        FlushingFileHandler fileHandler = new FlushingFileHandler(
	                "../logs/java_process_proposal_request.log",
	                true
	        );

	        fileHandler.setEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
	        fileHandler.setLevel(java.util.logging.Level.ALL);

	        fileHandler.setFormatter(new java.util.logging.Formatter() {
	            @Override
	            public String format(java.util.logging.LogRecord record) {
	                if (record.getThrown() != null) {
	                    java.io.StringWriter sw = new java.io.StringWriter();
	                    try (java.io.PrintWriter pw = new java.io.PrintWriter(sw)) {
	                        record.getThrown().printStackTrace(pw);
	                    }
	                    return sw.toString();
	                }

	                String timestamp = LOG_TIMESTAMP.format(
	                        java.time.Instant.ofEpochMilli(record.getMillis())
	                );

	                return "["
	                        + timestamp
	                        + "] "
	                        + record.getMessage()
	                        + System.lineSeparator();
	            }
	        });

	        LOGGER.addHandler(fileHandler);
	    } catch (java.io.IOException | SecurityException e) {
	        throw new ExceptionInInitializerError(e);
	    }
	}

	private void log(String message) {
	    LOGGER.log(
	            java.util.logging.Level.INFO,
	            "(" + myId + ") " + String.valueOf(message)
	    );
	}

	private void logE(Exception ex) {
	    java.util.logging.LogRecord record =
	            new java.util.logging.LogRecord(java.util.logging.Level.SEVERE, "");

	    record.setLoggerName(LOGGER.getName());
	    record.setThrown(ex);

	    LOGGER.log(record);
	}

	@Override
	public void close() throws IOException {
		dastub.close();
	}
	
	
}
