package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import java.io.IOException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import org.json.JSONException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.amqp.CrearArchivosParaSKU;

public class ReSendToSKUCreation {

	
	public static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		try(CrearArchivosParaSKU c = new CrearArchivosParaSKU()){
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			qp.put("fields", "Product2G.ProductNo,Product2GLog.CreationDate(PIM),Product2GLog.ModificationDate(PIM)");
			qp.put("query", "Product2G.CurrentStatus = \"Creación de SKU\" and Product2GLog.CreationDate(PIM) >= 2026-01-28T11:16:00");
			rw.collectData("list", "Product2G", null, "bySearch", qp, row -> {
				org.json.JSONArray values = row.getJSONArray("values");
				System.out.println("Sending: " + values);
				String[] data;
				try {
					data = c.creacionDeArchivos(values.getString(0), (short) 0);
					try {
						c.handleContent(values.getString(0), data);
					} catch (JSONException | IOException e) {
						e.printStackTrace();
					}
				} catch (ServiceUnavailableException | JSONException e) {
					e.printStackTrace();
				}
			});
		} catch (java.io.IOException e1) {
			e1.printStackTrace();
		}
	}
	
}
