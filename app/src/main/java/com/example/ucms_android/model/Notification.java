package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class Notification {

    @SerializedName("id")
    private Long id;

    @SerializedName("ticketId")
    private Long ticketId;

    @SerializedName("message")
    private String message;

    @SerializedName("isRead")
    private boolean isRead;

    @SerializedName("createdAt")
    private String createdAt;

    public Long getId() { return id; }
    public Long getTicketId() { return ticketId; }
    public String getMessage() { return message; }
    public boolean isRead() { return isRead; }
    public String getCreatedAt() { return createdAt; }
}
