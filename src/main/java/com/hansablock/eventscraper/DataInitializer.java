package com.hansablock.eventscraper;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(EventRepository eventRepository) {
        return args -> {
            eventRepository.save(new Event(null, "Event 1", "Location 1", LocalDate.of(2024, 1, 1), "Heavy Metal", LocalTime.of(19, 0), LocalTime.of(20, 0), new BigDecimal("10.00")));
            eventRepository.save(new Event(null, "Event 2", "Location 2", LocalDate.of(2025, 1, 2), "Heavy Blouse", LocalTime.of(19, 0), LocalTime.of(20, 0), new BigDecimal("10.00")));
            eventRepository.save(new Event(null, "Event 3", "Location 3", LocalDate.of(2026, 1, 3), "Rock", LocalTime.of(19, 0), LocalTime.of(20, 0), new BigDecimal("10.00")));
            eventRepository.save(new Event(null, "Event 4", "Location 4", LocalDate.of(2027, 1, 4), "Iii Pop", LocalTime.of(19, 0), LocalTime.of(20, 0), new BigDecimal("10.00")));
        };
    }
}
