package mx.com.liverpool.p360.services.core.temp.lookup;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class PPHPartyItemGroupBrandAnalysis {

	private static final RESTWorkshop rw = new RESTWorkshop();
	
	public static void main(String[] srgs) {
		/*
		java.util.Set<String> templates1 = new java.util.TreeSet<>();
		java.util.Set<String> parties = new java.util.TreeSet<>();
		java.util.Set<String> itemGroups = new java.util.TreeSet<>();
		java.util.Set<String> brands = new java.util.TreeSet<>();
		java.util.Set<String> templatesParties = new java.util.TreeSet<>();
		loadFileContentsResult(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "Party PPHL4Templates ItemGroup", "analysis.csv"), templates1, parties, itemGroups, brands, templatesParties);
		System.out.println("Templates:\t\t\t" + templates1.size());
		System.out.println("Party:\t\t\t" + parties.size());
		System.out.println("ItemGruops:\t\t\t" + itemGroups.size());
		System.out.println("Brands:\t\t\t" + brands.size());
		System.out.println("PlantillasProveedor:\t\t\t" + templatesParties.size());
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("C:\\opt\\LVP\\desorden\\Party PPHL4Templates ItemGroup\\hola.csv")))){
			templatesParties.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		templatesParties.forEach(System.out::println);
		System.exit(0);
		*/
		System.out.println("Starting...");
		java.util.Set<String> currentSetupTemplates = templatesInMetadata();
		java.util.Map<String, ReferenceData> dataParty = new java.util.TreeMap<>();
		java.util.Map<String, ReferenceData> dataPPH = new java.util.TreeMap<>();
		java.util.Map<String, ReferenceData> dataItemGroup = new java.util.TreeMap<>();
		loadFileContents(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "Party PPHL4Templates ItemGroup", "Marcas Proveedores.csv"), dataParty);
		loadFileContents(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "Party PPHL4Templates ItemGroup", "Marcas PPH_L4_Templates.csv"), dataPPH);
		loadFileContents(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "Party PPHL4Templates ItemGroup", "Marcas de Grupos de Artículos.csv"), dataItemGroup);
		java.util.LinkedList<String[]> tuplas = new java.util.LinkedList<>();
		// Los itemgroup de la plantilla
		// Las marcas de la plantilla
		java.util.Map<String, java.util.Set<String>> itemGroupToTemplate = new java.util.TreeMap<>();
		java.util.Map<String, java.util.Set<String>> brandToTemplate = new java.util.TreeMap<>();
		transpose(dataPPH, itemGroupToTemplate, brandToTemplate);
		dataParty.forEach((k,v)->{
			java.util.Map<String, String> itemGroupParty = v.getLookup("Grupo artículosLOV");
			java.util.Map<String, String> brandsParty = v.getLookup("SubmarcaLOV");
			ReferenceData rd = null;
			java.util.Map<String, String> brandsItemGroup = null;
			java.util.Map<String, String> brandsTemplate = null;
			java.util.Set<String> templates = null;
			ReferenceData rdTemplate = null;
			if(itemGroupParty != null && brandsParty != null) {
				for(java.util.Map.Entry<String, String> entry : itemGroupParty.entrySet()) {
					rd = dataItemGroup.get(entry.getKey());
					if(rd != null) {
						brandsItemGroup = rd.getLookup("SubmarcaLOV");
						templates = itemGroupToTemplate.get(entry.getKey());
						if(templates != null && !templates.isEmpty() && brandsItemGroup != null) {
							for( String template : templates ) {
								if(currentSetupTemplates.contains(template)) {
									rdTemplate = dataPPH.get(template);
									if(rdTemplate != null) {
										brandsTemplate = rdTemplate.getLookup("SubmarcaLOV");
										if(brandsTemplate != null) {
											for(java.util.Map.Entry<String, String> entryBrandTemplate : brandsTemplate.entrySet()) {
												
												if(brandsItemGroup.containsKey(entryBrandTemplate.getKey())
														&& brandsParty.containsKey(entryBrandTemplate.getKey())) {
													tuplas.addLast(new String[] { k, template, entry.getKey(), entryBrandTemplate.getKey() });
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		});
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "Party PPHL4Templates ItemGroup", "analysis.csv").toString())))){
			tuplas.forEach(tupla -> pw.println(tupla[0] + "|" + tupla[1] + "|" + tupla[2] + "|" + tupla[3]) );
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		

		java.util.Set<String> templates1 = new java.util.TreeSet<>();
		java.util.Set<String> parties = new java.util.TreeSet<>();
		java.util.Set<String> itemGroups = new java.util.TreeSet<>();
		java.util.Set<String> brands = new java.util.TreeSet<>();
		java.util.Set<String> templatesParties = new java.util.TreeSet<>();
		loadFileContentsResult(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "Party PPHL4Templates ItemGroup", "analysis.csv"), templates1, parties, itemGroups, brands, templatesParties);
		System.out.println("Templates:\t\t\t" + templates1.size());
		System.out.println("Party:\t\t\t" + parties.size());
		System.out.println("ItemGruops:\t\t\t" + itemGroups.size());
		System.out.println("Brands:\t\t\t" + brands.size());
		System.out.println("PlantillasProveedor:\t\t\t" + templatesParties.size());
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream("C:\\opt\\LVP\\desorden\\Party PPHL4Templates ItemGroup\\hola.csv")))){
			templatesParties.forEach(pw::println);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		templatesParties.forEach(System.out::println);
		
		
		System.out.println("Done.");
	}
	
	private static java.util.Set<String> templatesInMetadata(){
		java.util.Set<String> templates = new java.util.TreeSet<>();
		rw.setBaseUrl("https://webctep360qas.liverpool.com.mx/rest/V2.0");
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "StandardizationValue.StructureGroup->LookupValue.Code");
		qp.put("dictionary", "ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla");
		qp.put("pageSize", "1200");
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		int a = 0;
		int b = 0;
		do {
			qp.put("startIndex", String.valueOf(a));
			response = rw.makeRequest("GET", "/list/StandardizationValue/byDictionary", qp, null);
			if(response != null) {
				b = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					values = rows.getJSONObject(i).getJSONArray("values");
					if(!"".equals(values.getString(0))) {
						templates.add(values.getString(0));
					}
				}
				a += response.getInt("pageSize");
			}else {
				System.out.println("ERROR: " + rw.getRawResponse());
			}
			System.out.println(a + "/" + b);
		}while(a<b);
		a = 0;
		return templates;
	}
	
	private static void transpose(java.util.Map<String, ReferenceData> templateData
			, java.util.Map<String, java.util.Set<String>> itemGroupToTemplate
			, java.util.Map<String, java.util.Set<String>> brandToTemplate
		) {
		templateData.forEach((k,v)->{
			java.util.Map<String, String> itemGroups = v.getLookup("Grupo artículosLOV");
			java.util.Map<String, String> brands = v.getLookup("SubmarcaLOV");
			if(itemGroups != null && brands != null) {
				itemGroups.forEach((k1,v1)->{
					java.util.Set<String> rds = null;
					rds = itemGroupToTemplate.get(k1);
					if(rds == null) {
						rds = new java.util.TreeSet<>();
						itemGroupToTemplate.put(k1, rds);
					}
					rds.add(k);
				});
				brands.forEach((k1,v1)->{
					java.util.Set<String> rds = null;
					rds = brandToTemplate.get(k1);
					if(rds == null) {
						rds = new java.util.TreeSet<>();
						brandToTemplate.put(k1, rds);
					}
					rds.add(k);
				});
			}
		});
	}
	
	private static void loadFileContentsResult(java.nio.file.Path p, java.util.Set<String> templates, java.util.Set<String> parties, java.util.Set<String> itemGroups, java.util.Set<String> brands, java.util.Set<String> templateParty) {
		ReferenceData rd = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(p.toString())))){
			String ln = null;
			String[] pieces;
			String delim = "";
			String sep = "|";
			String esc = "";
			ln = br.readLine();
			String[] header = rw.parseLine(ln, delim, sep, esc);
			while((ln = br.readLine()) != null) {
				pieces = rw.parseLine(ln, delim, sep, esc);
				templates.add(pieces[0]);
				parties.add(pieces[1]);
				itemGroups.add(pieces[2]);
				brands.add(pieces[3]);
				templateParty.add(pieces[0] + "|" + pieces[1]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void loadFileContents(java.nio.file.Path p, java.util.Map<String, ReferenceData> data) {
		ReferenceData rd = null;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(p.toString())))){
			String ln = null;
			String[] pieces;
			String delim = "";
			String sep = "|";
			String esc = "";
			ln = br.readLine();
			String[] header = rw.parseLine(ln, delim, sep, esc);
			while((ln = br.readLine()) != null) {
				pieces = rw.parseLine(ln, delim, sep, esc);
				rd = data.get(pieces[0]);
				if(rd == null) {
					rd = new ReferenceData(pieces[1]);
					data.put(pieces[0], rd);
				}
				rd.addReferenceValue(pieces[2], pieces[3], pieces[4]);
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	/*
	 	"Code"|"Name(es)"|"LookupReference"|"LookupReferenceValueCode"|"LookupReferenceValueName(es)"
		EU4-30596699|Atril|Characteristic|SupplierPartNumber|Modelo
		EU4-30596699|Atril|Grupo artículosLOV|36508|ACCESORIOS
		EU4-30596699|Atril|Grupo artículosLOV|63124|BASE Y ATRIL
		EU4-32110663|Goma para tacón|Characteristic|AltoVaD|
		EU4-32110663|Goma para tacón|Grupo artículosLOV|55008|ACC ZAPATOS
		EU4-32110663|Goma para tacón|ProductoLOV|1442|Goma tacón
		EU4-31266088|Cigarrera|Characteristic|MaterialAtt|Material
		EU4-31266088|Cigarrera|Grupo artículosLOV|21308|FUMADOR
		EU4-31266088|Cigarrera|Grupo artículosLOV|37710|FUMADOR
		EU4-31266088|Cigarrera|Grupo artículosLOV|21311|ACC CABALLERO
		EU4-28908318|Pulsera/Banda inteligente|Characteristic|clothin
	 */
	static class ReferenceData{
		
		private String name;
		private java.util.Map<String, java.util.Map<String, String>> references = new java.util.TreeMap<>();
		
		public ReferenceData(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public void addReferenceValue(String lookup, String code, String value) {
			java.util.Map<String, String> content = references.get(lookup);
			if(content == null) {
				content = new java.util.TreeMap<>();
				references.put(lookup, content);
			}
			content.put(code, value);
		}
		
		public java.util.Map<String, String> getLookup(String lookup){
			return references.get(lookup);
		}
		
		public String getValue(String lookup, String code) {
			java.util.Map<String, String> data = references.get(lookup);
			if(data != null) {
				return data.get(code);
			}
			return null;
		}
		
		public java.util.Map<String, java.util.Map<String, String>> getReferences(){
			return references;
		}
		
	}
}
