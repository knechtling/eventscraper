package com.hansablock.eventscraper;

import static org.junit.jupiter.api.Assertions.*;

import com.hansablock.eventscraper.scraper.HanseScraper;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

public class HanseScraperDateParseTest {

  @Test
  void parsesMultipleDotDates() throws Exception {
    HanseScraper s = new HanseScraper();
    Method m = HanseScraper.class.getDeclaredMethod("parseDates", String.class);
    m.setAccessible(true);
    @SuppressWarnings("unchecked")
    List<LocalDate> dates = (List<LocalDate>) m.invoke(s, "21.08.2025, 22.08.2025 und 23.08.2025");
    assertEquals(3, dates.size());
    assertEquals(LocalDate.of(2025, 8, 21), dates.get(0));
  }

  @Test
  void parsesSlashDatesAndTwoDigitYear() throws Exception {
    HanseScraper s = new HanseScraper();
    Method m = HanseScraper.class.getDeclaredMethod("parseDates", String.class);
    m.setAccessible(true);
    @SuppressWarnings("unchecked")
    List<LocalDate> dates = (List<LocalDate>) m.invoke(s, "05/09/25 06/09/2025");
    assertEquals(2, dates.size());
    assertEquals(LocalDate.of(2025, 9, 5), dates.get(0));
  }
}
