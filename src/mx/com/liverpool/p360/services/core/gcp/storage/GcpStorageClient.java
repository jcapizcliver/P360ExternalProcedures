package mx.com.liverpool.p360.services.core.gcp.storage;

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
 * Minimal Google Cloud Storage client backed by the JSON API.
 *
 * <p>The project already includes google-auth, but not the full
 * google-cloud-storage client. Keep bucket transport concerns here so business
 * jobs can read objects without knowing GCS HTTP details.</p>
 */
public class GcpStorageClient {

    private static final String STORAGE_SCOPE = "https://www.googleapis.com/auth/devstorage.read_only";
    private static final String STORAGE_BASE_URL = "https://storage.googleapis.com/storage/v1/b/";

    private final String serviceAccountFile;
    private GoogleCredentials credentials;

    public GcpStorageClient(String serviceAccountFile) {
        this.serviceAccountFile = serviceAccountFile;
    }

    public InputStream read(String bucket, String objectName) throws IOException {
        return new ByteArrayInputStream(readBytes(bucket, objectName));
    }

    public byte[] readBytes(String bucket, String objectName) throws IOException {
        URL url = new URL(objectUrl(bucket, objectName) + "?alt=media");
        return executeGet(url);
    }

    private byte[] executeGet(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(0);

        int status = connection.getResponseCode();
        try (InputStream in = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream()) {
            byte[] response = readFully(in);
            if (status < 200 || status >= 300) {
                throw new IOException("GCS request failed. method=GET, status=" + status + ", url=" + url
                        + ", response=" + new String(response, StandardCharsets.UTF_8));
            }
            return response;
        } finally {
            connection.disconnect();
        }
    }

    private String objectUrl(String bucket, String objectName) throws IOException {
        return STORAGE_BASE_URL + encodePathPart(bucket) + "/o/" + encodePathPart(objectName);
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
