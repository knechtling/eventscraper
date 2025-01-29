package com.hansablock.eventscraper.scraper;

import com.hansablock.eventscraper.Event;
import com.hansablock.eventscraper.EventHasher;
import com.hansablock.eventscraper.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

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
                    .peek(event -> event.setEventHash(EventHasher.generateHash(event)))
                    .toList();

            saveEventsSmart(events);
            System.out.println("Scraped " + events.size() + " events from " + scraper.getClass().getSimpleName());
        }
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
        // Only update mutable fields
        if (StringUtils.hasText(newVersion.getPrice())) {
            existing.setPrice(newVersion.getPrice());
        }
        if (StringUtils.hasText(newVersion.getDescription())) {
            existing.setDescription(newVersion.getDescription());
        }

        eventRepository.save(existing);
    }
}