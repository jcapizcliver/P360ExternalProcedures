import java.sql.*;import java.nio.file.*;import java.util.*;import org.json.*;
/** Read-only fallback for native deletion events without external identifiers. */
public class ResolveEventIdentity {
 public static void main(String[] args)throws Exception{
  JSONArray input=new JSONArray(Files.readString(Path.of(args[0])));JSONObject out=new JSONObject();
  try(Connection c=new mx.com.liverpool.p360.services.core.QuickJdbcConnectionManager().openConnection(true)){
   c.setReadOnly(true);
   for(int entity:new int[]{1000,1100}){
    Map<String,JSONArray> wanted=new LinkedHashMap<>();Set<Long> aids=new LinkedHashSet<>();
    for(int i=0;i<input.length();i++){JSONObject j=input.getJSONObject(i);if(j.getInt("entity")!=entity)continue;String key=j.getString("key");wanted.put(key,new JSONArray());aids.add(j.getLong("aid"));}
    List<Long> ids=new ArrayList<>(aids);
    for(int off=0;off<ids.size();off+=900){List<Long> part=ids.subList(off,Math.min(off+900,ids.size()));String marks=String.join(",",Collections.nCopies(part.size(),"?"));
     String table=entity==1000?"ARTICULO":"PRODUCTO";
     String[] sqls={"SELECT ArticleID,CatalogID,RevisionID,Identifier FROM P360_EXPLOIT."+table+" WHERE ArticleID IN ("+marks+")",
      "SELECT \"ArticleID\",\"CatalogID\",\"RevisionID\",\"Identifier\" FROM PIM_MASTER.\"ArticleRevision\" WHERE \"ArticleID\" IN ("+marks+") AND \"EntityID\"="+entity+" AND \"DeletionTimestamp\"=TIMESTAMP '9999-12-31 00:00:00'"};
     for(String sql:sqls)try(PreparedStatement q=c.prepareStatement(sql)){q.setQueryTimeout(900);int k=1;for(long id:part)q.setLong(k++,id);try(ResultSet r=q.executeQuery()){while(r.next()){String key=entity+":"+r.getLong(1)+":"+r.getLong(2)+":"+r.getLong(3);JSONArray values=wanted.get(key);if(values!=null&&r.getString(4)!=null)values.put(r.getString(4));}}}

    }
    for(var e:wanted.entrySet())out.put(e.getKey(),e.getValue());
   }
  }
  Files.writeString(Path.of(args[1]),out.toString());
 }
}
