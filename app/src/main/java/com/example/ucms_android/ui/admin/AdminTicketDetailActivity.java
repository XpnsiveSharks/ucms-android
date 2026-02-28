package com.example.ucms_android.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.StatusUpdateRequest;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.TicketService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminTicketDetailActivity extends AppCompatActivity {

    private TextView tvTicketNumber, tvStatus, tvDate, tvTicketTitle,
            tvDescription, tvStudentName, tvStudentId, tvCourseYear,
            tvAttachmentName, tvAttachmentLabel;
    private ImageView ivAvatar, ivAttachmentPreview;
    private ImageButton btnBack;
    private Spinner spinnerCategory;
    private MaterialButton btnUpdateStatus;

    private TicketService ticketService;
    private Long ticketId;

    private static final String[] STATUS_OPTIONS = {"IN_PROGRESS", "RESOLVED", "CLOSED"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_ticket_detail);

        ticketId = getIntent().getLongExtra("ticketId", -1);
        if (ticketId == -1) {
            finish();
            return;
        }

        bindViews();

        ticketService = ApiClient.getInstance(this).create(TicketService.class);

        btnBack.setOnClickListener(v -> finish());

        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, STATUS_OPTIONS);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(statusAdapter);

        btnUpdateStatus.setOnClickListener(v -> updateStatus());

        loadTicket();
    }

    private void bindViews() {
        tvTicketNumber = findViewById(R.id.tvTicketNumber);
        tvStatus = findViewById(R.id.tvStatus);
        tvDate = findViewById(R.id.tvDate);
        tvTicketTitle = findViewById(R.id.tvTicketTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentId = findViewById(R.id.tvStudentId);
        tvCourseYear = findViewById(R.id.tvCourseYear);
        tvAttachmentName = findViewById(R.id.tvAttachmentName);
        tvAttachmentLabel = findViewById(R.id.tvAttachmentLabel);
        ivAvatar = findViewById(R.id.ivAvatar);
        ivAttachmentPreview = findViewById(R.id.ivAttachmentPreview);
        btnBack = findViewById(R.id.btnBack);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
    }

    private void loadTicket() {
        ticketService.getTicketById(ticketId).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    populateViews(response.body().getData());
                } else {
                    Toast.makeText(AdminTicketDetailActivity.this,
                            getString(R.string.error_loading_tickets), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                Toast.makeText(AdminTicketDetailActivity.this,
                        getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populateViews(Ticket ticket) {
        tvTicketNumber.setText(ticket.getTicketNumber());
        tvStatus.setText(ticket.getStatus());
        tvDate.setText(ticket.getCreatedAt());
        tvTicketTitle.setText(ticket.getTitle());
        tvDescription.setText(ticket.getDescription());
        tvStudentName.setText(ticket.getStudentName());
        tvStudentId.setText(ticket.getStudentId());
        tvCourseYear.setText(ticket.getCourseYear());

        tvStatus.setBackground(getStatusDrawable(ticket.getStatus()));

        if (ticket.getAttachmentName() != null && !ticket.getAttachmentName().isEmpty()) {
            tvAttachmentName.setText(ticket.getAttachmentName());
            tvAttachmentName.setVisibility(View.VISIBLE);
            ivAttachmentPreview.setVisibility(View.VISIBLE);
        } else {
            tvAttachmentLabel.setVisibility(View.GONE);
            ivAttachmentPreview.setVisibility(View.GONE);
            tvAttachmentName.setVisibility(View.GONE);
        }
    }

    private void updateStatus() {
        String selectedStatus = STATUS_OPTIONS[spinnerCategory.getSelectedItemPosition()];
        ticketService.updateTicketStatus(ticketId, new StatusUpdateRequest(selectedStatus))
                .enqueue(new Callback<ApiResponse<Ticket>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            Ticket updatedTicket = response.body().getData();
                            tvStatus.setText(updatedTicket.getStatus());
                            tvStatus.setBackground(getStatusDrawable(updatedTicket.getStatus()));
                            Snackbar.make(btnUpdateStatus,
                                    getString(R.string.status_updated), Snackbar.LENGTH_SHORT).show();
                        } else if (response.code() == 400) {
                            Snackbar.make(btnUpdateStatus,
                                    getString(R.string.error_invalid_transition), Snackbar.LENGTH_SHORT).show();
                        } else if (response.code() == 409) {
                            Snackbar.make(btnUpdateStatus,
                                    getString(R.string.error_student_not_confirmed), Snackbar.LENGTH_LONG).show();
                        } else {
                            Snackbar.make(btnUpdateStatus,
                                    getString(R.string.error_update_failed), Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                        Snackbar.make(btnUpdateStatus,
                                getString(R.string.error_network), Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    private android.graphics.drawable.Drawable getStatusDrawable(String status) {
        int drawableRes;
        if ("PENDING".equalsIgnoreCase(status)) {
            drawableRes = R.drawable.bg_badge_pending;
        } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            drawableRes = R.drawable.bg_badge_inprogress;
        } else if ("RESOLVED".equalsIgnoreCase(status)) {
            drawableRes = R.drawable.bg_badge_resolved;
        } else {
            drawableRes = R.drawable.bg_badge_closed;
        }
        return getDrawable(drawableRes);
    }
}
