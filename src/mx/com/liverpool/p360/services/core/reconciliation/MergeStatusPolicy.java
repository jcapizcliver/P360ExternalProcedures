package mx.com.liverpool.p360.services.core.reconciliation;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.*;
import java.util.regex.*;
import org.json.*;

/** Chooses workflow state for a duplicate group. No I/O and no ordinary edit timestamps. */
public final class MergeStatusPolicy {
    public static final String VERSION = "workflow-progress-with-rework-20260917";
    public static final Set<String> STATE_FIELDS = Set.of("currentStatus", "previousStatus", "externalStatus");
    private static final Map<String, String> KEYS = new HashMap<>();
    // Explicit workflow stages, never the numerical value of the enumeration key.
    // Image load and Foro are parallel enrichment branches at the same stage.
    private static final Map<String, Integer> PROGRESS = Map.ofEntries(
        Map.entry("10031", 10), Map.entry("1001", 20), Map.entry("1003", 30),
        Map.entry("1020", 40), Map.entry("1002", 50), Map.entry("1004", 60),
        Map.entry("1010", 60), Map.entry("1026", 60), Map.entry("1021", 70),
        Map.entry("1022", 80), Map.entry("1023", 90), Map.entry("1007", 100));
    private static final Set<String> REWORK = Set.of("1005", "1006", "1008", "1024", "1027", "1028", "1029", "1030");
    private static final Set<String> INACTIVE = Set.of("1009", "1025");
    private static final Pattern EVENT = Pattern.compile(
        "(?:estado|status)\\s+\"([^\"]+)\"\\s+(?:el|on|at)\\s+" +
        "(\\d{1,2}/\\d{1,2}/\\d{4}\\s+\\d{1,2}:\\d{2}(?::\\d{2})?(?:\\s*[AP]M)?)",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    static {
        add("1001", "01", "Propuesta Generada", "Proposal Generated");
        add("1002", "02", "Pendiente Inicio Enriquecimiento", "Pending Enrichment");
        add("1003", "03", "Revisión Compras", "Purchase Revision");
        add("1004", "04", "Carga de Imagen", "Image Load");
        add("1005", "05", "Rechazada", "Rejected");
        add("1006", "06", "Por Actualizar", "To Be Updated");
        add("1007", "07", "Aprobada", "Approved");
        add("1008", "08", "Modificación", "Modified");
        add("1009", "09", "Cancelado", "Canceled", "Cancelled");
        add("1010", "10", "En Proceso Liverpool", "Liverpool in progress");
        add("1011", "11", "En Proceso de Envío", "Sending in progress");
        add("1020", "12", "Creación de SKU", "SKU Creation");
        add("1021", "14", "Gobierno de Datos", "Data Gobernance", "Data Governance");
        add("1022", "15", "Revisión QA", "QA Revision");
        add("1023", "16", "Category");
        add("1024", "17", "Rechazo Publicación", "Publish Rejected");
        add("1025", "18", "Eliminada", "Deleted");
        add("1026", "19", "En Proceso Foro", "In Foro Process");
        add("10031", "20", "Borrador", "Draft");
        add("1027", "21", "Rechazo Compras", "Purchase Rejected");
        add("1028", "22", "Rechazo QA", "QA Rejected");
        add("1029", "23", "Rechazo Gobierno", "Data Governance Rejected");
        add("1030", "24", "Rechazo Category", "Category Rejected");
        add("1031", "1031", "Repoblamiento", "Repopulation");
        add("1032", "1032", "Excepción de Catalogación", "Cataloguing Exception");
    }
    private MergeStatusPolicy() { }
    private static void add(String key, String code, String... labels) {
        KEYS.put(normalize(key), key); KEYS.put(normalize(code), key);
        for (String label : labels) KEYS.put(normalize(label), key);
    }
    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
            .trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
    public static String key(Object value) {
        if (value == null || value == JSONObject.NULL) return "";
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            // A supplied key is authoritative even when its label is inconsistent.
            if (object.has("_key")) return key(object.opt("_key"));
            if (object.has("_code")) return key(object.opt("_code"));
            return key(object.opt("_label"));
        }
        String text = value.toString().trim();
        return KEYS.getOrDefault(normalize(text), text);
    }
    private static LocalDateTime parseDate(String date, String line) {
        String text = date.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        boolean ampm = text.endsWith("AM") || text.endsWith("PM");
        if (ampm) text = text.replaceAll("\\s*([AP]M)$", " $1");
        // Spanish P360 history is d/M 24h; US history with AM/PM is M/d 12h.
        // English 24h dates with both day/month <=12 are not guessed.
        if (!ampm && !normalize(line).contains(" el ")) {
            int first = Integer.parseInt(text.substring(0, text.indexOf('/')));
            if (first <= 12) return null;
        }
        DateTimeFormatter format = DateTimeFormatter.ofPattern(
            ampm ? "M/d/uuuu h:mm[:ss] a" : "d/M/uuuu H:mm[:ss]", Locale.US)
            .withResolverStyle(ResolverStyle.STRICT);
        try { return LocalDateTime.parse(text, format); }
        catch (RuntimeException invalidDate) { return null; }
    }
    /** Only returns the time if the last dated status event agrees with CurrentStatus. */
    public static LocalDateTime statusTime(JSONObject data) {
        String history = data.optString("statusModification", "").replace("\\r\\n", "\n").replace("\\n", "\n");
        LocalDateTime latest = null; Set<String> lastKeys = new HashSet<>();
        for (String line : history.split("\\R")) {
            Matcher event = EVENT.matcher(line);
            if (!event.find()) continue;
            LocalDateTime at = parseDate(event.group(2), line);
            if (at == null) return null;
            if (latest == null || at.isAfter(latest)) { latest = at; lastKeys.clear(); }
            if (at.equals(latest)) lastKeys.add(key(event.group(1)));
        }
        return lastKeys.size() == 1 && lastKeys.contains(key(data.opt("currentStatus"))) ? latest : null;
    }
    private static final class Candidate {
        final JSONObject data; final String id, key; final LocalDateTime at;
        Candidate(JSONObject data, String id) { this.data=data; this.id=id; key=key(data.opt("currentStatus")); at=statusTime(data); }
        boolean rework() { return REWORK.contains(key); }
    }
    private static Candidate newest(List<Candidate> candidates) {
        Candidate result=candidates.get(0);
        for (Candidate c:candidates) if (c.at!=null && (result.at==null || c.at.isAfter(result.at))) result=c;
        return result;
    }
    private static Candidate furthest(List<Candidate> candidates) {
        Candidate result=candidates.get(0);
        for (Candidate c:candidates) {
            int rank=PROGRESS.getOrDefault(c.key,0), old=PROGRESS.getOrDefault(result.key,0);
            if (rank>old || rank==old && c.at!=null && (result.at==null || c.at.isAfter(result.at))) result=c;
        }
        return result;
    }
    /** Sources must be in stable base-first order. Audit is local to the caller's merge journal. */
    public static void apply(JSONObject target, List<JSONObject> sources, List<String> identifiers, JSONArray audit) {
        if (sources.size()!=identifiers.size()) throw new IllegalArgumentException("Source/ID count mismatch");
        List<Candidate> all=new ArrayList<>();
        for (int i=0;i<sources.size();i++) { Candidate c=new Candidate(sources.get(i),identifiers.get(i)); if(!c.key.isBlank()) all.add(c); }
        if(all.isEmpty()) return;
        List<Candidate> active=new ArrayList<>(); for(Candidate c:all) if(!INACTIVE.contains(c.key)) active.add(c);
        Candidate chosen; String reason; JSONArray warnings=new JSONArray();
        if(active.isEmpty()) { chosen=newest(all); reason="ALL_SOURCES_INACTIVE"; }
        else if(active.stream().anyMatch(c->!PROGRESS.containsKey(c.key)&&!c.rework())) {
            chosen=active.get(0); reason="KEEP_BASE_UNRANKED_STATE"; warnings.put("UNRANKED_STATE_NO_ASSUMED_PROGRESS");
        } else {
            List<Candidate> corrections=new ArrayList<>(); for(Candidate c:active) if(c.rework()) corrections.add(c);
            List<Candidate> undated=new ArrayList<>(); for(Candidate c:corrections) if(c.at==null) undated.add(c);
            if(!undated.isEmpty()) {
                chosen=undated.get(0); reason="KEEP_REWORK_WITHOUT_PROVEN_LATER_PROGRESS";
                warnings.put("STATUS_CHRONOLOGY_MISSING_NO_AUTOMATIC_APPROVAL");
            } else if(!corrections.isEmpty()) {
                Candidate reset=newest(corrections); List<Candidate> after=new ArrayList<>();
                // A correction begins a new cycle; older approvals cannot resurrect it.
                for(Candidate c:active) if(!c.rework()&&c.at!=null&&c.at.isAfter(reset.at)) after.add(c);
                if(after.isEmpty()) { chosen=reset; reason="LATER_MODIFICATION_OR_REJECTION"; }
                else { chosen=furthest(after); reason="FURTHEST_AFTER_LATEST_REWORK"; }
                if(active.stream().anyMatch(c->c.at==null)) warnings.put("UNDATED_PROGRESS_NOT_USED_TO_OVERRIDE_REWORK");
            } else { chosen=furthest(active); reason="FURTHEST_WORKFLOW_STAGE"; }
        }
        // Copy the enum as a unit; never mix another source's label/code into its key.
        for(String field:STATE_FIELDS) {
            target.remove(field);
            Object value=chosen.data.opt(field);
            if(value!=null&&value!=JSONObject.NULL) target.put(field, value instanceof JSONObject ? new JSONObject(value.toString()) : value);
        }
        JSONArray evidence=new JSONArray();
        for(Candidate c:all) evidence.put(new JSONObject().put("source",c.id).put("key",c.key)
            .put("statusChangedAt",c.at==null?JSONObject.NULL:c.at.toString()).put("stage",PROGRESS.getOrDefault(c.key,0)));
        audit.put(new JSONObject().put("path","/currentStatus").put("policy",VERSION).put("source",chosen.id)
            .put("key",chosen.key).put("reason",reason).put("candidates",evidence).put("warnings",warnings));
    }
}
