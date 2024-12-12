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

    @Override
    public List<Event> scrapeEvents() {
        List<Event> events = new ArrayList<>();

        try {
            Document doc = Jsoup.connect("https://hanse3.de/Veranstaltungen/").get();
            Elements eventList = doc.select("#content > article:not(#past_events)");

            for (Element eventElement : eventList) {
                // Parse date
                String dateText = eventElement.select("h1 > span:nth-of-type(1)").text().trim();
                LocalDate date = parseDate(dateText);

                // Parse start time
                String timeText = eventElement.select("h1 > span:nth-of-type(2)").text().trim();
                LocalTime startTime = parseTime(timeText);

                // Parse title
                String title = eventElement.select("h2").text().trim();

                // Parse misc
                String misc = eventElement.select("div:not(.show_gallery):not(:has(img))").text().trim();

                // Parse thumbnail
                String thumbnail = eventElement.select("div.show_gallery a > img").attr("src").trim();

                // Limit title length to 255 characters
                if (title.length() > 255) {
                    title = title.substring(0, 252) + "...";
                }

                // Location is constant
                String location = "Hanse3";

                Event newEvent = new Event(null, title, location, date, misc, null, startTime, null, misc, thumbnail);
                events.add(newEvent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return events;
    }

    /**
     * Helper method to parse date from string.
     *
     * @param dateText the text containing the date
     * @return the parsed LocalDate
     */
    private LocalDate parseDate(String dateText) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return LocalDate.parse(dateText, formatter);
        } catch (Exception e) {
            System.err.println("Failed to parse date: " + dateText);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Helper method to parse time from string.
     *
     * @param timeText the text containing the time
     * @return the parsed LocalTime
     */
    private LocalTime parseTime(String timeText) {
        try {
            String timeCleaned = timeText.replace(" Uhr", "").trim();
            return LocalTime.parse(timeCleaned, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            System.err.println("Failed to parse time: " + timeText);
            e.printStackTrace();
            return null;
        }
    }
}
