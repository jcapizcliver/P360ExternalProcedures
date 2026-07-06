package mx.com.liverpool.p360.services.core;
import org.json.JSONObject;

public class PublicationExceptions {

    public boolean isException(RESTWorkshop rw, String inputProduct) {
    	boolean isException = false;
    	String objectAPIProduct2GURL = rw.getBaseUrl() + "/object/Product2G/";
    	String listAPIStandardizationValueByIdentifiersURL = rw.getBaseUrl() + "/list/StandardizationValue/byIdentifiers?identifiers=";
    	
    	java.util.Map<String, String> headers = rw.getRc().getHeader();

        String rawResponse = null;
        JSONObject response = null;

        int productFound = -1;
        int exceptionCancelledFound = -1;

        String productSKU = "";
        String productBusiness = "";
        String productBrandName = "";
        String productBrandNameSBB = "";
        String productItemGroup = "";
        String productItemGroupSBB = "";
        String productSupplier = "";
        String productSection = "";
        
        final RestClient rc = rw.getRc();

        try {
            rawResponse = rc.getRequest( "GET", 
            		objectAPIProduct2GURL + java.net.URLEncoder.encode(inputProduct, "UTF-8" ) + "?includeLabels=true", null, headers );
            System.out.println(rawResponse);
            response = new JSONObject(rawResponse);

            productSKU = getCharacteristicValueFromObject(response, "SKU");
            productBusiness = getCharacteristicLookupValueCodeFromObject(response, "Business");
            productBrandName = getCharacteristicLookupValueLabelFromObject(response, "BrandName");
            productBrandNameSBB = getCharacteristicLookupValueLabelFromObject(response, "BRAND_ID_S4H");
            productItemGroup = getCharacteristicLookupValueCodeFromObject(response, "ItemGroup");
            productItemGroupSBB = getCharacteristicLookupValueLabelFromObject(response, "ItemGroupS4H");
            productSupplier = getCharacteristicValueFromObject(response, "SupplierID");
            productSection = getCharacteristicLookupValueCodeFromObject(response, "Section");

            // Excepcion por Seccion y Negocio
            if (productSection != null && !"".equals( productSection ) && productBusiness != null && !"".equals( productBusiness ) && productFound != 1) {
                rawResponse = rc.getRequest("GET", listAPIStandardizationValueByIdentifiersURL  + java.net.URLEncoder.encode(productSection + "|" + productBusiness, "UTF-8") + "&dictionaryProxy=" + java.net.URLEncoder.encode("'ExcepcionesPorSeccionYNegocio'","UTF-8"), null, headers);
                response = new JSONObject(rawResponse);
                productFound = response.getInt("totalSize");
                if(productFound > 0) {
                	isException = true;
                    log(inputProduct + " is not published by Section and Business");
                    rawResponse = rc.getRequest("PUT", objectAPIProduct2GURL  + java.net.URLEncoder.encode(inputProduct, "UTF-8"), createPayload("true", "1"), headers);
                }
            }

            // Excepcion por Seccion y Marca
            if (productSection != null && !"".equals( productSection ) && (productBrandName != null && !"".equals( productBrandName ) || productBrandNameSBB != null && !"".equals( productBrandNameSBB )) && productFound <= 0) {
                if(productBrandName != null && !"".equals( productBrandName )) {
                    rawResponse = rc.getRequest("GET", listAPIStandardizationValueByIdentifiersURL  + java.net.URLEncoder.encode(productSection + "|" + productBrandName, "UTF-8") + "&dictionaryProxy=" + java.net.URLEncoder.encode("'ExcepcionesPorSeccionYMarca'","UTF-8"), null, headers);
                    response = new JSONObject(rawResponse);
                    productFound = response.getInt("totalSize");
                } else {
                    rawResponse = rc.getRequest("GET", listAPIStandardizationValueByIdentifiersURL  + java.net.URLEncoder.encode(productSection + "|" + productBrandNameSBB, "UTF-8") + "&dictionaryProxy=" + java.net.URLEncoder.encode("'ExcepcionesPorSeccionYMarca'","UTF-8"), null, headers);
                    response = new JSONObject(rawResponse);
                    productFound = response.getInt("totalSize");
                }
                if(productFound > 0) {
                	isException = true;
                    log(inputProduct + " is not published by Section and Brand");
                    rawResponse = rc.getRequest("PUT", objectAPIProduct2GURL  + java.net.URLEncoder.encode(inputProduct, "UTF-8"), createPayload("true", "2"), headers);
                }
            }

            // Excepcion por Seccion y Proveedor
            if (productSection != null && !"".equals( productSection ) && productSupplier != null && !"".equals( productSupplier ) && productFound <= 0) {
                rawResponse = rc.getRequest("GET", listAPIStandardizationValueByIdentifiersURL  + java.net.URLEncoder.encode(productSection + "|" + productSupplier, "UTF-8") + "&dictionaryProxy=" + java.net.URLEncoder.encode("'ExcepcionesPorSeccionYProveedor'","UTF-8"), null, headers);
                response = new JSONObject(rawResponse);
                productFound = response.getInt("totalSize");
                if(productFound > 0) {
                	isException = true;
                    log(inputProduct + " is not published by Section and Supplier");
                    rawResponse = rc.getRequest("PUT", objectAPIProduct2GURL  + java.net.URLEncoder.encode(inputProduct, "UTF-8"), createPayload("true", "3"), headers);
                }
            }

            // Excepcion por Grupo de Articulo
            if ((productItemGroup != null && !"".equals( productItemGroup ) || productItemGroupSBB != null && !"".equals( productItemGroupSBB )) && productFound <= 0) {
                if(productItemGroup != null && !"".equals( productItemGroup )) {
                    rawResponse = rc.getRequest("GET", listAPIStandardizationValueByIdentifiersURL  + java.net.URLEncoder.encode(productItemGroup, "UTF-8") + "&dictionaryProxy=" + java.net.URLEncoder.encode("'ExcepcionesPorGrupoDeArticulo'","UTF-8"), null, headers);
                    response = new JSONObject(rawResponse);
                    productFound = response.getInt("totalSize");
                } else {
                    rawResponse = rc.getRequest("GET", listAPIStandardizationValueByIdentifiersURL  + java.net.URLEncoder.encode(productItemGroupSBB, "UTF-8") + "&dictionaryProxy=" + java.net.URLEncoder.encode("'ExcepcionesPorGrupoDeArticulo'","UTF-8"), null, headers);
                    response = new JSONObject(rawResponse);
                    productFound = response.getInt("totalSize");
                }
                if(productFound > 0) {
                	isException = true;
                    log(inputProduct + " is not published by Group of Article");
                    rawResponse = rc.getRequest("PUT", objectAPIProduct2GURL  + java.net.URLEncoder.encode(inputProduct, "UTF-8"), createPayload("true", "4"), headers);
                }
            }

            // Excepcion por SKU
            if (productSKU != null && !"".equals( productSKU ) && productFound <= 0) {
                rawResponse = rc.getRequest("GET", listAPIStandardizationValueByIdentifiersURL  + java.net.URLEncoder.encode(productSKU, "UTF-8") + "&dictionaryProxy=" + java.net.URLEncoder.encode("'ExcepcionesPorSKU'","UTF-8"), null, headers);
                response = new JSONObject(rawResponse);
                productFound = response.getInt("totalSize");
                if(productFound > 0) {
                	isException = true;
                    log(inputProduct + " is not published by SKU");
                    rawResponse = rc.getRequest("PUT", objectAPIProduct2GURL  + java.net.URLEncoder.encode(inputProduct, "UTF-8"), createPayload("true", "5"), headers);
                }
            }

            // Excepcion Anulada
            if (productSKU != null && !"".equals( productSKU ) && productFound > 0) {
                rawResponse = rc.getRequest("GET", listAPIStandardizationValueByIdentifiersURL  + java.net.URLEncoder.encode(productSKU, "UTF-8") + "&dictionaryProxy=" + java.net.URLEncoder.encode("'ExcepcionesAnuladas'","UTF-8"), null, headers);
                response = new JSONObject(rawResponse);
                exceptionCancelledFound = response.getInt("totalSize");
                if(exceptionCancelledFound > 0) {
                    log(inputProduct + " is published due to it is a Cancelled Exception");
                    isException = false;
                    rawResponse = rc.getRequest("PUT", objectAPIProduct2GURL  + java.net.URLEncoder.encode(inputProduct, "UTF-8"), createPayload("false", "6"), headers);
                }
            }
        }catch(Exception e) {
        	logE(e);
        	e.printStackTrace();
        }
        return isException;
    }

    private String getCharacteristicLookupValueCodeFromObject(org.json.JSONObject objectAPIResponse, String characteristic){
        org.json.JSONObject entry = null;
        org.json.JSONObject data = null;
        org.json.JSONArray characteristicRecords = null;
        try{
            data = objectAPIResponse.getJSONObject("_data");
            characteristicRecords = data.getJSONArray("_characteristicRecords");
            for(int i=0; i<characteristicRecords.length(); i++){
                entry = characteristicRecords.getJSONObject(i);
                if(characteristic.equals( entry.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code") )){
                    return entry.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_code");
                }
            }
        }catch(org.json.JSONException e){ }
        return null;
    }


    private String getCharacteristicLookupValueLabelFromObject(org.json.JSONObject objectAPIResponse, String characteristic){
        org.json.JSONObject entry = null;
        org.json.JSONObject data = null;
        org.json.JSONArray characteristicRecords = null;
        try{
            data = objectAPIResponse.getJSONObject("_data");
            characteristicRecords = data.getJSONArray("_characteristicRecords");
            for(int i=0; i<characteristicRecords.length(); i++){
                entry = characteristicRecords.getJSONObject(i);
                if(characteristic.equals( entry.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code") )){
                    return entry.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0).getString("_label");
                }
            }
        }catch(org.json.JSONException e){ }
        return null;
    }

    private String getCharacteristicValueFromObject(org.json.JSONObject objectAPIResponse, String characteristic){
        org.json.JSONObject entry = null;
        org.json.JSONObject data = null;
        org.json.JSONArray characteristicRecords = null;
        try{
            data = objectAPIResponse.getJSONObject("_data");
            characteristicRecords = data.getJSONArray("_characteristicRecords");
            for(int i=0; i<characteristicRecords.length(); i++){
                entry = characteristicRecords.getJSONObject(i);
                if(characteristic.equals( entry.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code") )){
                    return String.valueOf( entry.getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").get(0) );
                }else {
                	System.out.println(characteristic + " ----->" + entry.getJSONObject("_qualification").getJSONObject("characteristic").getString("_code"));
                }
            }
        }catch(org.json.JSONException e){ }
        return null;
    }

    private static String createPayload (String isPublish, String exceptionType) {
        String payload = null;
        payload = "{\"_characteristicRecords\": [{\"_qualification\": {\"characteristic\": {\"_code\": \"IsPublishException\"}},\"_recordLang\": [{\"_qualification\":{\"language\": {\"_code\": \"zxx\"}},\"values\": [" + isPublish + "]}]},{\"_qualification\": {\"characteristic\": {\"_code\":\"PublishExceptionType\"}},\"_recordLang\": [{\"_qualification\": {\"language\": {\"_code\": \"zxx\"}},\"values\": [{\"_code\": \"" + exceptionType + "\"}]}]}]}";
        return payload;
    }

    private void log(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","publicationExceptions.log").toString(), true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "]  " + message);
        } catch (java.io.IOException e) {
        }
    }


    private static void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(java.nio.file.Paths.get("..","logs","publicationExceptions.log").toString(), true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }
}
