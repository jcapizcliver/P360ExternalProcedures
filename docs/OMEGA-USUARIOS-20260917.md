# Catálogo de usuarios Omega — 17-sep-2026

Carga terminada y contrastada a las 17:01 CDMX. Catálogo de referencia de identidades, compatible en nombres, tipos e IDs con los campos seleccionados de PIM_MAIN."User"; no es una copia del sistema de autenticación.

| Ambiente | Tabla | Filas | Activos no eliminados | Eliminados conservados | Diferencias contra origen |
|---|---|---:|---:|---:|---:|
| Prod | P360_EXPLOIT.CAT_USER | 199 | 192 | 7 | 0 |
| Dev | PIM_MASTER.EXP_PR_CAT_USER | 135 | 91 | 39 | 0 |

Dev incluye además cinco registros no eliminados pero inactivos. Se encontraron dos grupos de login repetido en Prod y cinco en Dev. Por eso LoginName tiene índice no único y la PK es ID: un ID histórico no se sustituye por otro usuario con el mismo login.

## Campos

Se copiaron explícitamente: ID, PartyID, PrincipalID, Identifier, LoginName, Alias, FirstName, LastName, eMail, Active, Domain, AuthenticationMode, DeletionTimestamp, DeletionUserID, LastSynchronisation, LastLoginDate y LastLoginSource.

Los nombres de columnas mantienen las mayúsculas/minúsculas del modelo nativo y se consultan entre comillas dobles. No se copiaron Password, PasswordHistory, ni permisos/grupos. Los datos son referencia para el futuro módulo de usuarios; no se habilitó autenticación, autorización ni inicio de sesión sobre esta tabla.

Campos adicionales: SourcePresent indica si el ID continúa existiendo físicamente en el origen; LoadedAt indica su incorporación al catálogo; LastSeenAt indica la última comprobación en origen. Estos timestamps no se presentan como fechas de creación/modificación del usuario. Un refresco posterior conserva usuarios retirados físicamente, con SourcePresent=0, para resolver el histórico.

## Resolver IDs

El join conserva usuarios inactivos y eliminados. No filtrar Active ni DeletionTimestamp al pintar el historial.

```sql
SELECT p.Identifier,
       p.ModificationUserID,
       u."LoginName",
       TRIM(u."FirstName" || ' ' || u."LastName") AS Nombre
FROM PIM_MASTER.EXP_PR_PRODUCTO p
LEFT JOIN PIM_MASTER.EXP_PR_CAT_USER u
  ON u."ID" = p.ModificationUserID
WHERE p.ID = 784174;
```

En Prod sustituir los nombres por P360_EXPLOIT.PRODUCTO y P360_EXPLOIT.CAT_USER. El mismo join aplica a CreationUserID y a los UserID de las tablas hijas.

```sql
SELECT h.Identifier, h.AttributeIdentifier, h.NewValue, h.ChangedAt,
       COALESCE(TO_CHAR(u."LoginName"), h.ChangedBy,
                TO_CHAR(h.ChangedByID)) AS Usuario
FROM PIM_MASTER.EXP_PR_ATTRIBUTE_HISTORY h
LEFT JOIN PIM_MASTER.EXP_PR_CAT_USER u
  ON u."ID" = h.ChangedByID
WHERE h.EntityID = 1100 AND h.OwnerID = 784174
ORDER BY h.ChangedAt;
```

Los historiales nativos que solo traen el nombre textual conservan ChangedBy. No se les adjudica un UserID por coincidencia de login. Si un ID ya había sido borrado físicamente antes de esta carga y no está en el origen, no puede recuperarse mediante este catálogo.

Prueba Dev: ID 2100 resuelve a rest, inactivo; ID 8101 resuelve a rest, activo. Se conservan ambos IDs distintos.

## Código, refresco y operación

Clase: `mx.com.liverpool.exploit.services.core.OmegaUserCatalog`, sincronizada en Memelos. Incluye carga repetible por MERGE y resolución por ID en lotes de hasta 900. Contingencia referencia Memelos; no se duplicaron fuentes ni se recargó su WAR.

Instalada en Prod (.6, gcpcatpap05) y Dev (.165, gcpcatdap03):

`/u01/workshop/java/operations/omega-users-20260917/refresh-users.sh`

La carga solicitada ya terminó. El script está preparado para refrescos posteriores; **no quedó calendarizado ni conectado aún a eventos de usuarios**. No se reiniciaron servicios. Las sesiones CyberArk se conservaron abiertas.

Evidencia remota: `refresh.log` en Dev; `quota-finish.log` en Prod. La comparación de campos contra el origen terminó en cero diferencias.

Prod encontró ORA-01536 en el primer intento. La cuota de P360_EXPLOIT en P360_EXPLOIT_DATA pasó de 483183820800 a 483200598016 bytes (+16 MiB). Se comprobó espacio libre físico superior a 50 GB antes de ampliar. No se crecieron datafiles ni se borraron registros. Esta ampliación permitió terminar este catálogo pequeño; no representa una solución de capacidad para todas las cargas Omega.
