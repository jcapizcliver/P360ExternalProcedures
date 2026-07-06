package mx.com.liverpool.p360.services.core.temp.source;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class XMLReportaConjuntoLookSAP {

	private static final RESTWorkshop workshop = new RESTWorkshop();
	private static final XMLMisc xmm = workshop.getXmm();

    public static void main(String[] args) {
    	try {
			processFile();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
    }
	
	public static void processFile() throws ParserConfigurationException, SAXException, IOException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse("C:\\opt\\LVP\\desorden\\GenericXMLconjunto20250612103338.XML");
		doc.getDocumentElement().normalize();
		Element rootElement = doc.getDocumentElement();

		java.util.LinkedList<Node> products = xmm.listImmediateChildElements(rootElement).get("Product");
		java.util.LinkedList<Node> conjuntoLookList = null;

		String idConjunto = null;
		String fechaEnv = null;
		String descr = null;
		String ffn = null;
		String fin = null;

		String attributeId = null;
		String value = null;

		java.util.LinkedList<Node> listaAtributoCL = null;
		java.util.LinkedList<Node> miembrosCL = null;
		java.util.LinkedList<Node> atributosMiembroCL = null;

		Element el = null;

		org.json.JSONObject conjunto = null;
		org.json.JSONArray miembros = null;
		org.json.JSONObject miembro = null;

		PubSubGCP ps = new PubSubGCP();
		
		org.json.JSONArray rows = new org.json.JSONArray();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		for(Node n : products) {
			conjuntoLookList = xmm.listImmediateChildElements(n).get("Values");
			for( Node cln : conjuntoLookList ) {
				listaAtributoCL = xmm.listImmediateChildElements(cln).get("Value");
				miembrosCL = xmm.listImmediateChildElements(cln).get("VALUE");
				conjunto = new org.json.JSONObject();
				miembros = new org.json.JSONArray();
				fin = null;
				ffn = null;
				descr = null;
				fechaEnv = null;
				idConjunto = null;
				for(Node acl : listaAtributoCL) {
					el = (Element) acl;

					attributeId = el.getAttribute("AttributeID");
					value = el.getTextContent();
					if("ZZFECHA_ENV".equals(attributeId)) {
						fechaEnv = value;
					}else if("ZZID_CONJUNTO".equals(attributeId)) {
						idConjunto = value;
					}else if("ZZFEC_INI".equals(attributeId)) {
						if(fin == null) {
							fin = value;
						} else {
							ffn = value;
						}
					}else if("ZZFEC_FIN".equals(attributeId)) {
						ffn = value;
					}else if("ZZDES_CONJUNTO".equals(attributeId)) {
						descr = value;
					}
				}
				System.out.println(idConjunto + " <::>" + descr);
				for(Node m : miembrosCL) {
					miembro = new org.json.JSONObject();
					atributosMiembroCL = xmm.listImmediateChildElements(m).get("Value");
					System.out.println("Processing VALUE tag");
					for(Node atm : atributosMiembroCL) {
						System.out.println("Processing value within VALUE. " + ((Element)atm).getAttribute("AttributeID") + " - " + atm.getTextContent());
						el = (Element)atm;
						attributeId = el.getAttribute("AttributeID");
						if("MATNR".equals(attributeId)) {
							miembro.put("sku", atm.getTextContent());
						}else if("ZZMAIN".equals(attributeId)) {
							miembro.put("itemPrincipal", Boolean.parseBoolean( atm.getTextContent() ));
						}else if("ZZSEQUENCE".equals(attributeId)) {
							miembro.put("sequence", atm.getTextContent());
						}else if("ZZSTATUS".equals(attributeId)) {
							miembro.put("status", atm.getTextContent());
						}
					}
					miembros.put(miembro);
				}
				conjunto.put("lookupGroupId", idConjunto);
				conjunto.put("approvedAt", fechaEnv);
				conjunto.put("createdAt", new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()));
				String end = ffn;
				try{
					end = getFarther(fin, ffn);
				}catch(java.text.ParseException e) {
					e.printStackTrace();
				}
				conjunto.put("startAt", fin.equals(end) ? ffn : fin  );
				conjunto.put("endAt", end);
				conjunto.put("variants", miembros);

				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id",  "'" + idConjunto + "'@'MASTER'")).put("values",
						new org.json.JSONArray().put(descr).put(fin).put(ffn).put(fechaEnv)));
				System.out.println(conjunto.toString());
//				ps.publishMessage("crp-dev-dig-vccatalog", "idmc_post_look", "/P360shared/IDMC/workshop/crp-dev-dig-vccatalog-b74410667aea.json", conjunto.toString());
//				ps.publishMessage("crp-qas-dig-vccatalog", "idmc_post_look", "/P360shared/IDMC/workshop/crp-qas-dig-vccatalog-416185bab156.json", conjunto.toString());
			}
//			org.json.JSONObject resp = workshop.makeRequest("POST", "/list/Product2G", qp,
//					new org.json.JSONObject()
//					.put("columns",
//						new org.json.JSONArray()
//							.put(new org.json.JSONObject().put("identifier", "Product2GLang.DescriptionShort(es)"))
//							.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('StartDate',root,\"0000.0000.RK\",'StartDate',-1)"))
//							.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('EndDate',root,\"0000.0000.RK\",'EndDate',-1)"))
//							.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('FechaEnvio',root,\"0000.0000.RK\",'FechaEnvio',-1)"))
//							)
//					.put("rows", rows )
//					.toString() );
//			System.out.println( resp == null ? "ERR: " + workshop.getRawResponse() : resp );
		}	
	}

	private static String getFarther(String d1, String d2) throws java.text.ParseException {
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
		java.util.Date date1 = sdf.parse(d1);
		java.util.Date date2 = sdf.parse(d2);
		return date1.compareTo(date2) > 0 ? d1 : d2;

	}
}
