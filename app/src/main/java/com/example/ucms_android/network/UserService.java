package com.example.ucms_android.network;

import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.User;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.PUT;

public interface UserService {
    @GET("api/users/admins")
    Call<ApiResponse<List<User>>> getAdmins();

    @GET("api/users/me")
    Call<ApiResponse<User>> getMe();

    @PUT("api/users/me/email")
    Call<Void> updateEmail(@Body Map<String, String> body);

    @PUT("api/users/me")
    Call<ApiResponse<User>> updateProfile(@Body Map<String, Object> body);

    @PUT("api/users/me/password")
    Call<ApiResponse<Void>> changePassword(@Body Map<String, String> body);

    @PATCH("api/users/me/email/verify")
    Call<Void> confirmEmailVerified();
}
