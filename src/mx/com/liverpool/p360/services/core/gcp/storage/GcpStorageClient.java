package mx.com.liverpool.p360.services.core.gcp.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * jobs can read, write, copy, delete, or move objects without knowing GCS HTTP
 * details.</p>
 */
public class GcpStorageClient {

    private static final String STORAGE_SCOPE = "https://www.googleapis.com/auth/devstorage.read_write";
    private static final String STORAGE_BASE_URL = "https://storage.googleapis.com/storage/v1/b/";
    private static final String UPLOAD_BASE_URL = "https://storage.googleapis.com/upload/storage/v1/b/";

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
        return executeBytes("GET", url, null, null);
    }

    public void writeBytes(String bucket, String objectName, byte[] content, String contentType) throws IOException {
        String encodedBucket = encodePathPart(bucket);
        String encodedObject = encodePathPart(objectName);
        URL url = new URL(UPLOAD_BASE_URL + encodedBucket + "/o?uploadType=media&name=" + encodedObject);
        executeBytes("POST", url, content, contentType == null ? "application/octet-stream" : contentType);
    }

    public void copyObject(String bucket, String sourceObject, String targetObject) throws IOException {
        String encodedSource = encodePathPart(sourceObject);
        String encodedTarget = encodePathPart(targetObject);
        URL url = new URL(objectUrl(bucket, sourceObject) + "/copyTo/b/" + encodePathPart(bucket) + "/o/" + encodedTarget);
        executeBytes("POST", url, new byte[0], "application/json");
    }

    public void deleteObject(String bucket, String objectName) throws IOException {
        URL url = new URL(objectUrl(bucket, objectName));
        executeBytes("DELETE", url, null, null);
    }

    public void moveObject(String bucket, String sourceObject, String targetObject) throws IOException {
        copyObject(bucket, sourceObject, targetObject);
        deleteObject(bucket, sourceObject);
    }

    private byte[] executeBytes(String method, URL url, byte[] body, String contentType) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(0);

        if (body != null) {
            connection.setDoOutput(true);
            if (contentType != null) {
                connection.setRequestProperty("Content-Type", contentType);
            }
            try (OutputStream out = connection.getOutputStream()) {
                out.write(body);
            }
        }

        int status = connection.getResponseCode();
        try (InputStream in = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream()) {
            byte[] response = readFully(in);
            if (status < 200 || status >= 300) {
                throw new IOException("GCS request failed. method=" + method + ", status=" + status + ", url=" + url
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
