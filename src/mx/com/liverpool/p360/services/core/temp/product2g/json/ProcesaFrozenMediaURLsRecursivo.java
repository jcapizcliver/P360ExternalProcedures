package mx.com.liverpool.p360.services.core.temp.product2g.json;

public class ProcesaFrozenMediaURLsRecursivo {

    private static final java.util.concurrent.atomic.AtomicLong SEQ = new java.util.concurrent.atomic.AtomicLong();

    public static void main(String[] args) {
        if(args == null || args.length < 5) {
            System.err.println("Uso:");
            System.err.println("java ... ProcesaFrozenMediaURLsRecursivo <rootDirectory> <baseUrl> <encodedBasicAuth> <templatesCacheDirectory> <doDeleteInputFile> [x]");
            return;
        }

        String rootDirectory = args[0];
        String baseUrl = args[1];
        String encoded = args[2];
        String templatesCacheDirectory = args[3];
        String doDeleteInputFile = args[4];
        boolean x = args.length > 5 && Boolean.parseBoolean(args[5]);

        java.nio.file.Path root = java.nio.file.Paths.get(rootDirectory);

        if(!java.nio.file.Files.exists(root)) {
            System.err.println("No existe rootDirectory: " + root);
            return;
        }

        java.util.List<java.nio.file.Path> files;

        try {
            files = listarArchivosDelMasViejoAlMasNuevo(root);
        } catch(java.io.IOException e) {
            e.printStackTrace();
            return;
        }

        System.out.println("Archivos a procesar: " + files.size());

        int ok = 0;
        int error = 0;
        int skipped = 0;

        for(java.nio.file.Path file : files) {
            java.nio.file.Path doneFile = java.nio.file.Paths.get(file.toString() + ".done");

            if(java.nio.file.Files.exists(doneFile)) {
                skipped++;
                continue;
            }

            System.out.println("Procesando: " + file);

            try {
                String rawFile = leerArchivoCompleto(file);

                org.json.JSONObject rootRequest = new org.json.JSONObject(rawFile);

                if(!rootRequest.has("input")) {
                    throw new IllegalArgumentException("Archivo sin atributo input: " + file);
                }

                mx.com.liverpool.p360.services.core.CreateProposalFrozenMediaURLs cp =
                    new mx.com.liverpool.p360.services.core.CreateProposalFrozenMediaURLs(
                        baseUrl,
                        encoded,
                        SEQ.incrementAndGet()
                    );

                String rawResponse = cp.doIt(
                    new String[] {
                        rootRequest.getString("input"),
                        templatesCacheDirectory,
                        doDeleteInputFile
                    },
                    x
                );

                java.nio.file.Files.write(
                    doneFile,
                    ("OK\t" + new java.util.Date() + "\tresponseLength=" + (rawResponse == null ? 0 : rawResponse.length()) + "\n")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                    java.nio.file.StandardOpenOption.WRITE
                );

                ok++;

            } catch(Exception e) {
                error++;

                System.err.println("Error procesando archivo: " + file);
                e.printStackTrace();

                java.nio.file.Path errorFile = java.nio.file.Paths.get(file.toString() + ".error.txt");

                try(java.io.PrintWriter pw = new java.io.PrintWriter(
                    new java.io.OutputStreamWriter(
                        new java.io.FileOutputStream(errorFile.toFile()),
                        java.nio.charset.StandardCharsets.UTF_8
                    )
                )) {
                    pw.println("Archivo: " + file);
                    pw.println("Fecha: " + new java.util.Date());
                    e.printStackTrace(pw);
                } catch(java.io.IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

        System.out.println("Terminado. OK=" + ok + ", ERROR=" + error + ", SKIPPED=" + skipped);
    }

    private static String leerArchivoCompleto(java.nio.file.Path file) throws java.io.IOException {
        StringBuilder sb = new StringBuilder(64 * 1024);

        try(java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
            String line;

            while((line = br.readLine()) != null) {
                sb.append(line);
            }
        }

        return sb.toString();
    }

    private static java.util.List<java.nio.file.Path> listarArchivosDelMasViejoAlMasNuevo(java.nio.file.Path root)
        throws java.io.IOException {

        try(java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(root)) {
            return stream
                .filter(java.nio.file.Files::isRegularFile)
                .filter(p -> {
                    String s = p.getFileName().toString();

                    return !s.endsWith(".done")
                        && !s.endsWith(".error.txt")
                        && !s.endsWith(".response.json")
                        && !s.endsWith(".log")
                        && !s.endsWith(".lck")
                        && !s.endsWith(".tmp")
                        && !s.endsWith(".swp");
                })
                .sorted((a, b) -> {
                    try {
                        int cmp = java.nio.file.Files.getLastModifiedTime(a)
                            .compareTo(java.nio.file.Files.getLastModifiedTime(b));

                        if(cmp != 0) {
                            return cmp;
                        }

                        return a.toString().compareTo(b.toString());
                    } catch(java.io.IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(java.util.stream.Collectors.toList());
        }
    }
}