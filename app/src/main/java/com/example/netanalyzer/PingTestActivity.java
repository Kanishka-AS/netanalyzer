package com.example.netanalyzer;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;

public class PingTestActivity extends AppCompatActivity {

    private EditText hostInput;
    private Button pingButton, quickTestButton;
    private TextView resultText, progressText;
    private ProgressBar progressBar;
    private RecyclerView historyRecyclerView;
    private PingHistoryAdapter historyAdapter;
    private List<PingResult> pingHistory = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping_test);

        setupToolbar();
        initializeViews();
        setupHistoryRecyclerView();

        // Check if we got a device IP from intent
        String deviceIp = getIntent().getStringExtra("device_ip");
        if (deviceIp != null) {
            hostInput.setText(deviceIp);
            // Auto-ping the device
            startPingTest(deviceIp, 4);
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ping Test");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initializeViews() {
        hostInput = findViewById(R.id.hostInput);
        pingButton = findViewById(R.id.pingButton);
        quickTestButton = findViewById(R.id.quickTestButton);
        resultText = findViewById(R.id.resultText);
        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.progressBar);

        // Pre-populate with common test targets
        setupQuickTestButtons();

        pingButton.setOnClickListener(v -> {
            String host = hostInput.getText().toString().trim();
            if (host.isEmpty()) {
                Toast.makeText(this, "Enter a host or IP address", Toast.LENGTH_SHORT).show();
                return;
            }
            startPingTest(host, 10); // 10 pings for detailed test
        });

        quickTestButton.setOnClickListener(v -> {
            String host = hostInput.getText().toString().trim();
            if (host.isEmpty()) {
                Toast.makeText(this, "Enter a host or IP address", Toast.LENGTH_SHORT).show();
                return;
            }
            startPingTest(host, 4); // 4 pings for quick test
        });
    }

    private void setupQuickTestButtons() {
        // Popular websites for testing
        String[] testServers = {"Google", "YouTube", "Facebook", "Instagram", "WhatsApp", "Router"};
        String[] testHosts = {"google.com", "youtube.com", "facebook.com", "instagram.com", "whatsapp.com", "192.168.1.1"};

        for (int i = 0; i < testServers.length; i++) {
            int buttonId = getResources().getIdentifier("testButton" + (i + 1), "id", getPackageName());
            Button button = findViewById(buttonId);
            if (button != null) {
                final String host = testHosts[i];
                final String name = testServers[i];
                button.setText(name);
                button.setOnClickListener(v -> {
                    hostInput.setText(host);
                    startPingTest(host, 5); // 5 pings for quick website test

                    // Show which website is being tested
                    Toast.makeText(PingTestActivity.this,
                            "Testing " + name + " (" + host + ")",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    private void setupHistoryRecyclerView() {
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        historyAdapter = new PingHistoryAdapter(pingHistory);
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyRecyclerView.setAdapter(historyAdapter);
    }

    private void startPingTest(String host, int count) {
        // Reset UI
        progressBar.setVisibility(View.VISIBLE);
        progressText.setVisibility(View.VISIBLE);
        resultText.setText("Testing...");
        pingButton.setEnabled(false);
        quickTestButton.setEnabled(false);

        PingService.pingHost(host, count, new PingService.PingCallback() {
            @Override
            public void onPingProgress(String message) {
                runOnUiThread(() -> progressText.setText(message));
            }

            @Override
            public void onPingComplete(PingResult result) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                    pingButton.setEnabled(true);
                    quickTestButton.setEnabled(true);

                    if (result.isSuccess()) {
                        resultText.setText(result.getFormattedResult());
                        resultText.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));

                        // Add to history
                        pingHistory.add(0, result); // Add to beginning
                        historyAdapter.notifyDataSetChanged();

                        // Show success message
                        Toast.makeText(PingTestActivity.this,
                                "Ping successful! Avg: " + result.getAvgTime() + "ms",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        resultText.setText("Ping failed: " + result.getErrorMessage());
                        resultText.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
                    }
                });
            }

            @Override
            public void onPingError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    progressText.setVisibility(View.GONE);
                    pingButton.setEnabled(true);
                    quickTestButton.setEnabled(true);
                    resultText.setText("Error: " + error);
                    resultText.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
                });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}