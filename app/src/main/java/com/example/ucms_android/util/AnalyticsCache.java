package com.example.ucms_android.util;

import com.example.ucms_android.model.CategoryCount;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.model.auth.AnalyticsSummary;

import java.util.List;

public class AnalyticsCache {

    private static AnalyticsCache instance;

    private AnalyticsSummary summary;
    private List<CategoryCount> categoryCounts;
    private List<Ticket> unresolvedTickets;
    private boolean hasData = false;

    private AnalyticsCache() {}

    public static synchronized AnalyticsCache getInstance() {
        if (instance == null) {
            instance = new AnalyticsCache();
        }
        return instance;
    }

    public boolean hasData() {
        return hasData;
    }

    public void store(AnalyticsSummary summary, List<CategoryCount> categoryCounts, List<Ticket> unresolvedTickets) {
        this.summary = summary;
        this.categoryCounts = categoryCounts;
        this.unresolvedTickets = unresolvedTickets;
        this.hasData = true;
    }

    public AnalyticsSummary getSummary() {
        return summary;
    }

    public List<CategoryCount> getCategoryCounts() {
        return categoryCounts;
    }

    public List<Ticket> getUnresolvedTickets() {
        return unresolvedTickets;
    }
}
