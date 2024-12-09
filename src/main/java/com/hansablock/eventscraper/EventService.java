package com.hansablock.eventscraper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<Event> searchEventsByTitle(String title) {
        return eventRepository.findByTitleContainingIgnoreCase(title);
    }
}
