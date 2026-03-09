package com.example.ucms_android.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class TicketListFragment extends Fragment {

    private TextView tvEmptyState;
    private RecyclerView rvTickets;
    private TextView tabAll, tabPending, tabInProgress;
    private EditText etSearch;
    private TicketAdapter adapter;
    private TicketService ticketService;
    
    private List<Ticket> allTickets = new ArrayList<>();
    private String currentFilter = "ALL";
    private String currentQuery = "";

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
        
        etSearch = view.findViewById(R.id.etSearch);
        tabAll = view.findViewById(R.id.tabAll);
        tabPending = view.findViewById(R.id.tabPending);
        tabInProgress = view.findViewById(R.id.tabInProgress);

        ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);

        adapter = new TicketAdapter(new ArrayList<>(), ticket -> {
            if (getActivity() instanceof com.example.ucms_android.MainActivity) {
                ((com.example.ucms_android.MainActivity) getActivity()).loadFragment(TicketDetailFragment.newInstance(ticket.getId()));
            }
        });
        rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTickets.setAdapter(adapter);

        setupSearchAndFilters();
        loadTickets();
    }
    
    private void setupSearchAndFilters() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().toLowerCase();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        tabAll.setOnClickListener(v -> {
            currentFilter = "ALL";
            updateTabUI(tabAll);
            applyFilters();
        });

        tabPending.setOnClickListener(v -> {
            currentFilter = "PENDING";
            updateTabUI(tabPending);
            applyFilters();
        });

        tabInProgress.setOnClickListener(v -> {
            currentFilter = "IN_PROGRESS";
            updateTabUI(tabInProgress);
            applyFilters();
        });
        
        updateTabUI(tabAll);
    }
    
    private void updateTabUI(TextView activeTab) {
        tabAll.setBackgroundResource(R.drawable.bg_chip_unselected);
        tabPending.setBackgroundResource(R.drawable.bg_chip_unselected);
        tabInProgress.setBackgroundResource(R.drawable.bg_chip_unselected);
        
        tabAll.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        tabPending.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        tabInProgress.setTextColor(getResources().getColor(R.color.colorTextSecondary));

        if (activeTab == tabAll) {
            tabAll.setBackgroundResource(R.drawable.bg_chip_selected); // Need to create this
            tabAll.setTextColor(getResources().getColor(R.color.black));
        } else if (activeTab == tabPending) {
            tabPending.setBackgroundResource(R.drawable.bg_chip_pending);
            tabPending.setTextColor(getResources().getColor(R.color.white));
        } else if (activeTab == tabInProgress) {
            tabInProgress.setBackgroundResource(R.drawable.bg_chip_inprogress);
            tabInProgress.setTextColor(getResources().getColor(R.color.black));
        }
    }
    
    private void applyFilters() {
        List<Ticket> filteredList = new ArrayList<>();
        for (Ticket ticket : allTickets) {
            boolean matchesFilter = currentFilter.equals("ALL") || 
                                    ticket.getStatus().equalsIgnoreCase(currentFilter);
            boolean matchesSearch = ticket.getTitle().toLowerCase().contains(currentQuery) || 
                                    ticket.getTicketNumber().toLowerCase().contains(currentQuery);
                                    
            if (matchesFilter && matchesSearch) {
                filteredList.add(ticket);
            }
        }
        
        adapter.updateData(filteredList);
        
        if (filteredList.isEmpty()) {
            rvTickets.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            rvTickets.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTickets();
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
                        applyFilters();
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
}
