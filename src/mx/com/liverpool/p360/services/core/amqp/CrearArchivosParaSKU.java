package mx.com.liverpool.p360.services.core.amqp;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.core.net.DataRequestor;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class CrearArchivosParaSKU implements Closeable {

	private final RESTWrapper rw = new RESTWrapper();
	private final RESTWorkshop workshop = rw.getRw();
	private final XMLMisc xmm = workshop.getXmm();
	private final java.util.Map<String, java.util.LinkedList< org.json.JSONObject >> characteristicsMap = new java.util.TreeMap<>();
	
	private DBAccessDataStub dastub = new DBAccessDataStub( new ELog() {
		
		@Override
		public void logE(Exception e) {
			CrearArchivosParaSKU.this.logE(e);
		}
		
		@Override
		public void log(String message) {
			CrearArchivosParaSKU.this.log(message);
		}
	} );
	
	private final DataRequestor dr = new DataRequestor(dastub);

    private static final String HOST = PropertiesManager.get( "p360.contingency.ecc.host" ); 
    private static final String HOST_SBB = PropertiesManager.get( "p360.contingency.s4h.host" );
    private static final int PORT = 22; // SFTP server port
    private static final String USER = "userp360"; // SFTP username
    private static final Path PRIVATE_KEY_PATH = Paths.get("/home/P360admin/.ssh/id_rsa"); // Path to private key
    private static final Path SEQUENCE_FILE = Paths.get("upload_sequence_to_SKU.properties");

    private String filePrefix = "P360A";
	
	private boolean running = true;
    
    public CrearArchivosParaSKU() {
    }

	public String[] creacionDeArchivos(String externalId, short modif) throws ServiceUnavailableException {
		String[] content = new String[5];
		content[0] = null;
		content[1] = null;
		content[2] = null;
		content[3] = null;
		content[4] = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("entityFilter", "Product2G,Product2GCharacteristicValue,Product2GStructureGroupMap");
		qp.put("includeLabels", "true");
		qp.put("includeIds", "true");
		log("Querying data... ");
		org.json.JSONObject response = null;
		org.json.JSONObject data = null;
		org.json.JSONArray characteristicRecords = null;
		java.util.Map<String, String> atributos = null;

		response = workshop.makeRequest("GET", "/object/Product2G/'" + externalId + "'@'MASTER'", qp, null);
		log(response == null ? workshop.getRawResponse() : response.toString());
		String sapObjectType = null;
		String business = null;
		String template = null;
		java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> lalista = null;
		java.util.LinkedList<String> lineasH = new java.util.LinkedList<>();
		java.util.LinkedList<String> lineasD = new java.util.LinkedList<>();
		java.util.LinkedList<String> lineasUOM = new java.util.LinkedList<>();
		java.util.LinkedList<String> lineasATT = new java.util.LinkedList<>();
		String lineasbb = null;
		String linea = null;

		StringBuilder sbH = new StringBuilder();
		StringBuilder sbUOM = new StringBuilder();
		StringBuilder sbATT= new StringBuilder();
		java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> yep = new java.util.LinkedList<>();
		String latapa = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date()) + "|" + new java.text.SimpleDateFormat("yyyyMMddhhmmss").format(new java.util.Date()) + "|STEP";
		if(response != null) {
			if(response.has("_data")) {
				data = response.getJSONObject("_data");
				if(data.has("_characteristicRecords")) {
					characteristicRecords = data.getJSONArray("_characteristicRecords");
					buildCharacteristicsMap(characteristicRecords, characteristicsMap);
					sapObjectType = grabSimpleValue("SAPObjectType");
					business = grabSimpleValue("Business");
					yep.addLast(new java.util.AbstractMap.SimpleEntry<>(externalId, characteristicsMap));
					log(" ******* " + business + " ******* ");
					atributos = seleccionaLasDesas(business);
					if(atributos == null) {
						log("Problema consultando atributos, servidor probablemente tomó mucho en responder.");
						return null;
					}
					log("*** " + externalId + " ***");
					log("SAPObjectType: " + sapObjectType);
					log("Negocio: " + business);
					log("TemplateId: " + getTemplateFromData(data));
					lalista = ayMisHijosNormal(externalId);
					if(modif == 1) {
						filePrefix = "STEPM";
					}else if(modif == 0) {
						filePrefix = "P360A";
					}
					if(lalista.size() == 1) {
						if(!"MKP".equals(business) && "00".equals(sapObjectType)) {
							externalId = lalista.getFirst().getKey();
							log("External ID changed for variant since it is individual, now: " + externalId);
						}
					}else {
						if(lalista.isEmpty()) {
							log("Problema, la propuesta no tiene variantes.");
							return null;
						}
					}
					log("Prefix is: " + filePrefix);
					template = getTemplateFromData(data);
					String typeMainBarCode = null;
					if("00".equals(sapObjectType)) {
						if("MKP".equals(business)) {
							if(lineasH != null && !lineasH.isEmpty())
								lineasH.removeFirst();
							if(lalista.size() == 1) {
								typeMainBarCode = grabSimpleValue("TypeMainBarCode", lalista.getFirst().getValue());
							}
							aymisHijoS(externalId, template, lineasH, modif);
							if(lineasH.isEmpty()) {
								log("Not files found, returning... (" + modif + ", " + externalId + ", " + template + ")");
								return null;
							}
							getDFileLines(
									/* lalista.size() == 1 ? externalId : null */
									null, 
									business, 
									/* lalista.size() == 1 ? template : externalId */ 
									externalId, lalista, atributos, lineasD, true, modif);
						}else {
							if(!lalista.isEmpty()) {
								if("SBB".equals(business)) {
									typeMainBarCode = grabSimpleValue("NUMTP_S4H", lalista.getFirst().getValue());
									getDFileLinesMarcianoJana(business, template, externalId, lalista.getFirst(), atributos, lineasD);
								}else {
									if(modif == 1) {
										getDFileLinesMarcianoModif(business, template, externalId, lalista.getFirst(), atributos, lineasD);
									}else {
										getDFileLinesMarciano(business, template, externalId, lalista.getFirst(), atributos, lineasD);
									}
									typeMainBarCode = grabSimpleValue("TypeMainBarCode", lalista.getFirst().getValue());
								}
							} else {
								log("No estuvieron mis hijos");
							}
						}
					}else if("01".equals(sapObjectType)) {
						if(!"SBB".equals(business)) {
							if(modif == 1) {
								getDFileLinesModif(business, externalId, lalista, yep.getLast(), atributos, lineasD);
							}else {
								getDFileLines(null, business, template, yep, atributos, lineasD, false, modif);
								getDFileLines(null, business, externalId, lalista, atributos, lineasD, false, modif);
							}
						}else {
							getDFileLinesJana(business, externalId.substring(1), lalista, atributos, lineasD);
						}
					}
					if(typeMainBarCode == null) {
						if(!"SBB".equals(business)) {
							typeMainBarCode = grabSimpleValue("TypeMainBarCode", characteristicsMap);
						}else {
							typeMainBarCode = grabSimpleValue("NUMTP_S4H", characteristicsMap);
						}
					}
					linea = "SBB".equals(business) ? 
							getHFileLineJana(externalId, template, grabSimpleValue("MainBarCodeS4H", "00".equals(sapObjectType) ? lalista.getFirst().getValue() : characteristicsMap) , characteristicsMap, typeMainBarCode) : 
								modif == 1 ?
									getHFileLineModif(externalId, template, grabSimpleValue("MainBarCode", "00".equals(sapObjectType) ? lalista.getFirst().getValue() : characteristicsMap) , characteristicsMap, typeMainBarCode) :
									getHFileLine(externalId, template , grabSimpleValue("MainBarCode", "00".equals(sapObjectType) ? lalista.getFirst().getValue() : characteristicsMap), characteristicsMap, lalista.size(), typeMainBarCode);
					if(lineasH.isEmpty())
						lineasH.addLast(linea);
					log("SAPObjectType: " + sapObjectType);
					
					if("SBB".equals(business)) {

						lineasbb = getHFileLineJanaUOM(externalId, template , characteristicsMap);
						lineasUOM.addLast(lineasbb);

						getAttFileLinesJana(business, template, yep, atributos, lineasATT);

						sbH.append( latapa );
						sbH.append("\n");
						sbH.append(String.join("\n", lineasH));
						sbH.append("\nEOF|" + (lineasH.size() + 2));
						sbUOM.append( latapa );
						sbUOM.append("\n");
						sbUOM.append(String.join("\n", lineasUOM));
						sbUOM.append("\nEOF|" + (lineasUOM.size() + 2));
						sbATT.append( latapa );
						sbATT.append(lineasATT.isEmpty() ? "" :"\n");
						sbATT.append(String.join("\n", lineasATT));
						sbATT.append("\nEOF|" + (lineasATT.size() + 2));
						content[0] = sbH.toString();
						content[1] = latapa + "\n" + String.join("\n", lineasD) + "\nEOF|" + (lineasD.size() + 2);
						content[2] = sbUOM.toString();
						content[3] = sbATT.toString();
						content[4] = business;
						log("**** MD ****");
						log( content[0] );
						log("**** VAR ****");
						log( content[1] );
						log("**** UOM ****");
						log( content[2] );
						log("**** ATT ****");
						log( content[3] );
					}else {

						sbH.append( latapa );
						sbH.append("\n");
						sbH.append(String.join("\n", lineasH));
						sbH.append("\nEOF|" + (lineasH.size() + 2));

						content[0] = sbH.toString();
						content[1] = latapa + "\n" + String.join("\n", lineasD) + "\nEOF|" + (lineasD.size() + 2);
						content[2] = latapa + "\nEOF|2";
						if("MKP".equals(business)) {
							String a = null;
							StringBuilder sba = new StringBuilder();
							for(java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> ent : lalista) {
								a = lalinea(ent.getKey(), externalId);
								sba.append(a).append("\n");
							}
							content[3] = latapa + "\n" + sba.toString().trim() + "\nEOF|" + (2 + lalista.size());
						}else {
							content[3] = latapa + "\n" + lalinea(externalId, template) + "\nEOF|3";
						}
						content[4] = business;
						log( "*** H ***" );
						log( content[0] );
						log( "*** D ***" );
						log( content[1] );
						log( "***  ***" );
						log( content[2] );
						log( "***  ***" );
						log( content[3] );
						
						System.out.println(modif + " - " + business);
						System.out.println( "*** H ***" );
						System.out.println( content[0] );
						System.out.println( "*** D ***" );
						System.out.println( content[1] );
						System.out.println( "***  ***" );
						System.out.println( content[2] );
						System.out.println( "***  ***" );
						System.out.println( content[3] );
					}

				}
			}
		}else {
			log("-->" + workshop.getRawResponse() + "<--");
			logE(workshop.getException());
		}
		return content;
	}

	private String getHFileLineModif(String id, String parentId, String ean, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicMap, String typeMainBarCode) {
		return   id +"|"+
				 grabSimpleValue("SupplierID")+"|"+
				 grabSimpleValue("SKU", characteristicMap)+"|"+
				 parentId+"|"+
				 "|"+
				 "H|"+
				 grabSimpleValue("SAPObjectType")+"|"+
				 grabSimpleValue("ZNUMV")+"|"+
				 grabSimpleValue("SkuType")+"|"+
				 grabSimpleValue("ItemGroup")+"|"+
				 grabSimpleValue("Negocio")+"|"+
				 grabSimpleValue("Temporada")+"|"+
				 grabSimpleValue("AnoEstacion")+"|"+
				 grabSimpleValue("TImportacion")+"|"+
				 grabSimpleValue("BrandName")+"|"+
				 grabSimpleValue("LicenseDescription")+"|"+
				 grabSimpleValue("GradoDemoda")+"|"+
				 "UN|"+
				 grabSimpleValue("SAP_BEHVO")+"|"+
				 grabSimpleValue("WHERL")+"|"+
				 "|"+
				 "UN|"+
				 typeMainBarCode /* grabSimpleValue("TypeMainBarCode", characteristicMap) */ +"|"+
				 ean /* grabSimpleValue("MainBarCode", characteristicMap) */ +"|"+
				 grabSimpleValue("PerfilDeRedondeo")+"|"+
				 dqGeneralTreatment( grabSimpleValue("SupplierPartNumber") ) +"|"+
				 grabSimpleValue("ProductWidth")+"|"+
				 grabSimpleValue("ProductDepth")+"|"+
				 grabSimpleValue("ProductHeight")+"|"+
				 grabSimpleValue("VOLUMAtt")+"|"+
				 grabSimpleValue("PesoBruto")+"|"+
		         grabSimpleValue("ProductWeight")+"|"+
		         ( "".equals( grabSimpleValue("ProductDepth") ) || grabSimpleValue("ProductDepth") == null ? "" : grabSimpleValue("UnidadDeMedidaLongitud") ) + "|" +
				 ( "".equals( grabSimpleValue("VOLUMAtt") ) || grabSimpleValue("VOLUMAtt") == null ? "" : grabSimpleValue("UnidadDeMedidaVolumen") ) + "|" +
				 ( "".equals( grabSimpleValue("PesoBruto") ) || grabSimpleValue("PesoBruto") == null ? "" : grabSimpleValue("UnidadDeMedidaPeso") ) + "|" +
				 grabSimpleValue("CostoEnMonedaExtranjera")+"|"+
				 grabSimpleValue("FechaInicioVigenciaCostoImportacion")+"|"+
				 (!"".equals(grabSimpleValue("CostoEnMonedaExtranjera")) ? grabSimpleValue("Currency") : "" )+"|"+
				 grabSimpleValue("CostobrutoSinIVA")+"|"+
				 grabSimpleValue("PrecioSugeridocIVA")+"|"+
				 grabSimpleValue("FechaInicioVigenciaPrecioVenta")+"|"+
				 "MXP" +"|"+
				 grabSimpleValue("IndicadordeImpuesto")+"|"+
				 grabSimpleValue("Descuento1")+"|"+
				 grabSimpleValue("Descuento2")+"|"+
				 "|"+
				 "|"+
				 grabSimpleValue("CostoNetoSinIVA")+"|"+
				 grabSimpleValue("FechaInicioVigenciaCostoNeto")+"|"+
				 "|"+
			dqGeneralTreatment( grabSimpleValue("Name") ) +"|"+
			dqGeneralTreatment( grabSimpleValue("TextoAdicional") ) +"|"+
				 "|"+
				 "|"+
				 grabSimpleValue("MesdeEntregadeMercancIa")+"|"+
				 grabSimpleValue("Evento")+"|"+
				 "|"+
				 grabSimpleValue("ProductTypeSAP")+"|"+
				 grabSimpleValue("Coleccion")+"|"+
				 grabSimpleValue("Armado")+"|"+
				 grabSimpleValue("MesdeEntregadeMuestra")+"|"+
				 "|"+
				 grabSimpleValue("Status")+"|"+
				 grabSimpleValue("ZBRECJ")+"|"+
				 grabSimpleValue("ZLAECJ")+"|"+
				 grabSimpleValue("ZHOECJ")+"|"+
				 ( "".equals( grabSimpleValue("ZHOECJ") ) || grabSimpleValue("ZHOECJ") == null ? "" : grabSimpleValue("UnidadDeMedidaLongitud") ) + "|" +
				 grabSimpleValue("ZVOLCJ")+"|"+
				 ( "".equals( grabSimpleValue("ZVOLCJ") ) || grabSimpleValue("ZVOLCJ") == null ? "" : grabSimpleValue("UnidadDeMedidaVolumen") ) + "|" +
				 grabSimpleValue("ZBRGCJ")+"|"+
				 grabSimpleValue("ZNTGCJ")+"|"+
				 ( "".equals( grabSimpleValue("ZBRGCJ") ) || grabSimpleValue("ZBRGCJ") == null ? "" : grabSimpleValue("UnidadDeMedidaPeso") ) + "|" +
				 grabSimpleValue("HNDLCODE")+"|"+
				 grabSimpleValue("WHSTC")+"|"+
				 grabSimpleValue("MVGR5")+"|"+
				 "|"+
				 grabSimpleValue("TipoDeEtiqueta")+"|"+
				 ( grabSimpleValue("ZHOEPQ") == null || "".equals(grabSimpleValue("ZHOEPQ")) ? "" : new java.math.BigDecimal(grabSimpleValue("ZHOEPQ")).intValue() )+"|"+
				 ( grabSimpleValue("ZBREPQ") == null || "".equals(grabSimpleValue("ZBREPQ")) ? "" : new java.math.BigDecimal(grabSimpleValue("ZBREPQ")).intValue() )+"|"+
				 ( grabSimpleValue("ZLAEPQ") == null || "".equals(grabSimpleValue("ZLAEPQ")) ? "" : new java.math.BigDecimal(grabSimpleValue("ZLAEPQ")).intValue() )+"|"+
				 ( "".equals( grabSimpleValue("ZLAEPQ") ) || grabSimpleValue("ZLAEPQ") == null ? "" : grabSimpleValue("UnidadDeMedidaLongitud") ) + "|" +
				 ( grabSimpleValue("ZVOLPQ") == null || "".equals(grabSimpleValue("ZVOLPQ")) ? "" : new java.math.BigDecimal(grabSimpleValue("ZVOLPQ")).intValue() ) +"|"+
				 ( "".equals( grabSimpleValue("ZVOLPQ") ) || grabSimpleValue("ZVOLPQ") == null ? "" : grabSimpleValue("UnidadDeMedidaVolumen") ) + "|" +
				 ( grabSimpleValue("ZBRGPQ") == null || "".equals( grabSimpleValue("ZBRGPQ"))? "" : new java.math.BigDecimal(grabSimpleValue("ZBRGPQ")).intValue() )+"|"+
				 ( grabSimpleValue("ZNTGPQ") == null || "".equals(grabSimpleValue("ZNTGPQ")) ? "" : new java.math.BigDecimal(grabSimpleValue("ZNTGPQ")).intValue() )+"|"+
				 ( "".equals( grabSimpleValue("ZBRGPQ") ) || grabSimpleValue("ZBRGPQ") == null ? "" : grabSimpleValue("UnidadDeMedidaPeso") ) + "|" +
				 grabSimpleValue("MAX_STACK")
				 ;
	}

	private void getDFileLinesMarcianoModif(String business, String parentId, String id, java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> entry, java.util.Map<String, String> lasDesas, java.util.LinkedList<String> lineasD) throws ServiceUnavailableException {
		int counter = 1;
		String elese = null;
		String typeMainBarCode = null;
		String elcampoLatalla = null;
		String latalla = null;
		String localCode = null;
		latalla = grabSimpleValue("TamanoUnico", entry.getValue(), true);
		String itemGroup = null; // grabSimpleValue("ItemGroup");
		itemGroup = grabSimpleValue(!"Suburbia".equals(business) ? "ItemGroup" : "ItemGrouopS4H");
		if(itemGroup == null || "".equals(itemGroup)) {
			//PANIC
		}
		log("(Mars 1.1) El campo la talla: " + itemGroup + " (latalla: " + latalla + ")");
		typeMainBarCode = grabSimpleValue("TypeMainBarCode", entry.getValue());
		if(typeMainBarCode == null || "".equals(typeMainBarCode)) {
			typeMainBarCode = grabSimpleValue("TypeMainBarCode");
		}
		if(latalla != null && !"".equals(latalla)) {
			elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
			log("El campoLatalla: " + elcampoLatalla);
			if(elcampoLatalla != null) {
				elcampoLatalla = lasDesas.get(elcampoLatalla);
			}
			localCode = getCodeLatalla(latalla, elcampoLatalla);
			log("Getting local code for latalla: " + localCode + " (campoLatalla: " + elcampoLatalla + ").");
			log("Getting local code for latalla: " + localCode + " (campoLatalla: " + elcampoLatalla + ").");
			log("(Mars) El campo la talla: " + elcampoLatalla + " (" + itemGroup + ")");
			if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
				elcampoLatalla = ""; // "TMU01";
			}
			String p1 = grabSimpleValue("PrecioSugeridocIVA", entry.getValue());
			String a1 = grabSimpleValue("CostoEnMonedaExtranjera", entry.getValue());
			String a2 = grabSimpleValue("CostoNetoSinIVA", entry.getValue());
			String a3 = grabSimpleValue("CostobrutoSinIVA", entry.getValue());
			String z1 = grabSimpleValue("FechaInicioVigenciaPrecioVenta", entry.getValue());
			String z2 = grabSimpleValue("FechaInicioVigenciaCostoImportacion", entry.getValue());
			String z3 = grabSimpleValue("FechaInicioVigenciaCostoNeto", entry.getValue());
			lineasD.addLast(
				  id + "|"
				+ grabSimpleValue("SupplierID") + "|"
				+ grabSimpleValue("SKU", entry.getValue()) + "|"
				+ parentId + "|"
				+ ""  + "|"
				+ "D" + "|"
				+ "00" + "|" // grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
				+ "" + "|" // ("00".equals(grabSimpleValue("SAPObjectType", entry.getValue())) ? "" : paddZeros(4, grabSimpleValue("ZNUMV") ) ) + "|"
				+ typeMainBarCode + "|"
				+ "" + "|"
				+ aSeisPosiciones(counter) + "|"
				+ elcampoLatalla + "|"
				+ paddZeros(4, localCode) + "|"
				+ "" + "|"
				+ ("".equals(p1) ? grabSimpleValue("PrecioSugeridocIVA") : p1 ) + "|"
				+ ("".equals(z1) ? grabSimpleValue("FechaInicioVigenciaPrecioVenta") : z1) + "|"
				+ ("".equals(a1) ? grabSimpleValue("CostoEnMonedaExtranjera") : a1) + "|"
				+ ("".equals(z2) ? grabSimpleValue("FechaInicioVigenciaCostoImportacion") : z2) + "|"
				+ ("".equals(a2) ? grabSimpleValue("CostoNetoSinIVA") : a2) + "|"
				+ ("".equals(z3) ? grabSimpleValue("FechaInicioVigenciaCostoNeto") : z3) + "|"
				+ grabSimpleValue("MainBarCode", entry.getValue()) + "|"
				+ dqGeneralTreatment( grabSimpleValue("TextoAdicional") ) + "|"
				+ ("".equals(a3) ? grabSimpleValue("CostobrutoSinIVA") : a3) + "|"
				+ grabSimpleValue("Status") + "|"
				+ grabSimpleValue("TipoDeEtiqueta") + "|"
				+ dqGeneralTreatment( grabSimpleValue("SupplierPartNumber", entry.getValue()) )
			);
			counter++;
		}
		for(java.util.Map.Entry<String, String> ladesa : lasDesas.entrySet()) {
			elese = grabSimpleValue(ladesa.getKey());
			if(elese == null || "".equals(elese)) {
				elese = grabSimpleValue(ladesa.getKey(), entry.getValue());
			}
			if(elese != null && !"".equals(elese)) {
				String p1 = grabSimpleValue("PrecioSugeridocIVA", entry.getValue());
				String a1 = grabSimpleValue("CostoEnMonedaExtranjera", entry.getValue());
				String a2 = grabSimpleValue("CostoNetoSinIVA", entry.getValue());
				String a3 = grabSimpleValue("CostobrutoSinIVA", entry.getValue());
				String z1 = grabSimpleValue("FechaInicioVigenciaPrecioVenta", entry.getValue());
				String z2 = grabSimpleValue("FechaInicioVigenciaCostoImportacion", entry.getValue());
				String z3 = grabSimpleValue("FechaInicioVigenciaCostoNeto", entry.getValue());
				lineasD.addLast(
					  id + "|"
					+ grabSimpleValue("SupplierID") + "|"
					+ grabSimpleValue("SKU", entry.getValue()) + "|"
					+ parentId + "|"
					+ ""  + "|"
					+ "D" + "|"
					+ grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
					+ ("00".equals(grabSimpleValue("SAPObjectType",entry.getValue())) ? "" : paddZeros(4, grabSimpleValue("ZNUMV") ) ) + "|"
					+ typeMainBarCode + "|"
					+ "" + "|"
					+ aSeisPosiciones(counter) + "|"
					+ ladesa.getValue() + "|"
					+ elese + "|"
					+ "" + "|"
					+ ("".equals(p1) ? grabSimpleValue("PrecioSugeridocIVA") : p1 ) + "|"
					+ ("".equals(z1) ? grabSimpleValue("FechaInicioVigenciaPrecioVenta") : z2) + "|"
					+ ("".equals(a1) ? grabSimpleValue("CostoEnMonedaExtranjera") : a1) + "|"
					+ ("".equals(z2) ? grabSimpleValue("FechaInicioVigenciaCostoImportacion") : z2) + "|"
					+ ("".equals(a2) ? grabSimpleValue("CostoNetoSinIVA") : a2) + "|"
					+ ("".equals(z3) ? grabSimpleValue("FechaInicioVigenciaCostoNeto") : z3) + "|"
					+ grabSimpleValue("MainBarCode", entry.getValue()) + "|"
					+ dqGeneralTreatment( grabSimpleValue("TextoAdicional") ) + "|"
					+ ("".equals(a3) ? grabSimpleValue("CostobrutoSinIVA") : a3) + "|"
					+ grabSimpleValue("Status") + "|"
					+ grabSimpleValue("TipoDeEtiqueta") + "|"
					+ dqGeneralTreatment( grabSimpleValue("SupplierPartNumber", entry.getValue()) )
				);
				counter++;
			}
		}
		counter = 1;

	}

	private String queryColor(String key, String dictionary) {
		String[] response = new String[1];
		response[0] = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				  "StandardizationValue.Value"
				+ ",StandardizationValue.AlternativeValue"
			);
		qp.put("query", 
				"StandardizationValue.Dictionary->StandardizationDictionary.Identifier = \"" + dictionary + "\""
				+ " and "
				+ "StandardizationValue.Value equalsIC \"" + key + "\"");
		qp.put("dictionaryProxy", "'" + dictionary + "'");
		rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
			response[0] = row.getJSONArray("values").getString(1);
		}, this::log);
		return response[0];
	}

	private void getDFileLinesModif(
			String business,
			String parentId,
			java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> lalista, java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> elpadrino,
			java.util.Map<String, String> lasDesas,
			java.util.LinkedList<String> lineasD) throws ServiceUnavailableException {
		int counter = 1;
		String elese = null;
		String elcampoLatalla = null;
		String itemGroup = null;
		String latalla = null;
		String localCode = null;
		itemGroup = grabSimpleValue(!"SBB".equals(business) ? "ItemGroup" : "ItemGrouopS4H");
		if(itemGroup == null || "".equals(itemGroup)) {
			//PANIC
		}
		elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
		log("(Normal) El campo la talla: " + elcampoLatalla + " (" + itemGroup + ", " + business + ")");
		if(elcampoLatalla != null) {
			log(elcampoLatalla + " - " + lasDesas.get(elcampoLatalla));
			elcampoLatalla = lasDesas.get(elcampoLatalla);
		}
		if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
			elcampoLatalla = ""; // "TMU01";
		}
		for(java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> entry : lalista ) {
			for(java.util.Map.Entry<String, String> ladesa : lasDesas.entrySet()) {
				elese = grabSimpleValue(ladesa.getKey(), entry.getValue());
				if(elese != null && !"".equals(elese)) {
					String p1 = grabSimpleValue("PrecioSugeridocIVA", entry.getValue());
					String a1 = grabSimpleValue("CostoEnMonedaExtranjera", entry.getValue());
					String a2 = grabSimpleValue("CostoNetoSinIVA", entry.getValue());
					String a3 = grabSimpleValue("CostobrutoSinIVA", entry.getValue());
					String z1 = grabSimpleValue("FechaInicioVigenciaPrecioVenta", entry.getValue());
					String z2 = grabSimpleValue("FechaInicioVigenciaCostoImportacion", entry.getValue());
					String z3 = grabSimpleValue("FechaInicioVigenciaCostoNeto", entry.getValue());
					lineasD.addLast(
						  entry.getKey() + "|"
						+ grabSimpleValue("SupplierID") + "|"
						+ grabSimpleValue("SKU", entry.getValue()) + "|"
						+ parentId + "|"
						+ ""  + "|"
						+ "D" + "|"
						+ grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
						+ ("00".equals(grabSimpleValue("SAPObjectType",entry.getValue())) ? "" : paddZeros(4, grabSimpleValue("ZNUMV") ) ) + "|"
						+ grabSimpleValue("TypeMainBarCode", entry.getValue()) + "|"
						+ "" + "|"
						+ aSeisPosiciones(counter) + "|"
						+ ladesa.getValue() + "|"
						+ elese + "|"
						+ "" + "|"
						+ ("".equals(p1) ? grabSimpleValue("PrecioSugeridocIVA") : p1) + "|"
						+ ("".equals(z1) ? grabSimpleValue("FechaInicioVigenciaPrecioVenta") : z1) + "|"
						+ ("".equals(a1) ? grabSimpleValue("CostoEnMonedaExtranjera") : a1) + "|"
						+ ("".equals(z2) ? grabSimpleValue("FechaInicioVigenciaCostoImportacion") : z2) + "|"
						+ ("".equals(a2) ? grabSimpleValue("CostoNetoSinIVA") : a2) + "|"
						+ ("".equals(z3) ? grabSimpleValue("FechaInicioVigenciaCostoNeto") : z3) + "|"
						+ grabSimpleValue("MainBarCode", entry.getValue()) + "|"
						+ dqGeneralTreatment( grabSimpleValue("TextoAdicional") ) + "|"
						+ ("".equals(a3) ? grabSimpleValue("CostobrutoSinIVA") : a3) + "|"
						+ grabSimpleValue("Status") + "|"
						+ grabSimpleValue("TipoDeEtiqueta") + "|"
						+ dqGeneralTreatment( grabSimpleValue("SupplierPartNumber", entry.getValue()) )
					);
					counter++;
				}
			}
			latalla = grabSimpleValue("TamanoUnico", entry.getValue(), true);
			if(latalla != null && !"".equals(latalla)) {
				elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
				if(elcampoLatalla != null) {
					log(elcampoLatalla + " - " + lasDesas);
					elcampoLatalla = lasDesas.get(elcampoLatalla);
				}
				if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
					elcampoLatalla = ""; // "TMU01";
			 	}
				localCode = getCodeLatalla(latalla, elcampoLatalla);
				log("Getting local code for latalla: " + localCode + ".");
				String p1 = grabSimpleValue("PrecioSugeridocIVA", entry.getValue());
				String a1 = grabSimpleValue("CostoEnMonedaExtranjera", entry.getValue());
				String a2 = grabSimpleValue("CostoNetoSinIVA", entry.getValue());
				String a3 = grabSimpleValue("CostobrutoSinIVA", entry.getValue());
				String z1 = grabSimpleValue("FechaInicioVigenciaPrecioVenta", entry.getValue());
				String z2 = grabSimpleValue("FechaInicioVigenciaCostoImportacion", entry.getValue());
				String z3 = grabSimpleValue("FechaInicioVigenciaCostoNeto", entry.getValue());
				lineasD.addLast(
					  entry.getKey() + "|"
					+ grabSimpleValue("SupplierID") + "|"
					+ grabSimpleValue("SKU", entry.getValue()) + "|"
					+ parentId + "|"
					+ ""  + "|"
					+ "D" + "|"
					+ grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
					+ ("00".equals(grabSimpleValue("SAPObjectType")) ? "" : paddZeros(4, grabSimpleValue("ZNUMV") ) ) + "|"
					+ grabSimpleValue("TypeMainBarCode", entry.getValue()) + "|"
					+ "" + "|"
					+ aSeisPosiciones(counter) + "|"
					+ elcampoLatalla + "|"
					+ paddZeros(4, localCode) + "|"
					+ "" + "|"
					+ ("".equals(p1) ? grabSimpleValue("PrecioSugeridocIVA") : p1 ) + "|"
					+ ("".equals(z1) ? grabSimpleValue("FechaInicioVigenciaPrecioVenta") : z1) + "|"
					+ ("".equals(a1) ? grabSimpleValue("CostoEnMonedaExtranjera") : a1) + "|"
					+ ("".equals(z2) ? grabSimpleValue("FechaInicioVigenciaCostoImportacion") : z2) + "|"
					+ ("".equals(a2) ? grabSimpleValue("CostoNetoSinIVA") : a2) + "|"
					+ ("".equals(z3) ? grabSimpleValue("FechaInicioVigenciaCostoNeto") : z3) + "|"
					+ grabSimpleValue("MainBarCode", entry.getValue()) + "|"
					+ grabSimpleValue("TextoAdicional") + "|"
					+ ("".equals(a3) ? grabSimpleValue("CostobrutoSinIVA") : a3) + "|"
					+ grabSimpleValue("Status") + "|"
					+ grabSimpleValue("TipoDeEtiqueta") + "|"
					+ grabSimpleValue("SupplierPartNumber", entry.getValue())
				);
				counter++;
			}
			if(elpadrino != null) {
				for(java.util.Map.Entry<String, String> ladesa : lasDesas.entrySet()) {
					elese = grabSimpleValue(ladesa.getKey(), elpadrino.getValue());
					if(elese != null && !"".equals(elese)) {
						String p1 = grabSimpleValue("PrecioSugeridocIVA", entry.getValue());
						String a1 = grabSimpleValue("CostoEnMonedaExtranjera", entry.getValue());
						String a2 = grabSimpleValue("CostoNetoSinIVA", entry.getValue());
						String a3 = grabSimpleValue("CostobrutoSinIVA", entry.getValue());
						String z1 = grabSimpleValue("FechaInicioVigenciaPrecioVenta", entry.getValue());
						String z2 = grabSimpleValue("FechaInicioVigenciaCostoImportacion", entry.getValue());
						String z3 = grabSimpleValue("FechaInicioVigenciaCostoNeto", entry.getValue());
						lineasD.addLast(
							  entry.getKey() + "|"
							+ grabSimpleValue("SupplierID") + "|"
							+ grabSimpleValue("SKU", entry.getValue()) + "|"
							+ parentId + "|"
							+ ""  + "|"
							+ "D" + "|"
							+ grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
							+ ("00".equals(grabSimpleValue("SAPObjectType",entry.getValue())) ? "" : paddZeros(4, grabSimpleValue("ZNUMV") ) ) + "|"
							+ grabSimpleValue("TypeMainBarCode", entry.getValue()) + "|"
							+ "" + "|"
							+ aSeisPosiciones(counter) + "|"
							+ ladesa.getValue() + "|"
							+ elese + "|"
							+ "" + "|"
							+ ("".equals(p1) ? grabSimpleValue("PrecioSugeridocIVA") : p1) + "|"
							+ ("".equals(z1) ? grabSimpleValue("FechaInicioVigenciaPrecioVenta") : z1) + "|"
							+ ("".equals(a1) ? grabSimpleValue("CostoEnMonedaExtranjera") : a1) + "|"
							+ ("".equals(z2) ? grabSimpleValue("FechaInicioVigenciaCostoImportacion") : z2) + "|"
							+ ("".equals(a2) ? grabSimpleValue("CostoNetoSinIVA") : a2) + "|"
							+ ("".equals(z3) ? grabSimpleValue("FechaInicioVigenciaCostoNeto") : z3) + "|"
							+ grabSimpleValue("MainBarCode", entry.getValue()) + "|"
							+ grabSimpleValue("TextoAdicional") + "|"
							+ ("".equals(a3) ? grabSimpleValue("CostobrutoSinIVA") : a3) + "|"
							+ grabSimpleValue("Status") + "|"
							+ grabSimpleValue("TipoDeEtiqueta") + "|"
							+ grabSimpleValue("SupplierPartNumber", entry.getValue())
						);
						counter++;
					}
				}
			}
			counter = 1;
		}
	}


	private String lalinea(String id, String parentId) {
//		ID||'|'||SupplierID||'|'||SKU||'|'||PARENT_ID||'||T|'||ArgumentoDeVenta
		return id + "|" + grabSimpleValue("SupplierID") + "|" + grabSimpleValue("SKU") + "|" + parentId + "||T|" + grabSimpleValue("ArgumentoDeVenta");
	}

	private String getHFileLineJanaUOM(String id, String parentId, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicMap) {
		return  id.substring(1) +"|"+
				"0007|"+
				grabSimpleValue("SupplierPartNumber", characteristicMap)+"|"+
				"PI|"+
				"1|"+
				"1|"+
				grabSimpleValue("ProductDepth", characteristicMap)+"|"+
				grabSimpleValue("ProductWidth", characteristicMap)+"|"+
				grabSimpleValue("ProductHeight", characteristicMap)+"|"+
				( "".equals(grabSimpleValue("ProductHeight")) || grabSimpleValue("ProductHeight") == null ? "" : grabSimpleValue("UnidadDeMedidaLongitud", characteristicMap) ) +"|"+
				grabSimpleValue("VOLUMAtt", characteristicMap)+"|"+
				( "".equals(grabSimpleValue("VOLUMAtt")) || grabSimpleValue("VOLUMAtt") == null ? "" : grabSimpleValue("UnidadDeMedidaVolumen", characteristicMap) ) +"|"+
				grabSimpleValue("PesoBruto", characteristicMap)+"|"+
				grabSimpleValue("ProductWeight", characteristicMap)+"|"+
				( (!"".equals(grabSimpleValue("PesoBruto", characteristicMap)) && grabSimpleValue("PesoBruto", characteristicMap) != null) || ( grabSimpleValue("ProductWeight", characteristicMap) != null && !"".equals(grabSimpleValue("ProductWeight", characteristicMap))) ? grabSimpleValue("UnidadDeMedidaPeso", characteristicMap) : "" ) 
				;
	}

	private void buildCharacteristicsMap(org.json.JSONArray characteristicRecords, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicsMap) {
		if(!characteristicsMap.isEmpty()) {
			characteristicsMap.clear();
		}
		org.json.JSONObject characteristicRecord = null;
		String characteristicIdentifier = null;
		java.util.LinkedList<org.json.JSONObject> objectList = null;
		for(int i=0; i<characteristicRecords.length(); i++) {
			characteristicRecord = characteristicRecords.getJSONObject(i);
			characteristicIdentifier = characteristicRecord.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			objectList = characteristicsMap.get(characteristicIdentifier);
			if(objectList == null) {
				objectList = new java.util.LinkedList<>();
				characteristicsMap.put(characteristicIdentifier, objectList);
			}
			objectList.addLast(characteristicRecord);
		}
	}

	public void aymisHijoS(String proposalId, String template, java.util.LinkedList<String> lineasH, short modif) {
		java.util.LinkedList<java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> lalista = new java.util.LinkedList<>();
		java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> elmapa = null;
		String linea = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Article.SupplierAID");
		qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + proposalId + "\") = \"" + proposalId + "\"");
		qp.put("pageSize", "1200");
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		String variantId = null;
		String sku = null;
		String typeMainBarCode = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			qp.clear();
			qp.put("entityFilter", "ArticleCharacteristicValue");
			qp.put("includeLabels", "true");
			qp.put("includeIds", "true");
			for(int i=0; i<rows.length(); i++) {
				values = rows.getJSONObject(i).getJSONArray("values");
				variantId = values.getString(0);
				response = workshop.makeRequest("GET", "/object/Article/'" + variantId + "'@'MASTER'", qp, null);
				if(response.has("_data") && response.getJSONObject("_data").has("_characteristicRecords")) {
					elmapa = new java.util.TreeMap<>();
					buildCharacteristicsMap(response.getJSONObject("_data").getJSONArray("_characteristicRecords"), elmapa);
					sku = grabSimpleValue("SKU", elmapa);
					typeMainBarCode = grabSimpleValue("TypeMainBarCode", elmapa);
					log(variantId + ", " + sku + ", " + modif);
					if(modif == 0 && (sku == null || "".equals(sku))) {
						lalista.addLast(elmapa);
						linea = getHFileLine( /* rows.length() == 1 ? proposalId : variantId */ variantId, /* rows.length() == 1 ? template : proposalId */ proposalId, grabSimpleValue("MainBarCode", elmapa), elmapa, null, typeMainBarCode);
						lineasH.addLast(linea);
					}else if(modif == 1 && sku != null && !"".equals(sku)) {
						lalista.addLast(elmapa);
						linea = getHFileLineModif(/* rows.length() == 1 ? proposalId : variantId */ variantId, /* rows.length() == 1 ? template : proposalId */ proposalId, grabSimpleValue("MainBarCode", elmapa), elmapa, typeMainBarCode);
						lineasH.addLast(linea);
					}
				}
			}
		}else {
			log("PANIC: " + workshop.getRawResponse());
		}
	}

	public java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> ayMisHijosNormal(String proposalId) throws ServiceUnavailableException{
		java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> lalista = new java.util.LinkedList<>();
		java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> elmapa = null;
		int a = 0;
		int b = 0;
		do {
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			qp.put("fields", "Article.SupplierAID");
			qp.put("query", "ProductReference.ReferencedSupplierAid(\"" + proposalId + "\") = \"" + proposalId + "\"");
			qp.put("pageSize", "1200");
			org.json.JSONObject response = workshop.makeRequest("GET", "/list/Article/bySearch", qp, null);
			org.json.JSONArray rows = null;
			org.json.JSONArray values = null;
			String variantId = null;
			if(response != null) {
				rows = response.getJSONArray("rows");
				b = response.getInt("totalSize");
				a += response.getInt("pageSize");
				qp.clear();
				qp.put("entityFilter", "ArticleCharacteristicValue");
				qp.put("includeLabels", "true");
				qp.put("includeIds", "true");
				qp.put("pageSize", "1200");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					variantId = values.getString(0);
					response = workshop.makeRequest("GET", "/object/Article/'" + variantId + "'@'MASTER'?includeLabels=true");
					if(response != null && response.has("_data") && response.getJSONObject("_data").has("_characteristicRecords")) {
						elmapa = new java.util.TreeMap<>();
						buildCharacteristicsMap(response.getJSONObject("_data").getJSONArray("_characteristicRecords"), elmapa);
						lalista.addLast(new java.util.AbstractMap.SimpleEntry<>(variantId, elmapa));
					}else {
						log("Problem getting article via object api --->" + workshop.getRawResponse());
					}
				}
			}else {
				log("### ERR: " + workshop.getRawResponse());
			}
		}while(a < b);
		a = 0;
		return lalista;
	}

	private void getAttFileLinesJana(String business, String parentId, java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> lalista, java.util.Map<String, String> lasDesas, java.util.LinkedList<String> lineasD) {
		int counter = 1;
		String elese = null;
		for(java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> entry : lalista ) {
			for(java.util.Map.Entry<String, String> ladesa : lasDesas.entrySet()) {
				elese = grabSimpleValue(ladesa.getKey(), entry.getValue());
				if(elese != null && !"".equals(elese)) {
					lineasD.addLast(
						  entry.getKey() + "|"
						+ grabSimpleValue("SKU") + "|"
						+ aSeisPosiciones(counter) + "|"
						+ ladesa.getValue() + "|"
						+ elese
					);
					counter++;
				}
			}
			counter = 1;
		}
		log("<:::::::>" + lineasD.size());
	}

	private String getCodeLatalla(String value, String campoLatalla) {
		log("Looking for: " + value + " in: " + campoLatalla + "LOV");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code");
		qp.put("query", "LookupValueLang.Name(es) equals \"" + value.replaceAll("\"", "\\\\\"") + "\"");
		qp.put("lookup", "'" + campoLatalla + "LOV'");
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		org.json.JSONArray rows = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			return rows.length() > 0 ? rows.getJSONObject(0).getJSONArray("values").getString(0) : null;
		}else {
			log("### ERR: " + workshop.getRawResponse());
		}
		return null;
	}

//	private String getSAPFieldECC(String field) {
//		log("Looking for: " + field + " in: Characteristics");
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("fields", "LookupValueIdentifier.Code(ECC)");
//		qp.put("query", "LookupValueLang.Name(es) equals \"" + field + "\"");
//		qp.put("lookup", "'Characteristics'");
//		org.json.JSONObject response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		if(response != null) {
//			rows = response.getJSONArray("rows");
//			return rows.length() > 0 ? rows.getJSONObject(0).getJSONArray("values").getString(0) : null;
//		}else {
//			log("### ERR: " + workshop.getRawResponse());
//		}
//		return null;
//	}
	
	private String getCodeFromNameEs(String name, String lookup) {
		RESTWrapper rw = new RESTWrapper();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code");
		qp.put("query", "LookupValueLang.Name(es) = \"" + name + "\"");
		qp.put("lookup", "'" + lookup + "'");
		final String[] data = new String[1];
		data[0] = null;
		rw.collectData("list", "LookupValue", null, "bySearch", qp, row -> data[0] = row.getJSONArray("values").getString(0), this::log);
		return data[0];
	}

	private void getDFileLinesJana(String business, String parentId, java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> lalista, java.util.Map<String, String> lasDesas, java.util.LinkedList<String> lineasD) throws ServiceUnavailableException {
		String elcampoLatalla = null;
		String itemGroup = null;
		String latalla = null;
		String color = null;
		String colorSb = null;
		String colorLv = null;
		String localCode = null;
		itemGroup = grabSimpleValue("SBB".equals(business) ? "ItemGroupS4H" : "ItemGroup");
		if(itemGroup == null || "".equals(itemGroup)) {
		}
		elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
		log("Asking for latalla: " + itemGroup + " on " + business + ": " + elcampoLatalla);
		if(elcampoLatalla != null) {
			log(elcampoLatalla + " - " + lasDesas.get(elcampoLatalla));
		}
		if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
			elcampoLatalla = ""; // "TMU01";
		}
		java.util.Map<String, String> success = new java.util.TreeMap<>();
		for(java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> entry : lalista ) {
			latalla = grabSimpleValue("TamanoUnico", entry.getValue(), true);
			localCode = getCodeLatalla(latalla, elcampoLatalla);
			log("Getting local code for latalla: " + localCode + ".");
			colorSb = grabSimpleValue("SB_COLORES", entry.getValue());
			colorLv = grabSimpleValue("ColoursLiverpoolAtt", entry.getValue(), true);
			log("SB_COLORES: " + colorSb + ", C100: " + colorLv);
			if(colorSb == null || "".equals(colorSb)) {
				if(colorLv != null && !"".equals(colorLv)) {
					color = success.get(colorLv);
					log("A known colorLv ? " + (color != null));
					if(color == null) {
						String colorSuburbia = queryColor(colorLv, "ExtensionDeMetadatos_RelacionColoresLiverpoolSuburbia");
						log("Searching " + color + " in SB_COLORES, got: " + colorSuburbia);
						if(colorSuburbia != null && !"".equals(colorSuburbia)) {
							color = getCodeFromNameEs(colorSuburbia, "SB_COLORESLOV");
							success.put(colorLv, color);
						}else {
							color = colorSb;
						}
					}
				}else {
					color = colorSb;
				}
			}else {
				color = colorSb;
			}
			
//			color = color == null || "".equals(color) ? grabSimpleValue("ColoursLiverpoolAtt", entry.getValue()) : color;

			elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
			if(elcampoLatalla != null) {
				log(elcampoLatalla + " - " + lasDesas);
			}
			if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
				elcampoLatalla = ""; //"TMU01";
			}else {
			}
			log("Poniendo: " + localCode + " en: " + elcampoLatalla);
			log("--->" + entry.getKey());
			String p1 = grabSimpleValue("PrecioSugeridocIVA", entry.getValue());
			lineasD.addLast(
					grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
					+ entry.getKey().substring(1) + "|"
					+ parentId + "|"//ID step
					+ "0007|"
					+ "|"
					+ trimZeros( dqGeneralTreatment( grabSimpleValue("SupplierPartNumber") ) ) + "|"
					+ ( "".equals(grabSimpleValue("SupplierPartNumber", entry.getValue())) ? grabSimpleValue("SupplierPartNumber") : grabSimpleValue("SupplierPartNumber", entry.getValue()) ) + "|"
					+ color + "|"
					+ localCode + "|"
					+ grabSimpleValue("CostoNetoSinIVA") + "|"
					+ grabSimpleValue("Currency") + "|"
					+ ("".equals(p1) ? grabSimpleValue("PrecioSugeridocIVA") : p1) + "|"
					+ grabSimpleValue("SKU", entry.getValue()) + "|"
					+ grabSimpleValue("MainBarCodeS4H", entry.getValue()) + "|"
					+ grabSimpleValue("NUMTP_S4H", entry.getValue()) + "|"
					+ trimZeros( dqGeneralTreatment( ( "".equals(grabSimpleValue("SupplierPartNumber", entry.getValue())) ? grabSimpleValue("SupplierPartNumber") : grabSimpleValue("SupplierPartNumber", entry.getValue()) ) ) )
				);
			// ("00".equals(grabSimpleValue("SAPObjectType", entry.getValue())) ? ("".equals( grabSimpleValue("SupplierPartNumber", entry.getValue())) ? grabSimpleValue("SupplierPartNumber") : grabSimpleValue("SupplierPartNumber", entry.getValue()) ) : grabSimpleValue("SupplierPartNumber", entry.getValue()) )
		}
	}

	private void getDFileLinesMarcianoJana(String business, String parentId, String id, java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> entry, java.util.Map<String, String> lasDesas, java.util.LinkedList<String> lineasD) throws ServiceUnavailableException {
		String elcampoLatalla = null;
		String itemGroup = null;
		String latalla = null;
		String localCode = null;
		String colorLv = null;
		String colorSb = null;
		String color = null;
		java.util.Map<String, String> success = new java.util.TreeMap<>();
		itemGroup = grabSimpleValue("SBB".equals(business) ? "ItemGroupS4H" : "ItemGroup");
		if(itemGroup == null || "".equals(itemGroup)) {
		}
		elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
		log("Asking for latalla: " + itemGroup + " on " + business + ": " + elcampoLatalla);
		if(elcampoLatalla != null) {
			log(elcampoLatalla + " - " + lasDesas.get(elcampoLatalla));
		}
		if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
			elcampoLatalla = ""; // "TMU01";
		}
		latalla = grabSimpleValue("TamanoUnico", entry.getValue(), true);
		colorSb = grabSimpleValue("SB_COLORES", entry.getValue());
		colorLv = grabSimpleValue("ColoursLiverpoolAtt", entry.getValue(), true);
		log("SB_COLORES: " + colorSb + ", C100: " + colorLv);
		if(colorSb == null || "".equals(colorSb)) {
			if(colorLv != null && !"".equals(colorLv)) {
				color = success.get(colorLv);
				log("A known colorLv ? " + (color != null));
				if(color == null) {
					String colorSuburbia = queryColor(colorLv, "ExtensionDeMetadatos_RelacionColoresLiverpoolSuburbia");
					if(colorSuburbia != null && !"".equals(colorSuburbia)) {
						color = getCodeFromNameEs(colorSuburbia, "SB_COLORESLOV");
						success.put(colorLv, color);
					}else {
						color = colorSb;
					}
				}
			}else {
				color = colorSb;
			}
		}else {
			color = colorSb;
		}

		elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
		if(elcampoLatalla != null) {
			log(elcampoLatalla + " - " + lasDesas);
		}
		if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
			elcampoLatalla = ""; // "TMU01";
		}else {
		}
		localCode = getCodeLatalla(latalla, elcampoLatalla);
		log("Getting local code for latalla: " + localCode + ".");
		String p1 = grabSimpleValue("PrecioSugeridocIVA", entry.getValue());
		String a1 = grabSimpleValue("CostoNetoSinIVA", entry.getValue());
		lineasD.addLast(
				grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
				+ id.substring(1) + "|"
				+ parentId + "|"//ID step
				+ "0007|"
				+ "|"
				+ "|"
				+ trimZeros( dqGeneralTreatment( grabSimpleValue("SupplierPartNumber") ) ) + "|"
				+ color + "|"
				+ localCode + "|"
				+ ("".equals(a1) ? grabSimpleValue("CostoNetoSinIVA") : a1) + "|"
				+ grabSimpleValue("Currency") + "|"
				+ ("".equals(p1) ? grabSimpleValue("PrecioSugeridocIVA") : p1) + "|"
				+ ("".equals(grabSimpleValue("SKU", entry.getValue())) ? grabSimpleValue("SKU") : grabSimpleValue("SKU", entry.getValue())) + "|"
				+ ("".equals(grabSimpleValue("MainBarCodeS4H", entry.getValue())) ? grabSimpleValue("MainBarCodeS4H") : grabSimpleValue("MainBarCodeS4H", entry.getValue())) + "|"
				+ ("".equals(grabSimpleValue("NUMTP_S4H", entry.getValue())) ? grabSimpleValue("NUMTP_S4H") : grabSimpleValue("NUMTP_S4H", entry.getValue())) + "|"
				+ trimZeros( dqGeneralTreatment( ( "".equals(grabSimpleValue("SupplierPartNumber", entry.getValue())) ? grabSimpleValue("SupplierPartNumber") : grabSimpleValue("SupplierPartNumber", entry.getValue())) ) )
			);
	}

	private String getHFileLineJana(String id, String parentId, String ean, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicMap, String numtpS4H) {
		return  id.substring(1) +"|"+
				parentId+"|"+
			 trimZeros( dqGeneralTreatment( grabSimpleValue("SupplierPartNumber", characteristicMap) ) ) +"|"+
				"0007|"+
				grabSimpleValue("FSH_SEASON_YEAR", characteristicMap)+"|"+
				grabSimpleValue("FSH_SEASON", characteristicMap)+"|"+
				grabSimpleValue("FSH_COLLECTION", characteristicMap)+"|"+
				paddZeros(10, grabSimpleValue("SupplierID", characteristicMap)) +"|"+
				grabSimpleValue("TImportacion", characteristicMap)+"|"+
				grabSimpleValue("ItemGroupS4H", characteristicMap)+"|"+
				grabSimpleValue("SAPObjectType", characteristicMap)+"|"+
				grabSimpleValue("WHERL", characteristicMap)+"|"+
				grabSimpleValue("BRAND_ID_S4H", characteristicMap)+"|"+
				"|"+
				grabSimpleValue("ZZLIC_S4H", characteristicMap)+"|"+
				"|"+
				"|"+
				grabSimpleValue("PLGTP", characteristicMap)+"|"+
				grabSimpleValue("FSH_THEME", characteristicMap)+"|"+
				grabSimpleValue("FIBER_CODE1", characteristicMap)+"|"+
				grabSimpleValue("FIBER_CODE2", characteristicMap)+"|"+
				grabSimpleValue("FIBER_CODE3", characteristicMap)+"|"+
				grabSimpleValue("FIBER_CODE4", characteristicMap)+"|"+
				grabSimpleValue("FIBER_CODE5", characteristicMap)+"|"+
				grabSimpleValue("FIBER_PART1", characteristicMap)+"|"+
				grabSimpleValue("FIBER_PART2", characteristicMap)+"|"+
				grabSimpleValue("FIBER_PART3", characteristicMap)+"|"+
				grabSimpleValue("FIBER_PART4", characteristicMap)+"|"+
				grabSimpleValue("FIBER_PART5", characteristicMap)+"|"+
				grabSimpleValue("LABOR_S4H", characteristicMap)+"|"+
				grabSimpleValue("SAP_BEHVO", characteristicMap)+"|"+
				grabSimpleValue("CostoNetoSinIVA", characteristicMap)+"|"+
				grabSimpleValue("Currency", characteristicMap)+"|"+
				grabSimpleValue("PrecioSugeridocIVA", characteristicMap)+"|"+
				grabSimpleValue("SKU", characteristicMap)+"|"+
				ean +"|"+
				numtpS4H /* grabSimpleValue("NUMTP_S4H", characteristicMap) */ +"|"+
				grabSimpleValue("ArgumentoDeVenta", characteristicMap)+"|"+
			dqGeneralTreatment( grabSimpleValue("Name", characteristicMap) ) +"|"+
				grabSimpleValue("Status", characteristicMap)+"|"+
				grabSimpleValue("BWSCL", characteristicMap)+"|"+
				grabSimpleValue("EXTWG_S4H", characteristicMap)+"|"+
				grabSimpleValue("TAXKM1_S4H", characteristicMap)+"|"+
				grabSimpleValue("TAXKM2_S4H", characteristicMap)+"|"+
				grabSimpleValue("TAXM3_S4H", characteristicMap)+"|"+
			dqGeneralTreatment( grabSimpleValue("TextoAdicional", characteristicMap) ) +"|"+
				grabSimpleValue("BWVOR", characteristicMap)+"|"+
				grabSimpleValue("MAX_STACK", characteristicMap)+"|"+
				grabSimpleValue("MesdeEntregadeMercancIa", characteristicMap,true)+"|"+
				grabSimpleValue("GROES", characteristicMap);
	}

	private String getHFileLine(String id, String parentId, String ean, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicMap, Integer misHijos, String typeMainBarCode) {
		return  id +"|"+
				grabSimpleValue("SupplierID")+"|"+
				grabSimpleValue("SKU", characteristicMap)+"|"+
				parentId+"|"+
				"|"+
				"H|"+
				grabSimpleValue("SAPObjectType")+"|"+
				(misHijos == null ? "" : misHijos) +"|"+
				grabSimpleValue("SkuType")+"|"+
				grabSimpleValue("ItemGroup")+"|"+
				grabSimpleValue("Negocio")+"|"+
				grabSimpleValue("Temporada")+"|"+
				grabSimpleValue("AnoEstacion")+"|"+
				grabSimpleValue("TImportacion")+"|"+
				grabSimpleValue("BrandName")+"|"+
				grabSimpleValue("LicenseDescription")+"|"+
				grabSimpleValue("GradoDemoda")+"|"+
				"UN|"+
				grabSimpleValue("SAP_BEHVO")+"|"+
				grabSimpleValue("WHERL")+"|"+
				"|"+
				"UN|"+
				typeMainBarCode /* grabSimpleValue("TypeMainBarCode", characteristicMap) */ +"|"+
				ean+"|"+
				grabSimpleValue("PerfilDeRedondeo")+"|"+
			dqGeneralTreatment( grabSimpleValue("SupplierPartNumber") ) +"|"+ // 
				grabSimpleValue("ProductWidth")+"|"+
				grabSimpleValue("ProductDepth")+"|"+
				grabSimpleValue("ProductHeight")+"|"+
				grabSimpleValue("VOLUMAtt")+"|"+
				grabSimpleValue("PesoBruto")+"|"+
				grabSimpleValue("ProductWeight")+"|"+
		        ( "".equals( grabSimpleValue("ProductDepth") ) || grabSimpleValue("ProductDepth") == null ? "" : grabSimpleValue("UnidadDeMedidaLongitud") ) + "|" +
				( "".equals( grabSimpleValue("VOLUMAtt") ) || grabSimpleValue("VOLUMAtt") == null ? "" : grabSimpleValue("UnidadDeMedidaVolumen") ) + "|" +
				( "".equals( grabSimpleValue("PesoBruto") ) || grabSimpleValue("PesoBruto") == null ? "" : grabSimpleValue("UnidadDeMedidaPeso") ) + "|" +
				grabSimpleValue("CostoEnMonedaExtranjera")+"|"+
				grabSimpleValue("FechaInicioVigenciaCostoImportacion")+"|"+
				(!"".equals(grabSimpleValue("CostoEnMonedaExtranjera")) ? grabSimpleValue("Currency") : "" )+"|"+
				grabSimpleValue("CostobrutoSinIVA")+"|"+
				grabSimpleValue("PrecioSugeridocIVA")+"|"+
				grabSimpleValue("FechaInicioVigenciaPrecioVenta")+"|"+
				"MXP" +"|"+
				grabSimpleValue("IndicadordeImpuesto")+"|"+
				grabSimpleValue("Descuento1")+"|"+
				grabSimpleValue("Descuento2")+"|"+
				"|"+
				"|"+
				grabSimpleValue("CostoNetoSinIVA")+"|"+
				grabSimpleValue("FechaInicioVigenciaCostoNeto")+"|"+
				"|"+
			dqGeneralTreatment( grabSimpleValue("Name") ) +"|"+
			dqGeneralTreatment( grabSimpleValue("TextoAdicional") ) +"|"+
				"|"+
				"|"+
				grabSimpleValue("MesdeEntregadeMercancIa")+"|"+
				grabSimpleValue("Evento")+"|"+
				"|"+
				grabSimpleValue("ProductTypeSAP")+"|"+
				grabSimpleValue("Coleccion")+"|"+
				grabSimpleValue("Armado")+"|"+
				grabSimpleValue("MesdeEntregadeMuestra")+"|"+
				grabSimpleValue("ZBRECJ")+"|"+
				grabSimpleValue("ZLAECJ")+"|"+
				grabSimpleValue("ZHOECJ")+"|"+
				( "".equals( grabSimpleValue("ZLAECJ") ) || grabSimpleValue("ZLAECJ") == null ? "" : grabSimpleValue("UnidadDeMedidaLongitud") ) + "|" +
				grabSimpleValue("ZVOLCJ")+"|"+
				( "".equals( grabSimpleValue("ZVOLCJ") ) || grabSimpleValue("ZVOLCJ") == null ? "" : grabSimpleValue("UnidadDeMedidaVolumen") ) + "|" +
				grabSimpleValue("ZBRGCJ")+"|"+
				grabSimpleValue("ZNTGCJ")+"|"+
				( "".equals( grabSimpleValue("ZBRGCJ") ) || grabSimpleValue("ZBRGCJ") == null || "".equals( grabSimpleValue("ZNTGCJ") ) || grabSimpleValue("ZNTGCJ") == null  ? "" : grabSimpleValue("UnidadDeMedidaPeso") ) + "|" +
				grabSimpleValue("HNDLCODE")+"|"+
				grabSimpleValue("WHSTC")+"|"+
				grabSimpleValue("MVGR5")+"|"+
				"|"+
				"|"+
				( grabSimpleValue("ZHOEPQ") == null || "".equals(grabSimpleValue("ZHOEPQ")) ? "" : new java.math.BigDecimal(grabSimpleValue("ZHOEPQ")).intValue() )+"|"+
				( grabSimpleValue("ZBREPQ") == null || "".equals(grabSimpleValue("ZBREPQ")) ? "" : new java.math.BigDecimal(grabSimpleValue("ZBREPQ")).intValue() )+"|"+
				( grabSimpleValue("ZLAEPQ") == null || "".equals(grabSimpleValue("ZLAEPQ")) ? "" : new java.math.BigDecimal(grabSimpleValue("ZLAEPQ")).intValue() )+"|"+
				( "".equals( grabSimpleValue("ZLAEPQ") ) || grabSimpleValue("ZLAEPQ") == null ? "" : grabSimpleValue("UnidadDeMedidaLongitud") ) + "|" +
				( grabSimpleValue("ZVOLPQ") == null || "".equals(grabSimpleValue("ZVOLPQ")) ? "" : new java.math.BigDecimal(grabSimpleValue("ZVOLPQ")).intValue() ) +"|"+
				( "".equals( grabSimpleValue("ZVOLPQ") ) || grabSimpleValue("ZVOLPQ") == null ? "" : grabSimpleValue("UnidadDeMedidaVolumen") ) + "|" + // ¿Quieres decir que si tiene valor en el otro campo ponga ese y solo si no tiene deje el qu está?¡
				( grabSimpleValue("ZBRGPQ") == null || "".equals( grabSimpleValue("ZBRGPQ"))? "" : new java.math.BigDecimal(grabSimpleValue("ZBRGPQ")).intValue() )+"|"+
				( grabSimpleValue("ZNTGPQ") == null || "".equals(grabSimpleValue("ZNTGPQ")) ? "" : new java.math.BigDecimal(grabSimpleValue("ZNTGPQ")).intValue() )+"|"+
				( "".equals( grabSimpleValue("ZBRGPQ") ) || grabSimpleValue("ZBRGPQ") == null ? "" : grabSimpleValue("UnidadDeMedidaPeso") ) + "|" +
				grabSimpleValue("NORMT")+"|"+
				grabSimpleValue("LABOR") + "|" +
				grabSimpleValue("MAX_STACK")
				;
	}

	private java.util.Map<String, String> seleccionaLasDesas(String business) {

		java.util.Map<String, String> lasdesas = new java.util.TreeMap<>();

		String attributeGroup =
				"SBB".equals(business)
					? "CategorySpecificAttributesS4H"
					: "CategorySpecificAttributesSAP";

		String externalSystem =
				"SBB".equals(business)
					? "S4HANA"
					: "ECC";

		java.util.Map<String, java.util.Set<String>> groups =
				dastub.getSourceLookupValueCodesByReferencedLookupValueCodes(
						"Characteristics",
						"AttributeGroup",
						java.util.Collections.singletonList(attributeGroup));

		java.util.Set<String> characteristics = groups.get(attributeGroup);

		if (characteristics == null || characteristics.isEmpty()) {
			return lasdesas;
		}

		for (org.json.JSONObject row :
				dastub.getLookupValueCodeNameExternalCodeRows(
						"Characteristics",
						10,
						externalSystem,
						true)) {

			String code = row.optString("code", "");

			if (characteristics.contains(code)) {
				lasdesas.put(
						code,
						row.optString("externalCode", ""));
			}
		}

		return lasdesas;
	}
	
//	private java.util.Map<String, String> seleccionaLasDesas(String business){
//		java.util.Map<String, String> lasdesas = new java.util.TreeMap<>();
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		qp.put("lookup", "'Characteristics'");
//		qp.put("query", "LookupValueReference.LookupValues('AttributeGroup')->LookupValue.Code in (\"" + ("SBB".equals(business) ? "CategorySpecificAttributesS4H" : "CategorySpecificAttributesSAP") + "\")");
//		qp.put("fields", "LookupValue.Code,LookupValueIdentifier.Code(" + ("SBB".equals(business) ? "S4HANA" : "ECC") + ")");
//		qp.put("pageSize", "250");
//
//		int currentIndex = 0;
//		int totalSize = 0;
//		try {
//			do {
//				qp.put("startIndex", String.valueOf(currentIndex));
//				response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
//				totalSize = response.getInt("totalSize");
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					currentIndex++;
//					values = rows.getJSONObject(i).getJSONArray("values");
//					lasdesas.put(values.getString(0),values.getString(1));
//				}
//			}while(currentIndex < totalSize);
//			currentIndex = 0;
//		}catch(org.json.JSONException e) {
//			log(workshop.getRawResponse());
//			logE(e);
//			return null;
//		}
//		return lasdesas;
//	}

	private String dqGeneralTreatment(String input) {
		return input == null || "".equals(input) ? input : 
			input
				.replaceAll("[ÁÀÄÂ]+", "A")
				.replaceAll("[ÉÈËÊ]+", "E")
				.replaceAll("[ÍÌÏÎ]+", "I")
				.replaceAll("[ÓÒÖÔ]+", "O")
				.replaceAll("[ÚÙÜÛ]+", "U")
				.replaceAll("[Ñ]+", "N")
				.replaceAll("[áâäà]+", "a")
				.replaceAll("[éèëê]+", "e")
				.replaceAll("[íìïî]+", "i")
				.replaceAll("[óöôò]+", "o")
				.replaceAll("[úüù]+", "u")
				.replaceAll("[ñ]+", "n")
				.replaceAll("[^A-Z0-9a-z\\. ]+", " ")
				.replaceAll(" {2,}", " ")
				.trim()
				;
	}
	
	private String trimZeros(String input) {
		return input == null || "".equals(input) ? input : input.replaceAll("^0+", "");
	}
	
	private void getDFileLinesMarciano(String business, String parentId, String id, java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> entry, java.util.Map<String, String> lasDesas, java.util.LinkedList<String> lineasD) throws ServiceUnavailableException {
		int counter = 1;
		String elese = null;
		String typeMainBarCode = null;
		String elcampoLatalla = null;
		String latalla = null;
		String localCode = null;
		latalla = grabSimpleValue("TamanoUnico", entry.getValue(), true);
		String itemGroup = null; // grabSimpleValue("ItemGroup");
		itemGroup = grabSimpleValue(!"Suburbia".equals(business) ? "ItemGroup" : "ItemGroupS4H");
		if(itemGroup == null || "".equals(itemGroup)) {
			//PANIC
		}
		log("(Mars 1.1) El campo la talla: " + itemGroup + " (latalla: " + latalla + ")");
		typeMainBarCode = grabSimpleValue("TypeMainBarCode", entry.getValue());
		if(typeMainBarCode == null || "".equals(typeMainBarCode)) {
			typeMainBarCode = grabSimpleValue("TypeMainBarCode");
		}
		if(latalla != null && !"".equals(latalla)) {
			elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
			log("El campoLatalla: " + elcampoLatalla);
			if(elcampoLatalla != null) {
				elcampoLatalla = lasDesas.get(elcampoLatalla);
			}
			localCode = getCodeLatalla(latalla, elcampoLatalla);
			log("Getting local code for latalla: " + localCode + " (campoLatalla: " + elcampoLatalla + ").");
			log("Getting local code for latalla: " + localCode + " (campoLatalla: " + elcampoLatalla + ").");
			log("(Mars) El campo la talla: " + elcampoLatalla + " (" + itemGroup + ")");
			if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
				elcampoLatalla = ""; // "TMU01";
			}
			String p1 = grabSimpleValue("PrecioSugeridocIVA", entry.getValue());
			String a1 = grabSimpleValue("CostoEnMonedaExtranjera", entry.getValue());
			String a2 = grabSimpleValue("CostoNetoSinIVA", entry.getValue());
			String a3 = grabSimpleValue("CostobrutoSinIVA", entry.getValue());
			String z1 = grabSimpleValue("FechaInicioVigenciaPrecioVenta", entry.getValue());
			String z2 = grabSimpleValue("FechaInicioVigenciaCostoImportacion", entry.getValue());
			String z3 = grabSimpleValue("FechaInicioVigenciaCostoNeto", entry.getValue());
			lineasD.addLast(
				  id + "|"
				+ grabSimpleValue("SupplierID") + "|"
				+ grabSimpleValue("SKU", entry.getValue()) + "|"
				+ parentId + "|"
				+ ""  + "|"
				+ "D" + "|"
				+ "00" + "|"  // grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
				+ "" + "|" // ("00".equals(grabSimpleValue("SAPObjectType", entry.getValue())) ? "" : paddZeros(4, grabSimpleValue("ZNUMV") ) ) + "|"
				+ typeMainBarCode + "|"
				+ "" + "|"
				+ aSeisPosiciones(counter) + "|"
				+ elcampoLatalla + "|"
				+ paddZeros(4, localCode) + "|"
				+ "" + "|"
				+ ("".equals(p1) ? grabSimpleValue("PrecioSugeridocIVA") : p1) + "|"
				+ ("".equals(z1) ? grabSimpleValue("FechaInicioVigenciaPrecioVenta") : z1) + "|"
				+ ("".equals(a1) ? grabSimpleValue("CostoEnMonedaExtranjera") : a1) + "|"
				+ ("".equals(z2) ? grabSimpleValue("FechaInicioVigenciaCostoImportacion") : z2) + "|"
				+ ("".equals(a2) ? grabSimpleValue("CostoNetoSinIVA") : a2) + "|"
				+ ("".equals(z3) ? grabSimpleValue("FechaInicioVigenciaCostoNeto") : z3) + "|"
				+ grabSimpleValue("MainBarCode", entry.getValue()) + "|"
				+ dqGeneralTreatment( grabSimpleValue("TextoAdicional") ) + "|"
				+ ("".equals(a3) ? grabSimpleValue("CostobrutoSinIVA") : a3) + "|"
				+ trimZeros( dqGeneralTreatment( grabSimpleValue("SupplierPartNumber", entry.getValue()) ) )
			);
			counter++;
		}
		for(java.util.Map.Entry<String, String> ladesa : lasDesas.entrySet()) {
			elese = grabSimpleValue(ladesa.getKey());
			if(elese == null || "".equals(elese)) {
				elese = grabSimpleValue(ladesa.getKey(), entry.getValue());
			}
			if(elese != null && !"".equals(elese)) {
				String p1 = grabSimpleValue("PrecioSugeridocIVA", entry.getValue());
				String a1 = grabSimpleValue("CostoEnMonedaExtranjera", entry.getValue());
				String a2 = grabSimpleValue("CostoNetoSinIVA", entry.getValue());
				String a3 = grabSimpleValue("CostobrutoSinIVA", entry.getValue());
				String z1 = grabSimpleValue("FechaInicioVigenciaPrecioVenta", entry.getValue());
				String z2 = grabSimpleValue("FechaInicioVigenciaCostoImportacion", entry.getValue());
				String z3 = grabSimpleValue("FechaInicioVigenciaCostoNeto", entry.getValue());
				lineasD.addLast(
					  id + "|"
					+ grabSimpleValue("SupplierID") + "|"
					+ grabSimpleValue("SKU", entry.getValue()) + "|"
					+ parentId + "|"
					+ ""  + "|"
					+ "D" + "|"
					+ grabSimpleValue("SAPObjectType", entry.getValue()) + "|"
					+ ("00".equals(grabSimpleValue("SAPObjectType",entry.getValue())) ? "" : paddZeros(4, grabSimpleValue("ZNUMV") ) ) + "|"
					+ typeMainBarCode + "|"
					+ "" + "|"
					+ aSeisPosiciones(counter) + "|"
					+ ladesa.getValue() + "|"
					+ elese + "|"
					+ "" + "|"
					+ ("".equals(p1) ? grabSimpleValue("PrecioSugeridocIVA") : p1) + "|"
					+ ("".equals(z1) ? grabSimpleValue("FechaInicioVigenciaPrecioVenta") : z1) + "|"
					+ ("".equals(a1) ? grabSimpleValue("CostoEnMonedaExtranjera") : a1) + "|"
					+ ("".equals(z2) ? grabSimpleValue("FechaInicioVigenciaCostoImportacion") : z2) + "|"
					+ ("".equals(a2) ? grabSimpleValue("CostoNetoSinIVA") : a2) + "|"
					+ ("".equals(z3) ? grabSimpleValue("FechaInicioVigenciaCostoNeto") : z3) + "|"
					+ grabSimpleValue("MainBarCode", entry.getValue()) + "|"
					+ dqGeneralTreatment( grabSimpleValue("TextoAdicional") ) + "|"
					+ ("".equals(a3) ? grabSimpleValue("CostobrutoSinIVA") : a3) + "|"
					+ trimZeros( dqGeneralTreatment( grabSimpleValue("SupplierPartNumber", entry.getValue()) ) )
				);
				counter++;
			}
		}
		counter = 1;

	}

	private String getAtributoSapLatalla(String itemGroup, String business) throws ServiceUnavailableException {
		String value = null;
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl(this.workshop.getBaseUrl());
		rw.addHeader("Authorization", this.workshop.getRc().getHeader().get("Authorization"));
		String dp = ("SBB".equals(business) ? "TallaUnicavsTallaS4H" : "TallaUnicavsTallaERP");
		rw.putParameter("dictionaryProxy", "'" + dp + "'");
		rw.putParameter("fields", "StandardizationValue.AlternativeValue");
		rw.putParameter("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + dp + "\" and StandardizationValue.Value equals \"" + itemGroup + "\"");

		org.json.JSONObject response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch");
		if(response != null) {
			org.json.JSONArray rows = response.getJSONArray("rows");
			if(rows.length() > 0) {
				value = rows.getJSONObject(0).getJSONArray("values").getString(0);
			}
		}else {
			log("###$$ ERROR: " + rw.getRawResponse());
		}
		if(value == null || ("".equals(value) && !"SBB".equals(business))) {
			dp = ("ItemGroupSAPSizeAttribute");
			rw.putParameter("dictionaryProxy", "'" + dp + "'");
			rw.putParameter("fields", "StandardizationValue.AlternativeValue");
			rw.putParameter("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + dp + "\" and StandardizationValue.Value equals \"" + itemGroup + "\"");

			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch");
			if(response != null) {
				org.json.JSONArray rows = response.getJSONArray("rows");
				if(rows.length() > 0) {
					value = rows.getJSONObject(0).getJSONArray("values").getString(0);
				}
			}else {
				log("###$$ ERROR: " + rw.getRawResponse());
			}
		}
		return value;
	}

	private void getDFileLines(
			String id, 
			String business, 
			String parentId, 
			java.util.LinkedList<java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>>> lalista, 
			java.util.Map<String, String> lasDesas, 
			java.util.LinkedList<String> lineasD, 
			boolean ismkp,
			short modif
			) throws ServiceUnavailableException {
		int counter = 1;
		String elese = null;
		String elcampoLatalla = null;
		String itemGroup = null;
		String latalla = null;
		String localCode = null;
		String sku = null;
		itemGroup = grabSimpleValue(!"SBB".equals(business) ? "ItemGroup" : "ItemGrouopS4H");
		if(itemGroup == null || "".equals(itemGroup)) {
			//PANIC
		}
		elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
		log("(Normal) El campo la talla (para mkt): " + elcampoLatalla + " (" + itemGroup + ", " + business + ")");
		if(elcampoLatalla != null) {
			log("*******************####################******************* " + elcampoLatalla + " - "
					+ lasDesas.get(elcampoLatalla));
			elcampoLatalla = lasDesas.get(elcampoLatalla);
		}
		if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
			elcampoLatalla = "";
		}
		for(java.util.Map.Entry<String, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>>> entry : lalista ) {
			if(ismkp) {
				sku = grabSimpleValue("SKU", entry.getValue());
				if(modif == 0 && sku != null && !"".equals(sku)) {
					continue;
				}else if(modif == 1 && (sku == null || "".equals(sku))) {
					continue;
				}
			}
			for(java.util.Map.Entry<String, String> ladesa : lasDesas.entrySet()) {
				if(ismkp) {
					elese = grabSimpleValue(ladesa.getKey());
					if(elese == null || "".equals(elese)) {
						elese = grabSimpleValue(ladesa.getKey(), entry.getValue());
					}
				}else {
					elese = grabSimpleValue(ladesa.getKey(), entry.getValue());
				}
				String p1 = grabSimpleValue("PrecioSugeridocIVA", entry.getValue());
				String a1 = grabSimpleValue("CostoEnMonedaExtranjera", entry.getValue());
				String a2 = grabSimpleValue("CostoNetoSinIVA", entry.getValue());
				String a3 = grabSimpleValue("CostobrutoSinIVA", entry.getValue());
				String z1 = grabSimpleValue("FechaInicioVigenciaPrecioVenta", entry.getValue());
				String z2 = grabSimpleValue("FechaInicioVigenciaCostoImportacion", entry.getValue());
				String z3 = grabSimpleValue("FechaInicioVigenciaCostoNeto", entry.getValue());
				if(elese != null && !"".equals(elese) && ladesa.getValue() != null && !"".equals(ladesa.getValue())) {
					lineasD.addLast(
						  (id == null ? entry.getKey() : id) + "|"
						+ grabSimpleValue("SupplierID") + "|"
						+ grabSimpleValue("SKU", entry.getValue()) + "|"
						+ parentId + "|"
						+ ""  + "|"
						+ "D" + "|"
						+ (ismkp ? "00" : grabSimpleValue("SAPObjectType", entry.getValue()) ) + "|"
						+ ( "00".equals(grabSimpleValue("SAPObjectType", entry.getValue()) ) ? "" : paddZeros(4, grabSimpleValue("ZNUMV") ) ) + "|"
						+ grabSimpleValue("TypeMainBarCode", entry.getValue()) + "|"
						+ "" + "|"
						+ aSeisPosiciones(counter) + "|"
						+ ladesa.getValue() + "|"
						+ elese + "|"
						+ "" + "|"
						+ ("".equals(p1) ? grabSimpleValue("PrecioSugeridocIVA") : p1) + "|"
						+ ("".equals(z1) ? grabSimpleValue("FechaInicioVigenciaPrecioVenta") : z1) + "|"
						+ ("".equals(a1) ? grabSimpleValue("CostoEnMonedaExtranjera") : a1) + "|"
						+ ("".equals(z2) ? grabSimpleValue("FechaInicioVigenciaCostoImportacion") : z2) + "|"
						+ ("".equals(a2) ? grabSimpleValue("CostoNetoSinIVA") : a2) + "|"
						+ ("".equals(z3) ? grabSimpleValue("FechaInicioVigenciaCostoNeto") : z3) + "|"
						+ grabSimpleValue("MainBarCode", entry.getValue()) + "|"
						+ dqGeneralTreatment( grabSimpleValue("TextoAdicional") ) + "|"
						+ ("".equals(a3) ? grabSimpleValue("CostobrutoSinIVA") : a3) + "|"
						+ dqGeneralTreatment( grabSimpleValue("SupplierPartNumber", entry.getValue()) )
					);
					counter++;
				}else {
					if(ladesa.getValue() == null || "".equals(ladesa.getValue())) {
						log("Non mapped key: " + ladesa.getKey() + ", the value (" + elese + ")");
						log("Non mapped key: " + ladesa.getKey() + ", the value (" + elese + ")");
					}else {
//						log("Parent key: " + parentId + ", No data found.");
					}
				}
			}
			latalla = grabSimpleValue("TamanoUnico", entry.getValue(), true);
			if(latalla != null && !"".equals(latalla)) {
				elcampoLatalla = getAtributoSapLatalla(itemGroup, business);
				if(elcampoLatalla != null) {
//					log(elcampoLatalla + " - " + lasDesas);
					elcampoLatalla = lasDesas.get(elcampoLatalla);
				}
				if(elcampoLatalla == null || "".equals(elcampoLatalla)) {
					elcampoLatalla = ""; // "TMU01";
				}
				localCode = getCodeLatalla(latalla, elcampoLatalla);
				log("Getting local code for latalla: " + localCode + ".");
				String p1 = grabSimpleValue("PrecioSugeridocIVA", entry.getValue());
				String a1 = grabSimpleValue("CostoEnMonedaExtranjera", entry.getValue());
				String a2 = grabSimpleValue("CostoNetoSinIVA", entry.getValue());
				String a3 = grabSimpleValue("CostobrutoSinIVA", entry.getValue());
				String z1 = grabSimpleValue("FechaInicioVigenciaPrecioVenta", entry.getValue());
				String z2 = grabSimpleValue("FechaInicioVigenciaCostoImportacion", entry.getValue());
				String z3 = grabSimpleValue("FechaInicioVigenciaCostoNeto", entry.getValue());
				lineasD.addLast(
						(id == null ? entry.getKey() : id) + "|"
					+ grabSimpleValue("SupplierID") + "|"
					+ grabSimpleValue("SKU", entry.getValue()) + "|"
					+ parentId + "|"
					+ ""  + "|"
					+ "D" + "|"
					+  (ismkp ? "00" : grabSimpleValue("SAPObjectType", entry.getValue()) ) + "|"
					+ ("00".equals(grabSimpleValue("SAPObjectType")) ? "" : paddZeros(4, grabSimpleValue("ZNUMV") ) ) + "|"
					+ grabSimpleValue("TypeMainBarCode", entry.getValue()) + "|"
					+ "" + "|"
					+ aSeisPosiciones(counter) + "|"
					+ elcampoLatalla + "|"
					+ paddZeros(4, localCode) + "|"
					+ "" + "|"
					+ ("".equals(p1) ? grabSimpleValue("PrecioSugeridocIVA") : p1) + "|"
					+ ("".equals(z1) ? grabSimpleValue("FechaInicioVigenciaPrecioVenta") : z1) + "|"
					+ ("".equals(a1) ? grabSimpleValue("CostoEnMonedaExtranjera") : a1) + "|"
					+ ("".equals(z2) ? grabSimpleValue("FechaInicioVigenciaCostoImportacion") : z2) + "|"
					+ ("".equals(a2) ? grabSimpleValue("CostoNetoSinIVA") : a2) + "|"
					+ ("".equals(z3) ? grabSimpleValue("FechaInicioVigenciaCostoNeto") : z3) + "|"
					+ grabSimpleValue("MainBarCode", entry.getValue()) + "|"
					+ dqGeneralTreatment( grabSimpleValue("TextoAdicional") ) + "|"
					+ ("".equals(a3) ? grabSimpleValue("CostobrutoSinIVA") : a3) + "|"
					+ dqGeneralTreatment( grabSimpleValue("SupplierPartNumber", entry.getValue()) )
				);
				counter++;
			}
			counter = 1;

//			ID||'|'||
//			SupplierID||'|'||
//			SKU||'|'||
//			PARENT_ID||
//			'||'||
//			'D|'||
//			SAPObjectType||'|'||
//			ZNUMV||'|'||
//			TypeMainBarCode||
//			'||'||
//			'00000'||Contador||'|'||
//			NameSap||
//			'|'||IIF( Length(REPLACECHR( 0, Parsed1, '\[\]\"', NULL ))=1 , '000'||REPLACECHR( 0, Parsed1, '\[\]\"', NULL ), IIF( Length(REPLACECHR( 0, Parsed1, '\[\]\"', NULL ))=2,'00'||REPLACECHR( 0, Parsed1, '\[\]\"', NULL ),IIF( Length(REPLACECHR( 0, Parsed1, '\[\]\"', NULL ))=3,'0'||REPLACECHR( 0, Parsed1, '\[\]\"', NULL ),REPLACECHR( 0, Parsed1, '\[\]\"', NULL ))))||
//			'||'||
//			PrecioSugeridocIVA||'|'||
//			FechaInicioVigenciaPrecioVenta||'|'||
//			CostoEnMonedaExtranjera||'|'||
//			FechaInicioVigenciaCostoImportacion||'|'||
//			CostoNetoSinIVA||'|'||
//			FechaInicioVigenciaCostoNeto||'|'||
//			MainBarCode||'|'||
//			TextoAdicional||'|'||
//			CostobrutoSinIVA||'|'||
//			SupplierPartNumber
		}
	}

	private String paddZeros(int zeros, String value) {
		StringBuilder sb = new StringBuilder();
		int toPadd = value == null ? zeros : zeros - value.length();
		for(int i=0; i<toPadd; i++) {
			sb.append("0");
		}
		sb.append(value == null ? "" : value);
		return sb.toString();
	}

	private String aSeisPosiciones(int elcounter) {
		StringBuilder sb = new StringBuilder();
		int trail = 5 - String.valueOf(elcounter).length();
		for(int i=0; i<trail; i++) {
			sb.append("0");
		}
		sb.append(elcounter);
		return sb.toString();
	}

	private String getTemplateFromData(org.json.JSONObject data) {
		String template = null;
		String structureGroupId = null;
		if(data.has("structureGroupMap")) {
			org.json.JSONArray structureGroupMap = data.getJSONArray("structureGroupMap");
			for(int i=0; i<structureGroupMap.length(); i++) {
				structureGroupId = structureGroupMap.getJSONObject(i).getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId");
				if(structureGroupId.contains("PrimaryProductTaxonomy")) {
					template = structureGroupId.replaceAll("((^')|(('@'PrimaryProductTaxonomy')$))", "");
					break;
				}
			}
		}
		return template;
	}

	public String grabSimpleValue(String characteristicName, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicMap, boolean getLabel) {
		java.util.LinkedList<org.json.JSONObject> objectList = characteristicMap.get(characteristicName);
		return objectList == null || objectList.isEmpty() ? "" : grabCharacteristicValue(objectList.getFirst(), getLabel);
	}

	public String grabSimpleValue(String characteristicName, java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> characteristicMap) {
		java.util.LinkedList<org.json.JSONObject> objectList = characteristicMap.get(characteristicName);
		return objectList == null || objectList.isEmpty() ? "" : grabCharacteristicValue(objectList.getFirst(), false);
	}

	public String grabSimpleValue(String characteristicName) {
		java.util.LinkedList<org.json.JSONObject> objectList = characteristicsMap.get(characteristicName);
		return objectList == null || objectList.isEmpty() ? "" : grabCharacteristicValue(objectList.getFirst(), false);
	}

	public String grabSimpleValue(String characteristicName, boolean getLabel) {
		java.util.LinkedList<org.json.JSONObject> objectList = characteristicsMap.get(characteristicName);
		return objectList == null || objectList.isEmpty() ? "" : grabCharacteristicValue(objectList.getFirst(), getLabel);
	}

	public String grabCharacteristicValue(org.json.JSONObject characteristicObject, boolean getLabel) {
		String value = null;
		Object o = characteristicObject.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0);
		if(o instanceof org.json.JSONObject) {
			value = ((org.json.JSONObject)o).getString(getLabel ? "_label" : "_code");
		}else if(o instanceof java.lang.String) {
			value = (String) o;
		}else if(o instanceof java.lang.Integer){
			value = String.valueOf(o);
		} else {
			value = String.valueOf(o);
			log("No data type identified: " + o + ( o == null ? "null" : " " + o.getClass().getName() ));
		}
		return value == null ? "" : value;
	}

	public static void main(String[] args) throws ServiceUnavailableException {
		try(CrearArchivosParaSKU creati = new CrearArchivosParaSKU()){
//		String a[] = ("1754611671031441\r\n"
//				+ "1754611671028552").split("\\r\\n");
//		for(String a0 : a) {
//			try {
//				creati.handleContent(a0, creati.creacionDeArchivos(a0, (short) 0));
//			} catch (ServiceUnavailableException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}

			new Thread(creati::run).start();
			creati.process(args[0], args[1], args[2]);
		} catch (java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void run() {
		while(running) {
			try(
				java.net.ServerSocket server = new java.net.ServerSocket(23544);
				java.net.Socket cli = server.accept();
				java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(cli.getInputStream()));
				java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(cli.getOutputStream()))
			){
				try{
					org.json.JSONObject req = new org.json.JSONObject(br.readLine());
					String action = req.getString("action");
					if("finish".equals(action.toLowerCase())) {
						this.running = false;
					}
				}catch(org.json.JSONException e) {
					logE(e);
				}
			}catch(java.io.IOException e) {
				logE(e);
			}
			try {
				Thread.sleep(100);
			}catch(InterruptedException e) {
				logE(e);
			}
		}
		log("Finishing...");
	}

	private ConnectionFactory connectionFactory = null;
	private Connection connection;
	private Session session;
	private Destination responseQueue;
	private MessageConsumer consumer;
	private Message responseMessage;

	private boolean connected = false;
	private boolean failed = false;

	private void connect(String host, String port, String qName){
		try{
			connectionFactory = new ActiveMQConnectionFactory("tcp://" + host + ":" + port + "?wireFormat.maxInactivityDuration=60000&keepAlive=true");
			connection = connectionFactory.createConnection();
			connection.start();
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
	        responseQueue = session.createQueue(qName);
	        consumer = session.createConsumer(responseQueue);
			connected = true;
		}catch(JMSException e){
			e.printStackTrace();
			failed = true;
		}
	}

	
	
	private void process(String host, String port, String qName) throws ServiceUnavailableException {
		if(!failed){
			if(!connected){
				connect(host, port, qName);
				if(failed){
				}
			}
		}else{
		}
		if(failed) {
		}
		try{
			while(running){
				responseMessage = consumer.receive(3000);
			    if (responseMessage != null && responseMessage instanceof TextMessage) {
			     	procesaPropuesta(((TextMessage) responseMessage).getText());
			     	log("Doney");
				}
			}
		}catch(org.json.JSONException e){
    		logE(e);
    	} catch (JMSException e) {
    		logE(e);
		} catch (ParserConfigurationException e) {
			logE(e);
		} catch (SAXException e) {
			logE(e);
		} catch (IOException e) {
			logE(e);
		}finally {
			disconnect();
		}
		log("Done processing...");
	}

	private void procesaPropuesta(String message) throws ParserConfigurationException, SAXException, IOException, ServiceUnavailableException {
		log("<::>" + message + "<::>");
		String entity = null;
		String externalId = null;
		String changeSummary = null;

		String currentStatusNew = null;
		org.json.JSONArray changedField = null;

		java.util.Set<String> changedFieldSet = new java.util.TreeSet<>();

		org.json.JSONObject json = new org.json.JSONObject(message);
     	entity = json.getJSONObject("entityItemChange").getString("_entity");
     	changeSummary = json.getJSONObject("entityItemChange").getString("_changeSummary");
     	changedField = json.getJSONObject("entityItemChange").has("_changedField") ? json.getJSONObject("entityItemChange").getJSONArray("_changedField") : new org.json.JSONArray();

		for(int i=0; i<changedField.length(); i++) {
			changedFieldSet.add(changedField.getString(i));
		}

		log("changeFields: " + changedField);

     	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;

    	try(java.io.ByteArrayInputStream baos = new java.io.ByteArrayInputStream( changeSummary.getBytes(java.nio.charset.StandardCharsets.UTF_8) )){
    		doc = builder.parse( baos );
    		doc.getDocumentElement().normalize();
    	}

		if(doc != null) {
			Element rootElement = doc.getDocumentElement();
			try {
				log(xmm.prettyPrint(rootElement));
			} catch (TransformerException e) {
				logE(e);
			}
			if("Product2G".equals(entity)) {
				externalId = json.getJSONObject("entityItemChange").getString("_identifier");
				log("GOT proposalId: " + externalId);
				if(changedFieldSet.contains("Product2G.CurrentStatus")) {
					try{
						currentStatusNew = xmm.byName( xmm.byName( xmm.byName( xmm.byName(rootElement, "product"), "currentStatus"), "_current"), "_key" ).getTextContent();
						log("Got currentStatus: " + currentStatusNew);
					}catch(NullPointerException e) {
						logE(e);
						currentStatusNew = "";
					}
				}
				if("1020".equals(currentStatusNew)) {
					log("Ya vinimos aquí -.-");
					String[] info = checkProductWrapper( externalId );
					if(info == null) {
						log("Not found.");
						String[] contenido = creacionDeArchivos(externalId, (short) 0); // CREA
						handleContent(externalId, contenido);
					}else {
						log("With actual data: " + java.util.Arrays.asList(info));
						if("MKP".equals(info[1])) {
							if(!"".equals(info[2])) {
								String[] contenido = creacionDeArchivos(externalId, (short) 0); // CREA
								handleContent(externalId, contenido);
								
								contenido = creacionDeArchivos(externalId, (short) 1); // MODIF
								handleContent(externalId, contenido);
							}else {
								if("".equals(info[2]) && "MKP".equals(info[1])) {
									String[] contenido = creacionDeArchivos(externalId, (short) 0); // CREA
									handleContent(externalId, contenido);
								}
							}
						}else {
							if(!"".equals(info[2])) {
								String[] contenido = creacionDeArchivos(externalId, (short) 1); // MODIF
								handleContent(externalId, contenido);
							}else {
								String[] contenido = creacionDeArchivos(externalId, (short) 0); // CREA
								handleContent(externalId, contenido);
							}
						}
					}
				}
			}else if("StructureGroup".equals(entity)) {

			}else if("LookupValue".equals(entity)) {

			}
		}
	}
	
	private String[] checkProductWrapper(String id) {
		String[] data = checkProduct(id);
		if(data != null && "00".equals(data[0]) && "".equals(data[2])) {
			java.util.Set<String> varSet = dr.getVariants(id);
			if(varSet.size() == 1) {
				String rr = dr.getArticleData(new org.json.JSONArray().put(new java.util.ArrayList<>(varSet).get(0)));
				org.json.JSONObject jr = new org.json.JSONObject(rr);
				org.json.JSONArray items = jr.getJSONArray("items");
				org.json.JSONObject item = items.getJSONObject(0);
				if(!"".equals(item.getString("SKU"))) {
					data[2] = item.getString("SKU");
					log("Cambiamos el SKU de un individual. (" + id + ", " + item + ")");
				}
			}
		}
		return data;
	}
	
	private String[] checkProduct(String id) {
		try {
				String resp = dr.getProductData(new org.json.JSONArray().put(id));
				if(resp != null) {
					org.json.JSONObject rj = new org.json.JSONObject(resp);
					org.json.JSONArray items = rj.getJSONArray("items");
					org.json.JSONObject j = items.getJSONObject(0);
					if(
						j.has("SAPObjectType") 
						&& !"".equals(j.getString("SAPObjectType")) 
						&& !"02".equals(j.getString("SAPObjectType")))
						return new String[] {
								 j.getString("SAPObjectType")
								,j.getString("Business")
								,j.getString("SKU")
								,j.getString("CurrentStatus")
						};
					else return null;
				}
		}catch(org.json.JSONException e) {
			logE(e);
		}
		return null;
		/*
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields",
				  "Product2GCharacteristicValue.LookupValue('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType')->LookupValue.Code"
				+ ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business')->LookupValue.Code"
				+ ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
				+ ",Product2G.CurrentStatus");
		qp.put("query", "Product2G.ProductNo equals \"" + id + "\"");
		org.json.JSONObject response = null;
		response = workshop.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
		org.json.JSONArray rows = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			if(rows.length() > 0) {
				return new String[] { 
						  rows.getJSONObject(0).getJSONArray("values").getJSONArray(0).getString(0)
						, rows.getJSONObject(0).getJSONArray("values").getJSONArray(1).getString(0)
						, rows.getJSONObject(0).getJSONArray("values").getJSONArray(2).getString(0)
						, rows.getJSONObject(0).getJSONArray("values").getString(3)
					};
			}
		}else {
			log("ERROR: " + workshop.getRawResponse());
		}
		return null;
		*/
	}

	public void handleContent(String proposalId, String[] content) throws IOException {
		if(content == null || content[0] == null) {
			log("Problama enviando a creación de SKU: " + proposalId);
			log("Es probable que se deba a tiempos largos de respuesta de P360.");
			notifyStatus(false, proposalId);
			return;
		}
		boolean sent = false;
		long waitMillis = 100l;
		do {
	        SshClient client = SshClient.setUpDefaultClient();
	        client.start();
	        log("*****" + content[4]);
	        log("CONNECTING TO: " + ("SBB".equals(content[4]) ? HOST_SBB : HOST));
	        try (ClientSession session = client.connect(USER, "SBB".equals(content[4]) ? HOST_SBB : HOST, PORT)
	                .verify(10, TimeUnit.SECONDS)
	                .getSession()) {
	
	            FileKeyPairProvider keyProvider = new FileKeyPairProvider(PRIVATE_KEY_PATH);
	            keyProvider.setPasswordFinder(FilePasswordProvider.EMPTY);
	            keyProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);
	
	            session.auth().verify(10, TimeUnit.SECONDS);
	
	            try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
	            	writeToSftp(proposalId, sftp, content, PropertiesManager.get(
	            			"SBB".equals(content[4]) ? "p360.contingency.s4h.remote_directory_base" : 
	            				"p360.contingency.ecc.remote_directory_base", "/interfase/mer/in/step"));
	            }
	            sent = true;
	        } catch(java.io.IOException e) {
	        	log("Could not send request: " + e.getMessage());
	        	logE(e);
	        	
	        } finally {
	            client.stop();
	        }
	        if(!sent) {
	        	log("Retrying in: " + waitMillis);
	        	try{
	        		Thread.sleep(waitMillis);
	        	}catch(InterruptedException ex) {
	        		logE(ex);
	        	}
	        }
		}while(!sent);
		notifyStatus(true, proposalId);
	}
	
	private void notifyStatus(boolean sent, String externalId) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeObjectsInProtocol", "false");
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		columns.put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('ToSKUCreationStatus',root,\"0000.0000.RK\",'ToSKUCreationStatus',-1)"));
		org.json.JSONArray rows = new org.json.JSONArray();
		request.put("columns", columns);
		request.put("rows", rows);
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalId + "'@1")).put("values", new org.json.JSONArray().put(sent ? "Enviado: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) : "Necesita reenvío manual")));
	}

    private String writeToSftp(String proposalId, SftpClient sftp, String[] content, String remoteBasePath) throws IOException {

    	LocalDateTime now = LocalDateTime.now();
        String dateKey = now.format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        String periodKey = dateKey ; // + "_" + now.getHour() + String.format("%02d", block);

        Properties sequenceProps = new Properties();
        int sequence = 1;

        if (Files.exists(SEQUENCE_FILE)) {
            try (InputStream in = Files.newInputStream(SEQUENCE_FILE)) {
                sequenceProps.load(in);
                String lastPeriod = sequenceProps.getProperty("period");
                String lastSeq = sequenceProps.getProperty("seq");
                if (periodKey.equals(lastPeriod) && lastSeq != null) {
                    sequence = Integer.parseInt(lastSeq) + 1;
                }
            }
        }
//		STEPMDYYYYMMDDNum
//		STEPATTYYYYMMDDNum
//		STEPVARYYYYMMDDNum
//		STEPUOMYYYYMMDDNum

        String fileName = null;
        String fullPath = null;
        log("Now generating files... -->" + filePrefix + "<--");
        fileName = String.format("SBB".equals(content[4]) ? "STEPMD%s%04d.txt" : filePrefix +  "%s%04dH.txt", dateKey, sequence);
        log("First path: " + fileName);
        fullPath = remoteBasePath.endsWith("/") ? remoteBasePath + fileName : remoteBasePath + "/" + fileName;
        log("Writing: " + fullPath);
        keepFileToLocal(proposalId, fileName, content[0].getBytes(java.nio.charset.StandardCharsets.UTF_8), content[4]);
        try (OutputStream os = sftp.write(fullPath)) {
        	log("Writing out: " + fullPath);
            os.write(content[0].getBytes(java.nio.charset.StandardCharsets.UTF_8));
            log("LOG:: WROTE.");
        }
        fileName = String.format("SBB".equals(content[4]) ? "STEPVAR%s%04d.txt" : filePrefix + "%s%04dD.txt", dateKey, sequence);
        log("Second path: " + fileName);
        fullPath = remoteBasePath.endsWith("/") ? remoteBasePath + fileName : remoteBasePath + "/" + fileName;
        log("Writing: " + fullPath);
        keepFileToLocal(proposalId, fileName, content[1].getBytes(java.nio.charset.StandardCharsets.UTF_8), content[4]);
        try (OutputStream os = sftp.write(fullPath)) {
        	os.write(content[1].getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        fileName = String.format("SBB".equals(content[4]) ? "STEPUOM%s%04d.txt" : filePrefix + "%s%04dS.txt", dateKey, sequence);
        fullPath = remoteBasePath.endsWith("/") ? remoteBasePath + fileName : remoteBasePath + "/" + fileName;
        log("Writing: " + fullPath);
        keepFileToLocal(proposalId, fileName, content[2].getBytes(java.nio.charset.StandardCharsets.UTF_8), content[4]);
        try (OutputStream os = sftp.write(fullPath)) {
        	os.write(content[2].getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        fileName = String.format("SBB".equals(content[4]) ? "STEPATT%s%04d.txt" : filePrefix + "%s%04dT.txt", dateKey, sequence);
        fullPath = remoteBasePath.endsWith("/") ? remoteBasePath + fileName : remoteBasePath + "/" + fileName;
        log("Writing: " + fullPath);
        keepFileToLocal(proposalId, fileName, content[3].getBytes(java.nio.charset.StandardCharsets.UTF_8), content[4]);
        try (OutputStream os = sftp.write(fullPath)) {
        	os.write(content[3].getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        try (OutputStream out = Files.newOutputStream(SEQUENCE_FILE)) {
            sequenceProps.setProperty("date", dateKey);
            sequenceProps.setProperty("period", periodKey);
            sequenceProps.setProperty("seq", String.valueOf(sequence));
            sequenceProps.store(out, null);
        }

        return fullPath;
    }

    private void keepFileToLocal(String proposalId, String fileName, byte[] content, String business) {
    	log("Writing to : " + ("SBB".equals(business) ? "/u01/stage/SBB_SKU/" + proposalId + "_" + fileName : "/u01/stage/ECC_SKU/" + proposalId + "__" + fileName));
    	try(java.io.FileOutputStream fos = new java.io.FileOutputStream("SBB".equals(business) ? "/u01/stage/SBB_SKU/" + proposalId + "_" + fileName : "/u01/stage/ECC_SKU/" + proposalId + "__" + fileName)){
    		fos.write(content);
    	}catch(java.io.IOException e) {
    		logE(e);
    	}
    }

	private void disconnect() {
		if(connection != null){
			try{
				connection.close();
			}catch(JMSException e){
				e.printStackTrace();
			}
		}
	}

	private static final Logger LOGGER = Logger.getLogger(CrearArchivosParaSKU.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/amqp/crearArchivosParaSKU/activeMQToSKUCreation.log", 25 * 1024 * 1024, 10, true);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            fileHandler.setLevel(Level.ALL);

            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    java.time.LocalDateTime dateTime =
                        java.time.Instant.ofEpochMilli(record.getMillis())
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();

                    String timestamp = dateTime.format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );

                    return "[" + timestamp + "] [" + record.getLevel() + "] " + formatMessage(record) + System.lineSeparator();
                }
            });

            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);

        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el logger", e);
        }
    }

	private void log(String message) {
		LOGGER.info(message);
//        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//                new java.io.FileOutputStream("../logs/activeMQToSKUCreation.log", true)))) {
//            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
//                    + "]  " + message);
//        } catch (java.io.IOException e) {
//        }
//		System.out.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))+ "]  " + message);
    }

    private void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/activeMQToSKUCreation.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }

	@Override
	public void close() throws IOException {
		dastub.close();
	}

}
