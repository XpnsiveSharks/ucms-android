package com.example.ucms_android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.TimelineEvent;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {

    private final List<TimelineEvent> events;

    public TimelineAdapter(List<TimelineEvent> events) {
        this.events = events;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimelineEvent event = events.get(position);

        holder.tvTimelineDate.setText(event.getDate());
        holder.tvTimelineTitle.setText(event.getTitle());

        // Handle Admin Response Card visibility
        if (event.getAdminResponse() != null && !event.getAdminResponse().isEmpty()) {
            holder.cvAdminResponse.setVisibility(View.VISIBLE);
            holder.tvAdminMessage.setText(event.getAdminResponse());
        } else {
            holder.cvAdminResponse.setVisibility(View.GONE);
        }

        // Color the dot based on status (completed vs pending)
        if (event.isCompleted()) {
            holder.vTimelineDot.setBackgroundResource(R.drawable.bg_badge_inprogress); // Light green dot for active/completed
        } else {
            holder.vTimelineDot.setBackgroundResource(R.drawable.bg_badge_closed); // Grey dot for pending
        }

        // Hide the connecting line for the last item
        if (position == events.size() - 1) {
            holder.vTimelineLine.setVisibility(View.INVISIBLE);
        } else {
            holder.vTimelineLine.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View vTimelineLine, vTimelineDot;
        TextView tvTimelineDate, tvTimelineTitle, tvAdminMessage;
        MaterialCardView cvAdminResponse;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            vTimelineLine = itemView.findViewById(R.id.vTimelineLine);
            vTimelineDot = itemView.findViewById(R.id.vTimelineDot);
            tvTimelineDate = itemView.findViewById(R.id.tvTimelineDate);
            tvTimelineTitle = itemView.findViewById(R.id.tvTimelineTitle);
            tvAdminMessage = itemView.findViewById(R.id.tvAdminMessage);
            cvAdminResponse = itemView.findViewById(R.id.cvAdminResponse);
        }
    }
}
