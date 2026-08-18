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

    public PlaceholderStatusDictionary() {
        values.put(normalize("aprobado"), "Aceptado");
        values.put(normalize("aprobado con cambios"), "Aceptado con ajustados");
        values.put(normalize("rechazado"), "Cancelado");
        values.put(normalize("rechazado para modificación"), "Rechazada");
    }

    public String resolve(String fileStatus) {
        return values.get(normalize(fileStatus));
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
