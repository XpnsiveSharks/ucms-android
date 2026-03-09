package com.example.ucms_android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ucms_android.ui.auth.LoginActivity;
import com.example.ucms_android.model.ApiError;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.RegisterRequest;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.AuthService;
import com.example.ucms_android.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^\\d{4}\\d{4}-[A-Za-z]$");

    private TextInputEditText etStudentId;
    private TextInputEditText etFullName;
    private TextInputEditText etCourse;
    private TextInputEditText etYearLevel;
    private TextInputEditText etPassword;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;
    private View rootView;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authService = ApiClient.getInstance(this).create(AuthService.class);

        rootView = findViewById(android.R.id.content);
        etStudentId = findViewById(R.id.etStudentId);
        etFullName = findViewById(R.id.etFullName);
        etCourse = findViewById(R.id.etCourse);
        etYearLevel = findViewById(R.id.etYearLevel);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);

        btnRegister.setOnClickListener(v -> attemptRegister());
        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void attemptRegister() {
        String studentId = getText(etStudentId);
        String name = getText(etFullName);
        String course = getText(etCourse);
        String yearLevelText = getText(etYearLevel);
        String password = getText(etPassword);

        if (studentId.isEmpty() || name.isEmpty() || course.isEmpty() || yearLevelText.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_empty_fields));
            return;
        }

        if (!STUDENT_ID_PATTERN.matcher(studentId).matches()) {
            showError(getString(R.string.error_invalid_student_id));
            return;
        }

        int yearLevel;
        try {
            yearLevel = Integer.parseInt(yearLevelText);
        } catch (NumberFormatException exception) {
            showError(getString(R.string.error_empty_fields));
            return;
        }

        setLoading(true);

        RegisterRequest request = new RegisterRequest(studentId, name, course, yearLevel, password);
        authService.register(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Void>> call, @NonNull Response<ApiResponse<Void>> response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(RegisterActivity.this, getString(R.string.success_register), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
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
            public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(getString(R.string.error_register_failed));
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
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
