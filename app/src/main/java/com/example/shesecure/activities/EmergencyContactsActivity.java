package com.example.shesecure.activities;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.shesecure.R;
import com.example.shesecure.adapters.EmergencyContactAdapter;
import com.example.shesecure.models.EmergencyContact;
import com.example.shesecure.models.EmergencyContactResponse;
import com.example.shesecure.services.ApiService;
import com.example.shesecure.utils.ApiUtils;
import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EmergencyContactsActivity extends BaseActivity
        implements EmergencyContactAdapter.OnContactActionListener {

    private RecyclerView contactsRecyclerView;
    private EmergencyContactAdapter adapter;
    private List<EmergencyContact> contacts = new ArrayList<>();
    private EditText nameEditText, phoneEditText;
    private Button addButton, cancelButton;
    private String currentEditingId = null;
    private ApiService apiService;
    private String authToken;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_contacts);
        apiService = ApiUtils.initializeApiService(this, ApiService.class);

        initializeViews();
        setupRecyclerView();
        setupButtonListeners();
        loadContacts();
    }

    private void initializeViews() {
        contactsRecyclerView = findViewById(R.id.contacts_recycler_view);
        nameEditText = findViewById(R.id.et_name);
        phoneEditText = findViewById(R.id.et_phone);
        addButton = findViewById(R.id.btn_add);
        cancelButton = findViewById(R.id.btn_cancel);

        authToken = "Bearer " + authManager.getToken();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
    }

    private void setupRecyclerView() {
        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmergencyContactAdapter(contacts, this);
        contactsRecyclerView.setAdapter(adapter);
    }

    private void setupButtonListeners() {
        addButton.setOnClickListener(v -> handleSubmit());
        cancelButton.setOnClickListener(v -> cancelEdit());
    }

    private void loadContacts() {
        progressDialog.show();
        Call<EmergencyContactResponse> call = apiService.getEmergencyContacts(authToken);

        call.enqueue(new Callback<EmergencyContactResponse>() {
            @Override
            public void onResponse(Call<EmergencyContactResponse> call,
                                   Response<EmergencyContactResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful() && response.body() != null) {
                    contacts = response.body().getContacts();
                    adapter.updateContacts(contacts);
                    if (contacts.isEmpty()) {
                        showToast("No emergency contacts found");
                    }
                } else {
                    showToast("Failed to load contacts");
                }
            }

            @Override
            public void onFailure(Call<EmergencyContactResponse> call, Throwable t) {
                progressDialog.dismiss();
                showToast("Error: " + t.getMessage());
            }
        });
    }

    private void handleSubmit() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        if (!isValidPhone(phone)) {
            phoneEditText.setError("Please enter a valid 10-digit phone number");
            return;
        }

        EmergencyContact contact = new EmergencyContact(name, phone);
        if (currentEditingId != null) {
            updateContact(currentEditingId, contact);
        } else {
            addContact(contact);
        }
    }

    private boolean isValidPhone(String phone) {
        return phone.matches("\\d{10}");
    }

    private void addContact(EmergencyContact contact) {
        progressDialog.show();
        Call<EmergencyContactResponse> call = apiService.addEmergencyContact(authToken, contact);

        call.enqueue(new Callback<EmergencyContactResponse>() {
            @Override
            public void onResponse(Call<EmergencyContactResponse> call,
                                   Response<EmergencyContactResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    showToast("Contact added successfully");
                    resetForm();
                    loadContacts();
                } else {
                    showToast("Failed to add contact");
                }
            }

            @Override
            public void onFailure(Call<EmergencyContactResponse> call, Throwable t) {
                progressDialog.dismiss();
                showToast("Error: " + t.getMessage());
            }
        });
    }

    private void updateContact(String contactId, EmergencyContact contact) {
        progressDialog.show();
        Call<EmergencyContactResponse> call =
                apiService.updateEmergencyContact(contactId, authToken, contact);

        call.enqueue(new Callback<EmergencyContactResponse>() {
            @Override
            public void onResponse(Call<EmergencyContactResponse> call,
                                   Response<EmergencyContactResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    showToast("Contact updated successfully");
                    resetForm();
                    loadContacts();
                } else {
                    showToast("Failed to update contact");
                }
            }

            @Override
            public void onFailure(Call<EmergencyContactResponse> call, Throwable t) {
                progressDialog.dismiss();
                showToast("Error: " + t.getMessage());
            }
        });
    }

    private void resetForm() {
        nameEditText.setText("");
        phoneEditText.setText("");
        currentEditingId = null;
        addButton.setText("Add Contact");
        cancelButton.setVisibility(View.GONE);
    }

    private void cancelEdit() {
        resetForm();
    }

    @Override
    public void onEditContact(EmergencyContact contact) {
        nameEditText.setText(contact.getName());
        phoneEditText.setText(contact.getPhoneNumber());
        currentEditingId = contact.getId();
        addButton.setText("Update");
        cancelButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDeleteContact(String contactId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Contact")
                .setMessage("Are you sure you want to delete this contact?")
                .setPositiveButton("Delete", (dialog, which) -> deleteContact(contactId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteContact(String contactId) {
        progressDialog.show();
        Call<EmergencyContactResponse> call =
                apiService.removeEmergencyContact(contactId, authToken);

        call.enqueue(new Callback<EmergencyContactResponse>() {
            @Override
            public void onResponse(Call<EmergencyContactResponse> call,
                                   Response<EmergencyContactResponse> response) {
                progressDialog.dismiss();
                if (response.isSuccessful()) {
                    showToast("Contact deleted successfully");
                    loadContacts();
                } else {
                    showToast("Failed to delete contact");
                }
            }

            @Override
            public void onFailure(Call<EmergencyContactResponse> call, Throwable t) {
                progressDialog.dismiss();
                showToast("Error: " + t.getMessage());
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}