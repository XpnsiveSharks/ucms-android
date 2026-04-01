package com.example.ucms_android.ui.common;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.AnalyticsOverview;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.CategoryCount;
import com.example.ucms_android.model.DailyTicketVolume;
import com.example.ucms_android.network.AnalyticsService;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.ui.view.ThreeDBarView;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalyticsFragment extends Fragment {

    private ShimmerFrameLayout shimmerAnalytics;
    private View nestedScrollView;
    private View layoutError;
    private TextView tvResolutionRate, tvResolutionTrend;
    private TextView tvAvgWaitTime, tvWaitTimeTrend;
    private ViewGroup llCategoryChart, llTimelineChart, llStatusDistribution;
    private RecyclerView rvCategoryBreakdown;

    private AnalyticsService analyticsService;
    private CategoryBreakdownAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shimmerAnalytics = view.findViewById(R.id.shimmerAnalytics);
        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        layoutError = view.findViewById(R.id.layoutError);
        tvResolutionRate = view.findViewById(R.id.tvResolutionRate);
        tvResolutionTrend = view.findViewById(R.id.tvResolutionTrend);
        tvAvgWaitTime = view.findViewById(R.id.tvAvgWaitTime);
        tvWaitTimeTrend = view.findViewById(R.id.tvWaitTimeTrend);
        llCategoryChart = view.findViewById(R.id.llCategoryChart);
        llTimelineChart = view.findViewById(R.id.llTimelineChart);
        llStatusDistribution = view.findViewById(R.id.llStatusDistribution);
        rvCategoryBreakdown = view.findViewById(R.id.rvCategoryBreakdown);

        view.findViewById(R.id.btnRetry).setOnClickListener(v -> fetchAnalytics());

        analyticsService = ApiClient.getInstance(requireContext()).create(AnalyticsService.class);
        
        adapter = new CategoryBreakdownAdapter(new ArrayList<>());
        rvCategoryBreakdown.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategoryBreakdown.setAdapter(adapter);

        fetchAnalytics();
    }

    private void fetchAnalytics() {
        showLoading(true);
        layoutError.setVisibility(View.GONE);

        analyticsService.getOverview().enqueue(new Callback<ApiResponse<AnalyticsOverview>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AnalyticsOverview>> call, @NonNull Response<ApiResponse<AnalyticsOverview>> response) {
                if (isAdded()) {
                    showLoading(false);
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        bindOverview(response.body().getData());
                    } else {
                        handleFetchError("Server returned an empty response.");
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AnalyticsOverview>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    showLoading(false);
                    handleFetchError("Network failure: " + t.getLocalizedMessage());
                }
            }
        });
    }

    private void handleFetchError(String message) {
        layoutError.setVisibility(View.VISIBLE);
        nestedScrollView.setVisibility(View.GONE);
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        
        // FOR DEBUGGING/OFFLINE: Still try to show mock data if you want to see the UI
        // bindOverview(createMockData());
    }

    private void bindOverview(AnalyticsOverview overview) {
        if (overview == null) return;
        layoutError.setVisibility(View.GONE);
        nestedScrollView.setVisibility(View.VISIBLE);

        // 1. Top Metrics
        tvResolutionRate.setText(String.format(Locale.getDefault(), "%.1f%%", overview.getResolutionRate()));
        String resTrendSign = overview.getResolutionTrend() >= 0 ? "+" : "";
        tvResolutionTrend.setText(String.format(Locale.getDefault(), "%s%.1f%% vs last week", resTrendSign, overview.getResolutionTrend()));
        tvResolutionTrend.setTextColor(overview.getResolutionTrend() >= 0 ? getResources().getColor(R.color.dm_mint_green) : getResources().getColor(R.color.colorError));

        tvAvgWaitTime.setText(String.format(Locale.getDefault(), "%.1fh", overview.getAverageWaitTimeHours()));
        String waitTrendSign = overview.getAverageWaitTimeTrendHours() >= 0 ? "+" : "";
        tvWaitTimeTrend.setText(String.format(Locale.getDefault(), "%s%.1fh vs last week", waitTrendSign, overview.getAverageWaitTimeTrendHours()));
        tvWaitTimeTrend.setTextColor(overview.getAverageWaitTimeTrendHours() <= 0 ? getResources().getColor(R.color.dm_mint_green) : getResources().getColor(R.color.dm_orange_peach));

        // 2. Timeline Chart
        if (overview.getTicketVolumeLast7Days() != null) {
            bindTimelineChart(overview.getTicketVolumeLast7Days());
        }

        // 3. Category 3D Chart
        if (overview.getCategoryBreakdown() != null) {
            bindCategoryChart(overview.getCategoryBreakdown());
            adapter.setCategories(overview.getCategoryBreakdown());
        }
        
        // 4. Status Distribution
        bindStatusDistribution(overview);
    }

    private void bindTimelineChart(List<DailyTicketVolume> volume) {
        if (llTimelineChart == null || volume == null) return;
        llTimelineChart.removeAllViews();

        long maxCount = 0;
        for (DailyTicketVolume d : volume) {
            if (d.getTicketCount() > maxCount) maxCount = d.getTicketCount();
        }

        for (DailyTicketVolume d : volume) {
            View bar = new View(requireContext());
            int heightPx = maxCount > 0 ? (int) (dpToPx(120) * (d.getTicketCount() / (double) maxCount)) : 0;
            if (d.getTicketCount() > 0) heightPx = Math.max(heightPx, dpToPx(10));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, heightPx, 1f);
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            bar.setLayoutParams(params);
            bar.setBackgroundResource(R.drawable.bg_button_pill);
            bar.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorSecondary)));
            bar.setAlpha(0.8f);
            
            llTimelineChart.addView(bar);
        }
    }

    private void bindCategoryChart(List<CategoryCount> categories) {
        if (categories == null || categories.isEmpty() || llCategoryChart == null) return;
        llCategoryChart.removeAllViews();

        List<CategoryCount> sorted = new ArrayList<>(categories);
        Collections.sort(sorted, (c1, c2) -> Long.compare(c2.getTicketCount(), c1.getTicketCount()));

        long maxCount = 0;
        for (CategoryCount v : sorted) {
            if (v.getTicketCount() > maxCount) maxCount = v.getTicketCount();
        }

        int maxBarHeightDp = 140; 
        int[] barColors = {0xFFF77F00, 0xFFFCBF49, 0xFF10B981, 0xFF2196F3, 0xFF9C27B0, 0xFF56CCF2, 0xFFBB6BD9};

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < sorted.size(); i++) {
            CategoryCount data = sorted.get(i);
            View barItem = inflater.inflate(R.layout.item_chart_bar, llCategoryChart, false);
            ThreeDBarView vBar = barItem.findViewById(R.id.vBar);
            TextView tvDay = barItem.findViewById(R.id.tvDay);

            if (vBar != null && tvDay != null) {
                ViewGroup.LayoutParams params = vBar.getLayoutParams();
                int heightDp = maxCount > 0 ? (int) (maxBarHeightDp * (data.getTicketCount() / (double) maxCount)) : 0;
                if (data.getTicketCount() > 0) heightDp = Math.max(heightDp, 25); // Minimum height for 3D visibility
                params.height = dpToPx(heightDp);
                vBar.setLayoutParams(params);
                vBar.setBarColor(barColors[i % barColors.length]);
                tvDay.setText(data.getCategoryName().toUpperCase());
            }
            llCategoryChart.addView(barItem);
            if (i < sorted.size() - 1) {
                View spacer = new View(requireContext());
                spacer.setLayoutParams(new LinearLayout.LayoutParams(dpToPx(24), 1));
                llCategoryChart.addView(spacer);
            }
        }
    }

    private void bindStatusDistribution(AnalyticsOverview data) {
        if (llStatusDistribution == null) return;
        llStatusDistribution.removeAllViews();

        addStatusItem("PENDING", data.getPendingCount(), data.getTotalTickets(), getResources().getColor(R.color.colorStatusPending));
        addStatusItem("IN PROGRESS", data.getInProgressCount(), data.getTotalTickets(), getResources().getColor(R.color.colorStatusInProgress));
        addStatusItem("RESOLVED", data.getResolvedCount(), data.getTotalTickets(), getResources().getColor(R.color.colorStatusResolved));
    }

    private void addStatusItem(String label, long count, long total, int color) {
        View item = LayoutInflater.from(requireContext()).inflate(R.layout.item_status_distribution, llStatusDistribution, false);
        TextView tvName = item.findViewById(R.id.tvStatusName);
        TextView tvCount = item.findViewById(R.id.tvStatusCount);
        LinearProgressIndicator progress = item.findViewById(R.id.progressStatus);

        tvName.setText(label);
        tvCount.setText(String.format(Locale.getDefault(), "%d logs", count));
        progress.setIndicatorColor(color);
        int percentage = total > 0 ? (int) ((count / (double) total) * 100) : 0;
        progress.setProgress(percentage);

        llStatusDistribution.addView(item);
    }

    private void showLoading(boolean loading) {
        if (shimmerAnalytics != null) {
            shimmerAnalytics.setVisibility(loading ? View.VISIBLE : View.GONE);
            if (loading) shimmerAnalytics.startShimmer();
            else shimmerAnalytics.stopShimmer();
        }
        if (nestedScrollView != null) {
            nestedScrollView.setVisibility(loading ? View.GONE : View.VISIBLE);
        }
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private static class CategoryBreakdownAdapter extends RecyclerView.Adapter<CategoryBreakdownAdapter.ViewHolder> {
        private final List<CategoryCount> categories;
        private final int[] colors = {0xFFF77F00, 0xFFFCBF49, 0xFF10B981, 0xFF2196F3, 0xFF9C27B0, 0xFF56CCF2, 0xFFBB6BD9};

        CategoryBreakdownAdapter(List<CategoryCount> categories) {
            this.categories = categories;
        }

        void setCategories(List<CategoryCount> newCategories) {
            this.categories.clear();
            if (newCategories != null) {
                this.categories.addAll(newCategories);
                Collections.sort(this.categories, (c1, c2) -> Long.compare(c2.getTicketCount(), c1.getTicketCount()));
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category_count, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CategoryCount category = categories.get(position);
            holder.tvName.setText(category.getCategoryName());
            holder.tvCount.setText(String.format(Locale.getDefault(), "%d logs", category.getTicketCount()));
            holder.vColor.setBackgroundColor(colors[position % colors.length]);
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            View vColor;
            TextView tvName, tvCount;

            ViewHolder(View itemView) {
                super(itemView);
                vColor = itemView.findViewById(R.id.vCategoryColor);
                tvName = itemView.findViewById(R.id.tvCategoryName);
                tvCount = itemView.findViewById(R.id.tvTicketCount);
            }
        }
    }
}
