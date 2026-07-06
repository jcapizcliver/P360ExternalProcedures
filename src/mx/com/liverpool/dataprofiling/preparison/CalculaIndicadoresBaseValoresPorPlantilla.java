package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CalculaIndicadoresBaseValoresPorPlantilla {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final  RESTWorkshop workshop = rw.getRw();
	
	public static void main(String[] args) {
		
		long init = System.currentTimeMillis();
		System.out.println("Now collecting refs...");
		try(
			java.io.BufferedReader br = 
				new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(
							java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Attributes", "AtributosPlantillas.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8));
			java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(
							java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "prof", "AtributosPlantillasConMedicionesBase.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))
		){
			pw.println( "Template,PIM_PROD_ID,PIM_ATRIBUTO_ID,PIM_ATRIBUTO_VAL_ID,PIM_ATRIBUTO_VAL,PIM_SKU_CVE,PIM_PADRE_SKU_CVE,PIM_PROD_TIPO_NOM,EspaciosMultiples,EspaciosInicioFin,CantidadCaracteresEspeciales" );
			String line = null;
			String[] pieces = null;
			br.readLine();
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line);
				pw.println(
						workshop.serializeChunk(new Object[] {
							 pieces[0]
							,pieces[1]
							,pieces[2]
							,pieces[3]
							,pieces[4]
							,pieces[5]
							,pieces[6]
							,pieces[7]
							,pieces[4].matches(".*\\s{2,}.*")
							,pieces[4].matches("^(\\s+.*)|(.*\\s+)$")
							,caracteresEspeciales(pieces[4])
						})
					);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		/*
		 * 
		 * 	Análisis de Impacto.
		 * 
		 * 
		 */
		System.out.println("Done. " + workshop.formatTime(System.currentTimeMillis() - init));
	}
	
	private static int caracteresEspeciales(String v) {
		int cosos = 0;
		if(v != null) {
			for(int i=0; i<v.length(); i++) {
				if( !v.substring(i, i+1).matches("[A-Za-z0-9 áéíóúÁÉÍÓÚÑñüÜ]") ) {
					cosos++;
				}
			}
		}
		return cosos;
	}
	
	private static String nvl(String v) {
		return v == null ? "" : v;
	}
	
	
}
