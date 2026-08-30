package com.example.model

enum class UpdateStatus(val label: String) {
    PENDING_APPROVAL("Pending Approval"),
    APPROVED("Approved"),
    IN_REVIEW("In Review"),
    FLAGGED("Flagged")
}

data class Project(
    val id: String,
    val name: String,
    val code: String,
    val location: String,
    val client: String,
    val activeWorkersCount: Int = 42
)

data class Worker(
    val workerId: String,
    val name: String,
    val role: String,
    val department: String,
    val phoneNumber: String,
    val assignedProjectId: String,
    val shift: String = "Day Shift (08:00 - 17:00)",
    val digitalId: String = ""
)

data class VoiceUpdate(
    val id: String,
    val projectId: String,
    val projectName: String,
    val workerId: String,
    val workerName: String,
    val timestamp: Long,
    val formattedDateTime: String,
    val durationSeconds: Int,
    val transcript: String,
    val status: UpdateStatus = UpdateStatus.PENDING_APPROVAL,
    val category: String = "Progress Update",
    val supervisorRemarks: String? = null,
    val audioWaveform: List<Float> = emptyList(),
    val audioFilePath: String? = null,
    val photoFilePath: String? = null
)

data class WorkerStats(
    val totalUpdates: Int,
    val approvedUpdates: Int,
    val pendingUpdates: Int,
    val hoursLogged: Double
)
