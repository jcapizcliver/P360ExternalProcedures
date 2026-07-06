package mx.com.liverpool.p360.services.core.temp.exports;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class AttributeGroupToATG {

	private static RESTWorkshop workshop = new RESTWorkshop();
	
	public static void main(String[] args) {
		String[] gruposParaElAtg = (
				  "CategorySpecificAttributesLVP\r\n"
				+ "CategorySpecificAttributesSAP\r\n"
				+ "SAP_Attributes\r\n"
				+ "VariantsSpecificAttributes\r\n"
				+ "SalesItemMarketingDescriptions\r\n"
				+ "ATG_Attributes\r\n"
				+ "ConjuntoLookMaintenance\r\n"
				+ "ConjuntoLookMetadata").split("\\r\\n");
		StringBuilder sb = new StringBuilder();
		for(int a = 0; a<gruposParaElAtg.length; a++ ) {
			sb.append(a == 0 ? "" : ",");
			sb.append("\"");
			sb.append(gruposParaElAtg[a]);
			sb.append("\"");
		}
		java.util.Set<String> atgs = new java.util.TreeSet<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "Characteristics");
		qp.put("fields", "LookupValue.Code");
		qp.put("query", "LookupValueReference.LookupValues('AttributeGroup')->LookupValue.Code in (" + sb.toString() + ")");
		qp.put("pageSize", "900");
		int ci = 0;
		int tz = 0;
		org.json.JSONObject responsi = null;
		org.json.JSONArray rowsi = null;
		org.json.JSONArray valsi = null;
		do {
			qp.put("startIndex", String.valueOf(ci));
			responsi = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			if(responsi != null) {
				rowsi = responsi.getJSONArray("rows");
				for(int a = 0; a<rowsi.length(); a++) {
					ci++;
					valsi = rowsi.getJSONObject(a).getJSONArray("values");
					atgs.add(valsi.getString(0));
				}
			}
		}while(ci < tz);
		ci = 0;
		java.util.Set<String> losDelAtg = new java.util.TreeSet<>(java.util.Arrays.asList(gruposParaElAtg));
	}
}
