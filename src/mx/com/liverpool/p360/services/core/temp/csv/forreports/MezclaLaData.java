package mx.com.liverpool.p360.services.core.temp.csv.forreports;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class MezclaLaData {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		String i1 = "C:\\opt\\LVP\\tmp\\workspace\\Memelos\\LaMasa_mdpad_.csv";
		String i2 = "C:\\opt\\LVP\\tmp\\workspace\\Memelos\\LaMasa_mdcd_.csv";
		String o = "C:\\opt\\LVP\\desorden\\PROD\\LaMasaMD.csv";
		try(
			java.io.BufferedReader br1 = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(i1).toFile())));
			java.io.BufferedReader br2 = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(i2).toFile())));
			java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get(o).toFile())));
		){
			boolean abort = false;
			String line1 = br1.readLine();
			String line2 = br2.readLine();
			String[] pieces1 = line1 == null ? null : rw.getRw().parseLine(line1);
			String[] pieces2 = line2 == null ? null : rw.getRw().parseLine(line2);
			String[] pieces2B = null;
			java.util.List<String[]> losUnos = new java.util.ArrayList<>();
			java.util.List<String[]> losIguales = new java.util.ArrayList<>();
			while( pieces1 != null && pieces2 != null ) {
				if( pieces1[0].compareTo(pieces2[0]) == 0 ) {
					pieces2B = pieces2;
					while( pieces1[0].compareTo(pieces2B[0]) == 0 ) {
						losIguales.add(pieces2);
						String[] c = new String[pieces1.length + pieces2.length];
						System.arraycopy(pieces1, 0, c, 0, pieces1.length);
						System.arraycopy(pieces2, 0, c, pieces1.length, pieces2.length);
						pw.println( rw.getRw().serializeChunk( sc(c) ) );
						line2 = br2.readLine();
						if(line2 == null) {
							abort = true;
							break;
						}else {
							pieces2 = line2 == null ? null : rw.getRw().parseLine(line2);
						}
					}
					if(abort) {
						break;
					}
					for(String[] pc2 : losIguales) {
						if(losUnos.isEmpty()) {
							while( pieces1[0].compareTo(pc2[0]) == 0 ) {
								losUnos.add(pieces1);
								String[] c = new String[pieces1.length + pc2.length];
								System.arraycopy(pieces1, 0, c, 0, pieces1.length);
								System.arraycopy(pc2, 0, c, pieces1.length, pc2.length);
								pw.println( rw.getRw().serializeChunk(pieces1) );
								line1 = br1.readLine();
								if(line1 == null) {
									abort = true;
									break;
								}else {
									pieces1 = line1 == null ? null : rw.getRw().parseLine(line1);
								}
							}
						}else {
							losUnos.remove(0);
							for( String[] pc1 : losUnos ) {
								String[] c = new String[pc1.length + pc2.length];
								System.arraycopy(pc1, 0, c, 0, pc1.length);
								System.arraycopy(pc2, 0, c, pc1.length, pc2.length);
								pw.println( rw.getRw().serializeChunk( sc(c) ) );
							}
						}
					}
				}else if(pieces1[0].compareTo(pieces2[0]) > 0) {
					while( pieces1[0].compareTo(pieces2[0]) > 0 ) {
						line2 = br2.readLine();
						if(line2 == null) {
							abort = true;
							break;
						}else {
							pieces2 = line2 == null ? null : rw.getRw().parseLine(line2);
						}
					}
				}else{
					while( pieces1[0].compareTo(pieces2[0]) < 0 ) {
						line1 = br1.readLine();
						if(line1 == null) {
							abort = true;
							break;
						}else {
							pieces1 = line1 == null ? null : rw.getRw().parseLine(line1);
						}
					}
				}
				if(abort) {
					break;
				}
			}
			while( pieces1 != null ) {
				pw.println( rw.getRw().serializeChunk( sc(pieces1) ) );
				line1 = br1.readLine();
				pieces1 = line1 == null ? null : rw.getRw().parseLine(line1);
			}
			while( pieces2 != null ) {
				pw.println( rw.getRw().serializeChunk( sc(pieces2) ) );
				line2 = br2.readLine();
				pieces2 = line2 == null ? null : rw.getRw().parseLine(line2);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private static final String[] sc(String[] data) {
		for(int i=0; i<data.length; i++) {
			data[i] = data[i].replaceAll("\\n", "\n");
		}
		return data;
	}
	
}
