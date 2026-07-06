package mx.com.liverpool.p360.services.core.net;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class Loader extends RESTWrapper {

	public static void main(String[] args) {
		Loader l = new Loader();
		boolean b = args.length > 0 && Boolean.parseBoolean(args[0]);
		java.nio.file.Path p = java.nio.file.Paths.get("pez");
		int bs = 25000;
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		java.util.Map<String, String> qp1 = new java.util.TreeMap<>();
		qp1.put("query", "not ProductReference.ReferencedSupplierAid is empty");
		qp1.put("pageSize", String.valueOf(bs));
		qp1.put("fields", "ProductReference.ReferencedSupplierAid");
		if(b && java.nio.file.Files.exists(p)) {
			System.out.println("Readng existing...");
			try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(p)){
				lns.map(l.getRw()::parseLine).forEach( a -> data.put(a[0], a[1]) );
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		if(b && !java.nio.file.Files.exists(p)) {
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(p.toFile())))){
				l.collectData("list", "Article", "ProductReference", "bySearch", qp1, row->{
					org.json.JSONArray values = row.getJSONArray("values");
					data.put(row.getJSONObject("object").getString("id"), values.getString(0));
					pw.println( l.getRw().serializeChunk( new String[] { row.getJSONObject("object").getString("id"), values.getString(0) } ) ); 
				}, System.out::println);
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
		qp.put("query", "not characteristic('SKU') is empty");
		qp.put("pageSize", String.valueOf( bs ));
		int[] counter = new int[1];
		counter[0] = 0;
		StringBuilder sb = new StringBuilder();
		org.json.JSONArray items = new org.json.JSONArray();
		l.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			sb.append(counter[0] == 0 ? "" : ",");
			sb.append("'");
			sb.append(values.getString(0));
			sb.append("'@1");
			items.put(new org.json.JSONObject().put("productNo", values.getString(0)).put("sku", values.getJSONArray(1).getString(0)));
			counter[0] ++;
			if(counter[0] == 1000) {
				try(
					java.net.Socket socket = new java.net.Socket("localhost", 23540);
					java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
					java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream()), true)
				){
					System.out.println("Sending " + bs + " products...");
					pw.println( new org.json.JSONObject().put("items", items).put("action", "skuProductNo") );
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}
				while(items.length() > 0) {
					items.remove(0);
				}
				counter[0] = 0;
				java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
				qp0.put("products", sb.toString());
				qp0.put("fields", "Article.SupplierAID,ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
				qp0.put("pageSize", String.valueOf( bs ));
				l.collectData("list", "Article", null, "byProducts", qp0, row0 -> {
					org.json.JSONArray values0 = row0.getJSONArray("values");
					String prod = data.get(row0.getJSONObject("object").getString("id"));
					if(prod != null) {
						items.put(new org.json.JSONObject().put("supplierAID", values0.getString(0))
								.put("sku", values0.getJSONArray(1).getString(0)).put("productNo", prod));
					}
				}, System.out::println);
				try(
						java.net.Socket socket = new java.net.Socket("localhost", 23540);
						java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
						java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream()), true)
					){
						System.out.println("Sending " + bs + " variants from previous products...");
						pw.println( new org.json.JSONObject().put("items", items).put("action", "skuSupplierAID") );
					}catch(java.io.IOException e) {
						e.printStackTrace();
					}
					while(items.length() > 0) {
						items.remove(0);
					}
				sb.setLength(0);
			}
		}, System.out::println);
		if(counter[0] > 0) {
			try(
				java.net.Socket socket = new java.net.Socket("localhost", 23540);
				java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
				java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream()), true)
			){
				System.out.println("Sending " + bs + " products...");
				pw.println( new org.json.JSONObject().put("items", items).put("action", "skuProductNo") );
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
			while(items.length() > 0) {
				items.remove(0);
			}
			counter[0] = 0;
			java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
			qp0.put("products", sb.toString());
			qp0.put("fields", "Article.SupplierAID,ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
			qp0.put("pageSize", String.valueOf( bs ));
			l.collectData("list", "Article", null, "byProducts", qp0, row0 -> {
				org.json.JSONArray values0 = row0.getJSONArray("values");
				String prod = data.get(row0.getJSONObject("object").getString("id"));
				if(prod != null) {
					items.put(new org.json.JSONObject().put("supplierAID", values0.getString(0)).put("sku", values0.getJSONArray(1).getString(0)).put("productNo", prod));
				}
			}, System.out::println);
			try(
					java.net.Socket socket = new java.net.Socket("localhost", 23540);
					java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
					java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream()), true)
			){
				System.out.println("Sending " + bs + " variants from previous products...");
				pw.println( new org.json.JSONObject().put("items", items).put("action", "skuSupplierAID") );
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
			while(items.length() > 0) {
				items.remove(0);
			}
			sb.setLength(0);
		}
	}
	
}
