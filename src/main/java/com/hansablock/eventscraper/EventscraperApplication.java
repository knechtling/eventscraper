package com.hansablock.eventscraper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EventscraperApplication {

  public static void main(String[] args) {
    SpringApplication.run(EventscraperApplication.class, args);
  }
}
