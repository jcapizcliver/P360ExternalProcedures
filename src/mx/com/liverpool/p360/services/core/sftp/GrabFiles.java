package mx.com.liverpool.p360.services.core.sftp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.SimpleLog;

public class GrabFiles implements SimpleLog {

	// SFTP connection parameters
	private static final String HOST = PropertiesManager.get( "p360.contingency.ecc.host" );// SFTP server address: 172.16.204.243
	private static final int PORT = Integer.parseInt(PropertiesManager.get( "p360.contingency.ecc.port" ));// SFTP server port: 22
	private static final String USER = PropertiesManager.get( "p360.contingency.ecc.userp360" ); //username: userp360 SFTP 
	private static final Path PRIVATE_KEY_PATH = Paths.get(PropertiesManager.get( "p360.contingency.ecc.private_key_path" ));// Path to private key: /home/P360admin/.ssh/id_rsa 
	private static final String REMOTE_DIR = PropertiesManager.get( "p360.contingency.ecc.remote_directory_122_holder" );//Remote directory to monitor: /interfase/mer/in/step/P360/zrtuab122
	private static final Path LOCAL_PROCESSED_DIR = Paths.get(PropertiesManager.get( "p360.contingency.ecc.local_processed_dir_122_holder" ));//Path: /u01/stage/ecc.122/processed
//	private static final Path STATE_FILE = Paths.get(PropertiesManager.get( "p360.contingency.ecc.state_file_122" ));//File: processed_ecc.122.properties
//	private static boolean USE_CACHE =Boolean.parseBoolean(PropertiesManager.get( "p360.contingency.ecc.use_cache" ));//false;

    public static void main(String[] args) {
    	GrabFiles object = new GrabFiles();
    	try {
			object.runOnSftp();
		} catch (ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
    }
    

    static long eventTimeMillis(DirEntry e) {
        String name = e.getFilename();
        long t = extractTsFromNameMillis(name);
        if (t >= 0) return t;
        try {
            var attrs = e.getAttributes();
            if (attrs != null && attrs.getModifyTime() != null) {
                return attrs.getModifyTime().toMillis();
            }
        } catch (Exception ignore) {}
        return Long.MAX_VALUE;
    }

    static long extractTsFromNameMillis(String name) {
        var m = java.util.regex.Pattern
            .compile("(\\d{8}_\\d{6})|(\\d{14})")
            .matcher(name);

        if (!m.find()) return -1L;

        String ts14 = (m.group(1) != null) ? m.group(1).replace("_", "") : m.group(2); // yyyyMMddHHmmss

        var ldt = java.time.LocalDateTime.parse(
            ts14,
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
        );

        return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
    
	public void runOnSftp() throws ParserConfigurationException, SAXException {

		log("Starting...");
		 try (SshClient client = SshClient.setUpDefaultClient()) {
	            client.setKeyIdentityProvider(new FileKeyPairProvider(PRIVATE_KEY_PATH));
	            client.start();
	                try (ClientSession session = client.connect(USER, HOST, PORT)
	                        .verify(15, TimeUnit.SECONDS)
	                        .getSession()) {

	                    session.auth().verify(15, TimeUnit.SECONDS);

	                    try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {

	                        try {
	                            Iterable<DirEntry> entriesIt = sftp.readDir(REMOTE_DIR);
	                            
	                            java.util.List<DirEntry> entries = new java.util.ArrayList<>();
	                            for (DirEntry e : entriesIt) {
	                                String name = e.getFilename();
	                                if (name.equals(".") || name.equals("..")) continue;
	                                if (!name.startsWith("GenericXMLattributes") && !name.startsWith("GenericXMLproducts")) continue;
	                                entries.add(e);
	                            }
	                            java.util.Map<Long, java.util.List<DirEntry>> byTs = new java.util.TreeMap<>();
	                            for (DirEntry e : entriesIt) {
	                                long ts = extractTsFromNameMillis(e.getFilename());
	                                if (ts < 0) ts = eventTimeMillis(e);
	                                byTs.computeIfAbsent(ts, k -> new java.util.ArrayList<>()).add(e);
	                            }

	                            for (var group : byTs.values()) {

	                                for (DirEntry ent : group) {
	                             	   String name = ent.getFilename();
	                            	   String filePath = REMOTE_DIR + "/" + name;
	                                    if (name.startsWith("GenericXMLproducts")) {
			                                try (InputStream input = sftp.read(filePath);
			                                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
		
			                                    copyStream(input, out);
		
			                                    Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
			                                    java.nio.file.Files.write(localCopy, out.toByteArray());
				
//					                            sftp.remove(filePath);
		
			                                } catch (Exception perFileError) {
			                                    logE(perFileError);
			                                    String msg = String.valueOf(perFileError.getMessage());
			                                    if (msg.contains("client is closed") || msg.contains("Channel is closed")) {
			                                        throw perFileError;
			                                    }
			                                }
	                                    	
	                                    }
	                                }
	                                for (DirEntry ent : group) {
                                    	String name = ent.getFilename();
 	                            	    String filePath = REMOTE_DIR + "/" + name;
	                                    if (name.startsWith("GenericXMLattributes")) {
 			                                try (InputStream input = sftp.read(filePath);
 			                                     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
 		
 			                                    copyStream(input, out);
 		
 			                                    Path localCopy = LOCAL_PROCESSED_DIR.resolve(name);
 			                                    java.nio.file.Files.write(localCopy, out.toByteArray());
 			                                } catch (Exception perFileError) {
 			                                    logE(perFileError);
 			                                    String msg = String.valueOf(perFileError.getMessage());
 			                                    if (msg.contains("client is closed") || msg.contains("Channel is closed")) {
 			                                        throw perFileError;
 			                                    }
 			                                }
	                                    }
	                                }
	                            }
	                        } catch (Exception sftpBroken) {
	                            log("SFTP/session se rompió; reconecto en el siguiente ciclo.");
	                            logE(sftpBroken);
	                        }
	                    }

	                } catch (Exception connectOrAuthError) {
	                    log("No se pudo conectar/auth; reintento en el siguiente ciclo.");
	                    logE(connectOrAuthError);
	                }
	        } catch (java.io.IOException e) {
				logE(e);
			}
	}

    private static void copyStream(InputStream input, ByteArrayOutputStream output) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

	@Override
	public final void log(String message) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","grabFiles.log").toString(), true)))) {
			pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
					+ "]  " + message);
		} catch (java.io.IOException e) {
		}
	}

	@Override
	public final void logE(Exception ex) {
		try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
				new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","grabFiles.log").toString(), true)))) {
			ex.printStackTrace(pw);
		} catch (java.io.IOException e) {
		}
	}
}
