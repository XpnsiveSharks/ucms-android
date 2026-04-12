package com.example.ucms_android.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.example.ucms_android.R;
import com.example.ucms_android.model.AssignTicketRequest;
import com.example.ucms_android.model.AttachmentResponse;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.CreateResponseRequest;
import com.example.ucms_android.model.StatusUpdateRequest;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.model.TicketResponse;
import com.example.ucms_android.model.UrgencyOverrideRequest;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.TicketService;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.adapter.TimelineAdapter;
import com.example.ucms_android.util.DateFormatter;
import com.example.ucms_android.util.StatusChipHelper;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminTicketDetailActivity extends AppCompatActivity {

    private TextView tvTicketId, tvCategory, tvUrgencyReason, tvSelectedCategory;
    private TextView tvTitle, tvDescription, tvAttachmentName;
    private TextView tvAssignedAdminLabel, tvAssignedAdmin;
    private MaterialButton btnAssignToMe;
    private View btnBack, cvAttachment, cvAiScoring;
    private MaterialButton btnUpdateStatus;
    private ImageButton btnSendComment;
    private ProgressBar pbSendComment;
    private ImageView ivSendError;
    private View btnOverrideUrgency;
    private EditText etResponse, etUrgencyOverrideReason;
    private AutoCompleteTextView dropUrgencyLevel;
    private ImageView ivAttachmentImage;
    private View loadingOverlay;
    private LinearLayout layoutError;
    private NestedScrollView scrollContent;
    private TicketService ticketService;
    private Long ticketId;
    private String currentStatus;
    private ShimmerFrameLayout shimmerAttachment;
    private ShimmerFrameLayout shimmerTimeline;
    private RecyclerView rvTimeline;
    private TimelineAdapter timelineAdapter;
    private SessionManager sessionManager;
    private Gson gson;
    private Ticket currentTicket;
    private List<TicketResponse> currentResponses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ticket_detail);

        ticketId = getIntent().getLongExtra("ticketId", -1);
        if (ticketId == -1) {
            finish();
            return;
        }

        initViews();
        ticketService = ApiClient.getInstance(this).create(TicketService.class);
        sessionManager = new SessionManager(this);
        gson = new Gson();
        
        btnBack.setOnClickListener(v -> finish());
        btnSendComment.setOnClickListener(v -> appendComment());
        btnAssignToMe.setOnClickListener(v -> assignToMe());

        btnOverrideUrgency.setOnClickListener(v -> {
            if (dropUrgencyLevel.getVisibility() == View.GONE) {
                dropUrgencyLevel.setVisibility(View.VISIBLE);
                etUrgencyOverrideReason.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Override fields enabled.", Toast.LENGTH_SHORT).show();
            } else {
                applyUrgencyOverride();
            }
        });
        findViewById(R.id.btnRetry).setOnClickListener(v -> {
            loadTicketDetails();
            loadResponses();
        });

        setupUrgencyOverrideDropdown();

        timelineAdapter = new TimelineAdapter(new ArrayList<>());
        rvTimeline.setLayoutManager(new LinearLayoutManager(this));
        rvTimeline.setAdapter(timelineAdapter);
        
        loadFromCache();
        loadTicketDetails();
        loadResponses();
    }

    private void initViews() {
        tvTicketId = findViewById(R.id.tvTicketId);
        tvCategory = findViewById(R.id.tvCategory);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvAttachmentName = findViewById(R.id.tvAttachmentName);
        
        cvAiScoring = findViewById(R.id.cvAiScoring);
        tvUrgencyReason = findViewById(R.id.tvUrgencyReason);
        
        btnBack = findViewById(R.id.btnBack);
        cvAttachment = findViewById(R.id.cvAttachment);
        ivAttachmentImage = findViewById(R.id.ivAttachmentImage);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        
        tvSelectedCategory = findViewById(R.id.tvSelectedCategory);
        etResponse = findViewById(R.id.etResponse);
        btnSendComment = findViewById(R.id.btnSendComment);
        pbSendComment = findViewById(R.id.pbSendComment);
        ivSendError = findViewById(R.id.ivSendError);

        btnOverrideUrgency = findViewById(R.id.btnOverrideUrgency);
        
        loadingOverlay = findViewById(R.id.loadingOverlay);
        layoutError = findViewById(R.id.layoutError);
        scrollContent = findViewById(R.id.scrollContent);
        shimmerAttachment = findViewById(R.id.shimmerAttachment);
        shimmerTimeline = findViewById(R.id.shimmerTimeline);
        rvTimeline = findViewById(R.id.rvTimeline);
        
        dropUrgencyLevel = findViewById(R.id.dropUrgencyLevel);
        etUrgencyOverrideReason = findViewById(R.id.etUrgencyOverrideReason);

        tvAssignedAdminLabel = findViewById(R.id.tvAssignedAdminLabel);
        tvAssignedAdmin = findViewById(R.id.tvAssignedAdmin);
        btnAssignToMe = findViewById(R.id.btnAssignToMe);
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

    private void buildTimeline() {
        if (currentTicket == null) return;

        List<TimelineAdapter.TimelineEvent> events = new ArrayList<>();
        String status = currentTicket.getStatus();

        List<TicketResponse> sortedResponses = new ArrayList<>(currentResponses);
        sortedResponses.sort(Comparator.comparing(TicketResponse::getCreatedAt, Comparator.nullsLast(String::compareTo)));

        events.add(new TimelineAdapter.TimelineEvent(
                DateFormatter.formatDate(currentTicket.getCreatedAt()),
                "Ticket Created",
                null,
                android.graphics.Color.parseColor("#9E9E9E")
        ));

        java.util.Map<String, List<TimelineAdapter.AdminResponse>> statusResponses = new java.util.HashMap<>();
        for (TicketResponse r : sortedResponses) {
            String s = r.getTicketStatus() != null ? r.getTicketStatus().toUpperCase() : "PENDING";
            if (!statusResponses.containsKey(s)) {
                statusResponses.put(s, new ArrayList<>());
            }
            statusResponses.get(s).add(new TimelineAdapter.AdminResponse(
                    r.getAdminName(), r.getMessage(), DateFormatter.formatDate(r.getCreatedAt())));
        }

        List<TimelineAdapter.AdminResponse> pendingRes = statusResponses.get("PENDING");
        if (pendingRes != null) {
            events.add(new TimelineAdapter.TimelineEvent(
                    pendingRes.get(0).time,
                    "Admin Responded",
                    pendingRes,
                    android.graphics.Color.parseColor("#FFAA33")
            ));
        }

        if ("IN_PROGRESS".equalsIgnoreCase(status) || "RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            List<TimelineAdapter.AdminResponse> inProgRes = statusResponses.get("IN_PROGRESS");
            String time = (inProgRes != null && !inProgRes.isEmpty()) ? inProgRes.get(0).time : "";
            events.add(new TimelineAdapter.TimelineEvent(
                    time,
                    "Status changed to In-Progress",
                    inProgRes,
                    android.graphics.Color.parseColor("#FFA726")
            ));
        }

        if ("RESOLVED".equalsIgnoreCase(status) || "CLOSED".equalsIgnoreCase(status)) {
            List<TimelineAdapter.AdminResponse> resRes = statusResponses.get("RESOLVED");
            String time = (resRes != null && !resRes.isEmpty()) ? resRes.get(0).time : DateFormatter.formatDate(currentTicket.getUpdatedAt());
            events.add(new TimelineAdapter.TimelineEvent(
                    time,
                    "Concern Resolved",
                    resRes,
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
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        scrollContent.setVisibility(View.GONE);
        switch (state) {
            case "LOADING":
                if (loadingOverlay != null) loadingOverlay.setVisibility(View.VISIBLE);
                break;
            case "ERROR":
                layoutError.setVisibility(View.VISIBLE);
                break;
            case "DATA":
                scrollContent.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void loadFromCache() {
        String cachedJson = sessionManager.getTicketDetailJson(ticketId);

        // Fallback: find the ticket inside the all-tickets cache
        if (cachedJson == null) {
            String allJson = sessionManager.getAdminAllTicketsJson();
            if (allJson != null) {
                Type listType = new TypeToken<List<Ticket>>() {}.getType();
                List<Ticket> all = gson.fromJson(allJson, listType);
                if (all != null) {
                    for (Ticket t : all) {
                        if (ticketId.equals(t.getId())) {
                            cachedJson = gson.toJson(t);
                            break;
                        }
                    }
                }
            }
        }

        if (cachedJson != null) {
            Ticket cached = gson.fromJson(cachedJson, Ticket.class);
            if (cached != null) {
                displayTicket(cached);
                showState("DATA");
                loadAttachments();
            }
        }
    }

    private void loadTicketDetails() {
        if (scrollContent.getVisibility() != View.VISIBLE) {
            showState("LOADING");
        }
        ticketService.getTicketById(ticketId).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Ticket>> call, @NonNull Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    currentTicket = response.body().getData();
                    sessionManager.saveTicketDetailJson(ticketId, gson.toJson(currentTicket));
                    displayTicket(currentTicket);
                    showState("DATA");
                    loadAttachments();
                    buildTimeline();
                } else {
                    if (scrollContent.getVisibility() != View.VISIBLE) {
                        showState("ERROR");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Ticket>> call, @NonNull Throwable t) {
                if (scrollContent.getVisibility() != View.VISIBLE) {
                    showState("ERROR");
                }
            }
        });
    }

    private void displayTicket(Ticket ticket) {
        tvTicketId.setText(ticket.getTicketNumber() != null ? ticket.getTicketNumber() : "TKT-#" + ticket.getId());
        
        String categoryName = ticket.getCategoryName() != null ? ticket.getCategoryName() : "N/A";
        tvCategory.setText(categoryName.toUpperCase());
        
        tvTitle.setText(ticket.getTitle());
        tvDescription.setText(ticket.getDescription());

        String urgencyDetails = buildUrgencyDetails(ticket);
        if (urgencyDetails != null && !urgencyDetails.trim().isEmpty()) {
            tvUrgencyReason.setText(urgencyDetails);
            cvAiScoring.setVisibility(View.VISIBLE);
        } else {
            cvAiScoring.setVisibility(View.GONE);
        }
        
        String priorityLevel = StatusChipHelper.resolvePriorityLevel(ticket);
        if (priorityLevel != null && dropUrgencyLevel != null) {
            dropUrgencyLevel.setText(priorityLevel, false);
        }

        currentStatus = ticket.getStatus();
        configureStatusActions();
        configureAssignSection(ticket);
    }

    private void setupUrgencyOverrideDropdown() {
        String[] options = new String[]{"CRITICAL", "HIGH", "LOW", "MUTED"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                options
        );
        dropUrgencyLevel.setAdapter(adapter);
    }

    private void applyUrgencyOverride() {
        String selected = dropUrgencyLevel.getText() != null
                ? dropUrgencyLevel.getText().toString().trim().toUpperCase(Locale.ROOT)
                : "";
        if (selected.isEmpty()) {
            dropUrgencyLevel.setError("Select urgency level");
            return;
        }

        String reason = etUrgencyOverrideReason.getText().toString().trim();
        ticketService.overrideTicketUrgency(ticketId, new UrgencyOverrideRequest(selected, reason.isEmpty() ? null : reason))
                .enqueue(new Callback<ApiResponse<Ticket>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Ticket>> call,
                                           @NonNull Response<ApiResponse<Ticket>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            displayTicket(response.body().getData());
                            Toast.makeText(AdminTicketDetailActivity.this, "Urgency override applied", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Ticket>> call, @NonNull Throwable t) {
                        Toast.makeText(AdminTicketDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAttachments() {
        shimmerAttachment.setVisibility(View.VISIBLE);
        shimmerAttachment.startShimmer();
        cvAttachment.setVisibility(View.GONE);
        ticketService.getAttachments(ticketId).enqueue(new Callback<ApiResponse<List<AttachmentResponse>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<AttachmentResponse>>> call,
                                   @NonNull Response<ApiResponse<List<AttachmentResponse>>> response) {
                shimmerAttachment.stopShimmer();
                shimmerAttachment.setVisibility(View.GONE);
                if (!isFinishing() && response.isSuccessful() && response.body() != null && response.body().getData() != null && !response.body().getData().isEmpty()) {
                    AttachmentResponse attachment = response.body().getData().get(0);
                    cvAttachment.setVisibility(View.VISIBLE);
                    tvAttachmentName.setText(attachment.getOriginalFilename());
                    String mime = attachment.getMimeType();
                    if (mime != null && mime.startsWith("image/")) {
                        ivAttachmentImage.setVisibility(View.VISIBLE);
                        com.bumptech.glide.signature.ObjectKey stableKey = attachment.getId() != null
                                ? new com.bumptech.glide.signature.ObjectKey("attachment-" + attachment.getId())
                                : new com.bumptech.glide.signature.ObjectKey("ticket-attachment-" + ticketId);
                        Glide.with(AdminTicketDetailActivity.this)
                                .load(attachment.getSignedUrl())
                                .signature(stableKey)
                                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.AUTOMATIC)
                                .into(ivAttachmentImage);
                        cvAttachment.setOnClickListener(v -> {
                            android.content.Intent intent = new android.content.Intent(
                                    AdminTicketDetailActivity.this,
                                    com.example.ucms_android.ui.ImageViewerActivity.class);
                            intent.putExtra(
                                    com.example.ucms_android.ui.ImageViewerActivity.EXTRA_TICKET_ID,
                                    ticketId);
                            startActivity(intent);
                        });
                    } else {
                        cvAttachment.setOnClickListener(null);
                    }
                }
            }
            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<AttachmentResponse>>> call, @NonNull Throwable t) {
                shimmerAttachment.stopShimmer();
                shimmerAttachment.setVisibility(View.GONE);
            }
        });
    }

    private String getNextStatus(String current) {
        if ("PENDING".equalsIgnoreCase(current)) return "IN_PROGRESS";
        if ("IN_PROGRESS".equalsIgnoreCase(current)) return "RESOLVED";
        if ("RESOLVED".equalsIgnoreCase(current)) return "CLOSED";
        return null;
    }

    private void configureStatusActions() {
        String nextStatus = getNextStatus(currentStatus);
        if ("RESOLVED".equalsIgnoreCase(currentStatus)) {
            btnUpdateStatus.setEnabled(false);
            btnUpdateStatus.setAlpha(0.5f);
            btnUpdateStatus.setText("WAITING FOR CONFIRMATION");
            tvSelectedCategory.setText("RESOLVED");
        } else if (nextStatus == null) {
            btnUpdateStatus.setEnabled(false);
            btnUpdateStatus.setText("UPDATE STATUS");
            tvSelectedCategory.setText(currentStatus != null ? currentStatus.toUpperCase() : "N/A");
        } else {
            btnUpdateStatus.setEnabled(true);
            btnUpdateStatus.setAlpha(1.0f);
            btnUpdateStatus.setText("UPDATE STATUS");
            tvSelectedCategory.setText(nextStatus.toUpperCase());
            btnUpdateStatus.setOnClickListener(v -> updateStatus(nextStatus));
        }
    }

    private void updateStatus(String newStatus) {
        String message = etResponse.getText().toString().trim();
        btnUpdateStatus.setEnabled(false);
        btnUpdateStatus.setText("Updating...");

        StatusUpdateRequest updateRequest = new StatusUpdateRequest(newStatus);
        if (!message.isEmpty()) updateRequest.setComment(message);

        ticketService.updateTicketStatus(ticketId, updateRequest)
                .enqueue(new Callback<ApiResponse<Ticket>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Ticket>> call, @NonNull Response<ApiResponse<Ticket>> response) {
                        btnUpdateStatus.setText("Update Status");
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            Toast.makeText(AdminTicketDetailActivity.this, "Status updated", Toast.LENGTH_SHORT).show();
                            etResponse.setText("");
                            currentTicket = response.body().getData();
                            displayTicket(currentTicket);
                            loadResponses();
                        } else {
                            btnUpdateStatus.setEnabled(true);
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Ticket>> call, @NonNull Throwable t) {
                        btnUpdateStatus.setEnabled(true);
                        Toast.makeText(AdminTicketDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void appendComment() {
        String message = etResponse.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "Enter a comment first", Toast.LENGTH_SHORT).show();
            return;
        }
        setSendState("SENDING");
        ticketService.postResponse(ticketId, new CreateResponseRequest(message))
                .enqueue(new Callback<ApiResponse<TicketResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<TicketResponse>> call,
                                           @NonNull Response<ApiResponse<TicketResponse>> response) {
                        if (response.isSuccessful()) {
                            etResponse.setText("");
                            setSendState("IDLE");
                            loadResponses();
                        } else {
                            setSendState("ERROR");
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<TicketResponse>> call, @NonNull Throwable t) {
                        setSendState("ERROR");
                    }
                });
    }

    private void setSendState(String state) {
        switch (state) {
            case "SENDING":
                btnSendComment.setVisibility(View.INVISIBLE);
                ivSendError.setVisibility(View.GONE);
                pbSendComment.setVisibility(View.VISIBLE);
                break;
            case "ERROR":
                pbSendComment.setVisibility(View.GONE);
                btnSendComment.setVisibility(View.GONE);
                ivSendError.setVisibility(View.VISIBLE);
                // Tap the error icon to retry
                ivSendError.setOnClickListener(v -> appendComment());
                Toast.makeText(this, "Failed to send — tap to retry", Toast.LENGTH_SHORT).show();
                break;
            case "IDLE":
            default:
                pbSendComment.setVisibility(View.GONE);
                ivSendError.setVisibility(View.GONE);
                btnSendComment.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void configureAssignSection(Ticket ticket) {
        String assignedName = ticket.getAssignedAdminName();
        boolean isPending = "PENDING".equalsIgnoreCase(ticket.getStatus());

        if (assignedName != null && !assignedName.isEmpty()) {
            tvAssignedAdminLabel.setVisibility(View.VISIBLE);
            tvAssignedAdmin.setVisibility(View.VISIBLE);
            tvAssignedAdmin.setText(assignedName);
            btnAssignToMe.setVisibility(View.GONE);
        } else if (isPending) {
            tvAssignedAdminLabel.setVisibility(View.VISIBLE);
            tvAssignedAdmin.setVisibility(View.GONE);
            btnAssignToMe.setVisibility(View.VISIBLE);
            btnAssignToMe.setEnabled(true);
        } else {
            tvAssignedAdminLabel.setVisibility(View.GONE);
            tvAssignedAdmin.setVisibility(View.GONE);
            btnAssignToMe.setVisibility(View.GONE);
        }
    }

    private void assignToMe() {
        btnAssignToMe.setEnabled(false);
        btnAssignToMe.setText("Assigning...");
        ticketService.assignAdmin(ticketId, new AssignTicketRequest())
                .enqueue(new Callback<ApiResponse<Ticket>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Ticket>> call,
                                           @NonNull Response<ApiResponse<Ticket>> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().getData() != null) {
                            currentTicket = response.body().getData();
                            displayTicket(currentTicket);
                            Toast.makeText(AdminTicketDetailActivity.this,
                                    "Ticket assigned to you", Toast.LENGTH_SHORT).show();
                        } else {
                            btnAssignToMe.setEnabled(true);
                            btnAssignToMe.setText("ASSIGN TO ME");
                            Toast.makeText(AdminTicketDetailActivity.this,
                                    "Assignment failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Ticket>> call, @NonNull Throwable t) {
                        btnAssignToMe.setEnabled(true);
                        btnAssignToMe.setText("ASSIGN TO ME");
                        Toast.makeText(AdminTicketDetailActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String buildUrgencyDetails(Ticket ticket) {
        if (ticket == null || ticket.getUrgencyReason() == null) return null;
        return ticket.getUrgencyReason()
                .replaceAll("(?i)\\bstatus\\s*=\\s*[^;\\n]+;?\\s*", "")
                .replaceAll("(?i)\\bageHours\\s*=\\s*\\d+;?\\s*", "")
                .trim();
    }
}
