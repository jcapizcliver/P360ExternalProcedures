# Auditor de relaciones Jana

Auditor de solo lectura en gcpcatpap06. No ejecuta los parsers de importación, no cambia asociaciones y no dispara exportaciones.

Recorre GenericXMLproducts*.XML (extensión sin distinción de mayúsculas), selecciona el archivo más reciente por fecha del nombre para cada MATNR y guarda un índice JSONL. Si encuentra XML inválidos, registra los errores y detiene el paso a consultas P360 para no auditar silenciosamente información anterior.

Audita ATTYP=00 y ATTYP=02. Los genéricos ATTYP=01 se reportan como no sujetos a relación. Consulta P360 secuencialmente con medio segundo entre registros y se detiene después de cinco fallos consecutivos. No es una fotografía transaccional: cada resultado incluye la hora de consulta.

En individuales, un padre existente en P360 tiene prioridad; OK indica que existe la relación que resolvería el parser, no que se haya demostrado que ese padre es correcto frente a otra fuente maestra. En variantes se contrasta contra el producto cuyo SKU es SATNR. Resultados múltiples se reportan como ambiguos.

Archivos en /u01/workshop/java/salidas/jana-relations-20260907:

- audit.log: avance de indexación y auditoría.
- index/index-summary.json: XML leídos, SKU distintos y errores.
- index/latest-products.jsonl: último registro XML de cada SKU.
- index/index-errors.jsonl: XML que requieren revisión.
- relations.jsonl: una línea JSON por resultado, con SKU, XML, IDs y estado.
- summary.json: conteo acumulado por estado.
- run.pid y exit.status: proceso y código final (0 completado, distinto de 0 detenido por error).

Estados: OK, MISSING_REFERENCE, WRONG_PARENT, MISSING_PRODUCT, MISSING_ARTICLE, MISSING_PARENT_SKU_IN_XML, ARTICLE_SKU_CONFLICT, AMBIGUOUS, UNKNOWN_ATTYP, ERROR y GENERIC_NO_RELATION_REQUIRED.

Lanzamiento/reanudación desde /u01/workshop/java:

```sh
nohup bash releases/jana-audit-20260907/run-jana-audit.sh >> salidas/jana-relations-20260907/audit.log 2>&1 &
```

La reanudación omite resultados ya registrados para el mismo SKU/XML/fecha del archivo. Para volver a evaluar errores o capturar XML nuevos, usar un directorio nuevo de resultados y los modos index/audit de la clase. Si el equipo se interrumpió justo al escribir una línea, una última línea incompleta del reporte requiere revisión antes de reanudar; el auditor no elimina evidencia automáticamente.

Despliegue aislado: /u01/workshop/java/releases/jana-audit-20260907/classes. Usa las librerías del servidor y las funciones de resolución de la versión corregida de ParseJana122Response; si cambian esas funciones privadas, recompilar/actualizar el auditor.

No se reprocesan todos los XML históricos. Después de revisar el reporte, seleccionar casos concretos. La importación de productos debe preceder a atributos y ambos XML deben filtrarse a los SKU seleccionados. El XML de atributos más reciente puede ser distinto del que comparte fecha con productos. El auditor actual no valida valores de características ni reconstruye esa historia de atributos.
