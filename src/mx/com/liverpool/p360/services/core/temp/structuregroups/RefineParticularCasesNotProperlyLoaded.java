package mx.com.liverpool.p360.services.core.temp.structuregroups;

import java.io.IOException;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class RefineParticularCasesNotProperlyLoaded {

	private static final RESTWorkshop rw = new RESTWorkshop();
	
	public static void main(String[] args) {
		rw.addHeader("Authorization", "Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
		rw.setBaseUrl(PropertiesManager.get("p360.contingency.base_url"));
		RefineParticularCasesNotProperlyLoaded r = new RefineParticularCasesNotProperlyLoaded();
		java.util.LinkedList<String> data = new java.util.LinkedList<>();
		r.collectStructureGroupsWithStructureAsDirectParent(data);
		System.out.println("Data: " + data.size());
		System.out.println(data.getFirst());
//		r.findAndCopyFiles(data, "/u01/stage/ECC_140/processed", "GPOARTP360");
	}
	
	private void findAndCopyFiles(java.util.LinkedList<String> data, String sourceDir, String preffix) {
		java.io.File[] files = new java.io.File(sourceDir).listFiles(ff->ff.getName().startsWith(preffix) && ff.getName().toUpperCase().endsWith(".XML"));
		java.nio.file.Path tp = java.nio.file.Paths.get("/", "u01", "workshop", "data", "missing_structure_groups");
		try {
			java.nio.file.Files.createDirectories(tp);
			System.out.println("Dir created: " + tp.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		int total = data.size();
		int current = 0;
		java.util.Set<String> alreadyCoppied = new java.util.TreeSet<>();
		java.util.Map<String, String> fileContents = new java.util.TreeMap<>();
		for(String sgId : data) {
			current++;
			for(java.io.File f : files) {
				if(containsString(sgId, f, fileContents)) {
					if(!alreadyCoppied.contains(f.getName()))
					try {
						java.nio.file.Files.copy(java.nio.file.Paths.get(sourceDir, f.getName()), java.nio.file.Paths.get(tp.toString(), f.getName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING );
						System.out.println("Copied: " + f.getAbsolutePath());
						alreadyCoppied.add(f.getName());
					} catch (IOException e) {
						e.printStackTrace();
					}
					break;
				}
			}
			System.out.println(current + "/" + total);
		}
	}
	
	private boolean containsString(String str, java.io.File f, java.util.Map<String, String> fileContents) {
		String content = fileContents.get(f.getName());
		if(content != null) {
			return content.contains(">" + str + "<");
		}else {
			boolean exists = false;
			try(
				java.io.FileInputStream fis = new java.io.FileInputStream(f);
				java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()
			){
				int length;
				byte[] chunk = new byte[1024];
				while((length = fis.read(chunk)) != -1) {
					baos.write(chunk, 0, length);
				}
				content = baos.toString(java.nio.charset.StandardCharsets.UTF_8);
				fileContents.put(f.getName(), content);
				exists = content.contains(">" + str + "<");
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
			return exists;
		}
	}
	
	private void collectStructureGroupsWithStructureAsDirectParent(java.util.LinkedList<String> data) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("structure", "CommercialECC");
		qp.put("fields", "StructureGroup.Identifier");
		qp.put("query", "StructureGroup.ParentIdentifier = \"Structure_1754611647119004\"");
		qp.put("pageSize", "500");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int a = 0;
		int b = 0;
		do {
			qp.put("startIndex", String.valueOf(a));
			response = rw.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
			if(response != null && response.has("totalSize")) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					if(!values.getString(0).endsWith("-L1ECC"))
						data.addLast(values.getString(0).replaceAll("-.+", ""));
				}
				a += response.getInt("pageSize");
			}else {
				System.out.println("PROBLEM: " + rw.getRawResponse());
			}
		}while(a < b);
		a = 0;
	}
	
	
}
