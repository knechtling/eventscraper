package com.hansablock.eventscraper;

import org.springframework.beans.factory.annotation.Autowired;
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

    // Upcoming-only, sorted by date
    public List<Event> getUpcomingEventsSorted() {
        return eventRepository.findAllByDateGreaterThanEqualOrderByDateAsc(LocalDate.now());
    }

    // Upcoming-only search in title or description
    public List<Event> searchUpcomingEventsByTitleOrDescription(String searchTerm) {
        return eventRepository.searchUpcoming(searchTerm, LocalDate.now());
    }

    // Unique locations from upcoming events only
    public List<String> getUniqueLocationsFromUpcoming() {
        return eventRepository.findAllByDateGreaterThanEqualOrderByDateAsc(LocalDate.now()).stream()
                .map(Event::getLocation)
                .filter(loc -> loc != null && !loc.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
