package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.UxBorder
import com.example.ui.theme.UxOrange
import com.example.ui.theme.UxOrangeDark
import com.example.ui.theme.UxOrangeLight
import com.example.ui.theme.UxPrimaryText
import com.example.ui.theme.UxSecondaryText
import com.example.ui.theme.UxSurfaceBright
import com.example.ui.theme.UxWhite
import kotlinx.coroutines.launch

data class OnboardingScreenData(
    val title: String,
    val subtitle: String
)

private val ONBOARDING_PAGES = listOf(
    OnboardingScreenData(
        title = "Speak. Report. Get\nThings Done.",
        subtitle = "Tap to speak updates in your own language. We'll take care of the rest."
    ),
    OnboardingScreenData(
        title = "Accurate. Verified.\nAlways in Sync.",
        subtitle = "Your updates are verified, synced, and linked to the right project and location."
    ),
    OnboardingScreenData(
        title = "Real Progress.\nReal Impact.",
        subtitle = "From field to office – real-time visibility that drives better decisions."
    )
)

/**
 * High-fidelity 3-Screen Onboarding Flow matching the exact reference specification:
 * - Saffron-Orange curved gradient header with concentric ring watermark & Skip button.
 * - 3 swipeable screens with clean vector illustrations:
 *   1. Construction worker with microphone, audio soundwaves & site backdrop.
 *   2. Smartphone with 'Update Submitted' checkmark and orbiting verified sync badges.
 *   3. Real-time Project Progress chart dashboard with field engineer reviewing telemetry.
 * - Bottom row with 3 pagination dots and dynamic Next/Get Started button.
 */
@Composable
fun LandingScreen(
    onGetStartedClick: () -> Unit,
    onDirectLoginClick: () -> Unit = onGetStartedClick,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UxWhite)
            .navigationBarsPadding()
    ) {
        // 1. Curved Orange Gradient Header (Exact same half-circle arch from profile screen)
        OnboardingCurvedHeader(
            onSkipClick = onGetStartedClick
        )

        // 2. Swipeable Middle Content Pager (Illustration + Typography)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            OnboardingPageContent(
                pageIndex = page,
                data = ONBOARDING_PAGES[page]
            )
        }

        // 3. Bottom Controls (Pagination Dots + Next / Get Started Action Button)
        OnboardingBottomBar(
            currentPage = pagerState.currentPage,
            totalPages = 3,
            onDotClick = { target ->
                scope.launch { pagerState.animateScrollToPage(target) }
            },
            onNextClick = {
                if (pagerState.currentPage < 2) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    onGetStartedClick()
                }
            }
        )
    }
}

/**
 * Signature Orange Curved Header with Half-Circle Bottom Arch
 */
@Composable
private fun OnboardingCurvedHeader(
    onSkipClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 190.dp,
    curveDip: Dp = 34.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        // Canvas with saffron-orange gradient and deep parabolic bottom curve
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val dip = curveDip.toPx()

            // Smooth curved half-circle arc bottom path
            val arcPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h - dip)
                cubicTo(
                    w * 0.70f, h + dip * 0.4f,
                    w * 0.30f, h + dip * 0.4f,
                    0f, h - dip
                )
                close()
            }

            val gradient = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF9A3412),
                    Color(0xFFC2410C),
                    Color(0xFFEA580C)
                ),
                startY = 0f,
                endY = h
            )
            drawPath(path = arcPath, brush = gradient)

            // Concentric watermark engineering arc rings radiating from top center
            val centerTop = Offset(w * 0.5f, -20f)
            val ringColor = Color.White.copy(alpha = 0.08f)
            for (r in listOf(60.dp.toPx(), 110.dp.toPx(), 160.dp.toPx(), 210.dp.toPx(), 260.dp.toPx())) {
                drawCircle(color = ringColor, radius = r, center = centerTop, style = Stroke(width = 1.5f))
            }
        }

        // Top Status Bar Area with Skip Button on Top-Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Color.White, bounded = true),
                        onClick = onSkipClick
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("onboarding_skip_button")
            ) {
                Text(
                    text = "Skip",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Slide Page Content (Vector Illustration + Crisp Headings & Subtitles)
 */
@Composable
private fun OnboardingPageContent(
    pageIndex: Int,
    data: OnboardingScreenData
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // 1. Clean Vector Illustration Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            contentAlignment = Alignment.Center
        ) {
            when (pageIndex) {
                0 -> IllustrationScreenOne()
                1 -> IllustrationScreenTwo()
                else -> IllustrationScreenThree()
            }
        }

        // 2. Headings & Subtitles
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = data.title,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827),
                lineHeight = 32.sp,
                letterSpacing = (-0.4).sp
            )

            Text(
                text = data.subtitle,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF64748B),
                lineHeight = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

/**
 * SCREEN 1 ILLUSTRATION:
 * Construction worker in orange helmet and vest speaking into mic + sound wave badge + crane & pin.
 */
@Composable
private fun IllustrationScreenOne() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Background elements: Crane, sand contour & location pin
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Soft earth/sand dunes background curves
            val dunePath = Path().apply {
                moveTo(w * 0.35f, h * 0.95f)
                cubicTo(w * 0.55f, h * 0.75f, w * 0.75f, h * 0.85f, w * 0.98f, h * 0.92f)
                lineTo(w * 0.98f, h * 0.98f)
                lineTo(w * 0.35f, h * 0.98f)
                close()
            }
            drawPath(
                path = dunePath,
                color = Color(0xFFFFEDD5).copy(alpha = 0.8f),
                style = Fill
            )

            // Construction Crane Silhouette (right background)
            val craneColor = Color(0xFFFDBA74).copy(alpha = 0.6f)
            val cranePath = Path().apply {
                // Mast
                moveTo(w * 0.78f, h * 0.95f)
                lineTo(w * 0.78f, h * 0.28f)
                lineTo(w * 0.82f, h * 0.28f)
                lineTo(w * 0.82f, h * 0.95f)

                // Horizontal Jib Arm
                moveTo(w * 0.60f, h * 0.32f)
                lineTo(w * 0.96f, h * 0.32f)

                // Crane Cables
                moveTo(w * 0.80f, h * 0.22f)
                lineTo(w * 0.60f, h * 0.32f)
                moveTo(w * 0.80f, h * 0.22f)
                lineTo(w * 0.96f, h * 0.32f)
            }
            drawPath(cranePath, color = craneColor, style = Stroke(width = 2.5f))

            // Lattice trusses inside crane mast
            for (i in 1..4) {
                val y1 = h * (0.35f + i * 0.12f)
                val y2 = h * (0.42f + i * 0.12f)
                drawLine(
                    color = craneColor,
                    start = Offset(w * 0.78f, y1),
                    end = Offset(w * 0.82f, y2),
                    strokeWidth = 1.5f
                )
            }
        }

        // Left Foreground: Vector Construction Worker
        WorkerVectorCharacter(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 4.dp)
        )

        // Right Foreground: Audio Sound Wave Floating Badge
        AudioWaveformBadge(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 24.dp, bottom = 16.dp)
        )

        // Location Pin Marker on the right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 40.dp, bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = UxOrange,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Stylized Vector Construction Worker Figure
 */
@Composable
private fun WorkerVectorCharacter(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(150.dp)
            .height(190.dp)
    ) {
        val w = size.width
        val h = size.height

        // 1. Worker Body (Blue Shirt + High-Vis Orange Vest)
        val bodyPath = Path().apply {
            moveTo(w * 0.15f, h * 0.98f)
            lineTo(w * 0.15f, h * 0.62f)
            cubicTo(w * 0.25f, h * 0.48f, w * 0.55f, h * 0.48f, w * 0.75f, h * 0.62f)
            lineTo(w * 0.78f, h * 0.98f)
            close()
        }
        // Blue shirt base
        drawPath(bodyPath, color = Color(0xFF1E3A5F))

        // High-vis Orange Safety Vest
        val vestPath = Path().apply {
            moveTo(w * 0.20f, h * 0.98f)
            lineTo(w * 0.20f, h * 0.62f)
            lineTo(w * 0.35f, h * 0.55f)
            lineTo(w * 0.42f, h * 0.98f)
            close()
        }
        val vestPathRight = Path().apply {
            moveTo(w * 0.72f, h * 0.98f)
            lineTo(w * 0.72f, h * 0.62f)
            lineTo(w * 0.55f, h * 0.55f)
            lineTo(w * 0.48f, h * 0.98f)
            close()
        }
        drawPath(vestPath, color = Color(0xFFEA580C))
        drawPath(vestPathRight, color = Color(0xFFEA580C))

        // Reflective Silver Vest Stripes
        drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(w * 0.22f, h * 0.74f),
            end = Offset(w * 0.40f, h * 0.74f),
            strokeWidth = 7f
        )
        drawLine(
            color = Color(0xFFE2E8F0),
            start = Offset(w * 0.50f, h * 0.74f),
            end = Offset(w * 0.70f, h * 0.74f),
            strokeWidth = 7f
        )

        // 2. Neck
        drawRect(
            color = Color(0xFFFDBA74),
            topLeft = Offset(w * 0.40f, h * 0.40f),
            size = Size(w * 0.16f, h * 0.16f)
        )

        // 3. Head & Face (Profile / Semi-profile)
        val facePath = Path().apply {
            moveTo(w * 0.35f, h * 0.24f)
            cubicTo(w * 0.35f, h * 0.45f, w * 0.62f, h * 0.46f, w * 0.66f, h * 0.35f)
            lineTo(w * 0.68f, h * 0.32f) // nose
            lineTo(w * 0.65f, h * 0.28f)
            lineTo(w * 0.62f, h * 0.20f)
            close()
        }
        drawPath(facePath, color = Color(0xFFFDBA74))

        // Dark hair
        val hairPath = Path().apply {
            moveTo(w * 0.32f, h * 0.24f)
            cubicTo(w * 0.32f, h * 0.38f, w * 0.42f, h * 0.38f, w * 0.42f, h * 0.30f)
            lineTo(w * 0.35f, h * 0.20f)
            close()
        }
        drawPath(hairPath, color = Color(0xFF1E293B))

        // 4. Hardhat / Safety Helmet (Orange)
        val helmetPath = Path().apply {
            moveTo(w * 0.22f, h * 0.20f)
            cubicTo(w * 0.22f, h * 0.04f, w * 0.68f, h * 0.04f, w * 0.70f, h * 0.20f)
            lineTo(w * 0.78f, h * 0.22f) // Brim front
            lineTo(w * 0.18f, h * 0.22f) // Brim back
            close()
        }
        drawPath(helmetPath, color = Color(0xFFEA580C))

        // Helmet ridge
        val ridgePath = Path().apply {
            moveTo(w * 0.38f, h * 0.10f)
            cubicTo(w * 0.45f, h * 0.06f, w * 0.55f, h * 0.06f, w * 0.62f, h * 0.10f)
        }
        drawPath(ridgePath, color = Color(0xFFC2410C), style = Stroke(width = 3.5f))

        // 5. Arm & Hand holding Walkie-Talkie / Microphone
        val armPath = Path().apply {
            moveTo(w * 0.68f, h * 0.65f)
            lineTo(w * 0.88f, h * 0.48f)
            lineTo(w * 0.82f, h * 0.42f)
            lineTo(w * 0.62f, h * 0.58f)
            close()
        }
        drawPath(armPath, color = Color(0xFF1E3A5F))

        // Hand
        drawCircle(
            color = Color(0xFFFDBA74),
            radius = 9f,
            center = Offset(w * 0.86f, h * 0.44f)
        )

        // Walkie-Talkie body
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(w * 0.82f, h * 0.32f),
            size = Size(18f, 28f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Antenna
        drawLine(
            color = Color(0xFF0F172A),
            start = Offset(w * 0.85f, h * 0.32f),
            end = Offset(w * 0.85f, h * 0.22f),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * Floating Audio Waveform Pill Badge
 */
@Composable
private fun AudioWaveformBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color(0x33EA580C))
            .clip(RoundedCornerShape(20.dp))
            .background(UxWhite)
            .border(1.dp, Color(0xFFFFEDD5), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val heights = listOf(8.dp, 16.dp, 24.dp, 18.dp, 12.dp, 22.dp, 14.dp, 8.dp)
            heights.forEach { h ->
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .height(h)
                        .clip(RoundedCornerShape(2.dp))
                        .background(UxOrange)
                )
            }
        }
    }
}

/**
 * SCREEN 2 ILLUSTRATION:
 * Smartphone in center with 'Update Submitted' checkmark + 5 orbiting verified sync badges on dashed ring.
 */
@Composable
private fun IllustrationScreenTwo() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Orbiting Dashed Ring
        Canvas(modifier = Modifier.size(244.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f + 6f)
            drawCircle(
                color = Color(0xFFE2E8F0),
                radius = size.width * 0.44f,
                center = center,
                style = Stroke(
                    width = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            )
        }

        // Center Phone Mockup (offset downward to give clear separation from the top calendar badge)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 52.dp)
        ) {
            PhoneCenterMockup()
        }

        // 1. TOP: Calendar Icon Badge (Clean clearance above mobile top bezel)
        OrbitIconBadge(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 0.dp),
            icon = Icons.Default.DateRange,
            tint = Color(0xFFEA580C),
            bg = Color(0xFFFFF7ED),
            border = Color(0xFFFFEDD5)
        )

        // 2. LEFT: Location Pin Icon Badge
        OrbitIconBadge(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp, top = 12.dp),
            icon = Icons.Default.LocationOn,
            tint = Color(0xFF2563EB),
            bg = Color(0xFFEFF6FF),
            border = Color(0xFFDBEAFE)
        )

        // 3. RIGHT: Cloud Sync Icon Badge
        OrbitIconBadge(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp, top = 12.dp),
            icon = Icons.Default.CloudDone,
            tint = Color(0xFF7C3AED),
            bg = Color(0xFFF5F3FF),
            border = Color(0xFFEDE9FE)
        )

        // 4. BOTTOM-LEFT: Checklist / WBS Icon Badge
        OrbitIconBadge(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 30.dp, bottom = 6.dp),
            icon = Icons.Default.Assignment,
            tint = Color(0xFFD97706),
            bg = Color(0xFFFFFBEB),
            border = Color(0xFFFEF3C7)
        )

        // 5. BOTTOM-RIGHT: Shield / Verified Icon Badge
        OrbitIconBadge(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 30.dp, bottom = 6.dp),
            icon = Icons.Default.Shield,
            tint = Color(0xFF16A34A),
            bg = Color(0xFFF0FDF4),
            border = Color(0xFFDCFCE7)
        )
    }
}

@Composable
private fun PhoneCenterMockup() {
    Box(
        modifier = Modifier
            .width(112.dp)
            .height(164.dp)
            .shadow(10.dp, RoundedCornerShape(18.dp), spotColor = Color(0x22000000))
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0F172A))
            .padding(3.5.dp)
    ) {
        // Inner Phone Screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(UxWhite)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Speaker notch
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFCBD5E1))
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Update\nSubmitted",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B),
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Big Green Checkmark Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun OrbitIconBadge(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    bg: Color,
    border: Color
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .shadow(5.dp, CircleShape, spotColor = tint.copy(alpha = 0.25f))
            .clip(CircleShape)
            .background(bg)
            .border(1.2.dp, border, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

/**
 * SCREEN 3 ILLUSTRATION:
 * Project Progress dashboard window (line graph, donut chart, checklist) + Engineer on right with tablet.
 */
@Composable
private fun IllustrationScreenThree() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Dashboard Card Window (Left/Center)
        Card(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
                .width(235.dp)
                .height(205.dp)
                .shadow(10.dp, RoundedCornerShape(16.dp), spotColor = Color(0x18000000)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = UxWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header Bar with 3 Window Dots & Orange title bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Project Progress",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Line Chart Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(0.8.dp, Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                        .padding(4.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Horizontal grid line
                        drawLine(
                            color = Color(0xFFE2E8F0),
                            start = Offset(0f, h * 0.5f),
                            end = Offset(w, h * 0.5f),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )

                        // Upward trending green line
                        val trendPath = Path().apply {
                            moveTo(4f, h * 0.78f)
                            lineTo(w * 0.25f, h * 0.65f)
                            lineTo(w * 0.50f, h * 0.72f)
                            lineTo(w * 0.75f, h * 0.40f)
                            lineTo(w * 0.95f, h * 0.20f)
                        }
                        drawPath(trendPath, color = Color(0xFF22C55E), style = Stroke(width = 2.5f, cap = StrokeCap.Round))

                        // Green data dots
                        val pts = listOf(
                            Offset(4f, h * 0.78f),
                            Offset(w * 0.25f, h * 0.65f),
                            Offset(w * 0.50f, h * 0.72f),
                            Offset(w * 0.75f, h * 0.40f),
                            Offset(w * 0.95f, h * 0.20f)
                        )
                        pts.forEach { pt ->
                            drawCircle(color = Color(0xFF22C55E), radius = 3f, center = pt)
                            drawCircle(color = Color.White, radius = 1.5f, center = pt)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Donut Chart + Checklist Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Chart
                    Canvas(modifier = Modifier.size(54.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.width * 0.38f
                        val stroke = 10f

                        // Segment 1 (Orange)
                        drawArc(
                            color = Color(0xFFEA580C),
                            startAngle = -90f,
                            sweepAngle = 130f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = stroke)
                        )
                        // Segment 2 (Blue)
                        drawArc(
                            color = Color(0xFF3B82F6),
                            startAngle = 40f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = stroke)
                        )
                        // Segment 3 (Green)
                        drawArc(
                            color = Color(0xFF10B981),
                            startAngle = 130f,
                            sweepAngle = 140f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = stroke)
                        )
                    }

                    // 3 Checklist items
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        for (i in 1..3) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDCFCE7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(8.dp)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .width(when (i) { 1 -> 52.dp; 2 -> 40.dp; else -> 46.dp })
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFFCBD5E1))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Engineer Character on Right Holding Tablet
        EngineerReviewerVector(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 2.dp)
        )
    }
}

/**
 * Engineer Character Reviewing Tablet
 */
@Composable
private fun EngineerReviewerVector(
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .width(115.dp)
            .height(170.dp)
    ) {
        val w = size.width
        val h = size.height

        // Body & Vest
        val bodyPath = Path().apply {
            moveTo(w * 0.30f, h * 0.98f)
            lineTo(w * 0.30f, h * 0.65f)
            cubicTo(w * 0.40f, h * 0.50f, w * 0.70f, h * 0.50f, w * 0.85f, h * 0.65f)
            lineTo(w * 0.85f, h * 0.98f)
            close()
        }
        drawPath(bodyPath, color = Color(0xFF1E3A5F))

        // Orange Vest
        val vest = Path().apply {
            moveTo(w * 0.38f, h * 0.98f)
            lineTo(w * 0.38f, h * 0.62f)
            lineTo(w * 0.52f, h * 0.55f)
            lineTo(w * 0.58f, h * 0.98f)
            close()
        }
        drawPath(vest, color = Color(0xFFEA580C))

        // Neck & Head
        drawRect(
            color = Color(0xFFFDBA74),
            topLeft = Offset(w * 0.50f, h * 0.42f),
            size = Size(w * 0.16f, h * 0.14f)
        )

        // Head profile looking left toward dashboard
        val face = Path().apply {
            moveTo(w * 0.45f, h * 0.28f)
            lineTo(w * 0.42f, h * 0.33f) // nose
            lineTo(w * 0.46f, h * 0.37f)
            cubicTo(w * 0.52f, h * 0.46f, w * 0.70f, h * 0.44f, w * 0.70f, h * 0.30f)
            close()
        }
        drawPath(face, color = Color(0xFFFDBA74))

        // Hair
        drawCircle(
            color = Color(0xFF0F172A),
            radius = 12f,
            center = Offset(w * 0.65f, h * 0.30f)
        )

        // White Safety Helmet
        val helmet = Path().apply {
            moveTo(w * 0.36f, h * 0.24f)
            cubicTo(w * 0.36f, h * 0.08f, w * 0.76f, h * 0.08f, w * 0.78f, h * 0.24f)
            lineTo(w * 0.84f, h * 0.26f)
            lineTo(w * 0.32f, h * 0.26f)
            close()
        }
        drawPath(helmet, color = Color(0xFFF8FAFC))
        drawPath(helmet, color = Color(0xFFCBD5E1), style = Stroke(width = 1.5f))

        // Arms holding Black Tablet
        val tablet = Path().apply {
            moveTo(w * 0.18f, h * 0.70f)
            lineTo(w * 0.48f, h * 0.60f)
            lineTo(w * 0.54f, h * 0.82f)
            lineTo(w * 0.24f, h * 0.92f)
            close()
        }
        drawPath(tablet, color = Color(0xFF0F172A))
    }
}

/**
 * Bottom Controls: Pagination Dots + Circular Arrow Button or Pill 'Get Started' Button
 */
@Composable
private fun OnboardingBottomBar(
    currentPage: Int,
    totalPages: Int,
    onDotClick: (Int) -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pagination Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until totalPages) {
                val isSelected = i == currentPage
                val dotSize by animateDpAsState(
                    targetValue = if (isSelected) 9.dp else 8.dp,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    label = "dot_size"
                )
                val dotColor = if (isSelected) UxOrange else Color(0xFFCBD5E1)

                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor)
                        .clickable { onDotClick(i) }
                )
            }
        }

        // Action Button: Circular Arrow FAB on screens 1 & 2, Wide Pill 'Get Started' on screen 3
        AnimatedContent(
            targetState = currentPage == totalPages - 1,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
            },
            label = "onboarding_action_button"
        ) { isLastPage ->
            if (isLastPage) {
                Button(
                    onClick = onNextClick,
                    modifier = Modifier
                        .height(50.dp)
                        .shadow(6.dp, RoundedCornerShape(25.dp), spotColor = UxOrange.copy(alpha = 0.4f))
                        .testTag("onboarding_get_started_button"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UxOrange,
                        contentColor = Color.White
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 26.dp,
                        vertical = 12.dp
                    )
                ) {
                    Text(
                        text = "Get Started",
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(6.dp, CircleShape, spotColor = UxOrange.copy(alpha = 0.4f))
                        .clip(CircleShape)
                        .background(UxOrange)
                        .testTag("onboarding_next_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next screen",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
