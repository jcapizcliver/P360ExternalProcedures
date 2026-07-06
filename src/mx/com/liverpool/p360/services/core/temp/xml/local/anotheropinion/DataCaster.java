package mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion;

import java.io.FileNotFoundException;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class DataCaster extends DataCasterFileTraversor {

	private final java.util.Map<String, String> qp = new java.util.HashMap<>();
	private final RESTWrapper rw;
	private final String entity;
	private final String attId;
	private final String dataType;
	private final int bs = 200;
	private final org.json.JSONArray columns = new org.json.JSONArray();
	private final org.json.JSONArray rows = new org.json.JSONArray();
	private final org.json.JSONObject request = new org.json.JSONObject().put("columns", columns).put("rows", rows);
	private final java.io.PrintWriter simpleLog;
	private final java.io.PrintWriter simpleErrLog;
	private final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
	
	private int bn = 0;
	
	public DataCaster(String entity, RESTWrapper rw, String attId, String dataType) throws FileNotFoundException {
		this.entity = entity;
		this.rw = rw;
		columns.put( new org.json.JSONObject().put("identifier", entity + "CharacteristicValue" + "Lang.Value('" + attId + "',root,\"0000.0000.RK\",'" + attId + "'" + ",-1)" ));
		System.out.println(columns.get(0));
		this.attId = attId;
		this.dataType = dataType;
		qp.put("includeObjectsInProtocol", "false");
		simpleLog = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("remainingData", entity + "_" + processFileName( attId ) + ".log").toFile())));
		simpleErrLog = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("remainingData", entity + "_" + processFileName( attId ) + ".err").toFile())));
	}

	private String processFileName(String fn) {
		return fn.contains("/") ? fn.replace("/", "<::>") : fn;
	}
	
	public void addValue(Value value, String identifier) {
		String castedValue = "LOOKUP".equals(dataType) ? nvlById(value) : nvl(value);
		request.getJSONArray("rows").put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + identifier + "'@1")).put("values", new org.json.JSONArray().put(castedValue)));
		if(rows.length() == bs) {
			bn++;
			sendData();
		}
	}
	
	public void addValue(String value, String identifier) {
		String castedValue = value;
		request.getJSONArray("rows").put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + identifier + "'@1")).put("values", new org.json.JSONArray().put(castedValue)));
		if(rows.length() == bs) {
			bn++;
			sendData();
		}
	}
    
	public void sendData() {
		rw.writeData("list", entity, null, qp, request, this::log);
	}
	
    private String nvl(Value v) {
    	return v == null ? "" : v.getText();
    }
    
    private String nvlById(Value v) {
    	return v == null ? "" : v.getId() == null ? v.getText() == null ? "" : v.getText() : v.getId();
    }
    
    public void log(String message) {
    	simpleLog.println( "[" + sdf.format(new java.util.Date()) + "] (" + attId + ", " + entity + ", " + rows.length() + ") " + bn + " -> " + message );
    }
    
    public void logE(Exception e) {
    	simpleErrLog.println( "[" + sdf.format(new java.util.Date()) + "] (" + attId + ", " + entity + ", " + rows.length() + ") " + bn + " ERROR " );
    	e.printStackTrace(simpleErrLog);
    }

    public void close() {
    	simpleLog.close();
    }
    
    public String getAttId() {
    	return attId;
    }
    
    public String getEntity() {
    	return this.entity;
    }
	
}
