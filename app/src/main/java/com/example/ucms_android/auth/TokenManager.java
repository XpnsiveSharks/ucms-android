package com.example.ucms_android.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class TokenManager {
    private static final String PREFS_NAME = "ucms_secure_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_ROLE = "user_role";

    private static TokenManager instance;

    private TokenManager() {
    }

    public static synchronized TokenManager getInstance() {
        if (instance == null) {
            instance = new TokenManager();
        }
        return instance;
    }

    public void saveToken(Context context, String token) {
        getPrefs(context).edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken(Context context) {
        return getPrefs(context).getString(KEY_TOKEN, null);
    }

    public void saveRole(Context context, String role) {
        getPrefs(context).edit().putString(KEY_ROLE, role).apply();
    }

    public String getRole(Context context) {
        return getPrefs(context).getString(KEY_ROLE, null);
    }

    public void clear(Context context) {
        getPrefs(context).edit().clear().apply();
    }

    public boolean hasToken(Context context) {
        String token = getToken(context);
        return token != null && !token.trim().isEmpty();
    }

    private SharedPreferences getPrefs(Context context) {
        Context appContext = context.getApplicationContext();
        try {
            MasterKey masterKey = new MasterKey.Builder(appContext)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            return EncryptedSharedPreferences.create(
                    appContext,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            android.util.Log.e("TokenManager", "Secure prefs corrupted, clearing and retrying", e);
            appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply();
            try {
                MasterKey masterKey = new MasterKey.Builder(appContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build();
                return EncryptedSharedPreferences.create(
                        appContext,
                        PREFS_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (GeneralSecurityException | IOException retryException) {
                android.util.Log.e("TokenManager", "Failed to initialize secure prefs after retry", retryException);
                return appContext.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE);
            }
        }
    }
}
