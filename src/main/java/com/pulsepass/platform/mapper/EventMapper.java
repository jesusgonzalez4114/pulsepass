package com.pulsepass.platform.mapper;

import com.pulsepass.platform.domain.Artist;
import com.pulsepass.platform.domain.Event;
import com.pulsepass.platform.dto.response.EventResponse;
import com.pulsepass.platform.dto.response.EventSummaryResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "venueCode", source = "venue.code")
    @Mapping(target = "venueName", source = "venue.name")
    @Mapping(target = "artists", expression = "java(mapArtistNames(event))")
    EventResponse toResponse(Event event);

    @Mapping(target = "venueName", source = "venue.name")
    EventSummaryResponse toSummary(Event event);

    default java.util.List<String> mapArtistNames(Event event) {
        return event.getArtists().stream()
                .map(Artist::getStageName)
                .toList();
    }
}
