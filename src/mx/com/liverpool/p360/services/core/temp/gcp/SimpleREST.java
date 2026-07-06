package mx.com.liverpool.p360.services.core.temp.gcp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;

import mx.com.liverpool.p360.services.core.PropertiesManager;

public class SimpleREST {
	public static void main(String[] args) throws IOException {
		while(true) {
	        try {
	            // Determina ruta absoluta del archivo dev.json en base al cwd
	            String path = Paths.get(
	            		/* args[0] */ 
	            		"C:\\opt\\LVP\\tmp\\dev.json"
	            		).toAbsolutePath().toString();
	            File credentialsFile = new File(path);
	
	            // Transporte HTTP
	            HttpTransport transport = new NetHttpTransport();
	
	            // Carga credenciales y crea ID token (para Cloud Run personalizado)
	            IdTokenCredentials credentials = IdTokenCredentials.newBuilder()
	                .setIdTokenProvider((IdTokenProvider) GoogleCredentials.fromStream(new FileInputStream(PropertiesManager.get("p360.contingency.gcp.ia_itemgroup_sa"))))
	                .setTargetAudience(PropertiesManager.get("p360.contingency.gcp.ia_itemgroup_url"))
	                .build();
	
//	            credentials.refresh();
//	            String idToken = credentials.getAccessToken().getTokenValue();
	
	            // Construye URL destino
	            GenericUrl url = new GenericUrl(PropertiesManager.get("p360.contingency.gcp.ia_itemgroup_url_ta"));
	            org.json.JSONObject body = new org.json.JSONObject().put("input", new org.json.JSONArray().put( new org.json.JSONObject()
	            		.put("pim_product_name", "PANTALLA SONY OLED  de   8K Television  con  SAPHI")
	            		.put("pim_template_id", "EU4-113578")
	            		.put("product_type_sap", "5190")
	            		.put("product_description", "PANTALLA SONY OLED  de   8K Television  con  SAPHI")
	            		.put("image", "")));
	            HttpContent content = new ByteArrayContent("application/json", 
	            		body.toString().getBytes());
	            // Construye request
	            HttpRequestFactory requestFactory = transport.createRequestFactory();
	            HttpRequest request = requestFactory.buildPostRequest(url, content);
//	            request.getHeaders().setAuthorization("Bearer " + idToken);
	
	            // Timeouts opcionales
	            request.setConnectTimeout(100);
	            request.setReadTimeout(0);
	
	            // Ejecuta la petición
	            HttpResponse response = request.execute();
	            System.out.println("Response status: " + response.getStatusCode());
	            System.out.println("Response body: " + response.parseAsString());
	
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        try {
	        	System.out.println("Sleeping...");
	        	Thread.sleep(30000);
	        }catch(InterruptedException e) {
	        	e.printStackTrace();
	        }
		}
    }
}
