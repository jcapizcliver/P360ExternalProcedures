package mx.com.liverpool.p360.services.core.temp.pvia;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.net.CliTest;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class MandaLosQueNoTienes {

	
	public static void main(String[] args) {
		java.util.List<String> hola = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile())))){
			String line = null;
			while((line = br.readLine()) != null){
				hola.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		org.json.JSONObject jr;
		org.json.JSONArray items;
		org.json.JSONObject item;
		try(DBAccessDataStub dastub = new DBAccessDataStub( new ELog() {
			
			@Override
			public void logE(Exception e) {
			}
			
			@Override
			public void log(String message) {
			}
		} )){
			DataRequestor dr = new DataRequestor(dastub);
			String s = null;
			boolean isP = "product".equals(args[1]);
			for(String a : hola){
				s = dr.getProductData(new org.json.JSONArray().put(a));
				jr = new org.json.JSONObject(s);
				items = jr.getJSONArray("items");
				item = items.getJSONObject(0);
				if("".equals(item.getString("CurrentStatus"))) {
					CliTest.main(new String[] { isP ? "putProductData" : "putArticleData", a });
				}
			}
		}
	}
}
