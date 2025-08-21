package com.hansablock.eventscraper.scraper;

import com.hansablock.eventscraper.Event;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.util.MapTimeZoneCache;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TanteJuScraper implements Scraper {

    private static final Logger logger = LoggerFactory.getLogger(TanteJuScraper.class);
    private static final String FALLBACK_ICS_URL = "https://www.liveclub-dresden.de/events.ics";

    static {
        System.setProperty("net.fortuna.ical4j.timezone.cache.impl", MapTimeZoneCache.class.getName());
    }

    @Override
    public List<Event> scrapeEvents() {
        List<Event> events = new ArrayList<>();
        try {
            String icsUrl = FALLBACK_ICS_URL;
            logger.info("Using fallback ICS URL: {}", icsUrl);
            events = parseICSFile(icsUrl);
        } catch (Exception e) {
            logger.error("Error scraping events from Tante Ju", e);
        }
        return events;
    }

    private List<Event> parseICSFile(String icsUrl) {
        List<Event> events = new ArrayList<>();
        try (InputStream in = new URL(icsUrl).openStream()) {
            CalendarBuilder builder = new CalendarBuilder();
            Calendar calendar = builder.build(in);
            logger.info("Parsing ICS file. Total components: {}", calendar.getComponents().size());
            calendar.getComponents().forEach(component -> {
                if (component instanceof VEvent) {
                    try {
                        VEvent vEvent = (VEvent) component;
                        Event event = createEventFromVEvent(vEvent);
                        events.add(event);
                        logger.info("Added event: {}", event);
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

    private Event createEventFromVEvent(VEvent vEvent) {
        String title = vEvent.getSummary() != null ? vEvent.getSummary().getValue() : "No Title";
        LocalDateTime startDateTime = LocalDateTime.ofInstant(vEvent.getStartDate().getDate().toInstant(), ZoneId.of("Europe/Berlin"));
        String url = vEvent.getProperty("URL") != null ? vEvent.getProperty("URL").getValue() : "";
        String description = vEvent.getDescription() != null ? vEvent.getDescription().getValue() : "";
        return fetchEventDetails(title, startDateTime, url, description);
    }

    private Event fetchEventDetails(String title, LocalDateTime startDateTime, String url, String descriptionFromIcs) {
        String price = "";
        String thumbnailUrl = "";
        LocalTime einlassTime = null;
        LocalTime beginnTime = null;
        String pageSnippet = "";
        try {
            if (url != null && !url.isBlank()) {
                Document doc = Jsoup.connect(url).get();
                price = extractPrice(doc);
                thumbnailUrl = extractThumbnailUrl(doc);
                Element infosElement = doc.selectFirst("div.single_event_infos");
                if (infosElement != null) {
                    String infosText = infosElement.text();
                    einlassTime = extractTime(infosText, "Einlass:");
                    beginnTime = extractTime(infosText, "Beginn:");
                }
                Element snippet = doc.selectFirst("div.single_event_text");
                if (snippet != null) {
                    pageSnippet = snippet.text();
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching event details for URL: {}", url, e);
        }
        String misc = (descriptionFromIcs == null ? "" : descriptionFromIcs) + (url == null ? "" : ("\n" + url));
        String description = truncate(pageSnippet.isBlank() ? descriptionFromIcs : pageSnippet, 200);
        return new Event(null, title, "Tante Ju", startDateTime != null ? startDateTime.toLocalDate() : null, description, einlassTime, beginnTime, price, misc, thumbnailUrl);
    }

    private String truncate(String text, int length) {
        if (text == null) return "";
        if (text.length() <= length) return text;
        return text.substring(0, length) + "...";
    }

    private String extractPrice(Document doc) {
        Element priceElement = doc.selectFirst("div.single_event_price");
        if (priceElement != null) {
            String[] prices = priceElement.text().replace("Eintritt:", "").trim().split("/");
            StringBuilder price = new StringBuilder();
            for (String p : prices) {
                if (p.matches(".*\\d.*")) {
                    price.append(p).append(" ");
                }
            }
            return price.toString().trim();
        }
        return "";
    }

    public String extractThumbnailUrl(Document doc) {
        if (doc == null) return "";
        Element thumbnailElement = doc.selectFirst(".news img[src$=.jpg], .news img[src$=.jpeg], .news img[src$=.png]");
        if (thumbnailElement == null) {
            thumbnailElement = doc.selectFirst("img.soliloquy-preload");
        }
        if (thumbnailElement == null) {
            thumbnailElement = doc.selectFirst("img[src]");
        }
        return thumbnailElement != null ? thumbnailElement.attr("src") : "";
    }

    public static LocalTime extractTime(String text, String label) {
        if (text == null) return null;
        // First try strict label-prefixed pattern
        try {
            Pattern pattern = Pattern.compile(Pattern.quote(label) + "\\s*(\\d{2}:\\d{2})\\s*Uhr");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String timeString = matcher.group(1);
                return LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"));
            }
        } catch (Exception ignored) {}
        // Fallback: find any HH:mm pattern in the text
        Matcher generic = Pattern.compile("(\\d{1,2})[:.](\\d{2})").matcher(text);
        if (generic.find()) {
            int h = Integer.parseInt(generic.group(1));
            int m = Integer.parseInt(generic.group(2));
            if (h >= 0 && h < 24 && m >= 0 && m < 60) {
                return LocalTime.of(h, m);
            }
        }
        return null;
    }
}
