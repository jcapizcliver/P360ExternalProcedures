# Análisis de productos con SKU duplicado

Se conectó el CSV de 22,683 Identifiers / 11,337 SKU duplicados con `ReconciliationPlan` y `ProductDataReconciler`. Es una ejecución de **solo análisis**: no crea registros, no escribe relaciones, no quita SKU/EAN y no aplica merges.

## Ejecución desplegada

Release en ambos servidores: `/u01/workshop/java/releases/duplicate-analysis-20260907`.

| Etapa | Servidor | Proceso inicial | Evidencia |
|---|---|---:|---|
| Snapshot Oracle y propuestas | gcpcatpap01 | 588870 | `db.log`, `db.exit`, `db-deployment.json` |
| Candidatos Mongo | gcpcatpap06 | 2291759 | `mongo.log`, `mongo.exit`, `mongo-deployment.json` |
| Copia e índice final | gcpcatpap06 | 2306461 | `review.log`, `review.exit`, `review-deployment.json` |

Los PID son históricos; confirmar el estado mediante los archivos `.exit` y `complete.json`. Un `.exit` con 0 indica terminación satisfactoria. La ausencia de `.exit` por sí sola no demuestra que un proceso esté vivo.

Mongo terminó con 11,337 grupos: 8,304 con candidatos de producto y 10,895 con candidatos de SKU. El recolector P360 quedó activo, con 2,200 grupos analizados y ninguna incidencia en el corte de 2026-09-07 22:16, hora de México. El índice final depende de que ambas etapas terminen bien; no interpreta un fallo como datos vacíos.

## Resultados

- Oracle: `/u01/workshop/java/salidas/duplicate-analysis-20260907` en gcpcatpap01. Al finalizar se copia completo a la misma ruta en gcpcatpap06.
- Mongo: `/u01/workshop/java/salidas/duplicate-mongo-20260907` en gcpcatpap06.
- Revisión final: `/u01/workshop/java/salidas/duplicate-review-20260907` en gcpcatpap06. Se genera cuando termina Oracle; buscar `complete.json`, `RESUMEN.md` y `review-index.csv`.
- Cada grupo tiene `<SKU>.json.gz`. El JSON de revisión referencia las evidencias P360/Mongo y lista campos complementables, asociaciones propuestas, ambigüedades y conflictos por registro base.
- Existe una prueba completa de tres grupos en `releases/duplicate-analysis-20260907/review-pilot` de gcpcatpap06.

El CSV original se copia a cada salida. Su SHA-256 es `43802a95c7ecb944c049e194f12af8bbcddcf5b45b57775f3e61b6a290799397`. El constructor del índice exige que las huellas y cantidades coincidan.

## Alcance de la comparación

`DuplicateGroupAnalysis` lee 100 grupos por transacción Oracle de solo lectura. Usa consultas separadas e índices sobre identificadores, referencias y revision IDs; consulta como máximo 500 revisiones por bloque. Evita un join masivo entre características, idiomas y dominios. Cada consulta tiene un máximo de 120 segundos y lectura JDBC de 180 segundos.

Captura filas activas de ArticleRevision, ArticleDetail, ArticleLang, ArticleDomain, ArticleStructureMap, ArticleReference, ArticleCharactValue y ArticleCharactValueLang. Descubre todos los artículos activos relacionados mediante RefIntArtID **y** RefExtArtIdentifier, incluidos aquellos cuyo Identifier coincide con el del producto. Excluye del análisis artículos con CurrentStatus 1025 y bloquea grupos con identidad ambigua, Detail ambiguo, metadatos de característica ausentes o SKU de producto cambiado desde el CSV. Las filas crudas conservan fechas y usuarios; no se deben utilizar como payload de API.

Genera una alternativa por cada posible producto base. Mantiene la lógica existente: completar vacíos y conservar el valor base ante conflicto. No elige un ganador. Conserva calificadores de idioma, dominio, característica, recordKey y parentRecordKey. Excluye del complemento las identidades, EAN/SKU, metadatos técnicos y StatusModification/ownLog/log. Las referencias crudas se conservan como evidencia; las propuestas de asociación provienen del análisis de artículos por SKU/Identifier. Las alternativas son mutuamente excluyentes y no deben ejecutarse juntas.

La captura no equivale a toda la Object API: no incluye todas las tablas relacionadas del repositorio. Los valores de lookup se conservan como IDs de la misma base; no se transforman en etiquetas ni se inventan tipos.

`MongoDuplicateEvidence` usa índices por nombre: products `_id_`, `properties.material_1`; skus `_id_`, `parent`, `externalId_1`. El índice `parent` se elige por nombre para evitar ambigüedad con el índice parcial del mismo campo. Consulta IDs y SKU como cadenas y, cuando procede, como enteros; conserva los tipos BSON originales en Extended JSON. No escanea colecciones completas. Cada consulta tiene límite de 30 segundos y los documentos se guardan comprimidos por grupo. Cero coincidencias no demuestra ausencia bajo otra identidad. Las lecturas Mongo no son una transacción única.

**Pendiente de diseño/validación:** correspondencia de identidades y características de Mongo con P360, prioridad Mongo > P360, desempate mediante fechas del valor/Detail/Domain, creación del golden record, tabla de trazabilidad y eventual traslado de SKU/EAN. Nada de eso se ejecuta con este análisis.

## Validaciones

- Compilación Java 17 de clases nuevas, helper compartido y ambos parsers; 17 verificaciones previas de conciliación y las nuevas pruebas de campos protegidos, logs, complementos, conflictos, inmutabilidad, ambigüedad y CSV incompleto.
- Piloto Oracle: tres grupos, seis productos, 38 artículos; 1.6 segundos, sin incidencias.
- Piloto Mongo y constructor de revisión: tres grupos completos y seis alternativas.
- Canary Object API del artículo S52271625: SKU 4051807, padre S52271625 y 29 códigos de características coinciden con el snapshot Oracle, contando características anidadas. Evidencia: `canary-validation.json` y `canary-object.json` en el release de gcpcatpap06. Esto valida ese caso, no toda la equivalencia semántica del repositorio.

Fuentes en `src/mx/com/liverpool/p360/services/core/sftp/{DuplicateGroupAnalysis,MongoDuplicateEvidence}.java`; scripts en `scripts/reconciliation`. Pruebas: `tests/run-reconciliation-tests.ps1`. El despliegue usa clases aisladas en el release y la conexión Oracle del perfil existente; Mongo lee el archivo protegido ya disponible en el release anterior. No contiene credenciales en las fuentes.

No se reiniciaron servicios ni se sustituyeron los parsers de producción durante esta ejecución. Las campañas de recuperación SAP son procesos separados.
