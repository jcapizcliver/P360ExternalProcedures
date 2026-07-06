package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ColocaTypeMainBarCode12Digits {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('TypeMainBarCode',root,\"0000.0000.RK\",'TypeMainBarCode',-1)")), 1000, request -> rw.writeData("list", "Article", null, qp, request, System.out::println) );
		RequestHandler rh2 = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('NUMTP_S4H',root,\"0000.0000.RK\",'NUMTP_S4H',-1)")), 1000, request -> rw.writeData("list", "Article", null, qp, request, System.out::println) );
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser('"',',','\\',"\n",java.nio.charset.StandardCharsets.UTF_8, row -> {
			if(row.length == 0 || "Identifier".equals(row[0])) {
				
				return;
			}
			String type = null;
			if("1754611681848316".equals(row[0]))
				System.out.println(java.util.Arrays.asList(row));
			if("245870".equals(row[3]) || "245869".equals(row[3])) {
				type = getTypeMainBarCode(row[4], "Liverpool");
				if("1754611681848316".equals(row[0]))
					System.out.println(type);
				rh .addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row[0] + "'@1")).put("values", new org.json.JSONArray().put(type)));
			}else {
				type = getTypeMainBarCode(row[4], "Suburbia");
				if("1754611681848316".equals(row[0]))
					System.out.println(type);
				rh2.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + row[0] + "'@1")).put("values", new org.json.JSONArray().put(type)));
			}
		});
		parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "sqlrunner_PIM_MASTER_20260702_000951.csv"));
		rh.sendData();
		rh2.sendData();
	}
	
	private static String getTypeMainBarCode(String mainBarCode, String business) {
		try{
			Long mbc = Long.parseLong(mainBarCode);
			if("Suburbia".equals(business)) {
				if( mbc.compareTo(750_013_500_000l) >= 0 && mbc.compareTo(750_013_599_999l) <= 0 ) {
					return "MP";
				}else if( mbc.compareTo(750_013_600_000l) >= 0 && mbc.compareTo(999_999_999_999l) <= 0 ) {
					return "H2";
				}
			}else {
				if( mbc.compareTo(300_000_000_000l) >= 0 && mbc.compareTo(750_057_499_999l) <= 0 ) {
					return "HE";
				}else if( mbc.compareTo(750_057_500_000l) >= 0 && mbc.compareTo(750_057_599_999l) <= 0 ) {
					return "MP";
				}else if(mbc.compareTo(750_057_600_000l) >= 0 && mbc.compareTo(999_999_999_999l) <= 0) {
					return "H2";
				}
			}
		}catch(NumberFormatException e) {
			e.printStackTrace();
		}
		return "UC";
	}
	
}
