package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class UrgencyOverrideRequest {

    @SerializedName("priorityLevel")
    private final String priorityLevel;

    @SerializedName("reason")
    private final String reason;

    public UrgencyOverrideRequest(String priorityLevel, String reason) {
        this.priorityLevel = priorityLevel;
        this.reason = reason;
    }

    public String getPriorityLevel() { return priorityLevel; }
    public String getReason() { return reason; }
}
