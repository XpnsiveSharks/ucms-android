package com.example.ucms_android.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.example.ucms_android.ui.common.ChangePasswordActivity;
import com.google.android.material.card.MaterialCardView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentProfileFragment extends Fragment {

    private TextView tvStudentNameHeader;
    private TextView tvStudentSubtitle;
    private MaterialCardView cvEditProfile;
    private MaterialCardView cvChangePassword;
    private View cvLogout;
    private SessionManager sessionManager;
    private UserService userService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        userService = ApiClient.getInstance(requireContext()).create(UserService.class);

        tvStudentNameHeader = view.findViewById(R.id.tvStudentNameHeader);
        tvStudentSubtitle = view.findViewById(R.id.tvStudentSubtitle);
        cvEditProfile = view.findViewById(R.id.cvEditProfile);
        cvChangePassword = view.findViewById(R.id.cvChangePassword);
        cvLogout = view.findViewById(R.id.cvLogout);

        String cachedName = sessionManager.getCachedName();
        String cachedStudentId = sessionManager.getCachedStudentId();
        String cachedCourse = sessionManager.getCachedCourse();
        String cachedYearLevel = sessionManager.getCachedYearLevel();
        if (!cachedName.isEmpty()) {
            tvStudentNameHeader.setText(cachedName);
        }
        if (tvStudentSubtitle != null && !cachedStudentId.isEmpty()) {
            StringBuilder subtitleBuilder = new StringBuilder(cachedStudentId);
            if (!cachedCourse.isEmpty()) {
                subtitleBuilder.append(" \u2022 ").append(cachedCourse);
            }
            if (!cachedYearLevel.isEmpty()) {
                subtitleBuilder.append(" \u2022 Year ").append(cachedYearLevel);
            }
            tvStudentSubtitle.setText(subtitleBuilder.toString());
        }

        userService.getMe().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call,
                                   @NonNull Response<ApiResponse<User>> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    User user = response.body().getData();
                    tvStudentNameHeader.setText(user.getName());
                    StringBuilder subtitleBuilder = new StringBuilder();
                    if (user.getStudentId() != null && !user.getStudentId().isEmpty()) {
                        subtitleBuilder.append(user.getStudentId());
                    }
                    if (user.getCourse() != null && !user.getCourse().isEmpty()) {
                        if (subtitleBuilder.length() > 0) {
                            subtitleBuilder.append(" \u2022 ");
                        }
                        subtitleBuilder.append(user.getCourse());
                    }
                    if (user.getYearLevel() != null) {
                        if (subtitleBuilder.length() > 0) {
                            subtitleBuilder.append(" \u2022 ");
                        }
                        subtitleBuilder.append("Year ").append(user.getYearLevel());
                    }
                    if (tvStudentSubtitle != null) {
                        tvStudentSubtitle.setText(subtitleBuilder.toString());
                    }
                    sessionManager.saveProfileCache(user.getName(), user.getStudentId(),
                            user.getCourse(), user.getYearLevel());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {
            }
        });

        cvEditProfile.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new StudentEditProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        cvChangePassword.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ChangePasswordActivity.class)));

        cvLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }
}
