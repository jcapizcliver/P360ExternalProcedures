package mx.com.liverpool.p360.services.core.temp.dataloader;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CargaAcotacionesECCPorItemGroup {


	private static final RESTWorkshop workshop = new RESTWorkshop(true, PropertiesManager.get("p360.contingency.base_url"), "Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));

	public static void main(String[] args) {

		org.json.JSONObject response = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject responseJ = null;

		java.util.LinkedList<String[]> vals = new java.util.LinkedList<>();

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\tmp\\Datos SAP QA - ECC - Relación grupos caracteristicas.tsv")))){
			String line = null;
			String[] pieces = null;
			String sep = "\t";
			String delim = "";
			String esc = "\\";
			br.readLine();
			int cnt = 0;
			while((line = br.readLine()) != null) {
				cnt++;
				pieces = workshop.parseLine(line, delim, sep, esc);
				vals.addLast(new String[] {pieces[4], pieces[7], pieces[9], pieces[10]});
//				if(cnt <= 3) {
//					continue;
//				}
//				if(!"FIBER_PARTLOV".equals(pieces[0]))
//					continue;
//				if("".equals(pieces[1]) && "".equals(pieces[2])) {
//					System.out.println(java.util.Arrays.asList(pieces));
//					continue;
//				}
//				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + ("".equals(pieces[1]) ? pieces[2] : pieces[1]) + "'@'" + pieces[0] +"'" )).put("values", new org.json.JSONArray().put(pieces[2]).put(true)));
//				if(rows.length() == 50) {
//					responseJ = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject()
//							.put("columns",
//									new org.json.JSONArray()
//										.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))
//										.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))
//							)
//							.put("rows", rows).toString());
//					System.out.println(responseJ == null ? workshop.getRawResponse() : responseJ);
//					while(rows.length() > 0) {
//						rows.remove(0);
//					}
//				}
			}
//			if(rows.length() > 0) {
//				responseJ = workshop.makeRequest("POST", "/list/Characteristic", qp, new org.json.JSONObject()
//						.put("columns",
//								new org.json.JSONArray()
//									.put(new org.json.JSONObject().put("LookupValue", "LookupValueLang.Name(es)"))
//									.put(new org.json.JSONObject().put("LookupValue", "LookupValue.IsActive"))
//						)
//						.put("rows", rows).toString());
//				System.out.println(responseJ == null ? workshop.getRawResponse() : responseJ);
//			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

		java.util.Collections.sort(vals, (o1,o2)->{
			int cmp = o1[0].compareTo(o2[0]);
			if(cmp == 0) {
				cmp = o1[1].compareTo(o2[1]);
			}
			return cmp;
			}
		);
		vals.forEach(arr -> System.out.println(java.util.Arrays.asList(arr)));
//		System.exit(0);
		org.json.JSONObject postResponse = null;
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		org.json.JSONArray losValores = new org.json.JSONArray();
		String currItemGroup = null;
		String prevItemGroup = null;
		String currAttribute = null;
		String prevAttribute = null;
		java.util.Map<String, String> emptyQp = new java.util.TreeMap<>();
		for(String[] curr : vals) {
			currItemGroup = curr[0];
			currAttribute = curr[1];
			if( (prevItemGroup != null && !prevItemGroup.equals(currItemGroup)) || (prevAttribute != null && !prevAttribute.equals(currAttribute)) ) {
				rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + prevItemGroup + "'@'MATKLLOV'")).put("values", new org.json.JSONArray().put(losValores)));
				postResponse = workshop.makeRequest("POST", "/list/LookupValue", emptyQp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('" + prevAttribute + "LOV')"))).put("rows", rowsPayload).toString());
				if(postResponse == null) {
					System.out.println(workshop.getRawResponse() + "\n\t" + rowsPayload);
				}else {
					System.out.println("Pushed. " + postResponse.getJSONObject("counters"));
				}
				rowsPayload = new org.json.JSONArray();
				losValores = new org.json.JSONArray();
			}
			losValores.put(curr[2]);
			prevItemGroup = currItemGroup;
			prevAttribute = currAttribute;
		}
		rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + prevItemGroup + "'@'MATKLLOV'")).put("values", new org.json.JSONArray().put(losValores)));
		postResponse = workshop.makeRequest("POST", "/list/LookupValue", emptyQp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('" + prevAttribute + "LOV')"))).put("rows", rowsPayload).toString());
		if(postResponse == null) {
			System.out.println(workshop.getRawResponse() + "\n\t" + rowsPayload);
		}else {
			System.out.println("Pushed. " + postResponse.getJSONObject("counters"));
		}
		rowsPayload = new org.json.JSONArray();
		losValores = new org.json.JSONArray();

	}



}
