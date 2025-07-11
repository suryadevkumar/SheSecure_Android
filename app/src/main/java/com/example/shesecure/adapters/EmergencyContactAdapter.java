package com.example.shesecure.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.shesecure.R;
import com.example.shesecure.models.EmergencyContact;
import java.util.List;

public class EmergencyContactAdapter extends RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder> {

    private List<EmergencyContact> contacts;
    private OnContactActionListener listener;

    public EmergencyContactAdapter(List<EmergencyContact> contacts, OnContactActionListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        EmergencyContact contact = contacts.get(position);
        holder.bind(contact);
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void updateContacts(List<EmergencyContact> contacts) {
        this.contacts = contacts;
        notifyDataSetChanged();
    }

    class ContactViewHolder extends RecyclerView.ViewHolder {
        private TextView contactName, contactPhone;
        private ImageButton btnCall, btnMore;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactName = itemView.findViewById(R.id.contact_name);
            contactPhone = itemView.findViewById(R.id.contact_phone);
            btnCall = itemView.findViewById(R.id.btn_call);
            btnMore = itemView.findViewById(R.id.btn_more);
        }

        public void bind(EmergencyContact contact) {
            contactName.setText(contact.getName());
            contactPhone.setText(contact.getPhoneNumber());

            // Set click listener for call button
            btnCall.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + contact.getPhoneNumber()));
                itemView.getContext().startActivity(intent);
            });

            // Set click listener for more options button
            btnMore.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(itemView.getContext(), v);
                popup.inflate(R.menu.contact_menu);
                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_edit) {
                        listener.onEditContact(contact);
                        return true;
                    } else if (itemId == R.id.menu_delete) {
                        listener.onDeleteContact(contact.getId());
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }

    public interface OnContactActionListener {
        void onEditContact(EmergencyContact contact);
        void onDeleteContact(String contactId);
    }
}