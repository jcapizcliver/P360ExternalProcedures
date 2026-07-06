package mx.com.liverpool.p360.services.core.temp.move.utils;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class GeneralOperations {

	
	public org.json.JSONObject adjustData(org.json.JSONArray values, org.json.JSONArray columns){
		org.json.JSONArray nv = new org.json.JSONArray();
		org.json.JSONArray nc = new org.json.JSONArray();
		for(int i=0; i<values.length() ; i++) {
			nv.put(values.get(i));
			nc.put(columns.get(i));
		}
		for(int i=values.length(); i<values.length(); i++) {
			if(!"".equals(values.getString(i))) {
				nv.put(values.get(i));
				nc.put(columns.get(i));
			}
		}
		return new org.json.JSONObject().put("columns", nc).put("rows", new org.json.JSONArray().put(new org.json.JSONObject().put("object", new org.json.JSONObject().put("id", "'" + values.getString(0) + "'")).put("values", nv)));
	}
	
	private boolean compareArrayObjects(Object o1, Object o2) {
		if(o1 == null || o2 == null)
			return false;
		return o1 instanceof org.json.JSONArray && o2 instanceof org.json.JSONArray ? jsonArrayEquals((org.json.JSONArray)o1,(org.json.JSONArray)o2) : o1.equals(o2);
	}
	
	private boolean jsonArrayEquals(org.json.JSONArray a1, org.json.JSONArray a2) {
		if(a1 == null || a2 == null || a1.length() != a2.length())
			return false;
		for(int i=0; i<a1.length(); i++) {
			if(!a1.get(i).equals(a2.get(i)))
				return false;
		}
		return true;
	}
	
	public void chooseToApply(org.json.JSONArray values, org.json.JSONArray values2, org.json.JSONArray columns, RESTWorkshop rw, OnDifference od) {
		boolean good = true;
		for(int i=0; i<values.length(); i++) {
			if(!compareArrayObjects(values.get(i), values2.get(i))) {
				System.out.println("Not equal: " + values.get(i) + " vs " + values2.get(i));
				od.doResolve(columns, values, rw);
				good = false;
			}else {
			}
		}
		if(good)
			System.out.println("We are good");
		else
			System.out.println("No good");
	}
	
	public java.util.Map<String, String[]> collectTemplatesFromStructureGroups(RESTWorkshop rw, String structureSystem){
		java.util.Map<String, String[]> data = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		org.json.JSONObject object = null;
		int a = 0;
		int b = 0;
		qp.put("structure", structureSystem);
		qp.put("pageSize", "600");
		qp.put("fields", 
				  "StructureGroup.Identifier,StructureGroupLang.Name(es)"
			);
		qp.put("query", "StructureGroup.Identifier wildcard \"EU4-%\"");
		do {
			qp.put("startIndex", String.valueOf(a));
			response = rw.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
			if(response != null && response.has("totalSize")) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					object = rows.getJSONObject(i).getJSONObject("object");
					data.put(object.getString("id"), new String[] { values.getString(0), values.getString(1) });
				}
				a += response.getInt("pageSize");
			}else {
				System.out.println(rw.getRawResponse());
			}
		}while(a < b);
		a = 0;
		return data;
	}
	
	public java.util.Map<String, org.json.JSONObject> collectStructureGroupAttributes(RESTWorkshop rw, String structureSystem){
		java.util.Map<String, org.json.JSONObject> data = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int a = 0;
		int b = 0;
		qp.put("structure", structureSystem);
		qp.put("pageSize", "600");
		qp.put("fields", 
				  "StructureGroupAttribute.StructureAttribute"
				+ ",StructureGroupAttribute.Datatype"
				+ ",StructureGroupAttributeLang.Name(es)"
				+ ",StructureGroupAttributeValue.Value(es,-1)"
			);
		qp.put("qualificationFilter", "name(NameGuide,NameExceptions,OrderOfAtributesForName),language(10)");
		java.util.Map<String, String[]> dictionary = collectTemplatesFromStructureGroups(rw, structureSystem);
		org.json.JSONObject object = null;
		org.json.JSONObject content = null;
		String[] sgData = null;
		do {
			qp.put("startIndex", String.valueOf(a));
			response = rw.makeRequest("GET", "/list/StructureGroup/StructureGroupAttribute/byStructure", qp, null);
			if(response != null && response.has("totalSize")) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					object = rows.getJSONObject(i).getJSONObject("object");
//					System.out.println(rows.getJSONObject(i).getJSONObject("qualification") + "\t\t\t\t" + values);
					sgData = dictionary.get(object.getString("id"));
					if(sgData == null) {
						// Not an EU4- id
					}else {
						content = data.get(sgData[0]);
						if(content == null) {
							content = new org.json.JSONObject();
							data.put(sgData[0], content);
							content.put("_templateNameEs", sgData[1]);
						}
						content.put(values.getString(2), values.getString(3));
					}
				}
				a += response.getInt("pageSize");
			}else {
				System.out.println(rw.getRawResponse());
			}
		}while(a < b);
		a = 0;
		return data;
	}
	
	public java.util.Map<String, org.json.JSONArray> collectCharacteristic(String baseUrl) {
		java.util.Map<String, org.json.JSONArray> data = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
		if(baseUrl != null)
			rw.setBaseUrl(baseUrl);
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		qp.put("query", "Characteristic.Identifier wildcard \"ProductImageDetail_%\"");
//		qp.put("query", "Characteristic.Identifier wildcard \"ProductImageDetail_%\"");
//		qp.put("query", "Characteristic.ParentCharacteristic is empty");
		qp.put("fields", 
				  "Characteristic.Identifier"
			    + ",Characteristic.Category->LookupValue.Code"
			    + ",Characteristic.Purposes->LookupValue.Code"
			    + ",Characteristic.Entities"
			    + ",Characteristic.Lookup->Lookup.Identifier"
			    + ",Characteristic.Order"
			    + ",Characteristic.ParentCharacteristic->Characteristic.Identifier"
			    + ",Characteristic.DataType"
			    + ",Characteristic.IsActive"
				+ ",Characteristic.LowerBound"
				+ ",Characteristic.UpperBound"
				+ ",Characteristic.IsMultiValue"
				+ ",Characteristic.IsMultiLine"
				+ ",Characteristic.IsLanguageSpecific"
				+ ",CharacteristicLang.Name(es)"
				+ ",CharacteristicLang.Name(en)"
				+ ",CharacteristicLang.Description(es)"
				+ ",CharacteristicLang.Description(en)"
				+ ",CharacteristicLang.DefaultValue(es)"
				+ ",CharacteristicLang.DefaultValue(en)"
//				+ ",CharacteristicIdentifier.AlternativeIdentifier(ECC)"
//				+ ",CharacteristicIdentifier.AlternativeIdentifier(S4HANA)"
//				+ ",CharacteristicIdentifier.AlternativeIdentifier(ATG)"
			);
		qp.put("pageSize", "1200");
		System.out.println("Entering collection...");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					data.put(values.getString(0), values);
				}
			}else {
				System.out.println(rw.getRawResponse());
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}
	
	public java.util.Map<String, org.json.JSONArray> collectActiveCharacteristic(RESTWorkshop rw) {
		java.util.Map<String, org.json.JSONArray> data = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		qp.put("query", "Characteristic.IsActive = true and Characteristic.ParentCharacteristic is empty");
		qp.put("fields", 
				  "Characteristic.Identifier"
			    + ",Characteristic.Category->LookupValue.Code"
			    + ",Characteristic.Purposes->LookupValue.Code"
			    + ",Characteristic.Entities"
			    + ",Characteristic.Lookup->Lookup.Identifier"
			    + ",Characteristic.Order"
			    + ",Characteristic.ParentCharacteristic->Characteristic.Identifier"
			    + ",Characteristic.DataType"
			    + ",Characteristic.IsActive"
				+ ",Characteristic.LowerBound"
				+ ",Characteristic.UpperBound"
				+ ",Characteristic.IsMultiValue"
				+ ",Characteristic.IsMultiLine"
				+ ",Characteristic.IsLanguageSpecific"
				+ ",CharacteristicLang.Name(es)"
				+ ",CharacteristicLang.Name(en)"
				+ ",CharacteristicLang.Description(es)"
				+ ",CharacteristicLang.Description(en)"
				+ ",CharacteristicLang.DefaultValue(es)"
				+ ",CharacteristicLang.DefaultValue(en)"
			);
		qp.put("pageSize", "1200");
		System.out.println("Entering collection...");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					data.put(values.getString(0), values);
				}
			}else {
				System.out.println(rw.getRawResponse());
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}
	
	public java.util.Map<String, org.json.JSONArray> collectCharacteristic(RESTWorkshop rw) {
		java.util.Map<String, org.json.JSONArray> data = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
//		qp.put("query", "Characteristic.Identifier wildcard \"%_ProductImageDetail\"");
//		qp.put("query", "not Characteristic.ParentCharacteristic is empty");
//		qp.put("query", "Characteristic.Identifier = \"msj_AE358Att\"");
		qp.put("query", 
				"Characteristic.Identifier wildcard \"mdr_%\""
				+ "or Characteristic.Identifier wildcard \"msj_%\""
				+ "or Characteristic.Identifier wildcard \"rem_%\""
				+ "or Characteristic.Identifier wildcard \"rma_%\""
				+ "or Characteristic.Identifier wildcard \"rmum_%\""
				+ "or Characteristic.Identifier wildcard \"rrd_%\""
				+ "or Characteristic.Identifier wildcard \"rre_%\""
				);
		qp.put("fields", 
				  "Characteristic.Identifier"
			    + ",Characteristic.Category->LookupValue.Code"
			    + ",Characteristic.Purposes->LookupValue.Code"
			    + ",Characteristic.Entities"
			    + ",Characteristic.Lookup->Lookup.Identifier"
			    + ",Characteristic.Order"
			    + ",Characteristic.ParentCharacteristic->Characteristic.Identifier"
			    + ",Characteristic.DataType"
			    + ",Characteristic.IsActive"
				+ ",Characteristic.LowerBound"
				+ ",Characteristic.UpperBound"
				+ ",Characteristic.IsMultiValue"
				+ ",Characteristic.IsMultiLine"
				+ ",Characteristic.IsLanguageSpecific"
				+ ",CharacteristicLang.Name(es)"
				+ ",CharacteristicLang.Name(en)"
				+ ",CharacteristicLang.Description(es)"
				+ ",CharacteristicLang.Description(en)"
				+ ",CharacteristicLang.DefaultValue(es)"
				+ ",CharacteristicLang.DefaultValue(en)"
//				+ ",CharacteristicIdentifier.AlternativeIdentifier(ECC)"
//				+ ",CharacteristicIdentifier.AlternativeIdentifier(S4HANA)"
//				+ ",CharacteristicIdentifier.AlternativeIdentifier(ATG)"
			);
		qp.put("pageSize", "1200");
		System.out.println("Entering collection...");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					data.put(values.getString(0), values);
				}
			}else {
				System.out.println(rw.getRawResponse());
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}
	
	public java.util.Map<String, org.json.JSONArray> gatherDictionaryData(RESTWorkshop rw, String dictionary){
//		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, org.json.JSONArray> data = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		
//		if(baseUrl != null) {
//			rw.setBaseUrl(baseUrl);
//		}
		qp.put("dictionary", dictionary);
		qp.put("fields", 
				  "StandardizationValue.Value"
				+ ",StandardizationValue.AlternativeValue"
				+ ",StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
			);
		qp.put("pageSize", "1200");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					data.put(values.getString(0), values);
				}
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}
	
	public java.util.Map<String, String> loadTemplateCharProperties(RESTWorkshop rw, String dictionary){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		
		qp.put("dictionary", dictionary);
		qp.put("fields", 
				  "StandardizationValue.Value"
				+ ",StandardizationValue.AlternativeValue"
				+ ",StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
			);
		qp.put("pageSize", "1200");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					data.put(values.getString(2) + "<::>" + values.getString(3) + "<::>" + values.getString(5), values.getString(0));
				}
				currentIndex+=response.getInt("pageSize");
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
			System.out.println(currentIndex + "/" + totalSize);
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}
	
	public java.util.Map<String, org.json.JSONArray> gatherTemplateMetaData(RESTWorkshop rw, String dictionary, String templateId){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, org.json.JSONArray> data = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		
		qp.put("dictionaryProxy", "'" + dictionary + "'");
		qp.put("fields", 
				  "StandardizationValue.Value"
				+ ",StandardizationValue.AlternativeValue"
				+ ",StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
			);
		qp.put("query", "StandardizationValue.Dictionary->StandardizationDictionary.Identifier equals \"" + dictionary + "\" and StandardizationValue.StructureGroup->LookupValue.Code equals \"" + templateId + "\"");
		qp.put("pageSize", "1200");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					data.put(values.getString(0), values);
				}
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}
	
	public java.util.Map<String, org.json.JSONObject> parseToProperties(java.util.Map<String, org.json.JSONArray> data){
		String prev = null;
		java.util.Map<String, org.json.JSONObject> nd = new java.util.TreeMap<>();
		org.json.JSONObject content = new org.json.JSONObject();
		java.util.LinkedList<java.util.Map.Entry<String, org.json.JSONArray>> holder = new java.util.LinkedList<>(data.entrySet());
		java.util.Collections.sort( holder, (e1,e2)-> e1.getValue().getString(3).compareTo(e2.getValue().getString(3)));
		for(java.util.Map.Entry<String, org.json.JSONArray> entry : holder) {
			if(prev != null && !prev.equals(entry.getValue().getString(3))) {
				nd.put(prev, content);
				content = new org.json.JSONObject();
			}
			content.put(entry.getValue().getString(5), entry.getValue().getString(6));
			prev = entry.getValue().getString(3);
		}
		if(prev != null)
			nd.put(prev, content);
		content = null;
		return nd;
	}
	
	public java.util.Map<String, String> collectCharacteristicAlternativeIdentifier(RESTWorkshop rw, String externalSystem){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		qp.put("fields", 
				  "Characteristic.Identifier"
				+ ",CharacteristicIdentifier.AlternativeIdentifier(" + externalSystem + ")"
			);
		qp.put("query", "not CharacteristicIdentifier.AlternativeIdentifier(" + externalSystem + ") is empty");
		qp.put("pageSize", "1200");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					data.put(values.getString(0), values.getString(1));
				}
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}
	
	public java.util.Set<String> listActiveBaseCharacteristics(RESTWorkshop rw){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Set<String> data = new java.util.TreeSet<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		qp.put("fields", 
				  "Characteristic.Identifier"
			);
		qp.put("query", "Characteristic.RootCharacteristic is empty and not Characteristic.Identifier wildcard \"Rechazo\" and not Characteristic.Identifier wildcard \"Rejection\"");
		qp.put("pageSize", "1200");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Characteristic/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					data.add(values.getString(0));
				}
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}
	
	public java.util.Set<String> collectDistinctCharacteristicInDictionary(RESTWorkshop rw, String dictionary, String query){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Set<String> data = new java.util.TreeSet<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		qp.put("dictionaryProxy", "'" + dictionary + "'");
		qp.put("fields", 
				"StandardizationValue.Characteristic->Characteristic.Identifier"
			);
		qp.put("query", query);
		qp.put("pageSize", "1200");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					data.add(values.getString(0));
				}
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}
	
	public java.util.Map<String, org.json.JSONObject> collectAttributeDefinitions(RESTWorkshop rw, String dictionary){
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		java.util.Map<String, org.json.JSONObject> data = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		String prev = null;
		org.json.JSONObject content = new org.json.JSONObject();
		int currentIndex = 0;
		int totalSize = 0;
		qp.put("dictionary", dictionary);
		qp.put("fields", 
				  "StandardizationValue.Value"
				+ ",StandardizationValue.AlternativeValue"
				+ ",StandardizationValue.StructureGroup->LookupValue.Code"
				+ ",StandardizationValue.Characteristic->Characteristic.Identifier"
				+ ",StandardizationValue.CreationType->LookupValue.Code"
				+ ",StandardizationValue.Property->LookupValue.Code"
				+ ",StandardizationValue.PropertyValue"
			);
		qp.put("orderBy", "3-ASC");
		qp.put("pageSize", "1200");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if(prev != null && !prev.equals(values.getString(3))) {
						data.put(prev, content);
						content = new org.json.JSONObject();
					}
					content.put(values.getString(5), values.getString(6));
					prev = values.getString(3);
				}
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		data.put(prev, content);
		content = null;
		return data;
	}
	
//	public java.util.LinkedList<org.json.JSONArray> queryLookupBySearch(String baseUrl, String lookup, String fields, String query){
//		RESTWorkshop rw = new RESTWorkshop();
//		java.util.Map<String, String> qp = new java.util.TreeMap<>();
//		java.util.Map<String, org.json.JSONObject> data = new java.util.TreeMap<>();
//		org.json.JSONObject response = null;
//		org.json.JSONArray rows = null;
//		org.json.JSONArray values = null;
//		String prev = null;
//		org.json.JSONObject content = new org.json.JSONObject();
//		int currentIndex = 0;
//		int totalSize = 0;
//		if(baseUrl != null) {
//			rw.setBaseUrl(baseUrl);
//		}
//		qp.put("lookup", lookup);
//		qp.put("fields", fields);
//		qp.put("query", query);
//		do {
//			qp.put("startIndex", String.valueOf(currentIndex));
//			response = rw.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
//			if(response != null) {
//				totalSize = response.getInt("totalSize");
//				rows = response.getJSONArray("rows");
//				for(int i=0; i<rows.length(); i++) {
//					currentIndex++;
//					values = rows.getJSONObject(i).getJSONArray("values");
//					if(prev != null && !prev.equals(values.getString(3))) {
//						data.put(prev, content);
//						content = new org.json.JSONObject();
//					}
//					content.put(values.getString(5), values.getString(6));
//					prev = values.getString(3);
//				}
//			}else {
//				System.out.println("ERROR: " + rw.getRawResponse());
//			}
//		}while(currentIndex < totalSize);
//		currentIndex = 0;
//		data.put(prev, content);
//		content = null;
//		return data;
//	}
	
	public java.util.Map<String, org.json.JSONArray> collectProductData(RESTWorkshop rw, String fields, String query){
		java.util.Map<String, org.json.JSONArray> data = new java.util.TreeMap<>();
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int currentIndex = 0;
		int totalSize = 0;
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", fields);
		qp.put("query", query);
		qp.put("pageSize", "1200");
		
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
			if(response != null) {
				rows = response.getJSONArray("rows");
				totalSize = response.getInt("totalSize");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					data.put(values.getString(0), values);
				}
			} else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}
	
	public java.util.Map<String, String> collectLookupValueData(String baseUrl, String lookup){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		RESTWorkshop rw = new RESTWorkshop();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null; 
		org.json.JSONArray values = null;
		qp.put("lookup", lookup);
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("query", "LookupValue.IsActive = true");
		qp.put("pageSize", "650");
		if(baseUrl != null) {
			rw.setBaseUrl(baseUrl);
		}
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					data.put(values.getString(0), values.getString(1));
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}
	
	public java.util.Map<String, String> collectLookupValueData(RESTWorkshop rw, String lookup){
		java.util.Map<String, String> data = new java.util.TreeMap<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null; 
		org.json.JSONArray values = null;
		qp.put("lookup", lookup);
		qp.put("fields", "LookupValue.Code,LookupValueLang.Name(es)");
		qp.put("query", "LookupValue.IsActive = true");
		qp.put("pageSize", "650");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/LookupValue/bySearch", qp, null);
			if(response != null) {
//				System.out.println(response);
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					data.put(values.getString(0), values.getString(1));
				}
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return data;
	}
}

