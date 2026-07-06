package mx.com.liverpool.p360.services.core.temp;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class SampleProveedorNegocioGrupoArticuloMarcas extends RESTWorkshop {

	public static void main(String[] args) throws KeyManagementException, NoSuchAlgorithmException, URISyntaxException {
		RESTWorkshop workshop = new RESTWorkshop();
		java.util.ArrayList<String> headers = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\Users\\jcapizc\\Downloads\\TESTIMPORT (1).csv"), java.nio.charset.Charset.forName("UTF-8")))){
			String line = null;
			String[] pieces = null;
			headers = new java.util.ArrayList<>(java.util.Arrays.asList(workshop.parseLine(br.readLine())));
			int supplierNumber = headers.indexOf("supplierNumber");
			int companies = headers.indexOf("companies");
			int ga = headers.indexOf("GA");
			int idMarca = headers.indexOf("ID Marca");
			int idPlantilla = headers.indexOf("Id Plantilla");
			java.util.LinkedList<Object[]> tuples = new java.util.LinkedList<>();
			org.json.JSONArray values = null;
			org.json.JSONArray valuesBusiness = null;
			Object[] tuple = null;
			String[] subPieces = null;
			org.json.JSONArray valuesForUpload = null;
			org.json.JSONObject object = null;
			org.json.JSONArray rows = new org.json.JSONArray();
			org.json.JSONObject request = null;
			org.json.JSONArray columns = new org.json.JSONArray();
			columns.put(new org.json.JSONObject().put("identifier", "GroupOfArticleTemplateValue.Supplier"));
			columns.put(new org.json.JSONObject().put("identifier", "GroupOfArticleTemplateValue.Brand"));
			columns.put(new org.json.JSONObject().put("identifier", "GroupOfArticleTemplateValue.System"));
			columns.put(new org.json.JSONObject().put("identifier", "GroupOfArticleTemplateValue.Template"));
			columns.put(new org.json.JSONObject().put("identifier", "GroupOfArticleTemplateValue.GroupOfArticle"));
			while((line = br.readLine()) != null) {
				object = new org.json.JSONObject();
				valuesForUpload = new org.json.JSONArray();
				tuple = new Object[3];
				pieces = workshop.parseLine(line);
				valuesForUpload.put(pieces[supplierNumber]);
				values = new org.json.JSONArray();
				subPieces = pieces[idMarca].split(";");
				for (String element : subPieces) {
					values.put(element.length() == 3 ? "0" + element : element.length() == 2 ? "00" + element : element);
				}
				values = workshop.treatment(values);
				valuesForUpload.put(values);
				valuesBusiness = new org.json.JSONArray();
				subPieces = pieces[companies].split(";");
				for (String element : subPieces) {
					valuesBusiness.put(element.trim());
				}
				valuesBusiness = workshop.treatment(valuesBusiness);
				object.put("object", new org.json.JSONObject().put("id", "'" + pieces[ga] + "-" + pieces[idPlantilla] + "-" + pieces[supplierNumber] + "-" + workshop.joinJSONArray(values, ";") + "'@'Plantilla - Grupo de Artículos'"));
				valuesForUpload.put(values);
				valuesForUpload.put(pieces[idPlantilla]);
				valuesForUpload.put(pieces[ga]);
				tuple[2] = valuesBusiness;
				tuple[1] = values;
				tuple[0] = pieces[supplierNumber];
				tuples.addLast(tuple);
				object.put("values", valuesForUpload);
				rows.put(object);
			}
			request = new org.json.JSONObject().put("columns", columns).put("rows", rows);
//			System.out.println(request.toString());
//			String rawResponse = workshop.makeRequest("POST", "/list/GroupOfArticleTemplateValue", request.toString());
//			System.out.println(rawResponse);
			org.json.JSONArray marcas = null;
			org.json.JSONArray negocios = null;
			try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("D:\\tmp\\proveedor_marca_negocio_extrita.csv")))){
				pw.println("Proveedor" + workshop.getSeparator() + "Marca" + workshop.getSeparator() + "Negocio");
				for(Object[] objectss : tuples) {
					marcas = (org.json.JSONArray) objectss[1];
					negocios = (org.json.JSONArray) objectss[2];
					for(int i=0; i<marcas.length(); i++) {
						for(int j=0; j<negocios.length(); j++) {
							pw.println( workshop.serializeChunk(new String[] {(String)objectss[0], marcas.getString(i), negocios.getString(j)}) );
						}
					}
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}

}
