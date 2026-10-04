package com.pulsepass.platform.service.impl;

import com.pulsepass.platform.dto.response.ArtistResponse;
import com.pulsepass.platform.exception.ResourceNotFoundException;
import com.pulsepass.platform.mapper.ArtistMapper;
import com.pulsepass.platform.repository.ArtistRepository;
import com.pulsepass.platform.service.ArtistService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ArtistServiceImpl implements ArtistService {

    private final ArtistRepository repository;
    private final ArtistMapper mapper;

    public ArtistServiceImpl(ArtistRepository repository, ArtistMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ArtistResponse findById(Long id) {
        return repository.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + id));
    }

    @Override
    public ArtistResponse findByStageName(String stageName) {
        return repository.findByStageNameIgnoreCase(stageName)
                .map(mapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found: " + stageName));
    }

    @Override
    public List<ArtistResponse> findActiveArtists() {
        return repository.findByActiveTrueOrderByStageNameAsc()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }
}
