package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.PropertiesManager;
import mx.com.liverpool.p360.services.core.RESTWrapper;

/**
 * Writes Product2G user remarks using the same complex characteristic shape
 * consumed by CreateProposal and GetProposals.
 */
public class Product2GUserRemarksUpdater {

    private static final String CHARACTERISTIC = "Comentario";
    private static final String CONFIG_SUBMITTING_ROLE =
            "p360.contingency.placeholder_status.user_remarks.submitting_role";
    private static final String CONFIG_TARGET_ROLE =
            "p360.contingency.placeholder_status.user_remarks.target_role";
    private static final String CONFIG_STATUS =
            "p360.contingency.placeholder_status.user_remarks.status";
    private static final String CONFIG_ACTION =
            "p360.contingency.placeholder_status.user_remarks.action";

    private final RESTWrapper rw;
    private final Logger logger;

    public Product2GUserRemarksUpdater(RESTWrapper rw, Logger logger) {
        this.rw = rw;
        this.logger = logger;
    }

    public void updateRemarks(java.util.List<PlaceholderStatusUpdate> updates) {
        if (updates == null || updates.isEmpty()) {
            return;
        }
        for (PlaceholderStatusUpdate update : updates) {
            if (!update.hasComment()) {
                continue;
            }
            updateRemark(update);
        }
    }

    private void updateRemark(PlaceholderStatusUpdate update) {
        JSONObject product = getProduct2GObject(update.getPlaceholderId());
        String internalId = product.getJSONObject("_entityItem").getString("_internalId");
        JSONArray characteristicRecords = getCharacteristicRecords(product);
        JSONArray recordsToUpdate = new JSONArray();
        Map<String, LinkedList<JSONObject>> recordsByCharacteristic = toCharacteristicMap(characteristicRecords);

        LinkedList<JSONObject> existingComments = recordsByCharacteristic.get(CHARACTERISTIC);
        int max = -1;
        if (existingComments != null) {
            for (JSONObject existingComment : existingComments) {
                deactivateRecord(existingComment, recordsToUpdate);
            }
            max = getMaxRecordKey(existingComments);
        }

        addUserRemark(buildUserRemark(update), max + 1, recordsToUpdate);
        JSONObject request = new JSONObject().put("_characteristicRecords", recordsToUpdate);
        logger.info("Updating Product2G user remark. placeholderId=" + update.getPlaceholderId()
                + ", internalId=" + internalId);
        JSONObject response = rw.getRw().makeRequest("PUT",
                "/object/Product2G/" + internalId + "?includeLabels=true", null, request.toString());
        String rawResponse = rw.getRw().getRawResponse();
        if (rawResponse == null || rawResponse.trim().length() == 0) {
            throw new IllegalStateException("Empty response from Product2G user remark update");
        }
        if (response != null && (response.has("error") || response.has("Error"))) {
            throw new IllegalStateException(rawResponse);
        }
    }

    private JSONObject getProduct2GObject(String placeholderId) {
        String objectId = "'" + placeholderId.replace("'", "") + "'@'MASTER'";
        String path = "/object/Product2G/" + objectId
                + "?entityFilter=Product2G,Product2GStructureGroupMap,Product2GCharacteristicValue"
                + "&includeLabels=true&includeIds=true";
        JSONObject response = rw.getRw().makeRequest("GET", path, new TreeMap<String, String>(), null);
        String rawResponse = rw.getRw().getRawResponse();
        if (rawResponse == null || rawResponse.trim().length() == 0) {
            throw new IllegalStateException("Empty response from Product2G object read");
        }
        if (response == null || response.has("error") || response.has("Error")) {
            throw new IllegalStateException(rawResponse);
        }
        return response;
    }

    private static JSONArray getCharacteristicRecords(JSONObject product) {
        JSONObject data = product.getJSONObject("_data");
        if (!data.has("_characteristicRecords")) {
            return new JSONArray();
        }
        return data.getJSONArray("_characteristicRecords");
    }

    private static Map<String, LinkedList<JSONObject>> toCharacteristicMap(JSONArray characteristicRecords) {
        Map<String, LinkedList<JSONObject>> recordsByCharacteristic = new TreeMap<>();
        for (int i = 0; i < characteristicRecords.length(); i++) {
            JSONObject record = characteristicRecords.getJSONObject(i);
            removeLookupValues(record);
            String code = record.getJSONObject("_qualification")
                    .getJSONObject("characteristic").getString("_code");
            LinkedList<JSONObject> records = recordsByCharacteristic.get(code);
            if (records == null) {
                records = new LinkedList<>();
                recordsByCharacteristic.put(code, records);
            }
            records.add(record);
        }
        return recordsByCharacteristic;
    }

    private static void removeLookupValues(JSONObject record) {
        record.remove("lookupValue");
        if (!record.has("_children")) {
            return;
        }
        JSONArray children = record.getJSONArray("_children");
        for (int i = 0; i < children.length(); i++) {
            removeLookupValues(children.getJSONObject(i));
        }
    }

    private static int getMaxRecordKey(LinkedList<JSONObject> records) {
        int max = -1;
        for (JSONObject record : records) {
            String recordKey = record.getJSONObject("_qualification").getString("recordKey");
            String[] parts = recordKey.split("\\.");
            int firstPart = Integer.parseInt(parts[0]);
            int value = firstPart == 0 ? Integer.parseInt(parts[1]) : firstPart;
            max = max > value ? max : value;
        }
        return max;
    }

    private static void deactivateRecord(JSONObject record, JSONArray recordsToUpdate) {
        boolean changed = false;
        if (record.has("_children")) {
            JSONArray children = record.getJSONArray("_children");
            for (int i = 0; i < children.length(); i++) {
                JSONObject child = children.getJSONObject(i);
                String code = child.getJSONObject("_qualification")
                        .getJSONObject("characteristic").getString("_code");
                if (("rem_" + CHARACTERISTIC).equals(code)) {
                    JSONObject recordLang = getUnlocalized(child.getJSONArray("_recordLang"));
                    if (recordLang != null) {
                        JSONObject value = recordLang.getJSONArray("values").getJSONObject(0);
                        value.remove("_key");
                        if (!"CS02".equals(value.optString("_code"))) {
                            value.put("_code", "CS02");
                            changed = true;
                        }
                    }
                }
            }
        }
        if (changed) {
            recordsToUpdate.put(record);
        }
    }

    private static JSONObject getUnlocalized(JSONArray recordLang) {
        for (int i = 0; i < recordLang.length(); i++) {
            JSONObject language = recordLang.getJSONObject(i)
                    .getJSONObject("_qualification").getJSONObject("language");
            if ("zxx".equals(language.getString("_code"))) {
                return recordLang.getJSONObject(i);
            }
        }
        return null;
    }

    private static void addUserRemark(JSONObject userRemark, int index, JSONArray characteristicArray) {
        String recordKey = index == 0 ? "0000.0000.RK" : "0000." + pad4(index) + ".RK";
        JSONArray children = new JSONArray();
        children.put(textChild(recordKey, "msj_" + CHARACTERISTIC, userRemark.getString("comment")));
        children.put(textChild(recordKey, "rmum_" + CHARACTERISTIC, userRemark.getString("date")));
        children.put(labelChild(recordKey, "rre_" + CHARACTERISTIC, userRemark.getString("submittingRole")));
        if (userRemark.has("action")) {
            children.put(codeChild(recordKey, "rma_" + CHARACTERISTIC, userRemark.getString("action")));
        }
        children.put(labelChild(recordKey, "rrd_" + CHARACTERISTIC, userRemark.getString("targetRole")));
        children.put(codeOrLabelChild(recordKey, "rem_" + CHARACTERISTIC, userRemark.getString("status")));
        characteristicArray.put(new JSONObject()
                .put("_qualification", qualification(recordKey, CHARACTERISTIC))
                .put("_recordLang", new JSONArray().put(new JSONObject().put("values", new JSONArray())))
                .put("_children", children));
    }

    private static JSONObject textChild(String recordKey, String characteristic, String value) {
        return new JSONObject()
                .put("_qualification", qualification(recordKey, characteristic))
                .put("_recordLang", new JSONArray().put(new JSONObject()
                        .put("values", new JSONArray().put(value))));
    }

    private static JSONObject labelChild(String recordKey, String characteristic, String label) {
        return new JSONObject()
                .put("_qualification", qualification(recordKey, characteristic))
                .put("_recordLang", new JSONArray().put(new JSONObject()
                        .put("values", new JSONArray().put(localizedValue("_label", label)))));
    }

    private static JSONObject codeChild(String recordKey, String characteristic, String code) {
        return new JSONObject()
                .put("_qualification", qualification(recordKey, characteristic))
                .put("_recordLang", new JSONArray().put(new JSONObject()
                        .put("values", new JSONArray().put(localizedValue("_code", code)))));
    }

    private static JSONObject codeOrLabelChild(String recordKey, String characteristic, String value) {
        String key = value != null && value.matches("[A-Z]{2}[0-9]{2}") ? "_code" : "_label";
        return new JSONObject()
                .put("_qualification", qualification(recordKey, characteristic))
                .put("_recordLang", new JSONArray().put(new JSONObject()
                        .put("values", new JSONArray().put(localizedValue(key, value)))));
    }

    private static JSONObject localizedValue(String key, String value) {
        return new JSONObject()
                .put("_qualification", new JSONObject()
                        .put("language", new JSONObject().put("_code", "zxx")))
                .put(key, value);
    }

    private static JSONObject qualification(String recordKey, String characteristic) {
        return new JSONObject()
                .put("recordKey", recordKey)
                .put("characteristic", new JSONObject().put("_code", characteristic));
    }

    private static JSONObject buildUserRemark(PlaceholderStatusUpdate update) {
        JSONObject userRemark = new JSONObject()
                .put("comment", update.getComment())
                .put("date", now())
                .put("submittingRole", getConfig(CONFIG_SUBMITTING_ROLE, "O9"))
                .put("targetRole", getConfig(CONFIG_TARGET_ROLE, "Proveedor"))
                .put("status", getConfig(CONFIG_STATUS, "CS01"));
        String action = getConfig(CONFIG_ACTION, "");
        if (action.length() > 0) {
            userRemark.put("action", action);
        }
        return userRemark;
    }

    private static String getConfig(String key, String defaultValue) {
        String value = PropertiesManager.get(key);
        if (value == null || value.trim().length() == 0) {
            return defaultValue;
        }
        return value.trim();
    }

    private static String now() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("America/Mexico_City"));
        return formatter.format(new Date());
    }

    private static String pad4(int value) {
        String text = String.valueOf(value);
        StringBuilder sb = new StringBuilder();
        for (int i = text.length(); i < 4; i++) {
            sb.append('0');
        }
        sb.append(text);
        return sb.toString();
    }
}
