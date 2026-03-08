package com.example.ucms_android.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ucms_android.R;
import com.example.ucms_android.api.request.PasswordRecoverRequest;
import com.example.ucms_android.network.SupabaseApiClient;
import com.example.ucms_android.network.SupabaseAuthService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPassword";

    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private MaterialButton btnSendReset;
    private TextView tvBackToLogin;
    private ProgressBar progressBar;

    private SupabaseAuthService supabaseAuthService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        btnSendReset = findViewById(R.id.btnSendReset);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        progressBar = findViewById(R.id.progressBar);

        supabaseAuthService = SupabaseApiClient.getAuthService();

        btnSendReset.setOnClickListener(v -> handleSendReset());

        tvBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void handleSendReset() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";

        tilEmail.setError(null);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email));
            return;
        }

        setLoading(true);

        PasswordRecoverRequest request = new PasswordRecoverRequest(email, "ucms://reset-password");

        supabaseAuthService.recoverPassword(SupabaseApiClient.ANON_KEY, request)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Recovery email sent successfully");
                            Snackbar.make(btnSendReset, getString(R.string.success_reset_email), Snackbar.LENGTH_LONG).show();
                            btnSendReset.setEnabled(false);
                        } else {
                            Log.e(TAG, "Recovery failed - server error: " + response.code());
                            Snackbar.make(btnSendReset, getString(R.string.error_reset_failed), Snackbar.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        setLoading(false);
                        Log.e(TAG, "Network failure during password recovery");
                        Snackbar.make(btnSendReset, getString(R.string.error_reset_failed), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSendReset.setEnabled(!isLoading);
    }
}
