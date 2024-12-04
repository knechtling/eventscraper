package com.hansablock.eventscraper;

import java.util.List;

public class EventService {
    public String viewEvent() {
        return "Event";
    }

    public List<Event> searchEvents(String title) {
        return EventRepository.findByTitle(title);
    }
}
