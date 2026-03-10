package com.example.ucms_android.ui.common;

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
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;

public class SettingsFragment extends Fragment {

    private EditText etFullName, etDepartment;
    private MaterialButton btnSaveChanges, btnLogout;
    private SessionManager sessionManager;

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

        etFullName = view.findViewById(R.id.etFullName);
        etDepartment = view.findViewById(R.id.etDepartment);
        btnSaveChanges = view.findViewById(R.id.btnSaveChanges);
        btnLogout = view.findViewById(R.id.btnLogout);

        btnSaveChanges.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Changes saved successfully", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            sessionManager.clearSession();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }
}
