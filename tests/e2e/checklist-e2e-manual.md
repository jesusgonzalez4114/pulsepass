# Checklist de ejecución manual — PulsePass E2E

Marcar cada paso al terminar. Enviar **individualmente con Send** (sin Collection Runner). Un `2xx` no basta: revisar el cuerpo y, tras mutaciones, reconsultar.

## Orden de ejecución

📸 = ejecución que se fotografía (ver «Evidencia fotográfica» al final). Además, **todo FAIL, BLOCKED o 500** se fotografía.

Flujos del PRD: **A** descubrimiento público · **B** registro y JWT · **C** catálogo (ADMIN) · **D** tickets · **E** regresión de errores.

| ☐ | Orden | Foto | Caso | Actor | Petición | HTTP esperado |
|---|---|---|---|---|---|---|
| ☐ | S001 |  | PREP-01 | ANONYMOUS | `GET /api/venues/active` | 200 |
| ☐ | S002 |  | E2E-VEN-02 | ANONYMOUS | `GET /api/venues/active` | 200 |
| ☐ | S003 |  | E2E-VEN-01 | ANONYMOUS | `GET /api/venues/{{venueActiveCode}}` | 200 |
| ☐ | S004 |  | NEG-VEN-01 | ANONYMOUS | `GET /api/venues/NO-EXISTE-{{runId}}` | 404 |
| ☐ | S005 |  | E2E-ART-03 | ANONYMOUS | `GET /api/artists/active` | 200 |
| ☐ | S006 |  | E2E-ART-01 | ANONYMOUS | `GET /api/artists/{{artistId}}` | 200 |
| ☐ | S007 |  | E2E-ART-02 | ANONYMOUS | `GET /api/artists/by-stage-name` | 200 |
| ☐ | S008 |  | NEG-ART-01 | ANONYMOUS | `GET /api/artists/999999` | 404 |
| ☐ | S009 |  | NEG-ART-02 | ANONYMOUS | `GET /api/artists/by-stage-name` | 404 |
| ☐ | S010 |  | E2E-EVT-03 | ANONYMOUS | `GET /api/events/published` | 200 |
| ☐ | S011 |  | E2E-EVT-02 | ANONYMOUS | `GET /api/events/{{publishedEventCode}}` | 200 |
| ☐ | S012 | 📸 | TC-SEC-004 | ANONYMOUS | `GET /api/events/{{draftEventCode}}` | 404 |
| ☐ | S013 |  | E2E-EVT-06 | ANONYMOUS | `GET /api/events/by-artist` | 200 |
| ☐ | S014 | 📸 | TC-SEC-005 | ANONYMOUS | `GET /api/events/by-artist` | 200 |
| ☐ | S015 |  | TC-AUTH-015 | ANONYMOUS | `GET /api/venues/active` | 200 |
| ☐ | S016 | 📸 | E2E-USR-01 | ANONYMOUS | `POST /api/users` | 201 |
| ☐ | S017 |  | TC-BIZ-006 | ANONYMOUS | `POST /api/users` | 201 |
| ☐ | S018 |  | NEG-USR-409 | ANONYMOUS | `POST /api/users` | 409 |
| ☐ | S019 |  | NEG-USR-400 | ANONYMOUS | `POST /api/users` | 400 |
| ☐ | S020 |  | NEG-USR-PW | ANONYMOUS | `POST /api/users` | 400 |
| ☐ | S021 | 📸 | TC-SEC-016 | ANONYMOUS | `POST /api/users` | 201 |
| ☐ | S022 |  | E2E-AUTH-01 | ANONYMOUS | `POST /api/auth/login` | 200 |
| ☐ | S023 |  | TC-AUTH-003 | ANONYMOUS | `POST /api/auth/login` | 200 |
| ☐ | S024 |  | LOGIN-B | ANONYMOUS | `POST /api/auth/login` | 200 |
| ☐ | S025 |  | LOGIN-STAFF | ANONYMOUS | `POST /api/auth/login` | 200 |
| ☐ | S026 |  | LOGIN-ADMIN | ANONYMOUS | `POST /api/auth/login` | 200 |
| ☐ | S027 |  | LOGIN-INJ | ANONYMOUS | `POST /api/auth/login` | 200 |
| ☐ | S028 | 📸 | E2E-AUTH-02 | USER_A | `GET /api/auth/me` | 200 |
| ☐ | S029 |  | ME-B | USER_B | `GET /api/auth/me` | 200 |
| ☐ | S030 |  | ME-STAFF | STAFF | `GET /api/auth/me` | 200 |
| ☐ | S031 |  | ME-ADMIN | ADMIN | `GET /api/auth/me` | 200 |
| ☐ | S032 | 📸 | ME-INJ | USER (registro con role inyectado) | `GET /api/auth/me` | 200 |
| ☐ | S033 | 📸 | TC-AUTH-004 | ANONYMOUS | `POST /api/auth/login` | 401 |
| ☐ | S034 | 📸 | TC-AUTH-005 | ANONYMOUS | `POST /api/auth/login` | 401 |
| ☐ | S035 | 📸 | TC-AUTH-007 | ANONYMOUS | `GET /api/auth/me` | 401 |
| ☐ | S036 |  | TC-AUTH-008 | ANONYMOUS | `GET /api/auth/me` | 401 |
| ☐ | S037 | 📸 | TC-AUTH-009 | USER_A (token alterado) | `GET /api/auth/me` | 401 |
| ☐ | S038 |  | TC-AUTH-011 | token de otra clave | `GET /api/auth/me` | 401 |
| ☐ | S039 |  | TC-AUTH-012 | ANONYMOUS | `GET /api/auth/me` | 401 |
| ☐ | S040 |  | TC-AUTH-013a | ANONYMOUS | `POST /api/auth/login` | 200 |
| ☐ | S041 | 📸 | TC-AUTH-013b | INACTIVE_USER | `GET /api/auth/me` | 401 |
| ☐ | S042 |  | TC-AUTH-013c | ANONYMOUS | `POST /api/auth/login` | 401 |
| ☐ | S043 |  | E2E-USR-02 | USER_A | `GET /api/users/by-email` | 200 |
| ☐ | S044 |  | E2E-USR-03 | USER_A | `GET /api/users/by-username` | 200 |
| ☐ | S045 |  | USR-ADMIN | ADMIN | `GET /api/users/by-email` | 200 |
| ☐ | S046 | 📸 | TC-SEC-006 | USER_B | `GET /api/users/by-email` | 403 |
| ☐ | S047 |  | TC-SEC-007 | USER_B | `GET /api/users/by-username` | 403 |
| ☐ | S048 |  | NEG-USR-401 | ANONYMOUS | `GET /api/users/by-email` | 401 |
| ☐ | S049 |  | NEG-USR-404 | ADMIN | `GET /api/users/by-email` | 404 |
| ☐ | S050 |  | TC-SEC-001a | ANONYMOUS | `POST /api/events` | 401 |
| ☐ | S051 | 📸 | TC-SEC-001b | USER_A | `POST /api/events` | 403 |
| ☐ | S052 |  | TC-SEC-001c | STAFF | `POST /api/events` | 403 |
| ☐ | S053 | 📸 | E2E-EVT-01 | ADMIN | `POST /api/events` | 201 |
| ☐ | S054 |  | EVT-02-ADMIN | ADMIN | `GET /api/events/{{newEventCode}}` | 200 |
| ☐ | S055 |  | TC-SEC-015 | ADMIN | `GET /api/events/{{draftEventCode}}` | 200 |
| ☐ | S056 |  | EVT-02-STAFF | STAFF | `GET /api/events/{{draftEventCode}}` | 200 |
| ☐ | S057 | 📸 | EVT-02-ANON | ANONYMOUS | `GET /api/events/{{newEventCode}}` | 404 |
| ☐ | S058 |  | TC-SEC-003 | USER_A | `POST /api/events/{{newEventCode}}/artists/{{artistId}}` | 403 |
| ☐ | S059 |  | E2E-EVT-05 | ADMIN | `POST /api/events/{{newEventCode}}/artists/{{artistId}}` | 200 |
| ☐ | S060 |  | EVT-05-DUP | ADMIN | `POST /api/events/{{newEventCode}}/artists/{{artistId}}` | 409 |
| ☐ | S061 |  | EVT-05-404 | ADMIN | `POST /api/events/{{newEventCode}}/artists/999999` | 404 |
| ☐ | S062 |  | TC-SEC-002a | USER_A | `PATCH /api/events/{{eventToPublishCode}}/publish` | 403 |
| ☐ | S063 | 📸 | TC-SEC-002b | STAFF | `PATCH /api/events/{{eventToPublishCode}}/publish` | 403 |
| ☐ | S064 | 📸 | TC-SEC-002c | ADMIN | `GET /api/events/{{eventToPublishCode}}` | 200 |
| ☐ | S065 | 📸 | E2E-EVT-04 | ADMIN | `PATCH /api/events/{{newEventCode}}/publish` | 200 |
| ☐ | S066 |  | EVT-04-DUP | ADMIN | `PATCH /api/events/{{newEventCode}}/publish` | 409 |
| ☐ | S067 | 📸 | EVT-02-PUB | ANONYMOUS | `GET /api/events/{{newEventCode}}` | 200 |
| ☐ | S068 |  | EVT-03-NEW | ANONYMOUS | `GET /api/events/published` | 200 |
| ☐ | S069 |  | EVT-06-NEW | ANONYMOUS | `GET /api/events/by-artist` | 200 |
| ☐ | S070 |  | EVT-06-STAFF | STAFF | `GET /api/events/by-artist` | 200 |
| ☐ | S071 | 📸 | E2E-TKT-01 | USER_A | `POST /api/tickets` | 201 |
| ☐ | S072 |  | E2E-TKT-02 | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 |
| ☐ | S073 | 📸 | TC-SEC-009 | USER_B | `GET /api/tickets/{{ticketCancelableCode}}` | 403 |
| ☐ | S074 | 📸 | TC-SEC-011 | USER_B | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 403 |
| ☐ | S075 | 📸 | TKT-02-CHECK | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 |
| ☐ | S076 | 📸 | E2E-TKT-05 | USER_A | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 200 |
| ☐ | S077 | 📸 | TKT-05-CHECK | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 |
| ☐ | S078 | 📸 | TC-BIZ-011 | USER_A | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 409 |
| ☐ | S079 |  | TKT-01-T2 | USER_A | `POST /api/tickets` | 201 |
| ☐ | S080 | 📸 | TC-SEC-008 | USER_A | `POST /api/tickets` | 403 |
| ☐ | S081 |  | TC-SEC-010 | USER_B | `GET /api/tickets/by-user` | 403 |
| ☐ | S082 | 📸 | TKT-03-B | USER_B | `GET /api/tickets/by-user` | 200 |
| ☐ | S083 |  | TC-SEC-012 | USER_B | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 |
| ☐ | S084 |  | TKT-06-A | USER_A | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 |
| ☐ | S085 | 📸 | TC-SEC-013 | USER_A | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 403 |
| ☐ | S086 | 📸 | E2E-TKT-04 | STAFF | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 200 |
| ☐ | S087 |  | TKT-02-STAFF | STAFF | `GET /api/tickets/{{ticketUsableCode}}` | 200 |
| ☐ | S088 | 📸 | E2E-TKT-06 | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 200 |
| ☐ | S089 | 📸 | TKT-06-CHECK | USER_A | `GET /api/tickets/{{ticketUsableCode}}` | 200 |
| ☐ | S090 | 📸 | TC-BIZ-013 | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 409 |
| ☐ | S091 |  | TKT-05-USED | USER_A | `PATCH /api/tickets/{{ticketUsableCode}}/cancel` | 409 |
| ☐ | S092 |  | E2E-TKT-03 | USER_A | `GET /api/tickets/by-user` | 200 |
| ☐ | S093 |  | TKT-03-ADMIN | ADMIN | `GET /api/tickets/by-user` | 200 |
| ☐ | S094 |  | NEG-TKT-401 | ANONYMOUS | `POST /api/tickets` | 401 |
| ☐ | S095 |  | NEG-TKT-404 | USER_A | `GET /api/tickets/NO-EXISTE-{{runId}}` | 404 |
| ☐ | S096 | 📸 | TC-BIZ-014a | USER_A | `POST /api/tickets` | 409 |
| ☐ | S097 |  | TC-BIZ-014b | USER_A | `POST /api/tickets` | 409 |
| ☐ | S098 | 📸 | TC-BIZ-014c | USER_A | `POST /api/tickets` | 201 |
| ☐ | S099 | 📸 | TC-BIZ-014d | USER_B | `POST /api/tickets` | 409 |
| ☐ | S100 |  | NEG-TKT-404E | USER_A | `POST /api/tickets` | 404 |
| ☐ | S101 |  | NEG-TKT-403U | USER_A | `POST /api/tickets` | 403 |
| ☐ | S102 |  | NEG-TKT-400M | USER_A | `POST /api/tickets` | 400 |
| ☐ | S103 |  | NEG-TKT-400E | USER_A | `POST /api/tickets` | 400 |
| ☐ | S104 |  | NEG-JSON-400 | USER_A | `POST /api/tickets` | 400 |
| ☐ | S105 | 📸 | NEG-EVT-400 | ADMIN | `POST /api/events` | 400 |
| ☐ | S106 |  | NEG-EVT-400C | ADMIN | `POST /api/events` | 400 |
| ☐ | S107 |  | NEG-EVT-409D | ADMIN | `POST /api/events` | 409 |
| ☐ | S108 |  | NEG-EVT-409V | ADMIN | `POST /api/events` | 409 |
| ☐ | S109 |  | NEG-EVT-404V | ADMIN | `POST /api/events` | 404 |
| ☐ | S110 |  | NEG-EVT-404 | ANONYMOUS | `GET /api/events/NO-EXISTE-{{runId}}` | 404 |
| ☐ | S111 |  | NEG-TYPE-400 | ANONYMOUS | `GET /api/artists/abc` | 400 |
| ☐ | S112 |  | NEG-PARAM-400 | ANONYMOUS | `GET /api/artists/by-stage-name` | 400 |
| ☐ | S113 |  | NEG-405 | ADMIN | `PUT /api/events/{{publishedEventCode}}` | 405 |
| ☐ | S114 |  | NEG-404R | ADMIN | `GET /api/ruta-que-no-existe` | 404 |
| ☐ | S115 | 📸 | NEG-DENY-DEFAULT | ANONYMOUS | `GET /api/ruta-que-no-existe` | 401 |
| ☐ | S116 |  | TC-SEC-017a | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 409 |
| ☐ | S117 | 📸 | TC-SEC-017b | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 |
| ☐ | S118 | 📸 | TC-AUTH-010 | USER_A (token vencido) | `GET /api/auth/me` | 401 |
| ☐ | S119 |  | FIN-01 | ANONYMOUS | `GET /api/events/published` | 200 |
| ☐ | S120 | 📸 | FIN-02 | USER_A | `GET /api/tickets/by-user` | 200 |

## Matriz de cobertura de las 22 rutas

| Caso base | Método | Ruta | Request nominal | Variantes de seguridad / negocio |
|---|---|---|---|---|
| E2E-VEN-01 | GET | `/api/venues/{code}` | S003 E2E-VEN-01 | NEG-VEN-01 |
| E2E-VEN-02 | GET | `/api/venues/active` | S002 E2E-VEN-02 | PREP-01, TC-AUTH-015 |
| E2E-EVT-01 | POST | `/api/events` | S053 E2E-EVT-01 | TC-SEC-001a, TC-SEC-001b, TC-SEC-001c, NEG-EVT-400, NEG-EVT-400C, NEG-EVT-409D, NEG-EVT-409V, NEG-EVT-404V |
| E2E-EVT-02 | GET | `/api/events/{eventCode}` | S011 E2E-EVT-02 | TC-SEC-004, EVT-02-ADMIN, TC-SEC-015, EVT-02-STAFF, EVT-02-ANON, TC-SEC-002c, EVT-02-PUB, NEG-EVT-404 |
| E2E-EVT-03 | GET | `/api/events/published` | S010 E2E-EVT-03 | EVT-03-NEW, FIN-01 |
| E2E-EVT-04 | PATCH | `/api/events/{eventCode}/publish` | S065 E2E-EVT-04 | TC-SEC-002a, TC-SEC-002b, EVT-04-DUP |
| E2E-EVT-05 | POST | `/api/events/{eventCode}/artists/{artistId}` | S059 E2E-EVT-05 | TC-SEC-003, EVT-05-DUP, EVT-05-404 |
| E2E-EVT-06 | GET | `/api/events/by-artist` | S013 E2E-EVT-06 | TC-SEC-005, EVT-06-NEW, EVT-06-STAFF |
| E2E-ART-01 | GET | `/api/artists/{id}` | S006 E2E-ART-01 | NEG-ART-01 |
| E2E-ART-02 | GET | `/api/artists/by-stage-name` | S007 E2E-ART-02 | NEG-ART-02, NEG-PARAM-400 |
| E2E-ART-03 | GET | `/api/artists/active` | S005 E2E-ART-03 | — |
| E2E-USR-01 | POST | `/api/users` | S016 E2E-USR-01 | TC-BIZ-006, NEG-USR-409, NEG-USR-400, NEG-USR-PW, TC-SEC-016 |
| E2E-USR-02 | GET | `/api/users/by-email` | S043 E2E-USR-02 | USR-ADMIN, TC-SEC-006, NEG-USR-401, NEG-USR-404 |
| E2E-USR-03 | GET | `/api/users/by-username` | S044 E2E-USR-03 | TC-SEC-007 |
| E2E-TKT-01 | POST | `/api/tickets` | S071 E2E-TKT-01 | TKT-01-T2, TC-SEC-008, NEG-TKT-401, TC-BIZ-014a, TC-BIZ-014b, TC-BIZ-014c, TC-BIZ-014d, NEG-TKT-404E, NEG-TKT-403U, NEG-TKT-400M, NEG-TKT-400E, NEG-JSON-400 |
| E2E-TKT-02 | GET | `/api/tickets/{ticketCode}` | S072 E2E-TKT-02 | TC-SEC-009, TKT-02-CHECK, TKT-05-CHECK, TKT-02-STAFF, TKT-06-CHECK, NEG-TKT-404 |
| E2E-TKT-03 | GET | `/api/tickets/by-user` | S092 E2E-TKT-03 | TC-SEC-010, TKT-03-B, TKT-03-ADMIN, FIN-02 |
| E2E-TKT-04 | GET | `/api/events/{eventCode}/tickets/paid` | S086 E2E-TKT-04 | TC-SEC-013 |
| E2E-TKT-05 | PATCH | `/api/tickets/{ticketCode}/cancel` | S076 E2E-TKT-05 | TC-SEC-011, TC-BIZ-011, TKT-05-USED |
| E2E-TKT-06 | PATCH | `/api/tickets/{ticketCode}/use` | S088 E2E-TKT-06 | TC-SEC-012, TKT-06-A, TC-BIZ-013, TC-SEC-017a, TC-SEC-017b |
| E2E-AUTH-01 | POST | `/api/auth/login` | S022 E2E-AUTH-01 | TC-AUTH-003, LOGIN-B, LOGIN-STAFF, LOGIN-ADMIN, LOGIN-INJ, TC-AUTH-004, TC-AUTH-005, TC-AUTH-013a, TC-AUTH-013c |
| E2E-AUTH-02 | GET | `/api/auth/me` | S028 E2E-AUTH-02 | ME-B, ME-STAFF, ME-ADMIN, ME-INJ, TC-AUTH-007, TC-AUTH-008, TC-AUTH-009, TC-AUTH-011, TC-AUTH-012, TC-AUTH-013b, TC-AUTH-010 |

**Total: 22/22 rutas** (20 de negocio + login + me).

## Roles contrastados

| Rol | Casos clave |
|---|---|
| ANONYMOUS | rutas públicas, TC-AUTH-007/008/012, TC-SEC-001a, TC-SEC-004/005, NEG-DENY-DEFAULT |
| USER_A (propietario) | compra, consulta, cancelación y listado de sus tickets; su usuario |
| USER_B (ajeno) | TC-SEC-006/007/009/010/011/012 |
| STAFF | TKT-04, TKT-06, TC-SEC-001c, TC-SEC-002b, TC-SEC-017 |
| ADMIN | EVT-01/04/05, TC-SEC-015, USR-ADMIN, TKT-03-ADMIN |

## Trazabilidad requisito → evidencia

| Requisito | Evidencia |
|---|---|
| FR-E2E-001/002 | Importación de colección y ambiente; 22 rutas identificables (matriz anterior) |
| FR-E2E-003/004 | Logins y `/me` por actor (S0xx de la carpeta 03) |
| FR-E2E-005/006 | Revisión manual en cada fila de la bitácora; variables copiadas a mano |
| FR-E2E-007/008 | Casos 401/403 + reconsulta posterior (`*-CHECK`) |
| FR-E2E-009 | Columna «JSON observado» con los cinco campos de ErrorResponse |
| FR-E2E-010/016 | `fixtures/README.md` y `fixtures/seed-e2e.sql` |
| FR-E2E-011/012 | Este checklist y `registro-ejecucion-manual.md` |
| FR-E2E-013 | `.gitignore` de esta carpeta; no adjuntar tokens ni contraseñas |
| FR-E2E-014/015 | Sección «Defectos y regresiones» y estado BLOCKED |
| FR-E2E-017 | La colección sigue los contratos reales de las Fases 3 y 4 |

## Evidencia fotográfica

- Se fotografían las ejecuciones marcadas con 📸 y **cualquier FAIL, BLOCKED o 500**.
- Cada captura muestra: método y ruta, la variable de *Authorization* (el nombre, p. ej. `{{userAToken}}`, no su valor), el *Status* y el *Body*.
- **No fotografiar** el cuerpo de los logins (trae el `accessToken`) ni el ambiente con valores, ni nada que muestre contraseñas o el secreto JWT.
- Nombre sugerido del archivo: `S071_201_TC-TKT-01.png`. Guardarlas en `capturas/` (excluida por `.gitignore`) y mencionarlas en la columna «Evidencia» de la bitácora.

## Definition of Done

- ☐ Colección v2.1 importable, sin scripts (Pre-request ni Tests).
- ☐ 22/22 rutas probadas manualmente.
- ☐ Logins de A, B, STAFF y ADMIN con tokens en variables locales (sin hardcodear).
- ☐ Casos 400, 401, 403, 404 y 409 ejecutados.
- ☐ Visibilidad de borradores y propiedad de usuarios/tickets verificada.
- ☐ Compra ajena bloqueada y sin mutación comprobada (TKT-03-B).
- ☐ Estados de eventos y tickets comprobados con GET posterior.
- ☐ Estructura de ErrorResponse comprobada.
- ☐ Bitácora diligenciada (PASS/FAIL/BLOCKED) con observaciones.
- ☐ Variables y evidencias exportables sin contraseñas ni tokens.
- ☐ Defectos y bloqueos registrados; no se declara aprobado lo no ejecutado.
