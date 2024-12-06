package com.hansablock.eventscraper;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@Entity
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String location;
    private LocalDate date;
    private String genre;
    private LocalTime einlass;
    private LocalTime beginn;
    private BigDecimal price;

    public Event() {
    }

    public Event(Long id, String title, String location, LocalDate date, String genre, LocalTime einlass, LocalTime beginn, BigDecimal price) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.date = date;
        this.genre = genre;
        this.einlass = einlass;
        this.beginn = beginn;
        this.price = price;
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

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id) &&
                Objects.equals(title, event.title) &&
                Objects.equals(location, event.location) &&
                Objects.equals(date, event.date) &&
                Objects.equals(genre, event.genre) &&
                Objects.equals(einlass, event.einlass) &&
                Objects.equals(beginn, event.beginn) &&
                Objects.equals(price, event.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, location, date, genre, einlass, beginn, price);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", location='" + location + '\'' +
                ", date=" + date +
                ", genre='" + genre + '\'' +
                ", einlass=" + einlass +
                ", beginn=" + beginn +
                ", price=" + price +
                '}';
    }
}