package com.example.ucms_android.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.ucms_android.ui.adapter.TicketAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminTicketListFragment extends Fragment {

    private TextInputEditText etSearch;
    private Chip chipNeedsAction;
    private Chip chipInProgress;
    private Chip chipAll;
    private RecyclerView rvTickets;
    private TicketAdapter adapter;
    private TicketService ticketService;

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
        chipNeedsAction = view.findViewById(R.id.chipNeedsAction);
        chipInProgress = view.findViewById(R.id.chipInProgress);
        chipAll = view.findViewById(R.id.chipAll);
        rvTickets = view.findViewById(R.id.rvTickets);

        ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);

        adapter = new TicketAdapter(new ArrayList<>(), ticket -> {
            Intent intent = new Intent(requireActivity(), AdminTicketDetailActivity.class);
            intent.putExtra("ticketId", ticket.getId());
            startActivity(intent);
        });
        rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTickets.setAdapter(adapter);

        setupChips();
        setupSearch();
        loadTickets();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTickets();
    }

    private void setupChips() {
        chipAll.setOnClickListener(v -> {
            activeFilter = "ALL";
            applyFilter();
        });
        chipNeedsAction.setOnClickListener(v -> {
            activeFilter = "PENDING";
            applyFilter();
        });
        chipInProgress.setOnClickListener(v -> {
            activeFilter = "IN_PROGRESS";
            applyFilter();
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void loadTickets() {
        ticketService.getTickets(null).enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Ticket>>> call,
                                   @NonNull Response<ApiResponse<List<Ticket>>> response) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        allTickets = response.body().getData();
                        applyFilter();
                    } else {
                        Toast.makeText(requireContext(),
                                getString(R.string.error_loading_tickets),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Ticket>>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(),
                                getString(R.string.error_network),
                                Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void applyFilter() {
        String query = etSearch.getText() != null
                ? etSearch.getText().toString().toLowerCase().trim() : "";
        List<Ticket> filtered = new ArrayList<>();

        for (Ticket ticket : allTickets) {
            boolean matchesFilter = activeFilter.equals("ALL")
                    || activeFilter.equalsIgnoreCase(ticket.getStatus());
            boolean matchesSearch = query.isEmpty()
                    || (ticket.getTitle() != null && ticket.getTitle().toLowerCase().contains(query))
                    || (ticket.getTicketNumber() != null
                    && ticket.getTicketNumber().toLowerCase().contains(query));

            if (matchesFilter && matchesSearch) {
                filtered.add(ticket);
            }
        }

        adapter.updateData(filtered);
    }
}
