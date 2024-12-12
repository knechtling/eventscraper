package com.hansablock.eventscraper;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);
    List<Event> findAllByOrderByDateAsc();
    boolean existsByMisc(String misc);
    boolean existsByDescription(String description);
}