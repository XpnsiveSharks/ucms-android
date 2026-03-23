package com.example.ucms_android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.Ticket;
import com.example.ucms_android.util.DateFormatter;

import java.util.List;
import java.util.Locale;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.ViewHolder> {

    public interface OnTicketClickListener {
        void onTicketClick(Ticket ticket);
    }

    private List<Ticket> tickets;
    private final OnTicketClickListener listener;
    private final boolean isAdmin;

    public TicketAdapter(List<Ticket> tickets, boolean isAdmin, OnTicketClickListener listener) {
        this.tickets = tickets;
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    public void updateData(List<Ticket> newTickets) {
        this.tickets = newTickets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ticket, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ticket ticket = tickets.get(position);
        holder.tvTicketNumber.setText(ticket.getTicketNumber() != null ? ticket.getTicketNumber() : "#" + ticket.getId());
        holder.tvTicketTitle.setText(ticket.getTitle());
        holder.tvCategory.setText(ticket.getCategoryName());
        holder.tvTime.setText(DateFormatter.formatRelativeTime(ticket.getCreatedAt()));
        
        if (isAdmin) {
            holder.tvStatus.setVisibility(View.GONE);
            applyPriorityBadge(holder.tvUrgency, ticket);
        } else {
            holder.tvUrgency.setVisibility(View.GONE);
            applyStatusBadge(holder.tvStatus, ticket);
        }
        
        holder.itemView.setOnClickListener(v -> listener.onTicketClick(ticket));
    }

    private void applyStatusBadge(TextView badgeView, Ticket ticket) {
        if (ticket == null || ticket.getStatus() == null) {
            badgeView.setVisibility(View.GONE);
            return;
        }
        
        badgeView.setVisibility(View.VISIBLE);
        String status = ticket.getStatus();
        badgeView.setText(status.replace("_", " "));
        
        if ("PENDING".equalsIgnoreCase(status)) {
            badgeView.setBackgroundResource(R.drawable.bg_badge_outline_pending);
            badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorStatusPending));
        } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            badgeView.setBackgroundResource(R.drawable.bg_badge_outline_inprogress);
            badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorStatusInProgress));
        } else if ("RESOLVED".equalsIgnoreCase(status)) {
            badgeView.setBackgroundResource(R.drawable.bg_badge_outline_resolved);
            badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorStatusResolved));
        } else {
            badgeView.setBackgroundResource(R.drawable.bg_badge_outline_closed);
            badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorStatusClosed));
        }
    }

    private void applyPriorityBadge(TextView badgeView, Ticket ticket) {
        String priority = resolvePriorityLevel(ticket);
        if (priority == null) {
            badgeView.setVisibility(View.GONE);
            return;
        }

        badgeView.setVisibility(View.VISIBLE);
        badgeView.setText(priority);

        switch (priority) {
            case "CRITICAL":
                badgeView.setBackgroundResource(R.drawable.bg_badge_urgent);
                badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.white));
                break;
            case "HIGH":
                badgeView.setBackgroundResource(R.drawable.bg_badge_priority_high);
                badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorTextPrimary));
                break;
            case "LOW":
                badgeView.setBackgroundResource(R.drawable.bg_badge_priority_low);
                badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.white));
                break;
            default:
                badgeView.setBackgroundResource(R.drawable.bg_badge_priority_muted);
                badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.white));
                break;
        }
    }

    private String resolvePriorityLevel(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        String label = ticket.getUrgencyLabel();
        if (label != null && !label.trim().isEmpty()) {
            String normalized = label.trim().toUpperCase(Locale.ROOT);
            if ("CRITICAL".equals(normalized) || "HIGH".equals(normalized)
                    || "LOW".equals(normalized) || "MUTED".equals(normalized)) {
                return normalized;
            }
            if ("MEDIUM".equals(normalized)) {
                return "LOW";
            }
        }

        Integer score = ticket.getUrgencyScore();
        if (score != null) {
            if (score >= 80) return "CRITICAL";
            if (score >= 55) return "HIGH";
            if (score >= 25) return "LOW";
            return "MUTED";
        }

        if (ticket.isUrgent()) {
            return "HIGH";
        }
        return null;
    }

    private int getStatusBackgroundResource(String status) {
        if ("PENDING".equalsIgnoreCase(status)) {
            return R.drawable.bg_badge_pending;
        } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            return R.drawable.bg_badge_inprogress;
        } else if ("RESOLVED".equalsIgnoreCase(status)) {
            return R.drawable.bg_badge_resolved;
        } else {
            return R.drawable.bg_badge_closed;
        }
    }

    @Override
    public int getItemCount() { return tickets.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicketNumber, tvTicketTitle, tvCategory, tvTime, tvUrgency, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicketNumber = itemView.findViewById(R.id.tvTicketNumber);
            tvTicketTitle = itemView.findViewById(R.id.tvTicketTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvUrgency = itemView.findViewById(R.id.tvUrgency);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
