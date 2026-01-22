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

public class DnsHistoryAdapter extends RecyclerView.Adapter<DnsHistoryAdapter.ViewHolder> {

    private List<DnsTestFragment.DnsResult> dnsHistory;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    public DnsHistoryAdapter(List<DnsTestFragment.DnsResult> dnsHistory) {
        this.dnsHistory = dnsHistory;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dns_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DnsTestFragment.DnsResult result = dnsHistory.get(position);

        holder.domainText.setText(result.getDomain());

        // If you have DNS server info, display it. Otherwise show default.
        // You might want to add getDnsServer() method to DnsResult class
        holder.dnsServerText.setText("DNS: System Default");

        // Display time
        if (result.getTimestamp() != null) {
            holder.timeText.setText(timeFormat.format(result.getTimestamp()));
        } else {
            holder.timeText.setText("Just now");
        }

        if (result.isSuccess()) {
            holder.resultText.setText(result.getIpAddress() + " (" + result.getResponseTime() + "ms)");
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
        return dnsHistory.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView domainText, dnsServerText, timeText, resultText;

        ViewHolder(View itemView) {
            super(itemView);
            domainText = itemView.findViewById(R.id.domainText);
            dnsServerText = itemView.findViewById(R.id.dnsServerText);
            timeText = itemView.findViewById(R.id.timeText);
            resultText = itemView.findViewById(R.id.resultText);
        }
    }
}