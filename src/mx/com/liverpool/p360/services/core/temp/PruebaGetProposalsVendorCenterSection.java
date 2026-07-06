package mx.com.liverpool.p360.services.core.temp;

import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.RestClient;

public class PruebaGetProposalsVendorCenterSection {

	private static final String encoded = "cmVzdDpoZWlsZXI=";
	private static final String baseUrl = "https://webctep360dev.liverpool.com.mx/rest/V2.0";
	private static final RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);

	public static void main(String[] args) {
		java.util.Map<String, String> losQueSi = gatherFieldsToSendByBusiness("EU4-113578", "Liverpool");
		losQueSi.forEach((k,v)->System.out.println(k + ": " + v));
	}

	private static java.util.Map<String, String> gatherFieldsToSendByBusiness(String template, String business) {
	    java.util.Map<String, String> losQueSi = new java.util.TreeMap<>();
		String rawResponse = null;
		JSONObject response = null;
		org.json.JSONArray rows = null;
		java.util.LinkedList<String> sendToVendorCenter = new java.util.LinkedList<>();
		java.util.LinkedList<String> toBusiness = new java.util.LinkedList<>();
		int startIndex = 0;
		int totalSize = 0;
		String aux = null;
		int times = 0;
		try {
			String url = null;
			java.util.Map<String, String> properties = new java.util.TreeMap<>();
			do {
				url = baseUrl + "/list/StandardizationValue/bySearch?dictionaryProxy='ExtensionDeMetadatos_%20ValoresPredeterminadosPorPlantilla'&query=" + java.net.URLEncoder.encode( "(StandardizationValue.Property equals Business or StandardizationValue.Property equals SentToVendorCenter or StandardizationValue.Property equals VendorCenterSection) and StandardizationValue.StructureGroup equals \"" + template + "\" and StandardizationValue.CreationType equals Proposal and StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\"", "UTF-8" ) + "&metaData=true&fields=" + java.net.URLEncoder.encode("StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.Property->LookupValue.Code,StandardizationValue.PropertyValue", "UTF-8") + "&pageSize=1000&startIndex=" + startIndex;
				rawResponse = rc.getRequest( "GET", url, null );
				response = new JSONObject(rawResponse);
				rows = response.getJSONArray( "rows" );
				for(int i=0; i<rows.length(); i++) {
					aux = rows.getJSONObject( i ).getJSONArray( "values" ).getString( 0 ) + "<::>" + rows.getJSONObject( i ).getJSONArray( "values" ).getString( 1 );
					properties.put(aux, rows.getJSONObject( i ).getJSONArray( "values" ).getString( 2 ));
					if("SAPObjectType".equals(rows.getJSONObject( i ).getJSONArray( "values" ).getString( 0 ))) {
						System.out.println("--->" + aux + "<---");
					}
				}
				totalSize = response.getInt( "totalSize" );
				startIndex += rows.length();
			}while(startIndex < totalSize);
			java.util.LinkedList<java.util.Map.Entry<String, String>> wrapper = new java.util.LinkedList<>(properties.entrySet());
			java.util.Collections.sort(wrapper, (o1,o2)->o1.getKey().split("<::>")[0].compareTo(o2.getKey().split("<::>")[0]));
			String ccharac = null;
			String prevCharac = null;
			String[] pieces = null;
			String property = null;
			String sentToVendorCenter = null;
			String cBusiness = null;
			String vendorCenterSection = null;
			for(java.util.Map.Entry<String, String> charac : wrapper){
				pieces = charac.getKey().split("<::>");
				ccharac = pieces[0];
				property = pieces[1];
				if(prevCharac != null && !prevCharac.equals(ccharac)){
					if(cBusiness != null){
						if("1".equals(sentToVendorCenter) && cBusiness.contains(business)){
							losQueSi.put(prevCharac, vendorCenterSection);
							System.out.println("\tAgregada: " + prevCharac + " | " + vendorCenterSection);
						}else {
							System.out.println("\t\tNo agregada x.x " + prevCharac + " ..." + sentToVendorCenter + "..." + cBusiness + "...");
						}
					}else{
						System.out.println("A characteristic without allowed business... " + prevCharac);
					}
					vendorCenterSection = null;
					sentToVendorCenter = null;
					cBusiness = null;
				}
				times++;
				if(times < 200) {
					System.out.println(charac);
				}
				if("VendorCenterSection".equals(property)){
					vendorCenterSection = charac.getValue();
				}else if("SentToVendorCenter".equals(property)){
					sentToVendorCenter = charac.getValue();
				}else if("Business".equals(property)){
					cBusiness = charac.getValue();
				}
				prevCharac = ccharac;
			}
		}catch(Exception e) {
			System.out.println(rawResponse);
			e.printStackTrace();
		}
		return losQueSi;
	}
}
