package mx.com.liverpool.p360.services.core.temp.csv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

/**
 * Converts the ordered, long-form SQL Runner CSV into one wide CSV row per
 * ArticleRevision.
 *
 * <p>The input is parsed twice with the project's
 * {@link SimpleDelimitedFileParser}. The first pass discovers the
 * characteristics and the second pass streams the articles into the output.
 * The complete input is never retained in memory.</p>
 */
public final class SimpleDelimitedFileTableParser {

    private static final String ARTICLE_REVISION_ID =
            "ArticleRevisionID";
    private static final String ARTICLE_IDENTIFIER =
            "ArticleRevision.Identifier";
    private static final String EAN =
            "ArticleDetail.EAN";
    private static final String SKU =
            "SKU";
    private static final String CHARACTERISTIC_POSITION =
            "CharacteristicPosition";
    private static final String CHARACTERISTIC_ID =
            "CharacteristicID";
    private static final String CHARACTERISTIC_IDENTIFIER =
            "CharacteristicIdentifier";
    private static final String CHARACTERISTIC_VALUE =
            "CharacteristicValue";
    private static final String CHARACTERISTIC_LOOKUP_CODE =
            "CharacteristicLookupCode";
    private static final String CHARACTERISTIC_LOOKUP_NAME =
            "CharacteristicLookupName";

    private static final List<String> REQUIRED_COLUMNS =
            Arrays.asList(
                    ARTICLE_REVISION_ID,
                    ARTICLE_IDENTIFIER,
                    EAN,
                    SKU,
                    CHARACTERISTIC_POSITION,
                    CHARACTERISTIC_ID,
                    CHARACTERISTIC_IDENTIFIER,
                    CHARACTERISTIC_VALUE);

    private static final String MULTI_VALUE_SEPARATOR = " | ";
    private static final int EXCEL_MAX_CELL_CHARACTERS = 32767;

    private SimpleDelimitedFileTableParser() {
    }

    /**
     * Usage:
     * java mx.com.liverpool.p360.services.core.temp.csv.SimpleDelimitedFileTableParser
     * input.csv output.csv [code-name|code|name]
     */
    public static void main(String[] args) throws Exception {
//        if (args.length != 2) {
//            System.err.println(
//                    "Usage: java "
//                    + SimpleDelimitedFileTableParser.class.getName()
//                    + " <ordered-input.csv> <output.csv>");
//            System.exit(2);
//            return;
//        }
    	String templateID = "EU4-113198";
//    	String templateID = "EU4-4730318";
//    	String templateID = "EU4-5606796";
    	args = new String[] { "C:\\opt\\LVP\\desorden\\PROD\\" + templateID + ".csv", "C:\\opt\\LVP\\desorden\\PROD\\damy_" + templateID + ".csv" };
    	
        ConversionResult result =
                convert(
                        Paths.get(args[0]),
                        Paths.get(args[1]));

        System.out.println(result);
    }

    public static ConversionResult convert(
            Path inputFile,
            Path outputFile)
            throws IOException {

        return convert(
                inputFile,
                outputFile,
                LookupDisplay.CODE_AND_NAME);
    }

    public static ConversionResult convert(
            Path inputFile,
            Path outputFile,
            LookupDisplay lookupDisplay)
            throws IOException {

        Objects.requireNonNull(
                lookupDisplay,
                "lookupDisplay");

        Path input =
                requireInputFile(inputFile);
        Path output =
                requireDifferentOutput(input, outputFile);

        SchemaScan schema = scanSchema(input);

        Path parent = output.getParent();

        if (parent == null) {
            throw new IOException(
                    "Could not determine output directory: "
                    + output);
        }

        Files.createDirectories(parent);

        Path temporaryOutput =
                Files.createTempFile(
                        parent,
                        "." + output.getFileName() + ".",
                        ".tmp");

        boolean completed = false;

        try {
            ConversionResult result =
                    buildWideTable(
                            input,
                            temporaryOutput,
                            schema,
                            lookupDisplay);

            moveIntoPlace(temporaryOutput, output);
            completed = true;
            return result;
        } finally {
            if (!completed) {
                Files.deleteIfExists(temporaryOutput);
            }
        }
    }

    private static Path requireInputFile(Path inputFile) {
        Objects.requireNonNull(inputFile, "inputFile");

        Path input =
                inputFile.toAbsolutePath().normalize();

        if (!Files.isRegularFile(input)) {
            throw new IllegalArgumentException(
                    "Input file does not exist or is not a file: "
                    + input);
        }

        return input;
    }

    private static Path requireDifferentOutput(
            Path input,
            Path outputFile)
            throws IOException {

        Objects.requireNonNull(outputFile, "outputFile");

        Path output =
                outputFile.toAbsolutePath().normalize();

        if (input.equals(output)) {
            throw new IllegalArgumentException(
                    "Input and output must be different files");
        }

        if (Files.exists(output)
                && Files.isSameFile(input, output)) {

            throw new IllegalArgumentException(
                    "Input and output resolve to the same file");
        }

        return output;
    }

    private static SchemaScan scanSchema(Path input) {
        SchemaScan scan = new SchemaScan(input);
        parse(input, scan::accept);
        scan.finish();
        return scan;
    }

    private static ConversionResult buildWideTable(
            Path input,
            Path temporaryOutput,
            SchemaScan schema,
            LookupDisplay lookupDisplay)
            throws IOException {

        try (BufferedWriter writer =
                     Files.newBufferedWriter(
                             temporaryOutput,
                             StandardCharsets.UTF_8,
                             StandardOpenOption.WRITE,
                             StandardOpenOption.TRUNCATE_EXISTING)) {

            writer.write('\uFEFF');
            writeHeader(writer, schema.characteristics);

            OutputBuilder builder =
                    new OutputBuilder(
                            input,
                            writer,
                            schema,
                            lookupDisplay);

            try {
                parse(input, builder::accept);
                builder.finish();
            } catch (UncheckedIOException exception) {
                throw exception.getCause();
            }

            return new ConversionResult(
                    input,
                    schema.dataRecordCount,
                    builder.articleCount,
                    schema.characteristics.size(),
                    builder.nonEmptyValueCount);
        }
    }

    private static void parse(
            Path input,
            SimpleDelimitedFileParser.LineProcessor processor) {

        SimpleDelimitedFileParser parser =
                new SimpleDelimitedFileParser(
                        '"',
                        ',',
                        Character.valueOf('\\'),
                        "\n",
                        StandardCharsets.UTF_8,
                        processor);

        parser.parse(input);
    }

    private static final class SchemaScan {

        private final Path input;
        private final TreeMap<Integer, Characteristic> byPosition =
                new TreeMap<Integer, Characteristic>();

        private Header header;
        private List<Characteristic> characteristics;
        private long physicalRecord;
        private long dataRecordCount;

        private SchemaScan(Path input) {
            this.input = input;
        }

        private void accept(String[] parsedValues) {
            physicalRecord++;

            if (isEmptyRecord(parsedValues)) {
                return;
            }

            if (header == null) {
                header = Header.from(input, parsedValues);
                return;
            }

            String[] values =
                    header.normalize(
                            input,
                            physicalRecord,
                            parsedValues);

            dataRecordCount++;

            String positionText =
                    header.get(
                            values,
                            CHARACTERISTIC_POSITION);

            if (positionText.isEmpty()) {
                return;
            }

            int position =
                    parsePositivePosition(
                            input,
                            physicalRecord,
                            positionText);

            String characteristicID =
                    requiredValue(
                            input,
                            physicalRecord,
                            CHARACTERISTIC_ID,
                            header.get(
                                    values,
                                    CHARACTERISTIC_ID));

            String identifier =
                    requiredValue(
                            input,
                            physicalRecord,
                            CHARACTERISTIC_IDENTIFIER,
                            header.get(
                                    values,
                                    CHARACTERISTIC_IDENTIFIER));

            Characteristic current =
                    new Characteristic(
                            position,
                            characteristicID,
                            identifier);

            Characteristic previous =
                    byPosition.putIfAbsent(
                            Integer.valueOf(position),
                            current);

            if (previous != null
                    && (!previous.id.equals(characteristicID)
                    || !previous.identifier.equals(identifier))) {

                throw malformed(
                        input,
                        physicalRecord,
                        "Characteristic position "
                        + position
                        + " changed from "
                        + previous
                        + " to "
                        + current);
            }
        }

        private void finish() {
            if (header == null) {
                throw malformed(
                        input,
                        0L,
                        "Input does not contain a header");
            }

            Set<String> identifiers = new HashSet<String>();
            Set<String> ids = new HashSet<String>();

            for (Characteristic characteristic :
                    byPosition.values()) {

                if (!identifiers.add(
                            characteristic.identifier)) {

                    throw malformed(
                            input,
                            0L,
                            "Duplicate characteristic identifier: "
                            + characteristic.identifier);
                }

                if (!ids.add(characteristic.id)) {
                    throw malformed(
                            input,
                            0L,
                            "CharacteristicID appears in more than "
                            + "one position: "
                            + characteristic.id);
                }
            }

            characteristics =
                    new ArrayList<Characteristic>(
                            byPosition.values());

            for (int index = 0;
                    index < characteristics.size();
                    index++) {

                characteristics.get(index).outputIndex = index;
            }
        }
    }

    private static final class OutputBuilder {

        private final Path input;
        private final BufferedWriter writer;
        private final SchemaScan schema;
        private final LookupDisplay lookupDisplay;
        private final Map<Integer, Characteristic> byPosition =
                new LinkedHashMap<Integer, Characteristic>();

        private Header header;
        private ArticleRow currentArticle;
        private long physicalRecord;
        private long articleCount;
        private long nonEmptyValueCount;

        private OutputBuilder(
                Path input,
                BufferedWriter writer,
                SchemaScan schema,
                LookupDisplay lookupDisplay) {

            this.input = input;
            this.writer = writer;
            this.schema = schema;
            this.lookupDisplay = lookupDisplay;

            for (Characteristic characteristic :
                    schema.characteristics) {

                byPosition.put(
                        Integer.valueOf(
                                characteristic.position),
                        characteristic);
            }
        }

        private void accept(String[] parsedValues) {
            physicalRecord++;

            if (isEmptyRecord(parsedValues)) {
                return;
            }

            if (header == null) {
                header = Header.from(input, parsedValues);

                if (!header.columns.equals(
                            schema.header.columns)) {

                    throw malformed(
                            input,
                            physicalRecord,
                            "Header changed between parser passes");
                }

                return;
            }

            String[] values =
                    header.normalize(
                            input,
                            physicalRecord,
                            parsedValues);

            String articleRevisionID =
                    requiredValue(
                            input,
                            physicalRecord,
                            ARTICLE_REVISION_ID,
                            header.get(
                                    values,
                                    ARTICLE_REVISION_ID));

            String articleIdentifier =
                    header.get(values, ARTICLE_IDENTIFIER);
            String ean = header.get(values, EAN);
            String sku = header.get(values, SKU);

            if (currentArticle == null
                    || !currentArticle.articleRevisionID.equals(
                            articleRevisionID)) {

                flushCurrentArticle();

                currentArticle =
                        new ArticleRow(
                                articleRevisionID,
                                articleIdentifier,
                                ean,
                                sku,
                                schema.characteristics.size());
            } else {
                currentArticle.verifyBaseValues(
                        input,
                        physicalRecord,
                        articleIdentifier,
                        ean,
                        sku);
            }

            String positionText =
                    header.get(
                            values,
                            CHARACTERISTIC_POSITION);

            if (positionText.isEmpty()) {
                return;
            }

            int position =
                    parsePositivePosition(
                            input,
                            physicalRecord,
                            positionText);

            Characteristic characteristic =
                    byPosition.get(Integer.valueOf(position));

            if (characteristic == null) {
                throw malformed(
                        input,
                        physicalRecord,
                        "Unknown characteristic position: "
                        + position);
            }

            String characteristicID =
                    header.get(values, CHARACTERISTIC_ID);
            String identifier =
                    header.get(
                            values,
                            CHARACTERISTIC_IDENTIFIER);

            if (!characteristic.id.equals(characteristicID)
                    || !characteristic.identifier.equals(
                            identifier)) {

                throw malformed(
                        input,
                        physicalRecord,
                        "Characteristic metadata does not match "
                        + characteristic);
            }

            String rawValue =
                    header.get(
                            values,
                            CHARACTERISTIC_VALUE);

            String lookupCode =
                    header.getOptional(
                            values,
                            CHARACTERISTIC_LOOKUP_CODE);

            String lookupName =
                    header.getOptional(
                            values,
                            CHARACTERISTIC_LOOKUP_NAME);

            String value =
                    resolveCharacteristicValue(
                            rawValue,
                            lookupCode,
                            lookupName,
                            lookupDisplay);

            if (!value.isEmpty()) {
                currentArticle.appendValue(
                        input,
                        physicalRecord,
                        characteristic.outputIndex,
                        value);
                nonEmptyValueCount++;
            }
        }

        private void finish() {
            flushCurrentArticle();
        }

        private void flushCurrentArticle() {
            if (currentArticle == null) {
                return;
            }

            try {
                writeArticle(writer, currentArticle);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }

            articleCount++;
            currentArticle = null;
        }
    }

    private static final class Header {

        private final List<String> columns;
        private final Map<String, Integer> indexes;

        private Header(
                List<String> columns,
                Map<String, Integer> indexes) {

            this.columns = columns;
            this.indexes = indexes;
        }

        private static Header from(
                Path input,
                String[] rawHeader) {

            List<String> columns =
                    new ArrayList<String>(
                            rawHeader.length);
            Map<String, Integer> indexes =
                    new LinkedHashMap<String, Integer>();

            for (int index = 0;
                    index < rawHeader.length;
                    index++) {

                String name =
                        rawHeader[index] == null
                                ? ""
                                : rawHeader[index];

                if (index == 0
                        && !name.isEmpty()
                        && name.charAt(0) == '\uFEFF') {

                    name = name.substring(1);
                }

                if (name.isEmpty()) {
                    throw malformed(
                            input,
                            1L,
                            "Empty header at column "
                            + (index + 1));
                }

                Integer previous =
                        indexes.put(
                                name,
                                Integer.valueOf(index));

                if (previous != null) {
                    throw malformed(
                            input,
                            1L,
                            "Duplicate header: " + name);
                }

                columns.add(name);
            }

            for (String required : REQUIRED_COLUMNS) {
                if (!indexes.containsKey(required)) {
                    throw malformed(
                            input,
                            1L,
                            "Missing required column: "
                            + required);
                }
            }

            return new Header(columns, indexes);
        }

        private String[] normalize(
                Path input,
                long record,
                String[] parsedValues) {

            if (parsedValues.length > columns.size()) {
                throw malformed(
                        input,
                        record,
                        "Record contains "
                        + parsedValues.length
                        + " fields; header contains "
                        + columns.size());
            }

            String[] values =
                    Arrays.copyOf(
                            parsedValues,
                            columns.size());

            for (int index = 0;
                    index < values.length;
                    index++) {

                if (values[index] == null) {
                    values[index] = "";
                }
            }

            return values;
        }

        private String get(
                String[] values,
                String columnName) {

            return values[
                    indexes.get(columnName).intValue()];
        }

        private String getOptional(
                String[] values,
                String columnName) {

            Integer index = indexes.get(columnName);

            return index == null
                    ? ""
                    : values[index.intValue()];
        }
    }

    private static final class Characteristic {

        private final int position;
        private final String id;
        private final String identifier;
        private int outputIndex;

        private Characteristic(
                int position,
                String id,
                String identifier) {

            this.position = position;
            this.id = id;
            this.identifier = identifier;
        }

        @Override
        public String toString() {
            return identifier
                    + " (position="
                    + position
                    + ", id="
                    + id
                    + ")";
        }
    }

    private static final class ArticleRow {

        private final String articleRevisionID;
        private final String articleIdentifier;
        private final String ean;
        private final String sku;
        private final StringBuilder[] values;

        private ArticleRow(
                String articleRevisionID,
                String articleIdentifier,
                String ean,
                String sku,
                int characteristicCount) {

            this.articleRevisionID = articleRevisionID;
            this.articleIdentifier = articleIdentifier;
            this.ean = ean;
            this.sku = sku;
            this.values =
                    new StringBuilder[
                            characteristicCount];
        }

        private void verifyBaseValues(
                Path input,
                long record,
                String currentIdentifier,
                String currentEAN,
                String currentSKU) {

            if (!Objects.equals(
                        articleIdentifier,
                        currentIdentifier)
                    || !Objects.equals(ean, currentEAN)
                    || !Objects.equals(sku, currentSKU)) {

                throw malformed(
                        input,
                        record,
                        "ArticleRevisionID "
                        + articleRevisionID
                        + " has inconsistent Identifier, "
                        + "EAN or SKU");
            }
        }

        private void appendValue(
                Path input,
                long record,
                int outputIndex,
                String value) {

            if (values[outputIndex] == null) {
                values[outputIndex] =
                        new StringBuilder(value);
            } else {
                values[outputIndex]
                        .append(MULTI_VALUE_SEPARATOR)
                        .append(value);
            }

            if (values[outputIndex].length()
                    > EXCEL_MAX_CELL_CHARACTERS) {

                throw malformed(
                        input,
                        record,
                        "ArticleRevisionID "
                        + articleRevisionID
                        + " exceeds Excel's cell limit at "
                        + "characteristic output index "
                        + outputIndex);
            }
        }
    }

    private static void writeHeader(
            BufferedWriter writer,
            List<Characteristic> characteristics)
            throws IOException {

        List<String> cells =
                new ArrayList<String>(
                        3 + characteristics.size());

        cells.add(ARTICLE_IDENTIFIER);
        cells.add(EAN);
        cells.add(SKU);

        for (Characteristic characteristic :
                characteristics) {

            cells.add(characteristic.identifier);
        }

        writeCsvRow(writer, cells);
    }

    private static void writeArticle(
            BufferedWriter writer,
            ArticleRow article)
            throws IOException {

        List<String> cells =
                new ArrayList<String>(
                        3 + article.values.length);

        cells.add(excelSafeText(
                article.articleIdentifier));
        cells.add(excelExactNumericText(article.ean));
        cells.add(excelExactNumericText(article.sku));

        for (StringBuilder value : article.values) {
            cells.add(
                    excelSafeText(
                            value == null
                                    ? ""
                                    : value.toString()));
        }

        writeCsvRow(writer, cells);
    }

    private static void writeCsvRow(
            BufferedWriter writer,
            List<String> cells)
            throws IOException {

        for (int index = 0;
                index < cells.size();
                index++) {

            if (index > 0) {
                writer.write(',');
            }

            writer.write('"');
            writer.write(
                    onePhysicalLine(cells.get(index))
                            .replace("\"", "\"\""));
            writer.write('"');
        }

        // The project parser is configured with LF. A CR before LF is treated
        // as junk after a closing quote, so output LF records as well.
        writer.write('\n');
    }

    private static String excelSafeText(String value) {
        String normalized = onePhysicalLine(value);
        int index = 0;

        while (index < normalized.length()
                && Character.isWhitespace(
                        normalized.charAt(index))) {

            index++;
        }

        if (index < normalized.length()) {
            char first = normalized.charAt(index);

            if (first == '='
                    || first == '+'
                    || first == '-'
                    || first == '@') {

                return "'" + normalized;
            }
        }

        return normalized;
    }

    private static String excelExactNumericText(
            String value) {

        String normalized = onePhysicalLine(value);

        if (normalized.matches("[0-9]+")) {
            return "=\"" + normalized + "\"";
        }

        return excelSafeText(normalized);
    }

    private static String onePhysicalLine(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\r\n", " ")
                .replace('\r', ' ')
                .replace('\n', ' ');
    }

    private static boolean isEmptyRecord(
            String[] values) {

        return values == null
                || values.length == 0
                || (values.length == 1
                && (values[0] == null
                || values[0].isEmpty()));
    }

    private static int parsePositivePosition(
            Path input,
            long record,
            String value) {

        try {
            int position = Integer.parseInt(value);

            if (position < 1) {
                throw new NumberFormatException();
            }

            return position;
        } catch (NumberFormatException exception) {
            throw malformed(
                    input,
                    record,
                    "Invalid CharacteristicPosition: "
                    + value);
        }
    }

    private static String requiredValue(
            Path input,
            long record,
            String column,
            String value) {

        if (value == null || value.isEmpty()) {
            throw malformed(
                    input,
                    record,
                    "Empty required value: " + column);
        }

        return value;
    }

    private static String resolveCharacteristicValue(
            String rawValue,
            String lookupCode,
            String lookupName,
            LookupDisplay lookupDisplay) {

        boolean hasCode =
                lookupCode != null
                && !lookupCode.isEmpty();
        boolean hasName =
                lookupName != null
                && !lookupName.isEmpty();

        if (!hasCode && !hasName) {
            return rawValue == null ? "" : rawValue;
        }

        switch (lookupDisplay) {
            case CODE:
                return hasCode
                        ? lookupCode
                        : lookupName;

            case NAME:
                return hasName
                        ? lookupName
                        : lookupCode;

            case CODE_AND_NAME:
                if (!hasCode) {
                    return lookupName;
                }

                if (!hasName
                        || lookupCode.equals(lookupName)) {

                    return lookupCode;
                }

                return lookupCode
                        + " - "
                        + lookupName;

            default:
                throw new IllegalStateException(
                        "Unsupported lookup display: "
                        + lookupDisplay);
        }
    }

    private static IllegalStateException malformed(
            Path input,
            long record,
            String message) {

        return new IllegalStateException(
                message
                + " [file="
                + input
                + ", record="
                + record
                + "]");
    }

    private static void moveIntoPlace(
            Path temporaryOutput,
            Path output)
            throws IOException {

        try {
            Files.move(
                    temporaryOutput,
                    output,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(
                    temporaryOutput,
                    output,
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public enum LookupDisplay {
        CODE_AND_NAME,
        CODE,
        NAME;

        private static LookupDisplay fromArgument(
                String value) {

            if ("code-name".equalsIgnoreCase(value)
                    || "code_and_name".equalsIgnoreCase(
                            value)) {

                return CODE_AND_NAME;
            }

            if ("code".equalsIgnoreCase(value)) {
                return CODE;
            }

            if ("name".equalsIgnoreCase(value)) {
                return NAME;
            }

            throw new IllegalArgumentException(
                    "Invalid lookup display '"
                    + value
                    + "'. Expected code-name, code or name");
        }
    }

    public static final class ConversionResult {

        private final Path input;
        private final long inputRows;
        private final long outputRows;
        private final int characteristicColumns;
        private final long nonEmptyValues;

        private ConversionResult(
                Path input,
                long inputRows,
                long outputRows,
                int characteristicColumns,
                long nonEmptyValues) {

            this.input = input;
            this.inputRows = inputRows;
            this.outputRows = outputRows;
            this.characteristicColumns =
                    characteristicColumns;
            this.nonEmptyValues = nonEmptyValues;
        }

        public Path getInput() {
            return input;
        }

        public long getInputRows() {
            return inputRows;
        }

        public long getOutputRows() {
            return outputRows;
        }

        public int getCharacteristicColumns() {
            return characteristicColumns;
        }

        public long getNonEmptyValues() {
            return nonEmptyValues;
        }

        @Override
        public String toString() {
            return "ConversionResult{"
                    + "input="
                    + input
                    + ", inputRows="
                    + inputRows
                    + ", outputRows="
                    + outputRows
                    + ", characteristicColumns="
                    + characteristicColumns
                    + ", nonEmptyValues="
                    + nonEmptyValues
                    + '}';
        }
    }
}
