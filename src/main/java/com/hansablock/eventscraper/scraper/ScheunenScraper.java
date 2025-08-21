package com.hansablock.eventscraper.scraper;

import com.hansablock.eventscraper.Event;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ScheunenScraper.class);
    private static final String BASE_URL = "https://scheune.org";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public List<Event> scrapeEvents() {
        logger.info("Scraping events from Scheune");

        List<Event> events = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(BASE_URL).get();

            LocalDate currentEventDate = null; // To hold the last valid date
            int currentYear = LocalDate.now().getYear(); // Default year

            // Track the current month and year from headers
            for (Element headerOrEvent : doc.select("h3.head.monat, div.va")) {
                if (headerOrEvent.tagName().equals("h3")) {
                    // Update currentYear and month from header
                    String[] monthYearText = headerOrEvent.text().split(" ");
                    if (monthYearText.length == 2 && monthYearText[1].matches("\\d{4}")) {
                        currentYear = Integer.parseInt(monthYearText[1]);
                    }
                } else if (headerOrEvent.hasClass("va")) {
                    // Parse date for main events
                    if (!headerOrEvent.hasClass("follow_up")) {
                        String dateText = headerOrEvent.select("span.monat_tag").text().trim();
                        String[] dateParts = dateText.split(" ");
                        if (dateParts.length == 2) {
                            int day = Integer.parseInt(dateParts[0].replace(".", ""));
                            Month month = getMonthFromGerman(dateParts[1]);
                            currentEventDate = LocalDate.of(currentYear, month, day);
                        }
                    }

                    // Skip if date could not be determined yet
                    if (currentEventDate == null) {
                        continue;
                    }

                    // Parse title
                    String title = headerOrEvent.select("h2.va_titel").text();

                    // Parse Beginn time
                    String beginnText = headerOrEvent.select("span.va_uhrzeit").text();
                    String beginn = beginnText.replace(" Uhr", "");
                    LocalTime beginnTime = null;
                    try {
                        beginnTime = LocalTime.parse(beginn, TIME_FMT);
                    } catch (Exception ignored) {}

                    // Parse description
                    String description = headerOrEvent.select("a.term.reihe_link").text() + "\u003cbr\u003e" + headerOrEvent.select("a.va_location").text();

                    // Follow link for misc and thumbnail
                    String misc = "";
                    String thumbnail = "";
                    LocalTime einlassTime = null;
                    String price = null;
                    String location = "Scheune Dresden";

                    Element titleElement = headerOrEvent.selectFirst("div.titel > a");
                    String detailPageUrl = null;
                    if (titleElement != null) {
                        detailPageUrl = titleElement.attr("href");
                        if (detailPageUrl.startsWith("/")) {
                            detailPageUrl = BASE_URL + detailPageUrl;
                        }
                        Document detailDoc = Jsoup.connect(detailPageUrl).get();

                        // Prefer richer description from left column paragraphs
                        Elements leftParas = detailDoc.select("#cont .col_left p");
                        if (!leftParas.isEmpty()) {
                            StringBuilder desc = new StringBuilder();
                            int count = 0;
                            for (Element p : leftParas) {
                                String html = p.html().trim();
                                if (html.isEmpty()) continue;
                                if (desc.length() > 0) desc.append("<br>");
                                desc.append(html);
                                if (++count >= 3) break; // keep it concise, details page will show full source
                            }
                            if (desc.length() > 0) {
                                description = desc.toString();
                            }
                        }

                        // Fallback misc from any descriptive block
                        if (misc.isBlank()) {
                            Element miscElement = detailDoc.selectFirst("span.va_txt p, .contentblock p");
                            if (miscElement != null) {
                                misc = miscElement.text();
                            }
                        }

                        // Extract Einlass and Price from raw text
                        String allText = detailDoc.text();
                        java.util.regex.Matcher mEin = java.util.regex.Pattern.compile("Einlass\\s*(ab\\s*)?(\\d{1,2}:\\d{2})\\s*Uhr", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(allText);
                        if (mEin.find()) {
                            try { einlassTime = LocalTime.parse(mEin.group(2), TIME_FMT); } catch (Exception ignored) {}
                        }
                        java.util.regex.Matcher mBeg = java.util.regex.Pattern.compile("(\\d{1,2}:\\d{2})\\s*Uhr").matcher(allText);
                        if (beginnTime == null && mBeg.find()) {
                            try { beginnTime = LocalTime.parse(mBeg.group(1), TIME_FMT); } catch (Exception ignored) {}
                        }

                        // Price: only parse concise tokens and avoid capturing calendar/sidebar noise
                        String rightText = detailDoc.select("#cont .col_right").text();
                        java.util.Set<String> priceBits = new java.util.LinkedHashSet<>();
                        // Eintritt frei variants
                        java.util.regex.Matcher mFree = java.util.regex.Pattern.compile("(?i)eintritt( ist)? frei").matcher(allText);
                        if (mFree.find()) {
                            priceBits.add("Eintritt frei");
                        }
                        // AK: ... and VVK: ... limited tokens (up to 25 chars of typical price chars)
                        java.util.regex.Matcher mAk = java.util.regex.Pattern.compile("(?i)\\bAK:?\\s*([\\p{L}\\p{N}€.,/–- ]{1,25})").matcher(rightText);
                        while (mAk.find()) {
                            String val = mAk.group(1).trim();
                            if (!val.isEmpty()) priceBits.add("AK: " + val);
                        }
                        java.util.regex.Matcher mVvk = java.util.regex.Pattern.compile("(?i)\\bVVK:?\\s*([\\p{L}\\p{N}€.,/–- ]{1,25})").matcher(rightText);
                        while (mVvk.find()) {
                            String val = mVvk.group(1).trim();
                            if (!val.isEmpty()) priceBits.add("VVK: " + val);
                        }
                        // Clean up accidental lone 'frei'
                        java.util.List<String> cleaned = new java.util.ArrayList<>();
                        for (String pbit : priceBits) {
                            String t = pbit.replaceAll("\\s+", " ").trim();
                            if (t.equalsIgnoreCase("frei")) continue;
                            cleaned.add(t);
                        }
                        if (!cleaned.isEmpty()) {
                            price = String.join(" | ", cleaned);
                        }

                        // Parse more specific location if present from right column header line
                        String headerBlock = detailDoc.select("#cont .col_right").text();
                        java.util.regex.Matcher mHeaderLoc = java.util.regex.Pattern.compile(
                                "(?i)(Montag|Dienstag|Mittwoch|Donnerstag|Freitag|Samstag|Sonntag)\\s*\\|\\s*\\d{1,2}\\.\\s*[A-Za-zÄÖÜäöüß]+\\s+(.+?)\\s+\\d{1,2}:\\d{2}\\s*Uhr"
                        ).matcher(headerBlock);
                        if (mHeaderLoc.find()) {
                            String locCandidate = mHeaderLoc.group(2).trim();
                            // normalize common variants
                            if (locCandidate.toLowerCase().startsWith("scheune")) {
                                // e.g., "scheune Vorplatz" -> "Scheune Vorplatz"
                                location = locCandidate.substring(0,1).toUpperCase() + locCandidate.substring(1);
                            } else {
                                location = locCandidate;
                            }
                        } else {
                            // Fallback: try simple "scheune <word>" within page text
                            java.util.regex.Matcher mLoc = java.util.regex.Pattern.compile("scheune\\s+([A-Za-zÄÖÜäöüß]+)").matcher(allText);
                            if (mLoc.find()) {
                                location = "Scheune " + mLoc.group(1);
                            }
                        }

                        // Parse thumbnail from detail page
                        Element thumbnailElement = detailDoc.selectFirst("figure a[rel='galerie[]'] img, figure img");
                        if (thumbnailElement != null) {
                            thumbnail = thumbnailElement.attr("src");
                        }
                    }

                    // Create the event
                    Event newEvent = new Event(
                            null,
                            title,
                            location,
                            currentEventDate,
                            description,
                            einlassTime,
                            beginnTime,
                            price,
                            misc,
                            thumbnail
                    );

                    // set source URL if we visited details; otherwise default to base
                    newEvent.setSourceUrl(detailPageUrl != null && !detailPageUrl.isBlank() ? detailPageUrl : BASE_URL);

                    events.add(newEvent);
                }
            }
        } catch (IOException e) {
            logger.error("Error scraping events from Scheune", e);
        }

        return events;
    }

    /**
     * Helper method to map German month names to java.time.Month enum.
     */
    public static Month getMonthFromGerman(String germanMonth) {
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
