package mx.com.liverpool.p360.services.core.temp.pvia;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class EnviaSKUs {

	public static void main(String[] args) {
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
			java.util.Map<String, String> pidToSKU = new java.util.HashMap<>();
			org.json.JSONArray items0 = new org.json.JSONArray();
			SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row ->  {
				if(row.length > 0) {
					if(!"Identifier".equals(row[0])) {
						if(!"".equals(row[2])) {
							pids.put(row[0]);
							pidToSKU.put(row[0], row[2]);
							if(pids.length() == 1000) {
								String response = dr.getProductData(pids);
								org.json.JSONObject jr = new org.json.JSONObject(response);
								org.json.JSONArray items = jr.getJSONArray("items");
								org.json.JSONObject item = null;
								for(int i=0; i<items.length(); i++) {
									item = items.getJSONObject(i);
									String skuRef = pidToSKU.get(item.get("product"));
									if(skuRef != null && !"".equals(skuRef)) {
										item.put("SKU", skuRef);
										items0.put(item);
									}
								}
								if(items0.length() > 0) {
									dr.putProductData(items0);
									while(items0.length() > 0) {
										items0.remove(0);
									}
								}
								pidToSKU.clear();
								while(pids.length() > 0) {
									pids.remove(0);
								}
							}
						}
					}
				}
			}) ;
			parser.parse(java.nio.file.Paths.get(args[0]));
			if(pids.length() > 0) {
				String response = dr.getProductData(pids);
				org.json.JSONObject jr = new org.json.JSONObject(response);
				org.json.JSONArray items = jr.getJSONArray("items");
				org.json.JSONObject item = null;
				for(int i=0; i<items.length(); i++) {
					item = items.getJSONObject(i);
					String skuRef = pidToSKU.get(item.get("product"));
					if(skuRef != null && !"".equals(skuRef)) {
						item.put("SKU", skuRef);
						items0.put(item);
					}
				}
				if(items0.length() > 0) {
					dr.putProductData(items0);
					while(items0.length() > 0) {
						items0.remove(0);
					}
				}
				pidToSKU.clear();
				while(pids.length() > 0) {
					pids.remove(0);
				}
			}
			java.util.Map<String, String> pidToSKU2 = new java.util.HashMap<>();
			org.json.JSONArray items00 = new org.json.JSONArray();
			org.json.JSONArray pids2 = new org.json.JSONArray();
			parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row ->  {
				if(row.length > 0) {
					if(!"Identifier".equals(row[0])) {
						if(!"".equals(row[6])) {
							pids2.put(row[0]);
							pidToSKU2.put(row[0], row[6]);
							if(pids2.length() == 1000) {
								String response = dr.getArticleData(pids2);
								org.json.JSONObject jr = new org.json.JSONObject(response);
								org.json.JSONArray items = jr.getJSONArray("items");
								org.json.JSONObject item = null;
								for(int i=0; i<items.length(); i++) {
									item = items.getJSONObject(i);
									String skuRef = pidToSKU2.get(item.get("variant"));
									if(skuRef != null && !"".equals(skuRef)) {
										item.put("SKU", skuRef);
										items00.put(item);
									}
								}
								if(items00.length() > 0) {
									dr.putArticleData(items00);
									while(items00.length() > 0) {
										items00.remove(0);
									}
								}
								pidToSKU2.clear();
								while(pids2.length() > 0) {
									pids2.remove(0);
								}
							}
						}
					}
				}
			}) ;
			parser.parse(java.nio.file.Paths.get(args[1]));
			if(pids2.length() > 0) {
				String response = dr.getArticleData(pids2);
				org.json.JSONObject jr = new org.json.JSONObject(response);
				org.json.JSONArray items = jr.getJSONArray("items");
				org.json.JSONObject item = null;
				for(int i=0; i<items.length(); i++) {
					item = items.getJSONObject(i);
					String skuRef = pidToSKU2.get(item.get("variant"));
					if(skuRef != null && !"".equals(skuRef)) {
						item.put("SKU", skuRef);
						items00.put(item);
					}
				}
				if(items00.length() > 0) {
					dr.putArticleData(items00);
					while(items00.length() > 0) {
						items00.remove(0);
					}
				}
				pidToSKU2.clear();
				while(pids2.length() > 0) {
					pids2.remove(0);
				}
			}
		}
	}
	
}
