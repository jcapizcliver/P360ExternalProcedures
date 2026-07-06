package mx.com.liverpool.dataprofiling.preparison.envioproductos;

import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.xml.ProcessXMLFiles;
import mx.com.liverpool.p360.services.core.xml.ProductFileAssetElement;
import mx.com.liverpool.p360.services.core.xml.ProductFileHandler;
import mx.com.liverpool.p360.services.core.xml.ProductFileProductElement;
import mx.com.liverpool.p360.services.core.xml.ProductFileValueElement;

public class PruebaEnvioPubSubMediaAssets extends ProcessXMLFiles {
	
	private static final java.util.Map<String, String> globalVendorCenterSections = getMeTemplateData();

	private final RESTWrapper rw = new RESTWrapper();
	private final java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?<=Flujo Actual: )([^|]+)");
	private final java.util.regex.Pattern p0 = java.util.regex.Pattern.compile("(?<=Estado en el WF: )([^|]+)");
	private final java.util.Set<String> parentsToResend = new java.util.TreeSet<>();
	private final org.json.JSONArray products = new org.json.JSONArray();
	private final java.util.Map<String, String> internalStatusMap = loadInternalStatusMap();
	private final java.util.Map<String, String> externalStatusMap = loadExternalStatusMap();
	private final java.util.Map<String, String> internalToExternalStatusMap = loadInternalToExternalStatusMap();
	private final java.util.Map<String, String> qp = new java.util.HashMap<>();
	private final org.json.JSONObject listWriteBody;
	private final java.util.Map<String, java.util.Set<String>> idsToFiles = new java.util.HashMap<>();
	private final java.util.Set<String> variantesSinProductoPadre = new java.util.HashSet<>();

	private int productsCount = 0;
	private long gc = 0;
	
	public PruebaEnvioPubSubMediaAssets() {
		qp.put("includeObjectsInProtocol", "false");
		listWriteBody = new org.json.JSONObject()
				.put("columns", new org.json.JSONArray()
						.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"))
						.put(new org.json.JSONObject().put("identifier", "Product2G.PrevStatus"))
						.put(new org.json.JSONObject().put("identifier", "Product2G.ExternalStatus"))
						.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EnriquecidoEnForo',root,\"0000.0000.RK\",'EnriquecidoEnForo',-1)")))
				.put("rows", products);
	}

	private PubSubGCP pub = new PubSubGCP(
			 PropertiesManager.get( "p360.contingency.gcp.service_account_back" ),
			 PropertiesManager.get( "p360.contingency.gcp.project_back" ), 
			 PropertiesManager.get( "p360.contingency.gcp.post_products_topic" )
			);
	private org.json.JSONArray jps = new org.json.JSONArray();
	private org.json.JSONObject body = new org.json.JSONObject().put("products", jps);
	
	private int lacuenta = 0;
	private int lacuentaVars = 0;
	private int perdidas = 0;
	private boolean running = true;
    
    private void eleseFinish() {
    	java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWrapper rw = new RESTWrapper();
		rw.getRw().setBaseUrl("https://chat.googleapis.com/v1/spaces"); // ");
		qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
		qp.put("token", "H3kGU98FssCp15V7VT9s1nltZfbLxuj94WFRMOVzAs0");
		log( "" + rw.getRw().makeRequest("POST", "/AAAAvwSYdXo/messages", qp, 
				new org.json.JSONObject().put("text", 
						"Envío de productos a EU Cat finalizado. Productos procesados: " + lacuenta + " gen/ind (" + lacuentaVars + " vars, " + perdidas + " variantes sueltas) 😁.").toString()) );
    }
    
    private void eleseProgress() {
    	java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWrapper rw = new RESTWrapper();
		rw.getRw().setBaseUrl("https://chat.googleapis.com/v1/spaces"); // ");
		qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
		qp.put("token", "H3kGU98FssCp15V7VT9s1nltZfbLxuj94WFRMOVzAs0");
		log( "" + rw.getRw().makeRequest("POST", "/AAAAvwSYdXo/messages", qp, 
				new org.json.JSONObject().put("text", 
						"Productos enviados a EUCat: " + lacuenta + " gen/ind (" + lacuentaVars + " variantes) 😁.").toString()) );
    }
	
	@Override
	public void inspectHandler(DefaultHandler handler) {
		if( handler instanceof ProductFileHandler ) {
			ProductFileHandler pfh = (ProductFileHandler) handler;
			java.util.Map<String, ProductFileAssetElement> productAssetMap = pfh.getAssetMap();
			java.util.List<ProductFileProductElement> products = pfh.getFinished();
			for(ProductFileProductElement product : products) {
				processProductElement(product, globalVendorCenterSections, productAssetMap);
			}
			if(productsCount > 0) {
				log("Writing data... " + listWriteBody);
				while(this.products.length() > 0) {
					this.products.remove(0);
				}
				productsCount = 0;
			}
		}else {
			// Handler not supported.
		}
	}
	
	public String getFlujoActual(String calculatedWFAtt) {
		if(calculatedWFAtt != null) {
			java.util.regex.Matcher m = p.matcher(calculatedWFAtt);
			if(m.find()) {
				return m.group(1);
			}
		}
		return null;
	}
	
	public String getEstadoEnElWF(String calculatedWFAtt) {
		if(calculatedWFAtt != null) {
			java.util.regex.Matcher m = p0.matcher(calculatedWFAtt);
			if(m.find()) {
				return m.group(1);
			}
		}
		return null;
	}
	
	public String[] computeStatus(String calculatedWFAtt, String stateSKU, String fotoTomadaLiverpool, String productId) {
		String[] res = new String[3];
		res[0] = null;
		res[1] = null;
		res[2] = "false";
		String flujoActual = getFlujoActual(calculatedWFAtt);
		String estado = getEstadoEnElWF(calculatedWFAtt);
		if("N/A".equals(flujoActual) && "N/A".equals(estado) && !"Aprobado".equals(stateSKU) && "Y".equals(fotoTomadaLiverpool)) {
			res[0] = "1002"; // Pendiente Inicio Enriquecimiento
			res[1] = "1020";
		} else if("N/A".equals(flujoActual) && "N/A".equals(estado) && !"Aprobado".equals(stateSKU) && "N".equals(fotoTomadaLiverpool)) {
			res[0] = "1004";
			res[1] = "1020";
		} else if("N/A".equals(flujoActual) && "N/A".equals(estado) && "Aprobado".equals(stateSKU)) {
			res[0] = "1007";
			res[1] = "1023";
		} else if("ItemMaintenanceWorkFlow".equals(flujoActual) && "BuyerReview".equals(estado)) {
			res[0] = "1003";  // Revisión Compras
			res[1] = "10031"; // Borrador
		} else if("ItemMaintenanceWorkFlow".equals(flujoActual) && "SupplierReviewChange".equals(estado)) {
			res[0] = "1004";
			res[1] = "1020";
		} else if("ItemMaintenanceWorkFlow".equals(flujoActual) && "SupplierModification".equals(estado) && "Aprobado".equals(stateSKU)) {
			res[0] = "1007"; // Aprobado
			res[1] = "1023"; // Category
		} else if("ItemMaintenanceWorkFlow".equals(flujoActual) && "SupplierModification".equals(estado) && !"Aprobado".equals(stateSKU)) {
			res[0] = "1004"; // Carga de Imágen
			res[1] = "1020"; // Creación de SKU
		} else if("ItemMaintenanceWorkFlow".equals(flujoActual) && ("DataGovermentInitiate".equals(estado) || "ErrorRevision".equals(estado) || "DigitalAssetsReview".equals(estado))) {
			res[0] = "1021"; // Gobierno de Datos 
			res[1] = "1020";
		} else if("ItemMaintenanceWorkFlow".equals(flujoActual) && "QAReview".equals(estado)) {
			res[0] = "1022";
			res[1] = "1020";
		} else if("ItemMaintenanceWorkFlow".equals(flujoActual) && "CategoryManager".equals(estado)) {
			res[0] = "1023";
			res[1] = "1022";
		} else if("SalesItemCreationRevised".equals(flujoActual) && "Categorizacion".equals(estado)) {
			res[0] = "1021";
			res[1] = "1026"; // En proceso foro
			res[2] = "true";
		} else if("SalesItemCreationRevised".equals(flujoActual) && "Aseguramiento_de_Calidad".equals(estado)) {
			res[0] = "1022";
			res[1] = "1026";
			res[2] = "true";
		} else if("SalesItemCreationRevised".equals(flujoActual) && "Category_Manager".equals(estado)) {
			res[0] = "1023";
			res[1] = "1022";
			res[2] = "true";
		} else if("SalesItemCreationRevised".equals(flujoActual) && "Rechazos".equals(estado)) {
			res[0] = "1005";
			res[1] = "1022";
			res[2] = "true";
		} else if("SalesItemCreationRevised".equals(flujoActual) && "Revision_Categorizacion".equals(estado)) {
			res[0] = "1026";
			res[1] = "1002";
		} else if("SupplierCreationWF".equals(flujoActual) && "BuyerReview".equals(estado)) {
			res[0] = "1003";
			res[1] = "1001";
		} else if("SupplierCreationWF".equals(flujoActual) && "AssetReviewAndUpload".equals(estado)) {
			res[0] = "1004";
			res[1] = "1020";
		} else if("SupplierCreationWF".equals(flujoActual) && "SupplierRevision".equals(estado)) {
			res[0] = "1004";
			res[1] = "1020";
		} else if("SupplierCreationWF".equals(flujoActual) && ("DigitalAssetsReview".equals(estado) || "ErrorRevision".equals(estado))) {
			res[0] = "1021";
			res[1] = "1020";
		} else if("SupplierCreationWF".equals(flujoActual) && "QAReview".equals(estado)) {
			res[0] = "1022";
			res[1] = "1020";
		} else if("SupplierCreationWF".equals(flujoActual) && "CategoryManager".equals(estado)) {
			res[0] = "1023";
			res[1] = "1022";
		}else {
		}
		return res;
	}
	
	private int vecesQueUnIDYaEstaba = 0;
	private java.util.Set<String> losQueVan = new java.util.HashSet<>();
	private boolean unaVez = true;
	int a = 0;
	
	public void processProductElement(ProductFileProductElement product, java.util.Map<String, String> globalVendorCenterSections, java.util.Map<String, ProductFileAssetElement> productAssetMap) {
		java.util.Set<String> files = idsToFiles.get(product.getId());
		if(files == null) {
			files = new java.util.HashSet<>();
			idsToFiles.put(product.getId(), files);
		}
		files.add(currentWorkingFile);
		a++;
		if(a % 10000 == 0) {
			System.out.print(".");
			if(a % 1000000 == 0) {
				log( String.valueOf( a ));
			}
		}
		java.util.Map<String, ProductFileProductElement> childProducts = null;
		childProducts = product.getProducts();
			if(( "SalesItem".equals(product.getUserTypeId()) && !product.getParentId().matches("^S?[0-9]+") ) || !"SalesItemVariant".equals(product.getUserTypeId())) {
				if( !losQueVan.add(product.getId()) ) {
					return;
				}
				lacuenta++;
				java.util.Map<String, String> assetTypes = null;
				java.util.Map<String, String[]> assetURLs = null;
				org.json.JSONObject jp = null;
				org.json.JSONObject variant = null;
				org.json.JSONArray variants = null;
				java.util.Map<String, String[]> productValues = null;
				assetTypes = product.getAssetCrossReferences();
				assetURLs = getAssetURLs(productAssetMap, assetTypes);
				jp = new org.json.JSONObject();
				jps.put(jp);
				jp.put("proposalId", product.getId());
				jp.put("template", product.getParentId());
				buildMultimediaAssets(assetURLs, jp);
				productValues = toMap(product.getValues());
	    		String[] negocio = productValues.get("Negocio");
	    		String[] extwgS4h = productValues.get("EXTWG_S4H");
				String business = determineBusiness(negocio == null || negocio.length < 3 || negocio[2] == null ? "" : negocio[2], extwgS4h == null || extwgS4h.length < 3 || extwgS4h[2] == null ? "" : extwgS4h[2]);
				String[] direccion = productValues.get("Direction");
				String[] seccion = productValues.get("Direction");
				String[] supplierID = productValues.get("SupplierID");
				jp.put("migrado", 1);
				jp.put("Direction", direccion != null && direccion.length == 3 ? direccion[2] : "");
				jp.put("Section", seccion != null && seccion.length == 3 ? seccion[2] : "");
				jp.put("supplier", supplierID != null && supplierID.length == 3 ? "0".repeat(20 - supplierID[2].length()) + supplierID[2] : "");
				buildSections(globalVendorCenterSections, productValues, jp, false);
				jp.put("Business", business);
				java.util.Map<String, ProductFileValueElement> pvs = product.getValues();
				ProductFileValueElement ve = null;
				String calculatedWFAtt;
				String firstDateApprove;
				String fotoTomadaLiverpool;
				String currentStatus = null;
				String previousStatus = null;
				String externalStatus = null;
				ve = pvs.get("CalculatedWF_Att");
				calculatedWFAtt = ve == null ? "" : ve.getText();
				ve = pvs.get("FirstDateApprove");
				firstDateApprove = ve == null ? "" : "Aprobado";
				ve = pvs.get("FotoTomadaLiverpool");
				fotoTomadaLiverpool = ve == null ? "" : ve.getText();
				String[] statusInfo = computeStatus(calculatedWFAtt, firstDateApprove, fotoTomadaLiverpool, product.getId());
				if(statusInfo[0] != null) {
					org.json.JSONObject p = new org.json.JSONObject();
					org.json.JSONArray v = new org.json.JSONArray();
					v.put(statusInfo[0]);
					v.put(statusInfo[1]);
					v.put( internalToExternalStatusMap.get(statusInfo[0]) );
					v.put(new org.json.JSONArray().put(statusInfo[2]));
					p.put("values", v);
					p.put("object", new org.json.JSONObject().put("id", "'" + product.getId() + "'@1"));
					if(productsCount == 1000) {
						while(products.length() > 0) {
							products.remove(0);
						}
						productsCount = 0;
					}
					jp.put("currentStatus" ,  currentStatus = internalStatusMap.get(statusInfo[0]));
					jp.put("previousStatus", previousStatus = internalStatusMap.get(statusInfo[1]));
					String a = internalToExternalStatusMap.get(statusInfo[0]);
					if(a != null) {
						jp.put("externalStatus", externalStatus = externalStatusMap.get( a ) );
					} else {
						log("This was null... " + statusInfo[0]);
					}
				}
				ve = pvs.get("SKU");
				if(ve != null) {
					jp.put("SKU", ve.getText());
				}
				ve = pvs.get("MainBarCode");
				if(ve != null) {
					jp.put("MainBarCode", ve.getText());
				}
				ve = pvs.get("MainBarCodeS4H");
				if(ve != null) {
					jp.put("MainBarCodeS4H", ve.getText());
				}
				if(!childProducts.isEmpty()) {
					variants = new org.json.JSONArray();
					jp.put("variants", variants);
					for(java.util.Map.Entry<String, ProductFileProductElement> childProduct : childProducts.entrySet()) {
						lacuentaVars++;
						assetURLs = getAssetURLs(productAssetMap, assetTypes);
						variant = new org.json.JSONObject();
						variant.put("variantId", childProduct.getValue().getId());
						variants.put(variant);
						if(currentStatus != null) {
							variant.put("currentStatus", currentStatus);
						}
						if(previousStatus != null) {
							variant.put("previousStatus", previousStatus);
						}
						if(externalStatus != null) {
							variant.put("externalStatus", externalStatus);
						}
						buildImagesAssets(assetURLs, variant);
						productValues = toMap(childProduct.getValue().getValues());
						buildSections(globalVendorCenterSections, productValues, variant, true);
					}
				} else {
					variants = new org.json.JSONArray();
					jp.put("variants", variants);
					variant = new org.json.JSONObject();
					variant.put("variantId", product.getId());
					if(currentStatus != null) {
						variant.put("currentStatus", currentStatus);
					}
					if(previousStatus != null) {
						variant.put("previousStatus", previousStatus);
					}
					if(externalStatus != null) {
						variant.put("externalStatus", externalStatus);
					}
					buildSections(globalVendorCenterSections, productValues, variant, true);
					buildImagesAssets(assetURLs, variant);
					productValues = toMap(product.getValues());
					variants.put(variant);
				}
				gc++;
				if(gc % 500 == 0) {
					pub.publishMessage( body.toString() );
					if(unaVez) {
						log(body.toString());
						unaVez = false;
					}
					while(jps.length() > 0) {
						jps.remove(0);
					}
				}
			}else {
				if(product.getParentId().startsWith("S")) {
					parentsToResend.add(product.getParentId());
				}else {
					try { 
						Integer.parseInt(product.getParentId());
						parentsToResend.add(product.getParentId());
					}catch(NumberFormatException e) {
						variantesSinProductoPadre.add(product.getId());
					}
				}
			}
	}
    
	private String determineBusiness(String negocio, String extwgS4h) {
		return     "".equals(negocio) 
				&& "".equals(extwgS4h) ? null : 
					("".equals(negocio) && !"".equals(extwgS4h) ? "Suburbia": "ART. MARKETPLACE".equals(negocio) ? "Marketplace" : "Liverpool" );
	}
	
	private void buildSections(java.util.Map<String, String> sections, java.util.Map<String, String[]> productValues, org.json.JSONObject product, boolean includeProducto) {
		String[] data = null;
		org.json.JSONObject section = null;
		for(java.util.Map.Entry<String, String> entry : sections.entrySet()) {
			if((includeProducto && "producto".equals(entry.getValue())) || (includeProducto && "header".equals(entry.getValue())) || (!includeProducto && !"producto".equals(entry.getValue()))) {
				data = productValues.get(entry.getKey());
				if(data != null) {
					if(!includeProducto) {
						if(!product.has(entry.getValue())) {
							section = new org.json.JSONObject();
							product.put(entry.getValue(), section);
						} else {
							section = product.getJSONObject(entry.getValue());
						}
						section.put(entry.getKey(), data[2]);
					} else {
						product.put(entry.getKey(), data[2]);
					}
				}
			}
		}
	}
	
	private void buildImagesAssets(java.util.Map<String, String[]> assetURLs, org.json.JSONObject product) {
		org.json.JSONArray details = new org.json.JSONArray();
		org.json.JSONArray illustrations = new org.json.JSONArray();
		org.json.JSONArray smoshes = new org.json.JSONArray();
		for(java.util.Map.Entry<String, String[]> entry : assetURLs.entrySet()) {
			if( /* "ProductImage" */ "PrimaryProductImage".equals(entry.getValue()[0])) {
				product.put("productImage", entry.getValue()[1]);
			}else if( /* "ProductImageDetail" */ "ProductImage".equals(entry.getValue()[0])) {
				details.put(entry.getValue()[1]);
			}else if("Illustration".equals(entry.getValue()[0])) {
				illustrations.put(entry.getValue()[1]);
			}else if("ProductImageSmosh".equals(entry.getValue()[0])) {
				smoshes.put(entry.getValue()[1]);
			}
		}
		if(details.length() > 0) {
			product.put("detailImage", details);
		}
		if(illustrations.length() > 0) {
			product.put("illustration", illustrations);
		}
		if(smoshes.length() > 0) {
			product.put("smoshImage", smoshes);
		}
	}
	
	private void buildMultimediaAssets(java.util.Map<String, String[]> assetURLs, org.json.JSONObject product) {
		org.json.JSONArray ownersManual = new org.json.JSONArray();
		org.json.JSONArray liverpoolManual = new org.json.JSONArray();
		org.json.JSONArray noms = new org.json.JSONArray();
		for(java.util.Map.Entry<String, String[]> entry : assetURLs.entrySet()) {
			if("LiverpoolManual".equals(entry.getValue()[0])) {
				liverpoolManual.put(entry.getValue()[1]);
			}else if("OwnersManual".equals(entry.getValue()[0])) {
				ownersManual.put(entry.getValue()[1]);
			}
		}
		if(liverpoolManual.length() > 0) {
			product.put("liverpoolManual", liverpoolManual);
		}
		if(ownersManual.length() > 0) {
			product.put("ownersManual", ownersManual);
		}
		if(noms.length() > 0) {
			product.put("nom", noms);
		}
	}
	
	private java.util.Map<String, String[]> getAssetURLs(java.util.Map<String, ProductFileAssetElement> productAssetMap, java.util.Map<String, String> assetTypesMap) {
		java.util.Map<String, String[]> urls = new java.util.HashMap<>();
		ProductFileAssetElement assetElement = null;
		java.util.Map<String, String[]> assetValues = null;
		String imageURL = null;
		for(java.util.Map.Entry<String, String> assetType : assetTypesMap.entrySet()) {
			assetElement = productAssetMap.get(assetType.getKey());
			if(assetElement != null) {
				assetValues = toMap( assetElement.getValues() );
				if(assetValues == null) {
					log("No assets found for " + assetType.getKey());
				}else {
					String[] data = assetValues.get("ImageURL");
					imageURL = data != null ? data[2] : "";
					if("".equals(imageURL)) {
						data = assetValues.get("PdfURL");
						imageURL = data != null ? data[2] : "";
					}else {
						imageURL = getImageURL(imageURL);
					}
					urls.put(assetType.getKey(), new String[] { assetType.getValue(), imageURL });
				}
			}
		}
		return urls;
	}
	
	private String getImageURL(String url) {
		if(url != null) {
			java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\=([^,\\=]+).+").matcher(url.trim());
			if(m.find()) {
				return m.group(1);
			}
		}
		return url;
	}
	
	private java.util.Map<String, String[]> toMap(java.util.Map<String, ProductFileValueElement> values){
		java.util.Map<String, String[]> data = new java.util.HashMap<>();
		for(java.util.Map.Entry<String, ProductFileValueElement> value : values.entrySet()) {
			data.put(value.getKey(), new String[] { value.getValue().getId(), value.getValue().getUnidadId(), value.getValue().getText() });
		}
		return data;
	}
	
	private static java.util.Map<String, String> loadInternalStatusMap(){
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONObject row = null;
		java.util.Map<String, String> statusEnum = new java.util.TreeMap<>();
		RESTWrapper rw = new RESTWrapper();
		try {
			response = rw.getRw().makeRequest("GET", "/enum/Enum.Status", qp, null);
			rows = response.getJSONArray("entries");
			for (int i = 0; i < rows.length(); i++) {
				row = rows.getJSONObject(i);
				statusEnum.put(row.getString("key"), row.getString("label"));
			}
		} catch (org.json.JSONException e) {
		}
		return statusEnum;
	}
	
	private static java.util.Map<String, String> loadExternalStatusMap(){
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		java.util.Map<String, String> statusEnum = new java.util.TreeMap<>();
		RESTWrapper rw = new RESTWrapper();
		qp.put("lookup", "'ExternalStatus'");
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		rw.collectData("list", "LookupValue", null, "byLookup", qp, row -> statusEnum.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1)));
		return statusEnum;
	}
	
	private static java.util.Map<String, String> loadInternalToExternalStatusMap(){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StandardizationValue.Value,StandardizationValue.AlternativeValue");
		qp.put("dictionary", "ExternalStatus");
		java.util.Map<String, String> data = new java.util.HashMap<>();
		RESTWrapper rw = new RESTWrapper();
		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> data.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1)));
		return data;
	}
	
	private static java.util.Map<String, String> getMeTemplateData() {
		RESTWrapper rw = new RESTWrapper();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("dictionaryProxy", "'GlobalTemplateAttributeConfiguration'");
		qp.put("query", "StandardizationValue.Property->LookupValue.Code = \"SentToVendorCenter\" and StandardizationValue.PropertyValue = \"1\" and StandardizationValue.Dictionary->StandardizationDictionary.Identifier = \"GlobalTemplateAttributeConfiguration\"");
		qp.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier");
		qp.put("pageSize", "25000");
		java.util.Set<String> globalSendToVendorCenter = new java.util.TreeSet<>();
		java.util.Map<String, String> globalVendorCenterSections = new java.util.TreeMap<>();
		java.util.Set<String> localSendToVendorCenter = new java.util.TreeSet<>();
		java.util.Map<String, String> localVendorCenterSections = new java.util.HashMap<>();
		rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
			globalSendToVendorCenter.add(row.getJSONArray("values").getString(0));
		});
		qp.put("query",  "StandardizationValue.Property->LookupValue.Code = \"VendorCenterSection\" and StandardizationValue.Dictionary->StandardizationDictionary.Identifier = \"GlobalTemplateAttributeConfiguration\"");
		qp.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.PropertyValue");
		rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			globalVendorCenterSections.put(values.getString(0), values.getString(1));
		});
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("query", "StandardizationValue.Property->LookupValue.Code = \"SentToVendorCenter\" and StandardizationValue.PropertyValue = \"1\" and StandardizationValue.Dictionary->StandardizationDictionary.Identifier = \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\"");
		qp.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier");
		rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
			localSendToVendorCenter.add(row.getJSONArray("values").getString(0));
		});
		qp.put("query",  "StandardizationValue.Property->LookupValue.Code = \"VendorCenterSection\" and StandardizationValue.Dictionary->StandardizationDictionary.Identifier = \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\"");
		qp.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.PropertyValue");
		rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			localVendorCenterSections.put(values.getString(0), values.getString(1));
		});
		qp.clear();
		qp.put("dictionary", "SeccionesEntradaUnicaCatalogacion");
		qp.put("fields",     "StandardizationValue.Value,StandardizationValue.AlternativeValue");
		java.util.Map<String, String> sectionInterpretation = new java.util.HashMap<>();
		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			sectionInterpretation.put(values.getString(0), values.getString(1));
		});
		String t = null;
		for(java.util.Map.Entry<String, String> entry : globalVendorCenterSections.entrySet()) {
			t = sectionInterpretation.get(entry.getValue());
			if(t != null && !"".equals(t)) {
				globalVendorCenterSections.put(entry.getKey(), t);
			}else {
				System.out.println("--->" + entry.getValue());
			}
		}
		t = null;
		for(java.util.Map.Entry<String, String> entry : localVendorCenterSections.entrySet()) {
			t = sectionInterpretation.get(entry.getValue());
			if(t != null && !"".equals(t)) {
				globalVendorCenterSections.put(entry.getKey(), t);
			}else {
				System.out.println("--->" + entry.getValue());
			}
		}
		return globalVendorCenterSections;
	}
	
	private synchronized void log(String message) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/pruebaEnvioPubSubMediaAssets.log", true)))) {
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date()))
					+ "] " + message);
		} catch (java.io.IOException e) {
		}
	}

	private synchronized void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/pruebaEnvioPubSubMediaAssets.log", true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
	
//	public static void main(String[] args) {
//		PruebaEnvioPubSubMediaAssets a = new PruebaEnvioPubSubMediaAssets();
//		String b = "Flujo Actual: N/A|Estado en el WF: N/A|Ingreso al estado actual desde: 2025-02-06 19:13:32|Última fecha de modificación: 2025-02-06 19:13:32|Versión actual: 0.14|Detalle: Auto Generated|Última modificación por: RCLEONC";
//		String c = "Aprobado";
//		String d = "";
//		String e = "S55702499";
//		System.out.println( java.util.Arrays.asList( a.computeStatus(b, c, d, e) ) );
//	}
	
	public static void process(String content) {
		long init = System.currentTimeMillis();
		PruebaEnvioPubSubMediaAssets r = new PruebaEnvioPubSubMediaAssets();
		r.processFile(content);
		if(r.jps.length() > 0) {
			r.pub.publishMessage( r.body.toString() );
			while(r.jps.length() > 0) {
				r.jps.remove(0);
			}
			r.log("Veces que estuvo un id de genérico, individual o variante perdida: " + r.vecesQueUnIDYaEstaba);
			r.log("\n\n\tDone. " + r.gc);
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("/", "u01", "workshop", "land", "padres_a_rebuscar_" + new java.text.SimpleDateFormat("yyyyMMddHHmmss.SSS").format(new java.util.Date())).toFile())))){
				java.util.Set<String> archivos = null;
				java.util.List<String> els = null;
				for(String missingFada : r.parentsToResend) {
					archivos = r.idsToFiles.get(missingFada);
					String[] chunk = new String[archivos == null ? 2 : archivos.size() + 1];
					chunk[0] = missingFada;
					if(archivos == null) {
						chunk[1] = "";
					}else {
						els = new java.util.ArrayList<>(archivos);
						for(int i=1; i<archivos.size(); i++) {
							chunk[i] = els.get(i-0);
						}
					}
					pw.println( r.rw.getRw().serializeChunk( chunk ) );
				}
			}catch(java.io.IOException e) {
				r.logE(e);
			}
		}
		r.log("Done. " + r.rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.io.File[] files = new java.io.File(args[0]).listFiles();
		PruebaEnvioPubSubMediaAssets r = new PruebaEnvioPubSubMediaAssets();
		Thread t = new Thread(new Runnable(){
			@Override 
			public void run() {
				while(r.running) {
					try {
						Thread.sleep(600000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					r.eleseProgress();
				}
				System.out.println("Exiting.");
			}
		});
		t.setDaemon(true);
		t.start();
		r.processFiles(files);
		if(r.jps.length() > 0) {
			r.pub.publishMessage( r.body.toString() );
			while(r.jps.length() > 0) {
				r.jps.remove(0);
			}
			r.log("Veces que estuvo un id de genérico, individual o variante perdida: " + r.vecesQueUnIDYaEstaba);
//			log("IDs de product o individual que no estaban: " + r.noestaban.size());
//			log("IDs de variante que no estaban: " + r.noestabanv.size());
			r.log("\n\n\tDone. " + r.gc);
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("/", "u01", "workshop", "land", "padres_a_rebuscar").toFile())))){
				java.util.Set<String> archivos = null;
				java.util.List<String> els = null;
				for(String missingFada : r.parentsToResend) {
					archivos = r.idsToFiles.get(missingFada);
					String[] chunk = new String[archivos == null ? 2 : archivos.size() + 1];
					chunk[0] = missingFada;
					if(archivos == null) {
						chunk[1] = "";
					}else {
						els = new java.util.ArrayList<>(archivos);
						for(int i=1; i<archivos.size(); i++) {
							chunk[i] = els.get(i-0);
						}
					}
					pw.println( r.rw.getRw().serializeChunk( chunk ) );
				}
			}catch(java.io.IOException e) {
				r.logE(e);
			}
		}
		r.running = false;
		r.eleseFinish();
		r.log("Done. " + r.rw.getRw().formatTime(System.currentTimeMillis() - init));
	}

}
