package com.example.data.network

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@JsonClass(generateAdapter = true)
data class VerifyLocationRequest(
    val latitude: Double?,
    val longitude: Double?
)

@JsonClass(generateAdapter = true)
data class VerifyLocationResponse(
    val allowed: Boolean,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val success: Boolean,
    val token: String,
    val user: NetworkUser
)

@JsonClass(generateAdapter = true)
data class NetworkUser(
    val username: String,
    val name: String,
    val role: String,
    val responsibility: String,
    val assignedProjects: List<String>,
    val permissions: List<String>,
    val phone: String = "",
    val digitalId: String = "",
    val projectDetails: ProjectDetailsDto? = null
)

@JsonClass(generateAdapter = true)
data class ProjectDetailsDto(
    val id: String,
    val name: String,
    val location: String
)

@JsonClass(generateAdapter = true)
data class ProjectResponse(
    val id: String,
    val name: String,
    val code: String,
    val location: String,
    val client: String,
    val activeWorkersCount: Int
)

@JsonClass(generateAdapter = true)
data class VoiceUploadResponse(
    val success: Boolean,
    val update: NetworkVoiceUpdate
)

@JsonClass(generateAdapter = true)
data class NetworkVoiceUpdate(
    val id: String,
    val projectId: String,
    val projectName: String,
    val workerId: String,
    val workerName: String,
    val timestamp: Long,
    val formattedDateTime: String,
    val durationSeconds: Int,
    val transcript: String,
    val status: String,
    val category: String,
    val waveform: List<Float> = emptyList(),
    val audioFilePath: String? = null,
    val photoFilePath: String? = null
)
