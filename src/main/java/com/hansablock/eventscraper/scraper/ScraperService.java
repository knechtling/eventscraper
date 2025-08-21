package com.hansablock.eventscraper.scraper;

import com.hansablock.eventscraper.Event;
import com.hansablock.eventscraper.EventHasher;
import com.hansablock.eventscraper.EventRepository;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ScraperService {

    private final EventRepository eventRepository;
    private final List<Scraper> scrapers;

    private static final int MAX_TITLE = 255;
    private static final int MAX_PRICE = 255;
    private static final int MAX_THUMB = 255;
    private static final int MAX_LOCATION = 255;
    private static final int MAX_DESC = 2000;
    private static final int MAX_MISC = 4000;

    private static final Map<String, String> LOCATION_FALLBACK = Map.of(
            "Chemiefabrik", "https://www.chemiefabrik.info/gigs/",
            "Hanse3", "https://hanse3.de/Veranstaltungen/",
            "Scheune Dresden", "https://scheune.org",
            "Tante Ju", "https://www.liveclub-dresden.de/events/"
    );

    @Autowired
    public ScraperService(EventRepository eventRepository, List<Scraper> scrapers) {
        this.eventRepository = eventRepository;
        this.scrapers = scrapers;
    }

    @Scheduled(fixedRate = 3600000)
    public void scrapeAndSaveEvents() {
        for (Scraper scraper : scrapers) {
            List<Event> scraped = scraper.scrapeEvents();
            // Normalize, drop invalid/past, hash
            List<Event> normalized = scraped.stream()
                    .filter(e -> e.getDate() != null && !e.getDate().isBefore(LocalDate.now()))
                    .peek(this::normalize)
                    .peek(e -> e.setEventHash(EventHasher.generateHash(e)))
                    .toList();

            // In-batch de-duplication by eventHash: keep the "best" instance
            java.util.Map<String, Event> byHash = new java.util.LinkedHashMap<>();
            for (Event e : normalized) {
                Event existing = byHash.get(e.getEventHash());
                if (existing == null || isBetter(e, existing)) {
                    byHash.put(e.getEventHash(), e);
                }
            }
            List<Event> events = new java.util.ArrayList<>(byHash.values());

            saveEventsSmart(events);
            System.out.println("Scraped " + events.size() + " events from " + scraper.getClass().getSimpleName());
        }
    }

    private void normalize(Event e) {
        // Trim simple strings
        if (e.getTitle() != null) e.setTitle(e.getTitle().trim());
        if (e.getLocation() != null) e.setLocation(e.getLocation().trim());
        if (e.getPrice() != null) e.setPrice(e.getPrice().trim());
        if (e.getDescription() != null) e.setDescription(e.getDescription().trim());
        if (e.getMisc() != null) e.setMisc(e.getMisc().trim());
        if (e.getThumbnail() != null) e.setThumbnail(e.getThumbnail().trim());
        if (e.getSourceUrl() != null) e.setSourceUrl(e.getSourceUrl().trim());

        // Sanitize HTML-bearing fields and cap lengths
        e.setDescription(cap(cleanHtml(e.getDescription()), MAX_DESC));
        e.setMisc(cap(cleanHtml(e.getMisc()), MAX_MISC));
        e.setTitle(cap(e.getTitle(), MAX_TITLE));
        e.setPrice(cap(normalizePrice(e.getPrice()), MAX_PRICE));
        e.setThumbnail(cap(e.getThumbnail(), MAX_THUMB));
        e.setLocation(cap(e.getLocation(), MAX_LOCATION));

        // Ensure source URL fallback based on location if missing
        if (!StringUtils.hasText(e.getSourceUrl())) {
            String loc = e.getLocation();
            if (StringUtils.hasText(loc)) {
                String fb = LOCATION_FALLBACK.get(loc);
                if (fb != null) e.setSourceUrl(fb);
            }
        }
    }

    private static String cap(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String cleanHtml(String html) {
        if (!StringUtils.hasText(html)) return html;
        // allow basic inline tags and links; preserve <br>
        Safelist safelist = Safelist.basic();
        safelist.addTags("br");
        return Jsoup.clean(html, safelist);
    }

    private static String normalizePrice(String price) {
        if (!StringUtils.hasText(price)) return price;
        String p = price.trim();
        // Split candidate segments by pipe or newline and keep concise parts that look like prices
        String[] parts = p.split("[\n|]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            boolean looksLikePrice = t.matches(".*(\\d|€|Euro|frei|ermäßigt|zzgl).*");
            if (!looksLikePrice) continue;
            if (t.length() > 120) t = t.substring(0, 120);
            if (!sb.isEmpty()) sb.append(" | ");
            sb.append(t);
        }
        String res = sb.toString();
        return res.isEmpty() ? null : res;
    }

    // Prefer entries with more concrete data (beginn/einlass times, thumbnail, numeric price, longer description)
    private boolean isBetter(Event candidate, Event current) {
        int scoreC = score(candidate);
        int scoreX = score(current);
        if (scoreC != scoreX) return scoreC > scoreX;
        // tie-breaker: longer description
        int lenC = candidate.getDescription() == null ? 0 : candidate.getDescription().length();
        int lenX = current.getDescription() == null ? 0 : current.getDescription().length();
        return lenC > lenX;
    }

    private int score(Event e) {
        int s = 0;
        if (e.getBeginn() != null) s += 2;
        if (e.getEinlass() != null) s += 1;
        if (StringUtils.hasText(e.getThumbnail())) s += 1;
        if (StringUtils.hasText(e.getPrice()) && java.util.regex.Pattern.compile("\\d").matcher(e.getPrice()).find()) s += 1;
        return s;
    }

    private void saveEventsSmart(List<Event> newEvents) {
        newEvents.forEach(event -> {
            eventRepository.findByEventHash(event.getEventHash())
                    .ifPresentOrElse(
                            existing -> updateExisting(existing, event),
                            () -> eventRepository.save(event)
                    );
        });
    }

    private void updateExisting(Event existing, Event newVersion) {
        // Price: update if new has digits and (old empty or old lacks digits)
        if (StringUtils.hasText(newVersion.getPrice())) {
            boolean newHasDigits = newVersion.getPrice().matches(".*\\d.*");
            boolean oldHasDigits = StringUtils.hasText(existing.getPrice()) && existing.getPrice().matches(".*\\d.*");
            if (newHasDigits && (!oldHasDigits || !StringUtils.hasText(existing.getPrice()))) {
                existing.setPrice(newVersion.getPrice());
            }
        }
        // Description: update only if clearly better (longer) or old is blank
        if (StringUtils.hasText(newVersion.getDescription())) {
            int newLen = newVersion.getDescription().length();
            int oldLen = existing.getDescription() == null ? 0 : existing.getDescription().length();
            if (oldLen == 0 || newLen > oldLen + 20) { // 20-char threshold to avoid oscillation
                existing.setDescription(newVersion.getDescription());
            }
        }
        // Thumbnail: fill if missing
        if (StringUtils.hasText(newVersion.getThumbnail()) && !StringUtils.hasText(existing.getThumbnail())) {
            existing.setThumbnail(newVersion.getThumbnail());
        }
        // Times: fill if missing
        if (existing.getEinlass() == null && newVersion.getEinlass() != null) {
            existing.setEinlass(newVersion.getEinlass());
        }
        if (existing.getBeginn() == null && newVersion.getBeginn() != null) {
            existing.setBeginn(newVersion.getBeginn());
        }

        eventRepository.save(existing);
    }
}
