package com.example.ucms_android.ui.student;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
import com.example.ucms_android.model.TicketResponse;
import com.example.ucms_android.model.TimelineEvent;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.TicketService;
import com.example.ucms_android.ui.adapter.TimelineAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketDetailFragment extends Fragment {

    private ImageButton btnBack;
    private TextView tvTicketHeaderTitle, tvTicketHeaderCategory, tvDetailTitle, tvDetailDescription;
    private RecyclerView rvTimeline;
    private TimelineAdapter timelineAdapter;
    private TicketService ticketService;
    private Long ticketId;

    public static TicketDetailFragment newInstance(Long ticketId) {
        TicketDetailFragment fragment = new TicketDetailFragment();
        Bundle args = new Bundle();
        args.putLong("ticketId", ticketId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ticket_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            ticketId = getArguments().getLong("ticketId", -1);
        }

        if (ticketId == -1) {
            if (getActivity() != null) getActivity().onBackPressed();
            return;
        }

        bindViews(view);

        ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        rvTimeline.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadTicket();
    }

    private void bindViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        tvTicketHeaderTitle = view.findViewById(R.id.tvTicketHeaderTitle);
        tvTicketHeaderCategory = view.findViewById(R.id.tvTicketHeaderCategory);
        tvDetailTitle = view.findViewById(R.id.tvDetailTitle);
        tvDetailDescription = view.findViewById(R.id.tvDetailDescription);
        rvTimeline = view.findViewById(R.id.rvTimeline);
    }

    private void loadTicket() {
        ticketService.getTicketById(ticketId).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Ticket ticket = response.body().getData();
                    populateViews(ticket);
                    loadResponses(ticket);
                } else if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.error_loading_tickets), Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) getActivity().onBackPressed();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) getActivity().onBackPressed();
                }
            }
        });
    }

    private void populateViews(Ticket ticket) {
        tvTicketHeaderTitle.setText(ticket.getTicketNumber());
        tvTicketHeaderCategory.setText(ticket.getCategory());
        tvDetailTitle.setText(ticket.getTitle());
        tvDetailDescription.setText(ticket.getDescription());
    }

    private void loadResponses(Ticket ticket) {
        ticketService.getTicketResponses(ticketId).enqueue(new Callback<ApiResponse<List<TicketResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<TicketResponse>>> call, Response<ApiResponse<List<TicketResponse>>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<TicketResponse> responses = response.body().getData();
                    buildTimeline(ticket, responses);
                } else if (isAdded()) {
                    // If fetching responses fails, still show the initial creation event
                    buildTimeline(ticket, new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<TicketResponse>>> call, Throwable t) {
                if (isAdded()) {
                     buildTimeline(ticket, new ArrayList<>());
                }
            }
        });
    }

    private void buildTimeline(Ticket ticket, List<TicketResponse> responses) {
        List<TimelineEvent> events = new ArrayList<>();

        // 1. Initial Creation Event
        events.add(new TimelineEvent(ticket.getCreatedAt(), "Ticket Created", null, true));

        // 2. Add Responses to timeline
        for (TicketResponse res : responses) {
            String title = "Admin Response";
            boolean isCompleted = true; // Assume past responses are completed steps
            
            // Try to infer status change from response message (simplified logic for demo)
            if (res.getMessage() != null && res.getMessage().toLowerCase().contains("in-progress")) {
                 title = "Status changed to In-Progress";
            } else if (res.getMessage() != null && res.getMessage().toLowerCase().contains("resolved")) {
                 title = "Concern Resolved";
            }

            events.add(new TimelineEvent(res.getCreatedAt(), title, res.getMessage(), isCompleted));
        }

        // If the ticket is pending and has no responses, it's just created.
        // If it's in progress or resolved but has no explicit response detailing that, we might add a placeholder event,
        // but for now, rely on backend responses to build the timeline history.

        timelineAdapter = new TimelineAdapter(events);
        rvTimeline.setAdapter(timelineAdapter);
    }
}
