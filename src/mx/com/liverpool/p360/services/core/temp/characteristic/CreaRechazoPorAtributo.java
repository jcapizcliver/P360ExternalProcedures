package mx.com.liverpool.p360.services.core.temp.characteristic;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.temp.characteristic.AttributeListHandler.Attribute;
import mx.com.liverpool.p360.services.core.temp.characteristic.AttributeListHandler.AttributeGroupLink;
import mx.com.liverpool.p360.services.core.temp.characteristic.AttributeListHandler.MetaData;
import mx.com.liverpool.p360.services.core.temp.characteristic.AttributeListHandler.MultiValue;
import mx.com.liverpool.p360.services.core.temp.characteristic.AttributeListHandler.Value;

public class CreaRechazoPorAtributo extends RESTWrapper{

	private java.util.Map<String, String> qp = getQueryParameters();

	private java.util.Map<String, String> getQueryParameters(){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeObjectsInProtocol", "false");
		return qp;
	}
	
	private RequestHandler createCategories = new RequestHandler(
			new org.json.JSONArray()
			.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"))
			.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"))
			, 100, request -> writeData("list", "LookupValue", null, qp, request, System.out::println) );
	
	private RequestHandler createCharacteristics = new RequestHandler(
				new org.json.JSONArray()
					.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Name(es)"))
					.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Description(es)"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.Category"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.Entities"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.Order"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.Purposes"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))
					.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"))
			, 100, request -> {
					createCategories.sendData();
					writeData("list", "Characteristic", null, qp, request, System.out::println); 
				} );
	
	private RequestHandler createCharacteristicsRechazo = new RequestHandler(
			new org.json.JSONArray()
			.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Name(es)"))
			.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"))
			.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"))
			.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"))
			.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"))
			, 100, request -> {
//				createCategories.sendData();
//				createCharacteristics.sendData();
				writeData("list", "Characteristic", null, qp, request, System.out::println); 
			} );
	
	private void addRejection(String _id, String name, boolean firstDelete) {
		if(firstDelete) {
			java.util.Map<String, String> p = new java.util.TreeMap<>();
			RequestHandler rh = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive")), 10, request->{
				writeData("list", "Characteristic", null, p, request, System.out::println);
			});
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'mdr_" + _id + "'")).put("values", new org.json.JSONArray().put(false)));
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'msj_" + _id + "'")).put("values", new org.json.JSONArray().put(false)));
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'rem_" + _id + "'")).put("values", new org.json.JSONArray().put(false)));
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'rma_" + _id + "'")).put("values", new org.json.JSONArray().put(false)));
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'rmum_" + _id + "'")).put("values", new org.json.JSONArray().put(false)));
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'rre_" + _id + "'")).put("values", new org.json.JSONArray().put(false)));
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'rrd_" + _id + "'")).put("values", new org.json.JSONArray().put(false)));
			rh.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + _id + "_Rechazo'")).put("values", new org.json.JSONArray().put(false)));
			rh.sendData();
			p.clear();
			String items = null;
			p.put("items", items = "'mdr_" + _id + "','msj_" + _id + "','rem_" + _id + "','rma_" + _id + "','rmum_" + _id + "','rre_" + _id + "','rrd_" + _id + "'");
			System.out.println(items);
			deleteData("list", "Characteristic", null, "byItems", p, System.out::println);
			p.put("items", items = "'" + _id + "_Rechazo'");
			System.out.println(items);
			deleteData("list", "Characteristic", null, "byItems", p, System.out::println);
		}
		String id = _id + "_Rechazo";
		createCharacteristics.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + id + "'"))
				.put("values", 
					new org.json.JSONArray()
						.put(name + " (Rechazo)")
						.put("")
						.put(_id)
						.put(new org.json.JSONArray().put("Product2G"))
						.put("NONE")
						.put("")
						.put("64535")
						.put(new org.json.JSONArray())
						.put(true)
						.put("")
				)
			);
		createCharacteristicsRechazo.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "mdr_" + _id + "'"))
				.put("values", 
						new org.json.JSONArray()
						.put("Motivo (" + name + ")")
						.put("LOOKUP")
						.put("RejectReazonType")
						.put(true)
						.put(id)
						)
				);
		createCharacteristicsRechazo.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "msj_" + _id + "'"))
				.put("values", 
						new org.json.JSONArray()
						.put("Mensaje (" + name + ")")
						.put("TEXT")
						.put("")
						.put(true)
						.put(id)
						)
				);
		createCharacteristicsRechazo.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "rem_" + _id + "'"))
				.put("values", 
						new org.json.JSONArray()
						.put("Estatus del Rechazo (" + name + ")")
						.put("LOOKUP")
						.put("CommentStatus")
						.put(true)
						.put(id)
						)
				);
		createCharacteristicsRechazo.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "rma_" + _id + "'"))
				.put("values", 
						new org.json.JSONArray()
						.put("Acción Requerida (" + name + ")")
						.put("LOOKUP")
						.put("RechazoMensajeAccion")
						.put(true)
						.put(id)
						)
				);
		createCharacteristicsRechazo.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "rmum_" + _id + "'"))
				.put("values", 
						new org.json.JSONArray()
						.put("Estampa de Tiempo (" + name + ")")
						.put("DATETIME")
						.put("")
						.put(true)
						.put(id)
						)
				);
		createCharacteristicsRechazo.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "rrd_" + _id + "'"))
				.put("values", 
						new org.json.JSONArray()
						.put("Rol Destino (" + name + ")")
						.put("LOOKUP")
						.put("TargetRole")
						.put(true)
						.put(id)
						)
				);
		createCharacteristicsRechazo.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + "rre_" + _id + "'"))
				.put("values", 
						new org.json.JSONArray()
						.put("Rol Emisor (" + name + ")")
						.put("LOOKUP")
						.put("TargetRole")
						.put(true)
						.put(id)
						)
				);
		
	}
	
	public static void main(String[] args) throws ParserConfigurationException, SAXException {
       CreaRechazoPorAtributo crpa = new CreaRechazoPorAtributo();
       crpa.addRejection("CertificadoSostenible", "Certificado Sostenible", false);
       crpa.createCharacteristics.sendData();
       crpa.createCharacteristicsRechazo.sendData();
	}

}
