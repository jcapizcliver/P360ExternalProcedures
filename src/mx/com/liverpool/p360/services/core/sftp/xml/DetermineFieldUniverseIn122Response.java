package mx.com.liverpool.p360.services.core.sftp.xml;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.sftp.handlers.ECC122ResponseHandler;
import mx.com.liverpool.p360.services.core.sftp.handlers.Product122;

public class DetermineFieldUniverseIn122Response {

	
	public static void main(String[] args) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);

        // Harden parser (avoid XXE)
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
		SAXParser parser = null;
        try {
			parser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
        java.util.List<Product122> products = null;
        java.util.Set<String> keysNotInSection = new java.util.TreeSet<>();
        java.util.Map<String, String> keysInSection = new java.util.TreeMap<>();
        java.util.Map<String, String> qp = new java.util.TreeMap<>();
        qp.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier,StandardizationValue.PropertyValue");
        qp.put("query", "StandardizationValue.Property->LookupValue.Code = \"VendorCenterSection\"");
        qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
        qp.put("pageSize", "25000");
        java.util.Map<String, String> fieldVendorCenterSection = new java.util.TreeMap<>();
        RESTWrapper rw = new RESTWrapper();
        System.out.println("Now collecting from templates...");
        rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
        	org.json.JSONArray values = row.getJSONArray("values");
        	fieldVendorCenterSection.put(values.getString(0), values.getString(1));
        });
        System.out.println("Got: " + fieldVendorCenterSection.size());
        System.out.println("Now collecting from global...");
        qp.put("dictionaryProxy", "'GlobalTemplateAttributeConfiguration'");
        rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
        	org.json.JSONArray values = row.getJSONArray("values");
        	fieldVendorCenterSection.put(values.getString(0), values.getString(1));
        });
        System.out.println("Got: " + fieldVendorCenterSection.size());
        java.util.List<mx.com.liverpool.p360.services.core.sftp.handlers.Value> values = null;
        if(parser != null) {
        	String section = null;
			ECC122ResponseHandler handler = new ECC122ResponseHandler();
			java.io.File[] files = java.nio.file.Paths.get("/", "u01", "stage", "ECC_122", "processed").toFile().listFiles(ff -> ff.getName().endsWith(".XML"));
			java.math.BigDecimal a = new java.math.BigDecimal(0);
			java.math.BigDecimal len = new java.math.BigDecimal(files.length);
			for(java.io.File f : files) {
				try {
					System.out.print( getSizeInMB( java.nio.file.Files.size(f.toPath()) ) + " MB. ");
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					parser.parse(f, handler);
					products = handler.getCollected();
					for(Product122 p : products) {
						values = p.getValues();
						for(mx.com.liverpool.p360.services.core.sftp.handlers.Value v : values) {
							section = fieldVendorCenterSection.get(v.getAttributeId());
							if(section != null) {
								keysInSection.put(v.getAttributeId(), section);
							}else {
								keysNotInSection.add(v.getAttributeId());
							}
						}
					}
				} catch (SAXException | IOException e) {
					e.printStackTrace();
				}
				a = a.add(java.math.BigDecimal.ONE);
				System.out.println(a + "/" + files.length + " (" + java.math.BigDecimal.TEN.pow(2).multiply(a).divide(len, 4, java.math.RoundingMode.HALF_UP) + ")");
			}
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("InSection.csv").toFile())))){
				keysInSection.entrySet().forEach( entry -> pw.println( rw.getRw().serializeChunk(new Object[] { entry.getKey(), entry.getValue() }) ));
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("NotInSection.csv").toFile())))){
				keysNotInSection.forEach(pw::println);
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
        }
	}
	
	private static java.math.BigDecimal getSizeInMB(long l){
		return new java.math.BigDecimal(l).divide( new java.math.BigDecimal(1024).pow(2), 4, java.math.RoundingMode.HALF_UP );
	}
	
}
