package com.pulsepass.platform.dto.request;

import com.pulsepass.platform.domain.TicketType;

public record PurchaseTicketRequest(
        String userEmail,
        String eventCode,
        TicketType type
) {
}
