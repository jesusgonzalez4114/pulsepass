package com.pulsepass.platform.service.impl;

import com.pulsepass.platform.domain.Artist;
import com.pulsepass.platform.domain.Event;
import com.pulsepass.platform.domain.EventStatus;
import com.pulsepass.platform.domain.Venue;
import com.pulsepass.platform.dto.request.CreateEventRequest;
import com.pulsepass.platform.dto.response.EventResponse;
import com.pulsepass.platform.dto.response.EventSummaryResponse;
import com.pulsepass.platform.exception.BusinessRuleException;
import com.pulsepass.platform.exception.DuplicateResourceException;
import com.pulsepass.platform.exception.ResourceNotFoundException;
import com.pulsepass.platform.mapper.EventMapper;
import com.pulsepass.platform.repository.ArtistRepository;
import com.pulsepass.platform.repository.EventRepository;
import com.pulsepass.platform.repository.VenueRepository;
import com.pulsepass.platform.service.EventService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper mapper;

    public EventServiceImpl(EventRepository eventRepository, VenueRepository venueRepository,
                            ArtistRepository artistRepository, EventMapper mapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {

        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException("Event code already exists: " + request.eventCode());
        }

        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException("Venue not found: " + request.venueCode()));

        if (!venue.isActive()) {
            throw new BusinessRuleException("Cannot create event on inactive venue: " + request.venueCode());
        }

        if (request.eventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Event date must be in the future");
        }

        if (request.minimumAge() != null && request.minimumAge() < 0) {
            throw new BusinessRuleException("Minimum age cannot be negative");
        }

        Event event = new Event(
                request.eventCode(), request.name(), request.description(),
                request.category(), EventStatus.DRAFT, request.eventDate(), request.minimumAge());

        venue.addEvent(event);

        Event saved = eventRepository.save(event);

        return mapper.toResponse(saved);
    }

    @Override
    public EventResponse findByCode(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));
    }

    @Override
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {

        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT events can be published: " + eventCode);
        }

        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot publish an event with a past date");
        }

        if (!event.getVenue().isActive()) {
            throw new BusinessRuleException("Cannot publish event on inactive venue");
        }

        event.setStatus(EventStatus.PUBLISHED);
        Event saved = eventRepository.save(event);

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {

        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventCode));

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + artistId));

        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException("Cannot add artists to a " + event.getStatus() + " event");
        }

        if (event.getArtists().contains(artist)) {
            throw new BusinessRuleException("Artist already associated with this event");
        }

        event.addArtist(artist);
        Event saved = eventRepository.save(event);

        return mapper.toResponse(saved);
    }

    @Override
    public List<EventSummaryResponse> findByArtist(String stageName) {
        return eventRepository.findByArtistStageName(stageName)
                .stream()
                .map(mapper::toSummary)
                .toList();
    }
}
