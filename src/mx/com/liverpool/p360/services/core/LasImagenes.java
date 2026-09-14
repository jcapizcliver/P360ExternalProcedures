package mx.com.liverpool.p360.services.core;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Recepción de imágenes NO finales (características sin sufijo 2).
 *
 * Robustecimiento principal:
 *  - NO se borra nada de una variante hasta haber validado completamente su payload.
 *  - photos=[] se considera una intención explícita y válida de dejar la variante sin imágenes.
 *  - ausencia de la propiedad photos NO se interpreta como intención de borrar.
 *  - cuando replaceAssets=true se toma snapshot de los media records actuales antes del DELETE.
 *  - si el PUT/validación posterior falla, se intenta rollback desde el snapshot.
 *  - cada request/producto/variante deja una traza estructurada en receive_media-*.log.
 */
public class LasImagenes {

    /*
     * IMPORTANTE: no debe ser static. El servlet crea una instancia de LasImagenes
     * por request; así cada request conserva su propio RESTWrapper/rawResponse y no
     * comparte estado mutable con otra invocación concurrente.
     */
    private final RESTWrapper rw = new RESTWrapper();

    private static final Set<String> NON_FINAL_ROOTS = new LinkedHashSet<String>();

    static {
        NON_FINAL_ROOTS.add("ProductImage");
        NON_FINAL_ROOTS.add("ProductImageDetail");
        NON_FINAL_ROOTS.add("Illustration");
        NON_FINAL_ROOTS.add("ProductImageSmosh");
    }

    public String doIt(String arg) {
        final String requestId = UUID.randomUUID().toString();

        if (!ImageTrafficLimiter.tryAcquire()) {
            audit(requestId, "REQUEST_REJECTED",
                    "reason=IMAGE_TRAFFIC_LIMIT inFlight=" + ImageTrafficLimiter.getInFlight());
            return ImageTrafficLimiter.busyResponse();
        }

        final long requestStarted = System.currentTimeMillis();
        try {
            return processRequest(arg, requestId, requestStarted);
        } catch (Exception e) {
            auditError(requestId, "REQUEST_FATAL", e);
            return new JSONObject()
                    .put("requestId", requestId)
                    .put("error", "Error al procesar petición de imágenes.")
                    .put("detail", safeLogValue(e.getMessage()))
                    .toString();
        } finally {
            audit(requestId, "REQUEST_FINALLY",
                    "elapsedMs=" + (System.currentTimeMillis() - requestStarted));
            ImageTrafficLimiter.release();
        }
    }

    private String processRequest(String input, String requestId, long requestStarted) throws Exception {
        if (input == null) {
            input = "";
        }

        JSONObject request = new JSONObject(input);
        JSONArray products = request.optJSONArray("products");
        if (products == null) {
            products = new JSONArray();
        }

        boolean replaceAssets = request.has("replaceAssets") && request.getBoolean("replaceAssets");

        audit(requestId, "REQUEST_START",
                "replaceAssets=" + replaceAssets
                + " products=" + products.length()
                + " payloadChars=" + input.length()
                + " payloadSha256=" + sha256(input));

        JSONArray responses = new JSONArray();
        List<ProductPlan> productPlans = new ArrayList<ProductPlan>();
        List<VariantPlan> allVariantPlans = new ArrayList<VariantPlan>();

        /* Fase 1: construir y validar TODO antes de cualquier borrado. */
        for (int i = 0; i < products.length(); i++) {
            JSONObject product = products.optJSONObject(i);
            ProductPlan productPlan = buildProductPlan(product, i, replaceAssets, requestId);
            productPlans.add(productPlan);
            allVariantPlans.addAll(productPlan.variants);
        }

        EliminaImagenesDeVariantes eliminator = new EliminaImagenesDeVariantes();

        /*
         * Fase 2: para replaceAssets=true, tomar snapshot sólo de variantes cuyo
         * payload quedó completamente válido. Si no hay snapshot, NO se borra.
         */
        LinkedHashSet<String> variantIdsToDelete = new LinkedHashSet<String>();
        if (replaceAssets) {
            for (VariantPlan plan : allVariantPlans) {
                if (!plan.shouldProcess()) {
                    continue;
                }

                try {
                    plan.snapshotRecords = loadCurrentNonFinalRecords(plan.variantId);
                    plan.snapshotSignatures = mediaSignatures(plan.snapshotRecords);
                    plan.snapshotReady = true;

                    audit(requestId, "SNAPSHOT_OK",
                            ctx(plan)
                            + " existingRecords=" + plan.snapshotRecords.length()
                            + " existingSignatures=" + formatSignatures(plan.snapshotSignatures));

                    variantIdsToDelete.add(plan.variantId);
                } catch (Exception e) {
                    plan.snapshotReady = false;
                    plan.errors.add("No se pudo obtener snapshot previo; no se borró la variante.");
                    auditError(requestId, "SNAPSHOT_FAILED " + ctx(plan), e);
                }
            }

            if (!variantIdsToDelete.isEmpty()) {
                audit(requestId, "DELETE_NON_FINAL_BATCH_START",
                        "variants=" + variantIdsToDelete.size()
                        + " ids=" + formatIds(variantIdsToDelete));
                try {
                    eliminator.deleteAssetsBatched(variantIdsToDelete);
                    audit(requestId, "DELETE_NON_FINAL_BATCH_DISPATCHED",
                            "variants=" + variantIdsToDelete.size()
                            + " ids=" + formatIds(variantIdsToDelete));
                } catch (Exception e) {
                    /*
                     * El DELETE puede haber quedado parcial. No asumimos que nada
                     * cambió: cada variante será verificada y, si hace falta, se
                     * intentará restaurar desde su snapshot.
                     */
                    auditError(requestId, "DELETE_NON_FINAL_BATCH_EXCEPTION variants="
                            + variantIdsToDelete.size(), e);
                }
            }
        }

        /* Fase 3: aplicar por variante y verificar/rollback cuando reemplazamos. */
        int successfulVariants = 0;
        int failedVariants = 0;
        int skippedVariants = 0;
        int rolledBackVariants = 0;

        for (ProductPlan productPlan : productPlans) {
            JSONObject productResponse = new JSONObject();
            productResponse.put("proposalId", productPlan.proposalId);
            JSONArray variantResponses = new JSONArray();
            productResponse.put("variantResponses", variantResponses);
            responses.put(productResponse);

            long productStarted = System.currentTimeMillis();
            audit(requestId, "PRODUCT_APPLY_START",
                    "proposalId=" + safeLogValue(productPlan.proposalId)
                    + " variants=" + productPlan.variants.size()
                    + " replaceAssets=" + replaceAssets);

            for (VariantPlan plan : productPlan.variants) {
                VariantExecution execution;

                if (!plan.photosPresent) {
                    execution = VariantExecution.skipped("NO_PHOTOS_FIELD",
                            "La propiedad photos no llegó; no se interpretó como intención de borrar.");
                    skippedVariants++;
                    audit(requestId, "VARIANT_SKIPPED",
                            ctx(plan) + " reason=NO_PHOTOS_FIELD");
                } else if (!plan.valid) {
                    execution = VariantExecution.failed("INVALID_PAYLOAD",
                            join(plan.errors));
                    failedVariants++;
                    audit(requestId, "VARIANT_REJECTED",
                            ctx(plan) + " errors=" + safeLogValue(join(plan.errors)));
                } else if (replaceAssets && !plan.snapshotReady) {
                    execution = VariantExecution.failed("SNAPSHOT_FAILED",
                            join(plan.errors));
                    failedVariants++;
                    audit(requestId, "VARIANT_NOT_DELETED",
                            ctx(plan) + " reason=SNAPSHOT_NOT_AVAILABLE");
                } else if (replaceAssets) {
                    execution = executeReplace(plan, requestId, eliminator);
                    if (execution.success) {
                        successfulVariants++;
                    } else {
                        failedVariants++;
                        if (execution.rollbackAttempted) {
                            rolledBackVariants++;
                        }
                    }
                } else {
                    execution = executeMerge(plan, requestId);
                    if (execution.success) {
                        successfulVariants++;
                    } else if (execution.skipped) {
                        skippedVariants++;
                    } else {
                        failedVariants++;
                    }
                }

                variantResponses.put(buildVariantResponse(plan, execution));
            }

            audit(requestId, "PRODUCT_APPLY_END",
                    "proposalId=" + safeLogValue(productPlan.proposalId)
                    + " variants=" + productPlan.variants.size()
                    + " elapsedMs=" + (System.currentTimeMillis() - productStarted));
        }

        long elapsed = System.currentTimeMillis() - requestStarted;
        audit(requestId, "REQUEST_END",
                "replaceAssets=" + replaceAssets
                + " successfulVariants=" + successfulVariants
                + " failedVariants=" + failedVariants
                + " skippedVariants=" + skippedVariants
                + " rollbackAttempts=" + rolledBackVariants
                + " elapsedMs=" + elapsed);

        return new JSONObject()
                .put("requestId", requestId)
                .put("replaceAssets", replaceAssets)
                .put("successfulVariants", successfulVariants)
                .put("failedVariants", failedVariants)
                .put("skippedVariants", skippedVariants)
                .put("rollbackAttempts", rolledBackVariants)
                .put("responses", responses)
                .toString();
    }

    private ProductPlan buildProductPlan(JSONObject product,
                                         int productIndex,
                                         boolean replaceAssets,
                                         String requestId) {
        ProductPlan result = new ProductPlan();

        if (product == null) {
            result.proposalId = "";
            audit(requestId, "PRODUCT_INVALID",
                    "productIndex=" + productIndex + " reason=NOT_AN_OBJECT");
            return result;
        }

        result.proposalId = product.optString("proposalId", "");
        JSONArray variants = product.optJSONArray("variants");
        if (variants == null) {
            variants = new JSONArray();
        }

        audit(requestId, "PRODUCT_PLAN",
                "productIndex=" + productIndex
                + " proposalId=" + safeLogValue(result.proposalId)
                + " variants=" + variants.length()
                + " replaceAssets=" + replaceAssets);

        for (int j = 0; j < variants.length(); j++) {
            JSONObject variant = variants.optJSONObject(j);
            VariantPlan plan = buildVariantPlan(result.proposalId, variant, j, requestId);
            result.variants.add(plan);
        }

        return result;
    }

    private VariantPlan buildVariantPlan(String proposalId,
                                         JSONObject variant,
                                         int variantIndex,
                                         String requestId) {
        VariantPlan plan = new VariantPlan();
        plan.proposalId = proposalId;
        plan.variantIndex = variantIndex;

        if (variant == null) {
            plan.valid = false;
            plan.errors.add("La variante no es un objeto JSON.");
            audit(requestId, "VARIANT_PLAN_INVALID",
                    "proposalId=" + safeLogValue(proposalId)
                    + " variantIndex=" + variantIndex
                    + " reason=NOT_AN_OBJECT");
            return plan;
        }

        plan.variantId = variant.optString("variantId", "");
        if (isBlank(plan.variantId)) {
            plan.valid = false;
            plan.errors.add("Falta variantId.");
        }

        plan.photosPresent = variant.has("photos") && !variant.isNull("photos");
        if (!plan.photosPresent) {
            /* Ausente != []: no hay intención explícita de borrar. */
            plan.valid = true;
            plan.photoCount = -1;
            audit(requestId, "VARIANT_PLAN",
                    ctx(plan)
                    + " photosPresent=false action=NO_ACTION");
            return plan;
        }

        Object photosObject = variant.opt("photos");
        if (!(photosObject instanceof JSONArray)) {
            plan.valid = false;
            plan.errors.add("photos existe pero no es un arreglo JSON.");
            audit(requestId, "VARIANT_PLAN_INVALID",
                    ctx(plan) + " reason=PHOTOS_NOT_ARRAY");
            return plan;
        }

        JSONArray photos = (JSONArray) photosObject;
        plan.photoCount = photos.length();
        plan.clearIntent = photos.length() == 0;

        if (plan.clearIntent) {
            plan.valid = !isBlank(plan.variantId);
            audit(requestId, "VARIANT_PLAN",
                    ctx(plan)
                    + " photosPresent=true photoCount=0 clearIntent=true"
                    + " validationOk=" + plan.valid);
            return plan;
        }

        int detailIndex = 0;
        int illustrationIndex = 0;
        int smoshIndex = 0;
        int productImageCount = 0;

        for (int k = 0; k < photos.length(); k++) {
            JSONObject photo = photos.optJSONObject(k);
            if (photo == null) {
                plan.valid = false;
                plan.errors.add("photos[" + k + "] no es un objeto JSON.");
                audit(requestId, "PHOTO_INVALID",
                        ctx(plan) + " photoIndex=" + k + " reason=NOT_AN_OBJECT");
                continue;
            }

            String type = photo.optString("PhotoAssetType", "");
            String name = photo.optString("PhotoAssetName", "");
            String url = photo.optString("PhotoAssetURL", "");
            String status = photo.optString("PhotoAssetStatus", "");

            audit(requestId, "PHOTO_INPUT",
                    ctx(plan)
                    + " photoIndex=" + k
                    + " type=" + safeLogValue(type)
                    + " name=" + safeLogValue(name)
                    + " url=" + safeLogValue(url)
                    + " status=" + safeLogValue(status));

            if (isBlank(type) || isBlank(name) || isBlank(url)) {
                plan.valid = false;
                plan.errors.add("photos[" + k
                        + "] requiere PhotoAssetType, PhotoAssetName y PhotoAssetURL no vacíos.");
                continue;
            }

            String rootCode = rootCodeForPhotoType(type);
            if (rootCode == null) {
                plan.valid = false;
                plan.errors.add("photos[" + k + "] tiene PhotoAssetType no soportado: " + type);
                continue;
            }

            increment(plan.typeCounts, rootCode);

            String recordKey;
            if ("ProductImageDetail".equals(rootCode)) {
                recordKey = recordKey(detailIndex++);
            } else if ("Illustration".equals(rootCode)) {
                recordKey = recordKey(illustrationIndex++);
            } else if ("ProductImageSmosh".equals(rootCode)) {
                recordKey = recordKey(smoshIndex++);
            } else {
                /* ProductImage conserva la semántica histórica: un único RK. */
                recordKey = "0000.0000.RK";
                productImageCount++;
                if (productImageCount > 1) {
                    audit(requestId, "PHOTO_WARNING",
                            ctx(plan)
                            + " reason=MULTIPLE_PRODUCT_IMAGE count=" + productImageCount
                            + " recordKey=0000.0000.RK");
                }
            }

            plan.characteristicRecords.put(createAssetRecord(rootCode, recordKey, photo));
        }

        if (!plan.errors.isEmpty()) {
            plan.valid = false;
            /*
             * Regla deliberada: si UNA foto está mal, no aplicamos parcialmente la
             * variante. Así replaceAssets=true nunca borra todo para reponer sólo
             * el subconjunto que sí logró parsear.
             */
            plan.characteristicRecords = new JSONArray();
        } else if (!isBlank(plan.variantId)) {
            plan.valid = true;
        }

        audit(requestId, "VARIANT_PLAN",
                ctx(plan)
                + " photosPresent=true photoCount=" + plan.photoCount
                + " clearIntent=false"
                + " plannedRecords=" + plan.characteristicRecords.length()
                + " types=" + formatTypeCounts(plan.typeCounts)
                + " validationOk=" + plan.valid
                + " errors=" + safeLogValue(join(plan.errors)));

        return plan;
    }

    private VariantExecution executeMerge(VariantPlan plan, String requestId) {
        if (plan.clearIntent) {
            /* replaceAssets=false + [] no borra; es un no-op explícitamente auditado. */
            audit(requestId, "VARIANT_NOOP",
                    ctx(plan) + " replaceAssets=false clearIntent=true reason=NO_DELETE_REQUESTED");
            return VariantExecution.skipped("NOOP_EMPTY_MERGE",
                    "photos=[] recibido, pero replaceAssets=false; no se borró contenido existente.");
        }

        WriteResult write = putCharacteristicRecords(plan.variantId, plan.characteristicRecords, requestId,
                "WRITE_NON_FINAL_MERGE", plan);
        if (!write.success) {
            return VariantExecution.failed("WRITE_FAILED", write.message);
        }

        return VariantExecution.success("MERGED", write.rawResponse);
    }

    private VariantExecution executeReplace(VariantPlan plan,
                                            String requestId,
                                            EliminaImagenesDeVariantes eliminator) {
        List<String> expectedSignatures = mediaSignatures(plan.characteristicRecords);

        if (plan.clearIntent) {
            audit(requestId, "CLEAR_INTENT",
                    ctx(plan)
                    + " explicitPhotosArray=true photoCount=0 action=DELETE_ALL_NON_FINAL_MEDIA");

            VerificationResult verification = verifyCurrentState(plan.variantId,
                    Collections.<String>emptyList(), requestId, plan, "VERIFY_CLEAR");

            if (verification.matches) {
                return VariantExecution.success("CLEARED", null);
            }

            audit(requestId, "CLEAR_VERIFY_FAILED",
                    ctx(plan)
                    + " actual=" + formatSignatures(verification.actualSignatures));

            RollbackResult rollback = rollback(plan, requestId, eliminator);
            return VariantExecution.failedWithRollback(
                    "CLEAR_VERIFICATION_FAILED",
                    "No se pudo confirmar el borrado total; se intentó restaurar el snapshot.",
                    rollback.success);
        }

        WriteResult write = putCharacteristicRecords(plan.variantId, plan.characteristicRecords, requestId,
                "WRITE_NON_FINAL_REPLACE", plan);

        if (!write.success) {
            RollbackResult rollback = rollback(plan, requestId, eliminator);
            return VariantExecution.failedWithRollback(
                    "WRITE_FAILED",
                    write.message,
                    rollback.success);
        }

        VerificationResult verification = verifyCurrentState(plan.variantId,
                expectedSignatures, requestId, plan, "VERIFY_REPLACE");

        if (!verification.matches) {
            audit(requestId, "VERIFY_MISMATCH",
                    ctx(plan)
                    + " expected=" + formatSignatures(expectedSignatures)
                    + " actual=" + formatSignatures(verification.actualSignatures));

            RollbackResult rollback = rollback(plan, requestId, eliminator);
            return VariantExecution.failedWithRollback(
                    "VERIFY_MISMATCH",
                    "El estado leído después del PUT no coincide con las imágenes recibidas.",
                    rollback.success);
        }

        audit(requestId, "VARIANT_REPLACE_OK",
                ctx(plan)
                + " expectedRecords=" + expectedSignatures.size()
                + " signatures=" + formatSignatures(expectedSignatures));

        return VariantExecution.success("REPLACED", write.rawResponse);
    }

    private RollbackResult rollback(VariantPlan plan,
                                    String requestId,
                                    EliminaImagenesDeVariantes eliminator) {
        audit(requestId, "ROLLBACK_START",
                ctx(plan)
                + " snapshotRecords=" + (plan.snapshotRecords == null ? -1 : plan.snapshotRecords.length())
                + " snapshotSignatures=" + formatSignatures(plan.snapshotSignatures));

        try {
            String items = EliminaImagenesDeVariantes.toItems(
                    java.util.Collections.singletonList(plan.variantId));
            eliminator.deleteAssets(items);

            if (plan.snapshotRecords != null && plan.snapshotRecords.length() > 0) {
                WriteResult restore = putCharacteristicRecords(plan.variantId, plan.snapshotRecords, requestId,
                        "ROLLBACK_WRITE", plan);
                if (!restore.success) {
                    audit(requestId, "ROLLBACK_WRITE_FAILED",
                            ctx(plan) + " detail=" + safeLogValue(restore.message));
                    return new RollbackResult(false);
                }
            }

            VerificationResult verify = verifyCurrentState(plan.variantId,
                    plan.snapshotSignatures == null
                            ? Collections.<String>emptyList()
                            : plan.snapshotSignatures,
                    requestId, plan, "ROLLBACK_VERIFY");

            audit(requestId, verify.matches ? "ROLLBACK_OK" : "ROLLBACK_VERIFY_FAILED",
                    ctx(plan)
                    + " expected=" + formatSignatures(plan.snapshotSignatures)
                    + " actual=" + formatSignatures(verify.actualSignatures));

            return new RollbackResult(verify.matches);
        } catch (Exception e) {
            auditError(requestId, "ROLLBACK_EXCEPTION " + ctx(plan), e);
            return new RollbackResult(false);
        }
    }

    private WriteResult putCharacteristicRecords(String variantId,
                                                 JSONArray records,
                                                 String requestId,
                                                 String eventPrefix,
                                                 VariantPlan plan) {
        JSONObject reqObj = new JSONObject().put("_characteristicRecords", records);
        Map<String, String> qp = new java.util.TreeMap<String, String>();
        qp.put("includeLabels", "true");
        qp.put("includeIds", "true");

        audit(requestId, eventPrefix + "_START",
                ctx(plan)
                + " records=" + records.length()
                + " signatures=" + formatSignatures(mediaSignatures(records)));

        final String[] rawHolder = new String[1];
        try {
            rw.writeData("PUT", "object", "Article", "'" + variantId + "'@1", qp, reqObj,
                    rr -> rawHolder[0] = rr);
        } catch (Exception e) {
            auditError(requestId, eventPrefix + "_EXCEPTION " + ctx(plan), e);
            return new WriteResult(false, null, e.getMessage());
        }

        String raw = rawHolder[0];
        WriteResult result = evaluateWriteResponse(raw);
        audit(requestId, eventPrefix + "_RESULT",
                ctx(plan)
                + " success=" + result.success
                + " responseChars=" + (raw == null ? 0 : raw.length())
                + " detail=" + safeLogValue(result.message)
                + (result.success ? "" : " raw=" + safeLogValue(raw)));
        return result;
    }

    private WriteResult evaluateWriteResponse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new WriteResult(false, raw, "Respuesta vacía del Object API.");
        }

        try {
            JSONObject response = new JSONObject(raw);
            JSONObject protocol = response.optJSONObject("_protocol");
            if (protocol != null && protocol.optInt("errorCounter", 0) > 0) {
                return new WriteResult(false, raw,
                        "Object API reportó errorCounter=" + protocol.optInt("errorCounter", 0));
            }

            if (response.has("error") || response.has("Error")) {
                return new WriteResult(false, raw, "Object API devolvió un objeto de error.");
            }

            return new WriteResult(true, raw, "OK");
        } catch (JSONException e) {
            return new WriteResult(false, raw, "Respuesta no JSON: " + e.getMessage());
        }
    }

    private JSONArray loadCurrentNonFinalRecords(String variantId) throws Exception {
        String encodedVariant = URLEncoder.encode(variantId, StandardCharsets.UTF_8.name());
        String url = rw.getRw().getBaseUrl()
                + "/object/Article/'" + encodedVariant
                + "'@1?includeLabels=true&includeIds=true&entityFilter=ArticleCharacteristicValue,Article";

        String raw = rw.getRw().getRc().getRequest("GET", url, null);
        if (raw == null || raw.trim().isEmpty()) {
            throw new IOException("Object API GET devolvió respuesta vacía para " + variantId);
        }

        JSONObject response = new JSONObject(raw);
        JSONObject protocol = response.optJSONObject("_protocol");
        if (protocol != null && protocol.optInt("errorCounter", 0) > 0) {
            throw new IOException("Object API GET reportó errorCounter="
                    + protocol.optInt("errorCounter", 0) + " para " + variantId);
        }

        JSONObject data = response.optJSONObject("_data");
        JSONArray current = data == null ? null : data.optJSONArray("_characteristicRecords");
        JSONArray result = new JSONArray();

        if (current == null) {
            return result;
        }

        for (int i = 0; i < current.length(); i++) {
            JSONObject record = current.optJSONObject(i);
            if (record == null) {
                continue;
            }
            String rootCode = characteristicCode(record);
            if (NON_FINAL_ROOTS.contains(rootCode)) {
                result.put(sanitizeRecordForWrite(record));
            }
        }

        return result;
    }

    private VerificationResult verifyCurrentState(String variantId,
                                                  List<String> expectedSignatures,
                                                  String requestId,
                                                  VariantPlan plan,
                                                  String eventName) {
        try {
            JSONArray current = loadCurrentNonFinalRecords(variantId);
            List<String> actual = mediaSignatures(current);
            List<String> expected = expectedSignatures == null
                    ? Collections.<String>emptyList()
                    : new ArrayList<String>(expectedSignatures);
            Collections.sort(expected);
            Collections.sort(actual);
            boolean matches = expected.equals(actual);

            audit(requestId, eventName,
                    ctx(plan)
                    + " matches=" + matches
                    + " expectedCount=" + expected.size()
                    + " actualCount=" + actual.size()
                    + (matches ? "" : " expected=" + formatSignatures(expected)
                            + " actual=" + formatSignatures(actual)));

            return new VerificationResult(matches, actual);
        } catch (Exception e) {
            auditError(requestId, eventName + "_ERROR " + ctx(plan), e);
            return new VerificationResult(false, Collections.<String>emptyList());
        }
    }

    private static JSONObject createAssetRecord(String rootCode,
                                                String recordKey,
                                                JSONObject photo) {
        JSONArray children = new JSONArray();

        children.put(createTextChild(rootCode + "_Name", recordKey,
                photo.getString("PhotoAssetName")));
        children.put(createTextChild(rootCode + "_URL", recordKey,
                photo.getString("PhotoAssetURL")));

        if (photo.has("PhotoAssetStatus") && !photo.isNull("PhotoAssetStatus")) {
            children.put(createStatusChild(rootCode + "_Status", recordKey,
                    photo.optString("PhotoAssetStatus", "")));
        }

        return new JSONObject()
                .put("_qualification", qualification(rootCode, recordKey))
                .put("_recordLang", new JSONArray()
                        .put(new JSONObject().put("values", new JSONArray())))
                .put("_children", children);
    }

    private static JSONObject createTextChild(String characteristic,
                                              String recordKey,
                                              String value) {
        return new JSONObject()
                .put("_qualification", qualification(characteristic, recordKey))
                .put("_recordLang", new JSONArray()
                        .put(new JSONObject().put("values", new JSONArray().put(value))));
    }

    private static JSONObject createStatusChild(String characteristic,
                                                String recordKey,
                                                String status) {
        return new JSONObject()
                .put("_qualification", qualification(characteristic, recordKey))
                .put("_recordLang", new JSONArray()
                        .put(new JSONObject().put("values",
                                new JSONArray().put(
                                        new JSONObject()
                                                .put("_qualification",
                                                        new JSONObject().put("language",
                                                                new JSONObject().put("_code", "zxx")))
                                                .put("_label", status)))));
    }

    private static JSONObject qualification(String characteristic, String recordKey) {
        return new JSONObject()
                .put("recordKey", recordKey)
                .put("characteristic", new JSONObject().put("_code", characteristic));
    }

    private static String rootCodeForPhotoType(String type) {
        if (type.startsWith("ProductImageDetail")) {
            return "ProductImageDetail";
        }
        if ("ProductImage".equals(type)) {
            return "ProductImage";
        }
        if (type.startsWith("Illustration")) {
            return "Illustration";
        }
        if (type.startsWith("ProductImageSmosh")) {
            return "ProductImageSmosh";
        }
        return null;
    }

    private static String recordKey(int index) {
        return index == 0
                ? "0000.0000.RK"
                : String.format(java.util.Locale.ROOT, "0000.%04d.RK", index);
    }

    private static JSONObject sanitizeRecordForWrite(JSONObject source) {
        JSONObject result = new JSONObject();
        JSONObject sourceQualification = source.optJSONObject("_qualification");
        if (sourceQualification != null) {
            String recordKey = sourceQualification.optString("recordKey", "0000.0000.RK");
            JSONObject characteristic = sourceQualification.optJSONObject("characteristic");
            String code = characteristic == null ? "" : characteristic.optString("_code", "");
            result.put("_qualification", qualification(code, recordKey));
        }

        JSONArray recordLang = source.optJSONArray("_recordLang");
        if (recordLang != null) {
            result.put("_recordLang", new JSONArray(recordLang.toString()));
        }

        JSONArray children = source.optJSONArray("_children");
        if (children != null) {
            JSONArray cleanChildren = new JSONArray();
            for (int i = 0; i < children.length(); i++) {
                JSONObject child = children.optJSONObject(i);
                if (child != null) {
                    cleanChildren.put(sanitizeRecordForWrite(child));
                }
            }
            result.put("_children", cleanChildren);
        }

        return result;
    }

    private static List<String> mediaSignatures(JSONArray records) {
        List<String> result = new ArrayList<String>();
        if (records == null) {
            return result;
        }

        for (int i = 0; i < records.length(); i++) {
            JSONObject record = records.optJSONObject(i);
            if (record == null) {
                continue;
            }

            String root = characteristicCode(record);
            if (!NON_FINAL_ROOTS.contains(root)) {
                continue;
            }

            JSONObject qualification = record.optJSONObject("_qualification");
            String rk = qualification == null
                    ? ""
                    : qualification.optString("recordKey", "");

            String name = "";
            String url = "";
            JSONArray children = record.optJSONArray("_children");
            if (children != null) {
                for (int j = 0; j < children.length(); j++) {
                    JSONObject child = children.optJSONObject(j);
                    if (child == null) {
                        continue;
                    }
                    String childCode = characteristicCode(child);
                    if ((root + "_Name").equals(childCode)) {
                        name = firstRecordValue(child);
                    } else if ((root + "_URL").equals(childCode)) {
                        url = firstRecordValue(child);
                    }
                }
            }

            result.add(root + "|" + rk + "|" + name + "|" + url);
        }

        Collections.sort(result);
        return result;
    }

    private static String characteristicCode(JSONObject record) {
        JSONObject qualification = record == null ? null : record.optJSONObject("_qualification");
        JSONObject characteristic = qualification == null ? null : qualification.optJSONObject("characteristic");
        return characteristic == null ? "" : characteristic.optString("_code", "");
    }

    private static String firstRecordValue(JSONObject record) {
        JSONArray recordLang = record.optJSONArray("_recordLang");
        if (recordLang == null || recordLang.length() == 0) {
            return "";
        }

        JSONObject lang = recordLang.optJSONObject(0);
        JSONArray values = lang == null ? null : lang.optJSONArray("values");
        if (values == null || values.length() == 0) {
            return "";
        }

        Object value = values.opt(0);
        if (value == null || value == JSONObject.NULL) {
            return "";
        }
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            if (object.has("_label")) {
                return object.optString("_label", "");
            }
            if (object.has("_code")) {
                return object.optString("_code", "");
            }
            return object.toString();
        }
        return String.valueOf(value);
    }

    private static JSONObject buildVariantResponse(VariantPlan plan, VariantExecution execution) {
        JSONObject response = new JSONObject()
                .put("variantId", plan.variantId)
                .put("success", execution.success)
                .put("skipped", execution.skipped)
                .put("action", execution.action)
                .put("photosPresent", plan.photosPresent)
                .put("photoCount", plan.photoCount)
                .put("clearIntent", plan.clearIntent)
                .put("plannedRecords", plan.characteristicRecords.length())
                .put("rollbackAttempted", execution.rollbackAttempted)
                .put("rollbackSucceeded", execution.rollbackSucceeded)
                .put("message", execution.message);

        if (!plan.errors.isEmpty()) {
            response.put("structureProblems", new JSONArray(plan.errors));
        }
        if (execution.rawResponse != null) {
            try {
                response.put("objectApiResponse", new JSONObject(execution.rawResponse));
            } catch (JSONException e) {
                response.put("objectApiResponseRaw", execution.rawResponse);
            }
        }
        return response;
    }

    private static String ctx(VariantPlan plan) {
        return "proposalId=" + safeLogValue(plan.proposalId)
                + " variantId=" + safeLogValue(plan.variantId)
                + " variantIndex=" + plan.variantIndex;
    }

    private static void increment(Map<String, Integer> counts, String key) {
        Integer value = counts.get(key);
        counts.put(key, value == null ? 1 : value + 1);
    }

    private static String formatTypeCounts(Map<String, Integer> counts) {
        return counts == null ? "{}" : counts.toString();
    }

    private static String formatIds(java.util.Collection<String> ids) {
        if (ids == null) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        int count = 0;
        for (String id : ids) {
            if (count > 0) {
                sb.append(',');
            }
            if (count >= 100) {
                sb.append("...+").append(ids.size() - count);
                break;
            }
            sb.append(safeLogValue(id));
            count++;
        }
        return sb.append(']').toString();
    }

    private static String formatSignatures(List<String> signatures) {
        if (signatures == null) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < signatures.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            if (i >= 50) {
                sb.append("...+").append(signatures.size() - i);
                break;
            }
            sb.append(safeLogValue(signatures.get(i)));
        }
        return sb.append(']').toString();
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(value);
        }
        return sb.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String safeLogValue(String value) {
        if (value == null) {
            return "null";
        }
        String clean = value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ');
        int max = 1800;
        return clean.length() <= max ? clean : clean.substring(0, max) + "...[truncated]";
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format(java.util.Locale.ROOT, "%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return "UNAVAILABLE";
        }
    }

    private static final class ProductPlan {
        String proposalId = "";
        final List<VariantPlan> variants = new ArrayList<VariantPlan>();
    }

    private static final class VariantPlan {
        String proposalId = "";
        String variantId = "";
        int variantIndex;
        boolean photosPresent;
        int photoCount;
        boolean clearIntent;
        boolean valid = true;
        boolean snapshotReady;
        JSONArray characteristicRecords = new JSONArray();
        JSONArray snapshotRecords = new JSONArray();
        List<String> snapshotSignatures = new ArrayList<String>();
        final Map<String, Integer> typeCounts = new LinkedHashMap<String, Integer>();
        final List<String> errors = new ArrayList<String>();

        boolean shouldProcess() {
            return photosPresent && valid && !isBlank(variantId);
        }
    }

    private static final class WriteResult {
        final boolean success;
        final String rawResponse;
        final String message;

        WriteResult(boolean success, String rawResponse, String message) {
            this.success = success;
            this.rawResponse = rawResponse;
            this.message = message == null ? "" : message;
        }
    }

    private static final class VerificationResult {
        final boolean matches;
        final List<String> actualSignatures;

        VerificationResult(boolean matches, List<String> actualSignatures) {
            this.matches = matches;
            this.actualSignatures = actualSignatures;
        }
    }

    private static final class RollbackResult {
        final boolean success;

        RollbackResult(boolean success) {
            this.success = success;
        }
    }

    private static final class VariantExecution {
        final boolean success;
        final boolean skipped;
        final String action;
        final String message;
        final String rawResponse;
        final boolean rollbackAttempted;
        final boolean rollbackSucceeded;

        private VariantExecution(boolean success,
                                 boolean skipped,
                                 String action,
                                 String message,
                                 String rawResponse,
                                 boolean rollbackAttempted,
                                 boolean rollbackSucceeded) {
            this.success = success;
            this.skipped = skipped;
            this.action = action;
            this.message = message == null ? "" : message;
            this.rawResponse = rawResponse;
            this.rollbackAttempted = rollbackAttempted;
            this.rollbackSucceeded = rollbackSucceeded;
        }

        static VariantExecution success(String action, String rawResponse) {
            return new VariantExecution(true, false, action, "OK", rawResponse, false, false);
        }

        static VariantExecution skipped(String action, String message) {
            return new VariantExecution(false, true, action, message, null, false, false);
        }

        static VariantExecution failed(String action, String message) {
            return new VariantExecution(false, false, action, message, null, false, false);
        }

        static VariantExecution failedWithRollback(String action,
                                                   String message,
                                                   boolean rollbackSucceeded) {
            return new VariantExecution(false, false, action,
                    message + " rollbackSucceeded=" + rollbackSucceeded,
                    null, true, rollbackSucceeded);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(LasImagenes.class.getName());

    static {
        try {
            LOGGER.setUseParentHandlers(false);

            FileHandler fileHandler = new FileHandler(
                    "../logs/receive_media-%g.log",
                    25 * 1024 * 1024,
                    10,
                    true);
            fileHandler.setEncoding(StandardCharsets.UTF_8.name());
            fileHandler.setLevel(Level.ALL);

            fileHandler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    java.time.LocalDateTime dateTime =
                            java.time.Instant.ofEpochMilli(record.getMillis())
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDateTime();

                    String timestamp = dateTime.format(
                            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

                    return "[" + timestamp + "] [" + record.getLevel() + "] "
                            + formatMessage(record) + System.lineSeparator();
                }
            });

            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);

        } catch (IOException e) {
            throw new RuntimeException("No se pudo inicializar el logger", e);
        }
    }

    private static void audit(String requestId, String event, String detail) {
        LOGGER.info("[IMAGE_AUDIT] requestId=" + requestId
                + " event=" + event
                + (detail == null || detail.isEmpty() ? "" : " " + detail));
    }

    private static void auditError(String requestId, String event, Exception e) {
        LOGGER.log(Level.SEVERE,
                "[IMAGE_AUDIT] requestId=" + requestId
                + " event=" + event
                + " exception=" + safeLogValue(e == null ? null : e.toString()),
                e);
    }
}
