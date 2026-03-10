package com.example.ucms_android.ui.admin;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.TicketService;
import com.example.ucms_android.ui.adapter.RecentTicketAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardFragment extends Fragment {

    private TextView tvTotalTickets;
    private TextView tvPendingCount;
    private TextView tvResolvedCount;
    private TextView tvViewDatabase;
    private RecyclerView rvRecentTickets;
    private RecentTicketAdapter adapter;
    private TicketService ticketService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTotalTickets = view.findViewById(R.id.tvTotalTickets);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvResolvedCount = view.findViewById(R.id.tvResolvedCount);
        tvViewDatabase = view.findViewById(R.id.tvViewDatabase);
        rvRecentTickets = view.findViewById(R.id.rvRecentTickets);

        ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);

        adapter = new RecentTicketAdapter(new ArrayList<>(), ticket -> {
            Intent intent = new Intent(requireActivity(), AdminTicketDetailActivity.class);
            intent.putExtra("ticketId", ticket.getId());
            startActivity(intent);
        });
        rvRecentTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentTickets.setAdapter(adapter);

        tvViewDatabase.setOnClickListener(v -> {
            BottomNavigationView bottomNavView = requireActivity().findViewById(R.id.bottomNavView);
            if (bottomNavView != null) {
                // Updated to use the correct admin tickets menu ID
                bottomNavView.setSelectedItemId(R.id.nav_admin_tickets);
            }
        });

        loadTickets();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTickets();
    }

    private void loadTickets() {
        ticketService.getTickets(null).enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Ticket>>> call,
                                   @NonNull Response<ApiResponse<List<Ticket>>> response) {
                if (!isAdded()) {
                    return;
                }
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Ticket> tickets = response.body().getData();
                    updateStats(tickets);
                    updateRecentList(tickets);
                } else {
                    Toast.makeText(requireContext(),
                            getString(R.string.error_loading_tickets),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Ticket>>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(requireContext(),
                        getString(R.string.error_network),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStats(List<Ticket> tickets) {
        int total = tickets.size();
        int pending = 0;
        int resolved = 0;

        for (Ticket ticket : tickets) {
            if ("PENDING".equalsIgnoreCase(ticket.getStatus())) {
                pending++;
            } else if ("RESOLVED".equalsIgnoreCase(ticket.getStatus())) {
                resolved++;
            }
        }

        tvTotalTickets.setText(String.valueOf(total));
        tvPendingCount.setText(String.valueOf(pending));
        tvResolvedCount.setText(String.valueOf(resolved));
    }

    private void updateRecentList(List<Ticket> tickets) {
        List<Ticket> pendingTickets = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if ("PENDING".equalsIgnoreCase(ticket.getStatus())) {
                pendingTickets.add(ticket);
                if (pendingTickets.size() == 5) {
                    break;
                }
            }
        }
        adapter.updateData(pendingTickets);
    }
}
