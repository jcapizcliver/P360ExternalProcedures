package mx.com.liverpool.p360.services.core.temp.exports;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.naming.ServiceUnavailableException;
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

public class HierarchySitiosWeb4 {

    private static final RESTWrapper rw = new RESTWrapper();
    private static final RestClient rc = rw.getRw().getRc();

    private final java.nio.file.Path baseDirPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_dir_to_mirror_out_files"), "ToEcommerce");
    private final String baseDir = baseDirPath.toString();
    private final String outputXmlFile = java.nio.file.Paths.get(baseDir, System.currentTimeMillis() + "_pépeleJairarqui.xml").toString();

    public static void main(String[] args) throws ServiceUnavailableException {
        HierarchySitiosWeb4 h = new HierarchySitiosWeb4();
        h.createHierarchyFile( args );
//        h.createHierarchyFile( new String[] {"catst81951117"} );
    }

    public String createHierarchyFile(String[] ofInterest) throws ServiceUnavailableException {
        long init = System.currentTimeMillis();
        if(!java.nio.file.Files.exists(baseDirPath, java.nio.file.LinkOption.NOFOLLOW_LINKS)) {
            try {
                java.nio.file.Files.createDirectories(baseDirPath);
            } catch(java.io.IOException e) {
                // Silenced
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
                if (entryHelper == null) {
                    log("WARN: ID not found in preloaded hierarchy: " + element);
                    continue;
                }

                helperElement = pacheleWeb(entryHelper, doc, multisitios);
                if(!tableroDeControl.containsKey(entryHelper.getString("identifier"))) {
                    tableroDeControl.put(entryHelper.getString("identifier"), helperElement);
                }

                while(entryHelper.has("parentIdentifier") && !"".equals(entryHelper.optString("parentIdentifier", ""))) {
                    String parentId = entryHelper.getString("parentIdentifier");

                    if (tableroDeControl.containsKey(parentId)) {
                        Element existingParent = tableroDeControl.get(parentId);
                        if (helperElement.getParentNode() == null) {
                            existingParent.appendChild(helperElement);
                        }
                        break;
                    }

                    prevHelperElement = helperElement;
                    entryHelper = multisitios.get(parentId);
                    if (entryHelper == null) {
                        break;
                    }

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
                if (entryHelper != null) {
                    helperElement = tableroDeControl.get(element);
                    appendMisHijos(entryHelper, helperElement, tableroDeControl, multisitios, doc);
                }
            }

            for(org.json.JSONObject laRaiz : rescataLaRaiz) {
                helperElement = tableroDeControl.get(laRaiz.getString("identifier"));
                if(helperElement != null) {
                    log("La raiz: " + helperElement.getAttribute("ID"));
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

            RestClient batchClient = new RestClient("Content-Type: application/xml", "Accept: application/xml");
            String atgResponse;
			try {
				atgResponse = batchClient.getRequest("POST", PropertiesManager.get("p360.contingency.out.url_atg"), xmlOutput);
				log("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())
						+ "] (Ecomm) request sent: " + atgResponse);
			} catch (mx.com.liverpool.p360.services.core.ServiceUnavailableException | IOException e) {
				e.printStackTrace();
			}
            try {
                java.nio.file.Files.writeString(java.nio.file.Paths.get(outputXmlFile), xmlOutput, java.nio.charset.StandardCharsets.UTF_8);
                log("Done. " + new RESTWorkshop().formatTime(System.currentTimeMillis() - init));
                return outputXmlFile;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (TransformerException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        log("Done. " + new RESTWorkshop().formatTime(System.currentTimeMillis() - init));
        return null;
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
                    if (entryElement != null) {
                        entryElement.appendChild(myElement);
                    }
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

        // Control de unicidad de atributos locales del Nodo actual
        java.util.Set<String> atributosAgregados = new java.util.HashSet<>();

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

            // Inyectamos el parentIdentifier nativo calculado por el script (Nivel > 1)
            if (node.has("parentIdentifier") && aux > 1) {
                value = doc.createElement("Value");
                value.setAttribute("Changed", "true");
                value.setAttribute("AttributeID", "parentCategoryID");
                value.setTextContent(node.getString("parentIdentifier"));
                metaData.appendChild(value);
                atributosAgregados.add("parentCategoryID");
            }

            if(node.has("metadata")) {
                gpfs = node.getJSONArray("metadata");
                log(node.getString("identifier") + ": " + node.get("metadata"));
                if(gpfs != null){
                    for(int i=0; i<gpfs.length(); i++) {
                        gpf = gpfs.getJSONObject(i);
                        String featureKey = gpf.getString("featureKey");
                        String featureValue = gpf.getString("featureValue");

                        if("".equals(featureValue))
                            continue;

                        // Si ya procesamos esta propiedad, evitamos agregarla por duplicado
                        if (atributosAgregados.contains(featureKey)) {
                            continue;
                        }

                        if ("parentCategoryID".equals(featureKey) || "department".equals(featureKey)) {
                            continue;
                        }

                        // Ignoramos el DisplayName que viene de REST
                        if("DisplayName".equals(featureKey)) {
                            continue;
                        } else if("groupType".equals(featureKey)) {
                            groupType = !"".equals(featureValue) ? featureValue : null;
                            value = doc.createElement("Value");
                            value.setAttribute("Changed", "true");
                            value.setAttribute("AttributeID", "groupType");
                            value.setAttribute("ID", "Optics".equals(groupType) ? "3" : "MAC Non-Collection".equals(groupType) ? "2" : "MAC Collection".equals(groupType) ? "1" : "0");
                            value.setTextContent(groupType != null ? groupType : "Not Specified");
                            metaData.appendChild(value);
                            atributosAgregados.add("groupType");
                        } else if("isBrand".equals(featureKey)) {
                            value = doc.createElement("Value");
                            value.setAttribute("Changed", "true");
                            value.setAttribute("AttributeID", "isBrand");
                            value.setAttribute("ID", Boolean.parseBoolean(featureValue) ? "1" : "0");
                            value.setTextContent( Boolean.parseBoolean(featureValue) ? "True" : "False" );
                            metaData.appendChild(value);
                            atributosAgregados.add("isBrand");
                        } else if("isBrandLanding".equals(featureKey)) {
                            value = doc.createElement("Value");
                            value.setAttribute("Changed", "true");
                            value.setAttribute("AttributeID", "isBrandLanding");
                            value.setAttribute("ID", Boolean.parseBoolean(featureValue) ? "1" : "0");
                            value.setTextContent( Boolean.parseBoolean(featureValue) ? "True" : "False" );
                            metaData.appendChild(value);
                            atributosAgregados.add("isBrandLanding");
                        } else if("allowGiftMessage".equals(featureKey)) {
                            value = doc.createElement("Value");
                            value.setAttribute("Changed", "true");
                            value.setAttribute("AttributeID", "allowGiftMessage");
                            value.setAttribute("ID", Boolean.parseBoolean(featureValue) ? "Y" : "N");
                            value.setTextContent( Boolean.parseBoolean(featureValue) ? "True" : "False" );
                            metaData.appendChild(value);
                            atributosAgregados.add("allowGiftMessage");
                        } else if("sentToFA".equals(featureKey)) {
                            value = doc.createElement("Value");
                            value.setAttribute("Changed", "true");
                            value.setAttribute("AttributeID", "sentToFA");
                            value.setAttribute("ID", Boolean.parseBoolean(featureValue) ? "1" : "0");
                            value.setTextContent( Boolean.parseBoolean(featureValue) ? "True" : "False" );
                            metaData.appendChild(value);
                            atributosAgregados.add("sentToFA");
                        } else if("skipInventory".equals(featureKey)) {
                            value = doc.createElement("Value");
                            value.setAttribute("Changed", "true");
                            value.setAttribute("AttributeID", "skipInventory");
                            value.setAttribute("ID", "skip".equals(featureValue) ? "1" : "0");
                            value.setTextContent(featureValue);
                            metaData.appendChild(value);
                            atributosAgregados.add("skipInventory");
                        } else if("giftMessage".equals(featureKey)) {
                            value = doc.createElement("Value");
                            value.setAttribute("Changed", "true");
                            value.setAttribute("AttributeID", "giftMessage");
                            value.setAttribute("ID", "allow".equals(featureValue) ? "1" : "0");
                            value.setTextContent(featureValue);
                            metaData.appendChild(value);
                            atributosAgregados.add("giftMessage");
                        } else if("DeliveringToExternalSystems".equals(featureKey) || "LastUserDeliverIssuer".equals(featureKey)){
                            // Omitidos
                        } else {
                            value = doc.createElement("Value");
                            value.setAttribute("Changed", "true");
                            value.setAttribute("AttributeID", featureKey);
                            value.setTextContent(featureValue);
                            metaData.appendChild(value);
                            atributosAgregados.add(featureKey);
                        }
                    }
                    if(node.has("keywords") && !atributosAgregados.contains("KeyWords")) {
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
                        if(multiValue.getChildNodes().getLength() > 0) {
                            metaData.appendChild(multiValue);
                            atributosAgregados.add("KeyWords");
                        }
                    }
                }
            }

            // Forzamos a que el DisplayName se asigne siempre usando el name_es
            value = doc.createElement("Value");
            value.setAttribute("Changed", "true");
            value.setAttribute("AttributeID", "DisplayName");
            String nameEs = node.optString("name_es", "");
            value.setTextContent(!"".equals(nameEs) ? nameEs.replaceAll(" \\(.+\\)", "") : "");
            metaData.appendChild(value);
            atributosAgregados.add("DisplayName");

            if (!atributosAgregados.contains("groupType")) {
                value = doc.createElement("Value");
                value.setAttribute("Changed", "true");
                value.setAttribute("AttributeID", "groupType");
                value.setAttribute("ID", "0");
                value.setTextContent("Not Specified");
                metaData.appendChild(value);
                atributosAgregados.add("groupType");
            }

            // Filtrado por niveles aux exactos
            if ((aux == 0 || aux == 1) && !atributosAgregados.contains("department")) {
                if (aux == 0) {
                    department = node.optString("identifier", null);
                } else if (aux == 1) {
                    department = node.optString("parentIdentifier", null);
                }

                if (department != null) {
                    String deptClean = department.trim();
                    value = doc.createElement("Value");
                    value.setAttribute("Changed", "true");
                    value.setAttribute("AttributeID", "department");
                    value.setAttribute("ID",
                            "BabiesRUs".equalsIgnoreCase(deptClean) ? "BRU"
                                    : "Banana Republic".equalsIgnoreCase(deptClean) ? "BNR"
                                    : "Dupuis".equalsIgnoreCase(deptClean) ? "DPS"
                                    : "Fabletics".equalsIgnoreCase(deptClean) ? "FAB"
                                    : "GAP".equalsIgnoreCase(deptClean) ? "106"
                                    : "Liverpool".equalsIgnoreCase(deptClean) ? "NA"
                                    : "Pottery Barn".equalsIgnoreCase(deptClean) ? "104"
                                    : "Pottery Barn Kids".equalsIgnoreCase(deptClean) ? "105"
                                    : "Suburbia".equalsIgnoreCase(deptClean) ? "SB"
                                    : "ToysRUs".equalsIgnoreCase(deptClean) ? "424"
                                    : "West Elm".equalsIgnoreCase(deptClean) ? "107"
                                    : "LIVESTORE".equalsIgnoreCase(deptClean) ? "LVS"
                                    : "William Sonoma".equalsIgnoreCase(deptClean) ? "307" : "");

                    value.setTextContent(department);
                    metaData.appendChild(value);
                    atributosAgregados.add("department");
                }
            }

            if(metaData.getChildNodes().getLength() > 0) {
                classificationElement.appendChild(metaData);
            }
            return classificationElement;
        }
        return null;
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
            } while(currentIndex < totalSize);
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
        } catch(org.json.JSONException e) {
            log(rawResponse);
            e.printStackTrace();
        }
        return losepas;
    }

    private java.util.Map<String, org.json.JSONArray> prestameLosGoupFeatures(){
        java.util.Map<String, org.json.JSONArray> losesos = new java.util.TreeMap<>();
        RESTWrapper rw = new RESTWrapper();
        java.util.Map<String, String> qp0 = new java.util.HashMap<>();
        qp0.put("structure", "Sitios Web");
        qp0.put("fields", 
        		   "StructureAttribute.Identifier"
        		+ ",StructureGroupAttributeValue.Value"
        		+ ",StructureGroupAttributeValue.Identifier"
        		+ ",StructureGroupAttributeValue.LanguageID"
        	);
        qp0.put("pageSize", "5000");
        rw.collectData("list", "StructureGroup", "StructureGroupAttribute", "byStructure", qp0, row_ -> {
        	org.json.JSONArray values_ = row_.getJSONArray("values");
        	if("DEFAULT".equals(values_.getString(2)) && "10".equals(values_.getString(3))) {
	        	String objectId = row_.getJSONObject("object").getString("id");
	        	org.json.JSONArray arr = losesos.get(objectId);
	            if(arr == null) {
	                arr = new org.json.JSONArray();
	                losesos.put(objectId, arr);
	            }
	            if("categoryStartDate".equals(values_.getString(0)) || "categoryEndDate".equals(values_.getString(0))) {
	                arr.put(new org.json.JSONObject().put("featureKey", values_.getString(0)).put("featureValue", fixDateFormat( values_.getString(1) ) + " 00:00:00") );
	            } else {
	                arr.put(new org.json.JSONObject().put("featureKey", values_.getString(0)).put("featureValue", values_.getString(1)) );
	            }
        	}
        });
        return losesos;
    }

    private String fixDateFormat(String d) {
        String val = d;
        if(!d.matches("[0-9]{4}-.+")) {
            if(d.matches("[0-9]{2}.[0-9]{2}.[0-9]{4}")) {
                try {
                    val = new java.text.SimpleDateFormat("yyyy-MM-dd").format( new java.text.SimpleDateFormat("dd" + d.charAt(2) + "MM" + d.charAt(2) + "yyyy").parse(d));
                } catch(java.text.ParseException e) {
                    e.printStackTrace();
                }
            }
        }
        return val;
    }

    private void log(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","generateHierarchy.log").toString(), true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "]  " + message);
        } catch (java.io.IOException e) {
            // Silenced
        }
    }
}