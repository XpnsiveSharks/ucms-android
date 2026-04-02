package com.example.ucms_android.ui.admin;

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
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminEditProfileFragment extends Fragment {

    private EditText etFullName;
    private TextView tvEmail;
    private TextView tvAvatarLarge;
    private MaterialButton btnSaveChanges;
    private SessionManager sessionManager;
    private UserService userService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        userService = ApiClient.getInstance(requireContext()).create(UserService.class);

        etFullName = view.findViewById(R.id.etFullName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvAvatarLarge = view.findViewById(R.id.tvAvatarLarge);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisible(false);
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v -> goBack());
        btnSaveChanges.setOnClickListener(v -> saveProfile());

        loadCachedData();
        fetchFreshData();
    }

    private void loadCachedData() {
        String cachedName = sessionManager.getCachedName();
        if (etFullName != null) etFullName.setText(cachedName);
        if (tvAvatarLarge != null) tvAvatarLarge.setText(getInitials(cachedName));
    }

    private void fetchFreshData() {
        userService.getMe().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call,
                                   @NonNull Response<ApiResponse<User>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    User user = response.body().getData();
                    etFullName.setText(user.getName());
                    if (tvEmail != null) tvEmail.setText(user.getEmail());
                    tvAvatarLarge.setText(getInitials(user.getName()));
                    sessionManager.saveProfileCache(user.getName(), user.getStudentId(),
                            user.getCourse(), user.getYearLevel());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {}
        });
    }

    private void saveProfile() {
        String name = etFullName.getText().toString().trim();
        if (name.isEmpty()) {
            etFullName.setError("Name is required");
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("name", name);

        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("UPDATING...");

        userService.updateProfile(body).enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call,
                                   @NonNull Response<ApiResponse<User>> response) {
                if (!isAdded()) return;
                btnSaveChanges.setEnabled(true);
                btnSaveChanges.setText("EXECUTE UPDATE");

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisible(true);
        }
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
