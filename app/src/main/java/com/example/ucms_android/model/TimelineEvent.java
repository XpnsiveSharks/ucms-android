package com.example.ucms_android.model;

public class TimelineEvent {
    private String date;
    private String title;
    private String adminResponse;
    private boolean isCompleted;

    public TimelineEvent(String date, String title, String adminResponse, boolean isCompleted) {
        this.date = date;
        this.title = title;
        this.adminResponse = adminResponse;
        this.isCompleted = isCompleted;
    }

    public String getDate() { return date; }
    public String getTitle() { return title; }
    public String getAdminResponse() { return adminResponse; }
    public boolean isCompleted() { return isCompleted; }
}
