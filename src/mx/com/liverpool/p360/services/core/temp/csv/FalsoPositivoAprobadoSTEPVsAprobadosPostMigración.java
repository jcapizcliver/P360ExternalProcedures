package mx.com.liverpool.p360.services.core.temp.csv;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class FalsoPositivoAprobadoSTEPVsAprobadosPostMigración {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.List<String> trabajados = new java.util.ArrayList<>();
		java.util.List<String> falsoPositivo = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "involucradosEnAprobación").toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				trabajados.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "LosQueNoDebenTenerFirstDateApprove.txt").toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				falsoPositivo.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		String[] ref = trabajados.toArray(new String[] {});
		java.util.List<String> encontrados = new java.util.ArrayList<>();
		java.util.Arrays.sort(ref);
		java.util.List<String> products = new java.util.ArrayList<>();
		java.util.List<String> articles = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "pids.csv").toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				if(line.startsWith("S")) {
					products.add(line);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "aids.csv").toFile())))){
			String line = null;
			while((line = br.readLine()) != null) {
				if(line.startsWith("S")) {
					articles.add(line);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.List<String> forbiddenMemories = new java.util.ArrayList<>();
		String[] arrP = products.toArray(new String[] {});
		String[] arrA = articles.toArray(new String[] {});
		java.util.Arrays.sort(arrP);
		java.util.Arrays.sort(arrA);
		java.util.Map<String, String> qp0 = new java.util.HashMap<>();
		qp0.put("includeObjectsInProtocol", "false");
		RequestHandler rhP = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.FirstDateApproved")).put(new org.json.JSONObject().put("identifier", "Product2G.LastDateApproved")), 1000, request -> rw.writeData( "list", "Product2G", null, qp0, request, System.out::println));
		RequestHandler rhA = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.FirstDateApproved")).put(new org.json.JSONObject().put("identifier", "Article.LastDateApproved")), 1000, request -> rw.writeData( "list", "Article", null, qp0, request, System.out::println));
		boolean wasProduct = false;
		boolean wasArticle = false;
		System.out.println("Revisando " + falsoPositivo.size());
		for(String s : falsoPositivo) {
			if(java.util.Arrays.binarySearch(ref, s) > -1) {
				encontrados.add(s);
			}else {
				if( java.util.Arrays.binarySearch(arrP, s) > -1 ) {
					rhP.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + s + "'@1")).put("values", new org.json.JSONArray().put("").put("")));
					wasProduct = true;
				}
				if( java.util.Arrays.binarySearch(arrA, s) > -1 ) {
					rhA.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + s + "'@1")).put("values", new org.json.JSONArray().put("").put("")));
					wasArticle = true;
				}
				if(!wasProduct && !wasArticle) {
					forbiddenMemories.add(s);
				}
				wasProduct = false;
				wasArticle = false;
			}
		}
		rhP.sendData();
		rhA.sendData();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "FM").toFile())))){
			forbiddenMemories.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Encontrados: " + encontrados.size() + "/" + trabajados.size());
	}
	
}
