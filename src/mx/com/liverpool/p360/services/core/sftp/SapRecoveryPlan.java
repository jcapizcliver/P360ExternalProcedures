package mx.com.liverpool.p360.services.core.sftp;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.json.*;

/** Plans missing SKUs and relations from immutable latest XML rows and existing P360 entities. */
public final class SapRecoveryPlan {
    static String fallback(String origin,String sku){return (origin.equals("ECC")?"LVP":"SBB")+sku;}
    static JanaDatabaseAudit.Node unique(List<JanaDatabaseAudit.Node> list){if(list!=null&&list.size()>1)throw new IllegalStateException("AMBIGUOUS_SKU_OR_ID");return list==null||list.isEmpty()?null:list.get(0);}
    static JanaDatabaseAudit.Node byId(JanaDatabaseAudit.Snapshot db,int e,String id){return id.isEmpty()?null:unique(db.identifiers.get(e+":"+id));}
    static JanaDatabaseAudit.Node bySku(JanaDatabaseAudit.Snapshot db,int e,String sku){return unique(db.skus.get(e+":"+sku));}
    static boolean individual(String t){return t.equals("00")||t.equals("10");}
    static boolean compatible(JanaDatabaseAudit.Node n,String sku){return n==null||n.sku==null||n.sku.isBlank()||sku.equals(n.sku);}
    static void addSku(Map<String,JSONObject> skuRows,Set<String> conflicts,JanaDatabaseAudit.Snapshot db,JSONObject r,JanaDatabaseAudit.Node n,String expectedType){
        if(n==null||n.sku!=null&&!n.sku.isBlank())return;
        String sku=r.getString("sku"),key=n.entity+":"+n.id;
        if(db.skus.containsKey(n.entity+":"+sku))return;
        JSONObject candidate=new JSONObject(r.toString()).put("entity",n.entity==1000?"Article":"Product2G").put("identifier",n.id).put("expectedType",expectedType);
        JSONObject old=skuRows.putIfAbsent(key,candidate);if(old!=null&&!old.getString("sku").equals(sku))conflicts.add(key);
    }
    static void plan(Path input,String origin,Path output)throws Exception{
        if(!Set.of("ECC","S4H").contains(origin))throw new IllegalArgumentException("ECC|S4H");
        Files.createDirectories(output);if(Files.exists(output.resolve("complete.json")))throw new IOException("Use a new output directory");
        Set<String> ids=new HashSet<>(),skus=new HashSet<>();Map<String,String> productXmlIds=new HashMap<>();int records=0;
        try(BufferedReader b=Files.newBufferedReader(input)){String line;while((line=b.readLine())!=null){JSONObject r=new JSONObject(line);String sku=r.getString("sku"),id=r.getString("incomingId"),type=r.getString("attyp");records++;
            if(!sku.matches("[0-9]+")||!Set.of("00","10","01","02").contains(type))continue;
            ids.add(fallback(origin,sku));if(!id.isEmpty())ids.add(id);skus.add(sku);String ps=r.optString("parentSku");if(ps.matches("[0-9]+")){skus.add(ps);ids.add(fallback(origin,ps));}
            if(type.equals("01")||individual(type))productXmlIds.put(sku,id.isEmpty()?fallback(origin,sku):id);
        }}
        JanaDatabaseAudit.Snapshot db=new JanaDatabaseAudit.Snapshot();
        try(Connection c=SapBatchRecovery.connect()){
            c.setAutoCommit(false);try(Statement s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
            JanaDatabaseAudit.entities(c,db,skus,true);JanaDatabaseAudit.entities(c,db,ids,false);JanaDatabaseAudit.loadChildren(c,db,ids);
            Set<String> parents=JanaDatabaseAudit.parents(c,db);parents.removeIf(id->db.identifiers.containsKey("1100:"+id));JanaDatabaseAudit.entities(c,db,parents,false);c.rollback();
        }
        Map<String,JSONObject> skuRows=new TreeMap<>(),relations=new TreeMap<>();Set<String> skuConflicts=new HashSet<>(),relationConflicts=new HashSet<>();Map<String,Integer> counts=new TreeMap<>();
        try(BufferedReader b=Files.newBufferedReader(input);BufferedWriter skipped=Files.newBufferedWriter(output.resolve("review.jsonl"))){String line;while((line=b.readLine())!=null){JSONObject r=new JSONObject(line).put("origin",origin);String sku=r.getString("sku"),id=r.getString("incomingId"),type=r.getString("attyp");
            try{
                if(!sku.matches("[0-9]+")||!Set.of("00","10","01","02").contains(type))throw new IllegalStateException("UNKNOWN_TYPE_OR_SKU");
                JanaDatabaseAudit.Node a=null,p=null;
                if(type.equals("01")){
                    p=byId(db,1100,id);if(p==null)p=bySku(db,1100,sku);if(p==null)p=byId(db,1100,fallback(origin,sku));
                }else{
                    a=byId(db,1000,id);if(a==null)a=bySku(db,1000,sku);if(a==null)a=byId(db,1000,fallback(origin,sku));
                    if(individual(type)){
                        if(a==null){p=byId(db,1100,id);if(p==null)p=bySku(db,1100,sku);if(p==null)p=byId(db,1100,fallback(origin,sku));if(p!=null)a=unique(db.children.get(p.id));}
                        if(a!=null&&!a.parents.isEmpty()){if(a.parents.size()!=1)throw new IllegalStateException("MULTIPLE_PARENTS");p=byId(db,1100,a.parents.iterator().next());}
                        if(p==null)p=bySku(db,1100,sku);
                        if(p==null)p=byId(db,1100,id);
                        if(p==null&&a!=null)p=byId(db,1100,a.id);
                        if(p==null)p=byId(db,1100,fallback(origin,sku));
                    }else{
                        String ps=r.optString("parentSku");if(ps.isEmpty())throw new IllegalStateException("MISSING_PARENT_SKU");
                        p=bySku(db,1100,ps);if(p==null)p=byId(db,1100,productXmlIds.getOrDefault(ps,""));if(p==null)p=byId(db,1100,fallback(origin,ps));
                        if(!compatible(p,ps))throw new IllegalStateException("PARENT_SKU_CONFLICT");
                    }
                }
                if(!compatible(a,sku)||(!type.equals("02")&&!compatible(p,sku)))throw new IllegalStateException("EXISTING_SKU_CONFLICT");
                // Stage additions only after resolving the entire row without contradictions.
                if(a!=null)addSku(skuRows,skuConflicts,db,r,a,type);
                if(p!=null&&!type.equals("02"))addSku(skuRows,skuConflicts,db,r,p,type);
                String status;
                if(type.equals("01"))status=p==null?"MISSING_PRODUCT":"GENERIC";
                else if(a==null)status="MISSING_ARTICLE";
                else if(p==null)status="MISSING_PRODUCT";
                else if(a.parents.isEmpty()){
                    JSONObject candidate=new JSONObject(r.toString()).put("article",a.id).put("expectedProduct",p.id).put("status","MISSING_REFERENCE");
                    JSONObject old=relations.putIfAbsent(a.id,candidate);if(old!=null&&!old.getString("expectedProduct").equals(p.id))relationConflicts.add(a.id);status="MISSING_REFERENCE";
                }else status=a.parents.size()==1&&a.parents.contains(p.id)?"OK":"PARENT_ALREADY_PRESENT";
                counts.merge(status,1,Integer::sum);
                if(!Set.of("OK","GENERIC","MISSING_REFERENCE").contains(status)){skipped.write(r.put("review",status).toString());skipped.newLine();}
            }catch(IllegalStateException e){counts.merge(e.getMessage(),1,Integer::sum);skipped.write(r.put("review",e.getMessage()).toString());skipped.newLine();}
        }}
        // A SKU may not be proposed for two different missing targets in the same entity.
        Map<String,String> skuOwners=new HashMap<>();
        for(Map.Entry<String,JSONObject> e:skuRows.entrySet()){JSONObject r=e.getValue();String k=r.getString("entity")+":"+r.getString("sku"),old=skuOwners.putIfAbsent(k,e.getKey());if(old!=null&&!old.equals(e.getKey())){skuConflicts.add(old);skuConflicts.add(e.getKey());}}
        for(String k:skuConflicts)skuRows.remove(k);for(String k:relationConflicts)relations.remove(k);
        write(output.resolve("skus.jsonl"),skuRows.values());write(output.resolve("relations.jsonl"),relations.values());
        JSONObject summary=new JSONObject(counts).put("records",records).put("skuCandidates",skuRows.size()).put("relationCandidates",relations.size()).put("skuConflicts",new JSONArray(skuConflicts)).put("relationConflicts",new JSONArray(relationConflicts));
        Files.writeString(output.resolve("complete.json"),summary.toString(2));System.out.println("PLAN COMPLETE records="+records+" SKUs="+skuRows.size()+" relations="+relations.size());
    }
    static void write(Path p,Collection<JSONObject> rows)throws Exception{Path tmp=Paths.get(p.toString()+".partial");try(BufferedWriter b=Files.newBufferedWriter(tmp)){for(JSONObject r:rows){b.write(r.toString());b.newLine();}}Files.move(tmp,p);}
    public static void main(String[] a){try{if(a.length!=3)throw new IllegalArgumentException("index ECC|S4H output");plan(Paths.get(a[0]),a[1],Paths.get(a[2]));System.exit(0);}catch(Exception e){e.printStackTrace();System.exit(1);}}
}
