package com.pulsepass.platform.service;

import com.pulsepass.platform.dto.response.VenueResponse;

import java.util.List;

public interface VenueService {
    VenueResponse findByCode(String code);
    List<VenueResponse> findActiveVenues();
}
