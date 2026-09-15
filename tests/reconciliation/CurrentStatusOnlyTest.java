package mx.com.liverpool.p360.services.core.reconciliation;
import java.util.*;import org.bson.Document;import org.json.*;
public class CurrentStatusOnlyTest {
 public static void main(String[]args){
 if(!EntradaUnicaSync.STATUS_ONLY)throw new AssertionError();
 var r=new EntradaUnicaSync.Row(1,"P",1100,Map.of("currentStatus","Aprobada","sku","old"),Map.of(),Set.of(),Set.of());
 var d=new Document("status",new Document("internal","Aprobada")).append("sku","different");
 if(!EntradaUnicaSync.diff(r,d).isEmpty())throw new AssertionError("Other fields compared");
 d.put("status",new Document("internal","Pendiente"));if(!EntradaUnicaSync.diff(r,d).equals(List.of("currentStatus")))throw new AssertionError();
 JSONObject p=EntradaUnicaSync.payload(r,List.of("currentStatus"),"");if(p.length()!=2||!p.getString("currentStatus").equals("Aprobada"))throw new AssertionError();
 try{EntradaUnicaSync.payload(r,List.of("sku"),"");throw new AssertionError();}catch(IllegalArgumentException ok){}
 var a=new EntradaUnicaSync.Row(2,"A",1000,Map.of("currentStatus","Aprobada"),Map.of(),Set.of("P"),Set.of());
 JSONObject v=EntradaUnicaSync.payload(a,List.of("currentStatus"),"P").getJSONArray("variants").getJSONObject(0);if(v.length()!=2||!v.getString("currentStatus").equals("Aprobada"))throw new AssertionError();
 System.out.println("PASS status-only diff/payload guard, labels, product and variant contracts");
 }
}
