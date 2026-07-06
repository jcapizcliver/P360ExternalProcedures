package mx.com.liverpool.p360.services.core.temp.standardizationdictionaries;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.PubSubGCP;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;
import mx.com.liverpool.p360.services.core.temp.standardizationdictionaries.xml.AttributeLinkPPH;
import mx.com.liverpool.p360.services.core.temp.standardizationdictionaries.xml.MultiValuePPH;
import mx.com.liverpool.p360.services.core.temp.standardizationdictionaries.xml.NodePPH;
import mx.com.liverpool.p360.services.core.temp.standardizationdictionaries.xml.ValuePPH;
import mx.com.liverpool.p360.services.core.temp.standardizationdictionaries.xml.XMLHandlerJerarquiaProductosSTEP;

public class ReadTemplateMetadata {
	
	private static final RESTWrapper rw = new RESTWrapper();
	private static final org.json.JSONObject request = new org.json.JSONObject();
	private static final org.json.JSONArray rows = new org.json.JSONArray();
	private static final org.json.JSONArray columns = new org.json.JSONArray();
	private static final org.json.JSONArray templates = new org.json.JSONArray();
	private static final java.util.Map<String, String> allowedBusiness = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> vendorCenterSection = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> suburbia = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> liverpool = new java.util.TreeMap<>();
	private static final java.util.Map<String, String> global = new java.util.TreeMap<>();
	
	private static final String sa = PropertiesManager.get("p360.contingency.gcp.service_account_back");
	private static final String pubSubProject = PropertiesManager.get("p360.contingency.gcp.project_back");
	
	private static final java.util.Map<String, String> writeQP = readWriteQP();
	
	private static java.util.Map<String, String> readWriteQP(){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("includeObjectsInProtocol", "false");
		return qp;
	}
	
	private static final RequestHandler requestTemplate = new RequestHandler(new org.json.JSONArray()
				.put(new org.json.JSONObject().put( "identifier", "StructureGroupLang.Name(es)"))
			, 1000, request -> {
				rw.writeData("list", "StructureGroup", null, writeQP, request, System.out::println);
			});
	private static final RequestHandler requestForParentAssignment = new RequestHandler(new org.json.JSONArray()
				.put(new org.json.JSONObject().put( "identifier", "StructureGroup.ParentIdentifier"))
			, 1000, request -> {
				rw.writeData("list", "StructureGroup", null, writeQP, request, System.out::println);
			});
	private static final RequestHandler requestPPHL4Templates = new RequestHandler(new org.json.JSONArray()
				.put(new org.json.JSONObject().put( "identifier", "LookupValueLang.Name(es)"))
				.put(new org.json.JSONObject().put( "identifier", "LookupValue.IsActive"))
			, 1000, request -> {
				rw.writeData("list", "LookupValue", null, writeQP, request, System.out::println);
			});
	private static final RequestHandler requestGroupFeature = new RequestHandler(new org.json.JSONArray()
				.put(new org.json.JSONObject().put( "identifier", "StructureGroupAttributeValue.Value(Spanish,DEFAULT)"))
			, 1000, request -> {
				rw.writeData("list", "StructureGroup", "StructureGroupAttribute", writeQP, request, System.out::println);
			});
	private static final java.util.Set<String> bannedCharacteristics = new java.util.TreeSet<>();
	
	private static final int bs = 5000;
	
	static {
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.StructureGroup"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Characteristic"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.CreationType"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.Property"));
		columns.put(new org.json.JSONObject().put("identifier", "StandardizationValue.PropertyValue"));
		request.put("columns", columns);
		request.put("rows", rows);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", 
				  "StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.PropertyValue"
			);
		qp.put("query", 
				"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
				+ " and StandardizationValue.CreationType = \"CreateProposal\""
				+ " and StandardizationValue.Property = \"Business\""
			);
		qp.put("pageSize", "10000");
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
			allowedBusiness.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1));
		}, System.out::println);
		qp.clear();
		qp.put("fields", 
				"StandardizationValue.Characteristic->Characteristic.Identifier"
						+ ",StandardizationValue.PropertyValue"
				);
		qp.put("query", 
				"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla\""
						+ " and StandardizationValue.CreationType = \"CreateProposal\""
						+ " and StandardizationValue.Property = \"VendorCenterSection\""
				);
		qp.put("pageSize", "10000");
		qp.put("dictionaryProxy", "'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'");
		rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
			vendorCenterSection.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1));
		}, System.out::println);
		qp.clear();
		qp.put("fields", 
				"StandardizationValue.Characteristic->Characteristic.Identifier"
						+ ",StandardizationValue.PropertyValue"
				);
		qp.put("query", 
				"StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"GlobalTemplateAttributeConfiguration\""
						+ " and StandardizationValue.CreationType = \"CreateProposal\""
						+ " and StandardizationValue.Property = \"Business\""
				);
		qp.put("pageSize", "10000");
		qp.put("dictionaryProxy", "'GlobalTemplateAttributeConfiguration'");
		rw.collectData("list", "StandardizationValue", null, "bySearch", qp, row -> {
			global.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1));
		}, System.out::println);
		qp.clear();
		qp.put("fields", 
				  "Characteristic.Identifier"
				+ ",CharacteristicIdentifier.AlternativeIdentifier(S4HANA)"
			);
		qp.put("query", 
				"not CharacteristicIdentifier.AlternativeIdentifier(S4HANA) is empty"
			);
		qp.put("pageSize", "10000");
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> {
			suburbia.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1));
		}, System.out::println);
		qp.clear();
		qp.put("fields", 
				"Characteristic.Identifier"
						+ ",CharacteristicIdentifier.AlternativeIdentifier(ECC)"
				);
		qp.put("query", 
				"not CharacteristicIdentifier.AlternativeIdentifier(ECC) is empty"
				);
		qp.put("pageSize", "10000");
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> {
			liverpool.put(row.getJSONArray("values").getString(0), row.getJSONArray("values").getString(1));
		}, System.out::println);
		qp.clear();
		qp.put("fields", "StandardizationValue.Characteristic->Characteristic.Identifier");
		qp.put("dictionary", "CaracteristicasNoNecesariasEnCargaDePlantillas");
		rw.collectData("list", "StandardizationValue", null, "byDictionary", qp, row -> bannedCharacteristics.add(row.getJSONArray("values").getString(0)), System.out::println);
		System.out.println("Known attribute business: " + allowedBusiness.size());
		System.out.println("Known attribute global: " + global.size());
		System.out.println("Known attribute LVP: " + liverpool.size());
		System.out.println("Known attribute SBB: " + suburbia.size());
	}

	public static void main(String[] args) throws ParserConfigurationException, SAXException {
		javax.xml.parsers.SAXParserFactory factory = javax.xml.parsers.SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        } catch (Exception ignored) {}
		
		javax.xml.parsers.SAXParser parser = factory.newSAXParser();
		XMLHandlerJerarquiaProductosSTEP handler = new XMLHandlerJerarquiaProductosSTEP();
		try {
			parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "plantillas", "Plantillas_LDP_30sep2025.xml").toFile(), handler);
//			parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "plantillas", "step-3793808026665480449-exported.xml").toFile(), handler);
//			parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "plantillas", "step-6529882850129780316-exported (1).xml").toFile(), handler);
//			parser.parse(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "plantillas", "step-6091965653640836926-exported.xml").toFile(), handler);
			java.util.LinkedList<NodePPH> finished = handler.getFinished();
			finished.forEach(ReadTemplateMetadata::handlePPHToSend);
			requestPPHL4Templates.sendData();
			if(rows.length() > 0) {
				sendData();
			}
			requestTemplate.sendData();
			requestForParentAssignment.sendData();
			requestGroupFeature.sendData();
			if(templates.length() > 0) {
				System.out.println("Sending templates to PubSub...");
				new PubSubGCP().publishMessage(pubSubProject, "idmc_post_template", sa, new org.json.JSONObject().put("templates", templates).toString());
				new PubSubGCP().publishMessage(pubSubProject, "idmc_put_template", sa, new org.json.JSONObject().put("templates", templates).toString());
				while(templates.length() > 0) {
					templates.remove(0);
				}
			}
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void processGeneralStructureGroup(NodePPH node) {
		String structureGroupId = node.getId();
		requestTemplate.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + structureGroupId + "'@'PrimaryProductTaxonomy'")).put("values", new org.json.JSONArray().put(((node.getName() != null ? node.getName() : "") + " (" + structureGroupId + ")").trim())));
		requestPPHL4Templates.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + structureGroupId + "'@'PPH_L4_Templates'")).put("values", new org.json.JSONArray().put((node.getName() != null ? node.getName() : "").trim()).put(true)));
		if(node.getParentId() != null) {
			requestForParentAssignment.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + structureGroupId + "'@'PrimaryProductTaxonomy'")).put("values", new org.json.JSONArray().put(node.getParentId())));
		}
	}
	
	private static void processGeneralStructureGroupFeatures(NodePPH node) {
		java.util.LinkedList<ValuePPH> values = node.getValues();
		String attributeId = null;
		String structureGroupId = node.getId();
		for(ValuePPH v : values) {
			attributeId = v.getAttributeId();
			requestGroupFeature.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + structureGroupId + "'@'PrimaryProductTaxonomy'"))
					.put("qualification", new org.json.JSONObject()
							.put("identifier", "DEFAULT")
							.put("name", attributeId)
							.put("language", 10)
						)
					.put("values", new org.json.JSONArray().put(v.getText()))
				);
		}
	}
	
	public static void handlePPHToSend(NodePPH node) {
		processGeneralStructureGroup(node);
		processGeneralStructureGroupFeatures(node);
		
		java.util.LinkedList<AttributeLinkPPH> attributeLinks = node.getAttributeLinks();
		java.util.LinkedList<ValuePPH> values = node.getValues();
		java.util.LinkedList<MultiValuePPH> multiValues = node.getMultiValues();
		java.util.LinkedList<NodePPH> products = node.getProductos();
		java.util.LinkedList<ValuePPH> attributeLinkValues = null;
		java.util.LinkedList<ValuePPH> attributeLinkMetaDataValues = null;
		org.json.JSONArray keyWords = new org.json.JSONArray();
		String smoshImages = null;
		String illustrations = null;
		String attributeId = null;
		String business = null;
		String vendorCenterSection = null;
		String nameExceptions = null;
		String templateDescription = null;
		String nameGuide = null;
		String displayGroupOrder = null;
		String template = node.getId();
		ValuePPH sendToVendorCenter = null;
		ValuePPH relevantForATG = null;
		for(ValuePPH v : values) {
			attributeId = v.getAttributeId();
			if("NumberOfDetailImages".equals(attributeId)) {
			}else if("NumberOfIllustrationImages".equals(attributeId)) {
				illustrations = v.getText();
			}else if("NumberOfSmoshImages".equals(attributeId)) {
				smoshImages = v.getText();
			}else if("NameExceptions".equals(attributeId)) {
				nameExceptions = v.getText();
			}else if("DisplayGroupOrder".equals(attributeId)) {
				displayGroupOrder = v.getText();
			}else if("NameGuide".equals(attributeId)) {
				nameGuide = v.getText();
			}else if("TemplateDescription".equals(attributeId)) {
				templateDescription = v.getText();
			}
		}
		for(MultiValuePPH mv : multiValues) {
			if("KeyWords".equals(mv.getAttributeId())) {
				java.util.LinkedList<ValuePPH> vls = mv.getValues();
				for(ValuePPH v : vls) {
					keyWords.put(v.getText());
				}
			}else {
				System.out.println("WAS THIS: " + keyWords);
			}
		}
		if(smoshImages == null) {
			smoshImages = "0";
		}
		if(illustrations == null) {
			illustrations = "0";
		}
		if(template.startsWith("EU4-")) {
			templates.put(new org.json.JSONObject()
					.put("identifier", template)
					.put("name", node.getName() + " (" + node.getId()+ ")")
					.put("description", templateDescription == null || !templateDescription.isEmpty() ? "" : templateDescription)
					.put("nameExceptions", nameExceptions == null ? "" : nameExceptions)
					.put("nameGuide", nameGuide == null ? "" : nameGuide)
					.put("displayGroupOrder", displayGroupOrder != null ? displayGroupOrder : "")
					.put("products", new org.json.JSONArray())
					.put("keywords", keyWords)
					.put("itemsGroup", new org.json.JSONArray())
				);
			if(templates.length() == 100) {
				System.out.println("Sending template to pubSub");
				new PubSubGCP().publishMessage(pubSubProject, "idmc_post_template", sa, new org.json.JSONObject().put("templates", templates).toString());
				new PubSubGCP().publishMessage(pubSubProject, "idmc_put_template", sa, new org.json.JSONObject().put("templates", templates).toString());
				while(templates.length() > 0) {
					templates.remove(0);
				}
			}
			
			addImageRecords(template, "ProductImage", "1", "1");
			addImageRecords(template, "ProductImageDetail", "15", "1");
			addImageRecords(template, "ProductImageSmosh", smoshImages, "0");
			addImageRecords(template, "Illustration", illustrations, "0");
			java.util.LinkedList<String> filter = null;
			for(AttributeLinkPPH al : attributeLinks) {
				attributeId = al.getAttributeId();
				if(bannedCharacteristics.contains(attributeId)) {
					continue;
				}
				if(!global.containsKey(attributeId)) {
					attributeLinkValues = al.getFilterValues();
					if(!attributeLinkValues.isEmpty()) {
						filter = new java.util.LinkedList<>();
						for(ValuePPH v : attributeLinkValues) {
							filter.addLast(v.getId() != null ? v.getId() : v.getText());
						}
					}else {
						filter = null;
					}
					business = allowedBusiness.get(attributeId);
					if(business == null) {
						if(suburbia.containsKey(attributeId)) {
							business = "Suburbia";
						}else {
							if(liverpool.containsKey(attributeId)) {
								business = "Liverpool Marketplace";
							}
						}
					}
					vendorCenterSection = ReadTemplateMetadata.vendorCenterSection.get(attributeId);
					addValue(template, attributeId, "VendorCenterSection", vendorCenterSection == null ? "Atributos" : vendorCenterSection);
					addValue(template, attributeId, "IsMandatory", String.valueOf( al.isMandatory() ) );
					addValue(template, attributeId, "Business", business );
					attributeLinkMetaDataValues = al.getMetaDataValues();
					if(attributeLinkMetaDataValues != null) {
						for(ValuePPH vm : attributeLinkMetaDataValues) {
							if("AttMandatorySmartSheet".equals(vm.getAttributeId())) {
								sendToVendorCenter = vm;
							}else if("RelevantForATG".equals(vm.getAttributeId())) {
								relevantForATG = vm;
							}
						}
					}
					addValue(template, attributeId, "SentToVendorCenter", sendToVendorCenter == null ? "0" : "1" );
					addValue(template, attributeId, "RelevantForATG", relevantForATG == null ? "N" : relevantForATG.getId() );
					sendToVendorCenter = null;
					relevantForATG = null;
					if(filter != null && !filter.isEmpty()) {
						addValue(template, attributeId, "ListOfValuesFilter", rw.getRw().serializeChunk(filter.toArray(new String[] {}), "\"", ",", "\\"));
					}
				}
			}
			
		}
		for(NodePPH n : products) {
			handlePPHToSend(n);
		}
	}
	
	private static void addImageRecords(String template, String characteristic, String max, String min) {
		addValue(template, characteristic, "Business", "Liverpool Suburbia Marketplace");
		addValue(template, characteristic, "VariantLevel", "1");
		addValue(template, characteristic, "SentToVendorCenter", "1");
		addValue(template, characteristic, "VendorCenterSection", "Fotografías");
		addValue(template, characteristic, "Min", min);
		addValue(template, characteristic, "Max", max);
	}
	
	private static void addValue(String template, String attribute, String property, String value) {
		String key = String.join("<::>", new String[] { template, attribute, "CreateProposal", property });
		rows.put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + key + "'@'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'")).put("values", new org.json.JSONArray().put(template).put(attribute).put("CreateProposal").put(property).put(value)));
		if(rows.length() == bs) {
			sendData();
		}
	}
	
	private static void sendData() {
		org.json.JSONArray holder = new org.json.JSONArray();
		for(int i=0; i<rows.length(); i++) {
			holder.put(rows.get(i));
		}
		rw.writeData("list", "StandardizationValue", null, new java.util.TreeMap<>(), request, rr -> {
			try {
				java.util.Set<String> chars = new java.util.TreeSet<>();
				org.json.JSONObject r = new org.json.JSONObject(rr);
				if(r.getJSONObject("counters").getInt("errors") > 0){
					org.json.JSONArray entries = r.getJSONArray("entries");
					System.out.println("Following errors found:");
					for(int i=0; i<entries.length(); i++) {
						chars.add(holder.getJSONObject(entries.getJSONObject(i).getInt("row")).getJSONArray("values").getString(1));
						entries.getJSONObject(i).put("_content", holder.getJSONObject(entries.getJSONObject(i).getInt("row")));
					}
					System.out.println(r);
					RequestHandler enableCharacteristics = new RequestHandler(new org.json.JSONArray().put(new org.json.JSONObject().put("identifier", "Characteristic.IsActive")), 1000, request -> rw.writeData("list", "Characteristic", null, writeQP, request, System.out::println));
					for(String charId : chars) {
						enableCharacteristics.addRow(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + charId + "'")).put("values", new org.json.JSONArray().put(true)));
					}
					enableCharacteristics.sendData();
					sendData();
				}else {
					System.out.println(r.getJSONObject("counters"));
				}
			}catch(org.json.JSONException e) {
				e.printStackTrace();
			}
		});
	}
	
	public static void handleNodePPH(NodePPH node) {
		java.util.LinkedList<AttributeLinkPPH> attributeLinks = node.getAttributeLinks();
		java.util.LinkedList<MultiValuePPH> multiValues = node.getMultiValues();	
		java.util.LinkedList<ValuePPH> values = node.getValues();
		java.util.LinkedList<NodePPH> products = node.getProductos();
		java.util.LinkedList<ValuePPH> multiValueValues = null;
		java.util.LinkedList<ValuePPH> attributeLinkValues = null;
//		System.out.println("ID: " + node.getId() + ", UserTypeID: " + node.getUserTypeId() + ", Name: " + node.getName());
//		System.out.println("*** Values ***");
//		for(ValuePPH v : values) {
//			System.out.println("\t" + toStringValuePPH(v) );
//		}
//		System.out.println("*** MultiValues ***");
		for(MultiValuePPH mv : multiValues) {
//			System.out.println("\t" + (mv.getAttributeId()) );
			multiValueValues = mv.getValues();
//			for(ValuePPH v : multiValueValues) {
//				System.out.println("\t\t" + toStringValuePPH(v));
//			}
		}
//		System.out.println("*** AttributeLinks ***");
		for(AttributeLinkPPH al : attributeLinks) {
//			System.out.println("\t" + al.getAttributeId());
			attributeLinkValues = al.getFilterValues();
			if(!attributeLinkValues.isEmpty()) {
//				System.out.println("\tFILTER:");
				for(ValuePPH v : attributeLinkValues) {
//					System.out.println("\t\t" + toStringValuePPH(v));
				}
			}
		}
		for(NodePPH n : products) {
			handleNodePPH(n);
		}
	}
	
	private static String toStringValuePPH(ValuePPH v) {
		return (v.getAttributeId() != null ? "AttributeID: " + v.getAttributeId() + ", " : "") + ( v.getId() != null ? "ID: " + v.getId() + ", " : "" ) + (v.getText() != null ? "Text: " + v.getText() : "") ;
	}
}
