package mx.com.liverpool.p360.services.core.temp.dataloader;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class TraeteLosProductosDeLaMuestraDelEstet {

	private static final RESTWorkshop rw = new RESTWorkshop();
	private static final XMLMisc xmm = rw.getXmm();
	private static final java.util.ArrayList<String> nonExisting = new java.util.ArrayList<>();
	private static final java.util.Map<String, java.util.ArrayList<String>> entitiesMap = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> characteristicDataTypes = new java.util.TreeMap<>();
	
	public static void main(String[] args) {
		
		try {
			readProducts();
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
		}
		
	}
	
	private static void readProducts() throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse("C:\\opt\\LVP\\desorden\\SKUsExports2\\uncompressed\\711813124-711820371.xml");
		doc.getDocumentElement().normalize();
		java.util.Map<String, java.util.LinkedList<Node>> childElementsMap = null;
		java.util.LinkedList<Node> productNodes = xmm.listImmediateChildElements( xmm.listImmediateChildElements(doc.getDocumentElement()).get("Products").getFirst()).get("Product");
		java.util.LinkedList<Node> classificationNodes = null;
		java.util.LinkedList<Node> valueNodes = null;
		Element e = null;
		org.json.JSONArray characteristicRecords = new org.json.JSONArray();
		org.json.JSONArray characteristicRecordsForVariant = new org.json.JSONArray();
		org.json.JSONArray structureGroupMapArray = null;
		org.json.JSONArray higherLevelProduct = null;
		org.json.JSONObject requestBody = null;
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeLabels", "true");
		java.util.Map<String, String> unidades = new java.util.TreeMap<>();
		String elide = null;
		for(Node productNode : productNodes) {
			e = (Element) productNode;
			childElementsMap = xmm.listImmediateChildElements(productNode);
			classificationNodes = childElementsMap.get("ClassificationReference");
			valueNodes = xmm.listImmediateChildElements( childElementsMap.get("Values").getFirst() ).get("Value");
			structureGroupMapArray = collectStructureGroupClassifications(classificationNodes, characteristicRecords);
			structureGroupMapArray.put(new org.json.JSONObject().put( "_qualification", new org.json.JSONObject().put("structureGroup", new org.json.JSONObject().put("_externalId", "'" + e.getAttribute("ParentID") + "'@'PrimaryProductTaxonomy'"))));
			elide = "TURBO" + e.getAttribute("ID");
			higherLevelProduct = new org.json.JSONArray();
			System.out.println("Values to iterate: " + valueNodes.size());
			if("SalesItem".equals(e.getAttribute("UserTypeID"))) {
				addReferencedProduct(elide, higherLevelProduct);
				collectCharacteristicValuesForIndividual(valueNodes, characteristicRecords, characteristicRecordsForVariant, unidades);
				addUnidadesDeMedida(unidades, characteristicRecords);
				requestBody = new org.json.JSONObject();
				requestBody.put("_characteristicRecords", characteristicRecords);
				requestBody.put("structureGroupMap", structureGroupMapArray);
				System.out.println(requestBody);
				response = rw.makeRequest("PUT", "/object/Product2G/'" + elide + "'@1", qp, requestBody.toString());
				if(response.getJSONObject("_protocol").getInt("errorCounter") > 0) {
					System.out.println("There were errors: " + response);
					System.exit(0);
				}
				System.out.println("Generic: " + (response == null ? "ERR: " + rw.getRawResponse() : response));
				requestBody = new org.json.JSONObject();
				requestBody.put("_characteristicRecords", characteristicRecordsForVariant);
				requestBody.put("higherLevelProduct", higherLevelProduct);
				System.out.println("Variant Part: " + requestBody);
				response = rw.makeRequest("POST", "/object/Article", qp, requestBody.toString());
				if(response.getJSONObject("_protocol").getInt("errorCounter") > 0) {
					System.out.println("There were errors: " + response);
					System.exit(0);
				}
				System.out.println("Variant: " + (response == null ? "ERR: " + rw.getRawResponse() : response));
			}else {
				collectCharacteristicValues(valueNodes, characteristicRecords, "Product2G");
			}
		}
		System.out.println("Características que no tengo:");
		nonExisting.forEach(System.out::println);
	}
	
	private static void addReferencedProduct(String productId, org.json.JSONArray higherLevelProduct) {
		higherLevelProduct.put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("referencedIdentifier", productId)));
	}
	
	private static void addUnidadesDeMedida(java.util.Map<String, String> unidades, org.json.JSONArray characteristicRecords) {
		java.util.Map<String, String> unidadesPeso = new java.util.TreeMap<>();
		java.util.Map<String, String> unidadesLongitud = new java.util.TreeMap<>();
		java.util.Map<String, String> unidadesVolumen = new java.util.TreeMap<>();
		unidadesPeso.put("unece.unit.KGM", "KG");
		unidadesLongitud.put("unece.unit.CMT", "CM");
		unidadesVolumen.put("unece.unit.CMQ", "CM3");
		String unidadDeMedidaLongitud = null;
		String unidadDeMedidaVolumen = null;
		String unidadDeMedidaPeso = null;
		String[] atributosLongitud = new String[] { "ProductWidth", "ProductDepth", "ProductHeight", "ZBRECJ", "ZLAECJ", "ZHOECJ", "ZHOEPQ", "ZBREPQ", "ZLAEPQ" };
		String[] atributosVolumen = new String[] { "VOLUMAtt", "ZVOLCJ", "ZVOLPQ" };
		String[] atributosPeso = new String[] { "PesoBruto", "ProductWeight", "ZBRGCJ", "ZNTGCJ", "ZBRGPQ", "ZNTGPQ" };
		String unidadId = null;
		for(String a : atributosLongitud) {
			unidadId = unidades.get(a);
			if(unidadId != null) {
				unidadDeMedidaLongitud = unidadesLongitud.get(unidadId);
				break;
			}
		}
		for(String a : atributosVolumen) {
			unidadId = unidades.get(a);
			if(unidadId != null) {
				unidadDeMedidaVolumen = unidadesVolumen.get( unidadId );
				break;
			}
		}
		for(String a : atributosPeso) {
			unidadId = unidades.get(a);
			if(unidadId != null) {
				unidadDeMedidaPeso = unidadesPeso.get( unidadId );
				break;
			}
		}
		if(unidadDeMedidaLongitud == null) {
			System.out.println("No se obtuvo una unidad de medida de longitud");
		} else {
			characteristicRecords.put( createCharacteristicValueObject("UnidadDeMedidaLongitud", new org.json.JSONObject().put("_code", unidadDeMedidaLongitud) ) );
		}
		if(unidadDeMedidaPeso == null) {
			System.out.println("No se obtuvo unidad de medida de peso");
		}else {
			characteristicRecords.put( createCharacteristicValueObject("UnidadDeMedidaPeso", new org.json.JSONObject().put("_code", unidadDeMedidaPeso) ) );
		}
		if(unidadDeMedidaVolumen == null) {
			System.out.println("No se obtuvo unidad de medida de volumen");
		}else {
			characteristicRecords.put( createCharacteristicValueObject("UnidadDeMedidaVolumen", new org.json.JSONObject().put("_code", unidadDeMedidaVolumen) ) );
		}
		
	}
	
	private static void collectCharacteristicValuesForIndividual(java.util.LinkedList<Node> valueNodes, org.json.JSONArray characteristicRecords, org.json.JSONArray characteristicRecordsForVariant, java.util.Map<String, String> unidades){
		String attributeId = null;
		String valueId = null;
		String value = null;
		Object data = null;
		Element e = null;
		boolean isLookup = false;
		java.util.ArrayList<String> entities = null;
		int cnt = 0;
		int negocioTimes = 0;
		for(Node valueNode : valueNodes) {
			e = (Element) valueNode;
			attributeId = e.getAttribute("AttributeID");
			if(!nonExisting.contains(attributeId) && !checkCharacteristicExistance(attributeId)) {
				nonExisting.add(attributeId);
			}else {
				if(e.hasAttribute("UnitID")) {
					unidades.put(attributeId, e.getAttribute("UnitID"));
				}
				valueId = e.hasAttribute("ID") ? e.getAttribute("ID") : "TamanoUnico".equals(attributeId) ? e.getTextContent() : null;
				value = e.getTextContent();
				if("EXTWG_S4H".equals(attributeId) || "Negocio".equals(attributeId)) {
					negocioTimes++;
					if(negocioTimes > 1) {
						System.out.println("\n\t" + "!!!!");
					}
					capturaNegocio(value, attributeId, characteristicRecords);
				}
				isLookup = checkCharacteristicLookupValue(valueId, value, attributeId);
				entities = entitiesMap.get(attributeId);
				if(entities == null) {
					entities = collectEntities(attributeId);
				}
				if(entities != null) {
					if(!isLookup || (isLookup && valueId != null && !"".equals(valueId))) {
						data = isLookup ? new org.json.JSONObject()
								.put(valueId != null && !"".equals(valueId) ? "_code" : "_label", valueId != null && !"".equals(valueId) ? valueId : formatPlainValue( attributeId, value )) : formatPlainValue(attributeId, value );
						if(data != null) {
							if(entities.contains("Article")) {
								characteristicRecordsForVariant.put( createCharacteristicValueObject(attributeId, data ) );
							}
							if(entities.contains("Product2G")) {
								characteristicRecords.put( createCharacteristicValueObject(attributeId, data ) );
							}
						}
					}
				}
			}
			cnt++;
			System.out.println("\t" + (cnt) + "/" + valueNodes.size());
		}
	}
	
	private static void capturaNegocio(String valorNegocio, String etiqueta, org.json.JSONArray characteristicRecords) {
		if("EXTWG_S4H".equals(etiqueta)) {
			characteristicRecords.put( createCharacteristicValueObject("Business", new org.json.JSONObject().put("_code", "SBB" )) );
		}else {
			if("MARKETPLACE".equals(valorNegocio)) {
				characteristicRecords.put( createCharacteristicValueObject("Business", new org.json.JSONObject().put("_code", "MKP" )) );
			}else {
				characteristicRecords.put( createCharacteristicValueObject("Business", new org.json.JSONObject().put("_code", "LVP" )) );
			}
		}
	}
	
	private static Object formatPlainValue(String characteristic, String value) {
		String dataType = characteristicDataTypes.get(characteristic);
		if("DATETIME".equals(dataType)) {
			try {
				return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format( new java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy").parse(value) );
			}catch(java.text.ParseException e) {
				try {
					return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format( new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(value) );
				}catch(java.text.ParseException ex) {
					
				}
			}
		} else if("DECIMAL".equals(dataType)) {
			try{
				return new java.math.BigDecimal(value);
			}catch(NumberFormatException e) {
				System.out.println("Problem parsing a decimal data, the data: " + value);
				return null;
			}
		} else if("INTEGER".equals(dataType)) {
			try{
				return new java.math.BigDecimal(value).intValue();
			}catch(NumberFormatException e) {
				System.out.println("Problem interpreting a number data, the data: " +  value);
				return null;
			}
		}
		return value;
	}
	
	private static void collectCharacteristicValues(java.util.LinkedList<Node> valueNodes, org.json.JSONArray characteristicRecords, String entity){
		String attributeId = null;
		String valueId = null;
		String value = null;
		Element e = null;
		java.util.ArrayList<String> entities = null;
		boolean isLookup = false;
		for(Node valueNode : valueNodes) {
			e = (Element) valueNode;
			attributeId = e.getAttribute("AttributeID");
			if(!nonExisting.contains(attributeId) && !checkCharacteristicExistance(attributeId)) {
				nonExisting.add(attributeId);
			}else {
				valueId = e.hasAttribute("ID") ? e.getAttribute("ID") : "TamanoUnico".equals(attributeId) ? e.getTextContent() : null;
				value = e.getTextContent();
				if(valueId != null && !"".equals(valueId)) {
					isLookup = checkCharacteristicLookupValue(valueId, value, attributeId);
				}else {
					isLookup = false;
				}
//				System.out.println("### " + attributeId + " - " + isLookup);
				entities = entitiesMap.get(attributeId);
				if(entities == null) {
					entities = collectEntities(attributeId);
				}
				if(!entities.contains(entity)) {
					characteristicRecords.put( createCharacteristicValueObject(attributeId, isLookup && valueId != null && !"".equals(valueId) ? new org.json.JSONObject().put("_code", valueId) : value ) );
				}
			}
		}
	}
	
	private static boolean checkCharacteristicLookupValue(String code, String value, String characteristicId) {
		boolean isAlsoLookup = false;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Lookup->Lookup.Identifier,Characteristic.DataType");
		qp.put("query", "Characteristic.Identifier equals \"" + characteristicId + "\"");
		org.json.JSONObject response = null;
		response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
		if(response != null) {
			String lookup = null;
			if(response.getJSONArray("rows").length() > 0) {
				characteristicDataTypes.put(characteristicId, response .getJSONArray("rows")  .getJSONObject(0).getJSONArray("values").getString(1));
				lookup = response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0);
				if(lookup == null || "".equals(lookup)) {
//					System.out.println("La característica no tiene Lookup en P360: " + characteristicId);
				}else {
					isAlsoLookup = true;
					if(!checkValueInLookup(code, lookup)) {
						if(!checkValueInLookupByNameSpanish(value, lookup)) {
							insertLookupValue(code == null || "".equals(code) || "null".equals(code) ? value : code, value, lookup);
						}
					}
				}
			}
		}else{
			System.out.println("ERR: " + rw.getRawResponse());
		}
//		System.out.println("Característica: " + characteristicId + " - " + isAlsoLookup);
		return isAlsoLookup;
	}
	
	private static java.util.ArrayList<String> collectEntities(String characteristicId) {
		java.util.ArrayList<String> entities = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Entities");
		qp.put("query", "Characteristic.Identifier equals \"" + characteristicId + "\"");
		org.json.JSONObject response = null;
		response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
		if(response != null) {
			if(response.getJSONArray("rows").length() > 0) {
				org.json.JSONArray entitiesArray = response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(0);
				entities = new java.util.ArrayList<>();
				for(int i=0; i<entitiesArray.length(); i++) {
					entities.add(entitiesArray.getString(i));
				}
				entitiesMap.put(characteristicId, entities);
			}else {
				System.out.println("Not found characteristic: " + characteristicId);
			}
		}else{
			System.out.println("ERR: " + rw.getRawResponse());
		}
		return entities;
	}
	
	private static boolean checkCharacteristicExistance(String characteristicId) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier");
		qp.put("query", "Characteristic.Identifier equals \"" + characteristicId + "\"");
		org.json.JSONObject response = null;
		response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
		return response != null && response.getJSONArray("rows").length() > 0;
	}
	
	private static org.json.JSONArray collectStructureGroupClassifications(java.util.LinkedList<Node> classificationNodes, org.json.JSONArray characteristicRecords){
		org.json.JSONArray characteristicMapArray = new org.json.JSONArray();
		org.json.JSONObject mapEntry = null;
		String classificationId = null;
		String classificationNodeType = null;
		String supplierId = null;
		if(classificationNodes != null) {
			for(Node classificationNode : classificationNodes) {
				classificationNodeType = ((Element)classificationNode).getAttribute("Type");
				classificationId = ((Element)classificationNode).getAttribute("ClassificationID");
				if("WebsiteLink".equals(classificationNodeType)) {
					mapEntry = new org.json.JSONObject();
					mapEntry.put("_qualification", new org.json.JSONObject().put("structureGroup", new org.json.JSONObject().put("_externalId", "'" + classificationId + "'@'Sitios Web'")));
					characteristicMapArray.put(mapEntry);
				}else if("GALink".equals(classificationNodeType)) {
					mapEntry = new org.json.JSONObject();
					mapEntry.put("_qualification", new org.json.JSONObject().put("structureGroup", new org.json.JSONObject().put("_externalId", "'" + classificationId + "'@'" + ( classificationId.endsWith("ECC") ? "CommercialECC" : "CommercialS4H" ) + "'")));
//					characteristicMapArray.put(mapEntry);
				}else if("SupplierLink".equals(classificationNodeType)) {
					supplierId = classificationId.replaceAll("-SupplierProducts", "");
					handleProveedor(supplierId);
					characteristicRecords.put( createCharacteristicValueObject("SupplierID", supplierId) );
				}
			}
		}
		return characteristicMapArray;
	}
	
	private static void handleProveedor(String proveedor) {
		boolean existe = checkValueInLookup(proveedor, "Party");
		if(!existe) {
			insertLookupValue(proveedor, "Party");
		}
	}
	
	private static void insertLookupValue(String code, String lookup) {
		insertLookupValue(code, null, lookup);
	}
	
	private static void insertLookupValue(String code, String value, String lookup) {
		java.util.Map<String, String> empty = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		columns.put(new org.json.JSONObject().put("identifier", "LookupValueIdentifier.Code(\"STEP-PROD\")"));
		if(value != null && !"".equals(value)) {
			columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		}
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + code + "'@'" + lookup + "'")).put("values", new org.json.JSONArray().put(true).put(code)));
		if(value != null && !"".equals(value)) {
			rows.getJSONObject(0).getJSONArray("values").put(value);
		}
		request.put("columns", columns);
		request.put("rows", rows);
		org.json.JSONObject response = rw.makeRequest("POST", "/list/LookupValue", empty, request.toString());
		System.out.println("LookupValue wrote (" + lookup + "): " + (response == null ? rw.getRawResponse() : response));
	}

	private static boolean checkValueInLookup(String value, String lookup) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "'" + lookup + "'");
		qp.put("fields", "LookupValue.Code");
		qp.put("query", "LookupValue.Code equals \"" + value + "\"");
		org.json.JSONObject response = null;
		response = rw.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		return response != null && response.getJSONArray("rows").length() > 0;
	}

	private static boolean checkValueInLookupByNameSpanish(String value, String lookup) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "'" + lookup + "'");
		qp.put("fields", "LookupValue.Code");
		qp.put("query", "LookupValueLang.Name(es) equals \"" + value + "\"");
		org.json.JSONObject response = null;
		response = rw.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		return response != null && response.getJSONArray("rows").length() > 0;
	}

	private static org.json.JSONObject createCharacteristicValueObject(String characteristicName, Object value){
		return new org.json.JSONObject().put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(value)).put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx"))))).put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", characteristicName)));
	}
	
	
}
