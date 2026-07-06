package mx.com.liverpool.dataprofiling;

public class PackDeLineas implements java.lang.Runnable {

	private final java.util.concurrent.ArrayBlockingQueue<Pack> packs;
	private boolean running = true;
	
	public PackDeLineas(java.util.concurrent.ArrayBlockingQueue<Pack> packs) {
		this.packs = packs;
	}
	
	@Override
	public void run() {
		sortAndStore();
	}
	
	public void endRunning() {
		this.running = false;
	}
	
	public void sortAndStore() {
		Pack pack = null;
//		RESTWorkshop rw = new RESTWorkshop();
		while(running || !packs.isEmpty()) {
			try {
				pack = packs.poll(10, java.util.concurrent.TimeUnit.MILLISECONDS);
				if(pack != null) {
					java.util.Arrays.sort(pack.getPack());
//					java.util.Collections.sort(pack.getPack());
//					java.util.Iterator<String> iter = pack.getPack().iterator();
//					String ln = null;
					try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Data" + pack.getFoil() + ".chunk").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
						for(int a = 0; a<pack.getPack().length; a++) {
							pw.println( pack.getPack()[a] );
						}
//						while(iter.hasNext()) {
//							ln = iter.next();
//							iter.remove();
//							pw.println(ln);
//						}
					}catch(java.io.IOException e) {
						e.printStackTrace();
					}
				}
			}catch(java.lang.InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
}
