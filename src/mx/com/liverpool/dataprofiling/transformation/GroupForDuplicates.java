package mx.com.liverpool.dataprofiling.transformation;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class GroupForDuplicates {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();

	public static void main(String[] args) {
		java.util.Map<String, java.util.Map<String, String>> genericSKUData = new java.util.HashMap<>();
		java.util.Map<String, String> genericSKUDataValue = null;
		java.nio.file.Path sourcePath = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "DatosMaestrosPlantillas.csv");
		java.nio.file.Path targetPath = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "DatosParaMatch.csv");
		System.out.println("Building products dictionary.");
		
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(sourcePath.toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = br.readLine();
			String[] pieces = null;
			String prevTemplate = null;
			String prevProduct = null;
			String prevSKU = null;
			java.util.Map<String, String> attributeValues = new java.util.TreeMap<>();
			String attributeValue = null;
			int count = 0;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line);
				count++;
				if(pieces[7].startsWith("SalesItemFamily")) {
					if( (prevTemplate != null && !prevTemplate.equals(pieces[0])) || (prevProduct != null && !prevProduct.equals(pieces[1])) ) {
						genericSKUDataValue = genericSKUData.get(prevSKU);
						if(genericSKUDataValue != null) {
							System.out.println("Warning, set not sorted... " + count);
						}else {
							genericSKUDataValue = new java.util.HashMap<>();
							genericSKUData.put(prevSKU, genericSKUDataValue);
							for(String att : ATTRIBUTES) {
								attributeValue = attributeValues.get(att);
								genericSKUDataValue.put(att, attributeValue == null ? "" : attributeValue);
							}
						}
						attributeValues.clear();
					}
					attributeValues.put(pieces[2], pieces[4] );
					prevTemplate = pieces[0];
					prevProduct = pieces[1];
					prevSKU = pieces[5];
				}
				if(count % 100000 == 0) {
					System.out.print(".");
					if(count % 1000000 == 0) {
						System.out.println(count);
					}
				}
			}
			if(!attributeValues.isEmpty()) {
				genericSKUDataValue = genericSKUData.get(prevSKU);
				if(genericSKUDataValue != null) {
					System.out.println("Warning, set not sorted... " + count);
				}else {
					genericSKUDataValue = new java.util.HashMap<>();
					genericSKUData.put(prevSKU, genericSKUDataValue);
					for(String att : ATTRIBUTES) {
						attributeValue = attributeValues.get(att);
						genericSKUDataValue.put(att, attributeValue == null ? "" : attributeValue);
					}
				}
				attributeValues.clear();
			}
			System.out.println();
			System.out.println(count);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("Now generating data set...");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(sourcePath.toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = br.readLine();
			String[] pieces = null;
			String prevTemplate = null;
			String prevProduct = null;
			String prevSKU = null;
			String prevParentSKU = null;
			String prevSPT = null;
			java.util.Map<String, String[]> attributeValues = new java.util.TreeMap<>();
			String[] attributeValue = null;
			int count = 0;
			String genericValue = null;
			String[] dataHolder = new String[ ATTRIBUTES.size() + 5 ];
			String att = null;
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(targetPath.toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				String[] header = new String[ ATTRIBUTES.size() + 5 ];
				header[0] = "Template";
				header[1] = "Product";
				header[2] = "SKU";
				header[3] = "ParentSKU";
				header[4] = "StepObjectType";
				for(int i=0; i<ATTRIBUTES.size(); i++) {
					header[i + 5] = ATTRIBUTES.get(i);
				}
				pw.println( workshop.serializeChunk(header) );
				while((line = br.readLine()) != null) {
					pieces = workshop.parseLine(line);
					count++;
					if(!pieces[7].startsWith("SalesItemFamily")) {
						if( (prevTemplate != null && !prevTemplate.equals(pieces[0])) || (prevProduct != null && !prevProduct.equals(pieces[1])) ) {
							genericSKUDataValue = genericSKUData.get(prevParentSKU);
							if("SalesItem".equals(prevSPT) && "".equals(prevParentSKU)) {
								for(int i = 0; i<ATTRIBUTES.size(); i++) {
									att = ATTRIBUTES.get(i);
									attributeValue = attributeValues.get(att);
									dataHolder[i + 5] = attributeValue == null ? "" : workshop.serializeChunk(attributeValue, "\"", ";", "\\");
								}
								dataHolder[0] = prevTemplate;
								dataHolder[1] = prevProduct;
								dataHolder[2] = prevSKU;
								dataHolder[3] = prevParentSKU;
								dataHolder[4] = prevSPT;
								pw.println( workshop.serializeChunk(dataHolder) );
								for(int i=0; i<dataHolder.length; i++) {
									dataHolder[i] = "";
								}
							}else {
								if(genericSKUDataValue != null) {
									for(int i = 0; i<ATTRIBUTES.size(); i++) {
										att = ATTRIBUTES.get(i);
										genericValue = genericSKUDataValue.get(att);
										if("".equals(genericValue)) {
											attributeValue = attributeValues.get(att);
											dataHolder[i + 5] = attributeValue == null ? "" : workshop.serializeChunk(attributeValue, "\"", ";", "\\");
										}else {
											dataHolder[i + 5] = genericValue;
										}
									}
									dataHolder[0] = prevTemplate;
									dataHolder[1] = prevProduct;
									dataHolder[2] = prevSKU;
									dataHolder[3] = prevParentSKU;
									dataHolder[4] = prevSPT;
									pw.println( workshop.serializeChunk(dataHolder) );
									for(int i=0; i<dataHolder.length; i++) {
										dataHolder[i] = "";
									}
								}else {
									System.out.println("Warning, parent not found (" + prevParentSKU + ")... " + count);
								}
							}
							attributeValues.clear();
						}
						attributeValues.put(pieces[2], !"".equals(pieces[3]) ? new String[] { pieces[3], pieces[4] } : new String[] {pieces[4]});
						prevTemplate = pieces[0];
						prevProduct = pieces[1];
						prevSKU = pieces[5];
						prevParentSKU = pieces[6];
						prevSPT = pieces[7];
					}
					if(count % 100000 == 0) {
						System.out.print(".");
						if(count % 1000000 == 0) {
							System.out.println(count);
						}
					}
				}
				if(!attributeValues.isEmpty()) {
					genericSKUDataValue = genericSKUData.get(prevParentSKU);
					if("SalesItem".equals(prevSPT) && "".equals(prevParentSKU)) {
						for(int i = 0; i<ATTRIBUTES.size(); i++) {
							att = ATTRIBUTES.get(i);
							attributeValue = attributeValues.get(att);
							dataHolder[i + 5] = attributeValue == null ? "" : workshop.serializeChunk(attributeValue, "\"", ";", "\\");
						}
						dataHolder[0] = prevTemplate;
						dataHolder[1] = prevProduct;
						dataHolder[2] = prevSKU;
						dataHolder[3] = prevParentSKU;
						dataHolder[4] = prevSPT;
						pw.println( workshop.serializeChunk(dataHolder) );
						for(int i=0; i<dataHolder.length; i++) {
							dataHolder[i] = "";
						}
					}else {
						if(genericSKUDataValue != null) {
							for(int i = 0; i<ATTRIBUTES.size(); i++) {
								att = ATTRIBUTES.get(i);
								genericValue = genericSKUDataValue.get(att);
								if("".equals(genericValue)) {
									attributeValue = attributeValues.get(att);
									dataHolder[i + 5] = attributeValue == null ? "" : workshop.serializeChunk(attributeValue, "\"", ";", "\\");
								}else {
									dataHolder[i + 5] = genericValue;
								}
							}
							dataHolder[0] = prevTemplate;
							dataHolder[1] = prevProduct;
							dataHolder[2] = prevSKU;
							dataHolder[3] = prevParentSKU;
							dataHolder[4] = prevSPT;
							pw.println( workshop.serializeChunk(dataHolder) );
							for(int i=0; i<dataHolder.length; i++) {
								dataHolder[i] = "";
							}
						}else {
							System.out.println("Warning, parent not found (" + prevParentSKU + ")... " + count);
						}
					}
					attributeValues.clear();
				}
			}
			System.out.println();
			System.out.println(count);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done.");
	}

	
	public static final java.util.ArrayList<String> ATTRIBUTES = new java.util.ArrayList<>( java.util.Arrays.asList((
			  "ProductTypeSAP\r\n"
			+ "ColoursLiverpoolAtt\r\n"
			+ "SupplierPartNumber\r\n"
			+ "TamanoUnico\r\n"
			+ "BrandName\r\n"
			+ "ItemGroup"
		).split("\\r\\n")) );
	
}
