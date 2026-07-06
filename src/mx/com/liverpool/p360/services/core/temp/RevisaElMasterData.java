package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;

import org.json.JSONException;

import mx.com.liverpool.p360.services.core.RestClient;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import mx.com.liverpool.p360.services.xmlutils.XMLMisc;

public class RevisaElMasterData {

	private static final XMLMisc xmm = new XMLMisc();
	private static final String encoded = "cmVzdDpoZWlsZXI=";
	private static final String baseUrl = "https://webctep360dev.liverpool.com.mx/rest/V2.0";
	private static final RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);

	public static void main(String[] args) throws ServiceUnavailableException {

		long init = System.currentTimeMillis();


		try {
			System.out.println( rc.getRequest("POST", baseUrl + "/list/Product2G", new org.json.JSONObject()
					.put("columns", new org.json.JSONArray().put( new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus")) )
					.put("rows",
							new org.json.JSONArray()
							.put(
									new org.json.JSONObject()
										.put("object", new org.json.JSONObject().put("id", "'1698767480717828'@'MASTER'"))
										.put("values", new org.json.JSONArray().put(1011))
								)
							.put(
									new org.json.JSONObject()
										.put("object", new org.json.JSONObject().put("id", "'1698767480717833'@'MASTER'"))
										.put("values", new org.json.JSONArray().put(1011))
								)
					)
					.toString())  );
		} catch (JSONException 
				| IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
		/*
		int tz = 0;
		int c = 0;
		org.json.JSONArray nr = new org.json.JSONArray();
		org.json.JSONArray nru = new org.json.JSONArray();
		org.json.JSONArray nra = new org.json.JSONArray();
		try {
			for(int a=0; a<3; a++) {
				do { // Solo quitar msj y rmum para que a lo demás se le desactive
					String rr = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?"
							+ "query=" + java.net.URLEncoder.encode(
//									  "Characteristic.Identifier wildcard \"rem_%\" or "
//									+ "Characteristic.Identifier wildcard \"rma_%\" or "
//									+ "Characteristic.Identifier wildcard \"rmum_%\" or "
//									+ "Characteristic.Identifier wildcard \"rrd_%\" or "
//									+ "Characteristic.Identifier wildcard \"rre_%\" or "
//									+ "Characteristic.Identifier wildcard \"msj_%\" or "
//									+ "Characteristic.Identifier wildcard \"mdr_%\" or "
									"Characteristic.Identifier wildcard \"%_Rechazo\"",
									"UTF-8")
							+ "&pageSize=500", null);
					System.out.println(rr);
					org.json.JSONObject o = new org.json.JSONObject(rr);
					tz = o.getInt("totalSize");
					System.out.println(tz);
					org.json.JSONArray rws = o.getJSONArray("rows");
					for(int i=0; i<rws.length(); i++) {
						c++;
						nr.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", rws.getJSONObject(i).getJSONObject("object").getString("id"))).put("values", new org.json.JSONArray().put(false)));
						nru.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", rws.getJSONObject(i).getJSONObject("object").getString("id"))).put("values", new org.json.JSONArray().put(100)));
						nra.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", rws.getJSONObject(i).getJSONObject("object").getString("id"))).put("values", new org.json.JSONArray().put(true)));
					}
					System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", nr).toString()) );
//					System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.UpperBound"))).put("rows", nru).toString()) );
//					System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", nra).toString()) );
					while(nr.length() > 0) {
						nr.remove(0);
						nru.remove(0);
						nra.remove(0);
					}
				}while(c < tz);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

		System.exit(0);
		*/

		/* This block is used to read from xml file the attributes that are at product root level.
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc;
			doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
			doc.getDocumentElement().normalize();
			Element rootElement = doc.getDocumentElement();

			java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
			java.util.LinkedList<Node> lst = a.get("Products");
			Node productsRoot = lst.getFirst();
			Node pphHierarcyRoot = xmm.byAttributeValue(productsRoot, "ID", "ProductsSuppliersPortal");

			java.util.LinkedList<Node> primerosNodos = xmm.listImmediateChildElements(pphHierarcyRoot).get("AttributeLink");

			int cnt = 0;
			for(Node n : primerosNodos) {
				cnt++;
				System.out.println(((Element)n).getAttribute("AttributeID"));
			}
			System.out.println("Total: " + cnt);

		} catch (SAXException | IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.print("Done. " + formatMillis(System.currentTimeMillis() - init));
		System.exit(0);
		*/

		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = -1;

		/*
		while(totalSize != 0)
			try{
				System.out.println("Pitutsi");
				do {
					rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?query=" // Characteristic.Category->LookupValue.Code contains \"Master Data\" and
							+ java.net.URLEncoder.encode("(Characteristic.Identifier wildcard \"rem_%\" or Characteristic.Identifier wildcard \"rre_%\" or Characteristic.Identifier wildcard \"rrd_%\" or Characteristic.Identifier wildcard \"rma_%\" or Characteristic.Identifier wildcard \"mdr_%\") and not Characteristic.ValueProviderId is empty", "UTF-8")
							+ "&pageSize=60"
							+ "&startIndex=" + currentIndex
							, null);
					response = new org.json.JSONObject(rawResponse);
					totalSize = response.getInt("totalSize");
					System.out.println("Total Size: " + totalSize);
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						currentIndex++;
						values = rows.getJSONObject(i).getJSONArray("values");
						values.put("");
					}
					rawResponse = rc.getRequest("POST", baseUrl + "/list/Characteristic", new org.json.JSONObject()
							.put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.ValueProviderId")))
							.put("rows", rows).toString());
					try {
						System.out.println( new org.json.JSONObject(rawResponse).getJSONObject("counters") );
					}catch(org.json.JSONException e) {
						System.out.println(rawResponse);
					}
				}while(currentIndex < totalSize);
				currentIndex = 0;
			}catch(Exception e) {
				e.printStackTrace();
			}
		System.exit(0);
		*/

		/* Este bloque de código se usa para mover las características de la categoría Mast Data a Holder
		org.json.JSONArray losrows = new org.json.JSONArray();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\master_data_characteristics_reject")))){
			pw.println(new org.json.JSONArray().put("Identifier").put("IsActive").put("DataType").put("Entities").put("Lookup"));
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?query="
						+ java.net.URLEncoder.encode("Characteristic.Category->LookupValue.Code contains \"Master Data\" and Characteristic.Identifier wildcard \"%_Rechazo\"", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "Characteristic.Identifier,"
								+ "Characteristic.IsActive,"
								+ "Characteristic.DataType,"
								+ "Characteristic.Entities,"
								+ "Characteristic.Lookup->Lookup.Identifier"
							, "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=500"
						+ "&startIndex=" + currentIndex
						, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				System.out.println("Total Size: " + totalSize);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					losrows.put(new org.json.JSONObject().put("object", rows.getJSONObject(i).getJSONObject("object")).put("values", new org.json.JSONArray().put("MasterData_Rechazo")));
				}
				System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.Category"))).put("rows", losrows).toString()) );
			}while(currentIndex < totalSize);
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}
		*/

		/* Busca las características dentro del archivo de la pph de step para saber si hacen referencia a alguna lookup.
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc;
			doc = builder.parse("C:\\Users\\jcapizc\\Downloads\\step-4611212669058297112-exported.xml");
			doc.getDocumentElement().normalize();
			Element rootElement = doc.getDocumentElement();
			java.util.Map <String, java.util.LinkedList <Node>> a = xmm.listImmediateChildElements(rootElement);
			java.util.LinkedList<Node> lst = a.get("AttributeList");
			Node listOfValuesRoot = lst.getFirst();
			java.util.LinkedList<Node> listOfAttributes = xmm.listImmediateChildElements(listOfValuesRoot).get("Attribute");

			String lkpId = null;
			Element en = null;
			Element lookupName = null;
			java.util.LinkedList<Node> lookupValues = null;
			org.json.JSONArray losmemejes = new org.json.JSONArray();

			java.util.ArrayList<String> attributesOfInterest = new java.util.ArrayList<>( java.util.Arrays.asList(("MesdeEntregadeMercancia\r\n"
					+ "TipoConjuntoLookTend").split("\\r\\n")) );
			System.out.println(attributesOfInterest);
			for(Node n : listOfAttributes) {
				if(attributesOfInterest.contains( ((Element)n).getAttribute("ID")) ) {
					en = (Element)n;
					Node n0 = xmm.byName(n, "ListOfValueLink");
					if(n0 != null) {
						System.out.println(((Element)n).getAttribute("ListOfValueID"));
					}else {
						n0 = xmm.byName(n, "Validation");
						if(n0 != null) {
							System.out.println("Had validation");
						}else {
							System.out.println("Mesh");
						}
					}
				}
			}

			System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.Category"))).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'TipoConjuntoLookTend'")).put("values", new org.json.JSONArray().put("Holder"))).put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'MesdeEntregadeMercancia'")).put("values", new org.json.JSONArray().put( "Holder" )))).toString()) );

		} catch (SAXException | IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.exit(0);
		*/

		/*
		org.json.JSONArray unosRows = new org.json.JSONArray();
		java.util.LinkedList<String> unosIds = new java.util.LinkedList<>();

		try{
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?query=" // Characteristic.Category->LookupValue.Code contains \"Master Data\" and
						+ java.net.URLEncoder.encode(" Characteristic.Category in (\"Holder\",\"Holder2\",Holder3,Holder4,Holder5,Holder6)", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "Characteristic.Identifier,"
								+ "Characteristic.IsActive,"
								+ "Characteristic.DataType,"
								+ "Characteristic.Entities,"
								+ "Characteristic.Lookup->Lookup.Identifier"
							, "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=500"
						+ "&startIndex=" + currentIndex
						, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				System.out.println("Total Size: " + totalSize);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					unosIds.addLast(values.getString(0));
				}
			}while(currentIndex < totalSize);
			for(String identifier : unosIds) {
				unosRows.put(new org.json.JSONObject().put("object" , "'" + identifier + "'").put("values", new org.json.JSONArray().put("Master Data")));
			}
			System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.Category"))).put("rows", unosRows).toString()) );
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}

		System.exit(0);
		*/

		java.util.ArrayList<String> heavyOnes = new java.util.ArrayList<>(java.util.Arrays.asList(("ZCOMALOV\r\n"
				+ "ItemGroupProductLOV\r\n"
				+ "BrandLOV\r\n"
				+ "PE000LOV\r\n"
				+ "ItemGroupLOV\r\n"
				+ "googleTaxonomyMap_LOV\r\n"
				+ "MATKLLOV2\r\n"
				+ "MATKLLOV\r\n"
				+ "TamanoUnicoLOV\r\n"
				+ "TM001LOV\r\n"
				+ "LicenciaPersonajeLOV\r\n"
				+ "TDI01LOV\r\n"
				+ "TipoDeEquipoLOV\r\n"
				+ "PPH_L4_Templates\r\n"
				+ "CharacteristicCategories\r\n"
				+ "ItemGroupConProductoSBBLOV\r\n"
				+ "FranquiciaLOV\r\n"
				+ "TemplatesLOV\r\n"
				+ "NumeroDePiezasQueIncluyeLOV\r\n"
				+ "PaginasLOV\r\n"
				+ "ProfundidadLOV\r\n"
				+ "PesoDelProductoConSoporteLOV\r\n"
				+ "PesosDelProductoSinSoporteLOV\r\n"
				+ "PesoDeLaMesaLOV\r\n"
				+ "TML01LOV\r\n"
				+ "SB_0002LOV\r\n"
				+ "AE023LOV\r\n"
				+ "WHERRLOV\r\n"
				+ "ZZLICLOV\r\n"
				+ "DiametroDelProductoLOV\r\n"
				+ "AR003LOV\r\n"
				+ "TD001LOV\r\n"
				+ "MATKLLOV_S4H\r\n"
				+ "BRAND_IDLOV_S4H\r\n"
				+ "FormaLOV\r\n"
				+ "AE249LOV\r\n"
				+ "TDA01LOV\r\n"
				+ "MaterialLOV\r\n"
				+ "TAC01LOV\r\n"
				+ "EquipoCompatibleLOV\r\n"
				+ "GeneroliterarioLOV\r\n"
				+ "AE019LOV\r\n"
				+ "AE350LOV\r\n"
				+ "TI001LOV\r\n"
				+ "LargoMesaLOV\r\n"
				+ "TC001LOV\r\n"
				+ "LongitudLOV\r\n"
				+ "ProfundidadDeLaBaseLOV\r\n"
				+ "SB_T_HARDLINELOV\r\n"
				+ "PotenciaLOV\r\n"
				+ "AE316LOV\r\n"
				+ "AlturaMaximaLOV\r\n"
				+ "AltoDeLaBaseLOV\r\n"
				+ "TecnologiaDeTenisLOV\r\n"
				+ "EditorialLOV\r\n"
				+ "SB_THARDLINELOV\r\n"
				+ "AE211LOV").split("\\r\\n")));

		java.util.LinkedList<String> toAvoid = new java.util.LinkedList<>();

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\to_avoid.txt")))){
			String line = null;
			while((line = br.readLine()) != null) {
				toAvoid.addLast(line);
			}
		}catch(java.io.IOException e) { e.printStackTrace(); }

		org.json.JSONArray rowsPayload0 = new org.json.JSONArray();
		org.json.JSONArray rowsPayloadToDisable0 = new org.json.JSONArray();
		org.json.JSONArray rowsPayloadToEnable0 = new org.json.JSONArray();
		for(int a = 0; a < 6; a++) {
			try{
				int cnt = 0;
				for(String tav : toAvoid) {
					rowsPayload0.put(
							new org.json.JSONObject()
								.put("object", new org.json.JSONObject().put("id", "'" + tav + "'"))
								.put("values", new org.json.JSONArray().put("")));
					rowsPayloadToDisable0.put(
							new org.json.JSONObject()
							.put("object", new org.json.JSONObject().put("id", "'" + tav + "'"))
							.put("values", new org.json.JSONArray().put(false)));
					rowsPayloadToEnable0.put(
							new org.json.JSONObject()
							.put("object", new org.json.JSONObject().put("id", "'" + tav + "'"))
							.put("values", new org.json.JSONArray().put(true)));
					if(rowsPayload0.length() == 500) {
						if(a < 2) {
							System.out.println("Disabling");
							System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", (new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", rowsPayloadToDisable0)).toString() ) );
						}else if(a < 4 ) {
							System.out.println("Updating");
							System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", (new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.ValueProviderId"))).put("rows", rowsPayload0)).toString() ) );
						}else if(a < 6) {
							System.out.println("Enabling");
							System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", (new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", rowsPayloadToEnable0)).toString() ) );
						}
						while(rowsPayload0.length() > 0) {
							rowsPayload0.remove(0);
							rowsPayloadToDisable0.remove(0);
							rowsPayloadToEnable0.remove(0);
						}
						cnt += 500;
						System.out.println(cnt + "/" + toAvoid.size());
					}
				}
				if(rowsPayload0.length() > 0) {
					if(a < 2) {
						System.out.println("Disabling");
						System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", (new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", rowsPayloadToDisable0)).toString() ) );
					}else if(a < 4 ) {
						System.out.println("Updating");
						System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", (new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.ValueProviderId"))).put("rows", rowsPayload0)).toString() ) );
					}else if(a < 6) {
						System.out.println("Enabling");
						System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", (new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", rowsPayloadToEnable0)).toString() ) );
					}
					while(rowsPayload0.length() > 0) {
						rowsPayload0.remove(0);
						rowsPayloadToDisable0.remove(0);
						rowsPayloadToEnable0.remove(0);
					}
				}
				currentIndex = 0;
			}catch(Exception e) {
				e.printStackTrace();
			}
		}

		System.exit(0);

		/*
		java.util.LinkedList<String> templates = new java.util.LinkedList<>();
		try{
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/StructureGroup/bySearch?"
						+ "&query="
						+ java.net.URLEncoder.encode("StructureGroup.Identifier wildcard \"EU4-%\"", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode("StructureGroup.Identifier", "UTF-8")
						+ "&structure=PrimaryProductTaxonomy"
						+ "&metaData=true"
						+ "&pageSize=1000"
						+ "&startIndex=" + currentIndex
						, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					templates.addLast(values.getString(0));
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}

		java.util.Set<String> chars = new java.util.TreeSet<>();
		try{
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/StandardizationValue/bySearch?dictionaryProxy='"
						+ java.net.URLEncoder.encode("ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla", "UTF-8") + "'"
						+ "&query="
						+ java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and StandardizationValue.Property->LookupValue.Code equals ListOfValues", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "StandardizationValue.Characteristic->Characteristic.Identifier"
							, "UTF-8")
						+ "&pageSize=1000"
						+ "&startIndex=" + currentIndex
						, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					chars.add(values.getString(0));
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\characteristics_with_filters_within_extended_metadata")))){ chars.forEach(ch -> pw.println(ch)); }catch(IOException e) { e.printStackTrace(); }
		System.out.println("Found: " + chars.size() + " characteristics tied to lookups");
		java.util.Set<String> winners = new java.util.TreeSet<>();
		for(String chr : chars) {
			try{
				rawResponse = rc.getRequest("GET", baseUrl + "/list/StandardizationValue/bySearch?dictionaryProxy='"
						+ java.net.URLEncoder.encode("ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla", "UTF-8") + "'"
						+ "&query="
						+ java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and StandardizationValue.Property->LookupValue.Code equals ListOfValuesFilter and StandardizationValue.Characteristic->Characteristic.Identifier equals " + chr, "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "StandardizationValue.Characteristic->Characteristic.Identifier"
							, "UTF-8")
						+ "&pageSize=2"
						+ "&startIndex=" + currentIndex
						, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				if(totalSize == 0) {
					winners.add(chr);
				}
				currentIndex = 0;
			}catch(Exception e) {
				e.printStackTrace();
			}
		}

		System.out.println("*****");
		winners.forEach(System.out::println);

		System.exit(0);

		*/


		/*
		java.util.Set<String> characteristicsFound = new java.util.TreeSet<>();
		try{
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/StandardizationValue/bySearch?dictionaryProxy='"
						+ java.net.URLEncoder.encode("ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla", "UTF-8") + "'"
						+ "&query="
						+ java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and StandardizationValue.Property->LookupValue.Code equals ListOfValues", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "StandardizationValue.Characteristic->Characteristic.Identifier"
							, "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=500"
						+ "&startIndex=" + currentIndex
						, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					characteristicsFound.add(values.getString(0));
				}
			}while(currentIndex < totalSize);
			int a = 0;
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}

		java.util.Set<String> characteristicsWithFilter = new java.util.TreeSet<>();
		try{
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/StandardizationValue/bySearch?dictionaryProxy='"
						+ java.net.URLEncoder.encode("ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla", "UTF-8") + "'"
						+ "&query="
						+ java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and StandardizationValue.Property->LookupValue.Code equals ListOfValuesFilter", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "StandardizationValue.Characteristic->Characteristic.Identifier"
							, "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=500"
						+ "&startIndex=" + currentIndex
						, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					characteristicsWithFilter.add(values.getString(0));
				}
			}while(currentIndex < totalSize);
			int a = 0;
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}
		System.out.println("To avoid");
		characteristicsWithFilter.forEach(System.out::println);
		System.out.println("***");
		*/

		java.util.Set<String> toAvoid2 = new java.util.TreeSet<>( java.util.Arrays.asList( ("AE362\r\n"
				+ "AE485\r\n"
				+ "AE486\r\n"
				+ "AreaDeLimpiezaVaD\r\n"
				+ "CantidadDeTazasVaD\r\n"
				+ "ChildrenCharacteristicsAtt\r\n"
				+ "ClothesFitAtt\r\n"
				+ "ColoursLiverpoolAtt\r\n"
				+ "CompaniaCelularVaD\r\n"
				+ "CorteCinturaVaD\r\n"
				+ "Currency\r\n"
				+ "DisciplineAtt\r\n"
				+ "DisenoVaD\r\n"
				+ "DuracionBateriaVaD\r\n"
				+ "EdadRecomendadaVaD\r\n"
				+ "EliminaVaD\r\n"
				+ "EquipoVaD\r\n"
				+ "EsPortatilKVaD\r\n"
				+ "EscoteVaD\r\n"
				+ "FSH_ID\r\n"
				+ "FlavorAtt\r\n"
				+ "FotoTomadaLiverpool\r\n"
				+ "HeatingRateAtt\r\n"
				+ "IncluyeTapaVaD\r\n"
				+ "IndicadordeImpuesto\r\n"
				+ "LargoDeMangaVaD\r\n"
				+ "LavableVAD\r\n"
				+ "LineAtt\r\n"
				+ "LockAtt\r\n"
				+ "LongAtt\r\n"
				+ "Material2VaD\r\n"
				+ "MultiposicionVaD\r\n"
				+ "NivelDeCalentamientoVaD\r\n"
				+ "NivelDeConfortVaD\r\n"
				+ "NumberOf PiecesAtt\r\n"
				+ "NumeroDeCompartimientosVaD\r\n"
				+ "NumeroDePiezasQueIncluyeVaD\r\n"
				+ "OccasionAtt\r\n"
				+ "PackingAtt\r\n"
				+ "PatternAtt\r\n"
				+ "PresentacionVaD\r\n"
				+ "PresentationAtt\r\n"
				+ "ProductTypeSAP\r\n"
				+ "ProductTypeSAPTEMP\r\n"
				+ "ProductTypeSAPTEMPSBB\r\n"
				+ "ReversibleVaD\r\n"
				+ "ShapeAtt\r\n"
				+ "Status\r\n"
				+ "TAXESS4H\r\n"
				+ "TipoDeEquipoVaD\r\n"
				+ "TipoDeSoporteVaD\r\n"
				+ "TipoDeTirantesVaD\r\n"
				+ "UnidadDeMedidaVolumen\r\n"
				+ "UsoRecomendadoVaD\r\n"
				+ "VoltajeVaD\r\n"
				+ "WeightLiftedAtt").split("\\r\\n") ) );

		org.json.JSONArray rowsPayload = new org.json.JSONArray();
		org.json.JSONArray rowsPayloadToDisable = new org.json.JSONArray();
		org.json.JSONArray rowsPayloadToEnable = new org.json.JSONArray();
		try{
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?query="
						+ java.net.URLEncoder.encode("Characteristic.Lookup->Lookup.Identifier in (" + String.join(",", heavyOnes) + ") and not Characteristic.ValueProviderId is empty", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "Characteristic.Identifier,"
								+ "Characteristic.IsActive,"
								+ "Characteristic.DataType,"
								+ "Characteristic.Entities,"
								+ "Characteristic.Lookup->Lookup.Identifier,"
								+ "Characteristic.ValueProviderId"
							, "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=500"
						+ "&startIndex=" + currentIndex
						, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				System.out.println("Total Size: " + totalSize);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if(!toAvoid2.contains(values.getString(0))) {
						rowsPayload.put(
								new org.json.JSONObject()
									.put("object", rows.getJSONObject(i).getJSONObject("object"))
									.put("values", new org.json.JSONArray().put("")));
						rowsPayloadToDisable.put(
								new org.json.JSONObject()
								.put("object", rows.getJSONObject(i).getJSONObject("object"))
								.put("values", new org.json.JSONArray().put(false)));
						rowsPayloadToEnable.put(
								new org.json.JSONObject()
								.put("object", rows.getJSONObject(i).getJSONObject("object"))
								.put("values", new org.json.JSONArray().put(true)));
					}else {
						System.out.println("Avoiding this: " + values.getString(0)
						);
					}
				}
			}while(currentIndex < totalSize);
			org.json.JSONObject req = null;
			System.out.println("Disabling");
			System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", (req = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", rowsPayloadToDisable)).toString() ) );
			System.out.println("Updating");
			System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", (req = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.ValueProviderId"))).put("rows", rowsPayload)).toString() ) );
			System.out.println("Enabling");
			System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", (req = new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", rowsPayloadToEnable)).toString() ) );
			System.out.println("Request was: " + req);
			int a = 0;
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}

		System.exit(0);

		try{
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?query=" // Characteristic.Category->LookupValue.Code contains \"Master Data\" and
						+ java.net.URLEncoder.encode("Characteristic.Category->LookupValue.Code in (Holder3) ", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "Characteristic.Identifier,"
								+ "Characteristic.IsActive,"
								+ "Characteristic.DataType,"
								+ "Characteristic.Entities,"
								+ "Characteristic.Lookup->Lookup.Identifier"
							, "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=500"
						+ "&startIndex=" + currentIndex
						, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				System.out.println("Total Size: " + totalSize);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if(!"".equals( values.getString(4) ) ) {
						if(heavyOnes.contains(values.getString(4))) {
							System.out.println(values.getString(0) + " - " + values.getString(4));
						}
					}
				}
			}while(currentIndex < totalSize);
			int a = 0;
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}

		System.exit(0);

		// ZCOMALOV,ItemGroupProductLOV,BrandLOV,PE000LOV,ItemGroupLOV,MATKLLOV,TamanoUnicoLOV,LicenciaPersonajeLOV,PPH_L4_Templates,CharacteristicCategories,ItemGroupConProductoSBBLOV,TemplatesLOV,NumeroDePiezasQueIncluyeLOV,SB_0002LOV,AE023LOV,ZZLICLOV,AR003LOV,MATKLLOV_S4H,BRAND_IDLOV_S4H,FormaLOV,AE249LOV,MaterialLOV,GeneroliterarioLOV,AE019LOV,AE350LOV

		java.util.LinkedList<String> identifiers = new java.util.LinkedList<>();
		org.json.JSONArray losrows = new org.json.JSONArray();

		try{
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?query=" // Characteristic.Category->LookupValue.Code contains \"Master Data\" and
						+ java.net.URLEncoder.encode("Characteristic.Category->LookupValue.Code in (\"Master Data\",Holder3,Holder4) and Characteristic.Lookup in (ZCOMALOV,ItemGroupProductLOV,BrandLOV,PE000LOV,ItemGroupLOV,googleTaxonomyMap_LOV,MATKLLOV2,MATKLLOV,TamanoUnicoLOV,TM001LOV,LicenciaPersonajeLOV,TDI01LOV,TipoDeEquipoLOV,PPH_L4_Templates,CharacteristicCategories,ItemGroupConProductoSBBLOV,FranquiciaLOV,TemplatesLOV,NumeroDePiezasQueIncluyeLOV,PaginasLOV,ProfundidadLOV,PesoDelProductoConSoporteLOV,PesosDelProductoSinSoporteLOV,PesoDeLaMesaLOV,TML01LOV,SB_0002LOV,AE023LOV,WHERRLOV,ZZLICLOV,DiametroDelProductoLOV,AR003LOV,TD001LOV,MATKLLOV_S4H,BRAND_IDLOV_S4H,FormaLOV,AE249LOV,TDA01LOV,MaterialLOV,TAC01LOV,EquipoCompatibleLOV,GeneroliterarioLOV,AE019LOV,AE350LOV,TI001LOV,LargoMesaLOV,TC001LOV,LongitudLOV,ProfundidadDeLaBaseLOV,SB_T_HARDLINELOV,PotenciaLOV,AE316LOV,AlturaMaximaLOV,AltoDeLaBaseLOV,TecnologiaDeTenisLOV,EditorialLOV,SB_THARDLINELOV,AE211LOV)", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "Characteristic.Identifier,"
								+ "Characteristic.IsActive,"
								+ "Characteristic.DataType,"
								+ "Characteristic.Entities,"
								+ "Characteristic.Lookup->Lookup.Identifier"
							, "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=500"
						+ "&startIndex=" + currentIndex
						, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				System.out.println("Total Size: " + totalSize);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					identifiers.addLast(values.getString(0));
				}
			}while(currentIndex < totalSize);
			int a = 0;
			for(String identifier : identifiers) {
				a++;
				if(a< totalSize - 30) {
					continue;
				}
				losrows.put(new org.json.JSONObject().put("object" , "'" + identifier + "'").put("values", new org.json.JSONArray().put("Holder6")));
			}
			System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.Category"))).put("rows", losrows).toString()) );
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}

		System.exit(0);

		java.util.LinkedList<String> lookups = new java.util.LinkedList<>();
		java.util.Map<String, Integer> lookupRows = new java.util.TreeMap<>();

//		org.json.JSONArray losrows = new org.json.JSONArray();
		try{
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/Lookup/bySearch?query=" // Characteristic.Category->LookupValue.Code contains \"Master Data\" and
						+ java.net.URLEncoder.encode(" not Lookup.Identifier is empty", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "Lookup.Identifier,"
							, "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=500"
						+ "&startIndex=" + currentIndex
						, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				System.out.println("Total Size: " + totalSize);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					lookups.addLast(values.getString(0));
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}

		try{
			for(String lookup : lookups) {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/LookupValue/byLookup?lookup=" // Characteristic.Category->LookupValue.Code contains \"Master Data\" and
						+ java.net.URLEncoder.encode(lookup, "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "LookupValue.Code,"
							, "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=2"
						+ "&startIndex=" + currentIndex
						, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				System.out.println("Total Size: " + totalSize);
				lookupRows.put(lookup, totalSize);
				try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\lookup_row_count", true)))){
					pw.println(lookup + "<::>" + totalSize);
				}catch(java.io.IOException e) { e.printStackTrace(); }
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

		java.util.LinkedList<java.util.Map.Entry<String, Integer>> entrySet = new java.util.LinkedList<>(lookupRows.entrySet());
		java.util.Collections.sort(entrySet, ((o1,o2)->o2.getValue().compareTo(o1.getValue())));
		entrySet.forEach(System.out::println);

		System.exit(0);

//		java.util.LinkedList<String> identifiers = new java.util.LinkedList<>();

//		org.json.JSONArray losrows = new org.json.JSONArray();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\master_data_characteristics_reject")))){
			pw.println(new org.json.JSONArray().put("Identifier").put("IsActive").put("DataType").put("Entities").put("Lookup"));
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?query=" // Characteristic.Category->LookupValue.Code contains \"Master Data\" and
						+ java.net.URLEncoder.encode(" Characteristic.Identifier in (\"TamanoUnico\",\"BrandName\")", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "Characteristic.Identifier,"
								+ "Characteristic.IsActive,"
								+ "Characteristic.DataType,"
								+ "Characteristic.Entities,"
								+ "Characteristic.Lookup->Lookup.Identifier"
							, "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=500"
						+ "&startIndex=" + currentIndex
						, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				System.out.println("Total Size: " + totalSize);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					identifiers.addLast(values.getString(0));
				}
			}while(currentIndex < totalSize);
			int a = 0;
			for(String identifier : identifiers) {
				a++;
				if(a< totalSize - 30) {
					continue;
				}
				losrows.put(new org.json.JSONObject().put("object" , "'" + identifier + "'").put("values", new org.json.JSONArray().put("Holder6")));
			}
			System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.Category"))).put("rows", losrows).toString()) );
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}

		System.exit(0);

//		org.json.JSONArray losrows = new org.json.JSONArray();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\master_data_characteristics_reject")))){
			pw.println(new org.json.JSONArray().put("Identifier").put("IsActive").put("DataType").put("Entities").put("Lookup"));
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?query="
						+ java.net.URLEncoder.encode("Characteristic.Category->LookupValue.Code contains \"Master Data\" and Characteristic.IsActive equals false", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "Characteristic.Identifier,"
								+ "Characteristic.IsActive,"
								+ "Characteristic.DataType,"
								+ "Characteristic.Entities,"
								+ "Characteristic.Lookup->Lookup.Identifier"
							, "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=500"
						+ "&startIndex=" + currentIndex
						, null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				System.out.println("Total Size: " + totalSize);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					losrows.put(new org.json.JSONObject().put("object", rows.getJSONObject(i).getJSONObject("object")).put("values", new org.json.JSONArray().put(true)));
					System.out.println(values.getString(0));
//					System.out.println( rc.getRequest("POST", baseUrl + "/list/Characteristic", new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", losrows).toString()) );
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}

		System.out.println("Done. " + formatMillis(System.currentTimeMillis() - init));
	}

	private static String formatMillis(long millis){
	  	int days = (int)(millis/(1000*60*60*24));
	 	millis -= days*1000*60*60*24;
	  	int hours = (int) (millis/(1000*60*60));
	  	millis -= hours*1000*60*60;
	  	int minutes = (int) (millis/(1000*60));
	  	millis -= minutes*1000*60;
	  	int seconds = (int) (millis/1000);
	  	millis -= seconds*1000;
	  	return
	  		    (days < 10 ? "0" : "") + days + ":"
	  		+ (hours < 10 ? "0" : "") + hours + ":"
	  		+ (minutes < 10 ? "0" : "") + minutes + ":"
	  		+ (seconds < 10 ? "0" : "") + seconds
	  		+ "." + millis;
	  }
}
