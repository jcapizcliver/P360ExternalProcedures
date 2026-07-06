package mx.com.liverpool.p360.services.core.dq;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class TituloSinMarca extends RESTDQRuleImpl{
	
	private final String productName;
	
	public TituloSinMarca(String productName) {
		this.productName = productName;
	}

	@Override
	public void processData(Map<String, JSONObject> sourceData, JSONArray records) {
		String brandName = getCharacteristicValue( sourceData.get("BrandName") );
		if(brandName == null || "".equals(brandName)) {
			brandName = getCharacteristicValue( sourceData.get("BRAND_ID_S4H") );
		}
		if(brandName != null && !"".equals(brandName) && productName != null && !productName.isEmpty()) {
			String sinMarca = removeBrandIgnoreCaseAndAccents(productName, brandName); // productName.replaceFirst("(?iu)(?<![\\p{L}])" + java.util.regex.Pattern.quote(brandName) + "(?![\\p{L}])", "").replaceAll(" {2,}", " ").trim();
			records.put(
					createCharacteristicValueObject("TituloSinMarca", sinMarca)
				);
		}
	}
	
	private String removeBrandIgnoreCaseAndAccents(String productName, String brandName) {
	    if (productName == null || brandName == null || brandName.trim().isEmpty()) {
	        return productName;
	    }

	    String normalizedProduct = java.text.Normalizer.normalize(productName, java.text.Normalizer.Form.NFC);
	    String normalizedBrand = java.text.Normalizer.normalize(brandName, java.text.Normalizer.Form.NFC);

	    return normalizedProduct
	        .replaceFirst("(?iu)(?<![\\p{L}])" + java.util.regex.Pattern.quote(normalizedBrand) + "(?![\\p{L}])", "")
	        .replaceAll(" {2,}", " ")
	        .trim();
	}

}
