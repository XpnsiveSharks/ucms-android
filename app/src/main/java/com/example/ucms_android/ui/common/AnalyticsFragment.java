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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.ucms_android.R;
import com.example.ucms_android.model.AnalyticsOverview;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.CategoryCount;
import com.example.ucms_android.model.DailyTicketVolume;
import com.example.ucms_android.network.AnalyticsService;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.session.SessionManager;
import com.example.ucms_android.sync.SyncUpdateBus;
import com.example.ucms_android.ui.view.ThreeDBarView;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AnalyticsFragment extends Fragment {

    private ShimmerFrameLayout shimmerAnalytics;
    private SwipeRefreshLayout swipeRefresh;
    private View nestedScrollView;
    private View layoutError;
    private TextView tvResolutionRate, tvResolutionTrend;
    private TextView tvAvgWaitTime, tvWaitTimeTrend;
    private ViewGroup llCategoryChart, llStatusDistribution, llCategoryYAxis;
    private com.example.ucms_android.ui.view.LineChartView lineChartTimeline;
    private RecyclerView rvCategoryBreakdown;

    private AnalyticsService analyticsService;
    private CategoryBreakdownAdapter adapter;
    private SessionManager sessionManager;
    private Gson gson;

    private final SyncUpdateBus.Listener syncListener = domain -> {
        if (SyncUpdateBus.DOMAIN_ANALYTICS.equals(domain) && isAdded()) {
            loadFromCacheAndBind();
        }
    };

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
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        nestedScrollView = view.findViewById(R.id.nestedScrollView);
        layoutError = view.findViewById(R.id.layoutError);
        tvResolutionRate = view.findViewById(R.id.tvResolutionRate);
        tvResolutionTrend = view.findViewById(R.id.tvResolutionTrend);
        tvAvgWaitTime = view.findViewById(R.id.tvAvgWaitTime);
        tvWaitTimeTrend = view.findViewById(R.id.tvWaitTimeTrend);
        llCategoryChart = view.findViewById(R.id.llCategoryChart);
        llCategoryYAxis = view.findViewById(R.id.llCategoryYAxis);
        lineChartTimeline = view.findViewById(R.id.lineChartTimeline);
        llStatusDistribution = view.findViewById(R.id.llStatusDistribution);
        rvCategoryBreakdown = view.findViewById(R.id.rvCategoryBreakdown);

        view.findViewById(R.id.btnRetry).setOnClickListener(v -> fetchAnalytics());

        analyticsService = ApiClient.getInstance(requireContext()).create(AnalyticsService.class);
        sessionManager = new SessionManager(requireContext());
        gson = new Gson();

        adapter = new CategoryBreakdownAdapter(new ArrayList<>());
        rvCategoryBreakdown.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCategoryBreakdown.setAdapter(adapter);

        // Pull-to-refresh shows shimmer
        swipeRefresh.setOnRefreshListener(() -> {
            swipeRefresh.setRefreshing(false);
            fetchAnalytics();
        });

        // Load from cache first; if empty fetch with shimmer
        if (!loadFromCacheAndBind()) {
            fetchAnalytics();
        } else {
            // Cache hit — silently refresh in background
            fetchAnalyticsSilently();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SyncUpdateBus.getInstance().register(syncListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        SyncUpdateBus.getInstance().unregister(syncListener);
    }

    /** Reads cache, binds UI without shimmer. Returns true if cache was available. */
    private boolean loadFromCacheAndBind() {
        String cachedJson = sessionManager.getAnalyticsSummaryJson();
        if (cachedJson == null) return false;
        AnalyticsOverview cached = gson.fromJson(cachedJson, AnalyticsOverview.class);
        if (cached == null) return false;
        bindOverview(cached);
        return true;
    }

    /** Silent background refresh — no shimmer, updates cache and UI when done. */
    private void fetchAnalyticsSilently() {
        analyticsService.getOverview().enqueue(new Callback<ApiResponse<AnalyticsOverview>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AnalyticsOverview>> call,
                                   @NonNull Response<ApiResponse<AnalyticsOverview>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null
                        && response.body().getData() != null) {
                    AnalyticsOverview overview = response.body().getData();
                    sessionManager.saveAnalyticsSummaryJson(gson.toJson(overview));
                    bindOverview(overview);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<AnalyticsOverview>> call, @NonNull Throwable t) {
                // Silent failure — cached data remains displayed
            }
        });
    }

    /** Fetch with shimmer — used on first load (no cache) and manual pull-to-refresh. */
    private void fetchAnalytics() {
        showLoading(true);
        layoutError.setVisibility(View.GONE);

        analyticsService.getOverview().enqueue(new Callback<ApiResponse<AnalyticsOverview>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<AnalyticsOverview>> call,
                                   @NonNull Response<ApiResponse<AnalyticsOverview>> response) {
                if (isAdded()) {
                    showLoading(false);
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        AnalyticsOverview overview = response.body().getData();
                        sessionManager.saveAnalyticsSummaryJson(gson.toJson(overview));
                        bindOverview(overview);
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
    }

    private void bindOverview(AnalyticsOverview overview) {
        if (overview == null) return;
        layoutError.setVisibility(View.GONE);
        nestedScrollView.setVisibility(View.VISIBLE);

        tvResolutionRate.setText(String.format(Locale.getDefault(), "%.1f%%", overview.getResolutionRate()));
        String resTrendSign = overview.getResolutionTrend() >= 0 ? "+" : "";
        tvResolutionTrend.setText(String.format(Locale.getDefault(), "%s%.1f%% vs last week", resTrendSign, overview.getResolutionTrend()));
        tvResolutionTrend.setTextColor(overview.getResolutionTrend() >= 0 ? getResources().getColor(R.color.dm_mint_green) : getResources().getColor(R.color.colorError));

        tvAvgWaitTime.setText(String.format(Locale.getDefault(), "%.1fh", overview.getAverageWaitTimeHours()));
        String waitTrendSign = overview.getAverageWaitTimeTrendHours() >= 0 ? "+" : "";
        tvWaitTimeTrend.setText(String.format(Locale.getDefault(), "%s%.1fh vs last week", waitTrendSign, overview.getAverageWaitTimeTrendHours()));
        tvWaitTimeTrend.setTextColor(overview.getAverageWaitTimeTrendHours() <= 0 ? getResources().getColor(R.color.dm_mint_green) : getResources().getColor(R.color.dm_orange_peach));

        if (overview.getTicketVolumeLast7Days() != null) {
            bindTimelineChart(overview.getTicketVolumeLast7Days());
        }

        if (overview.getCategoryBreakdown() != null) {
            bindCategoryChart(overview.getCategoryBreakdown());
            adapter.setCategories(overview.getCategoryBreakdown());
        }

        bindStatusDistribution(overview);
    }

    private void bindTimelineChart(List<DailyTicketVolume> volume) {
        if (lineChartTimeline == null || volume == null) return;

        List<DailyTicketVolume> sortedVolume = new ArrayList<>(volume);
        Collections.sort(sortedVolume, (d1, d2) -> Integer.compare(getDayOrder(d1.getDay()), getDayOrder(d2.getDay())));

        List<com.example.ucms_android.ui.view.LineChartView.DataPoint> points = new ArrayList<>();
        for (DailyTicketVolume d : sortedVolume) {
            // Shorten day labels (e.g., "Monday" -> "Mon")
            String label = d.getDay();
            if (label != null && label.length() > 3) {
                label = label.substring(0, 3).toUpperCase();
            } else if (label != null) {
                label = label.toUpperCase();
            }
            points.add(new com.example.ucms_android.ui.view.LineChartView.DataPoint(label, d.getTicketCount()));
        }
        lineChartTimeline.setData(points);
    }

    private int getDayOrder(String day) {
        if (day == null) return 7;
        String d = day.toUpperCase();
        if (d.contains("MON")) return 0;
        if (d.contains("TUE")) return 1;
        if (d.contains("WED")) return 2;
        if (d.contains("THU")) return 3;
        if (d.contains("FRI")) return 4;
        if (d.contains("SAT")) return 5;
        if (d.contains("SUN")) return 6;
        return 7;
    }

    private void bindCategoryChart(List<CategoryCount> categories) {
        if (categories == null || categories.isEmpty() || llCategoryChart == null) return;
        llCategoryChart.removeAllViews();

        List<CategoryCount> sorted = new ArrayList<>(categories);
        Collections.sort(sorted, (c1, c2) -> Long.compare(c2.getTicketCount(), c1.getTicketCount()));

        long realMax = 0;
        for (CategoryCount v : sorted) {
            if (v.getTicketCount() > realMax) realMax = v.getTicketCount();
        }

        // Calculate nice scale for 4 intervals (5 lines)
        long stepSize;
        int steps = 4;
        if (realMax <= 4) {
            stepSize = 1;
            steps = realMax == 0 ? 1 : (int) realMax;
        } else {
            stepSize = (long) Math.ceil(realMax / 4.0);
            if (stepSize > 2 && stepSize % 2 != 0) stepSize++;
        }
        long maxCount = stepSize * steps;

        // Bind Y-Axis numbering (Must have 5 labels if steps=4)
        if (llCategoryYAxis != null) {
            llCategoryYAxis.removeAllViews();
            for (int i = steps; i >= 0; i--) {
                TextView tv = new TextView(requireContext());
                tv.setText(String.valueOf(stepSize * i));
                tv.setTextSize(10);
                tv.setTextColor(getResources().getColor(R.color.colorTextSecondary));

                // Use ConstraintLayout params to align perfectly with lines
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp =
                    new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                lp.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
                lp.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;

                // Vertical bias: 0.0 for top (i=steps), 1.0 for bottom (i=0)
                // Since steps might be less than 4 for small data, we need to handle that
                lp.verticalBias = (steps > 0) ? (1.0f - (float) i / steps) : 0.0f;

                tv.setLayoutParams(lp);
                llCategoryYAxis.addView(tv);
            }
        }

        int maxBarHeightDp = 200; // Fixed height for bars to leave 80dp for labels
        int[] barColors = {0xFFF77F00, 0xFFFCBF49, 0xFF10B981, 0xFF2196F3, 0xFF9C27B0, 0xFF56CCF2, 0xFFBB6BD9};

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (int i = 0; i < sorted.size(); i++) {
            CategoryCount data = sorted.get(i);
            View barItem = inflater.inflate(R.layout.item_chart_bar, llCategoryChart, false);
            ThreeDBarView vBar = barItem.findViewById(R.id.vBar);
            TextView tvDay = barItem.findViewById(R.id.tvDay);
            TextView tvCount = barItem.findViewById(R.id.tvCount);

            if (vBar != null && tvDay != null) {
                ViewGroup.LayoutParams params = vBar.getLayoutParams();
                // Use maxCount (the rounded up max) for consistent scaling
                int heightDp = maxCount > 0 ? (int) (maxBarHeightDp * (data.getTicketCount() / (double) maxCount)) : 0;
                if (data.getTicketCount() > 0) heightDp = Math.max(heightDp, 25);
                params.height = dpToPx(heightDp);
                vBar.setLayoutParams(params);
                vBar.setBarColor(barColors[i % barColors.length]);
                tvDay.setText(data.getCategoryName().toUpperCase());
                if (tvCount != null) {
                    tvCount.setText(String.valueOf(data.getTicketCount()));
                }
            }
            llCategoryChart.addView(barItem);
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
        CategoryBreakdownAdapter(List<CategoryCount> categories) { this.categories = categories; }
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
        public int getItemCount() { return categories.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            View vColor; TextView tvName, tvCount;
            ViewHolder(View itemView) {
                super(itemView);
                vColor = itemView.findViewById(R.id.vCategoryColor);
                tvName = itemView.findViewById(R.id.tvCategoryName);
                tvCount = itemView.findViewById(R.id.tvTicketCount);
            }
        }
    }
}
