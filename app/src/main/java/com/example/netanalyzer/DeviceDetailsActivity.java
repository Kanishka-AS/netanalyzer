package com.example.netanalyzer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.card.MaterialCardView;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DeviceDetailsActivity extends AppCompatActivity {

    private MainActivity.Device device;
    private TextView pingResult;
    private ProgressBar pingProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_details);

        // Get device data from intent
        try {
            device = (MainActivity.Device) getIntent().getSerializableExtra("device");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (device == null) {
            Toast.makeText(this, "Device information not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupToolbar(device.getDisplayName());
        populateDeviceDetails();
        setupClickListeners();
    }

    private void setupToolbar(String title) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void populateDeviceDetails() {
        try {
            // Basic Info Card
            TextView deviceName = findViewById(R.id.deviceName);
            TextView deviceIp = findViewById(R.id.deviceIp);
            TextView deviceMac = findViewById(R.id.deviceMac);
            TextView deviceVendor = findViewById(R.id.deviceVendor);
            TextView deviceType = findViewById(R.id.deviceType);

            if (deviceName != null) deviceName.setText(device.getDisplayName());
            if (deviceIp != null) deviceIp.setText(device.ipAddress != null ? device.ipAddress : "Unknown");
            if (deviceMac != null) deviceMac.setText(device.macAddress != null ? device.macAddress : "Unknown");

            // ENHANCED VENDOR DETECTION
            String vendor = device.vendor;
            if (vendor == null || vendor.equals("Unknown Vendor") || vendor.equals("Unknown")) {
                // Try to detect vendor from MAC if available
                if (device.macAddress != null && !device.macAddress.equals("Unknown")) {
                    vendor = OuiDatabaseHelper.getVendorFromMac(this, device.macAddress);
                    device.vendor = vendor; // Update the device object
                } else {
                    vendor = "Unknown Vendor";
                }
            }

            if (deviceVendor != null) deviceVendor.setText(vendor);

            // ENHANCED DEVICE TYPE DETECTION
            String deviceTypeStr = device.deviceType;
            if (deviceTypeStr == null || deviceTypeStr.equals("Unknown")) {
                deviceTypeStr = OuiDatabaseHelper.guessDeviceType(vendor, device.hostname, device.ipAddress);
                device.deviceType = deviceTypeStr; // Update the device object
            }

            if (deviceType != null) deviceType.setText(deviceTypeStr);

            // Connection Info Card
            TextView connectionStatus = findViewById(R.id.connectionStatus);
            TextView firstSeen = findViewById(R.id.firstSeen);
            TextView lastSeen = findViewById(R.id.lastSeen);
            TextView signalStrength = findViewById(R.id.signalStrength);

            if (connectionStatus != null) {
                connectionStatus.setText(device.isConnected ? "Connected" : "Disconnected");
                connectionStatus.setTextColor(getResources().getColor(
                        device.isConnected ? android.R.color.holo_green_dark : android.R.color.holo_red_dark, getTheme()));
            }

            if (firstSeen != null) firstSeen.setText(device.getFormattedFirstSeen());
            if (lastSeen != null) lastSeen.setText(device.getFormattedLastSeen());

            if (signalStrength != null) {
                signalStrength.setText(device.signalStrength != 0 ?
                        device.signalStrength + " dBm" : "Not available");
            }

            // Network Info Card
            TextView subnetInfo = findViewById(R.id.subnetInfo);
            TextView gatewayInfo = findViewById(R.id.gatewayInfo);

            if (subnetInfo != null && device.ipAddress != null) {
                String ip = device.ipAddress;
                String subnet = ip.substring(0, ip.lastIndexOf('.') + 1) + "0/24";
                subnetInfo.setText(subnet);
            }

            // Check if this device is the gateway
            String gateway = getIntent().getStringExtra("gateway");
            if (gatewayInfo != null) {
                if (gateway != null && device.ipAddress.equals(gateway)) {
                    gatewayInfo.setText(device.ipAddress + " (This Router)");
                    gatewayInfo.setTextColor(getResources().getColor(android.R.color.holo_orange_dark, getTheme()));
                } else if (gateway != null) {
                    gatewayInfo.setText(gateway);
                } else {
                    gatewayInfo.setText("192.168.1.1");
                }
            }

            // Ping Test Card
            pingResult = findViewById(R.id.pingResult);
            pingProgress = findViewById(R.id.pingProgress);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading device details", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        // Ping Test
        MaterialCardView pingTestCard = findViewById(R.id.pingTestCard);
        if (pingTestCard != null) {
            pingTestCard.setOnClickListener(v -> startPingTest());
        }

        // Port Scan
        MaterialCardView portScanCard = findViewById(R.id.portScanCard);
        if (portScanCard != null) {
            portScanCard.setOnClickListener(v -> startPortScan());
        }
    }

    private void startPingTest() {
        if (device == null || device.ipAddress == null) {
            Toast.makeText(this, "No device IP to ping", Toast.LENGTH_SHORT).show();
            return;
        }

        new PingTestTask().execute(device.ipAddress);
    }

    private void startPortScan() {
        if (device == null || device.ipAddress == null) {
            Toast.makeText(this, "No device IP to scan", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Port scan feature coming soon!", Toast.LENGTH_SHORT).show();
        // We'll implement this in Phase 3
    }

    private class PingTestTask extends AsyncTask<String, Void, PingResult> {

        @Override
        protected void onPreExecute() {
            if (pingResult != null) {
                pingResult.setText("Pinging...");
                pingResult.setTextColor(getResources().getColor(android.R.color.black, getTheme()));
            }
            if (pingProgress != null) {
                pingProgress.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected PingResult doInBackground(String... ips) {
            String ip = ips[0];
            PingResult result = new PingResult();
            result.ip = ip;

            try {
                long startTime = System.currentTimeMillis();
                InetAddress address = InetAddress.getByName(ip);
                boolean reachable = address.isReachable(3000); // 3 second timeout
                long endTime = System.currentTimeMillis();

                result.success = reachable;
                result.time = endTime - startTime;

            } catch (Exception e) {
                result.success = false;
                result.error = e.getMessage();
            }

            return result;
        }

        @Override
        protected void onPostExecute(PingResult result) {
            if (pingProgress != null) {
                pingProgress.setVisibility(View.GONE);
            }

            if (pingResult != null) {
                if (result.success) {
                    pingResult.setText("✅ Ping successful!\nResponse time: " + result.time + "ms");
                    pingResult.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
                } else {
                    pingResult.setText("❌ Ping failed\n" +
                            (result.error != null ? result.error : "Device not responding"));
                    pingResult.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
                }
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    // Helper class for ping results
    private static class PingResult {
        String ip;
        boolean success;
        long time;
        String error;
    }
}