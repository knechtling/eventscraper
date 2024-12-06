package com.hansablock.eventscraper;

import com.hansablock.eventscraper.scraper.ScraperService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(ScraperService scraperService) {
        return args -> {
            scraperService.scrapeAndSaveEvents();
        };
    }
}