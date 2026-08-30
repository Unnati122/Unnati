package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Project
import com.example.model.UpdateStatus
import com.example.model.VoiceUpdate
import com.example.model.Worker

@Entity(tableName = "voice_updates")
data class VoiceUpdateEntity(
    @PrimaryKey
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
    val supervisorRemarks: String?,
    val waveformCsv: String,
    val audioFilePath: String? = null,
    val photoFilePath: String? = null
) {
    fun toDomainModel(): VoiceUpdate {
        val parsedWaveform = if (waveformCsv.isNotBlank()) {
            waveformCsv.split(",").mapNotNull { it.toFloatOrNull() }
        } else {
            emptyList()
        }
        val parsedStatus = try {
            UpdateStatus.valueOf(status)
        } catch (e: Exception) {
            UpdateStatus.PENDING_APPROVAL
        }
        return VoiceUpdate(
            id = id,
            projectId = projectId,
            projectName = projectName,
            workerId = workerId,
            workerName = workerName,
            timestamp = timestamp,
            formattedDateTime = formattedDateTime,
            durationSeconds = durationSeconds,
            transcript = transcript,
            status = parsedStatus,
            category = category,
            supervisorRemarks = supervisorRemarks,
            audioWaveform = parsedWaveform,
            audioFilePath = audioFilePath,
            photoFilePath = photoFilePath
        )
    }

    companion object {
        fun fromDomainModel(update: VoiceUpdate): VoiceUpdateEntity {
            return VoiceUpdateEntity(
                id = update.id,
                projectId = update.projectId,
                projectName = update.projectName,
                workerId = update.workerId,
                workerName = update.workerName,
                timestamp = update.timestamp,
                formattedDateTime = update.formattedDateTime,
                durationSeconds = update.durationSeconds,
                transcript = update.transcript,
                status = update.status.name,
                category = update.category,
                supervisorRemarks = update.supervisorRemarks,
                waveformCsv = update.audioWaveform.joinToString(","),
                audioFilePath = update.audioFilePath,
                photoFilePath = update.photoFilePath
            )
        }
    }
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val code: String,
    val location: String,
    val client: String,
    val activeWorkersCount: Int
) {
    fun toDomainModel(): Project = Project(
        id = id,
        name = name,
        code = code,
        location = location,
        client = client,
        activeWorkersCount = activeWorkersCount
    )

    companion object {
        fun fromDomainModel(project: Project): ProjectEntity = ProjectEntity(
            id = project.id,
            name = project.name,
            code = project.code,
            location = project.location,
            client = project.client,
            activeWorkersCount = project.activeWorkersCount
        )
    }
}

@Entity(tableName = "workers")
data class WorkerEntity(
    @PrimaryKey
    val workerId: String,
    val name: String,
    val role: String,
    val department: String,
    val phoneNumber: String,
    val digitalId: String,
    val assignedProjectId: String,
    val shift: String
) {
    fun toDomainModel(): Worker = Worker(
        workerId = workerId,
        name = name,
        role = role,
        department = department,
        phoneNumber = phoneNumber,
        digitalId = digitalId,
        assignedProjectId = assignedProjectId,
        shift = shift
    )

    companion object {
        fun fromDomainModel(worker: Worker): WorkerEntity = WorkerEntity(
            workerId = worker.workerId,
            name = worker.name,
            role = worker.role,
            department = worker.department,
            phoneNumber = worker.phoneNumber,
            digitalId = worker.digitalId,
            assignedProjectId = worker.assignedProjectId,
            shift = worker.shift
        )
    }
}
