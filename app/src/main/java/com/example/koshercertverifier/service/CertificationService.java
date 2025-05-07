package com.example.koshercertverifier.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.koshercertverifier.MainActivity;
import com.example.koshercertverifier.R;
import com.example.koshercertverifier.api.ApiClient;
import com.example.koshercertverifier.api.CertificationRequest;
import com.example.koshercertverifier.api.CertificationResponse;
import com.example.koshercertverifier.util.DeviceInfoManager;
import com.example.koshercertverifier.util.PreferenceManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CertificationService extends Service {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PreferenceManager preferenceManager;
    private DeviceInfoManager deviceInfoManager;

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "KosherCertificationChannel";
    private static final long CHECK_INTERVAL = 5L; // minutes

    @Override
    public void onCreate() {
        super.onCreate();
        preferenceManager = new PreferenceManager(this);
        deviceInfoManager = new DeviceInfoManager(this);
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startCertificationCheck();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        scheduler.shutdownNow();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String name = "Kosher Certification Service";
            String descriptionText = "Ensures this device maintains kosher certification";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(descriptionText);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Kosher Certification Active")
                .setContentText("Verifying device certification")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void startCertificationCheck() {
        scheduler.scheduleAtFixedRate(
                this::checkCertification,
                0,
                CHECK_INTERVAL,
                TimeUnit.MINUTES
        );
    }

    private void checkCertification() {
        String deviceId = deviceInfoManager.getDeviceId();
        String deviceFingerprint = deviceInfoManager.getDeviceFingerprint();

        CertificationRequest request = new CertificationRequest(
                deviceId,
                deviceFingerprint,
                System.currentTimeMillis()
        );

        ApiClient.getCertificationService().checkCertification(request).enqueue(new Callback<CertificationResponse>() {
            @Override
            public void onResponse(Call<CertificationResponse> call, Response<CertificationResponse> response) {
                if (response.isSuccessful()) {
                    CertificationResponse certResponse = response.body();
                    if (certResponse != null && certResponse.isCertified()) {
                        mainHandler.post(() -> {
                            preferenceManager.setCertified(true);
                            preferenceManager.setLastCertifiedTime(System.currentTimeMillis());
                        });
                        Log.d("CertificationService", "Device certified successfully");
                    } else {
                        mainHandler.post(() -> preferenceManager.setCertified(false));
                        Log.d("CertificationService", "Device not certified");
                    }
                } else {
                    mainHandler.post(() -> preferenceManager.setCertified(false));
                    Log.e("CertificationService", "Error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<CertificationResponse> call, Throwable t) {
                Log.e("CertificationService", "Network error", t);
            }
        });
    }
}

// File: app/src/main/java/com/example/koshercertverifier/CertificationCheckWorker.java
