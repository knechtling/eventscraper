package com.hansablock.eventscraper;

import org.apache.commons.codec.digest.DigestUtils;

import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.StringJoiner;

public class EventHasher {
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    public static String generateHash(Event event) {
        StringJoiner components = new StringJoiner("|");

        // Use most stable/unique fields
        addComponent(components, event.getDate(), DATE_FORMAT);
        addComponent(components, event.getLocation());
        addComponent(components, event.getBeginn(), TIME_FORMAT);
        addComponent(components, event.getMisc());

        return DigestUtils.sha256Hex(components.toString());
    }

    private static void addComponent(StringJoiner sj, Object value) {
        if (value != null) sj.add(value.toString().trim().toLowerCase());
    }

    private static void addComponent(StringJoiner sj, TemporalAccessor time, DateTimeFormatter formatter) {
        if (time != null) sj.add(formatter.format(time));
    }
}