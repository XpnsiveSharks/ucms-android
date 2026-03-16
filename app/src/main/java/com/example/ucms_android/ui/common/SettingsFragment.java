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
import androidx.fragment.app.Fragment;

import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.User;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.UserService;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.auth.LoginActivity;
import com.google.android.material.card.MaterialCardView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsFragment extends Fragment {

    private TextView tvAdminName;
    private TextView tvAvatarLarge;
    private MaterialCardView cvEditProfile;
    private MaterialCardView cvChangePassword;
    private MaterialCardView cvLogout;
    private SessionManager sessionManager;
    private UserService userService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        userService = ApiClient.getInstance(requireContext()).create(UserService.class);

        tvAdminName = view.findViewById(R.id.tvAdminName);
        tvAvatarLarge = view.findViewById(R.id.tvAvatarLarge);
        cvEditProfile = view.findViewById(R.id.cvEditProfile);
        cvChangePassword = view.findViewById(R.id.cvChangePassword);
        cvLogout = view.findViewById(R.id.cvLogout);

        String cachedName = sessionManager.getCachedName();
        if (!cachedName.isEmpty()) {
            tvAdminName.setText(cachedName);
            tvAvatarLarge.setText(getInitials(cachedName));
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
                    tvAdminName.setText(user.getName());
                    tvAvatarLarge.setText(getInitials(user.getName()));
                    sessionManager.saveProfileCache(user.getName(), null, null, null);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {
            }
        });

        cvEditProfile.setOnClickListener(v ->
                Toast.makeText(requireContext(), getString(R.string.coming_soon), Toast.LENGTH_SHORT).show());

        cvChangePassword.setOnClickListener(v ->
                Toast.makeText(requireContext(), getString(R.string.coming_soon), Toast.LENGTH_SHORT).show());

        cvLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getString(R.string.avatar_initials);
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
