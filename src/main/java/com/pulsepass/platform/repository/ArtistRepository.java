package com.pulsepass.platform.repository;

import com.pulsepass.platform.domain.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArtistRepository extends JpaRepository<Artist, Long> {

    Optional<Artist> findByStageNameIgnoreCase(String stageName);
}