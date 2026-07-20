package mx.com.liverpool.p360.services.core.net;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CliTest {
	
	public static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		DataRequestor dr = new DataRequestor();
		try {
			if("HELP".equals(args[0])) {
				help();
			}else if("dump".equals(args[0])) {
				System.out.println( dr.dump() );
			} else if("getProductData".equals(args[0])) {
				System.out.println( dr.getProductData(new org.json.JSONArray().put(args[1])) );
			}else if("getArticleData".equals(args[0])) {
				System.out.println( dr.getArticleData(new org.json.JSONArray().put(args[1])) );
			}else if("getProductExtraData".equals(args[0])) {
				String resp = null;
				System.out.println( resp = dr.getProductExtraData(new org.json.JSONArray().put(args[1])) );
				try{
					org.json.JSONObject jr = new org.json.JSONObject(resp);
					System.out.println(jr.getJSONArray("items").getJSONObject(0).getString("DescriptionLong"));
				}catch(org.json.JSONException e) {
					e.printStackTrace();
				}
			}else if("getArticleExtraData".equals(args[0])) {
				System.out.println( dr.getArticleExtraData(new org.json.JSONArray().put(args[1])) );
			}else if("getSkuProductNo".equals(args[0])) {
				System.out.println( dr.getProductBySKU( new org.json.JSONArray().put(args[1]) ) );
			}else if("putProductData".equals(args[0])){
				enviaDataProducto(args[1]);
			}else if("putProposalData".equals(args[0])) {
				enviaDataPropuesta(args[1]);
			}else if("putArticleData".equals(args[0])){
				enviaDataArticulo(args[1]);
			}else if("skuSupplierAID".equals(args[0])){
				System.out.println( dr.putSkuSupplierAID( makeItemsForSKUSupplierAID(args[1]) ) );
			}else if("skuProductNo".equals(args[0])) {
				sendSkuProductNo(args[1]);
			}else if("checkArticleBySKU".equals(args[0])) {
				System.out.println( dr.articleBySKU( new org.json.JSONArray().put(args[1]) ) );
			}else if("productByVariant".equals(args[0])) {
				System.out.println( dr.getProductByVariant(new org.json.JSONArray().put(args[1])) );
			} else if("getVariants".equals(args[0])) {
				System.out.println( dr.getVariants(args[1]) );
			} else if("variantBySKU".equals(args[0])){
				String resp = dr.sendRequest(
						new org.json.JSONObject()
							.put("action", "variantBySKU")
							.put("skus", new org.json.JSONArray().put( args[1] ))
						.toString()
					);
				System.out.println(resp);
				java.util.Map<String, java.util.Set<String>> l = dr.articleBySKUs( new org.json.JSONArray().put( args[1] ));
				System.out.println(l);
			} else if("refreshProductLinks".equals(args[0])){
				refreshProductLinks(args[1], args.length > 2 ? args[3] : null);
			} else if("refreshSKUProductNoReferences".equals(args[0])) {
				refreshSKUProductNoReferences();
			} else if("refreshSKUProductNoReferences2".equals(args[0])) {
				refreshSKUProductNoReferences2(args[1]);
			} else if("retiraProducto".equals(args[0])) {
				dr.retiraProducto(new org.json.JSONArray().put(args[1]));
			} else if("retiraArticulo".equals(args[0])) {
				dr.retiraArticulo(new org.json.JSONArray().put(args[1]));
			} else if("retiraProductoPorSKU".equals(args[0])) {
				dr.retiraProductoPorSKU(new org.json.JSONArray().put(args[1]));
			} else if("retiraArticuloPorSKU".equals(args[0])) {
				dr.retiraArticuloPorSKU(new org.json.JSONArray().put(args[1]));
			}else if("loadExtraData".equals(args[0])) {
				putExtraData();
			}else if("articleByEAN".equals(args[0])) {
				System.out.println( dr.supplierAIDByEAN(new org.json.JSONArray().put(args[1])) );
			}else if("productByEAN".equals(args[0])) {
				System.out.println( dr.productNoByEAN(new org.json.JSONArray().put(args[1])) );
			}else if("fillInGlobalMetaDataForVendorCenterInfo".equals(args[0])) {
				sendGlobalMetaDataToAdmin();
			}else if("fillInTemplateCharacteristicMetaDataForVendorCenterInfo".equals(args[0])) {
				sendTemplateCharacteristicMetaDataToAdmin();
			}else if("sendCharacteristicsData".equals(args[0])) {
				sendCharacteristicData();
			}else if("sendTemplateNames".equals(args[0])) {
				sendTemplateNames();
			}else if("getCharacteristicData".equals(args[0])) {
				System.out.println( dr.getCharacteristicData(new org.json.JSONArray().put(args[1])) );
			}else if("getTemplateName".equals(args[0])) {
				System.out.println( dr.getTemplateName(new org.json.JSONArray().put(args[1])) );
			}else if("getGlobalMetaData".equals(args[0])) {
				System.out.println( dr.getGlobalMetaData() );
			}else if("addContenidoDeDiccionario".equals(args[0])) {
				System.out.println( dr.addContenidoDeDiccionario(new org.json.JSONArray().put( new org.json.JSONObject().put("diccionario", args[1]).put("idValor", args[2]).put(args[3], args[4]))) );
			}else if("addTemplateCharacteristicMetaData".equals(args[0])) {
				System.out.println( dr.addTemplateCharacteristicMetaData(new org.json.JSONArray().put( new org.json.JSONObject().put("template", args[1]).put("characteristic", args[2]).put("property", args[3]).put("propertyValue", args[4]).put("creationType", args.length > 5 ? args[5] : "CreateProposal"))) );
			} else if("sendContenidoDeDiccionario".equals(args[0])) {
				String r = dr.getContenidoDeDiccionario(new org.json.JSONArray().put(new org.json.JSONObject().put("diccionario", args[1]).put("idValor", args[2])));
				System.out.println(r);
			}else if("getContenidoDeDiccionario".equals(args[0])) {
				System.out.println( dr.getContenidoDeDiccionario(new org.json.JSONArray().put(new org.json.JSONObject().put("diccionario", args[1]).put("idValor", args[2]))) );
			}else if("getTemplateCharacteristicMetaDataByTemplate".equals(args[0])) {
				System.out.println( dr.getTemplateCharacteristicMetaDataByTemplate(new org.json.JSONArray().put(args[1])) );
			}else if("getTemplateCharacteristicMetaDataByTemplateCharacteristic".equals(args[0])) {
				System.out.println( dr.getTemplateCharacteristicMetaDataByTemplateCharacteristic(new org.json.JSONArray().put( new org.json.JSONObject().put("template", args[1]).put("characteristic", args[2]))) );
			}else if("getTemplateCharacteristicMetaDataByTemplateCharacteristicProperty".equals(args[0])) {
				System.out.println( dr.getTemplateCharacteristicMetaDataByTemplateCharacteristicProperty(new org.json.JSONArray().put( new org.json.JSONObject().put("template", args[1]).put("characteristic", args[2]).put("property", args[3]))) );
			}else if("removeGlobalMetaDataEntry".equals(args[0])) {
				System.out.println( dr.removeGlobalMetaDataEntry( new org.json.JSONArray().put( args[1] )) );
			}else if("removeGlobalMetaDataEntryByProperty".equals(args[0])) {
				System.out.println( dr.removeGlobalMetaDataEntryByProperty(new org.json.JSONArray().put( new org.json.JSONObject().put("characteristic", args[1]).put("property", args[2]))) );
			}else if("removeTemplateCharacteristicMetaData".equals(args[0])) {
				System.out.println( dr.removeTemplateCharacteristicMetaData(new org.json.JSONArray().put( args[1])) );
			}else if("removeTemplateCharacteristicMetaDataByCharacteristic".equals(args[0])) {
				System.out.println( dr.removeTemplateCharacteristicMetaDataByCharacteristic(new org.json.JSONArray().put( new org.json.JSONObject().put("template", args[1]).put("characteristic", args[2]))) );
			}else if("removeTemplateCharacteristicMetaDataByProperty".equals(args[0])) {
				System.out.println( dr.removeTemplateCharacteristicMetaDataByProperty(new org.json.JSONArray().put( new org.json.JSONObject().put("template", args[1]).put("characteristic", args[2]).put("property", args[3]))) );
			}else if("removeTemplateName".equals(args[0])) {
				System.out.println( dr.removeTemplateName(new org.json.JSONArray().put( args[1])) );
			}else if("removeCharacteristicData".equals(args[0])) {
				System.out.println( dr.removeCharacteristicData(new org.json.JSONArray().put( args[1])) );
			}else if("cargaDiccionario".equals(args[0])) {
				cargaDiccionario(args[1]);
			}else if("cargaValorDeDiccionario".equals(args[0])) {
				cargaValorDeDiccionario(args[1], args[2]);
			}else if("retiraEANProductNo".equals(args[0])) {
				System.out.println( dr.retiraEANProductNo(new org.json.JSONArray().put(args[1])) );
			}else if("retiraEANSupplierAID".equals(args[0])) {
				System.out.println( dr.retiraEANSupplierAID(new org.json.JSONArray().put(args[1])) );
			}else if("removeContenidoDeDiccionario".equals(args[0])) {
				System.out.println( dr.removeContenidoDeDiccionario(new org.json.JSONArray().put(new org.json.JSONObject().put("diccionario", args[1]).put("idValor", args[2]))) );
			} else {
				System.out.println( "No known service." );
			}
		}catch(org.json.JSONException e) {
			e.printStackTrace();
		}
		System.out.println("Done. " + new RESTWorkshop().formatTime(System.currentTimeMillis() - init));
	}
	
	public static void sendCharacteristicData() {
		DataRequestor dr = new DataRequestor();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("query", "Characteristic.IsActive = true and not Characteristic.Entities is empty and not Characteristic.DataType = \"NONE\"");
		qp.put("pageSize", "5000");
		qp.put("fields",
				   "Characteristic.Identifier"
				+ ",CharacteristicLang.Name(es)"
				+ ",Characteristic.DataType"
				+ ",Characteristic.Lookup->Lookup.Identifier"
			);
		org.json.JSONArray items = new org.json.JSONArray();
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			items.put(new org.json.JSONObject().put("characteristic", values.getString(0)).put("name", values.getString(1)).put("dataType", values.getString(2)).put("lookup", values.getString(3)) );
			if(items.length() == 5000) {
				System.out.println( "+" + items.length() + ", got: " + dr.addCharacteristicData(items) );
				while(items.length() > 0) {
					items.remove(0);
				}
			}
		});
		if(items.length() > 0) {
			System.out.println( "+" + items.length() + ", got: " + dr.addCharacteristicData(items) );
			while(items.length() > 0) {
				items.remove(0);
			}
		}
	}
	
	public static void cargaValorDeDiccionario(String diccionario, String llave) {
		DataRequestor dr = new DataRequestor();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("dictionaryProxy", "'" + diccionario + "'");
		qp.put("pageSize", "25000");
		qp.put("fields",
				   "StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
				+ ",StandardizationValue.Property->LookupValueIdentifier.Code(EUCat)"
				+ ",StandardizationValue.AlternativeValue"
				+ ",StandardizationValue.Value"
			);
		qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier = \"" + diccionario + "\" and StandardizationValue.Value = \"" + llave + "\"");
		org.json.JSONArray items = new org.json.JSONArray();
		rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			items.put(new org.json.JSONObject().put("diccionario", diccionario).put("idValor", values.getString(6)).put("structureGroup", values.getString(0)).put("characteristic", values.getString(1)).put("property", values.getString(2)).put("propertyValue", values.getString(3)).put("propertyShortCode", values.getString(4)).put("alternativeValue", values.getString(5)) );
			if(items.length() == 5000) {
				System.out.println( "+" + items.length() + ", got: " + dr.addContenidoDeDiccionario(items) );
				while(items.length() > 0) {
					items.remove(0);
				}
			}
		});
		if(items.length() > 0) {
			System.out.println( "+" + items.length() + ", got: " + dr.addContenidoDeDiccionario(items) );
			while(items.length() > 0) {
				items.remove(0);
			}
		}
	}
	
	public static void cargaDiccionario(String diccionario) {
		DataRequestor dr = new DataRequestor();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("dictionary", "'" + diccionario + "'");
		qp.put("pageSize", "25000");
		qp.put("fields",
				   "StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
				+ ",StandardizationValue.Property->LookupValueIdentifier.Code(EUCat)"
				+ ",StandardizationValue.AlternativeValue"
				+ ",StandardizationValue.Value"
			);
		org.json.JSONArray items = new org.json.JSONArray();
		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			items.put(new org.json.JSONObject().put("diccionario", diccionario).put("idValor", values.getString(6)).put("structureGroup", values.getString(0)).put("characteristic", values.getString(1)).put("property", values.getString(2)).put("propertyValue", values.getString(3)).put("propertyShortCode", values.getString(4)).put("alternativeValue", values.getString(5)) );
			if(items.length() == 5000) {
				System.out.println( "+" + items.length() + ", got: " + dr.addContenidoDeDiccionario(items) );
				while(items.length() > 0) {
					items.remove(0);
				}
			}
		});
		if(items.length() > 0) {
			System.out.println( "+" + items.length() + ", got: " + dr.addContenidoDeDiccionario(items) );
			while(items.length() > 0) {
				items.remove(0);
			}
		}
	}
	
	public static void sendTemplateNames() {
		DataRequestor dr = new DataRequestor();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("query", "StructureGroup.Identifier startsWith \"EU4-\"");
		qp.put("pageSize", "5000");
		qp.put("structure", "'PrimaryProductTaxonomy'");
		qp.put("fields",
					   "StructureGroup.Identifier"
					+ ",StructureGroupLang.Name(es)"
				);
		org.json.JSONArray items = new org.json.JSONArray();
		rw.collectData("list", "StructureGroup", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			items.put(new org.json.JSONObject().put("template", values.getString(0)).put("name", values.getString(1)) );
			if(items.length() == 5000) {
				System.out.println( "+" + items.length() + ", got: " + dr.addTemplateName(items) );
				while(items.length() > 0) {
					items.remove(0);
				}
			}
		});
		if(items.length() > 0) {
			System.out.println( "+" + items.length() + ", got: " + dr.addTemplateName(items) );
			while(items.length() > 0) {
				items.remove(0);
			}
		}
	}
	
	public static void sendGlobalMetaDataToAdmin() {
		DataRequestor dr = new DataRequestor();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("dictionary", "'GlobalTemplateAttributeConfiguration'");
		qp.put("pageSize", "5000");
		qp.put("fields",
				   "StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.Characteristic->CharacteristicLang.Name(es)"
				+ ",StandardizationValue.Property->LookupValueIdentifier.Code(EUCat)"
				+ ",StandardizationValue.PropertyValue"
				+ ",StandardizationValueLog.ModificationDate(PIM)"
				+ ",StandardizationValueLog.CreationDate(PIM)"
			);
		org.json.JSONArray items = new org.json.JSONArray();
		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			items.put(new org.json.JSONObject().put("characteristic", values.getString(0)).put("property", values.getString(2)).put("propertyValue", values.getString(3)) );
			if(items.length() == 5000) {
				System.out.println( "+" + items.length() + ", got: " + dr.addGlobalMetaData(items) );
				while(items.length() > 0) {
					items.remove(0);
				}
			}
		});
		if(items.length() > 0) {
			System.out.println( "+" + items.length() + ", got: " + dr.addGlobalMetaData(items) );
			while(items.length() > 0) {
				items.remove(0);
			}
		}
	}
	
	public static void sendTemplateCharacteristicMetaDataToAdmin() {
		DataRequestor dr = new DataRequestor();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("dictionary", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("pageSize", "5000");
		qp.put("fields",
				   "StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.Property->LookupValueIdentifier.Code(EUCat)"
				+ ",StandardizationValue.PropertyValue"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
			);
		org.json.JSONArray items = new org.json.JSONArray();
		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			items.put(new org.json.JSONObject().put("template", values.getString(0)).put("characteristic", values.getString(1)).put("property", values.getString(2)).put("propertyValue", values.getString(3)).put("creationType", values.getString(4)) );
			if(items.length() == 5000) {
				System.out.println( "+" + items.length() + ", got: " + dr.addTemplateCharacteristicMetaData(items) );
				while(items.length() > 0) {
					items.remove(0);
				}
			}
		});
		if(items.length() > 0) {
			System.out.println( "+" + items.length() + ", got: " + dr.addTemplateCharacteristicMetaData(items) );
			while(items.length() > 0) {
				items.remove(0);
			}
		}
	}
	
	public static void putExtraData() {
		long init = System.currentTimeMillis();
		System.out.println("Now requesting product data...");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('supplierShopId',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('ProductName',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('BuyerRejectionMessage',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('SupplierRejectionMessage',-1)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('SkuType')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('BWSCL',-1)->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('TImportacion')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('Negocio')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('EXTWG_S4H')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('MesdeEntregadeMercancIa')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('Temporada')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('BWVOR')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('AnoEstacion')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('TextoAdicional',-1)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('Evento',-1)->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('CostobrutoSinIVA',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('PrecioSugeridocIVA',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('Descuento1',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('Descuento2',-1)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('LABOR')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValue.LookupValue('NORMT')->LookupValueLang.Name(es)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('DescriptionLong',-1)"
				+ ",SimpleProduct2GCharacteristicValueLang.Value('DescriptionLong2',-1)"
			);
		qp.put("pageSize", "20000");
		java.util.List<String> pids = new java.util.ArrayList<>();
		org.json.JSONArray items = new org.json.JSONArray();
		DataRequestor dr = new DataRequestor();
		System.out.println("Now requesting variants data...");
		rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			pids.add(row.getJSONObject("object").getString("id"));
			org.json.JSONObject item = new org.json.JSONObject();
			item
				.put("product", values.getString(0))
				.put("supplierShopId", values.getJSONArray(1).getString(0))
				.put("ProductName", values.getJSONArray(2).getString(0))
				.put("BuyerRejectionMessage", values.getJSONArray(3).getString(0))
				.put("SupplierRejectionMessage", values.getJSONArray(4).getString(0))
				.put("SkuType", values.getJSONArray(5).getString(0))
				.put("BWSCL", values.getJSONArray(6).getString(0))
				.put("TImportacion", values.getJSONArray(7).getString(0))
				.put("Negocio", values.getJSONArray(8).getString(0))
				.put("EXTWG_S4H", values.getJSONArray(9).getString(0))
				.put("MesdeEntregadeMercancIa", values.getJSONArray(10).getString(0))
				.put("Temporada", values.getJSONArray(11).getString(0))
				.put("BWVOR", values.getJSONArray(12).getString(0))
				.put("AnoEstacion", values.getJSONArray(13).getString(0))
				.put("TextoAdicional", values.getJSONArray(14).getString(0))
				.put("Evento", values.getJSONArray(15).getString(0))
				.put("CostobrutoSinIVA", values.getJSONArray(16).getString(0))
				.put("PrecioSugeridocIVA", values.getJSONArray(17).getString(0))
				.put("Descuento1", values.getJSONArray(18).getString(0))
				.put("Descuento2", values.getJSONArray(19).getString(0))
				.put("LABOR", values.getJSONArray(20).getString(0))
				.put("NORMT", values.getJSONArray(21).getString(0))
				.put("DescriptionLong", values.getJSONArray(22).getString(0))
				.put("DescriptionLong2", values.getJSONArray(23).getString(0))
			;
			items.put(item);
			if(items.length() == 10000) {
				System.out.println( dr.putProductExtraData(items) );
				while(items.length() > 0) {
					items.remove(0);
				}
			}
		});
		if(items.length() > 0) {
			System.out.println( dr.putProductExtraData(items) );
			while(items.length() > 0) {
				items.remove(0);
			}
		}
		qp.clear();
		qp.put("fields", 
				   "Article.SupplierAID"
				+ ",SimpleArticleCharacteristicValue.LookupValue('SAPObjectType')->LookupValueLang.Name(es)"
				+ ",SimpleArticleCharacteristicValue.LookupValue('SkuType')->LookupValueLang.Name(es)"
				+ ",SimpleArticleCharacteristicValueLang.Value('CostobrutoSinIVA',-1)"
				+ ",SimpleArticleCharacteristicValueLang.Value('PrecioSugeridocIVA',-1)"
				+ ",SimpleArticleCharacteristicValueLang.Value('Descuento1',-1)"
				+ ",SimpleArticleCharacteristicValueLang.Value('Descuento2',-1)"
				+ ",SimpleArticleCharacteristicValueLang.Value('ProductName',-1)"
			);
		qp.put("pageSize", "10000");
		int a = 0;
		StringBuilder sb = new StringBuilder();
		for(String pid : pids) {
			sb.append(sb.length() == 0 ? "" : ",").append(pid);
			a++;
			if(a % 1000 == 0) {
				qp.put("products", sb.toString());
				rw.collectData("list", "Article", null, "byProducts", qp, row -> {
					org.json.JSONArray values = row.getJSONArray("values");
					org.json.JSONObject item = new org.json.JSONObject();
					item
						.put("variant", values.getString(0))
						.put("SAPObjectType", values.getJSONArray(1).getString(0))
						.put("SkuType", values.getJSONArray(2).getString(0))
						.put("CostobrutoSinIVA", values.getJSONArray(3).getString(0))
						.put("PrecioSugeridocIVA", values.getJSONArray(4).getString(0))
						.put("Descuento1", values.getJSONArray(5).getString(0))
						.put("Descuento2", values.getJSONArray(6).getString(0))
						.put("ProductName", values.getJSONArray(7).getString(0))
					;
					items.put(item);
					if(items.length() == 10000) {
						System.out.println( dr.putArticleExtraData(items) );
						while(items.length() > 0) {
							items.remove(0);
						}
					}
				});
				sb.setLength(0);
			}
		}
		if(items.length() > 0) {
			System.out.println( dr.putArticleExtraData(items) );
			while(items.length() > 0) {
				items.remove(0);
			}
		}
		System.out.println("Done. " + rw.getRw().formatTime( System.currentTimeMillis() - init ));
	}
	
	public static void refreshSKUProductNoReferences2(String id) {
		DataRequestor dr = new DataRequestor();
		org.json.JSONArray items = new org.json.JSONArray();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1),Product2G.ProductNo");
		qp.put("items", "'" + id + "'@1");
		rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String productNo = values.getString(1);
			String sku = values.getJSONArray(0).getString(0);
			if(!"".equals(sku)) {
				items.put(new org.json.JSONObject().put("productNo", productNo).put("sku", sku));
				System.out.println("Added " + productNo + " - " + sku);
				if(items.length() == 50000) {
					dr.skuProductNo( items );
					while(items.length() > 0) {
						items.remove(0);
					}
				}
			}
		});
		if(items.length() > 0) {
			dr.skuProductNo( items );
			while(items.length() > 0) {
				items.remove(0);
			}
		}
	}
	
	public static void refreshSKUProductNoReferences() {
		DataRequestor dr = new DataRequestor();
		org.json.JSONArray items = new org.json.JSONArray();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1),Product2G.ProductNo");
		qp.put("pageSize", "25000");
		rw.collectData("list", "Product2G", null, "byCatalog", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String productNo = values.getString(1);
			String sku = values.getJSONArray(0).getString(0);
			if(!"".equals(sku)) {
				items.put(new org.json.JSONObject().put("productNo", productNo).put("sku", sku));
				System.out.println("Added " + productNo + " - " + sku);
				if(items.length() == 50000) {
					dr.skuProductNo( items );
					while(items.length() > 0) {
						items.remove(0);
					}
				}
			}
		});
		if(items.length() > 0) {
			dr.skuProductNo( items );
			while(items.length() > 0) {
				items.remove(0);
			}
		}
	}
	
	public static void sendSkuProductNo(String productNo, String sku) {
		DataRequestor dr = new DataRequestor();
		org.json.JSONArray items = new org.json.JSONArray();
		if(!"".equals(sku)) {
			items.put(new org.json.JSONObject().put("productNo", productNo).put("sku", sku));
			dr.skuProductNo( items );
		}
	}
	
	public static void sendSkuProductNo(String productNo) {
		DataRequestor dr = new DataRequestor();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
		qp.put("items", "'" + productNo + "'@1");
		rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			org.json.JSONArray items = new org.json.JSONArray();
			String sku = values.getJSONArray(0).getString(0);
			System.out.println(values);
			if(!"".equals(sku)) {
				items.put(new org.json.JSONObject().put("productNo", productNo).put("sku", sku));
				dr.skuProductNo( items );
			}
		});
	}
	
	public static void refreshProductLinks(String modificationDateLower, String modificationDateUpper) {
		DataRequestor dr = new DataRequestor();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "Product2GLog.ModificationDate(PIM) >= " + modificationDateLower + ( modificationDateUpper != null ? " and Product2GLog.ModificationDate(PIM) < " + modificationDateUpper : "" ));
		qp.put("pageSize", "5000");
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			org.json.JSONArray items = makeItemsForSKUSupplierAID(values.getString(0));
			if(items.length() > 0) {
				dr.putSkuSupplierAID( items );
			}
		});
	}
	
	public static org.json.JSONArray makeItemsForSKUSupplierAID(String productNo){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID,ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
		qp.put("products", "'" + productNo + "'@1");
		org.json.JSONArray items = new org.json.JSONArray();
		rw.collectData("list", "Article", null, "byProducts", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String supplierAID = values.getString(0);
			String sku = values.getJSONArray(1).getString(0);
			items.put(new org.json.JSONObject().put("supplierAID", supplierAID).put("sku", sku).put("productNo", productNo));
		});
		return items;
	}
	
	public static void enviaDataArticulo(String supplierAid) {
		DataRequestor dr = new DataRequestor();
		org.json.JSONArray items = new org.json.JSONArray();
		org.json.JSONObject obj = new org.json.JSONObject();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
			   "Article.SupplierAID"
			+ ",ArticleCharacteristicValue.LookupValue('TamanoUnico',root,\"0000.0000.RK\",'TamanoUnico')->LookupValue.Code"
			+ ",ArticleCharacteristicValue.LookupValue('ColoursLiverpoolAtt',root,\"0000.0000.RK\",'ColoursLiverpoolAtt')->LookupValue.Code"
			+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)"
			+ ",ArticleCharacteristicValueLang.Value('AssignTakeNoTake',root,\"0000.0000.RK\",'AssignTakeNoTake',-1)"
			+ ",ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
			+ ",ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"
			+ ",ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"
		);
		qp.put("items", "'" + supplierAid + "'@1");
		rw.collectData("list", "Article", null, "byItems", qp, row->{
			System.out.println(row);
			org.json.JSONArray values = row.getJSONArray("values");
//			org.json.JSONObject obj = new org.json.JSONObject();
			obj.put("variant", values.getString(0));
			obj.put("ColoursLiverpoolAtt", values.getJSONArray(2).getString(0));
			obj.put("TamanoUnico", values.getJSONArray(1).getString(0));
			obj.put("ProductImage", values.getJSONArray(3).getString(0));
			obj.put("AssignTakeNoTake", values.getJSONArray(4).getString(0));
			obj.put("SKU", values.getJSONArray(5).getString(0));
			obj.put("MainBarCode", values.getJSONArray(6).getString(0));
			obj.put("MainBarCodeS4H", values.getJSONArray(7).getString(0));
		}, System.out::println);
		qp.put("fields", "ProductReference.ReferencedSupplierAid");
		rw.collectData("list", "Article", "ProductReference", "byItems", qp, row->{
			System.out.println(row);
			org.json.JSONArray values = row.getJSONArray("values");
//			org.json.JSONObject obj = new org.json.JSONObject();
			obj.put("ProductNo", values.getString(0));
		}, System.out::println);
		items.put(obj);
		if(items.length() > 0) {
			dr.putArticleData(items);
			while(items.length() > 0) {
				items.remove(0);
			}
		}
	}
	
	public static void enviaDataPropuesta(String productNo) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('Section',root,\"0000.0000.RK\",'Section')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('BrandName',root,\"0000.0000.RK\",'BrandName')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('SupplierID',root,\"0000.0000.RK\",'SupplierID',-1)"
				+ ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
				+ ",Product2GStructureMap.StructureGroup('PrimaryProductTaxonomy')->StructureGroup.Identifier"
				+ ",Product2G.CurrentStatus"
				+ ",Product2GCharacteristicValueLang.Value('AssignTakeNoTake',root,\"0000.0000.RK\",'AssignTakeNoTake',-1)"
				+ ",Product2GCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool',-1)->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"
				+ ",Product2GCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"
			);
		System.out.println("Going on " + productNo);
		qp.put("items", "'" + productNo + "'@1");
		DataRequestor dr = new DataRequestor();
		org.json.JSONArray items = new org.json.JSONArray();
		rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			org.json.JSONObject obj = new org.json.JSONObject();
			obj.put("product", values.getString(0));
			obj.put("Section", values.getJSONArray(1).get(0));
			obj.put("ItemGroup", values.getJSONArray(2).get(0));
			obj.put("ItemGroupS4H", values.getJSONArray(3).get(0));
			obj.put("BrandName", values.getJSONArray(4).get(0));
			obj.put("BRAND_ID_S4H", values.getJSONArray(5).get(0));
			obj.put("Business", values.getJSONArray(6).get(0));
			obj.put("SupplierID", values.getJSONArray(7).get(0));
			obj.put("SKU", values.getJSONArray(8).getString(0));
			obj.put("Template", values.getJSONArray(9).get(0));
			obj.put("CurrentStatus", values.getString(10));
			obj.put("AssignTakeNoTake", values.getJSONArray(11).get(0));
			obj.put("SAPObjectType", values.getJSONArray(12).get(0));
			obj.put("FotoTomadaLiverpool", String.valueOf( values.getJSONArray(13).get(0) ));
			obj.put("MainBarCode", values.getJSONArray(14).get(0));
			obj.put("MainBarCodeS4H", values.getJSONArray(15).get(0));
			items.put(obj);
			if(items.length() == 50000) {
				dr.putProductData(items);
				while(items.length() > 0) {
					items.remove(0);
				}
			}
		}, System.out::println);
		if(items.length() > 0) {
			dr.putProductData(items);
			while(items.length() > 0) {
				items.remove(0);
			}
		}
		
		org.json.JSONObject obj = new org.json.JSONObject();
		qp = new java.util.TreeMap<>();
		qp.put("fields", 
			   "Article.SupplierAID"
			+ ",ArticleCharacteristicValue.LookupValue('TamanoUnico',root,\"0000.0000.RK\",'TamanoUnico')->LookupValue.Code"
			+ ",ArticleCharacteristicValue.LookupValue('ColoursLiverpoolAtt',root,\"0000.0000.RK\",'ColoursLiverpoolAtt')->LookupValue.Code"
			+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)"
			+ ",ArticleCharacteristicValueLang.Value('AssignTakeNoTake',root,\"0000.0000.RK\",'AssignTakeNoTake',-1)"
			+ ",ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
			+ ",ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"
			+ ",ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"
		);
		qp.put("products", "'" + productNo + "'@1");
		rw.collectData("list", "Article", null, "byProducts", qp, row->{
			System.out.println(row);
			org.json.JSONArray values = row.getJSONArray("values");
			enviaDataArticulo(values.getString(0));
			obj.put("variant", values.getString(0));
			obj.put("ColoursLiverpoolAtt", values.getJSONArray(2).getString(0));
			obj.put("TamanoUnico", values.getJSONArray(1).getString(0));
			obj.put("ProductImage", values.getJSONArray(3).getString(0));
			obj.put("AssignTakeNoTake", values.getJSONArray(4).getString(0));
			obj.put("SKU", values.getJSONArray(5).getString(0));
			obj.put("MainBarCode", values.getJSONArray(6).getString(0));
			obj.put("MainBarCodeS4H", values.getJSONArray(7).getString(0));
		}, System.out::println);
		qp.put("fields", "ProductReference.ReferencedSupplierAid");
		rw.collectData("list", "Article", "ProductReference", "byProducts", qp, row->{
			System.out.println(row);
			org.json.JSONArray values = row.getJSONArray("values");
			obj.put("ProductNo", values.getString(0));
		}, System.out::println);
		items.put(obj);
		if(items.length() > 0) {
			dr.putArticleData(items);
			while(items.length() > 0) {
				items.remove(0);
			}
		}

	}
	
	public static void enviaDataProducto(String productNo) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("pageSize", "20000");
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('Section',root,\"0000.0000.RK\",'Section')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('BrandName',root,\"0000.0000.RK\",'BrandName')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('SupplierID',root,\"0000.0000.RK\",'SupplierID',-1)"
				+ ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
				+ ",Product2GStructureMap.StructureGroup('PrimaryProductTaxonomy')->StructureGroup.Identifier"
				+ ",Product2G.CurrentStatus"
				+ ",Product2GCharacteristicValueLang.Value('AssignTakeNoTake',root,\"0000.0000.RK\",'AssignTakeNoTake',-1)"
				+ ",Product2GCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool',-1)->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"
				+ ",Product2GCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"
			);
		qp.put("items", "'" + productNo + "'@1");
		DataRequestor dr = new DataRequestor();
		org.json.JSONArray items = new org.json.JSONArray();
		rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			org.json.JSONObject obj = new org.json.JSONObject();
			obj.put("product", values.getString(0));
			obj.put("Section", values.getJSONArray(1).get(0));
			obj.put("ItemGroup", values.getJSONArray(2).get(0));
			obj.put("ItemGroupS4H", values.getJSONArray(3).get(0));
			obj.put("BrandName", values.getJSONArray(4).get(0));
			obj.put("BRAND_ID_S4H", values.getJSONArray(5).get(0));
			obj.put("Business", values.getJSONArray(6).get(0));
			obj.put("SupplierID", values.getJSONArray(7).get(0));
			obj.put("SKU", values.getJSONArray(8).getString(0));
			obj.put("Template", values.getJSONArray(9).get(0));
			obj.put("CurrentStatus", values.getString(10));
			obj.put("AssignTakeNoTake", values.getJSONArray(11).get(0));
			obj.put("SAPObjectType", values.getJSONArray(12).get(0));
			obj.put("FotoTomadaLiverpool", String.valueOf( values.getJSONArray(13).get(0) ));
			obj.put("MainBarCode", values.getJSONArray(14).get(0));
			obj.put("MainBarCodeS4H", values.getJSONArray(15).get(0));
			items.put(obj);
			if(items.length() == 50000) {
				dr.putProductData(items);
				while(items.length() > 0) {
					items.remove(0);
				}
			}
		}, System.out::println);
		if(items.length() > 0) {
			dr.putProductData(items);
			while(items.length() > 0) {
				items.remove(0);
			}
		}
	}
	
	public static void help() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("pageSize", "20000");
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('Section',root,\"0000.0000.RK\",'Section')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('BrandName',root,\"0000.0000.RK\",'BrandName')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('SupplierID',root,\"0000.0000.RK\",'SupplierID',-1)"
				+ ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
				+ ",Product2GStructureMap.StructureGroup('PrimaryProductTaxonomy')->StructureGroup.Identifier"
				+ ",Product2G.CurrentStatus"
				+ ",Product2GCharacteristicValueLang.Value('AssignTakeNoTake',root,\"0000.0000.RK\",'AssignTakeNoTake',-1)"
				+ ",Product2GCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool',-1)->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"
				+ ",Product2GCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"
			);
//		qp.put("query", 
//				"Product2G.ProductNo startsWith \"175461166\""
//				+ " and Product2G.CurrentStatus = \"Creación de SKU\""
//				+ " and characteristic('Business') = 'MKP'@'BusinessQualified'"
//			);

		qp.put("query", "Product2G.CurrentStatus = \"Creación de SKU\" and characteristic('SKU') is empty and characteristic('Business') = 'MKP'@'BusinessQualified' and Product2GLog.CreationDate(PIM) >= 2026-01-28T00:00:00");
		DataRequestor dr = new DataRequestor();
		org.json.JSONArray items = new org.json.JSONArray();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			org.json.JSONObject obj = new org.json.JSONObject();
			obj.put("product", values.getString(0));
			obj.put("Section", values.getJSONArray(1).get(0));
			obj.put("ItemGroup", values.getJSONArray(2).get(0));
			obj.put("ItemGroupS4H", values.getJSONArray(3).get(0));
			obj.put("BrandName", values.getJSONArray(4).get(0));
			obj.put("BRAND_ID_S4H", values.getJSONArray(5).get(0));
			obj.put("Business", values.getJSONArray(6).get(0));
			obj.put("SupplierID", values.getJSONArray(7).get(0));
			obj.put("SKU", values.getJSONArray(8).getString(0));
			obj.put("Template", values.getJSONArray(9).get(0));
			obj.put("CurrentStatus", values.getString(10));
			obj.put("AssignTakeNoTake", values.getJSONArray(11).get(0));
			obj.put("SAPObjectType", values.getJSONArray(12).get(0));
			obj.put("FotoTomadaLiverpool", String.valueOf( values.getJSONArray(13).get(0) ));
			obj.put("MainBarCode", values.getJSONArray(14).get(0));
			obj.put("MainBarCodeS4H", values.getJSONArray(15).get(0));
			items.put(obj);
			if(items.length() == 50000) {
				dr.putProductData(items);
				while(items.length() > 0) {
					items.remove(0);
				}
			}
		}, System.out::println);
		if(items.length() > 0) {
			dr.putProductData(items);
			while(items.length() > 0) {
				items.remove(0);
			}
		}
	
	}
}
