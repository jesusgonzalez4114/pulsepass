# Variables del ambiente de Postman — PulsePass E2E

## Crear el ambiente a mano en Postman (alternativa a importar el JSON)

1. En Postman: *Environments* → **+** (Create Environment) → nombre `PulsePass — local E2E`.
2. Agrega **una fila por variable** de la tabla (son 40). Respeta el **Tipo**: las de tipo `secret` se ocultan en pantalla.
3. Escribe el **Initial value** tal como aparece (las filas «*vacío*» se dejan sin valor) y rellena el **Current value** según la columna indicada. Los valores secretos y los tokens van **solo en Current value**.
4. **Save** y selecciona ese ambiente arriba a la derecha antes de enviar cualquier request.

Esta misma tabla vive en `postman/PulsePass-local.postman_environment.json`; si importas ese archivo ya tendrás las variables creadas.

| # | Variable | Tipo | Initial value | Current value (qué poner) | Quién |
|---|---|---|---|---|---|
| 1 | `baseUrl` | default | `http://localhost:8080` | `http://localhost:8080` | viene lista |
| 2 | `runId` | default | `r01` | Código corto distinto en cada corrida (A: `ra`, B: `rb`) | tú |
| 3 | `futureEventDate` | default | `2027-06-15T20:00:00` | Fecha futura; cámbiala si ya pasó | viene lista |
| 4 | `userAEmail` | default | `usera.{{runId}}@example.test` | No tocar (usa `runId`) | viene lista |
| 5 | `userAUsername` | default | `usera_{{runId}}` | No tocar (usa `runId`) | viene lista |
| 6 | `userBEmail` | default | `userb.{{runId}}@example.test` | No tocar (usa `runId`) | viene lista |
| 7 | `userBUsername` | default | `userb_{{runId}}` | No tocar (usa `runId`) | viene lista |
| 8 | `userAPassword` | secret | *(vacío)* | Clave ficticia de 12 a 72 caracteres | tú |
| 9 | `userBPassword` | secret | *(vacío)* | Clave ficticia de 12 a 72 caracteres | tú |
| 10 | `adminIdentifier` | default | `e2e_admin` | `e2e_admin` | viene lista |
| 11 | `adminPassword` | secret | *(vacío)* | La misma que usaste en el seed (`admin_pw`) | tú |
| 12 | `staffIdentifier` | default | `e2e_staff` | `e2e_staff` | viene lista |
| 13 | `staffPassword` | secret | *(vacío)* | La misma que usaste en el seed (`staff_pw`) | tú |
| 14 | `inactiveIdentifier` | default | `e2e_inactive` | `e2e_inactive` | viene lista |
| 15 | `inactivePassword` | secret | *(vacío)* | La misma que usaste en el seed (`inactive_pw`) | tú |
| 16 | `userAToken` | secret | *(vacío)* | `accessToken` del login de A (S022) | copiar a mano |
| 17 | `userBToken` | secret | *(vacío)* | `accessToken` del login de B (S024) | copiar a mano |
| 18 | `staffToken` | secret | *(vacío)* | `accessToken` del login de STAFF (S025) | copiar a mano |
| 19 | `adminToken` | secret | *(vacío)* | `accessToken` del login de ADMIN (S026) | copiar a mano |
| 20 | `inactiveToken` | secret | *(vacío)* | `accessToken` del login de INACTIVE (S040) | copiar a mano |
| 21 | `injectedToken` | secret | *(vacío)* | `accessToken` del login del usuario inyectado (S027) | copiar a mano |
| 22 | `tamperedToken` | secret | *(vacío)* | `userAToken` con un carácter cambiado en el medio de la firma | preparar (S037) |
| 23 | `forgedToken` | secret | *(vacío)* | Token firmado fuera de línea con una clave inventada (README, sección 6) | preparar (S038) |
| 24 | `expiredToken` | secret | *(vacío)* | Copia del `accessToken` de A hecha en S022; se usa en S118 tras 15 min | copiar a mano |
| 25 | `venueActiveCode` | default | `E2E-VEN-01` | `E2E-VEN-01` | viene lista |
| 26 | `venueSmallCode` | default | `E2E-VEN-S1` | `E2E-VEN-S1` | viene lista |
| 27 | `venueInactiveCode` | default | `E2E-VEN-OFF` | `E2E-VEN-OFF` | viene lista |
| 28 | `artistId` | default | *(vacío)* | `id` de *Solar Beat* en la respuesta de S005 | copiar a mano |
| 29 | `artistStageName` | default | `Solar Beat` | `Solar Beat` | viene lista |
| 30 | `publishedEventCode` | default | `E2E-PUB-01` | `E2E-PUB-01` | viene lista |
| 31 | `draftEventCode` | default | `E2E-DRAFT-01` | `E2E-DRAFT-01` | viene lista |
| 32 | `eventToPublishCode` | default | `E2E-TOPUB-01` | `E2E-TOPUB-01` | viene lista |
| 33 | `smallEventCode` | default | `E2E-SMALL-01` | `E2E-SMALL-01` | viene lista |
| 34 | `adultEventCode` | default | `E2E-ADULT-01` | `E2E-ADULT-01` | viene lista |
| 35 | `newEventCode` | default | `E2E-{{runId}}-01` | No tocar (`E2E-{{runId}}-01`) | viene lista |
| 36 | `ticketCancelableCode` | default | *(vacío)* | `ticketCode` de la compra T1 (S071) | copiar a mano |
| 37 | `ticketUsableCode` | default | *(vacío)* | `ticketCode` de la compra T2 (S079) | copiar a mano |
| 38 | `expectedIssuer` | default | `pulsepass-api` | `pulsepass-api` | viene lista |
| 39 | `expectedTokenTtlSeconds` | default | `900` | `900` | viene lista |
| 40 | `expectedForeignResourceStatus` | default | `403` | `403` | viene lista |

> Los tokens duran 15 minutos: cuando uno venza, repite el login del actor y vuelve a pegar el `accessToken` en su variable (son cuatro: `userAToken`, `userBToken`, `staffToken`, `adminToken`).


## Orden de los access tokens (dónde y cuándo se llena cada uno)

Todo token va **solo en *Current value*** de la variable. Con PowerShell: `.\tests\e2e\scripts\02-Obtener-Tokens.ps1 -Pegar` hace los logins, copia cada token al portapapeles **en este orden** y te dice a qué variable pegarlo.

| Orden | Antes de | Variable | Qué contiene | Comando |
|---|---|---|---|---|
| 0 | S001 | `runId`, `*Password` | Código de corrida y 5 claves | `02-Obtener-Tokens.ps1 -SoloAmbiente -RunId ra` (luego *Import*) |
| 1 | S005 | `artistId` | `id` de *Solar Beat* de la respuesta de S005 | a mano (Postman) |
| 2 | S022 | `userAToken` | `accessToken` de A (requiere S016–S021 hechos) | `02-Obtener-Tokens.ps1 -Pegar` |
| 3 | S024 | `userBToken` | `accessToken` de B | (mismo comando) |
| 4 | S025 | `staffToken` | `accessToken` de STAFF | (mismo comando) |
| 5 | S026 | `adminToken` | `accessToken` de ADMIN | (mismo comando) |
| 6 | S027 | `injectedToken` | `accessToken` del usuario inyectado | (mismo comando) |
| 7 | S037 | `tamperedToken` | `userAToken` con 1 carácter cambiado | lo genera el mismo comando |
| 8 | S038 | `forgedToken` | JWT firmado con clave inventada | lo genera `-SoloAmbiente` |
| 9 | S041 | `inactiveToken` | `accessToken` de INACTIVE, **antes** de desactivarlo | `02-Obtener-Tokens.ps1 -Actores INACTIVE -Pegar` y después `04-Sql-Auxiliar.ps1 -Accion DesactivarInactivo` |
| 10 | S072 | `ticketCancelableCode` | `ticketCode` de la compra S071 | a mano (Postman) |
| 11 | S080 | `ticketUsableCode` | `ticketCode` de la compra S079 | a mano (Postman) |
| 12 | S118 | `expiredToken` | Copia del primer `userAToken` (el script la guarda sola); debe tener más de 15 min | no tocar |

**Renovar:** los tokens duran 15 min. Cuando una petición protegida dé **401 inesperado**, o antes de S116, ejecuta `02-Obtener-Tokens.ps1 -Actores A,B,STAFF,ADMIN -Pegar` y pega de nuevo. `03-Verificar-Ambiente.ps1` muestra cuántos minutos le quedan a cada token.
