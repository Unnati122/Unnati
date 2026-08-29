package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.RecordingResult
import com.example.audio.RecordingState
import com.example.audio.VoiceRecordingManager
import com.example.data.local.AppDatabase
import com.example.data.repository.TimeAgentRepository
import com.example.model.Project
import com.example.model.UpdateStatus
import com.example.model.VoiceUpdate
import com.example.model.Worker
import com.example.model.WorkerStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import android.media.MediaPlayer
import android.util.Log
import java.io.File
import com.example.data.network.RetrofitClient
import com.example.data.network.LoginRequest

enum class AppScreen {
    LANDING,
    LOGIN,
    MAIN,
    SUCCESS
}

enum class AppTab(val route: String, val title: String) {
    HOME("home", "Voice Agent"),
    UPDATES("updates", "Updates"),
    PROFILE("profile", "Profile")
}

class TimeAgentViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = TimeAgentRepository(database)
    val recordingManager = VoiceRecordingManager(application, viewModelScope)

    // Screen & Tab Navigation Controller (State-Hoisted)
    private val _currentScreen = MutableStateFlow(AppScreen.MAIN)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    fun navigateToScreen(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            stopPlayback()
            _isProcessingUpdate.value = false
            recordingManager.reset()
            _currentScreen.value = screen
        }
    }

    fun selectTab(tab: AppTab) {
        if (_currentTab.value != tab) {
            // Pre-clean audio playback, recorder, and temporary state before displaying new tab content
            stopPlayback()
            _isProcessingUpdate.value = false
            recordingManager.reset()
            _currentTab.value = tab
        }
    }

    private val prefs = application.getSharedPreferences("time_agent_prefs", Context.MODE_PRIVATE)

    init {
        val storedToken = prefs.getString("key_auth_token", null)
        if (storedToken != null) {
            RetrofitClient.token = storedToken
            val savedWorkerId = prefs.getString("key_logged_in_worker_id", null)
            if (savedWorkerId != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    val w = database.workerDao().getWorkerById(savedWorkerId)
                    if (w != null) {
                        _selectedWorker.value = w.toDomainModel()
                        val p = database.projectDao().getProjectById(w.assignedProjectId)
                        if (p != null) {
                            _selectedProject.value = p.toDomainModel()
                        }
                    }
                    repository.syncMyUpdates()
                }
            }
        }
    }

    // Onboarding State (True once user passes or skips 3-screen onboarding)
    private val _isOnboardingCompleted = MutableStateFlow(
        prefs.getBoolean("key_onboarding_completed", false)
    )
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    fun completeOnboarding() {
        prefs.edit().putBoolean("key_onboarding_completed", true).apply()
        _isOnboardingCompleted.value = true
    }

    fun resetOnboarding() {
        prefs.edit().putBoolean("key_onboarding_completed", false).apply()
        _isOnboardingCompleted.value = false
    }

    // Authentication State (False for first-time launch until user enters credentials or quick logins)
    private val _isAuthenticated = MutableStateFlow(
        prefs.getBoolean("key_is_authenticated", false)
    )
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Current Active Project (OIL Pipeline Expansion)
    private val _selectedProject = MutableStateFlow(
        Project(
            id = "PRJ-01",
            name = "OIL Pipeline Expansion",
            code = "OPE-24",
            location = "Barmer-Salaya Corridor, Rajasthan",
            client = "Oil India Ltd / Bharat Petro Infra",
            activeWorkersCount = 164
        )
    )
    val selectedProject: StateFlow<Project> = _selectedProject.asStateFlow()

    // Current Active Worker (WK-10245)
    private val _selectedWorker = MutableStateFlow(
        Worker(
            workerId = "WK-10245",
            name = "Rajesh Sharma",
            role = "Site Supervisor",
            department = "Piping & Field Operations",
            phoneNumber = "+91 98201 45892",
            assignedProjectId = "PRJ-01",
            shift = "Morning Shift (07:00 - 15:30)"
        )
    )
    val selectedWorker: StateFlow<Worker> = _selectedWorker.asStateFlow()

    // Processing status during AI schedule-linking
    private val _isProcessingUpdate = MutableStateFlow(false)
    val isProcessingUpdate: StateFlow<Boolean> = _isProcessingUpdate.asStateFlow()

    // Confirmation & Review Dialog State
    private val _pendingRecordingResult = MutableStateFlow<RecordingResult?>(null)
    val pendingRecordingResult: StateFlow<RecordingResult?> = _pendingRecordingResult.asStateFlow()

    private val _isPreviewPlaying = MutableStateFlow(false)
    val isPreviewPlaying: StateFlow<Boolean> = _isPreviewPlaying.asStateFlow()

    private val _previewProgress = MutableStateFlow(0f)
    val previewProgress: StateFlow<Float> = _previewProgress.asStateFlow()

    // Projects & Workers from DB
    val allProjects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allWorkers: StateFlow<List<Worker>> = repository.allWorkers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Updates from DB
    val allUpdates: StateFlow<List<VoiceUpdate>> = repository.allUpdates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filter & Search for Updates Screen
    private val _selectedStatusFilter = MutableStateFlow<UpdateStatus?>(null)
    val selectedStatusFilter: StateFlow<UpdateStatus?> = _selectedStatusFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredUpdates: StateFlow<List<VoiceUpdate>> = combine(
        repository.allUpdates,
        _selectedStatusFilter,
        _searchQuery
    ) { updates, filter, query ->
        updates.filter { update ->
            val matchesFilter = filter == null || update.status == filter
            val matchesQuery = query.isBlank() ||
                    update.id.contains(query, ignoreCase = true) ||
                    update.projectName.contains(query, ignoreCase = true) ||
                    update.transcript.contains(query, ignoreCase = true) ||
                    update.category.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stats
    val workerStats: StateFlow<WorkerStats> = repository.workerStats
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            WorkerStats(totalUpdates = 4, approvedUpdates = 2, pendingUpdates = 1, hoursLogged = 24.5)
        )

    // Last Submitted Update (for Success Screen)
    private val _lastSubmittedUpdate = MutableStateFlow<VoiceUpdate?>(null)
    val lastSubmittedUpdate: StateFlow<VoiceUpdate?> = _lastSubmittedUpdate.asStateFlow()

    // Audio Playback state & MediaPlayer
    private val _playingUpdateId = MutableStateFlow<String?>(null)
    val playingUpdateId: StateFlow<String?> = _playingUpdateId.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var playbackJob: Job? = null

    // Login worker
    fun login(workerId: String, pin: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val trimmedId = workerId.trim()
        if (trimmedId.isBlank()) {
            onError("Please enter your Worker ID")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(trimmedId, pin))
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    RetrofitClient.token = body.token
                    
                    val u = body.user
                    repository.saveAuthenticatedWorker(u.username, u.name, u.role, u.responsibility, u.assignedProjects)
                    
                    val pId = if (u.assignedProjects.isNotEmpty()) u.assignedProjects[0] else "PROJ-1"
                    val savedWorker = database.workerDao().getWorkerById(u.username)?.toDomainModel()
                    val savedProject = database.projectDao().getProjectById(pId)?.toDomainModel()
                    
                    launch(Dispatchers.Main) {
                        if (savedWorker != null) {
                            _selectedWorker.value = savedWorker
                        }
                        if (savedProject != null) {
                            _selectedProject.value = savedProject
                        }
                        prefs.edit()
                            .putBoolean("key_is_authenticated", true)
                            .putString("key_logged_in_worker_id", u.username)
                            .putString("key_auth_token", body.token)
                            .apply()
                        _isAuthenticated.value = true
                        _currentTab.value = AppTab.HOME
                        _currentScreen.value = AppScreen.MAIN
                        onSuccess()
                    }
                    
                    repository.syncMyUpdates()
                } else {
                    launch(Dispatchers.Main) {
                        onError("Invalid credentials. Please verify your Worker ID and PIN.")
                    }
                }
            } catch (e: Exception) {
                Log.e("TimeAgentViewModel", "Network login error", e)
                // Fallback offline login for demo convenience if server is offline
                val foundWorker = allWorkers.value.find { it.workerId.equals(trimmedId, ignoreCase = true) }
                if (foundWorker != null && pin == "4892") {
                    launch(Dispatchers.Main) {
                        _selectedWorker.value = foundWorker
                        prefs.edit()
                            .putBoolean("key_is_authenticated", true)
                            .putString("key_logged_in_worker_id", foundWorker.workerId)
                            .apply()
                        _isAuthenticated.value = true
                        _currentTab.value = AppTab.HOME
                        _currentScreen.value = AppScreen.MAIN
                        onSuccess()
                    }
                } else {
                    launch(Dispatchers.Main) {
                        onError("Connection error: Unable to reach the server. Please try again.")
                    }
                }
            }
        }
    }

    // Biometric Login (Fingerprint / Face Unlock)
    fun loginWithBiometrics(workerId: String? = null, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val targetId = workerId?.trim()?.ifBlank { null } ?: _selectedWorker.value.workerId
        val savedToken = prefs.getString("key_auth_token", null)
        val foundWorker = allWorkers.value.find { it.workerId.equals(targetId, ignoreCase = true) }
            ?: _selectedWorker.value
            
        RetrofitClient.token = savedToken
        _selectedWorker.value = foundWorker
        
        prefs.edit()
            .putBoolean("key_is_authenticated", true)
            .putString("key_logged_in_worker_id", foundWorker.workerId)
            .apply()
        _isAuthenticated.value = true
        _currentTab.value = AppTab.HOME
        _currentScreen.value = AppScreen.MAIN
        
        viewModelScope.launch(Dispatchers.IO) {
            repository.syncMyUpdates()
        }
        onSuccess()
    }

    // Logout
    fun logout() {
        stopPlayback()
        recordingManager.reset()
        RetrofitClient.token = null
        prefs.edit()
            .putBoolean("key_is_authenticated", false)
            .remove("key_auth_token")
            .apply()
        _isAuthenticated.value = false
        _currentTab.value = AppTab.HOME
        _currentScreen.value = AppScreen.LOGIN
    }

    // Start Recording
    fun startVoiceRecording() {
        stopPlayback()
        _pendingRecordingResult.value = null
        _isProcessingUpdate.value = false
        recordingManager.startRecording(_selectedProject.value.name)
    }

    // Cancel active recording and discard audio
    fun cancelVoiceRecording() {
        stopPlayback()
        _pendingRecordingResult.value = null
        _isProcessingUpdate.value = false
        recordingManager.cancelRecording()
    }

    // Finish active recording and prepare for confirmation review
    fun stopRecordingForReview() {
        stopPlayback()
        val result = recordingManager.stopRecording(_selectedProject.value.name)
        _pendingRecordingResult.value = result
    }

    // Discard pending review
    fun discardPendingRecording() {
        stopPlayback()
        val pending = _pendingRecordingResult.value
        if (pending?.audioFilePath != null) {
            try {
                File(pending.audioFilePath).delete()
            } catch (e: Exception) {}
        }
        _pendingRecordingResult.value = null
        recordingManager.reset()
    }

    // Toggle audio preview of the pending unsubmitted recording
    fun togglePreviewPlayback() {
        if (_isPreviewPlaying.value) {
            stopPlayback()
            return
        }

        val pending = _pendingRecordingResult.value ?: return
        stopPlayback()

        val filePath = pending.audioFilePath
        val fileExists = filePath != null && File(filePath).exists()

        if (fileExists) {
            try {
                val player = MediaPlayer().apply {
                    setDataSource(filePath)
                    prepare()
                    setOnCompletionListener {
                        _isPreviewPlaying.value = false
                        _previewProgress.value = 0f
                        playbackJob?.cancel()
                    }
                    start()
                }
                mediaPlayer = player
                _isPreviewPlaying.value = true
                _previewProgress.value = 0f

                playbackJob = viewModelScope.launch(Dispatchers.Main) {
                    while (player.isPlaying) {
                        val duration = player.duration
                        if (duration > 0) {
                            _previewProgress.value = (player.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        }
                        delay(40)
                    }
                }
                return
            } catch (e: Exception) {
                Log.e("TimeAgentViewModel", "Failed to play preview file: $filePath", e)
            }
        }

        // Fallback simulation for playback preview
        _isPreviewPlaying.value = true
        _previewProgress.value = 0f

        val totalDurationMs = (pending.durationSeconds * 1000L).coerceAtLeast(2000L)
        val intervalMs = 50L
        val steps = (totalDurationMs / intervalMs).toInt()

        playbackJob = viewModelScope.launch(Dispatchers.Default) {
            for (i in 0..steps) {
                delay(intervalMs)
                _previewProgress.value = i.toFloat() / steps.toFloat()
            }
            _isPreviewPlaying.value = false
            _previewProgress.value = 0f
        }
    }

    // Confirm & Submit Voice Update to Database and Schedule linking engine
    fun confirmAndSubmitRecording(onComplete: (VoiceUpdate) -> Unit) {
        val result = _pendingRecordingResult.value ?: return
        stopPlayback()
        _pendingRecordingResult.value = null
        _isProcessingUpdate.value = true

        viewModelScope.launch(Dispatchers.IO) {
            val savedUpdate = repository.submitVoiceUpdate(
                project = _selectedProject.value,
                worker = _selectedWorker.value,
                durationSeconds = result.durationSeconds,
                transcript = result.transcript,
                waveform = result.waveform,
                audioFilePath = result.audioFilePath
            )
            _lastSubmittedUpdate.value = savedUpdate
            delay(900) // Brief smooth AI schedule-linking layer processing
            _isProcessingUpdate.value = false
            recordingManager.reset()
            launch(Dispatchers.Main) {
                onComplete(savedUpdate)
            }
        }
    }

    // Legacy direct stop & save if needed
    fun stopVoiceRecording(onComplete: (VoiceUpdate) -> Unit) {
        stopRecordingForReview()
        confirmAndSubmitRecording(onComplete)
    }

    fun resetRecordingState() {
        _isProcessingUpdate.value = false
        recordingManager.reset()
    }

    fun selectProject(project: Project) {
        _selectedProject.value = project
    }

    fun selectWorker(worker: Worker) {
        _selectedWorker.value = worker
    }

    fun setStatusFilter(status: UpdateStatus?) {
        _selectedStatusFilter.value = status
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun approveUpdate(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStatus(id, UpdateStatus.APPROVED)
        }
    }

    fun togglePlayback(update: VoiceUpdate) {
        if (_playingUpdateId.value == update.id) {
            stopPlayback()
        } else {
            startPlayback(update)
        }
    }

    private fun startPlayback(update: VoiceUpdate) {
        stopPlayback()

        val filePath = update.audioFilePath
        val fileExists = filePath != null && File(filePath).exists()

        if (fileExists) {
            try {
                val player = MediaPlayer().apply {
                    setDataSource(filePath)
                    prepare()
                    setOnCompletionListener {
                        _playingUpdateId.value = null
                        _playbackProgress.value = 0f
                        playbackJob?.cancel()
                    }
                    start()
                }
                mediaPlayer = player
                _playingUpdateId.value = update.id
                _playbackProgress.value = 0f

                playbackJob = viewModelScope.launch(Dispatchers.Main) {
                    while (player.isPlaying) {
                        val duration = player.duration
                        if (duration > 0) {
                            _playbackProgress.value = (player.currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        }
                        delay(50)
                    }
                }
                return
            } catch (e: Exception) {
                Log.e("TimeAgentViewModel", "Failed to play audio file: $filePath", e)
            }
        }

        // Fallback simulation for updates without physical audio files
        _playingUpdateId.value = update.id
        _playbackProgress.value = 0f

        val totalDurationMs = (update.durationSeconds * 1000L).coerceAtLeast(2000L)
        val intervalMs = 50L
        val steps = (totalDurationMs / intervalMs).toInt()

        playbackJob = viewModelScope.launch(Dispatchers.Default) {
            for (i in 0..steps) {
                delay(intervalMs)
                _playbackProgress.value = i.toFloat() / steps.toFloat()
            }
            _playingUpdateId.value = null
            _playbackProgress.value = 0f
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (e: Exception) {
            Log.e("TimeAgentViewModel", "Error stopping MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
        _playingUpdateId.value = null
        _playbackProgress.value = 0f
        _isPreviewPlaying.value = false
        _previewProgress.value = 0f
    }

    override fun onCleared() {
        super.onCleared()
        stopPlayback()
    }
}
