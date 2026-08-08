package mx.com.liverpool.p360.services.core.temp.exports;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RestClient;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

public class HierarchySitiosWeb {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RestClient rc = rw.getRw().getRc();

	private final java.nio.file.Path baseDirPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_dir_to_mirror_out_files"), "ToEcommerce");
	private final String baseDir = baseDirPath.toString();
	private final String outputXmlFile = java.nio.file.Paths.get(baseDir, System.currentTimeMillis() + "_pépeleJairarqui.xml").toString();
	
	
	
	public static void main(String[] args) throws ServiceUnavailableException {
		HierarchySitiosWeb h = new HierarchySitiosWeb();
		h.createHierarchyFile( new String[] {"catst83801977","catst84046110","catst83801982","catst83801983","catst83801984","catst83801987","catst83801988","catst83801989","catst83801991","catst83801992","catst83801993","ctst84046005","catst84046006","catst84046007","catst84046009","catst84046010","catst84046011","catst84046012","catst84046014","catst84046015","catst83850477","catst83801733","catst83801734","catst84787001","catst84787002","catst84787003"} );
	}
	
	private void createHierarchyFile(String[] ofInterest) throws ServiceUnavailableException {
		long init = System.currentTimeMillis();
		if(!java.nio.file.Files.exists(baseDirPath, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
			try {
				java.nio.file.Files.createDirectories(baseDirPath);
			}catch(java.io.IOException e) {
				
			}
		}
		java.util.LinkedList<org.json.JSONObject> rescataLaRaiz = new java.util.LinkedList<>();
		java.util.Map<String, org.json.JSONObject> multisitios = new java.util.TreeMap<>();
		precargaJerarquia("Sitios Web", rescataLaRaiz);
		
		org.json.JSONObject entryHelper = null;
        try {

        	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        	DocumentBuilder builder = factory.newDocumentBuilder();
        	Document doc = builder.newDocument();

        	log("Building global lists for attributes and lookups");
        	Element spim = doc.createElement("STEP-ProductInformation");
        	spim.setAttribute("ExportTime", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format( new java.util.Date() ));
        	spim.setAttribute("ExportContext", "Context2");
        	spim.setAttribute("ContextID", "Context2");
        	spim.setAttribute("WorkspaceID", "Main");
        	spim.setAttribute("UseContextLocale", "false");

        	doc.appendChild(spim);

        	log("Reading templates...");
        	Element classifications = doc.createElement("Classifications");
        	spim.appendChild(classifications);

        	Element raizClassification = doc.createElement("Classification");
        	raizClassification.setAttribute("ID", "Classification 1 root");
        	raizClassification.setAttribute("UserTypeID", "Classification 1 user-type root");
        	raizClassification.setAttribute("Selected", "false");
        	classifications.appendChild(raizClassification);
        	Element helperElement = null;
        	Element prevHelperElement = null;
        	java.util.Map<String, Element> tableroDeControl = new java.util.TreeMap<>();
        	buildMapFromList(rescataLaRaiz.getFirst(), multisitios);
        	for(String element : ofInterest) {
    			entryHelper = multisitios.get(element);
    			helperElement = pacheleWeb(entryHelper, doc, multisitios);
    			if(!tableroDeControl.containsKey(entryHelper.getString("identifier"))) {
    				tableroDeControl.put(entryHelper.getString("identifier"), helperElement);
    			}
    			while(entryHelper.has("parentIdentifier") && !"".equals(entryHelper.get("parentIdentifier")) && !tableroDeControl.containsKey(entryHelper.getString("parentIdentifier"))) {
    				prevHelperElement = helperElement;
    				entryHelper = multisitios.get(entryHelper.getString("parentIdentifier"));
    				helperElement = pacheleWeb(entryHelper, doc, multisitios);
    				if(helperElement == null) {
    					log("PANIC: No element could be made from: " + entryHelper);
    					break;
    				}
    				helperElement.appendChild(prevHelperElement);
    				tableroDeControl.put(entryHelper.getString("identifier"), helperElement);
    			}
        	}
        	for(String element : ofInterest) {
        		entryHelper = multisitios.get(element);
        		helperElement = tableroDeControl.get(element);
        		appendMisHijos(entryHelper, helperElement, tableroDeControl, multisitios, doc);
        	}
        	for(org.json.JSONObject laRaiz : rescataLaRaiz) {
        		helperElement = tableroDeControl.get(laRaiz.getString("identifier"));
        		log("La raiz: " + helperElement.getAttribute("ID"));
        		if(helperElement != null) {
        			raizClassification.appendChild(helperElement);
        		}
        	}
        	TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");

			java.io.StringWriter writer = new java.io.StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
			String xmlOutput = writer.getBuffer().toString()
					.replace("&lt;CRLF&gt;", "&#13;&#10;")
				    .replace("<CRLF>", "&#13;&#10;");
//            transformer.transform(xmlOutput, new StreamResult(new java.io.File(outputXmlFile)));
            try {
				java.nio.file.Files.writeString(java.nio.file.Paths.get(outputXmlFile), xmlOutput, java.nio.charset.StandardCharsets.UTF_8);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (TransformerException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		log("Done. " + new RESTWorkshop().formatTime(System.currentTimeMillis() - init));
	}
	
	private void appendMisHijos(org.json.JSONObject entry, Element entryElement, java.util.Map<String, Element> tableroDeControl, java.util.Map<String, org.json.JSONObject> multisitios, Document doc) {
		Element myElement = null;
		if(entry.has("children")) {
			org.json.JSONArray children = entry.getJSONArray("children");
			for(int i=0; i<children.length(); i++) {
				myElement = tableroDeControl.get(children.getJSONObject(i).getString("identifier"));
				if(myElement == null) {
					myElement = pacheleWeb(children.getJSONObject(i), doc, multisitios);
					tableroDeControl.put(children.getJSONObject(i).getString("identifier"), myElement);
					entryElement.appendChild(myElement);
				}
				appendMisHijos(children.getJSONObject(i), myElement, tableroDeControl, multisitios, doc);
			}
		}
	}
	
	private void buildMapFromList(org.json.JSONObject element, java.util.Map<String, org.json.JSONObject> elements) {
		elements.put(element.getString("identifier"), element);
		if(element.has("children")) {
			org.json.JSONArray children = element.getJSONArray("children");
			for(int i=0; i<children.length(); i++) {
				buildMapFromList(children.getJSONObject(i), elements);
			}
		}
	}

	private Element pacheleWeb(JSONObject node, Document doc, java.util.Map<String, org.json.JSONObject> multisitios) {
		String displayName = null;
		String groupType = null;
		String department = null;
		Element metaData = null;
		Element value = null;
		Element multiValue = null;
		org.json.JSONArray gpfs = null;
		org.json.JSONObject gpf = null;
		int aux = -1;
		Element departmentElement = null;
        if (node != null) {
        	metaData = doc.createElement("MetaData");
            Element classificationElement = doc.createElement("Classification");
            classificationElement.appendChild(metaData);
            if (node.has("identifier")) {
            	String valID =  node.optString("identifier", "");
            	if (node.has("level")) {
	            	aux = node.getInt("level");
	            	aux--;
	                valID = -1 == aux && "Sitios Web".equals(valID) ? "WebHierarchyRoot" : valID ;
            	}
            	if("WebHierarchyRoot".equals(valID)) {
                    classificationElement.setAttribute("Selected", "true");
            	}
                classificationElement.setAttribute("ID", valID);
            }
            if (node.has("level")) {
            	aux = node.getInt("level");
            	aux--;
                classificationElement.setAttribute("UserTypeID", -1 == aux ? "WebHierarchyRoot" : 0 == aux ? "WebsiteRoot" : "WebLevel" + aux );
            }
            if (node.has("parentIdentifier") && aux > 1 /* && node.getString("parentIdentifier").startsWith("cat") */) {
            	value = doc.createElement("Value");
        		value.setAttribute("Changed", "true");
            	value.setAttribute("AttributeID", "parentCategoryID");
            	value.setTextContent(node.getString("parentIdentifier"));
                metaData.appendChild(value);
            }
            if(node.has("metadata")) {
            	gpfs = node.getJSONArray("metadata");
            	log(node.getString("identifier") + ": " + node.get("metadata"));
            	if(gpfs != null){
	            	for(int i=0; i<gpfs.length(); i++) {
	            		gpf = gpfs.getJSONObject(i);
	            		if("".equals(gpf.getString("featureValue")))
	            			continue;
	            		value = doc.createElement("Value");
	            		value.setAttribute("Changed", "true");
	            		value.setAttribute("AttributeID", gpf.getString("featureKey"));
	            		value.setTextContent(gpf.getString("featureValue"));
	            		value.setAttribute("Changed", "true");
	            		if("DisplayName".equals(gpf.getString("featureKey"))) {
	            			displayName = !"".equals(gpf.getString("featureValue")) ? gpf.getString("featureValue") : null;
	            		}else if("groupType".equals(gpf.getString("featureKey"))) {
	            			groupType = !"".equals(gpf.getString("featureValue")) ? gpf.getString("featureValue") : null;
	            		}else if("department".equals(gpf.getString("featureKey"))) {
	            			department = !"".equals(gpf.getString("featureValue")) ? gpf.getString("featureValue") : null;
	            			departmentElement = value;
	            		}else if("isBrand".equals(gpf.getString("featureKey"))) {
	            			value = doc.createElement("Value");
		            		value.setAttribute("Changed", "true");
	                    	value.setAttribute("AttributeID", "isBrand");
	                    	value.setAttribute("ID", Boolean.parseBoolean(gpf.getString("featureValue")) ? "1" : "0");
	                    	value.setTextContent( Boolean.parseBoolean(gpf.getString("featureValue")) ? "True" : "False" );
	                    	metaData.appendChild(value);
	            		}else if("isBrandLanding".equals(gpf.getString("featureKey"))) {
	            			value = doc.createElement("Value");
		            		value.setAttribute("Changed", "true");
	                    	value.setAttribute("AttributeID", "isBrandLanding");
	                    	value.setAttribute("ID", Boolean.parseBoolean(gpf.getString("featureValue")) ? "1" : "0");
	                    	value.setTextContent( Boolean.parseBoolean(gpf.getString("featureValue")) ? "True" : "False" );
	                    	metaData.appendChild(value);
	            		}else if("allowGiftMessage".equals(gpf.getString("featureKey"))) {
	            			value = doc.createElement("Value");
		            		value.setAttribute("Changed", "true");
	                    	value.setAttribute("AttributeID", "allowGiftMessage");
	                    	value.setAttribute("ID", Boolean.parseBoolean(gpf.getString("featureValue")) ? "Y" : "N");
	                    	value.setTextContent( Boolean.parseBoolean(gpf.getString("featureValue")) ? "True" : "False" );
	                    	metaData.appendChild(value);
	            		}else if("sentToFA".equals(gpf.getString("featureKey"))) {
	            			value = doc.createElement("Value");
		            		value.setAttribute("Changed", "true");
	                    	value.setAttribute("AttributeID", "sentToFA");
	                    	value.setAttribute("ID", Boolean.parseBoolean(gpf.getString("featureValue")) ? "1" : "0");
	                    	value.setTextContent( Boolean.parseBoolean(gpf.getString("featureValue")) ? "True" : "False" );
	                    	metaData.appendChild(value);
	            		}else if("skipInventory".equals(gpf.getString("featureKey"))) {
	            			value = doc.createElement("Value");
		            		value.setAttribute("Changed", "true");
	                    	value.setAttribute("AttributeID", "skipInventory");
	                    	value.setAttribute("ID", "skip".equals(gpf.getString("featureValue")) ? "1" : "0");
	                    	value.setTextContent(gpf.getString("featureValue"));
	                    	metaData.appendChild(value);
	            		}else if("giftMessage".equals(gpf.getString("featureKey"))) {
	            			value = doc.createElement("Value");
		            		value.setAttribute("Changed", "true");
	                    	value.setAttribute("AttributeID", "giftMessage");
	                    	value.setAttribute("ID", "allow".equals(gpf.getString("featureValue")) ? "1" : "0");
	                    	value.setTextContent(gpf.getString("featureValue"));
	                    	metaData.appendChild(value);
	            		}else if("DeliveringToExternalSystems".equals(gpf.getString("featureKey")) || "LastUserDeliverIssuer".equals(gpf.getString("featureKey"))){
	            			
	            		} else {
	            			metaData.appendChild(value);
	            		}
	            	}
	            	if(node.has("keywords")) {
	            		multiValue = doc.createElement("MultiValue");
	            		for(int i=0; i<node.getJSONArray("keywords").length(); i++) {
	            			value = doc.createElement("Value");
		            		value.setAttribute("Changed", "true");
	            			if(!"".equals(node.getJSONArray("keywords").getString(i))){
		            			value.setTextContent(node.getJSONArray("keywords").getString(i));
		            			multiValue.appendChild(value);
	            			}
	            		}
	            		multiValue.setAttribute("AttributeID", "KeyWords");
	            		if(multiValue.getChildNodes().getLength() > 0)
	            			metaData.appendChild(multiValue);
	            	}
            	}
            }
            if ((displayName == null || "".equals(displayName)) && node.has("name_es")) {
            	value = doc.createElement("Value");
        		value.setAttribute("Changed", "true");
            	value.setAttribute("AttributeID", "DisplayName");
            	if(!"".equals(node.getString("name_es"))) {
	            	value.setTextContent(node.getString("name_es").replaceAll(" \\(.+\\)", ""));
	            	metaData.appendChild(value);
            	}
            }else {
            	value = doc.createElement("Value");
        		value.setAttribute("Changed", "true");
            	value.setAttribute("AttributeID", "DisplayName");
            	value.setTextContent(node.getString("name_es").replaceAll(" \\(.+\\)", ""));
            	metaData.appendChild(value);
            }
            if(groupType == null) {
            	value = doc.createElement("Value");
        		value.setAttribute("Changed", "true");
            	value.setAttribute("AttributeID", "groupType");
            	value.setAttribute("ID", "0");
            	value.setTextContent("Not Specified");
            	metaData.appendChild(value);
            }else {
            	value = doc.createElement("Value");
        		value.setAttribute("Changed", "true");
            	value.setAttribute("AttributeID", "groupType");
            	value.setAttribute("ID", "Optics".equals(groupType) ? "3" : "MAC Non-Collection".equals(groupType) ? "2" : "MAC Collection".equals(groupType) ? "1" : "0");
            	value.setTextContent(groupType);
            	metaData.appendChild(value);
            }
            if(aux == 1 && department == null) {
//            	System.out.println("Ea ea ##");
            	department = getMeParentDepartment(multisitios.get(node.getString("parentIdentifier")), doc);
//            	System.out.println("Suelo suelo suelo (" + node.getString("parentIdentifier") + "): " + department);
            	value = doc.createElement("Value");
        		value.setAttribute("Changed", "true");
            	value.setAttribute("AttributeID", "department");
            	value.setAttribute("ID", 
            			"BabiesRUs".equals(department) ? "BRU" 
        					: "Banana Republic".equals(department) ? "BNR" 
        							: "Dupuis".equals(department) ? "DPS" 
        									: "Fabletics".equals(department) ? "FAB" 
        											: "GAP".equals(department) ? "106" 
        													: "Liverpool".equals(department) ? "NA" 
        															: "Pottery Barn".equals(department) ? "104" 
        																	: "Pottery Barn Kids".equals(department) ? "105" 
        																			: "Suburbia".equals(department) ? "SB" 
        																					: "ToysRUs".equals(department) ? "424" 
        																							: "West Elm".equals(department) ? "107" 
        																									: "William Sonoma".equals(department) ? "307" : "");
            	value.setTextContent(department);
            	metaData.appendChild(value);
            }
            if(aux > 1 && department != null) {
            	try{ metaData.removeChild(departmentElement); }catch(org.w3c.dom.DOMException e) { log(e.getMessage()); }
            }
            if(metaData.getChildNodes().getLength() > 0)
            	classificationElement.appendChild(metaData);
            return classificationElement;
        }
        return null;
    }
	
	private String getMeParentDepartment(org.json.JSONObject node, Document doc) {
		String department = null;
		Element value = null;
		org.json.JSONArray gpfs = null;
		org.json.JSONObject gpf = null;
        if (node != null) {
            if(node.has("metadata")) {
            	gpfs = node.getJSONArray("metadata");
            	if(gpfs != null){
	            	for(int i=0; i<gpfs.length(); i++) {
	            		gpf = gpfs.getJSONObject(i);
	            		value = doc.createElement("Value");
	            		value.setAttribute("Changed", "true");
	            		value.setAttribute("AttributeID", gpf.getString("featureKey"));
	            		value.setTextContent(gpf.getString("featureValue"));
	            		log(node.getString("identifier") + ", " + gpf.getString("featureKey") + " - " + gpf.getString("featureValue"));
	            		if(!"".equals(gpf.getString("featureValue"))) {}
	            		if("department".equals(gpf.getString("featureKey"))) {
	            			department = !"".equals(gpf.getString("featureValue")) ? gpf.getString("featureValue") : null;
	            		}
//	            		System.out.println("--- " + gpf);
	            	}
//	            	System.out.println("Ya no le sabes");
            	}else {
//            		System.out.println("2 No metadata found: " + node.getString("identifier"));
            	}
            }else {
//        		System.out.println("No metadata found: " + node.getString("identifier"));
        	}
        }else {
//        	System.out.println("Ta null");
        }
        return department;
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
		log("Collecting group features...");
		losesos = prestameLosGoupFeatures();
		log("Done collecting gpf");
		try {
			do {
				url = rw.getRw().getBaseUrl() + "/list/StructureGroup/byStructure?structure="
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
					gpf = gpf == null ? new org.json.JSONArray() : gpf;
					if("ToysRUs".equals(values.getString(0))) {
						log(rows.getJSONObject(i).getJSONObject("object").getString("id"));
						System.out.println(rows.getJSONObject(i).getJSONObject("object").getString("id"));
						System.out.println(gpf.toString());
					}
					entradasJerarquia.addLast(new org.json.JSONObject()
							.put("identifier", values.getString(0))
							.put("name_es", values.getString(1))
							.put("level", Integer.parseInt(values.getString(2)))
							.put("parentIdentifier", values.getString(3))
							.put("description", values.getString(4))
							.put("keywords", values.get(6))
							.put("metadata", gpf)
							.put("categories", values.get(7))
						);
				}
				currentIndex += response.getInt("pageSize");
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
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("structure", "Sitios Web");
		qp.put("fields", "StructureAttribute.Identifier,StructureGroupAttributeValue.Value(es,DEFAULT)");
		qp.put("pageSize", "5000");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONObject row = null;
		String objectId = null;
		int currentIndex = 0;
		int totalSize = 0;
		boolean hadGroupType = false;
		qp.put("items", "237955@42000");
		response = rw.getRw().makeRequest("GET", "/list/StructureGroup/StructureGroupAttribute/byItems", qp, null);
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.getRw().makeRequest("GET", "/list/StructureGroup/StructureGroupAttribute/byStructure", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					row = rows.getJSONObject(i);
					values = row.getJSONArray("values");
					objectId = row.getJSONObject("object").getString("id");
//					if("44271@12000".equals(objectId)) {
//						System.out.println("\n\n\t\tHERE!!!! " + row + " \n\n\n");
//					}else if("237955@42000".equals(objectId)) {
//						System.out.println("\n\n\t\tHERE!!!! " + row + " \n\n\n");
//					}
					arr = losesos.get(objectId);
					if(arr == null) {
						arr = new org.json.JSONArray();
						losesos.put(objectId, arr);
					}
					if("categoryStartDate".equals(values.getString(0)) || "categoryEndDate".equals(values.getString(0))) {
						arr.put(new org.json.JSONObject().put("featureKey", values.getString(0)).put("featureValue", fixDateFormat( values.getString(1) ) + " 00:00:00") );
					} else {
						arr.put(new org.json.JSONObject().put("featureKey", values.getString(0)).put("featureValue", values.getString(1)) );
					}
				}
				currentIndex += response.getInt("pageSize");
				log(currentIndex + "/" + totalSize + ", " + rows.length());
				response.remove("rows");
//				System.out.println(currentIndex + "/" + totalSize + ", " + rows.length() + " - " + response);
			}else {
				log("ERROR: " + rw.getRw().getRawResponse());
				log(response == null ? "ERR: " + rw.getRw().getRawResponse() : String.valueOf(response) );
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return losesos;
	}

	private String fixDateFormat(String d) {
		String val = d;
		if(!d.matches("[0-9]{4}-.+")) {
			if(d.matches("[0-9]{2}.[0-9]{2}.[0-9]{4}")) {
				try {
					val = new java.text.SimpleDateFormat("yyyy-MM-dd").format( new java.text.SimpleDateFormat("dd" + d.charAt(2) + "MM" + d.charAt(2) + "yyyy").parse(d));
				}catch(java.text.ParseException e) {
					e.printStackTrace();
				}
			}
		}
		return val;
	}
	
	private java.util.Map<String, org.json.JSONObject> getAttributes(String baseUrl, String templateId){
		java.util.Map<String, org.json.JSONObject> data = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
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
				log(rw.getRawResponse());
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
	
	private void collectLookupDefinitions(String baseUrl, java.util.Map<String, org.json.JSONObject> data, java.util.Set<String> processed, Element listsOfValues, Document doc, java.util.Map<String, java.util.Map<String, String>> lookupContents) {
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
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
		}
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
//			log("For ids: " + ids + ", got: " + rows);
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				attributeElement = doc.createElement("Attribute");
				attributeElement.setAttribute("ID", values.getString(0));
				attributeElement.setAttribute("MultiValued", values.getString(4));
				attributeElement.setAttribute("Referenced", "true");
				nameElement = doc.createElement("Name");
				nameElement.setTextContent(values.getString(1));
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
						validationElement.setAttribute("MaxLength", "2000");
					}
				}
				if(!"".equals(values.getString(6))) {
					valueElement = doc.createElement("Value");
            		valueElement.setAttribute("Changed", "true");
					valueElement.setAttribute("AttributeID", "DisplaySequence");
					valueElement.setTextContent(values.getString(6));
					metaDataElement.appendChild(valueElement);
				}
				if(!"".equals(values.getString(7))) {
					valueElement = doc.createElement("Value");
            		valueElement.setAttribute("Changed", "true");
					valueElement.setAttribute("AttributeID", "AttributeHelpText");
					valueElement.setTextContent(values.getString(7));
					metaDataElement.appendChild(valueElement);
				}
				if(values.getJSONArray(8).length() > 0 && !"".equals(values.getJSONArray(8).getString(0)) ) {
					for(int j=0; j<values.getJSONArray(8).length(); j++) {
						if("isFaceted".equals(values.getJSONArray(8).getString(j))) {
							valueElement = doc.createElement("Value");
		            		valueElement.setAttribute("Changed", "true");
							valueElement.setAttribute("ID", "Y");
							valueElement.setTextContent("true");
							metaDataElement.appendChild(valueElement);
						}else if(values.getJSONArray(8).getString(j).endsWith("GPO")) {
							atgGroupLabel = atgGroups.get(values.getJSONArray(8).getString(j));
							valueElement = doc.createElement("Value");
		            		valueElement.setAttribute("Changed", "true");
							valueElement.setAttribute("ID", values.getJSONArray(8).getString(j));
							valueElement.setTextContent(atgGroupLabel == null ? "" : atgGroupLabel);
							multiValueElement.appendChild(valueElement);
						}
					}
				}
				if(!"".equals(values.getString(9)) || !"".equals(values.getString(10))) {
					String dat = "".equals(values.getString(9)) ? values.getString(10) : values.getString(9);
					valueElement = doc.createElement("Value");
            		valueElement.setAttribute("Changed", "true");
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
				attributeList.appendChild(attributeElement);
			}
		}else {
			log("ERROR: " + rw.getRawResponse());
		}
	}
	
	private void queryDataForLookup(String baseUrl, String ids, Element listsOfValues, Document doc, java.util.Map<String, java.util.Map<String, String>> lookupContents) {
		Element listOfValuesElement = null;
		Element nameElement = null;
		Element validationElement = null;
		Element valueElement = null;
		java.util.LinkedList<java.util.Map.Entry<String, String>> lookupData = null;
		RESTWorkshop rw = new RESTWorkshop();
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
				listOfValuesElement.setAttribute("Referenced", "true");
				nameElement = doc.createElement("Name");
				nameElement.setTextContent(values.getString(1));
				validationElement = doc.createElement("Validation");
				validationElement.setAttribute("BaseType", values.getString(2).toLowerCase());
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
	            		valueElement.setAttribute("Changed", "true");
						valueElement.setAttribute("ID", entry.getKey());
						valueElement.setTextContent(entry.getValue());
						listOfValuesElement.appendChild(valueElement);
					}
					
				}
				listsOfValues.appendChild(listOfValuesElement);
			}
		}else {
			log("ERROR: " + rw.getRawResponse());
		}
	}
	
	private java.util.LinkedList<java.util.Map.Entry<String, String>> collectLookupValues(String baseUrl, String lookup){
		java.util.LinkedList<java.util.Map.Entry<String, String>> data = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
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
		qp.put("pageSize", "600");
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
				log("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}

	private java.util.Map<String, java.util.Map<String, org.json.JSONObject>> getAttributes(String baseUrl){
		java.util.Map<String, java.util.Map<String, org.json.JSONObject>> data = new java.util.TreeMap<>();
		java.util.Map<String, org.json.JSONObject> attributeMetaData = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
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
		qp.put("pageSize", "1000");
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
				log(rw.getRawResponse());
				log(response == null ? "ERR: " + rw.getRawResponse() : String.valueOf( response ) );
			}
			log(currentIndex + "/" + totalSize);
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
		qp.put("pageSize", "1000");
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
				log(rw.getRawResponse());
				log(response == null ? "ERR: " + rw.getRawResponse() : String.valueOf( response ) );
			}
			log(currentIndex + "/" + totalSize);
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
