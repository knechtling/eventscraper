package com.hansablock.eventscraper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class EventService {

  private final EventRepository eventRepository;

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  @Autowired
  public EventService(EventRepository eventRepository) {
    this.eventRepository = eventRepository;
  }

  // Upcoming-only, sorted by date (pageable)
  public Page<Event> getUpcomingEvents(Pageable pageable) {
    return eventRepository.findAllByDateGreaterThanEqual(LocalDate.now(), pageable);
  }

  // Upcoming-only search in title or description (pageable)
  public Page<Event> searchUpcomingEventsByTitleOrDescription(
      String searchTerm, Pageable pageable) {
    return eventRepository.searchUpcoming(searchTerm, LocalDate.now(), pageable);
  }

  // New: filter by optional q, start, end, location
  public Page<Event> searchWithFilters(
      String q, String start, String end, String loc, Pageable pageable) {
    LocalDate from = parseDateOrDefault(start, LocalDate.now());
    LocalDate to = parseDateOrNull(end);
    String location = (loc == null || loc.isBlank()) ? null : loc.trim();
    String query = (q == null || q.isBlank()) ? null : q.trim();
    return eventRepository.searchUpcomingWithFilters(query, from, to, location, pageable);
  }

  private LocalDate parseDateOrDefault(String s, LocalDate def) {
    LocalDate d = parseDateOrNull(s);
    return d == null ? def : d;
  }

  private LocalDate parseDateOrNull(String s) {
    if (s == null || s.isBlank()) return null;
    try {
      return LocalDate.parse(s.trim(), DATE_FMT);
    } catch (DateTimeParseException ex) {
      return null;
    }
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
