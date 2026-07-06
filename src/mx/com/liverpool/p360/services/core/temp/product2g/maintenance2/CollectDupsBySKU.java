package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CollectDupsBySKU {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID,Article.SKU");
		qp.put("pageSize", "20000");
		qp.put("query", "not Article.SKU is empty");
		java.util.List<String[]> data = new java.util.ArrayList<>();
		rw.collectData("list", "Article", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			data.add(new String[] { values.getString(0), values.getString(1) });
		});
		java.util.List<String> toDelete = new java.util.ArrayList<>();
		java.util.Collections.sort(data, (o1,o2)-> o1[1].compareTo(o2[1]));
		String[] p = null;
		java.util.List<String[]> current = new java.util.ArrayList<>();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("ArticleSKUs.txt").toFile())))){
			for(String[] d : data) {
				if(p != null && !p[1].equals(d[1])) {
					current.forEach(pw::println);
					for(String[] s : current) {
						if(!s[0].startsWith("S")) {
							toDelete.add(s[0]);
						}
					}
					current.clear();
				}
				current.add(d);
				p = d;
			}
			if(!current.isEmpty()) {
				for(String[] s : current) {
					if(!s[0].startsWith("S")) {
						toDelete.add(s[0]);
					}
				}
				current.forEach(pw::println);
				current.clear();
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Now deleting article (" + toDelete.size() + ")");
		StringBuilder sb = new StringBuilder();
		int a = 0;
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("pageSize", "2000");
		for(String s : toDelete) {
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append(s);
			if(a % 2000 == 0) {
				qp0.put("items", sb.toString());
				rw.deleteData("list", "Article", null, "byItems", qp0, System.out::println);
				sb.setLength(0);
			}
		}
		if(a % 2000 != 0) {
			qp0.put("items", sb.toString());
			rw.deleteData("list", "Article", null, "byItems", qp0, System.out::println);
			sb.setLength(0);
		}
		onProducts();
	}
	
	private static void onProducts() {
		System.out.println("Now on products.");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2G.SKU");
		qp.put("pageSize", "20000");
		java.util.List<String[]> data = new java.util.ArrayList<>();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			if(!"".equals(values.getJSONArray(1).getString(0)))
				data.add(new String[] { values.getString(0), values.getJSONArray(1).getString(0) });
		});
		java.util.List<String> toDelete = new java.util.ArrayList<>();
		java.util.Collections.sort(data, (o1,o2)-> o1[1].compareTo(o2[1]));
		String[] p = null;
		java.util.List<String[]> current = new java.util.ArrayList<>();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("Product2GSKUs.txt").toFile())))){
			for(String[] d : data) {
				if(p != null && !p[1].equals(d[1])) {
					current.forEach(pw::println);
					for(String[] s : current) {
						if(!s[0].startsWith("S")) {
							toDelete.add(s[0]);
						}
					}
					current.clear();
				}
				current.add(d);
				p = d;
			}
			if(!current.isEmpty()) {
				for(String[] s : current) {
					if(!s[0].startsWith("S")) {
						toDelete.add(s[0]);
					}
				}
				current.forEach(pw::println);
				current.clear();
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Now deleting product2g (" + toDelete.size() + ")");
		StringBuilder sb = new StringBuilder();
		int a = 0;
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("pageSize", "2000");
		for(String s : toDelete) {
			sb.append(sb.length() == 0 ? "" : ",");
			sb.append(s);
			if(a % 2000 == 0) {
				qp0.put("items", sb.toString());
				rw.deleteData("list", "Product2G", null, "byItems", qp0, System.out::println);
				sb.setLength(0);
			}
		}
		if(a % 2000 != 0) {
			qp0.put("items", sb.toString());
			rw.deleteData("list", "Product2G", null, "byItems", qp0, System.out::println);
			sb.setLength(0);
		}
	}
	
}
