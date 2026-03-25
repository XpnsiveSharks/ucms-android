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
import com.example.ucms_android.util.StatusChipHelper;

import java.util.List;

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

        // Apply ticket number background based on status
        if ("RESOLVED".equalsIgnoreCase(ticket.getStatus())) {
            holder.tvTicketNumber.setBackgroundResource(R.drawable.bg_ticket_number_resolved);
        } else {
            holder.tvTicketNumber.setBackgroundResource(R.drawable.bg_ticket_number);
        }

        // Use reusable helper for status and priority chips
        StatusChipHelper.applyStatusBadge(holder.tvStatus, ticket);
        
        if (isAdmin) {
            StatusChipHelper.applyPriorityBadge(holder.tvUrgency, ticket);
            holder.tvUrgency.setVisibility(holder.tvUrgency.getText().length() > 0 ? View.VISIBLE : View.GONE);
        } else {
            holder.tvUrgency.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onTicketClick(ticket));
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
            
            // Chips from included layout
            View statusChips = itemView.findViewById(R.id.layoutStatusChips);
            tvStatus = statusChips.findViewById(R.id.tvStatus);
            tvUrgency = statusChips.findViewById(R.id.tvUrgency);
        }
    }
}
