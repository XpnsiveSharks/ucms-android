package com.example.ucms_android.ui.admin;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.TicketService;
import com.example.ucms_android.util.DateFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminTicketDetailActivity extends AppCompatActivity {

    private TextView tvTicketId, tvStatus, tvAvatarInitials, tvStudentName, tvStudentId, tvStudentCourse;
    private TextView tvDate, tvTitle, tvDescription, tvAttachmentName, tvActionTitle, tvSelectedCategory;
    private MaterialCardView btnBack, cvAttachment;
    private MaterialButton btnUpdateStatus;
    private TicketService ticketService;
    private Long ticketId;

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
        
        btnBack.setOnClickListener(v -> finish());
        
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
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
    }

    private void loadTicketDetails() {
        ticketService.getTicketById(ticketId).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Ticket>> call, @NonNull Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    displayTicket(response.body().getData());
                } else {
                    Toast.makeText(AdminTicketDetailActivity.this, "Failed to load ticket", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Ticket>> call, @NonNull Throwable t) {
                Toast.makeText(AdminTicketDetailActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayTicket(Ticket ticket) {
        tvTicketId.setText(ticket.getTicketNumber() != null ? "Ticket " + ticket.getTicketNumber() : "Ticket #" + ticket.getId());
        tvStatus.setText(ticket.getStatus());
        tvStatus.setBackgroundResource(getStatusBackgroundResource(ticket.getStatus()));
        
        String studentName = ticket.getStudentName() != null ? ticket.getStudentName() : "Unknown Student";
        tvStudentName.setText(studentName);
        tvAvatarInitials.setText(getInitials(studentName));
        tvStudentId.setText("School ID: " + (ticket.getStudentId() != null ? ticket.getStudentId() : "N/A"));
        tvStudentCourse.setText(ticket.getCourseYear() != null ? ticket.getCourseYear() : "N/A");
        
        tvDate.setText(DateFormatter.formatDate(ticket.getCreatedAt()));
        tvTitle.setText(ticket.getTitle());
        tvDescription.setText(ticket.getDescription());
        
        if (ticket.getAttachmentUrl() != null && !ticket.getAttachmentUrl().isEmpty()) {
            cvAttachment.setVisibility(View.VISIBLE);
            tvAttachmentName.setText(ticket.getAttachmentName() != null ? ticket.getAttachmentName() : "Attached File");
        } else {
            cvAttachment.setVisibility(View.GONE);
        }
        
        tvActionTitle.setText(ticket.getCategory() + " Actions");
        tvSelectedCategory.setText(ticket.getCategory());
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
