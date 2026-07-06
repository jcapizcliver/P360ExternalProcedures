package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class FlattenDireccionSeccionGrupoDeArticulos {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields",
				"Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
				+ ",Product2GCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"
				+ ",Product2GCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"
				+ ",Product2GCharacteristicValue.LookupValue('Negocio',root,\"0000.0000.RK\",'Negocio')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Negocio',root,\"0000.0000.RK\",'Negocio')->LookupValueLang.Name(es)"
				+ ",Product2GCharacteristicValue.LookupValue('EXTWG_S4H',root,\"0000.0000.RK\",'EXTWG_S4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('EXTWG_S4H',root,\"0000.0000.RK\",'EXTWG_S4H')->LookupValueLang.Name(es)"
				+ ",Product2GCharacteristicValueLang.Value('SupplierID',root,\"0000.0000.RK\",'SupplierID',-1)"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValueLang.Name(es)"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValueLang.Name(es)"
				+ ",Product2GCharacteristicValue.LookupValue('Section',root,\"0000.0000.RK\",'Section')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Section',root,\"0000.0000.RK\",'Section')->LookupValueLang.Name(es)"
				+ ",Product2GCharacteristicValue.LookupValue('Direction',root,\"0000.0000.RK\",'Direction')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Direction',root,\"0000.0000.RK\",'Direction')->LookupValueLang.Name(es)"
			);
		qp.put("pageSize", "20000");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "samples", "Productos3").toFile())))){
			pw.println( rw.getRw().serializeChunk(new String[] { "ProposalID", "SKU", "MainBarCode", "MainBarCodeS4H", "Negocio", "Negocio_ES", "EXTWG_S4H"
					, "EXTWG_S4H_ES", "SupplierID", "ItemGroup", "ItemGroup_ES", "ItemGroupS4H", "ItemGroupS4H_ES", "Section", "Section_ES", "Direction", "Direction_ES"}) );
			rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> printRowData(row, pw), System.out::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void printRowData(org.json.JSONObject row, java.io.PrintWriter pw) {
		org.json.JSONArray values = row.getJSONArray("values");
		String[] data = new String[values.length()];
		data[0] = values.getString(0);
		for(int i=1; i<data.length; i++) {
			data[i] = values.getJSONArray(i).getString(0);
		}
		pw.println(rw.getRw().serializeChunk(data));
	}
	
}
