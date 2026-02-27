package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class StatusUpdateRequest {

    @SerializedName("status")
    private String status;

    public StatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }
}
