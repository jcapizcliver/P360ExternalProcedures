package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.temp.characteristic.CheckCharacteristicsIsActive;

public class RestauraValoresTrasCambiarLaLKP {

	private static final RESTWrapper rw = new RESTWrapper();
	
	private static final java.util.Map<String, java.util.Map<String, String>> LKPS = new java.util.TreeMap<>(); 
	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeObjectsInProtocol", "false");
		java.util.concurrent.ConcurrentLinkedQueue<String[]> lines = new java.util.concurrent.ConcurrentLinkedQueue<>();
		try(java.util.stream.Stream<String> stream = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "tmp", "Productos con valores a respaldar para cambio de lkp.bkp240720251531"))){
			stream.parallel().map(rw.getRw()::parseLine).forEach(lines::add);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Read data. " + lines.size() + " in " + rw.getRw().formatTime(System.currentTimeMillis() - init));
		java.util.LinkedList<String[]> headers = new java.util.LinkedList<>();
		try(java.util.stream.Stream<String> stream = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "tmp", "Productos con valores a respaldar para cambio de lkp.bkp240720251531HEADER"))){
			stream.map(rw.getRw()::parseLine).forEach(headers::addLast);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		if(!headers.isEmpty()) {
			String[] header = headers.getFirst();
			org.json.JSONArray columns = new org.json.JSONArray();
			for(int i=1; i<header.length; i++) {
				columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('" + header[i] + "',root,\"0000.0000.RK\",'" + header[i] + "',-1)"));
			}
			RequestHandler restoreData = new RequestHandler(columns, 3000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println));
			org.json.JSONArray values = null;
			java.util.Map<String, String> lkpData = null;
			String lkpLabel = null;
			loadLkpData();
			for(String[] data : lines) {
				values = new org.json.JSONArray();
				for(int i=1; i<data.length; i++) {
					if("".equals(data[i])) {
						values.put( "" );
					}else {
						lkpData = LKPS.get(header[i]);
						if(lkpData == null) {
							System.out.println("No lkp found for: " + header[i]);
							System.exit(1);
						}
						lkpLabel = lkpData.get(data[i]);
						if(lkpLabel == null) {
							System.out.println("No value found for: " + data[i] + " in " + header[i]);
							values.put( "" );
						}else {
							values.put(new org.json.JSONArray().put( data[i] ));
						}
					}
				}
				restoreData.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + data[0] + "'@1")).put("values", values));
			}
			restoreData.sendData();
		}
		System.out.println("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
	private static void loadLkpData() {
		java.util.Map<String, String> data = null;
		String id = null;
		String[] pieces = null;
		for( int i=0; i < CheckCharacteristicsIsActive.rows.length; i++) {
			pieces = rw.getRw().parseLine( CheckCharacteristicsIsActive.rows[i] );
			id = pieces[2];
			data = LKPS.get(id);
			if(data == null) {
				data = collectData(id);
				LKPS.put(pieces[0], data);
			}
		}
	}
	
	private static java.util.Map<String, String> collectData(String id){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, String> map = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("pageSize", "2000");
		qp.put("lookup", "'" + id + "'");
		rw.collectData("list", "LookupValue", null, "byLookup", qp, row -> map.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1)), System.out::println);
		return map;
	}
	
}
