package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.util.logging.Level;
import java.util.logging.Logger;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;

/**
 * Resolves a SKU to the P360 product identifier stored in
 * ArticleRevision.Identifier.
 */
public class SkuProductNoPlaceholderIdResolver implements PlaceholderIdResolver {

    private final Logger logger;

    public SkuProductNoPlaceholderIdResolver(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String resolve(String sku) throws Exception {
        try (DBAccessDataStub db = new DBAccessDataStub(new DBAccessDataStub.ELog() {
            @Override
            public void logE(Exception e) {
                logger.log(Level.WARNING, "Error resolving placeholder identifier for SKU: " + sku, e);
            }

            @Override
            public void log(String message) {
                logger.info(message);
            }
        })) {
            return db.getSkuProductNo(sku);
        }
    }
}
