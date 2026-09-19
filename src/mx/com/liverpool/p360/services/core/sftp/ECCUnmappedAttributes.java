package mx.com.liverpool.p360.services.core.sftp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;

/** Durable replay evidence. Remove an unmapped field only after its values are persisted. */
final class ECCUnmappedAttributes {
    static synchronized void deferUnmapped(Map<String,List<String>> incoming, Map<String,String> mapping,
            Path file, String sourceFile, String sku) throws IOException {
        java.util.Iterator<Map.Entry<String,List<String>>> it = incoming.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String,List<String>> e = it.next();
            String target = mapping.get(e.getKey());
            if (target != null && !target.isBlank()) continue;
            JSONObject row = new JSONObject().put("schemaVersion", 1)
                    .put("recordedAt", java.time.Instant.now().toString())
                    .put("sourceFile", sourceFile == null ? JSONObject.NULL : sourceFile)
                    .put("sku", sku).put("eccAttribute", e.getKey())
                    .put("values", e.getValue()).put("reason", "NO_ECC_MAPPING");
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE, StandardOpenOption.APPEND); FileLock lock = channel.lock()) {
                ByteBuffer bytes = StandardCharsets.UTF_8.encode(row.toString() + "\n");
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            it.remove();
        }
    }
}
