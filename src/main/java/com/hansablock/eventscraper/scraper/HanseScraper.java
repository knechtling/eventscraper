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
            Elements eventList = doc.select("#content > .termin");

            for (Element eventElement : eventList) {
                // Parse one or more dates (e.g., "21.08.2025, 22.08.2025" or with slashes)
                String dateText = eventElement.select("h1 > span:nth-of-type(1)").text().trim();
                List<LocalDate> dates = parseDates(dateText);

                // Parse start time (e.g., 20:00 Uhr)
                String timeText = eventElement.select("h1 > span:nth-of-type(2)").text().trim();
                LocalTime startTime = parseTime(timeText);

                // Parse title
                String title = eventElement.select("h2").text().trim();

                // Description teaser from h3 (HTML allowed, will be sanitized later)
                String descriptionHtml = eventElement.select("h3").html().trim();
                if (descriptionHtml.length() > 2000) {
                    descriptionHtml = descriptionHtml.substring(0, 1997) + "...";
                }

                // Brief misc from the first content div after gallery, text-only
                String misc = eventElement.select(".show_gallery ~ div").text().trim();
                if (misc.length() > 600) {
                    misc = misc.substring(0, 597) + "...";
                }

                // Parse thumbnail
                String thumbnail = eventElement.select("div.show_gallery a > img").attr("src").trim();

                // Source URL: use page itself as fallback
                String sourceUrl = "https://hanse3.de/Veranstaltungen/";

                // Limit title length to 255 characters
                if (title.length() > 255) {
                    title = title.substring(0, 252) + "...";
                }

                // Location is constant
                String location = "Hanse 3";

                if (dates.isEmpty()) {
                    // If no date parsed, skip this article (ScraperService will already drop null dates)
                    continue;
                }

                for (LocalDate date : dates) {
                    Event newEvent = new Event(null, title, location, date, descriptionHtml, null, startTime, null, misc, thumbnail);
                    if (sourceUrl == null || sourceUrl.isBlank()) sourceUrl = "https://hanse3.de/Veranstaltungen/";
                    newEvent.setSourceUrl(sourceUrl);
                    events.add(newEvent);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return events;
    }

    // Parses possibly multiple dates from a string and returns all recognized dates
    private List<LocalDate> parseDates(String dateText) {
        List<LocalDate> out = new ArrayList<>();
        if (dateText == null || dateText.isBlank()) return out;
        java.util.regex.Pattern dot = java.util.regex.Pattern.compile("\\b(\\d{1,2}\\.\\d{1,2}\\.\\d{2,4})\\b");
        java.util.regex.Pattern slash = java.util.regex.Pattern.compile("\\b(\\d{1,2}/\\d{1,2}/\\d{2,4})\\b");
        java.util.regex.Matcher m1 = dot.matcher(dateText);
        while (m1.find()) {
            LocalDate d = parseDate(m1.group(1));
            if (d != null) out.add(d);
        }
        java.util.regex.Matcher m2 = slash.matcher(dateText);
        while (m2.find()) {
            LocalDate d = parseDate(m2.group(1));
            if (d != null) out.add(d);
        }
        return out;
    }

    private LocalDate parseDate(String dateText) {
        try {
            if (dateText == null || dateText.isBlank()) return null;
            String t = dateText.trim();
            if (t.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}")) {
                String[] parts = t.split("\\.");
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);
                if (parts[2].length() == 2) year += 2000;
                return LocalDate.of(year, month, day);
            }
            if (t.matches("\\d{1,2}/\\d{1,2}/\\d{2,4}")) {
                String[] parts = t.split("/");
                int day = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);
                if (parts[2].length() == 2) year += 2000;
                return LocalDate.of(year, month, day);
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
