package mx.com.liverpool.p360.services.core.net;

public class ProductVariantInfoAdmin {

	private static final java.util.concurrent.ConcurrentHashMap<String, java.util.Map<String, String>> data = new java.util.concurrent.ConcurrentHashMap<>();
	private boolean running = true;
	
	private class Worker implements Runnable {
		
		private boolean running = true;
		private java.util.concurrent.ArrayBlockingQueue<java.net.Socket> clientes;
		private java.util.concurrent.ConcurrentHashMap<String, java.util.Map<String, String>> data;
		
		public Worker(java.util.concurrent.ArrayBlockingQueue<java.net.Socket> clientes, java.util.concurrent.ConcurrentHashMap<String, java.util.Map<String, String>> data) {
			this.clientes = clientes;
			this.data = data;
		}
		
		@Override
		public void run() {
			java.net.Socket socket = null;
			while(running) {
				try {
					socket = clientes.poll(10, java.util.concurrent.TimeUnit.MILLISECONDS);
					if(socket != null) {
						handleConnection(socket);
					}
				}catch(java.lang.InterruptedException e) {
					
				}
			}
		}
		
		public void setRunning(boolean running) {
			this.running = running;
		}
		
		private void handleConnection(java.net.Socket socket) {
			org.json.JSONObject request = new org.json.JSONObject();
			org.json.JSONArray products = null;
			org.json.JSONObject product = null;
			org.json.JSONObject values = null;
			String id = null;
			org.json.JSONArray variants = null;
			org.json.JSONArray variant = null;
			String variantId = null;
			java.util.Map<String, String> dictionary = null;
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))){
				String message = br.readLine();
				try {
					if(request.has("action") && request.has("products")) {
						if("updateRecord".equals(request.getString("action"))) {
							products = request.getJSONArray("products");
							for(int i=0; i<products.length(); i++) {
								product = products.getJSONObject(i);
								id = product.getString("proposalId");
								dictionary = data.get(id);
								if(dictionary == null) {
									dictionary = new java.util.TreeMap<>();
									data.put(id, dictionary);
								}
								values = product.getJSONObject("values");
								for(String name : org.json.JSONObject.getNames(values)) {
									if(name != null && !name.isEmpty()) {
										dictionary.put(name, String.valueOf( values.get(name) ));
									}
								}
							}
						}else if("newRecord".equals(request.getString("action"))) {
							
						}
					}
				}catch(org.json.JSONException e) {
					e.printStackTrace();
				}
			}catch(java.io.IOException e) {
				logE(e);
			}
		}
		
	}
	
	private void startListener() {
		Thread t = new Thread() {
			@Override
			public void run() {
				while(running) {
					try(java.net.ServerSocket server = new java.net.ServerSocket(23540)){
						java.net.Socket socket = server.accept();
							
						
					}catch(java.io.IOException e) {
						logE(e);
						running = false;
						log("Stopping... " + e.getMessage());
					}
				}
			}
		};
		t.setDaemon(false);
		t.start();
	}
	
	private void handleAction(org.json.JSONObject request) {
		try {
//			org.json.JSONObject request = new org.json.JSONObject(message);
			if(request.has("action")) { 
				if( "finish".equals(request.getString("action")) ) {
					running = false;
				}else if( "wru".equals(request.getString("action")) ) {
//					try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream(), java.nio.charset.StandardCharsets.UTF_8))){
//						pw.println(this.getClass().getName());
//					}
				}
			}
		}catch(org.json.JSONException e) {
			logE(e);
		}
	}


	private void log(String message) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/information_admin.log", true)))) {
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date()))
					+ "] " + message);
		} catch (java.io.IOException e) {
		}
	}

	private void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream("../logs/information_admin.log", true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
	
}
