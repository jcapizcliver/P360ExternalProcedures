package mx.com.liverpool.p360.services.core.sftp;

/** Shared, in-memory reconciliation. This class never calls P360 or deletes data.
 * Legacy entry points retain the existing parser semantics; planning is separate.
 */
public final class ProductDataReconciler {
    private ProductDataReconciler() { }
	public static void mergeObjectMissing(org.json.JSONObject target, org.json.JSONObject source, java.util.Set<String> excludedKeys) {
	    for(Object keyObject : source.keySet()) {
	        String key = String.valueOf(keyObject);

	        if(excludedKeys != null && excludedKeys.contains(key)) {
	            continue;
	        }

	        Object sourceValue = source.opt(key);

	        if(isEmptyJsonValue(sourceValue)) {
	            continue;
	        }

	        Object targetValue = target.opt(key);

	        if(isEmptyJsonValue(targetValue)) {
	            target.put(key, cloneJsonValue(sourceValue));
	            continue;
	        }

	        if(sourceValue instanceof org.json.JSONObject && targetValue instanceof org.json.JSONObject) {
	            mergeObjectMissing((org.json.JSONObject) targetValue, (org.json.JSONObject) sourceValue, null);
	            continue;
	        }

	        if(sourceValue instanceof org.json.JSONArray && targetValue instanceof org.json.JSONArray) {
	            mergeArrayMissing(key, (org.json.JSONArray) targetValue, (org.json.JSONArray) sourceValue);
	        }
	    }
	}

	public static void mergeArrayMissing(String sectionName, org.json.JSONArray targetArray, org.json.JSONArray sourceArray) {
	    java.util.Map<String, org.json.JSONObject> targetByKey = new java.util.LinkedHashMap<>();

	    for(int i = 0; i < targetArray.length(); i++) {
	        Object value = targetArray.opt(i);

	        if(value instanceof org.json.JSONObject) {
	            org.json.JSONObject object = (org.json.JSONObject) value;
	            targetByKey.put(buildArrayItemKey(sectionName, object), object);
	        }
	    }

	    for(int i = 0; i < sourceArray.length(); i++) {
	        Object sourceValue = sourceArray.opt(i);

	        if(!(sourceValue instanceof org.json.JSONObject)) {
	            if(!arrayContainsEquivalentValue(targetArray, sourceValue)) {
	                targetArray.put(cloneJsonValue(sourceValue));
	            }
	            continue;
	        }

	        org.json.JSONObject sourceObject = (org.json.JSONObject) sourceValue;
	        String sourceKey = buildArrayItemKey(sectionName, sourceObject);
	        org.json.JSONObject targetObject = targetByKey.get(sourceKey);

	        if(targetObject == null) {
	            targetArray.put(new org.json.JSONObject(sourceObject.toString()));
	        } else {
	            mergeObjectMissing(targetObject, sourceObject, null);
	        }
	    }
	}

	public static String buildArrayItemKey(String sectionName, org.json.JSONObject object) {
	    if("lang".equals(sectionName)) {
	        return "lang|" + nestedValue(object, "_qualification.language._key");
	    }

	    if("structureGroupMap".equals(sectionName)) {
	        return "structureGroupMap|" + objectKey(object.optJSONObject("_qualification"), "structureGroup");
	    }

	    if("attribute".equals(sectionName)) {
	        String identifier = object.optString("identifier", "");
	        if(!isBlank(identifier)) {
	            return "attribute|" + identifier;
	        }

	        return "attribute|" + nestedValue(object, "_qualification.nameInKeyLang");
	    }

	    if("_characteristicRecords".equals(sectionName)) {
	        String characteristic = objectKey(object.optJSONObject("_qualification"), "characteristic");
	        String recordKey = nestedValue(object, "_qualification.recordKey");
	        String parentRecordKey = nestedValue(object, "_qualification.parentRecordKey");
	        return "_characteristicRecords|" + characteristic + "|" + recordKey + "|" + parentRecordKey;
	    }

	    if("productExtraData".equals(sectionName)) {
	        return "productExtraData|" + objectKey(object.optJSONObject("_qualification"), "targetMarket");
	    }

	    if("value".equals(sectionName)) {
	        String lang = nestedValue(object, "_qualification.language._key");
	        String identifier = nestedValue(object, "_qualification.identifier");
	        return "value|" + lang + "|" + identifier;
	    }

	    if("_recordLang".equals(sectionName)) {
	        return "_recordLang|" + nestedValue(object, "_qualification.language._key");
	    }

	    return sectionName + "|" + object.toString();
	}

	public static String objectKey(org.json.JSONObject parent, String childName) {
	    if(parent == null) {
	        return "";
	    }

	    org.json.JSONObject child = parent.optJSONObject(childName);

	    if(child == null) {
	        return "";
	    }

	    org.json.JSONObject key = child.optJSONObject("_key");

	    if(key != null) {
	        String externalId = key.optString("_externalId", "");
	        String internalId = key.optString("_internalId", "");
	        String entityId = String.valueOf(key.opt("_entityId"));
	        return firstNotBlank(externalId, internalId, entityId);
	    }

	    String externalId = child.optString("_externalId", "");
	    String internalId = child.optString("_internalId", "");
	    String code = child.optString("_code", "");
	    String keyValue = String.valueOf(child.opt("_key"));

	    return firstNotBlank(externalId, internalId, code, keyValue);
	}

	public static String nestedValue(org.json.JSONObject object, String path) {
	    if(object == null || isBlank(path)) {
	        return "";
	    }

	    String[] parts = path.split("\\.");
	    Object current = object;

	    for(String part : parts) {
	        if(!(current instanceof org.json.JSONObject)) {
	            return "";
	        }

	        current = ((org.json.JSONObject) current).opt(part);

	        if(current == null || current == org.json.JSONObject.NULL) {
	            return "";
	        }
	    }

	    return String.valueOf(current);
	}

	public static boolean arrayContainsEquivalentValue(org.json.JSONArray array, Object value) {
	    String valueString = String.valueOf(value);

	    for(int i = 0; i < array.length(); i++) {
	        Object current = array.opt(i);

	        if(String.valueOf(current).equals(valueString)) {
	            return true;
	        }
	    }

	    return false;
	}

	public static Object cloneJsonValue(Object value) {
	    if(value instanceof org.json.JSONObject) {
	        return new org.json.JSONObject(((org.json.JSONObject) value).toString());
	    }

	    if(value instanceof org.json.JSONArray) {
	        return new org.json.JSONArray(((org.json.JSONArray) value).toString());
	    }

	    return value;
	}

	public static boolean isEmptyJsonValue(Object value) {
	    if(value == null || value == org.json.JSONObject.NULL) {
	        return true;
	    }

	    if(value instanceof String) {
	        return ((String) value).trim().isEmpty();
	    }

	    if(value instanceof org.json.JSONArray) {
	        return ((org.json.JSONArray) value).length() == 0;
	    }

	    if(value instanceof org.json.JSONObject) {
	        return ((org.json.JSONObject) value).length() == 0;
	    }

	    return false;
	}

	public static boolean isBlank(String value) {
	    return value == null || value.trim().isEmpty();
	}

	public static String firstNotBlank(String... values) {
	    if(values == null) {
	        return "";
	    }

	    for(String value : values) {
	        if(!isBlank(value) && !"null".equalsIgnoreCase(value)) {
	            return value;
	        }
	    }

	    return "";
	}

}
