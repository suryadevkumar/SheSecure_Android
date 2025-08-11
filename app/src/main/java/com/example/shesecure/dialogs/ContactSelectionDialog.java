package com.example.shesecure.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.shesecure.R;
import com.example.shesecure.models.EmergencyContact;
import java.util.ArrayList;
import java.util.List;

public class ContactSelectionDialog extends Dialog {
    private List<EmergencyContact> contacts;
    private ContactSelectionListener listener;
    private LinearLayout contactsContainer;
    private Button btnCancel, btnConfirm;

    public interface ContactSelectionListener {
        void onContactsSelected(List<String> selectedContactNumbers);
        void onCancelled();
    }

    public ContactSelectionDialog(Context context,
                                  List<EmergencyContact> contacts,
                                  ContactSelectionListener listener) {
        super(context);
        this.contacts = contacts;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_contact_selection);

        contactsContainer = findViewById(R.id.contacts_container);
        btnCancel = findViewById(R.id.btn_cancel);
        btnConfirm = findViewById(R.id.btn_confirm);

        // Populate contacts
        for (EmergencyContact contact : contacts) {
            View contactView = getLayoutInflater().inflate(
                    R.layout.item_contact_select, contactsContainer, false);

            TextView tvName = contactView.findViewById(R.id.tv_name);
            TextView tvNumber = contactView.findViewById(R.id.tv_number);
            CheckBox checkBox = contactView.findViewById(R.id.checkbox);

            tvName.setText(contact.getName());
            tvNumber.setText(contact.getPhoneNumber());
            checkBox.setTag(contact);

            contactsContainer.addView(contactView);
        }

        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancelled();
            }
            dismiss();
        });

        btnConfirm.setOnClickListener(v -> {
            List<String> selectedNumbers = new ArrayList<>();

            for (int i = 0; i < contactsContainer.getChildCount(); i++) {
                View child = contactsContainer.getChildAt(i);
                CheckBox checkBox = child.findViewById(R.id.checkbox);
                if (checkBox.isChecked()) {
                    EmergencyContact contact = (EmergencyContact) checkBox.getTag();
                    selectedNumbers.add(contact.getPhoneNumber());
                }
            }

            if (listener != null) {
                listener.onContactsSelected(selectedNumbers);
            }
            dismiss();
        });
    }
}