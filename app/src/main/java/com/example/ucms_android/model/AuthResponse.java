package com.example.ucms_android.model;

public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String refreshToken;

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public String getRefreshToken() { return refreshToken; }
}
