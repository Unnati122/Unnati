package com.example.data.repository

import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.ProjectEntity
import com.example.data.local.VoiceUpdateEntity
import com.example.data.local.WorkerEntity
import com.example.data.network.RetrofitClient
import com.example.model.Project
import com.example.model.UpdateStatus
import com.example.model.VoiceUpdate
import com.example.model.Worker
import com.example.model.WorkerStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

class TimeAgentRepository(private val database: AppDatabase) {

    private val voiceUpdateDao = database.voiceUpdateDao()
    private val projectDao = database.projectDao()
    private val workerDao = database.workerDao()

    val allUpdates: Flow<List<VoiceUpdate>> = voiceUpdateDao.getAllUpdates()
        .map { entities -> entities.map { it.toDomainModel() } }

    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()
        .map { entities -> entities.map { it.toDomainModel() } }

    val allWorkers: Flow<List<Worker>> = workerDao.getAllWorkers()
        .map { entities -> entities.map { it.toDomainModel() } }

    val workerStats: Flow<WorkerStats> = allUpdates.map { updates ->
        val total = updates.size
        val approved = updates.count { it.status == UpdateStatus.APPROVED }
        val pending = updates.count { it.status == UpdateStatus.PENDING_APPROVAL }
        val totalHours = updates.sumOf { it.durationSeconds } / 3600.0 + (total * 5.8)
        WorkerStats(
            totalUpdates = total,
            approvedUpdates = approved,
            pendingUpdates = pending,
            hoursLogged = Math.round(totalHours * 10.0) / 10.0
        )
    }

    suspend fun syncMyUpdates() {
        try {
            val response = RetrofitClient.apiService.getMyUpdates()
            if (response.isSuccessful) {
                val networkUpdates = response.body() ?: emptyList()
                val entities = networkUpdates.map { net ->
                    val parsedStatus = when (net.status) {
                        "APPROVED", "Linked" -> UpdateStatus.APPROVED
                        "PENDING_APPROVAL", "Pending Review" -> UpdateStatus.PENDING_APPROVAL
                        "IN_REVIEW" -> UpdateStatus.IN_REVIEW
                        "FLAGGED" -> UpdateStatus.FLAGGED
                        else -> UpdateStatus.PENDING_APPROVAL
                    }
                    VoiceUpdateEntity(
                        id = net.id,
                        projectId = net.projectId,
                        projectName = net.projectName,
                        workerId = net.workerId,
                        workerName = net.workerName,
                        timestamp = net.timestamp,
                        formattedDateTime = net.formattedDateTime,
                        durationSeconds = net.durationSeconds,
                        transcript = net.transcript,
                        status = parsedStatus.name,
                        category = net.category,
                        supervisorRemarks = null,
                        waveformCsv = net.waveform.joinToString(","),
                        audioFilePath = net.audioFilePath,
                        photoFilePath = net.photoFilePath
                    )
                }
                voiceUpdateDao.insertAllUpdates(entities)
            }
        } catch (e: Exception) {
            Log.e("TimeAgentRepository", "Failed to sync updates from backend", e)
        }
    }

    suspend fun saveAuthenticatedWorker(
        username: String,
        name: String,
        role: String,
        responsibility: String,
        assignedProjects: List<String>,
        phone: String,
        digitalId: String,
        projectDetails: com.example.data.network.ProjectDetailsDto?
    ) {
        val resolvedProjId = if (assignedProjects.isNotEmpty()) assignedProjects[0] else "PROJ-1"
        
        // Ensure local project matches name from DB if possible
        val localProj = projectDao.getProjectById(resolvedProjId)
        if (localProj == null || (projectDetails != null && localProj.name != projectDetails.name)) {
            val projName = projectDetails?.name ?: "OIL Pipeline Expansion"
            val projLoc = projectDetails?.location ?: "Barmer-Salaya Corridor, Rajasthan"
            projectDao.insertProjects(
                listOf(
                    ProjectEntity(
                        id = resolvedProjId,
                        name = projName,
                        code = "PRJ-${resolvedProjId.takeLast(4)}",
                        location = projLoc,
                        client = "Oil India Ltd / Bharat Petro Infra",
                        activeWorkersCount = 164
                    )
                )
            )
        }

        val workerEntity = WorkerEntity(
            workerId = username,
            name = name,
            role = role,
            department = responsibility,
            phoneNumber = if (phone.isNotBlank()) phone else "+91 98201 45892",
            digitalId = digitalId,
            assignedProjectId = resolvedProjId,
            shift = "Morning Shift (07:00 - 15:30)"
        )
        workerDao.insertWorker(workerEntity)
    }

    suspend fun submitVoiceUpdate(
        project: Project,
        worker: Worker,
        durationSeconds: Int,
        transcript: String,
        waveform: List<Float>,
        audioFilePath: String? = null,
        photoFilePath: String? = null
    ): VoiceUpdate {
        // Prepare local/fallback model
        val now = System.currentTimeMillis()
        val randomDigits = String.format(Locale.US, "%06d", (100..999).random() * 100 + (1..99).random())
        val updateId = "UPD-2024-$randomDigits"
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
        val formattedDate = dateFormat.format(Date(now))

        val resolvedCategory = when {
            transcript.contains("safety", ignoreCase = true) || transcript.contains("talk", ignoreCase = true) -> "Safety Briefing"
            transcript.contains("rebar", ignoreCase = true) || transcript.contains("concrete", ignoreCase = true) -> "Foundation & Rebar"
            transcript.contains("excavat", ignoreCase = true) || transcript.contains("meter", ignoreCase = true) -> "Earthworks & Tunnel"
            transcript.contains("steel", ignoreCase = true) || transcript.contains("cement", ignoreCase = true) -> "Material Handling"
            else -> "Site Progress"
        }

        var voiceUpdate = VoiceUpdate(
            id = updateId,
            projectId = project.id,
            projectName = project.name,
            workerId = worker.workerId,
            workerName = worker.name,
            timestamp = now,
            formattedDateTime = formattedDate,
            durationSeconds = maxOf(1, durationSeconds),
            transcript = transcript.ifBlank { "Voice update logged for ${project.name}. Work progress recorded on schedule." },
            status = UpdateStatus.PENDING_APPROVAL,
            category = resolvedCategory,
            audioWaveform = if (waveform.isNotEmpty()) waveform else generateDefaultWaveform(),
            audioFilePath = audioFilePath,
            photoFilePath = photoFilePath
        )

        // Attempt upload to backend
        try {
            val audioFile = audioFilePath?.let { File(it) }
            val audioPart = if (audioFile != null && audioFile.exists()) {
                val requestFile = audioFile.asRequestBody("audio/mp4".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("audio", audioFile.name, requestFile)
            } else {
                val requestFile = ByteArray(0).toRequestBody("audio/mp4".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("audio", "dummy.m4a", requestFile)
            }

            val photoFile = photoFilePath?.let { File(it) }
            val photoPart = if (photoFile != null && photoFile.exists()) {
                val requestFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("photo", photoFile.name, requestFile)
            } else {
                null
            }

            val durationBody = durationSeconds.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val waveformBody = waveform.joinToString(",").toRequestBody("text/plain".toMediaTypeOrNull())
            val transcriptBody = transcript.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = RetrofitClient.apiService.uploadVoice(
                audio = audioPart,
                photo = photoPart,
                durationSeconds = durationBody,
                waveformCsv = waveformBody,
                transcript = transcriptBody
            )

            if (response.isSuccessful && response.body() != null) {
                val net = response.body()!!.update
                val parsedStatus = when (net.status) {
                    "APPROVED", "Linked" -> UpdateStatus.APPROVED
                    "PENDING_APPROVAL", "Pending Review" -> UpdateStatus.PENDING_APPROVAL
                    "IN_REVIEW" -> UpdateStatus.IN_REVIEW
                    "FLAGGED" -> UpdateStatus.FLAGGED
                    else -> UpdateStatus.PENDING_APPROVAL
                }
                
                voiceUpdate = VoiceUpdate(
                    id = net.id,
                    projectId = net.projectId,
                    projectName = net.projectName,
                    workerId = net.workerId,
                    workerName = net.workerName,
                    timestamp = net.timestamp,
                    formattedDateTime = net.formattedDateTime,
                    durationSeconds = net.durationSeconds,
                    transcript = net.transcript,
                    status = parsedStatus,
                    category = net.category,
                    audioWaveform = net.waveform,
                    audioFilePath = net.audioFilePath,
                    photoFilePath = net.photoFilePath
                )
            }
        } catch (e: Exception) {
            Log.e("TimeAgentRepository", "Upload failed, falling back to local storage", e)
        }

        // Always save to Room to keep local UI updated
        voiceUpdateDao.insertUpdate(VoiceUpdateEntity.fromDomainModel(voiceUpdate))
        return voiceUpdate
    }

    suspend fun updateStatus(id: String, status: UpdateStatus) {
        voiceUpdateDao.updateStatus(id, status.name)
    }

    suspend fun deleteUpdate(id: String) {
        voiceUpdateDao.deleteUpdate(id)
    }

    private fun generateDefaultWaveform(): List<Float> {
        val random = Random()
        return List(15) { 0.2f + random.nextFloat() * 0.7f }
    }
}
