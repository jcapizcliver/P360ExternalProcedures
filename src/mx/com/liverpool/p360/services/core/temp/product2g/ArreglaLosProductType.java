package mx.com.liverpool.p360.services.core.temp.product2g;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import org.json.JSONException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.temp.move.utils.GeneralOperations;

public class ArreglaLosProductType {
	
	public static void main(String[] args) {
		RESTWorkshop rw = new RESTWorkshop();
		ArreglaLosProductType a = new ArreglaLosProductType();
		try {
			a.doIt(rw);
		} catch (ServiceUnavailableException e) {
			e.printStackTrace();
		}
		
	}
	
	private void doIt(RESTWorkshop rw) throws ServiceUnavailableException {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, org.json.JSONArray> data = null;
		GeneralOperations go = new GeneralOperations();
		data = go.collectProductData(null,
				  "Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('SAP_BEHVO',root,\"0000.0000.RK\",'SAP_BEHVO')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('FSH_ID',root,\"0000.0000.RK\",'FSH_ID')->LookupValue.Code"
						  , 
				"Product2GCharacteristicValue.LookupValue('SAP_BEHVO',root,\"0000.0000.RK\",'SAP_BEHVO') is empty"
				+ " and "
				+ "(Product2G.ProductNo wildcard \"LVP%\" or Product2G.ProductNo wildcard \"SBB%\")");
		for(java.util.Map.Entry<String, org.json.JSONArray> entry : data.entrySet()) {
			org.json.JSONArray characteristicRecords = new org.json.JSONArray();
			try {
				calculaProductType(
						entry.getValue().getJSONArray(1).getString(0), 
						entry.getValue().getJSONArray(2).getString(0), 
						entry.getValue().getJSONArray(3).getString(0), 
						entry.getValue().getJSONArray(4).getString(0), 
						characteristicRecords, 
						rw);
			} catch (KeyManagementException | NoSuchAlgorithmException | JSONException | URISyntaxException
					| IOException e) {
				e.printStackTrace();
			}
			System.out.println("--->" + characteristicRecords);
			rw.makeRequest("PUT", "/object/Product2G/'" + entry.getValue().getString(0) + "'@1", qp, new org.json.JSONObject().put("_characteristicRecords", characteristicRecords).toString());
			System.out.println(rw.getRawResponse());
		}
	}
	
	private void calculaProductType(String negocio, String itemGroup, String sapBehvo1, String fshId, org.json.JSONArray newCharacteristicRecords, RESTWorkshop rw) throws KeyManagementException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException, IOException, ServiceUnavailableException {
		String sapBehvo = null;
		if("LVP".equals(negocio) || "MKP".equals(negocio)) {
			int month = Integer.parseInt( new java.text.SimpleDateFormat("MM").format(new java.util.Date()) );
			int year = Integer.parseInt( new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) ) + (month < 11 ? 0 : 1);
			newCharacteristicRecords.put( createCharacteristicValueObject("AnoEstacion", String.valueOf(year) ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("Temporada", new org.json.JSONObject().put("_code", "0003") ) );
			sapBehvo = lookupValue(itemGroup, "GpoArtVsEnvase", rw);
		}else if("SBB".equals(negocio)) {
			sapBehvo = lookupValue(itemGroup, "GpoArtVsEnvase_S4H", rw);
			if(fshId != null && fshId.length() >= 4) {
				newCharacteristicRecords.put( createCharacteristicValueObject("FSH_SEASON_YEAR",  fshId.subSequence(0, 4)) );
			}
		}
		if(sapBehvo == null || "".equals(sapBehvo)) {
			return;
		}
		sapBehvo = sapBehvo.substring(0,2);
		if(sapBehvo1 == null || "".equals(sapBehvo1)) {
			newCharacteristicRecords.put( createCharacteristicValueObject("SAP_BEHVO", new org.json.JSONObject().put("_code", sapBehvo.substring(0,2) )) );
		}
		if(sapBehvo != null && !"".equals(sapBehvo)) {
			System.out.println("Got " + sapBehvo + " for SAP_BEHVO.");
			String thevalue = "1";
			try{
				org.json.JSONArray rws = new org.json.JSONObject( rw.getRc().getRequest("GET", rw.getBaseUrl() + "/list/StandardizationValue/bySearch"
						+ "?dictionaryProxy=" + java.net.URLEncoder.encode("'BEHVO_LookupTable'", "UTF-8")
						+ "&query=" + java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"BEHVO_LookupTable\" and StandardizationValue.Value equals \"" + sapBehvo.substring(0,2) + "\"", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode("StandardizationValue.AlternativeValue", "UTF-8")
						, null) ).getJSONArray("rows");
				System.out.println("Checking sapBehvo: " + sapBehvo);
				if(rws.length() > 0) {
					thevalue = rws.getJSONObject(0).getJSONArray("values").getString(0);
				}
			}catch(org.json.JSONException e) {
				e.printStackTrace();
			}
			newCharacteristicRecords.put( createCharacteristicValueObject("ProductType",  new org.json.JSONObject().put("_code", thevalue) ) );
			System.out.println("Placing value: " + thevalue + " for ProductType");
		}else {
			System.out.println("No SAP_BEHVO found, placing value 1.");
			newCharacteristicRecords.put( createCharacteristicValueObject("ProductType",  new org.json.JSONObject().put("_code", "1") ) );
		}
	}
	
	private String lookupValue(String value, String standardizationDictionary, RESTWorkshop rw) throws KeyManagementException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException, IOException, ServiceUnavailableException {
		String rr = null;
		org.json.JSONObject resp = null;
		org.json.JSONArray rows = null;
		rr = rw.getRc().getRequest("GET", rw.getBaseUrl() + "/list/StandardizationValue/bySearch?dictionaryProxy=" + java.net.URLEncoder.encode("'" + standardizationDictionary + "'", "UTF-8")
			+ "&fields=" + java.net.URLEncoder.encode("StandardizationValue.AlternativeValue", "UTF-8") + "&query=" + java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + standardizationDictionary + "\" and StandardizationValue.Value equals \"" + value + "\"", "UTF-8"), null);
		System.out.println("Value: " + value + " in " + standardizationDictionary + ": " + rr);
		resp = new org.json.JSONObject(rr);
		rows = resp.getJSONArray("rows");
		if(rows.length() > 0) {
			return rows.getJSONObject(0).getJSONArray("values").getString(0);
		}
		return null;
	}

	private org.json.JSONObject createCharacteristicValueObject(String characteristicName, Object value){
		return new org.json.JSONObject().put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(value)).put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx"))))).put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", characteristicName)));
	}

}
