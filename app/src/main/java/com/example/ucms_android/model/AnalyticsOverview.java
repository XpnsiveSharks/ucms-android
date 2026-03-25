package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AnalyticsOverview {
    @SerializedName("resolutionRate")
    private double resolutionRate;

    @SerializedName("resolutionTrend")
    private double resolutionTrend;

    @SerializedName("averageWaitTimeHours")
    private double averageWaitTimeHours;

    @SerializedName("averageWaitTimeTrendHours")
    private double averageWaitTimeTrendHours;

    @SerializedName("totalTickets")
    private long totalTickets;

    @SerializedName("resolvedCount")
    private long resolvedCount;

    @SerializedName("pendingCount")
    private long pendingCount;

    @SerializedName("inProgressCount")
    private long inProgressCount;

    @SerializedName("unresolvedCount")
    private long unresolvedCount;

    @SerializedName("ticketVolumeLast7Days")
    private List<DailyTicketVolume> ticketVolumeLast7Days;

    @SerializedName("categoryBreakdown")
    private List<CategoryCount> categoryBreakdown;

    public double getResolutionRate() { return resolutionRate; }
    public double getResolutionTrend() { return resolutionTrend; }
    public double getAverageWaitTimeHours() { return averageWaitTimeHours; }
    public double getAverageWaitTimeTrendHours() { return averageWaitTimeTrendHours; }
    public long getTotalTickets() { return totalTickets; }
    public long getResolvedCount() { return resolvedCount; }
    public long getPendingCount() { return pendingCount; }
    public long getInProgressCount() { return inProgressCount; }
    public long getUnresolvedCount() { return unresolvedCount; }
    public List<DailyTicketVolume> getTicketVolumeLast7Days() { return ticketVolumeLast7Days; }
    public List<CategoryCount> getCategoryBreakdown() { return categoryBreakdown; }
}
