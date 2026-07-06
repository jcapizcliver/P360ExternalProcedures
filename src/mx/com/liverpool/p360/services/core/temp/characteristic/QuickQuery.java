package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class QuickQuery {

	private static final RESTWrapper rw = new RESTWrapper();

	public static void main(String[] args) {
		rw.getRw().setBaseUrl("https://172.18.251.2:1512/rest/V2.0");
		rw.getRw().getRc().getHeader().put("Authorization",
				"Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Characteristic.Identifier,Characteristic.DataType");
		qp.put("query",
				"Characteristic.Category->LookupValue.Code = \"Master Data\" and Characteristic.IsActive = true and Characteristic.Entities contains \"Article\"");
		qp.put("pageSize", "1000");
//		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> System.out.println( ".put(new org.json.JSONObject().put(\"identifier\", \"ArticleCharacteristicValueLang.Value('" + row.getJSONArray("values").getString(0) + "',root,\\\"0000.0000.RK\\\",'" + row.getJSONArray("values").getString(0) + "',-1)\"))" ));
//		rw.collectData("list", "Characteristic", null, "bySearch", qp,
//				row -> System.out.println(
//							"Value " + toLowerCamelCase(row.getJSONArray("values").getString(0)) + " = valMap.get(\"" + row.getJSONArray("values").getString(0) + "\");"
//						));
//		rw.collectData("list", "Characteristic", null, "bySearch", qp,
//				row -> {
//					String var = toLowerCamelCase(row.getJSONArray("values").getString(0));
//					System.out.println(
//							"String " + var + "Str = " + var + " == null ? \"\" : " + var + ".getId() != null ? " + var + ".getId() : " + var + ".getText() == null ? \"\" : " + var + ".getText() ;"
//						);
//				});
		rw.collectData("list", "Characteristic", null, "bySearch", qp,
				row -> {
					String var = toLowerCamelCase(row.getJSONArray("values").getString(0));
					System.out.println(
								"vals.put(" + var + "Str);"
							);
				});

//		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> System.out.println( ".put(new org.json.JSONObject().put(\"identifier\", \"Product2GCharacteristicValueLang.Value('" + row.getJSONArray("values").getString(0) + "',root,\\\"0000.0000.RK\\\",'" + row.getJSONArray("values").getString(0) + "',-1)\"))" ));
//		rw.collectData("list", "Characteristic", null, "bySearch", qp,
//				row -> System.out.println(
//							"Value " + toLowerCamelCase(row.getJSONArray("values").getString(0)) + " = valMap.get(\"" + row.getJSONArray("values").getString(0) + "\");"
//						));
//		rw.collectData("list", "Characteristic", null, "bySearch", qp,
//				row -> {
//					String var = toLowerCamelCase(row.getJSONArray("values").getString(0));
//					System.out.println(
//							"String " + var + "Str = " + var + " == null ? \"\" : " + var + ".getId() != null ? " + var + ".getId() : " + var + ".getText() == null ? \"\" : " + var + ".getText() ;"
//						);
//				});
//		rw.collectData("list", "Characteristic", null, "bySearch", qp,
//				row -> {
//					String var = toLowerCamelCase(row.getJSONArray("values").getString(0));
//					System.out.println(
//								"vals.put(" + var + "Str);"
//							);
//				});
	}

	private static String toLowerCamelCase(String input) {
		if (input == null)
			return null;
		String s = input.trim();
		if (s.isEmpty())
			return s;
		StringBuilder out = new StringBuilder(s.length());
		boolean nextUpper = false;
		boolean started = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);

			if (!Character.isLetterOrDigit(c)) {
				if (started)
					nextUpper = true;
				continue;
			}
			if (!started) {
				out.append(Character.toLowerCase(c));
				started = true;
				nextUpper = false;
			} else if (nextUpper) {
				out.append(Character.toUpperCase(c));
				nextUpper = false;
			} else {
				out.append(Character.toLowerCase(c));
			}
		}
		return out.toString();
	}

}
