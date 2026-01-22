package com.example.netanalyzer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private RecyclerView devicesRecyclerView;
    private DeviceAdapter deviceAdapter;
    private TextView statusTextView, deviceCountTextView;
    private MaterialButton scanButton;
    private ProgressBar progressBar;
    private BottomNavigationView bottomNavigationView;
    private FrameLayout fragmentContainer;
    private View networkScannerContainer;

    // Data
    private List<Device> deviceList = new ArrayList<>();

    // Network
    private WifiManager wifiManager;
    private String gateway;

    // Permissions
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = "NetAnalyzer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize all views
        initializeViews();
        setupRecyclerView();
        setupWiFiManager();
        checkAndRequestPermissions();
        setupBottomNavigation();
    }

    private void initializeViews() {
        // Find all views
        devicesRecyclerView = findViewById(R.id.devicesRecyclerView);
        statusTextView = findViewById(R.id.statusTextView);
        deviceCountTextView = findViewById(R.id.deviceCountTextView);
        scanButton = findViewById(R.id.scanButton);
        progressBar = findViewById(R.id.progressBar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentContainer = findViewById(R.id.fragment_container);
        networkScannerContainer = findViewById(R.id.network_scanner_container);

        // Setup scan button
        scanButton.setOnClickListener(v -> {
            if (checkAllPermissions()) {
                startNetworkScan();
            } else {
                requestNeededPermissions();
            }
        });

        // Setup device adapter
        deviceAdapter = new DeviceAdapter(this, deviceList);
        devicesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        devicesRecyclerView.setAdapter(deviceAdapter);
    }

    private void setupRecyclerView() {
        // Already done in initializeViews()
    }

    private void setupWiFiManager() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            showToast("WiFi service not available");
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_network) {
                // Show Network Scanner
                showNetworkScanner();
                return true;
            } else if (itemId == R.id.nav_dns) {
                // Show DNS Test Fragment
                showDnsTestFragment();
                return true;
            } else if (itemId == R.id.nav_ping) {
                // Open PingTestActivity
                Intent intent = new Intent(MainActivity.this, PingTestActivity.class);
                startActivity(intent);
                return false; // Don't change selection
            }

            return false;
        });
    }

    private void showNetworkScanner() {
        // Hide any fragment
        hideAllFragments();

        // Show network scanner container
        networkScannerContainer.setVisibility(View.VISIBLE);
    }

    private void showDnsTestFragment() {
        // Hide network scanner
        networkScannerContainer.setVisibility(View.GONE);

        // Create or show DNS Test Fragment
        Fragment dnsFragment = getSupportFragmentManager().findFragmentByTag("DNS_FRAGMENT");

        if (dnsFragment == null) {
            dnsFragment = new DnsTestFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.fragment_container, dnsFragment, "DNS_FRAGMENT");
            transaction.commit();
        } else {
            // Show existing fragment
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.show(dnsFragment);
            transaction.commit();
        }
    }

    private void hideAllFragments() {
        Fragment dnsFragment = getSupportFragmentManager().findFragmentByTag("DNS_FRAGMENT");
        if (dnsFragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.hide(dnsFragment);
            transaction.commit();
        }
    }

    private boolean checkAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void checkAndRequestPermissions() {
        if (checkAllPermissions()) {
            updateStatus("Ready to scan");
            scanButton.setEnabled(true);
        } else {
            requestNeededPermissions();
        }
    }

    private void requestNeededPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                updateStatus("Permissions granted");
                scanButton.setEnabled(true);
            } else {
                updateStatus("Permissions required");
            }
        }
    }

    private void startNetworkScan() {
        if (!wifiManager.isWifiEnabled()) {
            showToast("Please enable WiFi first");
            return;
        }
        new NetworkScannerTask().execute();
    }

    private class NetworkScannerTask extends AsyncTask<Void, String, List<Device>> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
            scanButton.setEnabled(false);
            deviceList.clear();
            deviceAdapter.notifyDataSetChanged();
            updateStatus("Starting network scan...");
        }

        @Override
        protected List<Device> doInBackground(Void... voids) {
            List<Device> discoveredDevices = new ArrayList<>();

            try {
                // Get network information
                DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                gateway = intToIp(dhcpInfo.gateway);
                String myIp = intToIp(wifiManager.getConnectionInfo().getIpAddress());

                publishProgress("My IP: " + myIp);
                publishProgress("Gateway: " + gateway);

                // Calculate subnet
                String subnet = getSubnet(myIp);
                publishProgress("Scanning subnet: " + subnet + "0/24");

                // Scan for active devices
                List<String> activeIPs = scanSubnet(subnet);
                publishProgress("Found " + activeIPs.size() + " active IPs");

                // Get MAC addresses from ARP (will fail on non-rooted)
                Map<String, String> arpTable = readARPTable();

                // Create device objects
                for (String ip : activeIPs) {
                    Device device = new Device();
                    device.ipAddress = ip;

                    // Get MAC from ARP or set as Unknown
                    String mac = arpTable.get(ip);
                    if (mac == null || mac.equals("00:00:00:00:00:00")) {
                        device.macAddress = "Unknown";
                        device.vendor = "Unknown Vendor";
                    } else {
                        device.macAddress = mac;
                        // Use OUI database for vendor detection
                        device.vendor = OuiDatabaseHelper.getVendorFromMac(MainActivity.this, mac);
                    }

                    // Try to get hostname
                    try {
                        String hostname = InetAddress.getByName(ip).getHostName();
                        if (!hostname.equals(ip)) {
                            device.hostname = hostname;
                        }
                    } catch (Exception e) {
                        // Keep default
                    }

                    // Guess device type with IP for router detection
                    device.deviceType = OuiDatabaseHelper.guessDeviceType(device.vendor, device.hostname, ip);

                    // Check if this is the gateway/router
                    if (ip.equals(gateway)) {
                        device.deviceType = "Router/Gateway";
                        device.vendor = "Router Device";
                    }

                    discoveredDevices.add(device);
                    publishProgress("Found: " + ip);
                }

            } catch (Exception e) {
                publishProgress("Error: " + e.getMessage());
                Log.e(TAG, "Scan error: " + e.getMessage());
            }

            return discoveredDevices;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            updateStatus(values[0]);
        }

        @Override
        protected void onPostExecute(List<Device> devices) {
            progressBar.setVisibility(View.GONE);
            scanButton.setEnabled(true);

            deviceList.clear();
            deviceList.addAll(devices);
            deviceAdapter.notifyDataSetChanged();
            updateDeviceCount(devices.size());

            if (devices.isEmpty()) {
                updateStatus("No devices found");
            } else {
                updateStatus("Scan complete. Found " + devices.size() + " devices");
            }
        }

        private String getSubnet(String ip) {
            if (ip == null) return "192.168.1.";
            String[] parts = ip.split("\\.");
            if (parts.length >= 3) {
                return parts[0] + "." + parts[1] + "." + parts[2] + ".";
            }
            return "192.168.1.";
        }

        private List<String> scanSubnet(String subnet) {
            List<String> activeIPs = new ArrayList<>();
            ExecutorService executor = Executors.newFixedThreadPool(20);

            for (int i = 1; i < 255; i++) {
                final String ip = subnet + i;
                executor.execute(() -> {
                    try {
                        if (InetAddress.getByName(ip).isReachable(300)) {
                            synchronized (activeIPs) {
                                activeIPs.add(ip);
                            }
                        }
                    } catch (Exception e) {
                        // Ignore - host not reachable
                    }
                });
            }

            executor.shutdown();
            try {
                executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return activeIPs;
        }

        private Map<String, String> readARPTable() {
            Map<String, String> arpTable = new HashMap<>();
            try (BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"))) {
                String line;
                br.readLine(); // Skip header
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4 && !parts[3].equals("00:00:00:00:00:00")) {
                        arpTable.put(parts[0], parts[3].toUpperCase());
                    }
                }
            } catch (Exception e) {
                // ARP reading fails on non-rooted devices - that's normal
                Log.d(TAG, "ARP table read failed (normal for non-rooted): " + e.getMessage());
            }
            return arpTable;
        }

        private String intToIp(int addr) {
            return ((addr & 0xFF) + "." +
                    ((addr >> 8) & 0xFF) + "." +
                    ((addr >> 16) & 0xFF) + "." +
                    ((addr >> 24) & 0xFF));
        }
    }

    private void updateStatus(String message) {
        statusTextView.setText(message);
    }

    private void updateDeviceCount(int count) {
        deviceCountTextView.setText(count + " devices");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Get gateway for DeviceDetailsActivity
    public String getGateway() {
        return gateway;
    }

    // Device model class
    static class Device implements Serializable {
        String ipAddress;
        String macAddress = "Unknown";
        String hostname;
        String vendor = "Unknown Vendor";
        String deviceType = "Unknown";
        Date firstSeen;
        Date lastSeen;
        boolean isConnected = true;
        int signalStrength = 0;

        // Constructor
        public Device() {
            Date now = new Date();
            this.firstSeen = now;
            this.lastSeen = now;
        }

        String getDisplayName() {
            if (hostname != null && !hostname.isEmpty() && !hostname.equals(ipAddress)) {
                return hostname;
            }
            if (ipAddress != null) {
                return "Device " + ipAddress.substring(ipAddress.lastIndexOf('.') + 1);
            }
            return "Unknown Device";
        }

        String getFormattedFirstSeen() {
            if (firstSeen == null) return "Just now";
            return formatDate(firstSeen);
        }

        String getFormattedLastSeen() {
            if (lastSeen == null) return "Just now";
            return formatDate(lastSeen);
        }

        private String formatDate(Date date) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                return sdf.format(date);
            } catch (Exception e) {
                return "Unknown";
            }
        }
    }
}