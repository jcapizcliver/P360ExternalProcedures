package mx.com.liverpool.p360.services.core;
import java.util.*;
/** Request-local dictionary reads. No cache survives a completed Foro request. */
public final class ForoLookupCache {
 private static final ThreadLocal<Map<List<String>,String>> VALUES = new ThreadLocal<>();
 private ForoLookupCache() { }
 public static void begin() { VALUES.set(new HashMap<>()); }
 public static void end() { VALUES.remove(); }
 public static String query(DBAccessDataStub db,String value,String dictionary) {
  Map<List<String>,String> values=VALUES.get();
  if(values==null)return db.queryDictionary(value,dictionary);
  List<String> key=Arrays.asList(dictionary,value);
  if(values.containsKey(key))return values.get(key);
  String result=db.queryDictionary(value,dictionary);
  if(result!=null)values.put(key,result);
  return result;
 }
}
