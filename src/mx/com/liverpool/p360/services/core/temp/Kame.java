package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class Kame {


	public static void main(String[] args) {
		String val1 = ",Hola me llamo \"Cerezo \",";
		String val2 = "La clave es: \\mañana,vemos\\";
		String val3 = ",Necesito aprender \"más allá\" de mis horizontes";
		String val4 = val1 + "," + val2 + "," + val3;
		String val5 = null;
		System.out.println("Values are...\n");
		System.out.println(val1);
		System.out.println(val2);
		System.out.println(val3);
		System.out.println("\n*******");
		RESTWorkshop w = new RESTWorkshop();
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\hola")))){
			pw.println(w.serializeLine(val1));
			pw.println(w.serializeLine(val2));
			pw.println(w.serializeLine(val3));
			pw.println(w.serializeLine(val1) + "," + w.serializeLine(val2) + "," + w.serializeLine(val3));
			pw.println(w.serializeChunk(new String[] {val1, val2, val3}));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		String rv1 = null;
		String rv2 = null;
		String rv3 = null;
		String rv4 = null;
		String rv5 = null;
		// Marketplace siempre debe ser genérico/individual
		/*
		 * 	Si solo tiene una variante se crea como individual, en marketplace le pueden sumar más variantes múltiples proveedores. Todo producto de MarketPlace es genérico
		 *
		 **/
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\hola")))){
			System.out.println(rv1 = String.join("\t", w.parseLine( br.readLine() ) ));
			System.out.println(rv2 = String.join("\t", w.parseLine( br.readLine() ) ));
			System.out.println(rv3 = String.join("\t", w.parseLine( br.readLine() ) ));
			System.out.println(rv4 = String.join("\t", w.parseLine( br.readLine() ) ));
			System.out.println(rv5 = String.join("\t", w.parseLine( br.readLine() ) ));

			System.out.println(rv1.equals(val1));
			System.out.println(rv2.equals(val2));
			System.out.println(rv3.equals(val3));
			System.out.println(rv4.equals(String.join("\t", new String[] {val1, val2, val3})));
			System.out.println(rv5.equals(String.join("\t", new String[] {val1, val2, val3})));
		}catch(java.io.IOException e) {

		}
	}
}
