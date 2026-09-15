package mx.com.liverpool.p360.services.core.sftp;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.json.*;

/** Read-only audit. Never calls processFile, flushPendingWrites or a write API. */
public class JanaProcessedAudit {
    static final Pattern FILE=Pattern.compile("GenericXMLproducts(\\d{14})\\.xml",Pattern.CASE_INSENSITIVE);
    static String norm(String s){return s==null?"":s.trim().replaceFirst("^0+(?!$)","");}
    static String id(String s){s=s==null?"":s.trim();return s.length()==15&&!s.startsWith("S")?"1"+s:s;}
    static String quote(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
    static SAXParser parser() throws Exception {
        SAXParserFactory f=SAXParserFactory.newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        f.setFeature("http://xml.org/sax/features/external-general-entities",false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        return f.newSAXParser();
    }
    static Map<String,JSONObject> readFile(Path p,SAXParser parser) throws Exception {
        Map<String,JSONObject> records=new HashMap<>();
        parser.parse(p.toFile(),new DefaultHandler(){
            Map<String,String> values; String field; StringBuilder text=new StringBuilder();
            public void startElement(String u,String l,String q,Attributes a){
                if(q.equals("Product"))values=new HashMap<>();
                if(q.equals("Value")){field=a.getValue("AttributeID");text.setLength(0);}
            }
            public void characters(char[] c,int s,int n){if(field!=null)text.append(c,s,n);}
            public void endElement(String u,String l,String q){
                if(q.equals("Value")){if(values!=null&&field!=null)values.put(field,text.toString());field=null;}
                if(q.equals("Product")&&values!=null){
                    String sku=norm(values.get("MATNR"));
                    if(!sku.isEmpty())records.put(sku,new JSONObject().put("sku",sku)
                        .put("attyp",values.getOrDefault("ATTYP","").trim())
                        .put("incomingId",id(values.get("PRODUCT_ID")))
                        .put("parentSku",norm(values.get("SATNR"))));
                    values=null;
                }
            }
        });
        return records;
    }
    static void index(Path dir,Path output) throws Exception {
        Files.createDirectories(output);
        List<Path> files=new ArrayList<>();
        try(DirectoryStream<Path> stream=Files.newDirectoryStream(dir)){
            for(Path p:stream)if(FILE.matcher(p.getFileName().toString()).matches()&&Files.isRegularFile(p))files.add(p);
        }
        files.sort(Comparator.comparing((Path p)->p.getFileName().toString().toLowerCase(Locale.ROOT)).thenComparing(Path::toString));
        Map<String,JSONObject> latest=new TreeMap<>();SAXParser xml=parser();int count=0,errors=0;
        try(BufferedWriter bad=Files.newBufferedWriter(output.resolve("index-errors.jsonl"),StandardCharsets.UTF_8)){
            for(Path p:files){
                try{
                    long size=Files.size(p),mtime=Files.getLastModifiedTime(p).toMillis();
                    Map<String,JSONObject> entries=readFile(p,xml);
                    if(size!=Files.size(p)||mtime!=Files.getLastModifiedTime(p).toMillis())throw new IOException("XML changed during indexing");
                    for(JSONObject row:entries.values())latest.put(row.getString("sku"),row.put("source",p.toAbsolutePath().toString()).put("size",size).put("mtime",mtime));
                }catch(Exception e){errors++;bad.write(new JSONObject().put("source",p.toString()).put("error",e.toString()).toString());bad.newLine();bad.flush();}
                if(++count%500==0)System.out.println("INDEX files="+count+"/"+files.size()+" SKUs="+latest.size()+" errors="+errors);
            }
        }
        Path tmp=output.resolve("latest-products.jsonl.partial");
        try(BufferedWriter w=Files.newBufferedWriter(tmp,StandardCharsets.UTF_8)){
            for(JSONObject row:latest.values()){w.write(row.toString());w.newLine();}
        }
        Files.move(tmp,output.resolve("latest-products.jsonl"),StandardCopyOption.REPLACE_EXISTING);
        JSONObject summary=new JSONObject().put("files",files.size()).put("skus",latest.size()).put("errors",errors);
        Files.writeString(output.resolve("index-summary.json"),summary.toString(2));System.out.println("INDEX_COMPLETE "+summary);
        if(errors>0)throw new IOException("Index incomplete: inspect index-errors.jsonl before auditing");
    }
    static final class Reader {
        final ParseJana122Response parser=new ParseJana122Response();final Method lookup,resolve;
        Reader() throws Exception {
            lookup=ParseJana122Response.class.getDeclaredMethod("lookupIndividualData",String.class,String.class);lookup.setAccessible(true);
            resolve=ParseJana122Response.class.getDeclaredMethod("resolveIndividualTargets",String.class,String.class);resolve.setAccessible(true);
        }
        @SuppressWarnings("unchecked") List<JSONObject> search(String e,String q) throws Exception{return (List<JSONObject>)lookup.invoke(parser,e,q);}
        JSONObject byId(String e,String value) throws Exception {
            if(value.isEmpty())return null;
            List<JSONObject> rows=search(e,(e.equals("Article")?"Article.SupplierAID":"Product2G.ProductNo")+" equals "+quote(value));
            return rows.isEmpty()?null:rows.get(0);
        }
        JSONObject bySku(String e,String sku) throws Exception {
            if(!sku.matches("[0-9]+"))throw new IllegalArgumentException("Invalid SKU: "+sku);
            List<JSONObject> rows=search(e,e+".SKU = "+sku);return rows.isEmpty()?null:rows.get(0);
        }
        JSONObject audit(JSONObject input) throws Exception {
            JSONObject r=new JSONObject(input.toString()).put("checkedAt",java.time.Instant.now().toString());
            String type=r.getString("attyp"),sku=r.getString("sku"),incoming=r.getString("incomingId");
            if(type.equals("01"))return r.put("status","GENERIC_NO_RELATION_REQUIRED");
            JSONObject article;String expected;
            if(type.equals("00")){
                JanaIndividualTargets t=(JanaIndividualTargets)resolve.invoke(parser,sku,incoming);
                r.put("expectedProduct",t.productId).put("article",t.articleId);
                if(t.newProduct)return r.put("status","MISSING_PRODUCT");
                expected=t.productId;article=byId("Article",t.articleId);
            }else if(type.equals("02")){
                article=byId("Article",incoming);if(article==null)article=bySku("Article",sku);
                if(article==null)return r.put("status","MISSING_ARTICLE");
                r.put("article",article.getString("identifier"));
                if(r.getString("parentSku").isEmpty())return r.put("status","MISSING_PARENT_SKU_IN_XML");
                JSONObject product=bySku("Product2G",r.getString("parentSku"));
                if(product==null)return r.put("status","MISSING_PRODUCT");
                expected=product.getString("identifier");r.put("expectedProduct",expected);
            }else return r.put("status","UNKNOWN_ATTYP");
            if(article==null)return r.put("status","MISSING_ARTICLE");
            String actualSku=article.optString("sku","");
            if(!actualSku.isEmpty()&&!actualSku.equals(sku))return r.put("status","ARTICLE_SKU_CONFLICT").put("actualSku",actualSku);
            JSONArray parents=article.optJSONArray("higherLevelProduct");
            r.put("currentParents",parents==null?new JSONArray():parents);
            if(parents==null||parents.length()==0)return r.put("status","MISSING_REFERENCE");
            if(parents.length()>1)return r.put("status","AMBIGUOUS");
            String actual=parents.getJSONObject(0).getJSONObject("_qualification").getString("referencedIdentifier");
            return r.put("status",actual.equals(expected)?"OK":"WRONG_PARENT");
        }
    }
    static String key(JSONObject r){return r.getString("sku")+"|"+r.getString("source")+"|"+r.getLong("mtime");}
    static void audit(Path index,Path out,int limit) throws Exception {
        Files.createDirectories(out);Path lock=out.resolve("audit.lock");
        try(java.nio.channels.FileChannel channel=java.nio.channels.FileChannel.open(lock,StandardOpenOption.CREATE,StandardOpenOption.WRITE);
            java.nio.channels.FileLock held=channel.tryLock()){
            if(held==null)throw new IOException("Another auditor is running in this output directory");
            Path report=out.resolve("relations.jsonl");Set<String> done=new HashSet<>();Map<String,Integer> totals=new TreeMap<>();
            if(Files.exists(report))try(BufferedReader b=Files.newBufferedReader(report)){
                String line;while((line=b.readLine())!=null){JSONObject r=new JSONObject(line);done.add(key(r));totals.merge(r.getString("status"),1,Integer::sum);}
            }
            Reader db=new Reader();int processed=0,consecutiveErrors=0;
            try(BufferedReader b=Files.newBufferedReader(index);BufferedWriter w=Files.newBufferedWriter(report,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND)){
                String line;while((line=b.readLine())!=null){
                    JSONObject input=new JSONObject(line);if(done.contains(key(input)))continue;
                    if(limit>0&&processed>=limit)break;
                    JSONObject r;
                    try{r=db.audit(input);consecutiveErrors=0;}catch(Exception e){
                        Throwable cause=e instanceof InvocationTargetException?e.getCause():e;
                        String msg=String.valueOf(cause);boolean ambiguous=msg.toLowerCase(Locale.ROOT).contains("ambigu");
                        r=new JSONObject(input.toString()).put("checkedAt",java.time.Instant.now().toString()).put("status",ambiguous?"AMBIGUOUS":"ERROR").put("error",msg);
                        consecutiveErrors=ambiguous?0:consecutiveErrors+1;
                    }
                    w.write(r.toString());w.newLine();w.flush();processed++;totals.merge(r.getString("status"),1,Integer::sum);
                    Files.writeString(out.resolve("summary.json"),new JSONObject(totals).toString(2));
                    if(processed%25==0)System.out.println("AUDIT processed="+processed+" totals="+totals);
                    if(consecutiveErrors>=5)throw new IOException("Stopped after 5 consecutive query failures; inspect report");
                    if(!r.getString("status").equals("GENERIC_NO_RELATION_REQUIRED"))Thread.sleep(500);
                }
            }
            System.out.println("AUDIT_COMPLETE thisRun="+processed+" totals="+totals);
        }
    }
    public static void main(String[] args){
        try{
            if(args.length<3)throw new IllegalArgumentException("index <processed-dir> <output-dir> | audit <index-jsonl> <output-dir> [max-records]");
            if(args[0].equals("index"))index(Paths.get(args[1]),Paths.get(args[2]));
            else if(args[0].equals("audit"))audit(Paths.get(args[1]),Paths.get(args[2]),args.length>3?Integer.parseInt(args[3]):0);
            else throw new IllegalArgumentException("Unknown mode");
            System.exit(0);
        }catch(Exception e){e.printStackTrace();System.exit(1);}
    }
}
