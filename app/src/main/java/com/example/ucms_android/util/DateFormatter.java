package com.example.ucms_android.util;

import android.text.format.DateUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateFormatter {
    private static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static String formatRelativeTime(String isoString) {
        if (isoString == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT, Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date date = sdf.parse(isoString);
            if (date == null) return "";
            long now = System.currentTimeMillis();
            return DateUtils.getRelativeTimeSpanString(date.getTime(), now, DateUtils.MINUTE_IN_MILLIS).toString();
        } catch (ParseException e) {
            return isoString;
        }
    }

    public static String formatDate(String isoString) {
        if (isoString == null) return "";
        SimpleDateFormat isoSdf = new SimpleDateFormat(ISO_FORMAT, Locale.US);
        isoSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat targetSdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
        try {
            Date date = isoSdf.parse(isoString);
            return date != null ? targetSdf.format(date) : "";
        } catch (ParseException e) {
            return isoString;
        }
    }
}
