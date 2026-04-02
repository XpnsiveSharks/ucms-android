package com.example.ucms_android.sync;

import android.content.Context;
import android.os.SystemClock;

import com.example.ucms_android.BuildConfig;
import com.example.ucms_android.model.RealtimeEvent;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.BufferedSource;

public class RealtimeStreamManager {

    private static final long RECONNECT_DELAY_MS = 3_000L;
    private static final long MIN_SYNC_TRIGGER_MS = 1_500L;

    private final OkHttpClient okHttpClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Gson gson = new Gson();
    private final BootstrapCoordinator bootstrapCoordinator;

    private volatile boolean running = false;
    private volatile Call activeCall;
    private volatile long lastSyncTriggeredAt = 0L;

    public RealtimeStreamManager(Context context) {
        Context appContext = context.getApplicationContext();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(BuildConfig.DEBUG
                ? HttpLoggingInterceptor.Level.BASIC
                : HttpLoggingInterceptor.Level.NONE);
        loggingInterceptor.redactHeader("Authorization");

        this.okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new com.example.ucms_android.network.AuthInterceptor(appContext))
                .addInterceptor(loggingInterceptor)
                .build();
        this.bootstrapCoordinator = new BootstrapCoordinator(appContext);
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        executor.execute(this::runLoop);
    }

    public synchronized void stop() {
        running = false;
        if (activeCall != null) {
            activeCall.cancel();
        }
    }

    private void runLoop() {
        while (running) {
            Response response = null;
            try {
                Request request = new Request.Builder()
                        .url(BuildConfig.BACKEND_BASE_URL + "api/realtime/stream")
                        .header("Accept", "text/event-stream")
                        .build();

                activeCall = okHttpClient.newCall(request);
                response = activeCall.execute();

                if (!response.isSuccessful() || response.body() == null) {
                    sleepBeforeReconnect();
                    continue;
                }

                BufferedSource source = response.body().source();
                while (running) {
                    String line = source.readUtf8Line();
                    if (line == null) {
                        break;
                    }
                    handleSseLine(line);
                }
            } catch (IOException ignored) {
            } finally {
                if (response != null) {
                    response.close();
                }
                activeCall = null;
            }

            sleepBeforeReconnect();
        }
    }

    private void handleSseLine(String line) {
        if (line == null || !line.startsWith("data:")) {
            return;
        }

        String payload = line.substring(5).trim();
        if (payload.isEmpty()) {
            return;
        }

        try {
            RealtimeEvent event = gson.fromJson(payload, RealtimeEvent.class);
            if (event == null || "CONNECTED".equalsIgnoreCase(event.getEventType())) {
                return;
            }

            long now = SystemClock.elapsedRealtime();
            if (now - lastSyncTriggeredAt < MIN_SYNC_TRIGGER_MS) {
                return;
            }
            lastSyncTriggeredAt = now;
            bootstrapCoordinator.runBackgroundSync();
        } catch (Exception ignored) {
        }
    }

    private void sleepBeforeReconnect() {
        if (!running) {
            return;
        }
        try {
            Thread.sleep(RECONNECT_DELAY_MS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
