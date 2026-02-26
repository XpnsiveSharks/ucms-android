package com.example.ucms_android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ucms_android.auth.TokenManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TokenManager tokenManager = TokenManager.getInstance();
        if (!tokenManager.hasToken(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String role = tokenManager.getRole(this);
        String safeRole = role == null || role.trim().isEmpty() ? "UNKNOWN" : role;
        Toast.makeText(this, "Logged in as " + safeRole, Toast.LENGTH_SHORT).show();
    }
}
