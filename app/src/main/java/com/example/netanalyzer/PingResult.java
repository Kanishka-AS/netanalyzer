package com.example.netanalyzer;

import java.io.Serializable;
import java.util.Date;

public class PingResult implements Serializable {
    private String target;
    private String ipAddress;
    private int sent;
    private int received;
    private int lost;
    private long minTime;
    private long avgTime;
    private long maxTime;
    private Date timestamp;
    private boolean success;
    private String errorMessage;

    public PingResult(String target) {
        this.target = target;
        this.timestamp = new Date();
        this.sent = 0;
        this.received = 0;
        this.lost = 0;
        this.success = false;
    }

    // Getters and Setters
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public int getSent() { return sent; }
    public void setSent(int sent) { this.sent = sent; }

    public int getReceived() { return received; }
    public void setReceived(int received) { this.received = received; }

    public int getLost() { return lost; }
    public void setLost(int lost) { this.lost = lost; }

    public long getMinTime() { return minTime; }
    public void setMinTime(long minTime) { this.minTime = minTime; }

    public long getAvgTime() { return avgTime; }
    public void setAvgTime(long avgTime) { this.avgTime = avgTime; }

    public long getMaxTime() { return maxTime; }
    public void setMaxTime(long maxTime) { this.maxTime = maxTime; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    // Helper methods
    public double getPacketLoss() {
        if (sent == 0) return 0;
        return ((double) lost / sent) * 100;
    }

    public String getFormattedPacketLoss() {
        return String.format("%.1f%%", getPacketLoss());
    }

    public String getFormattedResult() {
        if (!success) {
            return errorMessage != null ? errorMessage : "Ping failed";
        }
        return String.format("Avg: %dms | Min: %dms | Max: %dms | Loss: %s",
                avgTime, minTime, maxTime, getFormattedPacketLoss());
    }

    public String getShortResult() {
        if (!success) return "Failed";
        return avgTime + "ms";
    }
}