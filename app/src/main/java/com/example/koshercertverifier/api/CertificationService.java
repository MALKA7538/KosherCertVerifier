package com.example.koshercertverifier.api;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface CertificationService {
    @POST("check-certification")
    Call<CertificationResponse> checkCertification(@Body CertificationRequest request);
}
