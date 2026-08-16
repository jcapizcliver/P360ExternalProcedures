package mx.com.liverpool.p360.services.core.temp.pvia;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class RemoveEANsThatAreInProductAndArticle {

	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		try(DBAccessDataStub dastub = new DBAccessDataStub( new ELog() {
			
			@Override
			public void logE(Exception e) {
			}
			
			@Override
			public void log(String message) {
			}
		} )){
			DataRequestor dr = new DataRequestor(dastub);
			org.json.JSONArray pids = new org.json.JSONArray();
			int[] hits = new int[] {0};
			java.util.Map<String, String> eansQuitados = new java.util.HashMap<>();
			SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser('"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
				if(row.length > 0) {
					if(!"".equals(row[0])) {
						pids.put(row[0]);
						if(pids.length() == 10000) {
							String r = dr.getProductData(pids);
							org.json.JSONObject jr = new org.json.JSONObject(r);
							org.json.JSONArray items = jr.getJSONArray("items");
							org.json.JSONObject item = null;
							for(int i=0; i<items.length(); i++) {
								item = items.getJSONObject(i);
								String mainBarCode = item.getString("MainBarCode");
								String mainBarCodeS4H = item.getString("MainBarCodeS4H");
								if(!"".equals(mainBarCode) || !"".equals(mainBarCodeS4H)) {
									java.util.Set<String> variants = dr.getVariants(item.getString("product"));
									org.json.JSONArray jv = new org.json.JSONArray();
									for(String v : variants) {
										jv.put(v);
									}
									String r0 = dr.getArticleData(jv);
									org.json.JSONObject jr0 = new org.json.JSONObject(r0);
									org.json.JSONArray items0 = jr0.getJSONArray("items");
									org.json.JSONObject item0 = null;
									for(int j=0; j<items0.length(); j++) {
										item0 = items0.getJSONObject(j);
										String artMainBarCode = item0.getString("MainBarCode");
										String artMainBarCodeS4H = item0.getString("MainBarCodeS4H");
										if(!"".equals(artMainBarCode)) {
											if(artMainBarCode.equals(mainBarCode) || artMainBarCode.equals(mainBarCodeS4H)) {
												dr.retiraEANProductNo( new org.json.JSONArray().put( artMainBarCode ));
												eansQuitados.put(artMainBarCode, item0.getString("variant"));
												hits[0]++;
											}
										}
										if(!"".equals(artMainBarCodeS4H)) {
											if(artMainBarCodeS4H.equals(mainBarCode) || artMainBarCodeS4H.equals(mainBarCodeS4H)) {
												dr.retiraEANProductNo( new org.json.JSONArray().put( artMainBarCodeS4H ));
												eansQuitados.put(artMainBarCodeS4H, item0.getString("variant"));
												hits[0]++;
											}
										}
									}
								}
							}
							while(pids.length() > 0) {
								pids.remove(0);
							}
						}
					}
				}
			});
			parser.parse(java.nio.file.Paths.get(args[0]));
			if(pids.length() > 0) {
	
				String r = dr.getProductData(pids);
				org.json.JSONObject jr = new org.json.JSONObject(r);
				org.json.JSONArray items = jr.getJSONArray("items");
				org.json.JSONObject item = null;
				for(int i=0; i<items.length(); i++) {
					item = items.getJSONObject(i);
					String mainBarCode = item.getString("MainBarCode");
					String mainBarCodeS4H = item.getString("MainBarCodeS4H");
					if(!"".equals(mainBarCode) || !"".equals(mainBarCodeS4H)) {
						java.util.Set<String> variants = dr.getVariants(item.getString("product"));
						org.json.JSONArray jv = new org.json.JSONArray();
						for(String v : variants) {
							jv.put(v);
						}
						String r0 = dr.getArticleData(jv);
						org.json.JSONObject jr0 = new org.json.JSONObject(r0);
						org.json.JSONArray items0 = jr0.getJSONArray("items");
						org.json.JSONObject item0 = null;
						for(int j=0; j<items0.length(); j++) {
							item0 = items0.getJSONObject(j);
							String artMainBarCode = item0.getString("MainBarCode");
							String artMainBarCodeS4H = item0.getString("MainBarCodeS4H");
							if(!"".equals(artMainBarCode)) {
								if(artMainBarCode.equals(mainBarCode) || artMainBarCode.equals(mainBarCodeS4H)) {
									dr.retiraEANProductNo( new org.json.JSONArray().put( artMainBarCode ));
									eansQuitados.put(artMainBarCode, item0.getString("variant"));
									hits[0]++;
								}
							}
							if(!"".equals(artMainBarCodeS4H)) {
								if(artMainBarCodeS4H.equals(mainBarCode) || artMainBarCodeS4H.equals(mainBarCodeS4H)) {
									dr.retiraEANProductNo( new org.json.JSONArray().put( artMainBarCodeS4H ));
									eansQuitados.put(artMainBarCodeS4H, item0.getString("variant"));
									hits[0]++;
								}
							}
						}
					}
				}
				while(pids.length() > 0) {
					pids.remove(0);
				}
			
			}
			System.out.println("Hits: " + hits[0]);
			eansQuitados.entrySet().forEach(System.out::println);
			System.out.println("Done. " + new RESTWorkshop().formatTime(System.currentTimeMillis() - init));
		}
	}
	
}
