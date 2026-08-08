package mx.com.liverpool.p360.services.core.gcp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;

/**
 * Resolves Google credentials from ADC and optionally impersonates a target
 * service account.
 */
public class GcpCredentialsProvider {

    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    private static final int IMPERSONATED_TOKEN_LIFETIME_SECONDS = 3600;

    private final String impersonateServiceAccount;
    private final List<String> scopes;
    private final Logger logger;

    private GoogleCredentials credentials;

    public GcpCredentialsProvider(Collection<String> scopes, Logger logger) {
        this(null, scopes, logger);
    }

    public GcpCredentialsProvider(String impersonateServiceAccount, Collection<String> scopes, Logger logger) {
        this.impersonateServiceAccount = normalize(impersonateServiceAccount);
        this.scopes = normalizeScopes(scopes);
        this.logger = logger;
    }

    public GoogleCredentials getCredentials() throws IOException {
        if (credentials != null) {
            return credentials;
        }
        synchronized (this) {
            if (credentials != null) {
                return credentials;
            }

            GoogleCredentials sourceCredentials = GoogleCredentials.getApplicationDefault().createScoped(CLOUD_PLATFORM_SCOPE);
            String metadataServiceAccount = resolveMetadataServiceAccountEmail();
            logInfo("Loaded Google Application Default Credentials. type="
                    + sourceCredentials.getClass().getName()
                    + ", authType=" + safe(sourceCredentials.getAuthenticationType())
                    + ", metadataServiceAccount=" + safe(metadataServiceAccount));

            if (impersonateServiceAccount == null) {
                credentials = sourceCredentials.createScoped(scopes);
                logInfo("Using ADC credentials for GCP requests. scopes=" + scopes);
            } else {
                credentials = ImpersonatedCredentials.create(
                        sourceCredentials,
                        impersonateServiceAccount,
                        null,
                        scopes,
                        IMPERSONATED_TOKEN_LIFETIME_SECONDS);
                logInfo("Using impersonated credentials for GCP requests. targetServiceAccount="
                        + impersonateServiceAccount + ", scopes=" + scopes);
            }
            return credentials;
        }
    }

    public String getAccessToken() throws IOException {
        GoogleCredentials currentCredentials = getCredentials();
        currentCredentials.refreshIfExpired();
        AccessToken token = currentCredentials.getAccessToken();
        if (token == null) {
            currentCredentials.refresh();
            token = currentCredentials.getAccessToken();
        }
        return token.getTokenValue();
    }

    private static String resolveMetadataServiceAccountEmail() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL("http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/email");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Metadata-Flavor", "Google");
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            if (connection.getResponseCode() != 200) {
                return "";
            }
            return new String(readFully(connection.getInputStream()), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            return "";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
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

    private static List<String> normalizeScopes(Collection<String> scopes) {
        List<String> normalizedScopes = new ArrayList<>();
        if (scopes != null) {
            for (String scope : scopes) {
                String normalized = normalize(scope);
                if (normalized != null) {
                    normalizedScopes.add(normalized);
                }
            }
        }
        if (normalizedScopes.isEmpty()) {
            normalizedScopes.add(CLOUD_PLATFORM_SCOPE);
        }
        return normalizedScopes;
    }

    private void logInfo(String message) {
        if (logger != null) {
            logger.info(message);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        return value.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
