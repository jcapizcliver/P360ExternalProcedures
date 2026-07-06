package mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class DataCasterFileTraversorWorker implements Runnable {
	
	private final java.util.concurrent.ArrayBlockingQueue<DataCaster> casters;
	private final RESTWrapper rw;
	
	private DataCaster caster = null;
	private boolean running = true;
	
	public DataCasterFileTraversorWorker(RESTWrapper rw, java.util.concurrent.ArrayBlockingQueue<DataCaster> casters) {
		this.rw = rw;
		this.casters = casters;
	}
	
	@Override
	public void run() {
		while(running || !casters.isEmpty()) {
			try {
				caster = casters.poll(10, java.util.concurrent.TimeUnit.MICROSECONDS);
				if(caster != null) {
					processCaster();
				}
			}catch(InterruptedException e) {
				e.printStackTrace();
				break;
			}// <Value AttributeID="ColoursLiverpoolAtt" ID="0001">Negro</Value>
		}
	}
	
	public void processCaster() {
		String fileName = processFileName( caster.getAttId() ) + ".csv";
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get( ("Product2G".equals( caster.getEntity() ) ? "salidasProduct2G" : "salidasArticle"), fileName ).toFile())))){
			String line;
			String[] pieces;
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				caster.addValue(pieces[1], pieces[0]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		caster.sendData();
//		eleseFinish();
		caster.close();
	}

	private String processFileName(String fn) {
		return fn.contains("/") ? fn.replace("/", "<::>") : fn;
	}
    
    private void eleseFinish() {
    	java.util.Map<String, String> qp = new java.util.TreeMap<>();
		RESTWrapper rw = new RESTWrapper();
		rw.getRw().setBaseUrl("https://chat.googleapis.com/v1/spaces"); // ");
		qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
		qp.put("token", "u-P1Me5vwfb04AoqTpZI0QCmPNR4fELWlqPgmupabSY");
		caster.log( "" + rw.getRw().makeRequest("POST", "/AAAAZpaMbww/messages", qp, 
				new org.json.JSONObject().put("text", 
						"Finalizado inserción de valores para " + caster.getAttId() + ".  😁.").toString()) );
    }
    
	public void setRunning(boolean running) {
		this.running = running;
	}
	
}
