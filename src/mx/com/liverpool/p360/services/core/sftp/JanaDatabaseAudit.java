package mx.com.liverpool.p360.services.core.sftp;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.json.*;

/** Indexed JDBC reads in batches; no P360 writes and no REST calls. */
public class JanaDatabaseAudit {
    static final String ACTIVE="timestamp '9999-12-31 00:00:00.0'";
    static final int BATCH=500;
    static final class Node {
        long revision;int entity;String id,sku;Set<String> parents=new LinkedHashSet<>();
        JSONObject json(){JSONObject j=new JSONObject().put("identifier",id);if(sku!=null)j.put("sku",sku);
            if(!parents.isEmpty()){JSONArray p=new JSONArray();for(String id:parents)p.put(new JSONObject().put("_qualification",new JSONObject().put("referencedIdentifier",id)));j.put("higherLevelProduct",p);}return j;}
    }
    static final class Snapshot implements JanaIndividualTargets.Lookup {
        Map<Long,Node> revisions=new HashMap<>();
        Map<String,List<Node>> identifiers=new HashMap<>(),skus=new HashMap<>(),children=new HashMap<>();
        void add(ResultSet rs)throws Exception{
            long rev=rs.getLong(1);Node n=revisions.get(rev);String sku=rs.getString(4);
            if(n!=null){if(!Objects.equals(n.sku,sku))throw new SQLException("Multiple active Detail rows for revision "+rev);return;}
            n=new Node();n.revision=rev;n.entity=rs.getInt(2);n.id=rs.getString(3);n.sku=sku;revisions.put(rev,n);
            identifiers.computeIfAbsent(n.entity+":"+n.id,k->new ArrayList<>()).add(n);
            if(sku!=null)skus.computeIfAbsent(n.entity+":"+sku,k->new ArrayList<>()).add(n);
        }
        static int entity(String e){return e.equals("Article")?1000:1100;}
        static List<JSONObject> json(List<Node> nodes){List<JSONObject> result=new ArrayList<>();if(nodes!=null)for(Node n:nodes)result.add(n.json());return result;}
        public JSONObject byId(String e,String id){List<JSONObject> n=json(identifiers.get(entity(e)+":"+id));if(n.size()>1)throw new IllegalStateException("Ambiguous Identifier "+id);return n.isEmpty()?null:n.get(0);}
        public List<JSONObject> bySku(String e,String sku){return json(skus.get(entity(e)+":"+sku));}
        public List<JSONObject> children(String id){return json(children.get(id));}
        JSONObject uniqueSku(String e,String sku){List<JSONObject> rows=bySku(e,sku);if(rows.size()>1)throw new IllegalStateException("Ambiguous SKU "+sku);return rows.isEmpty()?null:rows.get(0);}
    }
    static String placeholders(int n){return String.join(",",Collections.nCopies(n,"?"));}
    static String activeAr(String a){return a+".\"RevisionID\"=1 AND "+a+".\"CatalogID\"=1 AND "+a+".\"DeletionTimestamp\"="+ACTIVE;}
    static void entities(Connection c,Snapshot db,Collection<String> keys,boolean sku)throws Exception{
        List<String> values=new ArrayList<>(keys);int loaded=0;
        for(int start=0;start<values.size();start+=BATCH){
            List<String> batch=values.subList(start,Math.min(start+BATCH,values.size()));
            String sql="SELECT "+(sku?"/*+ leading(ad ar) use_nl(ar) index(ad IX_AD_TUNE_01) */ ":"/*+ leading(ar ad) use_nl(ad) index(ar \"XAK2_ArticleRevision\") */ ")
                +"ar.\"ID\",ar.\"EntityID\",ar.\"Identifier\",ad.\"Res_Int_02\" FROM "
                +(sku?"\"ArticleDetail\" ad JOIN \"ArticleRevision\" ar ON ar.\"ID\"=ad.\"ArticleRevisionID\" ":"\"ArticleRevision\" ar LEFT JOIN \"ArticleDetail\" ad ON ad.\"ArticleRevisionID\"=ar.\"ID\" AND ad.\"DeletionTimestamp\"="+ACTIVE+" ")
                +"WHERE "+activeAr("ar")+" AND ar.\"EntityID\" IN (1000,1100) AND (ad.\"CurrentStatus\" IS NULL OR ad.\"CurrentStatus\"<>1025) AND "
                +(sku?"ad.\"DeletionTimestamp\"="+ACTIVE+" AND ad.\"Res_Int_02\"":"ar.\"Identifier\"")+" IN ("+placeholders(batch.size())+")";
            try(PreparedStatement p=c.prepareStatement(sql)){p.setFetchSize(2000);p.setQueryTimeout(120);
                for(int i=0;i<batch.size();i++){if(sku)p.setLong(i+1,Long.parseLong(batch.get(i)));else p.setString(i+1,batch.get(i));}
                try(ResultSet rs=p.executeQuery()){while(rs.next())db.add(rs);}
            }
            loaded+=batch.size();if(loaded%10000==0||loaded==values.size())System.out.println("DB "+(sku?"SKU":"ID")+" keys="+loaded+"/"+values.size()+" nodes="+db.revisions.size());
        }
    }
    static Set<String> parents(Connection c,Snapshot db)throws Exception{
        List<Long> keys=new ArrayList<>();for(Node n:db.revisions.values())if(n.entity==1000)keys.add(n.revision);
        Set<String> parentIds=new HashSet<>();
        for(int start=0;start<keys.size();start+=BATCH){List<Long> batch=keys.subList(start,Math.min(start+BATCH,keys.size()));
            String sql="SELECT /*+ leading(r p) use_nl(p) index(r \"XIE3_ArticleReference\") index(p \"XAK1_ArticleRevision\") */ r.\"ArticleRevisionID\",p.\"Identifier\" FROM \"ArticleReference\" r JOIN \"ArticleRevision\" p ON p.\"ArticleID\"=r.\"RefIntArtID\" AND p.\"Identifier\"=r.\"RefExtArtIdentifier\" AND p.\"EntityID\"=1100 AND "+activeAr("p")
                +" WHERE r.\"DeletionTimestamp\"="+ACTIVE+" AND r.\"ArticleRevisionID\" IN ("+placeholders(batch.size())+")";
            try(PreparedStatement p=c.prepareStatement(sql)){p.setFetchSize(2000);p.setQueryTimeout(120);for(int i=0;i<batch.size();i++)p.setLong(i+1,batch.get(i));
                try(ResultSet rs=p.executeQuery()){while(rs.next()){Node n=db.revisions.get(rs.getLong(1));String parent=rs.getString(2);if(n.parents.add(parent))db.children.computeIfAbsent(parent,k->new ArrayList<>()).add(n);parentIds.add(parent);}}
            }
            if(start%10000==0)System.out.println("DB REFERENCES articles="+Math.min(start+BATCH,keys.size())+"/"+keys.size());
        }
        return parentIds;
    }
    static void loadChildren(Connection c,Snapshot db,Set<String> incomingIds)throws Exception{
        List<String> keys=new ArrayList<>();
        for(String id:incomingIds)if(db.identifiers.containsKey("1100:"+id)&&!db.identifiers.containsKey("1000:"+id))keys.add(id);
        for(int start=0;start<keys.size();start+=BATCH){List<String> batch=keys.subList(start,Math.min(start+BATCH,keys.size()));
            String sql="SELECT /*+ leading(p r ar ad) use_nl(r ar ad) index(p \"XAK2_ArticleRevision\") index(r IX_ARTREF_TUNE_01) */ ar.\"ID\",ar.\"EntityID\",ar.\"Identifier\",ad.\"Res_Int_02\" FROM \"ArticleRevision\" p JOIN \"ArticleReference\" r ON r.\"RefIntArtID\"=p.\"ArticleID\" AND r.\"RefExtArtIdentifier\"=p.\"Identifier\" AND r.\"DeletionTimestamp\"="+ACTIVE
                +" JOIN \"ArticleRevision\" ar ON ar.\"ID\"=r.\"ArticleRevisionID\" AND ar.\"EntityID\"=1000 AND "+activeAr("ar")
                +" LEFT JOIN \"ArticleDetail\" ad ON ad.\"ArticleRevisionID\"=ar.\"ID\" AND ad.\"DeletionTimestamp\"="+ACTIVE
                +" WHERE "+activeAr("p")+" AND p.\"EntityID\"=1100 AND (ad.\"CurrentStatus\" IS NULL OR ad.\"CurrentStatus\"<>1025) AND p.\"Identifier\" IN ("+placeholders(batch.size())+")";
            try(PreparedStatement p=c.prepareStatement(sql)){p.setFetchSize(2000);p.setQueryTimeout(120);for(int i=0;i<batch.size();i++)p.setString(i+1,batch.get(i));try(ResultSet rs=p.executeQuery()){while(rs.next())db.add(rs);}}
            if(start%10000==0)System.out.println("DB CHILDREN products="+Math.min(start+BATCH,keys.size())+"/"+keys.size());
        }
    }
    static JSONObject compare(JSONObject input,Snapshot db){
        JSONObject r=new JSONObject(input.toString()).put("checkedAt",java.time.Instant.now().toString()).put("comparison","JDBC_BATCH_SNAPSHOT");
        String sku=r.getString("sku"),type=r.getString("attyp"),incoming=r.getString("incomingId");JSONObject article;String expected;
        try{
            if(type.equals("01"))return r.put("status","GENERIC_NO_RELATION_REQUIRED");
            if(type.equals("00")){
                JanaIndividualTargets t=JanaIndividualTargets.resolve(sku,incoming,db);r.put("article",t.articleId).put("expectedProduct",t.productId);
                if(t.newProduct)return r.put("status","MISSING_PRODUCT");article=db.byId("Article",t.articleId);expected=t.productId;
            }else if(type.equals("02")){
                article=incoming.isEmpty()?null:db.byId("Article",incoming);if(article==null)article=db.uniqueSku("Article",sku);
                if(article==null)return r.put("status","MISSING_ARTICLE");r.put("article",article.getString("identifier"));
                String parentSku=r.getString("parentSku");if(parentSku.isEmpty())return r.put("status","MISSING_PARENT_SKU_IN_XML");
                JSONObject product=db.uniqueSku("Product2G",parentSku);if(product==null)return r.put("status","MISSING_PRODUCT");expected=product.getString("identifier");r.put("expectedProduct",expected);
            }else return r.put("status","UNKNOWN_ATTYP");
            if(article==null)return r.put("status","MISSING_ARTICLE");
            String actual=article.optString("sku","");if(!actual.isEmpty()&&!actual.equals(sku))return r.put("status","ARTICLE_SKU_CONFLICT");
            JSONArray parents=article.optJSONArray("higherLevelProduct");r.put("currentParents",parents==null?new JSONArray():parents);
            if(parents==null||parents.length()==0)return r.put("status","MISSING_REFERENCE");
            if(parents.length()>1)return r.put("status","AMBIGUOUS");
            return r.put("status",expected.equals(parents.getJSONObject(0).getJSONObject("_qualification").getString("referencedIdentifier"))?"OK":"WRONG_PARENT");
        }catch(Exception e){return r.put("status","AMBIGUOUS").put("reason",e.toString());}
    }
    public static void main(String[] args){try{
        if(args.length!=2)throw new IllegalArgumentException("<latest-products.jsonl> <new-output-dir>");
        Path index=Paths.get(args[0]),out=Paths.get(args[1]);Files.createDirectories(out);if(Files.exists(out.resolve("relations.jsonl")))throw new IOException("Use a new output directory");
        Set<String> skus=new HashSet<>(),ids=new HashSet<>();int records=0;
        try(BufferedReader b=Files.newBufferedReader(index)){String line;while((line=b.readLine())!=null){JSONObject r=new JSONObject(line);records++;String type=r.getString("attyp");if(!type.equals("00")&&!type.equals("02"))continue;
            for(String field:List.of("sku","parentSku")){String v=r.getString(field);if(v.matches("[0-9]+"))skus.add(v);}String id=r.getString("incomingId");if(!id.isEmpty())ids.add(id);
        }}
        System.out.println("TARGETS records="+records+" SKUs="+skus.size()+" IDs="+ids.size());Snapshot db=new Snapshot();
        Properties props=new Properties();props.setProperty("user",Objects.requireNonNull(System.getenv("ORACLE_JDBC_USER")));props.setProperty("password",Objects.requireNonNull(System.getenv("ORACLE_JDBC_PASSWORD")));props.setProperty("oracle.jdbc.ReadTimeout","180000");
        String snapshot=java.time.Instant.now().toString();
        try(Connection c=DriverManager.getConnection(Objects.requireNonNull(System.getenv("ORACLE_JDBC_URL")),props)){
            c.setAutoCommit(false);try(Statement s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
            entities(c,db,skus,true);entities(c,db,ids,false);loadChildren(c,db,ids);Set<String> p=parents(c,db);
            p.removeIf(id->db.identifiers.containsKey("1100:"+id));entities(c,db,p,false);c.rollback();
        }
        Map<String,Integer> totals=new TreeMap<>();Path partial=out.resolve("relations.jsonl.partial");
        try(BufferedReader b=Files.newBufferedReader(index);BufferedWriter w=Files.newBufferedWriter(partial)){
            String line;while((line=b.readLine())!=null){JSONObject r=compare(new JSONObject(line),db).put("snapshotStartedAt",snapshot);w.write(r.toString());w.newLine();totals.merge(r.getString("status"),1,Integer::sum);}
        }
        Files.move(partial,out.resolve("relations.jsonl"));Files.writeString(out.resolve("summary.json"),new JSONObject(totals).put("TOTAL",records).toString(2));System.out.println("COMPLETE "+totals);
        System.exit(0);
    }catch(Exception e){e.printStackTrace();System.exit(1);}}
}
