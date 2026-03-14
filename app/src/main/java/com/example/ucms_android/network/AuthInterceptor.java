package com.example.ucms_android.network;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.ucms_android.auth.TokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private final Context appContext;

    public AuthInterceptor(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String path = originalRequest.url().encodedPath();

        // Skip auth for public endpoints
        if (path.contains("/api/auth/login")
                || path.contains("/api/auth/register")
                || path.contains("/api/auth/forgot-password")) {
            return chain.proceed(originalRequest);
        }

        String token = TokenManager.getInstance().getToken(appContext);
        if (token != null && !token.trim().isEmpty()) {
            Request authRequest = originalRequest.newBuilder()
                    .addHeader("Authorization", "Bearer " + token)
                    .build();
            return chain.proceed(authRequest);
        }

        return chain.proceed(originalRequest);
    }
}
