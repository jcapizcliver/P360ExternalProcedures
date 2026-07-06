package mx.com.liverpool.p360.services.core.temp.product2g.maintenance4;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ListChild {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("pageSize", "2000");
		int[] arts = new int[] {0};
		arts[0] = 0;
		StringBuilder sb = new StringBuilder();
		int a = 0;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "MiniMonolit.csv").toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				a++;
				sb.append(sb.length() == 0 ? "" : ",").append("'").append(line).append("'@1");
				if(a % 1000 == 0) {
					qp.put("products", sb.toString());
					rw.collectData("list", "Article", null, "byProducts", qp, row -> {
						arts[0]++;
					});
					sb.setLength(0);
				}
			}
			if(sb.length() > 0) {
				qp.put("products", sb.toString());
				rw.collectData("list", "Article", null, "byProducts", qp, row -> {
					arts[0]++;
				});
				sb.setLength(0);
			}
			System.out.println("Found: " + args[0]);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
