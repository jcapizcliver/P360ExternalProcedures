# GetProposals: prioridad de talla y color

Cambio del 7 septiembre 2026 en `GetProposals.java`:

- TamanoUnico toma primero articleExtraData(MX).tamanoUnico.
- ColoursLiverpoolAtt toma primero articleExtraData(MX).coloursLiverpoolAtt.
- Si ExtraData no aporta valor, se conserva el valor procesado de la característica. Si ninguno existe, se devuelve vacío.
- Se prefiere etiqueta de lookup; si está vacía, se usa su código. Se recorren las entradas MX aunque no sean la primera del array.
- Una característica vacía/malformada no invalida una dimensión válida de ExtraData.
- Las demás propiedades conservan su precedencia anterior.

Compilación Java 17 y 11 comprobaciones sin llamadas de red, tanto locales como con las dependencias del WAR del servidor.

WAR publicado en gcpcatpap06: `/u01/workshop/tomcat/apache-tomcat-9.0.104/webapps/process-engine.war`.
Respaldo y manifiesto: `/u01/workshop/java/releases/variant-dimensions-20260907`.
Se conservó byte por byte la mitigación previa de Foro. Solo se sustituyeron clases de GetProposals y sus clases internas recompiladas.

Tomcat permanece detenido por el trabajo de compresión solicitado. El script de archivado lo iniciará con este WAR cuando termine. No se realizó verificación HTTP en vivo durante esa parada.
