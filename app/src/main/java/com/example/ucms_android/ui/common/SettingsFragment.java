package com.example.ucms_android.ui.common;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.ucms_android.MainActivity;
import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.User;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.UserService;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.auth.LoginActivity;
import com.example.ucms_android.ui.student.StudentEditProfileFragment;
import com.google.android.material.card.MaterialCardView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    private TextView tvUserName;
    private TextView tvAvatarLarge;
    private TextView tvUserDetails;
    private View cvEditProfile;
    private View cvChangePassword;
    private View cvLogout;
    private SessionManager sessionManager;
    private UserService userService;
    private boolean isAdmin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        sessionManager = new SessionManager(requireContext());
        isAdmin = "ADMIN".equalsIgnoreCase(sessionManager.getRole());

        return inflater.inflate(R.layout.fragment_student_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userService = ApiClient.getInstance(requireContext()).create(UserService.class);

        // Map views based on the common IDs in fragment_student_settings
        tvUserName = view.findViewById(R.id.tvUserName);
        tvAvatarLarge = view.findViewById(R.id.tvAvatarLarge);
        tvUserDetails = view.findViewById(R.id.tvUserDetails);

        cvEditProfile = view.findViewById(R.id.cvEditProfile);
        cvChangePassword = view.findViewById(R.id.cvChangePassword);
        cvLogout = view.findViewById(R.id.cvLogout);

        // Update labels based on role
        TextView tvConfigLabel = view.findViewById(R.id.tvConfigLabel);
        if (isAdmin && tvConfigLabel != null) {
            tvConfigLabel.setText("ADMINISTRATOR CONFIG");
        }

        loadInitialData();
        fetchLiveData();
        if (cvEditProfile != null) {
            cvEditProfile.setOnClickListener(v -> {
                if (isAdmin) {
                    Toast.makeText(requireContext(), getString(R.string.coming_soon), Toast.LENGTH_SHORT).show();
                } else {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).loadFragment(new StudentEditProfileFragment(), true);
                    }
                }
            });
        }

        if (cvChangePassword != null) {
            cvChangePassword.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), ChangePasswordActivity.class)));
        }

        if (cvLogout != null) {
            cvLogout.setOnClickListener(v -> {
                sessionManager.clearSession();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        }
    }

    private void loadInitialData() {
        String cachedName = sessionManager.getCachedName();
        if (!cachedName.isEmpty()) {
            if (tvUserName != null) tvUserName.setText(cachedName);
            if (tvAvatarLarge != null) tvAvatarLarge.setText(getInitials(cachedName));
        }

        if (tvUserDetails != null) {
            if (isAdmin) {
                tvUserDetails.setText("SYSTEM ACCESS: LEVEL 4");
                updateRoleBadge("SYSTEM ADMINISTRATOR", R.color.colorAccentBlue);
            } else {
                String studentId = sessionManager.getCachedStudentId();
                String course = sessionManager.getCachedCourse();
                String year = sessionManager.getCachedYearLevel();

                StringBuilder sb = new StringBuilder();
                if (studentId != null) sb.append(studentId);
                if (course != null) {
                    if (sb.length() > 0) sb.append(" • ");
                    sb.append(course);
                }
                if (year != null) {
                    if (sb.length() > 0) sb.append(" • ");
                    sb.append("Year ").append(year);
                }
                tvUserDetails.setText(sb.toString());
                updateRoleBadge("ACTIVE STUDENT", R.color.colorSuccess);
            }
        }
    }

    private void fetchLiveData() {
        userService.getMe().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call,
                                   @NonNull Response<ApiResponse<User>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    User user = response.body().getData();
                    if (tvUserName != null) tvUserName.setText(user.getName());
                    if (tvAvatarLarge != null) tvAvatarLarge.setText(getInitials(user.getName()));

                    if (tvUserDetails != null) {
                        if (isAdmin) {
                            tvUserDetails.setText("SYSTEM ACCESS: LEVEL 4");
                            updateRoleBadge("SYSTEM ADMINISTRATOR", R.color.colorAccentBlue);
                        } else {
                            StringBuilder sb = new StringBuilder();
                            if (user.getStudentId() != null) sb.append(user.getStudentId());
                            if (user.getCourse() != null) {
                                if (sb.length() > 0) sb.append(" • ");
                                sb.append(user.getCourse());
                            }
                            if (user.getYearLevel() != null) {
                                if (sb.length() > 0) sb.append(" • ");
                                sb.append("Year ").append(user.getYearLevel());
                            }
                            tvUserDetails.setText(sb.toString());
                            updateRoleBadge("ACTIVE STUDENT", R.color.colorSuccess);
                        }
                    }

                    sessionManager.saveProfileCache(user.getName(), user.getStudentId(),
                            user.getCourse(), user.getYearLevel());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {}
        });
    }

    private void updateRoleBadge(String label, int colorResId) {
        View view = getView();
        if (view == null) return;
        
        TextView tvRoleLabel = view.findViewById(R.id.tvRoleLabel);
        View vRoleIndicator = view.findViewById(R.id.vRoleIndicator);
        
        if (tvRoleLabel != null) {
            tvRoleLabel.setText(label);
            tvRoleLabel.setTextColor(ContextCompat.getColor(requireContext(), colorResId));
        }
        if (vRoleIndicator != null) {
            vRoleIndicator.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), colorResId)));
        }
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "??";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
