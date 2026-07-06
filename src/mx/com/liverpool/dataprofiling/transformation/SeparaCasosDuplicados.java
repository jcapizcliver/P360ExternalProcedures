package mx.com.liverpool.dataprofiling.transformation;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class SeparaCasosDuplicados {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args) {

		java.nio.file.Path target1S = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Match", "Caso1Sorted.csv");
		java.nio.file.Path target2S = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Match", "Caso2Sorted.csv");
		java.nio.file.Path target3S = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Match", "Caso3Sorted.csv");
		
		java.nio.file.Path target2M = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Match", "CasosMatch.csv");
		java.nio.file.Path target3M = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Match", "Caso3Match.csv");
		
		java.util.List<String[]> pieces1 = null;
		java.util.List<String[]> pieces2 = null;
		java.util.List<String[]> pieces3 = null;
		
		String headerLine = "Template,Product,SKU,ParentSKU,StepObjectType,ProductTypeSAP,ColoursLiverpoolAtt,SupplierPartNumber,TamanoUnico,BrandName,ItemGroup";
		System.out.println("Collecting data...");
		int counter = 1;
		try( java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(target2S, java.nio.charset.StandardCharsets.UTF_8) ){
			String line = br.readLine();
			String[] pieces = null;


//			Product   = 20
//			ItemGroup = 10
//			BrandName = 20
//			Modelo    = 40
//			Color     =  5
//			Talla     =  5
//			boolean caso1 = hayModelo && ( hayProducto || hayMarca ) ;
//			boolean caso2 = hayModelo && hayItemGroup && hayColorTalla;
//			boolean caso3 = hayMarca && hayProducto && hayItemGroup && hayColorTalla;
//	         0,Template
//			 1,Product
//			 2,SKU
//			 3,ParentSKU
//			 4,StepObjectType
//			 5,ProductTypeSAP
//			 6,ColoursLiverpoolAtt
//			 7,SupplierPartNumber
//			 8,TamanoUnico
//			 9,BrandName
//			10,ItemGroup
			String supplierPartNumber = null;
			String producto = null;
			String marca = null;
			String itemGroup = null;
			String color = null;
			String talla = null;
			java.util.List<String[]> lalista = new java.util.ArrayList<>();
			try( java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(java.nio.file.Files.newOutputStream(target2M), java.nio.charset.StandardCharsets.UTF_8)) ){
				pieces = workshop.parseLine(line);
				String[] header = java.util.Arrays.copyOf(pieces, pieces.length + 2);
				header[pieces.length] = "Caso";
				header[pieces.length + 1] = "GroupID";
				pw.println( workshop.serializeChunk(header) );
				while((line = br.readLine()) != null) {
					pieces = workshop.parseLine(line);
					if( (supplierPartNumber != null && !supplierPartNumber.equals(pieces[7])) 
							|| (itemGroup != null && !itemGroup.equals(pieces[10]))
							|| (color != null && !color.equals(pieces[6]))
							|| (talla != null && !talla.equals(pieces[8]))
					) {
						if(lalista.size() > 1) {
							for(int i=0; i<lalista.size(); i++) {
								String[] na = java.util.Arrays.copyOf(lalista.get(i), lalista.get(i).length + 2);
								na[lalista.get(i).length] = "1";
								na[lalista.get(i).length + 1] = String.valueOf( counter );
								pw.println( workshop.serializeChunk(na) );
							}
							counter++;
						}
						lalista.clear();
					}
					lalista.add(pieces);
					producto = pieces[5];
					color = pieces[6];
					supplierPartNumber = pieces[7];
					talla = pieces[8];
					marca = pieces[9];
					itemGroup = pieces[10];
				}
				if(lalista.size() > 1) {
					for(int i=0; i<lalista.size(); i++) {
						String[] na = java.util.Arrays.copyOf(lalista.get(i), lalista.get(i).length + 2);
						na[lalista.get(i).length] = "1";
						na[lalista.get(i).length + 1] = String.valueOf( counter );
						pw.println( workshop.serializeChunk(na) );
					}
					counter++;
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		try( java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(target3S, java.nio.charset.StandardCharsets.UTF_8) ){
			String line = br.readLine();
			String[] pieces = null;

//			boolean caso1 = hayModelo && ( hayProducto || hayMarca ) ;
//			boolean caso2 = hayModelo && hayItemGroup && hayColorTalla;
//			boolean caso3 = hayMarca && hayProducto && hayItemGroup && hayColorTalla;
//	         0,Template
//			 1,Product
//			 2,SKU
//			 3,ParentSKU
//			 4,StepObjectType
//			 5,ProductTypeSAP
//			 6,ColoursLiverpoolAtt
//			 7,SupplierPartNumber
//			 8,TamanoUnico
//			 9,BrandName
//			10,ItemGroup
			String supplierPartNumber = null;
			String producto = null;
			String marca = null;
			String itemGroup = null;
			String color = null;
			String talla = null;
			java.util.List<String[]> lalista = new java.util.ArrayList<>();
			try( java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(target2M.toFile(), true), java.nio.charset.StandardCharsets.UTF_8)) ){
//				pw.println(line);
				while((line = br.readLine()) != null) {
					pieces = workshop.parseLine(line);
					if( (marca != null && !marca.equals(pieces[9])) 
							|| (producto != null && !producto.equals(pieces[5]))
							|| (itemGroup != null && !itemGroup.equals(pieces[10]))
							|| (color != null && !color.equals(pieces[6]))
							|| (talla != null && !talla.equals(pieces[8]))
							|| (supplierPartNumber != null && !supplierPartNumber.equals(pieces[7]))
					) {
						if(lalista.size() > 1) {
							for(int i=0; i<lalista.size(); i++) {
								String[] na = java.util.Arrays.copyOf(lalista.get(i), lalista.get(i).length + 2);
								na[lalista.get(i).length] = "2";
								na[lalista.get(i).length + 1] = String.valueOf( counter );
								pw.println( workshop.serializeChunk(na) );
							}
							counter++;
						}
						lalista.clear();
					}
					lalista.add(pieces);
					producto = pieces[5];
					color = pieces[6];
					supplierPartNumber = pieces[7];
					talla = pieces[8];
					marca = pieces[9];
					itemGroup = pieces[10];
				}
				if(lalista.size() > 1) {
					for(int i=0; i<lalista.size(); i++) {
						String[] na = java.util.Arrays.copyOf(lalista.get(i), lalista.get(i).length + 2);
						na[lalista.get(i).length] = "2";
						na[lalista.get(i).length + 1] = String.valueOf( counter );
						pw.println( workshop.serializeChunk(na) );
					}
					counter++;
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		
	}
}
