package mx.com.liverpool.dataprofiling;

public class AVerSiDeveras {

	
	public static void main(String[] args) {
		String pl = null;
		String ln = null;
		long cnt = 0;
		int cmp = 0;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "BQ", "tmp", "Final.dat").toFile())))){
			while((ln = br.readLine()) != null) {
				cnt++;
				if(pl != null) {
					cmp = pl.compareTo(ln);
					if(cmp > 0) {
						System.out.println("Pito:\n" + pl + "\n" + ln);
						System.out.println(cnt);
						System.exit(0);
					}else if(cmp == 0) {
						System.out.println("---->" + pl);
					}
				}
				pl = ln;
				if(cnt % 1000000 == 0) {
					System.out.print(".");
					if(cnt % 10000000 == 0) {
						System.out.println(cnt);
					}
				}
			}
			System.out.println("Survived... " + cnt);
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
}
