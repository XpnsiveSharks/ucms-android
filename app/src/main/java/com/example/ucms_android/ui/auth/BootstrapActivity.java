package com.example.ucms_android.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ucms_android.MainActivity;
import com.example.ucms_android.R;
import com.example.ucms_android.sync.BootstrapCoordinator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class BootstrapActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private TextView tvBootstrapMessage;
    private MaterialButton btnRetry;
    private BootstrapCoordinator bootstrapCoordinator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bootstrap);

        progressBar = findViewById(R.id.progressBootstrap);
        tvBootstrapMessage = findViewById(R.id.tvBootstrapMessage);
        btnRetry = findViewById(R.id.btnRetryBootstrap);

        bootstrapCoordinator = new BootstrapCoordinator(this);

        btnRetry.setOnClickListener(v -> startBootstrap());
        startBootstrap();
    }

    private void startBootstrap() {
        setLoading(true, getString(R.string.bootstrap_loading));
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
                    setLoading(false, getString(R.string.bootstrap_failed));
                    Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    private void setLoading(boolean isLoading, String message) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRetry.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        tvBootstrapMessage.setText(message);
    }
}
