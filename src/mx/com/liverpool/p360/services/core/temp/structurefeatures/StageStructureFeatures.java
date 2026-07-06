package mx.com.liverpool.p360.services.core.temp.structurefeatures;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.temp.move.utils.GeneralOperations;

public class StageStructureFeatures {
	
	private final RESTWorkshop rw = new RESTWorkshop(true, PropertiesManager.get("p360.contingency.base_url"), "Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
	
	public static void main(String[] args) {
		StageStructureFeatures ssf = new StageStructureFeatures();
		GeneralOperations go = new GeneralOperations();
		java.util.Map<String, org.json.JSONObject> data = go.collectStructureGroupAttributes(ssf.rw, "PrimaryProductTaxonomy");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(PropertiesManager.get("p360.contingency.create_proposal.structure_group_attribute_name_guide"))))){
			final String[] pieces = new String[4];
			data.forEach((k,v)->{
				pieces[0] = k;
				pieces[1] = v.has("NameGuide") ? v.getString("NameGuide") : "";
				pieces[2] = v.has("OrderOfAtributesForName") ? v.getString("OrderOfAtributesForName") : "";
				pieces[3] = v.getString("_templateNameEs");
				if(!"".equals(pieces[1]) || !"".equals(pieces[2])) {
					pw.println(ssf.rw.serializeChunk(pieces));
				}
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

}
