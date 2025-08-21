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
import java.util.stream.Collectors;

@Component
public class ChemoScraper implements Scraper {

    @Override
    public List<Event> scrapeEvents() {
        List<Event> events = new ArrayList<>();

        try {
            Document doc = Jsoup.connect("https://www.chemiefabrik.info/gigs/").get();
            Elements eventList = doc.select("div.jet-listing-grid__item"
                    + ":not(:contains(abgesagt))"
                    + ":not(:contains(verlegt))");

            for (Element event : eventList) {
                // Parse date (format like: "Do 21.08.25")
                String dateText = event.selectFirst(".jet-listing-dynamic-field__content").text();
                String trimmed = dateText.substring(dateText.indexOf(' ') + 1); // Remove day of week
                String[] dateParts = trimmed.split("\\.");
                int day = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]);
                String yearPart = dateParts[2];
                int year = yearPart.length() == 2 ? Integer.parseInt(yearPart) + 2000 : Integer.parseInt(yearPart);
                LocalDate date = LocalDate.of(year, month, day);

                // Parse entry and start times
                LocalTime entryTime = parseTime(event, ".elementor-element-6c7315b .elementor-widget-container", "Einlass:");
                LocalTime startTime = parseTime(event, ".elementor-element-c75cd6a .elementor-widget-container", "Beginn:");

                String priceRegex = "(?:AK|VVK): \\d+(?:(?:\\.|\\,)\\d{1,2})?";
                Elements priceElements = event.select(".jet-listing-dynamic-field__content:matches(" + priceRegex + ")");
                String priceText = priceElements.stream()
                        .map(Element::text)
                        .distinct()
                        .collect(Collectors.joining("\n"));

                // Parse title
                String title = event.select(".elementor-element-a0688f1 h4").text();

                // Source URL if present
                String sourceUrl = event.selectFirst(".elementor-element-a0688f1 a") != null ? event.selectFirst(".elementor-element-a0688f1 a").attr("href") : "";

                // Parse description
                Elements descriptionElements = event.select("div.jet-listing-dynamic-repeater__item:not(:has(h4)):has(.bandlink)");
                StringBuilder description = new StringBuilder();
                for (Element descriptionElement : descriptionElements) {
                    description.append(descriptionElement.text()).append("\u003cbr /\u003e");
                }

                // Parse misc
                String misc = event.select(".elementor-widget-jet-listing-dynamic-field > .elementor-widget-container > .display-inline p").text();

                // Parse thumbnail
                String thumbnail = event.select(".size-medium_large").attr("src");

                String location = "Chemiefabrik";
                Event newEvent = new Event(null, title, location, date, description.toString().trim(), entryTime, startTime, priceText, misc, thumbnail);
                if (sourceUrl != null && !sourceUrl.isBlank()) newEvent.setSourceUrl(sourceUrl);
                events.add(newEvent);
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }

        return events;
    }

    /**
     * Helper method to parse time from the event element.
     */
    public LocalTime parseTime(Element event, String selector, String prefix) {
        String timeText = event.select(selector).text();
        if (timeText.contains(prefix)) {
            String[] parts = timeText.split(" ");
            if (parts.length > 1) {
                String timePart = parts[1].replace(".", ":");
                try {
                    return LocalTime.parse(timePart, DateTimeFormatter.ofPattern("HH:mm"));
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }
}
