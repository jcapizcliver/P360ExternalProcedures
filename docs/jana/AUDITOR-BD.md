# Auditor Jana por lotes JDBC — reemplazo del barrido REST

Ejecuta en gcpcatpap01 con las variables ORACLE_JDBC_URL, ORACLE_JDBC_USER y ORACLE_JDBC_PASSWORD del perfil existente, como SqlRunner. No incluye credenciales en fuentes ni archivos auxiliares.

Reutiliza el índice de 980631 SKU de gcpcatpap06; no vuelve a leer los 33412 XML. Recoge SKU, SATNR e identificadores de los registros ATTYP=00/02. Consulta lotes de hasta 500 claves con fetchSize=2000, una conexión y una transacción Oracle READ ONLY. No ejecuta REST durante el barrido.

Consultas basadas en DBAccessDataStub:
- ArticleDetail.Res_Int_02 → ArticleRevision: índice IX_AD_TUNE_01.
- ArticleRevision.Identifier: XAK2_ArticleRevision.
- ArticleReference.ArticleRevisionID: XIE3_ArticleReference.
- Referencias inversas de productos recibidos por Identifier: IX_ARTREF_TUNE_01.
- Padre por ArticleID: XAK1_ArticleRevision.

Se verificó la existencia y las columnas de estos índices en all_ind_columns. Filtros: catálogo 1, revisión 1, EntityID 1000/1100, DeletionTimestamp activo; Detail activo y CurrentStatus distinto de 1025 o nulo. Referencias activas enlazadas por RefIntArtID/ArticleID y RefExtArtIdentifier/Identifier a Product2G activo. No toca ArticleCharactValue ni crea tablas, índices o datos P360.

Carga artículos hijos de productos recibidos por Identifier, aunque esos hijos no aparezcan en el XML. También carga padres existentes sin SKU. La comparación de individuales reutiliza JanaIndividualTargets; para variantes contrasta SATNR. Conserva los ambiguos, no elige arbitrariamente entre duplicados. Los tipos SAP se vuelven a verificar mediante REST solo en la recuperación.

Validación previa a la ejecución completa: compilación Java 17 local y servidor; 21 casos con resultados idénticos al reporte REST (5 faltantes, 5 correctos, 5 ambiguos, 5 padres distintos, 1 producto faltante), más 3 pruebas del individual 5016517048 recibiendo ID del Article, ID del Product2G y sin ID. Las tres conservaron Product2G 1754611687045238 / Article 1754611687045246.

Release en ambos servidores: /u01/workshop/java/releases/jana-db-audit-20260907.
Lanzador en gcpcatpap01: run-jana-db-audit.sh, bajo nohup y prioridad reducida.
Resultados y avance en gcpcatpap01: /u01/workshop/java/salidas/jana-relations-db-20260907/{audit.log,run.pid,exit.status,summary.json,relations.jsonl}.
Al terminar correctamente publica relations.jsonl y summary.json en esa misma ruta de gcpcatpap06. El reporte solo se publica completo; el parcial no debe usarse para cargar relaciones.

El auditor REST anterior fue detenido; su índice y resultados se conservan en /u01/workshop/java/salidas/jana-relations-20260907. Ningún parser Jana/ECC, Tomcat ni nodo P360 fue reiniciado.

El recuperador sigue siendo independiente y no se ejecuta automáticamente. Para generar un plan nuevo a partir del reporte completo por BD, en gcpcatpap06:

```sh
cd /u01/workshop/java
JANA_RELATION_REPORT=/u01/workshop/java/salidas/jana-relations-db-20260907/relations.jsonl \
  bash releases/jana-relation-repair-20260907/run-jana-relation-repair.sh plan \
  /u01/workshop/java/salidas/jana-relation-repair-db-20260907
```

Después revisar con check usando ese directorio. apply requiere una ejecución explícita. No se reparan WRONG_PARENT ni AMBIGUOUS: se seleccionan únicamente MISSING_REFERENCE y se vuelven a comprobar en P360 antes y después de escribir.

## Resultado completo del 7 de septiembre

Ejecución definitiva en gcpcatpap01: 20:33:40 a 20:34:33, hora del servidor (53 segundos). Total: 980631 registros; MISSING_REFERENCE=447900, OK=402528, GENERIC_NO_RELATION_REQUIRED=117412, AMBIGUOUS=4878, WRONG_PARENT=3630, MISSING_PRODUCT=3678, MISSING_ARTICLE=486, ARTICLE_SKU_CONFLICT=119.

Se corrigieron los hints con nombres de índice de mayúsculas y minúsculas entre comillas dobles. Sin ellas Oracle ignoraba XAK2_ArticleRevision y elegía IDX$$_73E00002, que empieza por DeletionTimestamp y no por Identifier. El primer intento JDBC quedó preservado en salidas/jana-relations-db-20260907-initial-index-plan.

El reporte completo ya fue copiado a gcpcatpap06. La primera publicación automática falló al verificar la clave SSH del destino; se recuperó mediante scp desde gcpcatpap06. El lanzador actualizado usa un known_hosts del release con las claves públicas leídas directamente del servidor destino mediante la sesión autenticada. Se comprobó la conexión sin contraseña y con verificación estricta. El exit.status original conserva el fallo de aquella publicación; consultar completion.json para la recuperación final.

Plan completo: /u01/workshop/java/salidas/jana-relation-repair-db-20260907/candidates.jsonl, con 447900 artículos únicos y cero destinos contradictorios. No se ha ejecutado apply. El plan no cambia padres existentes, ni crea productos o artículos, ni carga características o tallas.
Comprobación del plan completo: check de los primeros 10 candidatos, READY=10, salida 0. Plan de ejecución Oracle verificado en v$sql_plan: XAK2_ArticleRevision para Identifier; XIE3_ArticleReference y XAK1_ArticleRevision para referencias. Evidencia en releases/jana-db-audit-20260907/verified-query-plan.csv.
