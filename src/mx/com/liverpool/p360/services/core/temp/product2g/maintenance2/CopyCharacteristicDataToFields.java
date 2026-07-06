package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class CopyCharacteristicDataToFields {
	
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
//		rw.getRw().setBaseUrl("https://172.18.237.210:1512/rest/V2.0");
//		rw.getRw().addHeader("Authorization", java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
		String[] suppliers = collectSuppliers();
		java.util.Set<String> notInList = new java.util.TreeSet<>();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", 
				   "Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('SupplierID',root,\"0000.0000.RK\",'SupplierID',-1)"
				+ ",Product2GCharacteristicValueLang.Value('SupplierPartNumber',root,\"0000.0000.RK\",'SupplierPartNumber',-1)"
				+ ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
				+ ",Product2GCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"
				+ ",Product2GCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"
				+ ",Product2GCharacteristicValueLang.Value('ProductName',root,\"0000.0000.RK\",'ProductName',-1)"
				+ ",Product2GCharacteristicValue.LookupValue('Direction',root,\"0000.0000.RK\",'Direction')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Section',root,\"0000.0000.RK\",'Section')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('BrandName',root,\"0000.0000.RK\",'BrandName')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Negocio',root,\"0000.0000.RK\",'Negocio')->LookupValue.Code"
			);
		qp.put("pageSize", "30000");
		qp.put("query", "not (Product2G.ProductNo startsWith \"Product2G_\" or Product2G.ProductNo startsWith \"175461166\")");
		org.json.JSONArray columns = new org.json.JSONArray();
		columns
			.put(new org.json.JSONObject().put("identifier", "Product2G.Business"))
			.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.SupplierID(MX)"))
			.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.SupplierPartNumber(MX)"))
			.put(new org.json.JSONObject().put("identifier", "Product2G.SKU"))
			.put(new org.json.JSONObject().put("identifier", "Product2G.EAN"))
			.put(new org.json.JSONObject().put("identifier", "Product2GLang.ProductName(es)"))
			.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.Direccion(MX)"))
			.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.Section(MX)"))
			.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.ItemGroup(MX)"))
			.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.ItemGroupS4H(MX)"))
			.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.BrandName(MX)"))
			.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.BRAND_ID_S4H(MX)"))
			.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.SAPObjectType(MX)"))
			.put(new org.json.JSONObject().put("identifier", "Product2GExtraData.Negocio(MX)"))
		;
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler(columns, 10000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println));
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String business = trapVal( values.getJSONArray(0).getString(0), "BusinessQualified");
			String supplierID = trapVal( values.getJSONArray(1).getString(0), "Party");
			String supplierPartNumber = values.getJSONArray(2).getString(0);
			String sku = values.getJSONArray(3).getString(0);
			String mainBarCode = values.getJSONArray(4).getString(0);
			if("".equals(mainBarCode)) {
				mainBarCode = values.getJSONArray(5).getString(0);
			}
			mainBarCode = mainBarCode.replaceAll("\\s+", "").trim();
            mainBarCode = !"".equals(mainBarCode) && mainBarCode.matches("^[0-9]+$") ? mainBarCode : "";
			String productName = values.getJSONArray(6).getString(0);
			String direction = trapVal( values.getJSONArray(7).getString(0), "Direction");
			String section = trapVal( values.getJSONArray(8).getString(0), "Section");
			String itemGroup = trapVal( values.getJSONArray(9).getString(0), "MATKLLOV");
			String itemGroupS4H = trapVal( values.getJSONArray(10).getString(0), "MATKLLOV_S4H");
			String brandName = trapVal( values.getJSONArray(11).getString(0), "ZCOMALOV");
			String brandIdS4H = trapVal( values.getJSONArray(12).getString(0), "BRAND_ID_S4H");
			String sapObjectType = trapVal( values.getJSONArray(13).getString(0), "ATTYPLOV");
			String negocio = trapVal( values.getJSONArray(14).getString(0), "EXTWGLOV");
			if(java.util.Arrays.binarySearch(suppliers, supplierID) < 0) {
				notInList.add(supplierID);
				supplierID = "";
			}
			org.json.JSONObject rw = new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray()
					.put(business)
					.put(supplierID)
					.put(supplierPartNumber)
					.put(sku)
					.put(mainBarCode)
					.put(productName)
					.put(direction)
					.put(section)
					.put(itemGroup)
					.put(itemGroupS4H)
					.put(brandName)
					.put(brandIdS4H)
					.put(sapObjectType)
					.put(negocio)
				);
			rh.addRow(rw);
		});
		rh.sendData();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("suppliers").toFile(), true)))){
			notInList.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
	private static String[] collectSuppliers() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code");
		qp.put("query", "LookupValue.IsActive = true");
		qp.put("lookup", "'Party'");
		qp.put("pageSize", "20000");
		java.util.List<String> data = new java.util.ArrayList<>();
		rw.collectData("list", "LookupValue", null, "bySearch", qp, row -> {
			data.add(row.getJSONArray("values").getString(0));
		} );
		String[] arr = data.toArray(new String[] {});
		java.util.Arrays.sort(arr);
		return arr;
	}

	private static String trapVal(String val, String lkpName) {
		return val;
	}
}
