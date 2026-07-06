package mx.com.liverpool.p360.services.core.temp;

import java.io.IOException;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RestClient;
import mx.com.liverpool.p360.services.core.ServiceUnavailableException;

public class PrevengaLaRata {

	public static void main(String[] args) throws ServiceUnavailableException {

//		String[] atts = ("(VoltajeKVaD)\r\n"
//				+ "(AnchoDeLaBaseVaD)\r\n"
//				+ "(ControlPorVozVaD)\r\n"
//				+ "(SonidoCompatibleVaD)\r\n"
//				+ "(AnoDeFabricacionVaD)\r\n"
//				+ "(CanalDeRetornoDeAudioVaD)\r\n"
//				+ "(CompartirContenidoVaD)\r\n"
//				+ "(CompatibilidadDeRedVaD)\r\n"
//				+ "(DefinicionAudioVideoAtt)\r\n"
//				+ "(DenierVaD)\r\n"
//				+ "(EntradaDeAntenaVaD)\r\n"
//				+ "(EntradaParaAuricularesVaD)\r\n"
//				+ "(EsInalambricoKVaD)\r\n"
//				+ "(TieneFijaciónPantallaVAD)\r\n"
//				+ "(LicenciaPersonajeVaD)\r\n"
//				+ "(MaterialDeLaBaseVaD)\r\n"
//				+ "(PantallaAtt)\r\n"
//				+ "(SiluetaVaD)\r\n"
//				+ "(ModeloComercialVaD)\r\n"
//				+ "(SizeVaD)\r\n"
//				+ "(LuzDeFondoVaD)\r\n"
//				+ "(FrecuenciaBarridoVerticaVaDl)\r\n"
//				+ "(ServiciosDeStreamingVaD)\r\n"
//				+ "(AnchoDelSoporteVaD)\r\n"
//				+ "(TecnologiaDeMovimientoVaD)\r\n"
//				+ "(ControlSmartVaD)\r\n"
//				+ "(NumeroDePuertoUsbSVaD)\r\n"
//				+ "(SalidasDigitalesDeAudioOpticoVaD)\r\n"
//				+ "(SalidaDeLosAltavocesVaD)\r\n"
//				+ "(GarantiaDelFabricanteVAD)\r\n"
//				+ "(IncluyeCampanaVaD)\r\n"
//				+ "(Número total de puertos HDMI 2.0)\r\n"
//				+ "(ProductTypeSAP)\r\n"
//				+ "(Producto)").split("\\r\\n");
//		System.exit(0);

		org.json.JSONObject elobJect = new org.json.JSONObject();
		// { "name":"EL NEIM", "taskType": "SingleTask", "entity":"Article", "userGroup": "QA" }
		elobJect.put("task",
				new org.json.JSONObject()
					.put("id", "94005")
					.put("name", "QA Review")
					.put("taskType", "SingleTask")
//					.put("dynamic", "")
					.put("queryMode", "dynamic")
					.put("entity", "Product2G")
					.put("userGroup", "LiderCalidad")
					.put("description", "Tarea de Revisión QA.")
		);
		elobJect.put("content",
				new org.json.JSONObject()
					.put("identifier", "bySearch")
					.put("parameterList",
						new org.json.JSONArray()
							.put(
								new org.json.JSONObject()
									.put("key", "query")
									.put("value", "Product2G.CurrentStatus equals 1022")
								)
						)
				);
		System.out.println(elobJect);
//		System.exit(0);
		String rr = null;
		String baseUrl = "https://webctep360dev.liverpool.com.mx/rest/V2.0";
		RestClient rc = new RESTWorkshop().getRc();
		try {
			System.out.println("Now going to request...");
			rr = rc.getRequest("POST", baseUrl + "/manage/task", elobJect.toString());
			System.out.println("{\"action\":\"sentToYou\"}");
			System.out.println(rr);
		} catch ( IOException e) {
			System.out.println("{\"action\":\"fail\"}");
			System.out.println(rr);
			e.printStackTrace();
		}
	}
}
