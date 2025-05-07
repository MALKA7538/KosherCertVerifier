package com.example.koshercertverifier;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.koshercertverifier.api.ApiClient;
import com.example.koshercertverifier.api.CertificationRequest;
import com.example.koshercertverifier.api.CertificationResponse;
import com.example.koshercertverifier.util.DeviceInfoManager;
import com.example.koshercertverifier.util.PreferenceManager;

import java.io.IOException;

import retrofit2.Response;

public class CertificationCheckWorker extends Worker {
    private final Context context;

    public CertificationCheckWorker(
            @NonNull Context context,
            @NonNull WorkerParameters workerParams
    ) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        DeviceInfoManager deviceInfoManager = new DeviceInfoManager(context);
        PreferenceManager preferenceManager = new PreferenceManager(context);

        try {
            String deviceId = deviceInfoManager.getDeviceId();
            String deviceFingerprint = deviceInfoManager.getDeviceFingerprint();

            CertificationRequest request = new CertificationRequest(
                    deviceId,
                    deviceFingerprint,
                    System.currentTimeMillis()
            );

            Response<CertificationResponse> response = ApiClient
                    .getCertificationService()
                    .checkCertification(request)
                    .execute();

            if (response.isSuccessful()) {
                CertificationResponse certResponse = response.body();
                if (certResponse != null && certResponse.isCertified()) {
                    preferenceManager.setCertified(true);
                    preferenceManager.setLastCertifiedTime(System.currentTimeMillis());
                    Log.d("CertificationWorker", "Device certified successfully");
                } else {
                    preferenceManager.setCertified(false);
                    Log.d("CertificationWorker", "Device not certified");
                }
                return Result.success();
            } else {
                preferenceManager.setCertified(false);
                Log.e("CertificationWorker", "Error: " + response.code());
                return Result.retry();
            }
        } catch (IOException e) {
            Log.e("CertificationWorker", "Network error", e);
            return Result.retry();
        } catch (Exception e) {
            Log.e("CertificationWorker", "Unexpected error", e);
            return Result.failure();
        }
    }
}
