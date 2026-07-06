package mx.com.liverpool.p360.services.core.temp.product2g.maintenance2;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class EnlistaMismosSKUs {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qpp = new java.util.TreeMap<>();
		qpp.put("fields", "Product2G.ProductNo,Product2G.SKU");
		qpp.put("query", "not Product2G.SKU is empty");
		qpp.put("pageSize", "5000");
		java.util.List<String[]> pids = new java.util.ArrayList<>();
		Thread tp = new Thread( () -> {
			rw.collectData("list", "Product2G", null, "bySearch", qpp, row -> pids.add(new String[] { row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1) }));
		} );
		tp.setDaemon(true);
		tp.setPriority(Thread.currentThread().getPriority() - 1);
		tp.start();
		java.util.Map<String, String> qpa = new java.util.TreeMap<>();
		qpa.put("fields", "Article.SupplierAID,Article.SKU");
		qpa.put("query", "not Article.SKU is empty and ");
		qpa.put("pageSize", "5000");
		java.util.List<String[]> aids = new java.util.ArrayList<>();
		Thread ta = new Thread( () -> {
			rw.collectData("list", "Article", null, "bySearch", qpa, row -> aids.add(new String[] { row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1) }));
		} );
		ta.setDaemon(true);
		ta.setPriority(Thread.currentThread().getPriority() - 1);
		ta.start();
		try {
			tp.join();
			System.out.println("Product finished collecting data... " + pids.size());
		}catch(InterruptedException e) { e.printStackTrace(); }
		try {
			ta.join();
			System.out.println("Article finished collecting data... " + aids.size());
		}catch(InterruptedException e) { e.printStackTrace(); }
		java.util.Collections.sort(pids, (o1,o2)->o1[1].compareTo(o2[1]));
		java.util.Collections.sort(aids, (o1,o2)->o1[1].compareTo(o2[1]));
		String[] ppid = null;
		java.util.List<String> ostias = new java.util.ArrayList<>();
		int laOstia = 0;
		String ostia = null;
		try(
			java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("ProductNoToDelete.txt").toFile())));
			java.io.PrintWriter pwO = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("ProductNoToDelete.txt").toFile())));
		){
			for(String[] id : pids) {
				if(ppid != null && !ppid[1].equals(id[1])) {
					if(ostias.size() > 1) {
						for(int i=0; i<ostias.size(); i++) {
							if(ostias.get(i).startsWith("S")) {
								laOstia = i;
							}
						}
						ostia = ostias.remove(laOstia);
						pwO.print(ostia);
						laOstia = 0;
						ostias.forEach(pw::println);
						ostia = null;
					}
					ostias.clear();
				}
				ostias.add(id[0]);
				ppid = id;
			}
			if(ostias.size() > 1) {
				for(int i=0; i<ostias.size(); i++) {
					if(ostias.get(i).startsWith("S")) {
						laOstia = i;
					}
				}
				ostia = ostias.remove(laOstia);
				pwO.print(ostia);
				laOstia = 0;
				ostias.forEach(pw::println);
				ostia = null;
			}
			ostias.clear();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		pids.clear();
		String[] paid = null;
		try(
			java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("SupplierAIDToDelete.txt").toFile())));
			java.io.PrintWriter pwO = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("SupplierAIDThatSurvived.txt").toFile())));
		){
			for(String[] id : aids) {
				if(paid != null && !paid[1].equals(id[1])) {
					if(ostias.size() > 1) {
						for(int i=0; i<ostias.size(); i++) {
							if(ostias.get(i).startsWith("S")) {
								laOstia = i;
							}
						}
						ostia = ostias.remove(laOstia);
						pwO.print(ostia);
						laOstia = 0;
						ostias.forEach(pw::println);
						ostia = null;
					}
					ostias.clear();
				}
				ostias.add(id[0]);
				paid = id;
			}
			if(ostias.size() > 1) {
				for(int i=0; i<ostias.size(); i++) {
					if(ostias.get(i).startsWith("S")) {
						laOstia = i;
					}
				}
				ostia = ostias.remove(laOstia);
				pwO.print(ostia);
				laOstia = 0;
				ostias.forEach(pw::println);
				ostia = null;
			}
			ostias.clear();
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
