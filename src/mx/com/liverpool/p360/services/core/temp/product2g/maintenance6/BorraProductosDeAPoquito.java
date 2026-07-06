package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class BorraProductosDeAPoquito {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		args = args.length == 0 ? new String[] { "C:\\opt\\LVP\\desorden\\PROD\\Variantes creadas como individuales.csv" } : args;
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("pageSize", "1000");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile())))){
			String line = null;
			int a = 0;
			StringBuilder sb = new StringBuilder();
			while((line = br.readLine()) != null) {
				sb.append(sb.length() == 0 ? "" : "," ).append("'").append(line).append("'@1");
				a++;
				if( a % 20 == 0 ) {
					qp.put("items", sb.toString());
					rw.deleteData("list", "Product2G", null, "byItems", qp, System.out::println);
					sb.setLength(0);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
