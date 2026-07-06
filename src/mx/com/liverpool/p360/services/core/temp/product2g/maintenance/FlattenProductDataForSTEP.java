package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class FlattenProductDataForSTEP extends RESTWrapper {

	
	public static void main(String[] args) {
		FlattenProductDataForSTEP gld = new FlattenProductDataForSTEP();
		gld.getLookup();
	}
	
	private void getLookup() {
		
		int ap = Runtime.getRuntime().availableProcessors();
		
		java.util.concurrent.ArrayBlockingQueue<MiTarea> mt = new java.util.concurrent.ArrayBlockingQueue<>(ap);
		Worker[] workers = new Worker[ap];
		Thread[] threads = new Thread[ap];
		for(int i=0; i<workers.length; i++) {
			workers[i] = new Worker(mt);
			threads[i] = new Thread(workers[i]);
			threads[i].setPriority(Thread.currentThread().getPriority() - 1);
			threads[i].setDaemon(false);
			threads[i].start();
		}
		
		java.util.LinkedList<String[]> pairsList = new java.util.LinkedList<>();
		java.util.LinkedList<String> characteristicList = new java.util.LinkedList<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier");
		qp.put("query", "Characteristic.Identifier wildcard \"%VAD\" or Characteristic.Identifier wildcard \"%VaD\" or Characteristic.Identifier wildcard \"%Att\"");
		qp.put("pageSize", "10000");
		collectData("list", "Characteristic", null, "bySearch", qp, row->{
			org.json.JSONArray values = row.getJSONArray("values");
			String id = values.getString(0);
			if(
				!id.startsWith("mdr_")
				&& !id.startsWith("rma_")
				&& !id.startsWith("rrd_")
				&& !id.startsWith("rmum_")
				&& !id.startsWith("rre_")
				&& !id.startsWith("msj_")
				&& !id.startsWith("rem_")
				&& !id.matches("^[a-z]+_.+")
			) {
				characteristicList.addLast(id);
			}
		}, System.out::println);
		java.util.Set<String> aprobadosDeInterés = new java.util.TreeSet<>();
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "aprobados_de_interés.txt"))){
			lns.forEach(aprobadosDeInterés::add);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		qp.clear();
		qp.put("query", /*Product2G.ProductNo wildcard \"1754611%\" and */ "Product2G.CurrentStatus = 1007");
		qp.put("pageSize", "5000");
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
				+ ",Product2GStructureMap.StructureGroup('PrimaryProductTaxonomy')->StructureGroup.Identifier"
			);
		System.out.println(aprobadosDeInterés);
		collectData("list", "Product2G", null, "bySearch", qp, row->{
//			if(row.getJSONArray("values").getJSONArray(1).getString(0).startsWith("9999")) {
//				System.out.println(row.getJSONArray("values").getString(0));
//			}
			
			org.json.JSONArray values = row.getJSONArray("values");
			System.out.println(values);
			if(!"".equals(values.getJSONArray(1).getString(0)) && aprobadosDeInterés.contains(values.getString(0)))
				pairsList.addLast(new String[] { values.getJSONArray(1).getString(0), values.getJSONArray(2).getString(0), values.getString(0) });
			
		}, System.out::println);
		qp.clear();
		qp.put("includeLabels", "true");
		qp.put("includeIds", "true");
		System.out.println("Found: " + pairsList.size());
		int h = (int) (pairsList.size() / ap);
		System.out.println(h);
		java.util.LinkedList<String[]> misPairs = new java.util.LinkedList<>();
		int b = 0;
		for(int i=0; i<pairsList.size(); i++) {
			misPairs.addLast(pairsList.get(i));
			if((i+1) % h == 0) {
				b++;
				mt.add(new MiTarea(characteristicList, b, misPairs));
				misPairs = new java.util.LinkedList<>();
			}
		}
		if( !misPairs.isEmpty() ) {
			b++;
			mt.add(new MiTarea(characteristicList, b, misPairs));
			misPairs = new java.util.LinkedList<>();
		}
		for(int i=0; i<workers.length; i++) {
			workers[i].setRunning(false);
		}
		System.out.println("Now waiting for finish to come...");
		for(int i=0; i<threads.length; i++) {
			try {
				threads[i].join();
			}catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Done");
	}
	
	private class Worker implements Runnable{
		
		private boolean running = true;
		private java.util.concurrent.ArrayBlockingQueue<MiTarea> tareas;
		
		public Worker(java.util.concurrent.ArrayBlockingQueue<MiTarea> tareas) {
			this.tareas = tareas;
		}
		
		@Override
		public void run() {
			MiTarea mt = null;
			while(running || !tareas.isEmpty()) {
				try {
					mt = tareas.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
					if(mt != null) {
						mt.run();
					}
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		public void setRunning(boolean running) {
			this.running = running;
		}
		
	}
	
	private class MiTarea implements Runnable{
	
		private java.util.LinkedList<String[]> pairsList;
		private int miId;
		private java.util.LinkedList<String> characteristicList;
		
		public MiTarea(java.util.LinkedList<String> characteristicList, int miId, java.util.LinkedList<String[]> pairsList) {
			this.characteristicList = characteristicList;
			this.miId = miId;
			this.pairsList = pairsList;
		}
		
		@Override
		public void run() {
			System.out.println("Started...");
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			qp.put("includeLabels", "true");
			qp.put("includeIds", "true");
			org.json.JSONObject data = null;
			org.json.JSONArray characteristicArray = null;
			java.util.Map<String, org.json.JSONObject> dataMap = new java.util.TreeMap<>();
			String[] row = new String[characteristicList.size()];
			String[] fileRow = new String[row.length + 2];
			org.json.JSONObject cd = null;
			int index = 0;
			int count = 0;
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "TablaVADAtt_NoAprobadosConSKU__" + miId + ".csv").toString())))){
				fileRow[0] = "SKU";
				fileRow[1] = "Plantilla";
				for(String cid : characteristicList) {
					fileRow[index + 2] = cid;
					index++;
				}
				index = 0;
				pw.println( getRw().serializeChunk(fileRow) );
				for(String[] pair : pairsList) {
					data = getRw().makeRequest("GET", "/object/Product2G/'" + pair[2] + "'@1", qp, null);
					if(data == null || !data.has("_data") || !data.getJSONObject("_data").has("_characteristicRecords")) {
						System.out.println("PANIC: " + pair[2]);
					}else {
						data = data.getJSONObject("_data");
						characteristicArray = data.getJSONArray("_characteristicRecords");
						for(int i=0; i<characteristicArray.length(); i++) {
							dataMap.put(characteristicArray.getJSONObject(i).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"), characteristicArray.getJSONObject(i));
						}
						for(String cid : characteristicList) {
							cd = dataMap.get(cid);
							row[index] = getData(cd);
							index++;
						}
						index = 0;
						fileRow[0] = pair[0];
						fileRow[1] = pair[1];
						for(int i=0; i<row.length; i++) {
							fileRow[i + 2] = row[i];
						}
						pw.println( getRw().serializeChunk(fileRow) );
					}
					System.out.println( (count + 1) + "/" + pairsList.size() );
					count++;
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		
		private String getData(org.json.JSONObject cd) {
			return cd != null ? 
					cd.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0) instanceof org.json.JSONObject ? cd.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label") : cd.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0).replaceAll("\n", "\\\\n"
							+ "")
					: "";
		}
	
	}
}
