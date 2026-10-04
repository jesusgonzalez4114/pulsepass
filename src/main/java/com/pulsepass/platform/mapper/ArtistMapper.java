package com.pulsepass.platform.mapper;

import com.pulsepass.platform.domain.Artist;
import com.pulsepass.platform.dto.response.ArtistResponse;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArtistMapper {
    ArtistResponse toResponse(Artist artist);
}