package com.hansablock.eventscraper.scraper;

import com.hansablock.eventscraper.Event;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.util.MapTimeZoneCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Component
public class TanteJuScraper implements Scraper {

    private static final Logger logger = LoggerFactory.getLogger(TanteJuScraper.class);
    private static final String FALLBACK_ICS_URL = "https://www.liveclub-dresden.de/events.ics";

    static {
        // Ensure MapTimeZoneCache is used to resolve the cache issue
        System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache.class.getName());
    }

    @Override
    public List<Event> scrapeEvents() {
        List<Event> events = new ArrayList<>();

        try {
            // Use the fallback ICS URL
            String icsUrl = FALLBACK_ICS_URL;
            logger.info("Using fallback ICS URL: {}", icsUrl);

            // Parse ICS file and get events
            events = parseICSFile(icsUrl);
        } catch (Exception e) {
            logger.error("Error scraping events from Tante Ju", e);
        }

        return events;
    }

    /**
     * Parses the ICS file and converts it into a list of Event objects.
     *
     * @param icsUrl the URL of the ICS file
     * @return a list of Event objects
     */
    private List<Event> parseICSFile(String icsUrl) {
        List<Event> events = new ArrayList<>();
        try (InputStream in = new URL(icsUrl).openStream()) {
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(in);

            logger.info("Parsing ICS file. Total components: {}", calendar.getComponents().size());

            calendar.getComponents().forEach(component -> {
                logger.debug("Component type: {}", component.getName());
                if (component instanceof VEvent) {
                    try {
                        VEvent vEvent = (VEvent) component;

                        // Extract event details
                        String title = vEvent.getSummary() != null ? vEvent.getSummary().getValue() : "No Title";
                        LocalDateTime startDateTime = LocalDateTime.ofInstant(vEvent.getStartDate().getDate().toInstant(), ZoneId.of("Europe/Berlin"));
                        String url = vEvent.getProperty("URL") != null ? vEvent.getProperty("URL").getValue() : "";
                        String description = vEvent.getDescription() != null ? vEvent.getDescription().getValue() : "";

                        // Use description as misc
                        events.add(new Event(null, title, "Tante Ju", startDateTime.toLocalDate(), null, startDateTime.toLocalTime(), null, "", description, url));
                        logger.info("Added event: {}", title);
                    } catch (Exception e) {
                        logger.error("Error processing event: {}", component, e);
                    }
                }
            });
            logger.info("Total events added: {}", events.size());
        } catch (Exception e) {
            logger.error("Error parsing ICS file", e);
        }
        return events;
    }
}
