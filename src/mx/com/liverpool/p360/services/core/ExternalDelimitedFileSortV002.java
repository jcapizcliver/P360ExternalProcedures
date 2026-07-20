package mx.com.liverpool.p360.services.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * ExternalDelimitedFileSortV002
 *
 * External sort usando SimpleDelimitedFileParser.
 *
 * Caso default para tu CSV:
 *   header:
 *     external,internal,previous,proposalId,currentStatus,prevStatus,externalStatus
 *
 *   salida default:
 *     proposalId,currentStatus,prevStatus,externalStatus
 *
 * Uso:
 *   javac -encoding UTF-8 -d bin SimpleDelimitedFileParser.java ExternalDelimitedFileSortV002.java
 *
 *   java -Xmx512m -cp bin mx.com.liverpool.p360.services.core.ExternalDelimitedFileSortV002 \
 *     --in entrada.csv \
 *     --out salida_ordenada.csv \
 *     --tmp ./tmp_external_sort_v002
 *
 * Opcional:
 *   --key proposalId
 *   --last-cols 4
 *   --columns proposalId,currentStatus,prevStatus,externalStatus
 *   --chunk-lines 200000
 *   --min-cols 7
 */
public class ExternalDelimitedFileSortV002 {

    private static final int DEFAULT_CHUNK_LINES = 200_000;

    public static void main(String[] args) throws Exception {
        Config cfg = Config.parse(args);

        Files.createDirectories(cfg.tmpDir);

        SortStats stats = new ExternalDelimitedFileSortV002().externalSort(cfg);

        System.out.println("OK external projected sort");
        System.out.println("input=" + cfg.input);
        System.out.println("output=" + cfg.output);
        System.out.println("tmpDir=" + cfg.tmpDir);
        System.out.println("rows=" + stats.rows);
        System.out.println("chunks=" + stats.chunks);
        System.out.println("keyOriginalIndex=" + stats.keyOriginalIndex);
        System.out.println("selectedOriginalIndexes=" + Arrays.toString(stats.selectedOriginalIndexes));
        System.out.println("selectedColumns=" + stats.selectedColumns);
    }

    private SortStats externalSort(Config cfg) throws IOException {
        ParseState st = new ParseState(cfg);
        List<Path> chunks = new ArrayList<>();

        try {
            SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser(
                cfg.delim,
                cfg.sep,
                cfg.esc,
                cfg.endLine,
                cfg.charset,
                values -> {
                    try {
                        String[] safeValues = normalizeRecord(values);

                        if (cfg.hasHeader && !st.headerSeen) {
                            st.headerSeen = true;
                            st.header = safeValues;
                            st.resolveHeader();
                            return;
                        }

                        if (!cfg.hasHeader && !st.headerSeen) {
                            st.headerSeen = true;
                            st.resolveNoHeader(safeValues.length);
                        }

                        st.acceptDataRow(safeValues);

                        if (st.rows.size() >= cfg.chunkLines) {
                            chunks.add(sortAndWriteChunk(st.rows, cfg, chunks.size()));
                            st.rows.clear();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            );

            parser.parse(cfg.input);

            if (!st.rows.isEmpty()) {
                chunks.add(sortAndWriteChunk(st.rows, cfg, chunks.size()));
                st.rows.clear();
            }

            mergeChunks(chunks, cfg, st);
            return new SortStats(st.dataRows, chunks.size(), st.keyOriginalIndex, st.selectedOriginalIndexes, st.selectedColumns);
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException)e.getCause();
            }
            throw e;
        } finally {
            if (!cfg.keepTemp) {
                cleanupQuietly(chunks);
            }
        }
    }

    /**
     * Si el archivo trae CRLF pero se parsea con endLine LF, el \r queda pegado al último campo.
     * Lo removemos solo del último campo para no ensuciar externalStatus.
     */
    private static String[] normalizeRecord(String[] values) {
        if (values == null || values.length == 0) {
            return new String[0];
        }

        String[] out = Arrays.copyOf(values, values.length);

        int last = out.length - 1;
        if (out[last] != null && out[last].endsWith("\r")) {
            out[last] = out[last].substring(0, out[last].length() - 1);
        }

        return out;
    }

    private static Path sortAndWriteChunk(List<Row> rows, Config cfg, int chunkNo) throws IOException {
        rows.sort(rowComparator(cfg));

        Path chunkPath = cfg.tmpDir.resolve(String.format("external_projected_sort_v002_chunk_%05d.tmp", chunkNo));
        Base64.Encoder enc = Base64.getEncoder();

        try (BufferedWriter bw = Files.newBufferedWriter(chunkPath, StandardCharsets.UTF_8)) {
            for (Row row : rows) {
                bw.write(enc.encodeToString(row.key.getBytes(StandardCharsets.UTF_8)));
                bw.write('\t');
                bw.write(enc.encodeToString(row.outputLine.getBytes(StandardCharsets.UTF_8)));
                bw.newLine();
            }
        }

        return chunkPath;
    }

    private static void mergeChunks(List<Path> chunks, Config cfg, ParseState st) throws IOException {
        Path parent = cfg.output.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        PriorityQueue<MergeRow> pq = new PriorityQueue<>(mergeComparator(cfg));
        List<ChunkReader> readers = new ArrayList<>();

        try {
            for (int i = 0; i < chunks.size(); i++) {
                ChunkReader reader = new ChunkReader(i, chunks.get(i), cfg);
                readers.add(reader);

                MergeRow first = reader.next();
                if (first != null) {
                    pq.add(first);
                }
            }

            try (BufferedWriter out = Files.newBufferedWriter(cfg.output, cfg.charset)) {
                if (cfg.hasHeader && cfg.writeHeader) {
                    out.write(toDelimitedLine(st.selectedColumns, cfg));
                    out.newLine();
                }

                while (!pq.isEmpty()) {
                    MergeRow row = pq.poll();
                    out.write(row.outputLine);
                    out.newLine();

                    MergeRow next = readers.get(row.chunkIndex).next();
                    if (next != null) {
                        pq.add(next);
                    }
                }
            }
        } finally {
            for (ChunkReader reader : readers) {
                closeQuietly(reader);
            }
        }
    }

    private static Comparator<Row> rowComparator(Config cfg) {
        return (a, b) -> {
            int c = compareKeys(a.key, a.numericKey, b.key, b.numericKey, cfg);
            if (c != 0) {
                return c;
            }
            return Long.compare(a.seq, b.seq);
        };
    }

    private static Comparator<MergeRow> mergeComparator(Config cfg) {
        return (a, b) -> {
            int c = compareKeys(a.key, a.numericKey, b.key, b.numericKey, cfg);
            if (c != 0) {
                return c;
            }

            c = Integer.compare(a.chunkIndex, b.chunkIndex);
            if (c != 0) {
                return c;
            }

            return Long.compare(a.seqInChunk, b.seqInChunk);
        };
    }

    private static int compareKeys(String a, BigDecimal aNumeric, String b, BigDecimal bNumeric, Config cfg) {
        boolean aBlank = isBlankKey(a, cfg);
        boolean bBlank = isBlankKey(b, cfg);

        if (aBlank || bBlank) {
            if (aBlank && bBlank) {
                return 0;
            }

            int blankOrder = aBlank ? -1 : 1;
            if (cfg.blankLast) {
                blankOrder = -blankOrder;
            }
            return blankOrder;
        }

        int c;

        if (cfg.keyBigDecimal) {
            if (aNumeric == null || bNumeric == null) {
                throw new IllegalStateException("Llave numérica no inicializada.");
            }
            c = aNumeric.compareTo(bNumeric);
        } else {
            String aa = cfg.caseInsensitive ? a.toLowerCase() : a;
            String bb = cfg.caseInsensitive ? b.toLowerCase() : b;
            c = aa.compareTo(bb);
        }

        return cfg.desc ? -c : c;
    }

    private static boolean isBlankKey(String value, Config cfg) {
        if (value == null) {
            return true;
        }

        if (cfg.keyBigDecimal) {
            return value.trim().isEmpty();
        }

        return value.isEmpty();
    }

    private static BigDecimal parseBigDecimalKey(String key, Config cfg) {
        if (!cfg.keyBigDecimal) {
            return null;
        }

        if (key == null || key.trim().isEmpty()) {
            return null;
        }

        try {
            return new BigDecimal(key.trim());
        } catch (NumberFormatException e) {
            String keyLabel = cfg.keyName != null ? cfg.keyName : String.valueOf(cfg.keyIndex);
            throw new IllegalArgumentException("No pude convertir la llave a BigDecimal. columna=" + keyLabel + ", valor=[" + key + "]", e);
        }
    }

    private static String toDelimitedLine(String[] values, Config cfg) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(cfg.sep);
            }
            appendDelimitedValue(sb, values[i], cfg);
        }

        return sb.toString();
    }

    private static void appendDelimitedValue(StringBuilder sb, String value, Config cfg) {
        if (value == null) {
            value = "";
        }

        boolean mustQuote =
            cfg.quoteAll ||
            value.indexOf(cfg.sep) >= 0 ||
            value.indexOf(cfg.delim) >= 0 ||
            value.indexOf('\n') >= 0 ||
            value.indexOf('\r') >= 0 ||
            value.startsWith(" ") ||
            value.endsWith(" ");

        if (!mustQuote) {
            sb.append(value);
            return;
        }

        sb.append(cfg.delim);

        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);

            if (ch == cfg.delim) {
                // CSV estándar: " se escapa como ""
                sb.append(cfg.delim).append(cfg.delim);
            } else {
                sb.append(ch);
            }
        }

        sb.append(cfg.delim);
    }

    private static String[] pad(String[] values, int minCols) {
        if (minCols <= 0 || values.length >= minCols) {
            return values;
        }
        return Arrays.copyOf(values, minCols);
    }

    private static int findColumnIndex(String[] header, String keyName) {
        if (keyName == null || keyName.isBlank()) {
            return -1;
        }

        for (int i = 0; i < header.length; i++) {
            if (keyName.equals(header[i])) {
                return i;
            }
        }

        for (int i = 0; i < header.length; i++) {
            if (keyName.equalsIgnoreCase(header[i])) {
                return i;
            }
        }

        throw new IllegalArgumentException("No encontré la columna '" + keyName + "' en el header: " + Arrays.toString(header));
    }

    private static int[] resolveSelectedIndexes(Config cfg, String[] headerOrNull, int detectedCols) {
        if (cfg.columns != null && cfg.columns.length > 0) {
            if (headerOrNull == null) {
                throw new IllegalArgumentException("--columns requiere header.");
            }

            int[] idx = new int[cfg.columns.length];
            for (int i = 0; i < cfg.columns.length; i++) {
                idx[i] = findColumnIndex(headerOrNull, cfg.columns[i]);
            }
            return idx;
        }

        int cols = Math.max(detectedCols, cfg.minCols);
        int n = cfg.lastCols;

        if (n <= 0) {
            throw new IllegalArgumentException("--last-cols debe ser mayor a 0.");
        }

        if (n > cols) {
            throw new IllegalArgumentException("--last-cols " + n + " es mayor a columnas detectadas " + cols);
        }

        int[] idx = new int[n];
        int start = cols - n;

        for (int i = 0; i < n; i++) {
            idx[i] = start + i;
        }

        return idx;
    }

    private static String[] selectValues(String[] values, int[] indexes, int minCols) {
        String[] padded = pad(values, minCols);
        String[] out = new String[indexes.length];

        for (int i = 0; i < indexes.length; i++) {
            int ix = indexes[i];
            out[i] = ix >= 0 && ix < padded.length && padded[ix] != null ? padded[ix] : "";
        }

        return out;
    }

    private static String[] selectHeaderNames(Config cfg, String[] header, int[] indexes) {
        if (header != null) {
            String[] padded = pad(header, cfg.minCols);
            return selectValues(padded, indexes, cfg.minCols);
        }

        String[] out = new String[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            out[i] = "col_" + indexes[i];
        }
        return out;
    }

    private static void cleanupQuietly(List<Path> paths) {
        for (Path p : paths) {
            try {
                Files.deleteIfExists(p);
            } catch (Exception ignored) {
                // no-op
            }
        }
    }

    private static void closeQuietly(Closeable c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Exception ignored) {
            // no-op
        }
    }

    private static final class ParseState {
        final Config cfg;
        final List<Row> rows = new ArrayList<>();
        boolean headerSeen = false;
        String[] header;
        int keyOriginalIndex = -1;
        int[] selectedOriginalIndexes;
        String[] selectedColumns;
        long seq = 0;
        long dataRows = 0;

        ParseState(Config cfg) {
            this.cfg = cfg;
        }

        void resolveHeader() {
            header = pad(header, cfg.minCols);

            if (cfg.keyIndex >= 0) {
                keyOriginalIndex = cfg.keyIndex;
            } else {
                keyOriginalIndex = findColumnIndex(header, cfg.keyName);
            }

            selectedOriginalIndexes = resolveSelectedIndexes(cfg, header, header.length);
            selectedColumns = selectHeaderNames(cfg, header, selectedOriginalIndexes);

            validateKeyIndex();
        }

        void resolveNoHeader(int detectedCols) {
            if (cfg.keyIndex < 0) {
                throw new IllegalArgumentException("Si usas --no-header debes pasar --key-index.");
            }

            keyOriginalIndex = cfg.keyIndex;
            selectedOriginalIndexes = resolveSelectedIndexes(cfg, null, Math.max(detectedCols, cfg.minCols));
            selectedColumns = selectHeaderNames(cfg, null, selectedOriginalIndexes);

            validateKeyIndex();
        }

        void validateKeyIndex() {
            if (keyOriginalIndex < 0) {
                throw new IllegalArgumentException("No se pudo resolver la columna llave.");
            }
        }

        void acceptDataRow(String[] values) {
            values = pad(values, cfg.minCols);

            String key = keyOriginalIndex < values.length && values[keyOriginalIndex] != null ? values[keyOriginalIndex] : "";

            String[] selectedValues = selectValues(values, selectedOriginalIndexes, cfg.minCols);
            String outputLine = toDelimitedLine(selectedValues, cfg);

            BigDecimal numericKey = parseBigDecimalKey(key, cfg);

            rows.add(new Row(key, numericKey, outputLine, seq++));
            dataRows++;
        }
    }

    private static final class Row {
        final String key;
        final BigDecimal numericKey;
        final String outputLine;
        final long seq;

        Row(String key, BigDecimal numericKey, String outputLine, long seq) {
            this.key = key == null ? "" : key;
            this.numericKey = numericKey;
            this.outputLine = outputLine;
            this.seq = seq;
        }
    }

    private static final class MergeRow {
        final String key;
        final BigDecimal numericKey;
        final String outputLine;
        final int chunkIndex;
        final long seqInChunk;

        MergeRow(String key, BigDecimal numericKey, String outputLine, int chunkIndex, long seqInChunk) {
            this.key = key == null ? "" : key;
            this.numericKey = numericKey;
            this.outputLine = outputLine;
            this.chunkIndex = chunkIndex;
            this.seqInChunk = seqInChunk;
        }
    }

    private static final class ChunkReader implements Closeable {
        private final int chunkIndex;
        private final BufferedReader br;
        private final Config cfg;
        private long seqInChunk = 0;
        private final Base64.Decoder dec = Base64.getDecoder();

        ChunkReader(int chunkIndex, Path path, Config cfg) throws IOException {
            this.chunkIndex = chunkIndex;
            this.br = Files.newBufferedReader(path, StandardCharsets.UTF_8);
            this.cfg = cfg;
        }

        MergeRow next() throws IOException {
            String line = br.readLine();
            if (line == null) {
                return null;
            }

            int tab = line.indexOf('\t');
            if (tab < 0) {
                throw new IOException("Chunk temporal corrupto, falta TAB en chunk " + chunkIndex);
            }

            String key = new String(dec.decode(line.substring(0, tab)), StandardCharsets.UTF_8);
            String outputLine = new String(dec.decode(line.substring(tab + 1)), StandardCharsets.UTF_8);
            BigDecimal numericKey = parseBigDecimalKey(key, cfg);

            return new MergeRow(key, numericKey, outputLine, chunkIndex, seqInChunk++);
        }

        @Override
        public void close() throws IOException {
            br.close();
        }
    }

    private static final class SortStats {
        final long rows;
        final int chunks;
        final int keyOriginalIndex;
        final int[] selectedOriginalIndexes;
        final String selectedColumns;

        SortStats(long rows, int chunks, int keyOriginalIndex, int[] selectedOriginalIndexes, String[] selectedColumns) {
            this.rows = rows;
            this.chunks = chunks;
            this.keyOriginalIndex = keyOriginalIndex;
            this.selectedOriginalIndexes = selectedOriginalIndexes;
            this.selectedColumns = Arrays.toString(selectedColumns);
        }
    }

    private static final class Config {
        Path input;
        Path output;
        Path tmpDir = Paths.get("./tmp_external_sort_v002");

        Charset charset = StandardCharsets.UTF_8;

        char delim = '"';
        char sep = ',';
        Character esc = null;
        String endLine = "\n";

        String keyName = "proposalId";
        int keyIndex = -1;

        int lastCols = 4;
        String[] columns = null;
        int minCols = 7;

        int chunkLines = DEFAULT_CHUNK_LINES;

        boolean hasHeader = true;
        boolean writeHeader = true;
        boolean desc = false;
        boolean blankLast = true;
        boolean keepTemp = false;
        boolean caseInsensitive = false;
        boolean quoteAll = true;
        boolean keyBigDecimal = false;

        static Config parse(String[] args) {
            Config cfg = new Config();

            for (int i = 0; i < args.length; i++) {
                String a = args[i];

                switch (a) {
                    case "--in":
                        cfg.input = Paths.get(requireValue(args, ++i, a));
                        break;
                    case "--out":
                        cfg.output = Paths.get(requireValue(args, ++i, a));
                        break;
                    case "--tmp":
                        cfg.tmpDir = Paths.get(requireValue(args, ++i, a));
                        break;
                    case "--key":
                        cfg.keyName = requireValue(args, ++i, a);
                        cfg.keyIndex = -1;
                        break;
                    case "--key-index":
                        cfg.keyIndex = Integer.parseInt(requireValue(args, ++i, a));
                        cfg.keyName = null;
                        break;
                    case "--last-cols":
                        cfg.lastCols = Integer.parseInt(requireValue(args, ++i, a));
                        cfg.columns = null;
                        break;
                    case "--columns":
                        cfg.columns = splitCsvOption(requireValue(args, ++i, a));
                        break;
                    case "--min-cols":
                        cfg.minCols = Integer.parseInt(requireValue(args, ++i, a));
                        break;
                    case "--chunk-lines":
                        cfg.chunkLines = Integer.parseInt(requireValue(args, ++i, a));
                        if (cfg.chunkLines <= 0) {
                            throw new IllegalArgumentException("--chunk-lines debe ser mayor a 0.");
                        }
                        break;
                    case "--charset":
                        cfg.charset = Charset.forName(requireValue(args, ++i, a));
                        break;
                    case "--sep":
                        cfg.sep = parseChar(requireValue(args, ++i, a));
                        break;
                    case "--quote":
                    case "--delim":
                        cfg.delim = parseChar(requireValue(args, ++i, a));
                        break;
                    case "--escape":
                        cfg.esc = parseChar(requireValue(args, ++i, a));
                        break;
                    case "--endline":
                        cfg.endLine = parseEndLine(requireValue(args, ++i, a));
                        break;
                    case "--no-header":
                        cfg.hasHeader = false;
                        break;
                    case "--no-write-header":
                        cfg.writeHeader = false;
                        break;
                    case "--desc":
                        cfg.desc = true;
                        break;
                    case "--blank-first":
                        cfg.blankLast = false;
                        break;
                    case "--blank-last":
                        cfg.blankLast = true;
                        break;
                    case "--keep-temp":
                        cfg.keepTemp = true;
                        break;
                    case "--case-insensitive":
                        cfg.caseInsensitive = true;
                        break;
                    case "--key-bigdecimal":
                    case "--key-numeric":
                        cfg.keyBigDecimal = true;
                        break;
                    case "--quote-all":
                        cfg.quoteAll = true;
                        break;
                    case "--quote-minimal":
                        cfg.quoteAll = false;
                        break;
                    case "--help":
                    case "-h":
                        printUsageAndExit();
                        break;
                    default:
                        throw new IllegalArgumentException("Argumento no reconocido: " + a + "\n\n" + usage());
                }
            }

            if (cfg.input == null || cfg.output == null) {
                throw new IllegalArgumentException("Faltan --in y/o --out.\n\n" + usage());
            }

            if (cfg.input.equals(cfg.output)) {
                throw new IllegalArgumentException("El archivo de entrada y salida no deben ser el mismo.");
            }

            if (cfg.minCols < 0) {
                throw new IllegalArgumentException("--min-cols no puede ser negativo.");
            }

            return cfg;
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length) {
                throw new IllegalArgumentException("Falta valor para " + option);
            }
            return args[index];
        }

        private static String[] splitCsvOption(String raw) {
            String[] arr = raw.split(",");
            for (int i = 0; i < arr.length; i++) {
                arr[i] = arr[i].trim();
            }
            return arr;
        }

        private static char parseChar(String raw) {
            if ("TAB".equalsIgnoreCase(raw)) {
                return '\t';
            }
            if ("COMMA".equalsIgnoreCase(raw)) {
                return ',';
            }
            if ("PIPE".equalsIgnoreCase(raw)) {
                return '|';
            }
            if ("SEMICOLON".equalsIgnoreCase(raw)) {
                return ';';
            }
            if ("QUOTE".equalsIgnoreCase(raw) || "DOUBLE_QUOTE".equalsIgnoreCase(raw)) {
                return '"';
            }
            if ("BACKSLASH".equalsIgnoreCase(raw)) {
                return '\\';
            }
            if (raw.length() != 1) {
                throw new IllegalArgumentException("Valor de char inválido: " + raw);
            }
            return raw.charAt(0);
        }

        private static String parseEndLine(String raw) {
            if ("LF".equalsIgnoreCase(raw) || "\\n".equals(raw)) {
                return "\n";
            }
            if ("CRLF".equalsIgnoreCase(raw) || "\\r\\n".equals(raw)) {
                return "\r\n";
            }
            if ("CR".equalsIgnoreCase(raw) || "\\r".equals(raw)) {
                return "\r";
            }
            return raw;
        }

        private static void printUsageAndExit() {
            System.out.println(usage());
            System.exit(0);
        }

        private static String usage() {
            return ""
                + "Uso:\n"
                + "  javac -encoding UTF-8 -d bin SimpleDelimitedFileParser.java ExternalDelimitedFileSortV002.java\n"
                + "  java -Xmx512m -cp bin mx.com.liverpool.p360.services.core.ExternalDelimitedFileSortV002 --in entrada.csv --out salida.csv [opciones]\n\n"
                + "Default para tu caso:\n"
                + "  ordena por proposalId y conserva solo las últimas 4 columnas:\n"
                + "  proposalId,currentStatus,prevStatus,externalStatus\n\n"
                + "Opciones principales:\n"
                + "  --key proposalId                       Nombre de columna llave. Default: proposalId\n"
                + "  --key-index 3                          Índice 0-based de llave.\n"
                + "  --last-cols 4                          Conserva últimas N columnas. Default: 4\n"
                + "  --columns proposalId,currentStatus,... Conserva columnas por nombre.\n"
                + "  --min-cols 7                           Rellena filas cortas hasta N columnas. Default: 7\n"
                + "  --chunk-lines 200000                   Registros por chunk.\n"
                + "  --tmp ./tmp_external_sort_v002         Directorio temporal.\n"
                + "  --charset UTF-8                        Charset.\n"
                + "  --endline LF|CRLF                      Fin de línea esperado por el parser. Default: LF\n"
                + "  --desc                                 Orden descendente.\n"
                + "  --blank-last                           Llaves vacías al final. Default.\n"
                + "  --blank-first                          Llaves vacías al inicio.\n"
                + "  --quote-all                            Escribe todos los campos entre comillas. Default.\n"
                + "  --quote-minimal                        Solo comilla cuando sea necesario.\n"
                + "  --key-bigdecimal                       Ordena la llave como BigDecimal, no como texto.\n"
                + "  --keep-temp                            No borra chunks temporales.\n";
        }
    }
}