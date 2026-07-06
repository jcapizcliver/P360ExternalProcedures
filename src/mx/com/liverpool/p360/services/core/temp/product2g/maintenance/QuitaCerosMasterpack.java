package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class QuitaCerosMasterpack {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		/*
		mPack.add("ZLAEPQ");
		mPack.add("ZBREPQ");
		mPack.add("ZHOEPQ");
		mPack.add("ZVOLPQ");
		mPack.add("ZBRGPQ");
		mPack.add("ZNTGPQ"); 
		 **/
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZLAEPQ',root,\"0000.0000.RK\",'ZLAEPQ',-1)"));
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZBREPQ',root,\"0000.0000.RK\",'ZBREPQ',-1)"));
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZHOEPQ',root,\"0000.0000.RK\",'ZHOEPQ',-1)"));
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZVOLPQ',root,\"0000.0000.RK\",'ZVOLPQ',-1)"));
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZBRGPQ',root,\"0000.0000.RK\",'ZBRGPQ',-1)"));
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZNTGPQ',root,\"0000.0000.RK\",'ZNTGPQ',-1)"));
		org.json.JSONObject request = new org.json.JSONObject();
		request.put("columns", columns);
		request.put("rows", rows);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Product2GCharacteristicValueLang.Value('ZLAEPQ',root,\"0000.0000.RK\",'ZLAEPQ',-1)"
				+ ",Product2GCharacteristicValueLang.Value('ZBREPQ',root,\"0000.0000.RK\",'ZBREPQ',-1)"
				+ ",Product2GCharacteristicValueLang.Value('ZHOEPQ',root,\"0000.0000.RK\",'ZHOEPQ',-1)"
				+ ",Product2GCharacteristicValueLang.Value('ZVOLPQ',root,\"0000.0000.RK\",'ZVOLPQ',-1)"
				+ ",Product2GCharacteristicValueLang.Value('ZBRGPQ',root,\"0000.0000.RK\",'ZBRGPQ',-1)"
				+ ",Product2GCharacteristicValueLang.Value('ZNTGPQ',root,\"0000.0000.RK\",'ZNTGPQ',-1)"
			);
		qp.put("query", 
				"characteristic('ZLAEPQ') = \"0.0\""
				+ " or characteristic('ZBREPQ') = \"0.0\""
				+ " or characteristic('ZHOEPQ') = \"0.0\""
				+ " or characteristic('ZVOLPQ') = \"0.0\""
				+ " or characteristic('ZBRGPQ') = \"0.0\""
				+ " or characteristic('ZNTGPQ') = \"0.0\""
				+ " or characteristic('ZBREPQ') = \"0\""
				+ " or characteristic('ZHOEPQ') = \"0\""
				+ " or characteristic('ZVOLPQ') = \"0\""
				+ " or characteristic('ZBRGPQ') = \"0\""
				+ " or characteristic('ZNTGPQ') = \"0\""
				+ " or characteristic('ZLAEPQ') = \"0\""
			);
		qp.put("pageSize", "25000");
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			System.out.println(values);
			for(int i=0; i<values.length(); i++) {
				try {
					float val = Float.parseFloat(values.getJSONArray(i).getString(0));
					if(val == 0f) {
						values.put(i, new org.json.JSONArray().put(""));
					}
				}catch(NumberFormatException | NullPointerException e) {
					e.printStackTrace();
				}
			}
			rows.put(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", values));
		});
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		System.out.println(request);
		rw.writeData("list", "Product2G", null, qp0, request, System.out::println);
	}
	
}
