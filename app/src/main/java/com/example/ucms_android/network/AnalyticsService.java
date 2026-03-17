package com.example.ucms_android.network;

import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.CategoryCount;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.model.auth.AnalyticsSummary;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface AnalyticsService {

    @GET("api/analytics/summary")
    Call<ApiResponse<AnalyticsSummary>> getSummary();

    @GET("api/analytics/by-category")
    Call<ApiResponse<List<CategoryCount>>> getByCategory();

    @GET("api/analytics/unresolved")
    Call<ApiResponse<List<Ticket>>> getUnresolved();
}
