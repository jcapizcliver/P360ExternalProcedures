package mx.com.liverpool.p360.services.core.temp.exports;

import java.io.IOException;
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
import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RestClient;

public class HierarchySitiosWeb4Jdbc {

    private static final String urlDeATG =
            PropertiesManager.get("p360.contingency.out.url_atg");

    private static final RESTWrapper rw = new RESTWrapper();
    private final DBAccessDataStub dastub = new DBAccessDataStub(new ELog() {
        @Override
        public void logE(Exception e) {
            HierarchySitiosWeb4Jdbc.this.log("ERR: " + e.getMessage());
        }

        @Override
        public void log(String message) {
            HierarchySitiosWeb4Jdbc.this.log(message);
        }
    });

    private final java.nio.file.Path baseDirPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_dir_to_mirror_out_files"), "ToEcommerce");
    private final String baseDir = baseDirPath.toString();
    private final String outputXmlFile = java.nio.file.Paths.get(baseDir, System.currentTimeMillis() + "_pépeleJairarqui.xml").toString();

    public static void main(String[] args) throws ServiceUnavailableException {
        HierarchySitiosWeb4Jdbc h = new HierarchySitiosWeb4Jdbc();
        h.createHierarchyFile( new String[] {"cat5800034"} );
    }

    public java.util.List<org.json.JSONObject> getSelectableHierarchyRows() {
        return dastub.getWebHierarchyRows(10);
    }

    public PublicationResult publishHierarchy(
            String[] hierarchyIdentifiers)
            throws ServiceUnavailableException, IOException {

        if (hierarchyIdentifiers == null
                || hierarchyIdentifiers.length == 0) {

            throw new IllegalArgumentException(
                    "Debe seleccionar al menos una estructura de jerarquía.");
        }

        createHierarchyFile(hierarchyIdentifiers);

        java.nio.file.Path xmlPath =
                java.nio.file.Paths.get(outputXmlFile);

        if (!java.nio.file.Files.isRegularFile(xmlPath)) {
            throw new IOException(
                    "No se generó el archivo XML: " + xmlPath);
        }

        String xmlOutput =
                java.nio.file.Files.readString(
                        xmlPath,
                        java.nio.charset.StandardCharsets.UTF_8);

        RestClient batchClient =
                new RestClient(
                        "Content-Type: application/xml",
                        "Accept: application/xml");

        String atgResponse =
                batchClient.getRequest(
                        "POST",
                        urlDeATG,
                        xmlOutput);

        log(
                "(ATG) Hierarchy request sent for "
                + java.util.Arrays.toString(hierarchyIdentifiers)
                + ": "
                + atgResponse);

        return new PublicationResult(
                xmlPath,
                xmlOutput,
                atgResponse);
    }

    public static final class PublicationResult {
        private final java.nio.file.Path xmlPath;
        private final String xml;
        private final String atgResponse;

        private PublicationResult(
                java.nio.file.Path xmlPath,
                String xml,
                String atgResponse) {

            this.xmlPath = xmlPath;
            this.xml = xml;
            this.atgResponse = atgResponse;
        }

        public java.nio.file.Path getXmlPath() {
            return xmlPath;
        }

        public String getXml() {
            return xml;
        }

        public String getAtgResponse() {
            return atgResponse;
        }
    }

    private void createHierarchyFile(String[] ofInterest) throws ServiceUnavailableException {
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

    private java.util.Map<String, org.json.JSONObject> precargaJerarquia(
            String structureId,
            java.util.LinkedList<org.json.JSONObject> ondeVaLaRaiz) {

        java.util.Map<String, org.json.JSONObject> losepas =
                new java.util.TreeMap<>();

        java.util.Map<String, org.json.JSONArray> atributos =
                prestameLosGoupFeatures();

        java.util.Map<String, org.json.JSONArray> sinonimos =
                dastub.getWebHierarchySynonyms(10);

        java.util.List<org.json.JSONObject> rows =
                dastub.getWebHierarchyRows(10);

        for (org.json.JSONObject row : rows) {
            String identifier = row.optString("identifier", "");

            if (identifier.isBlank()) {
                continue;
            }

            String objectID =
                    row.optLong("structureGroupID") + "@12000";

            org.json.JSONArray metadata = atributos.get(objectID);

            if (metadata == null) {
                metadata = atributos.get(
                        String.valueOf(
                                row.optLong("structureGroupID")));
            }

            org.json.JSONArray keywords = sinonimos.get(identifier);

            losepas.put(
                    identifier,
                    new org.json.JSONObject()
                            .put("identifier", identifier)
                            .put(
                                    "name_es",
                                    row.optString("name", ""))
                            .put(
                                    "parentIdentifier",
                                    row.optString(
                                            "parentIdentifier",
                                            ""))
                            .put(
                                    "metadata",
                                    metadata == null
                                            ? new org.json.JSONArray()
                                            : metadata)
                            .put(
                                    "keywords",
                                    keywords == null
                                            ? new org.json.JSONArray()
                                            : keywords));
        }

        for (org.json.JSONObject node : losepas.values()) {
            node.put(
                    "level",
                    calculateLevel(
                            node,
                            losepas,
                            new java.util.HashSet<>()));
        }

        java.util.List<org.json.JSONObject> entradasJerarquia =
                new java.util.ArrayList<>(losepas.values());

        java.util.Collections.sort(
                entradasJerarquia,
                (o1, o2) -> {
                    int cmp = Integer.compare(
                            o1.getInt("level"),
                            o2.getInt("level"));

                    if (cmp == 0) {
                        cmp = o1.optString(
                                "parentIdentifier",
                                "").compareTo(
                                        o2.optString(
                                                "parentIdentifier",
                                                ""));
                    }

                    if (cmp == 0) {
                        cmp = o1.getString(
                                "identifier").compareTo(
                                        o2.getString(
                                                "identifier"));
                    }

                    return cmp;
                });

        for (org.json.JSONObject entrada : entradasJerarquia) {
            String parentIdentifier =
                    entrada.optString("parentIdentifier", "");

            org.json.JSONObject parent =
                    losepas.get(parentIdentifier);

            if (parent == null) {
                ondeVaLaRaiz.addLast(entrada);
                continue;
            }

            if (!parent.has("children")) {
                parent.put(
                        "children",
                        new org.json.JSONArray());
            }

            parent.getJSONArray("children").put(entrada);
        }

        if (ondeVaLaRaiz.isEmpty()
                && !entradasJerarquia.isEmpty()) {

            ondeVaLaRaiz.addLast(
                    entradasJerarquia.get(0));
        }

        return losepas;
    }

    private int calculateLevel(
            org.json.JSONObject node,
            java.util.Map<String, org.json.JSONObject> nodes,
            java.util.Set<String> visited) {

        String identifier =
                node.optString("identifier", "");

        if (!visited.add(identifier)) {
            log("WARN: hierarchy cycle detected at " + identifier);
            return 1;
        }

        String parentIdentifier =
                node.optString("parentIdentifier", "");

        org.json.JSONObject parent =
                nodes.get(parentIdentifier);

        if (parent == null) {
            return 1;
        }

        return 1
                + calculateLevel(
                        parent,
                        nodes,
                        visited);
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
            } else {
                log("ERROR: " + rw.getRw().getRawResponse());
            }
        } while(currentIndex < totalSize);
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
