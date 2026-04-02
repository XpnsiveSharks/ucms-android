package com.example.ucms_android.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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
import com.example.ucms_android.sync.SyncUpdateBus;
import com.example.ucms_android.ui.adapter.TicketAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketListFragment extends Fragment {

    private TextView tvEmptyState;
    private RecyclerView rvTickets;
    private MaterialButton btnFilter;
    private View btnSubmitTicket;
    private ShimmerFrameLayout shimmerLayout;
    private LinearLayout layoutError;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tabAll, tabInProgress, tabResolved;
    private View tabIndicator;
    private TicketAdapter adapter;
    private TicketService ticketService;
    private SessionManager sessionManager;
    private Gson gson;
    private List<Ticket> allTickets = new ArrayList<>();
    private final SyncUpdateBus.Listener syncListener = domain -> {
        if (SyncUpdateBus.DOMAIN_TICKETS.equals(domain) && isAdded()) {
            refreshFromCache();
        }
    };

    private String statusFilter = "ALL";
    private String sortFilter = "NEWEST";
    private String categoryFilter = "ALL";
    private String adminResponseFilter = "ALL";
    private String dateRangeFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ticket_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        rvTickets = view.findViewById(R.id.rvTickets);
        btnFilter = view.findViewById(R.id.btnFilter);
        btnSubmitTicket = view.findViewById(R.id.btnSubmitTicket);
        shimmerLayout = view.findViewById(R.id.shimmerLayout);
        layoutError = view.findViewById(R.id.layoutError);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        tabAll = view.findViewById(R.id.tabAll);
        tabInProgress = view.findViewById(R.id.tabInProgress);
        tabResolved = view.findViewById(R.id.tabResolved);
        tabIndicator = view.findViewById(R.id.tabIndicator);
        view.findViewById(R.id.btnRetry).setOnClickListener(v -> loadTickets());

        swipeRefresh.setOnRefreshListener(() -> {
            swipeRefresh.setRefreshing(false);
            showState("LOADING");
            loadTickets();
        });

        ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);
        sessionManager = new SessionManager(requireContext());
        gson = new Gson();

        adapter = new TicketAdapter(new ArrayList<>(), false, ticket -> {
            Intent intent = new Intent(requireActivity(), TicketDetailActivity.class);
            intent.putExtra("ticketId", ticket.getId());
            startActivity(intent);
        });
        rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTickets.setAdapter(adapter);

        btnFilter.setOnClickListener(v -> showFilterDialog());

        btnSubmitTicket.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.ucms_android.MainActivity) {
                ((com.example.ucms_android.MainActivity) getActivity()).loadFragment(new SubmitTicketFragment());
                // Update bottom nav selection
                com.google.android.material.bottomnavigation.BottomNavigationView nav = getActivity().findViewById(R.id.bottomNavView);
                if (nav != null) nav.setSelectedItemId(R.id.nav_student_add);
            }
        });

        setupTabs();
        boolean hasCached = loadFromCache();
        if (!hasCached) {
            loadTickets();
        }
    }

    private void setupTabs() {
        tabAll.setOnClickListener(v -> {
            statusFilter = "ALL";
            updateTabs();
            applyFilters();
        });
        tabInProgress.setOnClickListener(v -> {
            statusFilter = "IN_PROGRESS";
            updateTabs();
            applyFilters();
        });
        tabResolved.setOnClickListener(v -> {
            statusFilter = "RESOLVED";
            updateTabs();
            applyFilters();
        });
        updateTabs();
    }

    private void updateTabs() {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.colorHomeSubmitButton, typedValue, true);
        int colorSelected = typedValue.data;
        int colorUnselected = getResources().getColor(R.color.colorTextSecondary, null);

        tabAll.setTextColor("ALL".equals(statusFilter) ? colorSelected : colorUnselected);
        tabInProgress.setTextColor("IN_PROGRESS".equals(statusFilter) ? colorSelected : colorUnselected);
        tabResolved.setTextColor("RESOLVED".equals(statusFilter) ? colorSelected : colorUnselected);

        tabIndicator.post(() -> {
            TextView activeTab = tabAll;
            if ("IN_PROGRESS".equals(statusFilter)) activeTab = tabInProgress;
            else if ("RESOLVED".equals(statusFilter)) activeTab = tabResolved;

            android.widget.RelativeLayout.LayoutParams params = (android.widget.RelativeLayout.LayoutParams) tabIndicator.getLayoutParams();
            params.width = activeTab.getWidth();
            params.leftMargin = activeTab.getLeft();
            tabIndicator.setLayoutParams(params);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        SyncUpdateBus.getInstance().register(syncListener);
        refreshFromCache();
    }

    @Override
    public void onPause() {
        super.onPause();
        SyncUpdateBus.getInstance().unregister(syncListener);
    }

    private void showState(String state) {
        shimmerLayout.setVisibility(View.GONE);
        shimmerLayout.stopShimmer();
        layoutError.setVisibility(View.GONE);
        rvTickets.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);

        switch (state) {
            case "LOADING":
                shimmerLayout.setVisibility(View.VISIBLE);
                shimmerLayout.startShimmer();
                break;
            case "EMPTY":
                tvEmptyState.setVisibility(View.VISIBLE);
                break;
            case "ERROR":
                layoutError.setVisibility(View.VISIBLE);
                break;
            case "DATA":
                rvTickets.setVisibility(View.VISIBLE);
                break;
        }
    }

    private boolean loadFromCache() {
        String cachedJson = sessionManager.getStudentAllTicketsJson();
        if (cachedJson != null) {
            Type type = new TypeToken<List<Ticket>>() {}.getType();
            List<Ticket> cached = gson.fromJson(cachedJson, type);
            if (cached != null && !cached.isEmpty()) {
                allTickets = cached;
                applyFilters();
                return true;
            }
        }
        showState("LOADING");
        return false;
    }

    private void refreshFromCache() {
        String cachedJson = sessionManager.getStudentAllTicketsJson();
        if (cachedJson == null) return;
        Type type = new TypeToken<List<Ticket>>() {}.getType();
        List<Ticket> cached = gson.fromJson(cachedJson, type);
        if (cached != null) {
            allTickets = cached;
            applyFilters();
        }
    }

    private void loadTickets() {
        ticketService.getTickets(null).enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Ticket>>> call,
                                   @NonNull Response<ApiResponse<List<Ticket>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    allTickets = response.body().getData();
                    sessionManager.saveStudentAllTicketsJson(gson.toJson(allTickets));
                    applyFilters();
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

    private void showFilterDialog() {
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_student_ticket_filters, null, false);

        AutoCompleteTextView dropStatus = dialogView.findViewById(R.id.dropStatus);
        AutoCompleteTextView dropSortBy = dialogView.findViewById(R.id.dropSortBy);
        AutoCompleteTextView dropCategory = dialogView.findViewById(R.id.dropCategory);
        AutoCompleteTextView dropAdminResponse = dialogView.findViewById(R.id.dropAdminResponse);
        AutoCompleteTextView dropDateRange = dialogView.findViewById(R.id.dropDateRange);
        MaterialButton btnResetFilters = dialogView.findViewById(R.id.btnResetFilters);
        MaterialButton btnApplyFilters = dialogView.findViewById(R.id.btnApplyFilters);

        String[] statusOptions = new String[] {
                getString(R.string.filter_all_statuses),
                getString(R.string.filter_pending),
                getString(R.string.filter_in_progress),
                getString(R.string.filter_resolved),
                getString(R.string.filter_closed)
        };
        String[] sortOptions = new String[] {
                getString(R.string.filter_newest),
                getString(R.string.filter_oldest)
        };

        List<String> categoryOptions = new ArrayList<>();
        categoryOptions.add(getString(R.string.filter_all_categories));
        Set<String> categories = new LinkedHashSet<>();
        for (Ticket ticket : allTickets) {
            if (ticket.getCategoryName() != null && !ticket.getCategoryName().trim().isEmpty()) {
                categories.add(ticket.getCategoryName().trim());
            }
        }
        categoryOptions.addAll(categories);

        String[] adminResponseOptions = new String[] {
                getString(R.string.filter_all_responses),
                getString(R.string.filter_with_response),
                getString(R.string.filter_no_response)
        };

        String[] dateRangeOptions = new String[] {
                getString(R.string.filter_all_time),
                getString(R.string.filter_last_7_days),
                getString(R.string.filter_last_30_days)
        };

        dropStatus.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, statusOptions));
        dropSortBy.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, sortOptions));
        dropCategory.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, categoryOptions));
        dropAdminResponse.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, adminResponseOptions));
        dropDateRange.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, dateRangeOptions));

        dropStatus.setText(statusLabel(statusFilter), false);
        dropSortBy.setText(sortLabel(sortFilter), false);
        dropCategory.setText(categoryLabel(categoryFilter), false);
        dropAdminResponse.setText(adminResponseLabel(adminResponseFilter), false);
        dropDateRange.setText(dateRangeLabel(dateRangeFilter), false);

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(dialogView);

        btnResetFilters.setOnClickListener(v -> {
            statusFilter = "ALL";
            sortFilter = "NEWEST";
            categoryFilter = "ALL";
            adminResponseFilter = "ALL";
            dateRangeFilter = "ALL";

            applyFilters();
            dialog.dismiss();
        });

        btnApplyFilters.setOnClickListener(v -> {
            statusFilter = statusValue(dropStatus.getText() != null ? dropStatus.getText().toString() : "");
            sortFilter = sortValue(dropSortBy.getText() != null ? dropSortBy.getText().toString() : "");
            categoryFilter = categoryValue(dropCategory.getText() != null ? dropCategory.getText().toString() : "");
            adminResponseFilter = adminResponseValue(dropAdminResponse.getText() != null ? dropAdminResponse.getText().toString() : "");
            dateRangeFilter = dateRangeValue(dropDateRange.getText() != null ? dropDateRange.getText().toString() : "");

            applyFilters();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void applyFilters() {
        List<Ticket> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Ticket ticket : allTickets) {
            if (!matchesStatus(ticket)) {
                continue;
            }
            if (!matchesCategory(ticket)) {
                continue;
            }
            if (!matchesAdminResponse(ticket)) {
                continue;
            }
            if (!matchesDateRange(ticket, now)) {
                continue;
            }
            filtered.add(ticket);
        }

        Comparator<Ticket> comparator = Comparator.comparingLong(this::ticketTimestamp);
        if (!"OLDEST".equals(sortFilter)) {
            comparator = comparator.reversed();
        }
        filtered.sort(comparator);

        adapter.updateData(filtered);
        if (filtered.isEmpty()) {
            if (allTickets.isEmpty()) {
                tvEmptyState.setText(getString(R.string.no_tickets_yet));
            } else {
                tvEmptyState.setText(getString(R.string.no_tickets_match_filters));
            }
            showState("EMPTY");
        } else {
            showState("DATA");
        }
    }

    private boolean matchesStatus(Ticket ticket) {
        return "ALL".equals(statusFilter)
                || (ticket.getStatus() != null && statusFilter.equalsIgnoreCase(ticket.getStatus()));
    }

    private boolean matchesCategory(Ticket ticket) {
        if ("ALL".equals(categoryFilter)) {
            return true;
        }
        return ticket.getCategoryName() != null
                && categoryFilter.equalsIgnoreCase(ticket.getCategoryName().trim());
    }

    private boolean matchesAdminResponse(Ticket ticket) {
        if ("ALL".equals(adminResponseFilter)) {
            return true;
        }
        if ("WITH_RESPONSE".equals(adminResponseFilter)) {
            return ticket.hasAdminResponse();
        }
        if ("NO_RESPONSE".equals(adminResponseFilter)) {
            return !ticket.hasAdminResponse();
        }
        return true;
    }

    private boolean matchesDateRange(Ticket ticket, long now) {
        if ("ALL".equals(dateRangeFilter)) {
            return true;
        }

        long createdAt = ticketTimestamp(ticket);
        if (createdAt <= 0L) {
            return false;
        }

        long diff = now - createdAt;
        if ("7D".equals(dateRangeFilter)) {
            return diff <= 7L * 24L * 60L * 60L * 1000L;
        }
        if ("30D".equals(dateRangeFilter)) {
            return diff <= 30L * 24L * 60L * 60L * 1000L;
        }
        return true;
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

    private String statusLabel(String value) {
        if ("PENDING".equals(value)) {
            return getString(R.string.filter_pending);
        }
        if ("IN_PROGRESS".equals(value)) {
            return getString(R.string.filter_in_progress);
        }
        if ("RESOLVED".equals(value)) {
            return getString(R.string.filter_resolved);
        }
        if ("CLOSED".equals(value)) {
            return getString(R.string.filter_closed);
        }
        return getString(R.string.filter_all_statuses);
    }

    private String sortLabel(String value) {
        return "OLDEST".equals(value)
                ? getString(R.string.filter_oldest)
                : getString(R.string.filter_newest);
    }

    private String categoryLabel(String value) {
        if ("ALL".equals(value)) {
            return getString(R.string.filter_all_categories);
        }
        return value;
    }

    private String adminResponseLabel(String value) {
        if ("WITH_RESPONSE".equals(value)) {
            return getString(R.string.filter_with_response);
        }
        if ("NO_RESPONSE".equals(value)) {
            return getString(R.string.filter_no_response);
        }
        return getString(R.string.filter_all_responses);
    }

    private String dateRangeLabel(String value) {
        if ("7D".equals(value)) {
            return getString(R.string.filter_last_7_days);
        }
        if ("30D".equals(value)) {
            return getString(R.string.filter_last_30_days);
        }
        return getString(R.string.filter_all_time);
    }

    private String statusValue(String label) {
        if (label.equals(getString(R.string.filter_pending))) {
            return "PENDING";
        }
        if (label.equals(getString(R.string.filter_in_progress))) {
            return "IN_PROGRESS";
        }
        if (label.equals(getString(R.string.filter_resolved))) {
            return "RESOLVED";
        }
        if (label.equals(getString(R.string.filter_closed))) {
            return "CLOSED";
        }
        return "ALL";
    }

    private String sortValue(String label) {
        return label.equals(getString(R.string.filter_oldest)) ? "OLDEST" : "NEWEST";
    }

    private String categoryValue(String label) {
        if (label.equals(getString(R.string.filter_all_categories)) || label.trim().isEmpty()) {
            return "ALL";
        }
        return label.trim();
    }

    private String adminResponseValue(String label) {
        if (label.equals(getString(R.string.filter_with_response))) {
            return "WITH_RESPONSE";
        }
        if (label.equals(getString(R.string.filter_no_response))) {
            return "NO_RESPONSE";
        }
        return "ALL";
    }

    private String dateRangeValue(String label) {
        if (label.equals(getString(R.string.filter_last_7_days))) {
            return "7D";
        }
        if (label.equals(getString(R.string.filter_last_30_days))) {
            return "30D";
        }
        return "ALL";
    }
}
