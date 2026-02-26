package com.example.ucms_android.network;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.PUT;

public interface UserService {
    @PUT("api/users/me/email")
    Call<Void> updateEmail(@Body Map<String, String> body);
}
