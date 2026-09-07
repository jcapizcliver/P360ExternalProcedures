package mx.com.liverpool.p360.services.core.completeness;

import java.sql.SQLException;

/**
 * Frontera estable del pequeño motor semántico.
 *
 * La implementación V1 calcula Mandatory Completeness. Más adelante esta misma
 * frontera puede orquestar Vendor Center y ECommerce sin cambiar los callers.
 */
public interface ProductCompletenessService {
    CompletenessResult calculate(String productIdentifier) throws SQLException;
}
