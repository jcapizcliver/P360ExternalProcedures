package mx.com.liverpool.p360.services.core.temp.xml.local.anotheropinion;

/** Lightweight status calculator extracted from PruebaEnvioPubSubMediaAssets. */
public final class StepStatusComputer {
    private final java.util.regex.Pattern flowPattern =
            java.util.regex.Pattern.compile("(?<=Flujo Actual: )([^|]+)");
    private final java.util.regex.Pattern statePattern =
            java.util.regex.Pattern.compile("(?<=Estado en el WF: )([^|]+)");

    public String getCurrentFlow(String calculatedWFAtt) {
        if (calculatedWFAtt != null) {
            java.util.regex.Matcher m = flowPattern.matcher(calculatedWFAtt);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    public String getWorkflowState(String calculatedWFAtt) {
        if (calculatedWFAtt != null) {
            java.util.regex.Matcher m = statePattern.matcher(calculatedWFAtt);
            if (m.find()) return m.group(1);
        }
        return null;
    }

    public String[] computeStatus(
            String calculatedWFAtt,
            String stateSKU,
            String fotoTomadaLiverpool,
            String productId) {
        String[] res = new String[] { null, null, "false" };
        String flujoActual = getCurrentFlow(calculatedWFAtt);
        String estado = getWorkflowState(calculatedWFAtt);
        if ("N/A".equals(flujoActual) && "N/A".equals(estado) && !"Aprobado".equals(stateSKU) && "Y".equals(fotoTomadaLiverpool)) {
            res[0] = "1002"; res[1] = "1020";
        } else if ("N/A".equals(flujoActual) && "N/A".equals(estado) && !"Aprobado".equals(stateSKU) && "N".equals(fotoTomadaLiverpool)) {
            res[0] = "1004"; res[1] = "1020";
        } else if ("N/A".equals(flujoActual) && "N/A".equals(estado) && "Aprobado".equals(stateSKU)) {
            res[0] = "1007"; res[1] = "1023";
        } else if ("ItemMaintenanceWorkFlow".equals(flujoActual) && "BuyerReview".equals(estado)) {
            res[0] = "1003"; res[1] = "10031";
        } else if ("ItemMaintenanceWorkFlow".equals(flujoActual) && "SupplierReviewChange".equals(estado)) {
            res[0] = "1004"; res[1] = "1020";
        } else if ("ItemMaintenanceWorkFlow".equals(flujoActual) && "SupplierModification".equals(estado) && "Aprobado".equals(stateSKU)) {
            res[0] = "1007"; res[1] = "1023";
        } else if ("ItemMaintenanceWorkFlow".equals(flujoActual) && "SupplierModification".equals(estado) && !"Aprobado".equals(stateSKU)) {
            res[0] = "1004"; res[1] = "1020";
        } else if ("ItemMaintenanceWorkFlow".equals(flujoActual) && ("DataGovermentInitiate".equals(estado) || "ErrorRevision".equals(estado) || "DigitalAssetsReview".equals(estado))) {
            res[0] = "1021"; res[1] = "1020";
        } else if ("ItemMaintenanceWorkFlow".equals(flujoActual) && "QAReview".equals(estado)) {
            res[0] = "1022"; res[1] = "1020";
        } else if ("ItemMaintenanceWorkFlow".equals(flujoActual) && "CategoryManager".equals(estado)) {
            res[0] = "1023"; res[1] = "1022";
        } else if ("SalesItemCreationRevised".equals(flujoActual) && "Categorizacion".equals(estado)) {
            res[0] = "1021"; res[1] = "1026"; res[2] = "true";
        } else if ("SalesItemCreationRevised".equals(flujoActual) && "Aseguramiento_de_Calidad".equals(estado)) {
            res[0] = "1022"; res[1] = "1026"; res[2] = "true";
        } else if ("SalesItemCreationRevised".equals(flujoActual) && "Category_Manager".equals(estado)) {
            res[0] = "1023"; res[1] = "1022"; res[2] = "true";
        } else if ("SalesItemCreationRevised".equals(flujoActual) && "Rechazos".equals(estado)) {
            res[0] = "1005"; res[1] = "1022"; res[2] = "true";
        } else if ("SalesItemCreationRevised".equals(flujoActual) && "Revision_Categorizacion".equals(estado)) {
            res[0] = "1026"; res[1] = "1002";
        } else if ("SupplierCreationWF".equals(flujoActual) && "BuyerReview".equals(estado)) {
            res[0] = "1003"; res[1] = "1001";
        } else if ("SupplierCreationWF".equals(flujoActual) && "AssetReviewAndUpload".equals(estado)) {
            res[0] = "1004"; res[1] = "1020";
        } else if ("SupplierCreationWF".equals(flujoActual) && "SupplierRevision".equals(estado)) {
            res[0] = "1004"; res[1] = "1020";
        } else if ("SupplierCreationWF".equals(flujoActual) && ("DigitalAssetsReview".equals(estado) || "ErrorRevision".equals(estado))) {
            res[0] = "1021"; res[1] = "1020";
        } else if ("SupplierCreationWF".equals(flujoActual) && "QAReview".equals(estado)) {
            res[0] = "1022"; res[1] = "1020";
        } else if ("SupplierCreationWF".equals(flujoActual) && "CategoryManager".equals(estado)) {
            res[0] = "1023"; res[1] = "1022";
        }
        return res;
    }
}
