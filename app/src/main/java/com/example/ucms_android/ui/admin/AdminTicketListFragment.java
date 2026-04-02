package com.example.ucms_android.ui.admin;

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
import com.example.ucms_android.util.StatusChipHelper;
import com.facebook.shimmer.ShimmerFrameLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminTicketListFragment extends Fragment {

    private TextView tvTabAll, tvTabInProgress, tvTabResolved;
    private View tabIndicator;
    private RecyclerView rvTickets;
    private ShimmerFrameLayout shimmerLayout;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmptyState;
    private LinearLayout layoutError;
    private TicketAdapter adapter;
    private TicketService ticketService;
    private SessionManager sessionManager;
    private Gson gson;

    private List<Ticket> allTickets = new ArrayList<>();
    private String activeFilter = "ALL";

    private String statusFilter = "ALL";
    private String sortFilter = "NEWEST";
    private String categoryFilter = "ALL";
    private String adminResponseFilter = "ALL";
    private String dateRangeFilter = "ALL";
    private final SyncUpdateBus.Listener syncListener = domain -> {
        if (SyncUpdateBus.DOMAIN_TICKETS.equals(domain) && isAdded()) {
            refreshFromCache();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_ticket_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTabAll = view.findViewById(R.id.tvTabAll);
        tvTabInProgress = view.findViewById(R.id.tvTabInProgress);
        tvTabResolved = view.findViewById(R.id.tvTabResolved);
        tabIndicator = view.findViewById(R.id.tabIndicator);
        
        rvTickets = view.findViewById(R.id.rvTickets);
        shimmerLayout = view.findViewById(R.id.shimmerLayout);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        layoutError = view.findViewById(R.id.layoutError);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);

        view.findViewById(R.id.btnRetry).setOnClickListener(v -> loadTickets());
        view.findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());

        swipeRefresh.setOnRefreshListener(() -> {
            swipeRefresh.setRefreshing(false);
            showState("LOADING");
            loadTickets();
        });

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

        boolean hasCached = loadFromCache();
        setupTabs();
        if (!hasCached) {
            loadTickets();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SyncUpdateBus.getInstance().register(syncListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        SyncUpdateBus.getInstance().unregister(syncListener);
    }

    private void setupTabs() {
        tvTabAll.setOnClickListener(v -> {
            activeFilter = "ALL";
            updateTabs();
            applyFilter();
        });
        tvTabInProgress.setOnClickListener(v -> {
            activeFilter = "IN_PROGRESS";
            updateTabs();
            applyFilter();
        });
        tvTabResolved.setOnClickListener(v -> {
            activeFilter = "RESOLVED";
            updateTabs();
            applyFilter();
        });
        
        updateTabs();
    }

    private void updateTabs() {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(R.attr.colorHomeSubmitButton, typedValue, true);
        int colorSelected = typedValue.data;
        int colorUnselected = getResources().getColor(R.color.colorTextSecondary, null);

        tvTabAll.setTextColor(activeFilter.equals("ALL") ? colorSelected : colorUnselected);
        tvTabInProgress.setTextColor(activeFilter.equals("IN_PROGRESS") ? colorSelected : colorUnselected);
        tvTabResolved.setTextColor(activeFilter.equals("RESOLVED") ? colorSelected : colorUnselected);

        if (tabIndicator != null) {
            tabIndicator.post(() -> {
                View activeView = activeFilter.equals("ALL") ? tvTabAll : 
                                 activeFilter.equals("IN_PROGRESS") ? tvTabInProgress : tvTabResolved;
                tabIndicator.animate()
                        .x(activeView.getLeft())
                        .setDuration(200)
                        .start();
                ViewGroup.LayoutParams lp = tabIndicator.getLayoutParams();
                lp.width = activeView.getWidth();
                tabIndicator.setLayoutParams(lp);
            });
        }
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

            applyFilter();
            dialog.dismiss();
        });

        btnApplyFilters.setOnClickListener(v -> {
            statusFilter = statusValue(dropStatus.getText() != null ? dropStatus.getText().toString() : "");
            sortFilter = sortValue(dropSortBy.getText() != null ? dropSortBy.getText().toString() : "");
            categoryFilter = categoryValue(dropCategory.getText() != null ? dropCategory.getText().toString() : "");
            adminResponseFilter = adminResponseValue(dropAdminResponse.getText() != null ? dropAdminResponse.getText().toString() : "");
            dateRangeFilter = dateRangeValue(dropDateRange.getText() != null ? dropDateRange.getText().toString() : "");

            applyFilter();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showState(String state) {
        shimmerLayout.setVisibility(View.GONE);
        shimmerLayout.stopShimmer();
        tvEmptyState.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        rvTickets.setVisibility(View.GONE);

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
        String cachedJson = sessionManager.getAdminAllTicketsJson();
        if (cachedJson != null) {
            Type type = new TypeToken<List<Ticket>>() {}.getType();
            List<Ticket> cached = gson.fromJson(cachedJson, type);
            if (cached != null && !cached.isEmpty()) {
                allTickets = cached;
                applyFilter();
                return true;
            }
        }
        showState("LOADING");
        return false;
    }

    /** Called by syncListener — cache already updated by BootstrapCoordinator, just re-bind. */
    private void refreshFromCache() {
        String cachedJson = sessionManager.getAdminAllTicketsJson();
        if (cachedJson == null) return;
        Type type = new TypeToken<List<Ticket>>() {}.getType();
        List<Ticket> cached = gson.fromJson(cachedJson, type);
        if (cached != null) {
            allTickets = cached;
            applyFilter();
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
        List<Ticket> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Ticket ticket : allTickets) {
            boolean matchesTab = activeFilter.equals("ALL") || activeFilter.equalsIgnoreCase(ticket.getStatus());
            if (!matchesTab) continue;

            if (!"ALL".equals(statusFilter) && !statusFilter.equalsIgnoreCase(ticket.getStatus())) continue;
            if (!"ALL".equals(categoryFilter) && !categoryFilter.equalsIgnoreCase(ticket.getCategoryName())) continue;
            if ("WITH_RESPONSE".equals(adminResponseFilter) && !ticket.hasAdminResponse()) continue;
            if ("NO_RESPONSE".equals(adminResponseFilter) && ticket.hasAdminResponse()) continue;

            if (!"ALL".equals(dateRangeFilter)) {
                long ts = ticketTimestamp(ticket);
                if (ts > 0) {
                    long diff = now - ts;
                    if ("7D".equals(dateRangeFilter) && diff > 7L * 24L * 60L * 60L * 1000L) continue;
                    if ("30D".equals(dateRangeFilter) && diff > 30L * 24L * 60L * 60L * 1000L) continue;
                }
            }

            filtered.add(ticket);
        }

        Comparator<Ticket> comparator = Comparator.comparingLong(this::ticketTimestamp);
        if (!"OLDEST".equals(sortFilter)) {
            comparator = comparator.reversed();
        }
        filtered.sort(comparator);

        adapter.updateData(filtered);
        showState(filtered.isEmpty() ? "EMPTY" : "DATA");
    }

    private int priorityRank(Ticket ticket) {
        if (ticket == null) return 5;
        String priority = StatusChipHelper.resolvePriorityLevel(ticket);
        if ("CRITICAL".equals(priority)) return 0;
        if ("HIGH".equals(priority)) return 1;
        if ("LOW".equals(priority)) return 2;
        if ("MUTED".equals(priority)) return 3;
        return 4;
    }

    private long ticketTimestamp(Ticket ticket) {
        String raw = ticket.getCreatedAt();
        if (raw == null || raw.trim().isEmpty()) return 0L;
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
        if ("PENDING".equals(value)) return getString(R.string.filter_pending);
        if ("IN_PROGRESS".equals(value)) return getString(R.string.filter_in_progress);
        if ("RESOLVED".equals(value)) return getString(R.string.filter_resolved);
        if ("CLOSED".equals(value)) return getString(R.string.filter_closed);
        return getString(R.string.filter_all_statuses);
    }

    private String sortLabel(String value) {
        return "OLDEST".equals(value) ? getString(R.string.filter_oldest) : getString(R.string.filter_newest);
    }

    private String categoryLabel(String value) {
        return "ALL".equals(value) ? getString(R.string.filter_all_categories) : value;
    }

    private String adminResponseLabel(String value) {
        if ("WITH_RESPONSE".equals(value)) return getString(R.string.filter_with_response);
        if ("NO_RESPONSE".equals(value)) return getString(R.string.filter_no_response);
        return getString(R.string.filter_all_responses);
    }

    private String dateRangeLabel(String value) {
        if ("7D".equals(value)) return getString(R.string.filter_last_7_days);
        if ("30D".equals(value)) return getString(R.string.filter_last_30_days);
        return getString(R.string.filter_all_time);
    }

    private String statusValue(String label) {
        if (label.equals(getString(R.string.filter_pending))) return "PENDING";
        if (label.equals(getString(R.string.filter_in_progress))) return "IN_PROGRESS";
        if (label.equals(getString(R.string.filter_resolved))) return "RESOLVED";
        if (label.equals(getString(R.string.filter_closed))) return "CLOSED";
        return "ALL";
    }

    private String sortValue(String label) {
        return label.equals(getString(R.string.filter_oldest)) ? "OLDEST" : "NEWEST";
    }

    private String categoryValue(String label) {
        if (label.equals(getString(R.string.filter_all_categories)) || label.trim().isEmpty()) return "ALL";
        return label.trim();
    }

    private String adminResponseValue(String label) {
        if (label.equals(getString(R.string.filter_with_response))) return "WITH_RESPONSE";
        if (label.equals(getString(R.string.filter_no_response))) return "NO_RESPONSE";
        return "ALL";
    }

    private String dateRangeValue(String label) {
        if (label.equals(getString(R.string.filter_last_7_days))) return "7D";
        if (label.equals(getString(R.string.filter_last_30_days))) return "30D";
        return "ALL";
    }
}
