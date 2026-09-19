package mx.com.liverpool.p360.services.core.temp.product2g.maintenance9;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import javax.net.ssl.*;
import org.json.*;
import mx.com.liverpool.p360.services.core.dq.TitleText;

/** Indexed, resumable reader; all P360 changes use its REST list API. */
public final class ApprovedTitleWithoutBrand {
 static final String LIVE="timestamp '9999-12-31 00:00:00'";
 static final String TITLE_FIELD="Product2GCharacteristicValueLang.Value('TituloSinMarca',root,\"0000.0000.RK\",'TituloSinMarca',-1)";
 static String auth,base;static SSLSocketFactory ssl;static PrintWriter report;
 static long examined,changed,skipped;static boolean apply,s4h;
 public static void main(String[] args)throws Exception {
  if(args.length<2)throw new IllegalArgumentException("Usage: <output-directory> <https://172.18.251.7:1512> [--apply] [--s4h-fallback] [--max-pages=N]");
  Path out=Paths.get(args[0]);Files.createDirectories(out);base=args[1];
  if(!base.equals("https://172.18.251.7:1512"))throw new IllegalArgumentException("Unexpected P360 destination");
  List<String> flags=Arrays.asList(args);apply=flags.contains("--apply");s4h=flags.contains("--s4h-fallback");
  int pages=0,maxPages=Integer.MAX_VALUE;for(String a:args)if(a.startsWith("--max-pages="))maxPages=Integer.parseInt(a.substring(12));
  Properties props=new Properties();try(InputStream in=Files.newInputStream(Paths.get("/u01/workshop/p360_contingencyservices.properties"))){props.load(in);}
  for(String k:props.stringPropertyNames())if(k.endsWith("basic_token_auth"))auth=props.getProperty(k).trim();
  if(auth==null)throw new IllegalStateException("Missing P360 API credentials");if(!auth.startsWith("Basic "))auth="Basic "+auth;
  SSLContext c=SSLContext.getInstance("TLS");c.init(null,new javax.net.ssl.TrustManager[]{new X509TrustManager(){public java.security.cert.X509Certificate[] getAcceptedIssuers(){return new java.security.cert.X509Certificate[0];}public void checkClientTrusted(java.security.cert.X509Certificate[] x,String a){}public void checkServerTrusted(java.security.cert.X509Certificate[] x,String a){}}},null);ssl=c.getSocketFactory();
  Path checkpoint=out.resolve(apply?"apply.checkpoint":"preview.checkpoint");long last=Files.exists(checkpoint)?Long.parseLong(Files.readString(checkpoint).trim()):0;
  try(java.nio.channels.FileChannel fc=java.nio.channels.FileChannel.open(out.resolve("campaign.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);java.nio.channels.FileLock lock=fc.tryLock()){
   if(lock==null)throw new IllegalStateException("Campaign already active in this directory");
   report=new PrintWriter(Files.newBufferedWriter(out.resolve(apply?"applied.csv":"preview.csv"),StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND));
   report.println("Time,Identifier,Status,Title,Brand,Result,Reason");report.flush();
   Properties db=new Properties();db.setProperty("user",env("ORACLE_JDBC_USER"));db.setProperty("password",env("ORACLE_JDBC_PASSWORD"));db.setProperty("oracle.net.CONNECT_TIMEOUT","15000");db.setProperty("oracle.jdbc.ReadTimeout","30000");
   try(Connection conn=DriverManager.getConnection(env("ORACLE_JDBC_URL"),db)){
    conn.setReadOnly(true);
    while(pages++<maxPages){
     Path pause=out.resolve("pause.requested"), paused=out.resolve("paused");
     if(Files.exists(pause)) {
      Files.writeString(paused,java.time.OffsetDateTime.now().toString());
      System.out.println("PAUSED at completed batch boundary");
      while(Files.exists(pause))Thread.sleep(1000);
      Files.deleteIfExists(paused);System.out.println("RESUMED");
     }

     System.out.println(java.time.OffsetDateTime.now()+" READ_PAGE after="+last);
     List<Long> ids=new ArrayList<>();
     String q="select /*+ leading(d ar) use_nl(ar) index(d IDX$$_73E00001) index(ar PK_ArticleRevision) */ ar.\"ID\" from PIM_MASTER.\"ArticleDetail\" d join PIM_MASTER.\"ArticleRevision\" ar on ar.\"ID\"=d.\"ArticleRevisionID\" where d.\"CurrentStatus\"=1007 and d.\"DeletionTimestamp\"="+LIVE+" and d.\"ArticleRevisionID\">? and ar.\"EntityID\"=1100 and ar.\"RevisionID\"=1 and ar.\"DeletionTimestamp\"="+LIVE+" order by d.\"ArticleRevisionID\" fetch first 100 rows only";
     try(PreparedStatement p=conn.prepareStatement(q)){p.setLong(1,last);p.setQueryTimeout(25);p.setFetchSize(100);try(ResultSet rs=p.executeQuery()){while(rs.next())ids.add(rs.getLong(1));}}
     if(ids.isEmpty())break;
     System.out.println(java.time.OffsetDateTime.now()+" READ_NATIVE count="+ids.size());
     process(conn,ids,out);
     last=ids.get(ids.size()-1);Files.writeString(checkpoint,Long.toString(last),StandardCharsets.UTF_8);
     System.out.println(java.time.OffsetDateTime.now()+" page="+pages+" examined="+examined+" changed="+changed+" skipped="+skipped+" last="+last);
     Thread.sleep(200);
    }
   }finally{report.close();}
   System.out.println("DONE examined="+examined+" changed="+changed+" skipped="+skipped);
  }
 }
 static void process(Connection c,List<Long> ids,Path out)throws Exception {
  String in=ids.toString().replace('[','(').replace(']',')');
  String q="select /*+ leading(ar d l dom) use_nl(d l dom br bl sr sl) index(ar PK_ArticleRevision) index(d XAK1_ArticleDetail) index(l XAK1_ArticleLang) index(dom XAK1_ArticleDomain) index(br XAK1_LookupValueRevision) index(bl XAK1_LookupValueLang) index(sr XAK1_LookupValueRevision) index(sl XAK1_LookupValueLang) */ ar.\"Identifier\",l.\"Res_Text250_01\",bl.\"Name\",sl.\"Name\" from PIM_MASTER.\"ArticleRevision\" ar join PIM_MASTER.\"ArticleDetail\" d on d.\"ArticleRevisionID\"=ar.\"ID\" and d.\"CurrentStatus\"=1007 and d.\"DeletionTimestamp\"="+LIVE
   +" left join PIM_MASTER.\"ArticleLang\" l on l.\"ArticleRevisionID\"=ar.\"ID\" and l.\"LanguageID\"=10 and l.\"EntityID\"=1105 and l.\"ChannelID\"=1 and l.\"DeletionTimestamp\"="+LIVE
   +" left join PIM_MASTER.\"ArticleDomain\" dom on dom.\"ArticleRevisionID\"=ar.\"ID\" and dom.\"TargetMarket\"='MX' and dom.\"EntityID\"=21006 and dom.\"ChannelID\"=1 and dom.\"DeletionTimestamp\"="+LIVE
   +lookup("br","bl","dom.\"Res_Int_05\"")+lookup("sr","sl","dom.\"Res_Int_06\"")
   +" where ar.\"ID\" in "+in+" and ar.\"EntityID\"=1100 and ar.\"RevisionID\"=1 and ar.\"DeletionTimestamp\"="+LIVE;
  Map<String,String[]> candidates=new LinkedHashMap<>();
  try(PreparedStatement p=c.prepareStatement(q)){p.setQueryTimeout(25);p.setFetchSize(100);try(ResultSet r=p.executeQuery()){while(r.next()){
   String id=r.getString(1),title=n(r.getString(2)),brand=n(r.getString(3));if(brand.isBlank()&&s4h)brand=n(r.getString(4));
   if(candidates.containsKey(id))throw new IllegalStateException("Ambiguous native rows for "+id);
   candidates.put(id,new String[]{title,brand,TitleText.withoutBrand(title,brand)});
  }}}
  if(candidates.isEmpty())return;
  Map<String,String> values=readTitles(c,ids);
  System.out.println(java.time.OffsetDateTime.now()+" NATIVE_READY count="+candidates.size());
  JSONArray rows=new JSONArray();List<String> written=new ArrayList<>();
  for(Map.Entry<String,String[]> e:candidates.entrySet()){
   examined++;String id=e.getKey();String[] d=e.getValue();String v=values.get(id);String reason=null;
   if(d[0].isBlank())reason="EMPTY_TITLE";else if(d[1].isBlank())reason="NO_BRAND_LABEL";else if(d[2].isBlank())reason="EMPTY_RESULT";
   else if(d[2].equals(v))reason="ALREADY_EQUAL";
   if(reason!=null){skipped++;row(id,"SKIPPED",d,reason);continue;}
   if(!apply){row(id,"PREVIEW",d,"WOULD_UPDATE");continue;}
   rows.put(new JSONObject().put("object",new JSONObject().put("id","'"+id+"'@1")).put("values",new JSONArray().put(d[2])));written.add(id);
  }
  if(rows.length()>0){
   JSONObject req=new JSONObject().put("columns",new JSONArray().put(new JSONObject().put("identifier",TITLE_FIELD))).put("rows",rows);
   Path evidence=out.resolve("batch-"+ids.get(0));Files.writeString(Paths.get(evidence+".request.json"),req.toString());
   System.out.println(java.time.OffsetDateTime.now()+" API_WRITE count="+rows.length());
   JSONObject res=api("POST","/rest/V1.0/list/Product2G",req);Files.writeString(Paths.get(evidence+".response.json"),res.toString());
   // Verify the actual persisted value; an HTTP success alone is insufficient.
   Map<String,String> actual=readTitles(c,ids);
   boolean failed=false;for(String id:written){String[] d=candidates.get(id);boolean ok=d[2].equals(actual.get(id));row(id,ok?"OK":"VERIFY_FAILED",d,ok?"P360_READBACK":"STOP");if(ok)changed++;else failed=true;}
   if(failed)throw new IllegalStateException("Readback mismatch; stopped without advancing checkpoint");
  }
 }
 static Map<String,String> readTitles(Connection c,List<Long> ids)throws Exception {
  String in=ids.toString().replace('[','(').replace(']',')');Map<String,String> result=new HashMap<>();
  String q="select /*+ leading(ar v) use_nl(v) index(ar PK_ArticleRevision) index(v XAK1_ArticleCharactValue) */ ar.\"Identifier\",v.\"Value\" from PIM_MASTER.\"ArticleRevision\" ar left join PIM_MASTER.\"ArticleCharactValue\" v on v.\"ArticleRevisionID\"=ar.\"ID\" and v.\"DeletionTimestamp\"="+LIVE+" and v.\"CharacteristicID\"=7666 and v.\"RecordKey\"='0000.0000.RK' and v.\"ParentRecordKey\"='root' where ar.\"ID\" in "+in;
  try(PreparedStatement p=c.prepareStatement(q)){p.setQueryTimeout(25);p.setFetchSize(100);try(ResultSet r=p.executeQuery()){while(r.next()){String id=r.getString(1);if(result.containsKey(id))throw new IllegalStateException("Ambiguous title rows "+id);result.put(id,n(r.getString(2)));}}}return result;
 }
 static String lookup(String r,String l,String field){return " left join PIM_MAIN.\"LookupValueRevision\" "+r+" on "+r+".\"LookupValueID\"="+field+" and "+r+".\"RevisionID\"=1 and "+r+".\"DeletionTimestamp\"="+LIVE+" left join PIM_MAIN.\"LookupValueLang\" "+l+" on "+l+".\"LookupValueRevisionID\"="+r+".\"ID\" and "+l+".\"LanguageID\"=10 and "+l+".\"DeletionTimestamp\"="+LIVE;}
 static String n(String s){return s==null?"":s;}
 static String text(Object o){if(o==null||o==JSONObject.NULL)return "";if(o instanceof JSONArray)return ((JSONArray)o).length()==0?"":text(((JSONArray)o).opt(0));return String.valueOf(o);}
 static String env(String k){String v=System.getenv(k);if(v==null||v.isBlank())throw new IllegalStateException("Missing "+k);return v;}
 static String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
 static JSONObject api(String method,String path,JSONObject body)throws Exception{
  HttpsURLConnection c=(HttpsURLConnection)new URL(base+path).openConnection();c.setSSLSocketFactory(ssl);c.setHostnameVerifier((h,s)->h.equals("172.18.251.7"));c.setConnectTimeout(15000);c.setReadTimeout(45000);c.setRequestMethod(method);c.setRequestProperty("Authorization",auth);c.setRequestProperty("Accept","application/json");
  try{if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
   int status=c.getResponseCode();InputStream in=status<400?c.getInputStream():c.getErrorStream();String data=in==null?"":new String(in.readAllBytes(),StandardCharsets.UTF_8);if(in!=null)in.close();if(status>=300)throw new IOException("P360 HTTP "+status+" "+data.substring(0,Math.min(300,data.length())));return new JSONObject(data);
  }finally{c.disconnect();}
 }
 static void row(String id,String status,String[] d,String reason){String[] a={java.time.OffsetDateTime.now().toString(),id,status,d[0],d[1],d[2],reason};StringJoiner s=new StringJoiner(",");for(String x:a)s.add("\""+n(x).replace("\"","\"\"")+"\"");report.println(s);report.flush();}
}
