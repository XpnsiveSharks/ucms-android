package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class AnalyticsSummary {

    @SerializedName("totalTickets")
    private long totalTickets;

    @SerializedName("resolvedCount")
    private long resolvedCount;

    @SerializedName("resolvedPercentage")
    private double resolvedPercentage;

    @SerializedName("unresolvedCount")
    private long unresolvedCount;

    public long getTotalTickets() { return totalTickets; }
    public long getResolvedCount() { return resolvedCount; }
    public double getResolvedPercentage() { return resolvedPercentage; }
    public long getUnresolvedCount() { return unresolvedCount; }
}
