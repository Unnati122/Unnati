package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UpdateStatus
import com.example.model.VoiceUpdate
import com.example.ui.components.AppMenuButton
import com.example.ui.theme.UxCardBorder
import com.example.ui.theme.UxInReviewBlue
import com.example.ui.theme.UxInReviewContainer
import com.example.ui.theme.UxOrange
import com.example.ui.theme.UxOrangeContainer
import com.example.ui.theme.UxOrangeLight
import com.example.ui.theme.UxPendingContainer
import com.example.ui.theme.UxPendingOrange
import com.example.ui.theme.UxSuccessContainer
import com.example.ui.theme.UxSuccessGreen
import com.example.ui.theme.UxSuccessGreenDark
import com.example.ui.theme.UxSurfaceBright
import com.example.ui.theme.UxSurfaceContainer
import com.example.ui.theme.UxSurfaceContainerLow
import com.example.ui.theme.UxSurfaceLowest
import com.example.ui.theme.UxTextMuted
import com.example.ui.theme.UxTextSecondary
import com.example.viewmodel.TimeAgentViewModel
import coil.compose.AsyncImage
import com.example.data.network.RetrofitClient

@Composable
fun UpdatesScreen(
    viewModel: TimeAgentViewModel,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val updates by viewModel.filteredUpdates.collectAsState()
    val selectedFilter by viewModel.selectedStatusFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val playingUpdateId by viewModel.playingUpdateId.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()

    var showSearchBar by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UxSurfaceBright)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search & Filter Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onOpenDrawer != null) {
                    AppMenuButton(
                        onClick = onOpenDrawer,
                        isLightOnDark = false,
                        testTag = "menu_button"
                    )
                }

                Column {
                    Text(
                        text = "Update History",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1D)
                    )
                    Text(
                        text = "${updates.size} updates logged",
                        fontSize = 12.sp,
                        color = UxTextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = UxOrange),
                        onClick = { showSearchBar = !showSearchBar }
                    )
                    .testTag("toggle_search_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = "Search updates",
                    tint = UxOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Search Bar (Expandable)
        AnimatedVisibility(visible = showSearchBar) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search by ID, project, transcript...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = UxTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search",
                                tint = UxTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("search_text_field"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = UxOrange,
                    unfocusedBorderColor = UxCardBorder,
                    focusedContainerColor = UxSurfaceLowest,
                    unfocusedContainerColor = UxSurfaceLowest
                ),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { viewModel.setStatusFilter(null) },
                    label = { Text("All", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = UxOrange,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_chip_all")
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == UpdateStatus.PENDING_APPROVAL,
                    onClick = { viewModel.setStatusFilter(UpdateStatus.PENDING_APPROVAL) },
                    label = { Text("Pending (${updates.count { it.status == UpdateStatus.PENDING_APPROVAL }})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = UxOrange,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_chip_pending")
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == UpdateStatus.APPROVED,
                    onClick = { viewModel.setStatusFilter(UpdateStatus.APPROVED) },
                    label = { Text("Approved (${updates.count { it.status == UpdateStatus.APPROVED }})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = UxSuccessGreenDark,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_chip_approved")
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == UpdateStatus.IN_REVIEW,
                    onClick = { viewModel.setStatusFilter(UpdateStatus.IN_REVIEW) },
                    label = { Text("In Review (${updates.count { it.status == UpdateStatus.IN_REVIEW }})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = UxInReviewBlue,
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("filter_chip_in_review")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Update List
        if (updates.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = UxTextMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No updates found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = UxTextSecondary
                    )
                    Text(
                        text = "Record your first voice update on the Home screen",
                        fontSize = 13.sp,
                        color = UxTextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(updates, key = { it.id }) { update ->
                    VoiceUpdateCard(
                        update = update,
                        isPlaying = playingUpdateId == update.id,
                        playbackProgress = if (playingUpdateId == update.id) playbackProgress else 0f,
                        onTogglePlay = { viewModel.togglePlayback(update) },
                        onApprove = { viewModel.approveUpdate(update.id) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun VoiceUpdateCard(
    update: VoiceUpdate,
    isPlaying: Boolean,
    playbackProgress: Float,
    onTogglePlay: () -> Unit,
    onApprove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("update_card_${update.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = UxSurfaceLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, UxCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Update ID & Category Tag on Left, Status Badge on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = update.id,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF191C1D)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(UxSurfaceContainer)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = update.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = UxTextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                StatusBadge(status = update.status)
            }

            // Project Name & Date/Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = update.projectName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UxOrange,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = update.formattedDateTime,
                    fontSize = 11.sp,
                    color = UxTextSecondary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Text(
                text = update.transcript,
                fontSize = 13.sp,
                color = Color(0xFF2E3132),
                lineHeight = 18.sp
            )

            // Display site progress photo if available
            if (!update.photoFilePath.isNullOrBlank()) {
                val base = RetrofitClient.baseUrl.removeSuffix("/")
                val path = update.photoFilePath.removePrefix("/")
                val imageUrl = "$base/$path"
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Site progress photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            // Audio Player Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(UxSurfaceContainerLow)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(UxOrange)
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play voice note",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isPlaying) "Playing Audio..." else "Voice Note",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isPlaying) UxOrange else UxTextSecondary
                        )
                        Text(
                            text = "00:${String.format("%02d", update.durationSeconds)}",
                            fontSize = 11.sp,
                            color = UxTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { if (isPlaying) playbackProgress else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = UxOrange,
                        trackColor = UxSurfaceContainer
                    )
                }
            }

            // Supervisor remarks if present
            if (!update.supervisorRemarks.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(UxSuccessContainer)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = UxSuccessGreenDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = update.supervisorRemarks,
                            fontSize = 11.sp,
                            color = Color(0xFF005320),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // If pending, allow quick verification test
            if (update.status == UpdateStatus.PENDING_APPROVAL) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onApprove,
                        colors = ButtonDefaults.textButtonColors(contentColor = UxSuccessGreenDark),
                        modifier = Modifier.testTag("approve_button_${update.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verify & Approve", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: UpdateStatus) {
    val (bgColor, textColor, icon) = when (status) {
        UpdateStatus.PENDING_APPROVAL -> Triple(
            UxPendingContainer,
            UxPendingOrange,
            Icons.Default.Schedule
        )
        UpdateStatus.APPROVED -> Triple(
            UxSuccessContainer,
            UxSuccessGreenDark,
            Icons.Default.CheckCircle
        )
        UpdateStatus.IN_REVIEW -> Triple(
            UxInReviewContainer,
            UxInReviewBlue,
            Icons.Default.HourglassTop
        )
        UpdateStatus.FLAGGED -> Triple(
            Color(0xFFFFDAD6),
            Color(0xFFBA1A1A),
            Icons.Default.Schedule
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = status.label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
