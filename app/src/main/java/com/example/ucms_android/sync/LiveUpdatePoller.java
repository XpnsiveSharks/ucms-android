package com.example.ucms_android.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class LiveUpdatePoller {

    private static final long POLL_INTERVAL_MS = 20_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final BootstrapCoordinator bootstrapCoordinator;

    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            bootstrapCoordinator.runBackgroundSync();
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    public LiveUpdatePoller(Context context) {
        this.bootstrapCoordinator = new BootstrapCoordinator(context.getApplicationContext());
    }

    public void start() {
        stop();
        handler.post(pollRunnable);
    }

    public void stop() {
        handler.removeCallbacks(pollRunnable);
    }
}
