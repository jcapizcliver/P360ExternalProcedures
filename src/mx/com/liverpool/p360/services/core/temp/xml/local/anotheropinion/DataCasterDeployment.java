package mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion;

import java.io.FileNotFoundException;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class DataCasterDeployment {

	
	public static void main(String[] args) throws FileNotFoundException, InterruptedException {
//		int nap = Runtime.getRuntime().availableProcessors();
//		nap = nap <= 0 ? 2 : nap - 1;
		int nap = 15;
		System.out.println("Using " + nap + " panas.");
		java.util.List<String> product2g = new java.util.ArrayList<>();
		java.util.List<String> article = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader( new java.io.FileInputStream(java.nio.file.Paths.get("ProductLevelAttributeIDs.csv").toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				product2g.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader( new java.io.FileInputStream(java.nio.file.Paths.get("ChildProductLevelAttributeIDs.csv").toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				article.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		RESTWrapper rw = new RESTWrapper();
		java.util.concurrent.ArrayBlockingQueue<DataCaster> casters = new java.util.concurrent.ArrayBlockingQueue<>(4000);
		DataCasterFileTraversorWorker[] workers = new DataCasterFileTraversorWorker[nap];
		Thread[] ts = new Thread[nap];
		for(int i=0; i<workers.length; i++) {
			workers[i] = new DataCasterFileTraversorWorker(rw, casters);
			ts[i] = new Thread( workers[i] );
			ts[i].setPriority(Thread.currentThread().getPriority() - 1);
			ts[i].setDaemon(false);
			ts[i].start();
		}
		java.util.Map<String, String[]> characteristics = new java.util.HashMap<>();
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("fields", "Characteristic.Identifier,Characteristic.DataType,Characteristic.Entities");
		qp.put("query", "Characteristic.ParentCharacteristic is empty and not Characteristic.DataType = \"NONE\" and not Characteristic.Entities is empty and not Characteristic.Identifier wildcard \"%_Rechazo\"");
		qp.put("pageSize", "5000");
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			System.out.println(values);
			org.json.JSONArray entries = values.getJSONArray(2);
			StringBuilder sb = new StringBuilder();
			for(int j=0; j<entries.length(); j++) {
				sb.append(j == 0 ? "" : ",").append(entries.get(j));
			}
			characteristics.put(values.getString(0), new String[] { values.getString(1), sb.toString() });
		});
		String[] dataType = null;
		for(String att : article) {
			dataType = characteristics.get(att);
			if(dataType != null && dataType[1].contains("Article"))
				casters.put(new DataCaster("Article", rw, att, dataType[0]));
			else
				System.out.println("Not a known characteristic: " + att);
		}
		for(String att : product2g) {
			dataType = characteristics.get(att);
			if(dataType != null && dataType[1].contains("Product2G"))
				casters.put(new DataCaster("Product2G", rw, att, dataType[0]));
			else
				System.out.println("Not a known characteristic: " + att);
		}
		DataCasterFileTraversorWorker dcft = new DataCasterFileTraversorWorker(rw, casters);
		dcft.setRunning(false);
		for(int i=0; i<workers.length; i++) {
			workers[i].setRunning(false);
		}
		dcft.run();
		System.out.println("Now dequeuing the queue");
		for(int i=0; i<workers.length; i++) {
			try {
				ts[i].join();
			}catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
