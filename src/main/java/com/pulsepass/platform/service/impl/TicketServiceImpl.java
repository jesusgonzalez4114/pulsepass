package com.pulsepass.platform.service.impl;

import com.pulsepass.platform.domain.*;
import com.pulsepass.platform.dto.request.PurchaseTicketRequest;
import com.pulsepass.platform.dto.response.TicketResponse;
import com.pulsepass.platform.exception.BusinessRuleException;
import com.pulsepass.platform.exception.ResourceNotFoundException;
import com.pulsepass.platform.mapper.TicketMapper;
import com.pulsepass.platform.repository.EventRepository;
import com.pulsepass.platform.repository.TicketRepository;
import com.pulsepass.platform.repository.UserRepository;
import com.pulsepass.platform.service.TicketService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    private static final BigDecimal BASE_PRICE = new BigDecimal("100000");

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final TicketMapper mapper;

    public TicketServiceImpl(UserRepository userRepository, EventRepository eventRepository,
                             TicketRepository ticketRepository, TicketMapper mapper) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketRepository = ticketRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {

        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userEmail()));

        if (!user.isActive()) {
            throw new BusinessRuleException("User is not active: " + request.userEmail());
        }

        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.eventCode()));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Event is not PUBLISHED: " + request.eventCode());
        }

        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot purchase a ticket for a past event");
        }

        if (event.getMinimumAge() != null && event.getMinimumAge() > 0) {
            LocalDate birthDate = user.getProfile().getBirthDate();
            int age = Period.between(birthDate, event.getEventDate().toLocalDate()).getYears();
            if (age < event.getMinimumAge()) {
                throw new BusinessRuleException("User does not meet minimum age for this event");
            }
        }

        long paidTickets = ticketRepository.countByEventEventCodeAndStatus(
                request.eventCode(), TicketStatus.PAID);
        int capacity = event.getVenue().getCapacity();

        if (paidTickets >= capacity) {
            throw new BusinessRuleException("Event has no available capacity: " + request.eventCode());
        }

        BigDecimal price = calculatePrice(request.type());

        Ticket ticket = new Ticket(
                generateTicketCode(), request.type(), TicketStatus.PAID,
                price, LocalDateTime.now(), user, event);

        Ticket saved = ticketRepository.save(ticket);

        if (paidTickets + 1 == capacity) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return mapper.toResponse(saved);
    }

    private BigDecimal calculatePrice(TicketType type) {
        return switch (type) {
            case GENERAL -> BASE_PRICE;
            case STUDENT -> BASE_PRICE.multiply(new BigDecimal("0.5"));
            case VIP -> BASE_PRICE.multiply(new BigDecimal("2"));
            case BACKSTAGE -> BASE_PRICE.multiply(new BigDecimal("3"));
        };
    }

    private String generateTicketCode() {
        return "TCK-" + System.currentTimeMillis();
    }

    @Override
    public TicketResponse findByCode(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));
    }

    @Override
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository.findByEventEventCodeAndStatus(eventCode, TicketStatus.PAID)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {

        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));

        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException("Only PAID tickets can be cancelled: " + ticketCode);
        }

        if (ticket.getEvent().getEventDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot cancel a ticket after the event date");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        Ticket saved = ticketRepository.save(ticket);

        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {

        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketCode));

        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException("Only PAID tickets can be marked as used: " + ticketCode);
        }

        ticket.setStatus(TicketStatus.USED);
        Ticket saved = ticketRepository.save(ticket);

        return mapper.toResponse(saved);
    }
}
