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

    @SerializedName("category")
    private String category;

    @SerializedName("studentId")
    private String studentId;

    @SerializedName("studentName")
    private String studentName;

    @SerializedName("courseYear")
    private String courseYear;

    @SerializedName("attachmentUrl")
    private String attachmentUrl;

    @SerializedName("attachmentName")
    private String attachmentName;

    @SerializedName("confirmedResolved")
    private boolean confirmedResolved;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public Long getId() { return id; }
    public String getTicketNumber() { return ticketNumber; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public String getCategory() { return category; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getCourseYear() { return courseYear; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public String getAttachmentName() { return attachmentName; }
    public boolean isConfirmedResolved() { return confirmedResolved; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }
}
