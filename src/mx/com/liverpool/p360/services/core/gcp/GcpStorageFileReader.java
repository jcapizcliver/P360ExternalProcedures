package mx.com.liverpool.p360.services.core.gcp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

/**
 * Reads objects from Google Cloud Storage using the service account JSON configured
 * for the bucket listener.
 *
 * <p>This project does not currently include the google-cloud-storage client JAR,
 * so this class uses the GCS JSON API over HTTP and reuses the existing
 * google-auth dependency already present in Memelos. The caller provides the
 * bucket and object name received from the Pub/Sub bucket notification.</p>
 *
 * <p>Required IAM permission for the service account: read access to the target
 * bucket/object, normally through roles/storage.objectViewer.</p>
 */
public class GcpStorageFileReader {

    private static final String STORAGE_SCOPE = "https://www.googleapis.com/auth/devstorage.read_only";

    private final String serviceAccountFile;
    private GoogleCredentials credentials;

    public GcpStorageFileReader(String serviceAccountFile) {
        this.serviceAccountFile = serviceAccountFile;
    }

    /**
     * Downloads a GCS object and returns it as an InputStream.
     *
     * <p>The content is buffered in memory because the current first processing
     * step only inspects a small portion of the file. If this flow starts
     * handling large production files, prefer streaming directly from the
     * connection instead of returning a ByteArrayInputStream.</p>
     */
    public InputStream read(String bucket, String objectName) throws IOException {
        byte[] content = readBytes(bucket, objectName);
        return new ByteArrayInputStream(content);
    }

    /**
     * Downloads the full object content. Any non-2xx response is converted into
     * an IOException with the GCS response body to simplify listener retry logic.
     */
    public byte[] readBytes(String bucket, String objectName) throws IOException {
        String encodedBucket = encodePathPart(bucket);
        String encodedObject = encodePathPart(objectName);
        URL url = new URL("https://storage.googleapis.com/storage/v1/b/" + encodedBucket + "/o/" + encodedObject + "?alt=media");

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(0);

        int status = connection.getResponseCode();
        try (InputStream in = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream()) {
            byte[] response = readFully(in);
            if (status < 200 || status >= 300) {
                throw new IOException("GCS read failed. status=" + status + ", bucket=" + bucket + ", object=" + objectName
                        + ", response=" + new String(response, StandardCharsets.UTF_8));
            }
            return response;
        } finally {
            connection.disconnect();
        }
    }

    private String getAccessToken() throws IOException {
        GoogleCredentials currentCredentials = getOrLoadCredentials();
        currentCredentials.refreshIfExpired();
        AccessToken token = currentCredentials.getAccessToken();
        if (token == null) {
            currentCredentials.refresh();
            token = currentCredentials.getAccessToken();
        }
        return token.getTokenValue();
    }

    private GoogleCredentials getOrLoadCredentials() throws IOException {
        if (credentials != null) {
            return credentials;
        }
        synchronized (this) {
            if (credentials != null) {
                return credentials;
            }
            try (FileInputStream in = new FileInputStream(serviceAccountFile)) {
                credentials = GoogleCredentials.fromStream(in).createScoped(STORAGE_SCOPE);
            }
            return credentials;
        }
    }

    private static String encodePathPart(String value) throws IOException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
    }

    private static byte[] readFully(InputStream in) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }
}
