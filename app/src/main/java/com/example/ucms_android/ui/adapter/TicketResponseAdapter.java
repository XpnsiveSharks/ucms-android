package com.example.ucms_android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.TicketResponse;

import java.util.List;

public class TicketResponseAdapter extends RecyclerView.Adapter<TicketResponseAdapter.ViewHolder> {

    private List<TicketResponse> responses;

    public TicketResponseAdapter(List<TicketResponse> responses) {
        this.responses = responses;
    }

    public void updateData(List<TicketResponse> newResponses) {
        this.responses = newResponses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_response, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TicketResponse response = responses.get(position);
        holder.tvAdminName.setText(response.getAdminName());
        holder.tvMessage.setText(response.getMessage());
        holder.tvCreatedAt.setText(response.getCreatedAt());
    }

    @Override
    public int getItemCount() { return responses.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAdminName, tvMessage, tvCreatedAt;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAdminName = itemView.findViewById(R.id.tvAdminName);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
        }
    }
}
