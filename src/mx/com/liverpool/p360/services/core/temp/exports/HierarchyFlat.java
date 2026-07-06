package mx.com.liverpool.p360.services.core.temp.exports;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class HierarchyFlat {

	private static final RESTWorkshop rw = new RESTWorkshop();

	public static void main(String[] args) {
		long init = System.currentTimeMillis();
        String delim = "\"";
        String sep = ";";
        String esc = "\\";
        String sgid = "EU4-28753232";
        String structure = "PrimaryProductTaxonomy";
        java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("structure", structure);
		qp.put("fields", "StructureGroup.Identifier,StructureGroupLang.Name(es),StructureGroup.ParentIdentifier");
		qp.put("query", "StructureGroup.Identifier equals \"" + sgid + "\"");
    	org.json.JSONObject response = null;
    	org.json.JSONArray rows = null;
    	org.json.JSONArray values = null;
    	response = rw.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
    	if(response != null) {
    		rows = response.getJSONArray("rows");
            try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("C:\\opt\\LVP\\tmp\\pelépele.csv")))){
            	pw.println( "hierarchy-code;hierarchy-label;hierarchy-parent-code;update-delete" );
            	for(int i=0; i<rows.length(); i++) {
            		values = rows.getJSONObject(i).getJSONArray("values");
            		pw.println( rw.serializeChunk( new String[] { values.getString(0), values.getString(1), values.getString(2), "update" }, delim, sep, esc ) );
            	}
            }catch(java.io.IOException e) {
            	e.printStackTrace();
            }
    	}else {
    		System.out.println("ERR: " + rw.getRawResponse());
    	}
		System.out.println("Done. " + new RESTWorkshop().formatTime(System.currentTimeMillis() - init));
	}

}
