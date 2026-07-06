package mx.com.liverpool.p360.services.core.temp.product2g.maintenance;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class EnviarAnInputEspecificos {

	
	public static void main(String[] args) {
		java.util.List<String> proposals = new java.util.ArrayList<>();
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			String line = null;
			while((line = br.readLine()) != null) {
				proposals.add(line);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		if(!proposals.isEmpty()) {
			java.util.Map<String, Integer> hits = new java.util.TreeMap<>();
			Integer freq = null;
			java.util.Collections.sort(proposals);
			RESTWrapper rw = new RESTWrapper();
			rw.getRw().setBaseUrl("http://localhost:8080/process-engine");
			rw.getRw().getRc().getHeader().remove("Authorization");
			java.util.Map<String, String> qp = new java.util.TreeMap<>();
			try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[1]).toFile()), java.nio.charset.StandardCharsets.UTF_8))){
				String line = null;
				org.json.JSONObject request = null;
				org.json.JSONArray products = null;
				while((line = br.readLine()) != null) {
					try{
						request = new org.json.JSONObject(line);
						products = request.getJSONArray("products");
						for(int i=0; i<products.length(); i++) {
							if(java.util.Collections.binarySearch( proposals, products.getJSONObject(i).getString("proposalId") ) >= 0) {
								rw.getRw().makeRequest("POST", "/public/rt/CreateProposal", qp, new org.json.JSONObject().put("input", request.toString()).toString());
								freq = hits.get(products.getJSONObject(i).getString("proposalId"));
								hits.put(products.getJSONObject(i).getString("proposalId"), (freq == null ? 0 : freq) + 1);
								System.out.println(rw.getRw().getRawResponse());
								break;
							}
						}
					}catch(org.json.JSONException e) {
						System.out.println("Unparseable --->" + line + "<---");
					}
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
			System.out.println("Now these are not in the logs...");
			for(String productIdHit : proposals) {
				if(!hits.containsKey(productIdHit)) {
					System.out.println(productIdHit);
				}
			}
		}
	}
	
}
