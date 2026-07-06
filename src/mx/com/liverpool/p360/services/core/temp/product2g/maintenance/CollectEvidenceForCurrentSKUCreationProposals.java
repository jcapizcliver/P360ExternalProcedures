package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import java.util.MissingFormatArgumentException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CollectEvidenceForCurrentSKUCreationProposals {

	
	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.util.Set<String> toSKU = new java.util.TreeSet<>();
		java.util.LinkedList<String> lst = new java.util.LinkedList<>();
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("/", "u01", "workshop", "tmp", "lalista"))){
			lns.forEach(lst::addLast);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.io.File[] toSKUCreationFiles = java.nio.file.Paths.get("/", "u01", "stage", "ECC_SKU").toFile().listFiles();
		for(java.io.File f : toSKUCreationFiles) {
			toSKU.add(f.getName().replaceAll("__.+", ""));
		}
		toSKU.remove("null");
		int a = 0;
		RESTWrapper rw = new RESTWrapper();
		RESTWorkshop workshop = rw.getRw();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("entityFilter", "Product2G");
		org.json.JSONObject response = null;
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("(.+)(?=(\\.))");
		java.util.regex.Matcher m = null;
		String statusModification = null;
		java.util.LinkedList<String> fueronEnviados = new java.util.LinkedList<>();
		java.util.LinkedList<String> noFueronEnviados = new java.util.LinkedList<>();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("/", "u01", "workshop", "missingCrearArchivosParaSKU").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			for(String ofInterest : lst) {
				if(!toSKU.contains(ofInterest)) {
					response = workshop.makeRequest("GET", "/object/Product2G/'" + ofInterest + "'@1", qp, null);
					statusModification = response.getJSONObject("_data").getString("statusModification");
					m = p.matcher(statusModification);
					if(m.find()) {
						pw.println(ofInterest + " - " + m.group());
					}else {
						System.out.println("PANIC: " + statusModification);
					}
					noFueronEnviados.addLast(ofInterest);
					a++;
				}else {
					fueronEnviados.addLast(ofInterest);
				}
			}
		}catch(java.io.IOException e){
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("/", "u01", "workshop", "fueronEnviadosECC").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			for(String ofInterest : fueronEnviados) {
				pw.println(ofInterest);
			}
		}catch(java.io.IOException e){
			e.printStackTrace();
		}
		System.out.println(a);
		toSKU.clear();
		java.util.Map<String, String> heavyMap122 = new java.util.TreeMap<>();
		System.out.println("Now reading response files (122)");
		java.io.File[] files = java.nio.file.Paths.get("/", "u01", "stage", "ECC_122", "processed").toFile().listFiles(ff -> ff.getName().endsWith(".XML"));
		for(java.io.File f : files) {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(f)))){
				String line = null;
				StringBuilder sb = new StringBuilder();
				while((line = br.readLine()) != null) {
					sb.append(line);
				}
				heavyMap122.put(f.getAbsolutePath(), sb.toString());
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		files = java.nio.file.Paths.get("/", "u01", "stage", "SBB_122", "processed").toFile().listFiles(ff -> ff.getName().endsWith(".XML"));
		for(java.io.File f : files) {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(f)))){
				String line = null;
				StringBuilder sb = new StringBuilder();
				while((line = br.readLine()) != null) {
					sb.append(line);
				}
				heavyMap122.put(f.getAbsolutePath(), sb.toString());
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Now reading error files");
		files = java.nio.file.Paths.get("/", "u01", "stage", "ECC_ERR", "processed").toFile().listFiles(ff -> ff.getName().endsWith(".XML"));
		for(java.io.File f : files) {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(f)))){
				String line = null;
				StringBuilder sb = new StringBuilder();
				while((line = br.readLine()) != null) {
					sb.append(line);
				}
				heavyMap122.put(f.getAbsolutePath(), sb.toString());
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Now searching...");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("/", "u01", "workshop", "responsePresence").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			for(String ofInterest : lst) {
				for(java.util.Map.Entry<String, String> entry : heavyMap122.entrySet()) {
					if(entry.getValue().contains(ofInterest)) {
						pw.println(ofInterest + " - " + entry.getKey() + " - " + ( fueronEnviados.contains(ofInterest) ) + " - " + noFueronEnviados.contains(ofInterest));
					}
				}
			}
		}catch(java.io.IOException e){
			e.printStackTrace();
		}
		System.out.println("Done. " + workshop.formatTime(System.currentTimeMillis() - init));
	}
	
}
