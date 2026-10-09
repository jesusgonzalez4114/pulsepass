package com.pulsepass.platform.service;

import com.pulsepass.platform.domain.*;
import com.pulsepass.platform.dto.request.PurchaseTicketRequest;
import com.pulsepass.platform.exception.BusinessRuleException;
import com.pulsepass.platform.exception.ResourceNotFoundException;
import com.pulsepass.platform.mapper.TicketMapper;
import com.pulsepass.platform.repository.EventRepository;
import com.pulsepass.platform.repository.TicketRepository;
import com.pulsepass.platform.repository.UserRepository;
import com.pulsepass.platform.service.impl.TicketServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private TicketMapper mapper;

    @InjectMocks
    private TicketServiceImpl service;

    private User activeAdultUser() {
        User user = new User("andrea", "andrea@mail.com");
        UserProfile profile = new UserProfile("Andrea", "Gomez", "300", "Santa Marta",
                LocalDate.now().minusYears(25));
        user.assignProfile(profile);
        return user;
    }

    private Event publishedEvent(int minimumAge) {
        Venue venue = new Venue("VEN-SMR-01", "Marina", "Santa Marta", "Calle 1", 3);
        Event event = new Event("CMF-2026", "Caribbean Music Fest", "desc",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusDays(30), minimumAge);
        venue.addEvent(event);
        return event;
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        PurchaseTicketRequest request = new PurchaseTicketRequest("unknown@mail.com", "CMF-2026", TicketType.GENERAL);

        when(userRepository.findByEmailIgnoreCase("unknown@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.purchase(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserInactive() {
        User inactiveUser = activeAdultUser();
        inactiveUser.setActive(false);

        PurchaseTicketRequest request = new PurchaseTicketRequest("andrea@mail.com", "CMF-2026", TicketType.GENERAL);

        when(userRepository.findByEmailIgnoreCase("andrea@mail.com")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> service.purchase(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEventNotPublished() {
        User user = activeAdultUser();
        Event draftEvent = publishedEvent(0);
        draftEvent.setStatus(EventStatus.DRAFT);

        PurchaseTicketRequest request = new PurchaseTicketRequest("andrea@mail.com", "CMF-2026", TicketType.GENERAL);

        when(userRepository.findByEmailIgnoreCase("andrea@mail.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(draftEvent));

        assertThatThrownBy(() -> service.purchase(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUserBelowMinimumAge() {
        User minor = new User("laura", "laura@mail.com");
        UserProfile profile = new UserProfile("Laura", "Diaz", "300", "Santa Marta",
                LocalDate.now().minusYears(17));
        minor.assignProfile(profile);

        Event event = publishedEvent(18);

        PurchaseTicketRequest request = new PurchaseTicketRequest("laura@mail.com", "CMF-2026", TicketType.GENERAL);

        when(userRepository.findByEmailIgnoreCase("laura@mail.com")).thenReturn(Optional.of(minor));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));


        assertThatThrownBy(() -> service.purchase(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenNoCapacityAvailable() {
        User user = activeAdultUser();
        Event event = publishedEvent(0); // capacity = 3

        PurchaseTicketRequest request = new PurchaseTicketRequest("andrea@mail.com", "CMF-2026", TicketType.GENERAL);

        when(userRepository.findByEmailIgnoreCase("andrea@mail.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID)).thenReturn(3L);

        assertThatThrownBy(() -> service.purchase(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldSetEventToSoldOutWhenLastTicketPurchased() {
        User user = activeAdultUser();
        Event event = publishedEvent(0); // capacity = 3

        Ticket savedTicket = new Ticket("TCK-X", TicketType.GENERAL, TicketStatus.PAID,
                java.math.BigDecimal.valueOf(100000), LocalDateTime.now(), user, event);

        PurchaseTicketRequest request = new PurchaseTicketRequest("andrea@mail.com", "CMF-2026", TicketType.GENERAL);

        when(userRepository.findByEmailIgnoreCase("andrea@mail.com")).thenReturn(Optional.of(user));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID)).thenReturn(2L);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(savedTicket);

        service.purchase(request);

        verify(eventRepository).save(argThat(e -> e.getStatus() == EventStatus.SOLD_OUT));
    }

    @Test
    void shouldCancelPaidTicket() {
        User user = activeAdultUser();
        Event event = publishedEvent(0);
        Ticket ticket = new Ticket("TCK-Y", TicketType.GENERAL, TicketStatus.PAID,
                java.math.BigDecimal.valueOf(100000), LocalDateTime.now(), user, event);

        when(ticketRepository.findByTicketCode("TCK-Y")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.cancel("TCK-Y");

        verify(ticketRepository).save(argThat(t -> t.getStatus() == TicketStatus.CANCELLED));
    }

    @Test
    void shouldThrowWhenCancellingUsedTicket() {
        User user = activeAdultUser();
        Event event = publishedEvent(0);
        Ticket ticket = new Ticket("TCK-Z", TicketType.GENERAL, TicketStatus.USED,
                java.math.BigDecimal.valueOf(100000), LocalDateTime.now(), user, event);

        when(ticketRepository.findByTicketCode("TCK-Z")).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.cancel("TCK-Z"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldMarkPaidTicketAsUsed() {
        User user = activeAdultUser();
        Event event = publishedEvent(0);
        Ticket ticket = new Ticket("TCK-W", TicketType.GENERAL, TicketStatus.PAID,
                java.math.BigDecimal.valueOf(100000), LocalDateTime.now(), user, event);

        when(ticketRepository.findByTicketCode("TCK-W")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        service.markAsUsed("TCK-W");

        verify(ticketRepository).save(argThat(t -> t.getStatus() == TicketStatus.USED));
    }

    @Test
    void shouldThrowWhenUsingCancelledTicket() {
        User user = activeAdultUser();
        Event event = publishedEvent(0);
        Ticket ticket = new Ticket("TCK-V", TicketType.GENERAL, TicketStatus.CANCELLED,
                java.math.BigDecimal.valueOf(100000), LocalDateTime.now(), user, event);

        when(ticketRepository.findByTicketCode("TCK-V")).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.markAsUsed("TCK-V"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
