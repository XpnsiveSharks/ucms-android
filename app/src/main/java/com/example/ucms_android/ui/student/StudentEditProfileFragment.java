package com.example.ucms_android.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ucms_android.MainActivity;
import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.User;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.UserService;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.auth.LoginActivity;
import com.example.ucms_android.ui.common.DialogUtils;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentEditProfileFragment extends Fragment {

    private EditText etFullName;
    private EditText etCourse;
    private EditText etYearLevel;
    private TextView tvStudentNumber;
    private TextView tvAvatarLarge;
    private MaterialButton btnSaveChanges;
    private SessionManager sessionManager;
    private UserService userService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        userService = ApiClient.getInstance(requireContext()).create(UserService.class);

        etFullName = view.findViewById(R.id.etFullName);
        etCourse = view.findViewById(R.id.etCourse);
        etYearLevel = view.findViewById(R.id.etYearLevel);
        tvStudentNumber = view.findViewById(R.id.tvStudentNumber);
        tvAvatarLarge = view.findViewById(R.id.tvAvatarLarge);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);

        loadCachedData();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisible(false);
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v -> goBack());
        btnSaveChanges.setOnClickListener(v -> saveProfile());
        
        fetchFreshData();
    }

    private void loadCachedData() {
        String cachedName = sessionManager.getCachedName();
        String cachedStudentId = sessionManager.getCachedStudentId();
        String course = sessionManager.getCachedCourse();
        String yearLevel = sessionManager.getCachedYearLevel();

        if (etFullName != null) etFullName.setText(cachedName);
        if (tvStudentNumber != null) tvStudentNumber.setText(cachedStudentId);
        if (etCourse != null) etCourse.setText(course);
        if (etYearLevel != null) etYearLevel.setText(yearLevel);
        if (tvAvatarLarge != null) tvAvatarLarge.setText(getInitials(cachedName));
    }

    private void fetchFreshData() {
        userService.getMe().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call, @NonNull Response<ApiResponse<User>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    User user = response.body().getData();
                    if (user != null) {
                        etFullName.setText(user.getName());
                        tvStudentNumber.setText(user.getStudentId());
                        etCourse.setText(user.getCourse());
                        etYearLevel.setText(user.getYearLevel() != null ? String.valueOf(user.getYearLevel()) : "");
                        tvAvatarLarge.setText(getInitials(user.getName()));
                        sessionManager.saveProfileCache(user.getName(), user.getStudentId(), user.getCourse(), user.getYearLevel());
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {}
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisible(true);
        }
    }

    private void saveProfile() {
        String name = etFullName.getText().toString().trim();
        String course = etCourse.getText().toString().trim();
        String yearLevelText = etYearLevel.getText().toString().trim();

        if (name.isEmpty()) {
            etFullName.setError("Name is required");
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("course", course);
        if (!yearLevelText.isEmpty()) {
            try {
                body.put("yearLevel", Integer.parseInt(yearLevelText));
            } catch (NumberFormatException ignored) {}
        }

        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("UPDATING...");

        userService.updateProfile(body).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call,
                                   @NonNull Response<ApiResponse<User>> response) {
                if (!isAdded()) return;

                btnSaveChanges.setEnabled(true);
                btnSaveChanges.setText("EXECUTE UPDATE");

                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body().getData();
                    sessionManager.saveProfileCache(user.getName(), user.getStudentId(),
                            user.getCourse(), user.getYearLevel());
                    
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    goBack();
                } else {
                    Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                btnSaveChanges.setEnabled(true);
                btnSaveChanges.setText("EXECUTE UPDATE");
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private void goBack() {
        if (getFragmentManager() != null) {
            getFragmentManager().popBackStack();
        }
    }
}
