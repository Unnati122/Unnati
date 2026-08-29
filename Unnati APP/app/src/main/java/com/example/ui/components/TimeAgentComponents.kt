package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.VolumeUp
import com.example.audio.RecordingResult
import com.example.model.Project
import com.example.model.Worker
import com.example.ui.theme.UxBorder
import com.example.ui.theme.UxCardBorder
import com.example.ui.theme.UxLightSurface
import com.example.ui.theme.UxOrange
import com.example.ui.theme.UxOrangeBorder
import com.example.ui.theme.UxOrangeDark
import com.example.ui.theme.UxOrangeLight
import com.example.ui.theme.UxPrimaryText
import com.example.ui.theme.UxSecondaryText
import com.example.ui.theme.UxSurfaceBright
import com.example.ui.theme.UxSurfaceContainer
import com.example.ui.theme.UxSurfaceContainerLow
import com.example.ui.theme.UxWhite

/**
 * Unified, reusable Government-Grade Curved Arc / Half-Circle Top Header.
 * Used identically across Home and Profile pages to ensure 100% visual consistency.
 *
 * Features:
 * - Rich saffron-orange gradient
 * - Smooth geometric half-circle arc bottom curve with curveDip
 * - Concentric watermark engineering rings
 * - Safe statusBarsPadding and bottom clearance ensuring all content is completely visible above the curve
 */
@Composable
fun CurvedHeader(
    modifier: Modifier = Modifier,
    height: Dp = 270.dp,
    curveDip: Dp = 22.dp,
    topBar: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        // High-End Canvas Drawing: Multi-layer Architectural Terrain & Blueprint Shader
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val dip = curveDip.toPx()

            // 1. Smooth curved arc path
            val arcPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h - dip)
                cubicTo(
                    w * 0.75f, h,
                    w * 0.25f, h,
                    0f, h - dip
                )
                close()
            }

            // 2. Rich Multi-tone Burnt Amber to Radiant Saffron Angular/Vertical Gradient
            val baseGradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF7C2D12), // Deep burnt umber at top
                    Color(0xFF9A3412), // Terracotta rust
                    Color(0xFFC2410C), // Industrial saffron
                    Color(0xFFEA580C)  // Luminous orange
                ),
                startY = 0f,
                endY = h
            )
            drawPath(path = arcPath, brush = baseGradient)

            // 3. Ambient Warm Radial Luminescence behind avatar
            val radialGlow = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFDBA74).copy(alpha = 0.25f),
                    Color(0xFFFB923C).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(w * 0.5f, h * 0.42f),
                radius = w * 0.55f
            )
            drawPath(path = arcPath, brush = radialGlow)

            // 4. Civil Engineering / Surveying Topographic Contour Wave Lines
            val contourPath1 = Path().apply {
                moveTo(0f, h * 0.25f)
                cubicTo(
                    w * 0.25f, h * 0.15f,
                    w * 0.65f, h * 0.40f,
                    w, h * 0.20f
                )
            }
            drawPath(
                path = contourPath1,
                color = Color.White.copy(alpha = 0.07f),
                style = Stroke(width = 1.8f)
            )

            val contourPath2 = Path().apply {
                moveTo(0f, h * 0.55f)
                cubicTo(
                    w * 0.35f, h * 0.70f,
                    w * 0.70f, h * 0.45f,
                    w, h * 0.60f
                )
            }
            drawPath(
                path = contourPath2,
                color = Color.White.copy(alpha = 0.05f),
                style = Stroke(width = 1.4f)
            )

            val contourPath3 = Path().apply {
                moveTo(0f, h * 0.75f)
                cubicTo(
                    w * 0.30f, h * 0.85f,
                    w * 0.75f, h * 0.65f,
                    w, h * 0.80f
                )
            }
            drawPath(
                path = contourPath3,
                color = Color.White.copy(alpha = 0.04f),
                style = Stroke(width = 1.2f)
            )

            // 5. Architectural CAD Blueprint Dot Matrix
            val dotSpacing = 28.dp.toPx()
            val dotRadius = 1.2f
            val dotColor = Color.White.copy(alpha = 0.08f)
            var curX = dotSpacing * 0.5f
            while (curX < w) {
                var curY = dotSpacing * 0.5f
                while (curY < h - dip) {
                    drawCircle(
                        color = dotColor,
                        radius = dotRadius,
                        center = Offset(curX, curY)
                    )
                    curY += dotSpacing
                }
                curX += dotSpacing
            }

            // 6. Concentric watermark telemetry rings radiating from center
            val centerAnchor = Offset(w * 0.5f, h * 0.42f)
            val ringColor = Color.White.copy(alpha = 0.06f)
            for (r in listOf(45.dp.toPx(), 75.dp.toPx(), 110.dp.toPx(), 150.dp.toPx(), 200.dp.toPx())) {
                drawCircle(
                    color = ringColor,
                    radius = r,
                    center = centerAnchor,
                    style = Stroke(width = 1.2f)
                )
            }

            // 7. Subtle Survey Coordinate Crosshairs (+)
            val crosshairColor = Color.White.copy(alpha = 0.12f)
            val crossPoints = listOf(
                Offset(w * 0.16f, h * 0.28f),
                Offset(w * 0.84f, h * 0.32f),
                Offset(w * 0.12f, h * 0.68f),
                Offset(w * 0.88f, h * 0.64f)
            )
            for (pt in crossPoints) {
                val len = 4.dp.toPx()
                drawLine(crosshairColor, Offset(pt.x - len, pt.y), Offset(pt.x + len, pt.y), strokeWidth = 1.2f)
                drawLine(crosshairColor, Offset(pt.x, pt.y - len), Offset(pt.x, pt.y + len), strokeWidth = 1.2f)
            }

            // 8. Crisp luminous bottom curve rim/beveled edge
            val bottomEdgePath = Path().apply {
                moveTo(0f, h - dip)
                cubicTo(
                    w * 0.25f, h,
                    w * 0.75f, h,
                    w, h - dip
                )
            }
            val edgeGradients = Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.05f),
                    Color.White.copy(alpha = 0.40f),
                    Color.White.copy(alpha = 0.05f)
                )
            )
            drawPath(
                path = bottomEdgePath,
                brush = edgeGradients,
                style = Stroke(width = 2.0f)
            )
        }

        // Inner Content positioned with safe area insets and structured vertical weights
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 4.dp,
                    bottom = (curveDip + 16.dp) // Ensures all details are positioned comfortably above the bottom curve
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar Slot (Menu button, Government emblem, Status indicators)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                topBar()
            }

            // Main Content Slot (User information, Avatar, Titles) with weight to adapt dynamically
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                content()
            }
        }
    }
}

/**
 * Official Ministry of Petroleum & Natural Gas (MoPNG) Header Badge
 */
@Composable
fun MoPNGPill(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
            )
            Text(
                text = "GOVT OF INDIA • MoPNG",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Header Status / Verification Badge Pill
 */
@Composable
fun HeaderStatusBadge(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = text,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Unified Circular Menu / Sidebar Button for all sections and screens.
 * Guarantees identical 38dp x 38dp circular geometry, centered 20dp 3-line hamburger icon,
 * frosted translucent styling on curved headers or crisp solid styling on light surfaces.
 */
@Composable
fun AppMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLightOnDark: Boolean = true,
    testTag: String = "menu_button"
) {
    val bgColor = if (isLightOnDark) Color.White.copy(alpha = 0.22f) else Color.White
    val borderColor = if (isLightOnDark) Color.White.copy(alpha = 0.35f) else Color(0xFFE2E8F0)
    val iconTint = if (isLightOnDark) Color.White else Color(0xFF334155)
    val rippleTint = if (isLightOnDark) Color.White else UxOrange

    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = rippleTint),
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Open navigation drawer",
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun TimeAgentTopBar(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(UxWhite)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppMenuButton(
                onClick = onMenuClick,
                isLightOnDark = false,
                testTag = "menu_button"
            )

            Column {
                Text(
                    text = "UNNATI",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = UxPrimaryText,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = "Infrastructure Voice Layer",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = UxSecondaryText
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(UxLightSurface)
                .border(1.dp, UxBorder, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security Verified",
                    tint = UxOrangeDark,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "UX4G Verified",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = UxPrimaryText
                )
            }
        }
    }
}

@Composable
fun InfoCard(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "info_card",
    subLabel: String? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                onClick = onClick
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = UxWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, UxBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(UxOrangeLight)
                        .border(1.dp, UxOrangeBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = UxOrangeDark,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = UxSecondaryText,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = value,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = UxPrimaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subLabel.isNullOrBlank()) {
                        Text(
                            text = subLabel,
                            fontSize = 11.sp,
                            color = UxSecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Select $label",
                tint = UxSecondaryText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun IdleMicSection(
    onMicClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "idle_mic_transition")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Multi-ring concentric radar mic beacon (Spacious & Crisp)
        Box(
            modifier = Modifier.size(136.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outermost animated radar pulse ring
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .scale(pulseScale)
                    .border(
                        width = 2.dp,
                        color = UxOrange.copy(alpha = pulseAlpha),
                        shape = CircleShape
                    )
            )

            // Outer fixed guide ring
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .border(
                        width = 1.dp,
                        color = UxOrange.copy(alpha = 0.18f),
                        shape = CircleShape
                    )
            )

            // Inner subtle halo ring
            Box(
                modifier = Modifier
                    .size(94.dp)
                    .background(UxOrangeLight.copy(alpha = 0.5f), CircleShape)
                    .border(
                        width = 1.dp,
                        color = UxOrangeBorder,
                        shape = CircleShape
                    )
            )

            // Center Mic Button (White core with metallic orange glyph and crisp border)
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape,
                        ambientColor = UxOrange.copy(alpha = 0.15f),
                        spotColor = UxOrange.copy(alpha = 0.35f)
                    )
                    .clip(CircleShape)
                    .background(UxWhite)
                    .border(width = 2.5.dp, color = UxOrange, shape = CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = UxOrange),
                        onClick = onMicClick
                    )
                    .testTag("start_recording_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Tap to speak and record voice update",
                    tint = UxOrangeDark,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Tap to Speak Ground Update",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = UxPrimaryText
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.RecordVoiceOver,
                contentDescription = null,
                tint = UxOrangeDark,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Vernacular Voice: Hindi • English • Regional",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = UxSecondaryText,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Example Voice Prompt Box
        Box(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .clip(RoundedCornerShape(12.dp))
                .background(UxLightSurface)
                .border(1.dp, UxBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = UxOrange,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Example: “Line 24 pipe laying completed today at KM 142.”",
                    fontSize = 11.5.sp,
                    color = UxSecondaryText,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RecordingActiveSection(
    elapsedSeconds: Int,
    waveform: List<Float>,
    liveTranscript: String,
    onStopClick: () -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_recording")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "active_pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "active_pulse_alpha"
    )

    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textPulse"
    )

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Listening badge indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDC2626).copy(alpha = textAlpha))
            )
            Text(
                text = "Recording Active (Voice AI Analyzing...)",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = UxOrangeDark.copy(alpha = textAlpha)
            )
        }

        // Active Pulsing Mic Button (Compact)
        Box(
            modifier = Modifier.size(92.dp),
            contentAlignment = Alignment.Center
        ) {
            // Pulse Ring
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .scale(pulseScale)
                    .border(
                        width = 2.dp,
                        color = UxOrange.copy(alpha = pulseAlpha),
                        shape = CircleShape
                    )
            )

            // Outer fixed guide ring
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .border(
                        width = 1.dp,
                        color = UxOrange.copy(alpha = 0.25f),
                        shape = CircleShape
                    )
            )

            // Center Orange Filled Button
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape, spotColor = UxOrange.copy(alpha = 0.4f))
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFEA580C), Color(0xFFC2410C))
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color.White),
                        onClick = onStopClick
                    )
                    .testTag("active_recording_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Recording Active - Tap to Finish",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Dynamic waveform visualizer bars
        Row(
            modifier = Modifier
                .height(18.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bars = if (waveform.isNotEmpty()) waveform else List(12) { 0.35f }
            bars.forEachIndexed { _, amp ->
                val barHeight = (3.dp + (amp * 15).dp)
                val alpha = 0.4f + (amp * 0.6f).coerceIn(0f, 0.6f)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .width(3.dp)
                        .height(barHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(UxOrange.copy(alpha = alpha))
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Live Timer
        Text(
            text = formattedTime,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = UxPrimaryText,
            modifier = Modifier.testTag("recording_timer")
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Live transcript or prompt hint
        Text(
            text = if (liveTranscript.isNotBlank()) "\"$liveTranscript\"" else "“Line 24 pipe laying completed today.”",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (liveTranscript.isNotBlank()) UxPrimaryText else UxSecondaryText,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Dual Action Controls: Cancel / Discard & Stop & Review
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Cancel / Discard Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0x11000000))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(width = 1.2.dp, color = Color(0xFFCBD5E1), shape = RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color(0xFF64748B)),
                        onClick = onCancelClick
                    )
                    .testTag("cancel_recording_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Recording",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Cancel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                }
            }

            // 2. Finish & Review Button (Stop Recording)
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .height(44.dp)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(22.dp), spotColor = Color(0xFFEA580C).copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFEA580C), Color(0xFFC2410C))
                        )
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White),
                        onClick = onStopClick
                    )
                    .testTag("stop_recording_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Finish and Review",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = "Finish & Review",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ProcessingSection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = UxOrange,
            strokeWidth = 3.5.dp,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Processing Ground Update...",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = UxPrimaryText
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "AI Schedule-Linking Engine in progress.\nMatching spoken report to Primavera P6 WBS tasks.",
            fontSize = 13.sp,
            color = UxSecondaryText,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun AudioSubmissionConfirmDialog(
    project: Project,
    worker: Worker,
    recordingResult: RecordingResult,
    isPlaying: Boolean,
    playbackProgress: Float,
    onTogglePlay: () -> Unit,
    onConfirmSubmit: () -> Unit,
    onDiscard: () -> Unit,
    onStopPlayback: (() -> Unit)? = null
) {
    // Automatically stop any playing audio whenever this dialog leaves composition
    DisposableEffect(Unit) {
        onDispose {
            onStopPlayback?.invoke()
        }
    }

    val totalSeconds = recordingResult.durationSeconds
    val currentSeconds = (totalSeconds * playbackProgress).toInt()
    val formattedCurrent = String.format("%02d:%02d", currentSeconds / 60, currentSeconds % 60)
    val formattedTotal = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)

    // Dynamic waveform bars
    val rawWave = recordingResult.waveform.ifEmpty {
        listOf(0.3f, 0.6f, 0.8f, 0.4f, 0.9f, 0.7f, 0.5f, 0.8f, 0.4f, 0.6f, 0.7f, 0.5f, 0.9f, 0.6f, 0.3f)
    }

    Dialog(
        onDismissRequest = onDiscard,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("audio_confirm_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFFF1EB)
                            ) {
                                Text(
                                    text = project.code,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = UxOrangeDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "Voice Submission",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = project.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color(0xFF94A3B8)),
                                onClick = onDiscard
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Audio Playback & Waveform Strip
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Play/Pause Action
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(UxOrange)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = Color.White),
                                    onClick = onTogglePlay
                                )
                                .testTag("preview_audio_play_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause audio" else "Play recorded audio",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Interactive Multi-bar Waveform
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val barCount = 18
                            for (i in 0 until barCount) {
                                val normalizedIdx = (i.toFloat() / barCount * rawWave.size).toInt().coerceIn(0, rawWave.size - 1)
                                val barHeightFraction = rawWave[normalizedIdx].coerceIn(0.2f, 1.0f)
                                val isPlayed = (i.toFloat() / barCount) <= playbackProgress

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height((barHeightFraction * 26).dp.coerceAtLeast(4.dp))
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (isPlayed) UxOrange else Color(0xFFCBD5E1)
                                        )
                                )
                            }
                        }

                        // Time display
                        Text(
                            text = if (isPlaying) formattedCurrent else formattedTotal,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }
                }

                // Transcript Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFAFAFA),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFECEFF1))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TRANSCRIPT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "\"${recordingResult.transcript}\"",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF1E293B),
                            lineHeight = 19.sp
                        )
                    }
                }

                // Operator & Destination Context
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Syncs with Primavera P6 schedule",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Discard Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color(0xFF94A3B8)),
                                onClick = onDiscard
                            )
                            .testTag("discard_recording_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Discard",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }

                    // Confirm Submit Button
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(UxOrange)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = Color.White),
                                onClick = onConfirmSubmit
                            )
                            .testTag("confirm_submit_recording_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Submit Update",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
