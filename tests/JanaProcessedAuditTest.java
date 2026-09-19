package mx.com.liverpool.p360.services.core.sftp;
import java.nio.file.*;
import org.json.*;
public class JanaProcessedAuditTest {
    public static void main(String[] args)throws Exception{
        Path root=Files.createTempDirectory("jana-audit-test-"),in=Files.createDirectory(root.resolve("in")),out=root.resolve("out");
        String first="<Products><Product><Values><Value AttributeID=\"MATNR\">000123</Value><Value AttributeID=\"ATTYP\">00</Value><Value AttributeID=\"PRODUCT_ID\">754611687045246</Value></Values></Product></Products>";
        Files.writeString(in.resolve("GenericXMLproducts20260901000000.XML"),first);
        Files.writeString(in.resolve("GenericXMLproducts20260902000000.xml"),first.replace(">00<",">02<"));
        Files.writeString(in.resolve("GenericXMLattributes20260903000000.XML"),first);
        JanaProcessedAudit.index(in,out);
        java.util.List<String> lines=Files.readAllLines(out.resolve("latest-products.jsonl"));
        if(lines.size()!=1)throw new AssertionError("Latest SKU not deduplicated");
        JSONObject row=new JSONObject(lines.get(0));
        if(!row.getString("sku").equals("123")||!row.getString("attyp").equals("02")||!row.getString("incomingId").equals("1754611687045246"))throw new AssertionError(row);
        Path bad=in.resolve("GenericXMLproducts20260904000000.XML");Files.writeString(bad,"<!DOCTYPE root [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><Products>&x;</Products>");
        try{JanaProcessedAudit.index(in,root.resolve("bad"));throw new AssertionError("Unsafe XML accepted");}catch(java.io.IOException expected){}
        System.out.println("PASS: latest per SKU, case-insensitive extension, products-only, normalized IDs, malformed/DOCTYPE stops audit");
    }
}
