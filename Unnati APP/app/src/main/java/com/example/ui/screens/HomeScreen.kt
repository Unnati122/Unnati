package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.RecordingState
import com.example.ui.components.AppMenuButton
import com.example.ui.components.AudioSubmissionConfirmDialog
import com.example.ui.components.CurvedHeader
import com.example.ui.components.HeaderStatusBadge
import com.example.ui.components.IdleMicSection
import com.example.ui.components.MoPNGPill
import com.example.ui.components.ProcessingSection
import com.example.ui.components.RecordingActiveSection
import com.example.ui.theme.UxBorder
import com.example.ui.theme.UxOrange
import com.example.ui.theme.UxOrangeBorder
import com.example.ui.theme.UxOrangeDark
import com.example.ui.theme.UxOrangeLight
import com.example.ui.theme.UxPrimaryText
import com.example.ui.theme.UxSecondaryText
import com.example.ui.theme.UxSurfaceBright
import com.example.ui.theme.UxWhite
import com.example.viewmodel.TimeAgentViewModel

import java.io.File
import android.util.Log

@Composable
fun HomeScreen(
    viewModel: TimeAgentViewModel,
    onOpenDrawer: () -> Unit,
    onOpenProjectSelector: () -> Unit,
    onOpenWorkerSelector: () -> Unit,
    onNavigateToSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedProject by viewModel.selectedProject.collectAsState()
    val selectedWorker by viewModel.selectedWorker.collectAsState()
    val recordingState by viewModel.recordingManager.recordingState.collectAsState()
    val elapsedSeconds by viewModel.recordingManager.elapsedSeconds.collectAsState()
    val liveWaveform by viewModel.recordingManager.liveWaveform.collectAsState()
    val liveTranscript by viewModel.recordingManager.liveTranscript.collectAsState()
    val isProcessing by viewModel.isProcessingUpdate.collectAsState()
    val pendingRecording by viewModel.pendingRecordingResult.collectAsState()
    val isPreviewPlaying by viewModel.isPreviewPlaying.collectAsState()
    val previewProgress by viewModel.previewProgress.collectAsState()
    val capturedPhotoFile by viewModel.capturedPhotoFile.collectAsState()

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val file = File(context.cacheDir, "photo_${System.currentTimeMillis()}.jpg")
            try {
                file.outputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                viewModel.setCapturedPhotoFile(file)
            } catch (e: Exception) {
                Log.e("HomeScreen", "Error saving photo bitmap", e)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    val takePhoto = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.startVoiceRecording()
    }

    val handleStartRecording = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startVoiceRecording()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val handleStopRecording = {
        viewModel.stopRecordingForReview()
    }

    val handleCancelRecording = {
        viewModel.cancelVoiceRecording()
    }

    // Audio Submission Review & Confirmation Dialog with playback
    pendingRecording?.let { result ->
        AudioSubmissionConfirmDialog(
            project = selectedProject,
            worker = selectedWorker,
            recordingResult = result,
            isPlaying = isPreviewPlaying,
            playbackProgress = previewProgress,
            photoFile = capturedPhotoFile,
            onTakePhoto = takePhoto,
            onRemovePhoto = { viewModel.setCapturedPhotoFile(null) },
            onTogglePlay = { viewModel.togglePreviewPlayback() },
            onConfirmSubmit = {
                viewModel.confirmAndSubmitRecording {
                    onNavigateToSuccess()
                }
            },
            onDiscard = {
                viewModel.discardPendingRecording()
            },
            onStopPlayback = {
                viewModel.stopPlayback()
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UxSurfaceBright)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Signature Government Half-Circle / Curved Arc Top Header (Identical layout to Profile page)
        CurvedHeader(
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppMenuButton(
                        onClick = onOpenDrawer,
                        isLightOnDark = true,
                        testTag = "menu_button"
                    )

                    HeaderStatusBadge(text = "UX4G Verified", icon = Icons.Default.Shield)
                }
            }
        ) {
            // Centered User Information Block inside the Dome (Identical to Profile screen)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Hardhat Operator Avatar with Frosted Glass Outer Ring & On-Duty Indicator
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                                .shadow(3.dp, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Engineering,
                                contentDescription = "Operator Avatar",
                                tint = UxOrangeDark,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                    // Verified On-Duty Indicator
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                            .border(2.dp, Color.White, CircleShape)
                    )
                }

                Text(
                    text = selectedWorker.name,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${selectedWorker.role} • ${selectedWorker.department}",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.94f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 2. Main Content Body with Fixed Compact Info Strip and Center Mic Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Sleek, Fixed Dual-Information Bar (Active Project & Operator ID - Read-only, no chevrons)
            FixedProjectAndOperatorBar(
                projectName = selectedProject.name,
                projectCode = selectedProject.code,
                workerId = selectedWorker.workerId,
                shift = selectedWorker.shift
            )

            // Center Voice Recording Interface Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color.Black.copy(alpha = 0.04f)
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = UxWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, UxBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 22.dp, horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isProcessing) {
                        ProcessingSection(modifier = Modifier.fillMaxWidth())
                    } else {
                        AnimatedContent(
                            targetState = recordingState,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "recording_state_transition"
                        ) { state ->
                            when (state) {
                                RecordingState.RECORDING -> {
                                    RecordingActiveSection(
                                        elapsedSeconds = elapsedSeconds,
                                        waveform = liveWaveform,
                                        liveTranscript = liveTranscript,
                                        onStopClick = handleStopRecording,
                                        onCancelClick = handleCancelRecording,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                else -> {
                                    IdleMicSection(
                                        onMicClick = handleStartRecording,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

/**
 * Sleek, elegant metadata card displaying the Active Project & Operator ID.
 * Seamless single-card layout with clean hierarchy, matching orange theme branding and parallel alignment.
 */
@Composable
private fun FixedProjectAndOperatorBar(
    projectName: String,
    projectCode: String,
    workerId: String,
    shift: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fixed_project_operator_bar"),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Active Project with Leading Orange Icon and Code Tag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF1EB)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = "Project Icon",
                        tint = UxOrangeDark,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = projectName,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = UxPrimaryText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFFF1EB),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFFFD8C2))
                ) {
                    Text(
                        text = projectCode,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = UxOrangeDark,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)

            // Row 2: Operator ID & Shift - Parallel Orange Badge Icon matching Row 1, No green dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF1EB)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = "Worker ID Badge",
                        tint = UxOrangeDark,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "ID: $workerId",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                Text(
                    text = "•",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )

                Text(
                    text = shift,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}


