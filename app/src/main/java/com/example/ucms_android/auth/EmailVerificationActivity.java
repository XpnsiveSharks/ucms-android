package com.example.ucms_android.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ucms_android.MainActivity;
import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiError;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.UserService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmailVerificationActivity extends AppCompatActivity {
    private TextInputEditText etEmail;
    private MaterialButton btnSendVerification;
    private ProgressBar progressBar;
    private View rootView;
    private UserService userService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        userService = ApiClient.getInstance(this).create(UserService.class);

        rootView = findViewById(android.R.id.content);
        etEmail = findViewById(R.id.etEmail);
        btnSendVerification = findViewById(R.id.btnSendVerification);
        progressBar = findViewById(R.id.progressBar);

        btnSendVerification.setOnClickListener(v -> sendVerificationEmail());
        findViewById(R.id.tvSkip).setOnClickListener(v -> {
            startActivity(new Intent(EmailVerificationActivity.this, MainActivity.class));
            finish();
        });
    }

    private void sendVerificationEmail() {
        String email = getText(etEmail);
        if (email.isEmpty()) {
            showError(getString(R.string.error_empty_fields));
            return;
        }

        setLoading(true);

        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        userService.updateEmail(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    if (response.isSuccessful()) {
                        Toast.makeText(
                                EmailVerificationActivity.this,
                                "Verification email sent. Check your inbox.",
                                Toast.LENGTH_SHORT
                        ).show();
                        finish();
                        return;
                    }

                    ApiError apiError = parseApiError(response);
                    String message = apiError != null && apiError.getMessage() != null
                            ? apiError.getMessage()
                            : getString(R.string.error_register_failed);
                    showError(message);
                });
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(getString(R.string.error_register_failed));
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSendVerification.setEnabled(!loading);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private void showError(String message) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
    }

    private ApiError parseApiError(Response<?> response) {
        if (response.errorBody() == null) {
            return null;
        }

        try {
            return new Gson().fromJson(response.errorBody().string(), ApiError.class);
        } catch (IOException exception) {
            return null;
        }
    }
}
