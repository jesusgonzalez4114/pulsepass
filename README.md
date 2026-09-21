# PulsePass

Implementar la capa de persistencia proyecto PulsePass — plataforma de eventos, artistas y entradas.
**Java 21 · Spring Boot 4 · Spring Data JPA · Hibernate · Flyway · PostgreSQL · Testcontainers**

## Desarolladores

Jesus Gonzalez-2023214046

Anuar Hatum-2023214056

## Descripción

PulsePass es el núcleo de datos de una plataforma para descubrir eventos y administrar entradas de conciertos, festivales, conferencias y actividades culturales. Este proyecto implementa exclusivamente la **capa de persistencia** definida en el PRD v1.0: modelo relacional, migraciones versionadas, entidades JPA, repositories y consultas, validados con pruebas de integración contra PostgreSQL real.

No incluye API REST, capa Service, autenticación, pagos ni frontend — están explícitamente fuera de alcance del MVP académico (sección 2.4 del PRD).

## Modelo de datos

```
Venue 1 ──── N Event
Event N ──── M Artist        (tabla intermedia: event_artists)
User  1 ──── 1 UserProfile
User  1 ──── N Ticket
Event 1 ──── N Ticket
```

### Entidades

| Entidad | Descripción |
|---|---|
| `Venue` | Recintos donde se realizan los eventos |
| `Event` | Eventos (conciertos, festivales, conferencias) |
| `Artist` | Artistas que participan en eventos |
| `User` | Usuarios de la plataforma |
| `UserProfile` | Perfil individual de cada usuario (1:1) |
| `Ticket` | Entrada emitida a un usuario para un evento, con tipo, precio y estado propios |

**Por qué `Ticket` es una entidad y no un `@ManyToMany` simple:** un ticket contiene datos propios (`ticketCode`, `type`, `price`, `status`, `purchaseDate`) que no pertenecen ni a `User` ni a `Event` — en cuanto una relación "sabe algo" por sí misma, deja de ser una simple asociación y se convierte en una entidad de pleno derecho, con dos relaciones `@ManyToOne`.

### Enums

- `EventCategory`: MUSIC, SPORTS, TECHNOLOGY, EDUCATION, CULTURE, ENTERTAINMENT
- `EventStatus`: DRAFT, PUBLISHED, SOLD_OUT, CANCELLED, FINISHED
- `TicketType`: GENERAL, VIP, BACKSTAGE, STUDENT
- `TicketStatus`: RESERVED, PAID, CANCELLED, USED

Todos se persisten con `@Enumerated(EnumType.STRING)` — por nombre estable, nunca por ordinal (BR-008), para que el esquema no dependa del orden de declaración en Java.

## Relaciones y constraints

| Relación | Mecanismo |
|---|---|
| `Venue 1:N Event` | FK `venue_id` en `events` |
| `User 1:1 UserProfile` | FK `user_id` en `user_profiles`, con `UNIQUE` |
| `Event N:M Artist` | Tabla intermedia `event_artists`, PK compuesta `(event_id, artist_id)` |
| `Ticket → User`, `Ticket → Event` | FKs `user_id` y `event_id` en `tickets`, ambas `NOT NULL` |

Constraints reforzados en PostgreSQL: `UNIQUE` en `venues.code`, `events.event_code`, `artists.stage_name`, `users.username`, `users.email`, `tickets.ticket_code`; `CHECK` en `venues.capacity > 0` y `tickets.price >= 0`.

## Requisitos previos

- Java 21
- Docker Desktop (o Docker Engine) corriendo — necesario para Testcontainers
- Maven (o el wrapper `mvnw` / `mvnw.cmd` incluido)

## Cómo ejecutar

```bash
./mvnw clean install        # Mac/Linux
.\mvnw.cmd clean install    # Windows
```

Para correr contra un PostgreSQL real (no necesario para los tests), configura `DB_URL`, `DB_USER`, `DB_PASSWORD`, o usa los valores por defecto de `application.yml` (`localhost:5432/pulsepass`).

## Cómo ejecutar los tests

```bash
./mvnw test        # Mac/Linux
.\mvnw.cmd test     # Windows
```

Todos los tests están en `PersistenceIntegrationTest`, y corren contra un contenedor real de PostgreSQL levantado automáticamente por Testcontainers (NFR-004, NFR-005) — Docker debe estar corriendo antes de ejecutar este comando.

## Flyway

Flyway es el único responsable de crear y evolucionar el esquema (NFR-002). Se usa `ddl-auto: validate`, de modo que Hibernate únicamente valida que las entidades coincidan con las tablas ya creadas, sin poder modificarlas.

| Migración | Objetivo |
|---|---|
| `V1__create_schema.sql` | Crea las 7 tablas (`venues`, `events`, `artists`, `event_artists`, `users`, `user_profiles`, `tickets`) con PK, FK, UNIQUE, CHECK e índices |
| `V2__insert_initial_artists.sql` | Inserta el catálogo inicial: Solar Beat, Neon Waves, Caribbean Sound, Ocean Drive, Digital Pulse |
| `V3__add_streaming_url_to_event.sql` | Agrega `streaming_url` (nullable) a `events`, sin modificar V1 (FR-EVT-006) |

Una base vacía puede reconstruirse por completo ejecutando las 3 migraciones en orden (NFR-003).

## Testcontainers

Los tests de integración no usan H2: se ejecutan contra un contenedor real de PostgreSQL (`postgres:18-alpine`), levantado y destruido automáticamente por Testcontainers en cada corrida, vía `@Testcontainers` y `@ServiceConnection`. Esto permite comprobar constraints reales (UNIQUE, FK, 1:1) con la misma fidelidad que tendría el entorno de producción.

## Query Methods implementados

| Repository | Método | Requisito |
|---|---|---|
| `VenueRepository` | `findByCode(String code)` | FR-VEN-001 |
| `EventRepository` | `findByEventCode(String eventCode)` | FR-EVT-002 |
| `EventRepository` | `findByStatusOrderByEventDateAsc(EventStatus status)` | FR-EVT-005 |
| `EventRepository` | `findByVenueCode(String venueCode)` | FR-VEN-004 |
| `UserRepository` | `findByEmailIgnoreCase(String email)` | FR-USR-002 |
| `TicketRepository` | `findByUserEmailIgnoreCase(String email)` / `...AndStatus(...)` | FR-TKT-006 |
| `TicketRepository` | `findByEventEventCodeAndStatus(String eventCode, TicketStatus status)` | FR-TKT-007 |
| `ArtistRepository` | `findByStageNameIgnoreCase(String stageName)` | — |

## Consultas JPQL implementadas (`@Query`)

| Repository | Método | Requisito |
|---|---|---|
| `EventRepository` | `findByArtistStageName(String stageName)` | FR-ART-004, FR-SRC-001 |
| `EventRepository` | `findByVenueCityAndArtistStageName(String city, String stageName)` | FR-SRC-002 |
| `EventRepository` | `findRecommendedEvents(LocalDateTime afterDate, String city, String artistText)` | FR-SRC-003 |
| `TicketRepository` | `countPaidTicketsByEventCode(String eventCode)` | FR-TKT-008 |

## Reglas del taller respetadas

- `ddl-auto: validate` — Flyway crea el esquema, Hibernate solo valida.
- Sin H2 — todas las pruebas corren contra PostgreSQL real vía Testcontainers.
- Sin SQL nativo en los repositories — todas las consultas personalizadas usan JPQL.
- Sin Lombok `@Data` sobre las entidades.
- Precios modelados con `BigDecimal`/`NUMERIC`, nunca `float`/`double` (BR-007, NFR-008).