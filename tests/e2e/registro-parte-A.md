# Bitácora — Jesus Gonzalez (PulsePass E2E)

 Ejecutor: Jesus Gonzalez (verificó que todos los casos coincidieron con lo esperado) · runId: `ra` · Base: `pulsepass_e2e` (Docker) 



Orden: S001–S070, S105–S110, S119 y al final S118. Los 401 de S033, S034, S036–S039, S041 y S042 son esperados.
| Orden | Foto | Caso | Ruta | HTTP esperado | HTTP observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|
| S001 |  | PREP-01 | `GET /api/venues/active` | 200 | 200 | PASS | |
| S002 |  | E2E-VEN-02 | `GET /api/venues/active` | 200 | 200 | PASS | |
| S003 |  | E2E-VEN-01 | `GET /api/venues/{{venueActiveCode}}` | 200 | 200 | PASS | |
| S004 |  | NEG-VEN-01 | `GET /api/venues/NO-EXISTE-{{runId}}` | 404 | 404 | PASS | |
| S005 |  | E2E-ART-03 | `GET /api/artists/active` | 200 | 200 | PASS | |
| S006 |  | E2E-ART-01 | `GET /api/artists/{{artistId}}` | 200 | 200 | PASS | |
| S007 |  | E2E-ART-02 | `GET /api/artists/by-stage-name` | 200 | 200 | PASS | |
| S008 |  | NEG-ART-01 | `GET /api/artists/999999` | 404 | 404 | PASS | |
| S009 |  | NEG-ART-02 | `GET /api/artists/by-stage-name` | 404 | 404 | PASS | |
| S010 |  | E2E-EVT-03 | `GET /api/events/published` | 200 | 200 | PASS | |
| S011 |  | E2E-EVT-02 | `GET /api/events/{{publishedEventCode}}` | 200 | 200 | PASS | |
| S012 | Sí | TC-SEC-004 | `GET /api/events/{{draftEventCode}}` | 404 | 404 | PASS | |
| S013 |  | E2E-EVT-06 | `GET /api/events/by-artist` | 200 | 200 | PASS | |
| S014 | Sí | TC-SEC-005 | `GET /api/events/by-artist` | 200 | 200 | PASS | |
| S015 |  | TC-AUTH-015 | `GET /api/venues/active` | 200 | 200 | PASS | |
| S016 | Sí | E2E-USR-01 | `POST /api/users` | 201 | 201 | PASS | |
| S017 |  | TC-BIZ-006 | `POST /api/users` | 201 | 201 | PASS | |
| S018 |  | NEG-USR-409 | `POST /api/users` | 409 | 409 | PASS | |
| S019 |  | NEG-USR-400 | `POST /api/users` | 400 | 400 | PASS | |
| S020 |  | NEG-USR-PW | `POST /api/users` | 400 | 400 | PASS | |
| S021 | Sí | TC-SEC-016 | `POST /api/users` | 201 | 201 | PASS | |
| S022 |  | E2E-AUTH-01 | `POST /api/auth/login` | 200 | 200 | PASS | |
| S023 |  | TC-AUTH-003 | `POST /api/auth/login` | 200 | 200 | PASS | |
| S024 |  | LOGIN-B | `POST /api/auth/login` | 200 | 200 | PASS | |
| S025 |  | LOGIN-STAFF | `POST /api/auth/login` | 200 | 200 | PASS | |
| S026 |  | LOGIN-ADMIN | `POST /api/auth/login` | 200 | 200 | PASS | |
| S027 |  | LOGIN-INJ | `POST /api/auth/login` | 200 | 200 | PASS | |
| S028 | Sí | E2E-AUTH-02 | `GET /api/auth/me` | 200 | 200 | PASS | |
| S029 |  | ME-B | `GET /api/auth/me` | 200 | 200 | PASS | |
| S030 |  | ME-STAFF | `GET /api/auth/me` | 200 | 200 | PASS | |
| S031 |  | ME-ADMIN | `GET /api/auth/me` | 200 | 200 | PASS | |
| S032 | Sí | ME-INJ | `GET /api/auth/me` | 200 | 200 | PASS | |
| S033 | Sí | TC-AUTH-004 | `POST /api/auth/login` | 401 | 401 | PASS | |
| S034 | Sí | TC-AUTH-005 | `POST /api/auth/login` | 401 | 401 | PASS | |
| S035 | Sí | TC-AUTH-007 | `GET /api/auth/me` | 401 | 401 | PASS | |
| S036 |  | TC-AUTH-008 | `GET /api/auth/me` | 401 | 401 | PASS | |
| S037 | Sí | TC-AUTH-009 | `GET /api/auth/me` | 401 | 401 | PASS | |
| S038 |  | TC-AUTH-011 | `GET /api/auth/me` | 401 | 401 | PASS | |
| S039 |  | TC-AUTH-012 | `GET /api/auth/me` | 401 | 401 | PASS | |
| S040 |  | TC-AUTH-013a | `POST /api/auth/login` | 200 | 200 | PASS | |
| S041 | Sí | TC-AUTH-013b | `GET /api/auth/me` | 401 | 401 | PASS | |
| S042 |  | TC-AUTH-013c | `POST /api/auth/login` | 401 | 401 | PASS | |
| S043 |  | E2E-USR-02 | `GET /api/users/by-email` | 200 | 200 | PASS | |
| S044 |  | E2E-USR-03 | `GET /api/users/by-username` | 200 | 200 | PASS | |
| S045 |  | USR-ADMIN | `GET /api/users/by-email` | 200 | 200 | PASS | |
| S046 | Sí | TC-SEC-006 | `GET /api/users/by-email` | 403 | 403 | PASS | |
| S047 |  | TC-SEC-007 | `GET /api/users/by-username` | 403 | 403 | PASS | |
| S048 |  | NEG-USR-401 | `GET /api/users/by-email` | 401 | 401 | PASS | |
| S049 |  | NEG-USR-404 | `GET /api/users/by-email` | 404 | 404 | PASS | |
| S050 |  | TC-SEC-001a | `POST /api/events` | 401 | 401 | PASS | |
| S051 | Sí | TC-SEC-001b | `POST /api/events` | 403 | 403 | PASS | |
| S052 |  | TC-SEC-001c | `POST /api/events` | 403 | 403 | PASS | |
| S053 | Sí | E2E-EVT-01 | `POST /api/events` | 201 | 201 | PASS | |
| S054 |  | EVT-02-ADMIN | `GET /api/events/{{newEventCode}}` | 200 | 200 | PASS | |
| S055 |  | TC-SEC-015 | `GET /api/events/{{draftEventCode}}` | 200 | 200 | PASS | |
| S056 |  | EVT-02-STAFF | `GET /api/events/{{draftEventCode}}` | 200 | 200 | PASS | |
| S057 | Sí | EVT-02-ANON | `GET /api/events/{{newEventCode}}` | 404 | 404 | PASS | |
| S058 |  | TC-SEC-003 | `POST /api/events/{{newEventCode}}/artists/{{artistId}}` | 403 | 403 | PASS | |
| S059 |  | E2E-EVT-05 | `POST /api/events/{{newEventCode}}/artists/{{artistId}}` | 200 | 200 | PASS | |
| S060 |  | EVT-05-DUP | `POST /api/events/{{newEventCode}}/artists/{{artistId}}` | 409 | 409 | PASS | |
| S061 |  | EVT-05-404 | `POST /api/events/{{newEventCode}}/artists/999999` | 404 | 404 | PASS | |
| S062 |  | TC-SEC-002a | `PATCH /api/events/{{eventToPublishCode}}/publish` | 403 | 403 | PASS | |
| S063 | Sí | TC-SEC-002b | `PATCH /api/events/{{eventToPublishCode}}/publish` | 403 | 403 | PASS | |
| S064 | Sí | TC-SEC-002c | `GET /api/events/{{eventToPublishCode}}` | 200 | 200 | PASS | |
| S065 | Sí | E2E-EVT-04 | `PATCH /api/events/{{newEventCode}}/publish` | 200 | 200 | PASS | |
| S066 |  | EVT-04-DUP | `PATCH /api/events/{{newEventCode}}/publish` | 409 | 409 | PASS | |
| S067 | Sí | EVT-02-PUB | `GET /api/events/{{newEventCode}}` | 200 | 200 | PASS | |
| S068 |  | EVT-03-NEW | `GET /api/events/published` | 200 | 200 | PASS | |
| S069 |  | EVT-06-NEW | `GET /api/events/by-artist` | 200 | 200 | PASS | |
| S070 |  | EVT-06-STAFF | `GET /api/events/by-artist` | 200 | 200 | PASS | |
| S105 | Sí | NEG-EVT-400 | `POST /api/events` | 400 | 400 | PASS | |
| S106 |  | NEG-EVT-400C | `POST /api/events` | 400 | 400 | PASS | |
| S107 |  | NEG-EVT-409D | `POST /api/events` | 409 | 409 | PASS | |
| S108 |  | NEG-EVT-409V | `POST /api/events` | 409 | 409 | PASS | |
| S109 |  | NEG-EVT-404V | `POST /api/events` | 404 | 404 | PASS | |
| S110 |  | NEG-EVT-404 | `GET /api/events/NO-EXISTE-{{runId}}` | 404 | 404 | PASS | |
| S119 |  | FIN-01 | `GET /api/events/published` | 200 | 200 | PASS | |
| S118 | Sí | TC-AUTH-010 | `GET /api/auth/me` | 401 | 401 | PASS | |
