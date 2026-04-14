package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class AttachmentResponse {

    @SerializedName("id")
    private Long id;

    @SerializedName("originalFilename")
    private String originalFilename;

    @SerializedName("mimeType")
    private String mimeType;

    @SerializedName("sizeBytes")
    private long sizeBytes;

    @SerializedName("signedUrl")
    private String signedUrl;

    @SerializedName("uploadedAt")
    private String uploadedAt;

    @SerializedName("uploaderRole")
    private String uploaderRole;

    public Long getId() { return id; }
    public String getOriginalFilename() { return originalFilename; }
    public String getMimeType() { return mimeType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSignedUrl() { return signedUrl; }
    public String getUploadedAt() { return uploadedAt; }
    public String getUploaderRole() { return uploaderRole; }
}
