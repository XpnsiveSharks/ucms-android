package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class DailyTicketVolume {
    @SerializedName("day")
    private String day;

    @SerializedName("ticketCount")
    private long ticketCount;

    public DailyTicketVolume(String day, long count) {
        this.day = day;
        this.ticketCount = count;
    }

    public String getDay() { return day; }
    public long getTicketCount() { return ticketCount; }
}
