package mx.com.liverpool.p360.services.core.temp.product2g.json;

public class ProcesaLasImagenesStage {

	
	public static void procesarArchivosRecursivoDelMasViejoAlMasNuevo(java.nio.file.Path root) throws java.io.IOException {
	    try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(root)) {
	        stream
	            .filter(java.nio.file.Files::isRegularFile)
	            .sorted((a, b) -> {
	                try {
	                    int cmp = java.nio.file.Files.getLastModifiedTime(a)
	                        .compareTo(java.nio.file.Files.getLastModifiedTime(b));

	                    if (cmp != 0) {
	                        return cmp;
	                    }

	                    return a.toString().compareTo(b.toString());
	                } catch (java.io.IOException e) {
	                    throw new RuntimeException(e);
	                }
	            })
	            .forEach(path -> {
	                try {
	                    procesarArchivo(path);
	                } catch (Exception e) {
	                    System.err.println("Error procesando archivo: " + path);
	                    e.printStackTrace();
	                }
	            });
	    }
	}

	private static void procesarArchivo(java.nio.file.Path path) throws java.io.IOException {
	    System.out.println("Procesando: " + path);

	    try (java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(path, java.nio.charset.StandardCharsets.UTF_8)) {
	        String line;

	        while ((line = br.readLine()) != null) {
	            // Tu lógica aquí
	        }
	    }
	}
	
}
