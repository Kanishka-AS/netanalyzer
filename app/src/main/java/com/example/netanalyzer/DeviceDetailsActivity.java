package com.example.netanalyzer;

import android.content.Intent;
import android.util.Log;
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
import com.example.netanalyzer.PingResult;
import com.example.netanalyzer.PingService;
import com.example.netanalyzer.PingHistoryAdapter;

public class DeviceDetailsActivity extends AppCompatActivity implements PingService.PingCallback {

    private MainActivity.Device device;
    private TextView pingResult, progressText;
    private ProgressBar pingProgress;
    private String gateway;
    private boolean isPinging = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_details);

        // Get device data from intent
        try {
            device = (MainActivity.Device) getIntent().getSerializableExtra("device");
            gateway = getIntent().getStringExtra("gateway");
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
                    vendor = getVendorFromMac(device.macAddress);
                    device.vendor = vendor; // Update the device object
                } else {
                    vendor = "Unknown Vendor";
                }
            }

            if (deviceVendor != null) deviceVendor.setText(vendor);

            // ENHANCED DEVICE TYPE DETECTION
            String deviceTypeStr = device.deviceType;
            if (deviceTypeStr == null || deviceTypeStr.equals("Unknown")) {
                deviceTypeStr = guessDeviceType(vendor, device.hostname, device.ipAddress);
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
            progressText = findViewById(R.id.pingProgressText);
            pingProgress = findViewById(R.id.pingProgress);

            // Initialize ping result text
            if (pingResult != null) {
                pingResult.setText("Tap to test ping");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error loading device details", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupClickListeners() {
        // Ping Test - Quick test (4 pings)
        MaterialCardView pingTestCard = findViewById(R.id.pingTestCard);
        if (pingTestCard != null) {
            Log.d("DeviceDetails", "Ping test card found, setting up listeners");

            pingTestCard.setOnClickListener(v -> {
                Log.d("DeviceDetails", "Short click on ping test card");
                if (isPinging) {
                    Toast.makeText(this, "Ping test already in progress", Toast.LENGTH_SHORT).show();
                    return;
                }
                startQuickPingTest();
            });

            // Long press for advanced ping test
            pingTestCard.setOnLongClickListener(v -> {
                Log.d("DeviceDetails", "LONG PRESS detected on ping test card");
                Log.d("DeviceDetails", "Device IP: " + device.ipAddress);

                try {
                    Intent intent = new Intent(DeviceDetailsActivity.this, PingTestActivity.class);
                    intent.putExtra("device_ip", device.ipAddress);
                    if (gateway != null) {
                        intent.putExtra("gateway", gateway);
                    }

                    Log.d("DeviceDetails", "Starting PingTestActivity with IP: " + device.ipAddress);
                    startActivity(intent);
                    Log.d("DeviceDetails", "Activity started successfully");

                } catch (Exception e) {
                    Log.e("DeviceDetails", "Error starting PingTestActivity: " + e.getMessage());
                    e.printStackTrace();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                return true;  // MUST return true to consume the long press
            });
        } else {
            Log.e("DeviceDetails", "Ping test card is NULL! Check layout ID");
        }

        // Port Scan
        MaterialCardView portScanCard = findViewById(R.id.portScanCard);
        if (portScanCard != null) {
            Log.d("DeviceDetails", "Port scan card found");
            portScanCard.setOnClickListener(v -> startPortScan());
        }
    }

    private void startQuickPingTest() {
        if (device == null || device.ipAddress == null) {
            Toast.makeText(this, "No device IP to ping", Toast.LENGTH_SHORT).show();
            return;
        }

        isPinging = true;

        // Show progress
        if (pingResult != null) {
            pingResult.setText("Starting ping test...");
            pingResult.setTextColor(getResources().getColor(android.R.color.black, getTheme()));
        }
        if (progressText != null) {
            progressText.setText("Preparing to ping " + device.ipAddress);
            progressText.setVisibility(View.VISIBLE);
        }
        if (pingProgress != null) {
            pingProgress.setVisibility(View.VISIBLE);
        }

        // Start enhanced ping test (4 pings)
        PingService.quickPing(device.ipAddress, this);
    }

    private void startPortScan() {
        if (device == null || device.ipAddress == null) {
            Toast.makeText(this, "No device IP to scan", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Port scan feature coming in Phase 3!", Toast.LENGTH_SHORT).show();
        // We'll implement this in Phase 3
    }

    // PingService callback methods
    @Override
    public void onPingProgress(String message) {
        runOnUiThread(() -> {
            if (progressText != null) {
                progressText.setText(message);
            }
        });
    }

    @Override
    public void onPingComplete(PingResult result) {
        runOnUiThread(() -> {
            isPinging = false;

            if (pingProgress != null) {
                pingProgress.setVisibility(View.GONE);
            }
            if (progressText != null) {
                progressText.setVisibility(View.GONE);
            }

            if (pingResult != null) {
                if (result.isSuccess()) {
                    String formattedResult = String.format(
                            "✅ Ping Successful!\n" +
                                    "Avg: %dms | Min: %dms | Max: %dms\n" +
                                    "Packet Loss: %s | Sent: %d | Received: %d",
                            result.getAvgTime(),
                            result.getMinTime(),
                            result.getMaxTime(),
                            result.getFormattedPacketLoss(),
                            result.getSent(),
                            result.getReceived()
                    );

                    pingResult.setText(formattedResult);
                    pingResult.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));

                    // Show success toast
                    Toast.makeText(this,
                            "Ping to " + device.ipAddress + ": " + result.getAvgTime() + "ms avg",
                            Toast.LENGTH_SHORT).show();

                } else {
                    pingResult.setText("❌ Ping Failed\n" +
                            (result.getErrorMessage() != null ? result.getErrorMessage() : "No response from device"));
                    pingResult.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
                }
            }
        });
    }

    @Override
    public void onPingError(String error) {
        runOnUiThread(() -> {
            isPinging = false;

            if (pingProgress != null) {
                pingProgress.setVisibility(View.GONE);
            }
            if (progressText != null) {
                progressText.setVisibility(View.GONE);
            }
            if (pingResult != null) {
                pingResult.setText("⚠️ Error: " + error);
                pingResult.setTextColor(getResources().getColor(android.R.color.holo_orange_dark, getTheme()));
            }
        });
    }

    // Helper methods for vendor and device type detection
    private String getVendorFromMac(String mac) {
        if (mac == null || mac.equals("Unknown") || mac.length() < 8) {
            return "Unknown Vendor";
        }

        String oui = mac.replace(":", "").replace("-", "").substring(0, 6).toUpperCase();

        // Common vendor OUIs - you can expand this list
        if (oui.startsWith("001C10") || oui.startsWith("A4D1D1") || oui.startsWith("F0F0F0")) {
            return "Apple";
        } else if (oui.startsWith("0019B9") || oui.startsWith("5C3C27")) {
            return "Samsung";
        } else if (oui.startsWith("001A11") || oui.startsWith("DC537C")) {
            return "Google";
        } else if (oui.startsWith("001D0F") || oui.startsWith("000D3A")) {
            return "Microsoft";
        } else if (oui.startsWith("C4E984") || oui.startsWith("001DE1")) {
            return "TP-Link";
        } else if (oui.startsWith("E45F01") || oui.startsWith("001E46")) {
            return "NETGEAR";
        } else if (oui.startsWith("0016CB") || oui.startsWith("00000C")) {
            return "Cisco";
        } else if (oui.startsWith("001F5B") || oui.startsWith("001D60")) {
            return "ASUS";
        } else if (oui.startsWith("0022B0") || oui.startsWith("001DE8")) {
            return "Dell";
        } else if (oui.startsWith("001A4B") || oui.startsWith("001217")) {
            return "Linksys";
        } else if (oui.startsWith("001124") || oui.startsWith("64167F")) {
            return "Huawei";
        }

        return "Unknown Vendor";
    }

    private String guessDeviceType(String vendor, String hostname, String ip) {
        if (vendor != null) {
            if (vendor.contains("Apple")) {
                if (hostname != null && (hostname.contains("iPhone") || hostname.contains("iPad"))) {
                    return "Mobile Device";
                }
                return "Apple Device";
            } else if (vendor.contains("Samsung")) {
                return "Samsung Device";
            } else if (vendor.contains("TP-Link") || vendor.contains("NETGEAR") ||
                    vendor.contains("Cisco") || vendor.contains("ASUS") ||
                    vendor.contains("Linksys")) {
                return "Network Router";
            } else if (vendor.contains("Dell") || vendor.contains("Microsoft")) {
                return "Computer";
            } else if (vendor.contains("Google")) {
                return "Google Device";
            }
        }

        // Guess based on IP address (often routers use .1 or .254)
        if (ip != null) {
            if (ip.endsWith(".1") || ip.endsWith(".254")) {
                return "Router/Gateway";
            }
        }

        // Guess based on hostname
        if (hostname != null) {
            hostname = hostname.toLowerCase();
            if (hostname.contains("android") || hostname.contains("phone") || hostname.contains("mobile")) {
                return "Mobile Device";
            } else if (hostname.contains("pc") || hostname.contains("laptop") || hostname.contains("desktop")) {
                return "Computer";
            } else if (hostname.contains("tv") || hostname.contains("smart")) {
                return "Smart TV";
            } else if (hostname.contains("printer")) {
                return "Printer";
            }
        }

        return "Unknown Device";
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}