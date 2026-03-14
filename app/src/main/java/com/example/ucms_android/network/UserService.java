package com.example.ucms_android.network;

import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.User;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;

public interface UserService {
    @GET("api/users/me")
    Call<ApiResponse<User>> getMe();

    @PUT("api/users/me/email")
    Call<Void> updateEmail(@Body Map<String, String> body);

    @PUT("api/users/me")
    Call<ApiResponse<User>> updateProfile(@Body Map<String, Object> body);

    @PATCH("api/users/me/email/verify")
    Call<Void> confirmEmailVerified();
}
