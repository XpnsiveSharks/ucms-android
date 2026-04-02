package com.example.ucms_android.model;

public class RealtimeEvent {
    private String domain;
    private String eventType;
    private String entityId;
    private String updatedAt;
    private String actorRole;

    public String getDomain() {
        return domain;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public String getActorRole() {
        return actorRole;
    }
}
