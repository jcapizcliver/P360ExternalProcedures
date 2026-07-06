package mx.com.liverpool.p360.services.core.temp.move.dictionaries;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.temp.move.utils.GeneralOperations;

public class MoveMetadata {

	public static void main(String[] args) {
		MoveMetadata m = new MoveMetadata();
//		String sd = 
//				"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla"
//				"GlobalTemplateAttributeConfiguration"
//				;
		String[] sds = (
				"AttributeGroupRef\r\n"
				+ "BEHVO_LookupTable\r\n"
				+ "CaracteristicasATG\r\n"
				+ "CaracteristicasHeredables\r\n"
				+ "ConversionFechaATG\r\n"
				+ "ErroresSKU\r\n"
				+ "ExcepcionesAnuladas\r\n"
				+ "ExcepcionesPorGrupoDeArticulo\r\n"
				+ "ExcepcionesPorNegocioYSeccion\r\n"
				+ "ExcepcionesPorSeccionYMarca\r\n"
				+ "ExcepcionesPorSeccionYNegocio\r\n"
				+ "ExcepcionesPorSeccionYProveedor\r\n"
				+ "ExcepcionesPorSKU\r\n"
				+ "ExcepcionesPorSkuYArticulo\r\n"
				+ "Extension de Metadatos Plantillas\r\n"
				+ "ExtensionDeMetadatos_CatalogoColoresLiverpool\r\n"
				+ "ExtensionDeMetadatos_CatalogoColoresSuburbia\r\n"
				+ "ExtensionDeMetadatos_CompradoresPermitidos\r\n"
				+ "ExtensionDeMetadatos_RelacionColoresLiverpoolSuburbia\r\n"
				+ "ExtensionDeMetadatos_SistemaPropietarioDeLaCaracteristica\r\n"
				+ "ExternalStatus\r\n"
				+ "FTNT_Prioridad_Tallas_LVP\r\n"
				+ "FTNT_Prioridad_Tallas_SBB\r\n"
				+ "FTNT_StylistWorld\r\n"
				+ "FTNT_TipoDeToma\r\n"
				+ "GPA_Plantilla\r\n"
				+ "GpoArtVsEnvase\r\n"
				+ "GpoArtVsEnvase_S4H\r\n"
				+ "Grupo_de_Articulos_S4H\r\n"
				+ "ItemGroupSAPSizeAttribute\r\n"
				+ "JobReportS4H\r\n"
				+ "JobSKUCreation\r\n"
				+ "LimpiezaDeExcepciones\r\n"
				+ "MargenVsIndicadorImp\r\n"
				+ "MetadatosPlantillaCaracteristicaGlobales\r\n"
				+ "NextStatus\r\n"
				+ "PythonImportPrefixes\r\n"
				+ "ReferenciaDeDatosDeNegocio_CaracteristicasConfigurables\r\n"
				+ "ReferenciaDeDatosDeNegocio_ProductoConfigurable\r\n"
				+ "RelAttribSTDATG\r\n"
				+ "RelAttribTallaATG\r\n"
				+ "RelevantesParaMarketplace\r\n"
				+ "SB_0106\r\n"
				+ "SB_0107\r\n"
				+ "SB_TBebes\r\n"
				+ "SB_TCaballeros\r\n"
				+ "SB_TCalceteria\r\n"
				+ "SB_TDamas\r\n"
				+ "SB_THogar\r\n"
				+ "SB_TInfantiles\r\n"
				+ "SB_TJoyeriayAccesorios\r\n"
				+ "SB_TJuniors\r\n"
				+ "SB_TLenceria\r\n"
				+ "SB_TRopaInterior\r\n"
				+ "SB_TZapatos\r\n"
				+ "SBTHardline\r\n"
				+ "SBTTecnoEntren\r\n"
				+ "SeccionesEntradaUnicaCatalogacion\r\n"
				+ "SferaSizeAtt\r\n"
				+ "ShoeSizeLivAtt\r\n"
				+ "SizeERPvsUniqueSize\r\n"
				+ "SizeEstandarize\r\n"
				+ "TallaCaballeros\r\n"
				+ "TallaCosmeticos\r\n"
				+ "TallaDamas\r\n"
				+ "TallaDeportes\r\n"
				+ "TallaInfantiles\r\n"
				+ "TallaNormalizada\r\n"
				+ "TallaOptica\r\n"
				+ "TallaSfera\r\n"
				+ "TallasInfantilesVsMarca\r\n"
				+ "TallaUnicavsTallaERP\r\n"
				+ "TallaUnicavsTallaS4H\r\n"
				+ "TallaZapatos\r\n"
				+ "TamañoDirección1\r\n"
				+ "TamañoDirección3\r\n"
				+ "TamañoDirección6\r\n"
				+ "ValidDirection\r\n"
				+ "VariantOrder"
				).split("\r\n");
		for(String sd : sds) {
			System.out.println("*** " + sd + " ***");
			m.doMoveDictionary(sd);
			System.out.println("///");
		}
	}
	
	private void doMoveDictionary(String sd) {
		RESTWorkshop rwQA = new RESTWorkshop();
		rwQA.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		GeneralOperations go = new GeneralOperations();
		RESTWorkshop rw = new RESTWorkshop();
		RESTWorkshop rwd = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject request = new org.json.JSONObject();
		org.json.JSONArray columns = new org.json.JSONArray();
		org.json.JSONArray rows = new org.json.JSONArray();
		java.util.Map<String, org.json.JSONArray> data = null;
		java.util.Map<String, org.json.JSONArray> data2 = null;
		java.util.LinkedList<org.json.JSONArray> brandNew = new java.util.LinkedList<>();
		java.util.LinkedList<String> both = new java.util.LinkedList<>();
		org.json.JSONArray values = null;
		StringBuilder sb = new StringBuilder();
		String baseUrl2 = "https://webctep360pro.liverpool.com.mx/rest/V2.0";
		int counter = 0;
		rw.setBaseUrl(baseUrl2);
		rw.addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
		
		
		rwd.setBaseUrl(baseUrl2);
		rwd.addHeader("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString("jcapizc:algolindo".getBytes()));
		rwd.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
		
		System.out.println("Collecting values source 1");
		data = go.gatherDictionaryData(rwQA, sd);
		System.out.println("Collecting values source 2");
		data2 = go.gatherDictionaryData(rw, sd);
		System.out.println("Now performing...");
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Value"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.AlternativeValue"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"));
		for(java.util.Map.Entry<String, org.json.JSONArray> entry : data.entrySet()) {
			values = data2.get(entry.getKey());
			if(values != null) {
				go.chooseToApply(entry.getValue(), values, columns, rw, (cl, vl, rw0)->{
					java.util.Map<String, String> qp0 = new java.util.TreeMap<>();
					rw0.makeRequest("POST", "/list/StandardizationValue", qp0, new org.json.JSONObject().put("columns", cl).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + vl.getString(0) + "'@'" + sd + "'")).put("values", vl))).toString());
					System.out.println("From updating standardization value entry: " + vl.getString(0) + ": " + rw0.getRawResponse());
				});
				both.addLast(values.getString(0));
			}else {
				brandNew.addLast(entry.getValue());
			}
		}
		System.out.println("Now working on new data (" + brandNew.size() + ")");
		request.put("columns", columns);
		request.put("rows", rows);
		for(org.json.JSONArray nv : brandNew) {
			rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + nv.getString(0).replaceAll("'", "\\\\'") + "'@'" + sd + "'")).put("values", nv));
			if(rows.length() == 1000) {
				rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
				System.out.println(rw.getRawResponse());
				while(rows.length() > 0) {
					rows.remove(0);
				}
			}
		}
		if(rows.length() > 0) {
			rw.makeRequest("POST", "/list/StandardizationValue", qp, request.toString());
			System.out.println(rw.getRawResponse());
			while(rows.length() > 0) {
				rows.remove(0);
			}
		}
		System.out.println("Now working on extra data");
		qp.put("dictionaryProxy", "'" + sd + "'");
		for(java.util.Map.Entry<String, org.json.JSONArray> entry : data2.entrySet()) {
			if(!both.contains(entry.getKey())) {
				sb.append(sb.length() > 0 ? "," : "");
				sb.append("\"");
				sb.append(entry.getKey());
				sb.append("\"");
				counter++;
				if(counter % 10 == 0) {
					qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + sd + "\" and StandardizationValue.Value in (" + sb.toString() + ")");
					rwd.makeRequest("DELETE", "/list/StandardizationValue/bySearch", qp, null);
					System.out.println(rwd.getRawResponse());
					sb.setLength(0);
				}
			}
		}
		if(counter % 10 != 0) {
			qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + sd + "\" and StandardizationValue.Value in (" + sb.toString() + ")");
			rwd.makeRequest("DELETE", "/list/StandardizationValue/bySearch", qp, null);
			System.out.println(rwd.getRawResponse());
			sb.setLength(0);
		}
	}
	
}
