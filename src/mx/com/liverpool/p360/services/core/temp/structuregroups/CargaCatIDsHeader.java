package mx.com.liverpool.p360.services.core.temp.structuregroups;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class CargaCatIDsHeader {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "OtrosCatIDs.csv").toFile())))){
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "clasificacion_unica_5.csv").toFile())))){
			String line = br.readLine();
			String[] header = rw.getRw().parseLine(line);
			String[] pieces = null;
			String[] subs = null;
			int a = 0;
			java.util.Map<String, String> qp0 = new java.util.HashMap<>();
			java.util.Set<String> ids = new java.util.TreeSet<>();
			RequestHandler rh = new RequestHandler( new org.json.JSONArray()
					.put(new org.json.JSONObject().put("identifier", "Product2GStructureMap.ManualMap('Sitios Web')"))
					, 2000, request -> rw.writeData("list", "Product2G", null, qp0, request, System.out::println) );
			while((line = br.readLine()) != null) {
				pieces = rw.getRw().parseLine(line);
				if(pieces.length > 1) {
					subs = rw.getRw().parseLine(pieces[1], "\"", ";", "\\");
					org.json.JSONArray vals = new org.json.JSONArray();
					for(int i=0; i<subs.length; i++) {
						vals.put(subs[i]);
					}
					ids.add(pieces[0]);
					org.json.JSONObject ob = new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[0] + "'@1")).put("values", new org.json.JSONArray().put(vals));
//					System.out.println(ob);
					if(vals.length() > 0)
						rh.addRow(ob);
				}else {
					a++;
				}
			}
			rh.sendData();
			System.out.println("Productos netos: " + ids.size());
			System.out.println("Missings: " + a);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
