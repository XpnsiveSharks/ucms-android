package com.example.ucms_android.sync;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.ucms_android.model.AnalyticsOverview;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.Notification;
import com.example.ucms_android.model.SyncPayload;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.model.User;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.SyncService;
import com.example.ucms_android.session.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BootstrapCoordinator {

    public interface CallbackResult {
        void onSuccess();
        void onFailure(String message);
    }

    private static final String ROLE_ADMIN = "ADMIN";

    private final SessionManager sessionManager;
    private final Gson gson;
    private final SyncService syncService;

    public BootstrapCoordinator(Context context) {
        Context appContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(appContext);
        this.gson = new Gson();
        this.syncService = ApiClient.getInstance(appContext).create(SyncService.class);
    }

    public boolean hasWarmCache(String role) {
        return sessionManager.hasBootstrapCache(role);
    }

    public void runBootstrap(CallbackResult callbackResult) {
        fetchProfile(new StepCallback() {
            @Override
            public void onSuccess() {
                fetchTickets(new StepCallback() {
                    @Override
                    public void onSuccess() {
                        fetchNotifications(new StepCallback() {
                            @Override
                            public void onSuccess() {
                                maybeFetchAnalytics(callbackResult);
                            }

                            @Override
                            public void onFailure(String message) {
                                callbackResult.onFailure(message);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String message) {
                        callbackResult.onFailure(message);
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                callbackResult.onFailure(message);
            }
        });
    }

    public void runBackgroundSync() {
        runBootstrap(new CallbackResult() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure(String message) {
            }
        });
    }

    private void maybeFetchAnalytics(CallbackResult callbackResult) {
        if (!ROLE_ADMIN.equalsIgnoreCase(sessionManager.getRole())) {
            callbackResult.onSuccess();
            return;
        }

        String since = getSinceParam(SessionManager.DOMAIN_ANALYTICS);
        syncService.syncAnalytics(since).enqueue(new Callback<ApiResponse<SyncPayload<AnalyticsOverview>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SyncPayload<AnalyticsOverview>>> call,
                                   @NonNull Response<ApiResponse<SyncPayload<AnalyticsOverview>>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null
                        || response.body().getData().getItems() == null) {
                    callbackResult.onFailure("Failed to sync analytics.");
                    return;
                }

                sessionManager.saveAnalyticsSummaryJson(gson.toJson(response.body().getData().getItems()));
                sessionManager.markDomainSynced(SessionManager.DOMAIN_ANALYTICS);
                SyncUpdateBus.getInstance().publish(SyncUpdateBus.DOMAIN_ANALYTICS);
                callbackResult.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SyncPayload<AnalyticsOverview>>> call, @NonNull Throwable t) {
                callbackResult.onFailure("Failed to sync analytics.");
            }
        });
    }

    private void fetchProfile(StepCallback callback) {
        String since = getSinceParam(SessionManager.DOMAIN_PROFILE);
        syncService.syncProfile(since).enqueue(new Callback<ApiResponse<SyncPayload<User>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SyncPayload<User>>> call,
                                   @NonNull Response<ApiResponse<SyncPayload<User>>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null
                        || response.body().getData().getItems() == null) {
                    callback.onFailure("Failed to load user profile.");
                    return;
                }

                User user = response.body().getData().getItems();
                sessionManager.saveProfileCache(user.getName(), user.getStudentId(), user.getCourse(), user.getYearLevel());

                String userId = user.getAuthUserId() != null ? user.getAuthUserId() : user.getStudentId();
                if (userId != null && !userId.trim().isEmpty()) {
                    sessionManager.setCurrentUserId(userId);
                }

                sessionManager.markDomainSynced(SessionManager.DOMAIN_PROFILE);
                SyncUpdateBus.getInstance().publish(SyncUpdateBus.DOMAIN_PROFILE);
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SyncPayload<User>>> call, @NonNull Throwable t) {
                callback.onFailure("Failed to load user profile.");
            }
        });
    }

    private void fetchTickets(StepCallback callback) {
        String since = getSinceParam(SessionManager.DOMAIN_TICKETS);
        syncService.syncTickets(since).enqueue(new Callback<ApiResponse<SyncPayload<List<Ticket>>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SyncPayload<List<Ticket>>>> call,
                                   @NonNull Response<ApiResponse<SyncPayload<List<Ticket>>>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null
                        || response.body().getData().getItems() == null) {
                    callback.onFailure("Failed to sync tickets.");
                    return;
                }

                List<Ticket> tickets = response.body().getData().getItems();
                boolean isFullRefresh = response.body().getData().isFullRefresh();
                String role = sessionManager.getRole();
                boolean isFullSync = since == null;

                String existingJson = ROLE_ADMIN.equalsIgnoreCase(role)
                        ? sessionManager.getAdminAllTicketsJson()
                        : sessionManager.getStudentAllTicketsJson();
                List<Ticket> tickets = isFullSync ? delta : mergeTickets(existingJson, delta);

                if (ROLE_ADMIN.equalsIgnoreCase(role)) {
                    cacheAdminTicketData(tickets, isFullRefresh);
                } else {
                    cacheStudentTicketData(tickets, isFullRefresh);
                }

                String previousSignature = sessionManager.getTicketsSignature();
                // We should build the signature from the FULL list if it's available
                List<Ticket> fullList;
                if (ROLE_ADMIN.equalsIgnoreCase(role)) {
                    fullList = getAdminFullList();
                } else {
                    fullList = getStudentFullList();
                }
                String signature = buildTicketSignature(fullList != null ? fullList : tickets);
                sessionManager.saveTicketsSignature(signature);
                sessionManager.markDomainSynced(SessionManager.DOMAIN_TICKETS);
                if (!signature.equals(previousSignature)) {
                    SyncUpdateBus.getInstance().publish(SyncUpdateBus.DOMAIN_TICKETS);
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SyncPayload<List<Ticket>>>> call, @NonNull Throwable t) {
                callback.onFailure("Failed to sync tickets.");
            }
        });
    }

    private void fetchNotifications(StepCallback callback) {
        if (ROLE_ADMIN.equalsIgnoreCase(sessionManager.getRole())) {
            callback.onSuccess();
            return;
        }

        String since = getSinceParam(SessionManager.DOMAIN_NOTIFICATIONS);
        syncService.syncNotifications(since).enqueue(new Callback<ApiResponse<SyncPayload<List<Notification>>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<SyncPayload<List<Notification>>>> call,
                                   @NonNull Response<ApiResponse<SyncPayload<List<Notification>>>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null
                        || response.body().getData().getItems() == null) {
                    callback.onFailure("Failed to sync notifications.");
                    return;
                }

                List<Notification> notifications = response.body().getData().getItems();
                boolean isFullRefresh = response.body().getData().isFullRefresh();
                String previousSignature = sessionManager.getNotificationsSignature();
                
                List<Notification> fullNotifications;
                if (isFullRefresh) {
                    fullNotifications = notifications;
                } else {
                    fullNotifications = mergeNotifications(getNotificationsFullList(), notifications);
                }
                
                sessionManager.saveNotificationsJson(gson.toJson(fullNotifications));
                String signature = buildNotificationSignature(fullNotifications);
                sessionManager.saveNotificationsSignature(signature);
                sessionManager.markDomainSynced(SessionManager.DOMAIN_NOTIFICATIONS);
                if (!signature.equals(previousSignature)) {
                    SyncUpdateBus.getInstance().publish(SyncUpdateBus.DOMAIN_NOTIFICATIONS);
                }
                callback.onSuccess();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<SyncPayload<List<Notification>>>> call, @NonNull Throwable t) {
                callback.onFailure("Failed to sync notifications.");
            }
        });
    }

    /**
     * Merge an incremental ticket delta into the existing cached list by id.
     * Incoming items overwrite existing entries with the same id; new items are appended.
     * Order is preserved (existing first, new ones at the end) so downstream sorting still applies.
     */
    private List<Ticket> mergeTickets(String existingJson, List<Ticket> delta) {
        Map<Long, Ticket> merged = new LinkedHashMap<>();
        if (existingJson != null) {
            try {
                Type type = new TypeToken<List<Ticket>>() {}.getType();
                List<Ticket> existing = gson.fromJson(existingJson, type);
                if (existing != null) {
                    for (Ticket t : existing) {
                        if (t != null && t.getId() != null) merged.put(t.getId(), t);
                    }
                }
            } catch (Exception ignored) { }
        }
        if (delta != null) {
            for (Ticket t : delta) {
                if (t != null && t.getId() != null) merged.put(t.getId(), t);
            }
        }
        return new ArrayList<>(merged.values());
    }

    private String getSinceParam(String domain) {
        long lastSyncedAt = sessionManager.getDomainLastSyncedAt(domain);
        if (lastSyncedAt <= 0L) {
            return null;
        }
        return Instant.ofEpochMilli(lastSyncedAt).toString();
    }

    private void cacheStudentTicketData(List<Ticket> newTickets, boolean isFullRefresh) {
        List<Ticket> allTickets;
        if (isFullRefresh) {
            allTickets = newTickets;
        } else {
            allTickets = mergeTickets(getStudentFullList(), newTickets);
        }

        sessionManager.saveStudentAllTicketsJson(gson.toJson(allTickets));

        List<Ticket> recent = allTickets.size() > 3 ? allTickets.subList(0, 3) : allTickets;
        sessionManager.saveStudentRecentTicketsJson(gson.toJson(recent));

        int total = allTickets.size();
        int pending = 0;
        int resolved = 0;

        for (Ticket ticket : allTickets) {
            String status = ticket.getStatus();
            if ("PENDING".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status)) {
                pending++;
            }
            if ("RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
                resolved++;
            }
        }
        sessionManager.saveTicketStatsCache(total, pending, resolved);
    }

    private void cacheAdminTicketData(List<Ticket> newTickets, boolean isFullRefresh) {
        List<Ticket> allTickets;
        if (isFullRefresh) {
            allTickets = newTickets;
        } else {
            allTickets = mergeTickets(getAdminFullList(), newTickets);
        }

        sessionManager.saveAdminAllTicketsJson(gson.toJson(allTickets));

        int total = allTickets.size();
        int pending = 0;
        int resolved = 0;
        for (Ticket ticket : allTickets) {
            if ("PENDING".equalsIgnoreCase(ticket.getStatus())) pending++;
            if ("RESOLVED".equalsIgnoreCase(ticket.getStatus()) || "CLOSED".equalsIgnoreCase(ticket.getStatus())) resolved++;
        }
        sessionManager.saveAdminStatsCache(total, pending, resolved);

        List<Ticket> pendingTickets = new ArrayList<>();
        for (Ticket ticket : allTickets) {
            if ("PENDING".equalsIgnoreCase(ticket.getStatus())) {
                pendingTickets.add(ticket);
            }
        }
        pendingTickets.sort(Comparator.comparing(Ticket::getCreatedAt, Comparator.nullsLast(String::compareTo)).reversed());
        if (pendingTickets.size() > 5) {
            pendingTickets = pendingTickets.subList(0, 5);
        }
        sessionManager.saveAdminRecentTicketsJson(gson.toJson(pendingTickets));
    }

    private List<Ticket> mergeTickets(List<Ticket> existing, List<Ticket> deltas) {
        if (existing == null || existing.isEmpty()) return deltas;
        if (deltas == null || deltas.isEmpty()) return existing;

        Map<Long, Ticket> map = new HashMap<>();
        for (Ticket t : existing) {
            if (t.getId() != null) map.put(t.getId(), t);
        }
        for (Ticket t : deltas) {
            if (t.getId() != null) map.put(t.getId(), t);
        }

        List<Ticket> merged = new ArrayList<>(map.values());
        merged.sort(Comparator.comparing(Ticket::getCreatedAt, Comparator.nullsLast(String::compareTo)).reversed());
        return merged;
    }

    private List<Notification> mergeNotifications(List<Notification> existing, List<Notification> deltas) {
        if (existing == null || existing.isEmpty()) return deltas;
        if (deltas == null || deltas.isEmpty()) return existing;

        Map<Long, Notification> map = new HashMap<>();
        for (Notification n : existing) {
            if (n.getId() != null) map.put(n.getId(), n);
        }
        for (Notification n : deltas) {
            if (n.getId() != null) map.put(n.getId(), n);
        }

        List<Notification> merged = new ArrayList<>(map.values());
        // Sort by ID descending (assuming newer IDs are higher) or use a date field if available
        merged.sort((n1, n2) -> Long.compare(n2.getId(), n1.getId()));
        return merged;
    }

    private List<Ticket> getStudentFullList() {
        String json = sessionManager.getStudentAllTicketsJson();
        if (json == null) return null;
        Type type = new TypeToken<List<Ticket>>(){}.getType();
        return gson.fromJson(json, type);
    }

    private List<Ticket> getAdminFullList() {
        String json = sessionManager.getAdminAllTicketsJson();
        if (json == null) return null;
        Type type = new TypeToken<List<Ticket>>(){}.getType();
        return gson.fromJson(json, type);
    }

    private List<Notification> getNotificationsFullList() {
        String json = sessionManager.getNotificationsJson();
        if (json == null) return null;
        Type type = new TypeToken<List<Notification>>(){}.getType();
        return gson.fromJson(json, type);
    }

    private String buildTicketSignature(List<Ticket> tickets) {
        StringBuilder builder = new StringBuilder();
        for (Ticket ticket : tickets) {
            builder.append(ticket.getId() == null ? "-" : ticket.getId()).append(':')
                    .append(ticket.getUpdatedAt() == null ? "-" : ticket.getUpdatedAt()).append(';');
        }
        return String.valueOf(builder.toString().hashCode());
    }

    private String buildNotificationSignature(List<Notification> notifications) {
        StringBuilder builder = new StringBuilder();
        for (Notification notification : notifications) {
            builder.append(notification.getId() == null ? "-" : notification.getId()).append(':')
                    .append(notification.isRead()).append(';');
        }
        return String.valueOf(builder.toString().hashCode());
    }

    private interface StepCallback {
        void onSuccess();
        void onFailure(String message);
    }
}
