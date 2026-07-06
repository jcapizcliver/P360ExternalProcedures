package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class MeLoAvientoEnDosMinutos {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		org.json.JSONArray rows = new org.json.JSONArray();
		String[] par = null;
		for(String elese : losesos) {
			par = elese.split("\t");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + par[1] + "'@'ZZLICLOV'")).put("values", new org.json.JSONArray().put(par[0]).put(true)));
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("rows", rows).put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).toString());
		System.out.println(response == null ? workshop.getRawResponse() : response);
	}

	private static final String[] losesos = ("DISNEY	0001\r\n"
			+ "MARVEL INC.	0002\r\n"
			+ "MINIONS	0003\r\n"
			+ "LEGO	0004\r\n"
			+ "CHELSEA FOOTBALL	0005\r\n"
			+ "THE AVENGERS	0006\r\n"
			+ "STRANGER THINGS	0007\r\n"
			+ "AMONGUS	0008\r\n"
			+ "7 PECADOS	0010\r\n"
			+ "SEIYA	0011\r\n"
			+ "SPY X FAMILY	0012\r\n"
			+ "NINTENTO	0013\r\n"
			+ "POKEMON	0014\r\n"
			+ "GOD OF WAR	0015\r\n"
			+ "DARLING IN THE FRANX	0016\r\n"
			+ "SPRITE	0017\r\n"
			+ "TANJIRO	0018\r\n"
			+ "DEMON SLAYER	0019\r\n"
			+ "QUINTILLIZAS	0020").split("\\r\\n");

}
