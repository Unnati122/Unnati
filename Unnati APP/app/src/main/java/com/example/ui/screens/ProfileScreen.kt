package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppMenuButton
import com.example.ui.components.CurvedHeader
import com.example.ui.components.HeaderStatusBadge
import com.example.ui.components.MoPNGPill
import com.example.ui.theme.UxBorder
import com.example.ui.theme.UxBrownPrimary
import com.example.ui.theme.UxCardBorder
import com.example.ui.theme.UxLightSurface
import com.example.ui.theme.UxOrange
import com.example.ui.theme.UxOrangeBorder
import com.example.ui.theme.UxOrangeContainer
import com.example.ui.theme.UxOrangeDark
import com.example.ui.theme.UxOrangeLight
import com.example.ui.theme.UxPrimaryText
import com.example.ui.theme.UxSecondaryText
import com.example.ui.theme.UxSuccessContainer
import com.example.ui.theme.UxSuccessGreenDark
import com.example.ui.theme.UxSurfaceBright
import com.example.ui.theme.UxSurfaceContainer
import com.example.ui.theme.UxSurfaceContainerLow
import com.example.ui.theme.UxSurfaceLowest
import com.example.ui.theme.UxTextMuted
import com.example.ui.theme.UxTextSecondary
import com.example.ui.theme.UxWhite
import com.example.viewmodel.TimeAgentViewModel

@Composable
fun ProfileScreen(
    viewModel: TimeAgentViewModel,
    onOpenWorkerSelector: () -> Unit,
    onOpenProjectSelector: () -> Unit,
    onOpenDrawer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val worker by viewModel.selectedWorker.collectAsState()
    val project by viewModel.selectedProject.collectAsState()
    val stats by viewModel.workerStats.collectAsState()

    var autoTranscribeEnabled by remember { mutableStateOf(true) }
    var offlineSyncEnabled by remember { mutableStateOf(true) }
    var biometricUnlockEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UxSurfaceBright)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Signature Government Half-Circle / Curved Arc Top Header (Exact same unified CurvedHeader component)
        CurvedHeader(
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onOpenDrawer != null) {
                        AppMenuButton(
                            onClick = onOpenDrawer,
                            isLightOnDark = true,
                            testTag = "menu_button"
                        )
                    } else {
                        Spacer(modifier = Modifier.size(38.dp))
                    }

                    HeaderStatusBadge(text = "UX4G Verified", icon = Icons.Default.Shield)
                }
            }
        ) {
            // Centered User Information Block inside the Dome (Comfortably placed high above the bottom curve)
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
                    text = worker.name,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.2.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${worker.role} • ${worker.department}",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.94f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 2. Main Profile Content Body Below the Curved Dome
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Unified Activity & Work Stats Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_stats_container"),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileMetricItem(
                        title = "Updates",
                        value = "${stats.totalUpdates}",
                        icon = Icons.Default.CheckCircle,
                        accentColor = Color(0xFF2563EB),
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFFE2E8F0)))
                    ProfileMetricItem(
                        title = "Approved",
                        value = "${stats.approvedUpdates}",
                        icon = Icons.Default.Verified,
                        accentColor = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFFE2E8F0)))
                    ProfileMetricItem(
                        title = "Pending",
                        value = "${stats.pendingUpdates}",
                        icon = Icons.Default.Schedule,
                        accentColor = UxOrangeDark,
                        modifier = Modifier.weight(1f)
                    )
                    Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color(0xFFE2E8F0)))
                    ProfileMetricItem(
                        title = "Hours",
                        value = "${stats.hoursLogged.toInt()}h",
                        icon = Icons.Default.AccessTime,
                        accentColor = Color(0xFF9333EA),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Site & Assigned Project Card (Fixed & Clean)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("profile_project_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = UxWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, UxBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE SITE DEPLOYMENT",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = UxSecondaryText,
                            letterSpacing = 0.6.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 8.dp, vertical = 2.5.dp)
                        ) {
                            Text(
                                text = "ON-SITE ACTIVE",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    HorizontalDivider(color = UxBorder.copy(alpha = 0.6f))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(UxOrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = UxOrangeDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Current Project", fontSize = 11.sp, color = UxSecondaryText)
                            Text(project.name, fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = UxPrimaryText)
                            Text("${project.code} • ${project.location}", fontSize = 11.sp, color = UxSecondaryText)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(UxLightSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GpsFixed,
                                contentDescription = null,
                                tint = Color(0xFF2563EB),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("GPS Site Coordinates", fontSize = 11.sp, color = UxSecondaryText)
                            Text("26.2389° N, 71.3712° E (Sector 4 Geofence)", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = UxPrimaryText)
                        }
                    }
                }
            }

            // Official Verification & UX4G Credentials Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = UxWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, UxBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "OFFICIAL OPERATOR CREDENTIALS",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = UxSecondaryText,
                        letterSpacing = 0.6.sp
                    )

                    HorizontalDivider(color = UxBorder.copy(alpha = 0.6f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = UxOrangeDark, modifier = Modifier.size(20.dp))
                            Column {
                                Text("UX4G Digital ID", fontSize = 11.sp, color = UxSecondaryText)
                                Text("IND-MOPNG-99482", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = UxPrimaryText)
                            }
                        }
                        Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = UxOrangeDark, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Contact Telemetry", fontSize = 11.sp, color = UxSecondaryText)
                                Text(worker.phoneNumber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = UxPrimaryText)
                            }
                        }
                    }
                }
            }

            // Voice & App Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = UxWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, UxBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "VOICE ENGINE & SYNC SETTINGS",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = UxSecondaryText,
                        letterSpacing = 0.6.sp
                    )

                    HorizontalDivider(color = UxBorder.copy(alpha = 0.6f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = UxOrangeDark, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Vernacular Speech AI", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = UxPrimaryText)
                                Text("Automatic multilingual recognition", fontSize = 11.sp, color = UxSecondaryText)
                            }
                        }
                        Switch(
                            checked = autoTranscribeEnabled,
                            onCheckedChange = { autoTranscribeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = UxOrange
                            )
                        )
                    }

                    HorizontalDivider(color = UxBorder.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = UxOrangeDark, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Offline Queue & Sync", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = UxPrimaryText)
                                Text("Automatic sync upon network re-connection", fontSize = 11.sp, color = UxSecondaryText)
                            }
                        }
                        Switch(
                            checked = offlineSyncEnabled,
                            onCheckedChange = { offlineSyncEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = UxOrange
                            )
                        )
                    }

                    HorizontalDivider(color = UxBorder.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = UxOrangeDark, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Biometric Fast Unlock", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = UxPrimaryText)
                                Text("Fingerprint & face recognition for login", fontSize = 11.sp, color = UxSecondaryText)
                            }
                        }
                        Switch(
                            checked = biometricUnlockEnabled,
                            onCheckedChange = { biometricUnlockEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = UxOrange
                            )
                        )
                    }
                }
            }

            // Sign Out of Session Button
            OutlinedButton(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("logout_button"),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.6f)),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Sign Out of Session", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileMetricItem(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
            )
        }
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = UxPrimaryText
        )
        Text(
            text = title,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = UxSecondaryText,
            maxLines = 1
        )
    }
}
