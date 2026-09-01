package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class AITemplatesCreation {

	private final RESTWrapper rw = new RESTWrapper();
	
	private class ListaDeValores{
		
		private String id = null;
		private String etiqueta = null;
		private final java.util.List<Value> valores = new java.util.ArrayList<>();
		
	}
	
	private class Atributo{
		
		private String id = null;
		private String nombre = null;
		private String tipoDeDato = null;
		private String idListaDeValores = null;
		private String description = null;
		private Integer displaySequence = null;
		private String isFaceted = null;
		private final java.util.List<Value> valoresMetadatos = new java.util.ArrayList<>(); 
        private final java.util.LinkedList<MultiValor> multiValoresMetadatos = new java.util.LinkedList<>();
        private final java.util.List<String> attributeGroupLink = new java.util.ArrayList<>();
		
	}
	
	private class AtributoPlantilla{
		
		private String id = null;
		private String mandatory = null;
		private final java.util.List<String> valueFilter = new java.util.ArrayList<>();
		private final java.util.List<Value> valoresMetadata = new java.util.ArrayList<>();
		
	}
	
	private class Plantilla{
		
		private String id = null;
		private String parentId = null;
		private String nombre = null;
		private final java.util.LinkedList<AtributoPlantilla> atributos = new java.util.LinkedList<>();
		
	}
	
	private class MultiValor{
		
		private String attributeId = null;
		private java.util.List<Value> values = new java.util.ArrayList<>();
	}
	
	private class Value{

    	private String attributeId;
    	private String id;
    	private String text;
    	private String unidadId;
    	
    	public Value(String attributeId, String id, String unidadId) {
    		this.attributeId = attributeId;
    		this.id = id;
    		this.unidadId = unidadId;
    	}

    }
	
	public class Handler extends DefaultHandler {
    	
        private final java.util.LinkedList<Plantilla> pilaDePlantillas = new java.util.LinkedList<>();
        private final java.util.List<Plantilla> finished = new ArrayList<>();
        private final java.util.LinkedList<ListaDeValores> pilaDeListasDeValores = new java.util.LinkedList<>();
        private final java.util.LinkedList<Atributo> pilaDeAtributos = new java.util.LinkedList<>();
        private final java.util.List<ListaDeValores> listasDeValores = new java.util.ArrayList<>();
        private final java.util.List<Atributo> atributos = new java.util.ArrayList<>();
        private final java.util.List<Value> bloqueActualDeMetadata = new java.util.ArrayList<>();
        private final java.util.List<Value> bloqueMultiValor = new java.util.ArrayList<>();
        
        private boolean isAttributeList = false;
        private boolean isListOfValuesList = false;
        private boolean isPlantillas = false;
        private boolean isMetadata = false;
        private boolean isValueFilter = false;
        private boolean isValue = false;
        private boolean isName = false;
        private boolean isMultiValue = false;

        private Value value = null;
        
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            String name = localName != null && !localName.isEmpty() ? localName : qName;
            if("ListsOfValues".equals(name)) {
            	isListOfValuesList = true;
            	isAttributeList = false;
            	isPlantillas = false;
            }else if("AttributeList".equals(name)) {
            	isListOfValuesList = false;
            	isAttributeList = true;
            	isPlantillas = false;
            }else if("Products".equals(name)) {
            	isListOfValuesList = false;
            	isAttributeList = false;
            	isPlantillas = true;
            }else if("ListOfValue".equals(name) && isListOfValuesList) {
            	ListaDeValores ldv = new ListaDeValores();
            	ldv.id = attributes.getValue("ID");
            	pilaDeListasDeValores.addLast(ldv);
            }else if("Attribute".equals(name) && isAttributeList) {
            	Atributo atributo = new Atributo();
            	atributo.id = attributes.getValue("ID");
            	atributo.tipoDeDato = attributes.getValue("DataType");
            	pilaDeAtributos.addLast(atributo);
            }else if("Product".equals(name) && isPlantillas) {
            	Plantilla p = new Plantilla();
            	p.id = attributes.getValue("ID");
            	p.parentId = attributes.getValue("ParentID");
            	pilaDePlantillas.addLast(p);
            }else if("AttributeLink".equals(name) && isPlantillas) {
            	if(!pilaDePlantillas.isEmpty()) {
            		Plantilla p = pilaDePlantillas.getLast();
            		AtributoPlantilla ap = new AtributoPlantilla();
            		ap.id = attributes.getValue("AttributeID");
            		ap.mandatory = attributes.getValue("Mandatory");
            		p.atributos.addLast(ap);
            	}
            }else if("ValueFilter".equals(name)) {
            	isValueFilter = true;
            }else if("MetaData".equals(name)) {
            	isMetadata = true;
            }else if("AttributeGroupLink".equals(name)) {
            	if(!pilaDeAtributos.isEmpty()) {
            		Atributo a = pilaDeAtributos.getLast();
            		a.attributeGroupLink.add(attributes.getValue("AttributeGroupID"));
            	}
            }else if("MultiValue".equals(name)) {
            	isMultiValue = true;
            	if(!pilaDeAtributos.isEmpty()) {
            		Atributo a = pilaDeAtributos.getLast();
            		MultiValor mv = new MultiValor();
            		mv.attributeId = attributes.getValue("AttributeID");
            		a.multiValoresMetadatos.addLast( mv );
            	}
            }else if("ListOfValueLink".equals(name)) {
            	if(!pilaDeAtributos.isEmpty()) {
            		Atributo a = pilaDeAtributos.getLast();
            		a.tipoDeDato = "LOOKUP";
            		a.idListaDeValores = attributes.getValue("ListOfValueID");
            	}
            }else if("Name".equals(name)) {
            	isName = true;
            }else if("Value".equals(name)) {
            	isValue = true;
            	value = new Value( attributes.getValue("AttributeID"), attributes.getValue("ID"), attributes.getValue("UnitID") );
            	if(isMultiValue) {
            		bloqueMultiValor.add(value);
            	}else if(isMetadata) {
            		bloqueActualDeMetadata.add(value);
            	}else if(isValueFilter) {
            		if(!pilaDePlantillas.isEmpty()) {
            			Plantilla p = pilaDePlantillas.getLast();
            			if(!p.atributos.isEmpty()) {
            				AtributoPlantilla ap = p.atributos.getLast();
            				ap.valueFilter.add(value.id == null ? value.text : value.id);
            			}
            		}
            	}else if(isListOfValuesList) {
            		if(!pilaDeListasDeValores.isEmpty()) {
            			ListaDeValores ldv = pilaDeListasDeValores.getLast();
            			ldv.valores.add(value);
            		}
            	}
            }

        }

        @Override
        public void characters(char[] ch, int start, int length) {
        	if(isName) {
        		if(isListOfValuesList) {
        			if(!pilaDeListasDeValores.isEmpty()) {
        				ListaDeValores ldv = pilaDeListasDeValores.getLast();
        				StringBuilder sb = new StringBuilder();
        				sb.append(ldv.etiqueta == null ? "" : ldv.etiqueta);
        				sb.append( ch, start, length );
        				ldv.etiqueta = sb.toString();
        			}
        		}else if(isAttributeList) {
        			if(!pilaDeAtributos.isEmpty()) {
        				Atributo a = pilaDeAtributos.getLast();
        				StringBuilder sb = new StringBuilder();
        				sb.append(a.nombre == null ? "" : a.nombre);
        				sb.append( ch, start, length );
        				a.nombre = sb.toString();
        			}
        		}else if(isPlantillas) {
        			if(!pilaDePlantillas.isEmpty()) {
        				Plantilla p = pilaDePlantillas.getLast();
        				StringBuilder sb = new StringBuilder();
        				sb.append(p.nombre == null ? "" : p.nombre);
        				sb.append( ch, start, length );
        				p.nombre = sb.toString();
        			}
        		}
        	}else if(isValue) {
        		StringBuilder sb = new StringBuilder();
				sb.append(value.text == null ? "" : value.text);
				sb.append( ch, start, length );
				value.text = sb.toString();
        	}
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String name = localName != null && !localName.isEmpty() ? localName : qName;
            if("Name".equals(name)) {
            	isName = false;
            }else if("Value".equals(name)) {
            	isValue = false;
            	value = null;
            }else if("MetaData".equals(name)) {
            	isMetadata = false;
            	if(isPlantillas) {
            		if(!pilaDePlantillas.isEmpty()) {
            			Plantilla p = pilaDePlantillas.getLast();
            			if(!p.atributos.isEmpty()) {
            				AtributoPlantilla ap = p.atributos.getLast();
            				bloqueActualDeMetadata.forEach( ap.valoresMetadata::add );
            			}
            		}
            	}else if(isAttributeList) {
            		if(!pilaDeAtributos.isEmpty()) {
            			Atributo a = pilaDeAtributos.getLast();
            			bloqueActualDeMetadata.forEach( a.valoresMetadatos::add );
            		}
            	}
            	bloqueActualDeMetadata.clear();
            }else if("MultiValue".equals(name)) {
            	isMultiValue = false;
            	if(isMetadata) {
            		if(isAttributeList) {
            			if(!pilaDeAtributos.isEmpty()) {
            				Atributo a = pilaDeAtributos.getLast();
            				for(Value v : bloqueMultiValor) {
            					a.multiValoresMetadatos.getLast().values.add(v);
            					log("Added values: (" + v.id + ") " + v.text);
            				}
            			}
            		}
            	}
            	bloqueMultiValor.clear();
            }else if("Attribute".equals(name)) {
            	if(!pilaDeAtributos.isEmpty()) {
            		atributos.add(pilaDeAtributos.removeLast());
            	}
            }else if("ListOfValue".equals(name)) {
            	if(!pilaDeListasDeValores.isEmpty()) {
            		listasDeValores.add(pilaDeListasDeValores.removeLast());
            	}
            }else if("Product".equals(name)) {
            	if(!pilaDePlantillas.isEmpty()) {
            		finished.add(pilaDePlantillas.removeLast());
            	}
            }
        }
    }
	
	public void processData(java.io.ByteArrayInputStream bais) throws ParserConfigurationException, SAXException, IOException {
		long init = System.currentTimeMillis();
		log("Got element");
		SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities",          false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",        false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
        SAXParser parser = factory.newSAXParser();
        Handler handler = new Handler();
        parser.parse(bais, handler);
        org.json.JSONArray columns = new org.json.JSONArray();
        org.json.JSONArray rows = new org.json.JSONArray();
        org.json.JSONObject request = new org.json.JSONObject();
        request.put("columns", columns);
        request.put("rows", rows);
        columns.put(new org.json.JSONObject().put("identifier", "LookupLang.Name(es)"));
        java.util.Map<String, String> qp = new java.util.HashMap<>();
        qp.put("includeObjectsInProtocol", "false");
        org.json.JSONArray columnsLookupValue = new org.json.JSONArray();
        org.json.JSONObject requestLookupValue = new org.json.JSONObject();
        org.json.JSONArray rowsLookupValue = new org.json.JSONArray();
        columnsLookupValue.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
        columnsLookupValue.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
        requestLookupValue.put("columns", columnsLookupValue);
        requestLookupValue.put("rows", rowsLookupValue);
        log("Writing lookups:");
        for(ListaDeValores ldv : handler.listasDeValores) {
        	rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + ldv.id + "'")).put("values", new org.json.JSONArray().put( ldv.etiqueta == null ? ldv.id + " (IA)" : ldv.etiqueta )));
        	if(rows.length() == 2000) {
        		rw.writeData("list", "Lookup", null, qp, request, this::log);
        		while(rows.length() > 0) {
    				rows.remove(0);
    			}
        	}
        }
        if(rows.length() > 0) {
        	rw.writeData("list", "Lookup", null, qp, request, this::log);
        	while(rows.length() > 0) {
				rows.remove(0);
			}
        }
        log("Writing lookup values.");
        for(ListaDeValores ldv : handler.listasDeValores) {
        	for(Value v : ldv.valores) {
        		rowsLookupValue.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + v.id.replaceAll("'", "\\'") + "'@'" + ldv.id + "'")).put("values", new org.json.JSONArray().put(v.text != null ? v.text : "").put(true)));
        		if(rowsLookupValue.length() == 2000) {
        			rw.writeData("list", "LookupValue", null, qp, requestLookupValue, this::log);
        			while(rowsLookupValue.length() > 0) {
        				rowsLookupValue.remove(0);
        			}
        		}
        	}
        }
        if(rowsLookupValue.length() > 0) {
        	rw.writeData("list", "LookupValue", null, qp, requestLookupValue, this::log);
        	while(rowsLookupValue.length() > 0) {
				rowsLookupValue.remove(0);
			}
        }
        log("Writing attributes");
        for(Atributo a : handler.atributos) {
        	rowsLookupValue.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + a.id + "'@'CharacteristicCategories'")).put("values", new org.json.JSONArray().put(a.nombre != null ? a.nombre : "").put(true)));
        	if(rowsLookupValue.length() == 2000) {
    			rw.writeData("list", "LookupValue", null, qp, requestLookupValue, this::log);
    			while(rowsLookupValue.length() > 0) {
    				rowsLookupValue.remove(0);
    			}
    		}
        }
        if(rowsLookupValue.length() > 0) {
        	rw.writeData("list", "LookupValue", null, qp, requestLookupValue, this::log);
        	while(rowsLookupValue.length() > 0) {
				rowsLookupValue.remove(0);
			}
        }
        columns = new org.json.JSONArray();
        columns.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Name(es)"));
        columns.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Description(es)"));
        columns.put(new org.json.JSONObject().put("identifier", "Characteristic.Entities"));
        columns.put(new org.json.JSONObject().put("identifier", "Characteristic.Category"));
        columns.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"));
        columns.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"));
        columns.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"));
        columns.put(new org.json.JSONObject().put("identifier", "Characteristic.Order"));
        columns.put(new org.json.JSONObject().put("identifier", "Characteristic.Purposes"));
        request.put("columns", columns);
        org.json.JSONArray purposes = null;
        columnsLookupValue = new org.json.JSONArray();
        requestLookupValue.put("columns", columnsLookupValue);
        columnsLookupValue.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
        columnsLookupValue.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
        columnsLookupValue.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('AttributeGroup')"));
        columnsLookupValue.put(new org.json.JSONObject().put("identifier", "LookupValueReference.LookupValues('ATGAttributeGroups')"));
        org.json.JSONArray ags = null;
        org.json.JSONArray agrupaciones = new org.json.JSONArray();
        org.json.JSONArray columnsCharRej = new org.json.JSONArray();
        org.json.JSONArray rowsCharRej = new org.json.JSONArray();
        org.json.JSONObject reqCharRej = new org.json.JSONObject();
        reqCharRej.put("columns", columnsCharRej);
        reqCharRej.put("rows", rowsCharRej);
        org.json.JSONArray columnsCharRejChild = new org.json.JSONArray();
        org.json.JSONArray rowsCharRejChild = new org.json.JSONArray();
        org.json.JSONObject reqCharRejChild = new org.json.JSONObject();
        reqCharRejChild.put("columns", columnsCharRejChild);
        reqCharRejChild.put("rows", rowsCharRejChild);
        columnsCharRej.put(new org.json.JSONObject().put("identifier", "Characteristic.Category"));
        columnsCharRej.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"));
        columnsCharRej.put(new org.json.JSONObject().put("identifier", "Characteristic.Entities"));
        columnsCharRej.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"));
        columnsCharRejChild.put(new org.json.JSONObject().put("identifier", "Characteristic.DataType"));
        columnsCharRejChild.put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive"));
        columnsCharRejChild.put(new org.json.JSONObject().put("identifier", "CharacteristicLang.Name(es)"));
        columnsCharRejChild.put(new org.json.JSONObject().put("identifier", "Characteristic.ParentCharacteristic"));
        columnsCharRejChild.put(new org.json.JSONObject().put("identifier", "Characteristic.Lookup"));
        log("Now processing attributes from handler.");
        for(Atributo a : handler.atributos) {
        	purposes = new org.json.JSONArray();
        	ags = new org.json.JSONArray();
        	agrupaciones = new org.json.JSONArray();
        	for(Value v : a.valoresMetadatos) {
        		if("AttributeHelpText".equals( v.attributeId )) {
        			a.description = v.text;
        		}else if("DisplaySequence".equals( v.attributeId )) {
        			a.displaySequence = Integer.parseInt( v.text );
        		}else if("isFaceted".equals( v.attributeId )) {
        			a.isFaceted = v.text;
        		}
        	}
        	if( Boolean.parseBoolean(a.isFaceted) ) {
        		purposes.put("isFaceted");
        	}
        	for(MultiValor mv : a.multiValoresMetadatos) {
        		if("isAttInGroupAtt".equals(mv.attributeId)) {
        			for(Value v : mv.values) {
        				purposes.put(v.id);
        				ags.put(v.id);
        			}
        		}
        	}
        	for(String agl : a.attributeGroupLink) {
        		agrupaciones.put(agl);
        	}
        	rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + a.id + "'")).put("values", new org.json.JSONArray()
        			.put( a.nombre != null ? a.nombre : "" )
        			.put( a.description != null ? a.description : "" )
        			.put( "Product2G" )
        			.put( a.id )
        			.put( true )
        			.put( a.idListaDeValores != null ? "LOOKUP" : "TEXT" )
        			.put( a.idListaDeValores == null ? "" : a.idListaDeValores )
        			.put( a.displaySequence != null ? a.displaySequence : "" )
        			.put( purposes.length() == 0 ? "" : purposes )
        		));
        	if(rows.length() == 2000) {
        		rw.writeData("list", "Characteristic", null, qp, request, this::log);
        		while(rows.length() > 0) {
        			rows.remove(0);
        		}
        	}
        	if(ags.length() > 0 || agrupaciones.length() > 0) {
        		rowsLookupValue.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + a.id + "'@'Characteristics'")).put("values", new org.json.JSONArray().put(true).put(a.nombre == null ? "" : a.nombre).put(agrupaciones.length() == 0 ? "" : agrupaciones).put( ags.length() > 0 ? ags : "" )));
        		if(rowsLookupValue.length() == 2000) {
        			rw.writeData("list", "LookupValue", null, qp, requestLookupValue, this::log);
        			while(rowsLookupValue.length() > 0) {
        				rowsLookupValue.remove(0);
        			}
        		}
        	}
        	rowsCharRej.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + a.id + "_Rechazo'")).put("values", new org.json.JSONArray().put(a.id).put("NONE").put("Product2G").put(true)));
        	rowsCharRejChild.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'mdr_" + a.id + "'")).put("values", new org.json.JSONArray().put("LOOKUP").put(true).put("Motivo de Rechazo").put(a.id + "_Rechazo").put("RejectReazonType")));
        	rowsCharRejChild.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'msj_" + a.id + "'")).put("values", new org.json.JSONArray().put("TEXT").put(true).put("Mensaje de Rechazo").put( a.id + "_Rechazo").put("")));
        	rowsCharRejChild.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'rem_" + a.id + "'")).put("values", new org.json.JSONArray().put("LOOKUP").put(true).put("Estado de Rechazo").put(a.id + "_Rechazo").put("CommentStatus")));
        	rowsCharRejChild.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'rma_" + a.id + "'")).put("values", new org.json.JSONArray().put("LOOKUP").put(true).put("Acción sobre Mensaje de Rechazo").put( a.id + "_Rechazo").put("RechazoMensajeAccion")));
        	rowsCharRejChild.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'rmum_" + a.id + "'")).put("values", new org.json.JSONArray().put("DATETIME").put(true).put("Fecha de Rechazo").put(a.id + "_Rechazo").put("")));
        	rowsCharRejChild.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'rrd_" + a.id + "'")).put("values", new org.json.JSONArray().put("LOOKUP").put(true).put("Destino de Rechazo").put(a.id + "_Rechazo").put("TargetRole")));
        	rowsCharRejChild.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'rre_" + a.id + "'")).put("values", new org.json.JSONArray().put("LOOKUP").put(true).put("Emisor de Rechazo").put(a.id + "_Rechazo").put("TargetRole")));
        	if(rowsCharRej.length() == 2000) {
        		log("Sending rejection characteristic creations.");
        		rw.writeData("list", "Characteristic", null, qp, reqCharRej, this::log);
    			while(rowsCharRej.length() > 0) {
    				rowsCharRej.remove(0);
    			}
    			log("Sending child rejection characteristic creations.");
    			rw.writeData("list", "Characteristic", null, qp, reqCharRejChild, this::log);
    			while(rowsCharRejChild.length() > 0) {
    				rowsCharRejChild.remove(0);
    			}
        	}
        }
        if(rows.length() > 0) {
        	log("Last characteristic creations.");
        	rw.writeData("list", "Characteristic", null, qp, request, this::log);
    		while(rows.length() > 0) {
    			rows.remove(0);
    		}
        }
        if(rowsLookupValue.length() > 0) {
        	log("Last lookup value creations.");
			rw.writeData("list", "LookupValue", null, qp, requestLookupValue, this::log);
			while(rowsLookupValue.length() > 0) {
				rowsLookupValue.remove(0);
			}
		}
        if(rowsCharRej.length() > 0) {
        	log("Sending rejection characteristic creations.");
        	rw.writeData("list", "Characteristic", null, qp, reqCharRej, this::log);
			while(rowsCharRej.length() > 0) {
				rowsCharRej.remove(0);
			}
        }
        if(rowsCharRejChild.length() > 0) {
        	log("Sending child rejection characteristic creations.");
        	rw.writeData("list", "Characteristic", null, qp, reqCharRejChild, this::log);
        	while(rowsCharRejChild.length() > 0) {
        		rowsCharRejChild.remove(0);
        	}
        }
        org.json.JSONObject reqStructureGroup = new org.json.JSONObject();
        org.json.JSONArray columnsStructureGroup = new org.json.JSONArray();
        org.json.JSONArray rowsStructureGroup = new org.json.JSONArray();
        reqStructureGroup.put("columns", columnsStructureGroup);
        reqStructureGroup.put("rows", rowsStructureGroup);
        columns = new org.json.JSONArray();
        request.put("columns", columns);
        columns.put(new org.json.JSONObject().put("identifier", "LookupValue.IsActive"));
        columns.put(new org.json.JSONObject().put("identifier", "LookupValueLang.Name(es)"));
        log("Now processing template... (for PPH_L4_Templates and PrimaryProductTaxonomy)");
        for(Plantilla p : handler.finished) {
        	log("Read " + p.id);
        	rowsStructureGroup.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + p.id + "'@'PrimaryProductTaxonomy'")).put("values", new org.json.JSONArray()));
        	if(rowsStructureGroup.length() == 2000) {
        		rw.writeData("list", "StructureGroup", null, qp, reqStructureGroup, this::log);
            	while(rowsStructureGroup.length() > 0) {
            		rowsStructureGroup.remove(0);
            	}
        	}
        	rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + p.id + "'@'PPH_L4_Templates'")).put("values", new org.json.JSONArray().put(true).put( p.nombre != null ? p.nombre : "" )));
        	if(rows.length() == 2000) {
        		rw.writeData("list", "LookupValue", null, qp, request, this::log);
        		while(rows.length() > 0) {
        			rows.remove(0);
        		}
        	}
        }
        if(rows.length() > 0) {
        	rw.writeData("list", "LookupValue", null, qp, request, this::log);
    		while(rows.length() > 0) {
    			rows.remove(0);
    		}
        }
        if(reqStructureGroup.length() > 0) {
        	rw.writeData("list", "StructureGroup", null, qp, reqStructureGroup, this::log);
        	while(rowsStructureGroup.length() > 0) {
        		rowsStructureGroup.remove(0);
        	}
        }
        columnsStructureGroup.put(new org.json.JSONObject().put("identifier", "StructureGroup.ParentIdentifier"));
        log("Now updating PrimaryProductTaxonomy for parent adoption.");
        for(Plantilla p : handler.finished) {
        	rowsStructureGroup.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + p.id + "'@'PrimaryProductTaxonomy'")).put("values", new org.json.JSONArray().put(p.parentId)));
        	if(rowsStructureGroup.length() == 2000) {
        		rw.writeData("list", "StructureGroup", null, qp, reqStructureGroup, this::log);
            	while(rowsStructureGroup.length() > 0) {
            		rowsStructureGroup.remove(0);
            	}
        	}
        }
        if(reqStructureGroup.length() > 0) {
        	rw.writeData("list", "StructureGroup", null, qp, reqStructureGroup, this::log);
        	while(rowsStructureGroup.length() > 0) {
        		rowsStructureGroup.remove(0);
        	}
        }
        org.json.JSONObject reqStandardizationValue = new org.json.JSONObject();
        org.json.JSONArray columnsStandardizationValue = new org.json.JSONArray();
        org.json.JSONArray rowsStandardizationValue = new org.json.JSONArray();
        reqStandardizationValue.put("columns", columnsStandardizationValue);
        reqStandardizationValue.put("rows", rowsStandardizationValue);
        columnsStandardizationValue.put(new org.json.JSONObject().put("identifier", "LookupValue.StructureGroup"));
        columnsStandardizationValue.put(new org.json.JSONObject().put("identifier", "LookupValue.Characteristic"));
        columnsStandardizationValue.put(new org.json.JSONObject().put("identifier", "LookupValue.CreationType"));
        columnsStandardizationValue.put(new org.json.JSONObject().put("identifier", "LookupValue.Property"));
        columnsStandardizationValue.put(new org.json.JSONObject().put("identifier", "LookupValue.PropertyValue"));
        StringBuilder sb = new StringBuilder();
        log("Now writing metadata to dictionary");
        for(Plantilla p : handler.finished) {
        	for( AtributoPlantilla ap : p.atributos ) {
        		rowsStandardizationValue.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + p.id + "<::>" + ap.id + "<::>CreateProposal<::>Business" + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(p.id).put(ap.id).put("CreateProposal").put("Business").put("Liverpool Marketplace")));
        		rowsStandardizationValue.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + p.id + "<::>" + ap.id + "<::>CreateProposal<::>IsMandatory" + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(p.id).put(ap.id).put("CreateProposal").put("IsMandatory").put(ap.mandatory)));
        		rowsStandardizationValue.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + p.id + "<::>" + ap.id + "<::>CreateProposal<::>VendorCenterSection" + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(p.id).put(ap.id).put("CreateProposal").put("VendorCenterSection").put("Atributos")));
        		rowsStandardizationValue.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + p.id + "<::>" + ap.id + "<::>CreateProposal<::>SentToVendorCenter" + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(p.id).put(ap.id).put("CreateProposal").put("SentToVendorCenter").put("1")));
        		if(ap.valueFilter != null && !ap.valueFilter.isEmpty()) {
        			for(int i=0; i<ap.valueFilter.size(); i++) {
        				sb.append(i == 0 ? "" : ",");
        				sb.append(ap.valueFilter.get(i));
        			}
        			rowsStandardizationValue.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + p.id + "<::>" + ap.id + "<::>CreateProposal<::>ListOfValuesFilter" + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(p.id).put(ap.id).put("CreateProposal").put("ListOfValuesFilter").put(sb.toString())));
        			sb.setLength(0);
        		}
        		for(Value v : ap.valoresMetadata) {
        			if("RelevantForATG".equals(v.attributeId)) {
        				rowsStandardizationValue.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + p.id + "<::>" + ap.id + "<::>CreateProposal<::>RelevantForATG" + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(p.id).put(ap.id).put("CreateProposal").put("RelevantForATG").put(v.id)));
        			}
        		}
        		if(rowsStandardizationValue.length() >= 2000) {
        			rw.writeData("list", "LookupValue", null, qp, reqStandardizationValue, this::log);
        			while(rowsStandardizationValue.length() > 0) {
        				rowsStandardizationValue.remove(0);
        			}
        		}
        	}
        }
        if(rowsStandardizationValue.length() > 0) {
        	rw.writeData("list", "LookupValue", null, qp, reqStandardizationValue, this::log);
			while(rowsStandardizationValue.length() > 0) {
				rowsStandardizationValue.remove(0);
			}
        }
        /*
        for( Atributo a : handler.atributos ) {
        	log(a.id + " - " + a.nombre + " - " + a.description);
        	for(MultiValor mv : a.multiValoresMetadatos) {
        		log("\t" + mv.attributeId);
        		for(Value v : mv.values) {
        			log("\t\tAttributeID: " + v.attributeId + " ID: " + v.id + " - " + v.text);
        		}
        	}
        	log("MetaData");
        	for(Value v : a.valoresMetadatos) {
        		log("\t" + v.attributeId + " (" + v.id + ") " + v.text);
        	}
        	log("Attribute Groups: ");
        	for(String atgl : a.attributeGroupLink) {
        		log("\t" + atgl);
        	}
        }
        log("Lkps: ");
        for( ListaDeValores ldv : handler.listasDeValores ) {
        	log(ldv.id + " (" + ldv.etiqueta + ")");
        	for(Value v : ldv.valores) {
        		log("\t" + v.attributeId + " (" + v.id + ") - " + v.text);
        	}
        }
        log("Plantillas");
        for(Plantilla p : handler.finished) {
        	log(p.id + " (" + p.nombre + "), parent: " + p.parentId);
        	for(AtributoPlantilla ap : p.atributos ) {
        		log("\t" + ap.id + " is mandatory? " + ap.mandatory);
        		log("\tFiltro:");
        		for(String v : ap.valueFilter) {
        			log("\t\t" + v);
        		}
        		log("Metadata:");
        		for(Value v : ap.valoresMetadata ) {
        			log("\t" + v.attributeId + " (" + v.id + ") - " + v.text);
        		}
        	}
        }
        */
        log("Done. " + rw.getRw().formatTime(System.currentTimeMillis() - init));
	}


	private void log(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream("../logs/iaTemplates.log", true) ))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date())) + "]  " + message);
        } catch (java.io.IOException e) {
        }
    }

    private void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter( new java.io.FileOutputStream("../logs/iaTemplates.log", true) ))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }
	
}
