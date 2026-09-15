package mx.com.liverpool.p360.services.core.reconciliation;
import java.util.*;
import java.io.*;
import org.bson.Document;
import org.json.*;

public class EntradaUnicaSyncTest {
 static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
 public static void main(String[] args)throws Exception{
  Map<String,String> v=new LinkedHashMap<>();v.put("sku","001234");v.put("ean","0005678");v.put("direction","1");v.put("section","863");v.put("name","Nombre \"especial\"");
  EntradaUnicaSync.Row p=new EntradaUnicaSync.Row(1,"p1",1100,v,Map.of("direction","1 - HOGAR","section","863 - JUGUETES"),Set.of(),Set.of());
  Document mongo=new Document("sku","001234").append("upcEan","0005678").append("address","1 - HOGAR").append("section",new Document("idLevel","863-L3S4H")).append("nameProduct","Nombre \"especial\"");
  check(EntradaUnicaSync.diff(p,mongo).isEmpty(),"Equivalent codes and labels should match");
  mongo.put("sku","1234");check(EntradaUnicaSync.diff(p,mongo).equals(List.of("sku")),"SKU leading zeros are significant");
  JSONObject body=EntradaUnicaSync.payload(p,List.of("sku","ean"),"");
  check(body.getJSONObject("header").getString("SKU").equals("001234"),"SKU must remain a string");
  check(!body.has("variants")&&!body.has("currentStatus"),"Never inject unrelated fields");
  v.put("ean","");body=EntradaUnicaSync.payload(p,List.of("ean"),"");
  check(!body.has("header"),"Blank source must never clear Mongo");
  EntradaUnicaSync.Row a=new EntradaUnicaSync.Row(2,"a1",1000,Map.of("size","CH","colour","Azul","prevStatus","Category","currentStatus","Aprobada"),Map.of(),Set.of("p1"),Set.of());
  body=EntradaUnicaSync.payload(a,List.of("size","colour","prevStatus"),"p1");
  JSONObject variant=body.getJSONArray("variants").getJSONObject(0);
  check(body.getString("proposalId").equals("p1")&&variant.getString("variantId").equals("a1"),"Keep product and variant IDs separate");
  check(variant.getString("TamanoUnico").equals("CH")&&variant.getString("ColoursLiverpoolAtt").equals("Azul"),"Variant field contract");
  check(variant.getString("previousStatus").equals("Category")&&!variant.has("currentStatus"),"Only changed statuses");
  Map<String,JSONObject> grouped=new LinkedHashMap<>();
  EntradaUnicaSync.mergePayload(grouped,body);
  EntradaUnicaSync.mergePayload(grouped,new JSONObject().put("proposalId","p1").put("variants",new JSONArray().put(new JSONObject().put("variantId","a2"))));
  check(grouped.size()==1&&grouped.get("p1").getJSONArray("variants").length()==2,"One product envelope must retain every variant");
  StringWriter csv=new StringWriter();EntradaUnicaSync.csv(csv,"a,b","line1\nline2","a\"b");
  check(csv.toString().equals("\"a,b\",\"line1\nline2\",\"a\"\"b\"\r\n"),"CSV escaping");
  check(EntradaUnicaSync.observed(new Document(),"ean").isEmpty(),"Missing fields are empty, not literal null");
  check(EntradaUnicaSync.code("SB18 - HARD LINES").equals("18"),"S4 direction label normalization");
  System.out.println("PASS: comparison, identifiers, partial payload, blanks, statuses, CSV");
 }
}
