package mx.com.liverpool.p360.services.core.temp.csv.forreports;

public class OrdenaLaDataYJuntala {

	
	public static void main(String[] args) {
		Thread t1 = new Thread(()-> TeLoOrdeno.main(new String[] { "C:\\opt\\LVP\\desorden\\PROD\\MD_MainProductArticleData.csv", "mdpad_" }) );
		Thread t2 = new Thread(()-> TeLoOrdeno.main(new String[] { "C:\\opt\\LVP\\desorden\\PROD\\MD_CharacteristicsData.csv", "mdcd_" }) );
		t1.start();
		t2.start();
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
