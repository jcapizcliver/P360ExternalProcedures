package mx.com.liverpool.dataprofiling.consistency;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;

public class ConsistenciaP360EUC {

	
	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		ConsistenciaP360EUC p = new ConsistenciaP360EUC();
		p.compare(java.nio.file.Paths.get(args[0]), java.nio.file.Paths.get(args[1]));
	}
	
	private void compare(java.nio.file.Path sourceEUC, java.nio.file.Path sourceP360) {
		long init = System.currentTimeMillis();
		java.util.Map<String, String[]> dataEUC = new java.util.HashMap<>();
		java.util.Map<String, String[]> dataP360 = new java.util.HashMap<>();
		SimpleDelimitedFileParser sdeuc = new SimpleDelimitedFileParser( '"', ',' , '\\', "\n", java.nio.charset.StandardCharsets.UTF_8,  arr -> {
			if(arr.length > 0)
				dataEUC.put(arr[0], new String[] { arr[2], arr[3], arr.length > 6 ? arr[6] : "", arr[4], arr[5] });
		});
		sdeuc.parse(sourceEUC);
		SimpleDelimitedFileParser sdp360 = new SimpleDelimitedFileParser( '"', ',' , '\\', "\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
			if(arr.length > 0)
				dataP360.put(arr[0], new String[] { arr.length > 5 ? arr[5] : "", getBusinessLabel( arr[4] ), getStatusLabel( arr[3] ), getStatusLabel( arr[1] ), getExternalStatusLabel( arr[2] ) });
		});
		sdp360.parse(sourceP360);
		System.out.println("On EUC:  " + dataEUC.size());
		System.out.println("On P360: " + dataP360.size());
		String[] entryP360 = null;
		java.util.Map<String, Object[]> missmatches = new java.util.HashMap<>();
		java.util.List<String> missingP360 = new java.util.ArrayList<>();
		java.util.List<String> missingEUC  = new java.util.ArrayList<>();
		java.util.List<String> innerJoin   = new java.util.ArrayList<>();
		Boolean[] missmatchesPositions = null;
		boolean miss = false;
		int mc = 0;
		for(java.util.Map.Entry<String, String[]> entry : dataEUC.entrySet()) {
			entryP360 = dataP360.get(entry.getKey());
			if(entryP360 == null) {
				missingP360.add(entry.getKey());
			}else {
				innerJoin.add(entry.getKey());
				missmatchesPositions = compare(entry.getValue(), entryP360);
				if( missmatchesPositions != null ) {
					missmatches.put(entry.getKey(), new Object[] { entryP360, entry.getValue(), missmatchesPositions });
					for(int i=0; i<missmatchesPositions.length; i++) {
						miss |= missmatchesPositions[i];
					}
					mc += miss ? 0 : 1;
					miss = false;
				}
			}
		}
		innerJoin.forEach( key -> {
			dataP360.remove(key);
			dataEUC.remove(key);
		});
		missingP360.addAll(dataEUC.keySet());
		missingEUC.addAll(dataP360.keySet());
		printData( missingP360, java.nio.file.Paths.get("MissingP360") );
		printData( missingEUC, java.nio.file.Paths.get("MissingEUC") );
		printDataMissmatches( missmatches, java.nio.file.Paths.get("Result") );
		System.out.println("MissingEUC: " + missingEUC.size());
		System.out.println("MissingP360: " + missingP360.size());
		System.out.println("Coincidences: " + missmatches.size());
		System.out.println("Missmatches: " + mc);
		System.out.println("Good matches: " + (missmatches.size() - mc));
		System.out.println("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}
	
	private void printDataMissmatches(java.util.Map<String, Object[]> data, java.nio.file.Path dest) {
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(dest.toFile())))){
			pw.println( rw.getRw().serializeChunk( new Object[] { 
					"Identifier", "p360Template", "p360Business", "p360PrevStatus", "p360CurrentStatus", "p360ExternalStatus"
					, "eucTemplate", "eucBusiness", "eucPrevStatus", "eucCurrentStatus", "eucExternalStatus"
					, "isDiffTemplate", "isDiffBusiness", "isDiffPrevStatus", "isDiffCurrentStatus", "isDiffExternalStatus"
					} ) );
			data.entrySet().forEach(entry -> {
				String[]  dataEUC   = (String[])  entry.getValue()[0];
				String[]  dataP360  = (String[])  entry.getValue()[1];
				Boolean[] positions = (Boolean[]) entry.getValue()[2];
				Object[] todo = new Object[16];
				todo[0] = entry.getKey();
				for(int i=0; i<dataEUC.length; i++) {
					todo[i+1] = dataEUC[i];
				}
				for(int i=0; i<dataP360.length; i++) {
					todo[i+1+dataEUC.length] = dataP360[i];
				}
				for(int i=0; i<positions.length; i++) {
					todo[i+1+dataEUC.length+dataP360.length] = positions[i];
				}
				pw.println( rw.getRw().serializeChunk( todo ) );
			});
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private void printData(java.util.List<String> data, java.nio.file.Path dest) {
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(dest.toFile())))){
			data.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private Boolean[] compare(String[] entryEUC, String[] entryP360) {
		if(entryEUC.length != entryP360.length)
			return null;
		Boolean[] missmatches = new Boolean[entryEUC.length];
		for(int i=0; i<entryEUC.length; i++) {
			missmatches[i] = !entryEUC[i].equals(entryP360[i]);
		}
		return missmatches;
	}
	
	private String getExternalStatusLabel(String key) {
		return "Aprobada".equals(key) ? "Aprobada" :
				"Borrador".equals(key) ? "Borrador" :
				"Cancelada".equals(key) ? "Cancelado" :
				"CargaDeImagen".equals(key) ? "Carga de Imagen" :
				"Eliminada".equals(key) ? "Eliminada" :
				"EnProcesoLiverpool".equals(key) ? "En Proceso Liverpool" :
				"EnRevision".equals(key) ? "En Revisión" :
				"Modificacion".equals(key) ? "Modificación" :
				"PorActualizar".equals(key) ? "Por Actualizar" :
				"PropuestaGenerada".equals(key) ? "Propuesta Generada" :
				"Rechazo".equals(key) ? "Rechazada" :
				"Repoblamiento".equals(key) ? "Repoblamiento" :
				"EnProcesoLiverpool".equals(key) ? "En Proceso Liverpool" :
				"Aprobada".equals(key) ? "Aprobada" :
				"Rechazo".equals(key) ? "Rechazada" :
				"EnRevision".equals(key) ? "En Revisión" : "";
	}
	
	private String getBusinessLabel(String key) {
		return "MKP".equals(key) ? "Marketplace" : "LVP".equals(key) ? "Liverpool" : "SBB".equals(key) ? "Suburbia" : "";
	}
	
	private String getStatusLabel(String key) {
		return 
			  "1001".equals(key) ? "Propuesta Generada"
			: "1002".equals(key) ? "Pendiente Inicio Enriquecimiento"
			: "1003".equals(key) ? "Revisi\u00f3n Compras"
			: "1004".equals(key) ? "Carga de Imagen"
			: "1005".equals(key) ? "Rechazada"
			: "1006".equals(key) ? "Por Actualizar "
			: "1007".equals(key) ? "Aprobada"
			: "1008".equals(key) ? "Modificaci\u00f3n "
			: "1009".equals(key) ? "Cancelado"
			: "1010".equals(key) ? "En Proceso Liverpool"
			: "1011".equals(key) ? "En Proceso de Env\u00edo"
			: "1020".equals(key) ? "Creaci\u00f3n de SKU"
			: "1021".equals(key) ? "Gobierno de Datos"
			: "1022".equals(key) ? "Revisi\u00f3n QA"
			: "1023".equals(key) ? "Category"
			: "1024".equals(key) ? "Rechazo Publicaci\u00f3n"
			: "1025".equals(key) ? "Eliminada"
			: "1026".equals(key) ? "En Proceso Foro"
			: "1027".equals(key) ? "Rechazo Compras"
			: "1028".equals(key) ? "Rechazo QA"
			: "1029".equals(key) ? "Rechazo Gobierno"
			: "1030".equals(key) ? "Rechazo Category"
			: "1031".equals(key) ? "Repoblamiento"
			: "1032".equals(key) ? "Excepci\u00f3n de Catalogaci\u00f3n"
			: "";
	}
	
	
}
