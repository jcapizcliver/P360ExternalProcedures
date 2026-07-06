package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SeparaInformaciónDeProductosYVariantes {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args) {
		System.out.println("Collecting attributes by template.");
		String prevTemplate = null;
		String prevProduct = null;
		java.util.Map<String, String> attributeValues = new java.util.TreeMap<>();
		int count = 0;
		Integer freq = null;
		java.util.Map<String, Integer> hits = new java.util.TreeMap<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "DatosMaestrosPlantillas.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "PlantillasGenéricos2.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8)); java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "PlantillasVariantes2.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				String line = br.readLine();
				pw.println(line);
				pw2.println(line);
				String[] pieces = null;
				while((line = br.readLine()) != null) {
					pieces = workshop.parseLine(line);
					if("".equals(pieces[6])) {
						pw.println(line);
					} else {
						pw2.println(line);
						if( (prevTemplate != null && !prevTemplate.equals(pieces[0])) || (prevProduct != null && !prevProduct.equals(pieces[1]) )) {
							for(String s : ATTRIBUTES) {
								freq = hits.get(s);
								hits.put(s, (freq == null ? 0 : freq) + ( attributeValues.containsKey(s) ? 1 : 0 ));
							}
							attributeValues.clear();
						}
						attributeValues.put(pieces[2], workshop.serializeChunk(new Object[] { pieces[3], pieces[4] }, "\"", ";", "\\"));
					}
					count++;
					if(count % 100000 == 0) {
						System.out.print(".");
						if(count % 1000000 == 0) {
							System.out.println(count);
						}
					}
					prevTemplate = pieces[0];
					prevProduct = pieces[1];
				}
				System.out.println("Done. " + count);
				hits.forEach((k,v) -> System.out.println(k + " - " + v));
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	public static final java.util.ArrayList<String> ATTRIBUTES = new java.util.ArrayList<>( java.util.Arrays.asList((
			  "ProductType\r\n"
			+ "ColoursLiverpoolAtt\r\n"
			+ "SupplierPartNumber\r\n"
			+ "SupplierID\r\n"
			+ "Section\r\n"
			+ "Name\r\n"
			+ "ProductWidth\r\n"
			+ "ItemGroup"
		).split("\\r\\n")) );
	
	public static final java.util.Set<String> definingAttributes = new java.util.TreeSet<>( java.util.Arrays.asList(new String[] {
			"TamanoUnico",
			"ColoursLiverpoolAtt",
			"SB_COLORES",
			"SB_TJOYERIAYACCESORIOS",
			"SB_THOGAR",
			"LadiesSizeAtt",
			"SB_TCALCETERIA",
			"MenSizeAtt",
			"Direction1SizeAtt",
			"Direction3SizeAtt",
			"SB_T_HARDLINE",
			"SB_0025",
			"SB_TINFANTILES",
			"SB_TBEBES",
			"SB_0106",
			"SB_TROPAINTERIOR",
			"SB_TJUNIORS",
			"SB_TZAPATOS",
			"SB_TCABALLEROS",
			"SB_TLENCERIA",
			"SferaSizeAtt",
			"TamanoDireccion6Att",
			"TamanoDireccion8Att",
			"SizeCosmeticsAccAtt",
			"SportsSizeAtt",
			"TamanoPantallaAtt",
			"SB_T_TECNO_ENTREN",
			"ShoeSizeLivAtt",
			"SB_TDAMAS",
			"OpticalSizeAtt",
			"ChildrenSizeAtt",
			"SupplierPartNumber"
			
	}));


}
