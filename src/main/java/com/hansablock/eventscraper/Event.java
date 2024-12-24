package com.hansablock.eventscraper;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Entity
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String location;
    private LocalDate date;
    @Column(length = 2000)
    private String description;
    private LocalTime einlass;
    private LocalTime beginn;
    private String price;
    @Column(length = 4000)
    private String misc;

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    private String thumbnail;

    public Event() {
    }

    public Event(Long id, String title, String location, LocalDate date, String description, LocalTime einlass,
            LocalTime beginn, String price, String misc, String thumbnail) {
        this.id = id;
        this.title = title;
        this.location = location;
        this.date = date;
        this.description = description;
        this.einlass = einlass;
        this.beginn = beginn;
        this.price = price;
        this.misc = misc;
        this.thumbnail = thumbnail;
    }

    public String getFormattedDate() {
        if (this.date != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            return this.date.format(formatter);
        }
        return "";
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getMisc() {
        return misc;
    }

    public void setMisc(String misc) {
        this.misc = misc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id) &&
                Objects.equals(title, event.title) &&
                Objects.equals(location, event.location) &&
                Objects.equals(date, event.date) &&
                Objects.equals(description, event.description) &&
                Objects.equals(einlass, event.einlass) &&
                Objects.equals(beginn, event.beginn) &&
                Objects.equals(price, event.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, location, date, description, einlass, beginn, price);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", location='" + location + '\'' +
                ", date=" + date +
                ", description='" + description + '\'' +
                ", einlass=" + einlass +
                ", beginn=" + beginn +
                ", price=" + price +
                '}';
    }
}
