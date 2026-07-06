package mx.com.liverpool.dataprofiling.preparison;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;

public class CantidadDeColumnasYRegistrosPorTabla {

	private static final RESTWrapper rw = new RESTWrapper();	
	private static final RESTWorkshop workshop = rw.getRw();

	public static void main(String[] args) {
		long init = System.currentTimeMillis();
		java.util.Set<String> mdf = new java.util.TreeSet<>();
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "Characteristic.Identifier");
		qp.put("query", "Characteristic.Category->LookupValue.Code = \"Master Data\" or Characteristic.Category->LookupValue.Code = \"DatosLogisticos\"");
		qp.put("pageSize", "10000");
		rw.collectData("list", "Characteristic", null, "bySearch", qp, row -> mdf.add(row.getJSONArray("values").getString(0)) );
		System.out.println(mdf.size());
		mdf.removeIf( a -> a.endsWith("_Rechazo") );
		System.out.println(mdf.size());
		java.util.concurrent.ConcurrentMap<String, Long> productTableID = null;
		try( java.util.stream.Stream<String> lines = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "crp-pro-dwh-semanticagold.EIL_DP_VMASTER.VFAC_GOB_PROD_PRODUCTO_ATRIB_DET.csv.bkp")) ){
			productTableID = lines
					.parallel()
					.filter(l -> !"TPIM_PROD_ID,PIM_SKU_CVE,PIM_PADRE_SKU_CVE,PIM_PROD_NOM,PIM_PROD_TIPO_NOM,PIM_NIVEL_ID,PIM_PLANTILLA_ID,PIM_ATRIB_PRODUCTTYPESAP,PIM_ATRIB_PRODUCTNAME,PIM_ATRIB_ITEMGROUP,FCH_CARGA".equals(l))
					.map(workshop::parseLine)
					.map(a -> a[9])
					.map(l -> "null".equals(l) ? "" : l)
					.collect(java.util.stream.Collectors.groupingByConcurrent(java.util.function.Function.identity(), java.util.stream.Collectors.counting()))
				;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Base data read. " + workshop.formatTime(System.currentTimeMillis() - init));
		java.util.concurrent.ConcurrentMap<String, Long> tableCounters = new java.util.concurrent.ConcurrentHashMap<>();
		try( java.util.stream.Stream<String> lines = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "prof", "FrecuenciasPorAtributoPorGrupoDeArtículo.csv")) ){
			tableCounters = lines
					.parallel()
					.filter(l -> !"Template,Attribute,Freq".equals(l))
					.map(workshop::parseLine)
					.filter(a -> !mdf.contains( a[1]) )
					.map(a -> a[0])
					.collect(java.util.stream.Collectors.groupingByConcurrent(java.util.function.Function.identity(), java.util.stream.Collectors.counting()))
				;
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		java.util.LinkedList<java.util.Map.Entry<String, Long>> list = new java.util.LinkedList<>( tableCounters.entrySet() );
		java.util.concurrent.ConcurrentMap<String, Long> am = productTableID;
		java.util.Collections.sort(list, (o1,o2)-> am.get( o2.getKey() ).compareTo( am.get( o1.getKey()) ));
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "prof", "TableInfoIGByFields.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			pw.println( workshop.serializeChunk( new Object[] { "Template", "#Fields", "#Records" } ) );
			list.forEach(en -> pw.println( workshop.serializeChunk( new Object[] { en.getKey(), en.getValue(), am.get(en.getKey()) } ) ));
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
