package mx.com.liverpool.p360.services.core.temp.product2g.maintenance4;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class PrettyTextUtil {

    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");

    private static final Pattern DOUBLE_QUOTES =
            Pattern.compile("[\"“”]");

    private static final Pattern NO_APLICA =
            Pattern.compile("(?iu)(?<!\\p{L})no\\s+aplica(?!\\p{L})");

    private static final Pattern SI_APLICA =
            Pattern.compile("(?iu)(?<!\\p{L})s[ií]\\s+aplica(?!\\p{L})");

    private static final Pattern EMPTY_PARENTHESES =
            Pattern.compile("\\(\\s*\\)");

    private static final Pattern SPACES =
            Pattern.compile("\\s+");

    private static final Pattern SPACE_BEFORE_PUNCTUATION =
            Pattern.compile("\\s+([,.;:])");

    private static final Map<String, String> CANONICAL_PHRASES = new LinkedHashMap<>();

    static {
        // Más específicas primero
        CANONICAL_PHRASES.put("lauren raph lauren", "Lauren Ralph Lauren");
        CANONICAL_PHRASES.put("lauren ralph lauren", "Lauren Ralph Lauren");
        CANONICAL_PHRASES.put("ralph lauren", "Ralph Lauren");

        CANONICAL_PHRASES.put("anne klein", "Anne Klein");
        CANONICAL_PHRASES.put("carter's", "Carter's");
        CANONICAL_PHRASES.put("coach", "Coach");
        CANONICAL_PHRASES.put("life 180", "Life 180");

        // Opcionales, si quieres embellecer líneas/modelos
        CANONICAL_PHRASES.put("palm bay", "Palm Bay");
        CANONICAL_PHRASES.put("tampa", "Tampa");
        CANONICAL_PHRASES.put("pensacola", "Pensacola");
        CANONICAL_PHRASES.put("st. augustine", "St. Augustine");
        CANONICAL_PHRASES.put("coral gable", "Coral Gable");
        CANONICAL_PHRASES.put("juliet", "Juliet");
        CANONICAL_PHRASES.put("nike", "Nike");
        CANONICAL_PHRASES.put("tommy hilfiger", "Tommy Hilfiger");
        CANONICAL_PHRASES.put("brownie", "Brownie");
        CANONICAL_PHRASES.put("stella nova", "Stella Nova");
        CANONICAL_PHRASES.put("banana republic", "Banana Republic");
        CANONICAL_PHRASES.put("new balance", "New Balance");
        CANONICAL_PHRASES.put("calvin klein", "Calvin Klein");
        CANONICAL_PHRASES.put("pottery barn", "Pottery Barn");
        CANONICAL_PHRASES.put("smart tv", "Smart TV");
        CANONICAL_PHRASES.put("tcl", "TCL");
        CANONICAL_PHRASES.put("qled", "QLED");
        CANONICAL_PHRASES.put("4k", "4K");
        CANONICAL_PHRASES.put("uhd", "UHD");
        CANONICAL_PHRASES.put("g-shock", "G-Shock");
    }

    private PrettyTextUtil() {
    }

    public static String toPrettyDescription(String value) {
        if (value == null) {
            return "";
        }

        String text = value.trim();

        if (text.isEmpty()) {
            return "";
        }

        text = DOUBLE_QUOTES.matcher(text).replaceAll(" ");

        text = NO_APLICA.matcher(text).replaceAll(" ");
        text = SI_APLICA.matcher(text).replaceAll(" ");

        text = EMPTY_PARENTHESES.matcher(text).replaceAll(" ");

        // Opcional: si también quieres quitar atributos vacíos típicos.
        // Descomenta si para tu negocio estos textos son ruido.
        /*
        text = removeWholePhrase(text, "sin gema");
        text = removeWholePhrase(text, "sin kilataje");
        text = removeWholePhrase(text, "sin accesorios adicionales");
        */

        text = normalizeSpacesAndPunctuation(text);

        text = text.toLowerCase(ES_MX);
        text = capitalizeFirstLetter(text);

        text = applyCanonicalPhrases(text);

        text = normalizeSpacesAndPunctuation(text);

        return text;
    }

    private static String removeWholePhrase(String text, String phrase) {
        String regex = "(?iu)(?<![\\p{L}\\p{N}])" + Pattern.quote(phrase) + "(?![\\p{L}\\p{N}])";
        return Pattern.compile(regex).matcher(text).replaceAll(" ");
    }

    private static String normalizeSpacesAndPunctuation(String text) {
        text = SPACE_BEFORE_PUNCTUATION.matcher(text).replaceAll("$1");
        text = SPACES.matcher(text).replaceAll(" ");
        return text.trim();
    }

    private static String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        int offset = 0;

        while (offset < text.length()) {
            int codePoint = text.codePointAt(offset);

            if (Character.isLetter(codePoint)) {
                String before = text.substring(0, offset);
                String first = new String(Character.toChars(Character.toTitleCase(codePoint)));
                String after = text.substring(offset + Character.charCount(codePoint));

                return before + first + after;
            }

            offset += Character.charCount(codePoint);
        }

        return text;
    }

    private static String applyCanonicalPhrases(String text) {
        String result = text;

        for (Map.Entry<String, String> entry : CANONICAL_PHRASES.entrySet()) {
            String source = entry.getKey();
            String target = entry.getValue();

            String regex = "(?iu)(?<![\\p{L}\\p{N}])" + Pattern.quote(source) + "(?![\\p{L}\\p{N}])";

            result = Pattern.compile(regex).matcher(result).replaceAll(target);
        }

        return result;
    }

    public static void main(String[] args) {
        String[] samples = {
                "Pulsera lauren ralph lauren \"de\" latón \"con\" sin gema sin kilataje pulido \"acabado\"",
                "Corto \"de (si aplica)\" circular no aplica anne klein palm bay \"de\" aleación metálica sin kilataje \"con acabado\" pavé sin gema",
                "\"set/bolsa\" shoulder coach pocket juliet 30 no aplica \"de\" piel \"para\" (si aplica) mujer",
                "Cartera coach slim accordion zip no aplica \"para\" (si aplica) mujer"
        };

        for (String sample : samples) {
            System.out.println(toPrettyDescription(sample));
        }
    }
}