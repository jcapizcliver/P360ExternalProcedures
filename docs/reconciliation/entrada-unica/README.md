# Conciliacion P360 / Entrada Unica

Clase: mx.com.liverpool.p360.services.core.reconciliation.EntradaUnicaSync.
Ejecucion independiente con Java 17 y las bibliotecas de Memelos. No requiere instalar herramientas en servidores.

## Alcance y fuentes
- Base Mongo: BD_CAT_PRODUCTS; products.proposalId contra Product2G (1100), variants.variantId contra Article (1000).
- Por defecto recorre Mongo por _id con limite superior fijo, en paginas de 300. Busca los Identifier en PIM_MASTER mediante XAK2_ArticleRevision. Valida los documentos de Mongo; no inventaria todos los registros de P360 ausentes en Mongo.
- Alternativa de inventario P360 completo: propiedad Java -Dp360.sync.p360Scan=true; recorre por PK, sin OFFSET ni ordenamientos globales.
- Solo RevisionID=1 y DeletionTimestamp=9999-12-31. Los distintos EntityID se resuelven por separado.
- CurrentStatus y Res_Int_03 (PrevStatus) se convierten mediante enums REST. SKU=Res_Int_02; EAN=ArticleDetail.EAN.
- Product2G: nombre de ArticleLang.Res_Text250_01 (idioma 10), plantilla de PrimaryProductTaxonomy; direccion/seccion/grupo SAP desde ArticleDomain.
- Article: talla y color de ArticleDomain primero. SKU/EAN/talla/color tienen respaldo de caracteristicas solo cuando el campo nativo esta vacio; valores contradictorios no se eligen arbitrariamente.
- Los codigos de direccion/seccion y sus etiquetas equivalentes no generan envios falsos. SKU/EAN conservan ceros iniciales.
- **Pendiente: grupo de articulos.** Mongo itemGroup contiene una ruta de categorias; no equivale literalmente al grupo SAP. Se informa para revision y no se sobrescribe automaticamente.

## Seguridad de datos y envio
Oracle ejecuta exclusivamente SELECT. No hay escrituras SQL a P360 ni escrituras directas a Mongo. Cualquier escritura a P360 debe realizarse siempre por su API.

Sin --send solo audita. Con --send publica exclusivamente diferencias no vacias:
- post_products_topic: currentStatus/previousStatus, SKU/MainBarCode, direccion/seccion, plantilla, color/talla; variantes anidadas bajo su padre.
- idmc_put_products: nameProduct, usando el contrato de ReenvioEstatusPubSub.
- Padres ambiguos, documentos duplicados o datos P360 ambiguos no se publican.
- Un valor vacio de P360 conserva el valor de Mongo.
- Un ID de Pub/Sub prueba recepcion, no aplicacion por el consumidor.
- Cada batch guarda JSON, plan de valores y messageId. Un envio sin confirmacion aborta sin reintento automatico.
- Al terminar espera 600 segundos. Reconsulta solo los IDs enviados y comprueba tambien si P360 cambio durante la corrida.

## Ejecucion en servidor
Base: /u01/workshop/java.

Configurar la URI fuera del repositorio en ~/.config/p360/entrada-sync.env con permisos 600. Puede incluirse su carga en ~/.bash_profile. No ponerla en argumentos, logs ni commits.

~~~bash
# Auditoria
nohup bash scripts/run_entrada_unica_sync.sh \
  salidas/entrada-sync-audit-YYYYMMDD-HHMM > salidas/entrada-sync-audit-YYYYMMDD-HHMM.log 2>&1 &

# Conciliacion y verificacion posterior
nohup bash scripts/run_entrada_unica_sync.sh \
  salidas/entrada-sync-send-YYYYMMDD-HHMM --send > salidas/entrada-sync-send-YYYYMMDD-HHMM.log 2>&1 &
~~~

El directorio debe ser NUEVO. El script impide dos corridas simultaneas mediante flock y usa hasta 768 MB de heap.
--limit=600 permite pruebas acotadas. No necesita reiniciar PIM ni Tomcat.

Para repetir solo la verificacion de una corrida TERMINADA:
~~~bash
./jdk/jdk-17.0.12/bin/java \
  -Dlogback.configurationFile=releases/entrada-sync-20260908/final/logback-sync.xml \
  -cp 'releases/entrada-sync-20260908/final/classes:bin:lib/*:lib/mongodb/*:libPubSub/*' \
  mx.com.liverpool.p360.services.core.reconciliation.EntradaUnicaSync \
  salidas/DIRECTORIO_DE_LA_CORRIDA --verify-only
~~~
Reescribe unicamente los CSV de verificacion.

## Archivos y estados
- products_initial.csv / articles_initial.csv: ID, STATUS, REASON.
- products_verification.csv / articles_verification.csv: resultado posterior de los enviados.
- expected.jsonl: valores P360/Mongo, campos enviados, revision y messageId.
- batch-*.json / batch-*-names.json: mensajes.
- batch-*-planned.jsonl: plan persistido antes de enviar, incluso si se pierde la confirmacion.
- cursor-1100.json / cursor-1000.json: evidencia del avance, no reanudacion automatica.
- run.json, waiting-until.txt, complete.json y archivo externo .exit.

Estados: OK, MISMATCH, OK_PARTIAL (lo enviado coincide pero quedan campos sin modificar), SOURCE_CHANGED, SOURCE_EMPTY, MAPPING_REVIEW, MISSING_IDENTIFIER, MISSING_P360, MISSING_MONGO, MISSING_DETAIL, DUPLICATE_P360, DUPLICATE_MONGO, AMBIGUOUS_P360, AMBIGUOUS_PARENT, PENDING_VERIFY.
Exit 0 no implica que todos coincidan: revisar CSV.

## Indices y limites
Se verificaron los indices reales en PIM_MASTER. Sus nombres se citan exactamente en hints:
- ArticleRevision: XAK2_ArticleRevision y PK_ArticleRevision.
- ArticleDetail: XAK1_ArticleDetail.
- ArticleDomain: XAK1_ArticleDomain.
- ArticleLang: IX_ARTLANG_TUNE_01.
- ArticleStructureMap: XAK1_ArticleStructureMap.
- ArticleReference: XIE3_ArticleReference.
- ArticleCharactValue: XAK1_ArticleCharactValue.

El respaldo resuelve primero IDs de caracteristica; evita el join masivo de ArticleCharactValue con catalogos.
Timeout Oracle: 60s por consulta; red JDBC: 90s; Mongo: 30s consulta/45s socket; publicacion: 30s.
Mongo tiene indices por proposalId/variantId; pagina por _id. No ordena por updatedAt, sin indice en products.
No requiere external sort: cada lote es independiente. ExternalDelimitedFileSortV002 ya existe en Memelos y su SHA256 coincide con .6. LoQueFaltaPubSubPOSTArticles existe pero difiere del servidor; no fue sobreescrita.

## Dependencias y pruebas
Mongo Java Driver 5.6.5: bson, mongodb-driver-core y mongodb-driver-sync en lib/mongodb.
Eclipse .classpath incluye las tres. Otro checkout puede ejecutar scripts/entrada-sync/setup_mongo_dependencies.ps1, que verifica SHA256.

tests/reconciliation/EntradaUnicaSyncTest.java verifica etiquetas/codigos, ceros de SKU/EAN, vacios, payload de variantes, estados parciales y CSV.
Compilado con las bibliotecas reales de Eclipse. La auditoria contra las bases reales de 600 documentos termino sin errores.
