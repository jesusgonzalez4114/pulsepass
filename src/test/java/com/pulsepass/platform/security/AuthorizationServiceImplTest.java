package com.pulsepass.platform.security;

import com.pulsepass.platform.domain.Role;
import com.pulsepass.platform.domain.Ticket;
import com.pulsepass.platform.domain.User;
import com.pulsepass.platform.repository.TicketRepository;
import com.pulsepass.platform.repository.UserRepository;
import com.pulsepass.platform.service.impl.AuthorizationServiceImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {

    private static final long ANDREA_ID = 1L;
    private static final long CARLOS_ID = 2L;

    @Mock private UserRepository userRepository;
    @Mock private TicketRepository ticketRepository;

    @InjectMocks private AuthorizationServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(long id, String username, Role... roles) {
        SecurityPrincipal principal = new SecurityPrincipal(
                id, username, username + "@mail.com", null, true, List.of(roles));
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    private User user(long id, String username) {
        User user = new User(username, username + "@mail.com");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void ticketOwnedBy(String code, long ownerId, String ownerName) {
        Ticket ticket = mock(Ticket.class);
        when(ticket.getUser()).thenReturn(user(ownerId, ownerName));
        when(ticketRepository.findByTicketCode(code)).thenReturn(Optional.of(ticket));
    }

    // ---------- canViewTicket ----------

    // TEST-AUTHZ-001
    @Test
    void ownerCanViewOwnTicket() {
        loginAs(ANDREA_ID, "andrea", Role.USER);
        ticketOwnedBy("TCK-1", ANDREA_ID, "andrea");

        assertThat(service.canViewTicket("TCK-1")).isTrue();
    }

    // TEST-AUTHZ-002
    @Test
    void userCannotViewTicketOfAnotherUser() {
        loginAs(ANDREA_ID, "andrea", Role.USER);
        ticketOwnedBy("TCK-2", CARLOS_ID, "carlos");

        assertThat(service.canViewTicket("TCK-2")).isFalse();
    }

    @Test
    void staffAndAdminCanViewAnyTicket() {
        loginAs(10L, "laura", Role.USER, Role.STAFF);
        assertThat(service.canViewTicket("TCK-2")).isTrue();

        loginAs(11L, "admin", Role.ADMIN);
        assertThat(service.canViewTicket("TCK-2")).isTrue();
    }

    @Test
    void missingTicketIsLeftToTheServiceSoItAnswers404() {
        loginAs(ANDREA_ID, "andrea", Role.USER);
        when(ticketRepository.findByTicketCode("TCK-NOPE")).thenReturn(Optional.empty());

        assertThat(service.canViewTicket("TCK-NOPE")).isTrue();
    }

    @Test
    void anonymousCannotViewTickets() {
        assertThat(service.canViewTicket("TCK-1")).isFalse();

        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(service.canViewTicket("TCK-1")).isFalse();
    }

    // ---------- canCancelTicket ----------

    // TEST-AUTHZ-003
    @Test
    void ownerCanCancelOwnTicketButNotOthers() {
        loginAs(ANDREA_ID, "andrea", Role.USER);
        ticketOwnedBy("TCK-1", ANDREA_ID, "andrea");
        ticketOwnedBy("TCK-2", CARLOS_ID, "carlos");

        assertThat(service.canCancelTicket("TCK-1")).isTrue();
        assertThat(service.canCancelTicket("TCK-2")).isFalse();
    }

    @Test
    void staffCannotCancelTicketsOfOthersButAdminCan() {
        ticketOwnedBy("TCK-2", CARLOS_ID, "carlos");

        loginAs(10L, "laura", Role.STAFF);
        assertThat(service.canCancelTicket("TCK-2")).isFalse();

        loginAs(11L, "admin", Role.ADMIN);
        assertThat(service.canCancelTicket("TCK-2")).isTrue();
    }

    // ---------- canPurchaseFor ----------

    // TEST-AUTHZ-004
    @Test
    void canPurchaseOnlyForOwnEmailComparingPersistentId() {
        loginAs(ANDREA_ID, "andrea", Role.USER);
        when(userRepository.findByEmailIgnoreCase("ANDREA@mail.com")).thenReturn(Optional.of(user(ANDREA_ID, "andrea")));
        when(userRepository.findByEmailIgnoreCase("carlos@mail.com")).thenReturn(Optional.of(user(CARLOS_ID, "carlos")));
        when(userRepository.findByEmailIgnoreCase("nadie@mail.com")).thenReturn(Optional.empty());

        assertThat(service.canPurchaseFor("ANDREA@mail.com")).isTrue();
        assertThat(service.canPurchaseFor("carlos@mail.com")).isFalse();
        assertThat(service.canPurchaseFor("nadie@mail.com")).isFalse();
    }

    @Test
    void adminCannotPurchaseOnBehalfOfOthers() {
        loginAs(11L, "admin", Role.ADMIN);
        lenient().when(userRepository.findByEmailIgnoreCase("carlos@mail.com"))
                .thenReturn(Optional.of(user(CARLOS_ID, "carlos")));

        assertThat(service.canPurchaseFor("carlos@mail.com")).isFalse();
    }

    // ---------- usuarios ----------

    // TEST-AUTHZ-005
    @Test
    void userCanLookUpOnlyHimself() {
        loginAs(ANDREA_ID, "andrea", Role.USER);
        when(userRepository.findByEmailIgnoreCase("andrea@mail.com")).thenReturn(Optional.of(user(ANDREA_ID, "andrea")));
        when(userRepository.findByEmailIgnoreCase("carlos@mail.com")).thenReturn(Optional.of(user(CARLOS_ID, "carlos")));
        when(userRepository.findByUsername("andrea")).thenReturn(Optional.of(user(ANDREA_ID, "andrea")));
        when(userRepository.findByUsername("carlos")).thenReturn(Optional.of(user(CARLOS_ID, "carlos")));

        assertThat(service.canAccessUserByEmail("andrea@mail.com")).isTrue();
        assertThat(service.canAccessUserByEmail("carlos@mail.com")).isFalse();
        assertThat(service.canAccessUserByUsername("andrea")).isTrue();
        assertThat(service.canAccessUserByUsername("carlos")).isFalse();
    }

    @Test
    void adminCanLookUpAnyUserButStaffCannot() {
        lenient().when(userRepository.findByEmailIgnoreCase("carlos@mail.com"))
                .thenReturn(Optional.of(user(CARLOS_ID, "carlos")));
        lenient().when(userRepository.findByUsername("carlos"))
                .thenReturn(Optional.of(user(CARLOS_ID, "carlos")));

        loginAs(11L, "admin", Role.ADMIN);
        assertThat(service.canAccessUserByEmail("carlos@mail.com")).isTrue();
        assertThat(service.canAccessUserByUsername("carlos")).isTrue();

        loginAs(10L, "laura", Role.STAFF);
        assertThat(service.canAccessUserByEmail("carlos@mail.com")).isFalse();
        assertThat(service.canAccessUserByUsername("carlos")).isFalse();
    }

    // ---------- STAFF ----------

    @Test
    void hasStaffAccessIsTrueOnlyForStaffAndAdmin() {
        assertThat(service.hasStaffAccess()).isFalse();

        loginAs(ANDREA_ID, "andrea", Role.USER);
        assertThat(service.hasStaffAccess()).isFalse();

        loginAs(10L, "laura", Role.USER, Role.STAFF);
        assertThat(service.hasStaffAccess()).isTrue();

        loginAs(11L, "admin", Role.ADMIN);
        assertThat(service.hasStaffAccess()).isTrue();
    }

    @Test
    void aPrincipalThatIsNotOurSecurityPrincipalIsNeverTrusted() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        "admin", null, AuthorityUtils.createAuthorityList("ROLE_ADMIN")));

        assertThat(service.hasStaffAccess()).isFalse();
        assertThat(service.canViewTicket("TCK-1")).isFalse();
    }
}
