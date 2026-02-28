package com.example.ucms_android.ui.student;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ucms_android.R;
import com.example.ucms_android.auth.EmailVerificationActivity;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.Category;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.model.TicketRequest;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.CategoryService;
import com.example.ucms_android.network.TicketService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubmitTicketActivity extends AppCompatActivity {

    private Spinner spinnerCategory;
    private TextInputLayout tilTitle, tilDescription;
    private TextInputEditText etTitle, etDescription;
    private MaterialButton btnAttachment, btnSubmit;
    private TextView tvAttachmentName;

    private TicketService ticketService;
    private CategoryService categoryService;
    private List<Category> categories = new ArrayList<>();
    private Uri selectedFileUri;

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    tvAttachmentName.setText(uri.getLastPathSegment());
                    tvAttachmentName.setVisibility(android.view.View.VISIBLE);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_ticket);

        spinnerCategory = findViewById(R.id.spinnerCategory);
        tilTitle = findViewById(R.id.tilTitle);
        tilDescription = findViewById(R.id.tilDescription);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        btnAttachment = findViewById(R.id.btnAttachment);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvAttachmentName = findViewById(R.id.tvAttachmentName);

        ticketService = ApiClient.getInstance(this).create(TicketService.class);
        categoryService = ApiClient.getInstance(this).create(CategoryService.class);

        btnAttachment.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        btnSubmit.setOnClickListener(v -> submitTicket());

        loadCategories();
    }

    private void loadCategories() {
        categoryService.getCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    categories = response.body().getData();
                    ArrayAdapter<Category> adapter = new ArrayAdapter<>(
                            SubmitTicketActivity.this,
                            android.R.layout.simple_spinner_item,
                            categories);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(adapter);
                } else {
                    Toast.makeText(SubmitTicketActivity.this,
                            getString(R.string.error_loading_categories), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                Toast.makeText(SubmitTicketActivity.this,
                        getString(R.string.error_network), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitTicket() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        boolean valid = true;
        if (title.isEmpty()) {
            tilTitle.setError(getString(R.string.error_title_required));
            valid = false;
        } else {
            tilTitle.setError(null);
        }
        if (description.isEmpty()) {
            tilDescription.setError(getString(R.string.error_description_required));
            valid = false;
        } else {
            tilDescription.setError(null);
        }
        if (!valid) return;

        String category = categories.isEmpty() ? "" : categories.get(spinnerCategory.getSelectedItemPosition()).getName();
        TicketRequest request = new TicketRequest(title, description, category);

        ticketService.createTicket(request).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(SubmitTicketActivity.this,
                            getString(R.string.ticket_submitted), Toast.LENGTH_SHORT).show();
                    finish();
                } else if (response.code() == 403) {
                    startActivity(new Intent(SubmitTicketActivity.this, EmailVerificationActivity.class));
                } else {
                    Snackbar.make(btnSubmit,
                            getString(R.string.error_submit_failed), Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                Snackbar.make(btnSubmit,
                        getString(R.string.error_network), Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
