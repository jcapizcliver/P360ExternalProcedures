package com.example.ei.forfun.logic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class CsvExternalSorter {

    private CsvExternalSorter() {
    }

    public static void sortCsv(Path input,
                               Path output,
                               char delimiter,
                               List<String> keyColumns,
                               int chunkSize) throws IOException {

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize debe ser > 0");
        }

        List<Path> chunks = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("Archivo vacío: " + input);
            }

            List<String> header = CsvRowUtils.parseLine(headerLine, delimiter);
            List<Integer> keyIndexes = CsvRowUtils.resolveColumnIndexes(header, keyColumns);

            List<String> buffer = new ArrayList<>(chunkSize);
            String line;

            while ((line = reader.readLine()) != null) {
                buffer.add(line);

                if (buffer.size() >= chunkSize) {
                    chunks.add(writeSortedChunk(input.getParent(), headerLine, buffer, delimiter, keyIndexes));
                    buffer.clear();
                }
            }

            if (!buffer.isEmpty()) {
                chunks.add(writeSortedChunk(input.getParent(), headerLine, buffer, delimiter, keyIndexes));
            }

            mergeChunks(chunks, output, headerLine, delimiter, keyIndexes);
        } finally {
            for (Path chunk : chunks) {
                try {
                    Files.deleteIfExists(chunk);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static Path writeSortedChunk(Path dir,
                                         String headerLine,
                                         List<String> lines,
                                         char delimiter,
                                         List<Integer> keyIndexes) throws IOException {

        lines.sort(buildLineComparator(delimiter, keyIndexes));

        Path chunk = Files.createTempFile(dir, "chunk_", ".csv");

        try (BufferedWriter writer = Files.newBufferedWriter(chunk, StandardCharsets.UTF_8)) {
            writer.write(headerLine);
            writer.newLine();

            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }

        return chunk;
    }

    private static Comparator<String> buildLineComparator(char delimiter, List<Integer> keyIndexes) {
        return (a, b) -> {
            List<String> ra = CsvRowUtils.parseLine(a, delimiter);
            List<String> rb = CsvRowUtils.parseLine(b, delimiter);

            for (Integer idx : keyIndexes) {
                String va = idx < ra.size() ? ra.get(idx) : "";
                String vb = idx < rb.size() ? rb.get(idx) : "";
                int cmp = va.compareTo(vb);
                if (cmp != 0) {
                    return cmp;
                }
            }

            return a.compareTo(b);
        };
    }

    private static void mergeChunks(List<Path> chunks,
                                    Path output,
                                    String headerLine,
                                    char delimiter,
                                    List<Integer> keyIndexes) throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write(headerLine);
            writer.newLine();

            List<ChunkReader> readers = new ArrayList<>();
            PriorityQueue<ChunkLine> pq = new PriorityQueue<>(
                (x, y) -> buildLineComparator(delimiter, keyIndexes).compare(x.line, y.line)
            );

            try {
                for (Path chunk : chunks) {
                    ChunkReader cr = new ChunkReader(chunk);
                    readers.add(cr);

                    String line = cr.nextDataLine();
                    if (line != null) {
                        pq.add(new ChunkLine(line, cr));
                    }
                }

                while (!pq.isEmpty()) {
                    ChunkLine next = pq.poll();
                    writer.write(next.line);
                    writer.newLine();

                    String replacement = next.reader.nextDataLine();
                    if (replacement != null) {
                        pq.add(new ChunkLine(replacement, next.reader));
                    }
                }
            } finally {
                for (ChunkReader reader : readers) {
                    reader.close();
                }
            }
        }
    }

    private static class ChunkLine {
        private final String line;
        private final ChunkReader reader;

        private ChunkLine(String line, ChunkReader reader) {
            this.line = line;
            this.reader = reader;
        }
    }

    private static class ChunkReader implements AutoCloseable {
        private final BufferedReader reader;
        private boolean headerConsumed = false;

        private ChunkReader(Path file) throws IOException {
            this.reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        }

        private String nextDataLine() throws IOException {
            if (!headerConsumed) {
                reader.readLine();
                headerConsumed = true;
            }
            return reader.readLine();
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}