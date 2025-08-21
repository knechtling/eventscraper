package com.hansablock.eventscraper;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
public class ScrapeRun {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String scraperName;
  private Instant startedAt;
  private Instant finishedAt;
  private int added;
  private int updated;
  private int errors;

  @Column(length = 1000)
  private String message;

  public ScrapeRun() {}

  public ScrapeRun(String scraperName, Instant startedAt) {
    this.scraperName = scraperName;
    this.startedAt = startedAt;
  }

  public Long getId() {
    return id;
  }

  public String getScraperName() {
    return scraperName;
  }

  public void setScraperName(String scraperName) {
    this.scraperName = scraperName;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(Instant startedAt) {
    this.startedAt = startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }

  public void setFinishedAt(Instant finishedAt) {
    this.finishedAt = finishedAt;
  }

  public int getAdded() {
    return added;
  }

  public void setAdded(int added) {
    this.added = added;
  }

  public int getUpdated() {
    return updated;
  }

  public void setUpdated(int updated) {
    this.updated = updated;
  }

  public int getErrors() {
    return errors;
  }

  public void setErrors(int errors) {
    this.errors = errors;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
