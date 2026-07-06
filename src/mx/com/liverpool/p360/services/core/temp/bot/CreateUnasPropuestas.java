package mx.com.liverpool.p360.services.core.temp.bot;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CreateUnasPropuestas {


	public static void main(String[] args) {
		CreateUnasPropuestas cup = new CreateUnasPropuestas();
		cup.getTemplate("EU4-28201209", "Liverpool");
	}
	
	private void getTemplate(String template, String business) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("http://gcpcatqap04.liverpool.com.mx:8080/process-engine");
		qp.put("template", template);
		qp.put("business", business);
		org.json.JSONObject response = rw.makeRequest("GET", "/public/rt/GetTemplate", qp, null);
		Object o = null;
		org.json.JSONArray characteristics = null;
		org.json.JSONObject characteristic = null;
		java.util.Set<String> dataTypes = new java.util.TreeSet<>();
		java.util.Map<String, String> characteristicsWithFilter = new java.util.TreeMap<>();
		java.util.Map<String, String> characteristicsDataType = new java.util.TreeMap<>();
		if(response != null) {
			for(String name : org.json.JSONObject.getNames(response)) {
				o = response.get(name);
				if(o instanceof org.json.JSONArray) {
					characteristics = (org.json.JSONArray) o;
					for(int i=0; i<characteristics.length(); i++) {
						characteristic = characteristics.getJSONObject(i);
						if(characteristic.has("dataType")) {
							dataTypes.add(characteristic.getString("dataType"));
						} else {
							System.out.println("No data type: " + characteristic.getString("name"));
						}
					}
				}
			}
			dataTypes.forEach(System.out::println);
		}
	}
	
}
