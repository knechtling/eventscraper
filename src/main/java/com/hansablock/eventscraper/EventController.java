package com.hansablock.eventscraper;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/")
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    @Autowired
    public EventController(EventService eventService, EventRepository eventRepository) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
    }

@GetMapping
    public String getEvents(@RequestParam(value = "search", required = false) String search,
                            @RequestParam(value = "start", required = false) String start,
                            @RequestParam(value = "end", required = false) String end,
                            @RequestParam(value = "loc", required = false) String loc,
                            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
                            @RequestParam(value = "size", required = false, defaultValue = "24") int size,
                            Model model) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("date").ascending());
        Page<Event> eventsPage = eventService.searchWithFilters(search, start, end, loc, pageable);
        model.addAttribute("eventsPage", eventsPage);
        model.addAttribute("events", eventsPage.getContent());
        model.addAttribute("search", search);
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        model.addAttribute("loc", loc);
        model.addAttribute("size", size);
        model.addAttribute("locations", eventService.getUniqueLocationsFromUpcoming());
        return "welcome";
    }

    @GetMapping("/event/details/{id}")
    public String showEventDetails(@PathVariable Long id, Model model) {
        Event event = eventRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid event Id:" + id));
        model.addAttribute("event", event);
        return "eventDetails";
    }

    @GetMapping(value = "/event/details/{id}.ics", produces = "text/calendar")
    public ResponseEntity<byte[]> exportIcs(@PathVariable Long id) {
        Event e = eventRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid event Id:" + id));
        String ics = buildIcs(e);
        byte[] body = ics.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "calendar", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=event-" + id + ".ics");
        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }

    private String buildIcs(Event e) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//eventscraper//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");
        sb.append("BEGIN:VEVENT\r\n");
        String uid = e.getId() + "@eventscraper";
        sb.append("UID:").append(icsEscape(uid)).append("\r\n");
        String dtstamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(ZoneId.of("UTC"))
                .format(Instant.now());
        sb.append("DTSTAMP:").append(dtstamp).append("\r\n");

        LocalDate date = e.getDate();
        LocalTime startTime = e.getBeginn();
        if (date != null && startTime != null) {
            LocalDateTime ldt = LocalDateTime.of(date, startTime);
            String dtstart = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss").format(ldt);
            sb.append("DTSTART;TZID=").append(ZONE.getId()).append(":").append(dtstart).append("\r\n");
        } else if (date != null) {
            String dtstart = DateTimeFormatter.ofPattern("yyyyMMdd").format(date);
            sb.append("DTSTART;VALUE=DATE:").append(dtstart).append("\r\n");
        }
        // Optional DTEND if we have a start time: assume 2h duration
        if (date != null && startTime != null) {
            LocalDateTime end = LocalDateTime.of(date, startTime).plusHours(2);
            String dtend = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss").format(end);
            sb.append("DTEND;TZID=").append(ZONE.getId()).append(":").append(dtend).append("\r\n");
        }

        String summary = e.getTitle() != null ? e.getTitle() : "Event";
        sb.append("SUMMARY:").append(icsEscape(summary)).append("\r\n");
        if (e.getLocation() != null) {
            sb.append("LOCATION:").append(icsEscape(e.getLocation())).append("\r\n");
        }
        String descHtml = e.getDescription();
        String descText = descHtml != null ? Jsoup.parse(descHtml).text() : null;
        if (descText == null || descText.isBlank()) {
            descText = e.getMisc();
        }
        if (descText != null && !descText.isBlank()) {
            sb.append("DESCRIPTION:").append(icsEscape(descText)).append("\r\n");
        }
        if (e.getSourceUrl() != null && !e.getSourceUrl().isBlank()) {
            sb.append("URL:").append(icsEscape(e.getSourceUrl())).append("\r\n");
        }
        sb.append("END:VEVENT\r\n");
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private static String icsEscape(String s) {
        String out = s.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
        // Simple line folding at 75 octets: not strictly necessary for most clients, skip for brevity
        return out;
    }
}
