package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class EncuentraCosasConVariantes {
	
	private static final RESTWrapper rw = new RESTWrapper();
	private static int a = 0;
	
	public static void main(String[] args) {
		rw.getRw().setBaseUrl("https://172.18.237.210:1512/rest/V2.0");
		rw.getRw().addHeader("Authorization", java.util.Base64.getEncoder().encodeToString("rest:heiler".getBytes()));
		java.util.Map<String, String> qp = new java.util.HashMap<>();
		qp.put("pageSize", "2000");
		qp.put("fields",   "ProductReference.ReferencedSupplierAid");
		String[] array = new String[1000000];
		rw.collectData("list", "Article", "ProductReference", "withProduct", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			array[a] = values.getString(0);
			a++;
		});
		String[] array2 = java.util.Arrays.copyOf(array, a);
		java.util.Arrays.sort(array2);
		Object[][] pairs = new Object[a][];
		String prev = null;
		int b = 0;
		int c = 0;
		for(int i=0; i<array2.length; i++) {
			if(prev != null && !prev.equals(array2[i])) {
				pairs[b] = new Object[2];
				pairs[b][0] = prev;
				pairs[b][1] = c;
				b++;
				c = 0;
			}
			c++;
			prev = array2[i];
		}
		pairs[b] = new Object[2];
		pairs[b][0] = prev;
		pairs[b][1] = c;
		b++;
		c = 0;
		Object[][] pairs2 = java.util.Arrays.copyOf(pairs, b);
		java.util.Arrays.sort(pairs2, (o1,o2)-> Integer.compare( (int)o1[1], (int)o2[1]) );
		for(int i=0; i<pairs2.length; i++) {
			System.out.println(pairs2[i][0] + " - " + pairs2[i][1]);
		}
	}
}
