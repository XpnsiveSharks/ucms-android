package com.example.ucms_android.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.bumptech.glide.Glide;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.example.ucms_android.R;
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
import com.example.ucms_android.util.DateFormatter;
import com.example.ucms_android.util.StatusChipHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminTicketDetailActivity extends AppCompatActivity {

    private TextView tvTicketId, tvCategory, tvUrgencyReason, tvAvatarInitials, tvStudentName, tvStudentId, tvStudentCourse;
    private TextView tvDate, tvTitle, tvDescription, tvAttachmentName, tvActionTitle, tvSelectedCategory;
    private TextView tvOverrideState, tvStatus;
    private MaterialCardView btnBack, cvAttachment, cvAiScoring;
    private MaterialButton btnUpdateStatus;
    private View btnOverrideUrgency;
    private EditText etResponse, etUrgencyOverrideReason;
    private AutoCompleteTextView dropUrgencyLevel;
    private ImageView ivAttachmentImage;
    private ProgressBar progressBar;
    private LinearLayout layoutError;
    private NestedScrollView scrollContent;
    private TicketService ticketService;
    private Long ticketId;
    private String currentStatus;
    private ShimmerFrameLayout shimmerAttachment;
    private SessionManager sessionManager;
    private Gson gson;
    private Ticket currentTicket;

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
        
        View btnSendIconInside = findViewById(R.id.btnSendIconInside);
        if (btnSendIconInside != null) {
            btnSendIconInside.setOnClickListener(v -> sendResponse());
        }
        
        btnOverrideUrgency.setOnClickListener(v -> {
            if (dropUrgencyLevel.getVisibility() == View.GONE) {
                dropUrgencyLevel.setVisibility(View.VISIBLE);
                etUrgencyOverrideReason.setVisibility(View.VISIBLE);
                if (tvOverrideState != null) tvOverrideState.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Override fields enabled. Select level and reason below.", Toast.LENGTH_SHORT).show();
            } else {
                applyUrgencyOverride();
            }
        });
        findViewById(R.id.btnRetry).setOnClickListener(v -> loadTicketDetails());

        setupUrgencyOverrideDropdown();
        
        loadFromCache();
        loadTicketDetails();
    }

    private void initViews() {
        tvTicketId = findViewById(R.id.tvTicketId);
        tvCategory = findViewById(R.id.tvCategory);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        
        cvAiScoring = findViewById(R.id.cvAiScoring);
        tvUrgencyReason = findViewById(R.id.tvUrgencyReason);
        tvStatus = findViewById(R.id.tvStatus);
        
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentId = findViewById(R.id.tvStudentId);
        tvStudentCourse = findViewById(R.id.tvStudentCourse);
        tvDate = findViewById(R.id.tvDate);
        tvActionTitle = findViewById(R.id.tvActionTitle);
        tvAttachmentName = findViewById(R.id.tvAttachmentName);
        
        btnBack = findViewById(R.id.btnBack);
        cvAttachment = findViewById(R.id.cvAttachment);
        ivAttachmentImage = findViewById(R.id.ivAttachmentImage);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        
        tvSelectedCategory = findViewById(R.id.tvSelectedCategory);
        etResponse = findViewById(R.id.etResponse);
        
        btnOverrideUrgency = findViewById(R.id.btnOverrideUrgency);
        
        progressBar = findViewById(R.id.progressBar);
        layoutError = findViewById(R.id.layoutError);
        scrollContent = findViewById(R.id.scrollContent);
        shimmerAttachment = findViewById(R.id.shimmerAttachment);
        
        // Internal fields for logic that might be hidden in UI
        dropUrgencyLevel = findViewById(R.id.dropUrgencyLevel);
        etUrgencyOverrideReason = findViewById(R.id.etUrgencyOverrideReason);
        tvOverrideState = findViewById(R.id.tvOverrideState);
    }

    private void showState(String state) {
        progressBar.setVisibility(View.GONE);
        layoutError.setVisibility(View.GONE);
        scrollContent.setVisibility(View.GONE);
        switch (state) {
            case "LOADING":
                progressBar.setVisibility(View.VISIBLE);
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
                    Ticket ticket = response.body().getData();
                    sessionManager.saveTicketDetailJson(ticketId, gson.toJson(ticket));
                    displayTicket(ticket);
                    showState("DATA");
                    loadAttachments();
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
        currentTicket = ticket;
        tvTicketId.setText(ticket.getTicketNumber() != null ? ticket.getTicketNumber() : "TKT-#" + ticket.getId());
        
        String categoryName = ticket.getCategoryName() != null ? ticket.getCategoryName() : "N/A";
        tvCategory.setText(categoryName.toUpperCase());
        
        tvTitle.setText(ticket.getTitle());
        tvDescription.setText(ticket.getDescription());

        // AI Scoring Section - Only show analysis text
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

        if (ticket.isUrgencyOverridden() && tvOverrideState != null) {
            String overrideReason = ticket.getUrgencyOverrideReason();
            if (overrideReason == null || overrideReason.trim().isEmpty()) {
                tvOverrideState.setText("Urgency is manually overridden by admin.");
            } else {
                tvOverrideState.setText("Urgency override reason: " + overrideReason.trim());
            }
            tvOverrideState.setVisibility(View.VISIBLE);
        } else if (tvOverrideState != null) {
            tvOverrideState.setVisibility(View.GONE);
        }
    }

    private void setupUrgencyOverrideDropdown() {
        String[] options = new String[]{"CRITICAL", "HIGH", "LOW", "MUTED"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                options
        );
        dropUrgencyLevel.setAdapter(adapter);
        dropUrgencyLevel.setText("HIGH", false);
    }

    private void applyUrgencyOverride() {
        String selected = dropUrgencyLevel.getText() != null
                ? dropUrgencyLevel.getText().toString().trim().toUpperCase(Locale.ROOT)
                : "";
        if (selected.isEmpty()) {
            dropUrgencyLevel.setError("Select urgency level");
            dropUrgencyLevel.requestFocus();
            return;
        }

        String reason = etUrgencyOverrideReason.getText() == null
                ? null
                : etUrgencyOverrideReason.getText().toString().trim();
        if (reason != null && reason.isEmpty()) {
            reason = null;
        }
        final String overrideReason = reason;

        btnOverrideUrgency.setEnabled(false);
        btnOverrideUrgency.setAlpha(0.5f);

        ticketService.overrideTicketUrgency(ticketId, new UrgencyOverrideRequest(selected, overrideReason))
                .enqueue(new Callback<ApiResponse<Ticket>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Ticket>> call,
                                           @NonNull Response<ApiResponse<Ticket>> response) {
                        btnOverrideUrgency.setEnabled(true);
                        btnOverrideUrgency.setAlpha(1.0f);
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            Ticket updated = response.body().getData();
                            if (overrideReason != null) {
                                etUrgencyOverrideReason.setText(overrideReason);
                            }
                            displayTicket(updated);
                            Toast.makeText(AdminTicketDetailActivity.this,
                                    "Urgency override applied",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String message = "Failed to apply urgency override";
                        if (response.body() != null && response.body().getMessage() != null) {
                            message = response.body().getMessage();
                        }
                        Toast.makeText(AdminTicketDetailActivity.this, message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Ticket>> call, @NonNull Throwable t) {
                        btnOverrideUrgency.setEnabled(true);
                        btnOverrideUrgency.setAlpha(1.0f);
                        Toast.makeText(AdminTicketDetailActivity.this,
                                "Network error while overriding urgency",
                                Toast.LENGTH_SHORT).show();
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
                if (!isFinishing() && response.isSuccessful()
                        && response.body() != null
                        && response.body().getData() != null
                        && !response.body().getData().isEmpty()) {

                    AttachmentResponse attachment = response.body().getData().get(0);
                    cvAttachment.setVisibility(View.VISIBLE);
                    cvAttachment.setOnClickListener(v -> {
                        android.content.Intent intent = new android.content.Intent(AdminTicketDetailActivity.this,
                                com.example.ucms_android.ui.ImageViewerActivity.class);
                        intent.putExtra(com.example.ucms_android.ui.ImageViewerActivity.EXTRA_TICKET_ID, ticketId);
                        startActivity(intent);
                    });
                    tvAttachmentName.setText(attachment.getOriginalFilename());

                    String mime = attachment.getMimeType();
                    if (mime != null && mime.startsWith("image/")) {
                        ivAttachmentImage.setVisibility(View.VISIBLE);
                        Glide.with(AdminTicketDetailActivity.this)
                                .load(attachment.getSignedUrl())
                                .placeholder(R.drawable.ic_image)
                                .error(R.drawable.ic_image)
                                .into(ivAttachmentImage);
                        ivAttachmentImage.setOnClickListener(v -> {
                            android.content.Intent intent = new android.content.Intent(AdminTicketDetailActivity.this,
                                    com.example.ucms_android.ui.ImageViewerActivity.class);
                            intent.putExtra(com.example.ucms_android.ui.ImageViewerActivity.EXTRA_TICKET_ID, ticketId);
                            startActivity(intent);
                        });
                        cvAttachment.setOnClickListener(v -> {
                            android.content.Intent intent = new android.content.Intent(AdminTicketDetailActivity.this,
                                    com.example.ucms_android.ui.ImageViewerActivity.class);
                            intent.putExtra(com.example.ucms_android.ui.ImageViewerActivity.EXTRA_TICKET_ID, ticketId);
                            startActivity(intent);
                        });
                    } else {
                        ivAttachmentImage.setVisibility(View.GONE);
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
            btnUpdateStatus.setAlpha(0.5f);
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
        btnUpdateStatus.setEnabled(false);
        btnUpdateStatus.setText("Updating...");

        ticketService.updateTicketStatus(ticketId, new StatusUpdateRequest(newStatus))
                .enqueue(new Callback<ApiResponse<Ticket>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<Ticket>> call,
                                           @NonNull Response<ApiResponse<Ticket>> response) {
                        btnUpdateStatus.setText("Update Status");
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            Toast.makeText(AdminTicketDetailActivity.this,
                                    "Status updated to " + newStatus.replace("_", " "),
                                    Toast.LENGTH_SHORT).show();
                            displayTicket(response.body().getData());
                            showState("DATA");
                        } else {
                            btnUpdateStatus.setEnabled(true);
                            Toast.makeText(AdminTicketDetailActivity.this,
                                    "Failed to update status", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<Ticket>> call, @NonNull Throwable t) {
                        btnUpdateStatus.setText("Update Status");
                        btnUpdateStatus.setEnabled(true);
                        Toast.makeText(AdminTicketDetailActivity.this,
                                "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendResponse() {
        String message = etResponse.getText().toString().trim();
        if (message.isEmpty()) {
            etResponse.setError("Response cannot be empty");
            etResponse.requestFocus();
            return;
        }

        View btnSend = findViewById(R.id.btnSendIconInside);
        btnSend.setEnabled(false);
        btnSend.setAlpha(0.5f);

        ticketService.postResponse(ticketId, new CreateResponseRequest(message))
                .enqueue(new Callback<ApiResponse<TicketResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<TicketResponse>> call,
                                           @NonNull Response<ApiResponse<TicketResponse>> response) {
                        btnSend.setEnabled(true);
                        btnSend.setAlpha(1.0f);
                        if (response.isSuccessful()) {
                            etResponse.setText("");
                            etResponse.clearFocus();
                            Toast.makeText(AdminTicketDetailActivity.this,
                                    "Response transmitted.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(AdminTicketDetailActivity.this,
                                    "Failed to send response.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiResponse<TicketResponse>> call,
                                          @NonNull Throwable t) {
                        btnSend.setEnabled(true);
                        btnSend.setAlpha(1.0f);
                        Toast.makeText(AdminTicketDetailActivity.this,
                                "Network error.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "??";
        String[] parts = name.split(" ");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return name.substring(0, Math.min(name.length(), 2)).toUpperCase();
    }

    private String buildUrgencyDetails(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        String reason = ticket.getUrgencyReason();
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isEmpty()) {
            return null;
        }

        // UI requirement: show only human-readable reason message.
        // Hide raw diagnostic tokens like status=... and ageHours=... when present.
        String cleaned = normalizedReason
                .replaceAll("(?i)\\bstatus\\s*=\\s*[^;\\n]+;?\\s*", "")
                .replaceAll("(?i)\\bageHours\\s*=\\s*\\d+;?\\s*", "")
                .replaceAll("\\s{2,}", " ")
                .trim();

        if (cleaned.endsWith(";")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }

        return cleaned.isEmpty() ? null : cleaned;
    }
}
