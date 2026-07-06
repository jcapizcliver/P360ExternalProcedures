package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;

import org.json.JSONObject;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

import mx.com.liverpool.p360.services.core.dq.NameAndProductName;
import mx.com.liverpool.p360.services.core.dq.NumberOfVariants;
import mx.com.liverpool.p360.services.core.dq.TituloSinMarca;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class CreateProposal {

	private String input = null;
	private String response = null;
	private String baseCacheDirectory = null;

	private boolean deleteInputFile = true;
	
	private final java.util.Map<String, String> charCategories = new java.util.TreeMap<>();

	private final String baseUrl; // = "http://172.18.237.162:1512/rest/V2.0";
	private final String encoded; // = "cmVzdDpoZWlsZXI=";

	private final RESTWrapper rw = new RESTWrapper();
	private final RESTWorkshop workshop; // = new RESTWorkshop(true, baseUrl, "Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + encoded, "Accept-Language: es");

	private final String objectAPIProduct2GURL; // = baseUrl + "/object/Product2G";
	private final String objectAPIArticleURL; // = baseUrl + "/object/Article";
	private final String listAPIArticleURL; // = baseUrl + "/list/Article/bySearch?query=";

	private org.json.JSONArray responses = new org.json.JSONArray();
	private org.json.JSONArray notFound = new org.json.JSONArray();

	private org.json.JSONObject genericResponse = null;
	private org.json.JSONArray variantResponsesArray = new org.json.JSONArray();

	private org.json.JSONArray genericFieldErrors = new org.json.JSONArray();
	private org.json.JSONArray variantFieldErrors = new org.json.JSONArray();


	private final RestClient rc; // = workshop.getRc();

	private String nextStatusDictionaryIdentifier = "NextStatus";
	private String externalStatusDictionaryIdentifier = "ExternalStatus";

	private String userAction = null;

	private java.util.Map<String, String> nextStatusMap = new java.util.TreeMap<>();
	private java.util.Map<String, String> externalStatusMap = new java.util.TreeMap<>();

	private java.util.Map<String, String> statusEnum = new java.util.TreeMap<>();
	private java.util.Map<String, String> externalStatusEnum = new java.util.TreeMap<>();

	
	
//	private java.util.Map<String, String> characteristicsThatAreLookups = null;
	private final DataRequestor dr = new DataRequestor();

	private java.util.Map<String, String> templateCharacteristicFilters = new java.util.TreeMap<>();

	private boolean ex = false;
	
	private java.util.Map<String, String[]> templateMetaData = new java.util.TreeMap<>();
	
	private final long myId;
	
	private java.util.concurrent.ConcurrentMap<String, String[]> parties = new java.util.concurrent.ConcurrentHashMap<String, String[]>(30000);
	
	private void loadParties() {
		try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "party"))){
			lns.parallel().map(s -> workshop.parseLine(s, "\"", ";", "\\")).forEach(arr -> parties.put(arr[0], new String[] { arr[1], arr[2], arr[3], arr[4] }));
		}catch(java.io.IOException e) {
			logE(e);
		}
	}
	
	public CreateProposal(String baseUrl, String encoded, long myId) {
		this.baseUrl = baseUrl;
		this.encoded = encoded;
		this.myId = myId;
		this.workshop = rw.getRw();
		this.rc = workshop.getRc();
		this.objectAPIProduct2GURL = baseUrl + "/object/Product2G";
		this.objectAPIArticleURL = baseUrl + "/object/Article";
		this.listAPIArticleURL = baseUrl + "/list/Article/bySearch?query=";
		
		loadParties();
	}
	
	private void loadStatusEnum() {
		if (!statusEnum.isEmpty()) {
			return;
		}
		java.util.Map<String, String> headers = new java.util.HashMap<>();
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONObject row = null;
//		try {
//			headers.put("Content-Type", "application/json");
//			headers.put("Accept", "application/json");
//			headers.put("Accept-Language", "es");
//			headers.put("Authorization", "Basic " + encoded);
//			rawResponse = this.rc.getRequest("GET", baseUrl + "/enum/Enum.Status", null, headers);
//			response = new org.json.JSONObject(rawResponse);
//			rows = response.getJSONArray("entries");
//			for (int i = 0; i < rows.length(); i++) {
//				row = rows.getJSONObject(i);
//				statusEnum.put(row.getString("key"), row.getString("label"));
//			}
//		} catch (Exception e) {
//			log(rawResponse);
//			logE(e);
//		}

		if(!java.nio.file.Files.exists(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "enums"))) {
			try {
				java.nio.file.Files.createDirectory(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "enums"));
				log("Ya creamos la carpeta =D");
			} catch (IOException e) {
				logE(e);
			}
		}
		String enm = "ExternalStatus";
		if(!java.nio.file.Files.exists(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "enums", enm))) {
			try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "enums", enm).toFile()), java.nio.charset.StandardCharsets.UTF_8))) {
				headers.put("Content-Type", "application/json");
				headers.put("Accept", "application/json");
				headers.put("Accept-Language", "es");
				headers.put("Authorization", "Basic " + encoded);
				rawResponse = this.rc.getRequest("GET", baseUrl + "/enum/Enum.Status", null, headers);
				response = new org.json.JSONObject(rawResponse);
				rows = response.getJSONArray("entries");
				for (int i = 0; i < rows.length(); i++) {
					row = rows.getJSONObject(i);
					statusEnum.put(row.getString("key"), row.getString("label"));
					pw.println( rw.getRw().serializeChunk(new Object[] { row.getString("key"), row.getString("label") }) );
				}
				log("Lo leímos de aquí");
			} catch (Exception e) {
				log(rawResponse);
				logE(e);
			}
		}else {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "enums", enm).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				String line = null;
				String[] partes = null;
				while((line = br.readLine()) != null) {
					partes = rw.getRw().parseLine(line);
					if(partes.length == 2)
						statusEnum.put(partes[0], partes[1]);
				}
				log("Lo leímos del archivo");
			}catch(java.io.IOException e) {
				logE(e);
			}
		}
	}

	private void loadExternalStatusEnum() {
		if (!externalStatusEnum.isEmpty()) {
			return;
		}
		java.util.Map<String, String> headers = new java.util.HashMap<>();
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONObject row = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
//		try {
//			headers.put("Content-Type", "application/json");
//			headers.put("Accept", "application/json");
//			headers.put("Authorization", "Basic " + encoded);
//			do {
//				rawResponse = this.rc.getRequest("GET", baseUrl
//						+ "/list/LookupValue/byLookup?lookup=ExternalStatus&fields=LookupValue.Code,LookupValueLang.Name(es)&pageSize=200&startIndex="
//						+ currentIndex, null, headers);
//				response = new org.json.JSONObject(rawResponse);
//				rows = response.getJSONArray("rows");
//				for (int i = 0; i < rows.length(); i++) {
//					row = rows.getJSONObject(i);
//					values = row.getJSONArray("values");
//					externalStatusEnum.put(values.getString(0), values.getString(1));
//				}
//				currentIndex += rows.length();
//				totalSize = response.getInt("totalSize");
//			} while (currentIndex < totalSize);
//		} catch (Exception e) {
//			log(rawResponse);
//			logE(e);
//		}
		String lkp = "ExternalStatus";
		if(!java.nio.file.Files.exists(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "global_lookups", lkp))) {
			log("Lo vamos a rehacer ES.");
			try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "global_lookups", lkp).toFile()), java.nio.charset.StandardCharsets.UTF_8))) {
				headers.put("Content-Type", "application/json");
				headers.put("Accept", "application/json");
				headers.put("Authorization", "Basic " + encoded);
				do {
					rawResponse = this.rc.getRequest("GET", baseUrl
							+ "/list/LookupValue/byLookup?lookup='" + lkp + "'&fields=LookupValue.Code,LookupValueLang.Name(es)&pageSize=200&startIndex="
							+ currentIndex, null, headers);
					response = new org.json.JSONObject(rawResponse);
					rows = response.getJSONArray("rows");
					for (int i = 0; i < rows.length(); i++) {
						row = rows.getJSONObject(i);
						values = row.getJSONArray("values");
						externalStatusEnum.put(values.getString(0), values.getString(1));
						pw.println( rw.getRw().serializeChunk(new Object[] { values.getString(0), values.getString(1) }, "\"", ";", "\\") );
					}
					currentIndex += rows.length();
					totalSize = response.getInt("totalSize");
				} while (currentIndex < totalSize);
			} catch (Exception e) {
				log(rawResponse);
				logE(e);
			}
		}else {
			log("Lo leímos ES");
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "global_lookups", lkp).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				String line = null;
				String[] partes = null;
				while((line = br.readLine()) != null) {
					partes = rw.getRw().parseLine(line, "\"", ";", "\\");
					if(partes.length == 2)
						externalStatusEnum.put(partes[0], partes[1]);
				}
			}catch(java.io.IOException e) {
				logE(e);
			}
		}
		
	}

	private void loadExternalStatusDictionary() {
		if (!externalStatusMap.isEmpty()) {
			return;
		}
		java.util.Map<String, String> headers = new java.util.HashMap<>();
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONObject row = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		if(!java.nio.file.Files.exists(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "dictionaries", this.externalStatusDictionaryIdentifier))) {
			try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "dictionaries", this.externalStatusDictionaryIdentifier).toFile()), java.nio.charset.StandardCharsets.UTF_8))) {
				headers.put("Content-Type", "application/json");
				headers.put("Accept", "application/json");
				headers.put("Authorization", "Basic " + encoded);
				do {
					rawResponse = this.rc.getRequest("GET", baseUrl + "/list/StandardizationValue/byDictionary?dictionary="
							+ java.net.URLEncoder.encode(this.externalStatusDictionaryIdentifier, "UTF-8")
							+ "&fields=StandardizationValue.Value,StandardizationValue.AlternativeValue&pageSize=200&startIndex="
							+ currentIndex, null, headers);
					response = new org.json.JSONObject(rawResponse);
					rows = response.getJSONArray("rows");
					for (int i = 0; i < rows.length(); i++) {
						row = rows.getJSONObject(i);
						values = row.getJSONArray("values");
						externalStatusMap.put(values.getString(0), values.getString(1));
						pw.println( rw.getRw().serializeChunk(new Object[] { values.getString(0), values.getString(1) }) );
					}
					currentIndex += rows.length();
					totalSize = response.getInt("totalSize");
				} while (currentIndex < totalSize);
			} catch (Exception e) {
				log(rawResponse);
				logE(e);
			}
		}else {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "dictionaries", this.externalStatusDictionaryIdentifier).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				String line = null;
				String[] partes = null;
				while((line = br.readLine()) != null) {
					partes = rw.getRw().parseLine(line, "\"", ";", "\\");
					if(partes.length == 2)
						externalStatusMap.put(partes[0], partes[1]);
				}
			}catch(java.io.IOException e) {
				logE(e);
			}
		}
	}

	private void loadNextStatusDictionary() {
		if (!nextStatusMap.isEmpty()) {
			return;
		}
		java.util.Map<String, String> headers = new java.util.HashMap<>();
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONObject row = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		if(!java.nio.file.Files.exists(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "dictionaries", this.nextStatusDictionaryIdentifier))) {
			try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "dictionaries", this.nextStatusDictionaryIdentifier).toFile()), java.nio.charset.StandardCharsets.UTF_8))) {
				headers.put("Content-Type", "application/json");
				headers.put("Accept", "application/json");
				headers.put("Authorization", "Basic " + encoded);
				do {
					rawResponse = this.rc.getRequest("GET", baseUrl + "/list/StandardizationValue/byDictionary?dictionary="
							+ java.net.URLEncoder.encode(this.nextStatusDictionaryIdentifier, "UTF-8")
							+ "&fields=StandardizationValue.Value,StandardizationValue.AlternativeValue&pageSize=200&startIndex="
							+ currentIndex, null, headers);
					response = new org.json.JSONObject(rawResponse);
					rows = response.getJSONArray("rows");
					for (int i = 0; i < rows.length(); i++) {
						row = rows.getJSONObject(i);
						values = row.getJSONArray("values");
						nextStatusMap.put(values.getString(0), values.getString(1));
						pw.println( rw.getRw().serializeChunk(new Object[] { values.getString(0), values.getString(1) }) );
					}
					currentIndex += rows.length();
					totalSize = response.getInt("totalSize");
				} while (currentIndex < totalSize);
			} catch (Exception e) {
				log(rawResponse);
				logE(e);
			}
		}else {
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "dictionaries", this.nextStatusDictionaryIdentifier).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				String line = null;
				String[] partes = null;
				while((line = br.readLine()) != null) {
					partes = rw.getRw().parseLine(line, "\"", ";", "\\");
					if(partes.length == 2)
						nextStatusMap.put(partes[0], partes[1]);
				}
			}catch(java.io.IOException e) {
				logE(e);
			}
		}
		
	}
/*
	private void loadTemplateCharacteristicsThatUseLookupValue(String templateId) {
		java.util.Map<String, String> headers = new java.util.HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Accept", "application/json");
		headers.put("Authorization", "Basic " + encoded);
		headers.put("Accept-Language", "es_ES");
		String rawResponse = null;
		JSONObject jsonResponse = null;
		org.json.JSONArray rows = null;
		int totalSize = 0;
		int startIndex = 0;
//		java.util.LinkedList<String> characteristics = null;
//		java.util.LinkedList<String> characteristicWithLookupType = null;
		try {
			do {
				rawResponse = this.rc.getRequest("GET",
						baseUrl + "/list/StructureGroup/bySearch?pageSize=500&structure=PrimaryProductTaxonomy&query="
								+ java.net.URLEncoder
										.encode("StructureGroup.Identifier equals \"" + templateId + "\"", "UTF-8")
								+ "&metaData=true&fields="
								+ java.net.URLEncoder
										.encode("StructureGroup.CharacteristicCategories->LookupValue.Code", "UTF-8")
								+ "&startIndex=" + startIndex,
						null, headers);
				jsonResponse = new JSONObject(rawResponse);
				rows = jsonResponse.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(0);
				for (int i = 0; i < rows.length(); i++) {
//					if (!this.characteristicOfCategories.containsKey(rows.getString(i))) {
//						characteristics = new java.util.LinkedList<>();
//						characteristicWithLookupType = new java.util.LinkedList<>();
//						if(!"".equals(rows.getString(i))) {
//							collectCharacteristicIdentifiers(rows.getString(i), characteristicWithLookupType,
//									characteristics);
//							this.characteristicOfCategories.put(rows.getString(i),
//									new CoupleOfLists(characteristics, characteristicWithLookupType));
//						}
//					}
				}
				totalSize = jsonResponse.getInt("totalSize");
				startIndex += jsonResponse.getInt("rowCount");
			} while (startIndex < totalSize);
		} catch (Exception e) {
			logE(e);
			log("#### ERROR: " + rawResponse);
		}
	}
*/
//	private void collectCharacteristicIdentifiers(String category,
//			java.util.LinkedList<String> characteristicWithLookupType, java.util.LinkedList<String> characteristics) {
//		java.util.Map<String, String> headers = new java.util.HashMap<>();
//		headers.put("Content-Type", "application/json");
//		headers.put("Accept", "application/json");
//		headers.put("Authorization", "Basic " + encoded);
//		headers.put("Accept-Language", "es_ES");
//		String rawResponse = null;
//		JSONObject jsonResponse = null;
//		org.json.JSONArray rows = null;
//		int totalSize = 0;
//		int startIndex = 0;
//		try {
//			do {
//				rawResponse = this.rc.getRequest("GET",
//						baseUrl + "/list/Characteristic/bySearch?query="
//								+ java.net.URLEncoder.encode("Characteristic.Category equals \"" + category
//										+ "\" and Characteristic.IsActive = true", "UTF-8")
//								+ "&metaData=true&fields=Characteristic.Identifier,Characteristic.DataType&startIndex="
//								+ startIndex,
//						null, headers);
//				jsonResponse = new JSONObject(rawResponse);
//				rows = jsonResponse.getJSONArray("rows");
//				for (int i = 0; i < rows.length(); i++) {
//					if ("LOOKUP".equals(rows.getJSONObject(i).getJSONArray("values").getString(1))) {
//						characteristicWithLookupType.addLast(rows.getJSONObject(i).getJSONArray("values").getString(0));
//					}
//					characteristics.addLast(rows.getJSONObject(i).getJSONArray("values").getString(0));
//					this.charCategories.put(rows.getJSONObject(i).getJSONArray("values").getString(0), category);
//				}
//				totalSize = jsonResponse.getInt("totalSize");
//				startIndex += jsonResponse.getInt("rowCount");
//			} while (startIndex < totalSize);
//		} catch (Exception e) {
//			log("Error, but rawresponse: " + rawResponse);
//			logE(e);
//		}
//	}

//	private void ll(String content) {
//		try (java.io.PrintWriter pw = new java.io.PrintWriter(
//				new java.io.OutputStreamWriter(new java.io.FileOutputStream("/P360shared/IDMC/stage/IDQ/CreateProposal/"
//						+ new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date()))))) {
//			pw.println(content);
//		} catch (java.io.IOException e) {
//			e.printStackTrace();
//		}
//	}

//	public static void main(String[] args) {
//		CreateProposal cp = new CreateProposal("http://172.18.237.162:1512/rest/V2.0", "cmVzdDpoZWlsZXI=");
//		System.out.println("75092832: " +  cp.isValidEan8("75092832") );
//		System.out.println("75084844: " + cp.isValidEan8("75084844") );
//		cp.doIt(args);
//		CreateProposal cp = new CreateProposal(null, null, -1);
//		String ean = "036000291452";
//		ean = "654316711848";
//		ean = "974508215782";
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "EANS12").toFile())))){
//			String line = null;
//			while((line = br.readLine()) != null) {
//				ean = line;
//				System.out.println(ean + " - " + cp.isValidUPCA(ean));
//			}
//		}catch(java.io.IOException e) {
//			e.printStackTrace();
//		}
//	}

	public String doIt(String[] args) {
		return doIt(args, false);
	}

	public String doIt(String[] args, boolean ex) {
		this.ex = ex;
		try {
			input = args[0];
			log("An input: " + input);
//			inFile = args[0];
//			log("Working with: " + inFile);
//			try (java.io.BufferedReader br = new java.io.BufferedReader(
//					new java.io.InputStreamReader(new java.io.FileInputStream(inFile)))) {
//				String line = null;
//				StringBuilder sb = new StringBuilder();
//				while ((line = br.readLine()) != null) {
//					sb.append(line);
//				}
//				input = sb.toString();
//			} catch (java.io.IOException e) {
//				logE(e);
//			}
			baseCacheDirectory = args[1];
			if (args.length > 2) {
				deleteInputFile = Boolean.parseBoolean( args[2] );
				if (args.length > 3) {
//					dictionary = args[3];
					if (args.length > 4) {
						nextStatusDictionaryIdentifier = args[4];
						if(args.length > 5) {
							externalStatusDictionaryIdentifier = args[5];
						}
					}
				}
			}
			return run();
		} catch (ArrayIndexOutOfBoundsException e) {
			logE(e);
			System.err.print(
					"Invalid number of arguments for java routine. Mandatories are: <inputFile> <baseCacheDir>, optionals are: <dictionary> <nextStatusDictionaryId> <externalStatusDictionaryId>, in that order.");
		}
		return null;
	}

	private boolean checkValue(String code, String lkp) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code");
		qp.put("query", "LookupValue.Code equals \"" + code + "\" and LookupValue.IsActive = true");
		qp.put("lookup", lkp);
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		return response != null && response.has("rows") && response.getJSONArray("rows").length() > 0;
	}

	private String queryColor(String value, String dictionary) {
		
		if(value == null)
			return null;
		String container = dictionary.replaceAll("/", "<::>");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(
				java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "dictionaries", container).toString())))){
			String line = null;
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, escp);
				if(value.toUpperCase().equals(pieces[0].toUpperCase()))
					return pieces[1];
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return null;
		
	}

	private String queryDictionary(String value, String dictionary) {
		if(value == null)
			return null;
		String container = dictionary.replaceAll("/", "<::>");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "dictionaries", container).toString())))){
			String line = null;
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, escp);
				if(value.equals(pieces[0]))
					return pieces[1];
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private String queryLkpBack(String value, String lkp) {
		String container = lkp.replaceAll("/", "<::>");
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), "global_lookups", container).toString())))){
			String line = null;
			String delim = "\"";
			String sep = ";";
			String escp = "\\";
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line, delim, sep, escp);
				if(value.equals(pieces[1]))
					return pieces[0];
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private String getLookupFromChar(String charId) {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.templates_cache_directory"), PropertiesManager.get("p360.contingency.characteristic_and_lookups")).toString())))){
			String line = null;
			line = br.readLine();
			String[] partes = null;
			String delim = "\"";
			String sep = ";";
			String esc = "\\";
			while((line = br.readLine()) != null) {
				partes = workshop.parseLine(line, delim, sep, esc);
				if(charId.equals(partes[0])) {
					log("Retrieved: " + partes[0] + " for " + charId);
					return partes[1];
				}
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		return null;
	}

	private java.util.ArrayList<String> getValidValues(String grupoArticulos, String lalookup, String laMeralookup){
		java.util.LinkedList<String> values = new java.util.LinkedList<>();
		
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), PropertiesManager.get("p360.contingency.itemgroup_valid_values" + laMeralookup)).toString())))){
			String delim = "\"";
			String sep = ";";
			String esc = "\\";
			String ln = null;
			String[] partes = null;
			String[] pieces = null;
			while((ln = br.readLine()) != null) {
				partes = workshop.parseLine(ln, delim, sep, esc);
				if(partes[0].equals(grupoArticulos) && partes[1].equals(lalookup)) {
					pieces = workshop.parseLine(partes[2], "\"", ",", "\\");
					values.addAll(java.util.Arrays.asList(pieces));
					return new java.util.ArrayList<>(values);
				}
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
		
		/*
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		log("Checking losvalores for " + lalookup + " in " + laMeralookup);
		qp.put("fields", "LookupValueReference.LookupValues('" + lalookup + "')->LookupValue.Code");
		qp.put("query", "LookupValue.Code equals \"" + grupoArticulos + "\" and LookupValue.IsActive = true");
		qp.put("lookup", "'" + laMeralookup + "'");
		org.json.JSONObject response = workshop.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
		org.json.JSONArray rows = null;
		org.json.JSONArray vls = null;
		org.json.JSONArray losvalues = null;
		if(response != null) {
			rows = response.getJSONArray("rows");
			for(int i=0; i<rows.length(); i++) {
				vls = rows.getJSONObject(i).getJSONArray("values");
				losvalues = vls.getJSONArray(0);
				for(int j=0; j<losvalues.length(); j++) {
					values.addLast(losvalues.getString(j));
				}
			}
		}else {
			log("Problem querying los meros valores: " + workshop.getRawResponse());
		}
		*/
		return new java.util.ArrayList<>(values);
	}
	
	private void loadTemplateMetaData() {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream( PropertiesManager.get("p360.contingency.create_proposal.structure_group_attribute_name_guide") )))){
			String line = null;
			String[] pieces = null;
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line);
				templateMetaData.put(pieces[0], new String[] {pieces[1], pieces[2], pieces[3]});
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
	}

	public void computeGeneric(
			  String proposalId
			, org.json.JSONArray characteristics
			, String template
			, String status
			, String negocio
			, String itemGroup
			, java.util.List<String> sections
			, int variantsWithMe
			, boolean unMasiosare
	) throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException, IOException, ServiceUnavailableException {
		log("Ya venimos aquí a ver que Chao");
//		characteristicsThatAreLookups = getCharacteristicsThatAreLookups();
		org.json.JSONArray characteristicRecords = characteristics; //dt.getJSONArray("_characteristicRecords");
		String rr = null;
		org.json.JSONObject resp = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray newCharacteristicRecords = characteristics;
		java.util.Map<String, org.json.JSONObject> characteristicsMap = new java.util.TreeMap<>();
		org.json.JSONObject json = null;
		String characteristicIdentifier = null;
		for(int i=0; i<characteristicRecords.length(); i++) {
			json = characteristicRecords.getJSONObject(i);
			characteristicIdentifier = json.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			characteristicsMap.put(characteristicIdentifier, json);
		}
		org.json.JSONObject rawConsFlagVal = characteristicsMap.get("Consignacion");
		if(rawConsFlagVal != null) {
			String consFlagVal = getCharacteristicValue( rawConsFlagVal );
			if(consFlagVal != null) {
				
			}
		}else {
			
		}
		log("Template: " + template);
		if(template != null && ( "1020".equals(status) || "1003".equals(status) || unMasiosare)) {
			addElement("ParentSKU", template, characteristics);
			loadTemplateMetaData();
			String[] templateMD = templateMetaData.get(template);
			if(templateMD == null) {
				try { 
					log("Not found, querying for it...");
					log("Querying: " + baseUrl + "/list/StructureGroup/bySearch?structure=PrimaryProductTaxonomy&query="
							+ java.net.URLEncoder.encode("StructureGroup.Identifier equals \"" + template + "\"", "UTF-8")
							+ "&fields=" + java.net.URLEncoder.encode(
									"StructureGroupAttributeValue.Value(\"NameGuide\",es,DEFAULT),StructureGroupAttributeValue.Value(\"OrderOfAtributesForName\",es,DEFAULT),StructureGroupLang.Name(es)", "UTF-8")
							);
					rr = rc.getRequest("GET", baseUrl + "/list/StructureGroup/bySearch?structure=PrimaryProductTaxonomy&query="
							+ java.net.URLEncoder.encode("StructureGroup.Identifier equals \"" + template + "\"", "UTF-8")
							+ "&fields=" + java.net.URLEncoder.encode(
									"StructureGroupAttributeValue.Value(\"NameGuide\",es,DEFAULT),StructureGroupAttributeValue.Value(\"OrderOfAtributesForName\",es,DEFAULT),StructureGroupLang.Name(es)", "UTF-8"), null);
//					log("GOT: " + rr);
					org.json.JSONObject j = new org.json.JSONObject(rr);
					if(j.has("rows") && j.getJSONArray("rows").length() > 0) {
						org.json.JSONArray values = j.getJSONArray("rows").getJSONObject(0).getJSONArray("values");
						templateMD = new String[3];
						templateMD[0] = values.getString(0);
						templateMD[1] = values.getString(1);
						templateMD[2] = values.getString(2);
					}
				}catch(org.json.JSONException e) {
					logE(e);
				}
			}
			if(templateMD != null) {
				java.util.Map<String, org.json.JSONObject> dataMap = new java.util.TreeMap<>();
				NameAndProductName nameAndProductName = new NameAndProductName(proposalId, templateMD[1], myId, genericFieldErrors);
				log("Processing neim... and product neim...");
				nameAndProductName.setSourceTemplate(template);
				nameAndProductName.processData( dataMap , newCharacteristicRecords);
				TituloSinMarca tituloSinMarca = new TituloSinMarca( getCharacteristicValue( characteristicsMap.get("Name") ) );
				tituloSinMarca.processData(characteristicsMap, newCharacteristicRecords);
			}else {
				log("No pude encontrar datos para la plantilla: " + template);
			}
			if(proposalId != null) {
				NumberOfVariants nov = new NumberOfVariants(proposalId);
				nov.processData(null, characteristicRecords);
			}else {
				newCharacteristicRecords.put( createCharacteristicValueObject("ZNUMV", variantsWithMe) );
			}
		}
		org.json.JSONArray values = null;
		String mesDeEntregaDeMercancia = getCharacteristicValue( characteristicsMap.get("MesdeEntregadeMercancIa"), true );
		int year = -1;
		if(mesDeEntregaDeMercancia != null & !"".equals(mesDeEntregaDeMercancia)){
			try{
				int mes = Integer.parseInt(mesDeEntregaDeMercancia.replaceFirst("^0+", ""));
				LocalDate hoy = LocalDate.now();

				int mesActual = hoy.getMonthValue();
				int anio = hoy.getYear();

				if (mes >= 10 || mes < mesActual) {
				    anio++;
				}
				year = anio;
				newCharacteristicRecords.put( createCharacteristicValueObject("AnoEstacion", String.valueOf(anio)) );
			}catch(NumberFormatException e) {
				logE(e);
			}
		}
		String fshId = getCharacteristicValue( characteristicsMap.get("FSH_ID"), true );
		if(fshId != null) {
			String fshTemporada = fshId == null || fshId.length() < 8 ? "" : fshId.substring(4,8);
			String fshTheme = null;
			if(!"".equals(fshTemporada)) {
				newCharacteristicRecords.put( createCharacteristicValueObject("FSH_SEASON", new org.json.JSONObject().put("_code", fshTemporada)) );
			}
			log("Got business: " + negocio);
			log("FSH_ID: " + fshId);
			if(fshId != null && !"".equals(fshId) && "Suburbia".equals(negocio) && fshId.length() > 9) {
				log("FSH_COLLECTION: " + fshId.substring(8,10));
				newCharacteristicRecords.put( createCharacteristicValueObject("FSH_COLLECTION", new org.json.JSONObject().put("_code", fshId.substring(8,10))) );
				if(fshId.length() > 10) {
					fshTheme = fshId.substring(10);
					if(checkValue(fshTheme, "FSH_THEMELOV_S4H")) {
						log("FSH_THEME: " + fshTheme);
						newCharacteristicRecords.put( createCharacteristicValueObject("FSH_THEME", new org.json.JSONObject().put("_code", fshTheme)) );
					}else {
						log("FSH_THEME not found, value: " + fshTheme);
					}
				}
			}
		}
		log("Placing Status...");
		newCharacteristicRecords.put( createCharacteristicValueObject("Status", new org.json.JSONObject().put("_code", "01")) );
		String wherl = getCharacteristicValue( characteristicsMap.get("WHERL") );
		String supplier = getCharacteristicValue( characteristicsMap.get("SupplierID") );
//		String monedaExtranjera = getCharacteristicValue( characteristicsMap.get("CostoEnMonedaExtranjera") );
		// Si el proveedor es nacional, CostoEnMonedaExtranjera no debe tener valor.
		// KONWA -> Nombre en SAP para la denominación del CostoEnMonedaExtranjera
		if(wherl != null && !"".equals(wherl) && supplier != null && !"".equals(supplier)) {
			if(!"Marketplace".equals(negocio)) {
				if("México".equals(wherl)) {
					newCharacteristicRecords.put( createCharacteristicValueObject("TImportacion", new org.json.JSONObject().put("_code", "N")) );
				}else {
					if(supplier != null && !"".equals(supplier)) {
						rr = rc.getRequest("GET", baseUrl + "/list/LookupValue/bySearch?lookup=" + java.net.URLEncoder.encode("'Party'", "UTF-8") + "&fields=" + java.net.URLEncoder.encode("LookupValueReference.LookupValues('TipoDeProveedorLOV')->LookupValue.Code,LookupValueReference.LookupValues('TipoProveedorSAPAttLOV')->LookupValue.Code", "UTF-8") + "&query=" + java.net.URLEncoder.encode("LookupValue.Code equals \"" + supplier + "\"", "UTF-8"), null);
						resp = new org.json.JSONObject(rr);
						rows = resp.getJSONArray("rows");
						if(rows.length() > 0) {
							values = rows.getJSONObject(0).getJSONArray("values");
							String tipoDeProveedor = values.getJSONArray(1).getString(0);
							log("GOT: " + values);
							if("PNA".equals(tipoDeProveedor)) {
								newCharacteristicRecords.put( createCharacteristicValueObject("TImportacion", new org.json.JSONObject().put("_code", "I")) );
							}else if("PEX".equals(tipoDeProveedor)) {
								newCharacteristicRecords.put( createCharacteristicValueObject("TImportacion", new org.json.JSONObject().put("_code", "D")) );
							} else {
								genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Validity").put("message", "No es un proveedor válido para catalogación. (Verificar el tipo de proveedor SAP así como el país de origen y el negocio)").put("fields", new org.json.JSONArray().put("TImportacion")));
							}
						} else {
							genericFieldErrors
								.put(new org.json.JSONObject().put("QualityDimension", "Validity").put("message"
									, "No es un proveedor válido para catalogación. (Verificar el tipo de proveedor SAP así como el país de origen y el negocio)").put("fields", new org.json.JSONArray().put("TImportacion")));
						}
					}
				}
			}else {
				if(supplier != null && !"".equals(supplier)) {
					rr = rc.getRequest("GET", baseUrl + "/list/LookupValue/bySearch?lookup=" + java.net.URLEncoder.encode("'Party'", "UTF-8") + "&fields=" + java.net.URLEncoder.encode("LookupValueReference.LookupValues('TipoDeProveedorLOV')->LookupValue.Code,LookupValueReference.LookupValues('TipoProveedorSAPAttLOV')->LookupValue.Code", "UTF-8") + "&query=" + java.net.URLEncoder.encode("LookupValue.Code equals \"" + supplier + "\"", "UTF-8"), null);
					resp = new org.json.JSONObject(rr);
					rows = resp.getJSONArray("rows");
					if(rows.length() > 0) {
						values = rows.getJSONObject(0).getJSONArray("values");
						String tipoDeProveedor = values.getJSONArray(1).getString(0);
						log("GOT: " + values);
						if("PNA".equals(tipoDeProveedor) && "México".equals(wherl)) {
							newCharacteristicRecords.put( createCharacteristicValueObject("TImportacion", new org.json.JSONObject().put("_code", "N")) );
						}else if("PNA".equals(tipoDeProveedor) && !"México".equals(wherl)) {
							newCharacteristicRecords.put( createCharacteristicValueObject("TImportacion", new org.json.JSONObject().put("_code", "I")) );
						}else {
							genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Validity").put("message", "No es un proveedor válido para catalogación. (Verificar el tipo de proveedor SAP así como el país de origen y el negocio)").put("fields", new org.json.JSONArray().put("TImportacion")));
						}
					}
				}
			}
		}

		// Al ser un proveedor nacional, aunque haya sido otro país
		if(sections.contains("basicData")) {

			Boolean consignacion = Boolean.parseBoolean( getCharacteristicValue( characteristicsMap.get("Consignacion") ) );
			log("Loveprool: " + consignacion);
			log("El bisnes: " + negocio);
			if(negocio != null && !"".equals(negocio)) {
				String skuType = "Liverpool".equals(negocio) ? consignacion ? "CONS" : "HAWA" : "Marketplace".equals(negocio) ? "SERV" : "" ;
				
				log("Elese ca u taip: " + skuType);
				if(!"".equals(skuType)) {
					newCharacteristicRecords.put( createCharacteristicValueObject("SkuType", new org.json.JSONObject().put("_code", skuType)) );
				}
				String MTART_S4H = "Suburbia".equals(negocio) ? "MODE" : "";
				if(!"".equals(MTART_S4H)) {
					newCharacteristicRecords.put( createCharacteristicValueObject("MTART_S4H", new org.json.JSONObject().put("_code", MTART_S4H) ) );
				}
			}
			
			String identificaNegocio = getCharacteristicValue( characteristicsMap.get("IdentificaNegocio"), true );
			if(identificaNegocio == null || "".equals(identificaNegocio)) {
				newCharacteristicRecords.put( createCharacteristicValueObject("IdentificaNegocio", new org.json.JSONObject().put("_code", identificaNegocio = "Marketplace".equals(negocio) ? "ART. MARKETPLACE" : negocio.toUpperCase() )) );
				log("IdentificaNegocio: " + identificaNegocio);
			}
			if("SUBURBIA".equals( identificaNegocio )) {
				newCharacteristicRecords.put( createCharacteristicValueObject("BWVOR", new org.json.JSONObject().put("_code", "6" )) );
			}
			Boolean isDutyFree = Boolean.parseBoolean(getCharacteristicValue( characteristicsMap.get("isDuttyFree")) );
			log("isDuttyFree = " + isDutyFree + " <::> " + characteristicsMap.get("isDuttyFree") );
			if("Liverpool".equals(negocio)) {
				newCharacteristicRecords.put( createCharacteristicValueObject("TypeMainBarCode", new org.json.JSONObject().put("_code", "IE" )) );
				newCharacteristicRecords.put( createCharacteristicValueObject("Negocio", new org.json.JSONObject().put("_code", isDutyFree ? "DUTY FREE" : "REGULAR" )) );
			}else if ("Suburbia".equals(negocio)) {
				newCharacteristicRecords.put( createCharacteristicValueObject("NUMTP_S4H", new org.json.JSONObject().put("_code", "IS" )) );
				newCharacteristicRecords.put( createCharacteristicValueObject("EXTWG_S4H", new org.json.JSONObject().put("_code", identificaNegocio != null && !"".equals(identificaNegocio) ? identificaNegocio : "SBB SUBURBIA" )) );
			}else if("Marketplace".equals(negocio)) {
				log("NOCHETO: <::::::>");
				newCharacteristicRecords.put( createCharacteristicValueObject("TypeMainBarCode", new org.json.JSONObject().put("_code", "IE" )) );
				newCharacteristicRecords.put( createCharacteristicValueObject("Negocio", new org.json.JSONObject().put("_code", "MARKETPLACE" )) );
				newCharacteristicRecords.put( createCharacteristicValueObject("FotoTomadaLiverpool", new org.json.JSONObject().put("_code", "N" )) );
			}
		}
//		String productTypeSAP  = getCharacteristicValue( characteristicsMap.get("ProductTypeSAP")  );
//		String descriptionLong = getCharacteristicValue( characteristicsMap.get("DescriptionLong") );
//		log("----> Épale mi compa: " + negocio + ", pts: " + productTypeSAP);
//		if("Marketplace".equals(negocio) && (productTypeSAP != null && !"".equals(productTypeSAP)) /* ((templateName != null && !"".equals(templateName)) || (productName != null && !"".equals(productName))) */ ) {
//			getItemGroupFromIA(productName == null || productName.isEmpty() ? templateName : productName, template, productTypeSAP, descriptionLong, templateName, newCharacteristicRecords);
//		}else {
//			itemGroup = getCharacteristicValue( characteristicsMap.get("ItemGroup"), true );
//			log("Fetching data for: " + characteristicsMap.get("ItemGroup"));
//			log("Another: got an itemGroup: " + itemGroup);	
//			if(itemGroup == null || "".equals(itemGroup)) {
//				itemGroup = getCharacteristicValue( characteristicsMap.get("ItemGroupS4H"), true );
//				log("Another: got an itemGroupS4H: " + itemGroup);
//			}
//			String brandName = getCharacteristicValue( characteristicsMap.get("BrandName"), true);
//			log("OLC");
//			if(brandName != null && !"".equals(brandName)) {
//				log("Validando marca y grupo de artículos: " + brandName + ", " + itemGroup + ", " + negocio);
//				if(!existeMarcaEnGrupoDeArticulos(brandName, itemGroup, negocio)) {
//					log("No valid pair.");
//					genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence").put("message", "La marca no es compatible con el grupo de artículos.").put("fields", new org.json.JSONArray().put( "Business" ).put(!"Suburbia".equals(negocio) ? "ItemGroupo" : "ItemGroupS4H").put( "BrandName" )).put("values", new org.json.JSONArray().put(negocio).put(itemGroup).put(brandName)));
//				}else {
//					log("Valid pair");
//				}
//			}
//		}
		log("/OLC.");
//		java.util.Map<String, String> miraklExcepProvEAN = getLkpValues("MarketplaceExcepProvEAN");
//		log("Loaded following values for ean mkt exceptions: " + miraklExcepProvEAN);
//		if(variants.length() == 1) {
//			if(mainBarCode == null || "".equals(mainBarCode)) {
//				if(!"".equals("Suburbia")) {
//					for(int t = 0; t< variants.length(); t+= 1) {
//						allPresent &= variants.getJSONObject(t).has("MainBarCode") && !"".equals(variants.getJSONObject(t).getString("MainBarCode"));
//						if(variants.getJSONObject(t).has("MainBarCode")) {
//							mainBarCode = variants.getJSONObject(t).getString("MainBarCode");
//							if(!"".equals(mainBarCode)) {
//								newCharacteristicRecords.put( createCharacteristicValueObject("MainBarCode", mainBarCode) );
//								log("Got the value from the only variant present.");
//							}else {
//								log("The variant had also no main bar code after all. (Variant number: " + t + ")");
//							}
//						}else {
//							mainBarCode = null;
//							log("Not even the variant contained value.");
//						}
//					}
//					if(mainBarCode != null && !"".equals(mainBarCode) && variants.length() == 1) {
//						newCharacteristicRecords.put( createCharacteristicValueObject("MainBarCode", mainBarCode) );
//					}else if(!allPresent) {
//						log("There was at least one variant with EAN missing... SUpplier: " + supplier + ", Business: " + negocio);
//						if("Marketplace".equals(negocio) && !miraklExcepProvEAN.containsKey(supplier)) {
//							genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Validity").put("message", "Se indicó el negocio Marketplace, el código EAN está vacío, sin embargo, el proveedor no se encuentra dentro de la lista permitida para esto.").put("fields", new org.json.JSONArray().put( "WHERL" ).put( "SupplierID")));
//						}
//					}
//				}else {
//					for(int t = 0; t< variants.length(); t+= 1) {
//						allPresent &= ( variants.getJSONObject(t).has("MainBarCodeS4H") || variants.getJSONObject(t).has("MainBarCode") ) && ( !"".equals(variants.getJSONObject(t).getString("MainBarCodeS4H")) || !"".equals(variants.getJSONObject(t).getString("MainBarCode")));
//						if(variants.getJSONObject(t).has("MainBarCodeS4H")) {
//							mainBarCode = variants.getJSONObject(t).has("MainBarCodeS4H") ? variants.getJSONObject(t).getString("MainBarCodeS4H") : variants.getJSONObject(t).getString("MainBarCode");
//							if(!"".equals(mainBarCode)) {
//								log("Got the value from the only variant present.");
//							}else {
//								log("The variant had also no main bar code after all. (Variant number: \"" + t + "\")");
//							}
//						}else {
//							mainBarCode = null;
//							log("Not even the variant contained value.");
//						}
//					}
//					if(mainBarCode != null && !"".equals(mainBarCode) && variants.length() == 1) {
//						newCharacteristicRecords.put( createCharacteristicValueObject("MainBarCodeS4H", mainBarCode) );
//					}else if(!allPresent) {
//						log("There was at least one variant with EAN missing... SUpplier: " + supplier);
//						if("Marketplace".equals(negocio) && !miraklExcepProvEAN.containsKey(supplier)) {
//							genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Validity").put("message", "Se indicó el negocio Marketplace, el código EAN está vacío, sin embargo, el proveedor no se encuentra dentro de la lista permitida para esto.").put("fields", new org.json.JSONArray().put( "WHERL" ).put( "SupplierID")));
//						}
//					}
//				}
//			}
//			log("!!!!!!!!!!! EAN (from only variant): " + mainBarCode);
//			if(mainBarCode != null && !"".equals(mainBarCode)) {
//				int lmbc = mainBarCode.length();
//				String typeMainBarCode =  lmbc == 8 ? "HK" : lmbc >= 6 && lmbc <= 12 ? "UC" : lmbc == 13 ? "HE" : lmbc == 14 ? "IC" : "IE";
//				newCharacteristicRecords.put( createCharacteristicValueObject(!"Suburbia".equals(negocio) ? "MainBarCode" : "MainBarCodeS4H", mainBarCode) );
//				newCharacteristicRecords.put( createCharacteristicValueObject(!"Suburbia".equals(negocio) ? "TypeMainBarCode" : "NUMTP_S4H", new org.json.JSONObject().put("_code", typeMainBarCode)) );
//			}else {
//				String typeMainBarCode = ("Liverpool".equals(negocio) || "Marketplace".equals(negocio) ? "IE" : "IS");
//				newCharacteristicRecords.put( createCharacteristicValueObject(!"Suburbia".equals(negocio) ? "TypeMainBarCode" : "NUMTP_S4H", new org.json.JSONObject().put("_code", typeMainBarCode)) );
//			}
//		}else {
//			String typeMainBarCode = ("Liverpool".equals(negocio) || "Marketplace".equals(negocio) ? "IE" : "IS");
//			newCharacteristicRecords.put( createCharacteristicValueObject(!"Suburbia".equals(negocio) ? "TypeMainBarCode" : "NUMTP_S4H", new org.json.JSONObject().put("_code", typeMainBarCode)) );
//		}
		/** Los valores de master pack no coinciden, (Ahorita si es igual a con empaque) **/
		/** Si en SAP un proveedor para Liverpool tiene el 10103 pero cataloga para 10101, de todos modos lo permite porque está dentro de la sección. En el caso
		 *  de Suburbia, en STEP se coloca el filtro. **/
		String sapBehvo = null;
		log("Vinimos a ver si itemGroup: " + itemGroup + ", con negocio: " + negocio);
		if(itemGroup != null && !"".equals(itemGroup)) {
			if("Liverpool".equals(negocio) || "Marketplace".equals(negocio)) {
				if(year == -1) {
					int month = Integer.parseInt( new java.text.SimpleDateFormat("MM").format(new java.util.Date()) );
					year = Integer.parseInt( new java.text.SimpleDateFormat("yyyy").format(new java.util.Date()) ) + (month < 11 ? 0 : 1);
					newCharacteristicRecords.put( createCharacteristicValueObject("AnoEstacion", String.valueOf(year) ) );
				}
				newCharacteristicRecords.put( createCharacteristicValueObject("Temporada", new org.json.JSONObject().put("_code", "0003") ) );
				sapBehvo = lookupValue(itemGroup, "GpoArtVsEnvase");
			}else if("Suburbia".equals(negocio)) {
				sapBehvo = lookupValue(itemGroup, "GpoArtVsEnvase_S4H");
				if(fshId != null && fshId.length() >= 4) {
					newCharacteristicRecords.put( createCharacteristicValueObject("FSH_SEASON_YEAR",  fshId.subSequence(0, 4)) );
				}
			}
			if(sapBehvo != null && !"".equals(sapBehvo)) {
				newCharacteristicRecords.put( createCharacteristicValueObject("SAP_BEHVO", new org.json.JSONObject().put("_code", sapBehvo.substring(0,2) )) );
				log("Got " + sapBehvo + " for SAP_BEHVO.");
				String thevalue = "1";
				try{
					org.json.JSONArray rws = new org.json.JSONObject( rc.getRequest("GET", baseUrl + "/list/StandardizationValue/bySearch"
							+ "?dictionaryProxy=" + java.net.URLEncoder.encode("'BEHVO_LookupTable'", "UTF-8")
							+ "&query=" + java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"BEHVO_LookupTable\" and StandardizationValue.Value equals \"" + sapBehvo.substring(0,2) + "\"", "UTF-8")
							+ "&fields=" + java.net.URLEncoder.encode("StandardizationValue.AlternativeValue", "UTF-8")
							, null) ).getJSONArray("rows");
					log("Checking sapBehvo: " + sapBehvo);
					if(rws.length() > 0) {
						thevalue = rws.getJSONObject(0).getJSONArray("values").getString(0);
					}
				}catch(org.json.JSONException e) {
					logE(e);
				}
				newCharacteristicRecords.put( createCharacteristicValueObject("ProductType",  new org.json.JSONObject().put("_code", thevalue) ) );
				log("Placing value: " + thevalue + " for ProductType");
			}else {
				log("No SAP_BEHVO found, placing value 1.");
				newCharacteristicRecords.put( createCharacteristicValueObject("ProductType",  new org.json.JSONObject().put("_code", "1") ) );
			}
		}
//		log("taste: " + newCharacteristicRecords);
		if(sections.contains("basicData")) {
			org.json.JSONArray fibrasObservadas = new org.json.JSONArray();
			String fiberPart1 = getCharacteristicValue( characteristicsMap.get("FIBER_PART1"), true );
			String fiberPart2 = getCharacteristicValue( characteristicsMap.get("FIBER_PART2"), true );
			String fiberPart3 = getCharacteristicValue( characteristicsMap.get("FIBER_PART3"), true );
			String fiberPart4 = getCharacteristicValue( characteristicsMap.get("FIBER_PART4"), true );
			String fiberPart5 = getCharacteristicValue( characteristicsMap.get("FIBER_PART5"), true );
			int boy = 0;
			int sum = 0;
			if( fiberPart1 != null && !"".equals(fiberPart1) ) {
				try {
					boy = new java.math.BigDecimal(fiberPart1).intValue();
					sum += boy;
					fibrasObservadas.put("FIBER_PART1");
					if( fiberPart2 != null && !"".equals(fiberPart2) ) {
						boy = new java.math.BigDecimal(fiberPart2).intValue();
						sum += boy;
						fibrasObservadas.put("FIBER_PART2");
						if( fiberPart3 != null && !"".equals(fiberPart3) ) {
							boy = new java.math.BigDecimal(fiberPart3).intValue();
							sum += boy;
							fibrasObservadas.put("FIBER_PART3");
							if( fiberPart4 != null && !"".equals(fiberPart4) ) {
								boy = new java.math.BigDecimal(fiberPart4).intValue();
								sum += boy;
								fibrasObservadas.put("FIBER_PART4");
								if(fiberPart5 != null && !"".equals(fiberPart5)) {
									boy = new java.math.BigDecimal(fiberPart5).intValue();
									sum += boy;
									fibrasObservadas.put("FIBER_PART5");
								}
							}
						}
					}
				}catch(NumberFormatException e) {
					logE(e);
				}
				if(sum != 100) {
					genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Precision").put("message", "Suma de Fibras incorrecta, favor de verificarla. (" + sum + ")").put("fields", fibrasObservadas));
				}
			}
		}

		if(sections.contains("datosVenta")) {
			String indicadorDeImpuesto = "Liverpool".equals(negocio) ? getCharacteristicValue( characteristicsMap.get("IndicadordeImpuesto"), true ) : "Marketplace".equals(negocio) ? "E2" : getCharacteristicValue( characteristicsMap.get("TAXESS4H"), true );
			String taxkm3S4H = !"".equals(indicadorDeImpuesto) && "Suburbia".equals(negocio) ? indicadorDeImpuesto.substring(2) : null;
//			String taxkm3S4H = !"".equals(indicadorDeImpuesto) && "Suburbia".equals(negocio) ? indicadorDeImpuesto.substring(0,1) : null;
			String ieps = "Liverpool".equals(negocio) ? null : "Marketplace".equals(negocio) ? "0" : !"".equals(indicadorDeImpuesto) ? 
					indicadorDeImpuesto.substring(1,2) : null;
			String impuestoALaVenta = "Liverpool".equals(negocio) ? null : "Marketplace".equals(negocio) ? "1" : !"".equals(indicadorDeImpuesto) ? indicadorDeImpuesto.substring(0,1) : null;
	
			if("Marketplace".equals(negocio)) {
				newCharacteristicRecords.put( createCharacteristicValueObject("IndicadordeImpuesto",  new org.json.JSONObject().put("_code", indicadorDeImpuesto) ) );
			}
			if(taxkm3S4H != null) {
				newCharacteristicRecords.put( createCharacteristicValueObject("TAXM3_S4H",  new org.json.JSONObject().put("_code", taxkm3S4H) ) );
			}
			if(ieps != null) {
				newCharacteristicRecords.put( createCharacteristicValueObject("Liverpool".equals(negocio) || "Marketplace".equals(negocio) ? "IEPS" : "TAXKM2_S4H",  new org.json.JSONObject().put("_code", ieps) ) );
			}
			if(impuestoALaVenta != null) {
				newCharacteristicRecords.put( createCharacteristicValueObject("Liverpool".equals(negocio) || "Marketplace".equals(negocio) ? "ImpuestoALaVenta" : "TAXKM1_S4H",  new org.json.JSONObject().put("_code", impuestoALaVenta) ) );
			}

			String costoBrutoSinIVAS = getCharacteristicValue( characteristicsMap.get("CostobrutoSinIVA"));
			String costoNetoSinIVAS = getCharacteristicValue( characteristicsMap.get("CostoNetoSinIVA"));
			String descuento1S = getCharacteristicValue( characteristicsMap.get("Descuento1"));
			String descuento2S = getCharacteristicValue( characteristicsMap.get("Descuento2"));

			java.math.BigDecimal costoBrutoSinIVA = costoBrutoSinIVAS != null && !"".equals(costoBrutoSinIVAS) ? new java.math.BigDecimal(costoBrutoSinIVAS) : null;
			java.math.BigDecimal costoNetoSinIVA = costoNetoSinIVAS != null && !"".equals(costoNetoSinIVAS) ? new java.math.BigDecimal(costoNetoSinIVAS) : null;
			java.math.BigDecimal descuento1 = descuento1S != null && !"".equals(descuento1S) ? new java.math.BigDecimal(descuento1S) : null;
			java.math.BigDecimal descuento2 = descuento2S != null && !"".equals(descuento2S) ? new java.math.BigDecimal(descuento2S) : null;

			if( ("Liverpool".equals(negocio) || "Suburbia".equals(negocio)) && costoBrutoSinIVA != null && descuento1 != null && descuento2 != null ) {
				log("Costo Bruto Sin IVA: " + costoBrutoSinIVA + ", Descuento1: " + descuento1 + ", Descuento2: " + descuento2);
				java.math.BigDecimal escala1 = descuento1.divide(java.math.BigDecimal.TEN.multiply(java.math.BigDecimal.TEN));
				java.math.BigDecimal escala2 = descuento2.divide(java.math.BigDecimal.TEN.multiply(java.math.BigDecimal.TEN));
				java.math.BigDecimal a = costoBrutoSinIVA.multiply(java.math.BigDecimal.ONE.subtract(escala1));
				java.math.BigDecimal b = a.multiply(escala2);
				java.math.BigDecimal c = a.subtract(b);
				costoNetoSinIVA = c; //( costoBrutoSinIVA.subtract (costoBrutoSinIVA.multiply( (descuento1.divide(new java.math.BigDecimal( 100 )) ))).multiply(descuento2.divide(new java.math.BigDecimal(100))).subtract  ((costoBrutoSinIVA.subtract (costoBrutoSinIVA.multiply(descuento1.divide(new java.math.BigDecimal(100)))))));
				costoNetoSinIVA.setScale(2, java.math.RoundingMode.HALF_UP);
				if(costoBrutoSinIVA != null) {
					if( costoNetoSinIVA != null && costoNetoSinIVA.floatValue() > 0f ) {
						newCharacteristicRecords.put( createCharacteristicValueObject("CostoNetoSinIVA", costoNetoSinIVA.toPlainString()) );
					}else {
						log("El costo neto sin iva, ya que no pasó la validación es: " + costoNetoSinIVA);
//					org.json.JSONObject j0 = null;
//					newCharacteristicRecords.put( j0 = createCharacteristicValueObject("Descuento1",  org.json.JSONObject.NULL ) );
//					log("Sent: " + j0);
//					newCharacteristicRecords.put( j0 = createCharacteristicValueObject("Descuento2",  org.json.JSONObject.NULL ) );
//					log("Sent: " + j0);
						genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence").put("message", "Se estableció costo bruto sin iva, pero costo neto sin iva " + (costoNetoSinIVA == null ? "no tiene un valor" : "es cero") + ".").put("fields", new org.json.JSONArray().put("CostobrutoSinIVA").put("CostoNetoSinIVA") ));
					}
				}
			}else
				if( "Marketplace".equals(negocio) ) {
					log("Vine aquí... ");
					newCharacteristicRecords.put( createCharacteristicValueObject("CostobrutoSinIVA",    ".01" ) );
					newCharacteristicRecords.put( createCharacteristicValueObject("PrecioSugeridocIVA",  ".01" ) );
					newCharacteristicRecords.put( createCharacteristicValueObject("CostoNetoSinIVA",     ".01" ) );
				}
		}else if("Marketplace".equals(negocio)) {
			log("Porque Marketpleis...");
			String indicadorDeImpuesto =  "E2" ;
			String ieps = "0";
			String impuestoALaVenta = "Liverpool".equals(negocio) ? null : "Marketplace".equals(negocio) ? "1" : !"".equals(indicadorDeImpuesto) ? indicadorDeImpuesto.substring(2) : null;
	
			newCharacteristicRecords.put( createCharacteristicValueObject("IndicadordeImpuesto",  new org.json.JSONObject().put("_code", indicadorDeImpuesto) ) );
			if(ieps != null) {
				newCharacteristicRecords.put( createCharacteristicValueObject("IEPS",  new org.json.JSONObject().put("_code", ieps) ) );
			}
			if(impuestoALaVenta != null) {
				newCharacteristicRecords.put( createCharacteristicValueObject( "ImpuestoALaVenta",  new org.json.JSONObject().put("_code", impuestoALaVenta) ) );
			}

			log("Vine aquí... ");
			newCharacteristicRecords.put( createCharacteristicValueObject("CostobrutoSinIVA",    ".01" ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("PrecioSugeridocIVA",  ".01" ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("CostoNetoSinIVA",     ".01" ) );
		}else {
			log("No ps tampoco..." + negocio);
		}

		if(sections.contains("logisticData")) {
			String unidadDeMedidaLongitud = getCharacteristicValue( characteristicsMap.get("UnidadDeMedidaLongitud"), true);
			newCharacteristicRecords.put(createCharacteristicValueObject("UnidadDeMedidaVolumen", "CM".equals(unidadDeMedidaLongitud) ?  new org.json.JSONObject().put("_code", "CM3") : "M".equals(unidadDeMedidaLongitud) ?  new org.json.JSONObject().put("_code", "M3") :  new org.json.JSONObject().put("_code", "MM3")));
			log("Ajua: " + ("CM".equals(unidadDeMedidaLongitud) ?  new org.json.JSONObject().put("_code", "CM3") : "M".equals(unidadDeMedidaLongitud) ?  new org.json.JSONObject().put("_code", "M3") :  new org.json.JSONObject().put("_code", "MM3")));
			
			boolean llenas = true;
			boolean llenasConEmpaque = true;
			boolean llenasMasterPack = true;
	
			String productWidth  = getCharacteristicValue( characteristicsMap.get("ProductWidth"));
			String productDepth  = getCharacteristicValue( characteristicsMap.get("ProductDepth"));
			String productHeight = getCharacteristicValue( characteristicsMap.get("ProductHeight"));
			log("pw: " + productWidth);
			log("pd: " + productDepth);
			log("ph: " + productHeight);
			
			try{
				log("El volum att: " + (new java.math.BigDecimal(productWidth).multiply(new java.math.BigDecimal(productDepth).multiply(new java.math.BigDecimal(productHeight))).setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() ));
			}catch(NumberFormatException e) {
				
			}
			
			try{
				newCharacteristicRecords.put(createCharacteristicValueObject("VOLUMAtt", new java.math.BigDecimal(productWidth).multiply(new java.math.BigDecimal(productDepth).multiply(new java.math.BigDecimal(productHeight))).setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() ));
			}catch(NullPointerException | NumberFormatException e) {
				
			}
			
//			String volume = getCharacteristicValue( characteristicsMap.get("VOLUMAtt"));
			String pesoBruto = getCharacteristicValue( characteristicsMap.get("PesoBruto"));
			String productWeight = getCharacteristicValue( characteristicsMap.get("ProductWeight"));
	
			if(
				!(
					(productWidth == null || "".equals(productWidth)) &&
					(productDepth == null || "".equals(productDepth)) &&
					(productHeight == null || "".equals(productHeight)) &&
//					(volume == null || "".equals(volume)) &&
					(productWeight == null || "".equals(productWeight))
				)
			) {
				if(
					(productWidth == null || "".equals(productWidth)) ||
					(productDepth == null || "".equals(productDepth)) ||
					(productHeight == null || "".equals(productHeight)) ||
//					(volume == null || "".equals(volume)) ||
					(productWeight == null || "".equals(productWeight))
				) {
					org.json.JSONArray aux = new org.json.JSONArray();
					if(productWidth == null || "".equals(productWidth)) {
						aux.put("ProductWidth");
					}
					if(productDepth == null || "".equals(productDepth)) {
						aux.put("ProductDepth");
					}
					if(productHeight == null || "".equals(productHeight)) {
						aux.put("ProductHeight");
					}
//					if(volume == null || "".equals(volume)) {
//						aux.put("VOLUMAtt");
//					}
					if(productWeight == null || "".equals(productWeight)) {
						aux.put("ProductWeight");
					}
					genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence")
							.put("message", "Hay medidas sin empaque faltantes.").put("fields", aux));
					llenas = false;
				}
			}else {
				llenas = false;
			}
	
			//Con empaque
			String anchoConEmpaque = getCharacteristicValue( characteristicsMap.get("ZBRECJ"));
			String largoConEmpaque = getCharacteristicValue( characteristicsMap.get("ZLAECJ"));
			String altoConEmpaque = getCharacteristicValue( characteristicsMap.get("ZHOECJ"));
			log("ace: " + anchoConEmpaque);
			log("lce: " + largoConEmpaque);
			log("alce: " + altoConEmpaque);
			try {
				log("El volum att: " + (String.valueOf( new java.math.BigDecimal(anchoConEmpaque).multiply(new java.math.BigDecimal(largoConEmpaque).multiply(new java.math.BigDecimal(altoConEmpaque))).setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() )));
			}catch(NumberFormatException e) {
				logE(e);
			}
			try{
				newCharacteristicRecords.put(createCharacteristicValueObject("ZVOLCJ", new java.math.BigDecimal(anchoConEmpaque).multiply(new java.math.BigDecimal(largoConEmpaque).multiply(new java.math.BigDecimal(altoConEmpaque))).setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() ) );
			}catch(NullPointerException | NumberFormatException e) {
				
			}
			
//			String volumenConEmpaque = getCharacteristicValue( characteristicsMap.get("ZVOLCJ"));
			String pesoBrutoConEmpaque = getCharacteristicValue( characteristicsMap.get("ZBRGCJ"));
			String pesoNetoConEmpaque = getCharacteristicValue( characteristicsMap.get("ZNTGCJ"));
	
			if(
				(anchoConEmpaque != null && !"".equals(anchoConEmpaque)) &&
				(largoConEmpaque != null && !"".equals(largoConEmpaque)) &&
				(altoConEmpaque != null && !"".equals(altoConEmpaque)) &&
//				(volumenConEmpaque != null && !"".equals(volumenConEmpaque)) &&
				(pesoBrutoConEmpaque != null && !"".equals(pesoBrutoConEmpaque)) /* &&
				(pesoNetoConEmpaque != null && !"".equals(pesoNetoConEmpaque)) */
			) {
				if(
					(anchoConEmpaque == null || "".equals(anchoConEmpaque)) ||
					(largoConEmpaque == null || "".equals(largoConEmpaque)) ||
					(altoConEmpaque == null || "".equals(altoConEmpaque)) ||
//					(volumenConEmpaque == null || "".equals(volumenConEmpaque)) ||
					(pesoBrutoConEmpaque == null || "".equals(pesoBrutoConEmpaque)) /* ||
					(pesoNetoConEmpaque == null || "".equals(pesoNetoConEmpaque)) */
				) {
					org.json.JSONArray aux = new org.json.JSONArray();
					if(anchoConEmpaque == null || "".equals(anchoConEmpaque)) {
						aux.put("ZBRECJ");
					}
					if(largoConEmpaque == null || "".equals(largoConEmpaque)) {
						aux.put("ZLAECJ");
					}
					if(altoConEmpaque == null || "".equals(altoConEmpaque)) {
						aux.put("ZHOECJ");
					}
//					if(volumenConEmpaque == null || "".equals(volumenConEmpaque)) {
//						aux.put("ZVOLCJ");
//					}
					if(pesoBrutoConEmpaque == null || "".equals(pesoBrutoConEmpaque)) {
						aux.put("ZBRGCJ");
					}
	//				if(pesoNetoConEmpaque == null || "".equals(pesoNetoConEmpaque)) {
	//					aux.put("ZNTGCJ");
	//				}
					genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence")
							.put("message", "Hay medidas con empaque faltantes.").put("fields", aux));
					llenasConEmpaque = false;
				}
			}else {
				llenasConEmpaque = false;
			}
	
			//Masterpack
			String altoMP = getCharacteristicValue( characteristicsMap.get("ZHOEPQ"));
			String anchoMP = getCharacteristicValue( characteristicsMap.get("ZBREPQ"));
			String largoMP = getCharacteristicValue( characteristicsMap.get("ZLAEPQ"));
	
			try{
				newCharacteristicRecords.put(createCharacteristicValueObject("ZVOLPQ", new java.math.BigDecimal(altoMP).multiply(new java.math.BigDecimal(anchoMP).multiply(new java.math.BigDecimal(largoMP))).setScale(4, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() ) );
			}catch(NullPointerException | NumberFormatException e) {
				
			}
			
//			String masterPack = getCharacteristicValue( characteristicsMap.get("ZVOLPQ"));
			String pesoBrutoMP = getCharacteristicValue( characteristicsMap.get("ZBRGPQ"));
			String pesoNetoMP = getCharacteristicValue( characteristicsMap.get("ZNTGPQ"));
	
			log("<:::::>productWeight: " + productWeight);
			log("pesoBruto: " + pesoBruto);
			log("pesoBrutoConEmpaque: " + pesoBrutoConEmpaque);
			log("pesoNetoConEmpaque: " + pesoNetoConEmpaque);
			log("pesoBrutoMP: " + pesoBrutoMP);
			log("pesoNetoMP: " + pesoNetoMP);
	
			if(
				(altoMP != null && !"".equals(altoMP)) &&
				(anchoMP != null && !"".equals(anchoMP)) &&
				(largoMP != null && !"".equals(largoMP)) &&
//				(masterPack != null && !"".equals(masterPack)) &&
				(pesoBrutoMP != null && !"".equals(pesoBrutoMP)) &&
				(pesoNetoMP != null && !"".equals(pesoNetoMP))
			) {
				if(
					(altoMP == null || "".equals(altoMP)) ||
					(anchoMP == null || "".equals(anchoMP)) ||
					(largoMP == null || "".equals(largoMP)) ||
//					(masterPack == null || "".equals(masterPack)) ||
					(pesoBrutoMP == null || "".equals(pesoBrutoMP)) ||
					(pesoNetoMP == null || "".equals(pesoNetoMP))
				) {
					org.json.JSONArray aux = new org.json.JSONArray();
					if(altoMP == null || "".equals(altoMP)) {
						aux.put("ZHOEPQ");
					}
					if(anchoMP == null || "".equals(anchoMP)) {
						aux.put("ZBREPQ");
					}
					if(largoMP == null || "".equals(largoMP)) {
						aux.put("ZLAEPQ");
					}
//					if(masterPack == null || "".equals(masterPack)) {
//						aux.put("ZVOLPQ");
//					}
					if(pesoBrutoMP == null || "".equals(pesoBrutoMP)) {
						aux.put("ZBRGPQ");
					}
					if(pesoNetoMP == null || "".equals(pesoNetoMP)) {
						aux.put("ZNTGPQ");
					}
					genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence")
							.put("message", "Hay medidas master pack faltantes.").put("fields", aux));
					llenasMasterPack = false;
				}
			}else {
				llenasMasterPack = false;
			}
	
			if(llenasMasterPack && !llenas) {
				org.json.JSONArray aux = new org.json.JSONArray();
				if(productWidth == null || "".equals(productWidth)) {
					aux.put("ProductWidth");
				}
				if(productDepth == null || "".equals(productDepth)) {
					aux.put("ProductDepth");
				}
				if(productHeight == null || "".equals(productHeight)) {
					aux.put("ProductHeight");
				}
//				if(volume == null || "".equals(volume)) {
//					aux.put("VOLUMAtt");
//				}
				if(pesoBruto == null || "".equals(pesoBruto)) {
					aux.put("PesoBruto");
				}
				if(productWeight == null || "".equals(productWeight)) {
					aux.put("ProductWeight");
				}
				genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence")
						.put("message", "Hay medidas sin empaque faltantes, ya que las medidas master pack han sido proporcionadas.").put("fields", aux));
			}
			if(llenasConEmpaque && !llenas) {
				org.json.JSONArray aux = new org.json.JSONArray();
				if(productWidth == null || "".equals(productWidth)) {
					aux.put("ProductWidth");
				}
				if(productDepth == null || "".equals(productDepth)) {
					aux.put("ProductDepth");
				}
				if(productHeight == null || "".equals(productHeight)) {
					aux.put("ProductHeight");
				}
//				if(volume == null || "".equals(volume)) {
//					aux.put("VOLUMAtt");
//				}
				if(pesoBruto == null || "".equals(pesoBruto)) {
					aux.put("PesoBruto");
				}
				if(productWeight == null || "".equals(productWeight)) {
					aux.put("ProductWeight");
				}
				genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence")
						.put("message", "Hay medidas sin empaque faltantes, ya que las medidas con empaque han sido proporcionadas.").put("fields", aux));
			}
			if(llenas && llenasConEmpaque) {
				org.json.JSONArray aux = new org.json.JSONArray();
				if( new java.math.BigDecimal(productWidth).compareTo(new java.math.BigDecimal(anchoConEmpaque)) >= 0 ) {
	//				aux.put("ProductWidth");
	//				aux.put("ZBRECJ");
				}
				if( new java.math.BigDecimal(productDepth).compareTo(new java.math.BigDecimal(largoConEmpaque)) >= 0 ) {
	//				aux.put("ProductDepth");
	//				aux.put("ZLAECJ");
				}
				if( new java.math.BigDecimal(productHeight).compareTo(new java.math.BigDecimal(altoConEmpaque)) >= 0 ) {
	//				aux.put("ProductHeight");
	//				aux.put("ZHOECJ");
				}
//				if( new java.math.BigDecimal(volume).compareTo(new java.math.BigDecimal(volumenConEmpaque)) >= 0 ) {
	//				aux.put("VOLUMAtt");
	//				aux.put("ZVOLCJ");
//				}
				try {
					if(pesoBruto != null && !"".equals(pesoBruto) && new java.math.BigDecimal(pesoBruto).compareTo(new java.math.BigDecimal(pesoBrutoConEmpaque)) > 0 ) {
						aux.put("PesoBruto");
						aux.put("ZBRGCJ");
					}
				}catch(NumberFormatException e) {
					log("PB: " + pesoBruto + " || " + pesoBrutoConEmpaque);
				}
				try {
					if(pesoNetoConEmpaque != null && !"".equals(pesoNetoConEmpaque) && new java.math.BigDecimal(productWeight).compareTo(new java.math.BigDecimal(pesoNetoConEmpaque)) > 0 ) {
						aux.put("ProductWeight");
						aux.put("ZNTGCJ");
					}
				}catch(NumberFormatException e) {
					log("PNCE: " + pesoNetoConEmpaque + " || " + productWeight);
				}
				if(aux.length() > 0) {
					genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence")
							.put("message", "Hay medidas sin empaque mayores que las de con empaque.").put("fields", aux));
				}
			}
			if(llenas && llenasMasterPack) {
				org.json.JSONArray aux = new org.json.JSONArray();
				if( new java.math.BigDecimal(productWidth).compareTo(new java.math.BigDecimal(anchoMP)) >= 0 ) {
					aux.put("ProductWidth");
					aux.put("ZBREPQ");
				}
				if( new java.math.BigDecimal(productDepth).compareTo(new java.math.BigDecimal(largoMP)) >= 0 ) {
					aux.put("ProductDepth");
					aux.put("ZLAEPQ");
				}
				if( new java.math.BigDecimal(productHeight).compareTo(new java.math.BigDecimal(altoMP)) >= 0 ) {
					aux.put("ProductHeight");
					aux.put("ZHOEPQ");
				}
//				if( new java.math.BigDecimal(volume).compareTo(new java.math.BigDecimal(masterPack)) >= 0 ) {
//					aux.put("VOLUMAtt");
//					aux.put("ZVOLPQ");
//				}
				try {
					if(pesoBruto != null && !"".equals(pesoBruto) && new java.math.BigDecimal(pesoBruto).compareTo(new java.math.BigDecimal(pesoBrutoMP)) >= 0 ) {
						aux.put("PesoBruto");
						aux.put("ZBRGPQ");
					}
				}catch(NumberFormatException e) {
					log("PNCE: " + pesoNetoConEmpaque + " || " + productWeight);
				}
				if( new java.math.BigDecimal(productWeight).compareTo(new java.math.BigDecimal(pesoNetoMP)) >= 0 ) {
					aux.put("ProductWeight");
					aux.put("ZNTGPQ");
				}
				if(aux.length() > 0) {
					genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence").put("message", "Hay medidas con empaque mayores que las de master pack.").put("fields", aux));
				}
			}
			if(llenasConEmpaque && llenasMasterPack) {
				org.json.JSONArray aux = new org.json.JSONArray();
				if( new java.math.BigDecimal(anchoConEmpaque).compareTo(new java.math.BigDecimal(anchoMP)) >= 0 ) {
					aux.put("ZBRECJ");
					aux.put("ZBREPQ");
				}
				if( new java.math.BigDecimal(largoConEmpaque).compareTo(new java.math.BigDecimal(largoMP)) >= 0 ) {
					aux.put("ZLAECJ");
					aux.put("ZLAEPQ");
				}
				if( new java.math.BigDecimal(altoConEmpaque).compareTo(new java.math.BigDecimal(altoMP)) >= 0 ) {
					aux.put("ZHOECJ");
					aux.put("ZHOEPQ");
				}
//				if( new java.math.BigDecimal(volumenConEmpaque).compareTo(new java.math.BigDecimal(masterPack)) >= 0 ) {
//					aux.put("ZVOLCJ");
//					aux.put("ZVOLPQ");
//				}
				if( new java.math.BigDecimal(pesoBrutoConEmpaque).compareTo(new java.math.BigDecimal(pesoBrutoMP)) >= 0 ) {
					aux.put("ZBRGCJ");
					aux.put("ZBRGPQ");
				}
				if(pesoNetoConEmpaque != null && !"".equals(pesoNetoConEmpaque) && new java.math.BigDecimal(pesoNetoConEmpaque).compareTo(new java.math.BigDecimal(pesoNetoMP)) >= 0 ) {
					aux.put("ZNTGCJ");
					aux.put("ZNTGPQ");
				}
				if(aux.length() > 0) {
					genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence").put("message", "Hay medidas con empaque mayores que las de master pack.").put("fields", aux));
				}
			}
	
			try {
				if(pesoBruto != null && !"".equals(pesoBruto) && productWeight != null && !"".equals(productWeight)) {
					if(new java.math.BigDecimal(productWeight).compareTo(new java.math.BigDecimal(pesoBruto)) > 0) {
						log("productWeight: " + productWeight);
						log("pesoBruto: " + pesoBruto);
						genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence")
								.put("message", "Peso bruto sin empaque es menor o igual al peso neto sin empaque .")
								.put("fields", new org.json.JSONArray().put( "ProductWeight" ).put( "PesoBruto" ) )
								.put("values", new org.json.JSONArray().put( productWeight ).put( pesoBruto ) )
								);
					}
				}
			}catch(NumberFormatException e) {
				log("Exception in number format, PesoNetoSE: " + productWeight + ", PesoBrutoSE: " + pesoBruto);
			}
			try {
				if(pesoBrutoConEmpaque != null && !"".equals(pesoBrutoConEmpaque) && pesoNetoConEmpaque != null && !"".equals(pesoNetoConEmpaque)) {
					if(new java.math.BigDecimal(pesoBrutoConEmpaque).compareTo(new java.math.BigDecimal(pesoNetoConEmpaque)) < 0) {
						log("pesoBrutoConEmpaque: " + pesoBrutoConEmpaque);
						log("pesoNetoConEmpaque: " + pesoNetoConEmpaque);
						genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence")
								.put("message", "Peso bruto con empaque es menor o igual que peso neto con empaque.")
								.put("fields", new org.json.JSONArray().put( "ZBRGCJ" ).put( "ZNTGCJ" ) )
								.put("values", new org.json.JSONArray().put( pesoBrutoConEmpaque ).put( pesoNetoConEmpaque ) )
								);
					}
				}
			}catch(NumberFormatException e) {
				log("Exception in number format, PesoBrutoCE: " + pesoBrutoConEmpaque + ", PesoNetoCE: " + pesoNetoConEmpaque);
			}
	
			try {
				if( pesoBrutoMP != null && !"".equals(pesoBrutoMP) && pesoNetoMP != null && !"".equals(pesoNetoMP)) {
					if(new java.math.BigDecimal(pesoBrutoMP).compareTo(new java.math.BigDecimal(pesoNetoMP)) < 0) {
						log("pesoBrutoMP: " + pesoBrutoMP);
						log("pesoNetoMP: " + pesoNetoMP);
						genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence")
								.put("message", "Peso bruto master pack es menor o igual al peso neto master pack.")
								.put("fields", new org.json.JSONArray().put( "ZBRGPQ" ).put( "ZNTGPQ" ) )
								.put("values", new org.json.JSONArray().put( pesoBrutoMP ).put( pesoNetoMP ) )
								);
					}
				}
			}catch(NumberFormatException e) {
				log("Exception in number format, PesoBrutoMP: " + pesoBrutoMP + ", PesoNetoMP: " + pesoNetoMP);
			}
		}

		String[] direccionSeccion = getDireccionSeccion(itemGroup, negocio);
		if(direccionSeccion != null) {
			newCharacteristicRecords.put( createCharacteristicValueObject("Direction", new org.json.JSONObject().put("_code", direccionSeccion[0]) ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("Section", new org.json.JSONObject().put("_code", direccionSeccion[1] )) );
		}

//		if(newCharacteristicRecords.length() > 0) {
//			log("************* RECATALOGACIÓN *************");
//			log(newCharacteristicRecords.toString());
//			log("******************************************");
//			rr = rc.getRequest("PUT", baseUrl + "/object/Product2G/'" + proposalId + "'@'MASTER'",
//					new org.json.JSONObject()
//						.put("_characteristicRecords",
//								newCharacteristicRecords).toString());
//			log("Actualizamos el ese: " + rr);
//		}
		
	}
	
	public void getItemGroupFromIA(String productName, String template, String productTypeSAP, String productDescription, String templateName, org.json.JSONArray newCharacteristicRecords) throws IOException {
		long init = System.currentTimeMillis();
		String itemGroup = null;
//		String jsonKeyPath = PropertiesManager.get("p360.contingency.gcp.ia_itemgroup_sa"); // "/u01/stage/dev.json";// "/P360shared/IDMC/dev.json";
        String targetAudience = PropertiesManager.get("p360.contingency.gcp.ia_itemgroup_url_ta"); // "https://service-idga-prediction-335803992526.us-central1.run.app/api/post_iga_prediction";
        
		try {
			// Transporte HTTP
            HttpTransport transport = new NetHttpTransport();

            // Carga credenciales y crea ID token (para Cloud Run personalizado)
//            IdTokenCredentials credentials = IdTokenCredentials.newBuilder()
//                .setIdTokenProvider((IdTokenProvider) GoogleCredentials.fromStream(new FileInputStream(jsonKeyPath)))
//                .setTargetAudience(PropertiesManager.get("p360.contingency.gcp.ia_itemgroup_url"))
//                .build();

//            credentials.refresh();
//            String idToken = credentials.getAccessToken().getTokenValue();

            // Construye URL destino
            GenericUrl url = new GenericUrl(targetAudience);
            log("Querying IA for Item group with: ProductName: " + productName + ", Template: " + template + ", ProductTypeSAP: " + productTypeSAP + ", Desc: " + productDescription);
			org.json.JSONObject body = new org.json.JSONObject().put("input", new org.json.JSONArray().put( new org.json.JSONObject()
					.put("pim_product_name", productName)
					.put("pim_template_id", template)
					.put("product_type_sap", productTypeSAP)
					.put("product_description", productDescription == null || "".equals(productDescription) ? templateName : productDescription)
					.put("image", "")));
            HttpContent content = new ByteArrayContent("application/json", 
            		body.toString().getBytes());
            log("Using body for AI ItemGroup request: " + body);
            // Construye request
            HttpRequestFactory requestFactory = transport.createRequestFactory();
            HttpRequest request = requestFactory.buildPostRequest(url, content);
//            request.getHeaders().setAuthorization("Bearer " + idToken);
            // Timeouts opcionales
            request.setConnectTimeout(Integer.parseInt( PropertiesManager.get("p360.contingency.gcp.ia_itemgroup_connect_timeout") ));
            request.setReadTimeout( Integer.parseInt( PropertiesManager.get("p360.contingency.gcp.ia_itemgroup_read_timeout") ) );

            // Ejecuta la petición
            HttpResponse response = request.execute();
            log("Response status: " + response.getStatusCode());
            String rsp = response.parseAsString();
            log("Response body: " + rsp);
            org.json.JSONObject jsonResponse = null;
            jsonResponse = new org.json.JSONArray( rsp ).getJSONObject(0);
			
			String direction = String.valueOf( jsonResponse.get("direction") );
			String section = String.valueOf( jsonResponse.get("section") );
			itemGroup = String.valueOf( jsonResponse.get("item_group") );
			newCharacteristicRecords.put( createCharacteristicValueObject("ItemGroup",  new org.json.JSONObject().put("_code", itemGroup) ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("Section",  new org.json.JSONObject().put("_code", section) ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("Direction",  new org.json.JSONObject().put("_code", direction) ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("ItemGroupIAConfidenceDir", String.valueOf( jsonResponse.getDouble("direction_confidence") ) ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("ItemGroupIAConfidenceSec", String.valueOf( jsonResponse.getDouble("section_confidence") ) ) );
			newCharacteristicRecords.put( createCharacteristicValueObject("ItemGroupIAConfidenceIG",  String.valueOf( jsonResponse.getDouble("item_group_confidence") ) ) );
		}catch(Exception e) {
			genericFieldErrors.put(new org.json.JSONObject().put("message", "Error al calcular grupo de artículos desde la IA.").put("fields", new org.json.JSONArray() /* .put("ProductTypeSAP").put("Name") */ ));
			logE(e);
		}
		log("La IA took: " + workshop.formatTime(System.currentTimeMillis() - init));
	}
	
	public String getCodigoSBB(String cammelCase) throws ServiceUnavailableException {
		RESTWorkshop rw = new RESTWorkshop();
		rw.setBaseUrl(workshop.getBaseUrl());
		rw.addHeader("Authorization", rw.getRc().getHeader().get("Authorization"));
		rw.putParameter("lookup", "SB_COLORESLOV");
		rw.putParameter("fields", "LookupValue.Code");
		rw.putParameter("query", "LookupValue.IsActive = true and LookupValueLang.Name(es) equals \"" + cammelCase + "\"");

		org.json.JSONObject response = null;

		response = rw.makeRequest("GET", "/list/LookupValue/bySearch");

		return response == null ? null : response.has("rows") && response.getJSONArray("rows").length() > 0 ? response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0) : null;

	}

	public String toCammelCase(String value) {
		StringBuilder sb = new StringBuilder();
		String piece = null;
		java.util.LinkedList<String> tokens = new java.util.LinkedList<>();
		for(int i=0; i<value.length(); i++) {
			piece = value.substring(i, i+1);
			if(tokens.isEmpty()) {
				tokens.addLast(piece.toUpperCase());
			}else {
				if(" ".equals(tokens.getLast())) {
					tokens.addLast(piece.toUpperCase());
				}else {
					tokens.addLast(piece.toLowerCase());
				}
			}
		}
		for(String t : tokens) {
			sb.append(t);
		}
		return sb.toString();
	}

	public boolean isValidEan11(String ean) {
        if (ean == null || !ean.matches("\\d{11}")) {
			return false;
		}
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            int digit = Character.getNumericValue(ean.charAt(i));
            sum += (i % 2 == 0) ? digit * 3 : digit;
        }
        int checksum = (10 - (sum % 10)) % 10;
        return checksum == Character.getNumericValue(ean.charAt(10));
    }

	public boolean isValidEan8(String ean) {
        if (ean == null || !ean.matches("\\d{8}")) {
			return false;
		}
        int sum = 0;
        for (int i = 0; i < 7; i++) {
            int digit = Character.getNumericValue(ean.charAt(i));
            sum += (i % 2 == 0) ? digit * 3 : digit;
        }
        int checksum = (10 - (sum % 10)) % 10;
        return checksum == Character.getNumericValue(ean.charAt(7));
    }

	public boolean isValidUPCA(String ean) {
        if (ean == null || !ean.matches("\\d{12}")) {
			return false;
		}
        int sum = 0;
        for (int i = 10; i >= 0; i--) {
            int digit = Character.getNumericValue(ean.charAt(i));
            sum += ((-1*(i-11)) % 2 == 0) ? digit : digit*3;
        }
        int checksum = (10 - (sum % 10)) % 10;
        return checksum == Character.getNumericValue(ean.charAt(11));
    }

	public boolean isValidEan13(String ean) {
        if (ean == null || !ean.matches("\\d{13}")) {
			return false;
		}
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(ean.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        int checksum = (10 - (sum % 10)) % 10;
        return checksum == Character.getNumericValue(ean.charAt(12));
    }

	private void checkVariantMainBarCode(org.json.JSONObject variant, String negocio, String supplier, int varNumber, String[] typeMainBarCodeA) throws ServiceUnavailableException {
		
		log("\n\t---> Got called for variant main bar code: " + (variant.has("variantId") ? variant.getString("variantId") : "new variant" ));
		
		String mainBarCode = null;

		int lmbc = 0;
		if(variant.has("MainBarCode") || variant.has("MainBarCodeS4H")) {
			mainBarCode = variant.has("MainBarCodeS4H") ? variant.getString("MainBarCodeS4H") : variant.getString("MainBarCode");
			mainBarCode = mainBarCode == null ? "" : mainBarCode.replaceFirst("^0+", "").replaceAll("\s{2,}", " ").trim();
			lmbc = mainBarCode == null ? 0 : mainBarCode.length();
			String typeMainBarCode = null; 
			try{
				log("Came here (ean computations)");
				typeMainBarCode = (mainBarCode == null || "".equals(mainBarCode)) ? ("Liverpool".equals(negocio) || "Marketplace".equals(negocio) ? "IE" : "IS") : ( lmbc == 8 ? "HK" : lmbc >= 6 && lmbc <= 12 ? getTypeMainBarCode(mainBarCode, negocio) : lmbc == 13 ? Long.parseLong(mainBarCode) < 3000_000_000_000l ? "EE" : "HE" : lmbc == 14 ? "IC" : "IE" );
			}catch(NumberFormatException e) {
				variantFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Validity").put("message", "El código EAN no corresponde con un número válido.").put("fields", new org.json.JSONArray().put( "MainBarCode" )));
			}
			log("EAN: " + typeMainBarCode + " (ean computations)");
			if("".equals(typeMainBarCode) && lmbc > 0) {
				variantFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Validity").put("message", "El código EAN no corresponde con una longitud válida.").put("fields", new org.json.JSONArray().put( "MainBarCode" )));
			}else {

				if(!"".equals(typeMainBarCode)) {
					if(!"Suburbia".equals(negocio)) {
						variant.put("TypeMainBarCode", typeMainBarCode);
					}else {
						log("\n\t----------------------->" + negocio + ", " + typeMainBarCode + ", " + mainBarCode);
						variant.put("NUMTP_S4H", typeMainBarCode);
						if(!variant.has("MainBarCodeS4H")) {
							variant.put("MainBarCodeS4H", mainBarCode);
							log("Setting MainBarCodeS4H from MainBarCode");
						}
					}
				}

			}
			typeMainBarCodeA[0] = typeMainBarCode;
			boolean dup = false;
			DataRequestor dr = new DataRequestor();
			log("Now for dup on: " + mainBarCode);
			String res = dr.supplierAIDByEAN(new org.json.JSONArray().put(mainBarCode));
			if(res != null) {
				try {
					org.json.JSONObject jr = new org.json.JSONObject(res);
					org.json.JSONArray items = jr.getJSONArray("items");
					log("i) Got: " + jr);
					if(!"".equals(items.getString(0))) {
						log("Within checking variants EAN, got business for a coincidence: " + items.getString(0) + " vs " + negocio + " of currentEvaluating id (" + (variant.has("variantId") ? variant.getString("variantId") : "NoVaID" ) + ")");
						if(!variant.has("variantId") || !items.getString(0).equals(variant.getString("variantId"))) {
							res = dr.getProductByVariant(new org.json.JSONArray().put(items.getString(0)));
							if(res != null) {
								try {
									jr = new org.json.JSONObject(res);
									org.json.JSONArray items0 = jr.getJSONArray("items");
									log("Got product: " + items0.getString(0));
									res = dr.getProductData(new org.json.JSONArray().put(items0.getString(0)));
									if(res != null) {
										java.util.Set<String> vars = dr.getVariants(items.getString(0));
										if(!variant.has("variantId") || !vars.contains(variant.getString("variantId"))) {
											try {
												jr = new org.json.JSONObject(res);
												org.json.JSONArray items1 = jr.getJSONArray("items");
												String business = items1.getJSONObject(0).getString("Business");
												business = "MKP".equals(business) ? "Marketplace" : "LVP".equals(business) ? "Liverpool" : "SBB".equals(business) ? "Suburbia" : business;
												log("Got business: " + business);
												if(business.equals(negocio) && "Marketplace".equals(negocio)) {
													variantFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence").put("message", "El código EAN ya existe en catalogación para este negocio, se tiene que hacer una multioferta.").put("values", new org.json.JSONArray().put( variant.has("MainBarCode") ? variant.getString("MainBarCode") : variant.has("MainBarCodeS4H") ? variant.getString("MainBarCodeS4H") : "" )).put("fields", new org.json.JSONArray().put( "MainBarCode" )));
												}else if(business.equals(negocio) && !"Marketplace".equals(negocio)) {
													variantFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence").put("message", "El código EAN ya existe en catalogación para este negocio.").put("values", new org.json.JSONArray().put( variant.has("MainBarCode") ? variant.getString("MainBarCode") : variant.has("MainBarCodeS4H") ? variant.getString("MainBarCodeS4H") : "" )).put("fields", new org.json.JSONArray().put( "MainBarCode" )));
												}else {
													if("Marketplace".equals(negocio) && "Liverpool".equals(business)) {
														variantFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence").put("message", "El código EAN ya existe para Liverpool, se tiene que realizar Stockout.").put("values", new org.json.JSONArray().put( variant.has("MainBarCode") ? variant.getString("MainBarCode") : variant.has("MainBarCodeS4H") ? variant.getString("MainBarCodeS4H") : "" )).put("fields", new org.json.JSONArray().put( "MainBarCode" )));
													}else if("Liverpool".equals(negocio) && "Marketplace".equals(business)) {
														variantFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence").put("message", "El código EAN ya existe para Marketplace, se tiene que liberar el EAN.").put("values", new org.json.JSONArray().put( variant.has("MainBarCode") ? variant.getString("MainBarCode") : variant.has("MainBarCodeS4H") ? variant.getString("MainBarCodeS4H") : "" )).put("fields", new org.json.JSONArray().put( "MainBarCode" )));
													}
												}
												log("! " + variantFieldErrors + " !");
												dup = true;
											}catch(org.json.JSONException e) {
												logE(e);
											}
										}
									}
								}catch(org.json.JSONException e) {
									logE(e);
								}
							}
						}
					}
				}catch(org.json.JSONException e) {
					logE(e);
				}
			}
			if(!dup) {
				res = dr.productNoByEAN(new org.json.JSONArray().put(mainBarCode));
				if(res != null) {
					try {
						org.json.JSONObject jr = new org.json.JSONObject(res);
						org.json.JSONArray items = jr.getJSONArray("items");
						if(!"".equals(items.getString(0))) {
							java.util.Set<String> vars = dr.getVariants(items.getString(0));
							if(!variant.has("variantId") || !vars.contains(variant.getString("variantId"))) {
								log("Within checking variants EAN, got business for a coincidence: " + items.getString(0) + " vs " + negocio + " of currentEvaluating id (" + (variant.has("variantId") ? variant.getString("variantId") : "NoVaID" ) + ")");
								try {
									res = dr.getProductData(new org.json.JSONArray().put(items.getString(0)));
									if(res != null) {
										try {
											jr = new org.json.JSONObject(res);
											org.json.JSONArray items1 = jr.getJSONArray("items");
											String business = items1.getJSONObject(0).getString("Business");
											business = "MKP".equals(business) ? "Marketplace" : "LVP".equals(business) ? "Liverpool" : "SBB".equals(business) ? "Suburbia" : business;
											if(business.equals(negocio) && "Marketplace".equals(negocio)) {
												variantFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence").put("message", "El código EAN ya existe en catalogación para este negocio, se tiene que hacer una multioferta.").put("values", new org.json.JSONArray().put( variant.has("MainBarCode") ? variant.getString("MainBarCode") : variant.has("MainBarCodeS4H") ? variant.getString("MainBarCodeS4H") : "" )).put("fields", new org.json.JSONArray().put( "MainBarCode" )));
											}else if(business.equals(negocio) && !"Marketplace".equals(negocio)) {
												variantFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence").put("message", "El código EAN ya existe en catalogación para este negocio.").put("values", new org.json.JSONArray().put( variant.has("MainBarCode") ? variant.getString("MainBarCode") : variant.has("MainBarCodeS4H") ? variant.getString("MainBarCodeS4H") : "" )).put("fields", new org.json.JSONArray().put( "MainBarCode" )));
											}else {
												if("Marketplace".equals(negocio) && "Liverpool".equals(business)) {
													variantFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence").put("message", "El código EAN ya existe para Liverpool, se tiene que realizar Stockout.").put("values", new org.json.JSONArray().put( variant.has("MainBarCode") ? variant.getString("MainBarCode") : variant.has("MainBarCodeS4H") ? variant.getString("MainBarCodeS4H") : "" )).put("fields", new org.json.JSONArray().put( "MainBarCode" )));
												}else if("Liverpool".equals(negocio) && "Marketplace".equals(business)) {
													variantFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence").put("message", "El código EAN ya existe para Marketplace, se tiene que liberar el EAN.").put("values", new org.json.JSONArray().put( variant.has("MainBarCode") ? variant.getString("MainBarCode") : variant.has("MainBarCodeS4H") ? variant.getString("MainBarCodeS4H") : "" )).put("fields", new org.json.JSONArray().put( "MainBarCode" )));
												}
											}
											dup = true;
										}catch(org.json.JSONException e) {
											logE(e);
										}
									}
								}catch(org.json.JSONException e) {
									logE(e);
								}
							}
						}
					}catch(org.json.JSONException e) {
						logE(e);
					}
				}
			}
			if(mainBarCode != null) {
				if(mainBarCode.length() == 13 && !isValidEan13(mainBarCode)) {
					genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Validity").put("message", "El código EAN es inconsistente con el dígito verificador.").put("fields", new org.json.JSONArray().put( "MainBarCode" )));
				}else if(mainBarCode.length() == 8) {
					if(!isValidEan8(mainBarCode)) {
						genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Validity").put("message", "El código EAN es inconsistente con el dígito verificador.").put("fields", new org.json.JSONArray().put( "MainBarCode" )));
					}
				}else if(mainBarCode.length() == 12) {
					if(!isValidUPCA(mainBarCode)) {
						genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Validity").put("message", "El código EAN (" + mainBarCode + ") es inconsistente con el dígito verificador. Variante en el arreglo: #" + varNumber ).put("fields", new org.json.JSONArray().put( "MainBarCode" )));
					}
				}
			}
			if(!dup) {
				if(variant.has("variantId")) {
					res = dr.getArticleData(new org.json.JSONArray().put(variant.getString("variantId")));
					if(res != null) {
						try {
							org.json.JSONObject jr = new org.json.JSONObject(res);
							org.json.JSONArray items1 = jr.getJSONArray("items");
							org.json.JSONObject item = items1.getJSONObject(0);
							if("".equals(item.getString("SKU"))) {
								try(ReferenceFileCheck rfc = new ReferenceFileCheck()){
									if(rfc.exists(mainBarCode)) {
										variantFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence").put("message", "El código EAN ya existe catalogado.").put("values", new org.json.JSONArray().put( variant.has("MainBarCode") ? variant.getString("MainBarCode") : variant.has("MainBarCodeS4H") ? variant.getString("MainBarCodeS4H") : "" )).put("fields", new org.json.JSONArray().put( "MainBarCode" )));
									}
								}catch(java.io.IOException e) {
									log("No index for EAN found!, or real problem was encountered: " + e.getMessage());
								}
							}
						}catch(org.json.JSONException e) {
							logE(e);
						}
					}
				}else {
					try(ReferenceFileCheck rfc = new ReferenceFileCheck()){
						if(rfc.exists(mainBarCode)) {
							variantFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Coherence").put("message", "El código EAN ya existe catalogado.").put("values", new org.json.JSONArray().put( variant.has("MainBarCode") ? variant.getString("MainBarCode") : variant.has("MainBarCodeS4H") ? variant.getString("MainBarCodeS4H") : "" )).put("fields", new org.json.JSONArray().put( "MainBarCode" )));
						}
					}catch(java.io.IOException e) {
						log("No index for EAN found!, or real problem was encountered: " + e.getMessage());
					}
				}
			}
		}else {
			log("No variant mainBarCode.");
			if(!"Suburbia".equals(negocio)) {
				if("Marketplace".equals(negocio)) {
					java.util.Map<String, String> miraklExcepProvEAN = getLkpValues("MarketplaceExcepProvEAN");
					if( miraklExcepProvEAN.containsKey(supplier) ) {
						variant.put("TypeMainBarCode", "IE");
						typeMainBarCodeA[0] = "IE";
					}else {
						genericFieldErrors.put(new org.json.JSONObject().put("QualityDimension", "Validity").put("message", "Seller debe especificar un EAN.").put("fields", new org.json.JSONArray().put( "MainBarCode" )));
					}
				}else {
					variant.put("TypeMainBarCode", "IE");
					typeMainBarCodeA[0] = "IE";
				}
			}else {
				variant.put("NUMTP_S4H", "IS");
				typeMainBarCodeA[0] = "IS";
			}
		}

	}

	private String getTypeMainBarCode(String mainBarCode, String business) {
		try{
			Long mbc = Long.parseLong(mainBarCode);
			if("Suburbia".equals(business)) {
				if( mbc.compareTo(750_013_500_000l) >= 0 && mbc.compareTo(750_013_599_999l) <= 0 ) {
					return "MP";
				}else if( mbc.compareTo(750_013_600_000l) >= 0 && mbc.compareTo(999_999_999_999l) <= 0 ) {
					return "H2";
				}
			}else { // 8,809,473,198,816
				if( mbc.compareTo(300_000_000_000l) >= 0 && mbc.compareTo(750_057_499_999l) <= 0 ) {
					return "HE";
				}else if( mbc.compareTo(750_057_500_000l) >= 0 && mbc.compareTo(750_057_599_999l) <= 0 ) {
					return "MP";
				}else if(mbc.compareTo(750_057_600_000l) >= 0 && mbc.compareTo(999_999_999_999l) <= 0) {
					return "H2";
				}
			}
		}catch(NumberFormatException e) {
			logE(e);
		}
		return "UC";
	}
	
	private String lookupValue(String value, String standardizationDictionary) throws KeyManagementException, NoSuchAlgorithmException, UnsupportedEncodingException, URISyntaxException, IOException {
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.base_directory"), "cache", "templates", "dictionaries", standardizationDictionary).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			String[] partes = null;
			while((line = br.readLine()) != null) {
				partes = rw.getRw().parseLine(line, "\"", ";", "\\");
				if(partes.length == 2)
					if(partes[0].equals(value)) return partes[1];
			}
		}catch(java.io.IOException e) {
			logE(e);
		}
//		String rr = null;
//		org.json.JSONObject resp = null;
//		org.json.JSONArray rows = null;
//		rr = rc.getRequest("GET", baseUrl + "/list/StandardizationValue/bySearch?dictionaryProxy=" + java.net.URLEncoder.encode("'" + standardizationDictionary + "'", "UTF-8")
//			+ "&fields=" + java.net.URLEncoder.encode("StandardizationValue.AlternativeValue", "UTF-8") + "&query=" + java.net.URLEncoder.encode("StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + standardizationDictionary + "\" and StandardizationValue.Value equals \"" + value + "\"", "UTF-8"), null);
//		log("Value: " + value + " in " + standardizationDictionary + ": " + rr);
//		resp = new org.json.JSONObject(rr);
//		rows = resp.getJSONArray("rows");
//		if(rows.length() > 0) {
//			return rows.getJSONObject(0).getJSONArray("values").getString(0);
//		}
		return null;
	}

	private org.json.JSONObject createCharacteristicValueObject(String characteristicName, Object value){
		return new org.json.JSONObject().put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values", new org.json.JSONArray().put(value)).put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx"))))).put("_qualification", new org.json.JSONObject().put("characteristic", new org.json.JSONObject().put("_code", characteristicName)));
	}

	private String getCharacteristicValue(org.json.JSONObject characteristic, boolean getCode) {
		if(characteristic == null) {
			return "";
		}
		String dataType = characteristic.has("_datatype") ? characteristic.getString("_datatype") : "";
		if("LOOKUP".equals(dataType) || characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0) instanceof org.json.JSONObject) {
			try{
				return characteristic
						.getJSONArray("_recordLang")
						.getJSONObject(0)
						.getJSONArray("values")
						.getJSONObject(0).has(getCode ? "_code" : "_label") ? 
								characteristic
									.getJSONArray("_recordLang")
									.getJSONObject(0)
									.getJSONArray("values")
									.getJSONObject(0)
									.getString(getCode ? "_code" : "_label") : "";
			}catch(org.json.JSONException e){
				log("--->" + characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0));
				throw e;
			}
		}else {
			return String.valueOf( characteristic.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0) );
		}
	}

	private String getCharacteristicValue(org.json.JSONObject characteristic) {
		return getCharacteristicValue(characteristic, false);
	}

	private void addElement(String characteristic, Object value, org.json.JSONArray characteristics) {
		characteristics.put(
				new org.json.JSONObject()
	                .put( "_qualification",
	                	new org.json.JSONObject()
	                      .put( "characteristic", new org.json.JSONObject().put( "_code", characteristic ) )
	                )
	                .put( "_recordLang",
	                	new org.json.JSONArray()
	                		.put(
	                			new org.json.JSONObject()
			                       .put( "values",
			                    		   new org.json.JSONArray()
			                    		   		.put(
			                    		   			value
			                    		   		)
			                    	)
			                       .put( "_qualification",
			                    		new org.json.JSONObject()
			                    		 .put( "language",
			                    		     new org.json.JSONObject()
			                    				.put( "_code", "zxx" )
			                    		)
			                       )
			               )
	                )
	        );
	}

//	private String getTemplate(org.json.JSONObject data){
//		String template = null;
//		if(data.has("structureGroupMap")) {
//			org.json.JSONArray sgm = data.getJSONArray("structureGroupMap");
//			String extId = null;
//			java.util.regex.Pattern p = java.util.regex.Pattern.compile("'(.+)'@'PrimaryProductTaxonomy'");
//			java.util.regex.Matcher m = null;
//			log("sgm: " + sgm);
//			for(int i=0; i<sgm.length(); i++) {
//				extId = sgm.getJSONObject(i).getJSONObject("_qualification").getJSONObject("structureGroup").getString("_externalId");
//				if(extId.endsWith("@'PrimaryProductTaxonomy'")) {
//					m = p.matcher(extId);
//					if(m.find()) {
//						return m.group(1);
//					}
//				}
//			}
//		}else {
//			log("No structureGroupMap found.");
//		}
//		return template;
//	}
	
	private static synchronized String keepValueToFile(String label, String lookup, long myId) {
		String[] code = new String[1];
		code[0] = null;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("query", "LookupValueLang.Name(es) = \"" + label + "\"");
		qp.put("lookup", "'" + lookup + "'");
		RESTWrapper rw = new RESTWrapper();
		rw.collectData("list", "LookupValue", null, "bySearch", qp, row->{
			code[0] = row.getJSONArray("values").getString(0);
		}, (message)-> { try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/java_active_process_proposal_create.log", true)))) {
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date()))
					+ "] (" + myId + ") " + message);
		} catch (java.io.IOException e) {
		}});
		if(code[0] == null) {
			try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
					new java.io.FileOutputStream("../logs/java_active_process_proposal_create.log", true)))) {
				pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date()))
						+ "] (" + myId + ") Probablemente borraron el valor y se había quedado en caché. ");
			} catch (java.io.IOException e) {
			}
		}else {
			try(java.io.PrintWriter pw = new java.io.PrintWriter(
					new java.io.OutputStreamWriter(
							new java.io.FileOutputStream(java.nio.file.Paths.get(
									PropertiesManager.get("p360.contingency.templates_cache_directory"), 
					"global_lookups",
					lookup).toString(), true)))){
				String delim = "\"";
				String sep = ";";
				String esc = "\\";
				pw.println( rw.getRw().serializeChunk(new String[] { code[0], label }, delim, sep, esc) );
			}catch(java.io.IOException e) {
				try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
						new java.io.FileOutputStream("../logs/java_active_process_proposal_create.log", true)))) {
					e.printStackTrace(pw);
				} catch (java.io.IOException ex) {
				}
			}
			try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
					new java.io.FileOutputStream("../logs/java_active_process_proposal_create.log", true)))) {
				pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date()))
						+ "] (" + myId + ") Staged a missing value in caché: " + code[0] + "<::>" + label + " in " + lookup);
			} catch (java.io.IOException e) {
			}
		}
		return code[0];
	}

	private String run() {
		long init = System.currentTimeMillis();
		loadNextStatusDictionary();
		loadExternalStatusDictionary();
		loadStatusEnum();
		loadExternalStatusEnum();
		JSONObject request = null;
		String rawResp = null;
		boolean unMasiosare = false;
		String externalProductId = null;
		JSONObject resp = null;
		JSONObject basicData = null;
		JSONObject attributes = null;
		JSONObject logisticData = null;
		JSONObject datosVenta = null;
		org.json.JSONArray variantes = null;
		JSONObject variante = null;
		org.json.JSONArray multimediaArray = null;
		JSONObject multimedia = null;
		org.json.JSONArray cosos = null;
		String d = null;
		java.util.Map<String, String> validCodes = null;
		String direction = null;
		String section = null;
		String codeValue = null;
		String templateId = null;
		String previousStatus = "";
		String internalStatus = "";
		String externalStatus = null;
		String nextStatus = null;
		String itemGroup = null;
		String itemGroupS4H = null;
		String marca = null;
		String productFromItemGroup = null;
		String creationDate = null;
		String business = null;
		String sapObjectType = null;
		String supplier = null;
		String brandName = null;
		String brandIdS4H = null;
		String supplierPartNumber = null;
		String sapObjectTypeLabel = null;
		String[] direccionSeccion = null;
		org.json.JSONArray structureProblems = new org.json.JSONArray();
		org.json.JSONArray photosArray = null;
		String characteristicLookup = null;
		String targetRole = "";
		String holder = null;
		String mainBarCode = null;
		String longDescription = null;
		String longDescription2 = null;
		String embedCodeWEB = null;
		String embedCodeWAP = null;
		String refundPolicy = null;
		String externalEmail = null;
		Integer numberOfCurrentVariantsReal = 0;
		boolean selfAdded = false;
		boolean sample = false;
		java.util.List<String> sections = new java.util.ArrayList<>();
		try {
			request = new JSONObject(input);
			org.json.JSONArray products = (org.json.JSONArray) request.remove("products");
			products = products == null ? new org.json.JSONArray() : products;
			JSONObject product = null;
			for (int i = 0; i < products.length(); i++) {
				externalEmail = null;
				request = null;
				rawResp = null;
				externalProductId = null;
				resp = null;
				basicData = null;
				attributes = null;
				logisticData = null;
				datosVenta = null;
				variantes = null;
				variante = null;
				multimediaArray = null;
				multimedia = null;
				cosos = null;
				d = null;
				validCodes = null;
				codeValue = null;
				templateId = null;
				previousStatus = "";
				internalStatus = "";
				externalStatus = null;
				nextStatus = null;
				itemGroup = null;
				productFromItemGroup= null;
				itemGroupS4H = null;
				marca = null;
				creationDate = null;
				business = null;
				sapObjectType = null;
				sapObjectTypeLabel = null;
				direccionSeccion = null;
				structureProblems = new org.json.JSONArray();
				characteristicLookup = null;
				targetRole = "";
				holder = null;
				numberOfCurrentVariantsReal = 0;
				selfAdded = false;
				supplier = null;
				longDescription = null;
				sample = false;
				brandName = null;
				brandIdS4H = null;
				supplierPartNumber = null;
				sections.clear();
				
				product = products.getJSONObject(i);
				org.json.JSONObject photo = null;
				int timesDetailImage = 0;
				int timesIllustration = 0;
				int timesSmosh = 0;
				org.json.JSONArray children = null;
				String recordKey = null;

				/*****
				 * 
				 * 
				 * {
				 * 	"products": [
				 * 			{
				 * 				"lookGroupId": "MIPROPUESTA",
				 * 				"name": "",
				 * 				"description": "",
				 * 				"startAt": "",
				 * 				"endAt": "",
				 * 				"basicData": {
				 * 					"BrandName": ""
				 * 				},
				 * 				"photos": [
				 * 					{
				 * 						"PhotoAssetType":"ProductImage", // Detail Image, Illustration, Smosh
				 * 						"PhotoAssetStatus":"active",
				 * 						"PhotoAssetName": "hola.jpg",
				 * 						"PhotoAssetURL": "http://hola.jpg"
				 * 					}
				 * 				],
				 * 				"modifiedFields": {
				 * 
				 * 				}
				 * 			}
				 * 		]
				 * }
				 * {
				 * 	"_characteristicRecords": [
				 * 		{
				 * 			"_qualification": { 
				 * 				"recordKey": "0000.0000.RK",
				 * 				"parentRecordKey": "root",
				 * 				"language": {
				 * 					"_code":"es"
				 * 				},
				 * 				"characteristic": {
				 * 					"_code":"ProductImage"
				 * 				}
				 * 			},
				 * 			"dataType": "NONE",
				 * 			"_children": [
				 * 				{
				 * 					"_qualification": {
				 * 						"recordKey": "0000.0000.RK",
				 * 						"parentRecordKey": "0000.0000.RK",
				 * 						"language": {
				 * 							"_code": "es"
				 * 						},
				 * 						"characteristic": {
				 * 							"_code": "ProductImageDetail_Name"
				 * 						}
				 * 
				 * 					},
				 * 					"dataType":"TEXT",
				 * 					"_recordLang": [
				 * 						{
				 * 							"values":[
				 * 								"hola.jpg"
				 * 							]
				 * 						}
				 * 					]
				 * 				},
				 * 				{
				 * 					"_qualification": {
				 * 						"recordKey": "0000.0000.RK",
				 * 						"parentRecordKey": "0000.0000.RK",
				 * 						"language": {
				 * 							"_code": "es"
				 * 						},
				 * 						"characteristic": {
				 * 							"_code": "ProductImageDetail_URL"
				 * 						}
				 * 
				 * 					},
				 * 					"dataType":"TEXT",
				 * 					"_recordLang": [
				 * 						{
				 * 							"values":[
				 * 								"http://alangalanga/hola.jpg"
				 * 							]
				 * 						}
				 * 					]
				 * 				},
				 * 				{
				 * 					"_qualification": {
				 * 						"recordKey": "0000.0000.RK",
				 * 						"parentRecordKey": "0000.0000.RK",
				 * 						"language": {
				 * 							"_code": "es"
				 * 						},
				 * 						"characteristic": {
				 * 							"_code": "ProductImageDetail_Status"
				 * 						}
				 * 					},
				 * 					"dataType":"LOOKUP",
				 * 					"_recordLang": [
				 * 						{
				 * 							"values":[
				 * 								{
				 * 									"_code": "01"
				 * 								}
				 * 							]
				 * 						}
				 * 					]
				 * 				}
				 * 			]
				 * 		}
				 * 	]
				 * }
				 * 
				 * 
				 * */
				
				if(product.has("lookGroupId")) {
					String conjuntoLookId = product.getString("lookGroupId");
					String name = product.has("name") ? product.getString("name") : null;
					String desc = product.has("description") ? product.getString("description") : null;
					String startAt = product.has("startAt") ? product.getString("startAt") : null;
					String endAt = product.has("endAt") ? product.getString("endAt") : null;
					
					org.json.JSONObject _data = new org.json.JSONObject();
					org.json.JSONArray lang = new org.json.JSONArray();
					_data.put("lang", lang);
					org.json.JSONArray characteristicArray = new org.json.JSONArray();
					org.json.JSONObject langObject = new org.json.JSONObject();
					lang.put(langObject);
					if(name != null) {
						langObject.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es"))).put("descriptionShort", name);
					}
					if(desc != null) {
						langObject.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es"))).put("descriptionLong", desc);
					}
					if(startAt != null) {
						characteristicArray
						.put(new org.json.JSONObject()
								.put("_qualification",
										new JSONObject()
										.put("recordKey", recordKey)
												.put("characteristic",
														new JSONObject().put("_code", "StartDate")))
								.put("_recordLang",
										new org.json.JSONArray()
												.put(new JSONObject().put("values", new org.json.JSONArray().put(startAt))))
								);
					}
					if(endAt != null) {
						characteristicArray
						.put(new org.json.JSONObject()
								.put("_qualification",
										new JSONObject()
										.put("recordKey", recordKey)
										.put("characteristic",
												new JSONObject().put("_code", "EndDate")))
								.put("_recordLang",
										new org.json.JSONArray()
										.put(new JSONObject().put("values", new org.json.JSONArray().put(endAt))))
								);
					}
					characteristicArray
					.put(new org.json.JSONObject()
							.put("_qualification",
									new JSONObject()
									.put("recordKey", recordKey)
									.put("characteristic",
											new JSONObject().put("_code", "SAPObjectType")))
							.put("_recordLang",
									new org.json.JSONArray()
									.put(new JSONObject().put("values", new org.json.JSONArray().put(new org.json.JSONObject().put("_code", "CLK")))))
							);
					_data.put("_characteristicRecords", characteristicArray);
					photosArray = product.has("photos") ? product.getJSONArray("photos") : new org.json.JSONArray();
					for (int j = 0; j < photosArray.length(); j++) {
						photo = photosArray.getJSONObject(j);
						try {
							if (photo.getString("PhotoAssetType").startsWith("ProductImageDetail")) {
								recordKey = timesDetailImage == 0 ? "0000.0000.RK" : "0000." + ( timesDetailImage < 10 ? "000" + timesDetailImage : timesDetailImage < 100 ? "00" + timesDetailImage : timesDetailImage < 1000 ? "0" + timesDetailImage : timesDetailImage ) + ".RK";
								children = new org.json.JSONArray();
								children.put(new org.json.JSONObject()
										.put("_qualification",
												new org.json.JSONObject()
												.put("recordKey", recordKey)
														.put("characteristic",
																new org.json.JSONObject().put("_code",
																		"ProductImageDetail_Name")))
										.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
												new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
								children.put(new org.json.JSONObject()
										.put("_qualification",
												new org.json.JSONObject()
												.put("recordKey", recordKey)
														.put("characteristic",
																new org.json.JSONObject().put("_code",
																		"ProductImageDetail_URL")))
										.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
												new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
								if(photo.has("PhotoAssetStatus")) {
									children.put(
										new org.json.JSONObject()
												.put("_qualification",
														new org.json.JSONObject()
														.put("recordKey", recordKey)
																.put("characteristic",
																		new org.json.JSONObject().put("_code",
																				"ProductImageDetail_Status")))
												.put("_recordLang",
														new org.json.JSONArray()
																.put(new org.json.JSONObject().put("values",
																		new org.json.JSONArray()
																				.put(new JSONObject()
																						.put("_qualification",
																								new JSONObject().put(
																										"language",
																										new JSONObject().put(
																												"_code", "zxx")))
																						.put("_label", photo.optString(
																								"PhotoAssetStatus")))))));
								}
								characteristicArray
										.put(new org.json.JSONObject()
												.put("_qualification",
														new JSONObject()
														.put("recordKey", recordKey)
																.put("characteristic",
																		new JSONObject().put("_code", "ProductImageDetail")))
												.put("_recordLang",
														new org.json.JSONArray()
																.put(new JSONObject().put("values", new org.json.JSONArray())))
												.put("_children", children));
								timesDetailImage++;
							} else if (photo.getString("PhotoAssetType").equals("ProductImage")) {
								log("Adding a productImage ");
								children = new org.json.JSONArray();
								children.put(new org.json.JSONObject()
										.put("_qualification",
												new org.json.JSONObject()
												.put("recordKey", "0000.0000.RK")
														.put("characteristic",
																new org.json.JSONObject().put("_code", "ProductImage_Name")))
										.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
												new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
								children.put(new org.json.JSONObject()
										.put("_qualification",
												new org.json.JSONObject()
												.put("recordKey", "0000.0000.RK")
														.put("characteristic",
																new org.json.JSONObject().put("_code", "ProductImage_URL")))
										.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
												new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
								if(photo.has("PhotoAssetStatus")) {
									children.put(
										new org.json.JSONObject()
												.put("_qualification",
														new org.json.JSONObject()
														.put("recordKey", "0000.0000.RK")
																.put("characteristic",
																		new org.json.JSONObject().put("_code",
																				"ProductImage_Status")))
												.put("_recordLang",
														new org.json.JSONArray()
																.put(new org.json.JSONObject().put("values",
																		new org.json.JSONArray()
																				.put(new JSONObject()
																						.put("_qualification",
																								new JSONObject().put(
																										"language",
																										new JSONObject().put(
																												"_code", "zxx")))
																						.put("_label", photo.optString(
																								"PhotoAssetStatus")))))));
								}
								characteristicArray.put(new org.json.JSONObject().put("_qualification",
										new JSONObject()
											.put("recordKey", "0000.0000.RK")
											.put("characteristic", 
													new JSONObject()
														.put("_code", "ProductImage")))
										.put("_recordLang",
												new org.json.JSONArray()
														.put(new JSONObject().put("values", new org.json.JSONArray())))
										.put("_children", children));
							} else if (photo.getString("PhotoAssetType").startsWith("Illustration")) {
								recordKey = timesIllustration == 0 ? "0000.0000.RK" : "0000." + ( timesIllustration < 10 ? "000" + timesIllustration : timesIllustration < 100 ? "00" + timesIllustration : timesIllustration < 1000 ? "0" + timesIllustration : timesIllustration ) + ".RK";
								children = new org.json.JSONArray();
								children.put(new org.json.JSONObject()
										.put("_qualification",
												new org.json.JSONObject()
												.put("recordKey",recordKey)
														.put("characteristic",
																new org.json.JSONObject().put("_code", "Illustration_Name")))
										.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
												new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
								children.put(new org.json.JSONObject()
										.put("_qualification",
												new org.json.JSONObject()
												.put("recordKey", recordKey)
														.put("characteristic",
																new org.json.JSONObject().put("_code", "Illustration_URL")))
										.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
												new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
								if(photo.has("PhotoAssetStatus")) {
									children.put(
										new org.json.JSONObject()
												.put("_qualification",
														new org.json.JSONObject()
														.put("recordKey", recordKey)
																.put("characteristic",
																		new org.json.JSONObject().put("_code",
																				"Illustration_Status")))
												.put("_recordLang",
														new org.json.JSONArray()
																.put(new org.json.JSONObject().put("values",
																		new org.json.JSONArray()
																				.put(new JSONObject()
																						.put("_qualification",
																								new JSONObject().put(
																										"language",
																										new JSONObject().put(
																												"_code", "zxx")))
																						.put("_label", photo.optString(
																								"PhotoAssetStatus")))))));
								}
								characteristicArray.put(new org.json.JSONObject().put("_qualification",
										new JSONObject()
										.put("recordKey", recordKey)
												.put("characteristic", new JSONObject().put("_code", "Illustration")))
										.put("_recordLang",
												new org.json.JSONArray()
														.put(new JSONObject().put("values", new org.json.JSONArray())))
										.put("_children", children));
								timesIllustration++;
							} else if (photo.getString("PhotoAssetType").startsWith("ProductImageSmosh")) {
								recordKey = timesSmosh == 0 ? "0000.0000.RK" : "0000." + ( timesSmosh < 10 ? "000" + timesSmosh : timesSmosh < 100 ? "00" + timesSmosh : timesSmosh < 1000 ? "0" + timesSmosh : timesSmosh ) + ".RK";
								children = new org.json.JSONArray();
								children.put(new org.json.JSONObject()
										.put("_qualification",
												new org.json.JSONObject()
												.put("recordKey", recordKey)
														.put("characteristic",
																new org.json.JSONObject().put("_code",
																		"ProductImageSmosh_Name")))
										.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
												new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
								children.put(new org.json.JSONObject()
										.put("_qualification",
												new org.json.JSONObject()
												.put("recordKey", recordKey)
														.put("characteristic",
																new org.json.JSONObject().put("_code",
																		"ProductImageSmosh_URL")))
										.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
												new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
								if(photo.has("PhotoAssetStatus")) {
									children.put(
											new org.json.JSONObject()
													.put("_qualification",
															new org.json.JSONObject()
															.put("recordKey",recordKey)
																	.put("characteristic",
																			new org.json.JSONObject().put("_code",
																					"ProductImageSmosh_Status")))
													.put("_recordLang",
															new org.json.JSONArray()
																	.put(new org.json.JSONObject().put("values",
																			new org.json.JSONArray()
																					.put(new JSONObject()
																							.put("_qualification",
																									new JSONObject().put(
																											"language",
																											new JSONObject().put(
																													"_code", "zxx")))
																							.put("_label", photo.optString(
																									"PhotoAssetStatus")))))));
								}
								characteristicArray
										.put(new org.json.JSONObject()
												.put("_qualification",
														new JSONObject()
														.put("recordKey", recordKey)
																.put("characteristic",
																		new JSONObject().put("_code", "ProductImageSmosh")))
												.put("_recordLang",
														new org.json.JSONArray()
																.put(new JSONObject().put("values", new org.json.JSONArray())))
												.put("_children", children));
								timesSmosh++;
							}
						} catch (org.json.JSONException | NullPointerException e) {
							structureProblems.put(new org.json.JSONObject()
									.put("Message", "Error in structure, failed when processing photo object structure. Mandatory attributes are: PhotoAssetURL and PhotoAssetName")
									.put("Object", photo));
						}
					}
					java.util.Map<String, String> qp = new java.util.TreeMap<>();
					org.json.JSONObject respo = workshop.makeRequest("PUT", "/object/Product2G/'" + conjuntoLookId + "'@'MASTER'?includeIds=true&includeLabels=true", qp, _data.toString());
					log(respo == null ? "Problem updating product2G: " + workshop.getRawResponse() : respo.toString());
					responses.put(respo);
					continue;
				}

				if(product.has("userRemarks") || product.has("modifiedFields")) {
					processModifiedFields(product);
				}
				if(product.has("basicData")) {
					sections.add("basicData");
				}
				if(product.has("attributes")) {
					sections.add("attributes");
				}
				if(product.has("logisticData")) {
					sections.add("logisticData");
				}
				if(product.has("datosVenta")) {
					sections.add("datosVenta");
				}
				if(product.has("unMasiosare")) {
					try{
						unMasiosare = product.getBoolean("unMasiosare");
					}catch(org.json.JSONException e) {
						
					}
					product.remove("unMasiosare");
				}
				this.userAction = product.has("userAction") ? product.getString("userAction") : "InProgress";
				externalEmail = product.has("userEmail") ? product.getString("userEmail") : "";
				targetRole = product.has("targetRole") ? product.getString("targetRole") : "";
				templateId = product.has("template") ? product.getString("template") : null;
				basicData = (org.json.JSONObject) product.remove("basicData");
				attributes = (org.json.JSONObject) product.remove("attributes");
				logisticData = (org.json.JSONObject) product.remove("logisticData");
				datosVenta = (org.json.JSONObject) product.remove("datosVenta");
				multimediaArray = (org.json.JSONArray) product.remove("multiMedia");
				photosArray = (org.json.JSONArray) product.remove("photos");
				variantes = (org.json.JSONArray) product.remove("variants");
				variantes = variantes == null ? new org.json.JSONArray() : variantes;
				cosos = new org.json.JSONArray();
				Object o = product.remove("__sample");
				if(o instanceof Boolean) {
					sample = (Boolean) o;
				}
				business = product.has("Business") ? product.getString("Business") : null;
				externalProductId = product.has("proposalId") ? product.getString("proposalId") : product.has("lookGroupId") ? product.getString("lookGroupId") : null;
				log("ExternalProductId: " + (product.has("proposalId") ? product.getString("proposalId") : "---"));
				if (inconsistentWithVariants(variantes, product.has("proposalId"))) {
					responses.put(new org.json.JSONObject().put("faultCode", 400).put("message",
							"El presente es un error técnico: Inconsistencia entre producto y variantes, hay al menos una variante con \"variantId\", pero el producto no especifica un \"proposalId\", de modo que no se pueden hacer actualizaciones a variantes que ya podrían pertenecer a un producto, favor de remover las llaves \"variantId\" del objeto \"JSON\" en la petición."));
					continue;
				}else if((business == null || templateId == null ) && !product.has("proposalId")) {
					responses.put(new org.json.JSONObject().put("faultCode", 400).put("message",
							"Falta plantilla o negocio para crear la propuesta."));
					continue;
				}
				if (basicData != null) {
					if(business != null) {
						basicData.put("Business", business);
					}
					cosos.put(basicData);
				}
				if (attributes != null) {
					cosos.put(attributes);
				}
				if (logisticData != null) {
					cosos.put(logisticData);
				}
				if (datosVenta != null) {
					cosos.put(datosVenta);
				}
				if(photosArray == null) {
					photosArray = new org.json.JSONArray();
				}
					/*****
					 *
					 * Carga lista para validación de que la característica pertenezca a la
					 * plantilla. loadTemplateCharacteristicsThatUseLookupValue( product.getString(  "template" ) );
					 *
					 *********************************************************************************************************/
				if(variantes != null) {
					for(int k = 0; k<variantes.length(); k++) {
						numberOfCurrentVariantsReal += (variantes.getJSONObject(k).has("variantId")) ? 0 : 1 ;
					}
					log("Variantes for real: " + numberOfCurrentVariantsReal);
				}
				org.json.JSONArray characteristicArray = new org.json.JSONArray();
				org.json.JSONArray writeDataFails = new org.json.JSONArray();
				JSONObject charBody = null;
				itemGroup = null;
				itemGroupS4H = null;
				if (cosos.length() > 0) {
//					if(characteristicsThatAreLookups == null || characteristicsThatAreLookups.isEmpty()) {
//						characteristicsThatAreLookups = getCharacteristicsThatAreLookups();
//					}
				}
				boolean tellme = false;
				for (int j = 0; j < cosos.length(); j++) {
					if (!JSONObject.NULL.equals(cosos.getJSONObject(j)) && cosos.getJSONObject(j) != null) {
						if(JSONObject.getNames(cosos.getJSONObject(j)) != null) {
							for (String name : JSONObject.getNames(cosos.getJSONObject(j))) {
								holder = String.valueOf(cosos.getJSONObject(j).get(name)).replaceAll(" {2,}", " ").trim();
								if(!"".equals(holder)) {
									if ("ProductTypeSAPTEMP".equals(name)) {
										log("Got an item group (ProductTypeSAPTEMP):<::>" + cosos.getJSONObject(j).get(name) + "<::>.");
										itemGroup = String.valueOf(cosos.getJSONObject(j).get(name)).replaceAll(" - .+", ""); // 93904 3043
										if(itemGroup.length() >= 5) {
											productFromItemGroup = itemGroup.substring(5);
											itemGroup = itemGroup.substring(0,5);
											charBody = new org.json.JSONObject()
													.put("_datatype", "LOOKUP")
													.put("_qualification",
															new JSONObject().put("characteristic",
																	new JSONObject().put("_code", "ItemGroup")))
													.put("_recordLang", new org.json.JSONArray().put(
															new JSONObject()
															.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
															.put("values", new org.json.JSONArray()
																	.put(
																		new org.json.JSONObject().put("_code", itemGroup)
																	)
																)));
											characteristicArray.put(charBody);
											if(!"".equals(productFromItemGroup)) {
												charBody = new org.json.JSONObject()
														.put("_datatype", "LOOKUP")
														.put("_qualification",
																new JSONObject().put("characteristic",
																		new JSONObject().put("_code", "ProductTypeSAP")))
														.put("_recordLang", new org.json.JSONArray().put(
																new JSONObject()
																.put("_qualification", new org.json.JSONObject()
																		.put("language", new org.json.JSONObject().put("_code", "zxx")))
																.put("values", new org.json.JSONArray().put(
																		new org.json.JSONObject().put("_code", productFromItemGroup)
																		))));
												characteristicArray.put(charBody);
											}
										}else {
											productFromItemGroup = "";
										}
									} else if ("ProductTypeSAPTEMPSBB".equals(name)) {
										itemGroupS4H = String.valueOf(cosos.getJSONObject(j).get(name)).replaceAll(" - .+", "");
										if(itemGroupS4H.length() >= 5) {
											if(itemGroupS4H.startsWith("SB")) {
												itemGroupS4H = itemGroupS4H.substring(0,7);
												productFromItemGroup = itemGroupS4H.substring(7);
											}else {
												itemGroupS4H = itemGroupS4H.substring(0,5);
												productFromItemGroup = itemGroupS4H.substring(5);
											}
											charBody = new org.json.JSONObject()
													.put("_datatype", "LOOKUP")
													.put("_qualification",
															new JSONObject().put("characteristic",
																	new JSONObject().put("_code", "ItemGroupS4H")))
													.put("_recordLang", new org.json.JSONArray().put(
															new JSONObject()
															.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
															.put("values", new org.json.JSONArray().put(
																	new org.json.JSONObject().put("_code", itemGroupS4H)
																	))));
											characteristicArray.put(charBody);
											if(!"".equals(productFromItemGroup)) {
												charBody = new org.json.JSONObject()
														.put("_datatype", "LOOKUP")
														.put("_qualification",
																new JSONObject().put("characteristic",
																		new JSONObject().put("_code", "SB_0002")))
														.put("_recordLang", new org.json.JSONArray().put(
																new JSONObject()
																.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
																.put("values", new org.json.JSONArray().put(
																		new org.json.JSONObject().put("_code", productFromItemGroup)
																		))));
												characteristicArray.put(charBody);
											}
										}else {
											productFromItemGroup = "";
										}
										// Texto del nombre del producto y las unidades de medidas.
									}
									if("DescriptionLong".equals(name)) {
										longDescription = holder;
										continue;
									}
									if("DescriptionLong2".equals(name)) {
										longDescription2 = holder;
										continue;
									}
									if("EmbedCodeWAP".equals(name)) {
										embedCodeWAP = holder;
										continue;
									}
									if("EmbedCodeWEB".equals(name)) {
										embedCodeWEB = holder;
										continue;
									}
									if("refundPolicy".equals(name)) {
										refundPolicy = holder;
										continue;
									}
									if("MainBarCode".equals(name)) {
										mainBarCode = holder.replaceFirst("^0+", "").replaceAll("\s{2,}", " ").trim();
									}
									if("MainBarCodeS4H".equals(name)) {
										mainBarCode = holder.replaceFirst("^0+", "").replaceAll("\s{2,}", " ").trim();
									}
									if("SupplierPartNumber".equals(name)) {
										supplierPartNumber = holder;
									}
									String drr = dr.getCharacteristicData(new org.json.JSONArray().put(name));
									characteristicLookup = drr != null ? new org.json.JSONObject(drr).getJSONArray("items").getJSONObject(0).getString("lookup") : null; // characteristicsThatAreLookups.get(name);
									log("That are lookup: " + characteristicLookup + " (" + name + ")");
									if (characteristicLookup == null || "".equals(characteristicLookup)) {
										if( cosos.getJSONObject(j).get(name) instanceof org.json.JSONArray ) {
											org.json.JSONArray auxi = (org.json.JSONArray) cosos.getJSONObject(j).get(name);
											org.json.JSONArray theValues = new org.json.JSONArray();
											for(int z=0; z<auxi.length(); z++) {
												theValues.put( auxi.getString(z).replaceAll(" {2,}", " ").trim() );
											}
											charBody = new org.json.JSONObject()
													.put("_qualification",
															new JSONObject().put("characteristic",
																	new JSONObject().put("_code", name)))
													.put("_recordLang", new org.json.JSONArray().put(new JSONObject().put("values",
															new org.json.JSONArray().put( theValues ))));
											characteristicArray.put(charBody);
											tellme = true;
										}else {
											charBody = new org.json.JSONObject()
													.put("_qualification",
															new JSONObject().put("characteristic",
																	new JSONObject().put("_code", name)))
													.put("_recordLang", new org.json.JSONArray().put(new JSONObject().put("values",
															new org.json.JSONArray().put( holder ))));
											characteristicArray.put(charBody);
										}
									} else {
										int s = 0;
										validCodes = procedeACargarValoresValidos(templateId, name);
										if (validCodes.isEmpty()) {
											validCodes = procedeACargarValoresLookup(characteristicLookup);
											s = 1;
										}
										if("Currency".equals(name)) {
											log("Values for currency: " + validCodes);
										}
										if (!validCodes.isEmpty()) {
											if( cosos.getJSONObject(j).get(name) instanceof org.json.JSONArray ) {
												org.json.JSONArray auxi = (org.json.JSONArray) cosos.getJSONObject(j).get(name);
												org.json.JSONArray theValues = new org.json.JSONArray();
												for(int z=0; z<auxi.length(); z++) {
													codeValue = validCodes.get(auxi.getString(z).replaceAll(" {2,}", " ").trim());
													if (codeValue != null) {
														theValues.put(new org.json.JSONObject().put("_label", auxi.getString(z)).put("_code", codeValue));
													} else {
														/*
														genericFieldErrors.put(new JSONObject().put("fields", new org.json.JSONArray().put(name))
																.put("values", new org.json.JSONArray().put( cosos.getJSONObject(j).get(name) ))
																.put("message",
																		"Problem identifying current value within valid lookup value list")
																.put("characteristic", name)
																.put("index", z)
																);
														*/
													}
												}
												charBody = new org.json.JSONObject()
														.put("_qualification",
																new JSONObject().put("characteristic",
																		new JSONObject().put("_code", name)))
														.put("_recordLang", new org.json.JSONArray().put(
																new JSONObject().put("values", theValues)));
												characteristicArray.put(charBody);
												tellme = true;
											}else {
												if (name.startsWith("ProductTypeSAPTEMP")) {
													codeValue = cosos.getJSONObject(j).getString(name).replaceAll(" - .*", "");
													log("Got: " + codeValue);
												}else {
													codeValue = validCodes.get(cosos.getJSONObject(j).getString(name).replaceAll(" {2,}", " ").trim());
												}
												if (codeValue != null) {
													if("BrandName".equals(name)) {
														brandName = codeValue;
													}
													if("BRAND_ID_S4H".equals(name)) {
														brandIdS4H = codeValue;
													}
													charBody = new org.json.JSONObject()
															.put("_datatype", "LOOKUP")
															.put("_qualification",
																	new JSONObject().put("characteristic",
																			new JSONObject().put("_code", name)))
															.put("_recordLang", new org.json.JSONArray().put(
																	new JSONObject()
																	.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
																	.put("values", new org.json.JSONArray().put(
																			new org.json.JSONObject().put("_code", codeValue).put("_label",  cosos.getJSONObject(j).get(name))
																			))));
													characteristicArray.put(charBody);
												} else {
													codeValue = keepValueToFile(holder, characteristicLookup, myId);
													if(codeValue != null) {
														charBody = new org.json.JSONObject()
																.put("_datatype", "LOOKUP")
																.put("_qualification",
																		new JSONObject().put("characteristic",
																				new JSONObject().put("_code", name)))
																.put("_recordLang", new org.json.JSONArray().put(
																		new JSONObject()
																		.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
																		.put("values", new org.json.JSONArray().put(
																				new org.json.JSONObject().put("_code", codeValue).put("_label", holder)
																				))));
														characteristicArray.put(charBody);
													}else {
														log("%%%%%%%%%%%%%%% Maylov %%%%%%%%%%%%%%%% \n\t" + s + "\n\t" + validCodes + "\n\t" + new JSONObject().put("fields", new org.json.JSONArray().put( name ))
															.put("values", new org.json.JSONArray().put( holder ))
															.put("message",
																	"Problem identifying current value within valid lookup value list")
															.put("characteristic", name)
															.put("values", new org.json.JSONArray().put( holder )));
														children = new org.json.JSONArray();
														children.put(new org.json.JSONObject()
																.put("_qualification",
																		new org.json.JSONObject()
																				.put("characteristic",
																						new org.json.JSONObject().put("_code",
																								"WriteDataIssue_CharacteristicID")))
																.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
																		new org.json.JSONArray().put(name)))));
														children.put(new org.json.JSONObject()
																.put("_qualification",
																		new org.json.JSONObject()
																				.put("characteristic",
																						new org.json.JSONObject().put("_code",
																								"WriteDataIssue_TiempoReportado")))
																.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
																		new org.json.JSONArray().put(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new java.util.Date()))))));
														children.put(new org.json.JSONObject()
																.put("_qualification",
																		new org.json.JSONObject()
																		.put("characteristic",
																				new org.json.JSONObject().put("_code",
																						"WriteDataIssue_Detalle")))
																.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
																		new org.json.JSONArray().put("No fue posible encontrar un identificador válido para la característica en la plantilla, posiblemente por contar con acotación y no estar actualizada. Valor proporcionado: " + cosos.getJSONObject(j).getString(name).substring(0, Integer.min(cosos.getJSONObject(j).getString(name).length(), 1825)))))));
														writeDataFails
															.put(new org.json.JSONObject()
																	.put("_qualification",
																			new JSONObject()
																					.put("characteristic",
																							new JSONObject().put("_code", "WriteDataIssue")))
																	.put("_recordLang",
																			new org.json.JSONArray()
																					.put(new JSONObject().put("values", new org.json.JSONArray())))
																	.put("_children", children));
													}
													/*
													genericFieldErrors.put(new JSONObject().put("fields", new org.json.JSONArray().put( name ))
															.put("values", new org.json.JSONArray().put( cosos.getJSONObject(j).get(name) ))
															.put("message",
																	"Problem identifying current value within valid lookup value list")
															.put("characteristic", name)
															.put("values", new org.json.JSONArray().put( cosos.getJSONObject(j).get(name) )));
													*/
													if("ItemGroup".equals(name)) {
														log("2nd.- ValidCodes for BrandName (looking for: " + ("ItemGroup".equals(name) ? cosos.getJSONObject(j).getString(name).replaceAll("^[0-9]+ - ", "") : cosos.getJSONObject(j).get(name)) + "): " + validCodes + "<::::::::::::::::::::>");
													}
												}
											}
										} else {
											codeValue = keepValueToFile( cosos.getJSONObject(j).getString(name), characteristicLookup, myId );
											if(codeValue != null) {
												charBody = new org.json.JSONObject()
														.put("_datatype", "LOOKUP")
														.put("_qualification",
																new JSONObject().put("characteristic",
																		new JSONObject().put("_code", name)))
														.put("_recordLang", new org.json.JSONArray().put(
																new JSONObject()
																.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
																.put("values", new org.json.JSONArray().put(
																		new org.json.JSONObject().put("_code", codeValue).put("_label", holder)
																		))));
												characteristicArray.put(charBody);
											}else {
												/********************************************/
												// insert new value with object api.
												/********************************************/

												children = new org.json.JSONArray();
												children.put(new org.json.JSONObject()
														.put("_qualification",
																new org.json.JSONObject()
																		.put("characteristic",
																				new org.json.JSONObject().put("_code",
																						"WriteDataIssue_CharacteristicID")))
														.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
																new org.json.JSONArray().put(name)))));
												children.put(new org.json.JSONObject()
														.put("_qualification",
																new org.json.JSONObject()
																		.put("characteristic",
																				new org.json.JSONObject().put("_code",
																						"WriteDataIssue_TiempoReportado")))
														.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
																new org.json.JSONArray().put(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new java.util.Date()))))));
												children.put(new org.json.JSONObject()
														.put("_qualification",
																new org.json.JSONObject()
																.put("characteristic",
																		new org.json.JSONObject().put("_code",
																				"WriteDataIssue_Detalle")))
														.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
																new org.json.JSONArray().put("No fue posible encontrar un identificador válido para la característica en la plantilla, posiblemente por contar con acotación y no estar actualizada. Valor proporcionado: " + cosos.getJSONObject(j).getString(name).substring(0, Integer.min(cosos.getJSONObject(j).getString(name).length(), 1825)))))));
												writeDataFails
													.put(new org.json.JSONObject()
															.put("_qualification",
																	new JSONObject()
																			.put("characteristic",
																					new JSONObject().put("_code", "WriteDataIssue")))
															.put("_recordLang",
																	new org.json.JSONArray()
																			.put(new JSONObject().put("values", new org.json.JSONArray())))
															.put("_children", children));
												log( "No lookup values found: " + new JSONObject().put("fields", new org.json.JSONArray().put( name ))
														.put("values", new org.json.JSONArray().put( cosos.getJSONObject(j).get(name) ))
														.put("message", "Problem identifying current lookup values")
														.put("characteristic", name));
											}
											/*
											genericFieldErrors.put(new JSONObject().put("fields", new org.json.JSONArray().put( name ))
													.put("values", new org.json.JSONArray().put( cosos.getJSONObject(j).get(name) ))
													.put("message", "Problem identifying current lookup values")
													.put("characteristic", name));
											*/
										}
									}
								}
								if(tellme) {
									tellme = false;
								}
							}
						}
					}
				}
				if(business != null && !"".equals(business)) {
					charBody = new org.json.JSONObject()
							.put("_datatype", "LOOKUP")
							.put("_qualification",
									new JSONObject().put("characteristic", new JSONObject().put("_code", "Business")))
							.put("_recordLang", new org.json.JSONArray().put(new JSONObject().put("values",
									new org.json.JSONArray().put(new org.json.JSONObject().put("_label", business)))));
					characteristicArray.put(charBody);
					if (product.has("supplier")) {
						charBody = new org.json.JSONObject()
								.put("_qualification",
										new JSONObject().put("characteristic", new JSONObject().put("_code", "SupplierID")))
								.put("_recordLang", new org.json.JSONArray().put(new JSONObject()
										.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
										.put("values",
												new org.json.JSONArray().put(supplier = product.getString("supplier").replaceAll("^0+", "")))));
						characteristicArray.put(charBody);
					}
				}
				if( "Finished".equals(userAction) && ("SKU".equals(targetRole) || "Compras".equals(targetRole)) ) {
					String rr = rc.getRequest("GET", listAPIArticleURL
							+ java.net.URLEncoder.encode("ProductReference.ReferencedSupplierAid(\"" + externalProductId + "\") equals \"" + externalProductId + "\"", "UTF-8")
							+ "&fields=Article.SupplierAID,ProductReference.ReferencedSupplierAid(%22" + externalProductId + "%22)", null);
					resp = new org.json.JSONObject(rr);
					org.json.JSONArray rows = resp.getJSONArray("rows");
					numberOfCurrentVariantsReal += rows.length();
					sapObjectType = "Marketplace".equals(business) ? "00" : numberOfCurrentVariantsReal > 1 ? "01" : numberOfCurrentVariantsReal.equals(1) ? "00" : null;
					sapObjectTypeLabel = "Marketplace".equals(business) && "00".equals(sapObjectType) ? "Artículo genérico/individual" : "00".equals(sapObjectType) ? "Artículo individual" : "Artículo genérico";
					if (sapObjectType != null) {
						charBody = new org.json.JSONObject()
								.put("_qualification",
										new JSONObject().put("characteristic",
												new JSONObject().put("_code", "SAPObjectType")))
								.put("_recordLang",
										new org.json.JSONArray().put(new JSONObject().put("values", new org.json.JSONArray()
												.put(new org.json.JSONObject().put("_code", sapObjectType)))));
						characteristicArray.put(charBody);
						log("Placed SAPObjectType for generic part: " + sapObjectType);
						if(variantes.length() > 0) {
							for(int i0 = 0; i0 < variantes.length(); i0++) {
								variantes.getJSONObject(i0).put("SAPObjectType", "01".equals(sapObjectType) ? "Variante" : "Artículo individual");
							}
						}
						org.json.JSONArray jarr = new org.json.JSONArray(); 
						for(int a = 0; a<rows.length(); a++) {
							jarr.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + rows.getJSONObject(a).getJSONArray("values").getString(0) + "'@1")).put("values", new org.json.JSONArray().put("01".equals(sapObjectType) ? "02" : "00")));
						}
						java.util.Map<String, String> qp00 = new java.util.TreeMap<>();
						qp00.put("includeObjectsInProtocol", "false");
						rw.writeData("list", "Article", null, qp00, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('SAPObjectType',root,\"0000.0000.RK\",'SAPObjectType',-1)"))).put("rows", jarr), this::log);
					}else {
						log("########################### Business: " + business + " ##############" + sapObjectType + "#######" + sapObjectTypeLabel + "#######");
					}
				}else {
					log("_______________________TTTTTTTTTTTTTTTTTTT ÑEL " + externalProductId);
				}
				children = null;
				int timesOwnersManual = 0;
				int timesLiverpoolManual = 0;
				int timesProductVideo = 0;
				int timesNOM = 0;
				multimediaArray = multimediaArray == null ? new org.json.JSONArray() : multimediaArray;
				recordKey = null;
				for (int j = 0; j < multimediaArray.length(); j++) {
					try {
						multimedia = multimediaArray.getJSONObject(j);
						if (multimedia.getString("MultimediaAssetType").startsWith("LiverpoolManual")) {
							recordKey = timesLiverpoolManual == 0 ? "0000.0000.RK" : "0000." + ( timesLiverpoolManual < 10 ? "000" + timesLiverpoolManual : timesLiverpoolManual < 100 ? "00" + timesLiverpoolManual : timesLiverpoolManual < 1000 ? "0" + timesLiverpoolManual : timesLiverpoolManual ) + ".RK";
							children = new org.json.JSONArray();
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"LiverpoolManual_Name")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetName"))))));
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"LiverpoolManual_URL")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetURL"))))));
							characteristicArray
							.put(new org.json.JSONObject()
									.put("_qualification",
											new JSONObject()
											.put("recordKey", recordKey)
											.put("characteristic",
													new JSONObject().put("_code", "LiverpoolManual")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray())))
									.put("_children", children));
							timesLiverpoolManual++;
						} else if (multimedia.getString("MultimediaAssetType").startsWith("ProductVideo")) {
							recordKey = timesProductVideo == 0 ? "0000.0000.RK" : "0000." + ( timesProductVideo < 10 ? "000" + timesProductVideo : timesProductVideo < 100 ? "00" + timesProductVideo : timesProductVideo < 1000 ? "0" + timesProductVideo : timesProductVideo ) + ".RK";
							children = new org.json.JSONArray();
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"ProductVideo_Name")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetName"))))));
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code", "ProductVideo_URL")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetURL"))))));
							characteristicArray
									.put(new org.json.JSONObject()
											.put("_qualification",
													new JSONObject()
													.put("recordKey", recordKey)
															.put("characteristic",
																	new JSONObject().put("_code", "ProductVideo")))
											.put("_recordLang",
													new org.json.JSONArray().put(
															new JSONObject()
																.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
																.put("values", new org.json.JSONArray())))
											.put("_children", children));
							timesProductVideo++;
						} else if (multimedia.getString("MultimediaAssetType").startsWith("OwnersManual")) {
							recordKey = timesOwnersManual == 0 ? "0000.0000.RK" : "0000." + ( timesOwnersManual < 10 ? "000" + timesOwnersManual : timesOwnersManual < 100 ? "00" + timesOwnersManual : timesOwnersManual < 1000 ? "0" + timesOwnersManual : timesOwnersManual ) + ".RK";
							children = new org.json.JSONArray();
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"OwnersManual_Name")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetName"))))));
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code", "OwnersManual_URL")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetURL"))))));
							characteristicArray
									.put(new org.json.JSONObject()
											.put("_qualification",
													new JSONObject()
													.put("recordKey", recordKey)
															.put("characteristic",
																	new JSONObject().put("_code", "OwnersManual")))
											.put("_recordLang",
													new org.json.JSONArray().put(
															new JSONObject()
																.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
																.put("values", new org.json.JSONArray())))
											.put("_children", children));
							timesOwnersManual++;
						} else if (multimedia.getString("MultimediaAssetType").startsWith("NOM")) {
							recordKey = timesNOM == 0 ? "0000.0000.RK" : "0000." + ( timesNOM < 10 ? "000" + timesNOM : timesNOM < 100 ? "00" + timesNOM : timesNOM < 1000 ? "0" + timesNOM : timesNOM ) + ".RK";
							children = new org.json.JSONArray();
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"NOM_Name")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetName"))))));
							children.put(new org.json.JSONObject()
									.put("_qualification",
											new org.json.JSONObject()
											.put("recordKey", recordKey)
													.put("characteristic",
															new org.json.JSONObject().put("_code", "NOM_URL")))
									.put("_recordLang",
											new org.json.JSONArray().put(
													new org.json.JSONObject()
														.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
														.put("values", new org.json.JSONArray()
															.put(multimedia.getString("MultimediaAssetURL"))))));
							characteristicArray
									.put(new org.json.JSONObject()
											.put("_qualification",
													new JSONObject()
													.put("recordKey", recordKey)
															.put("characteristic",
																	new JSONObject().put("_code", "NOM")))
											.put("_recordLang",
													new org.json.JSONArray().put(
															new JSONObject()
																.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "zxx")))
																.put("values", new org.json.JSONArray())))
											.put("_children", children));
							timesNOM ++;
						}else {
							log("Multimedia element not found: " + multimedia);
						}
					} catch (org.json.JSONException | NullPointerException e) {
						structureProblems.put(new org.json.JSONObject()
								.put("Message",
										"Error in structure, failed when processing multimedia object structure.")
								.put("Object", multimedia));
					}
				}
				
				String[] typeMainBarCodeA = new String[1];
				typeMainBarCodeA[0] = null;

				try {

						if(variantes != null && variantes.length() > 0) {
							if(mainBarCode != null && !"".equals(mainBarCode) && variantes.length() == 1 && !variantes.getJSONObject(0).has("MainBarCode") && !variantes.getJSONObject(0).has("MainBarCodeS4H")) {
								variantes.getJSONObject(0).put("MainBarCode", mainBarCode);
								log("Moved main bar code to variant for an individual since it was only at product level.");
							}
							java.util.ArrayList<String> mbc = new java.util.ArrayList<>();
							for(int n=0; n<variantes.length(); n++) {
								variantResponsesArray.put(new org.json.JSONObject());
								log( "Now checking values for variants... " );
								validateVariants(
										  variantes.getJSONObject(n)
										, templateId
										, business
										, itemGroup != null && !"".equals(itemGroup) ? itemGroup : itemGroupS4H
										, marca
										, n
										, selfAdded
										, supplier
										, typeMainBarCodeA
									);
								variantes.getJSONObject(n).put("SAPObjectType", variantes.length() > 1 ? "Variante" : "Artículo individual" );
								String mb = variantes.getJSONObject(n).has("MainBarCode") ? variantes.getJSONObject(n).getString("MainBarCode") : variantes.getJSONObject(n).has("MainBarCodeS4H") ? variantes.getJSONObject(n).getString("MainBarCodeS4H") : null;
								if(mb != null && !"".equals(mb)) {
									if(!variantes.getJSONObject(n).has("variantId") && (variantes.getJSONObject(n).has("MainBarCode") || variantes.getJSONObject(n).has("MainBarCodeS4H"))) {
										if(mbc.contains(mb)) {
											variantFieldErrors.put(
													new JSONObject()
														.put("values", new org.json.JSONArray().put( variantes.getJSONObject(n).has( "MainBarCode" ) ? variantes.getJSONObject(n).getString("MainBarCode") : variantes.getJSONObject(n).getString("MainBarCodeS4H")))
														.put("message", "El valor del EAN está duplicado con el valor de otra variante.")
														.put("characteristic", "MainBarCode")
														.put("fields", new org.json.JSONArray().put("MainBarCode")))
											;
											log("HAVE A LOOK: " + variantes);
											variantResponsesArray.getJSONObject(n).put("fieldProblems", variantFieldErrors);
											variantFieldErrors = new org.json.JSONArray();
										} else {
											String toAdd = variantes.getJSONObject(n).has("MainBarCode") ? variantes.getJSONObject(n).getString("MainBarCode") : variantes.getJSONObject(n).getString("MainBarCodeS4H");
											if(!"".equals(toAdd))
												mbc.add(toAdd);
										}
									}
								}
							}
						}
					if(genericFieldErrors.length() > 0) {
						log("Habían errores :) " + genericFieldErrors);
					}
						log("UwU :>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ");
						if(externalProductId != null && !"".equals(externalProductId) && (internalStatus == null || "".equals(internalStatus))) {
							java.util.Map<String, String> qp = new java.util.TreeMap<>();
							qp.put("entityFilter", "Product2G");
							qp.put("includeLabels", "true");
							qp.put("includeIds", "true");
							org.json.JSONObject laresponse = workshop.makeRequest("GET", "/object/Product2G/'" + externalProductId + "'@'MASTER'", qp, null);
							if(laresponse != null) {
								org.json.JSONObject ladata = laresponse.getJSONObject("_data");
								if(ladata != null) {
									internalStatus = !ladata.has("currentStatus")  ? "" : String.valueOf( ladata.getJSONObject("currentStatus").getInt("_key") );
									previousStatus = !ladata.has("previousStatus") ? "" : String.valueOf( ladata.getJSONObject("previousStatus").getInt("_key") );
									externalStatus = !ladata.has("externalStatus") ? "" : ladata.getJSONObject("externalStatus").getString("_code");
								}
							}else {
								log("Error trying to retrieve status info: " + workshop.getRawResponse());
								log("Error trying to retrieve status info (exception): " + workshop.getException());
								logE(workshop.getException());
							}
						}
						log((externalProductId == null ? "---" : externalProductId) +  " User Action: " + userAction + ", Target Role: " + product.optString("targetRole", "---"));
						if(!"InProgress".equals(userAction)) {
							previousStatus = "10031".equals(previousStatus) || "".equals(previousStatus) ? "" : previousStatus;
							internalStatus = "10031".equals(internalStatus) || "".equals(internalStatus) ? "" : internalStatus;
							if("||F|SKU".equals(previousStatus + "|" + internalStatus + "|" + userAction.substring(0, 1) + "|" + targetRole) || "||F|Compras".equals(previousStatus + "|" + internalStatus + "|" + userAction.substring(0, 1) + "|" + targetRole)) {
								internalStatus = "1001";
							}
							if(userAction.startsWith("C")) {
								targetRole = "";
								log("Aquí lo debimos haber borrado. " + externalProductId);
								java.util.Map<String, String> qp = new java.util.HashMap<>();
								qp.put("includeObjectsInProtocol", "false");
								RequestHandler rhP = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Product2G.EAN")).put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)")).put(new org.json.JSONObject().put("identifier", "Product2GCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)")), 100, request0 -> rw.writeData("list", "Product2G", null, qp, request0, this::log) );
								RequestHandler rhA = new RequestHandler( new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Article.EAN")).put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MainBarCode',root,\"0000.0000.RK\",'MainBarCode',-1)")).put(new org.json.JSONObject().put("identifier", "ArticleCharacteristicValueLang.Value('MainBarCodeS4H',root,\"0000.0000.RK\",'MainBarCodeS4H',-1)")).put(new org.json.JSONObject().put("identifier", "Article.CurrentStatus")), 100, request0 -> rw.writeData("list", "Article", null, qp, request0, this::log) );
								rhP.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + externalProductId + "'@1")).put("values", new org.json.JSONArray().put("").put("").put("")));
								rhP.sendData();
								java.util.Map<String, String> qp0 = new java.util.HashMap<>();
								qp0.put("products", "'" + externalProductId + "'@1");
								qp0.put("pageSize", "100");
								rw.collectData("list", "Article", null, "byProducts", qp0, row -> {
									rhA.addRow(new org.json.JSONObject().put("object", row.getJSONObject("object")).put("values", new org.json.JSONArray().put("").put("").put("").put("Cancelado")));
								});
								rhA.sendData();
								nextStatus = "1009";
							} else {
								nextStatus = this.nextStatusMap
										.get(previousStatus + "|" + internalStatus + "|" + userAction.substring(0, 1) + "|" + targetRole);
							}
							if(nextStatus == null) {
								log("No valid key found: " + previousStatus + "|" + internalStatus + "|" + userAction.substring(0, 1) + "|" + targetRole + ": " + nextStatus);
								responses.put(new org.json.JSONObject().put("faultCode", 400).put("message", "Problema técnico de incompatibilidad de estatus, acción y rol de destino, el valor de la llave \"userAction\": " + userAction + ", en conjunto con el valor de la llave \"targetRole\": " + targetRole + ", para el estatus actual de la propuesta: \"" + statusEnum.get(internalStatus) + "\" y estado previo de la propuesta: \"" + statusEnum.get(previousStatus) + "\", es desconocido, favor de reportarlo con el equipo de soporte."));
								continue;
							}
							log("For: " + previousStatus + "|" + internalStatus + "|" + userAction.substring(0, 1) + "|" + targetRole + ": " + nextStatus);
							internalStatus = internalStatus == null || "".equals(internalStatus) ? "10031" : internalStatus;
							previousStatus = nextStatus != null && !"".equals(nextStatus) ? internalStatus : previousStatus;
							internalStatus = nextStatus != null && !"".equals(nextStatus) ? nextStatus : internalStatus;
							externalStatus = this.externalStatusMap.get(internalStatus);
							externalStatus = externalStatus == null ? "Borrador" : externalStatus;
						}else{
							log("For: " + previousStatus + "|" + internalStatus + "|" + userAction.substring(0, 1) + "|" + targetRole + ": " + nextStatus);
							internalStatus = internalStatus == null || "".equals(internalStatus) ? "10031" : internalStatus;
							externalStatus = this.externalStatusMap.get(internalStatus);
							externalStatus = externalStatus == null ? "Borrador" : externalStatus;
						} log("Los estatus: " + previousStatus + "|" + internalStatus + "|" + externalStatus);
						log("<:::::IG::" + itemGroup + "::::::::><:::::::IGS4H::" + itemGroupS4H + "::::::::>" + productFromItemGroup + "<::>");
						if(productFromItemGroup != null && !"".equals(productFromItemGroup)) {
							characteristicArray.put( createCharacteristicValueObject("Suburbia".equals(business) ? "SB_0002" : "ProductTypeSAP", new org.json.JSONObject().put("_code", productFromItemGroup ) ) );
						}
						if(!sections.isEmpty() || unMasiosare) {
							computeGeneric(externalProductId, characteristicArray, templateId, internalStatus, business, itemGroup == null || "".equals(itemGroup) ? itemGroupS4H : itemGroup, sections, variantes.length(), unMasiosare);
							boolean fnd = false;
							if("00".equals( sapObjectType ) ){
								for(int p=0; p<characteristicArray.length(); p++) {
									if(business.equals("Suburbia")) {
										if("NUMTP_S4H".equals( characteristicArray.getJSONObject(p).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code") )) {
											if(typeMainBarCodeA[0] != null) {
												characteristicArray.getJSONObject(p).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).put("_code", typeMainBarCodeA[0]);
												log("TypeMainBarCode on upperProduct part changed: " + characteristicArray.getJSONObject(p));
											}
											fnd = true;
										}
									}else {
										if("TypeMainBarCode".equals( characteristicArray.getJSONObject(p).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code") )) {
											if(typeMainBarCodeA[0] != null) {
												characteristicArray.getJSONObject(p).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).put("_code", typeMainBarCodeA[0]);
												log("TypeMainBarCode on upperProduct part changed: " + characteristicArray.getJSONObject(p));
											}
											fnd = true;
										}
									}
								}
								if(!fnd) {
									if(typeMainBarCodeA[0] != null) {
										if(business.equals("Suburbia")) {
											characteristicArray.put( createCharacteristicValueObject("NUMTP_S4H", new org.json.JSONObject().put("_code", typeMainBarCodeA[0] )) );
										}else {
											characteristicArray.put( createCharacteristicValueObject("TypeMainBarCode", new org.json.JSONObject().put("_code", typeMainBarCodeA[0] )) );
										}
									}else {
										log("Not able to set TypeMainBarCode since there was no value obtained from articles.");
									}
								}else {
									log("No TypeMainBarCode found on parent after computed fields.");
								}
							}else {
								log("SAPObjectType was not individual -->" + java.util.Arrays.asList(typeMainBarCodeA) + "<--");
							}
						}
					if(genericFieldErrors.length() == 0 && !errorInVariant() && variantFieldErrors.length() == 0 ) {
						JSONObject reqObj = new org.json.JSONObject();
						if(characteristicArray != null && characteristicArray.length() > 0) {
							if(writeDataFails != null && writeDataFails.length() > 0) {
								for(int k=0; k<writeDataFails.length(); k++) {
									characteristicArray.put(writeDataFails.getJSONObject(k));
								}
							}
							reqObj.put("_characteristicRecords", characteristicArray);
						}
						String pn = null;
						java.math.BigDecimal igConf = null;
						java.math.BigDecimal se = null;
						java.math.BigDecimal dirConf = null;
						for(int idx=0; idx<characteristicArray.length(); idx++) {
							if("ItemGroupIAConfidenceDir".equals(characteristicArray.getJSONObject(idx).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"))) {
								dirConf = new java.math.BigDecimal( characteristicArray.getJSONObject(idx).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) );
							}else if("ItemGroupIAConfidenceIG".equals(characteristicArray.getJSONObject(idx).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"))) {
								igConf = new java.math.BigDecimal( characteristicArray.getJSONObject(idx).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) );
							}else if("ItemGroupIAConfidenceSec".equals(characteristicArray.getJSONObject(idx).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"))) {
								se = new java.math.BigDecimal( characteristicArray.getJSONObject(idx).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0) );
							}else if("ProductName".equals(characteristicArray.getJSONObject(idx).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"))) {
								pn = characteristicArray.getJSONObject(idx).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getString(0);
							}
						}
						if( igConf != null || se != null || dirConf != null ) {
							if(igConf != null) {
								if(igConf.compareTo( new java.math.BigDecimal(0.999517) ) <= 0) {
									internalStatus = "1021";
								}
							}
							if(dirConf != null) {
								if(dirConf.compareTo( new java.math.BigDecimal(0.999517) ) <= 0) {
									internalStatus = "1021";
								}
							}
							if(se != null) {
								if(se.compareTo( new java.math.BigDecimal(0.999517) ) <= 0) {
									internalStatus = "1021";
								}
							}
						}
						if(longDescription != null) {
							reqObj.put("lang", new org.json.JSONArray().put(
									new org.json.JSONObject()
										.put("descriptionLong", longDescription)
										.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es"))))
								);
						}
						if(longDescription2 != null) {
							if(reqObj.has("lang")) {
								org.json.JSONArray lang = reqObj.getJSONArray("lang");
								lang.put(
										new org.json.JSONObject()
											.put("descriptionLong2", longDescription2)
											.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es"))))
									;
							}else {
								reqObj.put("lang", new org.json.JSONArray().put(
										new org.json.JSONObject()
											.put("descriptionLong2", longDescription2)
											.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es"))))
									);
							}
						}
						if(pn != null) {
							if(reqObj.has("lang")) {
								org.json.JSONArray lang = reqObj.getJSONArray("lang");
								lang.put(
										new org.json.JSONObject()
											.put("productName", pn)
											.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es"))))
									;
							}else {
								reqObj.put("lang", new org.json.JSONArray().put(
										new org.json.JSONObject()
											.put("productName", pn)
											.put("_qualification", new org.json.JSONObject().put("language", new org.json.JSONObject().put("_code", "es"))))
									);
							}
						}
						if(embedCodeWEB != null) {
							reqObj.put("embedCodeWEB", embedCodeWEB);
						}
						if(embedCodeWAP != null) {
							reqObj.put("embedCodeWAP", embedCodeWAP);
						}
						if(refundPolicy != null) {
							reqObj.put("refundPolicy", refundPolicy);
						}
						if(externalEmail != null && !"".equals(externalEmail)) {
							reqObj.put("lasModificationUserEmail", externalEmail);
						}
						if(mainBarCode != null && !"".equals(mainBarCode)) {
							reqObj.put("gtin", mainBarCode);
						}
						if(templateId != null && !"".equals(templateId)) {
							reqObj.put("structureGroupMap", new org.json.JSONArray().put(new org.json.JSONObject().put("_qualification", new org.json.JSONObject().put("structureGroup", new org.json.JSONObject().put("_externalId", "'" + templateId + "'@'PrimaryProductTaxonomy'") ) )));
							log("Adding template: " + reqObj.getJSONArray("structureGroupMap"));
						}
						if(business != null) {
							reqObj.put("business", new org.json.JSONObject().put("_label", business));
						}
						org.json.JSONObject jo1 = null;
						for(int m=0; m<characteristicArray.length(); m++) {
							jo1 = characteristicArray.getJSONObject(m);
							String cid = jo1.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
							if("Direction".equals(cid)) {
								direction = jo1.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
							}else if("Section".equals(cid)) {
								section = jo1.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
							}
						}
						org.json.JSONObject extraData = new org.json.JSONObject();
						if(itemGroupS4H != null) {
							extraData.put("itemGroupS4H", new org.json.JSONObject().put("_code", itemGroupS4H) );
						}
						if(itemGroup != null) {
							extraData.put("itemGroup", new org.json.JSONObject().put("_code", itemGroup) );
						}
						if(sapObjectType != null) {
							extraData.put("sapObjectType", new org.json.JSONObject().put("_code", sapObjectType) );
						}
						if(supplier != null) {
							extraData.put("supplierID", new org.json.JSONObject().put("_code", supplier) );
						}
						if(brandName != null) {
							extraData.put("brandName", new org.json.JSONObject().put("_code", brandName));
						}
						if(brandIdS4H != null) {
							extraData.put("brandIdS4H", new org.json.JSONObject().put("_code", brandIdS4H));
						}
						if(supplierPartNumber != null) {
							extraData.put("supplierPartNumber", supplierPartNumber);
						}
						
						if(extraData.length() > 0) {
							extraData.put("_qualification", new org.json.JSONObject().put("targetMarket", new org.json.JSONObject().put("_code", "MX")));
							reqObj.put("productExtraData", extraData);
						}
						
						if(direction != null) {
							extraData.put("direction", new org.json.JSONObject().put("_code", direction));
						}
						if(section != null) {
							extraData.put("section", new org.json.JSONObject().put("_code", section));
						}
						
						if(!sample) {

//							if(!sample && !"".equals(externalProductId)) {
//								reqObj = new org.json.JSONObject();
								reqObj.put("currentStatus", new org.json.JSONObject().put("_code", internalStatus));
								if (!"".equals(previousStatus) && previousStatus != null) {
									log("Placing previous Status: " + previousStatus);
									reqObj.put("previousStatus", new org.json.JSONObject().put("_code", previousStatus));
								}
								reqObj.put("externalStatus", new org.json.JSONObject().put("_code", externalStatus));
//								log("External Product Id 2: " + (externalProductId));
//								if(externalProductId != null && !"".equals(externalProductId)) {
//									log("GOING WITH PUT <:>" + reqObj + "<:>");
//									rawResp = this.rc.getRequest("PUT",
//											this.objectAPIProduct2GURL + "/'" + externalProductId + "'@'MASTER'?includeLabels=true", reqObj.toString());
//								}
//							}
							
							log("External Product Id 2: " + (externalProductId));
							if(externalProductId != null && !"".equals(externalProductId)) {
								log("GOING WITH PUT <:>" + reqObj + "<:>");
								rawResp = this.rc.getRequest("PUT",
										this.objectAPIProduct2GURL + "/'" + externalProductId + "'@'MASTER'?includeLabels=true", reqObj.toString());
							}else {
								log("GOING WITH POST <:>" + reqObj + "<:>");
								rawResp = this.rc.getRequest("POST",
										this.objectAPIProduct2GURL + "?includeLabels=true", reqObj.toString());
								if(rawResp != null) {
									log("OOP<::>" + rawResp);
									org.json.JSONObject jo = new org.json.JSONObject(rawResp);
									externalProductId = jo == null ? "" :
										jo.getJSONObject("_entityItem").getString("_externalId").split("@")[0]
												.replaceAll("^'|'$", "");
								}else {
									log("See this: " + rawResp);
								}
							}
						}
						log("Tutul té? " + ex);
						if (rawResp != null && rawResp.contains(" not found in enumeration 'Enum.CharacteristicLookupValueEnumProvider'. Either the code is not part of the enumeration or the user has no read permission")) {
							log("Échale un vistazo a: " + reqObj);
							java.util.regex.Matcher m = java.util.regex.Pattern.compile("'(.+)(?=' not found in enumeration)").matcher(rawResp);
							org.json.JSONArray possibleProblems = new org.json.JSONArray();
							if(m.find()) {
								String v0 = m.group(1);
								if(v0 != null && !"".equals(v0)) {
									try {
										for(int z = 0; z<characteristicArray.length(); z++) {
											if( characteristicArray.getJSONObject(z).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").length() > 0 && characteristicArray.getJSONObject(z).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0) instanceof org.json.JSONObject ) {
												if(characteristicArray.getJSONObject(z).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).has("_code")) {
													if(v0.equals(characteristicArray.getJSONObject(z).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code") ) ) {
														possibleProblems.put(characteristicArray.getJSONObject(z).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"));
													}
												}else {
													log("JSONObject without _code: " + characteristicArray.getJSONObject(z).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0));
												}
											}
										}
									}catch(org.json.JSONException e) {
										logE(e);
									}
									log("Possible invalid fields: " + possibleProblems);
								}
							}
							throw new org.json.JSONException(new org.json.JSONObject().put("rawMessage", "Valor de característica no conocido en lista de valores.").put("possibleFieldProblems", possibleProblems).toString());
						}
						if(rawResp != null && rawResp.startsWith("java.lang.NullPointerException")) {
							log(reqObj.toString());
							throw new org.json.JSONException("Internal error processing payload.");
						}
						org.json.JSONObject jo = sample ? null : new org.json.JSONObject(rawResp);
						if(jo != null && jo.has("_protocol") && jo.getJSONObject("_protocol").getInt("errorCounter") > 0) {
							log("Problem: " + jo + ", given req: " + reqObj.toString());
							throw new org.json.JSONException("Problema persistiendo datos. Solicitar ayuda y presentar el siguiente código: " + myId + ", junto con la siguiente estampa temporal: " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
						}
						
						log("On writing proposal... " + rawResp);
						/** If there are any errors, should report them back **/
						if(new org.json.JSONObject(rawResp).getJSONObject("_protocol").getInt("errorCounter") == 0) {
							if("1021".equals(internalStatus))
								ingresaWorkflow(externalProductId, "23543", "IGIAStewardship", "Item Group Review");
							log("Bout to do");
							if(!sample) {
								log("Array ?" + (characteristicArray == null ? "x.x" : characteristicArray.length()));
								if(characteristicArray != null) {
									String ig = null;
									String igs = null;
									String bn = null;
									String bids = null;
									String bs = business;
									String spl = supplier;
									String tmpl = templateId;
									String cs = internalStatus;
									String atnt = "";
									String sot = null;
									String ftl = null;
									String mbc = null;
									String mbcs = null;
									String cid = null;
									try {
										jo = null;
										for(int m=0; m<characteristicArray.length(); m++) {
											jo = characteristicArray.getJSONObject(m);
											cid = jo.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
											if("Direction".equals(cid)) {
												direction = jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
											}else if("Section".equals(cid)) {
												section = jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
											}else if("ItemGroup".equals(cid)) {
												ig = jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
											}else if("ItemGroupS4H".equals(cid)) {
												igs = jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
											}else if("BrandName".equals(cid)) {
												bn = jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
											}else if("BRAND_ID_S4H".equals(cid)) {
												bids = jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
											}else if("Business".equals(cid)) {
												bs = jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0) instanceof org.json.JSONObject ? jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).has("_code") ? jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code") : "Marketplace".equals( jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label") ) ? "MKP" : "Liverpool".equals( jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label") ) ? "LVP" : "SBB" : String.valueOf( jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0) );
											}else if("SAPObjectType".equals(cid)) {
												sot = jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
											}else if("FotoTomadaLiverpool".equals(cid)) {
												ftl = jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
											}else if("MainBarCode".equals(cid)) {
												mbc = jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
											}else if("MainBarCodeS4H".equals(cid)) {
												mbcs = jo.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
											}
										}
										DataRequestor dr = new DataRequestor();
										if(externalProductId != null && !"".equals(externalProductId)) {
											String r = dr.getProductData( new org.json.JSONArray().put(externalProductId) );
											if(r != null) {
												log("JK -> " + r);
												org.json.JSONObject jr = new org.json.JSONObject(r);
												org.json.JSONArray items = jr.getJSONArray("items");
												org.json.JSONObject item = items.getJSONObject(0);
												if(section != null)
													item.put("Section", section);
												if(ig != null)
													item.put("ItemGroup", ig);
												if(igs != null)
													item.put("ItemGroupS4H", igs);
												if(bn != null)
													item.put("BrandName", bn);
												if(bids != null)
													item.put("BRAND_ID_S4H", bids);
												if(bs != null)
													item.put("Business", bs);
												if(spl != null)
													item.put("SupplierID", spl);
												if(tmpl != null && !"".equals(tmpl))
													item.put("Template", tmpl);
												if(cs != null)
													item.put("CurrentStatus", cs);
												if(atnt != null)
													item.put("AssignTakeNoTake", atnt);
												if(sot != null)
													item.put("SAPObjectType", sot);
												if(ftl != null)
													item.put("FotoTomadaLiverpool", ftl);
												if(mbc != null)
													item.put("MainBarCode", mbc);
												if(mbcs != null)
													item.put("MainBarCodeS4H", mbcs);
												log( "to local admin: " + dr.putProductData(new org.json.JSONArray().put(item)) );
												log("*** " + item + " ***");
											}else {
												log("Got null for " + externalProductId);
											}
										}else {
											log("no productId this time... u.u ");
										}
									}catch(org.json.JSONException | NullPointerException e) {
										logE(e);
									}
								}
							}else {
								log("Yerk.");
							}
						}
						try {
							String labelPrevStat =  null;
							String labelInternalStat = null;
							String labelExternalStat = null;
							labelPrevStat = statusEnum.get(previousStatus);
							labelInternalStat = statusEnum.get(internalStatus);
							labelExternalStat = externalStatusEnum.get(externalStatus); log("--->" + externalStatus + "|" + labelExternalStat + "<::>" + externalStatusEnum);
							itemGroup = itemGroup != null
									? itemGroup.matches("\\d+ ?- ?.+?") ? itemGroup.replaceAll(" ?- ?.+", "") : itemGroup
									: null;
							itemGroupS4H = itemGroupS4H != null
									? itemGroupS4H.matches("\\d+ ?- ?.+?") ? itemGroupS4H.replaceAll(" ?- ?.+", "")
											: itemGroupS4H
									: null;
							direccionSeccion = !"Marketplace".equals(business)
									? getDireccionSeccion(itemGroup != null ? itemGroup : itemGroupS4H, business)
									: null;
							log("Item Group is: " + itemGroup);
							if(direccionSeccion != null) {
								log(java.util.Arrays.asList(direccionSeccion).toString());
							} 
							JSONObject rsp = rawResp == null ? null : new JSONObject(rawResp);
							genericResponse = new JSONObject();
							genericResponse.put("direccion", direccionSeccion != null ? direccionSeccion[0] : null)
									.put("seccion",
											direccionSeccion != null && direccionSeccion.length > 1 ? direccionSeccion[1]
													: null)
									.put("SAPObjectType", sapObjectTypeLabel)
									.put("creationDate",
											creationDate == null
													? new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(
															new java.util.Date())
													: creationDate)
									.put("previousStatus", labelPrevStat == null ? "" : labelPrevStat)
									.put("internalStatus", labelInternalStat == null ? "" : labelInternalStat)
									.put("externalStatus", labelExternalStat == null ? "" : labelExternalStat)
									.put("structureProblems", structureProblems.length() > 0 ? structureProblems : null)
									.put("proposalId", externalProductId = (rsp == null ? "" :
															rsp.getJSONObject("_entityItem").getString("_externalId").split("@")[0]
																	.replaceAll("^'|'$", "")
													)
										);
							variantResponsesArray = new org.json.JSONArray();
							log("<::> Status: " + internalStatus + "<::> " + previousStatus);
							if( "1001".equals(internalStatus) || "1001".equals(previousStatus) ) {
							}
							for (int j = 0; j < variantes.length(); j++) {
								variante = variantes.getJSONObject(j);
								processVariant(
										  externalProductId
									    , variante
										, this.rc, business
										, templateId
										, d
										, internalStatus
										, supplier
										, sample
									);
							}
							
							org.json.JSONArray tru = new org.json.JSONArray();
							for(int m=0; m<variantResponsesArray.length(); m++) {
								if(variantResponsesArray.getJSONObject(m).length() == 0) {
								}else {
									variantResponsesArray.getJSONObject(m).put("variantPosition", m);
									tru.put(variantResponsesArray.getJSONObject(m));
								}
							}
							genericResponse.put("variants", tru);
							genericResponse.put("fieldProblems", genericFieldErrors);
							responses.put(genericResponse);
						} catch (org.json.JSONException e) {
							logE(e);
							log("There was an exception: " + e.getMessage() + ", received: " + rawResp);
							try{
								genericResponse = genericResponse == null ? new org.json.JSONObject() : genericResponse;
								genericResponse.put("Error", new JSONObject(rawResp));
							}catch(org.json.JSONException ex) {
								genericResponse = genericResponse == null ? new org.json.JSONObject() : genericResponse;
								genericResponse.put("Error", rawResp);
							}
							responses.put(genericResponse);
						}
					} else {
						log("Los errores...");
						genericResponse = new JSONObject();
						if(!"InProgress".equals(userAction)) {
							genericResponse.put("fieldProblems", genericFieldErrors);
							if(variantResponsesArray != null && variantResponsesArray.length() > 0) {
								org.json.JSONArray tru = new org.json.JSONArray();
								for(int m=0; m<variantResponsesArray.length(); m++) {
									if(variantResponsesArray.getJSONObject(m).length() == 0) {
									}else {
										variantResponsesArray.getJSONObject(m).put("variantPosition", m);
										tru.put(variantResponsesArray.getJSONObject(m));
									}
								}
								genericResponse.put("variants", tru);
							}
						}else {
							if(genericFieldErrors.length() > 0) {
								genericResponse.put("fieldProblems", genericFieldErrors);
							}
							if(variantResponsesArray != null && variantResponsesArray.length() > 0) {
								org.json.JSONArray tru = new org.json.JSONArray();
								for(int m=0; m<variantResponsesArray.length(); m++) {
									if(variantResponsesArray.getJSONObject(m).length() == 0) {
									}else {
										variantResponsesArray.getJSONObject(m).put("variantPosition", m);
										tru.put(variantResponsesArray.getJSONObject(m));
									}
								}
								genericResponse.put("variants", tru);
							}
						}
						responses.put(genericResponse);
					}
				} catch (Exception e) {
					logE(e);
					log("There was an exception: " + e.getMessage() + ", received: " + rawResp);
					try{
						genericResponse = genericResponse == null ? new org.json.JSONObject() : genericResponse;
						genericResponse.put("Error", rawResp == null ? "Not known error" : "Error al procesar petición. " + e.getMessage());
					}catch(org.json.JSONException ex) {
						genericResponse = genericResponse == null ? new org.json.JSONObject() : genericResponse;
						genericResponse.put("Error", "Error al procesar petición: " + ex.getMessage());
					}
					responses.put(genericResponse);
				}
				genericFieldErrors = new org.json.JSONArray();
				genericResponse = new JSONObject();
				variantResponsesArray = new org.json.JSONArray();
			}
		}catch (ServiceUnavailableException e) {
			logE(e);
			log("Elapsed time response: " + workshop.formatTime(System.currentTimeMillis() - init));
			throw e;
		}catch (Exception e) {
			try {
				response = new org.json.JSONObject().put("Error", "Petición mal formada.").toString();
			} catch (org.json.JSONException ignore) {
			}
			logE(e);
		}

		response = response != null ? response
				: new JSONObject().put("responses", responses).put("notFoundOrInactiveCharacteristics", notFound)
						.toString();
		responses = new org.json.JSONArray();
		notFound = new org.json.JSONArray();
		charCategories.clear();
		genericResponse = null;
		genericFieldErrors = new org.json.JSONArray();
		variantFieldErrors = new org.json.JSONArray();
		nextStatusMap.clear();
		externalStatusMap.clear();
		if(deleteInputFile) {
		}
		log("Elapsed time response: " + workshop.formatTime(System.currentTimeMillis() - init));
		return response;
	}

	private void ingresaWorkflow(String internalId, String processId, String workflowId, String status) {
		org.json.JSONObject rb = new org.json.JSONObject();
		rb.put("processId", processId);
		rb.put("workflowId", workflowId);
		rb.put("status", status);
		rb.put("entity", "Product2G");
		org.json.JSONArray itemIds = new org.json.JSONArray();
		org.json.JSONObject response = null;
		itemIds.put(internalId);
		rb.put("itemId", itemIds);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		response = workshop.makeRequest("POST", "/manage/workflow/status/enter", qp, rb.toString());
		log(response == null ? "ERR: " + workshop.getRawResponse() : response.toString());
	}
	
//	private java.util.Map<String, String[]> readVariantEANData(){
//		java.util.Map<String, String[]> data = new java.util.TreeMap<>();
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.templates_cache_directory"), 
//				PropertiesManager.get("p360.contingency.article_ean_file")).toString())))){
//			String line = null;
//			String[] pieces = null;
//			int a = 0;
//			while((line = br.readLine()) != null) {
//				pieces = workshop.parseLine(line, "\"", ";", "\\");
//				a++;
//				try{
//					data.put(pieces[0], new String[] { pieces[1], pieces[2] });
//				}catch(ArrayIndexOutOfBoundsException e) {
//					log("Línea mal formada -->" + a + "<-- " + a);
//				}
//			}
//		}catch(java.io.IOException e) {
//			logE(e);
//		}
//		return data;
//	}

	private boolean errorInVariant() {
		for(int i=0; i<variantResponsesArray.length(); i++) {
			if(variantResponsesArray.getJSONObject(i).has("fieldProblems") && variantResponsesArray.getJSONObject(i).getJSONArray("fieldProblems").length() > 0) {
				log("Habían errores en variante :) " + variantResponsesArray);
				return true;
			}
		}
		return false;
	}

	private void processVariant(String objectId, JSONObject variant,
			RestClient rc, String business, String template, String d, String proposalStatus
			, String supplier, boolean sample)
			throws Exception {
		DataRequestor dr = new DataRequestor();
		String rawResp = null;
		String internalItemId = null;
		JSONObject resp = null;
		JSONObject attributes = null;
		JSONObject logisticData = null;
		JSONObject datosVenta = null;
		org.json.JSONArray photosArray = null;
		JSONObject photo = null;
		org.json.JSONArray cosos = null;
		java.util.Map<String, String> validCodes = null;
		String codeValue = null;
		String previousStatus = "";
		String internalStatus = "";
		String externalStatus = null;
		String nextStatus = null;
		String[] ids = null;
		String externalItemId = null;
		String userAction = null;
		String targetRole = "";
		String creationDate = null;
		String lookup = null;
		String color = null;
		String tamaño = null;
		String mainBarCode = null;
		String supplierPartNumber = null;
		String productImage = null;
		String sapObjectTypeLabel = null;
		String sapObjectType = null;
		org.json.JSONArray structureProblems = new org.json.JSONArray();
		try {
			log("Came to variant");
			userAction = variant.has("userAction") ? variant.getString("userAction") : null;
			targetRole = variant.has("targetRole") ? variant.getString("targetRole") : null;
			variant.remove("userAction");
			if (!variant.has("variantId")) {
				/** Make request using object API to generate an ID **/
				try {
					variant.put("template", template);
					variant.put("Business", business);
					ids = createArticle(variant, null, null, false, objectId, template, sample);
					variant.remove("template");
					variant.remove("Business");
					if (ids != null) {
						internalItemId = ids[0];
						externalItemId = ids[1];
					}
				} catch (Exception e) {
					logE(e);
				}
			} else {
				externalItemId = variant.getString("variantId");
				rawResp = rc.getRequest("GET", objectAPIArticleURL + "/'" + externalItemId + "'@'MASTER'?includeLabels=true&includeIds=true&entityFilter=Article,ArticleLog",null);
				resp = new JSONObject(rawResp);
				if (resp.length() > 0 && resp.has("_data")) {
					org.json.JSONObject data = resp.getJSONObject("_data");
					internalItemId = !resp.has("_entityItem") ? null : resp.getJSONObject("_entityItem").getString("_internalId");
					internalStatus = !data.has("currentStatus") ? "" : String.valueOf( data.getJSONObject("currentStatus").getInt("_key") );
					previousStatus = !data.has("previousStatus") ? "" : String.valueOf( data.getJSONObject("previousStatus").getInt("_key") );
					externalStatus = !data.has("externalStatus") ? "" : data.getJSONObject("externalStatus").getString("_code");
					org.json.JSONArray log = data.has("log") ? data.getJSONArray("log") : null;
					if(log != null) {
						for(int a=0; a<log.length(); a++) {
							if("HPM".equals( log.getJSONObject(a).getJSONObject("_qualification").getJSONObject("channel").getString("_key")) ) {
								creationDate = log.getJSONObject(a).getString("creationDate");
								break;
							}
						}
					}
				} else {
					variant.put("Business", business);
					variant.put("template", template);
					ids = createArticle(variant, null, externalItemId, false, objectId, template, sample);
					if (ids != null) {
						internalItemId = ids[0];
					}
					variant.remove("template");
					variant.remove("Business");
				}
			}
			if (internalItemId == null) {
				/** There was an error **/
				log("MeSi");
				return;
			}
			log("Variant User Action: " + (userAction == null ? proposalStatus + " (from parent)" : userAction) + ", Target Role: " + ("".equals(targetRole) ? "No target role" : targetRole));
			if(userAction != null) {
				previousStatus = "10031".equals(previousStatus) ? "" : previousStatus;
				internalStatus = "10031".equals(internalStatus) ? "" : internalStatus;
				if("||F|Compras".equals(previousStatus + "|" + internalStatus + "|" + userAction.substring(0, 1) + "|" + targetRole)) {
					internalStatus = "1001";
				}
				nextStatus = this.nextStatusMap
						.get(previousStatus + "|" + internalStatus + "|" + userAction.substring(0, 1) + "|" + targetRole);
				if(nextStatus == null) {
					log("No valid key found: " + previousStatus + "|" + internalStatus + "|" + userAction.substring(0, 1) + "|" + targetRole + ": " + nextStatus);
					variantResponsesArray.put(new org.json.JSONObject().put("faultCode", 400).put("message", "Problema técnico de incompatibilidad de estatus, acción y rol de destino, el valor de la llave \"userAction\": " + userAction + ", en conjunto con el valor de la llave \"targetRole\": " + targetRole + ", para el estatus actual de la propuesta: \"" + statusEnum.get(internalStatus) + "\" y estado previo de la propuesta: \"" + statusEnum.get(previousStatus) + "\", es desconocido, favor de reportarlo con el equipo de soporte."));
					return;
				}
				log("For: " + previousStatus + "|" + internalStatus + "|" + userAction.substring(0, 1) + "|" + targetRole + ": " + nextStatus);
				internalStatus = internalStatus == null || "".equals(internalStatus) ? "10031" : internalStatus;
				previousStatus = nextStatus != null && !"".equals(nextStatus) ? internalStatus : previousStatus;
				internalStatus = nextStatus != null && !"".equals(nextStatus) ? nextStatus : internalStatus;
				externalStatus = this.externalStatusMap.get(internalStatus);
				externalStatus = externalStatus == null ? "Borrador" : externalStatus;
			}else{
				if(proposalStatus != null) {
					previousStatus = internalStatus;
					internalStatus = proposalStatus == null || "".equals(proposalStatus) ? "10031" : proposalStatus;
					externalStatus = this.externalStatusMap.get(proposalStatus);
					externalStatus = externalStatus == null ? "Borrador" : externalStatus;
				}
			}
			variant.remove("variantId");
			variant.remove("currentStatus");
			variant.remove("externalStatus");
			variant.remove("previousStatus");
			attributes = (JSONObject) variant.remove("attributes");
			logisticData = (JSONObject) variant.remove("logisticData");
			datosVenta = (JSONObject) variant.remove("datosVenta");
			photosArray = (org.json.JSONArray) variant.remove("photos");
			if(photosArray == null) {
				photosArray = new org.json.JSONArray();
			}
			cosos = new org.json.JSONArray()
					.put(variant);
			if (attributes != null) {
				cosos.put(attributes);
			}
			if (logisticData != null) {
				cosos.put(logisticData);
			}
			if (datosVenta != null) {
				cosos.put(datosVenta);
			}
			if (cosos.length() > 0) {
			}
			org.json.JSONArray characteristicArray = new org.json.JSONArray();
			org.json.JSONArray writeDataFails = new org.json.JSONArray();
			org.json.JSONArray children  = null;
			JSONObject charBody = null;
			String holder = null;
			for (int j = 0; j < cosos.length(); j++) {
				if (!JSONObject.NULL.equals(cosos.get(j)) && cosos.get(j) != null) {
					if(JSONObject.getNames(cosos.getJSONObject(j)) != null) {
						for (String name : JSONObject.getNames(cosos.getJSONObject(j))) {
							holder = String.valueOf(cosos.getJSONObject(j).get(name)).replaceAll(" {2,}", " ").trim();
							log(externalItemId + " -- " + name + " -->" + holder);
							if("TypeMainBarCode".equals(name) || "NUMTP_S4H".equals(name)) {
								log("EléjeleeeeeeEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee: " + name + "<::>" + holder);
								charBody = new org.json.JSONObject()
									.put("_qualification",
										new JSONObject().put("characteristic",
											new JSONObject().put("_code", name)))
									.put("_recordLang", new org.json.JSONArray()
										.put(new JSONObject().put("values", new org.json.JSONArray()
											.put(new org.json.JSONObject().put("_code", holder)))));
								characteristicArray.put(charBody);
								log("Continuing...");
								continue;
							}
							if("".equals(holder)) {
								continue;
							}
							if("SAPObjectType".equals(name)) {
								sapObjectType = holder;
								sapObjectTypeLabel = "00".equals(sapObjectType) ? "Artículo Individual" : "02".equals(sapObjectType) ? "Variante" : null;
							}
//							if (characteristicsThatAreLookups == null) {
//								characteristicsThatAreLookups = getCharacteristicsThatAreLookups();
//							}
							String drr = dr.getCharacteristicData(new org.json.JSONArray().put(name));
							lookup = drr != null ? new org.json.JSONObject(drr).getJSONArray("items").getJSONObject(0).getString("lookup") : null; // characteristicsThatAreLookups.get(name);
//							lookup = characteristicsThatAreLookups.get(name);
							if (lookup == null || "".equals(lookup)) {
								if("SupplierPartNumber".equals(name)) {
									supplierPartNumber = holder;
								}else if("MainBarCode".equals(name)) {
									mainBarCode = holder.replaceAll("\s{2,}", " ").trim();
								}else if("MainBarCodeS4H".equals(name)) {
									mainBarCode = holder.replaceAll("\s{2,}", " ").trim();
								}
								charBody = new org.json.JSONObject()
										.put("_qualification",
												new JSONObject().put("characteristic", new JSONObject().put("_code", name)))
										.put("_recordLang", new org.json.JSONArray().put(new JSONObject().put("values",
												new org.json.JSONArray().put(holder))));
								characteristicArray.put(charBody);
							} else {
								if("TypeMainBarCode".equals(name)) {
									log("##########################$$$$$$$$$$$$$$$$$$################## PANIC: why did it get here... " + name + " - " + holder);
									continue;
								}
								validCodes = procedeACargarValoresValidos(template, name);
								if("TamanoUnico".equals(name)) {
									log("kkkkkkkkkkMMMMM " + template + " MMMMkkkkkkkk " + validCodes.size());
								}
								if (validCodes.isEmpty()) {
									log("Loading allllllll values...");
									validCodes = procedeACargarValoresLookup(lookup);
									log(validCodes.size() + " found finally.");
								} else {
									log("Enough with filter values // kkkkkkkkkkMMMMMMMMMkkkkkkkk");
								}
								if (!validCodes.isEmpty()) {
									codeValue = validCodes.get(holder);
									if (codeValue != null) {
										if("TamanoUnico".equals(name)) {
											tamaño = codeValue;
										}else if("ColoursLiverpoolAtt".equals(name)) {
											color = codeValue;
										}
										charBody = new org.json.JSONObject()
												.put("_qualification",
														new JSONObject().put("characteristic",
																new JSONObject().put("_code", name)))
												.put("_recordLang", new org.json.JSONArray()
														.put(new JSONObject().put("values", new org.json.JSONArray()
																.put(new org.json.JSONObject().put("_code", codeValue)))));
										characteristicArray.put(charBody);
									} else {
										children = new org.json.JSONArray();
										children.put(new org.json.JSONObject()
												.put("_qualification",
														new org.json.JSONObject()
																.put("characteristic",
																		new org.json.JSONObject().put("_code",
																				"WriteDataIssue_CharacteristicID")))
												.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
														new org.json.JSONArray().put(name)))));
										children.put(new org.json.JSONObject()
												.put("_qualification",
														new org.json.JSONObject()
																.put("characteristic",
																		new org.json.JSONObject().put("_code",
																				"WriteDataIssue_TiempoReportado")))
												.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
														new org.json.JSONArray().put(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new java.util.Date()))))));
										children.put(new org.json.JSONObject()
												.put("_qualification",
														new org.json.JSONObject()
														.put("characteristic",
																new org.json.JSONObject().put("_code",
																		"WriteDataIssue_Detalle")))
												.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
														new org.json.JSONArray().put("No fue posible encontrar un identificador válido para la característica en la plantilla, posiblemente por contar con acotación y no estar actualizada. Valor proporcionado: " + cosos.getJSONObject(j).getString(name).substring(0, Integer.min(cosos.getJSONObject(j).getString(name).length(), 1825)))))));
										writeDataFails
											.put(new org.json.JSONObject()
													.put("_qualification",
															new JSONObject()
																	.put("characteristic",
																			new JSONObject().put("_code", "WriteDataIssue")))
													.put("_recordLang",
															new org.json.JSONArray()
																	.put(new JSONObject().put("values", new org.json.JSONArray())))
													.put("_children", children));
									}
								} else {
									children = new org.json.JSONArray();
									children.put(new org.json.JSONObject()
											.put("_qualification",
													new org.json.JSONObject()
															.put("characteristic",
																	new org.json.JSONObject().put("_code",
																			"WriteDataIssue_CharacteristicID")))
											.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
													new org.json.JSONArray().put(name)))));
									children.put(new org.json.JSONObject()
											.put("_qualification",
													new org.json.JSONObject()
															.put("characteristic",
																	new org.json.JSONObject().put("_code",
																			"WriteDataIssue_TiempoReportado")))
											.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
													new org.json.JSONArray().put(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new java.util.Date()))))));
									children.put(new org.json.JSONObject()
											.put("_qualification",
													new org.json.JSONObject()
													.put("characteristic",
															new org.json.JSONObject().put("_code",
																	"WriteDataIssue_Detalle")))
											.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
													new org.json.JSONArray().put("No fue posible encontrar un identificador válido para la característica en la plantilla, posiblemente por contar con acotación y no estar actualizada. Valor proporcionado: " + cosos.getJSONObject(j).getString(name).substring(0, Integer.min(cosos.getJSONObject(j).getString(name).length(), 1825)))))));
									writeDataFails
										.put(new org.json.JSONObject()
												.put("_qualification",
														new JSONObject()
																.put("characteristic",
																		new JSONObject().put("_code", "WriteDataIssue")))
												.put("_recordLang",
														new org.json.JSONArray()
																.put(new JSONObject().put("values", new org.json.JSONArray())))
												.put("_children", children));
								}
							}
						}
					}
				}
			}
			int timesDetailImage = 0;
			int timesIllustration = 0;
			int timesSmosh = 0;
			String recordKey = null;
			for (int j = 0; j < photosArray.length(); j++) {
				photo = photosArray.getJSONObject(j);
				try {
					if (photo.getString("PhotoAssetType").startsWith("ProductImageDetail")) {
						recordKey = timesDetailImage == 0 ? "0000.0000.RK" : "0000." + ( timesDetailImage < 10 ? "000" + timesDetailImage : timesDetailImage < 100 ? "00" + timesDetailImage : timesDetailImage < 1000 ? "0" + timesDetailImage : timesDetailImage ) + ".RK";
						children = new org.json.JSONArray();
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", recordKey)
												.put("characteristic",
														new org.json.JSONObject().put("_code",
																"ProductImageDetail_Name")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", recordKey)
												.put("characteristic",
														new org.json.JSONObject().put("_code",
																"ProductImageDetail_URL")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
						if(photo.has("PhotoAssetStatus")) {
							children.put(
								new org.json.JSONObject()
										.put("_qualification",
												new org.json.JSONObject()
												.put("recordKey", recordKey)
														.put("characteristic",
																new org.json.JSONObject().put("_code",
																		"ProductImageDetail_Status")))
										.put("_recordLang",
												new org.json.JSONArray()
														.put(new org.json.JSONObject().put("values",
																new org.json.JSONArray()
																		.put(new JSONObject()
																				.put("_qualification",
																						new JSONObject().put(
																								"language",
																								new JSONObject().put(
																										"_code", "zxx")))
																				.put("_label", photo.optString(
																						"PhotoAssetStatus")))))));
						}
						characteristicArray
								.put(new org.json.JSONObject()
										.put("_qualification",
												new JSONObject()
												.put("recordKey", recordKey)
														.put("characteristic",
																new JSONObject().put("_code", "ProductImageDetail")))
										.put("_recordLang",
												new org.json.JSONArray()
														.put(new JSONObject().put("values", new org.json.JSONArray())))
										.put("_children", children));
						timesDetailImage++;
					} else if (photo.getString("PhotoAssetType").equals("ProductImage")) {
						log("Adding a productImage ");
						children = new org.json.JSONArray();
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", "0000.0000.RK")
												.put("characteristic",
														new org.json.JSONObject().put("_code", "ProductImage_Name")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", "0000.0000.RK")
												.put("characteristic",
														new org.json.JSONObject().put("_code", "ProductImage_URL")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
						productImage = photo.getString("PhotoAssetURL");
						if(photo.has("PhotoAssetStatus")) {
							children.put(
								new org.json.JSONObject()
										.put("_qualification",
												new org.json.JSONObject()
												.put("recordKey", "0000.0000.RK")
														.put("characteristic",
																new org.json.JSONObject().put("_code",
																		"ProductImage_Status")))
										.put("_recordLang",
												new org.json.JSONArray()
														.put(new org.json.JSONObject().put("values",
																new org.json.JSONArray()
																		.put(new JSONObject()
																				.put("_qualification",
																						new JSONObject().put(
																								"language",
																								new JSONObject().put(
																										"_code", "zxx")))
																				.put("_label", photo.optString(
																						"PhotoAssetStatus")))))));
						}
						characteristicArray.put(new org.json.JSONObject().put("_qualification",
								new JSONObject()
									.put("recordKey", "0000.0000.RK")
										.put("characteristic", new JSONObject().put("_code", "ProductImage")))
								.put("_recordLang",
										new org.json.JSONArray()
												.put(new JSONObject().put("values", new org.json.JSONArray())))
								.put("_children", children));
					} else if (photo.getString("PhotoAssetType").startsWith("Illustration")) {
						recordKey = timesIllustration == 0 ? "0000.0000.RK" : "0000." + ( timesIllustration < 10 ? "000" + timesIllustration : timesIllustration < 100 ? "00" + timesIllustration : timesIllustration < 1000 ? "0" + timesIllustration : timesIllustration ) + ".RK";
						children = new org.json.JSONArray();
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey",recordKey)
												.put("characteristic",
														new org.json.JSONObject().put("_code", "Illustration_Name")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", recordKey)
												.put("characteristic",
														new org.json.JSONObject().put("_code", "Illustration_URL")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
						if(photo.has("PhotoAssetStatus")) {
							children.put(
								new org.json.JSONObject()
										.put("_qualification",
												new org.json.JSONObject()
												.put("recordKey", recordKey)
														.put("characteristic",
																new org.json.JSONObject().put("_code",
																		"Illustration_Status")))
										.put("_recordLang",
												new org.json.JSONArray()
														.put(new org.json.JSONObject().put("values",
																new org.json.JSONArray()
																		.put(new JSONObject()
																				.put("_qualification",
																						new JSONObject().put(
																								"language",
																								new JSONObject().put(
																										"_code", "zxx")))
																				.put("_label", photo.optString(
																						"PhotoAssetStatus")))))));
						}
						characteristicArray.put(new org.json.JSONObject().put("_qualification",
								new JSONObject()
								.put("recordKey", recordKey)
										.put("characteristic", new JSONObject().put("_code", "Illustration")))
								.put("_recordLang",
										new org.json.JSONArray()
												.put(new JSONObject().put("values", new org.json.JSONArray())))
								.put("_children", children));
						timesIllustration++;
					} else if (photo.getString("PhotoAssetType").startsWith("ProductImageSmosh")) {
						recordKey = timesSmosh == 0 ? "0000.0000.RK" : "0000." + ( timesSmosh < 10 ? "000" + timesSmosh : timesSmosh < 100 ? "00" + timesSmosh : timesSmosh < 1000 ? "0" + timesSmosh : timesSmosh ) + ".RK";
						children = new org.json.JSONArray();
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", recordKey)
												.put("characteristic",
														new org.json.JSONObject().put("_code",
																"ProductImageSmosh_Name")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetName"))))));
						children.put(new org.json.JSONObject()
								.put("_qualification",
										new org.json.JSONObject()
										.put("recordKey", recordKey)
												.put("characteristic",
														new org.json.JSONObject().put("_code",
																"ProductImageSmosh_URL")))
								.put("_recordLang", new org.json.JSONArray().put(new org.json.JSONObject().put("values",
										new org.json.JSONArray().put(photo.getString("PhotoAssetURL"))))));
						if(photo.has("PhotoAssetStatus")) {
							children.put(
									new org.json.JSONObject()
											.put("_qualification",
													new org.json.JSONObject()
													.put("recordKey",recordKey)
															.put("characteristic",
																	new org.json.JSONObject().put("_code",
																			"ProductImageSmosh_Status")))
											.put("_recordLang",
													new org.json.JSONArray()
															.put(new org.json.JSONObject().put("values",
																	new org.json.JSONArray()
																			.put(new JSONObject()
																					.put("_qualification",
																							new JSONObject().put(
																									"language",
																									new JSONObject().put(
																											"_code", "zxx")))
																					.put("_label", photo.optString(
																							"PhotoAssetStatus")))))));
						}
						characteristicArray
								.put(new org.json.JSONObject()
										.put("_qualification",
												new JSONObject()
												.put("recordKey", recordKey)
														.put("characteristic",
																new JSONObject().put("_code", "ProductImageSmosh")))
										.put("_recordLang",
												new org.json.JSONArray()
														.put(new JSONObject().put("values", new org.json.JSONArray())))
										.put("_children", children));
						timesSmosh++;
					}
				} catch (org.json.JSONException | NullPointerException e) {
					structureProblems.put(new org.json.JSONObject()
							.put("Message", "Error in structure, failed when processing photo object structure. Mandatory attributes are: PhotoAssetURL and PhotoAssetName")
							.put("Object", photo));
				}
			}
			if(writeDataFails != null && writeDataFails.length() > 0) {
				for(int i=0; i<writeDataFails.length(); i++) {
					characteristicArray.put(writeDataFails.getJSONObject(i));
				}
			}
			JSONObject reqObj = new org.json.JSONObject().put("_characteristicRecords", characteristicArray);
			reqObj.put("currentStatus", new org.json.JSONObject().put("_code", internalStatus));
			if (!"".equals(previousStatus) && previousStatus != null) {
				reqObj.put("previousStatus", new org.json.JSONObject().put("_code", previousStatus));
			}
			reqObj.put("externalStatus", new org.json.JSONObject().put("_code", externalStatus));
			log("Cocqiutus: " + proposalStatus + ", business: " + business);
			if(("Liverpool".equals(business) || "Suburbia".equals(business)) && "1020".equals(proposalStatus)) {
				String[] supplierData = parties.get(supplier);
				log("Parties: " + rw.getRw().serializeChunk(supplierData));
				String supplierType = supplierData[2];
				java.time.LocalDate ld = java.time.LocalDate.now();
				java.time.LocalDate cd = ld;
				int added = 0;
				int toAdd = 2;
				java.time.DayOfWeek dow = null;
				while(added < toAdd) {
					cd.plusDays(1);
					dow = cd.getDayOfWeek();
					if(dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY) {
						added++;
					}
				}
				String dtv = cd.format( java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd") );
				if("PNA".equals(supplierType)) {
					charBody = new org.json.JSONObject()
							.put("_qualification",
									new JSONObject().put("characteristic",
											new JSONObject().put("_code", "FechaInicioVigenciaPrecioVenta")))
							.put("_recordLang", new org.json.JSONArray()
									.put(new JSONObject().put("values", new org.json.JSONArray()
											.put(dtv))));
					characteristicArray.put(charBody);
					charBody = new org.json.JSONObject()
							.put("_qualification",
									new JSONObject().put("characteristic",
											new JSONObject().put("_code", "FechaInicioVigenciaCostoNeto")))
							.put("_recordLang", new org.json.JSONArray()
									.put(new JSONObject().put("values", new org.json.JSONArray()
											.put(dtv))));
					characteristicArray.put(charBody);
				}else {
					charBody = new org.json.JSONObject()
							.put("_qualification",
									new JSONObject().put("characteristic",
											new JSONObject().put("_code", "FechaInicioVigenciaPrecioVenta")))
							.put("_recordLang", new org.json.JSONArray()
									.put(new JSONObject().put("values", new org.json.JSONArray()
											.put(dtv))));
					characteristicArray.put(charBody);
					charBody = new org.json.JSONObject()
							.put("_qualification",
									new JSONObject().put("characteristic",
											new JSONObject().put("_code", "FechaInicioVigenciaCostoImportacion")))
							.put("_recordLang", new org.json.JSONArray()
									.put(new JSONObject().put("values", new org.json.JSONArray()
											.put(dtv))));
					characteristicArray.put(charBody);
				}
			}

			if(business != null) {
				reqObj.put("business", new org.json.JSONObject().put("_label", business));
			}
			org.json.JSONObject extraData = new org.json.JSONObject();
			if(color != null) {
				extraData.put("coloursLiverpoolAtt", new org.json.JSONObject().put("_code", color) );
			}
			if(tamaño != null) {
				extraData.put("tamanoUnico", new org.json.JSONObject().put("_code", tamaño) );
			}
			if(sapObjectType != null && sapObjectTypeLabel != null) {
				extraData.put("sapObjectType", new org.json.JSONObject().put("_code", sapObjectType) );
			}else if(sapObjectType != null && sapObjectTypeLabel == null) {
				extraData.put("sapObjectType", new org.json.JSONObject().put("_code", "Variante".equals( sapObjectType ) ? "02" : "00") );
			}
			if(supplierPartNumber != null) {
				extraData.put("supplierPartNumber", supplierPartNumber);
			}
			if(mainBarCode != null && !"".equals(mainBarCode)) {
				reqObj.put("gtin", mainBarCode);
			}
			if(productImage != null){
				reqObj.put("productImageURL", productImage);
			}
			if(extraData.length() > 0) {
				extraData.put("_qualification", new org.json.JSONObject().put("targetMarket", new org.json.JSONObject().put("_code", "MX")));
				reqObj.put("articleExtraData", extraData);
			}
			log("REO: " + reqObj);
			try {
				rc.getHeader().put("Accept-Language", "es");
				if(!sample) {
					rawResp = rc.getRequest("PUT", this.objectAPIArticleURL + "/" + internalItemId + "?includeLabels=true", reqObj.toString());
					log("From PUT (" + variant + "): " + rawResp + "");
					resp = new JSONObject(rawResp);
					/** If there are any errors, should report them back **/
					if(resp.getJSONObject("_protocol").getInt("errorCounter") == 0 && externalItemId != null && objectId != null && !"".equals(objectId)) {
						String r = dr.getArticleData(new org.json.JSONArray().put(externalItemId));
						if(r != null) {
							try{
								org.json.JSONObject rj = new org.json.JSONObject(r);
								org.json.JSONArray items = rj.getJSONArray("items");
								org.json.JSONObject item = items.getJSONObject(0);
								String pn = item.getString("ProductNo");
								if("".equals(pn)) {
									item.put("ProductNo", objectId);
								}
								if(productImage != null) {
									item.put("ProductImage", productImage);
								}
								if(color != null) {
									item.put("ColoursLiverpoolAtt", color);
								}
								if(tamaño != null) {
									item.put("TamanoUnico", tamaño);
								}
								if(supplierPartNumber != null) {
									item.put("SupplierPartNumber", supplierPartNumber);
								}
								if(mainBarCode != null) {
									item.put("MainBarCode", mainBarCode);
								}
								log("Sending variant data to admin (" + items + "): " + dr.putArticleData(items));
							}catch(org.json.JSONException e) {
								logE(e);
							}
						}else {
							log("(VAR) fue null");
						}
					}
				}else {
					log("was a sample.");
					resp = null;
				}
				try {
					String labelPrevStat = statusEnum.get(previousStatus);
					String labelInternalStat = statusEnum.get(internalStatus);
					String labelExternalStat = externalStatusEnum.get(externalStatus);
					variantResponsesArray.put(new JSONObject()
							.put("fieldProblems", variantFieldErrors).put("SAPObjectType",
									sapObjectTypeLabel)
							.put("creationDate",
									creationDate == null
									? new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(
											new java.util.Date())
											: creationDate)
							.put("previousStatus", labelPrevStat == null ? "" : labelPrevStat)
							.put("internalStatus", labelInternalStat == null ? "" : labelInternalStat)
							.put("externalStatus", labelExternalStat == null ? "" : labelExternalStat)
							.put("structureProblems", structureProblems.length() > 0 ? structureProblems : null)
							.put("variantId", resp == null ? "" : resp.getJSONObject("_entityItem").getString("_externalId").split("@")[0]
									.replaceAll("^'|'$", "")));
				} catch (org.json.JSONException e) {
					logE(e);
					log("Caught an exception: " + rawResp + "___<_>___" + externalItemId);
					variantResponsesArray.put(new JSONObject().put("variantId", externalItemId).put("error", rawResp));
				}
			} catch (org.json.JSONException e) {
				logE(e);
			}
		} catch (Exception e) {
			log("RawResp: " + rawResp);
			logE(e);
		}
		variantFieldErrors = new org.json.JSONArray();
	}

	private void validateVariants( JSONObject variant, String template, String business, String itemGroup, String marca, int variantPosition, boolean selfAdded /*, java.util.Map<String, String[]> dataEANs */, String supplier, String[] typeMainBarCodeA ) throws ServiceUnavailableException {

		JSONObject attributes = null;
		JSONObject logisticData = null;
		JSONObject datosVenta = null;
		org.json.JSONArray cosos = null;
		java.util.Map<String, String> validCodes = null;
		String codeValue = null;
		String lookup = null;
		log("Checking variants...");

		attributes = variant.has("attributes") ? (JSONObject) variant.get("attributes") : null;
		logisticData = variant.has("logisticData") ? (JSONObject) variant.get("logisticData") : null;
		datosVenta = variant.has("datosVenta") ? (JSONObject) variant.get("datosVenta") : null;
		cosos = new org.json.JSONArray().put(variant);
		if (attributes != null) {
			cosos.put(attributes);
		}
		if (logisticData != null) {
			cosos.put(logisticData);
		}
		if (datosVenta != null) {
			cosos.put(datosVenta);
		}
		if (cosos.length() > 0) {
		}

		String holder = null;

		for (int j = 0; j < cosos.length(); j++) {
			if (!JSONObject.NULL.equals(cosos.get(j)) && cosos.get(j) != null) {
				for (String name : JSONObject.getNames(cosos.getJSONObject(j))) {
					holder = String.valueOf(cosos.getJSONObject(j).get(name));
					if("".equals(holder)) {
						continue;
					}
					String drr = dr.getCharacteristicData(new org.json.JSONArray().put(name));
					lookup = drr != null ? new org.json.JSONObject(drr).getJSONArray("items").getJSONObject(0).getString("lookup") : null; // characteristicsThatAreLookups.get(name);
					if (lookup == null || "".equals(lookup)) {
					} else {
						if("Suburbia".equals(business) && "TamanoUnico".equals(name)) {
							String tamanoUnico = (String) cosos.getJSONObject(j).get(name);
							if(tamanoUnico != null && !"".equals(tamanoUnico)) {
								log("Tamaño único: " + tamanoUnico + ", gonna query itemgroup: " + itemGroup);
								String elCampo = queryDictionary(itemGroup, "TallaUnicavsTallaERP");
								log("El campo: " + elCampo + ", checado en: " + business + ", itemGroup: " + itemGroup);
								if(elCampo != null && !"".equals(elCampo)) {
									String lalookup = getLookupFromChar(elCampo);
									log("La lookup: " + elCampo);
									if(lalookup != null && !"".equals(lalookup)) {
										java.util.ArrayList<String> validValues = getValidValues(itemGroup, lalookup, "Suburbia".equals(business) ? "_s4h" : "" );
										log("Los valores: " + validValues);
										if(validValues != null) {
											if(!validValues.contains(tamanoUnico)) {
												org.json.JSONArray frutsis = new org.json.JSONArray();
												for(String val : validValues) {
													frutsis.put(val);
												}
												log(new org.json.JSONObject().put("QualityDimension", "Validity").put("message", "El valor del tamaño único seleccionado no pertenece a los valores acotados para el negocio " + business + ", en el grupo de artículos: " + itemGroup + ", favor de ver los valores válidos correspondientes en la llave \"valoresValidos\".").put("valoresValidos", frutsis).put("fields", new org.json.JSONArray().put("TamanoUnico")).toString());
											}else {
												log("Sí es válida la talla para la desa.");
											}
										}else {
											// No hay registro de acotación en la lista de valores de Grupo de Artículos para el campo encontrado.
											log("No hay registro de acotación en la lista de valores de Grupo de Artículos para el campo encontrado.");
										}
									}else {
										// El campo no tiene lookup...
										log("El campo no tiene lookup en P360");
									}
								}else {
									// No hay un atributo de talla registrado para el grupo de artículos.
									log("No hay un atributo de talla registrado para el grupo de artículos.");
								}
							}else {
								log("Para marketplace no se vale...");
							}

						}
						if("TamanoUnico".equals(name)) {
							String tamanoUnico = (String) cosos.getJSONObject(j).get(name);
							String tallaNormalizada = queryDictionary(marca + "<::>" + tamanoUnico, "TallaNormalizada"); // getTallaNormalizada
							log("Calculando talla normalizada: " + marca + "<::>" + tamanoUnico + " = " + tallaNormalizada);
							if(tallaNormalizada != null && !"".equals(tallaNormalizada)) {
								variant.put("TamanoUnicoSTD", tallaNormalizada);
							}
						}
						validCodes = procedeACargarValoresValidos(template, name);
						if (validCodes.isEmpty()) {
							validCodes = procedeACargarValoresLookup(lookup);
						} else {
						}
						if (!validCodes.isEmpty()) {
							codeValue = validCodes.get(cosos.getJSONObject(j).get(name));
							if (codeValue != null) {
								log("Within checking variants, name: " + name + ", business: " + business);
								if("Suburbia".equals(business) && "ColoursLiverpoolAtt".equals(name)) {
									String color = (String) cosos.getJSONObject(j).get(name);
									String colorCammelCase = queryColor(color, "ExtensionDeMetadatos_RelacionColoresLiverpoolSuburbia");
									if(colorCammelCase != null) {
										String colorId = queryLkpBack(colorCammelCase, "SB_COLORESLOV");
										if(colorId != null) {
											variant.put("SB_COLORES", colorCammelCase);
										}
										log("Color LVP: " + color + ", cammelCase: " + colorCammelCase + ", colorId: " + colorId);
									}else {
										log("Problema con los colores, no hay cammel case de: " + color);
									}
								}
							} else {
//								variantFieldErrors.put(
//										new JSONObject()
//										.put("values", new org.json.JSONArray().put( cosos.getJSONObject(j).get(name) ))
//										.put("message",
//												"Problem identifying current value within valid lookup value list")
//										.put("characteristic", name)
//										.put("fields", new org.json.JSONArray().put(name))
//										);
							}
						} else {
//							variantFieldErrors.put(
//									new JSONObject()
//									.put("values", new org.json.JSONArray().put( cosos.getJSONObject(j).get(name) ))
//									.put("message", "Problem identifying current lookup values")
//									.put("fields", new org.json.JSONArray().put(name))
//									.put("characteristic", name));
						}
					}
				}
			}
		}
		checkVariantMainBarCode(variant, business, supplier, variantPosition + 1, typeMainBarCodeA);
		if(variantFieldErrors.length() > 0) {
			org.json.JSONObject variantResponse = variantResponsesArray.getJSONObject(variantPosition);
			variantResponse.put("fieldProblems", variantFieldErrors);
			variantFieldErrors = new org.json.JSONArray();
		}
	}

//	private java.util.Map<String, String> getCharacteristicsThatAreLookups() {
//		java.util.Map<String, String> set = new java.util.TreeMap<>();
//		String delim = "\"";
//		String sep = ";";
//		String esc = "\\";
//		Yep y = new Yep();
//		String line = null;
//		String[] pieces = null;
//		try (java.io.BufferedReader br = new java.io.BufferedReader(
//				new java.io.InputStreamReader(new java.io.FileInputStream(
//						java.nio.file.Paths.get(baseCacheDirectory, "characteristic_and_lookups").toString())))) {
//			br.readLine();
//			while ((line = br.readLine()) != null) {
//				pieces = y.parseLine(line, delim, sep, esc);
//				set.put(pieces[0], pieces[1]);
//			}
//		} catch (IOException e) {
//			logE(e);
//		}
//		return set;
//	}

	private java.util.Map<String, java.util.Map<String, String>> globalLookupValues = new java.util.TreeMap<>();
	private java.util.Map<String, java.util.Map<String, String>> specificLookupValues = new java.util.TreeMap<>();

	private java.util.Map<String, String> getLkpValues(String lkp) throws ServiceUnavailableException{
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		RESTWorkshop rw  = new RESTWorkshop();
		rw.setBaseUrl(baseUrl);
		rw.addHeader("Authorization", this.workshop.getRc().getHeader().get("Authorization"));
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		rw.putParameter("lookup", "'" + lkp + "'");
		rw.putParameter("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		rw.putParameter("pageSize", "1200");
		do {
			rw.putParameter("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/LookupValue/byLookup");
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i = 0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					data.put(values.getString(0), values.getString(1));
				}
				currentIndex += response.getInt("pageSize");
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		
		return data;
	}

	private java.util.Map<String, String> procedeACargarValoresLookup(String lookup) throws ServiceUnavailableException {

//		java.util.Map<String, String> validValues = globalLookupValues.get(lookup);
//		if (validValues != null) {
//			return validValues;
//		}
		java.util.Map<String, String> validValues = new java.util.TreeMap<>(); // getLkpValues(lookup);
//		globalLookupValues.put(lookup, validValues);
		Yep yep = new Yep();
		final String delim = "\"";
		final String sep = ";";
		final String esc = "\\";
		String line = null;
		try (java.io.BufferedReader br = new java.io.BufferedReader(
				new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths
						.get(baseCacheDirectory, "global_lookups", lookup.replaceAll("/", "<::>")).toString())))) {
			String[] pieces = null;
			while ((line = br.readLine()) != null) {
				pieces = yep.parseLine(line, delim, sep, esc);
				validValues.put(pieces[1], pieces[0]);
			}
//			globalLookupValues.put(lookup, validValues);
		} catch (IOException | ArrayIndexOutOfBoundsException | IllegalStateException e) {
			logE(e);
			log(line);
			log("(rep) Going to correct a value staged for lookup " + lookup);
//			log("(Lasis) Going to correct a value staged for lookup " + lookup);

			String url = null;
			String rawResponse = null;
			org.json.JSONObject response = null;
			org.json.JSONArray rows = null;
			org.json.JSONArray values = null;
			int currentIndex = 0;
			int totalSize = 0;
			String[] pieces = new String[2];
			RESTWorkshop w = new RESTWorkshop();
			validValues = new java.util.TreeMap<>();
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths
					.get(baseCacheDirectory, "global_lookups", lookup.replaceAll("/", "<::>")).toString())))){
				do {
					url = baseUrl + "/list/LookupValue/bySearch?lookup=" + java.net.URLEncoder.encode("'" + lookup + "'", "UTF-8") + "&fields=LookupValue.Code,LookupValueLang.Name(es)&metaData=true&pageSize=600&startIndex=" + currentIndex;
					rawResponse = rc.getRequest("GET", url, null);
					response = new org.json.JSONObject(rawResponse);
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						currentIndex++;
						values = rows.getJSONObject(i).getJSONArray("values");
						for(int j=0; j<pieces.length; j++) {
							pieces[j] = values.getString(j);
						}
						validValues.put(pieces[1], pieces[0]);
						pw.println( w.serializeChunk(pieces, delim, sep, esc));
						if(currentIndex % 100 == 0) {
							log(".");
							if(currentIndex % 1000 == 0) {
								log("\n" + currentIndex);
							}
						}
					}
				}while(currentIndex < totalSize);
				currentIndex = 0;
				log("Loaded: " + validValues.size() + " for " + lookup);
				globalLookupValues.put(lookup, validValues);
			}catch(org.json.JSONException ex) {
				log(rawResponse);
				logE(ex);
			}catch (UnsupportedEncodingException e1) {
				logE(e1);
			} catch (IOException e1) {
				logE(e1);
			}

//			log("Malformed value: " + line + ". characteristic: " + lookup + ", file: " + baseCacheDirectory
//					+ java.io.File.separator + "global" + java.io.File.separator + lookup);
//
//			throw new IllegalStateException("Malformed value: " + line + ". characteristic: " + lookup + ", file: "
//					+ baseCacheDirectory + java.io.File.separator + "global" + java.io.File.separator + lookup);
		}
		return validValues;
	}

	private java.util.Map<String, String> procedeACargarValoresValidos(String templateId, String characteristic) {
		java.util.Map<String, String> validValues = specificLookupValues.get(templateId + "<::>" + characteristic);
		if (validValues != null) {
			return validValues;
		}
		Yep yep = new Yep();
		final String delim = "\"";
		final String sep = ";";
		final String esc = "\\";
		if(templateCharacteristicFilters.isEmpty()) {
			try(java.io.BufferedReader br = new java.io.BufferedReader(
					new java.io.InputStreamReader(
							new java.io.FileInputStream(java.nio.file.Paths.get(baseCacheDirectory, "template_characteristic_lookup_filter").toString())))){
				String line = br.readLine();
				String[] pieces = null;
				String[] miniSplit = null;
				while((line = br.readLine()) != null) {
					pieces = yep.parseLine(line, delim, sep, esc);
					miniSplit = pieces[1].split("_");
					if(miniSplit.length < 2) {
						log("PICES: " + line);
					}else {
						templateCharacteristicFilters.put(miniSplit[0] + "<::>" + miniSplit[1], pieces[2] + "<::>" + pieces[3]);
					}
				}
			}catch(java.io.IOException e) {
				logE(e);
			}
		}
		String cachedFilter = templateCharacteristicFilters.get(templateId + "<::>" + characteristic);
		String[] pedazos = null;
		String[] ei = null;
		validValues = new java.util.TreeMap<>();
		java.util.Set<String> codes = new java.util.TreeSet<>();
		if(cachedFilter != null) {
			StringBuilder sb = new StringBuilder();
			ei = cachedFilter.split("<::>");
			pedazos = ei[1].split(",");
			for(int i=0; i<pedazos.length; i++) {
				sb.append(i == 0 ? "" : ",");
				sb.append("\"");
				sb.append(pedazos[i].replaceAll("\"", "\\\\\""));
				sb.append("\"");
				codes.add(pedazos[i]);
			}
			String line = null;
			try (java.io.BufferedReader br = new java.io.BufferedReader(
					new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths
							.get(baseCacheDirectory, "global_lookups", ei[0].replaceAll("/", "<::>")).toString())))) {
				String[] pieces = null;
				while ((line = br.readLine()) != null) {
					pieces = yep.parseLine(line, delim, sep, esc);
					if(codes.contains(pieces[0])) {
						validValues.put(pieces[1], pieces[0]);
					}
				}
//				log("Encountered: " +
//						 validValues);
				globalLookupValues.put(ei[0], validValues);
			} catch (IOException | ArrayIndexOutOfBoundsException | IllegalStateException e) {
				
			}
			/*
			try {
				String url = null;
				String rawResponse = null;
				org.json.JSONObject response = null;
				org.json.JSONArray rows = null;
				org.json.JSONArray values = null;
				int currentIndex = 0;
				int totalSize = 0;
				do {
					url = baseUrl + "/list/LookupValue/bySearch"
							+ "?lookup=" + java.net.URLEncoder.encode("'" + ei[0] + "'", "UTF-8")
							+ "&query=" + java.net.URLEncoder.encode("LookupValue.IsActive = true and LookupValue.Code in (" + sb.toString() + ")", "UTF-8")
							+ "&fields=LookupValue.Code,LookupValueLang.Name(es)"
							+ "&metaData=true"
							+ "&pageSize=900"
							+ "&startIndex=" + currentIndex;
					log("Querying: " + url);
					rawResponse = rc.getRequest("GET", url, null);
					log("--->" + rawResponse);
					response = new org.json.JSONObject(rawResponse);
					totalSize = response.getInt("totalSize");
					log("TZ: " + totalSize);
					rows = response.getJSONArray("rows");
					for(int i=0; i<rows.length(); i++) {
						currentIndex++;
						values = rows.getJSONObject(i).getJSONArray("values");
						label = values.getString(1);
						code = validValues.get(label);
						if(code != null) {
							if(code.length() < values.getString(0).length()) {
								validValues.put(label, code);
							}
						} else {
							validValues.put(values.getString(1), values.getString(0));
						}
					}
				}while(currentIndex < totalSize);
				currentIndex = 0;
			}catch(java.io.IOException | KeyManagementException | NoSuchAlgorithmException | URISyntaxException e) {
				logE(e);
			}
				*/
		}else {
//			log("No filter found for: " + templateId + "<::>" + characteristic);
		}
		return validValues;
		/*
		String line = null;
		try (java.io.BufferedReader br = new java.io.BufferedReader(
				new java.io.InputStreamReader(new java.io.FileInputStream(
						java.nio.file.Paths.get(baseCacheDirectory, "global", templateId + "_" + characteristic).toString())))) {
			String[] pieces = null;
			while ((line = br.readLine()) != null) {
				pieces = yep.parseLine(line, delim, sep, esc);
				validValues.put(pieces[1], pieces[0]);
			}
			specificLookupValues.put(templateId + "<::>" + characteristic, validValues);
		} catch (java.io.IOException | ArrayIndexOutOfBoundsException | IllegalStateException e) {

			log("Going to correct a value staged for template " + templateId + ", characteristic " + characteristic);

			String url = null;
			String rawResponse = null;
			org.json.JSONObject response = null;
			org.json.JSONArray rows = null;
			org.json.JSONArray values = null;
			int currentIndex = 0;
			int totalSize = 0;
			String[] pieces = new String[2];
			RESTWorkshop w = new RESTWorkshop();
			validValues = new java.util.TreeMap<>();
			String lookup = null;
			String elements = null;
			try {
				url = baseUrl + "/list/StandardizationValue/bySearch"
						+ "?dictionaryProxy=" + java.net.URLEncoder.encode("'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'", "UTF-8")
						+ "&query=" + java.net.URLEncoder.encode(
								  "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\" and "
								+ "StandardizationValue.StructureGroup->LookupValue.Code equals \"" + templateId + "\" and "
								+ "StandardizationValue.Characteristic->Characteristic.Identifier equals \"" + characteristic + "\" and "
								+ "StandardizationValue.CreationType equals CreateProposal and "
								+ "StandardizationValue.Property equals ListOfValuesFilter"
							, "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								  "StandardizationValue.Characteristic->Characteristic.Lookup->Lookup.Identifier,"
								+ "StandardizationValue.PropertyValue", "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=6"
						+ "&startIndex=" + currentIndex;
				rawResponse = rc.getRequest("GET", url, null);
			}catch(java.io.IOException | KeyManagementException | NoSuchAlgorithmException | URISyntaxException ex) {
				logE(ex);
			}
			try {
				response = new org.json.JSONObject(rawResponse);
				rows = response.getJSONArray("rows");

					if(rows.length() > 0) {
						values = rows.getJSONObject(0).getJSONArray("values");
						lookup = values.getString(0);
						elements = values.getString(1);
						String[] pcs = elements.split(",");
						StringBuilder sb = new StringBuilder();
						for(int i=0; i<pcs.length; i++) {
							sb.append(i == 0 ? "" : ",").append("\"").append(pcs[i].trim()).append("\"");
						}
						log("Found list: " + sb.toString() + ".");
						try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(baseCacheDirectory, "global", templateId + "_" + characteristic).toString())))){
							do {
							url = baseUrl + "/list/LookupValue/bySearch"
									+ "?lookup=" + java.net.URLEncoder.encode(lookup, "UTF-8")
									+ "&query=" + java.net.URLEncoder.encode("LookupValue.IsActive = true and LookupValue.Code in (" + sb.toString() + ")", "UTF-8")
									+ "&fields=LookupValue.Code,LookupValueLang.Name(es)"
									+ "&metaData=true"
									+ "&pageSize=900"
									+ "&startIndex=" + currentIndex;
							rawResponse = rc.getRequest("GET", url, null);
							response = new org.json.JSONObject(rawResponse);
							rows = response.getJSONArray("rows");
							for(int i=0; i<rows.length(); i++) {
								currentIndex++;
								values = rows.getJSONObject(i).getJSONArray("values");
								for(int j=0; j<pieces.length; j++) {
									pieces[j] = values.getString(j);
								}
								validValues.put(pieces[1], pieces[0]);
								pw.println( w.serializeChunk(pieces, delim, sep, esc));
								if(currentIndex % 100 == 0) {
									log(".");
									if(currentIndex % 1000 == 0) {
										log("\n" + currentIndex);
									}
								}
							}
						}while(currentIndex < totalSize);
						currentIndex = 0;

						}catch(org.json.JSONException ex) {
							log(rawResponse);
							logE(ex);
						}catch (UnsupportedEncodingException e1) {
							logE(e1);
						} catch (KeyManagementException e1) {
							logE(e1);
						} catch (NoSuchAlgorithmException e1) {
							logE(e1);
						} catch (URISyntaxException e1) {
							logE(e1);
						} catch (IOException e1) {
							logE(e1);
						}
					}
					log("Loaded: " + validValues.size() + " for " + lookup + " and template " + templateId + ", characteristic " + characteristic);
					specificLookupValues.put(templateId + "<::>" + characteristic, validValues);
			}catch(org.json.JSONException ex) {
				log("Perhaps filter not found... leave it. (" + rawResponse + ")");

			}
//			log("Malformed value: " + line + ". characteristic: " + characteristic + ", file: " + baseCacheDirectory
//					+ java.io.File.separator + "global" + java.io.File.separator + characteristic);
//			throw new IllegalStateException("Malformed value: " + ". characteristic: " + characteristic + ", file: "
//					+ baseCacheDirectory + java.io.File.separator + "global" + java.io.File.separator + characteristic);
		}
		return validValues;
		*/
	}

	private String[] createArticle(org.json.JSONObject product, java.util.Map<String, String> headers,
			String externalProductId, boolean isProduct, String objectId, String templateId, boolean sample) throws ServiceUnavailableException {
		if (externalProductId != null) {
			String url = baseUrl + "/list/";
			org.json.JSONObject request = new org.json.JSONObject()
					.put("columns",
							new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "Product2G.CurrentStatus"))
									.put(new org.json.JSONObject().put("identifier",
											"Product2GStructureMap.StructureGroup(PrimaryProductTaxonomy)"))
									.put(new org.json.JSONObject().put("identifier",
											"SimpleProduct2GCharacteristicValue.LookupValue(Business)")))
					.put("rows", new org.json.JSONArray().put(new org.json.JSONObject()
							.put("object", new org.json.JSONObject().put("id", "'" + externalProductId + "'@'MASTER'"))
							.put("values", new org.json.JSONArray()
									.put(product.has("userAction") && "Finished".equals(product.getString("userAction"))
											? "1001"
											: "10031")
									.put(new org.json.JSONObject().put("id",
											"'" + templateId + "'@'PrimaryProductTaxonomy'"))
									.put(new org.json.JSONObject().put("id",
											procedeACargarValoresLookup("BusinessQualified")
													.get(product.getString("Business"))))))

					);
			if (!isProduct) {
				url += "Article";
				request.getJSONArray("columns").put(new org.json.JSONObject().put("identifier",
						"ProductReference.ReferencedSupplierAid(" + objectId + ")"));
				request.getJSONArray("rows").getJSONObject(0).getJSONArray("values").put(objectId);
			} else {
				url += "Product2G";
			}
			try {
				if(sample) {
					return null;
				}else {
					String rawResponse = rc.getRequest("POST", url, request.toString());
					log("Got response from create article: " + rawResponse + "\n\rRequest: " + request);
					org.json.JSONObject response = new org.json.JSONObject(rawResponse);
					org.json.JSONArray objects = response.getJSONArray("objects");
					String internalId = objects.getJSONObject(0).getJSONObject("object").getString("id");
					return new String[] { internalId, externalProductId };
				}
			} catch ( IOException e ) {
				logE(e);
			}
		} else {
			String rawResp = null;
			org.json.JSONObject resp = null;
			JSONObject initR = null;
			try {
				initR = new JSONObject()
						.put("currentStatus", new JSONObject().put("_code",
								product.has("userAction") && "Finished".equals(product.getString("userAction")) ? "1001"
										: "10031"))
						.put("structureGroupMap", // {"structureGroupMap": [ { "_qualification": { "structureGroup": { "_externalId": "..." } } } ]}
								new org.json.JSONArray().put(new JSONObject().put("_qualification",
										new JSONObject().put("structureGroup", new JSONObject().put("_externalId",
												"'" + templateId + "'@'PrimaryProductTaxonomy'")))));
				if (objectId != null) {
					initR.put("higherLevelProduct", new JSONObject().put("_qualification",
							new JSONObject().put("referencedIdentifier", objectId)));
				}
				JSONObject charBody = new org.json.JSONObject()
						.put("_qualification",
								new JSONObject().put("characteristic", new JSONObject().put("_code", "Business")))
						.put("_recordLang",
								new org.json.JSONArray().put(new JSONObject().put("values", new org.json.JSONArray()
										.put(new org.json.JSONObject().put("_label", product.getString("Business"))))));
				org.json.JSONArray characteristicArray = new org.json.JSONArray();
				characteristicArray.put(charBody);
				if(characteristicArray.length() > 0) {
					initR.put("_characteristicRecords", characteristicArray);
				}
				if(sample) {
					rawResp = null;
				}else {
					rawResp = this.rc.getRequest("POST", (isProduct ? this.objectAPIProduct2GURL : this.objectAPIArticleURL) + "?includeLabels=true&includeIds=true",
							initR.toString());
					log("Intentando crear una simple propuesta usando: " + product + ", headers: " + headers + ", externalProductId: " + externalProductId + ", isProduct: " + isProduct + ", objectId: " + objectId + ", templateId: " + templateId + "\n\t\t" + initR);
					log("Intentando crear una simple propuesta usando (URL): " + (isProduct ? this.objectAPIProduct2GURL : this.objectAPIArticleURL) + "?includeLabels=true&includeIds=true");
					log("Obtuvimos: " + rawResp);
				}
				resp = rawResp == null ? null : new JSONObject(rawResp);
				String productId = resp == null ? null : resp.getJSONObject("_entityItem").getString("_internalId");
				String[] pair = resp == null ? null : new String[] { productId, resp.getString("_identifier") };
				return pair;
			} catch (Exception e) {
				log(initR != null ? initR.toString() : "NaN");
				log(rawResp);
				logE(e);
			}
		}
		return null;
	}

	private boolean inconsistentWithVariants(org.json.JSONArray variants, boolean productContainsProposalId) {
		if (!productContainsProposalId) {
			try {
				for (int i = 0; i < variants.length(); i++) {
					if (variants.getJSONObject(i).has("variantId")) {
						return true;
					}
				}
			} catch (org.json.JSONException e) {
			}
		}
		return false;
	}

	private String[] getDireccionSeccion(String groupOfArticle, String business) {
		String[] values = null;
		String rawResponse = null;
		org.json.JSONObject response = null;
		String section = null;
		String structure = "Liverpool".equals(business) ? "CommercialECC"
				: "Suburbia".equals(business) ? "CommercialS4H" : "";
		if(!"".equals(structure)) {
			try {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/StructureGroup/bySearch?structure="
						+ java.net.URLEncoder.encode(structure, "UTF-8")
						+ "&query=" + java.net.URLEncoder
								.encode("StructureGroup.Identifier wildcard \"" + groupOfArticle + "-L4%\"", "UTF-8")
						+ "&fields=StructureGroup.ParentIdentifier,StructureGroupLang.Name(es)", null);
				log("------->" + rawResponse);
				response = new org.json.JSONObject(rawResponse);
				log("#############" + response.getJSONArray("rows"));
				if (response.getJSONArray("rows") != null && !org.json.JSONObject.NULL.equals(response.getJSONArray("rows"))
						&& response.getJSONArray("rows").length() > 0) {
					section = response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0);
					if (!"".equals(section) && section != null) {
						log("Section: " + section);
						rawResponse = rc.getRequest("GET", baseUrl + "/list/StructureGroup/bySearch?structure="
								+ java.net.URLEncoder.encode("Liverpool".equals(business) ? "CommercialECC"
										: "Suburbia".equals(business) ? "CommercialS4H" : "", "UTF-8")
								+ "&query=" + java.net.URLEncoder
										.encode("StructureGroup.Identifier wildcard \"" + section + "\"", "UTF-8")
								+ "&fields=StructureGroup.ParentIdentifier,StructureGroupLang.Name(es)", null);
						response = new org.json.JSONObject(rawResponse);
						log("Response from Direccion: " + response);
						values = new String[] {
								response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0).replaceAll("-.+", "")
								,
								section.replaceAll("-.+", "")
							};
					}
				}else {
					log("Nel, según: " + response.getJSONArray("rows").length());
				}
			} catch (Exception e) {
				log("Error, got: " + rawResponse);
				logE(e);
			}
		}
		return values;
	}

	private void processModifiedFields(org.json.JSONObject product) {
		if(!product.has("proposalId")) {
			return;
		}
		org.json.JSONArray melemes = new org.json.JSONArray();
		org.json.JSONArray userRemarks = null;
		org.json.JSONObject userRemark;
		org.json.JSONObject modifiedFields = (org.json.JSONObject) product.remove("modifiedFields");
		if(modifiedFields != null || product.has("userRemarks")) {

			log("******************* PROCESSING rejections *******************");
			int max = 0;

			org.json.JSONObject p2g = getProduct2GObject(product.getString("proposalId"));
			String id = p2g.getJSONObject("_entityItem").getString("_internalId");
			org.json.JSONArray characteristicRecords = p2g.getJSONObject("_data").getJSONArray("_characteristicRecords");
			java.util.Map<String, java.util.LinkedList<org.json.JSONObject> > mep = new java.util.TreeMap<>();
			java.util.Map<String, org.json.JSONObject > mepsiman = new java.util.TreeMap<>();
			java.util.LinkedList<org.json.JSONObject> lst = null;
			org.json.JSONObject charRec = null;
			for(int i=0; i<characteristicRecords.length(); i++) {
				charRec = characteristicRecords.getJSONObject(i);
				trincaleLosLookups(charRec);
				lst = mep.get(charRec.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"));
				if(lst == null) {
					lst = new java.util.LinkedList<>();
					mep.put(charRec.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"), lst);
				}
				lst.addLast(charRec);
			}
			if(product.has("userRemarks")) {
				log("*************** PROCESSING USER REMARKS ***************");
				userRemarks = product.getJSONArray("userRemarks");
				lst = mep.get("Comentario");
				if(lst != null) {
					for(org.json.JSONObject rej : lst) {
						deactivateBoy(rej, "Comentario", melemes);
					}
					max = getMaxBoy(lst);
				}
				max++;
				for(int i=0; i<userRemarks.length(); i++) {
					userRemark = userRemarks.getJSONObject(i);
					addUserRemark(userRemark, max, melemes);
					log("User remark added.");
					max++;
				}
				max = 0;
			}else {
				log("~~~~~~~~~~~~~~~~~++ No User Remarks ++~~~~~~~~~~~~~~~~~");
			}
			if(modifiedFields == null) {
				if(melemes.length() > 0) {
					try {
						org.json.JSONObject data = new org.json.JSONObject().put("_characteristicRecords", melemes);
						String respMessage = rc.getRequest("PUT", baseUrl + "/object/Product2G/" + id + "?includeLabels=true", data .toString());
						log( "Upon product (" + product.getString("proposalId") + ") rejection information update: " + respMessage );
						if(respMessage.contains("not found in enumeration 'Enum.Product2GCharacteristics'. Either the code is not part of the enumeration or the user has no read permission")) {
							String charId = null;
							try {
								String message = new org.json.JSONObject(respMessage).getJSONObject("_protocol").getString("message");
								java.util.regex.Matcher m = java.util.regex.Pattern.compile("Code value '(.+)'").matcher(message);
								boolean found = false;
								if(found = m.find()) {
									charId = m.group(1);
								}
								log("On registering message (" + found + "): " + charId + " || " + message);
							}catch(org.json.JSONException e) {
								logE(e);
							}
							genericFieldErrors.put(new JSONObject().put("fields", new org.json.JSONArray().put("Característica de Rechazo"))
									.put("values", new org.json.JSONArray().put( "" ))
									.put("message",
											"Problema escribiendo en rechazo por atributo, favor de reportarlo incluyendo plantilla y número de propuesta." + (charId != null ? " Característica a revisar: " + charId : ""))
									.put("characteristic", "Rechazo por atributo")
									);
							try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/rejection_payloads", true), java.nio.charset.StandardCharsets.UTF_8))){
								pw.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new java.util.Date()) + "]: " + data.toString());
							}catch(java.io.IOException e) {
								e.printStackTrace();
							}
						}
					} catch (Exception e) {
						logE(e);
					}
				}else {
					log("No rejection to send.");
				}
				return;
			}
			log("e...nesis ___--" + modifiedFields + "--___");

			org.json.JSONObject basicData = (org.json.JSONObject) modifiedFields.remove("basicData");
			org.json.JSONObject attributes = (org.json.JSONObject) modifiedFields.remove("attributes");
			org.json.JSONObject logisticData = (org.json.JSONObject) modifiedFields.remove("logisticData");
			org.json.JSONObject datosVenta = (org.json.JSONObject) modifiedFields.remove("datosVenta");
			org.json.JSONObject multimedia = (org.json.JSONObject) modifiedFields.remove("multiMedia");

			java.util.Map<String, java.util.LinkedList<Long>> dictMulti = new java.util.TreeMap<>();
			java.util.LinkedList<org.json.JSONObject> multimediaElementlList = mep.get("OwnersManual");
			if(multimediaElementlList != null){
				dictMulti.putAll( processRejectionChildrenRecordId(multimediaElementlList) );
				for(org.json.JSONObject elm : multimediaElementlList) {
					chagama(elm, mep, mepsiman, "OwnersManual");
				}
			}
			multimediaElementlList = mep.get("ProductVideo");
			if(multimediaElementlList != null){
				dictMulti.putAll( processRejectionChildrenRecordId(multimediaElementlList) );
				for(org.json.JSONObject elm : multimediaElementlList) {
					log("##################_________________________ On Video: " + elm + "...");
					chagama(elm, mep, mepsiman, "ProductVideo");
				}
			}
			multimediaElementlList = mep.get("LiverpoolManual");
			if(multimediaElementlList != null){
				dictMulti.putAll( processRejectionChildrenRecordId(multimediaElementlList) );
				for(org.json.JSONObject elm : multimediaElementlList) {
					chagama(elm, mep, mepsiman, "LiverpoolManual");
				}
			}
			log("MmChagamM ___--" + mep.keySet() + "--___");
			if(multimedia != null) {
				for(String name : org.json.JSONObject.getNames(multimedia)) {
					for(String subNeim : org.json.JSONObject.getNames(multimedia.getJSONObject(name))) {
						log("-->" + subNeim);
						lst = mep.get(name + "_" + subNeim);
						if(lst != null) {
							for(org.json.JSONObject rej : lst) {
								deactivateBoy(rej, name, melemes);
							}
							max = getMaxBoy(lst);
							max++;
							addRejectionForMultimedia(
									multimedia.getJSONObject(name).getJSONArray(subNeim).getJSONObject(0)
									,
									mepsiman.get(name + "_" + subNeim).getJSONArray("_children")
									,
									name
									,
									dictMulti);
							melemes.put(mepsiman.get(name + "_" + subNeim));
						}else {
							log("Nel.." + name + "_" + subNeim + ".. \nMeps: "+ mep.keySet() + "\nMepsiman: " + mepsiman.keySet());
						}
					}
				}
			}else {
				log("No rejections on multimedia. \nmeps: "
						+ mep.keySet() + "\n" + mepsiman.keySet());
			}
			log("*** PROCESSING attribute rejections (Product2G) ***");
			org.json.JSONArray normalCharacteristics = new org.json.JSONArray();
			if(basicData != null) {
				normalCharacteristics.put(basicData);
			}
			if(attributes != null) {
				normalCharacteristics.put(attributes);
			}
			if(logisticData != null) {
				normalCharacteristics.put(logisticData);
			}
			if(datosVenta != null) {
				normalCharacteristics.put(datosVenta);
			}
			org.json.JSONObject elm = null;
			String theName = null;
			for(int j = 0; j<normalCharacteristics.length(); j++) {
				elm = normalCharacteristics.getJSONObject(j);
				for(String name : org.json.JSONObject.getNames(elm)) {
					userRemarks = elm.getJSONArray(name);
					userRemark = userRemarks.getJSONObject(0);
					theName = name + "_Rechazo";
					lst = mep.get(theName);
					if(lst != null) {
						for(org.json.JSONObject rej :lst) {
							deactivateBoy(rej, name, melemes);
						}
						max = getMaxBoy(lst);
					}else {
						max = -1;
					}
					max++;
					addComplexCharacteristicNamedValue(userRemark, max, 0, melemes, name);
				}
			}
			if(melemes.length() > 0) {
				try {
					org.json.JSONObject data = new org.json.JSONObject().put("_characteristicRecords", melemes);
					String respMessage = rc.getRequest("PUT", baseUrl + "/object/Product2G/" + id + "?includeLabels=true", data .toString());
					log( "Upon product (" + product.getString("proposalId") + ") rejection information update: " + respMessage );
					if(respMessage.contains("not found in enumeration 'Enum.Product2GCharacteristics'. Either the code is not part of the enumeration or the user has no read permission")) {
						String charId = null;
						try {
							String message = new org.json.JSONObject(respMessage).getJSONObject("_protocol").getString("message");
							java.util.regex.Matcher m = java.util.regex.Pattern.compile("Code value '(.+)'").matcher(message);
							boolean found = false;
							if(found = m.find()) {
								charId = m.group(1);
							}
							log("On registering message (" + found + "): " + charId + " || " + message);
						}catch(org.json.JSONException e) {
							logE(e);
						}
						genericFieldErrors.put(new JSONObject().put("fields", new org.json.JSONArray().put("Característica de Rechazo"))
								.put("values", new org.json.JSONArray().put( "" ))
								.put("message",
										"Problema escribiendo en rechazo por atributo, favor de reportarlo incluyendo plantilla y número de propuesta." + (charId != null ? "Característica a revisar: " + charId : ""))
								.put("characteristic", "Rechazo por atributo")
								);
						try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("../logs/rejection_payloads", true), java.nio.charset.StandardCharsets.UTF_8))){
							pw.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new java.util.Date()) + "]: " + data.toString());
						}catch(java.io.IOException e) {
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					logE(e);
				}
			}else {
				log("No rejection to send.");
			}
			org.json.JSONObject variantes = (org.json.JSONObject) modifiedFields.remove("variants");
			if(variantes != null) {
				log(" <:::> V A R I A N T E S <:::>");
				org.json.JSONObject variant = null;
				for(String variantId : org.json.JSONObject.getNames(variantes)) {
					variant = variantes.getJSONObject(variantId);
					log("--->" + variant);
					basicData = (org.json.JSONObject) variant.remove("basicData");
					attributes = (org.json.JSONObject) variant.remove("attributes");
					logisticData = (org.json.JSONObject) variant.remove("logisticData");
					datosVenta = (org.json.JSONObject) variant.remove("datosVenta");
					org.json.JSONObject photos = (org.json.JSONObject) variant.remove("photos");

					max = 0;

					org.json.JSONObject article = getArticleObject(variantId);
					log("querying detail for: " + variantId);
					id = article.getJSONObject("_entityItem").getString("_internalId");
					characteristicRecords = article.getJSONObject("_data").getJSONArray("_characteristicRecords");
					log("CharRecords: " + characteristicRecords.length());
					mep = new java.util.TreeMap<>();
					mepsiman = new java.util.TreeMap<>();
					lst = null;
					charRec = null;
					melemes = new org.json.JSONArray();
					for(int i=0; i<characteristicRecords.length(); i++) {
						charRec = characteristicRecords.getJSONObject(i);
						trincaleLosLookups(charRec);
						lst = mep.get(charRec.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"));
						if(lst == null) {
							lst = new java.util.LinkedList<>();
							mep.put(charRec.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"), lst);
						}
						lst.addLast(charRec);
					}
					java.util.Map<String, java.util.LinkedList<Long>> dict = new java.util.TreeMap<>();
					multimediaElementlList = mep.get("ProductImage");
					if(multimediaElementlList != null){
						for(org.json.JSONObject elm1 : multimediaElementlList) {
							chagama(elm1, mep, mepsiman, "ProductImage");
						}
					}
					multimediaElementlList = mep.get("ProductImageDetail");
					if(multimediaElementlList != null){
						dict.putAll( processRejectionChildrenRecordId(multimediaElementlList) );
						for(org.json.JSONObject elm1 : multimediaElementlList) {
							chagama(elm1, mep, mepsiman, "ProductImageDetail");
						}
					}
					multimediaElementlList = mep.get("ProductImageSmosh");
					if(multimediaElementlList != null){
						dict.putAll( processRejectionChildrenRecordId(multimediaElementlList) );
						for(org.json.JSONObject elm1 : multimediaElementlList) {
							chagama(elm1, mep, mepsiman, "ProductImageSmosh");
						}
					}
					multimediaElementlList = mep.get("Illustration");
					if(multimediaElementlList != null){
						dict.putAll( processRejectionChildrenRecordId(multimediaElementlList) );
						for(org.json.JSONObject elm1 : multimediaElementlList) {
							chagama(elm1, mep, mepsiman, "Illustration");
						}
					}
					if(photos != null) {
						for(String name : org.json.JSONObject.getNames(photos)) {
							log("Vamos a tomar los neims de: " + name);
							for(String subNeim : org.json.JSONObject.getNames(photos.getJSONObject(name))) {
								log("-->" + subNeim);
								lst = mep.get(name + "_" + subNeim);
								log(variantId + ", " + name + "_" + subNeim + ": " + lst);
								if(lst != null) {
									for(org.json.JSONObject rej :lst) {
										deactivateBoy(rej, name, melemes);
									}
									max = getMaxBoy(lst);
								}else {
									max = -1;
								}
								max++;
								log("Using max value: " + max + " for " + subNeim);
								log("Las fotos: " + photos);
								if(mepsiman.containsKey(name + "_" + subNeim)) {
									addRejectionForMultimedia(
										photos
											.getJSONObject(name)
											.getJSONArray(subNeim)
											.getJSONObject(0)
										,
										mepsiman
											.get(name + "_" + subNeim)
											.getJSONArray("_children")
										,
										name
										,
										dict);
//									addComplexCharacteristicNamedValue(
//											photos
//											.getJSONObject(name)
//											.getJSONArray(subNeim)
//											.getJSONObject(0),
//											max,
//											0,
//											mepsiman
//											.get(name + "_" + subNeim)
//											.getJSONArray("_children")
//											, name
//											,
//											mepsiman
//											.get(name + "_" + subNeim)
//											.getJSONObject("_qualification").getString("recordKey")
//											, true);
									melemes.put(mepsiman.get(name + "_" + subNeim));
								}
							}
						}
					}
//					log("*** Eléjele ***");
					normalCharacteristics = new org.json.JSONArray();
					if(basicData != null) {
						normalCharacteristics.put(basicData);
					}
					if(attributes != null) {
						normalCharacteristics.put(attributes);
					}
					if(logisticData != null) {
						normalCharacteristics.put(logisticData);
					}
					if(datosVenta != null) {
						normalCharacteristics.put(datosVenta);
					}
					if(variant.length() > 0) {
						normalCharacteristics.put(variant);
					}
					elm = null;
					theName = null;
					for(int j = 0; j<normalCharacteristics.length(); j++) {
						elm = normalCharacteristics.getJSONObject(j);
						for(String name : org.json.JSONObject.getNames(elm)) {
							try{
								userRemarks = elm.getJSONArray(name);
								userRemark = userRemarks.getJSONObject(0);
								theName = name + "_Rechazo";
								lst = mep.get(theName);
								if(lst != null) {
									for(org.json.JSONObject rej :lst) {
										deactivateBoy(rej, name, melemes);
									}
									max = getMaxBoy(lst);
								}else {
									max = -1;
								}
								max++;
	//							log("Adding complex characteristic: " + name + ", foil: " + max);
									addComplexCharacteristicNamedValue(userRemark, max, 0, melemes, name);
							}catch(Exception e) {
								log("CHAGAMMMMMMÁ");
								logE(e);
							}
	//						System.out.println("Done with: " + name);
						}
						max = -1;
					}
					if(melemes.length() > 0) {
//						log("Variant (" + variantId + "): " +  new org.json.JSONObject().put("_characteristicRecords", melemes) );
						try {
							String message = null;
							org.json.JSONObject request = null;
							message = "upon variant (" + variantId + ") rejection data update: " +  rc.getRequest("PUT", baseUrl + "/object/Article/" + id + "?includeLabels=true", (request = new org.json.JSONObject().put("_characteristicRecords", melemes)) .toString()) + " <::::> " + melemes;
							if(message.contains("Unexpected exception from Java Persistence API.")) {
								log("Analyze this, produced bad message x.x (" + variantId + ")\n" + request + "\n");
							}else {
//								log("------------------------------------------------" + message + "---------------------------------------");
							}
						} catch (Exception e) {
							logE(e);
						}
					}else {
						log("No melemes ma broda: " + melemes);
					}
				}
				log(" </:::> V A R I A N T E S <:::>");
			}
		}
	}

	private void quitalLosKeisAlChagama(org.json.JSONObject chagama) {
		if(chagama.has("_datatype") && chagama.getString("_datatype").equals("LOOKUP") && chagama.has("_recordLang")) {
			chagama.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).remove("_key");
		}
		org.json.JSONArray children = chagama.has("_children") ? chagama.getJSONArray("_children") : null;
		if(children != null) {
			for(int i=0; i<children.length(); i++) {
				quitalLosKeisAlChagama(children.getJSONObject(i));
			}
		}
	}

	private void chagama(
			org.json.JSONObject elm,
			java.util.Map<String, java.util.LinkedList<org.json.JSONObject>> meps,
			java.util.Map<String, org.json.JSONObject> pepsiman,
			String parentTag
	) {
		String chagamaBaseName = elm.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
		String chagamaName = null;
		if(elm.has("_children")) {
			org.json.JSONArray children = elm.getJSONArray("_children");
			org.json.JSONObject child = null;
			org.json.JSONObject qualification = null;
			org.json.JSONObject characteristic = null;
			org.json.JSONArray recordLang = null;
			org.json.JSONObject recordLangEntry = null;
			org.json.JSONArray values = null;
			String name = null;
			java.util.LinkedList<org.json.JSONObject> chagamitas = new java.util.LinkedList<>();
			for(int i=0; i<children.length(); i++) {
				child = children.getJSONObject(i);
				qualification = child.getJSONObject("_qualification");
				characteristic = qualification.getJSONObject("characteristic");
				name = characteristic.getString("_code");
				log("__Examining this: " + name);
				if( (chagamaBaseName + "_Name").equals(name) ) {
					recordLang = child.getJSONArray("_recordLang");
					recordLangEntry = recordLang.getJSONObject(0);
					values = recordLangEntry.getJSONArray("values");
					chagamaName = values.getString(0);
				}else if( (chagamaBaseName + "_Rejection").equals(name) ) {
					chagamitas.addLast(child);
					quitalLosKeisAlChagama(child);
				}
			}
			log("__Using chagamaNeim: " + chagamaName +  " (parent tag: " + parentTag + ")");
			meps.put(parentTag + "_" + chagamaName, chagamitas);
			pepsiman.put(parentTag + "_" + chagamaName, elm);
		}else {
			log("$$$$$$$$$$$$$$$$$$$$$$$$$$$$ There was a chagama without children: " + elm);
		}
	}

	private int getMaxBoy(java.util.LinkedList<org.json.JSONObject> lst) {
		String recordKey = null;
		String[] recordKeyParts = null;
		Integer firstPart = null;
		Integer secondPart = null;
		int max = -1;
		for(org.json.JSONObject ent : lst) {
			recordKey = ent.getJSONObject("_qualification").getString("recordKey");
			recordKeyParts = recordKey.split("\\.");
			firstPart = Integer.parseInt(recordKeyParts[0]);
			if(firstPart == 0) {
				secondPart = Integer.parseInt(recordKeyParts[1]);
				max = max > secondPart ? max : secondPart;
			}else {
				max = max > firstPart ? max : firstPart;
			}
		}
		return max;
	}

	private void trincaleLosLookups(org.json.JSONObject j) {
		j.remove("lookupValue");
		org.json.JSONArray children = j.has("_children") ? j.getJSONArray("_children") : null;
		if(children != null) {
			for(int i=0; i<children.length(); i++) {
				trincaleLosLookups(children.getJSONObject(i));
			}
		}
	}

	private void deactivateBoy(org.json.JSONObject boy, String baseName, org.json.JSONArray memejes) {
		boolean changed = false;
		if(boy.has("_children")) {
			org.json.JSONArray children = boy.getJSONArray("_children");
			org.json.JSONObject lindependent = null;
			for(int i=0; i<children.length(); i++) {
				if(("rem_" + baseName).equals(children.getJSONObject(i).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"))) {
					lindependent = getMeTheUnlocalized( children.getJSONObject(i).getJSONArray("_recordLang"));
					if(lindependent == null) {
						// PANIC
					}else {
						lindependent.getJSONArray("values").getJSONObject(0).remove("_key");
						if(!"CS02".equals(lindependent.getJSONArray("values").getJSONObject(0).getString("_code"))) {
							lindependent.getJSONArray("values").getJSONObject(0).put("_code", "CS02");
							changed = true;
						}
					}
				}
			}
		}
		if(changed) {
//			log("Adding to the array for update... " + boy.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code") + " - " + boy.getJSONObject("_qualification").getString("recordKey"));
			memejes.put(boy);
		}
	}

	private org.json.JSONObject getMeTheUnlocalized(org.json.JSONArray recordLang){
		for(int i=0; i<recordLang.length(); i++) {
			if("zxx".equals(recordLang.getJSONObject(i).getJSONObject("_qualification").getJSONObject("language").getString("_code"))) {
				return recordLang.getJSONObject(i);
			}
		}
		return null;
	}

	/******
	 *
	 * Look at me ese
	 *
	 * ***************/
	private void addUserRemark(org.json.JSONObject userRemark, int i, org.json.JSONArray characteristicArray) {
		String mainCharacteristicIdentifier = "Comentario";
		org.json.JSONArray children = new org.json.JSONArray();
		String recordKey = i == 0 ? "0000.0000.RK" : "0000." + ( i < 10 ? "000" + i : i < 100 ? "00" + i : i < 1000 ? "0" + i : i ) + ".RK";
		children.put(new org.json.JSONObject()
				.put("_qualification",
						new org.json.JSONObject()
								.put("recordKey", recordKey)
								.put("characteristic",
										new org.json.JSONObject().put("_code",
												"msj_" + mainCharacteristicIdentifier)))
				.put("_recordLang",
						new org.json.JSONArray().put(new org.json.JSONObject().put("values",
								new org.json.JSONArray().put(userRemark.getString("comment"))))));
		children.put(new org.json.JSONObject()
				.put("_qualification",
						new org.json.JSONObject()
						.put("recordKey", recordKey)
								.put("characteristic",
										new org.json.JSONObject().put("_code",
												"rmum_" + mainCharacteristicIdentifier)))
				.put("_recordLang",
						new org.json.JSONArray().put(new org.json.JSONObject().put("values",
								new org.json.JSONArray().put(userRemark.getString("date"))))));
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
								.put("recordKey", recordKey)
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"rre_" + mainCharacteristicIdentifier)))
						.put("_recordLang",
								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
											new JSONObject().put("language",
												new JSONObject().put("_code", "zxx")))
										.put("_label", userRemark.getString("submittingRole")))))));
		if(userRemark.has("action")) {
			children.put(
					new org.json.JSONObject()
					.put("_qualification",
							new org.json.JSONObject()
							.put("recordKey", recordKey)
							.put("characteristic",
									new org.json.JSONObject().put("_code",
											"rma_" + mainCharacteristicIdentifier)))
					.put("_recordLang",
							new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
											new JSONObject()
											.put("_qualification",
													new JSONObject().put("language",
															new JSONObject().put("_code", "zxx")))
											.put("_code", userRemark.getString("action")))))));
		}
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
								.put("recordKey", recordKey)
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"rrd_" + mainCharacteristicIdentifier)))
						.put("_recordLang",
								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
											new JSONObject().put("language",
												new JSONObject().put("_code", "zxx")))
										.put("_label", userRemark.getString("targetRole")))))));
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
								.put("recordKey", recordKey)
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"rem_" + mainCharacteristicIdentifier)))
						.put("_recordLang",
								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
											new JSONObject().put("language",
												new JSONObject().put("_code", "zxx")))
										.put("_label", userRemark.getString("status")))))));
		characteristicArray.put(new org.json.JSONObject().put("_qualification",
				new JSONObject()
				.put("recordKey", recordKey)
						.put("characteristic", new JSONObject().put("_code", mainCharacteristicIdentifier)))
				.put("_recordLang",
						new org.json.JSONArray()
								.put(new JSONObject().put("values", new org.json.JSONArray())))
				.put("_children", children));
	}


	/******
	 *
	 * Look at me ese
	 *
	 * ***************/
	private void addComplexCharacteristicNamedValue(org.json.JSONObject userRemark, int i, int j, org.json.JSONArray characteristicArray, String mainCharacteristicIdentifier) {
		addComplexCharacteristicNamedValue(userRemark, i, j, characteristicArray, mainCharacteristicIdentifier, "0000.0000.RK", false);
	}

	/******
	 *
	 * Look at me ese
	 *
	 * ***************/
	private org.json.JSONObject addComplexCharacteristicNamedValue(org.json.JSONObject userRemark, int a, int b, org.json.JSONArray characteristicArray, String mainCharacteristicIdentifier, String parentRecordKey, boolean multimedia) {
		org.json.JSONArray children = new org.json.JSONArray();
		String recordKey = a == 0 ? "0000.0000.RK" : "0000." + ( a < 10 ? "000" + a : a < 100 ? "00" + a : a < 1000 ? "0" + a : a ) + ".RK";
		children.put(new org.json.JSONObject()
				.put("_qualification",
						new org.json.JSONObject()
								.put("recordKey", recordKey)
								.put("characteristic",
										new org.json.JSONObject().put("_code",
												(multimedia ? mainCharacteristicIdentifier + "_AdditionalComment" : "msj_" + mainCharacteristicIdentifier) )))
				.put("_recordLang",
						new org.json.JSONArray().put(new org.json.JSONObject().put("values",
								new org.json.JSONArray().put(userRemark.getString("comment"))))));
		children.put(new org.json.JSONObject()
				.put("_qualification",
						new org.json.JSONObject()
						.put("recordKey", recordKey)
								.put("characteristic",
										new org.json.JSONObject().put("_code",
												"rmum_" + mainCharacteristicIdentifier)))
				.put("_recordLang",
						new org.json.JSONArray().put(new org.json.JSONObject().put("values",
								new org.json.JSONArray().put(userRemark.getString("date"))))));
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
								.put("recordKey", recordKey)
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"rre_" + mainCharacteristicIdentifier)))
						.put("_recordLang",
								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
											new JSONObject().put("language",
												new JSONObject().put("_code", "zxx")))
										.put("_label", userRemark.getString("submittingRole")))))));
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
								.put("recordKey", recordKey)
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"rrd_" + mainCharacteristicIdentifier)))
						.put("_recordLang",
								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
											new JSONObject().put("language",
												new JSONObject().put("_code", "zxx")))
										.put("_label", userRemark.getString("targetRole")))))));
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
								.put("recordKey", recordKey)
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"rem_" + mainCharacteristicIdentifier)))
						.put("_recordLang",
								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
											new JSONObject().put("language",
												new JSONObject().put("_code", "zxx")))
										.put("_label", userRemark.getString("status")))))));
		if(userRemark.has("action")) {
			children.put(
				new org.json.JSONObject()
				.put("_qualification",
						new org.json.JSONObject()
						.put("recordKey", recordKey)
						.put("characteristic",
								new org.json.JSONObject().put("_code",
										"rma_" + mainCharacteristicIdentifier)))
				.put("_recordLang",
						new org.json.JSONArray().put(new org.json.JSONObject().put("values",
								new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
												new JSONObject().put("language",
														new JSONObject().put("_code", "zxx")))
										.put("_code", userRemark.getString("action")))))));
		}
//		log("Placing new: " + recordKey);
		org.json.JSONObject nw = null;
		characteristicArray.put(nw = new org.json.JSONObject().put("_qualification",
				new JSONObject()
				.put("recordKey", recordKey)
//				.put("parentRecordKey", parentRecordKey)
						.put("characteristic", new JSONObject().put("_code", mainCharacteristicIdentifier + (multimedia ? "_Rejection" : "_Rechazo"))))
				.put("_recordLang",
						new org.json.JSONArray()
								.put(new JSONObject().put("values", new org.json.JSONArray())))
				.put("_children", children));
		if(multimedia) {
			log("BUILT a rejection for an image or multimedia: " + nw);
		}
		return nw;
	}

	private org.json.JSONObject addRejectionForMultimedia(org.json.JSONObject userRemark, org.json.JSONArray characteristicArray, String mainCharacteristicIdentifier, java.util.Map<String, java.util.LinkedList<Long>> dict) {
		java.util.LinkedList<Long> yo = null;
		org.json.JSONArray children = new org.json.JSONArray();
		String recordKey = null;
		Long currentUpperKey = null;
		String csi = null;

		csi = mainCharacteristicIdentifier + "_AdditionalComment";
		yo = dict.get(csi);
		log("Getting list for: " + csi + ", got: " + yo);
		currentUpperKey = yo == null ? -1 : yo.getFirst();
		currentUpperKey++;
		recordKey = rebuildRecordKey(currentUpperKey);
		if(yo == null) {
			yo = new java.util.LinkedList<>();
			dict.put(csi, yo);
		}
		yo.addFirst(currentUpperKey);
		children.put(new org.json.JSONObject()
				.put("_qualification",
						new org.json.JSONObject()
								.put("recordKey", recordKey)
								.put("characteristic",
										new org.json.JSONObject().put("_code",
												(mainCharacteristicIdentifier + "_AdditionalComment") )))
				.put("_recordLang",
						new org.json.JSONArray().put(new org.json.JSONObject().put("values",
								new org.json.JSONArray().put(userRemark.getString("comment"))))));

		csi = "rmum_" + mainCharacteristicIdentifier;
		yo = dict.get(csi);
		log("Getting list for: " + csi + ", got: " + yo);
		currentUpperKey = yo == null ? -1 : yo.getFirst();
		currentUpperKey++;
		recordKey = rebuildRecordKey(currentUpperKey);
		if(yo == null) {
			yo = new java.util.LinkedList<>();
			dict.put(csi, yo);
		}
		yo.addFirst(currentUpperKey);
		children.put(new org.json.JSONObject()
				.put("_qualification",
						new org.json.JSONObject()
						.put("recordKey", recordKey)
								.put("characteristic",
										new org.json.JSONObject().put("_code",
												"rmum_" + mainCharacteristicIdentifier)))
				.put("_recordLang",
						new org.json.JSONArray().put(new org.json.JSONObject().put("values",
								new org.json.JSONArray().put(userRemark.getString("date"))))));

		csi = "rre_" + mainCharacteristicIdentifier;
		yo = dict.get(csi);
		log("Getting list for: " + csi + ", got: " + yo);
		currentUpperKey = yo == null ? -1 : yo.getFirst();
		currentUpperKey++;
		recordKey = rebuildRecordKey(currentUpperKey);
		if(yo == null) {
			yo = new java.util.LinkedList<>();
			dict.put(csi, yo);
		}
		yo.addFirst(currentUpperKey);
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
								.put("recordKey", recordKey)
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"rre_" + mainCharacteristicIdentifier)))
						.put("_recordLang",
								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
											new JSONObject().put("language",
												new JSONObject().put("_code", "zxx")))
										.put("_label", userRemark.getString("submittingRole")))))));

		csi = "rrd_" + mainCharacteristicIdentifier;
		yo = dict.get(csi);
		log("Getting list for: " + csi + ", got: " + yo);
		currentUpperKey = yo == null ? -1 : yo.getFirst();
		currentUpperKey++;
		recordKey = rebuildRecordKey(currentUpperKey);
		if(yo == null) {
			yo = new java.util.LinkedList<>();
			dict.put(csi, yo);
		}
		yo.addFirst(currentUpperKey);
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
								.put("recordKey", recordKey)
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"rrd_" + mainCharacteristicIdentifier)))
						.put("_recordLang",
								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
											new JSONObject().put("language",
												new JSONObject().put("_code", "zxx")))
										.put("_label", userRemark.getString("targetRole")))))));

		csi = "rem_" + mainCharacteristicIdentifier;
		yo = dict.get(csi);
		log("Getting list for: " + csi + ", got: " + yo);
		currentUpperKey = yo == null ? -1 : yo.getFirst();
		currentUpperKey++;
		recordKey = rebuildRecordKey(currentUpperKey);
		if(yo == null) {
			yo = new java.util.LinkedList<>();
			dict.put(csi, yo);
		}
		yo.addFirst(currentUpperKey);
		children.put(
				new org.json.JSONObject()
						.put("_qualification",
								new org.json.JSONObject()
								.put("recordKey", recordKey)
										.put("characteristic",
												new org.json.JSONObject().put("_code",
														"rem_" + mainCharacteristicIdentifier)))
						.put("_recordLang",
								new org.json.JSONArray().put(new org.json.JSONObject().put("values",
									new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
											new JSONObject().put("language",
												new JSONObject().put("_code", "zxx")))
										.put("_label", userRemark.getString("status")))))));

		csi = "rma_" + mainCharacteristicIdentifier;
		yo = dict.get(csi);
		log("Getting list for: " + csi + ", got: " + yo);
		currentUpperKey = yo == null ? -1 : yo.getFirst();
		currentUpperKey++;
		recordKey = rebuildRecordKey(currentUpperKey);
		if(yo == null) {
			yo = new java.util.LinkedList<>();
			dict.put(csi, yo);
		}
		yo.addFirst(currentUpperKey);
		if(userRemark.has("action")) {
			children.put(
				new org.json.JSONObject()
				.put("_qualification",
						new org.json.JSONObject()
						.put("recordKey", recordKey)
						.put("characteristic",
								new org.json.JSONObject().put("_code",
										"rma_" + mainCharacteristicIdentifier)))
				.put("_recordLang",
						new org.json.JSONArray().put(new org.json.JSONObject().put("values",
								new org.json.JSONArray().put(
										new JSONObject()
										.put("_qualification",
												new JSONObject().put("language",
														new JSONObject().put("_code", "zxx")))
										.put("_code", userRemark.getString("action")))))));
		}
		org.json.JSONObject nw = null;
		characteristicArray.put(nw = new org.json.JSONObject().put("_qualification",
				new JSONObject()
				.put("recordKey", recordKey)
						.put("characteristic", new JSONObject().put("_code", mainCharacteristicIdentifier + "_Rejection" )))
				.put("_recordLang",
						new org.json.JSONArray()
								.put(new JSONObject().put("values", new org.json.JSONArray())))
				.put("_children", children));
		log("BUILT a rejection for an image or multimedia: " + nw);
		return nw;
	}

	private java.util.Map<String, java.util.LinkedList<Long>> processRejectionChildrenRecordId(java.util.LinkedList<org.json.JSONObject> multimedia) {
		java.util.Map<String, java.util.LinkedList<Long>> pep = new java.util.TreeMap<>();
		String charId = null;
		org.json.JSONArray children = null;
		for(org.json.JSONObject multi : multimedia) {
			charId = multi.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
			children = multi.has("_children") ? multi.getJSONArray("_children") : null;
			if(children != null) {
				for(int i=0; i<children.length(); i++) {
					charId = children.getJSONObject(i).getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
					log("** Processing: " + charId);
					if(charId.endsWith("_Rejection")) {
						extraccionDelosDesos(children.getJSONObject(i), pep);
					}
				}
			}
		}
		for(java.util.Map.Entry<String, java.util.LinkedList<Long>> entry : pep.entrySet()) {
			java.util.Collections.sort(entry.getValue());
		}
		return pep;
	}

	private void extraccionDelosDesos(org.json.JSONObject elemento, java.util.Map<String, java.util.LinkedList<Long>> pep) {
		if(elemento.has("_children")) {
			org.json.JSONArray children = elemento.getJSONArray("_children");
			org.json.JSONObject child = null;
			String charId = null;
			java.util.LinkedList<Long> recordKeys = null;
			for(int i=0; i<children.length(); i++) {
				child = children.getJSONObject(i);
				charId = child.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code");
				recordKeys = pep.get(charId);
				if(recordKeys == null) {
					recordKeys = new java.util.LinkedList<>();
					pep.put(charId, recordKeys);
				}
				recordKeys.addLast( parseRecordKey( child.getJSONObject("_qualification").getString("recordKey") ) );
			}
		}
	}

	private Long parseRecordKey(String recordKey) {
		if(!"".equals(recordKey)) {
			String[] pieces = recordKey.split("\\.");
			return Long.parseLong(pieces[0] + pieces[1]);
		}
		return null;
	}

	private String rebuildRecordKey(Long number) {
		String val = paddZeros(String.valueOf(number));
		return String.join(".", new String[] { val.substring(0,4), val.substring(4), "RK" });
	}

	private String paddZeros(String value) {
		StringBuilder sb = new StringBuilder();
		int l = 8 - value.length();
		for(int i=0; i<l; i++) {
			sb.append("0");
		}
		sb.append(value);
		return sb.toString();
	}


	private org.json.JSONObject getProduct2GObject(String proposalId){
		String rawResponse = null;
		org.json.JSONObject response = null;
		try {
			String url = null;
			rawResponse = rc.getRequest("GET", url = baseUrl + "/object/Product2G/'" + proposalId + "'@'MASTER'?entityFilter=Product2G,Product2GStructureGroupMap,Product2GCharacteristicValue&includeLabels=true&includeIds=true", null);
			log("querying: " + url);
			response = new org.json.JSONObject(rawResponse);
		} catch (Exception e) {
			log(rawResponse);
			logE(e);
		}
		return response;
	}

	private org.json.JSONObject getArticleObject(String proposalId){
		String rawResponse = null;
		org.json.JSONObject response = null;
		try {
			rawResponse = rc.getRequest("GET",
					baseUrl+ "/object/Article/'" + proposalId + "'@'MASTER'?entityFilter=ArticleCharacteristicValue&includeIds=true", null);
			response = new org.json.JSONObject(rawResponse);
		} catch (Exception e) {
			logE(e);
		}
		return response;
	}

	
//	private static final Logger LOGGER = Logger.getLogger(CreateProposal.class.getName());
//	private static java.io.PrintWriter pw;
	
    static {
//        try {
//            LOGGER.setUseParentHandlers(false);

//            FileHandler fileHandler = new FileHandler("../logs/java_active_process_proposal_create.log" /*, 15 * 1024 * 1024, 10 */, true);
//            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
//            fileHandler.setLevel(Level.ALL);
//
//            fileHandler.setFormatter(new Formatter() {
//                @Override
//                public String format(LogRecord record) {
//                    java.time.LocalDateTime dateTime =
//                        java.time.Instant.ofEpochMilli(record.getMillis())
//                            .atZone(java.time.ZoneId.systemDefault())
//                            .toLocalDateTime();
//
//                    String timestamp = dateTime.format(
//                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
//                    );
//
//                    return "[" + timestamp + "] [" + record.getLevel() + "] " + formatMessage(record) + System.lineSeparator();
//                }
//            });
//
//            LOGGER.addHandler(fileHandler);
//////            LOGGER.setLevel(Level.ALL);
//
//        } catch (IOException e) {
//            throw new RuntimeException("No se pudo inicializar el logger", e);
//        }
//    	try {
//			pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("../logs/java_active_process_proposal_create.log").toFile())));
//		} catch (FileNotFoundException e) {
//			e.printStackTrace();
//		}
    }

	private void log(String message) {
//		pw.println();
//		LOGGER.info("(" + myId + ") " + message);
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/java_active_process_proposal_create.log", true)))) {
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date()))
					+ "] (" + myId + ") " + message);
		} catch (java.io.IOException e) {
		}
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/java_active_process_proposal_create.log", true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}

}