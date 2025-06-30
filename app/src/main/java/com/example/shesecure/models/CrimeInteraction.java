// CrimeInteraction.java
package com.example.shesecure.models;

import java.util.List;

public class CrimeInteraction {
    private int supports;
    private int unsupports;
    private List<Comment> comments;
    private UserInteraction userInteraction;

    // Constructors
    public CrimeInteraction() {}

    public CrimeInteraction(int supports, int unsupports, List<Comment> comments, UserInteraction userInteraction) {
        this.supports = supports;
        this.unsupports = unsupports;
        this.comments = comments;
        this.userInteraction = userInteraction;
    }

    // Getters and Setters
    public int getSupports() {
        return supports;
    }

    public void setSupports(int supports) {
        this.supports = supports;
    }

    public int getUnsupports() {
        return unsupports;
    }

    public void setUnsupports(int unsupports) {
        this.unsupports = unsupports;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public UserInteraction getUserInteraction() {
        return userInteraction;
    }

    public void setUserInteraction(UserInteraction userInteraction) {
        this.userInteraction = userInteraction;
    }
}
