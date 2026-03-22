package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class Ticket {

    @SerializedName("id")
    private Long id;

    @SerializedName("ticketNumber")
    private String ticketNumber;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("status")
    private String status;

    @SerializedName("confirmedResolved")
    private boolean confirmedResolved;

    @SerializedName("categoryId")
    private Long categoryId;

    @SerializedName("categoryName")
    private String categoryName;

    @SerializedName("studentName")
    private String studentName;

    @SerializedName("studentId")
    private String studentId;

    @SerializedName("studentCourse")
    private String studentCourse;

    @SerializedName("hasAdminResponse")
    private boolean hasAdminResponse;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public Long getId() { return id; }
    public String getTicketNumber() { return ticketNumber; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public boolean isConfirmedResolved() { return confirmedResolved; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getStudentName() { return studentName; }
    public String getStudentId() { return studentId; }
    public String getStudentCourse() { return studentCourse; }
    public boolean hasAdminResponse() { return hasAdminResponse; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    // Setters kept for legacy use in StudentHomeFragment dummy data removal
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
    public void setTitle(String title) { this.title = title; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
