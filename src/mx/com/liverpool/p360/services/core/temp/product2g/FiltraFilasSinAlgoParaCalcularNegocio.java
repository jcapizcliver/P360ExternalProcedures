package mx.com.liverpool.p360.services.core.temp.product2g;

import mx.com.liverpool.p360.services.core.RESTWrapper;

public class FiltraFilasSinAlgoParaCalcularNegocio {

	private static final RESTWrapper rw = new RESTWrapper();
	
	public static void main(String[] args) {
		FiltraFilasSinAlgoParaCalcularNegocio pn = new FiltraFilasSinAlgoParaCalcularNegocio();
		pn.recuperaCososSinBusiness();
	}
	
	private void recuperaCososSinBusiness() {
		long init = System.currentTimeMillis();
		java.util.concurrent.ConcurrentLinkedQueue<Object[]> elements = new java.util.concurrent.ConcurrentLinkedQueue<Object[]>();
		try(java.util.stream.Stream<String> stream = java.nio.file.Files.lines(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "samples", "Productos3"))){
			stream.parallel().map(this::retrieveData).filter(this::filterData).forEach( elements::add );
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println("Data read, parsed and collected took: " + rw.getRw().formatTime(System.currentTimeMillis() - init));
		System.out.println("Total elements: " + elements.size());
		System.out.println( rw.getRw().serializeChunk( elements.element() ) );
	}
	
	private String[] retrieveData(String s) {
		String[] pieces = rw.getRw().parseLine(s);
		return pieces;
	}
	
	private boolean filterData(String[] data) {
		return "".equals(data[1]) && !(!"".equals(data[4]) || !"".equals(data[6]));
	}
	
}
