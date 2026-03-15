package com.example.ucms_android.network;

import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.Notification;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.Path;

public interface NotificationService {

    @GET("api/notifications")
    Call<ApiResponse<List<Notification>>> getNotifications();

    @PATCH("api/notifications/{id}/read")
    Call<ApiResponse<Notification>> markAsRead(@Path("id") Long id);

    @PATCH("api/notifications/read-all")
    Call<ApiResponse<Void>> markAllAsRead();
}
