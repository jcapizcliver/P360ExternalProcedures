# Jana: separar Product2G y Article en individuales

Caso: SKU 5016517048, PRODUCT_ID=754611687045246, ATTYP=00, archivo GenericXMLproducts20260907114707.XML.

P360 tenía desde el 3 de septiembre Article 1754611687045246 asociado a Product2G 1754611687045238. El 7 de septiembre a las 11:47:15 Jana creó por error Product2G 1754611687045246. Aunque había consultado el padre correcto, las escrituras posteriores de características, unidades y campos generales seguían usando el ID del artículo.

La ruta ATTYP=00 ahora resuelve ambos IDs antes de escribir. Prioriza el padre real de Article en P360, incluso si existe un producto duplicado con el mismo ID del artículo o con su SKU. Conserva el comportamiento de IDs nuevos cuando ninguna entidad existe. Rechaza resultados ambiguos, relaciones contradictorias y errores de lectura, en lugar de adivinar un destino. Todas las rutas de individuales pasan por esta resolución común; sus ramas anteriores quedan excluidas por la condición exterior.

Las escrituras Product2G usan productId; las de Article usan articleId. Se registra siempre Article → Product2G para procesarla después de crear ambos extremos. Se conserva la corrección previa de relaciones de genéricos y variantes.

Validación:
- Compilación Java 17 local con dependencias Eclipse y en gcpcatpap06 con sus librerías.
- Nueve escenarios de resolución: caso real con duplicado, PRODUCT_ID vacío, ID de producto, alta nueva con/sin ID, artículo sin relación, producto sin artículo, ambigüedad y padre inexistente.
- Resolución de solo lectura contra P360 confirmó los IDs esperados.
- Reproceso de un XML reducido exclusivamente al SKU 5016517048 usando el parser corregido y su inicialización normal. Completó correctamente.
- API posterior: Product2G 1754611687045238 recibió SKU 5016517048; Article 1754611687045246 conserva su relación original; el JSON del producto duplicado permaneció idéntico al de antes de la prueba.

El producto duplicado existente no fue eliminado ni se limpiaron sus campos; queda pendiente de depuración.

Release y respaldo: gcpcatpap06:/u01/workshop/java/releases/jana-individual-20260907. Incluye fuentes, clases, pruebas, XML de un SKU, logs y JSON antes/después. Despliegue limitado a ParseJana122Response y JanaIndividualTargets. Se solicitó action=finish y se terminó la JVM anterior cuando había regresado al reposo de main; después se inició una sola instancia. No se reiniciaron nodos P360 ni Tomcat.

Para compilar manualmente desde /u01/workshop/java, incluir src/JanaIndividualTargets.java junto con src/ParseJana122Response.java. El script existente también puede compilar el parser usando el helper ya desplegado en bin.
