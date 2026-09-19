package mx.com.liverpool.p360.services.core;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/** File-scoped routing to an existing SKU owner; consolidation remains a separate campaign. */
public final class InboundSkuResolver implements AutoCloseable {
    private static final String LIVE="TIMESTAMP '9999-12-31 00:00:00'";
    private final Connection read;
    private final SkuCoordinationLock lock;
    private final Consumer<String> log;
    private final Map<String,Set<String>> bySku=new HashMap<>();
    private final Map<String,String> selected=new HashMap<>();
    private final Set<String> lockedSkus;
    private final Map<String,Row> byId=new HashMap<>();
    private final Set<String> loadedIds=new HashSet<>();
    private final Map<String,String> mapCache=new HashMap<>();
    private static final class Row {String id,sku,alias;int type;Row(int t,String i,String s,String a){type=t;id=i;sku=s;alias=a;}}

    /** SQLite ingestion uses the same coordination and identity rules without manufacturing XML. */
    public static InboundSkuResolver openSkus(Collection<String> values, Consumer<String> log)throws SQLException {
        Set<String> skus=new TreeSet<>();
        for(String value:values)if(value!=null&&!value.isBlank())skus.add(SkuCoordinationLock.normalize(value));
        return new InboundSkuResolver(skus,log);
    }

    public static InboundSkuResolver open(byte[] xml,Consumer<String> log)throws Exception {
        DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        f.setFeature("http://xml.org/sax/features/external-general-entities",false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        Set<String> skus=new TreeSet<>();
        Document d=f.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml));
        NodeList nodes=d.getElementsByTagNameNS("*","Value");
        for(int i=0;i<nodes.getLength();i++){Element e=(Element)nodes.item(i);String a=e.getAttribute("AttributeID");if(a.equals("MATNR")||a.equals("SATNR")){String s=e.getTextContent().trim();if(!s.isEmpty())skus.add(SkuCoordinationLock.normalize(s));}}
        if(skus.isEmpty())throw new IllegalStateException("SKU_GUARD_NO_MATNR: input retained for review");
        return new InboundSkuResolver(skus,log);
    }
    private InboundSkuResolver(Set<String> skus,Consumer<String> log)throws SQLException {
        this.log=log;this.lockedSkus=skus;
        this.lock=new SkuCoordinationLock(new QuickJdbcConnectionManager().openConnection(true),skus);
        Connection c=null;
        try {c=new QuickJdbcConnectionManager().openConnection(true);c.setReadOnly(true);this.read=c;preload(skus);}
        catch(SQLException|RuntimeException e){if(c!=null)try{c.close();}catch(Exception ignored){}try{lock.close();}catch(Exception ignored){}throw e;}
    }
    private static String k(int type,String value){return type+":"+value;}
    private static String blank(String value){return value==null||value.isBlank()||value.equalsIgnoreCase("null")?null:value.trim();}
    public static int rank(String id){if(id.matches("[0-9]{16}"))return 3;String s=id.toUpperCase(Locale.ROOT);return s.startsWith("LVP")||s.startsWith("SBB")?1:2;}
    public static String uniqueBest(Collection<String> ids) {
        String best=null;int score=-1;boolean tie=false;
        for(String id:new TreeSet<>(ids)){int r=rank(id);if(r>score){best=id;score=r;tie=false;}else if(r==score)tie=true;}
        return best; // TreeSet makes equal-rank routing stable, independent of Oracle result order.
    }
    private void add(ResultSet r)throws SQLException {
        Row row=new Row(r.getInt(1),r.getString(2),r.getString(3),blank(r.getString(4)));
        String key=k(row.type,row.id);Row old=byId.put(key,row);
        if(old!=null&&(!Objects.equals(old.sku,row.sku)||!Objects.equals(old.alias,row.alias)))throw new IllegalStateException("Ambiguous live identity: "+key);
        loadedIds.add(key);if(row.sku!=null)bySku.computeIfAbsent(k(row.type,SkuCoordinationLock.normalize(row.sku)),x->new LinkedHashSet<>()).add(row.id);
    }
    private void preload(Set<String> skus)throws SQLException {
        List<String> all=new ArrayList<>(skus);
        for(int off=0;off<all.size();off+=900){List<String> chunk=all.subList(off,Math.min(off+900,all.size()));
            String sql="SELECT /*+ LEADING(ad ar) USE_NL(ar) INDEX(ad IX_AD_TUNE_01) */ ar.\"EntityID\",ar.\"Identifier\",ad.\"Res_Int_02\",ad.\"SupplierAltAID\" FROM PIM_MASTER.\"ArticleDetail\" ad JOIN PIM_MASTER.\"ArticleRevision\" ar ON ar.\"ID\"=ad.\"ArticleRevisionID\" WHERE ad.\"Res_Int_02\" IN ("+String.join(",",Collections.nCopies(chunk.size(),"?"))+") AND ad.\"DeletionTimestamp\"="+LIVE+" AND ar.\"DeletionTimestamp\"="+LIVE+" AND ar.\"RevisionID\"=1 AND ar.\"CatalogID\"=1 AND ar.\"EntityID\" IN (1000,1100) AND NVL(ad.\"CurrentStatus\",0)<>1025";
            try(PreparedStatement p=read.prepareStatement(sql)){p.setQueryTimeout(900);p.setFetchSize(900);for(int i=0;i<chunk.size();i++)p.setBigDecimal(i+1,new java.math.BigDecimal(chunk.get(i)));try(ResultSet r=p.executeQuery()){while(r.next())add(r);}}
        }
        log.accept("SKU_GUARD_READY skus="+skus.size()+" liveIdentities="+byId.size());
    }
    private Row row(int type,String id)throws SQLException {
        if(id==null)return null;String key=k(type,id);if(loadedIds.contains(key))return byId.get(key);
        String sql="SELECT /*+ LEADING(ar ad) USE_NL(ad) INDEX(ar IX_AR_TUNE_01) INDEX(ad XAK1_ArticleDetail) */ ar.\"EntityID\",ar.\"Identifier\",ad.\"Res_Int_02\",ad.\"SupplierAltAID\" FROM PIM_MASTER.\"ArticleRevision\" ar JOIN PIM_MASTER.\"ArticleDetail\" ad ON ad.\"ArticleRevisionID\"=ar.\"ID\" AND ad.\"DeletionTimestamp\"="+LIVE+" WHERE ar.\"Identifier\"=? AND ar.\"EntityID\"=? AND ar.\"CatalogID\"=1 AND ar.\"RevisionID\"=1 AND ar.\"DeletionTimestamp\"="+LIVE+" AND NVL(ad.\"CurrentStatus\",0)<>1025";
        try(PreparedStatement p=read.prepareStatement(sql)){p.setQueryTimeout(900);p.setNString(1,id);p.setInt(2,type);try(ResultSet r=p.executeQuery()){while(r.next())add(r);}}loadedIds.add(key);return byId.get(key);
    }
    private String mapped(int type,String id)throws SQLException {
        String cache=k(type,id);if(mapCache.containsKey(cache))return mapCache.get(cache);
        try(PreparedStatement p=read.prepareStatement("SELECT TargetIdentifier FROM P360_EXPLOIT.CONCILIACION_ID_MAP WHERE EntityID=? AND OldIdentifier=? AND Estado='CONSOLIDATED'")){p.setQueryTimeout(900);p.setInt(1,type);p.setString(2,id);try(ResultSet r=p.executeQuery()){String value=r.next()?blank(r.getString(1)):null;mapCache.put(cache,value);return value;}}
    }
    public String redirect(int type,String id) {
        id=blank(id);if(id==null)return null;String original=id;Set<String> seen=new HashSet<>();
        try {while(true){if(!seen.add(id))throw new IllegalStateException("SKU_ALIAS_CYCLE "+original);String next=mapped(type,id);Row r=row(type,id);if(next==null&&type==1100&&r!=null&&(r.sku==null||r.sku.isBlank()))next=r.alias;if(next==null)return id;if(row(type,next)==null)throw new IllegalStateException("SKU_ALIAS_TARGET_MISSING "+id+" -> "+next);id=next;}}
        catch(SQLException e){throw new IllegalStateException("SKU_IDENTITY_READ_FAILED",e);}
    }
    public String existing(int type,String sku){return resolve(type,sku,null);}
    public void validateIndividual(String sku,String incoming) {
        String s=SkuCoordinationLock.normalize(sku),product=existing(1100,s),article=existing(1000,s);
        try {
            if(article==null&&incoming!=null){String id=redirect(1000,incoming);if(row(1000,id)!=null)article=id;}
            if(article==null)return;
            String sql="SELECT /*+ LEADING(ar r) USE_NL(r) INDEX(ar IX_AR_TUNE_01) INDEX(r XAK1_ArticleReference) */ r.\"RefExtArtIdentifier\" FROM PIM_MASTER.\"ArticleRevision\" ar JOIN PIM_MASTER.\"ArticleReference\" r ON r.\"ArticleRevisionID\"=ar.\"ID\" AND r.\"DeletionTimestamp\"="+LIVE+" WHERE ar.\"Identifier\"=? AND ar.\"EntityID\"=1000 AND ar.\"CatalogID\"=1 AND ar.\"RevisionID\"=1 AND ar.\"DeletionTimestamp\"="+LIVE+" AND r.\"RefEntityID\"=1100";
            Set<String> parents=new HashSet<>(),rawParents=new HashSet<>();try(PreparedStatement p=read.prepareStatement(sql)){p.setQueryTimeout(900);p.setNString(1,article);try(ResultSet r=p.executeQuery()){while(r.next()){rawParents.add(r.getString(1));parents.add(redirect(1100,r.getString(1)));}}}
            if(rawParents.size()>1)throw new IllegalStateException("IDENTITY_RECORD_MULTIPLE_PARENTS "+article);
            if(!parents.isEmpty()){
                String parent=parents.iterator().next();if(product!=null&&!product.equals(parent))throw new IllegalStateException("IDENTITY_RECORD_PARENT_CONFLICT article="+article+" current="+parent+" canonical="+product);
                Row pr=row(1100,parent);if(pr==null)throw new IllegalStateException("IDENTITY_RECORD_PARENT_MISSING "+parent);
                if(pr.sku!=null&&!s.equals(SkuCoordinationLock.normalize(pr.sku))&&!pr.sku.startsWith("999"))throw new IllegalStateException("IDENTITY_RECORD_PARENT_HAS_DIFFERENT_SKU "+parent+" sku="+pr.sku);
            }
        }catch(SQLException e){throw new IllegalStateException("SKU_IDENTITY_READ_FAILED",e);}
    }
    public String resolve(int type,String sku,String incoming) {
        String s=SkuCoordinationLock.normalize(sku);incoming=blank(incoming);
        if(s.isEmpty())return redirect(type,incoming);
        if(!lockedSkus.contains(s))throw new IllegalStateException("SKU_NOT_IN_INPUT_LOCK_SET "+s);
        String cache=k(type,s);if(selected.containsKey(cache)){String winner=selected.get(cache);if(incoming!=null&&!incoming.equals(winner))log.accept("SKU_ROUTED entity="+type+" sku="+s+" incoming="+incoming+" base="+winner);return winner;}
        try {
            String routed=redirect(type,incoming);Set<String> candidates=new LinkedHashSet<>();
            for(String id:new ArrayList<>(bySku.getOrDefault(cache,Collections.emptySet())))candidates.add(redirect(type,id));
            String winner=uniqueBest(candidates);
            if(candidates.size()>1)log.accept("SKU_EXISTING_BASE_SELECTED entity="+type+" sku="+s+" base="+winner+" candidates="+new TreeSet<>(candidates)+" consolidation=pending");
            if(routed!=null&&!Objects.equals(routed,incoming)&&winner!=null&&!winner.equals(routed))throw new IllegalStateException("SKU_ALIAS_CONFLICT "+s+" "+routed+" "+winner);
            if(winner==null)winner=routed;
            if(winner!=null){Row r=row(type,winner);if(r!=null&&r.sku!=null&&!s.equals(SkuCoordinationLock.normalize(r.sku))&&!r.sku.startsWith("999"))throw new IllegalStateException("SKU_TARGET_HAS_OTHER_SKU "+winner+" "+r.sku+" incoming="+s);selected.put(cache,winner);if(!Objects.equals(winner,incoming))log.accept("SKU_ROUTED entity="+type+" sku="+s+" incoming="+incoming+" base="+winner);}
            return winner;
        }catch(SQLException e){throw new IllegalStateException("SKU_IDENTITY_READ_FAILED",e);}
    }
    public void close()throws SQLException {try{read.close();}finally{lock.close();}}
}
