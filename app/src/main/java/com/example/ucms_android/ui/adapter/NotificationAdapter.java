package com.example.ucms_android.ui.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.model.Notification;
import com.example.ucms_android.util.DateFormatter;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    private List<Notification> notifications;
    private final OnNotificationClickListener listener;

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications != null ? new ArrayList<>(notifications) : new ArrayList<>();
        this.listener = listener;
    }

    public void updateData(List<Notification> newData) {
        this.notifications = newData != null ? new ArrayList<>(newData) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void removeById(Long notificationId) {
        if (notificationId == null || notifications == null || notifications.isEmpty()) return;
        for (int i = 0; i < notifications.size(); i++) {
            Notification n = notifications.get(i);
            if (n != null && notificationId.equals(n.getId())) {
                notifications.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        
        String fullMessage = notification.getMessage();
        String category = "Ticket Update";
        String title = fullMessage;
        String body = "";

        if (fullMessage != null) {
            if (fullMessage.contains(": ")) {
                String[] parts = fullMessage.split(": ", 2);
                title = parts[0];
                body = parts[1];
            } else if (fullMessage.contains("updated") || fullMessage.contains("changed")) {
                title = fullMessage;
                body = "A ticket has been updated.";
            }
        }

        holder.tvCategory.setText(category);
        holder.tvTitle.setText(title);
        holder.tvMessage.setText(body);
        holder.tvTime.setText(DateFormatter.formatRelativeTime(notification.getCreatedAt()));

        boolean isRead = notification.isRead();
        holder.viewUnreadIndicator.setVisibility(isRead ? View.INVISIBLE : View.VISIBLE);
        
        int orange = ContextCompat.getColor(holder.itemView.getContext(), R.color.colorOrange);
        int grey = ContextCompat.getColor(holder.itemView.getContext(), R.color.colorTextSecondary);
        int black = ContextCompat.getColor(holder.itemView.getContext(), R.color.black);

        if (isRead) {
            holder.tvCategory.setTextColor(grey);
            holder.tvTime.setTextColor(grey);
            holder.tvTitle.setTextColor(grey);
            holder.tvMessage.setTextColor(grey);
        } else {
            holder.tvCategory.setTextColor(orange);
            holder.tvTime.setTextColor(orange);
            holder.tvTitle.setTextColor(black);
            holder.tvMessage.setTextColor(grey);
        }

        holder.itemView.setOnClickListener(v -> listener.onNotificationClick(notification));
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvTitle, tvMessage, tvTime;
        View viewUnreadIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            viewUnreadIndicator = itemView.findViewById(R.id.viewUnreadIndicator);
        }
    }
}
