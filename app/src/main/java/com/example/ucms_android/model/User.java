package com.example.ucms_android.model;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("authUserId") private String authUserId;
    @SerializedName("studentId") private String studentId;
    @SerializedName("name") private String name;
    @SerializedName("email") private String email;
    @SerializedName("emailVerified") private boolean emailVerified;
    @SerializedName("course") private String course;
    @SerializedName("yearLevel") private Integer yearLevel;
    @SerializedName("role") private String role;
    @SerializedName("createdAt") private String createdAt;

    public String getAuthUserId() { return authUserId; }
    public String getStudentId() { return studentId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public boolean isEmailVerified() { return emailVerified; }
    public String getCourse() { return course; }
    public Integer getYearLevel() { return yearLevel; }
    public String getRole() { return role; }
    public String getCreatedAt() { return createdAt; }
}
