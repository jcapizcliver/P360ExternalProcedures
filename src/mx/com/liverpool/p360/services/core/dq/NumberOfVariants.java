package mx.com.liverpool.p360.services.core.dq;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class NumberOfVariants extends RESTDQRuleImpl {
	
	private final String productNo;
	
	public NumberOfVariants(String productNo) {
		this.productNo = productNo;
	}

	public static void main(String[] args) {
		String productNo = "1754611649402823";
		org.json.JSONArray characteristicRecords = new org.json.JSONArray();
		NumberOfVariants nov = new NumberOfVariants(productNo);
		nov.processData(null, characteristicRecords);
	}
	
	@Override
	public void processData(Map<String, JSONObject> sourceData, JSONArray records) {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("products", "'" + productNo + "'@1");
		int[] data = new int[1];
		data[0] = 0;
		rw.collectData("list", "Article", null, "byProducts", qp, row -> {
			data[0]++;
		});

		records.put( createCharacteristicValueObject("ZNUMV", data[0]) );
	}

}
