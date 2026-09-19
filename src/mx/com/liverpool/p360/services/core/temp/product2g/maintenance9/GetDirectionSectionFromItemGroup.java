package mx.com.liverpool.p360.services.core.temp.product2g.maintenance9;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import mx.com.liverpool.p360.services.core.RESTWorkshop;
import mx.com.liverpool.p360.services.core.RESTWrapper;
import mx.com.liverpool.p360.services.core.RequestHandler;

public class GetDirectionSectionFromItemGroup {

//	private static final RESTWrapper rw = new RESTWrapper();
//	
//	public static void main(String[] args) {
//		StringBuilder db = new StringBuilder();
//		java.util.Map<String, String> qp = new java.util.HashMap<>();
//		qp.put("fields", "Product2G.ProductNo,Product2GExtraData.ItemGroup(MX)->LookupValue.Code,Product2GExtraData.ItemGroupS4H(MX)->LookupValue.Code");
//		try(java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(new java.io.FileInputStream(java.nio.file.Paths.get("C:", "opt", "LVP", "desorden", "PROD", "IDs de productos sin Sección con ItemGroup o ItemGroupS4H.csv").toFile())))){
//			String line = null;
//			while((line = br.readLine()) != null) {
//				
//			}
//		}catch(java.io.IOException e) {
//			e.printStackTrace();
//		}
////		qp.put("items", );
//	}
	
	private static final Path DEFAULT_INPUT = Paths.get( "C:\\opt\\LVP\\desorden\\PROD\\IDs sin dirección sección.csv" );
//	private static final Path DEFAULT_INPUT = Paths.get( "C:\\opt\\LVP\\desorden\\PROD\\IDs de productos sin Sección con ItemGroup o ItemGroupS4H.csv" );

    private static final int READ_BATCH_SIZE = 1000;
    private static final int WRITE_BATCH_SIZE = 1000;

    private final RESTWrapper rw = new RESTWrapper();
    private final RESTWorkshop workshop = rw.getRw();

    private final Map<String, String[]> direccionSeccionCache = new HashMap<>();
    private final Map<String, String> direccionPorSeccionCache = new HashMap<>();

    private final Map<String, String> writeQp = new HashMap<>();

    private final RequestHandler direccionSeccionWriter = new RequestHandler(
            new JSONArray()
                    .put(new JSONObject().put("identifier", "Product2GExtraData.Direccion(MX)"))
                    .put(new JSONObject().put("identifier", "Product2GExtraData.Section(MX)")),
            WRITE_BATCH_SIZE,
            request -> rw.writeData("list", "Product2G", null, writeQp, request, this::log));

    public GetDirectionSectionFromItemGroup() {
        writeQp.put("includeObjectsInProtocol", "false");
    }

    public static void main(String[] args) throws Exception {
        Path input = DEFAULT_INPUT;
        boolean apply = true;

        for (String arg : args) {
            if ("--apply".equalsIgnoreCase(arg)) {
                apply = true;
            } else if (arg != null && !arg.trim().isEmpty()) {
                input = Paths.get(arg);
            }
        }

        new GetDirectionSectionFromItemGroup().run(input, apply);
    }

    public void run(Path input, boolean apply) throws Exception {
        if (!Files.isRegularFile(input)) {
            throw new IllegalArgumentException("No existe el archivo: " + input);
        }

        List<String> ids = readIds(input);

        log("Archivo: " + input);
        log("IDs únicos encontrados: " + ids.size());
        log("Modo: " + (apply ? "APPLY" : "DRY-RUN"));

        Path output = buildOutputPath(input);
        Counters counters = new Counters();

        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
            writer.write(
                    "PRODUCT_ID,BUSINESS_CODE,BUSINESS_NAME,ITEM_GROUP,ITEM_GROUP_S4H,"
                  + "DIRECCION_ACTUAL,SECCION_ACTUAL,ESTRUCTURA,"
                  + "DIRECCION_CALCULADA,SECCION_CALCULADA,ACCION,DETALLE");
            writer.newLine();

            for (int from = 0; from < ids.size(); from += READ_BATCH_SIZE) {
                int to = Math.min(from + READ_BATCH_SIZE, ids.size());
                processBatch(ids.subList(from, to), writer, apply, counters);
                log("Procesados " + to + " / " + ids.size());
            }

            if (apply) {
                direccionSeccionWriter.sendData();
            }
        }

        log("");
        log("===========================================");
        log("TERMINADO");
        log("===========================================");
        log("Archivo de auditoría: " + output);
        log("Leídos:               " + counters.read);
        log("Preparados:            " + counters.ready);
        log("Enviados:              " + counters.sent);
        log("Ya tenían sección:     " + counters.alreadyHadSection);
        log("Sin ItemGroup:         " + counters.noItemGroup);
        log("Marketplace:           " + counters.marketplace);
        log("Sin estructura:        " + counters.noStructure);
        log("Sin mapeo Dir/Sección: " + counters.noMapping);
        log("No encontrados:        " + counters.notFound);
        log("Errores:               " + counters.errors);
        log("===========================================");
    }

    private void processBatch(List<String> batch, BufferedWriter auditWriter, boolean apply, Counters counters) {
        Map<String, ProductData> products = new HashMap<>();
        StringBuilder items = new StringBuilder();

        for (String id : batch) {
            if (items.length() > 0) items.append(",");
            items.append("'").append(escapeItemId(id)).append("'@1");
            products.put(id, new ProductData(id));
        }

        Map<String, String> qp = new HashMap<>();
        qp.put("items", items.toString());
        qp.put("pageSize", String.valueOf( READ_BATCH_SIZE ));
        qp.put("fields",
                "Product2G.ProductNo"
              + ",Product2G.Business->LookupValue.Code"
              + ",Product2G.Business->LookupValueLang.Name(es)"
              + ",Product2GExtraData.ItemGroup(MX)->LookupValue.Code"
              + ",Product2GExtraData.ItemGroupS4H(MX)->LookupValue.Code"
              + ",Product2GExtraData.Direccion(MX)->LookupValue.Code"
              + ",Product2GExtraData.Section(MX)->LookupValue.Code");

        try {
            rw.collectData("list", "Product2G", null, "byItems", qp, row -> {
                try {
                	JSONArray values = row.getJSONArray("values");
                	String productNo = valueAt(values, 0);

                    if (isBlank(productNo)) {
                        log("Row sin Product2G.ProductNo, no se puede correlacionar con el archivo: " + row);
                        return;
                    }

                    ProductData p = products.get(productNo);
                    if (p == null) {
                        p = new ProductData(productNo);
                        products.put(productNo, p);
                    }

                    p.found = true;
                    p.businessCode     = valueAt(values, 1);
                    p.businessName     = valueAt(values, 2);
                    p.itemGroup        = normalizeGroup(valueAt(values, 3));
                    p.itemGroupS4H     = normalizeGroup(valueAt(values, 4));
                    p.currentDirection = valueAt(values, 5);
                    p.currentSection   = valueAt(values, 6);
                } catch (Exception e) {
                    log("Error leyendo row de Product2G: " + row);
                    logException(e);
                }
            }, this::log);
        } catch (Exception e) {
            log("Error leyendo lote Product2G: " + items);
            logException(e);
            for (String id : batch) {
                ProductData p = products.get(id);
                p.detail = "ERROR_READING_PRODUCT_BATCH";
                p.action = "ERROR";
                counters.errors++;
                writeAuditQuietly(auditWriter, p);
            }
            return;
        }

        for (String id : batch) {
            counters.read++;
            ProductData p = products.get(id);

            if (p == null || !p.found) {
                if (p == null) p = new ProductData(id);
                p.action = "NOT_FOUND";
                p.detail = "Product2G no fue devuelto por /list/Product2G/byItems";
                counters.notFound++;
                writeAuditQuietly(auditWriter, p);
                continue;
            }

            try {
                resolveAndPrepare(p, apply, counters);
            } catch (Exception e) {
                p.action = "ERROR";
                p.detail = e.getClass().getSimpleName() + ": " + nullToEmpty(e.getMessage());
                counters.errors++;
                log("Error con Product2G " + p.id);
                logException(e);
            }

            writeAuditQuietly(auditWriter, p);
        }
    }

    private void resolveAndPrepare(ProductData p, boolean apply, Counters counters) throws Exception {
        if (!isBlank(p.currentSection)) {
            p.action = "SKIP_ALREADY_HAS_SECTION";
            p.detail = "Section actual=" + p.currentSection;
            counters.alreadyHadSection++;
            return;
        }

        String structure = resolveStructure(p);
        p.structure = structure;
        if ("MARKETPLACE".equals(structure)) {
            p.action = "SKIP_MARKETPLACE";
            p.detail = "CreateProposal no calcula Dirección/Sección para Marketplace";
            counters.marketplace++;
            return;
        }

        if (isBlank(structure)) {
            p.action = "SKIP_NO_STRUCTURE";
            p.detail = "No pude determinar CommercialECC/CommercialS4H";
            counters.noStructure++;
            return;
        }

        String group = selectGroup(p, structure);
        if (isBlank(group)) {
            p.action = "SKIP_NO_ITEMGROUP";
            p.detail = "No tiene ItemGroup ni ItemGroupS4H utilizable";
            counters.noItemGroup++;
            return;
        }
        String[] ds = getDireccionSeccion(group, structure);
        if (ds == null || ds.length < 2 || isBlank(ds[0]) || isBlank(ds[1])) {
            p.action = "SKIP_NO_MAPPING";
            p.detail = "No hubo mapeo de StructureGroup para " + group;
            counters.noMapping++;
            return;
        }

        p.calculatedDirection = ds[0];
        p.calculatedSection = ds[1];

        if (!isBlank(p.currentDirection) && !p.currentDirection.equals(p.calculatedDirection)) {
            p.action = "SKIP_DIRECTION_MISMATCH";
            p.detail = "Direccion actual=" + p.currentDirection + ", calculada=" + p.calculatedDirection;
            counters.errors++;
            return;
        }

        counters.ready++;

        if (!apply) {
            p.action = "DRY_RUN";
            p.detail = "Listo para insertar Dirección/Sección";
            return;
        }

        direccionSeccionWriter.addRow(
                new JSONObject()
                        .put("object", new JSONObject().put("id", "'" + escapeItemId(p.id) + "'@1"))
                        .put("values", new JSONArray()
                                .put(p.calculatedDirection)
                                .put(p.calculatedSection)));

        p.action = "QUEUED";
        p.detail = "Agregado al RequestHandler";
        counters.sent++;
    }

    private String[] getDireccionSeccion(String groupOfArticle, String structure) throws Exception {
        String group = normalizeGroup(groupOfArticle);
        if (isBlank(group)) return null;

        String cacheKey = structure + "|" + group;
        if (direccionSeccionCache.containsKey(cacheKey)) {
            return direccionSeccionCache.get(cacheKey);
        }

        Map<String, String> qp = new HashMap<>();
        qp.put("structure", structure);
        qp.put("query", "StructureGroup.Identifier wildcard \"" + escapeSearchValue(group) + "-L4%\"");
        qp.put("fields", "StructureGroup.ParentIdentifier,StructureGroupLang.Name(es)");

        JSONObject response = workshop.makeRequest("GET", "/list/StructureGroup/bySearch", qp, null);
        if (response == null || !response.has("rows") || response.getJSONArray("rows").length() == 0) {
            log("Sin sección para " + structure + " / " + group + " -> " + workshop.getRawResponse());
            direccionSeccionCache.put(cacheKey, null);
            return null;
        }

        JSONArray values = response.getJSONArray("rows").getJSONObject(0).getJSONArray("values");
        String sectionIdentifier = valueAt(values, 0);

        if (isBlank(sectionIdentifier)) {
            direccionSeccionCache.put(cacheKey, null);
            return null;
        }

        String directionCacheKey = structure + "|" + sectionIdentifier;
        String directionIdentifier = direccionPorSeccionCache.get(directionCacheKey);

        if (directionIdentifier == null) {
            Map<String, String> qp2 = new HashMap<>();
            qp2.put("structure", structure);
            qp2.put("query", "StructureGroup.Identifier wildcard \"" + escapeSearchValue(sectionIdentifier) + "\"");
            qp2.put("fields", "StructureGroup.ParentIdentifier,StructureGroupLang.Name(es)");

            JSONObject response2 = workshop.makeRequest("GET", "/list/StructureGroup/bySearch", qp2, null);

            if (response2 == null || !response2.has("rows") || response2.getJSONArray("rows").length() == 0) {
                log("Sin dirección para " + structure + " / sección " + sectionIdentifier
                        + " -> " + workshop.getRawResponse());
                direccionSeccionCache.put(cacheKey, null);
                return null;
            }

            JSONArray values2 = response2.getJSONArray("rows").getJSONObject(0).getJSONArray("values");
            directionIdentifier = valueAt(values2, 0);

            if (isBlank(directionIdentifier)) {
                direccionSeccionCache.put(cacheKey, null);
                return null;
            }

            direccionPorSeccionCache.put(directionCacheKey, directionIdentifier);
        }

        String direction = stripHierarchySuffix(directionIdentifier);
        String section = stripHierarchySuffix(sectionIdentifier);

        String[] result = new String[] { direction, section };
        direccionSeccionCache.put(cacheKey, result);

        log("Mapeo " + structure + " / " + group
                + " => Dirección=" + direction + ", Sección=" + section);

        return result;
    }

    private String resolveStructure(ProductData p) {
    	
    	if (!isBlank(p.itemGroup) && isBlank(p.itemGroupS4H)) return "CommercialECC";
    	if (!isBlank(p.itemGroupS4H) && isBlank(p.itemGroup)) return "CommercialS4H";

    	String code = upper(p.businessCode);
        String name = upper(p.businessName);

        if ("MKP".equals(code) || name.contains("MARKETPLACE")) return "CommercialECC";
        if ("LVP".equals(code) || name.contains("LIVERPOOL")) return "CommercialECC";
        if ("SBB".equals(code) || name.contains("SUBURBIA")) return "CommercialS4H";


        return null;
    }

    private String selectGroup(ProductData p, String structure) {
        if ("CommercialECC".equals(structure)) return p.itemGroup;
        if ("CommercialS4H".equals(structure)) return p.itemGroupS4H;
        return !isBlank(p.itemGroup) ? p.itemGroup : p.itemGroupS4H;
    }

    private List<String> readIds(Path input) throws IOException {
        Set<String> ids = new LinkedHashSet<>();

        try (BufferedReader br = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                line = stripBom(line).trim();
                if (line.isEmpty()) continue;

                String first = firstCsvColumn(line);

                if (firstLine) {
                    firstLine = false;
                    String u = upper(first);
                    if ("ID".equals(u)
                            || "PRODUCT_ID".equals(u)
                            || "PRODUCT2G".equals(u)
                            || "PRODUCT2G_ID".equals(u)
                            || u.contains("PRODUCT")) {
                        continue;
                    }
                }

                first = unquote(first).trim();
                if (!first.isEmpty()) ids.add(first);
            }
        }

        return new ArrayList<>(ids);
    }

    private String firstCsvColumn(String line) {
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                quoted = !quoted;
                continue;
            }

            if (!quoted && (c == ',' || c == ';' || c == '\t' || c == '|')) {
                return line.substring(0, i);
            }
        }

        return line;
    }

    private Path buildOutputPath(Path input) {
        String name = input.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return input.resolveSibling(base + "_direccion_seccion.csv");
    }

    private void writeAuditQuietly(BufferedWriter bw, ProductData p) {
        try {
            writeAudit(bw, p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeAudit(BufferedWriter bw, ProductData p) throws IOException {
        bw.write(csv(p.id)); bw.write(",");
        bw.write(csv(p.businessCode)); bw.write(",");
        bw.write(csv(p.businessName)); bw.write(",");
        bw.write(csv(p.itemGroup)); bw.write(",");
        bw.write(csv(p.itemGroupS4H)); bw.write(",");
        bw.write(csv(p.currentDirection)); bw.write(",");
        bw.write(csv(p.currentSection)); bw.write(",");
        bw.write(csv(p.structure)); bw.write(",");
        bw.write(csv(p.calculatedDirection)); bw.write(",");
        bw.write(csv(p.calculatedSection)); bw.write(",");
        bw.write(csv(p.action)); bw.write(",");
        bw.write(csv(p.detail));
        bw.newLine();
        bw.flush();
    }

    private static String valueAt(JSONArray values, int index) {
        if (values == null || index >= values.length()) return null;

        Object v = values.opt(index);
        if (v == null || JSONObject.NULL.equals(v)) return null;

        if (v instanceof JSONArray) {
            JSONArray a = (JSONArray) v;
            if (a.length() == 0) return null;
            return scalar(a.opt(0));
        }

        return scalar(v);
    }

    private static String scalar(Object v) {
        if (v == null || JSONObject.NULL.equals(v)) return null;

        if (v instanceof JSONObject) {
            JSONObject jo = (JSONObject) v;
            if (jo.has("_code")) return trimToNull(jo.optString("_code", null));
            if (jo.has("_label")) return trimToNull(jo.optString("_label", null));
            return trimToNull(jo.toString());
        }

        return trimToNull(String.valueOf(v));
    }

    private static String normalizeObjectId(String raw) {
        if (raw == null) return "";

        String s = raw.trim();
        int at = s.lastIndexOf('@');
        if (at > 0) s = s.substring(0, at);

        return unquote(s);
    }

    private static String normalizeGroup(String value) {
        String s = trimToNull(value);
        if (s == null) return null;

        if (s.matches("\\d+ ?- ?.+")) {
            s = s.replaceAll(" ?- ?.+", "");
        }

        return trimToNull(s);
    }

    private static String stripHierarchySuffix(String value) {
        String s = trimToNull(value);
        return s == null ? null : s.replaceAll("-.+", "");
    }

    private static String escapeItemId(String s) {
        return nullToEmpty(s).replace("'", "''");
    }

    private static String escapeSearchValue(String s) {
        return nullToEmpty(s).replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String csv(String s) {
        String value = nullToEmpty(s);
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static String stripBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') {
            return s.substring(1);
        }
        return s;
    }

    private static String unquote(String s) {
        String v = nullToEmpty(s).trim();

        if (v.length() >= 2
                && ((v.startsWith("\"") && v.endsWith("\""))
                    || (v.startsWith("'") && v.endsWith("'")))) {
            return v.substring(1, v.length() - 1);
        }

        return v;
    }

    private static String upper(String s) {
        return nullToEmpty(s).trim().toUpperCase(Locale.ROOT);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private void log(String msg) {
        System.out.println("[" + java.time.LocalDateTime.now() + "] " + msg);
    }

    private void logException(Throwable t) {
        if (t != null) t.printStackTrace(System.out);
    }

    private static class ProductData {
        final String id;
        boolean found;
        String businessCode;
        String businessName;
        String itemGroup;
        String itemGroupS4H;
        String currentDirection;
        String currentSection;
        String structure;
        String calculatedDirection;
        String calculatedSection;
        String action;
        String detail;

        ProductData(String id) {
            this.id = id;
        }
    }

    private static class Counters {
        long read;
        long ready;
        long sent;
        long alreadyHadSection;
        long noItemGroup;
        long marketplace;
        long noStructure;
        long noMapping;
        long notFound;
        long errors;
    }
	
}
