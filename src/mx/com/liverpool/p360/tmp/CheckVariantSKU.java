package mx.com.liverpool.p360.tmp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CheckVariantSKU {

	private static final RESTWorkshop workshop = new RESTWorkshop();
	
	public static void main(String[] args) {
		String[] info = null;
		for(String sku : skus) {
			info = checkProductBySKU(sku);
			if(info != null) {
				checkSomeData(info[0], sku);
//				checkParentVariantsCompleteness(info[0], sku);
			}else {
				System.out.println("SE OMITE: " + sku);
			}
		}
	}
	
	private static void checkParentVariantsCompleteness(String productId, String sku) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				"Article.SupplierAID"
				+ ",ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + productId + "\") equals \"" + productId + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
//		System.out.println(sku);
		response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
		if(response != null) {
			rows = response.getJSONArray("rows");
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				sb.append( (sb.length() == 0 ? "" : ",") + values.getJSONArray(1).getString(0));
			}
			System.out.println(sb);
		}else {
		}
	}
	
	private static void checkSomeData(String productId, String sku) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				  "Product2GCharacteristicValueLang.Value('Name',root,\"0000.0000.RK\",'Name',-1)"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValue.Code");
		qp.put("query", "Product2G.ProductNo equals \"" + productId + "\"");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		response = workshop.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
		if(response != null) {
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				System.out.println(values.getJSONArray(0).getString(0));
//				System.out.println("".equals(values.getJSONArray(1).getString(0)) ? values.getJSONArray(2).getString(0) : values.getJSONArray(1).getString(0));
			}
		}else {
			System.out.println(workshop.getRawResponse());
		}
	}
	
	private static String[] checkProductBySKU(String sku0) {
		String sku = sku0.startsWith("SB") ? sku0.substring(2) : sku0;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("query",  "characteristic('SKU',-1) equals \"" + sku + "\"");
		qp.put("fields", 
				  "Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool')->LookupValue.Code"
				+ ",Product2G.CurrentStatus"
				);
		org.json.JSONObject response = null;
		response = workshop.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
		return (response != null && response.getJSONArray("rows").length() > 0) ? new String[] { 
				  response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0)
				, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(1).getString(0)
				, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(2).getString(0)
				, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(3).getString(0)
				, response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(4)
				} : null;
	}
	
	
	private static final String[] skus = (
			  "1033607397\r\n"
			+ "1033607438\r\n"
			+ "1033607443\r\n"
			+ "1033634629\r\n"
			+ "1033624283\r\n"
			+ "1033594163\r\n"
			+ "1033626301\r\n"
			+ "1033607381\r\n"
			+ "1033607110\r\n"
			+ "1033626297\r\n"
			+ "1033626301\r\n"
			+ "Sin Insumo\r\n"
			+ "1033607110\r\n"
			+ "1033594279\r\n"
			+ "1033624135\r\n"
			+ "1033634629\r\n"
			+ "100000000275\r\n"
			+ "1033607079\r\n"
			+ "1033594212\r\n"
			+ "Sin Insumo\r\n"
			+ "1033594228\r\n"
			+ "Sin Insumo\r\n"
			+ "1698767481633155\r\n"
			+ "1698767481633167\r\n"
			+ "1698767481633155\r\n"
			+ "1698767481633167\r\n"
			+ "1033589227\r\n"
			+ "1033607977\r\n"
			+ "1698767481635018\r\n"
			+ "1698767481635055\r\n"
			+ "1698767481635055\r\n"
			+ "1698767481635055\r\n"
			+ "1033607951\r\n"
			+ "1033607306\r\n"
			+ "1698767481635055"
			/*
			  "SB5006179225\r\n"
			+ "SB5006175955\r\n"
			+ "SB5006177290\r\n"
			+ "SB5006190971\r\n"
			+ "SB5006191691\r\n"
			+ "SB5006179268\r\n"
			+ "SB5006195859\r\n"
			+ "SB5006195824\r\n"
			+ "SB5006178717\r\n"
			+ "SB5006179241\r\n"
			+ "SB5006184008\r\n"
			+ "SB5006183796\r\n"
			+ "SB5006183761\r\n"
			+ "SB5006185357\r\n"
			+ "SB5006187988\r\n"
			+ "SB5006187996\r\n"
			+ "SB5006183974\r\n"
			+ "SB5006185063\r\n"
			+ "SB5006187007\r\n"
			+ "SB5006187937\r\n"
			+ "SB5006178881\r\n"
			+ "SB5006191691\r\n"
			+ "SB5006190971\r\n"
			+ "SB5006179098\r\n"
			+ "SB5006187937\r\n"
			+ "1698767481633155\r\n"
			+ "1698767481633167\r\n"
			+ "1698767481633155\r\n"
			+ "1698767481633167\r\n"
			+ "1698767481635018\r\n"
			+ "1698767481635055\r\n"
			+ "1698767481635055\r\n"
			+ "1698767481635055\r\n"
			+ "1698767481635055"
			*/
//			"1033574912\r\n"
//			+ "1033541801\r\n"
//			+ "1033603855\r\n"
//			+ "1033542899\r\n"
//			+ "1033624283"
/*			
			  "1033603855\r\n"
			  + "1033612650\r\n"
			  + "1033573258\r\n"
			  + "1033583369\r\n"
			  + "1033626301\r\n"
			  + "1033545821\r\n"
			  + "1033541402\r\n"
			  + "1033521681\r\n"
			  + "1033626297\r\n"
			  + "1033626301\r\n"
			  + "1033612650\r\n"
			  + "1033612668\r\n"
			  + "1033521673\r\n"
			  + "1033542875\r\n"
			  + "1033594279\r\n"
			  + "1033573096\r\n"
			  + "1033634629\r\n"
			  + "1033606873\r\n"
			  + "100000000275\r\n"
			  + "1033541887\r\n"
			  + "1033574904\r\n"
			  + "1033521703\r\n"
			  + "1033520871\r\n"
			  + "1033522731\r\n"
			  + "1033608361\r\n"
			  + "1033615381\r\n"
			  + "1033610129\r\n"
			  + "1033590578\r\n"
			  + "1698767481633155\r\n"
			  + "1698767481633167\r\n"
			  + "1698767481633155\r\n"
			  + "1698767481633167\r\n"
			  + "1033589227\r\n"
			  + "1033607977\r\n"
			  + "1698767481635018\r\n"
			  + "1698767481635055\r\n"
			  + "1698767481635055\r\n"
			  + "1698767481635055\r\n"
			  + "1033607951\r\n"
			  + "1033607306\r\n"
			  + "1698767481635055"
*/ 
		).split("\\r\\n");
	
	private static final String[] genSKU = (
			  "1033574912\r\n"
			+ "1033541801\r\n"
			+ "1033603855\r\n"
			+ "1033542899\r\n"
			+ "1033624283\r\n"
			+ "1033603855\r\n"
			+ "1033612650\r\n"
			+ "1033573258\r\n"
			+ "1033583369\r\n"
			+ "1033626301\r\n"
			+ "1033545821\r\n"
			+ "1033541402\r\n"
			+ "1033521681\r\n"
			+ "1033626297\r\n"
			+ "1033626301\r\n"
			+ "1033612650\r\n"
			+ "1033612668\r\n"
			+ "1033521673\r\n"
			+ "1033542875\r\n"
			+ "1033594279\r\n"
			+ "1033573096\r\n"
			+ "1033634629\r\n"
			+ "1033606873\r\n"
			+ "100000000275\r\n"
			+ "1033541887\r\n"
			+ "1033574904\r\n"
			+ "1033521703\r\n"
			+ "1033520871\r\n"
			+ "1033522731\r\n"
			+ "1033608361\r\n"
			+ "1033615381\r\n"
			+ "1033610129\r\n"
			+ "1033590578\r\n"
			+ "1698767481633155\r\n"
			+ "1698767481633167\r\n"
			+ "1698767481633155\r\n"
			+ "1698767481633167\r\n"
			+ "1033589227\r\n"
			+ "1033607977\r\n"
			+ "1698767481635018\r\n"
			+ "1698767481635055\r\n"
			+ "1698767481635055\r\n"
			+ "1698767481635055\r\n"
			+ "1033607951\r\n"
			+ "1033607306\r\n"
			+ "1698767481635055"
		).split("\r\n");
}
