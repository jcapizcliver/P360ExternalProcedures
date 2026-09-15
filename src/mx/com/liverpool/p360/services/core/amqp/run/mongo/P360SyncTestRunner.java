package mx.com.liverpool.p360.services.core.amqp.run.mongo;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

/**
 * Runner manual. Lee el archivo línea por línea reutilizando UN SOLO MongoClient.
 */
public class P360SyncTestRunner {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("Uso: <archivo.log> <mongoUri> <database> <productsCollection>");
            System.exit(2);
        }

        Path file = Path.of(args[0]);
        String mongoUri = args[1];
        String database = args[2];
        String productsCollection = args[3];

        try (MongoClient mongoClient = MongoClients.create(mongoUri)) {
            P360SyncService service = P360SyncFactory.create(
                    mongoClient,
                    database,
                    productsCollection);

            int lineNumber = 0;
            int applied = 0;

            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                lineNumber++;
                try {
                    int current = service.process(line);
                    applied += current;
                    if (current > 0) {
                        System.out.println("Linea " + lineNumber + ": aplicados=" + current);
                    }
                } catch (Exception e) {
                    System.err.println("Error en linea " + lineNumber + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            System.out.println("Lineas=" + lineNumber + ", eventos aplicados=" + applied);
        }
    }
}
