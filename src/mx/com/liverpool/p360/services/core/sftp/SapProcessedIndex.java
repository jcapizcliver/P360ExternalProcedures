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
public class SapProcessedIndex {
    static String origin;
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
            Map<String,String> values; String proposal; String field; StringBuilder text=new StringBuilder();
            public void startElement(String u,String l,String q,Attributes a){
                if(q.equals("Product")){values=new HashMap<>();proposal=a.getValue("ZNPRST");}
                if(q.equals("Value")){field=a.getValue("AttributeID");text.setLength(0);}
            }
            public void characters(char[] c,int s,int n){if(field!=null)text.append(c,s,n);}
            public void endElement(String u,String l,String q){
                if(q.equals("Value")){if(values!=null&&field!=null)values.put(field,text.toString());field=null;}
                if(q.equals("Product")&&values!=null){
                    String sku=norm(values.get("MATNR"));
                    if(!sku.isEmpty())records.put(sku,new JSONObject().put("sku",sku)
                        .put("attyp",values.getOrDefault("ATTYP","").trim())
                        .put("incomingId",origin.equals("ECC")?(proposal!=null?proposal.trim():values.getOrDefault("ZNPRST","").trim()):id(values.get("PRODUCT_ID")))
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
                    long size=Files.size(p),mtime=Files.getLastModifiedTime(p).toMillis(); if(size==0){System.out.println("EMPTY_XML_IGNORED "+p);continue;}
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
    public static void main(String[] args){try{if(args.length!=3||!Set.of("ECC","S4H").contains(args[2]))throw new IllegalArgumentException("directory output ECC|S4H");origin=args[2];index(Paths.get(args[0]),Paths.get(args[1]));System.exit(0);}catch(Exception e){e.printStackTrace();System.exit(1);}}
}
