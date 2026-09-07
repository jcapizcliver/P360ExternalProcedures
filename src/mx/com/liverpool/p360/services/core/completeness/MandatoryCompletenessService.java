package mx.com.liverpool.p360.services.core.completeness;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Motor V1 de Mandatory Completeness.
 *
 * Semántica deliberadamente basada en la consulta validadora proporcionada:
 *   - Metadata local: ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla
 *   - CreationType = CreateProposal
 *   - IsMandatory = 1/true/yes/si/sí
 *   - ColoursLiverpoolAtt y TamanoUnico se excluyen del Mandatory de Product2G
 *   - Si la plantilla tiene configuración Business aplicable al negocio, se
 *     restringe a los mandatory cuya configuración Business también aplica.
 *
 * Porcentaje persistible en Product2G.MandatoryCompleteness:
 *   100.0000 -> completo / sin obligatorios / bypass de producto especial
 *   0.0000   -> falta plantilla o negocio (no scorable, pero visible en dashboard)
 *   NULL     -> producto no encontrado
 */
public final class MandatoryCompletenessService implements ProductCompletenessService {

    public static final String ENGINE_VERSION = "MANDATORY-1.0.0";
    public static final String WORK_TABLE = "P360_EXPLOIT.TT_PRODUCT_COMPLETENESS_WORK";

    private final Connection connection;

    public MandatoryCompletenessService(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection is required");
        }
        this.connection = connection;
    }

    @Override
    public CompletenessResult calculate(String productIdentifier) throws SQLException {
        if (productIdentifier == null || productIdentifier.isBlank()) {
            throw new IllegalArgumentException("productIdentifier is required");
        }

        String runId = UUID.randomUUID().toString();
        try {
            insertWorkRow(runId, productIdentifier.trim());
            List<CompletenessResult> values = calculateWorkBatch(runId);
            return values.isEmpty() ? null : values.get(0);
        } finally {
            deleteWorkRows(runId);
        }
    }

    public List<CompletenessResult> calculateWorkBatch(String runId) throws SQLException {
        if (runId == null || runId.isBlank()) {
            return Collections.emptyList();
        }

        List<CompletenessResult> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(BATCH_SQL)) {
            ps.setString(1, runId);
            ps.setFetchSize(2000);
            ps.setQueryTimeout(120);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(fromRow(rs));
                }
            }
        }
        return result;
    }

    private CompletenessResult fromRow(ResultSet rs) throws SQLException {
        String identifier = rs.getString("Identifier");
        long rawRevisionId = rs.getLong("ArticleRevisionID");
        Long articleRevisionId = rs.wasNull() ? null : Long.valueOf(rawRevisionId);
        String template = rs.getString("Template");
        String businessCode = rs.getString("BusinessCode");
        String businessName = rs.getString("BusinessName");
        int total = rs.getInt("CamposObligatorios");
        int present = rs.getInt("CamposPoblados");
        int missing = rs.getInt("CamposFaltantes");
        String missingDetail = rs.getString("CamposFaltantesDetalle");

        CompletenessResult.MetricResult metric =
                buildMetric(identifier, articleRevisionId, template, businessCode,
                        total, present, missing, missingDetail);

        return new CompletenessResult(
                identifier,
                articleRevisionId,
                template,
                businessCode,
                businessName,
                metric,
                Instant.now(),
                ENGINE_VERSION);
    }

    private CompletenessResult.MetricResult buildMetric(
            String identifier,
            Long articleRevisionId,
            String template,
            String businessCode,
            int total,
            int present,
            int missing,
            String missingDetail) {

        if (articleRevisionId == null) {
            return new CompletenessResult.MetricResult(
                    total, present, missing, null, false,
                    "PRODUCT_NOT_FOUND", missingDetail);
        }

        if (isSpecialBypass(identifier)) {
            return new CompletenessResult.MetricResult(
                    total, present, missing, pct(100), true,
                    "BYPASS_SPECIAL_PRODUCT", missingDetail);
        }

        if (template == null || template.isBlank()) {
            return new CompletenessResult.MetricResult(
                    total, present, missing, pct(0), false,
                    "MISSING_TEMPLATE", missingDetail);
        }

        if (businessCode == null || businessCode.isBlank()) {
            return new CompletenessResult.MetricResult(
                    total, present, missing, pct(0), false,
                    "MISSING_BUSINESS", missingDetail);
        }

        if (total == 0) {
            return new CompletenessResult.MetricResult(
                    0, 0, 0, pct(100), true,
                    "NO_MANDATORY_CONFIG", null);
        }

        BigDecimal percentage = BigDecimal.valueOf(present)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);

        return new CompletenessResult.MetricResult(
                total,
                present,
                missing,
                percentage,
                true,
                missing == 0 ? "OK" : "MISSING_FIELDS",
                missingDetail);
    }

    private static BigDecimal pct(int value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.UNNECESSARY);
    }

    private static boolean isSpecialBypass(String identifier) {
        if (identifier == null) return false;
        String value = identifier.trim();
        return value.length() == 12 && value.startsWith("10000");
    }

    private void insertWorkRow(String runId, String productIdentifier) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "insert into " + WORK_TABLE + " (RUN_ID, PRODUCT_ID) values (?, ?)")) {
            ps.setString(1, runId);
            ps.setNString(2, productIdentifier);
            ps.executeUpdate();
        }
    }

    public void deleteWorkRows(String runId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "delete from " + WORK_TABLE + " where RUN_ID = ?")) {
            ps.setString(1, runId);
            ps.executeUpdate();
        }
    }

    /**
     * Consulta set-based derivada de "Validación obligatorios por plantilla.sql".
     * La única diferencia estructural relevante es que la fuente es la tabla de
     * trabajo por RUN_ID, de forma que puede procesar 10k+ IDs sin un IN gigante.
     */
    private static final String BATCH_SQL = """
        with input_ids as (
            select /*+ materialize */
                   w.PRODUCT_ID as "InputIdentifier"
            from P360_EXPLOIT.TT_PRODUCT_COMPLETENESS_WORK w
            where w.RUN_ID = ?
        ),
        src as (
            select /*+
                       materialize
                       leading(tt ar ad)
                       use_nl(ar ad)
                       index(ar IX_AR_TUNE_01)
                       index(ad XAK1_ArticleDetail)
                   */
                   tt."InputIdentifier",
                   ar.ID as "ArticleRevisionID",
                   ar."Identifier" as "P360Identifier",
                   ad."Res_Int_01" as "BusinessID"
            from input_ids tt
            left join PIM_MASTER."ArticleRevision" ar
                on ar."Identifier" = tt."InputIdentifier"
               and ar."EntityID" = 1100
               and ar."RevisionID" = 1
               and ar."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
            left join PIM_MASTER."ArticleDetail" ad
                on ad."ArticleRevisionID" = ar.ID
               and ad."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
        ),
        templates as (
            select /*+ materialize leading(s asm sr) use_nl(asm sr) */
                   s."InputIdentifier",
                   s."ArticleRevisionID",
                   min(asm."StructureGroupIdentifier") as "Template"
            from src s
            inner join PIM_MASTER."ArticleStructureMap" asm
                on asm."ArticleRevisionID" = s."ArticleRevisionID"
               and asm."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
            inner join PIM_MAIN."StructureRevision" sr
                on sr."StructureID" = asm."StructureID"
               and sr."Identifier" = N'PrimaryProductTaxonomy'
               and sr."RevisionID" = 1
               and sr."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
            where asm."StructureGroupIdentifier" is not null
            group by s."InputIdentifier", s."ArticleRevisionID"
        ),
        products as (
            select /*+ materialize */
                   s."InputIdentifier",
                   s."ArticleRevisionID",
                   s."P360Identifier",
                   t."Template",
                   bus."Code" as "BusinessCode",
                   case bus."Code"
                       when N'LVP' then N'Liverpool'
                       when N'SBB' then N'Suburbia'
                       when N'MKP' then N'Marketplace'
                       else bus."Code"
                   end as "BusinessName"
            from src s
            left join templates t
                on t."InputIdentifier" = s."InputIdentifier"
               and t."ArticleRevisionID" = s."ArticleRevisionID"
            left join PIM_MAIN."LookupValueRevision" bus
                on bus."LookupValueID" = s."BusinessID"
               and bus."RevisionID" = 1
               and bus."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
        ),
        relevant_templates as (
            select /*+ materialize */ distinct "Template"
            from products
            where "Template" is not null
        ),
        metadata_rows as (
            select /*+
                       materialize
                       leading(dict meta tpl tpl_lookup rt ct ct_lookup cr prop)
                       use_nl(meta tpl tpl_lookup rt ct ct_lookup cr prop)
                       index(dict XAK2_LookupRevision)
                       index(meta IX_LVREV_METADATA_EXT_01)
                   */
                   tpl."Code" as "Template",
                   cr."CharacteristicID" as "CharacteristicID",
                   cr."Identifier" as "Characteristic",
                   prop."Code" as "Property",
                   dbms_lob.substr(meta."Res_Text2G_01", 2000, 1) as "PropertyValue"
            from PIM_MAIN."LookupRevision" dict
            inner join PIM_MAIN."LookupValueRevision" meta
                on meta."LookupID" = dict."LookupID"
               and meta."RevisionID" = 1
               and meta."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
            inner join PIM_MAIN."LookupValueRevision" tpl
                on tpl."LookupValueID" = meta."Res_Int_01"
               and tpl."RevisionID" = 1
               and tpl."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
            inner join PIM_MAIN."LookupRevision" tpl_lookup
                on tpl_lookup."LookupID" = tpl."LookupID"
               and tpl_lookup."Identifier" = N'PPH_L4_Templates'
               and tpl_lookup."RevisionID" = 1
               and tpl_lookup."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
            inner join relevant_templates rt
                on rt."Template" = tpl."Code"
            inner join PIM_MAIN."LookupValueRevision" ct
                on ct."LookupValueID" = meta."Res_Int_04"
               and ct."Code" = N'CreateProposal'
               and ct."RevisionID" = 1
               and ct."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
            inner join PIM_MAIN."LookupRevision" ct_lookup
                on ct_lookup."LookupID" = ct."LookupID"
               and ct_lookup."Identifier" = N'CreationType'
               and ct_lookup."RevisionID" = 1
               and ct_lookup."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
            inner join PIM_MAIN."CharacteristicRevision" cr
                on cr."CharacteristicID" = meta."Res_Int_02"
               and cr."RevisionID" = 1
               and cr."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
            inner join PIM_MAIN."LookupValueRevision" prop
                on prop."LookupValueID" = meta."Res_Int_03"
               and prop."RevisionID" = 1
               and prop."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
            where dict."Identifier" = N'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'
              and dict."RevisionID" = 1
              and dict."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
              and prop."Code" in (N'IsMandatory', N'Business')
              and cr."Identifier" not in (N'ColoursLiverpoolAtt', N'TamanoUnico')
        ),
        mandatory_config as (
            select /*+ materialize */ distinct
                   mr."Template",
                   mr."CharacteristicID",
                   mr."Characteristic"
            from metadata_rows mr
            where mr."Property" = N'IsMandatory'
              and lower(trim(mr."PropertyValue")) in
                  (N'1', N'true', N'yes', N'si', N'sí')
        ),
        business_config as (
            select /*+ materialize */ distinct
                   mr."Template",
                   mr."CharacteristicID",
                   mr."Characteristic",
                   mr."PropertyValue" as "AllowedBusiness"
            from metadata_rows mr
            where mr."Property" = N'Business'
              and mr."PropertyValue" is not null
        ),
        business_scope as (
            select /*+ materialize */
                   p."InputIdentifier",
                   max(
                       case
                           when bc."CharacteristicID" is not null
                            and p."BusinessName" is not null
                            and instr(lower(bc."AllowedBusiness"), lower(p."BusinessName")) > 0
                           then 1 else 0
                       end
                   ) as "HasBusinessSpecificConfig"
            from products p
            left join business_config bc
                on bc."Template" = p."Template"
            group by p."InputIdentifier"
        ),
        mandatory_applicable as (
            select /*+ materialize */ distinct
                   p."InputIdentifier",
                   p."ArticleRevisionID",
                   p."Template",
                   p."BusinessCode",
                   p."BusinessName",
                   mc."CharacteristicID",
                   mc."Characteristic"
            from products p
            inner join business_scope bs
                on bs."InputIdentifier" = p."InputIdentifier"
            inner join mandatory_config mc
                on mc."Template" = p."Template"
            where bs."HasBusinessSpecificConfig" = 0
               or exists (
                   select 1
                   from business_config bc
                   where bc."Template" = p."Template"
                     and bc."CharacteristicID" = mc."CharacteristicID"
                     and p."BusinessName" is not null
                     and instr(lower(bc."AllowedBusiness"), lower(p."BusinessName")) > 0
               )
        ),
        mandatory_evaluation as (
            select /*+
                       materialize
                       leading(ma acv lvr lvl)
                       use_nl(acv lvr lvl)
                       index(acv IX_ACV_TUNE_02)
                   */
                   ma."InputIdentifier",
                   ma."Characteristic",
                   max(
                       case
                           when acv."Value" is not null
                            and length(trim(acv."Value")) > 0
                           then 1
                           when acv."Value" is null
                            and lvl."Name" is not null
                            and length(trim(lvl."Name")) > 0
                           then 1
                           else 0
                       end
                   ) as "HasValue"
            from mandatory_applicable ma
            left join PIM_MASTER."ArticleCharactValue" acv
                on acv."ArticleRevisionID" = ma."ArticleRevisionID"
               and acv."CharacteristicID" = ma."CharacteristicID"
               and acv."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
            left join PIM_MAIN."LookupValueRevision" lvr
                on lvr."LookupValueID" = acv."LookupValueID"
               and lvr."RevisionID" = 1
               and lvr."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
            left join PIM_MAIN."LookupValueLang" lvl
                on lvl."LookupValueRevisionID" = lvr.ID
               and lvl."LanguageID" = 10
               and lvl."DeletionTimestamp" = timestamp '9999-12-31 00:00:00.0'
            group by ma."InputIdentifier", ma."Characteristic"
        ),
        summary as (
            select
                   p."InputIdentifier",
                   p."ArticleRevisionID",
                   p."P360Identifier",
                   p."Template",
                   p."BusinessCode",
                   p."BusinessName",
                   count(me."Characteristic") as "CamposObligatorios",
                   nvl(sum(me."HasValue"), 0) as "CamposPoblados",
                   count(me."Characteristic") - nvl(sum(me."HasValue"), 0) as "CamposFaltantes",
                   listagg(
                       case when me."HasValue" = 0 then to_char(me."Characteristic") end,
                       ', '
                   ) within group (order by to_char(me."Characteristic"))
                       as "CamposFaltantesDetalle"
            from products p
            left join mandatory_evaluation me
                on me."InputIdentifier" = p."InputIdentifier"
            group by
                   p."InputIdentifier",
                   p."ArticleRevisionID",
                   p."P360Identifier",
                   p."Template",
                   p."BusinessCode",
                   p."BusinessName"
        )
        select
               s."InputIdentifier" as "Identifier",
               s."ArticleRevisionID",
               s."P360Identifier",
               s."Template",
               s."BusinessCode",
               s."BusinessName",
               s."CamposObligatorios",
               s."CamposPoblados",
               s."CamposFaltantes",
               s."CamposFaltantesDetalle"
        from summary s
        order by s."InputIdentifier"
        """;
}
