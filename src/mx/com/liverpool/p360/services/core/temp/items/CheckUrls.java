package mx.com.liverpool.p360.services.core.temp.items;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CheckUrls {

	public static void main(String[] args) {
		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)"
				+ ",ArticleCharacteristicValueLang.Value('ProductImageDetail',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImageDetail_URL',-1)"
				+ ",ArticleCharacteristicValueLang.Value('Illustration',\"0000.0000.RK\",\"0000.0000.RK\",'Illustration_URL',-1)"
				+ ",ArticleCharacteristicValueLang.Value('ProductImageSmosh',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImageSmosh_URL',-1)"
			);
		qp.put("pageSize", "1200");
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray currentBud = null;
		java.util.Set<String> a = new java.util.TreeSet<>();
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Article/byCatalog", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					for(int j=0; j<4; j++) {
						currentBud = values.getJSONArray(j);
						for(int k=0; k<currentBud.length(); k++) {
							if(!"".equals(currentBud.getString(k)) && currentBud.getString(k).matches(".+(\\.[a-z]+)$")) {
								a.add(currentBud.getString(k));
								System.out.println(currentBud.getString(k));
							}
						}
					}
				}
				currentIndex += response.getInt("pageSize");
			}else {
				
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;
//		a.forEach(System.out::println);
	}
}
