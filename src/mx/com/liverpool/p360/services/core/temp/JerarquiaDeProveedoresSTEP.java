package mx.com.liverpool.p360.services.core.temp;

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

public class JerarquiaDeProveedoresSTEP {

	private static RESTWorkshop workshop = new RESTWorkshop();
	private static XMLMisc xmm = workshop.getXmm();

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException {
		long init = System.currentTimeMillis();
		String baseUrl =
				"https://webctep360qas.liverpool.com.mx/rest/V2.0"
				;
		workshop.setBaseUrl(baseUrl);
//		checkIfInTemplate();
//		System.exit(0);
		crearProveedores();
		cargaProveedores();
		
//		negocios();
//		tipoDeProveedorEmail();
		
//		listaDeValores();
//		definirAlcance();
		
		System.out.println(workshop.formatTime(System.currentTimeMillis() - init));
	}

	private static void tipoDeProveedorEmail() throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse(
				"C:\\opt\\LVP\\desorden\\Proveedores Raiz.xml"
//				"C:\\Users\\jcapizc\\Downloads\\step-472799325730140560-exported.xml"
				);
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("Classifications");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> suppliers = xmm.listImmediateChildElements(assetsRoot).get("Classification");

		org.json.JSONObject payload = new org.json.JSONObject();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray values = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueIdentifier.Code(SupplierEmail)")).put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(TipoDeProveedorLOV)")).put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(TipoProveedorSAPAttLOV)"));
		org.json.JSONObject object = new org.json.JSONObject();
		payload.put("columns", columns);
		payload.put("rows", rows);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		Element el = null;
		String id = null;

		Node metaData = null;
		Node emailNodo = null;
		Node tipoDeProveedorNodo = null;
		Node tipoProveedorSAPNodo = null;

		for(Node n : suppliers) {
			el = (Element)n;
			id = el.getAttribute("ID").replaceAll("-.+", "");
			if(xmm.byName(n, "Name") != null ) {
				metaData = xmm.byName(n, "MetaData");
				if(metaData != null) {
					emailNodo = xmm.byAttributeValue(metaData, "AttributeID", "EmailProveedor");
					tipoDeProveedorNodo = xmm.byAttributeValue(metaData, "AttributeID", "TipoDeProveedor");
					tipoProveedorSAPNodo = xmm.byAttributeValue(metaData, "AttributeID", "TipoProveedorSAP");
					values = new org.json.JSONArray();
					values.put(emailNodo != null ? emailNodo.getTextContent() : "");
					values.put(tipoDeProveedorNodo != null ? ((Element)tipoDeProveedorNodo).getAttribute("ID") : "");
					values.put(tipoProveedorSAPNodo != null ? ((Element)tipoProveedorSAPNodo).getAttribute("ID") : "");
					object = new org.json.JSONObject().put("id", "'" + id + "'@'Party'");
					rows.put(new org.json.JSONObject().put("object", object).put("values", values));
					if(rows.length() == 250) {
						System.out.println( workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString()) );
						while(rows.length() > 0) {
							rows.remove(0);
						}
					}
				}
			}
		}
		if(rows.length() > 0) {
			System.out.println( workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString()) );
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}

	}

	private static void negocios() throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse(
//				"C:\\Users\\jcapizc\\Downloads\\step-472799325730140560-exported.xml"
				"C:\\opt\\LVP\\desorden\\Proveedores Raiz.xml"
				);
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("Classifications");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> suppliers = xmm.listImmediateChildElements(assetsRoot).get("Classification");

		org.json.JSONObject payload = new org.json.JSONObject();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray values = new org.json.JSONArray();
		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues(BusinessQualified)"));
		org.json.JSONObject object = new org.json.JSONObject();
		payload.put("columns", columns);
		payload.put("rows", rows);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		Element el = null;
		String id = null;

		java.util.Map<String, java.util.LinkedList<Node>> mapa = null;
		java.util.LinkedList<Node> nodos = null;

		Node metaData = null;
		Node negocios = null;
		String negocio = null;

		String email = null;
		String tipoDeProveedor = null;
		String tipoDeProveedorSAP = null;

		for(Node n : suppliers) {
			el = (Element)n;
			id = el.getAttribute("ID").replaceAll("-.+", "");
			if(xmm.byName(n, "Name") != null ) {
				metaData = xmm.byName(n, "MetaData");
				if(metaData != null) {
					negocios = xmm.byAttributeValue(metaData, "AttributeID", "NegociosProveedor");
					if(negocios != null) {
						mapa = xmm.listImmediateChildElements(negocios);
						if(mapa != null) {
							nodos = mapa.get("Value");
							if(nodos != null) {
								values = new org.json.JSONArray();
								for(Node value : nodos) {
									negocio = value.getTextContent();
									negocio = "LIVERPOOL".equals(negocio) ? "LVP" : "SUBURBIA".equals(negocio) ? "SBB" : "MARKETPLACE".equals(negocio) ? "MKP" : "";
									if(!"".equals(negocio)) {
										values.put(negocio);
									}
								}
								object = new org.json.JSONObject().put("id", "'" + id + "'@'Party'");
								rows.put(new org.json.JSONObject().put("object", object).put("values", new org.json.JSONArray().put( values )));
								if(rows.length() == 250) {
									System.out.println( workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString()) );
									while(rows.length() > 0) {
										rows.remove(0);
									}
								}
							}
						}
					}
				}
			}
		}
		if(rows.length() > 0) {
			System.out.println( workshop.makeRequest("POST", "/list/LookupValue", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString()) );
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}

	}

	private static void checkIfInTemplate() {
		java.util.LinkedList<String> primeros = proveedoresPrimerosDeHoy();
		java.util.Map<String, java.util.Set<String>> mep = cargaPlantillasConGruposDeArticulo();
		java.util.Set<String> gpasList = mep.get("EU4-27320066");
		java.util.Map<String, java.util.Set<String>> gpasProveedor = gruposArticuloProveedor();
		java.util.Set<String> gpas = null;
		java.util.Set<String> segundos = new java.util.TreeSet<>();
		java.util.Set<String> terceros = new java.util.TreeSet<>();
		java.util.Set<String> cuartos = new java.util.TreeSet<>();
		for(String proveedor : primeros) {
			gpas = gpasProveedor.get(proveedor);
			if(gpas != null) {
				for(String gpa : gpas) {
					if(gpasList.contains(gpa)) {
//						System.out.println("Sí está. " + gpa + " (" + proveedor + ")");
						segundos.add(proveedor);
					}
				}
			}else {
				System.out.println("No gpas for " + proveedor);
			}
		}
		gpasList = mep.get("EU4-113578");
		for(String proveedor : segundos) {
			gpas = gpasProveedor.get(proveedor);
			if(gpas != null) {
				for(String gpa : gpas) {
					if(gpasList.contains(gpa)) {
//						System.out.println("Maleta y Pantallas: " + proveedor);
						terceros.add(proveedor);
					}
				}
			}else {
				System.out.println("No gpas for " + proveedor);
			}
		}
		gpasList = mep.get("EU4-28122113");
		for(String proveedor : terceros) {
			gpas = gpasProveedor.get(proveedor);
			if(gpas != null) {
				for(String gpa : gpas) {
					if(gpasList.contains(gpa)) {
//						System.out.println("Maleta, Pantallas y Base de Maquillaje: " + proveedor);
						cuartos.add(proveedor);
					}
				}
			}else {
				System.out.println("No gpas for " + proveedor);
			}
		}
		cuartos.forEach(ep -> System.out.println("Maleta, Pantallas y Base de Maquillaje: " + ep));
	}



	public static java.util.Map<String, java.util.Set<String>> gruposArticuloProveedor(){
		java.util.Map<String, java.util.Set<String>> gpasProveedor = new java.util.TreeMap<>();
		java.util.Set<String> gpas = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(
				"C:\\Users\\jcapizc\\Downloads\\Datos SAP QA - ECC - Relación Proveedor Grupo SAP QA.tsv"), java.nio.charset.Charset.forName("UTF-8")))){
			 String line = null;
			 String delimitador = "\"";
			 String separador = "\t";
			 String escape = "\\";
			 String[] pieces = null;
			 String grupoDeArticulos = null;
			 String proveedor = null;
			 String[] header = workshop.parseLine(br.readLine(), delimitador, separador, escape);
			 while((line = br.readLine()) != null) {
				 pieces = workshop.parseLine(line, delimitador, separador, escape);
				 grupoDeArticulos = pieces[3];
				 proveedor = pieces[1];
				 gpas = gpasProveedor.get(proveedor);
				 if(gpas == null) {
					 gpas = new java.util.TreeSet<>();
					 gpasProveedor.put(proveedor, gpas);
				 }
				 gpas.add(grupoDeArticulos);
			 }
		 }catch(java.io.IOException e) {
			 e.printStackTrace();
		 }
		return gpasProveedor;
	}

	public static java.util.LinkedList<String> proveedoresPrimerosDeHoy(){
		java.util.LinkedList<String> proveedoresPrimeros = new java.util.LinkedList<>();
		String delim = "\"";
		String sep = ",";
		String esc = "\\";
		String[] pieces = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\proveedores_ecc_qa_2.csv")))){
			String linea = null;
			br.readLine();
			while((linea = br.readLine()) != null) {
				pieces = workshop.parseLine(linea, delim, sep, esc);
				proveedoresPrimeros.addLast(pieces[0]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return proveedoresPrimeros;
	}

	public static java.util.Map<String, java.util.Set<String>> cargaPlantillasConGruposDeArticulo() {
		RESTWorkshop workshop = new RESTWorkshop();
		java.util.Map<String, java.util.Set<String>> pares = new java.util.TreeMap<>();
		java.util.Set<String> gruposDeArticulo = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(
				"C:\\Users\\jcapizc\\Downloads\\relationship product-supplier.xlsx - Suppliers apply.tsv")))){
			workshop.getRc().getHeader().put("Content-Type", "application/json");
			String linea = null;
			String delimitador = "\"";
			String separador = "\t";
			String escape = "\\";
			String[] pieces = null;
			br.readLine();
			org.json.JSONArray columns = new org.json.JSONArray();
			columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('MATKLLOV')"));
			org.json.JSONArray rows = new org.json.JSONArray();
			org.json.JSONObject request = new org.json.JSONObject();
			request.put("columns", columns);
			request.put("rows", rows);
			while((linea = br.readLine()) != null) {
				pieces = workshop.parseLine(linea, delimitador, separador, escape);
				if(pieces[8].startsWith("EU4-")) {
					gruposDeArticulo = pares.get(pieces[8]);
					if(gruposDeArticulo == null) {
						gruposDeArticulo = new java.util.TreeSet<>();
						pares.put(pieces[8], gruposDeArticulo);
					}
					gruposDeArticulo.add(pieces[2]);
				}
			}
			System.out.println("Working with: " + pares.size());
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return pares;
	}

	private static void cargaProveedores() throws SAXException, IOException, ParserConfigurationException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse(
				"C:\\opt\\LVP\\desorden\\Proveedores Raiz.xml"
//				"C:\\Users\\jcapizc\\Downloads\\step-472799325730140560-exported.xml"
				);
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("Classifications");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> suppliers = xmm.listImmediateChildElements(assetsRoot).get("Classification");

		org.json.JSONObject payload = new org.json.JSONObject();
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONArray values = new org.json.JSONArray().put(true);
		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Party.IsSupplier"));
		payload.put("columns", columns);
		payload.put("rows", rows);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		Element el = null;
		String id = null;
		for(Node n : suppliers) {
			el = (Element)n;
			id = el.getAttribute("ID").replaceAll("-.+", "");
			if(xmm.byName(n, "Name") != null /* && xmm.byName(n, "Name").getTextContent().contains("PUMA") */) {
				System.out.println(id + " - " + xmm.byName(n, "Name").getTextContent());
//				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'")).put("values", values));
//				System.out.println( workshop.makeRequest("POST", "/list/Party", qp, payload.toString()) );
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'")).put("values", values));
				if(rows.length() == 100) {
					System.out.println( workshop.makeRequest("POST", "/list/Party", qp, payload.toString()) );
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
//				while(rows.length() > 0)
//					rows.remove(0);
			}
		}
		if(rows.length() > 0 ) {
			System.out.println( workshop.makeRequest("POST", "/list/Party", qp, payload.toString()) );
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}

	}

	private static void definirAlcance() throws SAXException, IOException, ParserConfigurationException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse(
//				"C:\\Users\\jcapizc\\Downloads\\step-472799325730140560-exported.xml"
				"C:\\opt\\LVP\\desorden\\Proveedores Raiz.xml"
				);
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("Classifications");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> suppliers = xmm.listImmediateChildElements(assetsRoot).get("Classification");

		org.json.JSONObject column = new org.json.JSONObject().put("identifier", "");
		org.json.JSONArray columns = new org.json.JSONArray().put(column).put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
		org.json.JSONArray rows = null;
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		org.json.JSONArray lookupValues = new org.json.JSONArray();
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		int totalSize = 0;
		int currentIndex = 0;
		org.json.JSONArray values = null;

		java.util.Set<String> remaining = new java.util.TreeSet<>();
		qp.put("query", "LookupValueLang.Name(es) is empty");
		qp.put("lookup", "Party");
		qp.put("fields", "LookupValue.Code");
		qp.put("pageSize", "900");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				remaining.add(values.getString(0));
			}
		}while(currentIndex<totalSize);
		currentIndex = 0;
		System.out.println("Caught " + remaining.size() + " providers remaining (no name)");

		qp.clear();

		qp.put("fields", "Characteristic.Lookup->Lookup.Identifier");

		Element el;
		java.util.Map<String, java.util.LinkedList<Node>> content = null;
		java.util.Map<String, java.util.LinkedList<Node>> attributeLinkContent = null;
		java.util.LinkedList<Node> attributeLink = null;
		java.util.LinkedList<Node> attributeLinkValueFilter = null;
		java.util.LinkedList<Node> attributeLinkValues = null;

		String attributeLinkId = null;
		String lookup = null;

		java.util.Map<String, String> fieldLookup = new java.util.TreeMap<>();

		org.json.JSONObject payload = new org.json.JSONObject();
		payload.put("columns", columns);
		payload.put("rows", rowsPayload);

		java.util.Map<String, org.json.JSONObject> characteristicObjects = recoverSomeCharacteristics();
		System.out.println(characteristicObjects.size() + " collected.");
		java.util.Set<String> collectedBoys = new java.util.TreeSet<>();
		java.util.Map<String, String> empty = new java.util.TreeMap<>();

		String id = null;
		String name = null;
		int count = 0;

		java.util.regex.Matcher m = null;
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("lookup for given code: ([0-9A-Za-z_-]+)");

		org.json.JSONArray losQueNoTienenAcotacion = new org.json.JSONArray();

		System.out.println("Loading Party...");
		java.util.Set<String> currentProviders = currentProviders();
		System.out.println("Loading ZCOMALOV...");
		java.util.Set<String> currentZCOMALOV = currentZCOMALOV();

		for(Node n : suppliers) {
			count++;
//			if(count < 8231)
//				continue;
			el = (Element) n;
			id = el.getAttribute("ID").replaceAll("-.+", "");
//			if(id.equals("103955"))
//				System.out.println(id);
//			if( !"103955".equals(id) /* !remaining.contains(id) */ ) {
//				continue;
//			}else {
//				System.out.println("Este " + id);
//			}
			if(!currentProviders.contains(id)) {
				System.out.println("Skipping: " + id);
				continue;
			}
			if(true /* "111838".equals(id) */) {
				if(xmm.byName(n, "Name") != null) {
					name = xmm.byName(n, "Name").getTextContent();
					content = xmm.listImmediateChildElements(n);
					if(content != null) {
						attributeLink = content.get("AttributeLink");
						if(attributeLink != null) {
//							System.out.println("\t" + attributeLink.size() + " attribute links...");
							for(Node al : attributeLink) {
								attributeLinkContent = xmm.listImmediateChildElements(al);
								if(attributeLinkContent != null) {
									attributeLinkId = ((Element)al).getAttribute("AttributeID");
									lookup = fieldLookup.get(attributeLinkId);
									if(lookup == null) {
										qp.put("query", "Characteristic.Identifier equals \"" + attributeLinkId + "\"");
										response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
										rows = response.getJSONArray("rows");
										if(rows.length() > 0) {
											lookup = rows.getJSONObject(0).getJSONArray("values").getString(0);
											fieldLookup.put(attributeLinkId, lookup);
										}else {
											System.out.println("\nNow trying to create it (" + attributeLinkId + ")");
											if(!collectedBoys.contains(attributeLinkId)) {
												if(!characteristicObjects.containsKey(attributeLinkId)) {
													System.out.println("################################# Missing: " + attributeLinkId);
													continue;
												}else {
													System.out.println( workshop.makeRequest("POST", "/object/Characteristic", empty, characteristicObjects.get(attributeLinkId).getJSONObject("_data").toString()) );
													collectedBoys.add(attributeLinkId);
												}
											}
											qp.put("query", "Characteristic.Identifier equals \"" + attributeLinkId + "\"");
											response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
											rows = response.getJSONArray("rows");
											if(rows.length() > 0) {
												lookup = rows.getJSONObject(0).getJSONArray("values").getString(0);
												fieldLookup.put(attributeLinkId, lookup);
											}else {
												lookup = null;
												System.out.println("Field: " + attributeLinkId + " is not a lookup valued fields.");
												System.exit(3);
											}
										}
									}
									attributeLinkValueFilter = attributeLinkContent.get("ValueFilter");
									if(attributeLinkValueFilter != null && !attributeLinkValueFilter.isEmpty()) {
										attributeLinkValues = xmm.listImmediateChildElements( attributeLinkValueFilter.getFirst() ).get("Value");
										if(attributeLinkValues != null) {
											column.put("identifier", "LookupValueReference.LookupValues(" + lookup + ")");
											for(Node alv : attributeLinkValues) {
												if(((Element)alv).getAttribute("ID") == null) {
													System.out.println("PANIC: " + el.getAttribute("ID") + " did contain a specification without IDs");
													System.exit(1);
												}else {
													if(currentZCOMALOV.contains(((Element)alv).getAttribute("ID"))) {
														lookupValues.put(((Element)alv).getAttribute("ID"));
													}
												}
											}
											if(lookupValues.length() > 0) {
												rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + el.getAttribute("ID").replaceAll("-.+", "") + "'@'Party'")).put("values", new org.json.JSONArray().put(lookupValues.length() > 0 ? lookupValues : org.json.JSONObject.NULL).put(name).put(true)));
												boolean done = false;
												do {
													response = workshop.makeRequest("POST", "/list/LookupValue", qp, payload.toString());
													if(response.getJSONArray("entries").length() > 0) {
														m = p.matcher(response.getJSONArray("entries").getJSONObject(0).getString("message"));
														if(m.find()) {
															int f = -1;
															String mc = m.group(1);
															for(int v=0; v<lookupValues.length(); v++) {
																if(lookupValues.getString(v).equals(mc)) {
																	f = v;
																	break;
																}
															}
															if(f > -1) {
																lookupValues.remove(f);
																System.out.println(response.getJSONArray("entries").getJSONObject(0).getString("message"));
															}else {
																System.out.println("Couldn't determine missing value. " + response + "\n\t" + rowsPayload);
																System.exit(6);
															}
														}else {
															System.out.println("Couldn't determine missing value. " + response + "\n\t" + rowsPayload);
															System.exit(6);
														}
													}else {
														done = true;
													}
												}while(!done);
												rowsPayload.remove(0);
												lookupValues = new org.json.JSONArray();
											}
										}else {
											System.out.println("PANIC, malformed xml, expected Value list within ValueFilter element.");
											System.exit(2);
										}
									}else {
										losQueNoTienenAcotacion.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@'Party'")).put("values", new org.json.JSONArray().put(name).put(true)));
										if(losQueNoTienenAcotacion.length() == 250) {
											System.out.println( workshop.makeRequest("POST", "/list/LookupValue", empty, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", losQueNoTienenAcotacion).toString()) );
											while(losQueNoTienenAcotacion.length() > 0) {
												losQueNoTienenAcotacion.remove(0);
											}
										}
									}
								}
							}
						}else {
							losQueNoTienenAcotacion.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'@'Party'")).put("values", new org.json.JSONArray().put(name).put(true)));
							if(losQueNoTienenAcotacion.length() == 250) {
								System.out.println( workshop.makeRequest("POST", "/list/LookupValue", empty, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", losQueNoTienenAcotacion).toString()) );
								while(losQueNoTienenAcotacion.length() > 0) {
									losQueNoTienenAcotacion.remove(0);
								}
							}
						}
					}else {
						System.out.println("Empty boy: " + el.getAttribute("ID"));
					}
				}
			}
			if(count % 100 == 0) {
				System.out.print(100f*(((float)count) / ((float)suppliers.size()) ) + " %\t");
				if(count % 1000 == 0) {
					System.out.println();
				}
			}
		}
		if(losQueNoTienenAcotacion.length() > 0 ) {
			System.out.println( workshop.makeRequest("POST", "/list/LookupValue", empty, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)")).put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", losQueNoTienenAcotacion).toString()) );
			while(losQueNoTienenAcotacion.length() > 0) {
				losQueNoTienenAcotacion.remove(0);
			}
		}
		System.out.print(100f*(((float)count) / ((float)suppliers.size()) ) + " %\t");
	}

	private static java.util.Set<String> currentZCOMALOV(){
		java.util.Set<String> cp = new java.util.TreeSet<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "ZCOMALOV");
		qp.put("query", "LookupValue.IsActive = true");
		qp.put("fields", "LookupValue.Code");
		qp.put("pageSize", "300");

		int currentIndex = 0;
		int totalSize = 0;

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				cp.add(values.getString(0));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		return cp;
	}

	private static java.util.Set<String> currentProviders(){
		java.util.Set<String> cp = new java.util.TreeSet<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("lookup", "Party");
		qp.put("query", "LookupValue.IsActive = true");
		qp.put("fields", "LookupValue.Code");
		qp.put("pageSize", "300");

		int currentIndex = 0;
		int totalSize = 0;

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				cp.add(values.getString(0));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		return cp;
	}

	private static java.util.Map<String, org.json.JSONObject> recoverSomeCharacteristics() {

		java.util.Map<String, org.json.JSONObject> characteristicObjects = new java.util.TreeMap<>();

		org.json.JSONObject json = null;

		java.util.regex.Matcher m = null;
		java.util.regex.Pattern p = java.util.regex.Pattern.compile("'(.+)'");
		String externalId = null;

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\Characteristic_objects.dat")))){
			String line = null;
			while((line = br.readLine()) != null) {
				if(!"null".equals(line)) {
					try {
						json = new org.json.JSONObject(line);
						externalId = json.getJSONObject("_entityItem").getString("_externalId");
						m = p.matcher(externalId);
						if(m.find()) {
							externalId = m.group(1);
							characteristicObjects.put(externalId, json);
						}else {
							System.out.println("Problem identifying id for: " + json);
							System.exit(4);
						}
					}catch(org.json.JSONException e) {
						System.out.println("Invalid JSONObject: " + line);
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}

		return characteristicObjects;
	}

	// De la entidad Party a Lista de Valores.
	private static void listaDeValores() throws SAXException, IOException, ParserConfigurationException{

		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeSuppliers", "true");
		qp.put("fields", "Party.ExternalIdentifier,Party.Name");
		qp.put("pageSize", "750");

		org.json.JSONObject payload = new org.json.JSONObject().put("columns", columns);

		int totalSize = 0;
		int currentIndex = 0;

		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Party/all", qp, null);
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(1) + "'@'Party'")).put("values", new org.json.JSONArray().put(values.getString(0))));
				if(rowsPayload.length() == 250) {
					response = workshop.makeRequest("POST", "/list/LookupValue", qp, payload.put("rows", rowsPayload).toString());
					System.out.println(response);
					while(rowsPayload.length() > 0) {
						rowsPayload.remove(0);
					}
				}
			}
			if(rowsPayload.length() > 0) {
				response = workshop.makeRequest("POST", "/list/LookupValue", qp, payload.put("rows", rowsPayload).toString());
				System.out.println(response);
				while(rowsPayload.length() > 0) {
					rowsPayload.remove(0);
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

	}

	/****
	 * 
	 * 
	 * 
	 * Esto carga la entidad Party, no la lookup Party
	 * 
	 * 
	 *****/
	private static void crearPartyProveedores() throws SAXException, IOException, ParserConfigurationException {

//		RESTWorkshop rw = new RESTWorkshop();
//		rw.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
//		java.util.Map<String, String> params = new java.util.TreeMap<>();
//		params.put("query", "not Party.ExternalIdentifier is empty and not Party.Name equals \"Heiler Product Manager\"");
//		System.out.println( rw.makeRequest("DELETE", "/list/Party/bySearch", params, null) );
//		System.exit(0);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse(
//				"C:\\Users\\jcapizc\\Downloads\\step-472799325730140560-exported.xml"
				"C:\\opt\\LVP\\desorden\\Proveedores Raiz.xml"
				);
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("Classifications");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> suppliers = xmm.listImmediateChildElements(assetsRoot).get("Classification");

		org.json.JSONArray columns = new org.json.JSONArray()
				.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('BusinessQualified')"))
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('')"))
				.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('')"))
				;
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		Element el;
		java.util.LinkedList<Node> names = null;
		java.util.LinkedList<Node> metaDatas = null;
		Node multiValue = null;
		Node nameNode = null;
		Node emailNode = null;
		Node marketPlaceNode = null;
		String name = null;
		String email = null;
		boolean marketPlace = false;
		boolean provider = false;

		String id = null;

		for(Node n : suppliers) {
			el = (Element) n;
			id = el.getAttribute("ID");
			metaDatas = xmm.listImmediateChildElements(n).get("MetaData");
			if(metaDatas != null && !metaDatas.isEmpty()) {
				emailNode = xmm.byAttributeValue( metaDatas.getFirst(), "AttributeID", "EmailProveedor" );
				if(emailNode != null) {
					email = emailNode.getTextContent();
				} else {
					email = "";
				}
				multiValue = xmm.byName(metaDatas.getFirst(), "MultiValue");
				if(multiValue != null) {
					marketPlaceNode = xmm.byAttributeValue(multiValue, "ID", "ART. MARKETPLACE");
					marketPlace = marketPlaceNode != null;
					provider = 
							!marketPlace && xmm.listImmediateChildElements(multiValue).size() > 0 ? true 
									: marketPlace && xmm.listImmediateChildElements(multiValue).size() > 1;
				}
			} else {
				email = "";
			}
			names = xmm.listImmediateChildElements(n).get("Name");
			if(names != null && !names.isEmpty()) {
				nameNode = names.getFirst();
				name = nameNode.getTextContent();
			}else {
				name = "";
			}
			if(!provider && !marketPlace) {
				System.out.println("PANIC. " + el.getAttribute("ID") + "; " + name);
				continue;
			}
			rows.put(new org.json.JSONObject().put("object", 
					new org.json.JSONObject()
					.put("id", "'" + id.replaceAll("-.+", "") + "'")).put("values", new org.json.JSONArray().put( name ).put( id ).put( email ).put(provider).put(marketPlace)));
			if(rows.length() == 250) {
				System.out.println("Requesting: " + workshop.getBaseUrl());
				response = workshop.makeRequest("POST", "/list/LookupValue", qp, 
						new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
				System.out.println(response);
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
			provider = false;
		}
		if(rows.length() > 0) {
			response = workshop.makeRequest("POST", "/list/Party", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
			System.out.println(response);
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}

	}

	/****
	 * 
	 * 
	 * 
	 * Esto carga la entidad Party, no la lookup Party
	 * 
	 * 
	 *****/
	private static void crearProveedores() throws SAXException, IOException, ParserConfigurationException {

//		RESTWorkshop rw = new RESTWorkshop();
//		rw.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
//		java.util.Map<String, String> params = new java.util.TreeMap<>();
//		params.put("query", "not Party.ExternalIdentifier is empty and not Party.Name equals \"Heiler Product Manager\"");
//		System.out.println( rw.makeRequest("DELETE", "/list/Party/bySearch", params, null) );
//		System.exit(0);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document doc;
		doc = builder.parse(
//				"C:\\Users\\jcapizc\\Downloads\\step-472799325730140560-exported.xml"
				"C:\\opt\\LVP\\desorden\\Proveedores Raiz.xml"
				);
		doc.getDocumentElement().normalize();

		Element rootElement = doc.getDocumentElement();
		java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
		java.util.LinkedList<Node> lst = a.get("Classifications");
		Node assetsRoot = lst.getFirst();
		java.util.LinkedList<Node> suppliers = xmm.listImmediateChildElements(assetsRoot).get("Classification");

		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Party.ExternalIdentifier")).put(new org.json.JSONObject().put("identifier", "Party.Acronym")).put(new org.json.JSONObject().put("identifier", "Party.EmailProveedor")).put(new org.json.JSONObject().put("identifier", "Party.IsProvider")).put(new org.json.JSONObject().put("identifier", "Party.IsSupplier"));
		org.json.JSONArray rows = new org.json.JSONArray();
		org.json.JSONObject response = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();

		Element el;
		java.util.LinkedList<Node> names = null;
		java.util.LinkedList<Node> metaDatas = null;
		Node multiValue = null;
		Node nameNode = null;
		Node emailNode = null;
		Node marketPlaceNode = null;
		String name = null;
		String email = null;
		boolean marketPlace = false;
		boolean provider = false;

		String id = null;

		for(Node n : suppliers) {
			el = (Element) n;
			id = el.getAttribute("ID");
			metaDatas = xmm.listImmediateChildElements(n).get("MetaData");
			if(metaDatas != null && !metaDatas.isEmpty()) {
				emailNode = xmm.byAttributeValue( metaDatas.getFirst(), "AttributeID", "EmailProveedor" );
				if(emailNode != null) {
					email = emailNode.getTextContent();
				} else {
					email = "";
				}
				multiValue = xmm.byName(metaDatas.getFirst(), "MultiValue");
				if(multiValue != null) {
					marketPlaceNode = xmm.byAttributeValue(multiValue, "ID", "ART. MARKETPLACE");
					marketPlace = marketPlaceNode != null;
					provider = !marketPlace && xmm.listImmediateChildElements(multiValue).size() > 0 ? true : marketPlace && xmm.listImmediateChildElements(multiValue).size() > 1;
				}
			} else {
				email = "";
			}
			names = xmm.listImmediateChildElements(n).get("Name");
			if(names != null && !names.isEmpty()) {
				nameNode = names.getFirst();
				name = nameNode.getTextContent();
			}else {
				name = "";
			}
			if(!provider && !marketPlace) {
				System.out.println("PANIC. " + el.getAttribute("ID") + "; " + name);
				continue;
			}
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id.replaceAll("-.+", "") + "'")).put("values", new org.json.JSONArray().put( name ).put( id ).put( email ).put(provider).put(marketPlace)));
			if(rows.length() == 250) {
				System.out.println("Requesting: " + workshop.getBaseUrl());
				response = workshop.makeRequest("POST", "/list/Party", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
				System.out.println(response);
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
			provider = false;
		}
		if(rows.length() > 0) {
			response = workshop.makeRequest("POST", "/list/Party", qp, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
			System.out.println(response);
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}

	}

	private static void prueba() {
		org.json.JSONObject response = null;
		java.util.Map<String, String> queryParameters = new java.util.TreeMap<>();
		queryParameters.put("dictionaryProxy", "'Informatica_DQ_Content/Dictionaries/General/color_base_infa.dic'");
		queryParameters.put("query", "");
		queryParameters.put("fields", "");
		org.json.JSONArray columns = new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "StandardizationValue.LookupValue"));
		org.json.JSONArray rows = new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'Air superiority blue'@'Informatica_DQ_Content/Dictionaries/General/color_base_infa.dic'")).put("values", new org.json.JSONArray().put(new org.json.JSONObject().put("id", "'I654'@'ZCOMALOV'" ) )));
		response = workshop.makeRequest("POST", "/list/StandardizationValue", queryParameters, new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
		System.out.println(response);
	}

}
