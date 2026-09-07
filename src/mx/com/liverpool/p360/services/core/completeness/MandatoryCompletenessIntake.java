package mx.com.liverpool.p360.services.core.completeness;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.function.BooleanSupplier;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.TextMessage;
import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager;

/** Journal before acknowledgement, on the existing consumer, without another JMS subscription. */
public final class MandatoryCompletenessIntake {
    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("p360.completeness.incremental.enabled",
            PropertiesManager.get("p360.completeness.incremental.enabled", "false")));
    private MandatoryCompletenessIntake() {}
    public static boolean enabled() {
        return ENABLED;
    }

    public static MessageConsumer wrap(MessageConsumer delegate, boolean acknowledge, BooleanSupplier running) {
        if (!enabled()) return delegate;
        return (MessageConsumer) Proxy.newProxyInstance(MessageConsumer.class.getClassLoader(),
                new Class<?>[] {MessageConsumer.class}, new java.lang.reflect.InvocationHandler() {
            private Connection connection;
            private long lastWarning;
            private void closeDb() {
                if (connection != null) try { connection.close(); } catch (Exception ignored) {}
                connection = null;
            }
            public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
                if (method.getName().equals("setMessageListener"))
                    throw new UnsupportedOperationException("Completeness intake requires synchronous receive");
                if (method.getName().equals("close")) closeDb();
                Object result;
                try { result = method.invoke(delegate, args); }
                catch (InvocationTargetException e) { throw e.getCause(); }
                if (method.getName().startsWith("receive") && result instanceof Message message) {
                    MandatoryCompletenessChange change = null;
                    if (message instanceof TextMessage text) {
                        try { change = MandatoryCompletenessChange.parse(text.getText()); }
                        catch (Exception malformed) {
                            // Preserve reconciliation even when an old/new qualification cannot be decoded.
                            System.err.println("Mandatory intake: unrecognized change; scheduling paged reconciliation. JMS="
                                    + message.getJMSMessageID());
                            change = MandatoryCompletenessChange.sweep();
                        }
                    }
                    while (change != null) {
                        if (!running.getAsBoolean()) { closeDb(); throw new javax.jms.JMSException("Intake stopped before acknowledgement"); }
                        try {
                            if (connection == null || connection.isClosed())
                                connection = new QuickJdbcConnectionManager().openConnection(false);
                            new MandatoryCompletenessPendingDao(connection).enqueue(change);
                            connection.commit();
                            break;
                        } catch (Exception unavailable) {
                            closeDb();
                            if (System.currentTimeMillis() - lastWarning > 60000) {
                                System.err.println("Mandatory intake waiting for DB/capacity; message remains in JMS: "
                                        + unavailable.getClass().getSimpleName() + " " + unavailable.getMessage());
                                lastWarning = System.currentTimeMillis();
                            }
                            try { Thread.sleep(1000); }
                            catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new javax.jms.JMSException("Intake interrupted before acknowledgement");
                            }
                        }
                    }
                    // These legacy receivers used AUTO_ACKNOWLEDGE (acknowledged at receive return).
                    // Keep their contract, now preceded by a committed durable invalidation.
                    if (acknowledge) message.acknowledge();
                }
                return result;
            }
        });
    }
}
