package mx.com.liverpool.p360.services.core.temp.pvia;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.SimpleDelimitedFileParser;
import mx.com.liverpool.p360.services.core.net.DataRequestor;

public class RefreshEANRels {

	private static final RESTWorkshop rw = new RESTWorkshop();
	
	public static void main(String[] args) {
		DataRequestor dr = new DataRequestor();
		org.json.JSONArray items = new org.json.JSONArray();
		int[] cnt = new int[] {0};
		SimpleDelimitedFileParser fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
			if(arr.length > 0) {
				String[] apples = rw.parseLine(arr[1], "\"", ";", "\\");
				String[] arr2 = new String[16];
				int m = Integer.min(apples.length, arr2.length);
				for(int i=0; i<m; i++) {
					arr2[i] = apples[i];
				}
				for(int j=m; j<arr2.length; j++) {
					arr2[j] = "";
				}
				if(!"".equals(arr2[14])) {
					items.put(arr2[14]);
				}else if(!"".equals(arr2[15])) {
					items.put(arr2[15]);
				}
				if(items.length() == 5000) {

					dr.retiraEANProductNo(items);
					while(items.length() > 0) {
						items.remove(0);
					}
				}
			}
			cnt[0]++;
			if(cnt[0] % 10000 == 0) {
				System.out.print(".");
				if(cnt[0] % 1000000 == 0) {
					System.out.println(cnt[0]);
				}
			}
		} );
		fileParser.parse(java.nio.file.Paths.get(args[0]));
		if(items.length() > 0) {
			dr.retiraEANProductNo(items);
			while(items.length() > 0) {
				items.remove(0);
			}
			System.out.println(cnt[0]);
		}
		System.out.println("Done with products.");
		fileParser = new SimpleDelimitedFileParser('"',',','\\',"\n", java.nio.charset.StandardCharsets.UTF_8, arr -> {
			if(arr.length > 0) {
				String[] apples = rw.parseLine(arr[1], "\"", ";", "\\");
				String[] arr2 = new String[8];
				int m = Integer.min(apples.length, arr2.length);
				for(int i=0; i<m; i++) {
					arr2[i] = apples[i];
				}
				for(int j=m; j<arr2.length; j++) {
					arr2[j] = "";
				}
				if(!"".equals(arr2[5])) {
					items.put(arr2[5]);
				}else if(!"".equals(arr2[6])) {
					items.put(arr2[6]);
				}
				if(items.length() == 5000) {
					dr.retiraEANSupplierAID(items);
					while(items.length() > 0) {
						items.remove(0);
					}
				}
			}
			cnt[0]++;
			if(cnt[0] % 10000 == 0) {
				System.out.print(".");
				if(cnt[0] % 1000000 == 0) {
					System.out.println(cnt[0]);
				}
			}
		});
		fileParser.parse(java.nio.file.Paths.get(args[1]));
		if(items.length() > 0) {
			System.out.println("\n\tSending...");
			dr.retiraEANSupplierAID(items);
			while(items.length() > 0) {
				items.remove(0);
			}
		}
		System.out.println(cnt[0]);
		System.out.println("Now refreshing...");
		LoadProductDataForAdmin.main(new String[] { args[2], args[3] });
	}
	
}
