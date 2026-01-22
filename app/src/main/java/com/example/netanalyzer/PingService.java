package com.example.netanalyzer;

import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import com.example.netanalyzer.PingResult;
import com.example.netanalyzer.PingService;
import com.example.netanalyzer.PingHistoryAdapter;
public class PingService {

    private static final String TAG = "PingService";

    public interface PingCallback {
        void onPingComplete(PingResult result);
        void onPingProgress(String message);
        void onPingError(String error);
    }

    public static void pingHost(String host, int count, PingCallback callback) {
        new PingTask(host, count, callback).execute();
    }

    private static class PingTask extends AsyncTask<Void, String, PingResult> {
        private final String host;
        private final int count;
        private final PingCallback callback;

        PingTask(String host, int count, PingCallback callback) {
            this.host = host;
            this.count = count;
            this.callback = callback;
        }

        @Override
        protected void onPreExecute() {
            callback.onPingProgress("Resolving host...");
        }

        @Override
        protected PingResult doInBackground(Void... voids) {
            PingResult result = new PingResult(host);
            List<Long> times = new ArrayList<>();

            try {
                // Resolve hostname to IP
                publishProgress("Resolving " + host + "...");
                InetAddress address = InetAddress.getByName(host);
                String ip = address.getHostAddress();
                result.setIpAddress(ip);
                publishProgress("Pinging " + ip + " (" + host + ")...");

                // Perform ping tests
                result.setSent(count);
                int received = 0;

                for (int i = 1; i <= count; i++) {
                    try {
                        long startTime = System.currentTimeMillis();
                        boolean reachable = address.isReachable(2000); // 2 second timeout
                        long endTime = System.currentTimeMillis();

                        if (reachable) {
                            long timeTaken = endTime - startTime;
                            times.add(timeTaken);
                            received++;
                            publishProgress("Reply from " + ip + ": time=" + timeTaken + "ms");
                        } else {
                            publishProgress("Request timed out");
                        }

                        // Small delay between pings
                        Thread.sleep(500);

                    } catch (Exception e) {
                        publishProgress("Ping error: " + e.getMessage());
                    }
                }

                // Calculate statistics
                result.setReceived(received);
                result.setLost(count - received);

                if (received > 0) {
                    // Calculate min, max, avg
                    long min = Long.MAX_VALUE;
                    long max = Long.MIN_VALUE;
                    long total = 0;

                    for (Long time : times) {
                        if (time < min) min = time;
                        if (time > max) max = time;
                        total += time;
                    }

                    result.setMinTime(min);
                    result.setMaxTime(max);
                    result.setAvgTime(total / received);
                    result.setSuccess(true);

                } else {
                    result.setErrorMessage("No response from host");
                    result.setSuccess(false);
                }

            } catch (Exception e) {
                Log.e(TAG, "Ping error: " + e.getMessage(), e);
                result.setSuccess(false);
                result.setErrorMessage("Error: " + e.getMessage());
            }

            return result;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            callback.onPingProgress(values[0]);
        }

        @Override
        protected void onPostExecute(PingResult result) {
            callback.onPingComplete(result);
        }
    }

    // Quick ping (single test) for device details
    public static void quickPing(String host, PingCallback callback) {
        pingHost(host, 4, callback); // 4 pings for quick test
    }

    // Multi-ping for detailed analysis
    public static void detailedPing(String host, PingCallback callback) {
        pingHost(host, 10, callback); // 10 pings for detailed analysis
    }
}