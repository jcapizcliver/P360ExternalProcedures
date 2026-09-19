# GetProposalsForo sin límite adicional de concurrencia

Se eliminó el semáforo de dos peticiones independientes de ForoRequestGate y su rechazo por capacidad. Las peticiones independientes pueden ejecutarse en paralelo sin ese límite de aplicación.

Se conserva la agrupación de peticiones idénticas en curso y la normalización del JSON. Las duplicadas esperan el resultado compartido; se eliminó el timeout de 30 segundos que provocaba un rechazo adicional. La limpieza de peticiones terminadas y el manejo de errores reales siguen vigentes.

El servlet ya no tiene la rama de rechazo por capacidad ni envía Retry-After. Una interrupción real conserva el flag del hilo y se responde como error 500, no como 503. Los errores de JSON continúan con 400 y los demás errores reales con 500.

Se conserva el resto de la implementación desplegada: cálculos, idempotencia de escrituras y lectura de datos. El WAR se construye desde el archivo activo y se verifica que cambien únicamente ForoRequestGate.class y GetProposalsForo.class. Se guarda el WAR anterior antes del reemplazo atómico; Tomcat recarga la aplicación.

Pruebas: ocho acciones independientes entran simultáneamente; dos peticiones equivalentes comparten una sola ejecución incluso después de 30 segundos; los errores reales se propagan, se libera la entrada al fallar y se mantiene la validación JSON.

Release: `/u01/workshop/java/releases/foro-unlimited-20260907`.
Respaldo: `backup/process-engine.war` dentro del release.
WAR activo: `/u01/workshop/tomcat/apache-tomcat-9.0.104/webapps/process-engine.war`.

Este cambio elimina el límite agregado en ForoRequestGate, no los límites físicos o de conexiones de Tomcat, Oracle y la infraestructura.
