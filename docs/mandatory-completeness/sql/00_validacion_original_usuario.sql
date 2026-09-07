with src as (
    select /*+
               materialize
               leading(tt ar ad)
               use_nl(ar ad)
               index(ar IX_AR_TUNE_01)
               index(ad XAK1_ArticleDetail)
           */
           tt.IDENTIFIER as "InputIdentifier",
           ar.ID as "ArticleRevisionID",
           ar."Identifier" as "P360Identifier",
           ad."Res_Int_01" as "BusinessID"
    from P360_EXPLOIT.TT_20260831_190712_UIWS tt

    left join PIM_MASTER."ArticleRevision" ar
        on ar."Identifier" = to_nchar(tt.IDENTIFIER)
       and ar."EntityID" = 1100
       and ar."RevisionID" = 1
       and ar."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'

    left join PIM_MASTER."ArticleDetail" ad
        on ad."ArticleRevisionID" = ar.ID
       and ad."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'
),

templates as (
    select /*+
               materialize
               leading(s asm sr)
               use_nl(asm sr)
           */
           s."InputIdentifier",
           s."ArticleRevisionID",
           min(
               asm."StructureGroupIdentifier"
           ) as "Template"
    from src s

    inner join PIM_MASTER."ArticleStructureMap" asm
        on asm."ArticleRevisionID" =
               s."ArticleRevisionID"
       and asm."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'

    inner join PIM_MAIN."StructureRevision" sr
        on sr."StructureID" =
               asm."StructureID"
       and sr."Identifier" =
               N'PrimaryProductTaxonomy'
       and sr."RevisionID" = 1
       and sr."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'

    where asm."StructureGroupIdentifier" is not null

    group by
           s."InputIdentifier",
           s."ArticleRevisionID"
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
        on t."InputIdentifier" =
               s."InputIdentifier"
       and t."ArticleRevisionID" =
               s."ArticleRevisionID"

    left join PIM_MAIN."LookupValueRevision" bus
        on bus."LookupValueID" =
               s."BusinessID"
       and bus."RevisionID" = 1
       and bus."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'
),

relevant_templates as (
    select /*+ materialize */
           distinct
           "Template"
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

           cr."CharacteristicID"
               as "CharacteristicID",

           cr."Identifier"
               as "Characteristic",

           prop."Code"
               as "Property",

           dbms_lob.substr(
               meta."Res_Text2G_01",
               2000,
               1
           ) as "PropertyValue"

    from PIM_MAIN."LookupRevision" dict

    inner join PIM_MAIN."LookupValueRevision" meta
        on meta."LookupID" =
               dict."LookupID"
       and meta."RevisionID" = 1
       and meta."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'

    inner join PIM_MAIN."LookupValueRevision" tpl
        on tpl."LookupValueID" =
               meta."Res_Int_01"
       and tpl."RevisionID" = 1
       and tpl."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'

    inner join PIM_MAIN."LookupRevision" tpl_lookup
        on tpl_lookup."LookupID" =
               tpl."LookupID"
       and tpl_lookup."Identifier" =
               N'PPH_L4_Templates'
       and tpl_lookup."RevisionID" = 1
       and tpl_lookup."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'

    inner join relevant_templates rt
        on rt."Template" =
               tpl."Code"

    inner join PIM_MAIN."LookupValueRevision" ct
        on ct."LookupValueID" =
               meta."Res_Int_04"
       and ct."Code" =
               N'CreateProposal'
       and ct."RevisionID" = 1
       and ct."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'

    inner join PIM_MAIN."LookupRevision" ct_lookup
        on ct_lookup."LookupID" =
               ct."LookupID"
       and ct_lookup."Identifier" =
               N'CreationType'
       and ct_lookup."RevisionID" = 1
       and ct_lookup."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'

    inner join PIM_MAIN."CharacteristicRevision" cr
        on cr."CharacteristicID" =
               meta."Res_Int_02"
       and cr."RevisionID" = 1
       and cr."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'

    inner join PIM_MAIN."LookupValueRevision" prop
        on prop."LookupValueID" =
               meta."Res_Int_03"
       and prop."RevisionID" = 1
       and prop."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'

    where dict."Identifier" =
              N'ExtensionDeMetadatos_ ValoresPredeterminadosPorPlantilla'

      and dict."RevisionID" = 1

      and dict."DeletionTimestamp" =
          timestamp '9999-12-31 00:00:00.0'

      and prop."Code" in (
          N'IsMandatory',
          N'Business'
      )

      and cr."Identifier" not in (
          N'ColoursLiverpoolAtt',
          N'TamanoUnico'
      )
),

mandatory_config as (
    select /*+ materialize */
           distinct
           mr."Template",
           mr."CharacteristicID",
           mr."Characteristic"

    from metadata_rows mr

    where mr."Property" =
              N'IsMandatory'

      and lower(
              trim(
                  mr."PropertyValue"
              )
          ) in (
              N'1',
              N'true',
              N'yes',
              N'si',
              N'sí'
          )
),

business_config as (
    select /*+ materialize */
           distinct
           mr."Template",
           mr."CharacteristicID",
           mr."Characteristic",
           mr."PropertyValue"
               as "AllowedBusiness"

    from metadata_rows mr

    where mr."Property" =
              N'Business'

      and mr."PropertyValue"
              is not null
),

business_scope as (
    select /*+ materialize */
           p."InputIdentifier",

           max(
               case
                   when bc."CharacteristicID"
                            is not null

                    and p."BusinessName"
                            is not null

                    and instr(
                            lower(
                                bc."AllowedBusiness"
                            ),
                            lower(
                                p."BusinessName"
                            )
                        ) > 0

                   then 1
                   else 0
               end
           ) as "HasBusinessSpecificConfig"

    from products p

    left join business_config bc
        on bc."Template" =
               p."Template"

    group by
           p."InputIdentifier"
),

mandatory_applicable as (
    select /*+ materialize */
           distinct
           p."InputIdentifier",
           p."ArticleRevisionID",
           p."Template",
           p."BusinessCode",
           p."BusinessName",

           mc."CharacteristicID",
           mc."Characteristic"

    from products p

    inner join business_scope bs
        on bs."InputIdentifier" =
               p."InputIdentifier"

    inner join mandatory_config mc
        on mc."Template" =
               p."Template"

    where
          bs."HasBusinessSpecificConfig" = 0

       or exists (
           select 1

           from business_config bc

           where bc."Template" =
                     p."Template"

             and bc."CharacteristicID" =
                     mc."CharacteristicID"

             and p."BusinessName"
                     is not null

             and instr(
                     lower(
                         bc."AllowedBusiness"
                     ),
                     lower(
                         p."BusinessName"
                     )
                 ) > 0
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
                    and length(
                            trim(
                                acv."Value"
                            )
                        ) > 0
                   then 1

                   when acv."Value" is null
                    and lvl."Name" is not null
                    and length(
                            trim(
                                lvl."Name"
                            )
                        ) > 0
                   then 1

                   else 0
               end
           ) as "HasValue"

    from mandatory_applicable ma

    left join PIM_MASTER."ArticleCharactValue" acv
        on acv."ArticleRevisionID" =
               ma."ArticleRevisionID"

       and acv."CharacteristicID" =
               ma."CharacteristicID"

       and acv."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'

    left join PIM_MAIN."LookupValueRevision" lvr
        on lvr."LookupValueID" =
               acv."LookupValueID"

       and lvr."RevisionID" = 1

       and lvr."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'

    left join PIM_MAIN."LookupValueLang" lvl
        on lvl."LookupValueRevisionID" =
               lvr.ID

       and lvl."LanguageID" = 10

       and lvl."DeletionTimestamp" =
           timestamp '9999-12-31 00:00:00.0'

    group by
           ma."InputIdentifier",
           ma."Characteristic"
),

summary as (
    select
           p."InputIdentifier",
           p."ArticleRevisionID",
           p."P360Identifier",
           p."Template",
           p."BusinessCode",
           p."BusinessName",

           count(
               me."Characteristic"
           ) as "CamposObligatorios",

           nvl(
               sum(
                   me."HasValue"
               ),
               0
           ) as "CamposPoblados",

           count(
               me."Characteristic"
           )
           -
           nvl(
               sum(
                   me."HasValue"
               ),
               0
           ) as "CamposFaltantes",

           listagg(
               case
                   when me."HasValue" = 0
                   then to_char(
                            me."Characteristic"
                        )
               end,
               ', '
           ) within group (
               order by
                   to_char(
                       me."Characteristic"
                   )
           ) as "CamposFaltantesDetalle"

    from products p

    left join mandatory_evaluation me
        on me."InputIdentifier" =
               p."InputIdentifier"

    group by
           p."InputIdentifier",
           p."ArticleRevisionID",
           p."P360Identifier",
           p."Template",
           p."BusinessCode",
           p."BusinessName"
)

select
       s."InputIdentifier"
           as "Identifier",

       s."P360Identifier",

       s."Template",

       s."BusinessCode",

       s."BusinessName",

       s."CamposObligatorios",

       s."CamposPoblados",

       s."CamposFaltantes",

       case
           when s."ArticleRevisionID"
                    is null
           then 'PRODUCTO NO ENCONTRADO'

           when substr(
                    to_nchar(
                        s."InputIdentifier"
                    ),
                    1,
                    5
                ) = N'10000'

            and length(
                    trim(
                        s."InputIdentifier"
                    )
                ) = 12

           then 'OK - BYPASS PRODUCTO ESPECIAL'

           when s."Template"
                    is null
           then 'FALTA PLANTILLA'

           when s."BusinessCode"
                    is null
           then 'FALTA NEGOCIO'

           when s."CamposObligatorios" = 0
           then 'OK - SIN OBLIGATORIOS CONFIGURADOS'

           when s."CamposFaltantes" = 0
           then 'OK'

           else 'FALTAN CAMPOS'
       end as "Validacion",

       s."CamposFaltantesDetalle"

from summary s

order by
       case
           when s."ArticleRevisionID" is null
           then 0

           when s."CamposFaltantes" > 0
           then 1

           else 2
       end,

       s."InputIdentifier"