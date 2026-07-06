package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class PreguntaPorCaracteristicas {

	
	public static void main(String[] args) {
		org.json.JSONObject data = new org.json.JSONObject(RAW_MESSAGE);
		RESTWrapper rw = new RESTWrapper();
		org.json.JSONArray characteristics = data.getJSONArray("_characteristicRecords");
		org.json.JSONObject characteristic = null;
		StringBuilder sb = new StringBuilder();
		for(int i=0; i< characteristics.length(); i++) {
			characteristic = characteristics.getJSONObject(i);
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append("'");
			sb.append(characteristic.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"));
			sb.append("'");
		}
		System.out.println("Collected: " + characteristics.length());
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("items", sb.toString());
		qp.put("fields", "Characteristic.Identifier,Characteristic.IsActive,Characteristic.Category->LookupValue.Code");
		qp.put("pageSize", "1000");
		rw.collectData("list", "Characteristic", null, "byItems", qp, row -> {
			if(!Boolean.parseBoolean(row.getJSONArray("values").getString(1))) {
				System.out.println(row.getJSONArray("values").getString(0) + ", " + row.getJSONArray("values").getString(2));
			}
		});
		
		qp.put("query", "Characteristic.Entities contains \"Item\" and Characteristic.IsActive = false");
		qp.remove("items");
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> System.out.println(row.getJSONArray("values")));
		
	}
	
	private static final String RAW_MESSAGE = "{\"_characteristicRecords\":[{\"_datatype\":\"LOOKUP\",\"_recordLang\":[{\"values\":[{\"_code\":\"MX\",\"_label\":\"México\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"WHERL\"}}},{\"_datatype\":\"LOOKUP\",\"_recordLang\":[{\"values\":[{\"_code\":\"1501\",\"_label\":\"Puente\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"ProductTypeSAP\"}}},{\"_recordLang\":[{\"values\":[\"false\"]}],\"_qualification\":{\"characteristic\":{\"_code\":\"Consignacion\"}}},{\"_datatype\":\"LOOKUP\",\"_recordLang\":[{\"values\":[{\"_code\":\"N\",\"_label\":\"Fotos las carga proveedor\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"FotoTomadaLiverpool\"}}},{\"_recordLang\":[{\"values\":[\"false\"]}],\"_qualification\":{\"characteristic\":{\"_code\":\"IsDuttyFree\"}}},{\"_datatype\":\"LOOKUP\",\"_recordLang\":[{\"values\":[{\"_code\":\"55709\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"ItemGroup\"}}},{\"_datatype\":\"LOOKUP\",\"_recordLang\":[{\"values\":[{\"_code\":\"55709\",\"_label\":\"55709 - PANTALON\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"ProductTypeSAPTEMP\"}}},{\"_datatype\":\"LOOKUP\",\"_recordLang\":[{\"values\":[{\"_code\":\"0\",\"_label\":\"False\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"exclusiveDiscount\"}}},{\"_recordLang\":[{\"values\":[\"Modelo\"]}],\"_qualification\":{\"characteristic\":{\"_code\":\"SupplierPartNumber\"}}},{\"_datatype\":\"LOOKUP\",\"_recordLang\":[{\"values\":[{\"_code\":\"004\",\"_label\":\"Abril\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"MesdeEntregadeMercancIa\"}}},{\"_datatype\":\"LOOKUP\",\"_recordLang\":[{\"values\":[{\"_code\":\"LVP\",\"_label\":\"Liverpool\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"Business\"}}},{\"_datatype\":\"LOOKUP\",\"_recordLang\":[{\"values\":[{\"_code\":\"E2\",\"_label\":\"I.V.A. 16 %\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"IndicadordeImpuesto\"}}},{\"_recordLang\":[{\"values\":[\"1500.00\"]}],\"_qualification\":{\"characteristic\":{\"_code\":\"CostoNetoSinIVA\"}}},{\"_datatype\":\"LOOKUP\",\"_recordLang\":[{\"values\":[{\"_code\":\"MXP\",\"_label\":\"Pesos\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"Currency\"}}},{\"_recordLang\":[{\"values\":[\"1500\"]}],\"_qualification\":{\"characteristic\":{\"_code\":\"CostobrutoSinIVA\"}}},{\"_recordLang\":[{\"values\":[\"180000\"]}],\"_qualification\":{\"characteristic\":{\"_code\":\"PrecioSugeridocIVA\"}}},{\"_datatype\":\"LOOKUP\",\"_recordLang\":[{\"values\":[{\"_label\":\"Liverpool\"}]}],\"_qualification\":{\"characteristic\":{\"_code\":\"Business\"}}},{\"_recordLang\":[{\"values\":[\"2290\"],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"SupplierID\"}}},{\"_recordLang\":[{\"values\":[{\"_code\":\"01\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"Status\"}}},{\"_recordLang\":[{\"values\":[{\"_code\":\"N\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"TImportacion\"}}},{\"_recordLang\":[{\"values\":[{\"_code\":\"LIVERPOOL\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"IdentificaNegocio\"}}},{\"_recordLang\":[{\"values\":[{\"_code\":\"IE\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"TypeMainBarCode\"}}},{\"_recordLang\":[{\"values\":[{\"_code\":\"REGULAR\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"Negocio\"}}},{\"_recordLang\":[{\"values\":[\"2026\"],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"AnoEstacion\"}}},{\"_recordLang\":[{\"values\":[{\"_code\":\"0003\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"Temporada\"}}},{\"_recordLang\":[{\"values\":[{\"_code\":\"SL\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"SAP_BEHVO\"}}},{\"_recordLang\":[{\"values\":[{\"_code\":\"1\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"ProductType\"}}},{\"_recordLang\":[{\"values\":[{\"_code\":\"5\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"Direction\"}}},{\"_recordLang\":[{\"values\":[{\"_code\":\"557\"}],\"_qualification\":{\"language\":{\"_code\":\"zxx\"}}}],\"_qualification\":{\"characteristic\":{\"_code\":\"Section\"}}}],\"currentStatus\":{\"_code\":\"10031\"},\"structureGroupMap\":[{\"_qualification\":{\"structureGroup\":{\"_externalId\":\"'EU4-27320066'@'PrimaryProductTaxonomy'\"}}}],\"lasModificationUserEmail\":\"\",\"externalStatus\":{\"_code\":\"Borrador\"}}";
	
}
