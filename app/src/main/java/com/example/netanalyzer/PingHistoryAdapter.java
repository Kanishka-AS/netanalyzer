package com.example.netanalyzer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PingHistoryAdapter extends RecyclerView.Adapter<PingHistoryAdapter.ViewHolder> {

    private List<PingResult> pingHistory;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public PingHistoryAdapter(List<PingResult> pingHistory) {
        this.pingHistory = pingHistory;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ping_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PingResult result = pingHistory.get(position);

        holder.targetText.setText(result.getTarget());
        if (result.getIpAddress() != null) {
            holder.ipText.setText(result.getIpAddress());
        } else {
            holder.ipText.setText("Unknown IP");
        }

        holder.timeText.setText(timeFormat.format(result.getTimestamp()));

        if (result.isSuccess()) {
            holder.resultText.setText(result.getFormattedResult());
            holder.resultText.setTextColor(holder.itemView.getResources()
                    .getColor(android.R.color.holo_green_dark, null));
        } else {
            holder.resultText.setText("Failed: " + result.getErrorMessage());
            holder.resultText.setTextColor(holder.itemView.getResources()
                    .getColor(android.R.color.holo_red_dark, null));
        }
    }

    @Override
    public int getItemCount() {
        return pingHistory.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView targetText, ipText, timeText, resultText;

        ViewHolder(View itemView) {
            super(itemView);
            targetText = itemView.findViewById(R.id.targetText);
            ipText = itemView.findViewById(R.id.ipText);
            timeText = itemView.findViewById(R.id.timeText);
            resultText = itemView.findViewById(R.id.resultText);
        }
    }
}