package mx.com.liverpool.p360.services.core.completeness;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resultado actual de completitud de un Product2G.
 *
 * La clase ya deja sitio conceptual para Vendor Center / ECommerce, pero en V1
 * sólo se llena Mandatory.
 */
public final class CompletenessResult {

    public static final class MetricResult {
        private final int total;
        private final int present;
        private final int missing;
        private final BigDecimal percentage;
        private final boolean scorable;
        private final String status;
        private final String missingDetail;

        public MetricResult(
                int total,
                int present,
                int missing,
                BigDecimal percentage,
                boolean scorable,
                String status,
                String missingDetail) {
            this.total = total;
            this.present = present;
            this.missing = missing;
            this.percentage = percentage;
            this.scorable = scorable;
            this.status = status;
            this.missingDetail = missingDetail;
        }

        public int getTotal() { return total; }
        public int getPresent() { return present; }
        public int getMissing() { return missing; }
        public BigDecimal getPercentage() { return percentage; }
        public boolean isScorable() { return scorable; }
        public String getStatus() { return status; }
        public String getMissingDetail() { return missingDetail; }
    }

    private final String productIdentifier;
    private final Long articleRevisionId;
    private final String template;
    private final String businessCode;
    private final String businessName;
    private final MetricResult mandatory;
    private final Instant calculatedAt;
    private final String engineVersion;

    public CompletenessResult(
            String productIdentifier,
            Long articleRevisionId,
            String template,
            String businessCode,
            String businessName,
            MetricResult mandatory,
            Instant calculatedAt,
            String engineVersion) {
        this.productIdentifier = productIdentifier;
        this.articleRevisionId = articleRevisionId;
        this.template = template;
        this.businessCode = businessCode;
        this.businessName = businessName;
        this.mandatory = mandatory;
        this.calculatedAt = calculatedAt;
        this.engineVersion = engineVersion;
    }

    public String getProductIdentifier() { return productIdentifier; }
    public Long getArticleRevisionId() { return articleRevisionId; }
    public String getTemplate() { return template; }
    public String getBusinessCode() { return businessCode; }
    public String getBusinessName() { return businessName; }
    public MetricResult getMandatory() { return mandatory; }
    public Instant getCalculatedAt() { return calculatedAt; }
    public String getEngineVersion() { return engineVersion; }

    @Override
    public String toString() {
        return "CompletenessResult{" +
                "productIdentifier='" + productIdentifier + '\'' +
                ", template='" + template + '\'' +
                ", businessCode='" + businessCode + '\'' +
                ", mandatory=" +
                (mandatory == null ? "null" :
                        mandatory.getPresent() + "/" + mandatory.getTotal() +
                        "=" + mandatory.getPercentage() + "%" +
                        " status=" + mandatory.getStatus()) +
                '}';
    }
}
