package com.hansablock.eventscraper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest(properties = {"spring.task.scheduling.enabled=false"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class EventRepositoryFilterTest {

  @Autowired EventRepository eventRepository;

  @BeforeEach
  void seed() {
    eventRepository.deleteAll();
    // Today and around
    eventRepository.save(make("Punk Night", "Chemiefabrik", LocalDate.now()));
    eventRepository.save(make("Jazz Evening", "Hanse 3", LocalDate.now().plusDays(1)));
    eventRepository.save(make("Rock Gala", "Scheune Dresden", LocalDate.now().plusDays(10)));
    eventRepository.save(make("Indie Showcase", "Chemiefabrik", LocalDate.now().plusDays(20)));
  }

  private Event make(String title, String loc, LocalDate date) {
    Event e = new Event();
    e.setTitle(title);
    e.setLocation(loc);
    e.setDate(date);
    e.setEventHash(title + "|" + loc + "|" + date);
    return eventRepository.save(e);
  }

  @Test
  void filters_by_location_and_range_and_search() {
    LocalDate from = LocalDate.now();
    LocalDate to = LocalDate.now().plusDays(15);
    Page<Event> page =
        eventRepository.searchUpcomingWithFilters(
            "rock", from, to, "Scheune Dresden", PageRequest.of(0, 10));
    assertThat(page.getContent()).extracting(Event::getTitle).containsExactly("Rock Gala");
  }

  @Test
  void returns_from_today_when_to_is_null() {
    Page<Event> page =
        eventRepository.searchUpcomingWithFilters(
            null, LocalDate.now(), null, null, PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(4);
  }
}
