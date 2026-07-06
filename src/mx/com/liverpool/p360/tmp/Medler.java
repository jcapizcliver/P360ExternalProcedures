package mx.com.liverpool.p360.tmp;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import org.json.JSONException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class Medler {

	private static final String BIN = "682177a28561e97a50122115";

	public static void main(String[] args) throws ServiceUnavailableException, JSONException {
//		tryIt();
//		System.exit(0);
		String bin = BIN;
		if("".equals(bin)) {
			String ip = getMe();
			RESTWorkshop rw = new RESTWorkshop("Content-Type: application/json", "X-Master-Key: $2a$10$cxqvW1QAzHLahBlMugTH4O0h9NY5hUl5G4OZuiataTTLGpDySPqiq");
			rw.setBaseUrl("https://api.jsonbin.io/v3/b");
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			org.json.JSONObject response = null;
			response = rw.makeRequest("POST", "", qp, new org.json.JSONObject().put("ip", ip).toString());
			bin = response.getJSONObject("metadata").getString("id");
			System.out.println("<::>" + rw.getRawResponse() + "<::>");
		}
		String prev = "201.102.36.171";
		while(true) {
			prev = putIt(bin, prev);
			System.out.println("Now zzZzleeping...");
			try {
				Thread.sleep(1000*60);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	private static String putIt(String bin, String prevIp) throws ServiceUnavailableException, JSONException {
		String ip = getMe();
		if(prevIp != null && prevIp.equals(ip)) {
			System.out.println("No changes...");
			return prevIp;
		}
		RESTWorkshop rw = new RESTWorkshop("Content-Type: application/json", "X-Master-Key: $2a$10$cxqvW1QAzHLahBlMugTH4O0h9NY5hUl5G4OZuiataTTLGpDySPqiq");
		rw.setBaseUrl("https://api.jsonbin.io/v3/b/" + bin);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		rw.makeRequest("PUT", "", qp, new org.json.JSONObject().put("ip", ip).toString());
		System.out.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ").format(new java.util.Date()) + "] Done. " + rw.getRawResponse());
		return ip;
	}

	private static void tryIt() throws ServiceUnavailableException {
		// https://api.jsonbin.io/v3/b
		RESTWorkshop rw = new RESTWorkshop("Accept: application/json", "X-Master-Key: $2a$10$cxqvW1QAzHLahBlMugTH4O0h9NY5hUl5G4OZuiataTTLGpDySPqiq");
		rw.setBaseUrl("https://api.jsonbin.io/v3/b/682177a28561e97a50122115");
		org.json.JSONObject response = null;
		response =  rw.makeRequest("GET", "");
		if(response == null) {
			System.out.println("-->" + rw.getRawResponse() + "<--");
		}else {
			System.out.println("Response: -->" + response + "<--");
		}
	}

	private static String getMe() throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl("https://checkip.amazonaws.com");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		response =  rw.makeRequest("GET", "");
		if(response == null) {
			System.out.println("-->" + rw.getRawResponse() + "<--");
			return rw.getRawResponse();
		}else {
		}
		return "";
	}

}
