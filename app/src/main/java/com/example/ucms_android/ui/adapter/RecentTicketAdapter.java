package com.example.ucms_android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.util.DateFormatter;

import java.util.ArrayList;
import java.util.List;

public class RecentTicketAdapter extends RecyclerView.Adapter<RecentTicketAdapter.ViewHolder> {
    private List<Ticket> tickets = new ArrayList<>();
    private final OnTicketClickListener listener;

    public interface OnTicketClickListener {
        void onTicketClick(Ticket ticket);
    }

    public RecentTicketAdapter(OnTicketClickListener listener) {
        this.listener = listener;
    }

    public void updateTickets(List<Ticket> newTickets) {
        this.tickets.clear();
        this.tickets.addAll(newTickets);
        notifyDataSetChanged();
    }

    public void updateData(List<Ticket> newTickets) {
        this.tickets = newTickets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_ticket, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ticket ticket = tickets.get(position);
        holder.tvTicketNumber.setText(ticket.getTicketNumber() != null ? ticket.getTicketNumber() : "#" + ticket.getId());
        holder.tvTicketTitle.setText(ticket.getTitle());
        
        String status = ticket.getStatus() != null ? ticket.getStatus().toUpperCase() : "PENDING";
        holder.tvStatus.setText(status);
        
        // Sizing is standard via XML, only colors change here
        if (status.contains("RESOLVED") || status.contains("DONE") || status.contains("CLOSED")) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_outline_resolved);
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.colorStatusResolved));
        } else if (status.contains("IN_PROGRESS") || status.contains("ACTIVE")) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_outline_inprogress);
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.colorStatusInProgress));
        } else if (status.contains("TODO") || status.contains("NEW")) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_outline_todo);
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.colorStatusTodo));
        } else if (status.contains("REJECTED") || status.contains("FAILED")) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_outline_rejected);
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.colorStatusRejected));
        } else {
            // PENDING / WAITING
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_outline_pending);
            holder.tvStatus.setTextColor(holder.itemView.getContext().getColor(R.color.colorStatusPending));
        }

        String category = ticket.getCategoryName();
        if (category != null) {
            holder.tvCategory.setText(category.toUpperCase().replace(" ", "-"));
        }
        
        holder.tvTimeAgo.setText(DateFormatter.formatRelativeTime(ticket.getCreatedAt()));
        holder.itemView.setOnClickListener(v -> listener.onTicketClick(ticket));
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicketNumber, tvTicketTitle, tvStatus, tvCategory, tvTimeAgo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicketNumber = itemView.findViewById(R.id.tvTicketNumber);
            tvTicketTitle = itemView.findViewById(R.id.tvTicketTitle);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
        }
    }
}
