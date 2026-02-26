package com.example.ucms_android.model;

public class RegisterRequest {
    private String studentId;
    private String fullName;
    private String course;
    private int yearLevel;
    private String password;

    public RegisterRequest(String studentId, String fullName, String course, int yearLevel, String password) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.course = course;
        this.yearLevel = yearLevel;
        this.password = password;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCourse() {
        return course;
    }

    public int getYearLevel() {
        return yearLevel;
    }

    public String getPassword() {
        return password;
    }
}
