package com.example.ucms_android.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    public static final String DOMAIN_PROFILE = "profile";
    public static final String DOMAIN_TICKETS = "tickets";
    public static final String DOMAIN_NOTIFICATIONS = "notifications";
    public static final String DOMAIN_ANALYTICS = "analytics";

    private static final String PREF_NAME = "ucms_session";
    private static final String KEY_TOKEN = "access_token";
    private static final String KEY_ROLE = "user_role";
    private static final String KEY_LOGGED_IN = "is_logged_in";
    private static final String KEY_CACHED_NAME = "cached_name";
    private static final String KEY_CACHED_STUDENT_ID = "cached_student_id";
    private static final String KEY_CACHED_COURSE = "cached_course";
    private static final String KEY_CACHED_YEAR_LEVEL = "cached_year_level";
    private static final String KEY_CACHED_TOTAL_TICKETS = "cached_total_tickets";
    private static final String KEY_CACHED_PENDING_COUNT = "cached_pending_count";
    private static final String KEY_CACHED_RESOLVED_TODAY = "cached_resolved_today";
    private static final String KEY_ADMIN_CACHED_TOTAL = "admin_cached_total";
    private static final String KEY_ADMIN_CACHED_PENDING = "admin_cached_pending";
    private static final String KEY_ADMIN_CACHED_RESOLVED = "admin_cached_resolved";
    private static final String KEY_ADMIN_RECENT_TICKETS_JSON = "admin_recent_tickets_json";
    private static final String KEY_ADMIN_ALL_TICKETS_JSON = "admin_all_tickets_json";
    private static final String KEY_STUDENT_RECENT_TICKETS_JSON = "student_recent_tickets_json";
    private static final String KEY_STUDENT_ALL_TICKETS_JSON = "student_all_tickets_json";
    private static final String KEY_NOTIFICATIONS_JSON = "notifications_json";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private static final String KEY_TICKETS_SIGNATURE = "tickets_signature";
    private static final String KEY_NOTIFICATIONS_SIGNATURE = "notifications_signature";
    private static final String KEY_THEME_MODE = "theme_mode";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void setThemeMode(int mode) {
        editor.putInt(KEY_THEME_MODE, mode);
        editor.apply();
    }

    public int getThemeMode() {
        // Default to MODE_NIGHT_FOLLOW_SYSTEM (-1)
        return sharedPreferences.getInt(KEY_THEME_MODE, -1);
    }

    public void saveSession(String token, String role) {
        editor.putString(KEY_TOKEN, token);
        editor.putString(KEY_ROLE, role);
        editor.putBoolean(KEY_LOGGED_IN, true);
        editor.apply();
    }

    public void saveProfileCache(String name, String studentId, String course, Integer yearLevel) {
        editor.putString(KEY_CACHED_NAME, name != null ? name : "");
        editor.putString(KEY_CACHED_STUDENT_ID, studentId != null ? studentId : "");
        editor.putString(KEY_CACHED_COURSE, course != null ? course : "");
        editor.putString(KEY_CACHED_YEAR_LEVEL, yearLevel != null ? String.valueOf(yearLevel) : "");
        editor.apply();
    }

    public void saveTicketStatsCache(int total, int pending, int resolvedToday) {
        editor.putInt(KEY_CACHED_TOTAL_TICKETS, total);
        editor.putInt(KEY_CACHED_PENDING_COUNT, pending);
        editor.putInt(KEY_CACHED_RESOLVED_TODAY, resolvedToday);
        editor.apply();
    }

    public int getCachedTotalTickets() {
        return sharedPreferences.getInt(KEY_CACHED_TOTAL_TICKETS, -1);
    }

    public int getCachedPendingCount() {
        return sharedPreferences.getInt(KEY_CACHED_PENDING_COUNT, -1);
    }

    public int getCachedResolvedToday() {
        return sharedPreferences.getInt(KEY_CACHED_RESOLVED_TODAY, -1);
    }

    public String getCachedName() {
        return sharedPreferences.getString(KEY_CACHED_NAME, "");
    }

    public String getCachedStudentId() {
        return sharedPreferences.getString(KEY_CACHED_STUDENT_ID, "");
    }

    public String getCachedCourse() {
        return sharedPreferences.getString(KEY_CACHED_COURSE, "");
    }

    public String getCachedYearLevel() {
        return sharedPreferences.getString(KEY_CACHED_YEAR_LEVEL, "");
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public String getRole() {
        return sharedPreferences.getString(KEY_ROLE, "STUDENT");
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_LOGGED_IN, false);
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    public void setCurrentUserId(String userId) {
        editor.putString(KEY_CURRENT_USER_ID, userId);
        editor.apply();
    }

    public String getCurrentUserId() {
        return sharedPreferences.getString(KEY_CURRENT_USER_ID, null);
    }

    public void markDomainSynced(String domain) {
        editor.putLong(syncKey(domain), System.currentTimeMillis());
        editor.apply();
    }

    public long getDomainLastSyncedAt(String domain) {
        return sharedPreferences.getLong(syncKey(domain), 0L);
    }

    public boolean hasBootstrapCache(String role) {
        boolean hasProfile = !getCachedName().isEmpty();
        if ("ADMIN".equalsIgnoreCase(role)) {
            return hasProfile && getAdminAllTicketsJson() != null;
        }
        return hasProfile && getStudentAllTicketsJson() != null;
    }

    public void saveTicketsSignature(String signature) {
        editor.putString(KEY_TICKETS_SIGNATURE, signature);
        editor.apply();
    }

    public String getTicketsSignature() {
        return sharedPreferences.getString(KEY_TICKETS_SIGNATURE, "");
    }

    public void saveNotificationsSignature(String signature) {
        editor.putString(KEY_NOTIFICATIONS_SIGNATURE, signature);
        editor.apply();
    }

    public String getNotificationsSignature() {
        return sharedPreferences.getString(KEY_NOTIFICATIONS_SIGNATURE, "");
    }

    public void saveNotificationsJson(String json) {
        editor.putString(KEY_NOTIFICATIONS_JSON, json);
        editor.apply();
    }

    public String getNotificationsJson() {
        return sharedPreferences.getString(KEY_NOTIFICATIONS_JSON, null);
    }

    private String syncKey(String domain) {
        String userId = getCurrentUserId();
        if (userId == null || userId.trim().isEmpty()) {
            userId = "anonymous";
        }
        return "last_sync_" + userId + "_" + domain;
    }

    // Admin stats cache
    public void saveAdminStatsCache(int total, int pending, int resolved) {
        editor.putInt(KEY_ADMIN_CACHED_TOTAL, total);
        editor.putInt(KEY_ADMIN_CACHED_PENDING, pending);
        editor.putInt(KEY_ADMIN_CACHED_RESOLVED, resolved);
        editor.apply();
    }

    public int getAdminCachedTotal() { return sharedPreferences.getInt(KEY_ADMIN_CACHED_TOTAL, -1); }
    public int getAdminCachedPending() { return sharedPreferences.getInt(KEY_ADMIN_CACHED_PENDING, -1); }
    public int getAdminCachedResolved() { return sharedPreferences.getInt(KEY_ADMIN_CACHED_RESOLVED, -1); }

    // Admin recent tickets cache (JSON string)
    public void saveAdminRecentTicketsJson(String json) {
        editor.putString(KEY_ADMIN_RECENT_TICKETS_JSON, json);
        editor.apply();
    }
    public String getAdminRecentTicketsJson() {
        return sharedPreferences.getString(KEY_ADMIN_RECENT_TICKETS_JSON, null);
    }

    // Admin all tickets cache (JSON string)
    public void saveAdminAllTicketsJson(String json) {
        editor.putString(KEY_ADMIN_ALL_TICKETS_JSON, json);
        editor.apply();
    }
    public String getAdminAllTicketsJson() {
        return sharedPreferences.getString(KEY_ADMIN_ALL_TICKETS_JSON, null);
    }

    // Student recent tickets cache (JSON string)
    public void saveStudentRecentTicketsJson(String json) {
        editor.putString(KEY_STUDENT_RECENT_TICKETS_JSON, json);
        editor.apply();
    }
    public String getStudentRecentTicketsJson() {
        return sharedPreferences.getString(KEY_STUDENT_RECENT_TICKETS_JSON, null);
    }

    // Student all tickets cache (JSON string)
    public void saveStudentAllTicketsJson(String json) {
        editor.putString(KEY_STUDENT_ALL_TICKETS_JSON, json);
        editor.apply();
    }
    public String getStudentAllTicketsJson() {
        return sharedPreferences.getString(KEY_STUDENT_ALL_TICKETS_JSON, null);
    }

    // Per-ticket detail cache (keyed by ticket ID)
    public void saveTicketDetailJson(Long ticketId, String json) {
        editor.putString("ticket_detail_" + ticketId, json);
        editor.apply();
    }

    public String getTicketDetailJson(Long ticketId) {
        return sharedPreferences.getString("ticket_detail_" + ticketId, null);
    }

    // Per-ticket responses cache (keyed by ticket ID)
    public void saveTicketResponsesJson(Long ticketId, String json) {
        editor.putString("ticket_responses_" + ticketId, json);
        editor.apply();
    }

    public String getTicketResponsesJson(Long ticketId) {
        return sharedPreferences.getString("ticket_responses_" + ticketId, null);
    }

    // Analytics cache
    public void saveAnalyticsSummaryJson(String json) {
        editor.putString("analytics_summary", json);
        editor.apply();
    }

    public String getAnalyticsSummaryJson() {
        return sharedPreferences.getString("analytics_summary", null);
    }

    public void saveAnalyticsByCategoryJson(String json) {
        editor.putString("analytics_by_category", json);
        editor.apply();
    }

    public String getAnalyticsByCategoryJson() {
        return sharedPreferences.getString("analytics_by_category", null);
    }
}
