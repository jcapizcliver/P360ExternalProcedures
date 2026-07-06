package mx.com.liverpool.p360.services.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.api.core.ApiFuture;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;

public class PubSubGCP {

    private final String PROJECT_ID; // = "";
    private final String TOPIC_ID;   // = "p360_put_proposals";
    private final String SERVICE_ACCOUNT_FILE;

    // ---- cache interno (no cambia tu API) ----
    private volatile GoogleCredentials cachedCredentials;
    private volatile Publisher cachedPublisher;

    // Para que si alguien “abandona” el objeto sin cerrar, no queden channels huérfanos
    private static final java.lang.ref.Cleaner CLEANER = java.lang.ref.Cleaner.create();
    private final AtomicReference<Publisher> publisherRef = new AtomicReference<>();
    private final java.lang.ref.Cleaner.Cleanable cleanable;

    private static final class State implements Runnable {
        private final AtomicReference<Publisher> ref;
        State(AtomicReference<Publisher> ref) { this.ref = ref; }

        @Override public void run() {
            Publisher p = ref.getAndSet(null);
            if (p != null) {
                p.shutdown();
                try {
                    if (!p.awaitTermination(60, TimeUnit.SECONDS)) {
                        p.shutdown();
                    }
                } catch (InterruptedException ignored) {
                    p.shutdown();
                    Thread.currentThread().interrupt();
                } catch (Exception ignored) {
                    // no reventar el cleaner
                }
            }
        }
    }

    public static void main(String[] args) {
        new PubSubGCP().publishMessage(args);
    }

    public void publishMessage(String[] args) {
        if (args != null && args.length >= 4) {
            PubSubGCP psp = new PubSubGCP(args[0], args[1], args[2]);
            try {
                psp.publishMessage(psp.readMessage(args[3]));
            } finally {
                // batch: cerrar limpio
                psp.close();
            }
        } else {
            log("Invalid invokation, need to specify service account, projectID, topicID and a json file as the first parameters in that order.");
        }
    }

    public PubSubGCP() {
        PROJECT_ID = null;
        TOPIC_ID = null;
        SERVICE_ACCOUNT_FILE = null;
        this.cleanable = CLEANER.register(this, new State(this.publisherRef));
    }

    public PubSubGCP(String sak, String projectId, String topicId) {
        SERVICE_ACCOUNT_FILE = sak;
        PROJECT_ID = projectId;
        TOPIC_ID = topicId;
        this.cleanable = CLEANER.register(this, new State(this.publisherRef));
    }

    private String readMessage(String file) {
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.FileInputStream(file)))) {
            String line = null;
            StringBuilder sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (java.io.IOException e) {
            logE(e);
        }
        return null;
    }

    // ---- helper interno ----
    private GoogleCredentials getOrLoadCredentials() throws IOException {
        if (cachedCredentials != null) return cachedCredentials;
        synchronized (this) {
            if (cachedCredentials != null) return cachedCredentials;

            // FIX: cerrar FileInputStream (antes fugaba FD)
            try (FileInputStream in = new FileInputStream(SERVICE_ACCOUNT_FILE)) {
                cachedCredentials = GoogleCredentials
                        .fromStream(in)
                        .createScoped("https://www.googleapis.com/auth/cloud-platform");
            }
            return cachedCredentials;
        }
    }

    // ---- helper interno ----
    private Publisher getOrCreatePublisher() throws IOException {
        Publisher p = cachedPublisher;
        if (p != null) return p;

        synchronized (this) {
            if (cachedPublisher != null) return cachedPublisher;

            ProjectTopicName topicName = ProjectTopicName.of(PROJECT_ID, TOPIC_ID);
            GoogleCredentials credentials = getOrLoadCredentials();

            Publisher created = Publisher.newBuilder(topicName)
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            cachedPublisher = created;
            publisherRef.set(created); // para Cleaner
            return created;
        }
    }

    // Si el publisher se corrompe/queda cerrado, lo reseteamos y recreamos
    private void resetPublisher() {
        Publisher old;
        synchronized (this) {
            old = cachedPublisher;
            cachedPublisher = null;
            publisherRef.set(null);
        }
        if (old != null) {
            old.shutdown();
            try {
                if (!old.awaitTermination(30, TimeUnit.SECONDS)) {
                    old.shutdown();
                }
            } catch (InterruptedException ignored) {
                old.shutdown();
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {}
        }
    }

    public String publishMessage(String message) {
        String myTag = null;
        if (message == null) {
            return null;
        }

        // 1 retry si el publisher estaba “roto”/cerrado
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                Publisher publisher = getOrCreatePublisher();

                ByteString data = ByteString.copyFromUtf8(message);
                PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

                ApiFuture<String> a = publisher.publish(pubsubMessage);
                log(myTag = a.get());
                log("Published message: " + message.substring(0,100) + "... Topic: " + TOPIC_ID + ", project: " + PROJECT_ID);
                return myTag;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logE(e);
                return null;

            } catch (IOException | ExecutionException | RuntimeException e) {
                // Si el publisher/channel murió o quedó huérfano, lo recreamos 1 vez
                logE(e);
                resetPublisher();
            }
        }
        return null;
    }

    public String publishMessage(String projectId, String topicId, String serviceAccountFile, String message) {
        if (message == null) {
            return null;
        }
        String aoc = null;
        ProjectTopicName topicName = ProjectTopicName.of(projectId, topicId);
        Publisher publisher = null;

        try {
            GoogleCredentials credentials;

            // FIX: cerrar FileInputStream (antes fugaba FD)
            try (FileInputStream in = new FileInputStream(serviceAccountFile)) {
                credentials = GoogleCredentials
                        .fromStream(in)
                        .createScoped("https://www.googleapis.com/auth/cloud-platform");
            }

            publisher = Publisher.newBuilder(topicName)
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();

            ByteString data = ByteString.copyFromUtf8(message);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

            ApiFuture<String> a = publisher.publish(pubsubMessage);
            log(aoc = a.get());
            log("Published message: " + message.substring(0, message.length() >= 1000 ? 1000 : message.length()) + (message.length() > 1000 ? "..." : ""));

        } catch (InterruptedException e) {
            logE(e);
            Thread.currentThread().interrupt();
        } catch (IOException | ExecutionException e) {
            logE(e);
        } finally {
            if (publisher != null) {
                publisher.shutdown();
                try {
                    if (!publisher.awaitTermination(60, TimeUnit.SECONDS)) {
                        publisher.shutdown();
                    }
                } catch (InterruptedException ignored) {
                    try{
                    	publisher.shutdown();
                    }catch(IllegalStateException e) {
                    	logE(e);
                    	log("Perhaps continuing...");
                    }
                    Thread.currentThread().interrupt();
                }
            }
        }
        return aoc;
    }

    // ---- NO rompe a nadie: cierre explícito recomendado ----
    public void close() {
        // Ejecuta el mismo cierre del Cleaner pero de forma explícita
        cleanable.clean();
        synchronized (this) {
            cachedPublisher = null;
        }
    }

    private void log(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/pubsub_writer.log", true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "]  " + message);
        } catch (java.io.IOException e) {
        }
    }

    private static void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/pubsub_writer.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }
}

