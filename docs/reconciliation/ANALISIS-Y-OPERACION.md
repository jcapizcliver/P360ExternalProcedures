# Conciliación P360 y lectura de Mongo — 7 septiembre 2026

## Entregado

- `ProductDataReconciler`: motor común de complementación JSON extraído de ECC y Jana. Las diez funciones comparadas son iguales, salvo formato. Ambos parsers delegan en él; se conserva la semántica previa. No se activaron rutas de conciliación deshabilitadas ni se desplegaron los parsers modificados.
- `ReconciliationPlan`: programa independiente que recibe snapshots de productos y sus artículos y produce una propuesta JSON. No tiene conexión a P360. Completa productos y artículos coincidentes por SKU o Identifier/SupplierAID; propone conservar los artículos adicionales y señala coincidencias ambiguas. No limpia SKU/EAN ni cambia relaciones.
- `MongoProductReader`: lector Java 17 limitado a `products` y `skus`, filtros JSON, índices, límites de documentos y tiempos máximos. Exporta Extended JSON por línea para conservar tipos BSON. La URI se lee de `MONGODB_URI`, nunca se incluye en el código.
- Dependencias MongoDB 5.6.5 en `lib/mongodb` y `.classpath` de Eclipse. Compilación sin Maven en los servidores. Se usó el [driver Java oficial](https://www.mongodb.com/docs/drivers/java/sync/current/get-started/).

## Softdelete

Resultado exacto: **386,742,147** filas con DeletionTimestamp distinto del centinela activo. Ejecución exitosa entre 02:15:40 y 02:21:08 del 7 septiembre. El 36.31% es aproximado: el denominador de 1,065,259,497 filas procede de estadísticas de Oracle del 5 septiembre, no de un conteo simultáneo.

El porcentaje no indica cuánto puede purgarse. La retención predeterminada de P360 es un año: solo se eliminan físicamente filas que cumplen la antigüedad. La implementación inspeccionada recorre entidades e hijos; no es una tarea exclusiva de ArticleCharactValue. La operación de limpieza inspeccionada abre una transacción alrededor de una limpieza de raíz: no se confirmó procesamiento en lotes pequeños. Por ello no debe extrapolarse el tiempo del SELECT al de DELETE.

`cleanup-proposal.ini` propone sábados 09:30, retención 1y. Validado con el Quartz 2.3.2 instalado: zona America/Mexico_City y siguientes fechas 12, 19 y 26 septiembre a las 09:30. **No activado** mientras se confirma alcance general o se acuerda una selección por entidad. No se reinició ningún nodo. No se emitió DELETE.

Antes de activarlo: acordar retención/entidades, estimar los elegibles por antigüedad en horario apropiado, comprobar capacidad de UNDO/REDO y observar una primera ejecución acotada. Configurar un solo job lógico para el clúster; verificar registro/activación del scheduler tras aplicar preferencias. No programar cuatro purgas independientes sobre la misma base.

## Consulta de duplicados

Nodo `gcpcatpap01`, base `/u01/workshop/java`:

- SQL: `queries/duplicate_product_skus_20260907.sql`.
- CSV: `salidas/duplicate_product_skus_20260907.csv`.
- Log y resultado de ejecución: mismo nombre con `.log` y `.status`; `.pid` identifica el lanzador.
- Se inició con nohup. Un `.status` de 0 indica éxito; no consumir CSV parcial.

Devuelve una fila por Identifier/SKU y cantidad de Identifiers distintos en el grupo. Se limita al catálogo maestro 1, EntityID 1100, RevisionID 1; tanto Revision como Detail vigentes, CurrentStatus != 1025, SKU no nulo. Un CurrentStatus nulo queda excluido, tal como la condición solicitada. Para analizar artículos se prepara una consulta separada con EntityID 1000; no mezclar productos y artículos en el mismo grupo.

Se verificaron índices reales: ArticleDetail tiene IX_AD_TUNE_01 (SKU, DeletionTimestamp, ArticleRevisionID) y XAK1_ArticleDetail (ArticleRevisionID, DeletionTimestamp). Un inventario global de duplicados requiere recorrer muchos registros; no se forzó un índice por intuición ni se usó ArticleCharactValue para esta consulta.

## Lectura Mongo instalada y probada

Nodo `gcpcatpap06`, directorio `/u01/workshop/java/releases/reconciliation-analysis-20260907`.

Se copiaron clases propias y se obtuvieron los tres jars oficiales verificando SHA-256 contra los preparados localmente. El classpath del lector está aislado de los servicios. No se instaló software ni se reiniciaron servicios.

La URI está en `mongodb.uri`, permisos 0600. `read_mongo.py` la coloca en el entorno del proceso Java. El archivo no forma parte de los ZIP ni del repositorio.

Desde ese directorio:

```sh
python3 read_mongo.py products salidas/otra-muestra.jsonl
python3 read_mongo.py products salidas/otro-producto.jsonl queries/products-by-id.json 20
python3 read_mongo.py skus salidas/otras-variantes.jsonl queries/skus-by-parent.json 20
python3 read_mongo.py skus salidas/otros-indices.jsonl --indexes 100
```

Cada salida debe tener un nombre nuevo. Durante la ejecución se escribe `.partial`; solo pasa a `.jsonl` al finalizar correctamente. Un fallo deja el parcial para diagnóstico y devuelve error. La clase limita cada lectura a entre 1 y 10,000 documentos (10 por defecto), con límite de consulta de 30 segundos.

Los JSON guardados incluyen 10 muestras por colección, 18 índices de products y 28 de skus. Las lecturas adicionales por ID y padre también terminaron bien. Se comprobó `products._id = '1171180783'` con cuatro `skus.parent` correspondientes: 1171180810, 1171180798, 1171180801 y 1171180828. En esta muestra las identidades son cadenas; no deben convertirse a número sin conocer el esquema del documento.

Índices útiles: products `_id_`, `properties.material_1`; skus `_id_`, `parent`, `externalId_1`. No se infiere la correspondencia completa SKU de P360 ↔ Mongo de una muestra. Las búsquedas exactas para el producto 5016513939 y el SKU 5016513964 no encontraron documentos por `_id`; eso no prueba ausencia bajo otra clave.

`products.details[].attributes[]` contiene `_id`, `attribute`, `value` y otros metadatos. El `_id` es candidato para mapear características, sujeto a comprobar el identificador/tipo en P360. Hay atributos adicionales bajo `properties`. `products.modifiedAt` aparece como BSON Date; `skus.lastUpdated` aparece como texto, incluso vacío. La zona horaria de ese texto debe confirmarse antes de desempatar por fecha.

## Ejecutar la propuesta independiente

Entrada JSON con esta estructura (los productos y artículos admiten el envelope `_data` de la Object API):

```json
{"base":{"product":{},"articles":[]},"sources":[{"product":{},"articles":[]}]}
```

```sh
java -cp 'bin:lib/*' mx.com.liverpool.p360.services.core.sftp.ReconciliationPlan entrada.json propuesta.json
```

El programa escribe una propuesta, **no un payload que deba enviarse directamente con PUT**. Hay que recopilar todos los artículos de cada producto antes de comparar; una muestra parcial no prueba ausencia. Se registran conflictos y se conserva el valor base. La unión de arrays mantiene la conducta heredada, incluso para valores múltiples; todavía no aplica precedencia Mongo ni timestamps. Los conflictos de arrays se reportan a nivel de sección para revisión.

La API de Article confirmó `log` y `ownLog`. El plan excluye esos campos y `statusModification` de los datos aportados por donantes, también anidados, y excluye characteristics con esos códigos. Los snapshots originales retienen esos campos para trazabilidad y futura lectura de fechas. No se excluye arbitrariamente todo campo que contenga “status”, porque puede ser dato de negocio.

La lógica heredada también usa EAN o color/talla/modelo como firma. No completa el artículo duplicado antes de su ruta de limpieza. La propuesta nueva no considera esas firmas evidencia suficiente y completa el artículo cuando hay una coincidencia única por SKU/Identifier; las coincidencias múltiples quedan para revisión.

## Diseño propuesto del golden record — no implementado ni ejecutado

1. Congelar candidatos de Oracle y snapshots completos de Mongo/P360 con fecha, huella y versión. Confirmar Product2G ↔ products y Article ↔ skus, catálogo, normalización de identificadores y tipos. Los índices facilitan la consulta, no prueban equivalencia semántica.
2. Generar propuesta determinista por SKU de producto: Mongo no vacío y válido gana; si no aporta dato, usar P360. Entre candidatos P360 usar la fecha de modificación del valor/registro correspondiente. Para Detail/Domain usar ModificationTimestamp del registro concreto, no la última fecha global del producto. Fechas faltantes, empates con valores distintos o tipos incompatibles requieren revisión. No usar fecha de extracción como fecha de modificación.
3. Mantener claves de characteristic, idioma, recordKey/parentRecordKey, unidad y lookup. No mezclar idiomas ni asumir que una etiqueta es código. Los vacíos de Mongo no borran valores útiles. Definir política por campo para arrays: no aplicar una unión indiscriminada cuando Mongo debe prevalecer.
4. Proponer un nuevo Identifier con trazabilidad; crear primero un registro sin identidades únicas que aún ocupan los anteriores. Validar tipo, estructura y atributos obligatorios. No crear características nuevas sin metadatos de tipo.
5. Diseñar la tabla auxiliar `P360_EXPLOIT.PRODUCT_GOLDEN_MAPPING` (nombre propuesto): RUN_ID, CATALOG_ID, ENTITY_ID, SKU_ORIGINAL, OLD_OBJECT_ID, OLD_IDENTIFIER, GOLDEN_OBJECT_ID, GOLDEN_IDENTIFIER, MONGO_PRODUCT_ID, STATE, SNAPSHOT_HASH, CREATED_AT, UPDATED_AT. Restricción lógica única por catálogo/entidad/registro anterior, con historial separado por ejecución. Incluir mapeos de artículos y sus padres; el producto solo no basta.
6. Mantener evidencia por campo y decisión fuera de una fila de mapeo: origen, valor elegido, candidatos descartados y timestamps. Estados sugeridos: PROPOSED, APPROVED, CREATED, MERGED, VERIFIED, IDENTITIES_MOVED, LINKED, FAILED. Reintentos deben ser idempotentes y reanudar por estado sin recrear golden records.
7. Antes de escribir, releer versiones/fechas para detectar cambios concurrentes de equipos. Aplicar por APIs soportadas, validar completitud de producto y artículos y sus relaciones; no hacer UPDATE directo a tablas internas de P360.
8. Solo tras verificación y aprobación, coordinar traslado de SKU/EAN para evitar colisiones: no existe atomicidad garantizada entre Oracle auxiliar, Mongo y REST P360. Requiere pasos recuperables, snapshots y compensación. Conservar los Identifiers antiguos, persistir una referencia al golden record en un campo/relación de P360 cuyo metadato se confirme, y probar consumidores que todavía usen los IDs anteriores. No definir esa referencia como texto arbitrario sin metadatos.

No se creó tabla, producto, relación ni golden record. No se ejecutó merge, limpieza de identidades ni borrado. Este documento y los snapshots son la base para revisar el diseño antes de ejecutar.
