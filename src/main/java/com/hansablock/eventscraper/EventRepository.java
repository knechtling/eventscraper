package com.hansablock.eventscraper;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEventHash(String eventHash);

    boolean existsByEventHash(String eventHash);

    List<Event> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);
    List<Event> findAllByOrderByDateAsc();
    boolean existsByMisc(String misc);
    boolean existsByDescription(String description);
}