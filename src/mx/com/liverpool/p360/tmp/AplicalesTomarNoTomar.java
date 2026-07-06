package mx.com.liverpool.p360.tmp;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import org.json.JSONException;

import mx.com.liverpool.p360.services.core.AgarraloONo;
import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class AplicalesTomarNoTomar {

	private static final RESTWorkshop rw = new RESTWorkshop();

	public static void main(String[] args) {
//		consultaLosQueYa().forEach(System.out::println);
//		System.exit(0);
//		java.util.LinkedList<String> propuestas = propuestas();
//		propuestas.forEach(AplicalesTomarNoTomar::elAgarraloONo);
		try {
			elAgarraloONo("1698767480974194");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServiceUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void elAgarraloONo(String externalId) throws JSONException, ServiceUnavailableException {
		if(externalId != null && !"".equals(externalId)) {
			AgarraloONo a = new AgarraloONo();
			a.checale(externalId, rw.getBaseUrl());
		}
	}

	private static java.util.LinkedList<String> propuestas(){
		java.util.LinkedList<String> productos = new java.util.LinkedList<>();

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
		qp.put("query", "not characteristic('SKU',-1) is empty");
		qp.put("pageSize", "900");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		do{
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
			if(response == null) {
				System.out.println(rw.getRawResponse());
				return null;
			}
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				productos.addLast(values.getString(0)); // values.getJSONArray(1).getString(0));
				System.out.println(values.getJSONArray(1).getString(0));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		return productos;
	}

	private static java.util.LinkedList<String> consultaLosQueYa(){
		java.util.LinkedList<String> productos = new java.util.LinkedList<>();

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
		qp.put("query", "not characteristic('AssignTakeNoTake',-1) is empty");
		qp.put("pageSize", "900");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		do{
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
			if(response == null) {
				System.out.println(rw.getRawResponse());
				return null;
			}
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				productos.addLast(values.getJSONArray(0).getString(0)); // values.getJSONArray(1).getString(0));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		return productos;
	}
}
