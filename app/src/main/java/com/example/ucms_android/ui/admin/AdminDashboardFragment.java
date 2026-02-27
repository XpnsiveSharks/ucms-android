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
            bottomNavView.setSelectedItemId(R.id.nav_tickets);
        });

        loadTickets();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTickets();
    }

    private void loadTickets() {
        ticketService.getTickets().enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(@NonNull Call<List<Ticket>> call,
                                   @NonNull Response<List<Ticket>> response) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Ticket> tickets = response.body();
                        updateStats(tickets);
                        updateRecentList(tickets);
                    } else {
                        Toast.makeText(requireContext(),
                                getString(R.string.error_loading_tickets),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<List<Ticket>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                getString(R.string.error_network),
                                Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updateStats(List<Ticket> tickets) {
        int total = tickets.size();
        int pending = 0;
        int resolvedToday = 0;

        for (Ticket ticket : tickets) {
            if ("PENDING".equalsIgnoreCase(ticket.getStatus())) {
                pending++;
            }
            if ("RESOLVED".equalsIgnoreCase(ticket.getStatus())) {
                resolvedToday++;
            }
        }

        tvTotalTickets.setText(String.valueOf(total));
        tvPendingCount.setText(String.valueOf(pending));
        tvResolvedCount.setText(String.valueOf(resolvedToday));
    }

    private void updateRecentList(List<Ticket> tickets) {
        List<Ticket> pending = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if ("PENDING".equalsIgnoreCase(ticket.getStatus())) {
                pending.add(ticket);
                if (pending.size() == 5) {
                    break;
                }
            }
        }
        adapter.updateData(pending);
    }
}
