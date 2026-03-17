package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {
    @SerializedName("success")
    private boolean success;
    private String message;
    private String errorCode;
    private T data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getErrorCode() { return errorCode; }
    public T getData() { return data; }
}
