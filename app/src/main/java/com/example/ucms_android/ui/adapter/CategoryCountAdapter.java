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

    public CategoryCountAdapter(List<CategoryCount> items) {
        this.items = items;
    }

    public void updateData(List<CategoryCount> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
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
        holder.tvCategoryName.setText(item.getCategoryName());
        holder.tvTicketCount.setText(String.valueOf(item.getTicketCount()));

        int color = position == 0
                ? holder.itemView.getContext().getColor(R.color.colorPrimary)
                : holder.itemView.getContext().getColor(R.color.colorSecondary);
        holder.vCategoryColor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategoryName, tvTicketCount;
        View vCategoryColor;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            tvTicketCount = itemView.findViewById(R.id.tvTicketCount);
            vCategoryColor = itemView.findViewById(R.id.vCategoryColor);
        }
    }
}
