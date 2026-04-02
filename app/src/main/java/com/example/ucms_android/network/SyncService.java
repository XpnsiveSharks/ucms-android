package com.example.ucms_android.network;

import com.example.ucms_android.model.AnalyticsOverview;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.Notification;
import com.example.ucms_android.model.SyncPayload;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface SyncService {

    @GET("api/sync/profile")
    Call<ApiResponse<SyncPayload<User>>> syncProfile(@Query("since") String since);

    @GET("api/sync/tickets")
    Call<ApiResponse<SyncPayload<List<Ticket>>>> syncTickets(@Query("since") String since);

    @GET("api/sync/notifications")
    Call<ApiResponse<SyncPayload<List<Notification>>>> syncNotifications(@Query("since") String since);

    @GET("api/sync/analytics")
    Call<ApiResponse<SyncPayload<AnalyticsOverview>>> syncAnalytics(@Query("since") String since);
}
