package com.pulsepass.platform.service;

import com.pulsepass.platform.domain.Artist;
import com.pulsepass.platform.dto.response.ArtistResponse;
import com.pulsepass.platform.exception.ResourceNotFoundException;
import com.pulsepass.platform.mapper.ArtistMapper;
import com.pulsepass.platform.repository.ArtistRepository;
import com.pulsepass.platform.service.impl.ArtistServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistServiceImplTest {

    @Mock
    private ArtistRepository repository;

    @Mock
    private ArtistMapper mapper;

    @InjectMocks
    private ArtistServiceImpl service;

    @Test
    void shouldFindArtistByStageName() {
        Artist artist = new Artist("Solar Beat", "Colombia", "Electronic");
        ArtistResponse response = new ArtistResponse(1L, "Solar Beat", "Colombia", "Electronic", true);

        when(repository.findByStageNameIgnoreCase("Solar Beat")).thenReturn(Optional.of(artist));
        when(mapper.toResponse(artist)).thenReturn(response);

        ArtistResponse result = service.findByStageName("Solar Beat");

        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldThrowWhenArtistNotFound() {
        when(repository.findByStageNameIgnoreCase("Unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByStageName("Unknown"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
