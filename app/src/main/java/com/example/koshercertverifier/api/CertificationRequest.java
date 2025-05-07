package com.example.koshercertverifier.api;

public class CertificationRequest {
    private String deviceId;
    private String deviceFingerprint;
    private long timestamp;

    public CertificationRequest(String deviceId, String deviceFingerprint, long timestamp) {
        this.deviceId = deviceId;
        this.deviceFingerprint = deviceFingerprint;
        this.timestamp = timestamp;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
