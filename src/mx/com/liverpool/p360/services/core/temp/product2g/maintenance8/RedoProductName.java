package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;
import mx.com.liverpool.p360.services.core.dq.NameAndProductName;

public class RedoProductName {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	private static int a = 0;
	private static StringBuilder sb = new StringBuilder();
	
	public static void main(String[] args) {
		
		org.json.JSONArray genFE = new org.json.JSONArray();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2GStructureMap.StructureGroup('PrimaryProductTaxonomy')->StructureGroupAttributeValue.Value(OrderOfAtributesForName,es,DEFAULT),Product2GStructureMap.StructureGroup('PrimaryProductTaxonomy')->StructureGroup.Identifier");
		qp.put("pageSize", "1000");
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GLang.ProductName(es)")), 1000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println) );
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', null, "\r\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			a++;
			if(row.length == 0) {
				return;
			}
			if(a == 1) {
				return;
			}//			System.out.println("....");
			sb.append((sb.length() == 0 ? "" : ",") + "'" + row[0] + "'@1");
			if( (a-1) % 1000 == 0 ) {
				qp.put("items", sb.toString());
				rw.collectData("list", "Product2G", null, "byItems", qp, row0 -> {
					org.json.JSONArray values = row0.getJSONArray("values");
					if(!"".equals(values.getJSONArray(1).getString(0))) {
						NameAndProductName nn = new NameAndProductName(values.getString(0), values.getJSONArray(1).getString(0), 0, genFE);
						nn.setSourceTemplate(values.getJSONArray(2).getString(0));
						java.util.Map<String, org.json.JSONObject> data = new java.util.HashMap<>();
						org.json.JSONArray characteristicRecords = new org.json.JSONArray();
						nn.processData(data, characteristicRecords);
						org.json.JSONObject productNameJO = data.get("ProductName");
						if(productNameJO != null) {
//							System.out.print(".");
							rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@1")).put("values", new org.json.JSONArray().put(productNameJO.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0))));
						}
					}
				});
				sb.setLength(0);
			}
		} );
		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SourceST.csv"));
//		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "OtrosSinTitulo.csv"));
//		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "SKUSINTITULO.txt"));
//		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "OtrosTitulos.csv"));
//		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "ElTitulos.csv"));
		qp.put("items", sb.toString());
		rw.collectData("list", "Product2G", null, "byItems", qp, row0 -> {
			org.json.JSONArray values = row0.getJSONArray("values");
			if(!"".equals(values.getJSONArray(1).getString(0))) {
				NameAndProductName nn = new NameAndProductName(values.getString(0), values.getJSONArray(1).getString(0), 0, genFE);
				nn.setSourceTemplate(values.getJSONArray(2).getString(0));
				java.util.Map<String, org.json.JSONObject> data = new java.util.HashMap<>();
				org.json.JSONArray characteristicRecords = new org.json.JSONArray();
				nn.processData(data, characteristicRecords);
				org.json.JSONObject productNameJO = data.get("ProductName");
				if(productNameJO != null) {
//					System.out.print(".");
					rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@1")).put("values", new org.json.JSONArray().put(productNameJO.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0))));
				}
			}
		});
		sb.setLength(0);
		rh.sendData();
		
	}
	
}

