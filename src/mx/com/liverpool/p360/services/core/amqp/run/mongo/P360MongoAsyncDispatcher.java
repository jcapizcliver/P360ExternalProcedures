package mx.com.liverpool.p360.services.core.amqp.run.mongo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Desacopla el procesamiento de Mongo del hilo principal de ActiveMQ.
 *
 * Características:
 * - Un solo worker para conservar el orden FIFO de los mensajes.
 * - submit() NO bloquea si la cola se llena.
 * - Reintenta errores de Mongo fuera del hilo principal.
 * - Los mensajes que agotan reintentos van a dead-letter.
 * - Durante shutdown intenta drenar la cola antes de terminar.
 *
 * Importante:
 * Si submit() devuelve false, el mensaje NO fue encolado.
 * Se registra el overflow, pero no se escribe el dead-letter desde submit()
 * para no meter I/O de disco en el hilo principal de ActiveMQ.
 */
public final class P360MongoAsyncDispatcher
        implements AutoCloseable {

    private static final int MAX_RETRIES = 3;

    private static final long POLL_TIMEOUT_MS = 500L;

    private static final long GRACEFUL_SHUTDOWN_SECONDS = 30L;

    private static final long FORCED_SHUTDOWN_WAIT_MS = 5000L;

    private final P360SyncService syncService;

    private final BlockingQueue<String> queue;

    private final int queueCapacity;

    private final Thread worker;

    private final Consumer<String> logger;

    private final Consumer<Exception> errorLogger;

    private final Path deadLetterFile;

    private volatile boolean running = true;

    private volatile boolean accepting = true;

    public P360MongoAsyncDispatcher(
            P360SyncService syncService,
            int queueCapacity,
            Path deadLetterFile,
            Consumer<String> logger,
            Consumer<Exception> errorLogger) {

        if (syncService == null) {
            throw new IllegalArgumentException(
                    "syncService no puede ser null");
        }

        if (queueCapacity <= 0) {
            throw new IllegalArgumentException(
                    "queueCapacity debe ser mayor a cero");
        }

        this.syncService =
                syncService;

        this.queueCapacity =
                queueCapacity;

        this.queue =
                new ArrayBlockingQueue<>(
                        queueCapacity);

        this.deadLetterFile =
                deadLetterFile;

        this.logger =
                logger != null
                        ? logger
                        : message -> {
                };

        this.errorLogger =
                errorLogger != null
                        ? errorLogger
                        : Exception::printStackTrace;

        this.worker =
                new Thread(
                        this::runLoop,
                        "P360-Mongo-Sync");

        /*
         * No daemon:
         *
         * Durante un shutdown normal queremos tener oportunidad
         * de terminar lo que ya está encolado.
         */
        this.worker.setDaemon(false);

        this.worker.start();
    }

    /**
     * Intenta encolar el mensaje SIN bloquear al caller.
     *
     * @return true si el mensaje fue aceptado (o era vacío);
     *         false si el dispatcher ya está cerrando o la cola está llena.
     */
    public boolean submit(String message) {

        if (message == null
                || message.isBlank()) {
            return true;
        }

        if (!accepting) {

            logger.accept(
                    "Mongo async dispatcher ya no acepta mensajes.");

            return false;
        }

        /*
         * offer() es intencional.
         *
         * A diferencia de put(), NO espera cuando la cola se llena.
         * De esta manera una lentitud o caída de Mongo no bloquea
         * el hilo principal del listener de ActiveMQ.
         */
        boolean accepted =
                queue.offer(message);

        if (!accepted) {

            logger.accept(
                    "WARNING: Mongo async queue llena. "
                            + "Mensaje NO encolado. pending="
                            + queue.size()
                            + ", capacity="
                            + queueCapacity);
        }

        return accepted;
    }

    public int pendingMessages() {
        return queue.size();
    }

    public int queueCapacity() {
        return queueCapacity;
    }

    public int remainingCapacity() {
        return queue.remainingCapacity();
    }

    public double queueUsagePercent() {

        return queueCapacity == 0
                ? 0.0
                : (queue.size() * 100.0)
                / queueCapacity;
    }

    public boolean isAccepting() {
        return accepting;
    }

    public boolean isWorkerAlive() {
        return worker.isAlive();
    }

    private void runLoop() {

        logger.accept(
                "Mongo async dispatcher iniciado. capacity="
                        + queueCapacity);

        try {

            while (running || !queue.isEmpty()) {

                String message;

                try {

                    message =
                            queue.poll(
                                    POLL_TIMEOUT_MS,
                                    TimeUnit.MILLISECONDS);

                } catch (InterruptedException e) {

                    Thread.currentThread()
                            .interrupt();

                    break;
                }

                if (message == null) {
                    continue;
                }

                processWithRetry(message);
            }

        } catch (Exception e) {

            /*
             * Una excepción inesperada no debe quedar silenciosa.
             */
            errorLogger.accept(e);

        } finally {

            /*
             * Si salimos forzadamente del loop y quedaron mensajes,
             * tratamos de preservarlos en el dead-letter.
             *
             * Esto ocurre en el worker, nunca en submit().
             */
            drainRemainingToDeadLetter();

            logger.accept(
                    "Mongo async dispatcher terminado. pending="
                            + queue.size());
        }
    }

    private void processWithRetry(
            String message) {

        Exception lastException = null;

        for (int attempt = 1;
             attempt <= MAX_RETRIES;
             attempt++) {

            /*
             * Si el shutdown ya fue forzado por interrupt,
             * preservamos el mensaje y dejamos de insistir.
             */
            if (Thread.currentThread().isInterrupted()) {

                writeDeadLetter(
                        message,
                        lastException);

                return;
            }

            try {

                int processed =
                        syncService.process(message);

                if (processed > 0) {

                    logger.accept(
                            "Mongo sync procesado. "
                                    + "events="
                                    + processed
                                    + ", pending="
                                    + queue.size());
                }

                return;

            } catch (Exception e) {

                lastException = e;

                logger.accept(
                        "Mongo sync falló. "
                                + "attempt="
                                + attempt
                                + "/"
                                + MAX_RETRIES
                                + ", pending="
                                + queue.size());

                errorLogger.accept(e);

                if (attempt < MAX_RETRIES) {

                    if (!sleepBeforeRetry(attempt)) {

                        /*
                         * Shutdown/interrupt durante el backoff:
                         * preservamos el mensaje actual.
                         */
                        writeDeadLetter(
                                message,
                                lastException);

                        return;
                    }
                }
            }
        }

        /*
         * Si agotamos retries no perdemos silenciosamente
         * el JSON: se manda al dead-letter local.
         */
        writeDeadLetter(
                message,
                lastException);
    }

    private boolean sleepBeforeRetry(
            int attempt) {

        long waitMillis;

        switch (attempt) {

            case 1:
                waitMillis = 250L;
                break;

            case 2:
                waitMillis = 1000L;
                break;

            default:
                waitMillis = 3000L;
                break;
        }

        try {

            Thread.sleep(
                    waitMillis);

            return true;

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            return false;
        }
    }

    private void drainRemainingToDeadLetter() {

        String pendingMessage;

        while ((pendingMessage = queue.poll()) != null) {

            writeDeadLetter(
                    pendingMessage,
                    null);
        }
    }

    private void writeDeadLetter(
            String message,
            Exception cause) {

        if (message == null
                || message.isBlank()) {
            return;
        }

        if (deadLetterFile == null) {

            logger.accept(
                    "Mongo sync no pudo procesar un mensaje "
                            + "y no existe dead-letter configurado.");

            if (cause != null) {
                errorLogger.accept(cause);
            }

            return;
        }

        try {

            Path parent =
                    deadLetterFile.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                    deadLetterFile,
                    message
                            + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);

            logger.accept(
                    "Mensaje Mongo enviado a dead-letter: "
                            + deadLetterFile);

        } catch (IOException e) {

            errorLogger.accept(e);
        }
    }

    @Override
    public void close() {

        /*
         * Desde este punto submit() deja de aceptar mensajes nuevos.
         */
        accepting = false;

        /*
         * El worker seguirá mientras haya elementos en la cola,
         * porque runLoop usa:
         *
         * while (running || !queue.isEmpty())
         */
        running = false;

        logger.accept(
                "Cerrando Mongo dispatcher. pending="
                        + queue.size());

        try {

            worker.join(
                    TimeUnit.SECONDS.toMillis(
                            GRACEFUL_SHUTDOWN_SECONDS));

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            return;
        }

        if (worker.isAlive()) {

            logger.accept(
                    "Mongo dispatcher no terminó "
                            + "en "
                            + GRACEFUL_SHUTDOWN_SECONDS
                            + " segundos. Se solicitará interrupción. pending="
                            + queue.size());

            /*
             * Si Mongo está atorado/reintentando, pedimos al worker
             * que abandone el loop. El finally intentará mandar
             * lo que quede en cola al dead-letter.
             */
            worker.interrupt();

            try {

                worker.join(
                        FORCED_SHUTDOWN_WAIT_MS);

            } catch (InterruptedException e) {

                Thread.currentThread()
                        .interrupt();

                return;
            }
        }

        if (worker.isAlive()) {

            /*
             * En este caso el worker probablemente está atrapado
             * dentro de una operación del driver que no reaccionó
             * al interrupt. No intentamos Thread.stop().
             */
            logger.accept(
                    "WARNING: Mongo dispatcher continúa vivo "
                            + "después del timeout de shutdown. pending="
                            + queue.size());

        } else {

            logger.accept(
                    "Mongo dispatcher cerrado correctamente.");
        }
    }
}
