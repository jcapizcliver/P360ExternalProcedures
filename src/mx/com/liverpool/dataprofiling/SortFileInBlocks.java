package mx.com.liverpool.dataprofiling;

public class SortFileInBlocks implements java.lang.Runnable {
	
	private final java.util.concurrent.ArrayBlockingQueue<String> lasLineas;
	private final String header;
	private boolean running = true;
	
	private final java.util.LinkedList<java.lang.Long> foils = new java.util.LinkedList<>();
	
	public SortFileInBlocks(String header, java.util.concurrent.ArrayBlockingQueue<String> lasLineas) {
		this.header = header;
		this.lasLineas = lasLineas;
	}
	
	public void endRunning() {
		this.running = false;
	}
	
	public java.util.LinkedList<java.lang.Long> getFoils(){
		return foils;
	}
	
	public void mergeFiles() {
		java.util.LinkedList<BRWrapper> readers = new java.util.LinkedList<>();
		java.nio.file.Path path = null;
		for(java.lang.Long foil : foils) {
			try {
				path = java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Data" + foil + ".chunk");
				readers.addLast(new BRWrapper(path, new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(path.toFile()), java.nio.charset.StandardCharsets.UTF_8))));
			}catch(java.io.FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		java.util.LinkedList<java.util.Map.Entry<String, BRWrapper>> values = new java.util.LinkedList<>();
		String ln = null;
		for(BRWrapper entry : readers) {
			try{
				ln = entry.getBr().readLine();
				if(ln != null) {
					values.addLast(new java.util.AbstractMap.SimpleEntry<>(ln, entry));
				}
			}catch(java.io.IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Working with: " + values.size() + " open files...");
		java.util.Collections.sort(values, (o1,o2) -> o1.getKey().compareTo(o2.getKey()) );
		java.util.Map.Entry<String, BRWrapper> entry = values.removeFirst();
		int cmp = 0;
		long a = 0;
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "OtroFinalSorted.dat").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			pw.println(header);
			while(!values.isEmpty()) {
				ln = entry.getKey();
				pw.println( ln );
				a++;
				if(a % 100000 == 0) {
					System.out.print(".");
					if(a % 1000000 == 0) {
						System.out.println(a);
					}
				}
				try{
					ln = entry.getValue().getBr().readLine();
					if(ln != null) {
						if(!values.isEmpty()) {
							cmp = ln.compareTo( values.getFirst().getKey() );
							if( cmp <= 0 ) {
								values.addFirst(new java.util.AbstractMap.SimpleEntry<>(ln, entry.getValue()));
							} else if( cmp > 0 && ln.compareTo( values.getLast().getKey() ) >= 0 ) {
								values.addLast(new java.util.AbstractMap.SimpleEntry<>(ln, entry.getValue()));
							} else {
								values.addFirst(new java.util.AbstractMap.SimpleEntry<>(ln, entry.getValue()));
								java.util.Collections.sort(values, (o1,o2) -> o1.getKey().compareTo(o2.getKey()) );
							}
						}else {
							values.addFirst(entry);
						}
					}else {
						entry.getValue().getBr().close();
						try {
							java.nio.file.Files.delete(entry.getValue().getPath());
						}catch(java.io.IOException e) {
							e.printStackTrace();
						}
					}
				}catch(java.io.IOException e) {
				}
				if(!values.isEmpty()) {
					entry = values.removeFirst();
				}
			}
			if(java.nio.file.Files.exists(entry.getValue().getPath())) {
				while((ln = entry.getValue().getBr().readLine()) != null) {
					pw.println( ln );
					a++;
					if(a % 100000 == 0) {
						System.out.print(".");
						if(a % 1000000 == 0) {
							System.out.println(a);
						}
					}
				}
				entry.getValue().getBr().close();
				try {
					java.nio.file.Files.delete(entry.getValue().getPath());
				}catch(java.io.IOException e) {
					e.printStackTrace();
				}
			}
			System.out.println(a);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		int avp = java.lang.Runtime.getRuntime().availableProcessors() - 2;
		java.util.concurrent.ArrayBlockingQueue<Pack> packs = new java.util.concurrent.ArrayBlockingQueue<>(avp);
		PackDeLineas[] workers = new PackDeLineas[avp];
		Thread[] threads = new Thread[avp];
		for(int i=0; i<avp; i++) {
			workers[i] = new PackDeLineas(packs);
			threads[i] = new Thread(workers[i]);
			threads[i].setPriority(Thread.currentThread().getPriority() - 1);
			threads[i].setDaemon(false);
			threads[i].start();
		}
		int bs = 10000000;
		String line = null;
//		java.util.LinkedList<String> pk = new java.util.LinkedList<>();
		String[] pk = new String[bs];
		int a = 0;
		long packCount = 0;
		while(running || !lasLineas.isEmpty()) {
			try {
				line = lasLineas.poll(10, java.util.concurrent.TimeUnit.MICROSECONDS);
				if(line != null) {
					pk[a] = line;
					a++;
					if(a == bs) {
						packCount++;
						foils.addLast(packCount);
						System.out.println("Now serving chunk... " + (packCount));
						packs.put(new Pack(packCount, pk));
						pk = new String[bs];
						a = 0;
					}
				}
			}catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
		if(a > 0) {
			String[] help = new String[a];
			for(int m=0; m<a; m++) {
				help[m] = pk[m];
			}
//		if(!pk.isEmpty()) {
			packCount++;
			foils.addLast(packCount);
			packs.add(new Pack(packCount, help));
		}
		for(int i=0; i<avp; i++) {
			workers[i].endRunning();
		}
		PackDeLineas p = new PackDeLineas(packs);
		p.endRunning();
		p.run();
		for(int i=0; i<avp; i++) {
			try {
				threads[i].join();
			}catch(java.lang.InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("Now merging files...");
		mergeFiles();
	}
	
	
}
