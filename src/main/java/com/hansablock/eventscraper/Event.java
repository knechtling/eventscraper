package com.hansablock.eventscraper;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
public class Event {
    @Id
    private Long id;

    private String title;
    private String location;
    private LocalDate date;
    private String genre;
    private LocalTime einlass;
    private LocalTime beginn;
    private float price;

    public Event(long id, String title, String location, LocalDate date, String genre, LocalTime einlass, LocalTime beginn, float price) {
        this.id = id;
        this.title = title;
        this.location = location;
    }

    public Event() {

    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getEinlass() {
        return einlass;
    }

    public void setEinlass(LocalTime einlass) {
        this.einlass = einlass;
    }

    public LocalTime getBeginn() {
        return beginn;
    }

    public void setBeginn(LocalTime beginn) {
        this.beginn = beginn;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }
}

