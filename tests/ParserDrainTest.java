package mx.com.liverpool.p360.services.core.sftp;
import java.nio.file.*;import java.util.*;import java.util.concurrent.atomic.AtomicInteger;import org.json.*;
public class ParserDrainTest {
 static void check(boolean ok,String m){if(!ok)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  Path p=Files.createTempDirectory("parser-drain-test-");DurableSftpQueue.atomic(p.resolve("job.json"),new JSONObject().put("name","file").toString());
  DurableSftpQueue.ACTIVE.set(new DurableSftpQueue.Job(p));List<Integer> applied=new ArrayList<>();
  try{DurableSftpQueue.records(List.of(0,1,2),i->{if(i==1)throw new java.io.IOException("unresolved");applied.add(i);},s->{});throw new AssertionError();}catch(java.io.IOException expected){}
  check(applied.equals(List.of(0,2)),"One failed row must not block later rows");
  DurableSftpQueue.ACTIVE.set(new DurableSftpQueue.Job(p));
  DurableSftpQueue.records(List.of(0,1,2),applied::add,s->{});check(applied.equals(List.of(0,2,1)),"Restart replays only missing row");
  AtomicInteger sends=new AtomicInteger(),writes=new AtomicInteger();
  var batch=new SkuPublicationBatch((e,r)->writes.incrementAndGet(),body->{sends.incrementAndGet();return "ack";},id->"P",s->{});
  batch.begin("file");batch.add("Article","A","000123","P");batch.finish();
  DurableSftpQueue.ACTIVE.set(new DurableSftpQueue.Job(p));batch.begin("file");batch.add("Article","A","000123","P");batch.finish();check(sends.get()==1&&writes.get()==1,"No repeat of confirmed SKU on restart");
  JSONObject unknown=new JSONObject().put("entity","Article").put("id","B").put("sku","0").put("parent","P");DurableSftpQueue.beforePublish(List.of(unknown));
  try{DurableSftpQueue.beforePublish(List.of(unknown));throw new AssertionError();}catch(IllegalStateException expected){}
  DurableSftpQueue.ACTIVE.remove();
  String xml="<Root X='1'><Products><Product ID='a'><Values><Value AttributeID='MATNR'>000123</Value></Values></Product><Product ID='b'><Values/></Product></Products></Root>";
  var parts=AttributeRecordXml.split(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));check(parts.size()==2,"split count");String first=new String(parts.get(0),java.nio.charset.StandardCharsets.UTF_8);check(first.contains("000123")&&!first.contains("ID=\"b\""),"preserves record and leading zeros");
  try{AttributeRecordXml.split("<!DOCTYPE x [<!ENTITY xx SYSTEM 'file:///etc/passwd'>]><Root/>".getBytes());throw new AssertionError();}catch(org.xml.sax.SAXException expected){}
  System.out.println("PASS checkpoints/restart, later good rows, SKU ACK suppression, uncertain ACK hold, XML splitting/XXE");
 }
}
