package com.example.ucms_android.model;

public class SyncPayload<T> {
    private T items;
    private boolean fullRefresh;
    private String serverTime;
    private String nextSince;

    public T getItems() {
        return items;
    }

    public boolean isFullRefresh() {
        return fullRefresh;
    }

    public String getServerTime() {
        return serverTime;
    }

    public String getNextSince() {
        return nextSince;
    }
}
