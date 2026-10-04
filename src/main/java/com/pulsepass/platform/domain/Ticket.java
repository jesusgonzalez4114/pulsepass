package com.pulsepass.platform.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 30)
    private String ticketCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TicketStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "purchase_date")
    private LocalDateTime purchaseDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    protected Ticket() {
    }

    public Ticket(String ticketCode, TicketType type, TicketStatus status,
                  BigDecimal price, LocalDateTime purchaseDate, User user, Event event) {
        this.ticketCode = ticketCode;
        this.type = type;
        this.status = status;
        this.price = price;
        this.purchaseDate = purchaseDate;
        this.user = user;
        this.event = event;
    }

    public Long getId() { return id; }
    public String getTicketCode() { return ticketCode; }
    public TicketType getType() { return type; }
    public TicketStatus getStatus() { return status; }
    public BigDecimal getPrice() { return price; }
    public LocalDateTime getPurchaseDate() { return purchaseDate; }
    public User getUser() { return user; }
    public Event getEvent() { return event; }

    public void setStatus(TicketStatus status) { this.status = status; }


}