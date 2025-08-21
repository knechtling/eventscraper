package com.hansablock.eventscraper.scraper;

import com.hansablock.eventscraper.Event;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChemoScraper implements Scraper {

  private static final Logger logger = LoggerFactory.getLogger(ChemoScraper.class);

  private static final Pattern VVK_PATTERN = Pattern.compile("VVK:\\s*([^\\n]+)");
  private static final Pattern AK_PATTERN = Pattern.compile("AK:\\s*([^\\n]+)");
  private static final Pattern SPENDE_PATTERN = Pattern.compile("Spendenempfehlung:\\s*([^\\n]+)");
  private static final Pattern VVK_STELLE_PATTERN = Pattern.compile("VVK-Stelle:\\s*([^\\n]+)");
  private static final Pattern EINLASS_PATTERN =
      Pattern.compile("Einlass:\\s*(\\d{1,2}[:.]\\d{2})");
  private static final Pattern BEGINN_PATTERN = Pattern.compile("Beginn:\\s*(\\d{1,2}[:.]\\d{2})");

  @Override
  public List<Event> scrapeEvents() {
    List<Event> events = new ArrayList<>();

    try {
      Document doc = Jsoup.connect("https://www.chemiefabrik.info/gigs/").get();
      Elements eventList =
          doc.select("div.jet-listing-grid__item:not(:contains(abgesagt)):not(:contains(verlegt))");

      for (Element event : eventList) {
        // Parse date (format like: "Fr. 10.10.25" or "Di. 02.09.25")
        String dateText = event.selectFirst(".jet-listing-dynamic-field__content").text();
        String trimmed = dateText.substring(dateText.indexOf(' ') + 1); // Remove weekday + space
        String[] dateParts = trimmed.split("\\.");
        int day = Integer.parseInt(dateParts[0]);
        int month = Integer.parseInt(dateParts[1]);
        String yearPart = dateParts[2];
        int year =
            yearPart.length() == 2 ? Integer.parseInt(yearPart) + 2000 : Integer.parseInt(yearPart);
        LocalDate date = LocalDate.of(year, month, day);

        // Try structured times first
        LocalTime entryTime =
            parseTime(event, ".elementor-element-6c7315b .elementor-widget-container", "Einlass:");
        LocalTime startTime =
            parseTime(event, ".elementor-element-c75cd6a .elementor-widget-container", "Beginn:");

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
          else if (t.startsWith("Spendenempfehlung:"))
            spende = cleanPriceValue(t.substring("Spendenempfehlung:".length()));
          else if (t.startsWith("VVK-Stelle:"))
            vvkStelle = cleanPriceValue(t.substring("VVK-Stelle:".length()));
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

        // Parse title: prefer clear heading widgets, fallback to first meaningful header in block
        String location = "Chemiefabrik";
        String title = extractTitle(event, location);
        if (title == null) title = "";

        // Source URL if present
        String sourceUrl =
            event.selectFirst(".elementor-element-a0688f1 a") != null
                ? event.selectFirst(".elementor-element-a0688f1 a").attr("href")
                : "";

        // Description: aggregate headers + meaningful dynamic fields (but do not duplicate the
        // title)
        List<String> descParts = new ArrayList<>();

        // Prepare normalization helpers
        String normTitle = normalize(title);

        // 1) Include headers within the block (e.g., lineup headings) and emphasize them
        for (Element h :
            event.select(
                "h1,h2,h3,h4,h5,h6, .jet-listing-dynamic-repeater__item h1, .jet-listing-dynamic-repeater__item h2, .jet-listing-dynamic-repeater__item h3, .jet-listing-dynamic-repeater__item h4, .jet-listing-dynamic-repeater__item h5, .jet-listing-dynamic-repeater__item h6")) {
          String ht = h.text().trim();
          if (!ht.isBlank()) {
            // avoid duplicating the main title in description
            if (!normalize(ht).equals(normTitle)) {
              descParts.add("<strong>" + ht + "</strong>");
            }
          }
        }

        // 2) Include dynamic field contents that are not meta labels (exclude title/date-like)
        for (Element el : event.select(".jet-listing-dynamic-field__content")) {
          String t = el.text().trim();
          if (t.isBlank()) continue;
          if (t.startsWith("VVK:")
              || t.startsWith("AK:")
              || t.startsWith("Spendenempfehlung:")
              || t.startsWith("VVK-Stelle:")
              || t.startsWith("Einlass:")
              || t.startsWith("Beginn:")) {
            continue;
          }
          if (normalize(t).equals(normTitle)) continue; // skip duplicates of title
          if (isDateLike(t)) continue; // skip date-only lines like "Fr. 12.09.25"
          if (!containsCaseInsensitive(descParts, t)) descParts.add(t);
        }

        // 3) Include repeater items (bands/genres/notes), also exclude title/date-like
        for (Element descriptionElement : event.select("div.jet-listing-dynamic-repeater__item")) {
          String line = descriptionElement.text().trim();
          if (line.isBlank()) continue;
          if (normalize(line).equals(normTitle)) continue;
          if (isDateLike(line)) continue;
          if (!containsCaseInsensitive(descParts, line)) descParts.add(line);
        }

        // 4) Aftershow and presenters if present
        for (String token : List.of("Aftershow", "Aftershow mit", "präsentiert", "presented by")) {
          Matcher m = Pattern.compile(token + ".*", Pattern.CASE_INSENSITIVE).matcher(fullText);
          if (m.find()) {
            String part = m.group().trim();
            if (!descParts.contains(part)) descParts.add(part);
          }
        }

        if (descParts.isEmpty()) {
          String preview = fullText.length() > 160 ? fullText.substring(0, 160) + "…" : fullText;
          logger.info("[Chemo] No headers found for '{}'; preview='{}'", title, preview);
        } else {
          logger.debug(
              "[Chemo] Headers for '{}': {}",
              title,
              descParts.stream().filter(s -> s.startsWith("<strong>")).toList());
        }

        StringBuilder description = new StringBuilder();
        for (String p : descParts) {
          description.append(p).append("\u003cbr /\u003e");
        }

        // Misc: include VVK-Stelle and any trailing info
        StringBuilder misc = new StringBuilder();
        if (vvkStelle != null) misc.append("VVK-Stelle: ").append(vvkStelle.trim());
        // Also include age/discount policy lines if present
        Matcher policy =
            Pattern.compile("All People.*", Pattern.CASE_INSENSITIVE).matcher(fullText);
        if (policy.find()) {
          if (!misc.isEmpty()) misc.append(" | ");
          misc.append(policy.group().trim());
        }

        // Parse thumbnail
        String thumbnail = event.select(".size-medium_large").attr("src");

        Event newEvent =
            new Event(
                null,
                title,
                location,
                date,
                description.toString().trim(),
                entryTime,
                startTime,
                priceText,
                misc.toString(),
                thumbnail);
        if (sourceUrl == null || sourceUrl.isBlank())
          sourceUrl = "https://www.chemiefabrik.info/gigs/";
        newEvent.setSourceUrl(sourceUrl);
        events.add(newEvent);
      }
    } catch (IOException | RuntimeException e) {
      e.printStackTrace();
    }

    return events;
  }

  // Normalization helper: collapse whitespace and lowercase
  private static String normalize(String s) {
    if (s == null) return "";
    return s.replaceAll("\\s+", " ").trim().toLowerCase();
  }

  // Case-insensitive contains check with normalization
  private static boolean containsCaseInsensitive(List<String> list, String value) {
    String nv = normalize(value);
    for (String it : list) {
      if (normalize(it).equals(nv)) return true;
    }
    return false;
  }

  // Detects simple date-like strings such as "Fr. 12.09.25" or "12.09.2025"
  private static boolean isDateLike(String s) {
    if (s == null) return false;
    String t = s.trim();
    String dayMonthYear = "^\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}$";
    String weekdayDayMonthYear = "^[A-Za-zÄÖÜäöü]{2,3}\\.?\\s*\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}$";
    return t.matches(dayMonthYear) || t.matches(weekdayDayMonthYear);
  }

  private static String findFirst(Pattern p, String text) {
    Matcher m = p.matcher(text);
    if (m.find()) return m.group(1);
    return null;
  }

  // Extracts the value after a label (e.g., "VVK:") up to the next known label or end of string
  private static String extractSegment(String text, String label) {
    if (text == null) return null;
    String pattern =
        Pattern.quote(label)
            + "\\s*(.*?)(?=\\s+(AK:|VVK:|Spendenempfehlung:|VVK-Stelle:|Einlass:|Beginn:)|$)";
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

  /** Extract a meaningful title from the event block. */
  private String extractTitle(Element event, String locationName) {
    // Strong candidates: dedicated title widgets
    Element anchor = event.selectFirst(".elementor-element-a0688f1 h4 a");
    if (anchor != null && !anchor.text().trim().isEmpty()) {
      return anchor.text().trim();
    }
    Element h4 = event.selectFirst(".elementor-element-a0688f1 h4");
    if (h4 != null && !h4.text().trim().isEmpty()) {
      return h4.text().trim();
    }
    // Fallback: first header that is not a meta label, not the location, not a date
    for (Element h : event.select("h1,h2,h3,h4,h5,h6")) {
      String t = h.text();
      String nt = normalize(t);
      if (t == null || nt.isBlank()) continue;
      if (isDateLike(t)) continue;
      if (nt.equals(normalize(locationName))) continue;
      if (nt.startsWith("ort:")
          || nt.startsWith("datum:")
          || nt.startsWith("einlass:")
          || nt.startsWith("beginn:")
          || nt.startsWith("preis:")) continue;
      return t.trim();
    }
    // Last resort: try first non-empty dynamic field that is not a label/date/location
    for (Element el : event.select(".jet-listing-dynamic-field__content")) {
      String t = el.text();
      String nt = normalize(t);
      if (t == null || nt.isBlank()) continue;
      if (t.contains(":")) continue; // avoid label-like lines
      if (isDateLike(t)) continue;
      if (nt.equals(normalize(locationName))) continue;
      return t.trim();
    }
    return null;
  }

  /** Helper method to parse time from the event element. */
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
