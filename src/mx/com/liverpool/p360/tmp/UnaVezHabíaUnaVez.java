package mx.com.liverpool.p360.tmp;

import java.io.IOException;

import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class UnaVezHabíaUnaVez {

	
	public static void main(String[] args) {
//		RESTWorkshop rw = new RESTWorkshop();
//		rw.setBaseUrl("https://pks.raenonx.cc/en/sleepdex/lookup");
//		rw.getRc().getHeader().remove("Authorization");
//		rw.makeRequest("GET", "");
//		System.out.println(rw.getRawResponse());
		UnaVezHabíaUnaVez u = new UnaVezHabíaUnaVez();
		try {
			u.elese();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void elese() throws java.io.IOException {
		// 1. Descargar la página
        String url = "https://pks.raenonx.cc/en/sleepdex/lookup";
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .get();

        // 2. Buscar todos los <script>
//        Elements scripts = doc.getElementsByTag("script");

//        // 3. Buscar self.__next_f.push(...)
//        Pattern pattern = Pattern.compile("self\\.__next_f\\.push\\((\\{.*?\\})\\)", Pattern.DOTALL);

        for (Element script : doc.select("script")) {
            String html = script.html();
//            Matcher matcher = pattern.matcher(html);

//            while (matcher.find()) {
                String content = script.html();

                if(content.contains("self.__next_f.push"))
                try {
                	System.out.println(content);
//                    JSONObject json = new JSONObject(jsonText);
//                    System.out.println("✔ Objeto JSON capturado:");
//                    System.out.println(json.toString(2)); // Pretty print
                } catch (JSONException e) {
                    System.out.println("❌ Error al parsear JSON: " + e.getMessage());
                }
//            }
        }
	}
	
}
