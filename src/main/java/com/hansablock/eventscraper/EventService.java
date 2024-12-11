package com.hansablock.eventscraper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;

    @Autowired
    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Event> getSortedEvents() {
        return eventRepository.findAllByOrderByDateAsc();
    }

    public List<Event> searchEventsByTitleOrDescription(String searchTerm) {
        return eventRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(searchTerm, searchTerm);
    }

    public List<String> getUniqueLocations() {
        return eventRepository.findAll().stream() // Get all events
                .map(Event::getLocation) // Get the location of each event
                .distinct() // Get only unique locations
                .collect(Collectors.toList()); // Collect the unique locations to a list
    }
}
