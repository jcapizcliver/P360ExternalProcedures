package mx.com.liverpool.p360.services.core.temp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class TraducemeLasCaracteristicas {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) {

		java.util.Map<String, String> losEsos = new java.util.TreeMap<>();
		java.util.Set<String> notFound = new java.util.TreeSet<>();

		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier");

		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		String identifier = null;

		int cnt = 0;
		for(String atributo : atributosABuscar) {
			cnt++;
			qp.put("query", "CharacteristicIdentifier.AlternativeIdentifier(ECC) wildcard \"" + atributo + "\"");
			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			rows = response.getJSONArray("rows");
			if(rows.length() > 0) {
				values = rows.getJSONObject(0).getJSONArray("values");
				identifier = values.getString(0);
				losEsos.put(atributo, identifier);
			}else {
				notFound.add(atributo);
			}
			System.out.println( cnt  + "/" + atributosABuscar.length );
		}

		System.out.println("Found:");
		java.util.LinkedList<java.util.Map.Entry<String, String>> lst = new java.util.LinkedList<>( losEsos.entrySet() );

		lst.forEach(el->{
			System.out.println( "<columns><identifier>Product2GCharacteristicValueLang.Value('" + el.getValue() + "',root,\"0000.0000.RK\", '" + el.getValue() + "',-1)</identifier></columns>" );
		});

		lst.forEach(el->{
			System.out.println("<Value> { $product/Value[@AttributeID = '" + el.getKey() + "']/text() } </Value>");
		});

		System.out.println("Not found:");
		notFound.forEach(System.out::println);

	}

	private static final String[] atributosABuscar = ("\r\n"
			+ "PE000\r\n"
			+ "MATNR\r\n"
			+ "MAKTX\r\n"
			+ "ZSEC\r\n"
			+ "MATKL\r\n"
			+ "ERSDA\r\n"
			+ "IDNLF\r\n"
			+ "FERTH\r\n"
			+ "MEINS\r\n"
			+ "ZMADE\r\n"
			+ "BRAND_DESCR\r\n"
			+ "WHERL\r\n"
			+ "LAENG\r\n"
			+ "HOEHE\r\n"
			+ "NTGEW\r\n"
			+ "BREIT\r\n"
			+ "KBETR\r\n"
			+ "SAITY\r\n"
			+ "EAN11_UPC\r\n"
			+ "ZDIR\r\n"
			+ "ZLICT\r\n"
			+ "VTEXT\r\n"
			+ "MTART\r\n"
			+ "SATNR\r\n"
			+ "EAN11_EAN\r\n"
			+ "NUMTP\r\n"
			+ "ZFANET\r\n"
			+ "ATTYP\r\n"
			+ "LIFNR\r\n"
			+ "NAME1\r\n"
			+ "REFGID\r\n"
			+ "GROUPID\r\n"
			+ "NUMCO\r\n"
			+ "DESCO\r\n"
			+ "SPART\r\n"
			+ "BEHVO\r\n"
			+ "SAISJ\r\n"
			+ "ZARGUMENTO\r\n"
			+ "ZCOLECCION\r\n"
			+ "FASHGRD\r\n"
			+ "PROFL\r\n"
			+ "EXTWG\r\n"
			+ "WHERR\r\n"
			+ "ZZINEX\r\n"
			+ "SAISO\r\n"
			+ "BISMT\r\n"
			+ "VOLUM\r\n"
			+ "TAXM1\r\n"
			+ "TAXM2\r\n"
			+ "TAXM3\r\n"
			+ "BRGEW\r\n"
			+ "VOLEH\r\n"
			+ "GEWEI\r\n"
			+ "MEABM\r\n"
			+ "RDPRF\r\n"
			+ "WAERS\r\n"
			+ "ZBRECJ\r\n"
			+ "ZLAECJ\r\n"
			+ "ZHOECJ\r\n"
			+ "ZMEACJ\r\n"
			+ "ZBRGCJ\r\n"
			+ "ZNTGCJ\r\n"
			+ "ZGEWCJ\r\n"
			+ "ZVOLCJ\r\n"
			+ "ZVOLEH\r\n"
			+ "CUBISCAN\r\n"
			+ "LOGGR\r\n"
			+ "HNDLCODE\r\n"
			+ "WHSTC\r\n"
			+ "MVGR5\r\n"
			+ "TIPOOPERACION\r\n"
			+ "LABOR\r\n"
			+ "NORMT\r\n"
			+ "SISTEMAORIGEN\r\n"
			+ "KLASE_CTE\r\n"
			+ "SERVV\r\n"
			+ "WESCH\r\n"
			+ "ZIDTENDEN\r\n"
			+ "ZIDKTEMP\r\n"
			+ "ZIDPERCTE\r\n"
			+ "ZZOMSUD\r\n"
			+ "ENV_ATG\r\n"
			+ "ZZFEEM\r\n"
			+ "PRODUCT_ID\r\n"
			+ "ZBREPQ\r\n"
			+ "ZLAEPQ\r\n"
			+ "ZHOEPQ\r\n"
			+ "ZMEAPQ\r\n"
			+ "ZBRGPQ\r\n"
			+ "ZNTGPQ\r\n"
			+ "ZGEWPQ\r\n"
			+ "ZVOLPQ\r\n"
			+ "ZVOLEHPQ\r\n"
			+ "MVGR2").split("\\r\\n");

}
