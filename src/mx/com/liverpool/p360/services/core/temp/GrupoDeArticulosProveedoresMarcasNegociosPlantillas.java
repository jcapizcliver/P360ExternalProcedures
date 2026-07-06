package mx.com.liverpool.p360.services.core.temp;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import org.json.JSONException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class GrupoDeArticulosProveedoresMarcasNegociosPlantillas {

	private static final RESTWorkshop workshop = new RESTWorkshop();

	public static void main(String[] args) throws ServiceUnavailableException {

		String rawResponse = null;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;

		int currentIndex = 0;
		int totalSize = 0;

		java.util.Map<String, org.json.JSONArray> refs = new java.util.TreeMap<>();
		java.util.Map<String, String> currentIds = new java.util.TreeMap<>();
		/*
		try{
			do {
				rawResponse = workshop.makeRequest("GET", "/list/GroupOfArticleTemplateValue/bySearch"
						+ "?dictionaryProxy='" + java.net.URLEncoder.encode("Plantilla - Grupo de Artículos", "UTF-8") + "'"
						+ "&query="
						+ java.net.URLEncoder.encode("not GroupOfArticleTemplateValue.Identifier is empty", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode(
								"GroupOfArticleTemplateValue.Identifier,"
										+ "GroupOfArticleTemplateValue.Supplier,"
										+ "GroupOfArticleTemplateValue.GroupOfArticle,"
										+ "GroupOfArticleTemplateValue.GroupOfArticleName,"
										+ "GroupOfArticleTemplateValue.Template->LookupValue.Code,"
										+ "GroupOfArticleTemplateValue.System,"
										+ "GroupOfArticleTemplateValue.Brand"
										, "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=200"
						+ "&startIndex=" + currentIndex
						, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if(!values.getString(0).matches("^[0-9]+$")) {
						System.out.println(values);
					}
					refs.put(values.getString(1) + "_" + values.getString(2) + "_" + values.getString(4), values);
					currentIds.put(values.getString(1) + "_" + values.getString(2) + "_" + values.getString(4), values.getString(0));
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}
		System.exit(0);

		*/

		rows = new org.json.JSONArray();
		int count = 0;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("D:\\tmp\\libro_de_producto_headless.csv"), java.nio.charset.Charset.forName("UTF-8")))){
			String line = null;
			String delimiter = "\"";
			String separator = "\t";
			String escape = "\\";
			String[] pieces = null;
			int iGrupoDeArticulo = 2;
			int iPlantilla = 8;
			int iProveedor = 10;
			int iMarca = 13;
			int iNegocios = 14;
			String grupoDeArticulo = null;
			String plantilla = null;
			String proveedor = null;
			String marca = null;
			String negocios = null;
			String currentKey = null;
			org.json.JSONArray columns = new org.json.JSONArray();
			columns.put(new org.json.JSONObject().put("identifier", "GroupOfArticleTemplateValue.Identifier"));
			columns.put(new org.json.JSONObject().put("identifier", "GroupOfArticleTemplateValue.Supplier"));
			columns.put(new org.json.JSONObject().put("identifier", "GroupOfArticleTemplateValue.GroupOfArticle"));
			columns.put(new org.json.JSONObject().put("identifier", "GroupOfArticleTemplateValue.Template"));
			columns.put(new org.json.JSONObject().put("identifier", "GroupOfArticleTemplateValue.System"));
			columns.put(new org.json.JSONObject().put("identifier", "GroupOfArticleTemplateValue.Brand"));
			while((line = br.readLine()) != null) {
				pieces = workshop.parseLine(line,delimiter, separator, escape);
				grupoDeArticulo = pieces[iGrupoDeArticulo];
				plantilla =  pieces[iPlantilla];
				proveedor = tratamientoProveeodr( pieces[iProveedor] );
				marca = tratamientoMarca( pieces[iMarca] );
				negocios = tratamientoNegociosParaFinesDeCatalogacion( pieces[iNegocios] );
				currentKey = proveedor + "_" + grupoDeArticulo + "_" + plantilla;
				if( refs.containsKey( currentKey ) ) {
					System.out.println(currentIds.get(currentKey));
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + currentIds.get(currentKey) + "'@'Plantilla - Grupo de Artículos'")).put("values", new org.json.JSONArray().put(currentKey).put(proveedor).put(grupoDeArticulo).put(plantilla).put(toJSONArray( negocios )).put(toJSONArray( marca ))));
					if(rows.length() == 200) {
						try {
							rawResponse = workshop.makeRequest("POST", "/list/GroupOfArticleTemplateValue", new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
						} catch (KeyManagementException | NoSuchAlgorithmException | JSONException | URISyntaxException e) {
							e.printStackTrace();
						}
						System.out.println(rawResponse);
						while(rows.length() > 0) {
							rows.remove(0);
						}
					}
					count++;
				}else {
					rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + currentKey + "'@'Plantilla - Grupo de Artículos'")).put("values", new org.json.JSONArray().put(currentKey).put(proveedor).put(grupoDeArticulo).put(plantilla).put(toJSONArray( negocios )).put(toJSONArray( marca ))));
					if(rows.length() == 200) {
						try {
							rawResponse = workshop.makeRequest("POST", "/list/GroupOfArticleTemplateValue", new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
						} catch (KeyManagementException | NoSuchAlgorithmException | JSONException | URISyntaxException e) {
							e.printStackTrace();
						}
						while(rows.length() > 0) {
							rows.remove(0);
						}
					}
					count++;
				}
			}
			if(rows.length() > 0) {
				try {
					rawResponse = workshop.makeRequest("POST", "/list/GroupOfArticleTemplateValue", new org.json.JSONObject().put("columns", columns).put("rows", rows).toString());
				} catch (KeyManagementException | NoSuchAlgorithmException | JSONException | URISyntaxException e) {
					e.printStackTrace();
				}
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Total of: " + refs.size());
		System.out.println("Hits: " + count);
		System.exit(0);

		try{
			do {
				rawResponse = workshop.makeRequest("GET", "/list/GeneralPurposeDictionary/bySearch?query="
						+ java.net.URLEncoder.encode("not GeneralPurposeDictionary.Identifier is empty", "UTF-8")
						+ "&fields=" + java.net.URLEncoder.encode("GeneralPurposeDictionary.Identifier", "UTF-8")
						+ "&metaData=true"
						+ "&pageSize=2"
						+ "&startIndex=" + currentIndex
						, null);
				response = new org.json.JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					System.out.println(rows.getJSONObject(i).getJSONArray("values").getString(0));
				}
			}while(currentIndex < totalSize);
			currentIndex = 0;
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static String tratamientoMarca(String marca) {
		if("NO DATA".equals(marca.toUpperCase())) {
			return "";
		}
		String[] marcas = marca.split(";");
		String[] pieces = null;
		java.util.Set<String> set = new java.util.TreeSet<>();
		for(int i=0; i<marcas.length; i++) {
			marcas[i] = marcas[i].trim();
			if(marcas[i].contains(" & ")) {
				pieces = marcas[i].split(" \\& ");
				for (String element : pieces) {
					set.add(element);
				}
			}else {
				set.add(marcas[i]);
			}
		}
		return String.join(";", set);
	}

	private static String tratamientoProveeodr(String proveedor) {
		String[] proveedores = proveedor.split(" & ");
		for(int i=0; i<proveedores.length; i++) {
			proveedores[i] = proveedores[i].replaceAll("-[A-Za-z_-]+", "").trim();
		}
		return String.join(";", proveedores);
	}

	private static String tratamientoNegociosParaFinesDeCatalogacion(String negocio) {
		String[] negocios = negocio.split(";");
		String[] subpiezas = null;
		java.util.Set<String> losnegocios = new java.util.TreeSet<>();
		for(int i=0; i<negocios.length; i++) {
			negocios[i] = negocios[i].trim();
			if(negocios[i].contains("/")) {
				subpiezas = negocios[i].split("/");
				for (String element : subpiezas) {
					losnegocios.add(element);
				}
			} else {
				losnegocios.add(negocios[i]);
			}
		}
		return String.join(";", losnegocios);
	}

	private static org.json.JSONArray toJSONArray(String val){
		String[] pieces = val.split(";");
		org.json.JSONArray arr = new org.json.JSONArray();
		for (String element : pieces) {
			arr.put(element);
		}
		return arr;
	}
}
