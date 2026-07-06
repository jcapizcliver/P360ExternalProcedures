package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import org.json.JSONObject;

public class ConfigurableProduct {


//    public static void main(String[] args) throws UnsupportedEncodingException {
//        ConfigurableProduct pe = new ConfigurableProduct();
        //pe.processInput("http://172.18.237.162:1512", "1698767480988016","88888,66666");
//        pe.processInput(args[0], args[1], args[2]);
        //pe.processInput(args[0]);
//    }

    public void processInput(String baseUrl, String proposalId, String relatedSKUs) {

        //String baseUrl = "http://172.18.237.162:1512/rest/V2.0";

        String objectAPIProduct2GURL = baseUrl + "/object/Product2G/";
        String listAPIProduct2GBySearchURL = baseUrl + "/list/Product2G/bySearch?query=";

        RestClient rc = new RestClient();
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put( "Content-Type", "application/json" );
        headers.put( "Accept", "application/json" );
        headers.put( "Authorization", "Basic " + PropertiesManager.get("p360.contingency.basic_token_auth") );
        headers.put( "Accept-Language", "es");

        String rawResponse = null;
        String rawResponseAux = null;
        JSONObject response = null;
        JSONObject responseAux = null;

        org.json.JSONArray rows = null;

        int currentIndex;
        int totalSize;

        String[] skus = relatedSKUs.split(",");
        ArrayList<String> skusList = new ArrayList<>(Arrays.asList(skus));
        ArrayList<String> skusListToInteractive = new ArrayList<>();


        String elAidi = "";
        String objectSKU = "";
        String objectModel = "";
        String objectBrand = "";
        String objectBrandS4H = "";
        String objectStructure = "";
        String objectBusiness = "";
        String skusToWrite = "";
        String payloadToWrite = "";

        rc = new RestClient();

        try {
            for(String value : skusList) {
                skusListToInteractive.add("\"" + value + "\"");
            }


            // Do the initial search
            String fieldsToSearch = "Product2G.ProductNo"
            		+ ",Product2GStructureMap.StructureGroup('PrimaryProductTaxonomy')->StructureGroup.Identifier"
            		+ ",SimpleProduct2GCharacteristicValue.LookupValue('BrandName')->LookupValue.Code"
            		+ ",SimpleProduct2GCharacteristicValueLang.Value('SupplierPartNumber',-1)"
            		+ ",SimpleProduct2GCharacteristicValueLang.Value('SKU',-1)"
            		+ ",SimpleProduct2GCharacteristicValue.LookupValue('BRAND_ID_S4H')->LookupValue.Code"
            		+ ",SimpleProduct2GCharacteristicValue.LookupValue('Business')->LookupValue.Code"
            		;

            try {
                rawResponse = rc.getRequest("GET", listAPIProduct2GBySearchURL + java.net.URLEncoder.encode("characteristic('ProductoConfigurable') is empty and characteristic('SKU') in " + skusListToInteractive.toString().replace("[", "(").replace("]", ")"), "UTF-8") + "&fields=" + java.net.URLEncoder.encode(fieldsToSearch, "UTF-8"), null, headers);

                response = new JSONObject(rawResponse);
                rows = response.getJSONArray("rows");

                //log(response);

            } catch (Exception e) {
            	log(rawResponse);
                logE(e);
            }


            // Do the process only if response checks with the list of SKUs
            if(response != null && response.getInt("totalSize") == skusList.size()) {
                log("Coincide...");

                // Get the SKU of Proposal
                try {
					rawResponseAux = rc.getRequest("GET", listAPIProduct2GBySearchURL + java.net.URLEncoder.encode("Product2G.ProductNo equals \"" + proposalId + "\"", "UTF-8") + "&fields=" + java.net.URLEncoder.encode(fieldsToSearch, "UTF-8"), null, headers);
				} catch (ServiceUnavailableException e) {
					logE(e);
				}

                responseAux = new JSONObject(rawResponseAux);

                //log(responseAux);

                log("Propuesta: " + proposalId);
                objectStructure = responseAux.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(1).getString(0);
                objectBrand = responseAux.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(2).getString(0);
                objectModel = responseAux.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(3).getString(0);
                objectSKU = responseAux.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(4).getString(0);
                objectBrandS4H = responseAux.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(5).getString(0);
                objectBusiness = responseAux.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(6).getString(0);


                /*
                log("SKU: " + objectSKU);
                log("Modelo: " + objectModel);
                log("Marca: " + objectBrand);
                log("Structure: " + objectStructure);
                log("Marca S4H: " + objectBrandS4H);
                log("Negocio: " + objectBusiness);
                 */

                skusListToInteractive.add("\"" + objectSKU + "\"");
                skusList.add(objectSKU);
                //log(skusList);

                // Master list is to write the list of SKUs in each product (Except its SKU)
                ArrayList<String> masterList = new ArrayList<>(skusListToInteractive);


                //log("Interactive List: " + skusListToInteractive);
                currentIndex = 0;

                do {
                    totalSize = 0;

                    skusListToInteractive = new ArrayList<>(masterList);

                    // Do a pre validation to verify each product check with the rule. It's only once before start to write
                    if(currentIndex == 0) {
                        for (int i = 0; i < rows.length(); i++) {
                            log(objectStructure + " VS " + rows.getJSONObject(i).getJSONArray("values").getJSONArray(1).getString(0));
                            log(objectModel + " VS " + rows.getJSONObject(i).getJSONArray("values").getJSONArray(3).getString(0));
                            if (Objects.equals(objectBusiness, "SBB")) {
                                log(objectBrand + " VS " + rows.getJSONObject(i).getJSONArray("values").getJSONArray(5).getString(0));
                            } else {
                                log(objectBrand + " VS " + rows.getJSONObject(i).getJSONArray("values").getJSONArray(2).getString(0));
                            }
                            log("------------");

                            if (!objectStructure.equals(rows.getJSONObject(i).getJSONArray("values").getJSONArray(1).getString(0))) {
                                log("Producto Configurable No Procede");
                                log("El producto \"" + proposalId + "\" esta en la plantilla \"" + objectStructure + "\" Y el producto \"" + rows.getJSONObject(i).getJSONArray("values").getString(0) + "\" esta en la plantilla \"" + rows.getJSONObject(i).getJSONArray("values").getJSONArray(1).getString(0) + "\"");
                                return;
                            }

                            if (!objectModel.equals(rows.getJSONObject(i).getJSONArray("values").getJSONArray(3).getString(0))) {
                                log("Producto Configurable No Procede");
                                log("El producto \"" + proposalId + "\" tiene el modelo \"" + objectBrand + "\" Y el producto \"" + rows.getJSONObject(i).getJSONArray("values").getString(0) + "\" tiene el modelo \"" + rows.getJSONObject(i).getJSONArray("values").getJSONArray(3).getString(0) + "\"");
                                return;
                            }

                            if (Objects.equals(objectBusiness, "SBB")) {
                                if (!objectBrandS4H.equals(rows.getJSONObject(i).getJSONArray("values").getJSONArray(5).getString(0))) {
                                    log("Producto Configurable No Procede");
                                    log("El producto \"" + proposalId + "\" tiene la marca (S4H) \"" + objectBrandS4H + "\" Y el producto \"" + rows.getJSONObject(i).getJSONArray("values").getString(0) + "\" tiene la marca (S4H) \"" + rows.getJSONObject(i).getJSONArray("values").getJSONArray(5).getString(0) + "\"");
                                    return;
                                }
                            } else {
                                if (!objectBrand.equals(rows.getJSONObject(i).getJSONArray("values").getJSONArray(2).getString(0))) {
                                    log("Producto Configurable No Procede");
                                    log("El producto \"" + proposalId + "\" tiene la marca \"" + objectBrand + "\" Y el producto \"" + rows.getJSONObject(i).getJSONArray("values").getString(0) + "\" tiene la marca \"" + rows.getJSONObject(i).getJSONArray("values").getJSONArray(2).getString(0) + "\"");
                                    return;
                                }
                            }

                        }
                    }

                    currentIndex++;

                    try {
						rawResponse = rc.getRequest( "GET", listAPIProduct2GBySearchURL + java.net.URLEncoder.encode("characteristic('ProductoConfigurable') is empty and characteristic('SKU') in " + masterList.toString().replace("[", "(").replace("]", ")"), "UTF-8") + "&fields=" + java.net.URLEncoder.encode(fieldsToSearch, "UTF-8"), null, headers );
					} catch (ServiceUnavailableException e) {
						logE(e);
					}

                    response = new JSONObject(rawResponse);


                    log(rawResponse);
                    elAidi = response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getString(0);
                    objectSKU = response.getJSONArray("rows").getJSONObject(0).getJSONArray("values").getJSONArray(4).getString(0);

                    log(elAidi + "  -----  " + objectSKU);
                    //skusList.remove(objectSKU);
                    skusListToInteractive.remove("\"" + objectSKU + "\"");
                    log("Interactive List: " + skusListToInteractive);
                    skusToWrite = skusListToInteractive.toString().replace("[","").replace("]","").replace(" ","").replace("[","").replace("\"","");
                    log(skusToWrite);

                    payloadToWrite = "{\"_characteristicRecords\":[{\"_qualification\":{\"characteristic\":{\"_code\":\"ProductoConfigurable\"}},\"_recordLang\":[{\"_qualification\":{\"language\":{\"_code\":\"zxx\"}},\"values\":[\"" + skusToWrite + "\"]}]}]}";

                    log( String.valueOf( masterList ) );
                    //log(skusList);
                    log( String.valueOf( response.getInt("totalSize") ) );
                    totalSize = response.getInt("totalSize");

                    rawResponseAux = rc.getRequest("PUT", objectAPIProduct2GURL + "'" + elAidi + "'@1", payloadToWrite, headers);

                } while(totalSize >= 1);


            } else {
                log("Posible problema en la petición REST, la lista de SKUs proporcionada tiene alguna inconsistencia o ya existe algún producto configurable previo");
            }

		} catch (IOException e) {
			logE(e);
        } catch (ServiceUnavailableException e) {
			logE(e);
		}


    }


    private void log(String message) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/configurableProdu.log", true)))) {
            pw.println("[" + (new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()))
                    + "]  " + message);
        } catch (java.io.IOException e) {
        }
    }


    private void logE(Exception ex) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(
                new java.io.FileOutputStream("../logs/configurableProdu.log", true)))) {
            ex.printStackTrace(pw);
        } catch (java.io.IOException e) {
        }
    }
}