package com.example.shesecure.services;

import com.example.shesecure.models.EmergencyContact;
import com.example.shesecure.models.FeedbackRes;
import com.example.shesecure.models.User;

import java.util.List;
import java.util.Map;

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
    Call<FeedbackRes> getAllFeedbacks();

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
    Call<ResponseBody> getUserCrimeReports(@Header("Authorization") String token);

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

    @POST("chat/request")
    Call<ResponseBody> createChatRequest(
            @Header("Authorization") String token,
            @Body RequestBody requestData
    );

    @POST("chat/request/accept")
    Call<ResponseBody> acceptChatRequest(
            @Header("Authorization") String token,
            @Body RequestBody acceptData
    );

    @POST("chat/message")
    Call<ResponseBody> sendMessage(
            @Header("Authorization") String token,
            @Body RequestBody messageData
    );

    @POST("chat/end")
    Call<ResponseBody> endChat(
            @Header("Authorization") String token,
            @Body RequestBody endData
    );

    @GET("chat/messages/unread")
    Call<ResponseBody> getUnreadCounts(@Query("userId") String userId);

    @POST("chat/typing")
    Call<ResponseBody> sendTypingIndicator(
            @Header("Authorization") String token,
            @Body RequestBody typingData
    );

    @POST("chat/stop-typing")
    Call<ResponseBody> sendStopTypingIndicator(
            @Header("Authorization") String token,
            @Body RequestBody typingData
    );

    // Response classes
    class EmergencyContactResponse {
        private List<EmergencyContact> contacts;

        public List<EmergencyContact> getContacts() {
            return contacts;
        }

        public void setContacts(List<EmergencyContact> contacts) {
            this.contacts = contacts;
        }
    }
}