import mx.com.liverpool.exploit.services.core.OmegaAudit;
import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.json.*;

/** Durable event changes -> Omega only. Never writes native P360 tables. */
public final class OmegaEventDelta {
 static long applied,stale,unchanged,fallbackOwners;
 static final Map<String,Set<String>> fallback=new LinkedHashMap<>();
 static List<Object> array(JSONArray a){List<Object> list=new ArrayList<>();for(int i=0;i<a.length();i++)list.add(a.get(i));return list;}
 static void repair(String scope,String identifier){fallback.computeIfAbsent(scope,k->new TreeSet<>()).add(identifier);}
 static String s(JSONObject j,String key){return j.isNull(key)?null:j.optString(key,null);}
 static String key(JSONObject p){return p.optString("name")+"|"+Objects.toString(s(p,"language"),"")+"|"+Objects.toString(s(p,"record"),"")+"|"+Objects.toString(s(p,"parent"),"");}
 static String rowKey(JSONObject r){return r.optString("ATRIBUTOIDENTIFIER")+"|"+Objects.toString(s(r,"LANGUAGEKEY"),"")+"|"+Objects.toString(s(r,"RECORDKEY"),"")+"|"+Objects.toString(s(r,"PARENTRECORDKEY"),"");}
 static Timestamp eventTime(String at){return Timestamp.valueOf(OffsetDateTime.parse(at).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());}
 static void apply(int entity,List<JSONObject> owners)throws Exception{
  String table=entity==1100?"PRODUCTO":"ARTICULO",owner=entity==1100?"ProductoID":"ArticuloID",valTable=table+"ATRIBUTOVALOR";
  List<String> identifiers=new ArrayList<>();Set<String> names=new TreeSet<>();
  Set<String> deprecated=new HashSet<>(ExploitLiveProjection.LANG.values());deprecated.addAll(ExploitLiveProjection.DETAIL.values());deprecated.addAll((entity==1100?ExploitLiveProjection.PROD_DOM:ExploitLiveProjection.ART_DOM).values());
  for(JSONObject o:owners){identifiers.add(o.getString("identifier"));for(Object x:array(o.getJSONArray("patches"))){JSONObject p=(JSONObject)x;if(!deprecated.contains(p.getString("name")))names.add(p.getString("name"));}}
  Map<String,JSONObject> roots=new HashMap<>();List<Long> ids=new ArrayList<>();
  for(JSONObject r:ExploitLiveProjection.query("SELECT ID,Identifier FROM P360_EXPLOIT."+table+" WHERE RevisionID=1 AND Identifier IN ("+ExploitLiveProjection.marks(identifiers.size())+") ORDER BY ID FOR UPDATE",identifiers.toArray())){
   String id=r.getString("IDENTIFIER");if(roots.containsKey(id))throw new IOException("Duplicate Omega identity: "+id);roots.put(id,r);ids.add(r.getLong("ID"));
  }
  Map<Long,Map<String,List<JSONObject>>> rows=new HashMap<>();
  if(!ids.isEmpty()&&!names.isEmpty()){
   List<String> allNames=new ArrayList<>(names);
   for(int off=0;off<allNames.size();off+=900){List<String> part=allNames.subList(off,Math.min(off+900,allNames.size()));List<Object> args=new ArrayList<>(ids);args.addAll(part);
    for(JSONObject r:ExploitLiveProjection.query("SELECT /*+ INDEX(v "+(entity==1100?"IX_EXP_PRO_VAL_OWNER":"IX_EXP_ART_VAL_OWNER")+") */ ID,"+owner+" OWNER_ID,AtributoIdentifier,LanguageKey,RecordKey,ParentRecordKey,Code,Value,LongValue,SourceModifiedAt FROM P360_EXPLOIT."+valTable+" v WHERE "+owner+" IN ("+ExploitLiveProjection.marks(ids.size())+") AND AtributoIdentifier IN ("+ExploitLiveProjection.marks(part.size())+") AND SourceTable IN ('ArticleCharactValue','ArticleCharactValueLang')",args.toArray()))rows.computeIfAbsent(r.getLong("OWNER_ID"),k->new HashMap<>()).computeIfAbsent(rowKey(r),k->new ArrayList<>()).add(r);
   }
  }
  String sql="UPDATE P360_EXPLOIT."+valTable+" SET Code=?,Value=?,LongValue=?,SourceModifiedAt=?,ModificationUserID=?,ModificationTimestamp=? WHERE ID=?";
  try(PreparedStatement p=ExploitLiveProjection.write.prepareStatement(sql)){
   p.setQueryTimeout(900);int pending=0;
   for(JSONObject o:owners){String identifier=o.getString("identifier");JSONObject root=roots.get(identifier);
    if(root==null){repair("FULL",identifier);continue;}
    for(Object raw:array(o.getJSONArray("patches"))){JSONObject patch=(JSONObject)raw;String name=patch.getString("name");if(deprecated.contains(name))continue;if(!patch.has("userId")){repair("CHAR",identifier);continue;}
     List<JSONObject> matches=rows.getOrDefault(root.getLong("ID"),Map.of()).getOrDefault(key(patch),List.of());
     if(patch.optBoolean("delete",false)&&matches.isEmpty()){unchanged++;continue;}
     if(matches.size()!=1){repair("CHAR",identifier);continue;}
     JSONObject old=matches.get(0);Timestamp at=eventTime(patch.getString("at"));String mt=s(old,"SOURCEMODIFIEDAT");
     if(mt==null){repair("CHAR",identifier);continue;}
     int order=Timestamp.valueOf(mt).compareTo(at);
     if(order>0){stale++;continue;}
     if(patch.optBoolean("delete",false)){
      if(order==0){repair("CHAR",identifier);continue;}
      ExploitLiveProjection.update("DELETE FROM P360_EXPLOIT."+valTable+" WHERE ID=?",old.getString("ID"));rows.get(root.getLong("ID")).remove(key(patch));applied++;continue;
     }
     String code=s(patch,"code"),value=s(patch,"value"),before=s(old,"VALUE");if(before==null)before=s(old,"LONGVALUE");
     boolean same=Objects.equals(code,s(old,"CODE"))&&Objects.equals(value,before);
     if(same){unchanged++;if(order==0)continue;}
     if(order==0){repair("CHAR",identifier);continue;} // Same timestamp, different payload: use authoritative section.
     p.setString(1,code);boolean longValue=value!=null&&value.getBytes(StandardCharsets.UTF_8).length>4000;
     p.setString(2,longValue?null:value);if(longValue)p.setNCharacterStream(3,new StringReader(value));else p.setNull(3,Types.NCLOB);
     p.setTimestamp(4,at);p.setString(5,s(patch,"userId"));p.setTimestamp(6,at);p.setString(7,old.getString("ID"));p.addBatch();if(!same)applied++;
     old.put("CODE",code==null?JSONObject.NULL:code);old.put("VALUE",longValue||value==null?JSONObject.NULL:value);old.put("LONGVALUE",longValue?value:JSONObject.NULL);old.put("SOURCEMODIFIEDAT",at.toString());
     if(++pending>=900){p.executeBatch();pending=0;}
    }
   }if(pending>0)p.executeBatch();
  }
  ExploitLiveProjection.flushUpdates();OmegaAudit.sourceRoots(ExploitLiveProjection.read,ExploitLiveProjection.write,"P360_EXPLOIT.",entity,ids,false);ExploitLiveProjection.write.commit();
 }
 static void process(String directory,String input)throws Exception{
  long start=System.nanoTime();applied=stale=unchanged=fallbackOwners=0;fallback.clear();Path dir=Path.of(directory);Files.createDirectories(dir);
  Path labels=dir.getParent().resolve("label-overrides.json");if(Files.exists(labels))Files.copy(labels,dir.resolve("label-overrides.json"),StandardCopyOption.REPLACE_EXISTING);
  ExploitLiveProjection.dir=dir;ExploitLiveProjection.run="masa-live";
  List<JSONObject> products=new ArrayList<>(),articles=new ArrayList<>();
  try(BufferedReader r=Files.newBufferedReader(Path.of(input))){String line;while((line=r.readLine())!=null){JSONObject o=new JSONObject(line);for(Object f:array(o.getJSONArray("fallback")))repair(f.toString(),o.getString("identifier"));(o.getInt("entity")==1100?products:articles).add(o);}}
  for(List<JSONObject> list:List.of(products,articles))for(int off=0;off<list.size();off+=450)apply(list==products?1100:1000,list.subList(off,Math.min(off+450,list.size())));
  // A FULL refresh is only for unknown/new/missing identity, never promoted across a batch.
  Set<String> full=fallback.getOrDefault("FULL",Set.of());
  for(var entry:fallback.entrySet()){
   Set<String> names=new TreeSet<>(entry.getValue());if(!entry.getKey().equals("FULL"))names.removeAll(full);if(names.isEmpty())continue;fallbackOwners+=names.size();
   Path request=dir.resolve(entry.getKey()+".txt");Files.write(request,names,StandardCharsets.UTF_8);
   String signature=ExploitLiveProjection.hash(String.join("\n",names)).substring(0,16);
   ExploitLiveProjection.process(new String[]{dir.resolve(entry.getKey()+"-"+signature).toString(),request.toString(),entry.getKey()});
  }
  JSONObject stats=new JSONObject().put("directUpdated",applied).put("staleProtected",stale).put("alreadyCurrent",unchanged).put("fallbackOwnerScopes",fallbackOwners).put("millis",(System.nanoTime()-start)/1000000);
  Files.writeString(dir.resolve("delta-stats.json"),stats.toString());System.out.println("DELTA_STATS "+stats);
 }
 public static void main(String[] args)throws Exception{
  ExploitLiveProjection.read=ExploitLiveProjection.connect();ExploitLiveProjection.read.setReadOnly(true);ExploitLiveProjection.write=ExploitLiveProjection.connect();ExploitLiveProjection.write.setAutoCommit(false);
  for(Connection c:List.of(ExploitLiveProjection.read,ExploitLiveProjection.write))try(CallableStatement tag=c.prepareCall("BEGIN DBMS_APPLICATION_INFO.SET_MODULE(?,?); END;")){tag.setString(1,"OMEGA_EVENT_DELTA");tag.setString(2,ProcessHandle.current().pid()+":"+(c==ExploitLiveProjection.read?"read":"write"));tag.execute();}
  try(BufferedReader r=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8))){String line;while((line=r.readLine())!=null){JSONObject req=new JSONObject(line),result=new JSONObject().put("request",req.getString("request"));
   try{process(req.getString("directory"),req.getString("input"));result.put("ok",true);}catch(Exception e){ExploitLiveProjection.clearUpdates();ExploitLiveProjection.write.rollback();result.put("ok",false).put("error",e.toString());e.printStackTrace();}
   System.out.println("PROJECTION_RESULT "+result);System.out.flush();
  }}finally{ExploitLiveProjection.read.close();ExploitLiveProjection.write.close();}
 }
}
