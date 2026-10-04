package com.pulsepass.platform.service;

import com.pulsepass.platform.domain.Venue;
import com.pulsepass.platform.dto.response.VenueResponse;
import com.pulsepass.platform.exception.ResourceNotFoundException;
import com.pulsepass.platform.mapper.VenueMapper;
import com.pulsepass.platform.repository.VenueRepository;
import com.pulsepass.platform.service.impl.VenueServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceImplTest {

    @Mock
    private VenueRepository repository;

    @Mock
    private VenueMapper mapper;

    @InjectMocks
    private VenueServiceImpl service;

    @Test
    void shouldFindVenueByCode() {
        Venue venue = new Venue("VEN-001", "Test Venue", "Bogota", "Calle 1", 1000);
        VenueResponse response = new VenueResponse(1L, "VEN-001", "Test Venue", "Bogota", "Calle 1", 1000, true);

        when(repository.findByCode("VEN-001")).thenReturn(Optional.of(venue));
        when(mapper.toResponse(venue)).thenReturn(response);

        VenueResponse result = service.findByCode("VEN-001");

        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldThrowWhenVenueNotFound() {
        when(repository.findByCode("VEN-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("VEN-999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldReturnOnlyActiveVenues() {
        Venue active = new Venue("VEN-A", "Active", "Cali", "Calle 2", 500);

        when(repository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(active));
        when(mapper.toResponse(active)).thenReturn(
                new VenueResponse(1L, "VEN-A", "Active", "Cali", "Calle 2", 500, true));

        List<VenueResponse> result = service.findActiveVenues();

        assertThat(result).hasSize(1);
    }
}
