package com.example.ucms_android.ui.student;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.AttachmentResponse;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.model.TicketResponse;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.TicketService;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.adapter.TimelineAdapter;
import com.example.ucms_android.util.DateFormatter;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketDetailActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvTicketNumber, tvCategoryName, tvTicketTitle, tvDescription, tvAttachmentName;
    private ImageView ivAttachmentPreview;
    private MaterialCardView cvAttachment;
    private LinearLayout layoutAttachmentContent;
    private ShimmerFrameLayout shimmerAttachment;
    private ShimmerFrameLayout shimmerTimeline;
    private RecyclerView rvTimeline;
    private TimelineAdapter timelineAdapter;
    private ProgressBar progressBar;
    private LinearLayout layoutError;
    private ScrollView scrollContent;
    private com.google.android.material.button.MaterialButton btnCloseTicket;
    private TicketService ticketService;
    private Long ticketId;
    private SessionManager sessionManager;
    private Gson gson;
    private Ticket currentTicket;
    private List<TicketResponse> currentResponses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_detail);

        ticketId = getIntent().getLongExtra("ticketId", -1);
        if (ticketId == -1) { finish(); return; }

        bindViews();

        ticketService = ApiClient.getInstance(this).create(TicketService.class);
        sessionManager = new SessionManager(this);
        gson = new Gson();

        btnBack.setOnClickListener(v -> finish());
        btnCloseTicket.setOnClickListener(v -> confirmCloseTicket());
        findViewById(R.id.btnRetry).setOnClickListener(v -> {
            loadTicket();
            loadResponses();
        });

        timelineAdapter = new TimelineAdapter(new ArrayList<>());
        rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        rvTimeline.setAdapter(timelineAdapter);

        loadFromCache();
        loadTicket();
        loadResponses();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTicketNumber = findViewById(R.id.tvTicketNumber);
        tvCategoryName = findViewById(R.id.tvCategoryName);
        tvTicketTitle = findViewById(R.id.tvTicketTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvAttachmentName = findViewById(R.id.tvAttachmentName);
        ivAttachmentPreview = findViewById(R.id.ivAttachmentPreview);
        cvAttachment = findViewById(R.id.cvAttachment);
        layoutAttachmentContent = findViewById(R.id.layoutAttachmentContent);
        shimmerAttachment = findViewById(R.id.shimmerAttachment);
        shimmerTimeline = findViewById(R.id.shimmerTimeline);
        rvTimeline = findViewById(R.id.rvTimeline);
        progressBar = findViewById(R.id.progressBar);
        layoutError = findViewById(R.id.layoutError);
        scrollContent = findViewById(R.id.scrollContent);
        btnCloseTicket = findViewById(R.id.btnCloseTicket);
    }

    private void showTimelineShimmer() {
        shimmerTimeline.setVisibility(View.VISIBLE);
        shimmerTimeline.startShimmer();
        rvTimeline.setVisibility(View.GONE);
    }

    private void hideTimelineShimmer() {
        shimmerTimeline.stopShimmer();
        shimmerTimeline.setVisibility(View.GONE);
        rvTimeline.setVisibility(View.VISIBLE);
    }

    private void loadFromCache() {
        String cachedJson = sessionManager.getTicketDetailJson(ticketId);
        if (cachedJson != null) {
            Ticket cached = gson.fromJson(cachedJson, Ticket.class);
            if (cached != null) {
                currentTicket = cached;
                populateViews(cached);
                showState("DATA");

                String cachedResponses = sessionManager.getTicketResponsesJson(ticketId);
                if (cachedResponses != null) {
                    Type type = new TypeToken<List<TicketResponse>>(){}.getType();
                    List<TicketResponse> responses = gson.fromJson(cachedResponses, type);
                    if (responses != null) {
                        currentResponses = responses;
                        buildTimeline();
                        hideTimelineShimmer();
                    }
                } else {
                    showTimelineShimmer();
                }

                loadAttachments();
            }
        }
    }

    private void loadTicket() {
        if (scrollContent.getVisibility() != View.VISIBLE) showState("LOADING");
        ticketService.getTicketById(ticketId).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Ticket>> call, @NonNull Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    currentTicket = response.body().getData();
                    sessionManager.saveTicketDetailJson(ticketId, gson.toJson(currentTicket));
                    populateViews(currentTicket);
                    showState("DATA");
                    loadAttachments();
                    buildTimeline();
                } else {
                    if (scrollContent.getVisibility() != View.VISIBLE) showState("ERROR");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Ticket>> call, @NonNull Throwable t) {
                if (scrollContent.getVisibility() != View.VISIBLE) showState("ERROR");
            }
        });
    }

    private void loadResponses() {
        if (sessionManager.getTicketResponsesJson(ticketId) == null) {
            showTimelineShimmer();
        }
        ticketService.getTicketResponses(ticketId).enqueue(new Callback<ApiResponse<List<TicketResponse>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<TicketResponse>>> call,
                                   @NonNull Response<ApiResponse<List<TicketResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    currentResponses = response.body().getData();
                    sessionManager.saveTicketResponsesJson(ticketId, gson.toJson(currentResponses));
                    buildTimeline();
                }
                hideTimelineShimmer();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<TicketResponse>>> call, @NonNull Throwable t) {
                hideTimelineShimmer();
            }
        });
    }

    private void populateViews(Ticket ticket) {
        tvTicketNumber.setText(ticket.getTicketNumber() != null ? "Ticket " + ticket.getTicketNumber() : "Ticket #" + ticket.getId());
        tvCategoryName.setText(ticket.getCategoryName() != null ? ticket.getCategoryName() : "");
        tvTicketTitle.setText(ticket.getTitle());
        tvDescription.setText(ticket.getDescription());
        layoutAttachmentContent.setVisibility(View.GONE);
        boolean showClose = "RESOLVED".equalsIgnoreCase(ticket.getStatus()) && !ticket.isConfirmedResolved();
        btnCloseTicket.setVisibility(showClose ? View.VISIBLE : View.GONE);
        btnCloseTicket.setEnabled(true);
        btnCloseTicket.setText(R.string.close_ticket);
    }

    private void confirmCloseTicket() {
        btnCloseTicket.setEnabled(false);
        btnCloseTicket.setText(R.string.closing);
        ticketService.confirmResolved(ticketId).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Ticket>> call,
                                   @NonNull Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    currentTicket = response.body().getData();
                    sessionManager.saveTicketDetailJson(ticketId, gson.toJson(currentTicket));
                    populateViews(currentTicket);
                    buildTimeline();
                } else {
                    btnCloseTicket.setEnabled(true);
                    btnCloseTicket.setText(R.string.close_ticket);
                    android.widget.Toast.makeText(TicketDetailActivity.this,
                            getString(R.string.error_close_ticket), android.widget.Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Ticket>> call, @NonNull Throwable t) {
                btnCloseTicket.setEnabled(true);
                btnCloseTicket.setText(R.string.close_ticket);
                android.widget.Toast.makeText(TicketDetailActivity.this,
                        getString(R.string.error_close_ticket), android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buildTimeline() {
        if (currentTicket == null) return;

        List<TimelineAdapter.TimelineEvent> events = new ArrayList<>();
        String status = currentTicket.getStatus();

        java.util.Map<String, List<TicketResponse>> responsesByStatus = new java.util.LinkedHashMap<>();
        for (TicketResponse r : currentResponses) {
            String s = r.getTicketStatus() != null ? r.getTicketStatus().toUpperCase() : "PENDING";
            if (!responsesByStatus.containsKey(s)) {
                responsesByStatus.put(s, new ArrayList<>());
            }
            responsesByStatus.get(s).add(r);
        }

        events.add(new TimelineAdapter.TimelineEvent(
                DateFormatter.formatDate(currentTicket.getCreatedAt()),
                "Ticket Created",
                null,
                android.graphics.Color.parseColor("#9E9E9E")
        ));

        List<TicketResponse> pendingResponses = responsesByStatus.get("PENDING");
        if (pendingResponses != null && !pendingResponses.isEmpty()) {
            List<TimelineAdapter.AdminResponse> adminResponses = new ArrayList<>();
            for (TicketResponse r : pendingResponses) {
                adminResponses.add(new TimelineAdapter.AdminResponse(
                        r.getAdminName(), r.getMessage(),
                        DateFormatter.formatDate(r.getCreatedAt())));
            }
            events.add(new TimelineAdapter.TimelineEvent(
                    DateFormatter.formatDate(pendingResponses.get(0).getCreatedAt()),
                    "Admin Responded",
                    adminResponses,
                    android.graphics.Color.parseColor("#FFAA33")
            ));
        }

        if ("IN_PROGRESS".equalsIgnoreCase(status) || "RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            List<TicketResponse> inProgressResponses = responsesByStatus.get("IN_PROGRESS");
            List<TimelineAdapter.AdminResponse> adminResponses = new ArrayList<>();
            if (inProgressResponses != null) {
                for (TicketResponse r : inProgressResponses) {
                    adminResponses.add(new TimelineAdapter.AdminResponse(
                            r.getAdminName(), r.getMessage(),
                            DateFormatter.formatDate(r.getCreatedAt())));
                }
            }
            String time = (inProgressResponses != null && !inProgressResponses.isEmpty())
                    ? DateFormatter.formatDate(inProgressResponses.get(0).getCreatedAt()) : "";
            events.add(new TimelineAdapter.TimelineEvent(
                    time,
                    "Status changed to In-Progress",
                    adminResponses.isEmpty() ? null : adminResponses,
                    android.graphics.Color.parseColor("#FFA726")
            ));
        }

        if ("RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            List<TicketResponse> resolvedResponses = responsesByStatus.get("RESOLVED");
            List<TimelineAdapter.AdminResponse> adminResponses = new ArrayList<>();
            if (resolvedResponses != null) {
                for (TicketResponse r : resolvedResponses) {
                    adminResponses.add(new TimelineAdapter.AdminResponse(
                            r.getAdminName(), r.getMessage(),
                            DateFormatter.formatDate(r.getCreatedAt())));
                }
            }
            String resolvedTime = (resolvedResponses != null && !resolvedResponses.isEmpty())
                    ? DateFormatter.formatDate(resolvedResponses.get(0).getCreatedAt())
                    : DateFormatter.formatDate(currentTicket.getUpdatedAt());
            events.add(new TimelineAdapter.TimelineEvent(
                    resolvedTime, "Concern Resolved",
                    adminResponses.isEmpty() ? null : adminResponses,
                    android.graphics.Color.parseColor("#66BB6A")
            ));
        }

        if ("CLOSED".equalsIgnoreCase(status)) {
            events.add(new TimelineAdapter.TimelineEvent(
                    DateFormatter.formatDate(currentTicket.getUpdatedAt()),
                    "Ticket Closed",
                    null,
                    android.graphics.Color.parseColor("#78909C")
            ));
        }

        timelineAdapter.updateData(events);
    }

    private void showState(String state) {
        progressBar.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        scrollContent.setVisibility(View.GONE);
        switch (state) {
            case "LOADING": progressBar.setVisibility(View.VISIBLE); break;
            case "ERROR": layoutError.setVisibility(View.VISIBLE); break;
            case "DATA": scrollContent.setVisibility(View.VISIBLE); break;
        }
    }

    private void loadAttachments() {
        cvAttachment.setVisibility(View.VISIBLE);
        shimmerAttachment.setVisibility(View.VISIBLE);
        shimmerAttachment.startShimmer();
        layoutAttachmentContent.setVisibility(View.GONE);

        ticketService.getAttachments(ticketId).enqueue(new Callback<ApiResponse<List<AttachmentResponse>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<AttachmentResponse>>> call,
                                   @NonNull Response<ApiResponse<List<AttachmentResponse>>> response) {
                shimmerAttachment.stopShimmer();
                shimmerAttachment.setVisibility(View.GONE);

                if (!isFinishing() && response.isSuccessful()
                        && response.body() != null
                        && response.body().getData() != null
                        && !response.body().getData().isEmpty()) {

                    AttachmentResponse attachment = response.body().getData().get(0);
                    tvAttachmentName.setText(attachment.getOriginalFilename());
                    layoutAttachmentContent.setVisibility(View.VISIBLE);

        ivAttachmentPreview.setVisibility(View.VISIBLE);
        ivAttachmentPreview.setImageResource(R.drawable.ic_attachment);

                String mime = attachment.getMimeType();
                if (mime != null && mime.startsWith("image/")) {
                    cvAttachment.setOnClickListener(v -> {
                        android.content.Intent intent = new android.content.Intent(TicketDetailActivity.this,
                                com.example.ucms_android.ui.ImageViewerActivity.class);
                        intent.putExtra(com.example.ucms_android.ui.ImageViewerActivity.EXTRA_TICKET_ID, ticketId);
                        startActivity(intent);
                    });
                } else {
                    cvAttachment.setOnClickListener(null);
                }
                } else {
                    cvAttachment.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<AttachmentResponse>>> call, @NonNull Throwable t) {
                shimmerAttachment.stopShimmer();
                shimmerAttachment.setVisibility(View.GONE);
                cvAttachment.setVisibility(View.GONE);
            }
        });
    }

}
