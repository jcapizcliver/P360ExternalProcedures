package mx.com.liverpool.p360.services.core;

public class StageEANBusiness {


	private static RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
		String baseDirectory = args[0];
		String eanMode = args[1];
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
		qp.put("query", "not characteristic('MainBarCode') is empty");
		qp.put("fields", "SimpleProduct2GCharacteristicValueLang.Value('MainBarCode',-1),SimpleProduct2GCharacteristicValue.LookupValue('Business',-1)->LookupValue.Code");
		qp.put("pageSize", "1200");
		final String delim = "\"";
		final String sep = ";";
		final String esc = "\\";
		if("FULL".equals(eanMode)) {
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream( java.nio.file.Paths.get(baseDirectory, "EAN_Negocio").toFile() ), java.nio.charset.Charset.forName("UTF-8")))){
				do {
					qp.put("startIndex", String.valueOf(currentIndex));
					r = workshop.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
					totalSize = r.getInt("totalSize");
					rows = r.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						currentIndex++;
						values = rows.getJSONObject(i).getJSONArray("values");
						pw.println(workshop.serializeChunk(new String[] { values.getJSONArray(0).getString(0), values.getJSONArray(1).getString(0) }, delim, sep, esc) );
					}
				}while(currentIndex < totalSize);
				currentIndex = 0;
				qp.put("fields", "SimpleArticleCharacteristicValueLang.Value('MainBarCode',-1),SimpleArticleCharacteristicValue.LookupValue('Business',-1)->LookupValue.Code");
				do {
					qp.put("startIndex", String.valueOf(currentIndex));
					r = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
					totalSize = r.getInt("totalSize");
					rows = r.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						currentIndex++;
						values = rows.getJSONObject(i).getJSONArray("values");
						pw.println(workshop.serializeChunk(new String[] { values.getJSONArray(0).getString(0), values.getJSONArray(1).getString(0) }, delim, sep, esc) );
					}
				}while(currentIndex < totalSize);
				currentIndex = 0;
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}else {
			String[] eanPieces = eanMode.split(":");
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream( java.nio.file.Paths.get(baseDirectory, "EAN_Negocio").toFile(), true ), java.nio.charset.Charset.forName("UTF-8")))){
				pw.println(workshop.serializeChunk(new String[] { eanPieces[0], eanPieces[1] }, delim, sep, esc) );
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}

	}

}
