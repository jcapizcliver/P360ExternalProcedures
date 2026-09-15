package mx.com.liverpool.p360.services.core.sftp;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Offline regression checks; no P360, DB, or SFTP initialization. */
public final class JanaAttributeParserTest {
    private static int checks;
    private static final List<String> errors = new ArrayList<>();
    private static final Map<String, Map<String, String>> codes = Map.of(
            "COLOR_LOV", Map.of("0003", "Azul"), "TamanoUnicoLOV", Map.of("M", "Mediana"),
            "UNITS", Map.of("KG", "Kilogramo"));
    private static final Map<String, Map<String, String>> labels = Map.of(
            "COLOR_LOV", Map.of("Azul", "0003"), "TamanoUnicoLOV", Map.of("Mediana", "M"),
            "UNITS", Map.of("Kilogramo", "KG"));

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    private static Element xml(String input) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8))).getDocumentElement();
    }
    private static Object convert(String type, String value) {
        return JanaAttributeParser.resolve("Color", type, value, "COLOR_LOV", codes, labels, errors::add);
    }
    private static String field(String id, String value) {
        return "<Value AttributeID='" + id + "'>" + value + "</Value>";
    }
    public static void main(String[] args) throws Exception {
        Element product = xml("<Values>" + field("OTHER", "wrong") + field("MATNR", " 00001234 ")
                + "<Attributes>" + field("ZZIDCONSEC", "1") + field("ATNAM", "COLOR")
                + field("EXTRA", "ignore") + field("ATWRT", "0003")
                + field("ZZIDCONSEC", "2") + field("ATWRT", "Algodón &amp; lino") + field("ATNAM", "MATERIAL")
                + field("ZZIDCONSEC", "3") + field("ATNAM", "COLOR") + field("ATWRT", "0004")
                + "</Attributes></Values>");
        check("1234".equals(JanaAttributeParser.sku(product)), "MATNR must be selected by name");
        Map<String, List<String>> parsed = JanaAttributeParser.attributes(product);
        check(parsed.get("COLOR").equals(List.of("0003", "0004")), "Repeated attributes must retain all values and leading zeros");
        check(parsed.get("MATERIAL").equals(List.of("Algodón & lino")), "Named fields must tolerate extra fields/order and decode XML");
        check(!parsed.containsKey("TamanoUnico"), "Partial input must not invent a size");
        check(JanaAttributeParser.attributes(xml("<Values/>" )).isEmpty(), "Missing attributes");
        check(JanaAttributeParser.sku(xml("<Values>" + field("OTHER", "12") + "</Values>")) == null, "Missing MATNR");
        check(JanaAttributeParser.attributes(xml("<Values><Attributes>" + field("ATNAM", "A") + field("ATWRT", " ")
                + "</Attributes></Values>")).isEmpty(), "Empty values must not erase data");
        parsed = JanaAttributeParser.attributes(xml("<Values><Attributes>" + field("ATNAM", "A") + field("ATWRT", "one")
                + field("ATNAM", "B") + field("ATWRT", "two") + "</Attributes></Values>"));
        check(parsed.size() == 2, "Named pairs without sequence fields");
        boolean rejected = false;
        try {
            JanaAttributeParser.attributes(xml("<Values><Attributes>" + field("ZZIDCONSEC", "1")
                    + field("ATNAM", "A") + field("ZZIDCONSEC", "2") + field("ATNAM", "B") + field("ATWRT", "bad")
                    + "</Attributes></Values>"));
        } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Incomplete records must not shift later attributes into the wrong name");
        check("0003".equals(((JSONObject)convert("LOOKUP", "0003")).getString("_code")), "Lookup by catalog ID, code input");
        check("0003".equals(((JSONObject)convert("LOOKUP", "Azul")).getString("_code")), "Lookup by label");
        check(convert("LOOKUP", "unknown") == null, "Unknown lookup must not write raw value");
        Object size = JanaAttributeParser.resolve("TamanoUnico", "LOOKUP", "Mediana", "TamanoUnicoLOV", codes, labels, errors::add);
        check("M".equals(((JSONObject)size).getString("_code")), "Size lookup preserved");
        Object units = JanaAttributeParser.resolve("UnidadDeMedidaPeso", "LOOKUP", "KG", "UNITS", codes, labels, errors::add);
        check("KG".equals(((JSONObject)units).getString("_code")), "Unit lookup must not be excluded");
        check(convert("INTEGER", "12.0").equals(12), "Integer conversion");
        check(convert("INTEGER", "12.5") == null, "No silent truncation");
        check(convert("INTEGER", "2147483648") == null, "No integer overflow");
        check(convert("DECIMAL", "123456789.123456789").toString().equals("123456789.123456789"), "Preserve decimal precision");
        check(convert("DECIMAL", "garbage") == null, "Reject malformed numbers");
        check(Boolean.TRUE.equals(convert("BOOLEAN", "true")), "True");
        check(Boolean.TRUE.equals(convert("BOOLEAN", "1")), "One");
        check(Boolean.FALSE.equals(convert("BOOLEAN", "0")), "Zero");
        check(convert("BOOLEAN", "unknown") == null, "Invalid booleans must not become false");
        check("2026-08-10".equals(convert("DATE", "20260810")), "SAP date");
        check("2026-08-10".equals(convert("DATE", "2026-08-10")), "ISO date");
        check(convert("DATE", "20260230") == null, "Reject impossible date");
        check("Descripción ñ".equals(convert("STRING", "Descripción ñ")), "Text preservation");
        JSONArray records = new JSONArray();
        JanaAttributeParser.addValues("Color", new JSONArray().put(convert("LOOKUP", "Azul")), records);
        JanaAttributeParser.addValues("Material", new JSONArray().put("Algodón").put("Lino"), records);
        JanaAttributeParser.addValues("Unknown", new JSONArray(), records);
        check(records.length() == 2, "Payload includes non-size attributes, excludes unresolved values");
        JSONArray materials = records.getJSONObject(1).getJSONArray("_recordLang").getJSONObject(0).getJSONArray("values");
        check(materials.length() == 2, "Multivalues share one characteristic record");

        if (args.length == 0) {
            System.out.println("PASS: " + checks + " checks; no external calls.");
            return;
        }
        Path input = Path.of(args[0]);
        List<Path> files;
        if (java.nio.file.Files.isDirectory(input)) {
            try (java.util.stream.Stream<Path> entries = java.nio.file.Files.list(input)) {
                files = entries.filter(java.nio.file.Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".xml"))
                        .sorted().toList();
            }
        } else {
            files = List.of(input);
        }
        check(!files.isEmpty(), "No XML fixtures found");
        int products = 0, values = 0, empty = 0, repeated = 0, rawValues = 0;
        java.util.Set<String> names = new java.util.TreeSet<>();
        for (Path file : files) {
            Document real = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.toFile());
            for (int i = 0; i < real.getElementsByTagName("Product").getLength(); i++) {
                Element item = (Element)real.getElementsByTagName("Product").item(i);
                Element v = (Element)item.getElementsByTagName("Values").item(0);
                String context = file.getFileName() + ": product " + i;
                // Independent oracle for the sequence/name/value triplets in these real exports.
                Element matnr = (Element)v.getElementsByTagName("Value").item(0);
                check("MATNR".equals(matnr.getAttribute("AttributeID")), context + " MATNR fixture layout");
                String expectedSku = matnr.getTextContent().trim().replaceFirst("^0+(?!$)", "");
                check(expectedSku.equals(JanaAttributeParser.sku(v)), context + " SKU");
                Map<String, List<String>> expected = new java.util.LinkedHashMap<>();
                org.w3c.dom.NodeList groups = v.getElementsByTagName("Attributes");
                for (int group = 0; group < groups.getLength(); group++) {
                    org.w3c.dom.NodeList fields = ((Element)groups.item(group)).getElementsByTagName("Value");
                    check(fields.getLength() % 3 == 0, context + " complete triplets");
                    for (int j = 0; j < fields.getLength(); j += 3) {
                        check("ZZIDCONSEC".equals(((Element)fields.item(j)).getAttribute("AttributeID"))
                                && "ATNAM".equals(((Element)fields.item(j + 1)).getAttribute("AttributeID"))
                                && "ATWRT".equals(((Element)fields.item(j + 2)).getAttribute("AttributeID")),
                                context + " triplet layout " + j);
                        String name = fields.item(j + 1).getTextContent().trim();
                        String value = fields.item(j + 2).getTextContent().trim();
                        names.add(name);
                        rawValues++;
                        if (value.isEmpty()) {
                            empty++;
                        } else {
                            List<String> existing = expected.computeIfAbsent(name, key -> new ArrayList<>());
                            if (existing.contains(value)) repeated++;
                            else existing.add(value);
                        }
                    }
                }
                Map<String, List<String>> actual = JanaAttributeParser.attributes(v);
                check(expected.equals(actual), context + " all names/values must match: " + expected + " vs " + actual);
                values += actual.values().stream().mapToInt(List::size).sum();
                products++;
            }
        }
        check(rawValues == values + empty + repeated, "Every input value accounted for");
        System.out.println("PASS: " + checks + " checks; " + files.size() + " XML files; " + products
                + " products; " + rawValues + " input values; " + values + " parsed values; " + empty
                + " empty values; " + repeated + " duplicates; " + names.size() + " attribute names; no external calls.");
    }
}
