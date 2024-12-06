package com.hansablock.eventscraper.scraper;

import com.hansablock.eventscraper.Event;

import java.util.List;

public interface Scraper {
    List<Event> scrapeEvents();
}