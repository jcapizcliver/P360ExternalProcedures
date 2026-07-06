package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class DeleteProposalsWithLVP extends RESTWrapper {

	private static final String sa = PropertiesManager.get("p360.contingency.gcp.service_account_back");
	private static final String pubSubProject = PropertiesManager.get("p360.contingency.gcp.project_back");
	
	public static void main(String[] args) {
		DeleteProposalsWithLVP d = new DeleteProposalsWithLVP();
		java.util.Map<String, String> qpU = new java.util.TreeMap<>();
		qpU.put("includeObjectsInProtocol", "false");
		RequestHandler rhp = new RequestHandler(new org.json.JSONArray().put( new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")), 500, request -> d.writeData("list", "Product2G", null, qpU, request, System.out::println ));
		RequestHandler rha = new RequestHandler(new org.json.JSONArray().put( new org.json.JSONObject().put("identifier", "Article.CurrentStatus")), 500, request -> d.writeData("list", "Article", null, qpU, request, System.out::println ));
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo,Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
		qp.put("pageSize", "25000");
		java.util.Map<String, java.util.LinkedList<String>> skuProposalId = new java.util.TreeMap<>();
		System.out.println("Starting...");
		if(!java.nio.file.Files.exists(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "productosPorSKU"))) {
			System.out.println("No exists...");
			d.collectData("list", "Product2G", null, "byCatalog", qp, row->{
				org.json.JSONArray values = row.getJSONArray("values");
				String id = values.getString(0);
				String sku = values.getJSONArray(1).getString(0);
				java.util.LinkedList<String> ids = skuProposalId.get(sku);
				if(ids == null) {
					ids = new java.util.LinkedList<>();
					skuProposalId.put(sku, ids);
				}
				ids.addLast(id);
			}, System.out::println);
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "productosPorSKU").toFile())))){
				for(java.util.Map.Entry<String, java.util.LinkedList<String>> entry : skuProposalId.entrySet()) {
					if(entry.getValue().size() > 1)
						pw.println( d.getRw().serializeChunk( new String[] { entry.getKey(), d.getRw().serializeChunk(entry.getValue().toArray(new String[] {}), "\"", ";", "\\") } ) );
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}else {
			System.out.println("Exists...");
			try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "productosPorSKU"))){
				lns.forEach(s -> {
					String[] pieces = d.getRw().parseLine(s);
					String[] ids = d.getRw().parseLine(pieces[1],"\"", ";", "\\");
					java.util.LinkedList<String> lids = new java.util.LinkedList<>();
					for(int i=0; i<ids.length; i++) {
						lids.addLast(ids[i]);
					}
					skuProposalId.put(pieces[0], lids);
				});
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Found: " + skuProposalId.size());
		qp.put("fields", "Article.SupplierAID,ArticleCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)");
		qp.put("pageSize", "25000");
		java.util.Map<String, java.util.LinkedList<String>> skuSupplierAid = new java.util.TreeMap<>();
		if(!java.nio.file.Files.exists(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "articulosPorSKU"))) {
			d.collectData("list", "Article", null, "byCatalog", qp, row->{
				org.json.JSONArray values = row.getJSONArray("values");
				String id = values.getString(0);
				String sku = values.getJSONArray(1).getString(0);
				java.util.LinkedList<String> ids = skuSupplierAid.get(sku);
				if(ids == null) {
					ids = new java.util.LinkedList<>();
					skuSupplierAid.put(sku, ids);
				}
				ids.addLast(id);
			}, System.out::println);
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "articulosPorSKU").toFile())))){
				for(java.util.Map.Entry<String, java.util.LinkedList<String>> entry : skuSupplierAid.entrySet()) {
					if(entry.getValue().size() > 1) 
						pw.println( d.getRw().serializeChunk( new String[] { entry.getKey(), d.getRw().serializeChunk(entry.getValue().toArray(new String[] {}), "\"", ";", "\\") } ) );
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}else {
			try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "articulosPorSKU"))){
				lns.forEach(s -> {
					String[] pieces = d.getRw().parseLine(s);
					String[] ids = d.getRw().parseLine(pieces[1],"\"", ";", "\\");
					java.util.LinkedList<String> lids = new java.util.LinkedList<>();
					for(int i=0; i<ids.length; i++) {
						lids.addLast(ids[i]);
					}
					skuSupplierAid.put(pieces[0], lids);
				});
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println(skuProposalId);
		org.json.JSONObject message = new org.json.JSONObject();
		org.json.JSONArray products = new org.json.JSONArray();
		message.put("products", products);
		StringBuilder sb = new StringBuilder();
		int counter = 0;
		PubSubGCP pubSub = new PubSubGCP();
		java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
		System.out.println(skuProposalId.size());
		for(java.util.Map.Entry<String, java.util.LinkedList<String>> entry : skuProposalId.entrySet()) {
			if(entry.getValue().size() > 1) {
				for(String id : entry.getValue()) {
					System.out.println(id);
					if(id.startsWith("LVP") || id.startsWith("SBB")) {
						sb.append(sb.length() == 0 ? "" : ",");
						sb.append("'");
						sb.append(id);
						sb.append("'@1");
						products.put(new org.json.JSONObject()
								.put("proposalId", id)
								.put("entityType", "Generic")
								.put("internalStatus", "Eliminada"));
//						rhp.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(1025)));
						counter++;
						if(counter % 500 == 0) {
							qp0.put("items", sb.toString());
//							System.out.println("Gonna delete: " + sb.toString());
							d.deleteData("list", "Product2G", null, "byItems", qp0, System.out::println);
							sb.setLength(0);
							System.out.println("Writing to pub sub");
							pubSub.publishMessage(pubSubProject, "idmc_put_products", sa, message.toString());
							while(products.length() > 0) {
								products.remove(0);
							}
						}
					}
				}
			}
		}
		System.out.println("counter: " + counter);
//		rhp.sendData();
		if(counter % 500 != 0) {
			qp0.put("items", sb.toString());
			d.deleteData("list", "Product2G", null, "byItems", qp0, System.out::println);
			sb.setLength(0);
			System.out.println("Writing to pub sub");
			pubSub.publishMessage(pubSubProject, "idmc_put_products", sa, message.toString());
			while(products.length() > 0) {
				products.remove(0);
			}
		}
		
		for(java.util.Map.Entry<String, java.util.LinkedList<String>> entry : skuSupplierAid.entrySet()) {
			if(entry.getValue().size() > 1) {
				for(String id : entry.getValue()) {
					if(id.startsWith("LVP") || id.startsWith("SBB")) {
						sb.append(sb.length() == 0 ? "" : ",");
						sb.append("'");
						sb.append(id);
						sb.append("'@1");
//						rha.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", new org.json.JSONArray().put(1025)));
						counter++;
						if(counter % 500 == 0) {
							qp0.put("items", sb.toString());
							System.out.println("Gonna delete: " + sb.toString());
							d.deleteData("list", "Article", null, "byItems", qp0, System.out::println);
							sb.setLength(0);
						}
					}
				}
			}
		}
//		rha.sendData();
		if(counter % 500 != 0) {
			qp0.put("items", sb.toString());
			d.deleteData("list", "Article", null, "byItems", qp0, System.out::println);
			sb.setLength(0);
		}
		
	}
	
}
