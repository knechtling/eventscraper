package com.hansablock.eventscraper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEventHash(String eventHash);

    boolean existsByEventHash(String eventHash);

    List<Event> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);
    List<Event> findAllByOrderByDateAsc();

    // Upcoming-only queries
    List<Event> findAllByDateGreaterThanEqualOrderByDateAsc(LocalDate date);

    @Query("SELECT e FROM Event e WHERE e.date >= :date AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY e.date ASC")
    List<Event> searchUpcoming(@Param("q") String q, @Param("date") LocalDate date);

    boolean existsByMisc(String misc);
    boolean existsByDescription(String description);
}
