package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class ProductTypeMasivoALB {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		doIt();
	}
	
	private static java.util.Map<String, String> collectLookup(String dictionary) {
		java.util.Map<String, String> map = new java.util.HashMap<>();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("dictionary", "'" + dictionary + "'");
		qp.put("pageSize", "2000");
		qp.put("fields", "StandardizationValue.Value,StandardizationValue.AlternativeValue");
		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			map.put(values.getString(0), values.getString(1));
		});
		return map;
	}
	
	private static void doIt() {
		java.util.Map<String, String> sapBEHVO = collectLookup("BEHVO_LookupTable");
		java.util.Map<String, String> igVsEnvase = collectLookup("GpoArtVsEnvase");
		java.util.Map<String, String> igVsEnvaseS4H = collectLookup("GpoArtVsEnvase_S4H");
		StringBuilder sb = new StringBuilder();
		for(String a : productos) {
			sb.append(sb.length() == 0 ? "" : ",").append("'").append(a).append("'@1");
		}
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields",
				   "Product2G.ProductNo"
			    + ",Product2G.Business->LookupValue.Code"
				+ ",Product2GExtraData.ItemGroup(MX)->LookupValue.Code"
				+ ",Product2GExtraData.ItemGroupS4H(MX)->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('SAP_BEHVO',root,\"0000.0000.RK\",'SAP_BEHVO')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('SkuType',root,\"0000.0000.RK\",'SkuType')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('MTART_S4H',root,\"0000.0000.RK\",'MTART_S4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('AlmacenamientoAtt',root,\"0000.0000.RK\",'AlmacenamientoAtt')->LookupValue.Code"
			);
		qp.put("items", sb.toString());
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rh = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ProductType',root,\"0000.0000.RK\",'ProductType',-1)")) , 1000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println
				));
		java.util.Set<String> itemGroups = new java.util.TreeSet<>();
		java.util.Set<String> itemGroupsS4H = new java.util.TreeSet<>();
		java.util.List<String> noItemGroup = new java.util.ArrayList<>();
		rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			boolean prendeles = false;
			if("S33246101".equals(values.getString(0))) {
				System.out.println(values);
				prendeles = true;
			}
			if("SBB".equals(values.getString(1))) {
				String productType = "1";
				String sapBehvo = values.getJSONArray(4).getString(0);
				String itemGroup = values.getString(3);
				if(!"".equals(itemGroup))
					itemGroupsS4H.add(itemGroup);
				else {
					noItemGroup.add(values.getString(0));
				}
//				if("".equals(sapBehvo)) {
					sapBehvo = igVsEnvaseS4H.get(itemGroup);
//				}
				if(!"".equals(sapBehvo) && sapBehvo != null) {
					productType = sapBEHVO.get(sapBehvo.substring(0, 2));
				}
				if("DIEN".equals(values.getJSONArray(6).getString(0)) && "SB87516".equals(itemGroup)) {
					productType = "6";
				}
				rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(productType)));
			}else {
				String productType = "1";
				String sapBehvo = values.getJSONArray(4).getString(0);
				String itemGroup = values.getString(2);
				if(!"".equals(itemGroup))
					itemGroups.add(itemGroup);
				else {
					noItemGroup.add(values.getString(0));
				}
				if(prendeles) {
					System.out.println("-->" + itemGroup);
					System.out.println("-->" + sapBehvo);
				}
//				if("".equals(sapBehvo)) {
					sapBehvo = igVsEnvase.get(itemGroup);
					if(prendeles)
						System.out.println("22-->" + sapBehvo);
//				}
				if(!"".equals(sapBehvo) && sapBehvo != null) {
					productType = sapBEHVO.get(sapBehvo.substring(0, 2));
					if(prendeles)
						System.out.println("33-->" + productType);
				}
				if("0001".equals(values.getJSONArray(7).getString(0)) && "SERV".equals(values.getJSONArray(5).getString(0))) {
					productType = "6";
					if(prendeles)
						System.out.println("44-->" + productType);
				}
				if(prendeles) {
					System.out.println("---->" + new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(productType)));
//					System.exit(0);
				}
				rh.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put(productType)));
			}
		});
		rh.sendData();
		System.out.println("**** ItemGroup ****");
		itemGroups.forEach(System.out::println);
		System.out.println("**** ItemGroupS4H ****");
		itemGroupsS4H.forEach(System.out::println);
		
		System.out.println("** NO ITEMGROUP **");
		noItemGroup.forEach(System.out::println);
	}
	
	private static final String[] productos = (
				  "S88777401\r\n"
				+ "S87813382\r\n"
				+ "S87811931\r\n"
				+ "S82989126\r\n"
				+ "S83012202\r\n"
				+ "S83005206\r\n"
				+ "S89949348\r\n"
				+ "S33881791\r\n"
				+ "S84544354\r\n"
				+ "S86103938\r\n"
				+ "S83457167\r\n"
				+ "S66170359\r\n"
				+ "S85566209\r\n"
				+ "LVP1189322174\r\n"
				+ "LVP1187620658\r\n"
				+ "S35776930\r\n"
				+ "S16462783\r\n"
				+ "S84728686\r\n"
				+ "S83445187\r\n"
				+ "S33246101\r\n"
				+ "S20279964\r\n"
				+ "S63848998\r\n"
				+ "S20240469\r\n"
				+ "S16627579\r\n"
				+ "S82679349\r\n"
				+ "S21482396\r\n"
				+ "S21482399\r\n"
				+ "LVP1148428227\r\n"
				+ "LVP1180959251\r\n"
				+ "LVP1180960496\r\n"
				+ "S96274448\r\n"
				+ "S95394111\r\n"
				+ "S94927322\r\n"
				+ "LVP1199469248\r\n"
				+ "1754611674286576\r\n"
				+ "LVP1199467091"
			).split("\\r\\n");
}
