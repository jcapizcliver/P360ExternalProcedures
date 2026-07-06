package mx.com.liverpool.p360.services.core.temp.characteristic;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CheckCharacteristicsIsActive {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		String[] pieces = null;
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<rows.length; i++) {
			pieces = rw.getRw().parseLine(rows[i]);
			sb.append(i == 0 ? "" : ",").append("\"").append(pieces[0]).append("\"");
		}
		System.out.println(sb.toString());
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier,Characteristic.Lookup->Lookup.Identifier,Characteristic.IsActive");
		qp.put("query", "Characteristic.Identifier in (" + sb.toString() + ")");
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> System.out.println(row.getJSONArray("values")), System.out::println);
	}
	
	public static final String[] rows = (
			  "AlcanceVaD,IntensidadDeLuzLOV,AlcanceVaD,LOOKUP\r\n"
			+ "AlmacenamientoVaD,WHSTCLOV,AlmacenamientoLOV,LOOKUP\r\n"
			+ "AntideslizanteVaD,AE385LOV,AntideslizanteLOV,LOOKUP\r\n"
			+ "AparienciaVaD,AE412LOV,AparienciaLOV,LOOKUP\r\n"
			+ "AumentoVaD,AE049LOV,AumentoLOV,LOOKUP\r\n"
			+ "BrazosVaD,AE544LOV,BrazosLOV,LOOKUP\r\n"
			+ "CaducidadVaD,AE310LOV,CaducidadLOV,LOOKUP\r\n"
			+ "ConectividadVaD,AE064LOV,ConectividadLOV,LOOKUP\r\n"
			+ "CubiertaVaD,AE160LOV,CubiertaLOV,LOOKUP\r\n"
			+ "DenierVaD,AE500LOV,DenierLOV,LOOKUP\r\n"
			+ "DisplayVaD,AE095LOV,DisplayLOV,LOOKUP\r\n"
			+ "EmpaqueVaD,AE011LOV,EmpaqueLOV,LOOKUP\r\n"
			+ "EmpastadoVaD,AE216LOV,EmpastadoLOV,LOOKUP\r\n"
			+ "EscalaVaD,AE257LOV,EscalaLOV,LOOKUP\r\n"
			+ "EscoteVaD,AE144LOV,EscoteLOV,LOOKUP\r\n"
			+ "EsenciaVaD,AE443LOV,FraganciaLOV,LOOKUP\r\n"
			+ "EtapaVaD,AE212LOV,EtapaLOV,LOOKUP\r\n"
			+ "ForroVaD,AE028LOV,ForroLOV,LOOKUP\r\n"
			+ "FuncionamientoVaD,AE022LOV,FuncionamientoLOV,LOOKUP\r\n"
			+ "KilatajeVaD,AE071LOV,KilatajeLOV,LOOKUP\r\n"
			+ "LavadoVaD,AE029LOV,LavadoLOV,LOOKUP\r\n"
			+ "LongitudVaD,AE100LOV,LongitudLOV,LOOKUP\r\n"
			+ "LumenesVaD,AE284LOV,LumenesLOV,LOOKUP\r\n"
			+ "MontajeVaD,AE371LOV,MontajeLOV,LOOKUP\r\n"
			+ "PeriodicidadVaD,AE219LOV,PeriodicidadLOV,LOOKUP\r\n"
			+ "PlegableVaD,AE382LOV,PlegableLOV,LOOKUP\r\n"
			+ "PremiosVaD,AE306LOV,PremiosLOV,LOOKUP\r\n"
			+ "PresentacionVaD,AE033LOV,PresentacionLOV,LOOKUP\r\n"
			+ "ResistenciaVaD,AE106LOV,ResistenciaLOV,LOOKUP\r\n"
			+ "ResolucionVaD,AE283LOV,ResolucionLOV,LOOKUP\r\n"
			+ "RuedasVaD,AE440LOV,RuedasLOV,LOOKUP\r\n"
			+ "SAPLectorVaD,AE243LOV,LectorLOV,LOOKUP\r\n"
			+ "SoporteVaD,AE109LOV,SoporteLOV,LOOKUP\r\n"
			+ "TemaVaD,AE179LOV,TemaLOV,LOOKUP\r\n"
			+ "TipoDePrendaInferiorVaD,TipoDePrendaInferiorVaD,TipoDePrendaInferiorLOV,LOOKUP").split("\r\n");
	
}
