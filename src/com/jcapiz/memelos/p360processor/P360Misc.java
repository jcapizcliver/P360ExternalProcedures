package com.jcapiz.memelos.p360processor;

import com.jcapiz.memelos.misc.RestClient;

public class P360Misc {

	private RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic cmVzdDpoZWlsZXI=");

	public java.util.Map<String, org.json.JSONObject> collectCharacteristics() throws Exception{
		java.util.Map<String, org.json.JSONObject> characteristics = new java.util.TreeMap<>();
		int currentIndex = 0;
		String url = null;
		int totalSize = 0;
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONObject content = null;
		org.json.JSONArray rows = null;
		org.json.JSONObject row = null;
		String charId = null;
		/***
			Identifier
			DataType
			Lookup->Lookup.Identifier
			Lookup->LookupLang.Name(10)
			Lookup->LookupLang.Name(9)
			Category->LookupValue.Code
			Category->LookupValueLang.Name(10)
			Category->LookupValueLang.Name(9)
			RootCharacteristic
			ParentCharacteristic
			Entities
			IsActive
		 ***************************************/
		do {
			url = "https://webctep360dev.liverpool.com.mx/rest/V1.0/list/Characteristic/bySearch?query=not%20Characteristic.Identifier%20is%20empty&metaData=true&fields=Characteristic.Identifier,Characteristic.DataType,Characteristic.Lookup-%3ELookup.Identifier,Characteristic.Lookup-%3ELookupLang.Name(es),Characteristic.Lookup-%3ELookupLang.Name(en),Characteristic.Category-%3ELookupValue.Code,Characteristic.Category-%3ELookupValueLang.Name(es),Characteristic.Category-%3ELookupValueLang.Name(en),Characteristic.RootCharacteristic,Characteristic.ParentCharacteristic,Characteristic.Entities,Characteristic.IsActive&pageSize=1000&startIndex=" + currentIndex;
			rawResponse = rc.getRequest("GET", url, null);
			response = new org.json.JSONObject(rawResponse);
			if(!response.has("rows")) {
				System.out.println(rawResponse);
			}
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				row = rows.getJSONObject(i);
				charId = row.getJSONArray("values").getString(0);
				content = new org.json.JSONObject();
				content.put("DataType", row.getJSONArray("values").getString(1));
				content.put("LookupIdentifier", row.getJSONArray("values").getString(2));
				content.put("LookupNameEs", row.getJSONArray("values").getString(3));
				content.put("LookupNameEn", row.getJSONArray("values").getString(4));
				content.put("CategoryIdentifier", row.getJSONArray("values").getString(5));
				content.put("CategoryNameEs", row.getJSONArray("values").getString(6));
				content.put("CategoryNameEn", row.getJSONArray("values").getString(7));
				content.put("RootCharacteristic", String.valueOf( row.getJSONArray("values").get(8) ));
				content.put("ParentCharacteristic", String.valueOf( row.getJSONArray("values").get(9) ));
				content.put("Entities", String.valueOf( row.getJSONArray("values").get(10) ));
				content.put("IsActive", row.getJSONArray("values").getString(11));
				characteristics.put(charId, content);
				currentIndex++;
			}
			totalSize = response.getInt("totalSize");
		}while(currentIndex < totalSize);
		return characteristics;
	}

	public String formatMillis(long millis){
	  	int days = (int)(millis/(1000*60*60*24));
	 	millis -= days*1000*60*60*24;
	  	int hours = (int) (millis/(1000*60*60));
	  	millis -= hours*1000*60*60;
	  	int minutes = (int) (millis/(1000*60));
	  	millis -= minutes*1000*60;
	  	int seconds = (int) (millis/1000);
	  	millis -= seconds*1000;
	  	return
	  		    (days < 10 ? "0" : "") + days + ":"
  		+ (hours < 10 ? "0" : "") + hours + ":"
  		+ (minutes < 10 ? "0" : "") + minutes + ":"
  		+ (seconds < 10 ? "0" : "") + seconds
  		+ "." + millis;
	}
}
