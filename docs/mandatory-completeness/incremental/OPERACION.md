# Mandatory Completeness incremental

Clase: `mx.com.liverpool.p360.services.core.amqp.run.MandatoryCompletenessChangeProcessor`.
Se inicia y detiene con `P360ActiveMQBPMStage`. No crea queues ni consumers adicionales.

## Tablas y mantenimiento

- **`P360_EXPLOIT.TB_MANDATORY_COMP_PENDING`**: un pendiente por entidad/identificador. Conserva las características modificadas, versión, cursor y diagnóstico del último error; no almacena los mensajes completos ni un historial de ejecuciones.
- `P360_EXPLOIT.TB_MANDATORY_COMP_CONTROL`: dos filas. ID=1 contiene contador y límite; ID=2 proporciona exclusión de trabajadores mediante bloqueo Oracle en una conexión dedicada.
- El snapshot detallado sigue en `P360_EXPLOIT.TB_COMPLETENESS_SNAPSHOT`; el trabajo temporal usa `P360_EXPLOIT.TT_PRODUCT_COMPLETENESS_WORK`.

Mantenimiento automático: cada pendiente exitoso se elimina en la misma transacción que reduce el contador. La eliminación compara su versión: si llegó otro cambio durante el cálculo, el pendiente permanece. Los errores conservan una sola fila y un diagnóstico máximo de 1000 bytes; se reintentan desde 5 segundos hasta un máximo de 15 minutos, sin crear historial. Los registros temporales del trabajador nunca se confirman por separado y se revierten ante un fallo.

El límite inicial y máximo admitido es **100,000 entidades pendientes**. Las características ocupan como máximo 4000 bytes por fila; si su unión excede el espacio, se conserva una invalidación de producto completo. El contador y las admisiones están protegidos por una transacción; al alcanzar el límite, los mensajes nuevos permanecen sin confirmar en las queues existentes. Se registra una advertencia como máximo cada minuto por receptor. Esto puede frenar temporalmente ese receptor: es la contrapresión que evita crecimiento ilimitado o descartar trabajo. El límite es lógico de filas/payload; no es una cuota física del tablespace compartido.

No se purgan pendientes fallidos por antigüedad, porque implicaría perder cambios. Para adelantar un reintento después de corregir su causa:

```sql
UPDATE P360_EXPLOIT.TB_MANDATORY_COMP_PENDING
SET NEXT_ATTEMPT=SYSTIMESTAMP
WHERE ENTITY_TYPE=:entity AND ENTITY_IDENTIFIER=:identifier;
COMMIT;
```

Para bajar el límite, actualizar `MAX_PENDING` de ID=1, entre 1 y 100000. No borrar filas manualmente sin ajustar el contador en una transacción con el bloqueo de ID=1.

```sql
SELECT c.PENDING_COUNT,c.MAX_PENDING,
       (SELECT COUNT(*) FROM P360_EXPLOIT.TB_MANDATORY_COMP_PENDING) AS ACTUAL_COUNT
FROM P360_EXPLOIT.TB_MANDATORY_COMP_CONTROL c WHERE c.ID=1;
SELECT ENTITY_TYPE,COUNT(*) AS PENDING,MIN(CREATED_AT) AS OLDEST,
       SUM(CASE WHEN ATTEMPTS>0 THEN 1 ELSE 0 END) AS RETRYING
FROM P360_EXPLOIT.TB_MANDATORY_COMP_PENDING GROUP BY ENTITY_TYPE;
```

## Procesamiento

Los receptores existentes registran el pendiente antes de confirmar JMS. Los cinco processors que antes usaban AUTO_ACKNOWLEDGE conservan su contrato de confirmar antes del procesamiento legado, ahora después del commit del pendiente. El receptor principal conserva su confirmación posterior al procesamiento. Un fallo de lógica legado se registra y no termina el receptor; esto no migra ni corrige los archivos caché de Lookup/Diccionarios.

El trabajador tiene conexiones propias; resuelve padres actuales/anteriores de Article y calcula fuera del receptor EAN/SKU. La identificación de características usa las calificaciones de `_changeSummary`, incluso al borrar un valor. Consulta las características aplicables mediante los mismos CTE de template + CreateProposal + IsMandatory y Business del motor. No añade CatID ni reglas de cálculo. Cambios estructurales o calificaciones incompletas invalidan el producto entero. Los cambios exclusivos de `Product2G.MandatoryCompleteness` se ignoran para evitar ciclos.

Cambios de configuración de las plantillas soportadas, Characteristic, taxonomía primaria, sobres de borrado o un mensaje no interpretable generan una reconciliación conservadora por páginas de 100 productos. Se concentra en una sola fila `Metadata / *` con cursor; no materializa toda la base como pendientes. Cambios de diccionarios ajenos a esa configuración no disparan barridos.

Las escrituras del porcentaje usan exclusivamente `MandatoryCompletenessP360Writer` / RequestHandler / List API. No hay escrituras JDBC en PIM_MASTER. El snapshot se confirma antes de llamar a REST; si REST falla, se conserva el pendiente y se vuelve a calcular al reintentar.

## Configuración y arranque

Propiedad real en `/u01/workshop/p360_contingencyservices.properties`:

```properties
p360.completeness.incremental.enabled=true
```

Se lee al iniciar la JVM; cambiarla requiere reiniciar para que el modo de confirmación de JMS sea consistente. Valor predeterminado: false. El inicio valida las tablas antes de abrir los receptores. El trabajador verifica el campo de List API y conserva los pendientes si REST o DB no están disponibles.

Usa `/u01/Informatica/server.properties` y el archivo real de contingency. El script de reinicio fija `P360_COMPLETENESS_API_HOST=gcpcatpap02`, donde está activo el Repository que expone MandatoryCompleteness.

Mientras exista un `MandatoryCompletenessBootstrap` local, el trabajador recibe pendientes pero espera para calcular/escribir, evitando que ambos sobrescriban sus snapshots. Cuando el bootstrap termina, continúa automáticamente. El bloqueo de ID=2 impide un segundo trabajador incremental; no coordina otros bootstraps lanzados en servidores distintos. No arrancar otro bootstrap en paralelo.

Servidor: gcpcatpap06. Directorio: `/u01/workshop/java`.
Reinicio: `/u01/workshop/java/releases/mandatory-incremental-20260907/restart-listener.sh`.
El script detiene mediante `Apagalo localhost 23543`, verifica la salida y arranca con nohup usando el comando documentado en `howToRunActiveMQListener.txt`.
Log: `/u01/workshop/java/bpm_to_pubsub`; PID: `/u01/workshop/java/activemq-listener.pid`.

## Validación

Compilación Java 17 local con classpath de Eclipse y compilación con las librerías reales del servidor. SQL de negocio comparado con el anterior: idéntico al normalizar espacios. Pruebas de borrado, coalescencia, ciclos propios y XML seguro; pruebas transaccionales en Oracle de versión, cuota, rollback, eliminación y reintento. Prueba del receptor con mensaje JMS simulado: no confirma mientras Oracle está bloqueado y sólo confirma cuando otra conexión puede leer el pendiente confirmado. No se publican mensajes sintéticos al broker.
