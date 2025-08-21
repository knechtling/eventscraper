package com.hansablock.eventscraper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;

    @Autowired
    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    // Upcoming-only, sorted by date (pageable)
    public Page<Event> getUpcomingEvents(Pageable pageable) {
        return eventRepository.findAllByDateGreaterThanEqual(LocalDate.now(), pageable);
    }

    // Upcoming-only search in title or description (pageable)
    public Page<Event> searchUpcomingEventsByTitleOrDescription(String searchTerm, Pageable pageable) {
        return eventRepository.searchUpcoming(searchTerm, LocalDate.now(), pageable);
    }

    // Unique locations from upcoming events only (non-paged for filter options)
    public List<String> getUniqueLocationsFromUpcoming() {
        return eventRepository.findAllByDateGreaterThanEqualOrderByDateAsc(LocalDate.now()).stream()
                .map(Event::getLocation)
                .filter(loc -> loc != null && !loc.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
