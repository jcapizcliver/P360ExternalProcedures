package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class DropMe {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier,Characteristic.Entities");
		qp.put("query", "Characteristic.ParentCharacteristic is empty and not Characteristic.DataType = \"NONE\"");
		qp.put("pageSize", "10000");
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> {

			org.json.JSONArray entities = row.getJSONArray("values").getJSONArray(1);
			java.util.LinkedList<String> entitiesList = new java.util.LinkedList<>();
			for(int i=0; i<entities.length(); i++) {
				entitiesList.addLast(entities.getString(i));
			}
			System.out.println( rw.getRw().serializeChunk( new String[] { row.getJSONArray("values").getString(0), rw.getRw().serializeChunk( entitiesList.toArray(new String[] {}) ) }, "\"", ";", "\\" ) );
		
		});
	}
	
}
