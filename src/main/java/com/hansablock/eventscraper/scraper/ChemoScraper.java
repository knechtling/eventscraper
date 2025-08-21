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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ChemoScraper implements Scraper {

    private static final Pattern VVK_PATTERN = Pattern.compile("VVK:\\s*([^\\n]+)");
    private static final Pattern AK_PATTERN = Pattern.compile("AK:\\s*([^\\n]+)");
    private static final Pattern SPENDE_PATTERN = Pattern.compile("Spendenempfehlung:\\s*([^\\n]+)");
    private static final Pattern VVK_STELLE_PATTERN = Pattern.compile("VVK-Stelle:\\s*([^\\n]+)");
    private static final Pattern EINLASS_PATTERN = Pattern.compile("Einlass:\\s*(\\d{1,2}[:.]\\d{2})");
    private static final Pattern BEGINN_PATTERN = Pattern.compile("Beginn:\\s*(\\d{1,2}[:.]\\d{2})");

    @Override
    public List<Event> scrapeEvents() {
        List<Event> events = new ArrayList<>();

        try {
            Document doc = Jsoup.connect("https://www.chemiefabrik.info/gigs/").get();
            Elements eventList = doc.select("div.jet-listing-grid__item:not(:contains(abgesagt)):not(:contains(verlegt))");

            for (Element event : eventList) {
                // Parse date (format like: "Fr. 10.10.25" or "Di. 02.09.25")
                String dateText = event.selectFirst(".jet-listing-dynamic-field__content").text();
                String trimmed = dateText.substring(dateText.indexOf(' ') + 1); // Remove weekday + space
                String[] dateParts = trimmed.split("\\.");
                int day = Integer.parseInt(dateParts[0]);
                int month = Integer.parseInt(dateParts[1]);
                String yearPart = dateParts[2];
                int year = yearPart.length() == 2 ? Integer.parseInt(yearPart) + 2000 : Integer.parseInt(yearPart);
                LocalDate date = LocalDate.of(year, month, day);

                // Try structured times first
                LocalTime entryTime = parseTime(event, ".elementor-element-6c7315b .elementor-widget-container", "Einlass:");
                LocalTime startTime = parseTime(event, ".elementor-element-c75cd6a .elementor-widget-container", "Beginn:");

                // Fallback: extract times from full text
                String fullText = event.text();
                if (entryTime == null) {
                    Matcher m = EINLASS_PATTERN.matcher(fullText);
                    if (m.find()) entryTime = parseHHmm(m.group(1));
                }
                if (startTime == null) {
                    Matcher m = BEGINN_PATTERN.matcher(fullText);
                    if (m.find()) startTime = parseHHmm(m.group(1));
                }

                // Prices: collect VVK / AK / Spendenempfehlung and VVK-Stelle
                // 1) Prefer element-scoped extraction to avoid mixing in description
                String vvk = null, ak = null, spende = null, vvkStelle = null;
                for (Element el : event.select(".jet-listing-dynamic-field__content")) {
                    String t = el.text().trim();
                    if (t.startsWith("VVK:")) vvk = cleanPriceValue(t.substring(4));
                    else if (t.startsWith("AK:")) ak = cleanPriceValue(t.substring(3));
                    else if (t.startsWith("Spendenempfehlung:")) spende = cleanPriceValue(t.substring("Spendenempfehlung:".length()));
                    else if (t.startsWith("VVK-Stelle:")) vvkStelle = cleanPriceValue(t.substring("VVK-Stelle:".length()));
                }
                // 2) Fallback to label-bounded extraction from full text if not found
                if (vvk == null) vvk = extractSegment(fullText, "VVK:");
                if (ak == null) ak = extractSegment(fullText, "AK:");
                if (spende == null) spende = extractSegment(fullText, "Spendenempfehlung:");
                if (vvkStelle == null) vvkStelle = extractSegment(fullText, "VVK-Stelle:");

                StringBuilder priceSb = new StringBuilder();
                if (vvk != null && !vvk.isBlank()) priceSb.append("VVK: ").append(vvk.trim());
                if (ak != null && !ak.isBlank()) {
                    if (!priceSb.isEmpty()) priceSb.append(" | ");
                    priceSb.append("AK: ").append(ak.trim());
                }
                if (spende != null && !spende.isBlank()) {
                    if (!priceSb.isEmpty()) priceSb.append(" | ");
                    priceSb.append("Spendenempfehlung: ").append(spende.trim());
                }
                String priceText = cap(priceSb.toString(), 255);

                // Parse title
                String title = event.select(".elementor-element-a0688f1 h4").text();

                // Source URL if present
                String sourceUrl = event.selectFirst(".elementor-element-a0688f1 a") != null ? event.selectFirst(".elementor-element-a0688f1 a").attr("href") : "";

                // Bands/lineup and description
                Elements descriptionElements = event.select("div.jet-listing-dynamic-repeater__item:not(:has(h4))");
                StringBuilder description = new StringBuilder();
                for (Element descriptionElement : descriptionElements) {
                    String line = descriptionElement.text();
                    if (!line.isBlank()) description.append(line).append("\u003cbr /\u003e");
                }

                // Misc: include VVK-Stelle and any trailing info
                StringBuilder misc = new StringBuilder();
                if (vvkStelle != null) misc.append("VVK-Stelle: ").append(vvkStelle.trim());

                // Parse thumbnail
                String thumbnail = event.select(".size-medium_large").attr("src");

                String location = "Chemiefabrik";
                Event newEvent = new Event(null, title, location, date, description.toString().trim(), entryTime, startTime, priceText, misc.toString(), thumbnail);
                if (sourceUrl == null || sourceUrl.isBlank()) sourceUrl = "https://www.chemiefabrik.info/gigs/";
                newEvent.setSourceUrl(sourceUrl);
                events.add(newEvent);
            }
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }

        return events;
    }

    private static String findFirst(Pattern p, String text) {
        Matcher m = p.matcher(text);
        if (m.find()) return m.group(1);
        return null;
    }

    // Extracts the value after a label (e.g., "VVK:") up to the next known label or end of string
    private static String extractSegment(String text, String label) {
        if (text == null) return null;
        String pattern = Pattern.quote(label) + "\\s*(.*?)(?=\\s+(AK:|VVK:|Spendenempfehlung:|VVK-Stelle:|Einlass:|Beginn:)|$)";
        Matcher m = Pattern.compile(pattern).matcher(text);
        if (m.find()) {
            String val = m.group(1).trim();
            // keep it short to avoid pulling sentences; typical tokens are short
            if (val.length() > 120) val = val.substring(0, 120);
            return val;
        }
        return null;
    }

    private static String cleanPriceValue(String s) {
        if (s == null) return null;
        String v = s.trim();
        // Stop at first obvious sentence boundary to avoid dragging narrative text
        int cut = v.indexOf("  "); // double space often separates blocks
        if (cut < 0) cut = v.indexOf("  "); // non-breaking space
        if (cut > 0) v = v.substring(0, cut);
        // Heuristic: if there is a long sequence without currency/number, keep only first 40 chars
        if (!v.matches(".*(\\d|€|Euro|frei|ermäßigt|zzgl).*") && v.length() > 40) {
            v = v.substring(0, 40);
        }
        return v;
    }

    private static String cap(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static LocalTime parseHHmm(String s) {
        String norm = s.replace('.', ':');
        try {
            return LocalTime.parse(norm, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception ignored) {
            return null;
        }
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
