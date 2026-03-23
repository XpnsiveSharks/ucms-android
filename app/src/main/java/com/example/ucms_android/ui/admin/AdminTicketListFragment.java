package com.example.ucms_android.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.example.ucms_android.ui.adapter.TicketAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
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
    private MaterialButton btnNeedsAction;
    private MaterialButton btnInProgress;
    private MaterialButton btnAll;
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
        btnNeedsAction = view.findViewById(R.id.btnNeedsAction);
        btnInProgress = view.findViewById(R.id.btnInProgress);
        btnAll = view.findViewById(R.id.btnAll);
        rvTickets = view.findViewById(R.id.rvTickets);
        shimmerLayout = view.findViewById(R.id.shimmerLayout);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);
        layoutError = view.findViewById(R.id.layoutError);

        view.findViewById(R.id.btnRetry).setOnClickListener(v -> loadTickets());

        ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);
        sessionManager = new SessionManager(requireContext());
        gson = new Gson();

        adapter = new TicketAdapter(new ArrayList<>(), ticket -> {
            Intent intent = new Intent(requireActivity(), AdminTicketDetailActivity.class);
            intent.putExtra("ticketId", ticket.getId());
            startActivity(intent);
        });
        rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTickets.setAdapter(adapter);

        loadFromCache();

        setupFilters();
        setupSearch();
        loadTickets();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTickets();
    }

    private void setupFilters() {
        btnAll.setOnClickListener(v -> {
            activeFilter = "ALL";
            updateFilterButtons();
            applyFilter();
        });
        btnNeedsAction.setOnClickListener(v -> {
            activeFilter = "PENDING";
            updateFilterButtons();
            applyFilter();
        });
        btnInProgress.setOnClickListener(v -> {
            activeFilter = "IN_PROGRESS";
            updateFilterButtons();
            applyFilter();
        });
        
        updateFilterButtons();
    }

    private void updateFilterButtons() {
        btnAll.setStrokeColorResource(activeFilter.equals("ALL") ? R.color.colorPrimary : R.color.colorDivider);
        btnNeedsAction.setStrokeColorResource(activeFilter.equals("PENDING") ? R.color.colorPrimary : R.color.colorDivider);
        btnInProgress.setStrokeColorResource(activeFilter.equals("IN_PROGRESS") ? R.color.colorPrimary : R.color.colorDivider);
        
        btnAll.setTextColor(getResources().getColor(activeFilter.equals("ALL") ? R.color.colorPrimary : R.color.colorTextPrimary, null));
        btnNeedsAction.setTextColor(getResources().getColor(activeFilter.equals("PENDING") ? R.color.colorPrimary : R.color.colorTextPrimary, null));
        btnInProgress.setTextColor(getResources().getColor(activeFilter.equals("IN_PROGRESS") ? R.color.colorPrimary : R.color.colorTextPrimary, null));
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
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
        String query = etSearch.getText() != null ? etSearch.getText().toString().toLowerCase().trim() : "";
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
