package com.pulsepass.platform.service;

import com.pulsepass.platform.domain.*;
import com.pulsepass.platform.dto.request.CreateEventRequest;
import com.pulsepass.platform.dto.response.EventResponse;
import com.pulsepass.platform.exception.BusinessRuleException;
import com.pulsepass.platform.exception.DuplicateResourceException;
import com.pulsepass.platform.exception.ResourceNotFoundException;
import com.pulsepass.platform.mapper.EventMapper;
import com.pulsepass.platform.repository.ArtistRepository;
import com.pulsepass.platform.repository.EventRepository;
import com.pulsepass.platform.repository.VenueRepository;
import com.pulsepass.platform.service.impl.EventServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock private EventRepository eventRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private ArtistRepository artistRepository;
    @Mock private EventMapper mapper;

    @InjectMocks
    private EventServiceImpl service;

    @Test
    void shouldFindEventByCode() {
        Event event = new Event("EVT-001", "Test", "desc", EventCategory.MUSIC,
                EventStatus.DRAFT, LocalDateTime.now().plusDays(10), 0);

        when(eventRepository.findByEventCode("EVT-001")).thenReturn(Optional.of(event));
        when(mapper.toResponse(event)).thenReturn(
                new EventResponse(1L, "EVT-001", "Test", "desc", EventCategory.MUSIC,
                        EventStatus.DRAFT, event.getEventDate(), 0, "VEN-1", "Venue", List.of()));

        EventResponse result = service.findByCode("EVT-001");

        assertThat(result.eventCode()).isEqualTo("EVT-001");
    }

    @Test
    void shouldThrowWhenEventNotFound() {
        when(eventRepository.findByEventCode("EVT-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("EVT-999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldCreateValidEvent() {
        Venue venue = new Venue("VEN-001", "Test Venue", "Bogota", "Calle 1", 1000);

        CreateEventRequest request = new CreateEventRequest(
                "EVT-NEW", "New Event", "desc", EventCategory.MUSIC,
                LocalDateTime.now().plusDays(30), 0, "VEN-001");

        when(eventRepository.existsByEventCode("EVT-NEW")).thenReturn(false);
        when(venueRepository.findByCode("VEN-001")).thenReturn(Optional.of(venue));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Event.class))).thenReturn(
                new EventResponse(1L, "EVT-NEW", "New Event", "desc", EventCategory.MUSIC,
                        EventStatus.DRAFT, request.eventDate(), 0, "VEN-001", "Test Venue", List.of()));

        EventResponse result = service.create(request);

        assertThat(result.status()).isEqualTo(EventStatus.DRAFT);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void shouldThrowWhenVenueNotFoundOnCreate() {
        CreateEventRequest request = new CreateEventRequest(
                "EVT-X", "X", "desc", EventCategory.MUSIC,
                LocalDateTime.now().plusDays(10), 0, "VEN-999");

        when(eventRepository.existsByEventCode("EVT-X")).thenReturn(false);
        when(venueRepository.findByCode("VEN-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenVenueInactiveOnCreate() {
        Venue inactiveVenue = new Venue("VEN-IN", "Inactive", "Bogota", "Calle 1", 500);
        inactiveVenue.setActive(false);

        CreateEventRequest request = new CreateEventRequest(
                "EVT-Y", "Y", "desc", EventCategory.MUSIC,
                LocalDateTime.now().plusDays(10), 0, "VEN-IN");

        when(eventRepository.existsByEventCode("EVT-Y")).thenReturn(false);
        when(venueRepository.findByCode("VEN-IN")).thenReturn(Optional.of(inactiveVenue));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEventDateInPast() {
        Venue venue = new Venue("VEN-001", "Test Venue", "Bogota", "Calle 1", 1000);

        CreateEventRequest request = new CreateEventRequest(
                "EVT-Z", "Z", "desc", EventCategory.MUSIC,
                LocalDateTime.now().minusDays(1), 0, "VEN-001");

        when(eventRepository.existsByEventCode("EVT-Z")).thenReturn(false);
        when(venueRepository.findByCode("VEN-001")).thenReturn(Optional.of(venue));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldPublishValidDraftEvent() {
        Venue venue = new Venue("VEN-001", "Test Venue", "Bogota", "Calle 1", 1000);
        Event event = new Event("EVT-PUB", "Pub", "desc", EventCategory.MUSIC,
                EventStatus.DRAFT, LocalDateTime.now().plusDays(10), 0);
        venue.addEvent(event);

        when(eventRepository.findByEventCode("EVT-PUB")).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(mapper.toResponse(event)).thenReturn(
                new EventResponse(1L, "EVT-PUB", "Pub", "desc", EventCategory.MUSIC,
                        EventStatus.PUBLISHED, event.getEventDate(), 0, "VEN-001", "Test Venue", List.of()));

        EventResponse result = service.publish("EVT-PUB");

        assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test
    void shouldThrowWhenPublishingNonDraftEvent() {
        Venue venue = new Venue("VEN-001", "Test Venue", "Bogota", "Calle 1", 1000);
        Event event = new Event("EVT-CAN", "Can", "desc", EventCategory.MUSIC,
                EventStatus.CANCELLED, LocalDateTime.now().plusDays(10), 0);
        venue.addEvent(event);

        when(eventRepository.findByEventCode("EVT-CAN")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> service.publish("EVT-CAN"))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any());
    }
}
