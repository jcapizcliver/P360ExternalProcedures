package mx.com.liverpool.p360.services.core.temp.xml.local;

import mx.com.liverpool.dataprofiling.preparison.envioproductos.PruebaEnvioPubSubMediaAssets;
import mx.com.liverpool.p360.services.core.temp.xml.AnotherXMLHandlerClassify;
import mx.com.liverpool.p360.services.core.temp.xml.AnotherXMLHandlerLinkVariantsToProducts;

public class Pipeline {

	
	public static void main(String[] args) {
		try {
			/*
			 * 
			 */
			System.out.println("Sending lkp values...");
			AnotherXMLHandler2.sendLkpValues = true; 
			AnotherXMLHandler2.sendProduct = false; 
			AnotherXMLHandler2.main(args);
			System.out.println("Now sending products...");
			AnotherXMLHandler2.sendLkpValues = false;
			AnotherXMLHandler2.sendProduct = true;
			AnotherXMLHandler2.main(args);
			
			
			/*
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			// https://chat.googleapis.com/v1/spaces/AAAAUriYbrA/messages?key=AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI&token=d1Iz9J2WDhnywcZFg3TQBSg2U-kv0X9YWmwKDdfY_VU
			RESTWrapper rw = new RESTWrapper();
			rw.getRw().setBaseUrl("https://chat.googleapis.com/v1/spaces"); // ");
			//** Mopris 
			qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
			qp.put("token", "u-P1Me5vwfb04AoqTpZI0QCmPNR4fELWlqPgmupabSY");
			System.out.println( rw.getRw().makeRequest("POST", "/AAAAZpaMbww/messages", qp, new org.json.JSONObject().put("text", "<users/all> Alguien que le diga a Fer Carrillo porfa que ya se le pueden poner los estatus a las propuestas 😁. Proceso: " + args[1]).toString()) );
			//** Rediseño de Catalogación  
			qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
			qp.put("token", "d1Iz9J2WDhnywcZFg3TQBSg2U-kv0X9YWmwKDdfY_VU");
			System.out.println( rw.getRw().makeRequest("POST", "/AAAAUriYbrA/messages", qp, new org.json.JSONObject().put("text", "<users/all> Alguien que le diga a Fer Carrillo porfa que ya se le pueden poner los estatus a las propuestas 😁. Proceso: " + args[1]).toString()) );
			*/
			Thread t = new Thread(() -> PruebaEnvioPubSubMediaAssets.main(args));
			t.start();
			
			System.out.println("Now classify");
			AnotherXMLHandlerClassify.main(args);
			System.out.println("Now linking...");
			AnotherXMLHandlerLinkVariantsToProducts.main(args);
			System.out.println("Now sending names...");
			AnotherXMLHandlerSendNames.main(args);
			System.out.println("Now determining missing pas...");
			AnotherXMLHandlerRevisaPadresFaltantes.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
