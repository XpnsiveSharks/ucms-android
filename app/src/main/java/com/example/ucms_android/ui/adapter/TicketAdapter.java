package com.example.ucms_android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.Ticket;

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
        holder.tvTicketNumber.setText(ticket.getTicketNumber());
        holder.tvTicketTitle.setText(ticket.getTitle());
        holder.tvCategory.setText(ticket.getCategory());
        holder.tvTime.setText(ticket.getCreatedAt());
        holder.tvStatus.setText(ticket.getStatus());
        holder.tvStatus.setBackground(getStatusBackground(holder, ticket.getStatus()));
        holder.itemView.setOnClickListener(v -> listener.onTicketClick(ticket));
    }

    private android.graphics.drawable.Drawable getStatusBackground(ViewHolder holder, String status) {
        int drawableRes;
        if ("PENDING".equalsIgnoreCase(status)) {
            drawableRes = R.drawable.bg_badge_pending;
        } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            drawableRes = R.drawable.bg_badge_inprogress;
        } else if ("RESOLVED".equalsIgnoreCase(status)) {
            drawableRes = R.drawable.bg_badge_resolved;
        } else {
            drawableRes = R.drawable.bg_badge_closed;
        }
        return holder.itemView.getContext().getDrawable(drawableRes);
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
