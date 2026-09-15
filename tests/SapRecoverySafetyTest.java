package mx.com.liverpool.p360.services.core.sftp;
import java.util.*;
import org.json.*;
public class SapRecoverySafetyTest {
    static void expect(boolean v,String name){if(!v)throw new AssertionError(name);}
    static JanaDatabaseAudit.Node add(JanaDatabaseAudit.Snapshot db,long rev,int entity,String id,String sku){JanaDatabaseAudit.Node n=new JanaDatabaseAudit.Node();n.revision=rev;n.entity=entity;n.id=id;n.sku=sku;db.revisions.put(rev,n);db.identifiers.put(entity+":"+id,new ArrayList<>(List.of(n)));if(sku!=null)db.skus.computeIfAbsent(entity+":"+sku,k->new ArrayList<>()).add(n);return n;}
    public static void main(String[] args){
        JanaDatabaseAudit.Snapshot db=new JanaDatabaseAudit.Snapshot();
        JanaDatabaseAudit.Node a=add(db,1,1000,"A","123"),p=add(db,2,1100,"P","456");
        Map<Long,Set<String>> types=new HashMap<>();types.put(1L,Set.of("02"));types.put(2L,Set.of("01"));
        JSONObject r=new JSONObject().put("article","A").put("expectedProduct","P").put("sku","123").put("parentSku","456").put("attyp","02");
        expect(SapBatchRecovery.eligible(r,db,types,true).equals("READY"),"missing parent ready");
        a.parents.add("OTHER");expect(SapBatchRecovery.eligible(r,db,types,true).equals("PARENT_ALREADY_PRESENT"),"never overwrite another parent");
        a.parents.clear();a.parents.add("P");expect(SapBatchRecovery.eligible(r,db,types,true).equals("ALREADY_CORRECT"),"idempotent replay");a.parents.clear();
        types.put(1L,Set.of("00"));expect(SapBatchRecovery.eligible(r,db,types,true).equals("TYPE_CHANGED_OR_UNKNOWN"),"type mismatch skipped");types.put(1L,Set.of("02"));
        add(db,3,1100,"OTHER_PRODUCT","456");expect(SapBatchRecovery.eligible(r,db,types,true).equals("AMBIGUOUS_PRODUCT_SKU"),"duplicate product SKU skipped");
        JSONObject s=new JSONObject().put("identifier","A").put("entity","Article").put("sku","789").put("expectedType","02");
        expect(SapBatchRecovery.eligible(s,db,types,false).equals("EXISTING_SKU_PRESERVED"),"existing SKU preserved");a.sku=null;
        expect(SapBatchRecovery.eligible(s,db,types,false).equals("READY"),"empty SKU eligible");add(db,4,1000,"OTHER_ARTICLE","789");
        expect(SapBatchRecovery.eligible(s,db,types,false).equals("SKU_ALREADY_ASSIGNED_ELSEWHERE"),"SKU uniqueness protected");
        db.skus.remove("1000:789");types.remove(1L);s.put("incomingId","A").put("attyp","02");
        expect(SapBatchRecovery.eligible(s,db,types,false).equals("READY"),"explicit XML ID tolerates missing type for SKU only");
        s.put("incomingId","DIFFERENT");expect(SapBatchRecovery.eligible(s,db,types,false).equals("TYPE_CHANGED_OR_UNKNOWN"),"missing type cannot use inferred target");
        s.put("incomingId","A");types.put(1L,Set.of("00"));expect(SapBatchRecovery.eligible(s,db,types,false).equals("TYPE_CHANGED_OR_UNKNOWN"),"explicit ID never overrides conflicting type");
        List<JSONObject> rows=new ArrayList<>();for(int i=0;i<900;i++)rows.add(new JSONObject(r.toString()).put("article","A"+i));
        JSONObject body=SapBatchRecovery.request(rows,true,"Article");expect(body.getJSONArray("rows").length()==900,"900 rows");
        JSONObject first=body.getJSONArray("rows").getJSONObject(0);expect(first.getJSONObject("qualification").getString("referencedSupplierAid").equals("P")&&first.getJSONArray("values").getString(0).equals("P"),"qualification and value agree");
        System.out.println("PASS 13 safety checks");
    }
}
