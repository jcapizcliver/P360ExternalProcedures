package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ArreglaTituloSinMarca {

	private static final RESTWrapper rw = new RESTWrapper();
	private static String productName = null;
	private static String marca = null;
	private static String sinMarca = null;
	private static int count = 0;
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('TituloSinMarca',root,\"0000.0000.RK\",'TituloSinMarca',-1)")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', null, "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			
			count++;
			if(count == 1) {
				return;
			}
			if( row.length == 0 ) {
				return;
			}
			
			if(!"".equals(row[2]) && (!"".equals(row[3]) || !"".equals(row[4]))) {
				productName = row[2];
				marca = "".equals( row[3] ) ? row[4] : row[3];
				productName = productName.replaceAll(" {2,}", " ");
				sinMarca = marca != null && !"".equals(marca) ? productName.replaceAll("(?iu)" + java.util.regex.Pattern.quote(marca), "").replaceAll(" {2,}", " ").trim() : productName;
				if( row.length == 6 || !row[6].equals(sinMarca) ) {
					rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row[1] + "'@1")).put("values", new org.json.JSONArray().put(sinMarca)));
				}
			}
			
		} );
		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "ToCheckTituloSinMarca_20260721_165348.csv"));
		rh.sendData();
	}
	
}
