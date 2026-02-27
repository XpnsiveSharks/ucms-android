package com.example.ucms_android;

import android.app.Application;

import com.example.ucms_android.util.NotificationHelper;

public class UcmsApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannel(this);
    }
}
