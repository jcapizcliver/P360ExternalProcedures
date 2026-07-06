package com.example.ei.forfun.logic;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

public final class ApprovalTimestampBrokenPipeParser {

    private static final int PIPE = '|';
    private static final int CR = '\r';
    private static final int LF = '\n';
    private static final int EOF = -1;

    private final int minSkuDigits;

    private final Deque<Integer> pushback = new ArrayDeque<>();
    private InputStream in;

    public ApprovalTimestampBrokenPipeParser(int minSkuDigits) {
        this.minSkuDigits = minSkuDigits;
    }

    public static final class Row {
        public final String sku;
        public final String fechaPrimeraAprobacionNueva;
        public final String fechaUltimaAprobacion;

        public Row(String sku, String fechaPrimeraAprobacionNueva, String fechaUltimaAprobacion) {
            this.sku = sku;
            this.fechaPrimeraAprobacionNueva = fechaPrimeraAprobacionNueva;
            this.fechaUltimaAprobacion = fechaUltimaAprobacion;
        }
    }

    public static final class Report {
        public long bytesRead;
        public long rowsWritten;
        public long candidateSkuRejected;
        public long emptyDateFields;
        public long dateFieldsRead;
    }

    @FunctionalInterface
    public interface RowConsumer {
        void accept(Row row) throws IOException;
    }

    public Report parse(Path input, RowConsumer consumer) throws IOException {
        Report report = new Report();

        try (InputStream raw = new BufferedInputStream(Files.newInputStream(input), 64 * 1024)) {
            this.in = raw;
            this.pushback.clear();

            while (true) {
                String sku = readNextSku(report);

                if (sku == null) {
                    break;
                }

                String firstDate = readDateOrEmpty(report);
                String lastDate = readDateOrEmpty(report);

                consumer.accept(new Row(sku, firstDate, lastDate));
                report.rowsWritten++;
            }
        } finally {
            this.in = null;
            this.pushback.clear();
        }

        return report;
    }

    private String readNextSku(Report report) throws IOException {
        StringBuilder digits = new StringBuilder(32);

        while (true) {
            int b = read(report);

            if (b == EOF) {
                return null;
            }

            if (isDigit(b)) {
                digits.setLength(0);
                digits.append((char) b);

                while (true) {
                    b = read(report);

                    if (b == EOF) {
                        return null;
                    }

                    if (isDigit(b)) {
                        digits.append((char) b);
                        continue;
                    }

                    if (b == PIPE && digits.length() >= minSkuDigits) {
                        return digits.toString();
                    }

                    report.candidateSkuRejected++;
                    digits.setLength(0);

                    if (b == PIPE || b == CR || b == LF) {
                        break;
                    }

                    break;
                }
            }
        }
    }

    private String readDateOrEmpty(Report report) throws IOException {
        skipCrLf(report);

        int first = read(report);

        if (first == EOF) {
            report.emptyDateFields++;
            return "";
        }

        if (first == PIPE) {
            report.emptyDateFields++;
            return "";
        }

        unread(first);

        String candidate = peekChars(19, report);

        if (candidate.length() == 19 && looksLikeDate(candidate)) {
            consumeChars(19, report);
            consumeOptionalFieldBoundary(report);
            report.dateFieldsRead++;
            return candidate;
        }

        report.emptyDateFields++;
        return "";
    }

    private void consumeOptionalFieldBoundary(Report report) throws IOException {
        int b = read(report);

        if (b == EOF) {
            return;
        }

        if (b == PIPE || b == CR || b == LF) {
            if (b == CR) {
                int next = read(report);
                if (next != LF && next != EOF) {
                    unread(next);
                }
            }
            return;
        }

        unread(b);
    }

    private void skipCrLf(Report report) throws IOException {
        while (true) {
            int b = read(report);

            if (b == CR || b == LF) {
                continue;
            }

            if (b != EOF) {
                unread(b);
            }

            return;
        }
    }

    private String peekChars(int count, Report report) throws IOException {
        int[] bytes = new int[count];
        int len = 0;

        for (int i = 0; i < count; i++) {
            int b = read(report);

            if (b == EOF) {
                break;
            }

            bytes[len++] = b;
        }

        for (int i = len - 1; i >= 0; i--) {
            unread(bytes[i]);
        }

        StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            sb.append((char) bytes[i]);
        }

        return sb.toString();
    }

    private void consumeChars(int count, Report report) throws IOException {
        for (int i = 0; i < count; i++) {
            int b = read(report);

            if (b == EOF) {
                return;
            }
        }
    }

    private int read(Report report) throws IOException {
        Integer pushed = pushback.pollFirst();

        if (pushed != null) {
            return pushed;
        }

        int b = in.read();

        if (b != EOF) {
            report.bytesRead++;
        }

        return b;
    }

    private void unread(int b) {
        if (b != EOF) {
            pushback.addFirst(b);
        }
    }

    private static boolean isDigit(int b) {
        return b >= '0' && b <= '9';
    }

    private static boolean looksLikeDate(String s) {
        return s.length() == 19
            && isDigit(s.charAt(0))
            && isDigit(s.charAt(1))
            && s.charAt(2) == '-'
            && isDigit(s.charAt(3))
            && isDigit(s.charAt(4))
            && s.charAt(5) == '-'
            && isDigit(s.charAt(6))
            && isDigit(s.charAt(7))
            && isDigit(s.charAt(8))
            && isDigit(s.charAt(9))
            && s.charAt(10) == ' '
            && isDigit(s.charAt(11))
            && isDigit(s.charAt(12))
            && s.charAt(13) == ':'
            && isDigit(s.charAt(14))
            && isDigit(s.charAt(15))
            && s.charAt(16) == ':'
            && isDigit(s.charAt(17))
            && isDigit(s.charAt(18));
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static String cleanForPipe(String s) {
        if (s == null) {
            return "";
        }

        return s.replace('|', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ');
    }
    
    private static boolean titleWritten = false;

    public static Report writeCleanFile(Path input, Path output, int minSkuDigits) throws IOException {
        ApprovalTimestampBrokenPipeParser parser = new ApprovalTimestampBrokenPipeParser(minSkuDigits);

        try (BufferedWriter bw = Files.newBufferedWriter(output, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
        	if(!titleWritten){
	            bw.write("Codigo SKU|Fecha Primera Aprobacion Nueva|Fecha Ultima Aprobacion");
	            bw.newLine();
	            titleWritten = true;
        	}

            return parser.parse(input, row -> {
                bw.write(cleanForPipe(row.sku));
                bw.write('|');
                bw.write(cleanForPipe(row.fechaPrimeraAprobacionNueva));
                bw.write('|');
                bw.write(cleanForPipe(row.fechaUltimaAprobacion));
                bw.newLine();
            });
        }
    }

    private static void échatelo(java.io.File file) throws IOException {
    	
    	Path input = file.toPath();
        Path output = Path.of("/", "u01", "stage", "Elpis");

        Report report = writeCleanFile(input, output, 7);

        System.err.println("bytesRead=" + report.bytesRead);
        System.err.println("rowsWritten=" + report.rowsWritten);
        System.err.println("\t\tcandidateSkuRejected=" + report.candidateSkuRejected);
        System.err.println("emptyDateFields=" + report.emptyDateFields);
        System.err.println("dateFieldsRead=" + report.dateFieldsRead);
        System.err.println();
        
    }
    
    private static void lalekunga(java.io.File file) throws IOException {
    	
    	if(file.isDirectory()) {
    		java.io.File[] files = file.listFiles();
    		for(int i=0; i<files.length; i++)
    			lalekunga(files[i]);
    	}else {
    		échatelo(file);
    	}
    	
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Uso:");
            System.err.println("ApprovalTimestampBrokenPipeParser 'Ruta a donde hay archivos rotos'");
            System.exit(1);
        }

        lalekunga( new java.io.File(args[0]) );
        
    }
}
