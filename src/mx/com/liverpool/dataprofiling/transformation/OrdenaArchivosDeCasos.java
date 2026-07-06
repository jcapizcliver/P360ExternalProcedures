package mx.com.liverpool.dataprofiling.transformation;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class OrdenaArchivosDeCasos {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args) {

		java.nio.file.Path target1 = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Match", "Caso1.csv");
		java.nio.file.Path target2 = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Match", "Caso2.csv");
		java.nio.file.Path target3 = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Match", "Caso3.csv");

		java.nio.file.Path target1S = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Match", "Caso1Sorted.csv");
		java.nio.file.Path target2S = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Match", "Caso2Sorted.csv");
		java.nio.file.Path target3S = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Match", "Caso3Sorted.csv");
		
		java.util.List<String[]> pieces1 = null;
		java.util.List<String[]> pieces2 = null;
		java.util.List<String[]> pieces3 = null;
		
		String headerLine = "Template,Product,SKU,ParentSKU,StepObjectType,ProductTypeSAP,ColoursLiverpoolAtt,SupplierPartNumber,TamanoUnico,BrandName,ItemGroup";
		System.out.println("Collecting data...");
		try{
			pieces1 = java.nio.file.Files.lines(target1).parallel().filter(l -> !l.startsWith("Template")).map(workshop::parseLine).collect(java.util.stream.Collectors.toList());
			pieces2 = java.nio.file.Files.lines(target2).parallel().filter(l -> !l.startsWith("Template")).map(workshop::parseLine).collect(java.util.stream.Collectors.toList());
			pieces3 = java.nio.file.Files.lines(target3).parallel().filter(l -> !l.startsWith("Template")).map(workshop::parseLine).collect(java.util.stream.Collectors.toList());
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Now sorting...");
		
		java.util.Collections.sort(pieces1, (o1,o2)->{ 
			int cmp = 0;
			cmp = o1[7].compareTo(o2[7]);
			if(cmp == 0) {
				cmp = o1[5].compareTo(o2[5]);
				if(cmp == 0) {
					cmp = o1[9].compareTo(o2[9]);
				}
			}
			return cmp;
		});


//		boolean caso1 = hayModelo && ( hayProducto || hayMarca ) ;
//		boolean caso2 = hayModelo && hayItemGroup && hayColorTalla;
//		boolean caso3 = hayMarca && hayProducto && hayItemGroup && hayColorTalla;
//       0,Template
//		 1,Product
//		 2,SKU
//		 3,ParentSKU
//		 4,StepObjectType
//		 5,ProductTypeSAP
//		 6,ColoursLiverpoolAtt
//		 7,SupplierPartNumber
//		 8,TamanoUnico
//		 9,BrandName
//		10,ItemGroup
		
		java.util.Collections.sort(pieces2, (o1,o2)->{ 
			int cmp = 0;
			cmp = o1[7].compareTo(o2[7]);
			if(cmp == 0) {
				cmp = o1[10].compareTo(o2[10]);
				if(cmp == 0) {
					cmp = o1[6].compareTo(o2[6]);
					if(cmp == 0) {
						cmp = o1[8].compareTo(o2[8]);
						if(cmp == 0) {
							cmp = o1[7].compareTo(o2[7]);
						}
					}
				}
			}
			return cmp;
		});
		
//		boolean caso1 = hayModelo && ( hayProducto || hayMarca ) ;
//		boolean caso2 = hayModelo && hayItemGroup && hayColorTalla;
//		boolean caso3 = hayMarca && hayProducto && hayItemGroup && hayColorTalla;
//       0,Template
//		 1,Product
//		 2,SKU
//		 3,ParentSKU
//		 4,StepObjectType
//		 5,ProductTypeSAP
//		 6,ColoursLiverpoolAtt
//		 7,SupplierPartNumber
//		 8,TamanoUnico
//		 9,BrandName
//		10,ItemGroup
		
		java.util.Collections.sort(pieces3, (o1,o2)->{ 
			int cmp = 0;
			cmp = o1[9].compareTo(o2[9]);
			if(cmp == 0) {
				cmp = o1[5].compareTo(o2[5]);
				if(cmp == 0) {
					cmp = o1[10].compareTo(o2[10]);
					if(cmp == 0) {
						cmp = o1[6].compareTo(o2[6]);
						if(cmp == 0) {
							cmp = o1[8].compareTo(o2[8]);
						}
					}
				}
			}
			return cmp;
		});
		
		System.out.println("Now writing...");
		
		try(
			java.io.PrintWriter pw  = new java.io.PrintWriter(new java.io.OutputStreamWriter(java.nio.file.Files.newOutputStream(target1S), java.nio.charset.StandardCharsets.UTF_8));
			java.io.PrintWriter pw1 = new java.io.PrintWriter(new java.io.OutputStreamWriter(java.nio.file.Files.newOutputStream(target2S), java.nio.charset.StandardCharsets.UTF_8));
			java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(java.nio.file.Files.newOutputStream(target3S), java.nio.charset.StandardCharsets.UTF_8))
		){
			pw.println(headerLine);
			pw1.println(headerLine);
			pw2.println(headerLine);
			pieces1.forEach(a -> pw.println( workshop.serializeChunk(a) ));
			pieces2.forEach(a -> pw1.println( workshop.serializeChunk(a) ));
			pieces3.forEach(a -> pw2.println( workshop.serializeChunk(a) ));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
