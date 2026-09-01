package mx.com.liverpool.p360.services.core.temp.xml.local.neostream;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the real character encoding of a STEP XML before any processor has
 * side effects.
 *
 * ReceiveSTEPFile must first persist the HTTP body as raw bytes. This class
 * then decides the real charset from the bytes (BOM -> strict UTF-8 -> strict
 * Windows-1252 -> ISO-8859-1) instead of blindly trusting the XML declaration.
 *
 * Downstream parsers always receive a UTF-8-safe Path. If the original file is
 * already strict UTF-8 and its declaration is compatible with UTF-8, the raw
 * Path is reused with zero additional file copy. Otherwise a temporary UTF-8
 * canonical file is produced in streaming mode and deleted by close().
 */
public final class StepXmlCharsetNormalizer {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private static final Pattern XML_DECLARATION = Pattern.compile(
            "^\\s*<\\?xml\\s+.*?\\?>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern ENCODING_ATTRIBUTE = Pattern.compile(
            "encoding\\s*=\\s*(['\"])([^'\"]+)\\1",
            Pattern.CASE_INSENSITIVE);

    private static final int PREFIX_CHARS = 4096;
    private static final int IO_BUFFER = 64 * 1024;

    private StepXmlCharsetNormalizer() {
    }

    public static NormalizedStepFile prepare(
            Path rawPath,
            Consumer<String> logger) throws IOException {

        if (rawPath == null) {
            throw new IllegalArgumentException("STEP Path is null");
        }
        if (!Files.isRegularFile(rawPath)) {
            throw new IOException("STEP file does not exist: " + rawPath);
        }

        Consumer<String> log = logger == null ? s -> { } : logger;
        Detection detection = detect(rawPath);
        String declared = readDeclaredEncoding(rawPath, detection);

        boolean declarationMatchesUtf8 =
                declared == null || charsetEquals(declared, StandardCharsets.UTF_8);

        if (StandardCharsets.UTF_8.equals(detection.charset)
                && declarationMatchesUtf8) {

            log.accept(
                    "STEP charset: declared=" + printable(declared)
                    + ", detected=UTF-8, normalized=false");

            return new NormalizedStepFile(
                    rawPath,
                    rawPath,
                    detection.charset,
                    declared,
                    false,
                    false);
        }

        Path parent = rawPath.toAbsolutePath().getParent();
        if (parent == null) {
            parent = Path.of(".").toAbsolutePath();
        }

        String baseName = rawPath.getFileName() == null
                ? "step"
                : rawPath.getFileName().toString();

        Path normalized = Files.createTempFile(
                parent,
                baseName + ".normalized-",
                ".utf8.xml");

        boolean success = false;
        try {
            normalizeToUtf8(rawPath, normalized, detection);
            success = true;
        } finally {
            if (!success) {
                Files.deleteIfExists(normalized);
            }
        }

        log.accept(
                "STEP charset mismatch/normalization: declared="
                + printable(declared)
                + ", detected=" + detection.charset.name()
                + ", processing=UTF-8"
                + ", normalized=true"
                + ", source=" + rawPath
                + ", temp=" + normalized);

        return new NormalizedStepFile(
                rawPath,
                normalized,
                detection.charset,
                declared,
                true,
                true);
    }

    private static Detection detect(Path path) throws IOException {
        Bom bom = readBom(path);
        if (bom.charset != null) {
            return new Detection(bom.charset, bom.length);
        }

        if (canDecodeStrict(path, StandardCharsets.UTF_8, 0)) {
            return new Detection(StandardCharsets.UTF_8, 0);
        }

        if (canDecodeStrict(path, WINDOWS_1252, 0)) {
            return new Detection(WINDOWS_1252, 0);
        }

        // ISO-8859-1 maps every byte 0x00-0xFF. This is the final charset
        // fallback; XML-invalid control characters can still legitimately make
        // SAX reject the document later.
        return new Detection(StandardCharsets.ISO_8859_1, 0);
    }

    private static Bom readBom(Path path) throws IOException {
        byte[] first = new byte[4];
        int read;
        try (InputStream in = Files.newInputStream(path)) {
            read = in.read(first);
        }

        if (read >= 3
                && (first[0] & 0xff) == 0xef
                && (first[1] & 0xff) == 0xbb
                && (first[2] & 0xff) == 0xbf) {
            return new Bom(StandardCharsets.UTF_8, 3);
        }

        if (read >= 2
                && (first[0] & 0xff) == 0xff
                && (first[1] & 0xff) == 0xfe) {
            if (read >= 4 && first[2] == 0x00 && first[3] == 0x00) {
                return new Bom(Charset.forName("UTF-32LE"), 4);
            }
            return new Bom(StandardCharsets.UTF_16LE, 2);
        }

        if (read >= 2
                && (first[0] & 0xff) == 0xfe
                && (first[1] & 0xff) == 0xff) {
            return new Bom(StandardCharsets.UTF_16BE, 2);
        }

        if (read >= 4
                && first[0] == 0x00
                && first[1] == 0x00
                && (first[2] & 0xff) == 0xfe
                && (first[3] & 0xff) == 0xff) {
            return new Bom(Charset.forName("UTF-32BE"), 4);
        }

        return new Bom(null, 0);
    }

    private static boolean canDecodeStrict(
            Path path,
            Charset charset,
            int bomLength) throws IOException {

        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        try (Reader reader = new BufferedReader(
                new InputStreamReader(
                        openSkippingBom(path, bomLength),
                        decoder),
                IO_BUFFER)) {

            char[] buffer = new char[IO_BUFFER];
            while (reader.read(buffer) != -1) {
                // Validation only.
            }
            return true;

        } catch (CharacterCodingException e) {
            return false;
        }
    }

    private static String readDeclaredEncoding(
            Path path,
            Detection detection) throws IOException {

        try (Reader reader = new BufferedReader(
                new InputStreamReader(
                        openSkippingBom(path, detection.bomLength),
                        detection.charset),
                IO_BUFFER)) {

            String prefix = readPrefix(reader);
            if (prefix.startsWith("\ufeff")) {
                prefix = prefix.substring(1);
            }

            Matcher declaration = XML_DECLARATION.matcher(prefix);
            if (!declaration.find()) {
                return null;
            }

            Matcher encoding = ENCODING_ATTRIBUTE.matcher(declaration.group());
            return encoding.find() ? encoding.group(2) : null;
        }
    }

    private static void normalizeToUtf8(
            Path source,
            Path target,
            Detection detection) throws IOException {

        CharsetDecoder decoder = detection.charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        try (Reader reader = new BufferedReader(
                    new InputStreamReader(
                            openSkippingBom(source, detection.bomLength),
                            decoder),
                    IO_BUFFER);
             Writer writer = new BufferedWriter(
                    new OutputStreamWriter(
                            Files.newOutputStream(
                                    target,
                                    StandardOpenOption.TRUNCATE_EXISTING,
                                    StandardOpenOption.WRITE),
                            StandardCharsets.UTF_8),
                    IO_BUFFER)) {

            String prefix = readPrefix(reader);
            if (prefix.startsWith("\ufeff")) {
                prefix = prefix.substring(1);
            }

            writer.write(rewriteDeclaration(prefix));

            char[] buffer = new char[IO_BUFFER];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, read);
            }
        }
    }

    private static String readPrefix(Reader reader) throws IOException {
        char[] buffer = new char[PREFIX_CHARS];
        int total = 0;

        while (total < buffer.length) {
            int read = reader.read(buffer, total, buffer.length - total);
            if (read < 0) {
                break;
            }
            total += read;

            String soFar = new String(buffer, 0, total);
            int declEnd = soFar.indexOf("?>");
            if (declEnd >= 0) {
                break;
            }

            String trimmed = soFar.stripLeading();
            if (trimmed.length() >= 5
                    && !trimmed.regionMatches(true, 0, "<?xml", 0, 5)) {
                break;
            }
        }

        return new String(buffer, 0, total);
    }

    private static String rewriteDeclaration(String prefix) {
        Matcher declaration = XML_DECLARATION.matcher(prefix);

        if (!declaration.find()) {
            return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + prefix;
        }

        String original = declaration.group();
        Matcher encoding = ENCODING_ATTRIBUTE.matcher(original);
        String rewritten;

        if (encoding.find()) {
            rewritten = encoding.replaceFirst("encoding=\"UTF-8\"");
        } else {
            int close = original.lastIndexOf("?>");
            rewritten = original.substring(0, close).stripTrailing()
                    + " encoding=\"UTF-8\"?>";
        }

        return prefix.substring(0, declaration.start())
                + rewritten
                + prefix.substring(declaration.end());
    }

    private static InputStream openSkippingBom(Path path, int bomLength)
            throws IOException {
        BufferedInputStream in = new BufferedInputStream(
                Files.newInputStream(path),
                IO_BUFFER);

        long remaining = bomLength;
        while (remaining > 0) {
            long skipped = in.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
                continue;
            }
            if (in.read() < 0) {
                break;
            }
            remaining--;
        }
        return in;
    }

    private static boolean charsetEquals(String declared, Charset charset) {
        if (declared == null || declared.isBlank()) {
            return false;
        }
        try {
            return Charset.forName(declared).equals(charset);
        } catch (Exception e) {
            return false;
        }
    }

    private static String printable(String value) {
        return value == null || value.isBlank() ? "<none>" : value;
    }

    private static final class Detection {
        private final Charset charset;
        private final int bomLength;

        private Detection(Charset charset, int bomLength) {
            this.charset = charset;
            this.bomLength = bomLength;
        }
    }

    private static final class Bom {
        private final Charset charset;
        private final int length;

        private Bom(Charset charset, int length) {
            this.charset = charset;
            this.length = length;
        }
    }

    public static final class NormalizedStepFile implements AutoCloseable {
        private final Path sourcePath;
        private final Path processingPath;
        private final Charset detectedCharset;
        private final String declaredEncoding;
        private final boolean normalized;
        private final boolean deleteProcessingPath;

        private NormalizedStepFile(
                Path sourcePath,
                Path processingPath,
                Charset detectedCharset,
                String declaredEncoding,
                boolean normalized,
                boolean deleteProcessingPath) {
            this.sourcePath = sourcePath;
            this.processingPath = processingPath;
            this.detectedCharset = detectedCharset;
            this.declaredEncoding = declaredEncoding;
            this.normalized = normalized;
            this.deleteProcessingPath = deleteProcessingPath;
        }

        public Path getSourcePath() {
            return sourcePath;
        }

        public Path getProcessingPath() {
            return processingPath;
        }

        public Charset getDetectedCharset() {
            return detectedCharset;
        }

        public String getDeclaredEncoding() {
            return declaredEncoding;
        }

        public boolean isNormalized() {
            return normalized;
        }

        @Override
        public void close() throws IOException {
            if (deleteProcessingPath) {
                Files.deleteIfExists(processingPath);
            }
        }
    }
}
