# Mandatory Completeness V1

Primera implementación del motor de completitud aprobado.

## Campo P360

Se escribe únicamente:

`Product2G.MandatoryCompleteness`

El Repository Manager lo persiste en `ArticleDetail.Res_BigDecimal12_01` (`BigDecimal`, escala 4). El bootstrap **no actualiza esa columna por JDBC**: usa List API mediante `RequestHandler`.

## Semántica del porcentaje

- `OK`: `present / total * 100`, a 4 decimales.
- `MISSING_FIELDS`: mismo cálculo, menor de 100.
- `NO_MANDATORY_CONFIG`: `100.0000`.
- `BYPASS_SPECIAL_PRODUCT` (`10000.......`, longitud 12): `100.0000`.
- `MISSING_TEMPLATE`: `0.0000`, `MANDATORY_SCORABLE=N`.
- `MISSING_BUSINESS`: `0.0000`, `MANDATORY_SCORABLE=N`.
- `PRODUCT_NOT_FOUND`: porcentaje `NULL`; no se escribe P360.

Se usa `0` para template/business faltante para que los dashboards P360 no los oculten por NULL, pero el snapshot conserva que no eran realmente scorables.

## Tablas

Ejecutar `sql/01_completeness_tables.sql` una sola vez.

- `TT_PRODUCT_COMPLETENESS_WORK`: staging reusable por `RUN_ID`; evita `IN` gigantes y permite lotes de 10k+.
- `TB_COMPLETENESS_SNAPSHOT`: estado actual, numerador/denominador, faltantes, versión del motor y estado de sincronización a P360. Ya reserva columnas para Vendor Center y ECommerce.

## Primera corrida: NO escribir P360

Recomendación:

```bash
java ...mx.com.liverpool.p360.services.core.completeness.MandatoryCompletenessBootstrap \
  --dry-run=true \
  --batch-size=1000 \
  --max-products=1000
```

Comparar una muestra contra:

1. Mandatory Data.
2. `Validación obligatorios por plantilla.sql`.
3. `P360_EXPLOIT.TB_COMPLETENESS_SNAPSHOT`.

## Inicialización real

Después de validar la muestra:

```bash
java ...mx.com.liverpool.p360.services.core.completeness.MandatoryCompletenessBootstrap \
  --dry-run=false \
  --batch-size=10000 \
  --rest-batch-size=1000
```

El paginado de Product2G es por `ArticleRevision.ID`, no por OFFSET. La consulta de IDs se cierra antes de calcular/escribir cada lote.

## Reanudación manual

El log imprime `lastRevisionId`. Para reanudar:

```bash
--start-after-revision-id=<ultimo_id_confirmado>
```

El proceso es idempotente: tanto el snapshot (`MERGE`) como el campo P360 pueden recalcularse y sobrescribirse con el valor actual.

## Concurrencia / transacciones

El bootstrap hace `COMMIT` del snapshot y elimina la tabla de trabajo **antes** de la llamada REST. No mantiene una transacción Oracle abierta durante el POST.

## Próxima fase

El listener ActiveMQ no forma parte de esta V1. Debe limitarse a detectar qué Product2G recalcular y llamar a la misma frontera:

```java
CompletenessResult calculate(String productIdentifier)
```

No debe mantener porcentajes por `+1/-1`; siempre se recalcula el producto completo.

## Integración operativa 2026-09-07

Requiere Java 17 y las dependencias del proyecto Eclipse Memelos. Las clases se
agregan bajo src/mx/com/liverpool/p360/services/core/completeness.

Antes de paginar, el bootstrap exige los archivos externos reales:
- P360_SERVER_PROPERTIES (default /u01/Informatica/server.properties).
- EXTERNAL_CONFIG_PATH (default /u01/workshop/p360_contingencyservices.properties).

El preflight comprueba propiedades obligatorias sin imprimir secretos, columnas,
permisos DML de las tablas auxiliares mediante sentencias que no afectan filas,
la consulta completa con un RUN_ID vacío y la lectura del campo por List API.
La lectura API no demuestra el permiso de escritura; la primera escritura real
confirma ese permiso y se detiene al primer sublote fallido por defecto.

Ejecutar sql/03_preflight.sql antes del DDL: no volver a crear tablas existentes.
El DDL requiere los tablespaces P360_EXPLOIT_DATA y P360_EXPLOIT_INDEX.
Usar SqlRunner con la configuración real y ejecutar sql/04_validate_snapshot.sql
para revisar el snapshot. No se modifica ArticleDetail por JDBC.

Los argumentos desconocidos y booleanos incorrectos son errores. El cálculo
exige correspondencia exacta entre productos solicitados y resultados antes del
MERGE. Los mensajes batch con lastRevisionId son checkpoints confirmados.
Ante un fallo, reanudar desde el último checkpoint confirmado, lo que recalcula
el lote interrumpido. Evitar bootstraps simultáneos sobre los mismos productos:
el snapshot es estado actual y el RUN_ID no es un bloqueo de concurrencia.

La consulta y las reglas de Mandatory del ZIP se conservan sin modificaciones.

La comprobación del campo por List API es obligatoria sólo en modo real. El dry-run permite validar el cálculo y el snapshot mientras el campo aún no está activo en P360.

En PROD se verificó el índice XIF3_ArticleRevision (EntityID, ID). La paginación
solicita ese índice para evitar el timeout de la consulta original.
P360_COMPLETENESS_API_HOST permite seleccionar otro nodo del mismo entorno;
conserva protocolo, puerto, ruta y credenciales de la configuración real.
Para esta ejecución: bootstrap en gcpcatpap06, API en gcpcatpap02, donde el
Repository actualizado ya está activo. No requiere reiniciar gcpcatpap06.

Se ajustó también el orden de acceso de metadata para resolver primero las plantillas y CreateProposal y aprovechar IX_LVREV_METADATA_EXT_01. Se verificó que, quitando los hints del optimizador, MandatoryCompletenessService es idéntico al ZIP original.
