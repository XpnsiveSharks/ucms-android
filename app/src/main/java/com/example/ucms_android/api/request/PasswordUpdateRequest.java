package com.example.ucms_android.api.request;

public class PasswordUpdateRequest {
    private String password;

    public PasswordUpdateRequest(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }
}
