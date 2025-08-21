package com.hansablock.eventscraper.scraper;

import com.hansablock.eventscraper.Event;
import com.hansablock.eventscraper.EventHasher;
import com.hansablock.eventscraper.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class ScraperService {

    private final EventRepository eventRepository;
    private final List<Scraper> scrapers;

    @Autowired
    public ScraperService(EventRepository eventRepository, List<Scraper> scrapers) {
        this.eventRepository = eventRepository;
        this.scrapers = scrapers;
    }

    @Scheduled(fixedRate = 3600000)
    public void scrapeAndSaveEvents() {
        for (Scraper scraper : scrapers) {
            List<Event> events = scraper.scrapeEvents().stream()
                    // discard events without date or in the past
                    .filter(e -> e.getDate() != null && !e.getDate().isBefore(LocalDate.now()))
                    // basic normalization
                    .peek(this::normalize)
                    .peek(event -> event.setEventHash(EventHasher.generateHash(event)))
                    .toList();

            saveEventsSmart(events);
            System.out.println("Scraped " + events.size() + " events from " + scraper.getClass().getSimpleName());
        }
    }

    private void normalize(Event e) {
        if (e.getTitle() != null) e.setTitle(e.getTitle().trim());
        if (e.getLocation() != null) e.setLocation(e.getLocation().trim());
        if (e.getPrice() != null) e.setPrice(e.getPrice().trim());
        if (e.getDescription() != null) e.setDescription(e.getDescription().trim());
        if (e.getMisc() != null) e.setMisc(e.getMisc().trim());
    }

    private void saveEventsSmart(List<Event> newEvents) {
        newEvents.forEach(event -> {
            eventRepository.findByEventHash(event.getEventHash())
                    .ifPresentOrElse(
                            existing -> updateExisting(existing, event),
                            () -> eventRepository.save(event)
                    );
        });
    }

    private void updateExisting(Event existing, Event newVersion) {
        // Price: update if new has digits and (old empty or old lacks digits)
        if (StringUtils.hasText(newVersion.getPrice())) {
            boolean newHasDigits = newVersion.getPrice().matches(".*\\d.*");
            boolean oldHasDigits = StringUtils.hasText(existing.getPrice()) && existing.getPrice().matches(".*\\d.*");
            if (newHasDigits && (!oldHasDigits || !StringUtils.hasText(existing.getPrice()))) {
                existing.setPrice(newVersion.getPrice());
            }
        }
        // Description: update only if clearly better (longer) or old is blank
        if (StringUtils.hasText(newVersion.getDescription())) {
            int newLen = newVersion.getDescription().length();
            int oldLen = existing.getDescription() == null ? 0 : existing.getDescription().length();
            if (oldLen == 0 || newLen > oldLen + 20) { // 20-char threshold to avoid oscillation
                existing.setDescription(newVersion.getDescription());
            }
        }
        // Thumbnail: fill if missing
        if (StringUtils.hasText(newVersion.getThumbnail()) && !StringUtils.hasText(existing.getThumbnail())) {
            existing.setThumbnail(newVersion.getThumbnail());
        }
        // Times: fill if missing
        if (existing.getEinlass() == null && newVersion.getEinlass() != null) {
            existing.setEinlass(newVersion.getEinlass());
        }
        if (existing.getBeginn() == null && newVersion.getBeginn() != null) {
            existing.setBeginn(newVersion.getBeginn());
        }

        eventRepository.save(existing);
    }
}
