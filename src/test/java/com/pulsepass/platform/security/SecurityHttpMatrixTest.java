package com.pulsepass.platform.security;

import com.pulsepass.platform.controller.ArtistController;
import com.pulsepass.platform.controller.EventController;
import com.pulsepass.platform.controller.TicketController;
import com.pulsepass.platform.controller.UserController;
import com.pulsepass.platform.controller.VenueController;
import com.pulsepass.platform.domain.EventCategory;
import com.pulsepass.platform.domain.EventStatus;
import com.pulsepass.platform.domain.Role;
import com.pulsepass.platform.domain.Ticket;
import com.pulsepass.platform.domain.TicketStatus;
import com.pulsepass.platform.domain.TicketType;
import com.pulsepass.platform.domain.User;
import com.pulsepass.platform.dto.request.PurchaseTicketRequest;
import com.pulsepass.platform.dto.response.EventResponse;
import com.pulsepass.platform.dto.response.EventSummaryResponse;
import com.pulsepass.platform.dto.response.TicketResponse;
import com.pulsepass.platform.dto.response.UserResponse;
import com.pulsepass.platform.repository.TicketRepository;
import com.pulsepass.platform.repository.UserRepository;
import com.pulsepass.platform.service.ArtistService;
import com.pulsepass.platform.service.AuthService;
import com.pulsepass.platform.service.EventService;
import com.pulsepass.platform.service.TicketService;
import com.pulsepass.platform.service.UserService;
import com.pulsepass.platform.service.VenueService;
import com.pulsepass.platform.service.impl.AuthorizationServiceImpl;
import com.pulsepass.platform.support.WebMvcSecurityTestConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(controllers = {
        VenueController.class, ArtistController.class, UserController.class,
        EventController.class, TicketController.class})
@Import({WebMvcSecurityTestConfig.class, AuthorizationServiceImpl.class})
class SecurityHttpMatrixTest {

    private static final long ANDREA_ID = 1L;
    private static final long CARLOS_ID = 2L;
    private static final long LAURA_ID = 3L;
    private static final long ADMIN_ID = 4L;

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private CustomUserDetailsService userDetailsService;

    @MockitoBean private VenueService venueService;
    @MockitoBean private ArtistService artistService;
    @MockitoBean private UserService userService;
    @MockitoBean private AuthService authService;
    @MockitoBean private EventService eventService;
    @MockitoBean private TicketService ticketService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private TicketRepository ticketRepository;

    private static final String EVENT_BODY = """
            {
              "eventCode": "CMF-2026",
              "name": "Caribbean Music Fest 2026",
              "description": "Festival",
              "category": "MUSIC",
              "eventDate": "2026-12-20T20:00:00",
              "minimumAge": 18,
              "venueCode": "VEN-SMR-01"
            }
            """;

    @BeforeEach
    void resetIdentityMock() {

        Mockito.reset(userDetailsService);
    }

    // - helpers

    private String bearer(long userId, String username, boolean active, Role... roles) {
        when(userDetailsService.loadByUserId(userId)).thenReturn(
                new SecurityPrincipal(userId, username, username + "@mail.com", null, active, List.of(roles)));
        return "Bearer " + jwtService.generateToken(userId);
    }

    private String andrea() { return bearer(ANDREA_ID, "andrea", true, Role.USER); }
    private String laura()  { return bearer(LAURA_ID, "laura", true, Role.USER, Role.STAFF); }
    private String admin()  { return bearer(ADMIN_ID, "admin", true, Role.USER, Role.ADMIN); }

    private User userEntity(long id, String username) {
        User user = new User(username, username + "@mail.com");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void ticketOwnedBy(String code, long ownerId, String ownerName) {
        Ticket ticket = Mockito.mock(Ticket.class);
        when(ticket.getUser()).thenReturn(userEntity(ownerId, ownerName));
        when(ticketRepository.findByTicketCode(code)).thenReturn(Optional.of(ticket));
    }

    private TicketResponse ticketResponse(TicketStatus status) {
        return new TicketResponse(1L, "TCK-1", TicketType.GENERAL, new BigDecimal("100000"), status,
                LocalDateTime.of(2026, 10, 9, 10, 0), "andrea@mail.com", "CMF-2026", "Caribbean Music Fest 2026");
    }

    private EventResponse eventResponse(EventStatus status) {
        return new EventResponse(1L, "CMF-2026", "Caribbean Music Fest 2026", "Festival", EventCategory.MUSIC,
                status, LocalDateTime.of(2026, 12, 20, 20, 0), 18, "VEN-SMR-01", "Marina Convention Center", List.of());
    }

    private EventSummaryResponse summary(String code, EventStatus status) {
        return new EventSummaryResponse(code, "Evento " + code, LocalDateTime.of(2026, 12, 20, 20, 0),
                status, "Marina Convention Center");
    }

    private UserResponse userResponse(long id, String username) {
        return new UserResponse(id, username, username + "@mail.com", true, "Nombre", "Apellido", "Santa Marta");
    }

    // - rutas públicas

    // T-SEC-001
    @Test
    void anonymousCanListPublishedEvents() throws Exception {
        when(eventService.findPublishedEvents()).thenReturn(List.of(summary("CMF-2026", EventStatus.PUBLISHED)));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));
    }

    @Test
    void anonymousCanReadVenuesAndArtistsIncludingActiveRoutes() throws Exception {
        when(venueService.findActiveVenues()).thenReturn(List.of());
        when(artistService.findActiveArtists()).thenReturn(List.of());

        mockMvc.perform(get("/api/venues/active")).andExpect(status().isOk());
        mockMvc.perform(get("/api/artists/active")).andExpect(status().isOk());
    }

    @Test
    void publicRouteIgnoresAGarbageBearerTokenInsteadOfFailing() throws Exception {
        when(eventService.findPublishedEvents()).thenReturn(List.of());

        mockMvc.perform(get("/api/events/published").header("Authorization", "Bearer basura"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownRoutesAreDeniedByDefault() throws Exception {
        mockMvc.perform(get("/api/no-existe"))
                .andExpect(status().isUnauthorized());
    }

    // -eventos: roles

    // T-SEC-002
    @Test
    void anonymousCannotCreateEvents() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content(EVENT_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.details").isEmpty());

        verifyNoInteractions(eventService);
    }

    // T-SEC-003
    @Test
    void userCannotCreateEvents() throws Exception {
        mockMvc.perform(post("/api/events").header("Authorization", andrea())
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT_BODY))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta operación"));

        verifyNoInteractions(eventService);
    }

    @Test
    void staffCannotCreateEvents() throws Exception {
        mockMvc.perform(post("/api/events").header("Authorization", laura())
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT_BODY))
                .andExpect(status().isForbidden());

        verifyNoInteractions(eventService);
    }

    // T-SEC-004
    @Test
    void adminCanCreateEvents() throws Exception {
        when(eventService.create(any())).thenReturn(eventResponse(EventStatus.DRAFT));

        mockMvc.perform(post("/api/events").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));
    }

    // T-SEC-018
    @Test
    void invalidBodyFromAuthorizedUserIsStill400() throws Exception {
        mockMvc.perform(post("/api/events").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verifyNoInteractions(eventService);
    }

    // T-SEC-014
    @Test
    void adminCanPublishButUserAndStaffCannot() throws Exception {
        when(eventService.publish("CMF-2026")).thenReturn(eventResponse(EventStatus.PUBLISHED));

        mockMvc.perform(patch("/api/events/CMF-2026/publish").header("Authorization", admin()))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/events/CMF-2026/publish").header("Authorization", andrea()))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/events/CMF-2026/publish").header("Authorization", laura()))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/events/CMF-2026/publish"))
                .andExpect(status().isUnauthorized());

        verify(eventService, Mockito.times(1)).publish("CMF-2026");
    }

    @Test
    void onlyAdminCanAssociateArtists() throws Exception {
        when(eventService.addArtist("CMF-2026", 5L)).thenReturn(eventResponse(EventStatus.PUBLISHED));

        mockMvc.perform(post("/api/events/CMF-2026/artists/5").header("Authorization", admin()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/events/CMF-2026/artists/5").header("Authorization", andrea()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/events/CMF-2026/artists/5"))
                .andExpect(status().isUnauthorized());
    }

    // - eventos: borradores

    // T-SEC-017
    @Test
    void anonymousCannotSeeADraftEventAndGetsTheSame404AsAMissingOne() throws Exception {
        when(eventService.findByCode("CMF-DRAFT")).thenReturn(eventResponse(EventStatus.DRAFT));

        mockMvc.perform(get("/api/events/CMF-DRAFT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Event not found: CMF-DRAFT"))
                .andExpect(jsonPath("$.name").doesNotExist())
                .andExpect(jsonPath("$.venueCode").doesNotExist());
    }

    @Test
    void userCannotSeeADraftEventEither() throws Exception {
        when(eventService.findByCode("CMF-DRAFT")).thenReturn(eventResponse(EventStatus.DRAFT));

        mockMvc.perform(get("/api/events/CMF-DRAFT").header("Authorization", andrea()))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffAndAdminCanSeeADraftEvent() throws Exception {
        when(eventService.findByCode("CMF-DRAFT")).thenReturn(eventResponse(EventStatus.DRAFT));

        mockMvc.perform(get("/api/events/CMF-DRAFT").header("Authorization", laura()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
        mockMvc.perform(get("/api/events/CMF-DRAFT").header("Authorization", admin()))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCanSeeAPublishedEvent() throws Exception {
        when(eventService.findByCode("CMF-2026")).thenReturn(eventResponse(EventStatus.PUBLISHED));

        mockMvc.perform(get("/api/events/CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));
    }

    @Test
    void eventsByArtistHideNonPublishedOnesFromThePublicButNotFromStaff() throws Exception {
        when(eventService.findByArtist("Solar Beat")).thenReturn(List.of(
                summary("CMF-2026", EventStatus.PUBLISHED), summary("CMF-DRAFT", EventStatus.DRAFT)));

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat")
                        .header("Authorization", laura()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // -JWT inválidos

    // T-SEC-007
    @Test
    void expiredTokenIsRejectedWith401AndErrorResponse() throws Exception {
        Clock past = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        String expired = new JwtService(jwtProperties, past).generateToken(ANDREA_ID);
        when(userDetailsService.loadByUserId(ANDREA_ID)).thenReturn(
                new SecurityPrincipal(ANDREA_ID, "andrea", "andrea@mail.com", null, true, List.of(Role.USER)));

        mockMvc.perform(get("/api/tickets/TCK-1").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verifyNoInteractions(ticketService);
    }

    // T-SEC-008
    @Test
    void tokenWithTamperedSignatureIsRejectedWithoutTouchingBusiness() throws Exception {
        String token = jwtService.generateToken(ANDREA_ID);
        String[] parts = token.split("\\.");
        char first = parts[2].charAt(0);
        String tampered = parts[0] + "." + parts[1] + "." + (first == 'A' ? 'B' : 'A') + parts[2].substring(1);

        mockMvc.perform(get("/api/tickets/TCK-1").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ticketService);
    }

    @Test
    void malformedBearerHeadersGive401NeverA500() throws Exception {
        mockMvc.perform(get("/api/tickets/TCK-1").header("Authorization", "Bearer"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tickets/TCK-1").header("Authorization", "Bearer a.b.c"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tickets/TCK-1").header("Authorization", "Token algo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInQueryStringIsNeverAccepted() throws Exception {
        String token = jwtService.generateToken(ANDREA_ID);

        mockMvc.perform(get("/api/tickets/TCK-1").param("access_token", token))
                .andExpect(status().isUnauthorized());
    }

    // T-SEC-016
    @Test
    void validTokenOfADeactivatedUserIsRejected() throws Exception {
        String inactive = bearer(ANDREA_ID, "andrea", false, Role.USER);

        mockMvc.perform(get("/api/tickets/TCK-1").header("Authorization", inactive))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ticketService);
    }

    @Test
    void roleRevokedAfterTheTokenWasIssuedIsHonouredOnTheNextRequest() throws Exception {
        // Token emitido cuando Laura era STAFF...
        String token = jwtService.generateToken(LAURA_ID);
        // ...pero la base de datos ya solo le reconoce ROLE_USER.
        when(userDetailsService.loadByUserId(LAURA_ID)).thenReturn(
                new SecurityPrincipal(LAURA_ID, "laura", "laura@mail.com", null, true, List.of(Role.USER)));

        mockMvc.perform(patch("/api/tickets/TCK-1/use").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ticketService);
    }

    // ------------------------------------------------------------ tickets: propiedad (anti-IDOR)

    @Test
    void ownerCanReadOwnTicket() throws Exception {
        ticketOwnedBy("TCK-1", ANDREA_ID, "andrea");
        when(ticketService.findByCode("TCK-1")).thenReturn(ticketResponse(TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/TCK-1").header("Authorization", andrea()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TCK-1"));
    }

    // T-SEC-009
    @Test
    void userCannotReadSomeoneElsesTicket() throws Exception {
        ticketOwnedBy("TCK-2", CARLOS_ID, "carlos");

        mockMvc.perform(get("/api/tickets/TCK-2").header("Authorization", andrea()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permisos para realizar esta operación"))
                .andExpect(jsonPath("$.ticketCode").doesNotExist());

        verifyNoInteractions(ticketService);
    }

    @Test
    void staffCanReadAnyTicket() throws Exception {
        ticketOwnedBy("TCK-2", CARLOS_ID, "carlos");
        when(ticketService.findByCode("TCK-2")).thenReturn(ticketResponse(TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/TCK-2").header("Authorization", laura()))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotReadTickets() throws Exception {
        mockMvc.perform(get("/api/tickets/TCK-1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ticketService);
    }

    @Test
    void missingTicketStillAnswers404ForAnAuthenticatedUser() throws Exception {
        when(ticketRepository.findByTicketCode("TCK-NOPE")).thenReturn(Optional.empty());
        when(ticketService.findByCode("TCK-NOPE"))
                .thenThrow(new com.pulsepass.platform.exception.ResourceNotFoundException("Ticket not found: TCK-NOPE"));

        mockMvc.perform(get("/api/tickets/TCK-NOPE").header("Authorization", andrea()))
                .andExpect(status().isNotFound());
    }

    // T-SEC-010
    @Test
    void userCannotBuyATicketInSomeoneElsesName() throws Exception {
        when(userRepository.findByEmailIgnoreCase("carlos@mail.com")).thenReturn(Optional.of(userEntity(CARLOS_ID, "carlos")));

        mockMvc.perform(post("/api/tickets").header("Authorization", andrea())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userEmail": "carlos@mail.com", "eventCode": "CMF-2026", "type": "GENERAL"}
                                """))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    void userCanBuyATicketForHimself() throws Exception {
        when(userRepository.findByEmailIgnoreCase("andrea@mail.com")).thenReturn(Optional.of(userEntity(ANDREA_ID, "andrea")));
        when(ticketService.purchase(any(PurchaseTicketRequest.class))).thenReturn(ticketResponse(TicketStatus.PAID));

        mockMvc.perform(post("/api/tickets").header("Authorization", andrea())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userEmail": "andrea@mail.com", "eventCode": "CMF-2026", "type": "GENERAL"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void anonymousCannotBuyTickets() throws Exception {
        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userEmail": "andrea@mail.com", "eventCode": "CMF-2026", "type": "GENERAL"}
                                """))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(ticketService);
    }

    // T-SEC-011
    @Test
    void ownerCanCancelOwnTicket() throws Exception {
        ticketOwnedBy("TCK-1", ANDREA_ID, "andrea");
        when(ticketService.cancel("TCK-1")).thenReturn(ticketResponse(TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/TCK-1/cancel").header("Authorization", andrea()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void userCannotCancelSomeoneElsesTicket() throws Exception {
        ticketOwnedBy("TCK-2", CARLOS_ID, "carlos");

        mockMvc.perform(patch("/api/tickets/TCK-2/cancel").header("Authorization", andrea()))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).cancel(any());
    }

    @Test
    void ticketsByUserEmailAreOnlyForTheOwnerOrAdmin() throws Exception {
        when(userRepository.findByEmailIgnoreCase("andrea@mail.com")).thenReturn(Optional.of(userEntity(ANDREA_ID, "andrea")));
        when(userRepository.findByEmailIgnoreCase("carlos@mail.com")).thenReturn(Optional.of(userEntity(CARLOS_ID, "carlos")));
        when(ticketService.findByUserEmail(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/tickets/by-user").param("email", "andrea@mail.com")
                        .header("Authorization", andrea()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tickets/by-user").param("email", "carlos@mail.com")
                        .header("Authorization", andrea()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/tickets/by-user").param("email", "carlos@mail.com")
                        .header("Authorization", admin()))
                .andExpect(status().isOk());
    }

    // - tickets: STAFF

    // T-SEC-012
    @Test
    void userCannotMarkATicketAsUsed() throws Exception {
        mockMvc.perform(patch("/api/tickets/TCK-1/use").header("Authorization", andrea()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ticketService);
    }

    // T-SEC-013
    @Test
    void staffAndAdminCanMarkATicketAsUsed() throws Exception {
        when(ticketService.markAsUsed("TCK-1")).thenReturn(ticketResponse(TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/TCK-1/use").header("Authorization", laura()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));
        mockMvc.perform(patch("/api/tickets/TCK-1/use").header("Authorization", admin()))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotMarkATicketAsUsed() throws Exception {
        mockMvc.perform(patch("/api/tickets/TCK-1/use"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void paidTicketsOfAnEventAreOnlyForStaffAndAdmin() throws Exception {
        when(ticketService.findPaidTicketsByEvent("CMF-2026")).thenReturn(List.of());

        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid").header("Authorization", laura()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid").header("Authorization", admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid").header("Authorization", andrea()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid"))
                .andExpect(status().isUnauthorized());
    }

    // - usuarios

    // T-SEC-015
    @Test
    void userCannotLookUpAnotherUser() throws Exception {
        when(userRepository.findByEmailIgnoreCase("carlos@mail.com")).thenReturn(Optional.of(userEntity(CARLOS_ID, "carlos")));
        when(userRepository.findByUsername("carlos")).thenReturn(Optional.of(userEntity(CARLOS_ID, "carlos")));

        mockMvc.perform(get("/api/users/by-email").param("email", "carlos@mail.com")
                        .header("Authorization", andrea()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users/by-username").param("username", "carlos")
                        .header("Authorization", andrea()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void userCanLookUpHimselfAndAdminCanLookUpAnyone() throws Exception {
        when(userRepository.findByEmailIgnoreCase("andrea@mail.com")).thenReturn(Optional.of(userEntity(ANDREA_ID, "andrea")));
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(userEntity(ANDREA_ID, "andrea")));
        when(userRepository.findByEmailIgnoreCase("carlos@mail.com")).thenReturn(Optional.of(userEntity(CARLOS_ID, "carlos")));
        when(userService.findByEmail("andrea@mail.com")).thenReturn(userResponse(ANDREA_ID, "andrea"));
        when(userService.findByUsername("andrea")).thenReturn(userResponse(ANDREA_ID, "andrea"));
        when(userService.findByEmail("carlos@mail.com")).thenReturn(userResponse(CARLOS_ID, "carlos"));

        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@mail.com")
                        .header("Authorization", andrea()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/by-username").param("username", "andrea")
                        .header("Authorization", andrea()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/users/by-email").param("email", "carlos@mail.com")
                        .header("Authorization", admin()))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCannotLookUpUsers() throws Exception {
        mockMvc.perform(get("/api/users/by-email").param("email", "andrea@mail.com"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------ T-SEC-019

    @Test
    void errorResponsesNeverLeakStacktracesOrSecrets() throws Exception {
        String body = mockMvc.perform(get("/api/tickets/TCK-1").header("Authorization", "Bearer a.b.c"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(body)
                .doesNotContain("Exception", "at com.", "a.b.c", "password", "hash");
    }
}
