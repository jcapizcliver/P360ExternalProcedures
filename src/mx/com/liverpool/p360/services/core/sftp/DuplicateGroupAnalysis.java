package mx.com.liverpool.p360.services.core.sftp;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import org.json.*;

/** Read-only indexed snapshots and offline comparisons. Never performs a P360 write. */
public final class DuplicateGroupAnalysis {
    static final String ACTIVE="timestamp '9999-12-31 00:00:00.0'";
    static final int BATCH=100, IN_BATCH=500;
    static final Set<String> META=Set.of("id","articlerevisionid","articlecharactvalueid","articleid","creationtimestamp","creationuserid","modificationtimestamp","modificationuserid","deletiontimestamp","deletionuserid","revisionid","catalogid","entityid","identifer","identifier","statusmodification","ownlog","log","ean","res_int_02");
    static final Set<String> LOGS=Set.of("statusmodification","ownlog","log");
    static final Map<String,String> INDEX=Map.of("ArticleDetail","XAK1_ArticleDetail","ArticleLang","XAK1_ArticleLang","ArticleDomain","XAK1_ArticleDomain","ArticleStructureMap","XIE1_ArticleStructureMap","ArticleReference","XIE3_ArticleReference","ArticleCharactValue","IX_ACV_TUNE_02");
    static final class Node {
        final JSONObject revision;
        final Map<String,JSONArray> tables=new LinkedHashMap<>();
        Node(JSONObject revision){this.revision=revision;}
        String id(){return revision.get("Identifier").toString();}
        long rev(){return revision.getLong("ID");}
        JSONArray rows(String table){return tables.computeIfAbsent(table,k->new JSONArray());}
        JSONObject raw(){return new JSONObject().put("ArticleRevision",revision).put("tables",tables);}
    }
    static String marks(int n){return String.join(",",Collections.nCopies(n,"?"));}
    static String active(String alias){return alias+".\"RevisionID\"=1 AND "+alias+".\"CatalogID\"=1 AND "+alias+".\"DeletionTimestamp\"="+ACTIVE;}
    static PreparedStatement statement(Connection c,String sql,List<?> keys)throws Exception {
        PreparedStatement p=c.prepareStatement(sql);p.setQueryTimeout(120);p.setFetchSize(500);
        for(int i=0;i<keys.size();i++){Object v=keys.get(i);if(v instanceof Long)p.setLong(i+1,(Long)v);else p.setString(i+1,v.toString());}return p;
    }
    static JSONObject row(ResultSet rs)throws Exception {
        JSONObject r=new JSONObject();ResultSetMetaData m=rs.getMetaData();
        for(int i=1;i<=m.getColumnCount();i++){
            Object v;
            switch(m.getColumnType(i)){
                case Types.CLOB: case Types.NCLOB: v=rs.getString(i);break;
                case Types.TIMESTAMP: case Types.DATE: Timestamp t=rs.getTimestamp(i);v=t==null?null:t.toString();break;
                case Types.NUMERIC: case Types.DECIMAL: v=rs.getBigDecimal(i);break;
                case Types.BINARY: case Types.VARBINARY: case Types.LONGVARBINARY: case Types.BLOB:
                    throw new SQLException("Unsupported binary column "+m.getColumnLabel(i));
                default: v=rs.getString(i);
            }
            r.put(m.getColumnLabel(i),v==null?JSONObject.NULL:v);
        }return r;
    }
    static Map<Long,Node> products(Connection c,List<String> ids)throws Exception {
        Map<Long,Node> nodes=new LinkedHashMap<>();
        String sql="SELECT /*+ index(t \"XAK2_ArticleRevision\") no_parallel(t) */ t.* FROM PIM_MASTER.\"ArticleRevision\" t WHERE "+active("t")+" AND t.\"EntityID\"=1100 AND t.\"Identifier\" IN ("+marks(ids.size())+")";
        try(PreparedStatement p=statement(c,sql,ids);ResultSet rs=p.executeQuery()){while(rs.next()){Node n=new Node(row(rs));nodes.put(n.rev(),n);}}return nodes;
    }
    static Map<String,Set<Long>> children(Connection c,Map<Long,Node> nodes,List<String> ids)throws Exception {
        Map<String,Set<Long>> children=new LinkedHashMap<>();
        String sql="SELECT /*+ leading(p r a) use_nl(r a) index(p \"XAK2_ArticleRevision\") index(r \"IX_ARTREF_TUNE_01\") index(a \"PK_ArticleRevision\") no_parallel */ p.\"Identifier\" AS PARENT_IDENTIFIER,a.* FROM PIM_MASTER.\"ArticleRevision\" p JOIN PIM_MASTER.\"ArticleReference\" r ON r.\"RefIntArtID\"=p.\"ArticleID\" AND r.\"RefExtArtIdentifier\"=p.\"Identifier\" AND r.\"DeletionTimestamp\"="+ACTIVE+" JOIN PIM_MASTER.\"ArticleRevision\" a ON a.\"ID\"=r.\"ArticleRevisionID\" AND a.\"EntityID\"=1000 AND "+active("a")+" WHERE "+active("p")+" AND p.\"EntityID\"=1100 AND p.\"Identifier\" IN ("+marks(ids.size())+")";
        try(PreparedStatement p=statement(c,sql,ids);ResultSet rs=p.executeQuery()){while(rs.next()){JSONObject j=row(rs);String parent=j.getString("PARENT_IDENTIFIER");j.remove("PARENT_IDENTIFIER");Node n=new Node(j);nodes.putIfAbsent(n.rev(),n);children.computeIfAbsent(parent,k->new LinkedHashSet<>()).add(n.rev());}}return children;
    }
    static void tables(Connection c,Map<Long,Node> nodes)throws Exception {
        List<Long> revisions=new ArrayList<>(nodes.keySet());
        for(int start=0;start<revisions.size();start+=IN_BATCH){List<Long> keys=revisions.subList(start,Math.min(start+IN_BATCH,revisions.size()));
            for(String table:List.of("ArticleDetail","ArticleLang","ArticleDomain","ArticleStructureMap","ArticleReference","ArticleCharactValue")){
                String sql="SELECT /*+ index(t \""+INDEX.get(table)+"\") no_parallel(t) */ t.* FROM PIM_MASTER.\""+table+"\" t WHERE t.\"DeletionTimestamp\"="+ACTIVE+" AND t.\"ArticleRevisionID\" IN ("+marks(keys.size())+")";
                try(PreparedStatement p=statement(c,sql,keys);ResultSet rs=p.executeQuery()){while(rs.next()){JSONObject r=row(rs);nodes.get(r.getLong("ArticleRevisionID")).rows(table).put(r);}}
            }
            String sql="SELECT /*+ leading(v l) use_nl(l) index(v \"IX_ACV_TUNE_02\") index(l \"XIE1_ArticleCharactValueLang\") no_parallel */ v.\"ArticleRevisionID\" AS OWNER_REVISION,l.* FROM PIM_MASTER.\"ArticleCharactValue\" v JOIN PIM_MASTER.\"ArticleCharactValueLang\" l ON l.\"ArticleCharactValueID\"=v.\"ID\" AND l.\"DeletionTimestamp\"="+ACTIVE+" WHERE v.\"DeletionTimestamp\"="+ACTIVE+" AND v.\"ArticleRevisionID\" IN ("+marks(keys.size())+")";
            try(PreparedStatement p=statement(c,sql,keys);ResultSet rs=p.executeQuery()){while(rs.next()){JSONObject r=row(rs);long rev=r.getLong("OWNER_REVISION");r.remove("OWNER_REVISION");nodes.get(rev).rows("ArticleCharactValueLang").put(r);}}
        }
    }
    static String value(JSONObject r,String key){return r.isNull(key)?"":r.opt(key).toString();}
    static String qualifier(JSONObject r,String... fields){JSONArray a=new JSONArray();for(String f:fields)a.put(r.opt(f));return a.toString();}
    static JSONObject fields(JSONObject row,boolean detail){JSONObject out=new JSONObject();for(Object key:row.keySet()){String k=key.toString();
        String low=k.toLowerCase(Locale.ROOT);
        if(META.contains(low)&&!(low.equals("res_int_02")&&!detail))continue;
        if(low.startsWith("creation")||low.startsWith("modification")||low.startsWith("deletion"))continue;
        out.put(k,row.get(k));
    }return out;}
    static void putUnique(JSONObject map,String key,JSONObject value){if(map.has(key))throw new IllegalStateException("Ambiguous logical row "+key);map.put(key,value);}
    static JSONObject normalized(Node n,Map<String,String> characteristics)throws Exception {
        JSONObject out=new JSONObject().put("identifier",n.id()), sections=new JSONObject();out.put("dbFields",sections);
        JSONArray detail=n.rows("ArticleDetail");if(detail.length()!=1)throw new IllegalStateException("Expected one Detail row: "+n.id());
        JSONObject d=detail.getJSONObject(0);if(value(d,"CurrentStatus").equals("1025"))throw new IllegalStateException("Deleted business status: "+n.id());
        if(!value(d,"Res_Int_02").isEmpty())out.put("sku",value(d,"Res_Int_02"));
        sections.put("ArticleDetail",fields(d,true));
        for(String table:List.of("ArticleLang","ArticleDomain","ArticleStructureMap")){
            JSONObject section=new JSONObject();sections.put(table,section);JSONArray rows=n.rows(table);
            for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);String key;
                if(table.equals("ArticleLang"))key=qualifier(r,"EntityID","LanguageID","ChannelID","Res_LK_Text100_01","Res_LK_Int_01");
                else if(table.equals("ArticleDomain"))key=qualifier(r,"EntityID","TargetMarket","ChannelID","Std_LK_Text100_01","Std_LK_Text100_02","Res_LK_Int_01","Res_LK_Text100_01");
                else key=qualifier(r,"StructureID","StructureGroupIdentifier");
                putUnique(section,key,fields(r,false));
            }
        }
        JSONObject chars=new JSONObject();sections.put("characteristics",chars);Map<String,JSONObject> byId=new HashMap<>();
        JSONArray rows=n.rows("ArticleCharactValue");
        for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i);String id=value(r,"CharacteristicID");String code=characteristics.get(id);
            if(code==null)throw new IllegalStateException("Missing characteristic metadata "+id);
            if(LOGS.contains(code.toLowerCase(Locale.ROOT)))continue;
            String key=qualifier(r,"CharacteristicID","RecordKey","ParentRecordKey");JSONObject f=fields(r,false);f.put("_code",code);f.put("languages",new JSONObject());putUnique(chars,key,f);byId.put(value(r,"ID"),f);
        }
        rows=n.rows("ArticleCharactValueLang");for(int i=0;i<rows.length();i++){JSONObject r=rows.getJSONObject(i),f=byId.get(value(r,"ArticleCharactValueID"));if(f!=null)putUnique(f.getJSONObject("languages"),qualifier(r,"LanguageID","EntityID"),fields(r,false));}
        return out;
    }
    static Map<String,String> metadata(Connection c)throws Exception {
        Map<String,String> result=new HashMap<>();String sql="SELECT \"CharacteristicID\",\"Identifier\" FROM PIM_MAIN.\"CharacteristicRevision\" WHERE \"RevisionID\"=1 AND \"DeletionTimestamp\"="+ACTIVE;
        try(PreparedStatement p=statement(c,sql,List.of());ResultSet rs=p.executeQuery()){while(rs.next()){String id=rs.getString(1),code=rs.getString(2);if(result.putIfAbsent(id,code)!=null)throw new IllegalStateException("Ambiguous characteristic metadata "+id);}}return result;
    }
    static LinkedHashMap<String,List<String>> csv(Path file)throws Exception {
        LinkedHashMap<String,List<String>> groups=new LinkedHashMap<>();Map<String,Integer> expected=new HashMap<>();
        try(BufferedReader r=Files.newBufferedReader(file)){String line=r.readLine();if(line==null||!line.replace("\"","").trim().equals("IDENTIFIER,SKU,IDENTIFIERS_PER_SKU"))throw new IOException("Unexpected CSV header");
            while((line=r.readLine())!=null){if(line.isBlank())continue;String[] f=line.replace("\"","").split(",",-1);if(f.length!=3||!f[0].matches("[A-Za-z0-9_.-]+")||!f[1].matches("[0-9]+"))throw new IOException("Invalid CSV record");List<String> ids=groups.computeIfAbsent(f[1],k->new ArrayList<>());if(ids.contains(f[0]))throw new IOException("Repeated input row");ids.add(f[0]);int count=Integer.parseInt(f[2]);if(expected.putIfAbsent(f[1],count)!=null&&expected.get(f[1])!=count)throw new IOException("Inconsistent count");}
        }
        for(String sku:groups.keySet())if(groups.get(sku).size()!=expected.get(sku)||expected.get(sku)<2)throw new IOException("Incomplete group "+sku);return groups;
    }
    static void gzip(Path path,JSONObject value)throws Exception {
        Path partial=path.resolveSibling(path.getFileName()+".partial");try(Writer w=new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(partial,StandardOpenOption.CREATE_NEW)),StandardCharsets.UTF_8)){w.write(value.toString());}Files.move(partial,path,StandardCopyOption.ATOMIC_MOVE);
    }
    static JSONObject bundle(Node p,Map<Long,Node> nodes,Map<String,Set<Long>> children,Map<String,String> meta)throws Exception {
        JSONArray articles=new JSONArray();for(long r:children.getOrDefault(p.id(),Set.of())){Node n=nodes.get(r);JSONArray d=n.rows("ArticleDetail");if(d.length()==1&&value(d.getJSONObject(0),"CurrentStatus").equals("1025"))continue;articles.put(normalized(n,meta));}
        return new JSONObject().put("product",normalized(p,meta)).put("articles",articles);
    }
    public static void main(String[] args)throws Exception {
        if(args.length<2||args.length>3)throw new IllegalArgumentException("duplicates.csv NEW-output-directory [max-groups]");
        Path source=Path.of(args[0]),out=Path.of(args[1]);Files.createDirectory(out);Files.copy(source,out.resolve("input.csv"));
        LinkedHashMap<String,List<String>> groups=csv(source);List<String> skus=new ArrayList<>(groups.keySet());if(args.length==3)skus=skus.subList(0,Math.min(skus.size(),Integer.parseInt(args[2])));
        Properties props=new Properties();props.setProperty("user",Objects.requireNonNull(System.getenv("ORACLE_JDBC_USER")));props.setProperty("password",Objects.requireNonNull(System.getenv("ORACLE_JDBC_PASSWORD")));props.setProperty("oracle.jdbc.ReadTimeout","180000");props.setProperty("oracle.net.CONNECT_TIMEOUT","15000");
        int completed=0,review=0;long entityCount=0;Instant started=Instant.now();
        try(Connection c=DriverManager.getConnection(Objects.requireNonNull(System.getenv("ORACLE_JDBC_URL")),props);BufferedWriter summary=Files.newBufferedWriter(out.resolve("groups.jsonl"),StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW)){
            c.setAutoCommit(false);Map<String,String> meta=metadata(c);c.rollback();
            for(int start=0;start<skus.size();start+=BATCH){List<String> part=skus.subList(start,Math.min(start+BATCH,skus.size())),ids=new ArrayList<>();for(String sku:part)ids.addAll(groups.get(sku));
                if(ids.size()>900)throw new IllegalStateException("Oversized group batch");
                Instant snapshot=Instant.now();try(Statement s=c.createStatement()){s.execute("SET TRANSACTION READ ONLY");}
                Map<Long,Node> nodes=products(c,ids);Map<String,Set<Long>> kids=children(c,nodes,ids);tables(c,nodes);c.rollback();
                Map<String,List<Node>> products=new HashMap<>();for(Node n:nodes.values())if(n.revision.getInt("EntityID")==1100)products.computeIfAbsent(n.id(),k->new ArrayList<>()).add(n);
                for(String sku:part){JSONObject report=new JSONObject().put("sku",sku).put("identifiers",groups.get(sku)).put("snapshotStarted",snapshot.toString()).put("mode","READ_ONLY_ANALYSIS");JSONObject raw=new JSONObject();JSONArray bundles=new JSONArray(),plans=new JSONArray();String status="ANALYZED";String issue="";
                    Set<Long> included=new LinkedHashSet<>();for(String id:groups.get(sku)){for(Node n:products.getOrDefault(id,List.of()))included.add(n.rev());included.addAll(kids.getOrDefault(id,Set.of()));}for(long r:included)raw.put(Long.toString(r),nodes.get(r).raw());report.put("entities",raw);
                    try{
                        for(String id:groups.get(sku)){List<Node> found=products.getOrDefault(id,List.of());if(found.size()!=1)throw new IllegalStateException("Missing or ambiguous product "+id);JSONObject b=bundle(found.get(0),nodes,kids,meta);if(!sku.equals(b.getJSONObject("product").optString("sku")))throw new IllegalStateException("SKU changed for "+id);bundles.put(b);}
                        for(int b=0;b<bundles.length();b++){JSONArray donors=new JSONArray();for(int i=0;i<bundles.length();i++)if(i!=b)donors.put(bundles.getJSONObject(i));JSONObject input=new JSONObject().put("base",bundles.getJSONObject(b)).put("sources",donors);JSONObject plan=ReconciliationPlan.plan(input);plan.put("comparisonBase",groups.get(sku).get(b));plans.put(plan);}
                    }catch(IllegalStateException e){status="REVIEW_REQUIRED";issue=e.getMessage();review++;}
                    report.put("normalizedBundles",bundles).put("alternativeBasePlans",plans).put("status",status).put("issue",issue).put("scope","Raw active DB rows and legacy fill-missing comparisons for every possible base; not Object API payloads. No golden record chosen. No Mongo/timestamp precedence applied. Reference rows are evidence only; SKU/EAN and audit fields excluded from complements.");
                    gzip(out.resolve(sku+".json.gz"),report);JSONObject line=new JSONObject().put("sku",sku).put("status",status).put("issue",issue).put("products",groups.get(sku).size()).put("entities",included.size()).put("plans",plans.length());summary.write(line.toString());summary.newLine();summary.flush();completed++;entityCount+=included.size();
                }
                System.out.println(Instant.now()+" groups="+completed+"/"+skus.size()+" review="+review+" entitySnapshots="+entityCount);
            }
        }
        Files.writeString(out.resolve("complete.json"),new JSONObject().put("started",started.toString()).put("finished",Instant.now().toString()).put("groups",completed).put("reviewRequired",review).put("entitySnapshots",entityCount).toString(2),StandardOpenOption.CREATE_NEW);
    }
}
