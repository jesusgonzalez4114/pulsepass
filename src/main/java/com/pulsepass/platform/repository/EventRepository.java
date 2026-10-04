package com.pulsepass.platform.repository;

import com.pulsepass.platform.domain.Event;
import com.pulsepass.platform.domain.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEventCode(String eventCode);

    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    List<Event> findByVenueCode(String venueCode);

    boolean existsByEventCode(String eventCode);

    @Query("""
        select distinct e
        from Event e
        join e.artists a
        where lower(a.stageName) = lower(:stageName)
        order by e.eventDate
        """)
    List<Event> findByArtistStageName(@Param("stageName") String stageName);

    @Query("""
        select distinct e
        from Event e
        join e.venue v
        join e.artists a
        where lower(v.city) = lower(:city)
        and lower(a.stageName) = lower(:stageName)
        order by e.eventDate
        """)
    List<Event> findByVenueCityAndArtistStageName(
            @Param("city") String city,
            @Param("stageName") String stageName);

    @Query("""
        select distinct e
        from Event e
        join e.venue v
        join e.artists a
        where e.status = com.pulsepass.platform.domain.EventStatus.PUBLISHED
        and e.eventDate > :afterDate
        and lower(v.city) = lower(:city)
        and lower(a.stageName) like lower(concat('%', :artistText, '%'))
        order by e.eventDate
        """)
    List<Event> findRecommendedEvents(
            @Param("afterDate") LocalDateTime afterDate,
            @Param("city") String city,
            @Param("artistText") String artistText);
}