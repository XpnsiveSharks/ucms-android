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
    private TextView tvResolutionRate;
    private TextView tvResolutionTrend;
    private TextView tvAvgWaitTime;
    private TextView tvWaitTimeTrend;
    private RecyclerView rvCategoryBreakdown;
    private ViewGroup llCategoryChart;

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
        tvResolutionRate = view.findViewById(R.id.tvResolutionRate);
        tvResolutionTrend = view.findViewById(R.id.tvResolutionTrend);
        tvAvgWaitTime = view.findViewById(R.id.tvAvgWaitTime);
        tvWaitTimeTrend = view.findViewById(R.id.tvWaitTimeTrend);
        rvCategoryBreakdown = view.findViewById(R.id.rvCategoryBreakdown);
        llCategoryChart = view.findViewById(R.id.llCategoryChart);

        analyticsService = ApiClient.getInstance(requireContext()).create(AnalyticsService.class);
        
        adapter = new CategoryBreakdownAdapter(new ArrayList<>());
        rvCategoryBreakdown.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategoryBreakdown.setAdapter(adapter);

        fetchAnalytics();
    }

    private void bindChart(List<CategoryCount> categories) {
        if (categories == null || categories.isEmpty() || llCategoryChart == null) return;

        llCategoryChart.removeAllViews();

        List<CategoryCount> sorted = new ArrayList<>(categories);
        Collections.sort(sorted, (c1, c2) -> Long.compare(c2.getTicketCount(), c1.getTicketCount()));

        long maxCount = 0;
        for (CategoryCount v : sorted) {
            if (v.getTicketCount() > maxCount) maxCount = v.getTicketCount();
        }

        int maxBarHeightDp = 180; 
        int[] barColors = {
            0xFFF77F00, // colorTrendOrange
            0xFFFCBF49, // yellow
            0xFF10B981, // green
            0xFF2196F3, // blue
            0xFF9C27B0, // purple
            0xFF56CCF2, // light blue
            0xFFBB6BD9  // lavender
        };

        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (int i = 0; i < sorted.size(); i++) {
            CategoryCount data = sorted.get(i);
            
            View barItem = inflater.inflate(R.layout.item_chart_bar, llCategoryChart, false);
            ThreeDBarView vBar = barItem.findViewById(R.id.vBar);
            TextView tvDay = barItem.findViewById(R.id.tvDay);

            if (vBar != null && tvDay != null) {
                ViewGroup.LayoutParams params = vBar.getLayoutParams();
                int heightDp = maxCount > 0 ? (int) (maxBarHeightDp * (data.getTicketCount() / (double) maxCount)) : 0;
                if (data.getTicketCount() > 0) heightDp = Math.max(heightDp, 20);

                params.height = dpToPx(heightDp);
                vBar.setLayoutParams(params);
                
                vBar.setBarColor(barColors[i % barColors.length]);
                
                // Show full naming with rotation
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

    private void fetchAnalytics() {
        showLoading(true);
        
        analyticsService.getOverview().enqueue(new Callback<ApiResponse<AnalyticsOverview>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AnalyticsOverview>> call, @NonNull Response<ApiResponse<AnalyticsOverview>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    bindOverview(response.body().getData());
                }
                showLoading(false);
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AnalyticsOverview>> call, @NonNull Throwable t) {
                if (isAdded()) {
                    showLoading(false);
                    Toast.makeText(requireContext(), "Failed to load analytics", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void bindOverview(AnalyticsOverview overview) {
        if (overview == null) return;

        tvResolutionRate.setText(String.format(Locale.getDefault(), "%.1f%%", overview.getResolutionRate()));
        
        String resTrendSign = overview.getResolutionTrend() >= 0 ? "+" : "";
        tvResolutionTrend.setText(String.format(Locale.getDefault(), "%s%.1f%% from last week", 
                resTrendSign, overview.getResolutionTrend()));
        
        tvAvgWaitTime.setText(String.format(Locale.getDefault(), "%.1fh", overview.getAverageWaitTimeHours()));
        
        String waitTrendSign = overview.getAverageWaitTimeTrendHours() >= 0 ? "+" : "";
        tvWaitTimeTrend.setText(String.format(Locale.getDefault(), "%s%.1fh from last week", 
                waitTrendSign, overview.getAverageWaitTimeTrendHours()));
        
        adapter.setCategories(overview.getCategoryBreakdown());
        bindChart(overview.getCategoryBreakdown());
    }

    private void showLoading(boolean loading) {
        if (loading) {
            shimmerAnalytics.setVisibility(View.VISIBLE);
            shimmerAnalytics.startShimmer();
            nestedScrollView.setAlpha(0.3f);
        } else {
            shimmerAnalytics.stopShimmer();
            shimmerAnalytics.setVisibility(View.GONE);
            nestedScrollView.setAlpha(1.0f);
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
