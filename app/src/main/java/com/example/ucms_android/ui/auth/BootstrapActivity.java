package com.example.ucms_android.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.ucms_android.MainActivity;
import com.example.ucms_android.R;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.sync.BootstrapCoordinator;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class BootstrapActivity extends AppCompatActivity {

    private ShimmerFrameLayout shimmerBootstrap;
    private View layoutError;
    private TextView tvBootstrapMessage;
    private MaterialButton btnRetry;
    private BootstrapCoordinator bootstrapCoordinator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SessionManager sessionManager = new SessionManager(this);
        AppCompatDelegate.setDefaultNightMode(sessionManager.getThemeMode());
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bootstrap);

        shimmerBootstrap = findViewById(R.id.shimmerBootstrap);
        layoutError = findViewById(R.id.layoutBootstrapError);
        tvBootstrapMessage = findViewById(R.id.tvBootstrapMessage);
        btnRetry = findViewById(R.id.btnRetryBootstrap);

        bootstrapCoordinator = new BootstrapCoordinator(this);

        btnRetry.setOnClickListener(v -> startBootstrap());
        startBootstrap();
    }

    private void startBootstrap() {
        setLoading(true, null);
        bootstrapCoordinator.runBootstrap(new BootstrapCoordinator.CallbackResult() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    startActivity(new Intent(BootstrapActivity.this, MainActivity.class));
                    finish();
                });
            }

            @Override
            public void onFailure(String message) {
                runOnUiThread(() -> {
                    setLoading(false, message);
                });
            }
        });
    }

    private void setLoading(boolean isLoading, String errorMessage) {
        if (isLoading) {
            shimmerBootstrap.setVisibility(View.VISIBLE);
            shimmerBootstrap.startShimmer();
            layoutError.setVisibility(View.GONE);
        } else {
            shimmerBootstrap.stopShimmer();
            shimmerBootstrap.setVisibility(View.GONE);
            layoutError.setVisibility(View.VISIBLE);
            if (errorMessage != null) {
                tvBootstrapMessage.setText(errorMessage);
            }
        }
    }
}
