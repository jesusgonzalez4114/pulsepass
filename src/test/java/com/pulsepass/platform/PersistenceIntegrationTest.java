package com.pulsepass.platform;

import com.pulsepass.platform.domain.*;
import com.pulsepass.platform.repository.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("pulsepass_test")
                    .withUsername("pulsepass")
                    .withPassword("pulsepass");

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void flywayExecutedMigrations() {
        var versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history ORDER BY installed_rank",
                String.class
        );
        assertThat(versions).contains("1", "2", "3");
    }

    @Test
    void oneToManyVenueEvents() {
        Venue venue = venueRepository.save(
                new Venue("VEN-T1", "Test Arena", "Bogota", "Calle 1", 1000));

        Event event1 = new Event("EVT-T1-A", "Event A", "desc",
                EventCategory.MUSIC, EventStatus.DRAFT, LocalDateTime.now().plusDays(10), 0);
        Event event2 = new Event("EVT-T1-B", "Event B", "desc",
                EventCategory.MUSIC, EventStatus.DRAFT, LocalDateTime.now().plusDays(20), 0);

        venue.addEvent(event1);
        venue.addEvent(event2);

        eventRepository.save(event1);
        eventRepository.save(event2);

        List<Event> events = eventRepository.findByVenueCode("VEN-T1");

        assertThat(events).hasSize(2);
        assertThat(events).allMatch(e -> e.getVenue().getId().equals(venue.getId()));
    }

    @Test
    void oneToOneUserProfile() {
        User user = userRepository.save(new User("user_t1", "user.t1@mail.com"));

        UserProfile profile = new UserProfile(
                "Pedro", "Perez", "3224390696", "Santa Marta", null);

        user.assignProfile(profile);
        User savedUser = userRepository.saveAndFlush(user);

        assertThat(savedUser.getProfile().getId()).isNotNull();
        assertThat(savedUser.getProfile().getUser().getId()).isEqualTo(savedUser.getId());
    }

    //  Un segundo perfil para el mismo usuario debe fallar
    @Test
    void shouldRejectSecondProfileForSameUser() {
        User user = userRepository.save(new User("user_t2", "user.t2@mail.com"));

        UserProfile profile1 = new UserProfile("Carlos", "Ruiz", "3000000001", "Bogota", null);
        user.assignProfile(profile1);
        userRepository.saveAndFlush(user);

        UserProfile profile2 = new UserProfile("Carlos", "Ruiz", "3000000002", "Bogota", null);
        profile2.setUser(user);

        assertThatThrownBy(() -> userProfileRepository.saveAndFlush(profile2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void manyToManyEventArtists() {
        Venue venue = venueRepository.save(
                new Venue("VEN-T2", "Test Venue 2", "Medellin", "Calle 2", 2000));

        Event event = new Event("EVT-T2", "Multi Artist Event", "desc",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(30), 0);
        venue.addEvent(event);
        eventRepository.save(event);

        Artist artist1 = artistRepository.findByStageNameIgnoreCase("Solar Beat")
                .orElseThrow(() -> new IllegalStateException("Solar Beat no existe, revisa V2"));
        Artist artist2 = artistRepository.findByStageNameIgnoreCase("Neon Waves")
                .orElseThrow(() -> new IllegalStateException("Neon Waves no existe, revisa V2"));
        Artist artist3 = artistRepository.findByStageNameIgnoreCase("Caribbean Sound")
                .orElseThrow(() -> new IllegalStateException("Caribbean Sound no existe, revisa V2"));

        event.addArtist(artist1);
        event.addArtist(artist2);
        event.addArtist(artist3);

        Event savedEvent = eventRepository.saveAndFlush(event);

        assertThat(savedEvent.getArtists()).hasSize(3);
    }

    // Test Ticket -> User y Ticket -> Event
    @Test
    void ticketReferencesUserAndEvent() {
        Venue venue = venueRepository.save(
                new Venue("VEN-T3", "Test Venue 3", "Cali", "Calle 3", 500));

        Event event = new Event("EVT-T3", "Ticket Test Event", "desc",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(5), 0);
        venue.addEvent(event);
        eventRepository.save(event);

        User user = userRepository.save(new User("user_t3", "user.t3@mail.com"));

        Ticket ticket = new Ticket(
                "TCK-T3-001", TicketType.GENERAL, TicketStatus.PAID,
                new BigDecimal("120000"), LocalDateTime.now(), user, event);

        Ticket savedTicket = ticketRepository.saveAndFlush(ticket);

        assertThat(savedTicket.getUser().getUsername()).isEqualTo("user_t3");
        assertThat(savedTicket.getEvent().getEventCode()).isEqualTo("EVT-T3");
    }

    //  Query Methods (eventos publicados ordenados, tickets por usuario)
    @Test
    void queryMethodPublishedEventsOrdered() {
        Venue venue = venueRepository.save(
                new Venue("VEN-T4", "Test Venue 4", "Cartagena", "Calle 4", 800));

        Event e1 = new Event("EVT-T4-A", "Early", "desc", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(1), 0);
        Event e2 = new Event("EVT-T4-B", "Late", "desc", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.now().plusDays(50), 0);
        Event e3 = new Event("EVT-T4-C", "Draft", "desc", EventCategory.MUSIC,
                EventStatus.DRAFT, LocalDateTime.now().plusDays(10), 0);

        venue.addEvent(e1);
        venue.addEvent(e2);
        venue.addEvent(e3);

        eventRepository.save(e1);
        eventRepository.save(e2);
        eventRepository.save(e3);

        List<Event> published = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);

        assertThat(published).hasSize(2);
        assertThat(published.get(0).getEventCode()).isEqualTo("EVT-T4-A");
        assertThat(published.get(1).getEventCode()).isEqualTo("EVT-T4-B");
    }

    // Test JPQL (eventos por artista + conteo de tickets pagados)
    @Test
    void jpqlEventsByArtistAndPaidTicketCount() {
        Venue venue = venueRepository.save(
                new Venue("VEN-T5", "Test Venue 5", "Santa Marta", "Calle 5", 3000));

        Event event = new Event("EVT-T5", "JPQL Test Event", "desc",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(15), 0);
        venue.addEvent(event);
        eventRepository.save(event);

        Artist artist = artistRepository.findByStageNameIgnoreCase("Digital Pulse")
                .orElseThrow(() -> new IllegalStateException("Digital Pulse no existe, revisa V2"));
        event.addArtist(artist);
        eventRepository.saveAndFlush(event);

        List<Event> foundEvents = eventRepository.findByArtistStageName("Digital Pulse");
        assertThat(foundEvents).extracting(Event::getEventCode).contains("EVT-T5");

        User user1 = userRepository.save(new User("user_t5a", "user.t5a@mail.com"));
        User user2 = userRepository.save(new User("user_t5b", "user.t5b@mail.com"));

        ticketRepository.save(new Ticket("TCK-T5-001", TicketType.GENERAL, TicketStatus.PAID,
                new BigDecimal("100000"), LocalDateTime.now(), user1, event));
        ticketRepository.save(new Ticket("TCK-T5-002", TicketType.VIP, TicketStatus.PAID,
                new BigDecimal("200000"), LocalDateTime.now(), user2, event));
        ticketRepository.saveAndFlush(new Ticket("TCK-T5-003", TicketType.GENERAL, TicketStatus.CANCELLED,
                new BigDecimal("100000"), LocalDateTime.now(), user1, event));

        long paidCount = ticketRepository.countPaidTicketsByEventCode("EVT-T5");
        assertThat(paidCount).isEqualTo(2);
    }

    //Test de UNIQUE (ticketCode duplicado)
    @Test
    void uniqueConstraintOnTicketCode() {
        Venue venue = venueRepository.save(
                new Venue("VEN-T6", "Test Venue 6", "Bogota", "Calle 6", 1500));

        Event event = new Event("EVT-T6", "Unique Test Event", "desc",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(7), 0);
        venue.addEvent(event);
        eventRepository.save(event);

        User user = userRepository.save(new User("user_t6", "user.t6@mail.com"));

        ticketRepository.saveAndFlush(new Ticket(
                "TCK-DUP-001", TicketType.GENERAL, TicketStatus.PAID,
                new BigDecimal("120000"), LocalDateTime.now(), user, event));

        Ticket duplicate = new Ticket(
                "TCK-DUP-001", TicketType.VIP, TicketStatus.RESERVED,
                new BigDecimal("250000"), LocalDateTime.now(), user, event);

        assertThatThrownBy(() -> ticketRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}