package com.example.ucms_android.model;

public class RegisterRequest {
    private String studentId;
    private String name;
    private String course;
    private int yearLevel;
    private String password;

    public RegisterRequest(String studentId, String name, String course, int yearLevel, String password) {
        this.studentId = studentId;
        this.name = name;
        this.course = course;
        this.yearLevel = yearLevel;
        this.password = password;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getName() {
        return name;
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
