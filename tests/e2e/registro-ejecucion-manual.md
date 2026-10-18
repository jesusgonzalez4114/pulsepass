# Bitácora de ejecución manual — PulsePass E2E

> Rellenar tras cada **Send**. Resultado: `PASS`, `FAIL`, `BLOCKED` o `NOT RUN`.
> **No pegar tokens, contraseñas ni capturas que los muestren.**

| Campo | Valor |
|---|---|
| Ejecutor | |
| Fecha / hora de inicio | |
| runId | |
| Base de datos | pulsepass_e2e |
| Commit de `develop` probado | |
| Política 403/404 en recurso ajeno | 403 (recurso existente) · 404 (ausente o borrador para el público) |

## Casos

| Orden | Foto | Caso | Actor | Ruta | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Evidencia / observación |
|---|---|---|---|---|---|---|---|---|---|
| S001 |  | PREP-01 | ANONYMOUS | `GET /api/venues/active` | 200 | | | NOT RUN | |
| S002 |  | E2E-VEN-02 | ANONYMOUS | `GET /api/venues/active` | 200 | | | NOT RUN | |
| S003 |  | E2E-VEN-01 | ANONYMOUS | `GET /api/venues/{{venueActiveCode}}` | 200 | | | NOT RUN | |
| S004 |  | NEG-VEN-01 | ANONYMOUS | `GET /api/venues/NO-EXISTE-{{runId}}` | 404 | | | NOT RUN | |
| S005 |  | E2E-ART-03 | ANONYMOUS | `GET /api/artists/active` | 200 | | | NOT RUN | |
| S006 |  | E2E-ART-01 | ANONYMOUS | `GET /api/artists/{{artistId}}` | 200 | | | NOT RUN | |
| S007 |  | E2E-ART-02 | ANONYMOUS | `GET /api/artists/by-stage-name` | 200 | | | NOT RUN | |
| S008 |  | NEG-ART-01 | ANONYMOUS | `GET /api/artists/999999` | 404 | | | NOT RUN | |
| S009 |  | NEG-ART-02 | ANONYMOUS | `GET /api/artists/by-stage-name` | 404 | | | NOT RUN | |
| S010 |  | E2E-EVT-03 | ANONYMOUS | `GET /api/events/published` | 200 | | | NOT RUN | |
| S011 |  | E2E-EVT-02 | ANONYMOUS | `GET /api/events/{{publishedEventCode}}` | 200 | | | NOT RUN | |
| S012 | 📸 | TC-SEC-004 | ANONYMOUS | `GET /api/events/{{draftEventCode}}` | 404 | | | NOT RUN | |
| S013 |  | E2E-EVT-06 | ANONYMOUS | `GET /api/events/by-artist` | 200 | | | NOT RUN | |
| S014 | 📸 | TC-SEC-005 | ANONYMOUS | `GET /api/events/by-artist` | 200 | | | NOT RUN | |
| S015 |  | TC-AUTH-015 | ANONYMOUS | `GET /api/venues/active` | 200 | | | NOT RUN | |
| S016 | 📸 | E2E-USR-01 | ANONYMOUS | `POST /api/users` | 201 | | | NOT RUN | |
| S017 |  | TC-BIZ-006 | ANONYMOUS | `POST /api/users` | 201 | | | NOT RUN | |
| S018 |  | NEG-USR-409 | ANONYMOUS | `POST /api/users` | 409 | | | NOT RUN | |
| S019 |  | NEG-USR-400 | ANONYMOUS | `POST /api/users` | 400 | | | NOT RUN | |
| S020 |  | NEG-USR-PW | ANONYMOUS | `POST /api/users` | 400 | | | NOT RUN | |
| S021 | 📸 | TC-SEC-016 | ANONYMOUS | `POST /api/users` | 201 | | | NOT RUN | |
| S022 |  | E2E-AUTH-01 | ANONYMOUS | `POST /api/auth/login` | 200 | | | NOT RUN | |
| S023 |  | TC-AUTH-003 | ANONYMOUS | `POST /api/auth/login` | 200 | | | NOT RUN | |
| S024 |  | LOGIN-B | ANONYMOUS | `POST /api/auth/login` | 200 | | | NOT RUN | |
| S025 |  | LOGIN-STAFF | ANONYMOUS | `POST /api/auth/login` | 200 | | | NOT RUN | |
| S026 |  | LOGIN-ADMIN | ANONYMOUS | `POST /api/auth/login` | 200 | | | NOT RUN | |
| S027 |  | LOGIN-INJ | ANONYMOUS | `POST /api/auth/login` | 200 | | | NOT RUN | |
| S028 | 📸 | E2E-AUTH-02 | USER_A | `GET /api/auth/me` | 200 | | | NOT RUN | |
| S029 |  | ME-B | USER_B | `GET /api/auth/me` | 200 | | | NOT RUN | |
| S030 |  | ME-STAFF | STAFF | `GET /api/auth/me` | 200 | | | NOT RUN | |
| S031 |  | ME-ADMIN | ADMIN | `GET /api/auth/me` | 200 | | | NOT RUN | |
| S032 | 📸 | ME-INJ | USER (registro con role inyectado) | `GET /api/auth/me` | 200 | | | NOT RUN | |
| S033 | 📸 | TC-AUTH-004 | ANONYMOUS | `POST /api/auth/login` | 401 | | | NOT RUN | |
| S034 | 📸 | TC-AUTH-005 | ANONYMOUS | `POST /api/auth/login` | 401 | | | NOT RUN | |
| S035 | 📸 | TC-AUTH-007 | ANONYMOUS | `GET /api/auth/me` | 401 | | | NOT RUN | |
| S036 |  | TC-AUTH-008 | ANONYMOUS | `GET /api/auth/me` | 401 | | | NOT RUN | |
| S037 | 📸 | TC-AUTH-009 | USER_A (token alterado) | `GET /api/auth/me` | 401 | | | NOT RUN | |
| S038 |  | TC-AUTH-011 | token de otra clave | `GET /api/auth/me` | 401 | | | NOT RUN | |
| S039 |  | TC-AUTH-012 | ANONYMOUS | `GET /api/auth/me` | 401 | | | NOT RUN | |
| S040 |  | TC-AUTH-013a | ANONYMOUS | `POST /api/auth/login` | 200 | | | NOT RUN | |
| S041 | 📸 | TC-AUTH-013b | INACTIVE_USER | `GET /api/auth/me` | 401 | | | NOT RUN | |
| S042 |  | TC-AUTH-013c | ANONYMOUS | `POST /api/auth/login` | 401 | | | NOT RUN | |
| S043 |  | E2E-USR-02 | USER_A | `GET /api/users/by-email` | 200 | | | NOT RUN | |
| S044 |  | E2E-USR-03 | USER_A | `GET /api/users/by-username` | 200 | | | NOT RUN | |
| S045 |  | USR-ADMIN | ADMIN | `GET /api/users/by-email` | 200 | | | NOT RUN | |
| S046 | 📸 | TC-SEC-006 | USER_B | `GET /api/users/by-email` | 403 | | | NOT RUN | |
| S047 |  | TC-SEC-007 | USER_B | `GET /api/users/by-username` | 403 | | | NOT RUN | |
| S048 |  | NEG-USR-401 | ANONYMOUS | `GET /api/users/by-email` | 401 | | | NOT RUN | |
| S049 |  | NEG-USR-404 | ADMIN | `GET /api/users/by-email` | 404 | | | NOT RUN | |
| S050 |  | TC-SEC-001a | ANONYMOUS | `POST /api/events` | 401 | | | NOT RUN | |
| S051 | 📸 | TC-SEC-001b | USER_A | `POST /api/events` | 403 | | | NOT RUN | |
| S052 |  | TC-SEC-001c | STAFF | `POST /api/events` | 403 | | | NOT RUN | |
| S053 | 📸 | E2E-EVT-01 | ADMIN | `POST /api/events` | 201 | | | NOT RUN | |
| S054 |  | EVT-02-ADMIN | ADMIN | `GET /api/events/{{newEventCode}}` | 200 | | | NOT RUN | |
| S055 |  | TC-SEC-015 | ADMIN | `GET /api/events/{{draftEventCode}}` | 200 | | | NOT RUN | |
| S056 |  | EVT-02-STAFF | STAFF | `GET /api/events/{{draftEventCode}}` | 200 | | | NOT RUN | |
| S057 | 📸 | EVT-02-ANON | ANONYMOUS | `GET /api/events/{{newEventCode}}` | 404 | | | NOT RUN | |
| S058 |  | TC-SEC-003 | USER_A | `POST /api/events/{{newEventCode}}/artists/{{artistId}}` | 403 | | | NOT RUN | |
| S059 |  | E2E-EVT-05 | ADMIN | `POST /api/events/{{newEventCode}}/artists/{{artistId}}` | 200 | | | NOT RUN | |
| S060 |  | EVT-05-DUP | ADMIN | `POST /api/events/{{newEventCode}}/artists/{{artistId}}` | 409 | | | NOT RUN | |
| S061 |  | EVT-05-404 | ADMIN | `POST /api/events/{{newEventCode}}/artists/999999` | 404 | | | NOT RUN | |
| S062 |  | TC-SEC-002a | USER_A | `PATCH /api/events/{{eventToPublishCode}}/publish` | 403 | | | NOT RUN | |
| S063 | 📸 | TC-SEC-002b | STAFF | `PATCH /api/events/{{eventToPublishCode}}/publish` | 403 | | | NOT RUN | |
| S064 | 📸 | TC-SEC-002c | ADMIN | `GET /api/events/{{eventToPublishCode}}` | 200 | | | NOT RUN | |
| S065 | 📸 | E2E-EVT-04 | ADMIN | `PATCH /api/events/{{newEventCode}}/publish` | 200 | | | NOT RUN | |
| S066 |  | EVT-04-DUP | ADMIN | `PATCH /api/events/{{newEventCode}}/publish` | 409 | | | NOT RUN | |
| S067 | 📸 | EVT-02-PUB | ANONYMOUS | `GET /api/events/{{newEventCode}}` | 200 | | | NOT RUN | |
| S068 |  | EVT-03-NEW | ANONYMOUS | `GET /api/events/published` | 200 | | | NOT RUN | |
| S069 |  | EVT-06-NEW | ANONYMOUS | `GET /api/events/by-artist` | 200 | | | NOT RUN | |
| S070 |  | EVT-06-STAFF | STAFF | `GET /api/events/by-artist` | 200 | | | NOT RUN | |
| S071 | 📸 | E2E-TKT-01 | USER_A | `POST /api/tickets` | 201 | | | NOT RUN | |
| S072 |  | E2E-TKT-02 | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | | | NOT RUN | |
| S073 | 📸 | TC-SEC-009 | USER_B | `GET /api/tickets/{{ticketCancelableCode}}` | 403 | | | NOT RUN | |
| S074 | 📸 | TC-SEC-011 | USER_B | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 403 | | | NOT RUN | |
| S075 | 📸 | TKT-02-CHECK | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | | | NOT RUN | |
| S076 | 📸 | E2E-TKT-05 | USER_A | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 200 | | | NOT RUN | |
| S077 | 📸 | TKT-05-CHECK | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | | | NOT RUN | |
| S078 | 📸 | TC-BIZ-011 | USER_A | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 409 | | | NOT RUN | |
| S079 |  | TKT-01-T2 | USER_A | `POST /api/tickets` | 201 | | | NOT RUN | |
| S080 | 📸 | TC-SEC-008 | USER_A | `POST /api/tickets` | 403 | | | NOT RUN | |
| S081 |  | TC-SEC-010 | USER_B | `GET /api/tickets/by-user` | 403 | | | NOT RUN | |
| S082 | 📸 | TKT-03-B | USER_B | `GET /api/tickets/by-user` | 200 | | | NOT RUN | |
| S083 |  | TC-SEC-012 | USER_B | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 | | | NOT RUN | |
| S084 |  | TKT-06-A | USER_A | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 | | | NOT RUN | |
| S085 | 📸 | TC-SEC-013 | USER_A | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 403 | | | NOT RUN | |
| S086 | 📸 | E2E-TKT-04 | STAFF | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 200 | | | NOT RUN | |
| S087 |  | TKT-02-STAFF | STAFF | `GET /api/tickets/{{ticketUsableCode}}` | 200 | | | NOT RUN | |
| S088 | 📸 | E2E-TKT-06 | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 200 | | | NOT RUN | |
| S089 | 📸 | TKT-06-CHECK | USER_A | `GET /api/tickets/{{ticketUsableCode}}` | 200 | | | NOT RUN | |
| S090 | 📸 | TC-BIZ-013 | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 409 | | | NOT RUN | |
| S091 |  | TKT-05-USED | USER_A | `PATCH /api/tickets/{{ticketUsableCode}}/cancel` | 409 | | | NOT RUN | |
| S092 |  | E2E-TKT-03 | USER_A | `GET /api/tickets/by-user` | 200 | | | NOT RUN | |
| S093 |  | TKT-03-ADMIN | ADMIN | `GET /api/tickets/by-user` | 200 | | | NOT RUN | |
| S094 |  | NEG-TKT-401 | ANONYMOUS | `POST /api/tickets` | 401 | | | NOT RUN | |
| S095 |  | NEG-TKT-404 | USER_A | `GET /api/tickets/NO-EXISTE-{{runId}}` | 404 | | | NOT RUN | |
| S096 | 📸 | TC-BIZ-014a | USER_A | `POST /api/tickets` | 409 | | | NOT RUN | |
| S097 |  | TC-BIZ-014b | USER_A | `POST /api/tickets` | 409 | | | NOT RUN | |
| S098 | 📸 | TC-BIZ-014c | USER_A | `POST /api/tickets` | 201 | | | NOT RUN | |
| S099 | 📸 | TC-BIZ-014d | USER_B | `POST /api/tickets` | 409 | | | NOT RUN | |
| S100 |  | NEG-TKT-404E | USER_A | `POST /api/tickets` | 404 | | | NOT RUN | |
| S101 |  | NEG-TKT-403U | USER_A | `POST /api/tickets` | 403 | | | NOT RUN | |
| S102 |  | NEG-TKT-400M | USER_A | `POST /api/tickets` | 400 | | | NOT RUN | |
| S103 |  | NEG-TKT-400E | USER_A | `POST /api/tickets` | 400 | | | NOT RUN | |
| S104 |  | NEG-JSON-400 | USER_A | `POST /api/tickets` | 400 | | | NOT RUN | |
| S105 | 📸 | NEG-EVT-400 | ADMIN | `POST /api/events` | 400 | | | NOT RUN | |
| S106 |  | NEG-EVT-400C | ADMIN | `POST /api/events` | 400 | | | NOT RUN | |
| S107 |  | NEG-EVT-409D | ADMIN | `POST /api/events` | 409 | | | NOT RUN | |
| S108 |  | NEG-EVT-409V | ADMIN | `POST /api/events` | 409 | | | NOT RUN | |
| S109 |  | NEG-EVT-404V | ADMIN | `POST /api/events` | 404 | | | NOT RUN | |
| S110 |  | NEG-EVT-404 | ANONYMOUS | `GET /api/events/NO-EXISTE-{{runId}}` | 404 | | | NOT RUN | |
| S111 |  | NEG-TYPE-400 | ANONYMOUS | `GET /api/artists/abc` | 400 | | | NOT RUN | |
| S112 |  | NEG-PARAM-400 | ANONYMOUS | `GET /api/artists/by-stage-name` | 400 | | | NOT RUN | |
| S113 |  | NEG-405 | ADMIN | `PUT /api/events/{{publishedEventCode}}` | 405 | | | NOT RUN | |
| S114 |  | NEG-404R | ADMIN | `GET /api/ruta-que-no-existe` | 404 | | | NOT RUN | |
| S115 | 📸 | NEG-DENY-DEFAULT | ANONYMOUS | `GET /api/ruta-que-no-existe` | 401 | | | NOT RUN | |
| S116 |  | TC-SEC-017a | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 409 | | | NOT RUN | |
| S117 | 📸 | TC-SEC-017b | STAFF | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 | | | NOT RUN | |
| S118 | 📸 | TC-AUTH-010 | USER_A (token vencido) | `GET /api/auth/me` | 401 | | | NOT RUN | |
| S119 |  | FIN-01 | ANONYMOUS | `GET /api/events/published` | 200 | | | NOT RUN | |
| S120 | 📸 | FIN-02 | USER_A | `GET /api/tickets/by-user` | 200 | | | NOT RUN | |

## Consolidación (16.3)

| Módulo | PASS | FAIL | BLOCKED | NOT RUN |
|---|---|---|---|---|
| 00 Preparación / conectividad | | | | |
| 01 Venues (2 rutas) | | | | |
| 02 Artists (3 rutas) | | | | |
| 03 Auth / usuarios (registro, login, me) | | | | |
| 04 Usuarios / consulta y propiedad (2 rutas) | | | | |
| 05 Events / administración y visibilidad (6 rutas) | | | | |
| 06 Tickets / compra, consulta y ciclo de vida (6 rutas) | | | | |
| 07 Seguridad / escenarios negativos | | | | |
| 08 Validaciones y errores HTTP | | | | |
| 09 Flujos de negocio / secuencia recomendada | | | | |

Las 22 rutas tienen al menos un caso nominal ejecutado: ☐ sí  ☐ no (listar cuáles faltan).

## Defectos y regresiones (FR-E2E-014)

| # | Caso | Precondiciones | Pasos | HTTP esperado | HTTP observado | Reproducción |
|---|---|---|---|---|---|---|
| 1 | | | | | | |
