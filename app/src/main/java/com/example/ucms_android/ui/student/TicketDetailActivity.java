package com.example.ucms_android.ui.student;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.model.TicketResponse;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.TicketService;
import com.example.ucms_android.ui.adapter.TicketResponseAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TicketDetailActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvTicketNumber, tvStatus, tvDate, tvTicketTitle,
            tvDescription, tvAttachmentLabel, tvAttachmentName;
    private ImageView ivAttachmentPreview;
    private RecyclerView rvResponses;
    private TicketResponseAdapter responseAdapter;
    private TicketService ticketService;
    private Long ticketId;
    private ProgressBar progressBar;
    private LinearLayout layoutError;
    private ScrollView scrollContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_detail);

        ticketId = getIntent().getLongExtra("ticketId", -1);
        if (ticketId == -1) { finish(); return; }

        bindViews();

        ticketService = ApiClient.getInstance(this).create(TicketService.class);

        btnBack.setOnClickListener(v -> finish());

        findViewById(R.id.btnRetry).setOnClickListener(v -> {
            loadTicket();
            loadResponses();
        });

        responseAdapter = new TicketResponseAdapter(new ArrayList<>());
        rvResponses.setLayoutManager(new LinearLayoutManager(this));
        rvResponses.setAdapter(responseAdapter);

        loadTicket();
        loadResponses();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTicketNumber = findViewById(R.id.tvTicketNumber);
        tvStatus = findViewById(R.id.tvStatus);
        tvDate = findViewById(R.id.tvDate);
        tvTicketTitle = findViewById(R.id.tvTicketTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvAttachmentLabel = findViewById(R.id.tvAttachmentLabel);
        tvAttachmentName = findViewById(R.id.tvAttachmentName);
        ivAttachmentPreview = findViewById(R.id.ivAttachmentPreview);
        rvResponses = findViewById(R.id.rvResponses);
        progressBar = findViewById(R.id.progressBar);
        layoutError = findViewById(R.id.layoutError);
        scrollContent = findViewById(R.id.scrollContent);
    }

    private void loadTicket() {
        showState("LOADING");
        ticketService.getTicketById(ticketId).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    populateViews(response.body().getData());
                    showState("DATA");
                } else {
                    showState("ERROR");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                showState("ERROR");
            }
        });
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

    private void populateViews(Ticket ticket) {
        tvTicketNumber.setText(ticket.getTicketNumber());
        tvStatus.setText(ticket.getStatus());
        tvDate.setText(ticket.getCreatedAt());
        tvTicketTitle.setText(ticket.getTitle());
        tvDescription.setText(ticket.getDescription());
        tvStatus.setBackground(getStatusDrawable(ticket.getStatus()));

        // Attachment info not in ticket response — hide section
        tvAttachmentLabel.setVisibility(View.GONE);
        ivAttachmentPreview.setVisibility(View.GONE);
        tvAttachmentName.setVisibility(View.GONE);
    }

    private void loadResponses() {
        ticketService.getTicketResponses(ticketId).enqueue(new Callback<ApiResponse<List<TicketResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<TicketResponse>>> call, Response<ApiResponse<List<TicketResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    responseAdapter.updateData(response.body().getData());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<TicketResponse>>> call, Throwable t) {
                // Silently fail — responses are supplementary
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
