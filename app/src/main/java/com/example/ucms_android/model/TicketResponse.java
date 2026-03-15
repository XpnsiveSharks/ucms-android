package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class TicketResponse {

    @SerializedName("id")
    private Long id;

    @SerializedName("message")
    private String message;

    @SerializedName("adminName")
    private String adminName;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("ticketStatus")
    private String ticketStatus;

    public Long getId() { return id; }
    public String getMessage() { return message; }
    public String getAdminName() { return adminName; }
    public String getCreatedAt() { return createdAt; }
    public String getTicketStatus() { return ticketStatus; }
}
