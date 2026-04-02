package com.example.ucms_android.sync;

import android.os.Handler;
import android.os.Looper;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class SyncUpdateBus {

    public interface Listener {
        void onSyncUpdated(String domain);
    }

    public static final String DOMAIN_PROFILE = "profile";
    public static final String DOMAIN_TICKETS = "tickets";
    public static final String DOMAIN_NOTIFICATIONS = "notifications";
    public static final String DOMAIN_ANALYTICS = "analytics";

    private static SyncUpdateBus instance;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SyncUpdateBus() {
    }

    public static synchronized SyncUpdateBus getInstance() {
        if (instance == null) {
            instance = new SyncUpdateBus();
        }
        return instance;
    }

    public void register(Listener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void unregister(Listener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public void publish(String domain) {
        mainHandler.post(() -> {
            for (Listener listener : listeners) {
                listener.onSyncUpdated(domain);
            }
        });
    }
}
