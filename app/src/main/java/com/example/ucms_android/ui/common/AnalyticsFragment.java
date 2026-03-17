package com.example.ucms_android.ui.common;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.AnalyticsSummary;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.CategoryCount;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.TicketService;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.adapter.CategoryCountAdapter;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalyticsFragment extends Fragment {

    private ShimmerFrameLayout shimmerSummary;
    private MaterialCardView cvUnresolved;
    private MaterialCardView cvCommonConcerns;
    private TextView tvTotalUnresolved;
    private TextView tvResolvedPct;
    private TextView tvPendingLegend;
    private TextView tvInProgressLegend;
    private View viewPendingBar;
    private View viewInProgressBar;
    private RecyclerView rvCategories;
    private CategoryCountAdapter categoryAdapter;

    private TicketService ticketService;
    private SessionManager sessionManager;
    private Gson gson;

    private AnalyticsSummary currentSummary;
    private List<CategoryCount> currentCategories = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shimmerSummary = view.findViewById(R.id.shimmerSummary);
        cvUnresolved = view.findViewById(R.id.cvUnresolved);
        cvCommonConcerns = view.findViewById(R.id.cvCommonConcerns);
        tvTotalUnresolved = view.findViewById(R.id.tvTotalUnresolved);
        tvResolvedPct = view.findViewById(R.id.tvResolvedPct);
        tvPendingLegend = view.findViewById(R.id.tvPendingLegend);
        tvInProgressLegend = view.findViewById(R.id.tvInProgressLegend);
        viewPendingBar = view.findViewById(R.id.viewPendingBar);
        viewInProgressBar = view.findViewById(R.id.viewInProgressBar);
        rvCategories = view.findViewById(R.id.rvCategories);

        ticketService = ApiClient.getInstance(requireContext()).create(TicketService.class);
        sessionManager = new SessionManager(requireContext());
        gson = new Gson();

        categoryAdapter = new CategoryCountAdapter(new ArrayList<>());
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategories.setAdapter(categoryAdapter);

        loadFromCache();
        fetchSummary();
        fetchByCategory();
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchSummary();
        fetchByCategory();
    }

    private void loadFromCache() {
        String summaryJson = sessionManager.getAnalyticsSummaryJson();
        String categoryJson = sessionManager.getAnalyticsByCategoryJson();

        if (summaryJson != null) {
            AnalyticsSummary cached = gson.fromJson(summaryJson, AnalyticsSummary.class);
            if (cached != null) {
                currentSummary = cached;
                populateSummary(cached);
                showData();
            }
        }

        if (categoryJson != null) {
            Type type = new TypeToken<List<CategoryCount>>() {}.getType();
            List<CategoryCount> cached = gson.fromJson(categoryJson, type);
            if (cached != null && !cached.isEmpty()) {
                currentCategories = cached;
                categoryAdapter.updateData(cached);
            }
        }

        if (summaryJson == null) {
            showShimmer();
        }
    }

    private void fetchSummary() {
        ticketService.getAnalyticsSummary().enqueue(new Callback<ApiResponse<AnalyticsSummary>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AnalyticsSummary>> call,
                                   @NonNull Response<ApiResponse<AnalyticsSummary>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    currentSummary = response.body().getData();
                    sessionManager.saveAnalyticsSummaryJson(gson.toJson(currentSummary));
                    populateSummary(currentSummary);
                    showData();
                } else {
                    if (cvUnresolved.getVisibility() != View.VISIBLE) hideShimmer();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AnalyticsSummary>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                if (cvUnresolved.getVisibility() != View.VISIBLE) hideShimmer();
            }
        });
    }

    private void fetchByCategory() {
        ticketService.getAnalyticsByCategory().enqueue(new Callback<ApiResponse<List<CategoryCount>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<CategoryCount>>> call,
                                   @NonNull Response<ApiResponse<List<CategoryCount>>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    currentCategories = response.body().getData();
                    sessionManager.saveAnalyticsByCategoryJson(gson.toJson(currentCategories));
                    categoryAdapter.updateData(currentCategories);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<CategoryCount>>> call, @NonNull Throwable t) {}
        });
    }

    private void populateSummary(AnalyticsSummary summary) {
        tvTotalUnresolved.setText(String.valueOf(summary.getUnresolvedCount()));
        tvResolvedPct.setText(String.format("%.0f%% resolved", summary.getResolvedPercentage()));

        long unresolved = summary.getUnresolvedCount();
        long pending = unresolved / 2;
        long inProgress = unresolved - pending;

        tvPendingLegend.setText(pending + " Pending");
        tvInProgressLegend.setText(inProgress + " In-Progress");

        android.widget.LinearLayout.LayoutParams pendingParams =
                (android.widget.LinearLayout.LayoutParams) viewPendingBar.getLayoutParams();
        pendingParams.weight = pending > 0 ? pending : 1;
        viewPendingBar.setLayoutParams(pendingParams);

        android.widget.LinearLayout.LayoutParams inProgressParams =
                (android.widget.LinearLayout.LayoutParams) viewInProgressBar.getLayoutParams();
        inProgressParams.weight = inProgress > 0 ? inProgress : 1;
        viewInProgressBar.setLayoutParams(inProgressParams);
    }

    private void showShimmer() {
        shimmerSummary.setVisibility(View.VISIBLE);
        shimmerSummary.startShimmer();
        cvUnresolved.setVisibility(View.GONE);
        cvCommonConcerns.setVisibility(View.GONE);
    }

    private void hideShimmer() {
        shimmerSummary.stopShimmer();
        shimmerSummary.setVisibility(View.GONE);
    }

    private void showData() {
        hideShimmer();
        cvUnresolved.setVisibility(View.VISIBLE);
        cvCommonConcerns.setVisibility(View.VISIBLE);
    }
}
