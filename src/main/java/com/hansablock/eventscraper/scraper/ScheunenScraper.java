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
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class ScheunenScraper implements Scraper {

    private static final String BASE_URL = "https://scheune.org";

    @Override
    public List<Event> scrapeEvents() {
        List<Event> events = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(BASE_URL).get();
            Elements eventList = doc.select("div.va"); // Each "va" represents an event

            LocalDate currentEventDate = null; // To hold the last valid date
            Month currentMonth = null;
            int currentYear = LocalDate.now().getYear();

            // Find the month and year
            Element monthElement = doc.selectFirst("h3.head.monat");
            if (monthElement != null) {
                String[] monthYearText = monthElement.text().split(" ");
                currentMonth = getMonthFromGerman(monthYearText[0]);
                if (monthYearText.length > 1 && monthYearText[1].matches("\\d{4}")) {
                    currentYear = Integer.parseInt(monthYearText[1]);
                }
            }

            for (Element eventElement : eventList) {
                // Parse date for main events
                if (!eventElement.hasClass("follow_up")) {
                    String dateText = eventElement.select("span.monat_tag").text().trim();
                    String[] dateParts = dateText.split(" ");
                    if (dateParts.length == 2) {
                        int day = Integer.parseInt(dateParts[0].replace(".", ""));
                        currentEventDate = LocalDate.of(currentYear, currentMonth, day);
                    }
                }

                // Parse title
                String title = eventElement.select("h2.va_titel").text();

                // Parse Beginn time
                String beginnText = eventElement.select("span.va_uhrzeit").text();
                String beginn = beginnText.replace(" Uhr", "");

                // Parse description
                String description = eventElement.select("a.term.reihe_link").text() + "<br>" + eventElement.select("a.va_location").text();

                // Follow link for misc and thumbnail
                String misc = "";
                String thumbnail = "";
                Element titleElement = eventElement.selectFirst("div.titel > a");
                if (titleElement != null) {
                    String detailPageUrl = titleElement.attr("href");
                    Document detailDoc = Jsoup.connect(detailPageUrl).get();

                    // Parse misc from detail page
                    Element miscElement = detailDoc.selectFirst("span.va_txt p");
                    if (miscElement != null) {
                        misc = miscElement.html();
                    }

                    // Parse thumbnail from detail page
                    Element thumbnailElement = detailDoc.selectFirst("figure a[rel='galerie[]'] img");
                    if (thumbnailElement != null) {
                        thumbnail = thumbnailElement.attr("src");
                    }
                }

                // Set fixed location
                String location = "Scheune Dresden";

                // Create the event
                Event newEvent = new Event(
                        null,
                        title,
                        location,
                        currentEventDate,
                        description,
                        null, // Entry time not provided
                        LocalTime.parse(beginn),
                        null, // Price not provided
                        misc,
                        thumbnail
                );

                events.add(newEvent);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return events;
    }

    /**
     * Helper method to map German month names to java.time.Month enum.
     *
     * @param germanMonth the German month name
     * @return the corresponding java.time.Month
     */
    private static Month getMonthFromGerman(String germanMonth) {
        switch (germanMonth.toLowerCase(java.util.Locale.GERMAN)) {
            case "januar": return Month.JANUARY;
            case "februar": return Month.FEBRUARY;
            case "märz": return Month.MARCH;
            case "april": return Month.APRIL;
            case "mai": return Month.MAY;
            case "juni": return Month.JUNE;
            case "juli": return Month.JULY;
            case "august": return Month.AUGUST;
            case "september": return Month.SEPTEMBER;
            case "oktober": return Month.OCTOBER;
            case "november": return Month.NOVEMBER;
            case "dezember": return Month.DECEMBER;
            default: throw new IllegalArgumentException("Invalid month: " + germanMonth);
        }
    }
}
