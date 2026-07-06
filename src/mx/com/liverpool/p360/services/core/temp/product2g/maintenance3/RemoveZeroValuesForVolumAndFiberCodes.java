package mx.com.liverpool.p360.services.core.temp.product2g.maintenance3;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class RemoveZeroValuesForVolumAndFiberCodes {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.List<String[]> volumeData = new java.util.ArrayList<>();
		java.util.List<String[]> fiberData = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("sqlrunner_20260315_130825.csv").toFile())))){
			String line = br.readLine();
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					pieces = rw.getRw().parseLine(line);
					volumeData.add(pieces);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("sqlrunner_20260315_130319.csv").toFile())))){
			String line = br.readLine();
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				if(!"".equals(line)) {
					pieces = rw.getRw().parseLine(line);
					fiberData.add(pieces);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.Collections.sort(volumeData, (o1,o2)->o1[0].compareTo(o2[0]));
		java.util.Collections.sort(fiberData, (o1,o2)->o1[0].compareTo(o2[0]));
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler rh  = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('VOLUMAtt',root,\"0000.0000.RK\",'VOLUMAtt',-1)")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		RequestHandler rh2 = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZVOLCJ',root,\"0000.0000.RK\",'ZVOLCJ',-1)")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		RequestHandler rh3 = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ZVOLPQ',root,\"0000.0000.RK\",'ZVOLPQ',-1)")), 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		for(String[] volume : volumeData) {
			if("ZVOLPQ".equals(volume[1])) {
				rh3.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + volume[0] + "'@1")).put("values", new org.json.JSONArray().put("")));
			}else if("ZVOLCJ".equals(volume[1])) {
				rh2.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + volume[0] + "'@1")).put("values", new org.json.JSONArray().put("")));
			}else if("VOLUMAtt".equals(volume[1])) {
				rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + volume[0] + "'@1")).put("values", new org.json.JSONArray().put("")));
			}
		}
		rh.sendData();
		rh2.sendData();
		rh3.sendData();
		RequestHandler rh4 = new RequestHandler( 
				new org.json.JSONArray()
					.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_PART1',root,\"0000.0000.RK\",'FIBER_PART1',-1)"))
					.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_PART2',root,\"0000.0000.RK\",'FIBER_PART2',-1)"))
					.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_PART3',root,\"0000.0000.RK\",'FIBER_PART3',-1)"))
					.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_PART4',root,\"0000.0000.RK\",'FIBER_PART4',-1)"))
					.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FIBER_PART5',root,\"0000.0000.RK\",'FIBER_PART5',-1)"))
			, 1000, request -> rw.writeData("list", "Product2G", null, qp, request, System.out::println) );
		boolean fp1 = false;
		boolean fp2 = false;
		boolean fp3 = false;
		boolean fp4 = false;
		boolean fp5 = false;
		String pid = null;
		int count = 0;
		for(String[] fibers : fiberData) {
			if(pid != null && !pid.equals(fibers[0])) {
				if(fp1 && fp2 && fp3 && fp4 && fp5) {
					rh4.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pid + "'@1")).put("values", new org.json.JSONArray().put("").put("").put("").put("").put("")));
					count++;
				}
				fp1 = false;
				fp2 = false;
				fp3 = false;
				fp4 = false;
				fp5 = false;
			}
			if("FIBER_PART1".equals(fibers[1])) {
				fp1 = true;
			}else if("FIBER_PART2".equals(fibers[1])) {
				fp2 = true;
			}else if("FIBER_PART3".equals(fibers[1])) {
				fp3 = true;
			}else if("FIBER_PART4".equals(fibers[1])) {
				fp4 = true;
			}else if("FIBER_PART5".equals(fibers[1])) {
				fp5 = true;
			}
			pid = fibers[0];
		}
		if(fp1 && fp2 && fp3 && fp4 && fp5) {
			rh4.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pid + "'@1")).put("values", new org.json.JSONArray().put("").put("").put("").put("").put("")));
			count++;
		}
		fp1 = false;
		fp2 = false;
		fp3 = false;
		fp4 = false;
		fp5 = false;
		rh4.sendData();
		System.out.println("Worked for: " + count + " product2G.");
	}
	
}
