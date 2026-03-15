package com.example.ucms_android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {

    public static class AdminResponse {
        public String adminName;
        public String message;
        public String time;

        public AdminResponse(String adminName, String message, String time) {
            this.adminName = adminName;
            this.message = message;
            this.time = time;
        }
    }

    public static class TimelineEvent {
        public String time;
        public String title;
        public List<AdminResponse> responses;
        public int dotColor;

        public TimelineEvent(String time, String title, List<AdminResponse> responses, int dotColor) {
            this.time = time;
            this.title = title;
            this.responses = responses;
            this.dotColor = dotColor;
        }
    }

    private List<TimelineEvent> events;

    public TimelineAdapter(List<TimelineEvent> events) {
        this.events = events;
    }

    public void updateData(List<TimelineEvent> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimelineEvent event = events.get(position);

        holder.tvEventTime.setText(event.time);
        holder.tvEventTitle.setText(event.title);
        holder.viewDot.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(event.dotColor));

        holder.viewLineTop.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
        holder.viewLineBottom.setVisibility(position == getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);

        holder.llResponses.removeAllViews();

        if (event.responses != null && !event.responses.isEmpty()) {
            holder.llResponses.setVisibility(View.VISIBLE);
            for (AdminResponse response : event.responses) {
                View responseCard = LayoutInflater.from(holder.itemView.getContext())
                        .inflate(R.layout.item_admin_response_card, holder.llResponses, false);

                TextView tvName = responseCard.findViewById(R.id.tvAdminName);
                TextView tvMessage = responseCard.findViewById(R.id.tvAdminMessage);
                TextView tvTime = responseCard.findViewById(R.id.tvResponseTime);

                tvName.setText(response.adminName != null
                        ? "Admin Response (" + response.adminName + ")"
                        : "Admin Response");
                tvMessage.setText(response.message);
                if (tvTime != null) tvTime.setText(response.time);

                holder.llResponses.addView(responseCard);
            }
        } else {
            holder.llResponses.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTime, tvEventTitle;
        View viewDot, viewLineTop, viewLineBottom;
        LinearLayout llResponses;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTime = itemView.findViewById(R.id.tvEventTime);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            viewDot = itemView.findViewById(R.id.viewDot);
            viewLineTop = itemView.findViewById(R.id.viewLineTop);
            viewLineBottom = itemView.findViewById(R.id.viewLineBottom);
            llResponses = itemView.findViewById(R.id.llResponses);
        }
    }
}
