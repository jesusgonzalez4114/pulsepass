package com.pulsepass.platform.mapper;

import com.pulsepass.platform.domain.Venue;
import com.pulsepass.platform.dto.response.VenueResponse;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VenueMapper {
    VenueResponse toResponse(Venue venue);
}
