package mx.com.liverpool.p360.services.core.sftp;
import java.nio.file.*;
import java.util.*;
import org.json.*;
public class ECCUnmappedAttributesTest {
 public static void main(String[] args) throws Exception {
  Path dir=Files.createTempDirectory("ecc-unmapped-test");Path file=dir.resolve("pending.jsonl");
  Map<String,List<String>> data=new LinkedHashMap<>();data.put("KNOWN",List.of("ok"));data.put("AE018",List.of("azul\nmarino","ñ",""));
  ECCUnmappedAttributes.deferUnmapped(data,Map.of("KNOWN","Mapped"),file,"GenericXMLattributes-test.XML","123");
  if(data.size()!=1 || !data.containsKey("KNOWN"))throw new AssertionError("mapped data changed");
  JSONObject row=new JSONObject(Files.readAllLines(file).get(0));
  if(!row.getString("sourceFile").equals("GenericXMLattributes-test.XML") || row.getJSONArray("values").length()!=3 || !row.getJSONArray("values").getString(1).equals("ñ"))throw new AssertionError("evidence lost");
  data.put("UNKNOWN",List.of("retry"));
  try {ECCUnmappedAttributes.deferUnmapped(data,Map.of("KNOWN","Mapped"),dir,"x","123");throw new AssertionError("expected IO failure");}catch(java.io.IOException expected){}
  if(!data.containsKey("UNKNOWN"))throw new AssertionError("removed without evidence");
  ECCUnmappedAttributes.deferUnmapped(data,Map.of("KNOWN","Mapped"),file,"second.XML","123");
  if(Files.readAllLines(file).size()!=2)throw new AssertionError("append failed");
  System.out.println("PASS: mapped retained, raw values and filename preserved, append, write failure retains unmapped");
 }
}
