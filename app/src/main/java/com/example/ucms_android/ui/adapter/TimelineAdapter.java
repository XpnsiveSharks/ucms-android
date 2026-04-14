package com.example.ucms_android.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ucms_android.R;
import com.example.ucms_android.ui.ImageViewerActivity;

import java.util.List;

public class TimelineAdapter extends RecyclerView.Adapter<TimelineAdapter.ViewHolder> {

    public static class AdminResponse {
        public String adminName;
        public String message;
        public String time;
        public String attachmentSignedUrl;
        public String attachmentMimeType;
        public String attachmentFilename;
        public String responderRole;

        public AdminResponse(String adminName, String message, String time) {
            this.adminName = adminName;
            this.message = message;
            this.time = time;
        }

        public AdminResponse(String adminName, String message, String time,
                             String attachmentSignedUrl, String attachmentMimeType,
                             String attachmentFilename) {
            this(adminName, message, time);
            this.attachmentSignedUrl = attachmentSignedUrl;
            this.attachmentMimeType = attachmentMimeType;
            this.attachmentFilename = attachmentFilename;
        }

        public AdminResponse(String adminName, String message, String time,
                             String attachmentSignedUrl, String attachmentMimeType,
                             String attachmentFilename, String responderRole) {
            this(adminName, message, time, attachmentSignedUrl, attachmentMimeType, attachmentFilename);
            this.responderRole = responderRole;
        }
    }

    public static class TimelineEvent {
        public String time;
        public String title;
        public List<AdminResponse> responses;
        public int dotColor;

        public TimelineEvent(String time, String title, List<AdminResponse> responses, int dotColor) {
            this.time = time;
            this.title = title;
            this.responses = responses;
            this.dotColor = dotColor;
        }
    }

    private List<TimelineEvent> events;
    private Long ticketId;

    public TimelineAdapter(List<TimelineEvent> events) {
        this.events = events;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public void updateData(List<TimelineEvent> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimelineEvent event = events.get(position);

        holder.tvEventTime.setText(event.time);
        holder.tvEventTitle.setText(event.title);
        holder.tvEventTitle.setTextColor(event.dotColor);
        holder.viewDot.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(event.dotColor));

        holder.viewLineTop.setVisibility(position == 0 ? View.INVISIBLE : View.VISIBLE);
        holder.viewLineBottom.setVisibility(position == getItemCount() - 1 ? View.INVISIBLE : View.VISIBLE);

        holder.llResponses.removeAllViews();

        if (event.responses != null && !event.responses.isEmpty()) {
            holder.llResponses.setVisibility(View.VISIBLE);
            for (AdminResponse response : event.responses) {
                View responseCard = LayoutInflater.from(holder.itemView.getContext())
                        .inflate(R.layout.item_admin_response_card, holder.llResponses, false);

                TextView tvName = responseCard.findViewById(R.id.tvAdminName);
                TextView tvMessage = responseCard.findViewById(R.id.tvAdminMessage);
                TextView tvTime = responseCard.findViewById(R.id.tvResponseTime);
                View llAttachFile = responseCard.findViewById(R.id.llAttachmentFile);
                TextView tvAttachFilename = responseCard.findViewById(R.id.tvAttachFilename);

                boolean isStudent = "STUDENT".equals(response.responderRole);
                if (isStudent) {
                    tvName.setText("Student Response");
                } else {
                    tvName.setText(response.adminName != null
                            ? "Admin Response (" + response.adminName + ")"
                            : "Admin Response");
                }
                tvMessage.setText(response.message);
                if (tvTime != null) tvTime.setText(response.time);

                if (response.attachmentSignedUrl != null && llAttachFile != null) {
                    llAttachFile.setVisibility(View.VISIBLE);
                    if (tvAttachFilename != null) {
                        tvAttachFilename.setText(
                                response.attachmentFilename != null
                                        ? response.attachmentFilename
                                        : "Attachment");
                    }
                    boolean isImage = response.attachmentMimeType != null
                            && response.attachmentMimeType.startsWith("image/");
                    if (isImage && ticketId != null) {
                        llAttachFile.setOnClickListener(v -> {
                            Context ctx = v.getContext();
                            Intent intent = new Intent(ctx, ImageViewerActivity.class);
                            intent.putExtra(ImageViewerActivity.EXTRA_TICKET_ID, ticketId);
                            ctx.startActivity(intent);
                        });
                    } else {
                        llAttachFile.setOnClickListener(null);
                    }
                } else if (llAttachFile != null) {
                    llAttachFile.setVisibility(View.GONE);
                }

                holder.llResponses.addView(responseCard);
            }
        } else {
            holder.llResponses.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEventTime, tvEventTitle;
        View viewDot, viewLineTop, viewLineBottom;
        LinearLayout llResponses;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventTime = itemView.findViewById(R.id.tvEventTime);
            tvEventTitle = itemView.findViewById(R.id.tvEventTitle);
            viewDot = itemView.findViewById(R.id.viewDot);
            viewLineTop = itemView.findViewById(R.id.viewLineTop);
            viewLineBottom = itemView.findViewById(R.id.viewLineBottom);
            llResponses = itemView.findViewById(R.id.llResponses);
        }
    }
}
