package com.example.ucms_android.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public final class FilePickerHelper {
    private static final String CACHE_ATTACHMENTS_DIR = "ticket_attachments";

    private FilePickerHelper() {
    }

    public static void launchFilePicker(@NonNull ActivityResultLauncher<Intent> launcher) {
        launchFilePicker(launcher, new String[]{"*/*"});
    }

    public static void launchFilePicker(
            @NonNull ActivityResultLauncher<Intent> launcher,
            @NonNull String[] mimeTypes
    ) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

        if (mimeTypes.length == 1) {
            intent.setType(mimeTypes[0]);
        } else {
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        }

        launcher.launch(intent);
    }

    @NonNull
    public static File getFileFromUri(@NonNull Context context, @NonNull Uri uri) throws IOException {
        String fileName = getFileName(context, uri);
        if (fileName == null || fileName.trim().isEmpty()) {
            fileName = "attachment_" + System.currentTimeMillis();
        }

        String mimeType = getMimeType(context, uri);
        String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        if (!fileName.contains(".") && extension != null && !extension.isEmpty()) {
            fileName = fileName + "." + extension.toLowerCase(Locale.US);
        }

        fileName = sanitizeFileName(fileName);

        File attachmentCacheDir = new File(context.getCacheDir(), CACHE_ATTACHMENTS_DIR);
        if (!attachmentCacheDir.exists() && !attachmentCacheDir.mkdirs()) {
            throw new IOException("Unable to create cache directory for attachments");
        }

        File outFile = createUniqueFile(attachmentCacheDir, fileName);
        ContentResolver resolver = context.getContentResolver();

        try (InputStream inputStream = resolver.openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(outFile)) {
            if (inputStream == null) {
                throw new IOException("Unable to open input stream for URI: " + uri);
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }

        return outFile;
    }

    @Nullable
    public static String getMimeType(@NonNull Context context, @NonNull Uri uri) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null && !mimeType.isEmpty()) {
            return mimeType;
        }

        String fileName = getFileName(context, uri);
        if (fileName != null) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
            if (extension != null && !extension.isEmpty()) {
                return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase(Locale.US));
            }
        }

        return null;
    }

    @Nullable
    public static String getFileName(@NonNull Context context, @NonNull Uri uri) {
        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            String displayName = queryDisplayName(context, uri);
            if (displayName != null && !displayName.isEmpty()) {
                return displayName;
            }
        }

        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            File file = new File(uri.getPath() == null ? "" : uri.getPath());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                String mediaStoreName = queryMediaStoreDisplayName(context, uri);
                if (mediaStoreName != null && !mediaStoreName.isEmpty()) {
                    return mediaStoreName;
                }
            } else {
                File legacyRoot = Environment.getExternalStorageDirectory();
                String absolutePath = file.getAbsolutePath();
                if (legacyRoot != null && absolutePath.startsWith(legacyRoot.getAbsolutePath())) {
                    return file.getName();
                }
            }
            return file.getName();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            String mediaStoreName = queryMediaStoreDisplayName(context, uri);
            if (mediaStoreName != null && !mediaStoreName.isEmpty()) {
                return mediaStoreName;
            }
        }

        String lastPathSegment = uri.getLastPathSegment();
        if (lastPathSegment == null || lastPathSegment.isEmpty()) {
            return null;
        }

        int slashIndex = lastPathSegment.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex < lastPathSegment.length() - 1) {
            return lastPathSegment.substring(slashIndex + 1);
        }

        return lastPathSegment;
    }

    @Nullable
    private static String queryDisplayName(@NonNull Context context, @NonNull Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (SecurityException | IllegalArgumentException e) {
            android.util.Log.w("FilePickerHelper", "queryDisplayName failed: " + e.getMessage());
        }

        return null;
    }

    @Nullable
    private static String queryMediaStoreDisplayName(@NonNull Context context, @NonNull Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(
                uri,
                new String[]{MediaStore.MediaColumns.DISPLAY_NAME},
                null,
                null,
                null
        )) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (SecurityException | IllegalArgumentException e) {
            android.util.Log.w("FilePickerHelper", "queryMediaStoreDisplayName failed: " + e.getMessage());
        }

        return null;
    }

    @NonNull
    private static File createUniqueFile(@NonNull File parent, @NonNull String fileName) {
        File file = new File(parent, fileName);
        if (!file.exists()) {
            return file;
        }

        String baseName = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        int counter = 1;
        File candidate = file;
        while (candidate.exists()) {
            candidate = new File(parent, baseName + "_" + counter + extension);
            counter++;
        }
        return candidate;
    }

    @NonNull
    private static String sanitizeFileName(@NonNull String input) {
        String sanitized = input.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return sanitized.isEmpty() ? "attachment_" + System.currentTimeMillis() : sanitized;
    }
}
