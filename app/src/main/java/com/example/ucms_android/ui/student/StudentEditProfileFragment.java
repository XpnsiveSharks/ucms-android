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

import com.example.ucms_android.MainActivity;
import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.User;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.UserService;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.auth.LoginActivity;
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
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);

        String name = sessionManager.getCachedName();
        String course = sessionManager.getCachedCourse();
        String yearLevel = sessionManager.getCachedYearLevel();
        if (!name.isEmpty()) {
            etFullName.setText(name);
        }
        if (!course.isEmpty()) {
            etCourse.setText(course);
        }
        if (!yearLevel.isEmpty()) {
            etYearLevel.setText(yearLevel);
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisible(false);
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v -> goBack());
        btnSaveChanges.setOnClickListener(v -> saveProfile());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisible(true);
        }
    }

    private void saveProfile() {
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

        btnSaveChanges.setEnabled(false);
        userService.updateProfile(body).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call,
                                   @NonNull Response<ApiResponse<User>> response) {
                if (!isAdded()) {
                    return;
                }

                btnSaveChanges.setEnabled(true);
                if (response.code() == 401) {
                    sessionManager.clearSession();
                    Intent intent = new Intent(requireActivity(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                    return;
                }

                if (response.code() == 403) {
                    String message = getString(R.string.access_denied);
                    try {
                        if (response.errorBody() != null && response.errorBody().string().contains("ACCOUNT_LIMITED")) {
                            message = getString(R.string.account_limited_prompt);
                        }
                    } catch (IOException ignored) {
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (response.code() == 400 && response.body() != null && response.body().getMessage() != null) {
                    Toast.makeText(requireContext(), response.body().getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    User user = response.body().getData();
                    sessionManager.saveProfileCache(user.getName(), user.getStudentId(),
                            user.getCourse(), user.getYearLevel());
                    Toast.makeText(requireContext(), getString(R.string.profile_updated), Toast.LENGTH_SHORT).show();
                    goBack();
                    return;
                }

                Toast.makeText(requireContext(), getString(R.string.error_update_profile), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                btnSaveChanges.setEnabled(true);
                Toast.makeText(requireContext(), getString(R.string.error_no_connection), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goBack() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
