import java.sql.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import org.json.*;

/** Updates auxiliary reference rows only. Never updates product/article owners. */
public final class CatalogDelta {
 static Connection c; static Map<String,JSONObject> specs=new HashMap<>();
 static String q(String s){if(!s.matches("[A-Za-z0-9_]+"))throw new IllegalArgumentException(s);return "\""+s+"\"";}
 static int exec(String sql,Object...args)throws Exception{
  try(PreparedStatement p=c.prepareStatement(sql)){p.setQueryTimeout(900);for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);return p.executeUpdate();}
 }
 static int merge(String suffix,String predicate,Object...args)throws Exception{
  JSONObject s=specs.get(suffix);String target=s.getString("target");
  if(!target.equals("P360_EXPLOIT.CAT_"+suffix))throw new IllegalArgumentException(target);
  JSONArray a=s.getJSONArray("columns");List<String> cols=new ArrayList<>(),vals=new ArrayList<>(),sets=new ArrayList<>();
  for(int i=0;i<a.length();i++){String k=q(a.getString(i));cols.add(k);vals.add("v."+k);if(!k.equals("\"ID\""))sets.add("t."+k+"=v."+k);}
  return exec("MERGE INTO "+target+" t USING ("+s.getString("select")+" AND "+predicate+") v ON (t.\"ID\"=v.\"ID\") WHEN MATCHED THEN UPDATE SET "+String.join(",",sets)+" WHEN NOT MATCHED THEN INSERT ("+String.join(",",cols)+") VALUES ("+String.join(",",vals)+")",args);
 }
 static long scalar(String sql,long id,long rev)throws Exception{try(PreparedStatement p=c.prepareStatement(sql)){p.setLong(1,id);p.setLong(2,rev);try(ResultSet r=p.executeQuery()){return r.next()?r.getLong(1):-1;}}}
 static int lookup(long id,long rev)throws Exception{
  int n=merge("LKP","s.\"LookupID\"=? AND s.\"RevisionID\"=?",id,rev);
  n+=merge("LKP_LANG","s.\"LookupRevisionID\" IN (SELECT \"ID\" FROM PIM_MAIN.\"LookupRevision\" WHERE \"LookupID\"=? AND \"RevisionID\"=?)",id,rev);
  return n;
 }
 static int value(long id,long rev)throws Exception{
  long parent=scalar("SELECT \"LookupID\" FROM PIM_MAIN.\"LookupValueRevision\" WHERE \"LookupValueID\"=? AND \"RevisionID\"=? AND \"DeletionTimestamp\"=TIMESTAMP '9999-12-31 00:00:00'",id,rev);
  int n=0;if(parent!=-1)n+=lookup(parent,rev);
  n+=merge("LKP_VAL","s.\"LookupValueID\"=? AND s.\"RevisionID\"=?",id,rev);
  n+=merge("LKP_VAL_LANG","s.\"LookupValueRevisionID\" IN (SELECT \"ID\" FROM PIM_MAIN.\"LookupValueRevision\" WHERE \"LookupValueID\"=? AND \"RevisionID\"=?)",id,rev);
  n+=exec("DELETE FROM P360_EXPLOIT.CAT_LKP_VAL_LANG t WHERE t.\"LookupValueRevisionID\" IN (SELECT \"ID\" FROM P360_EXPLOIT.CAT_LKP_VAL WHERE \"LookupValueID\"=? AND \"RevisionID\"=?) AND NOT EXISTS (SELECT 1 FROM PIM_MAIN.\"LookupValueLang\" s JOIN PIM_MAIN.\"LookupValueRevision\" v ON v.\"ID\"=s.\"LookupValueRevisionID\" WHERE s.\"ID\"=t.\"ID\" AND s.\"DeletionTimestamp\"=TIMESTAMP '9999-12-31 00:00:00' AND v.\"DeletionTimestamp\"=TIMESTAMP '9999-12-31 00:00:00')",id,rev);
  n+=exec("DELETE FROM P360_EXPLOIT.CAT_LKP_VAL t WHERE t.\"LookupValueID\"=? AND t.\"RevisionID\"=? AND NOT EXISTS (SELECT 1 FROM PIM_MAIN.\"LookupValueRevision\" s WHERE s.\"ID\"=t.\"ID\" AND s.\"DeletionTimestamp\"=TIMESTAMP '9999-12-31 00:00:00')",id,rev);return n;
 }
 static final Map<String,String[]> OTHER=Map.of(
  "Characteristic",new String[]{"CHAR","CharacteristicID"},"StandardizationDictionary",new String[]{"DICT","ID"},"StandardizationValue",new String[]{"DICT_ENTRY","ID"},"Structure",new String[]{"STRUCT","StructureID"},"StructureGroup",new String[]{"SG","StructureGroupID"},"StructureAttribute",new String[]{"SA","StructureAttributeID"});
 static final Map<String,String[][]> CHILDREN=Map.ofEntries(
  Map.entry("CHAR",new String[][]{{"CHAR_LANG","CharacteristicRevisionID"}}),Map.entry("DICT",new String[][]{{"DICT_LANG","DictionaryID"}}),Map.entry("DICT_ENTRY",new String[][]{{"DICT_ENTRY_LANG","DictionaryEntryID"}}),
  Map.entry("STRUCT",new String[][]{{"STRUCT_LANG","StructureRevisionID"}}),Map.entry("SG",new String[][]{{"SG_LANG","StructureGroupRevisionID"},{"SG_DETAIL","StructureGroupRevisionID"},{"SG_MAP","StructureGroupRevisionID"},{"SG_ATTR","StructureGroupRevisionID"}}),
  Map.entry("SA",new String[][]{{"SA_LANG","StructureAttributeRevisionID"}}),Map.entry("SG_ATTR",new String[][]{{"SG_ATTR_LANG","StructureGroupAttributeID"},{"SG_ATTR_VAL","StructureGroupAttributeID"}}));
 static List<Long> ids(String sql,Object...args)throws Exception{List<Long> out=new ArrayList<>();try(PreparedStatement p=c.prepareStatement(sql)){p.setQueryTimeout(900);for(int i=0;i<args.length;i++)p.setObject(i+1,args[i]);try(ResultSet r=p.executeQuery()){while(r.next())out.add(r.getLong(1));}}return out;}
 static int graph(String suffix,String where,Object...args)throws Exception{
  JSONObject spec=specs.get(suffix);String source="PIM_MAIN."+q(spec.getString("source")),target=spec.getString("target");
  Set<Long> live=new HashSet<>(ids("SELECT s.\"ID\" FROM "+source+" s WHERE "+where+" AND s.\"DeletionTimestamp\"=TIMESTAMP '9999-12-31 00:00:00'",args));
  List<Long> old=ids("SELECT s.\"ID\" FROM "+target+" s WHERE "+where,args);int n=merge(suffix,where,args);
  Set<Long> all=new LinkedHashSet<>(old);all.addAll(live);
  for(long row:all){for(String[] child:CHILDREN.getOrDefault(suffix,new String[0][]))n+=graph(child[0],"s."+q(child[1])+"=?",row);if(!live.contains(row))n+=exec("DELETE FROM "+target+" WHERE \"ID\"=?",row);}
  return n;
 }
 static int other(String entity,long id,long revision)throws Exception{
  String[] e=OTHER.get(entity);if(e==null)throw new IllegalArgumentException("Unsupported entity "+entity);JSONObject spec=specs.get(e[0]);String source=spec.getString("source");boolean versioned=source.endsWith("Revision");String where="s."+q(e[1])+"=?"+(versioned?" AND s.\"RevisionID\"=?":"");Object[] args=versioned?new Object[]{id,revision}:new Object[]{id};
  String parent=entity.equals("Characteristic")?"LookupID":entity.equals("StructureGroup")||entity.equals("StructureAttribute")?"StructureID":entity.equals("StandardizationValue")?"DictionaryID":null;
  if(parent!=null)for(long pid:ids("SELECT DISTINCT s."+q(parent)+" FROM PIM_MAIN."+q(source)+" s WHERE "+where+" AND s."+q(parent)+" IS NOT NULL AND s.\"DeletionTimestamp\"=TIMESTAMP '9999-12-31 00:00:00'",args)){
   if(parent.equals("LookupID"))lookup(pid,revision);else other(parent.equals("StructureID")?"Structure":"StandardizationDictionary",pid,revision);
  }return graph(e[0],where,args);
 }
 public static void main(String[] args)throws Exception{
  JSONArray tables=new JSONObject(Files.readString(Path.of(args[0]))).getJSONArray("tables");for(int i=0;i<tables.length();i++){JSONObject t=tables.getJSONObject(i);specs.put(t.getString("suffix"),t);}
  try(Connection conn=new mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager().openConnection(true);BufferedReader in=new BufferedReader(new InputStreamReader(System.in))){c=conn;c.setAutoCommit(false);
   for(JSONObject spec:specs.values()){
    Set<String> available=new HashSet<>();try(PreparedStatement p=c.prepareStatement("SELECT COLUMN_NAME FROM ALL_TAB_COLUMNS WHERE OWNER='PIM_MAIN' AND TABLE_NAME=?")){p.setString(1,spec.getString("source"));try(ResultSet r=p.executeQuery()){while(r.next())available.add(r.getString(1));}}
    JSONArray columns=spec.getJSONArray("columns");Set<String> included=new HashSet<>();for(int i=0;i<columns.length();i++)included.add(columns.getString(i));List<String> add=new ArrayList<>();
    for(String name:new String[]{"CreationUserID","ModificationUserID"})if(!included.contains(name)){columns.put(name);add.add((available.contains(name)?"s."+q(name):"CAST(NULL AS NUMBER)")+" AS "+q(name));}
    if(!add.isEmpty())spec.put("select",spec.getString("select").replaceFirst("SELECT ","SELECT "+String.join(",",add)+","));
   }
   exec("BEGIN DBMS_APPLICATION_INFO.SET_MODULE('MasaCatalogDelta','references only'); END;");
   String line;while((line=in.readLine())!=null){JSONObject req=new JSONObject(line),out=new JSONObject().put("seq",req.getLong("seq"));try{
    long id=req.getLong("id"),rev=req.getLong("revision");String entity=req.getString("entity");int rows;
    if(entity.equals("LookupValue"))rows=value(id,rev);else if(entity.equals("Lookup"))rows=lookup(id,rev);else rows=other(entity,id,rev);
    c.commit();out.put("ok",true).put("rows",rows);
   }catch(Exception e){c.rollback();out.put("ok",false).put("error",e.toString());}
   System.out.println("CATALOG_RESULT "+out);System.out.flush();}
  }
 }
}
