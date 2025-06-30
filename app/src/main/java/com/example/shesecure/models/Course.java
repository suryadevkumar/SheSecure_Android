package com.example.shesecure.models;

import java.io.File;

public class Course {
    private String courseName;
    private String percentage;
    private File certificateFile;

    public Course(String courseName, String percentage, File certificateFile) {
        this.courseName = courseName;
        this.percentage = percentage;
        this.certificateFile = certificateFile;
    }

    // Getters
    public String getCourseName() {
        return courseName;
    }

    public String getPercentage() {
        return percentage;
    }

    public File getCertificateFile() {
        return certificateFile;
    }
}