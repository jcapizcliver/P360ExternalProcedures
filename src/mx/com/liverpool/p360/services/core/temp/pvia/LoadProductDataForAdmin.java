package mx.com.liverpool.p360.services.core.temp.pvia;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class LoadProductDataForAdmin {

	private static int products = 0;
	private static int articles = 0;
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.util.Map<String, String> productoNegocio = new java.util.HashMap<>();
		/*
		  Identifier
		 ,Status
		 ,SKU
		 ,EAN
		 ,Business
		 ,SupplierPartNumber
		 ,Section
		 ,SAPObjectType
		 ,BrandName
		 ,BRAND_ID_S4H
		 ,SupplierID
		 ,ItemGroup
		 ,ItemGroupS4H
		 ,FotoTomadaLiverpool
		 ,AssignTakeNoTake
		 ,Template
		 */
		DataRequestor dr = new DataRequestor();
		org.json.JSONArray items = new org.json.JSONArray();
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
			if(arr.length > 0 && !"Identifier".equals(arr[0])) {
				productoNegocio.put(arr[0], arr[4]);
				items.put(new org.json.JSONObject()
						.put("product", arr[0])
						.put("CurrentStatus", arr[1])
						.put("SKU", arr[2])
						.put("MainBarCode", !"SBB".equals(arr[4]) ? arr[3] : "")
						.put("MainBarCodeS4H", "SBB".equals(arr[4]) ? arr[3] : "")
						.put("Business", arr[4])
						.put("SupplierPartNumber", arr[5])
						.put("Section", arr[6])
						.put("SAPObjectType", arr[7])
						.put("BrandName", arr[8])
						.put("BRAND_ID_S4H", arr[9])
						.put("SupplierID", arr[10])
						.put("ItemGroup", arr[11])
						.put("ItemGroupS4H", arr[12])
						.put("FotoTomadaLiverpool", arr[13])
						.put("AssignTakeNoTake", arr[14])
						.put("Template", arr.length > 15 ? arr[15] : "")
					)
				;
				if(items.length() == 1000) {
					System.out.println( dr.putProductData(items)  + " +" + items.length());
					products += items.length();
					while(items.length() > 0 ) {
						items.remove(0);
					}
				}
			}
		} );
		parser.parse(java.nio.file.Paths.get(args[0]));
		if(items.length() > 0) {
			System.out.println( dr.putProductData(items)  + " +" + items.length());
			products += items.length();
			while(items.length() > 0 ) {
				items.remove(0);
			}
		}
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
		parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
			if(arr.length > 0 && !"variant".equals(arr[0])) {
				String negocio = productoNegocio.get(arr[1]);
				items.put(new org.json.JSONObject()
						.put("variant", arr[0])
						.put("ProductNo", arr[1] )
						.put("SupplierPartNumber", arr[2])
						.put("ColoursLiverpoolAtt", arr[3])
						.put("TamanoUnico", arr[4])
						.put("ProductImage", arr[5])
						.put("SKU", arr[6])
						.put("MainBarCode", negocio != null && !"".equals(negocio) ? ( !"SBB".equals(negocio) ? arr[7] : "" ) : arr[7])
						.put("MainBarCodeS4H", negocio != null && !"".equals(negocio) ? ( "SBB".equals(negocio) ? arr[7] : "" ) : arr[7])
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
		parser.parse(java.nio.file.Paths.get(args[1]));
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
