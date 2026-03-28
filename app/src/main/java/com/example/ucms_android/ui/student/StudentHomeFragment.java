package com.example.ucms_android.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.MainActivity;
import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.Notification;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.model.User;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.NotificationService;
import com.example.ucms_android.network.TicketService;
import com.example.ucms_android.network.UserService;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.adapter.RecentTicketAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentHomeFragment extends Fragment {

    private SessionManager sessionManager;
    private Gson gson;
    private RecentTicketAdapter recentTicketAdapter;
    private RecyclerView rvRecentTickets;
    private TextView tvTotalTicketsCount, tvPendingCount, tvResolvedCount;
    private TextView tvUserName, tvAvatarSmall;
    private ShimmerFrameLayout shimmerRecentNotifications;
    private TextView tvEmptyRecentNotifications;
    private View viewNotificationBadge;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        gson = new Gson();

        tvTotalTicketsCount = view.findViewById(R.id.tvTotalTicketsCount);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvResolvedCount = view.findViewById(R.id.tvResolvedCount);
        viewNotificationBadge = view.findViewById(R.id.viewNotificationBadge);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvAvatarSmall = view.findViewById(R.id.tvAvatarSmall);
        
        View flNotificationContainer = view.findViewById(R.id.flNotificationContainer);
        if (flNotificationContainer != null) {
            flNotificationContainer.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), NotificationsActivity.class));
            });
        }

        // Show cached stats immediately
        int cachedTotal = sessionManager.getCachedTotalTickets();
        int cachedPending = sessionManager.getCachedPendingCount();
        int cachedResolved = sessionManager.getCachedResolvedToday();
        if (cachedTotal >= 0) tvTotalTicketsCount.setText(String.valueOf(cachedTotal));
        if (cachedPending >= 0) tvPendingCount.setText(String.valueOf(cachedPending));
        if (cachedResolved >= 0) tvResolvedCount.setText(String.valueOf(cachedResolved));

        // Submit button
        view.findViewById(R.id.btnSubmitNewConcern).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new SubmitTicketFragment());
                com.google.android.material.bottomnavigation.BottomNavigationView nav =
                        getActivity().findViewById(R.id.bottomNavView);
                if (nav != null) nav.setSelectedItemId(R.id.nav_student_add);
            }
        });

        // View All
        view.findViewById(R.id.tvViewAll).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new TicketListFragment());
                com.google.android.material.bottomnavigation.BottomNavigationView nav =
                        getActivity().findViewById(R.id.bottomNavView);
                if (nav != null) nav.setSelectedItemId(R.id.nav_student_tickets);
            }
        });

        // User name & Avatar initials from cache
        String cachedName = sessionManager.getCachedName();
        if (!cachedName.isEmpty()) {
            if (tvUserName != null) tvUserName.setText(cachedName);
            if (tvAvatarSmall != null) tvAvatarSmall.setText(getInitials(cachedName));
        }

        UserService userService = ApiClient.getInstance(requireContext()).create(UserService.class);
        userService.getMe().enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<User>> call,
                                   @NonNull Response<ApiResponse<User>> response) {
                if (!isAdded() || !response.isSuccessful()
                        || response.body() == null || response.body().getData() == null) return;
                User user = response.body().getData();
                String name = user.getName();
                if (name != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (tvUserName != null) tvUserName.setText(name);
                        if (tvAvatarSmall != null) tvAvatarSmall.setText(getInitials(name));
                    });
                    sessionManager.saveProfileCache(user.getName(), user.getStudentId(),
                            user.getCourse(), user.getYearLevel());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<User>> call, @NonNull Throwable t) {}
        });

        // Recent tickets RecyclerView
        rvRecentTickets = view.findViewById(R.id.rvRecentNotifications);
        rvRecentTickets.setLayoutManager(new LinearLayoutManager(getContext()));
        recentTicketAdapter = new RecentTicketAdapter(ticket -> {
            android.content.Intent intent = new android.content.Intent(requireContext(),
                    com.example.ucms_android.ui.student.TicketDetailActivity.class);
            intent.putExtra("ticketId", ticket.getId());
            startActivity(intent);
        });
        rvRecentTickets.setAdapter(recentTicketAdapter);

        shimmerRecentNotifications = view.findViewById(R.id.shimmerRecentNotifications);
        tvEmptyRecentNotifications = view.findViewById(R.id.tvEmptyRecentNotifications);

        loadFromCache();
        loadTickets();
        loadUnreadCount();
    }

    private String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "??";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTickets();
        loadUnreadCount();
    }

    private void loadUnreadCount() {
        if (!isAdded()) return;
        NotificationService notificationService = ApiClient.getInstance(requireContext())
                .create(NotificationService.class);
        notificationService.getNotifications().enqueue(new Callback<ApiResponse<List<Notification>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Notification>>> call,
                                   @NonNull Response<ApiResponse<List<Notification>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    long unread = response.body().getData().stream()
                            .filter(n -> !n.isRead()).count();
                    requireActivity().runOnUiThread(() -> {
                        if (viewNotificationBadge != null) {
                            viewNotificationBadge.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Notification>>> call, @NonNull Throwable t) {}
        });
    }

    private void showRecentState(String state) {
        shimmerRecentNotifications.setVisibility(View.GONE);
        shimmerRecentNotifications.stopShimmer();
        tvEmptyRecentNotifications.setVisibility(View.GONE);
        rvRecentTickets.setVisibility(View.GONE);

        switch (state) {
            case "LOADING":
                shimmerRecentNotifications.setVisibility(View.VISIBLE);
                shimmerRecentNotifications.startShimmer();
                break;
            case "EMPTY":
                tvEmptyRecentNotifications.setVisibility(View.VISIBLE);
                break;
            case "DATA":
                rvRecentTickets.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void loadFromCache() {
        String cachedJson = sessionManager.getStudentRecentTicketsJson();
        if (cachedJson != null) {
            Type type = new TypeToken<List<Ticket>>(){}.getType();
            List<Ticket> cached = gson.fromJson(cachedJson, type);
            if (cached != null && !cached.isEmpty()) {
                recentTicketAdapter.updateTickets(cached);
                showRecentState("DATA");
                return;
            }
        }
        showRecentState("LOADING");
    }

    private void loadTickets() {
        if (!isAdded()) return;
        TicketService ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);
        ticketService.getTickets(null).enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Ticket>>> call,
                                   @NonNull Response<ApiResponse<List<Ticket>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    List<Ticket> all = response.body().getData();
                    updateStats(all);
                    List<Ticket> recent = all.size() > 3 ? all.subList(0, 3) : all;
                    recentTicketAdapter.updateTickets(recent);
                    sessionManager.saveStudentRecentTicketsJson(gson.toJson(recent));
                    showRecentState(recent.isEmpty() ? "EMPTY" : "DATA");
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

    private void updateStats(List<Ticket> tickets) {
        int total = tickets.size();
        int pending = 0;
        int resolvedToday = 0;
        String today = LocalDate.now().toString();

        for (Ticket ticket : tickets) {
            String status = ticket.getStatus();
            if ("PENDING".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status)) {
                pending++;
            }
            if ("RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
                String updatedAt = ticket.getUpdatedAt();
                if (updatedAt != null && updatedAt.startsWith(today)) {
                    resolvedToday++;
                }
            }
        }

        tvTotalTicketsCount.setText(String.valueOf(total));
        tvPendingCount.setText(String.valueOf(pending));
        tvResolvedCount.setText(String.valueOf(resolvedToday));

        sessionManager.saveTicketStatsCache(total, pending, resolvedToday);
    }
}
