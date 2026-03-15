package com.example.ucms_android.ui.student;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ucms_android.MainActivity;
import com.example.ucms_android.R;
import com.example.ucms_android.auth.EmailVerificationActivity;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.AttachmentResponse;
import com.example.ucms_android.model.Category;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.model.TicketRequest;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.CategoryService;
import com.example.ucms_android.network.TicketService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubmitTicketFragment extends Fragment {

    private Spinner spinnerCategory;
    private EditText etTitle, etDescription;
    private MaterialCardView cvAttachment;
    private MaterialButton btnSubmit;
    private TextView tvAttachmentName, tvAttachmentHint;
    private ProgressBar progressSubmit;
    private TextView tvSubmitStatus;

    private TicketService ticketService;
    private CategoryService categoryService;
    private List<Category> categories = new ArrayList<>();
    private Uri selectedFileUri;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    // Persist permission across process restarts
                    requireContext().getContentResolver()
                            .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    String segment = uri.getLastPathSegment();
                    tvAttachmentName.setText(segment != null ? segment : "file");
                    tvAttachmentName.setVisibility(View.VISIBLE);
                    tvAttachmentHint.setVisibility(View.GONE);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_submit_ticket, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerCategory = view.findViewById(R.id.spinnerCategory);
        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        cvAttachment = view.findViewById(R.id.cvAttachment);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        tvAttachmentName = view.findViewById(R.id.tvAttachmentName);
        tvAttachmentHint = view.findViewById(R.id.tvAttachmentHint);
        progressSubmit = view.findViewById(R.id.progressSubmit);
        tvSubmitStatus = view.findViewById(R.id.tvSubmitStatus);

        ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);
        categoryService = ApiClient.getInstance(requireContext()).create(CategoryService.class);

        // Restrict to images and PDF only — matches backend allowed MIME types
        cvAttachment.setOnClickListener(v ->
                filePickerLauncher.launch(new String[]{"image/jpeg", "image/png", "application/pdf"}));
        btnSubmit.setOnClickListener(v -> submitTicket());

        loadCategories();
    }

    private void setSubmitState(String state) {
        switch (state) {
            case "IDLE":
                btnSubmit.setEnabled(true);
                btnSubmit.setText(getString(R.string.submit_new_concern));
                progressSubmit.setVisibility(View.GONE);
                tvSubmitStatus.setVisibility(View.GONE);
                break;
            case "SUBMITTING":
                btnSubmit.setEnabled(false);
                btnSubmit.setText("Submitting...");
                progressSubmit.setVisibility(View.VISIBLE);
                tvSubmitStatus.setVisibility(View.VISIBLE);
                tvSubmitStatus.setText("Creating your ticket...");
                break;
            case "UPLOADING":
                btnSubmit.setEnabled(false);
                btnSubmit.setText("Submitting...");
                progressSubmit.setVisibility(View.VISIBLE);
                tvSubmitStatus.setVisibility(View.VISIBLE);
                tvSubmitStatus.setText("Uploading attachment...");
                break;
        }
    }

    private void loadCategories() {
        categoryService.getCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call,
                                   Response<ApiResponse<List<Category>>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    categories = response.body().getData();
                    ArrayAdapter<Category> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            categories);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerCategory.setAdapter(adapter);
                } else if (isAdded()) {
                    Toast.makeText(requireContext(),
                            getString(R.string.error_loading_categories), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Category>>> call, Throwable t) {
                if (isAdded()) {
                    Toast.makeText(requireContext(),
                            getString(R.string.error_network), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void submitTicket() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        boolean valid = true;
        if (title.isEmpty()) {
            etTitle.setError(getString(R.string.error_title_required));
            valid = false;
        }
        if (description.isEmpty()) {
            etDescription.setError(getString(R.string.error_description_required));
            valid = false;
        }
        if (categories.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.error_loading_categories), Toast.LENGTH_SHORT).show();
            valid = false;
        }
        if (!valid) return;

        if (selectedFileUri != null) {
            ContentResolver resolver = requireContext().getContentResolver();
            android.database.Cursor cursor = resolver.query(selectedFileUri, null, null, null, null);
            if (cursor != null) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                cursor.moveToFirst();
                long fileSize = cursor.getLong(sizeIndex);
                cursor.close();
                long maxSize = 10 * 1024 * 1024; // 10MB
                if (fileSize > maxSize) {
                    Snackbar.make(btnSubmit,
                            "File is too large. Maximum allowed size is 10MB.",
                            Snackbar.LENGTH_LONG).show();
                    return;
                }
            }
        }

        Long categoryId = categories.get(spinnerCategory.getSelectedItemPosition()).getId();
        TicketRequest request = new TicketRequest(title, description, categoryId);

        setSubmitState("SUBMITTING");

        ticketService.createTicket(request).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Ticket ticket = response.body().getData();
                    if (selectedFileUri != null && ticket != null) {
                        uploadAttachment(ticket.getId());
                    } else {
                        onSubmitComplete();
                    }
                } else if (response.code() == 403) {
                    setSubmitState("IDLE");
                    startActivity(new Intent(requireContext(), EmailVerificationActivity.class));
                } else {
                    setSubmitState("IDLE");
                    Snackbar.make(btnSubmit, getString(R.string.error_submit_failed), Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                if (isAdded()) {
                    setSubmitState("IDLE");
                    Snackbar.make(btnSubmit, getString(R.string.error_network), Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void uploadAttachment(Long ticketId) {
        setSubmitState("UPLOADING");
        try {
            ContentResolver resolver = requireContext().getContentResolver();
            String mimeType = resolver.getType(selectedFileUri);
            if (mimeType == null) mimeType = "application/octet-stream";

            InputStream inputStream = resolver.openInputStream(selectedFileUri);
            if (inputStream == null) {
                onSubmitComplete(); // ticket created, skip attachment silently
                return;
            }

            byte[] bytes;
            try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[4096];
                int n;
                while ((n = inputStream.read(chunk)) != -1) {
                    buffer.write(chunk, 0, n);
                }
                bytes = buffer.toByteArray();
            }
            inputStream.close();

            String filename = selectedFileUri.getLastPathSegment();
            if (filename == null) filename = "file";

            RequestBody requestBody = RequestBody.create(bytes, MediaType.parse(mimeType));
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", filename, requestBody);

            ticketService.uploadAttachment(ticketId, filePart).enqueue(new Callback<ApiResponse<AttachmentResponse>>() {
                @Override
                public void onResponse(Call<ApiResponse<AttachmentResponse>> call,
                                       Response<ApiResponse<AttachmentResponse>> response) {
                    if (!isAdded()) return;
                    // Attachment upload result is non-blocking — navigate regardless
                    if (!response.isSuccessful()) {
                        Toast.makeText(requireContext(),
                                "Ticket submitted but attachment failed to upload.", Toast.LENGTH_SHORT).show();
                    }
                    setSubmitState("IDLE");
                    onSubmitComplete();
                }

                @Override
                public void onFailure(Call<ApiResponse<AttachmentResponse>> call, Throwable t) {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                            "Ticket submitted but attachment failed to upload.", Toast.LENGTH_SHORT).show();
                    setSubmitState("IDLE");
                    onSubmitComplete();
                }
            });

        } catch (Exception e) {
            onSubmitComplete(); // ticket created, skip attachment on error
        }
    }

    private void onSubmitComplete() {
        if (!isAdded()) return;
        Toast.makeText(requireContext(), getString(R.string.ticket_submitted), Toast.LENGTH_SHORT).show();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(new StudentHomeFragment());
        }
    }
}
