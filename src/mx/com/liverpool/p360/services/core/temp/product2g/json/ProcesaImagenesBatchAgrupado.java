package mx.com.liverpool.p360.services.core.temp.product2g.json;

public class ProcesaImagenesBatchAgrupado {

    private static final int MAX_VARIANTS_PER_BATCH = 200;

    public static void main(String[] args) {
        if(args == null || args.length == 0) {
            System.err.println("Uso: java ... ProcesaImagenesBatchAgrupado /ruta/directorio");
            return;
        }

        java.nio.file.Path root = java.nio.file.Paths.get(args[0]);

        try {
            java.util.List<java.nio.file.Path> files = listarArchivos(root);

            mx.com.liverpool.p360.services.core.LasImagenes cp =
                new mx.com.liverpool.p360.services.core.LasImagenes();

            org.json.JSONArray batchProducts = new org.json.JSONArray();
            int batchVariantCount = 0;
            int batchNumber = 0;

            for(java.nio.file.Path file : files) {
                try {
                    String raw = leerArchivoCompleto(file);

                    org.json.JSONObject rootRequest = new org.json.JSONObject(raw);
                    org.json.JSONObject inputJson = new org.json.JSONObject(rootRequest.getString("input"));

                    org.json.JSONArray products = inputJson.optJSONArray("products");
                    boolean replaceAssets = inputJson.optBoolean("replaceAssets", false);

                    if(products == null || products.length() == 0) {
                        continue;
                    }

                    for(int i = 0; i < products.length(); i++) {
                        org.json.JSONObject product = products.getJSONObject(i);
                        int variantsCount = contarVariants(product);

                        if(batchVariantCount > 0 && batchVariantCount + variantsCount > MAX_VARIANTS_PER_BATCH) {
                            batchNumber++;
                            ejecutarBatch(cp, batchProducts, true, batchNumber);

                            batchProducts = new org.json.JSONArray();
                            batchVariantCount = 0;
                        }

                        // Copia defensiva porque LasImagenes modifica el JSON con remove(...)
                        batchProducts.put(new org.json.JSONObject(product.toString()));
                        batchVariantCount += variantsCount;
                    }

                } catch(Exception e) {
                    System.err.println("Error leyendo archivo: " + file);
                    e.printStackTrace();
                }
            }

            if(batchVariantCount > 0) {
                batchNumber++;
                ejecutarBatch(cp, batchProducts, true, batchNumber);
            }

            System.out.println("Terminado. batches=" + batchNumber);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void ejecutarBatch(
        mx.com.liverpool.p360.services.core.LasImagenes cp,
        org.json.JSONArray products,
        boolean replaceAssets,
        int batchNumber
    ) {
        org.json.JSONObject request = new org.json.JSONObject()
            .put("products", products)
            .put("replaceAssets", replaceAssets);

        System.out.println("Ejecutando batch " + batchNumber + " products=" + products.length());

        String rawResponse = cp.doIt(request.toString());

        java.nio.file.Path out = java.nio.file.Paths.get("batch_" + batchNumber + "_response.json");

        try {
            java.nio.file.Files.write(
                out,
                rawResponse.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                java.nio.file.StandardOpenOption.WRITE
            );
        } catch(java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private static int contarVariants(org.json.JSONObject product) {
        org.json.JSONArray variants = product.optJSONArray("variants");
        return variants == null ? 0 : variants.length();
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

    private static java.util.List<java.nio.file.Path> listarArchivos(java.nio.file.Path root) throws java.io.IOException {
        try(java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(root)) {
            return stream
                .filter(java.nio.file.Files::isRegularFile)
                .filter(p -> {
                    String s = p.getFileName().toString();
                    return !s.endsWith(".done")
                        && !s.endsWith(".response.json")
                        && !s.endsWith(".error.txt")
                        && !s.endsWith(".log")
                        && !s.endsWith(".lck");
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