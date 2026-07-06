package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class AgregaNameYProductName extends RESTWrapper {

	
	public static void main(String[] args) {
		java.util.Map<String, String[]> templateMetaData = new java.util.TreeMap<>();
		AgregaNameYProductName a = new AgregaNameYProductName();
		a.loadTemplateMetaData(templateMetaData);
		a.run(templateMetaData);
	}
	
	private void loadTemplateMetaData(java.util.Map<String, String[]> templateMetaData) {
		System.out.println(PropertiesManager.get("p360.contingency.create_proposal.structure_group_attribute_name_guide"));
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream( 
				PropertiesManager.get("p360.contingency.create_proposal.structure_group_attribute_name_guide") )))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = getRw().parseLine(line);
				templateMetaData.put(pieces[0], new String[] {pieces[1], pieces[2], pieces[3]});
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private void run(java.util.Map<String, String[]> templateMetaData) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GStructureMap.StructureGroup('PrimaryProductTaxonomy')->StructureGroup.Identifier"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup',-1)->LookupValueLang.Name(es)"
			);
		qp.put("query", "Product2G.ProductNo = \"1754611649400251\" and Product2G.ProductNo wildcard \"175461%\" and (characteristic('Name',-1) is empty or characteristic('ProductName',-1) is empty)");
		qp.put("pageSize", "25000");
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeLabels", "true");
		qp0.put("includeIds", "true");
		collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String productNo = values.getString(0);
			String template = values.getJSONArray(1).getString(0);
			System.out.println("A template: " + template);
			String[] templateMD = templateMetaData.get(template);
			String orderOfAttributesForName = templateMD[1];
			String productName = null;
			String itemGroup = values.getJSONArray(2).getString(0);
			org.json.JSONObject resp = getRw().makeRequest("GET", "/object/Product2G/'" + productNo + "'@1", qp0, null);
			System.out.println( getRw().getRawResponse() );
			org.json.JSONObject json = null;
			org.json.JSONArray characteristicRecords = resp.getJSONObject("_data").getJSONArray("_characteristicRecords");
			String characteristicIdentifier = null;
			java.util.Map<String, org.json.JSONObject> characteristicsMap = new java.util.TreeMap<>();
			for(int i=0; i<characteristicRecords.length(); i++) {
				json = characteristicRecords.getJSONObject(i);
				characteristicIdentifier = json.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
				characteristicsMap.put(characteristicIdentifier, json);
			}
			org.json.JSONArray newCharacteristicRecords = new org.json.JSONArray();
			if( orderOfAttributesForName != null ) {
				System.out.println("OoAfN: " + orderOfAttributesForName);
				String[] elements = orderOfAttributesForName.split(",");
				StringBuilder sb = new StringBuilder();
				for(String element : elements) {
					if("ProductTypeSAP".equals(element)) {
						sb.append(sb.length() == 0 ? "" : ", ").append(itemGroup.replaceAll("^\\d+ - ", ""));
					}else
						if(!element.contains("\"")) {
							sb
							.append(sb.length() == 0 ? "" : ", ")
							.append(getCharacteristicValue( characteristicsMap.get(element) ))
							;
						}else {
							sb
							.append(sb.length() == 0 ? "" : ", ")
							.append(element.replaceAll("\"", ""));
						}
				}
				productName = sb.toString().replaceAll(", +?,", ",").replaceAll(",(?! )", ", ").replaceAll(" ,", ",").replaceAll(",", "").replaceAll(" {2,}", " ").trim();
				System.out.println("El nm --->" + productName + "<---");
//				productName = "";
				newCharacteristicRecords.put(
						createCharacteristicValueObject("ProductName", productName)
					);
				newCharacteristicRecords.put(
						createCharacteristicValueObject("Name", productName)
					);
				
				writeData("PUT", "object", "Product2G", "'" + productNo + "'@1", qp0, new org.json.JSONObject()
						.put("_characteristicRecords", newCharacteristicRecords), System.out::println);
			}else {
				System.out.println("No order of attributes for name. " + productNo);
			}
		}, System.out::println);
	}

	private org.json.JSONObject createCharacteristicValueObject(String characteristicName, Object value){
		return new org.json.JSONObject().put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(value)).put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx"))))).put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", characteristicName)));
	}

	private String getCharacteristicValue(org.json.JSONObject characteristic, boolean getCode) {
		if(characteristic == null) {
			return "";
		}
		String dataType = characteristic.has("_datatype") ? characteristic.getString("_datatype") : "";
		if("LOOKUP".equals(dataType)) {
			try{
				return characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).has(getCode ? "_code" : "_label") ? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString(getCode ? "_code" : "_label") : "";
			}catch(org.json.JSONException e){
				System.out.println("--->" + characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0));
				throw e;
			}
		}else {
			return String.valueOf( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0) );
		}
	}

	private String getCharacteristicValue(org.json.JSONObject characteristic) {
		return getCharacteristicValue(characteristic, false);
	}
	
}
