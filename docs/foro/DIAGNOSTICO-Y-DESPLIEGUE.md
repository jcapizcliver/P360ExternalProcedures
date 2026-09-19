# Foro: consultas repetidas y AdmissionDate — 7 septiembre 2026

## Hallazgo

En gcpcatpap06, `GetProposalsForo` llama a `GetAttributeValuesForo`, que ejecuta `AgarraloONo` antes de leer los datos de respuesta. El cálculo generaba PUT para los artículos aunque los valores de negocio ya fueran iguales. Siempre generaba una nueva AdmissionDate. P360 registra la diferencia de fecha y publica un evento que PACactiveMQListener escribe completo.

Evidencia acotada, sin recorrer el log gigante:

- PACactiveMQListener.log: 492,647,794,198 bytes en la primera muestra (~492.65 GB decimales).
- Artículo S94064310: el evento cambia únicamente AdmissionDate de 03:27:03.121 a 03:27:33.005 del mismo día. La decisión seguía NO TOMAR y la razón era Tiene imagen principal.
- Dos cargas de GetAttributeValuesForo casi simultáneas: 54,283 Article cada una, registradas a las 03:26:45 y 03:26:47.
- Muestra de access_p360 de medianoche hasta aproximadamente 03:26: 4,836 llamadas a GetProposals y 26 a GetProposalsForo. No son el mismo endpoint; no se atribuye todo el tráfico a Foro.
- GetProposalsForo tenía respuestas de ~5,998,949 ms: casi 100 minutos. La configuración usa %D y [Tomcat 9 lo expresa en milisegundos](https://tomcat.apache.org/tomcat-9.0-doc/config/valve.html).
- Últimos ~2 MB de restclient-outbound-state: 5,080 GET, 4,859 PUT y 12 DELETE; una muestra, no una tasa por segundo. Cuatro hilos concentraban buena parte de la actividad.

Los X-Forwarded-For de Foro muestran proxies internos; estos logs no identifican inequívocamente al usuario o proceso original. No hay evidencia suficiente para calificarlo como ataque. Son lotes grandes, ejecuciones superpuestas y una lectura con efectos secundarios repetitivos.

## Corrección aplicada

`TakeNoTakeWriter` intercepta los cinco puntos de escritura del cálculo. Lee el estado actual del objeto, compara valores por característica/idioma y envía únicamente diferencias. Si no hay cambio, omite PUT. Una AdmissionDate existente se conserva si la decisión AssignTakeNoTake no cambia; la fecha se escribe al inicializar o cambiar esa decisión. Una modificación de razón u otro atributo sigue aplicándose sin refrescar por sí sola la fecha.

Las comparaciones reconocen texto, booleanos y códigos/etiquetas de lookup. No se usa caché de datos de negocio. Un bloqueo por objeto dentro de la JVM evita que dos escrituras de este mismo cálculo comparen simultáneamente contra el mismo estado antiguo. Si no se puede leer el objeto, se falla la operación; no se escribe a ciegas. No constituye un bloqueo distribuido contra otros nodos/escritores.

`ForoRequestGate` comparte el trabajo para JSON lógicamente idénticos (normaliza orden de claves, conserva orden de arrays) y permite como máximo dos lotes distintos simultáneos en la JVM. Las peticiones que exceden capacidad reciben 503 y Retry-After: 15. Un seguidor espera hasta 30 segundos por el resultado compartido; después puede reintentar sin cancelar al líder. Los resultados no se mantienen como caché después de terminar. El límite es por JVM.

## Validación y despliegue

- Compilado con Java 17 local y con las dependencias reales del WAR en el servidor.
- 12 comprobaciones locales: replay del artículo real sin escritura, cambio legítimo de decisión, cambio de razón conservando fecha, valores iniciales, tipos lookup/booleano, coalescencia, límite de concurrencia y recuperación tras errores.
- Verificación JVM de las cuatro clases con `-Xverify:all`.
- WAR armado desde el WAR que estaba desplegado: exactamente cuatro entradas nuevas/modificadas. El resto se comparó byte por byte. El código fuente local y la clase en producción diferían; para conservar las reglas de negocio desplegadas se redirigieron solo las cinco llamadas de persistencia de esa clase mediante una herramienta ASM de compilación. ASM no se agregó al WAR. El código fuente de Eclipse incorpora las mismas llamadas al nuevo helper.
- Clases: AgarraloONo, TakeNoTakeWriter, ForoRequestGate y GetProposalsForo.
- Instalación atómica en `/u01/workshop/tomcat/apache-tomcat-9.0.104/webapps/process-engine.war`.
- Tomcat terminó el despliegue a las 03:36:11.578, en 3,332 ms. SHA-256 de las cuatro clases desplegadas coincide con el parche preparado.
- POST de salud `{"products":[]}`: HTTP 200, `{"Responses":[]}`. No se ejecutó una carga masiva de prueba ni un PUT de prueba.
- No se reinició Tomcat ni ningún nodo P360. El PID de Tomcat se conservó: 1366154.

Directorio de entrega en gcpcatpap06: `/u01/workshop/java/releases/foro-idempotency-20260907`.

Contiene WAR nuevo, `backup/process-engine.war`, clase anterior, fuentes de los helpers/servlet, herramienta de parche, manifiesto de entradas y hashes, y verificación. Para rollback, reemplazar el WAR activo por ese respaldo de forma atómica; Tomcat volverá a recargar solo la aplicación. No se ejecutó rollback.

## Límites y pendientes

Tomcat reportó peticiones del contexto anterior todavía en ejecución durante la recarga. Estas pueden seguir con el código viejo hasta terminar; no se mataron hilos ni procesos. La corrección protege las nuevas solicitudes. No se afirma una reducción porcentual del tráfico o del log sin una ventana posterior comparable.

El archivo PAC existente no se truncó ni se rotó: sigue ocupando su espacio. Rotarlo requiere coordinar su escritor; copiar cientos de GB durante RMAN no forma parte de este ajuste. El parche reduce escrituras redundantes futuras, pero las modificaciones legítimas siguen generando eventos.

Los lotes de 54 mil artículos siguen siendo grandes aun sin escrituras redundantes. El siguiente cambio estructural sería paginar/acotar el trabajo y separar cálculo de consultas; no se cambió el contrato de tamaño de lote hoy. GetProposals mantiene su comportamiento y su protección de coalescencia existente. No se bloquearon IPs de proxies compartidos.

La limpieza general softdelete continúa pendiente del reinicio autorizado para el viernes; no se activó mediante reinicio durante este trabajo.
