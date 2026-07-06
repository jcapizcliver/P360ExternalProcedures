package mx.com.liverpool.p360.services.core.temp.dataloader;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CargaTallasInfantilesVsMarcas {

	public static void main(String[] args) {
		String[] pieces = null;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));
		for(String ln : content) {
			pieces = ln.split("\t");
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@'TallasInfantilesVsMarca'")).put("values", new org.json.JSONArray().put(pieces[1])));
		}
		org.json.JSONObject response = null;
		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		response = rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
		System.out.println(response == null ? rw.getRawResponse() : response);
	}
	
	private static final java.util.ArrayList<String> content = new java.util.ArrayList<>(
				java.util.Arrays.asList(
						("2011150630003	Bebe\r\n"
								+ "2011150630007	Adulto\r\n"
								+ "2450302440005	Adolecente\r\n"
								+ "40401C06088	XCH\r\n"
								+ "40401C06089	CH\r\n"
								+ "40401C06090	M\r\n"
								+ "40401C06091	G\r\n"
								+ "40401C06092	XG\r\n"
								+ "4210800660020	Medio año\r\n"
								+ "4210800660095	12 meses\r\n"
								+ "4210800660096	2 años\r\n"
								+ "4210800660098	Antes del año\r\n"
								+ "4210801560096	2 meses\r\n"
								+ "4210860740020	6 meses\r\n"
								+ "4210860740096	18 meses\r\n"
								+ "4210860740098	3 meses\r\n"
								+ "4430400890088	6-7 años\r\n"
								+ "4430400890089	8-9 años\r\n"
								+ "4430400890090	10-11 años\r\n"
								+ "4430400890091	12-14 años\r\n"
								+ "4430400890092	14-16 años\r\n"
								+ "5400100030003	Chiquita\r\n"
								+ "5450326330178	Extra Chica Prueba\r\n"
								+ "54503B6200002	Extra Chica\r\n"
								+ "5450800650006	Prueba 2a transformacion\r\n"
								+ "SB02001S4450001	2a tranformacion\r\n"
								+ "SB02001S4450160	Medio\r\n"
								+ "SB8320602270160	Prueba 2a transformacionSBB\r\n"
								+ "SB85504S0360050	CH").split("\\r\\n")
				)
			);
}
