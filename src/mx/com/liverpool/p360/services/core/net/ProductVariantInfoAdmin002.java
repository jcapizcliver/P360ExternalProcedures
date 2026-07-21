package mx.com.liverpool.p360.services.core.net;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.DBAccessDataStub.ELog;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ProductVariantInfoAdmin002 extends Thread {

	private static final RESTWrapper wrapper = new RESTWrapper();
	private static final RESTWorkshop rw = wrapper.getRw();
	
	private static final java.nio.file.Path skuProductNoPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.sku_to_productno_file"));
	private static final java.nio.file.Path skuSupplierAIDPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.sku_to_supplieraid_file"));
	private static final java.nio.file.Path supplierAIDProductNoPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.supplieraid_to_productno_file"));
	private static final java.nio.file.Path productNoAndVariantsPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.productno_to_variants_file"));
	private static final java.nio.file.Path productDataPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.product_data_file"));
	private static final java.nio.file.Path articleDataPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.article_data_file"));
	private static final java.nio.file.Path productDataExtraPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.product_data_extra_file"));
	private static final java.nio.file.Path articleDataExtraPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.article_data_extra_file"));
	private static final java.nio.file.Path globalMetaDataPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.global_meta_data_file"));
	private static final java.nio.file.Path templateCharacteristicMetaDataPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.template_characteristic_meta_data_file"));
	private static final java.nio.file.Path templateNamesPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.template_names_file"));
	private static final java.nio.file.Path characteristicDataPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.characteristic_names_file"));
	private static final java.nio.file.Path contenidoDeDiccionarioPath = java.nio.file.Paths.get(PropertiesManager.get("p360.contingency.contenido_de_diccionario_file"));
	
	private static final java.util.concurrent.ConcurrentHashMap<String, String> skuProductNo = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, String> skuSupplierAID = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, String> supplierAIDProductNo = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, String[]> productData = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, String[]> articleData = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, java.util.Set< String >> productNoAndVariants = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, String> eanSupplierAID = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, String> eanProductNo = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, String> brandModelSupplierAID = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, String> brandModelProductNo = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, String[]> productExtraData = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, String[]> articleExtraData = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>> globalMetaData = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>>> templateCharacteristicMetaData = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, String> templateNames = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, String[]> characteristicData = new java.util.concurrent.ConcurrentHashMap<>();
	private static final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String[]>> contenidoDeDiccionario = new java.util.concurrent.ConcurrentHashMap<>();
	
	private boolean running = true;
	private java.net.ServerSocket ss;
	private final java.util.LinkedList<Worker> workerWorkers = new java.util.LinkedList<>();
	
	public static void main(String[] args) {
		ProductVariantInfoAdmin002 admin = new ProductVariantInfoAdmin002();
		Runtime.getRuntime().addShutdownHook(admin);
		admin.startListener();
	}
	
	public ProductVariantInfoAdmin002() {
		if(java.nio.file.Files.exists( skuProductNoPath )) {
			
			SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> skuProductNo.put(arr[0], arr.length > 1 ? arr[1] : "") );
			fileParser.parse(skuProductNoPath);
			
			log("Loaded: " + skuProductNo.size() + " skuProductNos");
		}
		if(java.nio.file.Files.exists( skuSupplierAIDPath )) {
			
			SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> skuSupplierAID.put(arr[0], arr.length == 1 ? "" : arr[1] ) );
			fileParser.parse(skuSupplierAIDPath);
			
			log("Loaded: " + skuSupplierAID.size() + " skuSupplierAIDs");
		}
		if(java.nio.file.Files.exists( supplierAIDProductNoPath )) {
			
			SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> supplierAIDProductNo.put(arr[0], arr.length == 1 ? "" : arr[1]) );
			fileParser.parse(supplierAIDProductNoPath);
			log("Loaded: " + supplierAIDProductNo.size() + " supplierAIDProductNos");
		}
		if(java.nio.file.Files.exists( productDataPath )) {

			SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
				String[] apples = rw.parseLine(arr[1], "\"", ";", "\\");
				String[] arr2 = new String[16];
				int m = Integer.min(apples.length, arr2.length);
				for(int i=0; i<m; i++) {
					arr2[i] = apples[i];
				}
				for(int j=m; j<arr2.length; j++) {
					arr2[j] = "";
				}
				productData.put(arr[0], arr2 ); 
			} );
			fileParser.parse(productDataPath);
			
			log("Loaded: " + productData.size() + " productData");
		}
		if(java.nio.file.Files.exists( articleDataPath )) {
			
			SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> articleData.put(arr[0], rw.parseLine(arr[1], "\"", ";", "\\") ) );
			fileParser.parse(articleDataPath);
			
			log("Loaded: " + articleData.size() + " articleData");
		}
		if(java.nio.file.Files.exists( productNoAndVariantsPath )) {
			SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> productNoAndVariants.put(arr[0], new java.util.TreeSet<>( java.util.Arrays.asList( arr.length == 1 ? new String[] {} : rw.parseLine( arr[1] ,"\"", ";", "\\") ) ) ) );
			fileParser.parse(productNoAndVariantsPath);
			log("Loaded: " + productNoAndVariants.size() + " productNoAndVariants");
		}
		
		if(java.nio.file.Files.exists( productDataExtraPath )) {
			SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> productExtraData.put(arr[0], rw.parseLine( arr[1] ,"\"", ";", "\\") ) );
			fileParser.parse(productDataExtraPath);
			log("Loaded: " + productExtraData.size() + " productExtraData");
		}
		
		if(java.nio.file.Files.exists( articleDataExtraPath )) {
			SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> articleExtraData.put(arr[0], rw.parseLine( arr[1] ,"\"", ";", "\\") ) );
			fileParser.parse(articleDataExtraPath);
			log("Loaded: " + articleExtraData.size() + " articleExtraData");
		}
		
		if(java.nio.file.Files.exists( globalMetaDataPath )) {
			SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
				java.util.concurrent.ConcurrentHashMap<String, String> propertiesMap = globalMetaData.get(arr[0]);
				if(propertiesMap == null) {
					propertiesMap = new java.util.concurrent.ConcurrentHashMap<>();
					globalMetaData.put(arr[0], propertiesMap);
				}
				propertiesMap.put(arr[1], arr.length > 2 ? arr[2] : "");
			} );
			fileParser.parse(globalMetaDataPath);
			log("Loaded: " + globalMetaData.size() + " globalMetaData");
		}
		if(java.nio.file.Files.exists( templateCharacteristicMetaDataPath )) {
			SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
				java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>> characteristicMap = templateCharacteristicMetaData.get(arr[0]);
				if(characteristicMap == null) {
					characteristicMap = new java.util.concurrent.ConcurrentHashMap<>();
					templateCharacteristicMetaData.put(arr[0], characteristicMap);
				}
				java.util.concurrent.ConcurrentHashMap<String, String> properties = characteristicMap.get(arr[1]);
				if(properties == null) {
					properties = new java.util.concurrent.ConcurrentHashMap<>();
					characteristicMap.put(arr[1], properties);
				}
				properties.put(arr[2], arr.length > 3 ? arr[3] : "");
			} );
			fileParser.parse(templateCharacteristicMetaDataPath);
			log("Loaded: " + templateCharacteristicMetaData.size() + " templateCharacteristicMetaData");
		}
		if(java.nio.file.Files.exists( templateNamesPath )) {
			SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> templateNames.put(arr[0], arr.length == 1 ? "" : arr[1]) );
			fileParser.parse(templateNamesPath);
			log("Loaded: " + templateNames.size() + " templateNames");
		}
		if(java.nio.file.Files.exists( characteristicDataPath )) {
			SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> characteristicData.put(arr[0], rw.parseLine( arr[1], "\"", ";", "\\" ) ) );
			fileParser.parse(characteristicDataPath);
			log("Loaded: " + characteristicData.size() + " characteristics");
		}
		if(java.nio.file.Files.exists( contenidoDeDiccionarioPath )) {
			SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
				java.util.concurrent.ConcurrentHashMap<String, String[]> tuplas = contenidoDeDiccionario.get(arr[0]);
				if(tuplas == null) {
					tuplas = new java.util.concurrent.ConcurrentHashMap<>();
					contenidoDeDiccionario.put(arr[0], tuplas);
				}
				String[] data = tuplas.get(arr[1]);
				if(data == null) {
					data = new String[6];
					tuplas.put(arr[1], data);
				}
				data[0] = arr[2];
				data[1] = arr[3];
				data[2] = arr[4];
				data[3] = arr[5];
				data[4] = arr[6];
				data[5] = arr.length > 7 ? arr[7] : "";
			} );
			fileParser.parse(contenidoDeDiccionarioPath);
			log("Loaded: " + contenidoDeDiccionario.size() + " contenidoDeDiccionario");
		}
		for(java.util.Map.Entry<String, String[]> entry : productData.entrySet()) {
			if(entry.getValue() != null) {
				if(entry.getValue().length > 15) {
					if(!"".equals(entry.getValue()[14])) {
						eanProductNo.put(entry.getValue()[14], entry.getKey());
					}else if(!"".equals(entry.getValue()[15])) {
						eanProductNo.put(entry.getValue()[15], entry.getKey());
					}
				}
			}
		}
		for(java.util.Map.Entry<String, String[]> entry : articleData.entrySet()) {
			if(entry.getValue() != null) {
				if(entry.getValue().length > 6) {
					if(!"".equals(entry.getValue()[5])) {
						eanSupplierAID.put(entry.getValue()[5], entry.getKey());
					}else if(!"".equals(entry.getValue()[6])) {
						eanSupplierAID.put(entry.getValue()[6], entry.getKey());
					}
				}
			}
		}
		/*
		eanSupplierAID = new java.util.concurrent.ConcurrentHashMap<>();
		eanProductNo = new java.util.concurrent.ConcurrentHashMap<>();
		brandModelSupplierAID = new java.util.concurrent.ConcurrentHashMap<>();
		brandModelProductNo = new java.util.concurrent.ConcurrentHashMap<>();
		*/
	}
	
	@Override
	public void run() {
		keepData();
		setRunning(false);
	}
	
	private void keepData() {
		log("Keeping data...");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream( skuProductNoPath.toFile()) ))){
			skuProductNo.forEach((k,v)-> pw.println( rw.serializeChunk(new String[] {k, v}) ));
		}catch(java.io.IOException e) {
			logE(e);
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream( skuSupplierAIDPath.toFile()) ))){
			skuSupplierAID.forEach((k,v)-> pw.println( rw.serializeChunk(new String[] {k, v}) ));
		}catch(java.io.IOException e) {
			logE(e);
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream( supplierAIDProductNoPath.toFile()) ))){
			supplierAIDProductNo.forEach((k,v)-> pw.println( rw.serializeChunk(new String[] {k, v}) ));
		}catch(java.io.IOException e) {
			logE(e);
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream( productDataPath.toFile()) ))){
			productData.forEach((k,v)-> pw.println( rw.serializeChunk(new String[] {k, rw.serializeChunk( v, "\"", ";", "\\") }) ));
		}catch(java.io.IOException e) {
			logE(e);
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream( articleDataPath.toFile()) ))){
			articleData.forEach((k,v)-> pw.println( rw.serializeChunk(new String[] {k, rw.serializeChunk( v, "\"", ";", "\\") }) ));
		}catch(java.io.IOException e) {
			logE(e);
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream( productNoAndVariantsPath.toFile()) ))){
			productNoAndVariants.forEach((k,v)-> pw.println( rw.serializeChunk(new String[] {k, rw.serializeChunk( v.toArray(new String[] {}), "\"", ";", "\\") }) ));
		}catch(java.io.IOException e) {
			logE(e);
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream( productDataExtraPath.toFile()) ))){
			productExtraData.forEach((k,v)-> pw.println( rw.serializeChunk(new String[] {k, rw.serializeChunk( v, "\"", ";", "\\") }) ));
		}catch(java.io.IOException e) {
			logE(e);
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream( articleDataExtraPath.toFile()) ))){
			articleExtraData.forEach((k,v)-> pw.println( rw.serializeChunk(new String[] {k, rw.serializeChunk( v, "\"", ";", "\\") }) ));
		}catch(java.io.IOException e) {
			logE(e);
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream( globalMetaDataPath.toFile()) ))){
			globalMetaData.forEach((k,v)-> {
				v.forEach((k1,v1) -> {
					pw.println( rw.serializeChunk( new Object[] { k, k1, v1 } ) );
				});
			});
		}catch(java.io.IOException e) {
			logE(e);
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream( templateCharacteristicMetaDataPath.toFile()) ))){
			templateCharacteristicMetaData.forEach((k,v)-> {
				v.forEach((k1,v1) -> {
					v1.forEach((k2,v2) -> {
						pw.println( rw.serializeChunk( new Object[] { k, k1, k2, v2 } ) );
					});
				});
			});
		}catch(java.io.IOException e) {
			logE(e);
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream( templateNamesPath.toFile()) ))){
			templateNames.forEach((k,v)-> pw.println( rw.serializeChunk( new Object[] { k, v } ) ));
		}catch(java.io.IOException e) {
			logE(e);
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream( characteristicDataPath.toFile()) ))){
			characteristicData.forEach((k,v)-> pw.println( rw.serializeChunk( new Object[] { k, rw.serializeChunk(v, "\"", ";", "\\") } ) ));
		}catch(java.io.IOException e) {
			logE(e);
		}
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream( contenidoDeDiccionarioPath.toFile()) ))){
			contenidoDeDiccionario.forEach((k,v) -> {
				v.forEach((k1,v1)->{
					pw.println( rw.serializeChunk( new Object[] { k,  k1, v1[0], v1[1], v1[2], v1[3], v1[4], v1[5] } ) );
				});
			});
		}catch(java.io.IOException e) {
			logE(e);
		}
	}
	
	private class Worker implements Runnable {
		
		private final ProductVariantInfoAdmin002 host;
		private final java.util.concurrent.ArrayBlockingQueue<java.net.Socket> clientes;
		private DBAccessDataStub dastub = new DBAccessDataStub(new ELog() { @Override public void log(String message){ ProductVariantInfoAdmin002.this.log(message); } @Override public void logE(Exception e) { ProductVariantInfoAdmin002.this.logE(e); } });
		
		public Worker(
				 ProductVariantInfoAdmin002 host
				,java.util.concurrent.ArrayBlockingQueue<java.net.Socket> clientes
		) {
			this.host = host;
			this.clientes = clientes;
		}
		
		@Override
		public void run() {
			java.net.Socket socket = null;
			while(this.host.running) {
				try {
					socket = clientes.poll(10, java.util.concurrent.TimeUnit.MILLISECONDS);
					if(socket != null) {
						loAtiendo(socket);
					}
				}catch(java.lang.InterruptedException e) {
					logE(e);
				}
			}
		}
		
		public void setRunning(boolean running) {
			this.host.setRunning(running);
			if(!running) {
				try {
					this.host.ss.close();
				} catch (IOException e) {
					logE(e);
				}
				dastub.close();
			}
		}
		
		private void loAtiendo(java.net.Socket socket) {
			org.json.JSONObject request = null;
			org.json.JSONArray items = null;
			org.json.JSONArray responseArray = null;
			org.json.JSONObject item = null;
			String id = null;
			String sku = null;
			String parent = null;
			String article = null;
			java.util.Set<String> lst = null;
			org.json.JSONObject done = new org.json.JSONObject();
			done.put("action", "done");
//			log("Gonna read...");
			try(
					java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
					java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream()), true)
			){
//				log("Reading...");
				String message = br.readLine();
				try {
					request = new org.json.JSONObject(message);
					if(request.has("action")) {
//						log("got: " + message);
						if("quit".equals(request.getString("action"))) {
//							log("powering off...");
							try {
								setRunning( request.getBoolean("running") );
							}catch(Exception e) {
								logE(e);
							}
							pw.println(done);
						}else if("comoestas".equals(request.getString("action"))) {
							pw.println( new org.json.JSONObject().put("status", "OK").put("running", running).put("tasks", clientes.size() ).put("workers", workerWorkers.size()).put("serving", id).toString() );
						} else if("skuProductNo".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							for(int i=0; i<items.length(); i++) {
								item = items.getJSONObject(i);
								id   = item.getString("productNo");
								sku  = item.getString("sku");
								skuProductNo.put(sku, id);
							}
							pw.println(done);
						}else if("getSkuProductNo".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							responseArray = new org.json.JSONArray();
							for(int i=0; i<items.length(); i++) {
								if( Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb")) ) {
									parent = dastub.getSkuProductNo(items.getString(i));
								}else {
									parent = skuProductNo.get(items.getString(i));
								}
								responseArray.put(parent == null ? "" : parent);
							}
							pw.println( new org.json.JSONObject().put("items", responseArray) );
						} else if("skuSupplierAID".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							for(int i=0; i<items.length(); i++) {
								item = items.getJSONObject(i);
								id = item.getString("supplierAID");
								if(item.has("sku")) {
									sku = item.getString("sku");
									if(item.has("productNo") && !"".equals(item.getString("productNo"))) {
										parent = item.getString("productNo");
										skuSupplierAID.put(sku, id);
										supplierAIDProductNo.put(id, parent);
										lst = productNoAndVariants.get(parent);
										if(lst == null) {
											lst = new java.util.TreeSet<>();
											productNoAndVariants.put(parent, lst);
										}
										lst.add(id);
									}else {
										log("No productNo. " + item);
									}
								}else {
									log("No sku. " + item);
								}
							}
							pw.println(done);
						}else if("productByVariant".equals(request.getString("action"))) {
//							log("Serving products by variants: " + request);
							items = request.getJSONArray("variants");
							responseArray = new org.json.JSONArray();
							for(int i=0; i<items.length(); i++) {
								if(Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb"))) {
									parent = dastub.getProductByVariant(items.getString(i));
								}else {
									parent = supplierAIDProductNo.get(items.getString(i));
								}
								responseArray.put(parent == null ? "" : parent);
							}
							pw.println( new org.json.JSONObject().put("items", responseArray) );
						}else if("variantBySKU".equals(request.getString("action"))) {
//							log("Serving variants by skus: " + request);
//							log("skuSupplierAID: " + skuSupplierAID.size());
//							log("supplierAIDProductNo: " + supplierAIDProductNo.size());
							items = request.getJSONArray("skus");
							responseArray = new org.json.JSONArray();
							for(int i=0; i<items.length(); i++) {
								if(Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb"))) {
									String[] data = dastub.variantBySKU(items.getString(i));
									responseArray.put( new org.json.JSONObject()
											.put("article_sku", items.getString(i))
											.put("article", data[1] == null ? "" : data[1])
											.put("product", data[2] == null ? "" : data[2]) 
											.put("product_sku", data[3] == null ? "" : data[3])
										);
								}else {
									article = skuSupplierAID.get(items.getString(i));
									if(article != null) {
										parent = supplierAIDProductNo.get(article);
									}else {
										parent = null;
									}
									if(parent != null) {
										String[] pd = productData.get(parent);
										if(pd != null) {
											sku = pd[6];
										}
									}
									responseArray.put( new org.json.JSONObject()
											.put("article_sku", items.getString(i))
											.put("article", article == null ? "" : article)
											.put("product", parent == null ? "" : parent) 
											.put("product_sku", sku == null ? "" : sku)
										);
								}
							}
//							log("Got response: " + responseArray);
							pw.println( new org.json.JSONObject().put("items", responseArray) );
//							log("Response sent.");
						}else if("getProductVariants".equals(request.getString("action"))) {
							items = new org.json.JSONArray();
							if(Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb"))) {
								java.util.Set<String> variants = dastub.getProductVariants(request.getString("product"));
								for(String v : variants) {
									items.put(v);
								}
							}else {
								lst = productNoAndVariants.get(request.getString("product"));
								if(lst != null) {
									for(String variantId : lst) {
										items.put(variantId);
									}
								}
							}
							pw.println( new org.json.JSONObject().put("items", items) );
						}else if("putProductData".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							String[] currentOne = null;
							for(int i=0; i<items.length(); i++) {
								item = items.getJSONObject(i);
								parent = item.getString("product");
								currentOne = articleData.get(parent);
								if(currentOne != null) {
									if(currentOne.length > 14) {
										if(!"".equals(currentOne[13]) && "".equals(item.getString("MainBarCode"))) {
											eanProductNo.remove(currentOne[13]);
										}
										if(!"".equals(currentOne[14]) && "".equals(item.getString("MainBarCodeS4H"))) {
											eanProductNo.remove(currentOne[14]);
										}
									}else {
//										log("No proper line found. -->" + java.util.Arrays.asList(currentOne) + "<-- (" + currentOne.length + ")");
									}
								}
								productData.put(parent, new String[] { 
										 item.getString("Section")
										,item.getString("ItemGroup")
										,item.getString("ItemGroupS4H")
										,item.getString("BrandName")
										,item.getString("BRAND_ID_S4H")
										,item.getString("Business")
										,item.getString("SKU")
										,item.getString("SupplierID")
										,item.getString("Template")
										,item.getString("CurrentStatus")
										,item.getString("AssignTakeNoTake")
										,item.getString("SAPObjectType")
										,item.getString("FotoTomadaLiverpool")
										,item.getString("MainBarCode")
										,item.getString("MainBarCodeS4H")
										,item.has("SupplierPartNumber") ? item.getString("SupplierPartNumber") : ""
									});
								if(item.has("MainBarCode") && !"".equals(item.getString("MainBarCode"))) {
									eanProductNo.put(item.getString("MainBarCode"), parent);
								}else if(item.has("MainBarCodeS4H") && !"".equals(item.getString("MainBarCodeS4H"))) {
									eanProductNo.put(item.getString("MainBarCodeS4H"), parent);
								}
								sku = item.getString("SKU");
								if(!"".equals(sku)) {
									skuProductNo.put(sku, parent);
								}
							}
							pw.println(done);
						}else if("getProductData".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							responseArray = new org.json.JSONArray();
							for(int i=0; i<items.length(); i++) {
								if(Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb"))) {
									responseArray.put( dastub.getProductData(items.getString(i)) );
								}else {
									String[] values = null;
									values = productData.get(items.getString(i));
									responseArray.put( values == null ? 
											new org.json.JSONObject()
												.put("product", items.getString(i))
												.put("Section", "")
												.put("ItemGroup", "")
												.put("ItemGroupS4H", "")
												.put("BrandName", "")
												.put("BRAND_ID_S4H", "")
												.put("Business", "")
												.put("SKU", "")
												.put("SupplierID", "")
												.put("Template", "")
												.put("CurrentStatus", "")
												.put("AssignTakeNoTake", "")
												.put("SAPObjectType", "")
												.put("FotoTomadaLiverpool", "")
												.put("MainBarCode", "")
												.put("MainBarCodeS4H", "")
												.put("SupplierPartNumber", "")
										:
											new org.json.JSONObject()
												.put("product", items.getString(i))
												.put("Section", values[0])
												.put("ItemGroup", values[1])
												.put("ItemGroupS4H", values[2])
												.put("BrandName", values[3])
												.put("BRAND_ID_S4H", values[4])
												.put("Business", values[5])
												.put("SKU", values[6])
												.put("SupplierID", values[7])
												.put("Template", values[8])
												.put("CurrentStatus", values[9])
												.put("AssignTakeNoTake", values[10])
												.put("SAPObjectType", values.length > 11 ? values[11] : "")
												.put("FotoTomadaLiverpool", values.length > 12 ? values[12] : "")
												.put("MainBarCode", values.length > 13 ? values[13] : "")
												.put("MainBarCodeS4H", values.length > 14 ? values[14] : "")
												.put("SupplierPartNumber", values.length > 15 ? values[15] : "")
										);
								}
							}
							pw.println(new org.json.JSONObject().put("items", responseArray));
						}else if("putArticleData".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							String[] currentOne = null;
							for(int i=0; i<items.length(); i++) {
								item = items.getJSONObject(i);
								id = item.getString("variant");
								parent = item.getString("ProductNo");
								currentOne = articleData.get(id);
								if(currentOne != null) {
									if(currentOne.length > 6) {
										if(!"".equals(currentOne[5]) && "".equals(item.getString("MainBarCode"))) {
											eanSupplierAID.remove(currentOne[5]);
										}
										if(!"".equals(currentOne[6]) && "".equals(item.getString("MainBarCodeS4H"))) {
											eanSupplierAID.remove(currentOne[6]);
										}
									}else {
//										log("No proper article data object found: -->" + java.util.Arrays.asList(currentOne) + "<-- (" + currentOne.length + ")");
									}
								}
								sku = item.getString("SKU");
								articleData.put(id, new String[] { 
										 item.getString("ColoursLiverpoolAtt")
										,item.getString("TamanoUnico")
										,item.getString("ProductImage")
										,item.getString("AssignTakeNoTake")
										,item.getString("SKU")
										,item.getString("MainBarCode")
										,item.getString("MainBarCodeS4H")
										,item.has("SupplierPartNumber") ? item.getString("SupplierPartNumber") : ""
									});
								if(item.has("MainBarCode") && !"".equals(item.getString("MainBarCode"))) {
									eanSupplierAID.put(item.getString("MainBarCode"), id);
								}else if(item.has("MainBarCodeS4H") && !"".equals(item.getString("MainBarCodeS4H"))) {
									eanSupplierAID.put(item.getString("MainBarCodeS4H"), id);
								}
								if(!sku.isEmpty()) {
									skuSupplierAID.put(sku, id);
								}
								if(!"".equals(parent) && parent != null)
									supplierAIDProductNo.put(id, parent);
								else {
									supplierAIDProductNo.remove(id);
								}
								lst = productNoAndVariants.get(parent);
								if(lst == null) {
									lst = new java.util.TreeSet<>();
									productNoAndVariants.put(parent, lst);
								}
								lst.add(id);
							}
							pw.println(done);
						}else if("getArticleData".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							responseArray = new org.json.JSONArray();
							String[] values = null;
							for(int i=0; i<items.length(); i++) {
								if( Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb")) ) {
									responseArray.put( dastub.getArticleData(items.getString(i)) );
								}else {
									values = articleData.get(items.getString(i));
									parent = supplierAIDProductNo.get(items.getString(i));
									responseArray.put( values == null ? 
											new org.json.JSONObject()
												.put("variant", items.getString(i))
												.put("ProductNo", parent == null ? "" : parent )
												.put("ColoursLiverpoolAtt", "")
												.put("TamanoUnico", "")
												.put("ProductImage", "")
												.put("AssignTakeNoTake", "")
												.put("SKU", "")
												.put("MainBarCode", "")
												.put("MainBarCodeS4H", "")
												.put("SupplierPartNumber", "") :
											new org.json.JSONObject()
												.put("variant", items.getString(i))
												.put("ProductNo", parent == null ? "" : parent )
												.put("ColoursLiverpoolAtt", values[0])
												.put("TamanoUnico", values[1])
												.put("ProductImage", values[2])
												.put("AssignTakeNoTake", values.length > 3 ? values[3] : "")
												.put("SKU", values.length > 4 ? values[4] : "")
												.put("MainBarCode", values.length > 5 ? values[5] : "")
												.put("MainBarCodeS4H", values.length > 6 ? values[6] : "")
												.put("SupplierPartNumber", values.length > 7 ? values[7] : "")
										);
								}
							}
							pw.println(new org.json.JSONObject().put("items", responseArray));
						}else if("putProductExtraData".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							for(int i=0; i<items.length(); i++) {
								item = items.getJSONObject(i);
								parent = item.getString("product");
								productExtraData.put(parent, new String[] { 
										 item.getString("supplierShopId")
										,item.getString("ProductName")
										,item.getString("BuyerRejectionMessage")
										,item.getString("SupplierRejectionMessage")
										,item.getString("SkuType")
										,item.getString("BWSCL")
										,item.getString("TImportacion")
										,item.getString("Negocio")
										,item.getString("EXTWG_S4H")
										,item.getString("MesdeEntregadeMercancIa")
										,item.getString("Temporada")
										,item.getString("BWVOR")
										,item.getString("AnoEstacion")
										,item.getString("TextoAdicional")
										,item.getString("Evento")
										,item.getString("CostobrutoSinIVA")
										,item.getString("PrecioSugeridocIVA")
										,item.getString("Descuento1")
										,item.getString("Descuento2")
										,item.getString("NORMT")
										,item.getString("LABOR")
										,item.getString("DescriptionLong")
										,item.getString("DescriptionLong2")
									});
							}
							pw.println(done);
						}else if("getProductExtraData".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							responseArray = new org.json.JSONArray();
							String[] values = null;
							for(int i=0; i<items.length(); i++) {
								if( Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb")) ) {
									responseArray.put( dastub.getProductExtraData(items.getString(i), new String[] {
											"supplierShopId"
											,"ProductName"
											,"BuyerRejectionMessage"
											,"SupplierRejectionMessage"
											,"SkuType"
											,"BWSCL"
											,"TImportacion"
											,"Negocio"
											,"EXTWG_S4H"
											,"MesdeEntregadeMercancIa"
											,"Temporada"
											,"BWVOR"
											,"AnoEstacion"
											,"TextoAdicional"
											,"Evento"
											,"CostobrutoSinIVA"
											,"PrecioSugeridocIVA"
											,"Descuento1"
											,"Descuento2"
											,"LABOR"
											,"NORMT"
											,"DescriptionLong"
											,"DescriptionLong2"
											,"Currency"
											,"TypeMainBarCode"
											,"TextoAdicional"
									}) );
								}else {
									values = productExtraData.get(items.getString(i));
									responseArray.put( values == null ? 
											new org.json.JSONObject()
												.put("product", items.getString(i))
												.put("supplierShopId", "")
												.put("ProductName", "")
												.put("BuyerRejectionMessage", "")
												.put("SupplierRejectionMessage", "")
												.put("SkuType", "")
												.put("BWSCL", "")
												.put("TImportacion", "")
												.put("Negocio", "")
												.put("EXTWG_S4H", "")
												.put("MesdeEntregadeMercancIa", "")
												.put("Temporada", "")
												.put("BWVOR", "")
												.put("AnoEstacion", "")
												.put("TextoAdicional", "")
												.put("Evento", "")
												.put("CostobrutoSinIVA", "")
												.put("PrecioSugeridocIVA", "")
												.put("Descuento1", "")
												.put("Descuento2", "")
												.put("LABOR", "")
												.put("NORMT", "")
												.put("DescriptionLong", "")
												.put("DescriptionLong2", "")
										:
											new org.json.JSONObject()
											.put("product", items.getString(i))
											.put("supplierShopId", values[0])
											.put("ProductName",values[1])
											.put("BuyerRejectionMessage", values[2])
											.put("SupplierRejectionMessage", values[3])
											.put("SkuType", values[4])
											.put("BWSCL", values[5])
											.put("TImportacion", values[6])
											.put("Negocio", values[7])
											.put("EXTWG_S4H", values[8])
											.put("MesdeEntregadeMercancIa", values[9])
											.put("Temporada", values[10])
											.put("BWVOR", values[11])
											.put("AnoEstacion", values[12])
											.put("TextoAdicional", values[13])
											.put("Evento", values[14])
											.put("CostobrutoSinIVA", values[15])
											.put("PrecioSugeridocIVA", values[16])
											.put("Descuento1", values[17])
											.put("Descuento2", values[18])
											.put("LABOR", values[19])
											.put("NORMT", values[20])
											.put("DescriptionLong", values[21])
											.put("DescriptionLong2", values[22])
										);
								}
							}
							pw.println(new org.json.JSONObject().put("items", responseArray));
						}else if("putArticleExtraData".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							for(int i=0; i<items.length(); i++) {
								item = items.getJSONObject(i);
								id = item.getString("variant");
								articleExtraData.put(id, new String[] { 
										 item.getString("SAPObjectType")
										,item.getString("SkuType")
										,item.getString("CostobrutoSinIVA")
										,item.getString("PrecioSugeridocIVA")
										,item.getString("Descuento1")
										,item.getString("Descuento2")
										,item.getString("ProductName")
									});
							}
							pw.println(done);
						}else if("getArticleExtraData".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							responseArray = new org.json.JSONArray();
							String[] values = null;
							for(int i=0; i<items.length(); i++) {
								if( Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb")) ) {
									responseArray.put( dastub.getArticleExtraData(items.getString(i), new String[] {
											 "SkuType"
											,"CostobrutoSinIVA"
											,"PrecioSugeridocIVA"
											,"Descuento1"
											,"Descuento2"
											,"TypeMainBarCode"
									}) );
								}else {
									values = articleExtraData.get(items.getString(i));
									responseArray.put( values == null ? 
											new org.json.JSONObject()
												.put("variant", items.getString(i))
												.put("SAPObjectType", "")
												.put("SkuType", "")
												.put("CostobrutoSinIVA", "")
												.put("PrecioSugeridocIVA", "")
												.put("Descuento1", "")
												.put("Descuento2", "")
												.put("ProductName", "") :
											new org.json.JSONObject()
												.put("variant", items.getString(i))
												.put("SAPObjectType", values[0])
												.put("SkuType", values[1])
												.put("CostobrutoSinIVA", values[2])
												.put("PrecioSugeridocIVA", values.length > 3 ? values[3] : "")
												.put("Descuento1", values.length > 4 ? values[4] : "")
												.put("Descuento2", values.length > 5 ? values[5] : "")
												.put("ProductName", values.length > 6 ? values[6] : "")
										);
								}
							}
							pw.println(new org.json.JSONObject().put("items", responseArray));
						}else if("articleByEAN".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							responseArray = new org.json.JSONArray();
							String product = null;
							for(int i=0; i<items.length(); i++) {
								if( Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb")) ) {
									product = dastub.getEanSupplierAid(items.getString(i));
								}else {
									product = eanSupplierAID.get(items.getString(i));
								}
								responseArray.put( product == null ? "" : product );
							}
							pw.println( new org.json.JSONObject().put("items", responseArray) );
						}else if("productByEAN".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							responseArray = new org.json.JSONArray();
							String product = null;
							for(int i=0; i<items.length(); i++) {
								if( Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb")) ) {
									product = dastub.getEanProductNo(items.getString(i));
								}else {
									product = eanProductNo.get(items.getString(i));
								}
								responseArray.put( product == null ? "" : product );
							}
							pw.println( new org.json.JSONObject().put("items", responseArray) );
						}else if("retiraProducto".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							responseArray = new org.json.JSONArray();
							for(int i=0; i<items.length(); i++) {
								String[] info = productData.remove(items.getString(i));
								sku = info[6];
								if(sku != null && !"".equals(sku)) {
									skuProductNo.remove(sku);
									String ean = "".equals( info[13] ) ? info[14] : info[13];
									if(ean != null && !"".equals(ean)) {
										eanProductNo.remove(ean);
									}
								}
								java.util.Set<String> s = productNoAndVariants.remove(items.getString(i));
								for(String sid : s) {
									supplierAIDProductNo.remove(sid);
								}
								productExtraData.remove(items.getString(i));
							}
							pw.println(done);
						}else if("retiraArticulo".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							responseArray = new org.json.JSONArray();
							for(int i=0; i<items.length(); i++) {
								String[] info = articleData.remove(items.getString(i));
								if(info != null) {
									if(info.length < 5) {
//										log("Arreglo de datos de artículo incompleto, se obtuvo: " + java.util.Arrays.asList(info));
									}else {
										sku = info[4];
										if(sku != null && !"".equals(sku)) {
											skuSupplierAID.remove(sku);
										}
										if(info.length < 6) {
//											log("Arreglo de datos de artículo incompleto para EAN, se obtuvo: " + java.util.Arrays.asList(info));
										}else {
											String ean = "".equals( info[5] ) ? info.length > 6 ? info[6] : "" : info[5];
											if(ean != null && !"".equals(ean)) {
												eanSupplierAID.remove(ean);
											}
										}
									}
								}
								parent = supplierAIDProductNo.remove(items.getString(i));
								if(parent != null && !"".equals(parent)) {
									java.util.Set<String> set = productNoAndVariants.get(parent);
									if(set != null) {
										set.remove(items.getString(i));
									}
								}
								articleExtraData.remove(items.getString(i));
							}
							pw.println(done);
						}else if("retiraProductoPorSKU".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							for(int i=0; i<items.length(); i++) {
								sku = items.getString(i);
								String productNo = skuProductNo.remove(sku);
								if(productNo != null) {
									String[] info = productData.remove(productNo);
									if(info != null) {
										String ean = "".equals( info[13] ) ? info[14] : info[13];
										if(ean != null && !"".equals(ean)) {
											eanProductNo.remove(ean);
										}
									}
									java.util.Set<String> s = productNoAndVariants.remove(productNo);
									for(String sid : s) {
										supplierAIDProductNo.remove(sid);
									}
								}
							}
							pw.println(done);
						}else if("retiraEANSupplierAID".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							String supplierAID = null;
							String[] data = null;
							for(int i=0; i<items.length(); i++) {
								supplierAID = eanSupplierAID.remove(items.getString(i));
								if(supplierAID != null) {
									data = articleData.get(supplierAID);
									if(data != null) {
										if(data[5] != null && data[5].equals(items.getString(i))) {
											data[5] = "";
										}else if(data[6] != null && data[6].equals(items.getString(i))) {
											data[6] = "";
										}
									}
								}
							}
						}else if("retiraEANProductNo".equals(request.get("action"))) {
							items = request.getJSONArray("items");
							String productNo = null;
							String[] data = null;
							for(int i=0; i<items.length(); i++) {
								productNo = eanProductNo.remove(items.get(i));
								if(productNo != null) {
									data = productData.get(productNo);
									if(data != null) {
										try {
											if(data[13] != null && data[13].equals(items.getString(i))) {
												data[13] = "";
											}else if(data[14] != null && data[14].equals(items.getString(i))) {
												data[14] = "";
											}
										}catch(ArrayIndexOutOfBoundsException e) {
//											log("Not complete entry of " + data.length + " elements.");
										}
									}
								}
							} pw.println(done);
						}else if("retiraArticuloPorSKU".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							for(int i=0; i<items.length(); i++) {
								String supplierAID = skuSupplierAID.remove(items.getString(i));
								String[] info = articleData.remove(supplierAID);
								if(info != null) {
									String ean = "".equals( info[5] ) ? info[6] : info[5];
									if(ean != null && !"".equals(ean)) {
										eanSupplierAID.remove(ean);
									}
								}
								parent = supplierAIDProductNo.remove(supplierAID);
								if(parent != null && !"".equals(parent)) {
									java.util.Set<String> set = productNoAndVariants.get(parent);
									if(set != null) {
										set.remove(items.getString(i));
									}
								}
							}
							pw.println(done);
						}else if("addGlobalMetaData".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							java.util.concurrent.ConcurrentHashMap<String, String> properties = null;
							String characteristic = null;
							String property = null;
							String propertyValue = null;
							for(int i=0; i<items.length(); i++) {
								item = items.getJSONObject(i);
								characteristic = !item.has("characteristic") ? null : item.getString("characteristic");
								property = !item.has("property") ? null : item.getString("property");
								propertyValue = !item.has("propertyValue") ? null : item.getString("propertyValue");
								if(characteristic != null && property != null && propertyValue != null && !"".equals(characteristic) && !"".equals(property)) {
									properties = globalMetaData.get(characteristic);
									if(properties == null) {
										properties = new java.util.concurrent.ConcurrentHashMap<>();
										globalMetaData.put(characteristic, properties);
									}
									properties.put(property, propertyValue);
								}
							}
							pw.println(done);
						}else if("getGlobalMetaData".equals(request.getString("action"))) {
							items = new org.json.JSONArray();
							if( Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb")) ) {
								item = dastub.getGlobalMetadata("CreateProposal") ;
							}else {
								item = new org.json.JSONObject();
								for(java.util.Map.Entry<String, java.util.concurrent.ConcurrentHashMap<String, String>> entry : globalMetaData.entrySet()) {
									org.json.JSONObject properties = new org.json.JSONObject();
									for(java.util.Map.Entry<String, String> entry0 : entry.getValue().entrySet()) {
										properties.put(entry0.getKey(), entry0.getValue());
									}
									if(properties.length() > 0) {
										item.put(entry.getKey(), properties);
									}
								}
							}
							items.put(item);
							pw.println( new org.json.JSONObject().put("items", items) );
						}else if("addTemplateCharacteristicMetaData".equals(request.getString("action"))) {
							if(request.has("items")) {
								items = request.getJSONArray("items");
								String template = null;
								String characteristic = null;
								String property = null;
								String propertyValue = null;
								for(int i=0; i<items.length(); i++) {
									item = items.getJSONObject(i);
									template 			= !item.has("template") 		? null : item.getString("template");
									characteristic 		= !item.has("characteristic") 	? null : item.getString("characteristic");
									property 			= !item.has("property") 		? null : item.getString("property");
									propertyValue 		= !item.has("propertyValue") 	? null : item.getString("propertyValue");
									if(template != null && characteristic != null && property != null && propertyValue != null && !"".equals(template) && !"".equals(characteristic) && !"".equals(property)) {
										java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>> characteristicMap = templateCharacteristicMetaData.get(template);
										if(characteristicMap == null) {
											characteristicMap = new java.util.concurrent.ConcurrentHashMap<>();
											templateCharacteristicMetaData.put(template, characteristicMap);
										}
										java.util.concurrent.ConcurrentHashMap<String, String> properties = characteristicMap.get(characteristic);
										if(properties == null) {
											properties = new java.util.concurrent.ConcurrentHashMap<>();
											characteristicMap.put(characteristic, properties);
										}
										properties.put(property, propertyValue);
									}
								}
							}
							pw.println(done);
						}else if("getTemplateCharacteristicMetaDataByTemplate".equals(request.getString("action"))) {
							org.json.JSONArray itemsResponse = new org.json.JSONArray();
							if(request.has("items")) {
								try {
									items = request.getJSONArray("items");
									java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>> templateMap = null;
									for(int i=0; i<items.length(); i++) {
										if( Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb")) ) {
											itemsResponse = dastub.getTemplateCharacteristicPropertyValue(items.getString(i), "CreateProposal");
										}else {
											org.json.JSONObject ir = new org.json.JSONObject();
											itemsResponse.put(ir);
											templateMap = templateCharacteristicMetaData.get(items.getString(i));
											if(templateMap != null) {
												for(java.util.Map.Entry<String, java.util.concurrent.ConcurrentHashMap<String, String>> entry : templateMap.entrySet()) {
													org.json.JSONObject properties = new org.json.JSONObject();
													ir.put(entry.getKey(), properties);
													for(java.util.Map.Entry<String, String> entry0 : entry.getValue().entrySet()) {
														properties.put(entry0.getKey(), entry0.getValue());
													}
												}
											}
										}
									}
								}catch(org.json.JSONException e) {
									logE(e);
								}
							}
							pw.println( new org.json.JSONObject().put("items", itemsResponse) );
						}else if("getTemplateCharacteristicMetaDataByTemplateCharacteristic".equals(request.getString("action"))) {
							org.json.JSONArray itemsResponse = new org.json.JSONArray();
							if(request.has("items")) {
								try {
									items = request.getJSONArray("items");
									java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>> templateMap = null;
									for(int i=0; i<items.length(); i++) {
										item = items.getJSONObject(i);
										if( Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb")) ) {
											if(item.has("template") && item.has("characteristic")) {
												itemsResponse = dastub.getTemplateCharacteristicPropertyValue(item.getString("template"), item.getString("characteristic"), "CreateProposal");
											}
										}else {
											org.json.JSONObject ir = new org.json.JSONObject();
											itemsResponse.put(ir);
											if(item.has("template") && !"".equals(item.getString("template")) && item.has("characteristic") && !"".equals(item.getString("characteristic"))) {
												templateMap = templateCharacteristicMetaData.get(item.getString("template"));
												if(templateMap != null) {
													java.util.concurrent.ConcurrentHashMap<String, String> characteristicProperties = templateMap.get(item.getString("characteristic"));
													if(characteristicProperties != null) {
														org.json.JSONObject properties = new org.json.JSONObject();
														ir.put(item.getString("characteristic"), properties);
														for(java.util.Map.Entry<String, String> entry : characteristicProperties.entrySet()) {
															properties.put(entry.getKey(), entry.getValue());
														}
													}
												}
											}
										}
									}
								}catch(org.json.JSONException e) {
									logE(e);
								}
							}
							pw.println( new org.json.JSONObject().put("items", itemsResponse) );
						}else if("getTemplateCharacteristicMetaDataByTemplateCharacteristicProperty".equals(request.getString("action"))) {
							org.json.JSONArray itemsResponse = new org.json.JSONArray();
							if(request.has("items")) {
								try {
									items = request.getJSONArray("items");
									java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>> templateMap = null;
									for(int i=0; i<items.length(); i++) {
										item = items.getJSONObject(i);
										if( Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb")) ) {
											if(item.has("template") && item.has("characteristic") && item.has("property")) {
												itemsResponse = dastub.getTemplateCharacteristicPropertyValue(item.getString("template"), item.getString("characteristic"), "CreateProposal", item.getString("property"));
											}
										}else {
											org.json.JSONObject ir = new org.json.JSONObject();
											itemsResponse.put(ir);
											if(item.has("template") && !"".equals(item.getString("template")) && item.has("characteristic") && !"".equals(item.getString("characteristic")) && item.has("property") && !"".equals(item.getString("property"))) {
												templateMap = templateCharacteristicMetaData.get(item.getString("template"));
												if(templateMap != null) {
													java.util.concurrent.ConcurrentHashMap<String, String> characteristicProperties = templateMap.get(item.getString("characteristic"));
													if(characteristicProperties != null) {
														org.json.JSONObject properties = new org.json.JSONObject();
														ir.put(item.getString("characteristic"), properties);
														String propertyValue = characteristicProperties.get(item.getString("property"));
														if(propertyValue != null) {
															properties.put(item.getString("property"), propertyValue);
														}
													}
												}
											}
										}
									}
								}catch(org.json.JSONException e) {
									logE(e);
								}
							}
							pw.println( new org.json.JSONObject().put("items", itemsResponse) );
						}else if("removeGlobalMetaDataEntry".equals(request.getString("action"))) {
							if(request.has("items")) {
								items = request.getJSONArray("items");
								for(int i=0; i<items.length(); i++) {
									globalMetaData.remove(items.getString(i));
								}
							}
							pw.print(done);
						}else if("removeGlobalMetaDataEntryByProperty".equals(request.getString("action"))) {
							if(request.has("items")) {
								items = request.getJSONArray("items");
								for(int i=0; i<items.length(); i++) {
									item = items.getJSONObject(i);
									if(item.has("characteristic") && item.has("property")) {
										java.util.concurrent.ConcurrentHashMap<String, String> properties = globalMetaData.get(item.getString("characteristic"));
										if(properties != null) {
											properties.remove(item.getString("property"));
										}
									}
								}
							}
							pw.println(done);
						}else if("removeTemplateCharacteristicMetaData".equals(request.getString("action"))) {
							if(request.has("items")) {
								items = request.getJSONArray("items");
								for(int i=0; i<items.length(); i++) {
									templateCharacteristicMetaData.remove(items.getString(i));
								}
							}
							pw.print(done);
						}else if("removeTemplateCharacteristicMetaDataByCharacteristic".equals(request.getString("action"))) {
							if(request.has("items")) {
								items = request.getJSONArray("items");
								for(int i=0; i<items.length(); i++) {
									item = items.getJSONObject(i);
									if(item.has("template") && item.has("characteristic")) {
										java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>> characteristics = templateCharacteristicMetaData.get(item.getString("template"));
										if(characteristics != null) {
											characteristics.remove(item.getString("characteristic"));
										}
									}
								}
							}
							pw.println(done);
						}else if("removeTemplateCharacteristicMetaDataByProperty".equals(request.getString("action"))) {
							if(request.has("items")) {
								items = request.getJSONArray("items");
								for(int i=0; i<items.length(); i++) {
									item = items.getJSONObject(i);
									if(item.has("template") && item.has("characteristic") && item.has("property")) {
										java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>> characteristics = templateCharacteristicMetaData.get(item.getString("template"));
										if(characteristics != null) {
											java.util.concurrent.ConcurrentHashMap<String, String> properties = characteristics.get(item.getString("characteristic"));
											if(properties != null) {
												properties.remove(item.getString("property"));
											}
										}
									}
								}
							}
							pw.println(done);
						}else if("addTemplateName".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							for(int i=0; i<items.length(); i++) {
								item = items.getJSONObject(i);
								templateNames.put(item.getString("template"), item.getString("name"));
							}
							pw.println(done);
						}else if("getTemplateName".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							org.json.JSONArray itemsResponse = new org.json.JSONArray();
							String name = null;
							for(int i=0; i<items.length(); i++) {
								if( Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb")) ) {
									itemsResponse.put( dastub.getTemplateName(items.getString(i)) );
								}else {
									name = templateNames.get(items.getString(i));
									itemsResponse.put(name == null ? "" : name);
								}
							}
							pw.print( new org.json.JSONObject().put("items", itemsResponse) );
						}else if("removeTemplateName".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							for(int i=0; i<items.length(); i++) {
								templateNames.remove(items.getString(i));
							}
							pw.println(done);
						}else if("addCharacteristicData".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							for(int i=0; i<items.length(); i++) {
								item = items.getJSONObject(i);
								characteristicData.put(item.getString("characteristic"), new String[] { item.getString("name"), item.getString("dataType"), item.getString("lookup")});
							}
							pw.println(done);
						}else if("getCharacteristicData".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							org.json.JSONArray itemsResponse = new org.json.JSONArray();
							String[] data = null;
							for(int i=0; i<items.length(); i++) {
								if( Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb")) ) {
									itemsResponse.put( dastub.getCharacteristicData(items.getString(i)) );
								}else {
									data = characteristicData.get(items.getString(i));
									if(data == null) {
										itemsResponse.put(new org.json.JSONObject().put("characteristic", items.getString(i)).put("name", "").put("dataType", "").put("lookup", ""));
									}else {
										itemsResponse.put(new org.json.JSONObject().put("characteristic", items.getString(i)).put("name", data[0]).put("dataType", data[1]).put("lookup", data[2]));
									}
								}
							}
							pw.print( new org.json.JSONObject().put("items", itemsResponse) );
						}else if("removeCharacteristicData".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							for(int i=0; i<items.length(); i++) {
								characteristicData.remove(items.getString(i));
							}
							pw.println(done);
						}else if("addContenidoDeDiccionario".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							String diccionario = null;
							String idValor = null;
							for(int i=0; i<items.length(); i++) {
								item = items.getJSONObject(i);
								if(item.has("diccionario") && item.has("idValor")) {
									diccionario = item.getString("diccionario");
									idValor = item.getString("idValor");
//									log("From adding content to dictionary --> " + item);
									if(!"".equals(diccionario) && !"".equals(idValor)) {
										java.util.concurrent.ConcurrentHashMap<String, String[]> dt = contenidoDeDiccionario.get(diccionario);
										if(dt == null) {
											dt = new java.util.concurrent.ConcurrentHashMap<>();
											contenidoDeDiccionario.put(diccionario, dt);
										}
										String[] vals = dt.get(idValor);
										if(vals == null) {
											vals = new String[6];
											dt.put(idValor, vals);
										}
										vals[0] = !item.has("structureGroup") ? "" : item.getString("structureGroup");
										vals[1] = !item.has("characteristic") ? "" : item.getString("characteristic");
										vals[2] = !item.has("property") ? "" : item.getString("property");
										vals[3] = !item.has("propertyValue") ? "" : item.getString("propertyValue");
										vals[4] = !item.has("propertyShortCode") ? "" : item.getString("propertyShortCode");
										vals[5] = !item.has("alternativeValue") ? "" : item.getString("alternativeValue");
										
										if("GlobalTemplateAttributeConfiguration".equals(item.getString("diccionario"))) {
											java.util.concurrent.ConcurrentHashMap<String, String> gbmd = globalMetaData.get(vals[1]);
											if(gbmd == null) {
												gbmd = new java.util.concurrent.ConcurrentHashMap<>();
												globalMetaData.put(vals[1], gbmd);
											}
											gbmd.put(vals[4], vals[3]);
										}else if("ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla".equals(item.getString("diccionario"))) {
//											log("Its template metadata: " + java.util.Arrays.asList(vals));
											java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>> templateData = templateCharacteristicMetaData.get(vals[0]);
											if(templateData == null) {
												templateData = new java.util.concurrent.ConcurrentHashMap<>();
												templateCharacteristicMetaData.put(vals[0], templateData);
											}
											java.util.concurrent.ConcurrentHashMap<String, String> gbmd = templateData.get(vals[1]);
											if(gbmd == null) {
												gbmd = new java.util.concurrent.ConcurrentHashMap<>();
												templateData.put(vals[1], gbmd);
											}
											gbmd.put(vals[4], vals[3]);
										}else {
//											log("It is another dictionary: " + item.getString("diccionario"));
										}
									}
								}
							}
							pw.println(done);
						}else if("getContenidoDeDiccionario".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							org.json.JSONArray itemsResponse = new org.json.JSONArray();
							for(int i=0; i<items.length(); i++) {
								item = items.getJSONObject(i);
								if(item.has("diccionario") && item.has("idValor")) {
									if( Boolean.parseBoolean(PropertiesManager.get("p360.contingency.usedb")) ) {
										itemsResponse.put( dastub.getDictionaryEntry(item.getString("diccionario"), item.getString("idValor")) );
									}else {
										java.util.concurrent.ConcurrentHashMap<String, String[]> content = contenidoDeDiccionario.get(item.getString("diccionario"));
										if(content != null) {
											String[] tupla = content.get(item.getString("idValor"));
											if(tupla != null) {
												itemsResponse.put(new org.json.JSONObject().put("diccionario", item.getString("diccionario")).put("idValor", item.getString("idValor")).put("structureGroup", tupla[0]).put("characteristic", tupla[1]).put("property", tupla[2]).put("propertyValue", tupla[3]).put("propertyShortCode", tupla[4]).put("alternativeValue", tupla[5]));
											}else {
												itemsResponse.put(new org.json.JSONObject().put("diccionario", item.getString("diccionario")).put("idValor", item.getString("idValor")).put("structureGroup", "").put("characteristic", "").put("property", "").put("propertyValue", "").put("propertyShortCode", "").put("alternativeValue", ""));
											}
										}else {
											itemsResponse.put(new org.json.JSONObject().put("diccionario", item.getString("diccionario")).put("idValor", item.getString("idValor")).put("structureGroup", "").put("characteristic", "").put("property", "").put("propertyValue", "").put("propertyShortCode", "").put("alternativeValue", ""));
										}
									}
								}else {
									itemsResponse.put(new org.json.JSONObject().put("diccionario", !item.has("diccionario") ? "" : item.getString("diccionario")).put("idValor", !item.has("idValor") ? "" : item.getString("idValor")).put("structureGroup", "").put("characteristic", "").put("property", "").put("propertyValue", "").put("propertyShortCode", "").put("alternativeValue", ""));
								}
							}
							pw.print( new org.json.JSONObject().put("items", itemsResponse) );
						}else if("removeContenidoDeDiccionario".equals(request.getString("action"))) {
							items = request.getJSONArray("items");
							for(int i=0; i<items.length(); i++) {
								item = items.getJSONObject(i);
								if(item.has("diccionario") && item.has("idValor")) {
									java.util.concurrent.ConcurrentHashMap<String, String[]> content = contenidoDeDiccionario.get(item.getString("diccionario"));
									if(content != null) {
										String[] tupla = content.remove(item.getString("idValor"));
										if(tupla != null) {
											if("GlobalTemplateAttributeConfiguration".equals(item.getString("diccionario"))) {
												java.util.concurrent.ConcurrentHashMap<String, String> gbmd = globalMetaData.get(tupla[1]);
												if(gbmd != null) {
													gbmd.remove(tupla[4]);
												}
											}else if("ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla".equals(item.getString("diccionario"))) {
												java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.ConcurrentHashMap<String, String>> templateData = templateCharacteristicMetaData.get(tupla[0]);
												if(templateData != null) {
													java.util.concurrent.ConcurrentHashMap<String, String> gbmd = templateData.get(tupla[1]);
													if(gbmd != null) {
														gbmd.remove(tupla[4]);
													}
												}
											}
										}
									}
								}
							}
							pw.println(done);
						}else if("dump".equals(request.getString("action"))) {
							keepData();
							pw.print(done);
						}
					}
				}catch(org.json.JSONException e) {
					logE(e);
				}catch(NullPointerException e) {
					logE(e);
				}
			}catch(java.io.IOException e) {
				logE(e);
			}
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private void startListener() {
		Thread t = new Thread() {
			@Override
			public void run() {
				int a = Runtime.getRuntime().availableProcessors() - 3;
				a = a <= 0 ? 1 : a;
				java.util.concurrent.ArrayBlockingQueue<java.net.Socket> clientes = new java.util.concurrent.ArrayBlockingQueue<>(a);
				java.util.LinkedList<Thread> workers = new java.util.LinkedList<>();
				for(int i=0; i<a; i++) {
					Worker w = new Worker(ProductVariantInfoAdmin002.this, clientes);
					Thread t = new Thread(w);
					t.setPriority(Thread.currentThread().getPriority() - 1);
					t.setDaemon(false);
					t.start();
					workerWorkers.addLast(w);
					workers.addLast(t);
				}
				try(java.net.ServerSocket server = new java.net.ServerSocket( Integer.parseInt( PropertiesManager.get("p360.contingency.pvia.server.port", "23540") ) )){
					ss = server;
					while(running) {
						java.net.Socket socket = server.accept();
//						log("Adding a socket...");
						try {
							while(running && !clientes.offer(socket, 10, java.util.concurrent.TimeUnit.MICROSECONDS)) {
//								log("Waiting...");
							}
//							log("Socket added");
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}catch(java.io.IOException e) {
					logE(e);
					running = false;
					log("Interrupted... " + e.getMessage());
				}
				log("Now waiting for workers to finish...");
				try {
					for(Thread worker : workers) {
						worker.join();
					}
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		t.setDaemon(false);
		t.start();
		log("Now listenning...");
		try {
			t.join();
		}catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void setRunning(boolean running) {
		this.running = running;
	}



	private static final Logger LOGGER = Logger.getLogger(ProductVariantInfoAdmin002.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler("../logs/pvia/information_admin-%g.log", 5 * 1024 * 1024, 5, true);
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
//		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
//				new java.io.FileOutputStream("../logs/information_admin.log", true)))) {
//			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date()))
//					+ "] " + message);
//		} catch (java.io.IOException e) {
//		}
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/information_admin.log", true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
	
}
