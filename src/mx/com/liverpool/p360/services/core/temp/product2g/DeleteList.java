package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class DeleteList {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		StringBuilder sb = new StringBuilder();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeObjectsInProtocol", "false");
		int cnt = 0;
		try(java.io.BufferedReader br  = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\desorden\\hola\\MissunderstoodIndividualsToDelete")))){
			String line = null;
			while((line = br.readLine()) != null) {
				sb.append(cnt == 0 ? "" : ",").append("'").append(line).append("'@1");
				cnt++;
				if(cnt == 1000) {
					qp.put("items", sb.toString());
					rw.deleteData("list", "Product2G", null, "byItems", qp, System.out::println);
					cnt = 0;
					sb.setLength(0);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		if(cnt > 0) {
			qp.put("items", sb.toString());
			rw.deleteData("list", "Product2G", null, "byItems", qp, System.out::println);
		}
	}
	
}
