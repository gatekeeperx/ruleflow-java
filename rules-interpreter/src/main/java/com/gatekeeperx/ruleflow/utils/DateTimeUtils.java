package com.gatekeeperx.ruleflow.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class DateTimeUtils {
    private static final DateTimeFormatter[] SUPPORTED_FORMATS = new DateTimeFormatter[] {
        DateTimeFormatter.ISO_DATE_TIME, // '2024-06-01T12:30:00Z' or with offset
        DateTimeFormatter.ISO_DATE,      // '2024-06-01'
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"), // with offset
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmXXX")     // with offset, no seconds
    };

    public static Object parseDateTime(String value) {
        for (DateTimeFormatter fmt : SUPPORTED_FORMATS) {
            try {
                return fmt.parseBest(value, ZonedDateTime::from, LocalDateTime::from, LocalDate::from);
            } catch (DateTimeParseException ignored) {}
        }
        throw new IllegalArgumentException("Invalid date/datetime: " + value);
    }

    public static ZonedDateTime toZonedDateTime(Object value) {
        if (value instanceof ZonedDateTime) return (ZonedDateTime) value;
        if (value instanceof LocalDateTime) return ((LocalDateTime) value).atZone(ZoneId.systemDefault());
        if (value instanceof LocalDate) return ((LocalDate) value).atStartOfDay(ZoneId.systemDefault());
        if (value instanceof String) {
            Object parsed = parseDateTime((String) value);
            if (parsed instanceof ZonedDateTime) return (ZonedDateTime) parsed;
            if (parsed instanceof LocalDateTime) return ((LocalDateTime) parsed).atZone(ZoneId.systemDefault());
            if (parsed instanceof LocalDate) return ((LocalDate) parsed).atStartOfDay(ZoneId.systemDefault());
            throw new IllegalArgumentException("Cannot parse date/datetime: " + value);
        }
        throw new IllegalArgumentException("Cannot convert to ZonedDateTime: " + value);
    }

    public static ZonedDateTime truncate(ZonedDateTime zdt, String unit) {
        switch (unit.toLowerCase()) {
            case "day":
                return zdt.truncatedTo(ChronoUnit.DAYS);
            case "hour":
                return zdt.truncatedTo(ChronoUnit.HOURS);
            case "minute":
                return zdt.truncatedTo(ChronoUnit.MINUTES);
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
    }

    public static String format(ZonedDateTime zdt) {
        return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
} 