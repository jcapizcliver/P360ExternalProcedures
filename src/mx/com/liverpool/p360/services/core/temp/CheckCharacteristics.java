package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class CheckCharacteristics {

	public static void main(String[] args) {
		RESTWorkshop w = new RESTWorkshop();
		String rawResponse = null;
		try {
			 rawResponse = w.makeRequest("GET", "/list/Characteristic/bySearch?query=" + java.net.URLEncoder.encode("Characteristic.Category->LookupValue.Code equals \"Master Data\" and not Characteristic.CustomDataProviderId is emptu", "UTF-8"), null);

		} catch (KeyManagementException | NoSuchAlgorithmException | URISyntaxException | IOException | ServiceUnavailableException e) {
			e.printStackTrace();
		}
	}
}
