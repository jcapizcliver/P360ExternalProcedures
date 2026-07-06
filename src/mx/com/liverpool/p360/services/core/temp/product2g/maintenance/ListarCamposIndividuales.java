package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.temp.xml.local.AnotherXMLHandler2;
import mx.com.liverpool.p360.services.core.temp.xml.local.AnotherXMLHandler2.Handler;

public class ListarCamposIndividuales extends RESTWrapper {
	
	
	public static void main(String[] args) {
		
		ListarCamposIndividuales ex = new ListarCamposIndividuales();
		ex.process();
		
		
	}
	
	private void process() {
		java.util.Set<String> losQueSi = new java.util.TreeSet<>();
		java.util.LinkedList<org.json.JSONArray> pieces = new java.util.LinkedList<>(); 
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				   "StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
			);
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		qp.put("query", "StandardizationValue.Property->LookupValue.Code = \"RelevantForATG\" and StandardizationValue.PropertyValue = \"Y\"");
		qp.put("orderBy", "0-ASC;1-ASC");
		qp.put("pageSize", "20000");
		collectData("list", "StandardizationValue", null, "bySearch", qp, row->{
			pieces.addLast(row.getJSONArray("values"));
		}, System.out::println);
		org.json.JSONArray prev = null;
		for(org.json.JSONArray values : pieces) {
			if(prev != null && prev.getString(0).equals(values.getString(0)) && prev.getString(1).equals(values.getString(1))) {
				System.out.println("Got these: \n\t" + prev + "\n\t" + values);
				System.exit(0);
			}
			if("Y".equals(values.getString(3))) {
				losQueSi.add(values.getString(1));
			}
			prev = values;
		}
		collectGlobalMetaData(losQueSi);
		System.out.println("Los que sí: " + losQueSi.size());
		java.nio.file.Path p = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "currentNonSendables");
//		qp.clear();
//		qp.put("fields", "Product2G.ProductNo");
//		qp.put("pageSize", "50000");
//		java.util.Set<String> productNos = new java.util.TreeSet<>();
//		java.nio.file.Path p = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "currentProductNos");
//		if(!java.nio.file.Files.exists(p)) {
//			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(p.toFile())))){
//				collectData("list", "Product2G", null, "byCatalog", qp, row -> {
//					pw.println(row.getJSONArray("values").getString(0));
//				}, System.out::println);
//			}catch(java.io.IOException e) {
//				e.printStackTrace();
//			}
//		}else {
//			try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(p)){
//				lns.parallel().forEach(productNos::add);
//			}catch(java.io.IOException e) {
//				e.printStackTrace();
//			}
//		}
		qp.clear();
		java.util.Set<String> todoLoQueHay = new java.util.TreeSet<>();
//		java.nio.file.Path aQuitarPath = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "atributos_a_quitar_de_ATG.txt");
		if(!java.nio.file.Files.exists(p)) {
			System.out.println("Loading...");
			qp.put("fields", "Characteristic.Identifier");
			qp.put("query", "Characteristic.Entities in (\"Product2G\") and Characteristic.ParentCharacteristic is empty");
			qp.put("pageSize", "10000");
			collectData("list", "Characteristic", null, "bySearch", qp, row -> {
				String a = row.getJSONArray("values").getString(0);
				System.out.println("$$ " + a);
				if(!losQueSi.contains(a)) {
					todoLoQueHay.add(a);
				}
			}, System.out::println);
			System.out.println("Metadata collected.");
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(p.toString())))){
				todoLoQueHay.forEach(pw::println);
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}else {
			System.out.println("Elepa...");
			try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(p)){
				lns.forEach(todoLoQueHay::add);
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        java.util.Set<String> desdeArchivos = new java.util.TreeSet<>();
        try {
			SAXParser parser = factory.newSAXParser();
			AnotherXMLHandler2 a = new AnotherXMLHandler2();
			java.io.File[] files = new java.io.File(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BGP_82326611").toString()).listFiles(ff -> ff.getName().endsWith("xml"));
	        for(File input : files) {
		        Handler handler = a.new Handler();
		        try {
		        	parser.parse(input, handler);
		        }catch(org.xml.sax.SAXParseException | IOException e) {
		        	e.printStackTrace();
		        }
		        java.util.List<AnotherXMLHandler2.Product> products = handler.getFinished();
		        if(products != null) {
//		        	System.out.println("Found " + products.size());
			        for(AnotherXMLHandler2.Product product : products) {
			        	java.util.LinkedList<AnotherXMLHandler2.Value> values = product.getValues();
			        	if(values != null) {
//			        		System.out.println("\t" + values.size() + " values.");
			        		for(AnotherXMLHandler2.Value value : values) {
				        		desdeArchivos.add( value.getAttributeId() );
				        	}
			        	}
			        }
		        }
	        }
	        desdeArchivos.forEach(System.out::println);
	        System.out.println("*******");
//	        try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "migración", "tompa"))){
//	        	lns.forEach(todoLoQueHay::remove);
//	        }catch(java.io.IOException e) {
//	        	e.printStackTrace();
//	        }
			todoLoQueHay.removeAll(desdeArchivos);
			todoLoQueHay.forEach(System.out::println);
			System.out.println("Done");
		} catch (ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
        
	}
	
	private void collectGlobalMetaData(java.util.Set<String> losQueSi) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("dictionaryProxy", "'GlobalTemplateAttributeConfiguration'");
		qp.put("fields", 
				   "StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
				+ ",StandardizationValue.Characteristic->CharacteristicLang.Name(es)"
				+ ",StandardizationValue.Characteristic->CharacteristicLang.Description(es)"
				+ ",StandardizationValue.Characteristic->Characteristic.DataType"
				+ ",StandardizationValue.Characteristic->Characteristic.Lookup->Lookup.Identifier"
				+ ",StandardizationValue.Characteristic->Characteristic.IsMultiValue"
				+ ",StandardizationValue.Characteristic->Characteristic.Purposes->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Order"
			);
		qp.put("query", 
				  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"GlobalTemplateAttributeConfiguration\""
			);
		qp.put("orderBy", "0-ASC");
		qp.put("pageSize", "2000");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int totalSize = 0;
		int currentIndex = 0;
		org.json.JSONObject detail = new org.json.JSONObject();
		org.json.JSONArray prevValues = null;
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = getRw().makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if(prevValues != null && !prevValues.getString(0).equals(values.getString(0))) {
						detail.put("name", prevValues.getString(3));
						detail.put("description", prevValues.getString(4));
						detail.put("dataType", prevValues.getString(5));
						detail.put("lookup", prevValues.getString(6));
						detail.put("isMultiValue", prevValues.getString(7));
						detail.put("purposes", prevValues.getJSONArray(8));
						detail.put("order", prevValues.getString(9));
						if(detail.getJSONArray("purposes").length() == 1 && detail.getJSONArray("purposes").getString(0).equals(""))
							detail.getJSONArray("purposes").remove(0);
						if(detail.has("RelevantForATG") && "Y".equals(detail.getString("RelevantForATG"))) {
//							System.out.println("------->" + prevValues.getString(0));
							losQueSi.add(prevValues.getString(0));
						}
						detail = new org.json.JSONObject();
					}
					detail.put(values.getString(1), values.getString(2));
					prevValues = values;
				}
			}else {
				System.out.println("ERR: " + getRw().getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		if(detail.length() > 0) {
			detail.put("name", prevValues.getString(3));
			detail.put("description", prevValues.getString(4));
			detail.put("dataType", prevValues.getString(5));
			detail.put("lookup", prevValues.getString(6));
			detail.put("isMultiValue", prevValues.getString(7));
			detail.put("purposes", prevValues.getJSONArray(8));
			detail.put("order", prevValues.getString(9));
			if(detail.getJSONArray("purposes").length() == 1 && detail.getJSONArray("purposes").getString(0).equals(""))
				detail.getJSONArray("purposes").remove(0);
			if(detail.has("RelevantForATG") && "Y".equals(detail.getString("RelevantForATG")))
				losQueSi.add(prevValues.getString(0));
			detail = null;
		}
	
	}

}
