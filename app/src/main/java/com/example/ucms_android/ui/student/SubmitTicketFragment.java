package com.example.ucms_android.ui.student;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubmitTicketFragment extends Fragment {

    private Spinner spinnerCategory;
    private EditText etTitle, etDescription;
    private MaterialCardView cvAttachment;
    private MaterialButton btnSubmit;
    private TextView tvAttachmentName, tvAttachmentHint;

    private TicketService ticketService;
    private CategoryService categoryService;
    private List<Category> categories = new ArrayList<>();
    private Uri selectedFileUri;

    private final ActivityResultLauncher<String> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedFileUri = uri;
                    tvAttachmentName.setText(uri.getLastPathSegment());
                    tvAttachmentName.setVisibility(View.VISIBLE);
                    tvAttachmentHint.setVisibility(View.GONE);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

        ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);
        categoryService = ApiClient.getInstance(requireContext()).create(CategoryService.class);

        cvAttachment.setOnClickListener(v -> filePickerLauncher.launch("*/*"));
        btnSubmit.setOnClickListener(v -> submitTicket());

        loadCategories();
    }

    private void loadCategories() {
        categoryService.getCategories().enqueue(new Callback<ApiResponse<List<Category>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Category>>> call, Response<ApiResponse<List<Category>>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null && response.body().getData() != null) {
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
        if (!valid) return;

        String category = categories.isEmpty() ? "" : categories.get(spinnerCategory.getSelectedItemPosition()).getName();
        TicketRequest request = new TicketRequest(title, description, category);

        ticketService.createTicket(request).enqueue(new Callback<ApiResponse<Ticket>>() {
            @Override
            public void onResponse(Call<ApiResponse<Ticket>> call, Response<ApiResponse<Ticket>> response) {
                if (!isAdded()) return;
                
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(requireContext(),
                            getString(R.string.ticket_submitted), Toast.LENGTH_SHORT).show();
                    // Go back to Home
                    if (getActivity() instanceof com.example.ucms_android.MainActivity) {
                        ((com.example.ucms_android.MainActivity) getActivity()).loadFragment(new StudentHomeFragment());
                    }
                } else if (response.code() == 403) {
                    startActivity(new Intent(requireContext(), EmailVerificationActivity.class));
                } else {
                    Snackbar.make(btnSubmit,
                            getString(R.string.error_submit_failed), Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Ticket>> call, Throwable t) {
                if (isAdded()) {
                    Snackbar.make(btnSubmit,
                            getString(R.string.error_network), Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }
}
