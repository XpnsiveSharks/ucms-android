package com.example.ucms_android.ui.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.AuthService;
import com.example.ucms_android.network.UserService;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etCurrentPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private MaterialButton btnChangePassword;
    private View btnTransmitReset;
    private ProgressBar progressBar;
    private TextView tvEmail;

    private UserService userService;
    private AuthService authService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        sessionManager = new SessionManager(this);
        userService = ApiClient.getInstance(this).create(UserService.class);
        authService = ApiClient.getInstance(this).create(AuthService.class);

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnTransmitReset = findViewById(R.id.btnTransmitReset);
        progressBar = findViewById(R.id.progressBar);
        tvEmail = findViewById(R.id.tvEmail);

        // Load contact node from cache
        String studentId = sessionManager.getCachedStudentId();
        if (studentId != null && !studentId.isEmpty()) {
            tvEmail.setText(studentId);
        }

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnChangePassword.setOnClickListener(v -> handleChangePassword());
        
        if (btnTransmitReset != null) {
            btnTransmitReset.setOnClickListener(v -> handleTransmitReset());
        }
    }

    private void handleTransmitReset() {
        String studentId = sessionManager.getCachedStudentId();
        if (studentId == null || studentId.isEmpty()) {
            Toast.makeText(this, "Student ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        Map<String, String> body = new HashMap<>();
        body.put("studentId", studentId);

        authService.forgotPassword(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                setLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(ChangePasswordActivity.this, "Reset link sent to your email", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ChangePasswordActivity.this, "Failed to send reset link", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(ChangePasswordActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleChangePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (currentPassword.isEmpty()) {
            etCurrentPassword.setError("Current password is required");
            return;
        }
        if (newPassword.length() < 8) {
            etNewPassword.setError("Password must be at least 8 characters");
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        setLoading(true);
        Map<String, String> body = new HashMap<>();
        body.put("currentPassword", currentPassword);
        body.put("newPassword", newPassword);

        userService.changePassword(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                setLoading(false);

                if (response.code() == 401) {
                    sessionManager.clearSession();
                    Intent intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return;
                }

                if (response.isSuccessful()) {
                    DialogUtils.showSuccessDialog(
                            ChangePasswordActivity.this,
                            "SUCCESS",
                            "Your password has been updated successfully.",
                            ChangePasswordActivity.this::finish
                    );
                    return;
                }

                if (response.code() == 403) {
                    Toast.makeText(ChangePasswordActivity.this, "Current password incorrect", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(ChangePasswordActivity.this, "Failed to change password", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                setLoading(false);
                Toast.makeText(ChangePasswordActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnChangePassword.setEnabled(!loading);
        if (btnTransmitReset != null) btnTransmitReset.setEnabled(!loading);
    }
}
