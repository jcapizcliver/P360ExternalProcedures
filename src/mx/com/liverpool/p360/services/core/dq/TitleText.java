package mx.com.liverpool.p360.services.core.dq;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;

/** Pure title rules shared by online processing and the approved-product campaign. */
public final class TitleText {
    private TitleText() {}
    public static boolean mayCalculate(String status) {
        return "1003".equals(status) || "1020".equals(status);
    }
    public static void putProductName(JSONArray records, JSONObject value) {
        for (int i=records.length()-1;i>=0;i--) {
            JSONObject q=records.getJSONObject(i).optJSONObject("_qualification");
            JSONObject c=q==null?null:q.optJSONObject("characteristic");
            if(c!=null && "ProductName".equals(c.optString("_code"))) records.remove(i);
        }
        records.put(value);
    }
    public static String productNameForNative(JSONArray records, String fallback) {
        for (int i=records.length()-1;i>=0;i--) {
            JSONObject r=records.getJSONObject(i),q=r.optJSONObject("_qualification");
            JSONObject c=q==null?null:q.optJSONObject("characteristic");
            if(c!=null && "ProductName".equals(c.optString("_code"))) {
                JSONArray languages=r.optJSONArray("_recordLang");
                if(languages!=null && languages.length()>0) {
                    JSONArray v=languages.getJSONObject(0).optJSONArray("values");
                    if(v!=null && v.length()>0 && v.opt(0) instanceof String && !v.getString(0).trim().isEmpty()) return v.getString(0);
                }
            }
        }
        return fallback;
    }
    public static String withoutBrand(String title, String brand) {
        if (title==null || title.trim().isEmpty()) return "";
        if (brand==null || brand.trim().isEmpty()) return title.trim();
        StringBuilder folded=new StringBuilder();
        List<Integer> starts=new ArrayList<>(), ends=new ArrayList<>();
        for(int i=0;i<title.length();) {
            int end=i+Character.charCount(title.codePointAt(i));
            while(end<title.length() && Character.getType(title.codePointAt(end))==Character.NON_SPACING_MARK)
                end+=Character.charCount(title.codePointAt(end));
            String f=fold(title.substring(i,end));
            for(int j=0;j<f.length();j++){folded.append(f.charAt(j));starts.add(i);ends.add(end);}
            i=end;
        }
        String b=fold(brand.trim());
        if(b.isEmpty()) return title.trim();
        Matcher m=Pattern.compile("(?<![\\p{L}\\p{N}])"+Pattern.quote(b)+"(?![\\p{L}\\p{N}])").matcher(folded);
        if(!m.find()) return title.trim();
        String result=title.substring(0,starts.get(m.start()))+title.substring(ends.get(m.end()-1));
        return result.replaceAll("\\s{2,}"," ").trim();
    }
    private static String fold(String s) {
        return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
