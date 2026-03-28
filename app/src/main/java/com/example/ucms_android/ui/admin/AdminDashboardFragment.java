package com.example.ucms_android.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.MainActivity;
import com.example.ucms_android.ui.common.AnalyticsFragment;
import com.example.ucms_android.ui.adapter.RecentTicketAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardFragment extends Fragment {

    private TextView tvTotalTickets;
    private TextView tvPendingCount;
    private TextView tvResolvedCount;
    private ShimmerFrameLayout shimmerRecentTickets;
    private TextView tvEmptyRecent;
    private RecyclerView rvRecentTickets;
    private RecentTicketAdapter adapter;
    private TicketService ticketService;
    private SessionManager sessionManager;
    private Gson gson;

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
        rvRecentTickets = view.findViewById(R.id.rvRecentTickets);
        shimmerRecentTickets = view.findViewById(R.id.shimmerRecentTickets);
        tvEmptyRecent = view.findViewById(R.id.tvEmptyRecent);

        view.findViewById(R.id.btnTelemetry).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new AnalyticsFragment());
                BottomNavigationView nav = getActivity().findViewById(R.id.bottomNavView);
                if (nav != null) nav.setSelectedItemId(R.id.nav_admin_analytics);
            }
        });

        view.findViewById(R.id.tvViewAll).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new AdminTicketListFragment());
                BottomNavigationView nav = getActivity().findViewById(R.id.bottomNavView);
                if (nav != null) nav.setSelectedItemId(R.id.nav_admin_tickets);
            }
        });

        ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);
        sessionManager = new SessionManager(requireContext());
        gson = new Gson();

        adapter = new RecentTicketAdapter(ticket -> {
            Intent intent = new Intent(requireActivity(), AdminTicketDetailActivity.class);
            intent.putExtra("ticketId", ticket.getId());
            startActivity(intent);
        });
        rvRecentTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentTickets.setAdapter(adapter);

        loadFromCache();
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
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Ticket> tickets = response.body().getData();
                    updateStats(tickets);
                    updateRecentList(tickets);
                } else {
                    if (rvRecentTickets.getVisibility() != View.VISIBLE) {
                        showRecentState("EMPTY");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Ticket>>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                if (rvRecentTickets.getVisibility() != View.VISIBLE) {
                    showRecentState("EMPTY");
                }
            }
        });
    }

    private void loadFromCache() {
        int cachedTotal = sessionManager.getAdminCachedTotal();
        int cachedPending = sessionManager.getAdminCachedPending();
        int cachedResolved = sessionManager.getAdminCachedResolved();
        if (cachedTotal >= 0) tvTotalTickets.setText(String.valueOf(cachedTotal));
        if (cachedPending >= 0) tvPendingCount.setText(String.valueOf(cachedPending));
        if (cachedResolved >= 0) tvResolvedCount.setText(String.valueOf(cachedResolved));

        String cachedJson = sessionManager.getAdminRecentTicketsJson();
        if (cachedJson != null) {
            Type type = new TypeToken<List<Ticket>>() {}.getType();
            List<Ticket> cachedTickets = gson.fromJson(cachedJson, type);
            if (cachedTickets != null && !cachedTickets.isEmpty()) {
                adapter.updateData(cachedTickets);
                showRecentState("DATA");
                return;
            }
        }
        showRecentState("LOADING");
    }

    private void showRecentState(String state) {
        shimmerRecentTickets.setVisibility(View.GONE);
        shimmerRecentTickets.stopShimmer();
        tvEmptyRecent.setVisibility(View.GONE);
        rvRecentTickets.setVisibility(View.GONE);

        switch (state) {
            case "LOADING":
                shimmerRecentTickets.setVisibility(View.VISIBLE);
                shimmerRecentTickets.startShimmer();
                tvTotalTickets.setText("--");
                tvPendingCount.setText("--");
                tvResolvedCount.setText("--");
                break;
            case "EMPTY":
                tvEmptyRecent.setVisibility(View.VISIBLE);
                break;
            case "DATA":
                rvRecentTickets.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void updateStats(List<Ticket> tickets) {
        int total = tickets.size();
        int pending = 0;
        int resolved = 0;

        for (Ticket ticket : tickets) {
            if ("PENDING".equalsIgnoreCase(ticket.getStatus())) pending++;
            else if ("RESOLVED".equalsIgnoreCase(ticket.getStatus())) resolved++;
        }

        tvTotalTickets.setText(String.valueOf(total));
        tvPendingCount.setText(String.valueOf(pending));
        tvResolvedCount.setText(String.valueOf(resolved));

        sessionManager.saveAdminStatsCache(total, pending, resolved);
    }

    private void updateRecentList(List<Ticket> tickets) {
        List<Ticket> pendingTickets = new ArrayList<>();
        for (Ticket ticket : tickets) {
            if ("PENDING".equalsIgnoreCase(ticket.getStatus())) {
                pendingTickets.add(ticket);
                if (pendingTickets.size() == 5) break;
            }
        }
        adapter.updateData(pendingTickets);
        sessionManager.saveAdminRecentTicketsJson(gson.toJson(pendingTickets));
        showRecentState(pendingTickets.isEmpty() ? "EMPTY" : "DATA");
    }
}
