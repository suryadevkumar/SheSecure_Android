package com.example.shesecure.services;

import com.example.shesecure.models.CrimeReport;
import com.example.shesecure.models.EmergencyContact;
import com.example.shesecure.models.EmergencyContactResponse;
import com.example.shesecure.models.FeedbackResponse;
import com.example.shesecure.models.PlacesResponse;
import com.example.shesecure.models.User;

import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    // Auth endpoints
    @POST("auth/userExist")
    Call<ResponseBody> checkEmailExists(@Body RequestBody body);

    @POST("auth/send-otp")
    Call<ResponseBody> sendEmailOTP(@Body RequestBody body);

    @POST("auth/verify-otp")
    Call<ResponseBody> verifyEmailOTP(@Body RequestBody body);

    @POST("auth/login")
    Call<ResponseBody> login(@Body RequestBody body);

    @POST("auth/signup")
    Call<ResponseBody> signup(@Body User user);

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

    // Emergency Contacts endpoints
    @GET("emergency-contacts/get")
    Call<EmergencyContactResponse> getEmergencyContacts(@Header("Authorization") String token);

    @POST("emergency-contacts/add")
    Call<EmergencyContactResponse> addEmergencyContact(
            @Header("Authorization") String token,
            @Body EmergencyContact contact
    );

    @PUT("emergency-contacts/update/{contactId}")
    Call<EmergencyContactResponse> updateEmergencyContact(
            @Path("contactId") String contactId,
            @Header("Authorization") String token,
            @Body EmergencyContact contact
    );

    @DELETE("emergency-contacts/remove/{contactId}")
    Call<EmergencyContactResponse> removeEmergencyContact(
            @Path("contactId") String contactId,
            @Header("Authorization") String token
    );

    // Feedback endpoints
    @GET("feedback/get-feedback")
    Call<FeedbackResponse> getAllFeedbacks();

    // Location endpoints
    @POST("location/save-userLocation")
    Call<ResponseBody> saveUserLocation(
            @Header("Authorization") String authHeader,
            @Body RequestBody body
    );

    // Profile routes
    @GET("profile/get-details")
    Call<ResponseBody> getUserDetails(@Header("Authorization") String token);

    @Multipart
    @PUT("profile/update-profile")
    Call<ResponseBody> updateProfile(
            @Header("Authorization") String token,
            @Part MultipartBody.Part image,
            @Part("gender") RequestBody gender,
            @Part("dob") RequestBody dob,
            @Part("address") RequestBody address
    );

    // customer care
    @POST("auth/customer-care")
    Call<ResponseBody> submitCustomerCareRequest(
            @Header("Authorization") String authToken,
            @Body RequestBody requestBody
    );

    //feedback
    @GET("feedback/user")
    Call<ResponseBody> getUserFeedback(@Header("Authorization") String authToken);

    @POST("feedback/submit-feedback")
    Call<ResponseBody> submitFeedback(
            @Header("Authorization") String authToken,
            @Body RequestBody feedbackData
    );

    @GET("crime/my-reports")
    Call<ResponseBody>  getUserCrimeReports(@Header("Authorization") String token);

    @GET("crime/getAllReports")
    Call<ResponseBody> getAllCrimeReports(@Header("Authorization") String token);

    @Multipart
    @POST("crime/report-crime")
    Call<ResponseBody> submitCrimeReport(
            @Part("typeOfCrime") RequestBody typeOfCrime,
            @Part("description") RequestBody description,
            @Part("dateOfCrime") RequestBody dateOfCrime,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @Part("address") RequestBody address,
            @Part MultipartBody.Part FIR,
            @Part List<MultipartBody.Part> photos,
            @Part List<MultipartBody.Part> videos,
            @Part("suspects") RequestBody suspects,
            @Part List<MultipartBody.Part> suspectPhotos,
            @Part("witnesses") RequestBody witnesses,
            @Part List<MultipartBody.Part> witnessPhotos
    );

    @PUT("crime/verify-report/{reportId}")
    Call<ResponseBody> verifyCrimeReport(
            @Header("Authorization") String token,
            @Path("reportId") String reportId
    );

    @DELETE("crime/remove-report/{reportId}")
    Call<ResponseBody> deleteCrimeReport(
            @Header("Authorization") String token,
            @Path("reportId") String reportId
    );

    @POST("crime/get-crimes-near-me")
    Call<CrimeNearbyResponse> getCrimesNearMe(
            @Header("Authorization") String authToken,
            @Body RequestBody body
    );

    @GET("crimeInteraction/stats")
    Call<List<CrimeInteractionStats>> getCrimeInteractions(
            @Header("Authorization") String authToken
    );

    @GET("crimeInteraction/crime-interaction/{crimeId}")
    Call<CrimeInteractionDetails> getCrimeInteractionDetails(
            @Header("Authorization") String authToken,
            @Path("crimeId") String crimeId
    );

    @POST("crimeInteraction/{crimeId}/interact")
    Call<ResponseBody> interactWithCrime(
            @Header("Authorization") String authToken,
            @Path("crimeId") String crimeId,
            @Body RequestBody body
    );

    @POST("crimeInteraction/{crimeId}/comment")
    Call<ResponseBody> postCrimeComment(
            @Header("Authorization") String authToken,
            @Path("crimeId") String crimeId,
            @Body RequestBody body
    );

    @GET("location/location-history")
    Call<ResponseBody> fetchLocationHistory(
            @Query("date") String date,
            @Header("Authorization") String token
    );

    // Chat endpoints
    @GET("chat/rooms")
    Call<ResponseBody> getChatRooms(
            @Header("Authorization") String token,
            @Query("userId") String userId
    );

    @GET("chat/requests")
    Call<ResponseBody> getChatRequests(
            @Query("userId") String userId
    );

    @GET("chat/messages")
    Call<ResponseBody> getMessages(
            @Header("Authorization") String token,
            @Query("chatRoomId") String chatRoomId
    );

    @POST("chat/messages/read")
    Call<ResponseBody> markMessagesRead(@Body RequestBody body);

    @GET("chat/messages/unread")
    Call<ResponseBody> getUnreadCounts(@Query("userId") String userId);

    @POST("https://places.googleapis.com/v1/places:searchNearby")
    Call<PlacesResponse> searchNearbyPlaces(
            @Header("X-Goog-Api-Key") String apiKey,
            @Header("X-Goog-FieldMask") String fieldMask,
            @Body RequestBody body
    );

    class CrimeNearbyResponse {
        public boolean success;
        public String message;
        public List<CrimeReport> crimes;
    }

    class CrimeInteractionStats {
        public int supports;
        public int unsupports;
        public String crimeId;
    }

    class CrimeInteractionDetails {
        public int supports;
        public int unsupports;
        public List<CrimeComment> comments;
        public UserInteraction userInteraction;
    }

    class CrimeComment {
        public String text;
        public String createdAt;
        public User user;

        // Add getters if needed
        public String getText() { return text; }
        public String getCreatedAt() { return createdAt; }
        public User getUser() { return user; }
    }

    class UserInteraction {
        public String supportStatus;
    }

}