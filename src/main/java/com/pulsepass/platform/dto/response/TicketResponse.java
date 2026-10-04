package com.pulsepass.platform.dto.response;

import com.pulsepass.platform.domain.TicketStatus;
import com.pulsepass.platform.domain.TicketType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String ticketCode,
        TicketType type,
        BigDecimal price,
        TicketStatus status,
        LocalDateTime purchaseDate,
        String userEmail,
        String eventCode,
        String eventName
) {
}
