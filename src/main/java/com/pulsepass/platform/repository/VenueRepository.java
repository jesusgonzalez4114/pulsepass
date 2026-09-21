package com.pulsepass.platform.repository;

import com.pulsepass.platform.domain.Venue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    Optional<Venue> findByCode(String code);
}