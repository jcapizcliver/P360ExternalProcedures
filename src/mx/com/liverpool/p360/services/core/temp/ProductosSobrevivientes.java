package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class ProductosSobrevivientes {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		qp.put("fields", "Product2G.ProductNo");
		qp.put("pageSize", "900");

		int totalSize = 0;
		int currentIndex = 0;
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\proposalIds.csv")))){
			do{
				response = workshop.makeRequest("GET", "/list/Product2G/byCatalog", qp, null);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					pw.println(values.getString(0));
				}
			}while(currentIndex < totalSize);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

	}

}
