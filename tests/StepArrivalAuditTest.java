package mx.com.liverpool.p360.services.core.temp.xml.local.neostream;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.json.JSONObject;
public class StepArrivalAuditTest {
 public static void main(String[] args) throws Exception {
  Path dir=Files.createTempDirectory("neo-audit-test-");
  Instant t=Instant.parse("2026-09-08T23:00:00Z");
  String special="nuevo\"ñ\n";
  Path first=StepArrivalAudit.writeSnapshot(dir,Path.of("origen ñ.xml"),t,t,
   List.of("same",special),Map.of(special,"123"),Map.of("same","42@1"),
   List.of("same"),Map.of(),Map.of());
  var rows=Files.readAllLines(first);
  check(rows.size()==4,"one line per identifier plus summary");
  var p=new JSONObject(rows.get(0));var missing=new JSONObject(rows.get(1));var article=new JSONObject(rows.get(2));
  check(p.getBoolean("existedBeforeProcessing") && p.getString("sku").isEmpty(),"existing product without SKU");
  check(!missing.getBoolean("existedBeforeProcessing") && missing.getString("identifier").equals(special),"missing identifier and escaping");
  check(!article.getBoolean("existedBeforeProcessing") && article.getString("entity").equals("Article"),"entity isolation");
  var summary=new JSONObject(rows.get(3));check(summary.getInt("productsExisting")==1 && summary.getInt("articlesExisting")==0,"counts");
  Path retry=StepArrivalAudit.writeSnapshot(dir,Path.of("origen ñ.xml"),t,t,
   List.of("same"),Map.of(),Map.of(),List.of(),Map.of(),Map.of());
  check(!retry.equals(first) && Files.readAllLines(first).equals(rows),"retry preserves first audit");
  Path blocked=dir.resolve("not-directory");Files.writeString(blocked,"x");
  boolean failed=false;
  try {StepArrivalAudit.writeSnapshot(blocked,Path.of("x"),t,t,List.of(),Map.of(),Map.of(),List.of(),Map.of(),Map.of());}
  catch(java.io.IOException expected){failed=true;}
  check(failed,"write failure propagated before processing");
  try(var stream=Files.list(dir)){check(stream.noneMatch(pth->pth.toString().endsWith(".partial")),"complete reports published");}
  System.out.println("8 checks passed; fixtures="+dir);
 }
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);}
}