package mx.com.liverpool.p360.services.core.temp;

import java.io.FileNotFoundException;
import java.io.IOException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CargamentoDeValoresSAP {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] arsgs) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException, TransformerException {
//		marcas();
//		cargaGruposDeArticulo();

//		System.exit(0);
//		pubSubProveedorMarcaNegocio();
//		cargaPlantillasConGruposDeArticulo();
//		System.exit(0);
//		System.out.println("***");
//		test();
//		System.exit(0);
//		cargaGruposDeArticulo();
		java.util.Map<String, java.util.LinkedList<String>> proveedorMarcas = new java.util.TreeMap<>();
		java.util.Map<String, java.util.LinkedList<String>> proveedorGruposDeArticulo = new java.util.TreeMap<>();
		java.util.Map<String, java.util.LinkedList<String>> grupoDeArticulosProveedor = new java.util.TreeMap<>();
		java.util.Map<String, java.util.LinkedList<String>> grupossDeArticuloMarcas = new java.util.TreeMap<>();
		java.util.LinkedList<String> proveedores = new java.util.LinkedList<>();
		java.util.LinkedList<String> marcas = new java.util.LinkedList<>();
		java.util.LinkedList<String> gruposDeArticulo = new java.util.LinkedList<>();
		java.util.LinkedList<String> marcasDeProveedor = new java.util.LinkedList<>();
		 try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\Users\\jcapizc\\Downloads\\Datos SAP QA - ECC - Relación grupo+Marca SAP QA LV.tsv"), java.nio.charset.Charset.forName("UTF-8")))){
			 String line = null;
			 String delimitador = "\"";
			 String separador = "\t";
			 String escape = "\\";
			 String[] pieces = null;
			 String grupoDeArticulos = null;
			 String submarca = null;
			 workshop.parseLine(br.readLine(), delimitador, separador, escape);
			 while((line = br.readLine()) != null) {
				 pieces = workshop.parseLine(line, delimitador, separador, escape);
				 grupoDeArticulos = pieces[1];
				 submarca = pieces[2];
				 marcas = grupossDeArticuloMarcas.get(grupoDeArticulos);
				 if(marcas == null) {
					 marcas = new java.util.LinkedList<>();
					 grupossDeArticuloMarcas.put(grupoDeArticulos, marcas);
				 }
				 marcas.addLast(submarca);
			 }
		 }catch(java.io.IOException e) {
			 e.printStackTrace();
		 }
		 try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\Users\\jcapizc\\Downloads\\Datos SAP QA - ECC - Relación Proveedor Grupo SAP QA.tsv"), java.nio.charset.Charset.forName("UTF-8")))){
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
				 proveedores = grupoDeArticulosProveedor.get(grupoDeArticulos);
				 if(proveedores == null) {
					 proveedores = new java.util.LinkedList<>();
					 grupoDeArticulosProveedor.put(grupoDeArticulos, proveedores);
				 }
				 proveedores.addLast(proveedor);
				 gruposDeArticulo = proveedorGruposDeArticulo.get(proveedor);
				 if(gruposDeArticulo == null) {
					 gruposDeArticulo = new java.util.LinkedList<>();
					 proveedorGruposDeArticulo.put(proveedor, gruposDeArticulo);
				 }
				 gruposDeArticulo.addLast(grupoDeArticulos);
				 marcas = grupossDeArticuloMarcas.get(grupoDeArticulos);
				 marcasDeProveedor = proveedorMarcas.get(proveedor);
				 if(marcasDeProveedor == null) {
					 marcasDeProveedor = new java.util.LinkedList<>();
					 proveedorMarcas.put(proveedor, marcasDeProveedor);
				 }
				 if(marcas != null) {
					 marcasDeProveedor.addAll(marcas);
				 }
			 }
		 }catch(java.io.IOException e) {
			 e.printStackTrace();
		 }
		 boolean passFirstDeletion = true;
		 boolean passSecondDeletion = true;
		 java.util.Map<String, String> qp = new java.util.TreeMap<>();
		 qp.put("lookup", "Party");
		 qp.put("qualificationFilter", "refLookup('ZCOMALOV')");
//		 RESTWorkshop workshop = new RESTWorkshop("Accept: application/json", "Content-Type: application/x-www-form-urlencoded", "Accept-Language: es", "Authorization: Basic " + "cmVzdDpoZWlsZXI=");
		 org.json.JSONObject response = null;
//		 response = workshop.makeRequest("DELETE", "/list/LookupValue/LookupValueReference/byLookup", qp, null);
//		 System.out.println("From delete ZCOMALOV: " + response);
//		 System.out.println("Message: " + workshop.getRawResponse());
//		 passFirstDeletion = !workshop.getRawResponse().contains("upstream request timeout");
//		 qp.put("qualificationFilter", "refLookup('MATKLLOV')");
//		 response = workshop.makeRequest("DELETE", "/list/LookupValue/LookupValueReference/byLookup", qp, null);
//		 passSecondDeletion = !workshop.getRawResponse().contains("upstream request timeout");
//		 System.out.println("From delete MATKLLOV: " + response);
//		 System.out.println("Message: " + workshop.getRawResponse());
		 qp.put("lookup", "Party");
		 qp.remove("qualificationFilter");
		 org.json.JSONArray values = new org.json.JSONArray();
		 org.json.JSONArray rws = null;
		 int currentIndex = 0;
		 int totalSize = 0;
		 qp.clear();
		 qp.put("lookup", "Party");
		 qp.put("query", "LookupValue.IsActive = true");
		 qp.put("fields", "LookupValue.Code");
		 qp.put("pageSize", "900");
		 org.json.JSONArray rowsPayload = new org.json.JSONArray();
		 System.out.println("Dissabling elements");
		 java.util.Map<String, String> qpempty = new java.util.TreeMap<>();
//		 do {
//			 qp.put("startIndex", String.valueOf(currentIndex));
//			 response = CargamentoDeValoresSAP.workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
//			 if(response == null) {
//				 System.out.println("Following problems: " + CargamentoDeValoresSAP.workshop.getRawResponse());
//				 break;
//			 }
//			 totalSize = response.getInt("totalSize");
//			 rws = response.getJSONArray("rows");
//			 for(int i=0; i<rws.length(); i++) {
//				 currentIndex++;
//				 values = rws.getJSONObject(i).getJSONArray("values");
//				 rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'@'Party'")).put("values", new org.json.JSONArray().put(false)));
//			 }
//		 }while(currentIndex < totalSize);
//		 currentIndex = 0;
//		 if(rowsPayload.length() > 0) {
//			 response = CargamentoDeValoresSAP.workshop.makeRequest("POST", "/list/LookupValue", qpempty, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))).put("rows", rowsPayload).toString());
//			 if(response != null) {
//				 System.out.println("From disabling elements: " + response.getJSONObject("counters"));
//			 }else {
//				 System.out.println("Problems updating Party elements: " + CargamentoDeValoresSAP.workshop.getRawResponse());
//			 }
//			 while(rowsPayload.length() > 0) {
//				 rowsPayload.remove(0);
//			 }
//		 }
		 System.out.println("Elements dissabled.");
		 if(passFirstDeletion && passSecondDeletion) {
			 PubSubGCP psd = new PubSubGCP("D:\\tmp\\crp-dev-dig-vccatalog-b74410667aea.json", "crp-dev-dig-vccatalog", "idmc_post_provider_relation");
			 PubSubGCP psq = new PubSubGCP("D:\\tmp\\crp-qas-dig-vccatalog-416185bab156.json", "crp-qas-dig-vccatalog", "idmc_post_provider_relation");
			 org.json.JSONArray columns = new org.json.JSONArray();
			 columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ZCOMALOV')"));
			 columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('MATKLLOV')"));
			 columns.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('BusinessQualified')"));
			 columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
			 columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
			 org.json.JSONObject request = new org.json.JSONObject();
			 request.put("columns", columns);
			 org.json.JSONArray rows = new org.json.JSONArray();
			 request.put("rows", rows);
			 qp.clear();
			 System.out.println("Processing " + proveedorMarcas.keySet().size() + " elements.");
			 org.json.JSONArray business = new org.json.JSONArray();
			 business.put("LVP");
			 business.put("MKP");
			 java.util.Map<Long, org.w3c.dom.Node> proveedoresConocidos = BusquedaNombresDeProveedores.proveedoresConocidos();
			 org.w3c.dom.Node proveedor;
			 org.json.JSONObject json = null;
			 org.json.JSONArray jsons = null;
			 org.json.JSONObject providerRelation = null;
			 java.util.LinkedList<String> ofInterest =
					 new java.util.LinkedList<>();
//			 	 new java.util.LinkedList<>(java.util.Arrays.asList(new String[] { "46312", /* "15402","2290","3064446","3572","95174" */ }));
			 boolean lock = true;
			 for(java.util.Map.Entry<String, java.util.LinkedList<String>> entry : proveedorMarcas.entrySet()) {
				 if(ofInterest.isEmpty() || ofInterest.contains(entry.getKey())) {
					 if("M35684".equals(entry.getKey())) {
						 lock = false;
					 }
					 if(lock) {
						 continue;
					 }
					 marcas = entry.getValue();
					 gruposDeArticulo = proveedorGruposDeArticulo.get(entry.getKey());
					 try{
						 proveedor = proveedoresConocidos.get(Long.parseLong(entry.getKey()) );
					 }catch(NumberFormatException e) {
						 continue;
					 }
					 json = new org.json.JSONObject();
					 json.put("idProveedor", ponleCeros(entry.getKey()));
					 json.put("brandsLiv", toJSONArrayFlat(marcas));
					 json.put("brandsMkp", toJSONArrayFlat(marcas));
					 jsons = new org.json.JSONArray();
					 jsons.put(json);
					 providerRelation = new org.json.JSONObject();
					 providerRelation.put("providerRelation", jsons);
//					 psd.publishMessage(providerRelation.toString());
//					 psq.publishMessage(providerRelation.toString());
					 System.out.println("Sent: " + marcas.size() + " marcas.");
					 values = new org.json.JSONArray();
					 values.put( toJSONArray(marcas, "ZCOMALOV") );
					 values.put( toJSONArray(gruposDeArticulo, "MATKLLOV") );
					 values.put( business );
					 values.put( proveedor != null ? workshop.getXmm().byName(proveedor, "Name").getTextContent() : "" );
					 values.put(true);
					 rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + entry.getKey() + "'@'Party'")).put("values", values));
					 if(rows.length() == 150) {
						 response = CargamentoDeValoresSAP.workshop.makeRequest("POST", "/list/LookupValue", qp, request.toString());
						 System.out.println(response);
						 while(rows.length() > 0) {
							 rows.remove(0);
						 }
					 }
				 }
			 }
//			 System.out.println(jsons.length());
			 if(rows.length() > 0) {
				 response = CargamentoDeValoresSAP.workshop.makeRequest("POST", "/list/LookupValue", qp, request.toString());
				 if(response == null) {
					System.out.println("Wrong, got: " + CargamentoDeValoresSAP.workshop.getRawResponse());
				 }else {
					 System.out.println(response);
				 }
				 while(rows.length() > 0) {
					 rows.remove(0);
				 }
			 }
		 }
	}

	private static void test() {
		java.util.Map<String, java.util.LinkedList<String>> proveedorGruposDeArticulo = new java.util.TreeMap<>();
		java.util.Map<String, java.util.LinkedList<String>> grupoDeArticulosProveedor = new java.util.TreeMap<>();
		java.util.LinkedList<String> gruposDeArticulo = null;

		java.util.Set<String> proveedores = new java.util.TreeSet<>();

		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\Users\\jcapizc\\Downloads\\Datos SAP QA - ECC - Relación Proveedor Grupo SAP QA.tsv"), java.nio.charset.Charset.forName("UTF-8")))){
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
				 proveedor = pieces[1];
				 proveedores.add(proveedor);
			 }
			 System.out.println(proveedores.size());
		 }catch(java.io.IOException e) {
			 e.printStackTrace();
		 }
		proveedores.forEach(System.out::println);
	}

	private static String ponleCeros(String value) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<(20 - value.length()); i++) {
			sb.append("0");
		}
		sb.append(value);
		return sb.toString();
	}

	private static java.util.Set<String> llavesOcupadas() throws ServiceUnavailableException{

		java.util.Set<String> llavesOcupadas = new java.util.TreeSet<>();
		RESTWorkshop workshop = new RESTWorkshop();

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		workshop.putParameter("fields", "Product2GCharacteristicValue.LookupValue('ItemGroup',root,\"0000.0000.RK\",'ItemGroup')->LookupValue.Code");
		workshop.putParameter("query", "not characteristic('ItemGroup') is empty");
		workshop.putParameter("pageSize", "1200");

		do {
			workshop.putParameter("startIndex", String.valueOf(currentIndex));
			response = workshop.makeRequest("GET", "/list/Product2G/bySearch");
			if(response == null) {
				System.out.println(workshop.getRawResponse());
				return null;
			}
			totalSize = response.getInt("totalSize");
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				llavesOcupadas.add(values.getJSONArray(0).getString(0));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		System.out.println("Estas son las llaves ocupadas:");
		llavesOcupadas.forEach(System.out::println);
		System.out.println(llavesOcupadas.size() + ", vimos: " + totalSize + " elementos en la lista.");

		return llavesOcupadas;
	}

	private static org.json.JSONArray toJSONArray(java.util.Set<String> lst, String parent){
		org.json.JSONArray array = new org.json.JSONArray();
		if(lst != null) {
			for(String element : lst) {
				array.put( new org.json.JSONObject().put("id", "'" + element + "'@'" + parent + "'"));
			}
		}
		return array;

	}

	private static org.json.JSONArray toJSONArray(java.util.LinkedList<String> lst, String parent){
		org.json.JSONArray array = new org.json.JSONArray();
		if(lst != null) {
			for(String element : lst) {
				array.put( new org.json.JSONObject().put("id", "'" + element + "'@'" + parent + "'"));
			}
		}
		return array;

	}

	private static org.json.JSONArray toJSONArrayFlat(java.util.LinkedList<String> lst){
		org.json.JSONArray array = new org.json.JSONArray();
		java.util.Set<String> alv = new java.util.TreeSet<>();
		if(lst != null) {
			for(String element : lst) {
				alv.add(element);
			}
			for(String element : alv) {
				array.put(  element );
			}
		}
		return array;

	}

	private static void borraLookup(String lookup) throws ServiceUnavailableException {
		RESTWorkshop workshop = new RESTWorkshop();
		workshop.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
		workshop.putParameter("query", "not LookupValue.Code is empty");
		workshop.putParameter("lookup", lookup);
		org.json.JSONObject resp = workshop.makeRequest("DELETE", "/list/LookupValue/bySearch");
		System.out.print(resp == null ? workshop.getRawResponse() : resp);
	}

	private static void borraProductosYArticulos() throws ServiceUnavailableException {
		RESTWorkshop workshop = new RESTWorkshop();
		org.json.JSONObject resp = null;
		workshop.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
		resp = workshop.makeRequest("DELETE", "/list/Product2G/byCatalog");
		if(resp == null) {
			System.out.println(workshop.getRawResponse());
			System.exit(0);
		}
		System.out.println(resp);
		resp = workshop.makeRequest("DELETE", "/list/Article/byCatalog");
		if(resp == null) {
			System.out.println(workshop.getRawResponse());
			System.exit(0);
		}
		System.out.println(resp);
	}

	private static void marcas() throws ServiceUnavailableException {
		long init = System.currentTimeMillis();
//		borraLookup("ZCOMALOV");
//		borraProductosYArticulos();
		RESTWorkshop workshop = new RESTWorkshop();
		org.json.JSONObject resp = null;
		java.util.Set<String> llavesOcupadas = llavesOcupadas();
		StringBuilder sb = new StringBuilder();
		for(String llaveOcupada : llavesOcupadas) {
			sb.append(sb.length() == 0 ? "" : ",").append("\"").append(llaveOcupada).append("\"");
		}
		workshop.clearParameters();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\Users\\jcapizc\\Downloads\\EXPORTMARCASQASAP - Sheet1.tsv")))){
			workshop.getRc().getHeader().put("Content-Type", "application/json");
			String linea = null;
			String delimitador = "\"";
			String separador = "\t";
			String escape = "\\";
			String[] pieces = null;
			br.readLine();
			org.json.JSONArray columns = new org.json.JSONArray();
			columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
			columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
			org.json.JSONArray rows = new org.json.JSONArray();
			org.json.JSONObject request = new org.json.JSONObject();
			request.put("columns", columns);
			request.put("rows", rows);
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			org.json.JSONObject response = null;
			while((linea = br.readLine()) != null) {
				pieces = workshop.parseLine(linea, delimitador, separador, escape);
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[1] + "'@'ZCOMALOV'")).put("values", new org.json.JSONArray().put(pieces[2]).put(true)));
				if(rows.length() == 500) {
					response = workshop.makeRequest("POST", "/list/LookupValue", qp, request.toString());
					if(response != null) {
						if(response.getJSONObject("counters").getInt("errors") == 0) {
							System.out.println(response.getJSONObject("counters"));
						}else {
							System.out.println(response.getJSONArray("entries"));
						}
					}else {
						System.out.println("Got error: " + workshop.getRawResponse());
					}
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
			if(rows.length() > 0) {
				response = workshop.makeRequest("POST", "/list/LookupValue", qp, request.toString());
				if(response != null) {
					if(response.getJSONObject("counters").getInt("errors") == 0) {
						System.out.println(response.getJSONObject("counters"));
					}else {
						System.out.println(response.getJSONArray("entries"));
					}
				}else {
					System.out.println("Got error: " + workshop.getRawResponse());
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println(workshop.formatTime(System.currentTimeMillis() - init));
	}

	private static void cargaGruposDeArticulo() {
		long init = System.currentTimeMillis();
		RESTWorkshop workshop = new RESTWorkshop();
		org.json.JSONObject resp = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\Users\\jcapizc\\Downloads\\Datos SAP QA - ECC - Relación Proveedor Grupo SAP QA.tsv")))){
			workshop.getRc().getHeader().put("Content-Type", "application/json");
			String linea = null;
			String delimitador = "\"";
			String separador = "\t";
			String escape = "\\";
			String[] pieces = null;
			br.readLine();
			org.json.JSONArray columns = new org.json.JSONArray();
			columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
			columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
			org.json.JSONArray rows = new org.json.JSONArray();
			org.json.JSONObject request = new org.json.JSONObject();
			request.put("columns", columns);
			request.put("rows", rows);
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			org.json.JSONObject response = null;
			while((linea = br.readLine()) != null) {
				pieces = workshop.parseLine(linea, delimitador, separador, escape);
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + pieces[3] + "'@'MATKLLOV'")).put("values", new org.json.JSONArray().put(pieces[4]).put(true)));
				if(rows.length() == 500) {
					response = workshop.makeRequest("POST", "/list/LookupValue", qp, request.toString());
					if(response != null) {
						if(response.getJSONObject("counters").getInt("errors") == 0) {
							System.out.println(response.getJSONObject("counters"));
						}else {
							System.out.println(response.getJSONArray("entries"));
						}
					}else {
						System.out.println("Got error: " + workshop.getRawResponse());
					}
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
			if(rows.length() > 0) {
				response = workshop.makeRequest("POST", "/list/LookupValue", qp, request.toString());
				if(response != null) {
					if(response.getJSONObject("counters").getInt("errors") == 0) {
						System.out.println(response.getJSONObject("counters"));
					}else {
						System.out.println(response.getJSONArray("entries"));
					}
				}else {
					System.out.println("Got error: " + workshop.getRawResponse());
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done. " + workshop.formatTime(System.currentTimeMillis() - init));
	}

	private static java.util.Set<String> getECCGroupOfArticles() throws ServiceUnavailableException{

		java.util.Set<String> currentGroupOfArticles = new java.util.TreeSet<>();
		RESTWorkshop workshop = new RESTWorkshop();
		workshop.putParameter("lookup", "MATKLLOV");
		workshop.putParameter("pageSize", "1200");
		workshop.putParameter("fields", "LookupValue.Code");

		org.json.JSONObject resp = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;

		do {
			workshop.putParameter("startIndex", String.valueOf(currentIndex));
			resp = workshop.makeRequest("GET", "/list/LookupValue/byLookup");
			totalSize = resp.getInt("totalSize");
			rows = resp.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				currentIndex++;
				values = rows.getJSONObject(i).getJSONArray("values");
				currentGroupOfArticles.add(values.getString(0));
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;

		return currentGroupOfArticles;
	}

	public static void cargaPlantillasConGruposDeArticulo() throws ServiceUnavailableException {
		long init = System.currentTimeMillis();
		RESTWorkshop workshop = new RESTWorkshop();
		java.util.Map<String, java.util.Set<String>> pares = new java.util.TreeMap<>();
		java.util.Set<String> gruposDeArticulo = null;
		int masgrande = 0;
		java.util.Set<String> groupOfArticles = getECCGroupOfArticles();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\Users\\jcapizc\\Downloads\\relationship product-supplier.xlsx - Suppliers apply.tsv")))){
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
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			org.json.JSONObject response = null;
			while((linea = br.readLine()) != null) {
				pieces = workshop.parseLine(linea, delimitador, separador, escape);
				if(pieces[8].startsWith("EU4-")) {
					gruposDeArticulo = pares.get(pieces[8]);
					if(gruposDeArticulo == null) {
						gruposDeArticulo = new java.util.TreeSet<>();
						pares.put(pieces[8], gruposDeArticulo);
					}
					if(groupOfArticles.contains(pieces[2])) {
						gruposDeArticulo.add(pieces[2]);
					}
				}
			}
			System.out.println("Working with: " + pares.size());
			for(java.util.Map.Entry<String, java.util.Set<String>> par : pares.entrySet()) {
				rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + par.getKey() + "'@'PPH_L4_Templates'")).put("values", new org.json.JSONArray().put( toJSONArray(par.getValue(), "MATKLLOV") )));
				masgrande = masgrande < par.getValue().size() ? par.getValue().size() : masgrande;
				if(rows.length() == 200) {
					response = workshop.makeRequest("POST", "/list/LookupValue", qp, request.toString());
					if(response != null) {
						if(response.getJSONObject("counters").getInt("errors") == 0) {
							System.out.println(response.getJSONObject("counters"));
						}else {
							System.out.println(response.getJSONArray("entries"));
						}
					}
					while(rows.length() > 0) {
						rows.remove(0);
					}
				}
			}
			System.out.println("Más grnade: " + masgrande);
			if(rows.length() > 0) {
				response = workshop.makeRequest("POST", "/list/LookupValue", qp, request.toString());
				if(response != null) {
					if(response.getJSONObject("counters").getInt("errors") == 0) {
						System.out.println(response.getJSONObject("counters"));
					}else {
						System.out.println(response.getJSONArray("entries"));
					}
				}else {
					System.out.println("Got error: " + workshop.getRawResponse());
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done. " + workshop.formatTime(System.currentTimeMillis() - init));
	}

}
