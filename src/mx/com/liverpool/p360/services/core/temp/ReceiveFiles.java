package mx.com.liverpool.p360.services.core.temp;

public class ReceiveFiles {

	public static void main(String[] args) {
//		download();
		upload();
	}

	private static void upload() {
		try(java.net.Socket socket = new java.net.Socket("172.18.237.210", 1712); java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream()))){
			java.io.File[] fls = new java.io.File("D:\\tmp\\pluging_DEV").listFiles(ff -> ff.getName().endsWith(".jar"));
            for(java.io.File f : fls){
                    System.out.println("Gonna send: " + f.getName());
                    pw.println(new org.json.JSONObject().put("action", "file").put("fileName", f.getName()));
                    try(java.io.FileInputStream fis = new java.io.FileInputStream(f)){
                            int length = 0;
                            byte[] chunk = new byte[1024];
                            byte[] aux = null;
                            while((length = fis.read(chunk)) != -1){
                                    if(length < 1024){
                                            aux = new byte[length];
                                            for(int i=0; i<length; i++){
                                                    aux[i] = chunk[i];
                                            }
                                    }
                                    pw.println(new org.json.JSONObject().put("action", "send").put("content", java.util.Base64.getEncoder().encodeToString( length < 1024 ? aux : chunk )));
                            }
                    }
            }
		}catch(java.io.IOException e) { e.printStackTrace(); }
	}

	private static void download() {
		java.io.FileOutputStream fos = null;
		try(java.net.Socket socket = new java.net.Socket("172.18.237.162", 1712); java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream()))){
			String line = null;
			org.json.JSONObject msg = null;
			while((line = br.readLine()) != null) {
				msg = new org.json.JSONObject(line);
				if(msg.has("action") && "file".equals(msg.getString("action"))) {
					if(fos != null) {
						fos.close();
					}
					System.out.println("Now receiving: " + msg.getString("fileName"));
					fos = new java.io.FileOutputStream("D:\\tmp\\pluging_DEV\\" + msg.getString("fileName"));
				}else if(msg.has("action") && "send".equals( msg.getString("action") )) {
					fos.write( java.util.Base64.getDecoder().decode(msg.getString("content").getBytes()) );
				}
			}
		}catch(java.io.IOException e) { e.printStackTrace(); }
	}
}
