package com.example.ucms_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ucms_android.auth.EmailVerificationActivity;
import com.example.ucms_android.auth.TokenManager;
import com.example.ucms_android.model.ApiError;
import com.example.ucms_android.model.AuthResponse;
import com.example.ucms_android.model.LoginRequest;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.AuthService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^\\d{4}\\d{4}-[A-Za-z]$");

    private TextInputEditText etStudentId;
    private TextInputEditText etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private View rootView;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authService = ApiClient.getInstance(this).create(AuthService.class);

        rootView = findViewById(android.R.id.content);
        etStudentId = findViewById(R.id.etStudentId);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> attemptLogin());
        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
        findViewById(R.id.tvGoToRegister).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String studentId = getText(etStudentId);
        String password = getText(etPassword);

        if (studentId.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_empty_fields));
            return;
        }

        if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
            showError(getString(R.string.error_invalid_student_id));
            return;
        }

        setLoading(true);

        LoginRequest request = new LoginRequest(studentId, password);
        authService.login(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                runOnUiThread(() -> {
                    setLoading(false);

                    if (response.isSuccessful() && response.body() != null) {
                        AuthResponse authResponse = response.body();
                        TokenManager tokenManager = TokenManager.getInstance();
                        tokenManager.saveToken(LoginActivity.this, authResponse.getToken());
                        tokenManager.saveRole(LoginActivity.this, authResponse.getRole());

                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                        return;
                    }

                    ApiError apiError = parseApiError(response);
                    if (response.code() == 403
                            && apiError != null
                            && "ACCOUNT_LIMITED".equalsIgnoreCase(apiError.getCode())) {
                        startActivity(new Intent(LoginActivity.this, EmailVerificationActivity.class));
                        finish();
                        return;
                    }

                    String message = apiError != null && apiError.getMessage() != null
                            ? apiError.getMessage()
                            : getString(R.string.error_login_failed);
                    showError(message);
                });
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(getString(R.string.error_login_failed));
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
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
