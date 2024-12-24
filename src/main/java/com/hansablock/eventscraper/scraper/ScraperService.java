package com.hansablock.eventscraper.scraper;

import com.hansablock.eventscraper.Event;
import com.hansablock.eventscraper.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void scrapeAndSaveEvents() {
        for (Scraper scraper : scrapers) {
            List<Event> events = scraper.scrapeEvents();
            for (Event event : events) {
                eventRepository.save(event);
            }
        }
    }
}
