package com.example.ucms_android.network;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class SupabaseApiClient {

    private static final String BASE_URL = "https://icosjzuwekgilmmxgqwr.supabase.co/";
    public static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imljb3NqenV3ZWtnaWxtbXhncXdyIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzIwMDkyNTgsImV4cCI6MjA4NzU4NTI1OH0.wXKERkACJyoudEGMpumz83zAiIg9dMoJuC5xbbhriJA";

    private static Retrofit instance;

    public static Retrofit getInstance() {
        if (instance == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            instance = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return instance;
    }

    public static SupabaseAuthService getAuthService() {
        return getInstance().create(SupabaseAuthService.class);
    }
}
