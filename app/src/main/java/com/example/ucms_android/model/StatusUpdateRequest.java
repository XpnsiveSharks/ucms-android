package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class StatusUpdateRequest {

    @SerializedName("status")
    private String status;

    @SerializedName("comment")
    private String comment;

    public StatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
