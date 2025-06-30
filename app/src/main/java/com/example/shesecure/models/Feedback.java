package com.example.shesecure.models;

import com.google.gson.annotations.SerializedName;

public class Feedback {
    @SerializedName("_id")
    private String id;

    @SerializedName("userId")
    private User userId;

    @SerializedName("rating")
    private int rating;

    @SerializedName("review")
    private String review;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    // Constructors
    public Feedback() {}

    public Feedback(String id, User userId, int rating, String review, String createdAt, String updatedAt) {
        this.id = id;
        this.userId = userId;
        this.rating = rating;
        this.review = review;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUserId() {
        return userId;
    }

    public void setUserId(User userId) {
        this.userId = userId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getReview() {
        return review;
    }

    public void setReview(String review) {
        this.review = review;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Inner User class
    public static class User {
        @SerializedName("_id")
        private String id;

        @SerializedName("firstName")
        private String firstName;

        @SerializedName("lastName")
        private String lastName;

        @SerializedName("email")
        private String email;

        @SerializedName("additionalDetails")
        private AdditionalDetails additionalDetails;

        // Constructors
        public User() {}

        public User(String id, String firstName, String lastName, String email, AdditionalDetails additionalDetails) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.additionalDetails = additionalDetails;
        }

        // Getters and Setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public AdditionalDetails getAdditionalDetails() {
            return additionalDetails;
        }

        public void setAdditionalDetails(AdditionalDetails additionalDetails) {
            this.additionalDetails = additionalDetails;
        }

        // Inner AdditionalDetails class
        public static class AdditionalDetails {
            @SerializedName("image")
            private String image;

            // Constructors
            public AdditionalDetails() {}

            public AdditionalDetails(String image) {
                this.image = image;
            }

            // Getters and Setters
            public String getImage() {
                return image;
            }

            public void setImage(String image) {
                this.image = image;
            }
        }
    }
}