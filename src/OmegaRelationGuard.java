import java.sql.*;import java.nio.file.*;import java.nio.channels.*;import java.util.*;import java.io.*;import org.json.*;
/** Reconcile only Omega foreign keys. Native P360 is strictly read-only. */
public class OmegaRelationGuard {
 static final String LIVE="TIMESTAMP '9999-12-31 00:00:00'";
 static Connection db;static Path dir;static long scanned,updated,unresolved,last,cycle;
 static class Row {long id;String identifier;Long old;boolean nativePresent,bad;Set<Long> parents=new HashSet<>();String reason;}
 static String marks(int n){return String.join(",",Collections.nCopies(n,"?"));}
 static Long number(ResultSet r,int n)throws SQLException{long v=r.getLong(n);return r.wasNull()?null:v;}
 static Map<Long,Row> read(List<Long> ids)throws Exception{
  String sql="SELECT o.ID,o.Identifier,o.ProductoID,a.\"ID\",r.\"ID\",p.\"ID\",op.ID FROM P360_EXPLOIT.ARTICULO o LEFT JOIN PIM_MASTER.\"ArticleRevision\" a ON a.\"ID\"=o.ID AND a.\"EntityID\"=1000 AND a.\"RevisionID\"=1 AND a.\"DeletionTimestamp\"="+LIVE+" LEFT JOIN PIM_MASTER.\"ArticleReference\" r ON r.\"ArticleRevisionID\"=a.\"ID\" AND r.\"RefEntityID\"=1100 AND r.\"DeletionTimestamp\"="+LIVE+" LEFT JOIN PIM_MASTER.\"ArticleRevision\" p ON p.\"ArticleID\"=r.\"RefIntArtID\" AND p.\"CatalogID\"=r.\"RefIntCatID\" AND p.\"EntityID\"=1100 AND p.\"RevisionID\"=1 AND p.\"DeletionTimestamp\"="+LIVE+" LEFT JOIN P360_EXPLOIT.PRODUCTO op ON op.ID=p.\"ID\" WHERE o.ID IN ("+marks(ids.size())+") AND o.RevisionID=1";
  Map<Long,Row> rows=new TreeMap<>();try(PreparedStatement q=db.prepareStatement(sql)){q.setQueryTimeout(900);q.setFetchSize(1000);for(int i=0;i<ids.size();i++)q.setLong(i+1,ids.get(i));try(ResultSet r=q.executeQuery()){while(r.next()){
   long id=r.getLong(1);Row x=rows.get(id);if(x==null){x=new Row();x.id=id;x.identifier=r.getString(2);x.old=number(r,3);x.nativePresent=number(r,4)!=null;rows.put(id,x);}
   if(number(r,5)!=null){Long p=number(r,6),captured=number(r,7);if(p==null||captured==null){x.bad=true;x.reason=p==null?"NATIVE_PARENT_MISSING":"OMEGA_PARENT_MISSING";}else x.parents.add(p);}
  }}}
  return rows;
 }
 static boolean valid(Row r){return r.nativePresent&&!r.bad&&r.parents.size()<=1;}
 static Long target(Row r){return r.parents.isEmpty()?null:r.parents.iterator().next();}
 static void state(String phase)throws Exception{JSONObject j=new JSONObject().put("phase",phase).put("cycle",cycle).put("lastId",last).put("scanned",scanned).put("updated",updated).put("unresolved",unresolved).put("at",java.time.Instant.now().toString());Path t=dir.resolve("status.tmp");Files.writeString(t,j.toString());Files.move(t,dir.resolve("status.json"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
 static void journal(String name,JSONObject j)throws Exception{Files.writeString(dir.resolve(name),j.toString()+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
 static void pass()throws Exception{
  while(true){List<Long> ids=new ArrayList<>();try(PreparedStatement q=db.prepareStatement("SELECT ID FROM P360_EXPLOIT.ARTICULO WHERE ID>? AND RevisionID=1 ORDER BY ID FETCH FIRST 900 ROWS ONLY")){q.setLong(1,last);q.setQueryTimeout(900);try(ResultSet r=q.executeQuery()){while(r.next())ids.add(r.getLong(1));}}
   if(ids.isEmpty())return;
   Map<Long,Row> rows=read(ids);List<Long> changes=new ArrayList<>();
   for(Row r:rows.values()){
    if(!valid(r)){unresolved++;journal("pending.jsonl",new JSONObject().put("cycle",cycle).put("id",r.id).put("identifier",r.identifier).put("reason",r.reason!=null?r.reason:(!r.nativePresent?"NATIVE_ARTICLE_ABSENT":"MULTIPLE_NATIVE_PARENTS")));}
    else if(!Objects.equals(r.old,target(r)))changes.add(r.id);
   }
   if(!changes.isEmpty()){
    // Same owner order as the live projector. Re-read native edges AFTER taking Omega locks.
    try(PreparedStatement q=db.prepareStatement("SELECT ID FROM P360_EXPLOIT.ARTICULO WHERE ID IN ("+marks(changes.size())+") ORDER BY ID FOR UPDATE WAIT 60")){q.setQueryTimeout(900);for(int i=0;i<changes.size();i++)q.setLong(i+1,changes.get(i));try(ResultSet r=q.executeQuery()){while(r.next())r.getLong(1);}}
    List<JSONObject> evidence=new ArrayList<>();
    try(PreparedStatement u=db.prepareStatement("UPDATE P360_EXPLOIT.ARTICULO SET ProductoID=? WHERE ID=?")){u.setQueryTimeout(900);for(Row r:read(changes).values())if(valid(r)&&!Objects.equals(r.old,target(r))){Long t=target(r);if(t==null)u.setNull(1,Types.NUMERIC);else u.setLong(1,t);u.setLong(2,r.id);u.addBatch();evidence.add(new JSONObject().put("id",r.id).put("identifier",r.identifier).put("before",r.old==null?JSONObject.NULL:r.old).put("after",t==null?JSONObject.NULL:t).put("at",java.time.Instant.now().toString()));}u.executeBatch();}
    db.commit();updated+=evidence.size();for(JSONObject j:evidence)journal("changes.jsonl",j);
   }else db.commit();
   scanned+=ids.size();last=ids.get(ids.size()-1);state("RECONCILING");
  }
 }
 public static void main(String[] args)throws Exception{
  dir=Path.of(args[0]);Files.createDirectories(dir);
  try(FileChannel channel=FileChannel.open(dir.resolve("guard.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);FileLock lock=channel.tryLock()){
   if(lock==null)throw new IllegalStateException("Relation guard already running");
   if(Files.exists(dir.resolve("status.json"))){JSONObject s=new JSONObject(Files.readString(dir.resolve("status.json")));last=s.optLong("lastId");scanned=s.optLong("scanned");updated=s.optLong("updated");unresolved=s.optLong("unresolved");cycle=s.optLong("cycle");if(s.optString("phase").equals("CAUGHT_UP")){last=0;cycle++;}}
   while(true){
    try{db=new mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager().openConnection(false);db.setAutoCommit(false);pass();state("CAUGHT_UP");db.close();Thread.sleep(1800000);last=0;cycle++;state("RECONCILING");}
    catch(Exception e){if(db!=null){try{db.rollback();db.close();}catch(Exception ignored){}}journal("errors.jsonl",new JSONObject().put("at",java.time.Instant.now().toString()).put("error",e.toString()).put("lastId",last));state("RETRY_PENDING");Thread.sleep(30000);}
   }
  }
 }
}
