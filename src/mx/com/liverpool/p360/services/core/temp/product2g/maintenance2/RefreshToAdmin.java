package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class RefreshToAdmin {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		if(args.length == 1) {
			refreshDataForSupplier(args[0]);
		}else {
			java.util.concurrent.ArrayBlockingQueue<String> packs = new java.util.concurrent.ArrayBlockingQueue<String>(18);
			Worker[] workers = new Worker[3];
			Thread[] threads = new Thread[workers.length];
			for(int i=0; i<workers.length; i++) {
				workers[i] = new Worker(packs);
				workers[i].foil = i+1;
				threads[i] = new Thread(workers[i]);
				threads[i].setPriority(Thread.currentThread().getPriority() - 1);
				threads[i].setDaemon(true);
				threads[i].start();
			}
			java.util.ArrayList<String> ei = new java.util.ArrayList<>();
//			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("proveedores").toFile())))){
//				String line = null;
//				while((line = br.readLine()) != null) {
//					ei.add(line);
//				}
//			}catch(java.io.IOException e) {
//				e.printStackTrace();
//			}
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("/", "u01", "stage", "cache", "proveedores_migrados").toFile())))){
				String line = null;
				System.out.println("Now reading...");
				java.util.Set<String> hola = new java.util.TreeSet<>(ei);
				while((line = br.readLine()) != null) {
					if(!hola.contains(line)) {
						try{
							packs.put(line);
						}catch(InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
			Worker meWorker = new Worker(packs);
			meWorker.running = false;
			meWorker.run();
			for(int i=0; i<workers.length; i++) {
				workers[i].running = false;
			}
			System.out.println("Now waiting othres to finish...");
			for(int i=0; i<threads.length; i++) {
				try {
					threads[i].join();
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Done.");
		}
	}
	
	private static class Worker implements Runnable{
		
		private java.util.concurrent.ArrayBlockingQueue<String> packs;
		private boolean running = true;
		private int foil = -1;
		
		public Worker(java.util.concurrent.ArrayBlockingQueue<String> packs) {
			this.packs = packs;
		}
		
		@Override
		public void run() {
			System.out.println("Now running... (" + foil + ")");
			String supplier = null;
			while(running || !packs.isEmpty()) {
				try {
					supplier = packs.poll(10, java.util.concurrent.TimeUnit.MILLISECONDS);
					if(supplier != null)
						refreshDataForSupplier(supplier);
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Done with " + foil);
		}
		
	}
	
	private static void refreshDataForSupplier(String supplier) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "Product2G.ProductNo"
				+ ",Product2GCharacteristicValue.LookupValue('Section',root,\"0000.0000.RK\",'Section')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('ItemGroupS4H',root,\"0000.0000.RK\",'ItemGroupS4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('BrandName',root,\"0000.0000.RK\",'BrandName')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('BRAND_ID_S4H',root,\"0000.0000.RK\",'BRAND_ID_S4H')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('SupplierID',root,\"0000.0000.RK\",'SupplierID',-1)"
				+ ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
				+ ",Product2GStructureMap.StructureGroup('PrimaryProductTaxonomy')->StructureGroup.Identifier"
				+ ",Product2G.CurrentStatus"
				+ ",Product2GCharacteristicValueLang.Value('AssignTakeNoTake',root,\"0000.0000.RK\",'AssignTakeNoTake',-1)"
				+ ",Product2GCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('FotoTomadaLiverpool',root,\"0000.0000.RK\",'FotoTomadaLiverpool',-1)->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"
				+ ",Product2GCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"
			);
		qp.put("pageSize", "5000");
		System.out.println("Going on " + supplier);
//		qp.put("query", "Product2GExtraData.SupplierID(MX)->LookupValue.Code = \"" + supplier + "\" and Product2G.ProductNo startsWith \"S\" and (Product2G.CurrentStatus = \"Aprobada\")");
		qp.put("query", "Product2GExtraData.SupplierID(MX)->LookupValue.Code = \"" + supplier + "\" and Product2G.ProductNo startsWith \"S\" and (Product2G.CurrentStatus = \"Pendiente Inicio Enriquecimiento\")");
		DataRequestor dr = new DataRequestor();
		org.json.JSONArray items = new org.json.JSONArray();
		java.util.List<String> pids = new java.util.ArrayList<>();
		java.util.Map<String, String> internalToExternal = new java.util.HashMap<>();
		rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
			pids.add(row.getJSONObject("object").getString("id"));
			org.json.JSONArray values = row.getJSONArray("values");
			org.json.JSONObject obj = new org.json.JSONObject();
			obj.put("product", values.getString(0));
			obj.put("Section", values.getJSONArray(1).get(0));
			obj.put("ItemGroup", values.getJSONArray(2).get(0));
			obj.put("ItemGroupS4H", values.getJSONArray(3).get(0));
			obj.put("BrandName", values.getJSONArray(4).get(0));
			obj.put("BRAND_ID_S4H", values.getJSONArray(5).get(0));
			obj.put("Business", values.getJSONArray(6).get(0));
			obj.put("SupplierID", values.getJSONArray(7).get(0));
			obj.put("SKU", values.getJSONArray(8).getString(0));
			obj.put("Template", values.getJSONArray(9).get(0));
			obj.put("CurrentStatus", values.getString(10));
			obj.put("AssignTakeNoTake", values.getJSONArray(11).get(0));
			obj.put("SAPObjectType", values.getJSONArray(12).get(0));
			obj.put("FotoTomadaLiverpool", String.valueOf( values.getJSONArray(13).get(0) ));
			obj.put("MainBarCode", values.getJSONArray(14).get(0));
			obj.put("MainBarCodeS4H", values.getJSONArray(15).get(0));
			items.put(obj);
			internalToExternal.put(row.getJSONObject("object").getString("id"), values.getString(0));
			if(items.length() == 5000) {
				dr.putProductData(items);
				while(items.length() > 0) {
					items.remove(0);
				}
			}
		}, System.out::println);
		if(items.length() > 0) {
			dr.putProductData(items);
			while(items.length() > 0) {
				items.remove(0);
			}
		}
		
		org.json.JSONArray itemsA = new org.json.JSONArray();
		qp = new java.util.TreeMap<>();
		qp.put("fields", 
			   "Article.SupplierAID"
			+ ",ArticleCharacteristicValue.LookupValue('TamanoUnico',root,\"0000.0000.RK\",'TamanoUnico')->LookupValue.Code"
			+ ",ArticleCharacteristicValue.LookupValue('ColoursLiverpoolAtt',root,\"0000.0000.RK\",'ColoursLiverpoolAtt')->LookupValue.Code"
			+ ",ArticleCharacteristicValueLang.Value('ProductImage',\"0000.0000.RK\",\"0000.0000.RK\",'ProductImage_URL',-1)"
			+ ",ArticleCharacteristicValueLang.Value('AssignTakeNoTake',root,\"0000.0000.RK\",'AssignTakeNoTake',-1)"
			+ ",ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
			+ ",ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)"
			+ ",ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)"
		);
		qp.put("pageSize", "5000");
		StringBuilder sb = new StringBuilder();
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("fields", "ProductReference.ReferencedSupplierAid");
		qp0.put("pageSize", "5000");
		int i = 0;
		java.util.Map<String, org.json.JSONObject> articleToObjects = new java.util.HashMap<>();
		for(String pid : pids) {
			sb.append(sb.length() == 0 ? "" : ",").append(pid);
			i++;
			if(i % 1000 == 0) {
				qp.put("products", sb.toString());
				rw.collectData("list", "Article", null, "byProducts", qp, row->{
					org.json.JSONObject obj = new org.json.JSONObject();
					org.json.JSONArray values = row.getJSONArray("values");
					obj.put("variant", values.getString(0));
					obj.put("ColoursLiverpoolAtt", values.getJSONArray(2).getString(0));
					obj.put("TamanoUnico", values.getJSONArray(1).getString(0));
					obj.put("ProductImage", values.getJSONArray(3).getString(0));
					obj.put("AssignTakeNoTake", values.getJSONArray(4).getString(0));
					obj.put("SKU", values.getJSONArray(5).getString(0));
					obj.put("MainBarCode", values.getJSONArray(6).getString(0));
					obj.put("MainBarCodeS4H", values.getJSONArray(7).getString(0));
					articleToObjects.put(row.getJSONObject("object").getString("id"), obj);
				}, System.out::println);
				qp0.put("products", sb.toString());
				rw.collectData("list", "Article", "ProductReference", "byProducts", qp0, row->{
					org.json.JSONArray values = row.getJSONArray("values");
					org.json.JSONObject obj = articleToObjects.get(row.getJSONObject("object").getString("id"));
					if(obj != null) {
						obj.put("ProductNo", values.getString(0));
						itemsA.put(obj);
						if(itemsA.length() == 5000) {
							dr.putArticleData(itemsA);
							while(itemsA.length() > 0) {
								itemsA.remove(0);
							}
						}
					}else {
						System.out.println("PANIC, not fount: " + row.getJSONObject("object").getString("id"));
					}
				}, System.out::println);
				sb.setLength(0);
				articleToObjects.clear();
			}
		}
		if(sb.length() > 0) {
			qp.put("products", sb.toString());
			rw.collectData("list", "Article", null, "byProducts", qp, row->{
				org.json.JSONObject obj = new org.json.JSONObject();
				org.json.JSONArray values = row.getJSONArray("values");
				obj.put("variant", values.getString(0));
				obj.put("ColoursLiverpoolAtt", values.getJSONArray(2).getString(0));
				obj.put("TamanoUnico", values.getJSONArray(1).getString(0));
				obj.put("ProductImage", values.getJSONArray(3).getString(0));
				obj.put("AssignTakeNoTake", values.getJSONArray(4).getString(0));
				obj.put("SKU", values.getJSONArray(5).getString(0));
				obj.put("MainBarCode", values.getJSONArray(6).getString(0));
				obj.put("MainBarCodeS4H", values.getJSONArray(7).getString(0));
				articleToObjects.put(row.getJSONObject("object").getString("id"), obj);
			}, System.out::println);
			qp0.put("products", sb.toString());
			rw.collectData("list", "Article", "ProductReference", "byProducts", qp0, row->{
				org.json.JSONArray values = row.getJSONArray("values");
				org.json.JSONObject obj = articleToObjects.get(row.getJSONObject("object").getString("id"));
				if(obj != null) {
					obj.put("ProductNo", values.getString(0));
					itemsA.put(obj);
					if(itemsA.length() == 5000) {
						dr.putArticleData(itemsA);
						while(itemsA.length() > 0) {
							itemsA.remove(0);
						}
					}
				}else {
					System.out.println("PANIC, not fount: " + row.getJSONObject("object").getString("id"));
				}
			}, System.out::println);
			sb.setLength(0);
			articleToObjects.clear();
		}
		if(itemsA.length() > 0) {
			dr.putArticleData(itemsA);
			while(itemsA.length() > 0) {
				itemsA.remove(0);
			}
		}
		
	}
	
}
