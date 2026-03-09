package com.example.ucms_android.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.ui.adapter.RecentTicketAdapter;

import java.util.ArrayList;
import java.util.List;

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
            if (getActivity() instanceof com.example.ucms_android.MainActivity) {
                ((com.example.ucms_android.MainActivity) getActivity()).loadFragment(new SubmitTicketFragment());
                // Update bottom nav selection
                com.google.android.material.bottomnavigation.BottomNavigationView nav = getActivity().findViewById(R.id.bottomNavView);
                if (nav != null) nav.setSelectedItemId(R.id.nav_student_add);
            }
        });
        
        // Setup View All listener
        view.findViewById(R.id.tvViewAll).setOnClickListener(v -> {
            // Navigate to NotificationsActivity
        });

        // Setup RecyclerView with dummy data
        RecyclerView rvNotifications = view.findViewById(R.id.rvRecentNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        
        List<Ticket> dummyTickets = new ArrayList<>();
        Ticket t1 = new Ticket();
        t1.setTicketNumber("#10149");
        t1.setTitle("Issue with Subject Pre-requisites");
        t1.setCategory("Academic Dept");
        t1.setCreatedAt("10 mins ago");
        
        Ticket t2 = new Ticket();
        t2.setTicketNumber("#10150");
        t2.setTitle("Portal Login Failed");
        t2.setCategory("IT Support");
        t2.setCreatedAt("2 hours ago");
        
        dummyTickets.add(t1);
        dummyTickets.add(t2);
        
        RecentTicketAdapter adapter = new RecentTicketAdapter(dummyTickets, ticket -> {
            // Handle ticket click
        });
        rvNotifications.setAdapter(adapter);
    }
}
