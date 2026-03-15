package com.example.ucms_android.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.TicketService;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.util.DateFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminTicketDetailActivity extends AppCompatActivity {

    private TextView tvTicketId, tvStatus, tvAvatarInitials, tvStudentName, tvStudentId, tvStudentCourse;
    private TextView tvDate, tvTitle, tvDescription, tvAttachmentName, tvActionTitle, tvSelectedCategory;
    private MaterialCardView btnBack, cvAttachment;
    private MaterialButton btnUpdateStatus;
    private EditText etResponse;
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
        findViewById(R.id.btnSendResponse).setOnClickListener(v -> sendResponse());
        findViewById(R.id.btnRetry).setOnClickListener(v -> loadTicketDetails());
        
        loadFromCache();
        loadTicketDetails();
    }

    private void initViews() {
        tvTicketId = findViewById(R.id.tvTicketId);
        tvStatus = findViewById(R.id.tvStatus);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentId = findViewById(R.id.tvStudentId);
        tvStudentCourse = findViewById(R.id.tvStudentCourse);
        tvDate = findViewById(R.id.tvDate);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvAttachmentName = findViewById(R.id.tvAttachmentName);
        tvActionTitle = findViewById(R.id.tvActionTitle);
        tvSelectedCategory = findViewById(R.id.tvSelectedCategory);
        btnBack = findViewById(R.id.btnBack);
        cvAttachment = findViewById(R.id.cvAttachment);
        ivAttachmentImage = findViewById(R.id.ivAttachmentImage);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        etResponse = findViewById(R.id.etResponse);
        progressBar = findViewById(R.id.progressBar);
        layoutError = findViewById(R.id.layoutError);
        scrollContent = findViewById(R.id.scrollContent);
        shimmerAttachment = findViewById(R.id.shimmerAttachment);
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
        tvTicketId.setText(ticket.getTicketNumber() != null ? "Ticket " + ticket.getTicketNumber() : "Ticket #" + ticket.getId());
        tvStatus.setText(ticket.getStatus());
        tvStatus.setBackgroundResource(getStatusBackgroundResource(ticket.getStatus()));
        currentStatus = ticket.getStatus();
        configureStatusActions();

        // Student info not returned by backend — show placeholders
        tvStudentName.setText("Student");
        tvAvatarInitials.setText("?");
        tvStudentId.setText("School ID: N/A");
        tvStudentCourse.setText("N/A");

        tvDate.setText(DateFormatter.formatDate(ticket.getCreatedAt()));
        tvTitle.setText(ticket.getTitle());
        tvDescription.setText(ticket.getDescription());

        String categoryName = ticket.getCategoryName() != null ? ticket.getCategoryName() : "N/A";
        tvActionTitle.setText(categoryName.toUpperCase());
        tvSelectedCategory.setText(categoryName.toUpperCase());
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
            btnUpdateStatus.setText("Waiting for student confirmation");
            tvSelectedCategory.setText("CLOSED");
        } else if (nextStatus == null) {
            btnUpdateStatus.setEnabled(false);
            btnUpdateStatus.setAlpha(0.5f);
            btnUpdateStatus.setText("Update Status");
            tvSelectedCategory.setText(currentStatus.toUpperCase());
        } else {
            btnUpdateStatus.setEnabled(true);
            btnUpdateStatus.setAlpha(1.0f);
            btnUpdateStatus.setText("Update Status");
            tvSelectedCategory.setText(nextStatus.replace("_", " ").toUpperCase());
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

        com.google.android.material.button.MaterialButton btnSend =
                findViewById(R.id.btnSendResponse);
        btnSend.setEnabled(false);
        btnSend.setText("TRANSMITTING...");

        ticketService.postResponse(ticketId, new CreateResponseRequest(message))
                .enqueue(new Callback<ApiResponse<TicketResponse>>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiResponse<TicketResponse>> call,
                                           @NonNull Response<ApiResponse<TicketResponse>> response) {
                        btnSend.setEnabled(true);
                        btnSend.setText("TRANSMIT RESPONSE");
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
                        btnSend.setText("TRANSMIT RESPONSE");
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

    private int getStatusBackgroundResource(String status) {
        if ("PENDING".equalsIgnoreCase(status)) return R.drawable.bg_badge_pending;
        if ("IN_PROGRESS".equalsIgnoreCase(status)) return R.drawable.bg_badge_inprogress;
        if ("RESOLVED".equalsIgnoreCase(status)) return R.drawable.bg_badge_resolved;
        return R.drawable.bg_badge_closed;
    }
}
