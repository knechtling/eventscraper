package com.hansablock.eventscraper;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = "spring.profiles.active=test")
class EventRepositoryUpcomingTest {

    @Autowired
    private EventRepository repo;

    @Test
    void upcomingQueries_respectTodayBoundary() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate tomorrow = today.plusDays(1);

        Event past = new Event(null, "Past", "X", yesterday, "", null, null, "", "", "");
        past.setEventHash("past");
        Event todayE = new Event(null, "Today", "X", today, "", null, null, "", "", "");
        todayE.setEventHash("today");
        Event future = new Event(null, "Future", "X", tomorrow, "", null, null, "", "", "");
        future.setEventHash("future");
        repo.saveAll(List.of(past, todayE, future));

        List<Event> upcoming = repo.findAllByDateGreaterThanEqualOrderByDateAsc(today);
        assertEquals(2, upcoming.size());
        assertEquals("Today", upcoming.get(0).getTitle());
        assertEquals("Future", upcoming.get(1).getTitle());

        List<Event> searched = repo.searchUpcoming("tod", today, PageRequest.of(0, 10)).getContent();
        assertEquals(1, searched.size());
        assertEquals("Today", searched.get(0).getTitle());
    }
}

