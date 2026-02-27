package com.example.ucms_android.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.TicketService;
import com.example.ucms_android.ui.adapter.RecentTicketAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalTickets, tvPendingCount, tvResolvedCount, tvViewDatabase;
    private RecyclerView rvRecentTickets;
    private RecentTicketAdapter adapter;
    private TicketService ticketService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvTotalTickets = findViewById(R.id.tvTotalTickets);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvResolvedCount = findViewById(R.id.tvResolvedCount);
        tvViewDatabase = findViewById(R.id.tvViewDatabase);
        rvRecentTickets = findViewById(R.id.rvRecentTickets);

        ticketService = ApiClient.getInstance(this).create(TicketService.class);

        adapter = new RecentTicketAdapter(new ArrayList<>(), ticket -> {
            Intent intent = new Intent(this, AdminTicketDetailActivity.class);
            intent.putExtra("ticketId", ticket.getId());
            startActivity(intent);
        });
        rvRecentTickets.setLayoutManager(new LinearLayoutManager(this));
        rvRecentTickets.setAdapter(adapter);

        tvViewDatabase.setOnClickListener(v ->
                startActivity(new Intent(this, AdminTicketListActivity.class)));

        loadTickets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTickets();
    }

    private void loadTickets() {
        ticketService.getTickets().enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Ticket> tickets = response.body();
                    updateStats(tickets);
                    updateRecentList(tickets);
                } else {
                    Toast.makeText(AdminDashboardActivity.this,
                            getString(R.string.error_loading_tickets), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                Toast.makeText(AdminDashboardActivity.this,
                        getString(R.string.error_network), Toast.LENGTH_SHORT).show();
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
