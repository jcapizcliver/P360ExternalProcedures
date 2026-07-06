package mx.com.liverpool.p360.services.core.temp.characteristic;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.temp.characteristic.xml.Attribute;
import mx.com.liverpool.p360.services.core.temp.characteristic.xml.AttributeHandler;

public class CompareCharacteristicsThatAreLookupWithSTEPAttributes {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		java.nio.file.Path path = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "ele ka pés", "step-9836453337461897658-exported.xml");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields",
				  "Characteristic.Identifier"
				+ ",Characteristic.Lookup->Lookup.Identifier"
				+ ",Characteristic.DataType"
			);
		qp.put("pageSize", "5000");
		qp.put("query", "not Characteristic.Identifier is empty and not Characteristic.Category is empty and Characteristic.Entities in (\"Product2G\")");
		java.util.Map<String, String[]> data = new java.util.TreeMap<>();
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> data.put(row.getJSONArray("values").getString(0), new String[] { 
				  row.getJSONArray("values").getString(1)
				, row.getJSONArray("values").getString(2)
		}), System.out::println);
		qp.clear();
		qp.put("fields",
				"Lookup.Identifier"
			);
		qp.put("pageSize", "5000");
		qp.put("query", "not Lookup.Identifier is empty");
		java.util.LinkedList<String> lookups = new java.util.LinkedList<>();
		rw.collectData("list", "Lookup", null, "bySearch", qp, row -> {
			lookups.addLast(row.getJSONArray("values").getString(0));
		}, System.out::println);
		java.util.Set<String> lkpSet = new java.util.TreeSet<>(lookups);
		AttributeHandler ah = new AttributeHandler();
		javax.xml.parsers.SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        java.util.LinkedList<String> notFound = new java.util.LinkedList<>();
        java.util.LinkedList<String[]> bad = new java.util.LinkedList<>();
        java.util.LinkedList<String[]> badDataType = new java.util.LinkedList<>();
        java.util.LinkedList<String> lookupsToBeCreated = new java.util.LinkedList<>();
         try {
			javax.xml.parsers.SAXParser parser = factory.newSAXParser();
			parser.parse(path.toFile(), ah);
			java.util.Map<String, Attribute> attributes = ah.getAttributes();
			Attribute a;
			for(java.util.Map.Entry<String, String[]> entry : data.entrySet()) {
				a = attributes.get(entry.getKey());
				if(a != null && !"BaseUnitOfMeasure".equals(a.getId())) {
					if(a.getLookupValue() != null) {
						if("LOOKUP".equals(entry.getValue()[1])) {
							if(!a.getLookupValue().equals(entry.getValue()[0])) {
								bad.addLast(new String[] { a.getId(), entry.getValue()[0], a.getLookupValue(), entry.getValue()[1] });
								if(!lkpSet.contains(a.getLookupValue())){
									lookupsToBeCreated.addLast(a.getLookupValue());
								}
							}
						}else {
							badDataType.addLast(new String[] { a.getId(), entry.getValue()[0], a.getLookupValue(), entry.getValue()[1] });
							if(a.getLookupValue() != null) {
								if(!lkpSet.contains(a.getLookupValue())) {
									lookupsToBeCreated.addLast(a.getLookupValue());
								}
							}
						}
					}
				}else {
					notFound.addLast(entry.getKey());
				}
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
        System.out.println("*** BAD DataType ***");
        badDataType.forEach(arr -> System.out.println( rw.getRw().serializeChunk(arr) ));
        System.out.println(badDataType.size() + " in total.");
        System.out.println("*** BAD ***");
        bad.forEach(arr -> System.out.println( rw.getRw().serializeChunk(arr) ));
        System.out.println(bad.size() + " in total.");
        System.out.println("*** Lookups that do not exist in P360 ***");
        lookupsToBeCreated.forEach(System.out::println);
        System.out.println(lookupsToBeCreated.size() + " total");
        queryDataForSpecificAttributes(badDataType);
//        disableAndRestoreCharacteristics(badDataType);
	}
	
	private static void disableAndRestoreCharacteristics(java.util.LinkedList<String[]> bads) {
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "tmp", "BADS").toFile())))){
			for(String[] bad : bads) {
				pw.println( rw.getRw().serializeChunk(bad) );
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeObjectsInProtocol", "false");
		RequestHandler disableCharacteristics = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive")), 1000, request -> {
			rw.writeData("list", "Characteristic", null, qp, request, r -> { 
				if(r.contains("timeout")) {
				}
			});
		} );
		for(String[] bad : bads) {
			disableCharacteristics.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + bad[0] + "'")).put("values", new org.json.JSONArray().put(false)));
		}
		disableCharacteristics.sendData();
		RequestHandler updateCharacteristics = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.DataType")).put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup")), 1000, request -> rw.writeData("list", "Characteristic", null, qp, request, System.out::println) );
		for(String[] bad : bads) {
			updateCharacteristics.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + bad[0] + "'")).put("values", new org.json.JSONArray().put("LOOKUP").put(new org.json.JSONObject().put("id",  "'" + bad[2] + "'" ))));
		}
		updateCharacteristics.sendData();
		RequestHandler enableCharacteristics = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive")), 1000, request -> rw.writeData("list", "Characteristic", null, qp, request, System.out::println) );
		for(String[] bad : bads) {
			enableCharacteristics.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + bad[0] + "'")).put("values", new org.json.JSONArray().put(true)));
		}
		enableCharacteristics.sendData();
	}
	
	private static void queryDataForSpecificAttributes(java.util.LinkedList<String[]> bads) {
		org.json.JSONArray columns = new org.json.JSONArray();
		java.util.LinkedList<String> header = new java.util.LinkedList<>();
		StringBuilder sb = new StringBuilder();
		StringBuilder sbQ = new StringBuilder();
		header.addLast("ProductNo");
		sb.append("Product2G.ProductNo");
		for(String[] bad : bads) {
			header.addLast(bad[0]);
			columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('" + bad[0] + "',root,\"0000.0000.RK\",'" + bad[0] + "',-1)"));
			sb.append(sb.length() == 0 ? "" : ",").append("Product2GCharacteristicValueLang.Value('" + bad[0] + "',root,\"0000.0000.RK\",'" + bad[0] + "',-1)");
			sbQ.append(sbQ.length() == 0 ? "" : " or ").append("not Product2GCharacteristicValueLang.Value('" + bad[0] + "',root,\"0000.0000.RK\",'" + bad[0] + "',-1) is empty");
		}
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", sb.toString());
		qp.put("query", sbQ.toString());
		System.out.println("Using following query: " + sbQ.toString());
		qp.put("pageSize", "1000");
		java.util.concurrent.ConcurrentLinkedQueue<org.json.JSONArray> pieces = new java.util.concurrent.ConcurrentLinkedQueue<>();
		java.util.LinkedList<org.json.JSONArray> rows = new java.util.LinkedList<>();
		System.out.println("Collecting...");
		if(!java.nio.file.Files.exists(java.nio.file.Paths.get("C:", "opt", "LVP", "tmp", "Productos con valores a respaldar para cambio de lkp2"))) {
			rw.collectData("list", "Product2G", null, "bySearch", qp, row -> rows.addLast(row.getJSONArray("values")), System.out::println);
			System.out.println("Collected: " + rows.size());
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "tmp", "Productos con valores a respaldar para cambio de lkp2").toFile(), true)))){
				pw.println( rw.getRw().serializeChunk(header.toArray(new String[] {}) , "\"", ",", "\\") );
				for(org.json.JSONArray row : rows) {
					pw.println( rw.getRw().serializeChunk( toArray(row) , "\"", ",", "\\") );
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}else {
			try(java.util.stream.Stream<String> stream = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "tmp", "Productos con valores a respaldar para cambio de lkp2"))){
				stream.parallel().map( CompareCharacteristicsThatAreLookupWithSTEPAttributes::toJSONArray ).forEach( pieces::add );
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		java.util.Map<String, String> qpw = new java.util.TreeMap<>();
		qpw.put("includeObjectsInProtocol", "false");
		RequestHandler request = new RequestHandler(columns, 1000, requestW -> rw.writeData("list", "Product2G", null, qpw, requestW, System.out::println) );
		String id = null;
		for(org.json.JSONArray values : rows) {
			id = values.getString(0);
			values.remove(0);
			for(int j = 0; j<values.length(); j++) {
				values.put(j,  "" );
			}
			request.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@1")).put("values", values));
		}
		request.sendData();
	}
	
	private static org.json.JSONArray toJSONArray(String s){
		org.json.JSONArray values = new org.json.JSONArray();
		String[] pieces = rw.getRw().parseLine(s, "\"", ",", "\\");
		for(int i=0; i<pieces.length; i++) {
			values.put(pieces[i]);
		}
		return values;
	}

	private static String[] toArray(org.json.JSONArray values) {
		String[] data = new String[values.length()];
		Object o = null;
		for(int i=0; i<values.length(); i++) {
			o = values.get(i);
			data[i] = o instanceof org.json.JSONArray ? ((org.json.JSONArray)o).getString(0) : String.valueOf(values.get(i));
		}
		return data;
	}
}
