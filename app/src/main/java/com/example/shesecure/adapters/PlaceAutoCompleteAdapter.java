package com.example.shesecure.adapters;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.Tasks;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;

import java.util.ArrayList;
import java.util.List;

public class PlaceAutoCompleteAdapter extends ArrayAdapter<AutocompletePrediction> implements Filterable {

    private static final String TAG = "PlaceAutocompleteAdapter";
    private final PlacesClient placesClient;
    private final LatLngBounds bounds;
    private List<AutocompletePrediction> predictions = new ArrayList<>();

    public PlaceAutoCompleteAdapter(Context context, PlacesClient placesClient, LatLngBounds bounds) {
        super(context, android.R.layout.simple_dropdown_item_1line);
        this.placesClient = placesClient;
        this.bounds = bounds;
    }

    @Override
    public int getCount() {
        return predictions.size();
    }

    @Override
    public AutocompletePrediction getItem(int position) {
        return predictions.get(position);
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();

                if (constraint != null) {
                    // Get the autocomplete predictions
                    FindAutocompletePredictionsRequest request = FindAutocompletePredictionsRequest.builder()
                            .setLocationBias(RectangularBounds.newInstance(bounds))
                            .setQuery(constraint.toString())
                            .build();

                    try {
                        FindAutocompletePredictionsResponse response = Tasks.await(
                                placesClient.findAutocompletePredictions(request)
                        );

                        predictions = response.getAutocompletePredictions();
                        results.values = predictions;
                        results.count = predictions.size();
                    } catch (Exception e) {
                        Log.e(TAG, "Error getting autocomplete predictions", e);
                    }
                }

                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results != null && results.count > 0) {
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }
        };
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = super.getView(position, convertView, parent);

        AutocompletePrediction item = getItem(position);
        if (item != null) {
            TextView textView = (TextView) row.findViewById(android.R.id.text1);
            textView.setText(item.getFullText(null));
        }

        return row;
    }
}