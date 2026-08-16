package mx.com.liverpool.dataprofiling.preparison.envioproductos;

import org.xml.sax.helpers.DefaultHandler;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.xml.ProcessXMLFiles;
import mx.com.liverpool.p360.services.core.xml.ProductFileAssetElement;
import mx.com.liverpool.p360.services.core.xml.ProductFileClassificationElement;
import mx.com.liverpool.p360.services.core.xml.ProductFileHandler;
import mx.com.liverpool.p360.services.core.xml.ProductFileMultiValueElement;
import mx.com.liverpool.p360.services.core.xml.ProductFileProductElement;
import mx.com.liverpool.p360.services.core.xml.ProductFileValueElement;

public class PruebaEnvioPubSubMediaAssets extends ProcessXMLFiles {
	
    private static final long REFERENCE_DATA_TTL_MILLIS = 15L * 60L * 1000L;
    private static final Object REFERENCE_DATA_LOCK = new Object();
    private static volatile ReferenceData cachedReferenceData;

	private final RESTWrapper rw = new RESTWrapper();
	private final java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?<=Flujo Actual: )([^|]+)");
	private final java.util.regex.Pattern p0 = java.util.regex.Pattern.compile("(?<=Estado en el WF: )([^|]+)");
	private final java.util.Set<String> parentsToResend = new java.util.TreeSet<>();
	private final org.json.JSONArray products = new org.json.JSONArray();
    private final ReferenceData referenceData = getReferenceData();
	private final java.util.Map<String, String> internalStatusMap = referenceData.internalStatusMap;
	private final java.util.Map<String, String> externalStatusMap = referenceData.externalStatusMap;
	private final java.util.Map<String, String> internalToExternalStatusMap = referenceData.internalToExternalStatusMap;
    private final java.util.Map<String, String> globalVendorCenterSections = referenceData.vendorCenterSections;
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

    private static final PubSubGCP PUB = new PubSubGCP(
            PropertiesManager.get("p360.contingency.gcp.service_account_back"),
            PropertiesManager.get("p360.contingency.gcp.project_back"),
            PropertiesManager.get("p360.contingency.gcp.post_products_topic"));
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
					PUB.publishMessage( body.toString() );
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
		/*
		 * Esta es la única lectura de referencia que todavía usa la REST API porque
		 * DBAccessDataStub aún no tiene equivalente para Enum.Status. Al quedar bajo
		 * el cache TTL ya no se ejecuta por cada STEP.
		 */
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
		} catch (Exception e) {
            logStatic(e);
		}
		return statusEnum;
	}

    private static ReferenceData getReferenceData() {
        long now = System.currentTimeMillis();
        ReferenceData current = cachedReferenceData;
        if (current != null && now - current.loadedAt < REFERENCE_DATA_TTL_MILLIS) {
            return current;
        }

        synchronized (REFERENCE_DATA_LOCK) {
            now = System.currentTimeMillis();
            current = cachedReferenceData;
            if (current != null && now - current.loadedAt < REFERENCE_DATA_TTL_MILLIS) {
                return current;
            }

            java.util.Map<String, String> internalStatus = loadInternalStatusMap();
            java.util.Map<String, String> externalStatus = new java.util.TreeMap<>();
            java.util.Map<String, String> internalToExternal = new java.util.HashMap<>();
            java.util.Map<String, String> vendorCenterSections = new java.util.TreeMap<>();

            mx.com.liverpool.p360.services.core.ELog elog = new mx.com.liverpool.p360.services.core.ELog() {
                @Override
                public void logE(Exception e) {
                    logStatic(e);
                }

                @Override
                public void log(String message) {
                    logStatic(message);
                }
            };

            try(mx.com.liverpool.p360.services.core.DBAccessDataStub dastub = new mx.com.liverpool.p360.services.core.DBAccessDataStub(elog)){
	            try {
	                externalStatus.putAll(dastub.getLookupValueCodeNameMap("ExternalStatus", 10, false));
	                internalToExternal.putAll(dastub.getDictionaryValueAlternativeValueMap("ExternalStatus"));
	                vendorCenterSections.putAll(loadVendorCenterSections(dastub));
	            } catch (Exception e) {
	                logStatic(e);
	            }
            }

            /* Si una recarga falla, conserva la última copia válida por componente. */
            if (current != null) {
                if (internalStatus.isEmpty()) {
                    internalStatus = current.internalStatusMap;
                }
                if (externalStatus.isEmpty()) {
                    externalStatus = current.externalStatusMap;
                }
                if (internalToExternal.isEmpty()) {
                    internalToExternal = current.internalToExternalStatusMap;
                }
                if (vendorCenterSections.isEmpty()) {
                    vendorCenterSections = current.vendorCenterSections;
                }
            }

            ReferenceData loaded = new ReferenceData(
                    immutableCopy(internalStatus),
                    immutableCopy(externalStatus),
                    immutableCopy(internalToExternal),
                    immutableCopy(vendorCenterSections),
                    now);
            cachedReferenceData = loaded;
            return loaded;
        }
    }

    private static java.util.Map<String, String> loadVendorCenterSections(
            mx.com.liverpool.p360.services.core.DBAccessDataStub dastub) {

        java.util.Map<String, String> result = new java.util.TreeMap<>();
        java.util.Map<String, String> sectionInterpretation =
                dastub.getDictionaryValueAlternativeValueMap("SeccionesEntradaUnicaCatalogacion");

        org.json.JSONObject globalMetadata = dastub.getGlobalMetadata("CreateProposal");
        String characteristic = null;
        for (Object characteristicO : globalMetadata.keySet()) {
        	if(characteristicO instanceof String) {
        		characteristic = (String) characteristicO;
	            org.json.JSONObject properties = globalMetadata.optJSONObject(characteristic);
	            if (properties == null) {
	                continue;
	            }
	            String rawSection = properties.optString("VendorCenterSection", "");
	            if (rawSection == null || rawSection.isBlank()) {
	                continue;
	            }
	            String interpreted = sectionInterpretation.get(rawSection);
	            result.put(characteristic,
	                    interpreted == null || interpreted.isBlank() ? rawSection : interpreted);
        	}
        }

        /*
         * Mantiene la precedencia del código anterior: la metadata local pisa la
         * global solamente cuando su sección puede interpretarse.
         */
        java.util.Map<String, String> localSections = dastub.getVendorCenterSectionOverrides();
        for (java.util.Map.Entry<String, String> entry : localSections.entrySet()) {
            String interpreted = sectionInterpretation.get(entry.getValue());
            if (interpreted != null && !interpreted.isBlank()) {
                result.put(entry.getKey(), interpreted);
            }
        }

        return result;
    }

    private static java.util.Map<String, String> immutableCopy(java.util.Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return java.util.Collections.unmodifiableMap(new java.util.HashMap<>(source));
    }

    private static final class ReferenceData {
        private final java.util.Map<String, String> internalStatusMap;
        private final java.util.Map<String, String> externalStatusMap;
        private final java.util.Map<String, String> internalToExternalStatusMap;
        private final java.util.Map<String, String> vendorCenterSections;
        private final long loadedAt;

        private ReferenceData(
                java.util.Map<String, String> internalStatusMap,
                java.util.Map<String, String> externalStatusMap,
                java.util.Map<String, String> internalToExternalStatusMap,
                java.util.Map<String, String> vendorCenterSections,
                long loadedAt) {
            this.internalStatusMap = internalStatusMap;
            this.externalStatusMap = externalStatusMap;
            this.internalToExternalStatusMap = internalToExternalStatusMap;
            this.vendorCenterSections = vendorCenterSections;
            this.loadedAt = loadedAt;
        }
    }

    private static void logStatic(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/pruebaEnvioPubSubMediaAssets.log", true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date()))
                    + "] " + message);
        } catch (java.io.IOException ignored) {
        }
    }

    private static void logStatic(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/pruebaEnvioPubSubMediaAssets.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException ignored) {
        }
    }

    public static void closeSharedPublisher() {
        PUB.close();
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
	
    /**
     * Streaming Path entry point used by ReceiveSTEPFile / NeoSTEPFileProcessor.
     *
     * Two SAX passes are intentional:
     *   1) read only Asset definitions, because an Asset may be declared after
     *      a Product that references it;
     *   2) stream one root Product tree at a time and process it immediately.
     *
     * The STEP is never materialized as one String and the Product roots are not
     * accumulated in ProductFileHandler.finished.
     */
    public static void process(java.nio.file.Path path) {
        long init = System.currentTimeMillis();
        PruebaEnvioPubSubMediaAssets r = new PruebaEnvioPubSubMediaAssets();
        try {
            java.util.Map<String, ProductFileAssetElement> assetMap =
                    readAssetMapStreaming(path);
            r.processProductsStreaming(path, assetMap);
            finishProcess(r);
            r.log("Done. " + r.rw.getRw().formatTime(System.currentTimeMillis() - init));
        } catch (Exception e) {
            r.logE(e);
            throw new IllegalStateException("Error processing STEP MediaAssets: " + path, e);
        }
    }

    private static javax.xml.parsers.SAXParser newSafeSaxParser()
            throws javax.xml.parsers.ParserConfigurationException, org.xml.sax.SAXException {
        javax.xml.parsers.SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {
        }
        return factory.newSAXParser();
    }

    private static java.util.Map<String, ProductFileAssetElement> readAssetMapStreaming(
            java.nio.file.Path path) throws Exception {

        java.util.Map<String, ProductFileAssetElement> assetMap = new java.util.TreeMap<>();
        javax.xml.parsers.SAXParser parser = newSafeSaxParser();

        parser.parse(path.toFile(), new org.xml.sax.helpers.DefaultHandler() {
            private final java.util.LinkedList<ProductFileAssetElement> assetStack =
                    new java.util.LinkedList<>();
            private boolean assetName;

            @Override
            public void startElement(
                    String uri,
                    String localName,
                    String qName,
                    org.xml.sax.Attributes attributes) {

                String name = localName != null && !localName.isEmpty() ? localName : qName;

                if ("Asset".equals(name)) {
                    assetStack.addLast(new ProductFileAssetElement(
                            attributes.getValue("ID"),
                            attributes.getValue("UserTypeID")));
                    return;
                }

                if (assetStack.isEmpty()) {
                    return;
                }

                ProductFileAssetElement asset = assetStack.getLast();
                if ("Name".equals(name)) {
                    assetName = true;
                } else if ("Value".equals(name)) {
                    asset.setCurrentValue(new ProductFileValueElement(
                            attributes.getValue("AttributeID"), null, null));
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) {
                if (assetStack.isEmpty()) {
                    return;
                }

                ProductFileAssetElement asset = assetStack.getLast();
                ProductFileValueElement value = asset.getCurrentValue();
                if (value != null) {
                    String old = value.getText();
                    StringBuilder sb = new StringBuilder(old == null ? "" : old);
                    sb.append(ch, start, length);
                    value.setText(sb.toString());
                } else if (assetName) {
                    StringBuilder sb = new StringBuilder(
                            asset.getName() == null ? "" : asset.getName());
                    sb.append(ch, start, length);
                    asset.setName(sb.toString());
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                if (assetStack.isEmpty()) {
                    return;
                }

                String name = localName != null && !localName.isEmpty() ? localName : qName;
                ProductFileAssetElement asset = assetStack.getLast();

                if ("Value".equals(name)) {
                    asset.addValue();
                } else if ("Name".equals(name)) {
                    assetName = false;
                } else if ("Asset".equals(name)) {
                    assetStack.removeLast();
                    if (asset.getId() != null) {
                        assetMap.put(asset.getId(), asset);
                    }
                }
            }
        });

        return assetMap;
    }

    private void processProductsStreaming(
            java.nio.file.Path path,
            java.util.Map<String, ProductFileAssetElement> assetMap) throws Exception {

        currentWorkingFile = path.toAbsolutePath().toString();
        javax.xml.parsers.SAXParser parser = newSafeSaxParser();

        parser.parse(path.toFile(), new org.xml.sax.helpers.DefaultHandler() {
            private final java.util.LinkedList<ProductFileProductElement> productStack =
                    new java.util.LinkedList<>();
            private final java.util.LinkedList<String> elementStack =
                    new java.util.LinkedList<>();
            private final java.util.Set<ProductFileProductElement> valuesOpen =
                    java.util.Collections.newSetFromMap(
                            new java.util.IdentityHashMap<ProductFileProductElement, Boolean>());

            @Override
            public void startElement(
                    String uri,
                    String localName,
                    String qName,
                    org.xml.sax.Attributes attributes) {

                String name = localName != null && !localName.isEmpty() ? localName : qName;
                String parentTag = elementStack.isEmpty() ? null : elementStack.getLast();

                if ("Product".equals(name)) {
                    String parentId = attributes.getValue("ParentID");
                    if (parentId == null && !productStack.isEmpty()) {
                        parentId = productStack.getLast().getId();
                    }

                    ProductFileProductElement product = new ProductFileProductElement(
                            attributes.getValue("ID"),
                            parentId,
                            attributes.getValue("UserTypeID"));
                    /* Avoid null maps even for malformed/minimal Products. */
                    product.createList();
                    product.createMultiValueList();
                    productStack.addLast(product);

                } else if (!productStack.isEmpty()) {
                    ProductFileProductElement product = productStack.getLast();

                    /* Only the Product's direct <Values> block owns Product values. */
                    if ("Values".equals(name) && "Product".equals(parentTag)) {
                        valuesOpen.add(product);

                    } else if ("Value".equals(name) && valuesOpen.contains(product)) {
                        product.prepareValue(new ProductFileValueElement(
                                attributes.getValue("AttributeID"),
                                attributes.getValue("ID"),
                                attributes.getValue("UnitID")));

                    } else if ("MultiValue".equals(name) && valuesOpen.contains(product)) {
                        product.prepareMultiValue(new ProductFileMultiValueElement(
                                attributes.getValue("AttributeID")));

                    } else if ("ClassificationReference".equals(name)) {
                        product.prepareClassification(new ProductFileClassificationElement(
                                attributes.getValue("ClassificationID"),
                                attributes.getValue("Type")));

                    } else if ("AssetCrossReference".equals(name)) {
                        product.putAssetCrossReference(
                                attributes.getValue("AssetID"),
                                attributes.getValue("Type"));
                    }
                }

                elementStack.addLast(name);
            }

            @Override
            public void characters(char[] ch, int start, int length) {
                if (productStack.isEmpty()) {
                    return;
                }

                ProductFileValueElement value = productStack.getLast().getWorkingValue();
                if (value != null) {
                    StringBuilder sb = new StringBuilder(
                            value.getText() == null ? "" : value.getText());
                    sb.append(ch, start, length);
                    value.setText(sb.toString());
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) {
                String name = localName != null && !localName.isEmpty() ? localName : qName;

                if (!productStack.isEmpty()) {
                    ProductFileProductElement product = productStack.getLast();

                    if ("Value".equals(name) && product.getWorkingValue() != null) {
                        product.addValue();

                    } else if ("MultiValue".equals(name)
                            && product.getWorkingMultiValue() != null) {
                        product.addMultiValue();

                    } else if ("ClassificationReference".equals(name)
                            && product.getWorkingClassification() != null) {
                        product.addClassification();

                    } else if ("Values".equals(name) && valuesOpen.contains(product)) {
                        if (product.getWorkingMultiValue() != null) {
                            product.addMultiValue();
                        } else if (product.getWorkingValue() != null) {
                            product.addValue();
                        }
                        valuesOpen.remove(product);

                    } else if ("Product".equals(name)) {
                        if (product.getWorkingMultiValue() != null) {
                            product.addMultiValue();
                        } else if (product.getWorkingValue() != null) {
                            product.addValue();
                        }
                        if (product.getWorkingClassification() != null) {
                            product.addClassification();
                        }

                        valuesOpen.remove(product);
                        productStack.removeLast();

                        if (productStack.isEmpty()) {
                            processProductElement(
                                    product,
                                    globalVendorCenterSections,
                                    assetMap);
                        } else {
                            productStack.getLast().addProduct(product);
                        }
                    }
                }

                if (!elementStack.isEmpty()) {
                    elementStack.removeLast();
                }
            }
        });
    }

	public static void process(String content) {
		long init = System.currentTimeMillis();
		PruebaEnvioPubSubMediaAssets r = new PruebaEnvioPubSubMediaAssets();
		r.processFile(content);
        finishProcess(r);
		r.log("Done. " + r.rw.getRw().formatTime(System.currentTimeMillis() - init));
	}

    private static void finishProcess(PruebaEnvioPubSubMediaAssets r) {
        if (r.jps.length() > 0) {
            PUB.publishMessage(r.body.toString());
            while (r.jps.length() > 0) {
                r.jps.remove(0);
            }
            r.log("Veces que estuvo un id de genérico, individual o variante perdida: " + r.vecesQueUnIDYaEstaba);
            r.log("\n\n\tDone. " + r.gc);
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                    new java.io.FileOutputStream(java.nio.file.Paths.get(
                            "/", "u01", "workshop", "land",
                            "padres_a_rebuscar_" + new java.text.SimpleDateFormat("yyyyMMddHHmmss.SSS")
                                    .format(new java.util.Date())).toFile())))) {
                for (String missingFada : r.parentsToResend) {
                    java.util.Set<String> archivos = r.idsToFiles.get(missingFada);
                    String[] chunk = new String[archivos == null ? 2 : archivos.size() + 1];
                    chunk[0] = missingFada;
                    if (archivos == null) {
                        chunk[1] = "";
                    } else {
                        java.util.List<String> els = new java.util.ArrayList<>(archivos);
                        for (int i = 1; i < chunk.length; i++) {
                            chunk[i] = els.get(i - 1);
                        }
                    }
                    pw.println(r.rw.getRw().serializeChunk(chunk));
                }
            } catch (java.io.IOException e) {
                r.logE(e);
            }
        }
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
			PUB.publishMessage( r.body.toString() );
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
