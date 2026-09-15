# Recuperar relaciones faltantes de Jana

Servidor gcpcatpap06. Proceso preparado; la aplicación masiva no se ha ejecutado.

Script: /u01/workshop/java/releases/jana-relation-repair-20260907/run-jana-relation-repair.sh

1. `plan` toma una copia de las líneas completas del reporte existentes en ese momento y selecciona únicamente MISSING_REFERENCE. Descarta conflictos de destino para un mismo Article y evita duplicados. Los resultados posteriores del auditor requieren un plan nuevo.
2. `check` consulta de nuevo P360 y reporta READY, sin escribir relaciones. Es el modo predeterminado.
3. `apply` hace las mismas comprobaciones y escribe exclusivamente ProductReference cuando sigue faltando. Verifica la relación después de escribir.

Antes de aplicar se verifica que el XML de origen conserve tamaño/fecha, que el auditor siga resolviendo los mismos IDs, que el SKU del Article coincida, que los tipos SAP correspondan a individual o genérico/variante y que no haya padre en la última lectura. Los casos que ya estén correctos se omiten. No se reemplazan relaciones existentes, no se crean productos/artículos, no se modifican atributos ni se invocan los parsers de importación.

La lectura y la escritura de la API no forman una transacción condicional: evitar que otro proceso reasigne esos mismos artículos durante la aplicación. El script impide dos recuperadores concurrentes, pero ese bloqueo no controla otras aplicaciones P360.

Plan inicial: /u01/workshop/java/salidas/jana-relation-repair-20260907

Archivos: report-snapshot.jsonl, candidates.jsonl, plan-summary.json, check-results.jsonl, check-summary.json. Cuando se aplique se generarán apply-results.jsonl y apply-summary.json. Cada modo guarda su PID y código de salida.

Desde /u01/workshop/java, revisar todos los candidatos del plan inicial:

```sh
nohup bash releases/jana-relation-repair-20260907/run-jana-relation-repair.sh check > salidas/jana-relation-repair-20260907/check.log 2>&1 &
```

Aplicar el plan inicial solo cuando se decida ejecutar la recuperación:

```sh
nohup bash releases/jana-relation-repair-20260907/run-jana-relation-repair.sh apply > salidas/jana-relation-repair-20260907/apply.log 2>&1 &
```

Se admite un directorio de plan diferente como segundo argumento y un límite como tercero (0 = todos). Ejemplo de revisión de 10 registros:

```sh
bash releases/jana-relation-repair-20260907/run-jana-relation-repair.sh check /u01/workshop/java/salidas/jana-relation-repair-20260907 10
```

Para incorporar resultados nuevos del auditor, ejecutar `plan` con otro directorio todavía sin candidates.jsonl y usar ese mismo directorio en check/apply. Los planes existentes no se sobrescriben.

Una reejecución vuelve a verificar los candidatos: lo aplicado previamente queda ALREADY_CORRECT. Los resultados se anexan; el resumen corresponde a esa ejecución. Tras un error de comunicación o verificación, revisar el estado real y los resultados antes de reintentar. Se detiene tras tres errores consecutivos.

Validación: compilación Java 17 local y en servidor; nueve escenarios con API simulada, incluidos modo sin escrituras, destino cambiado, cambio de SKU/tipo, padre aparecido y fallo de verificación. Se preparó además una revisión real de cinco candidatos sin ejecutar apply.

Tallas y características no se incluyen porque el auditor no detecta valores faltantes. Reprocesar sus XML completos excedería esta recuperación de asociaciones y podría modificar valores actuales.

Auditor: indexó 980631 SKU en 33412 XML; un archivo de cero bytes quedó documentado en index/ignored-empty-files.json y no contiene registros que indexar. Se conservó index-errors.jsonl y se reanudó la auditoría sin modificar el XML original.
