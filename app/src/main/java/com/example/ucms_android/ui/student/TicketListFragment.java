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

public class TicketListFragment extends Fragment {

    private TextView tvEmptyState;
    private RecyclerView rvTickets;
    private MaterialButton btnSubmitTicket;
    private TicketAdapter adapter;
    private TicketService ticketService;

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
        btnSubmitTicket = view.findViewById(R.id.btnSubmitTicket);

        ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);

        adapter = new TicketAdapter(new ArrayList<>(), ticket -> {
            Intent intent = new Intent(requireActivity(), TicketDetailActivity.class);
            intent.putExtra("ticketId", ticket.getId());
            startActivity(intent);
        });
        rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTickets.setAdapter(adapter);

        btnSubmitTicket.setOnClickListener(v ->
                startActivity(new Intent(requireActivity(), SubmitTicketActivity.class)));

        loadTickets();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTickets();
    }

    private void loadTickets() {
        ticketService.getTickets().enqueue(new Callback<List<Ticket>>() {
            @Override
            public void onResponse(@NonNull Call<List<Ticket>> call,
                                   @NonNull Response<List<Ticket>> response) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Ticket> tickets = response.body();
                        if (tickets.isEmpty()) {
                            rvTickets.setVisibility(View.GONE);
                            tvEmptyState.setVisibility(View.VISIBLE);
                        } else {
                            rvTickets.setVisibility(View.VISIBLE);
                            tvEmptyState.setVisibility(View.GONE);
                            adapter.updateData(tickets);
                        }
                    } else {
                        Toast.makeText(requireContext(),
                                getString(R.string.error_loading_tickets),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<List<Ticket>> call, @NonNull Throwable t) {
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
