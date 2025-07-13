package com.example.shesecure.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.shesecure.R;
import com.example.shesecure.activities.CrimeReportActivity;
import com.example.shesecure.models.CrimeReport;

import java.util.List;

public class CrimeReportAdapter extends RecyclerView.Adapter<CrimeReportAdapter.ReportViewHolder> {
    private List<CrimeReport> reportList;
    private String userType;
    private Context context;

    public CrimeReportAdapter(List<CrimeReport> reportList, String userType, Context context) {
        this.reportList = reportList;
        this.userType = userType;
        this.context = context;
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

        holder.tvCrimeType.setText(report.getTypeOfCrime());
        holder.tvStatus.setText(report.getStatus());
        holder.tvDate.setText(report.getFormattedDate());

        // Set status color
        int statusColor = report.getStatus().equals("Verified") ?
                ContextCompat.getColor(context, R.color.green) :
                ContextCompat.getColor(context, R.color.orange);
        holder.tvStatus.setTextColor(statusColor);

        // Show/hide admin actions
        if (userType.equals("Admin") && report.getStatus().equals("In Progress")) {
            holder.btnVerify.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
        } else {
            holder.btnVerify.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        }

        // Set click listeners
        holder.btnVerify.setOnClickListener(v -> {
            ((CrimeReportActivity) context).verifyReport(report.getId());
        });

        holder.btnDelete.setOnClickListener(v -> {
            ((CrimeReportActivity) context).deleteReport(report.getId());
        });

        // Set item click listener
        holder.itemView.setOnClickListener(v -> {
            // Open detail view if needed
        });
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvCrimeType, tvStatus, tvDate;
        Button btnVerify, btnDelete;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCrimeType = itemView.findViewById(R.id.tvCrimeType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvDate = itemView.findViewById(R.id.tvDate);
            btnVerify = itemView.findViewById(R.id.btnVerify);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}