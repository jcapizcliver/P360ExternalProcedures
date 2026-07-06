package mx.com.liverpool.p360.services.core.temp.product2g.maintenance4;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class UrgentProcessingToPublishCheckProductName {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("query", "Product2GLog.CreationDate(PIM) >= 2026-04-19T00:00:00 and Product2GLog.CreationDate(PIM) < 2026-04-21T00:00:00 and Product2G.CurrentStatus = \"Aprobada\"");
//		qp.put("query", "Product2GLang.Remarks(en) = \"URGENT\" or Product2G.CurrentStatus = \"Category\"");
		qp.put("pageSize", "5000");
		qp.put("fields",
					   "Product2G.ProductNo"
					+ ",Product2GLang.ProductName(es)"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('BrandName')->LookupValueLang.Name(es)"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('BRAND_ID_S4H')->LookupValueLang.Name(es)"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('GenderAtt')->LookupValueLang.Name(es)"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('GeneroVaD')->LookupValueLang.Name(es)"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('Direction')->LookupValue.Code"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('Direction')->LookupValueLang.Name(es)"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('Section')->LookupValueLang.Name(es)"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('ItemGroup')->LookupValueLang.Name(es)"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('ItemGroupS4H')->LookupValueLang.Name(es)"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('Negocio')->LookupValue.Code"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('EXTWG_S4H')->LookupValue.Code"
				    + ",SimpleProduct2GCharacteristicValue.LookupValue('ProductTypeSAP')->LookupValueLang.Name(es)"
					+ ",SimpleProduct2GCharacteristicValue.LookupValue('SB_0002')->LookupValueLang.Name(es)" 
					+ ",Product2GStructureMap.ManualMap(PrimaryProductTaxonomy)"
					+ ",Product2GStructureMap.ManualMap('Sitios Web')"
				);
		// Si tenemos, producto, título, negocio y path. podemos irnos por eso, también marketplace; evitar que se vaya con dobles rutas
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		
		try(java.io.PrintWriter pw = 
				new java.io.PrintWriter(
						new java.io.OutputStreamWriter(
								new java.io.FileOutputStream(
										java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "URGENT_Base_Productos_ProductName2.csv").toFile())))){
			pw.println( rw.getRw().serializeChunk( new String[] {
					  "ID"
					, "ProductName"
					, "Marca"
					, "Marca (SBB)"
					, "GenderAtt"
					, "GeneroVaD" 
					, "Dirección (cod)"
					, "Dirección"
					, "Sección" 
					, "Grupo de Artículo"
					, "Grupo de Artículo (Suburbia)" 
					, "Negocio" 
					, "Negocio (Suburbia)" 
					, "Producto (ECC)" 
					, "Producto (S4H)"
					, "Plantilla"
					, "CatIDs"
			} ) );
			rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				pw.println( rw.getRw().serializeChunk( asArray(values) ) );
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static String[] asArray(org.json.JSONArray values) {
		java.util.List<String> values0 = new java.util.ArrayList<>();
		for(int i=0; i<values.length(); i++) {
			values0.add( values.get(i) instanceof org.json.JSONArray ? values.getJSONArray(i).getString(0) : values.getString(i) );
		}
		return values0.toArray( new String[] {} );
	}
	
}
