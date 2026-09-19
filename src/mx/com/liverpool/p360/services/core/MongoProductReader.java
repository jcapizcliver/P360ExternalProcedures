package mx.com.liverpool.p360.services.core;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.BufferedWriter;
import java.util.concurrent.TimeUnit;

/** Read-only, bounded JSONL export from products/skus. No P360 writes. */
public final class MongoProductReader {
    public static void main(String[] args) {
        try { run(args); }
        catch (Exception e) {
            // Driver exception messages may contain connection details or data.
            System.err.println("Export failed: " + e.getClass().getSimpleName());
            System.exit(1);
        }
    }
    private static void run(String[] args) throws Exception {
        if (args.length < 2 || args.length > 4) {
            throw new IllegalArgumentException("Usage: products|skus output.jsonl [filter.json|--indexes] [limit:1..10000]; MONGODB_URI required");
        }
        String collection=args[0];
        if (!collection.equals("products") && !collection.equals("skus")) throw new IllegalArgumentException("Invalid collection");
        int limit=args.length==4?Integer.parseInt(args[3]):10;
        if(limit<1 || limit>10000) throw new IllegalArgumentException("Limit must be 1..10000");
        String uri=System.getenv("MONGODB_URI");
        if(uri==null || uri.isBlank()) throw new IllegalArgumentException("MONGODB_URI required");
        boolean indexes=args.length>=3 && args[2].equals("--indexes");
        Document filter=args.length>=3 && !indexes?Document.parse(Files.readString(Path.of(args[2]),StandardCharsets.UTF_8)):new Document();
        ConnectionString cs=new ConnectionString(uri);
        if(!"product".equals(cs.getDatabase())) throw new IllegalArgumentException("Expected product database");
        MongoClientSettings settings=MongoClientSettings.builder().applyConnectionString(cs)
            .applicationName("P360-Reconciliation-ReadOnly")
            .applyToClusterSettings(b->b.serverSelectionTimeout(20,TimeUnit.SECONDS))
            .applyToSocketSettings(b->b.connectTimeout(10,TimeUnit.SECONDS).readTimeout(40,TimeUnit.SECONDS))
            .applyToConnectionPoolSettings(b->b.maxSize(2)).build();
        Path output=Path.of(args[1]);
        Path partial=output.resolveSibling(output.getFileName()+".partial");
        if(Files.exists(output)) throw new java.nio.file.FileAlreadyExistsException(output.toString());
        JsonWriterSettings json=JsonWriterSettings.builder().outputMode(JsonMode.EXTENDED).build();
        int count=0;
        try(MongoClient client=MongoClients.create(settings);
            BufferedWriter writer=Files.newBufferedWriter(partial,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)) {
            MongoCollection<Document> c=client.getDatabase("product").getCollection(collection);
            MongoCursor<Document> cursor=indexes?c.listIndexes().maxTime(30,TimeUnit.SECONDS).iterator():
                c.find(filter).limit(limit).batchSize(Math.min(limit,100)).maxTime(30,TimeUnit.SECONDS).iterator();
            try(cursor) {
                while(count<limit && cursor.hasNext()) {writer.write(cursor.next().toJson(json));writer.newLine();count++;}
            }
        }
        Files.move(partial,output);
        System.out.println("collection="+collection+" documents="+count+" output="+output);
    }
}
