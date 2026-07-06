package mx.com.liverpool.p360.services.core.temp;

import com.jcapiz.memelos.misc.RestClient;

public class QuitaleDuplicadosAQAS {

	private static final String baseUrlQA = "https://webctep360qas.liverpool.com.mx/rest/V2.0";
	private static final String encoded = "cmVzdDpoZWlsZXI=";
	private static RestClient rc = new RestClient("Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + encoded);

	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int tz = 0;
		int currentIndex = 0;
		StringBuilder sb = new StringBuilder();
		java.util.LinkedList<String> templateIds = new java.util.LinkedList<>();
//		try {
//			do {
//				rawResponse = rc.getRequest("GET", baseUrlQA + "/list/LookupValue/byLookup?"
//						+ "lookup=" + java.net.URLEncoder.encode("PPH_L4_Templates", "UTF-8") +
//						  "&fields=" + java.net.URLEncoder.encode(
//								  "LookupValue.Code", "UTF-8")
//						+ "&pageSize=500&startIndex=" + currentIndex
//						  , null);
//				response = new org.json.JSONObject(rawResponse);
//				tz = response.getInt("totalSize");
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					currentIndex++;
//					values = rows.getJSONObject(i).getJSONArray("values");
//					templateIds.add(values.getString(0));
//				}
//			}while(currentIndex < tz);
//			currentIndex = 0;
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		templateIds.add("EU4-113578");
		String currentId = null;
		String currentCharacteristic = null;
		String currentCreationType = null;
		String currentProperty = null;

		String prevCharacteristic = null;
		String prevCreationType = null;
		String prevProperty = null;
		java.util.LinkedList<String> wasteBasket = new java.util.LinkedList<>();
		java.util.LinkedList<String> ids = new java.util.LinkedList<>();
		java.util.LinkedList<String> emptyBoys = new java.util.LinkedList<>();
		try {
			for(String templateId : templateIds) {
				System.out.println("Processing template: " + templateId);
				do {
					rawResponse = rc.getRequest("GET", baseUrlQA + "/list/StandardizationValue/byDictionary?"
							+ "dictionary=" + java.net.URLEncoder.encode("ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla", "UTF-8") +
							  "&query=" + java.net.URLEncoder.encode("StandardizationValue.StructureGroup equals \"" + templateId + "\"", "UTF-8") +
							  "&fields=" + java.net.URLEncoder.encode(
									  "StandardizationValue.StructureGroup->LookupValue.Code," +
									  "StandardizationValue.Characteristic->Characteristic.Identifier,"+
									  "StandardizationValue.CreationType->LookupValue.Code,"
									  + "StandardizationValue.Property->LookupValue.Code", "UTF-8")
							  + "&orderBy=" + java.net.URLEncoder.encode("1-ASC,2-ASC,3-ASC", "UTF-8")
							  + "&pageSize=500&startIndex=" + currentIndex, null);
					response = new org.json.JSONObject(rawResponse);
					tz = response.getInt("totalSize");
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						currentIndex++;
						values = rows.getJSONObject(i).getJSONArray("values");
						currentId = rows.getJSONObject(i).getJSONObject("object").getString("id");
						currentCharacteristic = values.getString(1);
						currentCreationType = values.getString(2);
						currentProperty = values.getString(3);
						if((prevCharacteristic != null && prevCreationType != null && prevProperty != null) && (!prevCharacteristic.equals(currentCharacteristic) || !prevCreationType.equals(currentCreationType) || !prevProperty.equals(currentProperty))) {
							ids.removeFirst();
							if(!ids.isEmpty()) {
								wasteBasket.addAll(ids);
							}
							ids.clear();
						}
						ids.addLast(currentId);
						prevCharacteristic = currentCharacteristic;
						prevCreationType = currentCreationType;
						prevProperty = currentProperty;
					}
					if(rows.length() > 0) {
						ids.removeFirst();
						if(!ids.isEmpty()) {
							wasteBasket.addAll(ids);
							ids.clear();
						}
					}
					prevCharacteristic = null;
					prevCreationType = null;
					prevProperty = null;
				}while(currentIndex < tz);
				currentIndex = 0;
				for(String wb : wasteBasket) {
					sb.append(sb.length() == 0 ? "" : ",").append(wb);
				}
				if(sb.length() > 0) {
					System.out.println("Deleted... " +  new RestClient("Content-Type: application/x-www-form-urlencoded", "Accept: application/json", "Authorization: Basic " + encoded)
							.getRequest("DELETE", baseUrlQA + "/list/StandardizationValue/byItems?items=" + java.net.URLEncoder.encode( sb.toString(), "UTF-8"), null) );
					sb.setLength(0);

					try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\wasteBasket", true)))){
						wasteBasket.forEach(el -> pw.println(el));
					}catch(java.io.IOException e) {

					}
					wasteBasket.clear();
				}else {
					emptyBoys.addLast(templateId);
					System.out.println("Not deleting anything for " + templateId);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\emptyBoys", true)))){
			emptyBoys.forEach(eb -> pw.println(eb));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.print("Done. " + formatMillis(System.currentTimeMillis() - init));
//		System.exit(0);
	}

	  private static String formatMillis(long millis){
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
