--------------------------------------------------------------------------------
-- P360 completeness support tables
-- Current-state snapshot + reusable work table for set-based recalculation.
--------------------------------------------------------------------------------

create table P360_EXPLOIT.TT_PRODUCT_COMPLETENESS_WORK (
    RUN_ID        varchar2(36 char)  not null,
    PRODUCT_ID    nvarchar2(255)     not null,
    CREATED_AT    timestamp(6)       default systimestamp not null,
    constraint PK_TT_PRODUCT_COMPLETENESS_WORK
        primary key (RUN_ID, PRODUCT_ID)
        using index tablespace P360_EXPLOIT_INDEX
)
tablespace P360_EXPLOIT_DATA;

create index P360_EXPLOIT.IX_TT_PROD_COMP_WORK_01
    on P360_EXPLOIT.TT_PRODUCT_COMPLETENESS_WORK (PRODUCT_ID, RUN_ID)
    tablespace P360_EXPLOIT_INDEX;


create table P360_EXPLOIT.TB_COMPLETENESS_SNAPSHOT (
    PRODUCT_ID                         nvarchar2(255)    not null,
    ARTICLE_REVISION_ID                number(19,0),
    TEMPLATE                           nvarchar2(255),
    BUSINESS_CODE                      nvarchar2(100),
    BUSINESS_NAME                      nvarchar2(255),

    MANDATORY_TOTAL                    number(10,0),
    MANDATORY_PRESENT                  number(10,0),
    MANDATORY_MISSING                  number(10,0),
    MANDATORY_PCT                      number(12,4),
    MANDATORY_SCORABLE                 char(1 char),
    MANDATORY_STATUS                   varchar2(64 char),
    MANDATORY_MISSING_DETAIL           nclob,
    MANDATORY_CALC_TS                  timestamp(6),
    MANDATORY_ENGINE_VERSION           varchar2(64 char),
    MANDATORY_SYNC_STATUS              varchar2(32 char),
    MANDATORY_SYNC_TS                  timestamp(6),
    MANDATORY_SYNC_MESSAGE             varchar2(2000 char),

    -- Reservados desde ahora para las siguientes dos métricas.
    VENDORCENTER_TOTAL                 number(10,0),
    VENDORCENTER_PRESENT               number(10,0),
    VENDORCENTER_MISSING               number(10,0),
    VENDORCENTER_PCT                   number(12,4),
    VENDORCENTER_STATUS                varchar2(64 char),
    VENDORCENTER_CALC_TS               timestamp(6),
    VENDORCENTER_ENGINE_VERSION        varchar2(64 char),

    ECOMMERCE_TOTAL                    number(10,0),
    ECOMMERCE_PRESENT                  number(10,0),
    ECOMMERCE_MISSING                  number(10,0),
    ECOMMERCE_PCT                      number(12,4),
    ECOMMERCE_STATUS                   varchar2(64 char),
    ECOMMERCE_CALC_TS                  timestamp(6),
    ECOMMERCE_ENGINE_VERSION           varchar2(64 char),

    LAST_RUN_ID                        varchar2(36 char),
    LAST_CALC_TS                       timestamp(6) default systimestamp not null,

    constraint PK_TB_COMPLETENESS_SNAPSHOT
        primary key (PRODUCT_ID)
        using index tablespace P360_EXPLOIT_INDEX,

    constraint CK_TB_COMP_MAND_SCORABLE
        check (MANDATORY_SCORABLE in ('Y','N') or MANDATORY_SCORABLE is null),

    constraint CK_TB_COMP_MAND_PCT
        check (MANDATORY_PCT between 0 and 100 or MANDATORY_PCT is null)
)
tablespace P360_EXPLOIT_DATA
lob (MANDATORY_MISSING_DETAIL) store as securefile (
    tablespace P360_EXPLOIT_DATA
);

create index P360_EXPLOIT.IX_TB_COMP_SNAPSHOT_01
    on P360_EXPLOIT.TB_COMPLETENESS_SNAPSHOT
       (TEMPLATE, BUSINESS_CODE, MANDATORY_PCT)
    tablespace P360_EXPLOIT_INDEX;

create index P360_EXPLOIT.IX_TB_COMP_SNAPSHOT_02
    on P360_EXPLOIT.TB_COMPLETENESS_SNAPSHOT
       (MANDATORY_STATUS, MANDATORY_PCT)
    tablespace P360_EXPLOIT_INDEX;

create index P360_EXPLOIT.IX_TB_COMP_SNAPSHOT_03
    on P360_EXPLOIT.TB_COMPLETENESS_SNAPSHOT
       (ARTICLE_REVISION_ID)
    tablespace P360_EXPLOIT_INDEX;
