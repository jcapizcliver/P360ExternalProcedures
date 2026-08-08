package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class RESTWorkshop {

	private final XMLMisc xmm = new XMLMisc();
	private final RestClient rc;
	private final Yep yep = new Yep();

	private String baseUrl = "";

	private String delimiter = "\"";
	private String separator = ",";
	private String escape = "\\";

	private String rawResponse;
	private Exception ex = null;

	private java.util.Map<String, String> qp = new java.util.TreeMap<>();

	public RESTWorkshop() {
		String encoded = "cmVzdDpoZWlsZXI=";
		this.rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Accept-Language: es", "Authorization: Basic " + encoded);
	}

	public void addHeader(String name, String value) {
		this.rc.getHeader().put(name, value);
	}

	public String removeHeader(String name) {
		return this.rc.getHeader().remove(name);
	}

	public void putParameter(String name, String value) {
		qp.put(name, value);
	}

	public String removeParameter(String name) {
		return qp.remove(name);
	}

	public void clearParameters() {
		qp.clear();
	}

	public RESTWorkshop(boolean takeFirstAsURL, String... headers) {
		if(takeFirstAsURL) {
			this.baseUrl = headers[0];
			java.util.LinkedList<String> hs = new java.util.LinkedList<>();
			for(int i=1; i<headers.length; i++) {
				hs.addLast(headers[i]);
			}
			this.rc = new RestClient(hs.toArray(new String[] {}));
		} else {
			this.rc = new RestClient(headers);
		}
	}

	public RESTWorkshop(String... headers) {
		this.rc = new RestClient(headers);
	}

	public XMLMisc getXmm() {
		return xmm;
	}

	public RestClient getRc() {
		return rc;
	}

	public String[] parseLine(String line) {
		return yep.parseLine(line, delimiter, separator, escape);
	}

	public String[] parseLine(String line, String delimiter, String separator, String escape) {
		return yep.parseLine(line, delimiter, separator, escape);
	}

	public String serializeLine(String value) {
		try{
			return value == null ? "" : value.contains(separator) || value.contains(delimiter) || value.contains("\\".equals(escape) ? "\\" : escape) || value.contains("\n") ? delimiter + value.replaceAll("(?=[" + delimiter + ("\\".equals(escape) ? "\\\\" : escape) + "])", "\\".equals(escape) ? "\\\\" : escape) + delimiter: value;
		}catch(IllegalArgumentException e) {
			throw new RuntimeException(e);
		}
	}

	public String serializeLine(String value, String delimiter, String separator, String escape) throws IllegalArgumentException {
		return value == null ? "" : value.contains(separator) || value.contains(delimiter) || value.contains("\\".equals(escape) ? "\\" : escape) || value.contains("\n") ? delimiter + value.replaceAll("(?=[" + delimiter + ("\\".equals(escape) ? "\\\\" : escape) + "])", "\\".equals(escape) ? "\\\\" : escape) + delimiter: value;
	}

	public String serializeChunk(Object[] pieces, String delimiter, String separator, String escape) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<pieces.length; i++) {
			sb.append(i == 0 ? "" : separator).append(serializeLine(String.valueOf(pieces[i]), delimiter, separator, escape));
		}
		return sb.toString();
	}

	public String serializeChunk(Object[] pieces) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<pieces.length; i++) {
			sb.append(i == 0 ? "" : separator).append(serializeLine(String.valueOf(pieces[i])));
		}
		return sb.toString();
	}

	@Deprecated
	public String makeRequest(String method, String url, String payload) throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, IOException, ServiceUnavailableException {
		String rawResponse = null;
//		System.out.println("Requesting: " + baseUrl + url);
		rawResponse = rc.getRequest(method, baseUrl + url, payload);
		this.rawResponse = rawResponse;
		return rawResponse;
	}

	public String makeRequest(String method, String url, String payload, java.util.Map<String, String> headers) throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, IOException, ServiceUnavailableException {
		String rawResponse = null;
		rawResponse = rc.getRequest(method, baseUrl + url, payload, headers);
		return rawResponse;
	}

	public org.json.JSONObject makeRequest(String method, String path) throws ServiceUnavailableException{
		String rawResponse = null;
		org.json.JSONObject response = null;
		StringBuilder sb = new StringBuilder();
		int times = 0;
		for(java.util.Map.Entry<String, String> entry : qp.entrySet()) {
			sb.append( times == 0 ? "?" : "&" ).append(entry.getKey()).append("=").append(encode(entry.getValue()));
			times++;
		}
		try {
			rawResponse = makeRequest(method, path + sb.toString(), null);
		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
			e.printStackTrace();
		}
		try {
			response = new org.json.JSONObject(rawResponse);
		}catch(NullPointerException | org.json.JSONException e) {
//			System.out.println("############" + path + sb.toString() + "##########" + rawResponse + ".");
//			e.printStackTrace();
			ex = e;
//			path + "<::>" + sb.toString() + "____" +
		}
		return response;
	}
	
	public org.json.JSONObject makeRequestWS(String method, String path, java.util.Map<String, String> queryParameters, String message) throws ServiceUnavailableException {
		String rawResponse = null;
		org.json.JSONObject response = null;
		StringBuilder sb = new StringBuilder();
		int times = 0;
		for(java.util.Map.Entry<String, String> entry : (queryParameters == null ? qp : queryParameters).entrySet()) {
			sb.append( times == 0 ? "?" : "&" ).append(entry.getKey()).append("=").append(encode(entry.getValue()));
			times++;
		}
		try {
			rawResponse = makeRequest(method, path + sb.toString(), message);
		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException e) {
			e.printStackTrace();
		}
		try {
			response = new org.json.JSONObject(rawResponse);
		}catch(NullPointerException | org.json.JSONException e) {
			ex = e;
		}
		return response;
	}

	public org.json.JSONObject makeRequest(String method, String path, java.util.Map<String, String> queryParameters, String message) {
		String rawResponse = null;
		org.json.JSONObject response = null;
		StringBuilder sb = new StringBuilder();
		int times = 0;
		for(java.util.Map.Entry<String, String> entry : (queryParameters == null ? qp : queryParameters).entrySet()) {
			sb.append( times == 0 ? "?" : "&" ).append(entry.getKey()).append("=").append(encode(entry.getValue()));
			times++;
		}
		try {
			rawResponse = makeRequest(method, path + sb.toString(), message);
		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException | ServiceUnavailableException e) {
			e.printStackTrace();
		}
		try {
			response = new org.json.JSONObject(rawResponse);
		}catch(NullPointerException | org.json.JSONException e) {
			ex = e;
		}
		return response;
	}

	public Exception getException() {
		return ex;
	}

	public String getRawResponse() {
		return rawResponse;
	}

	public String encode(String value) {
		try{
			return java.net.URLEncoder.encode(value, "UTF-8");
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public org.json.JSONArray treatment(org.json.JSONArray vals) {
		String helper = null;
		java.util.LinkedList<String> h = new java.util.LinkedList<>();
		String[] pieces = null;
		java.util.Set<String> childValues = new java.util.TreeSet<>();
		for(int i=0; i<vals.length(); i++) {
			helper = vals.getString(i);
			if(helper.contains("/")) {
				pieces = helper.split("/");
				for (String element : pieces) {
					if(element != null) {
						childValues.add(element);
					}
				}
			}else if(helper.contains(" & ")) {
				pieces = helper.split(" \\& ");
				for (String element : pieces) {
					childValues.add(element);
				}
			}else {
				h.addLast(helper.trim());
			}
		}
		if(!childValues.isEmpty()) {
			for(String cv : childValues) {
				if(!"No data".equals(cv)) {
					h.addLast(cv.trim());
				}
			}
		}
		java.util.Collections.sort(h);
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for(String h0 : h) {
			sb.append(sb.length() == 1 ? "" : ",").append( org.json.JSONObject.quote(h0) );
		}
		sb.append("]");
		return new org.json.JSONArray(sb.toString());
	}

	public String joinJSONArray(org.json.JSONArray values, String separator) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i< values.length(); i++) {
			sb.append(sb.length() == 0 ? "" : separator).append(values.getString(i));
		}
		return sb.toString();
	}

	public String formatTime(long millis) {
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

	public String getBaseUrl() {
		return baseUrl;
	}

	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public String getEscape() {
		return escape;
	}

	public void setEscape(String escape) {
		this.escape = escape;
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

}
