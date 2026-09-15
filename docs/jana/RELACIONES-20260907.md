# Asociación Jana — 7 septiembre 2026

Caso reportado: SKU 5016514609; archivos GenericXMLproducts20260907090032.XML y GenericXMLattributes20260907090032.XML.

El XML de productos indica ATTYP=01 para 5016514609 y ATTYP=02 para 5016514618, con SATNR=5016514609. Este ejemplo no es un individual ATTYP=00.

El log parseJana122Response-0.log, 09:00:37, muestra que se resolvió el padre SBB5016514609, pero el bloque de relaciones pendientes envió el SKU 5016514609 como ProductReference. P360 rechazó el producto inexistente. La object API confirmó que Article SBB5016514618 existía sin higherLevelProduct.

Corrección: vaciar las escrituras SKU pendientes de Product2G y Article antes de resolver/escribir relaciones; resolver el SKU padre como Identifier mediante la consulta existente con fallback a P360; usar ese Identifier tanto en qualification como en values; omitir y registrar padres sin resolver. La creación previa también protege asociaciones de individuales nuevos que ya se encuentren en el mapa de relaciones. No se cambió la selección de registros de las otras ramas de individuales, que requieren otro caso de muestra.

Validación: compilación Java 17 con dependencias locales de Eclipse y con las del servidor. Prueba del bloque de relaciones extraído del fuente: Identifier resuelto, creación antes de consulta, asociación individual con IDs distintos, padre no resuelto, lotes de más de 1000 filas y ausencia de duplicados.

Reparación puntual por list API: Article SBB5016514618 → Product2G SBB5016514609; respuesta: 0 errores, 1 actualización. Lectura posterior de object API confirmó higherLevelProduct.referencedIdentifier=SBB5016514609.

Release en gcpcatpap06: /u01/workshop/java/releases/jana-relationships-20260907. Contiene fuente corregido, clases, respaldo y evidencia antes/después de la API. Se compiló sobre el fuente de producción para conservar su lógica de conciliación; el fuente de Eclipse conserva además la extracción ProductDataReconciler que ya estaba pendiente localmente.

Reinicio limitado a ParseJana122Response mediante su puerto de control 23545 (action=finish); no reiniciar P360 ni Tomcat. No se reprocesó todo el XML ni se modificaron otras asociaciones.
