# Bitácora — Anuar Hatum (PulsePass E2E)

Ejecutor: Anuar Hatum (verificó que todos los casos coincidieron con lo esperado) · runId: `rb` · Base: `pulsepass_e2e` (Docker) · 



Orden: S001 (conexión), S016, S017, S022, S024–S026, S071–S104, S111–S117, S119 y S120. 

| Orden | Foto | Caso | Ruta | HTTP esperado | HTTP observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|
| S001 |  | PREP-01 | `GET /api/venues/active` | 200 | 200 | PASS | |
| S016 | Sí | E2E-USR-01 | `POST /api/users` | 201 | 201 | PASS | |
| S017 |  | TC-BIZ-006 | `POST /api/users` | 201 | 201 | PASS | |
| S022 |  | E2E-AUTH-01 | `POST /api/auth/login` | 200 | 200 | PASS | |
| S024 |  | LOGIN-B | `POST /api/auth/login` | 200 | 200 | PASS | |
| S025 |  | LOGIN-STAFF | `POST /api/auth/login` | 200 | 200 | PASS | |
| S026 |  | LOGIN-ADMIN | `POST /api/auth/login` | 200 | 200 | PASS | |
| S071 | Sí | E2E-TKT-01 | `POST /api/tickets` | 201 | 201 | PASS | |
| S072 |  | E2E-TKT-02 | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | 200 | PASS | |
| S073 | Sí | TC-SEC-009 | `GET /api/tickets/{{ticketCancelableCode}}` | 403 | 403 | PASS | |
| S074 | Sí | TC-SEC-011 | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 403 | 403 | PASS | |
| S075 | Sí | TKT-02-CHECK | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | 200 | PASS | |
| S076 | Sí | E2E-TKT-05 | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 200 | 200 | PASS | |
| S077 | Sí | TKT-05-CHECK | `GET /api/tickets/{{ticketCancelableCode}}` | 200 | 200 | PASS | |
| S078 | Sí | TC-BIZ-011 | `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` | 409 | 409 | PASS | |
| S079 |  | TKT-01-T2 | `POST /api/tickets` | 201 | 201 | PASS | |
| S080 | Sí | TC-SEC-008 | `POST /api/tickets` | 403 | 403 | PASS | |
| S081 |  | TC-SEC-010 | `GET /api/tickets/by-user` | 403 | 403 | PASS | |
| S082 | Sí | TKT-03-B | `GET /api/tickets/by-user` | 200 | 200 | PASS | |
| S083 |  | TC-SEC-012 | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 | 403 | PASS | |
| S084 |  | TKT-06-A | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 | 403 | PASS | |
| S085 | Sí | TC-SEC-013 | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 403 | 403 | PASS | |
| S086 | Sí | E2E-TKT-04 | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 200 | 200 | PASS | |
| S087 |  | TKT-02-STAFF | `GET /api/tickets/{{ticketUsableCode}}` | 200 | 200 | PASS | |
| S088 | Sí | E2E-TKT-06 | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 200 | 200 | PASS | |
| S089 | Sí | TKT-06-CHECK | `GET /api/tickets/{{ticketUsableCode}}` | 200 | 200 | PASS | |
| S090 | Sí | TC-BIZ-013 | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 409 | 409 | PASS | |
| S091 |  | TKT-05-USED | `PATCH /api/tickets/{{ticketUsableCode}}/cancel` | 409 | 409 | PASS | |
| S092 |  | E2E-TKT-03 | `GET /api/tickets/by-user` | 200 | 200 | PASS | |
| S093 |  | TKT-03-ADMIN | `GET /api/tickets/by-user` | 200 | 200 | PASS | |
| S094 |  | NEG-TKT-401 | `POST /api/tickets` | 401 | 401 | PASS | |
| S095 |  | NEG-TKT-404 | `GET /api/tickets/NO-EXISTE-{{runId}}` | 404 | 404 | PASS | |
| S096 | Sí | TC-BIZ-014a | `POST /api/tickets` | 409 | 409 | PASS | |
| S097 |  | TC-BIZ-014b | `POST /api/tickets` | 409 | 409 | PASS | |
| S098 | Sí | TC-BIZ-014c | `POST /api/tickets` | 201 | 201 | PASS | |
| S099 | Sí | TC-BIZ-014d | `POST /api/tickets` | 409 | 409 | PASS | |
| S100 |  | NEG-TKT-404E | `POST /api/tickets` | 404 | 404 | PASS | |
| S101 |  | NEG-TKT-403U | `POST /api/tickets` | 403 | 403 | PASS | |
| S102 |  | NEG-TKT-400M | `POST /api/tickets` | 400 | 400 | PASS | |
| S103 |  | NEG-TKT-400E | `POST /api/tickets` | 400 | 400 | PASS | |
| S104 |  | NEG-JSON-400 | `POST /api/tickets` | 400 | 400 | PASS | |
| S111 |  | NEG-TYPE-400 | `GET /api/artists/abc` | 400 | 400 | PASS | |
| S112 |  | NEG-PARAM-400 | `GET /api/artists/by-stage-name` | 400 | 400 | PASS | |
| S113 |  | NEG-405 | `PUT /api/events/{{publishedEventCode}}` | 405 | 405 | PASS | |
| S114 |  | NEG-404R | `GET /api/ruta-que-no-existe` | 404 | 404 | PASS | |
| S115 | Sí | NEG-DENY-DEFAULT | `GET /api/ruta-que-no-existe` | 401 | 401 | PASS | |
| S116 |  | TC-SEC-017a | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 409 | 409 | PASS | |
| S117 | Sí | TC-SEC-017b | `PATCH /api/tickets/{{ticketUsableCode}}/use` | 403 | 403 | PASS | |
| S119 |  | FIN-01 | `GET /api/events/published` | 200 | 200 | PASS | |
| S120 | Sí | FIN-02 | `GET /api/tickets/by-user` | 200 | 200 | PASS | |
