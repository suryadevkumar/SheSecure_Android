package com.example.shesecure.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shesecure.R;
import com.example.shesecure.models.Place;

import java.util.ArrayList;
import java.util.List;

public class PlaceAdapter extends RecyclerView.Adapter<PlaceAdapter.PlaceViewHolder> {
    private List<Place> places;
    private final OnPlaceClickListener clickListener;

    public interface OnPlaceClickListener {
        void onPlaceClick(Place place);
    }

    public PlaceAdapter(List<Place> places, OnPlaceClickListener clickListener) {
        this.places = places;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public PlaceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_place, parent, false);
        return new PlaceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlaceViewHolder holder, int position) {
        Place place = places.get(position);
        holder.name.setText(place.getDisplayName());
        holder.address.setText(place.getFormattedAddress());

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPlaceClick(place);
            }
        });
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    public void updatePlaces(List<Place> newPlaces) {
        this.places = newPlaces;
        notifyDataSetChanged();
    }

    static class PlaceViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView address;

        public PlaceViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.place_name);
            address = itemView.findViewById(R.id.place_address);
        }
    }
}