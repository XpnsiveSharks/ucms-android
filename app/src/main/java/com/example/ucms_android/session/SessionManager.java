package com.example.ucms_android.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
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

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
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
}
