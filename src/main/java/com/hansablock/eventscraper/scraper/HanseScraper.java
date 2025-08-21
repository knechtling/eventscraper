package com.hansablock.eventscraper.scraper;

import com.hansablock.eventscraper.Event;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class HanseScraper implements Scraper {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public List<Event> scrapeEvents() {
        List<Event> events = new ArrayList<>();

        try {
            Document doc = Jsoup.connect("https://hanse3.de/Veranstaltungen/").get();
            Elements eventList = doc.select("#content > article:not(#past_events)");

            for (Element eventElement : eventList) {
                // Parse date (e.g., 21.08.2025)
                String dateText = eventElement.select("h1 > span:nth-of-type(1)").text().trim();
                LocalDate date = parseDate(dateText);

                // Parse start time (e.g., 20:00 Uhr)
                String timeText = eventElement.select("h1 > span:nth-of-type(2)").text().trim();
                LocalTime startTime = parseTime(timeText);

                // Parse title
                String title = eventElement.select("h2").text().trim();

                // Parse misc
                String misc = eventElement.select("div:not(.show_gallery):not(:has(img))").text().trim();
                if (misc.length() > 1000) {
                    misc = misc.substring(0, 997) + "...";
                }

                // Parse thumbnail
                String thumbnail = eventElement.select("div.show_gallery a \u003e img").attr("src").trim();

                // Source URL if present on title link
                String sourceUrl = eventElement.selectFirst("h2 a") != null ? eventElement.selectFirst("h2 a").attr("href") : "";

                // Limit title length to 255 characters
                if (title.length() > 255) {
                    title = title.substring(0, 252) + "...";
                }

                // Location is constant
                String location = "Hanse3";

                Event newEvent = new Event(null, title, location, date, misc, null, startTime, null, misc, thumbnail);
                if (sourceUrl == null || sourceUrl.isBlank()) sourceUrl = "https://hanse3.de/Veranstaltungen/";
                newEvent.setSourceUrl(sourceUrl);
                events.add(newEvent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return events;
    }

    private LocalDate parseDate(String dateText) {
        try {
            if (dateText == null || dateText.isBlank()) return null;
            // The site sometimes lists multiple dates separated by spaces, and uses either dd.MM.yyyy or dd/MM/yyyy
            String[] tokens = dateText.replace(',', ' ').trim().split("\\s+");
            for (String token : tokens) {
                String t = token.trim();
                if (t.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                    return LocalDate.parse(t, DATE_FMT);
                }
                if (t.matches("\\d{2}/\\d{2}/\\d{4}")) {
                    DateTimeFormatter slash = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                    return LocalDate.parse(t, slash);
                }
            }
            throw new IllegalArgumentException("No parsable date token in: " + dateText);
        } catch (Exception e) {
            System.err.println("[Hanse3] Failed to parse date: " + dateText);
            e.printStackTrace();
            return null;
        }
    }

    private LocalTime parseTime(String timeText) {
        try {
            String firstTime = timeText.replace(" Uhr", "").trim().split(" ")[0];
            return LocalTime.parse(firstTime, TIME_FMT);
        } catch (Exception e) {
            System.err.println("[Hanse3] Failed to parse time: " + timeText);
            e.printStackTrace();
            return null;
        }
    }
}
