package com.example.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/me")
    suspend fun getMe(): Response<NetworkUser>

    @GET("api/my-project")
    suspend fun getMyProject(): Response<ProjectResponse>

    @GET("api/my-updates")
    suspend fun getMyUpdates(): Response<List<NetworkVoiceUpdate>>

    @Multipart
    @POST("api/voice/upload")
    suspend fun uploadVoice(
        @Part audio: MultipartBody.Part,
        @Part("durationSeconds") durationSeconds: RequestBody,
        @Part("waveformCsv") waveformCsv: RequestBody,
        @Part("transcript") transcript: RequestBody
    ): Response<VoiceUploadResponse>
}
