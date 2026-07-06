package mx.com.liverpool.p360.tmp;

import mx.com.liverpool.p360.services.core.RESTWorkshop;

public class AnalisisSKUsBCC {
	
	public static final RESTWorkshop rw = new RESTWorkshop();
	
	public static void main(String[] args) {
		java.util.LinkedList<Long> p360Skus = recolectaEseCaUs();
		java.util.Collections.sort(p360Skus, (o1,o2)-> o1.compareTo(o2) );
		p360Skus.forEach(System.out::println);
		Long last = p360Skus.getLast();
//		last = last < 1033591779 ? 1033591779 : last;
		System.out.println("Tenemos: " + p360Skus.size());
		java.util.LinkedList<java.util.Map.Entry<Long, java.util.Date>> lalista = new java.util.LinkedList<>();
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MMM-yy hh.mm.ss.SSS a");
		int cnt = 1;
		int badNumberFormat = 0;
		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream("C:\\opt\\LVP\\desorden\\QA2_skus.txt")))){
			String delim = "\"";
			String sep = "\t";
			String esc = "";
			String[] header = null;
			String[] pieces = null;
			String line = null;
			header = rw.parseLine(br.readLine(), delim, sep, esc);
			String prev = null;
			StringBuilder sb = new StringBuilder();
//			java.util.Arrays.asList(rw.parseLine(br.readLine(), delim, dep, esc)).forEach(System.out::println);
			int refNumber = header.length;
			while((line = br.readLine()) != null) {
				cnt++;
				try{
					pieces = rw.parseLine(sb.length() == 0 ? line : sb.toString() + line, delim, sep, esc);
				}catch(IllegalStateException e) {
					if(e.getMessage().contains("missing enclosing character")) {
						sb.append(line);
						continue;
					}
					System.out.println(cnt);
					System.out.println(prev);
					System.out.println(line);
					e.printStackTrace();
					System.exit(0);
				}
				sb.setLength(0);
				try{
					lalista.addLast(new java.util.AbstractMap.SimpleEntry<>(Long.parseLong (pieces[1]), sdf.parse (pieces[3])));
				}catch(java.text.ParseException e) {
					e.printStackTrace();
				}catch(NumberFormatException e) {
					badNumberFormat++;
				}
				prev = line;
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
		System.out.println(cnt + " lines read.");
		System.out.println("Done reading... " + badNumberFormat + " bad number formats...");
		java.util.Collections.sort(lalista, (o1,o2)-> o1.getKey().compareTo(o2.getKey()));
		System.out.println("Done sorting...");
		Long curr = null;
		Long prev = null;
		int a = 0;
		int b = 0;
		System.out.println("Printing.... ***********");
		try(java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "SecuenciasDEVSKUS.csv").toFile()), java.nio.charset.StandardCharsets.UTF_8))){
			for(java.util.Map.Entry<Long, java.util.Date> ent : lalista) {
				if(ent.getKey().compareTo(1033815448L) > 0) {
	//				if(ent.getKey().compareTo(1033591779L) > 0) {
					curr = ent.getKey();
					if(prev != null && curr.compareTo(prev) != 1l) {
						System.out.println("A ver este: " + prev + " - " + curr + " (" + (curr - prev) + ")");
					}
					if(prev != null && curr.compareTo(prev) > 0) {
						pw.println(ent.getKey() /*+ " - " + sdf.format(ent.getValue())*/);
						a++;
						if(a == 2000) {
							break;
						}
					}
					prev = curr;
				}else if(ent.getKey().compareTo(last) == 0) {
					System.out.println("THIS ONE IGNORE: " + ent.getKey() + " - " + sdf.format(ent.getValue()));
				}
			}
		}catch(java.io.IOException e) {
			e.printStackTrace();
		}
	}
	
	public static java.util.LinkedList<Long> recolectaEseCaUs() {
		java.util.Map<String, String> qp = new java.util.TreeMap<>();
		qp.put("fields", "SimpleProduct2GCharacteristicValueLang.Value('SKU',-1)");
		qp.put("pageSize", "1200");
		qp.put("query", "( characteristic('Business',-1) = 'LVP'@'BusinessQualified' or characteristic('Business',-1) = 'MKP'@'BusinessQualified' ) and not characteristic('SKU',-1) is empty");
		int currentIndex = 0;
		int totalSize = 0;
		org.json.JSONObject response = null;
		org.json.JSONArray rows = null;
		org.json.JSONArray values = null;
		java.util.Set<Long> skus = new java.util.TreeSet<>();
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Product2G/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					if(!"1234567890".equals(values.getJSONArray(0).getString(0)))
						skus.add(Long.parseLong(values.getJSONArray(0).getString(0)));
				}
			}else{
				System.out.println("ERR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		qp.put("fields", "SimpleArticleCharacteristicValueLang.Value('SKU',-1)");
		do {
			qp.put("startIndex", String.valueOf(currentIndex));
			response = rw.makeRequest("GET", "/list/Article/bySearch", qp, null);
			if(response != null) {
				totalSize = response.getInt("totalSize");
				rows = response.getJSONArray("rows");
				for(int i=0; i<rows.length(); i++) {
					currentIndex++;
					values = rows.getJSONObject(i).getJSONArray("values");
					skus.add(Long.parseLong(values.getJSONArray(0).getString(0)));
				}
			}else{
				System.out.println("ERR: " + rw.getRawResponse());
			}
		}while(currentIndex < totalSize);
		currentIndex = 0;
		return new java.util.LinkedList<>(skus);
	}
}
