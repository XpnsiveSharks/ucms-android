package com.example.ucms_android.util;

import android.text.format.DateUtils;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

public class DateFormatter {
    private static final DateTimeFormatter PARSER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .optionalStart()
            .appendOffsetId()
            .optionalEnd()
            .toFormatter(Locale.US);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US);

    public static String formatRelativeTime(String isoString) {
        if (isoString == null) return "";
        try {
            Instant instant = parseToInstant(isoString);
            if (instant == null) return "";
            long now = System.currentTimeMillis();
            return DateUtils.getRelativeTimeSpanString(instant.toEpochMilli(), now, DateUtils.MINUTE_IN_MILLIS).toString();
        } catch (Exception e) {
            return isoString;
        }
    }

    public static String formatDate(String isoString) {
        if (isoString == null) return "";
        try {
            Instant instant = parseToInstant(isoString);
            if (instant == null) return "";
            return DATE_FORMATTER.withZone(ZoneOffset.UTC).format(instant);
        } catch (Exception e) {
            return isoString;
        }
    }

    private static Instant parseToInstant(String isoString) {
        TemporalAccessor parsed = PARSER.parseBest(
                isoString,
                Instant::from,
                OffsetDateTime::from,
                LocalDateTime::from
        );

        if (parsed instanceof Instant) {
            return (Instant) parsed;
        }
        if (parsed instanceof OffsetDateTime) {
            return ((OffsetDateTime) parsed).toInstant();
        }
        if (parsed instanceof LocalDateTime) {
            return ((LocalDateTime) parsed).toInstant(ZoneOffset.UTC);
        }

        throw new DateTimeParseException("Unsupported timestamp", isoString, 0);
    }
}
