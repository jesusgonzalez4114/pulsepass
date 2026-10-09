package com.pulsepass.platform.repository;

import com.pulsepass.platform.domain.Ticket;
import com.pulsepass.platform.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByUserEmailIgnoreCase(String email);

    List<Ticket> findByUserEmailIgnoreCaseAndStatus(String email, TicketStatus status);

    List<Ticket> findByEventEventCodeAndStatus(String eventCode, TicketStatus status);


    @Query("""
        select count(t)
        from Ticket t
        where t.event.eventCode = :eventCode
        and t.status = com.pulsepass.platform.domain.TicketStatus.PAID
        """)
    long countPaidTicketsByEventCode(@Param("eventCode") String eventCode);

    Optional<Ticket> findByTicketCode(String ticketCode);
    List<Ticket> findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(String email);
    long countByEventEventCodeAndStatus(String eventCode, TicketStatus status);
}