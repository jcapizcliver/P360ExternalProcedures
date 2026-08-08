package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ArticleByEANToUpdateColor {

	private static int count = 0;
	
	public static void main(String[] args) {
		try(DBAccessDataStub dastub = new DBAccessDataStub(new ELog() {
			
			@Override
			public void logE(Exception e) {
				e.printStackTrace();
			}
			
			@Override
			public void log(String message) {
				System.out.println(message);
			}
		})){
			java.util.List<String> ids = new java.util.ArrayList<>();
			java.util.Map<String, String> eanColor = new java.util.HashMap<>();
			SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '`', ',', null, "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
				count++;
				if(row.length == 0) {
					return;
				}
				if(count == 1) {
					return;
				}
				if("ColoursLiverpoolAtt".equals(row[1])) {
					String aid = dastub.getEanSupplierAid(row[0]);
					if(aid != null && !"".equals(aid)) {
						ids.add(aid);
						eanColor.put(row[0], row[2]);
					}else {
						System.out.println("Cosh: " + row[0]);
					}
				}
			} );
			parser.parse(java.nio.file.Paths.get(args[0]));
			java.util.Map<String, org.json.JSONObject> all = new java.util.HashMap<>();
			java.util.Map<String, org.json.JSONObject> data = null;
			java.util.List<String> tmp = new java.util.ArrayList<>();
			int cnt = 0;
			for(String id : ids) {
				cnt++;
				tmp.add(id);
				if(cnt % 1000 == 0) {
					data = dastub.getArticleData(tmp);
					tmp.clear();
					if(data != null) {
						all.putAll(data);
					}
				}
			}
			if(!tmp.isEmpty()) {
				data = dastub.getArticleData(tmp);
				if(data != null) {
					all.putAll(data);
				}
			}
			System.out.println("Data: " + all.size());
			RESTWrapper rw = new RESTWrapper();
			java.util.Map<String, String> qp = new java.util.HashMap<>();
			qp.put("includeObjectsInProtocol", "false");
			RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleExtraData.ColoursLiverpoolAtt(MX)")), 1000, request -> rw.writeData("list", "Article", null, qp, request, System.out::println) );
			String color = null;
			int concolor = 0;
			int sincolor = 0;
			java.util.Map<String, String> colorMap = procedeACargarValoresValidos(dastub);
			System.out.println("Got color map.");
			String colorCode = null;
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("Participantes").toFile())))){
				pw.println(rw.getRw().serializeChunk( new String[] { "ID", "Color" } ));
				for(java.util.Map.Entry<String, org.json.JSONObject> entry : all.entrySet()) {
					if("".equals(entry.getValue().getString("ColoursLiverpoolAtt"))) {
						color = eanColor.get(entry.getValue().getString("MainBarCode"));
						sincolor++;
						if(color != null) {
							colorCode = colorMap.get(color);
							if(colorCode != null) {
								pw.println(rw.getRw().serializeChunk( new String[] { entry.getKey(), color, colorCode } ));
								rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@1")).put("values", new org.json.JSONArray().put(colorCode)));
							}else {
								System.out.println("No color code for: " + color);
							}
						}else {
							System.out.println("No color for EAN: " + entry.getValue().getString("MainBarCode") + " (" + entry.getKey() + ")");
						}
					}else {
						concolor++;
					}
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
			System.out.println("Con color: " + concolor);
			System.out.println("Sin color: " + sincolor);
			rh.sendData();
		}
	}
	
	private static java.util.Map<String, String> procedeACargarValoresValidos(DBAccessDataStub dastub) {
		java.util.Map<String, String> validValues = new java.util.HashMap<>();
		org.json.JSONObject characteristicData = dastub.getCharacteristicData("ColoursLiverpoolAtt");
		String lookup = characteristicData.optString("lookup","");
		java.util.List<org.json.JSONObject> lookupRows = dastub.getLookupValueCodeNameExternalCodeRows(lookup, 10, "ATG", true);
		for (org.json.JSONObject lookupRow : lookupRows) {
			String code = lookupRow.optString("code", "");
			String name = lookupRow.optString("name", "");
			if(name != null && !"".equals(name)) {
				validValues.put(name, code);
			}
		}
		return validValues;
	}
}
