package com.example.ucms_android.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.TicketService;
import com.example.ucms_android.ui.adapter.TicketAdapter;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketListActivity extends AppCompatActivity {

    private TextView tvEmptyState;
    private RecyclerView rvTickets;
    private MaterialButton btnSubmitTicket;
    private TicketAdapter adapter;
    private TicketService ticketService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_list);

        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvTickets = findViewById(R.id.rvTickets);
        btnSubmitTicket = findViewById(R.id.btnSubmitTicket);

        ticketService = ApiClient.getInstance(this).create(TicketService.class);

        adapter = new TicketAdapter(new ArrayList<>(), ticket -> {
            Intent intent = new Intent(this, TicketDetailActivity.class);
            intent.putExtra("ticketId", ticket.getId());
            startActivity(intent);
        });
        rvTickets.setLayoutManager(new LinearLayoutManager(this));
        rvTickets.setAdapter(adapter);

        btnSubmitTicket.setOnClickListener(v ->
                startActivity(new Intent(this, SubmitTicketActivity.class)));

        loadTickets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTickets();
    }

    private void loadTickets() {
        ticketService.getTickets(null).enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Ticket>>> call, Response<ApiResponse<List<Ticket>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Ticket> tickets = response.body().getData();
                    if (tickets.isEmpty()) {
                        rvTickets.setVisibility(View.GONE);
                        tvEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvTickets.setVisibility(View.VISIBLE);
                        tvEmptyState.setVisibility(View.GONE);
                        adapter.updateData(tickets);
                    }
                } else {
                    Toast.makeText(TicketListActivity.this,
                            getString(R.string.error_loading_tickets), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Ticket>>> call, Throwable t) {
                Toast.makeText(TicketListActivity.this,
                        getString(R.string.error_network), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
