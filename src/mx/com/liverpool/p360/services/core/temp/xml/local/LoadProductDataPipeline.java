package mx.com.liverpool.p360.services.core.temp.xml.local;

import mx.com.liverpool.dataprofiling.preparison.envioproductos.PruebaEnvioPubSubMediaAssets;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.temp.xml.local.precise.AnotherXMLHandlerSecondOpinionOnSpecificProducts;

public class LoadProductDataPipeline {

	private static java.util.concurrent.ConcurrentLinkedQueue<Integer> productos = new java.util.concurrent.ConcurrentLinkedQueue<>();
	private static java.util.concurrent.ConcurrentLinkedQueue<String> paths = new java.util.concurrent.ConcurrentLinkedQueue<>();

	private static boolean running = true;
	private static int prev = 0;
	
	static {
		Thread t = new Thread(new Runnable(){
			@Override 
			public void run() {
				while(running) {
					if(!productos.isEmpty()) {
						int a = 0;
						for(Integer a0 : productos) {
							a += a0;
						}
						if(a == prev) {
							productos.clear();
							paths.clear();
							java.util.Set<String> pathsSet = new java.util.TreeSet<>( paths );
							for(String path : pathsSet)
								try {
									LoadProductDataSecondOpinionRelations.main(new String[] { path });
								} catch (Exception e) {
									e.printStackTrace();
								}
							java.util.Map<String, String> qp = new java.util.TreeMap<>();
							RESTWrapper rw = new RESTWrapper();
							rw.getRw().setBaseUrl("https://chat.googleapis.com/v1/spaces"); // ");
							qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
							qp.put("token", "H3kGU98FssCp15V7VT9s1nltZfbLxuj94WFRMOVzAs0");
							logMe( "" + rw.getRw().makeRequest("POST", "/AAAAvwSYdXo/messages", qp, 
									new org.json.JSONObject().put("text", 
											"Cadencia finalizada. Productos procesados: " + a + " 😁.").toString()) );
						}else {
							java.util.Map<String, String> qp = new java.util.TreeMap<>();
							RESTWrapper rw = new RESTWrapper();
							rw.getRw().setBaseUrl("https://chat.googleapis.com/v1/spaces"); // ");
							qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
							qp.put("token", "H3kGU98FssCp15V7VT9s1nltZfbLxuj94WFRMOVzAs0");
							logMe( "" + rw.getRw().makeRequest("POST", "/AAAAvwSYdXo/messages", qp, 
									new org.json.JSONObject().put("text", 
											"Productos procesados: " + a + " 😁.").toString()) );
							prev = a;
						}
						try {
							Thread.sleep(600000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				System.out.println("Exiting.");
			}
		});
		t.setDaemon(true);
		t.start();
	}
	
	public static void processContent(String content, String path) {
		logMe("Sending data media assets");
		PruebaEnvioPubSubMediaAssets.process(content);
		try {
			int a = AnotherXMLHandlerSecondOpinionOnSpecificProducts.processContent(content);
//			logMe("Sending second opinion");
//			int a = LoadProductDataSecondOpinion.processContent(content);
//			logMe("Sending remaining fields");
//			LoadProductDataRemainingFields.processContent(content);
//			logMe("Sending ftw");
//			LoadProductDataSecondOpinionFTW.processContent(content);
//			logMe("Sending data to admin");
//			LoadProductDataSecondOpinionSendThemToAdmin.processContent(content);
			productos.add(a);
			paths.add(path);
		} catch (Exception e) {
			logE(e);
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			RESTWrapper rw = new RESTWrapper();
			rw.getRw().setBaseUrl("https://chat.googleapis.com/v1/spaces");
			qp.put("key", "AIzaSyDdI0hCZtE6vySjMm-WEfRq3CPzqKqqsHI");
			qp.put("token", "H3kGU98FssCp15V7VT9s1nltZfbLxuj94WFRMOVzAs0");
			logMe( "" + rw.getRw().makeRequest("POST", "/AAAAvwSYdXo/messages", qp, 
					new org.json.JSONObject().put("text", 
							"🚨 El proceso falló.\nYa tronó y necesita revisión (última cuenta: " + prev + ").\nChequen logs porfa y el último error. " + e.getMessage() + " .").toString()) );
		}
	}
	
	public static void main(String[] args) {
		running = false;
//		logMe("Sending data media assets");
//		PruebaEnvioPubSubMediaAssets.main(args);
		try {
			logMe("Sending second opinion");
			LoadProductDataSecondOpinion.main(args);
			logMe("Sending remaining fields");
			LoadProductDataRemainingFields.main(args);
			logMe("Sending ftw");
			LoadProductDataSecondOpinionFTW.main(args);
			logMe("Now sending rels...");
			LoadProductDataSecondOpinionRelations.main(args);
			logMe("Sending data to admin");
			LoadProductDataSecondOpinionSendThemToAdmin.main(args);
		} catch (Exception e) {
			logE(e);
		}
	}

	
	private static void logMe(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/loadProductDataPipeline.log", true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "]  " + message);
        } catch (java.io.IOException e) {
        }
    }

    private static void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/loadProductDataPipeline.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }
}
