package com.example.ucms_android.model;

public class ApiError {
    private String message;
    private String code;

    public ApiError(String message, String code) {
        this.message = message;
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }
}
