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
import com.example.ucms_android.util.StatusChipHelper;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminTicketDetailActivity extends AppCompatActivity {

    private TextView tvTicketId, tvCategory, tvUrgencyReason, tvSelectedCategory;
    private TextView tvTitle, tvDescription, tvAttachmentName;
    private View btnBack, cvAttachment, cvAiScoring;
    private MaterialButton btnUpdateStatus;
    private ImageButton btnSendComment;
    private ProgressBar pbSendComment;
    private ImageView ivSendError;
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

        btnOverrideUrgency.setOnClickListener(v -> {
            if (dropUrgencyLevel.getVisibility() == View.GONE) {
                dropUrgencyLevel.setVisibility(View.VISIBLE);
                etUrgencyOverrideReason.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Override fields enabled.", Toast.LENGTH_SHORT).show();
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
        
        progressBar = findViewById(R.id.progressBar);
        layoutError = findViewById(R.id.layoutError);
        scrollContent = findViewById(R.id.scrollContent);
        shimmerAttachment = findViewById(R.id.shimmerAttachment);
        
        dropUrgencyLevel = findViewById(R.id.dropUrgencyLevel);
        etUrgencyOverrideReason = findViewById(R.id.etUrgencyOverrideReason);
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
                        Glide.with(AdminTicketDetailActivity.this).load(attachment.getSignedUrl()).into(ivAttachmentImage);
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
                            displayTicket(response.body().getData());
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

    private String buildUrgencyDetails(Ticket ticket) {
        if (ticket == null || ticket.getUrgencyReason() == null) return null;
        return ticket.getUrgencyReason()
                .replaceAll("(?i)\\bstatus\\s*=\\s*[^;\\n]+;?\\s*", "")
                .replaceAll("(?i)\\bageHours\\s*=\\s*\\d+;?\\s*", "")
                .trim();
    }
}
