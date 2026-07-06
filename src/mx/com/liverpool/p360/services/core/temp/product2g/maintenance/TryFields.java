package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class TryFields {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String,String> qp = new java.util.HashMap<>();
		qp.put("fields",
				"Product2G.ProductNo"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('ItemGroup')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('ItemGroup')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('SKU',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('EnrichmentRejectionMessage',-1)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('BrandName')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('BrandName',-1)->LookupValueLang.Name(es)"
				+ ",Product2G.CurrentStatus"
				+ ",Product2G.PrevStatus"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('IsPublishException')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('IsPublishException')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('SistemaOrigen')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('SistemaOrigen')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('Section')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('Section')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('Direction')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('Direction')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('Footnote',-1)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('FotoTomadaLiverpool')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('FotoTomadaLiverpool')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('IdentificaNegocio')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('IdentificaNegocio')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('GenderAtt')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('GenderAtt')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('ApprovedDateCalc',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('BrandNameATG',-1)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('ItemGroupS4H')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('ItemGroupS4H')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('SupplierName',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('SupplierID',-1)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('WHERL')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('WHERL')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('MainBarCode')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('MainBarCode')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('IDLastParent',-1)"
				+ ",Product2GStructureMap.StructureGroup('PrimaryProductTaxonomy')->StructureGroup.Identifier"
				+ ",Product2GStructureMap.StructureGroup('PrimaryProductTaxonomy')->StructureGroupLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('SupplierPartNumber',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('FechaUltimaAprobacion',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('AssignTakeNoTake',-1)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('MainBarCodeS4H')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('MainBarCodeS4H')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('Name',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('EnriquecidoEnForo',-1)"
				+ ",Product2GStructureMap.StructureGroup('Sitios Web')->StructureGroup.Identifier"
				+ ",Product2GStructureMap.StructureGroup('Sitios Web')->StructureGroupLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('BRAND_ID_S4H')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('BRAND_ID_S4H')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('EXTWG_S4H')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('EXTWG_S4H')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('ProductTypeSAP')->LookupValue.Code"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('ProductTypeSAP')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('BrandOwner',-1)"
			);
		qp.put("query","Product2G.ProductNo in (\"1754611647184002\",\"1754611647184010\",\"S65819902\",\"S65819904\",\"S65819905\",\"S65819909\",\"S65819911\",\"S65819915\",\"S73210586\",\"S73210588\",\"S73210589\",\"S73210590\",\"S73210591\",\"S73210592\",\"S73210593\",\"S73210594\",\"S73210595\",\"S73210597\",\"S73210601\",\"S73210604\",\"S73210605\",\"S73210606\",\"S73210607\",\"S73210608\",\"S73210609\",\"S73210610\",\"S73210611\",\"S73210612\",\"S73210613\",\"S73210614\",\"S73210615\",\"S73210616\",\"S73210617\",\"S73210618\",\"S73210619\",\"S73210620\",\"S73210621\",\"S73210622\",\"S73210623\",\"S73210624\",\"S73210626\",\"S73210631\",\"S74101005\",\"S74101013\",\"S74101020\",\"S74101021\",\"S74101027\",\"S74101028\",\"S74101030\",\"S74101031\",\"S74101034\",\"S74101035\",\"S74101037\",\"S74101039\",\"S74101040\",\"S74101041\",\"S74101048\",\"S74101049\",\"S74101051\",\"S74101052\",\"S74101053\",\"S74101054\",\"S74101056\",\"S74101058\",\"S74101062\",\"S74101063\",\"S74101064\",\"S74101065\",\"S74101066\",\"S74101067\",\"S74101069\",\"S74101070\",\"S74101073\",\"S74101074\",\"S74101077\",\"S74101081\",\"S74101083\",\"S74101084\",\"S74101085\",\"S74101086\",\"S74101091\",\"S74101096\",\"S74101097\",\"S73210781\",\"S73210782\",\"S73210784\",\"S73210786\",\"S73210788\",\"S73210790\",\"S74101278\",\"S74101280\",\"S74101282\",\"S74101288\",\"S74101290\",\"S74101295\",\"S74101296\",\"S74101297\",\"S74101299\",\"S74101300\",\"S74101302\")");
		java.util.List<String> ids = new java.util.ArrayList<>();
		rw.collectData("list","Product2G",null,"bySearch",qp,row -> {
			System.out.println(row.getJSONArray("values"));
			ids.add(row.getJSONObject("object").getString("id"));
		});
		StringBuilder sb = new StringBuilder();
		for(String id : ids) {
			sb.append(sb.length() == 0 ? "" : ",").append(id);
		}
		java.util.Map<String, String> qp1 = new java.util.TreeMap<>();
		qp1.put("fields", 
				"Article.SupplierAID"
				+ ",SimpleArticleCharacteristicValue.LookupValue('ColoursLiverpoolAtt')->LookupValue.Code"
				+ ",SimpleArticleCharacteristicValue.LookupValue('ColoursLiverpoolAtt')->LookupValueLang.Name(es)"
				+ ",SimpleArticleCharacteristicValue.LookupValue('TamanoUnico')->LookupValue.Code"
				+ ",SimpleArticleCharacteristicValue.LookupValue('TamanoUnico')->LookupValueLang.Name(es)"
				+ ",SimpleArticleCharacteristicValueLang.Value('TamanoUnicoSTD',-1)"
//				+ ",SimpleArticleCharacteristicValueLang.Value('clothingSize',-1)"
//				+ ",SimpleArticleCharacteristicValueLang.Value('SizeVaD',-1)"
				+ ",SimpleArticleCharacteristicValueLang.Value('SupplierPartNumber',-1)"
				+ ",SimpleArticleCharacteristicValueLang.Value('ProcedeNoProcede',-1)"
				+ ",SimpleArticleCharacteristicValueLang.Value('AIEnriched',-1)"
				+ ",SimpleArticleCharacteristicValueLang.Value('AIEnrichementDate',-1)"
			);
		qp1.put("products", sb.toString());
		qp1.put("pageSize", "10000");
		rw.collectData("list", "Article", null, "byProducts", qp1, row -> {
			System.out.println(row.getJSONArray("values"));
		});
        java.util.Map<String, String> itemToProduct = new java.util.HashMap<>();
        java.util.Map<String, String> qpRel = new java.util.HashMap<>();
        qpRel.put( "fields", "ProductReference.ReferencedSupplierAid" );
        qpRel.put( "pageSize", "50000" );
        qpRel.put("products", sb.toString());
		try { rw.collectData( "list", "Article", "ProductReference", "byProducts", qpRel, row -> { try{ itemToProduct.put( row.getJSONObject( "object" ).getString( "id" ), row.getJSONArray( "values" ).getString( 0 ) ); }catch(org.json.JSONException ignore) {} } ); }catch(org.json.JSONException ignore) {}
        itemToProduct.forEach((k,v)->System.out.println(k + " - " + v));
	}
	
}
