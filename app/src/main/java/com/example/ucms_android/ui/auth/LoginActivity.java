package com.example.ucms_android.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ucms_android.MainActivity;
import com.example.ucms_android.R;
import com.example.ucms_android.auth.EmailVerificationActivity;
import com.example.ucms_android.model.ApiError;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.AuthResponse;
import com.example.ucms_android.model.LoginRequest;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.AuthService;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.auth.TokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^\\d{4}\\d{4}-[A-Za-z]$");

    private EditText etStudentId;
    private EditText etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private View rootView;
    private AuthService authService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authService = ApiClient.getInstance(this).create(AuthService.class);
        sessionManager = new SessionManager(this);

        rootView = findViewById(android.R.id.content);
        etStudentId = findViewById(R.id.etStudentId);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        btnLogin.setOnClickListener(v -> attemptLogin());
        
        View tvForgot = findViewById(R.id.tvForgotPassword);
        if (tvForgot != null) {
            tvForgot.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));
        }
    }

    private void attemptLogin() {
        String studentId = etStudentId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

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
        authService.login(request).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AuthResponse>> call, @NonNull Response<ApiResponse<AuthResponse>> response) {
                runOnUiThread(() -> {
                    setLoading(false);

                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        AuthResponse authResponse = response.body().getData();
                        if (authResponse != null && authResponse.getAccessToken() != null) {
                            String role = extractRoleFromJwt(authResponse.getAccessToken());
                            sessionManager.saveSession(authResponse.getAccessToken(), role);
                            TokenManager.getInstance().saveToken(LoginActivity.this, authResponse.getAccessToken());
                            TokenManager.getInstance().saveRole(LoginActivity.this, role);
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                            return;
                        }
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
            public void onFailure(@NonNull Call<ApiResponse<AuthResponse>> call, @NonNull Throwable t) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(getString(R.string.error_login_failed));
                });
            }
        });
    }

    private String extractRoleFromJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return "STUDENT";
            String payload = parts[1];
            int mod = payload.length() % 4;
            if (mod != 0) payload += "====".substring(mod);
            byte[] decoded = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE);
            String json = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            org.json.JSONObject obj = new org.json.JSONObject(json);
            if (obj.has("user_metadata")) {
                org.json.JSONObject meta = obj.getJSONObject("user_metadata");
                if (meta.has("role")) return meta.getString("role");
            }
            return "STUDENT";
        } catch (Exception e) {
            return "STUDENT";
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
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
