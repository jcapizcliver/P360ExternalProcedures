package mx.com.liverpool.dataprofiling.preparison;

import java.io.IOException;
import java.nio.file.StandardCopyOption;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class RenameTemplatesFileToAddLabel {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StructureGroup.Identifier,StructureGroupLang.Name(es)");
		qp.put("query", "StructureGroup.Identifier wildcard \"EU4-%\"");
		qp.put("structure", "PrimaryProductTaxonomy");
		qp.put("pageSize", "5000");
		java.util.Map<String, String> templates = new java.util.TreeMap<>();
		rw.collectData("list", "StructureGroup", null, "bySearch", qp, row -> {
			org.json.JSONArray values = row.getJSONArray("values");
			templates.put(values.getString(0), values.getString(1));
//			System.out.println(values.getString(0) + " - " + values.getString(1));
		} );
//		System.exit(0);
		java.io.File[] files = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "templateAsTablesOnlyProducts").toFile().listFiles(ff->ff.getName().endsWith(".csv"));
		String label = null;
		String fileNamePelado = null;
		for(java.io.File f : files)
		try {
			fileNamePelado = f.getName().replaceAll("(\\.csv)$", "");
			label = templates.get(fileNamePelado);
			if(label != null) {
				System.out.println("This was: " + label);
				label += ".csv";
				java.nio.file.Path p = f.toPath();
				java.nio.file.Path p1 = p.resolveSibling(label.replaceAll("/", "_"));
				java.nio.file.Files.move(
						  p 
						, p1
						, StandardCopyOption.ATOMIC_MOVE
						);
			}else{
				System.out.println("This was not found: " + fileNamePelado);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
