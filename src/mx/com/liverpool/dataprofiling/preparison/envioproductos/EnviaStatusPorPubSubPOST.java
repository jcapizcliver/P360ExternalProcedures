package mx.com.liverpool.dataprofiling.preparison.envioproductos;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class EnviaStatusPorPubSubPOST {

	private final int batchSize;
	private final PubSubGCP pub;
	private String prevProdId = null;
	private org.json.JSONArray variants = null;
	private org.json.JSONObject currentProduct = null;
	private org.json.JSONArray products = new org.json.JSONArray();
	private org.json.JSONObject body = new org.json.JSONObject().put("products", products);

	private long recordNumber = 0;
	private long rowsRead = 0;
	private long rowsSkippedExternalOnly = 0;
	private long rowsSkippedNoProduct = 0;
	private long variantsAdded = 0;
	private long variantsDuplicated = 0;
	private long messagesPublished = 0;
	private long productsPublished = 0;

	public EnviaStatusPorPubSubPOST(int batchSize) {
		this.batchSize = batchSize <= 0 ? 500 : batchSize;

		this.pub = new PubSubGCP(
				PropertiesManager.get("p360.contingency.gcp.service_account_back"),
				PropertiesManager.get("p360.contingency.gcp.project_back"),
				PropertiesManager.get("p360.contingency.gcp.post_products_topic")
		);
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
		sendData();

		System.out.println("Done. rowsRead=" + rowsRead
				+ ", productsPublished=" + productsPublished
				+ ", variantsAdded=" + variantsAdded
				+ ", variantsDuplicated=" + variantsDuplicated
				+ ", rowsSkippedExternalOnly=" + rowsSkippedExternalOnly
				+ ", rowsSkippedNoProduct=" + rowsSkippedNoProduct
				+ ", messagesPublished=" + messagesPublished
				+ ", elapsedMs=" + (System.currentTimeMillis() - init));
	}
	
	private void sendData() {
		if(products.length() > 0) {
			pub.publishMessage( body.toString() );
			messagesPublished++;
	        productsPublished += products.length();
			while(products.length() > 0) {
				products.remove(0);
			}
		}
	}

	private void processLine(String[] values) {
		recordNumber++;
		if(recordNumber == 1) {
			return;
		}
		if(values.length == 0)
			return;
		org.json.JSONObject variant = new org.json.JSONObject();
		if(prevProdId == null) {
			currentProduct = new org.json.JSONObject();
			currentProduct.put("proposalId", values[0]);
			currentProduct.put("template", values[1]);
			currentProduct.put("Business", values[2]);
			currentProduct.put("SKU", values[3]);
			currentProduct.put("MainBarCode", values[4]);
			currentProduct.put("currentStatus", values[5]);
			currentProduct.put("previousStatus", values[6]);
			currentProduct.put("externalStatus", values[7]);
			variants = new org.json.JSONArray();
			currentProduct.put("variants", variants);
			variants.put(variant);
			variant.put("variantId", values[8]);
			variant.put("SKU", values[9]);
			variant.put("MainBarCode", values.length == 11 ? values[10] : "");
			variant.put("currentStatus", values[5]);
			variant.put("previousStatus", values[6]);
			variant.put("externalStatus", values[7]);
			products.put(currentProduct);
		}else if(!prevProdId.equals(values[0])) {
			if(batchSize == products.length()) {
				sendData();
			}
			currentProduct = new org.json.JSONObject();
			currentProduct.put("proposalId", values[0]);
			currentProduct.put("template", values[1]);
			currentProduct.put("Business", values[2]);
			currentProduct.put("SKU", values[3]);
			currentProduct.put("MainBarCode", values[4]);
			currentProduct.put("currentStatus", values[5]);
			currentProduct.put("previousStatus", values[6]);
			currentProduct.put("externalStatus", values[7]);
			variants = new org.json.JSONArray();
			currentProduct.put("variants", variants);
			variants.put(variant);
			variant.put("variantId", values[8]);
			variant.put("SKU", values[9]);
			variant.put("MainBarCode", values.length == 11 ? values[10] : "");
			variant.put("currentStatus", values[5]);
			variant.put("previousStatus", values[6]);
			variant.put("externalStatus", values[7]);
			products.put(currentProduct);
		}else {
			variants.put(variant);
			variant.put("variantId", values[8]);
			variant.put("SKU", values[9]);
			variant.put("MainBarCode", values.length == 11 ? values[10] : "");
			variant.put("currentStatus", values[5]);
			variant.put("previousStatus", values[6]);
			variant.put("externalStatus", values[7]);
		}
		prevProdId = values[0];
		rowsRead++;
		
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

	public static void main(String[] args) {
		args = new String[] { "C:\\opt\\LVP\\desorden\\PROD\\sqlrunner_PIM_MASTER_20260616_133243.csv" };
//		args = new String[] { "C:\\opt\\LVP\\desorden\\PROD\\sqlrunner_PIM_MASTER_20260612_105741.csv" };

		java.nio.file.Path csvPath = java.nio.file.Paths.get(args[0]);
		int batchSize = 500;

		new EnviaStatusPorPubSubPOST(batchSize).process(csvPath);
	}
}