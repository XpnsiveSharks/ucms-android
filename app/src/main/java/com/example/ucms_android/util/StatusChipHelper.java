package com.example.ucms_android.util;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.ucms_android.R;
import com.example.ucms_android.model.Ticket;

import java.util.Locale;

public class StatusChipHelper {

    public static void applyStatusBadge(TextView badgeView, Ticket ticket) {
        if (ticket == null || ticket.getStatus() == null) {
            badgeView.setVisibility(View.GONE);
            return;
        }

        badgeView.setVisibility(View.VISIBLE);
        String status = ticket.getStatus();

        // Display mapping
        String displayStatus = status;
        if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            displayStatus = "ONGOING";
        }
        badgeView.setText(displayStatus.replace("_", " ").toUpperCase(Locale.ROOT));

        // Background and text color mapping (Muted Palette Concept)
        if ("PENDING".equalsIgnoreCase(status)) {
            badgeView.setBackgroundResource(R.drawable.bg_badge_outline_pending);
            badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorStatusPending));
        } else if ("IN_PROGRESS".equalsIgnoreCase(status)) {
            badgeView.setBackgroundResource(R.drawable.bg_badge_outline_inprogress);
            badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorStatusInProgress));
        } else if ("RESOLVED".equalsIgnoreCase(status)) {
            badgeView.setBackgroundResource(R.drawable.bg_badge_outline_resolved);
            badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorStatusResolved));
        } else if ("TODO".equalsIgnoreCase(status) || "NEW".equalsIgnoreCase(status)) {
            badgeView.setBackgroundResource(R.drawable.bg_badge_outline_todo);
            badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorStatusTodo));
        } else if ("REJECTED".equalsIgnoreCase(status) || "FAILED".equalsIgnoreCase(status)) {
            badgeView.setBackgroundResource(R.drawable.bg_badge_outline_rejected);
            badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorStatusRejected));
        } else {
            badgeView.setBackgroundResource(R.drawable.bg_badge_outline_muted);
            badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorTextSecondary));
        }
    }

    public static void applyPriorityBadge(TextView badgeView, Ticket ticket) {
        String priority = resolvePriorityLevel(ticket);
        if (priority == null) {
            badgeView.setVisibility(View.GONE);
            return;
        }

        badgeView.setVisibility(View.VISIBLE);
        badgeView.setText(priority);

        switch (priority) {
            case "CRITICAL":
                badgeView.setBackgroundResource(R.drawable.bg_badge_outline_critical);
                badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorStatusUrgent));
                break;
            case "HIGH":
                badgeView.setBackgroundResource(R.drawable.bg_badge_outline_high);
                badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorPriorityHigh));
                break;
            case "LOW":
                badgeView.setBackgroundResource(R.drawable.bg_badge_outline_low);
                badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorStatusInProgress));
                break;
            default:
                badgeView.setBackgroundResource(R.drawable.bg_badge_outline_muted);
                badgeView.setTextColor(ContextCompat.getColor(badgeView.getContext(), R.color.colorTextSecondary));
                break;
        }
    }

    public static String resolvePriorityLevel(Ticket ticket) {
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
}
