package com.example.ucms_android.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ucms_android.R;

public class StudentHomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Setup Submit button listener
        view.findViewById(R.id.btnSubmitNewConcern).setOnClickListener(v -> {
            // Navigate to SubmitTicketActivity
        });
        
        // Setup View All listener
        view.findViewById(R.id.tvViewAll).setOnClickListener(v -> {
            // Navigate to NotificationsActivity
        });
    }
}
