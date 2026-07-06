package mx.com.liverpool.p360.services.core.temp.xml;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class FlattenSpecificData {

	private static final RESTWorkshop rw = new RESTWorkshop();
	private static final XMLMisc xmm = rw.getXmm();
	
	public static void main(String[] args) {
		FlattenSpecificData f = new FlattenSpecificData();
		try {
			f.flattenData("C:\\opt\\LVP\\desorden\\Migración\\second09092025\\data");
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}
	
	private void flattenData(String basePath) throws ParserConfigurationException, FileNotFoundException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
    	java.io.File[] files = new java.io.File(basePath).listFiles(ff->ff.getName().endsWith(".xml"));
		log("Collecting data...");

		int cnt = 0;
		java.util.LinkedList<String> multiValueContent = new java.util.LinkedList<>();
		java.util.Map<String, java.util.LinkedList<Node>> childElementsMap = null;
		java.util.LinkedList<Node> valuesList = null;
		java.util.LinkedList<Node> multiValueList = null;
		Element valueElement = null;
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		String proposalId = null;
		String template = null;
		String flujo = null;
		String bandeja = null;
		String supplierId = null;
		String elWorkflow = null;
		java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("(?<=Flujo Actual: )([^\\|]+)(?=\\|)");
		java.util.regex.Pattern p3 = java.util.regex.Pattern.compile("(?<=Estado en el WF: )([^\\|]+)(?=\\|)");
		java.util.regex.Matcher m = null;
		String delim = "\"";
		String sep = ",";
		String esc = "\\";
		String value = null;
		java.util.LinkedList<String> firstData = new java.util.LinkedList<>();
		java.util.LinkedList<String> header = new java.util.LinkedList<>();
		int gl = 0;
		int gral = 0;
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(
				"C:", "opt", "LVP", "desorden", "Migración", "09092025", "data", "flattened4.csv").toString()), java.nio.charset.StandardCharsets.UTF_8));
			java.io.PrintWriter pw2 = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(
						"C:", "opt", "LVP", "desorden", "Migración", "second09092025", "Amorfo4"
					).toString())))){
			for(String key  : ofInterest) {
				header.addLast(key);
			}
			header.addFirst("SUPPLIER_LINK");
			header.addFirst("TEMPLATE_ID");
			header.addFirst("PARENT_ID");
			header.addFirst("PRODUCT_ID");
			header.addLast("FLUJO");
			header.addLast("BANDEJA");
			header.addLast("FCH_CARGA");
	    	pw.println( rw.serializeChunk(header.toArray(new String[] {}), delim, sep, esc) );
	    	java.util.LinkedList<Node> hijos = null;
	    	java.util.LinkedList<Node> cr = null;
	    	Double[] timesCollector = new Double[1];
	    	timesCollector[0] = 0d;
	    	long fti = 0;
	    	long diff = 0;
	    	java.util.Map<String, Long> timesPerFile = new java.util.TreeMap<>();
	    	java.util.Map<String, java.util.LinkedList<Node>> productContents = null;
	    	java.util.Map<String, java.util.Map<String, java.util.LinkedList<Node>>> generalBoard = new java.util.TreeMap<>();
	    	java.util.Map<String, String> parentChild = new java.util.TreeMap<>();
	    	java.util.Map<String, Integer> childCount = new java.util.TreeMap<>();
			for(java.io.File f : files) {
				fti = System.currentTimeMillis();
				doc = builder.parse(new java.io.FileInputStream(f));
				doc.getDocumentElement().normalize();
				java.util.LinkedList<Node> productNodes = xmm.listImmediateChildElements( 
																xmm.listImmediateChildElements(doc.getDocumentElement()).get("Products").getFirst()
														).get("Product");
				System.out.println(f.getName() + " A file with: " + productNodes.size());
				for(Node productNode : productNodes) {
						template = ((Element)productNode).getAttribute("ParentID");
						proposalId = ((Element)productNode).getAttribute("ID");
//						parentChild.put(proposalId, template);
						childElementsMap = xmm.listImmediateChildElements(productNode);
						if(!childElementsMap.containsKey("Values")) {
							pw2.println(proposalId + " in " + f.getName());
							continue;
						}
						productContents = xmm.listImmediateChildElements( childElementsMap.get("Values").getFirst() );
//						generalBoard.put(proposalId, productContents);
						valuesList = productContents.get("Value");
						multiValueList = productContents.get("MultiValue");
						cr = childElementsMap.get("ClassificationReference");
						if(cr != null) {
							supplierId = null;
							for(Node ncr : cr) {
								if("SupplierLink".equals(((Element)ncr).getAttribute("Type"))) {
									supplierId = ((Element)ncr).getAttribute("ClassificationID").replaceAll("-.+", "");
								}
							}
						}else {
							supplierId = null;
						}
						hijos = childElementsMap.get("Product");
						if(valuesList != null) {
							for(Node valueNode : valuesList) {
								valueElement = (Element)valueNode;
								data.put(valueElement.getAttribute("AttributeID"), valueElement.hasAttribute("ID") ? valueElement.getAttribute("ID") : valueElement.getTextContent());
							}
						}
						if(multiValueList != null) {
							for(Node multiValueNode : multiValueList) {
								valuesList = xmm.listImmediateChildElements( multiValueNode ).get("Value");
								if(valuesList != null) {
									for(Node valueNode : valuesList) {
										multiValueContent.addLast(valueNode.getTextContent());
									}
									data.put(((Element)multiValueNode).getAttribute("AttributeID"), rw.serializeChunk(multiValueContent.toArray(new String[] {}), "\"", ";", "\\") );
								}
							}
						}
						elWorkflow = data.get("CalculatedWF_Att");
						if(elWorkflow != null) {
							m = p1.matcher(elWorkflow);
							if(m.find()) {
								flujo = m.group();
							}else {
								flujo = null;
							}
							m = p3.matcher(elWorkflow);
							if(m.find()) {
								bandeja = m.group();
							}else {
								bandeja = null;
							}
						}else {
							flujo = null;
							bandeja = null;
						}
						for(String key  : ofInterest) {
							value = data.get(key);
							firstData.addLast(value == null ? "" : value.replaceAll("\r\n", "<::>").replaceAll("\n", "<::>"));
						}
						firstData.addFirst(supplierId == null ? "" : supplierId);
						firstData.addFirst( template  );
						firstData.addFirst( "" );
						firstData.addFirst(proposalId);
						firstData.addLast(flujo == null ? "" : flujo);
						firstData.addLast(bandeja == null ? "" : bandeja);
						firstData.addLast(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ").format(new java.util.Date()));
						pw.println( rw.serializeChunk(firstData.toArray(new String[] {}), delim, sep, esc) );
						firstData.clear();
						data.clear();
//						System.out.println("\t" + proposalId + " A product with: " + (hijos == null ? 0 : hijos.size()));
						if(hijos != null) {
							childCount.put(proposalId, hijos.size());
							for(Node hijo : hijos) {
								template = ((Element)hijo).getAttribute("ParentID");
								proposalId = ((Element)hijo).getAttribute("ID");
//								parentChild.put(proposalId, template);
								childElementsMap = xmm.listImmediateChildElements(hijo);
								productContents = xmm.listImmediateChildElements( childElementsMap.get("Values").getFirst() );
//								generalBoard.put(proposalId, productContents);
								valuesList = productContents.get("Value");
								multiValueList = productContents.get("MultiValue");
								hijos = childElementsMap.get("Product");
								cr = childElementsMap.get("ClassificationReference");
								if(cr != null) {
									supplierId = null;
									for(Node ncr : cr) {
										if("SupplierLink".equals(((Element)ncr).getAttribute("Type"))) {
											supplierId = ((Element)ncr).getAttribute("ClassificationID").replaceAll("-.+", "");
										}
									}
								}else {
									supplierId = null;
								}
								if(valuesList != null) {
									for(Node valueNode : valuesList) {
										valueElement = (Element)valueNode;
										data.put(valueElement.getAttribute("AttributeID"), valueElement.hasAttribute("ID") ? valueElement.getAttribute("ID") : valueElement.getTextContent());
									}
								}
								if(multiValueList != null) {
									for(Node multiValueNode : multiValueList) {
										valuesList = xmm.listImmediateChildElements( multiValueNode ).get("Value");
										if(valuesList != null) {
											for(Node valueNode : valuesList) {
												multiValueContent.addLast(valueNode.getTextContent());
											}
											data.put(((Element)multiValueNode).getAttribute("AttributeID"), rw.serializeChunk(multiValueContent.toArray(new String[] {}), "\"", ";", "\\") );
										}
									}
								}
								elWorkflow = data.get("CalculatedWF_Att");
								if(elWorkflow != null) {
									m = p1.matcher(elWorkflow);
									if(m.find()) {
										flujo = m.group();
									}else {
										flujo = null;
									}
									m = p3.matcher(elWorkflow);
									if(m.find()) {
										bandeja = m.group();
									}else {
										bandeja = null;
									}
								}else {
									flujo = null;
									bandeja = null;
								}
								for(String key  : ofInterest) {
									value = data.get(key);
									firstData.addLast(value == null ? "" : value.replaceAll("\r\n", "<::>").replaceAll("\n", "<::>"));
								}
								firstData.addFirst(supplierId == null ? "" : supplierId);
								firstData.addFirst("");
								firstData.addFirst( template  );
								firstData.addFirst(proposalId);
								firstData.addLast(flujo == null ? "" : flujo);
								firstData.addLast(bandeja == null ? "" : bandeja);
								firstData.addLast(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ").format(new java.util.Date()));
								pw.println( rw.serializeChunk(firstData.toArray(new String[] {}), delim, sep, esc) );
								firstData.clear();
								data.clear();
								gl++;
								if(gl % 100 == 0) {
//									System.out.print(".");
									if(gl % 10000 == 0) {
//										System.out.println(gl);
									}
								}
							}
							gral++;
							gl++;
							if(gl % 100 == 0) {
//								System.out.print(".");
								if(gl % 10000 == 0) {
//									System.out.println(gl);
								}
							}
						}
				}
				timesCollector[0] += (diff = System.currentTimeMillis() - fti);
				timesPerFile.put(f.getName(), diff);
	    	}
			java.util.LinkedList<java.util.Map.Entry<String, Integer>> entries = new java.util.LinkedList<>(childCount.entrySet());
			java.util.Collections.sort(entries, (o1,o2)-> o2.getValue().compareTo(o1.getValue()) );
			entries.forEach(e -> System.out.println(e.getKey() + " - " + e.getValue()));
			java.util.LinkedList<java.util.Map.Entry<String, Long>> timesList = new java.util.LinkedList<>(timesPerFile.entrySet());
			java.util.Collections.sort(timesList, (o1,o2)-> o2.getValue().compareTo(o1.getValue()) );
			System.out.println("********************");
			timesList.forEach(e -> System.out.println(e.getKey() + " - " + e.getValue()));
			System.out.println("Avg: " + timesCollector[0]/files.length);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private void log(String message) {
		System.out.println(message);
	}
	
	private static final java.util.Set<String> ofInterest = new java.util.TreeSet<>( java.util.Arrays.asList(new String[] {
			"Direction",
			"Section",
			"ItemGroup",
			"ItemGroupS4H",
			"SKU",
			"MainBarCode",
			"MainBarCodeS4H",
			"BrandName",
			"BRAND_ID_S4H",
			"SupplierPartNumber",
			"CalculatedWF_Att",
			"Path",
			"Negocio",
			"ParentSKU",
			"SkuType",
			"Name",
			"ProductName",
			"SupplierID",
			"SupplierName",
			"StateSKU",
			"SKUCreationDate",
			"LastDateApprove",
			"FirstDateApprove",
			"SAPObjectType"
	})
			);
	
}
