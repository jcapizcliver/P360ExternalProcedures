import java.sql.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import mx.com.liverpool.exploit.services.core.*;

/** Additive migration and restartable backfill. No native P360 mutations. */
public final class OmegaAuditMaintenance {
 static Connection c,read;static String prefix;static Path dir;static JSONArray catalogs;
 static String aux(String name){if(prefix.equals("PIM_MASTER.EXP_PR_"))name=Map.of("PRODUCTOATRIBUTOVALOR","PRO_VAL","ARTICULOATRIBUTOVALOR","ART_VAL","CLASIFICACION","CLASIF").getOrDefault(name,name);return prefix+name;}
 static String q(String value){if(!value.matches("[A-Za-z][A-Za-z0-9_]*"))throw new IllegalArgumentException(value);return "\""+value+"\"";}
 static int execute(String sql,Object...args)throws Exception{if(!(sql.startsWith("SELECT")||sql.startsWith("ALTER SESSION")||sql.contains(prefix)))throw new IllegalArgumentException("Auxiliary writes only");try(PreparedStatement p=c.prepareStatement(sql)){p.setQueryTimeout(900);for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);return p.executeUpdate();}}
 static Set<String> columns(String owner,String table)throws Exception{Set<String> out=new HashSet<>();try(PreparedStatement p=c.prepareStatement("SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE OWNER=? AND TABLE_NAME=?")){p.setString(1,owner);p.setString(2,table);try(ResultSet r=p.executeQuery()){while(r.next())out.add(r.getString(1));}}return out;}
 static boolean exists(String table)throws Exception{String[] x=table.split("\\.");return !columns(x[0],x[1]).isEmpty();}
 static void controls(String table,boolean quoted)throws Exception{
  String[] x=table.split("\\.");Set<String> present=columns(x[0],x[1]);if(present.isEmpty())throw new SQLException("Missing "+table);
  List<String> add=new ArrayList<>();for(String name:OmegaAudit.CONTROL)if(!present.contains(quoted?name:name.toUpperCase(Locale.ROOT)))add.add((quoted?q(name):name)+(name.endsWith("ID")?" NUMBER(19)":" TIMESTAMP(8)"));
  if(!add.isEmpty()){execute("ALTER TABLE "+table+" ADD ("+String.join(",",add)+")");System.out.println("ADDED "+table+" "+add);}
 }
 static void install()throws Exception{
  execute("ALTER SESSION SET DDL_LOCK_TIMEOUT=900");
  for(String t:List.of("PRODUCTO","ARTICULO","PRODUCTOATRIBUTOVALOR","ARTICULOATRIBUTOVALOR","CLASIFICACION"))controls(aux(t),false);
  for(int i=0;i<catalogs.length();i++){JSONObject t=catalogs.getJSONObject(i);String table=prefix+"CAT_"+t.getString("suffix");if(exists(table))controls(table,true);}
  if(!exists(prefix+"ATTRIBUTE_TRACKING"))execute("CREATE TABLE "+prefix+"ATTRIBUTE_TRACKING(EntityID NUMBER(10) NOT NULL,AttributeIdentifier VARCHAR2(250) NOT NULL,Enabled NUMBER(1) DEFAULT 1 NOT NULL,PRIMARY KEY(EntityID,AttributeIdentifier),CHECK(Enabled IN(0,1)))");
  if(!exists(prefix+"ATTRIBUTE_HISTORY")){
   execute("CREATE TABLE "+prefix+"ATTRIBUTE_HISTORY(ID VARCHAR2(64) PRIMARY KEY,EntityID NUMBER(10) NOT NULL,OwnerID NUMBER(19) NOT NULL,Identifier VARCHAR2(250),RevisionID NUMBER(19) NOT NULL,AttributeIdentifier VARCHAR2(250) NOT NULL,OldCode VARCHAR2(4000),NewCode VARCHAR2(4000),NewValue VARCHAR2(4000),ChangedAt TIMESTAMP(8),ChangedByID NUMBER(19),ChangedBy VARCHAR2(250),TimePrecision VARCHAR2(16) NOT NULL,Origin VARCHAR2(32) NOT NULL,SourceOrdinal NUMBER(10),Evidence NCLOB,CapturedAt TIMESTAMP(8) DEFAULT SYSTIMESTAMP NOT NULL)");
   execute("CREATE INDEX "+prefix+"IX_ATTR_HIST_OWNER ON "+prefix+"ATTRIBUTE_HISTORY(EntityID,OwnerID,AttributeIdentifier,ChangedAt)");
  }
  for(int entity:new int[]{1100,1000})execute("MERGE INTO "+prefix+"ATTRIBUTE_TRACKING t USING(SELECT ? EntityID FROM dual)s ON(t.EntityID=s.EntityID AND t.AttributeIdentifier='CurrentStatus') WHEN NOT MATCHED THEN INSERT(EntityID,AttributeIdentifier,Enabled) VALUES(s.EntityID,'CurrentStatus',1)",entity);
  c.commit();System.out.println("SCHEMA_READY "+prefix);
 }
 static void status(JSONObject state)throws Exception{state.put("at",java.time.Instant.now().toString());Path tmp=dir.resolve("status.tmp");Files.writeString(tmp,state.toString());Files.move(tmp,dir.resolve("status.json"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);System.out.println(state);}
 static long checkpoint(String phase)throws Exception{Path f=dir.resolve(phase+".checkpoint");return Files.exists(f)?Long.parseLong(Files.readString(f).trim()):0;}
 static void checkpoint(String phase,long id)throws Exception{Path f=dir.resolve(phase+".tmp");Files.writeString(f,Long.toString(id));Files.move(f,dir.resolve(phase+".checkpoint"),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
 static String marks(int n){return String.join(",",Collections.nCopies(n,"?"));}
 static String assignments(boolean quoted){List<String> sets=new ArrayList<>();for(String name:OmegaAudit.CONTROL){String t="t."+(quoted?q(name):name);String v="s."+q(name);sets.add(t+"="+(name.startsWith("Creation")?"COALESCE("+t+","+v+")":"CASE WHEN t."+(quoted?q("ModificationTimestamp"):"ModificationTimestamp")+" IS NULL OR t."+(quoted?q("ModificationTimestamp"):"ModificationTimestamp")+"<=s.\"ModificationTimestamp\" THEN "+v+" ELSE "+t+" END"));}return String.join(",",sets);}
 static int valuesAudit(int entity,List<Long> ids)throws Exception{
  String table=aux(entity==1100?"PRODUCTOATRIBUTOVALOR":"ARTICULOATRIBUTOVALOR"),owner=entity==1100?"ProductoID":"ArticuloID";int changed=0;
  for(String source:List.of("ArticleDetail","ArticleLang","ArticleDomain","ArticleCharactValue","ArticleCharactValueLang")){
   List<Object> args=new ArrayList<>();args.add(source);args.addAll(ids);
   // Drive the native PK lookup from the indexed Omega owner range. No full ACV read.
   String select="SELECT /*+ LEADING(v n) USE_NL(n) NO_PARALLEL */ v.ID VALUE_ID,n.\"CreationUserID\",n.\"CreationTimestamp\",n.\"ModificationUserID\",n.\"ModificationTimestamp\" FROM "+table+" v JOIN PIM_MASTER."+q(source)+" n ON n.\"ID\"=v.SourceRowID WHERE v.SourceTable=? AND v."+owner+" IN ("+marks(ids.size())+") AND (v.CreationTimestamp IS NULL OR v.ModificationTimestamp IS NULL)";
   changed+=execute("MERGE INTO "+table+" t USING("+select+") s ON(t.ID=s.VALUE_ID) WHEN MATCHED THEN UPDATE SET "+assignments(false)+"",args.toArray());
  }
  changed+=execute("MERGE INTO "+aux("CLASIFICACION")+" t USING(SELECT /*+ LEADING(v n) USE_NL(n) NO_PARALLEL */ v.ID VALUE_ID,n.\"CreationUserID\",n.\"CreationTimestamp\",n.\"ModificationUserID\",n.\"ModificationTimestamp\" FROM "+aux("CLASIFICACION")+" v JOIN PIM_MASTER.\"ArticleStructureMap\" n ON n.\"ID\"=v.ID WHERE v."+owner+" IN ("+marks(ids.size())+") AND (v.CreationTimestamp IS NULL OR v.ModificationTimestamp IS NULL)) s ON(t.ID=s.VALUE_ID) WHEN MATCHED THEN UPDATE SET "+assignments(false)+"",ids.toArray());
  return changed;
 }
 static void roots(int entity,boolean withValues)throws Exception{
  String phase=(withValues?"VALUES_":"ROOT_")+entity,table=prefix+(entity==1100?"PRODUCTO":"ARTICULO");long pos=checkpoint(phase),count=0,changed=0;
  while(true){List<Long> ids=new ArrayList<>();try(PreparedStatement p=c.prepareStatement("SELECT ID FROM "+table+" WHERE ID>? ORDER BY ID FETCH FIRST 900 ROWS ONLY")){p.setQueryTimeout(900);p.setLong(1,pos);try(ResultSet r=p.executeQuery()){while(r.next())ids.add(r.getLong(1));}}if(ids.isEmpty())break;
   long start=System.nanoTime();try{if(withValues)changed+=valuesAudit(entity,ids);else OmegaAudit.sourceRoots(read,c,prefix,entity,ids,true);c.commit();}catch(Exception e){c.rollback();throw e;}
   pos=ids.get(ids.size()-1);count+=ids.size();checkpoint(phase,pos);status(new JSONObject().put("phase",phase).put("lastId",pos).put("ownersThisRun",count).put("auditRowsUpdated",changed).put("batchMillis",(System.nanoTime()-start)/1000000));
  }status(new JSONObject().put("phase",phase).put("state","COMPLETE").put("lastId",pos).put("ownersThisRun",count));
 }
 static void catalogs()throws Exception{
  for(int i=0;i<catalogs.length();i++){JSONObject spec=catalogs.getJSONObject(i);String suffix=spec.getString("suffix"),target=prefix+"CAT_"+suffix,source=spec.getString("source"),phase="CAT_"+suffix;if(!exists(target))continue;Set<String> sourceCols=columns("PIM_MAIN",source);List<String> select=new ArrayList<>();for(String name:OmegaAudit.CONTROL)select.add((sourceCols.contains(name)?"n."+q(name):"CAST(NULL AS "+(name.endsWith("ID")?"NUMBER":"TIMESTAMP")+")")+" "+q(name));long pos=checkpoint(phase),count=0;
   while(true){List<Long> ids=new ArrayList<>();try(PreparedStatement p=c.prepareStatement("SELECT \"ID\" FROM "+target+" WHERE \"ID\">? ORDER BY \"ID\" FETCH FIRST 900 ROWS ONLY")){p.setLong(1,pos);try(ResultSet r=p.executeQuery()){while(r.next())ids.add(r.getLong(1));}}if(ids.isEmpty())break;
    int n=execute("MERGE INTO "+target+" t USING(SELECT n.\"ID\","+String.join(",",select)+" FROM PIM_MAIN."+q(source)+" n WHERE n.\"ID\" IN ("+marks(ids.size())+")) s ON(t.\"ID\"=s.\"ID\") WHEN MATCHED THEN UPDATE SET "+assignments(true)+" WHERE t.\"ModificationTimestamp\" IS NULL OR t.\"ModificationTimestamp\"<=s.\"ModificationTimestamp\"",ids.toArray());c.commit();pos=ids.get(ids.size()-1);count+=n;checkpoint(phase,pos);status(new JSONObject().put("phase",phase).put("lastId",pos).put("rowsThisRun",count));
   }
  }
 }
 public static void main(String[] args)throws Exception{
  if(args.length<4)throw new IllegalArgumentException("prod|dev install|roots|values|catalogs|all directory catalog-plan");boolean dev=args[0].equals("dev");if(!dev&&!args[0].equals("prod"))throw new IllegalArgumentException(args[0]);prefix=dev?"PIM_MASTER.EXP_PR_":"P360_EXPLOIT.";dir=Path.of(args[2]);Files.createDirectories(dir);catalogs=new JSONObject(Files.readString(Path.of(args[3]))).getJSONArray("tables");
  try(java.nio.channels.FileChannel lock=java.nio.channels.FileChannel.open(dir.resolve("maintenance.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);java.nio.channels.FileLock held=lock.tryLock()){
   if(held==null)throw new IllegalStateException("Backfill already running");
   c=dev?BackendConfig.connect():new mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager().openConnection(false);read=dev?BackendConfig.connect():new mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager().openConnection(false);c.setAutoCommit(false);read.setReadOnly(true);
   try(CallableStatement p=c.prepareCall("BEGIN DBMS_APPLICATION_INFO.SET_MODULE('OMEGA_AUDIT_BACKFILL',?); END;")){p.setString(1,args[1]);p.execute();}
   try{String action=args[1];if(action.equals("install")){install();return;}if(action.equals("roots")||action.equals("all")){roots(1100,false);roots(1000,false);}if(action.equals("values")||action.equals("all")){roots(1100,true);roots(1000,true);}if(action.equals("catalogs")||action.equals("all"))catalogs();status(new JSONObject().put("state","COMPLETE").put("action",action));}catch(Exception e){c.rollback();status(new JSONObject().put("state","FAILED").put("error",e.toString()));throw e;}finally{c.close();read.close();}
  }
 }
}
