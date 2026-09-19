import mx.com.liverpool.p360.services.core.reconciliation.MergeStatusPolicy;
import java.util.*;
import org.json.*;

public final class MergeStatusPolicyTest {
 static int cases;
 static JSONObject state(int key,String label,String date) {
  JSONObject d=new JSONObject().put("currentStatus",new JSONObject().put("_key",key).put("_label",label));
  if(date!=null)d.put("statusModification","El usuario rest ha establecido el estado \""+label+"\" el "+date+".");
  return d;
 }
 static JSONObject choose(JSONObject... states) {
  JSONObject result=new JSONObject();List<String> ids=new ArrayList<>();for(int i=0;i<states.length;i++)ids.add("source-"+i);
  MergeStatusPolicy.apply(result,Arrays.asList(states),ids,new JSONArray());return result;
 }
 static void expect(int expected,String name,JSONObject... states) {
  String actual=MergeStatusPolicy.key(choose(states).opt("currentStatus"));
  if(!Integer.toString(expected).equals(actual))throw new AssertionError(name+": "+actual);cases++;
 }
 static void check(boolean yes,String name){if(!yes)throw new AssertionError(name);cases++;}
 public static void main(String[] args) {
  JSONObject approved=state(1007,"Aprobada","15/09/2026 1:26");
  JSONObject pending=state(1002,"Pendiente Inicio Enriquecimiento","17/09/2026 10:00");
  expect(1007,"approval beats a recently edited initial state",pending,approved);
  expect(1007,"approval does not regress with reverse source order",approved,pending);
  expect(1007,"no history is needed for unambiguous progress",state(1002,"Pendiente Inicio Enriquecimiento",null),state(1007,"Aprobada",null));
  JSONObject modified=state(1008,"Modificación","16/09/2026 9:00");
  expect(1008,"later modification wins",approved,modified);
  expect(1008,"same result in reverse order",modified,approved);
  expect(1007,"later approval closes modification",state(1008,"Modificación","14/09/2026 9:00"),approved);
  expect(1007,"ordinary modification date cannot move a rejection forward",approved,state(1028,"Rechazo QA","14/09/2026 9:00").put("modificationTimestamp","2026-09-17T10:00:00"));
  expect(1028,"later rejection wins",approved,state(1028,"Rechazo QA","16/09/2026 9:00"));
  expect(1022,"new cycle QA survives older approval",approved,modified,state(1022,"Revisión QA","17/09/2026 9:00"));
  expect(1028,"equal minute cannot prove approval came after rejection",approved,state(1028,"Rechazo QA","15/09/2026 1:26"));
  expect(1008,"unknown rework time cannot trigger automatic approval",approved,state(1008,"Modificación",null));
  expect(1008,"unknown approval time cannot override dated rework",state(1007,"Aprobada",null),modified);
  expect(1007,"inactive donor does not retire active result",approved,state(1025,"Eliminada","17/09/2026 9:00"));
  expect(1007,"active donor wins when base canceled",state(1009,"Cancelado","17/09/2026 9:00"),approved);
  expect(1023,"category is further than QA",state(1022,"Revisión QA",null),state(1023,"Category",null));
  expect(1026,"parallel enrichment stages use status chronology",state(1004,"Carga de Imagen","15/09/2026 9:00"),state(1026,"En Proceso Foro","16/09/2026 9:00"));
  JSONObject english=state(1008,"Modified",null).put("statusModification","The user rest set status \"Modified\" on 9/16/2026 9:00 AM.");
  expect(1008,"US and Spanish histories are comparable",approved,english);
  JSONObject mixed=state(1008,"Modificación",null).put("statusModification","El usuario rest ha establecido el estado \"Modified\" el 16/09/2026 9:00.");
  expect(1008,"mixed label language uses the enum key",approved,mixed);
  check(MergeStatusPolicy.statusTime(approved).toString().equals("2026-09-15T01:26"),"actual production history format");
  JSONObject stale=state(1008,"Modificación","16/09/2026 9:00");
  stale.put("statusModification",stale.getString("statusModification")+"\nEl usuario rest ha establecido el estado \"Aprobada\" el 17/09/2026 9:00.");
  check(MergeStatusPolicy.statusTime(stale)==null,"history must agree with current state");
  check(MergeStatusPolicy.statusTime(state(1007,"Aprobada","31/02/2026 9:00"))==null,"invalid dates are not normalized");
  check(MergeStatusPolicy.statusTime(state(1008,"Modified",null).put("statusModification","The user set status \"Modified\" on 9/10/2026 9:00."))==null,"ambiguous English date not guessed");
  JSONObject paired=state(1008,"Modificación","16/09/2026 9:00").put("previousStatus",new JSONObject().put("_key",1007)).put("externalStatus","EnProcesoProveedor");
  JSONObject result=choose(approved,paired);
  check(result.getJSONObject("previousStatus").getInt("_key")==1007&&result.getString("externalStatus").equals("EnProcesoProveedor"),"related workflow fields stay with chosen state");
  check(!result.has("statusModification"),"never writes someone else's history");
  JSONObject rankUnknown=state(7777,"Unknown",null);JSONArray audit=new JSONArray();
  MergeStatusPolicy.apply(new JSONObject(),List.of(rankUnknown,approved),List.of("base","donor"),audit);
  check(audit.getJSONObject(0).getJSONArray("warnings").length()>0,"unknown states have explicit evidence");
  expect(7777,"unknown stage keeps existing state and does not block other data",rankUnknown,approved);
  check("1007".equals(MergeStatusPolicy.key(new JSONObject().put("_key",1007).put("_label","Modificación"))),"key is authoritative");
  JSONObject scalar=new JSONObject().put("currentStatus","07");expect(1007,"supports status codes",pending,scalar);
  // Non-transitive pairwise selection would fail here. All group candidates are evaluated together.
  List<JSONObject> group=List.of(approved,modified,state(1022,"Revisión QA","17/09/2026 9:00"));
  for(int a=0;a<3;a++)for(int b=0;b<3;b++)if(a!=b)expect(1022,"group permutation",group.get(a),group.get(b),group.get(3-a-b));
  JSONObject immutable=new JSONObject(paired.toString());choose(approved,paired).getJSONObject("currentStatus").put("_label","changed");
  check(immutable.toString().equals(paired.toString()),"selection never mutates donor data");
  System.out.println("MERGE_STATUS_POLICY_PASS cases="+cases);
 }
}
