package com.example.ucms_android.api.request;

import com.google.gson.annotations.SerializedName;

public class PasswordRecoverRequest {
    private String email;

    @SerializedName("redirect_to")
    private String redirectTo;

    public PasswordRecoverRequest(String email, String redirectTo) {
        this.email = email;
        this.redirectTo = redirectTo;
    }

    public String getEmail() {
        return email;
    }

    public String getRedirectTo() {
        return redirectTo;
    }
}
