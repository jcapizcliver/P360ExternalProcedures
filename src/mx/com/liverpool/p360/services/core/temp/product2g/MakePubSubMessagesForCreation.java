package mx.com.liverpool.p360.services.core.temp.product2g;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.json.JSONObject;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RestClient;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.core.temp.xml.AnotherXMLHandlerSendRemaining;
import mx.com.liverpool.p360.services.core.temp.xml.AnotherXMLHandlerSendRemaining.Handler;
import mx.com.liverpool.p360.services.core.temp.xml.AnotherXMLHandlerSendRemaining.Product;
import mx.com.liverpool.p360.services.core.temp.xml.AnotherXMLHandlerSendRemaining.Value;

public class MakePubSubMessagesForCreation {

	  private final java.util.regex.Pattern sgp = java.util.regex.Pattern.compile("(?<=')(.+)(?='@'PrimaryProductTaxonomy')");
	  private final RESTWorkshop workshop;
	  private final RestClient rc; // = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + "cmVzdDpoZWlsZXI=");
	  private final java.util.Map<String, java.util.Map<String, String>> atributosValidosPorPlantilla = new java.util.TreeMap<>();

	  private final String baseURL; // = "http://172.18.237.162:1512/rest/V2.0";
	  private final long myId;
	

	private java.util.Set<String> multivalueCharacteristics = new java.util.TreeSet<>();
    private  int batchSize = 200;
	private java.util.Map<String, java.util.Map<String, String>> cacheLosQueSi = new java.util.TreeMap<>();
	private java.util.Map<String, String> global = new java.util.TreeMap<>();
	private java.util.Map<String, String> externalStatus = new java.util.TreeMap<>();
	private  java.util.Map<String, String> extStatus = new java.util.TreeMap<>();
	  
	public MakePubSubMessagesForCreation(String baseUrl, String encoded, long myId) throws ServiceUnavailableException {
		  this.baseURL = baseUrl;
		  this.workshop = new RESTWorkshop();
		  if(this.baseURL != null) {
			  workshop.setBaseUrl(baseUrl);
		  }
		  this.myId = myId;
		  rc = workshop.getRc();
		  if(encoded != null)
			  rc.getHeader().put("Authorization", "Basic: " + encoded);
		  
	        String rr = null;
	        long a = System.currentTimeMillis();
	        try {
		          int ci = 0;
		          int tz = 0;
		          do {
			          rr = rc.getRequest("GET", baseURL + "/list/Characteristic/bySearch?query="
			        		  + java.net.URLEncoder.encode("Characteristic.UpperBound > 1", "UTF-8")
			        		  + "&pageSize=200"
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
	      	  log("Exception, got: " + rr);
	      	  logE(e);
			} catch (UnsupportedEncodingException e) {
				logE(e);
			} catch (IOException e) {
				logE(e);
			}
	        log("Adding multivalued characteristics took: " + formatMillis(System.currentTimeMillis() - a));
	    	addGlobalData("Liverpool", global, baseURL, workshop.getRc().getHeader().get("Authorization"));
	    	java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
	    	qp0.put("fields", "StandardizationValue.Value,StandardizationValue.AlternativeValue");
	    	qp0.put("pageSize", "5000");
	    	qp0.put("dictionary", "'ExternalStatus'");
	    	RESTWrapper rw = new RESTWrapper();
	    	rw.collectData("list", "StandardizationValue", null, "byDictionary", qp0, row -> {
	    		extStatus.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1));
	    	}, this::log);
	    	System.out.println(extStatus);
	    	qp0.clear();
	    	qp0.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
	    	qp0.put("pageSize", "5000");
	    	qp0.put("lookup", "'ExternalStatus'");
	    	rw.collectData("list", "LookupValue", null, "byLookup", qp0, row->{
	    		externalStatus.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1));
	    	}, System.out::println);
	    	
		  
	  }

	public static void main(String[] args) throws ServiceUnavailableException {
		MakePubSubMessagesForCreation m = new MakePubSubMessagesForCreation(PropertiesManager.get("p360.contingency.base_url"), PropertiesManager.get("p360.contingency.basic_token_auth"), 100);
		try {
			m.processProduct(args[0]);
		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
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
	
	private void processProduct(String basePath) throws KeyManagementException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException, IOException, ParserConfigurationException, SAXException, ServiceUnavailableException {
		long init = System.currentTimeMillis();
		PubSubGCP ps = new PubSubGCP();
	    java.util.Map<String, String> headers = new java.util.HashMap<>();
	    headers.put( "Content-Type", "application/json" );
	    headers.put( "Accept", "application/json" );
	    headers.put( "Authorization", this.rc.getHeader().get("Authorization") );
	    headers.put( "Accept-Language", "es");
	    java.util.Map<String, String> losQueSi = null;
		java.util.LinkedList<String> productosParticipantes = new java.util.LinkedList<>();
		java.util.Set<String> skus = new java.util.TreeSet<>();
		java.util.Set<String> alreadySent = new java.util.TreeSet<>();
		try(java.util.stream.Stream<String> data = java.nio.file.Files.lines(java.nio.file.Paths.get("/", "u01", "workshop", "java", "alreadySent"))){
			data.forEach(alreadySent::add);
		}catch(java.io.IOException e) {
			logE(e);
		}
		log("Read: " + alreadySent.size() + " already sent.");
        log("Got: " + skus.size() + " to process...");
	        
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        AnotherXMLHandlerSendRemaining an = new AnotherXMLHandlerSendRemaining();
		java.io.File[] files = new java.io.File(basePath).listFiles(ff -> ff.getName().endsWith("xml"));
		log("Collecting products over " + files.length + " files.");
		String sku = null;
		java.util.Set<String> losQueEncontré = new java.util.TreeSet<>();
		for(java.io.File input : files) {
	        Handler handler = an.new Handler();
	        parser.parse(input, handler);
	        for (Product product : handler.getFinished()) {
	        	if(product.getParentId().startsWith("EU4") || product.getParentId().startsWith("UnCat")) {
	        		sku = getSKU(product.getValues());
	        		productosParticipantes.add(product.getId());
	        	}
	        }
        }
		
		for(String sk : skus) {
			if(!losQueEncontré.contains(sk)) {
				System.out.println("Este falta: " + sk);
			}
		}
		
        org.json.JSONObject productsObject = new org.json.JSONObject();
        org.json.JSONArray arr = new org.json.JSONArray();
		int count = 0;
		long a = 0l;
		log("Collected " + productosParticipantes.size() + " products.");
        System.out.println(externalStatus);
		if(productosParticipantes != null) {
			java.math.BigDecimal bdRefCounter = new java.math.BigDecimal(productosParticipantes.size());
			java.math.BigDecimal HUNDRED = java.math.BigDecimal.TEN.multiply(java.math.BigDecimal.TEN);
			for(String product : productosParticipantes) {
				if(alreadySent.contains(product)) {
					count++;
					continue;
				}
			    String business;
			    String rawResponse = null;
			    JSONObject response = null;
			    JSONObject json = null;
			    String structureGroupId = null;
			    String structureGroupName = null;
		        a = System.currentTimeMillis();
		        rawResponse = this.rc.getRequest( "GET", baseURL + "/object/Product2G/'" + java.net.URLEncoder.encode( product, "UTF-8" ) + "'@'MASTER'?includeLabels=true&includeIds=true", null, headers );
		        log("Retrieving data took: " + formatMillis(System.currentTimeMillis() - a));
		        try{
		        	response = new JSONObject(rawResponse);
		        }catch(org.json.JSONException e) {
		        	log(rawResponse);
		        	logE(e);
		        	count++;
		        	continue;
		        }
		        String negocio = getCharacteristicValueFromObjectCode(response, "Negocio");
		        String extwgs4h = getCharacteristicValueFromObjectCode(response, "EXTWG_S4H");
		        business = determineBusiness(negocio, extwgs4h);
		        if(business == null) {
		        	count++;
		        	log("Not able to determine business for: " + product);
		        	continue;
		        }
		        String modificationDate = null;
		        structureGroupId = getTemplate(response.getJSONObject("_data"));
		        log("Searching for following business: " + business + ", as well as for template: " + structureGroupId + " [" + product + "]");
		        a = System.currentTimeMillis();
		        losQueSi = cacheLosQueSi.get(structureGroupId + "<::>" + business);
		        if(losQueSi == null) {
			        losQueSi = !"".equals(business) && !"".equals(structureGroupId) && business != null && structureGroupId != null ? gatherFieldsToSendByBusiness( headers, structureGroupId, business ) : new java.util.TreeMap<>();
			        log("Adding template metadata data took (for " + structureGroupId + " and " + business + "): " + formatMillis(System.currentTimeMillis() - a));
			        if(losQueSi.isEmpty()) {
			      	  a = System.currentTimeMillis();
			        }
			        losQueSi.putAll(global);
			        log("Adding global meta data took: " + formatMillis(System.currentTimeMillis() - a));
			        cacheLosQueSi.put(structureGroupId + "<::>" + business, losQueSi);
		        }else {
		        	log("Recycled data");
		        }
		        try{
		        	modificationDate = response.getJSONObject("_data").getJSONArray("ownLog").getJSONObject(0).getString("modificationDate");
		        }catch(org.json.JSONException ignore){}
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
		        a = System.currentTimeMillis();
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
		          		  log("--------------------------------------------------------------->" + characteristicRecord);
		          	  }
		              o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
		              try{
		                basicData.put( characteristicIdentifier, o instanceof JSONObject ? ((JSONObject)o).getString( "_label" ) : String.valueOf( o ));
		              }catch(org.json.JSONException e) {
		                log("Bad reference attempted in: " + characteristicRecord.toString());
		              }
		            }else if("Fotografías".equals(vcs)){
		          	  try{
		          		  log("Got a picture for our compa... " + characteristicIdentifier);
			                o = characteristicRecord;
			                helper = new JSONObject();
			                helper.put( "PhotoAssetType", characteristicIdentifier );
			                json = (JSONObject) o;
			                children = json.getJSONArray( "_children" );
			                for(int k=0; k<children.length(); k++) {
			                  child = children.getJSONObject( k );
			                  if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Status" )) {
			                    helper.put( "PhotoAssetStatus",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getJSONObject( 0 ).getString( "_label" ));
			                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_URL" )) {
			                     helper.put( "PhotoAssetURL",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
			                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Name" )) {
			                     helper.put( "PhotoAssetName",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
			                   }
			                }
			                photos.put( helper );
			              }catch(org.json.JSONException e) {
			                log("ERROR: " + e.getMessage() + ". " + String.valueOf(child) + " ## " + characteristicIdentifier);
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
			                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_URL" )) {
			                     helper.put( "MultimediaAssetURL",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
			                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Name" )) {
			                     helper.put( "MultimediaAssetName",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
			                   }
			                }
			                multiMedia.put( helper );
			              }catch(org.json.JSONException e) {
			                log("ERROR: " + e.getMessage() + ". " + String.valueOf(child) + " ## " + characteristicIdentifier);
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
		        if(!basicData.has("DescriptionLong")) {
		            if(response.getJSONObject( "_data" ).has("lang")) {
		          	  org.json.JSONArray lang = response.getJSONObject("_data" ).getJSONArray("lang");
		          	  org.json.JSONObject innerObject = null;
		          	  for(int index=0; index<lang.length(); index++) {
		          		  innerObject = lang.getJSONObject(index);
		          		  if(innerObject.has("descriptionLong") && "esl".equals(innerObject.getJSONObject("_qualification").getJSONObject("language").getString("_code"))) {
		          			 descriptionLong = innerObject.getString("descriptionLong");
		          			 basicData.put("DescriptionLong", descriptionLong);
		          		  }
		          	  }
		            }
		        }
		        log("Building response took: " + formatMillis(System.currentTimeMillis() - a));
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
		        a = System.currentTimeMillis();
		        java.util.LinkedList<String> variants = getVariants(headers, product);
		        log("Getting variants took: " + formatMillis(System.currentTimeMillis() - a));
		        org.json.JSONObject variantRejectionBoard = new org.json.JSONObject();
		        a = System.currentTimeMillis();
		        String es = response.getJSONObject("_data").has("currentStatus") ? String.valueOf( response.getJSONObject("_data").getJSONObject("currentStatus").getInt("_key") ) : null;
		        String esl = null;
	        	esl = es != null ? extStatus.get( es ) != null ? externalStatus.get( extStatus.get( es ) ) : null : null;
		        org.json.JSONArray productVariants = gatherVariants( variants, headers, losQueSi, variantRejectionBoard, esl );
		        log("Gathering current variant data took: " + formatMillis( System.currentTimeMillis() - a ));
		        if(variantRejectionBoard.length() > 0) {
					modifiedFields.put("variants", variantRejectionBoard);
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
		        jsonRes.put("externalStatus", esl );
		        if(comments.length() > 0) {
		      	  jsonRes.put("userRemarks", comments);
		        }
		        if(modifiedFields.length() > 0) {
		      	  jsonRes.put("modifiedFields", modifiedFields);
		        }
		        arr.put(jsonRes);
		        if(arr.length() == batchSize) {
		        	productsObject.put("products", arr);
		        	ps.publishMessage(PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
									  PropertiesManager.get( "p360.contingency.gcp.post_products_topic" ), 
									  PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), productsObject.toString());
		        	arr = new org.json.JSONArray();
		        }
	        	count++;
	        	log(count + "/" + productosParticipantes.size() + " " + (new java.math.BigDecimal(count).multiply(HUNDRED).divide( bdRefCounter, 4, java.math.RoundingMode.HALF_UP )) + "% " + workshop.formatTime(System.currentTimeMillis() - init) + " <::> " + product);
			}
			if(arr.length() > 0) {
	        	productsObject.put("products", arr);
	        	ps.publishMessage(PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
								  PropertiesManager.get( "p360.contingency.gcp.post_products_topic" ), 
								  PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), productsObject.toString());
	        	arr = new org.json.JSONArray();
			}
			log("Done. " + formatMillis(System.currentTimeMillis() - init));
		}
	}
    
	
	public void processSingleProduct(String proposalId) throws KeyManagementException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException, IOException, ParserConfigurationException, SAXException, ServiceUnavailableException {
		long init = System.currentTimeMillis();
		PubSubGCP ps = new PubSubGCP();
	    java.util.Map<String, String> headers = new java.util.HashMap<>();
	    headers.put( "Content-Type", "application/json" );
	    headers.put( "Accept", "application/json" );
	    headers.put( "Authorization", this.rc.getHeader().get("Authorization") );
	    headers.put( "Accept-Language", "es");
        long a = System.currentTimeMillis();
        int batchSize = 200;
        org.json.JSONObject productsObject = new org.json.JSONObject();
        org.json.JSONArray arr = new org.json.JSONArray();
        log("Adding multivalued characteristics took: " + formatMillis(System.currentTimeMillis() - a));
		java.util.Map<String, java.util.Map<String, String>> cacheLosQueSi = new java.util.TreeMap<>();
	    java.util.Map<String, String> losQueSi = null;
    	System.out.println(externalStatus);
			String product = proposalId;
			    String business;
			    String rawResponse = null;
			    JSONObject response = null;
			    JSONObject json = null;
			    String structureGroupId = null;
			    String structureGroupName = null;
		        a = System.currentTimeMillis();
		        rawResponse = this.rc.getRequest( "GET", baseURL + "/object/Product2G/'" + java.net.URLEncoder.encode( product, "UTF-8" ) + "'@'MASTER'?includeLabels=true&includeIds=true", null, headers );
		        log("Retrieving data took: " + formatMillis(System.currentTimeMillis() - a));
		        try{
		        	response = new JSONObject(rawResponse);
		        }catch(org.json.JSONException e) {
		        	log(rawResponse);
		        	logE(e);
		        	return;
		        }
		        String negocio = getCharacteristicValueFromObjectCode(response, "Negocio");
		        String extwgs4h = getCharacteristicValueFromObjectCode(response, "EXTWG_S4H");
		        business = determineBusiness(negocio, extwgs4h);
		        if(business == null) {
		        	log("Not able to determine business for: " + product);
		        	return;
		        }
		        String modificationDate = null;
		        structureGroupId = getTemplate(response.getJSONObject("_data"));
		        log("Searching for following business: " + business + ", as well as for template: " + structureGroupId + " [" + product + "]");
		        a = System.currentTimeMillis();
		        losQueSi = cacheLosQueSi.get(structureGroupId + "<::>" + business);
		        if(losQueSi == null) {
			        losQueSi = !"".equals(business) && !"".equals(structureGroupId) && business != null && structureGroupId != null ? gatherFieldsToSendByBusiness( headers, structureGroupId, business ) : new java.util.TreeMap<>();
			        log("Adding template metadata data took (for " + structureGroupId + " and " + business + "): " + formatMillis(System.currentTimeMillis() - a));
			        if(losQueSi.isEmpty()) {
			      	  a = System.currentTimeMillis();
			        }
			        losQueSi.putAll(global);
			        log("Adding global meta data took: " + formatMillis(System.currentTimeMillis() - a));
			        cacheLosQueSi.put(structureGroupId + "<::>" + business, losQueSi);
		        }else {
		        	log("Recycled data");
		        }
		        try{
		        	modificationDate = response.getJSONObject("_data").getJSONArray("ownLog").getJSONObject(0).getString("modificationDate");
		        }catch(org.json.JSONException ignore){}
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
		        a = System.currentTimeMillis();
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
		          		  log("--------------------------------------------------------------->" + characteristicRecord);
		          	  }
		              o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
		              try{
		                basicData.put( characteristicIdentifier, o instanceof JSONObject ? ((JSONObject)o).getString( "_label" ) : String.valueOf( o ));
		              }catch(org.json.JSONException e) {
		                log("Bad reference attempted in: " + characteristicRecord.toString());
		              }
		            }else if("Fotografías".equals(vcs)){
		          	  try{
		          		  log("Got a picture for our compa... " + characteristicIdentifier);
			                o = characteristicRecord;
			                helper = new JSONObject();
			                helper.put( "PhotoAssetType", characteristicIdentifier );
			                json = (JSONObject) o;
			                children = json.getJSONArray( "_children" );
			                for(int k=0; k<children.length(); k++) {
			                  child = children.getJSONObject( k );
			                  if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Status" )) {
			                    helper.put( "PhotoAssetStatus",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getJSONObject( 0 ).getString( "_label" ));
			                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_URL" )) {
			                     helper.put( "PhotoAssetURL",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
			                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Name" )) {
			                     helper.put( "PhotoAssetName",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
			                   }
			                }
			                photos.put( helper );
			              }catch(org.json.JSONException e) {
			                log("ERROR: " + e.getMessage() + ". " + String.valueOf(child) + " ## " + characteristicIdentifier);
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
			                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_URL" )) {
			                     helper.put( "MultimediaAssetURL",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
			                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Name" )) {
			                     helper.put( "MultimediaAssetName",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
			                   }
			                }
			                multiMedia.put( helper );
			              }catch(org.json.JSONException e) {
			                log("ERROR: " + e.getMessage() + ". " + String.valueOf(child) + " ## " + characteristicIdentifier);
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
		        if(!basicData.has("DescriptionLong")) {
		            if(response.getJSONObject( "_data" ).has("lang")) {
		          	  org.json.JSONArray lang = response.getJSONObject("_data" ).getJSONArray("lang");
		          	  org.json.JSONObject innerObject = null;
		          	  for(int index=0; index<lang.length(); index++) {
		          		  innerObject = lang.getJSONObject(index);
		          		  if(innerObject.has("descriptionLong") && "esl".equals(innerObject.getJSONObject("_qualification").getJSONObject("language").getString("_code"))) {
		          			 descriptionLong = innerObject.getString("descriptionLong");
		          			 basicData.put("DescriptionLong", descriptionLong);
		          		  }
		          	  }
		            }
		        }
		        log("Building response took: " + formatMillis(System.currentTimeMillis() - a));
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
		        a = System.currentTimeMillis();
		        java.util.LinkedList<String> variants = getVariants(headers, product);
		        log("Getting variants took: " + formatMillis(System.currentTimeMillis() - a));
		        org.json.JSONObject variantRejectionBoard = new org.json.JSONObject();
		        a = System.currentTimeMillis();
		        String es = response.getJSONObject("_data").has("currentStatus") ? String.valueOf( response.getJSONObject("_data").getJSONObject("currentStatus").getInt("_key") ) : null;
		        String esl = null;
	        	esl = es != null ? extStatus.get( es ) != null ? externalStatus.get( extStatus.get( es ) ) : null : null;
		        org.json.JSONArray productVariants = gatherVariants( variants, headers, losQueSi, variantRejectionBoard, esl );
		        log("Gathering current variant data took: " + formatMillis( System.currentTimeMillis() - a ));
		        if(variantRejectionBoard.length() > 0) {
					modifiedFields.put("variants", variantRejectionBoard);
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
		        jsonRes.put("externalStatus", esl );
		        if(comments.length() > 0) {
		      	  jsonRes.put("userRemarks", comments);
		        }
		        if(modifiedFields.length() > 0) {
		      	  jsonRes.put("modifiedFields", modifiedFields);
		        }
		        arr.put(jsonRes);
		        if(arr.length() == batchSize) {
		        	productsObject.put("products", arr);
		        	ps.publishMessage(PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
									  PropertiesManager.get( "p360.contingency.gcp.post_products_topic" ), 
									  PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), productsObject.toString());
		        	arr = new org.json.JSONArray();
		        }
			if(arr.length() > 0) {
	        	productsObject.put("products", arr);
	        	ps.publishMessage(PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
								  PropertiesManager.get( "p360.contingency.gcp.post_products_topic" ), 
								  PropertiesManager.get( "p360.contingency.gcp.service_account_back" ), productsObject.toString());
	        	arr = new org.json.JSONArray();
			}
			log("Done. " + formatMillis(System.currentTimeMillis() - init));
	}
    
	private String getSKU(java.util.LinkedList<Value> values) {
    	if(values != null) {
	    	for(Value value : values) {
	    		if("SKU".equals(value.getAttributeId())) {
	    			return value.getText();
	    		}
	    	}
    	}
    	return null;
    }
	
	private String getTemplate(org.json.JSONObject data) {
		if(data.has("structureGroupMap")) {
			org.json.JSONArray sgm = data.getJSONArray("structureGroupMap");
			org.json.JSONObject sg;
			for(int i=0; i<sgm.length(); i++) {
				sg = sgm.getJSONObject(i);
				if(sg.getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId").endsWith("'@'PrimaryProductTaxonomy'")) {
					java.util.regex.Matcher m = sgp.matcher(sg.getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId"));
					if(m.find()) {
						return m.group();
					}
				}
			}
		}
		return null;
	}

	private String determineBusiness(String negocio, String extwgS4h) {
		return "".equals(negocio) && "".equals(extwgS4h) ? null : "".equals(negocio) && !"".equals(extwgS4h) ? "Suburbia": "MARKETPLACE".equals(negocio) ? "Marketplace" : "Liverpool";
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
					  log("No valid prefix found for: " + childRejectionClassName + " (" + childRejection + ")");
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
							  log("Look at me ese ---->" + childRejectionClassName);
						  }
						  if(targetStructure.length() > 0) {
							childRejectionTargetStructures.put(targetStructure);
						}
					  }
				  }else if(child.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code").endsWith("_Name")) {
					  assetName = child.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
				  }
			  }
			  if(childRejectionTargetStructures.length() > 0) {
				destination.put(assetName, childRejectionTargetStructures);
			}
		  }
	  }

	  private String getCharacteristicValueFromObjectCode(org.json.JSONObject objectAPIResponse, String characteristic){
	  	org.json.JSONObject entry = null;
	  	org.json.JSONObject data = null;
	  	org.json.JSONArray characteristicRecords = null;
	  	try{
	  		data = objectAPIResponse.getJSONObject("_data");
	  		characteristicRecords = data.getJSONArray("_characteristicRecords");
	  		for(int i=0; i<characteristicRecords.length(); i++){
	  			entry = characteristicRecords.getJSONObject(i);
	  			if(characteristic.equals( entry.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code") )){
	  				return entry.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
	  			}
	  		}
	  	}catch(org.json.JSONException e){ logE(e); }
	  	return null;
	  }

	  private org.json.JSONArray gatherVariants(java.util.LinkedList<String> articles, java.util.Map<String, String> headers, java.util.Map<String, String> variantLevelAttributes, org.json.JSONObject variantsRejectionBoard, String externalStatus){
	    org.json.JSONArray variants = new org.json.JSONArray();
	    try {
	      String rawResponse = null;
	      JSONObject response = null;
	      for(String article : articles) {
	        rawResponse = this.rc.getRequest( "GET", baseURL + "/object/Article/'" + java.net.URLEncoder.encode( article, "UTF-8" ) + "'@'MASTER'?includeLabels=true&entityFilter=ArticleCharacteristicValue,Article", null, headers );
	        response = new JSONObject(rawResponse);
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
	        log("Now classifying information found for variant: " + article);
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
	                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_URL" )) {
	                     helper.put( "PhotoAssetURL",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
	                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Name" )) {
	                     helper.put( "PhotoAssetName",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
	                   }
	                }
	                photos.put( helper );
	              }catch(org.json.JSONException e) {
	                log("ERROR: " + e.getMessage() + ". " + String.valueOf(child));
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
	                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_URL" )) {
	                     helper.put( "MultimediaAssetURL",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
	                   }else if(child.getJSONObject( "_qualification" ).getJSONObject( "characteristic" ).getString( "_code" ).endsWith( "_Name" )) {
	                     helper.put( "MultimediaAssetName",  child.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray( "values" ).getString( 0 ));
	                   }
	                }
	                multiMedia.put( helper );
	              }catch(org.json.JSONException e) {
	                log("ERROR: " + e.getMessage() + ". " + String.valueOf(child));
	              }
	            }
	            else{
	              o = characteristicRecord.getJSONArray( "_recordLang" ).getJSONObject( 0 ).getJSONArray("values").get( 0 );
	              jsonRes.put( characteristicIdentifier, o instanceof JSONObject ? ((JSONObject)o).getString( "_label" ) : String.valueOf( o ));
	            }
	          }
	        }

	        org.json.JSONObject modifiedFields = new org.json.JSONObject();
	        if(productImage.length() > 0) {
				modifiedFields.put("ProductImage", productImage);
			}
	        if(productImageDetail.length() > 0) {
				modifiedFields.put("ProductImageDetail", productImageDetail);
			}
	        if(productImageSmosh.length() > 0) {
				modifiedFields.put("ProductImageSmosh", productImageSmosh);
			}
	        if(illustration.length() > 0) {
				modifiedFields.put("Illustration", illustration);
			}
	        aggregateRejectionsByField(rechazos, variantLevelAttributes, modifiedFields);
	        if(modifiedFields.length() > 0) {
	        	variantsRejectionBoard.put(article, modifiedFields);
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
	        jsonRes.put("externalStatus", externalStatus != null && !externalStatus.isEmpty() ? externalStatus : response.getJSONObject("_data").has("externalStatus") ? response.getJSONObject("_data").getJSONObject("externalStatus").getString("_label") : "");
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

	    }
	    return variants;
	  }

	  private java.util.Map<String, String> gatherFieldsToSendByBusiness(java.util.Map<String, String> header, String template, String business) {
	    java.util.Map<String, String> losQueSi = new java.util.TreeMap<>();
	    if(!this.atributosValidosPorPlantilla.containsKey( template )) {
	      String rawResponse = null;
	      JSONObject response = null;
	      org.json.JSONArray rows = null;
	      int startIndex = 0;
	      int totalSize = 0;
	      long a = System.currentTimeMillis();
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
	        log("In collecting dictionary data took: " + formatMillis(System.currentTimeMillis() - a) + ", times: " + times + ", " + startIndex);
	        startIndex = 0;
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
		        		if("1".equals(sentToVendorCenter) && cBusiness.contains(business) /* && vendorCenterSection != null && !globalSections.contains(vendorCenterSection) */){
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
//			addGlobalData(business, losQueSi, baseURL, this.rc.getHeader().get("Authorization"));
//	        log("Los que sí: " + losQueSi);
	        this.atributosValidosPorPlantilla.put( template, losQueSi );
	      }catch(Exception e) {
	    	  log(rawResponse);
	    	  logE(e);
	      }
	    } else {
			losQueSi = this.atributosValidosPorPlantilla.get(template);
		}
	    return losQueSi;
	  }
		
		private void addGlobalData(String negocio, java.util.Map<String, String> attributeVendorCenterSection, String baseUrl, String authorization) throws ServiceUnavailableException {
			RESTWorkshop rw = new RESTWorkshop();
			if(baseUrl != null)
				rw.setBaseUrl(baseUrl);
			rw.addHeader("Authorization", authorization);
			rw.putParameter("dictionaryProxy", "'GlobalTemplateAttributeConfiguration'");
			rw.putParameter("fields", 
					   "StandardizationValue.Characteristic->Characteristic.Identifier"
					+ ",StandardizationValue.Characteristic->CharacteristicLang.Name(es)"
					+ ",StandardizationValue.Property->LookupValueIdentifier.Code(EUCat)"
					+ ",StandardizationValue.PropertyValue"
					+ ",StandardizationValueLog.ModificationDate(PIM)"
					+ ",StandardizationValueLog.CreationDate(PIM)"
				);
			rw.putParameter("query", 
					  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"GlobalTemplateAttributeConfiguration\""
				);
			rw.putParameter("orderBy", "0-ASC");
			rw.putParameter("pageSize", "3000");
			org.json.JSONObject response = null;
			org.json.JSONArray rows = null;
			org.json.JSONArray values = null;
			int totalSize = 0;
			int currentIndex = 0;
			org.json.JSONObject detail = new org.json.JSONObject();
			org.json.JSONArray prevValues = null;
			long a = System.currentTimeMillis();
			int times = 0;
			do {
				rw.putParameter("startIndex", String.valueOf(currentIndex));
				response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch");
				if(response != null && response.has("totalSize")) {
					totalSize = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						currentIndex++;
						values = rows.getJSONObject(i).getJSONArray("values");
						if(prevValues != null && !prevValues.getString(0).equals(values.getString(0))) {
							if(detail.has("senttoVendorCenter") && "1".equals(detail.getString("senttoVendorCenter")) && detail.has("allowedBusiness") && detail.getString("allowedBusiness").contains(negocio) && detail.has("vendorCenterSection") ) {
								attributeVendorCenterSection.put(prevValues.getString(0), detail.getString("vendorCenterSection"));
							}
							detail = new org.json.JSONObject();
						}
						detail.put("characteristic", values.getString(0));
						detail.put("friendlyName", values.getString(1));
						detail.put(values.getString(2), ("Section".equals(values.getString(0)) || "Direction".equals(values.getString(0))) && "senttoVendorCenter".equals(values.getString(2)) && "0".equals(values.getString(3)) ? "1" : values.getString(3));
						prevValues = values;
					}
				}else {
					log("ERR: " + rw.getRawResponse());
				}
				times++;
			}while(currentIndex < totalSize);
			log("In collecting global data took: " + formatMillis(System.currentTimeMillis() - a) + ", times: " + times + ", " + currentIndex);
			currentIndex = 0;
			if(detail.length() > 0) {
				if(detail.has("senttoVendorCenter") && "1".equals(detail.getString("senttoVendorCenter")) && detail.has("allowedBusiness") && detail.getString("allowedBusiness").contains(negocio) && detail.has("vendorCenterSection") ) {
					attributeVendorCenterSection.put(prevValues.getString(0), detail.getString("vendorCenterSection"));
				}
				detail = null;
			}
		}
		
		private void log(String message){
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get( "..", "logs", "get_proposals.log").toString(), true)))){
			  pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "] (" + myId + ") " + message);
			}catch(java.io.IOException e){}
		}

		private void logE(Exception ex){
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get( "..", "logs", "get_proposals.log").toString(), true)))){
			  ex.printStackTrace(pw);
			}catch(java.io.IOException e){}
		}
	
}
