package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class TicketRequest {

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("categoryId")
    private Long categoryId;

    public TicketRequest(String title, String description, Long categoryId) {
        this.title = title;
        this.description = description;
        this.categoryId = categoryId;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Long getCategoryId() { return categoryId; }
}
