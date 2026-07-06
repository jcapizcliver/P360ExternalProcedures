package mx.com.liverpool.p360.services.core.temp.structurefeatures;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.temp.move.utils.GeneralOperations;

public class UpdateStructureFeaturesToAllAttributeType {

	private final RESTWorkshop rw = new RESTWorkshop(true, PropertiesManager.get("p360.contingency.base_url"), "Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));

	public static void main(String[] args) {
		UpdateStructureFeaturesToAllAttributeType u = new UpdateStructureFeaturesToAllAttributeType();
		u.updateData();
	}
	
	private void updateData() {
		String structureGroupAttribute = "OrderOfAtributesForName";
		GeneralOperations go = new GeneralOperations();
		java.util.Map<String, org.json.JSONObject> data = go.collectStructureGroupAttributes(rw, "PrimaryProductTaxonomy");
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		columns.put(new org.json.JSONObject().put("identifier", "StructureGroupAttribute.Type(" + structureGroupAttribute + ")"));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		data.forEach((k,v)->{
			if(v.has(structureGroupAttribute)) {
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + k + "'@'PrimaryProductTaxonomy'")).put("values", new org.json.JSONArray().put(0)));
				if(rows.length() == 500) {
					rw.makeRequest("POST", "/list/StructureGroup", qp, request.toString());
					System.out.println(rw.getRawResponse());
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
		});
		if(rows.length() > 0) {
			rw.makeRequest("POST", "/list/StructureGroup", qp, request.toString());
			System.out.println(rw.getRawResponse());
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
	}
	
}
