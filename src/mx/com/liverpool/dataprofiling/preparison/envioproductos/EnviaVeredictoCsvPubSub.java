package mx.com.liverpool.dataprofiling.preparison.envioproductos;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class EnviaVeredictoCsvPubSub {

	private static final String LOG_FILE = "./logs/enviaVeredictoCsvPubSub.log";

	private final java.util.Map<String, Integer> header = new java.util.HashMap<>();
	private final java.util.Map<String, org.json.JSONObject> productsById = new java.util.LinkedHashMap<>();
	private final java.util.Map<String, java.util.Set<String>> variantsByProduct = new java.util.HashMap<>();

	private final int batchSize;
	private final boolean dryRun;
	private final PubSubGCP pub;

	private long recordNumber = 0;
	private long rowsRead = 0;
	private long rowsSkippedExternalOnly = 0;
	private long rowsSkippedNoProduct = 0;
	private long variantsAdded = 0;
	private long variantsDuplicated = 0;
	private long messagesPublished = 0;
	private long productsPublished = 0;

	public EnviaVeredictoCsvPubSub(int batchSize, boolean dryRun) {
		this.batchSize = batchSize <= 0 ? 500 : batchSize;
		this.dryRun = dryRun;

		if(dryRun) {
			this.pub = null;
		} else {
			this.pub = new PubSubGCP(
					PropertiesManager.get("p360.contingency.gcp.service_account_back"),
					PropertiesManager.get("p360.contingency.gcp.project_back"),
					PropertiesManager.get("p360.contingency.gcp.post_products_topic")
			);
		}
	}

	public void process(java.nio.file.Path csvPath) {
		long init = System.currentTimeMillis();

		String endLine = detectEndLine(csvPath);

		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
				'"',
				',',
				'\\',
				endLine,
				java.nio.charset.StandardCharsets.UTF_8,
				values -> processLine(values)
		);

		parser.parse(csvPath);

		publishAll();

		log("Done. rowsRead=" + rowsRead
				+ ", productsPublished=" + productsPublished
				+ ", variantsAdded=" + variantsAdded
				+ ", variantsDuplicated=" + variantsDuplicated
				+ ", rowsSkippedExternalOnly=" + rowsSkippedExternalOnly
				+ ", rowsSkippedNoProduct=" + rowsSkippedNoProduct
				+ ", messagesPublished=" + messagesPublished
				+ ", elapsedMs=" + (System.currentTimeMillis() - init));
	}

	private void processLine(String[] values) {
		recordNumber++;

		if(recordNumber == 1) {
			loadHeader(values);
			return;
		}

		rowsRead++;

		String veredicto = get(values, "Veredicto");

		if("SOLO_EN_EXTERNA".equalsIgnoreCase(veredicto)
				|| "SOLO_EN_ARCHIVO_EXTERNO".equalsIgnoreCase(veredicto)
				|| "SOLO_EN_EXT".equalsIgnoreCase(veredicto)) {
			rowsSkippedExternalOnly++;
			return;
		}

		String productId = get(values, "Identifier");

		if(isBlank(productId)) {
			rowsSkippedNoProduct++;
			log("Skipping row without Identifier. recordNumber=" + recordNumber);
			return;
		}

		org.json.JSONObject product = productsById.get(productId);

		if(product == null) {
			product = buildProduct(values, productId);
			productsById.put(productId, product);
			variantsByProduct.put(productId, new java.util.HashSet<>());
		} else {
			patchMissingProductValues(product, values);
		}

		String variantId = get(values, "ArticleIdentifier");

		if(isBlank(variantId)) {
			variantId = productId;
		}

		java.util.Set<String> knownVariants = variantsByProduct.get(productId);

		if(!knownVariants.add(variantId)) {
			variantsDuplicated++;
			return;
		}

		org.json.JSONArray variants = product.getJSONArray("variants");
		variants.put(buildVariant(values, variantId));
		variantsAdded++;
	}

	private void loadHeader(String[] values) {
		for(int i = 0; i < values.length; i++) {
			String name = normalizeHeader(values[i]);

			if(!isBlank(name) && !header.containsKey(name)) {
				header.put(name, i);
				header.put(name.toLowerCase(java.util.Locale.ROOT), i);
			}
		}

		requireHeader("Identifier");
		requireHeader("ArticleIdentifier");
	}

	private org.json.JSONObject buildProduct(String[] values, String productId) {
		org.json.JSONObject product = new org.json.JSONObject();

		product.put("proposalId", productId);
		putIfNotBlank(product, "template", get(values, "Template"));
		putIfNotBlank(product, "Business", get(values, "Business"));
		putIfNotBlank(product, "SKU", get(values, "SKU"));
		putIfNotBlank(product, "MainBarCode", get(values, "EAN"));

		putIfNotBlank(product, "currentStatus", get(values, "CurrentStaus", "CurrentStatus"));
		putIfNotBlank(product, "previousStatus", get(values, "PrevStatus"));
		putIfNotBlank(product, "externalStatus", get(values, "ExternalStatus"));

		product.put("variants", new org.json.JSONArray());

		return product;
	}

	private void patchMissingProductValues(org.json.JSONObject product, String[] values) {
		putIfMissingAndNotBlank(product, "template", get(values, "Template"));
		putIfMissingAndNotBlank(product, "Business", get(values, "Business"));
		putIfMissingAndNotBlank(product, "SKU", get(values, "SKU"));
		putIfMissingAndNotBlank(product, "MainBarCode", get(values, "EAN"));

		putIfMissingAndNotBlank(product, "currentStatus", get(values, "CurrentStaus", "CurrentStatus"));
		putIfMissingAndNotBlank(product, "previousStatus", get(values, "PrevStatus"));
		putIfMissingAndNotBlank(product, "externalStatus", get(values, "ExternalStatus"));
	}

	private org.json.JSONObject buildVariant(String[] values, String variantId) {
		org.json.JSONObject variant = new org.json.JSONObject();

		variant.put("variantId", variantId);

		putIfNotBlank(variant, "SKU", get(values, "ArticleSKU"));
		putIfNotBlank(variant, "MainBarCode", get(values, "ArticleEAN"));
		putIfNotBlank(variant, "Business", get(values, "ArticleBusiness"));

		putIfNotBlank(variant, "currentStatus", get(values, "ArticleCurrentStaus", "ArticleCurrentStatus"));
		putIfNotBlank(variant, "previousStatus", get(values, "ArticlePrevStatus"));
		putIfNotBlank(variant, "externalStatus", get(values, "ArticleExternalStatus"));

		return variant;
	}

	private void publishAll() {
		org.json.JSONArray jps = new org.json.JSONArray();
		int currentBatchCount = 0;

		for(org.json.JSONObject product : productsById.values()) {
			jps.put(product);
			currentBatchCount++;
			productsPublished++;

			if(currentBatchCount >= batchSize) {
				publishBatch(jps);
				jps = new org.json.JSONArray();
				currentBatchCount = 0;
			}
		}

		if(jps.length() > 0) {
			publishBatch(jps);
		}
	}

	private void publishBatch(org.json.JSONArray jps) {
		org.json.JSONObject body = new org.json.JSONObject().put("products", jps);

		if(dryRun) {
			System.out.println(body.toString(2));
			log("DRY_RUN body=" + body.toString());
		} else {
			pub.publishMessage(body.toString());
			log("Published message. products=" + jps.length());
		}

		messagesPublished++;
	}

	private String get(String[] values, String... columnNames) {
		for(String columnName : columnNames) {
			Integer idx = header.get(columnName);

			if(idx == null) {
				idx = header.get(columnName.toLowerCase(java.util.Locale.ROOT));
			}

			if(idx != null && idx >= 0 && idx < values.length) {
				return cleanValue(values[idx]);
			}
		}

		return "";
	}

	private void putIfNotBlank(org.json.JSONObject object, String key, String value) {
		if(!isBlank(value)) {
			object.put(key, value);
		}
	}

	private void putIfMissingAndNotBlank(org.json.JSONObject object, String key, String value) {
		if(!object.has(key) && !isBlank(value)) {
			object.put(key, value);
		}
	}

	private void requireHeader(String columnName) {
		if(!header.containsKey(columnName) && !header.containsKey(columnName.toLowerCase(java.util.Locale.ROOT))) {
			throw new IllegalStateException("Missing required header: " + columnName);
		}
	}

	private String normalizeHeader(String value) {
		if(value == null) {
			return "";
		}

		String v = value;

		if(!v.isEmpty() && v.charAt(0) == '\uFEFF') {
			v = v.substring(1);
		}

		return v.trim();
	}

	private String cleanValue(String value) {
		if(value == null) {
			return "";
		}

		String v = value;

		if(!v.isEmpty() && v.charAt(0) == '\uFEFF') {
			v = v.substring(1);
		}

		return v.trim();
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	private String detectEndLine(java.nio.file.Path path) {
		try(java.io.InputStream in = java.nio.file.Files.newInputStream(path)) {
			int previous = -1;
			int current;

			while((current = in.read()) != -1) {
				if(current == '\n') {
					return previous == '\r' ? "\r\n" : "\n";
				}

				if(current == '\r') {
					int next = in.read();
					if(next == '\n') {
						return "\r\n";
					}
					return "\r";
				}

				previous = current;
			}
		} catch(java.io.IOException e) {
			throw new RuntimeException("Could not detect line ending for " + path, e);
		}

		return "\r\n";
	}

	private synchronized void log(String message) {
		try {
			java.nio.file.Path logPath = java.nio.file.Paths.get(LOG_FILE);
			java.nio.file.Path parent = logPath.getParent();

			if(parent != null) {
				java.nio.file.Files.createDirectories(parent);
			}

			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
					new java.io.FileOutputStream(logPath.toFile(), true),
					java.nio.charset.StandardCharsets.UTF_8))) {
				pw.println("[" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date()) + "] " + message);
			}
		} catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
//		if(args.length < 1) {
//			System.out.println("Uso:");
//			System.out.println("java " + EnviaVeredictoCsvPubSub.class.getName() + " <archivo.csv> [batchSize] [--dry-run]");
//			return;
//		}
		args = new String[] { "C:\\opt\\LVP\\desorden\\PROD\\sqlrunner_PIM_MASTER_20260604_103552.csv" };
//		args = new String[] { "C:\\opt\\LVP\\desorden\\PROD\\DataEnvioEUCat_Stats_Business_Template_Prod_Arts_20260526_130859.csv" };

		java.nio.file.Path csvPath = java.nio.file.Paths.get(args[0]);
		int batchSize = 500;
		boolean dryRun = false;

		for(int i = 1; i < args.length; i++) {
			if("--dry-run".equalsIgnoreCase(args[i])) {
				dryRun = true;
			} else {
				batchSize = Integer.parseInt(args[i]);
			}
		}

		new EnviaVeredictoCsvPubSub(batchSize, dryRun).process(csvPath);
	}
}