# PulsePass

Plataforma para descubrir eventos y administrar entradas (conciertos, festivales, conferencias, eventos universitarios, deportivos y culturales).

**Java 21 · Spring Boot 4 · Spring Data JPA · Hibernate · Flyway · PostgreSQL · MapStruct · Spring Security · JJWT · JUnit 5 · Mockito · AssertJ · Testcontainers**

**Desarrolladores:** Jesus Gonzalez (2023214046) · Anuar Hatum (2023214056)

## Estado del proyecto

| Capa | Estado | Pruebas |
|---|---|---|
| Persistencia (entidades, repositories, Flyway) | Implementada | Integración con Testcontainers (PostgreSQL real) |
| Servicios (reglas de negocio, DTOs, mappers) | Implementada | Unitarias con Mockito y AssertJ |
| Controladores REST (endpoints, validación, errores) | Implementada | `@WebMvcTest` + MockMvc con Services mockeados |
| Seguridad (JWT, roles, autorización por propiedad) | Implementada | Unitarias, `@WebMvcTest` con seguridad real e integración con Testcontainers |

## Arquitectura

```
Spring Security    → filtro JWT, roles, 401/403 (JwtAuthenticationFilter, SecurityConfig)
      ↓
Controller (REST)  → Bean Validation, @PreAuthorize, códigos HTTP, GlobalExceptionHandler
      ↓
   DTOs (record)
      ↓
Service (interfaz + implementación)
      ├── Repository   → acceso a datos
      ├── Mapper       → Entity → DTO (MapStruct)
      ├── Reglas de negocio
      └── Transacciones
      ↓
Entity (JPA)
      ↓
PostgreSQL (esquema versionado con Flyway)
```

Cada capa solo habla con la inmediatamente inferior:

- **Controller** traduce HTTP a llamadas al Service y devuelve DTOs; no contiene reglas de negocio ni accede a repositories.
- **Repository** accede a los datos y no contiene reglas de negocio.
- **Service** decide si una operación está permitida (por ejemplo, si un usuario puede comprar una entrada) y nunca devuelve entidades JPA.
- **DTO / Mapper** separan el modelo persistente del contrato que verán las capas externas.

## Modelo de datos

```
Venue 1 ──── N Event
Event N ──── M Artist        (tabla intermedia: event_artists)
User  1 ──── 1 UserProfile
User  1 ──── 1 UserCredential   (hash de contraseña)
User  1 ──── N UserRole         (USER, STAFF, ADMIN)
User  1 ──── N Ticket
Event 1 ──── N Ticket
```

| Entidad | Descripción |
|---|---|
| `Venue` | Recinto donde se realiza un evento (código, ciudad, capacidad, activo) |
| `Event` | Evento con categoría, estado, fecha y edad mínima |
| `Artist` | Artista que participa en uno o varios eventos |
| `User` | Usuario de la plataforma |
| `UserProfile` | Perfil individual del usuario (1:1) |
| `UserCredential` | Hash BCrypt de la contraseña (nunca la contraseña) |
| `UserRole` | Rol de un usuario: `USER`, `STAFF` o `ADMIN` |
| `Ticket` | Entrada de un usuario para un evento, con tipo, precio y estado propios |

`Ticket` es una entidad y no un `@ManyToMany` simple porque tiene datos propios (`ticketCode`, `type`, `price`, `status`, `purchaseDate`).

**Enums** (persistidos con `EnumType.STRING`, nunca por ordinal):
`EventCategory`, `EventStatus` (DRAFT, PUBLISHED, SOLD_OUT, CANCELLED, FINISHED), `TicketType` (GENERAL, VIP, BACKSTAGE, STUDENT), `TicketStatus` (RESERVED, PAID, CANCELLED, USED).

### Integridad en PostgreSQL

| Regla | Mecanismo |
|---|---|
| Códigos y credenciales únicos | `UNIQUE` en `venues.code`, `events.event_code`, `artists.stage_name`, `users.username`, `users.email`, `tickets.ticket_code` |
| Capacidad y precio válidos | `CHECK (capacity > 0)`, `CHECK (price >= 0)` |
| Relación 1:1 usuario-perfil | FK `user_profiles.user_id` con `UNIQUE` |
| Relación N:M evento-artista | PK compuesta `(event_id, artist_id)` en `event_artists` |
| Estados y catálogos válidos | `CHECK` sobre `category`, `status` y `type` |

## Flyway

Flyway es el único responsable de crear y evolucionar el esquema. Hibernate trabaja con `ddl-auto: validate`: solo comprueba que las entidades coincidan con las tablas.

| Migración | Objetivo |
|---|---|
| `V1__create_schema.sql` | Crea las 7 tablas con PK, FK, UNIQUE, CHECK e índices |
| `V2__insert_initial_artists.sql` | Catálogo inicial: Solar Beat, Neon Waves, Caribbean Sound, Ocean Drive, Digital Pulse |
| `V3__add_streaming_url_to_event.sql` | Agrega `streaming_url` (nullable) a `events` sin modificar V1 |
| `V4__security_credentials_roles.sql` | Crea `user_credentials` y `user_roles`; los usuarios existentes reciben rol `USER` sin credencial |

## Capa de servicios

Cada servicio tiene interfaz e implementación `@Service`, con inyección por constructor. Las lecturas usan `@Transactional(readOnly = true)` y las escrituras `@Transactional`.

| Servicio | Operaciones |
|---|---|
| `VenueService` | `findByCode`, `findActiveVenues` |
| `ArtistService` | `findById`, `findByStageName`, `findActiveArtists` |
| `UserService` | `register`, `findByEmail`, `findByUsername` |
| `EventService` | `create`, `findByCode`, `findPublishedEvents`, `publish`, `addArtist`, `findByArtist` |
| `TicketService` | `purchase`, `findByCode`, `findByUserEmail`, `findPaidTicketsByEvent`, `cancel`, `markAsUsed` |

### Reglas de negocio principales

**Eventos**
- No pueden existir dos eventos con el mismo `eventCode`.
- El venue debe existir y estar activo.
- La fecha debe ser futura y `minimumAge >= 0`.
- Todo evento nuevo inicia en `DRAFT`; el request no controla el estado.
- Solo se publica un evento en `DRAFT`, con fecha futura y venue activo.
- No se puede asociar dos veces el mismo artista ni agregar artistas a eventos `CANCELLED` o `FINISHED`.

**Usuarios**
- `username` único y `email` único ignorando mayúsculas.
- `birthDate` no puede ser futura.
- `User` y `UserProfile` se crean en la misma transacción; el usuario nuevo inicia activo.

**Tickets**
- El usuario debe existir y estar activo.
- El evento debe existir, estar `PUBLISHED` y tener fecha futura.
- Si el evento tiene edad mínima, se valida con `UserProfile.birthDate` evaluada en la fecha del evento.
- Se valida capacidad: no se vende si `paidTickets >= venue.capacity`.
- Si la compra completa la capacidad, el evento pasa a `SOLD_OUT` en la misma transacción.
- Una compra válida genera un ticket `PAID`.
- Solo un ticket `PAID` puede cancelarse (y antes de la fecha del evento) o marcarse como usado (`PAID → USED`).
- Un ticket `CANCELLED` nunca puede usarse.

### Estrategia de precio

El cliente no envía el precio. El servicio lo calcula con `BigDecimal` a partir de un precio base y el tipo de ticket:

| Tipo | Precio |
|---|---|
| `GENERAL` | precio base |
| `STUDENT` | 50 % del precio base |
| `VIP` | 2 × precio base |
| `BACKSTAGE` | 3 × precio base |

### DTOs y mappers

- Los DTOs son `record` inmutables, organizados en `dto/request` y `dto/response`.
- Ningún servicio expone entidades JPA.
- MapStruct (`componentModel = "spring"`) transforma Entity → DTO. Por ejemplo, `TicketMapper` aplana `ticket.user.email`, `ticket.event.eventCode` y `ticket.event.name`.

### Excepciones

| Excepción | Cuándo se lanza |
|---|---|
| `ResourceNotFoundException` | El recurso solicitado no existe |
| `DuplicateResourceException` | Conflicto de unicidad (username, email, eventCode) |
| `BusinessRuleException` | El recurso existe, pero la operación viola una regla |

## Estructura del proyecto

```
src/main/java/com/pulsepass/platform
├── PulsepassApplication.java
├── domain/        entidades y enums
├── repository/    JpaRepository, Query Methods y JPQL
├── dto/
│   ├── request/   CreateEventRequest, RegisterUserRequest, PurchaseTicketRequest, LoginRequest
│   └── response/  VenueResponse, EventResponse, EventSummaryResponse,
│                  ArtistResponse, UserResponse, TicketResponse, ErrorResponse,
│                  AuthResponse, AuthenticatedUserResponse
├── mapper/        VenueMapper, ArtistMapper, EventMapper, UserMapper, TicketMapper
├── controller/    VenueController, EventController, ArtistController,
│                  UserController, TicketController, AuthController
├── security/      SecurityConfig, JwtService, JwtProperties, JwtAuthenticationFilter,
│                  SecurityPrincipal, CustomUserDetailsService,
│                  RestAuthenticationEntryPoint, RestAccessDeniedHandler
├── exception/     ResourceNotFound, BusinessRule, DuplicateResource, GlobalExceptionHandler
└── service/
    ├── (interfaces)  incluye AuthService y AuthorizationService
    └── impl/         implementaciones @Service

src/main/resources
├── application.yml
└── db/migration/  V1, V2, V3, V4

src/test/java/com/pulsepass/platform
├── PersistenceIntegrationTest.java
├── SecurityPersistenceIntegrationTest.java
├── AuthFlowIntegrationTest.java
├── service/       tests unitarios de cada servicio (incluye AuthServiceImplTest)
├── controller/    tests MockMvc de cada controller (incluye AuthControllerTest)
├── security/      JwtService, filtro, UserDetailsService, AuthorizationService y matriz HTTP
└── support/       configuración compartida de pruebas con seguridad
```



## Capa de controladores (API REST)

Los controllers son delgados: reciben HTTP, validan la estructura del request con Bean Validation y delegan en el Service. No acceden a repositories, no contienen reglas de negocio y solo devuelven DTOs (nunca entidades JPA).

### Endpoints

| Recurso | Método y ruta | Service | Éxito |
|---|---|---|---|
| Venues | `GET /api/venues/{code}` | `findByCode` | 200 |
| | `GET /api/venues/active` | `findActiveVenues` | 200 |
| Eventos | `POST /api/events` | `create` | 201 |
| | `GET /api/events/{eventCode}` | `findByCode` | 200 |
| | `GET /api/events/published` | `findPublishedEvents` | 200 |
| | `PATCH /api/events/{eventCode}/publish` | `publish` | 200 |
| | `POST /api/events/{eventCode}/artists/{artistId}` | `addArtist` | 200 |
| | `GET /api/events/by-artist?stageName=` | `findByArtist` | 200 |
| | `GET /api/events/{eventCode}/tickets/paid` | `findPaidTicketsByEvent` | 200 |
| Artistas | `GET /api/artists/{id}` | `findById` | 200 |
| | `GET /api/artists/by-stage-name?stageName=` | `findByStageName` | 200 |
| | `GET /api/artists/active` | `findActiveArtists` | 200 |
| Usuarios | `POST /api/users` | `register` | 201 |
| | `GET /api/users/by-email?email=` | `findByEmail` | 200 |
| | `GET /api/users/by-username?username=` | `findByUsername` | 200 |
| Auth | `POST /api/auth/login` | `AuthService.login` | 200 |
| | `GET /api/auth/me` | usuario autenticado | 200 |
| Tickets | `POST /api/tickets` | `purchase` | 201 |
| | `GET /api/tickets/{ticketCode}` | `findByCode` | 200 |
| | `GET /api/tickets/by-user?email=` | `findByUserEmail` | 200 |
| | `PATCH /api/tickets/{ticketCode}/cancel` | `cancel` | 200 |
| | `PATCH /api/tickets/{ticketCode}/use` | `markAsUsed` | 200 |

Los 20 métodos públicos de `VenueService`, `ArtistService`, `UserService`, `EventService` y `TicketService` tienen un endpoint; el acceso a cada uno se detalla en la sección Seguridad. El endpoint de tickets pagados por evento vive en `TicketController` porque usa `TicketService`.

### Validación de entrada

La validación estructural (campo obligatorio, formato de email, longitud, valor mínimo, JSON válido) ocurre en el Controller con `@Valid` y se rechaza con 400 antes de llegar al Service. Las reglas de negocio (usuario activo, evento publicado, edad mínima, capacidad, transiciones de estado) siguen siendo responsabilidad del Service.

| Request | Validaciones |
|---|---|
| `CreateEventRequest` | `eventCode`, `name`, `venueCode` obligatorios; `category`, `eventDate` obligatorios; `minimumAge` obligatorio y `>= 0`; `description` máx. 1000 caracteres |
| `RegisterUserRequest` | `username`, `email`, `firstName`, `lastName` obligatorios; `email` con formato válido; `birthDate` obligatorio; `password` obligatoria, de 12 a 72 caracteres y máximo 72 bytes en UTF-8 |
| `LoginRequest` | `identifier` (correo o username) obligatorio, máx. 150; `password` obligatoria, máx. 72 |
| `PurchaseTicketRequest` | `userEmail` obligatorio y con formato de email; `eventCode` obligatorio; `type` obligatorio |

### Manejo de errores

Un único `GlobalExceptionHandler` (`@RestControllerAdvice`) convierte cualquier excepción en un `ErrorResponse` uniforme, sin repetir `try/catch` en los controllers:

```json
{
  "timestamp": "2026-10-10T16:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "eventCode": "Event code is required",
    "venueCode": "Venue code is required"
  }
}
```

| Causa | HTTP |
|---|---|
| Bean Validation, JSON mal formado, enum inválido, parámetro faltante o de tipo incorrecto | 400 |
| Sin token, token inválido/expirado o credenciales incorrectas | 401 |
| Autenticado pero sin permiso (rol o propiedad) | 403 |
| `ResourceNotFoundException` y rutas inexistentes | 404 |
| Método HTTP no soportado | 405 |
| `DuplicateResourceException`, `BusinessRuleException` | 409 |
| Error inesperado (no se exponen detalles internos) | 500 |

Resumen de códigos de éxito: `201 Created` en las creaciones (evento, usuario, ticket) y `200 OK` en consultas y cambios de estado.

## Seguridad (Spring Security + JJWT)

La API es *stateless*: no hay sesión ni cookies. La identidad viaja en el encabezado `Authorization: Bearer <token>`.

### Autenticación

- **Token:** JWT firmado con HS256 (JJWT 0.13.0). Claims: `sub` (id del usuario), `iss`, `iat`, `exp` y `jti`.
- **Duración:** 15 minutos. No hay refresh token ni lista de revocación: al vencer, se vuelve a iniciar sesión.
- **Validación:** se exige algoritmo HS256, emisor esperado, `exp` y `sub`; cualquier otro token se rechaza con 401.
- **Contraseñas:** BCrypt, solo se guarda el hash. BCrypt solo usa los primeros 72 **bytes**, por eso la contraseña se limita a 72 bytes en UTF-8.
- **Login genérico:** usuario inexistente, contraseña incorrecta o usuario sin credencial responden con el mismo mensaje, para no revelar qué usuarios existen.
- **Secretos fuera del código:** la clave de firma se lee de la variable de entorno `PULSEPASS_JWT_SECRET_BASE64` (mínimo 32 bytes en Base64). Sin ella, o con una clave débil, la aplicación **no arranca**.

```powershell
# Generar un secreto de 32 bytes
$b = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
[Convert]::ToBase64String($b)
```

| Variable | Obligatoria | Descripción |
|---|---|---|
| `PULSEPASS_JWT_SECRET_BASE64` | Sí | Clave HS256 en Base64 (≥ 32 bytes) |
| `PULSEPASS_JWT_ISSUER` | No | Emisor del token (por defecto `pulsepass-api`) |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | No | Conexión a PostgreSQL (hay valores por defecto locales) |

Ver `.env.example`. En IntelliJ se definen en *Run → Edit Configurations → Environment variables*. El archivo `.env` está en `.gitignore`: nunca se sube.

### Roles

| Rol | Alcance |
|---|---|
| `USER` | Rol de todo registro nuevo: opera sobre sus propios datos |
| `STAFF` | Además, valida tickets y consulta tickets pagados y tickets ajenos |
| `ADMIN` | Gestiona eventos y puede actuar sobre datos de otros usuarios |

No existe endpoint público para crear `STAFF` o `ADMIN`: se asignan en la base de datos (tabla `user_roles`).

### Acceso por endpoint

| Endpoint | Acceso |
|---|---|
| `POST /api/auth/login`, `POST /api/users` | Público |
| `GET /api/venues/**`, `GET /api/artists/**` | Público |
| `GET /api/events/{eventCode}`, `/published`, `/by-artist` | Público (los eventos que no están `PUBLISHED` se ven como 404, salvo para STAFF/ADMIN) |
| `GET /api/auth/me` | Cualquier usuario autenticado |
| `POST /api/tickets` | Autenticado, solo a nombre de sí mismo |
| `GET /api/tickets/{ticketCode}` | Dueño del ticket, STAFF o ADMIN |
| `GET /api/tickets/by-user`, `GET /api/users/by-email`, `GET /api/users/by-username` | Dueño de los datos o ADMIN |
| `PATCH /api/tickets/{ticketCode}/cancel` | Dueño del ticket o ADMIN |
| `PATCH /api/tickets/{ticketCode}/use`, `GET /api/events/{eventCode}/tickets/paid` | STAFF o ADMIN |
| `POST /api/events`, `PATCH /api/events/{eventCode}/publish`, `POST /api/events/{eventCode}/artists/{artistId}` | ADMIN |
| Cualquier otra ruta | Autenticado (denegar por defecto) |

### Autorización por propiedad (anti-IDOR)

Los roles no bastan: un `USER` no debe ver los tickets de otro. `AuthorizationService` compara el **id persistente** del usuario autenticado (el que viaja en el token) con el dueño del recurso, y se aplica con `@PreAuthorize("@authorizationService...")` en los controllers. Nunca se confía en el correo que llega en la petición para decidir quién es el usuario.

### Errores 401 y 403

`RestAuthenticationEntryPoint` y `RestAccessDeniedHandler` delegan en `GlobalExceptionHandler`, así que 401 y 403 usan el mismo `ErrorResponse` que el resto de la API. El mensaje no revela si falló el token, el usuario o la contraseña.

### Decisiones y límites conocidos

- Los usuarios anteriores a la Fase 4 quedan con rol `USER` y **sin credencial**: no pueden iniciar sesión hasta que se les asigne una contraseña de forma segura. No se inventó ninguna contraseña por defecto.
- STAFF puede validar cualquier ticket: el modelo no relaciona al personal con eventos concretos.
- Sin refresh token, sin revocación, sin CORS configurado y sin registro de cada petición.
- `PasswordEncoder`, `AuthenticationManager` y la cadena de filtros se definen en `SecurityConfig`; CSRF está desactivado porque no hay cookies de sesión.

## Pruebas

El proyecto tiene varios tipos de pruebas con objetivos distintos.

### Pruebas unitarias de servicios

- JUnit 5 + Mockito + AssertJ, con `@ExtendWith(MockitoExtension.class)`.
- Repositories y mappers se reemplazan por mocks.
- No levantan PostgreSQL, ni Testcontainers, ni el contexto de Spring.
- Usan `when(...)`, `verify(...)` y `verify(..., never()).save(...)` en los caminos inválidos, para demostrar que una operación que viola una regla nunca se persiste.
- Cubren los caminos felices y de error de los cinco servicios, incluidas la compra del último ticket (cambio a `SOLD_OUT`) y las transiciones de estado del ticket.

Para ejecutar solo estas pruebas (no requieren Docker):

```bash
.\mvnw.cmd test -Dtest="*ServiceImplTest"
```

### Pruebas de controladores

- `@WebMvcTest` + `MockMvc`, con cada Service reemplazado por `@MockitoBean`.
- No levantan PostgreSQL, ni Testcontainers, ni el contexto completo de Spring: no requieren Docker.
- Validan el status HTTP, el `Content-Type`, los campos JSON relevantes con `jsonPath` y el contrato `ErrorResponse`.
- Usan `verify(...)` para comprobar la llamada al Service y `verify(..., never())` cuando un request inválido (400) no debe llegar a él.
- Cubren los caminos 200/201, 400, 404, 409 y 500 de los cinco controllers.

```bash
.\mvnw.cmd test -Dtest="*ControllerTest"
```

### Pruebas de integración de persistencia

`PersistenceIntegrationTest` corre contra un contenedor real de PostgreSQL (`postgres:18-alpine`) levantado por Testcontainers con `@ServiceConnection`. Verifica que Flyway aplique V1–V3, las relaciones 1:N, 1:1 y N:M, los Query Methods, las consultas JPQL y las constraints reales (UNIQUE, CHECK, 1:1). Requiere Docker.

### Pruebas de seguridad

- **Unitarias:** `JwtServiceTest` (token válido, expirado, manipulado, de otro emisor, `alg:none`, secreto débil), `CustomUserDetailsServiceTest`, `AuthorizationServiceImplTest`, `JwtAuthenticationFilterTest` y `AuthServiceImplTest`.
- **`@WebMvcTest` con la seguridad real:** `AuthControllerTest` y `SecurityHttpMatrixTest`, una matriz endpoint × identidad (anónimo, dueño, ajeno, STAFF, ADMIN, token inválido o expirado) que verifica 200/401/403.
- **Integración con PostgreSQL real:** `SecurityPersistenceIntegrationTest` (V4) y `AuthFlowIntegrationTest` (registro → login → `/me` → acceso a recursos propios y ajenos). Requieren Docker.
- Los tests de los controladores funcionales importan `WebMvcSecurityTestConfig` y `AllowAllAuthorizationTestConfig` y usan `@WithMockUser(roles = "ADMIN")`, para seguir verificando solo códigos HTTP y DTOs.
- Los tests usan una clave JWT ficticia definida en `src/test/resources/application.properties`; nunca usar esa clave fuera de las pruebas.

```bash
.\mvnw.cmd test -Dtest="Jwt*Test,Auth*Test,Security*Test,CustomUserDetailsServiceTest"
```

### Suite completa

```bash
.\mvnw.cmd clean test
```