package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CollectComputedIGIA {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		 java.util.Map<String, String> qp = new java.util.HashMap<>();
		 qp.put("fields", 
				    "Product2G.ProductNo"
		    	 + ",Product2G.SKU"
		    	 + ",Product2GLang.DescriptionLong(es)"
		    	 + ",Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroup.Identifier"
		    	 + ",Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)->StructureGroupLang.Name(es)"
		    	 + ",SimpleProduct2GCharacteristicValue.LookupValue('ProductTypeSAP')->LookupValue.Code"
		    	 + ",SimpleProduct2GCharacteristicValue.LookupValue('ProductTypeSAP')->LookupValueLang.Name(es)"
				 + ",Product2GExtraData.ItemGroup(MX)->LookupValue.Code"
				 + ",Product2GExtraData.ItemGroup(MX)->LookupValueLang.Name(es)"
				 + ",Product2GExtraData.Section(MX)->LookupValue.Code"
				 + ",Product2GExtraData.Section(MX)->LookupValueLang.Name(es)"
				 + ",Product2GExtraData.Direccion(MX)->LookupValue.Code"
				 + ",Product2GExtraData.Direccion(MX)->LookupValueLang.Name(es)"
				 + ",SimpleProduct2GCharacteristicValueLang.Value('ItemGroupIAConfidenceIG',-1)"
				 + ",SimpleProduct2GCharacteristicValueLang.Value('ItemGroupIAConfidenceSec',-1)"
				 + ",SimpleProduct2GCharacteristicValueLang.Value('ItemGroupIAConfidenceDir',-1)"
			);
		 qp.put("pageSize", "5000");
		 qp.put("query", "not characteristic('ItemGroupIAConfidenceIG') is empty");
		 try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "IGs desde IA").toFile())))){
			 pw.println( rw.getRw().serializeChunk(new Object[] { 
					  "ProductNo"
					 ,"SKU"
					 ,"DescriptionLong"
		    		 ,"Template"
		    		 ,"Template_Name_es"
		    		 ,"ProductTypeSAP"
		    		 ,"ProductTypeSAP_Name_es"
					 ,"ItemGroup"
					 ,"ItemGroup_Name_es"
					 ,"Section"
					 ,"Section_Name_es"
					 ,"Direction"
					 ,"Direction_Name_es"
					 ,"ItemGroupScore"
					 ,"SectionScore"
					 ,"DirectionScore"
			 }) );
			 rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
				 org.json.JSONArray values = row.getJSONArray("values");
				 pw.println( rw.getRw().serializeChunk( new Object[] { 
						   values.getString(0)
						 , values.getString(1)
						 , values.getString(2)
						 , values.getJSONArray(3).getString(0)
						 , values.getJSONArray(4).getString(0)
						 , values.getJSONArray(5).getString(0)
						 , values.getJSONArray(6).getString(0)
						 , values.getString(7)
						 , values.getString(8)
						 , values.getString(9)
						 , values.getString(10)
						 , values.getString(11)
						 , values.getString(12)
						 , values.getJSONArray(13).getString(0)
						 , values.getJSONArray(14).getString(0)
						 , values.getJSONArray(15).getString(0)
						} ) );
			 });
		 }catch(java.io.IOException e) {
			 e.printStackTrace();
		 }
	}
	
}
