package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class LoQueFaltaPubSubPOSTSKUEANs {

	
	private static final PubSubGCP pubPostProducts = new PubSubGCP(
		    PropertiesManager.get("p360.contingency.gcp.service_account_back"),
		    PropertiesManager.get("p360.contingency.gcp.project_back"),
		    PropertiesManager.get("p360.contingency.gcp.post_products_topic")
		);
	private static int cn = 0;
	private static String prevProdIdentifier = null;
	private static String prevProdSKU = null;
	private static String prevProdEAN = null;
	private static org.json.JSONArray variants = new org.json.JSONArray();
	
	public static void main(String[] args) {
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject body = new org.json.JSONObject();
		body.put("products", rows);
		
		rows.put( new org.json.JSONObject().put("proposalId", "1754611680334018").put("producto",new org.json.JSONObject().put("MainBarCode", "2")));
		System.out.println( pubPostProducts.publishMessage( body.toString() ) );
		while(rows.length() > 0 ) {
			rows.remove(0);
		}
		
		System.exit(0);
		/*
			sourcevariantId
			sourceupcEan
			sourcesize
			sourcecolour
			sourceurlImage
			sourcesku
			p360ArticleRevisionID
			p360ArticleIdentifier
			p360ArticleSKU
			p360ArticleEAN
			p360ProductID
			p360ProductSKU
			p360ProductEAN
		 */
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser('"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			cn++;
			if(row.length == 0) {
				return;
			}
			if(prevProdIdentifier != null && !prevProdIdentifier.equals(row[10])) {
//				org.json.JSONObject producto = null;
				rows.put( new org.json.JSONObject().put("proposalId", prevProdIdentifier).put("SKU", prevProdSKU).put("MainBarCode", prevProdEAN).put("variants", variants));
				if(rows.length() > 0) {
					System.out.println(body);
					pubPostProducts.publishMessage( body.toString() );
					while(rows.length() > 0 ) {
						rows.remove(0);
					}
				}
				variants = new org.json.JSONArray();
			}
			variants.put(new org.json.JSONObject().put("variantId", row[0]).put("SKU", row[8]).put("MainBarCode", row[9]));
//			if(cn < 101) {
//				System.out.println(variants);
//			}else { System.out.println(body); System.exit(0); }
			prevProdIdentifier = row[10];
			prevProdSKU = row[11];
			prevProdEAN = row.length > 12 ? row[12] : "";
		});
		parser.parse(java.nio.file.Paths.get("/", "u01", "stage", "DistinctSKU_or_EAN_Article_P360_EUCat.sorted"));
		rows.put( new org.json.JSONObject().put("proposalId", prevProdIdentifier).put("SKU", prevProdSKU).put("MainBarCode", prevProdEAN).put("variants", variants));
		pubPostProducts.publishMessage( body.toString() );
		while(rows.length() > 0 ) {
			rows.remove(0);
		}
	}
	
}
