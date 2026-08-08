package mx.com.liverpool.p360.services.core.temp.product2g.maintenance8;

public class RenombraACodeLabelContenido {

	
	public static void main(String[] args) {
		
		
		try(
			java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile())));
			java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(args[1]).toFile())))
		){
			String line = null;
			Object o = null;
			while((line = br.readLine()) != null){
				org.json.JSONObject j = new org.json.JSONObject(line);
				o = j.remove("lista_valores");
				org.json.JSONObject lv = o == null ? null : org.json.JSONObject.NULL.equals(o) ? null : (org.json.JSONObject) o;
				if(lv != null) {
					org.json.JSONArray valores = (org.json.JSONArray) lv.remove("valores");
					for(int i=0; i<valores.length(); i++) {
						String code = (String) valores.getJSONObject(i).remove("pim_valor_codigo");
						Object label = valores.getJSONObject(i).remove("pim_valor_desc");
						valores.getJSONObject(i).put("_code", code);
						valores.getJSONObject(i).put("_label", org.json.JSONObject.NULL.equals(label) ? "" : label);
					}
					j.put("values", valores);
				}
				pw.println( j );
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
}
