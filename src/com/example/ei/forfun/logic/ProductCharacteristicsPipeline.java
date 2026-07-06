package com.example.ei.forfun.logic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class ProductCharacteristicsPipeline {

    private static final RESTWrapper RW = new RESTWrapper();

    private static final String ACTIVE_TS = "TIMESTAMP '9999-12-31 00:00:00.0'";
    private static final int REVISION_ID = 1;
    private static final int LANGUAGE_ID = 10;

    private static final int DETAIL_IMAGE_CHARACTERISTIC_ID = 29005;
    private static final int ARTICLE_ENTITY_ID = 1000;

    // Producto. Para artículo los cambias después.
    private static final int OWNER_ENTITY_ID = 1100;
    private static final int VALUE_ENTITY_ID = 1160;

    private static final int FETCH_SIZE = 500;
    private static final int CHUNK_SIZE = 50_000;

    private static final int IDS_QUERY_TIMEOUT_SECONDS = 120;
    private static final int EXPORT_QUERY_TIMEOUT_SECONDS = 7200; // 30 min
    private static final int NETWORK_TIMEOUT_MS = 5_400_000;      // 60 min
    private static final int READ_TIMEOUT_MS = 3_600_000;         // 30 min
    private static final int HEARTBEAT_EVERY_ROWS = 1_000;

    private static final String RESUME_NO_DB_FLAG = "--resume-no-db";

    private static final ExecutorService JDBC_TIMEOUT_EXECUTOR =
        Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "jdbc-network-timeout");
                t.setDaemon(true);
                return t;
            }
        });

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Uso:");
            System.err.println("java ProductCharacteristicsPipeline <fixedQuery.sql> <outputDir> <CharacteristicIdentifier1> [CharacteristicIdentifier2] [.] [--resume-no-db]");
            System.err.println("También acepta un solo argumento CSV en la parte de características: A,B,C");
            System.err.println("Usa --resume-no-db para reanudar sin volver a descargar desde Oracle.");
            return;
        }

        Path fixedQueryFile = Paths.get(args[0]);
        Path outputDir = Paths.get(args[1]);
        boolean resumeNoDb = hasFlag(args, RESUME_NO_DB_FLAG);

        try {
            Files.createDirectories(outputDir);

            List<String> requestedCharacteristics = parseRequestedCharacteristics(args, 2);
            if (requestedCharacteristics.isEmpty()) {
                throw new IllegalArgumentException("No se recibieron características");
            }

            Path baseRawCsv = outputDir.resolve("10_base_raw.csv");
            Path baseSortedByArticleCsv = outputDir.resolve("11_base_sorted_by_article.csv");

            Path detailImagesRawCsv = outputDir.resolve("15_detail_images_raw.csv");
            Path detailImagesSortedCsv = outputDir.resolve("16_detail_images_sorted.csv");
            Path detailImagesWideCsv = outputDir.resolve("17_detail_images_wide.csv");

            Path baseWithImagesCsv = outputDir.resolve("18_base_with_images.csv");
            Path baseWithImagesSortedCsv = outputDir.resolve("19_base_with_images_sorted.csv");

            Path charsRawCsv = outputDir.resolve("20_characteristics_raw.csv");
            Path charsSortedCsv = outputDir.resolve("21_characteristics_sorted.csv");
            Path charsWideCsv = outputDir.resolve("22_characteristics_wide.csv");

            Path finalCsv = outputDir.resolve("30_final_joined.csv");

            if (!resumeNoDb) {
                String fixedQuerySql = Files.readString(fixedQueryFile, StandardCharsets.UTF_8);

                LinkedHashMap<String, Long> characteristicIds;
                try (Connection connection = openConnection()) {
                    characteristicIds = resolveCharacteristicIds(connection, requestedCharacteristics);
                }

                writeResolvedIds(outputDir.resolve("00_characteristic_ids.csv"), characteristicIds);

                String characteristicsQuerySql = buildCharacteristicsQuery(characteristicIds);
                Files.writeString(outputDir.resolve("01_characteristics_query.sql"), characteristicsQuerySql, StandardCharsets.UTF_8);

                String detailImagesQuerySql = buildDetailImagesQuery();
                Files.writeString(outputDir.resolve("02_detail_images_query.sql"), detailImagesQuerySql, StandardCharsets.UTF_8);

                System.out.println("Exportando base...");
                try (Connection connection = openConnection()) {
                    exportQueryToCsv(connection, fixedQuerySql, baseRawCsv);
                }

                System.out.println("Exportando imágenes de detalle...");
                try (Connection connection = openConnection()) {
                    exportQueryToCsv(connection, detailImagesQuerySql, detailImagesRawCsv);
                }

                System.out.println("Exportando características...");
                try (Connection connection = openConnection()) {
                    exportQueryToCsv(connection, characteristicsQuerySql, charsRawCsv);
                }
            } else {
                System.out.println("Modo resume-no-db activo: no se descargarán datos desde Oracle.");
                requireFile(baseRawCsv);
                requireFile(detailImagesRawCsv);
                requireFile(charsRawCsv);
            }

            int baseArticleIdentifierIndex = headerIndexOf(baseRawCsv, "ArticleIdentifier");

            System.out.println("Ordenando base por ArticleIdentifier...");
            externalSortCsv(baseRawCsv, baseSortedByArticleCsv, new int[] { baseArticleIdentifierIndex });
            System.out.println("Base ordenada por ArticleIdentifier: " + baseSortedByArticleCsv);

            System.out.println("Ordenando imágenes de detalle...");
            externalSortCsv(detailImagesRawCsv, detailImagesSortedCsv, new int[] { 0, 1 });
            System.out.println("Imágenes de detalle ordenadas: " + detailImagesSortedCsv);

            System.out.println("Desnormalizando imágenes de detalle...");
            denormalizeDetailImages(detailImagesSortedCsv, detailImagesWideCsv);
            System.out.println("Imágenes de detalle wide: " + detailImagesWideCsv);

            System.out.println("Juntando base + imágenes de detalle...");
            leftJoinAppendByColumn(baseSortedByArticleCsv, detailImagesWideCsv, baseWithImagesCsv, "ArticleIdentifier", "ArticleIdentifier");
            System.out.println("Base + imágenes: " + baseWithImagesCsv);

            System.out.println("Validando columna Identifier en base + imágenes...");
            int baseWithImagesIdentifierIndex = headerIndexOf(baseWithImagesCsv, "Identifier");
            System.out.println("Identifier index en base + imágenes: " + baseWithImagesIdentifierIndex);

            System.out.println("Ordenando base + imágenes por Identifier...");
            externalSortCsv(baseWithImagesCsv, baseWithImagesSortedCsv, new int[] { baseWithImagesIdentifierIndex });
            System.out.println("Base + imágenes ordenada por Identifier: " + baseWithImagesSortedCsv);

            System.out.println("Ordenando características...");
            externalSortCsv(charsRawCsv, charsSortedCsv, new int[] { 0, 1, 4, 3, 2, 5, 6 });
            System.out.println("Características ordenadas: " + charsSortedCsv);

            System.out.println("Desnormalizando características...");
            denormalizeCharacteristics(charsSortedCsv, requestedCharacteristics, charsWideCsv);
            System.out.println("Características wide: " + charsWideCsv);

            System.out.println("Juntando resultado final...");
            leftJoinAppendByColumn(baseWithImagesSortedCsv, charsWideCsv, finalCsv, "Identifier", "Identifier");

            System.out.println("Listo.");
            System.out.println("Base raw: " + baseRawCsv);
            System.out.println("Imágenes raw: " + detailImagesRawCsv);
            System.out.println("Características raw: " + charsRawCsv);
            System.out.println("Base + imágenes: " + baseWithImagesCsv);
            System.out.println("Base + imágenes sorted: " + baseWithImagesSortedCsv);
            System.out.println("Características sorted: " + charsSortedCsv);
            System.out.println("Características wide: " + charsWideCsv);
            System.out.println("Final: " + finalCsv);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (flag.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static void requireFile(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("No existe el archivo requerido para reanudar: " + file);
        }
    }

    private static String buildDetailImagesQuery() {
        return
            "select " +
            "      aa.\"Identifier\" \"ArticleIdentifier\" " +
            "    , cc.\"Value\" \"ImagenDeDetalle\" " +
            "from PIM_MASTER.\"ArticleRevision\" aa " +
            "inner join PIM_MASTER.\"ArticleCharactValue\" cc " +
            "    on aa.ID = cc.\"ArticleRevisionID\" " +
            "   and cc.\"DeletionTimestamp\" = " + ACTIVE_TS + " " +
            "   and cc.\"CharacteristicID\" = " + DETAIL_IMAGE_CHARACTERISTIC_ID + " " +
            "where " +
            "cc.\"Value\" is not null or cc.\"LookupValueID\" is not null";
    }

    private static Connection openConnection() throws SQLException {
        String user = getRequiredEnv("ORACLE_JDBC_USER");
        String password = getRequiredEnv("ORACLE_JDBC_PASSWORD");
        String url = getRequiredEnv("ORACLE_JDBC_URL");

        Properties props = new Properties();
        props.setProperty("user", user);
        props.setProperty("password", password);
        props.setProperty("oracle.jdbc.ReadTimeout", String.valueOf(READ_TIMEOUT_MS));

        Connection connection = DriverManager.getConnection(url, props);
        connection.setNetworkTimeout(JDBC_TIMEOUT_EXECUTOR, NETWORK_TIMEOUT_MS);
        connection.setAutoCommit(true);
        return connection;
    }

    private static String getRequiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Falta la env var: " + name);
        }
        return value.trim();
    }

    private static List<String> parseRequestedCharacteristics(String[] args, int startIndex) {
        List<String> result = new ArrayList<>();

        for (int i = startIndex; i < args.length; i++) {
            String value = args[i];

            if (value == null) {
                continue;
            }

            value = value.trim();

            if (value.isEmpty()) {
                continue;
            }

            if (value.startsWith("--")) {
                continue;
            }

            if (value.contains(",")) {
                for (String part : value.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                        result.add(trimmed);
                    }
                }
            } else {
                result.add(value);
            }
        }

        return result;
    }

    private static LinkedHashMap<String, Long> resolveCharacteristicIds(Connection connection, List<String> requestedCharacteristics) throws SQLException {
        String placeholders = requestedCharacteristics.stream().map(x -> "?").collect(Collectors.joining(","));

        String sql =
            "select \"CharacteristicID\", \"Identifier\" " +
            "from PIM_MAIN.\"CharacteristicRevision\" " +
            "where \"DeletionTimestamp\" = " + ACTIVE_TS + " " +
            "and \"RevisionID\" = " + REVISION_ID + " " +
            "and \"Identifier\" in (" + placeholders + ")";

        Map<String, Long> found = new HashMap<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setQueryTimeout(IDS_QUERY_TIMEOUT_SECONDS);

            for (int i = 0; i < requestedCharacteristics.size(); i++) {
                ps.setString(i + 1, requestedCharacteristics.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    found.put(rs.getString("Identifier"), rs.getLong("CharacteristicID"));
                }
            }
        }

        List<String> missing = new ArrayList<>();
        LinkedHashMap<String, Long> ordered = new LinkedHashMap<>();

        for (String characteristicIdentifier : requestedCharacteristics) {
            Long id = found.get(characteristicIdentifier);
            if (id == null) {
                missing.add(characteristicIdentifier);
            } else {
                ordered.put(characteristicIdentifier, id);
            }
        }

        if (!missing.isEmpty()) {
            throw new IllegalStateException("No se encontraron CharacteristicIDs para: " + String.join(",", missing));
        }

        return ordered;
    }

    private static void writeResolvedIds(Path output, LinkedHashMap<String, Long> characteristicIds) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(output, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(bw)) {
            pw.println("CharacteristicIdentifier,CharacteristicID");

            for (Map.Entry<String, Long> entry : characteristicIds.entrySet()) {
                pw.println(RW.getRw().serializeChunk(new String[] {
                    entry.getKey(),
                    String.valueOf(entry.getValue())
                }));
            }
        }
    }

    private static String buildCharacteristicsQuery(LinkedHashMap<String, Long> characteristicIds) {
        String inIds = characteristicIds.values().stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));

        return
            "select " +
            "      aa.\"Identifier\" \"Identifier\" " +
            "    , ww.\"Identifier\" \"CharacteristicIdentifier\" " +
            "    , bb.\"Order\" \"SortOrder\" " +
            "    , bb.\"RecordKey\" \"RecordKey\" " +
            "    , bb.\"ParentRecordKey\" \"ParentRecordKey\" " +
            "    , case " +
            "          when dd.\"Name\" is not null then dd.\"Name\" " +
            "          else bb.\"Value\" " +
            "      end \"CharacteristicValue\" " +
            "    , cc.\"Code\" \"CharacteristicValueCode\"" +
            "from PIM_MASTER.\"ArticleRevision\" aa " +
            "inner join PIM_MASTER.\"ArticleCharactValue\" bb " +
            "    on aa.ID = bb.\"ArticleRevisionID\" " +
            "   and aa.\"DeletionTimestamp\" = " + ACTIVE_TS + " " +
            "   and aa.\"RevisionID\" = " + REVISION_ID + " " +
            "   and aa.\"EntityID\" = " + OWNER_ENTITY_ID + " " +
            "   and bb.\"DeletionTimestamp\" = " + ACTIVE_TS + " " +
            "   and bb.\"EntityID\" = " + VALUE_ENTITY_ID + " " +
            "   and bb.\"CharacteristicID\" in (" + inIds + ") " +
            "left outer join PIM_MAIN.\"LookupValueRevision\" cc " +
            "    on bb.\"LookupValueID\" = cc.\"LookupValueID\" " +
            "   and cc.\"RevisionID\" = " + REVISION_ID + " " +
            "   and cc.\"DeletionTimestamp\" = " + ACTIVE_TS + " " +
            "left outer join PIM_MAIN.\"LookupValueLang\" dd " +
            "    on cc.ID = dd.\"LookupValueRevisionID\" " +
            "   and dd.\"LanguageID\" = " + LANGUAGE_ID + " " +
            "   and dd.\"DeletionTimestamp\" = " + ACTIVE_TS + " " +
            "left outer join PIM_MAIN.\"CharacteristicRevision\" ww " +
            "    on bb.\"CharacteristicID\" = ww.\"CharacteristicID\" " +
            "   and ww.\"RevisionID\" = " + REVISION_ID + " " +
            "   and ww.\"DeletionTimestamp\" = " + ACTIVE_TS;
    }

    private static void exportQueryToCsv(Connection connection, String sql, Path output) throws SQLException, IOException {
        try (Statement st = connection.createStatement();
             BufferedWriter bw = Files.newBufferedWriter(output, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(bw)) {

            st.setFetchSize(FETCH_SIZE);
            st.setQueryTimeout(EXPORT_QUERY_TIMEOUT_SECONDS);

            System.out.println("Ejecutando SQL para " + output.getFileName());

            try (ResultSet rs = st.executeQuery(sql)) {
                System.out.println("ResultSet abierto para " + output.getFileName());

                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                String[] header = new String[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    header[i - 1] = meta.getColumnLabel(i);
                }

                pw.println(RW.getRw().serializeChunk(header));

                int rowCount = 0;
                long lastBeat = System.currentTimeMillis();

                while (rs.next()) {
                    rowCount++;

                    String[] row = new String[columnCount];

                    for (int i = 1; i <= columnCount; i++) {
                        row[i - 1] = escapeVisibleNewlines(readAsString(rs, i));
                    }

                    pw.println(RW.getRw().serializeChunk(row));

                    if (rowCount % HEARTBEAT_EVERY_ROWS == 0) {
                        pw.flush();
                        long now = System.currentTimeMillis();
                        System.out.println("HEARTBEAT file=" + output.getFileName() + " rows=" + rowCount + " delta_ms=" + (now - lastBeat));
                        lastBeat = now;
                    }
                }

                pw.flush();
                System.out.println("FIN file=" + output.getFileName() + " rows=" + rowCount);
            } catch (SQLTimeoutException e) {
                System.err.println("Timeout ejecutando export sobre " + output.getFileName() + ": " + e.getMessage());
                throw e;
            }
        }
    }

    private static String readAsString(ResultSet rs, int columnIndex) throws SQLException, IOException {
        Object value = rs.getObject(columnIndex);

        if (value == null) {
            return "";
        }

        if (value instanceof Clob) {
            return readClobFully((Clob) value);
        }

        return rs.getString(columnIndex);
    }

    private static String readClobFully(Clob clob) throws SQLException, IOException {
        StringBuilder sb = new StringBuilder();

        try (Reader reader = clob.getCharacterStream()) {
            char[] buffer = new char[8192];
            int n;

            while ((n = reader.read(buffer)) != -1) {
                sb.append(buffer, 0, n);
            }
        }

        return sb.toString();
    }

    private static String escapeVisibleNewlines(String value) {
        return value == null ? "" : value.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\\n");
    }

    private static void externalSortCsv(Path input, Path output, int[] sortColumns) throws IOException {
        List<Path> chunks = new ArrayList<>();
        List<String[]> buffer = new ArrayList<>(CHUNK_SIZE);

        String[] header;

        try (BufferedReader br = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String headerLine = br.readLine();
            header = headerLine == null ? new String[0] : RW.getRw().parseLine(headerLine);

            String line;
            while ((line = br.readLine()) != null) {
                buffer.add(RW.getRw().parseLine(line));

                if (buffer.size() >= CHUNK_SIZE) {
                    spillChunk(buffer, chunks, sortColumns);
                }
            }
        }

        if (!buffer.isEmpty()) {
            spillChunk(buffer, chunks, sortColumns);
        }

        mergeChunks(chunks, output, header, sortColumns);
    }

    private static void spillChunk(List<String[]> rows, List<Path> chunks, int[] sortColumns) throws IOException {
        rows.sort(csvComparator(sortColumns));

        Path chunk = Files.createTempFile("p360_sort_", ".dat");

        try (BufferedWriter bw = Files.newBufferedWriter(chunk, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(bw)) {

            for (String[] row : rows) {
                pw.println(RW.getRw().serializeChunk(row));
            }
        }

        chunks.add(chunk);
        rows.clear();
    }

    private static void mergeChunks(List<Path> chunks, Path output, String[] header, int[] sortColumns) throws IOException {
        PriorityQueue<ChunkCursor> pq = new PriorityQueue<>((a, b) -> csvComparator(sortColumns).compare(a.row, b.row));
        List<ChunkCursor> open = new ArrayList<>();

        try (BufferedWriter bw = Files.newBufferedWriter(output, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(bw)) {

            pw.println(RW.getRw().serializeChunk(header));

            for (Path chunk : chunks) {
                BufferedReader br = Files.newBufferedReader(chunk, StandardCharsets.UTF_8);
                String line = br.readLine();

                if (line != null) {
                    ChunkCursor cursor = new ChunkCursor(br, chunk, RW.getRw().parseLine(line));
                    open.add(cursor);
                    pq.add(cursor);
                } else {
                    br.close();
                    Files.deleteIfExists(chunk);
                }
            }

            while (!pq.isEmpty()) {
                ChunkCursor current = pq.poll();

                pw.println(RW.getRw().serializeChunk(current.row));

                String next = current.reader.readLine();

                if (next != null) {
                    current.row = RW.getRw().parseLine(next);
                    pq.add(current);
                } else {
                    current.close();
                    Files.deleteIfExists(current.file);
                }
            }
        } finally {
            for (ChunkCursor cursor : open) {
                try {
                    cursor.close();
                } catch (IOException ignored) {
                }
            }

            for (Path chunk : chunks) {
                Files.deleteIfExists(chunk);
            }
        }
    }

    private static Comparator<String[]> csvComparator(int[] sortColumns) {
        return (left, right) -> {
            for (int column : sortColumns) {
                String lv = safe(left, column);
                String rv = safe(right, column);

                int cmp = lv.compareTo(rv);

                if (cmp != 0) {
                    return cmp;
                }
            }

            return 0;
        };
    }

    private static String safe(String[] row, int index) {
        if (row == null || index < 0 || index >= row.length || row[index] == null) {
            return "";
        }

        return row[index];
    }

    private static void denormalizeDetailImages(Path detailImagesSortedCsv, Path output) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(detailImagesSortedCsv, StandardCharsets.UTF_8);
             BufferedWriter bw = Files.newBufferedWriter(output, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(bw)) {

            String headerLine = br.readLine();

            if (headerLine == null) {
                pw.println(RW.getRw().serializeChunk(new String[] { "ArticleIdentifier", "ImagenesDeDetalle" }));
                return;
            }

            String[] inputHeader = RW.getRw().parseLine(headerLine);

            int idxArticleIdentifier = indexOf(inputHeader, "ArticleIdentifier");
            int idxImage = indexOf(inputHeader, "ImagenDeDetalle");

            if (idxArticleIdentifier < 0 || idxImage < 0) {
                throw new IllegalStateException("El archivo de imágenes no trae las columnas esperadas");
            }

            pw.println(RW.getRw().serializeChunk(new String[] { "ArticleIdentifier", "ImagenesDeDetalle" }));

            String currentArticleIdentifier = null;
            List<String> images = new ArrayList<>();

            String line;
            while ((line = br.readLine()) != null) {
                String[] row = RW.getRw().parseLine(line);

                String articleIdentifier = safe(row, idxArticleIdentifier);
                String image = safe(row, idxImage);

                if (currentArticleIdentifier == null) {
                    currentArticleIdentifier = articleIdentifier;
                } else if (!currentArticleIdentifier.equals(articleIdentifier)) {
                    flushDetailImagesRow(pw, currentArticleIdentifier, images);
                    images.clear();
                    currentArticleIdentifier = articleIdentifier;
                }

                if (image != null && !image.isEmpty()) {
                    images.add(image);
                }
            }

            if (currentArticleIdentifier != null) {
                flushDetailImagesRow(pw, currentArticleIdentifier, images);
            }
        }
    }

    private static void flushDetailImagesRow(PrintWriter pw, String articleIdentifier, List<String> images) {
        String joined = images.isEmpty() ? "" : String.join("|", images);

        pw.println(RW.getRw().serializeChunk(new String[] {
            articleIdentifier == null ? "" : articleIdentifier,
            joined == null ? "" : joined
        }));
    }

    private static void denormalizeCharacteristics(Path charsSortedCsv, List<String> requestedCharacteristics, Path output) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(charsSortedCsv, StandardCharsets.UTF_8);
             BufferedWriter bw = Files.newBufferedWriter(output, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(bw)) {

            String headerLine = br.readLine();

            if (headerLine == null) {
                String[] onlyHeader = new String[(requestedCharacteristics.size() * 2) + 1];
                onlyHeader[0] = "Identifier";

                int headerIdx = 1;
                for (String characteristic : requestedCharacteristics) {
                    onlyHeader[headerIdx++] = characteristic;
                    onlyHeader[headerIdx++] = characteristic + "_Code";
                }

                pw.println(RW.getRw().serializeChunk(onlyHeader));
                return;
            }

            String[] inputHeader = RW.getRw().parseLine(headerLine);

            int idxIdentifier = indexOf(inputHeader, "Identifier");
            int idxCharacteristic = indexOf(inputHeader, "CharacteristicIdentifier");
            int idxValue = indexOf(inputHeader, "CharacteristicValue");
            int idxValueCode = indexOf(inputHeader, "CharacteristicValueCode");

            if (idxIdentifier < 0 || idxCharacteristic < 0 || idxValue < 0 || idxValueCode < 0) {
                throw new IllegalStateException("El archivo de características no trae las columnas esperadas");
            }

            String[] outHeader = new String[(requestedCharacteristics.size() * 2) + 1];
            outHeader[0] = "Identifier";

            int outHeaderIdx = 1;
            for (String characteristic : requestedCharacteristics) {
                outHeader[outHeaderIdx++] = characteristic;
                outHeader[outHeaderIdx++] = characteristic + "_Code";
            }

            pw.println(RW.getRw().serializeChunk(outHeader));

            String currentIdentifier = null;
            Map<String, List<String>> valueAccumulator = new HashMap<>();
            Map<String, List<String>> codeAccumulator = new HashMap<>();

            String line;
            while ((line = br.readLine()) != null) {
                String[] row = RW.getRw().parseLine(line);

                String identifier = safe(row, idxIdentifier);
                String characteristic = safe(row, idxCharacteristic);
                String value = safe(row, idxValue);
                String valueCode = safe(row, idxValueCode);

                if (currentIdentifier == null) {
                    currentIdentifier = identifier;
                } else if (!currentIdentifier.equals(identifier)) {
                    flushWideRow(pw, currentIdentifier, requestedCharacteristics, valueAccumulator, codeAccumulator);
                    valueAccumulator.clear();
                    codeAccumulator.clear();
                    currentIdentifier = identifier;
                }

                valueAccumulator.computeIfAbsent(characteristic, k -> new ArrayList<>()).add(value);
                codeAccumulator.computeIfAbsent(characteristic, k -> new ArrayList<>()).add(valueCode);
            }

            if (currentIdentifier != null) {
                flushWideRow(pw, currentIdentifier, requestedCharacteristics, valueAccumulator, codeAccumulator);
            }
        }
    }

    private static void flushWideRow(
        PrintWriter pw,
        String identifier,
        List<String> requestedCharacteristics,
        Map<String, List<String>> valueAccumulator,
        Map<String, List<String>> codeAccumulator
    ) {
        String[] out = new String[(requestedCharacteristics.size() * 2) + 1];
        out[0] = identifier == null ? "" : identifier;

        int outIdx = 1;
        for (String characteristic : requestedCharacteristics) {
            List<String> values = valueAccumulator.get(characteristic);
            List<String> codes = codeAccumulator.get(characteristic);

            if (values == null || values.isEmpty()) {
                out[outIdx++] = "";
            } else {
                out[outIdx++] = values.stream()
                    .map(v -> v == null ? "" : v)
                    .collect(Collectors.joining(";"));
            }

            if (codes == null || codes.isEmpty()) {
                out[outIdx++] = "";
            } else {
                out[outIdx++] = codes.stream()
                    .map(v -> v == null ? "" : v)
                    .collect(Collectors.joining(";"));
            }
        }

        pw.println(RW.getRw().serializeChunk(out));
    }

    private static void leftJoinAppendByColumn(Path leftSortedCsv, Path rightSortedCsv, Path output, String leftKeyColumnName, String rightKeyColumnName) throws IOException {
        try (BufferedReader brLeft = Files.newBufferedReader(leftSortedCsv, StandardCharsets.UTF_8);
             BufferedReader brRight = Files.newBufferedReader(rightSortedCsv, StandardCharsets.UTF_8);
             BufferedWriter bw = Files.newBufferedWriter(output, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(bw)) {

            String leftHeaderLine = brLeft.readLine();
            String rightHeaderLine = brRight.readLine();

            if (leftHeaderLine == null) {
                return;
            }

            String[] leftHeader = RW.getRw().parseLine(leftHeaderLine);
            String[] rightHeader = rightHeaderLine == null ? new String[] { rightKeyColumnName } : RW.getRw().parseLine(rightHeaderLine);

            int leftKeyIndex = indexOf(leftHeader, leftKeyColumnName);
            int rightKeyIndex = indexOf(rightHeader, rightKeyColumnName);

            if (leftKeyIndex < 0) {
                throw new IllegalStateException("El archivo izquierdo no trae la columna llave: " + leftKeyColumnName);
            }

            if (rightKeyIndex < 0) {
                throw new IllegalStateException("El archivo derecho no trae la columna llave: " + rightKeyColumnName);
            }

            String[] finalHeader = concatExcludingIndex(leftHeader, rightHeader, rightKeyIndex);
            pw.println(RW.getRw().serializeChunk(finalHeader));

            String[] leftRow = readRow(brLeft);
            String[] rightRow = readRow(brRight);

            int rightTailSize = Math.max(0, rightHeader.length - 1);
            String[] emptyTail = new String[rightTailSize];
            Arrays.fill(emptyTail, "");

            while (leftRow != null) {
                String leftKey = safe(leftRow, leftKeyIndex);

                while (rightRow != null && safe(rightRow, rightKeyIndex).compareTo(leftKey) < 0) {
                    rightRow = readRow(brRight);
                }

                String[] finalRow;

                if (rightRow != null && safe(rightRow, rightKeyIndex).equals(leftKey)) {
                    finalRow = concatForFinalOutput(leftRow, valuesExcludingIndex(rightRow, rightKeyIndex));
                } else {
                    finalRow = concatForFinalOutput(leftRow, emptyTail);
                }

                pw.println(RW.getRw().serializeChunk(finalRow));
                leftRow = readRow(brLeft);
            }
        }
    }

    private static String[] readRow(BufferedReader br) throws IOException {
        String line = br.readLine();
        return line == null ? null : RW.getRw().parseLine(line);
    }

    private static String[] concatForFinalOutput(String[] left, String[] rightTail) {
        String[] out = new String[left.length + rightTail.length];
        int idx = 0;

        for (String value : left) {
            out[idx++] = value == null ? "" : value;
        }

        for (String value : rightTail) {
            out[idx++] = value == null ? "" : value;
        }

        return out;
    }

    private static String[] concatExcludingIndex(String[] left, String[] right, int excludedRightIndex) {
        String[] out = new String[left.length + Math.max(0, right.length - 1)];
        System.arraycopy(left, 0, out, 0, left.length);

        int idx = left.length;

        for (int i = 0; i < right.length; i++) {
            if (i != excludedRightIndex) {
                out[idx++] = right[i];
            }
        }

        return out;
    }

    private static String[] valuesExcludingIndex(String[] row, int excludedIndex) {
        String[] out = new String[Math.max(0, row.length - 1)];

        int idx = 0;

        for (int i = 0; i < row.length; i++) {
            if (i != excludedIndex) {
                out[idx++] = row[i];
            }
        }

        return out;
    }

    private static int headerIndexOf(Path csv, String columnName) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String headerLine = br.readLine();

            if (headerLine == null) {
                throw new IllegalStateException("El archivo no tiene header: " + csv);
            }

            String[] header = RW.getRw().parseLine(headerLine);
            int index = indexOf(header, columnName);

            if (index < 0) {
                throw new IllegalStateException("El archivo " + csv + " no trae la columna: " + columnName);
            }

            return index;
        }
    }

    private static int indexOf(String[] header, String name) {
        for (int i = 0; i < header.length; i++) {
            if (name.equals(header[i])) {
                return i;
            }
        }

        return -1;
    }

    private static final class ChunkCursor implements AutoCloseable {
        private final BufferedReader reader;
        private final Path file;
        private String[] row;

        private ChunkCursor(BufferedReader reader, Path file, String[] row) {
            this.reader = reader;
            this.file = file;
            this.row = row;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
