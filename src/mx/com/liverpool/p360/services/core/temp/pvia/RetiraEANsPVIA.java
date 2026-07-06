package mx.com.liverpool.p360.services.core.temp.pvia;

import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class RetiraEANsPVIA {

	
	public static void main(String[] args) {
		java.util.List<String> eans = new java.util.ArrayList<>( java.util.Arrays.asList(args) );
		if(eans.size() > 1) {
			String entity = eans.remove(0);
			org.json.JSONArray items = new org.json.JSONArray();
			eans.forEach(items::put);
			DataRequestor dr = new DataRequestor();
			if("product".equals(entity.toLowerCase())) {
				dr.retiraEANProductNo(items);
			}else if("article".equals(entity.toLowerCase())) {
				dr.retiraEANSupplierAID(items);
			}else {
				System.out.println("No known entity specified, need to be product or article");
			}
		}else {
			System.out.println("Número de parámetros incorrecto, se esperaba: <Entity> [<EAN>]*");
		}
	}
}
