# Auditoría Omega y seguimiento de CurrentStatus

Implementado y comprobado el 17 de septiembre de 2026. Última lectura operativa: 16:30, Ciudad de México. Estos contadores y PID son evidencia fechada; deben volver a consultarse para conocer el estado actual.

## Resultado

Se añadieron `CreationUserID`, `CreationTimestamp`, `ModificationUserID` y `ModificationTimestamp` a las cinco tablas de datos Omega y se completaron los campos que faltaban en sus catálogos. Los incorporadores conservan los valores nativos disponibles; las escrituras directas Omega preservan la creación y actualizan la modificación en la misma transacción.

- Producción: cinco tablas de `P360_EXPLOIT` y los 21 catálogos existentes.
- Dev: cinco tablas `PIM_MASTER.EXP_PR_*` y los 25 catálogos existentes. Los valores y clasificaciones usan los nombres reales `EXP_PR_PRO_VAL`, `EXP_PR_ART_VAL` y `EXP_PR_CLASIF`.
- Para Producto y Artículo, la auditoría raíz procede de `PIM_MASTER.ArticleRevision`, distinguiendo EntityID 1100 y 1000. El historial de estado procede de `ArticleDetail.StatusModification`.
- Se añadieron `ATTRIBUTE_TRACKING` y `ATTRIBUTE_HISTORY`, con el prefijo correspondiente a cada ambiente. El seguimiento habilitado inicialmente es `CurrentStatus` para ambas entidades.
- No se hicieron escrituras SQL sobre las tablas nativas de P360.

## Historial y atribución

Se guardan las transiciones que efectivamente aparecen en el historial nativo, con usuario, estado, fecha cuando existe y evidencia original. Se reconocieron formatos español e inglés en los registros probados. Se conserva su precisión de minuto o segundo; no se inventó una zona horaria ni una precisión mayor.

Las líneas no interpretables conservan el texto con origen `P360_UNPARSED`. Cuando no existe historial, se registra una observación del estado actual (`P360_BASELINE`) sin adjudicarle una fecha de transición. La fecha general de modificación del producto no se usa como si fuera la fecha del cambio de estado.

Las modificaciones directas Omega registran `OMEGA_WRITE` únicamente cuando cambia CurrentStatus; repetir el mismo estado no crea otra transición. La entrada de historial y el cambio de datos comparten transacción. Reprocesar historial nativo no duplica las entradas ya importadas.

En Dev, el usuario técnico vivo `rest` se resolvió a ID 8101 y se configuró como respaldo para escrituras sin actor explícito. Cuando el evento trae actor, se usa ese dato. No se presenta al usuario técnico como el usuario humano de un cambio; los nombres de usuarios históricos no se traducen arbitrariamente a un ID actual.

El seguimiento histórico nativo depende del contenido disponible en StatusModification. No reconstruye cambios que P360 no conservó. El historial normalizado nuevo vive en ATTRIBUTE_HISTORY; no se vuelve a serializar automáticamente a un texto StatusModification para las escrituras directas Omega.

## Activación y carga inicial

Producción, gcpcatpap05 (.6), lectura 16:30:

| Proceso | PID observado | Evidencia |
|---|---:|---|
| Incorporador MM | 2128614 | `CAUGHT_UP`, secuencia 49,068,690 |
| Incorporador catálogos | 2128615 | `CAUGHT_UP`, misma secuencia; conserva 31 pendientes anteriores |
| Coordinador de carga inicial de auditoría | 2128616 | Vivo; fase ROOT_1100; 17,100 propietarios recorridos desde el último arranque; checkpoint ID 372088 |

Dev, gcpcatdap03 (.165), lectura 16:30:

| Proceso | PID observado | Evidencia |
|---|---:|---|
| Incorporador nativo | 2982045 | Vivo, en espera; 8,653 recibos procesados |
| Consumidor de negocio | 2982046 | Vivo; nuevos eventos de COMPLETENESS terminando en DONE |
| Carga inicial de auditoría | 2982047 | Vivo; fase ROOT_1100; 23,400 propietarios recorridos desde el último arranque; checkpoint ID 833312 |

La carga inicial **no ha terminado**. Continúa con checkpoints después de cada commit, en lotes de 900 y sin límite total de lotes. Producción recorre raíces, catálogos y valores; Dev recorre raíces, valores y catálogos. Son procesos separados del incorporador continuo, que ya usa los cambios.

`ownersThisRun` cuenta propietarios recorridos, no filas nuevas ni transiciones. En la fase ROOT, el campo heredado `auditRowsUpdated: 0` del JSON no acumula los updates del helper: no debe usarse como contador de filas auditadas. En la fase VALUES sí registra sus actualizaciones.

La operación remota y su evidencia están en ambos ambientes en:

`/u01/workshop/java/operations/omega-audit-20260917`

Consultar `deployment.json`/`deployment-dev.json`, `verified-final.json`, `backfill/status.json` o `backfill-dev/status.json` y los respectivos logs. En producción, el coordinador reintenta una fase fallida conservando sus checkpoints. No ejecutar de nuevo los scripts locales de despliegue con PID históricos.

## Validaciones realizadas

- Compilación final de las clases modificadas: correcta.
- Diez verificaciones de interpretación e identidad del historial: correctas.
- Comparación contra P360 de ocho propietarios en producción: los cuatro campos de control coincidieron; 25 filas de historial y el mismo número tras repetir la carga.
- Comparación de veinte propietarios en Dev: correcta; 185 filas de historial y repetición sin duplicados.
- Contrato transaccional en Dev: creación de producto/artículo, cambio de estado, repetición sin cambio, preservación de creación, actor y rollback completo. No quedaron productos de prueba ni se publicaron esos cambios.
- GetProposals en Dev, propuesta 1698767482041001: HTTP 200 en 0.79 s; respuesta con el identificador solicitado, negocio Suburbia y estado Borrador.
- La carga amplia expuso fechas nulas tipadas incorrectamente en JDBC. Se corrigieron los parámetros TIMESTAMP; ambos procesos avanzaron después del arreglo. El lote de MM que quedó en reintento superó su cursor y llegó a CAUGHT_UP.
- El consumidor Dev rechazaba SELECT de completitud que empiezan con WITH. Se admitió esa lectura manteniendo el rechazo de DML; después se observaron eventos COMPLETENESS DONE.

## Código y despliegue

Fuentes y binarios compilados sincronizados en Memelos; Contingencia referencia ese proyecto mediante su classpath, no requiere una segunda copia de las clases fuente. El manifiesto local `work/omega-audit-20260917/source-sync.json` contiene los archivos y hashes.

Clases principales: OmegaAudit, OmegaAuditMaintenance, ExploitLiveProjection, OmegaEventDelta, OmegaDevNativeProjection, CatalogDelta, ProductRepository, ProductRecords, los escritores Product*, OmegaWriter, OmegaBusinessContext, OmegaCatalogWriter, OmegaCatalogProjection y OmegaCatalogSql. También se actualizaron los scripts de incorporación para transportar el actor del evento.

En producción se reiniciaron únicamente los incorporadores auxiliares afectados. En Dev se actualizaron exploit-core.jar, omega-native-catalog.jar y process-engine.war; Tomcat recargó la aplicación. No se reiniciaron nodos P360.

Se mantuvieron las sesiones CyberArk abiertas. Los respaldos de despliegue quedaron en el directorio de operaciones, fuera de las carpetas activas de JAR.
