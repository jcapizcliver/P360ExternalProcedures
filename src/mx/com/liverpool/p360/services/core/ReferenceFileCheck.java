package mx.com.liverpool.p360.services.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class ReferenceFileCheck implements AutoCloseable {

    private final NavigableMap<String, String> index = new TreeMap<>();
    private final Map<String, RandomAccessFile> openFiles = new HashMap<>();

    public ReferenceFileCheck() throws IOException {
        loadIndex();
    }

    private void loadIndex() throws IOException {
    	if(PropertiesManager.get("p360.contingency.reference_ean_dir") != null && java.nio.file.Files.exists(java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.reference_ean_dir"), "reference.index"))) {
	        try (BufferedReader br = new BufferedReader(new FileReader( java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.reference_ean_dir"), "reference.index").toFile()))) {
	            String line;
	            while ((line = br.readLine()) != null) {
	                String[] parts = line.split("\\|", 2);
	                if (parts.length == 2) {
	                    index.put(parts[0], parts[1]);
	                }
	            }
	        }
    	}
    }

    public boolean exists(String value, DBAccessDataStub dastub) throws IOException {
        if (value == null) return false;
        value = value.trim();
        if (value.isEmpty()) return false;

        var entry = index.floorEntry(value);
        if (entry == null) return false;

        String shardFile = entry.getValue();

        RandomAccessFile raf = openFiles.computeIfAbsent( java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.reference_ean_dir"),  shardFile).toString(), f -> {
            try {
                return new RandomAccessFile(f, "r");
            } catch (IOException e) {
                throw new UncheckedIOException(e); // para que funcione dentro de computeIfAbsent
            }
        });

        return checkOnExcept( value, binarySearchInFile(raf, value), dastub );
    }
    
    private boolean lookupValueCodeExists(String lookupIdentifier, String code, DBAccessDataStub dastub) {

    	return dastub.getLookupValueId(lookupIdentifier, code, true) != null;
    }
    
    private boolean checkOnExcept(String val, boolean prevResult, DBAccessDataStub dastub) {
    	return prevResult && !lookupValueCodeExists("EANsLiberados", val, dastub);
//    	if(!prevResult)
//    		return false;
//    	try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get( PropertiesManager.get("p360.contingency.reference_ean_dir"),  "excepts").toFile())))){
//    		String line = null;
//    		while((line = br.readLine()) != null) {
//    			if(line.equals(val)) {
//    				return false;
//    			}
//    		}
//    	}catch(java.io.IOException e) {
//    		throw new IllegalStateException(e);
//    	}
//    	return prevResult;
    }

    private boolean binarySearchInFile(RandomAccessFile raf, String target) throws IOException {
        long low = 0;
        long high = raf.length() - 1;

        while (low <= high) {
            long mid = (low + high) / 2;
            raf.seek(mid);
            raf.readLine(); // alinearse a línea completa

            String line = raf.readLine();
            if (line == null) break;

            String current = line.trim();
            int cmp = current.compareTo(target);

            if (cmp == 0) return true;
            if (cmp < 0) {
                low = raf.getFilePointer();
            } else {
                high = mid - 1;
            }
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        for (RandomAccessFile raf : openFiles.values()) {
            try {
                raf.close();
            } catch (IOException ignored) {
            }
        }
        openFiles.clear();
    }
}
