package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Example dictionary that maps file status values to P360 CurrentStatus values.
 */
public class PlaceholderStatusDictionary {

    private final Map<String, String> values = new HashMap<>();
    private final Map<String, String> actions = new HashMap<>();

    public PlaceholderStatusDictionary() {
        add("aprobado", "Aceptado", "A");
        add("aprobado con cambios", "Aceptado con ajustados", "J");
        add("rechazado", "Cancelado", "C");
        add("rechazado para modificación", "Rechazada", "R");
    }

    public String resolve(String fileStatus) {
        return values.get(normalize(fileStatus));
    }

    public String resolveAction(String fileStatus) {
        return actions.get(normalize(fileStatus));
    }

    private void add(String fileStatus, String p360Status, String action) {
        String key = normalize(fileStatus);
        values.put(key, p360Status);
        actions.put(key, action);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
