package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class CreateResponseRequest {

    @SerializedName("message")
    private String message;

    public CreateResponseRequest(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }
}
