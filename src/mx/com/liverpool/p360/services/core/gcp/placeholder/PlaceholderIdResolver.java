package mx.com.liverpool.p360.services.core.gcp.placeholder;

/**
 * Resolves the P360 placeholder identifier used by the update service.
 */
public interface PlaceholderIdResolver {
    String resolve(String sku) throws Exception;
}
