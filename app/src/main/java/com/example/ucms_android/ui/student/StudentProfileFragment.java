package com.example.ucms_android.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.User;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.UserService;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
public class StudentProfileFragment extends Fragment {

    private EditText etFullName, etCourse, etYearLevel;
    private MaterialButton btnLogout, btnSaveChanges, btnChangePassword;
    private SessionManager sessionManager;
    private UserService userService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        etFullName = view.findViewById(R.id.etFullName);
        etCourse = view.findViewById(R.id.etCourse);
        etYearLevel = view.findViewById(R.id.etYearLevel);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);

        String cachedName = sessionManager.getCachedName();
        String cachedStudentId = sessionManager.getCachedStudentId();
        String cachedCourse = sessionManager.getCachedCourse();
        String cachedYearLevel = sessionManager.getCachedYearLevel();

        if (!cachedName.isEmpty()) {
            etFullName.setText(cachedName);
            ((android.widget.TextView) view.findViewById(R.id.tvStudentNameHeader)).setText(cachedName);
        }
        if (!cachedStudentId.isEmpty()) {
            ((android.widget.TextView) view.findViewById(R.id.tvStudentNumber)).setText(cachedStudentId);
        }
        if (!cachedCourse.isEmpty()) etCourse.setText(cachedCourse);
        if (!cachedYearLevel.isEmpty()) etYearLevel.setText(cachedYearLevel);

        userService = ApiClient.getInstance(requireContext()).create(UserService.class);

        userService.getMe().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call, @NonNull Response<ApiResponse<User>> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().getData() == null) {
                    return;
                }

                User user = response.body().getData();
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        etFullName.setText(user.getName());
                        ((android.widget.TextView) view.findViewById(R.id.tvStudentNameHeader)).setText(user.getName());
                        ((android.widget.TextView) view.findViewById(R.id.tvStudentNumber)).setText(user.getStudentId());
                        etCourse.setText(user.getCourse() != null ? user.getCourse() : "");
                        Integer yearLevel = user.getYearLevel();
                        etYearLevel.setText(yearLevel != null ? String.valueOf(yearLevel) : "");
                    });
                    sessionManager.saveProfileCache(user.getName(), user.getStudentId(), user.getCourse(), user.getYearLevel());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {
            }
        });

        btnLogout.setOnClickListener(v -> logout());
        
        btnSaveChanges.setOnClickListener(v -> {
            Map<String, Object> body = new HashMap<>();
            body.put("name", etFullName.getText().toString().trim());
            body.put("course", etCourse.getText().toString().trim());

            String yearLevelText = etYearLevel.getText().toString().trim();
            if (!yearLevelText.isEmpty()) {
                try {
                    body.put("yearLevel", Integer.parseInt(yearLevelText));
                } catch (NumberFormatException ignored) {
                }
            }

            userService.updateProfile(body).enqueue(new Callback<ApiResponse<User>>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<User>> call, @NonNull Response<ApiResponse<User>> response) {
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (response.isSuccessful()) {
                                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {
                    if (isAdded() && getActivity() != null) {
                        getActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show());
                    }
                }
            });
        });

        btnChangePassword.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Redirecting to Change Password...", Toast.LENGTH_SHORT).show();
        });
    }

    private void logout() {
        sessionManager.clearSession();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
