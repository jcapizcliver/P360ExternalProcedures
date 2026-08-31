package mx.com.liverpool.p360.services.core.gcp.placeholder;

import java.util.Map;
import java.util.logging.Logger;

import mx.com.liverpool.p360.services.core.DBAccessDataStub;
import mx.com.liverpool.p360.services.core.ELog;

/**
 * Resolves Product2G workflow states using the same P360 dictionaries consumed
 * by CreateProposal.
 */
public class ProposalStatusFlow implements AutoCloseable {

    private static final String TARGET_ROLE = "Proveedor";
    private static final String DRAFT_STATUS = "10031";
    private static final String CANCELLED_STATUS = "1009";
    private static final String DRAFT_EXTERNAL_STATUS = "Borrador";

    private final DBAccessDataStub dataStub;
    private final Map<String, String> nextStatusMap;
    private final Map<String, String> externalStatusMap;

    public ProposalStatusFlow(final Logger logger) {
        this.dataStub = new DBAccessDataStub(new ELog() {
            @Override
            public void logE(Exception e) {
                logger.warning(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
            }

            @Override
            public void log(String message) {
                logger.info(message);
            }
        });
        this.nextStatusMap = dataStub.getDictionaryValueAlternativeValueMap("NextStatus");
        this.externalStatusMap = dataStub.getDictionaryValueAlternativeValueMap("ExternalStatus");
    }

    public Transition resolve(String previousStatus, String currentStatus, String action) {
        String previous = normalizeInitialStatus(previousStatus);
        String current = normalizeInitialStatus(currentStatus);
        String normalizedAction = action == null ? "" : action.trim().toUpperCase(java.util.Locale.ROOT);
        if (normalizedAction.length() == 0) {
            throw new IllegalArgumentException("Placeholder status action is required");
        }

        String next;
        if ("C".equals(normalizedAction)) {
            next = CANCELLED_STATUS;
        } else {
            String key = previous + "|" + current + "|" + normalizedAction + "|" + TARGET_ROLE;
            next = nextStatusMap.get(key);
            if (next == null || next.trim().length() == 0) {
                String fallbackKey = "|" + current + "|" + normalizedAction + "|" + TARGET_ROLE;
                next = nextStatusMap.get(fallbackKey);
            }
            if (next == null || next.trim().length() == 0) {
                throw new IllegalStateException("No NextStatus transition found for previousStatus=" + previous
                        + ", currentStatus=" + current + ", action=" + normalizedAction
                        + ", targetRole=" + TARGET_ROLE);
            }
        }

        String oldCurrent = current.length() == 0 ? DRAFT_STATUS : current;
        String external = externalStatusMap.get(next);
        if (external == null || external.trim().length() == 0) {
            external = DRAFT_EXTERNAL_STATUS;
        }
        return new Transition(oldCurrent, next, external);
    }

    private static String normalizeInitialStatus(String status) {
        String normalized = status == null ? "" : status.trim();
        return DRAFT_STATUS.equals(normalized) ? "" : normalized;
    }

    @Override
    public void close() {
        dataStub.close();
    }

    public static class Transition {
        private final String previousStatus;
        private final String currentStatus;
        private final String externalStatus;

        private Transition(String previousStatus, String currentStatus, String externalStatus) {
            this.previousStatus = previousStatus;
            this.currentStatus = currentStatus;
            this.externalStatus = externalStatus;
        }

        public String getPreviousStatus() {
            return previousStatus;
        }

        public String getCurrentStatus() {
            return currentStatus;
        }

        public String getExternalStatus() {
            return externalStatus;
        }
    }
}
