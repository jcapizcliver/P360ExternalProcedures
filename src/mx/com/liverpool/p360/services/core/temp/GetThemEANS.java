package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class GetThemEANS {

	private static final RESTWorkshop rw = new RESTWorkshop();

	public static void main(String[] args) throws ServiceUnavailableException {
		rw.putParameter("query", "not characteristic('MainBarCode',-1) is empty");
		rw.putParameter("fields", "Article.SupplierAID,ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)");
		rw.putParameter("pageSize", "200");
		org.json.JSONObject response = null; // rw.makeRequest("GET", "/list/Article/bySearch");
		org.json.JSONArray rows = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Set<String> eans = new java.util.TreeSet<>();

		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("C:\\opt\\LVP\\desorden\\EANS")))){
			do {
				response = rw.makeRequest("GET", "/list/Article/bySearch");
				currentIndex = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
//					System.out.println(rows.getJSONObject(i).getJSONArray("values"));
					eans.add(rows.getJSONObject(i).getJSONArray("values").getJSONArray(1).getString(0));
				}

			}while(currentIndex < totalSize);
			currentIndex = 0;
			eans.forEach(ean -> pw.println(ean) );
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

}
