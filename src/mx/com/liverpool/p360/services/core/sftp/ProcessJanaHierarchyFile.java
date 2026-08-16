package mx.com.liverpool.p360.services.core.sftp;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class ProcessJanaHierarchyFile {

	private final RESTWorkshop rw = new RESTWorkshop(true, PropertiesManager.get("p360.contingency.base_url"), "Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
	private final XMLMisc xmm = rw.getXmm();
	
	private final int batchSize = 250;
	
	private final org.json.JSONObject request = new org.json.JSONObject();
	private final org.json.JSONArray columns = new org.json.JSONArray();
	private final org.json.JSONArray rowsPayload = new org.json.JSONArray();
	
	private final java.util.Map<String, String> conSusPas = new java.util.TreeMap<>();
	
	private final String sge = "/list/StructureGroup";
	
	private static final java.nio.file.Path STATE_FILE = java.nio.file.Paths.get( "ProcessedForJanaHierarchy" );
	
	
	public ProcessJanaHierarchyFile() {
		columns.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"));
		request.put("columns", columns);
		request.put("rows", rowsPayload);
	}
	
	public static void main(String[] args) throws IOException {
		
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("structure", "'CommercialECC'");
//		RESTWorkshop workshop = new RESTWorkshop();
//		workshop.addHeader("Authorization", PropertiesManager.get("p360.contingency.basic_token_auth"));
//		workshop.addHeader("Content-Type", "application/x-www-form-urlencoded");
//		workshop.setBaseUrl(PropertiesManager.get("p360.contingency.base_url"));
//		workshop.makeRequest("POST", "/list/StructureGroup/", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"))).put("rows", new org.json.JSONArray()
//					.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'SE-L1ECC'@'CommercialECC'")).put("values", new org.json.JSONArray().put("CommercialECC")))
//					.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'D1-L1ECC'@'CommercialECC'")).put("values", new org.json.JSONArray().put("CommercialECC")))
//					.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'SL-L1ECC'@'CommercialECC'")).put("values", new org.json.JSONArray().put("CommercialECC")))
//					.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'HL-L1ECC'@'CommercialECC'")).put("values", new org.json.JSONArray().put("CommercialECC")))
//					.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'D2-L1ECC'@'CommercialECC'")).put("values", new org.json.JSONArray().put("CommercialECC")))
//					)
//				.toString());
//		System.out.println(workshop.getRawResponse());
//		System.exit(0);
		long init = System.currentTimeMillis();
		ProcessJanaHierarchyFile ecch = new ProcessJanaHierarchyFile();
		ecch.log("Now filtering files.-.");
		java.io.File[] files = new java.io.File(args[0]).listFiles(ff-> ff.getName().startsWith("JER") && ff.getName().toLowerCase().endsWith(".xml") );
		ecch.log("Processing over: " + files.length + " files.");

        java.util.Properties processedState = new java.util.Properties();
        if (java.nio.file.Files.exists(  STATE_FILE  )) {
            try (InputStream in = java.nio.file.Files.newInputStream(  STATE_FILE  )) {
                processedState.load(in);
            }
        }
        String previousTimestamp = null;
		for(java.io.File f : files) {
			ecch.log("Processing: " + f.getName());
			previousTimestamp = processedState.getProperty(f.getName());
            long remoteModified = f.lastModified();
			if(previousTimestamp != null && Long.parseLong(previousTimestamp) == remoteModified) {
				ecch.log("Skipping");
                continue;
            }
			try(java.io.FileInputStream br = new java.io.FileInputStream(f)){
				ecch.sourceContents(br);
			}catch(java.io.IOException e) {
				e.printStackTrace();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			}
            processedState.setProperty(f.getName(), String.valueOf(remoteModified));
			try (java.io.OutputStream stateOut = java.nio.file.Files.newOutputStream(  STATE_FILE  )) {
                processedState.store(stateOut, null);
            }
		}
		ecch.sendRemainingData();
		ecch.log("Done. " + ecch.rw.formatTime(System.currentTimeMillis() - init));
	}
	
	public void sendRemainingData() {
		if(rowsPayload.length() > 0) {
			sendIt();
			sendWithPas();
		}
	}
	
	public void sourceContents(java.io.InputStream is) throws SAXException, IOException, ParserConfigurationException {
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse(is);
		doc.getDocumentElement().normalize();
		Element rootElement = doc.getDocumentElement();
		java.util.LinkedList<Node> classificationsList = null;
		java.util.LinkedList<Node> classificationAllScList = null;

		Node nameNode = null;
		String idDirRaiz = null;
		String name = null;
		String id = null;
		String name1 = null;
		String idSr = null;
		String idGa = null;
		String idPAGrRt = null;
		java.util.LinkedList<Node> classificationDcRtList = null;
		
		java.util.LinkedList<Node> classificationDRtList = null;
		java.util.LinkedList<Node> classificationSrList = null;
		java.util.LinkedList<Node> classificationGaList = null;
		java.util.LinkedList<Node> classificationPAGrRtList = null;
		
		classificationsList = xmm.listImmediateChildElements(rootElement).get("Classifications");
		if(classificationsList != null) {
			for(Node classificationsListNode : classificationsList) {
				classificationAllScList = xmm.listImmediateChildElements(classificationsListNode).get("Classification");
				if(classificationAllScList != null) {
					log("Going to process a Classification");
					for(Node classificationAllScListNode : classificationAllScList) {
						classificationDcRtList = xmm.listImmediateChildElements(classificationAllScListNode).get("Classification");
						if(classificationDcRtList != null) {
							for(Node classificationDcRtListNode : classificationDcRtList) {
								nameNode = xmm.byName(classificationDcRtListNode, "Name");
								name = nameNode != null ? nameNode.getTextContent() : null;
								idDirRaiz = ((Element)classificationDcRtListNode).getAttribute("ID");
								if(idDirRaiz != null && !"".equals(idDirRaiz)) {
									rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + idDirRaiz + "'@'CommercialS4H'"))
											.put("values", new org.json.JSONArray().put(name != null ? name : "")));
									if(rowsPayload.length() == batchSize) {
										sendIt();
										sendWithPas();
									}
									classificationDRtList = xmm.listImmediateChildElements(classificationDcRtListNode).get("Classification");
									if(classificationDRtList != null) {
										for(Node classificationDRtListNode : classificationDRtList) {
											
											id = ((Element)classificationDRtListNode).getAttribute("ID");
											nameNode = xmm.byName(classificationDRtListNode, "Name");
											name1 = nameNode != null ? nameNode.getTextContent() : null;
											if(id != null && !"".equals(id)) {
												conSusPas.put(id, idDirRaiz);
												rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@'CommercialS4H'"))
														.put("values", new org.json.JSONArray().put(name1 == null ? "" : name1)));
												if(rowsPayload.length() == batchSize) {
													sendIt();
													sendWithPas();
												}
												
												classificationSrList = xmm.listImmediateChildElements(classificationDRtListNode).get("Classification");
												if(classificationSrList != null) {
													for(Node classificationSrNode : classificationSrList) {
														idSr = ((Element)classificationSrNode).getAttribute("ID");
														nameNode = xmm.byName(classificationSrNode, "Name");
														name = nameNode != null ? nameNode.getTextContent() : null;
														if(idSr != null && !"".equals(idSr)) {
															conSusPas.put(idSr, id);
															rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + idSr + "'@'CommercialS4H'"))
																	.put("values", new org.json.JSONArray().put(name == null ? "" : name)));
															if(rowsPayload.length() == batchSize) {
																sendIt();
																sendWithPas();
															}
															
															classificationGaList = xmm.listImmediateChildElements(classificationSrNode).get("Classification");
															if(classificationGaList != null) {
																for(Node classificationGaNode : classificationGaList) {
																	idGa = ((Element)classificationGaNode).getAttribute("ID");
																	nameNode = xmm.byName(classificationGaNode, "Name");
																	name = nameNode != null ? nameNode.getTextContent() : null;
																	if(idGa != null && !"".equals(idGa)) {
																		conSusPas.put(idGa, idSr);
																		rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + idGa + "'@'CommercialS4H'"))
																				.put("values", new org.json.JSONArray().put(name == null ? "" : name)));
																		if(rowsPayload.length() == batchSize) {
																			sendIt();
																			sendWithPas();
																		}
																		
																		classificationPAGrRtList = xmm.listImmediateChildElements(classificationGaNode).get("Classification");
																		if(classificationPAGrRtList != null) {
																			for(Node classificationPAGrRtNode : classificationPAGrRtList) {
																				idPAGrRt = ((Element)classificationPAGrRtNode).getAttribute("ID");
																				nameNode = xmm.byName(classificationPAGrRtNode, "Name");
																				name = nameNode != null ? nameNode.getTextContent() : null;
																				if(idPAGrRt != null && !"".equals(idPAGrRt)) {
																					conSusPas.put(idPAGrRt, idGa);
																					rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + idPAGrRt + "'@'CommercialS4H'"))
																							.put("values", new org.json.JSONArray().put(name == null ? "" : name)));
																					if(rowsPayload.length() == batchSize) {
																						sendIt();
																						sendWithPas();
																					}
																				}
																				
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
											
										}
									}
								}
							}
						}
					}
				} else {
					log("Abandoning");
				}
			}
		}
	}
	
	private void sendIt() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = rw.makeRequest("POST", sge, qp, request.toString());
		if(response != null && response.has("counters")) {
			try{
				log(response.getJSONObject("counters").toString());
			}catch(org.json.JSONException | NullPointerException e) {
				log("Unparseable content --->" + rw.getRawResponse());
				logE(e);
			}
		}else {
			log("ERROR: " + rw.getRawResponse());
			logE(rw.getException());
		}
		while(rowsPayload.length() > 0) {
			rowsPayload.remove(0);
		}
	}
	
	private void sendWithPas() {
		columns.remove(0);
		columns.put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"));
		conSusPas.forEach((k,v)->{
			rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + k + "'@'CommercialS4H'")).put("values", new org.json.JSONArray().put(v)));
			if(rowsPayload.length() == batchSize) {
				sendIt();
			}
		});
		if(rowsPayload.length() > 0) {
			sendIt();
		}
		conSusPas.clear();
		columns.remove(0);
		columns.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"));
	}
	
	private void log(String message) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseJanaHierarchy.log").toString(), true)))) {
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
					+ "]  " + message);
		} catch (java.io.IOException e) {
		}
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseJanaHierarchy.log").toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
	
}
