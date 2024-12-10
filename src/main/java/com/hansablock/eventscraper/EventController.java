package com.hansablock.eventscraper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/")
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;

    @Autowired
    public EventController(EventService eventService, EventRepository eventRepository) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
    }

    @GetMapping
    public String getEvents(@RequestParam(value = "search", required = false) String search, Model model) {
        List<Event> events;
        if (search != null && !search.isEmpty()) {
            events = eventService.searchEventsByTitleOrDescription(search);
        } else {
            events = eventService.getSortedEvents();
        }
        model.addAttribute("events", events);
        model.addAttribute("search", search);
        return "welcome";
    }
@GetMapping("/event/details/{id}")
public String showEventDetails(@PathVariable Long id, Model model) {
    Event event = eventRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid event Id:" + id));
    model.addAttribute("event", event);
    return "eventDetails";
}}
