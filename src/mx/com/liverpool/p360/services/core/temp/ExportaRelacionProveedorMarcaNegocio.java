package mx.com.liverpool.p360.services.core.temp;

import com.jcapiz.memelos.misc.RestClient;

public class ExportaRelacionProveedorMarcaNegocio {

	private static final String baseUrlDEV = "https://webctep360dev.liverpool.com.mx/rest/V2.0";
	private static final String encoded = "cmVzdDpoZWlsZXI=";
	private static RestClient rc = new RestClient("Content-Type: application/json", "Accept: application/json", "Authorization: Basic " + encoded);

	public static void main(String[] args) {
//		try {
//			System.out.println( rc.getRequest("GET", baseUrlDEV + "/object/Product2G/'1698767480767002'@'MASTER'", null) );
//		}catch(Exception e) {
//			e.printStackTrace();
//		}
//		System.exit(0);
		long init = System.currentTimeMillis();
		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int totalSize = 0;
		int currentIndex = 0;

		String supplier = null;
		org.json.JSONArray brand = null;
		org.json.JSONArray business = null;

		String psupplier = null;
		org.json.JSONArray pbrand = null;
		org.json.JSONArray pbusiness = null;

		java.util.LinkedList<Object[]> tuples = new java.util.LinkedList<>();
		java.util.LinkedList<Object[]> uniqueElements = new java.util.LinkedList<>();
		try {
			do {
				rawResponse = rc.getRequest("GET", baseUrlDEV + "/list/GroupOfArticleTemplateValue/bySearch?orderBy=0-ASC;1-ASC;2-ASC"
						+ "&dictionaryProxy=" + java.net.URLEncoder.encode("'Plantilla - Grupo de Artículos'", "UTF-8")
						+ "&query=" + java.net.URLEncoder.encode("GroupOfArticleTemplateValue.Dictionary->GeneralPurposeDictionary.Identifier equals \"Plantilla - Grupo de Artículos\"", "UTF-8")
						+ "&fields=GroupOfArticleTemplateValue.Supplier,GroupOfArticleTemplateValue.Brand,GroupOfArticleTemplateValue.System"
						+ "&pageSize=500"
						+ "&startIndex=" + currentIndex, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					supplier = String.valueOf( values.get(0) );
					brand = values.getJSONArray(1);
					business = values.getJSONArray(2);
					if(supplier == null || brand == null || business == null || "".equals(supplier) || "[\"\"]".equals(brand.toString())|| "[\"\"]".equals(business.toString())){
						if(!tuples.isEmpty()) {
							while(tuples.size() > 1) {
								tuples.removeLast();
							}
							uniqueElements.addLast( tuples.removeLast() );
						}
						continue;
					}
					if( psupplier != null && pbrand != null && pbusiness != null && (!psupplier.toString().equals(supplier.toString()) || !pbrand.toString().equals(brand.toString()) || !pbusiness.toString().equals(business.toString()) ) ) {
						System.out.println(psupplier + ";" + pbrand + ";" + pbusiness + "\n" + supplier + ";" + brand + ";" + business + "\n");
						while(tuples.size() > 1) {
							tuples.removeLast();
						}
						if(tuples.size() > 0) {
							uniqueElements.addLast( tuples.removeLast() );
						}else {
							System.out.println("Empty tuples...");
						}
					}
					tuples.addLast(new Object[] {supplier, treatment( brand ) , treatment( business )});
					psupplier = supplier;
					pbrand = brand;
					pbusiness = business;
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
			if(!tuples.isEmpty()) {
				while(tuples.size() > 1) {
					tuples.removeLast();
				}
				uniqueElements.addLast( tuples.removeLast() );
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		String delimitador = "\"";
		String separador = ",";
		String escape = "\\";
		org.json.JSONArray marcas = null;
		org.json.JSONArray negocios = null;
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\proveedor_marca_negocio.csv")))){
			pw.println("Proveedor" + separador + "Marca" + separador + "Negocio");
			for(Object[] pieces : uniqueElements) {
				marcas = (org.json.JSONArray) pieces[1];
				negocios = (org.json.JSONArray) pieces[2];
				for(int i=0; i<marcas.length(); i++) {
					for(int j=0; j<negocios.length(); j++) {
						pw.println( serializeChunk(new String[] {(String)pieces[0], marcas.getString(i), negocios.getString(j)}, delimitador, separador, escape) );
					}
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Done. " + formatMillis(System.currentTimeMillis() - init));
	}

	private static String serializeChunk(Object[] pieces, String delimitador, String separador, String escape) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<pieces.length; i++) {
			sb.append(i == 0 ? "" : separador).append(escapeValue(String.valueOf(pieces[i]), delimitador, separador, escape));
		}
		return sb.toString();
	}

	private static org.json.JSONArray treatment(org.json.JSONArray vals) {
		String helper = null;
		org.json.JSONArray h = new org.json.JSONArray();
		String[] pieces = null;
		java.util.Set<String> childValues = new java.util.TreeSet<>();
		System.out.println();
		System.out.println("Received: " + vals);
		boolean unplug = false;
		for(int i=0; i<vals.length(); i++) {
			System.out.println(vals.getString(i));
			helper = vals.getString(i);
			if(helper.contains("/")) {
				pieces = helper.split("/");
				System.out.println("Split from /: " + java.util.Arrays.asList(pieces));
				for (String element : pieces) {
					if(element != null) {
						childValues.add(element);
					}
				}
			}else if(helper.contains(" & ")) {
				unplug = true;
				pieces = helper.split(" \\& ");
				System.out.println("Split from  & : " + java.util.Arrays.asList(pieces));
				for (String element : pieces) {
					childValues.add(element);
				}
			}else {
				h.put(helper);
			}
		}
		if(!childValues.isEmpty()) {
			System.out.println("Adding: " + childValues);
			for(String cv : childValues) {
				if(!"No data".equals(cv)) {
					h.put(cv);
				}
			}
			System.out.println("New values: " + h);
//			if(unplug)
//				System.exit(0);
			return h;
		}else {
			return vals;
		}
	}

	private static String joinJSONArray(org.json.JSONArray array) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<array.length(); i++) {
			sb.append(i == 0 ? "" : ";").append(array.getString(i));
		}
		return sb.toString();
	}

	private static String escapeValue(String value, String delimitador, String separador, String escape) {
		try{
			return value == null ? "" : value.contains(separador) || value.contains(delimitador) || value.contains("\\".equals(escape) ? "\\" : escape) ? delimitador + value.replaceAll("(?=[" + delimitador + ("\\".equals(escape) ? "\\\\" : escape) + "])", "\\".equals(escape) ? "\\\\" : escape) + delimitador: value;
		}catch(IllegalArgumentException e) {
			System.out.println("--->" + value + "<---" + "(?=[" + delimitador + ("\\".equals(escape) ? "\\\\" : escape) + "])");
			throw new RuntimeException(e);
		}
	}

	private static String formatMillis(long millis){
	  	int days = (int)(millis/(1000*60*60*24));
	 	millis -= days*1000*60*60*24;
	  	int hours = (int) (millis/(1000*60*60));
	  	millis -= hours*1000*60*60;
	  	int minutes = (int) (millis/(1000*60));
	  	millis -= minutes*1000*60;
	  	int seconds = (int) (millis/1000);
	  	millis -= seconds*1000;
	  	return
	  		    (days < 10 ? "0" : "") + days + ":"
	  		+ (hours < 10 ? "0" : "") + hours + ":"
	  		+ (minutes < 10 ? "0" : "") + minutes + ":"
	  		+ (seconds < 10 ? "0" : "") + seconds
	  		+ "." + millis;
	  }
}
