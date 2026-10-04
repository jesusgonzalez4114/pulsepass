package com.pulsepass.platform.dto.response;

import com.pulsepass.platform.domain.EventStatus;

import java.time.LocalDateTime;

public record EventSummaryResponse(
        String eventCode,
        String name,
        LocalDateTime eventDate,
        EventStatus status,
        String venueName
) {
}
