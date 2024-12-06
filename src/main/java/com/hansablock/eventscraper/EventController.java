package com.hansablock.eventscraper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class EventController {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @GetMapping("/")
    public String showEvents(@RequestParam(value = "search", required = false) String search, Model model) {
        List<Event> events;
        if (search != null && !search.isEmpty()) {
            events = eventRepository.findByTitleContainingIgnoreCase(search);
        } else {
            events = eventService.getEvents();
        }
        model.addAttribute("events", events);
        return "welcome";
    }
}