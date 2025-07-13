package com.example.shesecure.services;

import com.example.shesecure.models.EmergencyContact;
import com.example.shesecure.models.FeedbackRes;
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
            @Header("Authorization") String token,
            @Part("typeOfCrime") RequestBody typeOfCrime,
            @Part("description") RequestBody description,
            @Part("dateOfCrime") RequestBody dateOfCrime,
            @Part("location") RequestBody location,
            @Part MultipartBody.Part FIR
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

    // Crime interaction endpoints
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