package mx.com.liverpool.p360.services.core.temp.product2g.maintenance6;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class AlForo40Manual {
	
	
	private static final RESTWrapper rw = new RESTWrapper();
	
	private static final String USER_ECC = PropertiesManager.get( "p360.contingency.ecc.userp360" ); //username: userp360 SFTP 
	private static final String HOST_ECC = PropertiesManager.get( "p360.contingency.ecc.host" );// SFTP server address: 172.16.204.243
	private static final int PORT_ECC = Integer.parseInt(PropertiesManager.get( "p360.contingency.ecc.port" ));// SFTP server port: 22
	private static final Path PRIVATE_KEY_PATH_ECC = Paths.get(PropertiesManager.get( "p360.contingency.ecc.private_key_path" ));// Path to private key: /home/P360admin/.ssh/id_rsa 
	private static final String REMOTE_DIR_ECC = PropertiesManager.get( "p360.contingency.ecc.remote_directory_f40" );//Remote directory to monitor: /interfase/mer/in/step/P360/zrtuab122
	
	private static final String USER_S4H = PropertiesManager.get( "p360.contingency.s4h.userp360" ); 
	private static final String HOST_S4H = PropertiesManager.get( "p360.contingency.s4h.host" );
	private static final int PORT_S4H = Integer.parseInt(PropertiesManager.get( "p360.contingency.s4h.port" ));
	private static final Path PRIVATE_KEY_PATH_S4H = Paths.get(PropertiesManager.get( "p360.contingency.s4h.private_key_path" )); 
	private static final String REMOTE_DIR_S4H = PropertiesManager.get( "p360.contingency.s4h.remote_directory_f40" );
	
	private static final void log(String message) {
		System.out.println(message);
	}
	
	public static void main(String[] args) throws IOException {
		if(args.length > 0) {
			String currentStatusNew = args[0];
			for(int i=1; i<args.length; i++) {
				yeah(args[i], currentStatusNew);
			}
		}
	}
	
	private static final void yeah(String externalId, String currentStatusNew) throws IOException {
		String sku = null;
		String business = null;
		String rsp = null;
		DataRequestor dr = new DataRequestor();
			log("(tf40) entramos a \"nos vamos a foro\"");
			rsp = dr.getProductData(new org.json.JSONArray().put(externalId));
			org.json.JSONObject jr = new org.json.JSONObject(rsp);
			org.json.JSONArray items = jr.getJSONArray("items");
			org.json.JSONObject j0 = items.getJSONObject(0);
			business = j0.getString("Business");
			log("(tf40) negocio: " + business);
			java.util.List<String> vars = java.util.Arrays.asList( dr.getVariants(externalId).toArray(new String[] {}) );
			if("LVP".equals(business)) {
				if("00".equals(j0.getString("SAPObjectType"))) {
					sku = j0.getString("SKU");
					if("".equals(sku)) {
						rsp = dr.getArticleData(new org.json.JSONArray().put(vars.get(0)));
						jr = new org.json.JSONObject(rsp);
						items = jr.getJSONArray("items");
						j0 = items.getJSONObject(0);
						sku = j0.getString("SKU");
					}
					log("(tf40) Soy individual.");
					if(!"".equals(sku)) {
						java.util.Map<String, String> qp00 = new java.util.HashMap<>();
						qp00.put("includeLabels", "true");
						qp00.put("includeIds", "true");
						qp00.put("entityFilter", "Product2GCharacteristicValue");
						qp00.put("qualificationFilter", "characteristic(SistemaOrigen)");
						org.json.JSONObject jResp = rw.getRw().makeRequest("GET", "/object/Product2G/'" + externalId + "'@1", qp00, null);
						org.json.JSONObject jd = jResp.getJSONObject("_data");
						String sistemaOrigen = jd.has("_characteristicRecords") ? jd.getJSONArray("_characteristicRecords").getJSONObject(0).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code") : "1";
						log("(tf40) SistemaOrigen: " + sistemaOrigen);
						Object[] objs = getClientToECC();
						try(SshClient cli = (SshClient)objs[0]; SftpClient sftpCli = (SftpClient)objs[1]){
							writeToSftp(sftpCli, "Código SKU|ESTADO|ORIGEN\n" + sku + "|" + ( "1007".equals(currentStatusNew) ? 1 : 3) + "|" + (sistemaOrigen == null || "".equals(sistemaOrigen) ? "1" : sistemaOrigen), REMOTE_DIR_ECC);
						}catch(java.io.IOException e) {
				        	log("No fue posible escribir a foro 40 " + externalId);
				        }
					}else {
						log("(tf40) Estaba vacío, por eso no se fue a foro 40");
					}
				}else {
					log("(tf40) Soy " + j0.getString("SAPObjectType"));
					sku = j0.getString("SKU");
					if(!"".equals(sku)) {
						java.util.Map<String, String> qp00 = new java.util.HashMap<>();
						qp00.put("includeLabels", "true");
						qp00.put("includeIds", "true");
						qp00.put("entityFilter", "Product2GCharacteristicValue");
						qp00.put("qualificationFilter", "characteristic(SistemaOrigen)");
						org.json.JSONObject jResp = rw.getRw().makeRequest("GET", "/object/Product2G/'" + externalId + "'@1", qp00, null);
						if(jResp == null) {
							log("Problem querying server: " + rw.getRw().getRawResponse());
						}else {
							org.json.JSONObject jd = jResp.getJSONObject("_data");
							String sistemaOrigen = jd.has("_characteristicRecords") ? jd.getJSONArray("_characteristicRecords").getJSONObject(0).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code") : "1";
							log("(tf40) SistemaOrigne: " + sistemaOrigen);
							org.json.JSONArray jvars = new org.json.JSONArray();
							for(int i=0; i<vars.size(); i++) {
								jvars.put(vars.get(i));
							}
							rsp = dr.getArticleData(jvars);
							jr = new org.json.JSONObject(rsp);
							items = jr.getJSONArray("items");
							Object[] objs = getClientToECC();
							try(SshClient cli = (SshClient)objs[0]; SftpClient sftpCli = (SftpClient)objs[1]){
								StringBuilder ssb = new StringBuilder();
								for(int i=0; i<items.length(); i++) {
									j0 = items.getJSONObject(i);
									sku = j0.getString("SKU");
									ssb.append(ssb.length() == 0 ? "" : "\n").append( sku + "|" + ( "1007".equals(currentStatusNew) ? 1 : 3) + "|" + (sistemaOrigen == null || "".equals(sistemaOrigen) ? "1" : sistemaOrigen) );
								}
								writeToSftp(sftpCli, "Código SKU|ESTADO|ORIGEN\n" + ssb.toString(), REMOTE_DIR_ECC);
							}catch(java.io.IOException e) {
					        	log("No fue posible escribir a foro 40 " + externalId);
					        }
						}
					}else {
						log("(tf40) Estaba vacío, por eso no se fue a foro 40");
					}
				}
			}
		if("SBB".equals(business)) {
			
			jr = new org.json.JSONObject(rsp);
			items = jr.getJSONArray("items");
			j0 = items.getJSONObject(0);
			business = j0.getString("Business");
			String fotoTomadaLiverpool = j0.getString("FotoTomadaLiverpool");
			vars = java.util.Arrays.asList( dr.getVariants(externalId).toArray(new String[] {}) );
			log("(tf40) SBB: " + j0);
			if("00".equals(j0.getString("SAPObjectType"))) {
				sku = j0.getString("SKU");
				if("".equals(sku)) {
					rsp = dr.getArticleData(new org.json.JSONArray().put(vars.get(0)));
					jr = new org.json.JSONObject(rsp);
					items = jr.getJSONArray("items");
					j0 = items.getJSONObject(0);
					sku = j0.getString("SKU");
				}
				if(!"".equals(sku)) {
					java.util.Map<String, String> qp00 = new java.util.HashMap<>();
					qp00.put("includeLabels", "true");
					qp00.put("includeIds", "true");
					qp00.put("entityFilter", "Product2G");
					org.json.JSONObject jResp = rw.getRw().makeRequest("GET", "/object/Product2G/'" + externalId + "'@1", qp00, null);
					org.json.JSONObject jd = jResp.getJSONObject("_data");
					String fda = jd.has("firstDateApproved") ? jd.getString("firstDateApproved").replace("T", " ") : "1007".equals(currentStatusNew) ? new java.util.Date().toInstant().atZone(java.time.ZoneId.systemDefault()).format( java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") ) : "" ;
					Object[] objs = getClientToS4H();
					try(SshClient cli = (SshClient)objs[0]; SftpClient sftpCli = (SftpClient)objs[1]){
						if("1007".equals(currentStatusNew)) {
							writeToSftp(sftpCli, "Código SKU,Estado,Responsabilidad de fotos,Fecha Primera Aprobación\n" +  sku + "," + 1 + "," + fotoTomadaLiverpool + "," + fda, REMOTE_DIR_S4H);
						}else {
							if("Y".equals(fotoTomadaLiverpool)) {
								writeToSftp(sftpCli, "Código SKU,Estado,Responsabilidad de fotos,Fecha Primera Aprobación\n" +  sku + "," + 3 + "," + fotoTomadaLiverpool + "," + fda, REMOTE_DIR_S4H);
							}else {
								writeToSftp(sftpCli, "Código SKU,Estado,Responsabilidad de fotos,Fecha Primera Aprobación\n" +  sku + "," + 2 + "," + "N" + "," + fda, REMOTE_DIR_S4H);
							}
						}
					}catch(java.io.IOException e) {
			        	log("No fue posible escribir a foro 40 " + externalId);
			        }
				}else {
					log("(tf40) Estaba vacío, por eso no se fue a foro 40");
				}
			}else {
				sku = j0.getString("SKU");
				if(!"".equals(sku)) {
					java.util.Map<String, String> qp00 = new java.util.HashMap<>();
					qp00.put("includeLabels", "true");
					qp00.put("includeIds", "true");
					qp00.put("entityFilter", "Product2G");
					org.json.JSONObject jResp = rw.getRw().makeRequest("GET", "/object/Product2G/'" + externalId + "'@1", qp00, null);
					org.json.JSONObject jd = jResp.getJSONObject("_data");
					String fda = jd.has("firstDateApproved") ? jd.getString("firstDateApproved").replace("T", " ") : "1007".equals(currentStatusNew) ? new java.util.Date().toInstant().atZone(java.time.ZoneId.systemDefault()).format( java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") ) : "";
					org.json.JSONArray jvars = new org.json.JSONArray();
					for(int i=0; i<vars.size(); i++) {
						jvars.put(vars.get(i));
					}
					rsp = dr.getArticleData(jvars);
					jr = new org.json.JSONObject(rsp);
					items = jr.getJSONArray("items");
					log("(tf40) About to send to sbb");
					Object[] objs = getClientToS4H();
					StringBuilder sb = new StringBuilder();
					sb.append("Código SKU,Estado,Responsabilidad de fotos,Fecha Primera Aprobación\n");
					try(SshClient cli = (SshClient)objs[0]; SftpClient sftpCli = (SftpClient)objs[1]){
						for(int i=0; i<items.length(); i++) {
							j0 = items.getJSONObject(i);
							sku = j0.getString("SKU");
							if("1007".equals(currentStatusNew)) {
								sb.append(sku + "," + 1 + "," + fotoTomadaLiverpool + "," + fda).append("\n");
							}else {
								if("Y".equals(fotoTomadaLiverpool)) {
									sb.append(sku + "," + 3 + "," + fotoTomadaLiverpool + "," + fda).append("\n");
								}else {
									sb.append(sku + "," + 2 + "," + "N" + "," + fda);
								}
							}
						}
						writeToSftp(sftpCli, sb.toString(), REMOTE_DIR_S4H);
					}catch(java.io.IOException e) {
			        	log("No fue posible escribir a foro 40 " + externalId);
			        	e.printStackTrace();
			        }
				}else {
					log("Estaba vacío, por eso no se fue a foro 40");
				}
			}
			
			
		}
	
	}
	

	private static final String filePrefix = "STEP_SKU";

    private static final String writeToSftp(SftpClient sftp, String content, String remoteBasePath) throws IOException {

    	LocalDateTime now = LocalDateTime.now();
        String dateKey = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")); // yyyyMMdd
        String fileName = null;
        String fullPath = null;
        log("(tf40) Now generating files... -->" + filePrefix + "<--");
        fileName = String.format( filePrefix +  "%s.txt", dateKey);
        log("(tf40) First path: " + fileName);
        fullPath = remoteBasePath.endsWith("/") ? remoteBasePath + fileName : remoteBasePath + "/" + fileName;
        log("(tf40) Writing: " + fullPath);
        OutputStream os = sftp.write(fullPath);
    	log("(tf40) Writing out: " + fullPath);
        os.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        os.flush();
        os.close();
        log("(tf40) LOG:: WROTE.");
        log("(tf40) content: " + content + "\n./Content.");

        return fullPath;
    }
	
	private static final Object[] getClientToECC() throws IOException {
		SshClient client = SshClient.setUpDefaultClient();
        client.setKeyIdentityProvider(new FileKeyPairProvider(PRIVATE_KEY_PATH_ECC));
        client.start();
        ClientSession session = client.connect(USER_ECC, HOST_ECC, PORT_ECC)
            .verify(15, TimeUnit.SECONDS)
            .getSession();
        session.auth().verify(15, TimeUnit.SECONDS);
        SftpClient sftp = SftpClientFactory.instance().createSftpClient(session);
        return new Object[] {client, sftp};
	}
	
	private static final Object[] getClientToS4H() throws IOException {
		SshClient client = SshClient.setUpDefaultClient();
		client.setKeyIdentityProvider(new FileKeyPairProvider(PRIVATE_KEY_PATH_S4H));
		client.start();
		ClientSession session = client.connect(USER_S4H, HOST_S4H, PORT_S4H)
				.verify(15, TimeUnit.SECONDS)
				.getSession();
		session.auth().verify(15, TimeUnit.SECONDS);
		SftpClient sftp = SftpClientFactory.instance().createSftpClient(session);
		return new Object[] { client, sftp };
	}

}
