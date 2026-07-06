package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class ListCharacteristics {

	public static void main(String[] args) {
		
		ListCharacteristics l = new ListCharacteristics();
		l.getMeCharacteristics();
		
	}
	
	private void getMeCharacteristics() {
		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		String[] pieces = new String[5];
		int totalSize = 0;
		int currentIndex = 0;
		qp.put("query", "Characteristic.ParentCharacteristic is empty and Characteristic.IsActive = true");
		qp.put("fields", 
				   "Characteristic.Identifier"
				+ ",CharacteristicLang.Name(es)"
				+ ",CharacteristicLang.Description(es)"
				+ ",Characteristic.DataType"
				+ ",Characteristic.Lookup->Lookup.Identifier"
			);
		qp.put("pageSize", "1200");
		System.out.println("Processing...");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("C:\\opt\\LVP\\tmp\\characteristics_p360.csv")))){
			do {
				qp.put("startIndex", String.valueOf(currentIndex));
				response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
				if(response != null) {
					totalSize = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						values = rows.getJSONObject(i).getJSONArray("values");
						for(int j=0; j<pieces.length; j++) {
							pieces[j] = values.getString(j);
						}
						 pw.println( rw.serializeChunk(pieces) );
					}
					currentIndex += response.getInt("pageSize");
				}
				System.out.println(currentIndex + "/" + totalSize);
			}while(currentIndex < totalSize);
			currentIndex = 0;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
}
