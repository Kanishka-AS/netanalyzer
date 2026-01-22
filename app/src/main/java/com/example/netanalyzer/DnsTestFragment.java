package com.example.netanalyzer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.net.InetAddress;
import java.util.Date;

public class DnsTestFragment extends Fragment {

    private EditText domainInput;
    private Button btnResolve;
    private TextView txtDomain, txtIPAddress, txtResponseTime, txtStatus;
    private ProgressBar dnsProgressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dns_test, container, false);

        // Initialize views
        domainInput = view.findViewById(R.id.domainInput);
        btnResolve = view.findViewById(R.id.btnResolve);
        txtDomain = view.findViewById(R.id.txtDomain);
        txtIPAddress = view.findViewById(R.id.txtIPAddress);
        txtResponseTime = view.findViewById(R.id.txtResponseTime);
        txtStatus = view.findViewById(R.id.txtStatus);
        dnsProgressBar = view.findViewById(R.id.dnsProgressBar);

        btnResolve.setOnClickListener(v -> resolveDNS());

        return view;
    }

    private void resolveDNS() {
        String domain = domainInput.getText().toString().trim();

        if (domain.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a domain name", Toast.LENGTH_SHORT).show();
            return;
        }

        new DnsResolutionTask().execute(domain);
    }

    private class DnsResolutionTask extends AsyncTask<String, Void, DnsResult> {

        private long startTime;

        @Override
        protected void onPreExecute() {
            startTime = System.currentTimeMillis();
            dnsProgressBar.setVisibility(View.VISIBLE);
            txtStatus.setText("Resolving...");
            txtStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        }

        @Override
        protected DnsResult doInBackground(String... params) {
            String domain = params[0];
            DnsResult result = new DnsResult(domain);

            try {
                InetAddress address = InetAddress.getByName(domain);
                long endTime = System.currentTimeMillis();

                result.setIpAddress(address.getHostAddress());
                result.setResponseTime(endTime - startTime);
                result.setSuccess(true);
                result.setTimestamp(new Date());

            } catch (Exception e) {
                result.setErrorMessage(e.getMessage());
                result.setSuccess(false);
                result.setTimestamp(new Date());
            }

            return result;
        }

        @Override
        protected void onPostExecute(DnsResult result) {
            dnsProgressBar.setVisibility(View.GONE);

            txtDomain.setText(result.getDomain());

            if (result.isSuccess()) {
                txtIPAddress.setText(result.getIpAddress());
                txtResponseTime.setText(result.getResponseTime() + " ms");
                txtStatus.setText("Success");
                txtStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));

                Toast.makeText(getContext(),
                        "Resolved to: " + result.getIpAddress(),
                        Toast.LENGTH_SHORT).show();
            } else {
                txtIPAddress.setText("Failed");
                txtResponseTime.setText("N/A");
                txtStatus.setText("Failed: " + result.getErrorMessage());
                txtStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }
        }
    }

    // PUBLIC static DnsResult class with getters/setters
    public static class DnsResult {
        private String domain;
        private String ipAddress;
        private long responseTime;
        private boolean success;
        private String errorMessage;
        private Date timestamp;

        public DnsResult(String domain) {
            this.domain = domain;
            this.timestamp = new Date();
        }

        // Getters and setters
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }

        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

        public long getResponseTime() { return responseTime; }
        public void setResponseTime(long responseTime) { this.responseTime = responseTime; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public Date getTimestamp() { return timestamp; }
        public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

        public String getFormattedResult() {
            if (success) {
                return domain + " → " + ipAddress + " (" + responseTime + "ms)";
            } else {
                return domain + " → Failed: " + errorMessage;
            }
        }
    }
}