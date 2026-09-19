package mx.com.liverpool.p360.services.core.completeness;

import java.nio.file.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.*;
import mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager;
import org.json.JSONObject;

/** Local durable invalidations. Oracle work never runs on the JMS receiver. */
public final class MandatoryCompletenessDurableQueue {
    private final Path root;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private FileChannel active;
    private int count;
    private long sequence = System.currentTimeMillis();
    private static MandatoryCompletenessDurableQueue instance;
    public static synchronized MandatoryCompletenessDurableQueue instance() throws Exception {
        if (instance == null) {
            instance = new MandatoryCompletenessDurableQueue(Path.of(System.getProperty(
                "p360.completeness.journal.directory", "/u01/workshop/java/operations/mandatory-intake-durable")));
            Thread t = new Thread(instance::run, "mandatory-durable-drain");
            t.setDaemon(true); t.start();
        }
        return instance;
    }
    MandatoryCompletenessDurableQueue(Path root) throws Exception {
        this.root = root; Files.createDirectories(root);
        lockChannel = FileChannel.open(root.resolve("writer.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        lock = lockChannel.tryLock();
        if (lock == null) throw new java.io.IOException("Another completeness journal writer is active");
        Path p = root.resolve("active.ndjson");
        // An unacknowledged partial tail can survive abrupt termination; retain every full durable record.
        try (java.io.RandomAccessFile f = new java.io.RandomAccessFile(p.toFile(), "rw")) {
            long end = f.length();
            while (end > 0) { f.seek(end-1); if (f.read() == '\n') break; end--; }
            f.setLength(end); f.getChannel().force(true);
        }
        open();
        if (active.size() > 0) seal();
    }
    private void syncDirectory() throws Exception {
        try (FileChannel d = FileChannel.open(root, StandardOpenOption.READ)) { d.force(true); }
    }
    private void open() throws Exception {
        active = FileChannel.open(root.resolve("active.ndjson"), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        syncDirectory(); count = 0;
    }
    public synchronized void append(String body) throws Exception {
        byte[] bytes = (new JSONObject().put("body", body).toString()+"\n").getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long before = active.size();
        try {
            while (buffer.hasRemaining()) active.write(buffer);
            active.force(true); // ACK is allowed only after this returns.
        } catch (Exception failure) {
            active.truncate(before); active.force(true); throw failure;
        }
        if (++count >= 900 || active.size() >= 16L*1024*1024) seal();
    }
    synchronized void seal() throws Exception {
        if (active.size() == 0) return;
        active.force(true); active.close();
        Path ready;
        do { ready = root.resolve(String.format("%019d.ready", ++sequence)); } while (Files.exists(ready));
        Files.move(root.resolve("active.ndjson"), ready, StandardCopyOption.ATOMIC_MOVE);
        syncDirectory(); open();
    }
    interface Sink { void accept(MandatoryCompletenessChange change) throws Exception; }
    int drainOne(Sink sink) throws Exception {
        List<Path> candidates = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(root, "*.ready")) {
            for (Path p : files) candidates.add(p);
        }
        candidates.sort(Comparator.comparing(Path::toString));
        List<Path> selected = new ArrayList<>(); long bytes=0;
        for(Path p:candidates) {
            long size=Files.size(p);
            if(!selected.isEmpty() && (selected.size()>=64 || bytes+size>64L*1024*1024))break;
            selected.add(p);bytes+=size;
        }
        if (selected.isEmpty()) return 0;
        Map<String, MandatoryCompletenessChange> merged = new LinkedHashMap<>(); int records=0;
        for(Path first:selected) {
        try (var lines = Files.newBufferedReader(first, StandardCharsets.UTF_8)) {
            for (String line; (line=lines.readLine()) != null;) {
                // Corrupt journal JSON must fail closed; never silently discard durable records.
                String body = new JSONObject(line).getString("body");
                MandatoryCompletenessChange change;
                try { change = MandatoryCompletenessChange.parse(body); }
                catch (Exception malformed) { change = MandatoryCompletenessChange.sweep(); }
                records++;
                if (change != null) {
                    String key=change.entity()+"\u0000"+change.identifier();
                    merged.merge(key,change,(a,b)->a.merge(b));
                }
            }
        }
        }
        for (var change : merged.values()) sink.accept(change);
        // Sink must commit before returning. A crash before deletion safely replays invalidations.
        for(Path first:selected) Files.delete(first);
        syncDirectory();
        System.out.println("MANDATORY_DURABLE_BATCH files="+selected.size()+" records="+records+" entities="+merged.size());
        return records;
    }
    private void run() {
        Connection db=null; long done=0, lastReport=0, lastError=0;
        while (true) {
            try {
                seal();
                if (db == null || db.isClosed()) db = new QuickJdbcConnectionManager().openConnection(false);
                final Connection connection=db;
                int n=drainOne(change -> {
                    try { new MandatoryCompletenessPendingDao(connection).enqueue(change); connection.commit(); }
                    catch (Exception failure) { try { connection.rollback(); } catch(Exception ignored) {} throw failure; }
                });
                done+=n;
                if (System.currentTimeMillis()-lastReport>60000) {
                    System.out.println("MANDATORY_DURABLE drained_records="+done+" directory="+root); lastReport=System.currentTimeMillis();
                }
                if (n==0) Thread.sleep(1000);
            } catch (Exception failure) {
                if (db!=null) try { db.close(); } catch(Exception ignored) {} db=null;
                if(System.currentTimeMillis()-lastError>60000) {
                    System.err.println("MANDATORY_DURABLE retained_for_retry="+failure.getClass().getSimpleName()+(failure instanceof java.sql.SQLException e ? " state="+e.getSQLState()+" code="+e.getErrorCode() : ""));lastError=System.currentTimeMillis();
                }
                try { Thread.sleep(5000); } catch(InterruptedException stopped) { return; }
            }
        }
    }
    void closeForTest() throws Exception { active.close(); lock.release(); lockChannel.close(); }
}
