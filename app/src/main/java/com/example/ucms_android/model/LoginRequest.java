package com.example.ucms_android.model;

public class LoginRequest {
    private String studentId;
    private String password;

    public LoginRequest(String studentId, String password) {
        this.studentId = studentId;
        this.password = password;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getPassword() {
        return password;
    }
}
