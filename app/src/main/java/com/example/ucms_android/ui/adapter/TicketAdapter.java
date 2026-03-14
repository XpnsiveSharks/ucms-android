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

import java.util.List;

public class TicketAdapter extends RecyclerView.Adapter<TicketAdapter.ViewHolder> {

    public interface OnTicketClickListener {
        void onTicketClick(Ticket ticket);
    }

    private List<Ticket> tickets;
    private final OnTicketClickListener listener;

    public TicketAdapter(List<Ticket> tickets, OnTicketClickListener listener) {
        this.tickets = tickets;
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
        holder.tvStatus.setText(ticket.getStatus());
        holder.tvStatus.setBackgroundResource(getStatusBackgroundResource(ticket.getStatus()));
        holder.itemView.setOnClickListener(v -> listener.onTicketClick(ticket));
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
        TextView tvTicketNumber, tvTicketTitle, tvCategory, tvTime, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicketNumber = itemView.findViewById(R.id.tvTicketNumber);
            tvTicketTitle = itemView.findViewById(R.id.tvTicketTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
