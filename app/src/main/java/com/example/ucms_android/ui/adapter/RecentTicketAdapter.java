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

public class RecentTicketAdapter extends RecyclerView.Adapter<RecentTicketAdapter.ViewHolder> {

    public interface OnTicketClickListener {
        void onTicketClick(Ticket ticket);
    }

    private List<Ticket> tickets;
    private final OnTicketClickListener listener;

    public RecentTicketAdapter(List<Ticket> tickets, OnTicketClickListener listener) {
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
                .inflate(R.layout.item_recent_ticket, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ticket ticket = tickets.get(position);
        holder.tvTicketNumber.setText(ticket.getTicketNumber());
        holder.tvTicketTitle.setText(ticket.getTitle());
        holder.tvCategory.setText(ticket.getCategory());
        holder.tvTimeAgo.setText(ticket.getCreatedAt());
        holder.itemView.setOnClickListener(v -> listener.onTicketClick(ticket));
    }

    @Override
    public int getItemCount() {
        return tickets.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicketNumber, tvTicketTitle, tvCategory, tvTimeAgo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicketNumber = itemView.findViewById(R.id.tvTicketNumber);
            tvTicketTitle = itemView.findViewById(R.id.tvTicketTitle);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
        }
    }
}
