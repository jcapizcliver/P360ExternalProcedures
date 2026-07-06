package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ListSKUCreationOnes extends RESTWrapper{

	private java.util.LinkedList<String> toSearch = new java.util.LinkedList<>();
	
	public static void main(String[] args) {
		ListSKUCreationOnes l = new ListSKUCreationOnes();
		l.collectData();
	}
	
	private void collectData() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Product2G.ProductNo");
		qp.put("query", "Product2G.CurrentStatus = 1020");
		qp.put("pageSize", "10000");
		collectData("list", "Product2G", null, "bySearch", qp, row -> {
			toSearch.addLast(row.getJSONArray("values").getString(0));
		});
		java.io.File[] files = java.nio.file.Paths.get("/", "u01", "stage", "ECC_122", "processed").toFile().listFiles(ff -> ff.getName().endsWith(".XML"));
		String s = null;
		boolean found = false;
		java.util.Set<String> filesToSend = new java.util.TreeSet<>();
		for(String id : toSearch) {
			for(java.io.File f : files) {
				try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines(f.toPath())){
					java.util.Iterator<String> iter = lns.iterator();
					while(iter.hasNext()) {
						s = iter.next();
						if(s.contains(id)) {
							filesToSend.add(f.getAbsolutePath());
							found = true;
							break;
						}
					}
					if(found) {
						found = false;
						break;
					}
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("Found: " + filesToSend.size() + " files to send.");
		String host = PropertiesManager.get( "p360.contingency.ecc.host" );
		int port = 22;
		String user = "userp360";
		java.nio.file.Path privateKeyPath = java.nio.file.Paths.get("/home/P360admin/.ssh/id_rsa");
		SshClient client = SshClient.setUpDefaultClient();
        client.start();
        try (ClientSession session = client.connect(user, host, port)
                .verify(10, TimeUnit.SECONDS)
                .getSession()) {
            FileKeyPairProvider keyProvider = new FileKeyPairProvider(privateKeyPath);
            keyProvider.setPasswordFinder(FilePasswordProvider.EMPTY);
            keyProvider.loadKeys(null).forEach(session::addPublicKeyIdentity);
            session.auth().verify(10, TimeUnit.SECONDS);
            StringBuilder sb = new StringBuilder();
            java.nio.file.Path fp = null;
            for(String ap : filesToSend) {
            	fp = java.nio.file.Paths.get(ap);
	            try(java.util.stream.Stream<String> lns = java.nio.file.Files.lines( fp )){
	            	lns.forEach(sb::append);
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}
	            try (SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)) {
	            	writeToSftp(sftp, sb.toString(), PropertiesManager.get("p360.contingency.ecc.remote_directory_base"), fp.toFile().getName());
	            }
	            sb.setLength(0);
            }
        } catch(java.io.IOException e) {
        	System.out.println("Could not send request: " + e.getMessage());
        	e.printStackTrace();
        } finally {
            client.stop();
        }
	}
	
	private void writeToSftp(SftpClient sftp, String content, String remoteBasePath, String fileName) throws IOException {

        String fullPath = null;
        fullPath = remoteBasePath.endsWith("/") ? remoteBasePath + fileName : remoteBasePath + "/" + fileName;
        System.out.println("Sending: " + fileName + " to " + fullPath);
        try (OutputStream os = sftp.write(fullPath)) {
        	os.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        System.out.println("sent");

    }
	
}
