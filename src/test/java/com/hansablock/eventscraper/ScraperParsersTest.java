package com.hansablock.eventscraper;

import static org.junit.jupiter.api.Assertions.*;

import com.hansablock.eventscraper.scraper.ChemoScraper;
import com.hansablock.eventscraper.scraper.ScheunenScraper;
import com.hansablock.eventscraper.scraper.TanteJuScraper;
import java.time.LocalTime;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class ScraperParsersTest {

  @Test
  void chemo_parseTime_handlesVariousFormats() {
    ChemoScraper chemo = new ChemoScraper();
    Element root =
        Jsoup.parse(
                "<div><div class='elementor-element-6c7315b'><div class='elementor-widget-container'>Einlass: 19.30</div></div></div>")
            .body();
    LocalTime t =
        chemo.parseTime(root, ".elementor-element-6c7315b .elementor-widget-container", "Einlass:");
    assertEquals(LocalTime.of(19, 30), t);

    root =
        Jsoup.parse(
                "<div><div class='elementor-element-c75cd6a'><div class='elementor-widget-container'>Beginn: 20:00</div></div></div>")
            .body();
    t = chemo.parseTime(root, ".elementor-element-c75cd6a .elementor-widget-container", "Beginn:");
    assertEquals(LocalTime.of(20, 0), t);
  }

  @Test
  void scheune_getMonthFromGerman_mapsCorrectly() {
    assertEquals(java.time.Month.MARCH, ScheunenScraper.getMonthFromGerman("März"));
    assertEquals(java.time.Month.AUGUST, ScheunenScraper.getMonthFromGerman("August"));
  }

  @Test
  void tanteJu_extractTime_parsesPattern() {
    LocalTime einlass = TanteJuScraper.extractTime("Einlass: 19:00 Uhr", "Einlass:");
    LocalTime beginn = TanteJuScraper.extractTime("foo Beginn: 20:30 Uhr bar", "Beginn:");
    assertEquals(LocalTime.of(19, 0), einlass);
    assertEquals(LocalTime.of(20, 30), beginn);
  }

  @Test
  void tanteJu_extractThumbnailUrl_handlesFallbacks() {
    TanteJuScraper t = new TanteJuScraper();
    Document d1 = Jsoup.parse("<div class='news'><img src='a.jpg'></div>");
    assertEquals("a.jpg", t.extractThumbnailUrl(d1));
    Document d2 = Jsoup.parse("<img class='soliloquy-preload' src='b.png'>");
    assertEquals("b.png", t.extractThumbnailUrl(d2));
    Document d3 = Jsoup.parse("<div>No image</div>");
    assertEquals("", t.extractThumbnailUrl(d3));
  }
}
