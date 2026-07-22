package mx.com.liverpool.p360.services.core.gcp.storage;

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
 * Optional Pub/Sub listener for Cloud Storage object events.
 *
 * <p>The current placeholder flow is expected to run as a scheduled job or from
 * a servlet. This listener remains transport-only so a future event-driven flow
 * can reuse bucket connectivity without embedding business logic here.</p>
 */
public class GcpBucketFileListener {

    private static final String CONFIG_SERVICE_ACCOUNT = "p360.contingency.gcp.storage.service_account";
    private static final String CONFIG_PROJECT_ID = "p360.contingency.gcp.storage.project_id";
    private static final String CONFIG_SUBSCRIPTION_ID = "p360.contingency.gcp.storage.subscription_id";
    private static final String CONFIG_PREFIX = "p360.contingency.gcp.storage.listener_prefix";

    private final GcpBucketObjectHandler handler;

    public GcpBucketFileListener(GcpBucketObjectHandler handler) {
        this.handler = handler;
    }

    public void listen() throws Exception {
        String serviceAccountFile = required(CONFIG_SERVICE_ACCOUNT);
        String projectId = required(CONFIG_PROJECT_ID);
        String subscriptionId = required(CONFIG_SUBSCRIPTION_ID);
        String prefix = optional(CONFIG_PREFIX);
        GcpStorageClient storageClient = new GcpStorageClient(serviceAccountFile);

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

                try (InputStream inputStream = storageClient.read(event.getBucket(), event.getObjectName())) {
                    handler.handle(event.getBucket(), event.getObjectName(), inputStream);
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
