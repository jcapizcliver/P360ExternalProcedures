package mx.com.liverpool.p360.services.core.temp.sftp;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.amqp.CrearArchivosParaSKU;

public class ManualResendToSKUCreation {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args)  {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				      "Product2G.ProductNo"
				   + ",Product2G.CurrentStatus"
				   + ",Product2GCharacteristicValue.LookupValue('Business',root,\"0000.0000.RK\",'Business',-1)->LookupValue.Code"
				   + ",Product2GCharacteristicValueLang.Value('SKU',root,\"0000.0000.RK\",'SKU',-1)"
			);
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("").toFile()), java.nio.charset.StandardCharsets.UTF_8)); CrearArchivosParaSKU ca = new CrearArchivosParaSKU()){
			String line = null;
			StringBuilder sb = new StringBuilder();
			int cn = 0;
			while((line = br.readLine()) != null) {
				sb.append(sb.length() == 0 ? "" : ",");
				sb.append("'");
				sb.append(line);
				sb.append("'@1");
				cn++;
				if(cn % 1000 == 0) {
					qp.put("items", sb.toString());
					rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
						try {
							org.json.JSONArray values = row.getJSONArray("values");
							String externalId = values.getString(0);
							String currentInternalStatus = String.valueOf( values.get(1) );
							String business = values.getJSONArray(2).getString(0);
							String sku = values.getJSONArray(3).getString(0);
							if("1020".equals( String.valueOf(currentInternalStatus) )) {
								try {
									if("MKP".equals(business)) {
										if(!"".equals(sku)) {
											String[] contenido = ca.creacionDeArchivos(externalId, (short) 0); // CREA
											ca.handleContent(externalId, contenido);
											
											contenido = ca.creacionDeArchivos(externalId, (short) 1); // MODIF
											ca.handleContent(externalId, contenido);
										}else {
											String[] contenido = ca.creacionDeArchivos(externalId, (short) 0); // CREA
											ca.handleContent(externalId, contenido);
										}
									}else {
										if(!"".equals(sku)) {
											String[] contenido = ca.creacionDeArchivos(externalId, (short) 1); // MODIF
											ca.handleContent(externalId, contenido);
										}else {
											String[] contenido = ca.creacionDeArchivos(externalId, (short) 0); // CREA
											ca.handleContent(externalId, contenido);
										}
									}
								} catch (java.io.IOException e) {
									e.printStackTrace();
								}
							}
						}catch(ServiceUnavailableException e) {
							e.printStackTrace();
						}
					});
					sb.setLength(0);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
