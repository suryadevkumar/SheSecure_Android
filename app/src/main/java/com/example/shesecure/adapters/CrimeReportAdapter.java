package com.example.shesecure.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.shesecure.R;
import com.example.shesecure.models.CrimeReport;
import com.example.shesecure.models.Suspect;
import com.example.shesecure.models.Witness;

import java.util.List;

public class CrimeReportAdapter extends RecyclerView.Adapter<CrimeReportAdapter.ReportViewHolder> {

    private List<CrimeReport> reportList;
    private String userType;
    private Context context;
    private OnReportActionListener listener;
    private int expandedPosition = -1;

    public interface OnReportActionListener {
        void onVerifyReport(String reportId);
        void onDeleteReport(String reportId);
        void onPhotoClicked(String photoUrl);
        void onVideoClicked(String videoUrl);
    }

    public CrimeReportAdapter(List<CrimeReport> reportList, String userType, Context context) {
        this.reportList = reportList;
        this.userType = userType;
        this.context = context;
        if (context instanceof OnReportActionListener) {
            this.listener = (OnReportActionListener) context;
        }
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_crime_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        CrimeReport report = reportList.get(position);

        // Basic info
        holder.tvCrimeType.setText(report.getTypeOfCrime());
        holder.tvStatus.setText(report.getStatus());
        holder.tvDate.setText(report.getFormattedDate());

        // Status background
        holder.tvStatus.setBackgroundResource(R.drawable.bg_rounded);
        Drawable background = holder.tvStatus.getBackground();
        if (background instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) background;
            switch (report.getStatus()) {
                case "Verified":
                    drawable.setColor(ContextCompat.getColor(context, R.color.green_600));
                    break;
                case "In Progress":
                    drawable.setColor(ContextCompat.getColor(context, R.color.yellow_400));
                    break;
                default:
                    drawable.setColor(ContextCompat.getColor(context, R.color.gray_600));
            }
        }

        // Expand/collapse logic
        final boolean isExpanded = position == expandedPosition;
        holder.detailsLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.ivExpand.setImageResource(isExpanded ? R.drawable.arrow_up : R.drawable.arrow_down);

        holder.ivExpand.setOnClickListener(v -> {
            expandedPosition = isExpanded ? -1 : position;
            notifyDataSetChanged();
        });

        // Admin actions
        if (userType.equals("Admin")) {
            holder.layoutAdminActions.setVisibility(View.VISIBLE);
            holder.btnVerify.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVerifyReport(report.getId());
                }
            });
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteReport(report.getId());
                }
            });
        } else {
            holder.layoutAdminActions.setVisibility(View.GONE);
        }

        // Expanded details
        if (isExpanded) {
            // Description
            holder.tvDescription.setText(report.getDescription());

            // Location details
            if (report.getLocation() != null) {
                holder.tvLocation.setText(report.getLocation().getFormattedAddress());
                holder.tvCoordinates.setText(String.format("%s, %s",
                        report.getLocation().getLatitude(),
                        report.getLocation().getLongitude()));
            }

            // FIR document
            if (report.getFirUrl() != null && !report.getFirUrl().isEmpty()) {
                holder.layoutFir.setVisibility(View.VISIBLE);
                Glide.with(context)
                        .load(report.getFirUrl())
                        .placeholder(R.drawable.document)
                        .into(holder.ivFir);

                holder.ivFir.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onPhotoClicked(report.getFirUrl());
                    }
                });
            } else {
                holder.layoutFir.setVisibility(View.GONE);
            }

            // Crime photos
            if (report.getPhotoUrls() != null && !report.getPhotoUrls().isEmpty()) {
                holder.layoutPhotos.setVisibility(View.VISIBLE);
                setupPhotoRecyclerView(holder.rvPhotos, report.getPhotoUrls());
            } else {
                holder.layoutPhotos.setVisibility(View.GONE);
            }

            // Crime videos
            if (report.getVideoUrls() != null && !report.getVideoUrls().isEmpty()) {
                holder.layoutVideos.setVisibility(View.VISIBLE);
                holder.tvVideoCount.setText(String.format("Videos (%d)", report.getVideoUrls().size()));
                setupVideoViews(holder.llVideos, report.getVideoUrls());
            } else {
                holder.layoutVideos.setVisibility(View.GONE);
            }

            // Suspects
            if (report.getSuspects() != null && !report.getSuspects().isEmpty()) {
                holder.layoutSuspects.setVisibility(View.VISIBLE);
                holder.tvSuspectCount.setText(String.format("Suspects (%d)", report.getSuspects().size()));
                setupSuspects(holder.llSuspects, report.getSuspects());
            } else {
                holder.layoutSuspects.setVisibility(View.GONE);
            }

            // Witnesses
            if (report.getWitnesses() != null && !report.getWitnesses().isEmpty()) {
                holder.layoutWitnesses.setVisibility(View.VISIBLE);
                holder.tvWitnessCount.setText(String.format("Witnesses (%d)", report.getWitnesses().size()));
                setupWitnesses(holder.llWitnesses, report.getWitnesses());
            } else {
                holder.layoutWitnesses.setVisibility(View.GONE);
            }
        }
    }

    private void setupPhotoRecyclerView(RecyclerView recyclerView, List<String> photoUrls) {
        recyclerView.setLayoutManager(new LinearLayoutManager(context,
                LinearLayoutManager.HORIZONTAL, false));
        MediaAdapter adapter = new MediaAdapter(photoUrls, true, url -> {
            if (listener != null) {
                listener.onPhotoClicked(url);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupVideoViews(LinearLayout container, List<String> videoUrls) {
        container.removeAllViews();
        for (String videoUrl : videoUrls) {
            View videoView = LayoutInflater.from(context)
                    .inflate(R.layout.item_crime_videoes, container, false);

            ImageView ivThumbnail = videoView.findViewById(R.id.ivThumbnail);
            // You would load video thumbnail here

            videoView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVideoClicked(videoUrl);
                }
            });
            container.addView(videoView);
        }
    }

    private void setupSuspects(LinearLayout container, List<Suspect> suspects) {
        container.removeAllViews();
        for (Suspect suspect : suspects) {
            View suspectView = LayoutInflater.from(context)
                    .inflate(R.layout.item_suspect, container, false);

            ImageView ivPhoto = suspectView.findViewById(R.id.ivSuspectPhoto);
            TextView tvName = suspectView.findViewById(R.id.tvSuspectName);
            TextView tvGender = suspectView.findViewById(R.id.tvSuspectGender);

            tvName.setText(suspect.getName());
            tvGender.setText(suspect.getGender());

            if (suspect.getPhotoUrl() != null && !suspect.getPhotoUrl().isEmpty()) {
                Glide.with(context)
                        .load(suspect.getPhotoUrl())
                        .placeholder(R.drawable.person)
                        .into(ivPhoto);

                ivPhoto.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onPhotoClicked(suspect.getPhotoUrl());
                    }
                });
            }
            container.addView(suspectView);
        }
    }

    private void setupWitnesses(LinearLayout container, List<Witness> witnesses) {
        container.removeAllViews();
        for (Witness witness : witnesses) {
            View witnessView = LayoutInflater.from(context)
                    .inflate(R.layout.item_witness, container, false);

            ImageView ivPhoto = witnessView.findViewById(R.id.ivWitnessPhoto);
            TextView tvName = witnessView.findViewById(R.id.tvWitnessName);
            TextView tvContact = witnessView.findViewById(R.id.tvWitnessContact);
            TextView tvAddress = witnessView.findViewById(R.id.tvWitnessAddress);

            tvName.setText(witness.getName());
            tvContact.setText(witness.getContactNumber());
            tvAddress.setText(witness.getAddress());

            if (witness.getPhotoUrl() != null && !witness.getPhotoUrl().isEmpty()) {
                Glide.with(context)
                        .load(witness.getPhotoUrl())
                        .placeholder(R.drawable.person)
                        .into(ivPhoto);

                ivPhoto.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onPhotoClicked(witness.getPhotoUrl());
                    }
                });
            }
            container.addView(witnessView);
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvCrimeType, tvDescription, tvStatus, tvDate;
        ImageView ivExpand, ivFir;
        LinearLayout detailsLayout, layoutAdminActions, layoutFir, layoutPhotos, layoutVideos;
        LinearLayout layoutSuspects, layoutWitnesses, llSuspects, llWitnesses, llVideos;
        TextView tvLocation, tvCoordinates, tvSuspectCount, tvWitnessCount, tvVideoCount;
        Button btnVerify, btnDelete;
        RecyclerView rvPhotos;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCrimeType = itemView.findViewById(R.id.tvCrimeType);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            ivExpand = itemView.findViewById(R.id.ivExpand);
            detailsLayout = itemView.findViewById(R.id.detailsLayout);
            layoutAdminActions = itemView.findViewById(R.id.layoutAdminActions);
            btnVerify = itemView.findViewById(R.id.btnVerify);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            // Expanded views
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvCoordinates = itemView.findViewById(R.id.tvCoordinates);
            layoutFir = itemView.findViewById(R.id.layoutFir);
            ivFir = itemView.findViewById(R.id.ivFir);
            layoutPhotos = itemView.findViewById(R.id.layoutPhotos);
            rvPhotos = itemView.findViewById(R.id.rvPhotos);
            layoutVideos = itemView.findViewById(R.id.layoutVideos);
            tvVideoCount = itemView.findViewById(R.id.tvVideoCount);
            llVideos = itemView.findViewById(R.id.llVideos);
            layoutSuspects = itemView.findViewById(R.id.layoutSuspects);
            tvSuspectCount = itemView.findViewById(R.id.tvSuspectCount);
            llSuspects = itemView.findViewById(R.id.llSuspects);
            layoutWitnesses = itemView.findViewById(R.id.layoutWitnesses);
            tvWitnessCount = itemView.findViewById(R.id.tvWitnessCount);
            llWitnesses = itemView.findViewById(R.id.llWitnesses);
        }
    }
}