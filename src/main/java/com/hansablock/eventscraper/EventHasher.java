package com.hansablock.eventscraper;

import org.apache.commons.codec.digest.DigestUtils;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.StringJoiner;

public class EventHasher {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String generateHash(Event event) {
        StringJoiner components = new StringJoiner("|");

        // Stable key: normalized title + date + location (exclude volatile fields like misc)
        addComponent(components, event.getTitle());
        addComponent(components, event.getDate(), DATE_FORMAT);
        addComponent(components, event.getLocation());

        return DigestUtils.sha256Hex(components.toString());
    }

    static void addComponent(StringJoiner sj, Object value) {
        if (value != null) sj.add(value.toString().trim().toLowerCase());
    }

    static void addComponent(StringJoiner sj, java.time.temporal.TemporalAccessor time, DateTimeFormatter formatter) {
        if (time != null) sj.add(formatter.format(time));
    }
}
