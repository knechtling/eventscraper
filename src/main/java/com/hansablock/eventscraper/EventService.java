package com.hansablock.eventscraper;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventService {

    public List<Event> getEvents() {
        List<Event> events = new ArrayList<>();
        events.add(new Event(1, "Event 1", "Location 1", LocalDate.of(2024, 1, 1), "Heavy Metal", LocalTime.of(19, 0), LocalTime.of(20, 0), 10.0f));
        events.add(new Event(1, "Event 2", "Location 2", LocalDate.of(2025, 1, 2), "Heavy Blouse", LocalTime.of(19, 0), LocalTime.of(20, 0), 10.0f));
        events.add(new Event(1, "Event 3", "Location 3", LocalDate.of(2026, 1, 3), "Rock", LocalTime.of(19, 0), LocalTime.of(20, 0), 10.0f));
        events.add(new Event(1, "Event 4", "Location 4", LocalDate.of(2027, 1, 4), "Iii Pop", LocalTime.of(19, 0), LocalTime.of(20, 0), 10.0f));
        return events;
    }
}
