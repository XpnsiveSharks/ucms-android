package com.example.ucms_android.network;

import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.AuthResponse;
import com.example.ucms_android.model.LoginRequest;
import com.example.ucms_android.model.RegisterRequest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {
    @POST("api/auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("api/auth/register")
    Call<ApiResponse<Void>> register(@Body RegisterRequest request);

    @POST("api/auth/forgot-password")
    Call<Void> forgotPassword(@Body Map<String, String> body);
}
