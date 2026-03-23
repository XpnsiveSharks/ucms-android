package com.example.ucms_android.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.Notification;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.NotificationService;
import com.example.ucms_android.ui.adapter.NotificationAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private NotificationAdapter adapter;
    private NotificationService notificationService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        rvNotifications = findViewById(R.id.rvNotifications);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);
        View btnClose = findViewById(R.id.btnClose);
        View btnMarkAllRead = findViewById(R.id.btnMarkAllRead);

        notificationService = ApiClient.getInstance(this).create(NotificationService.class);

        adapter = new NotificationAdapter(new ArrayList<>(), notification -> {
            // Mark as read if unread
            if (!notification.isRead()) {
                notificationService.markAsRead(notification.getId()).enqueue(new Callback<ApiResponse<Notification>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Notification>> call,
                                           @NonNull Response<ApiResponse<Notification>> response) {}
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Notification>> call, @NonNull Throwable t) {}
                });
            }

            // Open ticket detail
            if (notification.getTicketId() != null) {
                Intent intent = new Intent(this, TicketDetailActivity.class);
                intent.putExtra("ticketId", notification.getTicketId());
                startActivity(intent);
            }
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        if (btnMarkAllRead != null) {
            btnMarkAllRead.setOnClickListener(v -> {
                notificationService.markAllAsRead().enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Void>> call,
                                           @NonNull Response<ApiResponse<Void>> response) {
                        loadNotifications();
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Void>> call, @NonNull Throwable t) {}
                });
            });
        }

        loadNotifications();
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvNotifications.setVisibility(View.GONE);

        notificationService.getNotifications().enqueue(new Callback<ApiResponse<List<Notification>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Notification>>> call,
                                   @NonNull Response<ApiResponse<List<Notification>>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null
                        && !response.body().getData().isEmpty()) {
                    adapter.updateData(response.body().getData());
                    rvNotifications.setVisibility(View.VISIBLE);
                } else {
                    tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Notification>>> call, @NonNull Throwable t) {
                progressBar.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }
}
