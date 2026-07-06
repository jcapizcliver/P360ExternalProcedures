package mx.com.liverpool.p360.services.core.temp.exports;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RestClient;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.core.temp.move.utils.GeneralOperations;

public class Hierarchy {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final String baseUrlDEV = rw.getRw().getBaseUrl();
	private static final RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: " + rw.getRw().getRc().getHeader().get("Authorization"));

	private final String outputXmlFile = java.nio.file.Paths.get("C:","opt","LVP","tmp", System.currentTimeMillis() + "_pépeleJairarqui.xml").toString();
	
	public static void main(String[] args) {
		Hierarchy h = new Hierarchy();
		try {
			h.createHierarchyFile();
		} catch (ServiceUnavailableException e) {
			e.printStackTrace();
		}
	}
	
	private void createHierarchyFile() throws ServiceUnavailableException {
		long init = System.currentTimeMillis();

//		java.util.Map<String, java.util.LinkedList<String>> data = collectCharacteristicAttributeGroups();
//		java.util.LinkedList<String> attributeGroups = null;
//		data.forEach((k,v)->System.out.println(k + " - " + v));
//		System.exit(0);
		String structureSystem = "PrimaryProductTaxonomy";
		GeneralOperations go = new GeneralOperations();
		Element attributeList = null;
		Element lookupValuesList = null;
		java.util.Set<String> processedFields = new java.util.TreeSet<>();
		java.util.Set<String> processedLookups = new java.util.TreeSet<>();
		java.util.Map<String, java.util.Map<String, String>> lookupContents = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, org.json.JSONObject>> ostrasGlobales = getAttributes(baseUrlDEV);
		java.util.Map<String, String> atgGroups = go.collectLookupValueData(rw.getRw(), "ATGAttributeGroups");
		
		java.util.LinkedList<org.json.JSONObject> rescataLaRaiz = new java.util.LinkedList<>();
		java.util.Map<String, org.json.JSONObject> elementosDeLaJerarquía = precargaJerarquia(structureSystem, rescataLaRaiz); // precargaJerarquiasWeb("Sitios Web", rescataLaRaiz);
		org.json.JSONObject entry = null;
		org.json.JSONObject entryHelper = null;
        try {

        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder builder = factory.newDocumentBuilder();
        	Document doc = builder.newDocument();

        	attributeList = doc.createElement("AttributeList");
        	lookupValuesList = doc.createElement("ListsOfValues");
        	System.out.println("Building global lists for attributes and lookups");
        	for(java.util.Map.Entry<String, java.util.Map<String, org.json.JSONObject>> ostraEntry : ostrasGlobales.entrySet()) {
	        	collectAttributeDefinitions(baseUrlDEV, ostraEntry.getValue(), atgGroups, processedFields, attributeList, doc);
	        	collectLookupDefinitions(   baseUrlDEV, ostraEntry.getValue(), processedLookups, lookupValuesList, doc, lookupContents);
        	}
        	Element spim = doc.createElement("STEP-ProductInformation");
        	spim.setAttribute("ExportTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( new java.util.Date() ));
        	spim.setAttribute("ExportContext", "Context2");
        	spim.setAttribute("ContextID", "Context2");
        	spim.setAttribute("WorkspaceID", "Approved");
        	spim.setAttribute("UseContextLocale", "false");

        	doc.appendChild(spim);
        	spim.appendChild(lookupValuesList);
        	spim.appendChild(attributeList);

        	String[] webCategory = null;
        	log("Reading templates...");
        	webCategory = bringThemAll(structureSystem).toArray(new String[] {});
        	log("Ready. " + webCategory.length);
        	Element webHierarchyRoot = doc.createElement("Products");
        	spim.appendChild(webHierarchyRoot);
        	Element product0 = armaNivelCero(baseUrlDEV, doc, lookupContents);
        	webHierarchyRoot.appendChild(product0);
        	Element helperElement = null;
        	Element prevHelperElement = null;
        	java.util.Map<String, Element> tableroDeControl = new java.util.TreeMap<>();

        	for (String element : webCategory) {
    			entry = elementosDeLaJerarquía.get(element);
    			entryHelper = entry;
    			helperElement = pacheleMiCompa(entryHelper, ostrasGlobales, lookupContents, doc);
				tableroDeControl.put(entryHelper.getString("identifier"), helperElement);
    			while(entryHelper.has("parentIdentifier") && !"".equals(entryHelper.get("parentIdentifier"))) {
    				prevHelperElement = helperElement;
    				entryHelper = elementosDeLaJerarquía.get(entryHelper.getString("parentIdentifier"));
    				if(!tableroDeControl.containsKey(entryHelper.getString("identifier"))) {
    					helperElement = pacheleMiCompa(entryHelper, ostrasGlobales, lookupContents, doc);
    					if(helperElement == null) {
    						log("PANIC: No element could be made from: " + entryHelper);
    						break;
    					}
    					tableroDeControl.put(entryHelper.getString("identifier"), helperElement);
    				}else {
    					helperElement = tableroDeControl.get(entryHelper.getString("identifier"));
    				}
    				helperElement.appendChild(prevHelperElement);
    			}
        	}

        	for(java.util.Map.Entry<String, Element> entryElement : tableroDeControl.entrySet()) {
        		if(entryElement.getKey().startsWith("EU1-")) {
	        		helperElement = entryElement.getValue();
	        		if(helperElement != null) {
	        			product0.appendChild(helperElement); // webHierarchyRoot.appendChild(helperElement);
	        		}
        		}
        	}
        	TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            java.io.StringWriter writer = new java.io.StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            transformer.transform(new DOMSource(doc), new StreamResult(new java.io.File(outputXmlFile)));
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		log("Done. " + new RESTWorkshop().formatTime(System.currentTimeMillis() - init));
	}

	private java.util.LinkedList<String> bringThemAll(String structure){
		java.util.LinkedList<String> losEsos = new java.util.LinkedList<>();
		RESTWorkshop rw = Hierarchy.rw.getRw();
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("structure", structure);
		qp.put("query", "StructureGroup.Identifier wildcard \"EU4-%\"");
		qp.put("fields", "StructureGroup.Identifier");
		qp.put("pageSize", "10000");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					losEsos.addLast(values.getString(0));
				}
			}else {
				log("ERR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		return losEsos;
	}

	private Element pacheleMiCompa(JSONObject node, java.util.Map<String, java.util.Map<String, org.json.JSONObject>> ostras, java.util.Map<String, java.util.Map<String, String>> lookupContents, Document doc) {
		Element name = null;
		Element value = null;
		Element values = null;
		Element multiValue = null;
		Element attributeLinkElement = null;
		Element metaDataElement = null;
		Element valueElement = null;
		Element valueFilterElement = null;
		Element dependentAttributeElement = null;
		String aux = null;
		org.json.JSONObject ob = null;
		org.json.JSONArray gpfs = null;
		org.json.JSONObject gpf = null;
		String filter = null;
		String[] pieces = null;
		String piece = null;
		java.util.Map<String, String> lookupContent = null;
		java.util.Map<String, org.json.JSONObject> ostra = null;
		String dependentAttribute = null;
		org.json.JSONObject dependentAttributeContent = null;
        if (node != null) {
        	values = doc.createElement("Values");
        	name = doc.createElement("Name");
            Element productElement = doc.createElement("Product");
            productElement.appendChild(name);
            productElement.appendChild(values);
            if (node.has("name_es")) {
            	name.setTextContent(node.getString("name_es"));
            }
            if (node.has("identifier")) {
                productElement.setAttribute("ID", node.optString("identifier", ""));
                ostra = ostras.get( node.getString("identifier") );
                if(ostra != null) {
                	for(java.util.Map.Entry<String, org.json.JSONObject> entry : ostra.entrySet()) {
                		attributeLinkElement = doc.createElement("AttributeLink");
                		attributeLinkElement.setAttribute("AttributeID", entry.getKey());
                		valueFilterElement = doc.createElement("ValueFilter");
                		dependentAttributeElement = doc.createElement("DependentAttribute");
                		ob = entry.getValue();
                		if(ob.has("IsMandatory")) {
                			attributeLinkElement.setAttribute("Mandatory", ob.getString("IsMandatory"));
                		}
                		if(ob.has("Lookup") && ob.has("ListOfValuesFilter")) {
                			lookupContent = lookupContents.get(ob.getString("Lookup"));
                			if(lookupContent != null) {
	                			filter = ob.getString("ListOfValuesFilter");
	                			pieces = filter.split(",");
	                			for(int a = 0; a<pieces.length; a++) {
	                				piece = pieces[a].trim();
	                				if(lookupContent.containsKey(piece)) {
	                					valueElement = doc.createElement("Value");
	                        			valueElement.setAttribute("ID", piece);
	                        			valueFilterElement.appendChild(valueElement);
	                				}
	                			}
                			}
                		}
                		if(ob.has("DependentAttribute") && ob.has("DependentValues")) {
                			dependentAttribute = ob.getString("DependentAttribute");
                			dependentAttributeContent = ostra.get(dependentAttribute);
                			if(dependentAttributeContent != null) {
	                			lookupContent = lookupContents.get(dependentAttributeContent.getString("Lookup"));
	                			if(lookupContent != null) {
	                				dependentAttributeElement.setAttribute("AttributeID", dependentAttribute);
		                			filter = ob.getString("DependentValues");
		                			pieces = filter.split(",");
		                			for(int a = 0; a<pieces.length; a++) {
		                				piece = pieces[a].trim();
		                				if(lookupContent.containsKey(piece)) {
		                					valueElement = doc.createElement("Value");
		                        			valueElement.setAttribute("ID", piece);
		                        			valueElement.setTextContent(lookupContent.get(piece));
		                        			dependentAttributeElement.appendChild(valueElement);
		                				}
		                			}
	                			}else {
	                				System.out.println("No lookup content found for: " + dependentAttributeContent.getString("Lookup"));
	                			}
                			}else {
                				System.out.println("Dependent attribute content not found for: " + dependentAttribute + " within ostra (" + node.getString("identifier") + ")");
                			}
                		}
                		if(ob.has("RelevantForATG")) {
                			metaDataElement = doc.createElement("MetaData");
                			valueElement = doc.createElement("Value");
                			valueElement.setAttribute("AttributeID", "RelevantForATG");
                			valueElement.setAttribute("ID", ob.getString("RelevantForATG"));
                			valueElement.setTextContent("Y".equals(ob.getString("RelevantForATG")) ? "S" : "N");
                			metaDataElement.appendChild(valueElement);
                			attributeLinkElement.appendChild(metaDataElement);
                		}
                		if(valueFilterElement.getChildNodes().getLength() > 0) {
                			attributeLinkElement.appendChild(valueFilterElement);
                		}
                		if(dependentAttributeElement.getChildNodes().getLength() > 0) {
                			attributeLinkElement.appendChild(dependentAttributeElement);
                		}
                		productElement.appendChild(attributeLinkElement);
                	}
                }
            }
            if (node.has("level")) {
            	aux = String.valueOf(node.get("level"));
                productElement.setAttribute("UserTypeID", "Level" + aux );
            }
            if (node.has("parentIdentifier") && node.getString("parentIdentifier").startsWith("cat")) {
            	value = doc.createElement("Value");
            	value.setAttribute("AttributeID", "parentCategoryID");
            	value.setTextContent(node.getString("parentIdentifier"));
                name.appendChild(value);
            }
            if("EU4-58441045".equals(node.getString("identifier"))) {
            	System.out.println(node);
            }
            if(node.has("metadata")) {
            	gpfs = node.getJSONArray("metadata");
            	if(gpfs != null){
	            	for(int i=0; i<gpfs.length(); i++) {
	            		gpf = gpfs.getJSONObject(i);
	            		value = doc.createElement("Value");
	            		value.setAttribute("AttributeID", gpf.getString("featureKey"));
	            		value.setTextContent(gpf.getString("featureValue"));
	            		values.appendChild(value);
	            	}
	            	if(node.has("keywords")) {
	            		multiValue = doc.createElement("MultiValue");
	            		for(int i=0; i<node.getJSONArray("keywords").length(); i++) {
	            			value = doc.createElement("Value");
	            			value.setTextContent(node.getJSONArray("keywords").getString(i));
	            			multiValue.appendChild(value);
	            		}
	            		multiValue.setAttribute("AttributeID", "KeyWords");
	            		values.appendChild(multiValue);
	            	}
            	}
            }
            return productElement;
        }
        return null;
    }
	
	private Element armaNivelCero(String baseUrl, Document doc, java.util.Map<String, java.util.Map<String, String>> lookupContents) {
		Element product = doc.createElement("Product");
		java.util.Map<String, org.json.JSONObject> ostro = getGlobalAttributes(baseUrl);
		Element name = null;
		Element values = null;
		Element attributeLinkElement = null;
		Element metaDataElement = null;
		Element valueElement = null;
		Element valueFilterElement = null;
		Element dependentAttributeElement = null;
		org.json.JSONObject ob = null;
		String filter = null;
		String[] pieces = null;
		String piece = null;
		java.util.Map<String, String> lookupContent = null;
		String dependentAttribute = null;
		org.json.JSONObject dependentAttributeContent = null;
    	values = doc.createElement("Values");
    	name = doc.createElement("Name");
        product.appendChild(name);
        product .appendChild(values);
    	name.setTextContent("Productos Portal Proveedores");
        product.setAttribute("ID", "ProductsSuppliersPortal");
    	for(java.util.Map.Entry<String, org.json.JSONObject> entry : ostro.entrySet()) {
    		attributeLinkElement = doc.createElement("AttributeLink");
    		attributeLinkElement.setAttribute("AttributeID", entry.getKey());
    		valueFilterElement = doc.createElement("ValueFilter");
    		dependentAttributeElement = doc.createElement("DependentAttribute");
    		ob = entry.getValue();
    		if(ob.has("IsMandatory")) {
    			attributeLinkElement.setAttribute("Mandatory", ob.getString("IsMandatory"));
    		}
    		if(ob.has("Lookup") && ob.has("ListOfValuesFilter")) {
    			lookupContent = lookupContents.get(ob.getString("Lookup"));
    			if(lookupContent != null) {
        			filter = ob.getString("ListOfValuesFilter");
        			pieces = filter.split(",");
        			for(int a = 0; a<pieces.length; a++) {
        				piece = pieces[a].trim();
        				if(lookupContent.containsKey(piece)) {
        					valueElement = doc.createElement("Value");
                			valueElement.setAttribute("ID", piece);
                			valueFilterElement.appendChild(valueElement);
        				}
        			}
    			}
    		}
    		if(ob.has("DependentAttribute") && ob.has("DependentValues")) {
    			dependentAttribute = ob.getString("DependentAttribute");
    			dependentAttributeContent = ostro.get(dependentAttribute);
    			if(dependentAttributeContent != null) {
        			lookupContent = lookupContents.get(dependentAttributeContent.getString("Lookup"));
        			if(lookupContent != null) {
        				dependentAttributeElement.setAttribute("AttributeID", dependentAttribute);
            			filter = ob.getString("DependentValues");
            			pieces = filter.split(",");
            			for(int a = 0; a<pieces.length; a++) {
            				piece = pieces[a].trim();
            				if(lookupContent.containsKey(piece)) {
            					valueElement = doc.createElement("Value");
                    			valueElement.setAttribute("ID", piece);
                    			valueElement.setTextContent(lookupContent.get(piece));
                    			dependentAttributeElement.appendChild(valueElement);
            				}
            			}
        			}else {
        				System.out.println("No lookup content found for: " + dependentAttributeContent.getString("Lookup"));
        			}
    			}else {
    				System.out.println("Dependent attribute content not found for: " + dependentAttribute + " within ostra global");
    			}
    		}
    		if(ob.has("RelevantForATG")) {
    			metaDataElement = doc.createElement("MetaData");
    			valueElement = doc.createElement("Value");
    			valueElement.setAttribute("AttributeID", "RelevantForATG");
    			valueElement.setAttribute("ID", ob.getString("RelevantForATG"));
    			valueElement.setTextContent("Y".equals(ob.getString("RelevantForATG")) ? "S" : "N");
    			metaDataElement.appendChild(valueElement);
    			attributeLinkElement.appendChild(metaDataElement);
    		}
    		if(valueFilterElement.getChildNodes().getLength() > 0) {
    			attributeLinkElement.appendChild(valueFilterElement);
    		}
    		if(dependentAttributeElement.getChildNodes().getLength() > 0) {
    			attributeLinkElement.appendChild(dependentAttributeElement);
    		}
    		product.appendChild(attributeLinkElement);
    	}
        product.setAttribute("UserTypeID", "ProductsSuppliersPortal" );
		return product;
	}

	private java.util.Map<String, org.json.JSONObject> precargaJerarquia(String structureId, java.util.LinkedList<org.json.JSONObject> ondeVaLaRaiz) throws ServiceUnavailableException{
		String url = null;
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONArray values = null;
		java.util.LinkedList<org.json.JSONObject> entradasJerarquia = new java.util.LinkedList<>();
		java.util.Map<String, org.json.JSONObject> losepas = new java.util.TreeMap<>();
		org.json.JSONArray gpf = null;
		java.util.Map<String, org.json.JSONArray> losesos = null;
		System.out.println("Collecting group features...");
		losesos = prestameLosGoupFeatures();
		System.out.println("Done collecting gpf");
		try {
			do {
				url = baseUrlDEV + "/list/StructureGroup/byStructure?structure="
						+ java.net.URLEncoder.encode( structureId ,"UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(""
							+ "StructureGroup.Identifier"
							+ ",StructureGroupLang.Name(es)"
							+ ",StructureGroup.Level"
							+ ",StructureGroup.ParentIdentifier"
							+ ",StructureGroupLang.Description(es)"
							+ ",StructureGroupAttributeValue.Value(groupType,es,DEFAULT)"
							+ ",StructureGroupLang.Synonym(es)"
							+ ",StructureGroup.CharacteristicCategories->LookupValue.Code", "UTF-8")
						+ "&metaData=true&pageSize=5000&startIndex=" + currentIndex;
				rawResponse = rc.getRequest("GET", url, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i< rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					gpf = losesos.get(rows.getJSONObject(i).getJSONObject("object").getString("id"));
//					if("EU4-58441045".equals(values.getString(0))) {
//						System.out.println(rows.getJSONObject(i) + " - " + gpf);
//						if(gpf == null) {
//							System.out.println("\t" + losesos.size());
//						}
//					}
					gpf = gpf == null ? new org.json.JSONArray() : gpf;
					entradasJerarquia.addLast(new org.json.JSONObject().put("identifier", values.getString(0)).put("name_es", values.getString(1)).put("level", Integer.parseInt(values.getString(2))).put("parentIdentifier", values.getString(3)).put("description", values.getString(4)).put("keywords", values.get(6)).put("metadata", gpf).put("categories", values.get(7)));
					currentIndex++;
				}
				log(currentIndex + "/" + totalSize);
			}while(currentIndex < totalSize);
			log("Sorting data... " + structureId);
			java.util.Collections.sort(entradasJerarquia, (o1,o2)->{
					int cmp = Integer.valueOf(o1.getInt("level")).compareTo(Integer.valueOf(o2.getInt("level")));
					if(cmp == 0) {
						cmp = o1.getString("parentIdentifier").compareTo(o2.getString("parentIdentifier"));
						return cmp;
					}
					return cmp;
				});
			org.json.JSONObject miEpa = null;
			for(org.json.JSONObject entrada : entradasJerarquia) {
				if(!losepas.containsKey(entrada.getString("identifier"))) {
					losepas.put(entrada.getString("identifier"), entrada);
				}
				miEpa = losepas.get(entrada.get("parentIdentifier"));
				if(miEpa != null) {
					if(!miEpa.has("children")) {
						miEpa.put("children", new org.json.JSONArray());
					}
					miEpa.getJSONArray("children").put(entrada);
					entrada.put("parentIdentifier", miEpa.getString("identifier"));
				}
			}
			ondeVaLaRaiz.addLast(entradasJerarquia.getFirst());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}catch(org.json.JSONException e) {
			log(rawResponse);
			e.printStackTrace();
		}
		return losepas;
	}

	private java.util.Map<String, org.json.JSONArray> prestameLosGoupFeatures(){
		java.util.Map<String, org.json.JSONArray> losesos = new java.util.TreeMap<>();
		org.json.JSONArray arr = new org.json.JSONArray();
		RESTWorkshop rw = Hierarchy.rw.getRw();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("structure", "PrimaryProductTaxonomy");
		qp.put("fields", "StructureAttribute.Identifier,StructureGroupAttributeValue.Value(es,DEFAULT)");
		qp.put("pageSize", "50");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONObject row = null;
		String objectId = null;
		int currentIndex = 0;
		int totalSize = 0;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StructureGroup/StructureGroupAttribute/byStructure", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
//				System.out.println("startIndex: " + response.getInt("startIndex") + ", totalSize: " + response.getInt("totalSize") + ", pageSize: " + response.getInt("pageSize") + ", rowCount: " + response.getInt("rowCount") + ", currentIndex: " + currentIndex + ", losesos: " + losesos.size() + ", rows size: " + rows.length());
				for(int i=0; i<rows.length(); i++) {
					row = rows.getJSONObject(i);
					values = row.getJSONArray("values");
					objectId = row.getJSONObject("object").getString("id");
					arr = losesos.get(objectId);
					if(arr == null) {
						arr = new org.json.JSONArray();
						losesos.put(objectId, arr);
					}
					arr.put(new org.json.JSONObject().put("featureKey", values.getString(0)).put("featureValue", values.getString(1)) );
				}
				currentIndex += response.getInt("pageSize");
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
				log(response == null ? "ERR: " + rw.getRawResponse() : String.valueOf(response) );
			}
		}while(currentIndex < totalSize);
//		System.out.println("CurrentIndex: " + currentIndex);
		currentIndex = 0;
		return losesos;
	}

	private java.util.Map<String, org.json.JSONObject> getAttributes(String baseUrl, String templateId){
		java.util.Map<String, org.json.JSONObject> data = new java.util.TreeMap<>();
		RESTWorkshop rw = Hierarchy.rw.getRw();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
		}
		qp.put("fields", 
			      "StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
				+ ",StandardizationValue.Characteristic->Characteristic.Lookup->Lookup.Identifier"
			);
		qp.put("query", ( 
				  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
				+ " and StandardizationValue.CreationType->LookupValue.Code equals \"CreateProposal\""
				+ " and StandardizationValue.StructureGroup->LookupValue.Code equals \"" + templateId + "\""
				+ " and ("
				+ "			StandardizationValue.Property->LookupValue.Code equals \"ListOfValuesFilter\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"MaxLength\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"IsMandatory\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"RelevantForATG\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"Business\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"DependentAttribute\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"DependentValues\""
				+ "		)"
				).replaceAll("( |\t){2,}", " "));
		qp.put("orderBy", "0-ASC");
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray prevValues = null;
		org.json.JSONObject content = new org.json.JSONObject();
		int currentIndex = 0;
		int totalSize = 0;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if( "".equals(values.getString(0)) )
						continue;
					if(prevValues != null && !prevValues.getString(0).equals(values.getString(0)) ) {
						if(!"".equals(prevValues.getString(3))) {
							content.put("Lookup", prevValues.getString(3));
						}
						data.put(prevValues.getString(0), content);
						content = new org.json.JSONObject();
					}
					content.put(values.getString(1), values.getString(2));
					prevValues = values;
				}
			}else {
				System.out.println(rw.getRawResponse());
				log(response == null ? "ERR: " + rw.getRawResponse() : String.valueOf( response ) );
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		if(content.length() > 0) {
			if(!"".equals(prevValues.getString(3))) {
				content.put("Lookup", prevValues.getString(3));
			}
			data.put(prevValues.getString(0), content);
		}
		content = null;
		return data;
	}
	
	private void collectAttributeDefinitions(String baseUrl, java.util.Map<String, org.json.JSONObject> data, java.util.Map<String, String> atgGroups, java.util.Set<String> processed, Element attributeList, Document doc) {
		StringBuilder sb = new StringBuilder();
		int cnt = 0;
		for(String key : data.keySet()) {
			if(!processed.contains(key)) {
				processed.add(key);
				sb.append(sb.length() > 0 ? "," : "");
				sb.append("'");
				sb.append(key);
				sb.append("'");
				cnt++;
				if(cnt == 100) {
					queryDataForCharacteristics(baseUrl, sb.toString(), atgGroups, attributeList, doc);
					sb.setLength(0);
					cnt = 0;
				}
			}
		}
		if(sb.length() > 0) {
			queryDataForCharacteristics(baseUrl, sb.toString(), atgGroups, attributeList, doc);
			sb.setLength(0);
			cnt = 0;
		}
	}
	
	private void collectLookupDefinitions(
			  String baseUrl
			, java.util.Map<String, org.json.JSONObject> data
			, java.util.Set<String> processed
			, Element listsOfValues
			, Document doc
			, java.util.Map<String, java.util.Map<String, String>> lookupContents
	) {
		StringBuilder sb = new StringBuilder();
		int cnt = 0;
		for(java.util.Map.Entry<String, org.json.JSONObject> entry : data.entrySet()) {
			if(entry.getValue().has("Lookup")) {
				if(!processed.contains(entry.getValue().getString("Lookup"))) {
					processed.add(entry.getValue().getString("Lookup"));
					sb.append(sb.length() > 0 ? "," : "");
					sb.append("'");
					sb.append(entry.getValue().getString("Lookup"));
					sb.append("'");
					cnt++;
					if(cnt == 100) {
						queryDataForLookup(baseUrl, sb.toString(), listsOfValues, doc, lookupContents);
						sb.setLength(0);
						cnt = 0;
					}
				}
			}
		}
		if(sb.length() > 0) {
			queryDataForLookup(baseUrl, sb.toString(), listsOfValues, doc, lookupContents);
			sb.setLength(0);
			cnt = 0;
		}
	}
	
	private java.util.Map<String, java.util.LinkedList<String>> collectCharacteristicAttributeGroups(){
		java.util.Map<String, java.util.LinkedList<String>> data = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code,LookupValueReference.LookupValues('AttributeGroup')->LookupValue.Code");
		qp.put("filter", "not LookupValueReference.LookupValues('AttributeGroup') is empty");
		qp.put("lookup", "Characteristics");
		qp.put("pageSize", "5000");
		rw.collectData("list", "LookupValue", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			java.util.LinkedList<String> vals = new java.util.LinkedList<>();
			org.json.JSONArray ag = values.getJSONArray(1);
			for(int i=0; i<ag.length(); i++) {
				if(!ag.getString(i).isEmpty()) {
					vals.addLast(ag.getString(i));
				}
			}
			data.put(values.getString(0), vals);
		}, System.out::println);
		return data;
	}
	
	private void queryDataForCharacteristics(String baseUrl, String ids, java.util.Map<String, String> atgGroups, Element attributeList, Document doc) {
		Element attributeElement = null;
		Element listOfValueLink = null;
		Element nameElement = null;
		Element validationElement = null;
		Element metaDataElement = null;
		Element multiValueElement = null;
		Element valueElement = null;
		String atgGroupLabel = null;
		RESTWorkshop rw = new RESTWorkshop();
		rw.addHeader("Authorization", this.rw.getRw().getRc().getHeader().get("Authorization"));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
		}
		java.util.Map<String, java.util.LinkedList<String>> data = collectCharacteristicAttributeGroups();
		java.util.LinkedList<String> attributeGroups = null;
		qp.put("fields", 
				   "Characteristic.Identifier"
				+ ",CharacteristicLang.Name(es)"
				+ ",Characteristic.DataType"
				+ ",Characteristic.Lookup->Lookup.Identifier"
				+ ",Characteristic.IsMultiValue"
				+ ",Characteristic.LowerBound"
				+ ",Characteristic.Order"
				+ ",CharacteristicLang.Description(es)"
				+ ",Characteristic.Purposes->LookupValue.Code"
				+ ",CharacteristicIdentifier.AlternativeIdentifier(ECC)"
				+ ",CharacteristicIdentifier.AlternativeIdentifier(S4HANA)"
			);
		qp.put("items", ids);
		response = rw.makeRequest("GET", "/list/Characteristic/byItems", qp, null);
		if(response != null) {
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				attributeElement = doc.createElement("Attribute");
				attributeElement.setAttribute("ID", values.getString(0));
				attributeElement.setAttribute("MultiValued", values.getString(4));
				attributeElement.setAttribute("Referenced", "true");
				nameElement = doc.createElement("Name");
				nameElement.setTextContent("".equals(values.getString(1)) ? values.getString(0) : values.getString(1));
				validationElement = doc.createElement("Validation");
				validationElement.setAttribute("BaseType", values.getString(2));
				attributeElement.setAttribute("Mandatory", values.getString(5) == "0" ? "false": "true" );
				if("".equals(values.getString(3))) {
					attributeElement.appendChild(validationElement);
				}
				attributeElement.appendChild(nameElement);
				metaDataElement = doc.createElement("MetaData");
				multiValueElement = doc.createElement("MultiValue");
				if(!"".equals(values.getString(3))) {
					listOfValueLink = doc.createElement("ListOfValueLink");
					listOfValueLink.setAttribute("ListOfValueID", values.getString(3));
					attributeElement.appendChild(listOfValueLink);
				}else {
					validationElement.setAttribute("BaseType", values.getString(2).toLowerCase());
					if("TEXT".equals(values.getString(2))) {
						validationElement.setAttribute("MinValue", "");
						validationElement.setAttribute("MaxValue", "");
						validationElement.setAttribute("MaxLength", "2000");
						validationElement.setAttribute("InputMask", "");
					}
				}
				if(!"".equals(values.getString(6))) {
					valueElement = doc.createElement("Value");
					valueElement.setAttribute("AttributeID", "DisplaySequence");
					valueElement.setTextContent(values.getString(6));
					metaDataElement.appendChild(valueElement);
				}
				if(!"".equals(values.getString(7))) {
					valueElement = doc.createElement("Value");
					valueElement.setAttribute("AttributeID", "AttributeHelpText");
					valueElement.setTextContent(values.getString(7));
					metaDataElement.appendChild(valueElement);
				}
				if(values.getJSONArray(8).length() > 0 && !"".equals(values.getJSONArray(8).getString(0)) ) {
					for(int j=0; j<values.getJSONArray(8).length(); j++) {
						if("isFaceted".equals(values.getJSONArray(8).getString(j))) {
							valueElement = doc.createElement("Value");
							valueElement.setAttribute("ID", "Y");
							valueElement.setAttribute("AttributeID", "isFaceted");
							valueElement.setTextContent("true");
							metaDataElement.appendChild(valueElement);
						}else if(values.getJSONArray(8).getString(j).endsWith("GPO")) {
							atgGroupLabel = atgGroups.get(values.getJSONArray(8).getString(j));
							valueElement = doc.createElement("Value");
							valueElement.setAttribute("ID", values.getJSONArray(8).getString(j));
							valueElement.setAttribute("AttributeID", values.getJSONArray(8).getString(j));
							valueElement.setTextContent(atgGroupLabel == null ? "" : atgGroupLabel);
							multiValueElement.appendChild(valueElement);
						}
					}
				}
				if(!"".equals(values.getString(9)) || !"".equals(values.getString(10))) {
					String dat = "".equals(values.getString(9)) ? values.getString(10) : values.getString(9);
					valueElement = doc.createElement("Value");
					valueElement.setAttribute("AttributeID", "ExternalMapping");
					valueElement.setTextContent(dat);
					metaDataElement.appendChild(valueElement);
				}
				if(multiValueElement.getChildNodes().getLength() > 0) {
					multiValueElement.setAttribute("AttributeID", "isAttInGroupAtt");
					metaDataElement.appendChild(multiValueElement);
				}
				if(metaDataElement.getChildNodes().getLength() > 0) {
					attributeElement.appendChild(metaDataElement);
				}
				attributeGroups = data.get(values.getString(0));
				if(attributeGroups != null) {
					for(String attributeGroup : attributeGroups) {
						Element attributeGroupLink = doc.createElement("AttributeGroupLink");
						attributeGroupLink.setAttribute("AttributeGroupID", attributeGroup);
						attributeElement.appendChild(attributeGroupLink);
					}
				}
				attributeList.appendChild(attributeElement);
			}
		}else {
			System.out.println("ERROR: " + rw.getRawResponse());
		}
	}
	
	private void queryDataForLookup(
			String baseUrl
			, String ids
			, Element listsOfValues
			, Document doc
			, java.util.Map<String, java.util.Map<String, String>> lookupContents
	) {
		Element listOfValuesElement = null;
		Element nameElement = null;
		Element validationElement = null;
		Element valueElement = null;
		java.util.LinkedList<java.util.Map.Entry<String, String>> lookupData = null;
		RESTWorkshop rw = Hierarchy.rw.getRw();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, String> lookupContent = null;
		String lkp = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		boolean collect = false;
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
		}
		qp.put("fields", "Lookup.Identifier,LookupLang.Name(es),Lookup.DataType");
		qp.put("items", ids);
		response = rw.makeRequest("GET", "/list/Lookup/byItems", qp, null);
		if(response != null) {
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				listOfValuesElement = doc.createElement("ListOfValue");
				listOfValuesElement.setAttribute("ID", values.getString(0));
				listOfValuesElement.setAttribute("ParentID", "");
				listOfValuesElement.setAttribute("AllowUserValueAddition", "false");
				listOfValuesElement.setAttribute("UseValueID", "false");
				listOfValuesElement.setAttribute("Referenced", "true");
				nameElement = doc.createElement("Name");
				nameElement.setTextContent(values.getString(1));
				validationElement = doc.createElement("Validation");
				validationElement.setAttribute("BaseType", values.getString(2).toLowerCase());
				validationElement.setAttribute("MinValue", "");
				validationElement.setAttribute("MaxValue", "");
				validationElement.setAttribute("MaxLength", "100");
				validationElement.setAttribute("InputMask", "");
				listOfValuesElement.appendChild(validationElement);
				if(!"".equals(values.getString(1))) {
					listOfValuesElement.appendChild(nameElement);
				}
				lkp = values.getString(0);
				lookupData = collectLookupValues(baseUrl, lkp);
				if(!lookupData.isEmpty()) {
					lookupContent = lookupContents.get(lkp);
					if(lookupContent == null) {
						lookupContent = new java.util.TreeMap<>();
						lookupContents.put(lkp, lookupContent);
						collect = true;
					} else {
						collect = false;
					}
					for(java.util.Map.Entry<String, String> entry : lookupData) {
						if(collect) {
							lookupContent.put(entry.getKey(), entry.getValue());
						}
						valueElement = doc.createElement("Value");
						valueElement.setAttribute("ID", entry.getKey());
						valueElement.setTextContent(entry.getValue());
						listOfValuesElement.appendChild(valueElement);
					}
					
				}
				listsOfValues.appendChild(listOfValuesElement);
			}
		}else {
			System.out.println("ERROR: " + rw.getRawResponse());
		}
	}
	
	private java.util.LinkedList<java.util.Map.Entry<String, String>> collectLookupValues(String baseUrl, String lookup){
		java.util.LinkedList<java.util.Map.Entry<String, String>> data = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
		rw.addHeader("Authorization", this.rw.getRw().getRc().getHeader().get("Authorization"));
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
		}
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("query",  "LookupValue.IsActive = true");
		qp.put("lookup", lookup);
		qp.put("pageSize", "2000");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					data.addLast( new java.util.AbstractMap.SimpleEntry<>(values.getString(0), values.getString(1)) );
				}
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}

	private java.util.Map<String, java.util.Map<String, org.json.JSONObject>> getAttributes(String baseUrl){
		java.util.Map<String, java.util.Map<String, org.json.JSONObject>> data = new java.util.TreeMap<>();
		java.util.Map<String, org.json.JSONObject> attributeMetaData = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
		rw.addHeader("Authorization", this.rw.getRw().getRc().getHeader().get("Authorization"));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
		}
		qp.put("fields", 
			      "StandardizationValue.StructureGroup->LookupValue.Code"
		        + ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
				+ ",StandardizationValue.Characteristic->Characteristic.Lookup->Lookup.Identifier"
			);
		qp.put("query", ( 
				  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
				+ " and StandardizationValue.CreationType->LookupValue.Code equals \"CreateProposal\""
				+ " and ("
				+ "			StandardizationValue.Property->LookupValue.Code equals \"ListOfValuesFilter\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"MaxLength\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"IsMandatory\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"RelevantForATG\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"Business\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"DependentAttribute\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"DependentValues\""
				+ "		)"
				).replaceAll("( |\t){2,}", " "));
		qp.put("pageSize", "10000");
		qp.put("orderBy", "0-ASC,1-ASC");
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray prevValues = null;
		org.json.JSONObject content = new org.json.JSONObject();
		int currentIndex = 0;
		int totalSize = 0;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if("".equals(values.getString(0)) || "".equals(values.getString(1)))
						continue;
					if(prevValues != null && (!prevValues.getString(0).equals(values.getString(0)) || !prevValues.getString(1).equals(values.getString(1)) ) ) {
						if(!"".equals(prevValues.getString(4))) {
							content.put("Lookup", prevValues.getString(4));
						}
						attributeMetaData.put(prevValues.getString(1), content);
						content = new org.json.JSONObject();
						if(!prevValues.getString(0).equals(values.getString(0))) {
							data.put(prevValues.getString(0), attributeMetaData);
							attributeMetaData = new java.util.TreeMap<>();
						}
					}
					content.put(values.getString(2), values.getString(3));
					prevValues = values;
				}
			}else {
				System.out.println(rw.getRawResponse());
				log(response == null ? "ERR: " + rw.getRawResponse() : String.valueOf( response ) );
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;
		if(!"".equals(prevValues.getString(4))) {
			content.put("Lookup", prevValues.getString(4));
		}
		if(content.length() > 0) {
			attributeMetaData.put(prevValues.getString(1), content);
		}
		if(!attributeMetaData.isEmpty()) {
			attributeMetaData.put(prevValues.getString(1), content);
			data.put(prevValues.getString(0), attributeMetaData);
		}
		content = null;
		return data;
	}

	private java.util.Map<String, org.json.JSONObject> getGlobalAttributes(String baseUrl){
		java.util.Map<String, org.json.JSONObject> attributeMetaData = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
		rw.addHeader("Authorization", Hierarchy.rc.getHeader().get("Authorization"));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
		}
		qp.put("fields", 
		           "StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
				+ ",StandardizationValue.Characteristic->Characteristic.Lookup->Lookup.Identifier"
			);
		qp.put("query", ( 
				  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"GlobalTemplateAttributeConfiguration\""
				+ " and StandardizationValue.CreationType->LookupValue.Code equals \"CreateProposal\""
				+ " and ("
				+ "			StandardizationValue.Property->LookupValue.Code equals \"ListOfValuesFilter\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"MaxLength\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"IsMandatory\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"RelevantForATG\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"Business\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"DependentAttribute\""
				+ " or "
				+ "			StandardizationValue.Property->LookupValue.Code equals \"DependentValues\""
				+ "		)"
				).replaceAll("( |\t){2,}", " "));
		qp.put("pageSize", "10000");
		qp.put("orderBy", "0-ASC");
		qp.put("dictionaryProxy", "'GlobalTemplateAttributeConfiguration'");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray prevValues = null;
		org.json.JSONObject content = new org.json.JSONObject();
		int currentIndex = 0;
		int totalSize = 0;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if("".equals(values.getString(0)) || "".equals(values.getString(1)) || "".equals(values.getString(2)) || "".equals(values.getString(3)))
						continue;
					if(prevValues != null && (!prevValues.getString(0).equals(values.getString(0)) ) ) {
						if(!"".equals(prevValues.getString(3))) {
							content.put("Lookup", prevValues.getString(3));
						}
						attributeMetaData.put(prevValues.getString(0), content);
						content = new org.json.JSONObject();
					}
					content.put(values.getString(1), values.getString(2));
					prevValues = values;
				}
			}else {
				System.out.println(rw.getRawResponse());
				log(response == null ? "ERR: " + rw.getRawResponse() : String.valueOf( response ) );
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;
		if(!"".equals(prevValues.getString(3))) {
			content.put("Lookup", prevValues.getString(3));
		}
		if(content.length() > 0) {
			attributeMetaData.put(prevValues.getString(0), content);
		}
		content = null;
		return attributeMetaData;
	}

	private void log(String message) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","generateHierarchy.log").toString(), true)))) {
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
					+ "]  " + message);
		} catch (java.io.IOException e) {
		}
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","generateHierarchy.log").toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}

}
