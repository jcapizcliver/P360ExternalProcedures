package com.jcapiz.memelos.xmlprocessor;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.jcapiz.memelos.misc.RestClient;

public class FlatenPPHFromStepXML {

	private DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	private DocumentBuilder builder =null;
	private Document document = null;
	private NodeList parentNodes = null;
	private Element parentElement = null;

	private RestClient rc = new RestClient("Content-Type: application/json", "Accept: application/json", "Authorization: Basic YWRtaW46bGl2ZXJwb29s");

	private java.util.Map<String, Integer> lookupMap = new java.util.TreeMap<>();

	private Integer attributeGroupLookupInternalId = null;

	private void processFile() throws ParserConfigurationException, FileNotFoundException, SAXException, IOException {
		if(this.builder == null)
	    {
	      this.builder = this.factory.newDocumentBuilder();
	    }
	   this.document = this.builder.parse(new java.io.FileInputStream("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml"));
	   this.document.getDocumentElement().normalize();
	   this.parentNodes = this.document.getElementsByTagName("STEP-ProductInformation");
	   this.parentElement = (Element) this.parentNodes.item(0);
	   NodeList attributeGroupNodes = ((Element) parentElement.getElementsByTagName("AttributeGroupList").item(0)).getElementsByTagName("AttributeGroup");
	   NodeList attributeListNodes = parentElement.getElementsByTagName("AttributeList").item(0).getChildNodes();
	   NodeList products = parentElement.getElementsByTagName("Products").item(0).getChildNodes();
	   org.json.JSONArray rows = new org.json.JSONArray();
	   java.util.Map<String, org.json.JSONObject> attributesReference = new java.util.TreeMap<>();
	   for(int i=0; i<attributeListNodes.getLength(); i++) {
		   if(Node.ELEMENT_NODE == attributeListNodes.item(i).getNodeType()) {
			   collectAttributes(attributeListNodes.item(i), attributesReference);
		   }
	   }
	   attributesReference.forEach((k,v)->{ String baseType = v.getString("baseType"); if(v.has("listOfValues") && !"".equals(v.getString("listOfValues"))) {
		v.put("baseType", "List of Values");
	} else if(!"".equals(v.getString("baseType"))) {
		v.put("baseType", translateBaseType(v.getString("baseType")));
	} });
//	   attributesReference.forEach((k,v)->System.out.println(k + "_" + v));
	   java.util.Map<String, Integer> types = new java.util.TreeMap<>();
	   attributesReference.forEach((k,v)-> { try{ if(!"".equals(v.getString("baseType"))){ Integer freq = null; freq = types.get(v.getString("baseType")); types.put(v.getString("baseType"), (freq == null ? 0 : freq) + 1); } }catch(org.json.JSONException e) {} } );
	   java.util.LinkedList<java.util.Map.Entry<String, Integer>> typesEntries = new java.util.LinkedList<>( types.entrySet() );
	   java.util.Collections.sort(typesEntries, (o1,o2)-> o2.getValue().compareTo(o1.getValue()) );
//	   System.out.println("*****");
//	   typesEntries.forEach(System.out::println);
//	   System.exit(0);

	   for(int i=0; i<products.getLength(); i++) {
		   if(Node.ELEMENT_NODE == products.item(i).getNodeType() )
		 {
			processProductNode((Element)products.item(i), null, null, "", rows);
//			   System.out.println(products.item(i).getNodeName());
		}
	   }
	   java.util.Map<String, org.json.JSONObject> structureEntries = new java.util.TreeMap<>();
	   java.util.Map<String, Integer> levelWithAttributesCount = new java.util.TreeMap<>();
	   String levelsbs = null;
	   Integer freq = null;
	   org.json.JSONArray soloPlantillas = new org.json.JSONArray();
	   for(int i=0; i<rows.length(); i++) {
//		   System.out.println(rows.getJSONObject(i));
		   structureEntries.put(rows.getJSONObject(i).getString("ID"), rows.getJSONObject(i));
		   if(rows.getJSONObject(i).getString("ID").startsWith("EU4")) {
			   soloPlantillas.put(rows.getJSONObject(i));
		   }
		   if(rows.getJSONObject(i).getJSONObject("Attributes").length() > 0) {
			   levelsbs = rows.getJSONObject(i).getString("ID").substring(0, 3);
			   freq = levelWithAttributesCount.get(levelsbs);
			   levelWithAttributesCount.put(levelsbs, (freq == null ? 0 : freq) +1);
		   }
	   }
	   java.util.Map<String, org.json.JSONObject> superiorAttributesMap = new java.util.TreeMap<>();
	   structureEntries.forEach((k,v)->{ if(v.getJSONObject("Attributes").length() > 0 && !"EU4".equals(k.substring(0,3))) {
		superiorAttributesMap.put(k, v);
	} });
//	   java.util.LinkedList<java.util.Map.Entry<String, Integer>> freqList = new java.util.LinkedList<>(levelWithAttributesCount.entrySet());
//	   java.util.Collections.sort(freqList, (o1,o2)-> o2.getValue().compareTo(o1.getValue()));
//	   System.out.println("***");
//	   freqList.forEach(System.out::println);
//	   System.exit(0);
//	   try( java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\ElementsForStructureGroup"))) ){ pw.println(structureEntries); }catch(java.io.IOException e) { e.printStackTrace(); }
	   int currentIndex = 0;
	   int totalSize = 0;
	   String url = null;
	   String rawResponse = null;
	   org.json.JSONObject response = null;

	   /**
	    * Leer datos de muestra desde HPM para clasificar atributos de plantillas.
	    *
	    ***************************************************************************/
	   System.out.println("Gonna do this...");
	   java.util.Map<String, String> currentLevels = new java.util.TreeMap<>();
	   java.util.Map<String, String> caracteristicasSeccion = new java.util.TreeMap<>();
	   currentIndex = 0;
	   do {
		   url = "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/StandardizationValue/byDictionary?dictionary=" + java.net.URLEncoder.encode("ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla_bpk", "UTF-8") + "&fields=" + java.net.URLEncoder.encode("StandardizationValue.StructureGroup->LookupValue.Code,StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.Property->LookupValue.Code,StandardizationValue.PropertyValue", "UTF-8") + "&query=" + java.net.URLEncoder.encode("StandardizationValue.Property->LookupValue.Code equals \"VendorCenterSection\"", "UTF-8") + "&pageSize=400&startIndex=" + currentIndex;
		   try{
			   rawResponse = rc.getRequest("GET", url, null);
			   response = new org.json.JSONObject(rawResponse);
			   rows = response.getJSONArray("rows");
			   for(int i=0; i<rows.length(); i++) {
				   caracteristicasSeccion.put(rows.getJSONObject(i).getJSONArray("values").getString(1), rows.getJSONObject(i).getJSONArray("values").getString(3));
				   currentIndex++;
			   }
			   totalSize = response.getInt("totalSize");
		   }catch(Exception e) {
			   e.printStackTrace();
			   System.out.println(rawResponse);
		   }
	   }while(currentIndex < totalSize);
	   /**
	    * Obtener los elementos del diccionario actual de muestra por característica, como referencia.
	    ******************************************************************************************************/
	   System.out.println("Gonna do this again...");
	   String currentCharacteristic = null;
	   String previousCharacteristic = null;
	   org.json.JSONObject characteristicProperties = new org.json.JSONObject();
	   java.util.Map<String, org.json.JSONObject> characteristicPropertiesMap = new java.util.TreeMap<>();
	   currentIndex = 0;
	   do {
		   url = "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/StandardizationValue/byDictionary?dictionary=" + java.net.URLEncoder.encode("ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla_bpk", "UTF-8") + "&orderBy=1-ASC&fields=" + java.net.URLEncoder.encode("StandardizationValue.StructureGroup->LookupValue.Code,StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.Property->LookupValue.Code,StandardizationValue.PropertyValue", "UTF-8") + "&pageSize=400&startIndex=" + currentIndex;
		   try{
			   rawResponse = rc.getRequest("GET", url, null);
			   response = new org.json.JSONObject(rawResponse);
			   rows = response.getJSONArray("rows");
			   for(int i=0; i<rows.length(); i++) {
				   currentCharacteristic = rows.getJSONObject(i).getJSONArray("values").getString(1);
				   if(previousCharacteristic != null && !previousCharacteristic.equals(currentCharacteristic)) {
					   characteristicPropertiesMap.put(previousCharacteristic, characteristicProperties);
					   characteristicProperties = new org.json.JSONObject();
				   }
				   characteristicProperties.put(rows.getJSONObject(i).getJSONArray("values").getString(2), rows.getJSONObject(i).getJSONArray("values").getString(3));
				   currentIndex++;
				   previousCharacteristic = currentCharacteristic;
			   }
			   totalSize = response.getInt("totalSize");
		   }catch(Exception e) {
			   e.printStackTrace();
			   System.out.println(rawResponse);
		   }
	   }while(currentIndex < totalSize);
	   characteristicPropertiesMap.put(previousCharacteristic, characteristicProperties);
	   System.out.println("Gonna collect characteristics...");
	   java.util.Set<String> currentlyActiveCharacteristics = new java.util.TreeSet<>();
	   int notActive = 0;
	   currentIndex = 0;
	   do {
		   url = "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/Characteristic/bySearch?query=" + java.net.URLEncoder.encode("not Characteristic.Identifier is empty", "UTF-8") + "&fields=" + java.net.URLEncoder.encode("Characteristic.Identifier,Characteristic.IsActive", "UTF-8") + "&pageSize=400&startIndex=" + currentIndex;
		   try{
			   rawResponse = rc.getRequest("GET", url, null);
			   response = new org.json.JSONObject(rawResponse);
			   rows = response.getJSONArray("rows");
			   for(int i=0; i<rows.length(); i++) {
				   currentCharacteristic = rows.getJSONObject(i).getJSONArray("values").getString(0);
				   if(!"true".equals(rows.getJSONObject(i).getJSONArray("values").getString(1))) {
					   notActive++;
//					   System.out.println("\t" + rows.getJSONObject(i).getJSONArray("values").getString(0));
				   } else {
					   currentlyActiveCharacteristics.add(currentCharacteristic);
				   }
				   currentIndex++;
				   previousCharacteristic = currentCharacteristic;
			   }
			   totalSize = response.getInt("totalSize");
		   }catch(Exception e) {
			   e.printStackTrace();
			   System.out.println(rawResponse);
			   System.exit(0);
		   }
	   }while(currentIndex < totalSize);

	   System.out.println("Currently active characteristics: " + currentlyActiveCharacteristics.size() + ", not active: " + notActive);

	   java.util.Map<String, org.json.JSONObject> atributosGlobalesConSeccion = new java.util.TreeMap<>();
	   java.util.Set<String> seccionesParaLasQueSeEncontroUnAtributoDeNivelSuperior = new java.util.TreeSet<>();
	   java.util.Set<String> namesToSearch = new java.util.TreeSet<>();
	   superiorAttributesMap.forEach((k,v)->{ for(String name : org.json.JSONObject.getNames(v.getJSONObject("Attributes"))) {
//		   if(name.contains("Description")) {
//			   System.out.println("********************** FOUND IT (" + name + ") **********************");
//		   }
		   if("MainBarCode".equals(name)) {
			   System.out.println("\tContained in characteristics: " + (characteristicPropertiesMap.containsKey(name)));
			   System.out.println("\tAttributes map contains VendorCenterSection: " + (characteristicPropertiesMap.get(name).has("VendorCenterSection")));
		   }
		   if(!characteristicPropertiesMap.containsKey(name)) {
			   namesToSearch.add(name);
//			   System.out.println("-->" + name + " (" + attributesReference.get(name) + ")");
		   }else {
			   if(!characteristicPropertiesMap.get(name).has("VendorCenterSection")) {
//				   System.out.println("An attribute without a section in currently loaded data: " + name);
			   }else {
				   atributosGlobalesConSeccion.put(name, v.getJSONObject("Attributes").getJSONObject(name));
				   seccionesParaLasQueSeEncontroUnAtributoDeNivelSuperior.add(characteristicPropertiesMap.get(name).getString("VendorCenterSection"));
//				   System.out.println("\tGot it (" + name + "), " + characteristicPropertiesMap.get(name).getString("VendorCenterSection"));
			   }
		   }
	   } });
	   System.out.println("Atrubutos con seccion: " + atributosGlobalesConSeccion.size());
//	   namesToSearch.forEach(System.out::println);
//	   seccionesParaLasQueSeEncontroUnAtributoDeNivelSuperior.forEach(System.out::println);

	   /********
	    *   Carga campos calculados
	    ******************************/

	   String[][] pieces = new String[][] { atributosCalculadosECC.split("\r\n") , atributosCalculadosMKP.split("\r\n"), atributosCalculadosSBB.split("\r\n")};
	   java.util.Set<String> atributosCalculados = new java.util.TreeSet<>();
	   for (String[] element : pieces) {
		   for (String element2 : element) {
			   if(element2 != null && !"".equals(element2.trim())) {
				   atributosCalculados.add(element2.replaceAll("( |\\().+", "").trim());
			   }
		   }
	   }

//	   System.out.println("MOCK: " + characteristicPropertiesMap.get("MainBarCode"));
//	   System.out.println(atributosGlobalesConSeccion.get("MainBarCode"));
//	   System.exit(0);

	   org.json.JSONArray payloadRows = new org.json.JSONArray();
	   org.json.JSONObject plantilla = null;
	   org.json.JSONObject predefinedAttribute = null;
	   org.json.JSONObject atributoGlobal = null;
	   String templateId = null;
	   String seccion = null;
	   String dataType = null;
	   String sendToVendorCenter = null;
	   String description = null;
	   String maxLength = null;
	   String isEditable = null;
	   String isMultiselect = null;
	   String isMandatory = null;
	   String allowedBusiness = null;
	   String min = null;
	   String max = null;
	   String listOfValues = null;
	   String listOfValuesValidValues = null;
	   String numberOfDetailImages = null;
	   String numberOfIllustrationImages = null;
	   String numberOfSmoshImages = null;
	   String numberOfLiverpoolManuals = null;
	   String numberOfOwnersManual = null;
	   String numberOfNoms = null;
	   String creationType = "Proposal";

	   java.util.Map<String, java.util.Map<String, String>> lookupMaps = new java.util.TreeMap<>();
	   java.util.Map<String, String> lookupMap = null;

	   StringBuilder sb = new StringBuilder();
	   java.util.Map<String, org.json.JSONObject> missingCharacteristics = new java.util.TreeMap<>();

	   org.json.JSONObject values = null;
	   org.json.JSONObject templateAttribute = null;
	   org.json.JSONArray valueFilter = null;
	   String lookupCode = null;

	   /*
	   String js1 = null;
	   String js2 = null;

	   try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\tsurrea1.json")))){ String ln = null; StringBuilder sb1 = new StringBuilder(); while((ln = br.readLine()) != null) sb1.append(ln); js1 = sb1.toString(); }catch(java.io.IOException e) { e.printStackTrace(); }
	   try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\tsurrea2.json")))){ String ln = null; StringBuilder sb1 = new StringBuilder(); while((ln = br.readLine()) != null) sb1.append(ln); js2 = sb1.toString(); }catch(java.io.IOException e) { e.printStackTrace(); }

	   org.json.JSONObject json1 = new org.json.JSONObject(js1);
	   org.json.JSONObject json2 = new org.json.JSONObject(js2);

	   org.json.JSONArray currentArray = null;
	   org.json.JSONArray partnerArray = null;

	   org.json.JSONObject ce = null;
	   org.json.JSONObject pe = null;
	   boolean found = false;
	   System.out.println("Nervergonnagiveyouup.");
	   for(String maNeim : org.json.JSONObject.getNames(json1)) {
		   if(json1.get(maNeim) instanceof org.json.JSONArray) {
			   currentArray = json1.getJSONArray(maNeim);
			   partnerArray = json2.getJSONArray(maNeim);
			   for(int i=0; i<currentArray.length(); i++) {
				   ce = currentArray.getJSONObject(i);
				   for(int j=0; j<partnerArray.length(); j++) {
					   pe = partnerArray.getJSONObject(j);
					   if(pe.getString("name").equals(ce.getString("name"))) {
						   found = true;
						   break;
					   }
				   }
				   if(found) {
					   found = false;
					   continue;
				   }
				   seccion = maNeim;
				   allowedBusiness = ce.getString("allowedBusiness");
				   sendToVendorCenter = "1";
				   isEditable = ce.has("isEditable") ? ce.getString("isEditable") : "0";
				   isMandatory = ce.has("isMandatory") ? ce.getString("isMandatory") : "No";
				   String characteristicName = ce.getString("name");
				   templateId = "EU4-113578";
				   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "IsMandatory", isMandatory);
				   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "VendorCenterSection", seccion);
				   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "Business", allowedBusiness);
				   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "SentToVendorCenter", sendToVendorCenter);

				   description = ce.has("attributeHelpInformation") ? ce.getString("attributeHelpInformation") : null;
				   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "AttributeHelpInformation", description);

				   maxLength = ce.has("maxLength") ? ce.getString("maxLength") : null;
				   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "MaxLength", maxLength);

				   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "IsEditable", isEditable);

				   isMultiselect = ce.has("isMultiselect") ? ce.getString("isMultiselect") : null;
				   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "IsMultiselect", isMultiselect);

				   min = ce.has("min") ? ce.getString("min") : null;
				   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "Min", min);

				   max = ce.has("max") ? ce.getString("max") : null;
				   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "Max", max);

				   dataType = ce.has("dataType") ? ce.getString("dataType") : null;
				   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "DataType", dataType);

				   listOfValues = ce.has("listofValues") ? ce.getString("listofValues") : null;
				   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "ListOfValues", listOfValues);

				   if(listOfValues != null) {
					   try{
						   rawResponse = rc.getRequest("GET", "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/StandardizationValue/byDictionary?dictionary=" + java.net.URLEncoder.encode("ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla_bpk", "UTF-8") + "&fields=" + java.net.URLEncoder.encode("StandardizationValue.PropertyValue", "UTF-8") + "&query=" + java.net.URLEncoder.encode("StandardizationValue.Property->LookupValue.Code equals \"ListOfValuesFilter\" and StandardizationValue.PropertyValue equals \"" + characteristicName + "\"", "UTF-8"), null);
						   response = new org.json.JSONObject(rawResponse);
						   if(response.getJSONArray("rows").length() > 0) {
							   listOfValuesValidValues = response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0);
							   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "ListOfValuesFilter", listOfValuesValidValues);
						   }
					   }catch(Exception e) {
						   e.printStackTrace();
						   System.out.println(rawResponse);
					   }
				   }
			   }
		   }
	   }
	   try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\rows_not_found_in_conventional_template_attributes.json")))){ pw.println(new org.json.JSONObject()
				   .put("columns", new org.json.JSONArray()
					   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"))
					   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"))
					   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"))
					   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"))
					   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"))
				   )
				   .put("rows", payloadRows)
				   .toString()); }catch(java.io.IOException e) { e.printStackTrace(); }
	   try {
		   rawResponse = rc.getRequest("POST", "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/StandardizationValue", new org.json.JSONObject()
				   .put("columns", new org.json.JSONArray()
					   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"))
					   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"))
					   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"))
					   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"))
					   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"))
				   )
				   .put("rows", payloadRows)
				   .toString());
		   System.out.println("Inserted into dictionary response: " + rawResponse);
	   }catch(Exception e) {
		   e.printStackTrace();
	   }
	   System.exit(0);
	   */

	   payloadRows = new org.json.JSONArray();

	   for(int i=0; i<soloPlantillas.length(); i++) {
		   plantilla = soloPlantillas.getJSONObject(i);
		   templateId = plantilla.getString("ID");

		   if(!"EU4-28122113".equals(templateId)) {
			   continue;
		   }
		   System.out.println("We came here... " + templateId + "__<::>" + plantilla + "<::>__");

		   values = plantilla.getJSONObject("Values");
		   numberOfDetailImages = values.has("NumberOfDetailImages") ? values.getString("NumberOfDetailImages") : null;
		   numberOfIllustrationImages = values.has("NumberOfIllustrationImages") ? values.getString("NumberOfIllustrationImages") : null;
		   numberOfSmoshImages = values.has("NumberOfSmoshImages") ? values.getString("NumberOfSmoshImages") : null;
		   numberOfLiverpoolManuals = values.has("NumberOfLiverpoolManuals") ? values.getString("NumberOfLiverpoolManuals") : null;
		   numberOfOwnersManual = values.has("NumberOfOwnersManual") ? values.getString("NumberOfOwnersManual") : null;
		   numberOfNoms = values.has("NumberOfNoms") ? values.getString("NumberOfNoms") : null;

		   if(numberOfDetailImages != null) {
				addJSONToRow(payloadRows, templateId, "ProductImageDetail", creationType, "Min", numberOfDetailImages);
				addJSONToRow(payloadRows, templateId, "ProductImageDetail", creationType, "Max", numberOfDetailImages);
				addJSONToRow(payloadRows, templateId, "ProductImageDetail", creationType, "IsEditable", "1");
				addJSONToRow(payloadRows, templateId, "ProductImageDetail", creationType, "SentToVendorCenter", "1");
				addJSONToRow(payloadRows, templateId, "ProductImageDetail", creationType, "VendorCenterSection", "Fotografías");
				addJSONToRow(payloadRows, templateId, "ProductImageDetail", creationType, "VariantLevel", "1");
				addJSONToRow(payloadRows, templateId, "ProductImageDetail", creationType, "Business", "Liverpool Suburbia Marketplace");
		   }
		   if(numberOfIllustrationImages != null) {
				addJSONToRow(payloadRows, templateId, "Illustration", creationType, "Min", numberOfIllustrationImages);
				addJSONToRow(payloadRows, templateId, "Illustration", creationType, "Max", numberOfIllustrationImages);
				addJSONToRow(payloadRows, templateId, "Illustration", creationType, "IsEditable", "1");
				addJSONToRow(payloadRows, templateId, "Illustration", creationType, "SentToVendorCenter", "1");
				addJSONToRow(payloadRows, templateId, "Illustration", creationType, "VendorCenterSection", "Fotografías");
				addJSONToRow(payloadRows, templateId, "Illustration", creationType, "VariantLevel", "1");
				addJSONToRow(payloadRows, templateId, "Illustration", creationType, "Business", "Liverpool Suburbia Marketplace");
		   }
		   if(numberOfSmoshImages != null) {
				addJSONToRow(payloadRows, templateId, "ProductImageSmosh", creationType, "Min", numberOfSmoshImages);
				addJSONToRow(payloadRows, templateId, "ProductImageSmosh", creationType, "Max", numberOfSmoshImages);
				addJSONToRow(payloadRows, templateId, "ProductImageSmosh", creationType, "IsEditable", "1");
				addJSONToRow(payloadRows, templateId, "ProductImageSmosh", creationType, "SentToVendorCenter", "1");
				addJSONToRow(payloadRows, templateId, "ProductImageSmosh", creationType, "VendorCenterSection", "Fotografías");
				addJSONToRow(payloadRows, templateId, "ProductImageSmosh", creationType, "VariantLevel", "1");
				addJSONToRow(payloadRows, templateId, "ProductImageSmosh", creationType, "Business", "Liverpool Suburbia Marketplace");
		   }

		   if(numberOfLiverpoolManuals != null) {
			   addJSONToRow(payloadRows, templateId, "LiverpoolManual", creationType, "Min", numberOfLiverpoolManuals);
			   addJSONToRow(payloadRows, templateId, "LiverpoolManual", creationType, "Max", numberOfLiverpoolManuals);
			   addJSONToRow(payloadRows, templateId, "LiverpoolManual", creationType, "IsEditable", "1");
			   addJSONToRow(payloadRows, templateId, "LiverpoolManual", creationType, "SentToVendorCenter", "1");
			   addJSONToRow(payloadRows, templateId, "LiverpoolManual", creationType, "VendorCenterSection", "Multimedia");
			   addJSONToRow(payloadRows, templateId, "LiverpoolManual", creationType, "VariantLevel", "1");
			   addJSONToRow(payloadRows, templateId, "LiverpoolManual", creationType, "Business", "Liverpool Suburbia Marketplace");
		   }
		   if(numberOfOwnersManual != null) {
			   addJSONToRow(payloadRows, templateId, "OwnersManual", creationType, "Min", numberOfOwnersManual);
			   addJSONToRow(payloadRows, templateId, "OwnersManual", creationType, "Max", numberOfOwnersManual);
			   addJSONToRow(payloadRows, templateId, "OwnersManual", creationType, "IsEditable", "1");
			   addJSONToRow(payloadRows, templateId, "OwnersManual", creationType, "SentToVendorCenter", "1");
			   addJSONToRow(payloadRows, templateId, "OwnersManual", creationType, "VendorCenterSection", "Multimedia");
			   addJSONToRow(payloadRows, templateId, "OwnersManual", creationType, "VariantLevel", "1");
			   addJSONToRow(payloadRows, templateId, "OwnersManual", creationType, "Business", "Liverpool Suburbia Marketplace");
		   }
//		   if(numberOfNoms != null) {
//			   addJSONToRow(payloadRows, templateId, "", creationType, "", numberOfNoms);
//		   }

		   java.util.Set<String> bannedValues = new java.util.TreeSet<>();

		   for(String characteristicName : org.json.JSONObject.getNames(plantilla.getJSONObject("Attributes"))) {
			   if(!currentlyActiveCharacteristics.contains(characteristicName)) {
//				   System.out.println("----->" + characteristicName);
				   missingCharacteristics.put(characteristicName, attributesReference.get(characteristicName));
			   }else {
				   templateAttribute = plantilla.getJSONObject("Attributes").getJSONObject(characteristicName);
				   seccion = caracteristicasSeccion.get(characteristicName);
				   seccion = seccion == null || "".equals(seccion) ? "Atributos" : seccion;
				   predefinedAttribute = characteristicPropertiesMap.get(characteristicName);
				   atributoGlobal = attributesReference.get(characteristicName);
				   if(atributoGlobal != null) {
					   if(predefinedAttribute != null) {
						   allowedBusiness = predefinedAttribute.has("Business") ? predefinedAttribute.getString("Business") : null;
						   sendToVendorCenter = predefinedAttribute.getString("SentToVendorCenter");
						   isEditable = predefinedAttribute.getString("IsEditable");
					   }else {
						   allowedBusiness = "Liverpool";
						   sendToVendorCenter = "1";
						   if(!atributosCalculados.contains(characteristicName)) {
							isEditable = "1";
						}
					   }
					   isMandatory = templateAttribute.has("Mandatory") ? templateAttribute.getBoolean("Mandatory") ? "true" : "false" : "false";
					   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "IsMandatory", isMandatory);
					   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "VendorCenterSection", seccion);
					   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "Business", allowedBusiness);
					   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "SentToVendorCenter", sendToVendorCenter);

					   description = atributoGlobal.has("Description") ? atributoGlobal.getString("Description") : null;
					   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "AttributeHelpInformation", description);

					   maxLength = atributoGlobal.has("maxLength") ? atributoGlobal.getString("maxLength") : null;
					   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "MaxLength", maxLength);

					   isEditable = isEditable == null ? "1" : isEditable;
					   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "IsEditable", isEditable);

					   isMultiselect = atributoGlobal.has("Multivalued") ? atributoGlobal.getString("Multivalued") : null;
					   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "IsMultiselect", isMultiselect);

					   min = atributoGlobal.has("min") ? atributoGlobal.getString("min") : null;
					   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "Min", min);

					   max = atributoGlobal.has("max") ? atributoGlobal.getString("max") : null;
					   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "Max", max);

					   dataType = atributoGlobal.has("baseType") ? atributoGlobal.getString("baseType") : null;
					   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "DataType", dataType);

					   listOfValues = atributoGlobal.has("listOfValues") ? atributoGlobal.getString("listOfValues") : null;
					   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "ListOfValues", listOfValues);

					   listOfValuesValidValues = null;
					   valueFilter = templateAttribute.has("valueFilter") ? templateAttribute.getJSONArray("valueFilter") : null;
					   if(valueFilter != null && valueFilter.length() > 0) {
						   if(valueFilter.getJSONObject(0).has("Value")) {
							   // Search for each value
							   /*
							   lookupMap = lookupMaps.get(listOfValues);
							   if(lookupMap == null) {
								   lookupMap = new java.util.TreeMap<>();
								   for(int k=0; k<valueFilter.length(); k++) {
									   try{
										   rawResponse = rc.getRequest("GET", "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/LookupValue/bySearch?lookup=" + java.net.URLEncoder.encode(listOfValues, "UTF-8") + "&query=" + java.net.URLEncoder.encode("LookupValueLang.Name(es) equals \"" + valueFilter.getJSONObject(k).getString("Value") + "\"", "UTF-8") + "&fields=" + java.net.URLEncoder.encode("LookupValue.Code", "UTF-8") + "", null);
										   response = new org.json.JSONObject(rawResponse);
										   if(response.has("rows") && response.getJSONArray("rows").length() > 0) {
											   lookupMap.put(valueFilter.getJSONObject(k).getString("Value"), response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0));
											   sb.append(k == 0 ? "" : ",").append(response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0));
										   }else {
											   bannedValues.add(valueFilter.getJSONObject(k).getString("Value"));
											   System.out.println("A value was not found within its lookup in P360: " + valueFilter.getJSONObject(k).getString("Value") + " lk: " + listOfValues);
										   }
									   }catch(Exception e) {
										   e.printStackTrace();
										   System.out.println(rawResponse);
									   }
								   }
								   if(!lookupMaps.isEmpty())
									   lookupMaps.put(listOfValues, lookupMap);
							   }else {
								   for(int k=0; k<valueFilter.length(); k++) {
									   lookupCode = lookupMap.get(valueFilter.getJSONObject(k).getString("Value"));
									   if(lookupCode == null)
										   try{
											   rawResponse = rc.getRequest("GET", "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/LookupValue/bySearch?lookup=" + java.net.URLEncoder.encode(listOfValues, "UTF-8") + "&query=" + java.net.URLEncoder.encode("LookupValueLang.Name(es) equals \"" + valueFilter.getJSONObject(k).getString("Value") + "\"", "UTF-8") + "&fields=" + java.net.URLEncoder.encode("LookupValue.Code", "UTF-8") + "", null);
											   response = new org.json.JSONObject(rawResponse);
											   if(response.has("rows") && response.getJSONArray("rows").length() > 0) {
												   lookupMap.put(valueFilter.getJSONObject(k).getString("Value"), response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0));
												   sb.append(k == 0 ? "" : ",").append(response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0));
											   }else {
												   bannedValues.add(valueFilter.getJSONObject(k).getString("Value"));
												   System.out.println("A value was not found within its lookup in P360: " + valueFilter.getJSONObject(k).getString("Value") + " lk: " + listOfValues);
											   }
										   }catch(Exception e) {
											   e.printStackTrace();
											   System.out.println(rawResponse);
										   }
									   else {
										   sb.append(k == 0 ? "" : ",").append(lookupCode);
									   }
								   }
							   }
							   */
						   }else {
							   for(int k=0; k<valueFilter.length(); k++) {
								   sb.append(k == 0 ? "" : ",").append(valueFilter.getJSONObject(k).getString("ID"));
							   }
						   }
						   listOfValuesValidValues = sb.toString();
						   addJSONToRow(payloadRows, templateId, characteristicName, creationType, "ListOfValuesFilter", listOfValuesValidValues);
						   sb.setLength(0);
					   }else if(valueFilter == null) {
						   System.out.println("Como es esto posible (" + characteristicName + "): " + templateAttribute);
					   }
				   }
			   }
		   }
		   for(java.util.Map.Entry<String, org.json.JSONObject> atributoGlobalConSeccion : atributosGlobalesConSeccion.entrySet()) {
			   if(!currentlyActiveCharacteristics.contains(atributoGlobalConSeccion.getKey())) {
				   missingCharacteristics.put(atributoGlobalConSeccion.getKey(), attributesReference.get(atributoGlobalConSeccion.getKey()));
			   }else {
				   templateAttribute = atributoGlobalConSeccion.getValue();
				   seccion = caracteristicasSeccion.get(atributoGlobalConSeccion.getKey());
				   seccion = seccion == null || "".equals(seccion) ? "Atributos" : seccion;
				   predefinedAttribute = characteristicPropertiesMap.get(atributoGlobalConSeccion.getKey());
				   atributoGlobal = attributesReference.get(atributoGlobalConSeccion.getKey());
				   if(atributoGlobal != null) {
					   if(predefinedAttribute != null) {
						   allowedBusiness = predefinedAttribute.has("Business") ? predefinedAttribute.getString("Business") : null;
						   sendToVendorCenter = predefinedAttribute.getString("SentToVendorCenter");
						   isEditable = predefinedAttribute.getString("IsEditable");
					   }else {
						   allowedBusiness = "Liverpool";
						   sendToVendorCenter = "1";
						   if(!atributosCalculados.contains(atributoGlobalConSeccion.getKey())) {
							isEditable = "1";
						}
					   }
					   isMandatory = templateAttribute.has("Mandatory") ? templateAttribute.getBoolean("Mandatory") ? "true" : "false" : "false";
					   addJSONToRow(payloadRows, templateId, atributoGlobalConSeccion.getKey(), creationType, "IsMandatory", isMandatory);
					   addJSONToRow(payloadRows, templateId, atributoGlobalConSeccion.getKey(), creationType, "VendorCenterSection", seccion);
					   addJSONToRow(payloadRows, templateId, atributoGlobalConSeccion.getKey(), creationType, "Business", allowedBusiness);
					   addJSONToRow(payloadRows, templateId, atributoGlobalConSeccion.getKey(), creationType, "SentToVendorCenter", sendToVendorCenter);

					   description = atributoGlobal.has("Description") ? atributoGlobal.getString("Description") : null;
					   addJSONToRow(payloadRows, templateId, atributoGlobalConSeccion.getKey(), creationType, "AttributeHelpInformation", description);

					   maxLength = atributoGlobal.has("maxLength") ? atributoGlobal.getString("maxLength") : null;
					   addJSONToRow(payloadRows, templateId, atributoGlobalConSeccion.getKey(), creationType, "MaxLength", maxLength);

					   isEditable = isEditable == null ? "1" : isEditable;
					   addJSONToRow(payloadRows, templateId, atributoGlobalConSeccion.getKey(), creationType, "IsEditable", isEditable);

					   isMultiselect = atributoGlobal.has("Multivalued") ? atributoGlobal.getString("Multivalued") : null;
					   addJSONToRow(payloadRows, templateId, atributoGlobalConSeccion.getKey(), creationType, "IsMultiselect", isMultiselect);

					   min = atributoGlobal.has("min") ? atributoGlobal.getString("min") : null;
					   addJSONToRow(payloadRows, templateId, atributoGlobalConSeccion.getKey(), creationType, "Min", min);

					   max = atributoGlobal.has("max") ? atributoGlobal.getString("max") : null;
					   addJSONToRow(payloadRows, templateId, atributoGlobalConSeccion.getKey(), creationType, "Max", max);

					   dataType = atributoGlobal.has("baseType") ? atributoGlobal.getString("baseType") : null;
					   addJSONToRow(payloadRows, templateId, atributoGlobalConSeccion.getKey(), creationType, "DataType", dataType);

					   listOfValues = atributoGlobal.has("listOfValues") ? atributoGlobal.getString("listOfValues") : null;
					   addJSONToRow(payloadRows, templateId, atributoGlobalConSeccion.getKey(), creationType, "ListOfValues", listOfValues);

					   listOfValuesValidValues = null;
					   valueFilter = templateAttribute.has("valueFilter") ? templateAttribute.getJSONArray("valueFilter") : null;
					   if(valueFilter != null && valueFilter.length() > 0) {
						   if(valueFilter.getJSONObject(0).has("Value")) {
							   // Search for each value better do it another time...
							   /*
							   lookupMap = lookupMaps.get(listOfValues);
							   if(lookupMap == null) {
								   lookupMap = new java.util.TreeMap<>();
								   for(int k=0; k<valueFilter.length(); k++) {
									   try{
										   rawResponse = rc.getRequest("GET", "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/LookupValue/bySearch?lookup=" + java.net.URLEncoder.encode(listOfValues, "UTF-8") + "&query=" + java.net.URLEncoder.encode("LookupValueLang.Name(es) equals \"" + valueFilter.getJSONObject(k).getString("Value") + "\"", "UTF-8") + "&fields=" + java.net.URLEncoder.encode("LookupValue.Code", "UTF-8") + "", null);
										   response = new org.json.JSONObject(rawResponse);
										   if(response.has("rows") && response.getJSONArray("rows").length() > 0) {
											   lookupMap.put(valueFilter.getJSONObject(k).getString("Value"), response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0));
											   sb.append(k == 0 ? "" : ",").append(response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0));
										   }else {
											   bannedValues.add(valueFilter.getJSONObject(k).getString("Value"));
											   System.out.println("A value was not found within its lookup in P360: " + valueFilter.getJSONObject(k).getString("Value") + " lk: " + listOfValues);
										   }
									   }catch(Exception e) {
										   e.printStackTrace();
										   System.out.println(rawResponse);
									   }
								   }
								   if(!lookupMap.isEmpty())
									   lookupMaps.put(listOfValues, lookupMap);
							   }else {
								   for(int k=0; k<valueFilter.length(); k++) {
									   lookupCode = lookupMap.get(valueFilter.getJSONObject(k).getString("Value"));
									   if(lookupCode == null)
										   try{
											   rawResponse = rc.getRequest("GET", "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/LookupValue/bySearch?lookup=" + java.net.URLEncoder.encode(listOfValues, "UTF-8") + "&query=" + java.net.URLEncoder.encode("LookupValueLang.Name(es) equals \"" + valueFilter.getJSONObject(k).getString("Value") + "\"", "UTF-8") + "&fields=" + java.net.URLEncoder.encode("LookupValue.Code", "UTF-8") + "", null);
											   response = new org.json.JSONObject(rawResponse);
											   if(response.has("rows") && response.getJSONArray("rows").length() > 0) {
												   lookupMap.put(valueFilter.getJSONObject(k).getString("Value"), response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0));
												   sb.append(k == 0 ? "" : ",").append(response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0));
											   }else {
												   bannedValues.add(valueFilter.getJSONObject(k).getString("Value"));
												   System.out.println("A value was not found within its lookup in P360: " + valueFilter.getJSONObject(k).getString("Value") + " lk: " + listOfValues);
											   }
										   }catch(Exception e) {
											   e.printStackTrace();
											   System.out.println(rawResponse);
										   }
									   else {
										   sb.append(k == 0 ? "" : ",").append(lookupCode);
									   }
								   }
							   }
							   */
						   }else {
							   for(int k=0; k<valueFilter.length(); k++) {
								   sb.append(k == 0 ? "" : ",").append(valueFilter.getJSONObject(k).getString("ID"));
							   }
						   }
						   listOfValuesValidValues = sb.toString();
						   addJSONToRow(payloadRows, templateId, atributoGlobalConSeccion.getKey(), creationType, "ListOfValuesFilter", listOfValuesValidValues);
						   sb.setLength(0);
					   }else if(valueFilter == null) {
						   System.out.println("Como es esto posible (gral) " + atributoGlobalConSeccion + ", " + templateAttribute);
					   }
				   }
			   }
		   }
		   try {
			   rawResponse = rc.getRequest("POST", "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/StandardizationValue", new org.json.JSONObject()
					   .put("columns", new org.json.JSONArray()
						   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"))
						   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"))
						   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"))
						   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"))
						   .put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"))
					   )
					   .put("rows", payloadRows)
					   .toString());
			   System.out.println("Inserted into dictionary response: " + rawResponse);
		   }catch(Exception e) {
			   e.printStackTrace();
		   }
		   payloadRows = new org.json.JSONArray();
//		   System.out.println(soloPlantillas.getJSONObject(i));
//		   if(i == 10)
//			   break;
		   System.out.println((i+ 1) + "/" + (soloPlantillas.length()));
	   }
	   System.out.println("Missing characteristics: " + missingCharacteristics.size());
	   missingCharacteristics.forEach((k,v)->System.out.println(v));
	   System.exit(0);
	   /**
	    * Recupera los elementos existntes en la jerarquía de HPM.
	    *
	    ***************************************************************/
	   /*
	   java.util.Map<String, String> currentLevels = new java.util.TreeMap<>();
	   do {
		   url = "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/StructureGroup/byStructure?structure=PrimaryProductTaxonomy&fields=StructureGroup.Identifier,StructureGroup.Level,StructureGroupLang.Name(es)&pageSize=400&startIndex=" + currentIndex;
		   try{
			   rawResponse = rc.getRequest("GET", url, null);
			   response = new org.json.JSONObject(rawResponse);
			   rows = response.getJSONArray("rows");
			   for(int i=0; i<rows.length(); i++) {
				   if(Integer.parseInt(rows.getJSONObject(i).getJSONArray("values").getString(1)) > 0){
					   currentLevels.put(rows.getJSONObject(i).getJSONArray("values").getString(0), rows.getJSONObject(i).getJSONArray("values").getString(2));
				   }
				   currentIndex++;
			   }
			   totalSize = response.getInt("totalSize");
		   }catch(Exception e) {
			   e.printStackTrace();
			   System.out.println(rawResponse);
		   }
	   }while(currentIndex < totalSize);
	   System.out.println("Loaded " + currentLevels.size());
	   */
	   /*******
	    * Determina los elementos del documento XML que no están en la jerarquía de HPM.
	    *
	    ********************************************************************************/
	   /*
	   java.util.LinkedList<String> notFoundIDs = new java.util.LinkedList<>();
	   org.json.JSONArray notFoundElements = new org.json.JSONArray();
	   java.util.LinkedList<java.util.Map.Entry<String, org.json.JSONObject>> aux = new java.util.LinkedList<>(structureEntries.entrySet());
	   java.util.Collections.sort( aux, (o1, o2)-> o1.getKey().compareTo(o2.getKey()) );
	   aux.forEach(entry -> {if(!currentLevels.containsKey(entry.getKey()) && entry.getKey().startsWith("EU")) { notFoundElements.put(entry.getValue()); notFoundIDs.addLast(entry.getKey() + "_" + entry.getValue().getString("Name")); }});
	   notFoundIDs.forEach(System.out::println);
	   System.out.println(notFoundIDs.size() + " elements missing.");
	   rows = new org.json.JSONArray();
	   org.json.JSONObject notFoundElement = null;
	   org.json.JSONArray rowsForLookup = new org.json.JSONArray();

	   for(int i=0; i<notFoundElements.length(); i++) {
		   notFoundElement = notFoundElements.getJSONObject(i);
		   rows.put(new org.json.JSONObject()
				   .put("object", new org.json.JSONObject().put("id", "'" + notFoundElement.get("ID") + "'@'PrimaryProductTaxonomy'"))
				   .put("values", new org.json.JSONArray().put(notFoundElement.get("Name")).put(notFoundElement.get("Name")).put(notFoundElement.get("ParentID")))
			);
		   rowsForLookup.put(
				   new org.json.JSONObject()
				   .put("object", new org.json.JSONObject().put("id", "'" + notFoundElement.get("ID") + "'@'PPH_L4_Templates'"))
				   .put("values", new org.json.JSONArray().put(notFoundElement.get("Name")).put(notFoundElement.get("Name")))
				   );
	   }
	   */
	   /**
	    * Para la primera vez que se ejecuta, antes de escribir en HPM, guardar los json array con los elementos faltantes.
	    ********************************************************************************************************************/
	   /*
	   try( java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\NotFoundElementsForStructureGroup"))) ){ pw.println(rows); }catch(java.io.IOException e) { e.printStackTrace(); }
	   System.out.println("Wrote for StructureGroup (file stage)");
	   try( java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\NotFoundElementsForLookup"))) ){ pw.println(rowsForLookup); }catch(java.io.IOException e) { e.printStackTrace(); }
	   System.out.println("Wrote for Lookup (file stage)");
	   try( java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\NotFoundElementsArray"))) ){ pw.println(notFoundElements); }catch(java.io.IOException e) { e.printStackTrace(); }
	   System.out.println("Wrote for not found elements (stage file)");
	   */
	   /**
	    * Para veces posterioes, leer del archivo los json array con los elementos faltantes.
	    ********************************************************************************************************************/
	   /*
	   org.json.JSONArray notFoundElements = new org.json.JSONArray();
	   try( java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\NotFoundElementsArray"))) ){ notFoundElements = new org.json.JSONArray(br.readLine()); }catch(java.io.IOException | org.json.JSONException e) { e.printStackTrace(); }
	   rows = new org.json.JSONArray();
	   org.json.JSONObject notFoundElement = null;
	   org.json.JSONArray rowsForLookup = new org.json.JSONArray();

	   for(int i=0; i<notFoundElements.length(); i++) {
		   notFoundElement = notFoundElements.getJSONObject(i);
		   rows.put(new org.json.JSONObject()
				   .put("object", new org.json.JSONObject().put("id", "'" + notFoundElement.get("ID") + "'@'PrimaryProductTaxonomy'"))
				   .put("values", new org.json.JSONArray().put(notFoundElement.get("Name")).put(notFoundElement.get("Name")).put(notFoundElement.get("ParentID")))
				   );
	   }
	   */
	   /**
	    * Cargar los elementos faltantes en la jerarquía correspondiente y en la lookup.
	    *********************************************************************************/
	   /*
	   try{
		   rawResponse = rc.getRequest("POST", "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/StructureGroup", new org.json.JSONObject().put("columns",
				   new org.json.JSONArray()
				   	.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(en)")).put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"))).put("rows", rows).toString());
		   System.out.println("Response from inserting missing structure group elements: " + rawResponse);
//////////// Bloque de carga de la lookup.
//		   rawResponse = rc.getRequest("POST", "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/LookupValue", new org.json.JSONObject().put("columns",
//				   new org.json.JSONArray()
//				   	.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(en)"))).put("rows", rowsForLookup).toString());
//		   System.out.println("Response from inserting missing lookup PPH_L4_Template elements: " + rawResponse);
	   }catch(Exception e) {
		   e.printStackTrace();
	   }
	   */
//	   JSONArrayHandler handler = new JSONArrayHandler(rows, 200, this::enterRowsCharacteristicAttributeGroup);
//	   for(int i=0; i<attributeListNodes.getLength(); i++) {
//		   processAttributeListNode((Element)attributeListNodes.item(i), handler);
//	   }
//	   handler.processRows();
//	   try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\ejele.pa")))){
//		   characteristicsNotFound.forEach(pw::println);
//	   }catch(java.io.IOException e) {
//		   e.printStackTrace();
//	   }
//	   loadLookupMap();
//	   attributeGroupLookupInternalId = getLookupInternalId("AttributeGroup");
//	   JSONArrayHandler rowsHandler = new JSONArrayHandler(rows, 200, this::enterRows);
//	   for(int i=0; i<attributeGroupNodes.getLength(); i++) {
//		   processAttributeGroupNode((Element) attributeGroupNodes.item(i), null, rowsHandler);
//	   }
//	   if(rows.length() > 0) {
//		   enterRows(rows);
//	   }
	}

	private void addJSONToRow(org.json.JSONArray rows, String templateId, String characteristicName, String creationType, String propertyType, String propertyValue) throws org.json.JSONException {
		if(propertyValue != null && !"".equals(propertyValue)) {
			rows.put( makeJSONForRow(templateId, characteristicName, creationType, propertyType, propertyValue) );
		}
	}

	private org.json.JSONObject makeJSONForRow(String templateId, String name, String creationType, String propertyType, String propertyValue) throws org.json.JSONException {
		return new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + templateId + "_" + name + "_" + creationType + "_" + propertyType + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'") ).put("values", new org.json.JSONArray().put(new org.json.JSONObject().put("id", "'" + templateId + "'@'PPH_L4_Templates'").put("entityId", 7300)).put(name).put(creationType).put(propertyType).put(propertyValue));
	}

	private String translateBaseType(String baseType) {
		if("number".equals(baseType)) {
			return "Number";
		}else if("legacyisodatetime".equals(baseType)) {
			return "Timestamp";
		}else if("integer".equals(baseType)) {
			return "Integer";
		}else if("legacyisodate".equals(baseType)) {
			return "Date";
		}else if("isodate".equals(baseType)) {
			return "Date";
		}else {
			return "Text";
		}
	}

	private void collectAttributes(Node n, java.util.Map<String, org.json.JSONObject> attributes) {
		if(Node.ELEMENT_NODE == n.getNodeType() && "Attribute".equals(n.getNodeName())) {
			NodeList children = n.getChildNodes();
			Node cn = null;
			String id = ((Element)n).getAttribute("ID");
			String isMandatory = ((Element)n).getAttribute("Mandatory");
			String multivalued = ((Element)n).getAttribute("Multivalued");
			String baseType = "Text";
			String maxLength = "0";
			String min = null;
			String max = null;
			String listOfValues = null;
			String attributeName = null;
			String attributeDesc = null;
			NodeList metaDataChildNodes = null;
			Node mdcn = null;
			org.json.JSONObject metadataMap = new org.json.JSONObject();
			for(int i=0; i<children.getLength(); i++) {
				cn = children.item(i);
				if(cn.getNodeType() == Node.ELEMENT_NODE) {
					if("Name".equals(cn.getNodeName())) {
						attributeName = cn.getTextContent();
					}else if("MetaData".equals(cn.getNodeName())) {
						metaDataChildNodes = cn.getChildNodes();
						for(int j=0; j<metaDataChildNodes.getLength(); j++) {
							mdcn = metaDataChildNodes.item(j);
							if(mdcn.getNodeType() == Node.ELEMENT_NODE && "Value".equals(mdcn.getNodeName())) {
								metadataMap.put( ((Element)mdcn).getAttribute("AttributeID"), mdcn.getTextContent());
							}
						}
					}else if("ListOfValueLink".equals(cn.getNodeName())) {
						listOfValues = ((Element)cn).getAttribute("ListOfValueID");
					}else if("Validation".equals(cn.getNodeName())) {
						baseType = ((Element)cn).getAttribute("BaseType");
						maxLength = ((Element)cn).getAttribute("MaxLength");
						min = ((Element)cn).getAttribute("MinValue");
						max = ((Element)cn).getAttribute("MaxValue");
					}
				}
			}
			attributeDesc = metadataMap.has("AttributeHelpText") ? metadataMap.getString("AttributeHelpText") : "";
			attributes.put(id, new org.json.JSONObject()
					.put("ID", id)
					.put("Name", attributeName)
					.put("Mandatory", isMandatory)
					.put("Multivalued", multivalued)
					.put("Description", attributeDesc)
					.put("baseType", baseType)
					.put("maxLength", maxLength)
					.put("min", min)
					.put("max", max)
					.put("listOfValues", listOfValues)
					.put("metadata", metadataMap)
					);
		}
	}

	private java.util.LinkedList<String> characteristicsNotFound = new java.util.LinkedList<>();

	private void processAttributeGroupNode(Element n, String parentNodeID, JSONArrayHandler rowsHandler) {
		Node configNode = n.getElementsByTagName("Configuration").item(0);
		Node nameNode = n.getElementsByTagName("Name").item(0);
		String name = (nameNode == null ? "" : nameNode.getTextContent());
		String parentId = (parentNodeID == null ? "" : parentNodeID);
		rowsHandler.put( new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + n.getAttribute("ID") + "'@'AttributeGroup'")).put("values", new org.json.JSONArray().put(n.getAttribute("ID")).put(lookupMap.get(parentId)).put( attributeGroupLookupInternalId + "@21740" ) ) );
		NodeList ag = n.getElementsByTagName("AttributeGroup");
		for(int i=0; i<ag.getLength(); i++) {
			processAttributeGroupNode((Element) ag.item(i), n.getAttribute("ID"), rowsHandler);
		}
	}

	private void enterRowsCharacteristicAttributeGroup(org.json.JSONArray rows) {
		try {
			org.json.JSONObject request = new org.json.JSONObject().put("columns", new org.json.JSONArray()
					.put(new org.json.JSONObject().put("identifier", "CharacteristicAttributeGroup.Characteristic"))
					.put(new org.json.JSONObject().put("identifier", "CharacteristicAttributeGroup.AttributeGroup"))
					.put(new org.json.JSONObject().put("identifier", "CharacteristicAttributeGroup.Dictionary"))).put("rows", rows);
			org.json.JSONObject response = null;
			response = new org.json.JSONObject( rc.getRequest("POST", "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/CharacteristicAttributeGroup", request.toString()));
			if(response.getJSONObject("counters").getInt("errors") > 0) {
				org.json.JSONArray entries = response.getJSONArray("entries");
				int rowWithError = 0;
				String elese = null;
				for(int i=0; i<entries.length(); i++) {
					rowWithError = entries.getJSONObject(i).getInt("row");
					elese = rows.getJSONObject(rowWithError).getJSONArray("values").getJSONObject(0).getString("id");
					elese = elese.substring(1, elese.length() - 1);
					characteristicsNotFound.addLast(elese);
				}
				response.put("_RAW_REQUEST", request);
//				System.out.println("\t" + response);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void enterRows(org.json.JSONArray rows) {
		try {
			System.out.println( rc.getRequest("POST", "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/AttributeGroup", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "AttributeGroup.RefLookup")).put(new org.json.JSONObject().put("identifier", "AttributeGroup.Parent")).put(new org.json.JSONObject().put("identifier", "AttributeGroup.Dictionary"))).put("rows", rows).toString()) );
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	private Integer getLookupInternalId(String externalId) {
		try {
			String rawResponse = rc.getRequest("GET", "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/Lookup/bySearch?query=" + java.net.URLEncoder.encode("Lookup.Identifier equals " + externalId, "UTF-8"), null);
			org.json.JSONObject response = new org.json.JSONObject(rawResponse);
			return Integer.parseInt( response.getJSONArray("rows").getJSONObject(0).getJSONObject("object").getString("id") );
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void loadLookupMap() {
		String url = null;
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONObject row = null;
		int readRows = 0;
		int totalSize = 0;
		try {
			do {
				url = "https://webctep360dev.liverpool.com.mx/rest/V2.0/list/LookupValue/byLookup?lookup=AttributeGroup&fields=LookupValue.Code&startIndex=" + readRows;
				rawResponse = rc.getRequest("GET", url, null);
				response = new org.json.JSONObject(rawResponse);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					readRows++;
					row = rows.getJSONObject(i);
					lookupMap.put(row.getJSONArray("values").getString(0), Integer.parseInt(row.getJSONObject("object").getString("id").split("@")[0]));
				}
				totalSize = response.getInt("totalSize");
			}while(readRows < totalSize);
		}catch(Exception e) {
			System.out.println(rawResponse);
			e.printStackTrace();
		}
	}

	private void processProductNode(Element element, JSONArrayHandler primaryProductHierarchyRows, JSONArrayHandler attributeMetadataRows, String myParentID, org.json.JSONArray productHierarchyEntries) {

		String productHierarchyID = element.getAttribute("ID");
		String userTypeId = element.getAttribute("UserTypeID");
		userTypeId = userTypeId != null && userTypeId.startsWith("Level") ? userTypeId.replaceAll("Level", "") : userTypeId;
		String productHierarchyName = null;
		String isMandatory = null;

		NodeList nl = element.getChildNodes();
		Node n = null;

		NodeList products = null;
		Node product = null;

		Node attributeLinkNode = null;
		NodeList attributeLinkNodeChildren = null;
		Node attributeLinkNodeChild = null;

		NodeList attributeLinkMetaData = null;
		NodeList attributeLinkValueFilter = null;
		Node attributeLinkMetaDataNode = null;
		Node attributeLinkValueFilterNode = null;

		NodeList values = null;
		Node valuesValue = null;
		NodeList valuesFromMultiValue = null;
		Node valueFromMultiValue = null;

		java.util.Map<String, String> attributeValuesMap = new java.util.TreeMap<>();
		java.util.Map<String, org.json.JSONArray> attributeMultiValueList = new java.util.TreeMap<>();
		org.json.JSONArray multiValueList = null;

		java.util.Map<String, org.json.JSONArray> attributeLinkValueFilterMap = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Map<String, String>> attributeLinkMetaDataMap = new java.util.TreeMap<>();

		java.util.Map<String, String> metaDataMap = null;
		org.json.JSONArray valueFilterList = null;

		java.util.LinkedList<String> mandatoryAttributeLink = new java.util.LinkedList<>();
		java.util.LinkedList<String> simpleAttributes = new java.util.LinkedList<>();

		for(int i=0; i<nl.getLength(); i++) {
			n = nl.item(i);
			if(Node.ELEMENT_NODE == n.getNodeType()) {
				if("Product".equals(n.getNodeName())) {
					processProductNode((Element)n, primaryProductHierarchyRows, attributeMetadataRows, productHierarchyID, productHierarchyEntries);
				}else if("Name".equals(n.getNodeName())) {
					productHierarchyName = n.getTextContent();
				}else if("Values".equals(n.getNodeName())) {
					values = n.getChildNodes();
					for(int j=0; j<values.getLength(); j++) {
						valuesValue = values.item(j);
						if(valuesValue.getNodeType() == Node.ELEMENT_NODE) {
							if("Value".equals(valuesValue.getNodeName())) {
								if(((Element)valuesValue).hasAttribute("AttributeID")) {
									attributeValuesMap.put(((Element)valuesValue).getAttribute("AttributeID"), valuesValue.getTextContent());
								}
							}else if("MultiValue".equals(valuesValue.getNodeName())) {
								if(((Element)valuesValue).hasAttribute("AttributeID")) {
									valuesFromMultiValue = valuesValue.getChildNodes();
									multiValueList = new org.json.JSONArray();
									for(int k = 0; k<valuesFromMultiValue.getLength(); k++) {
										valueFromMultiValue = valuesFromMultiValue.item(k);
										if(Node.ELEMENT_NODE == valueFromMultiValue.getNodeType()) {
											if("Value".equals(valueFromMultiValue.getNodeName())) {
												 multiValueList.put( valueFromMultiValue.getTextContent());
											}
										}
									}
									attributeMultiValueList.put( ((Element)valuesValue).getAttribute("AttributeID"), multiValueList);
								}
							}
						}
					}
				} else  if("AttributeLink".equals(n.getNodeName())) {
					attributeLinkNode = n;
					attributeLinkNodeChildren = attributeLinkNode.getChildNodes();
					if(((Element)n).hasAttribute("Mandatory")) {
						isMandatory = ((Element)n).getAttribute("Mandatory");
						if("true".equals(isMandatory)) {
							mandatoryAttributeLink.addLast(((Element)n).getAttribute("AttributeID"));
						}
					}
					if(attributeLinkNodeChildren != null) {
						if(attributeLinkNodeChildren.getLength() == 0) {
							simpleAttributes.addLast(((Element)n).getAttribute("AttributeID"));
						} else {
							for(int j=0; j<attributeLinkNodeChildren.getLength(); j++) {
								attributeLinkNodeChild = attributeLinkNodeChildren.item(j);
								if(Node.ELEMENT_NODE == attributeLinkNodeChild.getNodeType() && ((Element) attributeLinkNode).hasAttribute("AttributeID")) {
									if("MetaData".equals(attributeLinkNodeChild.getNodeName())) {
										attributeLinkMetaData = attributeLinkNodeChild.getChildNodes();
										metaDataMap = new java.util.TreeMap<>();
										for(int k=0; k < attributeLinkMetaData.getLength(); k++) {
											attributeLinkMetaDataNode = attributeLinkMetaData.item(k);
											if(Node.ELEMENT_NODE == attributeLinkMetaDataNode.getNodeType() && ((Element)attributeLinkMetaDataNode).hasAttribute("AttributeID")) {
												if(attributeLinkMetaDataNode.getNodeType() == Node.ELEMENT_NODE) {
													if("Value".equals(attributeLinkMetaDataNode.getNodeName())) {
														if(((Element)attributeLinkMetaDataNode).hasAttribute("AttributeID") && ((Element)attributeLinkMetaDataNode).hasAttribute("ID")) {
															metaDataMap.put(((Element)attributeLinkMetaDataNode).getAttribute("AttributeID"), ((Element)attributeLinkMetaDataNode).getAttribute("ID"));
														}
													}
												}
											}
										}
										attributeLinkMetaDataMap.put(((Element)attributeLinkNode).getAttribute("AttributeID"), metaDataMap);
									}else if("ValueFilter".equals(attributeLinkNodeChild.getNodeName())) {
										attributeLinkValueFilter = attributeLinkNodeChild.getChildNodes();
										valueFilterList = new org.json.JSONArray();
										for(int k=0; k<attributeLinkValueFilter.getLength(); k++) {
											attributeLinkValueFilterNode = attributeLinkValueFilter.item(k);
											if(attributeLinkValueFilterNode.getNodeType() == Node.ELEMENT_NODE) {
												if("Value".equals(attributeLinkValueFilterNode.getNodeName())) {
													if( ((Element)attributeLinkValueFilterNode).hasAttribute("ID") ) {
														valueFilterList.put(new org.json.JSONObject().put("ID", ((Element)attributeLinkValueFilterNode).getAttribute("ID") ) );
													}else {
														valueFilterList.put(new org.json.JSONObject().put("Value", attributeLinkValueFilterNode.getTextContent() ) );
													}
												}
											}
										}
										attributeLinkValueFilterMap.put(((Element)attributeLinkNode).getAttribute("AttributeID"), valueFilterList);
									}
								}
							}
						}
					}
				}
			}
		}
		org.json.JSONObject attributes = new org.json.JSONObject();
		java.util.Set<String> mutual = new java.util.TreeSet<>();
		java.util.Set<String> onlyValues = new java.util.TreeSet<>();
		attributeLinkValueFilterMap.forEach((k,v)-> {
			java.util.Map<String, String> mdMap = attributeLinkMetaDataMap.remove(k);
			org.json.JSONObject ent = new org.json.JSONObject();
			if(mdMap != null) {
				mdMap.forEach((k1,v1)->ent.put(k1, v1));
				mutual.add(k);
			}else {
				onlyValues.add(k);
			}
			attributes.put(k, new org.json.JSONObject().put("metaData", ent).put("valueFilter", v));
		} );
		attributeLinkMetaDataMap.forEach((k,v)->{
			org.json.JSONObject ent = new org.json.JSONObject();
			v.forEach((k1,v1)->ent.put(k1, v1));
			attributes.put(k, new org.json.JSONObject().put("metaData", ent).put("valueFilter", new org.json.JSONArray()));
		});
		simpleAttributes.forEach(sa->{ if(!attributes.has(sa)) {
			attributes.put(sa, new org.json.JSONObject().put("metaData", new org.json.JSONObject()).put("valueFilter", new org.json.JSONArray()));
		} });
		mandatoryAttributeLink.forEach(ma->{ org.json.JSONObject jo = attributes.has(ma) ? attributes.getJSONObject(ma) : null; if(ma != null) {
			attributes.put(ma, new org.json.JSONObject().put("metaData", new org.json.JSONObject()).put("valueFilter", new org.json.JSONArray()).put("Mandatory", true));
		} else {
			jo.put("Mandatory", true);
		} });
		if(attributes.length() > 0) {
			for(String nm : org.json.JSONObject.getNames(attributes)) {
				if(!attributes.getJSONObject(nm).has("Mandatory")) {
					attributes.getJSONObject(nm).put("Mandatory", false);
				}
			}
		}
		org.json.JSONObject valuesArray = new org.json.JSONObject();
		attributeValuesMap.forEach((k,v)->valuesArray.put( k, v ));
		attributeMultiValueList.forEach((k,v)->valuesArray.put(k, v));
		org.json.JSONObject entry = new org.json.JSONObject()
				.put("ParentID", myParentID)
				.put("ID", productHierarchyID)
				.put("Level", userTypeId)
				.put("Name", productHierarchyName)
				.put("Values", valuesArray)
				.put("Attributes", attributes)
				;
		productHierarchyEntries.put( entry );
		if(attributes.length() > 0 && productHierarchyID.equals("ProductsSuppliersPortal") /* && "EU2-1396523".equals(productHierarchyID) */) {
			System.out.println(entry);
//		}else {
//			System.out.println(entry);
		}
	}

	private void processAttributeListNode(Element n, JSONArrayHandler rowsHandler) {
		boolean printit = false;
		n.hasAttribute("ID");
		String attributeId = n.getAttribute("ID");
		String externallyMaintained = n.getAttribute("ExternallyMaintained");
		String mandatory = n.getAttribute("Mandatory");
		String multiValued = n.getAttribute("MultiValued");
		String productMode = n.getAttribute("ProductMode");
		Node name = n.getElementsByTagName("Name").item(0);
		Node validation = n.getElementsByTagName("Validation").item(0);
		NodeList groupLink = n.getChildNodes();
		Node lovLink = n.getElementsByTagName("ListOfValueLink").item(0);
		Node node = null;
		Element validationElement = validation == null ? null : (Element) validation;
		Node unitLink = null;
		String unitId = null;
		org.json.JSONArray groupLinks = new org.json.JSONArray();
		if(groupLink != null) {
			for(int i=0; i<groupLink.getLength(); i++) {
				node = groupLink.item(i);
				if(Node.ELEMENT_NODE == node.getNodeType()) {
					if("AttributeGroupLink".equals(node.getNodeName())) {
						groupLinks.put(((Element)node).getAttribute("AttributeGroupID"));
					}
				}
			}
		}
		if(validationElement != null) {
			unitLink = validationElement.getElementsByTagName("UnitLink").item(0);
			if(unitLink != null) {
				unitId = ((Element)unitLink).getAttribute("UnitID");
			}
		}
		Node valueFilter = n.getElementsByTagName("ValueFilter").item(0);
		org.json.JSONArray valueIdsFromValueFilter = new org.json.JSONArray();
		if(valueFilter != null) {
			NodeList valuesFromValueFilter = valueFilter.getChildNodes();
			for(int i=0; i<valuesFromValueFilter.getLength(); i++) {
				node = valuesFromValueFilter.item(i);
				if(node.getNodeType() == Node.ELEMENT_NODE) {
					if("Value".equals(node.getNodeName())) {
						valueIdsFromValueFilter.put(((Element)valuesFromValueFilter.item(i)).getAttribute("ID"));
					}
				}
			}
		}
		Element metadata = (Element)n.getElementsByTagName("MetaData").item(0);
		org.json.JSONObject metadataValues = new org.json.JSONObject();
		org.json.JSONObject metadataMultiValue = new org.json.JSONObject();
		org.json.JSONArray metadatos = new org.json.JSONArray();
		String multiValueAttribute = null;
		Element ie = null;
		if(metadata != null) {
			Element metadataMultiValueElement = (Element) metadata.getElementsByTagName("MultiValue").item(0);
			if(metadataMultiValueElement != null) {
				multiValueAttribute = metadataMultiValueElement.getAttribute("AttributeID");
				NodeList metadataMultiValueNodes = metadataMultiValueElement.getElementsByTagName("Value");
				for(int i=0; i< metadataMultiValueNodes.getLength(); i++) {
					ie = (Element) metadataMultiValueNodes.item(i);
					metadatos.put(new org.json.JSONObject().put(ie.getAttribute("ID"), ie.getTextContent()));
				}
				metadataMultiValue.put("AttributeID", multiValueAttribute);
				metadataMultiValue.put("values", metadatos);
			}
			NodeList metadataValueElement = metadata.getChildNodes();
			if(metadataValueElement != null) {
				for(int i=0; i<metadataValueElement.getLength(); i++) {
					node = metadataValueElement.item(i);
					if(node.getNodeType() == Node.ELEMENT_NODE) {
						if("Value".equals(node.getNodeName())) {
							metadataValues.put(
							((Element)metadataValueElement.item(i)).getAttribute("AttributeID"),
							metadataValueElement.item(i).getTextContent());
							if("".equals(((Element)metadataValueElement.item(i)).getAttribute("AttributeID"))){
								printit = true;
							}
						}
					}
				}
			}
		}
		org.json.JSONArray columns = new org.json.JSONArray();
		columns
			.put(attributeId)
			.put(name != null ? name.getTextContent() : "")
			.put(externallyMaintained)
			.put(mandatory)
			.put(multiValued)
			.put(productMode)
			.put(groupLinks)
			.put(lovLink != null ? ((Element)lovLink).getAttribute("AttributeID") : "")
			.put(validationElement != null ? validationElement.getAttribute("BaseType") : "")
			.put(validationElement != null ? validationElement.getAttribute("MaxLength") : "")
			.put(validationElement != null ? validationElement.getAttribute("MinValue") : "")
			.put(validationElement != null ? validationElement.getAttribute("MaxAttribute") : "")
			.put(validationElement != null ? validationElement.getAttribute("InputMask") : "")
			.put(unitId == null ? "" : unitId)
			.put(valueIdsFromValueFilter)
			.put(metadataValues)
			.put(metadataMultiValue)
			;
		if(attributeId.equals("TamanoDireccion6Att")) {
			System.out.println(columns);
		}
		if(groupLinks.length() > 0)
		 {
			rowsHandler.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + attributeId + "'@'CharacteristicAttributeGroup'")).put("values", new org.json.JSONArray().put(new org.json.JSONObject().put("id", "'" + attributeId + "'")).put(groupLinks).put(new org.json.JSONObject().put("id", "34001@21005"))));
//		if(metadataValues.length() > 0)
//		if(printit)
//		if(groupLinks.length() > 0)
//			System.out.println(columns);
//		NodeList ag = n.getElementsByTagName("Attribute");
//		for(int i=0; i<ag.getLength(); i++) {
//			processAttributeListNode((Element) ag.item(i), n.getAttribute("ID"));
//		}
		}
	}

	private void processHierarchyNode(Element n, String parentNodeID) {
		Node configNode = n.getElementsByTagName("").item(0);
		Node nameNode = n.getElementsByTagName("Name").item(0);
		NodeList ag = n.getElementsByTagName("AttributeGroup");
		for(int i=0; i<ag.getLength(); i++) {
			processHierarchyNode((Element) ag.item(i), n.getAttribute("ID"));
		}
	}


	private class JSONArrayHandler{

		private org.json.JSONArray rows;
		private int batchSize = 10;

		private JSONArrayHandlerAction handler;

		public JSONArrayHandler(org.json.JSONArray rows, JSONArrayHandlerAction handler) {
			this.rows = rows;
			this.handler = handler;
		}

		public JSONArrayHandler(org.json.JSONArray rows, int batchSize, JSONArrayHandlerAction handler) {
			this.rows = rows;
			this.batchSize = batchSize;
			this.handler = handler;
		}

		public org.json.JSONArray getRows(){
			return rows;
		}

		public void put(org.json.JSONObject row) {
			this.rows.put(row);
			if(this.rows.length() == this.batchSize) {
				handler.processRows(rows);
				this.rows = new org.json.JSONArray();
			}
		}

		public void processRows() {
			this.handler.processRows(rows);
		}
	}

	private interface JSONArrayHandlerAction{
		public void processRows(org.json.JSONArray rows);
	}

	public static void main(String[] args) {
		FlatenPPHFromStepXML a = new FlatenPPHFromStepXML();
		try {
			a.processFile();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}

	private static final String atributosCalculadosECC = "Parent\r\n"
			+ "Name\r\n"
			+ "Direction\r\n"
			+ "Section\r\n"
			+ "ItemGroup\r\n"
			+ "ProductTypeSAP\r\n"
			+ "SkuType (SAP ECC)\r\n"
			+ "SAPObjectType\r\n"
			+ "BrandName (SAP ECC)\r\n"
			+ "WHERL\r\n"
			+ "TImportacion\r\n"
			+ "SupplierPartNumber\r\n"
			+ "Negocio \r\n"
			+ "AnoEstacion (SAP ECC)\r\n"
			+ "Temporada (SAP ECC)\r\n"
			+ "Evento (SAP ECC)\r\n"
			+ "\r\n"
			+ "Coleccion\r\n"
			+ "LicenseDescription (SAP ECC)\r\n"
			+ "\r\n"
			+ "GradoDemoda\r\n"
			+ "MainBarCode\r\n"
			+ "TypeMainBarCode (SAP ECC)\r\n"
			+ "SupplierID\r\n"
			+ "SKU\r\n"
			+ "ZNUMV\r\n"
			+ "BaseUnitOfMeasure\r\n"
			+ "MAPEO EN EL EXPORT HACIA SAP\r\n"
			+ "Status\r\n"
			+ "TipoDeEtiqueta\r\n"
			+ "HNDLCODE\r\n"
			+ "WHSTC\r\n"
			+ "MVGR5\r\n"
			+ "Armado\r\n"
			+ "SAP_BEHVO\r\n"
			+ "ProductHeight\r\n"
			+ "ProductWidth\r\n"
			+ "ProductDepth\r\n"
			+ "ProductWeight\r\n"
			+ "VOLUMAtt\r\n"
			+ "PesoBruto\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "ZHOECJ\r\n"
			+ "ZBRECJ\r\n"
			+ "ZLAECJ\r\n"
			+ "ZVOLCJ\r\n"
			+ "ZBRGCJ\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "ZNTGCJ\r\n"
			+ "ZHOEPQ\r\n"
			+ "ZBREPQ\r\n"
			+ "ZLAEPQ\r\n"
			+ "ZVOLPQ\r\n"
			+ "ZBRGPQ\r\n"
			+ "ZNTGPQ\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "FotoTomadaLiverpool\r\n"
			+ "\r\n"
			+ "MesdeEntregadeMercancIa\r\n"
			+ "PerfilDeRedondeo\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "CostobrutoSinIVA\r\n"
			+ "Descuento1\r\n"
			+ "Descuento2\r\n"
			+ "CostoNetoSinIVA\r\n"
			+ "IndicadordeImpuesto\r\n"
			+ "IEPS\r\n"
			+ "ImpuestoALaVenta\r\n"
			+ "PrecioSugeridocIVA\r\n"
			+ "FechaInicioVigenciaPrecioVenta\r\n"
			+ "\r\n"
			+ "CostoEnMonedaExtranjera\r\n"
			+ "FechaInicioVigenciaCostoNeto";

	private static final String atributosCalculadosMKP = "ID\r\n"
			+ "Parent\r\n"
			+ "Name\r\n"
			+ "Direction\r\n"
			+ "Section\r\n"
			+ "ItemGroup\r\n"
			+ "ProductTypeSAP\r\n"
			+ "SkuType (SAP ECC)\r\n"
			+ "SAPObjectType\r\n"
			+ "BrandName (SAP ECC)\r\n"
			+ "WHERL\r\n"
			+ "TImportacion\r\n"
			+ "SupplierPartNumber\r\n"
			+ "Negocio \r\n"
			+ "AnoEstacion (SAP ECC)\r\n"
			+ "Temporada (SAP ECC)\r\n"
			+ "Evento (SAP ECC)\r\n"
			+ "\r\n"
			+ "Coleccion\r\n"
			+ "LicenseDescription (SAP ECC)\r\n"
			+ "\r\n"
			+ "GradoDemoda\r\n"
			+ "MainBarCode\r\n"
			+ "TypeMainBarCode (SAP ECC)\r\n"
			+ "SupplierID\r\n"
			+ "SKU\r\n"
			+ "\r\n"
			+ "BaseUnitOfMeasure\r\n"
			+ "MAPEO EN EL EXPORT HACIA SAP\r\n"
			+ "Status\r\n"
			+ "\r\n"
			+ "HNDLCODE\r\n"
			+ "WHSTC\r\n"
			+ "MVGR5\r\n"
			+ "Armado\r\n"
			+ "SAP_BEHVO\r\n"
			+ "ProductHeight\r\n"
			+ "ProductWidth\r\n"
			+ "ProductDepth\r\n"
			+ "ProductWeight\r\n"
			+ "VOLUMAtt\r\n"
			+ "PesoBruto\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "ZHOECJ\r\n"
			+ "ZBRECJ\r\n"
			+ "ZLAECJ\r\n"
			+ "ZVOLCJ\r\n"
			+ "ZBRGCJ\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "ZNTGCJ\r\n"
			+ "ZHOEPQ\r\n"
			+ "ZBREPQ\r\n"
			+ "ZLAEPQ\r\n"
			+ "ZVOLPQ\r\n"
			+ "ZBRGPQ\r\n"
			+ "ZNTGPQ\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "CostobrutoSinIVA\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "IndicadordeImpuesto\r\n"
			+ "IEPS\r\n"
			+ "ImpuestoALaVenta\r\n"
			+ "PrecioSugeridocIVA\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "CostoEnMonedaExtranjera\r\n"
			+ "FechaInicioVigenciaCostoNeto";

	private static final String atributosCalculadosSBB = "ID\r\n"
			+ "Parent\r\n"
			+ "Name\r\n"
			+ "Direction\r\n"
			+ "Section\r\n"
			+ "ItemGroupS4H\r\n"
			+ "SB_0002 (S4H >Suburbia)\r\n"
			+ "MTART_S4H (S4H)\r\n"
			+ "SAPObjectType\r\n"
			+ "\"BRAND_ID_S4H (S4H)\r\n"
			+ "\"\r\n"
			+ "WHERL\r\n"
			+ "TImportacion\r\n"
			+ "SupplierPartNumber\r\n"
			+ "EXTWG_S4H (S4H >> Suburbia)\r\n"
			+ "FSH_SEASON_YEAR (S4H)\r\n"
			+ "FSH_SEASON (S4H )\r\n"
			+ "LABOR_S4H (S4H)\r\n"
			+ "FSH_THEME (S4H)\r\n"
			+ "FSH_COLLECTION\r\n"
			+ "ZZLIC_S4H (S4H)\r\n"
			+ "BWVOR\r\n"
			+ "\r\n"
			+ "MainBarCodeS4H\r\n"
			+ "NUMTP_S4H (S4H)\r\n"
			+ "SupplierID\r\n"
			+ "SKU\r\n"
			+ "\r\n"
			+ "BaseUnitOfMeasure\r\n"
			+ "\r\n"
			+ "Status\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "SAP_BEHVO\r\n"
			+ "ProductHeight\r\n"
			+ "ProductWidth\r\n"
			+ "ProductDepth\r\n"
			+ "ProductWeight\r\n"
			+ "VOLUMAtt\r\n"
			+ "PesoBruto\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "ZHOECJ\r\n"
			+ "ZBRECJ\r\n"
			+ "ZLAECJ\r\n"
			+ "ZVOLCJ\r\n"
			+ "ZBRGCJ\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "\r\n"
			+ "FotoTomadaLiverpool\r\n"
			+ "\r\n"
			+ "\"MesdeEntregadeMercancIa\r\n"
			+ "\"\r\n"
			+ "PerfilDeRedondeo\r\n"
			+ "PLGTP\r\n"
			+ "BWSCL\r\n"
			+ "CostobrutoSinIVA\r\n"
			+ "Descuento1\r\n"
			+ "Descuento2\r\n"
			+ "CostoNetoSinIVA\r\n"
			+ "TAXM3_S4H\r\n"
			+ "TAXKM2_S4H\r\n"
			+ "TAXM1_S4H\r\n"
			+ "PrecioSugeridocIVA\r\n"
			+ "\r\n"
			+ "TextoAdicional";

}
