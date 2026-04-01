package com.example.ucms_android.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
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
import com.example.ucms_android.model.CategoryCount;
import com.example.ucms_android.model.Notification;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.model.User;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.NotificationService;
import com.example.ucms_android.network.TicketService;
import com.example.ucms_android.network.UserService;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.MainActivity;
import com.example.ucms_android.ui.common.AnalyticsFragment;
import com.example.ucms_android.ui.student.NotificationsActivity;
import com.example.ucms_android.ui.adapter.RecentTicketAdapter;
import com.example.ucms_android.ui.view.ThreeDBarView;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardFragment extends Fragment {

    private TextView tvTotalTickets;
    private TextView tvUnresolvedCount;
    private TextView tvUnresolvedTrend;
    private TextView tvPendingCount;
    private TextView tvResolvedCount;
    private ShimmerFrameLayout shimmerRecentTickets;
    private TextView tvEmptyRecent;
    private RecyclerView rvRecentTickets;
    private ViewGroup llCategoryChart;
    private RecentTicketAdapter adapter;
    private TicketService ticketService;
    private SessionManager sessionManager;
    private Gson gson;
    private TextView tvAvatarSmall;
    private View viewNotificationBadge;

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
        tvUnresolvedCount = view.findViewById(R.id.tvUnresolvedCount);
        tvUnresolvedTrend = view.findViewById(R.id.tvUnresolvedTrend);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvResolvedCount = view.findViewById(R.id.tvResolvedCount);
        rvRecentTickets = view.findViewById(R.id.rvRecentTickets);
        shimmerRecentTickets = view.findViewById(R.id.shimmerRecentTickets);
        tvEmptyRecent = view.findViewById(R.id.tvEmptyRecent);
        tvAvatarSmall = view.findViewById(R.id.tvAvatarSmall);
        viewNotificationBadge = view.findViewById(R.id.viewNotificationBadge);
        llCategoryChart = view.findViewById(R.id.llCategoryChart);

        View flNotification = view.findViewById(R.id.flNotification);
        if (flNotification != null) {
            flNotification.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), NotificationsActivity.class));
            });
        }

        view.findViewById(R.id.tvViewAll).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new AdminTicketListFragment());
                BottomNavigationView nav = getActivity().findViewById(R.id.bottomNavView);
                if (nav != null) nav.setSelectedItemId(R.id.nav_admin_tickets);
            }
        });

        view.findViewById(R.id.cardCategoryTrends).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new AnalyticsFragment());
                BottomNavigationView nav = getActivity().findViewById(R.id.bottomNavView);
                if (nav != null) nav.setSelectedItemId(R.id.nav_admin_analytics);
            }
        });

        ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);
        sessionManager = new SessionManager(requireContext());
        gson = new Gson();

        // Load cached initials
        String cachedName = sessionManager.getCachedName();
        if (!cachedName.isEmpty()) {
            if (tvAvatarSmall != null) tvAvatarSmall.setText(getInitials(cachedName));
        }

        // Fetch live user data for avatar
        UserService userService = ApiClient.getInstance(requireContext()).create(UserService.class);
        userService.getMe().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call, @NonNull Response<ApiResponse<User>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    String name = response.body().getData().getName();
                    if (name != null && tvAvatarSmall != null) {
                        tvAvatarSmall.setText(getInitials(name));
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {}
        });

        adapter = new RecentTicketAdapter(ticket -> {
            Intent intent = new Intent(requireActivity(), AdminTicketDetailActivity.class);
            intent.putExtra("ticketId", ticket.getId());
            startActivity(intent);
        });
        rvRecentTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvRecentTickets.setAdapter(adapter);

        loadFromCache();
        loadTickets();
        loadUnreadCount();
        fetchCategoryAnalytics();
    }

    private void fetchCategoryAnalytics() {
        ticketService.getAnalyticsByCategory().enqueue(new Callback<ApiResponse<List<CategoryCount>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<CategoryCount>>> call, @NonNull Response<ApiResponse<List<CategoryCount>>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    bindCategoryChart(response.body().getData());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<CategoryCount>>> call, @NonNull Throwable t) {}
        });
    }

    private void bindCategoryChart(List<CategoryCount> categories) {
        if (categories == null || categories.isEmpty() || llCategoryChart == null) return;

        llCategoryChart.removeAllViews();

        // Sort and take top 10 (or all if you really want ALL, but 10 is usually safe for scroll)
        List<CategoryCount> sorted = new ArrayList<>(categories);
        Collections.sort(sorted, (c1, c2) -> Long.compare(c2.getTicketCount(), c1.getTicketCount()));
        if (sorted.size() > 10) sorted = sorted.subList(0, 10);

        long maxCount = 0;
        for (CategoryCount v : sorted) {
            if (v.getTicketCount() > maxCount) maxCount = v.getTicketCount();
        }

        int maxBarHeightDp = 140;
        int[] barColors = {
            0xFFF77F00, // colorTrendOrange
            0xFFFCBF49, // yellow
            0xFF10B981, // green
            0xFF2196F3, // blue
            0xFF9C27B0, // purple
            0xFF56CCF2, // light blue
            0xFFBB6BD9  // lavender
        };

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View.OnClickListener goToAnalytics = v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new AnalyticsFragment());
                BottomNavigationView nav = getActivity().findViewById(R.id.bottomNavView);
                if (nav != null) nav.setSelectedItemId(R.id.nav_admin_analytics);
            }
        };

        for (int i = 0; i < sorted.size(); i++) {
            CategoryCount data = sorted.get(i);
            
            View barItem = inflater.inflate(R.layout.item_chart_bar, llCategoryChart, false);
            ThreeDBarView vBar = barItem.findViewById(R.id.vBar);
            TextView tvDay = barItem.findViewById(R.id.tvDay);

            if (vBar != null && tvDay != null) {
                ViewGroup.LayoutParams params = vBar.getLayoutParams();
                int heightDp = maxCount > 0 ? (int) (maxBarHeightDp * (data.getTicketCount() / (double) maxCount)) : 0;
                if (data.getTicketCount() > 0) heightDp = Math.max(heightDp, 15);

                params.height = dpToPx(heightDp);
                vBar.setLayoutParams(params);
                
                vBar.setBarColor(barColors[i % barColors.length]);
                
                String label = data.getCategoryName();
                if (label.length() > 6) label = label.substring(0, 6).toUpperCase();
                tvDay.setText(label);

                // Make individual bars clickable too just in case scrollview consumes parent touches
                barItem.setOnClickListener(goToAnalytics);
            }

            llCategoryChart.addView(barItem);
            
            // Add spacing between bars
            if (i < sorted.size() - 1) {
                View spacer = new View(requireContext());
                spacer.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(12), 1));
                llCategoryChart.addView(spacer);
            }
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "AD";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private void loadUnreadCount() {
        if (!isAdded()) return;
        NotificationService notificationService = ApiClient.getInstance(requireContext()).create(NotificationService.class);
        notificationService.getNotifications().enqueue(new Callback<ApiResponse<List<Notification>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Notification>>> call, @NonNull Response<ApiResponse<List<Notification>>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    long unread = response.body().getData().stream().filter(n -> !n.isRead()).count();
                    if (viewNotificationBadge != null) {
                        viewNotificationBadge.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Notification>>> call, @NonNull Throwable t) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTickets();
        loadUnreadCount();
        fetchCategoryAnalytics();
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
        int unresolved = 0;

        for (Ticket ticket : tickets) {
            String status = ticket.getStatus();
            if ("PENDING".equalsIgnoreCase(status)) {
                pending++;
                unresolved++;
            } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                unresolved++;
            } else if ("RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
                resolved++;
            } else {
                // Other statuses like REJECTED might still be considered unresolved until CLOSED
                unresolved++;
            }
        }

        tvTotalTickets.setText(String.valueOf(total));
        tvUnresolvedCount.setText(String.valueOf(unresolved));
        tvPendingCount.setText(String.valueOf(pending));
        tvResolvedCount.setText(String.valueOf(resolved));

        if (tvUnresolvedTrend != null) {
            if (total > 0) {
                int percentage = (int) ((unresolved / (double) total) * 100);
                tvUnresolvedTrend.setText(String.format(java.util.Locale.getDefault(), "%d%% of\nTotal", percentage));
            } else {
                tvUnresolvedTrend.setText("0% of\nTotal");
            }
        }

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
