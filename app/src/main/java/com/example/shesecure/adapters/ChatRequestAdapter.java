package com.example.shesecure.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shesecure.R;
import com.example.shesecure.models.ChatRequest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatRequestAdapter extends RecyclerView.Adapter<ChatRequestAdapter.ChatRequestViewHolder> {
    private List<ChatRequest> chatRequests;
    private OnChatRequestClickListener listener;
    private String userType;

    public interface OnChatRequestClickListener {
        void onChatRequestClick(ChatRequest chatRequest);
    }

    public ChatRequestAdapter(List<ChatRequest> chatRequests, OnChatRequestClickListener listener, String userType) {
        this.chatRequests = chatRequests;
        this.listener = listener;
        this.userType = userType;
    }

    @NonNull
    @Override
    public ChatRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_request, parent, false);
        return new ChatRequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRequestViewHolder holder, int position) {
        ChatRequest request = chatRequests.get(position);
        holder.bind(request);
    }

    @Override
    public int getItemCount() {
        return chatRequests.size();
    }

    class ChatRequestViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView, problemTypeTextView, briefTextView, timeTextView;
        private Button acceptButton;
        private LinearLayout waitingLayout;

        public ChatRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            problemTypeTextView = itemView.findViewById(R.id.problemTypeTextView);
            briefTextView = itemView.findViewById(R.id.briefTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            waitingLayout = itemView.findViewById(R.id.waitingLayout);

            acceptButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onChatRequestClick(chatRequests.get(position));
                }
            });
        }

        public void bind(ChatRequest request) {

            if ("User".equals(userType)) {
                nameTextView.setVisibility(View.GONE);
                acceptButton.setVisibility(View.GONE);
                waitingLayout.setVisibility(View.VISIBLE);
            } else {
                nameTextView.setVisibility(View.VISIBLE);
                acceptButton.setVisibility(View.VISIBLE);
                waitingLayout.setVisibility(View.GONE);
                nameTextView.setText(request.getUser().getFullName());
            }

            problemTypeTextView.setText(request.getProblemType()+": ");
            briefTextView.setText(request.getBrief());

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                Date date = sdf.parse(request.getCreatedAt());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                timeTextView.setText(outputFormat.format(date));
            } catch (ParseException e) {
                e.printStackTrace();
                timeTextView.setText("");
            }
        }
    }
}