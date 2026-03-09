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

import com.example.ucms_android.R;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.auth.LoginActivity;
import com.google.android.material.button.MaterialButton;

public class StudentProfileFragment extends Fragment {

    private EditText etFullName, etCourse, etYearLevel;
    private MaterialButton btnLogout, btnSaveChanges, btnChangePassword;
    private SessionManager sessionManager;

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

        // Pre-fill with dummy data (In a real app, these would come from SessionManager/User model)
        etFullName.setText("Rain Louie Robles");
        etCourse.setText("BS in Computer Science");
        etYearLevel.setText("3rd Year");

        btnLogout.setOnClickListener(v -> logout());
        
        btnSaveChanges.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Profile changes saved (Simulated)", Toast.LENGTH_SHORT).show();
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
