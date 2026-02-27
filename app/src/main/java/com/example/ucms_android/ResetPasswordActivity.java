package com.example.ucms_android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ucms_android.api.request.PasswordUpdateRequest;
import com.example.ucms_android.network.SupabaseApiClient;
import com.example.ucms_android.network.SupabaseAuthService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputLayout tilNewPassword;
    private TextInputLayout tilConfirmPassword;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnResetPassword;
    private ProgressBar progressBar;

    private SupabaseAuthService supabaseAuthService;
    private String recoveryToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        progressBar = findViewById(R.id.progressBar);

        supabaseAuthService = SupabaseApiClient.getAuthService();

        // Extract recovery token from deep link
        recoveryToken = extractRecoveryToken(getIntent());

        if (recoveryToken == null || recoveryToken.trim().isEmpty()) {
            Snackbar.make(btnResetPassword, getString(R.string.error_invalid_reset_link), Snackbar.LENGTH_LONG).show();
            btnResetPassword.setEnabled(false);
            return;
        }

        btnResetPassword.setOnClickListener(v -> handleResetPassword());
    }

    private String extractRecoveryToken(Intent intent) {
        if (intent == null) return null;
        Uri data = intent.getData();
        if (data == null) return null;

        // Supabase sends token in fragment: ucms://reset-password#access_token=...
        String fragment = data.getFragment();
        if (fragment == null) return null;

        for (String param : fragment.split("&")) {
            if (param.startsWith("access_token=")) {
                return param.substring("access_token=".length());
            }
        }
        return null;
    }

    private void handleResetPassword() {
        String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);

        if (TextUtils.isEmpty(newPassword) || newPassword.length() < 8) {
            tilNewPassword.setError(getString(R.string.error_password_too_short));
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_passwords_do_not_match));
            return;
        }

        setLoading(true);

        PasswordUpdateRequest request = new PasswordUpdateRequest(newPassword);

        supabaseAuthService.updatePassword(
                SupabaseApiClient.ANON_KEY,
                "Bearer " + recoveryToken,
                request
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Snackbar.make(btnResetPassword, getString(R.string.success_password_reset), Snackbar.LENGTH_LONG).show();
                    btnResetPassword.postDelayed(() -> {
                        Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }, 1500);
                } else {
                    Snackbar.make(btnResetPassword, getString(R.string.error_password_reset_failed), Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                setLoading(false);
                Snackbar.make(btnResetPassword, getString(R.string.error_password_reset_failed), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnResetPassword.setEnabled(!isLoading);
    }
}
