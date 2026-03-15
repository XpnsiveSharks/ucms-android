package com.example.ucms_android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.CategoryCount;

import java.util.List;

public class CategoryCountAdapter extends RecyclerView.Adapter<CategoryCountAdapter.ViewHolder> {

    private List<CategoryCount> items;
    private long maxCount;

    public CategoryCountAdapter(List<CategoryCount> items) {
        this.items = items;
        this.maxCount = computeMax(items);
    }

    public void updateData(List<CategoryCount> newItems) {
        this.items = newItems;
        this.maxCount = computeMax(newItems);
        notifyDataSetChanged();
    }

    private long computeMax(List<CategoryCount> list) {
        long max = 1;
        for (CategoryCount c : list) {
            if (c.getTicketCount() > max) max = c.getTicketCount();
        }
        return max;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_count, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CategoryCount item = items.get(position);
        holder.tvRankBadge.setText(String.valueOf(position + 1));
        holder.tvCategoryName.setText(item.getCategoryName());
        holder.tvTicketCount.setText(String.valueOf(item.getTicketCount()));

        int maxWidthPx = holder.itemView.getResources().getDisplayMetrics().widthPixels - 200;
        int barWidth = maxCount > 0 ? (int) (item.getTicketCount() * maxWidthPx / maxCount) : 0;
        ViewGroup.LayoutParams params = holder.viewBar.getLayoutParams();
        params.width = Math.max(barWidth, 8);
        holder.viewBar.setLayoutParams(params);

        int color = position == 0
                ? holder.itemView.getContext().getColor(R.color.colorPrimary)
                : holder.itemView.getContext().getColor(R.color.colorSecondary);
        holder.tvRankBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        holder.viewBar.setBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRankBadge, tvCategoryName, tvTicketCount;
        View viewBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRankBadge = itemView.findViewById(R.id.tvRankBadge);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvTicketCount = itemView.findViewById(R.id.tvTicketCount);
            viewBar = itemView.findViewById(R.id.viewBar);
        }
    }
}
