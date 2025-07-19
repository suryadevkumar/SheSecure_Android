package com.example.shesecure.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shesecure.R;
import com.example.shesecure.models.ChatRoom;

import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder> {
    private List<ChatRoom> chatRooms;
    private OnChatRoomClickListener listener;

    public interface OnChatRoomClickListener {
        void onChatRoomClick(ChatRoom chatRoom) throws JSONException;
    }

    public ChatRoomAdapter(List<ChatRoom> chatRooms, OnChatRoomClickListener listener) {
        this.chatRooms = chatRooms;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_room, parent, false);
        return new ChatRoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomViewHolder holder, int position) {
        ChatRoom room = chatRooms.get(position);
        holder.bind(room);
    }

    @Override
    public int getItemCount() {
        return chatRooms.size();
    }

    class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView, problemTextView, timeTextView, unreadCountTextView, userInitialsView, chatStatusTextView;
        private View onlineIndicator;

        public ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            chatStatusTextView = itemView.findViewById(R.id.chatStatusTextView);
            userInitialsView = itemView.findViewById(R.id.userInitialsView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            problemTextView = itemView.findViewById(R.id.problemTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            unreadCountTextView = itemView.findViewById(R.id.unreadCountTextView);
            onlineIndicator = itemView.findViewById(R.id.onlineIndicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    try {
                        listener.onChatRoomClick(chatRooms.get(position));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

        public void bind(ChatRoom room) {
            // Set room name based on user type
            SharedPreferences prefs = itemView.getContext().getSharedPreferences("SheSecurePrefs", Context.MODE_PRIVATE);
            String userType = prefs.getString("userType", null);

            if ("User".equals(userType)) {
                nameTextView.setText(room.getCounsellor().getFullName());
                userInitialsView.setText(room.getCounsellor().getInitials().toUpperCase());
            } else {
                nameTextView.setText(room.getUser().getFullName());
                userInitialsView.setText(room.getUser().getInitials().toUpperCase());
            }

            // Set last message preview
            problemTextView.setText(room.getChatRequest().getProblemType() + ": " + room.getChatRequest().getBrief());

            // Set time
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                Date date = sdf.parse(room.getCreatedAt());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                timeTextView.setText(outputFormat.format(date));
            } catch (ParseException e) {
                e.printStackTrace();
                timeTextView.setText("");
            }

            // Set unread count
            if (room.getUnreadCount() > 0) {
                unreadCountTextView.setVisibility(View.VISIBLE);
                unreadCountTextView.setText(String.valueOf(room.getUnreadCount()));
            } else {
                unreadCountTextView.setVisibility(View.GONE);
            }

            if (room.isOnline()) {
                onlineIndicator.setVisibility(View.VISIBLE);
            } else {
                onlineIndicator.setVisibility(View.GONE);
            }

            if(room.isEnded()){
                chatStatusTextView.setVisibility(View.VISIBLE);
            }
            else{
                chatStatusTextView.setVisibility(View.GONE);
            }
        }
    }
}
