package com.example.koshercertverifier;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.koshercertverifier.api.CertificationService;
import com.example.koshercertverifier.util.DeviceInfoManager;
import com.example.koshercertverifier.util.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private ConstraintLayout containerLayout;
    private TextView statusTextView;
    private TextView timestampTextView;
    private TextView deviceIdTextView;
    private PreferenceManager preferenceManager;

    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        containerLayout = findViewById(R.id.container_layout);
        statusTextView = findViewById(R.id.status_text);
        timestampTextView = findViewById(R.id.timestamp_text);
        deviceIdTextView = findViewById(R.id.device_id_text);

        // Initialize preference manager
        preferenceManager = new PreferenceManager(this);

        // Check and request permissions
        checkAndRequestPermissions();

        // Start the certification service
        startCertificationService();

        // Schedule periodic checks
        schedulePeriodicChecks();

        // Update UI based on current status
        updateUI();
    }

    private void checkAndRequestPermissions() {
        String[] permissions = {Manifest.permission.READ_PHONE_STATE};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean needsPermission = false;

            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    needsPermission = true;
                    break;
                }
            }

            if (needsPermission) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
            } else {
                displayDeviceInfo();
            }
        } else {
            displayDeviceInfo();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                displayDeviceInfo();
            }
        }
    }

    private void displayDeviceInfo() {
        DeviceInfoManager deviceInfo = new DeviceInfoManager(this);
        String deviceId = deviceInfo.getDeviceId();
        deviceIdTextView.setText("Device ID: " + deviceId);
    }

    private void startCertificationService() {
        Intent serviceIntent = new Intent(this, CertificationService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void schedulePeriodicChecks() {
        PeriodicWorkRequest certificationCheckRequest =
                new PeriodicWorkRequest.Builder(CertificationCheckWorker.class, 15, TimeUnit.MINUTES)
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "certification_check",
                ExistingPeriodicWorkPolicy.KEEP,
                certificationCheckRequest
        );
    }

    private void updateUI() {
        boolean isCertified = preferenceManager.isCertified();
        long lastCertified = preferenceManager.getLastCertifiedTime();

        if (isCertified) {
            containerLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.green_certified));
            statusTextView.setText(getString(R.string.status_certified));
        } else {
            containerLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.red_uncertified));
            statusTextView.setText(getString(R.string.status_uncertified));
        }

        if (lastCertified > 0) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String formattedDate = dateFormat.format(new Date(lastCertified));
            timestampTextView.setText("Last Certified: " + formattedDate);
        } else {
            timestampTextView.setText("Not yet certified");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }
}
