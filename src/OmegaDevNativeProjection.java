import mx.com.liverpool.exploit.services.core.OmegaAudit;
import java.sql.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.zip.GZIPOutputStream;
import java.util.concurrent.Executors;
import org.json.*;

/** P360 read-only source -> explicitly authorized P360_EXPLOIT projection. */
public final class OmegaDevNativeProjection {
 static final String LIVE="TIMESTAMP '9999-12-31 00:00:00'", E="P360_EXPLOIT.";
 static final int BATCH=900;
 static final Set<String> TECH=Set.of("ID","ArticleRevisionID","ArticleID","EntityID","CreationUserID","CreationTimestamp","ModificationUserID","ModificationTimestamp","DeletionUserID","DeletionTimestamp","ChannelID","LanguageID","DomainID","Res_LK_Text100_01","Res_LK_Int_01");
 static final Map<String,String> LANG=Map.of("Res_Text250_01","ProductName","DescriptionShort","Name","DescriptionLong","DescriptionLong","Res_Text2G_01","DescriptionLong2");
 static final Map<String,String> DETAIL=Map.of("Res_Int_01","Business","Res_Int_02","SKU","Res_Int_03","PreviousStatus","Res_Int_04","ExternalStatus","EAN","EAN","CurrentStatus","CurrentStatus","Res_Text2G_02","EmbeddedCodeWAP","Res_Text2G_03","EmbeddedCodeWEB");
 static final Map<String,String> PROD_DOM=Map.of("Res_Int_01","Direction","Res_Int_02","Section","Res_Int_03","ItemGroup","Res_Int_04","ItemGroupS4H","Res_Int_05","BrandName","Res_Int_06","BRAND_ID_S4H","Res_Int_07","Negocio","Res_Int_08","ProductTypeSAP","Std_Int_10","Supplier","Res_Text250_01","SupplierPartNumber");
 static final Map<String,String> ART_DOM=Map.of("Res_Int_01","TamanoUnico","Res_Int_02","ColoursLiverpoolAtt","Res_Int_03","ProductTypeSAP","Res_Text250_01","SupplierPartNumber");
 static List<Long> selected = new ArrayList<>(); static Path dir; static String run; static Connection read,write;
 static Map<String,JSONObject> chars=new HashMap<>(),byName=new HashMap<>();
 static Map<String,String> structures=new HashMap<>();
 static Map<Long,String> ownerIdentifiers=new HashMap<>();
 static Map<String,List<JSONObject>> eventLabels=new HashMap<>();
 static void loadEventLabels()throws Exception {
  eventLabels.clear();ownerIdentifiers.clear();Path f=dir.getParent().resolve("label-overrides.json");
  if(!Files.exists(f))return;JSONArray a=new JSONArray(Files.readString(f));
  for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);eventLabels.computeIfAbsent(j.getString("identifier")+"|"+j.getString("name"),k->new ArrayList<>()).add(j);}
 }
 static String eventLabel(long owner,JSONObject row,String table,String name,String language,String domain,String record,String parent,String code,String fallback)throws Exception {
  if(code==null)return fallback;List<JSONObject> candidates=eventLabels.get(ownerIdentifiers.get(owner)+"|"+name);if(candidates==null)return fallback;
  for(JSONObject j:candidates){
   if(!code.equals(j.getString("code")))continue;String kind=j.getString("kind");
   if(kind.equals("CHAR")){
    if(!table.equals("ArticleCharactValue")&&!table.equals("ArticleCharactValueLang"))continue;
    if(!Objects.equals(str(j,"language"),language)||!Objects.equals(str(j,"record"),record)||!Objects.equals(str(j,"parent"),parent))continue;
   }else if(!kind.equals(table)||j.has("domain")&&!Objects.equals(str(j,"domain"),domain))continue;
   // A delayed message must not overwrite a later source edit.
   Timestamp modified=ts(row);Timestamp event=Timestamp.valueOf(java.time.OffsetDateTime.parse(j.getString("at")).withOffsetSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime());
   if(modified!=null&&modified.after(event))continue;
   return j.getString("label");
  }return fallback;
 }
 static LinkedHashMap<String,JSONObject> lookups=new LinkedHashMap<>(1000,.75f,true){protected boolean removeEldestEntry(Map.Entry<String,JSONObject> e){return size()>100000;}};
 static PreparedStatement statement(Connection c,String sql)throws SQLException {
  String[][] tables={{"PRODUCTOATRIBUTOVALOR","productoValores"},{"ARTICULOATRIBUTOVALOR","articuloValores"},{"CLASIFICACION","clasificacion"},{"PRODUCTO","producto"},{"ARTICULO","articulo"}};
  for(String[] t:tables)sql=sql.replace(E+t[0],mx.com.liverpool.exploit.services.core.BackendConfig.object(t[1]));
  sql=sql.replace(E+"CONCILIACION_CHECKPOINT","PIM_MASTER.EXP_PR_NATIVE_CHECKPOINT");
  if(sql.contains(E))throw new SQLException("Unmapped auxiliary table in DEV projector");
  return c.prepareStatement(sql);
 }
 static Connection connect()throws Exception {
  if(!"dev".equals(mx.com.liverpool.exploit.services.core.BackendConfig.get("environment")))throw new SQLException("DEV projector requires DEV configuration");
  Connection c=new mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager().openConnection(false);
  c.setNetworkTimeout(Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r);t.setDaemon(true);return t;}),900000);return c;
 }
 static List<JSONObject> query(String sql,Object...args)throws Exception {
  flushUpdates();try(PreparedStatement p=statement(sql.contains(E)?write:read,sql)){p.setFetchSize(900);p.setQueryTimeout(900);for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);try(ResultSet r=p.executeQuery()){
   List<JSONObject> out=new ArrayList<>();ResultSetMetaData md=r.getMetaData();while(r.next()){JSONObject j=new JSONObject();for(int i=1;i<=md.getColumnCount();i++){
    Object v=r.getObject(i);if(v instanceof Clob){try(Reader cr=((Clob)v).getCharacterStream()){StringBuilder b=new StringBuilder();char[] a=new char[8192];int n;while((n=cr.read(a))!=-1)b.append(a,0,n);v=b.toString();}}
    else if(v!=null && (md.getColumnType(i)==Types.TIMESTAMP || md.getColumnType(i)==Types.TIMESTAMP_WITH_TIMEZONE))v=r.getTimestamp(i).toString();
    j.put(md.getColumnLabel(i),v==null?JSONObject.NULL:v);
   }out.add(j);}return out;}}
 }
 static String str(JSONObject j,String key){return j==null||j.isNull(key)?null:j.optString(key,null);}
 static Object num(JSONObject j,String key){return j==null||j.isNull(key)?null:j.get(key);}
 static Timestamp ts(JSONObject j){String v=str(j,"ModificationTimestamp");return v==null?null:Timestamp.valueOf(v);}
 static String marks(int n){return String.join(",",Collections.nCopies(n,"?"));}
 static List<JSONObject> rows(String table,String key,List<Long> ids)throws Exception {
  String hint=table.equals("ArticleCharactValue")?"INDEX(t IX_ACV_TUNE_02)":table.equals("ArticleCharactValueLang")?"INDEX(t XAK1_ArticleCharactValueLang)":table.equals("ArticleReference")?"INDEX(t XIE3_ArticleReference)":"";
  return query("SELECT /*+ NO_PARALLEL(t) "+hint+" */ t.* FROM PIM_MASTER.\""+table+"\" t WHERE t.\""+key+"\" IN ("+marks(ids.size())+") AND t.\"DeletionTimestamp\"="+LIVE,ids.toArray());
 }
 static final LinkedHashMap<String,PreparedStatement> pendingUpdates=new LinkedHashMap<>();
 static int pendingCount;
 static void update(String sql,Object...args)throws Exception{
  if(!sql.contains(E))throw new IllegalArgumentException("Writes restricted to EXPLOIT");
  PreparedStatement p=pendingUpdates.get(sql);if(p==null){p=statement(write,sql);p.setQueryTimeout(900);pendingUpdates.put(sql,p);}
  for(int i=0;i<args.length;i++){if(args[i]==OmegaAudit.NULL_TIMESTAMP)p.setNull(i+1,Types.TIMESTAMP);else p.setObject(i+1,args[i]);}p.addBatch();if(++pendingCount>=BATCH)flushUpdates();
 }
 static void clearUpdates()throws SQLException{for(PreparedStatement p:pendingUpdates.values())p.close();pendingUpdates.clear();pendingCount=0;}
 static void flushUpdates()throws SQLException{
  try{for(PreparedStatement p:pendingUpdates.values())p.executeBatch();}finally{clearUpdates();}
 }
 static boolean equalValue(Object a,Object b){if(a==JSONObject.NULL)a=null;if(b==JSONObject.NULL)b=null;return Objects.equals(a==null?null:a.toString(),b==null?null:b.toString());}
 static Map<Long,JSONObject> existingRoots(String table,List<Long> ids)throws Exception{
  Map<Long,JSONObject> out=new HashMap<>();for(JSONObject row:query("SELECT * FROM "+E+table+" WHERE ID IN ("+marks(ids.size())+")",ids.toArray()))out.put(row.getLong("ID"),row);return out;
 }
 static void changeRoot(String table,long id,JSONObject previous,Map<String,Object> wanted)throws Exception{
  List<String> setters=new ArrayList<>();List<Object> args=new ArrayList<>();
  for(var e:wanted.entrySet())if(!equalValue(previous.opt(e.getKey().toUpperCase(Locale.ROOT)),e.getValue())){setters.add(e.getKey()+"=?");args.add(e.getValue());}
  if(setters.isEmpty())return;args.add(id);update("UPDATE "+E+table+" SET "+String.join(",",setters)+",CapturedAt=SYSTIMESTAMP WHERE ID=?",args.toArray());
 }
 static void classificationDelta(int entity,List<Long> owners,List<JSONObject> source)throws Exception{
  String owner=entity==1100?"ProductoID":"ArticuloID";Map<Long,JSONObject> previous=new HashMap<>();
  for(JSONObject r:query("SELECT * FROM "+E+"CLASIFICACION WHERE "+owner+" IN ("+marks(owners.size())+")",owners.toArray()))previous.put(r.getLong("ID"),r);
  for(JSONObject r:source){String structure=structures.get(str(r,"StructureID"));if(structure==null)structure="StructureID:"+str(r,"StructureID");if(structure.equals("PrimaryProductTaxonomy"))continue;
   long id=r.getLong("ID"),who=r.getLong("ArticleRevisionID");JSONObject old=previous.remove(id);String group=str(r,"StructureGroupIdentifier");
   if(old==null)update("INSERT INTO "+E+"CLASIFICACION(ID,"+owner+",StructureIdentifier,StructureGroupIdentifier,SourceModifiedAt,CreationUserID,CreationTimestamp,ModificationUserID,ModificationTimestamp) VALUES(?,?,?,?,?,?,?,?,?)",id,who,structure,group,OmegaAudit.nullableTimestamp(r,"ModificationTimestamp"),num(r,"CreationUserID"),OmegaAudit.nullableTimestamp(r,"CreationTimestamp"),num(r,"ModificationUserID"),OmegaAudit.nullableTimestamp(r,"ModificationTimestamp"));
   else if(!equalValue(old.opt(owner.toUpperCase(Locale.ROOT)),who)||!equalValue(old.opt("STRUCTUREIDENTIFIER"),structure)||!equalValue(old.opt("STRUCTUREGROUPIDENTIFIER"),group)||!equalValue(old.opt("SOURCEMODIFIEDAT"),ts(r))||old.isNull("CREATIONTIMESTAMP")||!equalValue(old.opt("MODIFICATIONUSERID"),num(r,"ModificationUserID")))update("UPDATE "+E+"CLASIFICACION SET "+owner+"=?,StructureIdentifier=?,StructureGroupIdentifier=?,SourceModifiedAt=?,CreationUserID=COALESCE(CreationUserID,?),CreationTimestamp=COALESCE(CreationTimestamp,?),ModificationUserID=?,ModificationTimestamp=? WHERE ID=?",who,structure,group,OmegaAudit.nullableTimestamp(r,"ModificationTimestamp"),num(r,"CreationUserID"),OmegaAudit.nullableTimestamp(r,"CreationTimestamp"),num(r,"ModificationUserID"),OmegaAudit.nullableTimestamp(r,"ModificationTimestamp"),id);
  }
  for(long id:previous.keySet())update("DELETE FROM "+E+"CLASIFICACION WHERE ID=?",id);
 }
 static long checkpoint(String phase)throws Exception{try(PreparedStatement p=statement(write,"SELECT LastID FROM "+E+"CONCILIACION_CHECKPOINT WHERE RunID=? AND Fase=?")){p.setString(1,run);p.setString(2,phase);try(ResultSet r=p.executeQuery()){return r.next()?r.getLong(1):0;}}}
 static void checkpoint(String phase,long last,long count)throws Exception {
  update("MERGE INTO "+E+"CONCILIACION_CHECKPOINT t USING (SELECT ? RunID,? Fase FROM dual)s ON(t.RunID=s.RunID AND t.Fase=s.Fase) WHEN MATCHED THEN UPDATE SET LastID=?,RowCount=RowCount+?,UpdatedAt=SYSTIMESTAMP WHEN NOT MATCHED THEN INSERT(RunID,Fase,LastID,RowCount) VALUES(s.RunID,s.Fase,?,?)",run,phase,last,count,last,count);
 }
 static void status(String phase,long id,long count)throws Exception{JSONObject j=new JSONObject().put("run",run).put("phase",phase).put("lastId",id).put("batchRows",count).put("at",java.time.Instant.now().toString());Path tmp=dir.resolve("status.tmp");Files.writeString(tmp,j.toString());Files.move(tmp,dir.resolve("status.json"),StandardCopyOption.REPLACE_EXISTING);System.out.println(j);}
 static long dimensionAt;
 static void dimensions()throws Exception {
  for(JSONObject j:query("SELECT \"ID\",\"CharacteristicID\",\"Identifier\",\"LookupID\" FROM PIM_MAIN.\"CharacteristicRevision\" WHERE \"RevisionID\"=1 AND \"DeletionTimestamp\"="+LIVE)){chars.put(str(j,"CharacteristicID"),j);byName.put(str(j,"Identifier"),j);}
  for(JSONObject j:query("SELECT \"StructureID\",\"Identifier\" FROM PIM_MAIN.\"StructureRevision\" WHERE \"RevisionID\"=1 AND \"DeletionTimestamp\"="+LIVE))structures.put(str(j,"StructureID"),str(j,"Identifier"));
 }
 static void lookup(Collection<String> requested)throws Exception {
  List<String> need=new ArrayList<>();for(String id:new LinkedHashSet<>(requested))if(id!=null&&!lookups.containsKey(id))need.add(id);
  for(int off=0;off<need.size();off+=900){List<String> part=need.subList(off,Math.min(off+900,need.size()));
   for(String id:part)lookups.put(id,new JSONObject());
   for(JSONObject j:query("SELECT /*+ NO_PARALLEL(v) */ v.\"LookupValueID\" ID,v.\"Code\" CODE,l.\"LanguageID\" LANG,l.\"Name\" LABEL FROM PIM_MAIN.\"LookupValueRevision\" v LEFT JOIN PIM_MAIN.\"LookupValueLang\" l ON l.\"LookupValueRevisionID\"=v.\"ID\" AND l.\"DeletionTimestamp\"="+LIVE+" WHERE v.\"LookupValueID\" IN ("+marks(part.size())+") AND v.\"RevisionID\"=1 AND v.\"DeletionTimestamp\"="+LIVE,part.toArray())){
    JSONObject x=lookups.get(str(j,"ID"));x.put("code",j.opt("CODE"));if(!j.isNull("LANG"))x.put(str(j,"LANG"),j.opt("LABEL"));
   }
  }
 }
 static String label(String id,String language){JSONObject j=lookups.get(id);return j==null?null:str(j,language==null?"10":language);}
 static String code(String id){return str(lookups.get(id),"code");}
 static String hash(String s)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}
 static Map<Long,List<JSONObject>> index(List<JSONObject> rows,String key){Map<Long,List<JSONObject>> m=new HashMap<>();for(JSONObject j:rows)m.computeIfAbsent(j.getLong(key),k->new ArrayList<>()).add(j);return m;}
 static JSONObject single(Map<Long,List<JSONObject>> m,long id,String section)throws Exception{List<JSONObject> a=m.getOrDefault(id,List.of());if(a.size()>1)throw new IOException("MULTIPLE_"+section+" for "+id);return a.isEmpty()?new JSONObject():a.get(0);}
 static void removeStaleIdentityCollisions(int entity,List<JSONObject> nativeRows)throws Exception {
  if(nativeRows.isEmpty())return;
  String table=entity==1100?"PRODUCTO":"ARTICULO",owner=entity==1100?"ProductoID":"ArticuloID";
  Map<String,Long> current=new HashMap<>();Set<String> identifiers=new LinkedHashSet<>(),catalogs=new LinkedHashSet<>();
  for(JSONObject r:nativeRows){catalogs.add(Long.toString(r.getLong("CatalogID")));identifiers.add(str(r,"Identifier"));current.put(str(r,"Identifier")+"|"+str(r,"CatalogID")+"|"+str(r,"RevisionID"),r.getLong("ID"));}
  List<JSONObject> collisions=new ArrayList<>();
  for(JSONObject old:query("SELECT ID,Identifier,CatalogID,RevisionID FROM "+E+table+" WHERE CatalogID IN ("+String.join(",",catalogs)+") AND RevisionID=1 AND Identifier IN ("+marks(identifiers.size())+") ORDER BY ID",identifiers.toArray())){
   Long desired=current.get(str(old,"IDENTIFIER")+"|"+str(old,"CATALOGID")+"|"+str(old,"REVISIONID"));
   if(desired!=null&&desired.longValue()!=old.getLong("ID"))collisions.add(old);
  }
  for(JSONObject old:collisions){long id=old.getLong("ID");
   // Serialize with other writers, then prove the native owner has actually disappeared.
   List<JSONObject> locked=query("SELECT ID,Identifier FROM "+E+table+" WHERE ID=? FOR UPDATE WAIT 60",id);
   if(locked.isEmpty()||!Objects.equals(str(locked.get(0),"IDENTIFIER"),str(old,"IDENTIFIER")))continue;
   if(!query("SELECT \"ID\" FROM PIM_MASTER.\"ArticleRevision\" WHERE \"ID\"=? AND \"DeletionTimestamp\"="+LIVE,id).isEmpty())throw new IOException("ACTIVE_NATIVE_IDENTITY_COLLISION "+old);
   debt("STALE_OMEGA_IDENTITY_REMOVED",id,old);
   update("DELETE FROM "+E+(entity==1100?"PRODUCTOATRIBUTOVALOR":"ARTICULOATRIBUTOVALOR")+" WHERE "+owner+"=?",id);
   update("DELETE FROM "+E+"CLASIFICACION WHERE "+owner+"=?",id);
   if(entity==1100){update("UPDATE "+E+"ARTICULO SET ProductoID=NULL WHERE ProductoID=?",id);update("UPDATE "+E+"PRODUCTO SET TargetRecord=NULL WHERE TargetRecord=?",id);}else update("UPDATE "+E+"ARTICULO SET TargetRecord=NULL WHERE TargetRecord=?",id);
   update("DELETE FROM "+E+table+" WHERE ID=?",id);flushUpdates();
  }
 }
 static void roots(int entity)throws Exception {
  String target=entity==1100?"PRODUCTO":"ARTICULO";
  List<JSONObject> ar=query("SELECT * FROM PIM_MASTER.\"ArticleRevision\" WHERE \"ID\" IN ("+marks(selected.size())+") AND \"EntityID\"=? AND \"RevisionID\"=1 AND \"DeletionTimestamp\"="+LIVE+" ORDER BY \"ID\"",withEntity(selected,entity));
  List<Long> ids=new ArrayList<>();for(JSONObject a:ar)ids.add(a.getLong("ID"));if(ids.isEmpty())return;
  removeStaleIdentityCollisions(entity,ar);
  Map<Long,List<JSONObject>> details=index(rows("ArticleDetail","ArticleRevisionID",ids),"ArticleRevisionID");
  List<String> lk=new ArrayList<>();for(List<JSONObject> list:details.values())for(JSONObject j:list){lk.add(str(j,"Res_Int_01"));lk.add(str(j,"Res_Int_04"));}lookup(lk);
  Map<Long,JSONObject> existing=existingRoots(target,ids);
  for(JSONObject a:ar)if(!existing.containsKey(a.getLong("ID"))){
   // Concurrent children can discover the same absent parent. The duplicate branch
   // accepts only the identical native identity and locks it before loading values.
   update("DECLARE existing_id NUMBER; BEGIN BEGIN INSERT INTO "+E+target+"(ID,ArticleID,Identifier,P360ArticleRevisionID,RevisionID,CatalogID) VALUES(?,?,?,?,?,?); EXCEPTION WHEN DUP_VAL_ON_INDEX THEN SELECT ID INTO existing_id FROM "+E+target+" WHERE ID=? AND ArticleID=? AND Identifier=? AND RevisionID=? AND CatalogID=? FOR UPDATE; END; END;",a.getLong("ID"),num(a,"ArticleID"),str(a,"Identifier"),a.getLong("ID"),num(a,"RevisionID"),num(a,"CatalogID"),a.getLong("ID"),num(a,"ArticleID"),str(a,"Identifier"),num(a,"RevisionID"),num(a,"CatalogID"));
  }
  flushUpdates();existing=existingRoots(target,ids);
  for(JSONObject a:ar){long id=a.getLong("ID");JSONObject d=single(details,id,"DETAIL");
   JSONObject previous=existing.get(id);
   if(previous==null)throw new IOException("Baseline identity was not created: "+id);
   Map<String,Object> wanted=new LinkedHashMap<>();String[] keys={"Identifier","CurrentStatus","PreviousStatus","ExternalStatus","Business","SKU","EAN","Supplier","SourceModifiedAt"};
   Object[] vals={str(a,"Identifier"),num(d,"CurrentStatus"),num(d,"Res_Int_03"),code(str(d,"Res_Int_04")),label(str(d,"Res_Int_01"),"10"),str(d,"Res_Int_02"),str(d,"EAN"),str(a,"MainSupplierID"),ts(a)};
   for(int i=0;i<keys.length;i++)wanted.put(keys[i],vals[i]);changeRoot(target,id,previous,wanted);
  }
  flushUpdates();OmegaAudit.sourceRoots(read,write,OmegaAudit.prefix(mx.com.liverpool.exploit.services.core.BackendConfig.object("producto")),entity,ids,true);
 }
 static Object[] withEntity(List<Long> ids,int entity){List<Object>a=new ArrayList<>(ids);a.add(entity);return a.toArray();}
 static final class Values implements AutoCloseable {
  PreparedStatement p,delete,modify;int pending;long unchanged,changed,removed;
  Map<String,JSONObject> previous=new HashMap<>();
  static final String[] COLS={"ID","OWNER_ID","ATRIBUTOID","ATRIBUTOIDENTIFIER","LANGUAGEKEY","DOMAINKEY","RECORDKEY","PARENTRECORDKEY","OCCURRENCEKEY","UNITID","CODE","VALUE","LONGVALUE","SOURCETABLE","SOURCEROWID","SOURCECOLUMN","SOURCEMODIFIEDAT","CREATIONUSERID","CREATIONTIMESTAMP","MODIFICATIONUSERID","MODIFICATIONTIMESTAMP"};
  boolean same(JSONObject old,Object[] a){for(int i=0;i<a.length;i++){Object b=old.opt(COLS[i]);if(b==JSONObject.NULL)b=null;if(!Objects.equals(a[i]==null?null:a[i].toString(),b==null?null:b.toString()))return false;}return true;}
  Values(int entity,List<Long> owners)throws Exception{this(entity,owners,"FULL");}
  Values(int entity,List<Long> owners,String scope)throws Exception{String target=entity==1100?"PRODUCTOATRIBUTOVALOR":"ARTICULOATRIBUTOVALOR",owner=entity==1100?"ProductoID":"ArticuloID";
   for(JSONObject old:query("SELECT /*+ INDEX(v "+(entity==1100?"IX_EXP_PRO_VAL_OWNER":"IX_EXP_ART_VAL_OWNER")+") */ v.*,v."+owner+" OWNER_ID FROM "+E+target+" v WHERE v."+owner+" IN ("+marks(owners.size())+")"+(scope.equals("CHAR")?" AND SourceTable IN ('ArticleCharactValue','ArticleCharactValueLang')":scope.equals("TEXT")?" AND SourceTable='ArticleLang'":scope.equals("DETAIL")?" AND SourceTable='ArticleDetail'":scope.equals("DOMAIN")?" AND SourceTable='ArticleDomain'":""),owners.toArray()))previous.put(old.getString("ID"),old);
   delete=statement(write,"DELETE FROM "+E+target+" WHERE ID=?");delete.setQueryTimeout(900);
   List<String> assignments=new ArrayList<>();for(int i=1;i<COLS.length;i++)assignments.add((i==1?owner:COLS[i])+"=?");
   modify=statement(write,"UPDATE "+E+target+" SET "+String.join(",",assignments)+" WHERE ID=?");modify.setQueryTimeout(900);
   p=statement(write,"INSERT INTO "+E+target+"(ID,"+owner+",AtributoID,AtributoIdentifier,LanguageKey,DomainKey,RecordKey,ParentRecordKey,OccurrenceKey,UnitID,Code,Value,LongValue,SourceTable,SourceRowID,SourceColumn,SourceModifiedAt,CreationUserID,CreationTimestamp,ModificationUserID,ModificationTimestamp) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");p.setQueryTimeout(900);
  }
  void add(long owner,JSONObject row,String table,String col,String name,JSONObject attr,String language,String domain,String record,String parent,String occurrence,String unit,String code,String value)throws Exception{
   value=eventLabel(owner,row,table,name,language,domain,record,parent,code,value);
   if(code==null&&value==null)return;String longValue=null;if(value!=null&&value.getBytes(StandardCharsets.UTF_8).length>4000){longValue=value;value=null;}
   if(code!=null&&code.getBytes(StandardCharsets.UTF_8).length>4000)throw new IOException("CODE_OVER_4000_BYTES "+name);
   Object[] a={hash(table+"|"+row.get("ID")+"|"+col+"|"+Objects.toString(language,"")),owner,num(attr,"ID"),name,language,domain,record,parent,occurrence,unit,code,value,longValue,table,row.get("ID"),col,ts(row),num(row,"CreationUserID"),OmegaAudit.timestamp(row,"CreationTimestamp"),num(row,"ModificationUserID"),ts(row)};
   JSONObject old=previous.remove(a[0].toString());if(old!=null&&same(old,a)){unchanged++;return;}
   changed++;PreparedStatement stmt=old==null?p:modify;
   if(old==null){for(int i=0;i<a.length;i++)bind(stmt,i+1,a[i],i);}
   else{for(int i=1;i<a.length;i++)bind(stmt,i,a[i],i);stmt.setString(a.length,a[0].toString());}
   stmt.addBatch();if(++pending>=BATCH)flush();
  }
  void bind(PreparedStatement stmt,int i,Object value,int column)throws Exception{if(Set.of(16,18,20).contains(column)){stmt.setTimestamp(i,(Timestamp)value);return;}if(column==12){if(value==null)stmt.setNull(i,Types.NCLOB);else stmt.setNCharacterStream(i,new StringReader(value.toString()));}else stmt.setObject(i,value);}
  void flush()throws Exception{if(pending>0){modify.executeBatch();p.executeBatch();pending=0;}}
  void finish()throws Exception{flush();int n=0;for(String id:previous.keySet()){delete.setString(1,id);delete.addBatch();removed++;if(++n%900==0)delete.executeBatch();}if(n%900!=0)delete.executeBatch();previous.clear();System.out.println(new JSONObject().put("valueDeltaUnchanged",unchanged).put("valueDeltaWritten",changed).put("valueDeltaRemoved",removed));}
  public void close()throws Exception{p.close();delete.close();modify.close();}
 }
 static void debt(String reason,long owner,Object detail)throws Exception {
  JSONObject j=new JSONObject().put("reason",reason).put("owner",owner).put("detail",detail).put("at",java.time.Instant.now().toString());
  Files.writeString(dir.resolve("debt.jsonl"),j+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
 }
 static boolean extra(JSONObject r,int entity){return r.optInt("EntityID")== (entity==1100?21006:21106) && "MX".equals(str(r,"TargetMarket"));}
 static void nativeValues(Values v,int entity,long owner,JSONObject r,String table)throws Exception {
  Map<String,String> mapping=table.equals("ArticleLang")?LANG:table.equals("ArticleDetail")?DETAIL:extra(r,entity)?(entity==1100?PROD_DOM:ART_DOM):Map.of();
  String language=str(r,"LanguageID"),domain=str(r,"TargetMarket");if(domain==null)domain=str(r,"DomainID");
  if(table.equals("ArticleDetail")){
   v.add(owner,r,table,"CreationTimestamp","CreadoEl",null,null,null,null,null,str(r,"ID"),null,null,str(r,"CreationTimestamp"));
   v.add(owner,r,table,"ModificationTimestamp","ModificadoEl",null,null,null,null,null,str(r,"ID"),null,null,str(r,"ModificationTimestamp"));
  }
  for(Object rawCol:r.keySet()){String col=rawCol.toString();if(TECH.contains(col)||r.isNull(col))continue;
   String name=mapping.getOrDefault(col,table+"."+col),val=str(r,col),c=null;
   boolean lk=table.equals("ArticleDetail")&&(col.equals("Res_Int_01")||col.equals("Res_Int_04"))||table.equals("ArticleDomain")&&mapping.containsKey(col)&&!col.equals("Res_Text250_01");
   if(lk){c=code(val);String l=label(val,language);if(c==null){debt("LOOKUP_NOT_RESOLVED",owner,new JSONObject().put("table",table).put("column",col).put("lookupId",val));}else val=l;}
   v.add(owner,r,table,col,name,byName.get(name),language,domain,null,null,str(r,"ID"),null,c,val);
  }
 }
 static void values(int entity)throws Exception {
  String phase="VALUES_"+entity,target=entity==1100?"PRODUCTO":"ARTICULO";long last=checkpoint(phase);
  {List<JSONObject> captured=query("SELECT \"ID\" ID FROM PIM_MASTER.\"ArticleRevision\" WHERE \"ID\" IN ("+marks(selected.size())+") AND \"EntityID\"=? AND \"RevisionID\"=1 AND \"DeletionTimestamp\"="+LIVE+" ORDER BY \"ID\"",withEntity(selected,entity));if(captured.isEmpty())return;
   List<Long> capturedIds=new ArrayList<>();for(JSONObject j:captured)capturedIds.add(j.getLong("ID"));
   List<JSONObject> ar=rows("ArticleRevision","ID",capturedIds);
   if(ar.isEmpty())return;
   List<Long> ids=new ArrayList<>();for(JSONObject a:ar)ids.add(a.getLong("ID"));Map<String,List<JSONObject>> source=new LinkedHashMap<>();source.put("ArticleRevision",ar);
   for(String table:List.of("ArticleDetail","ArticleLang","ArticleDomain","ArticleStructureMap","ArticleReference","ArticleCharactValue"))source.put(table,rows(table,"ArticleRevisionID",ids));
   List<Long> acvIds=new ArrayList<>();Map<Long,JSONObject> acvById=new HashMap<>();for(JSONObject j:source.get("ArticleCharactValue")){acvIds.add(j.getLong("ID"));acvById.put(j.getLong("ID"),j);}
   List<JSONObject> acvl=new ArrayList<>();for(int i=0;i<acvIds.size();i+=900)acvl.addAll(rows("ArticleCharactValueLang","ArticleCharactValueID",acvIds.subList(i,Math.min(acvIds.size(),i+900))));source.put("ArticleCharactValueLang",acvl);
   Set<String> lk=new HashSet<>();for(var e:source.entrySet())for(JSONObject r:e.getValue()){
    if(e.getKey().equals("ArticleCharactValue")||e.getKey().equals("ArticleCharactValueLang"))lk.add(str(r,"LookupValueID"));
    if(e.getKey().equals("ArticleDetail")){lk.add(str(r,"Res_Int_01"));lk.add(str(r,"Res_Int_04"));}
    if(e.getKey().equals("ArticleDomain"))for(String col:(entity==1100?PROD_DOM:ART_DOM).keySet())if(!col.equals("Res_Text250_01"))lk.add(str(r,col));
   }lookup(lk);

   Map<Long,JSONObject> previousRoots=existingRoots(target,ids);
   Map<Long,Map<String,Object>> wantedRoots=new LinkedHashMap<>();
   for(long id:ids){Map<String,Object> wanted=new LinkedHashMap<>();for(String key:List.of("PrimaryProductTaxonomyGroup","SupplierPartNumber","AlternativeIdentifier","FirstDateApproved","LastDateApproved"))wanted.put(key,null);if(entity==1000)for(String key:List.of("ProductoID","Color","SizeValue"))wanted.put(key,null);wantedRoots.put(id,wanted);}
   try(Values v=new Values(entity,ids)){
    for(String table:List.of("ArticleDetail","ArticleLang","ArticleDomain"))for(JSONObject r:source.get(table))nativeValues(v,entity,r.getLong("ArticleRevisionID"),r,table);
    Set<String> deprecated=new HashSet<>(LANG.values());deprecated.addAll(DETAIL.values());deprecated.addAll((entity==1100?PROD_DOM:ART_DOM).values());
    for(String table:List.of("ArticleCharactValue","ArticleCharactValueLang"))for(JSONObject r:source.get(table)){
     JSONObject base=table.equals("ArticleCharactValue")?r:acvById.get(r.getLong("ArticleCharactValueID"));JSONObject attr=chars.get(str(base,"CharacteristicID"));if(attr==null)debt("CHARACTERISTIC_NOT_RESOLVED",base.getLong("ArticleRevisionID"),base);String name=attr==null?"CharacteristicID:"+str(base,"CharacteristicID"):str(attr,"Identifier");if(deprecated.contains(name))continue;
     String language=str(r,"LanguageID"),lookupId=str(r,"LookupValueID"),c=null,val=str(r,"Value");if(lookupId!=null){c=code(lookupId);if(c==null){debt("CHAR_LOOKUP_NOT_RESOLVED",base.getLong("ArticleRevisionID"),r);if(val==null)val=lookupId;}else val=label(lookupId,language);}
     v.add(base.getLong("ArticleRevisionID"),r,table,"Value",name,attr,language,null,str(base,"RecordKey"),str(base,"ParentRecordKey"),str(base,"ID"),str(base,"UnitID"),c,val);
    }v.finish();
    for(JSONObject r:source.get("ArticleDetail")){Map<String,Object> wanted=wantedRoots.get(r.getLong("ArticleRevisionID"));wanted.put("AlternativeIdentifier",str(r,"SupplierAltAID"));wanted.put("FirstDateApproved",str(r,"Res_DateTime_02")==null?null:Timestamp.valueOf(str(r,"Res_DateTime_02")));wanted.put("LastDateApproved",str(r,"Res_DateTime_01")==null?null:Timestamp.valueOf(str(r,"Res_DateTime_01")));}
    for(JSONObject r:source.get("ArticleDomain")){if(!extra(r,entity))continue;Map<String,Object> wanted=wantedRoots.get(r.getLong("ArticleRevisionID"));wanted.put("SupplierPartNumber",str(r,"Res_Text250_01"));if(entity==1000){wanted.put("Color",label(str(r,"Res_Int_02"),"10"));wanted.put("SizeValue",label(str(r,"Res_Int_01"),"10"));}}
    for(JSONObject r:source.get("ArticleStructureMap"))if("PrimaryProductTaxonomy".equals(structures.get(str(r,"StructureID"))))wantedRoots.get(r.getLong("ArticleRevisionID")).put("PrimaryProductTaxonomyGroup",str(r,"StructureGroupIdentifier"));
    classificationDelta(entity,ids,source.get("ArticleStructureMap"));
    if(entity==1000){
     Set<String> parentIdentifiers=new HashSet<>();for(JSONObject r:source.get("ArticleReference"))if(r.optInt("RefEntityID")==1100)parentIdentifiers.add(str(r,"RefExtArtIdentifier"));parentIdentifiers.remove(null);
     Map<String,Long> found=new HashMap<>();List<String> requested=new ArrayList<>(parentIdentifiers);
     for(int i=0;i<requested.size();i+=900){List<String> part=requested.subList(i,Math.min(i+900,requested.size()));for(JSONObject p:query("SELECT ID,Identifier,ArticleID,CatalogID FROM "+E+"PRODUCTO WHERE Identifier IN ("+marks(part.size())+") AND RevisionID=1",part.toArray()))found.put(str(p,"IDENTIFIER")+"|"+str(p,"ARTICLEID")+"|"+str(p,"CATALOGID"),p.getLong("ID"));}
     Set<Long> unresolvedParents=new HashSet<>();Map<Long,Set<Long>> parents=new HashMap<>();for(JSONObject r:source.get("ArticleReference"))if(r.optInt("RefEntityID")==1100){
      Long parent=found.get(str(r,"RefExtArtIdentifier")+"|"+str(r,"RefIntArtID")+"|"+str(r,"RefIntCatID"));
      if(parent==null){debt("PARENT_NOT_CAPTURED",r.getLong("ArticleRevisionID"),r);unresolvedParents.add(r.getLong("ArticleRevisionID"));}else parents.computeIfAbsent(r.getLong("ArticleRevisionID"),k->new HashSet<>()).add(parent);
     }
     for(var e:parents.entrySet()){if(e.getValue().size()!=1){debt("MULTIPLE_PRODUCT_PARENTS",e.getKey(),e.getValue());unresolvedParents.add(e.getKey());continue;}wantedRoots.get(e.getKey()).put("ProductoID",e.getValue().iterator().next());}
     for(long unresolved:unresolvedParents)wantedRoots.get(unresolved).remove("ProductoID");
    }
    for(long id:ids)changeRoot(target,id,previousRoots.get(id),wantedRoots.get(id));
    flushUpdates();
    last=capturedIds.get(capturedIds.size()-1);status(phase,last,ar.size());
   }catch(Exception e){clearUpdates();write.rollback();throw e;}
  }
 }
 static void partialValues(int entity,List<Long> ids,String scope)throws Exception{
  if(ids.isEmpty())return;
  flushUpdates();OmegaAudit.sourceRoots(read,write,OmegaAudit.prefix(mx.com.liverpool.exploit.services.core.BackendConfig.object("producto")),entity,ids,Set.of("ROOT","DETAIL").contains(scope));
  if(Set.of("ROOT","DETAIL","DOMAIN","CLASS","REL").contains(scope)){section(entity,ids,scope);return;}
  if(scope.equals("TEXT")){try(Values v=new Values(entity,ids,scope)){for(JSONObject r:rows("ArticleLang","ArticleRevisionID",ids))nativeValues(v,entity,r.getLong("ArticleRevisionID"),r,"ArticleLang");v.finish();}return;}
  List<JSONObject> base=rows("ArticleCharactValue","ArticleRevisionID",ids);Map<Long,JSONObject> byID=new HashMap<>();List<Long> charIds=new ArrayList<>();for(JSONObject r:base){byID.put(r.getLong("ID"),r);charIds.add(r.getLong("ID"));}
  List<JSONObject> lang=new ArrayList<>();for(int i=0;i<charIds.size();i+=BATCH)lang.addAll(rows("ArticleCharactValueLang","ArticleCharactValueID",charIds.subList(i,Math.min(i+BATCH,charIds.size()))));
  Set<String> lk=new HashSet<>();for(List<JSONObject> list:List.of(base,lang))for(JSONObject r:list)lk.add(str(r,"LookupValueID"));lookup(lk);
  Set<String> deprecated=new HashSet<>(LANG.values());deprecated.addAll(DETAIL.values());deprecated.addAll((entity==1100?PROD_DOM:ART_DOM).values());
  try(Values v=new Values(entity,ids,scope)){
   for(String table:List.of("ArticleCharactValue","ArticleCharactValueLang"))for(JSONObject r:table.equals("ArticleCharactValue")?base:lang){JSONObject b=table.equals("ArticleCharactValue")?r:byID.get(r.getLong("ArticleCharactValueID"));JSONObject attr=chars.get(str(b,"CharacteristicID"));String name=attr==null?"CharacteristicID:"+str(b,"CharacteristicID"):str(attr,"Identifier");if(deprecated.contains(name))continue;
    String language=str(r,"LanguageID"),lookupId=str(r,"LookupValueID"),c=null,val=str(r,"Value");if(lookupId!=null){c=code(lookupId);if(c==null){debt("CHAR_LOOKUP_NOT_RESOLVED",b.getLong("ArticleRevisionID"),r);if(val==null)val=lookupId;}else val=label(lookupId,language);}
    v.add(b.getLong("ArticleRevisionID"),r,table,"Value",name,attr,language,null,str(b,"RecordKey"),str(b,"ParentRecordKey"),str(b,"ID"),str(b,"UnitID"),c,val);
   }v.finish();
  }
 }

 static void section(int entity,List<Long> ids,String scope)throws Exception {
  String target=entity==1100?"PRODUCTO":"ARTICULO";
  if(scope.equals("ROOT")){selected=ids;roots(entity);section(entity,ids,"DETAIL");return;}
  if(scope.equals("CLASS")){
   List<JSONObject> maps=rows("ArticleStructureMap","ArticleRevisionID",ids);classificationDelta(entity,ids,maps);
   Map<Long,String> ppt=new HashMap<>();for(JSONObject r:maps)if("PrimaryProductTaxonomy".equals(structures.get(str(r,"StructureID"))))ppt.put(r.getLong("ArticleRevisionID"),str(r,"StructureGroupIdentifier"));
   for(long id:ids)update("UPDATE "+E+target+" SET PrimaryProductTaxonomyGroup=?,CapturedAt=SYSTIMESTAMP WHERE ID=?",ppt.get(id),id);return;
  }
  if(scope.equals("REL")){
   if(entity!=1000)return;
   List<JSONObject> refs=rows("ArticleReference","ArticleRevisionID",ids);Map<Long,Set<Long>> parents=new HashMap<>();Set<Long> unresolved=new HashSet<>();
   Set<Long> parentArticleIds=new HashSet<>();for(JSONObject r:refs)if(r.optInt("RefEntityID")==1100)parentArticleIds.add(r.getLong("RefIntArtID"));
   Map<String,Long> found=new HashMap<>();List<Long> pp=new ArrayList<>(parentArticleIds);
   for(int off=0;off<pp.size();off+=900){List<Long> part=pp.subList(off,Math.min(off+900,pp.size()));for(JSONObject r:query("SELECT ID,ArticleID,CatalogID FROM "+E+"PRODUCTO WHERE ArticleID IN ("+marks(part.size())+") AND RevisionID=1",part.toArray()))found.put(str(r,"ARTICLEID")+"|"+str(r,"CATALOGID"),r.getLong("ID"));}
   for(JSONObject r:refs)if(r.optInt("RefEntityID")==1100){long owner=r.getLong("ArticleRevisionID");Long pid=found.get(str(r,"RefIntArtID")+"|"+str(r,"RefIntCatID"));if(pid==null){unresolved.add(owner);debt("PARENT_NOT_CAPTURED",owner,r);}else parents.computeIfAbsent(owner,k->new HashSet<>()).add(pid);}
   for(long id:ids){Set<Long> set=parents.getOrDefault(id,Set.of());if(unresolved.contains(id)||set.size()>1){debt("RELATION_PENDING",id,set);continue;}update("UPDATE "+E+"ARTICULO SET ProductoID=?,CapturedAt=SYSTIMESTAMP WHERE ID=?",set.isEmpty()?null:set.iterator().next(),id);}return;
  }
  String table=scope.equals("DETAIL")?"ArticleDetail":"ArticleDomain";List<JSONObject> data=rows(table,"ArticleRevisionID",ids);
  Set<String> lookupIds=new HashSet<>();for(JSONObject r:data)for(Object raw:r.keySet()){String col=raw.toString();if(scope.equals("DETAIL")?Set.of("Res_Int_01","Res_Int_04").contains(col):extra(r,entity)&&(entity==1100?PROD_DOM:ART_DOM).containsKey(col)&&!col.equals("Res_Text250_01"))lookupIds.add(str(r,col));}lookup(lookupIds);
  try(Values v=new Values(entity,ids,scope)){for(JSONObject r:data)nativeValues(v,entity,r.getLong("ArticleRevisionID"),r,table);v.finish();}
  Map<Long,JSONObject> old=existingRoots(target,ids);
  for(JSONObject r:data){long id=r.getLong("ArticleRevisionID");Map<String,Object> wanted=new LinkedHashMap<>();
   if(scope.equals("DETAIL")){wanted.put("CurrentStatus",num(r,"CurrentStatus"));wanted.put("PreviousStatus",num(r,"Res_Int_03"));wanted.put("Business",label(str(r,"Res_Int_01"),"10"));wanted.put("ExternalStatus",code(str(r,"Res_Int_04")));wanted.put("SKU",str(r,"Res_Int_02"));wanted.put("EAN",str(r,"EAN"));wanted.put("AlternativeIdentifier",str(r,"SupplierAltAID"));for(String[] pair:new String[][]{{"FirstDateApproved","Res_DateTime_02"},{"LastDateApproved","Res_DateTime_01"}})wanted.put(pair[0],str(r,pair[1])==null?null:Timestamp.valueOf(str(r,pair[1])));}
   else if(extra(r,entity)){wanted.put("SupplierPartNumber",str(r,"Res_Text250_01"));if(entity==1000){wanted.put("Color",label(str(r,"Res_Int_02"),"10"));wanted.put("SizeValue",label(str(r,"Res_Int_01"),"10"));}}
   if(old.containsKey(id))changeRoot(target,id,old.get(id),wanted);
  }
 }

 static void removeMissing(List<String> part,Set<Long> ids)throws Exception{
   for(int entity:new int[]{1000,1100}){
    String table=entity==1100?"PRODUCTO":"ARTICULO",owner=entity==1100?"ProductoID":"ArticuloID";
    List<JSONObject> old=query("SELECT ID FROM "+E+table+" WHERE Identifier IN ("+marks(part.size())+")",part.toArray());
    for(JSONObject o:old)if(!ids.contains(o.getLong("ID"))){long id=o.getLong("ID");
     update("DELETE FROM "+E+(entity==1100?"PRODUCTOATRIBUTOVALOR":"ARTICULOATRIBUTOVALOR")+" WHERE "+owner+"=?",id);
     update("DELETE FROM "+E+"CLASIFICACION WHERE "+owner+"=?",id);
     if(entity==1100){update("UPDATE "+E+"ARTICULO SET ProductoID=NULL WHERE ProductoID=?",id);update("UPDATE "+E+"PRODUCTO SET TargetRecord=NULL WHERE TargetRecord=?",id);}else update("UPDATE "+E+"ARTICULO SET TargetRecord=NULL WHERE TargetRecord=?",id);
     update("DELETE FROM "+E+table+" WHERE ID=?",id);
    }
   }
 }
 static List<Long> missingParents(List<Long> children)throws Exception {
  Set<Long> articleIds=new HashSet<>();Set<String> wanted=new HashSet<>(),names=new HashSet<>();
  for(JSONObject r:rows("ArticleReference","ArticleRevisionID",children))if(r.optInt("RefEntityID")==1100){articleIds.add(r.getLong("RefIntArtID"));wanted.add(str(r,"RefIntArtID")+"|"+str(r,"RefIntCatID"));if(str(r,"RefExtArtIdentifier")!=null)names.add(str(r,"RefExtArtIdentifier"));}
  if(articleIds.isEmpty())return List.of();List<Long> all=new ArrayList<>(articleIds);
  List<String> named=new ArrayList<>(names);for(int off=0;off<named.size();off+=900){List<String> part=named.subList(off,Math.min(off+900,named.size()));for(JSONObject r:query("SELECT ArticleID,CatalogID FROM "+E+"PRODUCTO WHERE Identifier IN ("+marks(part.size())+") AND RevisionID=1",part.toArray()))wanted.remove(str(r,"ARTICLEID")+"|"+str(r,"CATALOGID"));}
  if(wanted.isEmpty())return List.of();Set<Long> missing=new HashSet<>();for(String k:wanted)missing.add(Long.parseLong(k.split("\\|",2)[0]));all=new ArrayList<>(missing);List<Long> result=new ArrayList<>();
  for(int off=0;off<all.size();off+=900){List<Long> part=all.subList(off,Math.min(off+900,all.size()));for(JSONObject r:query("SELECT \"ID\",\"ArticleID\",\"CatalogID\" FROM PIM_MASTER.\"ArticleRevision\" WHERE \"ArticleID\" IN ("+marks(part.size())+") AND \"EntityID\"=1100 AND \"RevisionID\"=1 AND \"DeletionTimestamp\"="+LIVE,part.toArray()))if(wanted.contains(str(r,"ArticleID")+"|"+str(r,"CatalogID")))result.add(r.getLong("ID"));}
  return result;
 }
 static void process(String[] args)throws Exception {
  if(args.length<2||args.length>3)throw new IllegalArgumentException("OmegaDevNativeProjection <directory> <identifier-file>");
  dir=Path.of(args[0]);Files.createDirectories(dir);run="masa-live";
  loadEventLabels();
  if(chars.isEmpty() || System.currentTimeMillis()-dimensionAt>300000){lookups.clear();chars.clear();byName.clear();structures.clear();dimensions();dimensionAt=System.currentTimeMillis();}
  Path progress=dir.resolve("committed-lines.txt");
  int resume=Files.exists(progress)?Integer.parseInt(Files.readString(progress).trim()):0;
  int off=0;
  try(BufferedReader input=Files.newBufferedReader(Path.of(args[1]))){while(true){List<String> part=new ArrayList<>();String line;while(part.size()<450&&(line=input.readLine())!=null){if(!line.isBlank())part.add(line);}if(part.isEmpty())break;if(off+part.size()<=resume){off+=part.size();continue;}
   List<JSONObject> current=query("SELECT \"ID\" ID,\"Identifier\" IDENTIFIER,\"EntityID\" ENTITYID FROM PIM_MASTER.\"ArticleRevision\" WHERE \"Identifier\" IN ("+marks(part.size())+") AND \"RevisionID\"=1 AND \"DeletionTimestamp\"="+LIVE+" AND \"EntityID\" IN (1000,1100)",part.toArray());
   LinkedHashSet<Long> ids=new LinkedHashSet<>();for(JSONObject r:current){ids.add(r.getLong("ID"));ownerIdentifiers.put(r.getLong("ID"),r.getString("IDENTIFIER"));}
   String scope=args.length==3?args[2]:"FULL";
   if(!Set.of("FULL","CHAR","TEXT","ROOT","DETAIL","DOMAIN","CLASS","REL").contains(scope))throw new IllegalArgumentException("Invalid scope");
   if(!scope.equals("FULL")){
    Set<Long> present=new HashSet<>();for(String table:List.of("PRODUCTO","ARTICULO"))if(!ids.isEmpty())for(JSONObject j:query("SELECT ID FROM "+E+table+" WHERE ID IN ("+marks(ids.size())+")",ids.toArray()))present.add(j.getLong("ID"));
    List<Long> missing=new ArrayList<>(ids);missing.removeAll(present);
    if(!missing.isEmpty()){List<Long> baseline=new ArrayList<>(missing);baseline.addAll(missingParents(missing));selected=baseline;roots(1100);roots(1000);values(1100);values(1000);flushUpdates();write.commit();}
    // Only absent owners get a baseline; keep the requested scope for all existing owners.
   }
   // Include parents before children, even when only the child emitted a change.
   List<Long> initial=new ArrayList<>(ids);
   if(scope.equals("FULL")&&!initial.isEmpty())ids.addAll(missingParents(initial));
   List<Long> all=new ArrayList<>(ids);
   // Lock shared owners in the same order before reading/updating any projection values.
   // Parallel streams can include the same parent; serialize that owner, not the whole campaign.
   Collections.sort(all);
   for(String table:List.of("PRODUCTO","ARTICULO"))for(int i=0;i<all.size();i+=900){List<Long> locked=all.subList(i,Math.min(i+900,all.size()));query("SELECT ID FROM "+E+table+" WHERE ID IN ("+marks(locked.size())+") ORDER BY ID FOR UPDATE",locked.toArray());}
   if(!scope.equals("FULL")){
    for(int entity:new int[]{1100,1000}){List<Long> entityIds=new ArrayList<>();for(JSONObject r:current)if(r.getInt("ENTITYID")==entity)entityIds.add(r.getLong("ID"));partialValues(entity,entityIds,scope);}
    removeMissing(part,ids);flushUpdates();write.commit();off+=part.size();Path tmp=dir.resolve("committed-lines.tmp");Files.writeString(tmp,Integer.toString(off));Files.move(tmp,progress,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);status("APPLIED_"+scope,off,ids.size());continue;
   }
   for(int i=0;i<all.size();i+=900){selected=all.subList(i,Math.min(i+900,all.size()));roots(1100);roots(1000);}
   for(int entity:new int[]{1100,1000})for(int i=0;i<all.size();i+=900){selected=all.subList(i,Math.min(i+900,all.size()));values(entity);}
   removeMissing(part,ids);
   flushUpdates();write.commit();off+=part.size();
   Path tmp=dir.resolve("committed-lines.tmp");Files.writeString(tmp,Integer.toString(off));Files.move(tmp,progress,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
   status("APPLIED",off,ids.size());
  }}catch(Exception e){clearUpdates();write.rollback();throw e;}finally{clearUpdates();}
 }
 static JSONArray resolve(JSONObject change)throws Exception {
  JSONArray out=new JSONArray();String entity=change.optString("_entity");int e=entity.equals("Product2G")?1100:entity.equals("Article")?1000:0;
  if(e==0)throw new IllegalArgumentException("Only product/article identity resolution");
  Object raw=change.opt("_entityItem");if(raw==null)raw=change.opt("_entityItems");JSONArray items=raw instanceof JSONArray?(JSONArray)raw:new JSONArray().put(raw);
  String table=e==1100?"PRODUCTO":"ARTICULO";
  for(int i=0;i<items.length();i++){
   JSONObject item=items.optJSONObject(i);if(item==null)throw new IllegalArgumentException("Missing internal deletion identity");String key=item.optString("_internalId");
   if(!key.matches("[0-9]+@[0-9]+"))throw new IllegalArgumentException("Unknown internal identity "+key);
   String[] parts=key.split("@");long aid=Long.parseLong(parts[0]),catalog=Long.parseLong(parts[1]);
   for(JSONObject row:query("SELECT Identifier FROM "+E+table+" WHERE ArticleID=? AND CatalogID=? AND RevisionID=1",aid,catalog))out.put(row.getString("IDENTIFIER"));
   for(JSONObject row:query("SELECT \"Identifier\" IDENTIFIER FROM PIM_MASTER.\"ArticleRevision\" WHERE \"ArticleID\"=? AND \"CatalogID\"=? AND \"RevisionID\"=1 AND \"EntityID\"=? AND \"DeletionTimestamp\"="+LIVE,aid,catalog,e))out.put(row.getString("IDENTIFIER"));
  }return out;
 }
 public static void main(String[] args)throws Exception{
  read=connect();read.setReadOnly(true);write=connect();write.setAutoCommit(false);
  try{
   if(args.length==1&&args[0].equals("--server")){
    try(BufferedReader requests=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8))){String line;while((line=requests.readLine())!=null){JSONObject req=new JSONObject(line);JSONObject result=new JSONObject().put("request",req.getString("request"));
     try{if(req.has("catalog"))result.put("catalogRows",mx.com.liverpool.exploit.services.core.OmegaCatalogProjection.project(req.getJSONObject("catalog")));else if(req.has("resolve"))result.put("identifiers",resolve(req.getJSONObject("resolve")));else process(new String[]{req.getString("directory"),req.getString("input"),req.optString("scope","FULL")});result.put("ok",true);}catch(Exception e){clearUpdates();write.rollback();result.put("ok",false).put("error",e.toString());e.printStackTrace();}
     System.out.println("PROJECTION_RESULT "+result);System.out.flush();
    }}
   }else process(args);
  }finally{read.close();write.close();}
 }

}
