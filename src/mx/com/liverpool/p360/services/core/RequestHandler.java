package mx.com.liverpool.p360.services.core;

public class RequestHandler {

	private final org.json.JSONObject request = new org.json.JSONObject();
	private final org.json.JSONArray rows = new org.json.JSONArray();
	private final ProcessPayload pp;
	private final int bs;
	
	public RequestHandler(org.json.JSONArray columns, int bs, ProcessPayload pp) {
		this.bs = bs;
		this.pp = pp;
		request.put("columns", columns);
		request.put("rows", rows);
	}
	
	public void addRow(org.json.JSONObject row) {
		rows.put(row);
		if(rows.length() == bs) {
			pp.sendData(request);
		}
	}
	
	public void sendData() {
		if(rows.length() > 0) {
			pp.sendData(request);
		}
	}
	
	public org.json.JSONArray getRows(){
		return rows;
	}
	
}
