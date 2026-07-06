package mx.com.liverpool.dataprofiling.preparison.ccl;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.xml.ProductFileAssetElement;
import mx.com.liverpool.p360.services.core.xml.ProductFileHandler;
import mx.com.liverpool.p360.services.core.xml.ProductFileProductElement;
import mx.com.liverpool.p360.services.core.xml.ProductFileValueElement;

public class FlattenSTEPXMLData {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
//		try( java.io.BufferedReader br = new java.io.BufferedReader( new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "QA", "data_ccl2").toFile())) ); java.io.BufferedReader b2 = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "QA", "data_ccl").toFile()))) ){
//			StringBuilder sb  = new StringBuilder();
//			StringBuilder sb2 = new StringBuilder();
//			String line = null;
//			while((line = br.readLine()) != null) {
//				sb.append(line);
//			}
//			while((line = b2.readLine()) != null) {
//				sb2.append(line);
//			}
//			System.out.println(sb.toString().equals(sb2.toString()));
//			System.out.println(sb.toString().length());
//			System.out.println(sb2.toString().length());
//			java.util.List<Integer> pos = new java.util.ArrayList<>();
//			for(int i=0; i<sb2.length(); i++) {
//				if(sb.charAt(i)  != (sb2.charAt(i))) {
//					pos.add(i);
//				}
//			}
//			pos.forEach(System.out::println);
//		}catch(java.io.IOException e) {
//			e.printStackTrace();
//		}
//		
//		
//		System.exit(0);
		
		ProductFileHandler handler = new ProductFileHandler();
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = null;
        try {
			parser = factory.newSAXParser();
		} catch (ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
        if(parser != null) {
        	java.util.Map<String, String> assetIdURL = new java.util.HashMap<>();
        	java.util.Map<String, ProductFileValueElement> valuesMap = null;
        	java.io.File[] files = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "QA", "STEP").toFile().listFiles(ff -> ff.getName().endsWith("1033768636.xml"));
        	for(File input : files) {
    	        try {
    	        	System.out.println("~~ * ~~");
    	        	parser.parse(input, handler);
    	        	java.util.Map<String, ProductFileAssetElement> map = handler.getAssetMap();
    	        	for(java.util.Map.Entry<String, ProductFileAssetElement> entry : map.entrySet() ) {
//    	        		System.out.println(entry.getKey());
    	        		valuesMap = entry.getValue().getValues();
    	        		assetIdURL.put(entry.getKey(),  getValue( valuesMap, "ImageURL") );
    	        	}
    	        	assetIdURL.forEach((k,v)-> { 
//    	        		System.out.println(k + ";" + v + "\n");
//    	        		System.out.println(k + " - " + ( v.split(",")[0].split("\\=").length == 1 ? v.split(",")[0].split("\\=")[0] : v.split(",")[0].split("\\=")[1] ) ); 
    	        	});
    	        	System.out.println("Products");
    	        	java.util.List<ProductFileProductElement> products = handler.getFinished();
    	        	java.util.List<String[]> rows = new java.util.ArrayList<>();
    	        	for(ProductFileProductElement p : products) {
    	        		valuesMap = p.getValues();
//    	        		System.out.println(valuesMap.get("ProductName").getText() + ", ProductType: " + p.getUserTypeId() + ", childProducts: " + p.getProducts().size() + ". " + p.getParentId() );
    	        		String a = null;
    	        		for( java.util.Map.Entry<String, String> entry : p.getAssetCrossReferences().entrySet() ) {
    	        			String k = entry.getKey();
    	        			String v = entry.getValue();
    	        			if("PrimaryProductImage".equals(v)) {
    	        				a = ( assetIdURL.get(k).split(",")[0].split("\\=").length == 1 ? assetIdURL.get(k).split(",")[0].split("\\=")[0] : assetIdURL.get(k).split(",")[0].split("\\=")[1] );
    	        				break;
//    	        				System.out.println("\tIMG: " + 
//    	        						( assetIdURL.get(k).split(",")[0].split("\\=").length == 1 ? assetIdURL.get(k).split(",")[0].split("\\=")[0] : assetIdURL.get(k).split(",")[0].split("\\=")[1] )
//    	        				);	
    	        			}
    	        		}
    	        		rows.add(new String[] {
    	        				  p.getParentId()
    	        				, p.getId()
    	        				, getValue(valuesMap, "SKU")
    	        				, ""
    	        				, p.getUserTypeId()
    	        				, getValue(valuesMap, "ProductType")
    	        				, getValue(valuesMap, "ColoursLiverpoolAtt")
    	        				, getValue(valuesMap, "SupplierPartNumber")
    	        				, getValue(valuesMap, "SupplierID")
    	        				, getValue(valuesMap, "Section")
    	        				, getValue(valuesMap, "ProductName")
    	        				, getValue(valuesMap, "ProductWidth")
    	        				, getValue(valuesMap, "ItemGroup")
    	        				, getValue(valuesMap, "AE416")
    	        				, getValue(valuesMap, "DescriptionLong")
    	        				, a
    	        		});
//    	        		System.out.println("\tTemplate: " + p.getParentId());
//    	        		System.out.println("\tID: " + p.getId());
//    	        		System.out.println("\tSKU: " + getValue(valuesMap, "SKU"));
//    	        		System.out.println("\tProductType: " + getValue(valuesMap, "ProductType"));
//    	        		System.out.println("\tColoursLiverpoolAtt: " + getValue(valuesMap, "ColoursLiverpoolAtt"));
//    	        		System.out.println("\tSupplierPartNumber: " + getValue(valuesMap, "SupplierPartNumber"));
//    	        		System.out.println("\tSupplierID: " + getValue(valuesMap, "SupplierID"));
//    	        		System.out.println("\tSection: " + getValue(valuesMap, "Section"));
//    	        		System.out.println("\tProductName: " + getValue(valuesMap, "ProductName"));
//    	        		System.out.println("\tProductWidth: " + getValue(valuesMap, "ProductWidth"));
//    	        		System.out.println("\tItemGroup: " + getValue(valuesMap, "ItemGroup"));
//    	        		System.out.println("\tNoSpot: " + getValue(valuesMap, "AE416"));
//    	        		System.out.println("\tDL: " + getValue(valuesMap, "DescriptionLong"));
//    	        		System.out.println("\tIMG: " + getValue(valuesMap, "ImageURL"));
//    	        		p.getAssetCrossReferences().forEach((k,v)->{
//    	        			if("PrimaryProductImage".equals(v)) {
//    	        				System.out.println("\tIMG: " + 
//    	        						( assetIdURL.get(k).split(",")[0].split("\\=").length == 1 ? assetIdURL.get(k).split(",")[0].split("\\=")[0] : assetIdURL.get(k).split(",")[0].split("\\=")[1] )
//    	        				);	
//    	        			}
//    	        		});
    	        		System.out.println("~~~~");
    	        	}
    	        	java.util.Collections.sort(rows, (o1,o2) -> o1[2].compareTo(o2[2]));
    	        	try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "QA", "data_ccl2").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
    	        		rows.forEach( r -> pw.println(rw.getRw().serializeChunk(r)) );
    	        	}catch(java.io.IOException e) {
    	        		e.printStackTrace();
    	        	}
    	        	/*
    	        	 * 
    	        	 * 
									.put( "sku", key )
									.put( "ProductType", "" )
									.put( "ColoursLiverpoolAtt", "" )
									.put( "SupplierPartNumber", "" )
									.put( "SupplierID", "" )
									.put( "Section", "" )
									.put( "Name", "" )
									.put( "ProductWidth", "" )
									.put( "ItemGroup", "" )
									.put( "NoSpot", "" )
									.put( "DescriptionLong", "")
									.put( "ImageURL", "" )
    	        	 * 
    	        	 * 
    	        	 ******/
    	        	System.out.println();
    	        }catch(org.xml.sax.SAXParseException e) {
    	        	System.out.println("Problem processing following file: " + input.getName());
    	        } catch (SAXException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        }
	}
	
	private static String getValue(java.util.Map<String, ProductFileValueElement> map, String key) {
		ProductFileValueElement v = map.get(key);
		String id = v == null ? null : v.getId();
		String unit = v == null ? null : v.getUnidadId();
		return v == null ? "" : (id != null ? id + ";" : unit != null ? unit + ";" : "") + v.getText();
	}
	
}
