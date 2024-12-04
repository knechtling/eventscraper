package com.hansablock.eventscraper;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Event {
    @Id
    private Long id;

    private String title;









    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
