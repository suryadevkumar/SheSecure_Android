package com.example.shesecure.services;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

import com.example.shesecure.models.FeedbackResponse;
import com.example.shesecure.models.User;

public interface ApiService {
    // feedback
    @GET("feedback/get-feedback")
    Call<FeedbackResponse> getAllFeedbacks();

    // Check if user exists
    @POST("auth/userExist")
    Call<ResponseBody> checkEmailExists(@Body RequestBody body);

    // Send OTP to email
    @POST("auth/send-otp")
    Call<ResponseBody> sendEmailOTP(@Body RequestBody body);

    // Verify OTP
    @POST("auth/verify-otp")
    Call<ResponseBody> verifyEmailOTP(@Body RequestBody body);

    // Login user
    @POST("auth/login")
    Call<ResponseBody> login(@Body RequestBody body);

    // Simple signup for users
    @POST("auth/signup")
    Call<ResponseBody> signup(@Body User user);

    // Signup with qualifications for counselors/admins
    @Multipart
    @POST("auth/signup")
    Call<ResponseBody> signupWithQualifications(
            @Part("firstName") RequestBody firstName,
            @Part("lastName") RequestBody lastName,
            @Part("email") RequestBody email,
            @Part("mobileNumber") RequestBody mobileNumber,
            @Part("userType") RequestBody userType,
            @Part("qualifications") RequestBody qualifications,
            @Part List<MultipartBody.Part> certificates
    );

    //location routes
    @POST("location/save-userLocation")
    Call<ResponseBody> saveUserLocation(
            @Header("Authorization") String authHeader,
            @Body RequestBody body
    );

    @GET("api/crimeInteraction/stats")
    Call<ResponseBody> getCrimeInteractionStats(
            @Header("Authorization") String token
    );

    @GET("api/crimeInteraction/crime-interaction/{crimeId}")
    Call<ResponseBody> getCrimeInteractions(
            @Header("Authorization") String token,
            @Path("crimeId") String crimeId
    );

    @POST("api/crimeInteraction/{crimeId}/interact")
    Call<ResponseBody> interactWithCrime(
            @Header("Authorization") String token,
            @Path("crimeId") String crimeId,
            @Body RequestBody body
    );

    @POST("api/crimeInteraction/{crimeId}/comment")
    Call<ResponseBody> postCrimeComment(
            @Header("Authorization") String token,
            @Path("crimeId") String crimeId,
            @Body RequestBody body
    );

}