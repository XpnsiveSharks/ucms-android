package com.example.ucms_android.ui.student;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.TicketService;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.adapter.TicketAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketListFragment extends Fragment {

    private TextView tvEmptyState;
    private RecyclerView rvTickets;
    private MaterialButton btnSubmitTicket;
    private ShimmerFrameLayout shimmerLayout;
    private LinearLayout layoutError;
    private TicketAdapter adapter;
    private TicketService ticketService;
    private SessionManager sessionManager;
    private Gson gson;

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
        shimmerLayout = view.findViewById(R.id.shimmerLayout);
        layoutError = view.findViewById(R.id.layoutError);
        view.findViewById(R.id.btnRetry).setOnClickListener(v -> loadTickets());

        ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);
        sessionManager = new SessionManager(requireContext());
        gson = new Gson();

        adapter = new TicketAdapter(new ArrayList<>(), ticket -> {
            Intent intent = new Intent(requireActivity(), TicketDetailActivity.class);
            intent.putExtra("ticketId", ticket.getId());
            startActivity(intent);
        });
        rvTickets.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvTickets.setAdapter(adapter);

        btnSubmitTicket.setOnClickListener(v -> {
            if (getActivity() instanceof com.example.ucms_android.MainActivity) {
                ((com.example.ucms_android.MainActivity) getActivity()).loadFragment(new SubmitTicketFragment());
                // Update bottom nav selection
                com.google.android.material.bottomnavigation.BottomNavigationView nav = getActivity().findViewById(R.id.bottomNavView);
                if (nav != null) nav.setSelectedItemId(R.id.nav_student_add);
            }
        });

        loadFromCache();
        loadTickets();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadTickets();
    }

    private void showState(String state) {
        shimmerLayout.setVisibility(View.GONE);
        shimmerLayout.stopShimmer();
        layoutError.setVisibility(View.GONE);
        rvTickets.setVisibility(View.GONE);
        tvEmptyState.setVisibility(View.GONE);

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

    private void loadFromCache() {
        String cachedJson = sessionManager.getStudentAllTicketsJson();
        if (cachedJson != null) {
            Type type = new TypeToken<List<Ticket>>() {}.getType();
            List<Ticket> cached = gson.fromJson(cachedJson, type);
            if (cached != null && !cached.isEmpty()) {
                adapter.updateData(cached);
                showState("DATA");
                return;
            }
        }
        showState("LOADING");
    }

    private void loadTickets() {
        ticketService.getTickets(null).enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Ticket>>> call,
                                   @NonNull Response<ApiResponse<List<Ticket>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Ticket> tickets = response.body().getData();
                    sessionManager.saveStudentAllTicketsJson(gson.toJson(tickets));
                    if (tickets.isEmpty()) {
                        showState("EMPTY");
                    } else {
                        adapter.updateData(tickets);
                        showState("DATA");
                    }
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
}
