package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CreateTemplatesAsTables {

	private static final RESTWrapper rw = new RESTWrapper();
	private static final RESTWorkshop workshop = rw.getRw();
	
	private static String prevTemplate = null;
	private static String prevID = null;
	private static String prevSKU = null;
	private static String prevParentSKU = null;
	private static String prevType = null;
	private static java.util.Set<String> attributes = new java.util.TreeSet<>();
	private static java.util.Map<String, java.util.Set<String>> attributesByTemplate = new java.util.TreeMap<>();
	private static java.util.Map<String, String[]> values = new java.util.TreeMap<>();
	
	public static void main(String[] args) {
		System.out.println("Collecting attributes by template.");
		try(java.util.stream.Stream<String> lines = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "ItemGroup", "Attributes", "AtributosPlantillas.csv"))){
			lines
				.filter(l -> !l.startsWith("ItemGroup,PIM_PROD_ID,PIM_ATRIBUTO_ID,PIM_ATRIBUTO_VAL_ID,PIM_ATRIBUTO_VAL,PIM_SKU_CVE,PIM_PADRE_SKU_CVE,PIM_PROD_TIPO_NOM"))
				.map(workshop::parseLine)
				.forEach( a -> {
					if( !definingAttributes.contains(a[2]) ) {
						if(prevTemplate != null && !prevTemplate.equals(a[0])) {
							attributesByTemplate.put(prevTemplate, attributes);
							attributes = new java.util.TreeSet<>();
						}
						attributes.add(a[2]);
					}
					prevTemplate = a[0];
				} );
			if(!attributes.isEmpty()) {
				attributesByTemplate.put(prevTemplate, attributes);
				attributes = null;
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
		prevTemplate = null;
		java.util.Map<String, java.io.PrintWriter> writers = new java.util.TreeMap<>();
//		java.util.Map<String, java.io.PrintWriter> writersVariants = new java.util.TreeMap<>();
		java.io.PrintWriter pw = null;
		for(String template : attributesByTemplate.keySet()) {
			try {
				writers.put(template, pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "itemGroupsAsTablesOnlyProducts", template.replaceAll("/", "_") + ".csv").toFile()), java.nio.charset.StandardCharsets.UTF_8)));
				printHeader(pw, attributesByTemplate.get(template));
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println( writers.size() + ". Now collecting records...");
		int[] am = new int[1];
		am[0] = 0;
		java.math.BigDecimal tz = new java.math.BigDecimal(attributesByTemplate.size());
		java.util.Set<String> types = new java.util.TreeSet<>();
		types.add("SalesItemFamily");
		types.add("SalesItemFamilyMkt"); 
		types.add("SalesItem");
		try(java.util.stream.Stream<String> lines = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "ItemGroup", "Attributes", "AtributosPlantillas.csv"))){
			lines
				.filter(l -> !"ItemGroup,PIM_PROD_ID,PIM_ATRIBUTO_ID,PIM_ATRIBUTO_VAL_ID,PIM_ATRIBUTO_VAL,PIM_SKU_CVE,PIM_PADRE_SKU_CVE,PIM_PROD_TIPO_NOM".equals(l))
				.map(workshop::parseLine)
				.forEach( a -> {
					if( types.contains(a[7]) && ( !"SalesItem".equals(a[7]) || ( "SalesItem".equals(a[7]) && "".equals(a[6]) ) ) && attributesByTemplate.containsKey(a[0]) ) {
						if(prevTemplate != null && prevID != null && ( !prevTemplate.equals(a[0]) || !prevID.equals(a[1])) ) {
							printAccordingToFields( writers.get(prevTemplate) , attributesByTemplate.get(prevTemplate), prevID, values, prevTemplate, prevSKU, prevParentSKU, prevType );
							values = new java.util.TreeMap<>();
							if(!prevTemplate.equals(a[0])) {
								writers.remove(prevTemplate).close();
								am[0]++;
								System.out.println(am[0] + "/" + attributesByTemplate.size() + " (" + new java.math.BigDecimal(am[0]).multiply(java.math.BigDecimal.TEN.pow(2)).divide(tz, 4, java.math.RoundingMode.HALF_UP) + ")");
							}
						}
						values.put(a[2], new String[] { a[3], a[4] });
						prevTemplate = a[0];
						prevID = a[1];
						prevSKU = a[5];
						prevParentSKU = a[6];
						prevType = a[7];
					}
				} );
			if(!values.isEmpty()) {
				printAccordingToFields( writers.get(prevTemplate), attributesByTemplate.get(prevTemplate), prevID, values, prevTemplate, prevSKU, prevParentSKU, prevType );
				values = null;
				writers.remove(prevTemplate).close();
				am[0]++;
				System.out.println(am[0] + "/" + attributesByTemplate.size() + " (" + new java.math.BigDecimal(am[0]).multiply(java.math.BigDecimal.TEN.pow(2)).divide(tz, 4, java.math.RoundingMode.HALF_UP) + ")");
			}
			if(!writers.isEmpty()) {
				System.out.println("Épale pariente, aquí se te quedaron estos: ");
				writers.forEach((k,v) -> System.out.println(k) );
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static void printHeader(java.io.PrintWriter pw, java.util.Set<String> header) {
		String[] headerArray = header.toArray(new String[] {});
		String[] data = new String[header.size() + 4];
		data[0] = "IDProduct";
		for(int i=0; i<headerArray.length; i++) {
			data[i + 1] = headerArray[i];
		}
		data[data.length - 3] = "PIM_SKU_CVE";
		data[data.length - 2] = "PIM_PADRE_SKU_CVE";
		data[data.length - 1] = "PIM_PROD_TIPO_NOM";
		pw.println( workshop.serializeChunk( data ) );
	}
	
	private static void printAccordingToFields(
			  java.io.PrintWriter pw
			, java.util.Set<String> header
			, String id
			, java.util.Map<String, String[]> values
			, String template
			, String sku
			, String parentSKU
			, String productType
	) {
		String[] pieces = new String[header.size() + 4];
		String[] value = null;
		int i = 0;
		pieces[i] = id;
		i++;
		for(String column : header) {
			value = values.get(column);
			pieces[i] = value == null ? "" : !"".equals(value[0]) ? workshop.serializeChunk(value) : value[1];
			i++;
		}
		pieces[ pieces.length - 3 ] = sku;
		pieces[ pieces.length - 2 ] = parentSKU;
		pieces[ pieces.length - 1 ] = productType;
		if("".equals(pieces[0]) || pieces[0] == null) {
			System.out.println( "\tPrinting an empty ID! (" + template + ")" );
		}
		pw.println( workshop.serializeChunk( pieces ));
	}
	
	public static final java.util.Set<String> definingAttributes = new java.util.TreeSet<>( java.util.Arrays.asList(new String[] {
			"TamanoUnico",
			"ColoursLiverpoolAtt",
			"SB_COLORES",
			"SB_TJOYERIAYACCESORIOS",
			"SB_THOGAR",
			"LadiesSizeAtt",
			"SB_TCALCETERIA",
			"MenSizeAtt",
			"Direction1SizeAtt",
			"Direction3SizeAtt",
			"SB_T_HARDLINE",
			"SB_0025",
			"SB_TINFANTILES",
			"SB_TBEBES",
			"SB_0106",
			"SB_TROPAINTERIOR",
			"SB_TJUNIORS",
			"SB_TZAPATOS",
			"SB_TCABALLEROS",
			"SB_TLENCERIA",
			"SferaSizeAtt",
			"TamanoDireccion6Att",
			"TamanoDireccion8Att",
			"SizeCosmeticsAccAtt",
			"SportsSizeAtt",
			"TamanoPantallaAtt",
			"SB_T_TECNO_ENTREN",
			"ShoeSizeLivAtt",
			"SB_TDAMAS",
			"OpticalSizeAtt",
			"ChildrenSizeAtt",
			"SupplierPartNumber"
			
	}));
}
