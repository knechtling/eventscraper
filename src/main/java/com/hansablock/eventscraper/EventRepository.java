package com.hansablock.eventscraper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Pageable upcoming list
    Page<Event> findAllByDateGreaterThanEqual(LocalDate date, Pageable pageable);

    @Query(value = "SELECT e FROM Event e WHERE e.date >= :date AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY e.date ASC",
           countQuery = "SELECT COUNT(e) FROM Event e WHERE e.date >= :date AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :q, '%')))" )
    Page<Event> searchUpcoming(@Param("q") String q, @Param("date") LocalDate date, Pageable pageable);

    @Query(value = "SELECT e FROM Event e " +
            "WHERE e.date >= :from " +
            "AND (:to IS NULL OR e.date <= :to) " +
            "AND (:loc IS NULL OR LOWER(e.location) = LOWER(:loc)) " +
            "AND ((:q IS NULL OR :q = '') OR LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :q, '%'))) " +
            "ORDER BY e.date ASC",
           countQuery = "SELECT COUNT(e) FROM Event e " +
            "WHERE e.date >= :from " +
            "AND (:to IS NULL OR e.date <= :to) " +
            "AND (:loc IS NULL OR LOWER(e.location) = LOWER(:loc)) " +
            "AND ((:q IS NULL OR :q = '') OR LOWER(e.title) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :q, '%')))"
    )
    Page<Event> searchUpcomingWithFilters(@Param("q") String q,
                                          @Param("from") LocalDate from,
                                          @Param("to") LocalDate to,
                                          @Param("loc") String loc,
                                          Pageable pageable);

    boolean existsByMisc(String misc);
    boolean existsByDescription(String description);
}
