# Limpieza mensual: configuración aplicada

Autorizada retención general de siete días. Aplicadas y verificadas las mismas dos opciones en `/u01/Informatica/PIM/server/configuration/HPM/plugin_customization.ini` de gcpcatpap01, 02, 05 y 06:

```properties
com.heiler.ppm.softdelete.cleanup.server/cleanup.lifeTime.default = 7d
com.heiler.ppm.softdelete.cleanup.server/cleanup.job.repeatPattern = 0 30 9 ? * SAT#2
```

Segundo sábado de cada mes a las 09:30, America/Mexico_City: 12 septiembre, 10 octubre y 14 noviembre 2026. No hay overrides activos de retención por entidad en estos archivos.

Los archivos completos diferían antes del cambio; se conservaron las otras opciones de cada nodo. Respaldos con permisos 0600, en cada servidor:

- 01/02: `/u01/workshop/java/releases/softdelete-monthly-20260907-031847/plugin_customization.ini`.
- 05/06: `/u01/workshop/java/releases/softdelete-monthly-20260907-031914/plugin_customization.ini`.

El componente SoftdeleteCleanupJobScheduleInfo lee la preferencia al crear la programación. La escritura del archivo no demuestra recarga en una JVM ya iniciada. Pendiente reinicio/recarga y comprobación del siguiente disparo de CleanUpSoftdelete en P360. No se reiniciaron servicios ni se lanzó limpieza manual.

Este documento sustituye los valores de retención y periodicidad de la propuesta inicial. El alcance es general para datos softdeleteados que cumplan la antigüedad, no solo ArticleCharactValue.
