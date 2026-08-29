package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VoiceUpdate
import com.example.ui.theme.UxBrownPrimary
import com.example.ui.theme.UxCardBorder
import com.example.ui.theme.UxOrange
import com.example.ui.theme.UxOrangeBorder
import com.example.ui.theme.UxOrangeContainer
import com.example.ui.theme.UxOrangeLight
import com.example.ui.theme.UxPendingOrange
import com.example.ui.theme.UxSuccessContainer
import com.example.ui.theme.UxSuccessGreen
import com.example.ui.theme.UxSurfaceBright
import com.example.ui.theme.UxSurfaceContainer
import com.example.ui.theme.UxSurfaceContainerLow
import com.example.ui.theme.UxSurfaceLowest
import com.example.ui.theme.UxTextSecondary
import com.example.viewmodel.TimeAgentViewModel

@Composable
fun SuccessScreen(
    viewModel: TimeAgentViewModel,
    onViewMyUpdatesClick: () -> Unit,
    onRecordAnotherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastUpdate by viewModel.lastSubmittedUpdate.collectAsState()
    val scaleAnim = remember { Animatable(0f) }
    var showDetails by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val updateId = lastUpdate?.id ?: "UPD-2024-000123"
    val dateTime = lastUpdate?.formattedDateTime ?: "24 May 2024, 11:30 AM"
    val statusText = lastUpdate?.status?.label ?: "Pending Approval"
    val transcriptText = lastUpdate?.transcript ?: "Voice update recorded on site."

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UxSurfaceBright)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Success Icon (Green check in circle with scale-in animation)
        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(scaleAnim.value)
                .clip(CircleShape)
                .background(UxSuccessContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Success",
                tint = UxSuccessGreen,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Success Title & Subtitle
        Text(
            text = "Update Submitted!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF191C1D),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your update has been sent for\nreview and verification.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = UxTextSecondary,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Status Card matching Screenshot 3
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("status_summary_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = UxSurfaceLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, UxCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Update ID
                Column {
                    Text(
                        text = "Update ID",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = UxTextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = updateId,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF191C1D)
                    )
                }

                HorizontalDivider(color = UxSurfaceContainer, thickness = 1.dp)

                // Date & Time
                Column {
                    Text(
                        text = "Date & Time",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = UxTextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateTime,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF191C1D)
                    )
                }

                HorizontalDivider(color = UxSurfaceContainer, thickness = 1.dp)

                // Status
                Column {
                    Text(
                        text = "Status",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = UxTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = UxOrangeContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = statusText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = UxOrangeContainer
                        )
                    }
                }

                // Collapsible Transcript Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(UxSurfaceContainerLow)
                        .clickable { showDetails = !showDetails }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = UxOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Transcribed Voice Note",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = UxOrange
                                )
                            }
                            Icon(
                                imageVector = if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle transcript",
                                tint = UxTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        AnimatedVisibility(visible = showDetails) {
                            Text(
                                text = "\"$transcriptText\"",
                                fontSize = 13.sp,
                                color = Color(0xFF191C1D),
                                modifier = Modifier.padding(top = 8.dp),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // View My Updates Primary Button
        Button(
            onClick = onViewMyUpdatesClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("view_my_updates_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = UxOrangeContainer,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = "View My Updates",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Record Another Update
        OutlinedButton(
            onClick = {
                viewModel.resetRecordingState()
                onRecordAnotherClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("record_another_button"),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, UxOrangeBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = UxBrownPrimary)
        ) {
            Text(
                text = "Record Another Update",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
