package com.example.ucms_android.ui.common;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.CategoryCount;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.model.auth.AnalyticsSummary;
import com.example.ucms_android.network.AnalyticsService;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.ui.auth.LoginActivity;
import com.example.ucms_android.util.AnalyticsCache;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalyticsFragment extends Fragment {

    private NestedScrollView nestedScrollView;
    private com.facebook.shimmer.ShimmerFrameLayout shimmerLayout;
    private MaterialCardView cvUnresolved;
    private MaterialCardView cvCommonConcerns;
    private MaterialCardView cvCategoryTrends;
    private TextView tvAdminName;
    private TextView tvAvatar;
    private TextView tvTotalUnresolved;
    private TextView tvPendingCount;
    private TextView tvInProgressCount;
    private LinearLayout llProgressBar;
    private View vPendingBar;
    private View vInProgressBar;
    private TextView tvCat1Name;
    private TextView tvCat1Percent;
    private View vCat1Bar;
    private TextView tvCat2Name;
    private TextView tvCat2Percent;
    private View vCat2Bar;
    private TextView tvCat3Name;
    private TextView tvCat3Percent;
    private View vCat3Bar;
    private LinearLayout llBarChart;

    private AnalyticsService analyticsService;
    private SessionManager sessionManager;
    private AnalyticsCache analyticsCache;

    private AnalyticsSummary summary;
    private List<CategoryCount> categoryCounts = new ArrayList<>();
    private List<Ticket> unresolvedTickets = new ArrayList<>();
    private int completedCalls;
    private String firstErrorMessage;
    private boolean isFetchingWithShimmer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        shimmerLayout = view.findViewById(R.id.shimmerLayout);
        cvUnresolved = view.findViewById(R.id.cvUnresolved);
        cvCommonConcerns = view.findViewById(R.id.cvCommonConcerns);
        cvCategoryTrends = view.findViewById(R.id.cvCategoryTrends);
        tvAdminName = view.findViewById(R.id.tvAdminName);
        tvAvatar = view.findViewById(R.id.tvAvatar);
        tvTotalUnresolved = view.findViewById(R.id.tvTotalUnresolved);
        tvPendingCount = view.findViewById(R.id.tvPendingCount);
        tvInProgressCount = view.findViewById(R.id.tvInProgressCount);
        llProgressBar = view.findViewById(R.id.llProgressBar);
        vPendingBar = llProgressBar.getChildAt(0);
        vInProgressBar = llProgressBar.getChildAt(1);
        tvCat1Name = view.findViewById(R.id.tvCat1Name);
        tvCat1Percent = view.findViewById(R.id.tvCat1Percent);
        vCat1Bar = view.findViewById(R.id.vCat1Bar);
        tvCat2Name = view.findViewById(R.id.tvCat2Name);
        tvCat2Percent = view.findViewById(R.id.tvCat2Percent);
        vCat2Bar = view.findViewById(R.id.vCat2Bar);
        tvCat3Name = view.findViewById(R.id.tvCat3Name);
        tvCat3Percent = view.findViewById(R.id.tvCat3Percent);
        vCat3Bar = view.findViewById(R.id.vCat3Bar);
        llBarChart = view.findViewById(R.id.llBarChart);

        analyticsService = ApiClient.getInstance(requireContext()).create(AnalyticsService.class);
        sessionManager = new SessionManager(requireContext());
        analyticsCache = AnalyticsCache.getInstance();

        bindHeader();
        if (analyticsCache.hasData()) {
            summary = analyticsCache.getSummary();
            categoryCounts = analyticsCache.getCategoryCounts();
            unresolvedTickets = analyticsCache.getUnresolvedTickets();
            nestedScrollView.setVisibility(View.VISIBLE);
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
            bindUnresolvedCard();
            bindCategoryCards();
            fetchAnalytics(false);
        } else {
            showLoading(true);
            fetchAnalytics(true);
        }
    }

    private void bindHeader() {
        String name = sessionManager.getCachedName();
        if (name == null || name.trim().isEmpty()) {
            tvAdminName.setText("Admin User");
            tvAvatar.setText(getString(R.string.avatar_initials));
            return;
        }
        tvAdminName.setText(name);
        tvAvatar.setText(getInitials(name));
    }

    private void fetchAnalytics(boolean showShimmer) {
        isFetchingWithShimmer = showShimmer;
        completedCalls = 0;
        firstErrorMessage = null;
        if (showShimmer) {
            summary = null;
            categoryCounts = new ArrayList<>();
            unresolvedTickets = new ArrayList<>();
        }

        analyticsService.getSummary().enqueue(new Callback<ApiResponse<AnalyticsSummary>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AnalyticsSummary>> call,
                                   @NonNull Response<ApiResponse<AnalyticsSummary>> response) {
                if (!isAdded()) {
                    return;
                }

                String errorMessage = extractErrorMessage(response);
                if (errorMessage == null) {
                    summary = response.body() != null ? response.body().getData() : null;
                } else {
                    recordError(errorMessage);
                }
                onRequestFinished();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AnalyticsSummary>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                recordError(getString(R.string.error_no_connection));
                onRequestFinished();
            }
        });

        analyticsService.getByCategory().enqueue(new Callback<ApiResponse<List<CategoryCount>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<CategoryCount>>> call,
                                   @NonNull Response<ApiResponse<List<CategoryCount>>> response) {
                if (!isAdded()) {
                    return;
                }

                String errorMessage = extractErrorMessage(response);
                if (errorMessage == null) {
                    if (response.body() != null && response.body().getData() != null) {
                        categoryCounts = response.body().getData();
                    }
                } else {
                    recordError(errorMessage);
                }
                onRequestFinished();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<CategoryCount>>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                recordError(getString(R.string.error_no_connection));
                onRequestFinished();
            }
        });

        analyticsService.getUnresolved().enqueue(new Callback<ApiResponse<List<Ticket>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Ticket>>> call,
                                   @NonNull Response<ApiResponse<List<Ticket>>> response) {
                if (!isAdded()) {
                    return;
                }

                String errorMessage = extractErrorMessage(response);
                if (errorMessage == null) {
                    if (response.body() != null && response.body().getData() != null) {
                        unresolvedTickets = response.body().getData();
                    }
                } else {
                    recordError(errorMessage);
                }
                onRequestFinished();
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Ticket>>> call, @NonNull Throwable t) {
                if (!isAdded()) {
                    return;
                }
                recordError(getString(R.string.error_no_connection));
                onRequestFinished();
            }
        });
    }

    private void onRequestFinished() {
        completedCalls++;
        if (completedCalls != 3) {
            return;
        }

        if (firstErrorMessage == null && summary != null) {
            analyticsCache.store(summary, categoryCounts, unresolvedTickets);
        }

        if (isFetchingWithShimmer) {
            showLoading(false);
        }

        bindUnresolvedCard();
        bindCategoryCards();

        if (firstErrorMessage != null) {
            android.util.Log.e("AnalyticsFragment", "Error: " + firstErrorMessage);
            Toast.makeText(requireContext(), firstErrorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void recordError(String message) {
        android.util.Log.e("AnalyticsFragment", "recordError called with: " + message);
        if (firstErrorMessage == null && message != null && !message.isEmpty()) {
            firstErrorMessage = message;
        }
    }

    private String extractErrorMessage(Response<? extends ApiResponse<?>> response) {
        if (response.isSuccessful()) {
            ApiResponse<?> body = response.body();
            if (body == null) {
                return null;
            }
            if (!body.isSuccess()) {
                if (body.getMessage() != null && !body.getMessage().isEmpty()) {
                    return body.getMessage();
                }
                return "Something went wrong, please try again";
            }
            return null;
        }

        int code = response.code();
        String errorBody = readErrorBody(response);

        if (code == 401) {
            sessionManager.clearSession();
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
            return "Session expired. Please login again.";
        }

        if (code == 403) {
            if (errorBody != null && errorBody.contains("ACCOUNT_LIMITED")) {
                return getString(R.string.account_limited_prompt);
            }
            return getString(R.string.access_denied);
        }

        String errorMessage = extractMessageFromErrorBody(errorBody);

        if (code == 400 && errorMessage != null && !errorMessage.isEmpty()) {
            return errorMessage;
        }

        if (code >= 500) {
            return "Something went wrong, please try again";
        }

        if (errorMessage != null && !errorMessage.isEmpty()) {
            return errorMessage;
        }

        return "Something went wrong, please try again";
    }

    private String readErrorBody(Response<? extends ApiResponse<?>> response) {
        if (response.errorBody() == null) {
            return null;
        }
        try {
            return response.errorBody().string();
        } catch (IOException ignored) {
            return null;
        }
    }

    private String extractMessageFromErrorBody(String errorBody) {
        if (errorBody == null || errorBody.isEmpty()) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(errorBody);
            String message = jsonObject.optString("message", null);
            if (message != null && !message.isEmpty()) {
                return message;
            }
            return null;
        } catch (JSONException ignored) {
            return null;
        }
    }

    private void bindUnresolvedCard() {
        long unresolvedCount = summary != null ? summary.getUnresolvedCount() : 0;
        tvTotalUnresolved.setText(String.valueOf(unresolvedCount));

        int pending = 0;
        int inProgress = 0;
        for (Ticket ticket : unresolvedTickets) {
            String status = ticket.getStatus();
            if ("PENDING".equalsIgnoreCase(status)) {
                pending++;
            } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
                inProgress++;
            }
        }

        tvPendingCount.setText(String.format(Locale.getDefault(), "%d Pending", pending));
        tvInProgressCount.setText(String.format(Locale.getDefault(), "%d In-Progress", inProgress));
        updateUnresolvedProgress(pending, inProgress);
    }

    private void updateUnresolvedProgress(int pending, int inProgress) {
        LinearLayout.LayoutParams pendingParams = (LinearLayout.LayoutParams) vPendingBar.getLayoutParams();
        LinearLayout.LayoutParams inProgressParams = (LinearLayout.LayoutParams) vInProgressBar.getLayoutParams();

        int total = pending + inProgress;
        if (total == 0) {
            llProgressBar.setWeightSum(2f);
            pendingParams.weight = 1f;
            inProgressParams.weight = 1f;
        } else {
            llProgressBar.setWeightSum(total);
            pendingParams.weight = pending;
            inProgressParams.weight = inProgress;
        }

        vPendingBar.setLayoutParams(pendingParams);
        vInProgressBar.setLayoutParams(inProgressParams);
    }

    private void bindCategoryCards() {
        if (categoryCounts == null || categoryCounts.isEmpty()) {
            cvCommonConcerns.setVisibility(View.GONE);
            cvCategoryTrends.setVisibility(View.GONE);
            return;
        }

        cvCommonConcerns.setVisibility(View.VISIBLE);
        cvCategoryTrends.setVisibility(View.VISIBLE);

        List<CategoryCount> sortedCategories = new ArrayList<>(categoryCounts);
        sortedCategories.sort(Comparator.comparingLong(CategoryCount::getTicketCount).reversed());

        long totalTickets = summary != null ? summary.getTotalTickets() : 0;
        if (totalTickets <= 0) {
            for (CategoryCount categoryCount : sortedCategories) {
                totalTickets += categoryCount.getTicketCount();
            }
        }

        bindTopCategoryItem(0, sortedCategories, totalTickets);
        bindTopCategoryItem(1, sortedCategories, totalTickets);
        bindTopCategoryItem(2, sortedCategories, totalTickets);
        bindCategoryChart(sortedCategories);
    }

    private void bindTopCategoryItem(int index, List<CategoryCount> sortedCategories, long totalTickets) {
        TextView nameView;
        TextView percentView;
        View barView;

        if (index == 0) {
            nameView = tvCat1Name;
            percentView = tvCat1Percent;
            barView = vCat1Bar;
        } else if (index == 1) {
            nameView = tvCat2Name;
            percentView = tvCat2Percent;
            barView = vCat2Bar;
        } else {
            nameView = tvCat3Name;
            percentView = tvCat3Percent;
            barView = vCat3Bar;
        }

        if (index >= sortedCategories.size()) {
            nameView.setText("No data");
            percentView.setText("0%");
            updateTopCategoryBar(barView, 0f);
            return;
        }

        CategoryCount category = sortedCategories.get(index);
        long count = category.getTicketCount();
        float percentage = totalTickets > 0 ? (count * 100f) / totalTickets : 0f;

        String categoryName = category.getCategoryName();
        if (categoryName == null || categoryName.trim().isEmpty()) {
            categoryName = "Uncategorized";
        }

        nameView.setText(categoryName);
        percentView.setText(String.format(Locale.getDefault(), "%d%%", Math.round(percentage)));
        updateTopCategoryBar(barView, percentage);
    }

    private void updateTopCategoryBar(View barView, float percentage) {
        View parent = (View) barView.getParent();
        parent.post(() -> {
            int maxWidth = parent.getWidth();
            int minWidth = dpToPx(8);
            int width = (int) (maxWidth * Math.min(100f, Math.max(0f, percentage)) / 100f);
            if (percentage > 0f) {
                width = Math.max(width, minWidth);
            }

            ViewGroup.LayoutParams layoutParams = barView.getLayoutParams();
            layoutParams.width = width;
            barView.setLayoutParams(layoutParams);
        });
    }

    private void bindCategoryChart(List<CategoryCount> sortedCategories) {
        llBarChart.removeAllViews();
        if (sortedCategories.isEmpty()) {
            return;
        }

        long maxCount = 0;
        for (CategoryCount categoryCount : sortedCategories) {
            if (categoryCount.getTicketCount() > maxCount) {
                maxCount = categoryCount.getTicketCount();
            }
        }

        int maxHeightPx = dpToPx(160);
        for (int i = 0; i < sortedCategories.size(); i++) {
            CategoryCount category = sortedCategories.get(i);
            int barHeight = maxCount > 0
                    ? (int) ((category.getTicketCount() * 1f / maxCount) * maxHeightPx)
                    : dpToPx(4);
            barHeight = Math.max(barHeight, dpToPx(4));

            LinearLayout columnContainer = new LinearLayout(requireContext());
            columnContainer.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams columnParams = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1f
            );
            int margin = dpToPx(8);
            columnParams.setMargins(margin, 0, margin, 0);
            columnContainer.setLayoutParams(columnParams);

            FrameLayout chartArea = new FrameLayout(requireContext());
            LinearLayout.LayoutParams chartAreaParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
            );
            chartArea.setLayoutParams(chartAreaParams);

            View bar = new View(requireContext());
            FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    barHeight,
                    Gravity.BOTTOM
            );
            bar.setLayoutParams(barParams);
            int barColor = ContextCompat.getColor(requireContext(),
                    i % 2 == 0 ? R.color.colorSecondary : R.color.colorPrimary);
            bar.setBackgroundColor(barColor);

            chartArea.addView(bar);

            if (barHeight >= dpToPx(20)) {
                TextView countBadge = new TextView(requireContext());
                FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                        dpToPx(24),
                        dpToPx(24),
                        Gravity.TOP | Gravity.CENTER_HORIZONTAL
                );
                countBadge.setLayoutParams(badgeParams);
                countBadge.setText(String.valueOf(category.getTicketCount()));
                countBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
                countBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                countBadge.setTypeface(countBadge.getTypeface(), Typeface.BOLD);
                countBadge.setGravity(Gravity.CENTER);
                countBadge.setBackgroundResource(R.drawable.bg_button_pill);
                countBadge.getBackground().setTint(barColor);
                chartArea.addView(countBadge);
            }

            String rawCategoryName = category.getCategoryName();
            String categoryLabel = "Uncategorized";
            if (rawCategoryName != null && !rawCategoryName.trim().isEmpty()) {
                String[] words = rawCategoryName.trim().split("\\s+");
                if (words.length > 1) {
                    categoryLabel = words[0] + "...";
                } else {
                    categoryLabel = words[0];
                }
            }

            TextView labelView = new TextView(requireContext());
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            labelParams.topMargin = dpToPx(4);
            labelView.setLayoutParams(labelParams);
            labelView.setText(categoryLabel);
            labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9f);
            labelView.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSecondary));
            labelView.setGravity(Gravity.CENTER_HORIZONTAL);
            labelView.setMaxLines(2);
            labelView.setEllipsize(TextUtils.TruncateAt.END);

            columnContainer.addView(chartArea);
            columnContainer.addView(labelView);
            llBarChart.addView(columnContainer);
        }
    }

    private void showLoading(boolean loading) {
        if (loading) {
            shimmerLayout.setVisibility(View.VISIBLE);
            shimmerLayout.startShimmer();
            nestedScrollView.setVisibility(View.GONE);
        } else {
            shimmerLayout.stopShimmer();
            shimmerLayout.setVisibility(View.GONE);
            nestedScrollView.setVisibility(View.VISIBLE);
        }
        if (loading) {
            cvCommonConcerns.setVisibility(View.GONE);
            cvCategoryTrends.setVisibility(View.GONE);
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private String getInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.getDefault());
        }
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase(Locale.getDefault());
    }
}
