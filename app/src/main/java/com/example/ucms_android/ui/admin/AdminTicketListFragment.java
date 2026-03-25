package com.example.ucms_android.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import com.example.ucms_android.ui.adapter.TicketAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminTicketListFragment extends Fragment {

    private EditText etSearch;
    private View tabAll, tabInProgress, tabResolved;
    private TextView tvTabAll, tvTabInProgress, tvTabResolved;
    private View indicatorAll, indicatorInProgress, indicatorResolved;
    private RecyclerView rvTickets;
    private ShimmerFrameLayout shimmerLayout;
    private LinearLayout layoutEmpty;
    private LinearLayout layoutError;
    private TicketAdapter adapter;
    private TicketService ticketService;
    private SessionManager sessionManager;
    private Gson gson;

    private List<Ticket> allTickets = new ArrayList<>();
    private String activeFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_ticket_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etSearch = view.findViewById(R.id.etSearch);
        tabAll = view.findViewById(R.id.tabAll);
        tabInProgress = view.findViewById(R.id.tabInProgress);
        tabResolved = view.findViewById(R.id.tabResolved);
        
        tvTabAll = view.findViewById(R.id.tvTabAll);
        tvTabInProgress = view.findViewById(R.id.tvTabInProgress);
        tvTabResolved = view.findViewById(R.id.tvTabResolved);
        
        indicatorAll = view.findViewById(R.id.indicatorAll);
        indicatorInProgress = view.findViewById(R.id.indicatorInProgress);
        indicatorResolved = view.findViewById(R.id.indicatorResolved);
        
        rvTickets = view.findViewById(R.id.rvTickets);
        shimmerLayout = view.findViewById(R.id.shimmerLayout);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        layoutError = view.findViewById(R.id.layoutError);

        view.findViewById(R.id.btnRetry).setOnClickListener(v -> loadTickets());

        ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);
        sessionManager = new SessionManager(requireContext());
        gson = new Gson();

        adapter = new TicketAdapter(new ArrayList<>(), true, ticket -> {
            Intent intent = new Intent(requireActivity(), AdminTicketDetailActivity.class);
            intent.putExtra("ticketId", ticket.getId());
            startActivity(intent);
        });
        rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTickets.setAdapter(adapter);

        loadFromCache();
        setupTabs();
        loadTickets();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTickets();
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v -> {
            activeFilter = "ALL";
            updateTabs();
            applyFilter();
        });
        tabInProgress.setOnClickListener(v -> {
            activeFilter = "IN_PROGRESS";
            updateTabs();
            applyFilter();
        });
        tabResolved.setOnClickListener(v -> {
            activeFilter = "RESOLVED";
            updateTabs();
            applyFilter();
        });
        
        updateTabs();
    }

    private void updateTabs() {
        tvTabAll.setTextColor(getResources().getColor(activeFilter.equals("ALL") ? R.color.colorElevatedSession : R.color.colorTextSecondary, null));
        indicatorAll.setVisibility(activeFilter.equals("ALL") ? View.VISIBLE : View.INVISIBLE);
        
        tvTabInProgress.setTextColor(getResources().getColor(activeFilter.equals("IN_PROGRESS") ? R.color.colorElevatedSession : R.color.colorTextSecondary, null));
        indicatorInProgress.setVisibility(activeFilter.equals("IN_PROGRESS") ? View.VISIBLE : View.INVISIBLE);
        
        tvTabResolved.setTextColor(getResources().getColor(activeFilter.equals("RESOLVED") ? R.color.colorElevatedSession : R.color.colorTextSecondary, null));
        indicatorResolved.setVisibility(activeFilter.equals("RESOLVED") ? View.VISIBLE : View.INVISIBLE);
    }

    private void showState(String state) {
        shimmerLayout.setVisibility(View.GONE);
        shimmerLayout.stopShimmer();
        layoutEmpty.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        rvTickets.setVisibility(View.GONE);

        switch (state) {
            case "LOADING":
                shimmerLayout.setVisibility(View.VISIBLE);
                shimmerLayout.startShimmer();
                break;
            case "EMPTY":
                layoutEmpty.setVisibility(View.VISIBLE);
                break;
            case "ERROR":
                layoutError.setVisibility(View.VISIBLE);
                break;
            case "DATA":
                rvTickets.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void loadFromCache() {
        String cachedJson = sessionManager.getAdminAllTicketsJson();
        if (cachedJson != null) {
            Type type = new TypeToken<List<Ticket>>() {}.getType();
            List<Ticket> cached = gson.fromJson(cachedJson, type);
            if (cached != null && !cached.isEmpty()) {
                allTickets = cached;
                applyFilter();
                return;
            }
        }
        showState("LOADING");
    }

    private void loadTickets() {
        ticketService.getTickets(null).enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Ticket>>> call,
                                   @NonNull Response<ApiResponse<List<Ticket>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    allTickets = response.body().getData();
                    sessionManager.saveAdminAllTicketsJson(gson.toJson(allTickets));
                    applyFilter();
                } else {
                    if (rvTickets.getVisibility() != View.VISIBLE) {
                        showState("ERROR");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Ticket>>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                if (rvTickets.getVisibility() != View.VISIBLE) {
                    showState("ERROR");
                }
            }
        });
    }

    private void applyFilter() {
        String query = etSearch != null && etSearch.getText() != null ? etSearch.getText().toString().toLowerCase().trim() : "";
        List<Ticket> filtered = new ArrayList<>();

        for (Ticket ticket : allTickets) {
            boolean matchesFilter = activeFilter.equals("ALL") || activeFilter.equalsIgnoreCase(ticket.getStatus());
            boolean matchesSearch = query.isEmpty()
                    || (ticket.getTitle() != null && ticket.getTitle().toLowerCase().contains(query))
                    || (ticket.getTicketNumber() != null && ticket.getTicketNumber().toLowerCase().contains(query));

            if (matchesFilter && matchesSearch) {
                filtered.add(ticket);
            }
        }

        filtered.sort((left, right) -> {
            int leftRank = priorityRank(left);
            int rightRank = priorityRank(right);
            if (leftRank != rightRank) {
                return Integer.compare(leftRank, rightRank);
            }
            return Long.compare(ticketTimestamp(right), ticketTimestamp(left));
        });

        adapter.updateData(filtered);
        showState(filtered.isEmpty() ? "EMPTY" : "DATA");
    }

    private int priorityRank(Ticket ticket) {
        if (ticket == null) {
            return 5;
        }

        String priority = resolvePriorityLevel(ticket);
        if ("CRITICAL".equals(priority)) {
            return 0;
        }
        if ("HIGH".equals(priority)) {
            return 1;
        }
        if ("LOW".equals(priority)) {
            return 2;
        }
        if ("MUTED".equals(priority)) {
            return 3;
        }
        return 4;
    }

    private String resolvePriorityLevel(Ticket ticket) {
        String label = ticket.getUrgencyLabel();
        if (label != null && !label.trim().isEmpty()) {
            String normalized = label.trim().toUpperCase(Locale.ROOT);
            if ("MEDIUM".equals(normalized)) {
                return "LOW";
            }
            if ("CRITICAL".equals(normalized) || "HIGH".equals(normalized)
                    || "LOW".equals(normalized) || "MUTED".equals(normalized)) {
                return normalized;
            }
        }

        Integer score = ticket.getUrgencyScore();
        if (score != null) {
            if (score >= 80) return "CRITICAL";
            if (score >= 55) return "HIGH";
            if (score >= 25) return "LOW";
            return "MUTED";
        }

        if (ticket.isUrgent()) {
            return "HIGH";
        }
        return null;
    }

    private long ticketTimestamp(Ticket ticket) {
        String raw = ticket.getCreatedAt();
        if (raw == null || raw.trim().isEmpty()) {
            return 0L;
        }

        try {
            return OffsetDateTime.parse(raw).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        return 0L;
    }
}
