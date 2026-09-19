package mx.com.liverpool.p360.services.core.amqp.run;

import java.util.Collection;
import java.util.Set;
import org.w3c.dom.Node;

/** Legacy actions only: never filters durable history or completeness capture. */
final class CharacteristicEventWork {
    private static final Set<String> PRODUCT = Set.of(
        "ProcessingRepublish", "ProcessingPublishToMkt", "ProductoConfigurable",
        "ProcessingTomarNoTomar", "ProductName", "ResendToSKUCreation", "Section",
        "ItemGroup", "ItemGroupS4H", "BrandName", "BRAND_ID_S4H", "Business",
        "SupplierID", "SKU", "AssignTakeNoTake", "SAPObjectType",
        "FotoTomadaLiverpool", "MainBarCode", "MainBarCodeS4");
    private static final Set<String> ARTICLE = Set.of(
        "TamanoUnico", "SAPObjectType", "MensajeCreacionSKU", "ProductImage_URL",
        "ProductImage", "MainBarCode", "MainBarCodeS4H", "ColoursLiverpoolAtt",
        "AssignTakeNoTake", "SKU");

    static boolean needsWork(Iterable<Node> records, boolean product, Collection<String> resend) {
        if (records == null) return true; // Malformed input must reach the existing error path.
        for (Node record : records) {
            String code = code(record);
            if (code == null || (product ? PRODUCT : ARTICLE).contains(code)
                    || (!product && resend.contains(code))) return true;
        }
        return false;
    }

    static boolean containsAny(Iterable<Node> records, Collection<String> codes) {
        if (records == null) return false;
        for (Node record : records) if (codes.contains(code(record))) return true;
        return false;
    }

    private static String code(Node record) {
        Node n = child(child(child(record, "_qualification"), "characteristic"), "_code");
        return n == null ? null : n.getTextContent();
    }

    private static Node child(Node parent, String name) {
        if (parent != null) for (Node n=parent.getFirstChild(); n!=null; n=n.getNextSibling())
            if (name.equals(n.getNodeName())) return n;
        return null;
    }
}
