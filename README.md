# PulsePass

Plataforma para descubrir eventos y administrar entradas (conciertos, festivales, conferencias, eventos universitarios, deportivos y culturales).

**Java 21 · Spring Boot 4 · Spring Data JPA · Hibernate · Flyway · PostgreSQL · MapStruct · JUnit 5 · Mockito · AssertJ · Testcontainers**

**Desarrolladores:** Jesus Gonzalez (2023214046) · Anuar Hatum (2023214056)

## Estado del proyecto

| Capa | Estado | Pruebas |
|---|---|---|
| Persistencia (entidades, repositories, Flyway) | Implementada | Integración con Testcontainers (PostgreSQL real) |
| Servicios (reglas de negocio, DTOs, mappers) | Implementada | Unitarias con Mockito y AssertJ |

## Arquitectura

```
Controller (futuro)
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
│                  ArtistResponse, UserResponse, TicketResponse
├── mapper/        VenueMapper, ArtistMapper, EventMapper, UserMapper, TicketMapper
├── exception/     ResourceNotFound, BusinessRule, DuplicateResource
└── service/
    ├── (interfaces)
    └── impl/      implementaciones @Service

src/main/resources
├── application.yml
└── db/migration/  V1, V2, V3

src/test/java/com/pulsepass/platform
├── PersistenceIntegrationTest.java
└── service/       tests unitarios de cada servicio
```



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

### Pruebas de integración de persistencia

`PersistenceIntegrationTest` corre contra un contenedor real de PostgreSQL (`postgres:18-alpine`) levantado por Testcontainers con `@ServiceConnection`. Verifica que Flyway aplique V1–V3, las relaciones 1:N, 1:1 y N:M, los Query Methods, las consultas JPQL y las constraints reales (UNIQUE, CHECK, 1:1). Requiere Docker.

### Suite completa

```bash
.\mvnw.cmd clean test
```



