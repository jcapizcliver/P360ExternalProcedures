package mx.com.liverpool.p360.services.core.temp.pvia;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class LoadArticleDataForAdmin {

	private static int products = 0;
	private static int articles = 0;
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.util.Map<String, String> productoNegocio = new java.util.HashMap<>();
		DataRequestor dr = new DataRequestor();
		org.json.JSONArray items = new org.json.JSONArray();
		/*
			  variant
			 ,ProductNo
			 ,SupplierPartNumber
			 ,ColoursLiverpoolAtt
			 ,TamanoUnico
			 ,ProductImage
			 ,SKU
			 ,EAN
			 ,AssignTakeNoTake
		 */
		System.out.println("************ Now articles ************");
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
			if(arr.length > 0 && !"variant".equals(arr[0])) {
				items.put(new org.json.JSONObject()
						.put("variant", arr[0])
						.put("ProductNo", arr[1] )
						.put("SupplierPartNumber", arr[2])
						.put("ColoursLiverpoolAtt", arr[3])
						.put("TamanoUnico", arr[4])
						.put("ProductImage", arr[5])
						.put("SKU", arr[6])
						.put("MainBarCode", arr[7])
						.put("MainBarCodeS4H", "" /* negocio != null && !"".equals(negocio) ? ( "SBB".equals(negocio) ? arr[7] : "" ) : arr[7] */)
						.put("AssignTakeNoTake", arr.length > 8 ? arr[8] : "")
						)
				;
				if(items.length() == 1000) {
					System.out.println( dr.putArticleData(items)  + " +" + items.length());
					articles += items.length();
					while(items.length() > 0 ) {
						items.remove(0);
					}
				}
			}
		} );
		parser.parse(java.nio.file.Paths.get(args[0]));
		if(items.length() > 0) {
			System.out.println( dr.putArticleData(items)  + " +" + items.length());
			articles += items.length();
			while(items.length() > 0 ) {
				items.remove(0);
			}
		}
		System.out.println("Products: " + products);
		System.out.println("Articles: " + articles);
		System.out.println("Done. " + new RESTWorkshop().formatTime(System.currentTimeMillis() - init));
	}
	
}
