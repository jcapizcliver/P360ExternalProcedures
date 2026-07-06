package mx.com.liverpool.p360.services.core.temp.product2g.json;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class IncidenciaImagenesBatidas {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get(args[0]).toFile())))){
			String line = null;
			org.json.JSONObject json = null;
			org.json.JSONArray products = null;
			org.json.JSONObject product = null;
			org.json.JSONArray variants = null;
			org.json.JSONObject variant = null;
			org.json.JSONArray photos = null;
			org.json.JSONObject photo = null;
			String name = null;
			java.util.Set<String> skusInName = new java.util.TreeSet<>();
			java.util.regex.Pattern p = java.util.regex.Pattern.compile("^[0-9]+");
			java.util.regex.Matcher m = null;
			String sku = null;
			String nombreCrudo = null;
			java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("\\[([0-9 \\.:A-Aa-z-]+)\\]");
			java.util.regex.Matcher m2 = null;
			String rawDate = null;
			java.util.List<Object[]> pares = new java.util.ArrayList<>();
			while((line = br.readLine()) != null) {
				m2 = p2.matcher(line);
				if(m2.find()) {
					rawDate = m2.group(1);
//					try{
//						java.util.Date d = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(rawDate);
//						pares.add(new Object[] { d, line.replaceFirst("^.+ An input: ", "") });
						try {
							json = new org.json.JSONObject(line.replaceFirst("^.+ An input: ", ""));
							products = json.getJSONArray("products");
							for(int i=0; i<products.length(); i++) {
								product = products.getJSONObject(i);
								if(product.has("variants")) {
									variants = product.getJSONArray("variants");
									for(int j=0; j<variants.length(); j++) {
										variant = variants.getJSONObject(j);
										if(variant.has("photos")) {
											photos = variant.getJSONArray("photos");
											for(int k=0; k<photos.length(); k++) {
												photo = photos.getJSONObject(k);
												if(photo.has("PhotoAssetName")) {
													name = photo.getString("PhotoAssetName");
													m = p.matcher(name);
													if(m.find()) {
														sku = m.group();
														nombreCrudo = null;
														if(sku.length() > 6)
															skusInName.add(sku);
													}else {
														sku = null;
														nombreCrudo = name;
													}
												}
											}
											if(skusInName.size() > 2) {
												System.out.println("See this: " + (product.has("proposalId") ? product.getString("proposalId") : "prod at pos: " + i) + " - " + ( variant.has("variantId") ? variant.getString("variantId") : "No variantId, pos: " + j ) + " - " + skusInName + " --  " + photos.length());
											}
											skusInName.clear();
										}
									}
								}
							}
						}catch(org.json.JSONException e) {
//							e.printStackTrace();
//							System.err.println("--->" + line);
						}
//					}catch(java.text.ParseException e) {
//						e.printStackTrace();
//					}
				}else{
					System.out.println("No proper line started. " + line);
					System.exit(0);
				}
			}
//			java.util.Collections.sort(pares, (o1,o2) -> ((java.util.Date)o1[0]).compareTo((java.util.Date)o2[0]) );
//			for(Object[] par : pares) {
				
//			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
