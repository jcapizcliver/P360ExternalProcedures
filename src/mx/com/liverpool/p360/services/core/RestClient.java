package mx.com.liverpool.p360.services.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class RestClient {

    private static final Semaphore OUTBOUND_LIMIT = new Semaphore(16000, true);

    private static final int ACQUIRE_TIMEOUT_SECONDS = 3;
    private static final int CONNECT_TIMEOUT_MILLIS = 15000;
    private static final int READ_TIMEOUT_MILLIS = 600000;
    
    private static final java.util.concurrent.ConcurrentMap<Long, EndpointTrace> WAITING_ENDPOINTS =
            new java.util.concurrent.ConcurrentHashMap<Long, EndpointTrace>();

    private static final java.util.concurrent.ConcurrentMap<Long, EndpointTrace> RUNNING_ENDPOINTS =
            new java.util.concurrent.ConcurrentHashMap<Long, EndpointTrace>();

    private static final java.util.concurrent.atomic.AtomicLong REQ_SEQ =
            new java.util.concurrent.atomic.AtomicLong(0L);

    private static volatile boolean sslValidationDisabled = false;

    private final Map<String, String> header = new HashMap<String, String>();

    public RestClient() {
    }

    public RestClient(String... headers) {
        String[] headerPair = null;
        for (String header : headers) {
            headerPair = header.split(":", 2);
            if (headerPair.length == 2) {
                this.header.put(headerPair[0].trim(), headerPair[1].trim());
            }
        }
    }

    public Map<String, String> getHeader() {
        return header;
    }

    public String getRequest(String method, String url, String payload)
            throws IOException, ServiceUnavailableException {
        return getRequest(method, url, payload, header);
    }

    public String getRequest(String method, String url, String payload, Map<String, String> header)
            throws IOException, ServiceUnavailableException {

        boolean acquired = false;
        HttpURLConnection con = null;

        long startMillis = System.currentTimeMillis();
        long connectStartMillis = 0L;
        long connectEndMillis = 0L;

        int permitsBefore = OUTBOUND_LIMIT.availablePermits();

        long reqId = REQ_SEQ.incrementAndGet();
        EndpointTrace trace = new EndpointTrace(reqId, method, url);
        WAITING_ENDPOINTS.put(reqId, trace);
        dumpOutboundState("OUTBOUND_ARRIVED", trace);

        try {
            acquired = OUTBOUND_LIMIT.tryAcquire(ACQUIRE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!acquired) {
                dumpOutboundState("OUTBOUND_REJECTED_STATE", trace);

                log("OUTBOUND_REJECTED",
                        "method=" + method
                                + " url=" + url
                                + " permitsBefore=" + permitsBefore
                                + " permitsNow=" + OUTBOUND_LIMIT.availablePermits()
                                + " waitSeconds=" + ACQUIRE_TIMEOUT_SECONDS
                                + " thread=" + Thread.currentThread().getName());

                throw new ServiceUnavailableException("Outbound HTTP saturated");
            }

            WAITING_ENDPOINTS.remove(reqId);
            RUNNING_ENDPOINTS.put(reqId, trace);
            dumpOutboundState("OUTBOUND_STARTED", trace);

            ensureSslValidationDisabled();

            URL obj = new URI(url).toURL();
            con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod(method);
            con.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            con.setReadTimeout(READ_TIMEOUT_MILLIS);
            if (header != null) {
                for (Map.Entry<String, String> entry : header.entrySet()) {
                    con.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            if (payload != null && !payload.isEmpty()) {
                con.setDoOutput(true);
                byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
                con.setRequestProperty("Content-Length", String.valueOf(bytes.length));

                try (OutputStream os = con.getOutputStream()) {
                    os.write(bytes);
                    os.flush();
                }
            }

            connectStartMillis = System.currentTimeMillis();
            int responseCode = con.getResponseCode();
            connectEndMillis = System.currentTimeMillis();

            InputStream stream = (responseCode >= 200 && responseCode < 300)
                    ? con.getInputStream()
                    : con.getErrorStream();

            String responseBody = readStream(stream);

            long endMillis = System.currentTimeMillis();

            if (responseCode >= 200 && responseCode < 300) {
                log("OUTBOUND_OK",
                        "method=" + method
                                + " url=" + url
                                + " code=" + responseCode
                                + " connectMs=" + (connectEndMillis - connectStartMillis)
                                + " totalMs=" + (endMillis - startMillis)
                                + " permitsNow=" + OUTBOUND_LIMIT.availablePermits()
                                + " thread=" + Thread.currentThread().getName());

                return responseBody;
            }

            log("OUTBOUND_HTTP_ERROR",
                    "method=" + method
                            + " url=" + url
                            + " code=" + responseCode
                            + " connectMs=" + (connectEndMillis - connectStartMillis)
                            + " totalMs=" + (endMillis - startMillis)
                            + " bodySnippet=" + abbreviate(responseBody, 500)
                            + " permitsNow=" + OUTBOUND_LIMIT.availablePermits()
                            + " thread=" + Thread.currentThread().getName());

            throw new IOException("HTTP error " + responseCode + " calling " + url + ". Body: " + abbreviate(responseBody, 500));

        } catch (java.net.ConnectException e) {
            long endMillis = System.currentTimeMillis();

            log("OUTBOUND_CONNECT_EXCEPTION",
                    "method=" + method
                            + " url=" + url
                            + " message=" + safeMessage(e)
                            + " totalMs=" + (endMillis - startMillis)
                            + " permitsNow=" + OUTBOUND_LIMIT.availablePermits()
                            + " thread=" + Thread.currentThread().getName());

            throw e;

        } catch (SocketTimeoutException e) {
            long endMillis = System.currentTimeMillis();

            log("OUTBOUND_TIMEOUT",
                    "method=" + method
                            + " url=" + url
                            + " message=" + safeMessage(e)
                            + " totalMs=" + (endMillis - startMillis)
                            + " connectTimeoutMs=" + CONNECT_TIMEOUT_MILLIS
                            + " readTimeoutMs=" + READ_TIMEOUT_MILLIS
                            + " permitsNow=" + OUTBOUND_LIMIT.availablePermits()
                            + " thread=" + Thread.currentThread().getName());

            throw e;

        } catch (IOException e) {
            long endMillis = System.currentTimeMillis();

            log("OUTBOUND_IO_EXCEPTION",
                    "method=" + method
                            + " url=" + url
                            + " message=" + safeMessage(e)
                            + " totalMs=" + (endMillis - startMillis)
                            + " permitsNow=" + OUTBOUND_LIMIT.availablePermits()
                            + " thread=" + Thread.currentThread().getName());

            throw e;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            log("OUTBOUND_INTERRUPTED",
                    "method=" + method
                            + " url=" + url
                            + " permitsNow=" + OUTBOUND_LIMIT.availablePermits()
                            + " thread=" + Thread.currentThread().getName());

            throw new IOException("Interrupted while waiting for outbound slot", e);

        } catch (Exception e) {
            long endMillis = System.currentTimeMillis();

            log("OUTBOUND_UNEXPECTED_EXCEPTION",
                    "method=" + method
                            + " url=" + url
                            + " class=" + e.getClass().getName()
                            + " message=" + safeMessage(e)
                            + " totalMs=" + (endMillis - startMillis)
                            + " permitsNow=" + OUTBOUND_LIMIT.availablePermits()
                            + " thread=" + Thread.currentThread().getName());

            if (e instanceof ServiceUnavailableException) {
                throw (ServiceUnavailableException) e;
            }

            if (e instanceof IOException) {
                throw (IOException) e;
            }

            throw new IOException("Unexpected outbound HTTP error calling " + url, e);

        } finally {
            if (con != null) {
                con.disconnect();
            }
            WAITING_ENDPOINTS.remove(reqId);
            RUNNING_ENDPOINTS.remove(reqId);
            dumpOutboundState("OUTBOUND_FINISHED", trace);
            if (acquired) {
                OUTBOUND_LIMIT.release();
            }
        }
    }
    
    private static void dumpOutboundState(String event, EndpointTrace current) {
        long now = System.currentTimeMillis();

        StringBuilder sb = new StringBuilder();
        sb.append("RestClient [").append(event).append("] ");
        sb.append("current=").append(current == null ? "null" : current.toLogLine(now));
        sb.append(" waitingCount=").append(WAITING_ENDPOINTS.size());
        sb.append(" runningCount=").append(RUNNING_ENDPOINTS.size());
        sb.append(" permitsNow=").append(OUTBOUND_LIMIT.availablePermits());

        sb.append(System.lineSeparator()).append("  WAITING:");
        for (EndpointTrace t : WAITING_ENDPOINTS.values()) {
            sb.append(System.lineSeparator()).append("    ").append(t.toLogLine(now));
        }

        sb.append(System.lineSeparator()).append("  RUNNING:");
        for (EndpointTrace t : RUNNING_ENDPOINTS.values()) {
            sb.append(System.lineSeparator()).append("    ").append(t.toLogLine(now));
        }

//        System.out.println(sb.toString());
    }

    private static String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }

        try (BufferedReader in = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    private static String abbreviate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }

    private static String safeMessage(Throwable t) {
        return t.getMessage() == null ? "" : t.getMessage().replace('\n', ' ').replace('\r', ' ');
    }

    private static void log(String event, String message) {
    	writeOutboundLog(message);
//        System.out.println("RestClient [" + event + "] " + message);
    }

    private static final Object OUTBOUND_LOG_LOCK = new Object();
    private static final java.io.File OUTBOUND_LOG_FILE =
            new java.io.File("../logs/restclient-outbound-state.log");

    private static void writeOutboundLog(String text) {
        synchronized (OUTBOUND_LOG_LOCK) {
            try (java.io.FileWriter fw = new java.io.FileWriter(OUTBOUND_LOG_FILE, true);
                 java.io.BufferedWriter bw = new java.io.BufferedWriter(fw);
                 java.io.PrintWriter pw = new java.io.PrintWriter(bw)) {

                pw.println(text);

            } catch (java.io.IOException e) {
                System.err.println("RestClient [OUTBOUND_LOG_ERROR] " + e.getMessage());
            }
        }
    }
    
    private static void ensureSslValidationDisabled() throws NoSuchAlgorithmException, KeyManagementException {
        if (sslValidationDisabled) {
            return;
        }

        synchronized (RestClient.class) {
            if (sslValidationDisabled) {
                return;
            }

            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            sslValidationDisabled = true;
        }
    }
    
    private static final class EndpointTrace {
        private final long id;
        private final String method;
        private final String url;
        private final String thread;
        private final long createdAt;

        private EndpointTrace(long id, String method, String url) {
            this.id = id;
            this.method = method;
            this.url = url;
            this.thread = Thread.currentThread().getName();
            this.createdAt = System.currentTimeMillis();
        }

        private String toLogLine(long now) {
            return "#" + id
                    + " ageMs=" + (now - createdAt)
                    + " method=" + method
                    + " url=" + url
                    + " thread=" + thread;
        }
    }
}