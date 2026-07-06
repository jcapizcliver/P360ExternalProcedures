package mx.com.liverpool.p360.services.core;

public class StageLookupValues {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
		String baseDirectory = args[0];
		String lookup = args[1];
		String encoded = args.length > 2 ? args[2] : null;
		String baseUrl = args.length > 3 ? args[3] : null;
		if(encoded != null) {
			workshop.getRc().getHeader().put("Authorization", "Basic " + encoded);
		}
		if(baseUrl != null) {
			workshop.setBaseUrl(baseUrl);
		}
		org.json.JSONObject r = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		final java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", lookup);
		qp.put("query", "LookupValue.IsActive = true");
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("pageSize", "1200");
		final String delim = "\"";
		final String sep = ";";
		final String esc = "\\";
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream( java.nio.file.Paths.get(baseDirectory, "global_lookups", lookup).toFile() ), java.nio.charset.Charset.forName("UTF-8")))){
			do {
				qp.put("startIndex", String.valueOf(currentIndex));
				r = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
				totalSize = r.getInt("totalSize");
				rows = r.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					pw.println(workshop.serializeChunk(new String[] { values.getString(0), values.getString(1) }, delim, sep, esc) );
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
}
