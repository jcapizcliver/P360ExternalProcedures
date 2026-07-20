package mx.com.liverpool.p360.services.core.temp.product2g.maintenance7;

import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class EncuentraArchivosSAPECCAPartirDeListaDeIDs {

	
	public static void main(String[] args) {
		java.util.Map<String, java.util.List<String[]>> piecesMap = new java.util.HashMap<>();
		SimpleDelimitedFileParser parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			
			if(row.length == 0) {
				return;
			}
			
			if("null".equals(row[1]) || "".equals(row[1]))
				return;
			
			java.util.List<String[]> piecesList = piecesMap.get(row[1]);
			if(piecesList == null) {
				piecesList = new java.util.ArrayList<>();
				piecesMap.put(row[1], piecesList);
			}
			piecesList.add(row);
			
		} );
		parser.parse(java.nio.file.Paths.get(args[0]));
		java.util.List<String> notFound = new java.util.ArrayList<>();
		java.util.Set<String> filesToPush = new java.util.TreeSet<>();
		parser = new SimpleDelimitedFileParser( '"', ',', '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, row -> {
			
			if(row.length == 0) {
				return;
			}
			java.util.List<String[]> piecesList = piecesMap.get(row[0]);
			if(piecesList == null) {
				notFound.add(row[0]);
				return;
			}
			for(String[] pieces : piecesList) {
				filesToPush.add(pieces[4]);
			}
			
		} );
		parser.parse(java.nio.file.Paths.get(args[1]));
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("Lekungas.csv").toFile())))){
			filesToPush.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
