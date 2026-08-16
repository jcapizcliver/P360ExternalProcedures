package mx.com.liverpool.p360.services.core.amqp;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.sshd.sftp.client.SftpClient;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class CrearArchivosParaSKUModifs {

	private RESTWorkshop workshop = new RESTWorkshop();
	private XMLMisc xmm = workshop.getXmm();
	private final java.util.Map<String, java.util.LinkedList< org.json.JSONObject >> characteristicsMap = new java.util.TreeMap<>();

	private String[] creacionDeArchivos(String externalId) throws ServiceUnavailableException {
		String[] content = new String[5];
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("entityFilter", "Product2G,Product2GCharacteristicValue,Product2GStructureGroupMap");
		qp.put("includeLabels", "true");
		qp.put("includeIds", "true");

		org.json.JSONObject response = null;
		org.json.JSONObject data = null;
		org.json.JSONArray characteristicRecords = null;
		java.util.Map<String, String> atributos = null;

		response = workshop.makeRequest("GET", "/object/Product2G/'" + externalId + "'@'MASTER'", qp, null);

		String sapObjectType = null;
		String business = null;
		String template = null;
		java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> lalista = null;
		java.util.LinkedList<String> lineasH = new java.util.LinkedList<>();
		java.util.LinkedList<String> lineasD = new java.util.LinkedList<>();
		java.util.LinkedList<String> lineasUOM = new java.util.LinkedList<>();
		java.util.LinkedList<String> lineasATT = new java.util.LinkedList<>();
		String lineasbb = null;
		String linea = null;

		StringBuilder sbH = new StringBuilder();
		StringBuilder sbUOM = new StringBuilder();
		StringBuilder sbATT= new StringBuilder();
		java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> yep = new java.util.LinkedList<>();
		String latapa = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date()) + "|" + new java.text.SimpleDateFormat("yyyyMMddhhmmss").format(new java.util.Date()) + "|STEP";
		if(response != null) {
			if(response.has("_data")) {
				data = response.getJSONObject("_data");
				if(data.has("_characteristicRecords")) {
					characteristicRecords = data.getJSONArray("_characteristicRecords");
					buildCharacteristicsMap(characteristicRecords, characteristicsMap);
					yep.addLast(new java.util.AbstractMap.SimpleEntry<>(externalId, characteristicsMap));
					sapObjectType = grabSimpleValue("SAPObjectType");
					business = grabSimpleValue("Business");
					System.out.println(" ******* " + business + " ******* ");
					atributos = seleccionaLasDesas(business);
					System.out.println("*** " + externalId + " ***");
					System.out.println("SAPObjectType: " + sapObjectType);
					System.out.println("Negocio: " + business);
					System.out.println("TemplateId: " + getTemplateFromData(data));
					System.out.println();
					linea = "SBB".equals(business)?getHFileLineJana(externalId, template = getTemplateFromData(data), characteristicsMap):getHFileLine(externalId, template = getTemplateFromData(data), characteristicsMap);
					lineasH.addLast(linea);
					lalista = ayMisHijosNormal(externalId);
					if("00".equals(sapObjectType)) {
						aymisHijoS(externalId, lineasH);
						if("MKP".equals(business)) {
							getDFileLines(business, externalId, lalista, atributos, lineasD);
						}else {
							if(!lalista.isEmpty()) {
								if("SBB".equals(business)) {
//									getDFileLinesJana(business, template, lalista, atributos, lineasD);
									getDFileLinesMarcianoJana(business, template, externalId, lalista.getFirst(), atributos, lineasD);
								}else {
//									getDFileLines(business, template, yep, atributos, lineasD);
									getDFileLinesMarciano(business, template, externalId, lalista.getFirst(), atributos, lineasD);
								}
							} else {
								System.out.println("No estuvieron mis hijos");
							}
						}
					}else if("01".equals(sapObjectType)) {
						if(!"SBB".equals(business)) {
							System.out.println("***********");
							getDFileLines(business, template, yep, atributos, lineasD);
							System.out.println(String.join("\n", lineasD));
							getDFileLines(business, externalId, lalista, atributos, lineasD);
							System.out.println("/////////");
							System.out.println(String.join("\n", lineasD));
						}else {
							getDFileLinesJana(business, externalId.substring(1), lalista, atributos, lineasD);
						}
					}

					if("SBB".equals(business)) {

						lineasbb = getHFileLineJanaUOM(externalId, template = getTemplateFromData(data), characteristicsMap);
						lineasUOM.addLast(lineasbb);

						getAttFileLinesJana(business, template, yep, atributos, lineasATT);

						sbH.append( latapa );
						sbH.append("\n");
						sbH.append(String.join("\n", lineasH));
						sbH.append("\nEOF|" + (lineasH.size() + 2));
						sbUOM.append( latapa );
						sbUOM.append("\n");
						sbUOM.append(String.join("\n", lineasUOM));
						sbUOM.append("\nEOF|" + (lineasUOM.size() + 2));
						sbATT.append( latapa );
						sbATT.append(lineasATT.isEmpty() ? "" :"\n");
						sbATT.append(String.join("\n", lineasATT));
						sbATT.append("\nEOF|" + (lineasATT.size() + 2));
						content[0] = sbH.toString();
						content[1] = latapa + "\n" + String.join("\n", lineasD) + "\nEOF|" + (lineasD.size() + 2);
						content[2] = sbUOM.toString();
						content[3] = sbATT.toString();
						content[4] = business;
						System.out.println("**** MD ****");
						System.out.println( content[0] );
						System.out.println("**** VAR ****");
						System.out.println( content[1] );
						System.out.println("**** UOM ****");
						System.out.println( content[2] );
						System.out.println("**** ATT ****");
						System.out.println( content[3] );
					}else {

						sbH.append( latapa );
						sbH.append("\n");
						sbH.append(String.join("\n", lineasH));
						sbH.append("\nEOF|" + (lineasH.size() + 2));

						content[0] = sbH.toString();
						content[1] = latapa + "\n" + String.join("\n", lineasD) + "\nEOF|" + (lineasD.size() + 2);
						content[2] = latapa + "\nEOF|2";
						content[3] = latapa + "\n" + lalinea(externalId, template) + "\nEOF|3";
						content[4] = business;
						System.out.println( content[0] );
						System.out.println( content[1] );
//						System.out.println( content[2] );
//						System.out.println( content[3] );
					}

				}
			}
		}else {
			System.out.println("-->" + workshop.getRawResponse() + "<--");
		}
		return content;
	}

	private String lalinea(String id, String parentId) {
//		ID||'|'||SupplierID||'|'||SKU||'|'||PARENT_ID||'||T|'||ArgumentoDeVenta
		return id + "|" + grabSimpleValue("SupplierID") + "|" + grabSimpleValue("SKU") + "|" + parentId + "||T|" + grabSimpleValue("ArgumentoDeVenta");
	}

	private String getHFileLineJanaUOM(String id, String parentId, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicMap) {
		return  id.substring(1) +"|"+
				"0007|"+
				grabSimpleValue("SupplierPartNumber", characteristicMap)+"|"+
				"PI|"+
				"1|"+
				"1|"+
				grabSimpleValue("ProductDepth", characteristicMap)+"|"+
				grabSimpleValue("ProductWidth", characteristicMap)+"|"+
				grabSimpleValue("ProductHeight", characteristicMap)+"|"+
				grabSimpleValue("UnidadDeMedidaLongitud", characteristicMap)+"|"+
				grabSimpleValue("VOLUMAtt", characteristicMap)+"|"+
				grabSimpleValue("UnidadDeMedidaVolumen", characteristicMap)+"|"+
				grabSimpleValue("PesoBruto", characteristicMap)+"|"+
				grabSimpleValue("ProductWeight", characteristicMap)+"|"+
				grabSimpleValue("UnidadDeMedidaPeso", characteristicMap);
	}

	private void buildCharacteristicsMap(org.json.JSONArray characteristicRecords, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicsMap) {
		if(!characteristicsMap.isEmpty()) {
			characteristicsMap.clear();
		}
		org.json.JSONObject characteristicRecord = null;
		String characteristicIdentifier = null;
		java.util.LinkedList<org.json.JSONObject> objectList = null;
		for(int i=0; i<characteristicRecords.length(); i++) {
			characteristicRecord = characteristicRecords.getJSONObject(i);
			characteristicIdentifier = characteristicRecord.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			objectList = characteristicsMap.get(characteristicIdentifier);
			if(objectList == null) {
				objectList = new java.util.LinkedList<>();
				characteristicsMap.put(characteristicIdentifier, objectList);
			}
			objectList.addLast(characteristicRecord);
		}
	}

	public void aymisHijoS(String proposalId, java.util.LinkedList<String> lineasH) throws ServiceUnavailableException {
		java.util.LinkedList<java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> lalista = new java.util.LinkedList<>();
		java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> elmapa = null;
		String linea = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID");
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + proposalId + "\") = \"" + proposalId + "\"");
		qp.put("pageSize", "1200");
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/Article/bySearch");
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		String variantId = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			qp.clear();
			qp.put("entityFilter", "ArticleCharacteristicValue");
			qp.put("includeLabels", "true");
			qp.put("includeIds", "true");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				variantId = values.getString(0);
				response = workshop.makeRequest("GET", "/object/Article/'" + variantId + "'@'MASTER'");
				if(response.has("_data") && response.getJSONObject("_data").has("_characteristicRecords")) {
					elmapa = new java.util.TreeMap<>();
					buildCharacteristicsMap(response.getJSONObject("_data").getJSONArray("_characteristicRecords"), elmapa);
					lalista.addLast(elmapa);
				}
				linea = getHFileLine(variantId, proposalId, characteristicsMap);
				lineasH.addLast(linea);
			}
		}
	}

	public java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> ayMisHijosNormal(String proposalId) throws ServiceUnavailableException{
		java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> lalista = new java.util.LinkedList<>();
		java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> elmapa = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID");
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + proposalId + "\") = \"" + proposalId + "\"");
		qp.put("pageSize", "1200");
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		String variantId = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			qp.clear();
			qp.put("entityFilter", "ArticleCharacteristicValue");
			qp.put("includeLabels", "true");
			qp.put("includeIds", "true");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				variantId = values.getString(0);
				response = workshop.makeRequest("GET", "/object/Article/'" + variantId + "'@'MASTER'?includeLabels=true");
				if(response.has("_data") && response.getJSONObject("_data").has("_characteristicRecords")) {
					elmapa = new java.util.TreeMap<>();
					buildCharacteristicsMap(response.getJSONObject("_data").getJSONArray("_characteristicRecords"), elmapa);
					lalista.addLast(new java.util.AbstractMap.SimpleEntry<>(variantId, elmapa));
				}
			}
		}else {
			System.out.println("### ERR: " + workshop.getRawResponse());
		}
		return lalista;
	}

	private void getAttFileLinesJana(String business, String parentId, java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> lalista, java.util.Map<String, String> lasDesas, java.util.LinkedList<String> lineasD) {
		int counter = 1;
		String elese = null;
		for(java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> entry : lalista ) {
			for(java.util.Map.Entry<String, String> ladesa : lasDesas.entrySet()) {
				elese = grabSimpleValue(ladesa.getKey(), entry.getValue());
				if(elese != null && !"".equals(elese)) {
					lineasD.addLast(
						  entry.getKey() + "|"
						+ grabSimpleValue("SKU") + "|"
						+ aSeisPosiciones(counter) + "|"
						+ ladesa.getValue() + "|"
						+ elese
					);
					counter++;
				}
			}
			counter = 1;
		}
		System.out.println("<:::::::>" + lineasD.size());
	}

	private String getCodeLatalla(String value, String campoLatalla) {
		log("Looking for: " + value + " in: " + campoLatalla + "LOV");
//		System.out.println("Looking for: -->" + value + "<-- in: " + campoLatalla + "LOV");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code");
		qp.put("query", "LookupValueLang.Name(es) equals \"" + value.replaceAll("\"", "\\\\\"") + "\"");
		qp.put("lookup", "'" + campoLatalla + "LOV'");
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
//			System.out.println("<::>" + rows + "<::>");
			return rows.length() > 0 ? rows.getJSONObject(0).getJSONArray("values").getString(0) : null;
		}else {
			System.out.println("### ERR: " + workshop.getRawResponse());
		}
		return null;
	}

	private String getSAPFieldECC(String field) {
		log("Looking for: " + field + " in: Characteristics");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValueIdentifier.Code(ECC)");
		qp.put("query", "LookupValueLang.Name(es) equals \"" + field + "\"");
		qp.put("lookup", "'Characteristics'");
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			return rows.length() > 0 ? rows.getJSONObject(0).getJSONArray("values").getString(0) : null;
		}else {
			System.out.println("### ERR: " + workshop.getRawResponse());
		}
		return null;
	}

	private void getDFileLinesJana(String business, String parentId, java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> lalista, java.util.Map<String, String> lasDesas, java.util.LinkedList<String> lineasD) throws ServiceUnavailableException {
		String elcampoLatalla = null;
		String itemGroup = null;
		String latalla = null;
		String color = null;
		String localCode = null;
		itemGroup = grabSimpleValue("SBB".equals(business) ? "ItemGroupS4H" : "ItemGroup");
		if(itemGroup == null || "".equals(itemGroup)) {
		}
		elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
		System.out.println("Asking for latalla: " + itemGroup + " on " + business + ": " + elcampoLatalla);
		if(elcampoLatalla != null) {

			System.out.println(elcampoLatalla + " - " + lasDesas.get(elcampoLatalla));
		}
		if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
			elcampoLatalla = "TMU01";
		}
		for(java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> entry : lalista ) {
			latalla = grabSimpleValue("TamanoUnico", entry.getValue(), true);
			localCode = getCodeLatalla(latalla, elcampoLatalla);
			log("Getting local code for latalla: " + localCode + ".");
			color = grabSimpleValue("SB_COLORES", entry.getValue());
			color = color == null || "".equals(color) ? grabSimpleValue("ColoursLiverpoolAtt", entry.getValue()) : color;

			elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
			if(elcampoLatalla != null) {
				System.out.println(elcampoLatalla + " - " + lasDesas);
			}
			if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
				elcampoLatalla = "TMU01";
			}else {
			}
			System.out.println("Poniendo: " + localCode + " en: " + elcampoLatalla);
			System.out.println("--->" + entry.getKey());
			lineasD.addLast(
					grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
					+ entry.getKey().substring(1) + "|"
					+ parentId + "|"//ID step
					+ "0007|"
					+ "|"
					+ grabSimpleValue("SupplierPartNumber") + "|"
					+ ( "".equals(grabSimpleValue("SupplierPartNumber", entry.getValue())) ? grabSimpleValue("SupplierPartNumber") : grabSimpleValue("SupplierPartNumber", entry.getValue()) ) + "|"
					+ paddZeros(4, color) + "|"
					+ paddZeros(4, localCode) + "|"
					+ grabSimpleValue("CostoNetoSinIVA") + "|"
					+ grabSimpleValue("Currency") + "|"
					+ grabSimpleValue("PrecioSugeridocIVA") + "|"
					+ grabSimpleValue("SKU", entry.getValue()) + "|"
					+ grabSimpleValue("MainBarCodeS4H", entry.getValue()) + "|"
					+ grabSimpleValue("NUMTP_S4H", entry.getValue()) + "|"
					+ ( "".equals(grabSimpleValue("SupplierPartNumber", entry.getValue())) ? grabSimpleValue("SupplierPartNumber") : grabSimpleValue("SupplierPartNumber", entry.getValue()) )
				);
			// ("00".equals(grabSimpleValue("SAPObjectType", entry.getValue())) ? ("".equals( grabSimpleValue("SupplierPartNumber", entry.getValue())) ? grabSimpleValue("SupplierPartNumber") : grabSimpleValue("SupplierPartNumber", entry.getValue()) ) : grabSimpleValue("SupplierPartNumber", entry.getValue()) )
		}
	}

	private void getDFileLinesMarcianoJana(String business, String parentId, String id, java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> entry, java.util.Map<String, String> lasDesas, java.util.LinkedList<String> lineasD) throws ServiceUnavailableException {
		String elcampoLatalla = null;
		String itemGroup = null;
		String latalla = null;
		String localCode = null;
		String color = null;
		itemGroup = grabSimpleValue("SBB".equals(business) ? "ItemGroupS4H" : "ItemGroup");
		if(itemGroup == null || "".equals(itemGroup)) {
		}
		elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
		System.out.println("Asking for latalla: " + itemGroup + " on " + business + ": " + elcampoLatalla);
		if(elcampoLatalla != null) {
			System.out.println(elcampoLatalla + " - " + lasDesas.get(elcampoLatalla));
		}
		if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
			elcampoLatalla = "TMU01";
		}
		latalla = grabSimpleValue("TamanoUnico", entry.getValue(), true);
		color = grabSimpleValue("SB_COLORES", entry.getValue());
		color = color == null || "".equals(color) ? grabSimpleValue("ColoursLiverpoolAtt", entry.getValue()) : color;

		elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
		if(elcampoLatalla != null) {
			System.out.println(elcampoLatalla + " - " + lasDesas);
		}
		if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
			elcampoLatalla = "TMU01";
		}else {
		}
		localCode = getCodeLatalla(latalla, elcampoLatalla);
		log("Getting local code for latalla: " + localCode + ".");

		lineasD.addLast(
				grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
				+ id.substring(1) + "|"
				+ parentId + "|"//ID step
				+ "0007|"
				+ "|"
				+ "|"
				+ grabSimpleValue("SupplierPartNumber") + "|"
				+ paddZeros(4, color) + "|"
				+ paddZeros(4, latalla) + "|"
				+ grabSimpleValue("CostoNetoSinIVA") + "|"
				+ grabSimpleValue("Currency") + "|"
				+ grabSimpleValue("PrecioSugeridocIVA") + "|"
				+ ("".equals(grabSimpleValue("SKU", entry.getValue())) ? grabSimpleValue("SKU") : grabSimpleValue("SKU", entry.getValue())) + "|"
				+ ("".equals(grabSimpleValue("MainBarCodeS4H", entry.getValue())) ? grabSimpleValue("MainBarCodeS4H") : grabSimpleValue("MainBarCodeS4H", entry.getValue())) + "|"
				+ ("".equals(grabSimpleValue("NUMTP_S4H", entry.getValue())) ? grabSimpleValue("NUMTP_S4H") : grabSimpleValue("NUMTP_S4H", entry.getValue())) + "|"
				+ ( "".equals(grabSimpleValue("SupplierPartNumber", entry.getValue())) ? grabSimpleValue("SupplierPartNumber") : grabSimpleValue("SupplierPartNumber", entry.getValue()))
			);
	}

	private String getHFileLineJana(String id, String parentId, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicMap) {
		return  id.substring(1) +"|"+
				parentId+"|"+
				grabSimpleValue("SupplierPartNumber", characteristicMap)+"|"+
				"0007|"+
				grabSimpleValue("FSH_SEASON_YEAR", characteristicMap)+"|"+
				grabSimpleValue("FSH_SEASON", characteristicMap)+"|"+
				grabSimpleValue("FSH_COLLECTION", characteristicMap)+"|"+
				paddZeros(10, grabSimpleValue("SupplierID", characteristicMap)) +"|"+
				grabSimpleValue("TImportacion", characteristicMap)+"|"+
				grabSimpleValue("ItemGroupS4H", characteristicMap)+"|"+
				grabSimpleValue("SAPObjectType", characteristicMap)+"|"+
				grabSimpleValue("WHERL", characteristicMap)+"|"+
				grabSimpleValue("BRAND_ID_S4H", characteristicMap)+"|"+
				"|"+
				grabSimpleValue("ZZLIC_S4H", characteristicMap)+"|"+
				"|"+
				"|"+
				grabSimpleValue("PLGTP", characteristicMap)+"|"+
				grabSimpleValue("FSH_THEME", characteristicMap)+"|"+
				grabSimpleValue("FIBER_CODE1", characteristicMap)+"|"+
				grabSimpleValue("FIBER_CODE2", characteristicMap)+"|"+
				grabSimpleValue("FIBER_CODE3", characteristicMap)+"|"+
				grabSimpleValue("FIBER_CODE4", characteristicMap)+"|"+
				grabSimpleValue("FIBER_CODE5", characteristicMap)+"|"+
				grabSimpleValue("FIBER_PART1", characteristicMap)+"|"+
				grabSimpleValue("FIBER_PART2", characteristicMap)+"|"+
				grabSimpleValue("FIBER_PART3", characteristicMap)+"|"+
				grabSimpleValue("FIBER_PART4", characteristicMap)+"|"+
				grabSimpleValue("FIBER_PART5", characteristicMap)+"|"+
				grabSimpleValue("LABOR_S4H", characteristicMap)+"|"+
				grabSimpleValue("SAP_BEHVO", characteristicMap)+"|"+
				grabSimpleValue("CostoNetoSinIVA", characteristicMap)+"|"+
				grabSimpleValue("Currency", characteristicMap)+"|"+
				grabSimpleValue("PrecioSugeridocIVA", characteristicMap)+"|"+
				grabSimpleValue("SKU", characteristicMap)+"|"+
				grabSimpleValue("MainBarCodeS4H", characteristicMap)+"|"+
				grabSimpleValue("NUMTP_S4H", characteristicMap)+"|"+
				grabSimpleValue("ArgumentoDeVenta", characteristicMap)+"|"+
				grabSimpleValue("ProductName", characteristicMap)+"|"+
				grabSimpleValue("Status", characteristicMap)+"|"+
				grabSimpleValue("BWSCL", characteristicMap)+"|"+
				grabSimpleValue("EXTWG_S4H", characteristicMap)+"|"+
				grabSimpleValue("TAXKM1_S4H", characteristicMap)+"|"+
				grabSimpleValue("TAXKM2_S4H", characteristicMap)+"|"+
				grabSimpleValue("TAXM3_S4H", characteristicMap)+"|"+
				grabSimpleValue("TextoAdicional", characteristicMap)+"|"+
				grabSimpleValue("BWVOR", characteristicMap)+"|"+
				grabSimpleValue("MAX_STACK", characteristicMap)+"|"+
				grabSimpleValue("MesdeEntregadeMercancIa", characteristicMap)+"|"+
				grabSimpleValue("GROES", characteristicMap);
	}

	private String getHFileLine(String id, String parentId, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicMap) {
		return  id +"|"+
				grabSimpleValue("SupplierID", characteristicMap)+"|"+
				grabSimpleValue("SKU", characteristicMap)+"|"+
				parentId+"|"+
				"|"+
				"H|"+
				grabSimpleValue("SAPObjectType", characteristicMap)+"|"+
				grabSimpleValue("ZNUMV", characteristicMap)+"|"+
				grabSimpleValue("SkuType", characteristicMap)+"|"+
				grabSimpleValue("ItemGroup", characteristicMap)+"|"+
				grabSimpleValue("Negocio", characteristicMap)+"|"+
				grabSimpleValue("Temporada", characteristicMap)+"|"+
				grabSimpleValue("AnoEstacion", characteristicMap)+"|"+
				grabSimpleValue("TImportacion", characteristicMap)+"|"+
				grabSimpleValue("BrandName", characteristicMap)+"|"+
				grabSimpleValue("LicenseDescription", characteristicMap)+"|"+
				grabSimpleValue("GradoDemoda", characteristicMap)+"|"+
				"UN|"+
				grabSimpleValue("SAP_BEHVO", characteristicMap)+"|"+
				grabSimpleValue("WHERL", characteristicMap)+"|"+
				grabSimpleValue("RegionTEMP", characteristicMap) +
				"UN|"+
				grabSimpleValue("TypeMainBarCode", characteristicMap)+"|"+
				grabSimpleValue("MainBarCode", characteristicMap)+"|"+
				grabSimpleValue("PerfilDeRedondeo", characteristicMap)+"|"+
				grabSimpleValue("SupplierPartNumber", characteristicMap)+"|"+
				grabSimpleValue("ProductWidth", characteristicMap)+"|"+
				grabSimpleValue("ProductDepth", characteristicMap)+"|"+
				grabSimpleValue("ProductHeight", characteristicMap)+"|"+
				grabSimpleValue("VOLUMAtt", characteristicMap)+"|"+
				grabSimpleValue("PesoBruto", characteristicMap)+"|"+
				grabSimpleValue("ZNTGCJ", characteristicMap)+"|"+
				grabSimpleValue("UnidadDeMedidaLongitud", characteristicMap)+"|"+
				grabSimpleValue("UnidadDeMedidaVolumen", characteristicMap)+"|"+
				grabSimpleValue("UnidadDeMedidaPeso", characteristicMap)+"|"+
				grabSimpleValue("CostoEnMonedaExtranjera", characteristicMap)+"|"+
				grabSimpleValue("FechaInicioVigenciaCostoImportacion", characteristicMap)+"|"+
				("I".equals(grabSimpleValue("TImportacion", characteristicMap)) ? grabSimpleValue("Currency", characteristicMap) : "" )+"|"+
				grabSimpleValue("CostobrutoSinIVA", characteristicMap)+"|"+
				grabSimpleValue("PrecioSugeridocIVA", characteristicMap)+"|"+
				grabSimpleValue("FechaInicioVigenciaPrecioVenta", characteristicMap)+"|"+
				"MXP" +"|"+
				grabSimpleValue("IndicadordeImpuesto", characteristicMap)+"|"+
				grabSimpleValue("Descuento1", characteristicMap)+"|"+
				grabSimpleValue("Descuento2", characteristicMap)+"|"+
				"|"+
				"|"+
				grabSimpleValue("CostoNetoSinIVA", characteristicMap)+"|"+
				grabSimpleValue("FechaInicioVigenciaCostoNeto", characteristicMap)+"|"+
				"|"+
				grabSimpleValue("ProductName", characteristicMap)+"|"+
				grabSimpleValue("TextoAdicional", characteristicMap)+"|"+
				"|"+
				"|"+
				grabSimpleValue("MesdeEntregadeMercancIa", characteristicMap)+"|"+
				grabSimpleValue("Evento", characteristicMap)+"|"+
				"|"+
				grabSimpleValue("ProductTypeSAP", characteristicMap)+"|"+
				grabSimpleValue("Coleccion", characteristicMap)+"|"+
				grabSimpleValue("Armado", characteristicMap)+"|"+
				grabSimpleValue("MesdeEntregadeMuestra", characteristicMap)+"|"+
				grabSimpleValue("ZBRECJ", characteristicMap)+"|"+
				grabSimpleValue("ZLAECJ", characteristicMap)+"|"+
				grabSimpleValue("ZHOECJ", characteristicMap)+"|"+
				grabSimpleValue("UnidadDeMedidaLongitud", characteristicMap)+"|"+
				grabSimpleValue("ZVOLCJ", characteristicMap)+"|"+
				grabSimpleValue("UnidadDeMedidaVolumen", characteristicMap)+"|"+
				grabSimpleValue("ZBRGCJ", characteristicMap)+"|"+
				grabSimpleValue("ZNTGCJ", characteristicMap)+"|"+
				grabSimpleValue("UnidadDeMedidaPeso", characteristicMap)+"|"+
				grabSimpleValue("HNDLCODE", characteristicMap)+"|"+
				grabSimpleValue("WHSTC", characteristicMap)+"|"+
				grabSimpleValue("MVGR5", characteristicMap)+"|"+
				"|"+
				"|"+
				grabSimpleValue("ZHOEPQ", characteristicMap)+"|"+
				grabSimpleValue("ZBREPQ", characteristicMap)+"|"+
				grabSimpleValue("ZLAEPQ", characteristicMap)+"|"+
				grabSimpleValue("UnidadDeMedidaLongitud", characteristicMap)+"|"+
				grabSimpleValue("ZVOLPQ", characteristicMap)+"|"+
				grabSimpleValue("UnidadDeMedidaVolumen", characteristicMap)+"|"+
				grabSimpleValue("ZVOLPQ", characteristicMap)+"|"+
				grabSimpleValue("ZBRGPQ", characteristicMap)+"|"+
				grabSimpleValue("UnidadDeMedidaPeso", characteristicMap)+"|"+
				grabSimpleValue("MAX_STACK", characteristicMap);
	}

	private java.util.Map<String, String> seleccionaLasDesas(String business){
		java.util.Map<String, String> lasdesas = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "'Characteristics'");
		qp.put("query", "LookupValueReference.LookupValues('AttributeGroup')->LookupValue.Code in (\"" + ("SBB".equals(business) ? "CategorySpecificAttributesS4H" : "CategorySpecificAttributesSAP") + "\")");
		qp.put("fields", "LookupValue.Code,LookupValueIdentifier.Code(" + ("SBB".equals(business) ? "S4HANA" : "ECC") + ")");
		qp.put("pageSize", "250");

		int currentIndex = 0;
		int totalSize = 0;

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				lasdesas.put(values.getString(0),values.getString(1));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		return lasdesas;
	}

	private void getDFileLinesMarciano(String business, String parentId, String id, java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> entry, java.util.Map<String, String> lasDesas, java.util.LinkedList<String> lineasD) throws ServiceUnavailableException {
		int counter = 1;
		String elese = null;
		String typeMainBarCode = null;
		String elcampoLatalla = null;
		String latalla = null;
		String localCode = null;
		latalla = grabSimpleValue("TamanoUnico", entry.getValue(), true);
		String itemGroup = null; // grabSimpleValue("ItemGroup");
		itemGroup = grabSimpleValue(!"Suburbia".equals(business) ? "ItemGroup" : "ItemGrouopS4G");
		if(itemGroup == null || "".equals(itemGroup)) {
			//PANIC
		}
		System.out.println("(Mars 1.1) El campo la talla: " + itemGroup + " (latalla: " + latalla + ")");
		typeMainBarCode = grabSimpleValue("TypeMainBarCode", entry.getValue());
		if(typeMainBarCode == null || "".equals(typeMainBarCode)) {
			typeMainBarCode = grabSimpleValue("TypeMainBarCode");
		}
		if(latalla != null && !"".equals(latalla)) {
			elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
			System.out.println("El campoLatalla: " + elcampoLatalla);
			if(elcampoLatalla != null) {
				elcampoLatalla = lasDesas.get(elcampoLatalla);
			}
			localCode = getCodeLatalla(latalla, elcampoLatalla);
			log("Getting local code for latalla: " + localCode + " (campoLatalla: " + elcampoLatalla + ").");
			System.out.println("Getting local code for latalla: " + localCode + " (campoLatalla: " + elcampoLatalla + ").");
			System.out.println("(Mars) El campo la talla: " + elcampoLatalla + " (" + itemGroup + ")");
			if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
				elcampoLatalla = "TMU01";
			}
			lineasD.addLast(
				  id + "|"
				+ grabSimpleValue("SupplierID") + "|"
				+ grabSimpleValue("SKU", entry.getValue()) + "|"
				+ parentId + "|"
				+ ""  + "|"
				+ "D" + "|"
				+ grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
				+ ("00".equals(grabSimpleValue("SAPObjectType", entry.getValue())) ? "" : paddZeros(4, grabSimpleValue("ZNUMV") ) ) + "|"
				+ typeMainBarCode + "|"
				+ "" + "|"
				+ aSeisPosiciones(counter) + "|"
				+ elcampoLatalla + "|"
				+ paddZeros(4, localCode) + "|"
				+ "" + "|"
				+ grabSimpleValue("PrecioSugeridocIVA") + "|"
				+ grabSimpleValue("FechaInicioVigenciaPrecioVenta") + "|"
				+ grabSimpleValue("CostoEnMonedaExtranjera", entry.getValue()) + "|"
				+ grabSimpleValue("FechaInicioVigenciaCostoImportacion") + "|"
				+ grabSimpleValue("CostoNetoSinIVA") + "|"
				+ grabSimpleValue("FechaInicioVigenciaCostoNeto") + "|"
				+ grabSimpleValue("MainBarCode", entry.getValue()) + "|"
				+ grabSimpleValue("TextoAdicional") + "|"
				+ grabSimpleValue("CostobrutoSinIVA") + "|"
				+ grabSimpleValue("SupplierPartNumber", entry.getValue())
			);
			counter++;
		}
		for(java.util.Map.Entry<String, String> ladesa : lasDesas.entrySet()) {
			elese = grabSimpleValue(ladesa.getKey());
			if(elese == null || "".equals(elese)) {
				elese = grabSimpleValue(ladesa.getKey(), entry.getValue());
			}
			if(elese != null && !"".equals(elese)) {
				lineasD.addLast(
					  id + "|"
					+ grabSimpleValue("SupplierID") + "|"
					+ grabSimpleValue("SKU", entry.getValue()) + "|"
					+ parentId + "|"
					+ ""  + "|"
					+ "D" + "|"
					+ grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
					+ ("00".equals(grabSimpleValue("SAPObjectType",entry.getValue())) ? "" : paddZeros(4, grabSimpleValue("ZNUMV") ) ) + "|"
					+ typeMainBarCode + "|"
					+ "" + "|"
					+ aSeisPosiciones(counter) + "|"
					+ ladesa.getValue() + "|"
					+ elese + "|"
					+ "" + "|"
					+ grabSimpleValue("PrecioSugeridocIVA") + "|"
					+ grabSimpleValue("FechaInicioVigenciaPrecioVenta") + "|"
					+ grabSimpleValue("CostoEnMonedaExtranjera", entry.getValue()) + "|"
					+ grabSimpleValue("FechaInicioVigenciaCostoImportacion") + "|"
					+ grabSimpleValue("CostoNetoSinIVA") + "|"
					+ grabSimpleValue("FechaInicioVigenciaCostoNeto") + "|"
					+ grabSimpleValue("MainBarCode", entry.getValue()) + "|"
					+ grabSimpleValue("TextoAdicional") + "|"
					+ grabSimpleValue("CostobrutoSinIVA") + "|"
					+ grabSimpleValue("SupplierPartNumber", entry.getValue())
				);
				counter++;
			}
		}
		counter = 1;

	}

	private String getAtributoSapLatalla(String itemGroup, String business) throws ServiceUnavailableException {
		String value = null;
		RESTWorkshop rw = new RESTWorkshop();
		String dp = ("SBB".equals(business) ? "TallaUnicavsTallaS4H" : "TallaUnicavsTallaERP");
		rw.putParameter("dictionaryProxy", "'" + dp + "'");
		rw.putParameter("fields", "StandardizationValue.AlternativeValue");
		rw.putParameter("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + dp + "\" and StandardizationValue.Value equals \"" + itemGroup + "\"");

		org.json.JSONObject response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch");
		if(response != null) {
			org.json.JSONArray rows = response.getJSONArray("rows");
			if(rows.length() > 0) {
				value = rows.getJSONObject(0).getJSONArray("values").getString(0);
			}
		}else {
			System.out.println("###$$ ERROR: " + rw.getRawResponse());
		}
		if(value == null || "".equals(value) && !"SBB".equals(business)) {
			dp = ("ItemGroupSAPSizeAttribute");
			rw.putParameter("dictionaryProxy", "'" + dp + "'");
			rw.putParameter("fields", "StandardizationValue.AlternativeValue");
			rw.putParameter("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + dp + "\" and StandardizationValue.Value equals \"" + itemGroup + "\"");

			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch");
			if(response != null) {
				org.json.JSONArray rows = response.getJSONArray("rows");
				if(rows.length() > 0) {
					value = rows.getJSONObject(0).getJSONArray("values").getString(0);
				}
			}else {
				System.out.println("###$$ ERROR: " + rw.getRawResponse());
			}
		}
		return value;
	}

	private void getDFileLines(
			String business,
			String parentId,
			java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> lalista,
			java.util.Map<String, String> lasDesas,
			java.util.LinkedList<String> lineasD) throws ServiceUnavailableException {
		int counter = 1;
		String elese = null;
		String elcampoLatalla = null;
		String itemGroup = null;
		String latalla = null;
		String localCode = null;
		itemGroup = grabSimpleValue(!"SBB".equals(business) ? "ItemGroup" : "ItemGrouopS4G");
		if(itemGroup == null || "".equals(itemGroup)) {
			//PANIC
		}
		elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
		System.out.println("(Normal) El campo la talla: " + elcampoLatalla + " (" + itemGroup + ", " + business + ")");
		if(elcampoLatalla != null) {
			System.out.println(elcampoLatalla + " - " + lasDesas.get(elcampoLatalla));
			elcampoLatalla = lasDesas.get(elcampoLatalla);
		}
		if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
			elcampoLatalla = "TMU01";
		}
		for(java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> entry : lalista ) {
			for(java.util.Map.Entry<String, String> ladesa : lasDesas.entrySet()) {
				elese = grabSimpleValue(ladesa.getKey(), entry.getValue());
				if(elese != null && !"".equals(elese)) {
					lineasD.addLast(
						  entry.getKey() + "|"
						+ grabSimpleValue("SupplierID") + "|"
						+ grabSimpleValue("SKU", entry.getValue()) + "|"
						+ parentId + "|"
						+ ""  + "|"
						+ "D" + "|"
						+ grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
						+ ("00".equals(grabSimpleValue("SAPObjectType",entry.getValue())) ? "" : paddZeros(4, grabSimpleValue("ZNUMV") ) ) + "|"
						+ grabSimpleValue("TypeMainBarCode", entry.getValue()) + "|"
						+ "" + "|"
						+ aSeisPosiciones(counter) + "|"
						+ ladesa.getValue() + "|"
						+ elese + "|"
						+ "" + "|"
						+ grabSimpleValue("PrecioSugeridocIVA") + "|"
						+ grabSimpleValue("FechaInicioVigenciaPrecioVenta") + "|"
						+ grabSimpleValue("CostoEnMonedaExtranjera", entry.getValue()) + "|"
						+ grabSimpleValue("FechaInicioVigenciaCostoImportacion") + "|"
						+ grabSimpleValue("CostoNetoSinIVA") + "|"
						+ grabSimpleValue("FechaInicioVigenciaCostoNeto") + "|"
						+ grabSimpleValue("MainBarCode", entry.getValue()) + "|"
						+ grabSimpleValue("TextoAdicional") + "|"
						+ grabSimpleValue("CostobrutoSinIVA") + "|"
						+ grabSimpleValue("Status") + "|"
						+ grabSimpleValue("TipoDeEtiqueta") + "|"
						+ grabSimpleValue("SupplierPartNumber", entry.getValue())
					);
					counter++;
				}
			}
			latalla = grabSimpleValue("TamanoUnico", entry.getValue(), true);
			if(latalla != null && !"".equals(latalla)) {
				elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
				if(elcampoLatalla != null) {
					System.out.println(elcampoLatalla + " - " + lasDesas);
					elcampoLatalla = lasDesas.get(elcampoLatalla);
				}
				if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
					elcampoLatalla = "TMU01";
				}
				localCode = getCodeLatalla(latalla, elcampoLatalla);
				log("Getting local code for latalla: " + localCode + ".");
				lineasD.addLast(
					  entry.getKey() + "|"
					+ grabSimpleValue("SupplierID") + "|"
					+ grabSimpleValue("SKU", entry.getValue()) + "|"
					+ parentId + "|"
					+ ""  + "|"
					+ "D" + "|"
					+ grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
					+ ("00".equals(grabSimpleValue("SAPObjectType")) ? "" : paddZeros(4, grabSimpleValue("ZNUMV") ) ) + "|"
					+ grabSimpleValue("TypeMainBarCode", entry.getValue()) + "|"
					+ "" + "|"
					+ aSeisPosiciones(counter) + "|"
					+ elcampoLatalla + "|"
					+ paddZeros(4, localCode) + "|"
					+ "" + "|"
					+ grabSimpleValue("PrecioSugeridocIVA") + "|"
					+ grabSimpleValue("FechaInicioVigenciaPrecioVenta") + "|"
					+ grabSimpleValue("CostoEnMonedaExtranjera", entry.getValue()) + "|"
					+ grabSimpleValue("FechaInicioVigenciaCostoImportacion") + "|"
					+ grabSimpleValue("CostoNetoSinIVA") + "|"
					+ grabSimpleValue("FechaInicioVigenciaCostoNeto") + "|"
					+ grabSimpleValue("MainBarCode", entry.getValue()) + "|"
					+ grabSimpleValue("TextoAdicional") + "|"
					+ grabSimpleValue("CostobrutoSinIVA") + "|"
					+ grabSimpleValue("SupplierPartNumber", entry.getValue())
				);
				counter++;
			}
			counter = 1;

//			ID||'|'||
//			SupplierID||'|'||
//			SKU||'|'||
//			PARENT_ID||
//			'||'||
//			'D|'||
//			SAPObjectType||'|'||
//			ZNUMV||'|'||
//			TypeMainBarCode||
//			'||'||
//			'00000'||Contador||'|'||
//			NameSap||
//			'|'||IIF( Length(REPLACECHR( 0, Parsed1, '\[\]\"', NULL ))=1 , '000'||REPLACECHR( 0, Parsed1, '\[\]\"', NULL ), IIF( Length(REPLACECHR( 0, Parsed1, '\[\]\"', NULL ))=2,'00'||REPLACECHR( 0, Parsed1, '\[\]\"', NULL ),IIF( Length(REPLACECHR( 0, Parsed1, '\[\]\"', NULL ))=3,'0'||REPLACECHR( 0, Parsed1, '\[\]\"', NULL ),REPLACECHR( 0, Parsed1, '\[\]\"', NULL ))))||
//			'||'||
//			PrecioSugeridocIVA||'|'||
//			FechaInicioVigenciaPrecioVenta||'|'||
//			CostoEnMonedaExtranjera||'|'||
//			FechaInicioVigenciaCostoImportacion||'|'||
//			CostoNetoSinIVA||'|'||
//			FechaInicioVigenciaCostoNeto||'|'||
//			MainBarCode||'|'||
//			TextoAdicional||'|'||
//			CostobrutoSinIVA||'|'||
//			SupplierPartNumber
		}
	}

	private String paddZeros(int zeros, String value) {
		StringBuilder sb = new StringBuilder();
		int toPadd = value == null ? zeros : zeros - value.length();
		for(int i=0; i<toPadd; i++) {
			sb.append("0");
		}
		sb.append(value == null ? "" : value);
		return sb.toString();
	}

	private String aSeisPosiciones(int elcounter) {
		StringBuilder sb = new StringBuilder();
		int trail = 5 - String.valueOf(elcounter).length();
		for(int i=0; i<trail; i++) {
			sb.append("0");
		}
		sb.append(elcounter);
		return sb.toString();
	}

	private String getTemplateFromData(org.json.JSONObject data) {
		String template = null;
		String structureGroupId = null;
		if(data.has("structureGroupMap")) {
			org.json.JSONArray structureGroupMap = data.getJSONArray("structureGroupMap");
			for(int i=0; i<structureGroupMap.length(); i++) {
				structureGroupId = structureGroupMap.getJSONObject(i).getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId");
				if(structureGroupId.contains("PrimaryProductTaxonomy")) {
					template = structureGroupId.replaceAll("((^')|(('@'PrimaryProductTaxonomy')$))", "");
					break;
				}
			}
		}
		return template;
	}

	public String grabSimpleValue(String characteristicName, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicMap, boolean getLabel) {
		java.util.LinkedList<org.json.JSONObject> objectList = characteristicMap.get(characteristicName);
		return objectList == null || objectList.isEmpty() ? "" : grabCharacteristicValue(objectList.getFirst(), getLabel);
	}

	public String grabSimpleValue(String characteristicName, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicMap) {
		java.util.LinkedList<org.json.JSONObject> objectList = characteristicMap.get(characteristicName);
		return objectList == null || objectList.isEmpty() ? "" : grabCharacteristicValue(objectList.getFirst(), false);
	}

	public String grabSimpleValue(String characteristicName) {
		java.util.LinkedList<org.json.JSONObject> objectList = characteristicsMap.get(characteristicName);
		return objectList == null || objectList.isEmpty() ? "" : grabCharacteristicValue(objectList.getFirst(), false);
	}

	public String grabSimpleValue(String characteristicName, boolean getLabel) {
		java.util.LinkedList<org.json.JSONObject> objectList = characteristicsMap.get(characteristicName);
		return objectList == null || objectList.isEmpty() ? "" : grabCharacteristicValue(objectList.getFirst(), getLabel);
	}

	public String grabCharacteristicValue(org.json.JSONObject characteristicObject, boolean getLabel) {
		String value = null;
		Object o = characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0);
		if(o instanceof org.json.JSONObject) {
			value = ((org.json.JSONObject)o).getString(getLabel ? "_label" : "_code");
		}else if(o instanceof java.lang.String) {
			value = (String) o;
		}else if(o instanceof java.lang.Integer){
			value = String.valueOf(o);
		} else {
			System.out.println("No data type identified: " + o + ( o == null ? "null" : " " + o.getClass().getName() ));
		}
		return value;
	}

	public static void main(String[] args) throws ServiceUnavailableException {

		CrearArchivosParaSKUModifs creati = new CrearArchivosParaSKUModifs();
//		creati.process(args[0], args[1], args[2]);
		creati.creacionDeArchivos("1698767481459899");

	}

    private String writeToSftp(String proposalId, SftpClient sftp, String[] content, String remoteBasePath) throws IOException {

    	LocalDateTime now = LocalDateTime.now();
        String dateKey = now.format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        int minute = now.getMinute();
        int block = (minute / 30) * 30;
        String periodKey = dateKey ; // + "_" + now.getHour() + String.format("%02d", block);

        Properties sequenceProps = new Properties();
        int sequence = 1;

//        if (Files.exists(SEQUENCE_FILE)) {
//            try (InputStream in = Files.newInputStream(SEQUENCE_FILE)) {
//                sequenceProps.load(in);
//                String lastPeriod = sequenceProps.getProperty("period");
//                String lastSeq = sequenceProps.getProperty("seq");
//                if (periodKey.equals(lastPeriod) && lastSeq != null) {
//                    sequence = Integer.parseInt(lastSeq) + 1;
//                }
//            }
//        }


//		STEPMDYYYYMMDDNum
//		STEPATTYYYYMMDDNum
//		STEPVARYYYYMMDDNum
//		STEPUOMYYYYMMDDNum

        String fileName = null;
        String fullPath = null;
        log("Now generating files...");
        fileName = String.format("SBB".equals(content[4]) ? "STEPMD%s%04d.txt" : "STEPM1%s%04dH.txt", dateKey, sequence);
        log("First path: " + fileName);
        fullPath = remoteBasePath.endsWith("/") ? remoteBasePath + fileName : remoteBasePath + "/" + fileName;
        keepFileToLocal(proposalId, fileName, content[0].getBytes(java.nio.charset.StandardCharsets.UTF_8), content[4]);
        try (OutputStream os = sftp.write(fullPath)) {
        	log("Writing out: " + fullPath);
            os.write(content[0].getBytes(java.nio.charset.StandardCharsets.UTF_8));
            log("LOG:: WROTE.");
        }
        fileName = String.format("SBB".equals(content[4]) ? "STEPVAR%s%04d.txt" : "STEPM1%s%04dD.txt", dateKey, sequence);
        log("Second path: " + fileName);
        fullPath = remoteBasePath.endsWith("/") ? remoteBasePath + fileName : remoteBasePath + "/" + fileName;
        keepFileToLocal(proposalId, fileName, content[1].getBytes(java.nio.charset.StandardCharsets.UTF_8), content[4]);
        try (OutputStream os = sftp.write(fullPath)) {
        	os.write(content[1].getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        fileName = String.format("SBB".equals(content[4]) ? "STEPUOM%s%04d.txt" : "STEPM1%s%04dS.txt", dateKey, sequence);
        fullPath = remoteBasePath.endsWith("/") ? remoteBasePath + fileName : remoteBasePath + "/" + fileName;
        keepFileToLocal(proposalId, fileName, content[2].getBytes(java.nio.charset.StandardCharsets.UTF_8), content[4]);
        try (OutputStream os = sftp.write(fullPath)) {
        	os.write(content[2].getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        fileName = String.format("SBB".equals(content[4]) ? "STEPATT%s%04d.txt" : "STEPM1%s%04dT.txt", dateKey, sequence);
        fullPath = remoteBasePath.endsWith("/") ? remoteBasePath + fileName : remoteBasePath + "/" + fileName;
        keepFileToLocal(proposalId, fileName, content[3].getBytes(java.nio.charset.StandardCharsets.UTF_8), content[4]);
        try (OutputStream os = sftp.write(fullPath)) {
        	os.write(content[3].getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

//        try (OutputStream out = Files.newOutputStream(SEQUENCE_FILE)) {
//            sequenceProps.setProperty("date", dateKey);
//            sequenceProps.setProperty("period", periodKey);
//            sequenceProps.setProperty("seq", String.valueOf(sequence));
//            sequenceProps.store(out, null);
//        }

        return fullPath;
    }

    private void keepFileToLocal(String proposalId, String fileName, byte[] content, String business) {
//    	log("Writing to : " + ("SBB".equals(business) ? "/P360shared/IDMC/stage/SBB_SKU/" + proposalId + "_" + fileName : "/P360shared/IDMC/stage/ECC_SKU/" + proposalId + "__" + fileName));
//    	try(java.io.FileOutputStream fos = new java.io.FileOutputStream("SBB".equals(business) ? "/P360shared/IDMC/stage/SBB_SKU/" + proposalId + "_" + fileName : "/P360shared/IDMC/stage/ECC_SKU/" + proposalId + "__" + fileName)){
//    		fos.write(content);
//    	}catch(java.io.IOException e) {
//    		logE(e);
//    	}
    }

	private static final Logger LOGGER = Logger.getLogger(CrearArchivosParaSKUModifs.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/amqp//activeMQToSKUModification.log", 25 * 1024 * 1024, 10, true);
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
            LOGGER.setLevel(Level.ALL);

        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el logger", e);
        }
    }
	
	private void log(String message) {
		LOGGER.info(message);
//        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//                new java.io.FileOutputStream("../logs/activeMQToSKUModification.log", true)))) {
//            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//                    + "]  " + message);
//        } catch (java.io.IOException e) {
//        }
    }

    private static void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/activeMQToSKUModification.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }

}
