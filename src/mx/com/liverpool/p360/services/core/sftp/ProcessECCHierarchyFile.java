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

public class ProcessECCHierarchyFile {

	private final RESTWorkshop rw = new RESTWorkshop(true, PropertiesManager.get("p360.contingency.base_url"), "Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + PropertiesManager.get("p360.contingency.basic_token_auth"));
	private final XMLMisc xmm = rw.getXmm();
	
	private final int batchSize = 250;
	
	private final org.json.JSONObject request = new org.json.JSONObject();
	private final org.json.JSONArray columns = new org.json.JSONArray();
	private final org.json.JSONArray rowsPayload = new org.json.JSONArray();
	
	private final java.util.Map<String, String> conSusPas = new java.util.TreeMap<>();
	
	private final String sge = "/list/StructureGroup";
	
	private static final java.nio.file.Path STATE_FILE = java.nio.file.Paths.get( "ProcessedForECCHierarchy" );
	
	public ProcessECCHierarchyFile() {
		columns.put(new org.json.JSONObject().put("identifier", "StructureGroupLang.Name(es)"));
		request.put("columns", columns);
		request.put("rows", rowsPayload);
	}
	
	public static void main(String[] args) throws IOException {
		long init = System.currentTimeMillis();
		ProcessECCHierarchyFile ecch = new ProcessECCHierarchyFile();
		ecch.log("Now filtering files.-.");
		java.io.File[] files = new java.io.File(args[0]).listFiles(ff-> ff.getName().startsWith("GPOARTP360") && ff.getName().toLowerCase().endsWith(".xml") );
		ecch.log("Processing over: " + files.length + " files.");

        // Load state
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
		
		Node idDireccionRaizNode = null;
		String idDirRaiz = null;
		
		Node nameNode = null;
		String name = null;
		Node idNode = null;
		String id = null;
		String name1 = null;
		Node idNodeSr = null;
		String idSr = null;
		Node idNodeGa = null;
		String idGa = null;
		Node idNodePAGrRt = null;
		String idPAGrRt = null;
		java.util.LinkedList<Node> classificationDcRtList = null;
		
		java.util.LinkedList<Node> classificationDRtList = null;
		java.util.LinkedList<Node> classificationSrList = null;
		java.util.LinkedList<Node> classificationGaList = null;
		java.util.LinkedList<Node> classificationPAGrRtList = null;
		
		classificationsList = xmm.listImmediateChildElements(rootElement).get("Classifications");
		if(classificationsList != null) {
			for(Node classificationsListNode : classificationsList) {
				classificationAllScList = xmm.listImmediateChildElements(classificationsListNode).get("ClassificationSc");
				if(classificationAllScList != null) {
					log("Going to process a ClassificationSc");
					for(Node classificationAllScListNode : classificationAllScList) {
						classificationDcRtList = xmm.listImmediateChildElements(classificationAllScListNode).get("ClassificationDcRt");
						if(classificationDcRtList != null) {
							for(Node classificationDcRtListNode : classificationDcRtList) {
								nameNode = xmm.byName(classificationDcRtListNode, "Name");
								name = nameNode != null ? nameNode.getTextContent() : null;
								idDireccionRaizNode = xmm.byName(classificationDcRtListNode, "Id");
								idDirRaiz = idDireccionRaizNode != null ? idDireccionRaizNode.getTextContent() : null;
								if(idDirRaiz != null && !"".equals(idDirRaiz)) {
									idDirRaiz = idDirRaiz + "-L1ECC";
									rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + idDirRaiz + "'@'CommercialECC'"))
											.put("values", new org.json.JSONArray().put(name != null ? name : "")));
									if(rowsPayload.length() == batchSize) {
										sendIt();
										sendWithPas();
									}
									classificationDRtList = xmm.listImmediateChildElements(classificationDcRtListNode).get("ClassificationDRt");
									if(classificationDRtList != null) {
										for(Node classificationDRtListNode : classificationDRtList) {
											
											idNode = xmm.byName(classificationDRtListNode, "Id");
											nameNode = xmm.byName(classificationDRtListNode, "Name");
											id = idNode != null ? idNode.getTextContent() : null;
											name1 = nameNode != null ? nameNode.getTextContent() : null;
											if(id != null && !"".equals(id)) {
												id = id + "-L2ECC";
												conSusPas.put(id, idDirRaiz);
												rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@'CommercialECC'"))
														.put("values", new org.json.JSONArray().put(name1 == null ? "" : name1)));
												if(rowsPayload.length() == batchSize) {
													sendIt();
													sendWithPas();
												}
												
												classificationSrList = xmm.listImmediateChildElements(classificationDRtListNode).get("ClassificationSr");
												if(classificationSrList != null) {
													for(Node classificationSrNode : classificationSrList) {
														idNodeSr = xmm.byName(classificationSrNode, "Id");
														nameNode = xmm.byName(classificationSrNode, "Name");
														idSr = idNodeSr != null ? idNodeSr.getTextContent() : null;
														name = nameNode != null ? nameNode.getTextContent() : null;
														if(idSr != null && !"".equals(idSr)) {
															idSr = idSr + "-L3ECC";
															conSusPas.put(idSr, id);
															rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + idSr + "'@'CommercialECC'"))
																	.put("values", new org.json.JSONArray().put(name == null ? "" : name)));
															if(rowsPayload.length() == batchSize) {
																sendIt();
																sendWithPas();
															}
															
															classificationGaList = xmm.listImmediateChildElements(classificationSrNode).get("ClassificationGa");
															if(classificationGaList != null) {
																for(Node classificationGaNode : classificationGaList) {
																	idNodeGa = xmm.byName(classificationGaNode, "Id");
																	nameNode = xmm.byName(classificationGaNode, "Name");
																	idGa = idNodeGa != null ? idNodeGa.getTextContent() : null;
																	name = nameNode != null ? nameNode.getTextContent() : null;
																	if(idGa != null && !"".equals(idGa)) {
																		idGa = idGa + "-L4ECC";
																		conSusPas.put(idGa, idSr);
																		rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + idGa + "'@'CommercialECC'"))
																				.put("values", new org.json.JSONArray().put(name == null ? "" : name)));
																		if(rowsPayload.length() == batchSize) {
																			sendIt();
																			sendWithPas();
																		}
																		
																		classificationPAGrRtList = xmm.listImmediateChildElements(classificationGaNode).get("ClassificationPAGrRt");
																		if(classificationPAGrRtList != null) {
																			for(Node classificationPAGrRtNode : classificationPAGrRtList) {
																				idNodePAGrRt = xmm.byName(classificationPAGrRtNode, "Id");
																				nameNode = xmm.byName(classificationPAGrRtNode, "Name");
																				idPAGrRt = idNodePAGrRt != null ? idNodePAGrRt.getTextContent() : null;
																				name = nameNode != null ? nameNode.getTextContent() : null;
																				if(idPAGrRt != null && !"".equals(idPAGrRt)) {
																					idPAGrRt = idPAGrRt + "-L5ECC";
																					conSusPas.put(idPAGrRt, idGa);
																					rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + idPAGrRt + "'@'CommercialECC'"))
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
			rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + k + "'@'CommercialECC'")).put("values", new org.json.JSONArray().put(v)));
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
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseECCHierarchy.log").toString(), true)))) {
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
					+ "]  " + message);
		} catch (java.io.IOException e) {
		}
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","parseECCHierarchy.log").toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
	
}
