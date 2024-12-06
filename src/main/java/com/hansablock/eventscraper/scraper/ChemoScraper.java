package com.hansablock.eventscraper.scraper;

import com.hansablock.eventscraper.Event;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChemoScraper implements Scraper {

    @Override
    public List<Event> scrapeEvents() {
        List<Event> events = new ArrayList<>();

        try {
            Document doc = Jsoup.connect("https://www.chemiefabrik.info/gigs-2/").get();
            Elements eventList = doc.select("div.jet-listing-grid__item");

            for (Element event : eventList) {
                // Parse date
                String dateText = event.selectFirst(".jet-listing-dynamic-field__content").text();
                dateText = dateText.substring(dateText.indexOf(' ') + 1); // Remove day of week
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy");
                LocalDate date = LocalDate.parse(dateText, dateFormatter);

                // Parse entry and start times
                LocalTime entryTime = parseTime(event, ".elementor-element-6c7315b .elementor-widget-container", "Einlass:");
                LocalTime startTime = parseTime(event, ".elementor-element-c75cd6a .elementor-widget-container", "Beginn:");

                // Parse price (use AK price)
                String priceText = event.select(".jet-listing-dynamic-field__content:contains(AK)").text();
                BigDecimal price = parsePrice(priceText);

                // Parse title
                String title = event.select(".elementor-element-a0688f1 h4").text();

                // Parse genres
                Elements genreElements = event.select(".jet-listing-dynamic-repeater__item");
                String genre = genreElements.stream()
                        .map(Element::text)
                        .reduce((first, second) -> first + "\n" + second) // Concatenate with newlines
                        .orElse("");

                String location = "Chemiefabrik";
                Event newEvent = new Event(null, title, location, date, genre, entryTime, startTime, price);
                events.add(newEvent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return events;
    }

    /**
     * Helper method to parse time from the event element.
     *
     * @param event      the event element
     * @param selector   the CSS selector for the time element
     * @param prefix     the prefix to identify the time string
     * @return the parsed LocalTime, or null if not found
     */
    private LocalTime parseTime(Element event, String selector, String prefix) {
        String timeText = event.select(selector).text();
        if (timeText.contains(prefix)) {
            String[] parts = timeText.split(" ");
            if (parts.length > 1) {
                String timePart = parts[1].replace(".", ":");
                return LocalTime.parse(timePart, DateTimeFormatter.ofPattern("HH:mm"));
            }
        }
        return null;
    }

    /**
     * Helper method to parse price from the price text.
     *
     * @param priceText the price text
     * @return the parsed BigDecimal price, or BigDecimal.ZERO if parsing fails
     */
    private BigDecimal parsePrice(String priceText) {
        Pattern pattern = Pattern.compile("\\d+(\\.\\d{1,2})?");
        Matcher matcher = pattern.matcher(priceText);
        if (matcher.find()) {
            return new BigDecimal(matcher.group());
        }
        return BigDecimal.ZERO;
    }
}