package mx.com.liverpool.p360.services.core.gcp;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.PubsubMessage;

import mx.com.liverpool.p360.services.core.PropertiesManager;

/**
 * Background worker that listens for Cloud Storage object events through Pub/Sub.
 *
 * <p>Expected GCP setup:</p>
 * <ol>
 *   <li>The bucket publishes OBJECT_FINALIZE events to a Pub/Sub topic.</li>
 *   <li>A subscription is created for this worker.</li>
 *   <li>The configured service account can consume the subscription and read
 *       the bucket object.</li>
 * </ol>
 *
 * <p>Required properties:</p>
 * <pre>
 * p360.contingency.gcp.bucket_listener.service_account=/path/service-account.json
 * p360.contingency.gcp.bucket_listener.project_id=gcp-project-id
 * p360.contingency.gcp.bucket_listener.subscription_id=subscription-id
 * </pre>
 *
 * <p>Optional property:</p>
 * <pre>
 * p360.contingency.gcp.bucket_listener.prefix=incoming/
 * </pre>
 *
 * <p>Ack strategy: the message is acknowledged only after the file has been read
 * and processed. Errors trigger nack so Pub/Sub can retry according to the
 * subscription policy.</p>
 */
public class GcpBucketFileListener {

    private static final String CONFIG_SERVICE_ACCOUNT = "p360.contingency.gcp.bucket_listener.service_account";
    private static final String CONFIG_PROJECT_ID = "p360.contingency.gcp.bucket_listener.project_id";
    private static final String CONFIG_SUBSCRIPTION_ID = "p360.contingency.gcp.bucket_listener.subscription_id";
    private static final String CONFIG_PREFIX = "p360.contingency.gcp.bucket_listener.prefix";

    public static void main(String[] args) throws Exception {
        new GcpBucketFileListener().listen();
    }

    /**
     * Starts the subscriber and keeps the current JVM alive while it listens.
     */
    public void listen() throws Exception {
        String serviceAccountFile = required(CONFIG_SERVICE_ACCOUNT);
        String projectId = required(CONFIG_PROJECT_ID);
        String subscriptionId = required(CONFIG_SUBSCRIPTION_ID);
        String prefix = optional(CONFIG_PREFIX);

        GcpStorageFileReader storageReader = new GcpStorageFileReader(serviceAccountFile);
        GcpBucketFileProcessor processor = new GcpBucketFileProcessor();

        GoogleCredentials credentials;
        try (java.io.FileInputStream in = new java.io.FileInputStream(serviceAccountFile)) {
            credentials = GoogleCredentials.fromStream(in)
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
        }

        ProjectSubscriptionName subscriptionName = ProjectSubscriptionName.of(projectId, subscriptionId);
        MessageReceiver receiver = (PubsubMessage message, AckReplyConsumer consumer) -> {
            String payload = message.getData().toStringUtf8();
            try {
                GcpBucketFileEvent event = GcpBucketFileEvent.fromPubSubPayload(payload);
                log("Received " + event);

                if (prefix != null && prefix.trim().length() > 0 && !event.getObjectName().startsWith(prefix)) {
                    log("Ignoring object because it does not match prefix: " + event.getObjectName());
                    consumer.ack();
                    return;
                }

                // Transport ends here; all file-specific rules belong in the processor.
                try (InputStream inputStream = storageReader.read(event.getBucket(), event.getObjectName())) {
                    processor.process(event.getBucket() + "/" + event.getObjectName(), inputStream);
                }
                consumer.ack();
            } catch (Exception e) {
                logE(e);
                consumer.nack();
            }
        };

        Subscriber subscriber = Subscriber.newBuilder(subscriptionName, receiver)
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build();

        subscriber.startAsync().awaitRunning();
        log("Listening subscription: " + subscriptionName.toString());
        subscriber.awaitTerminated(Long.MAX_VALUE, TimeUnit.DAYS);
    }

    private static String required(String key) {
        String value = PropertiesManager.get(key);
        if (value == null || value.trim().length() == 0) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value;
    }

    private static String optional(String key) {
        String value = PropertiesManager.get(key);
        return value == null || value.trim().length() == 0 ? null : value;
    }

    private static void log(String message) {
        System.out.println("GcpBucketFileListener - " + message);
    }

    private static void logE(Exception e) {
        e.printStackTrace(System.err);
    }
}
