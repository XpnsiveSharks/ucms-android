package com.example.ucms_android.ui.common;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.ucms_android.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class DialogUtils {

    private DialogUtils() {
    }

    public static void showConfirmationDialog(
            @NonNull Context context,
            @NonNull String title,
            @NonNull String message,
            @NonNull Runnable onConfirm
    ) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> onConfirm.run())
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    public static void showSuccessDialog(
            @NonNull Context context,
            @NonNull String title,
            @NonNull String message,
            @NonNull Runnable onAcknowledge
    ) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_ok, (dialog, which) -> onAcknowledge.run())
                .setCancelable(false)
                .show();
    }
}
