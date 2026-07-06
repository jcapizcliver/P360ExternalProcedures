package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class GetProposals{

  private final RESTWorkshop workshop;
  private final RestClient rc; // = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + "cmVzdDpoZWlsZXI=");
  private final java.util.Map<String, java.util.Map<String, String>> atributosValidosPorPlantilla = new java.util.TreeMap<>();
  private final long myId;

  private final String baseURL; // = "http://172.18.237.162:1512/rest/V2.0";

  public GetProposals(String baseUrl, String encoded, long myId) {
	  this.baseURL = baseUrl;
	  this.workshop = new RESTWorkshop();
	  this.myId = myId;
	  if(this.baseURL != null) {
		  workshop.setBaseUrl(baseUrl);
	  }
	  rc = workshop.getRc();
	  if(encoded != null)
		  rc.getHeader().put("Authorization", "Basic: " + encoded);
  }
  
  public static void main(String[] args){
  }

  public String run(String[] args) {
	  return processFile(args[0]);
  }

  private String formatMillis(long millis){
  	int days = (int)(millis/(1000*60*60*24));
 	millis -= days*1000*60*60*24;
  	int hours = (int) (millis/(1000*60*60));
  	millis -= hours*1000*60*60;
  	int minutes = (int) (millis/(1000*60));
  	millis -= minutes*1000*60;
  	int seconds = (int) (millis/1000);
  	millis -= seconds*1000;
  	return
  		    (days < 10 ? "0" : "") + days + ":"
  		+ (hours < 10 ? "0" : "") + hours + ":"
  		+ (minutes < 10 ? "0" : "") + minutes + ":"
  		+ (seconds < 10 ? "0" : "") + seconds
  		+ "." + millis;
  }
  
  private String processFile(String input){
	long init = System.currentTimeMillis();
    java.util.Map<String, String> headers = new java.util.HashMap<>();
    headers.put( "Content-Type", "application/json" );
    headers.put( "Accept", "application/json" );
    headers.put( "Authorization", this.rc.getHeader().get("Authorization") );
    headers.put( "Accept-Language", "es");

    String business;

    String rawResponse = null;
    JSONObject response = null;
    JSONObject json = null;
    org.json.JSONArray rows = null;
    String sku = null;
    String productId = null;
    String product = null;
    String structureGroupId = null;
    String structureGroupName = null;
    org.json.JSONArray responses = new org.json.JSONArray();
    java.util.Map<String, String> losQueSi = null;
	String output = null;
    try {
    	log("An input: " + input);
      response = new JSONObject(input);
      rows = response.getJSONArray( "products" );
//      log("Request of: " + rows.length() + " elements");
      for(int i=0; i<rows.length(); i++) {
        json = rows.getJSONObject(i);
        sku = json.has("sku") ? json.getString("sku") : null;
        productId = json.has("proposalId") ? json.getString("proposalId") : null;
//        long a = System.currentTimeMillis();
        if(sku != null && !"".equals( sku )) {
        	rawResponse = this.rc.getRequest( "GET", baseURL + "/list/Product2G/bySearch?query=" + java.net.URLEncoder.encode("Product2G.SKU = \"" + sku + "\"", "UTF-8") + "&metaData=true&fields=Product2G.ProductNo", null, headers );
            response = new JSONObject(rawResponse);
            if(response.getInt( "rowCount" ) > 0) {
              productId = response.getJSONArray( "rows" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 );
            }else {
	            rawResponse = this.rc.getRequest( "GET", baseURL + "/list/Article/bySearch?query=" + java.net.URLEncoder.encode("Article.SKU = \"" + sku + "\"", "UTF-8") + "&metaData=true&fields=Article.SupplierAID", null, headers );
	            response = new JSONObject(rawResponse);
	            if(response.getInt( "rowCount" ) > 0) {
	              productId = response.getJSONArray( "rows" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 );
	            }
            }
        }
        if(product == null && productId != null && !"".equals( productId )) {
			rawResponse = this.rc.getRequest( "GET", baseURL + "/list/Product2G/byItems?items=" 
        + java.net.URLEncoder.encode("'" + productId + "'@1", "UTF-8") 
        + "&metaData=true&fields=" 
        + java.net.URLEncoder.encode(
        		"Product2G.ProductNo,Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroup.Identifier"
        		+ ",Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroupLang.Name(es)", "UTF-8"),
        null, headers );
			if(!rawResponse.startsWith( "Response code: " )) {
				response = new JSONObject(rawResponse);
				if(response.getInt( "rowCount" ) > 0) {
					product = response.getJSONArray( "rows" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 );
					structureGroupId = response.getJSONArray( "rows" ).getJSONObject( 0 ).getJSONArray( "values" ).getJSONArray( 1 ).getString( 0 );
					structureGroupName = response.getJSONArray( "rows" ).getJSONObject( 0 ).getJSONArray( "values" ).getJSONArray( 2 ).getString( 0 );
				}
			} 
			if(product == null || "".equals(product)) {
				String[] pieces = checkArticle(productId);
				if(pieces != null) {
					product = pieces[0];
					structureGroupId = pieces[1];
					structureGroupName = pieces[2];
				}else {
					responses.put( new org.json.JSONObject().put("status", "Identificador no conocido o artículo sin referencia a una propuesta.").put("productId", productId) );
					return responses.toString();
				}
			}
        }
//        log("In identifying took: " + formatMillis(System.currentTimeMillis() - a) );
        if(product == null) {
          responses.put( new JSONObject().put( "status", "Not found" ).put( "sku", sku == null ? "" : sku ).put( "productId", productId == null ? "" : productId ) );
        }else {
//          a = System.currentTimeMillis();
          rawResponse = this.rc.getRequest( "GET", baseURL + "/object/Product2G/'" + java.net.URLEncoder.encode( product, "UTF-8" ) + "'@'MASTER'?includeLabels=true&includeIds=true", null, headers );
//          log("Retrieving data took: " + formatMillis(System.currentTimeMillis() - a) + " -- " + rawResponse);
          response = new JSONObject(rawResponse);
          business = getBusinessValueFromObject(response, "Business");
          String modificationDate = null;
//          log("Searching for following business: " + business + ", as well as for template: " + structureGroupId);
//          a = System.currentTimeMillis();
          losQueSi = !"".equals(business) && !"".equals(structureGroupId) && business != null && structureGroupId != null ? gatherFieldsToSendByBusiness( headers, structureGroupId, business ) : new java.util.TreeMap<>();
//          log("Adding template metadata data took: " + formatMillis(System.currentTimeMillis() - a));
          if(losQueSi.isEmpty()) {
//        	  a = System.currentTimeMillis();
        	  addGlobalData("Liverpool", losQueSi, baseURL, workshop.getRc().getHeader().get("Authorization"));
//        	  log("Adding global meta data took: " + formatMillis(System.currentTimeMillis() - a));
          }
          try{
          	modificationDate = response.getJSONObject("_data").getJSONArray("ownLog").getJSONObject(0).getString("modificationDate");
          }catch(org.json.JSONException ignore){}
          java.util.Set<String> multivalueCharacteristics = new java.util.TreeSet<>();
          String rr = null;
//          a = System.currentTimeMillis();
          try {
	          int ci = 0;
	          int tz = 0;
	          do {
		          rr = rc.getRequest("GET", baseURL + "/list/Characteristic/bySearch?query="
		        		  + java.net.URLEncoder.encode("Characteristic.UpperBound > 1", "UTF-8")
		        		  + "&pageSize=5000"
		        		  + "&fields=Characteristic.Identifier"
		        		  + "&startIndex=" + ci
		        		  , null);
		          org.json.JSONObject r = new org.json.JSONObject(rr);
		          tz = r.getInt("totalSize");
		          org.json.JSONArray rw = r.getJSONArray("rows");
		          for(int m=0; m<rw.length(); m++) {
		        	  ci++;
		        	  multivalueCharacteristics.add(rw.getJSONObject(m).getJSONArray("values").getString(0));
		          }
	          }while(ci < tz);
	          ci = 0;
          }catch(org.json.JSONException e) {
//        	  log("Exception, got: " + rr);
        	  logE(e);
          }
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
        responses.put( jsonRes );
        }
        product = null;
      }
      output = responses.toString();
    }catch(Exception e) { // Este catch con Exception e, es solo por fines de depuración al desarrollar.
      logE(e);
//      log("Raw response: " + rawResponse);
    }
    log("Elapsed time: " + formatMillis(System.currentTimeMillis() - init) );
    return output;
  }
  
	private String[] checkArticle(String id) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "ProductReference.ReferencedSupplierAid"
				+ ",ProductReference.ReferencedArticle->Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroup.Identifier"
				+ ",ProductReference.ReferencedArticle->Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroupLang.Name(es)"
				+ ",ProductReference.ReferencedArticle->Product2GCharacteristicValue.LookupValue('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool')->LookupValue.Code"
				+ ",ProductReference.ReferencedArticle->Product2G.CurrentStatus"
				);
		qp.put("items", "'" + id + "'@1");
		org.json.JSONObject response = null;
		response = workshop.makeRequest("GET", "/list/Article/ProductReference/byItems", qp, null);
		if(response != null && response.getJSONArray("rows").length() > 0) {
			return new String[] { response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(1).getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(2).getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(3).getString(0)
					, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(4) };
		}else if(response == null){
			System.out.println("ERROR: " + workshop.getRawResponse());
		}else {
			System.out.println("Unknown article id: " + id);
		}
		return null;
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
  		if(data.has("_characteristicRecords")) {
	  		characteristicRecords = data.getJSONArray("_characteristicRecords");
	  		for(int i=0; i<characteristicRecords.length(); i++){
	  			entry = characteristicRecords.getJSONObject(i);
	  			if(characteristic.equals( entry.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code") )){
	  				return entry.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
	  			}
	  		}
  		}
  		if(data.has("business")) {
  			return data.getJSONObject("business").getString("_label");
  		}
  	}catch(org.json.JSONException e){ /* log("Problema. " + e.getMessage()); */ logE(e); }
  	return null;
  }

  private org.json.JSONArray gatherVariants(java.util.LinkedList<String> articles, java.util.Map<String, String> headers, java.util.Map<String, String> variantLevelAttributes, org.json.JSONObject variantsRejectionBoard, String supplierPartNumber){
    org.json.JSONArray variants = new org.json.JSONArray();
    try {
      String rawResponse = null;
      JSONObject response = null;
      for(String article : articles) {
        rawResponse = this.rc.getRequest( "GET", baseURL + "/object/Article/'" + java.net.URLEncoder.encode( article, "UTF-8" ) + "'@'MASTER'?includeLabels=true&entityFilter=ArticleCharacteristicValue,Article,ArticleExtraData", null, headers );
        response = new JSONObject(rawResponse);
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

  private java.util.LinkedList<String> getVariants(java.util.Map<String, String> header, String productId){
    java.util.LinkedList<String> variants = new java.util.LinkedList<>();
    try {
      String rawResponse = null;
      JSONObject response = null;
      org.json.JSONArray rows = null;
      String url = baseURL + "/list/Article/bySearch?query=ProductReference.ReferencedSupplierAid(%22" + productId + "%22)%20equals%20%22" + productId + "%22&metaData=true&fields=Article.SupplierAID";
      rawResponse = this.rc.getRequest( "GET", url, null, header );
      response = new JSONObject(rawResponse);
      rows = response.getJSONArray( "rows" );
      for(int i=0; i<rows.length(); i++) {
        variants.addLast(rows.getJSONObject( i ).getJSONArray( "values" ).getString( 0 ));
      }
    }catch(Exception e) {
    	logE(e);
    }
    return variants;
  }

  private java.util.Map<String, String> gatherFieldsToSendByBusiness(java.util.Map<String, String> header, String template, String business) {
	long a = System.currentTimeMillis();
    java.util.Map<String, String> losQueSi = new java.util.TreeMap<>();
    if(!this.atributosValidosPorPlantilla.containsKey( template )) {
//      String rawResponse = null;
//      JSONObject response = null;
//      org.json.JSONArray rows = null;
//      int startIndex = 0;
//      int totalSize = 0;
    	DataRequestor dr = new DataRequestor();
		String resp = dr.getTemplateCharacteristicMetaDataByTemplate( new org.json.JSONArray().put(template) );
		try {
			org.json.JSONObject jr = new org.json.JSONObject(resp);
			org.json.JSONArray items = jr.getJSONArray("items");
			java.util.Map<String, java.util.Map<String, String>> characteristics = new java.util.HashMap<>();
			java.util.Map<String, String> properties = null;
			org.json.JSONObject item = null;
			org.json.JSONObject itemProperties = null;
			for(int i=0; i<items.length(); i++) {
				item = items.getJSONObject(i);
				if(item.length() > 0) {
					for(String name : org.json.JSONObject.getNames(item)) {
						properties = characteristics.get( name );
						if(properties == null) {
							properties = new java.util.HashMap<>();
							characteristics.put(name, properties);
						}
						itemProperties = item.getJSONObject(name);
						if(itemProperties.length() > 0) {
							for(String sn : org.json.JSONObject.getNames(itemProperties)) {
								properties.put(sn, itemProperties.getString(sn));
							}
							if(properties.containsKey("senttoVendorCenter") && "1".equals(properties.get("senttoVendorCenter")) && properties.containsKey("allowedBusiness") && properties.get("allowedBusiness").contains(business) && properties.containsKey("vendorCenterSection") ) {
								losQueSi.put(name, properties.get("vendorCenterSection"));
							}
						}
					}
				}
			}
		}catch(org.json.JSONException e) {
			logE(e);
		}
      /*
      try {
        String url = null;
        java.util.Map<String, String> properties = new java.util.TreeMap<>();
        log("Gathering field by business (" + business + ") and template: " + template + " to get their Vendor Center Section.");
        int times = 0;
        do {
          url = baseURL + "/list/StandardizationValue/bySearch"
          		+ "?dictionaryProxy='ExtensionDeMetadatos_%20ValoresPredeterminadosPorPlantilla'"
          		+ "&query=" 
          			+ java.net.URLEncoder.encode( 
          					"(StandardizationValue.Property->LookupValue.Code equals \"Business\" or StandardizationValue.Property->LookupValue.Code equals \"SentToVendorCenter\" or StandardizationValue.Property->LookupValue.Code equals \"VendorCenterSection\" or StandardizationValue.Property->LookupValue.Code equals \"OnlyForRead\")"
          					+ " and StandardizationValue.StructureGroup equals \"" + template + "\""
  							+ " and StandardizationValue.CreationType->LookupValue.Code equals \"CreateProposal\""
  							+ " and StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
						, "UTF-8"
  					)
      			+ "&metaData=true"
      			+ "&fields=" 
      				+ java.net.URLEncoder.encode(
      						"StandardizationValue.Characteristic->Characteristic.Identifier"
      						+ ",StandardizationValue.Property->LookupValue.Code"
      						+ ",StandardizationValue.PropertyValue"
  						, "UTF-8")
      			+ "&pageSize=3000&startIndex=" + startIndex;
          rawResponse = this.rc.getRequest( "GET", url, null, header );
          response = new JSONObject(rawResponse);
          rows = response.getJSONArray( "rows" );
          for(int i=0; i<rows.length(); i++) {
          	properties.put(rows.getJSONObject( i ).getJSONArray( "values" ).getString( 0 ) + "<::>" + rows.getJSONObject( i ).getJSONArray( "values" ).getString( 1 ), rows.getJSONObject( i ).getJSONArray( "values" ).getString( 2 ));
          }
          totalSize = response.getInt( "totalSize" );
          startIndex += response.getInt("pageSize");
          times++;
        }while(startIndex < totalSize);
	    startIndex = 0;
        */
//        log("In collecting dictionary data took: " + formatMillis(System.currentTimeMillis() - a));
        /*
        java.util.LinkedList<java.util.Map.Entry<String, String>> wrapper = new java.util.LinkedList<>(properties.entrySet());
        java.util.Collections.sort(wrapper, (o1,o2)->o1.getKey().split("<::>")[0].compareTo(o2.getKey().split("<::>")[0]));
        String ccharac = null;
        String prevCharac = null;
        String[] pieces = null;
        String property = null;
        String sentToVendorCenter = null;
        String cBusiness = null;
        String vendorCenterSection = null;
        for(java.util.Map.Entry<String, String> charac : wrapper){
        	pieces = charac.getKey().split("<::>");
        	ccharac = pieces[0];
        	property = pieces[1];
        	if(prevCharac != null && !prevCharac.equals(ccharac)){
        		if(cBusiness != null){
	        		if("1".equals(sentToVendorCenter) && cBusiness.contains(business) ){
    	    			losQueSi.put(prevCharac, vendorCenterSection);
	        		}else {
	        		}
        		}else{
        		}
        		vendorCenterSection = null;
        		sentToVendorCenter = null;
        		cBusiness = null;
        	}
        	if("VendorCenterSection".equals(property)){
        		vendorCenterSection = charac.getValue();
        	}else if("SentToVendorCenter".equals(property)){
        		sentToVendorCenter = charac.getValue();
        	}else if("Business".equals(property)){
        		cBusiness = charac.getValue();
        	}
        	prevCharac = ccharac;
        }
        if(cBusiness != null){
    		if("1".equals(sentToVendorCenter) && cBusiness.contains(business)){
    			losQueSi.put(prevCharac, vendorCenterSection);
    		}else {
    		}
		}else{
		}
		vendorCenterSection = null;
		sentToVendorCenter = null;
		cBusiness = null;
		*/
		addGlobalData(business, losQueSi, baseURL, this.rc.getHeader().get("Authorization"));
//        log("Los que sí: " + losQueSi);
        this.atributosValidosPorPlantilla.put( template, losQueSi );
//      }catch(Exception e) {
//    	  log(rawResponse);
//    	  logE(e);
//      }
    } else {
		losQueSi = this.atributosValidosPorPlantilla.get(template);
	}
    return losQueSi;
  }
	
	private void addGlobalData(String negocio, java.util.Map<String, String> attributeVendorCenterSection, String baseUrl, String authorization) throws ServiceUnavailableException {
		long a = System.currentTimeMillis();
		DataRequestor dr = new DataRequestor();
		String resp = dr.getGlobalMetaData();
		try {
			org.json.JSONObject jr = new org.json.JSONObject(resp);
			org.json.JSONArray items = jr.getJSONArray("items");
			java.util.Map<String, java.util.Map<String, String>> characteristics = new java.util.HashMap<>();
			java.util.Map<String, String> properties = null;
			org.json.JSONObject item = null;
			org.json.JSONObject itemProperties = null;
			for(int i=0; i<items.length(); i++) {
				item = items.getJSONObject(i);
				for(String name : org.json.JSONObject.getNames(item)) {
					properties = characteristics.get( name );
					if(properties == null) {
						properties = new java.util.HashMap<>();
						characteristics.put(name, properties);
					}
					itemProperties = item.getJSONObject(name);
					for(String sn : org.json.JSONObject.getNames(itemProperties)) {
						properties.put(sn, itemProperties.getString(sn));
					}
					if(properties.containsKey("senttoVendorCenter") && "1".equals(properties.get("senttoVendorCenter")) && properties.containsKey("allowedBusiness") && properties.get("allowedBusiness").contains(negocio) && properties.containsKey("vendorCenterSection") ) {
						attributeVendorCenterSection.put(name, properties.get("vendorCenterSection"));
					}
				}
			}
		}catch(org.json.JSONException e) {
			logE(e);
		}
//		RESTWorkshop rw = new RESTWorkshop();
//		if(baseUrl != null)
//			rw.setBaseUrl(baseUrl);
//		rw.addHeader("Authorization", authorization);
//		rw.putParameter("dictionaryProxy", "'GlobalTemplateAttributeConfiguration'");
//		rw.putParameter("fields", 
//				   "StandardizationValue.Characteristic->Characteristic.Identifier"
//				+ ",StandardizationValue.Characteristic->CharacteristicLang.Name(es)"
//				+ ",StandardizationValue.Property->LookupValueIdentifier.Code(EUCat)"
//				+ ",StandardizationValue.PropertyValue"
//				+ ",StandardizationValueLog.ModificationDate(PIM)"
//				+ ",StandardizationValueLog.CreationDate(PIM)"
//			);
//		rw.putParameter("query", 
//				  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"GlobalTemplateAttributeConfiguration\""
//			);
//		rw.putParameter("orderBy", "0-ASC");
//		rw.putParameter("pageSize", "3000");
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		int totalSize = 0;
//		int currentIndex = 0;
//		org.json.JSONObject detail = new org.json.JSONObject();
//		org.json.JSONArray prevValues = null;
//		int times = 0;
//		do {
//			rw.putParameter("startIndex", String.valueOf(currentIndex));
//			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch");
//			if(response != null && response.has("totalSize")) {
//				totalSize = response.getInt("totalSize");
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					currentIndex++;
//					values = rows.getJSONObject(i).getJSONArray("values");
//					if(prevValues != null && !prevValues.getString(0).equals(values.getString(0))) {
//						if(detail.has("senttoVendorCenter") && "1".equals(detail.getString("senttoVendorCenter")) && detail.has("allowedBusiness") && detail.getString("allowedBusiness").contains(negocio) && detail.has("vendorCenterSection") ) {
//							attributeVendorCenterSection.put(prevValues.getString(0), detail.getString("vendorCenterSection"));
//						}
//						detail = new org.json.JSONObject();
//					}
//					detail.put("characteristic", values.getString(0));
//					detail.put("friendlyName", values.getString(1));
//					detail.put(values.getString(2), values.getString(3));
//					prevValues = values;
//				}
//			}else {
//				log("ERR: " + rw.getRawResponse());
//			}
//			times++;
//		}while(currentIndex < totalSize);
//		log("In collecting global data took: " + formatMillis(System.currentTimeMillis() - a));
//		currentIndex = 0;
//		if(detail.length() > 0) {
//			if(detail.has("senttoVendorCenter") && "1".equals(detail.getString("senttoVendorCenter")) && detail.has("allowedBusiness") && detail.getString("allowedBusiness").contains(negocio) && detail.has("vendorCenterSection") ) {
//				attributeVendorCenterSection.put(prevValues.getString(0), detail.getString("vendorCenterSection"));
//			}
//			detail = null;
//		}
	}

	
	private static final Logger LOGGER = Logger.getLogger(GetProposals.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/java_process_proposal_request.%g.log", 15 * 1024 * 1024, 10, true);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            fileHandler.setLevel(Level.ALL);

            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    java.time.LocalDateTime dateTime =
                        java.time.Instant.ofEpochMilli(record.getMillis())
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();

                    String timestamp = dateTime.format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );

                    return "[" + timestamp + "] [" + record.getLevel() + "] " + formatMessage(record) + System.lineSeparator();
                }
            });

            LOGGER.addHandler(fileHandler);
//            LOGGER.setLevel(Level.WARNING);

        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el logger", e);
        }
    }
    
	private void log(String message){
//		LOGGER.info(message);
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/java_process_proposal_request.log", true)))){
		  pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "] (" + myId + ") " + message);
		}catch(java.io.IOException e){}
	}

	private void logE(Exception ex){
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/java_process_proposal_request.err", true)))){
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "] (" + myId + ") ERR ");
			ex.printStackTrace(pw);
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "] (" + myId + ") ERR. ");
		}catch(java.io.IOException e){}
	}
}