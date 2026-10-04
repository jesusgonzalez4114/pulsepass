package com.pulsepass.platform.dto.response;

import com.pulsepass.platform.domain.EventCategory;
import com.pulsepass.platform.domain.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

public record EventResponse(
        Long id,
        String eventCode,
        String name,
        String description,
        EventCategory category,
        EventStatus status,
        LocalDateTime eventDate,
        Integer minimumAge,
        String venueCode,
        String venueName,
        List<String> artists
) {
}
