package com.example.ucms_android.network;

import com.example.ucms_android.api.request.PasswordRecoverRequest;
import com.example.ucms_android.api.request.PasswordUpdateRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;

public interface SupabaseAuthService {

    // Triggers password reset email
    @Headers("Content-Type: application/json")
    @POST("auth/v1/recover")
    Call<Void> recoverPassword(
            @Header("apikey") String apiKey,
            @Body PasswordRecoverRequest body
    );

    // Updates password using recovery token
    @Headers("Content-Type: application/json")
    @PUT("auth/v1/user")
    Call<Void> updatePassword(
            @Header("apikey") String apiKey,
            @Header("Authorization") String bearerToken,
            @Body PasswordUpdateRequest body
    );
}
