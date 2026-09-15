# Recuperación de relaciones y SKU — ECC / S4H

Clase SapBatchRecovery: lecturas JDBC en lotes; escritura únicamente por List API POST, con hasta 900 filas. Cada lote se vuelve a contrastar con ArticleRevision, ArticleDetail, ArticleReference y ArticleDomain activos. La validación posterior comprueba en BD cada relación/SKU enviado. Guarda solicitudes, respuestas, resultados por registro y resumen. Un error de API o verificación detiene esa campaña; para reanudar se vuelve a ejecutar sobre el mismo archivo, revalidando lo ya aplicado.

El tipo SAP viene de ArticleDomain.Res_Int_03 para Article y Res_Int_08 para Product2G; confirmado en Repository.repository. No sobrescribe padres existentes ni SKUs no vacíos. Omite tipos contradictorios, datos que cambiaron, SKU ocupados y destinos ambiguos. Para llenar un SKU vacío admite tipo SAP ausente únicamente cuando el Identifier coincide exactamente con el recibido en el XML; nunca usa esa excepción con destinos inferidos. No crea productos o artículos, ni toca características. Todas las campañas de escritura comparten un bloqueo en gcpcatpap01 y se ejecutan en segundo plano.

Fuentes:
- S4H: índice Jana existente de 980631 SKU, basado en GenericXMLproducts de SBB_122/processed.
- ECC: índice de 7392 archivos de ECC_122/processed, usando Product/@ZNPRST o Value/ZNPRST, MATNR, SATNR y ATTYP. Total final 794904 SKU.
- GenericXMLproducts20260827083022.XML tenía 41 caracteres U+001A en MAKTX. Se analizó una copia saneada, sin cambiar el original; 4928 filas fueron recuperadas. SHA256 y detalle en ecc-index/index-recovery.json.

Planes:
- Jana inicial: 447900 candidatos de relación. Primer lote: 823 escritos/verificados; 77 omitidos por tipo SAP.
- S4H: 525 candidatos de SKU; un barrido posterior de relaciones considera también los SKU recuperados.
- ECC: 2705 candidatos de SKU y 167446 candidatos de relación; colisiones separadas en complete.json y review.jsonl.

Servidor de ejecución: gcpcatpap01.
Release: /u01/workshop/java/releases/sap-recovery-20260907
Salidas: /u01/workshop/java/salidas/sap-recovery-20260907
Directorios: jana-relations, s4h-plan, s4h-skus, s4h-relations-followup, ecc-plan, ecc-skus, ecc-relations.
No ejecutar dos copias manuales fuera del lanzador run-batch.sh: ese script mantiene el bloqueo común. Se necesitan las variables Oracle del perfil, como SqlRunner. Las credenciales REST se toman de PropertiesManager y no se escriben en scripts.

Pruebas: compilación Java 17 local y servidor; trece comprobaciones de seguridad (preservación de padres y SKUs, colisiones, tipos, reejecución y formato de 900 filas); lote real Jana verificado por BD antes de lanzar el resto.

## Producto 1754611686795785

Sus cuatro artículos eran 1754611686795790, 1754611686795795, 1754611686795800 y 1754611686795810. La BD conserva sus relaciones originales a ese mismo producto. Fueron eliminados junto con esas relaciones el 2026-09-03 a las 09:23:05.380, 09:23:07.740, 09:23:10.190 y 09:23:12.690, por el usuario técnico rest (ID 2100).

access_p360.2026-09-03.log confirma cuatro DELETE a /process-engine/public/rt/EliminaVariantes?variantIds=..., todos con HTTP 200. La object API devuelve 404 para los artículos; el producto existe con estado Purchase Rejected. El caso no demuestra una pérdida de asociación en CreateProposal: hubo borrado explícito posterior. No se publica WAR por este caso ni se recrean IDs automáticamente. Una restauración requiere resolver si se debe revertir aquel borrado.

Evidencias en el release: proposal-matches.jsonl, proposal-state.csv, proposal-deletion.csv, proposal-deletion-user.csv, proposal-delete-access.json y proposal-1754611686795785-before.json. Los registros de acceso contienen la cadena de proxies; no permiten identificar por sí solos al usuario humano.
Validación final previa a las escrituras SKU: S4H READY=490 y 35 omitidos; ECC READY=2693 y 12 omitidos. Lote ECC de relaciones leído: READY=868, 22 pendientes de SKU y 10 omitidos por tipo. Jana canary=823 APPLIED_VERIFIED; el proceso completo sigue revalidando sus 447900 candidatos.

Lanzadores activos al cierre: Jana 562968; SKU S4H 566015; cadena ECC 568104; segunda pasada S4H 568105. Los tres últimos esperan la escritura previa o el bloqueo común. Se conserva el resultado real por fila; las cantidades del plan no son cantidades aplicadas.
