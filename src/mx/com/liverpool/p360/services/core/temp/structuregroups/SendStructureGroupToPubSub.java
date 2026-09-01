package mx.com.liverpool.p360.services.core.temp.structuregroups;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SendStructureGroupToPubSub extends RESTWrapper {

	private static final String sa = PropertiesManager.get("p360.contingency.gcp.service_account_back");
	private static final String pubSubProject = PropertiesManager.get("p360.contingency.gcp.project_back");
	
	public static void main(String[] args) {
		System.out.println(sa);
		System.out.println(pubSubProject);
		java.util.List<String> list = new java.util.ArrayList<>( java.util.Arrays.asList(("EU4-27315947").split("\\r\\n")) );
		SendStructureGroupToPubSub s = new SendStructureGroupToPubSub();
		RESTWrapper rw = new RESTWrapper();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StructureGroup.Identifier");
		qp.put("structure", "PrimaryProductTaxonomy");
		qp.put("pageSize", "10000");
		rw.collectData("list", "StructureGroup", null, "byStructure", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			String id = values.getString(0);
//			if(list.contains(id))
				s.sendDataToPubSub(id, "idmc_put_template");
		});
	}

	public String sendDataToPubSub( String structureGroupId, String topic ) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "StructureGroupLang.Name(es)"
				+ ",StructureGroupLang.Description(es)"
				+ ",StructureGroupLang.Synonym(es)"
				+ ",StructureGroupAttributeValue.Value(NameExceptions,esl,DEFAULT)"
				+ ",StructureGroupAttributeValue.Value(NameGuide,esl,DEFAULT)"
				+ ",StructureGroupAttributeValue.Value(DisplayGroupOrder,esl,DEFAULT)"
		);
		qp.put("query", "StructureGroup.Identifier = \"" + structureGroupId + "\"");
		qp.put("structure", "PrimaryProductTaxonomy");
		org.json.JSONArray items = new org.json.JSONArray();
		collectData("list", "StructureGroup", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			items.put( new org.json.JSONObject()
					.put("identifier", structureGroupId)
					.put("name", values.getString(0) + " (" + structureGroupId + ")")
					.put("description", values.getString(1))
					.put("nameExceptions", values.getString(3))
					.put("nameGuide", values.getString(4))
					.put("displayGroupOrder", values.getString(5))
					.put("products", new org.json.JSONArray())
					.put("keywords", values.getJSONArray(2))
					.put("itemsGroup", new org.json.JSONArray()) 
				);
		});
		org.json.JSONObject msg =  new org.json.JSONObject().put("templates", items);
		new PubSubGCP()
			.publishMessage(pubSubProject, topic, sa, msg.toString());
		return msg.toString();
				/*
		 	new org.json.JSONObject()
					.put("identifier", template)
					.put("name", node.getName() + " (" + node.getId()+ ")")
					.put("description", templateDescription == null || !templateDescription.isEmpty() ? "" : templateDescription)
					.put("nameExceptions", nameExceptions == null ? "" : nameExceptions)
					.put("nameGuide", nameGuide == null ? "" : nameGuide)
					.put("displayGroupOrder", displayGroupOrder != null ? displayGroupOrder : "")
					.put("products", new org.json.JSONArray())
					.put("keywords", keyWords)
					.put("itemsGroup", new org.json.JSONArray()) 
		 */
	}
	
}
