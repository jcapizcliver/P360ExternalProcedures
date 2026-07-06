package mx.com.liverpool.dataprofiling.transformation;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ProduceCasosMatch {
	
	
	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args) {
		java.nio.file.Path source = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "DatosParaMatch.csv");
		java.nio.file.Path target1 = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Match", "Caso1.csv");
		java.nio.file.Path target2 = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Match", "Caso2.csv");
		java.nio.file.Path target3 = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Match", "Caso3.csv");
		try( java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(source, java.nio.charset.StandardCharsets.UTF_8) ){
			String line = br.readLine();
//			boolean caso1 = hayModelo && ( hayProducto || hayMarca ) ;
//			boolean caso2 = hayModelo && hayItemGroup && hayColorTalla;
//			boolean caso3 = hayMarca && hayProducto && hayItemGroup && hayColorTalla;
			try(
				java.io.PrintWriter pw  = new java.io.PrintWriter(new java.io.OutputStreamWriter(java.nio.file.Files.newOutputStream(target1), java.nio.charset.StandardCharsets.UTF_8));
				java.io.PrintWriter pw1 = new java.io.PrintWriter(new java.io.OutputStreamWriter(java.nio.file.Files.newOutputStream(target2), java.nio.charset.StandardCharsets.UTF_8));
				java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(java.nio.file.Files.newOutputStream(target3), java.nio.charset.StandardCharsets.UTF_8))
			){
				int count = 0;
				pw.println(line);
				pw1.println(line);
				pw2.println(line);
				String[] pieces = null;
				while((line = br.readLine()) != null) {
					pieces = workshop.parseLine(line);
					// Template,Product,SKU,ParentSKU,StepObjectType,ProductTypeSAP,ColoursLiverpoolAtt,SupplierPartNumber,TamanoUnico,BrandName,ItemGroup
					if( !"".equals( pieces[7] ) && ( !"".equals(pieces[5]) || !"".equals(pieces[9]) ) ) {
						pw.println(line);
					}
					if( !"".equals( pieces[7] ) && !"".equals(pieces[10]) && ( !"".equals(pieces[6]) || !"".equals(pieces[8]) ) ) {
						pw1.println(line);
					}
					if( !"".equals( pieces[9] ) && !"".equals(pieces[5]) && !"".equals(pieces[10]) && ( !"".equals(pieces[6]) || !"".equals(pieces[8]) ) ) {
						pw2.println(line);
					}
					count++;
					if(count % 100000 == 0) {
						System.out.print(".");
						if(count % 1000000 == 0) {
							System.out.println(count);
						}
					}
				}
				System.out.println(count);
			}
		}catch(java.io.IOException e){
			e.printStackTrace();
		}
	}

}
