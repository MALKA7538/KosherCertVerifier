package com.example.koshercertverifier.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class DeviceInfoManager {
    private final Context context;

    public DeviceInfoManager(Context context) {
        this.context = context;
    }

    /**
     * Gets a unique device identifier using the best available method
     * First tries to get IMEI, falls back to Android ID
     */
    @SuppressLint("HardwareIds")
    public String getDeviceId() {
        // Try to get IMEI first (requires permission)
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String imei = telephonyManager.getImei();
                if (imei != null && !imei.isEmpty()) {
                    return imei;
                }
            } else {
                @SuppressWarnings("deprecation")
                String deviceId = telephonyManager.getDeviceId();
                if (deviceId != null && !deviceId.isEmpty()) {
                    return deviceId;
                }
            }
        } catch (SecurityException e) {
            // Permission not granted, fall back to Android ID
        }

        // Fallback to Android ID (less secure but doesn't require special permission)
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        if (androidId != null && !androidId.equals("9774d56d682e549c")) {
            // "9774d56d682e549c" is a known ANDROID_ID value for rooted devices
            return androidId;
        }

        // Last resort: generate a UUID and store it
        SharedPreferences prefs = context.getSharedPreferences("device_id_prefs", Context.MODE_PRIVATE);
        String uuid = prefs.getString("device_uuid", null);

        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            prefs.edit().putString("device_uuid", uuid).apply();
        }

        return uuid;
    }

    /**
     * Creates a device fingerprint using multiple hardware and software parameters
     * that together make a unique signature for the device
     */
    public String getDeviceFingerprint() {
        StringBuilder fingerprintData = new StringBuilder();

        // Add hardware info
        fingerprintData.append(Build.BOARD);
        fingerprintData.append(Build.BOOTLOADER);
        fingerprintData.append(Build.BRAND);
        fingerprintData.append(Build.DEVICE);
        fingerprintData.append(Build.DISPLAY);
        fingerprintData.append(Build.HARDWARE);
        fingerprintData.append(Build.MANUFACTURER);
        fingerprintData.append(Build.MODEL);
        fingerprintData.append(Build.PRODUCT);
        fingerprintData.append(Build.SERIAL);

        // Add Android ID
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        fingerprintData.append(androidId);

        // Create a hash of the fingerprint data
        return hashString(fingerprintData.toString());
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString(); // Fallback if hashing fails
        }
    }
}
