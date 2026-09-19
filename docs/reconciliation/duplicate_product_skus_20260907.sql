-- Catalogo maestro, productos vigentes. Un Identifier aparece una vez por SKU.
-- CurrentStatus NULL queda excluido por la condicion solicitada != 1025.
WITH eligible AS (
 SELECT DISTINCT ar."Identifier" AS IDENTIFIER, ad."Res_Int_02" AS SKU
 FROM PIM_MASTER."ArticleRevision" ar
 JOIN PIM_MASTER."ArticleDetail" ad ON ad."ArticleRevisionID"=ar.ID
 WHERE ar."EntityID"=1100 AND ar."RevisionID"=1 AND ar."CatalogID"=1
 AND ar."DeletionTimestamp"=TIMESTAMP '9999-12-31 00:00:00.0'
 AND ad."DeletionTimestamp"=TIMESTAMP '9999-12-31 00:00:00.0'
 AND ad."CurrentStatus" <> 1025 AND ad."Res_Int_02" IS NOT NULL
), numbered AS (
 SELECT IDENTIFIER, SKU, COUNT(*) OVER (PARTITION BY SKU) AS IDENTIFIERS_PER_SKU
 FROM eligible
)
SELECT /*+ NO_PARALLEL */ IDENTIFIER,SKU,IDENTIFIERS_PER_SKU
FROM numbered WHERE IDENTIFIERS_PER_SKU>1 ORDER BY SKU,IDENTIFIER;
