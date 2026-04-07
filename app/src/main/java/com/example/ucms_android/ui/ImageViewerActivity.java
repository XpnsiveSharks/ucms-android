package com.example.ucms_android.ui;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.ObjectKey;
import com.example.ucms_android.R;
import com.example.ucms_android.model.ApiResponse;
import com.example.ucms_android.model.AttachmentResponse;
import com.example.ucms_android.network.ApiClient;
import com.example.ucms_android.network.TicketService;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageViewerActivity extends AppCompatActivity {

    public static final String EXTRA_TICKET_ID = "ticket_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);

        long ticketId = getIntent().getLongExtra(EXTRA_TICKET_ID, -1);
        if (ticketId == -1) { finish(); return; }

        ImageView ivFullImage = findViewById(R.id.ivFullImage);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        ImageButton btnClose = findViewById(R.id.btnClose);

        btnClose.setOnClickListener(v -> finish());
        ivFullImage.setOnClickListener(v -> finish());

        // Fetch fresh signed URL
        TicketService ticketService = ApiClient.getInstance(this).create(TicketService.class);
        ticketService.getAttachments(ticketId).enqueue(new Callback<ApiResponse<List<AttachmentResponse>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<AttachmentResponse>>> call,
                                   @NonNull Response<ApiResponse<List<AttachmentResponse>>> response) {
                if (isFinishing()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getData() != null
                        && !response.body().getData().isEmpty()) {

                    AttachmentResponse att = response.body().getData().get(0);
                    String freshUrl = att.getSignedUrl();
                    ObjectKey stableKey = att.getId() != null
                            ? new ObjectKey("attachment-" + att.getId())
                            : new ObjectKey("ticket-attachment-" + ticketId);
                    progressBar.setVisibility(View.VISIBLE);

                    Glide.with(ImageViewerActivity.this)
                            .load(freshUrl)
                            .signature(stableKey)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                           Target<Drawable> target, boolean isFirstResource) {
                                    progressBar.setVisibility(View.GONE);
                                    ivFullImage.setVisibility(View.VISIBLE);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model,
                                                              Target<Drawable> target, DataSource dataSource,
                                                              boolean isFirstResource) {
                                    progressBar.setVisibility(View.GONE);
                                    ivFullImage.setVisibility(View.VISIBLE);
                                    return false;
                                }
                            })
                            .into(ivFullImage);
                } else {
                    progressBar.setVisibility(View.GONE);
                    finish();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<AttachmentResponse>>> call, @NonNull Throwable t) {
                if (!isFinishing()) {
                    progressBar.setVisibility(View.GONE);
                    finish();
                }
            }
        });
    }
}
