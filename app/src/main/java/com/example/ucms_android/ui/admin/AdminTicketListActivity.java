package com.example.ucms_android.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.TicketService;
import com.example.ucms_android.ui.adapter.TicketAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminTicketListActivity extends AppCompatActivity {

    private TextInputEditText etSearch;
    private Chip chipNeedsAction, chipInProgress, chipAll;
    private RecyclerView rvTickets;
    private TicketAdapter adapter;
    private TicketService ticketService;

    private List<Ticket> allTickets = new ArrayList<>();
    private String activeFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ticket_list);

        etSearch = findViewById(R.id.etSearch);
        chipNeedsAction = findViewById(R.id.chipNeedsAction);
        chipInProgress = findViewById(R.id.chipInProgress);
        chipAll = findViewById(R.id.chipAll);
        rvTickets = findViewById(R.id.rvTickets);

        ticketService = ApiClient.getInstance(this).create(TicketService.class);

        adapter = new TicketAdapter(new ArrayList<>(), ticket -> {
            Intent intent = new Intent(this, AdminTicketDetailActivity.class);
            intent.putExtra("ticketId", ticket.getId());
            startActivity(intent);
        });
        rvTickets.setLayoutManager(new LinearLayoutManager(this));
        rvTickets.setAdapter(adapter);

        setupChips();
        setupSearch();
        loadTickets();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTickets();
    }

    private void setupChips() {
        chipAll.setOnClickListener(v -> { activeFilter = "ALL"; applyFilter(); });
        chipNeedsAction.setOnClickListener(v -> { activeFilter = "PENDING"; applyFilter(); });
        chipInProgress.setOnClickListener(v -> { activeFilter = "IN_PROGRESS"; applyFilter(); });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadTickets() {
        ticketService.getTickets().enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(Call<List<Ticket>> call, Response<List<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allTickets = response.body();
                    applyFilter();
                } else {
                    Toast.makeText(AdminTicketListActivity.this,
                            getString(R.string.error_loading_tickets), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Ticket>> call, Throwable t) {
                Toast.makeText(AdminTicketListActivity.this,
                        getString(R.string.error_network), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilter() {
        String query = etSearch.getText() != null ? etSearch.getText().toString().toLowerCase().trim() : "";
        List<Ticket> filtered = new ArrayList<>();

        for (Ticket ticket : allTickets) {
            boolean matchesFilter = activeFilter.equals("ALL") ||
                    activeFilter.equalsIgnoreCase(ticket.getStatus());
            boolean matchesSearch = query.isEmpty() ||
                    (ticket.getTitle() != null && ticket.getTitle().toLowerCase().contains(query)) ||
                    (ticket.getTicketNumber() != null && ticket.getTicketNumber().toLowerCase().contains(query));

            if (matchesFilter && matchesSearch) filtered.add(ticket);
        }

        adapter.updateData(filtered);
    }
}
