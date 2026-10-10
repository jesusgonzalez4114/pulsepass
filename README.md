# PulsePass

Plataforma para descubrir eventos y administrar entradas (conciertos, festivales, conferencias, eventos universitarios, deportivos y culturales).

**Java 21 · Spring Boot 4 · Spring Data JPA · Hibernate · Flyway · PostgreSQL · MapStruct · JUnit 5 · Mockito · AssertJ · Testcontainers**

**Desarrolladores:** Jesus Gonzalez (2023214046) · Anuar Hatum (2023214056)

## Estado del proyecto

| Capa | Estado | Pruebas |
|---|---|---|
| Persistencia (entidades, repositories, Flyway) | Implementada | Integración con Testcontainers (PostgreSQL real) |
| Servicios (reglas de negocio, DTOs, mappers) | Implementada | Unitarias con Mockito y AssertJ |
| Controladores REST (endpoints, validación, errores) | Implementada | `@WebMvcTest` + MockMvc con Services mockeados |

## Arquitectura

```
Controller (REST)  → Bean Validation, códigos HTTP, GlobalExceptionHandler
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
│   ├── request/   CreateEventRequest, RegisterUserRequest, PurchaseTicketRequest
│   └── response/  VenueResponse, EventResponse, EventSummaryResponse,
│                  ArtistResponse, UserResponse, TicketResponse, ErrorResponse
├── mapper/        VenueMapper, ArtistMapper, EventMapper, UserMapper, TicketMapper
├── controller/    VenueController, EventController, ArtistController,
│                  UserController, TicketController
├── exception/     ResourceNotFound, BusinessRule, DuplicateResource, GlobalExceptionHandler
└── service/
    ├── (interfaces)
    └── impl/      implementaciones @Service

src/main/resources
├── application.yml
└── db/migration/  V1, V2, V3

src/test/java/com/pulsepass/platform
├── PersistenceIntegrationTest.java
├── service/       tests unitarios de cada servicio
└── controller/    tests MockMvc de cada controller
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
| Tickets | `POST /api/tickets` | `purchase` | 201 |
| | `GET /api/tickets/{ticketCode}` | `findByCode` | 200 |
| | `GET /api/tickets/by-user?email=` | `findByUserEmail` | 200 |
| | `PATCH /api/tickets/{ticketCode}/cancel` | `cancel` | 200 |
| | `PATCH /api/tickets/{ticketCode}/use` | `markAsUsed` | 200 |

Los 20 métodos públicos de los Services tienen un endpoint. El endpoint de tickets pagados por evento vive en `TicketController` porque usa `TicketService`.

### Validación de entrada

La validación estructural (campo obligatorio, formato de email, longitud, valor mínimo, JSON válido) ocurre en el Controller con `@Valid` y se rechaza con 400 antes de llegar al Service. Las reglas de negocio (usuario activo, evento publicado, edad mínima, capacidad, transiciones de estado) siguen siendo responsabilidad del Service.

| Request | Validaciones |
|---|---|
| `CreateEventRequest` | `eventCode`, `name`, `venueCode` obligatorios; `category`, `eventDate` obligatorios; `minimumAge` obligatorio y `>= 0`; `description` máx. 1000 caracteres |
| `RegisterUserRequest` | `username`, `email`, `firstName`, `lastName` obligatorios; `email` con formato válido; `birthDate` obligatorio |
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
| `ResourceNotFoundException` y rutas inexistentes | 404 |
| Método HTTP no soportado | 405 |
| `DuplicateResourceException`, `BusinessRuleException` | 409 |
| Error inesperado (no se exponen detalles internos) | 500 |

Resumen de códigos de éxito: `201 Created` en las creaciones (evento, usuario, ticket) y `200 OK` en consultas y cambios de estado.

## Pruebas

El proyecto tiene dos tipos de pruebas con objetivos distintos.

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

### Suite completa

```bash
.\mvnw.cmd clean test
```