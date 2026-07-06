package mx.com.liverpool.p360.services.core.temp;

import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RestClient;

public class AddRejectionChildCharacteristics {

	private static final String encoded = "cmVzdDpoZWlsZXI=";
	private static final String baseUrl = "https://webctep360dev.liverpool.com.mx/rest/V2.0";
	private static final RestClient rc = new RestClient("Accept: application/json", "Content-Type: application/json", "Authorization: Basic " + encoded);

	private static final RESTWorkshop workshop = new RESTWorkshop();
	private static final java.util.Map<String, String> qp = new java.util.TreeMap<>();

	static {
	}

	public static void main(String[] args) throws ServiceUnavailableException {
		addRejectionsToLogisticsData();
		System.exit(0);
		changeUnstandardizedNamesForRejectionCharacteristics();
//		collectRootRejectCharacteristics2();
//		collectRootRejectCharacteristicsForEnglish();
//		collectRootRejectCharacteristics();
//		String rawResponse = null;
//		JSONObject response = null;
//		String url = null;
//		try {
//			url = baseUrl + "/list/Characteristic";
//			rawResponse = rc.getRequest( "POST", url,
//					new org.json.JSONObject()
//						.put("columns",
//								new org.json.JSONArray()
//									.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))
//									.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"))
//									.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"))
//									.put(new org.json.JSONObject().put("identifier", "Characteristic.RootCharacteristic"))
//									.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))
//							)
//						.put("rows",
//								new org.json.JSONArray()
//									.put(
//											new org.json.JSONObject()
//												.put("object",
//														new org.json.JSONObject()
//															.put("id", "'Mermal'")
//													)
//												.put("values",
//																new org.json.JSONArray()
//																	.put("TEXT")
//																	.put("")
//																	.put("Rechazo_TemperaturaMinimaDeFuncionamientoVaD")
//																	.put("Rechazo_TemperaturaMinimaDeFuncionamientoVaD")
//																	.put(true)
//													)
//										)
//							)
//					.toString());
//			response = new JSONObject(rawResponse);
//			System.out.println(rawResponse);
//		}catch(java.io.IOException | org.json.JSONException e) {
//			System.out.println(rawResponse);
//			e.printStackTrace();
//		} catch (KeyManagementException e) {
//			e.printStackTrace();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
//		}
	}

	private static void changeUnstandardizedNamesForRejectionCharacteristics() throws ServiceUnavailableException {
		String rawResponse = null;
		JSONObject response = null;
		org.json.JSONArray rows = null;
		String url = null;
		int totalSize = 0;
		int currentIndex = 0;
		String currentId = null;
		org.json.JSONObject requestMessage = null;
		int elements = 0;
		try {
			String prefix = "Rechazo_";
//			String suffix = "_Rejection";
//			do {
//				rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?metaData=true&fields=Characteristic.IsActive&query=" + java.net.URLEncoder.encode("Characteristic.IsActive equals true and Characteristic.Identifier wildcard \"" + prefix + "%\"", "UTF-8") + "&pageSize=100", null);
//				System.out.println(rawResponse);
//				response = new org.json.JSONObject(rawResponse);
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					rows.getJSONObject(i).getJSONArray("values").put(0, false);
//					currentIndex++;
//				}
//				System.out.println(rc.getRequest("POST", baseUrl + "/list/Characteristic", response.toString()));
//				totalSize = response.getInt("totalSize");
//			}while(currentIndex < totalSize);
//			System.exit(0);
			do { // Characteristic.Identifier wildcard \"%_Rejection\"
				url = baseUrl + "/list/Characteristic/bySearch?query=" + java.net.URLEncoder.encode("Characteristic.Identifier wildcard \"" + prefix + "%\"", "UTF-8") + "&fields=Characteristic.Identifier&pageSize=300&startIndex=" + currentIndex;
				rawResponse = rc.getRequest( "GET", url, null);
				response = new JSONObject(rawResponse);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				System.out.println("Total Size: " + response.getInt("totalSize"));
				for(int i=0; i<rows.length(); i++) {
					elements++;
					currentId = rows.getJSONObject(i).getJSONArray("values").getString(0);
					rows.getJSONObject(i).getJSONArray("values").put(0, currentId.substring(prefix.length()) + "_Rechazo");
					currentIndex++;
					if(currentIndex % 300 == 0) {
						requestMessage = new org.json.JSONObject()
								.put("columns",
										new org.json.JSONArray()
										.put(new org.json.JSONObject().put("identifier", "Characteristic.Identifier"))
										)
								.put("rows", rows);
						rawResponse = rc.getRequest("POST", baseUrl + "/list/Characteristic", requestMessage.toString());
						System.out.println(rawResponse);
					}
				}
				requestMessage = new org.json.JSONObject()
						.put("columns",
								new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "Characteristic.Identifier"))
							)
						.put("rows", rows);
				rawResponse = rc.getRequest("POST", baseUrl + "/list/Characteristic", requestMessage.toString());
				System.out.println(rawResponse);
			}while(currentIndex < totalSize);
		}catch(java.io.IOException e) {
			System.out.println(rawResponse);
			e.printStackTrace();
		}
		System.out.println("Elements: " + elements);
	}

	private static void addRejectionsToLogisticsData() throws ServiceUnavailableException {
		String rawResponse = null;
		JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray vals = null;
		String url = null;
		int totalSize = 0;
		int currentIndex = 0;
		String currentId = null;
		org.json.JSONArray nrs = new org.json.JSONArray();
		org.json.JSONObject requestMessage = null;
		int elements = 0;
		try {
			String suffix = "_Rejection";
			RESTWorkshop delworkshop = new RESTWorkshop();
			delworkshop.getRc().getHeader().put("Content-Type", "application/x-www-form-urlencoded");
			qp.put("query", "Characteristic.ParentCharacteristic->Characteristic.Identifier equals \"UnidadDeMedidaLongitud\" or "
					+ "Characteristic.ParentCharacteristic->Characteristic.Identifier equals \"UnidadDeMedidaPeso\" or Characteristic.ParentCharacteristic->Characteristic.Identifier equals \"UnidadDeMedidaVolumen\"");
			qp.put("fields", "Characteristic.Identifier");
			response = workshop.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			rows = response.getJSONArray("rows");

			for(int a=0; a<rows.length(); a++) {
				vals = rows.getJSONObject(a).getJSONArray("values");
				nrs.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + vals.getString(0) + "'")).put("values", new org.json.JSONArray().put(false)));
			}
			System.out.println( workshop.makeRequest("POST", "/list/Characteristic", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", nrs).toString()) );
			nrs = new org.json.JSONArray();
			System.out.println("OP: " +  delworkshop.makeRequest("DELETE", "/list/Characteristic/bySearch", qp, null) );
			qp.clear();
			do { // Characteristic.Identifier wildcard \"%_Rejection\"
				url = baseUrl + "/list/Characteristic/bySearch?query=" + java.net.URLEncoder.encode("Characteristic.Category->LookupValue.Code equals \"DatosLogisticos\" and not ( Characteristic.Identifier wildcard \"%_Rechazo\" )", "UTF-8") + "&fields=Characteristic.Identifier&pageSize=300&startIndex=" + currentIndex;
				rawResponse = rc.getRequest( "GET", url, null);
				response = new JSONObject(rawResponse);
				rows = response.getJSONArray("rows");
				org.json.JSONArray rowsPayload = new org.json.JSONArray();
				org.json.JSONArray values = null;
				for(int i=0; i<rows.length(); i++) {
//					values = rows.getJSONObject(i).getJSONArray("values");
//					rowsPayload.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "_Rechazo'")).put("values", new org.json.JSONArray().put("NONE").put("DatosLogisticos").put(true)));
					elements++;
					currentId = rows.getJSONObject(i).getJSONArray("values").getString(0);
					nrs
						.put(
							new org.json.JSONObject()
								.put("object",
										new org.json.JSONObject()
											.put("id", "'rmum_" + currentId + "'") // rmum_ -> rechazo_mensaje_ultima_modificacion
									)
								.put("values",
												new org.json.JSONArray()
													.put("DATETIME")
													.put("")
													.put(currentId + "_Rechazo")
													.put(currentId + "_Rechazo")
													.put(true)
									)
							)
						.put(
							new org.json.JSONObject()
								.put("object",
										new org.json.JSONObject()
											.put("id", "'rma_" + currentId + "'") // rechazo_mensaje_accion
									)
								.put("values",
												new org.json.JSONArray()
													.put("LOOKUP")
													.put("RechazoMensajeAccion")
													.put(currentId + "_Rechazo")
													.put(currentId + "_Rechazo")
													.put(true)
									)
							)
						.put(
							new org.json.JSONObject()
								.put("object",
										new org.json.JSONObject()
											.put("id", "'rre_" + currentId + "'") // rechazo_rol_emisor
									)
								.put("values",
												new org.json.JSONArray()
													.put("LOOKUP")
													.put("TargetRole")
													.put(currentId + "_Rechazo")
													.put(currentId + "_Rechazo")
													.put(true)
									)
							)
						.put(
							new org.json.JSONObject()
								.put("object",
										new org.json.JSONObject()
											.put("id", "'rrd_" + currentId + "'") // rechazo_rol_destino
									)
								.put("values",
												new org.json.JSONArray()
													.put("LOOKUP")
													.put("TargetRole")
													.put(currentId + "_Rechazo")
													.put(currentId + "_Rechazo")
													.put(true)
									)
							)
						.put(
							new org.json.JSONObject()
								.put("object",
										new org.json.JSONObject()
											.put("id", "'rem_" + currentId + "'") // rechazo_estatus_mensaje
									)
								.put("values",
												new org.json.JSONArray()
													.put("LOOKUP")
													.put("CommentStatus")
													.put(currentId + "_Rechazo")
													.put(currentId + "_Rechazo")
													.put(true)
									)
							);
					currentIndex++;
					if(currentIndex % 50 == 0) {
						System.out.println("Going to try to insert an array of " + nrs.length() + " elements.");
						requestMessage = new org.json.JSONObject()
								.put("columns",
										new org.json.JSONArray()
											.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.RootCharacteristic"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))
									)
								.put("rows", nrs);
						rawResponse = rc.getRequest("POST", baseUrl + "/list/Characteristic", requestMessage.toString());
						System.out.println(rawResponse);
						nrs = new org.json.JSONArray();
					}
				}
//				System.out.println("--->" + rowsPayload);
//				System.out.println( workshop.makeRequest("POST", "/list/Characteristic", qp, new org.json.JSONObject().put("columns", new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.DataType")).put(new org.json.JSONObject().put("identifier", "Characteristic.Category")).put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))).put("rows", rowsPayload).toString()) );
				totalSize = response.getInt("totalSize");
				System.out.println("Going to try to insert an array of " + nrs.length() + " elements.");
				requestMessage = new org.json.JSONObject()
						.put("columns",
								new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.RootCharacteristic"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))
							)
						.put("rows", nrs);
				rawResponse = rc.getRequest("POST", baseUrl + "/list/Characteristic", requestMessage.toString());
				System.out.println(rawResponse);
				nrs = new org.json.JSONArray();
			}while(currentIndex < totalSize);
		}catch(java.io.IOException e) {
			System.out.println(rawResponse);
			e.printStackTrace();
		}
		System.out.println("Elements: " + elements);
	}

	private static void collectRootRejectCharacteristicsForEnglish() throws ServiceUnavailableException {
		String rawResponse = null;
		JSONObject response = null;
		org.json.JSONArray rows = null;
		String url = null;
		int totalSize = 0;
		int currentIndex = 0;
		String currentId = null;
		org.json.JSONArray nrs = new org.json.JSONArray();
		org.json.JSONObject requestMessage = null;
		int elements = 0;
		try {
			String suffix = "_Rejection";
			do { // Characteristic.Identifier wildcard \"%_Rejection\"
				url = baseUrl + "/list/Characteristic/bySearch?query=" + java.net.URLEncoder.encode("Characteristic.Identifier wildcard \"%" + suffix + "\"", "UTF-8") + "&fields=Characteristic.Identifier&pageSize=300&startIndex=" + currentIndex;
				rawResponse = rc.getRequest( "GET", url, null);
				response = new JSONObject(rawResponse);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					elements++;
					currentId = rows.getJSONObject(i).getJSONArray("values").getString(0);
					nrs
						.put(
							new org.json.JSONObject()
								.put("object",
										new org.json.JSONObject()
											.put("id", "'rmum_" + currentId + "'") // rmum_ -> rechazo_mensaje_ultima_modificacion
									)
								.put("values",
												new org.json.JSONArray()
													.put("DATETIME")
													.put("")
													.put(currentId + "_Rechazo")
													.put(currentId + "_Rechazo")
													.put(true)
									)
							)
						.put(
							new org.json.JSONObject()
								.put("object",
										new org.json.JSONObject()
											.put("id", "'rma_" + currentId + "'") // rechazo_mensaje_accion
									)
								.put("values",
												new org.json.JSONArray()
													.put("LOOKUP")
													.put("RechazoMensajeAccion")
													.put(currentId + "_Rechazo")
													.put(currentId + "_Rechazo")
													.put(true)
									)
							)
						.put(
							new org.json.JSONObject()
								.put("object",
										new org.json.JSONObject()
											.put("id", "'rre_" + currentId + "'") // rechazo_rol_emisor
									)
								.put("values",
												new org.json.JSONArray()
													.put("LOOKUP")
													.put("TargetRole")
													.put(currentId + "_Rechazo")
													.put(currentId + "_Rechazo")
													.put(true)
									)
							)
						.put(
							new org.json.JSONObject()
								.put("object",
										new org.json.JSONObject()
											.put("id", "'rrd_" + currentId + "'") // rechazo_rol_destino
									)
								.put("values",
												new org.json.JSONArray()
													.put("LOOKUP")
													.put("TargetRole")
													.put(currentId + "_Rechazo")
													.put(currentId + "_Rechazo")
													.put(true)
									)
							)
						.put(
							new org.json.JSONObject()
								.put("object",
										new org.json.JSONObject()
											.put("id", "'rem_" + currentId + "'") // rechazo_estatus_mensaje
									)
								.put("values",
												new org.json.JSONArray()
													.put("LOOKUP")
													.put("CommentStatus")
													.put(currentId + "_Rechazo")
													.put(currentId + "_Rechazo")
													.put(true)
									)
							);
					currentIndex++;
					if(currentIndex % 50 == 0) {
						System.out.println("Going to try to insert an array of " + nrs.length() + " elements.");
						requestMessage = new org.json.JSONObject()
								.put("columns",
										new org.json.JSONArray()
											.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.RootCharacteristic"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))
									)
								.put("rows", nrs);
						rawResponse = rc.getRequest("POST", baseUrl + "/list/Characteristic", requestMessage.toString());
						System.out.println(rawResponse);
						nrs = new org.json.JSONArray();
					}
				}
				totalSize = response.getInt("totalSize");
				System.out.println("Going to try to insert an array of " + nrs.length() + " elements.");
				requestMessage = new org.json.JSONObject()
						.put("columns",
								new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.RootCharacteristic"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))
							)
						.put("rows", nrs);
				rawResponse = rc.getRequest("POST", baseUrl + "/list/Characteristic", requestMessage.toString());
				System.out.println(rawResponse);
				nrs = new org.json.JSONArray();
			}while(currentIndex < totalSize);
		}catch(java.io.IOException e) {
			System.out.println(rawResponse);
			e.printStackTrace();
		}
		System.out.println("Elements: " + elements);
	}

	private static void collectRootRejectCharacteristics2() throws ServiceUnavailableException {
		String rawResponse = null;
		JSONObject response = null;
		org.json.JSONArray rows = null;
		String url = null;
		int totalSize = 0;
		int currentIndex = 0;
		String currentId = null;
		org.json.JSONArray nrs = new org.json.JSONArray();
		org.json.JSONObject requestMessage = null;
		int elements = 0;
		try {
//			do {
//				rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?metaData=true&fields=Characteristic.IsActive&query=" + java.net.URLEncoder.encode("Characteristic.IsActive equals true and (Characteristic.Identifier wildcard \"rnum_%\" or Characteristic.Identifier wildcard \"rma_%\" or Characteristic.Identifier wildcard \"rre_%\" or Characteristic.Identifier wildcard \"rrd_%\" or Characteristic.Identifier wildcard \"rem_%\")", "UTF-8") + "&pageSize=100", null);
//				System.out.println(rawResponse);
//				response = new org.json.JSONObject(rawResponse);
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					rows.getJSONObject(i).getJSONArray("values").put(0, false);
//				}
//				System.out.println(rc.getRequest("POST", baseUrl + "/list/Characteristic", response.toString()));
//				totalSize = response.getInt("totalSize");
//			}while(currentIndex < totalSize);
			String suffix = "_Rechazo";
			do {
				url = baseUrl + "/list/Characteristic/bySearch?query=" + java.net.URLEncoder.encode("Characteristic.Identifier wildcard \"%" + suffix + "\"", "UTF-8") + "&fields=Characteristic.Identifier&pageSize=300&startIndex=" + currentIndex;
				rawResponse = rc.getRequest( "GET", url, null);
				response = new JSONObject(rawResponse);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					elements++;
					currentId = rows.getJSONObject(i).getJSONArray("values").getString(0);
					nrs
						.put(
								new org.json.JSONObject()
									.put("object",
											new org.json.JSONObject()
												.put("id", "'rmum_" + currentId.substring(0, currentId.length() - suffix.length()) + "'") // rechazo_mensaje_ultima_modificacion
										)
									.put("values",
													new org.json.JSONArray()
														.put("DATETIME")
														.put("")
														.put(currentId + "_Rechazo")
														.put(currentId + "_Rechazo")
														.put(true)
										)
							)
						.put(
								new org.json.JSONObject()
									.put("object",
											new org.json.JSONObject()
												.put("id", "'rma_" + currentId.substring(0, currentId.length() - suffix.length()) + "'") // rechazo_mensaje_accion
										)
									.put("values",
													new org.json.JSONArray()
														.put("LOOKUP")
														.put("RechazoMensajeAccion")
														.put(currentId + "_Rechazo")
														.put(currentId + "_Rechazo")
														.put(true)
										)
							)
						.put(
								new org.json.JSONObject()
									.put("object",
											new org.json.JSONObject()
												.put("id", "'rre_" + currentId.substring(0, currentId.length() - suffix.length()) + "'") // rechazo_rol_emisor
										)
									.put("values",
													new org.json.JSONArray()
														.put("LOOKUP")
														.put("TargetRole")
														.put(currentId + "_Rechazo")
														.put(currentId + "_Rechazo")
														.put(true)
										)
							)
						.put(
								new org.json.JSONObject()
									.put("object",
											new org.json.JSONObject()
												.put("id", "'rrd_" + currentId.substring(0, currentId.length() - suffix.length()) + "'") // rechazo_rol_destino
										)
									.put("values",
													new org.json.JSONArray()
														.put("LOOKUP")
														.put("TargetRole")
														.put(currentId + "_Rechazo")
														.put(currentId + "_Rechazo")
														.put(true)
										)
							)
						.put(
								new org.json.JSONObject()
									.put("object",
											new org.json.JSONObject()
												.put("id", "'rem_" + currentId.substring(0, currentId.length() - suffix.length()) + "'") // rechazo_estatus_mensaje
										)
									.put("values",
													new org.json.JSONArray()
														.put("LOOKUP")
														.put("CommentStatus")
														.put(currentId + "_Rechazo")
														.put(currentId + "_Rechazo")
														.put(true)
										)
							);
					currentIndex++;
					if(currentIndex % 50 == 0) {
						System.out.println("Going to try to insert an array of " + nrs.length() + " elements.");
						requestMessage = new org.json.JSONObject()
								.put("columns",
										new org.json.JSONArray()
											.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.RootCharacteristic"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))
									)
								.put("rows", nrs);
						rawResponse = rc.getRequest("POST", baseUrl + "/list/Characteristic", requestMessage.toString());
						System.out.println(rawResponse);
						nrs = new org.json.JSONArray();
					}
				}
				totalSize = response.getInt("totalSize");
				System.out.println("Going to try to insert an array of " + nrs.length() + " elements.");
				requestMessage = new org.json.JSONObject()
						.put("columns",
								new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.RootCharacteristic"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))
							)
						.put("rows", nrs);
				rawResponse = rc.getRequest("POST", baseUrl + "/list/Characteristic", requestMessage.toString());
				System.out.println(rawResponse);
				nrs = new org.json.JSONArray();
			}while(currentIndex < totalSize);
		}catch(java.io.IOException e) {
			System.out.println(rawResponse);
			e.printStackTrace();
		}
		System.out.println("Elements: " + elements);
	}

	private static void collectRootRejectCharacteristics() throws ServiceUnavailableException {
		String rawResponse = null;
		JSONObject response = null;
		org.json.JSONArray rows = null;
		String url = null;
		int totalSize = 0;
		int currentIndex = 0;
		String currentId = null;
		org.json.JSONArray nrs = new org.json.JSONArray();
		org.json.JSONObject requestMessage = null;
		int elements = 0;
		try {
			do {
				rawResponse = rc.getRequest("GET", baseUrl + "/list/Characteristic/bySearch?metaData=true&fields=Characteristic.IsActive&query=" + java.net.URLEncoder.encode("Characteristic.IsActive equals true and (Characteristic.Identifier wildcard \"rnum_%\" or Characteristic.Identifier wildcard \"rma_%\" or Characteristic.Identifier wildcard \"rre_%\" or Characteristic.Identifier wildcard \"rrd_%\" or Characteristic.Identifier wildcard \"rem_%\")", "UTF-8") + "&pageSize=100", null);
				System.out.println(rawResponse);
				response = new org.json.JSONObject(rawResponse);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					rows.getJSONObject(i).getJSONArray("values").put(0, false);
				}
				System.out.println(rc.getRequest("POST", baseUrl + "/list/Characteristic", response.toString()));
				totalSize = response.getInt("totalSize");
			}while(currentIndex < totalSize);
			do {
				url = baseUrl + "/list/Characteristic/bySearch?query=" + java.net.URLEncoder.encode("Characteristic.Identifier wildcard \"Rechazo_%\"", "UTF-8") + "&fields=Characteristic.Identifier&pageSize=300&startIndex=" + currentIndex;
				rawResponse = rc.getRequest( "GET", url, null);
				response = new JSONObject(rawResponse);
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					elements++;
					currentId = rows.getJSONObject(i).getJSONArray("values").getString(0);
					nrs
						.put(
								new org.json.JSONObject()
									.put("object",
											new org.json.JSONObject()
												.put("id", "'rmum_" + currentId.substring(8) + "'") // rechazo_mensaje_ultima_modificacion
										)
									.put("values",
													new org.json.JSONArray()
														.put("DATETIME")
														.put("")
														.put(currentId + "_Rechazo")
														.put(currentId + "_Rechazo")
														.put(true)
										)
							)
						.put(
								new org.json.JSONObject()
									.put("object",
											new org.json.JSONObject()
												.put("id", "'rma_" + currentId.substring(8) + "'") // rechazo_mensaje_accion
										)
									.put("values",
													new org.json.JSONArray()
														.put("LOOKUP")
														.put("RechazoMensajeAccion")
														.put(currentId + "_Rechazo")
														.put(currentId + "_Rechazo")
														.put(true)
										)
							)
						.put(
								new org.json.JSONObject()
									.put("object",
											new org.json.JSONObject()
												.put("id", "'rre_" + currentId.substring(8) + "'") // rechazo_rol_emisor
										)
									.put("values",
													new org.json.JSONArray()
														.put("LOOKUP")
														.put("TargetRole")
														.put(currentId + "_Rechazo")
														.put(currentId + "_Rechazo")
														.put(true)
										)
							)
						.put(
								new org.json.JSONObject()
									.put("object",
											new org.json.JSONObject()
												.put("id", "'rrd_" + currentId.substring(8) + "'") // rechazo_rol_destino
										)
									.put("values",
													new org.json.JSONArray()
														.put("LOOKUP")
														.put("TargetRole")
														.put(currentId + "_Rechazo")
														.put(currentId + "_Rechazo")
														.put(true)
										)
							)
						.put(
								new org.json.JSONObject()
									.put("object",
											new org.json.JSONObject()
												.put("id", "'rem_" + currentId.substring(8) + "'") // rechazo_estatus_mensaje
										)
									.put("values",
													new org.json.JSONArray()
														.put("LOOKUP")
														.put("CommentStatus")
														.put(currentId + "_Rechazo")
														.put(currentId + "_Rechazo")
														.put(true)
										)
							);
					currentIndex++;
					if(currentIndex % 50 == 0) {
						System.out.println("Going to try to insert an array of " + nrs.length() + " elements.");
						requestMessage = new org.json.JSONObject()
								.put("columns",
										new org.json.JSONArray()
											.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.RootCharacteristic"))
											.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))
									)
								.put("rows", nrs);
						rawResponse = rc.getRequest("POST", baseUrl + "/list/Characteristic", requestMessage.toString());
						System.out.println(rawResponse);
						nrs = new org.json.JSONArray();
					}
				}
				totalSize = response.getInt("totalSize");
				System.out.println("Going to try to insert an array of " + nrs.length() + " elements.");
				requestMessage = new org.json.JSONObject()
						.put("columns",
								new org.json.JSONArray()
									.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.RootCharacteristic"))
									.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))
							)
						.put("rows", nrs);
				rawResponse = rc.getRequest("POST", baseUrl + "/list/Characteristic", requestMessage.toString());
				System.out.println(rawResponse);
				nrs = new org.json.JSONArray();
			}while(currentIndex < totalSize);
		}catch(java.io.IOException e) {
			System.out.println(rawResponse);
			e.printStackTrace();
		}
		System.out.println("Elements: " + elements);
	}

}
