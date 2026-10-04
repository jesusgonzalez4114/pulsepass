package com.pulsepass.platform.service;

import com.pulsepass.platform.dto.response.ArtistResponse;

import java.util.List;

public interface ArtistService {
    ArtistResponse findById(Long id);
    ArtistResponse findByStageName(String stageName);
    List<ArtistResponse> findActiveArtists();
}