package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.temp.product2g.maintenance4.ProductNameResolver;
import mx.com.liverpool.p360.services.core.temp.product2g.maintenance4.ProductNameResolver.ResolvedName;

public class CalcOrderOfAttributesForName {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		for(String id : ids) {
			calcProductName(id);
		}
		rh.sendData();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "dropped").toFile())))){
			droped.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "ok").toFile())))){
			ok.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "noOrderOfAttributesForName").toFile())))){
			noOrderOfAttributesForName.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "badOrderOfAttributesForName").toFile())))){
			badOrderOfAttributesForName.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void log(String message) {
		System.out.println(message);
	}
	
	private static final java.util.Set<String> noOrderOfAttributesForName = new java.util.TreeSet<>();
	private static final java.util.Set<String> badOrderOfAttributesForName = new java.util.TreeSet<>();
	private static final java.util.List<String> ok = new java.util.ArrayList<>();
	private static final java.util.List<String> droped = new java.util.ArrayList<>();
	
	private static final RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GLang.ProductName(es)")), 1000, request -> rw.writeData("list", "Product2G", null, new java.util.HashMap<>(), request, System.out::println) );
	
	private static void calcProductName(String proposalId) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("entityFilter", "Product2GCharacteristicValue,Product2GStructureMap");
		qp.put("includeLabels", "true");
		qp.put("includeIds", "true");
		org.json.JSONObject objectResponse = rw.getRw().makeRequest("GET", "/object/Product2G/'" + proposalId + "'@1", qp, null);
		log("Got prop ID " + proposalId);
		if(objectResponse != null && objectResponse.has("_data") && objectResponse.getJSONObject("_data").has("_characteristicRecords")) {
			log("got characteristic records");
			org.json.JSONArray cr = objectResponse.getJSONObject("_data").getJSONArray("_characteristicRecords");
			org.json.JSONObject json = null;
			java.util.Map<String, org.json.JSONObject> characteristicsMap = new java.util.TreeMap<>();
			String template = null;
			String templateName = null;
			String orderOfAttributesForName = null;
			for(int i=0; i<cr.length(); i++) {
				json = cr.getJSONObject(i);
				characteristicsMap.put(json.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"), json);
			}
			String[] td = new String[3];
			td[0] = null;
			td[1] = null;
			td[2] = null;
			java.util.Map<String, String> qp00 = new java.util.TreeMap<>();
			qp00.put("fields", "Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroup.Identifier,Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroupLang.Name(es),Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroupAttributeValue.Value(\"OrderOfAtributesForName\",es,DEFAULT)");
			qp00.put("items", "'" + proposalId + "'@1");
			log("gonna request product structure data...");
			rw.collectData("list", "Product2G", null, "byItems", qp00, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				log("Got this data: " + values);
				org.json.JSONArray sgc = values.getJSONArray(0);
				org.json.JSONArray sgn = values.getJSONArray(1);
				td[0] = sgc.getString(0);
				td[1] = sgn.getString(0);
				td[2] = values.getJSONArray(2).getString(0);
			});
			if(td[0] != null) {
				template = td[0];
				templateName = td[1];
				orderOfAttributesForName = td[2];
			}else {
//				No Product found
//				noOrderOfAttributesForName.add(templateName);
			}
			log("Got template: " + template);
			log("Got template name: " + templateName);
			log("Order of attributes for name: " + orderOfAttributesForName);
			log("Got these:");
			log("/gt.");
			if(orderOfAttributesForName != null && !"".equals(orderOfAttributesForName) /* && (prevPN == null || prevPN.isEmpty()) */ ) {
				if(orderOfAttributesForName.contains(" + ")) {
					badOrderOfAttributesForName.add(template);
					droped.add(proposalId);
				}else {
					log("Came here to loop");
					String[] elements = orderOfAttributesForName.split(",");
					StringBuilder sb = new StringBuilder();
					for(String element : elements) {
						if(!element.contains("\"")) {
							sb
							.append(sb.length() == 0 ? "" : ", ")
							.append(getCharacteristicValue( characteristicsMap.get(element) ));
						}else {
							sb
							.append(sb.length() == 0 ? "" : ", ")
							.append(element.replaceAll("\"", ""));
						}
					}
					String productName = sb.toString().replaceAll(", +?,", ",").replaceAll(",(?! )", ", ").replaceAll(" ,", ",").replaceAll(",", "").replaceAll(" {2,}", " ").trim();
					ResolvedName rn = ProductNameResolver.resolve(productName, "");
//					productName = rn.value();
					
					log("PN: " + productName);
					if(!"".equals(productName)) {
						rh.addRow(new org.json.JSONObject().put("object", "'" + proposalId + "'@1").put("values", new org.json.JSONArray().put(productName)));
						ok.add(proposalId);
					}else {
						droped.add(proposalId);
					}
				}
//				org.json.JSONObject pno = createCharacteristicValueObject("ProductName", prevProductName == null || prevProductName.isEmpty() ? productName : prevProductName);
//				org.json.JSONObject no  = createCharacteristicValueObject("Name", prevPN == null || prevPN.isEmpty() ? productName : prevPN);
//				records.put(pno);
//				records.put(no);
//				sourceData.put("ProductName", pno);
//				sourceData.put("Name", no);
			}else {
				if(template == null || "".equals(templateName)) {
					// PANIC, not even template found
				}else {
					noOrderOfAttributesForName.add(template);
				}
				droped.add(proposalId);
			}
//			org.json.JSONObject data = null;
//			if(prevPN != null && !"".equals(prevPN)) {
//				data = characteristicsMap.get("ProductName");
//				if(data != null) {
//					sourceData.put("ProductName", data);
//				}else { log("No ProductName"); }
//				data = characteristicsMap.get("Name");
//				if(data != null){
//					sourceData.put("Name", characteristicsMap.get("Name"));
//				}else { log("No name..."); }
//			}
		} else { droped.add(proposalId); log("Just no u.u"); }
	}
	
	private static String getCharacteristicValue(org.json.JSONObject characteristic) {
		return getCharacteristicValue(characteristic, false);
	}

	private static org.json.JSONObject createCharacteristicValueObject(String characteristicName, Object value){
		return new org.json.JSONObject().put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(value)).put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx"))))).put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", characteristicName)));
	}

	private static String getCharacteristicValue(org.json.JSONObject characteristic, boolean getCode) {
		if(characteristic == null) {
			return "";
		}
		String dataType = characteristic.has("_datatype") ? characteristic.getString("_datatype") : "";
		if("LOOKUP".equals(dataType)) {
			try{
				return characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).has(getCode ? "_code" : "_label") ? characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString(getCode ? "_code" : "_label") : "";
			}catch(org.json.JSONException e){
				throw e;
			}
		}else {
			return String.valueOf( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0) );
		}
	}
	
	private static final java.util.List<String> ids = java.util.Arrays.asList((
//			"1754611680119426\r\n"
//			+ "1754611680119420\r\n"
//			+ "1754611680119414\r\n"
//			+ "1754611680119408\r\n"
//			+ "1754611680119396\r\n"
//			+ "1754611680119390\r\n"
//			+ "1754611680119384\r\n"
//			+ "1754611680119378\r\n"
//			+ "1754611680119372\r\n"
//			+ "1754611680119366\r\n"
//			+ "1754611680119360\r\n"
//			+ "1754611680119354\r\n"
//			+ "1754611680119348\r\n"
//			+ "1754611680119341\r\n"
//			+ "1754611680119336\r\n"
//			+ "1754611680119327\r\n"
//			+ "1754611680119324\r\n"
//			+ "1754611680119315\r\n"
//			+ "1754611680119312\r\n"
//			+ "1754611680119303\r\n"
//			+ "1754611680119300\r\n"
//			+ "1754611680119294\r\n"
//			+ "1754611680119288\r\n"
//			+ "1754611680119282\r\n"
//			+ "1754611680119276\r\n"
//			+ "1754611680119270\r\n"
//			+ "1754611680119264\r\n"
//			+ "1754611680119258\r\n"
//			+ "1754611680119252\r\n"
//			+ "1754611680119246\r\n"
//			+ "1754611680119240\r\n"
//			+ "1754611680119231\r\n"
//			+ "1754611680119228\r\n"
//			+ "1754611680119219\r\n"
//			+ "1754611680119216\r\n"
//			+ "1754611680119207\r\n"
//			+ "1754611680119204\r\n"
//			+ "1754611680119195\r\n"
//			+ "1754611680119192\r\n"
//			+ "1754611680119183\r\n"
//			+ "1754611680119180\r\n"
//			+ "1754611680119171\r\n"
//			+ "1754611680119168\r\n"
//			+ "1754611680119157\r\n"
//			+ "1754611680119156\r\n"
//			+ "1754611680063257\r\n"
//			+ "1754611680060618\r\n"
//			+ "1754611680060612\r\n"
//			+ "1754611680060606\r\n"
//			+ "1754611680060600\r\n"
//			+ "1754611680060594\r\n"
//			+ "1754611680060588\r\n"
//			+ "1754611680060582\r\n"
//			+ "1754611680060572\r\n"
//			+ "1754611680060558\r\n"
//			+ "1754611680060552\r\n"
//			+ "1754611680060541\r\n"
//			+ "1754611680060535\r\n"
//			+ "1754611680060526\r\n"
//			+ "1754611680060523\r\n"
//			+ "1754611680060516\r\n"
//			+ "1754611680060511\r\n"
//			+ "1754611680060502\r\n"
//			+ "1754611680060499\r\n"
//			+ "1754611680060490\r\n"
//			+ "1754611680060487\r\n"
//			+ "1754611680060478\r\n"
//			+ "1754611680060475\r\n"
//			+ "1754611680060466\r\n"
//			+ "1754611680060463\r\n"
//			+ "1754611680060457\r\n"
//			+ "1754611680060447\r\n"
//			+ "1754611680060441\r\n"
//			+ "1754611680060435\r\n"
//			+ "1754611680060429\r\n"
//			+ "1754611680060423"
			"1754611681162285\r\n"
			+ "1754611681162288\r\n"
			+ "1754611681162351\r\n"
			+ "1754611681162354\r\n"
			+ "1754611681162414\r\n"
			+ "1754611681162423\r\n"
			+ "1754611681162477\r\n"
			+ "1754611681162480\r\n"
			+ "1754611681162537\r\n"
			+ "1754611681162540\r\n"
			+ "1754611681162597\r\n"
			+ "1754611681162603\r\n"
			+ "1754611681162657\r\n"
			+ "1754611681162660\r\n"
			+ "1754611681162717\r\n"
			+ "1754611681162747\r\n"
			+ "1754611681162777\r\n"
			+ "1754611681162804\r\n"
			+ "1754611681162831\r\n"
			+ "1754611681162858\r\n"
			+ "1754611681162885\r\n"
			+ "1754611681162912\r\n"
			+ "1754611681162939\r\n"
			+ "1754611681162966\r\n"
			+ "1754611681162993\r\n"
			+ "1754611681163021\r\n"
			+ "1754611681163048\r\n"
			+ "1754611681163075\r\n"
			+ "1754611681163102\r\n"
			+ "1754611681163135\r\n"
			+ "1754611681163165\r\n"
			+ "1754611681163195\r\n"
			+ "1754611681163225\r\n"
			+ "1754611681163255\r\n"
			+ "1754611681163285\r\n"
			+ "1754611681163291\r\n"
			+ "1754611681163345\r\n"
			+ "1754611681163357\r\n"
			+ "1754611681163402\r\n"
			+ "1754611681163407\r\n"
			+ "1754611681163456\r\n"
			+ "1754611681163459\r\n"
			+ "1754611681163510\r\n"
			+ "1754611681163516"
).split("\\r\\n"));
}
