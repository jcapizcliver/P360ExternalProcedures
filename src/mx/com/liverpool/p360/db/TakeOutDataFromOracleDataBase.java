
package mx.com.liverpool.p360.db;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class TakeOutDataFromOracleDataBase {

	private static RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {
		try {
			Class.forName("oracle.jdbc.OracleDriver");
		}catch(ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}
		String delim = "\"";
		String sep = "\t";
		String esc = "\\";
		String[] tables = new String[] {"CharacteristicRevision", "CharParentLookupValue", "CharacteristicLang", "CharacteristicIdentifier", "CharacteristicAuditLog", "Characteristic"};
		try(java.sql.Connection con = java.sql.DriverManager.getConnection("jdbc:oracle:thin:@//gcpcatddb01/INFPDEV", "PIM_MAIN", "pimadmin")){
			StringBuilder sb = new StringBuilder();
			int count = 0;
			for(String tableName : tables) {
				System.out.println("Processing: " + tableName);
				try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\data\\tbc_" + tableName + ".dat"))); java.sql.PreparedStatement pstmt = con.prepareStatement("select * from \"" + tableName + "\"")){
					for(int i=1; i<=pstmt.getMetaData().getColumnCount(); i++) {
						sb.append(i == 1 ? "" : sep).append( workshop.serializeLine(pstmt.getMetaData().getColumnName(i), delim, sep, esc) );
					}
					pw.println(sb.toString());
					sb.setLength(0);
					pstmt.setFetchSize(2000);
					try(java.sql.ResultSet rs = pstmt.executeQuery()){
						while(rs.next()) {
							count++;
							for(int i=1; i<=rs.getMetaData().getColumnCount(); i++) {
								sb.append(i == 1 ? "" : sep).append(workshop.serializeLine(String.valueOf( rs.getObject(i) ).replaceAll("\n", "\\\\n"), delim, sep, esc));
							}
							pw.println(sb.toString());
							sb.setLength(0);
							if(count % 1000 == 0) {
								System.out.print(".");
								if(count % 10000 == 0) {
									System.out.println(count);
								}
							}
						}
					}
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}
				System.out.println(count);
				count = 0;
			}
		}catch(java.sql.SQLException e) {
			e.printStackTrace();
		}
	}
}
