package mx.com.liverpool.p360.services.core.sftp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import mx.com.liverpool.p360.services.core.sftp.handlers.ECC122AttributesHandler;
import mx.com.liverpool.p360.services.core.sftp.handlers.Value;

public final class AttributeImportSupportTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private interface Checked { void run() throws Exception; }
    private static void fails(Checked action, String message) throws Exception {
        boolean rejected = false;
        try { action.run(); } catch (IOException expected) { rejected = true; }
        check(rejected, message);
    }
    private static Value field(String name, String text) {
        Value v = new Value(); v.setAttributeId(name); v.setText(text); return v;
    }
    private static final class FakeWorkshop extends mx.com.liverpool.p360.services.core.RESTWorkshop {
        JSONObject article;
        final java.util.List<String> paths = new java.util.ArrayList<>();
        final java.util.List<JSONObject> bodies = new java.util.ArrayList<>();
        @Override public JSONObject makeRequest(String method, String path, Map<String, String> qp, String message) {
            if ("GET".equals(method)) return article;
            paths.add(path); bodies.add(new JSONObject(message));
            return new JSONObject().put("_protocol", new JSONObject().put("errorCounter", 0));
        }
    }
    public static void main(String[] args) throws Exception {
        JSONObject article = new JSONObject(Files.readString(Path.of(args[0])));
        check("SBB5016513939".equals(AttributeImportSupport.parentIdentifier(article)), "Real Jana child routes to its parent");
        check(!"SBB5016513964".equals(AttributeImportSupport.parentIdentifier(article)), "Do not use child SKU as parent ID");
        FakeWorkshop api = new FakeWorkshop();
        api.article = article;
        String parent = AttributeImportSupport.parentOf(api, "SBB5016513964");
        JSONArray records = new JSONArray();
        JanaAttributeParser.addValues("SB_0001", new JSONArray().put(new JSONObject().put("_code", "0022")), records);
        AttributeImportSupport.write(api, "Product2G", parent, records);
        check(api.paths.equals(List.of("/object/Product2G/'SBB5016513939'@1")), "Actual outgoing route uses parent, not child");
        check(api.bodies.get(0).getJSONArray("_characteristicRecords").getJSONObject(0)
                .getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values").getJSONObject(0)
                .getString("_code").equals("0022"), "Outgoing product payload preserves catalog code");
        AttributeImportSupport.write(api, "Article", null, new JSONArray());
        check(api.paths.size() == 1, "Product-only import does not invent an Article write");
        fails(() -> AttributeImportSupport.write(api, "Article", null, records), "Pending Article fails before sending data");
        check(api.paths.size() == 1, "No write was sent to unresolved Article");
        check(AttributeImportSupport.parentIdentifier(new JSONObject().put("_data", new JSONObject())) == null,
                "Absent parent remains unresolved");
        fails(() -> AttributeImportSupport.parentIdentifier(null), "Unavailable API must not look like no parent");
        JSONObject ambiguous = new JSONObject().put("_data", new JSONObject().put("higherLevelProduct",
                new JSONArray().put(new JSONObject().put("_qualification", new JSONObject().put("referencedIdentifier", "P1")))
                .put(new JSONObject().put("_qualification", new JSONObject().put("referencedIdentifier", "P2")))));
        fails(() -> AttributeImportSupport.parentIdentifier(ambiguous), "Multiple parents must not select arbitrarily");
        AttributeImportSupport.requireTargets("SKU", "P1", null, true, false);
        check(true, "Product-only attributes do not require an article");
        fails(() -> AttributeImportSupport.requireTargets("SKU", "P1", null, true, true), "Individual without article stays pending");
        fails(() -> AttributeImportSupport.requireTargets("SKU", null, "A1", true, true), "Variant without parent stays pending");
        AttributeImportSupport.requireTargets("SKU", "P1", "A1", true, true);
        check(true, "Retry can proceed when both targets become visible");
        fails(() -> AttributeImportSupport.requireSuccess(null, "test"), "Missing response does not acknowledge XML");
        fails(() -> AttributeImportSupport.requireSuccess(new JSONObject().put("_protocol", new JSONObject().put("errorCounter", 1)), "test"),
                "P360 business error does not acknowledge XML");
        AttributeImportSupport.requireSuccess(new JSONObject().put("_protocol", new JSONObject().put("errorCounter", 0)), "test");
        check(true, "Successful object response");
        Map<String, List<String>> parsed = AttributeImportSupport.eccAttributes(List.of(
                field("ZZIDCONSEC", "1"), field("ATNAM", "A"), field("ATWRT", "0003"),
                field("ZZIDCONSEC", "2"), field("ATNAM", "A"), field("ATWRT", "0004"),
                field("ZZIDCONSEC", "3"), field("ATNAM", "B"), field("ATWRT", null)));
        check(parsed.get("A").equals(List.of("0003", "0004")), "ECC keeps repeated attribute values");
        check(!parsed.containsKey("B"), "ECC empty value preserves existing data");
        check("12".equals(AttributeImportSupport.eccSku(List.of(field("OTHER", "99"), field("MATNR", "00012")))), "ECC SKU is named MATNR");
        ECC122AttributesHandler handler = new ECC122AttributesHandler();
        javax.xml.parsers.SAXParserFactory.newInstance().newSAXParser().parse(Path.of(args[1]).toFile(), handler);
        check(handler.getCollected().size() == 3, "One real ECC sample contains three products");
        String[] skus = {"1193252385", "1193252393", "1204692374"};
        String[] sizes = {"0176", "0171"};
        for (int i = 0; i < skus.length; i++) {
            ECC122AttributesHandler.Product product = handler.getCollected().get(i);
            check(skus[i].equals(AttributeImportSupport.eccSku(product.getValues())), "Real ECC SKU " + i);
            Map<String, List<String>> attributes = AttributeImportSupport.eccAttributes(product.getAttributes());
            if (i < 2) {
                check(attributes.get("C100").equals(List.of("0300")), "Real ECC color " + i);
                check(attributes.get("TDI01").equals(List.of(sizes[i])), "Real ECC size " + i);
            } else {
                check(attributes.get("PE000").equals(List.of("2724")), "Real ECC non-size attribute");
            }
        }
        System.out.println("PASS: " + checks + " checks; real Jana parent and one real ECC XML; no writes or network calls.");
    }
}
