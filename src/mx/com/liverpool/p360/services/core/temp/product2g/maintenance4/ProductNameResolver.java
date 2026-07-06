package mx.com.liverpool.p360.services.core.temp.product2g.maintenance4;

public final class ProductNameResolver {

    public record ResolvedName(String value, String source) {
    }

    public static ResolvedName resolve(String productName, String nameCharacteristic) {
        boolean productNameBlank = isBlank(productName);
        boolean nameCharacteristicBlank = isBlank(nameCharacteristic);

        String prettyProductName = PrettyTextUtil.toPrettyDescription(productName);
        String prettyNameCharacteristic = PrettyTextUtil.toPrettyDescription(nameCharacteristic);

        if (productNameBlank && nameCharacteristicBlank) {
            return new ResolvedName("", "BOTH_EMPTY");
        }

        if (productNameBlank) {
            return new ResolvedName(prettyNameCharacteristic, "NAME_CHARACTERISTIC_USED_PRODUCT_NAME_EMPTY");
        }

        if (nameCharacteristicBlank) {
            return new ResolvedName(prettyProductName, "PRODUCT_NAME_USED_NAME_CHARACTERISTIC_EMPTY");
        }

        int productNameScore = score(productName, prettyProductName);
        int nameCharacteristicScore = score(nameCharacteristic, prettyNameCharacteristic);

        boolean productNameDirty = hasTemplateGarbage(productName);

        if (productNameDirty && nameCharacteristicScore >= productNameScore + 4) {
            return new ResolvedName(prettyNameCharacteristic, "NAME_CHARACTERISTIC_USED_PRODUCT_NAME_TOO_DIRTY");
        }

        return new ResolvedName(prettyProductName, productNameDirty ? "PRODUCT_NAME_CLEANED" : "PRODUCT_NAME_USED");
    }

    private static int score(String raw, String pretty) {
        if (isBlank(raw) || isBlank(pretty)) {
            return -100;
        }

        int score = 20;

        if (containsDoubleQuotes(raw)) {
            score -= 4;
        }

        if (containsNoAplica(raw)) {
            score -= 4;
        }

        if (containsSiAplica(raw)) {
            score -= 4;
        }

        if (pretty.length() < 8) {
            score -= 6;
        }

        int wordCount = pretty.trim().split("\\s+").length;

        if (wordCount < 2) {
            score -= 6;
        }

        if (wordCount >= 3 && wordCount <= 14) {
            score += 3;
        }

        if (pretty.contains(",")) {
            score -= 1;
        }

        return score;
    }

    private static boolean hasTemplateGarbage(String value) {
        return containsDoubleQuotes(value)
                || containsNoAplica(value)
                || containsSiAplica(value);
    }

    private static boolean containsDoubleQuotes(String value) {
        return value != null && (value.contains("\"") || value.contains("“") || value.contains("”"));
    }

    private static boolean containsNoAplica(String value) {
        return value != null && value.matches("(?iu).*\\bno\\s+aplica\\b.*");
    }

    private static boolean containsSiAplica(String value) {
        return value != null && value.matches("(?iu).*\\bs[ií]\\s+aplica\\b.*");
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}